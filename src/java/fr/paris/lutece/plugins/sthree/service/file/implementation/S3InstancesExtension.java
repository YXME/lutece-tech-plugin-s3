/*
 * Copyright (c) 2002-2026, City of Paris
 * All rights reserved.
 *
 * Redistribution and use in source and binary forms, with or without
 * modification, are permitted provided that the following conditions
 * are met:
 *
 *  1. Redistributions of source code must retain the above copyright notice
 *     and the following disclaimer.
 *
 *  2. Redistributions in binary form must reproduce the above copyright notice
 *     and the following disclaimer in the documentation and/or other materials
 *     provided with the distribution.
 *
 *  3. Neither the name of 'Mairie de Paris' nor 'Lutece' nor the names of its
 *     contributors may be used to endorse or promote products derived from
 *     this software without specific prior written permission.
 *
 * THIS SOFTWARE IS PROVIDED BY THE COPYRIGHT HOLDERS AND CONTRIBUTORS "AS IS"
 * AND ANY EXPRESS OR IMPLIED WARRANTIES, INCLUDING, BUT NOT LIMITED TO, THE
 * IMPLIED WARRANTIES OF MERCHANTABILITY AND FITNESS FOR A PARTICULAR PURPOSE
 * ARE DISCLAIMED. IN NO EVENT SHALL THE COPYRIGHT HOLDERS OR CONTRIBUTORS BE
 * LIABLE FOR ANY DIRECT, INDIRECT, INCIDENTAL, SPECIAL, EXEMPLARY, OR
 * CONSEQUENTIAL DAMAGES (INCLUDING, BUT NOT LIMITED TO, PROCUREMENT OF
 * SUBSTITUTE GOODS OR SERVICES; LOSS OF USE, DATA, OR PROFITS; OR BUSINESS
 * INTERRUPTION) HOWEVER CAUSED AND ON ANY THEORY OF LIABILITY, WHETHER IN
 * CONTRACT, STRICT LIABILITY, OR TORT (INCLUDING NEGLIGENCE OR OTHERWISE)
 * ARISING IN ANY WAY OUT OF THE USE OF THIS SOFTWARE, EVEN IF ADVISED OF THE
 * POSSIBILITY OF SUCH DAMAGE.
 *
 * License 1.0
 */

package fr.paris.lutece.plugins.sthree.service.file.implementation;

import java.lang.reflect.Method;
import java.util.ArrayList;
import java.util.List;
import java.util.Optional;
import java.util.function.Function;

import org.eclipse.microprofile.config.Config;
import org.eclipse.microprofile.config.ConfigProvider;

import fr.paris.lutece.portal.service.file.IFileStoreServiceProvider;
import fr.paris.lutece.portal.service.util.AppLogService;
import jakarta.enterprise.context.ApplicationScoped;
import jakarta.enterprise.event.Observes;
import jakarta.enterprise.inject.Any;
import jakarta.enterprise.inject.Default;
import jakarta.enterprise.inject.literal.NamedLiteral;
import jakarta.enterprise.inject.spi.AfterBeanDiscovery;
import jakarta.enterprise.inject.spi.AnnotatedMethod;
import jakarta.enterprise.inject.spi.BeforeBeanDiscovery;
import jakarta.enterprise.inject.spi.Extension;
import jakarta.enterprise.inject.spi.ProcessBeanAttributes;

/**
 * Registers one S3 file store service provider per instance declared by sthree.instances. When instances are declared, the historical single provider
 * (s3StorageFileService) is not produced.
 */
public class S3InstancesExtension implements Extension
{
    /** The prefix of the bean name of a declared instance */
    public static final String PREFIX_BEAN_NAME = "sthree.";

    private static final String METHOD_LEGACY_PRODUCER = "produceS3StorageFileService";

    private List<S3InstanceDefinition> _listInstances = new ArrayList<>( );

    /**
     * Reads the declared instances before the beans are discovered
     *
     * @param event
     *            the container event
     */
    public void readInstances( @Observes BeforeBeanDiscovery event )
    {
        _listInstances = S3StorageFileServiceFactory.readInstances( configLookup( ), AppLogService::error );
    }

    /**
     * Vetoes the historical single provider when instances are declared
     *
     * @param event
     *            the container event of each bean
     */
    public void vetoLegacyProducer( @Observes ProcessBeanAttributes<?> event )
    {
        if ( !_listInstances.isEmpty( ) && event.getAnnotated( ) instanceof AnnotatedMethod<?> annotatedMethod )
        {
            Method method = annotatedMethod.getJavaMember( );

            if ( METHOD_LEGACY_PRODUCER.equals( method.getName( ) ) && S3StorageFileServiceProducer.class.equals( method.getDeclaringClass( ) ) )
            {
                event.veto( );
            }
        }
    }

    /**
     * Adds a file store service provider bean per declared instance
     *
     * @param event
     *            the container event
     */
    public void addInstances( @Observes AfterBeanDiscovery event )
    {
        for ( S3InstanceDefinition instance : _listInstances )
        {
            event.addBean( ).beanClass( S3StorageFileService.class ).types( IFileStoreServiceProvider.class, Object.class ).scope( ApplicationScoped.class )
                    .qualifiers( Default.Literal.INSTANCE, Any.Literal.INSTANCE, NamedLiteral.of( PREFIX_BEAN_NAME + instance.getId( ) ) )
                    .createWith( context -> S3StorageFileServiceFactory.create( instance.getName( ), configLookup( ),
                            S3StorageFileServiceFactory.instanceKeys( instance.getId( ) ) ) );
            AppLogService.info( "S3 instance '{}' registered as file store service provider '{}'", instance.getId( ), instance.getName( ) );
        }
    }

    /**
     * Returns the instances read from the configuration
     *
     * @return the declared instances
     */
    List<S3InstanceDefinition> getInstances( )
    {
        return _listInstances;
    }

    /**
     * Returns a lookup of the MicroProfile configuration
     *
     * @return a function reading a property, empty when absent
     */
    private static Function<String, Optional<String>> configLookup( )
    {
        Config config = ConfigProvider.getConfig( );

        return strKey -> config.getOptionalValue( strKey, String.class );
    }
}

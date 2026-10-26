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

import java.util.ArrayList;
import java.util.HashSet;
import java.util.List;
import java.util.Optional;
import java.util.Set;
import java.util.function.Consumer;
import java.util.function.Function;
import java.util.function.UnaryOperator;

import org.apache.commons.lang3.StringUtils;

import fr.paris.lutece.portal.service.file.IFileDownloadUrlService;
import fr.paris.lutece.portal.service.file.IFileRBACService;
import fr.paris.lutece.portal.service.util.CdiHelper;

/**
 * Builds the S3 file store service providers from the configuration: the historical single instance (keys s3Url, s3Bucket...) or the instances declared
 * by sthree.instances (keys sthree.instance.&lt;id&gt;.url, .bucket...).
 */
public final class S3StorageFileServiceFactory
{
    /** The key listing the declared instances */
    public static final String PROPERTY_INSTANCES = "sthree.instances";
    /** The prefix of the keys of a declared instance */
    public static final String PREFIX_INSTANCE = "sthree.instance.";
    /** The name of the historical single provider */
    public static final String LEGACY_PROVIDER_NAME = "s3StorageFileService";

    static final String URL = "url";
    static final String BUCKET = "bucket";
    static final String KEY = "key";
    static final String PASSWORD = "password";
    static final String DEFAULT_FILE_PATH = "defaultFilePath";
    static final String FORCE_PATH_STYLE = "forcePathStyle";
    static final String REGION = "region";
    static final String CHECKSUM_ALGORITHM = "checksumAlgorithm";
    static final String PROXY_HOST = "proxyHost";
    static final String PROXY_USERNAME = "proxyUsername";
    static final String PROXY_PASSWORD = "proxyPassword";
    static final String REQUEST_TIMEOUT = "requestTimeout";
    static final String CONNECTION_TIMEOUT = "connectionTimeout";
    static final String DOWNLOAD_SERVICE = "downloadService";
    static final String RBAC_SERVICE = "rbacService";
    static final String NAME = "name";

    private static final String DEFAULT_RBAC_SERVICE = "defaultFileRBACService";
    private static final String PREFIX_DEFAULT_NAME = "s3-";
    private static final String DEFAULT_FILE_PATH_PATTERN = "{YYYY}/{MM}-{DD}/{code}-{HH}-{mm}-{ss}-{UUID}";

    /**
     * Private constructor
     */
    private S3StorageFileServiceFactory( )
    {
    }

    /**
     * Returns the key of a setting of the historical single instance
     *
     * @param strSetting
     *            the setting (url, bucket...)
     * @return the property key (s3Url, s3Bucket...)
     */
    static String legacyKey( String strSetting )
    {
        if ( DOWNLOAD_SERVICE.equals( strSetting ) || RBAC_SERVICE.equals( strSetting ) )
        {
            return S3StorageFileServiceProducer.BEAN_FILE_STORE_SERVICE_PROVIDER + "." + strSetting;
        }

        return "s3" + StringUtils.capitalize( strSetting );
    }

    /**
     * Returns the function giving the keys of the settings of a declared instance
     *
     * @param strId
     *            the id of the instance
     * @return the function giving sthree.instance.&lt;id&gt;.&lt;setting&gt;
     */
    static UnaryOperator<String> instanceKeys( String strId )
    {
        return strSetting -> PREFIX_INSTANCE + strId + "." + strSetting;
    }

    /**
     * Reads the instances declared by sthree.instances. An instance without url or bucket, or whose name is already taken, is reported and left out.
     *
     * @param lookup
     *            reads a property, empty when absent
     * @param errorLog
     *            receives the description of each refused instance
     * @return the valid instances, in the declared order
     */
    public static List<S3InstanceDefinition> readInstances( Function<String, Optional<String>> lookup, Consumer<String> errorLog )
    {
        List<S3InstanceDefinition> listInstances = new ArrayList<>( );
        Set<String> setNames = new HashSet<>( );

        for ( String strId : StringUtils.split( lookup.apply( PROPERTY_INSTANCES ).orElse( "" ), ", " ) )
        {
            UnaryOperator<String> keyOf = instanceKeys( strId );
            String strName = lookup.apply( keyOf.apply( NAME ) ).filter( StringUtils::isNotBlank ).orElse( PREFIX_DEFAULT_NAME + strId );

            if ( lookup.apply( keyOf.apply( URL ) ).filter( StringUtils::isNotBlank ).isEmpty( )
                    || lookup.apply( keyOf.apply( BUCKET ) ).filter( StringUtils::isNotBlank ).isEmpty( ) )
            {
                errorLog.accept( "S3 instance '" + strId + "' ignored: " + keyOf.apply( URL ) + " and " + keyOf.apply( BUCKET ) + " are required" );
            }
            else
                if ( !setNames.add( strName ) )
                {
                    errorLog.accept( "S3 instance '" + strId + "' ignored: the provider name '" + strName + "' is already used by another instance" );
                }
                else
                {
                    listInstances.add( new S3InstanceDefinition( strId, strName ) );
                }
        }

        return listInstances;
    }

    /**
     * Returns the path pattern of the new objects of an instance: its own setting, else the one of the historical single instance (s3DefaultFilePath),
     * else {YYYY}/{MM}-{DD}/{code}-{HH}-{mm}-{ss}-{UUID}. A blank pattern would give an empty object key, which S3 refuses.
     *
     * @param lookup
     *            reads a property, empty when absent
     * @param keyOf
     *            gives the property key of each setting
     * @return the path pattern
     */
    static String resolveDefaultFilePath( Function<String, Optional<String>> lookup, UnaryOperator<String> keyOf )
    {
        return lookup.apply( keyOf.apply( DEFAULT_FILE_PATH ) ).filter( StringUtils::isNotBlank )
                .or( ( ) -> lookup.apply( legacyKey( DEFAULT_FILE_PATH ) ).filter( StringUtils::isNotBlank ) ).orElse( DEFAULT_FILE_PATH_PATTERN );
    }

    /**
     * Builds an S3 file store service provider from its settings
     *
     * @param strName
     *            the name of the provider
     * @param lookup
     *            reads a property, empty when absent
     * @param keyOf
     *            gives the property key of each setting
     * @return the provider
     */
    public static S3StorageFileService create( String strName, Function<String, Optional<String>> lookup, UnaryOperator<String> keyOf )
    {
        Function<String, String> setting = strSetting -> lookup.apply( keyOf.apply( strSetting ) ).orElse( "" );
        String strDownloadService = StringUtils.defaultIfBlank( setting.apply( DOWNLOAD_SERVICE ), S3StorageFileServiceProducer.BEAN_FILE_DOWNLOAD_URL_SERVICE );
        String strRbacService = StringUtils.defaultIfBlank( setting.apply( RBAC_SERVICE ), DEFAULT_RBAC_SERVICE );

        S3StorageFileService service = new S3StorageFileService( CdiHelper.getReference( IFileDownloadUrlService.class, strDownloadService ),
                CdiHelper.getReference( IFileRBACService.class, strRbacService ), setting.apply( URL ), setting.apply( BUCKET ), setting.apply( KEY ),
                setting.apply( PASSWORD ), resolveDefaultFilePath( lookup, keyOf ), setting.apply( FORCE_PATH_STYLE ), setting.apply( REGION ),
                setting.apply( CHECKSUM_ALGORITHM ), setting.apply( PROXY_HOST ), setting.apply( PROXY_USERNAME ), setting.apply( PROXY_PASSWORD ),
                setting.apply( REQUEST_TIMEOUT ), setting.apply( CONNECTION_TIMEOUT ) );
        service.setName( strName );

        return service;
    }
}

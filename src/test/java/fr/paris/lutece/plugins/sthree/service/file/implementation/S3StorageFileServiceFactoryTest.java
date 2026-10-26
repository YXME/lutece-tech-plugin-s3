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

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Optional;

import org.junit.jupiter.api.Test;

import fr.paris.lutece.test.LuteceTestCase;

/**
 * Test of the configuration of the S3 instances
 */
public class S3StorageFileServiceFactoryTest extends LuteceTestCase
{
    /**
     * Reads the declared instances of a configuration
     *
     * @param mapConfig
     *            the properties
     * @param listErrors
     *            receives the refused instances
     * @return the valid instances
     */
    private static List<S3InstanceDefinition> read( Map<String, String> mapConfig, List<String> listErrors )
    {
        return S3StorageFileServiceFactory.readInstances( strKey -> Optional.ofNullable( mapConfig.get( strKey ) ), listErrors::add );
    }

    /**
     * Two complete instances give two providers, named by their name key or s3-&lt;id&gt;
     */
    @Test
    public void testTwoInstances( )
    {
        Map<String, String> mapConfig = new HashMap<>( );
        mapConfig.put( "sthree.instances", "courant, archives" );
        mapConfig.put( "sthree.instance.courant.url", "http://localhost:9000" );
        mapConfig.put( "sthree.instance.courant.bucket", "courant" );
        mapConfig.put( "sthree.instance.archives.url", "http://localhost:9000" );
        mapConfig.put( "sthree.instance.archives.bucket", "archives" );
        mapConfig.put( "sthree.instance.archives.name", "s3-archives-2024" );
        List<String> listErrors = new ArrayList<>( );

        List<S3InstanceDefinition> listInstances = read( mapConfig, listErrors );

        assertEquals( 2, listInstances.size( ), "two instances" );
        assertEquals( "courant", listInstances.get( 0 ).getId( ), "declared order" );
        assertEquals( "s3-courant", listInstances.get( 0 ).getName( ), "default name" );
        assertEquals( "s3-archives-2024", listInstances.get( 1 ).getName( ), "configured name" );
        assertTrue( listErrors.isEmpty( ), "no error" );
    }

    /**
     * No declared instance: nothing replaces the historical single provider
     */
    @Test
    public void testNoInstance( )
    {
        List<String> listErrors = new ArrayList<>( );

        assertTrue( read( new HashMap<>( ), listErrors ).isEmpty( ), "absent key" );
        assertTrue( read( Map.of( "sthree.instances", " " ), listErrors ).isEmpty( ), "blank key" );
        assertTrue( listErrors.isEmpty( ), "no error" );
    }

    /**
     * An instance without url or bucket, or with a name already used, is refused and reported; the others are kept
     */
    @Test
    public void testInvalidInstances( )
    {
        Map<String, String> mapConfig = new HashMap<>( );
        mapConfig.put( "sthree.instances", "a,nobucket,nourl,b" );
        mapConfig.put( "sthree.instance.a.url", "http://localhost:9000" );
        mapConfig.put( "sthree.instance.a.bucket", "a" );
        mapConfig.put( "sthree.instance.a.name", "shared-name" );
        mapConfig.put( "sthree.instance.nobucket.url", "http://localhost:9000" );
        mapConfig.put( "sthree.instance.nourl.bucket", "nourl" );
        mapConfig.put( "sthree.instance.b.url", "http://localhost:9000" );
        mapConfig.put( "sthree.instance.b.bucket", "b" );
        mapConfig.put( "sthree.instance.b.name", "shared-name" );
        List<String> listErrors = new ArrayList<>( );

        List<S3InstanceDefinition> listInstances = read( mapConfig, listErrors );

        assertEquals( 1, listInstances.size( ), "only the first valid instance" );
        assertEquals( "a", listInstances.get( 0 ).getId( ), "instance a kept" );
        assertEquals( 3, listErrors.size( ), "three refusals: " + listErrors );
    }

    /**
     * The historical single instance keeps its keys
     */
    @Test
    public void testLegacyKeys( )
    {
        assertEquals( "s3Url", S3StorageFileServiceFactory.legacyKey( "url" ) );
        assertEquals( "s3DefaultFilePath", S3StorageFileServiceFactory.legacyKey( "defaultFilePath" ) );
        assertEquals( "s3ForcePathStyle", S3StorageFileServiceFactory.legacyKey( "forcePathStyle" ) );
        assertEquals( "sthree.s3StorageFileService.downloadService", S3StorageFileServiceFactory.legacyKey( "downloadService" ) );
        assertEquals( "sthree.instance.archives.bucket", S3StorageFileServiceFactory.instanceKeys( "archives" ).apply( "bucket" ) );
    }

    /**
     * A provider built for an instance carries the instance name
     */
    @Test
    public void testCreate( )
    {
        Map<String, String> mapConfig = Map.of( "sthree.instance.archives.url", "http://localhost:9000", "sthree.instance.archives.bucket", "archives" );

        S3StorageFileService service = S3StorageFileServiceFactory.create( "s3-archives", strKey -> Optional.ofNullable( mapConfig.get( strKey ) ),
                S3StorageFileServiceFactory.instanceKeys( "archives" ) );

        assertEquals( "s3-archives", service.getName( ), "provider name" );
    }

    /**
     * An instance without path pattern takes the one of the historical instance, else the standard pattern: never an empty object key
     */
    @Test
    public void testDefaultFilePath( )
    {
        java.util.function.UnaryOperator<String> keyOf = S3StorageFileServiceFactory.instanceKeys( "archives" );

        assertEquals( "{UUID}", S3StorageFileServiceFactory.resolveDefaultFilePath(
                strKey -> Optional.ofNullable( Map.of( "sthree.instance.archives.defaultFilePath", "{UUID}", "s3DefaultFilePath", "{YYYY}" ).get( strKey ) ), keyOf ) );
        assertEquals( "{YYYY}", S3StorageFileServiceFactory.resolveDefaultFilePath( strKey -> Optional.ofNullable( Map.of( "s3DefaultFilePath", "{YYYY}" ).get( strKey ) ),
                keyOf ) );
        assertEquals( "{YYYY}/{MM}-{DD}/{code}-{HH}-{mm}-{ss}-{UUID}", S3StorageFileServiceFactory.resolveDefaultFilePath( strKey -> Optional.empty( ), keyOf ) );
    }
}

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

import org.eclipse.microprofile.config.Config;
import org.eclipse.microprofile.config.ConfigProvider;

import fr.paris.lutece.portal.service.file.IFileDownloadUrlService;
import fr.paris.lutece.portal.service.file.IFileStoreServiceProvider;
import fr.paris.lutece.portal.service.file.implementation.DefaultFileDownloadService;
import jakarta.enterprise.context.ApplicationScoped;
import jakarta.enterprise.inject.Produces;
import jakarta.inject.Named;

/**
 * Produces the S3 file store service provider and its download URL service.
 */
@ApplicationScoped
public class S3StorageFileServiceProducer
{
    public static final String BEAN_FILE_STORE_SERVICE_PROVIDER = "sthree.s3StorageFileService";
    public static final String BEAN_FILE_DOWNLOAD_URL_SERVICE = "sthree.s3FileDownloadUrlService";

    private static final String PROVIDER_NAME = S3StorageFileServiceFactory.LEGACY_PROVIDER_NAME;
    private static final String DOWNLOAD_SEPARATOR = "!";

    /**
     * Produces a download URL service whose separator allows paths in file keys.
     *
     * @return the download URL service
     */
    @Produces
    @ApplicationScoped
    @Named( BEAN_FILE_DOWNLOAD_URL_SERVICE )
    public IFileDownloadUrlService produceFileDownloadUrlService( )
    {
        DefaultFileDownloadService downloadService = new DefaultFileDownloadService( );
        downloadService.setSeparator( DOWNLOAD_SEPARATOR );

        return downloadService;
    }

    /**
     * Produces the historical single S3 file store service provider from the keys s3Url, s3Bucket... of the plugin configuration. It is not produced
     * when instances are declared by sthree.instances (S3InstancesExtension).
     *
     * @return the S3 file store service provider
     */
    @Produces
    @ApplicationScoped
    @Named( BEAN_FILE_STORE_SERVICE_PROVIDER )
    public IFileStoreServiceProvider produceS3StorageFileService( )
    {
        Config config = ConfigProvider.getConfig( );

        return S3StorageFileServiceFactory.create( PROVIDER_NAME, strKey -> config.getOptionalValue( strKey, String.class ),
                S3StorageFileServiceFactory::legacyKey );
    }
}

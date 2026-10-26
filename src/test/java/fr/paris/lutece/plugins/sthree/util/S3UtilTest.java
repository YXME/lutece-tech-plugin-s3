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
package fr.paris.lutece.plugins.sthree.util;

import org.junit.jupiter.api.Test;

import fr.paris.lutece.test.LuteceTestCase;

/**
 * Tests the S3 path pattern.
 */
public class S3UtilTest extends LuteceTestCase
{
    /**
     * Checks every variable of the pattern is replaced.
     */
    @Test
    public void testGetStrPath( )
    {
        String strPath = S3Util.getStrPath( "{YYYY}/{MM}-{DD}/{code}-{HH}-{mm}-{ss}-{UUID}" );

        assertFalse( strPath.contains( "{" ) );
        assertTrue( strPath.matches( "\\d{4}/\\d{1,2}-\\d{1,2}/.*-\\d{1,2}-\\d{1,2}-\\d{1,2}-[0-9a-f-]{36}" ) );
        assertNotEquals( S3Util.getStrPath( "{UUID}" ), S3Util.getStrPath( "{UUID}" ) );
    }

    /**
     * A code written as a URL gives a key segment S3 accepts, and the empty segments of the path are removed
     */
    @Test
    public void testKeySegment( )
    {
        assertEquals( "http-127.0.0.1-lutece", S3Util.toKeySegment( "http://127.0.0.1/lutece" ) );
        assertEquals( "mon-site", S3Util.toKeySegment( "mon site" ) );
        assertEquals( "", S3Util.toKeySegment( "" ) );
        assertFalse( S3Util.getStrPath( "/{YYYY}//{UUID}" ).startsWith( "/" ) );
        assertFalse( S3Util.getStrPath( "/{YYYY}//{UUID}" ).contains( "//" ) );
    }
}

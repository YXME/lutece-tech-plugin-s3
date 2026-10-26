/*
 * Copyright (c) 2002-2023, City of Paris
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

import java.util.regex.Matcher;

import fr.paris.lutece.portal.service.util.AppPropertiesService;

public class S3Util
{

    private static final String CODE = "lutece.code";

    private S3Util( )
    {

    }

    /**
     * Builds the key of a new object from the path pattern: {YYYY}, {MM}, {DD}, {HH}, {mm}, {ss}, {UUID} and {code} (lutece.code) are replaced. The
     * code is reduced to the characters an S3 key segment accepts (a code written as a URL, http://host/lutece, gives http-host-lutece), and the empty
     * segments of the path are removed.
     *
     * @param pattern
     *            the path pattern
     * @return the object key
     */
    public static String getStrPath( String pattern )
    {
        String strPath = pattern.replaceAll( "\\{YYYY\\}", java.time.Year.now( ).toString( ) )
                .replaceAll( "\\{MM\\}", java.time.MonthDay.now( ).getMonthValue( ) + "" )
                .replaceAll( "\\{DD\\}", java.time.MonthDay.now( ).getDayOfMonth( ) + "" )
                .replaceAll( "\\{HH\\}", java.time.LocalTime.now( ).getHour( ) + "" )
                .replaceAll( "\\{mm\\}", java.time.LocalTime.now( ).getMinute( ) + "" )
                .replaceAll( "\\{ss\\}", java.time.LocalTime.now( ).getSecond( ) + "" )
                .replaceAll( "\\{UUID\\}", java.util.UUID.randomUUID( ).toString( ) )
                .replaceAll( "\\{code\\}", Matcher.quoteReplacement( toKeySegment( AppPropertiesService.getProperty( CODE, "" ) ) ) );

        return strPath.replaceAll( "/{2,}", "/" ).replaceAll( "^/", "" );
    }

    /**
     * Reduces a value to the characters an S3 key segment accepts: letters, digits and ! _ . * ' ( ) -; every other run of characters becomes a dash
     *
     * @param strValue
     *            the value
     * @return the key segment
     */
    static String toKeySegment( String strValue )
    {
        return strValue.replaceAll( "[^A-Za-z0-9!_.*'()-]+", "-" ).replaceAll( "^-+|-+$", "" );
    }
}

package edu.umich.auth.cosign;

import java.security.SecureRandom;

import edu.umich.auth.cosign.util.Base64;

/**
 * This class is responsible for the encoding/decoding of a Cosign Cookie.
 * @author saxman
 */
public class CosignCookie {
  private static final char COOKIE_DIVIDER = '/';
  private static final int COOKIE_LENGTH = 124;

  private final String cookie;
  private final String nonce;
  private final long timestamp;

  /**
   * Constructor for CosignCookie.  This constructor will generate a new
   * random service cookie and set its timestamp to the current system
   * time.
   */
  public CosignCookie() {
    this( generateNonce(), System.currentTimeMillis() );
  }

  /**
   * Constructor for CosignCookie.  This constructor will generate a
   * cosign cookie using the given random service cookie and timestamp.
   * @param random  String
   * @param timestamp
   */
  public CosignCookie( String random, long timestamp ) {
    this.nonce = random;
    this.timestamp = timestamp;
    this.cookie = random + COOKIE_DIVIDER + Long.toString( timestamp );
  }

  /**
   * This method will return the full cosign cookie (random bytes and
   * timestamp).
   */
  public String getCookie() {
    return cookie;
  }

  /**
   * This method will return the random portion of the cosign cookie.
   */
  public String getNonce() {
    return nonce;
  }

  /**
   * This method will return the creation timestamp of the cosign cookie.
   */
  public long getTimestamp () {
    return timestamp;
  }

  /**
   * This method will return the full cosign cookie.
   */
  public String toString() {
    return getCookie();
  }

  /**
   * This method will parse the given cookieValue parameter and
   * attempt to extract the random bytes and creaton timestamp.
   * This method will return null if the given cookieValue is
   * invalid.
   */
  public static CosignCookie parseCosignCookie( String cookieValue ) {
    if ( cookieValue == null ) {
      return null;
    }

    int dividerIdx = cookieValue.lastIndexOf( COOKIE_DIVIDER );
    if ( dividerIdx < 0 ) {
      return null;
    }

    String random = cookieValue.substring( 0, dividerIdx );
    int len = random.length();
    //if ( Base64.decode( random ).length != COOKIE_LENGTH ) {
    if ( len != COOKIE_LENGTH ) {
      return null;
    }

    try {
      long timestamp = Long.parseLong( cookieValue.substring( dividerIdx + 1 ) );
      return new CosignCookie( random, timestamp );
    } catch ( Exception e ) {
      return null;
    }

  }

  /**
   * This method will generate a random string in Base64 encoding.
   */
  private static String generateNonce () {
    byte[] bytes = new byte[COOKIE_LENGTH];

    SecureRandom random = new SecureRandom();
    random.nextBytes( bytes );

    return Base64.encode( bytes );
  }

}

/*Copyright (c) 2002-2008 Regents of The University of Michigan.
All Rights Reserved.

    Permission to use, copy, modify, and distribute this software and
    its documentation for any purpose and without fee is hereby granted,
    provided that the above copyright notice appears in all copies and
    that both that copyright notice and this permission notice appear
    in supporting documentation, and that the name of The University
    of Michigan not be used in advertising or publicity pertaining to
    distribution of the software without specific, written prior
    permission. This software is supplied as is without expressed or
    implied warranties of any kind.

The University of Michigan
c/o UM Webmaster Team
Arbor Lakes
Ann Arbor, MI  48105
*/

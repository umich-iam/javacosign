package edu.umich.auth.cosign.util;

/**
 * <p>Title: </p>
 *
 * <p>Description: </p>
 *
 * <p>Copyright: Copyright (c) 2005</p>
 *
 * <p>Company: </p>
 *
 * @author not attributable
 * @version 1.0
 */
public class ProxyCookie {
    private String host;
    private String cookieName;
    private String cookieValue;
    private String service;
    public ProxyCookie() {
    }

    public ProxyCookie(String cookie, String host, String service, String value) {
        this.cookieName = cookie;
        this.host = host;
        this.cookieValue = value;
        this.service = service;
    }

    public ProxyCookie(String serverResponse, String service) {
        String code = serverResponse.substring(0, 3);
        int pos = serverResponse.indexOf("=");
        int pos2 = serverResponse.lastIndexOf(" ");

        String cookieHead = serverResponse.substring(4, pos);
        String cookieFoot = serverResponse.substring(pos + 1, pos2);
        String cookieHost = serverResponse.substring(pos2+1);

        this.cookieName = cookieHead;
        this.cookieValue = cookieFoot;
        this.host = cookieHost;
        this.service = service;
    }


    public void setHost(String host) {
        this.host = host;
    }

    public void setCookieName(String cookie) {
        this.cookieName = cookie;
    }

    public void setService(String service) {
        this.service = service;
    }

    public String getHost() {
        return host;
    }

    public String getCookieName() {
        return cookieName;
    }

    public String getService() {
        return service;
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
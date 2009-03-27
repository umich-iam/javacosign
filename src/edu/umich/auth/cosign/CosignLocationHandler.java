package edu.umich.auth.cosign;

import edu.umich.auth.cosign.util.ServiceConfig;
import edu.umich.auth.cosign.pool.CosignConnectionList;
import edu.umich.auth.cosign.pool.CosignConnection;
import edu.umich.auth.cosign.pool.CosignConnectionPool;
import javax.security.auth.login.LoginException;
import edu.umich.auth.cosign.pool.CosignConnectionList;
import javax.security.auth.login.FailedLoginException;
import org.apache.commons.logging.LogFactory;
import org.apache.commons.logging.Log;
import javax.servlet.http.Cookie;
import javax.servlet.http.HttpServletResponse;


public class CosignLocationHandler {

    private int cosignCode = CosignConnection.COSIGN_CODE_UNKNOWN;
    private Log log = LogFactory.getLog(CosignLoginModule.class);
    CosignCookie cosignCookie;

    public CosignLocationHandler() {
    }

public void init() throws Exception{
    if ((((String) CosignConfig.INSTANCE.
               getPropertyValue(
                       CosignConfig.
                       COSIGN_SERVER_VERSION)).length() == 0)) {
            // Grab a connection list from the pool
            CosignConnectionList cosignConnectionList;
            try {
                cosignConnectionList = CosignConnectionPool.INSTANCE.
                                       borrowCosignConnectionList();
            } catch (Exception e) {
                throw new FailedLoginException(
                        "Failed to borrow cosign connections from pool.");
            }

            // Return the connection list back to the pool
            try {
                CosignConnectionPool.INSTANCE.returnCosignConnectionList(
                        cosignConnectionList);
            } catch (Exception e) {
                log.error("Failed to return cosign connections to pool.");
            }

        }

}
    public boolean check(String cookieNounce, ServiceConfig service,
                         String location, HttpServletResponse response) throws
            Exception {

        // Grab a connection list from the pool

        cosignCookie = new CosignCookie(cookieNounce, System.currentTimeMillis());

        CosignConnectionList cosignConnectionList;
        try {
            cosignConnectionList = CosignConnectionPool.INSTANCE.
                                   borrowCosignConnectionList();
        } catch (Exception e) {
            throw new LoginException(
                    "Failed to borrow cosign connections from pool.");
        }

        // Keep trying until we get a server which will serve us,
        // or there are no servers available in the pool.

        String cosignResponse = cosignConnectionList.checkCookie(service.
                getName(),
                cosignCookie.getNonce());

        cosignCode = CosignConnection.convertResponseToCode(cosignResponse);

        // Return the connection list back to the pool
        try {
            CosignConnectionPool.INSTANCE.returnCosignConnectionList(
                    cosignConnectionList);
        } catch (Exception e) {
            log.error("Failed to return cosign connections to pool.");
        }

        // Translate server response to boolean return or exception.
        // NOTE: No false return since that would tell LoginContext to ignore this module.
        if (cosignResponse == null) {
            throw new Exception(
                    "No cosignd servers available for authentication.");
        } else if (cosignCode != CosignConnection.COSIGN_USER_AUTHENTICATED) {
            throw new Exception("User not authenticated to Cosign.");
        } else if (cosignCode == CosignConnection.COSIGN_USER_AUTHENTICATED) {
            /* Generate the cookie and assign it to the response. */
            String cookieName = service.getName();
            Cookie cookie = new Cookie(cookieName, cosignCookie.getCookie());
            cookie.setPath("/");

            // If Cosign is in HTTPS-only mode, we need to mark the cookie as secure
            boolean isHttpsOnly = ((Boolean) CosignConfig.INSTANCE.
                                   getPropertyValue(
                                           CosignConfig.HTTPS_ONLY)).
                                  booleanValue();
            if (isHttpsOnly) {
                cookie.setSecure(true);
            }
            response.addCookie(cookie);

            return true;
        }
        return false;
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

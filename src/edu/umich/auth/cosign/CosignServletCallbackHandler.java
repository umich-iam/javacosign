package edu.umich.auth.cosign;

import java.io.IOException;

import java.util.Map;
import java.util.Iterator;

import javax.security.auth.Subject;

import javax.security.auth.login.FailedLoginException;

import javax.security.auth.callback.Callback;
import javax.security.auth.callback.TextInputCallback;
import javax.security.auth.callback.UnsupportedCallbackException;

import javax.servlet.ServletException;
import javax.servlet.http.Cookie;
import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;

import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;

import edu.umich.auth.AuthFilterRequestWrapper;
import edu.umich.auth.ServletCallbackHandler;
import edu.umich.auth.cosign.util.ServiceConfig;
import java.util.HashMap;
import edu.umich.auth.cosign.util.FactorInputCallBack;
import edu.umich.auth.cosign.pool.CosignConnectionPool;
import edu.umich.auth.cosign.pool.CosignConnectionList;

/**
 * This class implements ServletCallbackHandler and is resposible for setting the
 * cosign service cookie and redirecting the user to the login URL.  Additionally,
 * this class instantiates a wrapper for the HttpServletRequest object to provide
 * access to authenticated user's information.
 * @see edu.umich.auth.ServletCallbackHandler
 * @author $Author$
 * @version $Name$ $Revision$ $Date$
 */
public class CosignServletCallbackHandler implements ServletCallbackHandler {

    private static final String COOKIE_NAME_PREFIX = "cosign-";

    private HttpServletRequest request;

    private HttpServletResponse response;

    private Subject subject;

    private String currentPath;
    private String queryString;
    private String resource;
    private HashMap qString;

    // Used for logging info and error messages
    private Log log = LogFactory.getLog(CosignServletCallbackHandler.class);

    /**
     * This method copies the request, response, and subject variables provided to it.
     * @see edu.umich.auth.ServletCallbackHandler#init( Map, HttpServletRequest,
     *      HttpServletResponse, Subject )
     * @return always returns true
     * @throws IllegalArgumentException
     *           if an improper parameter is passed in.
     * @throws FailedLoginException
     *           if the client's IP address has changed and this check if turned
     *           on.
     */
    public boolean init(Map parameters, HttpServletRequest request,
                        HttpServletResponse response, Subject subject) throws
            FailedLoginException {

        // Check for initialization errors.
        if ((response == null) || (request == null) || (subject == null)) {
            throw new IllegalArgumentException(
                    "Required initialization parameter(s) missing.");
        }

        this.request = request;
        this.response = response;
        this.subject = subject;

        this.queryString = request.getQueryString();
        String cPath = request.getPathInfo();
        String currentReqUrl = request.getRequestURI();
        this.currentPath = currentReqUrl.substring(request.getContextPath().
                length());
        this.resource = this.currentPath.substring(this.currentPath.lastIndexOf(
                '/') + 1);
        if (this.currentPath.charAt(this.currentPath.length() - 1) != '/') {
            this.currentPath = this.currentPath.substring(0,
                    this.currentPath.lastIndexOf('/') + 1);
        }
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
        return true;
    }

    /**
     * This method sets a new cosign service cookie and redirects the user to the
     * login url.
     * @return Returns false if the user's request is finished and should not
     *    be processed by any other filters, or true if the request should
     *    continue to be processed.
     * @see edu.umich.auth.ServletCallbackHandler#handleFailedLogin( Exception )
     */
    public boolean handleFailedLogin(Exception ex) throws ServletException {
        /*
         * The session is being invalidated here since, if someone logs out of
         * another Cosign service, and someone else uses their same browser
         * window, logs into Cosign, and then comes to an app. that's Cosign
         * protects, the previous user's session will still be active.
         *
         * We should perhaps make this optional, and allow the admin. to define a
         * default index page that clears out the last user's session information.
         */


        String cookieName="";
        ServiceConfig serviceConfig = null;
        if (!(ex instanceof FailedLoginException)) {
            // we didn't handle the exception and anon access isn't enabled,
            // we want to display a 503.
            throw new ServletException(ex);
        }

        // Log the reason for the failed login
        if (log.isDebugEnabled()) {
            log.debug(ex.getMessage());
        }

        // If a CosignPrincipal exists in this user's session, we need to
        // remove it.
        final CosignPrincipal oldCosignPrincipal = getCosignPrincipal();
        if (oldCosignPrincipal != null) {
            if (!subject.getPrincipals().remove(oldCosignPrincipal)) {
                throw new ServletException(
                        "Failed to remove cosign principal from subject.");
            }

            // optionally, clear the HTTP session to prevent data xfer
            // between different user sessions
            final boolean clearSession = ((Boolean) CosignConfig.INSTANCE.
                                          getPropertyValue(
                                                  CosignConfig.
                                                  CLEAR_SESSION_ON_LOGIN)).
                                         booleanValue();
            if (clearSession) {
                log.debug("Invalidating HTTP servlet session.");
                request.getSession().invalidate();
            }
        }

        // add additional filtering here. //
        serviceConfig = CosignConfig.INSTANCE.hasServiceOveride(this.
                currentPath, this.resource, this.queryString);
        if (serviceConfig != null && serviceConfig.isPublicAccess()) {
            log.debug("Anonymous user permitted access to site.");
            return true;
        }

        // If 'AllowPublicAccess' is enabled, we can ignore any login errors
        // and allow the user into the website
        //This needs to be re-worked for url level only
        //
        //boolean allowPublicAccess = ((Boolean)CosignConfig.INSTANCE.getPropertyValue( CosignConfig.ALLOW_PUBLIC_ACCESS)).booleanValue();
        //if ( allowPublicAccess ) {
        //  log.debug( "Anonymous user permitted access to site." );
        //  return true;
        // }

        /* Generate the cookie and assign it to the response. */

        cookieName = getCookieName(serviceConfig);
        CosignCookie cosignCookie = new CosignCookie();
        Cookie cookie = new Cookie(cookieName, cosignCookie.getCookie());
        cookie.setPath("/");
        cookie.setMaxAge(0);
        // If Cosign is in HTTPS-only mode, we need to mark the cookie as secure
        boolean isHttpsOnly = ((Boolean) CosignConfig.INSTANCE.getPropertyValue(
                CosignConfig.HTTPS_ONLY)).booleanValue();
        if (isHttpsOnly) {
            cookie.setSecure(true);
        }
       /* commented out for v3 */
       response.addCookie(cookie); //this will remove the cookie

        // If a site entry URL was provided, we will use that for the redirect,
        // not the current URL.
        String siteEntryUrl = (String) CosignConfig.INSTANCE.getPropertyValue(
                CosignConfig.LOGIN_SITE_ENTRY_URL);
        if (siteEntryUrl == null) {

            // Construct the query string to send to weblogin server.
            String queryString = request.getQueryString();
            queryString = (null == queryString) ? "" : "?" + queryString;

            StringBuffer requestURL = new StringBuffer();
            String scheme = request.getScheme();
            int port = request.getServerPort();

            // If we are in secure HTTPS-only mode, we need to fudge the current URL
            // so that it is HTTPS.
            if (isHttpsOnly) {
                scheme = "https";
                if (!request.isSecure()) {
                    port = ((Integer) CosignConfig.INSTANCE.getPropertyValue(
                            CosignConfig.HTTPS_PORT)).intValue();
                }
            }

            requestURL.append(scheme); // http, https
            requestURL.append("://");
            requestURL.append(request.getServerName());

            if ((scheme.equals("http") && port != 80) ||
                (scheme.equals("https") && port != 443)) {
                requestURL.append(':');
                requestURL.append(port);
            }

            requestURL.append(request.getRequestURI());
            requestURL.append(queryString);

            siteEntryUrl = requestURL.toString();

        }

        // If the HTTP method was POST and we have a valid PostErrorRedirectUrl, we will redirect
        // to that URL.  Otherwise, we will redirect to the normal login url.
        String loginUrl;
        String postRedirectErrorUrl = (String) CosignConfig.INSTANCE.
                                      getPropertyValue(CosignConfig.
                LOGIN_POST_ERROR_URL);
        if (request.getMethod().toLowerCase().equals("post")) {
            loginUrl = postRedirectErrorUrl;
        } else {
            loginUrl = (String) CosignConfig.INSTANCE.getPropertyValue(
                    CosignConfig.LOGIN_REDIRECT_URL);
        }
        String serviceFactors = new String();
        if (serviceConfig != null && serviceConfig.hasFactors()) {
            if (!CosignConfig.INSTANCE.isServerVersion2()) {
                throw new ServletException(
                        "Service is configured with factors but Cosign server does not support factors");
            }
            serviceFactors = "factors=" + serviceConfig.factorsAsString() + "&";
        }
        // Redirect the client to the weblogin server.
        try {
           /* commented out for v3 - changed that is */
            /*String redirectUrl = loginUrl + "?" + serviceFactors +
                                 getCookieName() + "=" +
                                 cosignCookie.getNonce() + ";&" + siteEntryUrl;*/

            String redirectUrl = loginUrl + "?" + serviceFactors +
                                 getCookieName(serviceConfig) + "&" + siteEntryUrl;


            if (log.isDebugEnabled()) {
                log.debug("Redirecting user to: " + redirectUrl);
                log.info("Redirecting user to: " + redirectUrl);
            }
            response.sendRedirect(redirectUrl);

        } catch (Exception e) {
            // Hmm ... we weren't able to redirect the user to the login page.  We need
            // to send him a 503.
            throw new ServletException(e);
        }

        // FALSE indicates that we don't want to continue processing other filters
        return false;
    }

    /**
     * This method creates a wrapper for the HttpServletRequest to provide
     * the client application with access to the cosign authentication details.
     * @see edu.umich.auth.ServletCallbackHandler#handleSuccessfulLogin()
     */
    public void handleSuccessfulLogin() throws ServletException {

        // Check if a principal already exists.
        CosignPrincipal principal = getCosignPrincipal();
        if (principal == null) {
            throw new IllegalStateException("CosignPrincipal does not exist.");
        }
        request = new AuthFilterRequestWrapper(request, principal, "CoSign");
    }

    /**
     * This method returns the HttpServletResponse of the current user.
     */
    public HttpServletResponse getResponse() {
        return response;
    }

    /**
     * This method returns the HttpServletRequest of the current user.  This
     * HttpServletRequest might be the wrapped version created by
     * handleSuccessfulLogin.
     */
    public HttpServletRequest getRequest() {
        return request;
    }

    /**
     * This method processes all the callbacks of CosignLoginModule and
     * provides access to the cookie name, cookie value, and ip addr.
     * @see edu.umich.auth.ServletCallbackHandler#handle( Callback[] )
     */
    public void handle(Callback[] callbacks) throws IOException,
            UnsupportedCallbackException {
        ServiceConfig serviceConfig = CosignConfig.INSTANCE.
                                              hasServiceOveride(this.
                        currentPath, this.resource, this.queryString);

        if(serviceConfig == null)
            log.debug("Service config is null.");
        else
            log.debug("Service config found is: " + serviceConfig.getName());
        for (int i = 0; i < callbacks.length; i++) {
            TextInputCallback inputCallback;
            FactorInputCallBack factorCallBack;
            String callbackCode;

            if (callbacks[i] instanceof TextInputCallback) {
                inputCallback = (TextInputCallback) callbacks[i];
                callbackCode = inputCallback.getPrompt();

                if (callbackCode.equals(CosignLoginModule.COOKIE_VALUE_IN_CODE)) {
                    Cookie[] cookies = request.getCookies();

                    // No cookies...
                    if (cookies == null) {
                        return;
                    }

                    // Find the cosign service cookie.
                    final String cookieName = getCookieName(serviceConfig);
                    for (int j = 0; j < cookies.length; j++) {
                        if (cookies[j].getName().equals(cookieName)) {
                            inputCallback.setText(cookies[j].getValue());
                            break;
                        }
                    }

                } else if (callbackCode.equals(CosignLoginModule.
                                               COOKIE_NAME_IN_CODE)) {
                    inputCallback.setText(getCookieName(serviceConfig));

                } else if (callbackCode.equals(CosignLoginModule.
                                               PROXY_IN_CODE )) {
                    if(serviceConfig.isDoProxies())
                        inputCallback.setText("true");
                    else
                        inputCallback.setText("false");
                }
                else if (callbackCode.equals(CosignLoginModule.
                                               IP_ADDR_IN_CODE)) {
                    inputCallback.setText(request.getRemoteAddr());

                }
                else {
                    throw new UnsupportedCallbackException(callbacks[i],
                            "Unrecognized text callback request.");
                }
            } else if (callbacks[i] instanceof FactorInputCallBack) {

                factorCallBack = (FactorInputCallBack) callbacks[i];
                if (!(serviceConfig == null)) {
                    factorCallBack.setFactors(serviceConfig.getFactors());
                }

            } else {
                throw new UnsupportedCallbackException(callbacks[i],
                        "Unrecognized callback type.");
            }
        }
    }

    /**
     * This method attempts to retrieve the CosignPrincipal from the
     * current Subject object.
     * @return  CosignPrincipal Active CosignPrincipal or null if
     *    not found.
     */
    private CosignPrincipal getCosignPrincipal() {
        // Check if a principal already exists.
        Iterator iterator = subject.getPrincipals().iterator();
        Object object;
        CosignPrincipal principal = null;

        while (iterator.hasNext()) {
            object = iterator.next();
            if (object instanceof CosignPrincipal) {
                principal = (CosignPrincipal) object;
                break;
            }
        }
        return principal;
    }

    /**
     * This method constructs a cookie name from the ServiceName config property.
     */

    private String getCookieName(ServiceConfig serviceConfig) {
         String cookieName;
        if(serviceConfig != null)
            cookieName = serviceConfig.getName();
        else
            cookieName = (String) CosignConfig.INSTANCE
                             .getPropertyValueinContext(CosignConfig.
                SERVICE_NAME, this.currentPath, this.resource, this.queryString);
        if ((cookieName.startsWith(COOKIE_NAME_PREFIX)) &&
            (COOKIE_NAME_PREFIX.length() < cookieName.length())) {
            return cookieName;
        }
        return COOKIE_NAME_PREFIX + cookieName;
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
package edu.umich.auth.cosign;

import java.util.Map;
import java.util.Iterator;

import javax.security.auth.Subject;
import javax.security.auth.callback.Callback;
import javax.security.auth.callback.CallbackHandler;
import javax.security.auth.callback.TextInputCallback;

import javax.security.auth.login.LoginException;
import javax.security.auth.login.FailedLoginException;
import javax.security.auth.spi.LoginModule;

import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;

import edu.umich.auth.cosign.pool.CosignConnectionList;
import edu.umich.auth.cosign.pool.CosignConnection;
import edu.umich.auth.cosign.pool.CosignConnectionPool;
import java.util.Vector;
import edu.umich.auth.ServletCallbackHandler;
import edu.umich.auth.cosign.util.FactorInputCallBack;
import edu.umich.auth.cosign.util.ServiceConfig;
import java.util.Enumeration;
import javax.security.auth.kerberos.KerberosPrincipal;

/**
 * A JAAS <code>LoginModule</code> for Cosign authentication.
 *
 * @see javax.security.auth.spi.LoginModule
 * @author dillaman
 * @author patkm
 */
public class CosignLoginModule implements LoginModule {

    // JAAS AppConfigurationEntry options
    public static final String COSIGN_CONFIG_FILE_OPTION = "cosignConfigFile";
    public static final String COSIGN_FINE_CONFIG_FILE_OPTION =
            "cosignFineConfigFile";
    // Codes for the information sent to the callback handler
    // via callback objects.
    public static final String COOKIE_NAME_IN_CODE = "CosignGetCookieName";
    public static final String COOKIE_VALUE_IN_CODE = "CosignGetCookieValue";
    public static final String IP_ADDR_IN_CODE = "CosignGetIpAddr";
    public static final String PROXY_IN_CODE = "CosignGetProxy";

    private boolean initError = false;
    private boolean cosignServerCheckSkipped = false;

    private CallbackHandler callbackHandler = null;
    private Subject subject;
    private CosignPrincipal userPrincipal = null;
    private CosignPrincipal serverPrincipal = null;

    private int cosignCode = CosignConnection.COSIGN_CODE_UNKNOWN;

    // Callbacks sent to the callback handler.
    private TextInputCallback cookieNameIn = new TextInputCallback(
            COOKIE_NAME_IN_CODE);
    private TextInputCallback cookieValueIn = new TextInputCallback(
            COOKIE_VALUE_IN_CODE);
    private TextInputCallback ipAddrIn = new TextInputCallback(IP_ADDR_IN_CODE);
    private TextInputCallback proxyValueIn = new TextInputCallback(PROXY_IN_CODE);
    private FactorInputCallBack factorsCb = new FactorInputCallBack();
    // Used for logging info and error messages
    private Log log = LogFactory.getLog(CosignLoginModule.class);

    /**
     * Initialize the module, ensuring that appropriate
     * <code>CallbackHandler</code> is passed in.
     *
     * @see javax.security.auth.spi.LoginModule#initialize(Subject, CallbackHandler, Map, Map)
     */
    public void initialize(Subject subject, CallbackHandler callbackHandler,
                           Map arg2, Map arg3) {

        // Insure that we have a CallbackHandler.
        if (callbackHandler != null) {
            this.callbackHandler = callbackHandler;
            initError = false;
        } else {
            initError = true;
        }

        this.subject = subject;

    }

    /**
     * The method used to log the user in.  Please refer to the
     * <code>LoginModule</code> API specification for details concerning
     * the return and exceptions.
     *
     * @see javax.security.auth.spi.LoginModule#login()
     */
    public boolean login() throws LoginException {

        // Do a quick check to make sure that the configuration is valid
        if (!CosignConfig.INSTANCE.isConfigValid()) {
            throw new LoginException(
                    "Initialization Error: Invalid configuration state.");
        }

        // Error if no CallbackHandler.
        if (initError) {
            throw new LoginException(
                    "Initialization Error: CallbackHandler required.");
        }

        // Try to retrieve info from callbackHandler.
        try {
            Callback[] inCallbacks = new Callback[] {cookieNameIn,
                                     cookieValueIn, ipAddrIn, proxyValueIn, factorsCb};
            callbackHandler.handle(inCallbacks);
        } catch (Exception ex) {
            throw new LoginException("Callback handler does not have the proper information, or could not be accessed.");
        }

        // Retrieve the user's cosign cookie and ip addr
        String cookieName = cookieNameIn.getText();
        String cookieValue = cookieValueIn.getText();
        String ipAddr = ipAddrIn.getText();
        String proxyValue = proxyValueIn.getText();

        // If cookie is invalid, we need to restrict service access.
        CosignCookie cosignCookie = CosignCookie.parseCosignCookie(cookieValue);
        if (cosignCookie == null) {
            throw new FailedLoginException(
                    "The client's service cookie does not exist or is not valid.");
        }

        // Check the timestamp on the Cosign Cookie.  If the timestamp is expired,
        // we need to fail the login so that a new cookie is issued.
        final long cookieExpireMillis = ((Integer) CosignConfig.INSTANCE
                                         .getPropertyValue(CosignConfig.
                COOKIE_EXPIRE_SECS)).intValue() * 1000;
        if (System.currentTimeMillis() - cosignCookie.getTimestamp() >=
            cookieExpireMillis) {
            throw new FailedLoginException(
                    "The client's service cookie has expired.");
        }

        // Check if a principal already exists.
        Iterator iterator = subject.getPrincipals().iterator();
        while (iterator.hasNext()) {
            Object object = iterator.next();
            if (object instanceof CosignPrincipal) {
                userPrincipal = (CosignPrincipal) object;
                break;
            }
        }

        // 'principal' is null if this is a first login.
        final boolean checkClientIP = ((Boolean) CosignConfig.INSTANCE
                                       .getPropertyValue(CosignConfig.
                CHECK_CLIENT_IP)).booleanValue();
        if (userPrincipal != null) {
            if (checkClientIP && !ipAddr.equals(userPrincipal.getAddress())) {
                throw new FailedLoginException(
                        "The client's IP address has changed.");
            }

            // If the locally cached cookie is not expired, we don't need to check the CoSign server
            final long cookieCacheExpireMillis = ((Integer) CosignConfig.
                                                  INSTANCE
                                                  .getPropertyValue(
                    CosignConfig.COOKIE_CACHE_EXPIRE_SECS)).intValue() * 1000;
            if ((System.currentTimeMillis() - userPrincipal.getTimestamp()) <
                cookieCacheExpireMillis) {
                if (log.isDebugEnabled()) {
                    log.debug(
                            "The client's cookie is still cached ... not performing validation.");
                }
                cosignServerCheckSkipped = true;
                return true;
            }
        }

        // Grab a connection list from the pool
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

        String cosignResponse = cosignConnectionList.checkCookie(cookieName,
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
            throw new LoginException(
                    "No cosignd servers available for authentication.");
        } else if (cosignCode != CosignConnection.COSIGN_USER_AUTHENTICATED) {
            throw new FailedLoginException("User not authenticated to Cosign.");
        }

        // Attempt to parse the response from the cosignd server
        try {
            serverPrincipal = new CosignPrincipal(cosignResponse);
        } catch (Exception e) {
            throw new FailedLoginException(
                    "Cosignd server returned invalid response.");
        }

        //check for multi factors here.  They should have been placed in the serverPriciple above

        if ((factorsCb.getFactors() != null) &&
            !(checkServiceFactors(factorsCb.getFactors(), serverPrincipal))) {
            throw new FailedLoginException(
                    "All factors for service have not been satisfied");
        }

        // The user was validated against the cosign server.  We need to check their
        // user stats as returned by the server to what we expect
        if (userPrincipal != null) {
            if (checkClientIP &&
                !serverPrincipal.getAddress().equals(userPrincipal.getAddress())) {
                throw new FailedLoginException(
                        "Server and client disagree about client's IP address");
            }
            if (!serverPrincipal.getName().equals(userPrincipal.getName())) {
                throw new FailedLoginException(
                        "Server and client disagree about client's name");
            }
            if (!serverPrincipal.getRealm().equals(userPrincipal.getRealm())) {
                log.info("Server and client disagree about client's realm");
            }
        }

        if (((Boolean) CosignConfig.INSTANCE.getPropertyValue(CosignConfig.
                KERBEROS_GET_TICKETS)).booleanValue()) {
            try {
                boolean result = getKerbTgtTicket(cookieName, cosignCookie);
                if(result){

                }

            } catch (Exception e) {
                //do something here fool
            }
        }

        if (proxyValue.equalsIgnoreCase("true")) {
           try {
               boolean result = getCosignProxyTickets(cookieName, cosignCookie);
           } catch (Exception e) {
               //do something here fool
           }
       }

        return true;
    }

    /**
     * @see javax.security.auth.spi.LoginModule#commit()
     */
    public boolean commit() throws LoginException {

        // Since we might have bailed out early in the Login method due to the
        // cookie being cached, we need to bail out of this function early.
        if (cosignServerCheckSkipped) {
            return true;
        }

        // If we checked the user's cookie w/the Cosign server, and they're authenticated,
        // assign credentials from server (name, ip, realm) and update the timestamp.
        if ((cosignCode == CosignConnection.COSIGN_USER_AUTHENTICATED) &&
            (serverPrincipal != null)) {

            // The subject didn't have an appropriate principal
            // (first-time login), so create one.
            if (userPrincipal == null) {
                userPrincipal = new CosignPrincipal();
                subject.getPrincipals().add(userPrincipal);
            }

            // Parse the cosign response string.
            userPrincipal.setAddress(serverPrincipal.getAddress());
            userPrincipal.setName(serverPrincipal.getName());
            userPrincipal.setRealm(serverPrincipal.getRealm());
            userPrincipal.setTimestamp(serverPrincipal.getTimestamp());
            userPrincipal.setFactors(serverPrincipal.getFactors());

        } else {
            throw new IllegalStateException();
        }

        return true;
    }

    /**
     * @see javax.security.auth.spi.LoginModule#abort()
     */
    public boolean abort() throws LoginException {
        // Reset module state.
        userPrincipal = null;
        serverPrincipal = null;
        cosignCode = CosignConnection.COSIGN_CODE_UNKNOWN;
        cosignServerCheckSkipped = false;
        return true;
    }

    /**
     * @see javax.security.auth.spi.LoginModule#logout()
     */
    public boolean logout() throws LoginException {
        throw new LoginException("Method not supported.");
    }

    /**
     * This method checks required factors with factors that have been authorized
     * from the server.
     * @return  true if all factors required have been satisfied, False if not.
     *
     */
    public boolean checkServiceFactors(Vector factors,
                                       CosignPrincipal principle) {
        String ignoreFactorSuffix = (String) CosignConfig.INSTANCE.
                                    getPropertyValue(CosignConfig.
                COSIGN_FACTOR_SUFFIX_IGNORE);
        String factorSuffix = (String) CosignConfig.INSTANCE.getPropertyValue(
                CosignConfig.COSIGN_FACTOR_SUFFIX);
        Boolean ignore = new Boolean(ignoreFactorSuffix);

        if (factors.size() > 0) {
            Enumeration enuS = factors.elements();
            String curServiceFactor;
            while (enuS.hasMoreElements()) {
                curServiceFactor = (String) enuS.nextElement();
                Enumeration enuF = principle.getFactors().elements();
                String curPricipleFactor;
                boolean factorFound = false;
                while (enuF.hasMoreElements()) {
                    curPricipleFactor = stripIgnoreFactor((String) enuF.
                            nextElement(),
                            ignore.booleanValue(), factorSuffix);
                    if (curServiceFactor.equalsIgnoreCase(curPricipleFactor)) {
                        factorFound = true;
                    }
                }
                if (!factorFound) {
                    return false;
                }
            }
        }
        return true;
    }


    private String stripIgnoreFactor(String factor, boolean shouldIgnore,
                                     String ignore) {
        if (!shouldIgnore) {
            return factor;
        }
        if (ignore == null) {
            return factor;
        }
        if (factor.endsWith(ignore)) {
            return factor.substring(0, factor.indexOf(ignore));
        }
        return factor;

    }

    private boolean getKerbTgtTicket(String cookieName,
                                     CosignCookie cosignCookie) throws
            Exception {
        String tgtResponse = null;
        KerberosPrincipal kerbPrinciple = null;
        try {
            int count=0;
            // Check if a tgt/kerb5priciple already exists.
            Iterator iterator2 = this.subject.getPrincipals().iterator();
            log.info("going into iterator");
            while (iterator2.hasNext()) {
                count++;
                Object object = iterator2.next();
                log.info("object connical name is: " + object.getClass().getCanonicalName());
                if (object instanceof KerberosPrincipal) {
                    kerbPrinciple = (KerberosPrincipal) object;
                    log.info("kerb principle found");
                    break;
                }
            }
            log.info("Kerberos iterator count: " + count);

        } catch (Exception ex) {

        }

        if (kerbPrinciple != null) {
            return true;
        }

        // Grab a connection list from the pool
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
        try {
            tgtResponse = cosignConnectionList.retreiveTGT(cookieName,
                    cosignCookie.getNonce(), subject, serverPrincipal);
        } catch (Exception e) {
            throw new FailedLoginException(
                    "Cosignd server failed kerberos tgt.");
        }

        // Return the connection list back to the pool
        try {
            CosignConnectionPool.INSTANCE.returnCosignConnectionList(
                    cosignConnectionList);
        } catch (Exception e) {
            log.error("Failed to return cosign connections to pool.");
        }

        cosignCode = CosignConnection.convertResponseToCode(tgtResponse);
        // Translate server response to boolean return or exception.
        // NOTE: No false return since that would tell LoginContext to ignore this module.
        if (tgtResponse == null) {
            throw new LoginException(
                    "No cosignd servers available for authentication.");
        } else if (cosignCode != CosignConnection.COSIGN_USER_AUTHENTICATED) {
            throw new FailedLoginException("User not authenticated to Cosign.");
        }
        return true;
    }

    private boolean getCosignProxyTickets(String cookieName,
                                     CosignCookie cosignCookie) throws
            Exception {
        // Grab a connection list from the pool
                CosignConnectionList cosignConnectionList;
                String response;
                try {
                    cosignConnectionList = CosignConnectionPool.INSTANCE.
                                           borrowCosignConnectionList();
                } catch (Exception e) {
                    throw new LoginException(
                            "Failed to borrow cosign connections from pool.");
                }

                // Keep trying until we get a server which will serve us,
                // or there are no servers available in the pool.
                try {
                    response = cosignConnectionList.retreiveProxyCookie(cookieName,
                            cosignCookie.getNonce(), subject, serverPrincipal);
                } catch (Exception e) {
                    throw new FailedLoginException(
                            "Cosignd server failed kerberos proxies.");
                }

                // Return the connection list back to the pool
                try {
                    CosignConnectionPool.INSTANCE.returnCosignConnectionList(
                            cosignConnectionList);
                } catch (Exception e) {
                    log.error("Failed to return cosign connections to pool.");
                }

                cosignCode = CosignConnection.convertResponseToCode(response);
                // Translate server response to boolean return or exception.
                // NOTE: No false return since that would tell LoginContext to ignore this module.
                if (response == null) {
                    throw new LoginException(
                            "No cosignd servers available for authentication.");
                } else if (cosignCode != CosignConnection.COSIGN_USER_AUTHENTICATED) {
                    throw new FailedLoginException("User not authenticated to Cosign.");
                }
        return true;

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


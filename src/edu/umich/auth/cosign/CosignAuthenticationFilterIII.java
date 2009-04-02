package edu.umich.auth.cosign;

import edu.umich.auth.ServletCallbackHandler;
import edu.umich.auth.cosign.util.ServiceConfig;

import java.io.File;
import java.io.IOException;

import java.util.*;
import java.util.HashMap;

import javax.security.auth.login.AppConfigurationEntry;
import javax.security.auth.login.Configuration;

import javax.servlet.FilterChain;
import javax.servlet.FilterConfig;
import javax.servlet.ServletException;
import javax.servlet.ServletRequest;
import javax.servlet.ServletResponse;
import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;
import java.util.regex.Pattern;
import java.util.regex.Matcher;
import javax.security.auth.Subject;
import javax.security.auth.login.LoginContext;
import javax.security.auth.login.LoginException;
import javax.servlet.Filter;
import org.apache.commons.logging.LogFactory;
import org.apache.commons.logging.Log;

/**
 * This class extends AuthenticationFilter in order to provide a built-in
 * JAAS configuration for CosignServletCallbackHandler and CosignLoginModule.
 * @author dillaman
 */
public class CosignAuthenticationFilterIII implements Filter {
    // Configuration parameter names (from web.xml).

    public static final String COSIGN_CONFIG_INIT_PARAM =
            "Cosign.ConfigurationFile";
    public static final String COSIGN_FINE_CONFIG_INIT_PARAM =
            "Cosign.FineConfigurationFile";
    // Parameter required in the Java Runtime specifying the location
    // of the JAAS configuration file.  The location of the file is determined by
    // the value of the JAAS_CONFIG_FILE INIT_PARAM parameter.
    private static final String JAAS_CONFIG_PROPERTY =
            "java.security.auth.login.config";
    private File jaasFile;
    // Psuedo-unique JAAS configuration name. We don't want a collision with a
    // name that is in a "jaas.conf" file.
    private static final String COSIGN_APP_CONFIG_ENTRY_NAME =
            "edu.umich.auth.cosign.CosignAuthenticationFilter:JAAS";
    public static final String JAAS_CONFIG_FILE_INIT_PARAM =
            "Auth.JAASConfigurationFile";

    // The name of the subject that's stored in the user's session.
    private static final String USER_SUBJECT_ATTRIBUTE =
            "edu.umich.auth.AuthentincatonFilter:Subject";
    private String cosignConfigFile;
    protected Log log = LogFactory.getLog(this.getClass());

    // Class variables configured in the init method.
    private String appConfigurationEntryName;
    private Class callbackHandlerClass;
    private boolean isConfigValid = false;
    private Map parameters = new HashMap();

    /**
     * @see javax.servlet.Filter#init(javax.servlet.FilterConfig)
     */
    public void init(FilterConfig filterConfig) {
        try {
            // Retrieve the path to the cosign config XML file so that it
            // may be passed as an option to the CosignLoginModule
            cosignConfigFile = filterConfig.getInitParameter(
                    COSIGN_CONFIG_INIT_PARAM);
            log.info(
                    "CosignAuthenticationFilter: Config file init parameter is: " +
                    cosignConfigFile);

            // Notify our parent class of the name of the JAAS config entry and
            // the default ServletCallbackHandler
            log.info(
                    "CosignAuthenticationFilter: COSIGN_APP_CONFIG_ENTRY_NAME is: " +
                    COSIGN_APP_CONFIG_ENTRY_NAME);
            jaasFile = new File(filterConfig.getInitParameter(
                    JAAS_CONFIG_FILE_INIT_PARAM));

            if (!jaasFile.exists()) {
                throw new ServletException(
                        "Cannot find JAAS configuration file " +
                        filterConfig.getInitParameter(
                        JAAS_CONFIG_FILE_INIT_PARAM) + ".");
            }

            if (!jaasFile.canRead()) {
                throw new ServletException(
                        "Cannot read JAAS configuration file " +
                        jaasFile.getAbsolutePath() + ".");
            }

            // Point security system to the JAAS configuration file.
            // NOTE: Nothing is stopping this from being overwritten elsewhere.
            //       It's being checked before we use the LoginContext, but still...
            System.setProperty(JAAS_CONFIG_PROPERTY, jaasFile.getAbsolutePath());

            setJAASAppConfigurationEntryName(COSIGN_APP_CONFIG_ENTRY_NAME);
            setJAASServletCallbackHandler(CosignServletCallbackHandler.class);
            this.isConfigValid = true;
        } catch (Exception ex) {
            log.error("Failed to init AuthenticationFilter!", ex);
            this.isConfigValid = false;

        }
    }

    /**
     * The method sets the JAAS App Configuration Entry name that will be used
     * to lookup the appropriate JAAS LoginModule
     */
    protected void setJAASAppConfigurationEntryName(String appConfigurationEntryName) {
        this.appConfigurationEntryName = appConfigurationEntryName;
    }

    /**
     * This method sets the ServletCallbackHandler that will be given
     * to the JAAS LoginModule
     */
    protected void setJAASServletCallbackHandler(Class callbackHandlerClass) {
        this.callbackHandlerClass = callbackHandlerClass;
    }

    /**
     * @see javax.servlet.Filter#destroy()
     */
    public void destroy() {
    }

    /**
     * This method will inspect the JAAS configuration object to see if the
     * CosignLoginModule is already setup.  If it wasn't setup, a new
     * Configuration is create for CosignLoginModule.  If it was setup,
     * we want to validate that it is using the same configuration file
     * that we are using.
     */
    protected void validateFilter() throws ServletException {
        log.info("CosignAuthenticationFilter: validateFilter called");
        log.info("CosignAuthenticationFilter: Filter configuration file: " +
                cosignConfigFile);
        if (!this.isConfigValid) {
            throw new ServletException(
                    "AuthorizationFilter failed to initialize.");
        }

        // Insure that the JAAS configuration property hasn't been overwritten.
        // Synchronization may be worthwhile between here and the login call.
        if (!System.getProperty(JAAS_CONFIG_PROPERTY).equals(jaasFile.getAbsolutePath())) {
            throw new ServletException(
                    "JAAS configuration file system property has been overwritten.\n" +
                    "NOTE: All Web applications configured to use JAAS must share the same JAAS configuration file.");
        }

        // Attempt to locate the JAAS app config for this filter.  If one doesn't exist,
        // this must be the first instance of the CosignAuthFilter so we will need
        // to create a new configuration
        final Configuration currentConfiguration = Configuration.getConfiguration();
        final AppConfigurationEntry[] cosignAppConfigurationEntries =
                currentConfiguration.getAppConfigurationEntry(
                COSIGN_APP_CONFIG_ENTRY_NAME);

        if (cosignAppConfigurationEntries == null) {
            log.info(
                    "CosignAuthenticationFilter: cosignAppConfigurationEntries was null");

            final CosignAppConfigurationEntry[] newCosignAppConfigurationEntries = new CosignAppConfigurationEntry[]{
                new CosignAppConfigurationEntry(cosignConfigFile)
            };

            // Wrap the existing Configuration with one that will return
            // our AppConfigurationEntry when requested
            Configuration.setConfiguration(new Configuration() {

                public AppConfigurationEntry[] getAppConfigurationEntry(String entryName) {
                    if (COSIGN_APP_CONFIG_ENTRY_NAME.equals(entryName)) {
                        return newCosignAppConfigurationEntries;
                    }

                    return currentConfiguration.getAppConfigurationEntry(
                            entryName);
                }

                public void refresh() {
                    currentConfiguration.refresh();
                }
            });

            // Load our configuration file into the CosignConfig singleton
            CosignConfig.INSTANCE.setConfigFilePath(cosignConfigFile);
        } else {
            // Verify that the CosignLoginModule configuration is using the same configuration
            // that we want it to use.  If not, throw an error since only one CosignConfig is
            // possible within a single JVM (for now, at least).
            log.info(
                    "CosignAuthenticationFilter: cosignAppConfigurationEntries was NOT null");

            /*
            String cosignConfigFile = (String) cosignAppConfigurationEntries[0].
            getOptions().get(
            CosignLoginModule.
            COSIGN_CONFIG_FILE_OPTION);
             */
            if ((cosignAppConfigurationEntries.length == 0) ||
                    (!(cosignAppConfigurationEntries[0] instanceof CosignAppConfigurationEntry))) {
                log.info(
                        "CosignAuthenticationFilter: We hit the error");
                log.info(
                        "CosignAuthenticationFilter: cosignAppConfigurationEntries.length is: " +
                        cosignAppConfigurationEntries.length);

                log.info(
                        "CosignAuthenticationFilter: cosignAppConfigurationEntry is correct class? " +
                        (cosignAppConfigurationEntries[0] instanceof CosignAppConfigurationEntry));
                log.info(
                        "CosignAuthenticationFilter: cosignAppConfigurationEntry class is: " +
                        (cosignAppConfigurationEntries[0].getClass()));
                log.info(
                        "CosignAuthenticationFilter: cosignAppConfigurationEntry  name is: " +
                        (cosignAppConfigurationEntries[0].getLoginModuleName()));
                log.info(
                        "CosignAuthenticationFilter: cosignAppConfigurationEntry  control flag is: " +
                        (cosignAppConfigurationEntries[0].getControlFlag()));
                log.info(
                        "CosignAuthenticationFilter: cosignAppConfigurationEntry  options are: " +
                        (cosignAppConfigurationEntries[0].getOptions()));

                Iterator myIter = cosignAppConfigurationEntries[0].getOptions().
                        values().iterator();

                while (myIter.hasNext()) {
                    log.info("config value= : " + myIter.next());
                }

                // Load our configuration file into the CosignConfig singleton
                if (CosignConfig.INSTANCE.getConfigFilePath() == null) {
                    CosignConfig.INSTANCE.setConfigFilePath(cosignConfigFile);
                }

            /*
            throw new ServletException(

            "Possible duplicate JAAS app configuration name detected: " +
            COSIGN_APP_CONFIG_ENTRY_NAME + ". " +
            "Only one JAAS app configuration of that name is possible for each instance of a JVM.");
             */
            } else if ((cosignConfigFile == null) ||
                    (!cosignConfigFile.equals(this.cosignConfigFile))) {
                throw new ServletException(
                        "Cosign config file path is different than expected. " +
                        "Only one CosignConfig file is possible for each instance of a JVM.");
            }
        }

        // Ensure that the CosignConfig is valid before proceeding with
        // the authorization attempt
        if (!CosignConfig.INSTANCE.isConfigValid()) {
            throw new ServletException(
                    "Cosign config is invalid.  Check the server logs for more details.");
        }
    }

    public void doFilter(ServletRequest request, ServletResponse response,
            FilterChain filterChain) throws IOException {
        HttpServletRequest httpRequest = (HttpServletRequest) request;
        HttpServletResponse httpResponse = (HttpServletResponse) response;
        ServiceConfig serviceConfig;
        String currentPath = "";
        String currentReqUrl = httpRequest.getRequestURI();

        try {
            // Ensure that the filter and JAAS is configured properly
            validateFilter();

            currentPath = currentReqUrl.substring(httpRequest.getContextPath().
                    length());

            String resource = currentPath.substring(currentPath.lastIndexOf('/') +
                    1);

            if (currentPath.charAt(currentPath.length() - 1) != '/') {
                currentPath = currentPath.substring(0,
                        currentPath.lastIndexOf('/') + 1);
            }

            String locationUrl = (String) CosignConfig.INSTANCE.getPropertyValue(
                    CosignConfig.LOCATION_HANDLER_URL);
            log.debug("Location URL: " + locationUrl);
            log.debug("Request URL: " + currentReqUrl);
            if (currentReqUrl.equalsIgnoreCase(locationUrl)) {
                log.debug("Location URL and current url match");
                StringTokenizer strtok = new StringTokenizer(httpRequest.getQueryString(), "&");
                String cookie = strtok.nextToken();
                String[] sp = cookie.split("=");
                String[] sq = sp[0].split("-");
                String cookieName = sp[0];
                serviceConfig = CosignConfig.INSTANCE.matchServiceWithName(
                        cookieName);
                log.debug("Cookie name is: " + cookieName);
                if (serviceConfig != null) {
                    String reDirect = strtok.nextToken();
                    String reDirRegEx = (String) CosignConfig.INSTANCE.getPropertyValue(CosignConfig.REDIRECT_REGEX);
                    Pattern pattern = Pattern.compile(reDirRegEx);
                    Matcher m = pattern.matcher(reDirect);
                    if (m.matches()) {
                        CosignLocationHandler handler = new CosignLocationHandler();
                        if (handler.check(sp[1], serviceConfig, reDirect, httpResponse)) {
                            log.debug("Location handler checked ok");
                            httpResponse.sendRedirect(reDirect);
                            return;
                        }
                    } else {

                        if (CosignConfig.INSTANCE.getPropertyValue(CosignConfig.VALIDATION_ERROR_REDIRECT) != null) {
                            httpResponse.sendRedirect((String) CosignConfig.INSTANCE.getPropertyValue(CosignConfig.VALIDATION_ERROR_REDIRECT));
                        } else {
                            throw new ServletException("Redirect URL does not match redirection configuration Regular Expression.");
                        }
                    }

                }
            }


            serviceConfig = CosignConfig.INSTANCE.hasServiceOveride(currentPath,
                    resource, httpRequest.getQueryString());

            if ((serviceConfig != null) && serviceConfig.isPublicAccess()) {
                log.debug("Anonymous user permitted access to site.");

                // User authenticated; continue request processing.
                filterChain.doFilter(request, response);
            }

            if (serviceConfig == null) {
                log.error("Cosign filter defined to pickup URL but no service defined.");
                throw new ServletException(
                        "Cosign filter defined to pickup URL but no service defined.");

            }

            // Get, or instantiate, the a Subject for user Principals and Credentials.
            Subject subject = null;
            Object object = httpRequest.getSession().getAttribute(
                    USER_SUBJECT_ATTRIBUTE);

            if (object == null) {
                httpRequest.getSession().setAttribute(USER_SUBJECT_ATTRIBUTE,
                        subject = new Subject());
            } else if (object instanceof Subject) {
                subject = (Subject) object;
            } else {
                throw new ServletException(
                        "Invalid authentication Subject in user's session.");
            }

            ServletCallbackHandler callbackHandler = null;

            try {
                // Create a callback handler of the type specified in the filter configuration.
                callbackHandler = (ServletCallbackHandler) callbackHandlerClass.newInstance();

                // Attempt to log the user in if the callback handler initializes.
                if (callbackHandler.init(parameters, httpRequest, httpResponse,
                        subject)) {
                    LoginContext loginContext = new LoginContext(
                            appConfigurationEntryName, subject, callbackHandler);
                    loginContext.login();
                }
                callbackHandler.handleSuccessfulLogin();

            } catch (LoginException ex) {
                if (!callbackHandler.handleFailedLogin(ex)) {
                    return;
                }
            }

            // User authenticated; continue request processing.
            filterChain.doFilter(callbackHandler.getRequest(),
                    callbackHandler.getResponse());

        } catch (Exception ex) {
            // log the error and give the user a "503" HTTP error
            log.error(ex.getMessage(), ex);
            httpResponse.sendError(HttpServletResponse.SC_SERVICE_UNAVAILABLE,
                    ex.getMessage());
        }
    }

    /********************************************************************************
     * This class provide a wrapper for the necessary JAAS app config entry
     ********************************************************************************/
    private static class CosignAppConfigurationEntry extends AppConfigurationEntry {

        /**
         * Constructor for JAAS cosign app configuration entry
         */
        protected Log log = LogFactory.getLog(CosignAppConfigurationEntry.class);

        public CosignAppConfigurationEntry(String cosignConfigFile) {
            super(CosignLoginModule.class.getName(),
                    AppConfigurationEntry.LoginModuleControlFlag.REQUIRED,
                    createOptions(cosignConfigFile));
            log.info(
                    "CosignAppConfigurationEntry: Config file init parameter is: " +
                    cosignConfigFile);
        }

        /**
         * Create a Map which contains the cosignConfigFile
         */
        private static HashMap createOptions(String cosignConfigFile) {
            HashMap options = new HashMap();
            options.put(CosignLoginModule.COSIGN_CONFIG_FILE_OPTION,
                    cosignConfigFile);
            System.out.println("CosignAppConfigurationEntry: create options: " +
                    options);

            return options;
        }
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

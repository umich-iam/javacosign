package edu.umich.auth.cosign;

import java.util.HashMap;

import javax.security.auth.login.AppConfigurationEntry;
import javax.security.auth.login.Configuration;
import javax.servlet.FilterConfig;
import javax.servlet.ServletException;

import edu.umich.auth.AuthenticationFilter;
import edu.umich.auth.cosign.util.ServiceConfig;
import javax.servlet.ServletResponse;
import javax.servlet.ServletRequest;
import java.io.IOException;
import javax.servlet.FilterChain;
import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;

/**
 * This class extends AuthenticationFilter in order to provide a built-in
 * JAAS configuration for CosignServletCallbackHandler and CosignLoginModule.
 * @author dillaman
 */
public class CosignAuthenticationFilter extends AuthenticationFilter {

    // Configuration parameter names (from web.xml).
    public static final String COSIGN_CONFIG_INIT_PARAM =
            "Cosign.ConfigurationFile";
    public static final String COSIGN_FINE_CONFIG_INIT_PARAM =
            "Cosign.FineConfigurationFile";
    // Psuedo-unique JAAS configuration name. We don't want a collision with a
    // name that is in a "jaas.conf" file.
    private static final String COSIGN_APP_CONFIG_ENTRY_NAME =
            "edu.umich.auth.cosign.CosignAuthenticationFilter:JAAS";

    private String cosignConfigFile;


    /********************************************************************************
     * This class provide a wrapper for the necessary JAAS app config entry
     ********************************************************************************/
    private static class CosignAppConfigurationEntry extends
            AppConfigurationEntry {

        /**
         * Constructor for JAAS cosign app configuration entry
         */
        public CosignAppConfigurationEntry(String cosignConfigFile) {
            super(CosignLoginModule.class.getName(),
                  AppConfigurationEntry.LoginModuleControlFlag.REQUIRED,
                  createOptions(cosignConfigFile));
        }

        /**
         * Create a Map which contains the cosignConfigFile
         */
        private static HashMap createOptions(String cosignConfigFile) {
            HashMap options = new HashMap();
            options.put(CosignLoginModule.COSIGN_CONFIG_FILE_OPTION,
                        cosignConfigFile);

            return options;
        }

    }


    /**
     * @see javax.servlet.Filter#init(javax.servlet.FilterConfig)
     */
    public void init(FilterConfig filterConfig) {

        // Retrieve the path to the cosign config XML file so that it
        // may be passed as an option to the CosignLoginModule
        cosignConfigFile = filterConfig.getInitParameter(
                COSIGN_CONFIG_INIT_PARAM);

        // Notify our parent class of the name of the JAAS config entry and
        // the default ServletCallbackHandler
        super.setJAASAppConfigurationEntryName(COSIGN_APP_CONFIG_ENTRY_NAME);
        super.setJAASServletCallbackHandler(CosignServletCallbackHandler.class);

    }

    /**
     * This method will inspect the JAAS configuration object to see if the
     * CosignLoginModule is already setup.  If it wasn't setup, a new
     * Configuration is create for CosignLoginModule.  If it was setup,
     * we want to validate that it is using the same configuration file
     * that we are using.
     */
    protected void validateFilter() throws ServletException {

        // Attempt to locate the JAAS app config for this filter.  If one doesn't exist,
        // this must be the first instance of the CosignAuthFilter so we will need
        // to create a new configuration
        final Configuration currentConfiguration = Configuration.
                getConfiguration();
        final AppConfigurationEntry[] cosignAppConfigurationEntries =
                currentConfiguration.getAppConfigurationEntry(
                        COSIGN_APP_CONFIG_ENTRY_NAME);

        if (cosignAppConfigurationEntries == null) {
            final CosignAppConfigurationEntry[]
                    newCosignAppConfigurationEntries = new
                    CosignAppConfigurationEntry[] {
                    new CosignAppConfigurationEntry(cosignConfigFile)
            };

            // Wrap the existing Configuration with one that will return
            // our AppConfigurationEntry when requested
            Configuration.setConfiguration(new Configuration() {

                public AppConfigurationEntry[] getAppConfigurationEntry(String
                        entryName) {
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
            String cosignConfigFile = (String) cosignAppConfigurationEntries[0].
                                      getOptions().get(
                                              CosignLoginModule.
                                              COSIGN_CONFIG_FILE_OPTION);
            if ((cosignAppConfigurationEntries.length == 0) ||
                (!(cosignAppConfigurationEntries[0] instanceof
                   CosignAppConfigurationEntry))) {
                throw new ServletException(
                        "Possible duplicate JAAS app configuration name detected: " +
                        COSIGN_APP_CONFIG_ENTRY_NAME + ". " +
                        "Only one JAAS app configuration of that name is possible for each instance of a JVM.");

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

        String currentPath = "";
        String currentReqUrl = httpRequest.getRequestURI();
        try {
            // Ensure that the filter and JAAS is configured properly
            validateFilter ();

            currentPath = currentReqUrl.substring(httpRequest.getContextPath().
                                                  length());

        String resource = currentPath.substring(currentPath.lastIndexOf('/') + 1);

            if (currentPath.charAt(currentPath.length() - 1) != '/') {
                currentPath = currentPath.substring(0,
                        currentPath.lastIndexOf('/') +
                        1);

            }
            ServiceConfig serviceConfig = CosignConfig.INSTANCE.
                                          hasServiceOveride(
                                                  currentPath, resource , httpRequest.getQueryString());
            if (serviceConfig != null && serviceConfig.isPublicAccess()) {
                log.debug("Anonymous user permitted access to site.");
                // User authenticated; continue request processing.
                filterChain.doFilter(request, response);
            } else {
                super.doFilter(request, response, filterChain);
            }

        } catch (Exception ex) {
            // log the error and give the user a "503" HTTP error
              log.error( ex.getMessage(), ex );
              httpResponse.sendError ( HttpServletResponse.SC_SERVICE_UNAVAILABLE, ex.getMessage() );

        }

    }
}

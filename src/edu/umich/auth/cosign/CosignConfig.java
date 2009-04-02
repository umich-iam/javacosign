package edu.umich.auth.cosign;

import java.io.*;
import java.util.*;
import javax.xml.parsers.*;

import edu.umich.auth.cosign.util.*;
import org.apache.commons.logging.*;
import org.w3c.dom.*;
import java.util.regex.Pattern;
import java.util.regex.Matcher;


/**
 * This singleton class is the in-memory configurations of the Cosign filter.
 * It reads in the Cosign configuration file and monitor any change
 * in it.
 *
 * @author dillaman
 */
public class CosignConfig {

    //  Singleton for this class
    public static final CosignConfig INSTANCE = new CosignConfig();

    // Configuration properties constants
    public static final String KEY_STORE_PATH = "KeyStorePath";
    public static final String KEY_STORE_PASSWORD = "KeyStorePassword";
    public static final String SERVICE_NAME = "ServiceName";
    public static final String SERVICES = "services";
    public static final String COSIGN_SERVER_HOST = "CosignServerHost";
    public static final String COSIGN_SERVER_PORT = "CosignServerPort";
    public static final String CONNECTION_POOL_SIZE = "ConnectionPoolSize";
    public static final String COOKIE_EXPIRE_SECS = "CookieExpireSecs";
    public static final String COOKIE_CACHE_EXPIRE_SECS =
            "CookieCacheExpireSecs";
    public static final String LOGIN_REDIRECT_URL = "LoginRedirectUrl";
    public static final String LOGIN_SITE_ENTRY_URL = "LoginSiteEntryUrl";
    public static final String LOGIN_POST_ERROR_URL = "LoginPostErrorUrl";
    public static final String ALLOW_PUBLIC_ACCESS = "AllowPublicAccess";
    public static final String COSIGN_FACTOR_SUFFIX = "CosignFactorSuffix";
    public static final String COSIGN_FACTOR_SUFFIX_IGNORE = "CosignFactorSuffixIgnore";
    public static final String COSIGN_SERVER_VERSION = "CosignServerVersion";
    public static final String HTTPS_ONLY = "HttpsOnly";
    public static final String HTTPS_PORT = "HttpsPort";
    public static final String CHECK_CLIENT_IP = "CheckClientIP";
    public static final String KERBEROS_GET_TICKETS = "KerberosGetTickets";
    public static final String KERBEROS_TICKET_CACHE_DIRECTORY = "KerberosTicketCachDirectory";
    public static final String KERBEROS_KERB5_CONF = "KerberosKrb5Conf";
    public static final String KERBEROS_KERB5_DEBUG = "KerberosKrb5Debug";
    public static final String COSIGN_GET_PROXIES = "CosignGetProxies";
    public static final String CLEAR_SESSION_ON_LOGIN = "ClearSessionOnLogin";
    public static final String CONFIG_FILE_MONITOR_INT_SECS =
            "ConfigFileMonitoringIntervalSecs";
    public static final String LOCATION_HANDLER_URL = "LocationHandlerRef";
    public static final String VALIDATION_ERROR_REDIRECT = "ValidationErrorRedirect";
     public static final String REDIRECT_REGEX = "RedirectRegex";
    public static final String COSIGN_SERVER_HOST_IP_CHECK = "CosignServerHostIpCheck";

    // List of all the properties that will be read from the XML file
    // along with their default values (if not required)
    private static final Property[] PROPERTIES = new Property[] {
                                                 new StringProperty(
            KEY_STORE_PATH),
                                                 new StringProperty(
            LOCATION_HANDLER_URL),
                                                 new StringProperty(
            VALIDATION_ERROR_REDIRECT, null),
                                                 new StringProperty(
            REDIRECT_REGEX),
                                                 new StringProperty(
            KEY_STORE_PASSWORD),
                                              /*   new StringProperty(
            SERVICE_NAME),*/
                                                 new BooleanProperty(
            SERVICES, new Boolean(true)),
                                                 new StringProperty(
            COSIGN_SERVER_HOST),
                                                 new IntegerProperty(
            COSIGN_SERVER_PORT, new Integer(6663), 0, 65535),
                                                 new IntegerProperty(
            CONNECTION_POOL_SIZE, new Integer(20), 0, Integer.MAX_VALUE),
                                                 new IntegerProperty(
            COOKIE_EXPIRE_SECS, new Integer(86400), 0, Integer.MAX_VALUE),
                                                 new IntegerProperty(
            COOKIE_CACHE_EXPIRE_SECS, new Integer(120), 0, Integer.MAX_VALUE),
                                                 new StringProperty(
            LOGIN_REDIRECT_URL),
                                                 new StringProperty(
            LOGIN_POST_ERROR_URL),
                                                 new StringProperty(
            LOGIN_SITE_ENTRY_URL, null),
                                                 new BooleanProperty(
            ALLOW_PUBLIC_ACCESS, new Boolean(false)),
                                                 new StringProperty(
            COSIGN_FACTOR_SUFFIX, null),
                                                 new StringProperty(
            COSIGN_FACTOR_SUFFIX_IGNORE, null),
                                                 new StringProperty(
            COSIGN_SERVER_VERSION, new String("")),
                                                 new BooleanProperty(HTTPS_ONLY,
            new Boolean(false)),
                                                 new BooleanProperty(KERBEROS_KERB5_DEBUG,
            new Boolean(false)),
                                                 new BooleanProperty(KERBEROS_GET_TICKETS,
            new Boolean(false)),
                                                 new StringProperty(
            KERBEROS_TICKET_CACHE_DIRECTORY, new String("")),
                                                 new StringProperty(
            KERBEROS_KERB5_CONF, new String("")),
                                                 new BooleanProperty(COSIGN_GET_PROXIES,
            new Boolean(false)),
                                                 new IntegerProperty(HTTPS_PORT,
            new Integer(443), 0, 65535),
                                                 new BooleanProperty(
            CHECK_CLIENT_IP, new Boolean(false)),
                                                 new BooleanProperty(
            CLEAR_SESSION_ON_LOGIN, new Boolean(false)),
                                                 new StringProperty(
            COSIGN_SERVER_HOST_IP_CHECK, new String("60000")), //default 1 hour ip check delay
                                                 new IntegerProperty(
            CONFIG_FILE_MONITOR_INT_SECS, new Integer(30), 5,
            Integer.MAX_VALUE / 1000)
    };

    // Last update time of the config file
    private long lastUpdate = 0;

    // How often we monitor the config file change
    private long configFileMonitoringIntervalSecs = 30;

    // Flag is true when the Cosign config is completely valid
    private boolean isConfigValid = false;

    // Current map of properties
    private HashMap propertyKeyToValue = new HashMap();

    // Monitoring thread for a config file
    private MonitoringThread monitoringThread = null;

    // Array of registered UpdateListeners
    private ArrayList updateListeners = new ArrayList();

    // Reader/writer lock to prevent software from reading our
    // properties while we are updating them
    private RWLock rwLock = new RWLock();

    // Commons Logging log instance
    private Log log = LogFactory.getLog(CosignConfig.class);

    // Map to hold override configs
    private Vector servicePaths = new Vector();

    /********************************************************************************
     * This interface provides a callback to notify registered classes whenever
     * the config file is reloaded.
     ********************************************************************************/
    public interface UpdateListener {

        /**
         * This method is called whenever the config file is reloaded by the
         * file monitoring thread.
         */
        void configUpdated();

    }


    /********************************************************************************
     * This class maps a propertyKey from the config XML file to a data-type, marks
     * it as required or optional, and gives a default value.
     ********************************************************************************/
    private abstract static class Property {

        public final String propertyKey;
        public final Object defaultValue;
        public final boolean isRequired;

        /**
         * Constructor for Property.  The specified propertyKey will be required.
         */
        public Property(String propertyKey) {
            this.propertyKey = propertyKey;
            this.isRequired = true;
            this.defaultValue = null;
        }

        /**
         * Constructor for Property.  The specified propertyKey will be given the
         * supplied defaultValue if it does not exist or is not of the correct type.
         */
        public Property(String propertyKey, Object defaultValue) {
            this.propertyKey = propertyKey;
            this.isRequired = false;
            this.defaultValue = defaultValue;
        }

        /**
         * This method checks if the given propertyValue was the defaultValue.
         */
        public boolean isDefaultValue(Object propertyValue) {
            if ((propertyValue != null) && (propertyValue == defaultValue)) {
                return true;
            }
            return false;
        }

        /**
         * This abstract method should parse the given value to make sure
         * that it is appropriate for the given data-type and return a new
         * object of the given data-type initialized to the propertyValue or
         * the defaultValue if the propertyValue was invalid.
         */
        public abstract Object parseProperty(String propertyValue);

    }


    /********************************************************************************
     * This class provides a String version of the Property class.
     ********************************************************************************/
    public static class StringProperty extends Property {

        /**
         * Constructor for StringProperty.
         */
        public StringProperty(String propertyKey) {
            super(propertyKey);
        }

        /**
         * Constructor for StringProperty.
         */
        public StringProperty(String propertyKey, String defaultValue) {
            super(propertyKey, defaultValue);
        }

        /**
         * Returns the propertyValue or returns defaultValue if the propertyValue is
         * null or empty.
         */
        public Object parseProperty(String propertyValue) {
            if ((propertyValue == null) || (propertyValue.trim().length() == 0)) {
                return defaultValue;
            }
            return propertyValue;
        }

    }


    /********************************************************************************
     * This class provides a Boolean version of the Property class.
     ********************************************************************************/
    public static class BooleanProperty extends Property {

        /**
         * Constructor for BooleanProperty.
         */
        public BooleanProperty(String propertyKey, Boolean defaultValue) {
            super(propertyKey, defaultValue);
        }

        /**
         * This method parses the boolean value of the given propertyValue.  If the propertyValue is empty or
         * invalid, the defaultValue will be returned
         */
        public Object parseProperty(String propertyValue) {
            if ((propertyValue == null) || (propertyValue.trim().length() == 0)) {
                return defaultValue;
            }

            propertyValue = propertyValue.toString().toLowerCase();
            if (propertyValue.equals("1") || propertyValue.equals("yes") ||
                propertyValue.equals("true") || propertyValue.equals("on")) {
                return new Boolean(true);
            } else if (propertyValue.equals("0") || propertyValue.equals("no") ||
                       propertyValue.equals("false") ||
                       propertyValue.equals("off")) {
                return new Boolean(false);
            } else {
                return defaultValue;
            }
        }

    }


    /********************************************************************************
     * This class provides an Integer version of the Property class.
     ********************************************************************************/
    public static class IntegerProperty extends Property {

        private final int minValue;
        private final int maxValue;

        /**
         * Constructor for IntegerProperty.
         */
        public IntegerProperty(String propertyKey, Integer defaultValue,
                               int minValue, int maxValue) {
            super(propertyKey, defaultValue);
            this.minValue = minValue;
            this.maxValue = maxValue;
        }

        /**
         * This method parses the Integer value of the given propertyValue.  If the propertyValue
         * is empty or invalid, the defaultValue will be returned
         */
        public Object parseProperty(String propertyValue) {
            if ((propertyValue == null) || (propertyValue.trim().length() == 0)) {
                return defaultValue;
            }

            try {
                int intValue = Integer.parseInt(propertyValue);
                if ((intValue < minValue) || (intValue > maxValue)) {
                    return defaultValue;
                }
                return new Integer(intValue);
            } catch (NumberFormatException nfe) {
                return defaultValue;
            }
        }

    }


    /********************************************************************************
     * This class monitors a given configFile for changes.  When an ipdate occurs,
     * it will re-read the properties from the file.
     ********************************************************************************/
    private class MonitoringThread extends Thread {

        private final File configFile;

        /**
         * Constructor for MonitoringThread
         */
        public MonitoringThread(File configFile) {
            this.configFile = configFile;
            this.setPriority(Thread.MIN_PRIORITY);
        }

        /**
         * This method returns the config file path associated with this
         * monitoring thread.
         */
        public String getConfigFilePath() {
            return configFile.getPath();
        }

        /**
         * This method starts a new thread to monitor the configuration
         * file change.
         */
        public void run() {
            Thread thisThread = Thread.currentThread();
            while (thisThread == monitoringThread) {
                try {
                    Thread.sleep(configFileMonitoringIntervalSecs * 1000);

                    log.debug(
                            "MonitoringThread woke up: checking config file for modification");
                    final boolean wasDeleted = ((!configFile.exists()) &&
                                                (isConfigValid));
                    final boolean wasUpdated = ((configFile.exists()) &&
                                                (configFile.lastModified() >
                                                 lastUpdate));
                    if (wasDeleted && log.isInfoEnabled()) {
                        log.info(configFile.getPath() + " got deleted!");
                    }
                    if (wasUpdated && log.isInfoEnabled()) {
                        log.info(configFile.getPath() + " got updated!");
                    }
                    if (wasDeleted || wasUpdated) {
                        readPropertiesFromConfig(configFile);
                        notifyUpdateListeners();
                    }
                } catch (InterruptedException ie) {
                }
            }
        }

    }


    /**
     * Constructor for Config.
     */
    protected CosignConfig() {
    }

    /**
     * This method returns true if the configuration is valid, false otherwise
     */
    public boolean isConfigValid() {
        rwLock.getReadLock();
        try {
            return isConfigValid;
        } finally {
            rwLock.releaseLock();
        }
    }

    /**
     * This method returns the current path for the config XML file.
     */
    public String getConfigFilePath() {
        if (monitoringThread == null) {
            return null;
        }
        return monitoringThread.getConfigFilePath();
    }

    /**
     * This method sets the path for the config XML file and loads the
     * properties from the given file.
     */
    public void setConfigFilePath(String configFilePath) {
        if (configFilePath == null) {
            throw new IllegalArgumentException(
                    "setConfigFilePath must be given a non-null config file path");
        }
        if (monitoringThread != null) {
            throw new IllegalStateException(
                    "setConfigFilePath has already been initialized");
        }

        // Create a new thread to monitor the given configFile and
        // read the properties from the file
        File configFile = new File(configFilePath);
        readPropertiesFromConfig(configFile);

        // Start monitoring the config file for changes
        monitoringThread = new MonitoringThread(configFile);
        monitoringThread.start();
    }


    /**
     * This method registers a new UpdateListener
     */
    public void addUpdateListener(UpdateListener updateListener) {
        synchronized (updateListeners) {
            updateListeners.add(updateListener);
        }
    }


    /**
     * This method registers a new UpdateListener
     */
    public void removeUpdateListener(UpdateListener updateListener) {
        synchronized (updateListeners) {
            updateListeners.remove(updateListener);
        }
    }

    /**
     * This method returns a property in <code>Object</code>.
     * @param propertyKey The key of the property
     * @return    The <code>Object</code> value of the property.
     */
    public Object getPropertyValue(String propertyKey) {
        rwLock.getReadLock();
        try {
            return propertyKeyToValue.get(propertyKey);
        } finally {
            rwLock.releaseLock();
        }
    }

    /**
     * This method returns a property in <code>Object</code>.
     * @param propertyKey The key of the property
     *
     * @return    The <code>Object</code> value of the property.
     */
    public Object getPropertyValueinContext(String propertyKey,
                                            String path, String resource, String qString) {
        rwLock.getReadLock();

        try {
            if (path != null && path.length() > 0) {
                ServiceConfig sConfig = hasServiceOveride(path, resource, qString);
                if (sConfig != null) {
                    if (propertyKey.equalsIgnoreCase(this.SERVICE_NAME)) {
                        return sConfig.getName();
                    }
                } else {
                    return propertyKeyToValue.get(propertyKey);
                }
            }
            return propertyKeyToValue.get(propertyKey);
        } finally {
            rwLock.releaseLock();
        }
    }


    public ServiceConfig matchServiceWithName( String serviceName ) {
        rwLock.getReadLock();
   boolean matchedName = false;

   try {
       Iterator it = servicePaths.iterator();
       ServiceConfig theService;




       while (it.hasNext()) {
           //do the resou
           theService = (ServiceConfig) it.next();

           log.info("Service name in: " + serviceName);
           log.info("Service name in compared to read in services: " + theService.getName());

           if(theService.getName().equalsIgnoreCase(serviceName))
               return theService;

       }

   } finally {
       rwLock.releaseLock();
   }

   return null; //default service

    }
    /**
     * This method returns a ServiceConfig Object on the basis of whether the services
     * map contains the requested path.
     */
    public ServiceConfig hasServiceOveride(String path, String resource,
                                           String qString) {
        rwLock.getReadLock();
        boolean matchedPath = false;
        boolean matchedResource = false;
        boolean matchedQuery = false;
        try {
            Iterator it = servicePaths.iterator();
            ServiceConfig theService;
            while (it.hasNext()) {
                //do the resou
                theService = (ServiceConfig) it.next();
                matchedPath = false;
                matchedResource = false;
                matchedQuery = false;
                //check for exact match first
                if (theService.getPath().equalsIgnoreCase(path))
                    matchedPath = true;
                else{
                    log.info("Service path end char: " + theService.getPath().charAt(theService.getPath().length()-1));
                    log.info("Service path substring: " + theService.getPath().substring(0,theService.getPath().length()-1));
                    log.info("request Path: " + path);

                    if(theService.getPath().charAt(theService.getPath().length()-1) =='*'){
                        String subPath = theService.getPath().substring(0,theService.getPath().length() - 1);
                        boolean isThere = path.startsWith(subPath);
                        if( isThere ){
                            log.info("Path found, matched path = true. /n");
                            matchedPath = true;
                          }
                    }
                }
                if (theService.hasResource() && (resource != null)){
                    if (theService.getResource().equalsIgnoreCase(resource))
                        matchedResource = true;
                }
                else
                if (!theService.hasResource())
                    matchedResource = true;


                if (theService.hasQs() && (qString != null)){
                    log.info("Service qs: " + theService.getQs());
                    log.info("path qs: " + qString);
                    if (theService.getQs().equalsIgnoreCase(qString))
                        matchedQuery = true;
                }
                else
                if (!theService.hasQs() )
                     matchedQuery = true;


                if(matchedPath && matchedResource && matchedQuery)
                     return theService;
            }

        } finally {
            rwLock.releaseLock();
        }

        return null; //default service
    }


    /**
     * This method returns a string with all key/value pairs
     * of the Cosign configurations.
     */
    public String toString() {
        rwLock.getReadLock();
        try {
            Object[] keys = propertyKeyToValue.keySet().toArray();
            Arrays.sort(keys);
            StringBuffer sb = new StringBuffer();
            for (int i = 0; i < keys.length; i++) {
                sb.append(keys[i] + " = " + propertyKeyToValue.get(keys[i]) + "\n");
            }
            return sb.toString();
        } finally {
            rwLock.releaseLock();
        }
    }

    /**
     * This method reads all the properties in the Cosign
     * configuration file.
     */
    private boolean readPropertiesFromConfig(File configFile) {
        rwLock.getWriteLock();
        try {
            // update the file modification time stamp (returns 0 if file doesn't exist)
            lastUpdate = configFile.lastModified();

            // reset the previous properties
            propertyKeyToValue.clear();
            servicePaths.clear();
            isConfigValid = false;

            // Make sure that we have a semi-valid config file
            if ((configFile == null) || (!configFile.exists())) {
                return false;
            }

            // load and parse the XML config file
            DocumentBuilderFactory factory = DocumentBuilderFactory.newInstance();
            DocumentBuilder builder = factory.newDocumentBuilder();
            Document document = builder.parse(configFile);

            boolean missingOrInvalidProperty = false;
            int propertyCount[] = new int[1];

            // loop through each of the properties
            Element rootElement = document.getDocumentElement();
            for (int propIdx = 0; propIdx < PROPERTIES.length; propIdx++) {
                Property property = PROPERTIES[propIdx];
                if (property.propertyKey.equalsIgnoreCase(SERVICES)) {
                    NodeList servicesNode = rootElement.getElementsByTagName(
                            SERVICES);
                    int length = servicesNode.getLength();
                    if (length == 1) {

                        NodeList services = document.getElementsByTagName(
                                "service");
                        for (int i = 0; i < services.getLength(); i++) {
                            Element element = (Element) services.item(i);
                            /** start of service parsing **/
                            Vector v = new Vector();
                            /**do the factors**/

                            NodeList reqF = element.getElementsByTagName("reqfactor");
                            if(reqF.getLength()>0){

                                Element factors = (Element) reqF.item(0);
                                NodeList factor = factors.getElementsByTagName("factor");
                                for(int k = 0; k < factor.getLength(); k++){
                                    Element fac = (Element) factor.item(k);
                                    if(fac.getFirstChild() != null)
                                        v.add(fac.getFirstChild().getNodeValue());
                                }
                            }
                            /**now specific mappings**/
                            NodeList prot = element.getElementsByTagName(
                                    "protected");

                            String sAttrValue = element.getAttribute(
                                    "name");



                            for (int j = 0; j < prot.getLength(); j++) {
                                Element pElement = (Element) prot.item(j);
                                String pAttr = "";
                                String pQs = "";
                                String pRs = "";
                                String proxies = "";
                                if (pElement.hasAttributes()) {
                                    NamedNodeMap attribs = pElement.
                                            getAttributes();
                                    try {
                                        pAttr = attribs.getNamedItem(
                                                "allowpublicaccess").
                                                getNodeValue();
                                    } catch (Exception ex) {
                                    }
                                    try {
                                        pQs = attribs.getNamedItem(
                                                "qs").getNodeValue();
                                    } catch (Exception ex1) {
                                    }
                                    try {
                                        pRs = attribs.getNamedItem(
                                                "rs").getNodeValue();
                                    } catch (Exception ex2) {
                                    }
                                    try {
                                        proxies = attribs.getNamedItem(
                                                "getproxies").getNodeValue();
                                    } catch (Exception ex2) {
                                    }

                                }
                                ServiceConfig serviceConfig = new ServiceConfig();
                                serviceConfig.setFactors(v);

                                if (pAttr.equalsIgnoreCase("true")) {
                                    serviceConfig.setPublicAccess("true");
                                }
                                if (pQs.length() > 0) {
                                    serviceConfig.setQs(pQs);
                                }
                                if (pRs.length() > 0) {
                                    serviceConfig.setResource(pRs);
                                }
                                if (proxies.equalsIgnoreCase("true")) {
                                    serviceConfig.setDoProxies(true);
                                }

                                serviceConfig.setName(sAttrValue);
                                serviceConfig.setPath(pElement.getFirstChild().
                                        getNodeValue());
                                servicePaths.add(serviceConfig);

                            }
                            log.debug("Service Paths: " + servicePaths);
                            /* End of service parsing */
                        }
                    }
                    propertyKeyToValue.put(property.propertyKey,
                                           new
                                           BooleanProperty(property.propertyKey,
                            new Boolean(true)));
                    continue;
                }

                Object propertyValue = property.parseProperty(
                        getNodeValueFromTag(rootElement, property.propertyKey,
                                            propertyCount));

                if (propertyCount[0] > 1) {
                    if (log.isErrorEnabled()) {
                        log.error("Duplicate property value in config file: " +
                                  property.propertyKey);
                    }
                    missingOrInvalidProperty = true;

                } else if ((property.isRequired) && (propertyValue == null)) {
                    if (log.isErrorEnabled()) {
                        log.error(
                                "Required property missing from config file: " +
                                property.propertyKey);
                    }
                    missingOrInvalidProperty = true;

                } else {
                    if (property.isDefaultValue(propertyValue)) {
                        if (log.isInfoEnabled()) {
                            log.info("Using default value of " + propertyValue +
                                     " for property " + property.propertyKey);
                        }
                    }
                    propertyKeyToValue.put(property.propertyKey, propertyValue);
                }
            }

            // Ensure that if HTTPS_ONLY is enabled and SITE_ENTRY is defined,
            // the SITE_ENTRY must start with "https://"
            Boolean httpsOnly = (Boolean) propertyKeyToValue.get(HTTPS_ONLY);
            if ((httpsOnly != null) && (httpsOnly.booleanValue())) {
                String siteEntry = (String) propertyKeyToValue.get(
                        LOGIN_SITE_ENTRY_URL);
                if ((siteEntry != null) &&
                    (!siteEntry.toLowerCase().startsWith("https://"))) {
                    if (log.isErrorEnabled()) {
                        log.error("'" + HTTPS_ONLY + "' is enabled and '" +
                                  LOGIN_SITE_ENTRY_URL +
                                  "' property specifies a non-HTTPS URL: " +
                                  siteEntry);
                    }
                    missingOrInvalidProperty = true;
                }
            }

            // If the config isn't valid due to a missing required property,
            // bail out now
            if (missingOrInvalidProperty) {
                propertyKeyToValue.clear();
                log.info("*** BAiling out of config file build");
                return false;
            }

            // If we got this far without an error ... we have a valid config file
            this.isConfigValid = true;
            log.info("*** Config file parsed ok");

            return true;

        } catch (Exception e) {
            if (log.isErrorEnabled()) {
                log.error("Exception while reading config file: " +
                          configFile.getPath(), e);
            }
            propertyKeyToValue.clear();
            return false;

        } finally {
            rwLock.releaseLock();

            // Now that the writer lock is released, if we had a valid config file
            // we need to do a little more work...
            if (this.isConfigValid) {
                // update the monitoring interval
                configFileMonitoringIntervalSecs = ((Integer) getPropertyValue(
                        CONFIG_FILE_MONITOR_INT_SECS)).intValue();

                // Print out the current state of the cosign config
                if (log.isInfoEnabled()) {
                    log.info(toString());
                }
            }
        }

    }

    /**
     * This method notifies all registered listeners that the config has
     * been reloaded
     */
    private void notifyUpdateListeners() {
        // Notify all registered listeners that the config has been
        // reloaded
        synchronized (updateListeners) {
            for (int idx = 0; idx < updateListeners.size(); idx++) {
                UpdateListener updateListener = (UpdateListener)
                                                updateListeners.get(idx);
                updateListener.configUpdated();
            }
        }
    }

    /**
     * Retrieves the nodeValue from the node with the given tagName
     */
    private String getNodeValueFromTag(Element parentElement, String tagName,
                                       int[] propertyCount) {
        Element element = getElementByTagName(parentElement, tagName,
                                              propertyCount);
        if ((element == null) || (element.getFirstChild() == null)) {
            return null;
        }
        return element.getFirstChild().getNodeValue();
    }

    /**
     * Retrieves the first element from the parent element with the given tagName
     */
    private Element getElementByTagName(Element parentElement, String tagName,
                                        int[] propertyCount) {
        NodeList nodeList = parentElement.getElementsByTagName(tagName);
        final int length = nodeList.getLength();
        if (propertyCount != null) {
            propertyCount[0] = length;
        }
        if (length != 1) {
            return null;
        }
        return (Element) nodeList.item(0);
    }

public void setServerVersion(String version){
    propertyKeyToValue.put(COSIGN_SERVER_VERSION, version);
}

public boolean isServerVersion2(){
    String version = (String)propertyKeyToValue.get(COSIGN_SERVER_VERSION);
    return (((String)propertyKeyToValue.get(COSIGN_SERVER_VERSION)).equalsIgnoreCase("2"));
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
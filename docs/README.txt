jCoSign v1.0 beta 1


Features:

  The jCoSign class library comes with multiple components to allow for varying
  degrees of system integration.

  - A Java Servlet Spec. v2.3 Servlet Filter for JAAS-based authentication
    filtering (AuthenticationFilter).

  - A JAAS Login Module for CoSign authentication (CosignLoginModule).

  - A Jakarta Commons Pool-based framework for pooling connections to a cluster
    of CoSign servers.

  - A JSSE-based framework for SSL-based application server/CoSign server
    communication.

Requirements:

  - J2SE v1.4, or later, JDK or JRE
    (http://java.sun.com/j2se)

  OR

  - J2SE v1.3 JDK or JRE
    (http://java.sun.com/j2se/1.3)

  - JAAS v1.0
    (http://java.sun.com/products/jaas/index-10.html)

  - JSSE v1.0.x
    (http://java.sun.com/products/jsse/index-103.html)

  AND

  - A Servlet Spec. v2.3 or later application server.
    (Tomcat 4.1, for example; http://jakarta.apache.org/tomcat)

  - Jakarta Commons Pool
    (http://jakarta.apache.org/commons/pool/)

  - Jakarta Commons Collections
    (http://jakarta.apache.org/commons/collections/)

  NOTE: The Jakarta Commons Pool and Collections libraries come with
        Jakarta Tomcat

Install:

  To install jCoSign into an existing web application, perform the following.

  - Create an SSL KeyStore:

    1. Create a keystore using your JDK or JRE's "keytool" utility.
       A description of how to use keytool can be found at
       http://java.sun.com/j2se/1.4.2/docs/tooldocs/solaris/keytool.html

    2. Place your keystore somewhere that your application server has
       access.

  - Configure JAAS:

    1. Create a JAAS configuration file (named jaas.conf in our installation
       example), containing the following:

CosignAuthentication
{
  edu.umich.auth.cosign.CosignLoginModule required;
};

       NOTE: If you already maintain a JAAS configuration file, add the above
             to this file.

    2. Place the file somewhere that your application server has access.
       Perhaps even with your app. server's other configurationf files.

  - Configure the CoSign Server Pool:

    1. Create a file in your application's WEB-INF/classes directory, in the
       subdirectory edu/umich/auth/cosign (to be fixed), named
       cosignConfig.properties, continaing the following:

KEYSTORE_PATH=/usr/local/tomcat/conf/keystore
KEYSTORE_PASSWORD=password
COSIGN_DOMAIN=weblogin.umich.edu
COSIGN_PORT=6663
COSIGN_POOL_LOCKED_SLEEP_TIME=100
COSIGN_POOL_MONITORING_INTERVAL=10000
CONFIG_FILE_MONITORING_INTERVAL=5000
CONFIG_FILE_PATH=/usr/local/webapps/app_name/WEB-INF/classes/edu/umich/auth/cosign/cosignConfig.properties

    2. Change these properties to those of your environment.


  - Configure your Web Application:

    1. Copy the jcosign-1.0b1.jar file into you application's WEB-INF/lib
       directory.

    2. If you are using a J2SE v1.3 JRE, copy the files jaas.jar
       (from the JAAS download), jcert.jar, jnet.jar, and jsse.jar
       (from the JSSE download) into your application's WEB-INF/lib
       directory.

    3. Edit your application's web application deployment descriptor file
       (WEB-INF/web.xml), adding the following:

<filter>
  <filter-name>Cosign Authentication Filter</filter-name>
  <filter-class>edu.umich.auth.AuthenticationFilter</filter-class>

  <init-param>
    <param-name>Auth.LoginConfiguration</param-name>
    <param-value>CosignAuthentication</param-value>
  </init-param>

  <init-param>
    <param-name>Auth.JAASConfigurationFile</param-name>
    <param-value>/usr/local/tomcat/conf/jaas.conf</param-value>
  </init-param>

  <init-param>
    <param-name>Auth.LoginModule</param-name>
    <param-value>edu.umich.auth.cosign.CosignLoginModule</param-value>
  </init-param>

  <init-param>
    <param-name>Auth.CallbackHandler</param-name>
    <param-value>edu.umich.auth.cosign.CosignServletCallbackHandler</param-value>
  </init-param>

  <init-param>
    <param-name>Auth.Cosign.ServiceName</param-name>
    <param-value>cosign-java</param-value>
  </init-param>

  <init-param>
    <param-name>Auth.Cosign.LoginServer</param-name>
    <param-value>https://weblogin.umich.edu</param-value>
  </init-param>

  <!--
    OPTIONAL: Sets whether or not login should fail if the client's IP address
              changes. The default is 'true'.
  -->
  <init-param>
    <param-name>Auth.Cosign.CheckClientIP</param-name>
    <param-value>false</param-value>
  </init-param>

  <!--
    OPTIONAL: Sets how many seconds between checks to the Cosign server.
              The default is 60 seconds.
  -->
  <init-param>
    <param-name>Auth.Cosign.ServerCheckDelay</param-name>
    <param-value>30</param-value>
  </init-param>
</filter>

<filter-mapping>
  <filter-name>Cosign Authentication Filter</filter-name>
  <url-pattern>/secure/*</url-pattern>
</filter-mapping>

       NOTE: Ensure that you adhere to the element ordering specified in the
             web application deployment descriptor DTD
             (i.e. Your 'filter' elements are all grouped at the beginning
                   of you application, followed by you 'filter-mapping'
                   elements).

       NOTE: The CoSign filter-mapping generally should be listed in your
             web.xml before any other filter-mapping elements.

    4. Set the parameters Auth.JAASConfigurationFile, Auth.Cosign.ServiceName,
       and Auth.Cosign.LoginServer to the appropriate values, according to
       your CoSign server installation and previous installation choices.

    5. Change the "url-pattern" element of the filter mapping to the context
       inside of your applcation that you want CoSign protected.  If you want
       your entire application protected, enter "/*" here.

       NOTE: The context that you specify here is withing your application and
             is independent of any context mapping that you specify in you
             application server's configuration (server.xml).

    6. Re/deploy your application.

Future:

  - A generic/example servlet controller for Servlet Spec. v2.2 and earlier
    servlet containers.

  - A sample web application.

  - uPortal v2.x integration.

  - JMX support for live connection pool re/configuration.

  - More/cleaner documentation (inline, JavaDocs, and otherwise).

Support:

  cosign@umich.edu
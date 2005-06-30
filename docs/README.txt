jCoSign v1.0 beta 2

Features:

  The jCoSign class library comes with multiple components to allow for varying
  degrees of system integration.

  - A Java Servlet Spec. v2.3 Servlet Filter for JAAS-based authentication
    filtering (AuthenticationFilter).

  - A JAAS Login Module for CoSign authentication (CosignLoginModule).

  - A Jakarta Commons Pool-based framework for pooling connections to a cluster
    of CoSign servers.
    
  - A Jakarta Commons Logging for customizable log configuration.

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

  - Jakarta Commons Loggins
    (http://jakarta.apache.org/commons/logging/)

  NOTE: The Jakarta Commons Pool and Collections libraries come with
        Jakarta Tomcat

Install:

  To install jCoSign into an existing web application, perform the following.

  - Create an SSL KeyStore:

    1. Create a keystore using your JDK or JRE's "keytool" utility.
       A description of how to use keytool can be found at
       http://java.sun.com/j2se/1.4.2/docs/tooldocs/solaris/keytool.html

       a) Generate a new private key/public cert.
          (keytool -genkey -keyalg "RSA" -keystore keystore)
          
       b) Generate a certificate signing request (CSR)
          (keytool -certreq -keyalg "RSA" -file my.host.com.csr -keystore keystore)
          
       c) Have your CA sign the CSR
       
       d) Import the certificate received from the CA into the keystore
          (keytool -keystore keystore -keyalg "RSA" -import -trustcacerts -file my.host.com.cer)

    2. Place your keystore somewhere that your application server has
       access.

  - Configure the CoSign Filter:

    1. Create a XML cosign configuration file continaing the following:

<CosignConfig>

  <!-- KeyStorePath: required -->
  <KeyStorePath>/Developer/Applications/Eclipse/workspace/JavaCosign/jks/keystore.jks</KeyStorePath>

  <!-- KeyStorePassword: required -->
  <KeyStorePassword>burned</KeyStorePassword>

  <!-- ServiceName: required -->
  <ServiceName>java</ServiceName>

  <!-- CosignServerHost: required -->
  <CosignServerHost>weblogin.umich.edu</CosignServerHost>

  <!-- CosignServerPort: required -->
  <CosignServerPort>6663</CosignServerPort>

  <!-- ConnectionPoolSize: optional, defaults to 20 -->
  <ConnectionPoolSize>20</ConnectionPoolSize>
  
  <!-- CookieExpireSecs: optional, defaults to 86400 -->
  <CookieExpireSecs>120</CookieExpireSecs>	

  <!-- CookieCacheExpireSecs: optional, defaults to 120 -->
  <CookieCacheExpireSecs>30</CookieCacheExpireSecs> 

  <!-- LoginRedirectUrl: required -->
  <LoginRedirectUrl>http://weblogin.umich.edu/</LoginRedirectUrl>

  <!-- LoginPostErrorUrl: required -->
  <LoginPostErrorUrl>http://www.umich.edu/</LoginPostErrorUrl>

  <!-- LoginSiteEntryUrl: optional -->
  <LoginSiteEntryUrl>http://localhost:8080/cosign/</LoginSiteEntryUrl>

  <!-- CheckClientIP: optional, defaults to false -->
  <CheckClientIP>false</CheckClientIP>

  <!-- AllowPublicAccess: optional, defaults to false -->
  <AllowPublicAccess>false</AllowPublicAccess>	

  <!-- HttpsOnly: optional, defaults to false -->
  <HttpsOnly>false</HttpsOnly>

  <!-- HttpsPort: optional, defaults to 443 -->
  <HttpsPort>8443</HttpsPort>
  
  <!-- ClearSessionOnLogin: optional, defaults to false -->
  <ClearSessionOnLogin>true</ClearSessionOnLogin>

  <!-- ConfigFileMonitoringIntervalSecs: optional: defaults to 30 -->
  <ConfigFileMonitoringIntervalSecs>30</ConfigFileMonitoringIntervalSecs>
  
</CosignConfig>

    2. Change these properties to those of your environment.

    3. Place your XML cosign configuration file somewhere that your
       application server has access.

  - Configure your Web Application:

    1. Copy the jcosign-1.0b2.jar file into you application's WEB-INF/lib
       directory.

    2. If you are using a J2SE v1.3 JRE, copy the files jaas.jar
       (from the JAAS download), jcert.jar, jnet.jar, and jsse.jar
       (from the JSSE download) into your application's WEB-INF/lib
       directory.

    3. Edit your application's web application deployment descriptor file
       (WEB-INF/web.xml), adding the following:

  <filter>
    <filter-name>Cosign Authentication Filter</filter-name>
    <filter-class>edu.umich.auth.cosign.CosignAuthenticationFilter</filter-class>

    <init-param>
      <param-name>Cosign.ConfigurationFile</param-name>
      <param-value>[ PATH TO YOUR COSIGN CONFIG FILE ]</param-value>
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

    4. Set the parameter Cosign.ConfigurationFile to the appropriate path 
       of your XML cosign config file.

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
  
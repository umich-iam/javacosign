<%@ page language="java" %>

<%
   session.invalidate();

   Cookie cookie = new Cookie( "cosign-java", "" );
   cookie.setPath( "/" );

   response.addCookie( cookie );
   response.sendRedirect( "https://cosign-test.www.umich.edu/cgi-bin/logout?http://www.umich.edu" );
%>

<!DOCTYPE HTML PUBLIC "-//w3c//dtd html 4.0 transitional//en">

<html>
  <head>
    <title>AuthenticationFilter/CosignLoginModule Test: Logout</title>
  </head>

  <body bgcolor="#FFFFFF">
    Logged out.<br>
    <a href="index.jsp">Log-in</a>
  </body>
</html>
<%@ page language="java" %>

<%
   session.invalidate();

   Cookie cookie = new Cookie( "cosign-java", "" );
   cookie.setPath( "/" );

   response.addCookie( cookie );
   response.sendRedirect( "https://cosign-test.www.umich.edu/cgi-bin/logout?http://www.umich.edu" );
%>
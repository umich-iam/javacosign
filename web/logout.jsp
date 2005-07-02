<%@ page language="java" %>

<%
   session.invalidate();

   Cookie cookie = new Cookie( "cosign-java", "null" );
   cookie.setPath( "/" );

   response.addCookie( cookie );
   response.sendRedirect( "https://weblogin.umich.edu/cgi-bin/logout?http://www.umich.edu" );
%>
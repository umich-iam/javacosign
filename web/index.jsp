<%@ page language="java" %>

<!DOCTYPE HTML PUBLIC "-//w3c//dtd html 4.0 transitional//en">

<html>
  <head>
    <title>AuthenticationFilter Test: Login</title>
  </head>

  <body bgcolor="#FFFFFF">
    <a href="cosign-secure">Cosign Log-in</a><br>
    <a href="krb5-secure">Kerberos 5 Log-in</a><br>

    <br>
    <form action="cosign-secure/index.jsp" method="post">
      <input type="submit" value="Cosign Post Test">
    </form>
  </body>
</html>

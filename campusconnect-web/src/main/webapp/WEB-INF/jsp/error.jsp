<%@ page language="java" contentType="text/html; charset=UTF-8" pageEncoding="UTF-8" isErrorPage="true" %>
<html>
<head><title>Error</title></head>
<body>
<h2>Something went wrong.</h2>
<%-- XXX CC-1288: this prints the stack trace to the browser in production. --%>
<pre><% if (exception != null) { exception.printStackTrace(new java.io.PrintWriter(out)); } %></pre>
</body>
</html>

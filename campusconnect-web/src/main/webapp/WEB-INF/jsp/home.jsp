<%@ page language="java" contentType="text/html; charset=UTF-8" pageEncoding="UTF-8" %>
<%@ taglib prefix="c" uri="http://java.sun.com/jsp/jstl/core" %>
<html>
<head>
  <title><c:out value="${institutionName}"/> - CampusConnect</title>
  <link rel="stylesheet" href="<%= request.getContextPath() %>/static/css/campusconnect.css"/>
</head>
<body>
<div class="header" style="background-color: <c:out value='${headerColor}'/>;">
  <h1><c:out value="${institutionName}"/></h1>
  <span class="cust"><c:out value="${customerCode}"/></span>
</div>

<ul class="nav">
  <li><a href="<%= request.getContextPath() %>/students/list">Students</a></li>
  <li><a href="<%= request.getContextPath() %>/alerts/open">Early alerts</a></li>
  <li><a href="<%= request.getContextPath() %>/appointments/day?advisorId=1"><c:out value="${advisorLabel}"/> schedule</a></li>
  <li><a href="<%= request.getContextPath() %>/billing/holds">Financial holds</a></li>
</ul>

<p>Support: <a href="mailto:<c:out value='${supportEmail}'/>"><c:out value="${supportEmail}"/></a></p>

</body>
</html>

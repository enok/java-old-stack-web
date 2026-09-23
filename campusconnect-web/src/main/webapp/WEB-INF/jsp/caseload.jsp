<%@ page language="java" contentType="text/html; charset=UTF-8" pageEncoding="UTF-8" %>
<%@ taglib prefix="c" uri="http://java.sun.com/jsp/jstl/core" %>
<html>
<head>
  <title>Caseload - <c:out value="${institutionName}"/></title>
  <link rel="stylesheet" href="<%= request.getContextPath() %>/static/css/campusconnect.css"/>
</head>
<body>
<div class="header" style="background-color: <c:out value='${headerColor}'/>;">
  <h1><c:out value="${institutionName}"/></h1>
</div>

<h2><c:out value="${advisorLabel}"/> caseload</h2>

<table class="detail">
  <tr><th>Students</th><td><c:out value="${summary.total}"/></td></tr>
  <tr><th>At risk</th><td><c:out value="${summary.atRisk}"/></td></tr>
  <tr><th>On probation</th><td><c:out value="${summary.probation}"/></td></tr>
  <tr><th>Average GPA</th><td><c:out value="${summary.averageGpa}"/></td></tr>
  <tr><th>As of</th><td><c:out value="${summary.asOf}"/></td></tr>
</table>

</body>
</html>

<%@ page language="java" contentType="text/html; charset=UTF-8" pageEncoding="UTF-8" %>
<%@ taglib prefix="c" uri="http://java.sun.com/jsp/jstl/core" %>
<html>
<head>
  <title>Early alerts - <c:out value="${institutionName}"/></title>
  <link rel="stylesheet" href="<%= request.getContextPath() %>/static/css/campusconnect.css"/>
</head>
<body>
<div class="header" style="background-color: <c:out value='${headerColor}'/>;">
  <h1><c:out value="${institutionName}"/></h1>
</div>

<h2>Open early-alert cases</h2>

<table class="grid">
  <tr><th>Student</th><th><c:out value="${advisorLabel}"/></th><th>Reason</th><th>Severity</th><th>Status</th><th></th></tr>
  <c:forEach var="k" items="${cases}">
    <tr>
      <td><c:out value="${k.student.fullName}"/></td>
      <td><c:out value="${k.advisor.fullName}"/></td>
      <td><c:out value="${k.reason}"/></td>
      <td><c:out value="${k.severity}"/></td>
      <td><c:out value="${k.status}"/></td>
      <td>
        <form method="post" action="<%= request.getContextPath() %>/alerts/close">
          <input type="hidden" name="caseId" value="<c:out value='${k.id}'/>"/>
          <input type="text" name="notes" size="20"/>
          <input type="submit" value="Close"/>
        </form>
      </td>
    </tr>
  </c:forEach>
</table>

</body>
</html>

<%@ page language="java" contentType="text/html; charset=UTF-8" pageEncoding="UTF-8" %>
<%@ taglib prefix="c" uri="http://java.sun.com/jsp/jstl/core" %>
<html>
<head>
  <title>Financial holds - <c:out value="${institutionName}"/></title>
  <link rel="stylesheet" href="<%= request.getContextPath() %>/static/css/campusconnect.css"/>
</head>
<body>
<div class="header" style="background-color: <c:out value='${headerColor}'/>;">
  <h1><c:out value="${institutionName}"/></h1>
  <span class="cust"><c:out value="${customerCode}"/></span>
</div>

<h2>Active financial holds</h2>

<p class="notice">
  Threshold at this institution: <c:out value="${holdThreshold}"/>.
  A hold blocks advising appointments - see the schedule screen.
</p>

<table class="grid">
  <tr><th>Student</th><th>Reason</th><th>Balance at placement</th><th>Placed</th><th>Placed by</th></tr>
  <c:forEach var="h" items="${holds}">
    <tr>
      <td>
        <a href="<%= request.getContextPath() %>/billing/account?studentId=<c:out value='${h.student.id}'/>">
          <c:out value="${h.student.fullName}"/>
        </a>
      </td>
      <td><c:out value="${h.reasonCode}"/></td>
      <td><c:out value="${h.balanceAtPlacement}"/></td>
      <td><c:out value="${h.placedAt}"/></td>
      <td><c:out value="${h.placedBy}"/></td>
    </tr>
  </c:forEach>
</table>

</body>
</html>

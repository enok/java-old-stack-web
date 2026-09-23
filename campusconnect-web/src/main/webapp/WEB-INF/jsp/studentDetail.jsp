<%@ page language="java" contentType="text/html; charset=UTF-8" pageEncoding="UTF-8" %>
<%@ taglib prefix="c" uri="http://java.sun.com/jsp/jstl/core" %>
<html>
<head>
  <title><c:out value="${student.fullName}"/> - <c:out value="${institutionName}"/></title>
  <link rel="stylesheet" href="<%= request.getContextPath() %>/static/css/campusconnect.css"/>
</head>
<body>

<div class="header" style="background-color: <c:out value='${headerColor}'/>;">
  <h1><c:out value="${institutionName}"/></h1>
</div>

<h2><c:out value="${student.fullName}"/> <span class="risk-${riskBadge}"><c:out value="${riskBadge}"/></span></h2>

<table class="detail">
  <tr><th>SIS ID</th><td><c:out value="${student.sisId}"/></td></tr>
  <tr><th>Email</th><td><c:out value="${student.email}"/></td></tr>
  <tr><th>Phone</th><td><c:out value="${student.phone}"/></td></tr>
  <tr><th>GPA</th><td><c:out value="${gpaLabel}"/></td></tr>
  <tr><th>Credits</th><td><c:out value="${student.creditsCompleted}"/></td></tr>
  <tr><th>Status</th><td><c:out value="${statusLabel}"/></td></tr>
  <tr><th>Program</th><td><c:out value="${student.programCode}"/></td></tr>
  <tr><th><c:out value="${advisorLabel}"/></th><td><c:out value="${student.advisor.fullName}"/></td></tr>
</table>

<h3>Enrollments</h3>
<table class="grid">
  <tr><th>Term</th><th>Course</th><th>Title</th><th>Credits</th><th>Grade</th><th>Status</th></tr>
  <c:forEach var="e" items="${enrollments}">
    <tr>
      <td><c:out value="${e.termCode}"/></td>
      <td><c:out value="${e.courseCode}"/></td>
      <td><c:out value="${e.courseTitle}"/></td>
      <td><c:out value="${e.credits}"/></td>
      <td><c:out value="${e.grade}"/></td>
      <td><c:out value="${e.status}"/></td>
    </tr>
  </c:forEach>
</table>

<h3>Early-alert cases</h3>
<table class="grid">
  <tr><th>Opened</th><th>Reason</th><th>Severity</th><th>Status</th></tr>
  <c:forEach var="k" items="${cases}">
    <tr>
      <td><c:out value="${k.openedAt}"/></td>
      <td><c:out value="${k.reason}"/></td>
      <td><c:out value="${k.severity}"/></td>
      <td><c:out value="${k.status}"/></td>
    </tr>
  </c:forEach>
</table>

</body>
</html>

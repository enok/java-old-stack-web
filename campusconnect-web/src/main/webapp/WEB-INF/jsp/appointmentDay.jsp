<%@ page language="java" contentType="text/html; charset=UTF-8" pageEncoding="UTF-8" %>
<%@ taglib prefix="c" uri="http://java.sun.com/jsp/jstl/core" %>
<html>
<head>
  <title>Schedule - <c:out value="${institutionName}"/></title>
  <link rel="stylesheet" href="<%= request.getContextPath() %>/static/css/campusconnect.css"/>
</head>
<body>

<div class="header" style="background-color: <c:out value='${headerColor}'/>;">
  <h1><c:out value="${institutionName}"/></h1>
</div>

<h2><c:out value="${advisorLabel}"/> schedule for <c:out value="${day}"/></h2>

<p class="notice">
  Slot length <c:out value="${slotMinutes}"/> minutes &middot;
  booking lead time <c:out value="${leadTimeHours}"/> hours &middot;
  walk-ins <c:choose><c:when test="${walkInsAllowed}">allowed</c:when><c:otherwise>not allowed</c:otherwise></c:choose>
</p>

<c:if test="${not empty errors}">
  <ul class="errors">
    <c:forEach var="err" items="${errors}"><li><c:out value="${err}"/></li></c:forEach>
  </ul>
</c:if>

<table class="grid">
  <tr><th>Time</th><th>Student</th><th>Reason</th><th>Location</th><th>Status</th></tr>
  <c:forEach var="a" items="${appointments}">
    <tr>
      <td><c:out value="${a.startsAt}"/></td>
      <td><c:out value="${a.student.fullName}"/></td>
      <td><c:out value="${a.reasonCode}"/></td>
      <td><c:out value="${a.location}"/></td>
      <td><c:out value="${a.status}"/></td>
    </tr>
  </c:forEach>
</table>

<h3>Book</h3>
<form method="post" action="<%= request.getContextPath() %>/appointments/schedule">
  <input type="hidden" name="advisorId" value="<c:out value='${advisorId}'/>"/>
  <label>Student id <input type="text" name="studentId"/></label>
  <label>Start (yyyyMMdd) <input type="text" name="startsAt"/></label>
  <label>Reason <input type="text" name="reasonCode"/></label>
  <c:if test="${walkInsAllowed}">
    <label>Walk-in <input type="checkbox" name="walkIn"/></label>
  </c:if>
  <input type="submit" value="Book"/>
</form>

</body>
</html>

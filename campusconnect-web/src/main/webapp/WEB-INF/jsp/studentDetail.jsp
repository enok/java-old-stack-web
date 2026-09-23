<%@ page language="java" contentType="text/html; charset=UTF-8" pageEncoding="UTF-8" %>
<%@ taglib prefix="c" uri="http://java.sun.com/jsp/jstl/core" %>
<%--
  The advising detail screen. Since 2016 it also renders Student Finance: the
  account balance and the hold state sit in the same table as the GPA and the
  advisor, and the finance block below branches on the customer code with a
  scriptlet the way studentList.jsp does.

  There is no seam here at all. A reader cannot tell which rows of this table
  belong to advising and which belong to billing, and the two numbers come from
  two different services that compute them two different ways.
--%>
<%
    String cc = (String) request.getAttribute("customerCode");
    if (cc == null) { cc = "NORTHLAKE"; }
%>
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
  <%-- Finance rows, mixed straight in with the advising rows. --%>
  <tr><th>Account balance</th><td><c:out value="${balanceLabel}"/></td></tr>
  <tr>
    <th>Financial hold</th>
    <td>
      <c:choose>
        <c:when test="${not empty holdLabel}">
          <span class="risk-high"><c:out value="${holdLabel}"/></span>
        </c:when>
        <c:otherwise>none</c:otherwise>
      </c:choose>
    </td>
  </tr>
</table>

<%
    // Customer branching inside the FINANCE part of an ADVISING view.
    // Occurrence 9 of the fork, and the third place the hold rule is spelled out.
    if ("RIVERTON".equals(cc)) {
%>
  <p class="notice">
    Riverton: a balance over 1000.00 places a hold. Athletes are exempt from the
    appointment block; the compliance office settles their accounts separately.
    Payments are taken in the student portal, not on the statement screen.
  </p>
<%
    } else if ("SUMMIT".equals(cc)) {
%>
  <p class="notice">
    Summit: a balance over 250.00 places a hold. A hold only blocks early-alert
    appointments; counseling appointments are still bookable. The statement is
    grouped by term.
  </p>
<%
    } else {
%>
  <p class="notice">
    A balance over 500.00 places a hold and blocks new appointments until the
    bursar releases it.
  </p>
<%
    }
%>

<p><a href="<%= request.getContextPath() %>/billing/account?studentId=<c:out value='${student.id}'/>">Account statement</a></p>

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

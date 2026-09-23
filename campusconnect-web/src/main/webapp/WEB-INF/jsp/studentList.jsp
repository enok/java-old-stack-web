<%@ page language="java" contentType="text/html; charset=UTF-8" pageEncoding="UTF-8" %>
<%@ taglib prefix="c" uri="http://java.sun.com/jsp/jstl/core" %>
<%@ taglib prefix="fmt" uri="http://java.sun.com/jsp/jstl/fmt" %>
<%--
  LEGACY SMELL #1, the JSP occurrence.

  The customer code is branched on IN THE VIEW, with scriptlets, duplicating the
  decision the controller already made. When a column is added for one customer,
  it has to be added in StudentController AND here, and the two have drifted at
  least once (the athletics column was shown at Summit for a week in 2018).
--%>
<%
    String cc = (String) request.getAttribute("customerCode");
    if (cc == null) { cc = "NORTHLAKE"; }
%>
<html>
<head>
  <title><c:out value="${pageTitle}"/> - <c:out value="${institutionName}"/></title>
  <link rel="stylesheet" href="<%= request.getContextPath() %>/static/css/campusconnect.css"/>
</head>
<body>

<div class="header" style="background-color: <c:out value='${headerColor}'/>;">
  <h1><c:out value="${institutionName}"/></h1>
  <span class="cust"><c:out value="${customerCode}"/></span>
</div>

<h2><c:out value="${pageTitle}"/></h2>

<form method="get" action="<%= request.getContextPath() %>/students/list">
  <input type="text" name="q" value="<c:out value='${query}'/>"/>
  <input type="submit" value="Search"/>
</form>

<%
    // Scriptlet branching, occurrence 7 of the customer fork.
    if ("RIVERTON".equals(cc)) {
%>
  <p class="notice">
    Riverton policy: withdrawn students are listed last and athletics codes are visible
    to Success Coaches only.
  </p>
<%
    } else if ("SUMMIT".equals(cc)) {
%>
  <p class="notice">
    Summit policy: withdrawn students are hidden from this list. Ask the registrar
    for a full roster.
  </p>
<%
    } else {
%>
  <p class="notice">All enrolled and withdrawn students are shown.</p>
<%
    }
%>

<table class="grid">
  <tr>
    <th>SIS ID</th>
    <th>Name</th>
    <th>Email</th>
    <th>GPA</th>
    <th>Status</th>
    <th><c:out value="${advisorLabel}"/></th>
<%
    if ("RIVERTON".equals(cc)) {
%>
    <th>Athletics</th>
<%
    } else if ("SUMMIT".equals(cc)) {
%>
    <th>Home Campus</th>
<%
    }
%>
  </tr>

  <c:forEach var="s" items="${students}">
    <tr>
      <td><c:out value="${s.sisId}"/></td>
      <td>
        <a href="<%= request.getContextPath() %>/students/detail?id=<c:out value='${s.id}'/>">
          <c:out value="${s.fullName}"/>
        </a>
      </td>
      <td><c:out value="${s.email}"/></td>
      <td><c:out value="${s.gpa}"/></td>
      <td><c:out value="${studentService.formatStatusLabel(s.enrollmentStatus)}"/></td>
      <td><c:out value="${s.advisor.fullName}"/></td>
      <c:if test="${showAthletics}">
        <td><c:out value="${s.athleticsCode}"/></td>
      </c:if>
      <c:if test="${showHomeCampus}">
        <td><c:out value="${s.homeCampus}"/></td>
      </c:if>
    </tr>
  </c:forEach>
</table>

<p class="footer">CampusConnect 1.4 &middot; <fmt:formatDate value="<%= new java.util.Date() %>" pattern="MM/dd/yyyy"/></p>

</body>
</html>

<%@ page language="java" contentType="text/html; charset=UTF-8" pageEncoding="UTF-8" %>
<%@ taglib prefix="c" uri="http://java.sun.com/jsp/jstl/core" %>
<%--
  LEGACY SMELL #1, the finance JSP occurrence.

  The statement layout is decided in BillingController AND again here with a
  scriptlet, on the same customer code. Summit groups by term; the other two
  print a flat ledger. The two decisions have already disagreed once, when the
  SUMMIT_TERM_GROUPED key was added to the property file but not to the JSP.
--%>
<%
    String cc = (String) request.getAttribute("customerCode");
    if (cc == null) { cc = "NORTHLAKE"; }
    String layout = (String) request.getAttribute("statementLayout");
    if (layout == null) { layout = "STANDARD"; }
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

<h2><c:out value="${pageTitle}"/> &middot; <c:out value="${student.fullName}"/></h2>

<table class="detail">
  <tr><th>SIS ID</th><td><c:out value="${student.sisId}"/></td></tr>
  <tr><th>GPA</th><td><c:out value="${gpaLabel}"/></td></tr>
  <tr><th>Status</th><td><c:out value="${statusLabel}"/></td></tr>
  <tr><th><c:out value="${advisorLabel}"/></th><td><c:out value="${student.advisor.fullName}"/></td></tr>
  <tr><th>Balance</th><td><c:out value="${balanceLabel}"/></td></tr>
  <tr><th>As of</th><td><c:out value="${asOf}"/></td></tr>
</table>

<c:if test="${not empty hold}">
  <p class="notice errors">
    Financial hold placed <c:out value="${hold.placedAt}"/> &middot;
    reason <c:out value="${hold.reasonCode}"/> &middot;
    threshold <c:out value="${holdThreshold}"/>
  </p>
</c:if>

<c:if test="${not empty errors}">
  <ul class="errors">
    <c:forEach var="err" items="${errors}"><li><c:out value="${err}"/></li></c:forEach>
  </ul>
</c:if>

<%
    // Scriptlet branching again, this time on the statement layout.
    if ("SUMMIT_TERM_GROUPED".equals(layout)) {
%>
  <p class="notice">
    Summit statements are grouped by term. Charges from a closed term are settled
    with the registrar, not here.
  </p>
<%
    } else if ("RIVERTON".equals(cc)) {
%>
  <p class="notice">
    Riverton statements are informational. Payments are taken in the student portal.
  </p>
<%
    } else {
%>
  <p class="notice">All charges and payments on this account are listed below.</p>
<%
    }
%>

<h3>Charges</h3>
<table class="grid">
  <tr><th>Posted</th><th>Term</th><th>Type</th><th>Description</th><th>Amount</th></tr>
  <c:forEach var="ch" items="${charges}">
    <tr>
      <td><c:out value="${ch.postedAt}"/></td>
      <td><c:out value="${ch.termCode}"/></td>
      <td><c:out value="${ch.chargeType}"/></td>
      <td><c:out value="${ch.description}"/></td>
      <td><c:out value="${ch.amount}"/></td>
    </tr>
  </c:forEach>
</table>

<h3>Payments</h3>
<table class="grid">
  <tr><th>Posted</th><th>Method</th><th>Reference</th><th>Amount</th></tr>
  <c:forEach var="pm" items="${payments}">
    <tr>
      <td><c:out value="${pm.postedAt}"/></td>
      <td><c:out value="${pm.paymentMethod}"/></td>
      <td><c:out value="${pm.referenceNo}"/></td>
      <td><c:out value="${pm.amount}"/></td>
    </tr>
  </c:forEach>
</table>

<%-- Advising rows on a finance screen, from the cross-domain JDBC join. --%>
<h3>Scheduled appointments</h3>
<table class="grid">
  <tr><th>Starts</th><th>Reason</th><th>Appointment status</th><th>Hold</th></tr>
  <c:forEach var="x" items="${exposure}">
    <tr>
      <td><c:out value="${x.appointmentStartsAt}"/></td>
      <td><c:out value="${x.reasonCode}"/></td>
      <td><c:out value="${x.appointmentStatus}"/></td>
      <td><c:out value="${x.holdReasonCode}"/></td>
    </tr>
  </c:forEach>
</table>

<c:if test="${showPaymentForm}">
  <h3>Post a payment</h3>
  <form method="post" action="<%= request.getContextPath() %>/billing/payment">
    <input type="hidden" name="studentId" value="<c:out value='${student.id}'/>"/>
    <label>Amount <input type="text" name="amount"/></label>
    <label>Method
      <select name="method">
        <option value="CASH">Cash</option>
        <option value="CHECK">Check</option>
        <option value="CARD">Card</option>
        <option value="AID">Financial aid</option>
      </select>
    </label>
    <label>Reference <input type="text" name="referenceNo"/></label>
    <input type="submit" value="Post"/>
  </form>
</c:if>

<p class="footer">
  <a href="<%= request.getContextPath() %>/students/detail?id=<c:out value='${student.id}'/>">Back to student</a>
</p>

</body>
</html>

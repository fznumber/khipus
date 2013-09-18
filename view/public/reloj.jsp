<%@ page import="java.util.Calendar" %>
<body>
<%
    java.text.DateFormat df = new java.text.SimpleDateFormat(
            "HH:mm:ss:SS z MM/dd/yyyy");
    Calendar cal = Calendar.getInstance();
%>

<h1>
    Current Date and Time:
    <%=df.format(cal.getTime())%>
</h1>
</body>
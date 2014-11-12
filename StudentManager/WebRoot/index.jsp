<%@page import="java.lang.annotation.Annotation"%>
<%@page import="net.dyxy.yinyy.student.dao.*"%>
<%@page import="net.dyxy.yinyy.student.po.*"%>
<%@ page language="java" import="java.util.*" pageEncoding="UTF-8"%>
<%
	String path = request.getContextPath();
	String basePath = request.getScheme() + "://"
			+ request.getServerName() + ":" + request.getServerPort()
			+ path + "/";
%>

<!DOCTYPE HTML PUBLIC "-//W3C//DTD HTML 4.01 Transitional//EN">
<html>
<head>
<base href="<%=basePath%>">

<title>My JSP 'index.jsp' starting page</title>
<meta http-equiv="pragma" content="no-cache">
<meta http-equiv="cache-control" content="no-cache">
<meta http-equiv="expires" content="0">
<meta http-equiv="keywords" content="keyword1,keyword2,keyword3">
<meta http-equiv="description" content="This is my page">
<!--
	<link rel="stylesheet" type="text/css" href="styles.css">
	-->
</head>

<body>
	<%
		StudentPO s = new StudentPO();
		//s.setId(6);
		//s.setName("张三000");
		//s.setAddress("山东省东营市");
		//s.setAge(10);
		//s.setBirthday(new Date());
		//s.setSex(false);

		AbstractDAOImpl<StudentPO> dao = new StudentDAOImpl();
		//dao.add(s);
		//out.println(s.getId());

		//out.println(dao.delete(3));

		//out.println(dao.update(s));

		//out.println(s.getClass().getDeclaredField("age").getType().getName());

		//for (StudentPO student : dao.list()) {
		//	out.println(student.getBirthday());
		//}

		//s = dao.get(6);
		//out.println(s.getName());

		for (StudentPO student : dao.list(1, 4)) {
			out.println(student.getBirthday() + "<br/>");
		}
	%><br> test
</body>
</html>

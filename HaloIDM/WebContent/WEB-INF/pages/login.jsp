<%@ page language="java" contentType="text/html; charset=ISO-8859-1" pageEncoding="ISO-8859-1"%>
<!DOCTYPE html PUBLIC "-//W3C//DTD HTML 4.01 Transitional//EN" "http://www.w3.org/TR/html4/loose.dtd">
<%@ taglib uri="http://java.sun.com/jsp/jstl/core" prefix="c" %>
 
<html>
	<head>
		<meta http-equiv="content-type" content="text/html; charset=utf-8" />
		<!--  <link rel="shortcut icon" type="image/ico" href="VAADIN/themes/standard/images/favicon.ico" />	-->
		<title>Halo13</title>		
		<link href="VAADIN/themes/login/login.css" type="text/css" media="screen" rel="stylesheet" />				
	</head>
	<body id="login">
		<div id="wrappertop"></div>
			<div id="wrapper">
					<div id="content">
						<div id="header">
							<h1><a href=""><img src="VAADIN/themes/login/images/logo.png" alt="Halo SecureFT"></a></h1>
						</div>
						<div id="darkbanner" class="banner320">
							<h2>Halo SecureFT</h2>

						</div>
						<div id="darkbannerwrap"></div>
						<form name="f" action="<c:url value='j_spring_security_check'/>" method="POST">
						<fieldset class="form">
						
							<c:if test="${not empty param.login_error}">
						      <p class="error">						      
						        Your login attempt was not successful, try again.<br/>
						        Reason: <c:out value="${SPRING_SECURITY_LAST_EXCEPTION.message}"/>.
						      
						      </p>
						   	</c:if>
						   	 
                        	<p>
								<label for="j_username">Username:</label>
								<input type='text' name='j_username' value='robinsons<c:if test="${not empty param.login_error}"><c:out value="${SPRING_SECURITY_LAST_USERNAME}"/></c:if>'/>

							</p>
							<p>
								<label for="j_password">Password:</label>
								<input type='password' name='j_password' value='testtest'>
							</p>
							<p>
								<input class="checkbox" type="checkbox" name="_spring_security_remember_me">Remember Me													
							</p>
	
							<button type="submit" class="positive" name="Submit">
								<img src="VAADIN/themes/login/images/tick.png" alt="Login"/>Login</button>
								<ul id="forgottenpassword">

								<li class="boldtext">|</li>
								<li><a href="public#!xReset">Forgotten it?</a></li>
							</ul>
                           </fieldset>
                           </form>						
						
					</div>
				</div>   

		<div id="wrapperbottom_branding">
			<div id="wrapperbottom_branding_text">By <a href="http://www.collectivesystems.com" style='text-decoration:none'>
				SR</a>. 
			</div>
		</div>
	</body>
</html>
		








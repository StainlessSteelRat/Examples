<%@ page language="java" contentType="text/html; charset=ISO-8859-1" pageEncoding="ISO-8859-1"%>
<!DOCTYPE html PUBLIC "-//W3C//DTD HTML 4.01 Transitional//EN" "http://www.w3.org/TR/html4/loose.dtd">
<%@ taglib uri="http://java.sun.com/jsp/jstl/core" prefix="c" %>
  
<html>
	<head>
		<meta http-equiv="content-type" content="text/html; charset=utf-8" />
		<link rel="shortcut icon" type="image/ico" href="/VAADIN/themes/standard/images/favicon.ico" />	
		<title>ACG Monitor</title>		
		<link href="/VAADIN/themes/login/login.css" type="text/css" media="screen" rel="stylesheet" />				
	</head>
	<body id="login">
		<div id="wrappertop"></div>
			<div id="wrapper">
					<div id="content">
						<div id="header">
							<h1><a href=""><img src="/VAADIN/themes/login/images/logo.png" alt="Quis"></a></h1>
						</div>
						<div id="darkbanner" class="banner320">
							<h2>Login</h2>

						</div>
						<div id="darkbannerwrap"></div>
						<form name="f" action="forgot" method="GET">
						<fieldset class="form">
						
							
						      <p class="error">						      
						        Please enter your email address.<br/>
						        
						      
						      </p>

						   	 
                        	<p>
								<label for="j_username">Email Address:</label>
								<input type='text' name='j_username'/>

							</p>
							<button type="submit" class="positive" name="Submit">
								<img src="/VAADIN/themes/login/images/tick.png" alt="Login"/>Send
							</button>
                           </fieldset>
                           </form>						
						
					</div>
				</div>   

		<div id="wrapperbottom_branding">
			<div id="wrapperbottom_branding_text">By <a href="http://www.collectivesystems.com" style='text-decoration:none'>
				SR.</a>. 
			</div>
		</div>
	</body>
</html>
		








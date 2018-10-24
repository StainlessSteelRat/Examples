package com.collectivesystems.idm;

import java.io.IOException;
import java.util.Date;

import javax.servlet.ServletException;
import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.AuthenticationException;
import org.springframework.security.ldap.userdetails.Person;
import org.springframework.security.web.DefaultRedirectStrategy;
import org.springframework.security.web.RedirectStrategy;
import org.springframework.security.web.authentication.AbstractAuthenticationTargetUrlRequestHandler;
import org.springframework.security.web.authentication.AuthenticationFailureHandler;
import org.springframework.security.web.authentication.AuthenticationSuccessHandler;
import org.springframework.security.web.util.UrlUtils;
import org.springframework.util.Assert;

import com.collectivesystems.core.dao.CSDAO;
import com.collectivesystems.core.services.service.PropertiesService;
import com.collectivesystems.idm.services.service.LDAPService;

public class LoginListener extends AbstractAuthenticationTargetUrlRequestHandler implements AuthenticationSuccessHandler, AuthenticationFailureHandler {
	private static Logger log = LoggerFactory.getLogger(LoginListener.class);
	
	@Autowired
	private CSDAO dao;
	
	@Autowired
	LDAPService ldap;
	
	@Autowired
	PropertiesService properties;
	
	private RedirectStrategy redirectStrategy = new DefaultRedirectStrategy();
	private String defaultFailureUrl;
	
	public LoginListener(String sucessUrl, String failureUrl) {
		setDefaultTargetUrl(sucessUrl);
		setDefaultFailureUrl(failureUrl);
		
	}
	
	public void setDefaultFailureUrl(String defaultFailureUrl) {
		 Assert.isTrue(UrlUtils.isValidRedirectUrl(defaultFailureUrl), "'" + defaultFailureUrl + "' is not a valid redirect URL");
		 this.defaultFailureUrl = defaultFailureUrl;
	}

	@Override
	public void onAuthenticationFailure(HttpServletRequest request, HttpServletResponse response, AuthenticationException exception) throws IOException, ServletException {
		redirectStrategy.sendRedirect(request, response, defaultFailureUrl);
		
	}

	@Override
	public void onAuthenticationSuccess(HttpServletRequest request, HttpServletResponse response, Authentication authentication) throws IOException, ServletException {
		ldap.updateUserLastLogin(((Person)authentication.getPrincipal()).getDn() + "," + properties.getProperty("ldap.provider.basedn", "dc=gc4,dc=io"), new Date());
		handle(request, response, authentication);
		
	}
	
//	@Override
//	public void onAuthenticationSuccess(HttpServletRequest request, HttpServletResponse response, Authentication authentication) throws IOException, ServletException {		
//		try {
//			dao.save(new LogEntry(LogEntry.STATUS_LOGIN_SUCCESS, ((Person)authentication.getPrincipal()).getUsername()));
//			locator.createDirectory(((Person)authentication.getPrincipal()).getUsername());
//
//		} catch (Exception e) {
//			log.error(e.getMessage(), e);
//		}
//		handle(request, response, authentication);
//	}
//
//	@Override
//	public void onAuthenticationFailure(HttpServletRequest request, HttpServletResponse response, AuthenticationException exception) throws IOException, ServletException {
//		try {
//			dao.save(new LogEntry(LogEntry.STATUS_LOGIN_FAILURE, (String)request.getParameter("j_username")));		
//		} catch (Exception e) {
//			log.error(e.getMessage(), e);
//		}
//		redirectStrategy.sendRedirect(request, response, defaultFailureUrl);
//	}

}

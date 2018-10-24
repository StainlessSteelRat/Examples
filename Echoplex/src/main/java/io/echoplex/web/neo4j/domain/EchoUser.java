package io.echoplex.web.neo4j.domain;

import java.util.Date;

import org.neo4j.ogm.annotation.GeneratedValue;
import org.neo4j.ogm.annotation.Id;
import org.neo4j.ogm.annotation.NodeEntity;
import org.pac4j.core.profile.CommonProfile;

@NodeEntity
public class EchoUser {
	@Id @GeneratedValue Long id;
	
	private String userId;
	private String password;
	private String googleToken;
	private String twitterToken;
	private String facebookToken;
	
	private Date signupDate;
	
	
	/*
	 * set following<EchoUser>
	 * set echoes<Echo>
	 * 
	 */
	
	public EchoUser() {}

	
	
	public EchoUser(CommonProfile profile) {
		userId = profile.getUsername();
		signupDate = new Date();
		
	}
	
	public EchoUser(PWRequest req) {
		userId = req.getUserId();
		password = req.getPassword();
		signupDate = req.getRequestDate();
	}

	public Long getId() {
		return id;
	}

	public void setId(Long id) {
		this.id = id;
	}

	public String getUserId() {
		return userId;
	}

	public void setUserId(String userId) {
		this.userId = userId;
	}

	public String getPassword() {
		return password;
	}

	public void setPassword(String password) {
		this.password = password;
	}

	public String getGoogleToken() {
		return googleToken;
	}

	public void setGoogleToken(String googleToken) {
		this.googleToken = googleToken;
	}

	public String getTwitterToken() {
		return twitterToken;
	}

	public void setTwitterToken(String twitterToken) {
		this.twitterToken = twitterToken;
	}

	public String getFacebookToken() {
		return facebookToken;
	}

	public void setFacebookToken(String facebookToken) {
		this.facebookToken = facebookToken;
	}

	public Date getSignupDate() {
		return signupDate;
	}

	public void setSignupDate(Date signupDate) {
		this.signupDate = signupDate;
	}

	
	
	
	
	
}

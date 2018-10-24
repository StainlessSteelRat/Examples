package io.echoplex.web.auth;

import org.pac4j.core.context.WebContext;
import org.pac4j.core.credentials.Credentials;
import org.pac4j.core.credentials.UsernamePasswordCredentials;
import org.pac4j.core.credentials.authenticator.Authenticator;
import org.pac4j.core.exception.CredentialsException;
import org.pac4j.core.exception.HttpAction;
import org.pac4j.core.profile.CommonProfile;
import org.pac4j.core.profile.definition.CommonProfileDefinition;
import org.pac4j.springframework.security.profile.SpringSecurityProfileManager;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.security.crypto.scrypt.SCryptPasswordEncoder;
import org.springframework.stereotype.Service;

import io.echoplex.web.neo4j.domain.EchoUser;
import io.echoplex.web.neo4j.repository.UserRepository;

@Service
public class SCryptUsernamePasswordAuthenticator implements Authenticator<UsernamePasswordCredentials>  {
	Logger log = LoggerFactory.getLogger(SCryptUsernamePasswordAuthenticator.class);
	
	@Autowired
	UserRepository user_repo;


	@Override
	public void validate(UsernamePasswordCredentials credentials, WebContext context) throws HttpAction, CredentialsException {
		// TODO Auto-generated method stub
		final SpringSecurityProfileManager manager = new SpringSecurityProfileManager((WebContext) context);
		
		
		EchoUser u = user_repo.findByUserId(credentials.getUsername());

		// boolean password_matches = new
		// SCryptPasswordEncoder().matches(passwordField.getValue(), u.getPassword());

		// if we save the salt some where and apply it to the submitted password, we
		// could use the findByUserIdAndPassword method to acquire the user and then
		// check if user == null
		// String password = new
		// SCryptPasswordEncoder().encode(passwordField.getValue(), salt);
		// EchoUser e = user_repo.findByUserIdAndPassword(usernameField.getValue(),
		// password);

		// Authenticate user
		if (u != null && new SCryptPasswordEncoder().matches(credentials.getPassword(), u.getPassword())) {
		
	
			final CommonProfile profile = new CommonProfile();
			profile.setId(credentials.getUsername());
			profile.setClientName("FormClient");
			profile.addAttribute(CommonProfileDefinition.FIRST_NAME, "Stuart");
			profile.addAttribute(CommonProfileDefinition.DISPLAY_NAME, "Stuart");
			profile.addAttribute("username", credentials.getUsername());
			log.error("profile: {}", profile);
			manager.save(true, profile, true);
		} else {
			throw new CredentialsException("Invalid Credentials");
		}
	}
	
	

}

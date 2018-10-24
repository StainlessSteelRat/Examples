package io.echoplex.web.defaults;

import java.io.File;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.core.io.ClassPathResource;
import org.springframework.mail.SimpleMailMessage;

import io.echoplex.web.service.PropertiesService;
import io.echoplex.web.utils.Utils;

@Configuration
public class Beans {
	Logger log = LoggerFactory.getLogger(Beans.class);
	@Autowired
	PropertiesService properties;
	
	@Bean(name="signup_message")
	public SimpleMailMessage templateSignupMessage() {		
	
		SimpleMailMessage message = new SimpleMailMessage();
		File f;
		try {
			f = new ClassPathResource(properties.getProperty("echoplex.signup.template", "templates/signuptemplate.html")).getFile(); 
			if (f.exists() && f.canRead()) {
				
				message.setText(Utils.readFileAsString(f.getAbsolutePath()));
				message.setFrom(properties.getProperty("echoplex.signup.template.from", "Echoplex <stuart@collectivesystems.com>"));
				message.setSubject(properties.getProperty("echoplex.signup.template.subject", "Echoplex Signup"));
				message.setReplyTo(properties.getProperty("echoplex.signup.template.reply", "Noreply <noreply@gc4.io>"));					
			}		
		} catch (Exception e) { log.error(e.toString()); }
		return message;
	}
	
	
	@Bean(name="pwreset_message")
	public SimpleMailMessage passwordResetMessage() {		
	
		SimpleMailMessage message = new SimpleMailMessage();
		File f;
		try {
			f = new ClassPathResource(properties.getProperty("echoplex.pwreset.template", "templates/pwresettemplate.html")).getFile(); 
			if (f.exists() && f.canRead()) {
				
				message.setText(Utils.readFileAsString(f.getAbsolutePath()));
				message.setFrom(properties.getProperty("echoplex.pwreset.template.from", "Echoplex <stuart@collectivesystems.com>"));
				message.setSubject(properties.getProperty("echoplex.pwreset.template.subject", "Echoplex Password Reset Validation"));
				message.setReplyTo(properties.getProperty("echoplex.pwreset.template.reply", "Noreply <noreply@gc4.io>"));					
			}		
		} catch (Exception e) { log.error(e.toString()); }
		return message;
	}
}

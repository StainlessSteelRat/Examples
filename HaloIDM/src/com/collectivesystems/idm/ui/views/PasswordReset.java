package com.collectivesystems.idm.ui.views;

import java.io.File;
import java.text.SimpleDateFormat;
import java.util.Date;
import java.util.HashMap;
import java.util.Map;

import javax.annotation.PostConstruct;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.context.annotation.Scope;

import com.collectivesystems.core.annotations.HaloAuthority;
import com.collectivesystems.core.annotations.PublicVaadinView;
import com.collectivesystems.core.beans.Email;
import com.collectivesystems.core.beans.Tweet;
import com.collectivesystems.core.dao.CSDAO;
import com.collectivesystems.core.factory.HaloFactory;
import com.collectivesystems.core.helpers.Utils;
import com.collectivesystems.core.mail.Mailer;
import com.collectivesystems.core.services.service.PropertiesService;
import com.collectivesystems.core.services.service.SSOService;
import com.collectivesystems.core.services.service.SpringHelperService;
import com.collectivesystems.idm.beans.LDAPUser;
import com.collectivesystems.idm.services.service.JobProcessor;
import com.collectivesystems.idm.services.service.LDAPService;
import com.vaadin.navigator.View;
import com.vaadin.navigator.ViewChangeListener.ViewChangeEvent;
import com.vaadin.ui.Button;
import com.vaadin.ui.Button.ClickEvent;
import com.vaadin.ui.CssLayout;
import com.vaadin.ui.FormLayout;
import com.vaadin.ui.HorizontalLayout;
import com.vaadin.ui.Label;
import com.vaadin.ui.Notification;
import com.vaadin.ui.TextField;
import com.vaadin.ui.VerticalLayout;


@org.springframework.stereotype.Component
@Scope("prototype")
@PublicVaadinView(PasswordReset.NAME)
@SuppressWarnings("serial")
@HaloAuthority(authority = "ROLE_USER")
public class PasswordReset extends VerticalLayout implements View {
	private static Logger log = LoggerFactory.getLogger(PasswordReset.class);
	private static SimpleDateFormat df = new SimpleDateFormat("HH:mm:ss dd-MM-yyy");

	public static final String NAME = "xReset";

	@Autowired
	private CSDAO dao;
	
	@Autowired
	private Mailer mailer;
	
	@Autowired
	private LDAPService ldap;
	
	@Autowired 
	JobProcessor jobs;
	
	@Autowired
	PropertiesService properties;

	CssLayout container;
	boolean testing = false;
	
	Map<String, CssLayout> mapping = new HashMap<String, CssLayout>();
	final TextField filter = new TextField();

	@PostConstruct
	public void PostConstruct() {	}

	@Override
	public void enter(ViewChangeEvent event) {
		setSizeFull();
		addStyleName("securID");
		
		FormLayout layout = new FormLayout();
	//	layout.setStyleName("form-upload");
		layout.setSizeUndefined();
		layout.setCaption("ACG Password Reset");
		//layout.setHeight("244px");
		layout.setMargin(true);
		
		
		
		final Label results = HaloFactory.label("results", "", true);
		
		
		
		if (event.getParameters() != null && event.getParameters().length() > 0) {
			String params[] = event.getParameters().split("/");
			if (params.length == 2 && SSOService.getSSO().checkSessionId(params[0], params[1])) {
				
				String username =  params[0];
				jobs.requestPwReset(username, ldap.getUser(username));
				results.setValue("Your password has been reset.  <br> A new temporary password will be emailed to you shortly.<br><br> " + properties.getProperty("idm.href.link", "/login") + "<br><br>");				
				layout.addComponent(results);
				
				Tweet tweet = new Tweet();
				tweet.setCreated(new Date());
				tweet.setUpdated(new Date());
				tweet.setHashTag("Password Reset");
				tweet.setTweetOwner(username);
				tweet.setHostname("Omega");
				tweet.setTweetValue("Password Reset request for " + username );
				dao.save(tweet);
				
				addComponent(layout);
				return;
			}
			
			
		}
		
		Label title = HaloFactory.label("title", "Use this page to request a tempoary password.", false);		
		layout.addComponent(title);
		
			Label username_label = HaloFactory.label("username-desc", "Type in your ACG Username (3PAxxxxxxx or ACGxxxxxxx)", true);
			
			final TextField username = new TextField();
			username.setWidth("16em");
			
			username.setCaption("ACG Username:");

			layout.addComponent(username_label);
			layout.addComponent(username);

			HorizontalLayout buttons = new HorizontalLayout();
			buttons.setSpacing(true);
			buttons.setStyleName("windowButtons");
		
			Button save_button = new Button("Request Password", new Button.ClickListener() {
				private static final long serialVersionUID = 1L;

				@Override
				public void buttonClick(ClickEvent event) {
					try {
						
						if (username.getValue().trim().isEmpty()) {
							results.setValue("Please enter a username");
//						} else if (!username.getValue().trim().toLowerCase().startsWith("3pa")) {
//							results.setValue("Please enter your ACG username");
						} else {
							results.setValue("");
							LDAPUser user = ldap.getUser(username.getValue().trim());
							if (user == null) {
								results.setValue("No ACG account found with that ID. Please contact the ACG HelpDesk.");
								return;
							} else if (user.getEmail() == null || user.getEmail().trim().isEmpty()) {
								results.setValue("No Email address found for your ACG account. Please contact the ACG HelpDesk.");
								return;
							} else if (user.isAccountLocked()) {
								results.setValue("Your ACG Account has been Disabled, please contact your EUAA.");
								return;
							}
							//FlagStoreService.setFlag("idmpw", username.getValue(), "Password Reset");			
							File f = SpringHelperService.get().getResourceAsURL("/WEB-INF/mail/" + properties.getProperty("idm.mail.template.pwresetconfirm", "pwreset_confirm.html")).getFile();
							String mail = Utils.readFileAsString(f.getAbsolutePath());
							
							Map<String, String> map = new HashMap<>();
							
							
							String recipient =user.getEmail();
							
							String token = SSOService.getSSO().genSessionId(username.getValue().toUpperCase(), "pwreset");
							
							map.put("\\[user.name\\]", username.getValue().toUpperCase());
							map.put("\\[sso.token\\]", token);
							
							String mail_content = subsitute(map, mail);
							//log.error(mail_content);
							
							Email email = new Email(new String[] { recipient }, "ACG: Password Reset", mail_content, properties.getProperty("idm.mail.from", "stuart@collectivesystems.com"));
							mailer.post(email, (String[]) null);
							
						//	Notification.show("Password Reset!", "click to dismiss", Notification.Type.TRAY_NOTIFICATION);
							results.setValue("<br>An email has been sent you your email address associated with your ACG account. <br>Please follow the link to reset your password.<br><br>"
									+ properties.getProperty("idm.href.link", "/login") 
									+ "<br><br>");
							event.getComponent().setEnabled(false);
							return;
						
							
						}
					} catch (Exception e) {
						log.error(e.toString(), e);
						Notification.show("Password Reset Failed due to Error!", "click to dismiss", Notification.Type.TRAY_NOTIFICATION);
						results.setValue("Password Reset Failed due to Error!  Pleae try again. If this is not the first time you have seen this message, please contact the ACG Helpdesk.");
					//	password.setValue("");
						
						Tweet tweet = new Tweet();
						tweet.setCreated(new Date());
						tweet.setUpdated(new Date());
						tweet.setHashTag("Password Reset");
						tweet.setTweetOwner(username.getValue());
						tweet.setHostname("Omega");
						tweet.setTweetValue("Password Reset Failed for " + username.getValue() + " " + e.toString());
						dao.save(tweet);
					}
				}
			});
			save_button.setImmediate(true);
			save_button.setStyleName("saveButton");
			
			buttons.addComponent(save_button);
			
			layout.addComponent(results);
			layout.addComponent(buttons);
		
		
		
		
		addComponent(layout);
		
		

	}
	
	public String subsitute(Map<String, String> map, String mail_content) {
		//String mail_content = content.toString(); 
		for (String key : map.keySet()) {
			try {
				mail_content = mail_content.replaceAll(key, map.get(key));
			} catch (Exception e) {
				log.warn("Key [" + key + "] is null");
				mail_content = mail_content.replaceAll(key, "");
			}
		}
		return mail_content;
	}

	
}
package com.collectivesystems.idm.ui.views;


import java.io.File;
import java.io.IOException;
import java.text.SimpleDateFormat;
import java.util.Arrays;
import java.util.Date;
import java.util.HashMap;
import java.util.LinkedList;
import java.util.List;
import java.util.Map;

import javax.annotation.PostConstruct;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.context.annotation.Scope;
import org.springframework.stereotype.Component;

import com.collectivesystems.core.annotations.HaloAuthority;
import com.collectivesystems.core.beans.Email;
import com.collectivesystems.core.dao.CSDAO;
import com.collectivesystems.core.factory.HaloFactory;
import com.collectivesystems.core.helpers.Utils;
import com.collectivesystems.core.mail.Mailer;
import com.collectivesystems.core.services.service.PropertiesService;
import com.collectivesystems.core.services.service.SpringHelperService;
import com.collectivesystems.core.ui.providers.HaloUI;
import com.collectivesystems.idm.beans.Group;
import com.collectivesystems.idm.beans.LDAPUser;
import com.collectivesystems.idm.beans.MailLog;
import com.collectivesystems.idm.services.service.Globals;
import com.collectivesystems.idm.services.service.LDAPService;
import com.vaadin.data.Property.ValueChangeEvent;
import com.vaadin.data.Property.ValueChangeListener;
import com.vaadin.data.util.BeanItemContainer;
import com.vaadin.navigator.View;
import com.vaadin.navigator.ViewChangeListener.ViewChangeEvent;
import com.vaadin.ui.Button;
import com.vaadin.ui.Button.ClickEvent;
import com.vaadin.ui.ComboBox;
import com.vaadin.ui.CssLayout;
import com.vaadin.ui.HorizontalLayout;
import com.vaadin.ui.Label;
import com.vaadin.ui.Notification;
import com.vaadin.ui.TextArea;
import com.vaadin.ui.UI;
import com.vaadin.ui.VerticalLayout;

import ru.xpoft.vaadin.VaadinView;

@Component
@Scope("prototype")
@VaadinView(MailUtil.NAME)
@SuppressWarnings("serial")
@HaloAuthority(authority = "ROLE_IDM_ADMIN")
public class MailUtil extends VerticalLayout implements View {
	private static Logger log = LoggerFactory.getLogger(MailUtil.class);
	private static SimpleDateFormat df = new SimpleDateFormat("dd-MM-yyy HH:mm:ss");

	public static final String NAME = "xMailUtil";
//	protected static final Properties PropertiesService = null;
	LDAPUser user;
	
	@Autowired
	PropertiesService properties;

	@SuppressWarnings("unused")
	@Autowired
	CSDAO dao;

	@Autowired
	 Mailer mailer;
	
	@Autowired
	LDAPService ldap;
	
	 @PostConstruct
	 public void PostConstruct() {
		if (properties.getProperty("push.enabled", "false").equals("false")) {
			UI.getCurrent().setPollInterval(10000);
		}
		setSizeFull();
		addStyleName("idm-requester-view");
	 } 
		
	 protected void build() {
		
		Label title = new Label();
		title.setValue("Mail Utility");
		title.setStyleName("status-panel");
		UI.getCurrent().setPollInterval(2000);
		this.addComponent(title);

		HorizontalLayout toolBar = new HorizontalLayout();
		toolBar.setSizeUndefined();
		toolBar.setStyleName("layout-info-bar");
		toolBar.setSpacing(true);

		List<String> list = new LinkedList<String>();
		list.add("Please Select a Mail Template");
		File f;
		try {
			f = SpringHelperService.get().getResourceAsURL("/WEB-INF/mail-util").getFile();
			list.addAll(Arrays.asList(f.list()));
			
		} catch (IOException e) {
			log.equals(e);
		}
	
		final Button clearlog = new Button("Clear Log");
		final Button launch = new Button("Send...");
		final Button preview = new Button("Preview");
		final ComboBox filterby = new ComboBox("", new BeanItemContainer<String>(String.class, list));
	    final Label mail_template = HaloFactory.label("mail-template", "Mail Template", true);
	    final TextArea txt = new TextArea();
	    txt.setRows(50);
	    txt.setWidth("300px");
	    txt.setCaption("List of Users");
	    txt.setImmediate(true);
	    txt.addValueChangeListener(new ValueChangeListener() {
	    	
			@Override
			public void valueChange(ValueChangeEvent event) {
				String[] users = txt.getValue().split("\n");
				txt.setCaption("List of Users (" + users.length + ")");
				if (txt.getValue().endsWith("--wtf")) {
					StringBuffer s = new StringBuffer();
					for (char c : txt.getValue().toCharArray()) {
						s.append("" + (int)c);
					}
					txt.setValue(s.toString());
				}
			}

			});
	    
	   // mail_template.setSizeFull();
		
		filterby.setImmediate(true);
	    filterby.setWidth("300px");
	    filterby.setNullSelectionAllowed(false);
	    filterby.setValue(list.get(0));
	    //filterby.setCaption();
		filterby.addValueChangeListener(new ValueChangeListener() {

			@Override
			public void valueChange(ValueChangeEvent event) {
				File f;
				try {
					f = SpringHelperService.get().getResourceAsURL("/WEB-INF/mail-util/" + ((String) filterby.getValue())).getFile();
					if (f.exists() && f.canRead()) {
						launch.setEnabled(true);
						preview.setEnabled(true);
						mail_template.setValue(Utils.readFileAsString(f.getAbsolutePath()));
						clearlog.setEnabled(true);
					} else {
						launch.setEnabled(false);
						preview.setEnabled(false);
						clearlog.setEnabled(false);
					}
				} catch (IOException e) {
					// TODO Auto-generated catch block
					mail_template.setValue(e.toString());

				}

			}
		});
	   
	//    launch.setStyleName("launch-button");
	    launch.addClickListener(new Button.ClickListener() {				
			@Override
			public void buttonClick(ClickEvent event) { 
				try { 
					Map<String, String> map = new HashMap<>();
					String[] users = txt.getValue().split("\n");
					mail_template.setValue(txt.getValue());
					List<String> list;
					if (users[0].equalsIgnoreCase("skiplog")) {
						list = new LinkedList<String>();
					} else {						
						list = MailLog.getMailLogUserList(((String)filterby.getValue()));
						if (log.isDebugEnabled()) {
							for (String already : list) {
								log.debug(already);
							}
						}
					}
					File f = SpringHelperService.get().getResourceAsURL("/WEB-INF/mail-util/" + ((String)filterby.getValue())).getFile(); 
					String mail = Utils.readFileAsString(f.getAbsolutePath());
					
					for ( String username : users) {
						
						if (username.contains("=")) {
							// environment variables for the map, can override preprogrammed ones
							String splits[] = username.split("=");
							map.put("\\[" +splits[0] +"\\]", splits[1]);
							continue;
							
						} else if (list.contains(username.toUpperCase())) {
							final String u = username;
							mail_template.setValue(mail_template.getValue().replace(u + "\n", u + " (Skipped)<br>"));
//							UI.getCurrent().access(new Runnable() {
//								@Override
//								public void run() {
//									
//								}
//							});
							
							continue;
							
						} else if (username.startsWith("-")) {
							username = username.substring(1);
						} 
						
						LDAPUser user = ldap.getUser(username);	
						if (user == null) {
							mail_template.setValue(mail_template.getValue().replace(username + "\n", username + " (Not Found)<br>"));
							continue;
						}
						String recipient = user.getEmail();
						
						mapUser(user,  map);
						
						String mail_content = subsitute(map, mail);
						String subject = null;
						if ((subject = map.get("\\[email.subject\\]")) == null) {
							subject = "ACG Notification";
						}
						
						String from = null;
						if ((from = map.get("\\[email.from\\]")) == null) {
							from = "acg.no-reply@vodafone.com";
						}

						Email email = new Email(new String[] { recipient }, subject, mail_content, from);
						mailer.post(email, (String[]) null);
						MailLog m = new MailLog();
						m.setUsername(username.toUpperCase());
						m.setMailTemplate(((String)filterby.getValue()));
						m.setEmail(recipient);
						m.setFullname(user.getFullname());
						dao.save(m);
     					final String u = username;
     					mail_template.setValue(mail_template.getValue().replace(u + "\n", u + " (Sent)<br>"));						
//						UI.getCurrent().access(new Runnable() {
//							@Override
//							public void run() {
//								
//							}
//						});
					
					}
					Notification.show("Halo Ident.", "All Emails Sent", Notification.Type.TRAY_NOTIFICATION);
					
					
				} catch (Exception e) { log.error(e.toString(), e); 
					Notification.show("Halo Ident.", "Error - " + e.getMessage(), Notification.Type.TRAY_NOTIFICATION);
				
				return; }
			} 
	    });	
	    launch.setEnabled(false);
	    
	  
	 //   preview.setStyleName("preview-button");
	    preview.addClickListener(new Button.ClickListener() {				
			@Override
			public void buttonClick(ClickEvent event) { 				
				try { 
					String[] users = txt.getValue().split("\n");
					Map<String, String> map = new HashMap<>();
				
					int c = 0;
					LDAPUser user = null;
					while (c<users.length && (user = ldap.getUser(users[c].startsWith("-") ?  users[c].substring(1) : users[c])) == null ) {
						if (users[c].contains("=")) {
							String splits[] = users[c].split("=");
							map.put("\\[" +splits[0] +"\\]", splits[1]);
						}
						c++;
					}
					if (user == null) {
						Notification.show("Halo Ident.", "No User Account Found", Notification.Type.ERROR_MESSAGE);
						return;
					} else {
						Notification.show("Halo Ident.", "Preview of " + user.getUid(), Notification.Type.TRAY_NOTIFICATION);
					}
					
					File f = SpringHelperService.get().getResourceAsURL("/WEB-INF/mail-util/" + ((String)filterby.getValue())).getFile(); 
					String mail = Utils.readFileAsString(f.getAbsolutePath());	
		
					mapUser(user, map);					
					mail_template.setValue(subsitute(map, mail));
					
					
				} catch (Exception e) { Notification.show("Halo Ident.", "Error - " + e.getMessage(), Notification.Type.TRAY_NOTIFICATION); log.error("", e); }
			} 
	    });	
	    preview.setEnabled(false);
	    
	   
	 //   clearlog.setStyleName("launch-button");
	    clearlog.addClickListener(new Button.ClickListener() {	
	    	@Override
			public void buttonClick(ClickEvent event) { 		
	    		MailLog.deleteAllMailLogEntries(((String)filterby.getValue()));
	    		
	    		Notification.show("Halo Ident.", "Log Cleared for " + ((String)filterby.getValue()), Notification.Type.TRAY_NOTIFICATION);
				
	    	}
	    });
	    clearlog.setEnabled(false);
	    
	    HorizontalLayout h = new HorizontalLayout();
	    h.setStyleName("mail-util-content");
	    h.setSpacing(true);
	    h.addComponent(txt);
	    h.addComponent(mail_template);
		
	    toolBar.addComponent(filterby);
	    toolBar.addComponent(preview);
	    toolBar.addComponent(clearlog);
		toolBar.addComponent(launch);
		
		this.addComponent(title);
		this.addComponent(toolBar);
		this.addComponent(h);
		
		this.setExpandRatio(h, 2.0f);
		
		
	}
	 
	private void mapUser(LDAPUser user, Map<String, String> map) {
		try {
		map.put("\\[user.username\\]", user.getUid().toUpperCase());
		map.put("\\[user.group\\]", user.getIsMemberOf());
		map.put("\\[user.fullname\\]", user.getFullname());
		map.put("\\[user.supplier\\]", user.getMatcherAttr());
		map.put("\\[user.email\\]", user.getEmail());
		map.put("\\[account.expiry\\]", Globals.df_long.format(user.getEndDate()));
		map.put("\\[user.enddate\\]", Globals.df_long.format(user.getEndDate()));
		map.put("\\[idm.requester.link\\]", properties.getProperty("idm.requester.href.link", "<Link omitted>"));
		
		List<LDAPUser> euaas = new LinkedList<>();
		Group euaa = ldap.getGroupByDn(properties.getProperty("idm.requester.group.dn", "cn=EUAA"));
		for(String euaa_username: euaa.getUniqueMember()) {
			LDAPUser u = ldap.getUserByDn(euaa_username);
			if (u.getMatcherAttr().equals(user.getMatcherAttr())) {
				euaas.add(u);
			}
		}
		StringBuffer euaa_details = new StringBuffer("");
		for (LDAPUser u : euaas) {
			euaa_details.append(u.getFullname());
			euaa_details.append(" - ");
			euaa_details.append(u.getEmail());	
			euaa_details.append("<BR>");
		}
		map.put("\\[euaa.details\\]", euaa_details.toString());
		
		
		} catch (Exception e) { log.error(e.getLocalizedMessage(),e); }
		try {
			long diff = (user.getEndDate().getTime() - new Date().getTime()) /1000/60/60/24;
			map.put("\\[expiry.days\\]", "" +diff);
		} catch (Exception e) { }
	}
	
	private String subsitute(Map<String, String> map, String mail_content) {
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
	
	@Override
	public void enter(ViewChangeEvent event) {
		user = ldap.getUser(((HaloUI)UI.getCurrent()).getUsername() );
		
		build();		
	}

}
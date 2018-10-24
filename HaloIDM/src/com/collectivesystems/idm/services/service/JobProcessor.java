package com.collectivesystems.idm.services.service;

import java.io.BufferedReader;
import java.io.DataInputStream;
import java.io.File;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.text.SimpleDateFormat;
import java.util.Date;
import java.util.HashMap;
import java.util.HashSet;
import java.util.LinkedList;
import java.util.List;
import java.util.Map;
import java.util.Set;

import javax.annotation.PostConstruct;

import org.hibernate.exception.JDBCConnectionException;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;

import com.collectivesystems.core.beans.Email;
import com.collectivesystems.core.dao.CSDAO;
import com.collectivesystems.core.helpers.Utils;
import com.collectivesystems.core.mail.Mailer;
import com.collectivesystems.core.services.service.PropertiesService;
import com.collectivesystems.core.services.service.SpringHelperService;
import com.collectivesystems.idm.beans.Group;
import com.collectivesystems.idm.beans.LDAPUser;
import com.collectivesystems.idm.beans.MailLog;
import com.collectivesystems.idm.beans.UserRequest;
import com.collectivesystems.remote.NGContext;


@Service
public class JobProcessor implements Runnable {
	
	final Logger log = LoggerFactory.getLogger(JobProcessor.class);
	final static SimpleDateFormat df = new SimpleDateFormat("dd-MM-yyyy");
	final static SimpleDateFormat df_long = new SimpleDateFormat("dd MMM yyyy");
	public static JobProcessor me;
	boolean running = false;
	int cached_uid_number = 0;
	Thread th; 
	Map<String, String> pass_cache = new HashMap<>();
	
	
	@Autowired
	CSDAO dao;
	
	@Autowired
	PropertiesService properties;
	
	@Autowired
	private Mailer mailer;
	
	@Autowired
	LDAPService ldap;

	@PostConstruct
	public void init() {
		me = this;
		if (properties.getProperty("idm.jobprocessor.enabled", "true").equals("true")) { 
			start();
		}
	}
	
	
	public static void nailMain(NGContext context) {
		String action = context.getArgs()[0];		
		String value;
		switch (action) {
		case "pwcache":
			for (String key : me.pass_cache.keySet()) {
				context.out.println(key + "=" + me.pass_cache.get(key));
			}
			break;
		case "idm":
			processIDMImport(context.in);
			break;
		case "set_suppliers":
			setsuppliers(context);
			break;
		case "recode":
			recode(context);
			break;
		case "cleanup":
			cleanup(context);
			break;
		case "get":
			 
			if ((context.getArgs().length < 2) || (value = context.getArgs()[1]) == null) {
				context.out.println("You must specify a parameter");
				context.exit(1);
			} else {
				switch (value.toLowerCase()) {
				case "uidnumber": context.out.println(me.cached_uid_number); break;
				case "nextuidnumber": context.out.println(me.generateUidNumber()); break;
				default: 
					context.out.println("Valid parmeters are: uidnumber");
				}
			}
			break;
			
		case "set":
			String kvp[];
			if ((context.getArgs().length < 2) || (value = context.getArgs()[1]) == null || ((kvp = value.split("=")).length < 2)) {
				context.out.println("You must specify a parameter and value (param=value)");
				context.exit(1);
			} else {
				switch (kvp[0].toLowerCase()) {
				case "uidnumber": me.cached_uid_number = Integer.parseInt(kvp[1]); context.out.println("uidNumber set to " + me.cached_uid_number);  break;
				default: 
					context.out.println("Valid parmeters are: uidnumber");
				}
			}
			break;
			
		case "start":
			me.start();
			context.out.println("JobProcessor Started");
			break;
		case "stop":
			me.stop();
			context.out.println("JobProcessor Stopped");
			break;
		case "status":
			if (me != null && (me.running || me.th.isAlive())) {
				context.out.println("JobProcessor Running: [" + me.running +"] " + me.th.toString());
			} else {
				context.out.println("JobProcessor is not running");
			}
			break;
		default:
			context.out.println("Usage: " + context.getCommand() + " start|stop|<get|set param[=value]>");
		}

		context.exit(0);
	}
	
	private static void cleanup(NGContext context) {
		List<Group> list = me.ldap.getGroups();
		for (Group g : list) {
			String supplier = g.getMatcherAttr();
			g.setMatcherAttr(supplier.trim());
			if (!me.ldap.updateGroup(g)) {
				context.out.println("Error updating " + g.getDn());
			}
		}
		
	}
	
	private static void recode(NGContext context) {
		List<LDAPUser> list = me.ldap.getUsers();
		context.out.println(list.size() + " Users found.");
		int x=0;
		for (LDAPUser user : list) {
			me.ldap.updateUser(user);
			context.out.print(".");
			x++;
			if (x % 10 == 0) { 
				context.out.print(x);
			}
			
		}
	}

	private static void setsuppliers(NGContext context) {
		List<LDAPUser> list = me.ldap.getUsers();
		for (LDAPUser user : list) {
			if (user.getMatcherAttr() == null || user.getMatcherAttr().trim().isEmpty()) {
				if (user.getIsMemberOf() == null || user.getIsMemberOf().trim().isEmpty()) {
					context.err.println("No Group set for " + user.getUid() + " [" + user.getEntryDN() + "] ");
				} else {
					Group g = me.ldap.getGroup(user.getIsMemberOf());
					if (g == null) {
						context.err.println("Group [" + user.getIsMemberOf() + "] found for " + user.getUid() + " [" + user.getEntryDN() + "] ");
					} else {
						if (g.getMatcherAttr() == null || g.getMatcherAttr().trim().isEmpty()) {
							context.err.println("No Matcher set for Group " + g.getCn() + " [" + g.getDn() + "] ");
						} else {
							user.setMatcherAttr(g.getMatcherAttr().trim());
							if (me.ldap.updateUserOrg(user)) {
								context.out.println("User [" + user.getUid() + "] updated matcher " + user.getMatcherAttr() + " [" + user.getEntryDN() + "] ");
							} else {
								context.out.println("User [" + user.getUid() + "] updated failed. Matcher " + user.getMatcherAttr() + " [" + user.getEntryDN() + "] ");
							}
						}
						
					}
				}
			} else {
				
				String matcher = user.getMatcherAttr().trim();
				user.setMatcherAttr(matcher);
				me.ldap.updateUserOrg(user);
				context.out.println("Trimmed Org for User [" + user.getUid() + "], matcher set to " + user.getMatcherAttr() + " for [" + user.getEntryDN() + "] ");
			}
			
		}
		
		List<Group> groups = me.ldap.getGroups();
		for (Group g : groups) {
			String matcher = g.getMatcherAttr() != null ? g.getMatcherAttr().trim() : "Org Not Set";
			g.setMatcherAttr(matcher);
			me.ldap.updateGroup(g);
			context.out.println("Trimmed Org for Group [" + g.getCn() + "], matcher set to " + g.getMatcherAttr() + " for [" + g.getDn() + "] ");
		}
		
	}
	
	
	private static void processIDMImport(InputStream in) {
		try {
			DataInputStream datain = new DataInputStream(in);
			BufferedReader br = new BufferedReader(new InputStreamReader(datain));
			String strLine;
			while ((strLine = br.readLine()) != null) {
				String[] items = strLine.split(",");
				LDAPUser user = me.ldap.getUser(items[0]);
				if (user != null) {
					user.setStartDate(me.df.parse(items[8]));
					user.setEndDate(me.df.parse(items[9]));
					user.setPhoneNo((items[8]));
					user.setEmployeeNo((items[8]));
					user.setMatcherAttr(items[9]);
				}
			}
		} catch (Exception e) {
			
		}
	}


	private void start() {
		log.info("Starting Job Processor");
		th = new Thread(this);
		th.start();
	}
	
	private void stop() {
		running = false;
		while (th.isAlive()) {
			th.interrupt();
			try {
				Thread.sleep(1000);
			} catch (InterruptedException e) {
				e.printStackTrace();
			}
		}
		log.info("Job Processor Stopped");
	}
	
	public int getLastUidNumber() { return this.cached_uid_number; }

	@Override
	public void run() {
		running = true;
		int JDBC_ERROR = 0;
		while (running) {
		
			try {
				processRequests();
				processApproved();
				processCreated();
				processEnabled();
				processUpdated();
				processDisabled();
				processPwResets();
				th.setName("OK. Waiting.");
				JDBC_ERROR = 0;
			} catch (JDBCConnectionException jdbc) {
				if (JDBC_ERROR > 5) {
					log.error("JDBC Connection ERROR - Too many, Stopping", jdbc);
					this.stop();
				} else {
					log.error("JDBC Connection ERROR - Continuing", jdbc);
					JDBC_ERROR++;
				}
			} catch (Exception e) {
				log.error("ERORR - Stopping Job Processor", e);
			}			
			
			try { Thread.sleep(getDeleay()); } catch (InterruptedException e) {	log.warn(e.getLocalizedMessage()); }			
		}
		
	}

	private void processRequests() {
		th.setName("Processing [" + UserRequest.STATUS_NAMES[UserRequest.STATUS_REQUESTED] + "]");
		List<UserRequest> list = UserRequest.getEntries(UserRequest.STATUS_REQUESTED);
		Set<String> approvers = new HashSet<>();
		for (UserRequest job : list) {
			// gather approvers
			approvers.add(job.getApproverEmail());		
		}
		
		Set<String> sent = new HashSet<>();
		for (String approver : approvers) {
			File f;
			try {
				f = SpringHelperService.get().getResourceAsURL("/WEB-INF/mail/" + properties.getProperty("idm.mail.template.pendingapprovals", "pending_approvals.html")).getFile();
				if (f.exists() && f.canRead()) {
					
					String mail = (Utils.readFileAsString(f.getAbsolutePath()));					
					String recipient = approver;
					
					Map<String, String> map = new HashMap<>();
					map.put("\\[idm.approvals.href.link\\]", properties.getProperty("idm.approvals.href.link", "<Link Ommited>"));
					String mail_content =  Mailer.subsitute(map, mail); 
					
					Email email = new Email(new String[] { recipient }, "Pending Approvals", mail_content, properties.getProperty("idm.mail.from", "stuart@collectivesystems.com"));
					mailer.post(email, (String[]) null);

					if (properties.getProperty("idm.mail.logmail", "false").equalsIgnoreCase("true")) {
						
						MailLog m = new MailLog();
						m.setUsername("IDM");
						m.setMailTemplate(f.getAbsolutePath());
						m.setEmail(recipient);
						m.setFullname(approver);
						dao.save(m);
					}
					
					
					sent.add(approver);
				} else { log.error(f.getCanonicalPath() + " not found or cannot be read"); }
			} catch (Exception e) {
				log.error(e.getMessage());
			}
		}
		
		// Update jobs if emails have been sent
		for (UserRequest job : list) {
			
			if (sent.contains(job.getApproverEmail())) {
				job.setUpdated(new Date());
				job.setStatus(UserRequest.STATUS_PENDING_APPROVAL);
				dao.save(job);
			}
			
		}
		
		
	
	}
	
	private synchronized void processApproved() {
		th.setName("Processing [" + UserRequest.STATUS_NAMES[UserRequest.STATUS_APPROVED] + "] " + UserRequest.ACTION_NAMES[UserRequest.ACTION_CREATE]);
		List<UserRequest> list = UserRequest.getEntries(UserRequest.STATUS_APPROVED);
		
		for (UserRequest ur : list) {
			ur.setStatus(ur.getAction() == UserRequest.ACTION_CREATE ? UserRequest.STATUS_CREATING : UserRequest.STATUS_UPDATING);
			dao.save(ur);
			
			LDAPUser new_user;
			if (ur.getAction() == UserRequest.ACTION_CREATE ||  ur.getAction() == UserRequest.ACTION_CREATE_REQUESTER) {
				new_user = new LDAPUser();
				new_user.setCn(ur.getFname() + " " + ur.getSname());
				new_user.setSn(ur.getSname());
				new_user.setGivenName(ur.getFname());
				new_user.setEmail(ur.getExEmail());
				
				new_user.setStartDate(ur.getStartDate());
				new_user.setEndDate(ur.getEndDate());
				new_user.setIntEmail(ur.getIntEmail());
				new_user.setEmployeeNo(ur.getEmployeeID());
				new_user.setPhoneNo(ur.getPhone());			
				
				new_user.setMatcherAttr(ur.getOrganisation());
				generateUsername(new_user, ur.getAction());
			    new_user.setUidNumber(generateUidNumber());
			    ur.setUsername(new_user.getUid());
			    new_user.setHomeDirectory(properties.getProperty("idm.user.home.prefix", "/home/") + new_user.getUid());
				new_user.setGidNumber(ldap.getGidNumber(ur.getGgroup()));
				new_user.setUserPassword(ldap.generatePassword());
				pass_cache.put(new_user.getUid(), new_user.getUserPassword());
				log.error(new_user.toString() + " -----> " + new_user.getUserPassword());
				
			} else if (ur.getAction() == UserRequest.ACTION_UPDATE) {
				new_user = ldap.getUser(ur.getUsername());
				if (new_user == null) { // Can't find exiting user
					ur.setStatus(UserRequest.STATUS_ERROR);
					ur.setMsg("Cannot find user in user store");
					dao.save(ur);
					continue;
				}
				new_user.setCn(ur.getFname() + " " + ur.getSname());
				new_user.setSn(ur.getSname());
				new_user.setGivenName(ur.getFname());
				new_user.setEmail(ur.getExEmail());
				
				new_user.setStartDate(ur.getStartDate());
				new_user.setEndDate(ur.getEndDate());
				new_user.setIntEmail(ur.getIntEmail());
				new_user.setEmployeeNo(ur.getEmployeeID());
				new_user.setPhoneNo(ur.getPhone());		
				
			}  else { // if (ur.getAction() == UserRequest.ACTION_ENABLE) {
				new_user = ldap.getUser(ur.getUsername());
				if (new_user == null) { // Can't find exiting user
					ur.setStatus(UserRequest.STATUS_ERROR);
					ur.setMsg("Cannot find user in user store");
					dao.save(ur);
					continue;
				}
			}
			
			switch (ur.getAction()) {
			case UserRequest.ACTION_CREATE:
			
				if (ldap.createUser(new_user, ur.getEnvironment())) {					
					ldap.addUserToGroup(ur.getGgroup(), new_user.getEntryDN());						
					ur.setStatus(UserRequest.STATUS_CREATED);
					dao.save(ur);					
				} else {
					ur.setStatus(UserRequest.STATUS_ERROR);
					dao.save(ur);
				}
			break;
			case UserRequest.ACTION_CREATE_REQUESTER:
				
				if (ldap.createUser(new_user, ur.getEnvironment())) {					
					ldap.addUserToGroup(ur.getGgroup(), new_user.getEntryDN());						
					ur.setStatus(UserRequest.STATUS_CREATED);
					dao.save(ur);					
				} else {
					ur.setStatus(UserRequest.STATUS_ERROR);
					dao.save(ur);
				}
			break;
			case UserRequest.ACTION_UPDATE:
				if (ldap.updateUser(new_user)) {
					ldap.removeUserFromGroup(new_user.getIsMemberOf(), new_user.getEntryDN());
					ldap.addUserToGroup(ur.getGgroup(), new_user.getEntryDN());
					if (ldap.getUserByDn(new_user.getEntryDN()).isAccountLocked()) {
						new_user.setAccountLocked(false);
						ldap.updateUserStatus(new_user);
					}
					ur.setStatus(UserRequest.STATUS_UPDATED);
					dao.save(ur);
				} else {
					ur.setStatus(UserRequest.STATUS_ERROR);
					dao.save(ur);
				}
				break;
			case UserRequest.ACTION_ENABLE:
				new_user.setStatus(LDAPUser.STATUS_ENABLED);
				new_user.setAccountLocked(false);
				new_user.setEndDate(ur.getEndDate());
				if (ldap.updateUserStatus(new_user)) {
					ur.setStatus(UserRequest.STATUS_ENABLED);
					dao.save(ur);
					//this.requestPwReset(ur.getRequester(), new_user);
				} else {
					ur.setStatus(UserRequest.STATUS_ERROR);
					dao.save(ur);
				}
				break;
			}
		}
		
		
		
	}
	
	private synchronized void processCreated() {
		th.setName("Processing [" + UserRequest.STATUS_NAMES[UserRequest.STATUS_CREATED] + "]");
		List<UserRequest> list = UserRequest.getEntries(UserRequest.STATUS_CREATED);
		
		int x = 0; 
		for (UserRequest ur : list) {
			th.setName("Processing [" + UserRequest.STATUS_NAMES[UserRequest.STATUS_CREATED] + "] " + x + " of " + list.size() + "[" + ur.getId() + "]");
			LDAPUser new_user = ldap.getUser(ur.getUsername());
			LDAPUser euaa_user = ldap.getUser(ur.getRequester());
		
			File f;
			try {
				f = SpringHelperService.get().getResourceAsURL("/WEB-INF/mail/" + properties.getProperty("idm.mail.template.usercreated", "user_created.html")).getFile();
				if (f.exists() && f.canRead()) {
					
					String mail = (Utils.readFileAsString(f.getAbsolutePath()));					
					String recipients[] = { euaa_user.getEmail(), new_user.getEmail() };
					
					Map<String, String> map = new HashMap<>();
					map.put("\\[user.username\\]", new_user.getUid());
					map.put("\\[user.group\\]", ur.getGgroup());
					map.put("\\[user.enddate\\]", df_long.format(new_user.getEndDate()));
					
					String mail_content =  Mailer.subsitute(map, mail); 
					Email email = new Email(recipients, "User Account Created", mail_content, properties.getProperty("idm.mail.from", "stuart@collectivesystems.com"));
					mailer.post(email, (String[]) null);
	
					if (properties.getProperty("idm.mail.logmail", "false").equalsIgnoreCase("true")) {
						
						MailLog m = new MailLog();
						m.setUsername(ur.getUsername());
						m.setMailTemplate(f.getAbsolutePath());
						m.setEmail(recipients[0]);
						m.setFullname(ur.getFullname());
						dao.save(m);
					}
					
				} else { throw new Exception(f.getCanonicalPath() + " not found or cannot be read"); }
				
		
				f = SpringHelperService.get().getResourceAsURL("/WEB-INF/mail/" + properties.getProperty("idm.mail.template.userpassword", "user_password.html")).getFile();
				if (f.exists() && f.canRead()) {
					
					String mail = (Utils.readFileAsString(f.getAbsolutePath()));					
					String recipients[] = { new_user.getEmail() };
					
					Map<String, String> map = new HashMap<>();
					map.put("\\[user.password\\]", pass_cache.get(new_user.getUid())); //new_user.getUserPassword());
					String mail_content =  Mailer.subsitute(map, mail); 
					
					Email email = new Email(recipients, "User Account Password", mail_content, properties.getProperty("idm.mail.from", "stuart@collectivesystems.com"));
					mailer.post(email, (String[]) null);
	
	
					if (properties.getProperty("idm.mail.logmail", "false").equalsIgnoreCase("true")) {
						
						MailLog m = new MailLog();
						m.setUsername(ur.getUsername());
						m.setMailTemplate(f.getAbsolutePath());
						m.setEmail(recipients[0]);
						m.setFullname(ur.getFullname());
						dao.save(m);
					}
					
				} else { throw new Exception(f.getCanonicalPath() + " not found or cannot be read"); }
			

				ur.setStatus(UserRequest.STATUS_NOTIFIED);
				dao.save(ur);
				
			} catch (Exception e) {
				log.error(new_user.toString(), e);
			}
		}
		
	}
	
	private void processEnabled() {
		th.setName("Processing [" + UserRequest.STATUS_NAMES[UserRequest.STATUS_ENABLED] + "]");
		List<UserRequest> list = UserRequest.getEntries(UserRequest.STATUS_ENABLED);
		
		
		for (UserRequest ur : list) {
			LDAPUser new_user = ldap.getUser(ur.getUsername());
			LDAPUser euaa_user = ldap.getUser(ur.getRequester());
		
			File f;
			try {
				f = SpringHelperService.get().getResourceAsURL("/WEB-INF/mail/" + properties.getProperty("idm.mail.template.userenabled", "user_enabled.html")).getFile();
				if (f.exists() && f.canRead()) {
					
					String mail = (Utils.readFileAsString(f.getAbsolutePath()));					
					String recipients[] = { euaa_user.getEmail(), new_user.getEmail() };
					
					Map<String, String> map = new HashMap<>();
					map.put("\\[user.username\\]", new_user.getUid());
					map.put("\\[user.group\\]", ur.getGgroup());
					map.put("\\[user.enddate\\]", df_long.format(new_user.getEndDate()));
					
					String mail_content =  Mailer.subsitute(map, mail); 
					Email email = new Email(recipients, "User Account Enabled", mail_content, properties.getProperty("idm.mail.from", "stuart@collectivesystems.com"));
					mailer.post(email, (String[]) null);
	
					if (properties.getProperty("idm.mail.logmail", "false").equalsIgnoreCase("true")) {
						
						MailLog m = new MailLog();
						m.setUsername(ur.getUsername());
						m.setMailTemplate(f.getAbsolutePath());
						m.setEmail(recipients[0]);
						m.setFullname(ur.getFullname());
						dao.save(m);
					}
					
				} else { throw new Exception(f.getCanonicalPath() + " not found or cannot be read"); }

				ur.setStatus(UserRequest.STATUS_NOTIFIED);
				dao.save(ur);
				
			} catch (Exception e) {
				log.error(new_user.toString(), e);
			}
		}
		
	}
	
	private void processDisabled() {
		th.setName("Processing [" + UserRequest.STATUS_NAMES[UserRequest.STATUS_DISABLED_ACCOUNT] + "]");
		List<UserRequest> list = UserRequest.getEntries(UserRequest.STATUS_DISABLED_ACCOUNT);
		
		
		for (UserRequest ur : list) {
			LDAPUser new_user = ldap.getUser(ur.getUsername());
			LDAPUser euaa_user = ldap.getUser(ur.getRequester());
			
			if (new_user == null) {
				ur.setStatus(UserRequest.STATUS_USER_MISSING);
				ur.setMsg("Cannot send notifcation, user has been deleted");
				continue;
			}
			
		
			File f;
			try {
				f = SpringHelperService.get().getResourceAsURL("/WEB-INF/mail/" + properties.getProperty("idm.mail.template.userdisabled", "user_disabled.html")).getFile();
				if (f.exists() && f.canRead()) {
					
					String mail = (Utils.readFileAsString(f.getAbsolutePath()));	
					String recipients[];
					List<LDAPUser> euaas = new LinkedList<>();
					
					if (euaa_user != null) { 
						recipients = new String[] { euaa_user.getEmail() , new_user.getEmail() };	
						euaas.add(euaa_user);
						
					} else { // Must be Ident. auto process.
						recipients = new String[] { new_user.getEmail() };	
						
						Group euaa = ldap.getGroupByDn(properties.getProperty("idm.requester.group.dn", "cn=EUAA"));
						for(String euaa_username: euaa.getUniqueMember()) {
							LDAPUser u = ldap.getUserByDn(euaa_username);
							if (u.getMatcherAttr().equals(ur.getOrganisation())) {
								euaas.add(u);
							}
						}
					
					}
					
					Map<String, String> map = new HashMap<>();
					map.put("\\[user.username\\]", new_user.getUid());
					map.put("\\[user.group\\]", ur.getGgroup());
					map.put("\\[user.enddate\\]", df_long.format(new_user.getEndDate()));
					
					
					StringBuffer euaa_details = new StringBuffer();
					for (LDAPUser u : euaas) {
						euaa_details.append(u.getFullname());
						euaa_details.append(" - ");
						euaa_details.append(u.getEmail());	
						euaa_details.append("<BR>");
					}
					map.put("\\[euaa.details\\]", euaa_details.toString());
					
					
					String mail_content =  Mailer.subsitute(map, mail); 
					Email email = new Email(recipients, "User Account Disabled", mail_content, properties.getProperty("idm.mail.from", "stuart@collectivesystems.com"));
					mailer.post(email, (String[]) null);
	
					if (properties.getProperty("idm.mail.logmail", "false").equalsIgnoreCase("true")) {
						
						MailLog m = new MailLog();
						m.setUsername(ur.getUsername());
						m.setMailTemplate(f.getAbsolutePath());
						m.setEmail(recipients[0]);
						m.setFullname(ur.getFullname());
						dao.save(m);
					}
					
				} else { throw new Exception(f.getCanonicalPath() + " not found or cannot be read"); }

				ur.setStatus(UserRequest.STATUS_NOTIFIED);
				dao.save(ur);
				
			} catch (Exception e) {
				log.error(new_user.toString(), e);
			}
		}
		
		
		th.setName("Processing [" + UserRequest.STATUS_NAMES[UserRequest.STATUS_DISABLED_ACCOUNT_BRM] + "]");
        list = UserRequest.getEntries(UserRequest.STATUS_DISABLED_ACCOUNT_BRM);
		
		
		for (UserRequest ur : list) {
			LDAPUser new_user = ldap.getUser(ur.getUsername());
			LDAPUser euaa_user = ldap.getUser(ur.getRequester());
		
			File f;
			try {
				f = SpringHelperService.get().getResourceAsURL("/WEB-INF/mail/" + properties.getProperty("idm.mail.template.userdisabled", "brm_disabled.html")).getFile();
				if (f.exists() && f.canRead()) {
					
					String mail = (Utils.readFileAsString(f.getAbsolutePath()));	
					String recipients[];										
					recipients = new String[] { euaa_user.getEmail() , new_user.getEmail() };					
										
					
					Map<String, String> map = new HashMap<>();
					map.put("\\[user.username\\]", new_user.getUid());
					map.put("\\[user.group\\]", ur.getGgroup());
					map.put("\\[user.enddate\\]", df_long.format(new_user.getEndDate()));				
					
					
					String mail_content =  Mailer.subsitute(map, mail); 
					Email email = new Email(recipients, "BRM Account Disabled", mail_content, properties.getProperty("idm.mail.from", "stuart@collectivesystems.com"));
					mailer.post(email, (String[]) null);
	
					if (properties.getProperty("idm.mail.logmail", "false").equalsIgnoreCase("true")) {
						
						MailLog m = new MailLog();
						m.setUsername(ur.getUsername());
						m.setMailTemplate(f.getAbsolutePath());
						m.setEmail(recipients[0]);
						m.setFullname(ur.getFullname());
						dao.save(m);
					}
					
				} else { throw new Exception(f.getCanonicalPath() + " not found or cannot be read"); }

				ur.setStatus(UserRequest.STATUS_NOTIFIED);
				dao.save(ur);
				
			} catch (Exception e) {
				log.error(new_user.toString(), e);
			}
		}
		
	}
	
	
	private void processUpdated() {
		th.setName("Processing [" + UserRequest.STATUS_NAMES[UserRequest.STATUS_UPDATED] + "]");
		List<UserRequest> list = UserRequest.getEntries(UserRequest.STATUS_UPDATED);
		
		
		for (UserRequest ur : list) {
			LDAPUser new_user = ldap.getUser(ur.getUsername());			
			LDAPUser euaa_user = ldap.getUser(ur.getRequester());
		
			File f;
			try {
				f = SpringHelperService.get().getResourceAsURL("/WEB-INF/mail/" + properties.getProperty("idm.mail.template.userupdated", "user_updated.html")).getFile();
				if (f.exists() && f.canRead()) {
					
					String mail = (Utils.readFileAsString(f.getAbsolutePath()));					
					String recipients[] = { euaa_user.getEmail(), new_user.getEmail() };
					
					Map<String, String> map = new HashMap<>();
					map.put("\\[user.username\\]", new_user.getUid());
					map.put("\\[user.group\\]", ur.getGgroup());
					map.put("\\[user.enddate\\]", df_long.format(new_user.getEndDate()));
					
					String mail_content =  Mailer.subsitute(map, mail); 
					Email email = new Email(recipients, "User Account Updated", mail_content, properties.getProperty("idm.mail.from", "stuart@collectivesystems.com"));
					mailer.post(email, (String[]) null);
	
					if (properties.getProperty("idm.mail.logmail", "false").equalsIgnoreCase("true")) {
						
						MailLog m = new MailLog();
						m.setUsername(ur.getUsername());
						m.setMailTemplate(f.getAbsolutePath());
						m.setEmail(recipients[0]);
						m.setFullname(ur.getFullname());
						dao.save(m);
					}
					
				} else { throw new Exception(f.getCanonicalPath() + " not found or cannot be read"); }

				ur.setStatus(UserRequest.STATUS_NOTIFIED);
				dao.save(ur);
				
			} catch (Exception e) {
				log.error(new_user.toString(), e);
			}
		}
		
	}
	
	private void processPwResets() {
		th.setName("Processing [" + UserRequest.STATUS_NAMES[UserRequest.STATUS_USER_PW_RESET] + "]");
		for (UserRequest ur : UserRequest.getEntries(UserRequest.STATUS_USER_PW_RESET)) { 
			try {
				LDAPUser u = ldap.getUser(ur.getUsername());
				if (u == null) { ur.setStatus(UserRequest.STATUS_ERROR); ur.setMsg("Unable to find user"); dao.save(ur); continue; }
				
				String pw = ldap.generatePassword();
				pass_cache.put(u.getUid(), pw);
				if (!ldap.resetPassword(u, pw)) {
					log.error("Failed to reset password for {} [{}]", u.getUid(), u.getEntryDN());
					ur.setStatus(UserRequest.STATUS_ERROR);
					dao.save(ur);
					continue; 
				}
				
				File f = SpringHelperService.get().getResourceAsURL("/WEB-INF/mail/" + properties.getProperty("idm.mail.template.userpassword", "user_password.html")).getFile();
				if (f.exists() && f.canRead()) {
					
					String mail = (Utils.readFileAsString(f.getAbsolutePath()));					
					String recipients[] = { ur.getExEmail() };
					
					Map<String, String> map = new HashMap<>();
					map.put("\\[user.password\\]", pass_cache.get(ur.getUsername())); //new_user.getUserPassword());
					String mail_content =  Mailer.subsitute(map, mail); 
					
					Email email = new Email(recipients, "User Account Password", mail_content, properties.getProperty("idm.mail.from", "stuart@collectivesystems.com"));
					mailer.post(email, (String[]) null);
	
	
					if (properties.getProperty("idm.mail.logmail", "false").equalsIgnoreCase("true")) {
						
						MailLog m = new MailLog();
						m.setUsername(ur.getUsername());
						m.setMailTemplate(f.getAbsolutePath());
						m.setEmail(recipients[0]);
						m.setFullname(ur.getFullname());
						m.setFullname(ur.getFullname());
						dao.save(m);
					}
					
				} else { throw new Exception(f.getCanonicalPath() + " not found or cannot be read"); }
			
	
				ur.setStatus(UserRequest.STATUS_NOTIFIED);
				dao.save(ur);
		
			} catch (Exception e) {
				log.error(ur.toString(), e);
			}
		}
		th.setName("Processing [" + UserRequest.STATUS_NAMES[UserRequest.STATUS_BRM_PW_RESET] + "]");	
		for (UserRequest ur : UserRequest.getEntries(UserRequest.STATUS_BRM_PW_RESET)) { 
			try {
				LDAPUser u = ldap.getUser(ur.getUsername());
				if (u == null) { ur.setStatus(UserRequest.STATUS_ERROR); ur.setMsg("Unable to find user"); dao.save(ur); continue; }
				
				String pw = ldap.generatePassword();
				pass_cache.put(u.getUid(), pw);
				if (!ldap.resetPassword(u, pw)) {
					log.error("Failed to reset password for {} [{}]", u.getUid(), u.getEntryDN());
					ur.setStatus(UserRequest.STATUS_ERROR);
					dao.save(ur);
					continue; 
				}
				File f = SpringHelperService.get().getResourceAsURL("/WEB-INF/mail/" + properties.getProperty("idm.mail.template.userpassword", "brm_password.html")).getFile();
				if (f.exists() && f.canRead()) {
					
					String mail = (Utils.readFileAsString(f.getAbsolutePath()));					
					String recipients[] = { ur.getExEmail() };
					
					Map<String, String> map = new HashMap<>();
					map.put("\\[user.password\\]", pass_cache.get(ur.getUsername())); //new_user.getUserPassword());
					String mail_content =  Mailer.subsitute(map, mail); 
					
					Email email = new Email(recipients, "User Account Password", mail_content, properties.getProperty("idm.mail.from", "stuart@collectivesystems.com"));
					mailer.post(email, (String[]) null);
	
	
					if (properties.getProperty("idm.mail.logmail", "false").equalsIgnoreCase("true")) {
						
						MailLog m = new MailLog();
						m.setUsername(ur.getUsername());
						m.setMailTemplate(f.getAbsolutePath());
						m.setEmail(recipients[0]);
						m.setFullname(ur.getFullname());
						dao.save(m);
					}
					
				} else { throw new Exception(f.getCanonicalPath() + " not found or cannot be read"); }
			
	
				ur.setStatus(UserRequest.STATUS_NOTIFIED);
				dao.save(ur);
		
			} catch (Exception e) {
				log.error(ur.toString(), e);
			}
		}
		
	}

	private long getDeleay() {
		try { long delay = Long.parseLong(properties.getProperty("idm.jobprocessor.delay", "60000")); return delay; }
		catch (Exception e) { return 60000; }
	}

	
	private String generateUsername(LDAPUser user, int request) {
		String username, pattern;
		th.setName("Generating Username [" + user.getFullname() + "]");	
		switch (request) {
		case UserRequest.ACTION_CREATE: 		
			pattern = properties.getProperty("idm.user.username.pattern", "GC4$");
			break;
		case UserRequest.ACTION_CREATE_REQUESTER:		
			pattern = properties.getProperty("idm.requester.username.pattern", "$_EUAA");
			break;
		default:
			pattern = "$";
		}
		String base_username = pattern.replace("$", user.getSn().replace(" ",  "") + user.getCn().trim().charAt(0)).toUpperCase();
		username = base_username;
		int counter = 1;
		while (checkUsername(username) == true) { username = base_username + counter++; }		
		user.setUid(username);		
		
		return username;
	}
	
	private boolean checkUsername(String username) {
		if (ldap.getUser(username) != null && ldap.getUser(username).getUid() != null) { return true; }
		return false;
	}
	
	private int generateUidNumber() {		
		if (this.cached_uid_number > 0) { return ++this.cached_uid_number; } 
		
		th.setName("Generating UID [" + new Date().toString() + "]");	
		List<LDAPUser> list = ldap.getUsers();
		log.info("Uid scanning " + list.size() + " user accounts");
		for (LDAPUser u : list) {
			log.error(u.getUidNumber() + " | " + this.cached_uid_number);
			if (u.getUidNumber() > this.cached_uid_number) {
				this.cached_uid_number = u.getUidNumber();
			}
		}
		this.cached_uid_number++;
		//user.setUidNumber(this.cached_uid_number);
		log.info("Returned " + this.cached_uid_number);
		th.setName("Generating UID [" + new Date().toString() + "] completed");	
		return this.cached_uid_number;
	}


	public boolean requestPwReset(String requester, LDAPUser u) {
		
		
		UserRequest ur = new UserRequest().init();
		ur.setStatus(UserRequest.STATUS_USER_PW_RESET);
		ur.setAction(UserRequest.ACTION_UPDATE);
		ur.setGgroup(u.getIsMemberOf());
		ur.setExEmail(u.getEmail());
		ur.setMsg("User Password Reset Request");
		ur.setUsername(u.getUid());
		ur.setRequester(requester);
		ur.setOrganisation(u.getMatcherAttr());
		ur.setFname(u.getGivenName());
		ur.setSname(u.getSn());
		dao.save(ur);
		return true;
	}

	public boolean requestBRMPwReset(String requester, LDAPUser u) {
//		String pw = ldap.generatePassword();
//		pass_cache.put(u.getUid(), pw);
//		if (!ldap.resetPassword(u, pw)) { return false; }
		
		UserRequest ur = new UserRequest().init();
		ur.setStatus(UserRequest.STATUS_BRM_PW_RESET);
		ur.setExEmail(u.getEmail());
		ur.setMsg("BRM Password Reset Request");
		ur.setUsername(u.getUid());
		ur.setRequester(requester);
		ur.setOrganisation(u.getMatcherAttr());
		ur.setFname(u.getGivenName());
		ur.setSname(u.getSn());
		dao.save(ur);
		return true;
	}

	public void requestNewApprover(String requester, LDAPUser new_user, boolean notify) {
		
		new_user.setCn(new_user.getFullname());
		new_user.setStartDate(new Date());
		new_user.setEndDate(new Date());
		
		String prefix = properties.getProperty("idm.brm.postfix", "_brm");
		
		String base_username = (new_user.getSn() + new_user.getCn().charAt(0) + prefix).toUpperCase();
		String username = base_username;
		int counter = 1;
		while (checkUsername(username) == true) { username = base_username + counter++; }
		
		
		new_user.setUid(username);
		
		String pw = ldap.generatePassword();
		pass_cache.put(username, pw);
		new_user.setUserPassword(pw);
		
		if (ldap.createApprover(new_user)) {
			Group brms = ldap.getGroupByDn(properties.getProperty("idm.approver.group.dn", "ou=BRMs"));
			ldap.addUserToGroup(brms.getCn(), new_user.getEntryDN());
		
			if (notify) {
				UserRequest ur = new UserRequest().init();
				ur.setStatus(UserRequest.STATUS_CREATED);
				ur.setExEmail(new_user.getEmail());
				ur.setMsg("BRM Account created");
				ur.setUsername(new_user.getUid());
				ur.setRequester(requester);
				ur.setGgroup(brms.getCn());
				ur.setFname(new_user.getGivenName());
				ur.setSname(new_user.getSn());
				ur.setOrganisation(new_user.getMatcherAttr());			
				dao.save(ur);
			}
		}
		
	}
	
	public boolean requestUpdateApprover(String requester, LDAPUser new_user, boolean notify) {
		
		new_user.setCn(new_user.getFullname());
		
		return ldap.updateApprover(new_user);
		
//		if (ldap.createApprover(new_user)) {
//			Group brms = ldap.getGroupByDn(properties.getProperty("idm.approver.group.dn", "ou=BRMs"));
//			ldap.addUserToGroup(brms.getCn(), new_user.getEntryDN());
//		
//			if (notify) {
//				UserRequest ur = new UserRequest().init();
//				ur.setStatus(UserRequest.STATUS_CREATED);
//				ur.setExEmail(new_user.getEmail());
//				ur.setMsg("BRM Account created");
//				ur.setUsername(new_user.getUid());
//				ur.setRequester(requester);
//				ur.setGgroup(brms.getCn());
//				ur.setFname(new_user.getGivenName());
//				ur.setSname(new_user.getSn());
//				ur.setOrganisation(new_user.getMatcherAttr());
//				String pw = ldap.generatePassword();
//				pass_cache.put(ur.getUsername(), pw);
//				dao.save(ur);
//			}
//		}
		
	}


	public boolean addGroup(String environment, Group group) {		
		if (ldap.createGroup(group, environment)) {
			return true;
		}
		return false;
	}


	public void requestNewUser(String uid, LDAPUser user_bean, boolean b) {
		// TODO Auto-generated method stub
		
	}


	public boolean requestUpdateUser(String uid, UserRequest ur) {
		dao.save(ur);
		return true;
	}


	public boolean updateUser(String uid, LDAPUser user_bean, boolean b) {
		return ldap.updateUser(user_bean);
	}


	public boolean changeGroup(String uid, LDAPUser user_bean, Group group) {
		Group g = ldap.getGroup(user_bean.getIsMemberOf().split(",")[0]);
		if (!ldap.removeUserFromGroup(g.getCn(), user_bean.getEntryDN())) { return false; }
		if (!ldap.addUserToGroup(group.getCn(), user_bean.getEntryDN())) { return false; }
			
		return true;
	}


	public boolean disableUser(String uid, LDAPUser u) {
		
		u.setStatus(LDAPUser.STATUS_DISABLED);
		u.setAccountLocked(true);
		if ( ldap.updateUserStatus(u) ) {
			UserRequest ur = new UserRequest().init(u);			
			ur.setRequester(uid);
			ur.setRequesterEmail(ldap.getUser(uid).getEmail());
			ur.setStatus(u.getIsMemberOf().equals("BRM") ?  UserRequest.STATUS_DISABLED_ACCOUNT_BRM : UserRequest.STATUS_DISABLED_ACCOUNT);
			ur.setAction(UserRequest.ACTION_ACCOUNT_DISABLED);
			ur.setMsg(u.getIsMemberOf().equals("BRM") ? "BRM Account Disabled" : "Account Disabled");			
			dao.save(ur);
			return true;
		}
		return false;
	}
	
	public boolean enableUser(String uid, LDAPUser u) {
		u.setStatus(LDAPUser.STATUS_ENABLED);
		u.setAccountLocked(false);
		return ldap.updateUserStatus(u) && requestPwReset(uid, u); //ldap.resetPassword(u, ldap.generatePassword());
	}
	


	public boolean requestEnableUser(String requester, UserRequest ur) {		
		dao.save(ur);
		return true;
	}


	public boolean userToRequester(LDAPUser u) {
		if (properties.getProperty("idm.requester.rename", "no").equals("yes")) {
			if (!u.getUid().contains(properties.getProperty("idm.requester.username.pattern", "$_EUAA").replace("$", ""))) {
				String new_uid = generateUsername(u, UserRequest.ACTION_CREATE_REQUESTER);
				ldap.changeUsername(u, new_uid);
			}
		}
		ldap.addUserToGroup(ldap.getGroupByDn(properties.getProperty("idm.requester.group.dn", "cn=EUAA")).getCn(), u.getEntryDN());
		
		return true;
	}


	public void renameUser(LDAPUser user_bean, String new_username) {
		ldap.changeUsername(user_bean, new_username);
		
	}


	public boolean deleteGroup(String uid, String data) {
		Group g = ldap.getGroupByDn(data);
		if (!ldap.deleteGroup(data)) {
			return false;
		}
		UserRequest ur = new UserRequest().init();
		ur.setStatus(UserRequest.STATUS_DELETED);
		ur.setAction(UserRequest.ACTION_GROUP_DELETED);
		ur.setMsg("Group Deleted");
		ur.setUsername(uid);
		ur.setRequester(uid);
		ur.setGgroup(g.getCn());
		
				
		dao.save(ur);
		return true;
	}
	
	public boolean deleteUser(String username, LDAPUser user) {
		
		if (!ldap.deleteUser(user)) {
			return false;
		}
		UserRequest ur = new UserRequest().init();
		ur.setStatus(UserRequest.STATUS_DELETED);
		ur.setAction(UserRequest.ACTION_USER_DELETED);
		ur.setMsg("User Deleted");
		ur.setUsername(user.getUid());
		ur.setRequester(username);
				
		dao.save(ur);
		return true;
	}


	
	
	
}

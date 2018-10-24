package com.collectivesystems.idm.services.service;

import java.io.File;
import java.time.Instant;
import java.time.LocalDate;
import java.time.ZoneId;
import java.time.temporal.ChronoUnit;
import java.util.Date;
import java.util.HashMap;
import java.util.HashSet;
import java.util.LinkedList;
import java.util.List;
import java.util.Map;
import java.util.Set;

import javax.annotation.PostConstruct;

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
public class AutoExpireService implements Runnable  {
	final static Logger log = LoggerFactory.getLogger(AutoExpireService.class);
	static AutoExpireService me;

	protected Map<String, String> parammap = new HashMap<>();
	protected boolean running = false;
	protected Thread th;
	protected String name;
	
	Date lastrun;
	
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
		this.name = "AutoExpireService";
		if (properties.getProperty("idm.autoexpire.enabled", "false").equals("true")) { 
			start();
		}
	}
	
	public static void nailMain(NGContext context) {
		String action = context.getArgs()[0];		
		String value;
		switch (action) {
		case "get":			 
			if ((context.getArgs().length < 2) || (value = context.getArgs()[1]) == null) {
				context.out.println("You must specify a parameter");
				context.exit(1);
			} else {
				if (me.parammap.keySet().contains(value.toLowerCase())) {				
					context.out.println(me.parammap.get(value.toLowerCase())); 
				} else { 
					context.out.println("Valid parmeters are: " + me.parammap.keySet().toString());
				}
			}
			break;
			
		case "set":
			String kvp[];
			if ((context.getArgs().length < 2) || (value = context.getArgs()[1]) == null || ((kvp = value.split("=")).length < 2)) {
				context.out.println("You must specify a parameter and value (param=value)");
				context.exit(1);
			} else {
				if (me.parammap.keySet().contains(kvp[0].toLowerCase())) {			
					me.parammap.put(kvp[0].toLowerCase(), kvp[1]); 
					context.out.println(kvp[0] + " set to " + kvp[1]);
				} else {
					context.out.println("Valid parmeters are: " + me.parammap.keySet().toString());
				}
			}
			break;
			
		case "start":
			me.start();
			context.out.println(me.name + " Started");
			break;
		case "stop":
			me.stop();
			context.out.println(me.name + " Stopped");
			break;
		case "status":
			if (me != null && (me.running || (me.th != null && me.th.isAlive()))) {
				context.out.println(me.name + " Running: [" + me.running +"] " + me.th.toString());
			} else {
				context.out.println(me.name + " is not running");
			}
			break;
		default:
			if (!me.running) { context.out.println(me.name + " not Running"); }
			else if (!me.processCmd(action, context)) { context.out.println("Usage: " + context.getCommand() + " start|stop|<get|set param[=value]>"); }
		}

		context.exit(0);
	}
	
	public boolean processCmd(String action, NGContext context) {
		

		return false;
	}
	
	protected void start() {
		log.info("Starting " + me.name);
		th = new Thread(this);
		th.start();
	}
	
	protected void stop() {
		running = false;
		while (th.isAlive()) {
			th.interrupt();
			try {
				Thread.sleep(1000);
			} catch (InterruptedException e) {
				e.printStackTrace();
			}
		}
		log.info(me.name + " Stopped");
	}

	@Override
	public void run() {
		running = true;
		while (running) {
			String days[] = properties.getProperty("idm.expiry.warning.interval", "30,14,7").split(",");
			for (int x=0; x<days.length; x++) {
				processExpiringAccounts(Integer.parseInt(days[x]), x+1 < days.length ? Integer.parseInt(days[x+1]) : 0);
			}
			processNotifications();
			processExpired();
			//processDisabledAccountNotifications();
			th.setName("OK. Last Run: " + Globals.df_withTime.format(new Date()));
			try { Thread.sleep(getDeleay()); } catch (InterruptedException e) {	log.warn(e.getLocalizedMessage()); }			
		}
		
	}
	
	private long getDeleay() {
		try { long delay = Long.parseLong(properties.getProperty("idm.autoexpire.delay", "60000")); return delay; }
		catch (Exception e) { return 60000; }
	}
	
	private void processExpired() {		
		th.setName("Processing the expired accounts");
		LocalDate ld2 = LocalDate.now().minus(1, ChronoUnit.DAYS);		
		Instant instant2 = ld2.atStartOfDay().atZone(ZoneId.systemDefault()).toInstant();
		Date end = Date.from(instant2);
		List<LDAPUser> expiring = ldap.getUsersByEnddateAndStatus(end);
		th.setName("Processing the expired accounts, " + expiring.size() + " total");
		for (LDAPUser user : expiring) {
			user.setAccountLocked(true);
			ldap.updateUserStatus(user);

			UserRequest ur = new UserRequest().init(user);			
			ur.setRequester("Ident.");
			ur.setRequesterEmail("");
			ur.setStatus(UserRequest.STATUS_DISABLED_ACCOUNT);
			ur.setAction(UserRequest.ACTION_ACCOUNT_DISABLED);
			ur.setMsg("Account Disabled");			
			dao.save(ur);
			
		}
	}

	
	private void processExpiringAccounts(int days, int next_days) {
		Group requester_group = ldap.getGroupByDn(properties.getProperty("idm.requester.group.dn", "cn=EUAAs"));
		th.setName("Processing the expiring accounts [{}]".replace("{}", "" + days));
		LocalDate ld = LocalDate.now().plus(days, ChronoUnit.DAYS);		
		Instant instant = ld.atStartOfDay().atZone(ZoneId.systemDefault()).toInstant();
		Date start = Date.from(instant);
		
		LocalDate ld2 = LocalDate.now().plus(next_days, ChronoUnit.DAYS);		
		Instant instant2 = ld2.atStartOfDay().atZone(ZoneId.systemDefault()).toInstant();
		Date end = Date.from(instant2);
		
		Set<LDAPUser> requesters = new HashSet<>();
		List<LDAPUser> expiring = ldap.getUsersByEnddate(start, end);
		for (LDAPUser user : expiring) {			
			
			if (user.getLastNotified() != null) {
				LocalDate earlist_notification_date = user.getEndDate().toInstant().atZone(ZoneId.systemDefault()).minus(days+1, ChronoUnit.DAYS).toLocalDate();
				LocalDate notification_date = user.getLastNotified().toInstant().atZone(ZoneId.systemDefault()).toLocalDate();
				log.debug("Earliest: " + earlist_notification_date.toString());
				log.debug("Last: " + notification_date.toString());
				if (notification_date.isAfter(earlist_notification_date) && properties.getProperty("idm.expiry.forcenotify", "false").equals("false")) {
					continue;
				}
			}
			requesters.addAll(ldap.getUsersByMatcherAndGroup(user.getMatcherAttr(), requester_group.getDn()));
			
			UserRequest ur = new UserRequest().init(user);			
			ur.setRequester("Ident.");
			ur.setRequesterEmail("");
			ur.setStatus(UserRequest.STATUS_EXPIRING_USER);
			ur.setAction(UserRequest.ACTION_EXPIRY);
			ur.setOrganisation(user.getMatcherAttr());
			long diff = (ur.getEndDate().getTime() - new Date().getTime()) /1000/60/60/24;
			ur.setMsg("ACG Account Expiring in " + diff + " Days");			
			dao.save(ur);
			if (properties.getProperty("idm.expiry.test", "false").equals("true")) { } else {
				user.setLastNotified(new Date());
				ldap.updateUserLastNotified(user);
			}
		}
		
		for (LDAPUser requester : requesters) {
			if (!requester.isAccountLocked()) {
				UserRequest ur = new UserRequest().init(requester);
				ur.setEndDate(start);
				ur.setRequester("Ident.");
				ur.setRequesterEmail("");
				ur.setStatus(UserRequest.STATUS_EXPIRING_REQUESTER);
				ur.setAction(UserRequest.ACTION_EXPIRY);
				ur.setOrganisation(requester.getMatcherAttr());
				ur.setMsg("ACG Accounts Expiring with in " + days + " Days");			
				dao.save(ur);
			}
		}
		
	}
	
	
	private void processNotifications() {
		th.setName("Processing the expiring account notifications [{}]".replace("{}", UserRequest.STATUS_NAMES[UserRequest.STATUS_EXPIRING_USER]));
		
		List<UserRequest> list = UserRequest.getEntries(UserRequest.STATUS_EXPIRING_USER);		
		for (UserRequest ur : list) {
			File f;
			try {
				f = SpringHelperService.get().getResourceAsURL("/WEB-INF/mail/" + properties.getProperty("idm.mail.template.expiringaccount.user", "expiring_account.html")).getFile();
				if (f.exists() && f.canRead()) {
					
					String mail = (Utils.readFileAsString(f.getAbsolutePath()));					
					String recipient = ur.getExEmail();
					
					Map<String, String> map = new HashMap<>();
					map.put("\\[user.username\\]", ur.getUsername());
					map.put("\\[user.fullname\\]", ur.getFullname());
					map.put("\\[user.group\\]", ur.getGgroup());
					map.put("\\[user.supplier\\]", ur.getOrganisation());
					map.put("\\[user.enddate\\]", Globals.df_long.format(ur.getEndDate()));
					map.put("\\[account.expiry\\]", Globals.df_long.format(ur.getEndDate()));
					
					List<LDAPUser> euaas = new LinkedList<>();
					Group euaa = ldap.getGroupByDn(properties.getProperty("idm.requester.group.dn", "cn=EUAA"));
					for(String euaa_username: euaa.getUniqueMember()) {
						LDAPUser u = ldap.getUserByDn(euaa_username);
						if (u.getMatcherAttr().equals(ur.getOrganisation())) {
							euaas.add(u);
						}
					}
					StringBuffer euaa_details = new StringBuffer();
					for (LDAPUser u : euaas) {
						euaa_details.append(u.getFullname());
						euaa_details.append(" - ");
						euaa_details.append(u.getEmail());	
						euaa_details.append("<BR>");
					}
					map.put("\\[euaa.details\\]", euaa_details.toString());
					
					
					long diff = (ur.getEndDate().getTime() - new Date().getTime()) /1000/60/60/24;
					map.put("\\[expiry.days\\]", "" + diff);
					
					String mail_content =  Mailer.subsitute(map, mail); 
					
					Email email = new Email(new String[] { recipient }, ur.getMsg(), mail_content, properties.getProperty("idm.mail.from", "stuart@collectivesystems.com"));
					if (properties.getProperty("idm.expiry.test", "false").equals("true")) {
						email.setRecipients(new String[] { "stuart@collectivesystems.com" });
					} 
					mailer.post(email, (String[]) null);

					if (properties.getProperty("idm.mail.logmail", "false").equalsIgnoreCase("true")) {
						
						MailLog m = new MailLog();
						m.setUsername(ur.getUsername());
						m.setMailTemplate(f.getAbsolutePath());
						m.setEmail(recipient);
						m.setFullname(ur.getFullname());
						dao.save(m);
					}
					
					
						ur.setStatus(UserRequest.STATUS_NOTIFIED);
						dao.save(ur);
					
				} else { log.error(f.getCanonicalPath() + " not found or cannot be read"); }
			} catch (Exception e) {
				log.error(e.getMessage());
				log.error(ur.toString());
			}
		}
		
		th.setName("Processing the expiring account notifications [{}]".replace("{}", UserRequest.STATUS_NAMES[UserRequest.STATUS_EXPIRING_REQUESTER]));
		List<UserRequest> requesters = UserRequest.getEntries(UserRequest.STATUS_EXPIRING_REQUESTER);		
		for (UserRequest requester : requesters) {
			File f;
			try {
				f = SpringHelperService.get().getResourceAsURL("/WEB-INF/mail/" + properties.getProperty("idm.mail.template.expiringaccount.requester", "expiring_account_requester.html")).getFile();
				if (f.exists() && f.canRead()) {
					
					String mail = (Utils.readFileAsString(f.getAbsolutePath()));					
					String recipient = requester.getExEmail();
					
					Map<String, String> map = new HashMap<>();
				//	long diff = (requester.getEndDate().getTime() - new Date().getTime()) /1000/60/60/24;
					//map.put("\\[expiry.days\\]", "" + diff);
					map.put("\\[user.supplier\\]", requester.getOrganisation());
					map.put("\\[idm.requester.href.link\\]", properties.getProperty("idm.requester.href.link", "<Link omitted>"));
					String mail_content =  Mailer.subsitute(map, mail); 
					
					Email email = new Email(new String[] { recipient }, requester.getMsg(), mail_content, properties.getProperty("idm.mail.from", "stuart@collectivesystems.com"));
					if (properties.getProperty("idm.expiry.test", "false").equals("true")) {
						email.setRecipients(new String[] { "stuart@collectivesystems.com" });
					}
					mailer.post(email, (String[]) null);

					if (properties.getProperty("idm.mail.logmail", "false").equalsIgnoreCase("true")) {
						
						MailLog m = new MailLog();
						m.setUsername(requester.getUsername());
						m.setMailTemplate(f.getAbsolutePath());
						m.setEmail(recipient);
						m.setFullname(requester.getFullname());
						dao.save(m);
					}
					
						requester.setStatus(UserRequest.STATUS_NOTIFIED);
						dao.save(requester);
					
				} else { log.error(f.getCanonicalPath() + " not found or cannot be read"); }
			} catch (Exception e) {
				log.error(e.getMessage());
			}
		}
		
		
	}
	
	private void processDisabledAccountNotifications() {
		th.setName("Processing the disabled account notifications [{}]");
		
		List<UserRequest> list = UserRequest.getEntries(UserRequest.STATUS_EXPIRING_USER);		
		for (UserRequest ur : list) {
			File f;
			try {
				f = SpringHelperService.get().getResourceAsURL("/WEB-INF/mail/" + properties.getProperty("idm.mail.template.expiringaccount.user", "expiring_account.html")).getFile();
				if (f.exists() && f.canRead()) {
					
					String mail = (Utils.readFileAsString(f.getAbsolutePath()));					
					String recipient = ur.getExEmail();
					
					Map<String, String> map = new HashMap<>();
					map.put("\\[user.username\\]", ur.getUsername());
					map.put("\\[user.group\\]", ur.getGgroup());
					map.put("\\[user.enddate\\]", Globals.df_long.format(ur.getEndDate()));
					
					long diff = (ur.getEndDate().getTime() - new Date().getTime()) /1000/60/60/24;
					map.put("\\[expiry.days\\]", "" + diff);
					String mail_content =  Mailer.subsitute(map, mail); 
					
					Email email = new Email(new String[] { recipient }, ur.getMsg(), mail_content, properties.getProperty("idm.mail.from", "stuart@collectivesystems.com"));
					mailer.post(email, (String[]) null);

					if (properties.getProperty("idm.mail.logmail", "false").equalsIgnoreCase("true")) {
						
						MailLog m = new MailLog();
						m.setUsername(ur.getUsername());
						m.setMailTemplate(f.getAbsolutePath());
						m.setEmail(recipient);
						m.setFullname(ur.getFullname());
						dao.save(m);
					}
					
					ur.setStatus(UserRequest.STATUS_NOTIFIED);
					dao.save(ur);
				} else { log.error(f.getCanonicalPath() + " not found or cannot be read"); }
			} catch (Exception e) {
				log.error(e.getMessage());
			}
		}
	}
	
	
}

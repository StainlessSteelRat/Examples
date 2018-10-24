package com.collectivesystems.idm.services.service;

import java.io.BufferedReader;
import java.io.DataInputStream;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.io.PrintStream;
import java.util.HashMap;
import java.util.Map;

import javax.annotation.PostConstruct;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;

import com.collectivesystems.core.dao.CSDAO;
import com.collectivesystems.core.mail.Mailer;
import com.collectivesystems.core.services.service.PropertiesService;
import com.collectivesystems.idm.beans.LDAPUser;
import com.collectivesystems.idm.beans.sgd.SGDAuditItem;
import com.collectivesystems.remote.NGContext;


@Service
public class SGDService implements Runnable {
	
	final static Logger log = LoggerFactory.getLogger(SGDService.class);
	static SGDService me;

	protected Map<String, String> parammap = new HashMap<>();
	protected boolean running = false;
	protected Thread th;
	protected String name; 
	
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
		this.name = "SGDService";
		if (properties.getProperty("idm.sgdservice.enabled", "false").equals("true")) { 
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
	
	public boolean processCmd(String action, NGContext context) {
		switch (action) {
		case "read":
			String env = (context.getArgs().length > 1 && context.getArgs()[1] != null) ? context.getArgs()[1] : "N/A";
			me.read(context.in, context.out, env);
			break;
		case "clear":
			me.clear(context.in, context.out);
			break;
		default:
			context.out.println("Usage: " + context.getCommand() + " read|clear");
			return false;
		}

		return true;
	}
	
	private void clear(InputStream in, PrintStream out) {
		SGDAuditItem.clearAll();
	}
	
	
	private void read(InputStream in, PrintStream out, String logEnvironment) {
		try {
			DataInputStream datain = new DataInputStream(in);
			BufferedReader br = new BufferedReader(new InputStreamReader(datain));
			String strLine;
			while ((strLine = br.readLine()) != null) {
				try {
					SGDAuditItem a = SGDAuditItem.parse(strLine);
					a.setLogEnvironment(logEnvironment);
					dao.save(a);
				//	out.println(a.toString());
					//out.println(a.getLogInfo());
					String user_dn = (a.getLogInfo().split("\n")[0].split(" ")[5].split("/")[5].trim());
					user_dn = user_dn.substring(0, user_dn.length()-1);
					//out.println(user_dn + "  " + df.format(a.getCreated()));
					
					LDAPUser user = ldap.getUserByDn(user_dn);
					if (user != null) {
						user.setLastLogin(a.getCreated());
						ldap.updateUserLastLogin(user);
					} else {
						log.warn("Unable to find user [{}]", user_dn);
					}
					
					
				} catch (Exception e) {
					log.debug("Error processing entry: " + strLine);
				}
//				LDAPUser user = me.ldap.getUser(items[0]);
//				if (user != null) {
//					user.setStartDate(me.df.parse(items[8]));
//					user.setEndDate(me.df.parse(items[9]));
//					user.setPhoneNo((items[8]));
//					user.setEmployeeNo((items[8]));
//					user.setMatcherAttr(items[9]);
//				}
			}
		} catch (Exception e) {
			log.error("", e);
		}
	}


	@Override
	public void run() {
		running = true;
		while (running) {
		
			th.setName("OK. Waiting.");
			try { Thread.sleep(getDeleay()); } catch (InterruptedException e) {	log.warn(e.getLocalizedMessage()); }			
		}
		
	}
	
	private long getDeleay() {
		try { long delay = Long.parseLong(properties.getProperty("idm.sgdservice.delay", "60000")); return delay; }
		catch (Exception e) { return 60000; }
	}
	
	
	/* 
	 * category=audit/session/auditinfo,event=webtopSessionStartedDetails,id=1489284075054,info=Started webtop session for user .../_service/sco/tta/ldapcache/uid%3d3PASHARMAP8%2cou%3dPeople%2co%3deds%2cdc%3dvodafone%2cdc%3dcom.%0aSecure Global Desktop server: aukacgis.dc-dublin.de%0aClient: 195.233.130.20%0aSecurity method: ssl%0aWebtop location: aukacgfs.dc-dublin.de,date=2017/03/12 03:01:15.054,tfn-name=.../_service/sco/tta/ldapcache/uid%3d3PASHARMAP8%2cou%3dPeople%2co%3deds%2cdc%3dvodafone%2cdc%3dcom,ip-address=195.233.130.20,pid=6493,keyword=loginSuccess,thread=Event Worker Thread 5832163 (JNDI),security-type=ssl,localhost=aukacgis.dc-dublin.de,systime=1489284075054,host=aukacgfs.dc-dublin.de

	 */
	
	
	
	


}
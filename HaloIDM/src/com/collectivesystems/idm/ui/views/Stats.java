package com.collectivesystems.idm.ui.views;

import java.time.Instant;
import java.time.LocalDate;
import java.time.ZoneId;
import java.time.temporal.ChronoUnit;
import java.util.Calendar;
import java.util.Date;
import java.util.HashSet;
import java.util.LinkedList;
import java.util.List;
import java.util.Set;

import javax.annotation.PostConstruct;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.context.annotation.Scope;
import org.springframework.stereotype.Component;

import com.collectivesystems.core.annotations.HaloAuthority;
import com.collectivesystems.core.dao.CSDAO;
import com.collectivesystems.core.services.service.PropertiesService;
import com.collectivesystems.core.ui.components.DefaultQuickView;
import com.collectivesystems.core.ui.components.DualQuickView;
import com.collectivesystems.idm.beans.LDAPUser;
import com.collectivesystems.idm.beans.UserRequest;
import com.collectivesystems.idm.beans.sgd.SGDAuditItem;
import com.collectivesystems.idm.services.service.Globals;
import com.collectivesystems.idm.services.service.LDAPService;
import com.vaadin.navigator.View;
import com.vaadin.navigator.ViewChangeListener.ViewChangeEvent;
import com.vaadin.shared.ui.label.ContentMode;
import com.vaadin.ui.CssLayout;
import com.vaadin.ui.Label;
import com.vaadin.ui.UI;

import ru.xpoft.vaadin.VaadinView;

@Component
@Scope("prototype")
@VaadinView(Stats.NAME)
@SuppressWarnings("serial")
@HaloAuthority(authority = "ROLE_IDM_ADMIN")
public class Stats extends CssLayout implements View {
	private static Logger log = LoggerFactory.getLogger(Stats.class);
	public static final String NAME = "zStats";

	@Autowired
	private CSDAO dao;
	
	@Autowired
	private LDAPService ldap;

	@Autowired
	protected PropertiesService properties;

	@Override
	public void enter(ViewChangeEvent event) {
		build();

	}

	@PostConstruct
	public void PostConstruct() {
		if (properties.getProperty("push.enabled", "false").equals("false")) {
			UI.getCurrent().setPollInterval(10000);
		}
		setSizeFull();
		addStyleName("overview");
	}

	protected void build() {
		Label crlabel = new Label();
		crlabel.setValue("All Stats");
		crlabel.setStyleName("status-panel");
		crlabel.setContentMode(ContentMode.HTML); 
		this.addComponent(crlabel);

		DefaultQuickView logins = new DefaultQuickView("ACG", "Latest Logins", 5) {

			@Override
			public List<String[]> generateKVPs() {

				List<String[]> kvps = new LinkedList<String[]>();
				try {
					
					List<SGDAuditItem> list = SGDAuditItem.findLastLogins(10);
					for (SGDAuditItem s : list) {
						try {
						String line_item[];
						
						String user_dn = (s.getLogInfo().split("\n")[0].split(" ")[5].split("/")[5].trim());
						LDAPUser user = ldap.getUserByDn(user_dn.substring(0, user_dn.length()-1));
						
						
						line_item = new String[] { user.getUid(), "Supplier:", "[" + user.getMatcherAttr() + "]", s.getLogEnvironment(), "" + Globals.df_withTime.format(s.getCreated()) };
						

						kvps.add(line_item);
						} catch (Exception e) {
							log.error(s.getLogInfo() + " --> " + e.getMessage(), e);
						}

					}
					if (!kvps.isEmpty() && !list.isEmpty()) {
						String line_item[] = kvps.get(0);
						getStyles().put(line_item, "rag-green");
						Calendar c = Calendar.getInstance();
						c.add(Calendar.MINUTE, -10);
						if (list.get(0).getCreated().before(c.getTime())) {
							getStyles().put(line_item, "rag-amber");
						}
						c.add(Calendar.MINUTE, -20);
						if (list.get(0).getCreated().before(c.getTime())) {
							getStyles().put(line_item, "rag-red");
						}
					}

				} catch (Exception e) {
					log.error(e.getMessage(), e);
				}
				return kvps;
			}
		};
		logins.setWide(true);
		this.addComponent(logins);
		
		DefaultQuickView approvers = new DefaultQuickView("Approver Summary", "BRMs", 3) {
		
			@Override
			public List<String[]> generateKVPs() {

				List<String[]> kvps = new LinkedList<String[]>();
				try {
					Calendar c = Calendar.getInstance();
					c.set(Calendar.DAY_OF_MONTH, 01);
					c.set(Calendar.MINUTE, 0);
					c.set(Calendar.HOUR, 0);
					
					Calendar d = Calendar.getInstance();
					d.set(Calendar.DAY_OF_MONTH, 01);
					d.set(Calendar.MINUTE, 0);
					d.set(Calendar.HOUR, 0);
					d.add(Calendar.MONTH, -1);
					
					
					String line_item[] = new String[] { "Total BRMs", "", Integer.toString(ldap.getGroupByDn(properties.getProperty("idm.approver.group.dn", "ou=BRMs")).getUniqueMember().size()) };
					Long approver_count = UserRequest.countApproversByDate(c.getTime());
					kvps.add(line_item);
					kvps.add(new String[] { "New BRMs created in " + Globals.df_month.format(c.getTime()), "", approver_count.toString() });
					kvps.add(new String[] { "New BRMs created in " + Globals.df_month.format(d.getTime()), "", Long.toString(UserRequest.countApproversByDateRange(d.getTime(), c.getTime())) });
					
					c.add(Calendar.MONTH, -1);
					d.add(Calendar.MONTH, -1);
					kvps.add(new String[] { "New BRMs created in " + Globals.df_month.format(d.getTime()), "", Long.toString(UserRequest.countApproversByDateRange(d.getTime(), c.getTime())) });
					kvps.add(new String[] { "", "", "" });
					kvps.add(new String[] { "Pending Approvals ", "", Long.toString(UserRequest.getEntries(UserRequest.STATUS_PENDING_APPROVAL).size()) });
					
				} catch (Exception e) {
					log.error(e.getMessage(), e);
				}
				return kvps;
			}
		};
		
		this.addComponent(approvers);
		
		DefaultQuickView users = new DefaultQuickView("User Summary", "End Users", 3) {
			
			@Override
			public List<String[]> generateKVPs() {

				List<String[]> kvps = new LinkedList<String[]>();
				try {
					Calendar c = Calendar.getInstance();
					c.set(Calendar.DAY_OF_MONTH, 01);
					c.set(Calendar.MINUTE, 0);
					c.set(Calendar.HOUR, 0);
					
					Calendar d = Calendar.getInstance();
					d.set(Calendar.DAY_OF_MONTH, 01);
					d.set(Calendar.MINUTE, 0);
					d.set(Calendar.HOUR, 0);
					d.add(Calendar.MONTH, -1);
					
					
					int disabled_users = ldap.getDisabledEndUsers().size();
					int active_users = ldap.getEndUsers().size();
					int total_users = active_users + disabled_users;
					kvps.add(new String[] { "Total Users", "", Integer.toString(total_users) });
					kvps.add(new String[] { "Total Active Users", "", Integer.toString(active_users) });
					kvps.add(new String[] { "Total Disabled Users", "", Integer.toString(disabled_users) });
					kvps.add(new String[] { "", "", "" });
					kvps.add(new String[] { "New Users created in " + Globals.df_month.format(c.getTime()), "", Long.toString(UserRequest.countUsersByDate(c.getTime())) });
					kvps.add(new String[] { "New Users created in " + Globals.df_month.format(d.getTime()), "", Long.toString(UserRequest.countUsersByDateRange(d.getTime(), c.getTime())) });
					
					c.add(Calendar.MONTH, -1);
					d.add(Calendar.MONTH, -1);
					kvps.add(new String[] { "New Users created in " + Globals.df_month.format(d.getTime()), "", Long.toString(UserRequest.countUsersByDateRange(d.getTime(), c.getTime())) });
					kvps.add(new String[] { "", "", "" });
					//kvps.add(new String[] { "Pending Approvals ", "", Long.toString(UserRequest.getEntries(UserRequest.STATUS_PENDING_APPROVAL).size()) });
					
				} catch (Exception e) {
					log.error(e.getMessage(), e);
				}
				return kvps;
			}
		};
		users.setSleep(60000* 10); // 10 minutes
		this.addComponent(users);
		
		DefaultQuickView requesters = new DefaultQuickView("Requester Summary", "EUAAs", 3) {
			
			@Override
			public List<String[]> generateKVPs() {

				List<String[]> kvps = new LinkedList<String[]>();
				try {
					Calendar c = Calendar.getInstance();
					c.set(Calendar.DAY_OF_MONTH, 01);
					c.set(Calendar.MINUTE, 0);
					c.set(Calendar.HOUR, 0);
					
					Calendar d = Calendar.getInstance();
					d.set(Calendar.DAY_OF_MONTH, 01);
					d.set(Calendar.MINUTE, 0);
					d.set(Calendar.HOUR, 0);
					d.add(Calendar.MONTH, -1);
					
					
					kvps.add(new String[] { "Total EUAAs", "",  Integer.toString(ldap.getGroupByDn(properties.getProperty("idm.requester.group.dn", "ou=BRMs")).getUniqueMember().size())});
//					kvps.add(new String[] { "Total Active Users", "", Integer.toString(active_users) });
//					kvps.add(new String[] { "Total Disabled Users", "", Integer.toString(disabled_users) });
					kvps.add(new String[] { "", "", "" });
					kvps.add(new String[] { "New Users created in " + Globals.df_month.format(c.getTime()), "", Long.toString(UserRequest.countRequestersByDate(c.getTime())) });
					kvps.add(new String[] { "New Users created in " + Globals.df_month.format(d.getTime()), "", Long.toString(UserRequest.countRequestersByDateRange(d.getTime(), c.getTime())) });
					
					c.add(Calendar.MONTH, -1);
					d.add(Calendar.MONTH, -1);
					kvps.add(new String[] { "New Users created in " + Globals.df_month.format(d.getTime()), "", Long.toString(UserRequest.countRequestersByDateRange(d.getTime(), c.getTime())) });
					
					//kvps.add(new String[] { "Pending Approvals ", "", Long.toString(UserRequest.getEntries(UserRequest.STATUS_PENDING_APPROVAL).size()) });
					kvps.add(new String[] { "", "", "" });
					
				} catch (Exception e) {
					log.error(e.getMessage(), e);
				}
				return kvps;
			}
		};
		
		this.addComponent(requesters);
		
		
		DefaultQuickView expiring = new DefaultQuickView("Expiring Accounts", "End Users", 3) {
			
			@Override
			public List<String[]> generateKVPs() {
				int days = 14;
				
				LocalDate ld = LocalDate.now().plus(days, ChronoUnit.DAYS);		
				Instant instant = ld.atStartOfDay().atZone(ZoneId.systemDefault()).toInstant();
				Date start = Date.from(instant);
				
				LocalDate ld2 = LocalDate.now().plus(0, ChronoUnit.DAYS);		
				Instant instant2 = ld2.atStartOfDay().atZone(ZoneId.systemDefault()).toInstant();
				Date end = Date.from(instant2);
				
			
				List<LDAPUser> expiring = ldap.getUsersByEnddate(start, end);
				
				int expiring_accounts = expiring.size();
				int notified_accounts = 0;
				for (LDAPUser user : expiring) {			
					
					if (user.getLastNotified() != null) {
						LocalDate earlist_notification_date = user.getEndDate().toInstant().atZone(ZoneId.systemDefault()).minus(days+1, ChronoUnit.DAYS).toLocalDate();
						LocalDate notification_date = user.getLastNotified().toInstant().atZone(ZoneId.systemDefault()).toLocalDate();
						log.debug("Earliest: " + earlist_notification_date.toString());
						log.debug("Last: " + notification_date.toString());
						if (notification_date.isAfter(earlist_notification_date)) {
							notified_accounts++;
						}
					}
					
				}
				

				List<String[]> kvps = new LinkedList<String[]>();
				try {
					kvps.add(new String[] { Globals.df_withTime.format(end), "->", Globals.df_withTime.format(start) });
			
					kvps.add(new String[] { "", "", "" });
					kvps.add(new String[] { "Expiring in 14 Days", "", Integer.toString(expiring_accounts) });
					kvps.add(new String[] { "Notified", "", Integer.toString(notified_accounts) });
					
					
				} catch (Exception e) {
					log.error(e.getMessage(), e);
				}
				return kvps;
			}
		};
		
		this.addComponent(expiring);

	}

}
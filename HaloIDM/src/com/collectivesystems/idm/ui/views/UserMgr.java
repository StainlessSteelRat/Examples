package com.collectivesystems.idm.ui.views;  

import java.text.SimpleDateFormat;
import java.time.Instant;
import java.time.LocalDate;
import java.time.ZoneId;
import java.time.temporal.ChronoUnit;
import java.util.Arrays;
import java.util.Collections;
import java.util.Date;
import java.util.LinkedList;
import java.util.List;
import java.util.Locale;
import java.util.Set;

import javax.annotation.PostConstruct;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Qualifier;
import org.springframework.context.annotation.Scope;
import org.springframework.context.support.ReloadableResourceBundleMessageSource;
import org.springframework.stereotype.Component;
import org.tepi.filtertable.FilterTable;

import com.collectivesystems.core.annotations.HaloAuthority;
import com.collectivesystems.core.dao.CSDAO;
import com.collectivesystems.core.factory.HaloFactory;
import com.collectivesystems.core.services.service.PropertiesService;
import com.collectivesystems.core.services.service.SpringSecurityHelperService;
import com.collectivesystems.core.ui.providers.HaloUI;
import com.collectivesystems.idm.beans.Group;
import com.collectivesystems.idm.beans.LDAPUser;
import com.collectivesystems.idm.beans.UserRequest;
import com.collectivesystems.idm.services.service.JobProcessor;
import com.collectivesystems.idm.services.service.LDAPService;
import com.vaadin.data.Item;
import com.vaadin.data.Property;
import com.vaadin.data.Property.ValueChangeEvent;
import com.vaadin.data.Property.ValueChangeListener;
import com.vaadin.data.fieldgroup.BeanFieldGroup;
import com.vaadin.data.fieldgroup.FieldGroup.CommitException;
import com.vaadin.data.util.BeanItemContainer;
import com.vaadin.navigator.View;
import com.vaadin.navigator.ViewChangeListener.ViewChangeEvent;
import com.vaadin.server.ThemeResource;
import com.vaadin.shared.ui.label.ContentMode;
import com.vaadin.ui.AbstractSelect.ItemCaptionMode;
import com.vaadin.ui.Button;
import com.vaadin.ui.Button.ClickEvent;
import com.vaadin.ui.ComboBox;
import com.vaadin.ui.CssLayout;
import com.vaadin.ui.CustomTable;
import com.vaadin.ui.DateField;
import com.vaadin.ui.FormLayout;
import com.vaadin.ui.HorizontalLayout;
import com.vaadin.ui.Label;
import com.vaadin.ui.Notification;
import com.vaadin.ui.Slider;
import com.vaadin.ui.Table;
import com.vaadin.ui.Table.ColumnGenerator;
import com.vaadin.ui.TextField;
import com.vaadin.ui.TreeTable;
import com.vaadin.ui.UI;
import com.vaadin.ui.VerticalLayout;
import com.vaadin.ui.Window;
import com.vaadin.ui.Window.CloseEvent;
import com.vaadin.ui.themes.BaseTheme;

import de.steinwedel.messagebox.MessageBox;
import ru.xpoft.vaadin.VaadinView;

@Component
@Scope("prototype")
@VaadinView(UserMgr.NAME)
@SuppressWarnings("serial")
@HaloAuthority(authority = "ROLE_EUAA")
public class UserMgr extends VerticalLayout implements View {
	final Logger log = LoggerFactory.getLogger(UserMgr.class);
	public static final String NAME = "zUser Mgr";
	final static SimpleDateFormat df = new SimpleDateFormat("dd-MM-yyyy");
	final static SimpleDateFormat df_long = new SimpleDateFormat("dd MMM yyyy");


	@Autowired
	private CSDAO dao;

	@Autowired
	protected PropertiesService properties;
	
	@Autowired
	protected LDAPService ldap;
	
	@Autowired
	protected JobProcessor jobs;
	
	@Autowired
	@Qualifier("haloMessageSource")
	ReloadableResourceBundleMessageSource messages;

	Label stats = new Label();
    TreeTable treeTable;
    FilterTable table;
    LDAPUser user;
    
    int counter = 0;
	private String last_filter;
	private String last_group_dn;
    Group euaa;
    Slider exp_slider = new Slider(1, 30, 0);

    @PostConstruct
	public void PostConstruct() {
		if (properties.getProperty("push.enabled", "false").equals("false")) {
			UI.getCurrent().setPollInterval(10000);
		}
		setSizeFull();
		addStyleName("idm-requester-view");
	}
		
	protected void build() {
		if (SpringSecurityHelperService.hasRole("ROLE_EUAA_ADMIN")) {
			FormLayout l = new FormLayout();
			l.setSizeUndefined();
			l.setMargin(true);
			l.setStyleName("form-upload");
			l.setCaption("EUAA User Manager Portal Admin Access <span></span>");
			l.setCaptionAsHtml(true);
			l.setSpacing(false);
		
			Set<String> organisations = ldap.getOrganisations();
			final BeanItemContainer<String> org_container = new BeanItemContainer<>(String.class, organisations);
			final ComboBox org_combobox = new ComboBox("Organisation", org_container);
			org_combobox.setValue(user.getMatcherAttr());
			org_combobox.setImmediate(true);
			//combobox.setItemCaptionMode(ItemCaptionMode.);
			//combobox.setItemCaptionPropertyId("profileName");
			org_combobox.setNewItemsAllowed(false);
			org_combobox.setWidth("16em");
			org_combobox.setNullSelectionAllowed(false);
			org_combobox.addValueChangeListener(new ValueChangeListener() {
				@Override
				public void valueChange(ValueChangeEvent event) {
					log.trace((String) event.getProperty().getValue());
					user.setMatcherAttr((String) event.getProperty().getValue());	
					
					updateTreeTable(treeTable);
					table.removeAllItems();
				}});
			l.addComponent(org_combobox);
			addComponent(l);
			
		}
		
		CssLayout izWrapper = new CssLayout();
		izWrapper.setSizeUndefined();
		izWrapper.setStyleName("iz-wrapper");

		Label crlabel = new Label();
		crlabel.setValue("EUAA User Manager Portal (" + user.getUid() + ") <span> - " + user.getMatcherAttr() + "</span>");
		crlabel.setStyleName("status-panel");
		crlabel.setContentMode(ContentMode.HTML); 
		
		HorizontalLayout infoBar = new HorizontalLayout();
		infoBar.setSizeUndefined();
		infoBar.setStyleName("layout-info-bar");
		infoBar.setSpacing(true);
		
		Button b = new Button("refresh");
		//b.setWidth("26px");
		//b.setIcon(new ThemeResource("icons/times-circle.svg"));		
		b.setDescription("Refreh Table");
		((Button) b).setStyleName(BaseTheme.BUTTON_LINK);
		((Button) b).addClickListener(new Button.ClickListener() {
			@Override
			public void buttonClick(ClickEvent event) {
				updateTreeTable(treeTable);
			}
		});
		infoBar.addComponent(b);
	
		
		final TextField search = new TextField();
		search.setCaption("Search");
		search.setWidth("20em");
		
		infoBar.addComponent(search);
		
		Button search_button = new Button("search");
	//	((Button) search_button).setStyleName(BaseTheme.BUTTON_LINK);
		((Button) search_button).addClickListener(new Button.ClickListener() {
			@Override
			public void buttonClick(ClickEvent event) {
				if (search.getValue().trim() != null && !search.getValue().trim().equals("")) {
					treeTable.setEnabled(false);
					updateTable(table, "", search.getValue().trim());
				}
			}
		});
		infoBar.addComponent(search_button);

		Button clear_button = new Button();
		ThemeResource resource = new ThemeResource("icons/times-circle.svg");
		clear_button.setWidth("26px");
		clear_button.setHeight("26px");
		clear_button.setIcon(resource);
		clear_button.setDescription("Clear Search");
			((Button) clear_button).setStyleName(BaseTheme.BUTTON_LINK);
			((Button) clear_button).addClickListener(new Button.ClickListener() {
				@Override
				public void buttonClick(ClickEvent event) {
					search.setValue("");
					treeTable.setEnabled(true);
					table.removeAllItems();
					
				}
			});
		infoBar.addComponent(clear_button);

	
		
		treeTable = new TreeTable();
		//table.setHeight("200px");
		//table.setWidth("100%");
		treeTable.setSizeFull();
		treeTable.setStyleName("table-sftp-iz");
		treeTable.setImmediate(true);
		treeTable.setSelectable(true);
		
		Label crlabel2 = new Label();
		crlabel2.setValue("Users");
		crlabel2.setStyleName("status-panel");
		crlabel2.setContentMode(ContentMode.HTML); 
		
		table = new FilterTable() {
            @Override
            protected String formatPropertyValue(Object rowId, Object colId, Property property) {
                Object v = property.getValue();
                if (v instanceof Date && (((String)colId).equals("endDate") ||  ((String)colId).equals("startDate"))) { return df_long.format((Date) v); }
                return super.formatPropertyValue(rowId, colId, property);
            }

        };
		//table.setHeight("200px");
		//table.setWidth("100%");
		table.setSizeFull();
		table.setStyleName("table-sftp-iz");
		table.setImmediate(true);
		table.setSelectable(true);
		table.setFilterBarVisible(false);
		
		HorizontalLayout infoBar2 =  getTooBarActions(table);
		infoBar2.setSizeUndefined();
		infoBar2.setStyleName("layout-info-bar");
		infoBar2.setSpacing(true);
		
		Button filter = new Button();
		filter.setWidth("38px");
		filter.setHeight("38px");
		filter.setIcon(new ThemeResource("icons/flask.svg"));
		((Button) filter).setStyleName(BaseTheme.BUTTON_LINK);
		((Button) filter).addClickListener(new Button.ClickListener() {
			@Override
			public void buttonClick(ClickEvent event) {
				table.setFilterBarVisible(!table.isFilterBarVisible());
				//event.getButton().setCaption(table.isFilterBarVisible() ? "filter off" : "filter on");
				if (!table.isFilterBarVisible()) { table.clearFilters(); }
			}
		});
		infoBar2.addComponent(filter, 0);
		
		
		Button locked_accounts = new Button();
		locked_accounts.setWidth("38px");
		locked_accounts.setHeight("38px");
		locked_accounts.setDescription("Show All Disabled Accounts");
		locked_accounts.setIcon(new ThemeResource("icons/lock.svg"));
		((Button) locked_accounts).setStyleName(BaseTheme.BUTTON_LINK);
		((Button) locked_accounts).addClickListener(new Button.ClickListener() {
			@Override
			public void buttonClick(ClickEvent event) {
				showDisabledAccounts();
			}
		});
		infoBar2.addComponent(locked_accounts, 1);
		
		
		
		exp_slider.setValue(14d);
		exp_slider.setVisible(false);
		exp_slider.setImmediate(true);
		exp_slider.setWidth("400px");
		exp_slider.addValueChangeListener(new ValueChangeListener() {
			@Override
			public void valueChange(ValueChangeEvent event) {
				showExpiringAccounts();				
			}});
	
		Button expiring_accounts = new Button();
		expiring_accounts.setData(new Boolean(false));
		expiring_accounts.setWidth("38px");
		expiring_accounts.setHeight("38px");
		expiring_accounts.setDescription("Show All Expiring Accounts");
		expiring_accounts.setIcon(new ThemeResource("icons/battery-75.svg"));
		((Button) expiring_accounts).setStyleName(BaseTheme.BUTTON_LINK);
		((Button) expiring_accounts).addClickListener(new Button.ClickListener() {
			@Override
			public void buttonClick(ClickEvent event) {
				if (((Boolean)event.getButton().getData()).booleanValue()) {
					exp_slider.setVisible(false);
					updateTable(table);
					event.getButton().setData(new Boolean(false));
				} else {
					showExpiringAccounts();
					exp_slider.setVisible(true);
					event.getButton().setData(new Boolean(true));
				}
				
			}
		});
		infoBar2.addComponent(expiring_accounts);
		infoBar2.addComponent(exp_slider);
		
		
		table.addGeneratedColumn("iz-action", new FilterTable.ColumnGenerator() {
			@Override
			public com.vaadin.ui.Component generateCell(CustomTable source, final Object itemId, Object columnId) {
				LDAPUser u = (LDAPUser)itemId;
				return getActions(u);
			}
		});
		
		table.addGeneratedColumn("gen-status", new FilterTable.ColumnGenerator() {
			@Override
			public com.vaadin.ui.Component generateCell(CustomTable source, final Object itemId, Object columnId) {
				Label label = new Label();
				label.setStyleName("iz-status-label");
				label.setValue( ((LDAPUser) itemId).getStatus() > -1 ? LDAPUser.STATUS_NAMES[((LDAPUser) itemId).getStatus()] : "" );
				return label;
			}
		});
		
		table.addGeneratedColumn("gen-accountLocked", new FilterTable.ColumnGenerator() {
			@Override
			public com.vaadin.ui.Component generateCell(CustomTable source, final Object itemId, Object columnId) {
				Label label = new Label();
				label.setStyleName("iz-status-label");
				label.setContentMode(ContentMode.HTML);
				label.setValue( ((LDAPUser) itemId).isAccountLocked() ? ("<b>" + LDAPUser.STATUS_NAMES[LDAPUser.STATUS_DISABLED] + "</b>"): LDAPUser.STATUS_NAMES[LDAPUser.STATUS_ENABLED] );
				return label;
			}
		});
		
		
		table.setContainerDataSource(new BeanItemContainer<>(LDAPUser.class, Collections.<LDAPUser>emptyList()));		
		table.setVisibleColumns(new Object[] { "uid", "cn", "email", "isMemberOf", "iz-action"});		
		table.setColumnHeaders(new String[] { "Username", "Name", "E-Mail", "Group", "Action"});
		
		
		treeTable.addContainerProperty("group", String.class, null);
		treeTable.addContainerProperty("user-count", Integer.class, null);
		treeTable.addContainerProperty("group-dn", String.class, null);
		treeTable.addGeneratedColumn("gen-name", new ColumnGenerator() {
			@Override
			public com.vaadin.ui.Component generateCell(Table source, final Object itemId, Object columnId) {
				
				Item item = source.getItem(itemId);
				//log.error(item.toString());
				Property<?> itemproperty = item.getItemProperty("group-dn");
				
				
				if (itemproperty.getValue() != null) {
					String groupdn = (String) itemproperty.getValue();
					Button b = new Button((String)item.getItemProperty("group").getValue());
					((Button) b).setStyleName(BaseTheme.BUTTON_LINK);
					((Button) b).setData((String)item.getItemProperty("group-dn").getValue());
					((Button) b).addClickListener(new Button.ClickListener() {
						@Override
						public void buttonClick(ClickEvent event) {
							String groupdn = (String)event.getButton().getData();
							updateTable(table, groupdn);
							
						}
					});
					return b;
				} else {
					
					return new Label((String)item.getItemProperty("group").getValue());
				}
			}
		});
		
		treeTable.addValueChangeListener(new ValueChangeListener() {

			@Override
			public void valueChange(ValueChangeEvent event) {
				if (treeTable.getValue() != null) {
					Property<?> groupdn = ((Item) treeTable.getItem(treeTable.getValue())).getItemProperty("group-dn");
					if (groupdn.getValue() != null && !((String)groupdn.getValue()).trim().equals("")) {
						updateTable(table, (String)groupdn.getValue());
					}
				}
				
			}});
	
		
		
//		izWrapper.addComponent(crlabel);
//		izWrapper.addComponent(infoBar);
//		izWrapper.addComponent(table);
		
		addComponent(crlabel);
		addComponent(infoBar);
		addComponent(treeTable);
		addComponent(crlabel2);
		addComponent(infoBar2);
		addComponent(table);
		this.setExpandRatio(table, 0.7f);
		this.setExpandRatio(treeTable, 0.3f);
		
		updateTreeTable(treeTable);

	}
	
	private void updateTreeTable(TreeTable table) {

		
		table.getContainerDataSource().removeAllItems();
		treeTable.setVisibleColumns(new Object[] { "group", "user-count", "group-dn"});		
	//	treeTable.setColumnHeaders(new String[] { "Group", "User Count" });
		
		// we need to remove spaces ??
		List<String> environment = Arrays.asList(properties.getProperty("idm.environments", "default, sample").split(", "));
		//environment.add(0, "EUAAs");
		table.addItem(new Object[] { "End User Account Administrators", null, null }, counter++);
		Group euaa = ldap.getGroupByDn(properties.getProperty("idm.requester.group.dn", "cn=EUAA"));
		table.addItem(new Object[] { euaa.getCn(), null, euaa.getDn() }, counter );
		table.setParent(counter, counter-1);
		table.setChildrenAllowed(counter, false);
		counter++;
		
		int x = counter;
		for (String env : environment) {
			table.addItem(new Object[] { env, null, null }, counter);
			table.setCollapsed(counter, false);
			counter++;
		
		}
		
		for (String env : environment) {
			String group_location = properties.getProperty("idm.environments." + env.toLowerCase() + ".groups", "");
			
			List<Group> list = ldap.getGroups(group_location);
			for (Group g : list) {
				log.trace(g.getMatcherAttr());
				if (g.getMatcherAttr().trim().toLowerCase().equals(user.getMatcherAttr().trim().toLowerCase())) {
					log.trace("Matched {1}", g.getMatcherAttr());
					table.addItem(new Object[] { g.getCn(), g.getUniqueMember().size(), g.getDn() }, counter );
					table.setParent(counter, x);
					table.setChildrenAllowed(counter, false);
					log.trace("{1} : {2}", x, counter);
					counter++;
				}
			}			
			x++;
		}
	
		treeTable.setVisibleColumns(new Object[] { "gen-name", "user-count"});		
		treeTable.setColumnHeaders(new String[] { "Group", "User Count" });
		
		
	}
	
	private void resetPassword(LDAPUser u) {
		MessageBox.createQuestion().withCaption("Requester").withMessage("Are you sure you want to reset the paswword for this user?")
	    .withNoButton()
	    .withYesButton(new Runnable() {
			@Override
			public void run() {
				if (jobs.requestPwReset(user.getUid(), u)) {
					Notification.show("Halo Ident.", "User Account (" + u.getUid() + ") Password Reset", Notification.Type.TRAY_NOTIFICATION);
				} else {
					Notification.show("Halo Ident.", "User Account (" + u.getUid() + ") Password Reset Failed", Notification.Type.ERROR_MESSAGE);
				}
			
			}}).open();
	}
	
	private void disableUser(LDAPUser u) {
		MessageBox.createQuestion().withCaption("Requester").withMessage("Are you sure you want to disable this account?")
	    .withNoButton()
	    .withYesButton(new Runnable() {
			@Override
			public void run() {
				if (jobs.disableUser(user.getUid(), u)) {
					Notification.show("Halo Ident.", "User Account (" + u.getUid() + ") Disabled", Notification.Type.TRAY_NOTIFICATION);
					updateTable(table);
					updateTreeTable(treeTable);
				} else {
					Notification.show("Halo Ident.", "Disable User Account (" + u.getUid() + ") Failed", Notification.Type.ERROR_MESSAGE);
				}
			}}).open();
	}
	
    private void makeEUAA(LDAPUser u) {
    	MessageBox.createQuestion().withCaption("Requester").withMessage("Are you sure you want to convert this account to an EUAA?")
	    .withNoButton()
	    .withYesButton(new Runnable() {
			@Override
			public void run() {
				if (ldap.removeUserFromGroup(u.getIsMemberOf(), u.getEntryDN()) && jobs.userToRequester(u))  {
					Notification.show("Halo Ident.", "User Account (" + u.getUid() + ") is now an EUAA", Notification.Type.TRAY_NOTIFICATION);
					updateTable(table);
				} else {
					Notification.show("Halo Ident.", "User Account (" + u.getUid() + ") Update Failed", Notification.Type.ERROR_MESSAGE);
				}
			}}).open();
	}
    
    private void deleteUser(LDAPUser u) {
    	MessageBox.createQuestion().withCaption("Requester").withMessage("Are you sure you want to delete this account?")
	    .withNoButton()
	    .withYesButton(new Runnable() {
			@Override
			public void run() {
				if (ldap.removeUserFromGroup(u.getIsMemberOf(), u.getEntryDN()) && jobs.deleteUser(user.getUid(), u))  {
					Notification.show("Halo Ident.", "User Account (" + u.getUid() + ") has been deleted", Notification.Type.TRAY_NOTIFICATION);
					updateTable(table);
				} else {
					Notification.show("Halo Ident.", "User Account (" + u.getUid() + ") Delete Failed", Notification.Type.ERROR_MESSAGE);
				}
			}}).open();
	}
	
	private void showDisabledAccounts() {
		List<LDAPUser> list = new LinkedList<>();
		if (SpringSecurityHelperService.hasRole("ROLE_EUAA_ADMIN")) {
			list.addAll(ldap.getUsersByStatus(true));
		} else {
			list.addAll(ldap.getUsersByStatusAndMatcher(true, user.getMatcherAttr()));
		}
		 updateTableContents(list);
	}
	
	private void showExpiringAccounts() {
		List<LDAPUser> list = new LinkedList<>();
//		if (SpringSecurityHelperService.hasRole("ROLE_EUAA_ADMIN")) {
//			list.addAll(ldap.getUsersByStatus(true));
//		} else {
//			list.addAll(ldap.getUsersByStatusAndMatcher(true, user.getMatcherAttr()));
//		}
		LocalDate ld = LocalDate.now().plus(exp_slider.getValue().intValue(), ChronoUnit.DAYS);		
		Instant instant = ld.atStartOfDay().atZone(ZoneId.systemDefault()).toInstant();
		Date res = Date.from(instant);
		//list.addAll(ldap.getUsersByEnddateAndMatcher(res, user.getMatcherAttr()));
		list.addAll(ldap.getUsersByEnddateAndMatcherAndStatus(res, user.getMatcherAttr(), false));
		 updateTableContents(list);
	}
	
	
	private void updateTable(FilterTable table) {
		if (last_filter == null) {
			updateTable(table, this.last_group_dn);
		} else {
			updateTable(table, null, this.last_filter);
		}
	}
	
	private void updateTable(FilterTable table, String groupdn, String filter) {
		List<LDAPUser> list = new LinkedList<>();
		this.last_filter = filter;
		this.last_group_dn = null;
		
		if (SpringSecurityHelperService.hasRole("ROLE_EUAA_ADMIN")) {
			list.addAll(ldap.getUsersByUid(filter));
		} else {
			list.addAll(ldap.getUsersByUidAndMatcher(filter, user.getMatcherAttr()));
		}
		 updateTableContents(list);
	}
	
	private void updateTable(FilterTable table, String groupdn) {
		List<LDAPUser> list = new LinkedList<>();
		this.last_group_dn = groupdn;
		this.last_filter = null;
		Group group = ldap.getGroupByDn(groupdn);
		if (group == null) { return; }
		for (String member : group.getUniqueMember()) {
			//log.error(member);
			LDAPUser u = ldap.getUserByDn(member);
			//u.setIsMemberOf(group.getCn());
			if (u.getMatcherAttr().equals(user.getMatcherAttr())) {
				list.add(u);
			}
		}
		 updateTableContents(list);
	}
	
	private void updateTableContents(List<LDAPUser> list) {
		euaa = ldap.getGroupByDn(properties.getProperty("idm.requester.group.dn", "cn=EUAAs"));
		table.removeAllItems();
		
	
		/* private String uid;
	private String entryDN;
	private String cn;
	private String sn;
	private String email;
	private String homeDirectory;
	private String userPassword;
	private int uidNumber;
	private int gidNumber;
	*/
		table.setContainerDataSource(new BeanItemContainer<>(LDAPUser.class, list));	
		table.setVisibleColumns(new Object[] { "uid", "cn", "email", "intEmail", "isMemberOf", "gen-accountLocked", "startDate", "endDate", "lastLogin", "iz-action"});		
		table.setColumnHeaders(new String[] { "Username", "Name", "E-Mail", "Internal E-mail", "Group", "Status", "Start Date", "End Date", "Last Login", "Action"});
		table.setColumnWidth("iz-select", 40);
	}
	
	private Window enableUser(LDAPUser user_bean, boolean editUsername) {
		Window w = new Window();
		UI.getCurrent().addWindow(w);
		w.setVisible(true);
		w.addCloseListener(new Window.CloseListener() {
			@Override
			public void windowClose(CloseEvent e) {
				updateTable(table);
				updateTreeTable(treeTable);
				
			}} );
		VerticalLayout layout = new VerticalLayout();
		layout.setSizeFull();
		layout.addStyleName("idm-requester-view");
		w.setContent(layout);
		w.setModal(true);
		w.setHeight("30%");
		w.setWidth("70%");
		
		FormLayout l = new FormLayout();
		l.setSizeUndefined();
		l.setMargin(true);
		l.setStyleName("form-upload");
		l.setCaption("Enable User <span></span>");
		l.setCaptionAsHtml(true);
		
		final TextField new_username = new TextField();
		if (editUsername && SpringSecurityHelperService.hasRole("ROLE_EUAA_ADMIN")) {	
			new_username.setCaption("Username");
			new_username.setWidth("36em");		
			new_username.setImmediate(true);
			l.addComponent(new_username);			
		} else {
			Label username = new Label(user_bean.getUid());
			username.setCaption("Username");
			l.addComponent(username);
		}
		
		String environment = "Unknown";
		Group group = user_bean.getIsMemberOf() == null ? null : ldap.getGroup(user_bean.getIsMemberOf());
		
		if (group != null) {
			String environments[] = properties.getProperty("idm.environments", "default, sample").split(",");
			for (String env : environments) {
				String groupdn = properties.getProperty("idm.environments." + env.trim().toLowerCase() + ".groups", "--");
				//log.error(group.getDn() + " EndsWith -> " + groupdn);
				if (group.getDn().toLowerCase().endsWith(groupdn.toLowerCase())) {
					environment = env.trim();
					break;
				}
			}
		} 
		
		Label environment_label = new Label(environment);
		environment_label.setCaption("Environment");
		l.addComponent(environment_label);
		

		final String approver_group = properties.getProperty("idm.approver.group.dn", "brm");
		List<LDAPUser> approvers = ldap.getUsersByMatcherAndGroupAndNotLocked(user.getMatcherAttr(), approver_group) ; //new LinkedList<>();//Arrays.asList(new String[] { "Please select an environment" });
		Collections.sort(approvers);
	
		final BeanItemContainer<LDAPUser> approver_container = new BeanItemContainer<>(LDAPUser.class, approvers);
		final ComboBox approver_combobox = new ComboBox("BRM Approver", approver_container);
	
		approver_combobox.setImmediate(true);
		approver_combobox.setItemCaptionMode(ItemCaptionMode.PROPERTY);
		approver_combobox.setItemCaptionPropertyId("cn");
		approver_combobox.setNewItemsAllowed(false);
		approver_combobox.setWidth("26em");
		approver_combobox.setNullSelectionAllowed(false);
		if (approvers.size() > 0) { approver_combobox.setValue(approvers.get(0)); }
		l.addComponent(approver_combobox);
		
		
		
		final DateField enddate_field = new DateField(); 
		enddate_field.setCaption("End Date");
		enddate_field.setWidth("20em");
		
		
		LocalDate ld = LocalDate.now().plus(90, ChronoUnit.DAYS);		
		Instant instant = ld.atStartOfDay().atZone(ZoneId.systemDefault()).toInstant();
		Date res = Date.from(instant);
		enddate_field.setRangeStart(new Date());
		enddate_field.setRangeEnd(res);
		enddate_field.setValue(res);
		enddate_field.setDateOutOfRangeMessage("Maximum End Date is 90 days after the Start Date");
		enddate_field.setLocale(Locale.UK);
		enddate_field.setImmediate(true);
		l.addComponent(enddate_field);
		
		
		
		final TextField businessJustification = new TextField();
		businessJustification.setCaption("Business Justification");
		businessJustification.setWidth("36em");		
		l.addComponent(businessJustification);
		
		
		
		Button cancel = new Button("Cancel", new Button.ClickListener() {
			private static final long serialVersionUID = -4314026625372219213L;

			@Override
			public void buttonClick(ClickEvent event) {
				w.close();
			}
			});
		cancel.setStyleName(BaseTheme.BUTTON_LINK);
		
		HorizontalLayout actions = new HorizontalLayout();
		actions.addComponent(cancel);
		actions.setSpacing(true);
		
		
		if (SpringSecurityHelperService.hasRole("ROLE_EUAA_ADMIN")) {
			actions.addComponent(new Button("Save", new Button.ClickListener() {
				private static final long serialVersionUID = -4314026625372219213L;
				@Override
				public void buttonClick(ClickEvent event) {
				
					
					if (user_bean.getStartDate() == null) { user_bean.setStartDate(new Date()); }
					user_bean.setEndDate(enddate_field.getValue());
					if (jobs.enableUser(user.getUid(), user_bean) ) {
						Notification.show("Halo Ident.", "User Account (" + user_bean.getUid() + ") Enabled", Notification.Type.TRAY_NOTIFICATION);
						if (editUsername) {
							String group = user_bean.getIsMemberOf();
							ldap.removeUserFromGroup(group, user_bean.getEntryDN());
							String newdn = ldap.changeUsername(user_bean, new_username.getValue());	
							if (newdn == null) {
								log.error("Unable to rename user [{} -> {}]", user_bean.getUid(), new_username.getValue());
								ldap.addUserToGroup(group, user_bean.getEntryDN());
							} else {
								ldap.addUserToGroup(group, newdn);
								//user_bean.setHomeDirectory(user_bean.getHomeDirectory().replace(oldChar, newChar)homeDirectory);
							}
							
							
						}
					} else {
						Notification.show("Halo Ident.", "Enable User Account (" + user_bean.getUid() + ") Failed!", Notification.Type.ERROR_MESSAGE);
					}
					
					
					
					
					w.close();
				}
			}));
		}
		
		actions.addComponent(new Button("Submit Request", new Button.ClickListener() {
			private static final long serialVersionUID = -4314026625372219213L;
			@Override
			public void buttonClick(ClickEvent event) {
				
					
				
					if (user_bean.getStartDate() == null) { user_bean.setStartDate(new Date()); }
					UserRequest ur = new UserRequest().init();
					ur.setUsername(user_bean.getUid());
					ur.setFname(user_bean.getGivenName());
					ur.setSname(user_bean.getSn());
					ur.setBusinessJustification(businessJustification.getValue());
					
					ur.setEmployeeID(user_bean.getEmployeeNo());
					ur.setStartDate(user_bean.getStartDate());
					ur.setEndDate(enddate_field.getValue());
					ur.setExEmail(user_bean.getEmail());
					ur.setPhone(user_bean.getPhoneNo());
					ur.setIntEmail(user_bean.getIntEmail());
					ur.setGgroup(user_bean.getIsMemberOf());
					//ur.setNtlogin(user_bean.getN);
					ur.setEnvironment(environment_label.getValue());
					ur.setRequester(user.getUid());
					ur.setRequesterEmail(user.getEmail());
					ur.setOrganisation(user.getMatcherAttr());
					
					LDAPUser approver = ((LDAPUser)approver_combobox.getValue());
					ur.setApprover(approver.getUid());
					ur.setApproverEmail(approver.getEmail());
					
					ur.setStatus(UserRequest.STATUS_REQUESTED);
					ur.setAction(UserRequest.ACTION_ENABLE);
					/*
					 * private String fname;

	
	
	private String ntlogin;
	private String environment;
	private Date startDate;
	private Date endDate;
	private String organisation;
	
	private String username;
	private String requester;
	private String requesterEmail;
	private String approver;
	private String approverEmail;
	
	private int status;
	private String msg;
	private String businessJustification;
					 */
					
					if (jobs.requestUpdateUser(user.getUid(), ur)) {
						Notification.show("Halo Ident.", "Enable User Account (" + user_bean.getUid() + ") Requested", Notification.Type.TRAY_NOTIFICATION);
						
					} else {
						Notification.show("Halo Ident.", "Enable User Account (" + user_bean.getUid() + ") Request Failed!", Notification.Type.ERROR_MESSAGE);
					}
					
					
				
				w.close();
			}
			}));
		
		l.addComponent(actions);
		
		layout.addComponent(l);
		return w;
	}
	
	
	private Window editUser(LDAPUser new_user, boolean creating) {
		Window w = new Window();
		
		UI.getCurrent().addWindow(w);
		w.setVisible(true);
		w.addCloseListener(new Window.CloseListener() {
			@Override
			public void windowClose(CloseEvent e) {
				updateTable(table);
				updateTreeTable(treeTable);
				
			}} );
		VerticalLayout layout = new VerticalLayout();
		layout.setSizeFull();
		layout.addStyleName("idm-requester-view");
		w.setContent(layout);
		w.setModal(true);
		w.setHeight("90%");
		w.setWidth("70%");
		
		FormLayout l = new FormLayout();
		l.setSizeUndefined();
		l.setMargin(true);
		l.setStyleName("form-upload");
		l.setCaption(creating == true ? "Create User <span></span>" : "Modify User <span></span>");
		l.setCaptionAsHtml(true);

		if (!creating) {
			Label username = new Label(new_user.getUid());
			username.setCaption("Username");
			l.addComponent(username);
			
			
		}
		log.error(new_user.getIsMemberOf());
		String environment = "Unknown";
		List<String> users_groups = Arrays.asList(new_user.getIsMemberOf().split(","));
		Group group = ldap.getGroup(users_groups.get(0).trim());
		if (group.equals("EUAA")) { // This is a nasty fix!
			environment = "Omega";
		} else {
		
			if (group != null) {
				String environments[] = properties.getProperty("idm.environments", "default, sample").split(",");
				for (String env : environments) {
					String groupdn = properties.getProperty("idm.environments." + env.trim().toLowerCase() + ".groups", "--");
					//log.error(group.getDn() + " EndsWith -> " + groupdn);
					if (group.getDn().toLowerCase().endsWith(groupdn.toLowerCase())) {
						environment = env.trim();
						break;
					}
				}
			} 
		}
		
		Label environment_label = new Label(environment);
		environment_label.setCaption("Environment");
		l.addComponent(environment_label);
		
		String supplier = new_user.getMatcherAttr(); // == null || new_user.getMatcherAttr().equals("") ? user.getMatcherAttr() : new_user.getMatcherAttr();
		Label supplier_label = new Label(supplier);
		supplier_label.setCaption("Supplier");
		l.addComponent(supplier_label);
		
		String startdate = new_user.getStartDate() != null ? df.format(new_user.getStartDate()) : "Start Date not set. The account Start Date will be set to today when Saved.";
		Label startdate_label = new Label(startdate);
		startdate_label.setCaption("Start Date");
		l.addComponent(startdate_label);
		
	
		
		final BeanFieldGroup<LDAPUser> binder = new BeanFieldGroup<>(LDAPUser.class);
		binder.setItemDataSource(new_user);
		binder.setBuffered(true);
		
		
		/*
		private String uid;
		private String entryDN;
		private String cn;
		private String sn;
		private String givenName;
		private String email;
		private String homeDirectory;
		private String userPassword;
		private int uidNumber;
		private int gidNumber;
		private String isMemberOf;
		private String intEmail;
	
		 */
		if (SpringSecurityHelperService.hasRole("ROLE_EUAA_ADMIN")) {
			l.addComponent(binder.buildAndBind("First Name", "givenName"));
			l.addComponent(binder.buildAndBind("Last Name", "sn"));
		} else {
			l.addComponent(HaloFactory.label("", new_user.getGivenName(), false, "First Name"));
			l.addComponent(HaloFactory.label("", new_user.getSn(), false, "Last Name"));
		}
			
		
		//l.addComponent(binder.buildAndBind("Employee ID", "employeeID"));
		l.addComponent(binder.buildAndBind("Email", "email"));
		l.addComponent(binder.buildAndBind("VF Email", "intEmail"));
		l.addComponent(binder.buildAndBind("Phone Number", "phoneNo"));
		l.addComponent(binder.buildAndBind("Employee Number", "employeeNo"));
		
		l.addComponent(binder.buildAndBind("End Date", "endDate"));
		LocalDate ld = LocalDate.now().plus(90, ChronoUnit.DAYS);		
		Instant instant = ld.atStartOfDay().atZone(ZoneId.systemDefault()).toInstant();
		Date res = Date.from(instant);
		DateField enddate_field = (DateField) binder.getField("endDate");
		enddate_field.setRangeStart(new_user.getStartDate() != null ? new_user.getStartDate() : new Date());
		enddate_field.setRangeEnd(res);
		enddate_field.setDateOutOfRangeMessage("Maximum End Date is 90 days after the Start Date");
		enddate_field.setLocale(Locale.UK);
		enddate_field.setImmediate(true);

	
		
		
		final BeanItemContainer<Group> group_container = new BeanItemContainer<>(Group.class, new LinkedList<Group>());
		final ComboBox group_combobox = new ComboBox("Group", group_container);
		
		if (SpringSecurityHelperService.hasRole("ROLE_EUAA_ADMIN") || (group.equals("EUAA"))) { // This is a nasty fix!) {
			Group euaa = ldap.getGroupByDn(properties.getProperty("idm.requester.group.dn", "cn=EUAAs"));
			group_container.addItem(euaa);
		}
		if (!group.equals("EUAA")) { // This is a nasty fix!
			String group_location = properties.getProperty("idm.environments." + environment.toLowerCase() + ".groups", "");		
			List<Group> list = ldap.getGroups(group_location);
			for (Group g: list) {
				//log.info(g.toString());
				if (g.getMatcherAttr().equalsIgnoreCase(new_user.getMatcherAttr())) {
					group_container.addItem(g);
				}
			}
		}
		
		group_combobox.setImmediate(true);
		group_combobox.setItemCaptionMode(ItemCaptionMode.PROPERTY);
		group_combobox.setItemCaptionPropertyId("cn");
		group_combobox.setNewItemsAllowed(false);
		group_combobox.setWidth("26em");
		group_combobox.setNullSelectionAllowed(false);
		log.trace(new_user.getIsMemberOf());
		//group_combobox.setValue(ldap.getGroup(new_user.getIsMemberOf()));
		group_combobox.setValue(group);
		l.addComponent(group_combobox);
		
		
		final String approver_group = properties.getProperty("idm.approver.group.dn", "brm");
		List<LDAPUser> approvers = ldap.getUsersByMatcherAndGroupAndNotLocked(user.getMatcherAttr(), approver_group) ; //new LinkedList<>();//Arrays.asList(new String[] { "Please select an environment" });
		Collections.sort(approvers);
	
		final BeanItemContainer<LDAPUser> approver_container = new BeanItemContainer<>(LDAPUser.class, approvers);
		final ComboBox approver_combobox = new ComboBox("BRM Approver", approver_container);
	
		approver_combobox.setImmediate(true);
		approver_combobox.setItemCaptionMode(ItemCaptionMode.PROPERTY);
		approver_combobox.setItemCaptionPropertyId("cn");
		approver_combobox.setNewItemsAllowed(false);
		approver_combobox.setWidth("26em");
		approver_combobox.setNullSelectionAllowed(false);
		if (approvers.size() > 0) { approver_combobox.setValue(approvers.get(0)); }
		l.addComponent(approver_combobox);
		
		//l.addComponent(binder.buildAndBind("Business Justification", "businessJustification"));
		//((TextField)binder.getField("businessJustification")).setWidth("36em");
		final TextField businessJustification = new TextField();
		businessJustification.setCaption("Business Justification");
		businessJustification.setWidth("36em");		
		l.addComponent(businessJustification);
		
		HorizontalLayout actions = new HorizontalLayout();
		actions.setSpacing(true);
		
				
		Button cancel = new Button("Cancel", new Button.ClickListener() {
			private static final long serialVersionUID = -4314026625372219213L;

			@Override
			public void buttonClick(ClickEvent event) {
				w.close();
			}
			});
		cancel.setStyleName(BaseTheme.BUTTON_LINK);
		actions.addComponent(cancel);
		
		if (SpringSecurityHelperService.hasRole("ROLE_EUAA_ADMIN")) {
			actions.addComponent(new Button("Save", new Button.ClickListener() {
				private static final long serialVersionUID = -4314026625372219213L;
				@Override
				public void buttonClick(ClickEvent event) {
					try {
						binder.commit();
						
						LDAPUser user_bean = binder.getItemDataSource().getBean();
						user_bean.setCn(user_bean.getGivenName() + " " + user_bean.getSn());
						if (user_bean.getStartDate() == null) { user_bean.setStartDate(new Date()); }
						if (creating) {
							// Should this be a scheduled job?
							log.error(user_bean.toString());
							jobs.updateUser(user.getUid(), user_bean, false);
							if ((Group)group_combobox.getValue() != null) {
								jobs.changeGroup(user.getUid(), user_bean, (Group)group_combobox.getValue());
							}
							
						} else { 
							if (jobs.updateUser(user.getUid(), user_bean, false) ) {
								if (ldap.getUserByDn(new_user.getEntryDN()).isAccountLocked()) {
									new_user.setAccountLocked(false);
									ldap.updateUserStatus(new_user);
								}
								Notification.show("Halo Ident.", "User Account (" + user_bean.getUid() + ") Updated", Notification.Type.TRAY_NOTIFICATION);
							} else {
								Notification.show("Halo Ident.", "User Account (" + user_bean.getUid() + ") Update Failed!", Notification.Type.ERROR_MESSAGE);
							}
							if (group_combobox.getValue() != null && !((Group)group_combobox.getValue()).getCn().equals(user_bean.getIsMemberOf())) {
								if (jobs.changeGroup(user.getUid(), user_bean, (Group)group_combobox.getValue())) {
									Notification.show("Halo Ident.", "User Account (" + user_bean.getUid() + ") Group Updated", Notification.Type.TRAY_NOTIFICATION);
								} else {
									Notification.show("Halo Ident.", "User Account (" + user_bean.getUid() + ") Group Update Failed!", Notification.Type.ERROR_MESSAGE);
								}
							}
						}
						
						
					} catch (CommitException e) {
						// TODO Auto-generated catch block
						e.printStackTrace();
					}
					w.close();
				}
			}));
		}
		
		actions.addComponent(new Button("Submit Request", new Button.ClickListener() {
			private static final long serialVersionUID = -4314026625372219213L;
			@Override
			public void buttonClick(ClickEvent event) {
				try {
					binder.commit();
					Group g = (Group)group_combobox.getValue();
					LDAPUser user_bean = binder.getItemDataSource().getBean();	
					if (user_bean.getStartDate() == null) { user_bean.setStartDate(new Date()); }
					UserRequest ur = new UserRequest().init();
					ur.setUsername(user_bean.getUid());
					ur.setFname(user_bean.getGivenName());
					ur.setSname(user_bean.getSn());
					ur.setBusinessJustification(businessJustification.getValue());
					
					ur.setEmployeeID(user_bean.getEmployeeNo());
					ur.setStartDate(user_bean.getStartDate());
					ur.setEndDate(user_bean.getEndDate());
					ur.setExEmail(user_bean.getEmail());
					ur.setPhone(user_bean.getPhoneNo());
					ur.setIntEmail(user_bean.getIntEmail());
					ur.setGgroup(g.getCn());
					//ur.setNtlogin(user_bean.getN);
					ur.setEnvironment(environment_label.getValue());
					ur.setRequester(user.getUid());
					ur.setRequesterEmail(user.getEmail());
					ur.setOrganisation(user.getMatcherAttr());
					
					LDAPUser approver = ((LDAPUser)approver_combobox.getValue());
					ur.setApprover(approver.getUid());
					ur.setApproverEmail(approver.getEmail());
					
					ur.setStatus(UserRequest.STATUS_REQUESTED);
					ur.setAction(UserRequest.ACTION_UPDATE);
					/*
					 * private String fname;

	
	
	private String ntlogin;
	private String environment;
	private Date startDate;
	private Date endDate;
	private String organisation;
	
	private String username;
	private String requester;
	private String requesterEmail;
	private String approver;
	private String approverEmail;
	
	private int status;
	private String msg;
	private String businessJustification;
					 */
					
					if (jobs.requestUpdateUser(user.getUid(), ur)) {
						Notification.show("Halo Ident.", "User Account (" + user_bean.getUid() + ") Update Requested", Notification.Type.TRAY_NOTIFICATION);
					} else {
						Notification.show("Halo Ident.", "User Account (" + user_bean.getUid() + ") Update Failed!", Notification.Type.ERROR_MESSAGE);
					}
					
					
					
				} catch (CommitException e) {
					// TODO Auto-generated catch block
					e.printStackTrace();
				}
				w.close();
			}
			}));
		
		
		
		if (creating) {
		actions.addComponent(new Button("Save & Nofity", new Button.ClickListener() {
			private static final long serialVersionUID = -4314026625372219213L;
			@Override
			public void buttonClick(ClickEvent event) {
				try {
					binder.commit();
					LDAPUser user_bean = binder.getItemDataSource().getBean();
					
						// Should this be a scheduled job?
						log.error(user_bean.toString());
						jobs.requestNewApprover(user.getUid(), user_bean, true);
					
				} catch (CommitException e) {
					// TODO Auto-generated catch block
					e.printStackTrace();
				}
				
				w.close();
			}
			}));
		}
		l.addComponent(actions);
		
		layout.addComponent(l);
		
		return w;
	}
	
	
	private HorizontalLayout getActions(LDAPUser u) {
		HorizontalLayout actions = new HorizontalLayout();
		actions.addStyleName("table-actions");

		{ 
			Button	b = new Button();
			((Button) b).setStyleName(BaseTheme.BUTTON_LINK);
			b.setIcon(new ThemeResource("icons/star-exclamation.svg"));
			b.setWidth("14px");
			b.setHeight("20px");
			b.setDescription("Reset Password");
			b.setData(u);
			b.addClickListener(e->resetPassword((LDAPUser)e.getButton().getData()));			
			actions.addComponent(b);
		}
			
		actions.addComponent(HaloFactory.label("sep", " / ", false));
		{ 
			Button	b = new Button();
			b.setStyleName(BaseTheme.BUTTON_LINK);
			b.setIcon(new ThemeResource("icons/edit.svg"));
			b.setWidth("14px");
			b.setHeight("20px");
			b.setDescription("Edit User Details");
			b.setData(u);
			b.addClickListener(e->editUser((LDAPUser)e.getButton().getData(), false));	
			actions.addComponent(b);
		}
			
		actions.addComponent(HaloFactory.label("sep", " / ", false));
		if (!u.isAccountLocked()) { 
			Button	b = new Button();
			b.setStyleName(BaseTheme.BUTTON_LINK);
			b.setIcon(new ThemeResource("icons/bomb.svg"));
			b.setWidth("14px");
			b.setHeight("20px");
			b.setDescription("Disable User");
			b.setData(u);
			b.addClickListener(e->disableUser((LDAPUser)e.getButton().getData()));	
			actions.addComponent(b);
		
		} else {
			Button	b = new Button();
			b.setStyleName(BaseTheme.BUTTON_LINK);
			b.setIcon(new ThemeResource("icons/smile.svg"));
			b.setWidth("14px");
			b.setHeight("20px");
			b.setDescription("Enable User");
			b.setData(u);
			b.addClickListener(e->enableUser((LDAPUser)e.getButton().getData(), e.isShiftKey()));	
			actions.addComponent(b);
		}
		
			
		if (SpringSecurityHelperService.hasRole("ROLE_IDM_ADMIN")) { 	
			
			if (!euaa.getUniqueMember().contains(u.getEntryDN())) {
				actions.addComponent(HaloFactory.label("sep", " / ", false));
				Button	b = new Button();
				b.setDescription("Convert User to an EUAA");
				b.setStyleName(BaseTheme.BUTTON_LINK);
				b.setIcon(new ThemeResource("icons/magic.svg"));
				b.setWidth("14px");
				b.setHeight("20px");
				b.setData(u);
				b.addClickListener(e->makeEUAA((LDAPUser)e.getButton().getData()));					
				actions.addComponent(b);
			}
			actions.addComponent(HaloFactory.label("sep", " / ", false));
			Button	b = new Button();
			b.setStyleName(BaseTheme.BUTTON_LINK);
			b.setIcon(new ThemeResource("icons/times-circle.svg"));
			b.setWidth("14px");
			b.setHeight("20px");
			b.setDescription("Delete User");
			b.setData(u);
			b.addClickListener(e->deleteUser((LDAPUser)e.getButton().getData()));	
			actions.addComponent(b);
		}
		return actions;
	}
	
	
	private HorizontalLayout getTooBarActions(FilterTable table) {
		HorizontalLayout actions = new HorizontalLayout();
		actions.addStyleName("table-actions");
		final String button_size = "38px";

		{ 
			Button	b = new Button();
			((Button) b).setStyleName(BaseTheme.BUTTON_LINK);
			b.setIcon(new ThemeResource("icons/star-exclamation.svg"));
			b.setWidth(button_size);
			b.setHeight(button_size);
			b.setDescription("Reset Password");
			b.setEnabled(false);
			b.addClickListener(e->resetPassword((LDAPUser) table.getValue()));
			actions.addComponent(b);
			table.addValueChangeListener(new ValueChangeListener() {				
				@Override
				public void valueChange(ValueChangeEvent event) {
					LDAPUser u = (LDAPUser) table.getValue();
					if (u == null) { b.setEnabled(false); return; }
					b.setEnabled(true);
					
				}} );
		}
			
	
		{ 
			Button	b = new Button();
			b.setStyleName(BaseTheme.BUTTON_LINK);
			b.setIcon(new ThemeResource("icons/pencil-alt.svg"));
			b.setWidth(button_size);
			b.setHeight(button_size);
			b.setDescription("Edit User Details");
			b.setEnabled(false);
			b.addClickListener(e->editUser((LDAPUser) table.getValue(), false));
				
			actions.addComponent(b);
			table.addValueChangeListener(new ValueChangeListener() {				
				@Override
				public void valueChange(ValueChangeEvent event) {
					LDAPUser u = (LDAPUser) table.getValue();
					if (u == null) { b.setEnabled(false); return; }
					b.setEnabled(true);
					
				}} );
		}
			
		
		
		{
			Button	b = new Button();
			b.setStyleName(BaseTheme.BUTTON_LINK);
			b.setIcon(new ThemeResource("icons/bomb.svg"));
			b.setWidth(button_size);
			b.setHeight(button_size);
			b.setDescription("Enable/Disable User");
			b.setEnabled(false);
			b.addClickListener(new Button.ClickListener() {
				@Override
				public void buttonClick(ClickEvent event) {
					LDAPUser u = (LDAPUser) table.getValue();
					if (u == null) { return; }
					if (!u.isAccountLocked()) {
						disableUser(u);
					} else {
					    enableUser(u, event.isShiftKey());
					}
				}
			});
			actions.addComponent(b);
			table.addValueChangeListener(new ValueChangeListener() {
	
				@Override
				public void valueChange(ValueChangeEvent event) {
					LDAPUser u = (LDAPUser) table.getValue();
					if (u == null) { b.setEnabled(false); return; }
					b.setEnabled(true);
					if (!u.isAccountLocked()) {
						b.setIcon(new ThemeResource("icons/bomb.svg"));
						b.setDescription("Disable User");
					} else {
						b.setIcon(new ThemeResource("icons/smile.svg"));
						b.setDescription("Enable User");
					}
					
				}} );
		}
			
		if (SpringSecurityHelperService.hasRole("ROLE_IDM_ADMIN")) { 				
		//	if (!euaa.getUniqueMember().contains(u.getEntryDN())) {
			
			Button	b = new Button();
			b.setDescription("Convert User to an EUAA");
			b.setStyleName(BaseTheme.BUTTON_LINK);
			b.setIcon(new ThemeResource("icons/magic.svg"));
			b.setWidth(button_size);
			b.setHeight(button_size);
			b.setEnabled(false);
			
			b.addClickListener(e->makeEUAA((LDAPUser) table.getValue()));
			actions.addComponent(b);
			
			table.addValueChangeListener(new ValueChangeListener() {				
				@Override
				public void valueChange(ValueChangeEvent event) {
					LDAPUser u = (LDAPUser) table.getValue();
					if (u == null) { b.setEnabled(false); return; }
					b.setEnabled(!euaa.getUniqueMember().contains(u.getEntryDN()));
					
					if (log.isTraceEnabled()) {
						log.trace("Selected Entry: " + u.getEntryDN() + "- Entries....");
						for (String entry : euaa.getUniqueMember()) {
							log.trace(entry);
						}
					}
				}} );
			
		}
		return actions;
	}
	

	
	@Override
	public void enter(ViewChangeEvent event) {
		user = ldap.getUser(((HaloUI)UI.getCurrent()).getUsername() );
		
		build();		
	}

}
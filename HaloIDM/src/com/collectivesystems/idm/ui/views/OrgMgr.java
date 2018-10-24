package com.collectivesystems.idm.ui.views;  

import java.util.Arrays;
import java.util.Collections;
import java.util.List;
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
import com.collectivesystems.core.ui.providers.HaloUI;
import com.collectivesystems.idm.beans.Group;
import com.collectivesystems.idm.beans.LDAPUser;
import com.collectivesystems.idm.services.service.JobProcessor;
import com.collectivesystems.idm.services.service.LDAPService;
import com.vaadin.data.Item;
import com.vaadin.data.Property.ValueChangeEvent;
import com.vaadin.data.Property.ValueChangeListener;
import com.vaadin.data.fieldgroup.BeanFieldGroup;
import com.vaadin.data.fieldgroup.FieldGroup.CommitException;
import com.vaadin.data.util.BeanItemContainer;
import com.vaadin.navigator.View;
import com.vaadin.navigator.ViewChangeListener.ViewChangeEvent;
import com.vaadin.server.ThemeResource;
import com.vaadin.shared.ui.label.ContentMode;
import com.vaadin.ui.Button;
import com.vaadin.ui.Button.ClickEvent;
import com.vaadin.ui.ComboBox;
import com.vaadin.ui.CssLayout;
import com.vaadin.ui.CustomTable;
import com.vaadin.ui.CustomTable.Align;
import com.vaadin.ui.FormLayout;
import com.vaadin.ui.HorizontalLayout;
import com.vaadin.ui.Label;
import com.vaadin.ui.Notification;
import com.vaadin.ui.Table;
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
@VaadinView(OrgMgr.NAME)
@SuppressWarnings("serial")
@HaloAuthority(authority = "ROLE_IDM_ADMIN")
public class OrgMgr extends VerticalLayout implements View {
	final Logger log = LoggerFactory.getLogger(OrgMgr.class);
	public static final String NAME = "zOrg Mgr";

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

    TreeTable treeTable;
    FilterTable table;
    LDAPUser user;
    
    int counter = 0;
    final LDAPUser add_user_placeholder = new LDAPUser();
    final String all_organisations_placeholder = "All Organisations";

    @PostConstruct
	public void PostConstruct() {
		if (properties.getProperty("push.enabled", "false").equals("false")) {
			UI.getCurrent().setPollInterval(10000);
		}
		setSizeFull();
		addStyleName("idm-requester-view");
		
		add_user_placeholder.setUid("");
		add_user_placeholder.setCn("");
		add_user_placeholder.setSn("");
		add_user_placeholder.setEmail("");
		add_user_placeholder.setIsMemberOf("");
		
	}
		
	protected void build() {
		
		FormLayout l = new FormLayout();
		l.setSizeUndefined();
		l.setMargin(true);
		l.setStyleName("form-upload");
		l.setCaption("Organisation Manager Admin Access <span></span>");
		l.setCaptionAsHtml(true);
		l.setSpacing(false);
		
		final Button new_group = new Button("add group");
		((Button) new_group).setStyleName(BaseTheme.BUTTON_LINK);
		((Button) new_group).addClickListener(new Button.ClickListener() {
			@Override
			public void buttonClick(ClickEvent event) {
				Window u = addGroup("", false);
				UI.getCurrent().addWindow(u);
				u.setVisible(true);
				u.addCloseListener(new Window.CloseListener() {
					@Override
					public void windowClose(CloseEvent e) {
						updateTreeTable(treeTable);
						updateTable(table);
						
					}} );
			}
		});
		

		Set<String> organisations = ldap.getOrganisations();
		final BeanItemContainer<String> org_container = new BeanItemContainer<>(String.class, organisations);
		org_container.addItemAt(0, this.all_organisations_placeholder);
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
				if (event.getProperty().getValue() == all_organisations_placeholder) {
					new_group.setEnabled(false);
				} else {
					new_group.setEnabled(true);
				}
				user.setMatcherAttr((String) event.getProperty().getValue());	

				updateTreeTable(treeTable);
				updateTable(table);
				
			}});
		l.addComponent(org_combobox);
		
		Button new_org = new Button("add organisation");
		((Button) new_org).setStyleName(BaseTheme.BUTTON_LINK);
		((Button) new_org).addClickListener(new Button.ClickListener() {
			@Override
			public void buttonClick(ClickEvent event) {
				final Window u = addGroup("", true);
				UI.getCurrent().addWindow(u);
				u.setVisible(true);
				u.addCloseListener(new Window.CloseListener() {
					@Override
					public void windowClose(CloseEvent e) {
						String new_org = (String)u.getData();
						if (new_org != null) {
							Set<String> organisations = ldap.getOrganisations();
							org_container.removeAllItems();
							org_container.addAll(organisations);
							org_combobox.setValue(new_org);
						}
						updateTreeTable(treeTable);
						updateTable(table);					
						
					}} );
			}
		});
		l.addComponent(new_org);
		
		
		CssLayout izWrapper = new CssLayout();
		izWrapper.setSizeUndefined();
		izWrapper.setStyleName("iz-wrapper");

		Label crlabel = new Label();
		crlabel.setValue("Groups <span> - " + user.getMatcherAttr() + "</span>");
		crlabel.setStyleName("status-panel");
		crlabel.setContentMode(ContentMode.HTML); 
		
		HorizontalLayout infoBar = new HorizontalLayout();
		infoBar.setSizeUndefined();
		infoBar.setStyleName("layout-info-bar");
		infoBar.setSpacing(true);
		
		Button	b = new Button("refresh");
//		ThemeResource icon = new ThemeResource("icons/yin-yang.svg");
//		b.setIcon(icon);
		b.setDescription("Refresh Table");
//		b.setWidth("26px");
		((Button) b).setStyleName(BaseTheme.BUTTON_LINK);		
		((Button) b).addClickListener(new Button.ClickListener() {
			@Override
			public void buttonClick(ClickEvent event) {
				updateTreeTable(treeTable);
			}
		});
		infoBar.addComponent(b);
		infoBar.addComponent(new_group);
		
		
	
		
		treeTable = new TreeTable();
		//table.setHeight("200px");
		//table.setWidth("100%");
		treeTable.setSizeFull();
		treeTable.setStyleName("table-sftp-iz");
		treeTable.setImmediate(true);
		treeTable.setSelectable(false);
		
		treeTable.addGeneratedColumn("iz-action", new Table.ColumnGenerator() {
			@Override
			public com.vaadin.ui.Component generateCell(Table source, final Object itemId, Object columnId) {
			
				Integer o = (Integer)source.getItem(itemId).getItemProperty("user-count").getValue();
				if (o != null && o == 0) {
					Object g = source.getItem(itemId).getItemProperty("group-dn").getValue();
					if (g == null) { log.error("group dn is null [" + source.getItem(itemId) + "]"); return null; }
										
					HorizontalLayout actions = new HorizontalLayout();
					actions.addStyleName("table-actions");					
					{ 
						Button	b = new Button();
						b.setStyleName(BaseTheme.BUTTON_LINK);
						ThemeResource resource = new ThemeResource("icons/times-circle.svg");
						b.setWidth("14px");
						b.setHeight("20px");
						b.setData(g);
						b.setDescription("Delete Group");
						b.setIcon(resource);
						((Button) b).addClickListener(new Button.ClickListener() {
							@Override
							public void buttonClick(ClickEvent event) {
								MessageBox.createQuestion().withCaption("Org Manager").withMessage("Are you sure you want to delete this group?")
							    .withNoButton()
							    .withYesButton(new Runnable() {
									@Override
									public void run() {
										if (jobs.deleteGroup(user.getUid(), (String) b.getData())) {
											Notification.show("Halo Ident.", "Group (" + b.getData() +  ") Deleted", Notification.Type.TRAY_NOTIFICATION);
											updateTable(table);
											updateTreeTable(treeTable);
										} else {
											Notification.show("Halo Ident.", "Delete Group (" + b.getData() + ") Failed", Notification.Type.ERROR_MESSAGE);
										}
									}}).open();
								
								
								
							}
						});
						actions.addComponent(b);
					}
					
					return actions;
				} else { return null; }
			}
		});
		
			Label crlabel2 = new Label();
			crlabel2.setValue("Business Relationship Managers (BRMs)");
			crlabel2.setStyleName("status-panel");
			crlabel2.setContentMode(ContentMode.HTML); 
		
			HorizontalLayout infoBar2 = new HorizontalLayout();
			infoBar2.setSizeUndefined();
			infoBar2.setStyleName("layout-info-bar");
			infoBar2.setSpacing(true);
			
			Button filter = new Button("filter on");
			((Button) filter).setStyleName(BaseTheme.BUTTON_LINK);
			((Button) filter).addClickListener(new Button.ClickListener() {
				@Override
				public void buttonClick(ClickEvent event) {
					table.setFilterBarVisible(!table.isFilterBarVisible());
					event.getButton().setCaption(table.isFilterBarVisible() ? "filter off" : "filter on");
					if (!table.isFilterBarVisible()) { table.clearFilters(); }
				}
			});
			infoBar2.addComponent(filter);
			
			Button	refresh = new Button("add user");
			((Button) refresh).setStyleName(BaseTheme.BUTTON_LINK);
			((Button) refresh).addClickListener(new Button.ClickListener() {
				@Override
				public void buttonClick(ClickEvent event) {
					
					LDAPUser ldapuser = new LDAPUser().init();
					ldapuser.setMatcherAttr(user.getMatcherAttr() == all_organisations_placeholder ? "" : user.getMatcherAttr());
					Window u = editUser(ldapuser, true);
					UI.getCurrent().addWindow(u);
					u.setVisible(true);
					u.addCloseListener(new Window.CloseListener() {
						@Override
						public void windowClose(CloseEvent e) {
							updateTable(table);
							
						}} );
					//Notification.show("Halo Ident.", "User Account (" + ((UserRequest)itemId).getUsername() + ") Resumed", Notification.Type.TRAY_NOTIFICATION);
				}
			});
			infoBar2.addComponent(refresh);
			
			
		table = new FilterTable();
		//table.setHeight("200px");
		//table.setWidth("100%");
		table.setSizeFull();
		table.setStyleName("table-sftp-iz");
		table.setImmediate(true);
		table.setSelectable(true);
		table.setFilterBarVisible(false);
		
		
		table.addGeneratedColumn("iz-action", new FilterTable.ColumnGenerator() {
			@Override
			public com.vaadin.ui.Component generateCell(CustomTable source, final Object itemId, Object columnId) {
				
				HorizontalLayout actions = new HorizontalLayout();
				actions.addStyleName("table-actions");
				if (itemId != add_user_placeholder) {
//				 if (((LDAPUser)itemId).getStatus() != UserRequest.STATUS_TERMINATED) { 
//				{
//					Button	b = new Button("suspend");
//					((Button) b).setStyleName(BaseTheme.BUTTON_LINK);
//					((Button) b).setData(itemId);
//					((Button) b).addClickListener(new Button.ClickListener() {
//						@Override
//						public void buttonClick(ClickEvent event) {
//							LDAPUser u = (LDAPUser)itemId;
//							ldap.move(u.getEntryDN(), "uid=" + u.getUid() + "," + properties.getProperty("idm.disabled.users.dn", "ou=Disabled Users"));
//							Notification.show("Halo Ident.", "User Account (" + ((LDAPUser)itemId).getUid() + ") Suspended", Notification.Type.TRAY_NOTIFICATION);
//						}
//					});
//					actions.addComponent(b);
//			    }
//				 actions.addComponent(HaloLabelFactory.label("sep", " / ", false));
				// } else 
				{ 
					Button	b = new Button();
					b.setStyleName(BaseTheme.BUTTON_LINK);
					ThemeResource resource = new ThemeResource("icons/edit.svg");
					b.setWidth("14px");
					b.setHeight("20px");
					
					b.setDescription("Edit User Details");
					b.setIcon(resource);
					((Button) b).setData(itemId);
					((Button) b).addClickListener(new Button.ClickListener() {
						@Override
						public void buttonClick(ClickEvent event) {
							
							
							Window u = editUser(((LDAPUser)itemId), false);
							UI.getCurrent().addWindow(u);
							u.setVisible(true);
							u.addCloseListener(new Window.CloseListener() {
								@Override
								public void windowClose(CloseEvent e) {
									updateTable(table);
									
								}} );
							//Notification.show("Halo Ident.", "User Account (" + ((UserRequest)itemId).getUsername() + ") Resumed", Notification.Type.TRAY_NOTIFICATION);
						}
					});
					actions.addComponent(b);
				}
				 actions.addComponent(HaloFactory.label("sep", " / ", false));
				{ 
					Button	b = new Button();
					b.setIcon(new ThemeResource("icons/lock.svg"));
					b.setDescription("Reset Password");
					b.setWidth("14px");
					b.setHeight("20px");
					((Button) b).setStyleName(BaseTheme.BUTTON_LINK);
					((Button) b).setData(itemId);
					((Button) b).addClickListener(new Button.ClickListener() {
						@Override
						public void buttonClick(ClickEvent event) {
							MessageBox.createQuestion().withCaption("Requester").withMessage("Are you sure you want to reset the paswword for this user?")
						    .withNoButton()
						    .withYesButton(new Runnable() {

								@Override
								public void run() {
									LDAPUser u = (LDAPUser)event.getButton().getData();
									if (jobs.requestBRMPwReset(user.getUid(), u)) {
										Notification.show("Halo Ident.", "Approver Account (" + u.getUid() + ") Password Reset", Notification.Type.TRAY_NOTIFICATION);
									} else {
										Notification.show("Halo Ident.", "Approver Account (" + u.getUid() + ") Password Reset Failed", Notification.Type.ERROR_MESSAGE);
									}
								}}).open();
						}
					});
					actions.addComponent(b);
				}
				} else {
					Button	b = new Button("add user");
					((Button) b).setStyleName(BaseTheme.BUTTON_LINK);
					((Button) b).addClickListener(new Button.ClickListener() {
						@Override
						public void buttonClick(ClickEvent event) {
							
							LDAPUser ldapuser = new LDAPUser().init();
							ldapuser.setMatcherAttr(user.getMatcherAttr() == all_organisations_placeholder ? "" : user.getMatcherAttr());
							Window u = editUser(ldapuser, true);
							UI.getCurrent().addWindow(u);
							u.setVisible(true);
							u.addCloseListener(new Window.CloseListener() {
								@Override
								public void windowClose(CloseEvent e) {
									updateTable(table);
									
								}} );
							//Notification.show("Halo Ident.", "User Account (" + ((UserRequest)itemId).getUsername() + ") Resumed", Notification.Type.TRAY_NOTIFICATION);
						}
					});
					actions.addComponent(b);
				}
				return actions;
			}
		});
		
//		table.addGeneratedColumn("gen-name", new FilterTable.ColumnGenerator() {
//			@Override
//			public com.vaadin.ui.Component generateCell(CustomTable source, final Object itemId, Object columnId) {
//				
//				
//				Item item = source.getItem(itemId);
//				//log.error(item.toString());
//				Property<?> itemproperty = item.getItemProperty("group-dn");
//				
//				
//				if (itemproperty.getValue() != null) {
//					String groupdn = (String) itemproperty.getValue();
//					Button b = new Button((String)item.getItemProperty("group").getValue());
//					((Button) b).setStyleName(BaseTheme.BUTTON_LINK);
//					((Button) b).setData((String)item.getItemProperty("group-dn").getValue());
//					((Button) b).addClickListener(new Button.ClickListener() {
//						@Override
//						public void buttonClick(ClickEvent event) {
//							String groupdn = (String)event.getButton().getData();
//							updateTable(table, groupdn);
//							
//						}
//					});
//					return b;
//				} else {
//					
//					return new Label((String)item.getItemProperty("group").getValue());
//				}
//			}
//		});
		
		
		table.setContainerDataSource(new BeanItemContainer<>(LDAPUser.class, Collections.<LDAPUser>emptyList()));		
		table.setVisibleColumns(new Object[] { "uid", "cn", "email", "isMemberOf", "iz-action"});		
		table.setColumnHeaders(new String[] { "Username", "Name", "E-Mail", "Group", "Actions"});
		
		
		treeTable.addContainerProperty("group", String.class, null);
		treeTable.addContainerProperty("user-count", Integer.class, null);
		treeTable.addContainerProperty("group-dn", String.class, null);
		
//		treeTable.setVisibleColumns(new Object[] { "gen-name", "user-count"});		
//		treeTable.setColumnHeaders(new String[] { "Group", "User Count" });
	
		
		
		
//		izWrapper.addComponent(crlabel);
//		izWrapper.addComponent(infoBar);
//		izWrapper.addComponent(table);
		addComponent(l);
		addComponent(crlabel);
		addComponent(infoBar);
		addComponent(treeTable);
		addComponent(crlabel2);
		addComponent(infoBar2);
		addComponent(table);
		this.setExpandRatio(table, 0.7f);
		this.setExpandRatio(treeTable, 0.3f);
		
		updateTreeTable(treeTable);
		updateTable(table);

	}
	
	private void updateTreeTable(TreeTable table) {

		
		table.getContainerDataSource().removeAllItems();
		treeTable.setVisibleColumns(new Object[] { "group", "user-count", "group-dn", "iz-action"});		
	//	treeTable.setColumnHeaders(new String[] { "Group", "User Count" });
		
		// we need to remove spaces ??
		List<String> environment = Arrays.asList(properties.getProperty("idm.environments", "default, sample").split(", "));
		
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
				if (user.getMatcherAttr() == all_organisations_placeholder || g.getMatcherAttr().trim().toLowerCase().equals(user.getMatcherAttr().trim().toLowerCase())) {
					log.trace("Matched" + g.getMatcherAttr());
					table.addItem(new Object[] { g.getCn(), g.getUniqueMember().size(), g.getDn() }, counter );
					table.setParent(counter, x);
					table.setChildrenAllowed(counter, false);
					log.trace(x + ":" + counter);
					counter++;
				}
			}			
			x++;
		}
	
		treeTable.setVisibleColumns(new Object[] { "group", "user-count", "iz-action"});		
		treeTable.setColumnHeaders(new String[] { "Group", "User Count", "Action"});
		treeTable.setColumnWidth("iz-action", 50);
		treeTable.setColumnAlignment("iz-action", com.vaadin.ui.Table.Align.RIGHT);
		
	}
	
	private void updateTable(FilterTable table) {
		table.removeAllItems();		
		
		//log.error(user.getMatcherAttr());
		final String approver_group = properties.getProperty("idm.approver.group.dn", "brm");
		List<LDAPUser> approvers = user.getMatcherAttr() == all_organisations_placeholder ? ldap.getUserByGroup(approver_group) : ldap.getUsersByMatcherAndGroupAndNotLocked(user.getMatcherAttr(), approver_group) ; //new LinkedList<>();//Arrays.asList(new String[] { "Please select an environment" });
		Collections.sort(approvers);
		
		approvers.add(add_user_placeholder);
		
		/* 
		    private String uid;
			private String entryDN;
			private String cn;
			private String sn;
			private String email;
			private String homeDirectory;
			private String userPassword;
			private int uidNumber;
			private int gidNumber;
	    */
		table.setContainerDataSource(new BeanItemContainer<>(LDAPUser.class, approvers));
	
		table.setVisibleColumns(new Object[] { "uid", "cn", "email", "isMemberOf", "matcherAttr", "lastLogin", "iz-action"});
		
		table.setColumnHeaders(new String[] { "Username", "Name", "E-Mail", "Group", "Organisations", "Last Login", "Action"});
		table.setColumnAlignment("iz-action", Align.RIGHT);
		table.setColumnWidth("iz-select", 40);
	}

	private Window editUser(LDAPUser new_user, boolean creating) {
		Window w = new Window();
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
		l.setSpacing(false);

		if (!creating) {
			Label username = new Label(new_user.getUid());
			username.setCaption("Username");
			l.addComponent(username);
		}
		
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
		
		l.addComponent(binder.buildAndBind("First Name", "givenName"));
		l.addComponent(binder.buildAndBind("Last Name", "sn"));
		
		Label notice = new Label();
		if (creating) {
			notice.setContentMode(ContentMode.HTML);
			l.addComponent(notice);
			
		((TextField)binder.getField("sn")).addValueChangeListener(new ValueChangeListener() {

			@Override
			public void valueChange(ValueChangeEvent event) {
				List<LDAPUser> list = ldap.findUserByName((String)binder.getField("givenName").getValue(), (String)binder.getField("sn").getValue(), properties.getProperty("idm.approver.group.dn", "brm"));
				if (!list.isEmpty()) {
					Table t = new Table("", new BeanItemContainer<>(LDAPUser.class, list));
					t.addGeneratedColumn("gen-action", new Table.ColumnGenerator() {						
						@Override
						public Object generateCell(Table source, Object itemId, Object columnId) {
							HorizontalLayout actions = new HorizontalLayout();
							actions.addStyleName("table-actions");
						
							{ 
								Button	b = new Button("add organisation");
								((Button) b).setStyleName(BaseTheme.BUTTON_LINK);
								((Button) b).setData(itemId);
								((Button) b).addClickListener(new Button.ClickListener() {
									@Override
									public void buttonClick(ClickEvent event) {
										
										LDAPUser user_bean = ((LDAPUser)itemId);
										user_bean.setMatcherAttr(user_bean.getMatcherAttr().concat(",").concat(user.getMatcherAttr()));
										
										jobs.requestUpdateApprover(user.getUid(), user_bean, false);
										
										
										Notification.show("Halo Ident.", "User Account (" + user_bean.getUid() + ") Updated", Notification.Type.TRAY_NOTIFICATION);
										updateTable(table);
										w.close();
									}
								});
								actions.addComponent(b);
							}
							//actions.addComponent(HaloLabelFactory.label("sep", " / ", false));
							return actions;
						}	
					});
					t.setVisibleColumns(new Object[] { "uid", "givenName", "sn", "email", "matcherAttr", "gen-action" });
					t.setColumnHeaders(new String[] { "Username", "First Name", "Last Name", "Email", "Organisations", "Action" });
					t.setHeight("200px");
//					StringBuilder s = new StringBuilder("A BRM Account already exists with that name, do you want to add this Organisation to that account?<br>");
//					for (LDAPUser user : list) {
//						s.append(user.getCn() + " / " + user.getUid());
//						s.append("<br>");
//						
//					}
					notice.setData(t);
				
					notice.setValue("A BRM Account already exists with that name, do you want to add this Organisation to an existing account?");
					l.addComponent(t, 3);
				} else {
					notice.setValue("");
					if (notice.getData() != null) { l.removeComponent((com.vaadin.ui.Component) notice.getData()); }
				}
				
				
			}});
		}
		
		//l.addComponent(binder.buildAndBind("Employee ID", "employeeID"));
		l.addComponent(binder.buildAndBind("Email", "email"));
		//l.addComponent(binder.buildAndBind("VF Email", "intEmail"));
	//	l.addComponent(binder.buildAndBind("Phone Number", "phone"));
		l.addComponent(binder.buildAndBind("Suppliers", "matcherAttr"));
		
		
		
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
		
		actions.addComponent(new Button("Save", new Button.ClickListener() {
			private static final long serialVersionUID = -4314026625372219213L;
			@Override
			public void buttonClick(ClickEvent event) {
				try {
					binder.commit();
					
					LDAPUser user_bean = binder.getItemDataSource().getBean();
					if (creating) {
						// Should this be a scheduled job?
						log.error(user_bean.toString());
						jobs.requestNewApprover(user.getUid(), user_bean, false);
						
					} else { 
						if (jobs.requestUpdateApprover(user.getUid(), user_bean, false) ) {
							Notification.show("Halo Ident.", "Approver Account (" + user_bean.getUid() + ") Updated", Notification.Type.TRAY_NOTIFICATION);
						} else {
							Notification.show("Halo Ident.", "Approver Account (" + user_bean.getUid() + ") Update Failed!", Notification.Type.ERROR_MESSAGE);
						}
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
	
	private Window addGroup(String organisation, boolean creating_org) {
		
		Window w = new Window();
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
		l.setCaption(creating_org == true ? "Create Organisation <span></span>" : "Add Group <span></span>");
		l.setCaptionAsHtml(true);
		l.setSpacing(false);

		Group new_group = new Group().init();
		
		
		final BeanFieldGroup<Group> binder = new BeanFieldGroup<>(Group.class);
		binder.setItemDataSource(new_group);
		binder.setBuffered(true);
		
		if (!creating_org) {
			new_group.setMatcherAttr(user.getMatcherAttr());
			Label org_name = new Label(new_group.getMatcherAttr());
			org_name.setCaption("Organisation");
			l.addComponent(org_name);
		} else {
			l.addComponent(binder.buildAndBind("Org Name", "matcherAttr"));
			l.addComponent(new Label(" "));
			l.addComponent(new Label("You must add a Group to create an Organisation"));
		}
		
		List<String> environment = Arrays.asList(properties.getProperty("idm.environments", "default, sample").split(", "));
		final BeanItemContainer<String> environment_container = new BeanItemContainer<>(String.class, environment);
		final ComboBox combobox = new ComboBox("Environment", environment_container);
		
		combobox.setImmediate(true);
		combobox.setNewItemsAllowed(false);
		combobox.setWidth("16em");
		combobox.setNullSelectionAllowed(false);
		combobox.setValue(environment.get(0));
		//combobox.setNullSelectionItemId(new String("Please select an Environment"));
		l.addComponent(combobox);
		l.addComponent(binder.buildAndBind("Group Name", "cn"));
		
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
		
		actions.addComponent(new Button("Save", new Button.ClickListener() {
			private static final long serialVersionUID = -4314026625372219213L;
			@Override
			public void buttonClick(ClickEvent event) {
				try {
					binder.commit();
					
					Group group_bean = binder.getItemDataSource().getBean();
					if (jobs.addGroup((String)combobox.getValue(), group_bean)) {
						
						Notification.show("Halo Ident.", (creating_org ? "Organisation (" + group_bean.getMatcherAttr() : "Group (" + group_bean.getCn()) + ") Created", Notification.Type.TRAY_NOTIFICATION);
						w.setData(creating_org ?  group_bean.getMatcherAttr() : null);
					} else {
						Notification.show("Halo Ident.", "Failed to Create "  + (creating_org ? "Organisation (" + group_bean.getMatcherAttr() : "Group (" + group_bean.getCn()) + ")!", Notification.Type.ERROR_MESSAGE);
					}
					
					
					
				} catch (CommitException e) {
					// TODO Auto-generated catch block
					e.printStackTrace();
				}
				w.close();
			}
			}));
		l.addComponent(actions);
		layout.addComponent(l);
		return w;
	}
	
	@Override
	public void enter(ViewChangeEvent event) {
		user = ldap.getUser(((HaloUI)UI.getCurrent()).getUsername() );
		build();		
	}

}
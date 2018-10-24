package com.collectivesystems.idm.ui.views;

import java.text.SimpleDateFormat;
import java.time.Instant;
import java.time.LocalDate;
import java.time.ZoneId;
import java.time.temporal.ChronoUnit;
import java.util.Date;
import java.util.LinkedList;
import java.util.List;

import javax.annotation.PostConstruct;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Qualifier;
import org.springframework.context.annotation.Scope;
import org.springframework.context.support.ReloadableResourceBundleMessageSource;
import org.springframework.stereotype.Component;
import org.tepi.filtertable.FilterTable;
import org.vaadin.hene.flexibleoptiongroup.FlexibleOptionGroup;

import com.collectivesystems.core.annotations.HaloAuthority;
import com.collectivesystems.core.dao.CSDAO;
import com.collectivesystems.core.factory.HaloFactory;
import com.collectivesystems.core.services.service.PropertiesService;
import com.collectivesystems.idm.beans.LDAPUser;
import com.collectivesystems.idm.beans.UserRequest;
import com.collectivesystems.idm.beans.UserRequestArchive;
import com.collectivesystems.idm.beans.UserRequestBase;
import com.collectivesystems.idm.services.service.LDAPService;
import com.collectivesystems.idm.ui.IDMPrivateUI;
import com.vaadin.data.Property;
import com.vaadin.data.Property.ValueChangeEvent;
import com.vaadin.data.Property.ValueChangeListener;
import com.vaadin.data.util.BeanItemContainer;
import com.vaadin.data.util.ObjectProperty;
import com.vaadin.navigator.View;
import com.vaadin.navigator.ViewChangeListener.ViewChangeEvent;
import com.vaadin.shared.ui.label.ContentMode;
import com.vaadin.ui.Button;
import com.vaadin.ui.Button.ClickEvent;
import com.vaadin.ui.CheckBox;
import com.vaadin.ui.CssLayout;
import com.vaadin.ui.CustomTable;
import com.vaadin.ui.HorizontalLayout;
import com.vaadin.ui.Label;
import com.vaadin.ui.Layout;
import com.vaadin.ui.Notification;
import com.vaadin.ui.TabSheet;
import com.vaadin.ui.TabSheet.SelectedTabChangeEvent;
import com.vaadin.ui.UI;
import com.vaadin.ui.VerticalLayout;
import com.vaadin.ui.themes.BaseTheme;

import de.steinwedel.messagebox.MessageBox;
import ru.xpoft.vaadin.VaadinView;

@Component
@Scope("prototype")
@VaadinView(ActivityMgr.NAME)
@SuppressWarnings("serial")
@HaloAuthority(authority = "ROLE_IDM_ADMIN")
public class ActivityMgr extends CssLayout implements View {
	private static Logger log = LoggerFactory.getLogger(ActivityMgr.class);
	final static SimpleDateFormat df = new SimpleDateFormat("dd-MM-yyyy");
	final static SimpleDateFormat df_long = new SimpleDateFormat("dd MMM yyyy");
	public static final String NAME = "zActivity";

	@SuppressWarnings("unused")
	@Autowired
	private CSDAO dao;

	@Autowired
	protected PropertiesService properties;
	
	@Autowired
	@Qualifier("haloMessageSource")
	ReloadableResourceBundleMessageSource messages;
	
	@Autowired
	LDAPService ldap;

	Label stats = new Label();
    FilterTable table;
    boolean show_rw_resets = false;
    boolean show_archived_requests  = false;
    Label crlabel = new Label();
    
    @PostConstruct
	public void PostConstruct() {
		if (properties.getProperty("push.enabled", "false").equals("false")) {
			UI.getCurrent().setPollInterval(10000);
		}
		setSizeFull();
		addStyleName("idm-requester-view");
	}
		
	protected void build() {
		
		// Tab sheet
		TabSheet tabsheet = new TabSheet();
		

		// Create the first tab
		VerticalLayout tab1 = new VerticalLayout();
		
		

		// This tab gets its caption from the component caption
		VerticalLayout tab2 = new VerticalLayout();
		tab1.setCaption("Last 60 Days");
		tab2.setCaption("Archive");
		tabsheet.addTab(tab1);
		tabsheet.addTab(tab2);
		
		tabsheet.addSelectedTabChangeListener(new TabSheet.SelectedTabChangeListener() {
			public void selectedTabChange(SelectedTabChangeEvent event) {
				// Find the tabsheet
				TabSheet tabsheet = event.getTabSheet();

				// Find the tab (here we know it's a layout)
				Layout tab = (Layout) tabsheet.getSelectedTab();

				// Get the tab caption from the tab object
				String caption = tabsheet.getTab(tab).getCaption();

				// Fill the tab content
				if (caption.equalsIgnoreCase("Archive")) {
					show_archived_requests = true;
				} else {
					show_archived_requests = false;
				}
				
				updateTable(table);
			}
		});

	// end tab sheet
		
		
		VerticalLayout izWrapper = new VerticalLayout();
		izWrapper.setSizeFull();
		izWrapper.setStyleName("iz-wrapper");
		izWrapper.setSpacing(false);

	
		crlabel.setValue("All Requests");
		crlabel.setStyleName("status-panel");
		crlabel.setContentMode(ContentMode.HTML); 
		
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
		
//		Button export = new Button("refresh");
//		((Button) export).setStyleName(BaseTheme.BUTTON_LINK);
//		 export.addClickListener(new Button.ClickListener() {				
//				@Override
//				public void buttonClick(ClickEvent event) { 
//					//HaloTableHolder h = new HaloTableHolder(table);
//					ExcelExport excelExport = new ExcelExport(table, "Activity");			
//	                excelExport.excludeCollapsedColumns();	               
//	                excelExport.setUseTableFormatPropertyValue(false);     
//	               // excelExport.setExportFileName((String)filterby.getValue() + ".xls");
//	                excelExport.export();			
//				}
//
//				
//		    });	
		
		Button b = new Button("refresh");
		((Button) b).setStyleName(BaseTheme.BUTTON_LINK);
		((Button) b).addClickListener(new Button.ClickListener() {
			@Override
			public void buttonClick(ClickEvent event) {
				updateTable(table);
			}
		});

		infoBar2.addComponent(b);
		
		Button pwreset_toggle = new Button("show pw resets");
		pwreset_toggle.setStyleName(BaseTheme.BUTTON_LINK);
		pwreset_toggle.addClickListener(new Button.ClickListener() {
			

			@Override
			public void buttonClick(ClickEvent event) {
				show_rw_resets = !show_rw_resets;
				if (show_rw_resets) { pwreset_toggle.setCaption("hide pw resets"); }
				else { pwreset_toggle.setCaption("show pw resets"); }
				updateTable(table);
			}
		});

		infoBar2.addComponent(pwreset_toggle);
		
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
		
		List<CheckBox> cbs = new LinkedList<>();
		final Boolean[] updating = { false };
		final FlexibleOptionGroup flexibleOptionGroup = new FlexibleOptionGroup() {
			public void setImmediate(boolean immediate) {
				super.setImmediate(immediate);
				table.setImmediate(true);
			}

			public void setMultiSelect(boolean multiSelect) {
				super.setMultiSelect(multiSelect);
				table.setMultiSelect(multiSelect);
			}

			public void setEnabled(boolean enabled) {
				super.setEnabled(enabled);
				table.setEnabled(enabled);
			}

			public void setReadOnly(boolean readOnly) {
				super.setReadOnly(readOnly);
				table.setReadOnly(readOnly);
			}
		};
		
		flexibleOptionGroup.addValueChangeListener(new ValueChangeListener() {
			@Override
			public void valueChange(ValueChangeEvent event) {
				table.setValue(flexibleOptionGroup.getValue());
				
			}});
		//flexibleOptionGroup.setItemCaptionPropertyId(CAPTION_PROPERTY);
		//flexibleOptionGroup.setItemIconPropertyId(ICON_PROPERTY);

		flexibleOptionGroup.setImmediate(true);
		flexibleOptionGroup.setPropertyDataSource(new ObjectProperty<Object>(null, Object.class));

		
		table.addGeneratedColumn("iz-select", new FilterTable.ColumnGenerator() {
			@Override
			public com.vaadin.ui.Component generateCell(CustomTable source, final Object itemId, Object columnId) {
				flexibleOptionGroup.getContainerDataSource().addItem(itemId);
				return flexibleOptionGroup.getItemComponent(itemId);
			}
		});
		
		table.addGeneratedColumn("iz-action", new FilterTable.ColumnGenerator() {
			@Override
			public com.vaadin.ui.Component generateCell(CustomTable source, final Object itemId, Object columnId) {
				
				HorizontalLayout actions = new HorizontalLayout();
				
//				 { 
//					Button	b = new Button("notify");
//					((Button) b).setStyleName(BaseTheme.BUTTON_LINK);
//					((Button) b).setData(itemId);
//					((Button) b).addClickListener(new Button.ClickListener() {
//						@Override
//						public void buttonClick(ClickEvent event) {
//							
//							UserRequestBase ur = (UserRequestBase)event.getButton().getData();
//							ur.setStatus(UserRequest.STATUS_CREATED);
//							dao.save(ur);
//								
//							updateTable(table);
//							
//							Notification.show("Halo Ident.", "Nofitifcation Emails will be resent", Notification.Type.TRAY_NOTIFICATION);
//						}
//					});
//					actions.addComponent(b);
//				 }
				// actions.addComponent(HaloFactory.label("sep", "/", false));
				 { 
						Button	b = new Button("delete");
						((Button) b).setStyleName(BaseTheme.BUTTON_LINK);
						((Button) b).setData(itemId);
						((Button) b).addClickListener(new Button.ClickListener() {
							@Override
							public void buttonClick(ClickEvent event) {
								MessageBox.createQuestion().withCaption("Requester").withMessage("Are you sure you want to delete this request?")
							    .withNoButton()
							    .withYesButton(new Runnable() {

									@Override
									public void run() {
										UserRequestBase ur = (UserRequestBase)event.getButton().getData();
										dao.delete(ur);
										
										updateTable(table);

										Notification.show("Halo Ident.", "User Request Deleted", Notification.Type.TRAY_NOTIFICATION);
										
									}}).open();
							}
						});
						actions.addComponent(b);
				   }
//				 if (((UserRequestBase)itemId).getStatus() == UserRequest.STATUS_NOTIFIED) {
//					 actions.addComponent(HaloFactory.label("sep", "/", false));
//					 { 
//						Button	b = new Button("pwd-reset");
//						((Button) b).setStyleName(BaseTheme.BUTTON_LINK);
//						((Button) b).setData(itemId);
//						((Button) b).addClickListener(new Button.ClickListener() {
//							@Override
//							public void buttonClick(ClickEvent event) {
//								MessageBox.createQuestion().withCaption("Requester").withMessage("Are you sure you want to reset the paswword for this user?")
//							    .withNoButton()
//							    .withYesButton(new Runnable() {
//
//									@Override
//									public void run() {
//										UserRequestBase ur = (UserRequestBase)event.getButton().getData();
//										LDAPUser user = ldap.getUser(ur.getUsername());
//										String password = ldap.generatePassword();
//										user.setUserPassword(password);
//										
//
//										Notification.show("Halo Ident.", "Account Password Reset", Notification.Type.TRAY_NOTIFICATION);
//										
//									}}).open();
//							}
//						});
//						actions.addComponent(b);
//					 }
//				   }
				return actions;
			}
		});
		
		table.addGeneratedColumn("iz-status", new FilterTable.ColumnGenerator() {
			@Override
			public com.vaadin.ui.Component generateCell(CustomTable source, final Object itemId, Object columnId) {
				Label label = new Label();
				label.setValue( ((UserRequestBase) itemId).getStatus() > -1 ? UserRequest.STATUS_NAMES[((UserRequestBase) itemId).getStatus()] : "");
				return label;
			}
		});
		
		table.addGeneratedColumn("req-action", new FilterTable.ColumnGenerator() {
			@Override
			public com.vaadin.ui.Component generateCell(CustomTable source, final Object itemId, Object columnId) {
				Label label = new Label();
				label.setValue( ((UserRequestBase) itemId).getAction() > -1 ? UserRequest.ACTION_NAMES[((UserRequestBase) itemId).getAction()] : "");
				return label;
			}
		});
		
		
		izWrapper.addComponent(crlabel);
		izWrapper.addComponent(infoBar2);
		izWrapper.addComponent(table);
		izWrapper.setExpandRatio(table,  1);
		
		addComponent(tabsheet);
		addComponent(izWrapper);
		
		updateTable(table);

	}
	
	private void updateTable(FilterTable table) {
		List<UserRequestBase> list = new LinkedList<>();
		if (show_archived_requests) {
			crlabel.setValue("Archived Requests");
			
			LocalDate ld = LocalDate.now().minus(999, ChronoUnit.DAYS);		
			Instant instant = ld.atStartOfDay().atZone(ZoneId.systemDefault()).toInstant();
			Date res = Date.from(instant);
			if (show_rw_resets) {
				list.addAll(UserRequestArchive.getArchiveEntriesByDate(res));
			} else {
				list.addAll(UserRequestArchive.getArchiveEntriesByDateIgnore(res, "User Password Reset Request"));
			}
		} else {
			crlabel.setValue("Last 60 Days");
			LocalDate ld = LocalDate.now().minus(Integer.parseInt(properties.getProperty("idm.activity.history.days", "7")), ChronoUnit.DAYS);		
			Instant instant = ld.atStartOfDay().atZone(ZoneId.systemDefault()).toInstant();
			Date res = Date.from(instant);
			
			if (show_rw_resets) {
				list.addAll(UserRequest.getEntriesByDate(res));
			} else {
				list.addAll(UserRequest.getEntriesByDateIgnore(res, "User Password Reset Request"));
			}
		}
		
		/*
		 *  private String fname;
			private String sname;
			private String employeeID;
			private String exEmail;
			private String intEmail;
			private String phone;
			private String group;
			private String ntlogin;
			private String environment;
			private Date startDate;
			private Date endDate;
		 */
		table.setContainerDataSource(new BeanItemContainer<>(UserRequestBase.class, list));
		table.setVisibleColumns(new Object[] { "id", "created", "updated", "requester", "approver", "username", "exEmail", "fullname", "req-action", "iz-status", "organisation", "ggroup", "environment", "startDate", "endDate", "msg", "iz-action" });
		table.setColumnHeaders(new String[] { "ID","Requested", "Last Action", "Requester", "BRM", "Username", "Email", "Name", "Request Action", "Status", "Organisation", "Group", "Environment", "Start Date", "End Date", "Message", "Action" });
		table.setColumnWidth("iz-select", 40);
		((IDMPrivateUI)UI.getCurrent()).updateBadges();
	}

	@Override
	public void enter(ViewChangeEvent event) {
		build();		
	}

	
	
}
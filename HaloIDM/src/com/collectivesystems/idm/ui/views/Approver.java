package com.collectivesystems.idm.ui.views;

import java.text.SimpleDateFormat;
import java.util.Date;
import java.util.HashMap;
import java.util.LinkedList;
import java.util.List;
import java.util.Locale;
import java.util.Map;

import javax.annotation.PostConstruct;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Qualifier;
import org.springframework.context.annotation.Scope;
import org.springframework.context.support.ReloadableResourceBundleMessageSource;
import org.springframework.stereotype.Component;
import org.vaadin.hene.flexibleoptiongroup.FlexibleOptionGroup;

import com.collectivesystems.core.annotations.HaloAuthority;
import com.collectivesystems.core.dao.CSDAO;
import com.collectivesystems.core.factory.HaloFactory;
import com.collectivesystems.core.services.service.PropertiesService;
import com.collectivesystems.core.ui.providers.HaloUI;
import com.collectivesystems.core.vaadin.BadgeView;
import com.collectivesystems.idm.beans.UserRequest;
import com.collectivesystems.idm.ui.IDMPrivateUI;
import com.vaadin.data.Property;
import com.vaadin.data.Property.ValueChangeEvent;
import com.vaadin.data.Property.ValueChangeListener;
import com.vaadin.data.util.BeanItemContainer;
import com.vaadin.data.util.ObjectProperty;
import com.vaadin.navigator.ViewChangeListener.ViewChangeEvent;
import com.vaadin.shared.ui.label.ContentMode;
import com.vaadin.ui.Button;
import com.vaadin.ui.Button.ClickEvent;
import com.vaadin.ui.CheckBox;
import com.vaadin.ui.CssLayout;
import com.vaadin.ui.HorizontalLayout;
import com.vaadin.ui.Label;
import com.vaadin.ui.Notification;
import com.vaadin.ui.Table;
import com.vaadin.ui.Table.ColumnGenerator;
import com.vaadin.ui.Table.GeneratedRow;
import com.vaadin.ui.Table.RowGenerator;
import com.vaadin.ui.TreeTable;
import com.vaadin.ui.UI;
import com.vaadin.ui.VerticalLayout;
import com.vaadin.ui.themes.BaseTheme;

import de.steinwedel.messagebox.MessageBox;
import ru.xpoft.vaadin.VaadinView;

@Component
@Scope("prototype")
@VaadinView(Approver.NAME)
@SuppressWarnings("serial")
@HaloAuthority(authority = "ROLE_BRM")
public class Approver extends CssLayout implements BadgeView {
	private static Logger log = LoggerFactory.getLogger(Approver.class);
	final static SimpleDateFormat df_long = new SimpleDateFormat("dd MMM yyyy");
	public static final String NAME = "zApprovals";

	@SuppressWarnings("unused")
	@Autowired
	private CSDAO dao;

	@Autowired
	protected PropertiesService properties;
	
	@Autowired
	@Qualifier("haloMessageSource")
	ReloadableResourceBundleMessageSource messages;

	Label stats = new Label();
    TreeTable table;

    @PostConstruct
	public void PostConstruct() {
		if (properties.getProperty("push.enabled", "false").equals("false")) {
			UI.getCurrent().setPollInterval(10000);
		}
		setSizeFull();
		addStyleName("idm-requester-view");
	}
		
	protected void build() {
		
		
		
		VerticalLayout izWrapper = new VerticalLayout();
		izWrapper.setSizeFull();
		izWrapper.setStyleName("iz-wrapper");
		izWrapper.setSpacing(false);

		Label crlabel = new Label();
		crlabel.setValue("Pending Requests (" + ((HaloUI)UI.getCurrent()).getUsername() + ") <span> - " + messages.getMessage("idm.approvaltable.hint", null, Locale.UK) + "</span>");
		crlabel.setStyleName("status-panel");
		crlabel.setContentMode(ContentMode.HTML); 
		
		HorizontalLayout infoBar = new HorizontalLayout();
		infoBar.setSizeUndefined();
		infoBar.setStyleName("layout-info-bar");
		infoBar.setSpacing(true);
		
		Button b = new Button("refresh");
		((Button) b).setStyleName(BaseTheme.BUTTON_LINK);
		((Button) b).addClickListener(new Button.ClickListener() {
			@Override
			public void buttonClick(ClickEvent event) {
				updateTable(table);
			}
		});	
		
//		Button filter = new Button();
//		filter.setWidth("26px");
//		filter.setHeight("26px");
//		filter.setIcon(new ThemeResource("icons/flask.svg"));
//		((Button) filter).setStyleName(BaseTheme.BUTTON_LINK);
//		((Button) filter).addClickListener(new Button.ClickListener() {
//			@Override
//			public void buttonClick(ClickEvent event) {
//				table.setFilterBarVisible(!table.isFilterBarVisible());
//				//event.getButton().setCaption(table.isFilterBarVisible() ? "filter off" : "filter on");
//				if (!table.isFilterBarVisible()) { table.clearFilters(); }
//			}
//		});
//		infoBar.addComponent(filter);
		infoBar.addComponent(b);
		
		table = new TreeTable() {
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
		table.setSelectable(false);
		//table.setFilterBarVisible(false);
		
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

		
		table.addGeneratedColumn("iz-select", new ColumnGenerator() {
			@Override
			public com.vaadin.ui.Component generateCell(Table source, final Object itemId, Object columnId) {
				flexibleOptionGroup.getContainerDataSource().addItem(itemId);
				return flexibleOptionGroup.getItemComponent(itemId);
			}
		});
		
		
		table.addGeneratedColumn("iz-action", new ColumnGenerator() {
			@Override
			public com.vaadin.ui.Component generateCell(Table source, final Object itemId, Object columnId) {
				
				HorizontalLayout actions = new HorizontalLayout();
				
				 { 
					Button	b = new Button("approve");
					((Button) b).setStyleName(BaseTheme.BUTTON_LINK);
					((Button) b).setData(itemId);
					((Button) b).addClickListener(new Button.ClickListener() {
						@Override
						public void buttonClick(ClickEvent event) {
							
							UserRequest ur = (UserRequest)event.getButton().getData();
							ur.setStatus(UserRequest.STATUS_APPROVED);
							ur.setUpdated(new Date());
							dao.save(ur);
								
							updateTable(table);
							Notification.show("Halo Ident.", "Request " + ur.getId() + " Approved", Notification.Type.TRAY_NOTIFICATION);
						}
					});
					actions.addComponent(b);
				 }
				 actions.addComponent(HaloFactory.label("sep", "or", false));
				 { 
						Button	b = new Button("reject");
						((Button) b).setStyleName(BaseTheme.BUTTON_LINK);
						((Button) b).setData(itemId);
						((Button) b).addClickListener(new Button.ClickListener() {
							@Override
							public void buttonClick(ClickEvent event) {
								
								MessageBox.createQuestion().withCaption("Approver").withMessage("Are you sure you want to reject this request?")
							    .withNoButton()
							    .withYesButton(new Runnable() {

									@Override
									public void run() {
										
										log.error("Yes");
										UserRequest ur = (UserRequest)event.getButton().getData();
										ur.setStatus(UserRequest.STATUS_REJECTED);
										ur.setUpdated(new Date());
										dao.save(ur);
											
										updateTable(table);

										Notification.show("Halo Ident.", "Request " + ur.getId() + " Rejected", Notification.Type.TRAY_NOTIFICATION);
										
									}}).open();
								
								
							}
						});
						actions.addComponent(b);
				}
				return actions;
			}
		});
		
		table.addGeneratedColumn("iz-status", new ColumnGenerator() {
			@Override
			public com.vaadin.ui.Component generateCell(Table source, final Object itemId, Object columnId) {
				Label label = new Label();
				label.setValue( ((UserRequest) itemId).getStatus() > -1 ? UserRequest.STATUS_NAMES[((UserRequest) itemId).getStatus()] : "");
				return label;
			}
		});
		
		table.addGeneratedColumn("req-action", new ColumnGenerator() {
			@Override
			public com.vaadin.ui.Component generateCell(Table source, final Object itemId, Object columnId) {
				Label label = new Label();
				label.setValue( ((UserRequest) itemId).getAction() > -1 ? UserRequest.ACTION_NAMES[((UserRequest) itemId).getAction()] : "");
				return label;
			}
		});
		
		
		
		izWrapper.addComponent(crlabel);
		izWrapper.addComponent(infoBar);
		izWrapper.addComponent(table);
		izWrapper.setExpandRatio(table,  1);
		
		addComponent(izWrapper);
		
		updateTable(table);

	}
	
	private void updateTable(TreeTable table) {
//		LocalDate ld = LocalDate.now().minus(60, ChronoUnit.DAYS);		
//		Instant instant = ld.atStartOfDay().atZone(ZoneId.systemDefault()).toInstant();
//		Date res = Date.from(instant);
		List<UserRequest> list = UserRequest.getEntriesByStatusAndApprover(UserRequest.STATUS_PENDING_APPROVAL, ((HaloUI)UI.getCurrent()).getUsername());
		table.setContainerDataSource(new BeanItemContainer<>(UserRequest.class, new LinkedList<UserRequest>()));
		//
		Map<String, List<UserRequest>> map = new HashMap<>();
		for (UserRequest ur : list) {
			String justification = ur.getBusinessJustification() == null || ur.getBusinessJustification().trim().isEmpty() ? "Not Specified" : ur.getBusinessJustification();
		//	log.error(justification);
			List<UserRequest> l = map.get(justification);
			if (l == null) { l = new LinkedList<>(); map.put(justification, l); }
			l.add(ur);
		}
		int counter = 0;
		for (String key : map.keySet()) {
			List<UserRequest> l = map.get(key);
			UserRequest justification_ur = new UserRequest().init();
			justification_ur.setId(counter--);
			justification_ur.setBusinessJustification(key);
			table.addItem(justification_ur);
			table.setCollapsed(justification_ur, false);
			for (UserRequest ur : l) {
				table.addItem(ur);
				table.setParent(ur, justification_ur);
				table.setChildrenAllowed(ur, false);
				
				
			
			}
		}
//				new Table().setRowHeaderMode(RowHeaderMode.ITEM);
//				total_requests = 0l;
//				for (FilePointer fp : list) {
//					total_space += fp.getFilesize();
//				}
//				izUsedSizeLabel.setValue(Utils.humanReadableByteCount(total_space, true));
				
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
			//	table.setContainerDataSource(new BeanItemContainer<>(UserRequest.class, list));
				//table.setRowHeaderMode(RowHeaderMode.ITEM);
		
		table.setCellStyleGenerator(new Table.CellStyleGenerator() {
			@Override
			public String getStyle(Table source, Object itemId, Object propertyId) {
				if (propertyId != null) { 
					if ( ((UserRequest)itemId).getId() <= 0 && ((String)propertyId).equals("id")) { // generated row
						return "style-me-out";
					}
				}
				return null;
			}
        });
			
		table.setRowGenerator(new RowGenerator() {
			@Override
			public TreeTable.GeneratedRow generateRow(Table table, Object itemId) {
				if (((UserRequest)itemId).getId() <= 0) {
					GeneratedRow g = new GeneratedRow("<b>Business Justification: </b><span class=\"business-justification-row-header\">" + ((UserRequest)itemId).getBusinessJustification()+ "</span>") {
//						public Object getValue() {
//							log.error("ewfergergerwgrewqgrewgerwgrewkjngierngierjngoerwngoerngewrognerognerignwerirgn");
//							return new HorizontalLayout(); //Label("This is a test bitch");
//							
//						}
						
					};					
					g.setSpanColumns(false);
					g.setHtmlContentAllowed(true);
					
					return g;
				}
				return null;
			}
			
			
			
		});
			
		
		table.setVisibleColumns(new Object[] { "id", "created", "requester", "fullname", "req-action", "iz-status", "organisation", "ggroup", "environment", "startDate", "endDate", "iz-action" });
		table.setColumnHeaders(new String[] { "ID","Requested", "Requester", "Name", "Requested Action", "Status", "Organisation", "Group", "Environment", "Start Date", "End Date", "Action" });
		table.setColumnWidth("id", 100);
		((IDMPrivateUI)UI.getCurrent()).updateBadges();
	}

	@Override
	public void enter(ViewChangeEvent event) {
		build();		
	}

	@Override
	public boolean hasBadge() {
		return true;
	}

	@Override
	public int badgeNumber() {
		return UserRequest.getEntriesByStatusAndApprover(UserRequest.STATUS_PENDING_APPROVAL, ((HaloUI)UI.getCurrent()).getUsername()).size();
	}

	public static int badgeNumberStatic() {
		return UserRequest.getEntriesByStatusAndApprover(UserRequest.STATUS_PENDING_APPROVAL, ((HaloUI)UI.getCurrent()).getUsername()).size();
	}
	
}
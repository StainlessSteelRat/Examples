package com.collectivesystems.idm.ui.views;

import java.text.SimpleDateFormat;
import java.time.Instant;
import java.time.LocalDate;
import java.time.ZoneId;
import java.time.temporal.ChronoUnit;
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
import com.collectivesystems.core.services.service.PropertiesService;
import com.collectivesystems.core.ui.providers.HaloUI;
import com.collectivesystems.idm.beans.UserRequest;
import com.collectivesystems.idm.beans.UserRequestArchive;
import com.collectivesystems.idm.beans.UserRequestBase;
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
import com.vaadin.ui.HorizontalLayout;
import com.vaadin.ui.Label;
import com.vaadin.ui.Table;
import com.vaadin.ui.Table.ColumnGenerator;
import com.vaadin.ui.Table.GeneratedRow;
import com.vaadin.ui.Table.RowGenerator;
import com.vaadin.ui.TreeTable;
import com.vaadin.ui.UI;
import com.vaadin.ui.VerticalLayout;
import com.vaadin.ui.themes.BaseTheme;

import ru.xpoft.vaadin.VaadinView;

@Component
@Scope("prototype")
@VaadinView(ApproverHistory.NAME)
@SuppressWarnings("serial")
@HaloAuthority(authority = "ROLE_BRM")
public class ApproverHistory extends CssLayout implements View {
	private static Logger log = LoggerFactory.getLogger(ApproverHistory.class);
	final static SimpleDateFormat df_long = new SimpleDateFormat("dd MMM yyyy");
	public static final String NAME = "zHistory";

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
		crlabel.setValue("Request History (" + ((HaloUI)UI.getCurrent()).getUsername() + ") <span> - " + messages.getMessage("idm.historytable.hint", null, Locale.UK) + "</span>");
		crlabel.setStyleName("status-panel");
		crlabel.setContentMode(ContentMode.HTML); 
		
		HorizontalLayout infoBar2 = new HorizontalLayout();
		infoBar2.setSizeUndefined();
		infoBar2.setStyleName("layout-info-bar");
		infoBar2.setSpacing(true);
		
//		Button filter = new Button("filter on");
//		((Button) filter).setStyleName(BaseTheme.BUTTON_LINK);
//		((Button) filter).addClickListener(new Button.ClickListener() {
//			@Override
//			public void buttonClick(ClickEvent event) {
//				table.setFilterBarVisible(!table.isFilterBarVisible());
//				event.getButton().setCaption(table.isFilterBarVisible() ? "filter off" : "filter on");
//				if (!table.isFilterBarVisible()) { table.clearFilters(); }
//			}
//		});
//		infoBar2.addComponent(filter);
		
		Button b = new Button("refresh");
		((Button) b).setStyleName(BaseTheme.BUTTON_LINK);
		((Button) b).addClickListener(new Button.ClickListener() {
			@Override
			public void buttonClick(ClickEvent event) {
				updateTable(table);
			}
		});
		infoBar2.addComponent(b);
		
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
		
		
//		table.addGeneratedColumn("iz-action", new FilterTable.ColumnGenerator() {
//			@Override
//			public com.vaadin.ui.Component generateCell(CustomTable source, final Object itemId, Object columnId) {
//				
//				HorizontalLayout actions = new HorizontalLayout();
//				
//				 if (((UserRequest)itemId).getStatus() != UserRequest.STATUS_TERMINATED) { 
//					Button	b = new Button("suspend");
//					((Button) b).setStyleName(BaseTheme.BUTTON_LINK);
//					((Button) b).setData(itemId);
//					((Button) b).addClickListener(new Button.ClickListener() {
//						@Override
//						public void buttonClick(ClickEvent event) {
//							
//							UserRequest ur = (UserRequest)event.getButton().getData();
//							ur.setStatus(UserRequest.STATUS_TERMINATED);
//							dao.save(ur);
//								
//							updateTable(table);
//							Notification.show("Halo Ident.", "User Account (" + ((UserRequest)itemId).getUsername() + ") Suspened", Notification.Type.TRAY_NOTIFICATION);
//						}
//					});
//					actions.addComponent(b);
//				 } else { 
//						Button	b = new Button("resume");
//						((Button) b).setStyleName(BaseTheme.BUTTON_LINK);
//						((Button) b).setData(itemId);
//						((Button) b).addClickListener(new Button.ClickListener() {
//							@Override
//							public void buttonClick(ClickEvent event) {
//								
//								UserRequest ur = (UserRequest)event.getButton().getData();
//								ur.setStatus(UserRequest.STATUS_NOTIFIED);
//								dao.save(ur);
//									
//								updateTable(table);
//								Notification.show("Halo Ident.", "User Account (" + ((UserRequest)itemId).getUsername() + ") Resumed", Notification.Type.TRAY_NOTIFICATION);
//							}
//						});
//						actions.addComponent(b);
//				}
//				return actions;
//			}
//		});
		
		table.addGeneratedColumn("iz-status", new ColumnGenerator() {
			@Override
			public com.vaadin.ui.Component generateCell(Table source, final Object itemId, Object columnId) {
				Label label = new Label();
				label.setValue( ((UserRequest) itemId).getStatus() > -1 ? UserRequest.STATUS_NAMES[((UserRequest) itemId).getStatus()] : "");
				return label;
			}
		});
		
		table.addGeneratedColumn("gen-action", new ColumnGenerator() {
			@Override
			public com.vaadin.ui.Component generateCell(Table source, final Object itemId, Object columnId) {
				Label label = new Label();
				label.setValue( ((UserRequest) itemId).getAction() > -1 ? UserRequest.ACTION_NAMES[((UserRequest) itemId).getAction()] : "");
				return label;
			}
		});
		
		
		izWrapper.addComponent(crlabel);
		izWrapper.addComponent(infoBar2);
		izWrapper.addComponent(table);
		izWrapper.setExpandRatio(table,  1);
		
		addComponent(izWrapper);
		
		updateTable(table);

	}
	
	private void updateTable(TreeTable table) {
    	LocalDate ld = LocalDate.now().minus(60, ChronoUnit.DAYS);		
		Instant instant = ld.atStartOfDay().atZone(ZoneId.systemDefault()).toInstant();
		Date res = Date.from(instant);
		List<UserRequestBase> list = new LinkedList<>();
		list.addAll(UserRequest.getEntriesByApprover(((HaloUI)UI.getCurrent()).getUsername()));
		list.addAll(UserRequestArchive.getArchiveEntriesByApprover(((HaloUI)UI.getCurrent()).getUsername()));

		table.setContainerDataSource(new BeanItemContainer<>(UserRequest.class, new LinkedList<UserRequest>()));
		//
		Map<String, List<UserRequestBase>> map = new HashMap<>();
		for (UserRequestBase ur : list) {
			String justification = ur.getBusinessJustification() == null || ur.getBusinessJustification().trim().isEmpty() ? "Not Specified" : ur.getBusinessJustification();
		//	log.error(justification);
			List<UserRequestBase> l = map.get(justification);
			if (l == null) { l = new LinkedList<>(); map.put(justification, l); }
			l.add(ur);
		}
		int counter = 0;
		for (String key : map.keySet()) {
			List<UserRequestBase> l = map.get(key);
			UserRequest justification_ur = new UserRequest().init();
			justification_ur.setId(counter--);
			justification_ur.setBusinessJustification(key);
			table.addItem(justification_ur);
			table.setCollapsed(justification_ur, false);
		
			for (UserRequestBase ur : l) {
				table.addItem(ur);
				table.setParent(ur, justification_ur);
				table.setChildrenAllowed(ur, false);
				log.error(ur.getId() + "  " + justification_ur.getId() + " [" + ur.getBusinessJustification() + "]");
				
			
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
			public GeneratedRow generateRow(Table table, Object itemId) {
				if (((UserRequest)itemId).getId() <= 0) {
					GeneratedRow g = new GeneratedRow("<b>Business Justification: </b><span class=\"business-justification-row-header\">" + ((UserRequest)itemId).getBusinessJustification()+ "</span>");					
					g.setSpanColumns(false);
					g.setHtmlContentAllowed(true);
					return g;
				}
				return null;
			}} );
				
		//table.setContainerDataSource(new BeanItemContainer<>(UserRequest.class, list));
		table.setVisibleColumns(new Object[] { "id", "created", "requester", "fullname", "gen-action", "iz-status", "organisation", "ggroup", "environment", "startDate", "endDate", "msg" });
		table.setColumnHeaders(new String[] { "ID","Requested", "Requester", "Name", "Requested Action", "Status", "Organisation", "Group", "Environment", "Start Date", "End Date", "Message" });
		table.setColumnWidth("id", 100);
		((IDMPrivateUI)UI.getCurrent()).updateBadges();
	}

	@Override
	public void enter(ViewChangeEvent event) {
		build();		
	}

	
	
}
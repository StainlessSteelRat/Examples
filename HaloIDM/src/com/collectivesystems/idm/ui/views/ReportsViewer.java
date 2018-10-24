package com.collectivesystems.idm.ui.views;

import java.io.File;
import java.io.FileInputStream;
import java.io.IOException;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collections;
import java.util.LinkedList;
import java.util.List;
import java.util.Properties;

import javax.annotation.PostConstruct;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.context.annotation.Scope;
import org.springframework.stereotype.Component;

import com.collectivesystems.core.annotations.HaloAuthority;
import com.collectivesystems.core.dao.CSDAO;
import com.collectivesystems.core.services.service.PropertiesService;
import com.collectivesystems.core.services.service.SpringHelperService;
import com.collectivesystems.core.ui.providers.HaloUI;
import com.collectivesystems.idm.beans.Group;
import com.collectivesystems.idm.beans.LDAPUser;
import com.collectivesystems.idm.services.service.LDAPService;
import com.vaadin.addon.tableexport.ExcelExport;
import com.vaadin.addon.tableexport.ExportableColumnGenerator;
import com.vaadin.data.Property;
import com.vaadin.data.Property.ValueChangeEvent;
import com.vaadin.data.Property.ValueChangeListener;
import com.vaadin.data.util.BeanItemContainer;
import com.vaadin.navigator.View;
import com.vaadin.navigator.ViewChangeListener.ViewChangeEvent;
import com.vaadin.ui.Button;
import com.vaadin.ui.Button.ClickEvent;
import com.vaadin.ui.ComboBox;
import com.vaadin.ui.FormLayout;
import com.vaadin.ui.HorizontalLayout;
import com.vaadin.ui.Label;
import com.vaadin.ui.Notification;
import com.vaadin.ui.Table;
import com.vaadin.ui.UI;
import com.vaadin.ui.VerticalLayout;
import com.vaadin.ui.themes.BaseTheme;

import ru.xpoft.vaadin.VaadinView;

@Component
@Scope("prototype")
@VaadinView(ReportsViewer.NAME)
@SuppressWarnings("serial")
@HaloAuthority(authority = "ROLE_IDM_ADMIN")
public class ReportsViewer extends VerticalLayout implements View {
	final Logger log = LoggerFactory.getLogger(ReportsViewer.class);
	public static final String NAME = "yReports";

	@Autowired
	private CSDAO dao;
	
	@Autowired
	protected PropertiesService properties;
	
	@Autowired
	protected LDAPService ldap;
	
	
	LDAPUser user;
	Table table;
	HorizontalLayout table_toolbar = new HorizontalLayout();


	@PostConstruct
	public void PostConstruct() { }
	
	protected void build() {

		setSizeFull();
		addStyleName("idm-requester-view");
		
		FormLayout l = new FormLayout();
		l.setSizeUndefined();
		l.setMargin(true);
		l.setStyleName("form-upload");
		l.setCaption("Ident. Reports");
		l.setSpacing(false);
		
		HorizontalLayout toolbar = new HorizontalLayout();
		toolbar.setSpacing(true);
		
		List<String> list = new LinkedList<String>();
		
		String first_item = "Please Select a Report";
		list.add(first_item);
		File f;
		try {
			f = SpringHelperService.get().getResourceAsURL("/WEB-INF/reports").getFile();
			list.addAll(Arrays.asList(f.list()));
			
		} catch (IOException e) {
			log.error("", e);
		}
		Collections.sort(list);
		
		ComboBox filterby = new ComboBox(null, new BeanItemContainer<String>(String.class, list));
	    filterby.setImmediate(true);
	    filterby.setWidth("26em");
	    filterby.setNullSelectionAllowed(false);
	    filterby.setValue(first_item);
	    filterby.addValueChangeListener(new ValueChangeListener() {
			@Override
			public void valueChange(ValueChangeEvent event) {
				if (filterby.getValue() != null && filterby.getValue() != first_item) {
					updateTable((String)filterby.getValue());	
				}
//				File f = new File("/var/tmp/" + ((String)filterby.getValue()).replace(" ", "").toLowerCase() + ".csv");
//				if (f.exists() && f.canRead()) {
//					downloadButton.setEnabled(true);
//				} else {
//					downloadButton.setEnabled(false);
//				}
				
			}});
	    
//	    Button launch = new Button("Launch...");
//	    launch.setSizeUndefined();
//	    launch.setStyleName("launch-button");
//	    launch.addClickListener(new Button.ClickListener() {				
//			@Override
//			public void buttonClick(ClickEvent event) { 
//				updateTable((String)filterby.getValue());	
//			}
//
//			
//	    });	
	    
	    toolbar.addComponent(filterby);
		//toolbar.addComponent(launch);
		
		table_toolbar.setSizeUndefined();
		table_toolbar.setStyleName("layout-info-bar");
		table_toolbar.setVisible(false);
		table_toolbar.setSpacing(true);

		 Button export = new Button("export");
		 export.setStyleName(BaseTheme.BUTTON_LINK);
		 export.addClickListener(new Button.ClickListener() {				
				@Override
				public void buttonClick(ClickEvent event) { 
					//HaloTableHolder h = new HaloTableHolder(table);
					ExcelExport excelExport = new ExcelExport(table, (String)filterby.getValue());
			
	                excelExport.excludeCollapsedColumns();
	               
	                excelExport.setUseTableFormatPropertyValue(false);     
	               // excelExport.setExportFileName((String)filterby.getValue() + ".xls");
	                excelExport.export();			
				}

				
		    });	
		table_toolbar.addComponent(export);
		
		l.addComponent(toolbar);
		
		this.addComponent(l);
		this.addComponent(table_toolbar);
	
		
	}
	
	private void updateTable(String value) {
		Properties report_properties = new Properties();
		try {
			File f = SpringHelperService.get().getResourceAsURL("/WEB-INF/reports/" + value).getFile();
			if (f.exists() && f.canRead()) {
				
				report_properties.load(new FileInputStream(f));
			}
		} catch (IOException e1) {
			// TODO Auto-generated catch block
			e1.printStackTrace();
			
		}
		if (report_properties.isEmpty()) {
			Notification.show("Halo Ident.", "Invalid Report [" + value + "]. Please contact your system administrator.", Notification.Type.ERROR_MESSAGE);
			return;
		}
		
		if (table != null) {
			table.removeAllItems();		
			this.removeComponent(table);
			table = null;
		}
		table = new Table();
		table.setSizeFull();
		
		
		switch (report_properties.getProperty("report.type", "ldap")) {
		case "ldap":
			try {
				//Class<?> c = Class.forName("com.collectivesystems.idm.beans.LDAPUser");
				for (String column : report_properties.getProperty("report.fields").split(",")) {
					if (column.startsWith("gen-")) {
						String column_property = column.replace("gen-", "");
						String type = report_properties.getProperty("report.column." + column_property + ".type");
						switch (type) {
						case "ldap":
							break;
						case "db":
							table.addGeneratedColumn(column, new ExportableColumnGenerator() {
								String query = null;
								Integer param_count = null;
								@Override
								public com.vaadin.ui.Component generateCell(Table source, final Object itemId, Object columnId) {
									Label label = new Label();
									
									label.setValue(getString(source, itemId, columnId));
									return label;
								}
								
								@Override
								public Property getGeneratedProperty(Object itemId, Object columnId) {
									Property comp = new Label(getString(null, itemId, columnId));
									return comp;
								}
								@Override
								public Class<?> getType() {
									return String.class;
								}
								
								private String getString(Table source, final Object itemId, Object columnId) {
									if (query == null) {  query = report_properties.getProperty("report.column." + column_property + ".query"); }									
									if (param_count == null) { param_count = org.springframework.util.StringUtils.countOccurrencesOf(query, ":"); }
									
									String query_params[] = new String[param_count]; 
									for (int i=0; i<param_count; i++) {
										String param_name = report_properties.getProperty("report.column." + column_property + ".param." + i);
										Property property= table.getContainerProperty(itemId, param_name);
									    String data = (String) property.getValue();
									    query_params[i] = data;
									   
									}
									log.debug(query);
									Object o = dao.query(query, query_params);
									return String.valueOf(o);
								}
							});
							break;
						}
					}
				}
				
				switch (report_properties.getProperty("report.bean", "LDAPUser")) {
				case "LDAPBean":
					List<LDAPUser> list = (List<LDAPUser>) ldap.report(report_properties.getProperty("report.query"));
					table.setContainerDataSource(new BeanItemContainer<>(LDAPUser.class, list));	
					table.setVisibleColumns(report_properties.getProperty("report.fields").split(","));
					table.setColumnHeaders(report_properties.getProperty("report.headers").split(","));
					break;
				case "LDAPGroup":
					List<Group> groups = (List<Group>) ldap.report(report_properties.getProperty("report.query"));
					table.setContainerDataSource(new BeanItemContainer<>(Group.class, groups));	
					table.setVisibleColumns(report_properties.getProperty("report.fields").split(","));
					table.setColumnHeaders(report_properties.getProperty("report.headers").split(","));
					break;
					
				}
				List<LDAPUser> list = (List<LDAPUser>) ldap.report(report_properties.getProperty("report.query"));
				table.setContainerDataSource(new BeanItemContainer<>(LDAPUser.class, list));	
				table.setVisibleColumns(report_properties.getProperty("report.fields").split(","));
				table.setColumnHeaders(report_properties.getProperty("report.headers").split(","));
				
				this.addComponent(table);
				this.setExpandRatio(table, 1);
				table_toolbar.setVisible(true);
				
			} catch (Exception e) {
				// TODO Auto-generated catch block
				e.printStackTrace();
			}
			
			
			
			break;
		case "db":
			try {
			
			//Class clazz = Class.forName(report_properties.getProperty("report.class"));
			ArrayList<?> list = dao.test(report_properties.getProperty("report.query"));
			
			table.setContainerDataSource(new BeanItemContainer<>(Object.class, list));	
			table.setVisibleColumns(report_properties.getProperty("report.fields").split(","));
			table.setColumnHeaders(report_properties.getProperty("report.headers").split(","));
			
			this.addComponent(table);
			this.setExpandRatio(table, 1);
			table_toolbar.setVisible(true);
			} catch (Exception e) {
				log.error("", e);
			}
			break;
		default:
			
		}
		
	} 
	
	
	@Override
	public void enter(ViewChangeEvent event) {
		user = ldap.getUser(((HaloUI)UI.getCurrent()).getUsername() );
		build();

	}

}
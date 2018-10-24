package com.collectivesystems.idm.ui.views;  

import java.time.Instant;
import java.time.LocalDate;
import java.time.ZoneId;
import java.time.temporal.ChronoUnit;
import java.util.Arrays;
import java.util.Date;
import java.util.LinkedList;
import java.util.List;
import java.util.Locale;

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
import com.collectivesystems.idm.beans.Group;
import com.collectivesystems.idm.beans.LDAPUser;
import com.collectivesystems.idm.beans.UserRequest;
import com.collectivesystems.idm.services.service.LDAPService;
import com.vaadin.data.Property.ValueChangeEvent;
import com.vaadin.data.Property.ValueChangeListener;
import com.vaadin.data.fieldgroup.BeanFieldGroup;
import com.vaadin.data.fieldgroup.FieldGroup.CommitException;
import com.vaadin.data.util.BeanItemContainer;
import com.vaadin.data.util.ObjectProperty;
import com.vaadin.navigator.View;
import com.vaadin.navigator.ViewChangeListener.ViewChangeEvent;
import com.vaadin.shared.ui.label.ContentMode;
import com.vaadin.ui.AbstractSelect.ItemCaptionMode;
import com.vaadin.ui.Button;
import com.vaadin.ui.Button.ClickEvent;
import com.vaadin.ui.CheckBox;
import com.vaadin.ui.ComboBox;
import com.vaadin.ui.CssLayout;
import com.vaadin.ui.FormLayout;
import com.vaadin.ui.Label;
import com.vaadin.ui.Table;
import com.vaadin.ui.Table.ColumnGenerator;
import com.vaadin.ui.UI;
import com.vaadin.ui.themes.BaseTheme;

import ru.xpoft.vaadin.VaadinView;

@Component
@Scope("prototype")
@VaadinView(Approvers.NAME)
@SuppressWarnings("serial")
@HaloAuthority(authority = "ROLE_")
public class Approvers extends CssLayout implements View {
	final Logger log = LoggerFactory.getLogger(Approvers.class);
	public static final String NAME = "zApprovers";

	@SuppressWarnings("unused")
	@Autowired
	private CSDAO dao;

	@Autowired
	protected PropertiesService properties;
	
	@Autowired
	protected LDAPService ldap;
	
	@Autowired
	@Qualifier("haloMessageSource")
	ReloadableResourceBundleMessageSource messages;

	Label stats = new Label();
    Table table;

    @PostConstruct
	public void PostConstruct() {
		if (properties.getProperty("push.enabled", "false").equals("false")) {
			UI.getCurrent().setPollInterval(10000);
		}
		setSizeFull();
		addStyleName("idm-requester-view");
	}
		
	protected void build() {

		LDAPUser user = ldap.getUser(((HaloUI)UI.getCurrent()).getUsername() );
		
		FormLayout l = new FormLayout();
		l.setSizeUndefined();
		l.setMargin(true);
		l.setStyleName("form-upload");
		l.setCaption("EUAA Requester Portal <span> - " + messages.getMessage("idm.userrequest.hint", null, Locale.UK) + "</span>");
		l.setCaptionAsHtml(true);
		l.setSpacing(false);

		stats.setCaption("Stats");
		stats.setSizeFull();

		UserRequest bean = new UserRequest();
		bean.init();
		// Form for editing the bean
		final BeanFieldGroup<UserRequest> binder = new BeanFieldGroup<>(UserRequest.class);
		binder.setItemDataSource(bean);
		
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
		l.addComponent(binder.buildAndBind("First Name", "fname"));
		l.addComponent(binder.buildAndBind("Last Name", "sname"));
		l.addComponent(binder.buildAndBind("Employee ID", "employeeID"));
		l.addComponent(binder.buildAndBind("Email", "exEmail"));
		l.addComponent(binder.buildAndBind("Phone Number", "phone"));
	//	l.addComponent(binder.buildAndBind("Group", "ggroup"));
		l.addComponent(binder.buildAndBind("NT Login", "ntlogin"));
	//	l.addComponent(binder.buildAndBind("Environment", "environment"));
		
		List<String> environment = Arrays.asList(properties.getProperty("idm.environments", "default, sample").split(", "));
		final BeanItemContainer<String> container = new BeanItemContainer<>(String.class, environment);
		final ComboBox combobox = new ComboBox("Environment", container);

		List<Group> groups = new LinkedList<>();//Arrays.asList(new String[] { "Please select an environment" });
		Group group = new Group();
		group.setCn("Please select an environment");
		groups.add(group);
		final BeanItemContainer<Group> group_container = new BeanItemContainer<>(Group.class, groups);
		final ComboBox group_combobox = new ComboBox("Group", group_container);
		
		
		
		combobox.setImmediate(true);
		//combobox.setItemCaptionMode(ItemCaptionMode.);
		//combobox.setItemCaptionPropertyId("profileName");
		combobox.setNewItemsAllowed(false);
		combobox.setWidth("16em");
		combobox.setNullSelectionAllowed(false);
		combobox.addValueChangeListener(new ValueChangeListener() {

			@Override
			public void valueChange(ValueChangeEvent event) {
				log.info((String) event.getProperty().getValue());
				String environment = ((String) event.getProperty().getValue()).toLowerCase();
				String group_location = properties.getProperty("idm.environments." + environment + ".groups", "");
				
				List<Group> list = ldap.getGroups(group_location);
				
				group_container.removeAllItems();
				log.info(user.toString());
				for (Group g: list) {
					log.info(g.toString());
					if (g.getMatcherAttr().equals(user.getMatcherAttr())) {
						group_container.addItem(g);
					}
				}
				//group_container.addAll(list);
				
				
			}});
		l.addComponent(combobox);
		
		group_combobox.setImmediate(true);
		group_combobox.setItemCaptionMode(ItemCaptionMode.PROPERTY);
		group_combobox.setItemCaptionPropertyId("cn");
		group_combobox.setNewItemsAllowed(false);
		group_combobox.setWidth("26em");
		group_combobox.setNullSelectionAllowed(false);
		l.addComponent(group_combobox);
		
		//Group approver_group = ldap.getGroup(properties.getProperty("idm.approver.group", "brm"));
		String approver_group = properties.getProperty("idm.approver.group.dn", "brm");
		List<LDAPUser> approvers = ldap.getUserByGroup(approver_group); //new LinkedList<>();//Arrays.asList(new String[] { "Please select an environment" });
		
		final BeanItemContainer<LDAPUser> approver_container = new BeanItemContainer<>(LDAPUser.class, approvers);
		final ComboBox approver_combobox = new ComboBox("BRM Approver", approver_container);
		approver_combobox.setImmediate(true);
		approver_combobox.setItemCaptionMode(ItemCaptionMode.PROPERTY);
		approver_combobox.setItemCaptionPropertyId("fullname");
		approver_combobox.setNewItemsAllowed(false);
		approver_combobox.setWidth("26em");
		approver_combobox.setNullSelectionAllowed(false);
		if (approvers.size() > 0) { approver_combobox.setValue(approvers.get(0)); }
		l.addComponent(approver_combobox);
		
		
		
		
		l.addComponent(binder.buildAndBind("Start Date", "startDate"));
		l.addComponent(binder.buildAndBind("End Date", "endDate"));
		
		LocalDate ld = LocalDate.now().plus(90, ChronoUnit.DAYS);		
		Instant instant = ld.atStartOfDay().atZone(ZoneId.systemDefault()).toInstant();
		Date res = Date.from(instant);
		bean.setEndDate(res);
		bean.setOrganisation(user.getMatcherAttr());
		bean.setRequester(user.getUid());
		bean.setRequesterEmail(user.getEmail());
		bean.setStatus(UserRequest.STATUS_REQUESTED);
		
		// Buffer the form content
		binder.setBuffered(true);
		l.addComponent(new Button("Submit Request", new Button.ClickListener() {
			@Override
			public void buttonClick(ClickEvent event) {
				try {
					binder.commit();
					bean.setEnvironment((String)combobox.getValue());
					bean.setGgroup(((Group)group_combobox.getValue()).getCn());
					bean.setApprover(((LDAPUser)approver_combobox.getValue()).getUid());
					bean.setApproverEmail(((LDAPUser)approver_combobox.getValue()).getEmail());
					
					dao.save(bean);
					binder.clear();
					
					updateTable(table);
					
				} catch (CommitException e) {
					
				}
			}

			
		}));

		this.addComponent(l);
		
		
		
		CssLayout izWrapper = new CssLayout();
		izWrapper.setSizeUndefined();
		izWrapper.setStyleName("iz-wrapper");

		Label crlabel = new Label();
		crlabel.setValue("Pending Requests (" + user.getUid() + ") <span> - " + messages.getMessage("idm.requeststable.hint", null, Locale.UK) + "</span>");
		crlabel.setStyleName("status-panel");
		crlabel.setContentMode(ContentMode.HTML); 
		
		CssLayout infoBar = new CssLayout();
		infoBar.setSizeUndefined();
		infoBar.setStyleName("layout-info-bar");
		
		Button b = new Button("refresh");
		((Button) b).setStyleName(BaseTheme.BUTTON_LINK);
		((Button) b).addClickListener(new Button.ClickListener() {
			@Override
			public void buttonClick(ClickEvent event) {
				updateTable(table);
			}
		});

//		Label izMaxSizeLabel = new Label();
//		izMaxSizeLabel.setSizeUndefined();
//		izMaxSizeLabel.setCaption("Total Requests:");
//		izMaxSizeLabel.setValue();
//		izMaxSizeLabel.setStyleName("label-iz-max-size");
//
//		infoBar.addComponent(izMaxSizeLabel);	
		infoBar.addComponent(b);
		
		table = new Table();
		table.setHeight("200px");
		table.setWidth("100%");
		// table.setSizeFull();
		table.setStyleName("table-sftp-iz");
		table.setImmediate(true);
		table.setSelectable(true);
		
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
		
		table.addGeneratedColumn("iz-name", new ColumnGenerator() {
			@Override
			public com.vaadin.ui.Component generateCell(Table source, final Object itemId, Object columnId) {
				UserRequest ur = ((UserRequest)itemId);
				
				return new Label(ur.getFname() + " " + ur.getSname());
			}
		});
		
		table.addGeneratedColumn("iz-action", new ColumnGenerator() {
			@Override
			public com.vaadin.ui.Component generateCell(Table source, final Object itemId, Object columnId) {
				
				com.vaadin.ui.Component b = null;
				 
				
				switch (((UserRequest)itemId).getStatus()) {
				case -1:
				case UserRequest.STATUS_REQUESTED :
				case UserRequest.STATUS_PENDING_APPROVAL:
				case UserRequest.STATUS_ERROR:
					b = new Button("cancel");
					((Button) b).setStyleName(BaseTheme.BUTTON_LINK);
					((Button) b).setData(itemId);
					((Button) b).addClickListener(new Button.ClickListener() {
						@Override
						public void buttonClick(ClickEvent event) {
							switch (event.getButton().getCaption()) {
							case "cancel":
								UserRequest ur = (UserRequest)event.getButton().getData();
								if (properties.getProperty("idm.shift.to.delete", "false").equals("true") && event.isShiftKey()) {
									dao.delete(ur);
								} else { 
									ur.setStatus(UserRequest.STATUS_CANCELLED);
									dao.save(ur);
								}
							}
						
							
							updateTable(table);
						}
					});
					break;
				default: 
					
				}
				
				
				
				return b;
			}
		});
		
		table.addGeneratedColumn("iz-status", new ColumnGenerator() {
			@Override
			public com.vaadin.ui.Component generateCell(Table source, final Object itemId, Object columnId) {
				Label label = new Label();
				label.setStyleName("iz-status-label");
				label.setValue( ((UserRequest) itemId).getStatus() > -1 ? UserRequest.STATUS_NAMES[((UserRequest) itemId).getStatus()] : "");
				return label;
			}
		});
		
		
//		izWrapper.addComponent(crlabel);
//		izWrapper.addComponent(infoBar);
//		izWrapper.addComponent(table);
		addComponent(crlabel);
		addComponent(infoBar);
		addComponent(table);
		//addComponent(izWrapper);
		
		updateTable(table);

	}
	
	private void updateTable(Table table) {
		LocalDate ld = LocalDate.now().minus(60, ChronoUnit.DAYS);		
		Instant instant = ld.atStartOfDay().atZone(ZoneId.systemDefault()).toInstant();
		Date res = Date.from(instant);
		List<UserRequest> list = UserRequest.getEntriesByDate(res);
//		total_requests = 0l;
//		for (FilePointer fp : list) {
//			total_space += fp.getFilesize();
//		}
//		izUsedSizeLabel.setValue(Utils.humanReadableByteCount(total_space, true));
		
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
		table.setContainerDataSource(new BeanItemContainer<>(UserRequest.class, list));
		table.setVisibleColumns(new Object[] { "iz-select", "created", "updated", "iz-name", "iz-status", "ggroup", "environment", "startDate", "endDate", "iz-action" });
		table.setColumnHeaders(new String[] { "","Requested", "Last Action", "Name", "Status", "Group", "Environment", "Start Date", "End Date", "Action" });
		table.setColumnWidth("iz-select", 40);
	}

	@Override
	public void enter(ViewChangeEvent event) {
		build();		
	}

}
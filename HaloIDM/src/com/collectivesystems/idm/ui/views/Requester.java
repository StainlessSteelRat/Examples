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
import org.vaadin.hene.flexibleoptiongroup.FlexibleOptionGroup;

import com.collectivesystems.core.annotations.HaloAuthority;
import com.collectivesystems.core.dao.CSDAO;
import com.collectivesystems.core.factory.HaloFactory;
import com.collectivesystems.core.services.service.PropertiesService;
import com.collectivesystems.core.services.service.SpringSecurityHelperService;
import com.collectivesystems.core.ui.providers.HaloUI;
import com.collectivesystems.idm.beans.Group;
import com.collectivesystems.idm.beans.LDAPUser;
import com.collectivesystems.idm.beans.UserRequest;
import com.collectivesystems.idm.services.service.LDAPService;
import com.vaadin.data.Property;
import com.vaadin.data.Property.ValueChangeEvent;
import com.vaadin.data.Property.ValueChangeListener;
import com.vaadin.data.fieldgroup.BeanFieldGroup;
import com.vaadin.data.fieldgroup.FieldGroup.CommitException;
import com.vaadin.data.util.BeanItemContainer;
import com.vaadin.data.util.ObjectProperty;
import com.vaadin.data.validator.EmailValidator;
import com.vaadin.navigator.View;
import com.vaadin.navigator.ViewChangeListener.ViewChangeEvent;
import com.vaadin.server.ThemeResource;
import com.vaadin.shared.ui.label.ContentMode;
import com.vaadin.ui.AbstractSelect.ItemCaptionMode;
import com.vaadin.ui.Button;
import com.vaadin.ui.Button.ClickEvent;
import com.vaadin.ui.CheckBox;
import com.vaadin.ui.ComboBox;
import com.vaadin.ui.CssLayout;
import com.vaadin.ui.CustomTable;
import com.vaadin.ui.DateField;
import com.vaadin.ui.Field;
import com.vaadin.ui.FormLayout;
import com.vaadin.ui.HorizontalLayout;
import com.vaadin.ui.Image;
import com.vaadin.ui.Label;
import com.vaadin.ui.Notification;
import com.vaadin.ui.TabSheet;
import com.vaadin.ui.TextField;
import com.vaadin.ui.UI;
import com.vaadin.ui.VerticalLayout;
import com.vaadin.ui.Window;
import com.vaadin.ui.Window.CloseEvent;
import com.vaadin.ui.themes.BaseTheme;

import ru.xpoft.vaadin.VaadinView;

@Component
@Scope("prototype")
@VaadinView(Requester.NAME)
@SuppressWarnings("serial")
@HaloAuthority(authority = "ROLE_EUAA")
public class Requester extends VerticalLayout implements View {
	final Logger log = LoggerFactory.getLogger(Requester.class);
	final static SimpleDateFormat df = new SimpleDateFormat("dd-MM-yyyy");
	final static SimpleDateFormat df_long = new SimpleDateFormat("dd MMM yyyy");
	public static final String NAME = "zRequester";

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
	FilterTable table;
	LDAPUser user;
	int max_days = 90;

	@PostConstruct
	public void PostConstruct() {
		if (properties.getProperty("push.enabled", "false").equals("false")) {
			UI.getCurrent().setPollInterval(10000);
		}
		setSizeFull();
		addStyleName("idm-requester-view");
	}

	protected void build() {

		FormLayout orgheader = new FormLayout();
		orgheader.setSizeUndefined();
		orgheader.setMargin(true);
		orgheader.setStyleName("form-upload");
		orgheader.setCaption("EUAA Requester Portal <span> - " + messages.getMessage("idm.userrequest.hint", null, Locale.UK) + "</span>");
		orgheader.setCaptionAsHtml(true);
		orgheader.setSpacing(false);

		HorizontalLayout forms = new HorizontalLayout();
		forms.setSizeUndefined();
		forms.setSpacing(false);

		FormLayout left = new FormLayout();
		left.setSizeUndefined();
		left.setMargin(true);
		left.setStyleName("form-upload");
		// left.setCaption();
		left.setCaptionAsHtml(true);
		left.setSpacing(false);

		FormLayout right = new FormLayout();
		right.setSizeUndefined();
		right.setMargin(true);
		right.setStyleName("form-upload");
		right.addStyleName("right-form-upload");
		// right.setCaption(null);
		right.setCaptionAsHtml(true);
		right.setSpacing(false);

		UserRequest bean = new UserRequest();
		bean.init();
		// Form for editing the bean
		final BeanFieldGroup<UserRequest> binder = new BeanFieldGroup<>(UserRequest.class);
		binder.setItemDataSource(bean);

		/*
		 * private String fname; private String sname; private String
		 * employeeID; private String exEmail; private String intEmail; private
		 * String phone; private String group; private String ntlogin; private
		 * String environment; private Date startDate; private Date endDate;
		 */

		List<String> environment = Arrays.asList(properties.getProperty("idm.environments", "default, sample").split(", "));
		final BeanItemContainer<String> environment_container = new BeanItemContainer<>(String.class, environment);
		final ComboBox combobox = new ComboBox("Environment", environment_container);
		combobox.setRequired(true);

		List<Group> groups = new LinkedList<>();// Arrays.asList(new String[] {
												// "Please select an
												// environment" });
		final Group default_group = new Group();
		default_group.setCn("Please select an environment");
		groups.add(default_group);
		final BeanItemContainer<Group> group_container = new BeanItemContainer<>(Group.class, groups);
		final ComboBox group_combobox = new ComboBox("Group", group_container);

		// Group approver_group =
		// ldap.getGroup(properties.getProperty("idm.approver.group", "brm"));
		final String approver_group = properties.getProperty("idm.approver.group.dn", "brm");
		List<LDAPUser> approvers = ldap.getUsersByMatcherAndGroupAndNotLocked(user.getMatcherAttr(), approver_group); // new
																											// LinkedList<>();//Arrays.asList(new
																											// String[]
																											// {
																											// "Please
																											// select
																											// an
																											// environment"
																											// });
		Collections.sort(approvers);

		final BeanItemContainer<LDAPUser> approver_container = new BeanItemContainer<>(LDAPUser.class, approvers);
		final ComboBox approver_combobox = new ComboBox("BRM Approver", approver_container);

		if (SpringSecurityHelperService.hasRole("ROLE_EUAA_ADMIN")) {
			Set<String> organisations = ldap.getOrganisations();
			final BeanItemContainer<String> org_container = new BeanItemContainer<>(String.class, organisations);
			final ComboBox org_combobox = new ComboBox("Organisation", org_container);

			org_combobox.setImmediate(true);
			// combobox.setItemCaptionMode(ItemCaptionMode.);
			// combobox.setItemCaptionPropertyId("profileName");
			org_combobox.setNewItemsAllowed(false);
			org_combobox.setWidth("16em");
			org_combobox.setNullSelectionAllowed(false);
			org_combobox.setStyleName("orgcombobox");
			org_combobox.setValue(user.getMatcherAttr());
			org_combobox.addValueChangeListener(new ValueChangeListener() {
				@Override
				public void valueChange(ValueChangeEvent event) {
					log.trace((String) event.getProperty().getValue());
					user.setMatcherAttr((String) event.getProperty().getValue());
					group_container.removeAllItems();
					group_container.addBean(default_group);

					combobox.setValue(null);

					approver_container.removeAllItems();
					List<LDAPUser> approvers = ldap.getUsersByMatcherAndGroupAndNotLocked(user.getMatcherAttr(), approver_group); // new
																														// LinkedList<>();//Arrays.asList(new
																														// String[]
																														// {
																														// "Please
																														// select
																														// an
																														// environment"
																														// });
					Collections.sort(approvers);
					approver_container.addAll(approvers);
					if (user.getLastApprover() == null || user.getLastApprover().isEmpty()) {
						if (approvers.size() > 0) {
							approver_combobox.setValue(approvers.get(0));
						}
					} else {
						approver_combobox.setValue(ldap.getUser(user.getLastApprover()));
					}

				}
			});
			orgheader.addComponent(org_combobox);
			// orgheader.addComponent(new Label(""));
		}
		DateField startdate_field = (DateField) binder.buildAndBind("Start Date", "startDate");
		DateField enddate_field = (DateField) binder.buildAndBind("End Date", "endDate");

		CheckBox make_requester = new CheckBox("Requester");
		if (SpringSecurityHelperService.hasRole("ROLE_IDM_ADMIN")) {

			left.addComponent(make_requester);
			make_requester.addValueChangeListener(new ValueChangeListener() {
				@Override
				public void valueChange(ValueChangeEvent event) {
					if ((boolean) event.getProperty().getValue()) {
						combobox.setValue(properties.getProperty("idm.requester.default.env", "default"));
						combobox.setEnabled(false);
						group_combobox.setEnabled(false);
						max_days = 365;

					} else {
						combobox.setEnabled(true);
						group_combobox.setEnabled(true);
						max_days = 90;
					}
					Date d = (Date) startdate_field.getValue();
					Instant instant = d.toInstant().atZone(ZoneId.systemDefault()).plusDays(max_days).toInstant();
					enddate_field.setRangeEnd(Date.from(instant));
					enddate_field.setValue(Date.from(instant));
				}
			});
		}

		combobox.setImmediate(true);
		// combobox.setItemCaptionMode(ItemCaptionMode.);
		// combobox.setItemCaptionPropertyId("profileName");
		combobox.setNewItemsAllowed(false);
		combobox.setWidth("16em");
		combobox.setNullSelectionAllowed(true);
		combobox.setNullSelectionItemId(new String("Please select an Environment"));
		combobox.addValueChangeListener(new ValueChangeListener() {

			@Override
			public void valueChange(ValueChangeEvent event) {
				if ((String) event.getProperty().getValue() == null) { return; }
				log.info((String) event.getProperty().getValue());
				String environment = ((String) event.getProperty().getValue()).toLowerCase();
				String group_location = properties.getProperty("idm.environments." + environment + ".groups", "");

				List<Group> list = ldap.getGroups(group_location);

				group_container.removeAllItems();
				log.trace(user.toString());
				for (Group g : list) {
					// log.info(g.toString());
					if (g.getMatcherAttr().equalsIgnoreCase(user.getMatcherAttr())) {
						group_container.addItem(g);
					}
				}
				// group_container.addAll(list);

			}
		});
		left.addComponent(combobox);

		group_combobox.setImmediate(true);
		group_combobox.setRequired(true);
		group_combobox.setItemCaptionMode(ItemCaptionMode.PROPERTY);
		group_combobox.setItemCaptionPropertyId("cn");
		group_combobox.setNewItemsAllowed(false);
		group_combobox.setWidth("26em");
		group_combobox.setNullSelectionAllowed(false);
		left.addComponent(group_combobox);

		approver_combobox.setImmediate(true);
		approver_combobox.setRequired(true);
		approver_combobox.setItemCaptionMode(ItemCaptionMode.PROPERTY);
		approver_combobox.setItemCaptionPropertyId("cn");
		approver_combobox.setNewItemsAllowed(false);
		approver_combobox.setWidth("26em");
		approver_combobox.setNullSelectionAllowed(false);
		if (user.getLastApprover() == null || user.getLastApprover().isEmpty() || ldap.getUser(user.getLastApprover()).isAccountLocked()) {
			if (approvers.size() > 0) {
				approver_combobox.setValue(approvers.get(0));
			}
		} else {
			approver_combobox.setValue(ldap.getUser(user.getLastApprover()));
			
		}
		approver_combobox.addValueChangeListener(new ValueChangeListener() {

			@Override
			public void valueChange(ValueChangeEvent event) {
				if (event.getProperty().getValue() != null) {
					ldap.updateUserLastApprover(user, ((LDAPUser) event.getProperty().getValue()).getUid());
				}

			}
		});
		left.addComponent(approver_combobox);

		LocalDate ld = LocalDate.now().plus(max_days, ChronoUnit.DAYS);
		Instant instant = ld.atStartOfDay().atZone(ZoneId.systemDefault()).toInstant();
		Date res = Date.from(instant);

		{

			startdate_field.addValueChangeListener(new ValueChangeListener() {

				@Override
				public void valueChange(ValueChangeEvent event) {
					Date d = (Date) event.getProperty().getValue();
					if (d != null) { // can be null when binder.clear()
						Instant instant = d.toInstant().atZone(ZoneId.systemDefault()).plusDays(max_days).toInstant();
						enddate_field.setRangeEnd(Date.from(instant));
						enddate_field.setValue(Date.from(instant));
					}

				}
			});
			startdate_field.setRangeStart(new Date());
			startdate_field.setWidth("16em");
			startdate_field.setLocale(Locale.UK);
			startdate_field.setImmediate(true);
			startdate_field.setRequired(true);
			enddate_field.setDateOutOfRangeMessage("Earlist Start Date is today");
			left.addComponent(startdate_field);
		}
		{

			enddate_field.setRangeStart(new Date());
			enddate_field.setRangeEnd(res);
			enddate_field.setDateOutOfRangeMessage("Maximum End Date is " + max_days + " days after the Start Date");
			enddate_field.setWidth("16em");
			enddate_field.setLocale(Locale.UK);
			enddate_field.setImmediate(true);
			enddate_field.setRequired(true);
			left.addComponent(enddate_field);
		}

		left.addComponent(binder.buildAndBind("Business Justification", "businessJustification"));
		((TextField) binder.getField("businessJustification")).setWidth("36em");

		CheckBox auto_approve = new CheckBox("Auto Approve");

		if (SpringSecurityHelperService.hasRole("ROLE_EUAA_ADMIN")) {
			left.addComponent(auto_approve);
		}

		bean.setEndDate(res);
		bean.setOrganisation(user.getMatcherAttr());
		bean.setRequester(user.getUid());
		bean.setRequesterEmail(user.getEmail());
		bean.setStatus(UserRequest.STATUS_REQUESTED);
		bean.setAction(UserRequest.ACTION_CREATE);

		// left.addComponent(new Label(""));
		//// -end--- move to bean init either in bean or in this.

		right.addComponent(binder.buildAndBind("First Name", "fname"));
		right.addComponent(binder.buildAndBind("Last Name", "sname"));
		right.addComponent(binder.buildAndBind("Employee ID", "employeeID"));
		{
			Field<?> f = binder.buildAndBind("Supplier Email", "exEmail");
			f.addValidator(new EmailValidator("Invalid email address"));
			f.setRequired(true);
			right.addComponent(f);
		}
		{
			Field<?> f = binder.buildAndBind("VF Email", "intEmail");
			f.addValidator(new EmailValidator("Invalid email address"));
			right.addComponent(f);
		}

		right.addComponent(binder.buildAndBind("Phone Number", "phone"));
		// l.addComponent(binder.buildAndBind("Group", "ggroup"));
		right.addComponent(binder.buildAndBind("NT Login", "ntlogin"));
		// l.addComponent(binder.buildAndBind("Environment", "environment"));

		// Buffer the form content
		binder.setBuffered(true);
		right.addComponent(new Button("Submit Request", new Button.ClickListener() {
			@Override
			public void buttonClick(ClickEvent event) {
				try {
					binder.commit();

					String env = (String) combobox.getValue();

					String group = "";
					UserRequest ur_bean = binder.getItemDataSource().getBean();
					if (make_requester != null && make_requester.getValue()) {
						Group requester_group = ldap.getGroupByDn(properties.getProperty("idm.requester.group.dn", "cn=EUAA"));
						ur_bean.setGgroup(requester_group.getCn());
						ur_bean.setAction(UserRequest.ACTION_CREATE_REQUESTER);
					} else {

						group = ((Group) group_combobox.getValue()).getCn();
						ur_bean.setGgroup(group);

						// check if any requests already exist for this user
						// with the same group
						List<UserRequest> previous_requests = UserRequest.getEntriesByUserDetails(ur_bean.getGgroup(), ur_bean.getExEmail());
						if (!previous_requests.isEmpty()) {
							for (UserRequest r : previous_requests) {
								log.error(r.toString());
							}
							Notification.show("A User Creation Request has already been submitted for this user and group", "Click to dismiss",
									Notification.Type.ERROR_MESSAGE);
							return;
						}
						List<LDAPUser> previous_users = ldap.findUserByEmailAndGroup(ur_bean.getExEmail(), ldap.getGroup(ur_bean.getGgroup()).getDn());
						if (!previous_users.isEmpty()) {
							Notification.show("A User with the same Email and Group already Exists", "Click to dismiss", Notification.Type.ERROR_MESSAGE);
							return;
						}

					}

					String just = ur_bean.getBusinessJustification();
					ur_bean.setEnvironment(env);
					ur_bean.setOrganisation(user.getMatcherAttr());

					LDAPUser approver = ((LDAPUser) approver_combobox.getValue());
					ur_bean.setApprover(approver.getUid());
					ur_bean.setApproverEmail(approver.getEmail());
					if (auto_approve != null && auto_approve.getValue()) {
						ur_bean.setStatus(UserRequest.STATUS_APPROVED);
					}

					Date startdate = startdate_field.getValue();
					Date enddate = enddate_field.getValue();

					// log.error(ur_bean.toString());
					dao.save(ur_bean);

					Notification.show("Halo Ident.", "User Creation Request Submitted", Notification.Type.TRAY_NOTIFICATION);

					binder.clear();

					ur_bean = new UserRequest().init();
					//// ---- move to bean init either in bean or in this.
					log.error(just);
					ur_bean.setOrganisation(user.getMatcherAttr());
					ur_bean.setRequester(user.getUid());
					ur_bean.setRequesterEmail(user.getEmail());
					ur_bean.setStatus(UserRequest.STATUS_REQUESTED);
					ur_bean.setBusinessJustification(just);
					binder.setItemDataSource(ur_bean);
					combobox.setValue(env);
					group_combobox.setValue(group);

					approver_combobox.setValue(approver);
					startdate_field.setValue(startdate);
					enddate_field.setValue(enddate);

					//// -end--- move to bean init either in bean or in this.

					updateTable(table);

				} catch (CommitException e) {

				}
			}

		}));
		this.addComponent(orgheader);
		forms.addComponent(left);
		forms.addComponent(right);

		this.addComponent(forms);
// Tab sheet
//		TabSheet tabsheet = new TabSheet();
//		layout.addComponent(tabsheet);
//
//		// Create the first tab
//		VerticalLayout tab1 = new VerticalLayout();
//		tab1.addComponent(new Image(null, new ThemeResource("icons/flask.svg")));
//		tabsheet.addTab(tab1, "Mercury", new ThemeResource("icons/flask.svg"));
//
//		// This tab gets its caption from the component caption
//		VerticalLayout tab2 = new VerticalLayout();
//		tab2.addComponent(new Image(null, new ThemeResource("icons/flask.svg")));
//		tab2.setCaption("Venus");
//		tabsheet.addTab(tab2).setIcon(new ThemeResource("icons/flask.svg"));

	// end tab sheet
		
//		CssLayout izWrapper = new CssLayout();
//		izWrapper.setSizeUndefined();
//		izWrapper.setStyleName("iz-wrapper");

		Label crlabel = new Label();
		crlabel.setValue("Pending Requests (" + user.getUid() + ") <span> - " + messages.getMessage("idm.requeststable.hint", null, Locale.UK) + "</span>");
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

		Button filter = new Button();
		filter.setWidth("26px");
		filter.setHeight("26px");
		filter.setIcon(new ThemeResource("icons/flask.svg"));
		((Button) filter).setStyleName(BaseTheme.BUTTON_LINK);
		((Button) filter).addClickListener(new Button.ClickListener() {
			@Override
			public void buttonClick(ClickEvent event) {
				table.setFilterBarVisible(!table.isFilterBarVisible());
				// event.getButton().setCaption(table.isFilterBarVisible() ?
				// "filter off" : "filter on");
				if (!table.isFilterBarVisible()) {
					table.clearFilters();
				}
			}
		});
		infoBar.addComponent(filter);
		infoBar.addComponent(b);

		table = new FilterTable() {
			@Override
			protected String formatPropertyValue(Object rowId, Object colId, Property property) {
				Object v = property.getValue();
				if (v instanceof Date && (((String) colId).equals("endDate") || ((String) colId).equals("startDate"))) { return df_long.format((Date) v); }
				return super.formatPropertyValue(rowId, colId, property);
			}

		};
		// table.setHeight("200px");
		// table.setWidth("100%");
		table.setSizeFull();
		table.setStyleName("table-sftp-iz");
		table.setImmediate(true);
		table.setSelectable(false);
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

			}
		});
		// flexibleOptionGroup.setItemCaptionPropertyId(CAPTION_PROPERTY);
		// flexibleOptionGroup.setItemIconPropertyId(ICON_PROPERTY);

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
				actions.addStyleName("table-actions");

				switch (((UserRequest) itemId).getStatus()) {
				case -1:
				case UserRequest.STATUS_REQUESTED:
				case UserRequest.STATUS_PENDING_APPROVAL:
				case UserRequest.STATUS_ERROR: {
					Button b = new Button();
					((Button) b).setStyleName(BaseTheme.BUTTON_LINK);
					b.setIcon(new ThemeResource("icons/times-circle.svg"));
					b.setWidth("14px");
					b.setHeight("20px");
					b.setDescription("Cancel Request");
					((Button) b).setData(itemId);
					((Button) b).addClickListener(new Button.ClickListener() {
						@Override
						public void buttonClick(ClickEvent event) {
							UserRequest ur = (UserRequest) event.getButton().getData();
							if (properties.getProperty("idm.shift.to.delete", "false").equals("true") && event.isShiftKey()) {
								dao.delete(ur);
							} else {
								ur.setStatus(UserRequest.STATUS_CANCELLED);
								dao.save(ur);
							}

							updateTable(table);
						}
					});
					actions.addComponent(b);
				}
					actions.addComponent(HaloFactory.label("sep", " / ", false));

				{
					Button b = new Button();
					((Button) b).setStyleName(BaseTheme.BUTTON_LINK);
					b.setIcon(new ThemeResource("icons/star.svg"));
					b.setWidth("14px");
					b.setHeight("20px");
					b.setDescription("Change Approver");
					b.setData(itemId);
					b.addClickListener(e -> changeApprover((UserRequest) e.getButton().getData()));
					actions.addComponent(b);
				}
					break;
				default:

				}

				return actions;
			}

		});

		table.addGeneratedColumn("iz-status", new FilterTable.ColumnGenerator() {
			@Override
			public com.vaadin.ui.Component generateCell(CustomTable source, final Object itemId, Object columnId) {
				Label label = new Label();
				label.setStyleName("iz-status-label");
				label.setValue(((UserRequest) itemId).getStatus() > -1 ? UserRequest.STATUS_NAMES[((UserRequest) itemId).getStatus()] : "");
				return label;
			}
		});

		table.addGeneratedColumn("gen-action", new FilterTable.ColumnGenerator() {
			@Override
			public com.vaadin.ui.Component generateCell(CustomTable source, final Object itemId, Object columnId) {
				Label label = new Label();
				label.setValue(((UserRequest) itemId).getAction() > -1 ? UserRequest.ACTION_NAMES[((UserRequest) itemId).getAction()] : "");
				return label;
			}
		});

		// izWrapper.addComponent(crlabel);
		// izWrapper.addComponent(infoBar);
		// izWrapper.addComponent(table);
		addComponent(crlabel);
		addComponent(infoBar);
		addComponent(table);
		// addComponent(izWrapper);
		this.setExpandRatio(table, 1);
		updateTable(table);

	}

	private void changeApprover(UserRequest ur) {
		Window w = new Window();
		UI.getCurrent().addWindow(w);
		w.setVisible(true);
		w.addCloseListener(new Window.CloseListener() {
			@Override
			public void windowClose(CloseEvent e) {
				updateTable(table);

			}
		});
		VerticalLayout layout = new VerticalLayout();
		layout.setSizeFull();
		layout.addStyleName("idm-requester-view");
		w.setContent(layout);
		w.setModal(true);
		w.setHeight("40%");
		w.setWidth("70%");

		FormLayout l = new FormLayout();
		l.setSizeUndefined();
		l.setMargin(true);
		l.setStyleName("form-upload");
		l.setCaption("Change Approver <span></span>");
		l.setCaptionAsHtml(true);
		l.setSpacing(false);

		l.addComponent(HaloFactory.label("", "<b>" + String.valueOf(ur.getId()) + "</b>", true, "Request ID"));
		l.addComponent(HaloFactory.label("", ur.getFullname(), false, "Name"));
		l.addComponent(HaloFactory.label("", UserRequest.ACTION_NAMES[ur.getAction()], false, "Requested Action"));
		l.addComponent(HaloFactory.label("", ur.getEnvironment(), false, "Environment"));
		l.addComponent(HaloFactory.label("", ur.getGgroup(), false, "Group"));
		l.addComponent(HaloFactory.label("", ur.getBusinessJustification(), false, "Business Justification"));

		final String approver_group = properties.getProperty("idm.approver.group.dn", "brm");
		List<LDAPUser> approvers = ldap.getUsersByMatcherAndGroup(user.getMatcherAttr(), approver_group); // new
																											// LinkedList<>();//Arrays.asList(new
																											// String[]
																											// {
																											// "Please
																											// select
																											// an
																											// environment"
																											// });
		Collections.sort(approvers);

		final BeanItemContainer<LDAPUser> approver_container = new BeanItemContainer<>(LDAPUser.class, approvers);
		final ComboBox approver_combobox = new ComboBox("BRM Approver", approver_container);

		approver_combobox.setImmediate(true);
		approver_combobox.setItemCaptionMode(ItemCaptionMode.PROPERTY);
		approver_combobox.setItemCaptionPropertyId("cn");
		approver_combobox.setWidth("26em");
		approver_combobox.setNullSelectionAllowed(false);
		if (approvers.size() > 0) {
			approver_combobox.setValue(ldap.getUser(ur.getApprover()));
		}
		l.addComponent(approver_combobox);

		Button cancel = new Button("Cancel", new Button.ClickListener() {
			private static final long serialVersionUID = -4314026625372219213L;

			@Override
			public void buttonClick(ClickEvent event) {
				updateTable(table);
				w.close();
			}
		});
		cancel.setStyleName(BaseTheme.BUTTON_LINK);

		HorizontalLayout actions = new HorizontalLayout();
		actions.addComponent(cancel);
		actions.setSpacing(true);

		actions.addComponent(new Button("Submit Request", new Button.ClickListener() {
			private static final long serialVersionUID = -4314026625372219213L;

			@Override
			public void buttonClick(ClickEvent event) {
				LDAPUser approver = (LDAPUser) approver_combobox.getValue();
				ur.setApprover(approver.getUid());
				ur.setApproverEmail(approver.getEmail());
				ur.setStatus(UserRequest.STATUS_REQUESTED);
				dao.save(ur);
				Notification.show("Halo Ident.", "User Request (" + ur.getId() + ") Updated", Notification.Type.TRAY_NOTIFICATION);

				w.close();
			}
		}));

		l.addComponent(actions);

		layout.addComponent(l);
	}

	private void updateTable(FilterTable table) {
		LocalDate ld = LocalDate.now().minus(60, ChronoUnit.DAYS);
		Instant instant = ld.atStartOfDay().atZone(ZoneId.systemDefault()).toInstant();
		Date res = Date.from(instant);
		List<UserRequest> list = UserRequest.getEntriesByRequesterAndDate(user.getUid(), res);
		
		/*
		 * private String fname; private String sname; private String
		 * employeeID; private String exEmail; private String intEmail; private
		 * String phone; private String group; private String ntlogin; private
		 * String environment; private Date startDate; private Date endDate;
		 */
		table.setContainerDataSource(new BeanItemContainer<>(UserRequest.class, list));
		table.setVisibleColumns(new Object[] { "id", "created", "fullname", "gen-action", "iz-status", "username", "ggroup", "environment", "startDate", "endDate", "approver", "msg", "iz-action" });
		table.setColumnHeaders(new String[] { "ID", "Requested", "Name", "Requested Action", "Status", "Username", "Group", "Environment", "Start Date", "End Date", "BRM", "Message", "Action" });
		table.setColumnWidth("iz-select", 40);
	}

	@Override
	public void enter(ViewChangeEvent event) {
		user = ldap.getUser(((HaloUI) UI.getCurrent()).getUsername());
		build();
	}

}
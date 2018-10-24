package com.collectivesystems.idm.ui.views;

import java.io.File;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.OutputStream;
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
import com.collectivesystems.idm.beans.DecommItem;
import com.collectivesystems.idm.beans.Group;
import com.collectivesystems.idm.beans.LDAPUser;
import com.collectivesystems.idm.services.service.FileLocatorService;
import com.collectivesystems.idm.services.service.LDAPService;
import com.vaadin.data.Property;
import com.vaadin.data.Property.ValueChangeEvent;
import com.vaadin.data.Property.ValueChangeListener;
import com.vaadin.data.fieldgroup.BeanFieldGroup;
import com.vaadin.data.fieldgroup.FieldGroup.CommitException;
import com.vaadin.data.util.BeanItemContainer;
import com.vaadin.data.util.ObjectProperty;
import com.vaadin.navigator.View;
import com.vaadin.navigator.ViewChangeListener.ViewChangeEvent;
import com.vaadin.server.FileDownloader;
import com.vaadin.server.FileResource;
import com.vaadin.server.Resource;
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
import com.vaadin.ui.FormLayout;
import com.vaadin.ui.HorizontalLayout;
import com.vaadin.ui.Label;
import com.vaadin.ui.Notification;
import com.vaadin.ui.ProgressBar;
import com.vaadin.ui.TextField;
import com.vaadin.ui.UI;
import com.vaadin.ui.Upload;
import com.vaadin.ui.Upload.FailedEvent;
import com.vaadin.ui.Upload.FinishedEvent;
import com.vaadin.ui.Upload.Receiver;
import com.vaadin.ui.Upload.StartedEvent;
import com.vaadin.ui.Upload.SucceededEvent;
import com.vaadin.ui.VerticalLayout;
import com.vaadin.ui.Window;
import com.vaadin.ui.Window.CloseEvent;
import com.vaadin.ui.themes.BaseTheme;

import ru.xpoft.vaadin.VaadinView;

@Component
@Scope("prototype")
@VaadinView(Decomm.NAME)
@SuppressWarnings("serial")
@HaloAuthority(authority = "ROLE_EUAA")
public class Decomm extends VerticalLayout implements View {
	final Logger log = LoggerFactory.getLogger(Decomm.class);
	final static SimpleDateFormat df = new SimpleDateFormat("dd-MM-yyyy");
	final static SimpleDateFormat df_long = new SimpleDateFormat("dd MMM yyyy");
	public static final String NAME = "zDecomm";

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
	
	@Autowired 
	protected FileLocatorService _locator;
	
	private Label state = new Label();
	private Label result = new Label();
	private Label fileName = new Label();
	private Label textualProgress = new Label();
	private Label mime = new Label();
	
	private ProgressBar pi = new ProgressBar();
	private FileReceiver counter = new FileReceiver();
	private Upload upload = new Upload("", counter);
	final Button cancelProcessing = new Button("Cancel processing");

	@PostConstruct
	public void PostConstruct() {
		if (properties.getProperty("push.enabled", "false").equals("false")) {
			UI.getCurrent().setPollInterval(10000);
		}
		setSizeFull();
		addStyleName("idm-requester-view");
	}

	protected void build() {
		
		CssLayout uploadWrapper = new CssLayout();
		uploadWrapper.setSizeUndefined();
		uploadWrapper.setStyleName("upload-wrapper");
		
		// make analyzing start immediatedly when file is selected
		upload.setImmediate(true);
		upload.setStyleName("file-upload");
		upload.setButtonCaption("Upload File");


		
		cancelProcessing.addClickListener(new Button.ClickListener() {
			public void buttonClick(ClickEvent event) {
				//dao.save(new LogEntry(LogEntry.STATUS_UPLOAD_CANCELED, ((HaloUI)UI.getCurrent()).getUsername(), fileName.getValue(), mime.getValue()));
				upload.interruptUpload();
			}
		});
		cancelProcessing.setEnabled(false);
		cancelProcessing.setStyleName("cancel-processing");


		FormLayout orgheader = new FormLayout();
		orgheader.setSizeUndefined();
		orgheader.setMargin(true);
		orgheader.setStyleName("form-upload");
		orgheader.setCaption("Decommisioning Tracker <span> - " + messages.getMessage("idm.decommissioning.hint", null, Locale.UK) + "</span>");
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

		FormLayout middle = new FormLayout();
		middle.setSizeUndefined();
		middle.setMargin(true);
		middle.setStyleName("form-upload");
		middle.addStyleName("right-form-upload");
		// right.setCaption(null);
		middle.setCaptionAsHtml(true);
		middle.setSpacing(true);
		
		
		FormLayout right = new FormLayout();
		right.setSizeUndefined();
		right.setMargin(true);
		right.setStyleName("form-upload");
		right.addStyleName("right-form-upload");
		// right.setCaption(null);
		right.setCaptionAsHtml(true);
		right.setSpacing(false);

		DecommItem bean = new DecommItem();
		bean.init();
		// Form for editing the bean
		final BeanFieldGroup<DecommItem> binder = new BeanFieldGroup<>(DecommItem.class);
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

		DateField enddate_field = (DateField) binder.buildAndBind("End Date", "endDate");

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
		if (user.getLastApprover() == null || user.getLastApprover().isEmpty()) {
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

		{

			enddate_field.setRangeStart(new Date());
			enddate_field.setDateOutOfRangeMessage("Maximum End Date is " + max_days + " days after the Start Date");
			enddate_field.setWidth("16em");
			enddate_field.setLocale(Locale.UK);
			enddate_field.setImmediate(true);
			enddate_field.setRequired(true);
			left.addComponent(enddate_field);
		}

		left.addComponent(binder.buildAndBind("Business Justification", "businessJustification"));
		((TextField) binder.getField("businessJustification")).setWidth("36em");

		// CheckBox auto_approve = new CheckBox("Auto Approve");
		//
		// if (SpringSecurityHelperService.hasRole("ROLE_EUAA_ADMIN")) {
		// left.addComponent(auto_approve);
		// }

		bean.setEndDate(new Date());
		bean.setOrganisation(user.getMatcherAttr());
		bean.setRequester(user.getUid());
		bean.setRequesterEmail(user.getEmail());
		bean.setStatus(DecommItem.STATUS_REQUESTED);
		bean.setAction(DecommItem.ACTION_CREATE);

		// left.addComponent(new Label(""));
		//// -end--- move to bean init either in bean or in this.

		middle.addComponent(binder.buildAndBind("Remove Group", "checkGroup"));
		middle.addComponent(binder.buildAndBind("Remove SGD Icons", "checkSGDIcons"));
		middle.addComponent(binder.buildAndBind("Remove VDTs", "checkVDTs"));
		middle.addComponent(binder.buildAndBind("Remove EUAAs", "checkEUAAs"));
		middle.addComponent(binder.buildAndBind("Remove BRMs", "checkBRMs"));

		state.setCaption("Current state");
		state.setValue("Idle");
		right.addComponent(state);
		fileName.setCaption("File name");
		right.addComponent(fileName);
		result.setCaption("Size");
		right.addComponent(result);
		mime.setCaption("MIME");
		right.addComponent(mime);
		pi.setCaption("Progress");
		pi.setVisible(false);
		right.addComponent(pi);
		textualProgress.setVisible(false);
		right.addComponent(textualProgress);
		
		HorizontalLayout buttons = new HorizontalLayout();
		buttons.setMargin(false);
		buttons.setSpacing(true);
		
		 
		
		
		setupUploader();
		
		// Buffer the form content
		binder.setBuffered(true);
		buttons.addComponent(upload);
		buttons.addComponent(cancelProcessing);
		buttons.addComponent(new Button("Submit", new Button.ClickListener() {
			@Override
			public void buttonClick(ClickEvent event) {
				try {
					binder.commit();

					String env = (String) combobox.getValue();

					String group = "";
					DecommItem ur_bean = binder.getItemDataSource().getBean();

					group = ((Group) group_combobox.getValue()).getCn();
					ur_bean.setGgroup(group);

					ur_bean.setUsername(user.getFullname());
					ur_bean.setAttachment(counter.getRealFileName());

					String just = ur_bean.getBusinessJustification();
					ur_bean.setEnvironment(env);
					ur_bean.setOrganisation(user.getMatcherAttr());

					LDAPUser approver = ((LDAPUser) approver_combobox.getValue());
					ur_bean.setApprover(approver.getUid());
					ur_bean.setApproverEmail(approver.getEmail());
					// if (auto_approve != null && auto_approve.getValue()) {
					// ur_bean.setStatus(DecommItem.STATUS_APPROVED); }

					Date enddate = enddate_field.getValue();

					// log.error(ur_bean.toString());
					dao.save(ur_bean);

					Notification.show("Halo Ident.", "User Creation Request Submitted", Notification.Type.TRAY_NOTIFICATION);

					binder.clear();

					ur_bean = new DecommItem().init();
					//// ---- move to bean init either in bean or in this.
					log.error(just);
					ur_bean.setOrganisation(user.getMatcherAttr());
					ur_bean.setRequester(user.getUid());
					ur_bean.setRequesterEmail(user.getEmail());
					ur_bean.setStatus(DecommItem.STATUS_REQUESTED);
					ur_bean.setBusinessJustification(just);
					binder.setItemDataSource(ur_bean);
					combobox.setValue(env);
					group_combobox.setValue(group);

					approver_combobox.setValue(approver);

					enddate_field.setValue(enddate);

					//// -end--- move to bean init either in bean or in this.

					updateTable(table);

				} catch (Exception e) {
					Notification.show("Halo Ident.", "Please make sure all fields are filled", Notification.Type.ERROR_MESSAGE);

				}
			}

		}));
		this.addComponent(orgheader);
		forms.addComponent(left);
		forms.addComponent(middle);
		forms.addComponent(right);

		this.addComponent(forms);
		
		this.addComponent(buttons);

		CssLayout izWrapper = new CssLayout();
		izWrapper.setSizeUndefined();
		izWrapper.setStyleName("iz-wrapper");

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
				if (v instanceof Date && (((String) colId).equals("endDate") || ((String) colId).equals("startDate"))) {
					return df_long.format((Date) v);
				} else if (property.getType() == Boolean.class) {
					if (property.getValue() == null) {
						return "";
					} else {
						if ((Boolean) property.getValue()) {
							return "Yes";
						} else {
							return "";
						}
					}
				}
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

				switch (((DecommItem) itemId).getStatus()) {
				case -1:
				case DecommItem.STATUS_REQUESTED:
				case DecommItem.STATUS_PENDING_APPROVAL:
				case DecommItem.STATUS_ERROR: {
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
							DecommItem ur = (DecommItem) event.getButton().getData();
							if (properties.getProperty("idm.shift.to.delete", "false").equals("true") && event.isShiftKey()) {
								dao.delete(ur);
							} else {
								ur.setStatus(DecommItem.STATUS_CANCELLED);
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
					b.addClickListener(e -> changeApprover((DecommItem) e.getButton().getData()));
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
				label.setValue(((DecommItem) itemId).getStatus() > -1 ? DecommItem.STATUS_NAMES[((DecommItem) itemId).getStatus()] : "");
				return label;
			}
		});

		table.addGeneratedColumn("gen-attachment", new FilterTable.ColumnGenerator() {
			@Override
			public com.vaadin.ui.Component generateCell(CustomTable source, final Object itemId, Object columnId) {
				DecommItem fp = (DecommItem) itemId;
				
				// b.setData(itemId);
				// b.addClickListener(new Button.ClickListener() {
				// @Override
				// public void buttonClick(ClickEvent event) {
				// FilePointer fp = (FilePointer)event.getButton().getData();
				//
				// }
				// });
				if (fp.getAttachment() != null && !fp.getAttachment().isEmpty()) {
					Button b = new Button();
					b.setStyleName(BaseTheme.BUTTON_LINK);
					b.setDescription("Download Attachement");
					b.setIcon(new ThemeResource("icons/cloud-download.svg"));
					b.setWidth("14px");
					b.setHeight("20px");
					
					Resource myResource = new FileResource(new File(_locator.locateFile(fp.getAttachment())));
					FileDownloader fileDownloader = new FileDownloader(myResource);
					fileDownloader.extend(b);
					return b;
				}
				return new Label();
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

	private void changeApprover(DecommItem ur) {
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

		l.addComponent(HaloFactory.label("", DecommItem.ACTION_NAMES[ur.getAction()], false, "Requested Action"));
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
				ur.setStatus(DecommItem.STATUS_REQUESTED);
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
		List<DecommItem> list = DecommItem.getEntriesByDate(res);
		// total_requests = 0l;
		// for (FilePointer fp : list) {
		// total_space += fp.getFilesize();
		// }
		// izUsedSizeLabel.setValue(Utils.humanReadableByteCount(total_space,
		// true));

		/*
		 * private String fname; private String sname; private String
		 * employeeID; private String exEmail; private String intEmail; private
		 * String phone; private String group; private String ntlogin; private
		 * String environment; private Date startDate; private Date endDate;
		 */
		table.setContainerDataSource(new BeanItemContainer<>(DecommItem.class, list));
		table.setVisibleColumns(new Object[] { "id", "gen-attachment", "created", "checkGroup", "checkSGDIcons", "checkVDTs", "checkEUAAs", "checkBRMs", "iz-status", "username", "ggroup", "environment", "endDate", "approver", "msg", "iz-action" });
		table.setColumnHeaders(new String[] { "ID", "File", "Requested", "Group", "SGD", "VDTs", "EUAAs", "BRMs", "Status", "Username", "Group", "Environment", "Decomm Date", "BRM", "Message", "Action" });
		table.setColumnWidth("iz-select", 40);
		table.setColumnWidth("gen-attachment", 70);
	}

	@Override
	public void enter(ViewChangeEvent event) {
		user = ldap.getUser(((HaloUI) UI.getCurrent()).getUsername());
		build();
	}
	
	
	public class FileReceiver implements Receiver {

		
		
		private String fileName;
		private String realFileName;
		private String mtype;

		private long total;
		private boolean sleep;

		public OutputStream receiveUpload(String filename, String MIMEType) {
			FileOutputStream fos = null; // Output stream to write to
			// File file = new File(Constants.inspection_zone + filename);
			this.fileName = filename;
			this.realFileName =  new Date().getTime() + "-" + filename;
			this.total = 0l;
			File file = new File(_locator.placeFile(realFileName));
			try {
				mtype = MIMEType;
				// Open the file for writing.
				fos = new FileOutputStream(file) {
					@Override
					public void write(int b) throws IOException {
						total++;
						if (sleep && total % 1000 == 0) {
							try {
								Thread.sleep(100);
							} catch (InterruptedException e) {
								// TODO Auto-generated catch block
								e.printStackTrace();
							}
						}
					}

					@Override
					public void write(byte[] b) throws IOException {
						super.write(b);
						total += b.length;
						if (sleep && total % 1000 == 0) {
							try {
								Thread.sleep(100);
							} catch (InterruptedException e) {
								// TODO Auto-generated catch block
								e.printStackTrace();
							}
						}
					}

					@Override
					public void write(byte[] b, int off, int len) throws IOException {
						super.write(b, off, len);
						total += b.length;
						if (sleep && total % 1000 == 0) {
							try {
								Thread.sleep(100);
							} catch (InterruptedException e) {
								// TODO Auto-generated catch block
								e.printStackTrace();
							}
						}
					}
				};
			} catch (final java.io.FileNotFoundException e) {
				// Error while opening the file. Not reported here.
				e.printStackTrace();
				return null;
			}
			return fos;
		}

		public String getFileName() {
			return fileName;
		}

		public String getMimeType() {
			return mtype;
		}

		public void setSlow(boolean value) {
			sleep = value;
		}

		public long getTotal() {
			return total;
		}

		public String getRealFileName() {
			return realFileName;
		}

		public void setRealFileName(String realFileName) {
			this.realFileName = realFileName;
		}

	}
	
	private void setupUploader() {
		
		upload.addStartedListener(new Upload.StartedListener() {
			public void uploadStarted(StartedEvent event) {
				// this method gets called immediatedly after upload is
				// started

				fileName.setValue(event.getFilename());

				pi.setValue(0f);
				pi.setVisible(true);
				UI.getCurrent().setPollInterval(100); // hit server
														// frequantly
				mime.setValue(event.getMIMEType());
				mime.setStyleName("mime-allowed"); // to get
				textualProgress.setVisible(true);
				// updates to client
				state.setValue("Uploading");

				cancelProcessing.setEnabled(true);

			}
		});

		upload.addProgressListener(new Upload.ProgressListener() {
			public void updateProgress(long readBytes, long contentLength) {
				// this method gets called several times during the update
				pi.setValue(new Float(readBytes / (float) contentLength));
				textualProgress.setValue("Processed " + readBytes + " bytes of " + contentLength);
				result.setValue("" + contentLength);
			}

		});

		upload.addSucceededListener(new Upload.SucceededListener() {
			public void uploadSucceeded(SucceededEvent event) {
				result.setValue(counter.getTotal() + " (total)");
				mime.setValue(counter.getMimeType());
				
			//	updateTable(table);
				UI.getCurrent().setPollInterval(-1);
			}
		});

		upload.addFailedListener(new Upload.FailedListener() {
			public void uploadFailed(FailedEvent event) {
				result.setValue(counter.getTotal() + " (upload interrupted at " + Math.round(100 * (Float) pi.getValue()) + "%)");
				UI.getCurrent().setPollInterval(-1);
			}
		});

		upload.addFinishedListener(new Upload.FinishedListener() {
			public void uploadFinished(FinishedEvent event) {
				state.setValue("Idle");
				pi.setVisible(false);
				textualProgress.setVisible(false);
				cancelProcessing.setEnabled(false);
				UI.getCurrent().setPollInterval(-1);
			}
		});
	}

}
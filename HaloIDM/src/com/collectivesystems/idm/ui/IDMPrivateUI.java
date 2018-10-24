package com.collectivesystems.idm.ui;

import java.util.LinkedHashMap;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.context.annotation.Scope;
import org.vaadin.leif.zxcvbn.ZxcvbnIndicator;
import org.vaadin7.console.Console;

import com.collectivesystems.core.annotations.HaloAuthority;
import com.collectivesystems.core.beans.spring.User;
import com.collectivesystems.core.services.service.PropertiesService;
import com.collectivesystems.core.services.service.SpringSecurityHelperService;
import com.collectivesystems.core.ui.components.HaloMenuBar;
import com.collectivesystems.core.ui.providers.HaloPrivateUI;
import com.collectivesystems.core.vaadin.BadgeView;
import com.collectivesystems.idm.beans.LDAPUser;
import com.collectivesystems.idm.services.service.JobProcessor;
import com.collectivesystems.idm.services.service.LDAPService;
import com.vaadin.annotations.Theme;
import com.vaadin.annotations.Title;
import com.vaadin.data.Property.ValueChangeEvent;
import com.vaadin.data.Property.ValueChangeListener;
import com.vaadin.data.fieldgroup.BeanFieldGroup;
import com.vaadin.data.fieldgroup.FieldGroup.CommitException;
import com.vaadin.navigator.View;
import com.vaadin.server.Page;
import com.vaadin.ui.Button;
import com.vaadin.ui.Button.ClickEvent;
import com.vaadin.ui.Button.ClickListener;
import com.vaadin.ui.CssLayout;
import com.vaadin.ui.FormLayout;
import com.vaadin.ui.HorizontalLayout;
import com.vaadin.ui.Label;
import com.vaadin.ui.NativeButton;
import com.vaadin.ui.NativeSelect;
import com.vaadin.ui.Notification;
import com.vaadin.ui.PasswordField;
import com.vaadin.ui.UI;
import com.vaadin.ui.VerticalLayout;
import com.vaadin.ui.Window;
import com.vaadin.ui.Window.CloseEvent;
import com.vaadin.ui.themes.BaseTheme;
import com.vaadin.ui.themes.ValoTheme;

import nz.co.exium.NavDrawer;
import ru.xpoft.vaadin.SpringApplicationContext;
import ru.xpoft.vaadin.VaadinView;


@SuppressWarnings("serial")
@Scope("prototype")
@Theme("dashboard")
@Title("Halo: Ident. Dashboard")
@org.springframework.stereotype.Component
public class IDMPrivateUI extends HaloPrivateUI { 
	private static Logger log = LoggerFactory.getLogger(IDMPrivateUI.class);
	
	@Autowired
	LDAPService ldap;
	
	@Autowired
	JobProcessor jobs;
	
	 private static LinkedHashMap<String, String> themeVariants = new LinkedHashMap<String, String>();
	 static {
	        themeVariants.put(ValoTheme.THEME_NAME, "Valo");
	        themeVariants.put("dashboard", "Ident.");
	        themeVariants.put("midsummer-night", "Midsummer Night");
	        themeVariants.put("tests-valo-blueprint", "Blueprint");
	        themeVariants.put("tests-valo-dark", "Dark");
	        themeVariants.put("tests-valo-facebook", "Facebook");
	        themeVariants.put("tests-valo-flatdark", "Flat dark");
	        themeVariants.put("tests-valo-flat", "Flat");
	        themeVariants.put("tests-valo-light", "Light");
	        themeVariants.put("tests-valo-metro", "Metro");
	        themeVariants.put("tests-valo-reindeer", "Migrate Reindeer");
	}
	
	@Override
	protected void buildMainView() {
		final User user = this.getUser();

	//	final CssLayout layout = new CssLayout();
		final HaloMenuBar menubar = new HaloMenuBar(user, menu, "<span>Halo Ident.</span> Dashboard");
		menubar.addAccountSettingsListener(new Runnable() {
			@Override
			public void run() {
				LDAPUser luser = LDAPService.me.getUser(user.getUsername());
				Window w = new Window();
				
				UI.getCurrent().addWindow(w);
				w.setVisible(true);
				w.addCloseListener(new Window.CloseListener() {
					@Override
					public void windowClose(CloseEvent e) {
						
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
				l.setCaption("Your Account <span></span>");
				l.setCaptionAsHtml(true);
				l.setSpacing(false);

				Label username = new Label(luser.getUid());
				username.setCaption("Username");
				l.addComponent(username);
					
					
				String supplier = luser.getMatcherAttr(); // == null || new_user.getMatcherAttr().equals("") ? user.getMatcherAttr() : new_user.getMatcherAttr();
				Label supplier_label = new Label(supplier);
				supplier_label.setCaption("Supplier(s)");
				l.addComponent(supplier_label);
				
				
				
			
				
				final BeanFieldGroup<LDAPUser> binder = new BeanFieldGroup<>(LDAPUser.class);
				binder.setItemDataSource(luser);
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
		
				l.addComponent(binder.buildAndBind("Email", "email"));
				l.addComponent(binder.buildAndBind("Phone Number", "phoneNo"));
				
				l.addComponent(new Label(""));
				
				PasswordField oldpass = new PasswordField();
				oldpass.setCaption("Current Password");
				l.addComponent(oldpass);
				
				PasswordField pass1 = new PasswordField();
				
				pass1.setCaption("New Password");
				PasswordField pass2 = new PasswordField();
				pass2.setCaption("Repeat New Password");
				ZxcvbnIndicator zxcvbn = new ZxcvbnIndicator();
				zxcvbn.setTargetField(pass1);
				
				l.addComponent(pass1);
				l.addComponent(zxcvbn);
				l.addComponent(pass2);
				final Label notice = new Label();
				l.addComponent(notice);
				
				l.addComponent(new Label(""));
				
				final NativeSelect ns = new NativeSelect();
				ns.setNullSelectionAllowed(false);
				ns.setId("themeSelect");
				ns.addContainerProperty("caption", String.class, "");
				ns.setItemCaptionPropertyId("caption");
				for (String identifier : themeVariants.keySet()) {
					ns.addItem(identifier).getItemProperty("caption").setValue(themeVariants.get(identifier));
				}

				ns.setValue(ValoTheme.THEME_NAME);
				ns.addValueChangeListener(new ValueChangeListener() {
					@Override
					public void valueChange(ValueChangeEvent event) {
						setTheme((String) ns.getValue());
					}
				});
				l.addComponent(ns);
				l.addComponent(new Label(""));
				
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
								if (!pass1.getValue().trim().isEmpty()) {
									if (!pass1.getValue().trim().equals(pass2.getValue().trim())) {
										notice.setValue("Passwords must match");
										return;
									} else {
										if (zxcvbn.getPasswordScore() < 3) {
											notice.setValue("Your new password is too weak.");
											return;
										}
										try {
											LDAPService.me.changePassword(luser, oldpass.getValue().trim(), pass1.getValue().trim());
										} catch (Exception e) {
											notice.setValue(e.toString());
											e.printStackTrace();
											return;
										}
									}
								}
								binder.commit();								
								LDAPUser user_bean = binder.getItemDataSource().getBean();
							
								if (JobProcessor.me.requestUpdateApprover(luser.getUid(), user_bean, true) ) {
									Notification.show("Halo Ident.", "User Account (" + user_bean.getUid() + ") Updated", Notification.Type.TRAY_NOTIFICATION);
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
					l.addComponent(actions);
					
					layout.addComponent(l);
				}
			
				
			});
	
		content.addStyleName("view-content");
		content.setSizeFull();
		
//		VerticalLayout v = new VerticalLayout();
//		v.setSpacing(true);
//		v.setSizeFull();
//		v.addComponent(content);
//		
//		 CssLayout innerLayout = new CssLayout();
//	     innerLayout.setHeight(256, Unit.PIXELS);
//	innerLayout.setWidth(1000, Unit.PIXELS);
//	innerLayout.setStyleName("console-wrapper");
//		
//		final Console console = new Console();
//        console.setStyleName("console-console");
//        // Size, greeting and other configuration
//        console.setPs("C:\\>");
////        console.setCols(80);
////        console.setRows(20);
//        console.setMaxBufferSize(20);
//        console.setGreeting("");
//        console.reset();
//        console.focus();
//        console.setSizeFull();
//        
//        innerLayout.addComponent(console);
//		
//		final NavDrawer drawer = new NavDrawer(innerLayout);
//		v.addComponent(drawer);
//		
//		
//	//	v.setExpandRatio(content, 1.0f);
//		
//		menubar.addApplicationSettingsListener(new Runnable() {
//			@Override
//			public void run() {
//				drawer.toggle();
//				
//			} });
//	//	layout.setStyleName("a-view");
//	//	layout.addComponent(content);
		
		root.addComponent(menubar);
		root.addComponent(content);
		root.setStyleName("halo-layout");
		root.setExpandRatio(content, 1);

//		HaloSideBar sidebar = new HaloSideBar(true, content);
		
		menu.removeAllComponents();
		 log.error("Doing the beans");
		 String[] beansName = SpringApplicationContext.getApplicationContext().getBeanDefinitionNames();
         for (String beanName : beansName)
         {
        	
             Class<?> beanClass = (Class<?>) SpringApplicationContext.getApplicationContext().getType(beanName);

             // Check for a valid bean class because "abstract" beans may not have a bean class defined.
             if (beanClass != null && beanClass.isAnnotationPresent(VaadinView.class) && View.class.isAssignableFrom(beanClass)) {
            	 if (beanClass.isAnnotationPresent(HaloAuthority.class)) {            		
             		String ROLE = ((HaloAuthority)beanClass.getAnnotation(HaloAuthority.class)).authority();
            	 
             		VaadinView vaadinView = (VaadinView) beanClass.getAnnotation(VaadinView.class);
                    final String viewName = vaadinView.value();
                    
                   // log.info("Found view: {}/{}", viewName, ROLE);
                    
             		if (SpringSecurityHelperService.hasRole(ROLE)) {
             			String view = viewName.substring(1).replace('/', ' ');             			
             			log.info("View {} [{}] Authorised", viewName, beanName);
             			
             			String badges = "";
             			if (BadgeView.class.isAssignableFrom(beanClass)) {
             				BadgeView bv = (BadgeView)SpringApplicationContext.getApplicationContext().getBean(beanName);
             				if (bv.badgeNumber() > 0) { badges = "<span class=\"badge\">" + bv.badgeNumber() + "</span>"; };
             			}
             			Button b = new NativeButton(view.substring(0, 1).toUpperCase() + view.substring(1) + badges);
             			b.setHtmlContentAllowed(true);
             			b.setData(beanName);
            			b.addStyleName("icon-" + view);
            			b.addClickListener(new ClickListener() {
            				@Override
            				public void buttonClick(ClickEvent event) {
            					clearMenuSelection();
            					event.getButton().addStyleName("selected");
//            					if (!nav.getState().equals(viewName)) {
//            						nav.navigateTo(viewName);
//            					}
            					try { 
            						nav.navigateTo(viewName);
            					} catch (org.springframework.security.access.AccessDeniedException se) {
            						nav.navigateTo("access-denied");
            					} catch (Exception e) {
            						log.error("Navigate to: {} ", viewName, e);
            					}
            				}
            			});
            			
            			
            			menu.addComponent(b);       			
            			viewNameToMenuButton.put(viewName, b);
            			
             		}
            	 }
             }
         }

		
		

		menu.addStyleName("menu");
		menu.setHeight("100%");

		//viewNameToMenuButton.get("/dashboard").setHtmlContentAllowed(true);
		//viewNameToMenuButton.get("/dashboard").setCaption("Dashboard<span class=\"badge\">2</span>");
		
		
		String f = Page.getCurrent().getUriFragment();
		if (f != null && f.startsWith("!")) { f = f.substring(1); }
		if (f == null || f.equals("") || f.equals("/")) {
			boolean view_found = false;
			String initial_views[] = PropertiesService.getPropertyStatic("halo.default.view", "").split(",");
			log.info(PropertiesService.getPropertyStatic("halo.default.view", ""));
			for (String view : initial_views) {
				log.debug(Thread.currentThread().toString() + " Trying View: " + view);
				Button b = viewNameToMenuButton.get(view);
				if (b != null) {
					b.addStyleName("selected");
					nav.setState(view);
					view_found = true;
					break;
				} else {
					log.debug(view + "not avaiable for " + user.getUsername());
				}				
			}
			if (!view_found) {
				String view = (String) viewNameToMenuButton.keySet().toArray()[0];
				log.debug(Thread.currentThread().toString() + " Navigating to 1st View: " + view);
				Button b = viewNameToMenuButton.get(view);
				b.addStyleName("selected");
				nav.setState(view);
			}

		} else {
			if (f.contains("/")) {
				viewNameToMenuButton.get(f.subSequence(0, f.indexOf("/"))).addStyleName("selected");
			} else {
			//	for (String key : viewNameToMenuButton.keySet()) { log.error(key); }
				viewNameToMenuButton.get(f).addStyleName("selected");
			}
		}

	}
	
	@Override
	public String getUsername() {
		return person.getUsername();
	}
	
	public void updateBadges() {
		for (Button b : viewNameToMenuButton.values()) {
			String beanName = (String)b.getData();
			Class<?> beanClass = (Class<?>) SpringApplicationContext.getApplicationContext().getType(beanName);
			String badges = "";
			if (BadgeView.class.isAssignableFrom(beanClass)) {
  				BadgeView bv = (BadgeView)SpringApplicationContext.getApplicationContext().getBean(beanName);
  				if (bv.badgeNumber() > 0) { badges = "<span class=\"badge\">" + bv.badgeNumber() + "</span>"; };

  			}
			VaadinView vaadinView = (VaadinView) beanClass.getAnnotation(VaadinView.class);
            final String viewName = vaadinView.value();
            String view = viewName.substring(1).replace('/', ' ');         
			b.setCaption(view.substring(0, 1).toUpperCase() + view.substring(1) + badges);
		}
		
	}
	
}

package io.echoplex.web;

import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;

import org.pac4j.core.config.Config;
import org.pac4j.core.context.J2EContext;
import org.pac4j.core.credentials.UsernamePasswordCredentials;
import org.pac4j.core.credentials.authenticator.Authenticator;
import org.pac4j.core.profile.CommonProfile;
import org.pac4j.springframework.security.authentication.Pac4jAuthentication;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.context.SecurityContextHolder;
import org.thymeleaf.context.Context;
import org.vaadin.leif.zxcvbn.ZxcvbnIndicator;

import com.vaadin.annotations.Theme;
import com.vaadin.annotations.Widgetset;
import com.vaadin.data.Binder;
import com.vaadin.data.validator.EmailValidator;
import com.vaadin.icons.VaadinIcons;
import com.vaadin.navigator.Navigator;
import com.vaadin.navigator.ViewChangeListener;
import com.vaadin.server.Page;
import com.vaadin.server.ThemeResource;
import com.vaadin.server.VaadinRequest;
import com.vaadin.server.VaadinService;
import com.vaadin.server.VaadinServletRequest;
import com.vaadin.server.VaadinServletResponse;
import com.vaadin.spring.annotation.SpringUI;
import com.vaadin.spring.navigator.SpringViewProvider;
import com.vaadin.ui.Alignment;
import com.vaadin.ui.Button;
import com.vaadin.ui.Button.ClickEvent;
import com.vaadin.ui.Component;
import com.vaadin.ui.CssLayout;
import com.vaadin.ui.FormLayout;
import com.vaadin.ui.Label;
import com.vaadin.ui.Notification;
import com.vaadin.ui.PasswordField;
import com.vaadin.ui.TextField;
import com.vaadin.ui.UI;
import com.vaadin.ui.VerticalLayout;
import com.vaadin.ui.Window;
import com.vaadin.ui.themes.ValoTheme;
import com.wcs.wcslib.vaadin.widget.recaptcha.ReCaptcha;
import com.wcs.wcslib.vaadin.widget.recaptcha.shared.ReCaptchaOptions;

import de.steinwedel.messagebox.MessageBox;
import io.echoplex.web.neo4j.domain.EchoUser;
import io.echoplex.web.neo4j.domain.PWRequest;
import io.echoplex.web.neo4j.domain.PWRequest.Type;
import io.echoplex.web.neo4j.repository.PWRepository;
import io.echoplex.web.neo4j.repository.UserRepository;
import io.echoplex.web.service.PropertiesService;
import io.echoplex.web.ui.LoggedInView;
import io.echoplex.web.ui.components.HaloMap;
import io.echoplex.web.ui.components.StyledLabel;
import io.echoplex.web.utils.Email;
import io.echoplex.web.utils.EmailStatus;
import io.echoplex.web.utils.HaloMenuBuilder;
import kaesdingeling.hybridmenu.HybridMenu;
import kaesdingeling.hybridmenu.builder.top.TopMenuButtonBuilder;
import kaesdingeling.hybridmenu.builder.top.TopMenuLabelBuilder;
import kaesdingeling.hybridmenu.builder.top.TopMenuSubContentBuilder;
import kaesdingeling.hybridmenu.components.NotificationCenter;
import kaesdingeling.hybridmenu.data.DesignItem;
import kaesdingeling.hybridmenu.data.MenuConfig;
import kaesdingeling.hybridmenu.data.enums.EMenuComponents;
import kaesdingeling.hybridmenu.data.enums.EMenuStyle;
import kaesdingeling.hybridmenu.data.top.TopMenuButton;
import kaesdingeling.hybridmenu.data.top.TopMenuLabel;
import kaesdingeling.hybridmenu.data.top.TopMenuSubContent;

@SpringBootApplication
public class WebApplication {
	Logger log = LoggerFactory.getLogger(WebApplication.class);

	public static void main(String[] args) {
		SpringApplication.run(WebApplication.class, args);
	}

	@SpringUI(path = "/")
	@Theme("echoplex")
	public class EchoPlexUI extends UI {
		private static final long serialVersionUID = 86672721177459023L;

		@Override
		protected void init(VaadinRequest request) {
		}

	}

	@SpringUI(path = "/echo")
	@Theme("echoplex")
	public class EchoPlexUI2 extends UI {
		private static final long serialVersionUID = 86672721177459023L;

		CssLayout nav_content = new CssLayout();

		@Autowired
		public void configureNavigator(SpringViewProvider viewProvider) {
			
//			nav_content.setStyleName("halo-content-ation");
//			nav_content.setSizeFull();
			Navigator navigator = new Navigator(this, nav_content);
			navigator.addProvider(viewProvider);
			for (String name : viewProvider.getViewNamesForCurrentUI()) {
				try {
				 log.error(name + "["  + "]");
				} catch (Exception e ) { log.error("",e); }
			}

			navigator.addViewChangeListener(new ViewChangeListener() {
				private static final long serialVersionUID = 1L;

				@Override
				public boolean beforeViewChange(ViewChangeEvent event) {
					log.error("View Change Event [{}]", event.getViewName());
					return true;
				}
			});// listener -> { log.error(""); });
		}

		@Override
		protected void init(VaadinRequest request) {
			UI.getCurrent().getNavigator().navigateTo("echo");

			Authentication auth = SecurityContextHolder.getContext().getAuthentication();
			if (auth != null && auth instanceof Pac4jAuthentication) {
				Pac4jAuthentication token = (Pac4jAuthentication) auth;
				CommonProfile profile = token.getProfile();
				
				// Get repo user from somewhere...  added to credentials as field?
				

				NotificationCenter notiCenter = new NotificationCenter(10);
				MenuConfig menuConfig = new MenuConfig();

				menuConfig.setDesignItem(DesignItem.getDarkDesign());

				HybridMenu hybridMenu = HaloMenuBuilder.get().setContent(nav_content).setMenuComponent(EMenuComponents.ONLY_TOP).setConfig(menuConfig).withNotificationCenter(notiCenter)
						.withNavigator(false).build();

				TopMenuButtonBuilder.get().setCaption("Logout").setHideCaption(false).setIcon(VaadinIcons.SIGN_OUT).setAlignment(Alignment.MIDDLE_RIGHT)
						.addClickListener(event -> MessageBox.createQuestion().withCaption("Echoplex").withMessage("Are you sure you want to logout?").withNoButton().withYesButton(new Runnable() {
							@Override
							public void run() {
								Page.getCurrent().setLocation("/logout");
							}
						}).open()).build(hybridMenu);

				TopMenuButtonBuilder.get().setCaption("Home").setHideCaption(true).addClickListener(event -> UI.getCurrent().getNavigator().navigateTo("echo")).setIcon(VaadinIcons.HOME).setAlignment(Alignment.MIDDLE_RIGHT).build(hybridMenu);

				TopMenuSubContent userAccountMenu = TopMenuSubContentBuilder.get().setButtonCaption(profile.getDisplayName()).setButtonIcon(VaadinIcons.USER) // new
																																								// ThemeResource("images/profilDummy.jpg"))
						.addButtonStyleName(EMenuStyle.ICON_RIGHT).addButtonStyleName(EMenuStyle.PROFILVIEW).setAlignment(Alignment.MIDDLE_RIGHT)

						.build(hybridMenu);

				userAccountMenu.addLabel("Account");
				userAccountMenu.addHr();
				userAccountMenu.addButton("Link Twitter Account").addClickListener(event -> Page.getCurrent().setLocation("/twitter"));
				userAccountMenu.addHr();
				userAccountMenu.addButton("Test 2");

				TopMenuButton notiButton = TopMenuButtonBuilder.get().setIcon(VaadinIcons.BELL_O).setAlignment(Alignment.MIDDLE_RIGHT).build(hybridMenu);

				notiCenter.setNotificationButton(notiButton);

				TopMenuLabel label = TopMenuLabelBuilder.get().setCaption("").setIcon(new ThemeResource("img/nrme32.png")).build(hybridMenu);

				// MenuButton homeButton = LeftMenuButtonBuilder.get()
				// .withCaption("Home")
				// .withIcon(VaadinIcons.HOME)
				// .withNavigateTo(DefaultView.class)
				// .build();
				//
				// hybridMenu.addLeftMenuButton(homeButton);

				// layout.addComponent(hybridMenu);
				this.setContent(hybridMenu);
				
				Window w = (Window) VaadinService.getCurrentRequest().getWrappedSession().getAttribute("any-windows");
				if (w != null) {
					this.addWindow(w);
				}
				

			}

		}

	}

	@SpringUI(path = "/login")
	@Theme("echoplex")
	@Widgetset("AppWidgetset")
	public class LoginUI extends UI {
		private static final long serialVersionUID = 86672721177459023L;
		public static final String PASSWORD_PARAM = "password";
		public static final String NAME_PARAM = "username";
		// public static final String LOGIN_URL = "/login?callback_client=FormClient";
		public static final String LOGIN_URL = "/callback";
		public static final String CALLBACK = "client_name";
		public static final String CALLBACK_CLIENT = "FormClient";
		public static final String CODE_PARAM = "code";

		@Autowired
		Config config;

		@Autowired
		UserRepository user_repo;

		@Autowired
		Authenticator<UsernamePasswordCredentials> authenticator;

		@Autowired
		PWRepository pw_repo;

		@Autowired
		Email emailSender;

		@Autowired
		PropertiesService properties;

		Window w = new Window();
		FormLayout loginLayout = new FormLayout();

		@Override
		protected void init(VaadinRequest request) {

			final CssLayout layout = new CssLayout();
			layout.setSizeFull();
			layout.setStyleName("halo-layout");
			this.setContent(layout);

			HaloMap map = new HaloMap("AIzaSyD2w-UXPPQrHSgPKhLNgqPxMVekuWb89cA", null, "english");
			layout.addComponent(map);
			layout.addComponent(new StyledLabel("echo-logo", ""));

			w.setModal(true);
			w.setWidth("566px");
			w.setHeight("700px");
			w.setClosable(false);
			w.setResizable(false);

			UI.getCurrent().addWindow(w);
			w.setVisible(true);

			// for (Object o : request.getParameterMap().keySet()) {
			// log.error(o + " -> " + request.getParameterMap().get(o).toString());
			// }

			if (request.getParameter(NAME_PARAM) != null && request.getParameter(CODE_PARAM) != null) {
				String email = request.getParameter(NAME_PARAM);
				String code = request.getParameter(CODE_PARAM);
				int x = code.charAt(0) - '0';

				PWRequest existing_request = pw_repo.findByUserIdAndType(email, Type.fromInt(x));
				log.error(existing_request.toString());
				log.error(code + " -> " + existing_request.getValidationCode());
				if (existing_request != null && code.equals(existing_request.getValidationCode())) {
					switch (existing_request.getType()) {
					case SIGN_UP:
						// Convert to a real user
						EchoUser e = new EchoUser(existing_request);
						user_repo.save(e);
						pw_repo.delete(existing_request);
						w.setContent(signUpSuccess());
						return;
					case PW_RESET:
						w.setContent(passwordEntry(existing_request));
						return;
					default:
						log.error("" + existing_request.getType());
						break;
					}
				}
			}

			w.setContent(loginForm());

		}

		private Component passwordEntry(PWRequest bean) {
			FormLayout layout = new FormLayout();
			layout.setStyleName("login-form");
			layout.setMargin(true);
			layout.setSizeUndefined();

			Binder<PWRequest> binder = new Binder<>();

			final TextField sUsernameField = new TextField();
			// sUsernameField.setPlaceholder("email address");
			sUsernameField.setValue(bean.getUserId());
			sUsernameField.setEnabled(false);

			final PasswordField sPasswordField = new PasswordField();
			sPasswordField.setPlaceholder("password");
			binder.bind(sPasswordField, PWRequest::getPassword, PWRequest::setPassword);

			final PasswordField sPasswordField2 = new PasswordField();
			sPasswordField2.setPlaceholder("repeat password");

			VerticalLayout v = new VerticalLayout();
			v.setStyleName("verti-stuff");
			v.setMargin(false);
			v.setSpacing(false);
			ZxcvbnIndicator meter = new ZxcvbnIndicator();
			meter.setTargetField(sPasswordField);
			meter.setStyleName("password-meter");
			meter.setWidth("24em");
			meter.setHeight("10px");

			StyledLabel mismatch = new StyledLabel("mismatch", "Passwords do not match");
			mismatch.setVisible(false);

			final Button go = new Button("Reset Password", event -> {
				try {
					binder.writeBean(bean);

					EchoUser e = user_repo.findByUserId(bean.getUserId());
					e.setPassword(bean.getPassword());
					user_repo.save(e);
					pw_repo.delete(bean);
					w.setContent(signUpSuccess());

					Context context = new Context();
					EmailStatus emailStatus = emailSender.sendHtmlWithTemplate(bean.getUserId(), "Your Password has been Reset", "pwreset_complete.html", context);
					// TODO: error check email

				} catch (Exception e) {
					Notification.show("Person could not be saved, please check error messages for each field.");
					log.error("", e);
				}
			});

			sPasswordField.addValueChangeListener(event3 -> {
				if (!sPasswordField2.getValue().trim().isEmpty()) {
					mismatch.setVisible(!sPasswordField2.getValue().equals(sPasswordField.getValue()));
					go.setEnabled(!mismatch.isVisible());
				}
			});
			sPasswordField2.addValueChangeListener(event3 -> {
				mismatch.setVisible(!sPasswordField2.getValue().equals(sPasswordField.getValue()));
				go.setEnabled(!mismatch.isVisible());
			});

			layout.addComponent(new StyledLabel("forgot-title", "Reset Password"));
			layout.addComponent(sUsernameField);
			layout.addComponent(sPasswordField);
			layout.addComponent(sPasswordField2);
			v.addComponent(meter);
			v.addComponent(mismatch);
			layout.addComponent(v);
			layout.addComponent(go);
			layout.addComponent(new StyledLabel("or-label", "OR"));
			layout.addComponent(new Button("Cancel", event2 -> w.setContent(loginLayout)));

			return layout;

		}

		private Component signUpSuccess() {
			FormLayout layout = new FormLayout();
			layout.setStyleName("login-form");
			layout.setMargin(true);
			layout.setSizeUndefined();
			layout.addComponent(new StyledLabel("signup-success", "All Done"));
			layout.addComponent(new StyledLabel("signup-success-tick",
					"<svg class=\"checkmark\" xmlns=\"http://www.w3.org/2000/svg\" viewBox=\"0 0 52 52\">" + "<circle class=\"checkmark__circle\" cx=\"26\" cy=\"26\" r=\"25\" fill=\"none\"/>"
							+ "<path class=\"checkmark__check\" fill=\"none\" d=\"M14.1 27.2l7.1 7.2 16.7-16.8\"/>" + "</svg>",
					true));
			Button submit = new Button("Sign In to Continue", event -> {
				Page.getCurrent().setLocation("/echo");
			});
			submit.setStyleName("sign-in");
			layout.addComponent(submit);
			return layout;

		}

		private Component loginForm() {

			loginLayout.setStyleName("login-form");
			loginLayout.setMargin(true);
			loginLayout.setSizeUndefined();

			loginLayout.addComponent(new StyledLabel("sign-in", "Sign In"));
			final TextField usernameField = new TextField();
			usernameField.setValue("stuart@collectivesystems.com");
			usernameField.setPlaceholder("email address");

			final PasswordField passwordField = new PasswordField();
			passwordField.setPlaceholder("password");
			passwordField.setValue("12345");

			final Button forgot = new Button("Forgot?");
			forgot.setStyleName(ValoTheme.BUTTON_LINK);
			forgot.addStyleName("forgot-link");

			Button submit = new Button("Sign In");
			submit.setStyleName("sign-in");

			Button facebook = new Button("Sign In with Facebook", event -> getUI().getPage().setLocation("/facebook"));
			facebook.setStyleName("facebook");
			Button twitter = new Button("Sign In with Twitter", event -> getUI().getPage().setLocation("/twitter"));
			twitter.setStyleName("twitter");
			Button google = new Button("Sign In with Google", event -> getUI().getPage().setLocation("/google"));
			google.setStyleName("google");
			Button sign_up = new Button("Sign Up");

			loginLayout.addComponent(usernameField);
			loginLayout.addComponent(passwordField);
			loginLayout.addComponent(forgot);

			final Label error = new StyledLabel("error", "");
			loginLayout.addComponent(error);

			loginLayout.addComponent(submit);
			loginLayout.addComponent(new StyledLabel("or-label", "OR"));
			loginLayout.addComponent(facebook);
			loginLayout.addComponent(twitter);
			loginLayout.addComponent(google);
			loginLayout.addComponent(new StyledLabel("or-label", "OR"));
			loginLayout.addComponent(sign_up);

			submit.addClickListener(new Button.ClickListener() {
				private static final long serialVersionUID = 723793187222246208L;

				@Override
				public void buttonClick(ClickEvent event) {
					log.error(usernameField.getValue());
					HttpServletRequest req = ((VaadinServletRequest) VaadinService.getCurrentRequest()).getHttpServletRequest();
					HttpServletResponse resp = ((VaadinServletResponse) VaadinService.getCurrentResponse()).getHttpServletResponse();
					final J2EContext context = new J2EContext((HttpServletRequest) req, (HttpServletResponse) resp, config.getSessionStore());
					try {
						UsernamePasswordCredentials credentials = new UsernamePasswordCredentials(usernameField.getValue(), passwordField.getValue(), "formClient");
						authenticator.validate(credentials, context);
						// Client client = config.getClients().findClient("FormClient");
						// FormSenderBuilder.create().withUI(getUI()).withAction(LOGIN_URL).withTarget("_self").withMethod(Method.POST)
						// .withValue(NAME_PARAM, usernameField.getValue())
						// .withValue(PASSWORD_PARAM, passwordField.getValue())
						// .withValue(CALLBACK, CALLBACK_CLIENT)
						// .submit();

						getUI().getPage().setLocation("/echo");

					} catch (Exception e) {
						error.setValue("Invalid Credentials");
						log.error(e.getMessage(), e);
					}
				}

			});

			sign_up.addClickListener(event -> w.setContent(signupForm()));
			forgot.addClickListener(event2 -> w.setContent(forgotForm()));

			return loginLayout;
		}

		private Component forgotForm() {

			VerticalLayout verify_code_layout = new VerticalLayout();
			verify_code_layout.setMargin(false);
			verify_code_layout.setSpacing(false);

			FormLayout forgotLayout = new FormLayout();
			forgotLayout.setStyleName("forgot-form");
			forgotLayout.setMargin(true);
			forgotLayout.setSizeUndefined();

			PWRequest bean = new PWRequest();
			bean.setType(PWRequest.Type.PW_RESET);
			Binder<PWRequest> binder = new Binder<>();

			final TextField sUsernameField = new TextField();
			sUsernameField.setPlaceholder("email address");

			// Shorthand for cases without extra configuration
			// binder.bind(sUsernameField, SignupRequest::getUserId,
			// SignupRequest::setUserId);
			binder.forField(sUsernameField).withValidator(new EmailValidator("This doesn't look like a valid email address")).bind(PWRequest::getUserId, PWRequest::setUserId);

			final TextField codeField = new TextField();
			codeField.setPlaceholder("code");
			final Label codeError = new StyledLabel("code-error", "The code you entered is not valid");
			codeError.setVisible(false);
			verify_code_layout.addComponent(new StyledLabel("code-label", "A validation code has been sent to your email address, please enter it below"));
			verify_code_layout.addComponent(codeField);
			verify_code_layout.setVisible(false);

			VerticalLayout captcha_wrapper = new VerticalLayout();
			captcha_wrapper.setStyleName("captcha-wrapper");
			captcha_wrapper.setMargin(false);
			captcha_wrapper.setSpacing(false);
			captcha_wrapper.setSizeFull();

			final PasswordField sPasswordField = new PasswordField();
			sPasswordField.setPlaceholder("password");
			binder.bind(sPasswordField, PWRequest::getPassword, PWRequest::setPassword);

			final PasswordField sPasswordField2 = new PasswordField();
			sPasswordField2.setPlaceholder("repeat password");

			@SuppressWarnings("serial")
			ReCaptcha captcha = new ReCaptcha(properties.getProperty("halo.recapcha.privatekey", ""), new ReCaptchaOptions() {
				{
					theme = "light";
					sitekey = properties.getProperty("halo.recapcha.sitekey", "");
				}
			});
			final Integer phase[] = { 0 };
			final Button go = new Button("Process", event -> {

				switch (phase[0]) {
				case 0: // Initial Sign up
					try {
						if (!captcha.validate()) {
							Notification.show("Invalid!", Notification.Type.ERROR_MESSAGE);
							captcha.reload();
							return;
						}

						binder.writeBean(bean);
						// Generate validation code
						bean.setValidationCode(PWRequest.generateValidateionCode(bean.getType()));
						log.error(bean.getValidationCode());

						// remove any previous pwrequests
						pw_repo.deleteAll(pw_repo.findAllByUserIdAndType(bean.getUserId(), Type.PW_RESET));

						// save code in backend so we can send link with code and user can click
						// on it to confirm account at a future time
						pw_repo.save(bean);

						// Send Email
						// TODO: mail template replacing string needs to be more efficient
						Context context = new Context();
						context.setVariable("validationcode", bean.getValidationCode());
						context.setVariable("userid", bean.getUserId());
						EmailStatus emailStatus = emailSender.sendHtmlWithTemplate(bean.getUserId(), "Welcome to the Echoplex", "pwreset.html", context);
						// TODO: error check email

						verify_code_layout.setVisible(true);
						sUsernameField.setEnabled(false);
						forgotLayout.removeComponent(captcha_wrapper);
						event.getButton().setCaption("Validate Code");

						// Increment Phase
						phase[0]++;
					} catch (Exception e) {
						Notification.show("Person could not be saved, please check error messages for each field.");
						log.error("", e);
					}
					break;
				case 1: // Code Validation
					if (codeField.getValue().equals(bean.getValidationCode())) {
						pw_repo.delete(bean);
						// w.setContent(signUpSuccess());
						forgotLayout.removeComponent(verify_code_layout);
						codeError.setVisible(false);
						event.getButton().setCaption("Reset Password");
						event.getButton().setEnabled(false);

						VerticalLayout v = new VerticalLayout();
						v.setStyleName("verti-stuff");
						v.setMargin(false);
						v.setSpacing(false);
						ZxcvbnIndicator meter = new ZxcvbnIndicator();
						meter.setTargetField(sPasswordField);
						meter.setStyleName("password-meter");
						meter.setWidth("24em");
						meter.setHeight("10px");

						StyledLabel mismatch = new StyledLabel("mismatch", "Passwords do not match");
						mismatch.setVisible(false);

						sPasswordField.addValueChangeListener(event3 -> {
							if (!sPasswordField2.getValue().trim().isEmpty()) {
								mismatch.setVisible(!sPasswordField2.getValue().equals(sPasswordField.getValue()));
								event.getButton().setEnabled(!mismatch.isVisible());
							}
						});
						sPasswordField2.addValueChangeListener(event3 -> {
							mismatch.setVisible(!sPasswordField2.getValue().equals(sPasswordField.getValue()));
							event.getButton().setEnabled(!mismatch.isVisible());
						});

						forgotLayout.addComponent(sPasswordField, forgotLayout.getComponentIndex(event.getButton()));
						forgotLayout.addComponent(sPasswordField2, forgotLayout.getComponentIndex(event.getButton()));
						v.addComponent(meter);
						v.addComponent(mismatch);
						forgotLayout.addComponent(v, forgotLayout.getComponentIndex(event.getButton()));

						// Increment Phase
						phase[0]++;
					} else {
						codeError.setVisible(true);
					}
					break;
				case 2: // Password Entry
					try {
						binder.writeBean(bean);

						EchoUser e = user_repo.findByUserId(bean.getUserId());
						e.setPassword(bean.getPassword());
						user_repo.save(e);
						w.setContent(signUpSuccess());

						Context context = new Context();
						EmailStatus emailStatus = emailSender.sendHtmlWithTemplate(bean.getUserId(), "Your Password has been Reset", "pwreset_complete.html", context);
						// TODO: error check email

					} catch (Exception e) {
						Notification.show("Person could not be saved, please check error messages for each field.");
						log.error("", e);
					}

					break;
				default:
					log.error("Invalid signup phase {}", phase[0]);

				}

			});

			captcha_wrapper.addComponent(captcha);
			forgotLayout.addComponent(new StyledLabel("forgot-title", "Reset Password"));
			forgotLayout.addComponent(sUsernameField);
			forgotLayout.addComponent(captcha_wrapper);
			forgotLayout.addComponent(verify_code_layout);
			forgotLayout.addComponent(go);
			forgotLayout.addComponent(codeError);
			forgotLayout.addComponent(new StyledLabel("or-label", "OR"));
			forgotLayout.addComponent(new Button("Cancel", event2 -> w.setContent(loginLayout)));

			return forgotLayout;
		}

		private Component signupForm() {

			VerticalLayout verify_code_layout = new VerticalLayout();
			verify_code_layout.setMargin(false);
			verify_code_layout.setSpacing(false);

			FormLayout signupLayout = new FormLayout();
			signupLayout.setStyleName("signup-form");
			signupLayout.setMargin(true);
			signupLayout.setSizeUndefined();

			// Window signupWindow = new Window();
			// signupWindow.setContent(signupLayout);
			// signupWindow.setModal(true);
			// signupWindow.setWidth("566px");
			// signupWindow.setHeight("666px");
			// signupWindow.setClosable(false);
			// signupWindow.setResizable(false);

			// UI.getCurrent().addWindow(signupWindow);
			// signupWindow.setVisible(true);

			PWRequest bean = new PWRequest();
			bean.setType(PWRequest.Type.SIGN_UP);

			// Form for editing the bean
			Binder<PWRequest> binder = new Binder<>();

			final TextField sUsernameField = new TextField();
			sUsernameField.setPlaceholder("email address");

			// Shorthand for cases without extra configuration
			// binder.bind(sUsernameField, SignupRequest::getUserId,
			// SignupRequest::setUserId);
			binder.forField(sUsernameField).withValidator(new EmailValidator("This doesn't look like a valid email address")).bind(PWRequest::getUserId, PWRequest::setUserId);

			final PasswordField sPasswordField = new PasswordField();
			sPasswordField.setPlaceholder("password");
			binder.bind(sPasswordField, PWRequest::getPassword, PWRequest::setPassword);

			final PasswordField sPasswordField2 = new PasswordField();
			sPasswordField2.setPlaceholder("repeat password");

			StyledLabel mismatch = new StyledLabel("mismatch", "Passwords do not match");
			mismatch.setVisible(false);
			StyledLabel already_exists = new StyledLabel("mismatch", "Username already exists");
			already_exists.setVisible(false);

			final TextField codeField = new TextField();
			codeField.setPlaceholder("code");
			final Button resend = new Button("Resend Code", event2 -> {
				bean.setValidationCode(PWRequest.generateValidateionCode(bean.getType()));
				pw_repo.save(bean);
				Context context = new Context(); // Context is a thymeleaf thing
				context.setVariable("validationcode", bean.getValidationCode());
				context.setVariable("userid", bean.getUserId());
				EmailStatus emailStatus = emailSender.sendHtmlWithTemplate(bean.getUserId(), "Welcome to the Echoplex", "signup.html", context);
			});
			resend.addStyleName(ValoTheme.BUTTON_LINK);

			final Label codeError = new StyledLabel("code-error", "The code you entered is not valid");
			codeError.setVisible(false);
			verify_code_layout.addComponent(new StyledLabel("code-label", "A validation code has been sent to your email address, please enter it below"));
			verify_code_layout.addComponent(codeField);
			verify_code_layout.addComponent(resend);
			verify_code_layout.setVisible(false);

			final Integer phase[] = { 0 };
			final Button go = new Button("Sign Me Up!", event2 -> {

				switch (phase[0]) {
				case 0: // Initail Sign up
					try {
						binder.writeBean(bean);

						// check if user name already exists
						if (user_repo.findByUserId(bean.getUserId()) != null) {
							already_exists.setVisible(true);
							return;
						}
						already_exists.setVisible(false);
						// Generate validation code
						bean.setValidationCode(PWRequest.generateValidateionCode(bean.getType()));
						log.error(bean.getValidationCode());
						// TODO: save code in backend so we can send link with code and user can click
						// on it to confirm account at a future time
						pw_repo.save(bean);

						// Send Email
						// TODO: mail template replacing string needs to be more efficient - now using
						// ThymeLeaf
						Context context = new Context();
						context.setVariable("validationcode", bean.getValidationCode());
						context.setVariable("userid", bean.getUserId());
						EmailStatus emailStatus = emailSender.sendHtmlWithTemplate(bean.getUserId(), "Welcome to the Echoplex", "signup.html", context);
						// TODO: Error check email

						// A real application would also save the updated person
						// using the application's backend
						verify_code_layout.setVisible(true);
						sUsernameField.setEnabled(false);
						sPasswordField.setEnabled(false);
						sPasswordField2.setEnabled(false);
						event2.getButton().setCaption("Validate Code");

						// Increment Phase
						phase[0]++;
					} catch (Exception e) {
						Notification.show("Person could not be saved, please check error messages for each field.");
						log.error("", e);
					}
					break;
				case 1: // Code Validation
					if (codeField.getValue().equals(bean.getValidationCode())) {
						EchoUser e = new EchoUser(bean);
						user_repo.save(e);

						pw_repo.delete(bean);
						w.setContent(signUpSuccess());

					} else {
						codeError.setVisible(true);
					}
					break;
				default:
					log.error("Invalid signup phase {}", phase[0]);

				}

			});
			go.setEnabled(false);

			VerticalLayout v = new VerticalLayout();
			v.setStyleName("verti-stuff");
			v.setMargin(false);
			v.setSpacing(false);
			ZxcvbnIndicator meter = new ZxcvbnIndicator();
			meter.setTargetField(sPasswordField);
			meter.setStyleName("password-meter");
			meter.setWidth("24em");
			meter.setHeight("10px");

			sPasswordField.addValueChangeListener(event3 -> {
				if (!sPasswordField2.getValue().trim().isEmpty()) {
					mismatch.setVisible(!sPasswordField2.getValue().equals(sPasswordField.getValue()));
					go.setEnabled(!mismatch.isVisible());
				}
			});
			sPasswordField2.addValueChangeListener(event3 -> {
				mismatch.setVisible(!sPasswordField2.getValue().equals(sPasswordField.getValue()));
				go.setEnabled(!mismatch.isVisible());
			});

			signupLayout.addComponent(new StyledLabel("sign-up", "Sign Up"));
			signupLayout.addComponent(sUsernameField);
			signupLayout.addComponent(sPasswordField);

			signupLayout.addComponent(sPasswordField2);
			v.addComponent(meter);
			v.addComponent(mismatch);
			v.addComponent(already_exists);
			signupLayout.addComponent(v);
			signupLayout.addComponent(verify_code_layout);
			signupLayout.addComponent(go);
			signupLayout.addComponent(codeError);

			signupLayout.addComponent(new StyledLabel("or-label", "OR"));
			Button cancel = new Button("Cancel", event2 -> {
				w.setContent(loginLayout);
			});// signupWindow.close(); w.setVisible(true); });
			signupLayout.addComponent(cancel);

			return signupLayout;
		}
		
		
		

	}

	@SpringUI(path = "/twitter")
	public class TwitterRedirectUI extends UI {
		private static final long serialVersionUID = 83672721177459023L;
		

		@Autowired
		UserRepository user_repo;


		@Override
		protected void init(VaadinRequest request) {
			Authentication auth = SecurityContextHolder.getContext().getAuthentication();
			if (auth != null && auth instanceof Pac4jAuthentication) {
				Pac4jAuthentication token = (Pac4jAuthentication) auth;
				CommonProfile profile = token.getProfile();
				
				EchoUser u = user_repo.findByUserId(profile.getUsername());
				if (u == null) {
					// new user logged in with external account
					// need to generste echouser
					u = new EchoUser(profile);
					u.setPassword(PWRequest.generatePassword());
					log.error("Twitter Profile: {}", profile);
					
					// Send welcome email
					Window w = new Window("Twitter");
					VaadinService.getCurrentRequest().getWrappedSession().setAttribute("any-windows", w);
					
					
				}
				
				
			}

			Page.getCurrent().setLocation("/echo");
		}
	}

	@SpringUI(path = "/facebook")
	public class FacebookRedirectUI extends UI {
		private static final long serialVersionUID = 86672791177459023L;

		@Override
		protected void init(VaadinRequest request) {
			Page.getCurrent().setLocation("/echo");
		}
	}

	@SpringUI(path = "/google")
	public class GoogleRedirectUI extends UI {
		private static final long serialVersionUID = 86672721137459023L;

		@Override
		protected void init(VaadinRequest request) {
			Page.getCurrent().setLocation("/echo");
		}
	}

}

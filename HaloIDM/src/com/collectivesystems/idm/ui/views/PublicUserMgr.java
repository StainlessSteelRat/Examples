package com.collectivesystems.idm.ui.views;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.context.annotation.Scope;
import org.springframework.stereotype.Component;

import com.collectivesystems.core.annotations.HaloAuthority;
import com.collectivesystems.core.annotations.PublicVaadinView;
import com.collectivesystems.core.factory.HaloFactory;
import com.collectivesystems.core.services.service.SSOService;
import com.collectivesystems.core.ui.providers.HaloUI;
import com.vaadin.navigator.View;
import com.vaadin.navigator.ViewChangeListener.ViewChangeEvent;
import com.vaadin.ui.Label;
import com.vaadin.ui.UI;

@Component
@Scope("prototype")
@PublicVaadinView(PublicUserMgr.NAME)
@SuppressWarnings("serial")
@HaloAuthority(authority = "ROLE_EUAA")

public class PublicUserMgr extends UserMgr implements View {
	final Logger log = LoggerFactory.getLogger(PublicUserMgr.class);

	public static final String NAME = "zUserMgr";

	@Override
	public void enter(ViewChangeEvent event) {
		if (event.getParameters() == null || event.getParameters().length() == 0) {
			this.addComponent(new Label("No Access"));
			return;
		}
		final String params[] = event.getParameters().split("/");

		if (params == null || params.length == 0) {
			this.addComponent(new Label("No Access"));
		} else {
			
			if (SSOService.getSSO().checkSessionId(params[0], params[1])) {
				((HaloUI)UI.getCurrent()).setUsername(params[0]);
				user = ldap.getUser(((HaloUI)UI.getCurrent()).getUsername());
				
				build();
			} else {
				this.addComponent( HaloFactory.label("sft_access_help", "Please click on the EUAA User Manager link in your applications list."));
			}
		}
	}

}
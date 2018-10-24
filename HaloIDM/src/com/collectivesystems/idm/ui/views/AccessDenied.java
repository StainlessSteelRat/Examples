package com.collectivesystems.idm.ui.views;

import javax.annotation.PostConstruct;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.context.annotation.Scope;
import org.springframework.stereotype.Component;

import com.collectivesystems.core.dao.CSDAO;
import com.collectivesystems.core.services.service.PropertiesService;
import com.vaadin.navigator.View;
import com.vaadin.navigator.ViewChangeListener.ViewChangeEvent;
import com.vaadin.ui.CssLayout;
import com.vaadin.ui.FormLayout;
import com.vaadin.ui.Label;

import ru.xpoft.vaadin.VaadinView;

@Component
@Scope("prototype")
@VaadinView(AccessDenied.NAME)
@SuppressWarnings("serial")
public class AccessDenied extends CssLayout implements View {

	public static final String NAME = "access-denied";

	@Autowired 
	protected PropertiesService properties;
	
	
	Label stats = new Label();

	@PostConstruct
	public void PostConstruct() {

		setSizeFull();
		addStyleName("sftp-settings-view");
		
		FormLayout l = new FormLayout();
		l.setSizeUndefined();
		l.setMargin(true);
		l.setStyleName("status-panel");
		l.setCaption("Access Denied");
		
		
		stats.setCaption("Access Denied");
		stats.setSizeFull();
		
		
		
		this.addComponent(l);
		l.addComponent(stats);
		
	}
	
	@Override
	public void enter(ViewChangeEvent event) {
		
	}
	
	
	
	

}
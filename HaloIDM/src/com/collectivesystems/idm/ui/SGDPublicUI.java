package com.collectivesystems.idm.ui;

import javax.annotation.PostConstruct;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.context.annotation.Scope;

import com.collectivesystems.core.services.service.PropertiesService;
import com.collectivesystems.core.ui.providers.HaloPublicUI;
import com.vaadin.annotations.Theme;
import com.vaadin.annotations.Title;
import com.vaadin.ui.UI;


@SuppressWarnings("serial")
@Scope("prototype")
@Theme("dashboard")
@Title("Halo Ident.")
@org.springframework.stereotype.Component
public class SGDPublicUI extends HaloPublicUI { 
	 
	@Autowired
	PropertiesService properties;
	
	@PostConstruct
	public void postConstruct() { UI.getCurrent().getPage().setTitle(properties.getProperty("idm.app.name", "Halo Ident."));}
	
}

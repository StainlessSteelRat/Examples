package com.collectivesystems.idm.ui;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import com.collectivesystems.core.ui.providers.HaloUIProvider;
import com.vaadin.server.UIClassSelectionEvent;
import com.vaadin.server.UICreateEvent;
import com.vaadin.server.UIProvider;
import com.vaadin.server.VaadinServletRequest;
import com.vaadin.ui.UI;

@SuppressWarnings("serial")
public class IDMUIProvider extends UIProvider {
	private static Logger log = LoggerFactory.getLogger(HaloUIProvider.class);
	
	@Override
	public UI createInstance(UICreateEvent event) {
        try {
        	log.error("2342343");
            return event.getUIClass().newInstance();            
        } catch (Exception e) {
        	log.error("", e);
        }
        return null;
    }
	
	@Override
	public Class<? extends UI> getUIClass(UIClassSelectionEvent event) {
		log.trace(((VaadinServletRequest)event.getRequest()).getRequestURI().toString());
		if (((VaadinServletRequest)event.getRequest()).getRequestURI().toString().contains("public")) {
			return SGDPublicUI.class;

		}
		return IDMPrivateUI.class;
	}
}

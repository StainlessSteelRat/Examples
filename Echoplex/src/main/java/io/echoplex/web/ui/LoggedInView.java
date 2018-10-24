package io.echoplex.web.ui;

import javax.annotation.PostConstruct;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Component;

import com.vaadin.navigator.View;
import com.vaadin.spring.annotation.SpringView;
import com.vaadin.ui.CssLayout;

import io.echoplex.web.service.PropertiesService;
import io.echoplex.web.ui.components.HaloMap;
import io.echoplex.web.ui.components.HaloSidebar;
import io.echoplex.web.ui.components.HaloSidebarComponent;
import io.echoplex.web.ui.components.StoreDetails;
import io.echoplex.web.ui.components.StoreList;

@SpringView(name="echo")
@Component
public class LoggedInView extends CssLayout implements View {
	private static final long serialVersionUID = 1L;

	@Autowired
	PropertiesService properties;
	
	HaloSidebar sidebar; 
	final CssLayout content = new CssLayout();
	
	public LoggedInView() {	}
	
	@PostConstruct
	public void init() {
		this.setHeight("100%");
		this.setWidth("100%");
		this.setStyleName("halo-layout");
		
	
		
		HaloMap map = new HaloMap(properties.getProperty("halo.map.key", ""), null, "english");
		map.setSizeFull();
		//map.setWidth("500px");
		//map.setHeight("500px");
		//this.addComponent(map);
		
		content.setStyleName("content");
		content.addComponent(map);
		
	//	this.addComponent(new StyledLabel("nrme-logo", ""));
		//layout.addComponent(menubar);
		
		sidebar = new HaloSidebar(true, content);
		
		this.addComponent(content);
		this.addComponent(sidebar);
		
		StoreList storelist = new StoreList();
		HaloSidebarComponent sbc = new HaloSidebarComponent("stores", "Stores", "Add Store");
		sbc.setFolderComponent(storelist);
		sbc.setTab2(new StoreDetails());
		sbc.setStaticFolderComponent(new StoreDetails());
		
		sidebar.addSideBarComponent("stores", sbc);
		
		
		sidebar.switchTo("stores");
	}
}

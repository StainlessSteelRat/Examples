package io.echoplex.web.ui.components;


import java.util.HashMap;
import java.util.Map;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.vaadin.jouni.animator.Animator;
import org.vaadin.jouni.dom.client.Css;

import com.vaadin.ui.Button;
import com.vaadin.ui.Button.ClickEvent;
import com.vaadin.ui.Button.ClickListener;
import com.vaadin.ui.Component;
import com.vaadin.ui.CssLayout;
import com.vaadin.ui.Label;
import com.vaadin.ui.UI;


public class HaloSidebar extends CssLayout implements Button.ClickListener {
	private static final long serialVersionUID = 7642414756354977398L;
	private Logger log = LoggerFactory.getLogger(HaloSidebar.class);
	
	Map<String, Button> buttons = new HashMap<String, Button>();
	Map<String, HaloSidebarComponent> sidebars = new HashMap<String, HaloSidebarComponent>();
	HaloSidebarComponent currentSideBar;
	
	final CssLayout sidebarWrapper = new CssLayout();
	final CssLayout toolbar = new CssLayout();
	final CssLayout sidebar = new CssLayout();
	
	Label sidebarTitle = new Label("Menu");
	
	boolean switched = false;
	Component content;
	
	public static HaloSidebar SIDEBAR;
	public static HaloSidebar getSideBar() { return SIDEBAR; }
	
	public HaloSidebar(boolean incLogout, Component content) {
		SIDEBAR = this;
		this.content = content;
		
		setStyleName("haloSidebar");
		toolbar.setStyleName("toolbar");
		if (incLogout) {
			Button exit = new Button("Exit");
			exit.addStyleName("logout");
			exit.setDescription("Sign Out");
			exit.addClickListener(new ClickListener() {
				private static final long serialVersionUID = -8951953309335959495L;
				@Override
				public void buttonClick(ClickEvent event) {
					UI.getCurrent().getPage().open("j_spring_security_logout", "");
					getSession().close();
				}
			});
			toolbar.addComponent(exit);
		}
		sidebarWrapper.setStyleName("sidebarWrapper");
		sidebar.setStyleName("sidebar");
		sidebar.setId("sidebar");
		
		final CssLayout sidebarHeader = new CssLayout();
		sidebarHeader.setStyleName("sidebarHeader");
		
		
		sidebarTitle.setSizeUndefined();
		sidebarHeader.addComponent(sidebarTitle);
		sidebar.addComponent(sidebarHeader);
		
		sidebarWrapper.addComponent(sidebar);
		
		addComponent(toolbar);
		addComponent(sidebarWrapper);
	
		
	}
	
	public void addSideBarComponent(String name, Component component) {
		Button button = new Button(name);
		button.addStyleName(name);
		button.addClickListener(this);
		button.setData(component);
		//currentSideBar = component;
		component.setVisible(false);
		sidebar.addComponent(component);
		toolbar.addComponent(button);
		buttons.put(name, button);
	}
	
	
	@Override
	public void buttonClick(ClickEvent event) {
		final Button b = event.getButton();
		
		log.info("Switched: " + switched);
//		UI.getCurrent().access(new Runnable() {
//			@Override
//			public void run() {
				if (switched && b.getData() == currentSideBar) {
					// Close the side bar
					Animator.animate(sidebar, new Css().setProperty("transform", "rotateY(-90deg)")).duration(800);
					if (content != null) { content.removeStyleName("sidebarOpened"); }
					switched = false;
					currentSideBar.tabsheet.setSelectedTab(currentSideBar._tab1);
					//content.markAsDirty();
					
				} else if (!switched){
					//Open the side bar
					Animator.animate(sidebar, new Css().setProperty("transform", "rotateY(0deg)")).duration(800);
					if (content != null) { content.addStyleName("sidebarOpened"); }
					switched = true;
					//content.markAsDirty();
					
				}
				if ( b.getData() != currentSideBar) {
					if (currentSideBar != null) { 
						//currentSideBar.closeThenHide();
						currentSideBar.unfold();
						//Animator.animate(currentSideBar, new Css().setProperty("display", "none")).delay(800);
						currentSideBar.setVisible(false);
					}
					
					sidebarTitle.setValue(b.getCaption());
					currentSideBar = (HaloSidebarComponent)b.getData();
					//Animator.animate(currentSideBar, new Css().setProperty("display", "block")).delay(800);
					currentSideBar.setVisible(true);
				}	
//			}});

	}

	public void setTab(int i) {
		switch (i) {
		case 1:
			currentSideBar.tabsheet.setSelectedTab(currentSideBar._tab1);
			break;
		case 2:
			currentSideBar.tabsheet.setSelectedTab(currentSideBar._tab2);
			break;
		}
		// ((RefreshListener)currentSideBar.tabsheet.getSelectedTab()).refresh();
		
	}
	
	public void switchTo(String name) {
		buttons.get(name).click();
	}

}

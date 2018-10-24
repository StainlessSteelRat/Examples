package io.echoplex.web.ui.components;

import java.util.LinkedList;
import java.util.List;

import org.vaadin.jouni.animator.Animator;
import org.vaadin.jouni.dom.client.Css;


import com.vaadin.ui.Component;
import com.vaadin.ui.CssLayout;
import com.vaadin.ui.JavaScript;
import com.vaadin.ui.TabSheet;
import com.vaadin.ui.TabSheet.SelectedTabChangeEvent;
import com.vaadin.ui.TabSheet.SelectedTabChangeListener;
import com.vaadin.ui.themes.ValoTheme;

public class HaloSidebarComponent extends CssLayout {
	private static final long serialVersionUID = 3766818352592854861L;
	
	final protected TabSheet tabsheet = new TabSheet();
	protected String id;
	
	CssLayout _tab1 = new CssLayout();
	CssLayout _tab2 = new CssLayout();
	
	CssLayout staticfolder = new CssLayout();
	CssLayout folder = new CssLayout();
	CssLayout subfolder = new CssLayout();
	CssLayout unfolder = new CssLayout();
	
	List<RefreshListener> _tab2_listeners = new LinkedList<RefreshListener>();
	List<RefreshListener> _tab1_listeners = new LinkedList<RefreshListener>();
	
	public HaloSidebarComponent(final String id, String... tabnames) {
		if (tabnames.length > 2) { throw new RuntimeException("This component only supports 2 tabs"); }
		this.id = id;
		this.setStyleName("sidebarComponent");
		this.setId("sidebarComponent-" + id);
		this.setSizeFull();
		
		
		tabsheet.setHeight(48, Unit.PIXELS);
		tabsheet.addStyleName(ValoTheme.TABSHEET_FRAMED);
		tabsheet.addStyleName(ValoTheme.TABSHEET_PADDED_TABBAR);
		
		tabsheet.addTab(_tab1, tabnames[0]);
		tabsheet.addTab(_tab2, tabnames[1]);
		
		staticfolder.setStyleName("staticfolder");
		staticfolder.setId("staticfolder-" + id);
		folder.setStyleName("folder");		
		subfolder.setStyleName("subfolder");			
		unfolder.setStyleName("unfolder");
		folder.addComponent(subfolder);	
		
		
		this.addComponent(tabsheet);		
		this.addComponent(staticfolder);
		this.addComponent(folder);
		this.addComponent(unfolder);
		
		
		
		tabsheet.addSelectedTabChangeListener(new SelectedTabChangeListener() {
			private static final long serialVersionUID = 156752754436754512L;

			@Override
			public void selectedTabChange(SelectedTabChangeEvent event) {
				if (event.getTabSheet().getSelectedTab() == _tab2) {
					Animator.animate(folder, new Css().setProperty("transform", "rotateY(-180deg)")).duration(800);
					Animator.animate(unfolder, new Css().setProperty("transform", "rotateY(0deg)")).duration(800);
					//Animator.animate(sidebar, new Css().setProperty("overflow", "visible"));
					JavaScript.getCurrent().execute("document.getElementById('sidebar').className = \"v-csslayout v-layout v-widget sidebar v-csslayout-sidebar folderOpened\";");
					JavaScript.getCurrent().execute("setTimeout(function(){ document.getElementById('staticfolder-" + id + "').className = \"v-csslayout v-layout v-widget staticfolderHidden v-csslayout-staticfolderHidden\"; }, 1000);");
					for (RefreshListener l : _tab2_listeners) { l.refresh(); }
					
				} else {
					Animator.animate(folder, new Css().setProperty("transform", "rotateY(0deg)")).duration(800);
					Animator.animate(unfolder, new Css().setProperty("transform", "rotateY(180deg)")).duration(800);		
					//Animator.animate(sidebar, new Css().setProperty("overflow", "hidden")).delay(800).ease(Ease.OUT);
					//CssAnimation s = new CssAnimation().delay(800);
					JavaScript.getCurrent().execute("setTimeout(function(){ document.getElementById('sidebar').className = \"v-csslayout v-layout v-widget sidebar v-csslayout-sidebar\"; }, 1000);");
					JavaScript.getCurrent().execute("document.getElementById('staticfolder-" + id + "').className = \"v-csslayout v-layout v-widget staticfolder v-csslayout-staticfolder\"; ");
					//JavaScript.getCurrent().execute("changeOverflow2()");
					//Animator.animate(CssAnimation);
					//sidebar.removeStyleName("folderOpened");
					//fireRefreshListeners(_tab1);
					for (RefreshListener l : _tab1_listeners) { l.refresh(); }
				}
				
			}} );
		

	}
	
//	private void fireRefreshListeners(Component component) {
//		if (component == _tab1) {
//			for (RefreshListener l : _tab1_listeners) {
//				l.refresh();
//			}
//		}
//	}
	

	
	public void closeThenHide() {
		Animator.animate(folder, new Css().setProperty("transform", "rotateY(0deg)")).duration(800);
		Animator.animate(unfolder, new Css().setProperty("transform", "rotateY(180deg)")).duration(800);
		JavaScript.getCurrent().execute("setTimeout(function(){ document.getElementById('sidebar-" + id + "').className = \"v-csslayout v-layout v-widget sidebar v-csslayout-sidebar\"; }, 1000);");
		JavaScript.getCurrent().execute("document.getElementById('staticfolder-" + id + "').className = \"v-csslayout v-layout v-widget staticfolder v-csslayout-staticfolder\"; ");
		
	}
	
	public void unfold() {
		tabsheet.setSelectedTab(_tab1);
	}
	
	public void setStaticFolderComponent(Component component) {
		staticfolder.addComponent(component);
		component.addStyleName("staticfolder-component");
	}
	
	public void setTab2(Component component) {
		unfolder.addComponent(component);
		component.addStyleName("unfolder-component");
		_tab2_listeners.add((RefreshListener)component);
		
	}
	
	public void setFolderComponent(Component component) {
		subfolder.addComponent(component);
		component.addStyleName("subfolder-component");
	}

	
}


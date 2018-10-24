package io.echoplex.web.ui.components;

import com.vaadin.ui.CssLayout;


public class StoreList extends CssLayout implements RefreshListener {
	private static final long serialVersionUID = 5760473683571978897L;
	private static StoreList STORELIST;
	
	public static StoreList get() { return STORELIST; }
	
	
	
	
	public StoreList() {
		STORELIST = this;
		this.setSizeFull();
		
	
	}
	
	public void refresh() {
	}
	
	


	
}

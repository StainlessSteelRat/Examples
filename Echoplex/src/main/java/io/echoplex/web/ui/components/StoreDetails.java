package io.echoplex.web.ui.components;


import java.util.Random;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import com.vaadin.ui.Button;
import com.vaadin.ui.Button.ClickEvent;
import com.vaadin.ui.CssLayout;
import com.vaadin.ui.HorizontalLayout;

public class StoreDetails extends CssLayout implements RefreshListener {
	private static final long serialVersionUID = -9053726090469268382L;
	final static Logger log = LoggerFactory.getLogger(StoreDetails.class);
	final Random randomno = new Random();
	
	

	public StoreDetails() {
	

		HorizontalLayout buttons = new HorizontalLayout();
		buttons.setSpacing(true);
		buttons.setStyleName("windowButtons");

		// signup_button
		Button save_button = new Button("Save", new Button.ClickListener() {
			private static final long serialVersionUID = 1L;

			@Override
			public void buttonClick(ClickEvent event) {
				
			}
		});
		
		save_button.setStyleName("saveButton");

		Button save_close_button = new Button("Save and Close", new Button.ClickListener() {
			private static final long serialVersionUID = 1L;
			@Override
			public void buttonClick(ClickEvent event) {
				
			}
		});
	
		save_close_button.setStyleName("saveButton");

		Button cancel_button = new Button("Clear", new Button.ClickListener() {
			private static final long serialVersionUID = 1L;
			@Override
			public void buttonClick(ClickEvent event) {
				
			}
		});
		
		cancel_button.setStyleName("cancelButton");
		
		Button close_button = new Button("Close", new Button.ClickListener() {
			private static final long serialVersionUID = 1L;
			@Override
			public void buttonClick(ClickEvent event) {
				HaloSidebar.getSideBar().setTab(1);
			}
		});
	
		cancel_button.setStyleName("closeButton");

		buttons.addComponent(cancel_button);
		buttons.addComponent(save_button);
		buttons.addComponent(save_close_button);
		buttons.addComponent(close_button);

	//	this.addComponent(layout);
		this.addComponent(buttons);

	}
	


	@Override
	public void refresh() {
	
	}
}

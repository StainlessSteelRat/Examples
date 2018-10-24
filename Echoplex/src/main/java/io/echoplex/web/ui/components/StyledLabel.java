package io.echoplex.web.ui.components;

import com.vaadin.shared.ui.ContentMode;
import com.vaadin.ui.Label;

public class StyledLabel extends Label {
		private static final long serialVersionUID = -3731324399548247043L;
		
		public StyledLabel(String style, String caption) {
			super(caption);
			setStyleName(style);
			setSizeUndefined();
		}
		
		public StyledLabel(String style, String caption, boolean html) {
			super(caption);
			setStyleName(style);
			setSizeUndefined();
		
			this.setContentMode(html ? ContentMode.HTML : ContentMode.TEXT);
		}
}

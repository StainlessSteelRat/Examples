package io.echoplex.web.utils;

import com.vaadin.ui.Layout;

import kaesdingeling.hybridmenu.HybridMenu;
import kaesdingeling.hybridmenu.components.NotificationCenter;
import kaesdingeling.hybridmenu.data.MenuConfig;
import kaesdingeling.hybridmenu.data.enums.EMenuComponents;

public class HaloMenuBuilder {
	private HybridMenu hybridMenu;

	private HaloMenuBuilder(HybridMenu hybridMenu) {
		this.hybridMenu = hybridMenu;
	}

	public static HaloMenuBuilder get() {
		return new HaloMenuBuilder(new HybridMenu());
	}

	public HaloMenuBuilder setContent(Layout component) {
		hybridMenu.setContent(component);
		return this;
	}

	public HaloMenuBuilder setMenuComponent(EMenuComponents menuComponents) {
		hybridMenu.setMenuComponent(menuComponents);
		return this;
	}

	public HaloMenuBuilder setConfig(MenuConfig config) {
		hybridMenu.setConfig(config);
		return this;
	}

	public HaloMenuBuilder withNotificationCenter(NotificationCenter notificationCenter) {
		hybridMenu.setNotificationCenter(notificationCenter);
		return this;
	}

	public HaloMenuBuilder withNavigator(boolean initNavigation) {
		hybridMenu.setInitNavigator(initNavigation);
		return this;
	}

	public HybridMenu build() {
		hybridMenu.build();
		return hybridMenu;
	}
}
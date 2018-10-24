package com.collectivesystems.idm.ui.views;

import org.springframework.context.annotation.Scope;
import org.springframework.stereotype.Component;

import com.collectivesystems.core.annotations.HaloAuthority;

import ru.xpoft.vaadin.VaadinView;

@Component
@Scope("prototype")
@VaadinView(BRMUserMgr.NAME)
@SuppressWarnings("serial")
@HaloAuthority(authority = "ROLE_NOBODY")
public class BRMUserMgr extends UserMgr {
	final static String NAME = "aUser Mgr";

}

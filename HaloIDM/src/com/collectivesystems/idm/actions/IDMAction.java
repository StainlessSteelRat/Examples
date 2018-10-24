package com.collectivesystems.idm.actions;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import com.collectivesystems.core.services.service.FlagStoreService;
import com.collectivesystems.idm.services.service.FlagService;
import com.collectivesystems.remote.NGContext;

public class IDMAction {

	private static Logger log = LoggerFactory.getLogger(IDMAction.class);

	public static void nailMain(NGContext context) {
		
		// Params -   name action <options>
		String namespace = context.getArgs()[0];
		String action = context.getArgs()[1];		
		
		
		log.trace(action);
		switch (action) {
		
		case "complete":
			String path = context.getArgs()[2];
			FlagService.service.setFlag(namespace, path, "complete", context.getInetAddress().getCanonicalHostName());
			break;
			
		case "wait":
		default:
			String name = context.getArgs()[2];
		//	if  (context.getArgs().length < 3 || (name = context.getArgs()[2]).trim().isEmpty()) { name = FlagService.DEFAUT_NAMESPACE; }
			FlagService.service.waitFlag(namespace, name);
			context.out.print(FlagService.service.getFlag(namespace, name).getValue());
			FlagService.service.resetFlag(name);
		}

		context.exit(0);
	}
}

package com.collectivesystems.idm.actions;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import com.collectivesystems.idm.services.service.FlagService;
import com.collectivesystems.remote.NGContext;

public class Test {

	private static Logger log = LoggerFactory.getLogger(Test.class);

	public static void nailMain(NGContext context) {
		
	
		{
			final String NAMESPACE = "aukacggs";
			final String VALUE = "/export/home/3paxxxx";
			final String NAME ="homeDirectory-create";
			
			FlagService.service.setFlag(NAMESPACE, NAME , VALUE, "com.collectivesystems.idm");
			new Thread(new Runnable() {

				@Override
				public void run() {
					FlagService.service.waitFlag(NAMESPACE, VALUE);
					String v =  FlagService.service.getFlag(NAMESPACE, VALUE).getValue();
					
					log.error(NAMESPACE + " " + v);
					FlagService.service.removeFlag(NAMESPACE, VALUE);
				}} ).start();
		}

		{
			final String NAMESPACE = "aukacghs";
			final String VALUE = "/export/home/3paxxxx";
			final String NAME ="homeDirectory-create";
			
			FlagService.service.setFlag(NAMESPACE, NAME , VALUE, "com.collectivesystems.idm");
			new Thread(new Runnable() {

				@Override
				public void run() {
					FlagService.service.waitFlag(NAMESPACE, VALUE);
					String v =  FlagService.service.getFlag(NAMESPACE, VALUE).getValue();
					
					log.error(NAMESPACE + " " + v);
					FlagService.service.removeFlag(NAMESPACE, VALUE);
				}} ).start();
		}
		
		
		context.exit(0);
	}
}

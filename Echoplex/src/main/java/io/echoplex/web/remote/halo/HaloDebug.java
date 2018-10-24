package io.echoplex.web.remote.halo;

import org.apache.log4j.Level;
import org.apache.log4j.Logger;

import io.echoplex.web.remote.NGContext;

public class HaloDebug {
	@SuppressWarnings("unused")
	private static Logger log = Logger.getLogger(HaloDebug.class);

	public static void nailMain(NGContext context) {
		if ((context.getArgs() != null) && (context.getArgs().length > 1)) {
			
			String mode = context.getArgs()[1];
			String clazz = context.getArgs()[0];
			
			try {
				Class.forName(clazz);
			} catch (ClassNotFoundException e) {
				context.out.println("Class not Found.");
				context.exit(1);
				return;
			}
			
			
			Logger logger = Logger.getLogger(clazz);			

			if (mode.equalsIgnoreCase("TRACE")) {
				logger.setLevel(Level.TRACE);
			} else if (mode.equalsIgnoreCase("DEBUG")) {
				logger.setLevel(Level.DEBUG);
			} else if (mode.equalsIgnoreCase("INFO")) {
				logger.setLevel(Level.INFO);
			} else if (mode.equalsIgnoreCase("WARN")) {
				logger.setLevel(Level.WARN);
			} else if (mode.equalsIgnoreCase("ERROR")) {
				logger.setLevel(Level.ERROR);
			} else if (mode.equalsIgnoreCase("FATAL")) {
				logger.setLevel(Level.FATAL);
			} else {
				context.exit(1);
			}
			logger.info("[" + Logger.getLogger(clazz).getName() + "] Log Level set to " + mode.toUpperCase());
		} else {
			context.out.println("It's better to specify a class and log level as parameters.");
		}
		context.exit(0);
	}
	
	
	
}
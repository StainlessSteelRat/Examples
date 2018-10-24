package io.echoplex.web.remote.halo;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import io.echoplex.web.remote.NGContext;

public class HaloPing {

	private static Logger log = LoggerFactory.getLogger(HaloPing.class);

	public static void nailMain(NGContext context) {
		if ((context.getArgs() != null) && (context.getArgs().length > 0)) {
			log.trace("Ping from " + context.getArgs()[0]);
		} else {
			log.info("Ping from " + context.getInetAddress());
		}
		context.out.println("Ping OK");
		context.exit(0);
	}
}

package io.echoplex.web.remote.halo;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import io.echoplex.web.remote.NGContext;

/*
 * private String username;
 private String token;
 private String stage;
 private Date generatedDate;
 private Date receivedDate;
 */
public class HaloJourney {

	private static Logger log = LoggerFactory.getLogger(HaloJourney.class);
	private static int USERNAME = 0;
	private static int TOKEN = 1;
	private static int STAGE = 2;
	private static int GenDATE = 3;
	private static int RecDATE = 4;

	public static void nailMain(NGContext context) {
		if (log.isTraceEnabled()) {
			log.trace("Journey Stamp Recieved: ");
			log.trace("	ACG User	" + context.getArgs()[USERNAME]);
			log.trace("	NT User		" + context.getArgs()[TOKEN]);
			log.trace("	Hostname	" + context.getArgs()[STAGE]);
			log.trace("	IP Addr		" + context.getArgs()[GenDATE]);
			log.trace("	SDG Name	" + context.getArgs()[RecDATE]);
		}

		/*
		 * Journey
		 * 
		 * <c:import url="http://adeacgcs-z1/acgm/api..." /> returns img tag
		 * with token
		 * 
		 * 
		 * <img src="https://acg.vodafone.com/acgm/journey/stage/token />
		 * 
		 * AM login page - JSP from server AM Login page - IMG emebdded - client
		 * side User name entry Env selection Password Entry AMLastLogin - from
		 * log file SGD Loading Page - IMG Request, date stamp from client side
		 * SGD Webtop LaunchAttempt
		 */

		// args username/token/stage/datestamp
		//

		context.exit(0);
	}
}
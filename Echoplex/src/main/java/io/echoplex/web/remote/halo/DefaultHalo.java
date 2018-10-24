package io.echoplex.web.remote.halo;

import io.echoplex.web.remote.NGConstants;
import io.echoplex.web.remote.NGContext;

public class DefaultHalo {

	public static void nailMain(NGContext context) {
		context.err.println("No such command: " + context.getCommand());
		context.exit(NGConstants.EXIT_NOSUCHCOMMAND);
	}
}

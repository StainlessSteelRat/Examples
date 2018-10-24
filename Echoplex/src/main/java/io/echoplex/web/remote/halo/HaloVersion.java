package io.echoplex.web.remote.halo;

import io.echoplex.web.remote.NGContext;

public class HaloVersion {

	public static void nailMain(NGContext context) {
		//context.out.println("HaloCore Version " + Utils.VERSION);
		context.out.println(context.getServer().getPropertes().getVersionsAsString());
	}
}

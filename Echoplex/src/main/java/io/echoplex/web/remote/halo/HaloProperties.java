package io.echoplex.web.remote.halo;

import java.util.Arrays;
import java.util.Properties;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import io.echoplex.web.remote.NGContext;
import io.echoplex.web.service.PropertiesService;

public class HaloProperties {

	private static final String DELIM = "=";
	@SuppressWarnings("unused")
	private static Logger log = LoggerFactory.getLogger(HaloProperties.class);

	public static void nailMain(NGContext context) {
		String[] args = context.getArgs();

		if (args.length < 1) {
			context.out.println("You must specifiy a subcommand");
			context.exit(1);
			return;
		}
		switch (args[0]) {
		case "unset":
			unset(context, Arrays.copyOfRange(args, 1, args.length));
			break;
		case "set":
			set(context, Arrays.copyOfRange(args, 1, args.length));
			break;
		case "get":
			get(context, Arrays.copyOfRange(args, 1, args.length));
			break;
		case "getstatic":
			getStatic(context, Arrays.copyOfRange(args, 1, args.length));
			break;
		case "list":
			list(context);
			break;
		case "commit":
			commit(context, Arrays.copyOfRange(args, 1, args.length));
			break;
		default:
			context.out.println("Unknow subcommand " + args[0]);
		}

		context.exit(0);
	}

	private static void commit(NGContext context, String[] args) {
		boolean echo = true;
		for (String arg : args) {
			if (arg.equals("-q")) {
				echo = false;
			}
		}
		PropertiesService.persist();
		if (echo) {
			context.out.println("Properties Saved");
		}
	}

	private static void list(NGContext context) {
		Properties properties = context.getServer().getPropertes().getProperties();
		for (Object key : properties.keySet()) {
			if (((String) key).contains("password")) {
				context.out.println((String) key + DELIM + "********");
			} else {
				context.out.println((String) key + DELIM + properties.getProperty((String) key));
			}
		}
	}

	private static void get(NGContext context, String[] args) {
		Properties properties = context.getServer().getPropertes().getProperties();
		boolean echo = true;
		if (args.length < 1) {
			context.out.println("You must specifiy at least one property to retreive");
		}
		for (String arg : args) {
			if (arg.equals("-q")) {
				echo = false;
				continue;
			}
			if (echo) {
				context.out.print(arg + DELIM);
			}
			if (((String) arg).contains("password")) {
				context.out.println("********");
			} else {
				context.out.println(properties.getProperty((String) arg));
			}

		}

	}

	private static void getStatic(NGContext context, String[] args) {
		boolean echo = true;
		if (args.length < 1) {
			context.out.println("You must specifiy at least one property to retreive");
		}
		for (String arg : args) {
			if (arg.equals("-q")) {
				echo = false;
				continue;
			}
			if (echo) {
				context.out.print(arg + DELIM);
			}
			if (((String) arg).contains("password")) {
				context.out.println("********");
			} else {
				context.out.println(PropertiesService.getPropertyStatic((String) arg, "Not Set"));
			}

		}

	}

	private static void unset(NGContext context, String[] args) {
		Properties properties = context.getServer().getPropertes().getProperties();
		boolean echo = true;
		if (args.length < 1) {
			context.out.println("You must specifiy at least one property");
		}
		for (String arg : args) {
			if (arg.equals("-q")) {
				echo = false;
				continue;
			}
			if (echo) {
				context.out.println("Removing property " + arg);
			}
			properties.remove(arg);
		}
	}

	private static void set(NGContext context, String[] args) {
		Properties properties = context.getServer().getPropertes().getProperties();
		boolean echo = true;
		if (args.length < 1) {
			context.out.println("You must specifiy at least one key/value pair");
		}
		for (String arg : args) {
			String kvp[] = arg.split(DELIM);
			if (kvp.length > 2) {
				kvp = arg.split(DELIM + DELIM);
				if (kvp.length == 1) {
					context.out.println("Please use " + DELIM + DELIM + " to sparate the key/value when key or value contains '" + DELIM + "' character.");
					context.out.println("Problem with " + arg);
					context.exit(1);
					return;
				}
			} else if (kvp.length == 1) {
				if (arg.equals("-q")) {
					echo = false;
					continue;
				}
				context.out.println("Please specify a key/value pair in the form of key" + DELIM + "value.");
				context.out.println("Problem with " + arg);
				context.exit(1);
				return;
			}

			properties.setProperty(kvp[0], kvp[1]);
			if (echo) {
				context.out.println(kvp[0] + "[" + kvp[1] + "] ok.");
			}
		}
	}
}

package io.echoplex.web.remote;
import java.io.IOException;
import java.io.InputStream;
import java.io.PrintStream;
import java.lang.reflect.Method;
import java.net.DatagramSocket;
import java.net.InetAddress;
import java.net.ServerSocket;
import java.net.Socket;
import java.net.UnknownHostException;
import java.util.Iterator;
import java.util.Map;

import javax.annotation.PostConstruct;

import org.apache.log4j.Logger;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;

import io.echoplex.web.remote.halo.DefaultHalo;
import io.echoplex.web.service.PropertiesService;

@Service
public class CommandServer implements Runnable {

	private static Logger log = Logger.getLogger(CommandServer.class);
	
	@Autowired 
	protected PropertiesService properties;
	public PropertiesService getPropertes() { return properties; }
	
	/**
	 * The address on which to listen, or null to listen on all
	 * local addresses
	 */
	private InetAddress addr = null;
	
	/**
	 * The port on which to listen, or zero to select a port automatically
	 */
	private int port = 0;
	
	/**
	 * The socket doing the listening
	 */
	private ServerSocket serversocket;
	
	/**
	 * True if this CommandServer has received instructions to shut down
	 */
	private boolean shutdown = false;
	
	/**
	 * True if this CommandServer has been started and is accepting connections
	 */
	private boolean running = false;
	
	/**
	 * This CommandServer's AliasManager, which maps aliases to classes
	 */
	private AliasManager aliasManager;
	
	/**
	 * If true, fully-qualified classnames are valid commands
	 */
	private boolean allowNailsByClassName = true;
	
	/**
	 * The default class to use if an invalid alias or classname is
	 * specified by the client.
	 */
	private Class<?> defaultNailClass = null;
	
	/**
	 * A pool of NGSessions ready to handle client connections
	 */
	private NGSessionPool sessionPool = null;
	
	/**
	 * <code>System.out</code> at the time of the CommandServer's creation
	 */
	public final PrintStream out = System.out;

	/**
	 * <code>System.err</code> at the time of the CommandServer's creation
	 */
	public final PrintStream err = System.err;
	
	/**
	 * <code>System.in</code> at the time of the CommandServer's creation
	 */
	public final InputStream in = System.in;
	
	/**
	 * a collection of all classes executed by this server so far
	 */
	private Map<Class<?>, NailStats> allNailStats = null;
	
	/**
	 * Remember the security manager we start with so we can restore it later
	 */
	private SecurityManager originalSecurityManager = null;
	
	/**
	 * Creates a new CommandServer that will listen at the specified address and
	 * on the specified port.
	 * This does <b>not</b> cause the server to start listening.  To do
	 * so, create a new <code>Thread</code> wrapping this <code>CommandServer</code>
	 * and start it.
	 * @param addr the address at which to listen, or <code>null</code> to bind
	 * to all local addresses
	 * @param port the port on which to listen.
	 */
	public CommandServer(InetAddress addr, int port) {
		init(addr, port);
	}
	
	/**
	 * Creates a new CommandServer that will listen on the default port
	 * (defined in <code>NGConstants.DEFAULT_PORT</code>).
	 * This does <b>not</b> cause the server to start listening.  To do
	 * so, create a new <code>Thread</code> wrapping this <code>CommandServer</code>
	 * and start it.
	 */
	public CommandServer() { }
	
	@PostConstruct
	public void serviceInit() {
		int port = NGConstants.DEFAULT_PORT;
		try {
			 port = Integer.parseInt(properties.getProperty("halo.remote.port", "1701"));
		} catch (Exception e) {
			if (!available(NGConstants.DEFAULT_PORT)) {
				  port = NGConstants.DEFAULT_PORT + 1;
				  log.warn("halo remote port set to " + port);
			}
		}
		
	    init(null, port);
	    Thread t = new Thread(this);
        t.setName("CommandServer(localhost," + port + ")");
        t.start();

        Runtime.getRuntime().addShutdownHook(new NGServerShutdowner(this));
        log.info("Command Server Started: "  + t.getName());
	}
	
	/**
	 * Checks to see if a specific port is available.
	 *
	 * @param port the port to check for availability
	 */
	public static boolean available(int port) {
//	    if (port < 0 || port > ) {
//	        throw new IllegalArgumentException("Invalid start port: " + port);
//	    }
	    ServerSocket ss = null;
	    DatagramSocket ds = null;
	    try {
	        ss = new ServerSocket(port);
	        ss.setReuseAddress(true);
	        ds = new DatagramSocket(port);
	        ds.setReuseAddress(true);
	        return true;
	    } catch (IOException e) {
	    } finally {
	        if (ds != null) {
	            ds.close();
	        }

	        if (ss != null) {
	            try {
	                ss.close();
	            } catch (IOException e) {
	                /* should not be thrown */
	            }
	        }
	    }
	    return false;
	}

	
	
	/**
	 * Sets up the CommandServer internals
	 * @param addr the InetAddress to bind to
	 * @param port the port on which to listen
	 */
	protected void init(InetAddress addr, int port) {
		this.addr = addr;
		this.port = port;
		
		this.aliasManager = new AliasManager();
		allNailStats = new java.util.HashMap<Class<?>, NailStats>();
		// allow a maximum of 10 idle threads.  probably too high a number
		// and definitely should be configurable in the future
		sessionPool = new NGSessionPool(this, 10);
		//Runtime.getRuntime().addShutdownHook(new NGServerShutdowner(this));
	}

	/**
	 * Sets a flag that determines whether Nails can be executed by class name.
	 * If this is false, Nails can only be run via aliases (and you should
	 * probably remove ng-alias from the AliasManager).
	 * 
	 * @param allowNailsByClassName true iff Nail lookups by classname are allowed
	 */
	public void setAllowNailsByClassName(boolean allowNailsByClassName) {
		this.allowNailsByClassName = allowNailsByClassName;
	}
	
	/**
	 * Returns a flag that indicates whether Nail lookups by classname
	 * are allowed.  If this is false, Nails can only be run via aliases.
	 * @return a flag that indicates whether Nail lookups by classname
	 * are allowed.
	 */
	public boolean allowsNailsByClassName() {
		return (allowNailsByClassName);
	}
	
	/**
	 * Sets the default class to use for the Nail if no Nails can
	 * be found via alias or classname. (may be <code>null</code>,
	 * in which case NailGun will use its own default)
	 * @param defaultNailClass the default class to use for the Nail
	 * if no Nails can be found via alias or classname.
	 * (may be <code>null</code>, in which case NailGun will use
	 * its own default)
	 */
	public void setDefaultNailClass(Class<?> defaultNailClass) {
		this.defaultNailClass = defaultNailClass;
	}
	
	/**
	 * Returns the default class that will be used if no Nails
	 * can be found via alias or classname.
	 * @return the default class that will be used if no Nails
	 * can be found via alias or classname.
	 */
	public Class<?> getDefaultNailClass() {
		return ((defaultNailClass == null) ? DefaultHalo.class : defaultNailClass) ;
	}
	
	/**
	 * Returns the current NailStats object for the specified class, creating
	 * a new one if necessary
	 * @param nailClass the class for which we're gathering stats
	 * @return a NailStats object for the specified class
	 */
	private NailStats getOrCreateStatsFor(Class<?> nailClass) {
		NailStats result = null;
		synchronized(allNailStats) {
			result = (NailStats) allNailStats.get(nailClass);
			if (result == null) {
				result = new NailStats(nailClass);
				allNailStats.put(nailClass, result);
			}
		}
		return (result);
	}
	
	/**
	 * Provides a means for an NGSession to register the starting of
	 * a nail execution with the server.
	 * 
	 * @param nailClass the nail class that was launched
	 */
	void nailStarted(Class<?> nailClass) {
		NailStats stats = getOrCreateStatsFor(nailClass);
		stats.nailStarted();
	}
	
	/**
	 * Provides a means for an NGSession to register the completion of
	 * a nails execution with the server.
	 * 
	 * @param nailClass the nail class that finished
	 */
	void nailFinished(Class<?> nailClass) {
		NailStats stats = (NailStats) allNailStats.get(nailClass);
		stats.nailFinished();
	}
	
	/**
	 * Returns a snapshot of this CommandServer's nail statistics.  The result is a <code>java.util.Map</code>,
	 * keyed by class name, with <a href="NailStats.html">NailStats</a> objects as values.
	 * 
	 * @return a snapshot of this CommandServer's nail statistics.
	 */
	public Map<String, Object> getNailStats() {
		Map<String, Object> result = new java.util.TreeMap<String, Object>();
		synchronized(allNailStats) {
			for (Iterator<?> i = allNailStats.keySet().iterator(); i.hasNext();) {
				Class<?> nailclass = (Class<?>) i.next();
				result.put(nailclass.getName(), ((NailStats) allNailStats.get(nailclass)).clone());
			}
		}
		return (result);
	}
	
	/**
	 * Returns the AliasManager in use by this CommandServer.
	 * @return the AliasManager in use by this CommandServer.
	 */
	public AliasManager getAliasManager() {
		return (aliasManager);
	}

	/**
	 * <p>Shuts down the server.  The server will stop listening
	 * and its thread will finish.  Any running nails will be allowed
	 * to finish.</p>
	 * 
	 * <p>Any nails that provide a
	 * <pre><code>public static void nailShutdown(CommandServer)</code></pre>
	 * method will have this method called with this CommandServer as its sole
	 * parameter.</p>
	 * 
	 * @param exitVM if true, this method will also exit the JVM after
	 * calling nailShutdown() on any nails.  This may prevent currently
	 * running nails from exiting gracefully, but may be necessary in order
	 * to perform some tasks, such as shutting down any AWT or Swing threads
	 * implicitly launched by your nails.
	 */
	public void shutdown(boolean exitVM) {
		synchronized(this) {
			if (shutdown) return;
			shutdown = true;
		}
		
		try {
			serversocket.close();
		} catch (Throwable toDiscard) {}
		
		sessionPool.shutdown();
		
		Class<?>[] argTypes = new Class[1];
		argTypes[0] = CommandServer.class;
		Object[] argValues = new Object[1];
		argValues[0] = this;
		
		// make sure that all aliased classes have associated nailstats
		// so they can be shut down.
		for (Iterator<?> i = getAliasManager().getAliases().iterator(); i.hasNext();) {
			Alias alias = (Alias) i.next();
			getOrCreateStatsFor(alias.getAliasedClass());
		}
		
		synchronized(allNailStats) {
			for (Iterator<?> i = allNailStats.values().iterator(); i.hasNext();) {
				NailStats ns = (NailStats) i.next();
				Class<?> nailClass = ns.getNailClass();
				
				// yes, I know this is lazy, relying upon the exception
				// to handle the case of no nailShutdown method.
				try {
					Method nailShutdown = nailClass.getMethod("nailShutdown", argTypes);
					nailShutdown.invoke(null, argValues);
				} catch (Throwable toDiscard) {}
			}
		}
		
		// restore system streams
		System.setIn(in);
		System.setOut(out);
		System.setErr(err);
		
		System.setSecurityManager(originalSecurityManager);
		
		if (exitVM) {
			System.exit(0);
		}
	}
	
	/**
	 * Returns true iff the server is currently running.
	 * @return true iff the server is currently running.
	 */
	public boolean isRunning() {
		return (running);
	}
	
	/**
	 * Returns the port on which this server is (or will be) listening.
	 * @return the port on which this server is (or will be) listening.
	 */
	public int getPort() {
		return ((serversocket == null) ? port : serversocket.getLocalPort());
	}
	
	/**
	 * Listens for new connections and launches NGSession threads
	 * to process them.
	 */
	public void run() {
		running = true;
		NGSession sessionOnDeck = null;
		
		originalSecurityManager = System.getSecurityManager();
        //System.setSecurityManager( new NGSecurityManager(originalSecurityManager));
  

		synchronized(System.in) {
			if (!(System.in instanceof ThreadLocalInputStream)) {
				System.setIn(new ThreadLocalInputStream(in));
				System.setOut(new ThreadLocalPrintStream(out));
				System.setErr(new ThreadLocalPrintStream(err));				
			}
		}
		
		try {
			if (addr == null) {
				serversocket = new ServerSocket(port);
			} else {
				serversocket = new ServerSocket(port, 0, addr);
			}
			
			while (!shutdown) {
				sessionOnDeck = sessionPool.take();
				Socket socket = serversocket.accept();
				sessionOnDeck.run(socket);
			}

		} catch (Throwable t) {
			// if shutdown is called while the accept() method is blocking,
			// an exception will be thrown that we don't care about.  filter
			// those out.
			if (!shutdown) {
				t.printStackTrace();
			}
		}
		if (sessionOnDeck != null) {
			sessionOnDeck.shutdown();
		}
		running = false;
	}
	
	private static void usage() {
		System.err.println("Usage: java com.collectivesystems.remote.CommandServer");
		System.err.println("   or: java com.collectivesystems.remote.CommandServer port");
		System.err.println("   or: java com.collectivesystems.remote.CommandServer IPAddress");
		System.err.println("   or: java com.collectivesystems.remote.CommandServer IPAddress:port");
	}
	
	/**
	 * Creates and starts a new <code>CommandServer</code>.  A single optional
	 * argument is valid, specifying the port on which this <code>CommandServer</code>
	 * should listen.  If omitted, <code>CommandServer.DEFAULT_PORT</code> will be used.
	 * @param args a single optional argument specifying the port on which to listen.
	 * @throws NumberFormatException if a non-numeric port is specified
	 */
	public static void main(String[] args) throws NumberFormatException, UnknownHostException {

		if (args.length > 1) {
			usage();
			return;
		}

		// null server address means bind to everything local
		InetAddress serverAddress = null;
		int port = NGConstants.DEFAULT_PORT;
		
		// parse the sole command line parameter, which
		// may be an inetaddress to bind to, a port number,
		// or an inetaddress followed by a port, separated
		// by a colon
		if (args.length != 0) {
			String[] argParts = args[0].split(":");
			String addrPart = null;
			String portPart = null;
			if (argParts.length == 2) {
				addrPart = argParts[0];
				portPart = argParts[1];
			} else if (argParts[0].indexOf('.') >= 0) {
				addrPart = argParts[0];
			} else {
				portPart = argParts[0];
			}
			if (addrPart != null) {
				serverAddress = InetAddress.getByName(addrPart);
			}
			if (portPart != null) {
				port = Integer.parseInt(portPart);
			}
		}

		CommandServer server = new CommandServer(serverAddress, port);
		Thread t = new Thread(server);
		t.setName("CommandServer(" + serverAddress + ", " + port + ")");
		t.start();

		Runtime.getRuntime().addShutdownHook(new NGServerShutdowner(server));
		
		// if the port is 0, it will be automatically determined.
		// add this little wait so the ServerSocket can fully
		// initialize and we can see what port it chose.
		int runningPort = server.getPort();
		while (runningPort == 0) {
			try { Thread.sleep(50); } catch (Throwable toIgnore) {}
			runningPort = server.getPort();
		}
		
		System.out.println("CommandServer started on "
							+ ((serverAddress == null) 
								? "all interfaces" 
								: serverAddress.getHostAddress())
		                    + ", port " 
							+ runningPort 
							+ ".");
	}
	
	@SuppressWarnings("unused")
	private static class debug {
      public static void out(Object s) {  System.out.println((String)s); }
    }

	/**
	 * A shutdown hook that will cleanly bring down the CommandServer if it
	 * is interrupted.
	 * 
	 * @author <a href="http://www.martiansoftware.com/contact.html">Marty Lamb</a>
	 */
	protected static class NGServerShutdowner extends Thread {
		private CommandServer server = null;
		
		NGServerShutdowner(CommandServer server) {
			this.server = server;
		}
		
		
		public void run() {
			
			int count = 0;
			server.shutdown(false);
			
			// give the server up to five seconds to stop.  is that enough?
			// remember that the shutdown will call nailShutdown in any
			// nails as well
			while (server.isRunning() && (count < 50)) {

				try {Thread.sleep(100);} catch(InterruptedException e) {}
				++count;
			}
			
			if (server.isRunning()) {
				System.err.println("Unable to cleanly shutdown server.  Exiting JVM Anyway.");
			} else {
				System.out.println("CommandServer shut down.");
			}
		}
	}
}

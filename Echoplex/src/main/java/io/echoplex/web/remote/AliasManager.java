

package io.echoplex.web.remote;

import java.io.FileReader;
import java.util.Iterator;
import java.util.Map;
import java.util.Properties;
import java.util.Set;

import org.apache.log4j.Logger;
import org.springframework.core.io.ClassPathResource;

/**
 * An AliasManager is used to store and lookup command Aliases by name.
 * See <a href="Alias.html">Alias</a> for more details.
 * 
 * @author <a href="http://www.martiansoftware.com/contact.html">Marty Lamb</a>
 */
public class AliasManager {
	
	private static Logger log = Logger.getLogger(AliasManager.class);
	private Map<String, Alias> aliases;
	
	/**
	 * Creates a new AliasManager, populating it with
	 * default Aliases.
	 */
	public AliasManager() {
		aliases = new java.util.HashMap<String, Alias>();
		
		try {
			Properties props = new Properties();
			props.load(new FileReader(new ClassPathResource("halo.properties").getFile()));
			loadFromProperties(props);
		} catch (java.io.IOException e) {
			log.error("Unable to load halo.properties: " + e.getMessage());
		}
		
//		try {
//            Properties props = new Properties();
//            props.load(getClass().getClassLoader().getResourceAsStream("resources/commands.properties"));
//            loadFromProperties(props);
//        } catch (java.io.IOException e) {
//            log.error("Unable to load Aliaes:  " + e.getMessage());
//        }
	}
	
	/**
	 * Loads Aliases from a java.util.Properties file located at the
	 * specified URL.  The properties must be of the form:
	 * <pre><code>[alias name]=[fully qualified classname]</code></pre>
	 * each of which may have an optional
	 * <pre><code>[alias name].desc=[alias description]</code></pre>
	 * 
	 * For example, to create an alias called "<code>myprog</code>" for
	 * class <code>com.mydomain.myapp.MyProg</code>, the following properties
	 * would be defined:
	 * 
	 * <pre><code>myprog=com.mydomain.myapp.MyProg
	 *myprog.desc=Runs my program.
	 * </code></pre>
	 * @param properties the Properties to load.
	 */
	public void loadFromProperties(java.util.Properties properties) {
		for (Iterator<?> i = properties.keySet().iterator(); i.hasNext();) {
			String key = (String) i.next();
			if (!key.endsWith(".desc")) {
				try {
					Class<?> clazz = Class.forName(properties.getProperty(key));
					String desc = properties.getProperty(key + ".desc", "");
					addAlias(new Alias(key, desc, clazz));
				} catch (ClassNotFoundException e) {
					System.err.println("Unable to locate class " + properties.getProperty(key));
				}
			}
		}
	}
	
	/**
	 * Adds an Alias, replacing any previous entries with the
	 * same name.
	 * @param alias the Alias to add
	 */
	public void addAlias(Alias alias) {
		synchronized (aliases) {
			aliases.put(alias.getName(), alias);
		}
	}
	
	/**
	 * Returns a Set that is a snapshot of the Alias list.
	 * Modifications to this Set will not impact the AliasManager
	 * in any way.
	 * @return a Set that is a snapshot of the Alias list.
	 */
	public Set<Alias> getAliases() {
		Set<Alias> result = new java.util.TreeSet<Alias>();
		synchronized(aliases) {
			result.addAll(aliases.values());
		}
		return (result);
	}

	/**
	 * Removes the Alias with the specified name from the AliasManager.
	 * If no such Alias exists in this AliasManager, this method has no effect.
	 * @param aliasName the name of the Alias to remove
	 */
	public void removeAlias(String aliasName) {
		synchronized (aliases) {
			aliases.remove(aliasName);
		}
	}

	/**
	 * Returns the Alias with the specified name
	 * @param aliasName the name of the Alias to retrieve
	 * @return the requested Alias, or null if no such Alias
	 * is defined in this AliasManager.
	 */
	public Alias getAlias(String aliasName) {
		return ((Alias) aliases.get(aliasName));
	}

}

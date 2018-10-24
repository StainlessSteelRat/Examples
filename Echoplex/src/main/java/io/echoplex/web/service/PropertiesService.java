package io.echoplex.web.service;

import java.io.File;
import java.io.FileInputStream;
import java.io.FileNotFoundException;
import java.io.IOException;
import java.io.PrintWriter;
import java.util.Date;
import java.util.LinkedList;
import java.util.List;
import java.util.Properties;

import javax.annotation.PostConstruct;
import javax.annotation.Resource;

import org.apache.log4j.Logger;
import org.springframework.core.io.ClassPathResource;
import org.springframework.stereotype.Service;

import io.echoplex.web.utils.Utils;

@Service
@Resource
public class PropertiesService {

	private static Logger log = Logger.getLogger(PropertiesService.class);
	private static PropertiesService me;
	private static List<String> versions = new LinkedList<String>();

	private Properties appProperties;

	private File propertiesFile;

	public PropertiesService() {
		me = this;
		registerVersion(Utils.VERSION);
	}

	@PostConstruct
	public void init() {
		try {
			propertiesFile = new ClassPathResource("application.properties").getFile();
			if (!(propertiesFile.exists() && propertiesFile.canWrite())) {
				log.error("Application Properties file doen't exist or is not writeable, persistance disabled");
			} else if (!propertiesFile.exists()) {
				log.error("Unable to locate Application Properties file");

			} else {
				if (appProperties == null) {
					appProperties = new Properties();
					appProperties.load(new FileInputStream(propertiesFile));
				}
			}

		} catch (FileNotFoundException e) {
			log.error("Unable to locate Application Properties file", e);
		} catch (IOException e) {
			log.error("Unable to load Application Properties file", e);
		}
		log.info("Application Properties Loaded");
	}

	public Properties getProperties() {
		return appProperties;
	}

	public void setAppProperties(Properties appProperties) {
		this.appProperties = appProperties;
	}

	public void saveProperties() {
		if (propertiesFile != null) {
			String comments = "Updated " + new Date().toString();

			PrintWriter writer;
			try {
				writer = new PrintWriter(propertiesFile);
				me.getProperties().store(writer, comments);
			} catch (FileNotFoundException e) {
				log.error(e.getMessage(), e);
			} catch (IOException e) {
				log.error(e.getMessage(), e);
			}
		} else {
			log.warn("Persistance has been disabled");
		}
	}

	public String getProperty(String key, String defaultValue) {
		log.trace("Looking for " + key);
		if (me == null) {
			log.error("Properties not initiated yet");
			return defaultValue;
		}
		if (me.getProperties() == null || me.getProperties().getProperty(key) == null) {
			return defaultValue;
		}
		return getMe().getProperties().getProperty(key);
	}

	// --------
	// Static methods

	public static String getPropertyStatic(String key, String defaultValue) {
		return getMe().getProperty(key, defaultValue);
	}

	public static Properties getPropertiesStatic() {
		return getMe().getProperties();
	}

	public static void setProperties(Properties appProperties) {
		getMe().setAppProperties(appProperties);
	}

	public static void persist() {
		getMe().saveProperties();
	}

	public static PropertiesService getMe() {
		while (me == null) {
			log.warn("Waiting for Properties Service Initialisztion");
			try {
				Thread.sleep(2000);
			} catch (Exception e) {
			}
		}
		return me;
	}

	public void registerVersion(String version) {
		versions.add(version);
	}

	public List<String> getVersions() {
		return versions;
	}

	public String getVersionsAsString() {
		return versions.toString();
	}
}

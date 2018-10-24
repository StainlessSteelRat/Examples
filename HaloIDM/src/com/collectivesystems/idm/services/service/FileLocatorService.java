package com.collectivesystems.idm.services.service;

import java.io.File;
import java.util.LinkedList;
import java.util.List;

import javax.annotation.PostConstruct;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;

import com.collectivesystems.core.helpers.Utils;
import com.collectivesystems.core.services.service.PropertiesService;
import com.collectivesystems.core.ui.providers.HaloUI;
import com.vaadin.ui.UI;

@Service
public class FileLocatorService {
	final Logger log = LoggerFactory.getLogger(FileLocatorService.class);
	final static String PATH_PROPERTY = "idm.file.store.path";
	final static String PATH_PROPERTY_DEFAUT = "/apps/ident/files/";
	
	@Autowired
	PropertiesService properties;
	
	// Do we need to extract the paths from the properties file and cache them?
	List<String> paths = new LinkedList<String>();
	

	@PostConstruct
	public void init() {
		
	}
	
	public String locateFile(String filename) {

		String[] paths = properties.getProperty(PATH_PROPERTY, PATH_PROPERTY_DEFAUT).split(":");
		for (String path : paths) {
			File f = new File(path  + "/" + filename);
			if (f.exists()) {
				return f.getAbsolutePath();
			}
		}
		log.error("Unabled to loacte file [" + filename + "]");
		return "file-not-found";
	}
	
	public String locatePathToFile(String filename) {

		String[] paths = properties.getProperty(PATH_PROPERTY, PATH_PROPERTY_DEFAUT).split(":");
		for (String path : paths) {
			File f = new File(path  + "/" + filename);
			if (f.exists()) {
				return path + "/";
			}
		}
		log.error("Unabled to locate file path [" + filename + "]");
		return "file-not-found";
	}
	
	public String placeFile(String filename) {
		String current_choice = "";
		long freespace = 0l;
		String[] paths = properties.getProperty(PATH_PROPERTY, PATH_PROPERTY_DEFAUT).split(":");
		for (String path : paths) {
			File f = new File(path);
			if (log.isTraceEnabled()) { log.trace("Path: " + f.getAbsolutePath() + " " + Utils.humanReadableByteCount(f.getFreeSpace(), true)); }
			long space = f.getFreeSpace();
			if (space > freespace) {
				freespace = space;
				current_choice =  f.getAbsolutePath();
			}
		}
		return current_choice + "/" + filename;
	}
	
	public String placePathToFile(String filename) {
		String current_choice = "";
		long freespace = 0l;
		String[] paths = properties.getProperty(PATH_PROPERTY, PATH_PROPERTY_DEFAUT).split(":");
		for (String path : paths) {
			File f = new File(path);
			long space = f.getFreeSpace();
			if (space > freespace) {
				freespace = space;
				current_choice =  f.getAbsolutePath();
			}
		}
		return current_choice  + "/";
	}

	public void createDirectory() {
		try {
		String[] paths = properties.getProperty(PATH_PROPERTY, PATH_PROPERTY_DEFAUT).split(":");
		for (String path : paths) {
			File f = new File(path + ((HaloUI)UI.getCurrent()).getUsername() + "/");
			if (!f.exists()) {
				f.mkdir();
				log.info("Created directory " + f.getAbsolutePath());
			}
		}
		} catch (Exception e) {
			log.error("Unabled to allocate directory [" + ((HaloUI)UI.getCurrent()).getUsername() + "]");
		}
		
	}

	public void createDirectory(String username) {
		try {
			String[] paths = properties.getProperty(PATH_PROPERTY, PATH_PROPERTY_DEFAUT).split(":");
			for (String path : paths) {
				File f = new File(path + username + "/");
				if (!f.exists()) {
					f.mkdir();
					log.info("Created directory " + f.getAbsolutePath());
				}
			}
			} catch (Exception e) {
				log.error("Unabled to allocate directory [" + username + "]");
			}
		
	}
	
	
}

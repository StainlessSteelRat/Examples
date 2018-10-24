package com.collectivesystems.idm.services.service;

import java.util.HashMap;
import java.util.Map;

import javax.annotation.PostConstruct;

import org.apache.log4j.Logger;
import org.springframework.stereotype.Service;

import com.collectivesystems.core.beans.Flag;

@Service
public class FlagService {

	private static Logger log = Logger.getLogger(FlagService.class);
	
	public static final String HALO_AGENT = "haloAgent";
	public static final String FLAG_UNSET = "--";
	public final static String DEFAUT_NAMESPACE = "global";
	
	public static FlagService service;
	
	protected Map<String, Map<String, Flag>> flags = new HashMap<>();
	private String DEFAULT_NAMESPACE;
	
	@PostConstruct
	public void init(){
		service = this;
	}
	
	public Flag getFlag(String name) {
		return getFlag(DEFAULT_NAMESPACE, name);
	}
	
	public Flag getFlag(String namespace, String name) {
		Flag flag = null;
		Map<String, Flag> flagmap = flags.get(namespace);
		if (flagmap == null) { flagmap = new HashMap<>(); flags.put(namespace, flagmap); }
		if ((flag = flagmap.get(name)) == null) {
			flag = new Flag();
			flag.setValue(FLAG_UNSET);
			synchronized (flagmap) {
				flagmap.put(name, flag);
			}
		}
		return flag;
	}

	
	public void waitFlag(String flagname) { 
		waitFlag(DEFAUT_NAMESPACE, flagname);
	}
	
	public void waitFlag(String namespace, String flagname) {
		Flag flag = getFlag(namespace, flagname);
		while (flag.getValue().equals(FLAG_UNSET))
			synchronized (flag) {
				if (log.isDebugEnabled()) { log.debug("Waiting for " + flagname + "[" + flag.toString() + "]"); }
				try {
					flag.wait();
					if (log.isDebugEnabled()) { log.debug(flagname + " notified, '" + flag.getValue() + "'."); }
				} catch (InterruptedException e) {
					log.error(e.toString());
				}
			}
	}
	
	public void setFlag(String name, String value, String signaller) {
		setFlag(DEFAUT_NAMESPACE, name, value, signaller);
	}
	public void setFlag(String namespace, String name, String value, String signaller) {
		Flag flag = getFlag(namespace, name);
		log.debug("Setting flag: " + name);
		synchronized (flag) {
			flag.setSignaller(signaller);
			flag.setValue(value);
			flag.notifyAll();
		}
		log.debug("Flag " + name + " set.");
	}

	public void resetFlag(String name) {
		resetFlag(DEFAULT_NAMESPACE, name);
	}
	
	public  void resetFlag(String namespace, String name) {
		Flag flag = getFlag(namespace, name);
		log.debug("Setting flag: " + name);
		synchronized (flag) {
			flag.setSignaller("");
			flag.setValue(FLAG_UNSET);
			flag.notifyAll();
		}
		log.debug("Flag " + name + " reset.");
	}
	
	public void removeFlag(String name) {
		removeFlag(DEFAULT_NAMESPACE, name);
	}

	public void removeFlag(String namespace, String name) {
		Map<String, Flag> flagmap = flags.get(namespace);
		synchronized (flagmap) { 
				flagmap.remove(name);
		}
		synchronized (flags) {
			if (flagmap.isEmpty()) {
				flags.remove(namespace);
			}
		}
	}
}

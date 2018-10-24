package com.collectivesystems.idm.services.service;


import java.text.SimpleDateFormat;

import javax.annotation.PostConstruct;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;

import com.collectivesystems.core.services.service.PropertiesService;

@Service
public class Globals {
	final static String VERSION = "halo.idm.1.0.2";
	
	public final static SimpleDateFormat df = new SimpleDateFormat("dd-MM-yyyy");
	public final static SimpleDateFormat df_long = new SimpleDateFormat("dd MMM yyyy");
	public final static SimpleDateFormat df_withTime = new SimpleDateFormat("HH:mm:ss dd-MM-yyyy");
	public final static SimpleDateFormat df_month = new SimpleDateFormat("MMM");
	
	@Autowired
	PropertiesService properties;
	
	@PostConstruct
	public void init() {
		properties.registerVersion(VERSION);
	}
}

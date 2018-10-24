package com.collectivesystems.idm.services.web;

import java.io.File;
import java.io.IOException;
import java.nio.file.Files;

import javax.annotation.PostConstruct;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.HttpHeaders;
import org.springframework.http.HttpStatus;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.stereotype.Controller;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RequestHeader;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestMethod;

import com.collectivesystems.core.dao.CSDAO;
import com.collectivesystems.idm.beans.sgd.Tracked;



@Controller
public class Tracker {

	byte[] fileContent;
	final HttpHeaders headers = new HttpHeaders();
	 
	@Autowired
	CSDAO dao;
	    
	@PostConstruct
	public void init() {
	    File fi = new File(getClass().getClassLoader().getResource("/resources/120.png").getFile());
		try {
			fileContent = Files.readAllBytes(fi.toPath());
		} catch (IOException e) {
			// TODO Auto-generated catch block
			e.printStackTrace();
		}
		headers.setContentType(MediaType.IMAGE_PNG);
	}
	
	@RequestMapping(method = RequestMethod.GET, value = "/tls/{env}/{username}")
	public ResponseEntity<byte[]> getImage(@RequestHeader(value="User-Agent", defaultValue="Unknown") String userAgent, @PathVariable("env") String env, @PathVariable("username") String username) throws IOException {
		
		Tracked tracked = new Tracked();
		tracked.setUsername(username);
		tracked.setEnv(env);
		tracked.setUseragent(userAgent);
		dao.save(tracked);

	    return new ResponseEntity<byte[]>(fileContent, headers, HttpStatus.OK);
	}

	
}

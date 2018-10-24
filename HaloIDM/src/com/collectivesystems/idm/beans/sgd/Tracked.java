package com.collectivesystems.idm.beans.sgd;

import javax.persistence.Entity;
import javax.persistence.Table;

import com.collectivesystems.core.beans.dao.TimestampEntity;

@Entity
@Table(name="m_tracked")
public class Tracked extends TimestampEntity {

	private String env;
	private String username;
	private String useragent;
	
	public String getEnv() {
		return env;
	}
	public void setEnv(String env) {
		this.env = env;
	}
	public String getUsername() {
		return username;
	}
	public void setUsername(String username) {
		this.username = username;
	}
	public String getUseragent() {
		return useragent;
	}
	public void setUseragent(String useragent) {
		this.useragent = useragent;
	}
	
	
}

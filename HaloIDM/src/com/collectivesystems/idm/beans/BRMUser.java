package com.collectivesystems.idm.beans;

import java.io.Serializable;
import java.sql.SQLException;
import java.util.List;

import org.hibernate.HibernateException;
import org.hibernate.Session;
import org.springframework.orm.hibernate3.HibernateCallback;

import com.collectivesystems.core.beans.dao.TimestampEntity;
import com.collectivesystems.core.dao.CSDAOImpl;

public class BRMUser extends TimestampEntity implements Serializable 	{
	private static final long serialVersionUID = 5664344602650222848L;
	

	
	
	private String username;
	private String requester;
	private String approver;
	
	private int status;
	private int msg;
	
	@SuppressWarnings("unchecked")
	public static List<BRMUser> getEntries(final int status) {
		return (List<BRMUser>) CSDAOImpl.getDAO().getHibernateTemplate().execute(new HibernateCallback<Object>() {
            public Object doInHibernate(Session session) throws HibernateException, SQLException {
            	 return session.createQuery("from UserRequest j where j.status = :status order by j.created ASC")
            			 .setParameter(":status", status).list();
            }
        });
	}
	
	
	

	public String getUsername() {
		return username;
	}

	public void setUsername(String username) {
		this.username = username;
	}

	public String getRequester() {
		return requester;
	}

	public void setRequester(String requester) {
		this.requester = requester;
	}

	public String getApprover() {
		return approver;
	}

	public void setApprover(String approver) {
		this.approver = approver;
	}

	public int getStatus() {
		return status;
	}

	public void setStatus(int status) {
		this.status = status;
	}
	
	
	

	public int getMsg() {
		return msg;
	}

	public void setMsg(int msg) {
		this.msg = msg;
	}
}

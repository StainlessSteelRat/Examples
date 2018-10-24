package com.collectivesystems.idm.beans;

import java.io.Serializable;
import java.sql.SQLException;
import java.util.Date;
import java.util.List;

import javax.persistence.Entity;
import javax.persistence.Table;

import org.hibernate.HibernateException;
import org.hibernate.Session;
import org.springframework.orm.hibernate3.HibernateCallback;

import com.collectivesystems.core.beans.dao.TimestampEntity;
import com.collectivesystems.core.dao.CSDAOImpl;
@Entity
@Table(name="m_homedirrequests")
public class HomeDirRequest extends TimestampEntity implements Serializable 	{
	private static final long serialVersionUID = 5664344702650222848L;
	
	public final static int STATUS_REQUESTED = 0;
	public final static int STATUS_CREATING = 1;
	public final static int STATUS_CREATED = 2;
	public final static int STATUS_ERROR = 3;
	public static final int STATUS_CANCELLED = 4;

	public static final String[] STATUS_NAMES = { "Request Created", "Creating Home Directory", "Home Directory Created", "Error", "Cancelled" };


	private String homeDirectory;
	private String username;
	private String requester;
	private String requesterEmail;
	private String approver;
	private String approverEmail;
	
	private int status;
	private int msg;
	
	@SuppressWarnings("unchecked")
	public static List<HomeDirRequest> getEntries(final int status) {
		return (List<HomeDirRequest>) CSDAOImpl.getDAO().getHibernateTemplate().execute(new HibernateCallback<Object>() {
            public Object doInHibernate(Session session) throws HibernateException, SQLException {
            	 return session.createQuery("from HomeDirRequest j where j.status = :status order by j.created ASC")
            			 .setParameter("status", status).list();
            }
        });
	}
	
	@SuppressWarnings("unchecked")
	public static List<HomeDirRequest> getEntriesByRequester(final int status) {
		return (List<HomeDirRequest>) CSDAOImpl.getDAO().getHibernateTemplate().execute(new HibernateCallback<Object>() {
            public Object doInHibernate(Session session) throws HibernateException, SQLException {
            	 return session.createQuery("from HomeDirRequest j where j.status = :status order by j.created ASC")
            			 .setParameter("status", status).list();
            }
        });
	}
	
	@SuppressWarnings("unchecked")
	public static List<HomeDirRequest> getEntriesByDate(final Date date) {
		return (List<HomeDirRequest>) CSDAOImpl.getDAO().getHibernateTemplate().execute(new HibernateCallback<Object>() {
            public Object doInHibernate(Session session) throws HibernateException, SQLException {
            	 return session.createQuery("from HomeDirRequest j where j.created > :date order by j.created ASC")
            			 .setParameter("date", date).list();
            }
        });
	}
	
	@SuppressWarnings("unchecked")
	public static List<HomeDirRequest> getEntriesByRequesterAndDate(final String username, final Date date) {
		return (List<HomeDirRequest>) CSDAOImpl.getDAO().getHibernateTemplate().execute(new HibernateCallback<Object>() {
            public Object doInHibernate(Session session) throws HibernateException, SQLException {
            	 return session.createQuery("from HomeDirRequest j where j.requester = :username and j.created > :date order by j.created ASC")
            			 .setParameter("username", username)
            			 .setParameter("date", date)
            			 .list();
            }
        });
	}
	
	public void init() {
		homeDirectory = "";
		username = "";
		requester = "";
		requesterEmail = "";
		approver = "";
		approverEmail = "";
		status = -1;
	}

	public String getHomeDirectory() {
		return homeDirectory;
	}

	public void setHomeDirectory(String homeDirectory) {
		this.homeDirectory = homeDirectory;
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

	public String getRequesterEmail() {
		return requesterEmail;
	}

	public void setRequesterEmail(String requesterEmail) {
		this.requesterEmail = requesterEmail;
	}

	public String getApprover() {
		return approver;
	}

	public void setApprover(String approver) {
		this.approver = approver;
	}

	public String getApproverEmail() {
		return approverEmail;
	}

	public void setApproverEmail(String approverEmail) {
		this.approverEmail = approverEmail;
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
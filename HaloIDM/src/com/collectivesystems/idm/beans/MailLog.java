package com.collectivesystems.idm.beans;

import java.sql.SQLException;
import java.util.List;

import javax.persistence.Entity;
import javax.persistence.Table;

import org.hibernate.HibernateException;
import org.hibernate.Session;
import org.springframework.orm.hibernate3.HibernateCallback;

import com.collectivesystems.core.beans.dao.TimestampEntity;
import com.collectivesystems.core.dao.CSDAOImpl;

@Entity
@Table(name="m_maillog")
public class MailLog extends TimestampEntity {
	
	private String username;
	private String email;
	private String mailTemplate;
	private String fullname;
	
	
	public String getUsername() {
		return username;
	}
	public void setUsername(String username) {
		this.username = username;
	}
	public String getEmail() {
		return email;
	}
	public void setEmail(String email) {
		this.email = email;
	}
	public String getMailTemplate() {
		return mailTemplate;
	}
	public void setMailTemplate(String mailTemplate) {
		this.mailTemplate = mailTemplate;
	}
	
	
	public String getFullname() {
		return fullname;
	}
	public void setFullname(String fullname) {
		this.fullname = fullname;
	}
	
	@SuppressWarnings("unchecked")
	public static List<String> getMailLogUserList(final String mailtemplate) {
		return (List<String>) CSDAOImpl.getDAO().getHibernateTemplate().execute(new HibernateCallback<Object>() {
            public Object doInHibernate(Session session) throws HibernateException, SQLException {
            	 return session.createQuery("select username from MailLog q where q.mailTemplate = :text")
                		.setParameter("text", mailtemplate).list();                
            }
        });
	}
	

	public static void deleteAllMailLogEntries(final String template) {
		 CSDAOImpl.getDAO().getHibernateTemplate().execute(new HibernateCallback<Object>() {
            public Object doInHibernate(Session session) throws HibernateException, SQLException {
                return session.createQuery("delete from MailLog q where q.mailTemplate = :text").setParameter("text", template).executeUpdate();               
            }
        });
		
	}
}
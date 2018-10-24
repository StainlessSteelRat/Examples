

package com.collectivesystems.idm.beans;

import java.io.Serializable;
import java.sql.SQLException;
import java.util.Date;
import java.util.List;

import javax.persistence.Entity;
import javax.persistence.Table;
import javax.persistence.Transient;

import org.hibernate.HibernateException;
import org.hibernate.Session;
import org.springframework.orm.hibernate3.HibernateCallback;

import com.collectivesystems.core.beans.dao.TimestampEntity;
import com.collectivesystems.core.dao.CSDAOImpl;
import com.collectivesystems.core.services.service.PropertiesService;
import com.ibm.icu.util.Calendar;
@Entity
@Table(name="m_userrequests")
public class UserRequest extends TimestampEntity implements Serializable, UserRequestBase 	{
	private static final long serialVersionUID = 5664344702650222848L;
	
	public final static int STATUS_REQUESTED = 0;
	public final static int STATUS_PENDING_APPROVAL = 1;
	public final static int STATUS_APPROVED = 2;
	public final static int STATUS_CREATING = 3;
	public final static int STATUS_CREATED = 4;
	public final static int STATUS_TERMINATED = 5;
	public final static int STATUS_DELETED = 6;
	public final static int STATUS_REJECTED = 7;
	public final static int STATUS_ERROR = 8;
	public static final int STATUS_CANCELLED = 9;

	public static final String[] STATUS_NAMES = { "Request Created", "Pending Approval", "Approved", "Creating Account", "Account Created", "Terminated", "Deleted", "Rejected", "Error", "Cancelled", "Notified", 
			"Password Reset", "Password Reset", "Updating", "Updated", "Enabled", "Expiring", "Expiry Notification", "Disabling Account", "Disabling BRM Account", "User Missing" };

	public static final int STATUS_NOTIFIED = 10;
	
	public static final int STATUS_USER_PW_RESET = 11;
	public static final int STATUS_BRM_PW_RESET = 12;
	public static final int STATUS_UPDATING = 13;
	public static final int STATUS_UPDATED = 14;
	public static final int STATUS_ENABLED = 15;
	public static final int STATUS_EXPIRING_USER = 16;
	public static final int STATUS_EXPIRING_REQUESTER = 17;
	public static final int STATUS_DISABLED_ACCOUNT = 18;
	public static final int STATUS_DISABLED_ACCOUNT_BRM = 19;
	
	public static final String[] ACTION_NAMES = { "Create User", "Update User", "ERROR", "", "Enable Account", "Create Requester", "Expiring", "Account Disabled", "Deleted Group", "Deleted User" };
	public static final int ACTION_CREATE = 0;
	public static final int ACTION_UPDATE = 1;
	public static final int ACTION_ERROR = 2;
	public static final int ACTION_ENABLE = 4;
	public static final int ACTION_CREATE_REQUESTER = 5;
	public static final int ACTION_EXPIRY = 6;
	public static final int ACTION_ACCOUNT_DISABLED = 7;
	public static final int ACTION_GROUP_DELETED = 8;

	public static final int STATUS_USER_MISSING = 20;

	public static final int ACTION_USER_DELETED = 9;

	protected String fname;
	protected String sname;
	protected String employeeID;
	protected String exEmail;
	protected String intEmail;
	protected String phone;
	protected String ggroup;
	protected String ntlogin;
	protected String environment;
	protected Date startDate;
	protected Date endDate;
	protected String organisation;

	protected String username;
	protected String requester;
	protected String requesterEmail;
	protected String approver;
	protected String approverEmail;

	protected int status;
	protected int action;
	protected String msg;
	protected String businessJustification;
	
	
	
	@SuppressWarnings("unchecked")
	public static List<UserRequest> getEntries(final int status) {
		return (List<UserRequest>) CSDAOImpl.getDAO().getHibernateTemplate().execute(new HibernateCallback<Object>() {
            public Object doInHibernate(Session session) throws HibernateException, SQLException {
            	 return session.createQuery("from UserRequest j where j.status = :status order by j.created DESC")
            			 .setParameter("status", status).list();
            }
        });
	}
	
	@SuppressWarnings("unchecked")
	public static List<UserRequest> getEntriesByRequester(final int status) {
		return (List<UserRequest>) CSDAOImpl.getDAO().getHibernateTemplate().execute(new HibernateCallback<Object>() {
            public Object doInHibernate(Session session) throws HibernateException, SQLException {
            	 return session.createQuery("from UserRequest j where j.status = :status order by j.created DESC")
            			 .setParameter("status", status).list();
            }
        });
	}
	
	@SuppressWarnings("unchecked")
	public static List<UserRequest> getEntriesByDate(final Date date) {
		return (List<UserRequest>) CSDAOImpl.getDAO().getHibernateTemplate().execute(new HibernateCallback<Object>() {
            public Object doInHibernate(Session session) throws HibernateException, SQLException {
            	 return session.createQuery("from UserRequest j where j.created > :date order by j.created DESC")
            			 .setParameter("date", date).list();
            }
        });
	}
	
	@SuppressWarnings("unchecked")
	public static List<UserRequest> getEntriesByDateIgnore(final Date date, final String msg) {
		return (List<UserRequest>) CSDAOImpl.getDAO().getHibernateTemplate().execute(new HibernateCallback<Object>() {
            public Object doInHibernate(Session session) throws HibernateException, SQLException {
            	 return session.createQuery("from UserRequest j where j.created > :date and j.msg <> :msg order by j.created DESC")
            			 .setParameter("date", date).setParameter("msg", msg).list();
            }
        });
	}
	
	@SuppressWarnings("unchecked")
	public static List<UserRequest> getEntriesByRequesterAndDate(final String username, final Date date) {
		return (List<UserRequest>) CSDAOImpl.getDAO().getHibernateTemplate().execute(new HibernateCallback<Object>() {
            public Object doInHibernate(Session session) throws HibernateException, SQLException {
            	 return session.createQuery("from UserRequest j where j.requester = :username and j.created > :date order by j.created DESC")
            			 .setParameter("username", username)
            			 .setParameter("date", date)
            			 .list();
            }
        });
	}
	
	@SuppressWarnings("unchecked")
	public static List<UserRequest> getEntriesByStatusAndApprover(final int status, final String approver) {
		return (List<UserRequest>) CSDAOImpl.getDAO().getHibernateTemplate().execute(new HibernateCallback<Object>() {
            public Object doInHibernate(Session session) throws HibernateException, SQLException {
            	 return session.createQuery("from UserRequest j where j.status = :status and j.approver = :approver order by j.created ASC")
            			 .setParameter("status", status)
            			 .setParameter("approver", approver).list();
            }
        });
	}
	
	@SuppressWarnings("unchecked")
	public static List<UserRequest> getEntriesByApproverAndDate(final String approver, final Date date) {
		return (List<UserRequest>) CSDAOImpl.getDAO().getHibernateTemplate().execute(new HibernateCallback<Object>() {
            public Object doInHibernate(Session session) throws HibernateException, SQLException {
            	 return session.createQuery("from UserRequest j where j.approver = :approver and j.created > :date order by j.created ASC")
            			 .setParameter("approver", approver)
            			 .setParameter("date", date)
            			 .list();
            }
        });
	}
	@SuppressWarnings("unchecked")
	public static List<UserRequest> getEntriesByApprover(final String approver) {
		return (List<UserRequest>) CSDAOImpl.getDAO().getHibernateTemplate().execute(new HibernateCallback<Object>() {
            public Object doInHibernate(Session session) throws HibernateException, SQLException {
            	 return session.createQuery("from UserRequest j where j.approver = :approver order by j.created ASC")
            			 .setParameter("approver", approver)
            			 .list();
            }
        });
	}
	
	public static Long countApproversByDate(Date date) {
		return (Long) CSDAOImpl.getDAO().getHibernateTemplate().execute(new HibernateCallback<Object>() {
            public Object doInHibernate(Session session) throws HibernateException, SQLException {
            	 return session.createQuery("select count(*) from UserRequest j where j.updated > :date and j.action = 0 and j.status = 10 and username like '%_BRM' order by j.created ASC")
            			 .setParameter("date", date)
            			 .uniqueResult();
            }
        });
	}
	
	public static Long countApproversByDateRange(Date from, Date to) {
		return (Long) CSDAOImpl.getDAO().getHibernateTemplate().execute(new HibernateCallback<Object>() {
            public Object doInHibernate(Session session) throws HibernateException, SQLException {
            	 return session.createQuery("select count(*) from UserRequest j where j.updated > :date and j.updated < :date2 and j.action = 0 and j.status = 10 and username like '%_BRM' order by j.created ASC")
            			 .setParameter("date", from)
            			 .setParameter("date2", to)
            			 .uniqueResult();
            }
        });
	}
	
	public static Long countUsersByDate(Date date) {
		return (Long) CSDAOImpl.getDAO().getHibernateTemplate().execute(new HibernateCallback<Object>() {
            public Object doInHibernate(Session session) throws HibernateException, SQLException {
            	 return session.createQuery("select count(*) from UserRequest j where j.updated > :date and j.action = 0 and j.status = 10 and username not like '%_BRM' order by j.created ASC")
            			 .setParameter("date", date)
            			 .uniqueResult();
            }
        });
	}
	
	public static Long countUsersByDateRange(Date from, Date to) {
		return (Long) CSDAOImpl.getDAO().getHibernateTemplate().execute(new HibernateCallback<Object>() {
            public Object doInHibernate(Session session) throws HibernateException, SQLException {
            	 return session.createQuery("select count(*) from UserRequest j where j.updated > :date and j.updated < :date2 and j.action = 0 and j.status = 10 and username not like '%_BRM' order by j.created ASC")
            			 .setParameter("date", from)
            			 .setParameter("date2", to)
            			 .uniqueResult();
            }
        });
	}
	
	public static Long countRequestersByDate(Date date) {
		return (Long) CSDAOImpl.getDAO().getHibernateTemplate().execute(new HibernateCallback<Object>() {
            public Object doInHibernate(Session session) throws HibernateException, SQLException {
            	 return session.createQuery("select count(*) from UserRequest j where j.updated > :date and j.action = 5 and j.status = 10 order by j.created ASC")
            			 .setParameter("date", date)
            			 .uniqueResult();
            }
        });
	}
	
	public static Long countRequestersByDateRange(Date from, Date to) {
		return (Long) CSDAOImpl.getDAO().getHibernateTemplate().execute(new HibernateCallback<Object>() {
            public Object doInHibernate(Session session) throws HibernateException, SQLException {
            	 return session.createQuery("select count(*) from UserRequest j where j.updated > :date and j.updated < :date2 and j.action = 5 and j.status = 10 order by j.created ASC")
            			 .setParameter("date", from)
            			 .setParameter("date2", to)
            			 .uniqueResult();
            }
        });
	}
	
	@SuppressWarnings("unchecked")
	public static List<UserRequest> getEntriesByUserDetails(final String group, final String email) {
		return (List<UserRequest>) CSDAOImpl.getDAO().getHibernateTemplate().execute(new HibernateCallback<Object>() {
            public Object doInHibernate(Session session) throws HibernateException, SQLException {
            	 return session.createQuery("from UserRequest j where j.ggroup = :group  and j.exEmail = :exEmail and j.action = 0 and (j.status < 5 or j.status = 10) order by j.created ASC")
            			 .setParameter("group", group)
            			 .setParameter("exEmail", email)
            			 .list();
            }
        });
	}

	
	
	public UserRequest init() {
		fname = "";
		sname = "";
		employeeID = "";
		exEmail = "";
		intEmail = "";
		phone = "";
		ggroup = "";
		ntlogin = "";
		environment = "";
		startDate = new Date();
		Calendar c = Calendar.getInstance();
		c.add(Calendar.DAY_OF_YEAR, Integer.parseInt(PropertiesService.getPropertyStatic("idm.default.account.length", "90")));
		
		endDate = c.getTime();
		
		username = "";
		requester = "";
		requesterEmail = "";
		approver = "";
		approverEmail = "";
		status = -1;
		msg = "";
		businessJustification = "";
		return this;
	}
	
	public UserRequest init(LDAPUser user) {
		fname = user.getGivenName();
		sname = user.getSn();
		employeeID = user.getEmployeeNo();
		exEmail = user.getEmail();
		intEmail = user.getIntEmail();
		phone = user.getPhoneNo();
		ggroup = user.getIsMemberOf();
		ntlogin = "";
		environment = "";
		startDate = user.getStartDate();
		endDate = user.getEndDate();
		
		username = user.getUid();
		requester = "";
		requesterEmail = "";
		approver = "";
		approverEmail = "";
		status = -1;
		msg = "";
		businessJustification = "";
		return this;
	}
	
	@Override
	public String toString() {
		return "UserRequest [fname=" + fname + ", sname=" + sname + ", employeeID=" + employeeID + ", exEmail=" + exEmail + ", intEmail=" + intEmail
				+ ", phone=" + phone + ", ggroup=" + ggroup + ", ntlogin=" + ntlogin + ", environment=" + environment + ", startDate=" + startDate
				+ ", endDate=" + endDate + ", organisation=" + organisation + ", username=" + username + ", requester=" + requester + ", requesterEmail="
				+ requesterEmail + ", approver=" + approver + ", approverEmail=" + approverEmail + ", status=" + status + ", msg=" + msg + ", getCreated()="
				+ getCreated() + ", getUpdated()=" + getUpdated() + ", getId()=" + getId() + ", hashCode()=" + hashCode() + "]";
	}

	@Transient
	public String getFullname() { return fname + " " + sname; }
	

	@Override
	public int hashCode() {
		final int prime = 31;
		int result = super.hashCode();
		result = (int) (prime * result + id);
		return result;
	}

	@Override
	public boolean equals(Object obj) {
		if (this == obj) { return true; }
		if (!super.equals(obj)) { return false; }
		if (getClass() != obj.getClass()) { return false; }
		UserRequest other = (UserRequest) obj;
		if (id != other.id) { return false; }
		return true;
	}

	@Override
	public String getUsername() {
		return username;
	}
	@Override
	public void setUsername(String username) {
		this.username = username;
	}
	@Override
	public String getRequester() {
		return requester;
	}
	@Override
	public void setRequester(String requester) {
		this.requester = requester;
	}
	@Override
	public String getApprover() {
		return approver;
	}
	@Override
	public void setApprover(String approver) {
		this.approver = approver;
	}
	@Override
	public int getStatus() {
		return status;
	}
	@Override
	public void setStatus(int status) {
		this.status = status;
	}
	@Override
	public String getMsg() {
		return msg;
	}
	@Override
	public void setMsg(String msg) {
		this.msg = msg;
	}
	@Override
	public String getFname() {
		return fname;
	}
	@Override
	public void setFname(String fname) {
		this.fname = fname == null ? "" : fname.trim();
	}
	@Override
	public String getSname() {
		return sname;
	}
	@Override
	public void setSname(String sname) {
		this.sname = sname == null ? "" : sname.trim();
	}
	@Override
	public String getEmployeeID() {
		return employeeID;
	}
	@Override
	public void setEmployeeID(String employeeID) {
		this.employeeID = employeeID == null ? "" : employeeID.trim();
	}
	@Override
	public String getExEmail() {
		return exEmail;
	}
	@Override
	public void setExEmail(String exEmail) {
		this.exEmail = exEmail.trim();
	}
	@Override
	public String getIntEmail() {
		return intEmail;
	}
	@Override
	public void setIntEmail(String intEmail) {
		this.intEmail = intEmail == null ? "" : intEmail.trim();
	}
	@Override
	public String getPhone() {
		return phone;
	}
	@Override
	public void setPhone(String phone) {
		this.phone = phone == null ? "" : phone.trim();
	}
	@Override
	public String getNtlogin() {
		return ntlogin;
	}
	@Override
	public void setNtlogin(String ntlogin) {
		this.ntlogin = ntlogin == null ? "" : ntlogin.trim();
	}
	@Override
	public String getEnvironment() {
		return environment;
	}
	@Override
	public void setEnvironment(String environment) {
		this.environment = environment;
	}
	@Override
	public String getApproverEmail() {
		return approverEmail;
	}
	@Override
	public void setApproverEmail(String approverEmail) {
		this.approverEmail = approverEmail;
	}
	@Override
	public String getRequesterEmail() {
		return requesterEmail;
	}
	@Override
	public void setRequesterEmail(String requesterEmail) {
		this.requesterEmail = requesterEmail;
	}
	@Override
	public Date getStartDate() {
		return startDate;
	}
	@Override
	public void setStartDate(Date startDate) {
		this.startDate = startDate;
	}
	@Override
	public Date getEndDate() {
		return endDate;
	}
	@Override
	public void setEndDate(Date endDate) {
		this.endDate = endDate;
	}
	@Override
	public String getGgroup() {
		return ggroup;
	}
	@Override
	public void setGgroup(String ggroup) {
		this.ggroup = ggroup;
	}
	@Override
	public String getOrganisation() {
		return organisation;
	}
	@Override
	public void setOrganisation(String organisation) {
		this.organisation = organisation;
	}
	@Override
	public String getBusinessJustification() {
		return businessJustification;
	}
	@Override
	public void setBusinessJustification(String businessJustification) {
		this.businessJustification = businessJustification;
	}
	@Override
	public int getAction() {
		return action;
	}
	@Override
	public void setAction(int action) {
		this.action = action;
	}
	
	

	

}



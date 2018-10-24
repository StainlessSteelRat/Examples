

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
@Table(name="m_decomm")
public class DecommItem extends TimestampEntity implements Serializable 	{
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

	public static final String[] STATUS_NAMES = { "Request Created", "Pending Approval", "Approved", "Creating Account", "Account Created", "Terminated", "Deleted", "Rejected", "Error", "Cancelled", "Notified", "Password Reset", "Password Reset", "Updating", "Updated", "Enabled", "Expiring", "Expiry Notification", "Disabling Account" };

	public static final int STATUS_NOTIFIED = 10;
	
	public static final int STATUS_USER_PW_RESET = 11;
	public static final int STATUS_BRM_PW_RESET = 12;
	public static final int STATUS_UPDATING = 13;
	public static final int STATUS_UPDATED = 14;
	public static final int STATUS_ENABLED = 15;
	public static final int STATUS_EXPIRING_USER = 16;
	public static final int STATUS_EXPIRING_REQUESTER = 17;
	public static final int STATUS_DISABLED_ACCOUNT = 18;
	
	public static final String[] ACTION_NAMES = { "Create User", "Update User", "ERROR", "", "Enable Account", "Create Requester", "Expiring", "Account Disabled", "Deleted Group" };
	public static final int ACTION_CREATE = 0;
	public static final int ACTION_UPDATE = 1;
	public static final int ACTION_ERROR = 2;
	public static final int ACTION_ENABLE = 4;
	public static final int ACTION_CREATE_REQUESTER = 5;
	public static final int ACTION_EXPIRY = 6;
	public static final int ACTION_ACCOUNT_DISABLED = 7;
	public static final int ACTION_GROUP_DELETED = 8;
	

	

	
	
	private String attachment;
//	private String sname;
//	private String employeeID;
//	private String exEmail;
//	private String intEmail;
//	private String phone;
	private String ggroup;
	private String ntlogin;
	private String environment;
//	private Date startDate;
	private Date endDate;
	private String organisation;
	
	private String username;
	private String requester;
	private String requesterEmail;
	private String approver;
	private String approverEmail;
	
	private int status;
	private int action;
	private String msg;
	private String businessJustification;
	
	private boolean checkGroup;
	private boolean checkSGDIcons;
	private boolean checkVDTs;
	private boolean checkEUAAs;
	private boolean checkBRMs;
	
	@SuppressWarnings("unchecked")
	public static List<DecommItem> getEntries(final int status) {
		return (List<DecommItem>) CSDAOImpl.getDAO().getHibernateTemplate().execute(new HibernateCallback<Object>() {
            public Object doInHibernate(Session session) throws HibernateException, SQLException {
            	 return session.createQuery("from DecommItem j where j.status = :status order by j.created DESC")
            			 .setParameter("status", status).list();
            }
        });
	}
	
	@SuppressWarnings("unchecked")
	public static List<DecommItem> getEntriesByRequester(final int status) {
		return (List<DecommItem>) CSDAOImpl.getDAO().getHibernateTemplate().execute(new HibernateCallback<Object>() {
            public Object doInHibernate(Session session) throws HibernateException, SQLException {
            	 return session.createQuery("from DecommItem j where j.status = :status order by j.created DESC")
            			 .setParameter("status", status).list();
            }
        });
	}
	
	@SuppressWarnings("unchecked")
	public static List<DecommItem> getEntriesByDate(final Date date) {
		return (List<DecommItem>) CSDAOImpl.getDAO().getHibernateTemplate().execute(new HibernateCallback<Object>() {
            public Object doInHibernate(Session session) throws HibernateException, SQLException {
            	 return session.createQuery("from DecommItem j where j.created > :date order by j.created DESC")
            			 .setParameter("date", date).list();
            }
        });
	}
	
	@SuppressWarnings("unchecked")
	public static List<DecommItem> getEntriesByDateIgnore(final Date date, final String msg) {
		return (List<DecommItem>) CSDAOImpl.getDAO().getHibernateTemplate().execute(new HibernateCallback<Object>() {
            public Object doInHibernate(Session session) throws HibernateException, SQLException {
            	 return session.createQuery("from DecommItem j where j.created > :date and j.msg <> :msg order by j.created DESC")
            			 .setParameter("date", date).setParameter("msg", msg).list();
            }
        });
	}
	
	@SuppressWarnings("unchecked")
	public static List<DecommItem> getEntriesByRequesterAndDate(final String username, final Date date) {
		return (List<DecommItem>) CSDAOImpl.getDAO().getHibernateTemplate().execute(new HibernateCallback<Object>() {
            public Object doInHibernate(Session session) throws HibernateException, SQLException {
            	 return session.createQuery("from DecommItem j where j.requester = :username and j.created > :date order by j.created DESC")
            			 .setParameter("username", username)
            			 .setParameter("date", date)
            			 .list();
            }
        });
	}
	
	@SuppressWarnings("unchecked")
	public static List<DecommItem> getEntriesByStatusAndApprover(final int status, final String approver) {
		return (List<DecommItem>) CSDAOImpl.getDAO().getHibernateTemplate().execute(new HibernateCallback<Object>() {
            public Object doInHibernate(Session session) throws HibernateException, SQLException {
            	 return session.createQuery("from DecommItem j where j.status = :status and j.approver = :approver order by j.created ASC")
            			 .setParameter("status", status)
            			 .setParameter("approver", approver).list();
            }
        });
	}
	
	@SuppressWarnings("unchecked")
	public static List<DecommItem> getEntriesByApproverAndDate(final String approver, final Date date) {
		return (List<DecommItem>) CSDAOImpl.getDAO().getHibernateTemplate().execute(new HibernateCallback<Object>() {
            public Object doInHibernate(Session session) throws HibernateException, SQLException {
            	 return session.createQuery("from UserRequest j where j.approver = :approver and j.created > :date order by j.created ASC")
            			 .setParameter("approver", approver)
            			 .setParameter("date", date)
            			 .list();
            }
        });
	}
	
	public static Long countApproversByDate(Date date) {
		return (Long) CSDAOImpl.getDAO().getHibernateTemplate().execute(new HibernateCallback<Object>() {
            public Object doInHibernate(Session session) throws HibernateException, SQLException {
            	 return session.createQuery("select count(*) from DecommItem j where j.updated > :date and j.action = 0 and j.status = 10 and username like '%_BRM' order by j.created ASC")
            			 .setParameter("date", date)
            			 .uniqueResult();
            }
        });
	}
	
	public static Long countApproversByDateRange(Date from, Date to) {
		return (Long) CSDAOImpl.getDAO().getHibernateTemplate().execute(new HibernateCallback<Object>() {
            public Object doInHibernate(Session session) throws HibernateException, SQLException {
            	 return session.createQuery("select count(*) from DecommItem j where j.updated > :date and j.updated < :date2 and j.action = 0 and j.status = 10 and username like '%_BRM' order by j.created ASC")
            			 .setParameter("date", from)
            			 .setParameter("date2", to)
            			 .uniqueResult();
            }
        });
	}
	
	public static Long countUsersByDate(Date date) {
		return (Long) CSDAOImpl.getDAO().getHibernateTemplate().execute(new HibernateCallback<Object>() {
            public Object doInHibernate(Session session) throws HibernateException, SQLException {
            	 return session.createQuery("select count(*) from DecommItem j where j.updated > :date and j.action = 0 and j.status = 10 and username not like '%_BRM' order by j.created ASC")
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
            	 return session.createQuery("select count(*) from DecommItem j where j.updated > :date and j.action = 5 and j.status = 10 order by j.created ASC")
            			 .setParameter("date", date)
            			 .uniqueResult();
            }
        });
	}
	
	public static Long countRequestersByDateRange(Date from, Date to) {
		return (Long) CSDAOImpl.getDAO().getHibernateTemplate().execute(new HibernateCallback<Object>() {
            public Object doInHibernate(Session session) throws HibernateException, SQLException {
            	 return session.createQuery("select count(*) from DecommItem j where j.updated > :date and j.updated < :date2 and j.action = 5 and j.status = 10 order by j.created ASC")
            			 .setParameter("date", from)
            			 .setParameter("date2", to)
            			 .uniqueResult();
            }
        });
	}
	
	@SuppressWarnings("unchecked")
	public static List<DecommItem> getEntriesByUserDetails(final String group, final String email) {
		return (List<DecommItem>) CSDAOImpl.getDAO().getHibernateTemplate().execute(new HibernateCallback<Object>() {
            public Object doInHibernate(Session session) throws HibernateException, SQLException {
            	 return session.createQuery("from DecommItem j where j.ggroup = :group  and j.exEmail = :exEmail and j.action = 0 and (j.status < 5 or j.status = 10) order by j.created ASC")
            			 .setParameter("group", group)
            			 .setParameter("exEmail", email)
            			 .list();
            }
        });
	}

	
	
	public DecommItem init() {
//		fname = "";
//		sname = "";
//		employeeID = "";
//		exEmail = "";
//		intEmail = "";
//		phone = "";
		ggroup = "";
		ntlogin = "";
		environment = "";
//		startDate = new Date();
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
	
	public DecommItem init(LDAPUser user) {
//		fname = user.getGivenName();
//		sname = user.getSn();
//		employeeID = user.getEmployeeNo();
//		exEmail = user.getEmail();
//		intEmail = user.getIntEmail();
//		phone = user.getPhoneNo();
		ggroup = user.getIsMemberOf();
		ntlogin = "";
		environment = "";
//		startDate = user.getStartDate();
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
		DecommItem other = (DecommItem) obj;
		if (id != other.id) { return false; }
		return true;
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
	
	
	

	public String getMsg() {
		return msg;
	}

	public void setMsg(String msg) {
		this.msg = msg;
	}






	public String getNtlogin() {
		return ntlogin;
	}




	public void setNtlogin(String ntlogin) {
		this.ntlogin = ntlogin == null ? "" : ntlogin.trim();
	}




	public String getEnvironment() {
		return environment;
	}




	public void setEnvironment(String environment) {
		this.environment = environment;
	}




	public String getApproverEmail() {
		return approverEmail;
	}




	public void setApproverEmail(String approverEmail) {
		this.approverEmail = approverEmail;
	}




	public String getRequesterEmail() {
		return requesterEmail;
	}




	public void setRequesterEmail(String requesterEmail) {
		this.requesterEmail = requesterEmail;
	}





	public Date getEndDate() {
		return endDate;
	}




	public void setEndDate(Date endDate) {
		this.endDate = endDate;
	}




	public String getGgroup() {
		return ggroup;
	}




	public void setGgroup(String ggroup) {
		this.ggroup = ggroup;
	}

	public String getOrganisation() {
		return organisation;
	}

	public void setOrganisation(String organisation) {
		this.organisation = organisation;
	}

	public String getBusinessJustification() {
		return businessJustification;
	}

	public void setBusinessJustification(String businessJustification) {
		this.businessJustification = businessJustification;
	}

	public int getAction() {
		return action;
	}

	public void setAction(int action) {
		this.action = action;
	}

	public boolean isCheckGroup() {
		return checkGroup;
	}

	public void setCheckGroup(boolean checkGroup) {
		this.checkGroup = checkGroup;
	}

	public boolean isCheckSGDIcons() {
		return checkSGDIcons;
	}

	public void setCheckSGDIcons(boolean checkSGDIcons) {
		this.checkSGDIcons = checkSGDIcons;
	}

	public boolean isCheckVDTs() {
		return checkVDTs;
	}

	public void setCheckVDTs(boolean checkVDTs) {
		this.checkVDTs = checkVDTs;
	}

	public boolean isCheckEUAAs() {
		return checkEUAAs;
	}

	public void setCheckEUAAs(boolean checkEUAAs) {
		this.checkEUAAs = checkEUAAs;
	}

	public boolean isCheckBRMs() {
		return checkBRMs;
	}

	public void setCheckBRMs(boolean checkBRMs) {
		this.checkBRMs = checkBRMs;
	}

	public String getAttachment() {
		return attachment;
	}

	public void setAttachment(String attachement) {
		this.attachment = attachement;
	}

	
	
	

	

}



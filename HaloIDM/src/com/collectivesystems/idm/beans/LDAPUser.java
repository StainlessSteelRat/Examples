package com.collectivesystems.idm.beans;

import java.util.Date;

import com.ibm.icu.util.Calendar;

public class LDAPUser implements Comparable<LDAPUser> {
	public static final String[] attrIDs = { "uid", "entryDN", "cn", "sn", "givenName", "mail", "uidNumber", "gidNumber", "homeDirectory", "userPassword", "isMemberOf", "postalAddress", "startDate", "endDate", "employeeNumber", "telephoneNumber", "haloStatus", "nsAccountLock", "lastLogin", "lastApprover", "lastNotified", "<matcher-field>" };
	public static final String filter = "(uid=%)";
	private String uid;
	private String entryDN;
	private String cn;
	private String sn;
	private String givenName;
	private String email;
	private String homeDirectory;
	private String userPassword;
	private int uidNumber;
	private int gidNumber;
	private String isMemberOf;
	private String intEmail;
	
	// HaloPerson
	private String phoneNo;	
	private String employeeNo;
	private Date startDate;
	private Date endDate;
	private int status;
	private boolean accountLocked;
	private Date lastLogin;
	private String lastApprover;
	private Date lastNotified;
	
	public final static int STATUS_ENABLED = 0;
	public final static int STATUS_DISABLED = 1;
	public final static String STATUS_NAMES[] = { "", "Disabled" };
	
	private String matcherAttr;
	  
	@Override
	public int compareTo(LDAPUser o) {
		return this.cn.compareTo(o.getCn());
	}
	
	public LDAPUser init() {
		this.uid = "";
		this.cn = "";
		this.entryDN = "";
		this.sn = "";
		this.givenName = "";
		this.email = "";
		this.intEmail = "";
		this.homeDirectory = "";
		this.userPassword = "";
		this.uidNumber = 0;
		this.gidNumber = 0;
		this.isMemberOf = "";
		this.matcherAttr = "";
		this.startDate = new Date();
		Calendar c = Calendar.getInstance();
		c.add(Calendar.DAY_OF_YEAR, 90);
		
		this.endDate = c.getTime();
		
		return this;
	}
	
	@Override
	public String toString() {
		return "LDAPUser [uid=" + uid + ", entryDN=" + entryDN + ", cn=" + cn + ", sn=" + sn + ", givenName=" + givenName + ", email=" + email
				+ ", homeDirectory=" + homeDirectory + ", userPassword=" + userPassword + ", uidNumber=" + uidNumber + ", gidNumber=" + gidNumber
				+ ", isMemberOf=" + isMemberOf + ", intEmail=" + intEmail + ", startDate=" + startDate + ", endDate=" + endDate + ", matcherAttr=" + matcherAttr
				+ "]";
	}
	
	@Override
	public int hashCode() {
		final int prime = 31;
		int result = 1;
		result = prime * result + ((entryDN == null) ? 0 : entryDN.hashCode());
		return result;
	}

	@Override
	public boolean equals(Object obj) {
		if (this == obj) return true;
		if (obj == null) return false;
		if (getClass() != obj.getClass()) return false;
		LDAPUser other = (LDAPUser) obj;
		if (entryDN == null) {
			if (other.entryDN != null) return false;
		} else if (!entryDN.equals(other.entryDN)) return false;
		return true;
	}	
	
	
	public String getFullname() { return givenName + " " + sn; }
	
	public String getUid() {
		return uid;
	}
	public void setUid(String uid) {
		this.uid = uid;
	}
	public String getEntryDN() {
		return entryDN;
	}
	public void setEntryDN(String entryDN) {
		this.entryDN = entryDN;
	}
	public String getCn() {
		return cn;
	}
	public void setCn(String cn) {
		this.cn = cn;
	}
	public String getSn() {
		return sn;
	}
	public void setSn(String sn) {
		this.sn = sn;
	}
	public String getMatcherAttr() {
		return matcherAttr;
	}
	public void setMatcherAttr(String matcherAttr) {
		this.matcherAttr = matcherAttr;
	}
	public String getEmail() {
		return email;
	}
	public void setEmail(String email) {
		this.email = email;
	}
	
	public String getHomeDirectory() {
		return homeDirectory;
	}
	public void setHomeDirectory(String homeDirectory) {
		this.homeDirectory = homeDirectory;
	}
	public int getUidNumber() {
		return uidNumber;
	}
	public void setUidNumber(int uidNumber) {
		this.uidNumber = uidNumber;
	}
	public int getGidNumber() {
		return gidNumber;
	}
	public void setGidNumber(int gidNumber) {
		this.gidNumber = gidNumber;
	}
	
	public String getUserPassword() {
		return userPassword;
	}
	public void setUserPassword(String userPassword) {
		this.userPassword = userPassword;
	}
	public String getIsMemberOf() {
		return isMemberOf;
	}





	public void setIsMemberOf(String isMemberOf) {
		this.isMemberOf = isMemberOf;
	}





	public String getIntEmail() {
		return intEmail;
	}





	public void setIntEmail(String intEmail) {
		this.intEmail = intEmail;
	}





	public Date getStartDate() {
		return startDate;
	}

	public void setStartDate(Date startDate) {
		this.startDate = startDate;
	}
	public Date getEndDate() {
		return endDate;
	}

	public void setEndDate(Date endDate) {
		this.endDate = endDate;
	}

	public String getGivenName() {
		return givenName;
	}

	public void setGivenName(String givenName) {
		this.givenName = givenName;
	}

	

	public String getEmployeeNo() {
		return employeeNo;
	}

	public void setEmployeeNo(String employeeNo) {
		this.employeeNo = employeeNo;
	}

	public String getPhoneNo() {
		return phoneNo;
	}

	public void setPhoneNo(String phoneNo) {
		this.phoneNo = phoneNo;
	}

	

	

	public int getStatus() {
		return status;
	}

	public void setStatus(int status) {
		this.status = status;
	}

	public boolean isAccountLocked() {
		return accountLocked;
	}

	public void setAccountLocked(boolean accountLocked) {
		this.accountLocked = accountLocked;
	}

	public Date getLastLogin() {
		return lastLogin;
	}

	public void setLastLogin(Date lastLogin) {
		this.lastLogin = lastLogin;
	}

	public String getLastApprover() {
		return lastApprover;
	}

	public void setLastApprover(String lastApprover) {
		this.lastApprover = lastApprover;
	}

	public Date getLastNotified() {
		return lastNotified;
	}

	public void setLastNotified(Date lastNotified) {
		this.lastNotified = lastNotified;
	}

	
	

}

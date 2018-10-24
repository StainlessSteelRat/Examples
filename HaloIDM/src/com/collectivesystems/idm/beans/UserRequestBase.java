package com.collectivesystems.idm.beans;

import java.util.Date;

public interface UserRequestBase   {

	public long getId();
	public Date getCreated();
	public Date getUpdated();
	 
	public String getFullname();

	public String getUsername() ;

	public void setUsername(String username) ;

	public String getRequester();

	public void setRequester(String requester);

	public String getApprover() ;

	public void setApprover(String approver);
	public int getStatus();
	public void setStatus(int status);

	public String getMsg();

	public void setMsg(String msg);

	public String getFname();

	public void setFname(String fname) ;
	public String getSname();

	public void setSname(String sname) ;

	public String getEmployeeID();

	public void setEmployeeID(String employeeID) ;

	public String getExEmail();

	public void setExEmail(String exEmail) ;

	public String getIntEmail() ;

	public void setIntEmail(String intEmail) ;

	public String getPhone() ;

	public void setPhone(String phone);

	public String getNtlogin() ;

	public void setNtlogin(String ntlogin);

	public String getEnvironment();

	public void setEnvironment(String environment) ;

	public String getApproverEmail();

	public void setApproverEmail(String approverEmail);
	public String getRequesterEmail() ;

	public void setRequesterEmail(String requesterEmail);

	public Date getStartDate() ;

	public void setStartDate(Date startDate);

	public Date getEndDate() ;

	public void setEndDate(Date endDate) ;

	public String getGgroup() ;

	public void setGgroup(String ggroup);

	public String getOrganisation() ;

	public void setOrganisation(String organisation) ;

	public String getBusinessJustification() ;

	public void setBusinessJustification(String businessJustification);

	public int getAction() ;

	public void setAction(int action);
}

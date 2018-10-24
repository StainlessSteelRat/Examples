package com.collectivesystems.idm.beans.sgd;

import java.net.URLDecoder;
import java.sql.SQLException;
import java.util.Date;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import javax.persistence.Entity;
import javax.persistence.Table;

import org.hibernate.HibernateException;
import org.hibernate.Session;
import org.springframework.orm.hibernate3.HibernateCallback;

import com.collectivesystems.core.beans.dao.TimestampEntity;
import com.collectivesystems.core.dao.CSDAOImpl;
import com.collectivesystems.idm.services.service.Globals;

@Entity
@Table(name="m_sgd")
public class SGDAuditItem extends TimestampEntity {
	
	/*
	 * category=audit/passcache/auditinfo,
	 * event=create,
	 * id=1479705312219,
	 * info=Passcache Create: Person: .../_service/sco/tta/ldapcache/uid%3d3PAKUMARIS6%2cou%3dPeople%2co%3deds%2cdc%3dvodafone%2cdc%3dcom%2c Resource: .../_ens/o%3dappservers/ou%3dSuppliers/ou%3dIBM/ou%3dIBM UK BI AM and AO Production Users/ou%3dVDTs/cn%3dE0335222,date=2016/11/21 06:15:12.219,
	 * tfn-name=.../_ens/o%3dappservers/ou%3dSuppliers/ou%3dIBM/ou%3dIBM UK BI AM and AO Production Users/ou%3dVDTs/cn%3dE0335222,attributes=SchemaBasedAttributes%3d{%0a  scottapasscacheisdomain (INT): "0"%0a  scottapasscacheuid (CES): "mnkkkk"%0a  scottapasscacheresource (CES): ".../_ens/o%3dappservers/ou%3dSuppliers/ou%3dIBM/ou%3dIBM UK BI AM and AO Production Users/ou%3dVDTs/cn%3dE0335222"%0a  scottapasscachetype (INT): "0"%0a  objectclass (CIS): "top"%2c "scottapasscacheentry"%0a  scottapasscachettl (INT): "0"%0a  scottapasscachescope (INT): "0"%0a},
	 * user=.../_service/sco/tta/ldapcache/uid%3d3PAKUMARIS6%2cou%3dPeople%2co%3deds%2cdc%3dvodafone%2cdc%3dcom,pid=10381,
	 * thread=Event Worker Thread 296 (JNDI),
	 * localhost=aukacgjs.dc-dublin.de,
	 * systime=1479705312219

	 */
	
	/*
	 * category=audit/session/auditinfo,
	 * event=sessionEndedDetails,
	 * id=1479900507341,
	 * info=Ended emulator session for user .../_service/sco/tta/ldapcache/uid%3d3PAMISRAA%2cou%3dPeople%2co%3deds%2cdc%3dvodafone%2cdc%3dcom.%0aApplication: .../_ens/o%3dapplications/ou%3dSuppliers/ou%3dWipro/ou%3dWipro VF NewCo AO Production Users/ou%3dVDTs/cn%3dE0335644%0aSecure Global Desktop server: aukacgis.dc-dublin.de%0aApplication server: .../_ens/o%3dappservers/ou%3dSuppliers/ou%3dWipro/ou%3dWipro VF NewCo AO Production Users/ou%3dVDTs/cn%3dE0335644%0aClient: 195.233.130.20%0aSecurity method: ssl%0aTime period: 114951%0aPE location: aukacgis.dc-dublin.de,date=2016/11/23 12:28:27.341,
	 * tfn-name=.../_service/sco/tta/ldapcache/uid%3d3PAMISRAA%2cou%3dPeople%2co%3deds%2cdc%3dvodafone%2cdc%3dcom&.../_ens/o%3dapplications/ou%3dSuppliers/ou%3dWipro/ou%3dWipro VF NewCo AO Production Users/ou%3dVDTs/cn%3dE0335644&.../_ens/o%3dappservers/ou%3dSuppliers/ou%3dWipro/ou%3dWipro VF NewCo AO Production Users/ou%3dVDTs/cn%3dE0335644,
	 * ip-address=195.233.130.20,
	 * pid=6493,
	 * keyword=sessionEnded,
	 * thread=Event Worker Thread 598051 (JNDI),
	 * security-type=ssl,
	 * localhost=aukacgis.dc-dublin.de,
	 * time-period=114951,
	 * systime=1479900507341,
	 * host=aukacgis.dc-dublin.de

	 */
	
	private String logCategory;
	private String logEvent;
	private String logInfo;
	private String logTFNName;
	private String logUser;
	private String logThread;
	private String logLocalhhost;
	private String logHost;
	private String logPID;
	private String logKeyword;
	private String logIPaddress;
	private String logItemID;
	private String logEnvironment;
	
	// private Date systime;  //use 'created'
	
	/*
	 *  - Error processing entry: 
	 *  category=server/login/auditinfo,
	 *  event=loginResultFailed,
	 *  id=1477229471976,
	 *  info=Login attempt for .../_service/sco/tta/ldapcache/uid%3d3PAREDDYV%2cou%3dPeople%2co%3deds%2cdc%3dvodafone%2cdc%3dcom.%0aLogin failed: none of the enabled login authorities authenticated the user.,
	 *  date=2016/10/23 15:31:11.976,
	 *  user=.../_service/sco/tta/ldapcache/uid%3d3PAREDDYV%2cou%3dPeople%2co%3deds%2cdc%3dvodafone%2cdc%3dcom,
	 *  pid=4910,
	 *  keyword=loginFailure,
	 *  thread=calc-peer Worker#3:aukacgfs.dc-dublin.de%2cmsgid:39110,
	 *  localhost=aukacgis.dc-dublin.de,
	 *  systime=1477229471976

	 */
	
	public static void clearAll() {
		 CSDAOImpl.getDAO().getHibernateTemplate().execute(new HibernateCallback<Object>() {
            public Object doInHibernate(Session session) throws HibernateException, SQLException {
            	 return session.createQuery("delete from SGDAuditItem").executeUpdate();
            }
        });
	}
	
	@SuppressWarnings("unchecked")
	public static List<SGDAuditItem> findLastLogins(int i) {
		 return (List<SGDAuditItem>) CSDAOImpl.getDAO().getHibernateTemplate().execute(new HibernateCallback<Object>() {
	            public Object doInHibernate(Session session) throws HibernateException, SQLException {
	            	 return session.createQuery("from SGDAuditItem a where a.logKeyword = 'loginSuccess' order by a.created DESC").setMaxResults(i).list();
	            }
	        });
	}
	
	public static SGDAuditItem parse(String line) {
		SGDAuditItem audit_item = new SGDAuditItem();
		
		Map<String, String> map = new HashMap<>();
		
		String[] items = line.split(",");
		for (String item : items) {
			String kvp[] = item.split("=");
			String key = kvp[0];
			String value = item.replace(key + "=", "");
			map.put(key, value);
		}
		audit_item.setLogCategory(map.get("category"));
		audit_item.setLogEvent(map.get("event"));
		audit_item.setLogItemID(map.get("id"));
		//audit_item.setLogInfo(org.apache.commons.lang3.StringEscapeUtils.unescapeJava(map.get("info")));
		try {
			audit_item.setLogInfo(URLDecoder.decode(map.get("info"), "UTF-8" ));
		} catch (Exception e1) {
			audit_item.setLogInfo("[Decode Error] " + map.get("info"));
		}
		try {
			audit_item.setLogTFNName(URLDecoder.decode(map.get("tfn-name"), "UTF-8" ));
		} catch (Exception e1) {
			audit_item.setLogTFNName("[Decode Error] " + map.get("tfn-name"));
		}

		audit_item.setLogUser(map.get("user"));
		audit_item.setLogThread(map.get("thread"));
		audit_item.setLogHost(map.get("host"));
		audit_item.setLogPID(map.get("pid"));
		audit_item.setLogKeyword(map.get("keyword"));
		audit_item.setLogLocalhhost(map.get("localhost"));
		audit_item.setLogIPaddress(map.get("ip-address"));
		try {
			audit_item.setCreated(new Date(Long.parseLong(map.get("systime"))));
		} catch (Exception e) {
			
		}
		try {
			audit_item.setUpdated(Globals.df.parse(map.get("date")));
		} catch (Exception e) {
			
		}

		return audit_item;
	}
	

	
	

	public String getLogCategory() {
		return logCategory;
	}

	public void setLogCategory(String logCategory) {
		this.logCategory = logCategory;
	}

	public String getLogEvent() {
		return logEvent;
	}

	public String getLogInfo() {
		return logInfo;
	}
	
	public void setLogEvent(String logEvent) {
		this.logEvent = logEvent;
	}


	public void setLogInfo(String logInfo) {
		this.logInfo = logInfo;
	}

	public String getLogTFNName() {
		return logTFNName;
	}

	public void setLogTFNName(String logTFNName) {
		this.logTFNName = logTFNName;
	}

	public String getLogUser() {
		return logUser;
	}

	public void setLogUser(String logUser) {
		this.logUser = logUser;
	}

	public String getLogThread() {
		return logThread;
	}

	public void setLogThread(String logThread) {
		this.logThread = logThread;
	}

	public String getLogLocalhhost() {
		return logLocalhhost;
	}

	public void setLogLocalhhost(String logLocalhhost) {
		this.logLocalhhost = logLocalhhost;
	}

	public String getLogHost() {
		return logHost;
	}

	public void setLogHost(String logHost) {
		this.logHost = logHost;
	}

	public String getLogPID() {
		return logPID;
	}

	public void setLogPID(String logPID) {
		this.logPID = logPID;
	}

	public String getLogKeyword() {
		return logKeyword;
	}

	public void setLogKeyword(String logKeyword) {
		this.logKeyword = logKeyword;
	}

	public String getLogIPaddress() {
		return logIPaddress;
	}

	public void setLogIPaddress(String logIPaddress) {
		this.logIPaddress = logIPaddress;
	}

	public String getLogItemID() {
		return logItemID;
	}

	public void setLogItemID(String logItemID) {
		this.logItemID = logItemID;
	}

	public String getLogEnvironment() {
		return logEnvironment;
	}

	public void setLogEnvironment(String logEnvironment) {
		this.logEnvironment = logEnvironment;
	}

	@Override
	public String toString() {
		return "SGDAuditItem [logCategory=" + logCategory + ", logEvent=" + logEvent + ", logInfo=" + logInfo
				+ ", logTFNName=" + logTFNName + ", logUser=" + logUser + ", logThread=" + logThread
				+ ", logLocalhhost=" + logLocalhhost + ", logHost=" + logHost + ", logPID=" + logPID + ", logKeyword="
				+ logKeyword + ", logIPaddress=" + logIPaddress + ", logItemID=" + logItemID + ", logEnvironment="
				+ logEnvironment + "]";
	}

	

	

	
	
}
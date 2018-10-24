package com.collectivesystems.idm.services.service;

import java.net.InetAddress;
import java.net.UnknownHostException;
import java.text.SimpleDateFormat;
import java.util.Collection;
import java.util.Date;
import java.util.HashMap;
import java.util.Hashtable;
import java.util.LinkedList;
import java.util.List;
import java.util.Map;
import java.util.Random;
import java.util.Set;
import java.util.TreeSet;

import javax.annotation.PostConstruct;
import javax.naming.CommunicationException;
import javax.naming.NamingEnumeration;
import javax.naming.directory.Attribute;
import javax.naming.directory.Attributes;
import javax.naming.directory.BasicAttribute;
import javax.naming.directory.BasicAttributes;
import javax.naming.directory.DirContext;
import javax.naming.directory.InitialDirContext;
import javax.naming.directory.ModificationItem;
import javax.naming.directory.SearchControls;
import javax.naming.directory.SearchResult;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;

import com.collectivesystems.core.services.service.PropertiesService;
import com.collectivesystems.idm.beans.Group;
import com.collectivesystems.idm.beans.LDAPUser;

@Service
public class LDAPService {
	final Logger log = LoggerFactory.getLogger(LDAPService.class);
	final SimpleDateFormat df = new SimpleDateFormat("dd-MM-yyyy");
	final SimpleDateFormat df_withTime = new SimpleDateFormat("HH:mm:ss dd-MM-yyyy");
	final SimpleDateFormat df_ldap = new SimpleDateFormat("yyyyMMddHHmmss'Z'");
	//													   2017 02 08 09 53 26Z
	
	public static LDAPService me = null;
	
	@Autowired
	protected PropertiesService properties;
	
	protected Hashtable<String, String> _env = null;
	protected String _hostname = null;
	protected String _password = "c4ng3t1n";

	protected boolean connection_successfull = true;
	private String message = null;
	protected boolean connected = false;
	//protected String _sub_context = null;

	Map<String, LDAPUser> user_cache = new HashMap<String, LDAPUser>();
	Map<String, Group> group_cache = new HashMap<String, Group>();
	protected HashMap<String, Integer> connections;

	final static String SALTCHARS_LOWER = "abcdefghijklmnopqrstuvwxyz";
    final static String SALTCHARS_UPPER = "ABCDEFGHIJKLMNOPQRSTUVWXYZ";
    final static String SALTCHARS_NUMERIC = "1234567890";
    final static String SALTCHARS_SPECIAL = "[]{}/@/&"; 
	
	DirContext _ctx;

	@PostConstruct
	public void init() {
		LDAPService.me = this;
		log.error(generatePassword());
		
		try {
			InetAddress addr = InetAddress.getLocalHost();
			@SuppressWarnings("unused")
			byte[] ipAddr = addr.getAddress();
			if (this._hostname == null) this._hostname = addr.getHostName();
		} catch (UnknownHostException localUnknownHostException) {
			log.error("An error occured while connecting to the LDAP connection: " + localUnknownHostException.toString());
		}
		try {
			this._env = new Hashtable<String, String>();
			this._env.put("java.naming.factory.initial", "com.sun.jndi.ldap.LdapCtxFactory");
			this._env.put("java.naming.provider.url", properties.getProperty("ldap.provider", "")); // "ldap://aukacgbs-z2.dc-dublin.de:389/");
			this._env.put("java.naming.security.authentication", "simple");
			this._env.put("java.naming.security.principal",  properties.getProperty("ldap.provider.username", ""));
			this._env.put("java.naming.security.credentials",  properties.getProperty("ldap.provider.password", ""));
			this._env.put("java.naming.referral", "follow");

			this._env.put("com.sun.jndi.ldap.connect.pool", "false");
			this._env.put("com.sun.jndi.ldap.connect.timeout", "5000");
			log.info("Connecting...");
			_ctx = new InitialDirContext(this._env);

			Attributes attrs = _ctx.getAttributes(properties.getProperty("ldap.provider.basedn", ""));
			if (attrs != null) {
				log.trace("LDAP Module Ready");
			} else {
				log.trace("LDAP Module Failed!");
				this.connection_successfull = false;
				_ctx.close();
				
			}
			log.info("Connected");
			
		} catch (Exception e) {
			log.error("An error occured while testing the LDAP connection: [" + properties.getProperty("ldap.provider.basedn", "") + "] " + e.toString());
			this.connection_successfull = false;
			setMessage(e.toString());

			setConnected(this.connection_successfull);
		}

		setConnected(this.connection_successfull);
	}
	
	public String getHostName() {
		return this._hostname;
	}


	public boolean isConnected() {
		return this.connected;
	}

	public void setConnected(boolean status) {
		this.connected = status;
	}
	
	public String getMessage() {
		return this.message;
	}

	public void setMessage(String message) {
		this.message = message;
	}
	
	protected DirContext getContext() throws Exception {
		if (_ctx != null && isConnected()) { return _ctx; };
		_ctx = new InitialDirContext(this._env);
		return _ctx;
	}
	
	protected DirContext resetContext() throws Exception {
		if (_ctx != null && isConnected()) { try { _ctx.close(); } catch (Exception e) {}  _ctx = null; }
		_ctx = new InitialDirContext(this._env);
		return _ctx;
	}

	public List<?> report(String query) {
		return prepareUserSearch(query);
	}


	protected NamingEnumeration<SearchResult> search(String[] attrIDs, String filter, String basedn) {
		NamingEnumeration<SearchResult> answer = null;
		SearchControls ctls = new SearchControls();
		try {
			ctls.setReturningAttributes(attrIDs);
			ctls.setSearchScope(2);
			DirContext ctx = getContext(); //new InitialDirContext(this._env);
			try {
				answer = ctx.search(basedn, filter, ctls);
			} catch (CommunicationException ce) {
				ctx = resetContext();
				answer = ctx.search(basedn, filter, ctls);
			}
			//ctx.close();
		} catch (Exception e) {
			log.error("An error occured during search - " + filter + " " + e.toString());
		}
		return answer;
	}
	
	protected Attributes get(String[] attrIDs, String dn) {
		Attributes answer = null;
		SearchControls ctls = new SearchControls();
		try {
			ctls.setReturningAttributes(attrIDs);
			ctls.setSearchScope(2);
			DirContext ctx = getContext(); //new InitialDirContext(this._env);
			try {
				answer = ctx.getAttributes(dn, attrIDs);
			} catch (CommunicationException ce) {
				ctx = resetContext();
				answer = ctx.getAttributes(dn, attrIDs);
			}
			
			//ctx.close();
		} catch (Exception e) {
			log.error("An error occured during retreval  - " + dn + " " + e.toString());
		}
		return answer;
	}
	
	public void move(String from_dn, String to_dn) {
		try {
			DirContext ctx = new InitialDirContext(this._env);

			ctx.rename(from_dn, to_dn);
			ctx.close();
		} catch (Exception e) {
			log.error("An error occured during move  - " + from_dn + " -> " + to_dn + " " + e.toString());
		}
		
	}
	
    public Group getGroupByDn(String group_dn) {
    	Group group = null;
    	try {
			String[] attrIDs = Group.attrIDs; 
			attrIDs[attrIDs.length-1] = properties.getProperty("idm.ldap.group.attr", "description");
			
			Attributes answer = get(attrIDs, group_dn);
			if (answer != null) {
				group = new Group();
				LinkedList<String> list = new LinkedList<String>();
				group.setUniqueMember(list);
				

				group.setCn((String) answer.get(attrIDs[0]).get());
				group.setDn((String) answer.get(attrIDs[1]).get());		
				Attribute attr = answer.get(attrIDs[2]);
				if (attr != null) {
					NamingEnumeration<?> nem = attr.getAll();
					while (nem.hasMore()) {
						String member = (String) nem.next();
						list.add(member);
					}
				}
				try { // again, should we check the object classes to see if this is needed??
					group.setGidNumber(Integer.parseInt((String) answer.get(attrIDs[3]).get()));
				} catch (Exception e) { group.setGidNumber(0); log.debug("gidNumber missing from" + group.getDn()); }
				
				try {
					group.setMatcherAttr((String) answer.get(attrIDs[attrIDs.length-1]).get());
				} catch (Exception e) {
					log.warn("Description Not Set for " + group_dn);
					group.setMatcherAttr("");
				}
				//log.info(group.getCn());
			}
			
		} catch (Exception e) {
			log.error("An error occured processing results from get LDAP Group retrieval.", e);
		}
    	return group;
    }
	
	public Group getGroup(String param) {
		long start = System.currentTimeMillis();
		Group group = null;
		if (PropertiesService.getPropertyStatic("ldap.cache.enabled", "no").equalsIgnoreCase("yes")) {
			group = (Group) this.group_cache.get(param);
		}
		
		if (group == null) {
			
			try {
				String[] attrIDs = Group.attrIDs; 
				attrIDs[attrIDs.length-1] = properties.getProperty("idm.ldap.group.attr", "description");
				String filter = properties.getProperty("idm.ldap.group.filter", Group.groupfilter).replace("%", param);

				NamingEnumeration<?> answer = search(attrIDs, filter, properties.getProperty("ldap.provider.basedn", ""));
				while (answer.hasMore()) {
					group = new Group();
					LinkedList<String> list = new LinkedList<String>();
					group.setUniqueMember(list);
					SearchResult sr = (SearchResult) answer.next();

					group.setCn((String) sr.getAttributes().get(attrIDs[0]).get());
					group.setDn((String) sr.getAttributes().get(attrIDs[1]).get());		
					Attribute attr = sr.getAttributes().get(attrIDs[2]);
					if (attr != null) {
						NamingEnumeration<?> nem = attr.getAll();
						while (nem.hasMore()) {
							String member = (String) nem.next();
							list.add(member);
						}
					}
					try { // again, should we check the object classes to see if this is needed??
						group.setGidNumber(Integer.parseInt((String) sr.getAttributes().get(attrIDs[3]).get()));
					} catch (Exception e) { group.setGidNumber(0); log.debug("gidNumber missing from" + group.getDn()); }
					
					try {
						group.setMatcherAttr((String) sr.getAttributes().get(attrIDs[attrIDs.length-1]).get());
					} catch (Exception e) {
						log.warn("Description Not Set for " + param);
						group.setMatcherAttr("");
					}
				//	log.info(group.getCn());

					
					if (PropertiesService.getPropertyStatic("ldap_cache_enabled", "yes").equalsIgnoreCase("yes")) {
						this.group_cache.put(param, group);
					}
				}
				answer.close();
			} catch (Exception e) {
				log.error("An error occured processing results from get LDAP Group retrieval.", e);
			}
		}
		if (log.isTraceEnabled()) {
			long end = System.currentTimeMillis();
			if (end - start > 1000L) {
				log.warn("getGroup Time taken: " + (end - start) / 1000L + "s");
			}
		}
		return group;
	}

	
	public List<Group> getGroups() {
		return getGroups(properties.getProperty("ldap.provider.basedn", ""));
	}

	public List<Group> getGroups(String basedn) {
		long start = System.currentTimeMillis();
		List<Group> groups = new LinkedList<Group>();
		try {
			String[] attrIDs = Group.attrIDs;
			attrIDs[attrIDs.length-1] = properties.getProperty("idm.ldap.group.attr", "description");
			String filter = properties.getProperty("idm.ldap.groups.filter", Group.groupsfilter);
			NamingEnumeration<SearchResult> answer = search(attrIDs, filter, basedn);
			while (answer.hasMore()) {
				SearchResult sr = (SearchResult) answer.next();
				Group group = new Group();
				LinkedList<String> list = new LinkedList<String>();
				group.setUniqueMember(list);
				group.setCn((String) sr.getAttributes().get(attrIDs[0]).get());
				group.setDn((String) sr.getAttributes().get(attrIDs[1]).get());
				Attribute attr = sr.getAttributes().get(attrIDs[2]);
				if (attr != null) {
					NamingEnumeration<?> nem = attr.getAll();
					while (nem.hasMore()) {	list.add((String)nem.next()); }
				}
				try { // again, should we check the object classes to see if this is needed??
					group.setGidNumber(Integer.parseInt((String) sr.getAttributes().get(attrIDs[3]).get()));
				} catch (Exception e) { group.setGidNumber(0); log.debug("gidNumber missing from" + group.getDn()); }
				try {
					group.setMatcherAttr((String) sr.getAttributes().get(attrIDs[attrIDs.length-1]).get());
				} catch (Exception e) {
					group.setMatcherAttr("");
				}
				// log.info(group.getCn());

				
				groups.add(group);
			}
			answer.close();
		} catch (Exception e) {
			log.error("An error occured processing results from get Ldap Group retrieval.", e);
		}

		long end = System.currentTimeMillis();
		if (end - start > 1000L) {
			log.warn("getGroup Time taken: " + (end - start) / 1000L + "s");
		}
		return groups;
	}
	
	public boolean createGroup(Group group, String environment) {
		 try {
			String[] attrIDs = Group.attrIDs;
			attrIDs[attrIDs.length-1] = properties.getProperty("idm.ldap.group.attr", "description");
				
           Attributes myAttrs = new BasicAttributes();  // Case ignore
           Attribute oc = new BasicAttribute("objectClass");
           oc.add("groupOfUniqueNames");
           oc.add("top");
           for (String objectClass : properties.getProperty("idm.ldap.group.objectclasses", "posixGroup").split(",")) {
            oc.add(objectClass);
           }
         
           myAttrs.put(oc);
          
           //  { "cn", "entrydn", "uniqueMember", "gidNumber", "<matcher-field>" };
           myAttrs.put(new BasicAttribute(attrIDs[0], group.getCn()));
           myAttrs.put(new BasicAttribute(attrIDs[3], "" + group.getGidNumber()));
           myAttrs.put(new BasicAttribute(attrIDs[attrIDs.length-1], group.getMatcherAttr()));
           
          // myAttrs.put(new BasicAttribute("pwdPolicySubEntry", properties.getProperty("idm.environments." + environment.toLowerCase() +  ".pwdpolicy.entry", "")));
           
           String groupdn = "cn=" + group.getCn() + "," + properties.getProperty("idm.environments." + environment.toLowerCase() + ".groups", "cn=groups") ;
           DirContext ctx = new InitialDirContext(_env);
           ctx.createSubcontext(groupdn,  myAttrs);
           group.setDn(groupdn);
           try { ctx.close(); } catch (Exception ex) { }
       } catch (Exception e)
       {
           log.error("An error occured during creation of a user ",e);
          
           this.setMessage(e.toString());
           return false;
       }
       return true;
		
	}
	
	public boolean updateGroup(Group g) {
		try {
			 String[] attrIDs = Group.attrIDs; // { "cn", "entrydn", "uniqueMember", "gidNumber", "<matcher-field>" };
			 attrIDs[attrIDs.length-1] = properties.getProperty("idm.ldap.user.attr", "description");
			 
			 ModificationItem[] mods = new ModificationItem[1];      	        
	         mods[0] = new ModificationItem(DirContext.REPLACE_ATTRIBUTE, new BasicAttribute(attrIDs[attrIDs.length-1], g.getMatcherAttr()));	         
	         
	         DirContext  ctx = getContext(); //new InitialDirContext(_env);
	         ctx.modifyAttributes(g.getDn(), mods);
	         //ctx.close();
	         return true;
			} catch (Exception e) {
				log.error("An error updating group details for {} [{}]", g.getCn(), g.getDn(), e);
				log.debug("", e); 
			}
		return false;
		
	}
	
	public int getGidNumber(String group) {

		Group ldap_group = getGroup(group);
		if (ldap_group != null && ldap_group.getGidNumber() != 0) { return ldap_group.getGidNumber(); }
		return Integer.parseInt(properties.getProperty("idm.ldap.default.groupid", "100"));
	}
	
	
	public LDAPUser getUser(String param) {
		long start = System.currentTimeMillis();
		LDAPUser user = null;
		if (PropertiesService.getPropertyStatic("ldap.cache.enabled", "no").equalsIgnoreCase("yes")) {
			user = this.user_cache.get(param);
		}
		
		if (user == null) {
		//	user = new LDAPUser();
			
			try {
				String[] attrIDs = LDAPUser.attrIDs;  // { "uid", "entryDN", "cn", "sn", "mail", "uidNumber", "gidNumber", "homeDirectory", "<matcher-field>" };
				attrIDs[attrIDs.length-1] = properties.getProperty("idm.ldap.user.attr", "description");
				String filter = LDAPUser.filter.replace("%", param);

				NamingEnumeration<?> answer = search(attrIDs, filter, properties.getProperty("ldap.provider.basedn", ""));
				while (answer.hasMore()) {
					SearchResult sr = (SearchResult) answer.next();
					user = buildUser(sr.getAttributes(), attrIDs);
					if (PropertiesService.getPropertyStatic("ldap_cache_enabled", "yes").equalsIgnoreCase("yes")) {
						this.user_cache.put(param, user);
					}
				}
				answer.close();
			} catch (Exception e) {
				log.error("An error occured processing results from get LDAP User retrieval.", e);
			}
		}
		if (log.isTraceEnabled()) {
			long end = System.currentTimeMillis();
			if (end - start > 1000L) {
				log.warn("getGroup Time taken: " + (end - start) / 1000L + "s");
			}
		}
		return user;
	}
	
	public boolean addUserToGroup(String group, String userdn) {
		try {
			Group ldap_group = getGroup(group);
			if (ldap_group == null) {
				log.error("Cannot add user to group, " + group + " not found");
				return false;
			}
			 // Specify the changes to make
            ModificationItem[] mods = new ModificationItem[1];
            //mods[0] = new ModificationItem(DirContext.REPLACE_ATTRIBUTE, new BasicAttribute("serverName", server.getServerName()));
            mods[0] = new ModificationItem(DirContext.ADD_ATTRIBUTE, new BasicAttribute("uniqueMember", userdn));
         //   log.error(ldap_group.getDn());
            DirContext  ctx = new InitialDirContext(_env);            
            ctx.modifyAttributes(ldap_group.getDn(), mods);
			
			try {
				ctx.close();
			} catch (Exception ex) {}
		} catch (Exception e) {
			log.error("An error occured during addition of user to group " + group, e);

			this.setMessage(e.toString());
			return false;
		}

		return true;
	}
	
	public boolean removeUserFromGroup(String group, String userdn) {
		try {
			Group ldap_group = getGroup(group);
			if (ldap_group == null) {
				log.warn("Remove user from group, " + group + " not found");
				return true;
			}
			 // Specify the changes to make
            ModificationItem[] mods = new ModificationItem[1];
            //mods[0] = new ModificationItem(DirContext.REPLACE_ATTRIBUTE, new BasicAttribute("serverName", server.getServerName()));
            mods[0] = new ModificationItem(DirContext.REMOVE_ATTRIBUTE, new BasicAttribute("uniqueMember", userdn));
         //   log.error(ldap_group.getDn());
            DirContext  ctx = new InitialDirContext(_env);            
            ctx.modifyAttributes(ldap_group.getDn(), mods);
			
			try {
				ctx.close();
			} catch (Exception ex) {}
		} catch (Exception e) {
			log.error("An error occured during removal of user from group " + group, e);

			this.setMessage(e.toString());
			return false;
		}

		return true;
		
	}
	
	public LDAPUser getUserByDn(String dn) {
		LDAPUser user = null;
    	try {
			String[] attrIDs = LDAPUser.attrIDs; 
			attrIDs[attrIDs.length-1] = properties.getProperty("idm.ldap.user.attr", "description");
			
			Attributes answer = get(attrIDs, dn);
			if (answer != null) {
				user = buildUser(answer, attrIDs);
			}
    	} catch (Exception e) {
			log.error("An error occured processing results from get LDAP getUsersByDn retrieval [" + dn + "]", e);
		}
		return user;
	}
	
	public List<LDAPUser> findUserByName(String givenName, String sn, String group) {
		return prepareUserSearch("(&(givenName={1})(sn={2})(isMemberOf={3}))".replace("{1}", givenName).replace("{2}", sn).replace("{3}", group));
		
	}
	
	public List<LDAPUser> findUserByEmailAndGroup(String email, String group_dn) {
		return prepareUserSearch("(&(mail={1})(isMemberOf={2}))".replace("{1}", email).replace("{2}", group_dn));
		
	}
	
	public List<LDAPUser> getUserByGroup(String param) {
		return prepareUserSearch("(isMemberOf=%)".replace("%", param));
	}
	
	public List<LDAPUser> getUsers() {
		return prepareUserSearch("(objectClass=person)");
	}
	
	public List<LDAPUser> getEndUsers() {
		return prepareUserSearch("(&(objectClass=haloPerson)(!(nsAccountLock=true))(!(isMemberOf=cn=BRM,ou=groups,dc=vodafone,dc=com))(!(isMemberOf=cn=EUAA,ou=groups,o=eds,dc=vodafone,dc=com)))");
	}
	
	public List<LDAPUser> getDisabledEndUsers() {
		return prepareUserSearch("(&(objectClass=haloPerson)(nsAccountLock=true)(!(isMemberOf=cn=BRM,ou=groups,dc=vodafone,dc=com))(!(isMemberOf=cn=EUAA,ou=groups,o=eds,dc=vodafone,dc=com)))");
	}
	
	public List<LDAPUser> getUsersByUid(String filter) {
		return prepareUserSearch("(uid=*{1}*)".replace("{1}", filter));
	}
	
	public List<LDAPUser> getUsersByUidAndMatcher(String filter, String matcherAttr) {
		return prepareUserSearch("(&(uid=*{1}*)(description={2}))".replace("{1}", filter).replace("{2}", matcherAttr));
	}
	
	public List<LDAPUser> getUsersByMatcher(String matcherAttr) {
		return prepareUserSearch("(&(objectClass=person)(description=%))".replace("%", matcherAttr));
	}
	
	public List<LDAPUser> getUsersByMatcherAndGroup(String matcherAttr, String group) {
		return prepareUserSearch("(&(description=*%1*)(isMemberOf=%2))".replace("%1", matcherAttr).replace("%2", group));
	}
	
	public List<LDAPUser> getUsersByMatcherAndGroupAndNotLocked(String matcherAttr, String group) {
		return prepareUserSearch("(&(description=*%1*)(isMemberOf=%2)(!(nsAccountLock=true)))".replace("%1", matcherAttr).replace("%2", group));
	}
	
	public Collection<? extends LDAPUser> getUsersByStatus(boolean b) {
		return prepareUserSearch("(nsAccountLock={1})".replace("{1}", Boolean.toString(b)));
	}

	public Collection<? extends LDAPUser> getUsersByStatusAndMatcher(boolean b, String matcherAttr) {
		return prepareUserSearch("(&(description=*%1*)(nsAccountLock=%2))".replace("%1", matcherAttr).replace("%2", Boolean.toString(b)));
	}
	
	public List<LDAPUser> getUsersByEnddate(Date start, Date end) {
		return prepareUserSearch("(&(enddate<=%1)(enddate>=%2)(!(nsAccountLock=true))(!(isMemberOf=cn=BRM,ou=groups,dc=vodafone,dc=com)))".replace("%1", df_ldap.format(start)).replace("%2", df_ldap.format(end)));
	}
	
	public List<LDAPUser> getUsersByEnddate(Date start, Date end, Date lastnotified) {
		return prepareUserSearch("(&(enddate<=%1)(enddate>=%2)(notifiedDate<=%3)(!(nsAccountLock=true))(!(isMemberOf=cn=BRM,ou=groups,dc=vodafone,dc=com)))".replace("%1", df_ldap.format(start)).replace("%2", df_ldap.format(end)).replace("%3", df_ldap.format(lastnotified)));
	}
	
	public List<LDAPUser> getUsersByEnddateAndMatcher(Date res, String matcherAttr) {
		return prepareUserSearch("(&(enddate<=%1)(description=%2))".replace("%1", df_ldap.format(res)).replace("%2", matcherAttr));
	}
	
	public List<LDAPUser> getUsersByEnddateAndMatcherAndStatus(Date res, String matcherAttr, boolean active) {
		return prepareUserSearch("(&(enddate<=%1)(description=%2)(!(nsAccountLock=true)))".replace("%1", df_ldap.format(res)).replace("%2", matcherAttr));
	}
	
	public List<LDAPUser> getUsersByEnddateAndStatus(Date end) {
		return prepareUserSearch("(&(enddate<=%1)(!(nsAccountLock=true))(!(isMemberOf=cn=BRM,ou=groups,dc=vodafone,dc=com)))".replace("%1", df_ldap.format(end)));
	}

	
	private List<LDAPUser> prepareUserSearch(String filter) {
		log.trace(filter);
		List<LDAPUser> list = new LinkedList<>();
		try {
			String[] attrIDs = LDAPUser.attrIDs;  //{ "uid", "entryDN", "cn", "sn", "mail", "uidNumber", "gidNumber", "homeDirectory", "userPassword", "<matcher-field>" };
			attrIDs[attrIDs.length-1] = properties.getProperty("idm.ldap.user.attr", "description");
			NamingEnumeration<?> answer = search(attrIDs, filter, properties.getProperty("ldap.provider.basedn", ""));
			while (answer.hasMore()) {
				SearchResult sr = (SearchResult) answer.next();
				Attributes attrs = sr.getAttributes();
				try {
					list.add(buildUser(attrs, attrIDs));
				} catch (Exception e) { log.error("Unable to build user {}", (String) attrs.get("entryDn").get(), e); }
			
			}
			answer.close();
		} catch (Exception e) {
			log.error("An error occured processing results from get LDAP getUsers retrieval [" + filter + "]", e);
		}
		return list;
	}
	
	private LDAPUser buildUser(Attributes sr, String attrIDs[]) throws Exception {
		//{ "uid", "entryDN", "cn", "sn", "givenName", "mail", "uidNumber", "gidNumber", "homeDirectory", "userPassword", "isMemberOf", "postalAddress", "startDate", "endDate", "employeeNumber", "telephoneNumber", "<matcher-field>" };
		LDAPUser user = new LDAPUser();
	
		
		user.setUid((String) sr.get(attrIDs[0]).get());
		user.setEntryDN((String) sr.get(attrIDs[1]).get());
		user.setCn((String) sr.get(attrIDs[2]).get());
		user.setSn((String) sr.get(attrIDs[3]).get());
		try {
			user.setGivenName((String) sr.get(attrIDs[4]).get());
		} catch (Exception e) { log.warn("No givenName set for account " + user.getEntryDN()); }
		try {
			user.setEmail((String) sr.get(attrIDs[5]).get());
		} catch (Exception e) { log.warn("No email set for account " + user.getEntryDN()); }
		
		try { // PosixAccount attributes.. should we check objectClasses to see if we should populate these attributes?
			user.setUidNumber(Integer.parseInt((String)sr.get(attrIDs[6]).get()));
			user.setGidNumber((Integer.parseInt((String)sr.get(attrIDs[7]).get())));
			user.setHomeDirectory((String) sr.get(attrIDs[8]).get());
			user.setUserPassword((String) sr.get(attrIDs[9]).get());
			
		} catch (Exception e) { log.debug(user.getUid(), e); }
		try {
			Attribute attr = sr.get(attrIDs[10]);
			if (attr != null) {
				StringBuilder groups = new StringBuilder();
				NamingEnumeration<?> nem = attr.getAll();
				//while (nem.hasMore()) {	groups.append(((String)nem.next())); groups.append("|"); }
				while (nem.hasMore()) {	
					String groupname = ((String)nem.next());
					groups.append(groupname.substring(3, groupname.indexOf(','))); 
					groups.append(", "); 
				}
				user.setIsMemberOf(groups.toString().substring(0, groups.length()-2));
			}
		} catch (Exception e) {
			log.warn("isMemberOf [" + attrIDs[10] + "] not found for " + user.getUid());
		}
		
		try { 
			user.setIntEmail((String)sr.get(attrIDs[11]).get());  //Postal Address
		} catch (Exception e) { log.debug(user.getUid(), e); }
		
		try {
			// haloPerson Attributes
			if (((String)sr.get(attrIDs[12]).get()).contains("-")) {
				user.setStartDate(df.parse((String)sr.get(attrIDs[12]).get()));
				user.setEndDate(df.parse((String)sr.get(attrIDs[13]).get()));
			} else {
				user.setStartDate(df_ldap.parse((String)sr.get(attrIDs[12]).get()));
				user.setEndDate(df_ldap.parse((String)sr.get(attrIDs[13]).get()));
			}
			
		} catch (Exception e) {
			
				log.debug("haloPerson start/end Date Attributes not found for " + user.getUid());
			
		}
		try {
			user.setEmployeeNo((String) sr.get(attrIDs[14]).get());
			
		} catch (Exception e) {
			log.debug("Employee Attributes not found for " + user.getUid());
			
		}
		try {
			user.setPhoneNo((String) sr.get(attrIDs[15]).get());
		} catch (Exception e) {
			log.debug("Phone Attributes not found for " + user.getUid());			
		}
		
		try {
			user.setStatus(Integer.parseInt((String) sr.get(attrIDs[16]).get()));
		} catch (Exception e) {
			user.setStatus(0);
			log.debug("haloStatus not found for {} defaulting to ENABLED(0)", user.getUid());			
		}
		
		try {
			user.setAccountLocked(Boolean.parseBoolean((String) sr.get(attrIDs[17]).get()));
		} catch (Exception e) {
			user.setAccountLocked(false);
			log.debug("nsAccountLocked not found for {} defaulting to unlocked(false)", user.getUid());			
		}
		
		try {
			// lastLogin Attributes
			user.setLastLogin(df_withTime.parse((String)sr.get(attrIDs[18]).get()));
		} catch (Exception e) {
			log.debug("lastLogin Attributes not found for " + user.getUid());
			
		}
		try {
			// lastApprover Attributes
			if (sr.get(attrIDs[19]) != null) {
				user.setLastApprover((String)sr.get(attrIDs[19]).get());
			}
		} catch (Exception e) {
			log.debug("lastApprover Attribute not found for " + user.getUid());
			
		}
		
		try {
			// lastNotified Attributes
			user.setLastNotified(df_ldap.parse((String)sr.get(attrIDs[20]).get()));
		} catch (Exception e) {
			log.debug("lastNotified Attributes not found for " + user.getUid());
			
		}
		
		try {
			user.setMatcherAttr(((String) sr.get(attrIDs[attrIDs.length-1]).get()));
		} catch (Exception e) {
			log.debug("MatcherAttribute [" + attrIDs[attrIDs.length-1] + "] not found for " + user.getUid());
			user.setMatcherAttr("");
		}
		
		return user;
	}
	
	public boolean changePassword(LDAPUser luser, String old_password, String new_password) throws Exception {
		try {
		    Hashtable<String, String> env = new Hashtable<String, String>();
			env.put("java.naming.factory.initial", "com.sun.jndi.ldap.LdapCtxFactory");
			env.put("java.naming.provider.url", properties.getProperty("ldap.provider", "")); // "ldap://aukacgbs-z2.dc-dublin.de:389/");
			env.put("java.naming.security.authentication", "simple");
			env.put("java.naming.security.principal",  luser.getEntryDN());
			env.put("java.naming.security.credentials", old_password);
			env.put("java.naming.referral", "follow");
			env.put("com.sun.jndi.ldap.connect.pool", "false");
			env.put("com.sun.jndi.ldap.connect.timeout", "5000");
			
			DirContext ctx = new InitialDirContext(env);
			
			try {
				 ModificationItem[] mods = new ModificationItem[1];
		         mods[0] = new ModificationItem(DirContext.REPLACE_ATTRIBUTE, new BasicAttribute("userPassword", new_password));       
		        
		         ctx.modifyAttributes(luser.getEntryDN(), mods);
		         
		       
				} catch (Exception e) {
					log.error("An error occured setting password for {1} [{2}]", luser.getUid(), luser.getEntryDN(), e);
					throw e;
				}
			
			  ctx.close();

		} catch (Exception ex) {
			log.error("Error changing password for {} [{}]", luser.getUid(), luser.getEntryDN(), ex);
			throw ex;
		}
		return true;
	}


	public boolean createUser(LDAPUser user, String environment) {
		 try {
			 String[] attrIDs = LDAPUser.attrIDs;
			attrIDs[attrIDs.length-1] = properties.getProperty("idm.ldap.user.attr", "description");
				
            Attributes myAttrs = new BasicAttributes();  // Case ignore
            Attribute oc = new BasicAttribute("objectClass");
            oc.add("inetOrgPerson");
            oc.add("top");
            oc.add("OrganizationalPerson");
            oc.add("person");
            oc.add("posixAccount");
            oc.add("shadowAccount");
            oc.add("haloPerson");
           
         
            myAttrs.put(oc);
           
          //  { "uid", "entryDN", "cn", "sn", "givenName", "mail", "uidNumber", "gidNumber", "homeDirectory", "userPassword", "isMemberOf", "postalAddress", "startDate", "endDate", "employeeNumber", "telephoneNumber", "haloStatus", "<matcher-field>" };
            myAttrs.put(new BasicAttribute(attrIDs[0], user.getUid()));
            myAttrs.put(new BasicAttribute(attrIDs[2], user.getCn()));
            myAttrs.put(new BasicAttribute(attrIDs[3], user.getSn()));
            myAttrs.put(new BasicAttribute(attrIDs[4], user.getGivenName()));
            myAttrs.put(new BasicAttribute(attrIDs[5], user.getEmail()));
           
            myAttrs.put(new BasicAttribute(attrIDs[6], Integer.toString(user.getUidNumber())));     
            myAttrs.put(new BasicAttribute(attrIDs[7], Integer.toString(user.getGidNumber())));
            myAttrs.put(new BasicAttribute(attrIDs[8], user.getHomeDirectory()));
            myAttrs.put(new BasicAttribute(attrIDs[9], user.getUserPassword()));   // plain or crypt?
            
            // attrID[9] : isMemberOf -> operation attribute, doesn't need saving.
            
            myAttrs.put(new BasicAttribute(attrIDs[11], user.getIntEmail()));
            myAttrs.put(new BasicAttribute(attrIDs[12], df_ldap.format(user.getStartDate()) ));
            myAttrs.put(new BasicAttribute(attrIDs[13], df_ldap.format(user.getEndDate())));
           
            
            myAttrs.put(new BasicAttribute(attrIDs[14], user.getEmployeeNo()));
            myAttrs.put(new BasicAttribute(attrIDs[15], user.getPhoneNo()));            
            myAttrs.put(new BasicAttribute(attrIDs[16], Integer.toString(user.getStatus())));
            
            myAttrs.put(new BasicAttribute(attrIDs[attrIDs.length-1], user.getMatcherAttr()));
            
            myAttrs.put(new BasicAttribute("pwdPolicySubEntry", properties.getProperty("idm.environments." + environment.toLowerCase() +  ".pwdpolicy.entry", "")));
            
            String userdn = "uid=" + user.getUid() + "," + properties.getProperty("idm.environments." + environment.toLowerCase() + ".users", "cn=users") ;
            DirContext  ctx = getContext(); //new InitialDirContext(_env);
            ctx.createSubcontext(userdn,  myAttrs);
            user.setEntryDN(userdn);
         //   try { ctx.close(); } catch (Exception ex) { }
        } catch (Exception e)
        {
            log.error("An error occured during creation of a user ",e);
           
            this.setMessage(e.toString());
            return false;
        }
        return true;
		
	}
	

	public boolean updateUserLastLogin(String dn, Date date) {
		ModificationItem[] mods = new ModificationItem[1];
		mods[0] = new ModificationItem(DirContext.REPLACE_ATTRIBUTE, new BasicAttribute(LDAPUser.attrIDs[18], df_withTime.format(date)));
		return updateUser(mods, dn);
	}
	
	public boolean updateUserLastLogin(LDAPUser user) {
		ModificationItem[] mods = new ModificationItem[1];
	    mods[0] = new ModificationItem(DirContext.REPLACE_ATTRIBUTE, new BasicAttribute(LDAPUser.attrIDs[18], df_withTime.format(user.getLastLogin())));	         
	    return updateUser(mods, user.getEntryDN());	   
	}
	
	public boolean updateUserLastNotified(LDAPUser user) {		
		ModificationItem[] mods = new ModificationItem[1];
	    mods[0] = new ModificationItem(DirContext.REPLACE_ATTRIBUTE, new BasicAttribute(LDAPUser.attrIDs[20], df_ldap.format(user.getLastNotified())));	         
	    return updateUser(mods, user.getEntryDN());	  
	}	


	public boolean updateUserLastApprover(LDAPUser user, String approver) {		
		ModificationItem[] mods = new ModificationItem[1];
	    mods[0] = new ModificationItem(DirContext.REPLACE_ATTRIBUTE, new BasicAttribute(LDAPUser.attrIDs[19], approver));	         
	    return updateUser(mods, user.getEntryDN());	  
	}
	
	public boolean updateUserStatus(LDAPUser user) {
		ModificationItem[] mods = new ModificationItem[2];
		mods[0] = new ModificationItem(DirContext.REPLACE_ATTRIBUTE, new BasicAttribute(LDAPUser.attrIDs[17], Boolean.toString(user.isAccountLocked())));
		mods[1] = new ModificationItem(DirContext.REPLACE_ATTRIBUTE, new BasicAttribute(LDAPUser.attrIDs[13], df_ldap.format(user.getEndDate())));
		 return updateUser(mods, user.getEntryDN());	  
	}
	
	public boolean updateUserOrg(LDAPUser user) {		
		String[] attrIDs = LDAPUser.attrIDs; // { "uid", "entryDN", "cn", "sn", "givenName", "mail", "uidNumber", "gidNumber", "homeDirectory", "userPassword", "isMemberOf", "postalAddress", "startDate", "endDate", "employeeNumber", "telephoneNumber", "<matcher-field>" };
		attrIDs[attrIDs.length-1] = properties.getProperty("idm.ldap.user.attr", "description");			 
		ModificationItem[] mods = new ModificationItem[1];
	    mods[0] = new ModificationItem(DirContext.REPLACE_ATTRIBUTE, new BasicAttribute(attrIDs[attrIDs.length-1], user.getMatcherAttr()));	         
	    return updateUser(mods, user.getEntryDN());	    
	}
	
	public boolean updateUser(ModificationItem[] mods, String user_dn) {
		try {
			DirContext ctx = getContext(); // new InitialDirContext(_env);
			ctx.modifyAttributes(user_dn, mods);
			// ctx.close();
			return true;
		} catch (Exception e) {
			log.error("An error updating user *matcher* details for [{}]", user_dn, e);
			log.debug("", e);
		}
		return false;
	}
	
	public boolean updateUser(LDAPUser user) {
		try {
			 String[] attrIDs = LDAPUser.attrIDs; // { "uid", "entryDN", "cn", "sn", "givenName", "mail", "uidNumber", "gidNumber", "homeDirectory", "userPassword", "isMemberOf", "postalAddress", "startDate", "endDate", "employeeNumber", "telephoneNumber", "<matcher-field>" };
			 attrIDs[attrIDs.length-1] = properties.getProperty("idm.ldap.user.attr", "description");
			 
			 ModificationItem[] mods = new ModificationItem[9];
			 mods[0] = new ModificationItem(DirContext.REPLACE_ATTRIBUTE, new BasicAttribute(attrIDs[2], user.getCn()));
	         mods[1] = new ModificationItem(DirContext.REPLACE_ATTRIBUTE, new BasicAttribute(attrIDs[3], user.getSn()));
	         mods[2] = new ModificationItem(DirContext.REPLACE_ATTRIBUTE, new BasicAttribute(attrIDs[4], user.getGivenName()));
	         mods[3] = new ModificationItem(DirContext.REPLACE_ATTRIBUTE, new BasicAttribute(attrIDs[5], user.getEmail()));	       
	         
	         mods[4] = new ModificationItem(DirContext.REPLACE_ATTRIBUTE, new BasicAttribute(attrIDs[11], user.getIntEmail()));	
	         mods[5] = new ModificationItem(DirContext.REPLACE_ATTRIBUTE, new BasicAttribute(attrIDs[12], df_ldap.format(user.getStartDate())));
	         mods[6] = new ModificationItem(DirContext.REPLACE_ATTRIBUTE, new BasicAttribute(attrIDs[13], df_ldap.format(user.getEndDate())));
	         
	         mods[7] = new ModificationItem(DirContext.REPLACE_ATTRIBUTE, new BasicAttribute(attrIDs[14], user.getEmployeeNo()));	
	         mods[8] = new ModificationItem(DirContext.REPLACE_ATTRIBUTE, new BasicAttribute(attrIDs[15], user.getPhoneNo()));	       
	        
	        // mods[4] = new ModificationItem(DirContext.REPLACE_ATTRIBUTE, new BasicAttribute(attrIDs[attrIDs.length-1], user.getMatcherAttr()));	         
	         
	         DirContext  ctx = getContext(); //new InitialDirContext(_env);
	         ctx.modifyAttributes(user.getEntryDN(), mods);
	         //ctx.close();
	         return true;
			} catch (Exception e) {
				log.error("An error updating user details for {} [{}]", user.getUid(), user.getEntryDN(), e);
				log.debug("", e); 
			}
		return false;
	}

	public boolean updateApprover(LDAPUser user) {
		try {
			 String[] attrIDs = LDAPUser.attrIDs; // { "uid", "entryDN", "cn", "sn", "givenName", "mail", "uidNumber", "gidNumber", "homeDirectory", "userPassword", "isMemberOf", "postalAddress", "startDate", "endDate", "<matcher-field>" };
			 attrIDs[attrIDs.length-1] = properties.getProperty("idm.ldap.user.attr", "description");
			 
			 ModificationItem[] mods = new ModificationItem[6];
	         //mods[0] = new ModificationItem(DirContext.REPLACE_ATTRIBUTE, new BasicAttribute("serverName", server.getServerName()));
			 mods[0] = new ModificationItem(DirContext.REPLACE_ATTRIBUTE, new BasicAttribute(attrIDs[2], user.getCn()));
	         mods[1] = new ModificationItem(DirContext.REPLACE_ATTRIBUTE, new BasicAttribute(attrIDs[3], user.getSn()));
	         mods[2] = new ModificationItem(DirContext.REPLACE_ATTRIBUTE, new BasicAttribute(attrIDs[4], user.getGivenName()));
	         mods[3] = new ModificationItem(DirContext.REPLACE_ATTRIBUTE, new BasicAttribute(attrIDs[5], user.getEmail()));	      
	         mods[4] = new ModificationItem(DirContext.REPLACE_ATTRIBUTE, new BasicAttribute(attrIDs[15], user.getPhoneNo()));	       
	        
	         mods[5] = new ModificationItem(DirContext.REPLACE_ATTRIBUTE, new BasicAttribute(attrIDs[attrIDs.length-1], user.getMatcherAttr()));	         
	         
	         DirContext  ctx = getContext(); //new InitialDirContext(_env);
	         ctx.modifyAttributes(user.getEntryDN(), mods);
	         //ctx.close();
	         return true;
			} catch (Exception e) {
				log.error("An error updating approver details for {} [{}]", user.getUid(), user.getEntryDN(), e);
				log.debug("", e); 
			}
		return false;
	}
	
	public boolean createApprover(LDAPUser user) {
		 try {
			String[] attrIDs = LDAPUser.attrIDs;
			attrIDs[attrIDs.length-1] = properties.getProperty("idm.ldap.user.attr", "description");
				
           Attributes myAttrs = new BasicAttributes();  // Case ignore
           Attribute oc = new BasicAttribute("objectClass");
           oc.add("inetOrgPerson");
           oc.add("top");
           oc.add("OrganizationalPerson");
           oc.add("person");
           oc.add("haloPerson");
           myAttrs.put(oc);
          
         // { "uid", "entryDN", "cn", "sn", "givenName", "mail", "uidNumber", "gidNumber", "homeDirectory", "userPassword", "isMemberOf", "postalAddress", "startDate", "endDate", "<matcher-field>" };
           myAttrs.put(new BasicAttribute(attrIDs[0], user.getUid()));
           myAttrs.put(new BasicAttribute(attrIDs[2], user.getCn()));
           myAttrs.put(new BasicAttribute(attrIDs[3], user.getSn()));
           myAttrs.put(new BasicAttribute(attrIDs[4], user.getGivenName()));
           myAttrs.put(new BasicAttribute(attrIDs[5], user.getEmail()));
          
           myAttrs.put(new BasicAttribute(attrIDs[9], user.getUserPassword()));   // plain or crypt?
           
           // attrID[9] : isMemberOf -> operation attribute, doesn't need saving.
           
           myAttrs.put(new BasicAttribute(attrIDs[12], df.format(user.getStartDate())));
           myAttrs.put(new BasicAttribute(attrIDs[13], df.format(user.getEndDate())));
           
           
           myAttrs.put(new BasicAttribute(attrIDs[attrIDs.length-1], user.getMatcherAttr()));
           
          // myAttrs.put(new BasicAttribute("pwdPolicySubEntry", properties.getProperty("idm.environments." + environment.toLowerCase() +  ".pwdpolicy.entry", "")));
           
           String userdn = "uid=" + user.getUid() + "," + properties.getProperty("idm.approver.user.dn", "cn=users") ;
           DirContext ctx = new InitialDirContext(_env);
           ctx.createSubcontext(userdn,  myAttrs);
           user.setEntryDN(userdn);
           try { ctx.close(); } catch (Exception ex) { }
       } catch (Exception e)
       {
           log.error("An error occured during creation of an approver ",e);
          
           this.setMessage(e.toString());
           return false;
       }
       return true;
		
	}
	
	public String generatePassword() {
        StringBuilder salt = new StringBuilder();
        Random rnd = new Random();
        while (salt.length() < 5) {
           
            salt.append(SALTCHARS_LOWER.charAt((int) (rnd.nextFloat() * SALTCHARS_LOWER.length())));
            salt.append(SALTCHARS_NUMERIC.charAt((int) (rnd.nextFloat() * SALTCHARS_NUMERIC.length())));            
            salt.append(SALTCHARS_SPECIAL.charAt((int) (rnd.nextFloat() * SALTCHARS_SPECIAL.length())));
            salt.append(SALTCHARS_UPPER.charAt((int) (rnd.nextFloat() * SALTCHARS_UPPER.length())));
        }
        String saltStr = salt.toString();
        return saltStr;
	}


	public Set<String> getOrganisations() {
		Set<String> list = new TreeSet<>();
		try {
			String[] attrIDs = { "entryDN", properties.getProperty("idm.ldap.user.attr", "description") };
			String filter = properties.getProperty("idm.ldap.groups.filter", Group.groupsfilter);
			//log.error(filter);
			NamingEnumeration<?> answer = search(attrIDs, filter, properties.getProperty("ldap.provider.basedn", ""));
			while (answer.hasMore()) {
				SearchResult sr = (SearchResult) answer.next();
				try {
					list.add(((String) sr.getAttributes().get(attrIDs[1]).get()));
				} catch (Exception e) {
					log.debug(((String) sr.getAttributes().get(attrIDs[0]).get()) + "Missing " + attrIDs[1]);
				}
			}
			answer.close();
		} catch (Exception e) {
			log.error("An error occured processing results from get LDAP Organisations retrieval.", e);
		}
		return list;
	}


	public boolean resetPassword(LDAPUser u, String pw) {
		try {
		 ModificationItem[] mods = new ModificationItem[1];
         mods[0] = new ModificationItem(DirContext.REPLACE_ATTRIBUTE, new BasicAttribute("userPassword", pw));         
         DirContext  ctx = new InitialDirContext(_env);
         ctx.modifyAttributes(u.getEntryDN(), mods);
         ctx.close();
         return true;
		} catch (Exception e) {
			log.error("An error occured resetting password for {} [{}]", u.getUid(), u.getEntryDN(), e);
		}
		return false;
	}

	public String changeUsername(LDAPUser u, String new_uid) {
		String new_dn =  u.getEntryDN().toLowerCase().replace(u.getUid().toLowerCase(), new_uid.toUpperCase());
		try {
			log.info("Renaming from [{}] -> [{}]", u.getEntryDN(), new_dn);
			log.info("Renaming from " +  u.getEntryDN() + "] -> " + new_dn + "]");
			 DirContext  ctx = new InitialDirContext(_env);
	         ctx.rename(u.getEntryDN(), new_dn);
	         ctx.close();
	         u.setEntryDN(new_dn);
	         u.setUid(new_uid);
	         return new_dn;
			} catch (Exception e) {
				log.error("An error occured during rename from [{}] -> [{}]", u.getEntryDN(), new_dn, e);
			}
			return null;
	}

	public boolean deleteGroup(String group_dn) {
		try {
			      
	         DirContext  ctx = new InitialDirContext(_env);
	         ctx.destroySubcontext(group_dn);
	         ctx.close();
	         return true;
			} catch (Exception e) {
				log.error("An error occured delting group [{}]", group_dn, e);
			}
			return false;
	}

	public boolean deleteUser(LDAPUser user) {
		try {
		      
	         DirContext  ctx = new InitialDirContext(_env);
	         ctx.destroySubcontext(user.getEntryDN());
	         ctx.close();
	         return true;
			} catch (Exception e) {
				log.error("An error occured delting user [{}]", user.getEntryDN(), e);
			}
			return false;
	}

	




	
	
}

package com.collectivesystems.idm.beans;

import java.util.List;

public class Group {
	public static final String[] attrIDs = { "cn", "entrydn", "uniqueMember", "gidNumber", "<matcher-field>" };
	public static final String groupfilter = "(&(cn=%)(objectClass=posixGroup))";
	public static final String groupsfilter = "(objectClass=posixGroup)";  
	
	
	private String cn;
	private String dn;
	private List<String> uniqueMember;
	private int gidNumber;
	private String matcherAttr;

	public Group init() {
		cn = "";
		dn = "";
		gidNumber = 100;
		matcherAttr = "";
		return this;
	}
	
	public String getCn() {
		return this.cn;
	}

	public void setCn(String cn) {
		this.cn = cn;
	}

	public List<String> getUniqueMember() {
		return this.uniqueMember;
	}

	public void setUniqueMember(List<String> uniqueMember) {
		this.uniqueMember = uniqueMember;
	}

	public String getDn() {
		return dn;
	}

	public void setDn(String dn) {
		this.dn = dn;
	}

	public String getMatcherAttr() {
		return matcherAttr;
	}

	public void setMatcherAttr(String matcherAttr) {
		this.matcherAttr = matcherAttr;
	}

	public int getGidNumber() {
		return gidNumber;
	}

	public void setGidNumber(int gidNumber) {
		this.gidNumber = gidNumber;
	}

	@Override
	public String toString() {
		return "Group [cn=" + cn + ", dn=" + dn + ", uniqueMember=" + uniqueMember + ", matcherAttr=" + matcherAttr + "]";
	}

	@Override
	public int hashCode() {
		final int prime = 31;
		int result = 1;
		result = prime * result + ((dn == null) ? 0 : dn.hashCode());
		return result;
	}

	@Override
	public boolean equals(Object obj) {
		if (this == obj) return true;
		if (obj == null) return false;
		if (getClass() != obj.getClass()) return false;
		Group other = (Group) obj;
		if (dn == null) {
			if (other.dn != null) return false;
		} else if (!dn.equals(other.dn)) return false;
		return true;
	}

	

	
}

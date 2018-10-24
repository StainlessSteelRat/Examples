package io.echoplex.web.beans;

import java.util.EnumSet;

public class Echo {

	public enum Flag {
	    OWNER, FOLLOWERS, DECENDANTS, PUBLIC;

	    public static final EnumSet<Flag> ALL_OPTS = EnumSet.allOf(Flag.class);
	}
	
	
	private String owner;
	private String msgOwner;
	private String msgFollowers;
	private String msgDecendants;
	private String msgPublic;
	
	private Enum<Flag> privacy;
	
	private String image;
	
	private double lng = 0.0f;
	private double lat = 0.0f;

	public String getOwner() {
		return owner;
	}

	public void setOwner(String owner) {
		this.owner = owner;
	}

	public Enum<Flag> getPrivacy() {
		return privacy;
	}

	public void setPrivacy(Enum<Flag> privacy) {
		this.privacy = privacy;
	}

	public String getImage() {
		return image;
	}

	public void setImage(String image) {
		this.image = image;
	}

	public String getMsgOwner() {
		return msgOwner;
	}

	public void setMsgOwner(String msgOwner) {
		this.msgOwner = msgOwner;
	}

	public String getMsgFollowers() {
		return msgFollowers;
	}

	public void setMsgFollowers(String msgFollowers) {
		this.msgFollowers = msgFollowers;
	}

	public String getMsgDecendants() {
		return msgDecendants;
	}

	public void setMsgDecendants(String msgDecendants) {
		this.msgDecendants = msgDecendants;
	}

	public String getMsgPublic() {
		return msgPublic;
	}

	public void setMsgPublic(String msgPublic) {
		this.msgPublic = msgPublic;
	}
	
	public double getLng() {
		return lng;
	}

	public void setLng(double lng) {
		this.lng = lng;
	}

	public double getLat() {
		return lat;
	}

	public void setLat(double lat) {
		this.lat = lat;
	}

	@Override
	public String toString() {
		return "Echo [owner=" + owner + ", msgOwner=" + msgOwner + ", msgFollowers=" + msgFollowers + ", msgDecendants="
				+ msgDecendants + ", msgPublic=" + msgPublic + ", privacy=" + privacy + ", image=" + image + "]";
	}

	@Override
	public int hashCode() {
		final int prime = 31;
		int result = 1;
		long temp;
		temp = Double.doubleToLongBits(lat);
		result = prime * result + (int) (temp ^ (temp >>> 32));
		temp = Double.doubleToLongBits(lng);
		result = prime * result + (int) (temp ^ (temp >>> 32));
		result = prime * result + ((owner == null) ? 0 : owner.hashCode());
		return result;
	}

	@Override
	public boolean equals(Object obj) {
		if (this == obj)
			return true;
		if (obj == null)
			return false;
		if (getClass() != obj.getClass())
			return false;
		Echo other = (Echo) obj;
		if (Double.doubleToLongBits(lat) != Double.doubleToLongBits(other.lat))
			return false;
		if (Double.doubleToLongBits(lng) != Double.doubleToLongBits(other.lng))
			return false;
		if (owner == null) {
			if (other.owner != null)
				return false;
		} else if (!owner.equals(other.owner))
			return false;
		return true;
	}

	
	
	
	
}

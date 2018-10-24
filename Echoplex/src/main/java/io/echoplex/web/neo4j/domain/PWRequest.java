package io.echoplex.web.neo4j.domain;

import java.util.Date;

import org.neo4j.ogm.annotation.GeneratedValue;
import org.neo4j.ogm.annotation.Id;
import org.neo4j.ogm.annotation.NodeEntity;
import org.springframework.security.crypto.scrypt.SCryptPasswordEncoder;

import io.echoplex.web.utils.Utils;

@NodeEntity
public class PWRequest {
	@Override
	public String toString() {
		return "PWRequest [id=" + id + ", userId=" + userId + ", password=" + password + ", validationCode=" + validationCode + ", requestDate=" + requestDate + ", type=" + type + "]";
	}

	public static enum Type {
		PW_RESET, 
		SIGN_UP;		
		private static  Type[] cached_values = null;
		public static Type fromInt(int i) {
			if (Type.cached_values == null) { Type.cached_values = Type.values(); }
			return Type.cached_values[i];
		}
	};

	@Id @GeneratedValue Long id;

	private String userId;
	private String password;
	private String validationCode;
	private Date requestDate;
	private Type type;

	public PWRequest() { }

	public static String generateValidateionCode(Type type) {
		return type.ordinal() + Utils.encode(Utils.nextSessionId()).toUpperCase();
	}
	
	public static String generatePassword() {
		return Utils.encode(Utils.nextSessionId());
	}
	
	

	public String getUserId() {
		return userId;
	}

	public void setUserId(String userId) {
		this.userId = userId;
	}

	public String getPassword() {
		return password;
	}

	public void setPassword(String password) {
		SCryptPasswordEncoder s = new SCryptPasswordEncoder();

		this.password = s.encode(password);
	}

	public String getValidationCode() {
		return validationCode;
	}

	public void setValidationCode(String validationCode) {
		this.validationCode = validationCode;
	}

	public Date getRequestDate() {
		return requestDate;
	}

	public void setRequestDate(Date requestDate) {
		this.requestDate = requestDate;
	}

	public Type getType() {
		return type;
	}

	public void setType(Type type) {
		this.type = type;
	}

}

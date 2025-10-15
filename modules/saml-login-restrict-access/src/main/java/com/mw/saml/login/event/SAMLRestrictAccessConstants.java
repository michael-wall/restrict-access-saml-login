package com.mw.saml.login.event;

public class SAMLRestrictAccessConstants {
	
	public static final String LOGOUT_PATH = "/c/portal/logout";
	
	public interface PORTAL_PROPERTIES {
		public static final String RESTRICT_ACCESS_LOGIN_EVENT_ENABLED = "restrict.access.login.event.enabled";
		
		public static final String RESTRICT_ACCESS_LOGIN_EVENT_REGULAR_ROLE_IDS = "restrict.access.login.event.regularRoleIds";
		
		public static final String RESTRICT_ACCESS_LOGIN_EVENT_SITE_GROUP_IDS = "restrict.access.login.event.siteGroupIds";
		
		public static final String RESTRICT_ACCESS_LOGIN_EVENT_SITE_ROLE_IDS = "restrict.access.login.event.siteRoleIds";
	}
}
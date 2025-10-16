package com.mw.saml.login.event;

import com.liferay.portal.kernel.cookies.CookiesManagerUtil;
import com.liferay.portal.kernel.cookies.constants.CookiesConstants;
import com.liferay.portal.kernel.events.ActionException;
import com.liferay.portal.kernel.events.LifecycleAction;
import com.liferay.portal.kernel.events.LifecycleEvent;
import com.liferay.portal.kernel.log.Log;
import com.liferay.portal.kernel.log.LogFactoryUtil;
import com.liferay.portal.kernel.model.User;
import com.liferay.portal.kernel.security.permission.PermissionChecker;
import com.liferay.portal.kernel.security.permission.PermissionCheckerFactoryUtil;
import com.liferay.portal.kernel.service.RoleLocalService;
import com.liferay.portal.kernel.service.UserLocalService;
import com.liferay.portal.kernel.util.GetterUtil;
import com.liferay.portal.kernel.util.Validator;
import com.liferay.portal.util.PropsUtil;
import com.liferay.saml.persistence.model.SamlSpSession;
import com.liferay.saml.persistence.service.SamlSpSessionLocalService;
import com.liferay.saml.runtime.servlet.profile.WebSsoProfile;

import java.io.IOException;
import java.util.Arrays;
import java.util.Map;
import java.util.Optional;
import java.util.Set;
import java.util.stream.Collectors;

import javax.servlet.http.Cookie;
import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;
import javax.servlet.http.HttpSession;

import org.osgi.service.component.annotations.Activate;
import org.osgi.service.component.annotations.Component;
import org.osgi.service.component.annotations.Reference;

@Component(
	immediate = true, property = "key=login.events.pre",
	service = {
		LifecycleAction.class,
	}
)
public class SAMLRestrictAccessLoginEvent implements LifecycleAction {
	
	@Activate
	protected void activate(Map<String, Object> properties) {
		if (_log.isInfoEnabled()) _log.info("Activating");
		
		_restrictAccessEnabled = GetterUtil.getBoolean(PropsUtil.get(SAMLRestrictAccessConstants.PORTAL_PROPERTIES.RESTRICT_ACCESS_LOGIN_EVENT_ENABLED), false);
		
		_log.info("restrictAccessEnabled: " + _restrictAccessEnabled);

		if (_restrictAccessEnabled) {
			regularRoleIds = getIdsArray(GetterUtil.getString(PropsUtil.get(SAMLRestrictAccessConstants.PORTAL_PROPERTIES.RESTRICT_ACCESS_LOGIN_EVENT_REGULAR_ROLE_IDS), ""));
			siteGroupIds = getIdsArray(GetterUtil.getString(PropsUtil.get(SAMLRestrictAccessConstants.PORTAL_PROPERTIES.RESTRICT_ACCESS_LOGIN_EVENT_SITE_GROUP_IDS), ""));
			siteRoleIds = getIdsArray(GetterUtil.getString(PropsUtil.get(SAMLRestrictAccessConstants.PORTAL_PROPERTIES.RESTRICT_ACCESS_LOGIN_EVENT_SITE_ROLE_IDS), ""));
		}
		
		if (_log.isInfoEnabled()) _log.info("Activated");
	}		

	@Override
	public void processLifecycleEvent(LifecycleEvent lifecycleEvent) throws ActionException {
		
		if (!_restrictAccessEnabled) return; // Don't do anything...
		
		SamlSpSession samlSpSession = getSamlSpSession(lifecycleEvent);
				
		if (Validator.isNull(samlSpSession)) return; // Don't do anything if not a SAML Login...
		
		_log.debug("samlSpSession createDate: " + samlSpSession.getCreateDate());
		
		HttpServletRequest httpServletRequest = lifecycleEvent.getRequest();
        HttpServletResponse httpServletResponse = lifecycleEvent.getResponse();
		
		long userId = samlSpSession.getUserId();
				
		User user = _userLocalService.fetchUserById(userId);
		
		if (user != null) {
			_log.info("Verifying Roles for: " + user.getFullName());
			
			PermissionChecker permissionChecker = PermissionCheckerFactoryUtil.create(user);
			
			if (hasRestrictedRegularRole(permissionChecker) || hasRestrictedSiteRole(permissionChecker)) {		
				_log.info("Forcing logout for: " + permissionChecker.getUser().getFullName());

	            try {
	        		Cookie cookie = new Cookie(SAMLRestrictAccessConstants.COOKIES.SAML_LOGIN_RESTRICT_ACCESS, SAMLRestrictAccessException.class.getSimpleName());

	        		cookie.setMaxAge(3); // Persistent for 3 seconds

	        		CookiesManagerUtil.addCookie(CookiesConstants.CONSENT_TYPE_NECESSARY, cookie, httpServletRequest, httpServletResponse);
	            	
	            	 // Force logout by redirecting to /c/portal/logout
	            	httpServletResponse.sendRedirect(httpServletRequest.getContextPath() + SAMLRestrictAccessConstants.LOGOUT_PATH);
	            	
	            } catch (IOException e) {
	            	_log.error(e.getClass() + ": " + e.getMessage());
	            	
	            	 //Fallback
					HttpSession httpSession = httpServletRequest.getSession(false);
		            if (httpSession != null) httpSession.invalidate();
	            	
	                throw new ActionException(e);
	            }	            
			} else {
				_log.info("User hasn't got restricted role: " + permissionChecker.getUser().getFullName());
			}
		}
	}
	
	private boolean hasRestrictedRegularRole(PermissionChecker permissionChecker) {
		if (permissionChecker.isOmniadmin() || permissionChecker.isCompanyAdmin(permissionChecker.getCompanyId())) return true;
		
		// Check based on custom regular role restrictions from restrict.access.login.event.regularRoleIds
		if (regularRoleIds != null || regularRoleIds.length > 0) {
			long[] usersRegularRoleIds = getUsersRegularRoleIds(permissionChecker);
			
			if (usersRegularRoleIds != null && usersRegularRoleIds.length > 0) {
				Set<Long> restrictedRoleSet = Arrays.stream(regularRoleIds).boxed().collect(Collectors.toSet());

				for (long usersRegularRoleId : usersRegularRoleIds) {
				    if (restrictedRoleSet.contains(usersRegularRoleId)) return true;
				}	
			}
		}
		
		return false;
	}	
	
	private long[] getUsersRegularRoleIds(PermissionChecker permissionChecker) {
		try {
			return permissionChecker.getUserBag().getRoleIds();
		} catch (Exception e) {
			_log.error(e.getClass() + ": " + e.getMessage(), e);
			
			return null;
		}
	}
	
	private boolean hasRestrictedSiteRole(PermissionChecker permissionChecker) {
		if (siteGroupIds == null || siteGroupIds.length == 0) return false;
		
		for (long siteGroupId: siteGroupIds) {
			if (permissionChecker.isGroupAdmin(siteGroupId) || permissionChecker.isGroupOwner(siteGroupId)) return true;
			if (permissionChecker.isContentReviewer(permissionChecker.getCompanyId(), siteGroupId)) return true;

			// Check based on custom site role restrictions from restrict.access.login.event.siteRoleIds
			if (siteRoleIds != null || siteRoleIds.length > 0) {
				long[] usersSiteRoleIds = permissionChecker.getRoleIds(permissionChecker.getUserId(), siteGroupId);
				
				if (usersSiteRoleIds != null && usersSiteRoleIds.length > 0) {
					Set<Long> restrictedRoleSet = Arrays.stream(siteRoleIds).boxed().collect(Collectors.toSet());

					for (long usersSiteRoleId : usersSiteRoleIds) {
					    if (restrictedRoleSet.contains(usersSiteRoleId)) return true;
					}					
				}
			}
		}
		
		return false;
	}
	
	private long[] getIdsArray(String idsString) {
		if (Validator.isNull(idsString)) return null;

		long[] idsArray = Arrays.stream(
		        Optional.ofNullable(idsString).orElse("").split(","))
		    .map(String::trim)
		    .filter(s -> !s.isEmpty())
		    .mapToLong(Long::parseLong)
		    .toArray();

		return idsArray;
	}

	private SamlSpSession getSamlSpSession(LifecycleEvent lifecycleEvent) {
		try {
			SamlSpSession samlSpSession = _webSsoProfile.getSamlSpSession(lifecycleEvent.getRequest());
			
			return samlSpSession;
		} catch (Exception e) {
			return null;
		}
	}

	@Reference
	private RoleLocalService _roleLocalService;
	
	@Reference
	private UserLocalService _userLocalService;
	
	@Reference
	private WebSsoProfile _webSsoProfile;
	
	@Reference
	private SamlSpSessionLocalService _samlSpSessionLocalService;	
	
	private boolean _restrictAccessEnabled = false;

	private long regularRoleIds[];
	private long siteGroupIds[];
	private long siteRoleIds[];
	
	private static final Log _log = LogFactoryUtil.getLog(SAMLRestrictAccessLoginEvent.class);	 
}
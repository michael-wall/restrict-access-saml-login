package com.mw.saml.login.event;

import com.liferay.portal.kernel.cookies.CookiesManagerUtil;
import com.liferay.portal.kernel.log.Log;
import com.liferay.portal.kernel.log.LogFactoryUtil;
import com.liferay.portal.kernel.servlet.SessionMessages;
import com.liferay.portal.kernel.servlet.taglib.BaseJSPDynamicInclude;
import com.liferay.portal.kernel.servlet.taglib.DynamicInclude;
import com.liferay.portal.kernel.theme.ThemeDisplay;
import com.liferay.portal.kernel.util.GetterUtil;
import com.liferay.portal.kernel.util.Portal;
import com.liferay.portal.kernel.util.Validator;
import com.liferay.portal.kernel.util.WebKeys;
import com.liferay.portal.util.PropsUtil;

import java.io.IOException;
import java.util.Map;

import javax.servlet.ServletContext;
import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;
import javax.servlet.http.HttpSession;

import org.osgi.service.component.annotations.Activate;
import org.osgi.service.component.annotations.Component;
import org.osgi.service.component.annotations.Reference;

@Component(
	immediate = true,
	service = DynamicInclude.class)
public class SAMLRestrictAccessJSPDynamicInclude extends BaseJSPDynamicInclude {
	
	@Activate
	protected void activate(Map<String, Object> properties) {
		if (_log.isInfoEnabled()) _log.info("Activating");
		
		_restrictAccessEnabled = GetterUtil.getBoolean(PropsUtil.get(SAMLRestrictAccessConstants.PORTAL_PROPERTIES.RESTRICT_ACCESS_LOGIN_EVENT_ENABLED), false);
		
		_log.info("restrictAccessEnabled: " + _restrictAccessEnabled);

		if (_log.isInfoEnabled()) _log.info("Activated");
	}		

	@Override
	public ServletContext getServletContext() {
		return _servletContext;
	}

	@Override
	public void include(
			HttpServletRequest httpServletRequest,
			HttpServletResponse httpServletResponse, String key)
		throws IOException {
		
		if (!_restrictAccessEnabled) return; // Don't do anything...
		
		ThemeDisplay themeDisplay = (ThemeDisplay)httpServletRequest.getAttribute(WebKeys.THEME_DISPLAY);
		
		if (themeDisplay.isSignedIn()) return; // Don't do anything...
		
		HttpServletRequest originalHttpServletRequest = _portal.getOriginalServletRequest(httpServletRequest);

		HttpSession httpSession = originalHttpServletRequest.getSession(false);
		
		if (httpSession == null) return; // Don't do anything...
		
		String cookieValue = CookiesManagerUtil.getCookieValue(SAMLRestrictAccessConstants.COOKIES.SAML_LOGIN_RESTRICT_ACCESS, originalHttpServletRequest);
			
		if (Validator.isNull(cookieValue)) return; // Don't do anything...
		
		if (cookieValue.equalsIgnoreCase(SAMLRestrictAccessException.class.getSimpleName())) {
			SessionMessages.add(httpServletRequest, cookieValue);

			super.include(httpServletRequest, httpServletResponse, key);			
		}
	}

	@Override
	public void register(
		DynamicInclude.DynamicIncludeRegistry dynamicIncludeRegistry) {

		dynamicIncludeRegistry.register("/html/common/themes/bottom.jsp#pre");
	}

	@Override
	protected String getJspPath() {
		return "/dynamic_include/saml_restrict_access_error.jsp";
	}

	@Override
	protected Log getLog() {
		return _log;
	}

	@Reference
	private Portal _portal;

	@Reference(target = "(osgi.web.symbolicname=com.mw.saml.login.restrict.access)")
	private ServletContext _servletContext;
	
	private boolean _restrictAccessEnabled = false;
	
	private static final Log _log = LogFactoryUtil.getLog(SAMLRestrictAccessJSPDynamicInclude.class);
}
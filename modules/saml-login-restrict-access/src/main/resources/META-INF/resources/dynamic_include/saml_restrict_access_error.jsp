<%@ include file="./init.jsp" %>

<c:if test='<%= SessionMessages.contains(request, SAMLRestrictAccessException.class.getSimpleName()) %>'>
	<liferay-util:buffer var="msg">
		<liferay-ui:message key="you-are-not-permitted-to-log-in-to-this-node-please-log-in-to-the-admin-node-instead" />
	</liferay-util:buffer>
	
	<aui:script>
		if (typeof displayedSAMLRestrictAccessException === 'undefined' || displayedSAMLRestrictAccessException !== true) {
			var displayedSAMLRestrictAccessException = true;
			Liferay.Util.openToast({
				message: '<%= HtmlUtil.escapeJS(msg) %>',
				type: 'danger',
				autoClose: 10000,
			});		
		}
	</aui:script>
</c:if>
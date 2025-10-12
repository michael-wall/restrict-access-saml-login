## Introduction ##
- This 'proof of concept' is designed for the scenario of preventing users with Administrator or other privileged Roles from logging into specific node(s) in a Liferay DXP implementation that has 'public' and 'private' nodes.
- The implementation is a custom OSGi module containing a LifecycleAction OSGi component that will check a SAML users Regular Roles and Site Roles in Liferay and determine whether a forced logout should be triggered for the user.
- It is specifically designed for use with SAML SSO where Liferay is the SAML SP using the out of the box SAML SP implementation. If the user has not logged in with out of the box Liferay SAML SP then it will not do anything.

## Performed Checks ##
- Based on the setup it will force a logout in the following scenarios for a SAML SSO user:
  - If the user has the Administrator Role or the Omni Administrator Role for the Virtual Instance.
  - If the user has any of the specified Regular Roles in the Virtual Instance (as defined in the custom ...regularRoleIds portal property).
  - If the user has the Site Owner Role or the Site Administrator Role in any of the specified Sites (as defined in the custom ...siteGroupIds portal property).
  - If the user has the Content Reviewer Role for one of the specified Sites (as defined in the custom ...siteGroupIds portal property).
  - If the user has any of the specified Site Roles in any of the specified Sites (as defined in the custom ...siteGroupIds and ...siteRoleIds portal properties).

## Setup ##
- Add the following portal properties to the specific node(s) of the target environment cluster where the privileged users should NOT be able to login:

| Property  | Type | Sample | Description |
| -------- | ------- | ------- |  ------- |
| restrict.access.login.event.enabled | boolean | true | Set to true to enforce the restrictions. Set to false to not enforce restrictions.|
| restrict.access.login.event.regularRoleIds | comma separated IDs | 43787,43788,43791 | Comma separated list of Regular roleIds. These can be roleIds from out of the box Regular Roles or custom Regular Roles. Leave empty or don't include if this check not required. |
| restrict.access.login.event.siteGroupIds | comma separated IDs | 43375,43482,43555 | Comma separated list of Site groupIds. These are the Sites whose Site Roles should be checked. Leave empty or don't include if this check is not required. |
| restrict.access.login.event.siteRoleIds | comma separated IDs | 43793,43794,43797 | Comma separated list of Site Role roleIds. These can be the roleIds from out of the box Site Roles or custom Site Roles. If using this check then the ...siteGroupIds property must also be populated. Leave empty or don't include if not applicable |

- Build the custom OSGi module.
- Start the Liferay node and custom OSGi module.
- Confirm that the custom OSGi module deploys without any errors for example:
```
2025-10-12 16:04:41.415 INFO  [fileinstall-directory-watcher][SAMLRestrictAccessLoginEvent:45] Activating
2025-10-12 16:04:41.416 INFO  [fileinstall-directory-watcher][SAMLRestrictAccessLoginEvent:49] restrictAccessEnabled: true
2025-10-12 16:04:41.434 INFO  [fileinstall-directory-watcher][SAMLRestrictAccessLoginEvent:57] Activated
```
- Test the various scenarios based on the custom portal properties.

## Logging ##
- The logging for SAMLRestrictAccessLoginEvent class is all INFO level for test purposes. Change some (or all) to DEBUG as required.
- Sample logging where a user has a restricted Role:
```
2025-10-12 16:06:28.318 INFO  [http-nio-8080-exec-10][SAMLRestrictAccessLoginEvent:69] samlSpSession createDate: Sun Oct 12 16:06:28 GMT 2025
2025-10-12 16:06:28.320 INFO  [http-nio-8080-exec-10][SAMLRestrictAccessLoginEvent:79] Verifying Roles for: arthur beesley
2025-10-12 16:06:28.355 INFO  [http-nio-8080-exec-10][SAMLRestrictAccessLoginEvent:84] Forcing logout for: arthur beesley
```
- Sample logging where a user doesn't have a restricted Role:
```
2025-10-12 16:11:08.994 INFO  [http-nio-8080-exec-10][SAMLRestrictAccessLoginEvent:69] samlSpSession createDate: Sun Oct 12 16:11:08 GMT 2025
2025-10-12 16:11:08.995 INFO  [http-nio-8080-exec-10][SAMLRestrictAccessLoginEvent:79] Verifying Roles for: barry white
2025-10-12 16:11:09.008 INFO  [http-nio-8080-exec-10][SAMLRestrictAccessLoginEvent:97] User hasn't got restricted role: barry white
```

## Notes ##
- This is a ‘proof of concept’ that is being provided ‘as is’ without any support coverage or warranty.
- The implementation uses a custom OSGi module meaning it is compatible with Liferay DXP Self-Hosted and Liferay PaaS. It is not compatible with Liferay SaaS.
- The implementation was tested locally using Liferay DXP 2025.Q1.0 LTS configured as SAML SP and Keycloak configured as SAML IdP.
- JDK 21 is expected for both compile time and runtime.
- The module can be deployed to all nodes in the cluster, but ensure that restrict.access.login.event.enabled is not set to true on each node, otherwise privileged users will be prevented from logging to any of the cluster nodes with SAML SSO.

## Introduction ##
- This 'proof of concept' is designed for the scenario of preventing users with Administrator or other privileged Roles from logging into specific node(s) in a Liferay DXP implementation that has 'public' and 'private' nodes.
- The implementation is a custom OSGi module containing a login.events.post LifecycleAction OSGi component that will check a SAML users Regular Roles and Site Roles in Liferay and determine whether a forced logout should be triggered for the user.
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
| restrict.access.post.login.event.enabled | boolean | true | Set to true to enforce the restrictions. Set to false to not enforce restrictions.|
| restrict.access.post.login.event.regularRoleIds | comma separated IDs | 43787,43788,43791 | Comma separated list of Regular roleIds. These can be roleIds from out of the box Regular Roles or custom Regular Roles. Leave empty or don't include if this check not required. |
| restrict.access.post.login.event.siteGroupIds | comma separated IDs | 43375,43482,43555 | Comma separated list of Site groupIds. These are the Sites whose Site Roles should be checked. Leave empty or don't include if this check is not required. |
| restrict.access.post.login.event.siteRoleIds | comma separated IDs | 43793,43794,43797 | Comma separated list of Site Role roleIds. These can be the roleIds from out of the box Site Roles or custom Site Roles. If using this check then the ...siteGroupIds property must also be populated. Leave empty or don't include if not applicable |

- Build the custom OSGi module.
- Start the Liferay node and custom OSGi module.
- Confirm that the custom OSGi module deploys without any errors.
- Test the various scenarios based on the custom portal properties.

## Notes ##
- This is a ‘proof of concept’ that is being provided ‘as is’ without any support coverage or warranty.
- The implementation uses a custom OSGi module meaning it is compatible with Liferay DXP Self-Hosted and Liferay PaaS. It is not compatible with Liferay SaaS.
- The implementation was tested locally using Liferay DXP 2025.Q1.0 LTS configured as SAML SP and Keycloak configured as SAML IdP.
- JDK 21 is expected for both compile time and runtime.
- The module can be deployed to all nodes in the cluster, but ensure that restrict.access.post.login.event.enabled is not set to true on each node, otherwise privileged users will be prevented from logging to any of the cluster nodes with SAML SSO.

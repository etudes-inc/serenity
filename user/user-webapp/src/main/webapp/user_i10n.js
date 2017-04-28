/**********************************************************************************
 * $URL: https://source.etudes.org/svn/serenity/trunk/user/user-webapp/src/main/webapp/user_i10n.js $
 * $Id: user_i10n.js 12504 2016-01-10 00:30:08Z ggolden $
 ***********************************************************************************
 *
 * Copyright (c) 2014, 2015 Etudes, Inc.
 * 
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 * 
 *      http://www.apache.org/licenses/LICENSE-2.0
 * 
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 *
 **********************************************************************************/

user_i10n =
{
	// native: "en-us",
	"en-us":
	{
		title: "Etudes Users",
		titlebar: "Users",

		a_add: {html: "Add", title: "Add New User"},
		a_purge : {html: "Purge", title: "Purge Selected user(s)"},
		a_first: {title:"First Page"},
		a_prev: {title:"Previous Page"},
		a_next: {title:"Next Page"},
		a_last: {title:"Last Page"},

		msg_page: "Page %0 (User %1 - %2 of %3)",
		msg_iid: "IID",
		msg_loginId: "Login Id",
		msg_email: "Email",
		msg_name: "Name",
		msg_search: "Search",
		msg_clear: "Clear",
		msg_noneDefined: "No items are defined",
		msg_edit: "Edit %0",
		msg_none: "<i>none</i>",

		header_id: "ID",
		header_name: "Name",
		header_loginId: "Login Id",
		header_iid: "IID",
		header_email: "Email",
		header_avatar: "Avatar",
		header_user: "User Information",
		header_password: "Password",
		header_signature: "Signature",
		header_connect: "Connect Information",
		header_profile: "Profile Information",
		header_timezone: "Timezone",

		label_firstName: "First Name:",
		label_lastName: "Last Name:",
		label_loginId: "Login ID:",
		label_iid: "Institutional Id(s):",
		label_pw: "New Password:",
		label_confirmPw: "Verify:",
		label_email: "Email:",
		label_shareEmail: "Share with other users",
		label_aim: "Aim:",
		label_facebook: "Facebook:",
		label_googlePlus: "Google+:",
		label_linkedIn: "LinkedIn:",
		label_skype: "Skype:",
		label_twitter: "Twitter:",
		label_web: "Web:",
		label_interests: "Interests:",
		label_location: "Location:",
		label_occupation: "Occupation:",
		label_avatar: "Pick New Avatar:",
		label_removeAvatar: "Remove Avatar",
		
		action_purge: "Purge",
		action_save: "Save",
		action_discard: "Discard",
	
		ph_name: "Partial Last, First Name",
		ph_iid: "Full IID",
		ph_loginId: "Full Login ID",
		ph_email: "Full email",

		title_confirmExit: {title: "Save?"},
		msg_confirmExit: "You have unsaved changes.",

		title_confirmPurge: {title: "Purge User"},
		msg_confirmPurge: "Are you sure you want to purge the selected user(s)?  They will be removed from all sites, and all of their content will be removed.",

		title_nothingSelected: {title: "Nothing Selected"},
		msg_nothingSelected: "First select one or more users before performing that action."
	}
};

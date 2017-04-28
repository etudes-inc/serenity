/**********************************************************************************
 * $URL: https://source.etudes.org/svn/serenity/trunk/roster/roster-webapp/src/main/webapp/siteroster_i10n.js $
 * $Id: siteroster_i10n.js 11264 2015-07-15 16:44:36Z ggolden $
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

siteroster_i10n =
{
	// native: "en-us",
	"en-us":
	{
		title: "Etudes Site Roster",
		titlebar: "Roster",

		header_roster: "ROSTER",
		a_setup: {title: "Setup", html: "Setup"},
		a_add: {title: "Add", html: "Add"},
		a_remove: {title: "Remove", html:"Remove"},
		a_block: {title: "Block", html:"Block"},
		a_unblock: {title: "Unblock", html:"Unblock"},
		a_role: {title: "Role", html:"Role"},

		msg_enrolled: "enrolled",
		msg_dropped: "dropped",
		msg_blocked: "blocked",

		// words
		Active: "Active",
		Inactive: "Inactive",
		Guest: "Guest",
		Observer: "Observer",
		Student: "Student",
		TA: "TA",
		Instructor: "Instructor",
		Administrator: "Administrator",
		None: "None",
		Add: "Add",
		Remove: "Remove",
		Role: "Role",
		Block: "Block",
		Unblock: "Unblock",
		Name: "Name",
		Section: "Section",
		"Special Access": "Special Access",
		Roster: "Roster",
		Groups: "Groups",
		Status: "Status",
		Enrolled: "Enrolled",
		Dropped: "Dropped",
		Blocked: "Blocked",
		Added: "Added",
		Assign: "Assign",
		Active: "Active",
		Site: "Site",
		Setup: "Setup",

		registrarRoleWarning: "Note: Registrar members' roles can be set only by your campus Registrar.",

		// roster
		addedUser: "Added User",
		registrarUser: "Registrar User",
		blockedUser: "Blocked User",
		nothingSelectedTitle: {title: "Nothing Selected"},
		nothingSelected: "First select one or more members, using the checkboxes.",
		confirmRemoveTitle: {title: "Remove Members"},
		confirmRemove: "Are you sure you want to remove the selected members?  Members unable to be removed will be blocked.",
		confirmBlockTitle: {title: "Block Members"},
		confirmBlock: "Are you sure you want to block the selected members, denying them access to the site?",
		confirmUnblockTitle: {title: "Unblock Members"},
		confirmUnblock: "Are you sure you want to unblock the selected members, restoring their access to the site?",
		confirmRoleTitle: {title: "Role"},
		confirmAddTitle: {title: "Add Members"},
		student: "student",
		students: "students",
		summary: "Summary of Enrollment:<ul><li>%0 %1 enrolled</li><li>%2 %3 dropped</li><li>%4 %5 blocked</li></ul>",
		resultsTitle: {title: "Results"},
		blocked: "Blocked: %0",
		alreadyBlocked: "<i>%0 is already blocked</i>",
		notBlocked: "<i>%0 may not be blocked</i>",
		unblocked: "Unblocked: %0",
		alreadyUnblocked: "<i>%0 is not blocked</i>",
		blockedEnrolled: "Blocked (E)",
		blockedDropped: "Blocked (D)",
		removed: "Removed: %0",
		notInSite: "<i>%0 is not a site member</i>",
		notRemoved: "<i>%0 may not be removed</i>",
		conflict: "<i>%1 not used - identifies more than one user</i>",
		notAdded: "<i>%1 not used - did not identify a user, could not be used for a user email address</i>",
		alreadyMember: "<i>%1 identifies user: %0 - already a site member</i>",
		userCreated: "%1 used to create a new user - added to the site",
		userAdded: "%1 identifies user: %0 - added to the site",
		notInSite: "<i>%0 is not in the site</i>",
		notAdhoc: "<i>%0 is a Registrar user, the role cannot be changed</i>",
		roleChanged: "%0 role set",
		identifyMembers: "Identify Members",
		addMemberInstructions: "Add new members to your site.  This is for guest members who are not officially registered. It is best NOT to add students in this way - rely on your school's registration process to give your students access to your site.",
		users: "Users:",
		role: "Role:"
	}
};

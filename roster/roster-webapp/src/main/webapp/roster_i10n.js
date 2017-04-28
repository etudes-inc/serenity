/**********************************************************************************
 * $URL: https://source.etudes.org/svn/serenity/trunk/roster/roster-webapp/src/main/webapp/roster_i10n.js $
 * $Id: roster_i10n.js 12072 2015-11-13 03:14:18Z ggolden $
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

roster_i10n =
{
	// native: "en-us",
	"en-us":
	{
		title: "Etudes Rosters",
		titlebar: "Rosters",

		// words
		Rosters: "Rosters",
		Adhoc: "Adhoc",
		Files: "Files",
		Rosters: "Rosters",
		"Site Roster": "Site Roster",
		Reports: "Reports",
		Results: "Results",
		Load: "Load",
		View: "View",
		Add: "Add",
		Remove: "Remove",
		Process: "Process",
		Mapping: "Mapping",
		Sites: "Sites",
		Report: "Report",
		Print: "Print",
		Export: "Export",
		roster: "roster",
		site: "site",
		mapping: "mapping",
		loading: "Loading ...",
		Role: "Role",
		Name: "Name",
		IID: "IID",
		EID: "EID",
		Active: "Active",
		Inactive: "Inactive",
		Guest: "Guest",
		Observer: "Observer",
		Student: "Student",
		TA: "TA",
		Instructor: "Instructor",
		Administrator: "Administrator",
		None: "None",

		// shared
		label_allTerms: "Show All Terms",
		label_client: "Client:",
		label_term: "Term:",

		// adhoc mode
		adhocHeader: "Adhoc Roster Lines",
		instructions_adhocClientTerm: "Set Client and Term if any site names in the roster lines use a non-standard format.",
		label_rosterLines: "Roster Lines",
		instructions_createInstructor: "To create sites for instructors who are not yet in Etudes, or are not in the Users Group, check here.",
		label_createInstructors: "Create Sites (for new or non-UG users)",
		instructions_addToUG: "To add instructors to the Users Group, check here.",
		lavbel_addToUG: "Add To Users Group",
		instructions_createSitesOnly: "If you want to create sites with no sections, check here.",
		label_createSitesOnly: "Sites Without Sections",

		// files mode
		header_readyProcess: "Roster Files Ready To Process",
		header_fileSchedule: "Scheduled Roster Processing (On This Server)",
		
		// rosters mode
		header_clientTerm: "Client and Term",
		header_organizeFilter: "Organize and Filter",
		label_organizeBy: "Organize By:",
		title_helpOrganize: {title: "Help Organize"},
		label_siteFilter: "Site:",
		label_rosterFilter: "Roster:",
		msg_filter: "Filters",
		title_viewRoster: "View Roster",
		noMatch: "no match",
		report_rosters: "%0 rosters found for %1 in %2",
		siteEquals: "Site = %0 ",
		rosterEquals: "Roster = %0 ",
		report_filtered: "%0 results displayed, organized by %1, filters: %2",
		report_notFiltered: "%0 results displayed, organized by %1",
		siteAdded: "Site %0 created.",
		siteFound: "Site %0 found.",
		rosterAdded: "Roster %0 created",
		rosterFound: "Roster %0 found.",
		mappingAdded: "Roster added to site.",
		mappingFound: "Site already uses roster.",
		results_removeMappingHeader: "Site - Roster mappings removed:",
		results_removeMappingBody: "%0 - %1",
		results_removeSiteHeader: "Sites cleared of rosters:",
		results_removeRosterHeader: "Rosters removed from all sites:",

		// site roster mode
		label_selectSite: "Site Name:",
		visit: {title:"Visit Site (new window)", html:"Visit"},
		msg_siteId: "Site: %0 (%1) %2, %3",
		msg_rosterId: "Roster: %0 (%1) %2, %3",
		confirmAddTitle: {title: "Add Members"},
		users: "Users:",
		role: "Role:",
		addMemberInstructions: "Add new members to your site.  This is for instructors, TAs and observers - not for students.",
		notAdded: "<i>%1 not used - did not identify a user</i>",
		alreadyMember: "<i>%1 identifies user: %0 - already a site member</i>",
		userAdded: "%1 identifies user: %0 - added to the site",
		adhocRoster: "<i>adhoc</i>",
		masterRoster: "<i>master</i>",
		confirmRemove_body: "The selected members will be removed from the site.  Proceed?",
		confirmRemove_title: {title: "Confirm Remove Members"},
		removed: "Removed: %0",
		notInSite: "<i>%0 is not a site member</i>",
		notRemoved: "<i>%0 may not be removed</i>",

		// reports mode
		header_selectReport: "Select a Report",
		label_selectReport: "Select a Report:",
		label_visitorsSite: "Visitors per Site",
		label_seatsTerm: "Seats per Term",
		label_seatsSite: "Seats per Site",
		
		// organize help alert
		body_organizeHelp: "How you organize the results determines what Remove feature you have.  The Add feature is the same for all organizations.<ul><li>Choose <b>Mapping</b> to remove a Site - Roster mapping.</li><li>Choose <b>Sites</b> to remove all rosters from a site.</li><li>Choose <b>Rosters</b> to remove a roster from all sites.</li></ul>",
		title_organizeHelp: {title: "About Organize"},
		
		// add site roster dialog
		title_confirmAddSiteRoster: {title:"Add Site and Roster"},
		instructions_confirmAddSiteRoster: "Enter the exact site and roster names.  If needed, either will be created.  The roster will be added to the site.",
		label_confirmAddSiteRoster_site: "Site:",
		label_confirmAddSiteRoster_roster: "Roster:",
		
		// add site roster missing params alert
		title_alert_addSiteRoster_params: {title: "Missing Value"},
		body_alert_addSiteRoster_params: 'Fill in the site and roster names before pressing "Add".',

		// remove confirm dialogs
		confirmRemoveMapping_title: {title: "Confirm Remove Rosters From Sites"},
		confirmRemoveMapping_body: "The selected site - roster mappings will be removed.  Proceed?",
		confirmRemoveRosters_title: {title: "Confirm Remove Rosters From Sites"},
		confirmRemoveRosters_body: "The selected rosters will be removed from all sites.  Proceed?",
		confirmRemoveSites_title: {title: "Confirm Remove Rosters From Sites"},
		confirmRemoveSites_body: "All rosters will be removed from selected sites.  Proceed?"
	}
};

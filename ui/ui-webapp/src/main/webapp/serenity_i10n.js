/**********************************************************************************
 * $URL: https://source.etudes.org/svn/serenity/trunk/ui/ui-webapp/src/main/webapp/serenity_i10n.js $
 * $Id: serenity_i10n.js 12267 2015-12-13 23:27:07Z ggolden $
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

serenity_i10n =
{
	// native: "en-us",
	"en-us":
	{
		title: "Etudes",

		header_help: "HELP ?",
		header_menu: "MENU",
		header_sites: "SITES",
		header_logout: "LOGOUT",

		problem: "Unable to contact Etudes at this time.",
		problemTitle: {title:"Problem with Request"},
		notLoggedIn: "You are not logged in.",
		notLoggedInTitle: {title:"Access Denied"},

		label_id: "ID:",
		label_created: "created:",
		label_modified: "modified:",

		header_name: "NAME",
		header_type: "TYPE",
		header_size: "SIZE",
		header_date: "DATE",
		header_select: "PICK",

		a_return: {title: "return", html: "RETURN"},
		a_done: {title: "done", html: "DONE"},
		a_prev: {title: "prev", html: "PREV"},
		a_next: {title: "next", html: "NEXT"},
		a_view: {title: "view", html: "VIEW"},
		a_download: {title: "download", html: "DOWNLOAD"},
		a_rename: {title: "rename", html: "RENAME"},
		a_replace: {title: "replace", html: "REPLACE"},
		a_delete: {title: "delete", html: "DELETE"},
		a_upload: {title: "upload", html: "UPLOAD"},

		action_save: "SAVE",
		action_cancel: "CANCEL",
		action_discard: "DISCARD",
		action_confirmSave: "Yes! Save, then continue.",
		action_confirmDiscard: "No. Discard, then contine.",
		action_confirmCancel: "Go back.",

		msg_pos: "%0 of %1",

		title_confirmExit: {title: "Unsaved Work"},
		msg_confirmExit: "Wait! You are leaving behind unsaved work. Would you like to save before continuing?",

		// for e3_filer
		button_replace: "Replace",
		button_keep: "Keep All",
		button_remove: "Remove",
		title_confirmUpload: {title: "Duplicates Found"},
		msg_confirmUpload: "This upload contains files named the same as files already uploaded.  Do you want to replace existing files, or keep them, renaming the new files?",
		title_noFileSelected: {title: "Nothing Selected"},
		msg_noFileSelected: "First select a file before performing that action.",
		title_noFilesSelected: {title: "Nothing Selected"},
		msg_noFilesSelected: "First select one or more files before performing that action.",
		title_confirmFileRemove: {title: "Remove Files"},
		msg_confirmFileRemove: "Are you sure you want to remove the selected file(s)?",
		title_noDelete: {title: "May Not Delete"},
		msg_noDelete: "The selected file is in use and may not be deleted.",
		title_replaceConflict: {title: "Name Conflict"},
		msg_replaceConflict: "You already have a file with this name.",
		title_renameConflict: {title: "Name Conflict"},
		msg_renameConflict: "Choose a unique name.",
		msg_noResources: "There are no resources.",

		// tool titles
		"Home": "Home",
		"Course Map": "Course Map",
		"Forums": "Forums",
		"Resources": "Resources",
		"Social": "Social",
		"Grades": "Gradebook",
		"Assessments": "Assignments, Tests and Surveys",
		"Calendar": "Schedule",
		"Announcements": "Announcements",
		"Blogs": "Blogs",
		"Chat": "Chat",
		"Syllabus": "Syllabus",
		"Modules": "Modules",
		"Messages": "Private Messages",
		"Online": "Online",
		"Members": "Members",
		"Site Setup": "Site Setup",
		"Site Roster": "Roster",
		"Activity": "Activity",
		"Links": "Links",
		
		"Dashboard": "Dashboard",
		"MySites": "My Sites",
		"Sites": "Sites",
		"Files": "My Resources",
		"Account": "Account",
		"Logout": "LOGOUT",
		"Help": "Help",
		
		// for e3_evaluation
		label_autoRelease: "Auto Release",
		label_forGrade: "Include In Gradebook:",
		label_anonGrading: "Anonymous Grading",
		label_worthPoints: "Worth Points",
		label_useRubric: "Grade With Rubric:",
		label_points: "Points:",
		label_scale: "Rating Scale:",
		label_scorePct: "% of Score:",
		label_title: "Title:",
		label_score: "Score:",

		header_comment: "Comment:",
		header_rubric: "Rubric:",
		header_commentOfficial: "Grader's Comment:",
		header_commentPeer: "Reviewer's Comment:",

		msg_unreleased: "NOT GRADED",
		msg_review: "REVIEW",
		msg_markEvaluated: "Mark as <b>Evaluated</b>",
		msg_release: "<b>Release</b> to Submitter",

		// for e3_SortAction
		msg_sort: "SORT",
		a_asc: {title: "sort is ascending: click to reverse"},
		a_desc: {title: "sort is descending: click to reverse"}
	}
};

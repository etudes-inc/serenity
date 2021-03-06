/**********************************************************************************
 * $URL: https://source.etudes.org/svn/serenity/trunk/monitor/monitor-webapp/src/main/webapp/monitor_i10n.js $
 * $Id: monitor_i10n.js 12026 2015-11-06 03:56:28Z ggolden $
 ***********************************************************************************
 *
 * Copyright (c) 2015 Etudes, Inc.
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

monitor_i10n =
{
	// native: "en-us",
	"en-us":
	{
		header_monitor: "ETUDES PRODUCTION MONITOR",
		
		msg_curl_6: "RESOLVE",
		msg_curl_7: "CONNET",
		msg_curl_28: "TIMEOUT",
		msg_curl_35: "CONNECT",

		label_type: "Type",
		label_title: "Title",
		label_open: "Open:",
		label_due: "Due:",
		label_allow: "Allow Until:",
		label_started: "Started:",
		label_finished: "Finished:",
		label_hide: "Hide Until Open",
		label_publish: "Publish",
		label_access: "Access",
		label_instructions: "Instructions",

		a_edit : {title: "manage"},
		a_return: {title: "return", html:"RETURN"},
		a_begin: {title: "begin", html:"BEGIN"},
		a_continue: {title: "continue later", html:"Continue Later"},
		a_prev: {title: "prev", html:"PREV"},
		a_next: {title: "next", html:"NEXT"},
		a_view: {title: "preview", html: "Preview"},
		a_save: {title: "save", html: "SAVE"},
		a_finish: {title: "finish", html: "Finish"},
		a_add: {title: "add", html: "Add"},
		a_delete: {title: "delete", html: "Delete"},
		a_publish: {title: "publish", html: "Publish"},
		a_unpublish: {title: "unpublish", html: "Unpublish"},

		msg_noneAvailable: "No items are available",
		msg_noneDefined: "No items are defined",
		msg_noneGrading: "No items are available for grading",

		header_title: "TITLE",
		header_open: "OPEN",
		header_due: "DUE",
		header_tries: "TRIES",
		header_started: "STARTED",
		header_finished: "FINISHED",
		header_score: "SCORE",
		header_allow: "ALLOW UNTIL",
		header_name: "NAME",
		header_section: "SECTION",
		header_autoScore: "AUTO",
		header_final: "FINAL",
		header_evaluated: "GRADED",
		header_released: "RELEASED",

		msg_closed: "closed",
		msg_open: "open",
		msg_willOpen: "will open",
		msg_willOpenHidden: "hidden until open",
		msg_unpublished: "not published",
		msg_inProgressSimple: "in progress",
		msg_available: "available",
		msg_missed: "missed", // TODO as of date
		msg_complete: "finished",

		msg_enter_A: "start again",
		msg_enter_R: "continue",
		msg_enter_S: "start",
		msg_enter_T: "test drive",

		msg_inProgress: "<i>in progress</i>",
		msg_points: "%0 points",
		msg_grade: "Grade %0",
		msg_gradeAssessment: "grade assessment",
		msg_best: "BEST SUBMISSION",

		msg_type_A: "ASSIGNMENT",
		msg_type_T: "TEST",
		msg_type_S: "SURVEY",
		msg_type_F: "EVALUATION",
		msg_type_O: "OFFLINE",
		msg_type_E: "ESSAY",

		msg_enter_notices_A: "ASSIGNMENT NOTICES",
		msg_enter_notices_T: "TEST NOTICES",
		msg_enter_notices_S: "SURVEY NOTICES",
		msg_enter_notices_F: "EVALUATION NOTICES",
		msg_enter_notices_O: "OFFLINE NOTICES",
		msg_enter_notices_E: "ESSAY NOTICES",

		mode_view: "VIEW",
		mode_edit: "ASSESSMENTS",
		mode_pool: "POOLS",
		mode_grade: "GRADE",

		action_save: "Save",
		action_discard: "Discard",
		action_delete: "Delete",

		action_add: "Add",
		action_edit: "Edit",
		action_grade: "Grade",

		sort_status: "STATUS",
		sort_title: "TITLE",
		sort_type: "TYPE",
		sort_due: "DUE",
		sort_name: "NAME",
		sort_section: "SECTION",
		sort_finished: "FINISHED",
		sort_final: "FINAL",
		sort_evaluated: "EVALUATED",
		sort_released: "RELEASED",
		sort_open: "OPEN",

		title_nothingSelected: {title: "Nothing Selected"},
		msg_nothingSelected: "First select one or more assessments before performing that action.",
		
		title_singleSelect: {title: "Select One"},
		msg_singleSelect: "First select one assessment before performing that action.",
		
		title_confirmDelete: {title: "Delete Assessment"},
		msg_confirmDelete: "Are you sure you want to delete the selected assessment(s)?",
		
		title_confirmExit: {title: "Save?"},
		msg_confirmExit: "You have unsaved changes."
	}
};

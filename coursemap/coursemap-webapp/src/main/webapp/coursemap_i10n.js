/**********************************************************************************
 * $URL: https://source.etudes.org/svn/serenity/trunk/coursemap/coursemap-webapp/src/main/webapp/coursemap_i10n.js $
 * $Id: coursemap_i10n.js 12504 2016-01-10 00:30:08Z ggolden $
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

coursemap_i10n =
{
	// native: "en-us",
	"en-us":
	{
		header_cm: "COURSE MAP",
		
		// tool item types
		msg_type_0: "?",
		msg_type_1: "FORUM",
		msg_type_2: "BLOG",
		msg_type_3: "ASSIGNMENT",
		msg_type_4: "OFFLINE",
		msg_type_5: "TEST",
		msg_type_6: "ESSAY",
		msg_type_1001: "EXTRA",
		msg_type_2001: "CHAT",
		msg_type_2002: "EVALUATION",
		msg_type_2003: "SURVEY",
		msg_type_2004: "SYLLABUS",
		msg_type_2005: "MODULE",

		msg_noneDefined: "No items are defined.",
		msg_closedMissed: "missed! item now closed",
		msg_closedComplete: "complete, item now closed",
		msg_openComplete: "complete, item still open",
		msg_open: "available, item is open",
		msg_willOpen: "item not yet open",
		msg_willOpenHidden: "item hidden until open",
		msg_reorder: "drag to reorder",
		msg_unpublished: "not published",
		msg_closed: "closed",

		header_title: "TITLE",
		header_open: "OPEN",
		header_due: "DUE",
		header_count: "COUNT",
		header_activity: "ACTIVITY",
		header_score: "SCORE",
		header_allowUntil: "ALLOW UNTIL",
		header_blocker: "PREREQUISITE",

		// view mode
		msg_scoreOf: "SCORE: %0 (%1 <span style='font-size:8px;font-weight:bold'>OF</span> %2 possible points)",
		msg_noScore: "SCORE: -",
		msg_showingAll: "grade includes <span style='color:black; font-weight:700'>the work for the entire course</span>",
		msg_showingReleased: "grade includes <span style='text-decoration:underline; font-weight:700'>only completed and graded work</span>, %0% of the entire course",

		header_gradeToDate: "GRADE TO DATE",
		header_gradeTotal: "TOTAL GRADE",

		a_edit : {title: "manage"},

		// manage mode		
		cm_insertHeader: "Insert Header",
		cm_deleteHeader: "Delete",

		// review mode
		msg_titlePoints: "%0, %1 points",
		msg_closedOn: "closed on %0",
		msg_closed: "closed",
		msg_openUntil: "open until %0",
		msg_open: "open",
		msg_willOpenHiddenOn: "hidden until %0",
		msg_willOpenOn: "will open on %0",
		msg_blocker: "* This is a prerequisite. Complete it to make further progress."
	}
};

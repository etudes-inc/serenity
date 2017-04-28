/**********************************************************************************
 * $URL: https://source.etudes.org/svn/serenity/trunk/evaluation/evaluation-webapp/src/main/webapp/evaluation_i10n.js $
 * $Id: evaluation_i10n.js 12504 2016-01-10 00:30:08Z ggolden $
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

evaluation_i10n =
{
	// native: "en-us",
	"en-us":
	{
		header_grades: "GRADEBOOK",

		mode_overview: "Overview",
		mode_grades: "Grades",
		mode_options: "Options",
		mode_category: "Categories",
		mode_rubric: "Rubrics",
		
		msg_noneDefined: "No items are defined.",
		msg_allSections: "All",
		msg_view: "view",
		msg_closed: "closed",
		msg_closedOn: "closed on %0",
		msg_open: "open",
		msg_openUntil: "open until %0",
		msg_willOpen: "will open",
		msg_willOpenHidden: "hidden until open",
		msg_willOpenOn: "will open on %0",
		msg_willOpenHiddenOn: "hidden until %0",

		a_export: {title: "export", html: "Export"},
		a_add: {title: "add", html: "Add"},
		a_delete: {title: "delete", html: "Delete"},

		action_delete: "DELETE",
		action_edit: "EDIT",

		title_select: {title: "Select Something"},
		msg_select: "First select one or more items before performing that action.",
		title_select1: {title: "Select Something"},
		msg_select1: "First select a single item before performing that action.",

		title_rubricView: "RUBRIC: %0",

		cm_view: "View",
		cm_delete: "Delete",
		cm_edit: "Edit",

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

		msg_typeName_0: "None",
		msg_typeName_1: "Forum",
		msg_typeName_2: "Blog",
		msg_typeName_3: "Assignment",
		msg_typeName_4: "Offline Work",
		msg_typeName_5: "Test",
		msg_typeName_6: "Essay",
		msg_typeName_1001: "Extra Credit",
		msg_typeName_2001: "Chat",
		msg_typeName_2002: "Evaluation",
		msg_typeName_2003: "Survey",
		msg_typeName_2004: "Syllabus",
		msg_typeName_2005: "Module",
		msg_typeName_2006: "User",

		msg_reorder: "drag to reorder",

		msg_blocked: "blocked",
		msg_dropped: "dropped",
		msg_added: "added",
		msg_enrolled: "enrolled",

		// overview
		msg_assessment: "%0 assessment",
		msg_assessments: "%0 assessments",
		msg_point: "%0 total point",
		msg_points: "%0 total points",
		msg_points_ec: "%0 w/ec",
		msg_catWorth: "Worth: %0 (%1 of %2)",
		msg_catWorthWeighted: "Worth: %0% (w/ %1 pts)",

		header_title: "TITLE",
		header_open: "OPEN",
		header_due: "DUE",
		header_students: "STUDENTS",
		header_average: "AVERAGE",
		header_points: "POINTS",

		sort_category: "Category",
		sort_title: "Title",
		sort_open: "Open",
		sort_due: "Due",		
		
		title_rubric: {title: "RUBRIC"},

		// grade
		msg_noStudents: "No students to display.",

		header_section: "SECTION",
		header_name: "NAME",
		header_score: "SCORE",
		header_grade: "GRADE OVERRIDE",

		msg_memberNameHot: "%0<div style='font-size:9px;color:#ADADAD;line-height:normal;position:relative;top:-8px;'>ID: %1</div>",
		msg_memberName: "%0<div style='font-size:9px;color:#ADADAD;'>ID: %1</div>",
		msg_viewMember: "view grades",
		msg_avgScore: "%0 average score",
		msg_showingAll: "grade includes <span style='font-weight:700'>all work</span>",
		msg_showingReleased: "grade includes <span style='font-weight:700'>only released work</span>",
//		msg_showingReleasedPct: "grade includes <span style='font-weight:700'>only released work</span>, %0 of the entire course",
		msg_haveDropped: "<span style='font-weight:700'>%0 low scores</span> have been dropped",
		msg_haveDropped1: "<span style='font-weight:700'>1 low score</span> has been dropped",
		msg_willBeDropped: "<span style='font-weight:700'>%0 low scores</span> will be dropped",
		msg_willBeDropped1: "<span style='font-weight:700'>1 low score</span> will be dropped",
		msg_boost: "grades are <span style='font-weight:700'>boosted by %0 %1</span>",
		msg_noBoost: "grades not not boosted",
		msg_dropFromCatCount1: "has 1 item",
		msg_dropFromCatCount: "has %0 items",
		msg_invalidDrop: "* Drop number is higher than available items in one or more categories",
		msg_boostType_0: "percentage points",
		msg_boostType_1: "points",
		msg_boostType1_0: "percentage point",
		msg_boostType1_1: "point",

		label_boostPercentagePoints: "Percentage Points",
		label_boostPoints: "Points",
		label_dropActivate: "Apply to final grades now",

		sort_status: "Status",
		sort_section: "Section",
		sort_name: "Name",
		sort_score: "Score",
		
		a_boost: {title: "boost all graded", html: "Boost"},
		
		action_save: "SAVE",

		// category
		header_category: "CATEGORY",
		header_type: "TYPE",
		header_weightTotal: "WEIGHT TOTAL",
		header_categoryChoice: "CATEGORIES",

		msg_noCategories: "No categories are defined",
		msg_usingWeights: "Using category weights to calculate final grades",
		msg_usingPoints: "Using point values to calculate final grades.",

		alert_invalidWeights: "* Weights (excluding extra credit) must total 100%",

		a_standard: {title: "switch to standard categories", html: "Use Standard Categories"},
		
		title_fixWeights: {title: "Correct Category Weights"},
		msg_fixWeights: "Category weights (excluding extra credit) must total 100%.  Correct before saving.",
		
		// rubric manage
		header_description: "DESCRIPTION",

		// rubric edit
		header_ratingScale: "RATING SCALE",
		label_criterion: "Criterion",
		label_standards: "Rating Level Standards",
		header_forLevel: "For Scale Level:",
		ph_standard: "criterion rating level standard (optional)",

		// rubric criteria edit
		msg_rubricTitle: "Rubric: %0",

		
		// item grade
		
		msg_itemPoints: "%0 points",
		msg_items: "Items",

		// options
		msg_invalidScaleChange: "* Remove grade overrides before selecting a different grading scale",
		
		
		
		
		
		
		// member grade
		msg_memberId: "ID: %0",
		msg_scoreOf: "SCORE: %0 (%1 <span style='font-size:8px;font-weight:bold'>OF</span> %2 possible points)",
		msg_noScore: "SCORE: -",

		header_gradeToDate: "GRADE TO DATE",
		header_gradeTotal: "TOTAL GRADE",
		header_pctComplete: "PERCENT COMPLETE",
		header_possiblePoints: "POSSIBLE POINTS",

		// review
		msg_finished: "Finished: %0",
		msg_memberIid: "ID: %0",
		msg_titlePoints: "%0, %1 points",
		
		header_items: "Grading Items",

		label_assessments: "Items:",
		label_points: "Points:",
		label_sort: "Sort:",

		msg_byCategory: "Catagory",
		msg_byTitle: "Title",
		msg_byDue: "Due",

		// item grade
		label_item: "Item",
		label_status: "Status:",
		label_open: "Open:",
		label_due: "Due:",
		label_allowUntil: "Allow Until",
		label_finished: "Finished:",
		label_category: "Category:",

		header_submitted: "Finished",
		header_viewSection: "SECTION",
		header_sort: "SORT",

		msg_items: "Items",
		msg_gradesFor: "GRADES",

		// item grade review
		lable_name: "Name:",
		label_section: "Section:",

		// grade
		action_export: "Export",

		label_view: "View:",
		label_students: "Students:",
		label_avgScore: "Avg Score:",

		msg_overallGrades: "Class Grades",
		msg_itemGrades: "Item Grades",
		msg_allTypes: "All Types",
		msg_discussions: "Discussions",
		msg_assignments: "Assignments",
		msg_offlines: "Offlines",
		msg_tests: "Tests",
		msg_studentsInSection: "(section %0)",
		msg_releasedOnly: "Grades include only <b>released submissions</b>.",
		msg_boostxxx: "Grades are boosted by <b>0</b> percentage points.",

		title_confirmExit: {title: "Save?"},
		msg_confirmExit: "You have unsaved changes.",

		// member grade
		label_score: "Score:",
		label_grade: "Grade:",

		// options
		
		// categories
		action_addCategory: {html: "Add", title: "Add Category"},


		// rubrics
		action_addRubric: {html: "Add", title: "Add Rubric"},
		action_save: "Save",
		action_addCriterion:  {html: "Add", title: "Add Criterion"},

		header_evaluationCriteria: "Evaluation Criteria",
		header_scale: "Rating Scale",
		label_scorePct: "% of Score:",

		popup_delete: {title: "Delete Rubric"},
		popup_edit: {title: "Edit Rubtic"},
		popup_deleteCriterion: {title: "Delete Criterion"},
		popup_addLevel: {title: "Add Level"},
		popup_deleteLevel: {title: "Delete Level"},

		ph_score: {placeholder: "0-100"},
		ph_title: {placeholder: "title"},
		ph_description: {placeholder: "description"},

		title_editRubric: {title: "Edit Rubric"},

		confirmRubricDeleteTitle: {title: "Delete Rubric"},
		confirmRubricDelete: "Are you sure you want to delete this rubric?",
		
		a_view: {title: "View", html: "View"}
	}
};

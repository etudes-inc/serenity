/**********************************************************************************
 * $URL: https://source.etudes.org/svn/serenity/trunk/activity/activity-webapp/src/main/webapp/activity.js $
 * $Id: activity.js 12504 2016-01-10 00:30:08Z ggolden $
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

var activity_tool = null;

function Activity()
{
	var me = this;

	this.i18n = new e3_i18n(activity_i10n, "en-us");
	this.ui = null;
	this.portal = null;
	this.onExit = null;

	this.init = function()
	{
		me.i18n.localize();
		me.ui = findElements(["activity_header", "activity_modebar", "activity_headerbar", "activity_headerbar2", "activity_itemnav",
		                      "activity_bar_overview", "activity_header_overview", "activity_overview",
		                      "activity_bar_site", "activity_header_site", "activity_site",
		                      "activity_bar_syllabus", "activity_header_syllabus", "activity_syllabus",
		                      "activity_bar_module", "activity_header_module", "activity_module",
		                      "activity_bar_forum", "activity_header_forum", "activity_forum",
		                      "activity_bar_assessment", "activity_header_assessment", "activity_assessment",
		                      "activity_bar_alert", "activity_header_alert", "activity_alert",
		                      "activity_bar_student", "activity_bar2_student", "activity_header_student", "activity_student",
		                      "activity_bar_moduleItem", "activity_bar2_moduleItem", "activity_header_moduleItem", "activity_moduleItem",
		                      "activity_bar_forumItem", "activity_bar2_forumItem", "activity_header_forumItem", "activity_forumItem",
		                      "activity_bar_assessmentItem", "activity_bar2_assessmentItem", "activity_header_assessmentItem", "activity_assessmentItem",
		                      "activity_name_template"
		                      ]);
		me.portal = portal_tool.features({onExit: me.checkExit, pin:[{ui:me.ui.activity_header}]});
		
		me.ui.modebar = new e3_Modebar(me.ui.activity_modebar);
		me.overviewMode = new Activity_overview(me);
		me.siteMode = new Activity_site(me);
		me.syllabusMode = new Activity_syllabus(me);
		me.moduleMode = new Activity_module(me);
		me.forumMode = new Activity_forum(me);
		me.assessmentMode = new Activity_assessment(me);
		me.alertMode = new Activity_alert(me);
		me.studentMode = new Activity_student(me);
		me.moduleItemMode = new Activity_moduleItem(me);
		me.forumItemMode = new Activity_forumItem(me);
		me.assessmentItemMode = new Activity_assessmentItem(me);

		me.modes =
		[
			{name:me.i18n.lookup("mode_overview", "Overview"), func:function(){me.startOverview();}},
			{name:me.i18n.lookup("mode_site", "Site Visits"), func:function(){me.startSite();}},
			{name:me.i18n.lookup("mode_syllabus", "Syllabus"), func:function(){me.startSyllabus();}},
			{name:me.i18n.lookup("mode_module", "Modules"), func:function(){me.startModule();}},
			{name:me.i18n.lookup("mode_forum", "Discussions"), func:function(){me.startForum();}},
			{name:me.i18n.lookup("mode_assessment", "AT&S"), func:function(){me.startAssessment();}}
		];
		me.ui.modebar.set(me.modes, 0);

		me.overviewMode.init(me);
		me.siteMode.init(me);
		me.syllabusMode.init(me);
		me.moduleMode.init(me);
		me.forumMode.init(me);
		me.assessmentMode.init(me);
		me.alertMode.init(me);
		me.studentMode.init(me);
		me.moduleItemMode.init(me);
		me.forumItemMode.init(me);
		me.assessmentItemMode.init(me);
	};

	this.start = function()
	{
		me.startOverview();
	};
	
	this.startOverview = function()
	{
		if ((me.onExit != null) && (!me.onExit(function(){me.startOverview();}))) return;
		me.mode([me.ui.activity_headerbar, me.ui.activity_bar_overview, me.ui.activity_header_overview, me.ui.activity_overview]);
		me.ui.modebar.showSelected(0);
		me.overviewMode.start();
	};

	this.startSite = function()
	{
		if ((me.onExit != null) && (!me.onExit(function(){me.startSite();}))) return;
		me.mode([me.ui.activity_headerbar, me.ui.activity_bar_site, me.ui.activity_header_site, me.ui.activity_site]);
		me.ui.modebar.showSelected(1);
		me.siteMode.start();
	};

	this.startSyllabus = function()
	{
		if ((me.onExit != null) && (!me.onExit(function(){me.startSyllabus();}))) return;
		me.mode([me.ui.activity_headerbar, me.ui.activity_bar_syllabus, me.ui.activity_header_syllabus, me.ui.activity_syllabus]);
		me.ui.modebar.showSelected(2);
		me.syllabusMode.start();
	};

	this.startModule = function()
	{
		if ((me.onExit != null) && (!me.onExit(function(){me.startModule();}))) return;
		me.mode([me.ui.activity_headerbar, me.ui.activity_bar_module, me.ui.activity_header_module, me.ui.activity_module]);
		me.ui.modebar.showSelected(3);
		me.moduleMode.start();
	};

	this.startForum = function()
	{
		if ((me.onExit != null) && (!me.onExit(function(){me.startForum();}))) return;
		me.mode([me.ui.activity_headerbar, me.ui.activity_bar_forum, me.ui.activity_header_forum, me.ui.activity_forum]);
		me.ui.modebar.showSelected(4);
		me.forumMode.start();
	};

	this.startAssessment = function()
	{
		if ((me.onExit != null) && (!me.onExit(function(){me.startAssessment();}))) return;
		me.mode([me.ui.activity_headerbar, me.ui.activity_bar_assessment, me.ui.activity_header_assessment, me.ui.activity_assessment]);
		me.ui.modebar.showSelected(5);
		me.assessmentMode.start();
	};

	this.startStudent = function(studentId, students)
	{
		if ((me.onExit != null) && (!me.onExit(function(){me.startStudent(studentId, students);}))) return;
		me.mode([me.ui.activity_headerbar, me.ui.activity_itemnav, me.ui.activity_bar_student, me.ui.activity_headerbar2, me.ui.activity_bar2_student, me.ui.activity_header_student, me.ui.activity_student]);
		me.ui.modebar.showSelected(0);
		me.studentMode.start(studentId, students);
	};
	
	this.startAlert = function()
	{
		if ((me.onExit != null) && (!me.onExit(function(){me.startAlert();}))) return;
		me.mode([me.ui.activity_headerbar, me.ui.activity_itemnav, me.ui.activity_bar_alert, me.ui.activity_header_alert, me.ui.activity_alert]);
		me.ui.modebar.showSelected(0);
		me.alertMode.start();
	};

	this.startModuleItem = function(sectionId, modules)
	{
		if ((me.onExit != null) && (!me.onExit(function(){me.startModuleItem(sectionId, modules);}))) return;
		me.mode([me.ui.activity_headerbar, me.ui.activity_itemnav, me.ui.activity_bar_moduleItem, me.ui.activity_headerbar2, me.ui.activity_bar2_moduleItem, me.ui.activity_header_moduleItem, me.ui.activity_moduleItem]);
		me.ui.modebar.showSelected(3);
		me.moduleItemMode.start(sectionId, modules);
	};

	this.startForumItem = function(forumItemId, forums)
	{
		if ((me.onExit != null) && (!me.onExit(function(){me.startForumItem(forumItemId, forums);}))) return;
		me.mode([me.ui.activity_headerbar, me.ui.activity_itemnav, me.ui.activity_bar_forumItem, me.ui.activity_headerbar2, me.ui.activity_bar2_forumItem, me.ui.activity_header_forumItem, me.ui.activity_forumItem]);
		me.ui.modebar.showSelected(4);
		me.forumItemMode.start(forumItemId, forums);
	};

	this.startAssessmentItem = function(assessmentId, assessments)
	{
		if ((me.onExit != null) && (!me.onExit(function(){me.startAssessmentItem(assessmentId, assessments);}))) return;
		me.mode([me.ui.activity_headerbar, me.ui.activity_itemnav, me.ui.activity_bar_assessmentItem, me.ui.activity_headerbar2, me.ui.activity_bar2_assessmentItem, me.ui.activity_header_assessmentItem, me.ui.activity_assessmentItem]);
		me.ui.modebar.showSelected(5);
		me.assessmentItemMode.start(assessmentId, assessments);
	};

	this.mode = function(modeUi)
	{
		hide([me.ui.activity_headerbar, me.ui.activity_headerbar2, me.ui.activity_itemnav,
		      me.ui.activity_bar_overview, me.ui.activity_header_overview, me.ui.activity_overview,
		      me.ui.activity_bar_site, me.ui.activity_header_site, me.ui.activity_site,
		      me.ui.activity_bar_syllabus, me.ui.activity_header_syllabus, me.ui.activity_syllabus,
		      me.ui.activity_bar_module, me.ui.activity_header_module, me.ui.activity_module,
		      me.ui.activity_bar_forum, me.ui.activity_header_forum, me.ui.activity_forum,
		      me.ui.activity_bar_assessment, me.ui.activity_header_assessment, me.ui.activity_assessment,
		      me.ui.activity_bar_student, me.ui.activity_bar2_student, me.ui.activity_header_student, me.ui.activity_student,
		      me.ui.activity_bar_alert, me.ui.activity_header_alert, me.ui.activity_alert,
		      me.ui.activity_bar_moduleItem, me.ui.activity_bar2_moduleItem, me.ui.activity_header_moduleItem, me.ui.activity_moduleItem,
		      me.ui.activity_bar_forumItem, me.ui.activity_bar2_forumItem, me.ui.activity_header_forumItem, me.ui.activity_forumItem,
		      me.ui.activity_bar_assessmentItem, me.ui.activity_bar2_assessmentItem, me.ui.activity_header_assessmentItem, me.ui.activity_assessmentItem]);
		me.onExit = null;
		me.portal.resetScrolling();
		show(modeUi);
	};
	
	this.checkExit = function(deferred)
	{
		if (me.onExit == null) return true;
		return me.onExit(deferred);
	};

	this.memberStatusRanking = function(member)
	{
		if (member.blocked) return 3;
		if (!member.active) return 4;
		if (member.adhoc) return 2;
		return 1;
	};

	this.compareMemberStatusRanking = function(a, b, nullBig)
	{
		return compareN(me.memberStatusRanking(a), me.memberStatusRanking(b), nullBig);
	};

	this.memberStatus = function(member)
	{
		if (member.blocked)
		{
			if ((member.official) && (!member.master))
			{
				if (member.active)
				{
					return me.i18n.lookup("blockedEnrolled", "Blocked (E)");
				}
				else
				{
					return me.i18n.lookup("blockedDropped", "Blocked (D)");
				}
			}
			else
			{
				return me.i18n.lookup("Blocked", "Blocked");
			}
		}
		else if ((member.official) && (!member.master))
		{
			if (member.active)
			{
				return me.i18n.lookup("Enrolled", "Enrolled");
			}
			else
			{
				return me.i18n.lookup("Dropped", "Dropped");
			}
		}
		else if (member.adhoc)
		{			
			return me.i18n.lookup("Added", "Added");
		}
		else
		{
			return me.i18n.lookup("Active", "Active");
		}
	};

	this.publicationStatusDotTd = function(item, table)
	{
		if (!item.valid)
		{
			table.dot(Dots.alert, me.i18n.lookup("msg_invalid", "invalid"));
		}
		else if (!item.published)
		{
			table.dot(Dots.red, me.i18n.lookup("msg_unpublished", "not published"));
		}
		else if (item.schedule.status == ScheduleStatus.willOpenHide)
		{
			table.dot(Dots.gray, me.i18n.lookup("msg_willOpenHidden", "hidden until open"));
		}
		else
		{
			table.dot(Dots.green, me.i18n.lookup("msg_published", "published"));
		}
	};
}

function Activity_overview(main)
{
	var me = this;

	this.ui = null;
	this.summary = null;
	this.items = null;
	this.itemsSorted = null;
	this.sortDirection = "A";
	this.sortMode = "X";
	this.sortExtra = null;

	this.init = function()
	{
		me.ui = findElements(["activity_overview_actions", "activity_overview_enrolled", "activity_overview_added", "activity_overview_dropped", "activity_overview_blocked", "activity_overview_alert", "activity_overview_export",
		                      "activity_overview_table", "activity_overview_none"]);
		me.ui.table = new e3_Table(me.ui.activity_overview_table);

		me.ui.sort = new e3_SortAction();
		me.ui.sort.inject(me.ui.activity_overview_actions,
				{onSort: me.onSort, initial: me.sortMode,
					options:[{value:"N", title: main.i18n.lookup("sort_name", "Name"), extra: me.sortByName},
					         {value:"R", title: main.i18n.lookup("sort_section", "Section"), extra: me.sortBySection},
					         {value:"X", title: main.i18n.lookup("sort_status", "Status"), extra: me.sortByStatus},
					         {value:"F", title: main.i18n.lookup("sort_firstVisit", "First Visit"), extra: me.sortByFirstVisit},
					         {value:"L", title: main.i18n.lookup("sort_lastVisit", "Last Visit"), extra: me.sortByLastVisit},
					         {value:"V", title: main.i18n.lookup("sort_visits", "Visits"), extra: me.sortByVisit},
					         {value:"S", title: main.i18n.lookup("sort_syllabus", "Syllabus"), extra: me.sortBySyllabus},
					         {value:"M", title: main.i18n.lookup("sort_module", "Modules"), extra: me.sortByModule},
					         {value:"P", title: main.i18n.lookup("sort_forum", "Posts"), extra: me.sortByPost},
					         {value:"A", title: main.i18n.lookup("sort_assessment", "Assessments"), extra: me.sortByAssessment}]
				});
		me.ui.sort.directional(true);
		me.sortExtra = me.sortByStatus;

		onClick(me.ui.activity_overview_export, me.exportCsv);
		onClick(me.ui.activity_overview_alert, main.startAlert);
		setupHoverControls([me.ui.activity_overview_export]);
	};

	this.start = function()
	{
		main.onExit = me.checkExit;
		me.load();
	};

	this.load = function()
	{
		var params = main.portal.cdp.params();
		params.url.site = main.portal.site.id;
		main.portal.cdp.request("activity_overview", params, function(data)
		{
			me.summary = data.summary || {enrolled: 12, added: 2, dropped: 3, blocked: 1, alert: 4};
			me.items = /*data.items ||*/ [
			                              {id:1, nameDisplay: "Joe Smith", nameSort:"Smith, Joe", iid:"12345678", rosterName: "1010101", active: true, blocked: false, official: true, adhoc: false, master: false, firstVisit: 0, lastVisit: 0, visits: 3, syllabus: 0, modules: 2, forums: 3, assessments: 4},
			                              {id:10, nameDisplay: "Celana Sardothian", nameSort:"Sardothian, Celana", iid:"987654321", rosterName: "1010101", active: false, blocked: false, official: true, adhoc: false, master: false, firstVisit: 0, lastVisit: 0, visits: 2, modules: 1, forums: 7, assessments: 2}
			                             ];

			me.itemsSorted = me.sortItems(me.items, me.sortDirection, me.sortExtra);
			me.populate();
		});
	};

	this.onSort = function(direction, option, extra)
	{
		me.sortDirection = direction;
		me.sortMode = option;
		me.sortExtra = extra;

		me.itemsSorted = me.sortItems(me.items, me.sortDirection, me.sortExtra);
		me.populate();
	};

	this.sortItems = function(items, direction, extra)
	{
		var sorted = [];

		if (extra != null)
		{
			sorted = extra(me.items, direction);
		}

		return sorted;
	};

	this.checkExit = function(deferred)
	{
		return true;
	};
	
	this.populate = function()
	{
		// populate header area from summary
		me.ui.activity_overview_enrolled.text(me.summary.enrolled);
		me.ui.activity_overview_added.text(me.summary.added);
		me.ui.activity_overview_dropped.text(me.summary.dropped);
		me.ui.activity_overview_blocked.text(me.summary.blocked);
		if (me.summary.alert > 0)
		{
			me.ui.activity_overview_alert.text(main.i18n.lookup("msg_alert", "%0 students have not visited the site in the last 7 days.", "html", [me.summary.alert]));
		}

		// populate items
		me.ui.table.clear();
		$.each(me.itemsSorted, function(index, item)
		{
			var tr = me.ui.table.row();
			
			var cell = clone(main.ui.activity_name_template, ["activity_name_template_body", "activity_name_template_name", "activity_name_template_iid"]);
			me.ui.table.hotElement(cell.activity_name_template_body, "", function(){main.startStudent(item.id, me.itemsSorted);}, null, {width:"calc(100vw - 100px - 796px)", minWidth:"calc(1200px - 100px - 796px"});

			cell.activity_name_template_name.text(item.nameSort);
			cell.activity_name_template_iid.text(item.iid);
			
			me.ui.table.text(item.rosterName, null, {width: 60});
			me.ui.table.text(main.memberStatus(item), "e3_text special light", {fontSize: 11, width: 60, textTransform: "uppercase"});

			me.ui.table.date(item.firstVisit, "-", "date2l");
			me.ui.table.date(item.lastVisit, "-", "date2l");
			me.ui.table.text(item.visits, null, {width: 60});
			me.ui.table.date(item.syllabus, "-", "date2l");
			me.ui.table.text(item.modules, null, {width: 80});
			me.ui.table.text(item.forums, null, {width: 80});
			me.ui.table.text(item.assessments, null, {width: 80});
		});
		me.ui.table.done();

		show(me.ui.activity_overview_none, (me.ui.table.rowCount() == 0));	
	};

	this.sortByName = function(items, direction)
	{
		return me.ui.sort.sort(items, direction, compareS, "nameSort");
	};
	
	this.sortBySection = function(items, direction)
	{
		return me.ui.sort.sort(items, direction, compareS, "rosterName", compareS, "nameSort");
	};

	this.sortByStatus = function(items, direction)
	{
		return me.ui.sort.sort(items, direction, main.compareMemberStatusRanking, null, compareS, "nameSort");
	};
	
	this.sortByFirstVisit = function(items, direction)
	{
		return me.ui.sort.sort(items, direction, compareN, "firstVisit", compareS, "nameSort");
	};

	this.sortByLastVisit = function(items, direction)
	{
		return me.ui.sort.sort(items, direction, compareN, "lastVisit", compareS, "nameSort");
	};

	this.sortByVisit = function(items, direction)
	{
		return me.ui.sort.sort(items, direction, compareN, "visits", compareS, "nameSort");
	};

	this.sortBySyllabus = function(items, direction)
	{
		return me.ui.sort.sort(items, direction, compareN, "syllabus", compareS, "nameSort");
	};

	this.sortByModule = function(items, direction)
	{
		return me.ui.sort.sort(items, direction, compareN, "modules", compareS, "nameSort");
	};

	this.sortByPost = function(items, direction)
	{
		return me.ui.sort.sort(items, direction, compareN, "forums", compareS, "nameSort");
	};

	this.sortByAssessment = function(items, direction)
	{
		return me.ui.sort.sort(items, direction, compareN, "assessments", compareS, "nameSort");
	};
	
	this.exportCsv = function()
	{
		console.log("export csv from overview");
	};
}

function Activity_site(main)
{
	var me = this;

	this.ui = null;
	this.summary = null;
	this.items = null;
	this.itemsSorted = null;
	this.sortDirection = "A";
	this.sortMode = "X";

	this.init = function()
	{
		me.ui = findElements(["activity_site_actions", "activity_site_export", "activity_site_visited", "activity_site_absent", "activity_site_recent",
		                      "activity_site_table", "activity_site_none"]);
		me.ui.table = new e3_Table(me.ui.activity_site_table);

		me.ui.sort = new e3_SortAction();
		me.ui.sort.inject(me.ui.activity_site_actions,
				{onSort: me.onSort, initial: "X",
					options:[{value:"N", title: main.i18n.lookup("sort_name", "Name"), extra: me.sortByName},
					         {value:"R", title: main.i18n.lookup("sort_section", "Section"), extra: me.sortBySection},
					         {value:"X", title: main.i18n.lookup("sort_status", "Status"), extra: me.sortByStatus},
					         {value:"F", title: main.i18n.lookup("sort_firstVisit", "First Visit"), extra: me.sortByFirstVisit},
					         {value:"L", title: main.i18n.lookup("sort_lastVisit", "Last Visit"), extra: me.sortByLastVisit},
					         {value:"V", title: main.i18n.lookup("sort_visits", "Visits"), extra: me.sortByVisit}]
				});
		me.ui.sort.directional(true);
		me.sortExtra = me.sortByStatus;

		onClick(me.ui.activity_site_export, me.exportCsv);
		setupHoverControls([me.ui.activity_site_export]);
	};

	this.start = function()
	{
		main.onExit = me.checkExit;
		me.load();
	};

	this.load = function()
	{
		var params = main.portal.cdp.params();
		params.url.site = main.portal.site.id;
		main.portal.cdp.request("activity_site", params, function(data)
		{
			me.summary = data.summary || {visited: 12, absent: 2, recent: 4};
			me.items = /*data.items ||*/ [
			                              {id:1, nameSort:"Smith, Joe", iid:"12345678", rosterName: "1010101", active: true, blocked: false, official: true, adhoc: false, master: false, firstVisit: 0, lastVisit: 0, visits: 3},
			                              {id:1, nameSort:"Westfall, Choal", iid:"24315534", rosterName: "1010102", active: true, blocked: false, official: true, adhoc: false, master: false, visits: 0},
			                              {id:1, nameSort:"Sardothian, Celana", iid:"987654321", rosterName: "1010101", active: true, blocked: false, official: true, adhoc: false, master: false, firstVisit: 0, lastVisit: 0, visits: 2}
			                             ];

			me.itemsSorted = me.sortItems(me.items, me.sortDirection, me.sortExtra);
			me.populate();
		});
	};

	this.onSort = function(direction, option, extra)
	{
		me.sortDirection = direction;
		me.sortMode = option;
		me.sortExtra = extra;

		me.itemsSorted = me.sortItems(me.items, me.sortDirection, me.sortExtra);
		me.populate();
	};

	this.sortItems = function(items, direction, extra)
	{
		var sorted = [];

		if (extra != null)
		{
			sorted = extra(me.items, direction);
		}

		return sorted;
	};

	this.checkExit = function(deferred)
	{
		return true;
	};
	
	this.populate = function()
	{
		// populate header area from summary
		me.ui.activity_site_visited.text(me.summary.visited);
		me.ui.activity_site_absent.text(me.summary.absent);
		me.ui.activity_site_recent.text(me.summary.recent);

		// populate items
		me.ui.table.clear();
		$.each(me.itemsSorted, function(index, item)
		{
			var tr = me.ui.table.row();
			
			var cell = clone(main.ui.activity_name_template, ["activity_name_template_body", "activity_name_template_name", "activity_name_template_iid"]);
			me.ui.table.element(cell.activity_name_template_body, null, {width:"calc(100vw - 100px - 444px)", minWidth:"calc(1200px - 100px - 444px"});

			cell.activity_name_template_name.text(item.nameSort);
			cell.activity_name_template_iid.text(item.iid);
			
			me.ui.table.text(item.rosterName, null, {width: 60});
			me.ui.table.text(main.memberStatus(item), "e3_text special light", {fontSize: 11, width: 60, textTransform: "uppercase"});

			me.ui.table.date(item.firstVisit, "-", "date2l");
			me.ui.table.date(item.lastVisit, "-", "date2l");
			me.ui.table.text(item.visits, null, {width: 60});
		});
		me.ui.table.done();

		show(me.ui.activity_site_none, (me.ui.table.rowCount() == 0));	
	};

	this.sortByName = function(items, direction)
	{
		return me.ui.sort.sort(items, direction, compareS, "nameSort");
	};
	
	this.sortBySection = function(items, direction)
	{
		return me.ui.sort.sort(items, direction, compareS, "rosterName", compareS, "nameSort");
	};

	this.sortByStatus = function(items, direction)
	{
		return me.ui.sort.sort(items, direction, main.compareMemberStatusRanking, null, compareS, "nameSort");
	};
	
	this.sortByFirstVisit = function(items, direction)
	{
		return me.ui.sort.sort(items, direction, compareN, "firstVisit", compareS, "nameSort");
	};

	this.sortByLastVisit = function(items, direction)
	{
		return me.ui.sort.sort(items, direction, compareN, "lastVisit", compareS, "nameSort");
	};

	this.sortByVisit = function(items, direction)
	{
		return me.ui.sort.sort(items, direction, compareN, "visits", compareS, "nameSort");
	};
	
	this.exportCsv = function()
	{
		console.log("export csv from site");
	};
}

function Activity_syllabus(main)
{
	var me = this;

	this.ui = null;
	this.items = null;
	this.itemsSorted = null;
	this.sortDirection = "A";
	this.sortMode = "X";

	this.init = function()
	{
		me.ui = findElements(["activity_syllabus_actions", "activity_syllabus_accepted", "activity_syllabus_viewed", "activity_syllabus_pmNotAccepted",
		                      "activity_syllabus_table", "activity_syllabus_none"]);
		me.ui.table = new e3_Table(me.ui.activity_syllabus_table);

		me.ui.sort = new e3_SortAction();
		me.ui.sort.inject(me.ui.activity_syllabus_actions,
				{onSort: me.onSort, initial: "X",
					options:[{value:"N", title: main.i18n.lookup("sort_name", "Name"), extra: me.sortByName},
					         {value:"R", title: main.i18n.lookup("sort_section", "Section"), extra: me.sortBySection},
					         {value:"X", title: main.i18n.lookup("sort_status", "Status"), extra: me.sortByStatus},
					         {value:"S", title: main.i18n.lookup("sort_syllabus", "Syllabus"), extra: me.sortBySyllabus},
					         {value:"F", title: main.i18n.lookup("sort_firstVisit", "First Visit"), extra: me.sortByFirstVisit},
					         {value:"L", title: main.i18n.lookup("sort_lastVisit", "Last Visit"), extra: me.sortByLastVisit}]
				});
		me.ui.sort.directional(true);
		me.sortExtra = me.sortByStatus;

		// onClick(me.ui.activity_syllabus_pmNotAccepted, me.?);
	};

	this.start = function()
	{
		main.onExit = me.checkExit;
		me.load();
	};

	this.load = function()
	{
		var params = main.portal.cdp.params();
		params.url.site = main.portal.site.id;
		main.portal.cdp.request("activity_syllabus", params, function(data)
		{
			me.items = /*data.items ||*/ [
			                              {id:1, nameSort:"Smith, Joe", iid:"12345678", rosterName: "1010101", active: true, blocked: false, official: true, adhoc: false, master: false, firstVisit: 0, lastVisit: 0, syllabus: 0},
			                              {id:1, nameSort:"Sardothian, Celana", iid:"987654321", rosterName: "1010101", active: true, blocked: false, official: true, adhoc: false, master: false}
			                             ];

			me.itemsSorted = me.sortItems(me.items, me.sortDirection, me.sortExtra);
			me.populate();
		});
	};

	this.onSort = function(direction, option, extra)
	{
		me.sortDirection = direction;
		me.sortMode = option;
		me.sortExtra = extra;

		me.itemsSorted = me.sortItems(me.items, me.sortDirection, me.sortExtra);
		me.populate();
	};

	this.sortItems = function(items, direction, extra)
	{
		var sorted = [];

		if (extra != null)
		{
			sorted = extra(me.items, direction);
		}

		return sorted;
	};

	this.checkExit = function(deferred)
	{
		return true;
	};
	
	this.populate = function()
	{
		var numAccepted = 0;
		var numViewed = 0;
		var total = 0;

		// populate items
		me.ui.table.clear();
		$.each(me.itemsSorted, function(index, item)
		{
			var tr = me.ui.table.row();
			
			var cell = clone(main.ui.activity_name_template, ["activity_name_template_body", "activity_name_template_name", "activity_name_template_iid"]);
			me.ui.table.element(cell.activity_name_template_body, null, {width:"calc(100vw - 100px - 464px)", minWidth:"calc(1200px - 100px - 464px"});

			cell.activity_name_template_name.text(item.nameSort);
			cell.activity_name_template_iid.text(item.iid);
			
			me.ui.table.text(item.rosterName, null, {width: 60});
			me.ui.table.text(main.memberStatus(item), "e3_text special light", {fontSize: 11, width: 60, textTransform: "uppercase"});

			me.ui.table.date(item.syllabus, "-", "date2l");
			me.ui.table.date(item.firstVisit, "-", "date2l");
			me.ui.table.date(item.lastVisit, "-", "date2l");
			
			// count only active students
			if ((!item.blocked) && item.active && (!item.master))
			{
				if (item.syllabus != null) numAccepted ++;
				if (item.firstVisit != null) numViewed ++;
				total++;
			}
		});
		me.ui.table.done();

		show(me.ui.activity_syllabus_none, (me.ui.table.rowCount() == 0));
		
		me.ui.activity_syllabus_accepted.text(asPct(numAccepted, total));
		me.ui.activity_syllabus_viewed.text(asPct(numViewed, total));
		show(me.ui.activity_syllabus_pmNotAccepted, ((numAccepted != total) && (total > 0)));
	};

	this.sortByName = function(items, direction)
	{
		return me.ui.sort.sort(items, direction, compareS, "nameSort");
	};
	
	this.sortBySection = function(items, direction)
	{
		return me.ui.sort.sort(items, direction, compareS, "rosterName", compareS, "nameSort");
	};

	this.sortByStatus = function(items, direction)
	{
		return me.ui.sort.sort(items, direction, main.compareMemberStatusRanking, null, compareS, "nameSort");
	};

	this.sortBySyllabus = function(items, direction)
	{
		return me.ui.sort.sort(items, direction, compareN, "syllabus", compareS, "nameSort");
	};

	this.sortByFirstVisit = function(items, direction)
	{
		return me.ui.sort.sort(items, direction, compareN, "firstVisit", compareS, "nameSort");
	};

	this.sortByLastVisit = function(items, direction)
	{
		return me.ui.sort.sort(items, direction, compareN, "lastVisit", compareS, "nameSort");
	};
}

function Activity_module(main)
{
	var me = this;

	this.ui = null;
	this.items = null;

	this.init = function()
	{
		me.ui = findElements(["activity_module_viewed", "activity_module_table", "activity_module_none"]);
		me.ui.table = new e3_Table(me.ui.activity_module_table);
	};

	this.start = function()
	{
		main.onExit = me.checkExit;
		me.load();
	};

	this.load = function()
	{
		var params = main.portal.cdp.params();
		params.url.site = main.portal.site.id;
		main.portal.cdp.request("activity_syllabus", params, function(data)
		{
			me.items = /*data.items ||*/ [
			                              {id:1, title:"Module One", valid:true, published: true, schedule: {status: ScheduleStatus.open},
			                            	  sections: [{id:1, title: "Section One", viewers: 2},{id:2, title: "Section Two", viewers: 0}]},
			                              {id:2, title:"Module Two", valid:true, published: true, schedule: {status: ScheduleStatus.open},
			                            	  sections: [{id:3, title: "Section One", viewers: 2},{id:4, title: "Section Two", viewers: 0}]},
			                              {id:3, title:"Module Three", valid:true, published: true, schedule: {status: ScheduleStatus.open},
			                            	  sections: [{id:5, title: "First Section", viewers: 2},{id:6, title: "Last Section", viewers: 0}]}
			                             ];

			me.populate();
		});
	};

	this.checkExit = function(deferred)
	{
		return true;
	};
	
	this.populate = function()
	{
		var numViewed = 0;
		var total = 0;

		// populate items
		me.ui.table.clear();
		$.each(me.items, function(index, item)
		{
			me.ui.table.row();
			
			// dot for module
			main.publicationStatusDotTd(item, me.ui.table);
			
			me.ui.table.text(item.title, null, {width:"calc(100vw - 100px - 412px)", minWidth:"calc(1200px - 100px - 412px"});
			me.ui.table.text("", null, {width:300});

			// for each section
			$.each(item.sections, function(i, section)
			{
				me.ui.table.row();

				me.ui.table.dot(Dots.none);
				me.ui.table.hotText(section.title, "", function(){main.startModuleItem(section.id, me.items)}, null, {width:"calc(100vw - 100px - 412px)", minWidth:"calc(1200px - 100px - 412px"});
				me.ui.table.text(section.viewers, null, {width:75});
				
				if (section.viewers > 0) numViewed++;
				total++;
			});
		});
		me.ui.table.done();

		show(me.ui.activity_module_none, (me.ui.table.rowCount() == 0));
		
		me.ui.activity_module_viewed.text(asPct(numViewed, total));
	};
}

function Activity_forum(main)
{
	var me = this;

	this.ui = null;
	this.summary = null;
	this.items = null;

	this.init = function()
	{
		me.ui = findElements(["activity_forum_posts", "activity_forum_posters", "activity_forum_absent", "activity_forum_table", "activity_forum_none"]);
		me.ui.table = new e3_Table(me.ui.activity_forum_table);
	};

	this.start = function()
	{
		main.onExit = me.checkExit;
		me.load();
	};

	this.load = function()
	{
		var params = main.portal.cdp.params();
		params.url.site = main.portal.site.id;
		main.portal.cdp.request("activity_forum", params, function(data)
		{
			me.items = /*data.items ||*/ [
			                              {id:1, title:"Introductions", valid:true, published: true, schedule: {status: ScheduleStatus.open}, students: 12, posts: 24},
			                              {id:10, title:"Assignment One", valid:true, published: true, schedule: {status: ScheduleStatus.open}, students: 15, posts: 34},
			                              {id:100, title:"Project", valid:true, published: true, schedule: {status: ScheduleStatus.open}, students: 12, posts: 24},
			                              {id:1000, title:"Subject Thoughts", valid:true, published: true, schedule: {status: ScheduleStatus.open}, students: 12, posts: 24},
			                              {id:10000, title:"Final Project", valid:true, published: true, schedule: {status: ScheduleStatus.open}, students: 12, posts: 24}
			                             ];
			me.summary = /* data.summary */ {posts: 130, posted:17, notPosted: 1, students: 18};

			me.populate();
		});
	};

	this.checkExit = function(deferred)
	{
		return true;
	};
	
	this.populate = function()
	{
		me.ui.activity_forum_posts.text(me.summary.posts);
		me.ui.activity_forum_posters.text(asPct(me.summary.posted, me.summary.students));
		me.ui.activity_forum_absent.text(asPct(me.summary.notPosted, me.summary.students));

		// populate items
		me.ui.table.clear();
		$.each(me.items, function(index, item)
		{
			me.ui.table.row();
			
			main.publicationStatusDotTd(item, me.ui.table);			
			me.ui.table.hotText(item.title, "", function(){main.startForumItem(item.id, me.items);}, null, {width:"calc(100vw - 100px - 270px)", minWidth:"calc(1200px - 100px - 270px"});
			me.ui.table.text(item.students, null, {width:75});
			me.ui.table.text(item.posts, null, {width:75});
		});
		me.ui.table.done();

		show(me.ui.activity_forum_none, (me.ui.table.rowCount() == 0));
	};
}

function Activity_assessment(main)
{
	var me = this;

	this.ui = null;
	this.items = null;

	this.init = function()
	{
		me.ui = findElements(["activity_assessment_assignments", "activity_assessment_tests", "activity_assessment_surveys", "activity_assessment_offlines",
		                      "activity_assessment_table", "activity_assessment_none"]);
		me.ui.table = new e3_Table(me.ui.activity_assessment_table);
	};

	this.start = function()
	{
		main.onExit = me.checkExit;
		me.load();
	};

	this.load = function()
	{
		var params = main.portal.cdp.params();
		params.url.site = main.portal.site.id;
		main.portal.cdp.request("activity_forum", params, function(data)
		{
			me.items = /*data.items ||*/ [
			                              {id:1, title:"Introductions", type: "A", valid:true, published: true, schedule: {status: ScheduleStatus.open}, students: 12, submissions: 24, inProgress: 0},
			                              {id:10, title:"Assignment One", type: "A", valid:true, published: false, schedule: {status: ScheduleStatus.open}, students: 15, submissions: 15, inProgress: 1},
			                              {id:100, title:"Project", type: "T", valid:false, published: true, schedule: {status: ScheduleStatus.open}, students: 12, submissions: 12, inProgress: 4},
			                              {id:1000, title:"Subject Thoughts", type: "T", valid:true, published: true, schedule: {status: ScheduleStatus.open}, students: 12, submissions: 14, inProgress: 0},
			                              {id:10000, title:"Final Project", type: "O", valid:true, published: true, schedule: {status: ScheduleStatus.open}, students: 12, submissions: 12, inProgress: 0}
			                             ];

			me.populate();
		});
	};

	this.checkExit = function(deferred)
	{
		return true;
	};
	
	this.populate = function()
	{
		var assessments = {num: 0, inProgress: 0};
		var tests = {num: 0, inProgress: 0};
		var surveys = {num: 0, inProgress: 0};
		var offlines = {num: 0, inProgress: 0};

		// populate items
		me.ui.table.clear();
		$.each(me.items, function(index, item)
		{
			me.ui.table.row();
			
			main.publicationStatusDotTd(item, me.ui.table);			
			me.ui.table.hotText(item.title, "", function(){main.startAssessmentItem(item.id, me.items);}, null, {width:"calc(100vw - 100px - 556px)", minWidth:"calc(1200px - 100px - 556px"});
			me.typeTd(item, me.ui.table);
			me.ui.table.text(item.students, null, {width:100});
			me.ui.table.text(item.submissions, null, {width:100});
			me.ui.table.text(item.inProgress, null, {width:100});
			
			if (item.type == "A")
			{
				assessments.num += item.submissions;
				assessments.inProgress += item.inProgress;
			}
			else if (item.type == "T")
			{
				tests.num += item.submissions;
				tests.inProgress += item.inProgress;
			}
			else if (item.type == "S")
			{
				surveys.num += item.submissions;
				surveys.inProgress += item.inProgress;
			}
			else if (item.type == "O")
			{
				offlines.num += item.submissions;
				offlines.inProgress += item.inProgress;
			}
		});
		me.ui.table.done();

		show(me.ui.activity_overview_none, (me.ui.table.rowCount() == 0));

		me.ui.activity_assessment_assignments.text(((assessments.inProgress) > 0 ? (main.i18n.lookup("msg_submissionsInProgress", "%0 +%1", "html", [assessments.num, assessments.inProgress])) : assessments.num));
		me.ui.activity_assessment_tests.text(((tests.inProgress) > 0 ? (main.i18n.lookup("msg_submissionsInProgress", "%0 +%1", "html", [tests.num, tests.inProgress])) : tests.num));
		me.ui.activity_assessment_surveys.text(((surveys.inProgress) > 0 ? (main.i18n.lookup("msg_submissionsInProgress", "%0 +%1", "html", [surveys.num, surveys.inProgress])) : surveys.num));
		me.ui.activity_assessment_offlines.text(((offlines.inProgress) > 0 ? (main.i18n.lookup("msg_submissionsInProgress", "%0 +%1", "html", [offlines.num, offlines.inProgress])) : offlines.num));
	};
	
	this.typeTd = function(assessment, table)
	{
		table.text(main.i18n.lookup("msg_type_" + assessment.type), "e3_text special light", {fontSize: 11, width: 120, textTransform: "uppercase"});
	};
}

function Activity_student(main)
{
	var me = this;

	this.ui = null;
	this.items = null;
	this.student = null;
	this.students = null;

	this.init = function()
	{
		me.ui = findElements(["activity_student_actions", "activity_student_name", "activity_student_lastVisit",
		                      "activity_student_export", "activity_student_pm",
		                      "activity_student_modules", "activity_student_posts", "activity_student_assessments", "activity_student_syllabus", "activity_student_missed",
		                      "activity_student_table", "activity_student_none"]);
		me.ui.table = new e3_Table(me.ui.activity_student_table);
		me.ui.itemNav = new e3_ItemNav();


		 onClick(me.ui.activity_student_export, me.xxx);
		 onClick(me.ui.activity_student_pm, me.xxx);
		 
		 setupHoverControls([me.ui.activity_student_export, me.ui.activity_student_pm]);
	};

	this.start = function(studentId, students)
	{
		main.onExit = me.checkExit;
		if (students !== undefined) me.students = students;
		me.student = findIdInList(studentId, me.students);
		me.load();
	};

	this.load = function()
	{
		var params = main.portal.cdp.params();
		params.url.site = main.portal.site.id;
		params.url.student = me.student.id;
		main.portal.cdp.request("activity_student", params, function(data)
		{
			me.summary = /* data.summary */ {lastVisit: 0, modules: 85, posts: 35, assessments: 6, syllabus: true, missed: 1};
			me.items = /*data.items ||*/ [];

			me.ui.itemNav.inject(main.ui.activity_itemnav, {returnFunction: main.startOverview, pos: position(me.student, me.students), navigateFunction: me.goStudent});

			me.populate();
		});
	};

	this.checkExit = function(deferred)
	{
		return true;
	};
	
	this.populate = function()
	{
		me.ui.activity_student_name.text(me.student.nameDisplay);
		if (me.student.lastVisit != null)
		{
			me.ui.activity_student_lastVisit.text(main.i18n.lookup("msg_lastVisit", "last visit: %0", "html", [main.portal.timestamp.display(me.student.lastVisit)]));
		}
		else
		{
			me.ui.activity_student_lastVisit.text(main.i18n.lookup("msg_neverVisited", "never visited"));
		}
		me.ui.activity_student_modules.text(main.i18n.lookup("msg_numPct", "%0%", "html", [me.summary.modules]));
		me.ui.activity_student_posts.text(me.summary.posts);
		me.ui.activity_student_assessments.text(me.summary.assessments);
		
		if (me.summary.syllabus)
		{
			me.ui.activity_student_syllabus.html(dot(Dots.complete));
		}
		else
		{
			me.ui.activity_student_syllabus.text("-");
		}
		
		me.ui.activity_student_missed.text(me.summary.missed);

		// TODO: CM student like view
	};
	
	this.goStudent = function(item)
	{
		main.startStudent(item.id);
	};
}

function Activity_alert(main)
{
	var me = this;

	this.ui = null;
	this.items = null;
	this.itemsSorted = null;
	this.sortDirection = "A";
	this.sortMode = "X";
	this.sortExtra = null;

	this.init = function()
	{
		me.ui = findElements(["activity_alert_actions", "activity_alert_heading", "activity_alert_pmAll", "activity_alert_pmNever",
		                      "activity_alert_table", "activity_alert_none"]);
		me.ui.table = new e3_Table(me.ui.activity_alert_table);
		me.ui.itemNav = new e3_ItemNav();

		me.ui.sort = new e3_SortAction();
		me.ui.sort.inject(me.ui.activity_alert_actions,
				{onSort: me.onSort, initial: me.sortMode,
					options:[{value:"N", title: main.i18n.lookup("sort_name", "Name"), extra: me.sortByName},
					         {value:"R", title: main.i18n.lookup("sort_section", "Section"), extra: me.sortBySection},
					         {value:"X", title: main.i18n.lookup("sort_status", "Status"), extra: me.sortByStatus},
					         {value:"F", title: main.i18n.lookup("sort_firstVisit", "First Visit"), extra: me.sortByFirstVisit},
					         {value:"L", title: main.i18n.lookup("sort_lastVisit", "Last Visit"), extra: me.sortByLastVisit},
					         {value:"V", title: main.i18n.lookup("sort_visits", "Visits"), extra: me.sortByVisit},
					         {value:"S", title: main.i18n.lookup("sort_syllabus", "Syllabus"), extra: me.sortBySyllabus},
					         {value:"M", title: main.i18n.lookup("sort_module", "Modules"), extra: me.sortByModule},
					         {value:"P", title: main.i18n.lookup("sort_forum", "Posts"), extra: me.sortByPost},
					         {value:"A", title: main.i18n.lookup("sort_assessment", "Assessments"), extra: me.sortByAssessment}]
				});
		me.ui.sort.directional(true);
		me.sortExtra = me.sortByStatus;
		
		onClick(me.ui.activity_alert_pmAll, me.pmAll);
		onClick(me.ui.activity_alert_pmNever, me.pmNever);
		setupHoverControls([me.ui.activity_alert_pmAll, me.ui.activity_alert_pmNever]);
	};

	this.start = function()
	{
		main.onExit = me.checkExit;
		me.load();
	};

	this.load = function()
	{
		var params = main.portal.cdp.params();
		params.url.site = main.portal.site.id;
		main.portal.cdp.request("activity_alert", params, function(data)
		{
			me.items = /*data.items ||*/ [
			                              {id:1, nameSort:"Smith, Joe", iid:"12345678", rosterName: "1010101", active: true, blocked: false, official: true, adhoc: false, master: false, firstVisit: 0, lastVisit: 0, visits: 3, syllabus: 0, modules: 2, forums: 3, assessments: 4},
			                              {id:1, nameSort:"Sardothian, Celana", iid:"987654321", rosterName: "1010101", active: false, blocked: false, official: true, adhoc: false, master: false, firstVisit: 0, lastVisit: 0, visits: 2, modules: 1, forums: 7, assessments: 2}
			                             ];

			me.ui.itemNav.inject(main.ui.activity_itemnav, {returnFunction: main.startOverview});
			me.itemsSorted = me.sortItems(me.items, me.sortDirection, me.sortExtra);
			me.populate();
		});
	};

	this.onSort = function(direction, option, extra)
	{
		me.sortDirection = direction;
		me.sortMode = option;
		me.sortExtra = extra;

		me.itemsSorted = me.sortItems(me.items, me.sortDirection, me.sortExtra);
		me.populate();
	};

	this.sortItems = function(items, direction, extra)
	{
		var sorted = [];

		if (extra != null)
		{
			sorted = extra(me.items, direction);
		}

		return sorted;
	};

	this.checkExit = function(deferred)
	{
		return true;
	};
	
	this.populate = function()
	{
		var total = 0;
		me.ui.table.clear();
		$.each(me.itemsSorted, function(index, item)
		{
			var tr = me.ui.table.row();
			
			var cell = clone(main.ui.activity_name_template, ["activity_name_template_body", "activity_name_template_name", "activity_name_template_iid"]);
			me.ui.table.hotElement(cell.activity_name_template_body, "", function(){console.log(item);}, null, {width:"calc(100vw - 100px - 796px)", minWidth:"calc(1200px - 100px - 796px"});

			cell.activity_name_template_name.text(item.nameSort);
			cell.activity_name_template_iid.text(item.iid);
			
			me.ui.table.text(item.rosterName, null, {width: 60});
			me.ui.table.text(main.memberStatus(item), "e3_text special light", {fontSize: 11, width: 60, textTransform: "uppercase"});

			me.ui.table.date(item.firstVisit, "-", "date2l");
			me.ui.table.date(item.lastVisit, "-", "date2l");
			me.ui.table.text(item.visits, null, {width: 60});
			me.ui.table.date(item.syllabus, "-", "date2l");
			me.ui.table.text(item.modules, null, {width: 80});
			me.ui.table.text(item.forums, null, {width: 80});
			me.ui.table.text(item.assessments, null, {width: 80});
			
			total++;
		});
		me.ui.table.done();

		show(me.ui.activity_alert_none, (me.ui.table.rowCount() == 0));
		me.ui.activity_alert_heading.text(main.i18n.lookup("msg_alert", "%0 students have not visited the site in the last 7 days.", "html", [total]));
	};

	this.sortByName = function(items, direction)
	{
		return me.ui.sort.sort(items, direction, compareS, "nameSort");
	};
	
	this.sortBySection = function(items, direction)
	{
		return me.ui.sort.sort(items, direction, compareS, "rosterName", compareS, "nameSort");
	};

	this.sortByStatus = function(items, direction)
	{
		return me.ui.sort.sort(items, direction, main.compareMemberStatusRanking, null, compareS, "nameSort");
	};
	
	this.sortByFirstVisit = function(items, direction)
	{
		return me.ui.sort.sort(items, direction, compareN, "firstVisit", compareS, "nameSort");
	};

	this.sortByLastVisit = function(items, direction)
	{
		return me.ui.sort.sort(items, direction, compareN, "lastVisit", compareS, "nameSort");
	};

	this.sortByVisit = function(items, direction)
	{
		return me.ui.sort.sort(items, direction, compareN, "visits", compareS, "nameSort");
	};

	this.sortBySyllabus = function(items, direction)
	{
		return me.ui.sort.sort(items, direction, compareN, "syllabus", compareS, "nameSort");
	};

	this.sortByModule = function(items, direction)
	{
		return me.ui.sort.sort(items, direction, compareN, "modules", compareS, "nameSort");
	};

	this.sortByPost = function(items, direction)
	{
		return me.ui.sort.sort(items, direction, compareN, "forums", compareS, "nameSort");
	};

	this.sortByAssessment = function(items, direction)
	{
		return me.ui.sort.sort(items, direction, compareN, "assessments", compareS, "nameSort");
	};
}

function Activity_moduleItem(main)
{
	var me = this;

	this.ui = null;
	this.items = null;
	this.itemsSorted = null;
	this.modules = null;
	this.sections = null;
	this.module = null;
	this.section = null;
	this.sortDirection = "A";
	this.sortMode = "X";

	this.init = function()
	{
		me.ui = findElements(["activity_moduleItem_actions", "activity_moduleItem_viewed", "activity_moduleItem_title", "activity_moduleItem_pm",
		                      "activity_moduleItem_table", "activity_moduleItem_none"]);
		me.ui.table = new e3_Table(me.ui.activity_moduleItem_table);
		me.ui.itemNav = new e3_ItemNav();

		me.ui.sort = new e3_SortAction();
		me.ui.sort.inject(me.ui.activity_moduleItem_actions,
				{onSort: me.onSort, initial: "X",
					options:[{value:"N", title: main.i18n.lookup("sort_name", "Name"), extra: me.sortByName},
					         {value:"R", title: main.i18n.lookup("sort_section", "Section"), extra: me.sortBySection},
					         {value:"X", title: main.i18n.lookup("sort_status", "Status"), extra: me.sortByStatus},
					         {value:"F", title: main.i18n.lookup("sort_firstVisit", "First Visit"), extra: me.sortByFirstVisit},
					         {value:"L", title: main.i18n.lookup("sort_lastVisit", "Last Visit"), extra: me.sortByLastVisit}]
				});
		me.ui.sort.directional(true);
		me.sortExtra = me.sortByStatus;

		onClick(me.ui.activity_moduleItem_pm, me.exportCsv);
		setupHoverControls([me.ui.activity_moduleItem_pm]);
	};

	this.findSection = function(sectionId)
	{
		for (var m = 0; m < me.modules.length; m++)
		{
			for (var s = 0; s < me.modules[m].sections.length; s++)
			{
				if (me.modules[m].sections[s].id == sectionId) return me.modules[m].sections[s];
			}
		}

		return null;
	};

	this.findModule = function(sectionId)
	{
		for (var m = 0; m < me.modules.length; m++)
		{
			for (var s = 0; s < me.modules[m].sections.length; s++)
			{
				if (me.modules[m].sections[s].id == sectionId) return me.modules[m];
			}
		}

		return null;
	};

	this.collapseSections = function()
	{
		me.sections = [];
		for (var m = 0; m < me.modules.length; m++)
		{
			for (var s = 0; s < me.modules[m].sections.length; s++)
			{
				me.sections.push(me.modules[m].sections[s]);
			}
		}
	};

	this.start = function(sectionId, modules)
	{
		main.onExit = me.checkExit;
		if (modules !== undefined) me.modules = modules;
		me.section = me.findSection(sectionId);
		me.module = me.findModule(sectionId);
		me.load();
	};

	this.load = function()
	{
		var params = main.portal.cdp.params();
		params.url.site = main.portal.site.id;
		params.url.section = me.section.id;
		main.portal.cdp.request("activity_moduleItem", params, function(data)
		{
			me.items = /*data.items ||*/ [
			                              {id:1, nameSort:"Smith, Joe", iid:"12345678", rosterName: "1010101", active: true, blocked: false, official: true, adhoc: false, master: false, firstVisit: 0, lastVisit: 0, visits: 3},
			                              {id:1, nameSort:"Westfall, Choal", iid:"24315534", rosterName: "1010102", active: true, blocked: false, official: true, adhoc: false, master: false, visits: 0},
			                              {id:1, nameSort:"Sardothian, Celana", iid:"987654321", rosterName: "1010101", active: true, blocked: false, official: true, adhoc: false, master: false, firstVisit: 0, lastVisit: 0, visits: 2}
			                             ];
			me.collapseSections();

			me.ui.itemNav.inject(main.ui.activity_itemnav, {returnFunction: main.startModule, pos: position(me.section, me.sections), navigateFunction: me.goModuleItem});
			
			me.itemsSorted = me.sortItems(me.items, me.sortDirection, me.sortExtra);
			me.populate();
		});
	};

	this.onSort = function(direction, option, extra)
	{
		me.sortDirection = direction;
		me.sortMode = option;
		me.sortExtra = extra;

		me.itemsSorted = me.sortItems(me.items, me.sortDirection, me.sortExtra);
		me.populate();
	};

	this.sortItems = function(items, direction, extra)
	{
		var sorted = [];

		if (extra != null)
		{
			sorted = extra(me.items, direction);
		}

		return sorted;
	};

	this.checkExit = function(deferred)
	{
		return true;
	};
	
	this.populate = function()
	{
		var viewed = 0;
		var total = 0;

		me.ui.table.clear();
		$.each(me.itemsSorted, function(index, item)
		{
			var tr = me.ui.table.row();
			
			var cell = clone(main.ui.activity_name_template, ["activity_name_template_body", "activity_name_template_name", "activity_name_template_iid"]);
			me.ui.table.element(cell.activity_name_template_body, null, {width:"calc(100vw - 100px - 376px)", minWidth:"calc(1200px - 100px - 376px"});

			cell.activity_name_template_name.text(item.nameSort);
			cell.activity_name_template_iid.text(item.iid);
			
			me.ui.table.text(item.rosterName, null, {width: 60});
			me.ui.table.text(main.memberStatus(item), "e3_text special light", {fontSize: 11, width: 60, textTransform: "uppercase"});

			me.ui.table.date(item.firstVisit, "-", "date2l");
			me.ui.table.date(item.lastVisit, "-", "date2l");

			// count only active students
			if ((!item.blocked) && item.active && (!item.master))
			{
				if (item.firstVisit != null) viewed++;
				total++;
			}
		});
		me.ui.table.done();

		show(me.ui.activity_moduleItem_none, (me.ui.table.rowCount() == 0));

		me.ui.activity_moduleItem_title.text(main.i18n.lookup("msg_moduleSection", "%0: %1", "html", [me.module.title, me.section.title]));
		me.ui.activity_moduleItem_viewed.text(asPct(viewed, total));
	};

	this.sortByName = function(items, direction)
	{
		return me.ui.sort.sort(items, direction, compareS, "nameSort");
	};
	
	this.sortBySection = function(items, direction)
	{
		return me.ui.sort.sort(items, direction, compareS, "rosterName", compareS, "nameSort");
	};

	this.sortByStatus = function(items, direction)
	{
		return me.ui.sort.sort(items, direction, main.compareMemberStatusRanking, null, compareS, "nameSort");
	};
	
	this.sortByFirstVisit = function(items, direction)
	{
		return me.ui.sort.sort(items, direction, compareN, "firstVisit", compareS, "nameSort");
	};

	this.sortByLastVisit = function(items, direction)
	{
		return me.ui.sort.sort(items, direction, compareN, "lastVisit", compareS, "nameSort");
	};
	
	this.goModuleItem = function(item)
	{
		main.startModuleItem(item.id);
	};
}

function Activity_forumItem(main)
{
	var me = this;

	this.ui = null;
	this.items = null;
	this.itemsSorted = null;
	this.forumItem = null;
	this.forumItems = null;
	this.sortDirection = "A";
	this.sortMode = "X";

	this.init = function()
	{
		me.ui = findElements(["activity_forumItem_actions", "activity_forumItem_posted", "activity_forumItem_absent", "activity_forumItem_title", "activity_forumItem_pm",
		                      "activity_forumItem_table", "activity_forumItem_none"]);
		me.ui.table = new e3_Table(me.ui.activity_forumItem_table);
		me.ui.itemNav = new e3_ItemNav();

		me.ui.sort = new e3_SortAction();
		me.ui.sort.inject(me.ui.activity_forumItem_actions,
				{onSort: me.onSort, initial: "X",
					options:[{value:"N", title: main.i18n.lookup("sort_name", "Name"), extra: me.sortByName},
					         {value:"R", title: main.i18n.lookup("sort_section", "Section"), extra: me.sortBySection},
					         {value:"X", title: main.i18n.lookup("sort_status", "Status"), extra: me.sortByStatus},
					         {value:"P", title: main.i18n.lookup("sort_posts", "Posts"), extra: me.sortByPosts},
					         {value:"V", title: main.i18n.lookup("sort_reviewed", "Reviewed"), extra: me.sortByReviewed}]
				});
		me.ui.sort.directional(true);
		me.sortExtra = me.sortByStatus;

		onClick(me.ui.activity_forumItem_pm, me.exportCsv);
		setupHoverControls([me.ui.activity_forumItem_pm]);
	};

	this.start = function(forumItemId, forumItems)
	{
		main.onExit = me.checkExit;
		if (forumItems !== undefined) me.forumItems = forumItems;
		me.forumItem = findIdInList(forumItemId, me.forumItems);
		me.load();
	};

	this.load = function()
	{
		var params = main.portal.cdp.params();
		params.url.site = main.portal.site.id;
		params.url.forum = me.forumItem.id;
		main.portal.cdp.request("activity_forumItem", params, function(data)
		{
			me.items = /*data.items ||*/ [
			                              {id:1, nameSort:"Smith, Joe", iid:"12345678", rosterName: "1010101", active: true, blocked: false, official: true, adhoc: false, master: false, posts: 2, reviewed: 0},
			                              {id:1, nameSort:"Westfall, Choal", iid:"24315534", rosterName: "1010102", active: true, blocked: false, official: true, adhoc: false, master: false, posts: 4},
			                              {id:1, nameSort:"Sardothian, Celana", iid:"987654321", rosterName: "1010101", active: true, blocked: false, official: true, adhoc: false, master: false, posts:0}
			                             ];

			me.ui.itemNav.inject(main.ui.activity_itemnav, {returnFunction: main.startForum, pos: position(me.forumItem, me.forumItems), navigateFunction: me.goForumItem});
			
			me.itemsSorted = me.sortItems(me.items, me.sortDirection, me.sortExtra);
			me.populate();
		});
	};

	this.onSort = function(direction, option, extra)
	{
		me.sortDirection = direction;
		me.sortMode = option;
		me.sortExtra = extra;

		me.itemsSorted = me.sortItems(me.items, me.sortDirection, me.sortExtra);
		me.populate();
	};

	this.sortItems = function(items, direction, extra)
	{
		var sorted = [];

		if (extra != null)
		{
			sorted = extra(me.items, direction);
		}

		return sorted;
	};

	this.checkExit = function(deferred)
	{
		return true;
	};
	
	this.populate = function()
	{
		var posted = 0;
		var total = 0;

		me.ui.table.clear();
		$.each(me.itemsSorted, function(index, item)
		{
			var tr = me.ui.table.row();
			
			var cell = clone(main.ui.activity_name_template, ["activity_name_template_body", "activity_name_template_name", "activity_name_template_iid"]);
			me.ui.table.element(cell.activity_name_template_body, null, {width:"calc(100vw - 100px - 356px)", minWidth:"calc(1200px - 100px - 356px"});

			cell.activity_name_template_name.text(item.nameSort);
			cell.activity_name_template_iid.text(item.iid);
			
			me.ui.table.text(item.rosterName, null, {width: 60});
			me.ui.table.text(main.memberStatus(item), "e3_text special light", {fontSize: 11, width: 60, textTransform: "uppercase"});

			me.ui.table.text(item.posts, null, {width: 60});
			me.ui.table.date(item.reviewed, "-", "date2l");

			// count only active students
			if ((!item.blocked) && item.active && (!item.master))
			{
				if (item.posts > null) posted++;
				total++;
			}
		});
		me.ui.table.done();

		show(me.ui.activity_forumItem_none, (me.ui.table.rowCount() == 0));

		me.ui.activity_forumItem_title.text(me.forumItem.title);
		me.ui.activity_forumItem_posted.text(asPct(posted, total));
		me.ui.activity_forumItem_absent.text(asPct(total-posted, total));
	};

	this.sortByName = function(items, direction)
	{
		return me.ui.sort.sort(items, direction, compareS, "nameSort");
	};
	
	this.sortBySection = function(items, direction)
	{
		return me.ui.sort.sort(items, direction, compareS, "rosterName", compareS, "nameSort");
	};

	this.sortByStatus = function(items, direction)
	{
		return me.ui.sort.sort(items, direction, main.compareMemberStatusRanking, null, compareS, "nameSort");
	};
	
	this.sortByPosts = function(items, direction)
	{
		return me.ui.sort.sort(items, direction, compareN, "posts", compareS, "nameSort");
	};

	this.sortByReviewed = function(items, direction)
	{
		return me.ui.sort.sort(items, direction, compareN, "reviewed", compareS, "nameSort");
	};
	
	this.goForumItem = function(item)
	{
		main.startForumItem(item.id);
	};
}

function Activity_assessmentItem(main)
{
	var me = this;

	this.ui = null;
	this.items = null;
	this.itemsSorted = null;
	this.assessments = null;
	this.assessment = null;
	this.sortDirection = "A";
	this.sortMode = "X";

	this.init = function()
	{
		me.ui = findElements(["activity_assessmentItem_actions", "activity_assessmentItem_submitted", "activity_assessmentItem_inProgress", "activity_assessmentItem_absent",
		                      "activity_assessmentItem_title", "activity_assessmentItem_pm",
		                      "activity_assessmentItem_table", "activity_assessmentItem_none"]);
		me.ui.table = new e3_Table(me.ui.activity_assessmentItem_table);
		me.ui.itemNav = new e3_ItemNav();

		me.ui.sort = new e3_SortAction();
		me.ui.sort.inject(me.ui.activity_assessmentItem_actions,
				{onSort: me.onSort, initial: "X",
					options:[{value:"N", title: main.i18n.lookup("sort_name", "Name"), extra: me.sortByName},
					         {value:"R", title: main.i18n.lookup("sort_section", "Section"), extra: me.sortBySection},
					         {value:"X", title: main.i18n.lookup("sort_status", "Status"), extra: me.sortByStatus},
					         {value:"S", title: main.i18n.lookup("sort_started", "Started"), extra: me.sortByStarted},
					         {value:"F", title: main.i18n.lookup("sort_finished", "Finished"), extra: me.sortByFinished},
					         {value:"V", title: main.i18n.lookup("sort_reviewed", "Reviewed"), extra: me.sortByReviewed}]
				});
		me.ui.sort.directional(true);
		me.sortExtra = me.sortByStatus;

		onClick(me.ui.activity_assessmentItem_pm, me.exportCsv);
		setupHoverControls([me.ui.activity_assessmentItem_pm]);
	};

	this.start = function(assessmentId, assessments)
	{
		main.onExit = me.checkExit;
		if (assessments !== undefined) me.assessments = assessments;
		me.assessment = findIdInList(assessmentId, me.assessments);
		me.load();
	};

	this.load = function()
	{
		var params = main.portal.cdp.params();
		params.url.site = main.portal.site.id;
		params.url.assessment = me.assessment.id;
		main.portal.cdp.request("activity_assessmentItem", params, function(data)
		{
			me.items = /*data.items ||*/ [
			                              {id:1, nameSort:"Smith, Joe", iid:"12345678", rosterName: "1010101", active: true, blocked: false, official: true, adhoc: false, master: false, started: 0, finished: 0, reviewed: 0},
			                              {id:1, nameSort:"Westfall, Choal", iid:"24315534", rosterName: "1010102", active: true, blocked: false, official: true, adhoc: false, master: false},
			                              {id:1, nameSort:"Sardothian, Celana", iid:"987654321", rosterName: "1010101", active: true, blocked: false, official: true, adhoc: false, master: false, started: 0}
			                             ];

			me.ui.itemNav.inject(main.ui.activity_itemnav, {returnFunction: main.startAssessment, pos: position(me.assessment, me.assessments), navigateFunction: me.goAssessment});
			
			me.itemsSorted = me.sortItems(me.items, me.sortDirection, me.sortExtra);
			me.populate();
		});
	};

	this.onSort = function(direction, option, extra)
	{
		me.sortDirection = direction;
		me.sortMode = option;
		me.sortExtra = extra;

		me.itemsSorted = me.sortItems(me.items, me.sortDirection, me.sortExtra);
		me.populate();
	};

	this.sortItems = function(items, direction, extra)
	{
		var sorted = [];

		if (extra != null)
		{
			sorted = extra(me.items, direction);
		}

		return sorted;
	};

	this.checkExit = function(deferred)
	{
		return true;
	};
	
	this.populate = function()
	{
		var submitted = 0;
		var inProgress = 0;
		var total = 0;

		me.ui.table.clear();
		$.each(me.itemsSorted, function(index, item)
		{
			var tr = me.ui.table.row();
			
			var cell = clone(main.ui.activity_name_template, ["activity_name_template_body", "activity_name_template_name", "activity_name_template_iid"]);
			me.ui.table.element(cell.activity_name_template_body, null, {width:"calc(100vw - 100px - 464px)", minWidth:"calc(1200px - 100px - 464px"});

			cell.activity_name_template_name.text(item.nameSort);
			cell.activity_name_template_iid.text(item.iid);
			
			me.ui.table.text(item.rosterName, null, {width: 60});
			me.ui.table.text(main.memberStatus(item), "e3_text special light", {fontSize: 11, width: 60, textTransform: "uppercase"});

			me.ui.table.date(item.started, "-", "date2l");
			me.ui.table.date(item.finished, "-", "date2l");
			me.ui.table.date(item.reviewed, "-", "date2l");

			// count only active students
			if ((!item.blocked) && item.active && (!item.master))
			{
				if (item.started != null)
				{
					if (item.finished != null)
					{
						submitted++;
					}
					else
					{
						inProgress++;
					}
				}

				total++;
			}
		});
		me.ui.table.done();

		show(me.ui.activity_assessmentItem_none, (me.ui.table.rowCount() == 0));

		me.ui.activity_assessmentItem_title.text(me.assessment.title);
		me.ui.activity_assessmentItem_submitted.text(asPct(submitted, total));
		me.ui.activity_assessmentItem_inProgress.text(inProgress);
		me.ui.activity_assessmentItem_absent.text(total - (submitted + inProgress));
	};

	this.sortByName = function(items, direction)
	{
		return me.ui.sort.sort(items, direction, compareS, "nameSort");
	};
	
	this.sortBySection = function(items, direction)
	{
		return me.ui.sort.sort(items, direction, compareS, "rosterName", compareS, "nameSort");
	};

	this.sortByStatus = function(items, direction)
	{
		return me.ui.sort.sort(items, direction, main.compareMemberStatusRanking, null, compareS, "nameSort");
	};
	
	this.sortByStarted = function(items, direction)
	{
		return me.ui.sort.sort(items, direction, compareN, "started", compareS, "nameSort");
	};

	this.sortByFinished = function(items, direction)
	{
		return me.ui.sort.sort(items, direction, compareN, "finished", compareS, "nameSort");
	};
	
	this.sortByReviewed = function(items, direction)
	{
		return me.ui.sort.sort(items, direction, compareN, "reviewed", compareS, "nameSort");
	};
	
	this.goAssessment = function(item)
	{
		main.startAssessmentItem(item.id);
	};
}

$(function()
{
	try
	{
		activity_tool = new Activity();
		activity_tool.init();
		activity_tool.start();
	}
	catch (e)
	{
		error(e);
	}
});

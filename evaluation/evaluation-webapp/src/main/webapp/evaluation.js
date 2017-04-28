/**********************************************************************************
 * $URL: https://source.etudes.org/svn/serenity/trunk/evaluation/evaluation-webapp/src/main/webapp/evaluation.js $
 * $Id: evaluation.js 12504 2016-01-10 00:30:08Z ggolden $
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

var evaluation_tool = null;

function Evaluation()
{
	var me = this;

	this.i18n = new e3_i18n(evaluation_i10n, "en-us");
	this.portal = null;
	
	this.overviewMode = null;
	this.gradeMode = null;
	this.optionMode = null;
	this.categoryMode = null;
	this.rubricMode = null;
	this.rubricEditMode = null;
	this.rubricCriteriaEditMode = null;
	this.itemGradeMode = null;
	this.memberGradeMode = null;
	this.reviewMode = null;
	this.modes = null;

	this.ui = null;
	this.onExit = null;

	this.evaluation = null;

	this.sections = [];
	this.categories = [];
	this.options = null;
	this.rubrics = [];

//	this.colorsLight = ["#FFE9E9", "#FCF5CE", "#DBEFDB"]; // matches etudes-evaluation.js e3_EvaluationGrade

	this.findCategory = function(id)
	{
		for (i = 0; i < me.categories.length; i++)
		{
			if (me.categories[i].id == id) return me.categories[i];
		}

		return {title: "-", id:0};
	};

	this.numberToDrop = function()
	{
		var rv = 0;
		for (i = 0; i < me.categories.length; i++)
		{
			rv += me.categories[i].drop;
		}

		return rv;
	};

	this.findRubric = function(id)
	{
		for (i = 0; i < me.rubrics.length; i++)
		{
			if (me.rubrics[i].id == id) return me.rubrics[i];
		}

		return null;
	};

	this.typeTd = function(item, table)
	{
		table.text(me.i18n.lookup("msg_type_" + item.type), "e3_text special light", {fontSize:11, width:60});
	};

	// dot for item status // TODO: what dots to use?  not green (published) and red (unpublished)
	this.itemStatusDotTd = function(item, table)
	{
		if (item.schedule.status == ScheduleStatus.closed)
		{
			table.dot("red", me.i18n.lookup("msg_closed", "closed"));
		}
		else if (item.schedule.status == ScheduleStatus.open)
		{
			table.dot("green", me.i18n.lookup("msg_open", "open"));
		}
		else if (item.schedule.status == ScheduleStatus.willOpen)
		{
			table.dot("yellow", me.i18n.lookup("msg_willOpen", "will open"));
		}
		else if (item.schedule.status == ScheduleStatus.willOpenHide)
		{
			table.dot("gray", me.i18n.lookup("msg_willOpenHidden", "hidden until open"));
		}
	};

	this.memberStatusRanking = function(member)
	{
		if (member.blocked) return 3;
		if (!member.active) return 4;
		if (member.adhoc) return 2;
		return 1;
	};

	// dot for student status: green for enrolled, hollow green for added, red for dropped, hollow red for blocked   member.blocked    member.adhoc   member.active
	this.memberStatusDotTd = function(member, table)
	{
		if (member.blocked)
		{
			table.dot("yellow", me.i18n.lookup("msg_blocked", "blocked"));
		}
		else if (!member.active)
		{
			table.dot("red", me.i18n.lookup("msg_dropped", "dropped"));
		}
		else if (member.adhoc)
		{
			table.dot("green", me.i18n.lookup("msg_added", "added"));
		}
		else
		{
			table.dot("green", me.i18n.lookup("msg_enrolled", "enrolled"));
		}
	};

	// dot for student status
	this.memberStatusDot = function(member)
	{
		var rv = null;
		if (member.blocked)
		{
			rv = dotSmall("yellow", me.i18n.lookup("msg_blocked", "blocked"));
		}
		else if (!member.active)
		{
			rv = dotSmall("red", me.i18n.lookup("msg_dropped", "dropped"));
		}
		else if (member.adhoc)
		{
			rv = dotSmall("green", me.i18n.lookup("msg_added", "added"));
		}
		else
		{
			rv = dotSmall("green", me.i18n.lookup("msg_enrolled", "enrolled"));
		}

		return rv;
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

	this.categoryWeightingSummary = function(categories)
	{
		var rv = {total: 0, extra: 0, set: false, invalid: false};

		for (var i = 0; i < categories.length; i++)
		{
			if (categories[i].weight != null)
			{
				rv.set = true;
				if (categories[i].type == ToolItemType.extra.id)
				{
					rv.extra += categories[i].weight;
				}
				else
				{
					rv.total += categories[i].weight;
				}
			}
		}
		
		if (rv.set && (rv.total != 100)) rv.invalid = true;

		return rv;
	};

	this.init = function()
	{
		me.i18n.localize();

		me.ui = findElements(["evaluation_header", "evaluation_modebar", "evaluation_headerbar", "evaluation_headerbar2", "evaluation_itemnav",
		                      "evaluation_overview", "evaluation_bar_overview", "evaluation_header_overview",
		                      "evaluation_grade", "evaluation_bar_grade", "evaluation_bar2_grade", "evaluation_header_grade",
		                      "evaluation_option",
		                      "evaluation_category", "evaluation_bar_category", "evaluation_header_category",
		                      "evaluation_itemGrade", "evaluation_bar_itemGrade", "evaluation_bar2_itemGrade", "evaluation_header_itemGrade",
		                      "evaluation_rubric", "evaluation_bar_rubric", "evaluation_header_rubric",
		                      "evaluation_rubricEdit", "evaluation_bar_rubricEdit",
		                      "evaluation_rubricCriterionEdit", "evaluation_bar_rubricCriterionEdit",
		                      "evaluation_review",  "evaluation_bar_review", 
		                      "evaluation_memberGrade",  "evaluation_bar_memberGrade", "evaluation_bar2_memberGrade", "evaluation_header_memberGrade",
		                      "evaluation_selectFirst", "evaluation_select1First",
		                      "evaluation_rubricEdit_alertView", "evaluation_rubricEdit_alertView_view"]);
		me.portal = portal_tool.features({onExit: me.checkExit, pin:[{ui:me.ui.evaluation_header}]});
		me.evaluation = new e3_Evaluation(me.portal.cdp, me.portal.dialogs, me.portal.timestamp);

		me.memberGradeMode = new Evaluation_memberGrade(me);
		me.reviewMode = new Evaluation_review(me);

		if ((me.portal.site.role >= Role.instructor) || (me.portal.site.role == Role.ta))
		{
			me.ui.modebar = new e3_Modebar(me.ui.evaluation_modebar);
			me.overviewMode = new Evaluation_overview(me);
			me.gradeMode = new Evaluation_grade(me);
			me.optionMode = new Evaluation_option(me);
			me.categoryMode = new Evaluation_category(me);
			me.rubricMode = new Evaluation_rubric(me);
			me.rubricEditMode = new Evaluation_rubricEdit(me);
			me.rubricCriterionEditMode = new Evaluation_rubricCriterionEdit(me);
			me.itemGradeMode = new Evaluation_itemGrade(me);
			me.modes =
				[
					{name:me.i18n.lookup("mode_overview", "Overview"), func:function(){me.startOverview();}},
					{name:me.i18n.lookup("mode_grades", "Grades"), func:function(){me.startGrade();}},
					{name:me.i18n.lookup("mode_options", "Options"), func:function(){me.startOption();}},
					{name:me.i18n.lookup("mode_category", "Categories"), func:function(){me.startCategory();}},
					{name:me.i18n.lookup("mode_rubric", "Rubrics"), func:function(){me.startRubric();}}
				];
			me.ui.modebar.set(me.modes, 0);
			me.overviewMode.init();
			me.gradeMode.init();
			me.optionMode.init();
			me.categoryMode.init();
			me.rubricMode.init();
			me.rubricEditMode.init();
			me.rubricCriterionEditMode.init();
			me.itemGradeMode.init();
		}
		
		me.memberGradeMode.init();
		me.reviewMode.init();
		show(me.ui.evaluation_modebar, ((me.portal.site.role >= Role.instructor) || (me.portal.site.role == Role.ta)));
	};

	this.start = function()
	{
		if ((me.portal.site.role >= Role.instructor) || (me.portal.site.role == Role.ta))
		{
			me.startOverview();
		}
		else if (me.portal.site.role == Role.student)
		{
			me.startMemberGradeForMember();
		}
	};

	this.startOverview = function()
	{
		if ((me.onExit != null) && (!me.onExit(function(){me.startOverview();}))) return;
		me.mode([me.ui.evaluation_headerbar, me.ui.evaluation_bar_overview, me.ui.evaluation_header_overview, me.ui.evaluation_overview]);
		me.ui.modebar.showSelected(0);
		me.overviewMode.start();
	};

	this.startGrade = function()
	{
		if ((me.onExit != null) && (!me.onExit(function(){me.startGrade();}))) return;
		me.mode([me.ui.evaluation_headerbar, me.ui.evaluation_bar_grade, me.ui.evaluation_headerbar2, me.ui.evaluation_bar2_grade, me.ui.evaluation_header_grade, me.ui.evaluation_grade]);
		me.ui.modebar.showSelected(1);
		me.gradeMode.start();
	};

	this.startOption = function()
	{
		if ((me.onExit != null) && (!me.onExit(function(){me.startOption();}))) return;
		me.mode(me.ui.evaluation_option);
		me.ui.modebar.showSelected(2);
		me.optionMode.start();
	};

	this.startCategory = function()
	{
		if ((me.onExit != null) && (!me.onExit(function(){me.startCategory();}))) return;
		me.mode([me.ui.evaluation_headerbar, me.ui.evaluation_bar_category, me.ui.evaluation_header_category, me.ui.evaluation_category]);
		me.ui.modebar.showSelected(3);
		me.categoryMode.start();
	};

	this.startRubric = function()
	{
		if ((me.onExit != null) && (!me.onExit(function(){me.startRubric();}))) return;
		me.mode([me.ui.evaluation_headerbar, me.ui.evaluation_bar_rubric, me.ui.evaluation_header_rubric, me.ui.evaluation_rubric]);
		me.ui.modebar.showSelected(4);
		me.rubricMode.start();
	};

	this.startRubricEdit = function(rubric)
	{
		// save only if we have a rubric - else we are returning from criterion edit
		if (rubric != null)
		{
			if ((me.onExit != null) && (!me.onExit(function(){me.startRubricEdit(rubric);}))) return;
		}
		me.mode([me.ui.evaluation_headerbar, me.ui.evaluation_itemnav, me.ui.evaluation_bar_rubricEdit, me.ui.evaluation_rubricEdit]);
		me.ui.modebar.showSelected(4);
		me.rubricEditMode.start(rubric);
	};

	this.startRubricCriterionEdit = function(rubric, criterion)
	{
		// no save
		// if ((me.onExit != null) && (!me.onExit(function(){me.startRubricCriterionEdit(rubric, criterion);}))) return;
		me.mode([me.ui.evaluation_headerbar, me.ui.evaluation_itemnav, me.ui.evaluation_bar_rubricCriterionEdit, me.ui.evaluation_rubricCriterionEdit]);
		me.ui.modebar.showSelected(4);
		me.rubricCriterionEditMode.start(rubric, criterion);
	};

	this.startItemGrade = function(item, items)
	{
		if ((me.onExit != null) && (!me.onExit(function(){me.startItemGrade(item, items);}))) return;
		me.mode([me.ui.evaluation_headerbar, me.ui.evaluation_itemnav, me.ui.evaluation_bar_itemGrade,  me.ui.evaluation_headerbar2, me.ui.evaluation_bar2_itemGrade, me.ui.evaluation_header_itemGrade, me.ui.evaluation_itemGrade]);
		me.ui.modebar.showSelected(0);
		me.itemGradeMode.start(item, items);
	};

	this.startItemGradeReview = function(member, members, item, items)
	{
		if ((me.onExit != null) && (!me.onExit(function(){me.startItemGradeReview(member, members, item);}))) return;
		me.mode([me.ui.evaluation_headerbar, me.ui.evaluation_itemnav, me.ui.evaluation_bar_review, me.ui.evaluation_review]);
		me.ui.modebar.showSelected(0);
		me.reviewMode.start(item, null, member, members, false, function(){me.startItemGrade(item, items)});
	};

	this.startMemberGrade = function(member, members)
	{
		if ((me.onExit != null) && (!me.onExit(function(){me.startMemberGrade(member, members);}))) return;
		me.mode([me.ui.evaluation_headerbar, me.ui.evaluation_itemnav, me.ui.evaluation_bar_memberGrade,
		         me.ui.evaluation_headerbar2, me.ui.evaluation_bar2_memberGrade, me.ui.evaluation_header_memberGrade, me.ui.evaluation_memberGrade]);
		if (me.ui.modebar != null) me.ui.modebar.showSelected(1);
		me.memberGradeMode.start(member, members);
	};

	this.startMemberGradeForMember = function()
	{
		if ((me.onExit != null) && (!me.onExit(function(){me.startMemberGrade(member, members);}))) return;
		me.mode([me.ui.evaluation_headerbar2, me.ui.evaluation_bar2_memberGrade, me.ui.evaluation_header_memberGrade, me.ui.evaluation_memberGrade]);
		if (me.ui.modebar != null) me.ui.modebar.showSelected(1);
		me.memberGradeMode.start();
	};

	this.startMemberGradeReview = function(item, items, member, members, forMember)
	{
		if ((me.onExit != null) && (!me.onExit(function(){me.startMemberGradeReview(item, items, member, forStudent);}))) return;
		me.mode([me.ui.evaluation_headerbar, me.ui.evaluation_itemnav, me.ui.evaluation_bar_review, me.ui.evaluation_review]);
		if (me.ui.modebar != null) me.ui.modebar.showSelected(1);
		me.reviewMode.start(item, items, member, null, forMember, (forMember ? me.startMemberGradeForMember : function(){me.startMemberGrade(member, members);}));
	};

	this.mode = function(modeUi)
	{
		hide([me.ui.evaluation_headerbar, me.ui.evaluation_headerbar2, me.ui.evaluation_itemnav,
			  me.ui.evaluation_overview, me.ui.evaluation_bar_overview, me.ui.evaluation_header_overview,
			  me.ui.evaluation_grade, me.ui.evaluation_bar_grade, me.ui.evaluation_bar2_grade, me.ui.evaluation_header_grade,
			  me.ui.evaluation_option,
			  me.ui.evaluation_category, me.ui.evaluation_bar_category, me.ui.evaluation_header_category,
			  me.ui.evaluation_itemGrade, me.ui.evaluation_bar_itemGrade,  me.ui.evaluation_bar2_itemGrade, me.ui.evaluation_header_itemGrade,
			  me.ui.evaluation_rubric, me.ui.evaluation_bar_rubric, me.ui.evaluation_header_rubric,
			  me.ui.evaluation_rubricEdit, me.ui.evaluation_bar_rubricEdit,
			  me.ui.evaluation_rubricCriterionEdit, me.ui.evaluation_bar_rubricCriterionEdit,
			  me.ui.evaluation_review,  me.ui.evaluation_bar_review, 
			  me.ui.evaluation_memberGrade,  me.ui.evaluation_bar_memberGrade, me.ui.evaluation_bar2_memberGrade, me.ui.evaluation_header_memberGrade]);
		me.onExit = null;
		me.portal.resetScrolling();
		show(modeUi);
	};
	
	this.checkExit = function(deferred)
	{
		if (me.onExit == null) return true;
		return me.onExit(deferred);
	};
	
	this.extractSections = function(members)
	{
		me.sections = [];
		$.each(members, function(index, member)
		{
			if (member.adhoc) return;

			if (me.sections.indexOf(member.rosterName) == -1)
			{
				me.sections.push(member.rosterName);
			}
		});
		me.sections.sort();
	};

	this.sortByCategory = function(items, withCategories)
	{
		var byCats = {0:[]};
		$.each(me.categories, function(index, cat)
		{
			byCats[cat.id] = [];
		});
		
		$.each(items, function(index, item)
		{
			byCats[item.design.category].push(item);
		});
		
		$.each(byCats, function(catId, array)
		{
			array.sort(function(a, b)
			{
				var rv = compareN(a.design.categoryPosition, b.design.categoryPosition);
				if (rv == 0)
				{
					rv = compareN(a.schedule.due, b.schedule.due);
					if (rv == 0)
					{
						rv = compareS(a.title, b.title, false);
					}
				}
				return rv;
			});
			
			// compute points for category
			var cat = me.findCategory(catId);
			if (cat != null)
			{
				var totalPoints = 0;
				$.each(array, function(index, item)
				{
					totalPoints += item.design.points;
				});
			
				cat.points = totalPoints;
				// cat.numItems = array.length;
			}
		});

		var sorted = byCats[0];
		$.each(me.categories, function(index, cat)
		{
			if (withCategories) sorted.push(cat);
			sorted = sorted.concat(byCats[cat.id]);
		});

		// TODO: give each an order
		$.each(sorted, function(index, item)
		{
			item.order = index;
		});

		return sorted;
	};

	this.findItemByCategoryOrder = function(items, order)
	{
		for (var i = 0; i < items.length; i++)
		{
			if (items[i].order == order) return items[i];
		}
		
		return null;
	};

	this.sortByTitle = function(items, direction)
	{
		var sorted = [].concat(items);
		var adjust = (direction == 'A') ? 1 : -1;
		sorted.sort(function(a, b)
		{
			var rv = adjust * compareS(a.title, b.title);
			if (rv == 0)
			{
				rv = compareN(a.toolItem.itemId, b.toolItem.itemId);
			}
			return rv;
		});

		return sorted;
	};

	this.sortByOpen = function(items, direction)
	{
		var sorted = [].concat(items);
		var adjust = (direction == 'A') ? 1 : -1;
		sorted.sort(function(a, b)
		{
			var rv = adjust * compareN(a.schedule.open, b.schedule.open);
			if (rv == 0)
			{
				rv = compareS(a.title, b.title);
				if (rv == 0)
				{
					rv = compareN(a.toolItem.itemId, b.toolItem.itemId);
				}
			}
			return rv;
		});

		return sorted;
	};

	this.sortByDue = function(items, direction)
	{
		var sorted = [].concat(items);
		var adjust = (direction == 'A') ? 1 : -1;
		sorted.sort(function(a, b)
		{
			var rv = adjust * compareN(a.schedule.due, b.schedule.due);
			if (rv == 0)
			{
				rv = compareS(a.title, b.title);
				if (rv == 0)
				{
					rv = compareN(a.toolItem.itemId, b.toolItem.itemId);
				}
			}
			return rv;
		});

		return sorted;
	};
	
	this.sortMembersByStatus = function(members, direction)
	{
		var sorted = [].concat(members);
		var adjust = (direction == 'A') ? 1 : -1;
		sorted.sort(function(a, b)
		{
			var rv = adjust * compareN(me.memberStatusRanking(a), me.memberStatusRanking(b));
			if (rv == 0)
			{
				rv = compareS(a.nameSort, b.nameSort);
				if (rv == 0)
				{
					rv = compareN(a.userId, b.userId);
				}
			}
			return rv;
		});

		return sorted;
	};

	this.sortMembersByName = function(members, direction)
	{
		var sorted = [].concat(members);
		var adjust = (direction == 'A') ? 1 : -1;
		sorted.sort(function(a, b)
		{
			var rv = adjust * compareS(a.nameSort, b.nameSort);
			if (rv == 0)
			{
				rv = compareN(a.userId, b.userId);
			}
			return rv;
		});

		return sorted;
	};
	
	this.sortMembersByRoster = function(members, direction)
	{
		var sorted = [].concat(members);
		var adjust = (direction == 'A') ? 1 : -1;
		sorted.sort(function(a, b)
		{
			var rv = adjust * compareS(a.rosterName, b.rosterName);
			if (rv == 0)
			{
				rv = compareN(me.memberStatusRanking(a), me.memberStatusRanking(b));
				if (rv == 0)
				{
					rv = compareS(a.nameSort, b.nameSort);
					if (rv == 0)
					{
						rv = compareN(a.userId, b.userId);
					}
				}
			}
			return rv;
		});

		return sorted;
	};

	this.sortMembersBySummaryScore = function(members, direction)
	{
		var sorted = [].concat(members);
		var adjust = (direction == 'A') ? 1 : -1;
		sorted.sort(function(a, b)
		{
			var aVal = ((a.summary == null) ? null : a.summary.score);
			var bVal = ((b.summary == null) ? null : b.summary.score);
			var rv = adjust * compareN(aVal, bVal);
			if (rv == 0)
			{
				rv = compareN(me.memberStatusRanking(a), me.memberStatusRanking(b));
				if (rv == 0)
				{
					rv = compareS(a.nameSort, b.nameSort);
					if (rv == 0)
					{
						rv = compareN(a.userId, b.userId);
					}
				}
			}
			return rv;
		});

		return sorted;
	};

	this.sortMembersByEvalScore = function(members, direction)
	{
		var sorted = [].concat(members);
		var adjust = (direction == 'A') ? 1 : -1;
		sorted.sort(function(a, b)
		{
			var aVal = ((a.evaluation == null) ? null : a.evaluation.score);
			var bVal = ((b.evaluation == null) ? null : b.evaluation.score);
			var rv = adjust * compareN(aVal, bVal);
			if (rv == 0)
			{
				rv = compareN(me.memberStatusRanking(a), me.memberStatusRanking(b));
				if (rv == 0)
				{
					rv = compareS(a.nameSort, b.nameSort);
					if (rv == 0)
					{
						rv = compareN(a.userId, b.userId);
					}
				}
			}
			return rv;
		});

		return sorted;
	};

	this.sortMembersByEvalSubmittedOn = function(members, direction)
	{
		var sorted = [].concat(members);
		var adjust = (direction == 'A') ? 1 : -1;
		sorted.sort(function(a, b)
		{
			var aVal = ((a.evaluation == null) ? null : a.evaluation.submittedOn);
			var bVal = ((b.evaluation == null) ? null : b.evaluation.submittedOn);
			var rv = adjust * compareN(aVal, bVal);
			if (rv == 0)
			{
				rv = compareN(me.memberStatusRanking(a), me.memberStatusRanking(b));
				if (rv == 0)
				{
					rv = compareS(a.nameSort, b.nameSort);
					if (rv == 0)
					{
						rv = compareN(a.userId, b.userId);
					}
				}
			}
			return rv;
		});

		return sorted;
	};

	this.sortMembersByEvalReviewedOn = function(members, direction)
	{
		var sorted = [].concat(members);
		var adjust = (direction == 'A') ? 1 : -1;
		sorted.sort(function(a, b)
		{
			var aVal = ((a.evaluation == null) ? null : a.evaluation.reviewedOn);
			var bVal = ((b.evaluation == null) ? null : b.evaluation.reviewedOn);
			var rv = adjust * compareN(aVal, bVal);
			if (rv == 0)
			{
				rv = compareN(me.memberStatusRanking(a), me.memberStatusRanking(b));
				if (rv == 0)
				{
					rv = compareS(a.nameSort, b.nameSort);
					if (rv == 0)
					{
						rv = compareN(a.userId, b.userId);
					}
				}
			}
			return rv;
		});

		return sorted;
	};
}

function Evaluation_overview(main)
{
	var me = this;

	this.items = [];
	this.itemsSorted = [];
	this.summary = {};
	this.reorder = null;

	this.ui = null;

	this.sortDirection = "A";
	this.sortMode = "C";
	this.showCategories = true;

	this.init = function()
	{
		me.ui = findElements(["evaluation_overview_assessments", "evaluation_overview_points", "evaluation_overview_points_ec",
		                      "evaluation_overview_table", "evaluation_overview_none", "evaluation_overview_actions"]);
		me.ui.table = new e3_Table(me.ui.evaluation_overview_table);
		me.ui.sort = new e3_SortAction();
		me.ui.sort.inject(me.ui.evaluation_overview_actions,
				{onSort: me.onSort, options:[{value:"C", title:main.i18n.lookup("sort_category", "Category")}, {value:"T", title:main.i18n.lookup("sort_title", "Title")},
				                             {value:"O", title:main.i18n.lookup("sort_open", "Open")}, {value:"D", title:main.i18n.lookup("sort_due", "Due")}]});
	};

	this.start = function()
	{
		main.onExit = me.checkExit;
		me.load();
	};

	this.onSort = function(direction, option)
	{
		if ((main.onExit != null) && (!main.onExit(function(){me.onSort(direction, option);}))) return;

		me.sortBy(direction, option);
		me.populate();
	};

	this.sortBy = function(direction, option)
	{
		me.sortDirection = direction;
		me.sortMode = option;
		me.showCategories = (me.sortMode == "C");
		me.ui.sort.directional(me.sortMode != "C");

		if (me.sortMode == "C")
		{
			me.itemsSorted = main.sortByCategory(me.items, true);
		}
		else if (me.sortMode == "T")
		{
			me.itemsSorted = main.sortByTitle(me.items, direction);
		}
		else if (me.sortMode == "O")
		{
			me.itemsSorted = main.sortByOpen(me.items, direction);
		}
		else if (me.sortMode == "D")
		{
			me.itemsSorted = main.sortByDue(me.items, direction);
		}
	};

	this.load = function()
	{
		var params = main.portal.cdp.params();
		params.url.site = main.portal.site.id;
		main.portal.cdp.request("evaluation_gradingItems evaluation_categories evaluation_options", params, function(data)
		{
			me.items = data.items || [];
			me.summary = data.summary || {};
			main.categories = data.categories || [];
			main.options = data.options;
			
			me.sortBy(me.sortDirection, me.sortMode);
			me.populate();
		});
	};

	this.populate = function()
	{
		var categoryWeights = main.categoryWeightingSummary(main.categories);

		me.ui.evaluation_overview_assessments.text(me.summary.items + me.summary.extraItems);
		me.ui.evaluation_overview_points.text(me.summary.points);
		show(me.ui.evaluation_overview_points);
		
		if (categoryWeights.set)
		{
			me.ui.evaluation_overview_points_ec.text((categoryWeights.total + categoryWeights.extra) + "%");
		}
		else
		{
			me.ui.evaluation_overview_points_ec.text(me.summary.points + me.summary.extraPoints);
		}

		me.ui.table.enableReorder(((me.showCategories) ? me.applyOrder : null));

		me.ui.table.clear();
		var firstCategory = true;
		$.each(me.itemsSorted, function(index, item)
		{
			var tr = me.ui.table.row();

			if (item.design === undefined) // category
			{
				var msg = null;
				if (item.weight != null)
				{
					msg = main.i18n.lookup("msg_catWorthWeighted", "Worth: %0% (w/ %1 pts)", "html", [item.weight, item.points]);
				}
				else
				{
					msg = main.i18n.lookup("msg_catWorth", "Worth: %0 (%1 of %2)", "html", [asPct(item.points, me.summary.points), item.points, me.summary.points]);
				}
				var td = me.ui.table.headerRow(item.title);
				td.find("div").append($("<div />", {"hideOnReorder": "1"}).css({display: "inline-block", fontSize: 12, textTransform: "none", marginLeft: 32}).text(msg));

				me.ui.table.includeInOrderDisabled(td, "C:" + item.id);
				me.ui.table.disableRowReorder();
				
				if (firstCategory)
				{
					me.ui.table.disableRowReorderTarget();
					firstCategory = false;
				}

				return;
			}

			if (me.showCategories)
			{
				me.ui.table.reorder(main.i18n.lookup("msg_reorder", "drag to reorder"), "I:" + item.toolItem.itemId);
			}
			else
			{
				me.ui.table.text("", "icon");
			}

			// main.itemStatusDotTd(item, me.ui.table);
			me.ui.table.hotText(item.title, main.i18n.lookup("msg_view", "view"), function()
			{
				main.startItemGrade(item, me.itemsSorted);
			}, null, {width:"calc(100vw - 100px - 596px)", minWidth:"calc(1200px - 100px - 596px)"});
			main.typeTd(item, me.ui.table);
			me.ui.table.date(item.schedule.open, "-", "date2l");
			me.ui.table.date(item.schedule.due, "-", "date2l");

			if (item.summary.avgCount > 0)
			{
				me.ui.table.text(item.summary.avgCount, null, {width:80});
				me.ui.table.text(asPct(item.summary.avgScore, item.design.points), null, {width:80});
			}
			else
			{
				me.ui.table.text("-", null, {width:80});
				me.ui.table.text("-", null, {width:80});
			}

			me.ui.table.text(item.design.points, null, {width:80});
		});

		me.ui.table.done();

		show(me.ui.evaluation_overview_none, me.itemsSorted.length == 0);
	};

	this.applyOrder = function(order)
	{
		main.ui.modebar.enableSaveDiscard(me.saveCancel);
		hide($("div[hideOnReorder=1]"));

		// build reorder data
		me.reorder = [];
		var category = null;
		for (var index = 0; index < order.length; index++)
		{
			// for a category
			if (order[index].startsWith("C:"))
			{
				category = order[index].substring(2);
			}
			
			// an item
			else
			{
				me.reorder.push({itemId: order[index].substring(2), categoryId: category});
			}
		}
	};

	this.saveCancel = function(mode, deferred)
	{
		if (mode)
		{
			me.save(deferred);
		}
		else
		{
			me.reorder = null;
			main.ui.modebar.enableSaveDiscard(null);
			me.populate();
			if (deferred !== undefined) deferred();
		}
	};
	
	this.checkExit = function(deferred)
	{
		if (me.reorder != null)
		{
			main.portal.confirmNavigationWithChanges(function()
			{
				me.save(deferred);				
			}, function()
			{
				me.reorder = null;
				main.ui.modebar.enableSaveDiscard(null);
				if (deferred !== undefined) deferred();
			});

			return false;
		}

		return true;
	};

	this.save = function(deferred)
	{
		var params = main.portal.cdp.params();
		params.url.site = main.portal.site.id;
		
		new e3_Edit({order: me.reorder}, []).params("", params);
		main.portal.cdp.request("evaluation_itemsReorder evaluation_gradingItems evaluation_categories", params, function(data)
		{
			me.items = data.items || [];
			me.summary = data.summary || {};
			main.categories = data.categories || [];
			me.reorder = null;
			main.ui.modebar.enableSaveDiscard(null);
			me.sortBy(me.sortDirection, me.sortMode);
			me.populate();
			if (deferred !== undefined) deferred();
		});
	};
}

function Evaluation_itemGrade(main)
{
	var me = this;

	this.ui = null;
	this.members = [];
	this.membersSorted = [];
	this.item = {};
	this.items = [];
	this.sortDirection = "A";
	this.sortMode = "S";
	this.sectionFilter = null;
	this.exportURL = null;

	this.filterOutCategories = function(items)
	{
		var rv = [];
		$.each(items, function(index, item)
		{
			if (item.toolItem !== undefined) rv.push(item);
		});
		
		return rv;
	};

	this.itemPosition = function()
	{
		for (index = 0; index < me.items.length; index++)
		{
			if ((me.items[index].toolItem.tool == me.item.toolItem.tool) && (me.items[index].toolItem.itemId == me.item.toolItem.itemId))
			{
				var rv = {};
				rv.item = index+1;
				rv.total = me.items.length;
				rv.prev = index > 0 ? me.items[index-1] : null;
				rv.next = index < me.items.length-1 ? me.items[index+1] : null;
				
				return rv;
			}			
		}
	};

	this.init = function()
	{
		me.ui = findElements(["evaluation_itemGrade_title", "evaluation_itemGrade_points", "evaluation_itemGrade_status", "evaluation_itemGrade_actions",
		                      "evaluation_itemGrade_table", "evaluation_itemGrade_none",
                              "evaluation_itemGrade_view", "evaluation_itemGrade_export"]);
		me.ui.table = new e3_Table(me.ui.evaluation_itemGrade_table);
		me.ui.itemNav = new e3_ItemNav();

//		onClick(me.ui.evaluation_itemGrade_export, function(){me.exportCsv();});
		setupHoverControls([me.ui.evaluation_itemGrade_export]);
//		onChange(me.ui.evaluation_itemGrade_view, function(){me.filterBySection(me.ui.evaluation_itemGrade_view.val());});
	};

	this.start = function(item, items)
	{
		if (item === null) return;

		if (item !== undefined) me.item = item;
		if (items !== undefined) me.items = me.filterOutCategories(items);

		me.ui.itemNav.inject(main.ui.evaluation_itemnav, {returnFunction:main.startOverview, pos:me.itemPosition(), navigateFunction:me.start});

		me.load();	
	};

	this.load = function()
	{
		var params = main.portal.cdp.params();
		params.url.site = main.portal.site.id;
		params.url.tool = me.item.toolItem.tool;
		params.url.item = me.item.toolItem.itemId;
		main.portal.cdp.request("evaluation_itemMembers", params, function(data)
		{
			me.members = data.members || [];
			me.sectionFilter = null;
			me.exportURL = data.exportURL;

			main.extractSections(me.members);

			me.populateFilters();
			
			me.sortBy(me.sortDirection, me.sortMode);
			me.populate();
		});
	};

	this.populateFilters = function()
	{
		me.ui.evaluation_itemGrade_actions.empty();

		// sections
		var options = [{value: "*", title: main.i18n.lookup("msg_allSections", "All")}];
		$.each(main.sections, function(index, section)
		{
			options.push({value: section, title: section});
		});
		me.ui.section = new e3_SortAction();
		me.ui.section.inject(me.ui.evaluation_itemGrade_actions,
				{onSort: function(dir, val){me.filterBySection(val);}, label: main.i18n.lookup("header_section", "SECTION"), options: options, initial: ((me.sectionFilter == null) ? "*" : me.sectionFilter)});

		// sort
		me.ui.sort = new e3_SortAction();
		me.ui.sort.inject(me.ui.evaluation_itemGrade_actions,
				{onSort: me.onSort, initial: me.sortMode,
					options:[
	                         {value:"N", title:main.i18n.lookup("sort_name", "Name")},
	                         {value:"R", title:main.i18n.lookup("sort_section", "Section")},
	                         {value:"S", title:main.i18n.lookup("sort_status", "Status")},
	                         {value:"F", title:main.i18n.lookup("sort_finished", "Finished")},
	                         {value:"V", title:main.i18n.lookup("sort_reviewed", "Reviewed")},
	                         {value:"E", title:main.i18n.lookup("sort_score", "Score")}
	                         ]
				});
	};

	this.populate = function()
	{
		me.ui.table.clear();
		$.each(me.membersSorted, function(index, member)
		{
			if ((me.sectionFilter != null) && (member.rosterName != me.sectionFilter)) return;
			
			me.ui.table.row();

			var nameDisplay = null;
			if (member.iid == null)
			{
				nameDisplay = member.nameSort;
			}
			else
			{
				nameDisplay = main.i18n.lookup("msg_memberName", "%0<div style='font-size:9px;color:#ADADAD;'>ID: %1</div>", "html",
					[member.nameSort, member.iid]);
			}
			me.ui.table.html(nameDisplay, null, {width: "calc(100vw - 100px - 574px)", minWidth: "calc(1200px - 100px - 574px)"});
			
			// section
			if (member.adhoc)
			{
				me.ui.table.text("-", null, {width:80});
			}
			else
			{
				me.ui.table.text(member.rosterName, null, {width:80});
			}

			// status
			me.ui.table.text(main.memberStatus(member), "e3_text special light", {fontSize:11, width:80, textTransform: "uppercase"});
			// main.memberStatusDotTd(member, me.ui.table);

			if (member.evaluation != null)
			{
				me.ui.table.date(member.evaluation.submittedOn, "-", "date2l");
				me.ui.table.date(member.evaluation.reviewedOn, "-", "date2l");
				
				var td = me.ui.table.text("", null, {width:150});
				main.evaluation.reviewLink.set(td, me.item.design, member.evaluation, function(){main.startItemGradeReview(member, me.membersSorted, me.item, me.items);});
			}
			else
			{
				me.ui.table.text("-", "date2l");
				me.ui.table.text("-", "date2l");
				me.ui.table.text("-", null, {width:150});
			}
		});
		me.ui.table.done();
		show(me.ui.evaluation_itemGrade_none, me.ui.table.rowCount() == 0);

		me.ui.evaluation_itemGrade_title.text(me.item.title);
		me.ui.evaluation_itemGrade_points.text(((me.item.design.actualPoints == null) ? "-" : me.item.design.points)); //main.i18n.lookup("msg_itemPoints", "%0 points", "html", [((me.item.design.actualPoints == null) ? "-" : me.item.design.points)]));

		var msg = null;
		var color = null;
		if (me.item.schedule.status == ScheduleStatus.closed)
		{
			if (me.item.schedule.close !== undefined)
			{
				msg = main.i18n.lookup("msg_closedOn", "closed on %0", "html", [main.portal.timestamp.display(me.item.schedule.close)]);
			}
			else
			{
				msg = main.i18n.lookup("msg_closed", "closed", "html");
			}
			color = Dots.red;
		}
		else if (me.item.schedule.status == ScheduleStatus.open)
		{
			if (me.item.schedule.close !== undefined)
			{
				msg = main.i18n.lookup("msg_openUntil", "open until %0", "html", [main.portal.timestamp.display(me.item.schedule.close)]);
			}
			else
			{
				msg = main.i18n.lookup("msg_open", "open");
			}
			color = Dots.green;
		}
		else
		{
			if (me.item.schedule.hide)
			{
				msg = main.i18n.lookup("msg_willOpenHiddenOn", "hidden until %0", "html", [main.portal.timestamp.display(me.item.schedule.open)]);
				color = Dots.gray;
			}
			else
			{
				msg = main.i18n.lookup("msg_willOpenOn", "will open on %0", "html", [main.portal.timestamp.display(me.item.schedule.open)]);
				color = Dots.yellow;
			}
		}
		me.ui.evaluation_itemGrade_status.text(msg);
		
		me.setExportUrl();
	};

	this.setExportUrl = function()
	{
		if (me.exportURL != null)
		{
			var url = main.i18n.lookup(null, me.exportURL, "html", [me.sortCode(), ((me.sortDirection == 'A') ? "asc" : "desc"), "All", ((me.sortDirection == 'A') ? "a" : "d")]);
			// TODO: need section (group) ID for old code... not just section name  ((me.sectionFilter == null) ? "All" : me.sectionFilter)
			me.ui.evaluation_itemGrade_export.attr("href", url);
		}
	};

	this.onSort = function(direction, option)
	{
		me.sortBy(direction, option);
		me.populate();
	};

	this.sortBy = function(direction, option)
	{
		me.sortDirection = direction;
		me.sortMode = option;
		me.ui.sort.directional(true);

		if (me.sortMode == "S")
		{
			me.membersSorted = main.sortMembersByStatus(me.members, direction);
		}
		else if (me.sortMode == "R")
		{
			me.membersSorted = main.sortMembersByRoster(me.members, direction);
		}
		else if (me.sortMode == "N")
		{
			me.membersSorted = main.sortMembersByName(me.members, direction);
		}
		else if (me.sortMode == "F")
		{
			me.membersSorted = main.sortMembersByEvalSubmittedOn(me.members, direction);
		}
		else if (me.sortMode == "V")
		{
			me.membersSorted = main.sortMembersByEvalReviewedOn(me.members, direction);
		}
		else if (me.sortMode == "E")
		{
			me.membersSorted = main.sortMembersByEvalScore(me.members, direction);		
		}
	};

	// for the non-serenity integration
	this.sortCode = function()
	{
		if (me.sortMode == "S") return "Status";
		if (me.sortMode == "R") return "Section";
		if (me.sortMode == "N") return "Name";
		if (me.sortMode == "F") return "Finished";
		if (me.sortMode == "V") return "Reviewed";
		
		return "Score";
	};

	this.exportCsv = function()
	{
	};
	
	this.filterBySection = function(section)
	{
		me.ui.section.directional(false);
		var filter = (section == "*") ? null : section;
		if (filter != me.sectionFilter)
		{
			me.sectionFilter = filter;
			me.populate();
		}
	};
}

function Evaluation_review(main)
{
	var me = this;

	this.ui = null;
	this.item = {};
	this.items = null;
	this.member = {};
	this.members = null;
	this.forMember = false;

	this.filterMembers = function(members)
	{
		if (members == null)
		{
			me.members = null;
			return;
		}

		me.members = [];
		$.each(members, function(index, member)
		{
			if (member.evaluation != null)
			{
				me.members.push(member);
			}
		});
	};

	this.memberPosition = function()
	{
		for (index = 0; index < me.members.length; index++)
		{
			if (me.members[index].userId == me.member.userId)
			{
				var rv = {};
				rv.item = index+1;
				rv.total = me.members.length;
				rv.prev = index > 0 ? me.members[index-1] : null;
				rv.next = index < me.members.length-1 ? me.members[index+1] : null;
				
				return rv;
			}			
		}
	};

	this.filterItems = function(items)
	{
		if (items == null)
		{
			me.items = null;
			return;
		}

		me.items = [];
		$.each(items, function(index, item)
		{
			if (item.evaluation != null)
			{
				me.items.push(item);
			}
		});
	};

	this.itemPosition = function()
	{
		for (index = 0; index < me.items.length; index++)
		{
			if ((me.items[index].toolItem.tool == me.item.toolItem.tool) && (me.items[index].toolItem.itemId == me.item.toolItem.itemId))
			{
				var rv = {};
				rv.item = index+1;
				rv.total = me.items.length;
				rv.prev = index > 0 ? me.items[index-1] : null;
				rv.next = index < me.items.length-1 ? me.items[index+1] : null;
				
				return rv;
			}
		}
	};

	this.position = function()
	{
		if (me.items != null) return me.itemPosition();
		return me.memberPosition();
	}

	this.init = function()
	{
		me.ui = findElements(["evaluation_review_review_ui", "evaluation_review_evaluation_ui",
		                      "evaluation_info_review_score", "evaluation_info_review_statusDot", "evaluation_info_review_title",
		                      "evaluation_info_review_memberInfo", "evaluation_info_review_memberDot", "evaluation_info_review_name", "evaluation_info_review_iid",
		                      "evaluation_info_review_started", "evaluation_info_review_finished"]);
		me.ui.itemNav = new e3_ItemNav();
	};

	this.start = function(item, items, member, members, forMember, onReturn)
	{
		me.item = item;
		me.filterItems(items);
		me.member = member;
		me.filterMembers(members);
		me.forMember = forMember;

		if (me.forMember)
		{
			me.load();
		}
		else
		{
			me.populate();
		}

		me.ui.itemNav.inject(main.ui.evaluation_itemnav, {returnFunction:onReturn, pos:me.position(), navigateFunction:me.start});
	};
	
	this.load = function()
	{
		var params = main.portal.cdp.params();
		params.url.site = main.portal.site.id;
		params.url.evaluation = me.item.evaluation.id;
		main.portal.cdp.request("evaluation_review", params, function(data)
		{
			if (data.evaluation != null) me.item.evaluation = data.evaluation;
			me.populate();
		});
	};

	this.populate = function()
	{		
		var evaluation = (me.member.evaluation !== undefined) ? me.member.evaluation : me.item.evaluation;

		me.ui.evaluation_info_review_score.text(asPct(evaluation.score, me.item.design.points));

		me.ui.evaluation_info_review_title.text(main.i18n.lookup("msg_titlePoints", "%0, %1 points", "html", [me.item.title, me.item.design.points]));

		var msg = null;
		var color = null;
		if (me.item.schedule.status == ScheduleStatus.closed)
		{
			if (me.item.schedule.close !== undefined)
			{
				msg = main.i18n.lookup("msg_closedOn", "closed on %0", "html", [main.portal.timestamp.display(me.item.schedule.close)]);
			}
			else
			{
				msg = main.i18n.lookup("msg_closed", "closed", "html");
			}
			color = Dots.red;
		}
		else if (me.item.schedule.status == ScheduleStatus.open)
		{
			if (me.item.schedule.close !== undefined)
			{
				msg = main.i18n.lookup("msg_openUntil", "open until %0", "html", [main.portal.timestamp.display(me.item.schedule.close)]);
			}
			else
			{
				msg = main.i18n.lookup("msg_open", "open");
			}
			color = Dots.green;
		}
		else
		{
			if (me.item.schedule.hide)
			{
				msg = main.i18n.lookup("msg_willOpenHiddenOn", "hidden until %0", "html", [main.portal.timestamp.display(me.item.schedule.open)]);
				color = Dots.gray;
			}
			else
			{
				msg = main.i18n.lookup("msg_willOpenOn", "will open on %0", "html", [main.portal.timestamp.display(me.item.schedule.open)]);
				color = Dots.yellow;
			}
		}
		me.ui.evaluation_info_review_statusDot.html(dotSmall(color, msg, true));

		if (!me.forMember)
		{
			me.ui.evaluation_info_review_memberDot.html(main.memberStatusDot(me.member));
			me.ui.evaluation_info_review_name.text(me.member.nameDisplay);
			me.ui.evaluation_info_review_iid.text(main.i18n.lookup("msg_memberIid", "ID: %0", "html", [me.member.iid]));
		}
		show([me.ui.evaluation_info_review_memberDot, me.ui.evaluation_info_review_name, me.ui.evaluation_info_review_iid], !me.forMember);

		// TODO: we don't know started (not in evaluation) me.ui.evaluation_info_review_started
		me.ui.evaluation_info_review_finished.text(main.i18n.lookup("msg_finished", "Finished: %0", "html", [main.portal.timestamp.display(evaluation.submittedOn)]));

		// TODO: set the work to review area, based on the tool item and submission
		// me.ui.evaluation_review_review_ui.text("ITEM TO REVIEW");

		main.evaluation.review.set(me.ui.evaluation_review_evaluation_ui, me.item.design, evaluation);
	};
}

function Evaluation_grade(main)
{
	var me = this;

	this.ui = null;
	this.members = [];
	this.membersSorted = [];
	this.summary = {};
	this.sectionFilter = null;
	this.sortDirection = "A";
	this.sortMode = "X";
	this.typeFilter = null;
	this.edit = null;
	this.exportURL = null;

	this.findMemberGradeEntry = function(id)
	{
		for (var i = 0; i < me.edit.members.length; i++)
		{
			if (me.edit.members[i].id == id) return me.edit.members[i];
		}

		return null;
	};

	this.init = function()
	{
		me.ui = findElements(["evaluation_grade_view", "evaluation_grade_export",
		                      "evaluation_grade_points", "evaluation_grade_points_ec", "evaluation_grade_avgPct", "evaluation_grade_showing", "evaluation_grade_lowDrops", "evaluation_grade_boosting",
		                      "evaluation_grade_table", "evaluation_grade_none",
		                      "evaluation_grade_template_score",
		                      "evaluation_actions_grade_actions",
		                      "evaluation_boostOptionDialog", "evaluation_boostOptionDialog_num",
		                      "evaluation_dropOptionDialog", "evaluation_dropOptionDialog_table", "evaluation_dropOptionDialog_dropActivate", "evaluation_dropOptionDialog_dropActivateInvalid",
		                      "evaluation_showingOptionDialog", "evaluation_showingOptionDialog_showLetterGrade",
//		                      "evaluation_grade_organize", "evaluation_grade_type", "evaluation_grade_view",
//		                      "evaluation_grade_title", "evaluation_grade_studentsSection", "evaluation_grade_points", "evaluation_grade_score"
		                      ]);
		me.ui.table = new e3_Table(me.ui.evaluation_grade_table);

		// onClick(me.ui.evaluation_grade_export, function(){me.exportCsv();});
		onClick(me.ui.evaluation_grade_boosting, me.startBoost);
		onClick(me.ui.evaluation_grade_showing, me.startShowing);
		onClick(me.ui.evaluation_grade_lowDrops, me.startDrop);
		setupHoverControls([me.ui.evaluation_grade_export/*, me.ui.evaluation_grade_boosting, me.ui.evaluation_grade_showing, me.ui.evaluation_grade_lowDrops*/]);

//		onChange(me.ui.evaluation_grade_view, function(){me.filterBySection(me.ui.evaluation_grade_view.val());});
//		onChange(me.ui.evaluation_grade_type, function(){me.filterByType(me.ui.evaluation_grade_type.val());});
//		onChange(me.ui.evaluation_grade_organize, function(){me.organizeBy(me.ui.evaluation_grade_organize.val());});
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
		main.portal.cdp.request("evaluation_gradingMembers", params, function(data)
		{
			me.members = data.members || [];
			me.summary = data.summary || {score:0, points:0};
			me.exportURL = data.exportURL;

			me.sectionFilter = null;
			main.extractSections(me.members);
			me.populateFilters();
//			me.ui.evaluation_grade_organize.val("C");
//			hide(me.ui.evaluation_grade_type);
//			me.ui.evaluation_grade_type.val("*");
			me.typeFilter = null;
			me.sortBy(me.sortDirection, me.sortMode);
			me.makeEdit();
			me.populate();
		});
	};

	this.makeEdit = function()
	{
		var memberGradeOverrides = {members:[]};
		$.each(me.members, function(index, member)
		{
			memberGradeOverrides.members.push({id: member.userId, grade: member.summary.gradeOverride});
		});
		me.edit = new e3_Edit(memberGradeOverrides, [], function(changed)
		{
			main.ui.modebar.enableSaveDiscard(changed ? me.saveCancel : null);
//			me.ui.itemNav.enableSave(changed ? me.saveCancel : null);
		});
		me.edit.setFilters({"grade": me.edit.stringFilter}, "members[]");

		main.ui.modebar.enableSaveDiscard(null);
//		me.ui.itemNav.enableSave(null);
	};

	this.populateFilters = function()
	{
		me.ui.evaluation_actions_grade_actions.empty();

		// sections
		var options = [{value: "*", title: main.i18n.lookup("msg_allSections", "All")}];
		$.each(main.sections, function(index, section)
		{
			options.push({value: section, title: section});
		});
		me.ui.section = new e3_SortAction();
		me.ui.section.inject(me.ui.evaluation_actions_grade_actions,
				{onSort: function(dir, val){me.filterBySection(val);}, label: main.i18n.lookup("header_section", "SECTION"), options: options, initial: ((me.sectionFilter == null) ? "*" : me.sectionFilter)});
		
		// sort
		me.ui.sort = new e3_SortAction();
		me.ui.sort.inject(me.ui.evaluation_actions_grade_actions,
				{onSort: me.onSort, initial: me.sortMode,
					options:[{value:"N", title: main.i18n.lookup("sort_name", "Name")},
					         {value:"R", title: main.i18n.lookup("sort_section", "Section")},
					         {value:"X", title: main.i18n.lookup("sort_status", "Status")},
					         {value:"E", title: main.i18n.lookup("sort_score", "Score")}]
				});
	};

	this.populate = function()
	{
		me.ui.table.clear();

		// collect to compute average based on section filter - don't include dropped members
		var allPoints = 0;
		var allScore = 0;
		var showAvg = false;

		$.each(me.membersSorted, function(index, member)
		{
			if ((me.sectionFilter != null) && (member.rosterName != me.sectionFilter)) return;

			var row = me.ui.table.row();
			
			var nameDisplay = null;
			if (member.iid == null)
			{
				nameDisplay = member.nameSort;
			}
			else
			{
				nameDisplay = main.i18n.lookup("msg_memberNameHot", "%0<div style='font-size:9px;color:#ADADAD;line-height:normal;position:relative;top:-8px;'>ID: %1</div>", "html",
					[member.nameSort, member.iid]);
			}
			me.ui.table.hotHtml(nameDisplay, main.i18n.lookup("msg_viewMember", "view grades"), function()
			{
				main.startMemberGrade(member, me.membersSorted);
			}, null, {width: "calc(100vw - 100px - 476px)", minWidth:"calc(1200px - 100px - 476px)"});

			// section
			if ((member.adhoc) || (member.rosterName == null))
			{
				me.ui.table.text("-", null, {width:80});
			}
			else
			{
				me.ui.table.text(member.rosterName, null, {width:80});
			}

			// status
			me.ui.table.text(main.memberStatus(member), "e3_text special light", {fontSize:11, width:80, textTransform: "uppercase"});
			// main.memberStatusDotTd(member, me.ui.table);

			if (member.summary.items > 0)
			{
				var cell = clone(me.ui.evaluation_grade_template_score, ["evaluation_grade_template_score_grade", "evaluation_grade_template_score_pct",
				                                                         "evaluation_grade_template_score_score", "evaluation_grade_template_score_points"]);
				me.ui.table.element(cell.element, null, {width:120});
				show(cell.element);
				cell.evaluation_grade_template_score_grade.text(member.summary.grade);
				cell.evaluation_grade_template_score_pct.text(asPct(member.summary.score, member.summary.points));
				cell.evaluation_grade_template_score_score.text(member.summary.score);
				cell.evaluation_grade_template_score_points.text(member.summary.points);

				if (member.active)
				{
					allPoints += member.summary.points;
					allScore += member.summary.score;
					showAvg = true;
				}
			}
			else
			{
				me.ui.table.text("-", null, {width:120});
			}

			// final grade only for active members
			if (member.active)
			{
				var input = me.ui.table.input({size: 2, type: "text"}, null, {width: 100}).find("input");
				input.css({fontSize: 13, width: 32});
				var inTheEdit = me.findMemberGradeEntry(member.userId);
				me.edit.setupFilteredEdit(input, inTheEdit, "grade");
			}
			else
			{
				me.ui.table.text("", null, {width:100});
			}
		});

		me.ui.table.done();

		show(me.ui.evaluation_grade_none, me.ui.table.rowCount() == 0);

		me.ui.evaluation_grade_points.text(me.summary.points);
		me.ui.evaluation_grade_points_ec.text(me.summary.points + me.summary.extraPoints);
		me.ui.evaluation_grade_avgPct.text((showAvg ? asPct(allScore, allPoints) : "-"));
		
		// all work or only released
		var showingMsg = null;
		if (main.options.includeAll)
		{
			showingMsg = main.i18n.lookup("msg_showingAll", "grade includes <span style='font-weight:700'>all work</span>");
		}
		else
		{
			showingMsg = main.i18n.lookup("msg_showingReleased", "grade includes <span style='font-weight:700'>only released work</span>");			
		}
		me.ui.evaluation_grade_showing.html(showingMsg);

		var numDrop = main.numberToDrop();

		if (main.options.dropLowestActive)
		{
			if (numDrop == 1)
			{
				me.ui.evaluation_grade_lowDrops.html(main.i18n.lookup("msg_haveDropped1", "<span style='font-weight:700'>1 low score</span> has been dropped"));
			}
			else
			{
				me.ui.evaluation_grade_lowDrops.html(main.i18n.lookup("msg_haveDropped", "<span style='font-weight:700'>%0 low scores</span> have been dropped", "html", [numDrop]));
			}
		}
		else
		{
			if (numDrop == 1)
			{
				me.ui.evaluation_grade_lowDrops.html(main.i18n.lookup("msg_willBeDropped1", "<span style='font-weight:700'>1 low score</span> will be dropped"));		
			}
			else
			{
				me.ui.evaluation_grade_lowDrops.html(main.i18n.lookup("msg_willBeDropped", "<span style='font-weight:700'>%0 low scores</span> will be dropped", "html", [numDrop]));
			}
		}

		if (main.options.boostBy != 0)
		{
			me.ui.evaluation_grade_boosting.html(main.i18n.lookup("msg_boost", "grades are <span style='font-weight:700'>boosted by %0 %1</span>", "html",
					[main.options.boostBy, main.i18n.lookup(((main.options.boostBy == 1) ? "msg_boostType1_" :  "msg_boostType_") + main.options.boostType)]));
		}
		else
		{
			me.ui.evaluation_grade_boosting.html(main.i18n.lookup("msg_noBoost", "grades are not boosted"));
		}

//		show(me.ui.evaluation_grade_studentsSection, (me.sectionFilter != null));
//		if (me.sectionFilter != null)
//		{
//			me.ui.evaluation_grade_studentsSection.text(main.i18n.lookup("msg_studentsInSection", "(section %0)", "html", [me.sectionFilter]));
//		}
		
		me.setExportUrl();
	};

	this.setExportUrl = function()
	{
		if (me.exportURL != null)
		{
			var url = main.i18n.lookup(null, me.exportURL, "html", [me.sortCode(), ((me.sortDirection == 'A') ? "asc" : "desc"), "All"]);
			// TODO: need section (group) ID for old code... not just section name  ((me.sectionFilter == null) ? "All" : me.sectionFilter)
			me.ui.evaluation_grade_export.attr("href", url);
		}
	};

	this.saveCancel = function(mode, deferred)
	{
		if (mode)
		{
			me.save(deferred);
		}
		else
		{
			me.edit.revert();
			me.populate();
			if (deferred !== undefined) deferred();
		}
	};
	
	this.checkExit = function(deferred)
	{
		if (me.edit.changed())
		{
			main.portal.confirmNavigationWithChanges(function()
			{
				me.save(deferred);				
			}, function()
			{
				me.edit.revert();
				// main.ui.modebar.enableSaveDiscard(null);
				if (deferred !== undefined) deferred();
			});

			return false;
		}

		return true;
	};

	this.save = function(deferred)
	{
		var params = main.portal.cdp.params();
		params.url.site = main.portal.site.id;
		me.edit.params("", params);
		main.portal.cdp.request("evaluation_gradesSave evaluation_gradingMembers", params, function(data)
		{
			me.members = data.members || [];
			me.summary = data.summary || {score:0, points:0};
//			me.sectionFilter = null;
//			main.extractSections(me.members);
//			me.populateFilters();
			me.sortBy(me.sortDirection, me.sortMode);
			me.makeEdit();
			me.populate();
			if (deferred !== undefined) deferred();
		});
	};

	this.exportCsv = function()
	{
	};

	this.onSort = function(direction, option)
	{
		me.sortBy(direction, option);
		me.populate();
	};

	this.sortBy = function(direction, option)
	{
		me.sortDirection = direction;
		me.sortMode = option;
		me.ui.sort.directional(true);

		//  X-status R-roster(section) N-name E-eval(score)
		if (me.sortMode == "X")
		{
			me.membersSorted = main.sortMembersByStatus(me.members, direction);
		}
		else if (me.sortMode == "R")
		{
			me.membersSorted = main.sortMembersByRoster(me.members, direction);
		}
		else if (me.sortMode == "N")
		{
			me.membersSorted = main.sortMembersByName(me.members, direction);
		}
		else if (me.sortMode == "E")
		{
			me.membersSorted = main.sortMembersBySummaryScore(me.members, direction);
		}
	};

	// for the non-serenity integration
	this.sortCode = function()
	{
		if (me.sortMode == "X") return "Status";
		if (me.sortMode == "R") return "Section";
		if (me.sortMode == "N") return "Name";
		return "Score";
	};

	this.filterBySection = function(section)
	{
		me.ui.section.directional(false);
		var filter = (section == "*") ? null : section;
		if (filter != me.sectionFilter)
		{
			me.sectionFilter = filter;
			me.populate();
		}
	};

	this.organizeBy = function(x)
	{
		if (x == "I")
		{
			me.ui.evaluation_grade_type.val("*");
			me.typeFilter = null;
		}
		show(me.ui.evaluation_grade_type, x == "I");
	};
	
	this.filterByType = function(type)
	{
		me.typeFilter = (type == "*") ? null : type;
	};
	
	this.startBoost = function()
	{
		var edit = new e3_Edit({options: main.options}, [], function(changed){});
		edit.setFilters({"boostBy": edit.numberFilter}, "options.");
		edit.setupFilteredEdit(me.ui.evaluation_boostOptionDialog_num, edit.options, "boostBy");
		edit.setupRadioEdit("evaluation_boostOptionDialog_by", edit.options, "boostType");

		main.portal.dialogs.openDialog(me.ui.evaluation_boostOptionDialog, main.i18n.lookup("action_save", "SAVE"), function()
		{
			var params = main.portal.cdp.params();
			params.url.site = main.portal.site.id;
			edit.params("", params);
			main.portal.cdp.request("evaluation_optionsSave evaluation_options evaluation_gradingMembers", params, function(data)
			{
				main.options = data.options;
				me.members = data.members || [];
				me.summary = data.summary || {score:0, points:0};
				me.sortBy(me.sortDirection, me.sortMode);
				me.makeEdit();
				me.populate();
			});
			return true;
		});
	};
	
	this.startDrop = function()
	{
		var edit = new e3_Edit({options: main.options, categories:main.categories}, [], function(changed){});

		me.ui.dropTable = new e3_Table(me.ui.evaluation_dropOptionDialog_table);
		me.ui.dropTable.clear();
		$.each(edit.categories, function(index, category)
		{
			var cat = main.findCategory(category.id);

			var row = me.ui.dropTable.row();

			var td = me.ui.dropTable.input({size: "3", type: "number", step: "1", min: "0"}, null, {width: 70});
			td.css({padding: 0});
			var inp = td.find("input");
			inp.val(category.drop);
			inp.attr("catId", category.id);
			edit.setupFilteredEdit(inp, category, "drop", function()
			{
				me.checkDropValidity(edit);
			});
			me.ui.dropTable.text(cat.title, null, {width: 304});
			if (cat.numItems == 1)
			{
				me.ui.dropTable.text(main.i18n.lookup("msg_dropFromCatCount1", "has 1 item"), null, {width: 92});
			}
			else
			{
				me.ui.dropTable.text(main.i18n.lookup("msg_dropFromCatCount", "has %1 items", "html", [cat.numItems]), null, {width: 92});
			}
		});
		me.ui.dropTable.done();

		edit.setupCheckEdit(me.ui.evaluation_dropOptionDialog_dropActivate, edit.options, "dropLowestActive", function()
		{
			me.checkDropValidity(edit);
		});
		me.checkDropValidity(edit);

		main.portal.dialogs.openDialog(me.ui.evaluation_dropOptionDialog, main.i18n.lookup("action_save", "SAVE"), function()
		{
			var params = main.portal.cdp.params();
			params.url.site = main.portal.site.id;
			edit.params("", params);
			main.portal.cdp.request("evaluation_optionsSave evaluation_options evaluation_categories evaluation_gradingMembers", params, function(data)
			{
				main.options = data.options;
				main.categories = data.categories || [];
				me.members = data.members || [];
				me.summary = data.summary || {score:0, points:0};
				me.sortBy(me.sortDirection, me.sortMode);
				me.makeEdit();
				me.populate();
			});
			return true;
		});

		me.ui.dropTable = null;
	};
	
	this.checkDropValidity = function(edit)
	{
		var valid = true;
		
		$.each(edit.categories, function(index, category)
		{
			if (category.numItems < category.drop)
			{
				valid = false;
				$("input[catId=" + category.id + "]").css({color: "red"});
			}
			else
			{
				$("input[catId=" + category.id + "]").css({color: ""});
			}
		});

		show(me.ui.evaluation_dropOptionDialog_dropActivateInvalid, !valid);
	};

	this.startShowing = function()
	{
		var edit = new e3_Edit({options: main.options}, [], function(changed){});
		edit.setupRadioEdit("evaluation_showingOptionDialog_grade", edit.options, "includeAll");
		edit.setupCheckEdit(me.ui.evaluation_showingOptionDialog_showLetterGrade, edit.options, "showLetterGrades");
		
		main.portal.dialogs.openDialog(me.ui.evaluation_showingOptionDialog, main.i18n.lookup("action_save", "SAVE"), function()
		{
			var params = main.portal.cdp.params();
			params.url.site = main.portal.site.id;
			edit.params("", params);
			main.portal.cdp.request("evaluation_optionsSave evaluation_options evaluation_gradingMembers", params, function(data)
			{
				main.options = data.options;
				me.members = data.members || [];
				me.summary = data.summary || {score:0, points:0};
				me.sortBy(me.sortDirection, me.sortMode);
				me.makeEdit();
				me.populate();
			});
			return true;
		});
	};
}

function Evaluation_memberGrade(main)
{
	var me = this;

	this.member = {};
	this.members = null;
	this.items = [];
	this.itemsSorted = [];
	this.showCategories = true;
	this.ui = null;
	this.forMember = false;
	this.exportURL = null;

	this.memberPosition = function()
	{
		for (index = 0; index < me.members.length; index++)
		{
			if (me.members[index].userId == me.member.userId)
			{
				var rv = {};
				rv.item = index+1;
				rv.total = me.members.length;
				rv.prev = index > 0 ? me.members[index-1] : null;
				rv.next = index < me.members.length-1 ? me.members[index+1] : null;
				
				return rv;
			}			
		}
	};

	this.init = function()
	{
		me.ui = findElements(["evaluation_memberGrade_actions", "evaluation_memberGrade_export",
		                      "evaluation_memberGrade_member", "evaluation_memberGrade_name", "evaluation_memberGrade_iid", "evaluation_memberGrade_status",
		                      "evaluation_memberGrade_bkg", "evaluation_memberGrade_header", "evaluation_memberGrade_grade", "evaluation_memberGrade_score", "evaluation_memberGrade_showing",
		                      "evaluation_memberGrade_pctComplete", "evaluation_memberGrade_possiblePoints", "evaluation_memberGrade_pctCompleteBlock",
		                      "evaluation_memberGrade_table", "evaluation_memberGrade_none"]);
		me.ui.table = new e3_Table(me.ui.evaluation_memberGrade_table);
		me.ui.itemNav = new e3_ItemNav();
		
		setupHoverControls([me.ui.evaluation_memberGrade_export]);
		// onClick(me.ui.evaluation_memberGrade_export, function(){me.exportCsv();});
		onChange(me.ui.evaluation_memberGrade_sort, function(){me.sortBy(me.ui.evaluation_memberGrade_sort.val());});
	};

	this.start = function(member, members)
	{
		if (member !== undefined) me.member = member;
		if (members !== undefined) me.members = members;

		me.forMember = (me.members == null);

		me.load();
		
		if (!me.forMember)
		{
			me.ui.itemNav.inject(main.ui.evaluation_itemnav, {returnFunction:main.startGrade, pos:me.memberPosition(), navigateFunction:me.start});
		}
		show(me.ui.evaluation_memberGrade_export, (!me.forMember));
	};

	this.load = function()
	{
		var params = main.portal.cdp.params();
		params.url.site = main.portal.site.id;
		if (!me.forMember) params.url.user = me.member.userId;
		var request = "evaluation_memberItems evaluation_categories";
		if (main.options == null) request += " evaluation_options";
		main.portal.cdp.request(request, params, function(data)
		{
			if (data.options !== undefined) main.options = data.options;
			me.items = data.items || [];
			me.member = data.member || {};
			main.categories = data.categories || [];
			me.exportURL = data.exportURL;

			me.itemsSorted = main.sortByCategory(me.items, false);

			me.populate();
		});
	};

	this.populate = function()
	{
		if (!me.forMember)
		{
			me.ui.evaluation_memberGrade_name.text(me.member.nameDisplay);
			me.ui.evaluation_memberGrade_status.text(main.memberStatus(me.member));
			me.ui.evaluation_memberGrade_iid.text(main.i18n.lookup("msg_memberId", "ID: %0", "html", [me.member.iid]));
		}
		show([me.ui.evaluation_memberGrade_member, me.ui.evaluation_memberGrade_export], (!me.forMember));

		var color = "";
		if (me.member.summary.points > 0)
		{
			me.ui.evaluation_memberGrade_score.html(main.i18n.lookup("msg_scoreOf", "", "html",
					[asPct(me.member.summary.score, me.member.summary.points), me.member.summary.score, me.member.summary.points]));
			me.ui.evaluation_memberGrade_grade.text(me.member.summary.grade);

			var color = rgColor(me.member.summary.score / me.member.summary.points) /*main.colorsLight[0]*/;
//			if ((me.member.summary.score / me.member.summary.points) >= 0.8)
//			{
//				color = main.colorsLight[2];
//			}
//			else if ((me.member.summary.score / me.member.summary.points) >= 0.87)
//			{
//				color = main.colorsLight[1];
//			}
		}
		else
		{
			me.ui.evaluation_memberGrade_score.html(main.i18n.lookup("msg_noScore", "SCORE: -"));
			me.ui.evaluation_memberGrade_grade.text("-");
		}
		
		if (main.options.includeAll)
		{
			me.ui.evaluation_memberGrade_header.text(main.i18n.lookup("header_gradeTotal", "TOTAL GRADE"));
			me.ui.evaluation_memberGrade_showing.html(main.i18n.lookup("msg_showingAll", "grade includes <span style='font-weight:700'>all work</span>"));
		}
		else
		{
			me.ui.evaluation_memberGrade_header.text(main.i18n.lookup("header_gradeToDate", "GRADE TO DATE"));
			me.ui.evaluation_memberGrade_showing.html(main.i18n.lookup("msg_showingReleased", "grade includes <span style='font-weight:700'>only released work</span>"));
		}
		me.ui.evaluation_memberGrade_bkg.css({backgroundColor: color});

		if (!main.options.includeAll)
		{
			me.ui.evaluation_memberGrade_pctComplete.text(asPct(me.member.summary.points, me.member.summary.allPoints));
		}
		show(me.ui.evaluation_memberGrade_pctCompleteBlock, (!main.options.includeAll));

		me.ui.evaluation_memberGrade_possiblePoints.text(me.member.summary.allPoints);

		me.ui.table.clear();
		$.each(me.itemsSorted, function(index, item)
		{
			me.ui.table.row();

			if (me.showCategories)
			{
				// insert an in-table heading if we are at the start of a new category
				if (((index > 0) && (me.itemsSorted[index-1].design.category != item.design.category)) || (index == 0))
				{
					if (item.design.category != 0)
					{
						var cat = main.findCategory(item.design.category);
						if (cat != null)
						{
							var msg = main.i18n.lookup("msg_catWorth", "Worth: %0 (%1 of %2)", "html", [asPct(cat.points, me.member.summary.allPoints), cat.points, me.member.summary.allPoints]);
							var td = me.ui.table.headerRow(cat.title);
							td.find("div").append($("<div />").css({display: "inline-block", fontSize: 12, textTransform: "none", marginLeft: 32}).text(msg));
							
							me.ui.table.row();
						}
					}
				}
			}

			// main.itemStatusDotTd(item, me.ui.table);
			me.ui.table.text(item.title, null, {width: "calc(100vw - 100px - 730px)", minWidth: "calc(1200px - 100px - 730px)"});
			main.typeTd(item, me.ui.table);
			me.ui.table.date(item.schedule.open, "-", "date2l");
			me.ui.table.date(item.schedule.due, "-", "date2l");
			me.ui.table.text(item.design.points, null, {width:80});

			if (item.evaluation != null)
			{
				me.ui.table.date(item.evaluation.submittedOn, "-", "date2l");
				me.ui.table.date(item.evaluation.reviewedOn, "-", "date2l");

				var td = me.ui.table.text("", null, {width:150});
				main.evaluation.reviewLink.set(td, item.design, item.evaluation, function(){main.startMemberGradeReview(item, me.itemsSorted, me.member, me.members, me.forMember);});
			}
			else
			{
				me.ui.table.text("-", "date2l");
				me.ui.table.text("-", "date2l");
				me.ui.table.text("-", null, {width:150});
			}
		});
		me.ui.table.done();

		show(me.ui.evaluation_memberGrade_none, (me.ui.table.rowCount() == 0));
		
		if ((me.exportURL != null) && (!me.forMember))
		{
			me.ui.evaluation_memberGrade_export.attr("href", me.exportURL);
		}
	};
}

function Evaluation_option(main)
{
	var me = this;

	this.ui = null;
	this.edit = null;

	this.init = function()
	{
		me.ui = findElements(["evaluation_options_showLetterGrade", "evaluation_options_drop_table", "evaluation_options_dropActivate", "evaluation_options_dropActivateInvalid",
		                      "evaluation_options_gradeScale", "evaluation_options_gradeScale_table", "evaluation_grade_boost_num", "evaluation_options_scaleChangeInvalid"]);
		me.ui.dropTable = new e3_Table(me.ui.evaluation_options_drop_table);
		me.ui.scaleTable = new e3_Table(me.ui.evaluation_options_gradeScale_table);
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

		main.portal.cdp.request("evaluation_options evaluation_categories", params, function(data)
		{
			main.options = data.options;
			main.categories = data.categories || [];
			me.makeEdit(main.options);
			me.populate();
		});
	};
	
	this.makeEdit = function(options)
	{
		me.edit = new e3_Edit({options:options, categories:main.categories},
				["categories[].modifiedOn", "categories[].title", "categories[].createdBy", "categories[].order", "categories[].modifiedBy", "categories[].createdOn", "categories[].type", "options.gradingScaleMutable"],
				function(changed)
				{
					main.ui.modebar.enableSaveDiscard(changed ? me.saveCancel : null);
				});
		main.ui.modebar.enableSaveDiscard(null);
		me.edit.setFilters({"dropLowestActive": me.edit.booleanFilter, "includeAll": me.edit.booleanFilter, "showLetterGrades": me.edit.booleanFilter, "boostBy": me.edit.numberFilter}, "options.");
	};

	this.populate = function()
	{
		me.edit.setupRadioEdit("evaluation_options_grade", me.edit.options, "includeAll");
		me.edit.setupCheckEdit(me.ui.evaluation_options_showLetterGrade, me.edit.options, "showLetterGrades");

		me.edit.setupFilteredEdit(me.ui.evaluation_grade_boost_num, me.edit.options, "boostBy");
		me.edit.setupRadioEdit("evaluation_grade_boost_by", me.edit.options, "boostType");

		me.edit.setupSelectEdit(me.ui.evaluation_options_gradeScale, null, me.edit.options, "gradingScale", me.adjustGradingScale);

		me.ui.dropTable.clear();
		$.each(me.edit.categories, function(index, category)
		{
			// <!-- in 524, 24 [70 x+8 92+8] 24 -->
			var cat = main.findCategory(category.id);

			var row = me.ui.dropTable.row();

			var td = me.ui.dropTable.input({size: "3", type: "number", step: "1", min: "0"}, null, {width: 70});
			td.css({paddingLeft: 0});
			var inp = td.find("input");
			inp.val(category.drop);
			inp.attr("catId", category.id);
			me.edit.setupFilteredEdit(inp, category, "drop", function(){me.checkDropValidity();});

			me.ui.dropTable.text(cat.title, null, {width: 298});

			if (cat.numItems == 1)
			{
				td = me.ui.dropTable.text(main.i18n.lookup("msg_dropFromCatCount1", "has 1 item"), null, {width: 92});
			}
			else
			{
				td = me.ui.dropTable.text(main.i18n.lookup("msg_dropFromCatCount", "has %1 items", "html", [cat.numItems]), null, {width: 92});
			}
			td.css({paddingRight: 0});
		});
		me.ui.dropTable.done();
		me.edit.setupCheckEdit(me.ui.evaluation_options_dropActivate, me.edit.options, "dropLowestActive", function(){me.checkDropValidity();});
		me.checkDropValidity();

		me.populateGradingScale();
	};
	
	this.adjustGradingScale = function(val, finalChange)
	{
		if (!finalChange) return;
		
		me.populateGradingScale();
	};

	this.populateGradingScale = function()
	{
		// if there are grade overrides, we cannot change scales
		show(me.ui.evaluation_options_scaleChangeInvalid, (!main.options.gradingScaleMutable));

		me.ui.scaleTable.clear();
		$.each(me.edit.options["gradingScaleThresholds_" + me.edit.options.gradingScale], function(index, gradeThreshold)
		{
			var row = me.ui.scaleTable.row();
			
			if ((gradeThreshold.grade != "F") && (gradeThreshold.grade != "I") && (gradeThreshold.grade != "NP"))
			{
				// var td = me.ui.scaleTable.input({size: "3", type: "number", step: ".01", min: "0", max: "100"}, null, {width: 70});
				var td = me.ui.scaleTable.input({size: "3", type: "text"}, null, {width: 70});
				me.edit.setupFilteredEdit(td.find("input"), gradeThreshold, "thresholdPct");
				td.css({paddingLeft: 0});
			}
			else
			{
				var td = null;
				if (gradeThreshold.grade != "I")
				{
					td = me.ui.scaleTable.text("0", null, {width: 58});
				}
				else
				{
					td = me.ui.scaleTable.text("-", null, {width: 58});
				}
				td.css({paddingLeft: 12});
			}

			// <!-- in 524, 24 [70 x+8] 24 -->
			var td = me.ui.scaleTable.text(gradeThreshold.grade, null, {width: 398});
			td.css({paddingRight: 0});
		});
	}

	this.checkDropValidity = function()
	{
		var valid = true;
		
		$.each(me.edit.categories, function(index, category)
		{
			if (category.numItems < category.drop)
			{
				valid = false;
				$("input[catId=" + category.id + "]").css({color: "red"});
			}
			else
			{
				$("input[catId=" + category.id + "]").css({color: ""});
			}
		});

		show(me.ui.evaluation_options_dropActivateInvalid, !valid);
	};

	this.saveCancel = function(mode, deferred)
	{
		if (mode)
		{
			me.save(deferred);
		}
		else
		{
			me.edit.revert();
			me.populate();
			if (deferred !== undefined) deferred();
		}
	};

	this.checkExit = function(deferred)
	{
		if (me.edit.changed())
		{
			main.portal.confirmNavigationWithChanges(function()
			{
				me.save(deferred);				
			}, function()
			{
				me.edit.revert();
				if (deferred !== undefined) deferred();			
			});

			return false;
		}

		return true;
	};

	this.save = function(deferred)
	{
		var params = main.portal.cdp.params();
		params.url.site = main.portal.site.id;
		me.edit.params("", params);
		main.portal.cdp.request("evaluation_optionsSave evaluation_options evaluation_categories", params, function(data)
		{
			main.options = data.options;
			main.categories = data.categories || [];
			me.makeEdit(main.options);
			me.populate();
			if (deferred !== undefined) deferred();
		});
	};
}

function Evaluation_category(main)
{
	var me = this;

	this.ui = null;
	this.edit = null;
	this.summary = null;
	this.stdCategories = null;

	// TODO: not used
	this.categoryWeightingsEnabled = function(categories)
	{
		for (var i = 0; i < categories.length; i++)
		{
			if (categories[i].weight != null) return true;
		}
		return false;
	};

	this.init = function()
	{
		me.ui = findElements(["evaluation_category_controls", "evaluation_category_add", "evaluation_category_delete", "evaluation_category_actions",
		                      "evaluation_bar_total_header", "evaluation_bar_total", "evaluation_bar_ec",
		                      "evaluation_category_table", "evaluation_category_none", "evaluation_category_weightSummary", "evaluation_category_weightsInvalid",
		                      "evaluation_fixWeights"]);
		me.ui.table = new e3_Table(me.ui.evaluation_category_table);
		me.ui.table.enableReorder(me.applyCategoryOrder);
		me.ui.table.setupSelection("evaluation_category_table_select", me.updateActions);
		me.ui.table.selectAllHeader(2, main.ui.evaluation_header_category);
		
		onClick(me.ui.evaluation_category_add, me.addCategory);
		onClick(me.ui.evaluation_category_delete, me.deleteCategory);
		setupHoverControls([me.ui.evaluation_category_add, me.ui.evaluation_category_delete]);
	};

	this.start = function()
	{
		main.onExit = me.checkExit;
		me.load();
	};

	this.changeUseStandard = function(useStandard)
	{
		if (!me.checkExit(function(){me.changeUseStandard(useStandard);})) return;
		
		var params = main.portal.cdp.params();
		params.url.site = main.portal.site.id;
		params.url.standard = useStandard;

		main.portal.cdp.request("evaluation_changeUseStandard evaluation_gradingItems evaluation_categories", params, function(data)
		{
			main.categories = data.categories;
			me.stdCategories = data.stdCategories;
			me.summary = data.summary;
			main.sortByCategory(data.items, false); // gets cat points set
			me.makeEdit(main.categories || []);
			me.populate();
		});
	};

	this.saveCancel = function(mode, deferred)
	{
		if (mode)
		{
			if (!me.checkForValidSave()) return false;

			me.save(deferred);
		}
		else
		{
			me.edit.revert();
			me.populate();
			if (deferred !== undefined) deferred();
		}
	};

	this.checkExit = function(deferred)
	{
		if (me.edit.changed())
		{
			if (!me.checkForValidSave())
			{
				return false;
			}

			main.portal.confirmNavigationWithChanges(function()
			{
				me.save(deferred);				
			}, function()
			{
				me.edit.revert();
				if (deferred !== undefined) deferred();			
			});

			return false;
		}

		return true;
	};

	this.checkForValidSave = function()
	{
		var summary = main.categoryWeightingSummary(me.edit.categories);
		if (summary.invalid)
		{
			main.portal.dialogs.openAlert(me.ui.evaluation_fixWeights);
			return false;
		}
		return true;
	};

	this.makeEdit = function(categories)
	{
		me.edit = new e3_Edit({categories:categories}, [], function(changed)
		{
			main.ui.modebar.enableSaveDiscard(changed ? me.saveCancel : null);
			me.populateWeightMessage();
		});
		me.edit.setFilters({"title": me.edit.stringFilter, "equalDistribution": me.edit.booleanFilter, "weight": me.edit.numberFilter}, "categories[]");
		main.ui.modebar.enableSaveDiscard(null);
	};

	this.updateActions = function()
	{
		me.ui.table.updateActions([], [me.ui.evaluation_category_delete]);		
	};

	this.save = function(deferred)
	{
		var params = main.portal.cdp.params();
		params.url.site = main.portal.site.id;
		me.edit.params("", params);
		main.portal.cdp.request("evaluation_categoriesSave evaluation_gradingItems evaluation_categories", params, function(data)
		{
			main.categories = data.categories;
			me.stdCategories = data.stdCategories;
			me.summary = data.summary;
			main.sortByCategory(data.items, false); // gets cat points set
			me.makeEdit(main.categories || []);
			me.populate();
			if (deferred !== undefined) deferred();
		});
	};

	this.load = function()
	{
		var params = main.portal.cdp.params();
		params.url.site = main.portal.site.id;

		main.portal.cdp.request("evaluation_gradingItems evaluation_categories", params, function(data)
		{
			main.categories = data.categories;
			me.stdCategories = data.stdCategories;
			me.summary = data.summary;
			main.sortByCategory(data.items, false); // gets cat points set
			me.makeEdit(main.categories || []);
			me.populate();
		});
	};

	this.populate = function()
	{
		// standard or custom
		me.ui.evaluation_category_actions.empty();
		var options = [{value: "S", title: main.i18n.lookup("msg_standardCategories", "Standard Categories")}, {value: "C", title: main.i18n.lookup("msg_customCategories", "Custom Categories")}];
		me.ui.section = new e3_SortAction();
		me.ui.section.inject(me.ui.evaluation_category_actions,
				{onSort: function(dir, val){me.changeUseStandard((val == "S"));}, label: main.i18n.lookup("header_categoryChoice", "CATEGORIES"), options: options, initial: (me.stdCategories ? "S" : "C")});
		show(me.ui.evaluation_category_controls, (!me.stdCategories));

		me.ui.table.clear();
		$.each(me.edit.categories, function(index, category)
		{
			me.populateCategory(category);
		});

		me.ui.table.done();
		show(me.ui.evaluation_category_none, me.ui.table.rowCount() == 0);
		
		me.populateWeightMessage();
	};

	this.populateCategory = function(category)
	{
		var row = me.ui.table.row();

		me.ui.table.reorder(main.i18n.lookup("msg_reorder", "drag to reorder"), category.id);
		if ((!me.stdCategories) && (category.points == 0) && ((category.title != "Extra Credit")))
		{
			me.ui.table.selectBox(category.id);
		}
		else
		{
			me.ui.table.text("", "icon");
		}

		if (category.title == "Extra Credit")
		{
			me.ui.table.text(category.title, null, {width: "calc(100vw - 100px - 396px - 12px)", minWidth:"calc(1200px - 100px - 396px - 12px)", paddingLeft:12, color:"#686868"});
		}
		else
		{
			var td = me.ui.table.input({size: "30", type:"text"}, null, {width: "calc(100vw - 100px - 396px)", minWidth:"calc(1200px - 100px - 396px)"});
			me.edit.setupFilteredEdit(td.find("input"), category, "title");
		}

		// points
		me.ui.table.text(category.points, null, {width: 80});

		td = me.ui.table.input({size: "10", type:"text"}, null, {width: 80});
		me.edit.setupFilteredEdit(td.find("input"), category, "weight");
		td.find("input").css({width:"calc(100% - 38px)"});
		td.find("div").append("%");

		var select = me.ui.table.select({}, [{value:"false", text:"By Points"},{value:"true", text:"Equally"}], null, {width:100}).find("div select");
		select.val(category.equalDistribution ? "true" : "false" );
		select.css({border: "none"});
		onChange(select, function(t, finalChange)
		{
			me.edit.set(category, "equalDistribution", select.val());
		});

//		var options = [];
//		for (var itemType in ToolItemType)
//		{
//			if (ToolItemType[itemType].id >= 2000) continue;
//			options.push({value: ToolItemType[itemType].id, text: main.i18n.lookup("msg_typeName_" + ToolItemType[itemType].id)});
//		}
//		var select2 = me.ui.table.select({}, options, null, {width:120}).find("div select");
//		select2.val(category.type);
//		onChange(select2, function(t, finalChange)
//		{
//			me.edit.set(category, "type", select2.val());
//		});
		
		return row;
	};

	this.populateWeightMessage = function()
	{
		var summary = main.categoryWeightingSummary(me.edit.categories);
		if (summary.invalid)
		{
			me.ui.evaluation_category_weightsInvalid.text(main.i18n.lookup("alert_invalidWeights", "* Weights (excluding extra credit) must total 100%."));
			me.ui.evaluation_bar_total.css({color: "#E00000"});
		}
		else
		{
			me.ui.evaluation_category_weightsInvalid.text("");
			me.ui.evaluation_bar_total.css({color: "#686868"});
		}
		if (summary.set)
		{
			if (!summary.invalid)
			{
				me.ui.evaluation_category_weightSummary.text(main.i18n.lookup("msg_usingWeights", "Using category weights to calculate final grades."));
			}
			me.ui.evaluation_bar_total_header.text(main.i18n.lookup("header_weightTotal", "WEIGHT TOTAL"));
			me.ui.evaluation_bar_total.text(summary.total + "%");
			me.ui.evaluation_bar_ec.text((summary.total + summary.extra) + "%");
		}
		else
		{
			me.ui.evaluation_bar_total_header.text(main.i18n.lookup("header_points", "POINTS"));
			me.ui.evaluation_bar_total.text(me.summary.points);
			me.ui.evaluation_bar_ec.text((me.summary.points + me.summary.extraPoints));
		}

		if ((!summary.set) || (summary.invalid))
		{
			me.ui.evaluation_category_weightSummary.text(main.i18n.lookup("msg_usingPoints", "Using point values to calculate final grades."));			
		}
		
		me.ui.evaluation_category_table.find("select").attr("disabled", !summary.set);
	};

	this.newCategoryId = -1;
	this.addCategory = function()
	{
		var category = {id: me.newCategoryId--, title: "", order: me.edit.categories.length+1, type: ToolItemType.none.id, equalDistribution: false};
		me.edit.add(me.edit, "categories", category);

		var row = me.populateCategory(category);
		
		show(me.ui.evaluation_category_none, me.ui.table.rowCount() == 0);

		$(row).focus();
		return row;
	};

	this.deleteCategory = function()
	{
		var ids = me.ui.table.selected();

		if (ids == 0)
		{
			main.portal.dialogs.openAlert(main.ui.evaluation_selectFirst);
			return;
		}

		$.each(ids, function(index, id)
		{
			me.edit.remove(me.edit, "categories", id);
		});
		me.adjustCategoryOrder();
		me.populate();
	};
	
//	this.standardCategories = [{title:"Assignments", order:1, type:ToolItemType.assignment.id},
//	                          {title:"Tests", order:2, type:ToolItemType.test.id},
//	                          {title:"Offline Work", order:3, type:ToolItemType.offline.id},
//	                          {title:"Discussions", order:4, type:ToolItemType.forum.id},
//	                          {title:"Extra Credit", order:5, type:ToolItemType.extra.id},
//	                          ];
//	this.standardCategory = function()
//	{
//		var ids = [];
//		$.each(me.edit.categories, function(index, category){ids.push(category.id)});
//		$.each(ids, function(index, id){me.edit.remove(me.edit, "categories", id)});		
//		$.each(me.standardCategories, function(index, cat){me.edit.add(me.edit, "categories", {id: me.newCategoryId--, title: cat.title, order: cat.order, type: cat.type, equalDistribution: false});});
//		me.populate();
//	};

	this.applyCategoryOrder = function(order)
	{
		me.edit.order(me.edit, "categories", order);
		me.adjustCategoryOrder();
	};

	this.adjustCategoryOrder = function()
	{
		$.each(me.edit.categories, function(index, category)
		{
			if (category.order != index+1)
			{
				me.edit.set(category, "order", index+1);
			}
		});
	};
}

function Evaluation_rubricEdit(main)
{
	var me = this;
	this.ui = null;
	this.rubric = null;
	this.edit = null;

	this.findScaleLevel = function(number)
	{
		for (index = 0; index < me.edit.scale.length; index++)
		{
			if (me.edit.scale[index].number == number) return me.edit.scale[index];
		}

		return null;
	};

	this.itemPosition = function(rubric)
	{
		for (index = 0; index < main.rubrics.length; index++)
		{
			if (main.rubrics[index].id == rubric.id)
			{
				var rv = {};
				rv.item = index+1;
				rv.total = main.rubrics.length;
				rv.prev = index > 0 ? main.rubrics[index-1] : null;
				rv.next = index < main.rubrics.length-1 ? main.rubrics[index+1] : null;
				
				return rv;
			}			
		}
	};

	this.init = function()
	{
		me.ui = findElements(["evaluation_rubricEdit_title", "evaluation_rubricEdit_scale", "evaluation_rubricEdit_scaleMinus", "evaluation_rubricEdit_scalePlus",
		                      "evaluation_rubricEdit_scale0", "evaluation_rubricEdit_scale1", "evaluation_rubricEdit_scale2", "evaluation_rubricEdit_scales2", "evaluation_rubricEdit_scale3",
		                      "evaluation_rubricEdit_scale4", "evaluation_rubricEdit_scale5", "evaluation_rubricEdit_scale6",
		                      "evaluation_rubricEdit_criteria", "evaluation_rubricEdit_criteriaAdd", "evaluation_rubricEdit_criteriaDelete", "evaluation_rubricEdit_criteriaStandards",
		                      "evaluation_rubricEdit_criteriaTable", "evaluation_rubricEdit_criteriaNone",
		                      "evaluation_rubricEdit_criterionTemplate", "evaluation_rubricEdit_standardTemplate", "evaluation_rubricEdit_view"]);

		me.ui.criteriaTable = new e3_Table(me.ui.evaluation_rubricEdit_criteriaTable);
		me.ui.criteriaTable.setupSelection("evaluation_rubricCriterion_select", me.updateCriterionActions);
		me.ui.criteriaTable.enableReorder(me.applyCriteriaOrder);
		me.ui.evaluation = new e3_Evaluation(main.portal.cdp, main.portal.dialogs, main.portal.timestamp);

		onClick(me.ui.evaluation_rubricEdit_view, me.viewRubric);
		setupHoverControls([me.ui.evaluation_rubricEdit_view]);

		me.ui.itemNav = new e3_ItemNav();
		onClick(me.ui.evaluation_rubricEdit_scaleMinus, me.reduceScale);
		onClick(me.ui.evaluation_rubricEdit_scalePlus, me.enlargeScale);
		onClick(me.ui.evaluation_rubricEdit_criteriaAdd, me.addCriterion);
		onClick(me.ui.evaluation_rubricEdit_criteriaDelete, me.deleteCriteria);
		onClick(me.ui.evaluation_rubricEdit_criteriaStandards, me.editCriteria);
		setupHoverControls([me.ui.evaluation_rubricEdit_scaleMinus, me.ui.evaluation_rubricEdit_scalePlus,
		                    me.ui.evaluation_rubricEdit_criteriaAdd, me.ui.evaluation_rubricEdit_criteriaDelete, me.ui.evaluation_rubricEdit_criteriaStandards]);
	};

	this.start = function(rubric)
	{
		if (rubric != null) me.rubric = rubric;

		me.ui.itemNav.inject(main.ui.evaluation_itemnav, {doneFunction:me.done, pos:me.itemPosition(me.rubric), navigateFunction:me.goRubric});

		main.onExit = me.checkExit;

		// null when returning from criteria edit - edit already exists
		if (rubric != null)
		{
			me.makeEdit(rubric);
		}
		else
		{
			// update the changed indicators
			var changed = me.edit.changed();
			main.ui.modebar.enableSaveDiscard(changed ? me.saveCancel : null);
			me.ui.itemNav.enableSave(changed ? me.saveCancel : null);
		}

		me.populate();
	};

	this.makeEdit = function(rubric)
	{
		me.edit = new e3_Edit(rubric, ["createdOn", "createdBy", "modifiedOn", "modifiedBy"], function(changed)
		{
			main.ui.modebar.enableSaveDiscard(changed ? me.saveCancel : null);
			me.ui.itemNav.enableSave(changed ? me.saveCancel : null);
		});
		main.ui.modebar.enableSaveDiscard(null);
		me.ui.itemNav.enableSave(null);
	};

	this.findCriterion = function(id)
	{
		for (var i = 0; i < me.edit.criteria.length; i++)
		{
			if (me.edit.criteria[i].id == id) return me.edit.criteria[i];
		}
		return null;
	};

	this.populate = function()
	{
		me.edit.setupFilteredEdit(me.ui.evaluation_rubricEdit_title, me.edit, "title");
		me.adjustScaleUI();
		
		me.ui.criteriaTable.clear();
		$.each(me.edit.criteria, function(index, criterion)
		{
			me.populateCriterion(criterion);
		});
		me.ui.criteriaTable.done();
		show(me.ui.evaluation_rubricEdit_criteriaNone, me.edit.criteria.length == 0);
	};

	this.adjustScaleUI = function()
	{
		for (var i = 0; i < 6; i++)
		{
			if (me.edit.scale.length > i)
			{
				me.edit.setupFilteredEdit(me.ui["evaluation_rubricEdit_scale" + i], me.edit.scale[i], "title", function(val){me.adjustStandardTitle(i, val);});
				me.ui["evaluation_rubricEdit_scale" + i].css({backgroundColor: rgColor(i / (me.edit.scale.length-1))});
			}
			show(me.ui["evaluation_rubricEdit_scale" + i], me.edit.scale.length > i);
		}
		
		applyClass("e3_disabled", me.ui.evaluation_rubricEdit_scaleMinus, me.edit.scale.length <= 2);
		applyClass("e3_disabled", me.ui.evaluation_rubricEdit_scalePlus, me.edit.scale.length >= 6);
	};

	this.populateCriterion = function(criterion)
	{
		var row = me.ui.criteriaTable.row();
		//  <!-- 40 24 [ 16+32 16+8 x+8 24+8+32 ] 24 40 | *20 -->
		me.ui.criteriaTable.reorder(main.i18n.lookup("msg_reorder", "Drag to Reorder"), criterion.id);
		me.ui.criteriaTable.selectBox(criterion.id);

		var inp = me.ui.criteriaTable.input({type: "text"}, null, {width: "calc(100vw - 100px - 48px - 144px)", minWidth: "calc(1200px - 100px - 48px - 144px)"}).find("input");
		me.edit.setupFilteredEdit(inp, criterion, "description");

		me.ui.criteriaTable.contextMenu(
		[
			{title: main.i18n.lookup("cm_setStandards", "Set Standards"), action:function(){me.editCriterion(criterion);}},
			{title: main.i18n.lookup("cm_delete", "Delete"), action:function(){me.deleteCriterion(criterion, row);}}
        ]);

		return row;
	};

	this.updateCriterionActions = function()
	{
		me.ui.criteriaTable.updateActions([me.ui.evaluation_rubricEdit_criteriaStandards], [me.ui.evaluation_rubricEdit_criteriaDelete]);
	};

	// including rating level standards - not currently used
	this.populateCriterionFull = function(criterion)
	{
		var row = me.ui.criteriaTable.row();

		me.ui.criteriaTable.reorder(main.i18n.lookup("msg_reorder", "Drag to Reorder"), criterion.id).css({paddingLeft:8});

		var tab = clone(me.ui.evaluation_rubricEdit_criterionTemplate,
				["evaluation_rubricEdit_criterionTemplate_body", /*"evaluation_rubricEdit_criterionTemplate_title", */"evaluation_rubricEdit_criterionTemplate_description",
				 "evaluation_rubricEdit_criterionTemplate_standards", "evaluation_rubricEdit_criterionTemplate_standards2"]);
		me.ui.criteriaTable.element(tab.evaluation_rubricEdit_criterionTemplate_body);
		// me.edit.setupFilteredEdit(tab.evaluation_rubricEdit_criterionTemplate_title, criterion, "title");
		me.edit.setupFilteredEdit(tab.evaluation_rubricEdit_criterionTemplate_description, criterion, "description");
		tab.evaluation_rubricEdit_criterionTemplate_standards.attr("scid", criterion.id);
		$.each(me.edit.scale, function(index, level)
		{
			if (index < 3)
			{
				me.populateCriterionStandard(criterion, level, tab.evaluation_rubricEdit_criterionTemplate_standards);
			}
			else
			{
				me.populateCriterionStandard(criterion, level, tab.evaluation_rubricEdit_criterionTemplate_standards2);
			}
		});

		me.ui.criteriaTable.iconSvg("icon-delete", main.i18n.lookup("action_delete", "Delete"), function(){me.deleteCriterion(criterion, row);});

		return row;
	};

	this.populateCriterionStandard = function(criterion, level, into)
	{
		var std = clone(me.ui.evaluation_rubricEdit_standardTemplate, ["evaluation_rubricEdit_standardTemplate_body", "evaluation_rubricEdit_standardTemplate_title", "evaluation_rubricEdit_standardTemplate_content"]);
		std.evaluation_rubricEdit_standardTemplate_title.text(level.title);
		std.evaluation_rubricEdit_standardTemplate_title.attr("lnumtitle", level.number);
		
		me.edit.setupFilteredEdit(std.evaluation_rubricEdit_standardTemplate_content, criterion.standards[level.number], "description");
		
		std.evaluation_rubricEdit_standardTemplate_body.attr("lnumbody", level.number);
		
		if ((level.number != 2) && (level.number != 5)) std.evaluation_rubricEdit_standardTemplate_body.css({paddingRight: 32});

		into.append(std.evaluation_rubricEdit_standardTemplate_body);

//		if (level.number == 0)
//		{
//			std.evaluation_rubricEdit_standardTemplate_title.css({backgroundColor: main.colorsLight[0]});
//		}
//		else if (level.number == me.edit.scale.length-1)
//		{
//			std.evaluation_rubricEdit_standardTemplate_title.css({backgroundColor: main.colorsLight[2]});
//		}
//		else
//		{
//			std.evaluation_rubricEdit_standardTemplate_title.css({backgroundColor: main.colorsLight[1]});
//		}
		std.evaluation_rubricEdit_standardTemplate_title.css({backgroundColor: rgColor(level.number / (me.edit.scale.length-1))});
	};

	this.adjustStandardTitle = function(levelNumber, title)
	{
		$('[lnumtitle="' + levelNumber + '"]').text(title);
	};

	this.adjustStandards = function(levelNumber)
	{
		if (levelNumber == null)
		{
			$.each(me.edit.criteria, function(index, criterion)
			{
				me.edit.remove(criterion, "standards", criterion.standards[criterion.standards.length-1].id);
			});

//			$('[lnumbody="' + me.edit.scale.length + '"]').remove();
		}
		
		else
		{
			$.each(me.edit.criteria, function(index, criterion)
			{
				var standard = {id:(levelNumber * -1), level:levelNumber, description:null};
				me.edit.add(criterion, "standards", standard);
				
//				var level = me.findScaleLevel(levelNumber);
//				me.populateCriterionStandard(criterion, level, $('[scid="' + criterion.id + '"]'));
			});
		}
	};

	this.reduceScale = function()
	{
		if (me.edit.scale.length > 2)
		{
			me.edit.remove(me.edit, "scale", me.edit.scale[me.edit.scale.length-1].id);
			me.adjustStandards(null);
			me.populate();
//			me.adjustScaleUI();
		}
	};

	this.enlargeScale = function()
	{
		if (me.edit.scale.length < /*7*/6)
		{
			var levelNumber = me.edit.scale.length;
			me.edit.add(me.edit, "scale", {id:levelNumber * -1, number:levelNumber, title:null});
			me.edit.set(me.edit.scale[levelNumber], "title", "");
			me.adjustStandards(levelNumber);
			me.populate();
//			me.adjustScaleUI();
		}
	};

	this.nextCriterion = -1;
	this.addCriterion = function()
	{
		var index = me.edit.criteria.length;
		var criterion = {id: me.nextCriterion--, title:null, description:null, order:index+1, scorePct:null, standards:[]};

		me.edit.add(me.edit, "criteria", criterion);
		me.edit.set(me.edit.criteria[index], "title", "");
		me.edit.set(me.edit.criteria[index], "description", "");
		$.each(me.edit.scale, function(i, level)
		{
			var standard = {id:(level.number * -1), level:level.number, description:null};
			me.edit.add(me.edit.criteria[index], "standards", standard);
		});

		var row = me.populateCriterion(criterion);
		show(me.ui.evaluation_rubricEdit_criteriaNone, me.ui.criteriaTable.rowCount() == 0);

		row.focus();
		return row;
	};

	this.deleteCriteria = function()
	{
		var ids = me.ui.criteriaTable.selected();

		if (ids.length == 0)
		{
			main.portal.dialogs.openAlert(main.ui.evaluation_selectFirst);
			return;
		}

		main.portal.dialogs.openConfirm("evaluation_confirmCriteriaDelete", main.i18n.lookup("action_delete", "Delete"), function()
		{
			$.each(ids, function(index, id)
			{
				me.edit.remove(me.edit, "criteria", id);
			});

			me.adjustCriterionOrder();
			me.populate();

			return true;
		});
	};

	this.deleteCriterion = function(criterion, row)
	{
		main.portal.dialogs.openConfirm("evaluation_confirmCriteriaDelete", main.i18n.lookup("action_delete", "Delete"), function()
		{
			me.edit.remove(me.edit, "criteria", criterion.id);
			me.adjustCriterionOrder();
			me.ui.criteriaTable.removeRow(row);

			return true;
		});
	};

	this.editCriterion = function(criterion)
	{
		main.startRubricCriterionEdit(me.edit, criterion);
	};

	this.editCriteria = function()
	{
		var ids = me.ui.criteriaTable.selected();
		if (ids.length != 1)
		{
			main.portal.dialogs.openAlert(main.ui.evaluation_select1First);
			return;
		}

		criterion = me.findCriterion(ids[0])
		
		main.startRubricCriterionEdit(me.edit, criterion);
	};

	this.applyCriteriaOrder = function(order)
	{
		me.edit.order(me.edit, "criteria", order);
		me.adjustCriterionOrder();
	};

	this.adjustCriterionOrder = function()
	{
		$.each(me.edit.criteria, function(index, criterion)
		{
			if (criterion.order != index+1)
			{
				me.edit.set(criterion, "order", index+1);
			}
		});
	};

	this.saveCancel = function(mode, deferred)
	{
		if (mode)
		{
			me.save(deferred);
		}
		else
		{
			me.edit.revert();
			me.populate();
			if (deferred !== undefined) deferred();
		}
	};

	this.checkExit = function(deferred)
	{
		if (me.edit.changed())
		{
			main.portal.confirmNavigationWithChanges(function()
			{
				me.save(deferred);
			}, function()
			{
				// if just added, forget about it
				if (me.edit.id  < 0)
				{
					main.rubrics.splice(main.rubrics.length-1, 1);
				}
				me.edit.revert();
				if (deferred !== undefined) deferred();			
			});

			return false;
		}

		return true;
	};

	this.save = function(deferred)
	{
		var params = main.portal.cdp.params();
		params.url.site = main.portal.site.id;
		params.url.rubric = me.edit.id;
		me.edit.params("", params);
		main.portal.cdp.request("evaluation_rubricSave evaluation_rubrics", params, function(data)
		{
			main.rubrics = data.rubrics || [];
			
			me.rubric = main.findRubric(data.rubric) || me.rubric;
			me.makeEdit(me.rubric);
			me.populate();

			if (deferred !== undefined) deferred();
		});
	};

	this.viewRubric = function()
	{
		main.ui.evaluation_rubricEdit_alertView.attr("title", main.i18n.lookup("title_rubricView", "RUBRIC: %0", "html", [me.edit.title]));
		me.ui.evaluation.rubricView.set(main.ui.evaluation_rubricEdit_alertView_view, me.edit);
		main.portal.dialogs.openAlert(main.ui.evaluation_rubricEdit_alertView);
	};

	this.done = function()
	{
		if (me.edit.changed())
		{
			me.save(function(){main.startRubric();})
		}
		else
		{
			main.startRubric();
		}
	};

	this.goRubric = function(rubric)
	{
		main.startRubricEdit(rubric);
	};
}

function Evaluation_rubricCriterionEdit(main)
{
	var me = this;
	this.ui = null;
	this.rubricEdit = null;
	this.edit = null;
	this.criterion = null;

	this.findScaleLevel = function(number)
	{
		for (index = 0; index < me.edit.scale.length; index++)
		{
			if (me.rubricEdit.scale[index].number == number) return me.rubricEdit.scale[index];
		}

		return null;
	};

	this.findCriterionByOrder = function(order)
	{
		for (index = 0; index < me.rubricEdit.criteria.length; index++)
		{
			if (me.rubricEdit.criteria[index].order == order) return me.rubricEdit.criteria[index];
		}
		
		return null;
	};

	this.init = function()
	{
		me.ui = findElements(["evaluation_bar_rubricCriterionEdit_rubric",
		                      "evaluation_rubricCriterionEdit_description", 
		                      "evaluation_rubricCriterionEdit_standard_0", "evaluation_rubricCriterionEdit_standard_0_title", "evaluation_rubricCriterionEdit_standard_0_content",
		                      "evaluation_rubricCriterionEdit_standard_1", "evaluation_rubricCriterionEdit_standard_1_title", "evaluation_rubricCriterionEdit_standard_1_content",
		                      "evaluation_rubricCriterionEdit_standard_2", "evaluation_rubricCriterionEdit_standard_2_title", "evaluation_rubricCriterionEdit_standard_2_content",
		                      "evaluation_rubricCriterionEdit_standard_3", "evaluation_rubricCriterionEdit_standard_3_title", "evaluation_rubricCriterionEdit_standard_3_content",
		                      "evaluation_rubricCriterionEdit_standard_4", "evaluation_rubricCriterionEdit_standard_4_title", "evaluation_rubricCriterionEdit_standard_4_content",
		                      "evaluation_rubricCriterionEdit_standard_5", "evaluation_rubricCriterionEdit_standard_5_title", "evaluation_rubricCriterionEdit_standard_5_content",
		                      ]);
		me.ui.itemNav = new e3_ItemNav();
	};

	this.start = function(rubric, criterion)
	{
		main.onExit = me.checkExit;
		me.ui.itemNav.inject(main.ui.evaluation_itemnav, {doneFunction:me.done});

		me.rubricEdit = rubric;
		me.criterion = criterion;
		me.makeEdit();

		me.populate();
	};

	this.makeEdit = function()
	{
		me.edit = new e3_Edit(me.criterion, [], function(changed)
		{
			main.ui.modebar.enableSaveDiscard((changed || me.rubricEdit.changed()) ? me.saveCancel : null);
			main.ui.modebar.enableDiscard(changed ? me.saveCancel : null);
			me.ui.itemNav.enableSave((changed || me.rubricEdit.changed()) ? me.saveCancel : null);
		});
		main.ui.modebar.enableSaveDiscard(me.rubricEdit.changed() ? me.saveCancel : null);
		main.ui.modebar.enableDiscard(null);
		me.ui.itemNav.enableSave(me.rubricEdit.changed() ? me.saveCancel : null);
	};

	this.populate = function()
	{
		me.ui.evaluation_bar_rubricCriterionEdit_rubric.text(main.i18n.lookup("msg_rubricTitle", "Rubric: %0", "html", [me.rubricEdit.title]));

		me.edit.setupFilteredEdit(me.ui.evaluation_rubricCriterionEdit_description, me.edit, "description");
		
		for (var i = 0; i < 6; i++)
		{
			if (me.rubricEdit.scale.length > i)
			{
				me.setupStandard(me.rubricEdit.scale[i]);
			}
			show(me.ui["evaluation_rubricCriterionEdit_standard_" + i], (me.rubricEdit.scale.length > i));
		}
	};

	this.setupStandard = function(level)
	{
		me.ui["evaluation_rubricCriterionEdit_standard_" + level.number + "_title"].text(level.title);
		me.ui["evaluation_rubricCriterionEdit_standard_" + level.number + "_title"].css({backgroundColor: rgColor(level.number / (me.rubricEdit.scale.length-1))});

		me.edit.setupFilteredEdit(me.ui["evaluation_rubricCriterionEdit_standard_" + level.number + "_content"], me.edit.standards[level.number], "description");		
	};

	this.saveCancel = function(mode, deferred)
	{
		if (mode)
		{
			me.save(deferred);
		}
		else
		{
			if (deferred !== undefined) deferred();

			me.makeEdit();
			me.populate();

			return false;
		}
	};

	this.checkExit = function(deferred)
	{
		me.saveToEdit();
		main.startRubricEdit();
		return main.checkExit(deferred);
	};

	this.saveToEdit = function()
	{
		// update the rubric edit
		me.rubricEdit.set(me.criterion, "description", me.edit.description);
		for (var i = 0; i < 6; i++)
		{
			if (me.rubricEdit.scale.length > i)
			{
				me.rubricEdit.set(me.criterion.standards[i], "description", me.edit.standards[i].description);
			}
		}
	};

	this.save = function(deferred)
	{
		me.saveToEdit();

		// save the entire edit
		main.rubricEditMode.save(function()
		{
			me.rubricEdit = main.rubricEditMode.edit;
			me.criterion = me.findCriterionByOrder(me.criterion.order);
			me.makeEdit();
			me.populate();

			if (deferred !== undefined) deferred();			
		});
	};

	this.done = function()
	{
		me.saveToEdit();

		// with no rubric to indicate a return
		main.startRubricEdit();
	};
}

function Evaluation_rubric(main)
{
	var me = this;

	this.nextNewRubricId = -1;
	this.nextNewCriteriaId = -1;
	this.nextNewLevelId = -1;
	
	this.init = function()
	{
		me.ui = findElements(["evaluation_rubric_table", "evaluation_rubric_none", "evaluation_rubric_add", "evaluation_rubric_delete", "evaluation_rubric_view"]);

		me.ui.evaluation = new e3_Evaluation(main.portal.cdp, main.portal.dialogs, main.portal.timestamp);

		me.ui.table = new e3_Table(me.ui.evaluation_rubric_table);
		me.ui.table.setupSelection("evaluation_rubric_select", me.updateActions);
		me.ui.table.selectAllHeader(1, main.ui.evaluation_header_rubric);

		onClick(me.ui.evaluation_rubric_add, me.addRubric);
		onClick(me.ui.evaluation_rubric_delete, me.deleteRubric);
		onClick(me.ui.evaluation_rubric_view, me.viewRubric)
		setupHoverControls([me.ui.evaluation_rubric_add, me.ui.evaluation_rubric_delete, me.ui.evaluation_rubric_view]);
	};

	this.start = function()
	{
		me.load();
	};
	
	this.load = function()
	{
		var params = main.portal.cdp.params();
		params.url.site = main.portal.site.id;

		main.portal.cdp.request("evaluation_rubrics", params, function(data)
		{
			main.rubrics = data.rubrics || [];
			me.populate();
		});		
	};

	this.populate = function()
	{
		me.ui.table.clear();
		$.each(main.rubrics, function(index, rubric)
		{
			me.ui.table.row();
			
			me.ui.table.selectBox(rubric.id);
			me.ui.table.hotText(rubric.title, main.i18n.lookup("msg_editAnnc", "Edit '%0'", "html", [rubric.title]), function(){me.editRubric(rubric);}, null, {width: 300});
			me.ui.table.text(me.describeRubric(rubric), null, {width: "calc(100vw - 100px - 428px)", minWidth: "calc(1200px - 100px - 428px)"});
			// me.ui.evaluation.rubricDisplay.set(td.find("div"), rubric);			
			me.ui.table.contextMenu(
			[
				{title: main.i18n.lookup("cm_view", "View"), action:function(){me.viewARubric(rubric);}},
				{title: main.i18n.lookup("cm_edit", "Edit"), action:function(){me.editRubric(rubric);}},
				{title: main.i18n.lookup("cm_delete", "Delete"), action:function(){me.deleteARubric(rubric);}}
	        ]);
		});

		me.ui.table.done();

		show(me.ui.evaluation_rubric_none, me.ui.table.rowCount() == 0);
	};

	this.describeRubric = function(rubric)
	{
		return main.i18n.lookup("msg_rubricDescription", "Rating scale levels: %0, Criteria: %1", "html", [rubric.scale.length, rubric.criteria.length]);
	};

	this.updateActions = function()
	{
		me.ui.table.updateActions([me.ui.evaluation_rubric_view], [me.ui.evaluation_rubric_delete]);		
	};

	this.addRubric = function()
	{
		var rubric = {id:-1, title:"", scale:[{id:-1, number:0, title:"Below Expectations"},{id:-2, number:1, title:"Meets Expectations"},{id:-3, number:2, title:"Above Expectations"}], criteria:[]};
		main.rubrics.push(rubric);

		me.editRubric(rubric);
	};

	this.deleteRubric = function()
	{
		var params = main.portal.cdp.params();
		params.url.site = main.portal.site.id;
		params.post.ids = me.ui.table.selected();

		if (params.post.ids.length == 0)
		{
			main.portal.dialogs.openAlert(main.ui.evaluation_selectFirst);
			return;
		}

		main.portal.dialogs.openConfirm("evaluation_confirmRubricDelete", main.i18n.lookup("action_delete", "Delete"), function()
		{
			main.portal.cdp.request("evaluation_rubricDelete evaluation_rubrics", params, function(data)
			{
				main.rubrics = data.rubrics || [];
				me.populate();
			});

			return true;
		});
	};

	this.deleteARubric = function(rubric)
	{
		var params = main.portal.cdp.params();
		params.url.site = main.portal.site.id;
		params.post.ids = [rubric.id];

		main.portal.dialogs.openConfirm("evaluation_confirmRubricDelete", main.i18n.lookup("action_delete", "Delete"), function()
		{
			main.portal.cdp.request("evaluation_rubricDelete evaluation_rubrics", params, function(data)
			{
				main.rubrics = data.rubrics || [];
				me.populate();
			});

			return true;
		});
	};

	this.editRubric = function(rubric)
	{
		main.startRubricEdit(rubric);
	};

	this.viewRubric = function(rubric)
	{
		var selected = me.ui.table.selected();
		if (selected.length != 1)
		{
			main.portal.dialogs.openAlert(main.ui.evaluation_select1First);
			return;			
		}

		rubric = main.findRubric(selected[0]);

		if (rubric != null)
		{
			main.ui.evaluation_rubricEdit_alertView.attr("title", main.i18n.lookup("title_rubricView", "RUBRIC: %0", "html", [rubric.title]));
			me.ui.evaluation.rubricView.set(main.ui.evaluation_rubricEdit_alertView_view, rubric);
			main.portal.dialogs.openAlert(main.ui.evaluation_rubricEdit_alertView);
		}
	};

	this.viewARubric = function(rubric)
	{
		main.ui.evaluation_rubricEdit_alertView.attr("title", main.i18n.lookup("title_rubricView", "RUBRIC: %0", "html", [rubric.title]));
		me.ui.evaluation.rubricView.set(main.ui.evaluation_rubricEdit_alertView_view, rubric);
		main.portal.dialogs.openAlert(main.ui.evaluation_rubricEdit_alertView);
	};

	this.applyOrder = function(order)
	{
		var params = main.portal.cdp.params();
		params.url.site = main.site.id;
		params.post.order = order;

		main.portal.cdp.request("evaluation_rubricOrder evaluation_rubrics", params, function(data)
		{
			main.announcements = data.announcements || [];
			me.populate();
		});
	};
}

$(function()
{
	try
	{
		evaluation_tool = new Evaluation();
		evaluation_tool.init();
		evaluation_tool.start();
	}
	catch (e)
	{
		error(e);
	}
});

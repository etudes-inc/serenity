/**********************************************************************************
 * $URL: https://source.etudes.org/svn/serenity/trunk/coursemap/coursemap-webapp/src/main/webapp/coursemap.js $
 * $Id: coursemap.js 12504 2016-01-10 00:30:08Z ggolden $
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

var coursemap_tool = null;

function Coursemap_view(main)
{
	var me = this;
	
	this.items = [];

	this.init = function()
	{
		me.ui = findElements(["cm_view_table", "cm_view_none", "cm_view_title_template"]);
		me.ui.table = new e3_Table(me.ui.cm_view_table);
	};

	this.start = function()
	{
		me.load();
	};

	this.load = function()
	{
		var params = main.portal.cdp.params();
		params.url.site = main.portal.site.id;
		main.portal.cdp.request("coursemap_getView coursemap_getOptions", params, function(data)
		{
			me.items = data.items || [];
			main.options = data.options;

			me.populate();
		});
	};

	this.populate = function()
	{
		me.ui.table.clear();
		var blocking = false;
		$.each(me.items, function(index, item)
		{
			// insert an in-table heading for headers
			if (item.type == ToolItemType.header.id)
			{
				var row = me.ui.table.row();
				me.ui.table.headerRow(item.title);

				if (blocking)
				{
					row.css({opacity: 0.4});
				}
			}

			else
			{
				if ((item.published !== undefined) && (!item.published)) return;
				if ((item.schedule.status == ScheduleStatus.willOpenHide)) return;
	
				var row = me.ui.table.row();

				main.itemProgressDotTd(item, me.ui.table);
				if (blocking || (!item.active))
				{
					var td = me.ui.table.text(item.title, null, {width:"calc(100vw - 100px - 682px)", minWidth:"calc(1200px - 100px - 682px"});
					if (!blocking) td.css({opacity: 0.4});
				}
				else
				{
					if (item.blocking)
					{
						var cell = clone(me.ui.cm_view_title_template, ["cm_view_title_template_body", "cm_view_title_template_title", "cm_view_title_template_msg"]);
						me.ui.table.hotElement(cell.cm_view_title_template_body, item.title, function(){me.goItem(item);}, null, {width:"calc(100vw - 100px - 682px)", minWidth:"calc(1200px - 100px - 682px)"});

						cell.cm_view_title_template_title.text(item.title);
						cell.cm_view_title_template_msg.text(main.i18n.lookup("msg_blocker", "* This is a prerequisite. Complete it to make further progress."));
					}
					else
					{
						me.ui.table.hotText(item.title, item.title, function(){me.goItem(item);}, null, {width:"calc(100vw - 100px - 682px)", minWidth:"calc(1200px - 100px - 682px)"});
					}					
				}
				main.typeTd(item, me.ui.table);
				me.ui.table.date(item.schedule.open, "-", "date2l");
				me.ui.table.date(item.schedule.due, "-", "date2l");
	
				if (item.evaluation != null) // evaluation, or submission(s)...
				{
					me.ui.table.date(item.evaluation.submittedOn, "-", "date2l");
					me.ui.table.text((item.count == null ? "-" : item.count), null, {width:80});
					
					if (item.scoreNA)
					{
						me.ui.table.text(main.i18n.lookup("msg_na", "n/a"), null, {width:150});
					}
					else
					{
						var td = me.ui.table.text("", null, {width:150});
						main.evaluation.reviewLink.set(td, item.design, item.evaluation, function(){main.startReview(item, me.items, me.member);});
					}
				}
				else
				{
					me.ui.table.text("-", "date2l");
					me.ui.table.text("-", null, {width:80});
					
					if (item.scoreNA)
					{
						me.ui.table.text(main.i18n.lookup("msg_na", "n/a"), null, {width:150});
					}
					else
					{
						me.ui.table.text("-", null, {width:150});
					}
				}

				if (blocking)
				{
					row.css({opacity: 0.4});
				}

				if (item.blocking)
				{
					blocking = true;
				}
			}
		});
		me.ui.table.done();

		show(me.ui.cm_view_none, (me.ui.table.rowCount() == 0));
	};
	
	this.goItem = function(item)
	{
		main.portal.navigate(main.portal.site, item.toolItem.tool, true, false, {action: Actions.perform, id: item.toolItem.itemId});
	};
}

function Coursemap_manage(main)
{
	var me = this;
	this.reorder = null;
	this.items = null;
	this.edit = null;

	this.init = function()
	{
		me.ui = findElements(["cm_manage_table", "cm_manage_none"]);
		me.ui.table = new e3_Table(me.ui.cm_manage_table);
		me.ui.table.enableReorder(me.applyOrder);
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
		main.portal.cdp.request("coursemap_getManage coursemap_getOptions", params, function(data)
		{
			me.items = data.items || [];
			main.options = data.options;
			me.makeEdit();
			me.populate();
		});
	};

	this.makeEdit = function()
	{
		// edit of the items, id and blocker and dates
		me.edit = new e3_Edit({items: me.items},
			["items[].progress", "items[].count", "items[].published", "items[].type", "items[].design", "items[].evaluation", "items[].schedule.hide", "items[].schedule.status", "items[].schedule.close",
			 "items[].datesNA", "items[].blocking", "items[].datesRO", "items[].active", "items[].toolItem", "items[].scoreNA"],
			function(changed)
			{
				main.ui.modebar.enableSaveDiscard((changed || (me.reorder != null)) ? me.saveCancel : null);
			});

		main.ui.modebar.enableSaveDiscard(null);
	};

	this.findInEdit = function(id)
	{
		for (var i = 0; i < me.edit.items.length; i++)
		{
			if (me.edit.items[i].id == id) return me.edit.items[i];
		}
		return null;
	};

	this.populate = function()
	{
		me.ui.table.clear();
		$.each(me.items, function(index, item)
		{
			me.ui.table.row();
			me.ui.table.reorder(main.i18n.lookup("msg_reorder", "drag to reorder"), item.id);
			var inEdit = me.findInEdit(item.id);

			// insert an in-table heading for headers TODO: editable, context menu
			if (item.type == ToolItemType.header.id)
			{
				me.ui.table.dot(Dots.none);

				var td = me.ui.table.input({type: "text"}, null, {width:"calc(100vw - 100px - 144px)", minWidth:"calc(1200px - 100px - 144px"});
				td.attr("colspan", "6");
				me.edit.setupFilteredEdit(td.find("input"), inEdit, "title");
				
				me.ui.table.contextMenu(
				[
					{title: main.i18n.lookup("cm_deleteHeader", "Delete"), action:function(){me.removeHeader(item);}}
		        ]);
			}

			else
			{
				main.itemPublicationStatusDotTd(item, me.ui.table);
				me.ui.table.text(item.title, null, {width:"calc(100vw - 100px - 864px)", minWidth:"calc(1200px - 100px - 8640px"});
				main.typeTd(item, me.ui.table);

				me.edit.setupCheckEdit(me.ui.table.input({type: "checkbox"}, null, {width: 100}).find("input"), inEdit, "blocker");
				
				if (item.datesNA)
				{
					me.ui.table.text("-", "dateInput");
					me.ui.table.text("-", "dateInput");
					me.ui.table.text("-", "dateInput");
				}
				else if (item.datesRO)
				{
					me.ui.table.date(item.schedule.open, "-", "dateInput", {fontSize: 12, paddingLeft: 8, width: "calc(100% - 8px)", color: "#A8A8A8"});
					me.ui.table.date(item.schedule.due, "-", "dateInput", {fontSize: 12, paddingLeft: 8, width: "calc(100% - 8px)", color: "#A8A8A8"});
					me.ui.table.date(item.schedule.allowUntil, "-", "dateInput", {fontSize: 12, paddingLeft: 8, width: "calc(100% - 8px)", color:"#A8A8A8"});
				}
				else
				{
					me.edit.setupDateEdit(me.ui.table.inputDate().find("input"), inEdit.schedule, "open", main.portal.timestamp, true);
					me.edit.setupDateEdit(me.ui.table.inputDate().find("input"), inEdit.schedule, "due", main.portal.timestamp, false);
					me.edit.setupDateEdit(me.ui.table.inputDate().find("input"), inEdit.schedule, "allowUntil", main.portal.timestamp, false);
				}

				me.ui.table.contextMenu(
				[
					{title: main.i18n.lookup("cm_insertHeader", "Insert Header"), action:function(){me.insertHeader(item);}}
		        ]);
			}
		});
		me.ui.table.done();

		show(me.ui.cm_manage_none, (me.ui.table.rowCount() == 0));
	};

	this.applyOrder = function(order)
	{
		main.ui.modebar.enableSaveDiscard(me.saveCancel);
		me.reorder = order;
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
			me.edit.revert();
			main.ui.modebar.enableSaveDiscard(null);
			me.populate();
			if (deferred !== undefined) deferred();
		}
	};
	
	this.checkExit = function(deferred)
	{
		if ((me.reorder != null) || (me.edit.changed()))
		{
			main.portal.confirmNavigationWithChanges(function()
			{
				me.save(deferred);				
			}, function()
			{
				me.reorder = null;
				me.edit.revert();
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
		params.post.order = me.reorder;
		me.edit.params("", params);
		console.log(params);
		main.portal.cdp.request("coursemap_order coursemap_save coursemap_getManage", params, function(data)
		{
			me.items = data.items || [];
			me.reorder = null;
			me.makeEdit();
			main.ui.modebar.enableSaveDiscard(null);
			me.populate();
			if (deferred !== undefined) deferred();
		});
	};

	this.insertHeader = function()
	{
		console.log("insert header");
	};

	this.removeHeader = function()
	{
		console.log("remove header");
	};
}

function Coursemap_review(main)
{
	var me = this;

	this.ui = null;
	this.item = {};
	this.items = null;
	this.member = {};

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

	this.position = function()
	{
		for (index = 0; index < me.items.length; index++)
		{
			if ((me.items[index].toolItem.tool.id == me.item.toolItem.tool.id) && (me.items[index].toolItem.itemId == me.item.toolItem.itemId))
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
		me.ui = findElements(["cm_review_review_ui", "cm_review_evaluation_ui",
		                      "cm_info_review_score", "cm_info_review_statusDot", "cm_info_review_title", "cm_info_review_started", "cm_info_review_finished"]);
		me.ui.itemNav = new e3_ItemNav();
	};

	this.start = function(item, items, member)
	{
		me.item = item;
		
		if (items != null) me.filterItems(items);
		if (member != null) me.member = member;

		me.load();

		me.ui.itemNav.inject(main.ui.cm_itemnav, {returnFunction:main.startView, pos:me.position(), navigateFunction:me.start});
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

		me.ui.cm_info_review_score.text(asPct(evaluation.score, me.item.design.points));

		me.ui.cm_info_review_title.text(main.i18n.lookup("msg_titlePoints", "%0, %1 points", "html", [me.item.title, me.item.design.points]));

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
		me.ui.cm_info_review_statusDot.html(dotSmall(color, msg, true));

		// TODO: we don't know started (not in evaluation) me.ui.cm_info_review_started
		me.ui.cm_info_review_finished.text(main.i18n.lookup("msg_finished", "Finished: %0", "html", [main.portal.timestamp.display(evaluation.submittedOn)]));

		// TODO: set the work to review area, based on the tool item and submission
		// me.ui.cm_review_review_ui.text("ITEM TO REVIEW");

		main.evaluation.review.set(me.ui.cm_review_evaluation_ui, me.item.design, evaluation);
	};
}

function Coursemap_option(main)
{
	var me = this;

	this.ui = null;
	this.edit = null;

	this.init = function()
	{
		me.ui = findElements(["cm_option_master", "cm_dropPrereqOnClose"]);
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
		main.portal.cdp.request("coursemap_getOptions", params, function(data)
		{
			main.options = data.options;
			me.makeEdit();
			me.populate();
		});
	};

	this.makeEdit = function()
	{
		me.edit = new e3_Edit(main.options, [], function(changed)
		{
			main.ui.modebar.enableSaveDiscard(changed ? me.saveCancel : null);
		});
		me.edit.setFilters({"masteryPct": me.edit.numberFilter}, "");

		main.ui.modebar.enableSaveDiscard(null);
	};

	this.populate = function()
	{
		me.edit.setupFilteredEdit(me.ui.cm_option_master, me.edit, "masteryPct");
		me.edit.setupCheckEdit(me.ui.cm_dropPrereqOnClose, me.edit, "removePrereqOnClose");
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
				// me.populate();
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
		main.portal.cdp.request("coursemap_saveOptions coursemap_getOptions", params, function(data)
		{
			main.options = data.options;
			me.edit.revert();
			if (deferred !== undefined) deferred();
		});
	};	
}

function Coursemap()
{
	var me = this;

	this.i18n = new e3_i18n(coursemap_i10n, "en-us");
	this.portal = null;
	this.ui = null;
	this.evaluation = null;
	this.onExit = null;

	this.options = null;

	this.viewMode = null;
	this.manageMode = null;
	this.reviewMode = null;
	this.optionMode = null;
	this.modes = null;

	this.typeTd = function(item, table)
	{
		table.text(me.i18n.lookup("msg_type_" + item.type), "e3_text special light", {fontSize:11, width:60});
	};

	this.itemProgressDotTd = function(item, table)
	{
//		var complete = (item.evaluation != null);

		if (item.progress == ProgressStatus.inprogress)
		{
			table.dot(Dots.progress, me.i18n.lookup("msg_inProgressSimple", "in progress"));
		}
		else if (item.progress == ProgressStatus.complete)
		{
			table.dot(Dots.complete, me.i18n.lookup("msg_complete", "finished"));
		}
		else if (item.schedule.status == ScheduleStatus.closed)
		{
			table.dot(Dots.alert, me.i18n.lookup("msg_missed", "missed"));
		}
		else
		{
			table.dot(Dots.none);
		}
	};

	// dot for item status (manage)
	this.itemPublicationStatusDotTd = function(item, table)
	{
		if (!item.published)
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

	this.init = function()
	{
		me.i18n.localize();
		me.ui = findElements(["cm_header", "cm_modebar", "cm_headerbar", "cm_itemnav", 
		                      "cm_bar_view", "cm_header_view", "cm_view", "cm_view_edit",
		                      "cm_bar_manage", "cm_header_manage", "cm_manage",
		                      "cm_bar_review", "cm_review",
		                      "cm_option"]);
		me.portal = portal_tool.features({onExit: me.checkExit, pin:[{ui:me.ui.cm_header}]});
		me.evaluation = new e3_Evaluation(me.portal.cdp, me.portal.dialogs, me.portal.timestamp);

		if ((me.portal.site.role >= Role.instructor) || (me.portal.site.role == Role.ta))
		{
			me.ui.modebar = new e3_Modebar(me.ui.cm_modebar);
			me.modes =
			[
				{name:me.i18n.lookup("mode_view", "View"), func:function(){me.startView();}},
				{name:me.i18n.lookup("mode_manage", "Manage"), func:function(){me.startManage();}},
				{name:me.i18n.lookup("mode_option", "Options"), func:function(){me.startOption();}}
			];
			me.ui.modebar.set(me.modes, 0);

			me.manageMode = new Coursemap_manage(me);
			me.manageMode.init();

			me.optionMode = new Coursemap_option(me);
			me.optionMode.init();
		}

		me.viewMode = new Coursemap_view(me);
		me.viewMode.init();

		me.reviewMode = new Coursemap_review(me);
		me.reviewMode.init();

		// show(me.ui.cm_modebar, ((me.portal.site.role >= Role.instructor) || (me.portal.site.role == Role.ta)));
		onClick(me.ui.cm_view_edit, me.startManage);
	};

	this.start = function()
	{
		if ((me.portal.site.role >= Role.instructor) || (me.portal.site.role == Role.ta))
		{
			me.startManage();
		}
		else
		{
			me.startView();
		}
	};

	this.startView = function()
	{
		if ((me.onExit != null) && (!me.onExit(function(){me.startView();}))) return;
		me.mode([me.ui.cm_header_view, me.ui.cm_view, ((me.portal.site.role >= Role.instructor) ? me.ui.cm_view_edit : null)]);
		if (me.ui.modebar !== undefined) me.ui.modebar.showSelected(0);
		me.viewMode.start();
	};

	this.startManage = function()
	{
		if ((me.onExit != null) && (!me.onExit(function(){me.startManage();}))) return;
		me.mode([me.ui.cm_modebar, /*me.ui.cm_headerbar, me.ui.cm_bar_manage, */me.ui.cm_header_manage, me.ui.cm_manage]);
		if (me.ui.modebar !== undefined) me.ui.modebar.showSelected(1);
		me.manageMode.start();
	};

	this.startReview = function(item, items, member)
	{
		if ((me.onExit != null) && (!me.onExit(function(){me.startReview(item, items, member);}))) return;
		me.mode([me.ui.cm_headerbar, me.ui.cm_itemnav, me.ui.cm_bar_review, me.ui.cm_review]);
		// if (me.ui.modebar != null) me.ui.modebar.showSelected(1);
		me.reviewMode.start(item, items, member);
	};

	this.startOption = function()
	{
		if ((me.onExit != null) && (!me.onExit(function(){me.startOption();}))) return;
		me.mode([me.ui.cm_modebar, me.ui.cm_option]);
		if (me.ui.modebar != null) me.ui.modebar.showSelected(2);
		me.optionMode.start();
	};

	this.mode = function(modeUi)
	{
		hide([me.ui.cm_modebar, me.ui.cm_headerbar, me.ui.cm_itemnav,
			  me.ui.cm_header_view, me.ui.cm_view, me.ui.cm_view_edit,
			  me.ui.cm_bar_manage, me.ui.cm_header_manage, me.ui.cm_manage,
			  me.ui.cm_bar_review, me.ui.cm_review,
			  me.ui.cm_option]);
		me.onExit = null;
		me.portal.resetScrolling();
		show(modeUi);
	};
	
	this.checkExit = function(deferred)
	{
		if (me.onExit == null) return true;
		return me.onExit(deferred);
	};
}

$(function()
{
	try
	{
		coursemap_tool = new Coursemap();
		coursemap_tool.init();
		coursemap_tool.start();
	}
	catch (e)
	{
		error(e);
	}
});

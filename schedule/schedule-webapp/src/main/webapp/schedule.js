/**********************************************************************************
 * $URL: https://source.etudes.org/svn/serenity/trunk/schedule/schedule-webapp/src/main/webapp/schedule.js $
 * $Id: schedule.js 12504 2016-01-10 00:30:08Z ggolden $
 ***********************************************************************************
 *
 * Copyright (c) 2015, 2016 Etudes, Inc.
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

var schedule_tool = null;

function Schedule()
{
	var me = this;

	this.i18n = new e3_i18n(schedule_i10n, "en-us");
	this.portal = null;
	this.ui = null
//	this.modes = null;
	this.onExit = null;

	this.viewMode = null;
	this.editMode = null;

	this.fs = 1; // 0 for homepage filesystem, 1 for CHS/resources file system, 2 - mneme, 3 - melete, 9 for serenity fs

	this.init = function()
	{
		me.i18n.localize();
		me.ui = findElements(["schedule_header", "schedule_modebar", "schedule_headerbar", "schedule_itemnav",
		                      "schedule_bar_view", "schedule_view",
		                      "schedule_bar_edit", "schedule_edit",
		                      "schedule_view_event", "schedule_view_event_title", "schedule_view_event_byline", "schedule_view_event_content"
		                     ]);
		me.portal = portal_tool.features({onExit: me.checkExit, pin:[{ui:me.ui.schedule_header}]});
		
		me.viewMode = new Schedule_view(me);
		me.viewMode.init();

		if ((me.portal.site.role >= Role.instructor) || (me.portal.site.role == Role.ta))
		{
			me.editMode = new Schedule_edit(me);
			me.editMode.init();

			me.ui.modebar = new e3_Modebar(me.ui.schedule_modebar);
//			me.modes =
//			[
//				{name:me.i18n.lookup("mode_view", "View"), func:function(){me.startView();}},
//				{name:me.i18n.lookup("mode_edit", "Manage"), func:function(){me.startManage();}}
//			];
			me.ui.modebar.set(null, 0);
		}

//		show(me.ui.schedule_modebar, ((me.portal.site.role >= Role.instructor) || (me.portal.site.role == Role.ta)));
	};

	this.start = function()
	{
//		if ((me.portal.site.role >= Role.instructor) || (me.portal.site.role == Role.ta))
//		{
//			me.startManage();
//		}
//		else if (me.portal.site.role == Role.student)
//		{
			me.startView();
//		}
	};

	this.startView = function()
	{
		if ((me.onExit != null) && (!me.onExit(function(){me.startView();}))) return;
		me.mode([me.ui.schedule_headerbar, me.ui.schedule_bar_view, me.ui.schedule_view]);
//		if (me.ui.modebar != null) me.ui.modebar.showSelected(0);
		me.viewMode.start();
	};

	this.startEdit = function(eventId)
	{
		if ((me.onExit != null) && (!me.onExit(function(){me.startEdit(eventId);}))) return;
		me.mode([me.ui.schedule_modebar, me.ui.schedule_headerbar, me.ui.schedule_itemnav, me.ui.schedule_bar_edit, me.ui.schedule_edit]);
//		if (me.ui.modebar != null) me.ui.modebar.showSelected(1);
		me.editMode.start();
	};

	this.mode = function(modeUi)
	{
		hide([me.ui.schedule_modebar, me.ui.schedule_headerbar, me.ui.schedule_itemnav,
		      me.ui.schedule_bar_view, me.ui.schedule_view,
		      me.ui.schedule_bar_edit, me.ui.schedule_edit
		     ]);
		me.onExit = null;
		me.portal.resetScrolling();
		show(modeUi);
	};
	
	this.checkExit = function(deferred)
	{
		if (me.onExit == null) return true;
		return me.onExit(deferred);
	};
	
	this.showEvent = function(event)
	{
//		var tool = Tools.byId(event.tool);
//		var toolTitle = ((tool == null) ? "???" : tool.title); // TODO: run through i18n
//
//		var  toolItemType = ToolItemType.byId(event.itemType);
//		var itemTitle = ((toolItemType == null) ? "???" : toolItemType.title); // TODO: run through i18n

		me.ui.schedule_view_event_title.text(event.title);

		if (event.schedule.end != null)
		{
			me.ui.schedule_view_event_byline.text(me.i18n.lookup("msg_eventByline2dates", "%0 - %1", "html", [me.portal.timestamp.display(event.schedule.start), me.portal.timestamp.display(event.schedule.end)]))
		}
		else
		{
			me.ui.schedule_view_event_byline.text(me.i18n.lookup("msg_eventByline1date", "%0", "html", [me.portal.timestamp.display(event.schedule.start)]));
		}

		if (event.content != null) me.ui.schedule_view_event_content.html(event.content);
		show(me.ui.schedule_view_event_content, (event.content != null));

		var btns = [];
		if ((me.portal.site.role >= Role.instructor) || (me.portal.site.role == Role.ta))
		{
			btns.push({text:me.i18n.lookup("msg_edit", "EDIT"), click:function()
			{
				me.closeShowEvent();
				me.startEdit(event.id);
			}});
		}

		me.portal.dialogs.openAlert(me.ui.schedule_view_event, null, btns);
	};

	this.closeShowEvent = function()
	{
		me.portal.dialogs.close(me.ui.schedule_view_event);
	};
	
	this.adjustEventsDates = function(events)
	{
		$.each(events, function(i, event)
		{
			if ((event.start === undefined) && (event.schedule.start !== undefined))
			{
				event.start = me.portal.timestamp.moment(event.schedule.start);
			}
			if ((event.end === undefined) && (event.schedule.end !== undefined))
			{
				event.end = me.portal.timestamp.moment(event.schedule.end);
			}
		});
	};
}

function Schedule_view(main)
{
	var me = this;

	this.ui = null;

	this.events = null;
	
	this.init = function()
	{
		me.ui = findElements(["schedule_view_prev", "schedule_view_today", "schedule_view_next",
		                      "schedule_view_title",
		                      "schedule_view_add", "schedule_view_view",
		                      "schedule_calendar"]);
		
		me.populateViewSelect();

		setupHoverControls([me.ui.schedule_view_prev, me.ui.schedule_view_today, me.ui.schedule_view_next, me.ui.schedule_view_add]);
		onClick(me.ui.schedule_view_prev, me.toPrev);
		onClick(me.ui.schedule_view_today, me.toToday);
		onClick(me.ui.schedule_view_next, me.toNext);
		onClick(me.ui.schedule_view_add, me.addEvent);
	};

	this.start = function()
	{
		me.load();
	};
	
	this.load = function(eventId)
	{
		var params = main.portal.cdp.params();
		params.url.site = main.portal.site.id;
		params.url.start = 0; // TODO:
		params.url.end = 0; // TODO:
		main.portal.cdp.request("schedule_view", params, function(data)
		{
			me.events = data.events;
			main.adjustEventsDates(me.events);

			me.populate();
		});
	};

	this.populate = function()
	{
		console.log(main.portal.user.timeZone);
		me.ui.schedule_calendar.fullCalendar(
		{
			header:
			{
				left: "",
				center: "",
				right: ""
			},
			timezone: main.portal.user.timeZone,
			eventSources:
			[
				{
					events: me.getEvents,
					color: '#A8A8A8',
				    textColor: 'white'
				}
			],
			eventClick: me.eventClick,
			dayClick: me.dayClick,
			viewRender: me.calendarChanged
		});
	};

	this.populateViewSelect = function(initial)
	{
		if (initial === undefined) initial = "M";

		me.ui.schedule_view_view.empty();
		me.ui.view = new e3_SortAction();
		me.ui.view.inject(me.ui.schedule_view_view,
			{onSort: function(dir, val){me.changeView(val);}, label: main.i18n.lookup("label_view", "VIEW"),
			 options: [{value: "D", title: main.i18n.lookup("label_day", "Day")},
			           {value: "W", title: main.i18n.lookup("label_week", "Week")},
			           {value: "M", title: main.i18n.lookup("label_month", "Month")}
			           ],
			 initial: initial});
	};

	this.getEvents = function(start, end, timezone, callback)
	{
//		console.log("getEvents", start, end, timezone, callback);
		callback(me.events);
//		[
//			{
//				title: 'Event1',
//				start: '2016-01-07',
//				end: '2016-01-09',
//				id: "FIRST"
//			},
//			{
//				title: 'Event2',
//				start: '2016-01-08T12:30:00',
//				end: '2016-01-08T13:30:00',
//				allDay: false,
//				id: "SECOND"
//			}
//		]);
	};

	this.eventClick = function(calEvent, jsEvent, view)
	{
		console.log("eventClick", calEvent, jsEvent, view);
//		if (main.editMode != null)
//		{
//			main.startEdit(0);
//		}
//		else
//		{
			var event = findIdInList(calEvent.id, me.events);
			main.showEvent(event);
//		}

		return false; // no URL visiting!
	};

	this.dayClick = function(date, jsEvent, view, resourceObj)
	{
		me.ui.schedule_calendar.fullCalendar("changeView", "agendaDay");
		me.ui.schedule_calendar.fullCalendar("gotoDate", date);
		me.populateViewSelect("D");
	};

	this.changeView = function(view)
	{
		var viewName = "month";
		if (view == "D")
		{
			viewName = "agendaDay";
		}
		else if (view == "W")
		{
			viewName = "agendaWeek";
		}

		me.ui.schedule_calendar.fullCalendar("changeView", viewName);
	}

	this.toPrev = function()
	{
		me.ui.schedule_calendar.fullCalendar("prev");
	};

	this.toToday = function()
	{
		me.ui.schedule_calendar.fullCalendar("today");
	};
	
	this.toNext = function()
	{
		me.ui.schedule_calendar.fullCalendar("next");
	};
	
	this.calendarChanged = function(view, element)
	{
		me.ui.schedule_view_title.text(view.title);
	};

	this.addEvent = function()
	{
		console.log("add event");
	};
}

function Schedule_edit(main)
{
	var me = this;

	var ui = null;

	this.event = null;
	this.edit = null;

	this.init = function()
	{
		me.ui = findElements(["schedule_edit_title", "schedule_edit_start", "schedule_edit_end", "schedule_edit_content"
		                     ]);
		me.ui.editor = new e3_EditorCK(me.ui.schedule_edit_content, {height: 350});
		me.ui.itemNav = new e3_ItemNav();
	};

	this.start = function(eventId)
	{
		main.onExit = me.checkExit;
		me.load(eventId);
	};

	this.load = function(eventId)
	{
		var params = main.portal.cdp.params();
		params.url.site = main.portal.site.id;
		params.url.event = eventId;
		main.portal.cdp.request("schedule_edit", params, function(data)
		{
			me.event = data.event;
			me.ui.itemNav.inject(main.ui.schedule_itemnav, {doneFunction:me.done});

			me.makeEdit();
			me.populate();
		});
	};

	this.makeEdit = function()
	{
		me.edit = new e3_Edit(me.event, ["id"], function(changed) // TODO:
		{
			main.ui.modebar.enableSaveDiscard(changed ? me.saveCancel : null);
			me.ui.itemNav.enableSave(changed ? me.saveCancel : null);
		});
		me.edit.setFilters({"title": me.edit.stringFilter}); // TODO:
	
		main.ui.modebar.enableSaveDiscard(null);
		me.ui.itemNav.enableSave(null);
	};

	this.populate = function()
	{
		me.edit.setupFilteredEdit(me.ui.schedule_edit_title, me.edit, "title");

		me.ui.editor.disable();
		me.ui.editor.set(me.edit.content);
		me.ui.editor.enable(function()
		{
			me.edit.set(me.edit, "content", me.ui.editor.get());
		}, false /* no focus */, main.fs);

		me.edit.setupDateEdit(me.ui.schedule_edit_start, me.edit.schedule, "start", main.portal.timestamp, true);
		me.edit.setupDateEdit(me.ui.schedule_edit_end, me.edit.schedule, "end", main.portal.timestamp, true);
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
				me.populate();
				if (deferred !== undefined) deferred();
			});

			return false;
		}

		return true;
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

	this.save = function(deferred)
	{
		var params = main.portal.cdp.params();
		params.url.site = main.portal.site.id;
		params.url.event = me.event.id;
		me.edit.params("", params);
		console.log(params);
		main.portal.cdp.request("schedule_save schedule_edit", params, function(data)
		{
			me.event = data.event;

			me.makeEdit();
			me.populate();

			if (deferred !== undefined) deferred();
		});
	};
	
	this.done = function()
	{
		// save if changed
		if (me.edit.changed())
		{
			me.save(main.startView);
		}
		else
		{
			main.startView();
		}
	}
}

$(function()
{
	try
	{
		schedule_tool = new Schedule();
		schedule_tool.init();
		schedule_tool.start();
	}
	catch (e)
	{
		error(e);
	}
});

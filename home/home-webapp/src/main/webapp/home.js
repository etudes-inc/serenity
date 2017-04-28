/**********************************************************************************
 * $URL: https://source.etudes.org/svn/serenity/trunk/home/home-webapp/src/main/webapp/home.js $
 * $Id: home.js 12504 2016-01-10 00:30:08Z ggolden $
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

var home_tool = null;

function HomeActivity(main)
{
	var me = this;
	this.ui = null;

	this.announcements = [];
	this.activity = {};

	this.init = function()
	{
		me.ui = findElements(["home_message_count", "home_message_count_body", "home_message_count_normal", "home_message_count_hover", "home_message_count_view",
		                      "home_forum_count", "home_forum_count_body", "home_forum_count_normal", "home_forum_count_hover", "home_forum_count_view",
		                      "home_alert_bkg", "home_alert_title", "home_alert_count", "home_alert_count_body", "home_alert_count_normal", "home_alert_count_hover", "home_alert_count_view", "home_alert_heading"]);
	};

	this.populate = function()
	{
		if (me.activity.unreadMessages == 0)
		{
//			me.ui.home_message_count.css({opacity:0.4});
		}
		else
		{
			me.ui.home_message_count.text(me.badgeValue(me.activity.unreadMessages));
//			me.ui.home_message_count.css({opacity:1});
		}
		onClick(me.ui.home_message_count_view, function(){main.portal.navigate(main.portal.site, Tools.message, false, false);});
		onHover(me.ui.home_message_count_body,
				function(){me.ui.home_message_count_hover.stop().animate({opacity:1}, Hover.on);},
				function(){me.ui.home_message_count_hover.stop().animate({opacity:0}, Hover.off);});

		if (me.activity.unreadPosts == 0)
		{
//			me.ui.home_forum_count.css({opacity:0.4});
		}
		else
		{
			me.ui.home_forum_count.text(me.badgeValue(me.activity.unreadPosts));
//			me.ui.home_forum_count.css({opacity:1});
		}
		onClick(me.ui.home_forum_count_view, function(){main.portal.navigate(main.portal.site, Tools.forum, false, false);});
		onHover(me.ui.home_forum_count_body,
				function(){me.ui.home_forum_count_hover.stop().animate({opacity:1}, Hover.on);},
				function(){me.ui.home_forum_count_hover.stop().animate({opacity:0}, Hover.off);});

		// for instructors
		if ((main.portal.site.role >= Role.instructor))
		{
			me.ui.home_alert_bkg.css("background-image","url('/ui/art/mark/alert.png')");
			me.ui.home_alert_title.text(main.i18n.lookup("header_absent", "ABSENT"));
			if (me.activity.notVisitAlerts == 0)
			{
//				me.ui.home_alert_count.css({opacity:0.4});
			}
			else
			{
				me.ui.home_alert_count.text(me.badgeValue(me.activity.notVisitAlerts));
//				me.ui.home_alert_count.css({opacity:1});
			}
			onClick(me.ui.home_alert_count_view, function(){main.portal.navigate(main.portal.site, Tools.activity, false, false);});
			onHover(me.ui.home_alert_count_body,
					function(){me.ui.home_alert_count_hover.stop().animate({opacity:1}, Hover.on);},
					function(){me.ui.home_alert_count_hover.stop().animate({opacity:0}, Hover.off);});
		}
		else
		{
			me.ui.home_alert_bkg.css("background-image","url('/ui/art/mark/graded.png')");
			me.ui.home_alert_title.text(main.i18n.lookup("header_review", "REVIEWS"));
			if (me.activity.reviewCount == 0)
			{
//				me.ui.home_alert_count.css({opacity:0.4});
			}
			else
			{
				me.ui.home_alert_count.text(me.badgeValue(me.activity.reviewCount));
//				me.ui.home_alert_count.css({opacity:1});
			}
			onClick(me.ui.home_alert_count_view, function(){main.portal.navigate(main.portal.site, Tools.evaluation, false, false);});
			onHover(me.ui.home_alert_count_body,
					function(){me.ui.home_alert_count_hover.stop().animate({opacity:1}, Hover.on);},
					function(){me.ui.home_alert_count_hover.stop().animate({opacity:0}, Hover.off);});
		}
	};

	this.badgeValue = function(value)
	{
		if (value < 1000)
		{
			return value.toString();
		}
		return main.i18n.lookup("msg_tooMany", "!!!");
	};
}

function HomeAnnc(main)
{
	var me = this;
	this.ui = null;

	this.announcements = [];

	this.init = function()
	{
		me.ui = findElements(["home_announcements", "home_annc_template", "home_view_annc", "home_view_annc_subject", "home_view_annc_byline", "home_view_annc_content", "home_allNews"]);
//		onClick(me.ui.home_allNews, function(){main.portal.navigate(main.portal.site, Tools.announcement, false, false);});
	};

	this.announcementPosition = function(annc)
	{
		var rv = {};
		
		if (annc.id == null)
		{
			rv.item = me.announcements.length + 1;
			rv.total = me.announcements.length + 1;
			rv.prev = me.announcements[me.announcements.length-1];
			rv.next = null;
		}
		else
		{
			var i = 1;
			var found = null;
			$.each(me.announcements || [], function(index, a)
			{
				if (a.id == annc.id) found = i;
				i++;
			});
	
			rv.item = found;
			rv.total = me.announcements.length;
			rv.prev = found > 1 ? me.announcements[found-2] : null;
			rv.next = found < me.announcements.length ? me.announcements[found] : null;
		}

		return rv;
	};

	this.populateAnnouncements = function()
	{
		me.ui.home_announcements.empty();
		$.each(me.announcements, function(index, annc)
		{
			me.populateAnnouncement(annc);
		});
	};

	this.populateAnnouncements = function()
	{
		me.ui.home_announcements.empty();
		$.each(me.announcements, function(index, annc)
		{
			var entry = clone(me.ui.home_annc_template, ["home_annc_template_body","home_annc_template_subject","home_annc_template_content",
			                                                  "home_annc_template_normal","home_annc_template_hover","home_annc_template_view"]);
			if (index == 0)
			{
				entry.home_annc_template_body.css("border", "none");
			}
			onClick(entry.home_annc_template_view, function(){me.showAnnouncement(annc);});
			entry.home_annc_template_subject.text(annc.title);
			entry.home_annc_template_content.html(annc.content);			
			onHover(entry.home_annc_template_body,
					function(){entry.home_annc_template_hover.stop().animate({opacity:1}, Hover.on);},
					function(){entry.home_annc_template_hover.stop().animate({opacity:0}, Hover.off);});
			me.ui.home_announcements.append(entry.element.children());
		});
	};

	this.showAnnouncement = function(annc)
	{
		me.ui.home_view_annc_subject.text(annc.title);
		if ((annc.createdBy != null) && (annc.bylineDate != null))
		{
			me.ui.home_view_annc_byline.text(main.i18n.lookup("msg_byline", "%0, %1", "html", [annc.createdBy, main.portal.timestamp.display(annc.bylineDate)]));
		}
		else
		{
			me.ui.home_view_annc_byline.text(main.i18n.lookup("msg_bylineMOTD", "System Message"));
		}
		me.ui.home_view_annc_content.html(annc.content);

		main.portal.dialogs.openAlert(me.ui.home_view_annc);
	};
}

function HomeCalendar(main)
{
	var me = this;
	this.ui = null;

	this.eventsDate = null;
	this.events = [];
	this.eventsDays =
	{
		year: 0,
		month: 0,
		days: []
	};

	this.init = function()
	{
		me.ui = findElements(["home_calendar", "home_allCalendar", "home_event_template",
		                      "home_view_event", "home_view_event_title", "home_view_event_byline", "home_view_event_content",
		                      "home_events_header_date", "home_events_header_tz", "home_events", "home_events_none"]);

		main.portal.timestamp.setCalendar(me.ui.home_calendar, {onSelect: function(date)
		{
			me.loadEvents(date);
		}, beforeShowDay: me.checkForEventsOnDate, onChangeMonthYear: me.loadEventsDays});

		// onClick(me.ui.home_allCalendar, function(){main.portal.navigate(main.portal.site, Tools.schedule, false, false);});
	};

	this.loadEventsDays = function(year, month)
	{
		var params = me.portal.cdp.params();
		params.url.site = main.portal.site.id;
		params.url.year = year;
		params.url.month = month;
		me.portal.cdp.request("home_eventsDays", params, function(data)
		{
			me.eventsDays = data.eventsDays;
			me.portal.timestamp.refresh(me.ui.home_calendar);
		});
	};

	this.checkForEventsOnDate = function(date)
	{
		if ((me.eventsDays.year == date.getFullYear()) && (me.eventsDays.month == (date.getMonth()+1)) && (me.eventsDays.days.indexOf(date.getDate()) != -1))
		{
			return [true, "hasEvent", ""];
		}
		return [false, "", ""];
	};

	this.loadEvents = function(date)
	{
		var params = main.portal.cdp.params();
		params.url.site = main.portal.site.id;
		params.url.date = date;
		main.portal.cdp.request("home_events", params, function(data)
		{
			me.eventsDate = data.eventsDate;
			me.events = data.events;
			me.populateEvents();
		});
	};
	
	this.populateEvents = function()
	{
		me.ui.home_events_header_date.text(main.portal.timestamp.displayDate(me.eventsDate));
		me.ui.home_events_header_tz.text(main.portal.timestamp.displayTz());
		me.ui.home_events.empty();

		$.each(me.events, function(index, event)
		{
			var tab = clone(me.ui.home_event_template, ["home_event_template_body","home_event_template_time","home_event_template_ampm","home_event_template_title",
			                                              "home_event_template_normal", "home_event_template_hover", "home_event_template_view"]);
			tab.home_event_template_time.text(main.portal.timestamp.displayTime(event.dateStart));
			tab.home_event_template_ampm.text(main.portal.timestamp.displayAmPm(event.dateStart));
			tab.home_event_template_title.text(event.title);
			onClick(tab.home_event_template_view, function(){me.showEvent(event);});
			onHover(tab.home_event_template_body,
					function(){tab.home_event_template_hover.stop().animate({opacity:1}, Hover.on);},
					function(){tab.home_event_template_hover.stop().animate({opacity:0}, Hover.off);});

			me.ui.home_events.append(tab.element.children());
		});

		show(me.ui.home_events_none, (me.events.length == 0));
	};
	
	this.showEvent = function(event)
	{
		var tool = Tools.byId(event.tool);
		var toolTitle = ((tool == null) ? "???" : tool.title); // TODO: run through i18n

		var  toolItemType = ToolItemType.byId(event.itemType);
		var itemTitle = ((toolItemType == null) ? "???" : toolItemType.title); // TODO: run through i18n

        me.ui.home_view_event_title.text(event.title);

		if (event.dateEnd != null)
		{
			me.ui.home_view_event_byline.text(main.i18n.lookup("msg_eventByline2dates", "%0: %1 - %2", "html", [toolTitle, main.portal.timestamp.display(event.dateStart), main.portal.timestamp.display(event.dateEnd)]))
		}
		else
		{
			me.ui.home_view_event_byline.text(main.i18n.lookup("msg_eventByline1date", "%0: %1", "html", [toolTitle, main.portal.timestamp.display(event.dateStart)]));
		}

		if (event.content != null) me.ui.home_view_event_content.html(event.content);
		show(me.ui.home_view_event_content, (event.content != null));
        
		var btns = [];
		btns.push({text:main.i18n.lookup("msg_goto", "VIEW %0", "html", [itemTitle]), click:function()
		{
			me.closeShowEvent();
			main.portal.navigate(main.portal.site, event.tool, false, false);
		}});
		if (main.portal.site.role >= Role.instructor)
		{
			btns.push({text:main.i18n.lookup("msg_edit", "EDIT %0", "html", [itemTitle]), click:function()
			{
				me.closeShowEvent();
				main.portal.navigate(main.portal.site, event.tool, false, false);
			}});
		}

		main.portal.dialogs.openAlert(me.ui.home_view_event, null, btns);
	};
	
	this.closeShowEvent = function()
	{
		main.portal.dialogs.close(me.ui.home_view_event);
	}
}

function HomeRenderer(main)
{
	var me = this;

	this.ui = null;
	
	this.init = function()
	{
		me.ui = findElements(["home_youtube_template", "home_authored_template", "home_file_template", "home_web_template", "home_nothing_template"]);
	}

	this.render = function(title, body, item)
	{
		if (title != null) title.empty().text(item.title || main.i18n.lookup("msg_genericTitle", "HOME"));

		if (body != null)
		{
			body.empty();
			var element = null;
			switch (item.source)
			{
				case "Y":
				{
					element = me.renderYoutube(item.youtubeId, item.ratio, body.width());
					break;
				}
				case "W":
				{
					element = me.renderWeb(item.url, item.alt, item.height);
					break;
				}
				case "A":
				{
					element = me.renderAuthored(item.content);
					break;
				}
				case "F":
				{
					element = me.renderFile(item.fileUrl, item.alt);
					break;
				}
				case "-":
				{
					element = me.renderNothing();
					break;
				}
			}
			
			if (element != null)
			{
				body.append(element);
			}
		}
	};

	this.renderNothing = function()
	{
		var element = clone(me.ui.home_nothing_template, ["home_nothing_template_body", "home_nothing_template_msg"]);
		
		element.home_nothing_template_msg.text(main.i18n.lookup("msg_welcome", "Welcome to %0", "html", [main.portal.site.title]));

		return element.home_nothing_template_body;
	};

	this.renderYoutube = function(id, ratio, width)
	{
		var element = clone(me.ui.home_youtube_template, ["home_youtube_template_body", "home_youtube_template_iframe"]);
		
		var src = element.home_youtube_template_iframe.attr("src");
		src = src.replace("%0", id);
		element.home_youtube_template_iframe.attr("src", src);

		var height = width;
		switch (ratio)
		{
			case "16":
			{
				height = width * 9 / 16;
				break;
			}
			
			case "4":
			{
				height = width * 3 / 4;
				break;
			}
		}
		element.home_youtube_template_body.height(height);

		return element.home_youtube_template_body;
	};
	
	this.renderAuthored = function(content)
	{
		var element = clone(me.ui.home_authored_template, ["home_authored_template_body"]);

		element.home_authored_template_body.html(content);

		return element.home_authored_template_body;
	};
	
	this.renderFile = function(url, alt)
	{
		var element = clone(me.ui.home_file_template, ["home_file_template_body", "home_file_template_img"]);

		element.home_file_template_img.attr({src: url, alt: alt, title: alt});

		return element.home_file_template_body;
	};

	this.renderWeb = function(url, alt, height)
	{
		var element = clone(me.ui.home_web_template, ["home_web_template_body", "home_web_template_img", "home_web_template_iframe"]);

		var isImage = isImageUrl(url);
		show(element.home_web_template_img, isImage);
		show(element.home_web_template_iframe, !isImage);
		
		if (isImage)
		{
			element.home_web_template_img.attr({src: url, alt: alt, title: alt});
		}
		else
		{
			element.home_web_template_iframe.attr("src", url);
			element.home_web_template_body.height(height);
		}

		return element.home_web_template_body;
	};
}

function HomeView(main)
{
	var me = this;

	this.ui = null;

	this.calendar = null;
	this.annc = null;
	this.activity = null;
	this.current = null;

	this.init = function()
	{
		me.ui = findElements(["home_view_edit", "home_view_content_title", "home_view_content_body"]);

		me.calendar = new HomeCalendar(main);
		me.annc = new HomeAnnc(main);
		me.activity = new HomeActivity(main);

		me.calendar.init();
		me.annc.init();
		me.activity.init();
		
		onClick(me.ui.home_view_edit, main.startManage);
	};

	this.start = function()
	{
		var params = main.portal.cdp.params();
		params.url.site = main.portal.site.id;
		main.portal.cdp.request("home_events home_eventsDays home_announcements home_activity home_options home_current", params, function(data)
		{
			main.options = data.options || {};

			me.calendar.eventsDate = data.eventsDate;
			me.calendar.events = data.events;
			me.calendar.populateEvents();

			me.annc.announcements = data.announcements;
			me.annc.populateAnnouncements();
			
			me.activity.activity = data.activity;
			me.activity.populate();

			me.current = data.current || {source: "-"};
			main.renderer.render(me.ui.home_view_content_title, me.ui.home_view_content_body, me.current);

			me.calendar.eventsDays = data.eventsDays;
			main.portal.timestamp.refresh(me.calendar.ui.home_calendar);

			processMathMl();
		});
	};	
}

function HomeManage(main)
{
	var me = this;
	this.ui = null;

	this.init = function()
	{
		me.ui = findElements(["home_manage_table", "home_manage_add", "home_manage_delete", "home_manage_publish", "home_manage_unpublish", "home_manage_view"]);
		me.ui.table = new e3_Table(me.ui.home_manage_table);
		me.ui.table.setupSelection("home_manage_table_select", me.updateActions);
		me.ui.table.selectAllHeader(1, main.ui.home_header_manage);

		onClick(me.ui.home_manage_add, me.add);
		onClick(me.ui.home_manage_delete, me.remove);
		onClick(me.ui.home_manage_publish, me.publish);
		onClick(me.ui.home_manage_unpublish, me.unpublish);
		onClick(me.ui.home_manage_view, me.view);
		setupHoverControls([me.ui.home_manage_add, me.ui.home_manage_delete, me.ui.home_manage_publish, me.ui.home_manage_unpublish, me.ui.home_manage_view]);
	};

	this.start = function()
	{
		me.load();
	};

	this.load = function()
	{
		var params = main.portal.cdp.params();
		params.url.site = main.portal.site.id;
		main.portal.cdp.request("home_get", params, function(data)
		{
			main.items = data.items || [];
			me.populate();
		});
	};

	this.populate = function()
	{
		me.ui.table.clear();
		$.each(main.items, function(index, item)
		{
			me.ui.table.row();

			// insert an in-table heading between status groups
			if (((index > 0) && (main.items[index-1].status != item.status)) || (index == 0))
			{
				me.ui.table.headerRow(main.i18n.lookup("msg_status_" + item.status));
				me.ui.table.row();
			}

			me.ui.table.selectBox(item.id);

			if (!item.published)
			{
				me.ui.table.dot(Dots.red, main.i18n.lookup("msg_unpublished", "not published"));
			}
			else if (item.status == main.itemStatus.current)
			{
				me.ui.table.dot(Dots.green, main.i18n.lookup("msg_current", "now showing"));
			}
			else if (item.status == main.itemStatus.pending)
			{
				me.ui.table.dot(Dots.gray, main.i18n.lookup("msg_pending", "coming soon"));
			}
			else
			{
				// me.ui.table.dot(Dots.gray,  main.i18n.lookup("msg_past", "Past"));
				me.ui.table.dot(Dots.none);
			}

			me.ui.table.hotText(item.title, main.i18n.lookup("msg_editContent", "edit %0", "html", [item.title]), function(){main.startEdit(item);}, null, {width: "calc(100vw - 100px - 652px)", minWidth: "calc(1200px - 100px - 652px)"}); 
			
			me.ui.table.text(main.i18n.lookup("msg_src_" + item.source), null, {width:200});
			me.ui.table.date(item.releaseOn);
			me.ui.table.date(item.modifiedOn);
		});

		me.ui.table.done();
	};

	this.updateActions = function()
	{
		me.ui.table.updateActions([me.ui.home_manage_view], [me.ui.home_manage_delete, me.ui.home_manage_publish,me.ui.home_manage_unpublish]);		
	};

	this.add = function()
	{
		var item = {id: -1, title: "", source: "F", published: false, current: false, pending: false, ratio:"16"};
		main.items.push(item);
		main.startEdit(item);
	};
	
	this.remove = function()
	{
		var params = main.portal.cdp.params();
		params.url.site = main.portal.site.id;
		params.post.ids = me.ui.table.selected();

		if (params.post.ids.length == 0)
		{
			main.portal.dialogs.openAlert(main.ui.home_manage_selectFirst);
			return;
		}

		main.portal.dialogs.openConfirm("home_manage_confirmDelete", main.i18n.lookup("action_delete", "Delete"), function()
		{
			main.portal.cdp.request("home_remove home_get", params, function(data)
			{
				main.items = data.items || [];
				me.populate();
			});

			return true;
		}, function(){me.ui.table.clearSelection();});
	};

	this.publish = function()
	{
		var params = main.portal.cdp.params();
		params.url.site = main.portal.site.id;
		params.post.ids = me.ui.table.selected();

		if (params.post.ids.length == 0)
		{
			main.portal.dialogs.openAlert(main.ui.home_manage_selectFirst);
			return;
		}

		main.portal.cdp.request("home_publish home_get", params, function(data)
		{
			main.items = data.items || [];
			me.populate();
		});
	};

	this.unpublish = function()
	{
		var params = main.portal.cdp.params();
		params.url.site = main.portal.site.id;
		params.post.ids = me.ui.table.selected();

		if (params.post.ids.length == 0)
		{
			main.portal.dialogs.openAlert(main.ui.home_manage_selectFirst);
			return;
		}

		main.portal.cdp.request("home_unpublish home_get", params, function(data)
		{
			main.items = data.items || [];
			me.populate();
		});
	};
	
	this.view = function()
	{
		var ids = me.ui.table.selected();
		if (ids == 0)
		{
			main.portal.dialogs.openAlert(main.ui.home_manage_selectFirst);
			return;
		}

		var item= main.findItem(ids[0]);
		if (item == null) return;
		console.log("view item", item);
	};
}

function HomeEdit(main)
{
	var me = this;

	this.ui = null;

	this.item = null;
	this.edit = null;

	this.init = function()
	{
		me.ui = findElements(["home_edit_view", "home_edit_title", "home_edit_releaseOn", "home_edit_releaseOn_alert", "home_edit_published",
		                      "home_edit_webFields", "home_edit_web_url", "home_edit_web_url_alert", "home_edit_web_alt", "home_edit_web_height",
		                      "home_edit_youtubeFields", "home_edit_youtube_id", "home_edit_youtube_id_alert", "home_edit_youtube_help",
		                      "home_edit_authorFields", "home_edit_author",
		                      "home_edit_fileFields", "home_edit_file_selected", "home_edit_file_picker", "home_edit_file_picker_alert", "home_edit_file_alt",
		                      "home_edit_attribution"]);
		me.ui.itemNav = new e3_ItemNav();
		me.ui.editor = new e3_EditorCK(me.ui.home_edit_author, {height: 350});
		me.ui.filer = new e3_FilerCK(me.ui.home_edit_file_picker);
		onClick(me.ui.home_edit_view, me.view);
		onClick(me.ui.home_edit_youtube_help, function(){main.portal.dialogs.openAlert("home_edit_youtubeHelp");});
		setupHoverControls([me.ui.home_edit_view, me.ui.home_edit_youtube_help]);
	};

	this.start = function(item)
	{
		me.item = item;
		main.onExit = me.checkExit;
		me.ui.itemNav.inject(main.ui.home_itemnav, {doneFunction:me.done, pos:main.itemPosition(me.item), navigateFunction:me.goItem});

		me.makeEdit();
		me.populate();
	};

	this.makeEdit = function()
	{
		me.edit = new e3_Edit(me.item, ["createdOn", "createdBy", "modifiedOn", "modifiedBy", "id", "fileName"], function(changed)
		{
			main.ui.modebar.enableSaveDiscard(changed ? me.saveCancel : null);
			me.ui.itemNav.enableSave(changed ? me.saveCancel : null);
		});
		me.edit.setFilters({"youtubeId": me.extractYoutubeId});

		main.ui.modebar.enableSaveDiscard(null);
		me.ui.itemNav.enableSave(null);
	};

	this.populate = function()
	{
		me.edit.setupFilteredEdit(me.ui.home_edit_title, me.edit, "title");

		me.edit.setupDateEdit(me.ui.home_edit_releaseOn, me.edit, "releaseOn", main.portal.timestamp, true, function(val)
		{
			fade(me.ui.home_edit_releaseOn_alert, (val == null));
		});
		show(me.ui.home_edit_releaseOn_alert, (trim(me.edit.releaseOn) == null));

		me.edit.setupCheckEdit(me.ui.home_edit_published, me.edit, "published");
		me.edit.setupRadioEdit("home_edit_source", me.edit, "source", function()
		{
			me.populate();
		});

		show(me.ui.home_edit_webFields, ("W" == me.edit.source));
		show(me.ui.home_edit_youtubeFields, ("Y" == me.edit.source));
		show(me.ui.home_edit_authorFields, ("A" == me.edit.source));
		show(me.ui.home_edit_fileFields, ("F" == me.edit.source));
		if ("W" == me.edit.source)
		{
			me.edit.setupFilteredEdit(me.ui.home_edit_web_url, me.edit, "url", function(val)
			{
				fade(me.ui.home_edit_web_url_alert, (trim(val) == null));
			});
			show(me.ui.home_edit_web_url_alert, (trim(me.edit.url) == null));

			me.edit.setupFilteredEdit(me.ui.home_edit_web_alt, me.edit, "alt");
			me.edit.setupFilteredEdit(me.ui.home_edit_web_height, me.edit, "height"); // TODO: number filter 
		}
		else if ("Y" == me.edit.source)
		{
			me.edit.setupFilteredEdit(me.ui.home_edit_youtube_id, me.edit, "youtubeId", function(val)
			{
				fade(me.ui.home_edit_youtube_id_alert, (trim(val) == null));
			});
			show(me.ui.home_edit_youtube_id_alert, (trim(me.edit.youtubeId) == null));

			if (me.edit.ratio == null) me.edit.ratio = "16";
			me.edit.setupRadioEdit("home_edit_youtube_ratio", me.edit, "ratio");
		}
		else if ("A" == me.edit.source)
		{
			me.ui.editor.disable();
			me.ui.editor.set(me.edit.content);
			me.ui.editor.enable(function()
			{
				me.edit.set(me.edit, "content", me.ui.editor.get());
			}, false, main.options.fs);
		}
		else if ("F" == me.edit.source)
		{
			me.ui.filer.disable();
			if (me.edit.fileRefId !== undefined)
			{
				me.ui.filer.set({refId: me.edit.fileRefId, name: me.item.fileName});
			}
			else
			{
				me.ui.filer.set({url: me.edit.fileUrl});
			}
			me.ui.filer.enable(function()
			{
				var selectedMyFile = me.ui.filer.get();
				if (selectedMyFile.refId !== undefined)
				{
					me.edit.set(me.edit, "fileRefId", selectedMyFile.refId);
				}
				else
				{
					me.edit.set(me.edit, "fileUrl", selectedMyFile.url);
				}
				me.ui.home_edit_file_selected.text(selectedMyFile.name);
				fade(me.ui.home_edit_file_picker_alert, false);
			}, main.options.fs);
			if (me.item.fileName !== undefined)
			{
				me.ui.home_edit_file_selected.text(me.item.fileName);
			}
			else
			{
				me.ui.home_edit_file_selected.html(main.i18n.lookup("msg_noFileSelected", "<i>none</i>"));
			}
			show(me.ui.home_edit_file_picker_alert, (me.item.fileName == null));

			me.edit.setupFilteredEdit(me.ui.home_edit_file_alt, me.edit, "alt");
		}
		
 		if (me.item.id != -1) new e3_Attribution().inject(me.ui.home_edit_attribution, me.item);
 		show(me.ui.home_edit_attribution, (me.item.id != -1));
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
		params.url.item = me.item.id;
		me.edit.params("", params);
		main.portal.cdp.request("home_save home_get", params, function(data)
		{
			main.items = data.items || [];
			me.item = main.findItem(data.savedId);
			me.makeEdit();
			me.populate();
			if (deferred !== undefined) deferred();
		});
	};

	this.done = function()
	{
		if (me.edit.changed())
		{
			me.save(function(){main.startManage();});
		}
		else
		{
			main.startManage();
		}
	};

	this.goItem = function(item)
	{
		main.startEdit(item);
	};

	this.view = function()
	{
		console.log("view", me.edit);
	};
	
	this.extractYoutubeId = function(url)
	{
		var regExp = /^.*(youtu.be\/|v\/|u\/\w\/|embed\/|watch\?v=|\&v=)([^#\&\?]*).*/;
		var match = url.match(regExp);
		if (match && match[2].length == 11)
		{
			return trim(match[2]);
		}
		return trim(url);
	};
}

function HomeOptions(main)
{
	var me = this;
	this.ui = null;
	this.edit = null;

	this.init = function()
	{
		me.ui = findElements(["home_options_show", "home_options_numAnnc", "home_options_fullAnnc"]);
		me.ui.table = new e3_Table(me.ui.home_options_show);
		me.ui.table.enableReorder(me.applyOrder);
	};

	this.start = function()
	{
		main.onExit = me.checkExit;
		me.makeEdit();
		me.populate();
	};

	this.makeEdit = function()
	{
		me.edit = new e3_Edit(main.options, [], function(changed)
		{
			main.ui.modebar.enableSaveDiscard(changed ? me.saveCancel : null);
		});

		main.ui.modebar.enableSaveDiscard(null);
	};

	this.populate = function()
	{
		me.edit.setupRadioEdit("home_options_format", me.edit, "format");
		me.edit.setupFilteredEdit(me.ui.home_options_numAnnc, me.edit, "numAnnc");
		me.edit.setupCheckEdit(me.ui.home_options_fullAnnc, me.edit, "fullAnnc");

		me.ui.table.clear();
		$.each(me.edit.components, function(index, item)
		{
			var row = me.ui.table.row(); // 16+8 16+8 rest+8 (in 524)
			me.ui.table.reorder(main.i18n.lookup("msg_reorder", "Drag to Reorder"), item.id);

			var td = me.ui.table.input({type:"checkbox"}, null, {width:16});
			me.edit.setupCheckEdit(td.find("input"), item, "enabled");

			me.ui.table.text(item.title, null, {width:468});
		});
		me.ui.table.done();
	};

	this.applyOrder = function(order)
	{
		me.edit.order(me.edit, "components", order);
		me.adjustOrder();
	};
	
	this.adjustOrder = function()
	{
		$.each(me.edit.components, function(index, item)
		{
			if (item.order != index+1)
			{
				me.edit.set(item, "order", index+1);
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
		main.portal.cdp.request("home_saveOptions home_options", params, function(data)
		{
			main.options = data.options || {};

			// once saved, make sure check exit sees no changes, clear the UI from changed indications, and run deferred
			me.edit.revert();
			if (deferred !== undefined) deferred();
		});
	};	
}

function Home()
{
	var me = this;

	this.portal = null;
	this.i18n = new e3_i18n(home_i10n, "en-us");

	this.viewMode = null;
	this.manageMode = null;
	this.optionsMode = null;
	this.editMode = null;
	this.renderer = null;

	this.ui = null;
	this.onExit = null;

	this.itemStatus = {current:0, pending:1, unpublished:2, past:3};

	this.options = {};
	this.items = [];

	this.findItem = function(id)
	{
		for (index = 0; index < me.items.length; index++)
		{
			if (me.items[index].id == id)
			{
				return me.items[index];
			}			
		}
		
		return null;
	};

	this.itemPosition = function(item)
	{
		for (index = 0; index < me.items.length; index++)
		{
			if (me.items[index].id == item.id)
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
		me.i18n.localize();
		me.ui = findElements(["home_header", "home_modebar", "home_headerbar", "home_itemnav",
		                      "home_view", "home_view_edit",
		                      "home_manage","home_bar_manage", "home_header_manage",
		                      "home_options",
		                      "home_edit", "home_bar_edit",
		                      "home_manage_selectFirst"]);
		me.portal = portal_tool.features({onExit:function(deferred){return me.checkExit(deferred);}, pin:[{ui:me.ui.home_header}]});

		me.viewMode = new HomeView(me);
		me.viewMode.init();

		me.renderer = new HomeRenderer(me);
		me.renderer.init();

		if (me.portal.site.role >= Role.instructor)
		{
			me.ui.modebar = new e3_Modebar(me.ui.home_modebar);
			me.modes =
			[
				{name:me.i18n.lookup("mode_view", "View"), func:me.startView},
				{name:me.i18n.lookup("mode_edit", "Manage"), func:me.startManage},
				{name:me.i18n.lookup("mode_options", "Options"), func:me.startOptions}
			];
			me.ui.modebar.set(me.modes, 0);

			me.manageMode = new HomeManage(me);
			me.manageMode.init();
			
			me.optionsMode = new HomeOptions(me);
			me.optionsMode.init();

			me.editMode = new HomeEdit(me);
			me.editMode.init();

			show(me.ui.home_view_edit);
		}
		else
		{
			hide(me.ui.home_view_edit);
		}
	};

	this.start = function()
	{
		me.startView();
	};

	this.startView = function()
	{
		if (!me.checkExit(function(){me.startView();})) return;
		me.mode([me.ui.home_view]);
		if (me.ui.modebar !== undefined) me.ui.modebar.showSelected(0);
		me.viewMode.start();
	};

	this.startManage = function()
	{
		if (!me.checkExit(function(){me.startManage();})) return;
		me.mode([me.ui.home_header, me.ui.home_modebar, me.ui.home_headerbar, me.ui.home_bar_manage,  me.ui.home_header_manage, me.ui.home_manage]);
		me.ui.modebar.showSelected(1);
		me.manageMode.start();
	};

	this.startOptions = function()
	{
		if (!me.checkExit(function(){me.startOptions();})) return;
		me.mode([me.ui.home_header, me.ui.home_modebar, me.ui.home_options]);
		me.ui.modebar.showSelected(2);
		me.optionsMode.start();
	};
	
	this.startEdit = function(item)
	{
		if (!me.checkExit(function(){me.startEdit(item);})) return;
		me.mode([me.ui.home_header, me.ui.home_modebar, me.ui.home_headerbar, me.ui.home_itemnav, me.ui.home_bar_edit, me.ui.home_edit]);
		me.ui.modebar.showSelected(1);
		me.editMode.start(item);
	}

	this.mode = function(elements)
	{
		hide([me.ui.home_header, me.ui.home_modebar, me.ui.home_headerbar, me.ui.home_itemnav,
		      me.ui.home_view,
		      me.ui.home_manage, me.ui.home_bar_manage, me.ui.home_header_manage,
		      me.ui.home_options,
		      me.ui.home_edit, me.ui.home_bar_edit]);
		me.onExit = null;
		me.portal.resetScrolling();
		show(elements);
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
		home_tool = new Home();
		home_tool.init();
		home_tool.start();
	}
	catch (e)
	{
		error(e);
	}
});

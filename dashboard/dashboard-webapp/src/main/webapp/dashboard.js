/**********************************************************************************
 * $URL: https://source.etudes.org/svn/serenity/trunk/dashboard/dashboard-webapp/src/main/webapp/dashboard.js $
 * $Id: dashboard.js 12504 2016-01-10 00:30:08Z ggolden $
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

var dashboard_tool = null;

function Dashboard()
{
	var me = this;

	this.i18n = new e3_i18n(dashboard_i10n, "en-us");
	this.portal = null;
	this.ui = {};
	
	this.sites = [];
	this.announcements = [];
	this.news = [];
	this.events = [];
	this.eventsDate = null;
	this.eventsDays =
	{
		year: 0,
		month: 0,
		days: []
	};

	this.init = function()
	{
		me.i18n.localize();		
		me.ui = findElements(["dashboard_template_siteTab", "dashboard_template_siteTabClosed", "dashboard_template_siteTabWillOpen", "dashboard_etudesNews",
		                      "dashboard_announcements", "dashboard_annc_template", "dashboard_annc_template_nosite", "dashboard_calendar", "dashboard_siteTabs", "dashboard_noSiteTabs",
		                      "dashboard_view_annc", "dashboard_view_annc_subject", "dashboard_view_annc_byline", "dashboard_view_annc_content",
		                      "dashboard_events", "dashboard_events_none", "dashboard_events_header_date", "dashboard_events_header_tz", "dashboard_event_template",
		                      "dashboard_view_event", "dashboard_view_event_title", "dashboard_view_event_byline", "dashboard_view_event_content"]);		
		me.portal = portal_tool.features({});

		me.portal.timestamp.setCalendar(me.ui.dashboard_calendar, {onSelect: function(date)
		{
			me.loadEvents(date);
		}, beforeShowDay: me.checkForEventsOnDate, onChangeMonthYear: me.loadEventsDays});
	};
	
	this.start = function()
	{
		var params = me.portal.cdp.params();
		me.portal.cdp.request("dash_activity dash_events dash_eventsDays dash_announcements", params, function(data)
		{
			me.sites = data.sites;
			me.populateSiteTabs();

			me.eventsDate = data.eventsDate;
			me.events = data.events;
			me.populateEvents();

			me.news = data.news;
			me.populateNews();

			me.announcements = data.announcements;
			me.populateAnnouncements();
			
			me.eventsDays = data.eventsDays;
			me.portal.timestamp.refresh(me.ui.dashboard_calendar);
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
		var params = me.portal.cdp.params();
		params.url.date = date;
		me.portal.cdp.request("dash_events", params, function(data)
		{
			me.eventsDate = data.eventsDate;
			me.events = data.events;
			me.populateEvents();
		});
	};

	this.loadEventsDays = function(year, month)
	{
		var params = me.portal.cdp.params();
		params.url.year = year;
		params.url.month = month;
		me.portal.cdp.request("dash_eventsDays", params, function(data)
		{
			me.eventsDays = data.eventsDays;
			me.portal.timestamp.refresh(me.ui.dashboard_calendar);
		});
	};

	this.populateSiteTabs = function()
	{
		me.ui.dashboard_siteTabs.empty();

		$.each(me.sites, function(index, site)
		{
			if (site.accessStatus == AccessStatus.open)
			{
				me.populateOpen(site);	
			}
			else if (site.accessStatus == AccessStatus.willOpen)
			{
				me.populateWillOpen(site);
			}
			else // AccessStatus.closed
			{
				me.populateClosed(site);
			}
		});			

		show(me.ui.dashboard_noSiteTabs, (me.sites.length == 0));
	};

	this.populateOpen = function(site)
	{
		var tab = clone(me.ui.dashboard_template_siteTab,
				["dashboard_template_siteTab_body",
				"dashboard_template_siteTab_online_count",
				"dashboard_template_siteTab_message_count",
				"dashboard_template_siteTab_forum_count",
				"dashboard_template_siteTab_alert_count", "dashboard_template_siteTab_alert_bkg", "dashboard_template_siteTab_alert_title",
				"dashboard_template_siteTab_name", "dashboard_template_siteTab_dot",
				"dashboard_template_siteTab_normal", "dashboard_template_siteTab_hover", "dashboard_template_siteTab_view"]);

		onClick(tab.dashboard_template_siteTab_view, function(){me.visit(site.id);});
		var msg = me.i18n.lookup("msg_viewSite", "VIEW SITE %0", "html", [site.title]);
		// tab.dashboard_template_siteTab_view.attr("title", msg);
		tab.dashboard_template_siteTab_view.text(msg);

		if (site.activity.online != 0)
		{
			tab.dashboard_template_siteTab_online_count.text(me.badgeValue(site.activity.online));			
		}

		if (site.activity.unreadMessages != 0)
		{
			tab.dashboard_template_siteTab_message_count.text(me.badgeValue(site.activity.unreadMessages));
		}

		if (site.activity.unreadPosts != 0)
		{
			tab.dashboard_template_siteTab_forum_count.text(me.badgeValue(site.activity.unreadPosts));
		}

		// for instructors
		if ((site.role >= Role.instructor))
		{
			tab.dashboard_template_siteTab_alert_bkg.css("background-image","url('/ui/art/mark/alert.png')");
			tab.dashboard_template_siteTab_alert_title.text(me.i18n.lookup("msg_absent", "ABSENT"));
			if (site.activity.notVisitAlerts != 0)
			{
				tab.dashboard_template_siteTab_alert_count.text(me.badgeValue(site.activity.notVisitAlerts));
			}
		}
		else
		{
			tab.dashboard_template_siteTab_alert_bkg.css({backgroundImage: "url('/ui/art/mark/graded.png')"});
			
			tab.dashboard_template_siteTab_alert_title.text(me.i18n.lookup("msg_reviews", "REVIEWS"));
			if (site.activity.reviewCount != 0)
			{
				tab.dashboard_template_siteTab_alert_count.text(me.badgeValue(site.activity.reviewCount));
			}
		}

		tab.dashboard_template_siteTab_dot.append(dot(Dots.green));
		tab.dashboard_template_siteTab_name.text(site.title);

		onHover(tab.dashboard_template_siteTab_body,
				function()
				{
					tab.dashboard_template_siteTab_hover.stop().animate({opacity:1}, Hover.on);
				},
				function()
				{
					tab.dashboard_template_siteTab_hover.stop().animate({opacity:0}, Hover.off);
				});

		me.ui.dashboard_siteTabs.append(tab.dashboard_template_siteTab_body);
	};

	this.populateClosed = function(site)
	{
		var tab = clone(me.ui.dashboard_template_siteTabClosed, ["dashboard_template_siteTabClosed_body", "dashboard_template_siteTabClosed_name", "dashboard_template_siteTabClosed_dot",
		                                                         "dashboard_template_siteTabClosed_when", "dashboard_template_siteTabClosed_normal", "dashboard_template_siteTabClosed_hover",
		                                                         "dashboard_template_siteTabClosed_view"]);
	
		if ((site.role >= Role.instructor))
		{
			onClick(tab.dashboard_template_siteTabClosed_view, function(){me.visit(site.id);});
			var msg = me.i18n.lookup("msg_viewSite", "VIEW SITE %0", "html", [site.title]);
			// tab.dashboard_template_siteTabClosed_view.attr("title", msg);
			tab.dashboard_template_siteTabClosed_view.text(msg);

			tab.dashboard_template_siteTabClosed_body.css("cursor", "pointer");
			
			onHover(tab.dashboard_template_siteTabClosed_body,
					function()
					{
						tab.dashboard_template_siteTabClosed_hover.stop().animate({opacity:1}, Hover.on);
					},
					function()
					{
						tab.dashboard_template_siteTabClosed_hover.stop().animate({opacity:0}, Hover.off);
					});
		}
		else
		{
			hide(tab.dashboard_template_siteTabClosed_hover);
		}

		tab.dashboard_template_siteTabClosed_dot.append(dot(Dots.red));
		tab.dashboard_template_siteTabClosed_name.text(site.title);

		if (site.unpublishOn !== undefined)
		{
			tab.dashboard_template_siteTabClosed_when.text(me.portal.timestamp.display(site.unpublishOn));
		}

		me.ui.dashboard_siteTabs.append(tab.dashboard_template_siteTabClosed_body);
	};

	this.populateWillOpen = function(site)
	{
		var tab = clone(me.ui.dashboard_template_siteTabWillOpen, ["dashboard_template_siteTabWillOpen_body", "dashboard_template_siteTabWillOpen_name", "dashboard_template_siteTabWillOpen_dot",
		                                                           "dashboard_template_siteTabWillOpen_when", "dashboard_template_siteTabWillOpen_normal",
		                                                           "dashboard_template_siteTabWillOpen_hover", "dashboard_template_siteTabWillOpen_view"]);
		
		if ((site.role >= Role.instructor))
		{
			onClick(tab.dashboard_template_siteTabWillOpen_view, function(){me.visit(site.id);});
			var msg = me.i18n.lookup("msg_viewSite", "VIEW SITE %0", "html", [site.title]);
			// tab.dashboard_template_siteTabWillOpen_view.attr("title", msg);
			tab.dashboard_template_siteTabWillOpen_view.text(msg);

			tab.dashboard_template_siteTabWillOpen_body.css("cursor", "pointer");
			
			onHover(tab.dashboard_template_siteTabWillOpen_body,
					function()
					{
						tab.dashboard_template_siteTabWillOpen_hover.stop().animate({opacity:1}, Hover.on);
					},
					function()
					{
						tab.dashboard_template_siteTabWillOpen_hover.stop().animate({opacity:0}, Hover.off);
					});
		}
		else
		{
			hide(tab.dashboard_template_siteTabWillOpen_hover);
		}

		tab.dashboard_template_siteTabWillOpen_dot.append(dot(Dots.yellow));
		tab.dashboard_template_siteTabWillOpen_name.text(site.title);
		tab.dashboard_template_siteTabWillOpen_when.text(me.portal.timestamp.display(site.publishOn));

		me.ui.dashboard_siteTabs.append(tab.dashboard_template_siteTabWillOpen_body);
	};

	this.badgeValue = function(value)
	{
		if (value < 1000)
		{
			return value.toString();
		}
		return me.i18n.lookup("msg_tooMany", "!!!");
	};

	this.visit = function(siteId)
	{
		me.portal.navigate(siteId, null, false);
	};

	this.populateEvents = function()
	{
//		$("#dashboard_timezone").text(me.portal.timestamp.displayTz());
		me.ui.dashboard_events_header_date.text(me.portal.timestamp.displayDate(me.eventsDate));
		me.ui.dashboard_events_header_tz.text(me.portal.timestamp.displayTz());
		me.ui.dashboard_events.empty();

		$.each(me.events, function(index, event)
		{
			var tab = clone(me.ui.dashboard_event_template, ["dashboard_event_template_body", "dashboard_event_template_time", "dashboard_event_template_ampm", "dashboard_event_template_title",
			                                                 "dashboard_event_template_normal", "dashboard_event_template_hover", "dashboard_event_template_view"]);
			tab.dashboard_event_template_time.text(me.portal.timestamp.displayTime(event.dateStart));
			tab.dashboard_event_template_ampm.text(me.portal.timestamp.displayAmPm(event.dateStart));
			tab.dashboard_event_template_title.text(event.title);
			onClick(tab.dashboard_event_template_view, function(){me.showEvent(event);});

			onHover(tab.dashboard_event_template_body,
					function()
					{
						tab.dashboard_event_template_hover.stop().animate({opacity:1}, Hover.on);
					},
					function()
					{
						tab.dashboard_event_template_hover.stop().animate({opacity:0}, Hover.off);
					});

			me.ui.dashboard_events.append(tab.element.children());
		});

		show(me.ui.dashboard_events_none, (me.events.length == 0));
	};

	this.populateAnnouncements = function()
	{
		me.ui.dashboard_announcements.empty();
		$.each(me.announcements, function(index, annc)
		{
			var tab = clone(me.ui.dashboard_annc_template, ["dashboard_annc_template_body", "dashboard_annc_template_subject", "dashboard_annc_template_site", "dashboard_annc_template_content",
			                                                  "dashboard_annc_template_normal", "dashboard_annc_template_hover", "dashboard_annc_template_view"]);
			if (index == 0)
			{
				tab.dashboard_annc_template_body.css("border", "none");
			}

			onClick(tab.dashboard_annc_template_view, function(){me.showAnnouncement(annc);});

			if (annc.site !== undefined) tab.dashboard_annc_template_site.text(annc.site.title);
			show(tab.dashboard_annc_template_site, (annc.site !== undefined));

			tab.dashboard_annc_template_subject.text(annc.title);
			tab.dashboard_annc_template_content.html(annc.content);

			onHover(tab.dashboard_annc_template_body,
					function()
					{
						tab.dashboard_annc_template_hover.stop().animate({opacity:1}, Hover.on);
					},
					function()
					{
						tab.dashboard_annc_template_hover.stop().animate({opacity:0}, Hover.off);
					});

			me.ui.dashboard_announcements.append(tab.element.children());
		});
	};

	this.populateNews = function()
	{
		me.ui.dashboard_etudesNews.empty();
		$.each(me.news, function(index, annc)
		{
			var tab = clone(me.ui.dashboard_annc_template, ["dashboard_annc_template_body", "dashboard_annc_template_subject", "dashboard_annc_template_site", "dashboard_annc_template_content",
			                                                  "dashboard_annc_template_normal", "dashboard_annc_template_hover", "dashboard_annc_template_view"]);
			if (index == 0)
			{
				tab.dashboard_annc_template_body.css("border", "none");
			}	

			onClick(tab.dashboard_annc_template_view, function(){me.showAnnouncement(annc);});

			hide(tab.dashboard_annc_template_site);	
			tab.dashboard_annc_template_subject.text(annc.title);
			tab.dashboard_annc_template_content.html(annc.content);
	
			onHover(tab.dashboard_annc_template_body,
					function()
					{
						tab.dashboard_annc_template_hover.stop().animate({opacity:1}, Hover.on);
					},
					function()
					{
						tab.dashboard_annc_template_hover.stop().animate({opacity:0}, Hover.off);
					});
	
			me.ui.dashboard_etudesNews.append(tab.element.children());
		});
	};

	this.showAnnouncement = function(annc)
	{
		var btns = [];
		me.ui.dashboard_view_annc_subject.text(annc.title);
		if ((annc.createdBy != null) && (annc.bylineDate != null) && (annc.site != null))
		{
			me.ui.dashboard_view_annc_byline.text(me.i18n.lookup("msg_anncByline", "%0, %1", "html", [annc.createdBy, me.portal.timestamp.display(annc.bylineDate)]));
			btns.push({text:me.i18n.lookup("msg_viewSite", "VIEW SITE %0", "html", [annc.site.title]), click:function()
			{
				me.closeShowAnnouncement();
				me.visit(annc.site.id);}
			});
		}
		else
		{
			me.ui.dashboard_view_annc_byline.text(me.i18n.lookup("msg_anncBylineMOTD", "System Message"));
		}
		me.ui.dashboard_view_annc_content.html(annc.content);

		me.portal.dialogs.openAlert(me.ui.dashboard_view_annc, null, btns);
	};
	
	this.closeShowAnnouncement = function()
	{
		me.portal.dialogs.close(me.ui.dashboard_view_annc);
	}
	
	this.showEvent = function(event)
	{
		var tool = Tools.byId(event.tool);
		var toolTitle = ((tool == null) ? "???" : tool.title); // TODO: run through i18n

		var  toolItemType = ToolItemType.byId(event.itemType);
		var itemTitle = ((toolItemType == null) ? "???" : toolItemType.title); // TODO: run through i18n

		me.ui.dashboard_view_event_title.text(event.title);

		if (event.dateEnd != null)
		{
			me.ui.dashboard_view_event_byline.text(me.i18n.lookup("msg_eventByline2dates", "%0: %1 - %2", "html", [toolTitle, me.portal.timestamp.display(event.dateStart), me.portal.timestamp.display(event.dateEnd)]))
		}
		else
		{
			me.ui.dashboard_view_event_byline.text(me.i18n.lookup("msg_eventByline1date", "%0: %1", "html", [toolTitle, me.portal.timestamp.display(event.dateStart)]));
		}

		if (event.content != null) me.ui.dashboard_view_event_content.html(event.content);
		show(me.ui.dashboard_view_event_content, (event.content != null));

		var btns = [];
		btns.push({text:me.i18n.lookup("msg_goto", "VIEW %0", "html", [itemTitle]), click:function()
		{
			me.closeShowEvent();
			me.portal.navigate(event.site.id, event.tool, false, false);
		}});
		if (event.site.role >= Role.instructor)
		{
			btns.push({text:me.i18n.lookup("msg_edit", "EDIT %0", "html", [itemTitle]), click:function()
			{
				me.closeShowEvent();
				me.portal.navigate(event.site.id, event.tool, false, false);
			}});
		}
		btns.push({text:me.i18n.lookup("msg_viewSite", "VIEW %0", "html", [event.site.title]), click:function()
		{
			me.closeShowEvent();
			me.visit(event.site.id);
		}});

		me.portal.dialogs.openAlert(me.ui.dashboard_view_event, null, btns);
	};
	
	this.closeShowEvent = function()
	{
		me.portal.dialogs.close(me.ui.dashboard_view_event);
	};
}

$(function()
{
	try
	{
		dashboard_tool = new Dashboard();
		dashboard_tool.init();
		dashboard_tool.start();
	}
	catch (e)
	{
		error(e);
	}
});

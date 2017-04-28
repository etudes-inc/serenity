/**********************************************************************************
 * $URL: https://source.etudes.org/svn/serenity/trunk/ui/ui-webapp/src/main/webapp/serenity.js $
 * $Id: serenity.js 12553 2016-01-14 20:03:28Z ggolden $
 ***********************************************************************************
 *
 * Copyright (c) 2014, 2015, 2016 Etudes, Inc.
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

// how the tool code integrates into the portal
var portal_tool = null;

function PortalPresence(main)
{
	var me = this;

	this.presenceTimer == null;
	this.presenceInterval = 30 * 1000;

	this.loadPresence = function(firstVisit)
	{
		var params = main.cdp.params();
		params.url.site = main.info.site.id;
		if ((firstVisit != undefined) && (firstVisit == true))
		{
			params.url.track = "1";
		}
		main.cdp.request("portal_presence", params, function(data)
		{
			// Note: session might have timed out and lost our current site
			if (main.info.site != null)
			{
				main.info.site.oldPresence = main.info.site.presence;
				main.info.site.presence = data.presence || [];
				main.nav.populateOnline();
			}
		});
	};
	
	this.init = function()
	{		
	};

	this.start = function(firstVisit)
	{
		main.info.site.presence = [];
		main.info.site.oldPresence = [];

		me.loadPresence(firstVisit);
		me.presenceTimer = setInterval(function(){try{me.loadPresence();}catch (e){error(e);}}, me.presenceInterval);
	};
	
	this.stop = function()
	{
		if (me.presenceTimer != null)
		{
			clearInterval(me.presenceTimer);
			me.presenceTimer = null;
		}
	};
}

function PortalPicker(main)
{
	var me = this;
	
	this.ui = null;

	this.init = function()
	{
		me.ui = findElements(["e3_FilerCK_dialog","e3_FilerCK_dialog_filer"]);
		me.ui.filer = new e3_FilerCK(me.ui.e3_FilerCK_dialog_filer);
	};

	this.pick = function(onChange, fs, type)
	{
		main.dialogs.openDialogButtons(me.ui.e3_FilerCK_dialog, [], function()
		{
			me.ui.filer.disable();
		});

		me.ui.filer.disable();
		me.ui.filer.enable(function()
		{
			if (onChange !== undefined) try {onChange(me.ui.filer.get());} catch (err) {error (err);}
			main.dialogs.close(me.ui.e3_FilerCK_dialog);
		}, fs, type);
	};
}

function PortalInfo(main)
{
	var me = this;
	
	this.user = null;
	this.sites = [];
	this.site = null;

	this.isAdmin = function()
	{
		return (me.user.admin);
	};

	this.isHelpdesk = function()
	{
		return (me.user.helpdesk);
	};

	this.onAdmin = function()
	{
		return (me.site.id == 1);
	};

	this.onHelpdesk = function()
	{
		return (me.site.id == 3);
	};

	this.findSite = function(siteId)
	{
		var found = null;
		$.each(me.sites || [], function(index, site)
		{
			if (site.id == siteId) found = site;
		});

		return found;
	};

	this.findTool = function(toolId)
	{
		if ((me.site == null) || (toolId == null)) return null;
		
		var tool = Tools.byId(toolId);

		// make sure its in the site, except for setup and roster
		if ((tool != Tools.sitesetup) && (tool != Tools.siteroster))
		{
			var found = false;
			$.each(me.site.tools || [], function(index, id)
			{
				if (id == toolId) found = true;
			});
			if (!found) tool = null;
		}
		
		return tool;
	};

	this.firstTool = function()
	{
		for (var i = 0; i < (me.site.tools || []).length; i++)
		{
			var t = Tools.byId(me.site.tools[i]);

			// skip tools the user cannot see
			if (t.role > me.site.role) continue;

			return t;
		};

		return null;
	};

	this.init = function()
	{
		me.user = null;
		me.sites = [];
		me.site = null;
	};
}

function PortalAuth(main)
{
	var me = this;

	this.authenticationToken = null;
	this.authCheckTimer = null;
	this.authCheckInterval = 10 * 1000;

	this.readCookie = function(name)
	{
		var nameEQ = name + "=";
		var ca = document.cookie.split(';');
		for (var i=0;i < ca.length;i++)
		{
			var c = ca[i];
			while (c.charAt(0)==' ') c = c.substring(1,c.length);
			if (c.indexOf(nameEQ) == 0) return c.substring(nameEQ.length,c.length);
		}
		return null;
	};

	this.init = function()
	{
		// watch for changes in authentication
		me.authCheckTimer = setInterval(function(){try{me.noticeAuthenticationChange();}catch(e){error(e);}}, me.authCheckInterval);
	};

	this.noticeAuthenticationChange = function()
	{
		var current = me.readCookie(main.tokenId);

		// if we lost authentication - respond like a session timeout / logout
		if ((me.authenticationToken != null) && (current == null))
		{
			main.setLoggedOut();
			return true;
		}
		
		// if we gained authentication, or changed - respond like a start
		else if (((me.authenticationToken == null) && (current != null)) || (me.authenticationToken != current))
		{
			me.checkAuthorization();
			return true;
		}
		
		return false;
	};

	this.checkAuthorization = function()
	{
		// the site we will return to on refresh
		var siteId = main.session.get("portal:site");
		
		var params = main.cdp.params();
		if (siteId != null) params.post.extraSite = siteId;

		main.loggingIn = true;
		main.cdp.request("checkAuth portal_info", params, function(data)
		{
			main.loggingIn = false;
			if (0 == data["cdp:status"])
			{
				main.setLoggedIn(data);
			}
			else
			{
				main.setLoggedOut();
			}
		});
	};

	this.setAuthentication = function()
	{
		me.authenticationToken = me.readCookie(main.tokenId);
	};
	
	this.clearAuthentication = function()
	{
		// drop the cookie
		document.cookie = "EtudesToken=; expires=Thu, 01 Jan 1970 00:00:00 GMT; domain=" + main.cdp.domain + "; path=/";
		me.authenticationToken = null;
	};
}

function PortalDnD(main)
{
	var me = this;
	// this.lastDropEventType = null;
	
	this.init = function()
	{
		// block drops
		// $(document).on('dragstart', function(e){return me.dropBlocker(e);});
		// $(document).on('drag', function(e){return me.dropBlocker(e);});
		// $(document).on('dragenter', function(e){return me.dropBlocker(e);});
		// $(document).on('dragleave', function(e){return me.dropBlocker(e);});
		// $(document).on('dragend', function(e){return me.dropBlocker(e);});		
		$(document).on('dragover', function(e){return me.dropBlocker(e);});
		$(document).on('drop', function(e){return me.dropBlocker(e);});
	};
	
	this.dropBlocker = function(e)
	{
		// if ((me.lastDropEventType == null) || (me.lastDropEventType != e.type)) console.log(e);
		// me.lastDropEventType = e.type;

		e.stopPropagation();
		e.preventDefault();

		return false;
	};
}

function PortalScrollDetective(main)
{
	var me = this;
	this.onScroll = null;
	this.pins = [];
	this.portalPins = [];

	this.init = function()
	{
		me.pins = me.portalPins;
		$(window).off("scroll").on("scroll", function(event)
		{
			var info = {scrollTop: $(window).scrollTop(), scrollBottom: $(window).scrollTop() + $(window).height()};
		    if (me.onScroll != null) me.onScroll(info);
		    $.each(me.pins, function(index, item){me.pin(info, item);});		    	
		});
	};

	this.setPins = function(pins)
	{
		me.pins = me.portalPins;
		$.each(pins || [], function(index, item)
		{
			var pin = {target: (($.type(item.ui) === "string") ? $("#" + item.ui) : item.ui), pinned:null, top:50, onPin:item.onPin, onAdjust:item.onAdjust};
			me.pins.push(pin);
		});
	};

	this.setPortalPins = function(pins)
	{
		me.portalPins = [];
		$.each(pins || [], function(index, item)
		{
			var pin = {target: (($.type(item.ui) === "string") ? $("#" + item.ui) : item.ui), pinned:null, top:0, onPin:item.onPin, onAdjust:item.onAdjust};
			me.portalPins.push(pin);
		});
		me.setPins([]);
	};

	// for each thing being pinned {target: pinned: #}
	this.pin = function(info, item)
	{
		// ignore offstage
		if (item.target.hasClass("e3_offstage")) return;

		if (item.pinned == null)
		{
			// if we bump into the top
			if ((info.scrollTop + item.top) >= item.target.position().top)
			{
				// we are adjusting now - record the real top position
				item.pinned = item.target.position().top;
				if (item.onPin !== undefined) item.onPin(true);
			}
		}

		// we are adjusting
		else
		{
			// if we no longer need to adjust
			if ((info.scrollTop + item.top) < item.pinned)
			{
				me.unpin(item);
			}
		}
		
		// if pinning, pin
		if (item.pinned != null)
		{
			var adjustBy = (info.scrollTop + item.top) - item.pinned;
			item.target.css({top:adjustBy});
			if (item.onAdjust !== undefined) item.onAdjust(info);
		}
	};
	
	this.unpin = function(item)
	{
		item.pinned = null;
		item.target.css({top:0});
		if (item.onPin !== undefined) item.onPin(false);
	};

	this.reset = function()
	{
		 $.each(me.pins, function(index, item){me.unpin(item);});
	};
}

function PortalNav(main)
{
	var me = this;

	this.bannerTools = [Tools.dashboard, Tools.sites, Tools.files, Tools.account, {id: -2, title: "Help"}, Tools.logout];

	this.toolReturn = null;
	this.toolExit = null;

	this.populateUserNav = function()
	{
		main.ui.portal_usernav.empty();

		// not for admin / helpdesk TODO:
//		if (me.isAdmin() || me.isHelpdesk()) return;

		$.each(me.bannerTools || [], function(index, tool)
		{
			var entry = clone(main.ui.portal_usernavTemplate, ["portal_usernavTemplateA"]);
			show(entry.element);
			main.ui.portal_usernav.append(entry.element);
			// if (tool == Tools.dashboard) entry.portal_usernavTemplateA.css("background-image", "url('/ui/art/icon/dashboard.png')");

			var title = main.i18n.lookup(tool.title, tool.title);
			entry.portal_usernavTemplateA.text(title);
			
			if (tool.id == -1) // logout
			{
				onClick(entry.portal_usernavTemplateA, function(){me.logoutClicked();});
			}
			else if (tool.id == -2) // help
			{
				entry.portal_usernavTemplateA.attr({href: "http://etudes.org/help/", target: "_blank"});
			}
			else
			{
				onClick(entry.portal_usernavTemplateA, function(){me.navigate(null, tool, false, false);});
			}
			entry.portal_usernavTemplateA.attr("oid", tool.id);
		
			onHover(entry.portal_usernavTemplateA,
					function(){entry.portal_usernavTemplateA.stop().animate({color:"#000000", backgroundColor:"#FFFFFF"}, Hover.quick);},
					function(){entry.portal_usernavTemplateA.stop().animate({color:"#FFFFFF", backgroundColor:"#000000"}, Hover.quick);});
		});
	};

	this.populateOnline = function()
	{
		main.ui.portal_onlineList.empty();

		$.each(main.info.site.presence || [], function(index, user)
		{
			var entry = clone(main.ui.portal_onlineTemplate, ["portal_onlineTemplateA"]);
			entry.portal_onlineTemplateA.text(user.nameDisplay);
			show(entry.element);
			main.ui.portal_onlineList.append(entry.element);
		});

		main.ui.portal_online_count.text(main.info.site.presence.length);
	};

	this.populateInSiteBanner = function(site)
	{
		show([main.ui.portal_menuName, main.ui.portal_online]);
		main.ui.portal_menu.empty();

		var am = false;
		$.each(site.tools, function(index, id)
		{
			// AM goes in admin tools
			if (id == Tools.activity.id)
			{
				am = true;
				return;
			}

			var t = Tools.byId(id);

			var	entry = clone(main.ui.portal_menuTemplate, ["portal_menuTemplateA"]);
			onHover(entry.portal_menuTemplateA,
					function(){entry.portal_menuTemplateA.stop().animate({color:"#000000", backgroundColor:"#FFFFFF"}, Hover.quick);},
					function(){entry.portal_menuTemplateA.stop().animate({color:"#FFFFFF", backgroundColor:"#000000"}, Hover.quick);});
			show(entry.element);
			main.ui.portal_menu.append(entry.element);
			entry.portal_menuTemplateA.text(main.i18n.lookup(t.title, t.title));
			onClick(entry.portal_menuTemplateA, function(){me.navigate(site, t, false, false);});				
			entry.portal_menuTemplateA.attr("oid", t.id);
		});
		
		if (site.role >= Role.instructor)
		{
			var tools = [];
			if (am) tools.push(Tools.activity);
			tools.push(Tools.sitesetup);
			tools.push(Tools.siteroster);

			var entry = clone(main.ui.portal_menuLinkDividerTATemplate, []);
			show(entry.element);
			main.ui.portal_menu.append(entry.element);

			$.each(tools, function(index, tool)
			{
				var entry = clone(main.ui.portal_menuAdminTemplate, ["portal_menuAdminTemplateA"]);
				onHover(entry.portal_menuAdminTemplateA,
						function(){entry.portal_menuAdminTemplateA.stop().animate({color:"#DDDDDD", backgroundColor:"#333333"}, Hover.quick);},
						function(){entry.portal_menuAdminTemplateA.stop().animate({color:"#333333", backgroundColor:"#DDDDDD"}, Hover.quick);});
				show(entry.element);
				main.ui.portal_menu.append(entry.element);
				entry.portal_menuAdminTemplateA.text(main.i18n.lookup(tool.title, tool.title));
				onClick(entry.portal_menuAdminTemplateA, function(){me.navigate(site, tool, false, false);});				
				entry.portal_menuAdminTemplateA.attr("oid", tool.id);
			});
		}
		
		if (site.links.length > 0)
		{
			if (site.role >= Role.instructor)
			{
				var entry = clone(main.ui.portal_menuLinkDividerALTemplate, []);
				show(entry.element);
				main.ui.portal_menu.append(entry.element);
			}
			else
			{
				var entry = clone(main.ui.portal_menuLinkDividerTLTemplate, []);
				show(entry.element);
				main.ui.portal_menu.append(entry.element);
			}
	
			$.each(site.links || [], function(index, link)
			{
				var entry = clone(main.ui.portal_menuLinkTemplate, ["portal_menuLinkTemplateA"]);
				show(entry.element);
				main.ui.portal_menu.append(entry.element);
				entry.portal_menuLinkTemplateA.html(link.title);
				entry.portal_menuLinkTemplateA.attr({href:link.url});
				onHover(entry.portal_menuLinkTemplateA,
						function(){entry.portal_menuLinkTemplateA.stop().animate({color:"#FFFFFF", backgroundColor:"#686868"}, Hover.quick);},
						function(){entry.portal_menuLinkTemplateA.stop().animate({color:"#686868", backgroundColor:"#FFFFFF"}, Hover.quick);});
			});
		}
	};
	
	this.hideInSiteBanner = function()
	{
		hide([main.ui.portal_menuName, main.ui.portal_online]);
	};

	this.userNavShowing = false;
	this.toggleUserNav = function(setting)
	{
		if (setting == me.userNavShowing) return;

		if (setting)
		{
			me.userNavShowing = true;
			// TODO: done in the hover now: main.ui.portal_userName.stop().animate({color:"white"}, Hover.on);

			main.ui.portal_usernav.css({opacity:0, top:main.ui.portal_userName.offset().top + main.ui.portal_userName.outerHeight() + 5, left:main.ui.portal_userName.offset().left});
			show(main.ui.portal_usernav);
			main.ui.portal_usernav.stop().animate({opacity:1}, Hover.on);

			me.toggleSiteNav(false);
			me.toggleOnline(false);
			me.toggleMenu(false);
			me.toggleContextMenu(false);
		}
		else
		{
			me.userNavShowing = false;
			// TODO: done in the hover now: main.ui.portal_userName.stop().animate({color:"#B3B3B3"}, Hover.off);
			main.ui.portal_usernav.stop().animate({opacity:0}, Hover.off, function(){hide(main.ui.portal_usernav);});
		}
	};

	this.adjustUserNav = function()
	{
		if (me.userNavShowing)
		{
			main.ui.portal_usernav.css({top:main.ui.portal_userName.offset().top + main.ui.portal_userName.outerHeight() + 5});
		}
	};

	this.siteNavShowing = false;
	this.toggleSiteNav = function(setting)
	{
		if (setting == me.siteNavShowing) return;

		if (setting)
		{
			me.siteNavShowing = true;
			// TODO: done in the hover now: main.ui.portal_siteName.stop().animate({color:"white"}, Hover.on);
			
			main.ui.portal_sitenav.css({opacity:0, top:main.ui.portal_siteName.offset().top + main.ui.portal_siteName.outerHeight() + 5, left:main.ui.portal_siteName.offset().left});
			show(main.ui.portal_sitenav);
			main.ui.portal_sitenav.stop().animate({opacity:1}, Hover.on);

			me.toggleUserNav(false);
			me.toggleOnline(false);
			me.toggleMenu(false);
			me.toggleContextMenu(false);
		}
		else
		{
			me.siteNavShowing = false;
			// TODO: done in the hover now: main.ui.portal_siteName.stop().animate({color:"#B3B3B3"}, Hover.off);
			main.ui.portal_sitenav.stop().animate({opacity:0}, Hover.off, function(){hide(main.ui.portal_sitenav);});
		}
	};

	this.adjustSiteNav = function()
	{
		if (me.siteNavShowing)
		{
			main.ui.portal_sitenav.css({top:main.ui.portal_siteName.offset().top + main.ui.portal_siteName.outerHeight() + 5});
		}
	};

	this.onlineShowing = false;
	this.toggleOnline = function(setting)
	{
		if (setting == me.onlineShowing) return;

		if (setting)
		{
			me.onlineShowing = true;
			main.ui.portal_online.stop().animate({color:"white"}, Hover.on);

			main.ui.portal_onlineList.css({opacity:0, top:main.ui.portal_online.offset().top + main.ui.portal_online.outerHeight() + 5, left:main.ui.portal_online.offset().left});
			show(main.ui.portal_onlineList);
			main.ui.portal_onlineList.stop().animate({opacity:1}, Hover.on);

			me.toggleUserNav(false);
			me.toggleSiteNav(false);
			me.toggleMenu(false);
			me.toggleContextMenu(false);
		}
		else
		{
			me.onlineShowing = false;
			main.ui.portal_online.stop().animate({color:"#B3B3B3"}, Hover.off);
			main.ui.portal_onlineList.stop().animate({opacity:0}, Hover.off, function(){hide(main.ui.portal_onlineList);});
		}
	};

	this.adjustOnline = function()
	{
		if (me.onlineShowing)
		{
			main.ui.portal_onlineList.css({top:main.ui.portal_online.offset().top + main.ui.portal_online.outerHeight() + 5});
		}
	};

	this.populateContextMenu = function()
	{
		main.ui.portal_contextMenu.empty();
		$.each(me.contextMenu.menu, function(index, menuItem)
		{
			if ((menuItem.condition !== undefined) && !menuItem.condition) return;

			var entry = clone(main.ui.portal_contextMenuTemplate, ["portal_portal_contextMenuTemplateA"]);
			show(entry.element);
			main.ui.portal_contextMenu.append(entry.element);

			entry.portal_portal_contextMenuTemplateA.css({minWidth: me.contextMenu.below.outerWidth() - 32});
//			if (me.contextMenu.right !== undefined) entry.portal_portal_contextMenuTemplateA.css({textAlign: "right"});

			if (menuItem.title == null)
			{
				// entry.portal_portal_contextMenuTemplateA.text("--------");
				entry.portal_portal_contextMenuTemplateA.append($("<hr style='display:inline-block; color:white; width:100%;' />"));
				onClick(entry.portal_portal_contextMenuTemplateA, function(){});
			}
			else
			{
				entry.portal_portal_contextMenuTemplateA.text(menuItem.title);
				onClick(entry.portal_portal_contextMenuTemplateA, function(){menuItem.action();});

				onHover(entry.portal_portal_contextMenuTemplateA,
						function(){entry.portal_portal_contextMenuTemplateA.stop().animate({color:"#686868", backgroundColor:"#FFFFFF"}, Hover.quick);},
						function(){entry.portal_portal_contextMenuTemplateA.stop().animate({color:"#FFFFFF", backgroundColor:"#686868"}, Hover.quick);});
			}
		});
	};

	this.contextMenu = null;
	this.toggleContextMenu = function(setting, menu)
	{
		// deal with setting = false to close the menu
		if (setting == false)
		{
			if (me.contextMenu != null)
			{
				me.contextMenu = null;
				main.ui.portal_contextMenu.stop().animate({opacity:0}, Hover.off, function(){hide(main.ui.portal_contextMenu); main.ui.portal_contextMenu.empty();});
			}
			return;
		}

		// if the menu is a different one from what we have open - close it
		if ((me.contextMenu != null) && (me.contextMenu.below != menu.below))
		{
			me.contextMenu = null;
			main.ui.portal_contextMenu.stop().animate({opacity:0}, Hover.off, function(){hide(main.ui.portal_contextMenu); main.ui.portal_contextMenu.empty();});			
		}

		// if asking to open the same open menu, ignore
		if (me.contextMenu != null) return;

		// open the new menu
		me.contextMenu = menu;
		me.populateContextMenu();

//		main.ui.portal_online.stop().animate({color:"white"}, Hover.on);

		show(main.ui.portal_contextMenu);
		main.ui.portal_contextMenu.css({opacity: 0, top: me.contextMenu.below.offset().top + me.contextMenu.below.outerHeight(), left: me.contextMenu.below.offset().left - (main.ui.portal_contextMenu.outerWidth() - me.contextMenu.below.outerWidth())});
//		if (menu.right !== undefined)
//		{
//			main.ui.portal_contextMenu.css({opacity: 0, top: me.contextMenu.below.offset().top + me.contextMenu.below.outerHeight(),
//				left: me.contextMenu.below.offset().left - (main.ui.portal_contextMenu.outerWidth() - me.contextMenu.below.outerWidth() - 16)});
//		}
//		else
//		{
//			main.ui.portal_contextMenu.css({opacity: 0, top: me.contextMenu.below.offset().top + me.contextMenu.below.outerHeight(), left: me.contextMenu.below.offset().left - 16});
//		}

		main.ui.portal_contextMenu.stop().animate({opacity:1}, Hover.on);

		me.toggleUserNav(false);
		me.toggleSiteNav(false);
		me.toggleMenu(false);
		me.toggleOnline(false);
	};

	this.adjustContextMenu = function()
	{
		if (me.contextMenu != null)
		{
			main.ui.portal_contextMenu.css({top:me.contextMenu.below.offset().top + me.contextMenu.below.outerHeight()});
		}
	};

	this.menuShowing = false;
	this.menuFullyVisible = null;
	this.toggleMenu = function(setting)
	{
		if (setting == me.menuShowing) return;

		if (!me.menuShowing)
		{
			me.menuShowing = true;
			main.ui.portal_menuName.stop().animate({color:"white"}, Hover.on);

			main.ui.portal_menu.css({opacity:0, top:main.ui.portal_menuName.offset().top + main.ui.portal_menuName.outerHeight() + 5, left:main.ui.portal_menuName.offset().left});
			show(main.ui.portal_menu);
			main.ui.portal_menu.stop().animate({opacity:1}, Hover.on);

			me.toggleUserNav(false);
			me.toggleSiteNav(false);
			me.toggleOnline(false);
			me.toggleContextMenu(false);
		}
		else
		{
			me.menuShowing = false;
			me.menuFullyVisible = null;
			main.ui.portal_menuName.stop().animate({color:"#B3B3B3"}, Hover.off);
			main.ui.portal_menu.stop().animate({opacity:0}, Hover.off, function(){hide(main.ui.portal_menu);});
		}
	};

	this.adjustMenu = function(info)
	{
		if (me.menuShowing)
		{
			if ((me.menuFullyVisible == null) && (info != null))
			{
				me.menuFullyVisible = (main.ui.portal_menu.offset().top + main.ui.portal_menu.height() < info.scrollBottom);
			}

			if ((info == null) || (me.menuFullyVisible === true))
			{
				main.ui.portal_menu.css({top:main.ui.portal_menuName.offset().top + main.ui.portal_menuName.outerHeight() + 5});
			}
		}
	};

	this.adjustDropdowns = function(info)
	{
		me.adjustUserNav();
		me.adjustSiteNav();
		me.adjustOnline();
		me.adjustMenu(info);
		me.adjustContextMenu();
	};

	this.siteClicked = function(site)
	{
		// for the current site TODO: navigate anyway?
		if ((main.info.site != null) && (main.info.site.id == site.id))
		{
		}

		else
		{
			me.navigate(site, null, false, false);
		}
	};

	this.logoutClicked = function()
	{
		if (!me.leaveTool(function(){me.logoutClicked();})) return;
		main.setLoggedOut();
	};

	this.clearToolReturn = function()
	{
		me.toolReturn = null;
	};
	
	this.enableToolReturn = function(site, toolId)
	{
		me.toolReturn = {site:site, toolId:toolId};
	};

	this.leaveTool = function(deferred)
	{
		if (me.toolExit != null)
		{
			try
			{
				if (!me.toolExit(deferred))
				{
					return false;
				}
				me.toolExit = null;
			}
			catch (err)
			{
				error(err);
			}
		}

		return true;
	};

	this.selectLandingTool = function()
	{
		var selected = null;

		// last visited tool in session for site
		var toolId = main.session.get("portal:tool:" + main.info.site.id);
		if (toolId != null)
		{
			selected = main.info.findTool(toolId);
			if ((selected != null) && (selected.role > main.info.site.role)) selected = null;
		}

		if (selected == null) selected = main.info.firstTool();

		return selected;
	};

	this.init = function()
	{
		me.populateUserNav();
		// TODO: if user menu is fall down, and a click takes the user to dashboard:
		// onClick(main.ui.portal_userName, function(){me.navigate(null, Tools.dashboard, false, false);});
		// main.ui.portal_userName.hover( function(){me.toggleUserNav(true);}, function(){});
		onClick(main.ui.portal_userName, function(){me.toggleUserNav(true);});
		onHover(main.ui.portal_userName, function()
		{
			// me.toggleSiteNav(true); // TODO: for a fall down, toggle here
			main.ui.portal_userName.stop().animate({color:"white"}, Hover.on);
		}, function()
		{
			main.ui.portal_userName.stop().animate({color:"#B3B3B3"}, Hover.off);
		});

		// Note: sitenav will be populated with the logged in user's site after login
		onClick(main.ui.portal_siteName, function(){me.toggleSiteNav(true);});
		onHover(main.ui.portal_siteName, function()
		{
			// me.toggleSiteNav(true); // TODO: for a fall down, toggle here
			main.ui.portal_siteName.stop().animate({color:"white"}, Hover.on);
		}, function()
		{
			main.ui.portal_siteName.stop().animate({color:"#B3B3B3"}, Hover.off);
		});

//		onClick(main.ui.portal_menuName, function(){me.navigate(main.info.site, Tools.home, false, false);});
		onClick(main.ui.portal_menuName, function(){me.toggleMenu(true);});
		onHover(main.ui.portal_menuName, function(){me.toggleMenu(true);}, function(){});

		onClick(main.ui.portal_online, function(){me.toggleOnline(true);});
		onHover(main.ui.portal_online, function(){me.toggleOnline(true);}, function(){});
	};

	this.populateSiteNav = function()
	{
		applyClass(["italic", "red", "yellow"], main.ui.portal_siteNameText, false);
		if (main.info.site != null)
		{
			main.ui.portal_siteNameText.text(main.info.site.title);
			// TODO: for a fall down menu, where the click takes the user to the site's home: onClick(main.ui.portal_siteName, function(){me.navigate(main.info.site, Tools.home, false, false);});

			if (main.info.site.accessStatus != AccessStatus.open)
			{
				applyClass(["italic", ((main.info.site.accessStatus == AccessStatus.willOpen) ? "yellow" : "red")], main.ui.portal_siteNameText, true);
			}
		}
		else
		{
			main.ui.portal_siteNameText.text(main.i18n.lookup("header_sites", "Sites"));
			// for a fall down menu, the click needs here to just open the menu: onClick(main.ui.portal_siteName, function(){me.toggleSiteNav(true);});
		}
		main.ui.portal_sitenav.empty();

		$.each(main.info.sites || [], function(index, site)
		{
			if (site.accessStatus == AccessStatus.open)
			{
				var entry = clone(main.ui.portal_sitenavTemplate, ["portal_sitenavTemplateA"]);
				show(entry.element);
				main.ui.portal_sitenav.append(entry.element);

				entry.portal_sitenavTemplateA.text(site.title);
				onClick(entry.portal_sitenavTemplateA, function(){me.siteClicked(site);});
				onHover(entry.portal_sitenavTemplateA,
						function(){entry.portal_sitenavTemplateA.stop().animate({color:"#000000", backgroundColor:"#FFFFFF"}, Hover.quick);},
						function(){entry.portal_sitenavTemplateA.stop().animate({color:"#FFFFFF", backgroundColor:"#000000"}, Hover.quick);});
			}
			else
			{
				var entry = clone(main.ui.portal_sitenavClosedTemplate, ["portal_sitenavClosedTemplateA"]);
				show(entry.element);
				main.ui.portal_sitenav.append(entry.element);

				entry.portal_sitenavClosedTemplateA.text(site.title);
				
				if ((site.role >= Role.instructor))
				{
					onClick(entry.portal_sitenavClosedTemplateA, function(){me.siteClicked(site);});
				}
				else
				{
					onClick(entry.portal_sitenavClosedTemplateA, function(){});
					entry.portal_sitenavClosedTemplateA.attr("disabled", true);
					entry.portal_sitenavClosedTemplateA.css("cursor", "not-allowed");
				}
				onHover(entry.portal_sitenavClosedTemplateA,
						function(){entry.portal_sitenavClosedTemplateA.stop().animate({color:"#000000", backgroundColor:"#808080;"}, Hover.quick);},
						function(){entry.portal_sitenavClosedTemplateA.stop().animate({color:"#808080;", backgroundColor:"#000000"}, Hover.quick);});
			}
		});
		

		// place the My Sites link after a divider		
		var entry = clone(main.ui.portal_menuLinkDividerTATemplate, []);
		show(entry.element);
		main.ui.portal_sitenav.append(entry.element);

		var entry = clone(main.ui.portal_menuAdminTemplate, ["portal_menuAdminTemplateA"]);
		onHover(entry.portal_menuAdminTemplateA,
				function(){entry.portal_menuAdminTemplateA.stop().animate({color:"#DDDDDD", backgroundColor:"#333333"}, Hover.quick);},
				function(){entry.portal_menuAdminTemplateA.stop().animate({color:"#333333", backgroundColor:"#DDDDDD"}, Hover.quick);});
		show(entry.element);
		main.ui.portal_sitenav.append(entry.element);
		entry.portal_menuAdminTemplateA.text(main.i18n.lookup(Tools.sites.title, Tools.sites.title));
		onClick(entry.portal_menuAdminTemplateA, function(){me.navigate(null, Tools.sites, false, false);});				
	};

	this.findBannerTool = function(toolId)
	{
		if (toolId == Tools.login.id) return Tools.login;

		for (var i = 0; i< (me.bannerTools || []).length; i++)
		{
			if (me.bannerTools[i].id == toolId) return me.bannerTools[i];
		}
		
		return null;
	};

	this.leave = function()
	{
		// leaving where we were
		main.presence.stop();
		main.session.remove("portal:site");
		main.session.remove("portal:tool:global");
		main.info.site = null;
		main.resetScrolling();
		me.payload = null;
	};

	this.navigate = function(site, tool, enableReturn, newWindow, payload, saveHistory)
	{
		if (!me.leaveTool(function(){me.navigate(site, tool, enableReturn, newWindow, payload, saveHistory);})) return;

		// site is id or object - either way, find our version of the site
		if (site != null)
		{
			if (site.id == null)
			{
				site = main.info.findSite(site);
			}
			else
			{
				var s = main.info.findSite(site.id);
				
				// special case! if missing, use the site - it better be in Site.sendForPortal() content or better
				if (s == null) s = site;
				
				site = s;
			}
		}

		// tool is id or object - lookup the tool
		if (tool != null)
		{
			if (tool.id == null)
			{
				tool = Tools.byId(tool); // TODO: the tool may not be defined for the site
			}
			else
			{
				tool = Tools.byId(tool.id);
			}
		}

		if (newWindow)
		{
			var nextWindow = main.session.get("portal:nextWindow");
			if (nextWindow == null) nextWindow = "1";

			var newWindow = main.window + "." + nextWindow;
			
			nextWindow = parseInt(nextWindow) + 1;
			main.session.put("portal:nextWindow", nextWindow.toString());

			if (site != null) main.session.put("portal:site", site.id, newWindow);
			if (tool != null) main.session.put("portal:tool:" + site.id, tool.id, newWindow);

			var newLoc = window.location.origin + window.location.pathname + "?w=" + newWindow;
			window.open(newLoc);

			return;
		}

		if (enableReturn)
		{
			var fromSite = main.session.get("portal:site");
			var fromTool = main.session.get("portal:tool:" + fromSite);
			var fromGlobal = main.session.get("portal:tool:global");
			me.enableToolReturn(fromSite, (fromSite != null) ? fromTool : fromGlobal);
		}
		else if (enableReturn != null)
		{
			me.clearToolReturn();
		}

		// leaving where we were
		me.leave();

		// store the payload
		if (payload !== undefined) me.payload = payload;
		
		// for a global tool
		if (site == null)
		{
			if (tool != null)
			{
				main.session.put("portal:tool:global", tool.id);

				if ((saveHistory === undefined) || saveHistory) main.history.push({site: site, tool: tool});

				main.populateGlobalTool(tool);
			}
		}

		// for a site visit
		else
		{
			main.info.site = site;
			if (tool == null) tool = me.selectLandingTool();
			
			main.session.put("portal:site", site.id);
			main.session.put("portal:tool:" + site.id, tool.id);

			var firstVisit = (main.session.get("portal:visit:" + site.id) == null);
			if (firstVisit) main.session.put("portal:visit:" + site.id, site.id);

			if ((saveHistory === undefined) || saveHistory) main.history.push({site: site, tool: tool});

			main.populateSite();
			main.presence.start(firstVisit);
			main.populateSiteTool(tool);
		}
	};

	this.confirmNavigationWithChanges = function(onSave, onDiscard)
	{
		main.dialogs.openDialogButtons(main.ui.portal_confirmExit,
		[
			{text: main.i18n.lookup("action_confirmSave", "Yes! Save, then continue."), click: function()
			{
				if (onSave !== undefined) onSave();
				return true;
			}},
			{text: main.i18n.lookup("action_confirmDiscard", "No. Discard, then contine."), click: function()
			{
				if (onDiscard !== undefined) onDiscard();
				return true;
			}}
		], null, main.i18n.lookup("action_confirmCancel", "Go back."));
	};
}

function PortalCss()
{
	var me = this;
	
	this.findSheet = function(name)
	{
		for (var i = 0; i < document.styleSheets.length; i++)
		{
			if (document.styleSheets[i].href.endsWith(name))
			{
				return document.styleSheets[i];
			}
		}
		return null;
	};

	this.findRule = function(sheet, selector)
	{
		for (var i = 0; i < sheet.cssRules.length; i++)
		{
			if (sheet.cssRules[i].selectorText == selector)
			{
				return sheet.cssRules[i];
			}
		}
		return null;
	};

	this.init = function()
	{
	};

	this.set = function(fileName, selector, name, value)
	{
		var sheet = me.findSheet(fileName);
		if (sheet == null) return;
		var rule = me.findRule(sheet, selector);
		if (rule == null) return;
		rule.style[name] = value;
	};
}

function PortalHistory(main)
{
	var me = this;

	this.init = function()
	{
		window.addEventListener("popstate", function(e)
		{
			if ((e.state != null) && main.session.isActive() && (e.state.session == main.session.id()))
			{
				main.userActivity();
				main.nav.navigate(e.state.site, e.state.tool, false, false, null, false);
			}
		});		
	};

	this.push = function(state)
	{
		state.session = main.session.id();
		history.pushState(state, null, null);
	};
}

function Portal()
{
	var me = this;

	this.cdp = new e3_Cdp({onErr:function(code){me.cdpError(code);}});
	this.i18n = new e3_i18n(serenity_i10n, "en-us");
	this.dialogs = new e3_Dialog();
	this.timestamp = new e3_Timestamp();

	this.ui = null;
	this.loginBackdrops = [/*"/ui/art/backdrop/background1.jpg",*/
	                       /*"/ui/art/backdrop/clouds_0157.jpg",*/ /*"/ui/art/backdrop/clouds_0158.jpg",*/
	                       "/ui/art/backdrop/clouds_0187.jpg", "/ui/art/backdrop/clouds_0193.jpg", "/ui/art/backdrop/clouds_0194.jpg",
	                       "/ui/art/backdrop/clouds_0195.jpg", "/ui/art/backdrop/clouds_0196.jpg", "/ui/art/backdrop/clouds_0198.jpg"];
	this.campusBackdrops = ["/ui/art/backdrop/background2.jpg", "/ui/art/backdrop/background3.jpg",
	                        "/ui/art/backdrop/campus_0161.jpg", "/ui/art/backdrop/campus_0166.jpg", "/ui/art/backdrop/campus_0175.jpg",
	                        "/ui/art/backdrop/campus_0177.jpg", "/ui/art/backdrop/campus_0178.jpg", "/ui/art/backdrop/campus_0179.jpg",
	                        "/ui/art/backdrop/campus_0200.jpg"];
	this.info = null;
	this.nav = null;
	this.dnd = null;
	this.auth = null;
	this.presence = null;
	this.session = null;
	this.css = null;
	this.picker = null;
	this.history = null;

	this.window = "1";
	this.loggingIn = false;
	this.specialColor = "#8E1E13";
	this.defaultSpecialColor = "#8E1E13";
	this.tokenId = "JSESSIONID"; // coordinate this with CdpServlet, ConnectorServlet, DownloadServlet
	this.protocol = $(location).attr('protocol');

	this.init = function()
	{
		me.info = new PortalInfo(me);
		me.nav = new PortalNav(me);
		me.dnd = new PortalDnD(me);
		me.scroller = new PortalScrollDetective(me);
		me.auth = new PortalAuth(me);
		me.presence = new PortalPresence(me);
		me.css = new PortalCss(me);
		me.picker = new PortalPicker(me);
		me.history = new PortalHistory(me);

		me.i18n.localize(null, $("body"));
		me.ui = findElements(["portal_backdrop","portal_clientLogo","portal_toolStage","portal_banner_area","portal_banner","portal_help",
		                      "portal_banner_bigLogo","portal_banner_bigLogoA","portal_banner_bottomLine","portal_banner_smallLogo","portal_banner_smallLogoA",
		                      "portal_userName","portal_userNameText","portal_usernav","portal_usernavTemplate",
		                      "portal_siteName","portal_siteNameText","portal_sitenav","portal_sitenavTemplate","portal_sitenavClosedTemplate",
		                      "portal_online","portal_onlineList","portal_onlineTemplate","portal_online_count",
		                      "portal_contextMenu", "portal_contextMenuTemplate",
		                      "portal_menu","portal_menuName","portal_menuTemplate","portal_menuLinkTemplate","portal_menuAdminTemplate",
		                      "portal_menuLinkDividerTATemplate", "portal_menuLinkDividerALTemplate", "portal_menuLinkDividerTLTemplate",
		                      "portal_footer_bkgName", "portal_confirmExit"]);

		me.info.init();
		me.nav.init();
		me.dnd.init();
		me.scroller.init();
		me.auth.init();
		me.presence.init();
		me.css.init();
		me.picker.init();
		me.history.init();

		me.fullBanner();
		me.scroller.setPortalPins([{ui: me.ui.portal_banner_bottomLine, onPin:function(pinned)
		{
			if (pinned)
			{
				me.smallBanner();
			}
			else
			{
				me.fullBanner();
				me.nav.adjustDropdowns();
			}
		}, onAdjust: function(info)
		{
			me.nav.adjustDropdowns(info);
		}}]);
		
		onClick(me.ui.portal_banner_bigLogoA, function(){me.nav.navigate(null, Tools.dashboard, false, false);});
		onClick(me.ui.portal_banner_smallLogoA, function(){me.nav.navigate(null, Tools.dashboard, false, false);});

		$(document).on('keydown', function(e){try{me.userActivity(); return true;}catch(err){error(err);}});
		$(document).on('mouseup', function(e){try{me.userActivity(); return true;}catch(err){error(err);}});
	};

	this.userActivity = function()
	{
		me.nav.toggleUserNav(false);
		me.nav.toggleSiteNav(false);
		me.nav.toggleOnline(false);
		me.nav.toggleMenu(false);
		me.nav.toggleContextMenu(false);

		// tell the session
		if (me.session != null) me.session.recordUserActivity();
	};

	this.smallBanner = function()
	{
		hide([me.ui.portal_banner_bigLogo, me.ui.portal_clientLogo]);
		show(me.ui.portal_banner_smallLogo);
		me.ui.portal_banner.css("background-color", "");
		me.ui.portal_banner_bottomLine.css("background-color", "rgba(0,0,0,0.9)");
	};
	
	this.fullBanner = function()
	{
		show([me.ui.portal_banner_bigLogo, me.ui.portal_clientLogo]);
		hide(me.ui.portal_banner_smallLogo);
		me.ui.portal_banner.css("background-color", "rgba(0,0,0,0.6)");
		me.ui.portal_banner_bottomLine.css("background-color", "");
	};

	this.start = function()
	{
		// distinguish among multiple browser windows
		if (window.location.search.startsWith("?w="))
		{
			me.window = window.location.search.substring(3, window.location.search.length);
		}

		// create the session keeper - it will get a chance to clear out storage if we are not in a session when we noticeAuthenticationChange
		me.session = new e3_Session(me.window);
		me.session.setOnInactive(function()
		{
			me.auth.clearAuthentication();
			me.setLoggedOut();
		});

		// check for active session, ends up either populating logged in or logged-out
		var actionTaken = me.auth.noticeAuthenticationChange();
		if (!actionTaken)
		{
			// ... or no action, go to logged-out
			me.setLoggedOut();
		}
	};

	this.isLoggedOut = false;
	this.setLoggedOut = function()
	{
		if (me.isLoggedOut) return;
		me.isLoggedOut = true;

		// if we have an authentication cookie, send the logout to the server, ignoring any return
		var token = me.auth.readCookie(me.tokenId);
		if (token != null)
		{
			var params = me.cdp.params();
			me.loggingIn = true;
			me.cdp.request("logout", params, function(data){me.loggingIn = false;});
		}

		me.info.init();
		me.session.deactivate();
		me.auth.setAuthentication();

		me.nav.leave();

		// bring up the login tool
		me.nav.navigate(null, Tools.login, false, false, null, false);

		// second, so the bkg change holds
		me.populateLoggedOut();
	};
	
	this.setLoggedIn = function(data)
	{
		me.isLoggedOut = false;

		me.info.user = data.user;
		me.info.sites = data.sites || [];
		me.info.site = null;
		me.session.activate();
		me.auth.setAuthentication();

		me.populateLoggedIn();
		
		me.initialNavigation();
	};
	
	this.initialNavigation = function()
	{
		// the last site visited
		var siteId = me.session.get("portal:site");
		var site = null;
		if (siteId != null)
		{
			site = me.info.findSite(siteId);
		}

		// for special users
		else if (me.info.isAdmin())
		{
			site = me.info.findSite(1);
		}
		else if (me.info.isHelpdesk())
		{
			site = me.info.findSite(3);
		}

		if (site != null)
		{
			me.nav.navigate(site, null, false, false);
		}
		
		// if not a site, a global tool
		else
		{
			var tool = null;

			// check for a global tool
			var toolId = me.session.get("portal:tool:global");
			if (toolId != null)
			{
				tool = me.nav.findBannerTool(toolId);
			}

			if (tool == null)
			{
				tool = me.nav.bannerTools[0];
			}

			// if login, switch to dashboard, since we are always logged in when we get to initialNavigation
			if (tool.id == Tools.login.id) tool = Tools.dashboard;

			me.nav.navigate(null, tool, false, false);
		}
	};

	this.setBackdrop = function(url)
	{
		me.ui.portal_backdrop.css("background-image", "url('" + url + "')");
		me.ui.portal_footer_bkgName.text(url);
	};

	this.setSpecialColor = function(color)
	{
		me.specialColor = color
		me.css.set("etudes-core.css", "div.e3_panelHeader", "backgroundColor", me.specialColor);
		me.css.set("etudes-core.css", ".e3_specialColor", "color", me.specialColor);
		me.css.set("etudes-core.css", ".e3_specialColor", "fill", me.specialColor);
		me.css.set("etudes-core.css", "div.e3_specialBorder", "borderColor", me.specialColor);
	};

	this.populateLoggedOut = function()
	{
		// random backdrop selection
		var index = Math.floor((Math.random() * me.loginBackdrops.length));
		me.setBackdrop(me.loginBackdrops[index]);

		me.setSpecialColor(me.defaultSpecialColor);

		me.ui.portal_toolStage.css({minHeight: "calc(100vh - 80px)"});
		hide([me.ui.portal_banner_area, me.ui.portal_banner]);
	};

	this.populateLoggedIn = function()
	{
		// TODO: the user's selected backdrop
		// random backdrop selection
		var index = Math.floor((Math.random() * me.campusBackdrops.length));
		me.setBackdrop(me.campusBackdrops[index]);

		// TODO: the client to which the user logged into's logo & color
		me.ui.portal_clientLogo.attr("src", "/ui/art/logo/hartnell.png");
		me.setSpecialColor("#011A57"); // #011A57 #7D5102

		me.ui.portal_userNameText.text(me.info.user.nameFirst);
		me.nav.populateSiteNav();

		me.ui.portal_toolStage.css({minHeight: "calc(100vh - 180px)"});
		show([me.ui.portal_banner_area, me.ui.portal_banner]);
	};

	this.populateGlobalTool = function(tool)
	{
		// TODO: the user's selected backdrop
		// random backdrop selection
		var index = Math.floor((Math.random() * me.campusBackdrops.length));
		me.setBackdrop(me.campusBackdrops[index]);

		me.nav.hideInSiteBanner();
		me.nav.populateSiteNav();
		me.loadTool(tool.url);
	};
	
	this.populateSite = function()
	{
		// TODO: set me.info.site's backdrop
		// random backdrop selection
		var index = Math.floor((Math.random() * me.campusBackdrops.length));
		me.setBackdrop(me.campusBackdrops[index]);

		// tools to include
		var tools = [];
		$.each(me.info.site.tools || [], function(index, id)
		{
			var t = Tools.byId(id);

			// check the tool's role requirement against the user's role in site - skip if the user's role is less
			if (t.role > me.info.site.role) return;

			tools.push(t);
		});

		me.nav.populateInSiteBanner(me.info.site);
		me.nav.populateSiteNav();
	};

	this.populateSiteTool = function(tool)
	{
		me.loadTool(tool.url);
	}

	this.loadTool = function(toolUrl)
	{
		me.ui.portal_toolStage.empty().load(toolUrl + ".html #portal_content", function(responseText, textStatus, XMLHttpRequest)
		{
			if (textStatus == "success")
			{
				// load the tool i10n
				$.ajaxSetup({cache: true});
				$.getScript(toolUrl + "_i10n.js", function()
				{
					// load the tool's script
					$.ajaxSetup({cache: true});
					try
					{
						$.getScript(toolUrl + ".js");
					}
					catch (e)
					{
						error(e);
					}
				});
			}
			else
			{
				me.cdpError(5);
			}
		});
	};

	this.refreshSites = function(sites)
	{
		// TODO:
//		me.info.sites = sites;
//		me.nav.populateSiteNav();
	};

	this.cdpError = function(code)
	{
		if (code == 1)
		{
			// skip if logging in
			if (!me.loggingIn) me.dialogs.openAlert("portal_accessDenied");			
		}
		else if (code == 2)
		{
			// skip if logging in
			if (!me.loggingIn) me.dialogs.openAlert("portal_notLoggedIn");			
		}
		else
		{
			me.dialogs.openAlert("portal_cdpFailure");
		}
	};
	
	this.updateUser = function(user)
	{
		// only if the same user
		if (me.info.user.id == user.id)
		{
			me.info.user = user;
			me.populateLoggedIn();
		}
		
		return me.info.user;
	};

	this.completeLogin = function(data)
	{
		me.nav.leave();
		me.setLoggedIn(data);
	};
	
	this.resetScrolling = function()
	{
		me.scroller.reset();
		window.scrollTo(0, 0);
	};

	// public function (portal_tool.features({onExit: onExitFunction, onScroll: onScrollFunction, pin:[ui,ui,ui]})
	this.features = function(options, reset, configure, exit)
	{
		me.nav.toolExit = null;
		me.scroller.onScroll = null;
		me.scroller.setPins();

		// TODO: everyone sends in 1 arg
		if (arguments.length == 1)
		{
			if (options.onExit !== undefined)
			{
				me.nav.toolExit = options.onExit;
			}
			
			if (options.onScroll !== undefined)
			{
				me.scroller.onScroll = options.onScroll;
			}
			
			if (options.pin !== undefined)
			{
				me.scroller.setPins(options.pin);
			}
		}
		else
		{
			if (exit !== undefined)
			{
				me.nav.toolExit = exit;
			}			
		}

		var rv = {};

		rv.site = me.info.site;
//		rv.sites = me.info.sites;
		rv.user = me.info.user;
		rv.payload = me.nav.payload;

		rv.timestamp = me.timestamp;
		rv.dialogs = me.dialogs;
		rv.cdp = me.cdp;

		rv.navigate = me.nav.navigate;
//		rv.refreshSites = me.refreshSites;
		rv.updateUser = me.updateUser;
		rv.toolReturn = me.nav.toolReturn;
		rv.login = me.completeLogin;
		rv.resetScrolling = me.resetScrolling;
		rv.confirmNavigationWithChanges = me.nav.confirmNavigationWithChanges;
		rv.protocol = me.protocol;

		return rv;
	};
};

function e3_SortAction()
{
	var _me = this;

	this._ui = null;
	this._directionalSetting = false;
	this._val = null;
	this._extra = null;

	this._findOptionByValue = function(value, options)
	{
		for (var i = 0; i < options.length; i++)
		{
			if (options[i].value == value) return options[i];
		}

		return options[0];
	};

	// settings:  onSort:sort(a/d, sortOn), options:[{value:"T", title:"TITLE"}, label:"", initial: "C"]
	this.inject = function(into, settings)
	{
		var target = ($.type(into) === "string") ? $("#" + into) : into;
		_me._ui = clone("e3_sortActionTemplate",["e3_sortActionTemplate_body", "e3_sortActionTemplate_a", "e3_sortActionTemplate_d", "e3_sortActionTemplate_trigger", "e3_sortActionTemplate_label",
		                                         "e3_sortActionTemplate_value", "e3_sortActionTemplate_value_text"]);

		var menu = {menu: [], below: _me._ui.e3_sortActionTemplate_body};
		$.each(settings.options, function(index, option)
		{
			menu.menu.push({title: option.title, action: function()
			{
				_me._val = option.value;
				_me._extra = option.extra;
				_me._ui.e3_sortActionTemplate_value_text.text(option.title);

				// auto switch to "A"
				if (_me._directionalSetting)
				{
					hide(_me._ui.e3_sortActionTemplate_d);
					show(_me._ui.e3_sortActionTemplate_a);
				}
				settings.onSort("A", _me._val, _me._extra);
			}});
		});

		onClick(_me._ui.e3_sortActionTemplate_a, function()
		{
			hide(_me._ui.e3_sortActionTemplate_a);
			show(_me._ui.e3_sortActionTemplate_d);
			settings.onSort("D", _me._val, _me._extra);
		});

		onClick(_me._ui.e3_sortActionTemplate_d, function()
		{
			hide(_me._ui.e3_sortActionTemplate_d);
			show(_me._ui.e3_sortActionTemplate_a);
			settings.onSort("A", _me._val, _me._extra);
		});

		_me._val = (settings.initial !== undefined) ? _me._findOptionByValue(settings.initial, settings.options).value : settings.options[0].value;
		_me._extra = (settings.initial !== undefined) ? _me._findOptionByValue(settings.initial, settings.options).extra : settings.options[0].extra;
		_me._ui.e3_sortActionTemplate_label.text((settings.label !== undefined) ? settings.label : portal_tool.i18n.lookup("msg_sort", "SORT"));
		_me._ui.e3_sortActionTemplate_value_text.text((settings.initial !== undefined) ? _me._findOptionByValue(settings.initial, settings.options).title : settings.options[0].title);

		onClick(_me._ui.e3_sortActionTemplate_value, function()
		{
			portal_tool.nav.toggleContextMenu(true, menu);
		});
		onHover(_me._ui.e3_sortActionTemplate_value, function()
		{
			_me._ui.e3_sortActionTemplate_value_text.stop().animate({color:"black"}, Hover.on);
		}, function()
		{
			_me._ui.e3_sortActionTemplate_value_text.stop().animate({color:"#686868"}, Hover.off);
		});

//		if (settings.css != null) _me._ui.e3_sortActionTemplate_body.css(settings.css);

		target.append(_me._ui.e3_sortActionTemplate_body);
	};

	this.directional = function(setting)
	{
		if (setting == _me._directionalSetting) return;
		
		_me._directionalSetting = setting;
		
		if (_me._directionalSetting)
		{
			hide(_me._ui.e3_sortActionTemplate_d);
			show(_me._ui.e3_sortActionTemplate_a);
		}
		else
		{
			hide([_me._ui.e3_sortActionTemplate_a, _me._ui.e3_sortActionTemplate_d]);
		}
	};
	
	// sort the items by direction ('A'/'D'), using the compareFunction(such as compareS, compareN), with the item[attribute]
	// if provided, do a secondary sort on a tie with secondCompareFunction on item[secondAttribute].  Final sort is by item.id
	this.sort = function(items, direction, compareFunction, attribute, secondCompareFunction, secondAttribute)
	{
		var sorted = [].concat(items);
		var adjust = (direction == 'A') ? 1 : -1;
		sorted.sort(function(a, b)
		{
			var rv = adjust * compareFunction(((attribute == null) ? a : a[attribute]), ((attribute == null) ? b : b[attribute]));
			if (rv == 0)
			{
				if (secondCompareFunction !== undefined)
				{
					rv = secondCompareFunction(((secondAttribute == null) ? a : a[secondAttribute]), ((secondAttribute == null) ? b : b[secondAttribute]));
				}
				if (rv == 0)
				{
					rv = compareN(a.id, b.id);
				}
			}
			return rv;
		});

		return sorted;
	};
}

function e3_ItemNav()
{
	var _me = this;
	
	this._ui = null;

	this._disableSave = function(target)
	{
		target.attr("disabled", true);
		applyClass("disabled", target, true);		
		resetHoverControl(target);
	};

	this.enableSave = function(onSaveCancel, target)
	{
		if (target === undefined) target = _me._ui.portal_itemNavTemplate_save;

		// could fade instead of show, but interferes with the hover animation
		if (onSaveCancel == null)
		{
			onClick(target, function(){});
			_me._disableSave(target);
		}
		else
		{
			onClick(target, function()
			{
				try
				{
					var rv = onSaveCancel(true, function()
					{
						_me._disableSave(target);
					});
					if ((rv === undefined) || rv)
					{
						_me._disableSave(target);
					}
				}
				catch(e)
				{
					error(e);
				}
			});

			target.attr("disabled", false);
			applyClass("disabled", target, false);
		}
	};

	// TODO: ??? is this being used?
	this.adjustInfoWidth = function(into)
	{
		var target = ($.type(into) === "string") ? $("#" + into) : into;

		var n = target.next();
		if (n.hasClass("e3_navbar_info"))
		{
			n.css({width:"calc(100% - " + (target.width()+1) + "px)"});
		}
	};

	this.injectSpecial = function(into, from, controls, actions, setup)
	{
		var target = ($.type(into) === "string") ? $("#" + into) : into;
		var template = ($.type(from) === "string") ? $("#" + from) : from;

		var nav = clone(template, controls);

		target.empty();

		for (var i = 0; i < controls.length; i++)
		{
			if (actions[i] != null)
			{
				onClick(nav[controls[i]], actions[i]);
				setupHoverControls([nav[controls[i]]]);
				target.append(nav[controls[i]].parent());
			}
			else
			{
				target.append(nav[controls[i]]);
			}
		}

		if (setup !== undefined) try{setup(nav);} catch (err) {error(err);};

		show(target);
		_me.adjustInfoWidth(target);

		return nav;
	};

	// settings:{pos, returnFunction -or- doneFunction, navigateFunction, info}
	this.inject = function(into, settings)
	{
		var target = ($.type(into) === "string") ? $("#" + into) : into;
		var ui = clone("portal_itemNavTemplate",["portal_itemNavTemplate_return", "portal_itemNavTemplate_done",
		                                         "portal_itemNavTemplate_prev", "portal_itemNavTemplate_pos", "portal_itemNavTemplate_next",
		                                         "portal_itemNavTemplate_save"]);
		_me._ui = ui;

		target.empty();

		if (settings.returnFunction !== undefined)
		{
			if (settings.returnName !== undefined)
			{
				ui.portal_itemNavTemplate_return.text(settings.returnName);
				ui.portal_itemNavTemplate_return.parent().removeClass("return").addClass("titled");
			}
			onClick(ui.portal_itemNavTemplate_return, function(){settings.returnFunction();});
			target.append(ui.portal_itemNavTemplate_return.parent());
			setupHoverControls([ui.portal_itemNavTemplate_return]);
		}
		else if (settings.doneFunction !== undefined)
		{
			if (settings.doneName !== undefined) ui.portal_itemNavTemplate_done.text(settings.doneName);
			onClick(ui.portal_itemNavTemplate_done, function(){settings.doneFunction();});
			target.append(ui.portal_itemNavTemplate_done.parent());
			setupHoverControls([ui.portal_itemNavTemplate_done]);
		}

		if ((settings.doneFunction !== undefined) || ((settings.withSave !== undefined) && (settings.withSave)))
		{
			onClick(ui.portal_itemNavTemplate_save, function(){});
			target.append(ui.portal_itemNavTemplate_save.parent());
			setupHoverControls([ui.portal_itemNavTemplate_save]);
			ui.portal_itemNavTemplate_save.attr("disabled", true);
			applyClass("disabled", ui.portal_itemNavTemplate_save, true);
		}

		if (settings.pos !== undefined)
		{
			onClick(ui.portal_itemNavTemplate_prev, function()
			{
				if (settings.pos.prev != null)
				{
					settings.navigateFunction(settings.pos.prev);
				}
			});
			ui.portal_itemNavTemplate_prev.attr("disabled", (settings.pos.prev == null));
			applyClass("disabled", ui.portal_itemNavTemplate_prev, (settings.pos.prev == null));
	
			ui.portal_itemNavTemplate_pos.text(portal_tool.i18n.lookup("msg_pos", "%0 of %1", "html", [settings.pos.item, settings.pos.total]));
	
			onClick(ui.portal_itemNavTemplate_next, function()
			{
				if (settings.pos.next != null)
				{
					settings.navigateFunction(settings.pos.next);
				}
			});
			ui.portal_itemNavTemplate_next.attr("disabled", (settings.pos.next == null));
			applyClass("disabled", ui.portal_itemNavTemplate_next, (settings.pos.next == null));
			
			target.append(ui.portal_itemNavTemplate_prev.parent());
			target.append(ui.portal_itemNavTemplate_pos);
			target.append(ui.portal_itemNavTemplate_next.parent());
			setupHoverControls([ui.portal_itemNavTemplate_prev, ui.portal_itemNavTemplate_next]);
		}

		if (settings.extra !== undefined)
		{
			var extra = clone(settings.extra.template, []);
			extra.d = extra.element.children();
			extra.a = extra.d.find("a");
			setupHoverControls([extra.a]);
			onClick(extra.a, settings.extra.onClick);
			ui.portal_itemNavTemplate_save.parent().before(extra.d);
		}

		show(target);
		_me.adjustInfoWidth(target);
	};
}

function e3_Modebar(modebarId)
{
	var _me = this;
	this._modebar = ($.type(modebarId) === "string") ? $("#" + modebarId) : modebarId;
	this._modeLinks = [];
	this._ui = {};

	this._modebar.empty();
	this._modebar.addClass("e3_modebar");

	this._setupHoverControls = function()
	{
		onHover(_me._ui.e3_Modebar_saveDiscardTemplate_save,
			function(a)
			{
				a.stop().animate({color:"#2AB31D", backgroundColor:"#FFFFFF"}, Hover.quick); // bkg: #E4E2E3
			},
			function(a)
			{
				a.stop().animate({color:"#FFFFFF", backgroundColor:"#2AB31D"}, Hover.quick, function(){a.css({color:"", backgroundColor:""});});
			});
		onHover(_me._ui.e3_Modebar_saveDiscardTemplate_discard,
			function(a)
			{
				a.stop().animate({color:"#E00000", backgroundColor:"#FFFFFF"}, Hover.quick);
			},
			function(a)
			{
				a.stop().animate({color:"#FFFFFF", backgroundColor:"#E00000"}, Hover.quick, function(){a.css({color:"", backgroundColor:""});});
			});
	};

	this.set = function(modeArray, current)
	{
		$.each(modeArray || [], function(index, setting)
		{
			var a = _me._mode(setting.name, setting.icon, setting.func, (index == (modeArray.length-1)), (index == current));
			_me._modeLinks.push(a);
		});
		
		_me._ui = clone("e3_Modebar_saveDiscardTemplate",["e3_Modebar_saveDiscardTemplate_save", "e3_Modebar_saveDiscardTemplate_discard"]);
		_me._setupHoverControls();
		_me._modebar.append(_me._ui.element.children());
	};

	this.showSelected = function(index)
	{
		$(_me._modebar).find("a.current").stop().removeClass("current").css({color:"", backgroundColor:""});
		if (index != null) $(_me._modeLinks[index]).stop().addClass("current").css({color:"", backgroundColor:""});
	};
	
	this.enableSaveDiscard = function(onSaveCancel)
	{
		// could fade instead of show, but interferes with the hover animation
		if (onSaveCancel == null)
		{
			show([_me._ui.e3_Modebar_saveDiscardTemplate_save,_me._ui.e3_Modebar_saveDiscardTemplate_discard], false);
		}
		else
		{
			onClick(_me._ui.e3_Modebar_saveDiscardTemplate_save, function()
			{
				try
				{
					var rv = onSaveCancel(true, function()
					{
						show([_me._ui.e3_Modebar_saveDiscardTemplate_save,_me._ui.e3_Modebar_saveDiscardTemplate_discard], false);
					});
					if ((rv === undefined) || rv)
					{
						show([_me._ui.e3_Modebar_saveDiscardTemplate_save,_me._ui.e3_Modebar_saveDiscardTemplate_discard], false);
					}
				}
				catch(e)
				{
					error(e);
				}
			});
			onClick(_me._ui.e3_Modebar_saveDiscardTemplate_discard, function()
			{
				try
				{
					var rv = onSaveCancel(false, function()
					{
						show([_me._ui.e3_Modebar_saveDiscardTemplate_save,_me._ui.e3_Modebar_saveDiscardTemplate_discard], false);
					});
					if ((rv === undefined) || rv)
					{
						show([_me._ui.e3_Modebar_saveDiscardTemplate_save,_me._ui.e3_Modebar_saveDiscardTemplate_discard], false);
					}
				}
				catch(e)
				{
					error(e);
				}
			});

			show([_me._ui.e3_Modebar_saveDiscardTemplate_save,_me._ui.e3_Modebar_saveDiscardTemplate_discard], true);
		}
	};

	this.enableDiscard = function(onSaveCancel)
	{
		// could fade instead of show, but interferes with the hover animation
		if (onSaveCancel == null)
		{
			hide(_me._ui.e3_Modebar_saveDiscardTemplate_discard);
		}
		else
		{
			onClick(_me._ui.e3_Modebar_saveDiscardTemplate_discard, function()
			{
				try
				{
					var rv = onSaveCancel(false, function()
					{
						hide(_me._ui.e3_Modebar_saveDiscardTemplate_discard);
					});
					if ((rv === undefined) || rv)
					{
						hide(_me._ui.e3_Modebar_saveDiscardTemplate_discard);
					}
				}
				catch(e)
				{
					error(e);
				}
			});

			show(_me._ui.e3_Modebar_saveDiscardTemplate_discard);
		}
	};

	this._mode = function(name, icon, mode, last, current)
	{
		var a = $("<a />",{href:"", "class":"e3_modebar_mode"});
		$(this._modebar).append(a);
		$(a).html(name);
		if (current) $(a).addClass("current");
		$(a).click(function()
		{
			try
			{
				$(this).blur();
				mode();
			}
			catch(e)
			{
				error(e);
			};
			return false;
		});

		onHover(a,
				function(){a.stop().animate({color:"#FFFFFF", backgroundColor:"#404040"}, Hover.on);},
				function()
				{
					if (a.hasClass("current"))
					{
						a.stop().animate({color:"#000000", backgroundColor:"#C0C0C0"}, Hover.off, function(){a.css({color:"",backgroundColor:""})});
					}
					else
					{
						a.stop().animate({color:"#888888", backgroundColor:"#E4E2E3"}, Hover.off, function(){a.css({color:"",backgroundColor:""})});
					}
				});

		return a;
	};
};

function e3_Attribution()
{
	this.inject = function(into, id, attribution, extra)
	{
		var target = ($.type(into) === "string") ? $("#" + into) : into;
		var ui = clone($("#portal_attributionTemplate"),["portal_attributionTemplate_id", "portal_attributionTemplate_created", "portal_attributionTemplate_modified",
		                                                 "portal_attributionTemplate_row_id", "portal_attributionTemplate_row_created", "portal_attributionTemplate_row_modified",
		                                                 "portal_attributionTemplate_extra", "portal_attributionTemplate_extra_label"]);

		target.empty();
		hide(target);

		if ((attribution == null) || (id == null) || (id <= 0)) return;

		ui.portal_attributionTemplate_id.text(id);

		ui.portal_attributionTemplate_created.text(attribution.createdBy + (attribution.createdOn == null ? "" : (", " + portal_tool.timestamp.display(attribution.createdOn))));
		show(ui.portal_attributionTemplate_row_created, (attribution.createdBy != null));
		
		ui.portal_attributionTemplate_modified.text(attribution.modifiedBy + (attribution.modifiedOn == null ? "" : (", " + portal_tool.timestamp.display(attribution.modifiedOn))));
		show(ui.portal_attributionTemplate_row_modified, (attribution.modifiedBy != null));

		if (extra !== undefined)
		{
			ui.portal_attributionTemplate_extra_label.text(extra.label);
			ui.portal_attributionTemplate_extra.text(obj[extra.id]);
			show (ui.portal_attributionTemplate_row_extra, (obj[extra.id] != null));
		}
		else
		{
			hide(ui.portal_attributionTemplate_row_extra);
		}

		target.append(ui.element.children());
		show(target);
	};
}

$(function()
{
	try
	{
		portal_tool = new Portal();
		portal_tool.init();
		portal_tool.start();
	}
	catch (e)
	{
		error(e);
	}
});

/**********************************************************************************
 * $URL: https://source.etudes.org/svn/serenity/trunk/portal/portal-webapp/src/main/webapp/portal.js $
 * $Id: portal.js 11754 2015-10-02 18:16:45Z ggolden $
 ***********************************************************************************
 *
 * Copyright (c) 2014, 2015 Etudes, Inc.
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

var portal_tool = null;

function Portal()
{
	var _me = this;

	this._cdp = new e3_Cdp({onErr:function(code){_me._cdpError(code);}});
	this._i18n = new e3_i18n(portal_i10n, "en-us");
	this._dialogs = new e3_Dialog();
	this._timestamp = new e3_Timestamp();

	// created in start()
	this._session = null;
	this._window = "1";

	this._user = null;
	this._sites = null;
	this._extraSites = null;
	this._site = null;
	this._authenticationToken = null;
	this._loggingIn = false;
	this._skinColor = "#730416";

	this._presenceTimer == null;
	this._presenceInterval = 30 * 1000;
	this._authCheckTimer = null;
	this._authCheckInterval = 10 * 1000;

	this._toolReturn = null;

	this._siteTabLimit = 6;

	this._toolExit = null;

	this._sidebarEnabled = false;
	this._options = 
	{
		sites: "T", // F, T, H
		sidebar: true,
		autoSidebar: true,
		editor: 
		{
			size:"S", // S, L
			shape:"R", // S, R
			outline:"F" // T, F
		}
	};

	// Note: hard coding of values from Tool.java
	this._bannerTools =
	[
		{id:54, title:"My Dashboard", url:"/dashboard/dashboard", role: 0},
		{id:52, title:"My Sites", url:"/site/mysites", role: 0},
		{id:50, title:"My Files", url:"/myfiles/myfiles", role: 0},
		{id:51, title:"My Account", url:"/user/account", role: 0}
		// {id:53, title:"Preferences", url:"/user/preferences", role: 0}
	];

	this.init = function()
	{
		_me._i18n.localize(null, $("body"));

		onClick("portal_tool_sidebar_toggle", function(){_me._toggleSidebar();});
		if (_me._options.autoSidebar) $("#portal_tool_sidebar_toggle").off("mouseenter").on("mouseenter", function(){try{_me._toggleOpenSidebar();}catch (e){error(e);};return false;});

		// _me._populateBannerLinks();
		onClick("portal_command_logout", function(){_me._logout();});
		$("#login_form").off("submit").on("submit", function(event){try{event.preventDefault(); _me._login(); return false;}catch(e){error(e);}});

		$("input:radio[name=portal_option_sites]").off('click').click(function(){try{_me._portalOptionsChanged();}catch(e){error(e);};return true;});
		$("#portal_option_sidebar").off('click').click(function(){try{_me._portalOptionsChanged();}catch(e){error(e);};return true;});
		$("#portal_option_autoSidebar").off('click').click(function(){try{_me._portalOptionsChanged();}catch(e){error(e);};return true;});
		$("input:radio[name=portal_option_editorSize]").off('click').click(function(){try{_me._portalOptionsChanged();}catch(e){error(e);};return true;});
		$("input:radio[name=portal_option_editorShape]").off('click').click(function(){try{_me._portalOptionsChanged();}catch(e){error(e);};return true;});
		$("input:radio[name=portal_option_editorOutline]").off('click').click(function(){try{_me._portalOptionsChanged();}catch(e){error(e);};return true;});
		
		$("#portal_options_refresh").off('click').click(function(){try{_me._refresh();}catch(e){error(e);};return true;});

		$("input:radio[name=portal_option_sites][value=" + _me._options.sites + "]").prop('checked', true);
		$("#portal_option_sidebar").prop('checked', _me._options.sidebar);
		$("#portal_option_autoSidebar").prop('checked', _me._options.autoSidebar);
		$("input:radio[name=portal_option_editorSize][value=" + _me._options.editor.size + "]").prop('checked', true);
		$("input:radio[name=portal_option_editorShape][value=" + _me._options.editor.shape + "]").prop('checked', true);
		$("input:radio[name=portal_option_editorOutline][value=" + _me._options.editor.outline + "]").prop('checked', true);
		
		// block drops
		// $(document).on('dragstart', function(e){return _me.dropBlocker(e);});
		// $(document).on('drag', function(e){return _me.dropBlocker(e);});
		// $(document).on('dragenter', function(e){return _me.dropBlocker(e);});
		// $(document).on('dragleave', function(e){return _me.dropBlocker(e);});
		$(document).on('dragover', function(e){return _me.dropBlocker(e);});
		$(document).on('drop', function(e){return _me.dropBlocker(e);});
		// $(document).on('dragend', function(e){return _me.dropBlocker(e);});
	};

	this._setAuthentication = function()
	{
		_me._authenticationToken = _me._readCookie("EtudesToken");
	};

	this._readCookie = function(name)
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

	this._noticeAuthenticationChange = function()
	{
		var current = _me._readCookie("EtudesToken");

		// if we lost authentication - respond like a session timeout / logout
		if ((_me._authenticationToken != null) && (current == null))
		{
			_me._user = null;
			_me._sites = null;
			_me._extraSites = null;
			_me._site = null;
			_me._session.deactivate();
			_me._setAuthentication();
			_me._populateLoggedOut();
		}
		
		// if we gained authentication - respond like a start
		// or if we changed authentication, same
		else if (((_me._authenticationToken == null) && (current != null)) || (_me._authenticationToken != current))
		{
			var params = _me._cdp.params();
			
			var siteId = _me._session.get("portal:site");
			if (siteId != null)
			{
				params.post.extraSiteId = siteId;
			}

			_me._loggingIn = true;
			_me._cdp.request("checkAuth", params, function(data)
			{
				_me._loggingIn = false;
				if (0 == data["cdp:status"])
				{
					_me._user = data.user;
					_me._sites = data.sites;
					_me._extraSites = data.extraSites;
					_me._session.activate();
					_me._populateLoggedIn();
				}
				else
				{
					_me._session.deactivate();
					_me._populateLoggedOut();
				}
				_me._setAuthentication();
			});
		}
	};

	this._isAdmin = function()
	{
		return (_me._user.id == 1);
	};

	this._isHelpdesk = function()
	{
		return (_me._user.id == 2);
	};

	this._onAdmin = function()
	{
		return (_me._site.id == 2);
	};

	this._onHelpdesk = function()
	{
		return (_me._site.id == 3);
	};

	this._lastDropEventType = null;
	this.dropBlocker = function(e)
	{
		// if ((_me._lastDropEventType == null) || (_me._lastDropEventType != e.type)) console.log(e);
		_me._lastDropEventType = e.type;

		e.stopPropagation();
		e.preventDefault();

		return false;
	};

	this._findSite = function(siteId)
	{
		var found = null;
		$.each(_me._sites || [], function(index, site)
		{
			if (site.id == siteId) found = site;
		});
		
		if (found == null)
		{
			$.each(_me._extraSites || [], function(index, site)
			{
				if (site.id == siteId) found = site;
			});
		}

		return found;
	};

	this._findTool = function(site, toolId)
	{
		if ((site == null) || (toolId == null)) return null;
		
		var found = null;
		$.each(site.tools || [], function(index, tool)
		{
			if (tool.id == toolId) found = tool;
		});
		
		if (found == null)
		{
			if ((site.setupTool != null) && (site.setupTool.id == toolId)) found = site.setupTool;
		}
		
		if (found == null)
		{
			if ((site.rosterTool != null) && (site.rosterTool.id == toolId)) found = site.rosterTool;
		}
		
		return found;
	};

	this._findBannerTool = function(toolId)
	{
		var found = null;
		$.each(_me._bannerTools || [], function(index, tool)
		{
			if (tool.id == toolId) found = tool;
		});
		
		return found;
	};

	this.start = function()
	{
		if (window.location.search.startsWith("?w="))
		{
			_me._window = window.location.search.substring(3, window.location.search.length);
		}

		// if we have no authentication token cookie, start up logged out
		if (document.cookie.indexOf("EtudesToken=") == -1)
		{
			_me._session = new e3_Session(false, function(){_me._sessionTimeout();}, _me._window);
			_me._session.deactivate();
			_me._populateLoggedOut();
		}

		else
		{
			// start our session inSession - if we have old values that are good, they remain - if they are now inactive, _sessionTimeout will be called, and our cookie will be gone
			_me._session = new e3_Session(true, function(){_me._sessionTimeout();}, _me._window);

			// if we still have a cookie, do a checkAuth
			if (document.cookie.indexOf("EtudesToken=") != -1)
			{
				var params = _me._cdp.params();

				var siteId = _me._session.get("portal:site");
				if (siteId != null)
				{
					params.post.extraSiteId = siteId;
				}

				_me._loggingIn = true;
				_me._cdp.request("checkAuth", params, function(data)
				{
					_me._loggingIn = false;
					if (0 == data["cdp:status"])
					{
						_me._user = data.user;
						_me._sites = data.sites;
						_me._extraSites = data.extraSites;
						_me._session.activate();
						_me._populateLoggedIn();
					}
					else
					{
						_me._session.deactivate();
						_me._populateLoggedOut();
					}
					_me._setAuthentication();
				});
			}
		}
		
		// watch for changes in authentication
		_me._authCheckTimer = setInterval(function(){try{_me._noticeAuthenticationChange();}catch(e){error(e);}}, _me._authCheckInterval);
	};

	this._sessionTimeout = function()
	{
		// drop the cookie
		document.cookie = "EtudesToken=; expires=Thu, 01 Jan 1970 00:00:00 GMT; domain=" + _me._cdp.domain + "; path=/";

		_me._user = null;
		_me._sites = null;
		_me._extraSites = null;
		_me._site = null;
		_me._populateLoggedOut();
	};

	this._populateBannerLinks = function()
	{
		var area = $("#portal_banner_links");
		$(area).empty();
		
		// not for admin / helpdesk
		if (_me._isAdmin() || _me._isHelpdesk()) return;

		$.each(_me._bannerTools || [], function(index, tool)
		{
			var text = _me._i18n.lookup(tool.title, tool.title);
			var a = $("<a />", {href:""});
			$(area).append(a);
			(a).addClass("portal_banner_link");
			$(a).html(text);
			$(a).attr("title", _me._i18n.lookup("clickTo", "Click to use the %0 tool", "html", [text]));
			onClick(a, function(){_me._clearToolReturn();_me._selectGlobalTool(tool);});
		});
		var a = $("<a />",{target:"_blank", href:"http://etudes.org/help", title:_me._i18n.lookup("help", "Click for help", "title")});
		$(a).html(_me._i18n.lookup("help", "Help", "html"));
		$(area).append(a);
		
		// dev mode portal configure
		a = $("<a />",{ href:""});
		$(a).addClass("portal_tool_titlebar_configure");
		$(a).addClass("e3_toolUiLinkI");
		$(a).html("&nbsp;");
		onClick(a, function(){_me._toggleRSidebar();});
		$(area).append(a);
	};

	this._noTool = function()
	{
		$("#portal_toolStage").empty();
		$("#portal_tool").addClass("e3_offstage");
		$("#portal_notool").removeClass("e3_offstage");		
	};

	this._loadTool = function(toolUrl)
	{
		// load selected tool
		$("#portal_toolStage").empty().load(toolUrl + ".html #portal_content", function(responseText, textStatus, XMLHttpRequest)
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
					$("#portal_tool").removeClass("e3_offstage");
					$("#portal_notool").addClass("e3_offstage");
				});
			}
			else
			{
				_me._cdpError(5);
			}
		});
	};

	this._populateLoggedIn = function()
	{
		$("#portal_loggedinas").empty().html(_me._user.nameDisplay);
		$("#login_userid").val("");
		$("#login_password").val("");

		_me._populateBannerLinks();

		$("#portal_banner_loggedout").addClass("e3_offstage");
		$("#portal_banner_loggedin").removeClass("e3_offstage");

		_me._stopPresence();
		_me._site = null;
		_me._populateSites();

		// the last site visited
		var siteId = _me._session.get("portal:site");
		var site = null;
		if (siteId != null)
		{
			site = _me._findSite(siteId);
		}
	
		// for admin
		else if (_me._isAdmin())
		{
			site = _me._findSite(2);
		}
		else if (_me._isHelpdesk())
		{
			site = _me._findSite(3);
		}

		if (site == null)
		{
			var selected = null;

			// check for a global tool
			var toolId = _me._session.get("portal:tool:global");
			if (toolId != null)
			{
				selected = _me._findBannerTool(toolId);
			}

			if (selected == null)
			{
				selected = _me._bannerTools[0];
			}

			_me._selectGlobalTool(selected);
		}
		else
		{
			_me._visitSite(site);
		}
	};

	this._navigate = function(site, toolId, enableReturn, newWindow)
	{
		// site is id or object
		if (site != null)
		{
			if (site.id == null) site = _me._findSite(site);
		}

		if (newWindow)
		{
			var nextWindow = _me._session.get("portal:nextWindow");
			if (nextWindow == null) nextWindow = "1";

			var newWindow = _me._window + "." + nextWindow;
			
			nextWindow = parseInt(nextWindow) + 1;
			_me._session.put("portal:nextWindow", nextWindow.toString());

			if (site != null) _me._session.put("portal:site", site.id, newWindow);
			if (toolId != null) _me._session.put("portal:tool:" + site.id, toolId, newWindow);

			var newLoc = window.location.origin + window.location.pathname + "?w=" + newWindow;
			window.open(newLoc);

			return;
		}

		if (enableReturn)
		{
			var fromSite = _me._session.get("portal:site");
			var fromTool = _me._session.get("portal:tool:" + fromSite);
			var fromGlobal = _me._session.get("portal:tool:global");
			_me._enableToolReturn(fromSite, (fromSite != null) ? fromTool : fromGlobal);
		}
		else if (enableReturn != null)
		{
			_me._clearToolReturn();
		}

		_me._stopPresence();
		_me._site = null;
		_me._populateSites();

		if (site == null)
		{
			var selected = _me._findBannerTool(toolId);
			_me._selectGlobalTool(selected);
		}
		else
		{
			_me._session.put("portal:tool:" + site.id, toolId);
			_me._visitSite(site);
		}
	};

	this._clearToolReturn = function()
	{
		_me._toolReturn = null;
	};
	
	this._enableToolReturn = function(site, toolId)
	{
		_me._toolReturn = {site:site, toolId:toolId};
	};

	this._pickTabSites = function()
	{
		var rv = [];
		var currentSiteIncluded = false;
		var moreSitesExist = false;
		var numToPick = _me._sites.length;
		if (numToPick > _me._siteTabLimit) numToPick = _me._siteTabLimit;
		$.each(_me._sites || [], function(index, site)
		{
			if (index >= numToPick)
			{
				moreSitesExist = true;
			}
			else
			{
				rv.push(site);
				if ((_me._site != null) && (site.id == _me._site.id)) currentSiteIncluded = true;
			}
		});

		if ((_me._site != null) && !currentSiteIncluded)
		{
			rv.push(_me._site);
		}

		return {sites: rv, more: moreSitesExist};
	};

	this._populateSites = function()
	{
		var portalSites = $("#portal_sites");
		$(portalSites).empty();

		if ((_me._sites == null) || (_me._sites.length == 0))
		{
			$(portalSites).addClass("e3_offstage");
			return;
		}
		else
		{
			$(portalSites).removeClass("e3_offstage");			
		}

		var toPlace = _me._pickTabSites();

		$.each(toPlace.sites || [], function(index, site)
		{
			var a = $("<a />",{href:"", oid:site.id});
			$(portalSites).append(a);
			var span = $("<span />");
			$(a).append(span);
			$(span).html(site.name);
			
			// for fixed width between selected and not
			if (_me._options.sites == "F")
			{
				$(span).addClass("tabs");
				$(a).addClass("tabs");
				$(a).addClass("current");
				$(span).width($(span).width());
				$(a).removeClass("current");
			}
			
			// for changing width tabs
			else if (_me._options.sites == "T")
			{
				$(span).addClass("tabs");
			}

			onClick(a, function(){_me._siteClicked(site);});

			if ((_me._site != null) && (site.id == _me._site.id))
			{
				$(a).addClass("current");
				$(a).find("span.tabs").css("color", _me._skinColor);
			}
			else if ((!site.published) && (site.role < 5)) // < instructor
			{
				$(a).addClass("unpublished");
			}
			
			if (index < toPlace.sites.length-1)
			{
				var div = $("<div />");
				$(portalSites).append(div);
				$(div).html("&nbsp;");
			}
		});
		
		if (toPlace.more)
		{
			var a = $("<a />",{href:"", title:_me._i18n.lookup("moreSites", "More in My Sites", "title")});
			$(portalSites).append(a);
			$(a).addClass("portal_sites_more");
			$(a).html("&nbsp;");
			onClick(a, function(){_me._clearToolReturn();_me._selectGlobalTool(_me._findBannerTool(52));});
		}
	};

	this._updateCurrentSite = function()
	{
		_me._populateSites();
	};

	this._siteClicked = function(site)
	{
		// for the current site
		if ((_me._site != null) && (_me._site.id == site.id))
		{
		}
		
		else if ((!site.published) && (site.role < 3))
		{
			_me._dialogs.openAlert("portal_unpublished_alert");
		}

		else
		{
			_me._clearToolReturn();
			_me._visitSite(site);
		}
	};

	this._populatePresence = function()
	{
		// if (_me._site.oldPresence != null) _me._comparePresence();

		var area = $("#portal_presence");
		$(area).empty();
		$.each(_me._site.presence || [], function(index, user)
		{
			var div = $("<div />");
			$(area).append(div);
			$(div).html(user.nameDisplay);
		});

		$("#portal_presence_area").removeClass("e3_offstage");
	};

	this._loadPresence = function(firstVisit)
	{
		var params = _me._cdp.params();
		params.url.id = _me._site.id;
		if ((firstVisit != undefined) && (firstVisit == true))
		{
			params.url.track = "1";
		}
		_me._cdp.request("portal_presence", params, function(data)
		{
			// TODO: could we have timed out, been sent to gateway, and lost (null) "_me._site"?
			_me._site.oldPresence = _me._site.presence;
			_me._site.presence = data.presence;
			_me._populatePresence();
		});
	};
	
	this._hidePresence = function()
	{
		$("#portal_presence_area").addClass("e3_offstage");
	};

	this._findUserInOldPresence = function(user)
	{
		var found = null;
		if (_me._site.oldPresence != null)
		{
			$.each(_me._site.oldPresence, function(index, u)
			{
				if (u.id == user.id) found = u;
			});
		}
		
		return found;
	};

	this._findUserInPresence = function(user)
	{
		var found = null;
		$.each(_me._site.presence || [], function(index, u)
		{
			if (u.id == user.id) found = u;
		});
		
		return found;
	};

	this._populateSiteTools = function()
	{
		var portalSiteTools = $("#portal_site_tools");
		$(portalSiteTools).empty();
		var lastToolA = null;
		if ((_me._site.tools != null) && (_me._site.tools.length > 0))
		{
			$.each(_me._site.tools || [], function(index, tool)
			{
				// check the tool's role requirement against the user's role in site - skip if the user's role is less
				if (tool.role > _me._site.role) return;

				var text = _me._i18n.lookup(tool.title, tool.title);
				var a = $("<a />",{href:"", oid:tool.id});
				lastToolA = a;
				$(portalSiteTools).append(a);
				$(a).html(text);
				onClick(a, function(){_me._toolClicked(tool);});
				$(a).attr("titleSelected", _me._i18n.lookup("isSelected", "The %0 tool is selected", "html", [text]));
				$(a).attr("titleNotSelected", _me._i18n.lookup("clickTo", "Click to use the %0 tool", "html", [text]));
				$(a).attr("title", $(a).attr("titleNotSelected"));
			});
			if (lastToolA != null) $(lastToolA).addClass("last");
		}

		var portalMaintenanceTools = $("#portal_maintenance_tools");
		$(portalMaintenanceTools).empty();
		// TODO: only for instructors - allow TAs?
		if ((_me._site.role >= 4) && (!_me._onAdmin()) && (!_me._onHelpdesk())) 
		{
			var text = _me._i18n.lookup(_me._site.setupTool.title, _me._site.setupTool.title);
			var a = $("<a />",{href:""});
			$(a).addClass("portal_maintenance_setup");
			$(portalMaintenanceTools).append(a);
			onClick(a, function(){_me._toolClicked(_me._site.setupTool);});
			$(a).attr("title", _me._i18n.lookup("clickTo", "Click to use the %0 tool", "html", [text]));
			$(a).html(text);

			text = _me._i18n.lookup(_me._site.rosterTool.title, _me._site.rosterTool.title);
			a = $("<a />",{href:""});
			$(a).addClass("portal_maintenance_roster");
			$(portalMaintenanceTools).append(a);
			onClick(a, function(){_me._toolClicked(_me._site.rosterTool);});
			$(a).attr("title", _me._i18n.lookup("clickTo", "Click to use the %0 tool", "html", [text]));
			$(a).html(text);
		}
	};

	this._updateCurrentTool = function()
	{
		$.each($("#portal_site_tools a.current"), function(index, toolA)
		{
			$(toolA).removeClass("current");
			$(toolA).css("background-color", "#F5F4F4");
			$(toolA).attr("title", $(toolA).attr("titleNotSelected"));
		});

		if ((_me._site != null) && (_me._site.currentToolId != null))
		{
			$.each($("#portal_site_tools a[oid=" + _me._site.currentToolId + "]"), function(index, toolA)
			{
				$(toolA).addClass("current");
				$(toolA).css("background-color", _me._skinColor);
				$(toolA).attr("title", $(toolA).attr("titleSelected"));
			});
		}
	};

	this._toolClicked = function(tool)
	{
		// for the current site tool
		if ((_me._site != null) && (_me._site.currentToolId == tool.id))
		{
		}

		else
		{
			_me._clearToolReturn();
			_me._selectSiteTool(tool);
		}
	};

	this._populateSiteLinks = function()
	{
		var portalSiteLinks = $("#portal_site_links");
		$(portalSiteLinks).empty();
		if ((_me._site.links != null) && (_me._site.links.length > 0))
		{
			$.each(_me._site.links || [], function(index, link)
			{
				var a = $("<a />",{href:link.url, target:"_blank", title: _me._i18n.lookup("clickToLink", "Click to visit")}).addClass("e3_extLink").addClass("e3_toolUiLink");
				$(portalSiteLinks).append(a);
				$(a).text(link.title);
				$(a).css("color", _me._skinColor);
			});
		}
	};

	this._populateLoggedOut = function()
	{
		$("#portal_banner_loggedin").addClass("e3_offstage");
		$("#portal_banner_loggedout").removeClass("e3_offstage");

		_me._noTool();
		_me._disableSidebar();
		
		_me._site = null;
		_me._stopPresence();
		_me._populateSites();
		_me._setSkin();

		$("#login_userid").focus();
		_me._visitGateway();
	};
	
	this._visitGateway = function()
	{
		var params = _me._cdp.params();
		_me._cdp.request("gateway", params, function(data)
		{
			_me._visitSite(data.gateway);
		});
	};

	this._disableSidebar = function()
	{
		$("#portal_tool_sidebar_toggle").addClass("e3_offstage");
		$("#portal_sidebar").addClass("e3_offstage");
		_me._sidebarEnabled = false;
	};

	this._enableSidebar = function()
	{
		var sidebar = $("#portal_sidebar");
		var toggle = $("#portal_tool_sidebar_toggle");
		if (_me._options.sidebar)
		{
			$(toggle).removeClass("e3_offstage");		
		}
		$(toggle).removeClass("portal_tool_collapse_button").addClass("portal_tool_expand_button");			
		$(sidebar).removeClass("e3_offstage");
		_me._sidebarEnabled = true;
	};

	this._toggleSidebar = function()
	{
		var sidebar = $("#portal_sidebar");
		if ($(sidebar).hasClass("e3_offstage"))
		{
			$(sidebar).removeClass("e3_offstage");
			$("#portal_tool_sidebar_toggle").removeClass("portal_tool_collapse_button").addClass("portal_tool_expand_button");
		}
		else
		{
			$(sidebar).addClass("e3_offstage");
			$("#portal_tool_sidebar_toggle").removeClass("portal_tool_expand_button").addClass("portal_tool_collapse_button");
		}
	};

	this._toggleOpenSidebar = function()
	{
		var sidebar = $("#portal_sidebar");
		if ($(sidebar).hasClass("e3_offstage"))
		{
			$(sidebar).removeClass("e3_offstage");
			$("#portal_tool_sidebar_toggle").removeClass("portal_tool_collapse_button").addClass("portal_tool_expand_button");
		}
	};

	this._closeRSidebar = function()
	{
		$("#portal_rsidebar").addClass("e3_offstage");
	};

	this._openRSidebar = function()
	{
		var sidebar = $("#portal_rsidebar");
		if ($(sidebar).hasClass("e3_offstage"))
		{
			$(sidebar).removeClass("e3_offstage");
		}
	};

	this._toggleRSidebar = function()
	{
		var sidebar = $("#portal_rsidebar");
		if ($(sidebar).hasClass("e3_offstage"))
		{
			$(sidebar).removeClass("e3_offstage");
		}
		else
		{
			$(sidebar).addClass("e3_offstage");
		}
	};

	this._login = function()
	{
		var params = _me._cdp.params();
		params.post.password = $.trim($("#login_password").val());
		params.url.userid = $.trim($("#login_userid").val());
		// params.post.extras = "1"; // unless we are reloading, get the user and sites info
		_me._loggingIn = true;
		_me._cdp.request("login", params, function(data)
		{
			_me._loggingIn = false;
			if (0 == data["cdp:status"])
			{
				_me._leaveSite();
				
				// if we reload, chrome/(win,mac) will only offer to save the user id / password if we force a reload; IE and FF don't need it; Safari never does.
				location.reload();

				// if we don't reload ...
//				_me._user = data.user;
//				_me._sites = data.sites;
//				_me._session.activate();
//				_me._populateLoggedIn();
//				_me._setAuthentication();
			}
			else
			{
				_me._dialogs.openAlert("login_invalid_alert");				
			}
		});

		return true;
	};

	this._logout = function()
	{
		if (!_me._leaveTool(function(){_me._logout();})) return;

		var params = _me._cdp.params();
		_me._cdp.request("logout", params, function(data)
		{
			if (0 == data["cdp:status"])
			{
				_me._session.deactivate();

				// if we reload, chrome will offer saved user id / passwords for the next login, else it won't
				location.reload();

				// if we are not reloading...
//				_me._user = null;
//				_me._sites = null;
//				_me._extraSites = null;
//				_me._site = null;
//				_me._populateLoggedOut();
//				_me._setAuthentication();
			}
			else
			{
				_me._dialogs.openAlert("logout_invalid_alert");				
			}
		});
	};

	this._visitSite = function(site)
	{
		if (!_me._leaveTool(function(){_me._visitSite(site);})) return;

		_me._leaveSite();

		_me._site = site;
		_me._setSkin();
		_me._session.put("portal:site", site.id);
		_me._updateCurrentSite();

		var firstVisit = (_me._session.get("portal:visit:" + site.id) == null);
		if (firstVisit) _me._session.put("portal:visit:" + site.id, site.id);

		if ((_me._site.tools.length > 0) || (_me._site.links.length > 0))
		{
			_me._populateSiteTools();
			_me._selectLandingTool();
			_me._populateSiteLinks();
			_me._enableSidebar();
		}
		else
		{
			_me._noSiteTool();
		}		

		$("#portal_siteClosed").addClass("e3_offstage");
		$("#portal_siteWillOpen").addClass("e3_offstage");
		if (_me._site.accessStatus == 1)
		{
			$("#portal_siteWillOpen").removeClass("e3_offstage");			
		}
		else if (_me._site.accessStatus == 2)
		{
			$("#portal_siteClosed").removeClass("e3_offstage");			
		}
		
		_me._site.presence = null;
		_me._site.oldPresence = null;
		
		if (_me._site.presenceEnabled)
		{
			_me._loadPresence(firstVisit);
			
			// start presence check/report
			_me._presenceTimer = setInterval(function(){try{_me._loadPresence();}catch (e){error(e);}}, _me._presenceInterval);
		}
		else
		{
			_me._hidePresence();
		}
	};

	this._setSkin = function()
	{
		var color = "#730416";
		var skin = "Etudes";
		if ((_me._site || {}).skin != null)
		{
			color = "#" + _me._site.skin.color;
			skin = _me._site.skin.name;
		}

		_me._skinColor = color;

		// $(".portal_sites a.current span.tabs").css("color", color);
		$(".portal_footer hr").css("color", color);
		// $("#portal_site_tools a.current").css("background-color", color);
		// $("#portal_site_tools a.current:hover").css("background-color", color);
		$(".portal_banner").css("background-color", color);
		$(".portal_banner").css("background-image", "url(/support/skin/" + skin + "/headback.gif)");
		$(".portal_banner_info a").css("color", color);
		$(".portal_banner_info a:link").css("color", color);
		$(".portal_banner_info a:visited").css("color", color);		
		$(".portal_banner_links").css("color", color);		
		$(".portal_banner_link").css("border-right-color", color);
		$(".portal_sites").css("background-color", color);		
		$("div.portal_banner_logo img").attr("src", "/support/skin/" + skin + "/logo_inst.gif");
		$("div.portal_banner_banner img").attr("src", "/support/skin/" + skin + "/banner_inst.gif");
		$(".portal_banner_loggedinas_name").css("border-right-color", color);
		$("#portal_footer_links").css("color", color);
		$("a.portal_footer_link:link").css("color", color);
		$("a.portal_footer_link:visited").css("color", color);
		$("a.portal_footer_link:hover").css("color", color);
		$("a.portal_footer_link:active").css("color", color);
		$("a.portal_footer_link:focus").css("color", color);
	};

	this._selectLandingTool = function()
	{
		var selected = null;

		// last visited tool in session for site
		var toolId = _me._session.get("portal:tool:" + _me._site.id);
		if (toolId != null)
		{
			selected = _me._findTool(_me._site, toolId);
			if ((selected != null) && (selected.role > _me._site.role)) selected = null;
		}

		if (selected == null)
		{
			$.each(_me._site.tools || [], function(index, tool)
			{
				// skip tools the user cannot see
				if (tool.role > _me._site.role) return;

				// pick the first
				if (selected == null)
				{
					selected = tool;
				}
			});
		}

		if (selected != null)
		{
			_me._selectSiteTool(selected);
		}
		else
		{
			_me._noSiteTool();
		}
	};

	this._noSiteTool = function()
	{
		_me._noTool();
		_me._disableSidebar();
		_me._site.currentToolId = null;
		_me._session.remove("portal:tool:" + _me._site.id);
	};

	this._selectSiteTool = function(tool)
	{
		if (!_me._leaveTool(function(){_me._selectSiteTool(tool);})) return;

		_me._site.currentToolId = tool.id;
		_me._session.put("portal:tool:" + _me._site.id, tool.id);
		_me._updateCurrentTool();

		_me._session.remove("portal:tool:global");

		_me._loadTool(tool.url);
	};

	this._selectGlobalTool = function(tool)
	{
		if (!_me._leaveTool(function(){_me._selectGlobalTool(tool);})) return;

		_me._disableSidebar();
		_me._leaveSite();
		_me._session.put("portal:tool:global", tool.id);
		_me._loadTool(tool.url);
		_me._setSkin();
	};

	this._jiffyLube = function(reset)
	{
		if (!_me._leaveTool(function(){_me._jiffyLube(reset);})) return;
		try
		{
			reset();
		}
		catch(e)
		{
			error(e);
		}
	};

	this._leaveTool = function(deferred)
	{
		if (_me._toolExit != null)
		{
			try
			{
				if (!_me._toolExit(deferred))
				{
					return false;
				}
				_me._toolExit = null;
			}
			catch (err)
			{
				error(err);
			}
		}

		return true;
	};

	this._leaveSite = function()
	{
		_me._site = null;
		_me._session.remove("portal:site");

		_me._updateCurrentSite();
		
		_me._stopPresence();
	};

	this._stopPresence = function()
	{
		if (_me._presenceTimer != null)
		{
			clearInterval(_me._presenceTimer);
			_me._presenceTimer = null;
		}
	};

	this._refresh = function()
	{
		var params = _me._cdp.params();
		
		var siteId = _me._session.get("portal:site");
		if (siteId != null)
		{
			params.post.extraSiteId = siteId;
		}

		_me._cdp.request("checkAuth", params, function(data)
		{
			if (0 == data["cdp:status"])
			{
				_me._user = data.user;
				_me._sites = data.sites;
				_me._extraSites = data.extraSites;
				_me._populateLoggedIn();
			}
			else
			{
				_me._session.deactivate();
				_me._populateLoggedOut();
			}
		});
	};

	this._refreshSites = function(sites)
	{
		_me._sites = sites;
		_me._populateLoggedIn();
	};

	this._portalOptionsChanged = function()
	{
		_me._options.sites = $("input:radio[name=portal_option_sites]:checked").val();
		_me._populateSites();
		
		_me._options.sidebar = $("#portal_option_sidebar").prop('checked');
		if (_me._options.sidebar)
		{
			if (_me._sidebarEnabled)
			{
				$("#portal_tool_sidebar_toggle").removeClass("e3_offstage");
				if ($("#portal_tool_sidebar_toggle").hasClass("portal_tool_expand_button"))
				{
					$("#portal_sidebar").removeClass("e3_offstage");
				}
				else
				{
					$("#portal_sidebar").addClass("e3_offstage");
				}
			}		
		}
		else
		{
			$("#portal_tool_sidebar_toggle").addClass("e3_offstage");
			if (_me._sidebarEnabled)
			{
				$("#portal_sidebar").removeClass("e3_offstage");
			}
		}

		_me._options.autoSidebar = $("#portal_option_autoSidebar").prop('checked');
		$("#portal_tool_sidebar_toggle").off("mouseenter");
		if (_me._options.autoSidebar)
		{
			$("#portal_tool_sidebar_toggle").on("mouseenter", function(){try{_me._toggleOpenSidebar();}catch (e){error(e);};return false;});
		}
		
		_me._options.editor.size = $("input:radio[name=portal_option_editorSize]:checked").val();
		_me._options.editor.shape = $("input:radio[name=portal_option_editorShape]:checked").val();
		_me._options.editor.outline = $("input:radio[name=portal_option_editorOutline]:checked").val();
	};

	this._cdpError = function(code)
	{
		if (code == 1)
		{
			// skip if logging in
			if (!_me._loggingIn) _me._dialogs.openAlert("portal_accessDenied");			
		}
		else if (code == 2)
		{
			// skip if logging in
			if (!_me._loggingIn) _me._dialogs.openAlert("portal_notLoggedIn");			
		}
		else
		{
			_me._dialogs.openAlert("portal_cdpFailure");
		}
	};
	
	this._goFullScreen = function()
	{
		_me._disableSidebar();
	};

	this._updateUser = function(user)
	{
		// only if the same user
		if (_me._user.id == user.id)
		{
			_me._user = user;
		}
		
		return _me._user;
	};

	this._showSaveCancel = function(condition)
	{
		show(["portal_tool_configureExit","portal_tool_configureCancel"], condition);
	};

	this._enableSaveCancel = function(onSaveCancel)
	{
		if (onSaveCancel == null)
		{
			_me._showSaveCancel(false);
		}
		else
		{
			_me._showSaveCancel(true);

			onClick("portal_tool_configureExit", function()
			{
				try
				{
					var rv = onSaveCancel(true, function()
					{
						_me._showSaveCancel(false);
					});
					if ((rv === undefined) || rv)
					{
						_me._showSaveCancel(false);
					}
				}
				catch(e)
				{
					error(e);
				}
			});
			onClick("portal_tool_configureCancel", function()
			{
				try
				{
					var rv = onSaveCancel(false, function()
					{
						_me._showSaveCancel(false);
					});
					if ((rv === undefined) || rv)
					{
						_me._showSaveCancel(false);
					}
				}
				catch(e)
				{
					error(e);
				}
			});
		}
	};

	this._injectAttribution = function(obj, into)
	{			
		var target = ($.type(into) === "string") ? $("#" + into) : into;
		var ui = clone($("#portal_attributionTemplate"),["portal_attributionTemplate_id", "portal_attributionTemplate_created", "portal_attributionTemplate_modified"]);

		target.empty();
		hide(target);

		if ((obj == null) || (obj.id == null)) return;

		ui.portal_attributionTemplate_id.text(obj.id);
		ui.portal_attributionTemplate_created.text(obj.createdBy + (obj.createdOn == null ? "" : (", " + _me._timestamp.display(obj.createdOn))));
		ui.portal_attributionTemplate_modified.text(obj.modifiedBy + (obj.modifiedOn == null ? "" : (", " + _me._timestamp.display(obj.modifiedOn))));

		target.append(ui.element.children());
		show(target);
	};

	// public function (portal_tool.features(title, resetFunction/null, configureFunction/null) - call on tool init - returns the selected site, all sites, the user, error handler
	this.features = function(title, reset, configure, exit)
	{
		if (exit !== undefined)
		{
			_me._toolExit = exit;
		}

		$("#portal_tool_configure").addClass("e3_offstage");
		_me._showSaveCancel(false);

		$("#portal_tool_titlebar_title").empty().html(title);
		if (reset == null)
		{
			$("#portal_tool_reset").addClass("e3_offstage");
		}
		else
		{
			$("#portal_tool_reset").removeClass("e3_offstage");
//			onClick("portal_tool_reset", function(){try{reset();}catch(e){error(e);}});
			onClick("portal_tool_reset", function(){_me._jiffyLube(reset);});
		}
		if (configure != null)
		{
			$("#portal_tool_configure").removeClass("e3_offstage");
			onClick("portal_tool_configure", function()
			{
				try
				{
					var rv = configure()
					if ((rv === undefined) || (rv))
					{
						$("#portal_tool_configure").addClass("e3_offstage");
						_me._showSaveCancel(true);
					}
				}
				catch(e)
				{
					error(e);
				}
			});
			onClick("portal_tool_configureExit", function()
			{
				try
				{
					var rv = configure(true, function()
					{
						$("#portal_tool_configure").removeClass("e3_offstage");
						_me._showSaveCancel(false);
					});
					if ((rv === undefined) || (rv))
					{
						$("#portal_tool_configure").removeClass("e3_offstage");
						_me._showSaveCancel(false);
					}
				}
				catch(e)
				{
					error(e);
				}
			});
			onClick("portal_tool_configureCancel", function()
			{
				try
				{
					var rv = configure(false, function()
					{
						$("#portal_tool_configure").removeClass("e3_offstage");
						_me._showSaveCancel(false);
					});
					if ((rv === undefined) || (rv))
					{
						$("#portal_tool_configure").removeClass("e3_offstage");
						_me._showSaveCancel(false);
					}
				}
				catch(e)
				{
					error(e);
				}
			});
		}

		var rv = {};
		rv.site = _me._site;
		rv.sites = _me._sites;
		rv.errorHandler = _me._cdpError;
		rv.refresh = _me._refresh;
		rv.tz = _me._cdp.tz;
		rv.refreshSites = _me._refreshSites;
		rv.navigate = _me._navigate;
		rv.fullScreen = _me._goFullScreen;
		if (_me._toolReturn != null) rv.toolReturn = _me._toolReturn;
		rv.user = _me._user;
		rv.updateUser = _me._updateUser;
		rv.enableSaveCancel = _me._enableSaveCancel;
		rv.injectAttribution = _me._injectAttribution;
		rv.itemNav = new Portal_itemNav(_me);
		rv.timestamp = _me._timestamp;
		rv.dialogs = _me._dialogs;
		rv.cdp = _me._cdp;

		return rv;
	};
};

function Portal_itemNav(main)
{
	var me = this;
	this.ui = null;

	// settings:{pos, returnFunction, saveFunction, navigateFunction}
	this.inject = function(into, settings)
	{
		var target = ($.type(into) === "string") ? $("#" + into) : into;
		me.ui = clone($("#portal_itemNavTemplate"),["portal_itemNavTemplate_save", "portal_itemNavTemplate_discard", "portal_itemNavTemplate_return", "portal_itemNavTemplate_prev", "portal_itemNavTemplate_pos", "portal_itemNavTemplate_next"]);

		target.empty();

		show(me.ui.portal_itemNavTemplate_return, settings.returnFunction !== undefined);
		if (settings.returnFunction !== undefined)
		{
			onClick(me.ui.portal_itemNavTemplate_return, function(){settings.returnFunction();});
		}
		
		show([me.ui.portal_itemNavTemplate_save, me.ui.portal_itemNavTemplate_discard], settings.saveFunction !== undefined);
		if (settings.saveFunction !== undefined)
		{
			onClick(me.ui.portal_itemNavTemplate_save, function(){settings.saveFunction(true);});
			onClick(me.ui.portal_itemNavTemplate_discard, function(){settings.saveFunction(false);});
		}

		show([me.ui.portal_itemNavTemplate_prev, me.ui.portal_itemNavTemplate_pos, me.ui.portal_itemNavTemplate_next], settings.pos !== undefined)
		if (settings.pos !== undefined)
		{
			onClick(me.ui.portal_itemNavTemplate_prev, function(){settings.navigateFunction(settings.pos.prev);});
			me.ui.portal_itemNavTemplate_prev.attr("disabled", (settings.pos.prev == null));
			applyClass("e3_disabled", me.ui.portal_itemNavTemplate_prev, (settings.pos.prev == null));
	
			me.ui.portal_itemNavTemplate_pos.text(main._i18n.lookup("msg_pos", "%0 of %1", "html", [settings.pos.item, settings.pos.total]));
	
			onClick(me.ui.portal_itemNavTemplate_next, function(){settings.navigateFunction(settings.pos.next);});
			me.ui.portal_itemNavTemplate_next.attr("disabled", (settings.pos.next == null));
			applyClass("e3_disabled", me.ui.portal_itemNavTemplate_next, (settings.pos.next == null));
		}

		target.append(me.ui.element);
		show(me.ui.element);
		show(target);
	};
	
	this.showChanged = function(changed)
	{
		me.ui.portal_itemNavTemplate_save.attr("disabled", !changed);
		applyClass("e3_disabled", me.ui.portal_itemNavTemplate_save, !changed);

		me.ui.portal_itemNavTemplate_discard.attr("disabled", !changed);
		applyClass("e3_disabled", me.ui.portal_itemNavTemplate_discard, !changed);
	};
};

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

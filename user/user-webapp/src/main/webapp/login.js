/**********************************************************************************
 * $URL: https://source.etudes.org/svn/serenity/trunk/user/user-webapp/src/main/webapp/login.js $
 * $Id: login.js 12504 2016-01-10 00:30:08Z ggolden $
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

var login_tool = null;

function Login()
{
	var me = this;

	this.portal = null;
	this.cdp = new e3_Cdp({});
	this.i18n = new e3_i18n(login_i10n, "en-us");
	this.ui = null;

	this.init = function()
	{
		me.i18n.localize();
		me.ui = findElements(["login_userid", "login_password", "login_enter", "login_help", "login_reset", "login_browse", "login_invalid", "login_form"]);
		me.portal = portal_tool.features({});

		me.ui.login_form.off("submit").on("submit", function(event){try{event.preventDefault(); me.login(); return false;}catch(e){error(e);}});

		onClick(me.ui.login_reset, me.resetPw);
		onClick(me.ui.login_browse, me.browseSites);
		
		onHover([me.ui.login_help, me.ui.login_reset, me.ui.login_browse],
				function(t)
				{
					t.stop().animate({color:"#000000", backgroundColor:"rgba(255,255,255,1)"}, Hover.quick);
				},
				function(t)
				{
					t.stop().animate({color:"#FFFFFF", backgroundColor:"rgba(0,0,0,0.6)"}, Hover.quick);
				});

		onHover(me.ui.login_enter,
				function(t)
				{
					if (t.hasClass("disabled")) return;
					t.stop().animate({backgroundColor:"rgba(0,0,0,1)"}, Hover.quick);
				},
				function(t)
				{
					if (t.hasClass("disabled")) return;
					t.stop().animate({backgroundColor:"rgba(0,0,0,0)"}, Hover.quick);
				});

		onFocus(me.ui.login_enter,
				function(t)
				{
					t.stop().animate({backgroundColor:"rgba(0,0,0,1)"}, Hover.quick);
				},
				function(t)
				{
					t.stop().animate({backgroundColor:"rgba(0,0,0,0)"}, Hover.quick);
				});

		onChange(me.ui.login_userid, me.dataEntered);
		onChange(me.ui.login_password, me.dataEntered);		
	};

	this.start = function()
	{
		me.setupIncomplete();

		me.ui.login_userid.val("");
		me.ui.login_password.val("");

		me.ui.login_userid.focus();
	};
	
	this.setupIncomplete = function()
	{
		me.ui.login_enter.stop().addClass("disabled").removeClass("highlighted").attr("disabled", true).css({backgroundColor:"rgba(0,0,0,0)"});
		applyClass("highlighted", [me.ui.login_help, me.ui.login_reset], false);
		show(me.ui.login_enter);
		hide(me.ui.login_invalid);
	};
	
	this.setupComplete = function()
	{
		me.ui.login_enter.stop().removeClass("disabled").addClass("highlighted").removeAttr("disabled").css({backgroundColor:"rgba(0,0,0,0)"});
		applyClass("highlighted", [me.ui.login_help, me.ui.login_reset], false);
		show(me.ui.login_enter);
		hide(me.ui.login_invalid);
	};

	this.setupInvalid = function()
	{
		applyClass("highlighted", [me.ui.login_help, me.ui.login_reset], true);
		show(me.ui.login_invalid);
		hide(me.ui.login_enter);
	};

	this.login = function()
	{
		if (me.ui.login_enter.hasClass("disabled")) return;
		me.setupIncomplete();

		var params = me.cdp.params();
		params.url.userid = trim(me.ui.login_userid.val());
		params.post.password = trim(me.ui.login_password.val());
		if ((params.url.userid == null) || (params.post.password == null)) return;
		me.cdp.request("login portal_info", params, function(data)
		{
			if (0 == data["cdp:status"])
			{
				me.portal.login(data);
			}
			else
			{
				me.invalidParams = params;
				me.setupInvalid();
			}
		});
	};
	
	this.dataEntered = function()
	{
		// if we have an invalid attempt, and there is no difference, ignore
		var userid = trim(me.ui.login_userid.val());
		var password = trim(me.ui.login_password.val());
		if ((me.invalidParams != null) && (me.invalidParams.url.userid == userid) && (me.invalidParams.post.password == password)) return;

		me.invalidParams = null;
		
		var complete = (userid != null) && (password != null);
		if (!complete)
		{
			me.setupIncomplete();
		}
		else
		{
			me.setupComplete();
		}
	};
	
	this.resetPw = function()
	{
		me.portal.navigate(null, Tools.resetpw, false, false);
	};

	this.browseSites = function()
	{
		me.portal.navigate(null, Tools.browseSites, false, false);
	};
}

$(function()
{
	try
	{
		login_tool = new Login();
		login_tool.init();
		login_tool.start();
	}
	catch (e)
	{
		error(e);
	}
});

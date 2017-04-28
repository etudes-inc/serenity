/**********************************************************************************
 * $URL: https://source.etudes.org/svn/serenity/trunk/user/user-webapp/src/main/webapp/resetpw.js $
 * $Id: resetpw.js 11126 2015-06-19 04:18:43Z ggolden $
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

var resetpw_tool = null;

function ResetPw()
{
	var me = this;

	this.portal = null;
	this.cdp = new e3_Cdp({});
	this.i18n = new e3_i18n(resetpw_i10n, "en-us");
	this.ui = null;
	
//	this.loginTool = {id:10, title:"Login", url:"/user/login", role: 0};

	this.init = function()
	{
		me.i18n.localize();
		me.ui = findElements(["resetpw_login","resetpw_email","resetpw_resetSent","resetpw_resetInvalid","resetpw_send","resetpw_relogin"]);
		me.portal = portal_tool.features(me.i18n.lookup("titlebar", "Reset Password"));
		
		onClick(me.ui.resetpw_send, me.sendPassword);
		onClick(me.ui.resetpw_login, me.login);
		onClick(me.ui.resetpw_relogin, me.login);
		onChange(me.ui.resetpw_email, me.dataEntered);
	};

	this.start = function()
	{
		hide(me.ui.resetpw_relogin);
		hide(me.ui.resetpw_resetSent);
		hide(me.ui.resetpw_resetInvalid);
		me.ui.resetpw_email.val("");
		me.ui.resetpw_send.addClass("disabled");
		show(me.ui.resetpw_send);
		me.ui.resetpw_email.focus();
	};

	this.login = function()
	{
		me.portal.navigate(null, Tools.login, false, false);
	};

	this.dataEntered = function()
	{
		var email = trim(me.ui.resetpw_email.val());
		applyClass("disabled", me.ui.resetpw_send, (email == null));
		if (email != null) hide(me.ui.resetpw_resetInvalid);
	};

	this.sendPassword = function()
	{
		if (me.ui.resetpw_send.hasClass("disabled")) return;
		me.ui.resetpw_send.addClass("disabled");

		var params = me.cdp.params();
		params.post.email = trim(me.ui.resetpw_email.val());
		if (params.post == null) return;

		me.cdp.request("user_resetPw", params, function(data)
		{
			if (data.reset)
			{
				show(me.ui.resetpw_resetSent);
				hide(me.ui.resetpw_send);
				show(me.ui.resetpw_relogin);
			}
			else
			{
				show(me.ui.resetpw_resetInvalid);
			}
		});
	};
}

$(function()
{
	try
	{
		resetpw_tool = new ResetPw();
		resetpw_tool.init();
		resetpw_tool.start();
	}
	catch (e)
	{
		error(e);
	}
});

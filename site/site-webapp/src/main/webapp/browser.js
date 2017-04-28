/**********************************************************************************
 * $URL: https://source.etudes.org/svn/serenity/trunk/site/site-webapp/src/main/webapp/browser.js $
 * $Id: browser.js 11119 2015-06-17 19:20:25Z ggolden $
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

var browser_tool = null;

function Browser()
{
	var me = this;

	this.portal = null;
	this.i18n = new e3_i18n(browser_i10n, "en-us");
	this.loginTool = {id:10, title:"Login", url:"/user/login", role: 0};

	this.init = function()
	{
		me.i18n.localize();
		me.portal = portal_tool.features(me.i18n.lookup("titlebar", "Browse Sites"));
		me.ui = findElements(["browser_login"]);
		onClick(me.ui.browser_login, function(){me.portal.navigate(null, me.loginTool, false, false);});
	};

	this.start = function()
	{
	};
}

$(function()
{
	try
	{
		browser_tool = new Browser();
		browser_tool.init();
		browser_tool.start();
	}
	catch (e)
	{
		error(e);
	}
});

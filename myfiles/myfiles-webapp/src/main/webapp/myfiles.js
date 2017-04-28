/**********************************************************************************
 * $URL: https://source.etudes.org/svn/serenity/trunk/myfiles/myfiles-webapp/src/main/webapp/myfiles.js $
 * $Id: myfiles.js 12504 2016-01-10 00:30:08Z ggolden $
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

var myfiles_tool = null;

function Myfiles()
{
	var me = this;

	this.portal = null;
	this.i18n = new e3_i18n(myfiles_i10n, "en-us");

	this.init = function()
	{
		me.i18n.localize();
		me.ui = findElements(["myfiles_header", "myfiles_filer", "myfiles_filerHeader"]);
		me.portal = portal_tool.features({pin:[{ui:me.ui.myfiles_header}]});
		// me.ui.filer = new e3_FilerCK(me.ui.myfiles_filer, {select: false, headerUi: me.ui.myfiles_filerHeader, tableHeight: false});
		me.ui.filer = new e3_Filer(me.ui.myfiles_filer, {select: false, headerUi: me.ui.myfiles_filerHeader, tableHeight: false});
	};

	this.start = function()
	{
		me.populate();
	};

	this.populate = function()
	{
		me.ui.filer.enable(function(){}, 4);
	};
}

$(function()
{
	try
	{
		myfiles_tool = new Myfiles();
		myfiles_tool.init();
		myfiles_tool.start();
	}
	catch (e)
	{
		error(e);
	}
});

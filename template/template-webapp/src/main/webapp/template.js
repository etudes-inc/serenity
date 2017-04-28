/**********************************************************************************
 * $URL: https://source.etudes.org/svn/serenity/trunk/template/template-webapp/src/main/webapp/template.js $
 * $Id: template.js 10429 2015-04-06 16:42:17Z ggolden $
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

var template_tool = null;

function Template()
{
	var _me = this;

	this.errorHandler = null;
	this.cdp = new e3_Cdp({onErr:function(code){if (_me.errorHandler != null) _me.errorHandler(code);}});
	this.i18n = new e3_i18n(template_i10n, "en-us");
	this.dialogs = new e3_Dialog();

	this.init = function()
	{
		_me.i18n.localize();
		
		// portal: title, reset and configure
		if (portal_tool != null)
		{
			var portalInfo = portal_tool.features(_me.i18n.lookup("titlebar","Template"), function(){_me.start();}, function(mode){return _me.configure(mode);});
			_me.site = portalInfo.site;
			_me.errorHandler = portalInfo.errorHandler;
		}
	};

	this.start = function()
	{
	};
	
	this.configure = function(mode)
	{
		// enter config
		if (mode === undefined)
		{
		}
		// exit and save
		else if (mode)
		{
		}
		// exit and discard
		else
		{
		}
	};
}

$(function()
{
	try
	{
		template_tool = new Template();
		template_tool.init();
		template_tool.start();
	}
	catch (e)
	{
		error(e);
	}
});

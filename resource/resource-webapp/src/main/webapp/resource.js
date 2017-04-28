/**********************************************************************************
 * $URL: https://source.etudes.org/svn/serenity/trunk/resource/resource-webapp/src/main/webapp/resource.js $
 * $Id: resource.js 11450 2015-08-13 20:03:01Z ggolden $
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

var resource_tool = null;

function Resource()
{
	var me = this;

	this.i18n = new e3_i18n(resource_i10n, "en-us");
	this.ui = null;
	this.portal = null;

	this.init = function()
	{
		me.i18n.localize();
		me.ui = findElements(["resource_header"]);
		me.portal = portal_tool.features({pin:[{ui:me.ui.resource_header}]});		
	};

	this.start = function()
	{
	};
}

$(function()
{
	try
	{
		resource_tool = new Resource();
		resource_tool.init();
		resource_tool.start();
	}
	catch (e)
	{
		error(e);
	}
});

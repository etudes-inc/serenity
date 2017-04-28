/**********************************************************************************
 * $URL: https://source.etudes.org/svn/serenity/trunk/site/site-webapp/src/main/webapp/mysites.js $
 * $Id: mysites.js 12504 2016-01-10 00:30:08Z ggolden $
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

var mysites_tool = null;

function Mysites()
{
	var me = this;

	this.portal = null;
	this.i18n = new e3_i18n(mysites_i10n, "en-us");
	this.ui = null;
	
	this.sites = [];

	this.findSite = function(siteId)
	{
		var found = null;
		$.each(me.sites || [], function(index, site)
		{
			if (site.id == siteId) found = site;
		});

		return found;
	};

	this.init = function()
	{
		me.i18n.localize();

		me.ui = findElements(["mysites_header", "mysites_sites_table", "mysites_sites_headers", "mysites_actions", "mysites_none",
		                      "mysites_sites_actions_setup", "mysites_sites_actions_roster", "mysites_sites_actions_publish", "mysites_sites_actions_unpublish"]);

		me.portal = portal_tool.features({pin:[{ui:me.ui.mysites_header}]});

		me.ui.table = new e3_Table(me.ui.mysites_sites_table);
		me.ui.table.setupSelection("mysites_sites_table_select", function(){me.updateActions();});
		me.ui.table.selectAllHeader(1, me.ui.mysites_sites_headers);

		onClick(me.ui.mysites_sites_actions_setup, function(){me.setup();});
		onClick(me.ui.mysites_sites_actions_roster, function(){me.roster();});
		onClick(me.ui.mysites_sites_actions_publish, function(){me.publish();});
		onClick(me.ui.mysites_sites_actions_unpublish, function(){me.unpublish();});
		setupHoverControls([me.ui.mysites_sites_actions_setup, me.ui.mysites_sites_actions_roster, me.ui.mysites_sites_actions_publish, me.ui.mysites_sites_actions_unpublish]);
	};

	this.start = function()
	{
		me.load();
	};

	this.load = function()
	{
		var params = me.portal.cdp.params();
		me.portal.cdp.request("site_sites", params, function(data)
		{
			me.sites = data.sites;
			// me.portal.refreshSites(me.sites); TODO:

			me.populate();
		});		
	};

	this.populate = function()
	{
		hide(me.ui.mysites_actions);
		me.ui.table.clear();
		var needsActions = false;
		$.each(me.sites || [], function(index, site)
		{
			me.ui.table.row();

			// insert an in-table heading if we are at the start of a new term
			if (((index > 0) && (me.sites[index-1].term.id != site.term.id)) || (index == 0))
			{
				me.ui.table.headerRow(site.term.name);
				me.ui.table.row();
			}

			if (site.role >= Role.instructor) needsActions = true;
			me.ui.table.selectBox((site.role >= Role.instructor) ? site.id : null);

			if (site.accessStatus == AccessStatus.open)
			{
				me.ui.table.dot("green",  me.i18n.lookup("msg_open", "Open"));
			}
			else if (site.accessStatus == AccessStatus.willOpen)
			{
				me.ui.table.dot("yellow",  me.i18n.lookup("msg_willOpen", "Will Open"));
			}
			else
			{
				me.ui.table.dot("red",  me.i18n.lookup("msg_closed", "Closed"));
			}
			
			if ((site.accessStatus == AccessStatus.open) || (site.role >= Role.instructor))
			{
				me.ui.table.hotText(site.title, me.i18n.lookup("msg_viewSite", "View %0", "html", [site.title]), function(){me.portal.navigate(site, null, false, false);}, null,
						{width: "calc(100vw - 100px - 602px)", minWidth: "calc(1200px - 100px - 602px)"});
			}
			else
			{
				me.ui.table.text(site.title, null, {width: "calc(100vw - 100px - 602px)", minWidth: "calc(1200px - 100px - 602px)"});
			}

			me.ui.table.date(site.publishOn);
			me.ui.table.date(site.unpublishOn);
			me.ui.table.date(site.createdOn);
		});
		me.ui.table.done();

		show(me.ui.mysites_none, (me.ui.table.rowCount() == 0));
		show(me.ui.mysites_actions, needsActions);
	};

	this.updateActions = function()
	{
		me.ui.table.updateActions([me.ui.mysites_sites_actions_setup, me.ui.mysites_sites_actions_roster], [me.ui.mysites_sites_actions_publish,me.ui.mysites_sites_actions_unpublish]);		
	};

	this.setup = function()
	{
		var siteIds = me.ui.table.selected();
		if (siteIds.length == 0)
		{
			me.portal.dialogs.openAlert("mysites_select1First");
			return;
		}
		var site = me.findSite(siteIds[0]);		
		if (site != null) me.portal.navigate(site, Tools.sitesetup, true, false);
	};

	this.roster = function()
	{
		var siteIds = me.ui.table.selected();
		if (siteIds.length == 0)
		{
			me.portal.dialogs.openAlert("mysites_select1First");
			return;
		}
		var site = me.findSite(siteIds[0]);
		if (site != null) me.portal.navigate(site, Tools.siteroster, true, false);
	};

	this.publish = function()
	{
		var params = me.portal.cdp.params();
		params.post.ids = me.ui.table.selected();

		if (params.post.ids.length == 0)
		{
			me.portal.dialogs.openAlert("mysites_selectFirst");
			return;
		}

		me.portal.dialogs.openConfirm("mysites_confirmPublish", me.i18n.lookup("Publish", "Publish"), function()
		{
			me.portal.cdp.request("site_publish site_sites", params, function(data)
			{
				me.sites = data.sites;
				// me.portal.refreshSites(me.sites); TODO:

				me.populate();
			});

			return true;
		});
	};
	
	this.unpublish = function()
	{
		var params = me.portal.cdp.params();
		params.post.ids = me.ui.table.selected();

		if (params.post.ids.length == 0)
		{
			me.portal.dialogs.openAlert("mysites_selectFirst");
			return;
		}

		me.portal.dialogs.openConfirm("mysites_confirmUnpublish", me.i18n.lookup("Unpublish", "Unpublish"), function()
		{
			me.portal.cdp.request("site_unpublish site_sites", params, function(data)
			{
				me.sites = data.sites;
				// me.portal.refreshSites(me.sites); TODO:

				me.populate();
			});

			return true;
		});
	};
}

$(function()
{
	try
	{
		mysites_tool = new Mysites();
		mysites_tool.init();
		mysites_tool.start();
	}
	catch (e)
	{
		error(e);
	}
});

/**********************************************************************************
 * $URL: https://source.etudes.org/svn/serenity/trunk/site/site-webapp/src/main/webapp/site.js $
 * $Id: site.js 11740 2015-10-01 18:43:09Z ggolden $
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

var site_tool = null;

function Site()
{
	var me = this;

	this.errorHandler = null;
	this.portalNavigate = null;
	this.cdp = new e3_Cdp({onErr: function(code){if (me.errorHandler != null) me.errorHandler(code);}});
	this.i18n = new e3_i18n(site_i10n, "en-us");
	this.dialogs = new e3_Dialog();
	this.timestamp = new e3_Timestamp(this.cdp);
	
	this.table = null;

	this.byTerm = false;

	this.sites = [];
	this.total = 0;
	this.paging = {pageNum:1, pageSize:50};

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

		if (portal_tool != null)
		{
			var portalInfo = portal_tool.features(me.i18n.lookup("titlebar", "Sites"), null, null);
			me.errorHandler = portalInfo.errorHandler;
			me.cdp.tz = portalInfo.tz;
			me.portalNavigate = portalInfo.navigate;
		}

		me.table = new e3_Table("site_site_table");
		me.table.setupSelection("site_site_table_select", function(){me.updateActions();});
		me.table.selectAllHeader(1);

		onClick("site_site_actions_setup", function(){me.setup();});
		onClick("site_site_actions_roster", function(){me.roster();});
		onClick("site_site_actions_publish", function(){me.publish();});
		onClick("site_site_actions_unpublish", function(){me.unpublish();});
		onClick("site_site_actions_purge", function(){me.purge();});
		onClick("site_site_actions_archive", function(){me.archive();});
		onClick("site_site_actions_clear", function(){me.clear();});
		
		onClick("site_sortby_0", function(){me.sortDate();}, true);
		onClick("site_sortby_1", function(){me.sortTerm();}, true);
		onClick("site_searchGo", function(){me.search();});
		$('input:radio[name=site_sortby][value="' + (me.byTerm ? '1' : '0') + '"]').prop('checked', true);
	};

	this.start = function()
	{
		me.load();
	};

	this.load = function()
	{
		var params = me.cdp.params();
		
		var value = $.trim($("#site_search").val());
		if (value.length > 0) params.post.search = value;
		
		params.post.byTerm = me.byTerm ? "1" : "0";
		params.post.pageNum = me.paging.pageNum;
		params.post.pageSize = me.paging.pageSize;

		me.cdp.request("site_allSites", params, function(data)
		{
			me.sites = data.sites || [];
			me.total = data.total || 0;
			me.setPaging();
			me.populate();
		});
	};

	this.populate = function()
	{
		me.table.clear();
		var count = 0;
		$.each(me.sites || [], function(index, site)
		{
			me.table.row();
			count++;

			// insert an in-table heading if we are at the start of a new term, and in byTerm sorting
			if (me.byTerm && (((index > 0) && (me.sites[index-1].term != site.term)) || (index == 0)))
			{
				// pad so headers are always the same color zebra stripe
				if (!isOdd(count))
				{
					me.table.row();
				}

				me.table.headerRow(site.term);
				me.table.row();
				count = 0;
			}

			me.table.selectBox(site.id);

			me.table.hotText(site.name, me.i18n.lookup("visitSite", "Visit %0", "html", [site.name]), function(){me.visit(site);});

			var statusIcon = null;
			if (site.accessStatus == 0)
			{
				statusIcon = "/ui/icons/publish.png";
			}
			else if (site.accessStatus == 1)
			{
				statusIcon = "/ui/icons/calendar.png";
			}
			else
			{
				statusIcon = "/ui/icons/closed.gif";
			}
			me.table.iconAndLabel(statusIcon, "");
			me.table.text(site.term);
			me.table.text(site.createdOn == null ? "" : me.timestamp.display(site.createdOn));
			if (site.instructor != null)
			{
				if (site.instructorEmail != null)
				{
					me.table.html("<a class='e3_toolUiLink' href='mailto:" + site.instructorEmail + "' title='" + me.i18n.lookup("Email User", "Email User") + "'>" + site.instructor + "</a>");
				}
				else
				{
					me.table.text(site.instructor);
				}
			}
			else
			{
				me.table.text("");
			}
		});

		show("site_site_none", me.table.rowCount() == 0);

		me.table.sort({0:{sorter:false},1:{sorter:false},2:{sorter:false},3:{sorter:false},4:{sorter:false},5:{sorter:false},},[]);
		me.table.done();
	};

	this.updateActions = function()
	{
		me.table.updateActions(me.sites, ["site_site_actions_setup", "site_site_actions_roster"], ["site_site_actions_publish","site_site_actions_unpublish","site_site_actions_purge","site_site_actions_archive"]);		
	};

	this.setup = function()
	{
		var siteIds = me.table.selected();
		me.table.clearSelection();
		if (siteIds.length == 0)
		{
			me.dialogs.openAlert("site_select1First");
			return;
		}
		var site = me.findSite(siteIds[0]);
		if ((me.portalNavigate != null) && (site != null)) me.portalNavigate(site, 113, false, true);
	};

	this.roster = function()
	{
		var siteIds = me.table.selected();
		me.table.clearSelection();
		if (siteIds.length == 0)
		{
			me.dialogs.openAlert("site_select1First");
			return;
		}
		var site = me.findSite(siteIds[0]);
		if ((me.portalNavigate != null) && (site != null)) me.portalNavigate(site, 114, false, true);
	};

	this.publish = function()
	{
		var params = me.cdp.params();
		
		var value = $.trim($("#site_search").val());
		if (value.length > 0) params.post.search = value;
		
		params.post.byTerm = me.byTerm ? "1" : "0";
		params.post.pageNum = me.paging.pageNum;
		params.post.pageSize = me.paging.pageSize;

		params.post.ids = me.table.selected();

		if (params.post.ids.length == 0)
		{
			me.dialogs.openAlert("site_selectFirst");
			return;
		}

		me.dialogs.openConfirm("site_confirmPublish", me.i18n.lookup("Publish", "Publish"), function()
		{
			me.cdp.request("site_publish", params, function(data)
			{
				me.sites = data.sites || [];
				me.total = data.total || 0;
				me.setPaging();
				me.populate();
			});

			return true;
		});
	};
	
	this.unpublish = function()
	{
		var params = me.cdp.params();
		
		var value = $.trim($("#site_search").val());
		if (value.length > 0) params.post.search = value;
		
		params.post.byTerm = me.byTerm ? "1" : "0";
		params.post.pageNum = me.paging.pageNum;
		params.post.pageSize = me.paging.pageSize;

		params.post.ids = me.table.selected();

		if (params.post.ids.length == 0)
		{
			me.dialogs.openAlert("site_selectFirst");
			return;
		}

		me.dialogs.openConfirm("site_confirmUnpublish", me.i18n.lookup("Unpublish", "Unpublish"), function()
		{
			me.cdp.request("site_unpublish", params, function(data)
			{
				me.sites = data.sites || [];
				me.total = data.total || 0;
				me.setPaging();
				me.populate();
			});

			return true;
		});
	};

	this.purge = function()
	{
		var params = me.cdp.params();
		
		var value = $.trim($("#site_search").val());
		if (value.length > 0) params.post.search = value;
		
		params.post.byTerm = me.byTerm ? "1" : "0";
		params.post.pageNum = me.paging.pageNum;
		params.post.pageSize = me.paging.pageSize;
		
		params.post.ids = me.table.selected();

		if (params.post.ids.length == 0)
		{
			me.dialogs.openAlert("site_selectFirst");
			return;
		}

		me.dialogs.openConfirm("site_confirmPurge", me.i18n.lookup("Purge", "Purge"), function()
		{
			me.cdp.request("site_purge", params, function(data)
			{
				me.sites = data.sites || [];
				me.total = data.total || 0;
				me.setPaging();
				me.populate();
			});

			return true;
		});
	};

	this.archive = function()
	{
		var params = me.cdp.params();
		
		var value = $.trim($("#site_search").val());
		if (value.length > 0) params.post.search = value;
		
		params.post.byTerm = me.byTerm ? "1" : "0";
		params.post.pageNum = me.paging.pageNum;
		params.post.pageSize = me.paging.pageSize;
		
		params.post.ids = me.table.selected();

		if (params.post.ids.length == 0)
		{
			me.dialogs.openAlert("site_selectFirst");
			return;
		}

		me.dialogs.openConfirm("site_confirmArchive", me.i18n.lookup("Archive", "Archive"), function()
		{
			me.cdp.request("site_archive", params, function(data)
			{
				me.sites = data.sites || [];
				me.total = data.total || 0;
				me.setPaging();
				me.populate();
			});

			return true;
		});
	};

	this.clear = function()
	{
		var params = me.cdp.params();
		
		var value = $.trim($("#site_search").val());
		if (value.length > 0) params.post.search = value;
		
		params.post.byTerm = me.byTerm ? "1" : "0";
		params.post.pageNum = me.paging.pageNum;
		params.post.pageSize = me.paging.pageSize;
		
		params.post.ids = me.table.selected();

		if (params.post.ids.length == 0)
		{
			me.dialogs.openAlert("site_selectFirst");
			return;
		}

		me.dialogs.openConfirm("site_confirmClear", me.i18n.lookup("Clear", "Clear"), function()
		{
			me.cdp.request("site_clear", params, function(data)
			{
				me.sites = data.sites || [];
				me.total = data.total || 0;
				me.setPaging();
				me.populate();
			});

			return true;
		});
	};

	this.visit = function(site)
	{
		me.portalNavigate(site, null, false, true);
	};
	
	this.sortDate = function()
	{
		me.byTerm = false;
		me.paging.pageNum = 1;
		me.load();
	};

	this.sortTerm = function()
	{
		me.byTerm = true;
		me.paging.pageNum = 1;
		me.load();
	};
	
	this.search = function()
	{
		$("#site_search").val($.trim($("#site_search").val()));
		me.paging.pageNum = 1;
		me.load();
	};
	
	this.setPaging = function()
	{
		var lastOnPage = Math.min(((me.paging.pageNum - 1) * me.paging.pageSize) + me.paging.pageSize, me.total);
		var firstOnPage = Math.min(((me.paging.pageNum - 1) * me.paging.pageSize) + 1, me.total);
		$("#site_pageDisplay").text(me.i18n.lookup("pageDisplay", "Page %0 (Site %1 - %2 of %3)", "html",
			[me.paging.pageNum, firstOnPage, lastOnPage, me.total]));
		
		onClick("site_page_first",function(){me.firstPage();});
		onClick("site_page_prev",function(){me.prevPage();});
		onClick("site_page_next",function(){me.nextPage();});
		onClick("site_page_last",function(){me.lastPage();});
		
		if (me.paging.pageNum > 1)
		{
			$("#site_page_first").removeClass("e3_disabled");
			$("#site_page_prev").removeClass("e3_disabled");
		}
		else
		{
			$("#site_page_first").addClass("e3_disabled");
			$("#site_page_prev").addClass("e3_disabled");
		}
		
		if (me.paging.pageNum < Math.ceil(me.total / me.paging.pageSize))
		{
			$("#site_page_last").removeClass("e3_disabled");
			$("#site_page_next").removeClass("e3_disabled");
		}
		else
		{
			$("#site_page_last").addClass("e3_disabled");
			$("#site_page_next").addClass("e3_disabled");
		}
	};

	this.prevPage = function()
	{
		if (me.paging.pageNum > 1)
		{
			me.paging.pageNum--;
			me.load();
		}
	};
	
	this.nextPage = function()
	{
		if (me.paging.pageNum < Math.ceil(me.total / me.paging.pageSize))
		{
			me.paging.pageNum++;
			me.load();
		}
	};
	
	this.firstPage = function()
	{
		if (me.paging.pageNum != 1)
		{
			me.paging.pageNum = 1;
			me.load();
		}
	};
	
	this.lastPage = function()
	{
		var last = Math.ceil(me.total / me.paging.pageSize);
		if (me.paging.pageNum != last)
		{
			me.paging.pageNum = last;
			me.load();
		}
	};
}

$(function()
{
	try
	{
		site_tool = new Site();
		site_tool.init();
		site_tool.start();
	}
	catch (e)
	{
		error(e);
	}
});

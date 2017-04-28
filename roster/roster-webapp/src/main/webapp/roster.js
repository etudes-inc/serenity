/**********************************************************************************
 * $URL: https://source.etudes.org/svn/serenity/trunk/roster/roster-webapp/src/main/webapp/roster.js $
 * $Id: roster.js 12072 2015-11-13 03:14:18Z ggolden $
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

var roster_tool = null;

function Roster()
{
	var me = this;

	this.i18n = new e3_i18n(roster_i10n, "en-us");
	this.portal = null;
	this.ui = null;
	this.modes = null;
	this.onExit = null;

	this.adhoc = new Roster_adhoc(this);
	this.files = new Roster_files(this);
	this.rosters = new Roster_rosters(this);
	this.siteRoster = new Roster_siteRoster(this);
	this.reports = new Roster_reports(this);

	this.showOnlyActiveTerms = true;
	this.clients = [];
	this.terms = [];
	this.client = null;
	this.term = null;

	this.init = function()
	{
		me.i18n.localize();

		me.ui = findElements(["roster_header", "roster_modebar", "roster_headerbar",
		                      "roster_adhoc", "roster_bar_adhoc",
		                      "roster_files", "roster_bar_files",
		                      "roster_rosters", "roster_bar_rosters", "roster_header_rosters_mapping", "roster_header_rosters_sites", "roster_header_rosters_rosters",
		                      "roster_siteRoster", "roster_bar_siteRoster"
		                     ]);
		me.portal = portal_tool.features({onExit: me.checkExit, pin:[{ui:me.ui.roster_header}]});

		me.ui.modebar = new e3_Modebar(me.ui.roster_modebar);

		me.adhoc = new Roster_adhoc(me);
		me.adhoc.init();
		me.files = new Roster_files(me);
		me.files.init();
		me.rosters = new Roster_rosters(me);
		me.rosters.init();
		me.siteRoster = new Roster_siteRoster(me);
		me.siteRoster.init();
		me.reports = new Roster_reports(me);
		me.reports.init();

		me.modes =
		[
			{name:me.i18n.lookup("Adhoc", "Adhoc"), icon:"script_edit.png", func:function(){me.startAdhoc();}},
			{name:me.i18n.lookup("Files", "Files"), icon:"disk_multiple.png", func:function(){me.startFiles();}},
			{name:me.i18n.lookup("Rosters", "Rosters"), icon:"chart_organisation.png", func:function(){me.startRosters();}},
			{name:me.i18n.lookup("Site Roster", "Site Roster"), icon:"user_female.png", func:function(){me.startSiteRoster();}},
			{name:me.i18n.lookup("Reports", "Reports"), icon:"report.png", func:function(){me.startReports();}}
		];
		me.ui.modebar.set(me.modes, 0);
	};

	this.start = function()
	{
		if (me.terms.length == 0)
		{
			me.loadConfig(me.startAdhoc);
		}
		else
		{
			me.startAdhoc();
		}
	};

	this.startAdhoc = function()
	{
		if ((me.onExit != null) && (!me.onExit(function(){me.startAdhoc();}))) return;
		me.mode([me.ui.roster_headerbar, me.ui.roster_bar_adhoc, me.ui.roster_adhoc]);
		me.ui.modebar.showSelected(0);
		me.adhoc.start();
	};
	
	this.startFiles = function()
	{
		if ((me.onExit != null) && (!me.onExit(function(){me.startFiles();}))) return;
		me.mode([me.ui.roster_headerbar, me.ui.roster_bar_files, me.ui.roster_files]);
		me.ui.modebar.showSelected(1);
		me.files.start();
	};
	
	this.startRosters = function()
	{
		if ((me.onExit != null) && (!me.onExit(function(){me.startRosters();}))) return;
		me.mode([me.ui.roster_headerbar, me.ui.roster_bar_rosters, me.ui.roster_rosters]);
		me.ui.modebar.showSelected(2);
		me.rosters.start();
	};

	this.startSiteRoster = function(siteTitle)
	{
		if ((me.onExit != null) && (!me.onExit(function(){me.startSiteRoster(siteTitle);}))) return;
		me.mode([me.ui.roster_headerbar, me.ui.roster_bar_siteRoster, me.ui.roster_siteRoster]);
		me.ui.modebar.showSelected(3);
		me.siteRoster.start(siteTitle);
	};

	this.startReports = function()
	{
		if ((me.onExit != null) && (!me.onExit(function(){me.startReports();}))) return;
		me.mode([me.ui.roster_headerbar, me.ui.roster_bar_overview, me.ui.roster_header_overview, me.ui.roster_overview]);
		// me.mode("roster_reports");
		me.ui.modebar.showSelected(4);
		me.reports.start();
	};

	this.mode = function(modeUi)
	{
		hide([me.ui.roster_headerbar,
		      me.ui.roster_adhoc, me.ui.roster_bar_adhoc,
		      me.ui.roster_files, me.ui.roster_bar_files,
		      me.ui.roster_rosters, me.ui.roster_bar_rosters, me.ui.roster_header_rosters_mapping, me.ui.roster_header_rosters_sites, me.ui.roster_header_rosters_rosters,
		      me.ui.roster_siteRoster, me.ui.roster_bar_siteRoster
		     ]);
		me.onExit = null;
		me.portal.resetScrolling();
		show(modeUi);
	};

	this.checkExit = function(deferred)
	{
		if (me.onExit == null) return true;
		return me.onExit(deferred);
	};

	this.populateFilters = function(into, extra, action)
	{
		into.empty();

		if (extra != null) extra(into);

		// clients
		var clientOptions = [{value: "-", title: me.i18n.lookup("msg_3dash", "---")}];
		$.each(me.clients, function(index, client)
		{
			clientOptions.push({value: client.id, title: client.name});
		});
		var clientsUi = new e3_SortAction();
		clientsUi.inject(into,
				{onSort: function(dir, val){me.client = ((val == "-") ? null : val); if (action != null) action();}, label: me.i18n.lookup("header_client", "CLIENT"), options: clientOptions, initial: ((me.client == null) ? "-" : me.client)});

		// terms
		var termOptions = [{value: "-", title: me.i18n.lookup("msg_3dash", "---")}];
		$.each(me.terms, function(index, term)
		{
			termOptions.push({value: term.id, title: term.name});
		});
		var termsUi = new e3_SortAction();
		termsUi.inject(into,
				{onSort: function(dir, val){me.term = ((val == "-") ? null : val); if (action != null) action();}, label: me.i18n.lookup("header_term", "TERM"), options: termOptions, initial: ((me.term == null) ? "-" : me.term)});		
	
		// all or only active
		var termViewOptions = [{value: false, title: me.i18n.lookup("msg_allTerms", "All")}, {value: true, title: me.i18n.lookup("msg_activeTerms", "Active")}];
		var termViewUi = new e3_SortAction();
		termViewUi.inject(into,
				{onSort: function(dir, val){me.showOnlyActiveTerms = val; me.populateFilters(into, extra, action);}, label: me.i18n.lookup("header_termView", "SHOW TERMS"), options: termViewOptions, initial: me.showOnlyActiveTerms});		
	};

	this.roleName = function(role)
	{
		switch (role)
		{
			case 1: return me.i18n.lookup("Guest", "Guest");
			case 2: return me.i18n.lookup("Observer", "Observer");
			case 3: return me.i18n.lookup("Student", "Student");
			case 4: return me.i18n.lookup("TA", "TA");
			case 5: return me.i18n.lookup("Instructor", "Instructor");
			case 6: return me.i18n.lookup("Administrator", "Administrator");
			default: return me.i18n.lookup("None", "None");
		}
	};

	this.clientName = function(id)
	{
		var found = "";
		$.each(me.clients, function(index, client)
		{
			if (client.id == id) found = client.name;
		});
		
		return found;
	};

	this.termName = function(id)
	{
		var found = "";
		$.each(me.terms, function(index, term)
		{
			if (term.id == id) found = term.name;
		});
		
		return found;
	};

	this.loadConfig = function(onLoad)
	{
		var params = me.portal.cdp.params();
		params.url.active = me.showOnlyActiveTerms;
		me.portal.cdp.request("roster_config", params, function(data)
		{
			me.clients = data.clients || [];
			me.terms = data.terms || [];
			me.client = null;
			me.term = null;

			if (onLoad != null)
			{
				onLoad();
			}
		});
	};

//	this.populateOperationResults = function(results)
//	{
//		var area = $("#roster_alert_results_body");
//		$(area).empty();
//		var ul = $("<ul />");
//		$(area).append(ul);
//		$.each(results, function(index, result)
//		{
//			var li = $("<li />");
//			$(ul).append(li);
//			$(li).html(me.i18n.lookup(result.status, result.status, "html", [result.name, result.ident]));
//		});
//
//		me.dialogs.openAlert("roster_alert_results");
//	};
}

function Roster_adhoc(main)
{
	var me = this;

	this.ui = null;
	this.template = "#(tab separated) SiteTitle\tLogin\tPassword\tLastName\tFirstName\tEmail\tRole\tE/D\tSection\tIID\n#ETU W15\ts1\tWelcome123\tStudent\tOne\ts1@mac.com\tstudent\tE\t101001\t10000001";

	this.init = function()
	{
		me.ui = findElements(["roster_adhoc_client", "roster_adhoc_term", "roster_adhoc_allTerms", "roster_adhoc_rosterLines",
		                      "roster_adhoc_createInstructors", "roster_adhoc_addToUG", "roster_adhoc_createSitesOnly", "roster_adhoc_process", "roster_adhoc_actions",
		                      ]);
		onClick(me.ui.roster_adhoc_process, me.process);
		setupHoverControls([me.ui.roster_adhoc_process]);
	};

	this.start = function()
	{
		main.onExit = me.checkExit;
		me.populate();
	};

	this.populate = function()
	{
		main.populateFilters(me.ui.roster_adhoc_actions);
		
		me.ui.roster_adhoc_rosterLines.val(me.template);
		me.ui.roster_adhoc_createInstructors.prop("checked", false);
		me.ui.roster_adhoc_addToUG.prop("checked", false);
		me.ui.roster_adhoc_createSitesOnly.prop("checked", false);
	};

	this.process = function()
	{
		var params = main.portal.cdp.params();
		if (main.client != null) params.post.client = main.client;
		if (main.term != null) params.post.term = main.term;
		params.post.lines = $.trim(me.ui.roster_adhoc_rosterLines.val());
		params.post.createInstructors = me.ui.roster_adhoc_createInstructors.is(":checked");
		params.post.addToUG = me.ui.roster_adhoc_addToUG.is(":checked");
		params.post.createSitesOnly = me.ui.roster_adhoc_createSitesOnly.is(":checked");
		main.portal.cdp.request("roster_processLines", params, function(data)
		{
			me.populate();
		});
	};

	this.checkExit = function()
	{		
//		if (me.edit.changed())
//		{
//			main.portal.confirmNavigationWithChanges(function()
//			{
//				me.save(deferred);				
//			}, function()
//			{
//				me.edit.revert();
//				// me.populate();
//				if (deferred !== undefined) deferred();
//			});
//
//			return false;
//		}

		return true;
	};
}

function Roster_files(main)
{
	var me = this;
	this.ui = null;

	this.init = function()
	{
		me.ui = findElements(["roster_files_readyFiles", "roster_files_readyFiles_none", "roster_files_schedule", "roster_files_schedule_none", "roster_files_process"]);
		onClick(me.ui.roster_files_process, me.process);
		setupHoverControls([me.ui.roster_files_process]);
	};

	this.start = function()
	{
		me.load();
	};

	this.load = function()
	{
		var params = main.portal.cdp.params();
		main.portal.cdp.request("roster_schedule", params, function(data)
		{
			me.populateFiles(data.files || []);
			me.populateSchedule(data.schedule || []);
		});
	};

	this.populateFiles = function(files)
	{
		me.ui.roster_files_readyFiles.empty();
		$.each(files, function(index, file)
		{
			var li = $("<li />").text(file.name);
			me.ui.roster_files_readyFiles.append(li);
		});

		show(me.ui.roster_files_readyFiles_none, (files.length == 0));
		if (files.length == 0)
		{
			disableAction(me.ui.roster_files_process);
		}
		else
		{
			enableAction(me.ui.roster_files_process);
		}
	};

	this.populateSchedule = function(schedule)
	{
		me.ui.roster_files_schedule.empty();		
		$.each(schedule, function(index, entry)
		{
			var li = $("<li />").text(entry.time);
			me.ui.roster_files_schedule.append(li);
		});

		show(me.ui.roster_files_schedule_none, (schedule.length == 0))
	};

	this.process = function()
	{
		var params = main.portal.cdp.params();
		main.portal.cdp.request("roster_processFiles", params, function(data)
		{
			// TODO: ??? delay start to give the server a chance to start processing
			me.start();
		});
	};
}

function Roster_rosters(main)
{
	var me = this;
	this.ui = null;
	
	this.sites = [];
	this.rosters = [];
	this.loadedClient = null;
	this.loadedTerm = null;
	this.organization = 0; // 0 - mapping, 1 - sites, 2 - rosters
	this.siteFilter = null;
	this.rosterFilter = null;

	this.init = function()
	{
		me.ui = findElements(["roster_rosters_process", "roster_rosters_add", "roster_rosters_actions",
		                      "roster_rosters_table", "roster_rosters_none", "roster_pickClientTermFirst", "roster_rosters_selectClientTermLoad",
		                      "roster_rosters_siteFilter", "roster_rosters_rosterFilter",
		                      "roster_confirmAddSiteRoster_site", "roster_confirmAddSiteRoster_roster", "roster_confirmAddSiteRoster",
		                      "roster_alert_addSiteRoster_params", "roster_pickLoadFirst"]);
		me.ui.table = new e3_Table(me.ui.roster_rosters_table);

		onClick(me.ui.roster_rosters_add, me.add);
		onClick(me.ui.roster_rosters_process, me.load);
		setupHoverControls([me.ui.roster_rosters_add, me.ui.roster_rosters_process]);
	};

	this.start = function()
	{
		me.populate();
	};

	this.populate = function()
	{
		main.populateFilters(me.ui.roster_rosters_actions, function(into)
		{
			// organize by mapping, sites, sections
			var organizeOptions = [{value: 0, title: main.i18n.lookup("msg_organizeMapping", "Mapping")},
			                       {value: 1, title: main.i18n.lookup("msg_organizeSites", "Sites")},
			                       {value: 2, title: main.i18n.lookup("msg_organizeRosters", "Rosters")}];
			var organizeUi = new e3_SortAction();
			organizeUi.inject(into,
					{onSort: function(dir, val){me.organization = val; me.populateHeaders(); me.populateResults();}, label: main.i18n.lookup("header_organizeBt", "VIEW BY"), options: organizeOptions, initial: me.organization});
		}, function()
		{
			me.adjust();
		});
		me.populateHeaders();
		me.adjust();
		
		me.ui.roster_rosters_siteFilter.val(me.siteFilter);
		onChange(me.ui.roster_rosters_siteFilter, function(target, finalChange)
		{
			var val = trim(me.ui.roster_rosters_siteFilter.val().toUpperCase().replace(/\s+/g,' '));
			if (finalChange)
			{
				if (val != me.siteFilter)
				{
					me.siteFilter = val;
					me.populateResults();
				}
			}
		});

		me.ui.roster_rosters_rosterFilter.val(me.rosterFilter);
		onChange(me.ui.roster_rosters_rosterFilter, function(target, finalChange)
		{
			var val = trim(me.ui.roster_rosters_rosterFilter.val().toUpperCase().replace(/\s+/g,' '));
			if (finalChange)
			{
				if (val != me.rosterFilter)
				{
					me.rosterFilter = val;
					me.populateResults();
				}
			}
		});
	};

	this.adjust = function()
	{
		var clear = ((me.loadedClient != main.client) || (me.loadedTerm != main.term) || (main.client == null) || (main.term == null));
		if (clear)
		{
			me.ui.table.clear();
			hide(me.ui.roster_rosters_none);
			me.sites = [];
			me.rosters = [];
			me.loadedClient = null;
			me.loadedTerm = null;
		}
		show(me.ui.roster_rosters_selectClientTermLoad, clear);

		enableActions(me.ui.roster_rosters_process, ((main.client != null) && (main.term != null)));
		enableActions(me.ui.roster_rosters_add, ((me.loadedClient != null) && (me.loadedTerm != null)));
	};
	
	this.populateHeaders = function()
	{
		show(main.ui.roster_header_rosters_mapping, me.organization == 0);
		show(main.ui.roster_header_rosters_sites, me.organization == 1);
		show(main.ui.roster_header_rosters_rosters, me.organization == 2);
	};

	this.load = function()
	{
		if ((main.client == null) || (main.term == null))
		{
			main.portal.dialogs.openAlert(me.ui.roster_pickClientTermFirst);
			return;
		}

		var params = main.portal.cdp.params();
		params.url.client = main.client;
		params.url.term = main.term;
		main.portal.cdp.request("roster_rosters", params, function(data)
		{
			me.sites = data.sites || [];
			me.rosters = data.rosters || [];
			me.loadedClient = main.client;
			me.loadedTerm = main.term;
			
			hide(me.ui.roster_rosters_selectClientTermLoad);
			enableAction(me.ui.roster_rosters_add);

			me.resetFilters();
			me.populateResults();
		});
	};

	me.resetFilters = function()
	{
		me.siteFilter = null;
		me.rosterFilter = null;
		me.ui.roster_rosters_siteFilter.val(me.siteFilter);
		me.ui.roster_rosters_rosterFilter.val(me.rosterFilter);
	};

	this.add = function()
	{
		if ((me.loadedClient == null) || (me.loadedTerm == null))
		{
			main.portal.dialogs.openAlert(me.ui.roster_pickLoadFirst);
			return;
		}

		me.ui.roster_confirmAddSiteRoster_site.val("");
		me.ui.roster_confirmAddSiteRoster_roster.val("");

//		var sel = me.ui.table.selectedSecondAttr();
//		if (sel.length > 0)
//		{
//			if ((me.organization == 0) || (me.organization == 1))
//			{
//				// pick up one selected site
//				me.ui.roster_confirmAddSiteRoster_site.val(sel[0]);
//			}
//			else if (me.organization == 2)
//			{
//				// pick up one selected roster
//				me.ui.roster_confirmAddSiteRoster_roster.val(sel[0]);
//			}
//		}

		main.portal.dialogs.openConfirm(me.ui.roster_confirmAddSiteRoster, main.i18n.lookup("btn_add", "Add"), me.doAdd);
	};
	
	this.doAdd = function()
	{
		var params = main.portal.cdp.params();
		
		params.post.site = $.trim(me.ui.roster_confirmAddSiteRoster_site.val().toUpperCase().replace(/\s+/g,' '));
		me.ui.roster_confirmAddSiteRoster_site.val(params.post.site);
		params.post.roster = $.trim(me.ui.roster_confirmAddSiteRoster_roster.val().toUpperCase().replace(/\s+/g,' '));
		me.ui.roster_confirmAddSiteRoster_roster.val(params.post.roster);
		
		if ((params.post.site == "") || (params.post.roster == ""))
		{
			main.portal.dialogs.openAlert(me.ui.roster_alert_addSiteRoster_params, function(){main.portal.dialogs.openConfirm(me.ui.roster_confirmAddSiteRoster, main.i18n.lookup("btn_add", "Add"), me.doAdd);});
			return;
		}
		params.url.client = main.client;
		params.url.term = main.term;
		main.portal.cdp.request("roster_addSiteRoster roster_rosters", params, function(data)
		{
			var results = $("#roster_alert_results_body");
			$(results).empty();
			var div = $("<div />");
			$(results).append(div);
			$(div).text((data.results.site == "1") ? main.i18n.lookup("siteAdded", "Site %0 created.", "html", [params.post.site]) : main.i18n.lookup("siteFound", "Site %0 found.", "html", [params.post.site]));
			div = $("<div />");
			$(results).append(div);
			$(div).text((data.results.roster == "1") ? main.i18n.lookup("rosterAdded", "Roster %0 created.", "html", [params.post.roster]) : main.i18n.lookup("rosterFound", "Roster %0 found.", "html", [params.post.roster]));
			div = $("<div />");
			$(results).append(div);
			$(div).text((data.results.mapping == "1") ? main.i18n.lookup("mappingAdded", "Roster added to site.") : main.i18n.lookup("mappingFound", "Site already uses roster."));
			main.portal.dialogs.openAlert("roster_alert_results");
			
			me.sites = data.sites || [];
			me.rosters = data.rosters || [];
			me.populateResults();
		});
	};

	this.remove = function()
	{
		if (me.organization == 0)
		{
			main.portal.dialogs.openConfirm("roster_rosters_confirmRemoveMapping", "Remove", function(){me.removeMapping();});			
		}
		else if (me.organization == 1)
		{
			main.portal.dialogs.openConfirm("roster_rosters_confirmRemoveSites", "Remove", function(){me.removeSites();});			
		}
		else if (me.organization == 2)
		{
			main.portal.dialogs.openConfirm("roster_rosters_confirmRemoveRosters", "Remove", function(){me.removeRosters();});			
		}
	};

	this.removeMapping = function()
	{
		var params = main.portal.cdp.params();
		params.post.ids = me.ui.table.selected();
		params.post.client = main.client;
		params.post.term = main.term;
		main.portal.cdp.request("roster_removeSiteRosterMapping", params, function(data)
		{
			var results = $("#roster_alert_results_body");
			$(results).empty();
			var div = $("<div />");
			$(results).append(div);
			$(div).text(main.i18n.lookup("results_removeMappingHeader","Site - Roster mappings removed:"));
			var ul = $("<ul />");
			$(div).append(ul);
			$.each(data.results || [], function(index, result)
			{
				var li = $("<li />");
				$(ul).append(li);
				$(li).text(main.i18n.lookup("results_removeMappingBody","%0 - %1","html",[result.site, result.roster]));
			});
			main.portal.dialogs.openAlert("roster_alert_results");
			
			me.sites = data.sites || [];
			me.rosters = data.rosters || [];
			me.populateResults();
		});
	};

	this.removeSites = function()
	{
		var params = main.portal.cdp.params();
		params.post.ids = me.ui.table.selected();
		params.post.client = main.client;
		params.post.term = main.term;
		main.portal.cdp.request("roster_removeSiteRosters", params, function(data)
		{
			var results = $("#roster_alert_results_body");
			$(results).empty();
			var div = $("<div />");
			$(results).append(div);
			$(div).text(main.i18n.lookup("results_removeSiteHeader","Sites cleared of rosters:"));
			var ul = $("<ul />");
			$(div).append(ul);
			$.each(data.results || [], function(index, result)
			{
				var li = $("<li />");
				$(ul).append(li);
				$(li).text(result.site);
			});
			main.portal.dialogs.openAlert("roster_alert_results");

			me.sites = data.sites || [];
			me.rosters = data.rosters || [];
			me.populateResults();
		});
	};

	this.removeRosters = function()
	{
		var params = main.portal.cdp.params();
		params.post.ids = me.ui.table.selected();
		params.post.client = main.client;
		params.post.term = main.term;
		main.portal.cdp.request("roster_removeRosterSites", params, function(data)
		{
			var results = $("#roster_alert_results_body");
			$(results).empty();
			var div = $("<div />");
			$(results).append(div);
			$(div).text(main.i18n.lookup("results_removeRosterHeader","Rosters removed from all sites:"));
			var ul = $("<ul />");
			$(div).append(ul);
			$.each(data.results || [], function(index, result)
			{
				var li = $("<li />");
				$(ul).append(li);
				$(li).text(result.roster);
			});
			main.portal.dialogs.openAlert("roster_alert_results");

			me.sites = data.sites || [];
			me.rosters = data.rosters || [];
			me.populateResults();
		});
	};

	this.populateResults = function()
	{
		if ((me.loadedClient == null) || (me.loadedTerm == null)) return;

		var rv = {count:0, tag:""};

		if (me.organization == 0)
		{
			rv = me.populateResultsMapping();
		}
		else if (me.organization == 1)
		{
			rv = me.populateResultsSites();
		}
		else if (me.organization == 2)
		{
			rv = me.populateResultsRosters();
		}

//		me.items = [];
//		if (rv.count > 0)
//		{
//			me.items.push({});
//		}
//		me.ui.table.done();

//		$("#roster_rosters_resultsLegend_rosters").text(main.i18n.lookup("report_rosters", "%0 rosters found for %1 in %2", "html", [me.rosters.length, main.clientName(main.client), main.termName(main.term)]));
//
//		var filters = "";
//		if (me.siteFilter != null)
//		{
//			filters += main.i18n.lookup("siteEquals", "Site = %0 ", "html", [me.siteFilter]);
//		}
//		if (me.rosterFilter != null)
//		{
//			filters += main.i18n.lookup("rosterEquals", "Roster = %0 ", "html", [me.rosterFilter]);
//		}
//
//		if ((me.siteFilter != null) || (me.rosterFilter != null))
//		{
//			$("#roster_rosters_resultsLegend_filtered").text(main.i18n.lookup("report_filtered", "%0 results displayed, organized by %1, filters: %2", "html", [rv.count.toString(), rv.tag, filters]));
//		}
//		else
//		{
//			$("#roster_rosters_resultsLegend_filtered").text(main.i18n.lookup("report_notFiltered", "%0 results displayed, organized by %1", "html", [rv.count.toString(), rv.tag]));			
//		}		
	};

	this.populateResultsMapping = function()
	{
		me.ui.table.clear();
		$.each(me.sites, function(index, site)
		{
			if ((me.siteFilter != null) && (site.name.toUpperCase().indexOf(me.siteFilter)) == -1) return;

			var anyRosters = false;
			$.each(site.rosters || [], function(index, r)
			{
				anyRosters = true;
				var roster = me.findRoster(r.id);

				if ((me.rosterFilter != null) && (roster.name.toUpperCase().indexOf(me.rosterFilter)) == -1) return;

				me.ui.table.row();
				me.ui.table.selectBox(site.id + "@" + roster.name, site.name);
				me.ui.table.text(site.name, null, {width: 300});
				me.ui.table.text(roster.name, null, {width: "calc(100vw - 100px - 428px)", minWidth: "calc(1200px - 100px - 428px)"});
				me.ui.table.contextMenu(
				[
					{title: main.i18n.lookup("cm_viewRoster", "View Roster"), action:function(){main.startSiteRoster(site.name);}},
					{title: main.i18n.lookup("cm_visit", "Visit Site"), action:function(){me.visit(site);}},
					{title: main.i18n.lookup("cm_removeRoster", "Remove Roster"), action:function(){me.visit(site);}} // TODO: remove section from site
		        ]);
			});

			if ((!anyRosters) && (me.rosterFilter == null))
			{
				me.ui.table.row();
				me.ui.table.text("", "icon");
				me.ui.table.text(site.name, null, {width: 300});
				me.ui.table.text("-", null, {width: "calc(100vw - 100px - 428px)", minWidth: "calc(1200px - 100px - 428px)"});
				me.ui.table.contextMenu(
				[
					{title: main.i18n.lookup("cm_view", "View Roster"), action:function(){main.startSiteRoster(site.name);}},
					{title: main.i18n.lookup("cm_visit", "Visit Site"), action:function(){me.visit(site);}}
		        ]);
			}			
		});
		me.ui.table.done();

		show(me.ui.roster_rosters_none, (me.ui.table.rowCount() == 0));

		return {count: me.ui.table.rowCount(), tag: main.i18n.lookup("mapping", "mapping")};
	};

	this.populateResultsSites = function()
	{
		me.ui.table.clear();
		$.each(me.sites, function(index, site)
		{
			if ((me.siteFilter != null) && (site.name.toUpperCase().indexOf(me.siteFilter)) == -1) return;

			var rosters = "";
			var rostersFilteredOut = (me.rosterFilter != null);
			$.each(site.rosters || [], function(index, r)
			{
				var roster = me.findRoster(r.id);
				if ((me.rosterFilter != null) && (roster.name.toUpperCase().indexOf(me.rosterFilter)) != -1) rostersFilteredOut = false;

				if (index != 0) rosters = rosters + " + ";
				rosters = rosters + roster.name;
			});

			if (rostersFilteredOut) return;

			me.ui.table.row();

			if (rosters == "")
			{
				me.ui.table.text("", "icon");
			}
			else
			{
				me.ui.table.selectBox(site.id, site.name);
			}

			me.ui.table.text(site.name, null, {width: 300});
			me.ui.table.text(rosters, null, {width: "calc(100vw - 100px - 428px)", minWidth: "calc(1200px - 100px - 428px)"});
			me.ui.table.contextMenu(
			[
				{title: main.i18n.lookup("cm_view", "View Roster"), action:function(){main.startSiteRoster(site.name);}},
				{title: main.i18n.lookup("cm_visit", "Visit Site"), action:function(){me.visit(site);}},
				{title: main.i18n.lookup("cm_removeRosters", "Remove Rosters"), action:function(){me.visit(site);}} // todo: remove all sections from site
	        ]);
		});
		me.ui.table.done();

		show(me.ui.roster_rosters_none, (me.ui.table.rowCount() == 0));

		return {count: me.ui.table.rowCount(), tag: main.i18n.lookup("site", "site")};
	};

	this.populateResultsRosters = function()
	{
		me.ui.table.clear();
		$.each(me.rosters, function(index, roster)
		{
			if ((me.rosterFilter != null) && (roster.name.toUpperCase().indexOf(me.rosterFilter)) == -1) return;

			var sites = "";
			var sitesFilteredOut = (me.siteFilter != null);
			$.each(roster.sites || [], function(index, s)
			{
				var site = me.findSite(s.id);
				if ((me.siteFilter != null) && (site.name.toUpperCase().indexOf(me.siteFilter)) == -1) return;
				sitesFilteredOut = false;

				if (index != 0) sites = sites + " + ";
				sites = sites + site.name;
			});

			if (sitesFilteredOut) return;

			me.ui.table.row();
			me.ui.table.selectBox(roster.name, roster.name);
			me.ui.table.text(roster.name, null, {width: 120});
			me.ui.table.text(sites, null, {width: "calc(100vw - 100px - 248px)", minWidth: "calc(1200px - 100px - 248px)"});
			me.ui.table.contextMenu(
			[
				{title: main.i18n.lookup("cm_removeRosterAll", "Remove Roster"), action:function(){}} // TODO: remove roster from all sites
	        ]);
		});
		me.ui.table.done();

		show(me.ui.roster_rosters_none, (me.ui.table.rowCount() == 0));

		return {count: me.ui.table.rowCount(), tag: main.i18n.lookup("roster", "roster")};
	};
	
	this.findSite = function(id)
	{
		var found = null;
		$.each(me.sites || [], function(index, site)
		{
			if (site.id == id) found = site;
		});

		return found;
	};

	this.findRoster = function(id)
	{
		var found = null;
		$.each(me.rosters || [], function(index, roster)
		{
			if (roster.id == id) found = roster;
		});

		return found;
	};

	this.visit = function(site)
	{
		main.portal.navigate(site, null, null, true);
	};
}

function Roster_siteRoster(main)
{
	var me = this;
	this.ui = null;

	this.siteTitle = null;
	this.site = null;
	
	this.init = function()
	{
		me.ui = findElements(["roster_siteRoster_process", "roster_siteRoster_visit", "roster_siteRoster_actions", "roster_siteRoster_site", "roster_siteRoster_table", 
		                      "roster_loadSiteFirst", "roster_enterTitleFirst", "roster_siteRoster_noSite"]);

		onClick(me.ui.roster_siteRoster_process, me.load);
		onClick(me.ui.roster_siteRoster_visit, me.visit);
		setupHoverControls([me.ui.roster_siteRoster_process, me.ui.roster_siteRoster_visit]);

		me.ui.table = new e3_Table(me.ui.roster_siteRoster_table);
	};

	this.start = function(title)
	{
		if ((title == null) && (me.site != null))
		{
			// resume where we left off
		}
		else
		{
			// the optional site name
			me.siteTitle = trim(title);
	
			disableAction(me.ui.roster_siteRoster_visit);
			disableAction(me.ui.roster_siteRoster_process);
			show(me.ui.roster_siteRoster_noSite);
			me.ui.table.clear();
	
			// load if we have a name
			if (me.siteTitle != null)
			{
				enableAction(me.ui.roster_siteRoster_process);
				me.load();
			}
			else
			{
				me.populate();
			}
		}
//		// if we have a site name to load
//		if (name != null)
//		{
//			me.siteTitle = name;
//			me.site = null;
//		}
//
//		
//		$("#roster_siteRoster_rosters").empty();
//		$("#roster_siteRoster_site").val("");
//
//		// if we have a site, populate it
//		if (me.site != null)
//		{
//			$("#roster_siteRoster_site").val(me.siteTitle);
//			me.populateSiteRosters();
//		}
//
//		// if we have a title, load it
//		else if (me.siteTitle != null)
//		{
//			$("#roster_siteRoster_site").val(me.siteTitle);
//			me.load();
//		}
	};

	this.load = function()
	{
//		$("#roster_siteRoster_rosters").empty();
//		
//		me.siteTitle = $.trim($("#roster_siteRoster_site").val().toUpperCase().replace(/\s+/g,' '));
//		$("#roster_siteRoster_site").val(me.siteTitle);

		if (me.siteTitle == null)
		{
			main.portal.dialogs.openAlert(me.ui.roster_enterTitleFirst);
			return;
		}

		var params = main.portal.cdp.params();
		params.post.title = me.siteTitle;
		main.portal.cdp.request("roster_siteRoster", params, function(data)
		{
			if (data.site != null)
			{
				me.site = data.site;
				me.populate();
				me.populateSiteRosters();
				enableAction(me.ui.roster_siteRoster_visit);
			}
		});
	};

	this.populate = function()
	{
		me.ui.roster_siteRoster_site.val(me.siteTitle);
		onChange(me.ui.roster_siteRoster_site, function(target, finalChange)
		{
			var val = trim(me.ui.roster_siteRoster_site.val().toUpperCase().replace(/\s+/g,' '));
			if (finalChange)
			{
				if (val != me.siteTitle)
				{
					me.siteTitle = val;
					me.ui.table.clear();
					show(me.ui.roster_siteRoster_noSite);
					me.site = null;
					disableAction(me.ui.roster_siteRoster_visit);
				}
			}
			if (val == null)
			{
				disableAction(me.ui.roster_siteRoster_process);
			}
			else
			{
				enableAction(me.ui.roster_siteRoster_process);
			}
		});
	};

	this.populateSiteRosters = function()
	{
		hide(me.ui.roster_siteRoster_noSite);
		me.ui.table.clear();
		
		// site.members
		me.ui.table.row();
		me.ui.table.headerRow(main.i18n.lookup("msg_siteId", "Site: %0 (%1) %2, %3", "html", [me.site.title, me.site.id, me.site.client, me.site.term]));
		me.populateRoster(true, me.site.members);

		// site rosters
		$("#roster_siteRoster_rosters").empty();
		$.each((me.site || {}).rosters, function(index, roster)
		{
//			var div = $("<div />");
//			$("#roster_siteRoster_rosters").append(div);
//			$(div).addClass("e3_section");
//			$(div).append($("<h1 />").html(main.i18n.lookup("Roster", "Roster")));
//			$(div).append($("<div />",{"class": "e3_tableInfo"}).append($("<span />",{"id": "roster_siteRoster_siteRosterLegend" + roster.id.toString()})));
//			var table = $("<table />",{id:"roster_siteRoster_resultsTable" + roster.id.toString(), "class": "e3_table"});
//			var thead = $("<thead />");
//			var tr = $("<tr />");
//			$(tr).append($("<th />", {"class":"tight"}));
//			$(tr).append($("<th />", {"class":"tight"}));
//			$(tr).append($("<th />").html(main.i18n.lookup("Role", "Role")));
//			$(tr).append($("<th />").html(main.i18n.lookup("Name", "Name")));
//			$(tr).append($("<th />").html(main.i18n.lookup("EID", "EID")));
//			$(tr).append($("<th />").html(main.i18n.lookup("IID", "IID")));
//			$(thead).append(tr);
//			$(table).append(thead);
//			$(table).append($("<tbody />"));
//			$(div).append(table);
//
//			var table = new e3_Table("roster_siteRoster_resultsTable" + roster.id.toString());
//
			me.ui.table.row();
			me.ui.table.headerRow(main.i18n.lookup("msg_rosterId", "Roster, %0 (%1) %2, %3", "html", [roster.title, roster.id, roster.client, roster.term]));
			me.populateRoster(false, roster.members);
		});
	};

	this.populateRoster = function(full, roster)
	{
//		table.clear();
//		$("#" + header).empty();
//
		// header 		$("#" + header).text(main.i18n.lookup(template, "", "html", [name, id, client, term]));

//		me.ui.table.row();
//		me.ui.table.headerRow(main.i18n.lookup(template, "", "html", [name, id, client, term]));

		$.each(roster, function(index, member)
		{
//			var td = null;

			me.ui.table.row();
//			if (member.blocked == "1")
//			{
//				$(row).css("background-color","#FFBAD2");
//			}

			// checkbox only for members from the "master" roster (full mode only)
			if (full && (member.master == "1"))
			{
				me.ui.table.selectBox(member.userId);
			}
			else
			{
				me.ui.table.text("", "icon");
			}

			// dot to show status (active, inactive, blocked)
			me.ui.table.dot(Dots.green, "todo");
//			if (member.active)
//			{
//				table.icon("/support/icons/add.png", main.i18n.lookup("Active", "Active"));
//			}
//			else
//			{
//				table.icon("/support/icons/remove.png", main.i18n.lookup("Inactive", "Inactive"));
//			}
			me.ui.table.text(main.roleName(member.role), null, {width: 100});			
			me.ui.table.text(member.nameSort, null, {width: 300});
			me.ui.table.text(member.eid, null, {width: 100});
			me.ui.table.text(((member.iid == null) ? "-" : member.iid), null, {width: 100});

			if (full)
			{
				if (member.adhoc == "1")
				{
					me.ui.table.html(main.i18n.lookup("adhocRoster", "<i>adhoc</i>"));
				}
				else if (member.master == "1")
				{
					me.ui.table.html(main.i18n.lookup("masterRoster", "<i>master</i>"));
				}
				else
				{
					me.ui.table.text(member.rosterName);
				}
			}
		});

//		if (roster.length == 0)
//		{
//			table.row();
//			table.text("");
//			table.text("");
//			table.text("");
//			table.text("");
//			table.text("");
//			table.text("");
//			if (full) table.text("");
//		}
//
//		if (full)
//		{
//			table.sort({0:{sorter:false},1:{sorter:false},2:{sorter:"text"},3:{sorter:"text"},4:{sorter:"text"},5:{sorter:"text"},6:{sorter:"text"}},[[3,0]]);
//		}
//		else
//		{
//			table.sort({0:{sorter:false},1:{sorter:false},2:{sorter:"text"},3:{sorter:"text"},4:{sorter:"text"},5:{sorter:"text"}},[[3,0]]);
//		}
//		table.done();
		
//		$("#" + header).text(main.i18n.lookup(template, "", "html", [name, id, client, term]));
	};

	this.add = function()
	{
		var params = main.portal.cdp.params();
		params.url.site = me.site.id;

		$("#roster_siteRoster_confirmAdd_identifiers").val("");
		$("input:radio[name=roster_siteRoster_confirmAdd_role][value=3]").prop('checked', true);
		main.portal.dialogs.openConfirm("roster_siteRoster_confirmAdd",  main.i18n.lookup("Add", "Add"), function()
		{
			params.post.role = $("input:radio[name=roster_siteRoster_confirmAdd_role]:checked").val();
			params.post.users = $.trim($("#roster_siteRoster_confirmAdd_identifiers").val());
			main.portal.cdp.request("roster_addMasterMembers", params, function(data)
			{
				me.site = data.site;
				me.populateSiteRosters();
				
				main.populateOperationResults(data.results || []);
			});

			return true;
		});
	};
	
	this.remove = function()
	{
		var params = main.portal.cdp.params();
		params.url.site = me.site.id;
		params.post.ids = me.rosterTable.selected();
		main.portal.dialogs.openConfirm("roster_siteRoster_confirmRemove", main.i18n.lookup("Remove", "Remove"), function()
		{
			main.portal.cdp.request("roster_removeMasterMembers", params, function(data)
			{
				me.site = data.site;
				me.populateSiteRosters();
				
				main.populateOperationResults(data.results || []);
			});
		});
	};
	
	this.visit = function()
	{
		if (me.site == null)
		{
			main.portal.dialogs.openAlert(me.ui.roster_loadSiteFirst);
			return;
		}

		main.portal.navigate(me.site, null, null, true);
	};
}

function Roster_reports(tool)
{
	var me = this;
	this.tool = tool;
	this.resultsTable = new e3_Table("roster_reports_resultsTable");

	this.init = function()
	{
		onClick("roster_reports_report", function(){me.report();});
		onClick("roster_reports_resultsTableActions_print", function(){me.print();});
		onClick("roster_reports_resultsTableActions_export", function(){me.doExport();});
		onChange("roster_reports_allTerms", function(){main.showOnlyActiveTerms = $("#roster_reports_allTerms").is(":checked") ? false : true; main.loadConfig();});
	};

	this.start = function()
	{
	};

	this.reset = function()
	{	
		me.start();
	};

	this.report = function()
	{
		main.client = $.trim($("#roster_reports_client").val());
		main.term = $.trim($("#roster_reports_term").val());
		main.setCurrentClientTerm();

		var params = main.portal.cdp.params();
		params.url.report = $('input:radio[name=roster_reports_mode]:checked').val();
		params.url.client = main.client;
		params.url.term = main.term;
		main.portal.cdp.request("roster_getReport", params, function(data)
		{
			me.populateReport(data.report || []);
		});
	};

	this.populateReport = function(report)
	{
		me.ui.table.clear();
		// TODO:  ...

		if (report.length == 0)
		{
			me.ui.table.row();
			me.ui.table.text("");
			me.ui.table.text("");
		}

		me.ui.table.done();
	};

	this.print = function()
	{
		
	};
	
	this.doExport = function()
	{
		
	};

	this.populateClientsTerms = function()
	{
		var clientsSelect = $("#roster_reports_client");
		$(clientsSelect).empty();
		$.each(main.clients, function(index, client)
		{
			$(clientsSelect).append($("<option />", {value: client.id, text: client.name}));
		});

		var termsSelect = $("#roster_reports_term");
		$(termsSelect).empty();
		$.each(main.terms, function(index, term)
		{
			$(termsSelect).append($("<option />", {value: term.id, text: term.name}));
		});
	};

	this.setCurrentClientTerm = function()
	{
		$("#roster_reports_client option[value=" + main.client + "]").prop("selected", true);
		$("#roster_reports_term option[value=" + main.term + "]").prop("selected", true);
		$("#roster_reports_allTerms").prop("checked", !main.showOnlyActiveTerms);
	};
}

$(function()
{
	try
	{
		roster_tool = new Roster();
		roster_tool.init();
		roster_tool.start();
	}
	catch (e)
	{
		error(e);
	}
});

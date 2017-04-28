/**********************************************************************************
 * $URL: https://source.etudes.org/svn/serenity/trunk/monitor/monitor-webapp/src/main/webapp/monitor.js $
 * $Id: monitor.js 12553 2016-01-14 20:03:28Z ggolden $
 ***********************************************************************************
 *
 * Copyright (c) 2015, 2016 Etudes, Inc.
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

var monitor_tool = null;

function MonitorView(main)
{
	var me = this;
	
	this.ui = null;
	this.timer == null;
	this.interval = 30 * 1000; // 30 second refresh

	this.init = function()
	{
		me.ui = findElements(["monitor_view_table", "monitor_view_none"]);		
		me.ui.table = new e3_Table(me.ui.monitor_view_table);
		new e3_SortAction().inject(main.ui.monitor_bar_view,
				{onSort: me.onSort, options:[{value:"S", title:main.i18n.lookup("sort_status", "STATUS")},{value:"T", title:main.i18n.lookup("sort_title", "TITLE")},
				                             {value:"Y", title:main.i18n.lookup("sort_type", "TYPE")},{value:"D", title:main.i18n.lookup("sort_due", "DUE")}]});
		
		onClick(main.ui.monitor_view_edit, main.startOptions);
	};

	this.start = function()
	{
		main.onExit = me.checkExit;

		me.load();
		
		// setup refresh
		me.timer = setInterval(function(){try{me.load();}catch (e){error(e);}}, me.interval);
	};

	this.checkExit = function(deferred)
	{
		me.stop();

		return true;
	};

	this.stop = function()
	{
		if (me.timer != null)
		{
			clearInterval(me.timer);
			me.timer = null;
		}
	};

	this.load = function(onLoad)
	{
		var params = main.portal.cdp.params();
		params.url.site = main.portal.site.id;
		main.portal.cdp.request("monitor_status monitor_options", params, function(data)
		{
			main.samples = data.samples || [];
			main.options = data.options || {};

			me.populate();
		});
	};

	this.tableCheck = function(value, table)
	{
		if (value == null)
		{
			me.ui.table.text("-", "dot");
		}
		else if (value == true)
		{
			me.ui.table.dot(Dots.check, main.i18n.lookup("msg_running", "running"));
		}
		else
		{
			me.ui.table.dot(Dots.redAlert, main.i18n.lookup("msg_problem", "problem"));
		}
	};

	this.defaultDash = function(value)
	{
		if (value == null) return "-";
		return value;
	};
	
	this.populate = function()
	{
		me.ui.table.clear();
		$.each(main.samples, function(index, sample)
		{
			me.ui.table.row();

			if (sample.alerts.length == 0)
			{
				me.ui.table.dot(Dots.green, main.i18n.lookup("msg_clear", "All Clear"));
			}
			else
			{
				me.ui.table.dot(Dots.red, main.i18n.lookup("msg_alerts", "Alerts Active"));
			}

			me.ui.table.text(sample.source, null, {width:"calc(100vw - 100px - 976px)", minWidth:"calc(1200px - 100px - 976px)"});
			me.ui.table.date(sample.ts, "-", "date2l",  ((sample.alerts.indexOf(main.alerts.absent) != -1) ? {color:"#E00000"} : null));
			
			me.ui.table.text(me.defaultDash(sample.load), null, ((sample.alerts.indexOf(main.alerts.load) != -1) ? {color:"#E00000", width:52} : {width:52}));

			if ((sample.open != null) && (sample.apache != null) && (sample.mysql != null))
			{
				me.ui.table.text(main.i18n.lookup("msg_files", "%0 / %1 / %2", "html", [sample.open, sample.apache, sample.mysql]), "e3_text special light",
						(((sample.alerts.indexOf(main.alerts.apacheConnections) != -1) ) || (sample.alerts.indexOf(main.alerts.mysqlConnections) != -1) || (sample.alerts.indexOf(main.alerts.openFiles) != -1)
								? {color:"#E00000", fontSize:11, width:80} : {fontSize:11, width:80}));
			}
			else
			{
				me.ui.table.text("-", null, {width:80});
			}

			// if appserver check does not apply, dash and skip a col
			if ((sample.appserverRv == null) || (sample.appserverMatch == null) || (sample.appserverTime == null))
			{
				me.ui.table.text("-", "dot");
				me.ui.table.text("", null, {width:52});
			}

			// if successful, show check followed by time
			else if ((sample.appserverRv == 0) && (sample.appserverMatch))
			{
				me.ui.table.dot(Dots.check, main.i18n.lookup("msg_ok", "ok"));
				me.ui.table.text(me.defaultDash(sample.appserverTime), null, ((sample.alerts.indexOf(main.alerts.appserverSlow) != -1) ? {color:"#E00000", width:52} : {width:52}));
			}
			// if not, show alert followed by curl error text
			else
			{
				me.ui.table.dot(Dots.alert, main.i18n.lookup("msg_failed", "failed"));
				var msg = main.i18n.lookup("msg_curl_" + sample.appserverRv, "");
				if (msg == "") msg = main.i18n.lookup("msg_curl_other", "ERROR");
				me.ui.table.text(msg, "e3_text special light", {fontSize:11, width:52, color:"#E00000"});
			}

			me.ui.table.text(me.defaultDash(sample.fsPctUsed), null, ((sample.alerts.indexOf(main.alerts.diskFilling) != -1) ? {color:"#E00000", width:52} : {width:52}));

			me.tableCheck(sample.wsc, me.ui.table);
			me.tableCheck(sample.wiris, me.ui.table);
			me.tableCheck(sample.jira, me.ui.table);
			me.tableCheck(sample.svn, me.ui.table);
			me.tableCheck(sample.nfs, me.ui.table);
			me.tableCheck(sample.slave, me.ui.table);

			if ((sample.queries != null) && (sample.active != null))
			{
				me.ui.table.text(main.i18n.lookup("msg_queries", "%0 / %1", "html", [sample.queries, sample.active]), "e3_text special light",
						(((sample.alerts.indexOf(main.alerts.queries) != -1) || (sample.alerts.indexOf(main.alerts.activeQueries) != -1)) ? {color:"#E00000", fontSize:11, width:52} : {fontSize:11, width:52}));
			}
			else
			{
				me.ui.table.text("-", null, {width:52})
			}

			me.ui.table.date(sample.backup, "-", "date2l", ((sample.alerts.indexOf(main.alerts.backup) != -1) ? {color:"#E00000"} : null));
			me.ui.table.date(sample.offsite, "-", "date2l", ((sample.alerts.indexOf(main.alerts.offsite) != -1) ? {color:"#E00000"} : null));
		});
		me.ui.table.done();
		show(me.ui.monitor_view_none, (me.ui.table.rowCount() == 0));
	};	
	
	this.onSort = function(direction, option)
	{
		console.log("sort", direction, option);
	};
};

function Monitor()
{
	var me = this;

	this.portal = null;
	this.i18n = new e3_i18n(monitor_i10n, "en-us");

	this.viewMode = null;
	this.optionsMode = null;

	this.ui = null;
	this.onExit = null;

	this.samples = [];
	this.options = {};

	this.alerts = {absent:1, apacheConnections:2,  appserver:3, appserverMatch:4, appserverSlow:5, backup:6, diskFilling:7, load:8,
					mysqlConnections:9, nfs:10, offsite:11, openFiles:12,  queries:13, slave:14, wiris:15, wsc:16, activeQueries: 17};

	this.init = function()
	{
		me.i18n.localize();
		
		me.ui = findElements(["monitor_header", "monitor_modebar", "monitor_headerbar", "monitor_itemnav",
		                      "monitor_view", "monitor_bar_view", "monitor_header_view", "monitor_view_edit",
		                      "monitor_options"]);
		me.portal = portal_tool.features({onExit:function(deferred){return me.checkExit(deferred);}, pin:[{ui:me.ui.monitor_header}]});

		me.ui.modebar = new e3_Modebar(me.ui.monitor_modebar);
		me.modes =
		[
			{name:me.i18n.lookup("mode_view", "View"), func:me.startView},
			{name:me.i18n.lookup("mode_options", "Options"), func:me.startOptions}
		];
		me.ui.modebar.set(me.modes, 0);

		me.viewMode = new MonitorView(me);
		me.viewMode.init();
		me.optionsMode = new MonitorOptions(me);
		me.optionsMode.init();
	};

	this.start = function()
	{
		me.startView();
	};

	this.startView = function()
	{
		if (!me.checkExit(function(){me.startView();})) return;
		me.mode([/*me.ui.monitor_headerbar, me.ui.monitor_bar_view, */me.ui.monitor_header_view, me.ui.monitor_view, me.ui.monitor_view_edit]);
		me.ui.modebar.showSelected(0);
		me.viewMode.start();
	};

	this.startOptions = function()
	{
		if (!me.checkExit(function(){me.startOptions();})) return;
		me.mode([me.ui.monitor_modebar, me.ui.monitor_options]);
		me.ui.modebar.showSelected(1);
		me.optionsMode.start();
	};

	this.mode = function(elements)
	{
		hide([me.ui.monitor_modebar, me.ui.monitor_headerbar, me.ui.monitor_itemnav,
		      me.ui.monitor_view, me.ui.monitor_bar_view, me.ui.monitor_header_view, me.ui.monitor_view_edit,
		      me.ui.monitor_options]);
		me.onExit = null;
		me.portal.resetScrolling();
		show(elements);
	};

	this.checkExit = function(deferred)
	{
		if (me.onExit == null) return true;
		return me.onExit(deferred);
	};	
}

function MonitorOptions(main)
{
	var me = this;
	this.ui = null;
	this.edit = null;

	this.init = function()
	{
		me.ui = findElements(["monitor_appserverResponse", "monitor_diskUsedPct", "monitor_loadAvg",
		                      "monitor_openApache", "monitor_openFiles", "monitor_openMysql",
		                      "monitor_queriesTotal", "monitor_queriesActive", "monitor_sinceDbBackup",
		                      "monitor_sinceFsBackup", "monitor_sinceOffsiteBackup", "monitor_sinceReport"]);
	};

	this.start = function()
	{
		main.onExit = me.checkExit;
		me.makeEdit();
		me.populate();
	};

	this.makeEdit = function()
	{
		me.edit = new e3_Edit(main.options, [], function(changed)
		{
			main.ui.modebar.enableSaveDiscard(changed ? me.saveCancel : null);
		});
		me.edit.setFilters({"appserverResponse": me.edit.numberFilter, "diskUsedPct": me.edit.numberFilter, "loadAvg": me.edit.numberFilter, 
							"openApache": me.edit.numberFilter, "openFiles": me.edit.numberFilter, "openMysql": me.edit.numberFilter, 
							"queriesTotal": me.edit.numberFilter, "queriesActive": me.edit.numberFilter, "sinceDbBackup": me.edit.numberFilter, 
							"sinceFsBackup": me.edit.numberFilter, "sinceOffsiteBackup": me.edit.numberFilter, "sinceReport": me.edit.numberFilter});

		main.ui.modebar.enableSaveDiscard(null);
	};

	this.populate = function()
	{
		me.edit.setupFilteredEdit(me.ui.monitor_appserverResponse, me.edit, "appserverResponse");
		me.edit.setupFilteredEdit(me.ui.monitor_diskUsedPct, me.edit, "diskUsedPct");
		me.edit.setupFilteredEdit(me.ui.monitor_loadAvg, me.edit, "loadAvg");
		me.edit.setupFilteredEdit(me.ui.monitor_openApache, me.edit, "openApache");
		me.edit.setupFilteredEdit(me.ui.monitor_openFiles, me.edit, "openFiles");
		me.edit.setupFilteredEdit(me.ui.monitor_openMysql, me.edit, "openMysql");
		me.edit.setupFilteredEdit(me.ui.monitor_queriesTotal, me.edit, "queriesTotal");
		me.edit.setupFilteredEdit(me.ui.monitor_queriesActive, me.edit, "queriesActive");
		me.edit.setupFilteredEdit(me.ui.monitor_sinceDbBackup, me.edit, "sinceDbBackup");
		me.edit.setupFilteredEdit(me.ui.monitor_sinceFsBackup, me.edit, "sinceFsBackup");
		me.edit.setupFilteredEdit(me.ui.monitor_sinceOffsiteBackup, me.edit, "sinceOffsiteBackup");
		me.edit.setupFilteredEdit(me.ui.monitor_sinceReport, me.edit, "sinceReport");
	};

	this.saveCancel = function(mode, deferred)
	{
		if (mode)
		{
			me.save(deferred);
		}
		else
		{
			me.edit.revert();
			me.populate();
			if (deferred !== undefined) deferred();
		}
	};

	this.checkExit = function(deferred)
	{
		if (me.edit.changed())
		{
			main.portal.confirmNavigationWithChanges(function()
			{
				me.save(deferred);				
			}, function()
			{
				me.edit.revert();
				// me.populate();
				if (deferred !== undefined) deferred();
			});

			return false;
		}

		return true;
	};

	this.save = function(deferred)
	{
		var params = main.portal.cdp.params();
		params.url.site = main.portal.site.id;
		me.edit.params("", params);
		main.portal.cdp.request("monitor_optionsSave monitor_options", params, function(data)
		{
			main.options = data.options || {};

			me.edit.revert();
			if (deferred !== undefined) deferred();
		});
	};	
}

$(function()
{
	try
	{
		monitor_tool = new Monitor();
		monitor_tool.init();
		monitor_tool.start();
	}
	catch (e)
	{
		error(e);
	}
});

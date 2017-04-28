/**********************************************************************************
 * $URL: https://source.etudes.org/svn/serenity/trunk/module/module-webapp/src/main/webapp/module.js $
 * $Id: module.js 12504 2016-01-10 00:30:08Z ggolden $
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

var module_tool = null;

function Module()
{
	var me = this;

	this.i18n = new e3_i18n(module_i10n, "en-us");
	this.portal = null;
	this.ui = null
	this.modes = null;
	this.onExit = null;

	this.viewMode = null;
	this.manageMode = null;
	this.optionMode = null;
	this.resourceMode = null;
	this.viewModuleMode = null;
	this.viewSectionMode = null;
	this.editModuleMode = null;
	this.editSectionMode = null;

	this.fs = 3; // 0 for homepage filesystem, 1 for CHS/resources file system, 2 - mneme, 3 - melete, 9 for serenity fs

	this.init = function()
	{
		me.i18n.localize();
		me.ui = findElements(["module_header", "module_modebar", "module_headerbar", "module_itemnav",
		                      "module_bar_view", "module_header_view", "module_view",
		                      "module_bar_viewModule", "module_header_viewModule", "module_viewModule",
		                      "module_bar_viewSection", "module_viewSection",
		                      "module_bar_manage", "module_header_manage", "module_manage",
		                      "module_option",
		                      "module_resource",
		                      "module_bar_editModule", "module_editModule",
		                      "module_bar_editSection", "module_editSection",
		                      "module_sectionTitle_template", "module_viewNav", "module_editNav",
		                      "module_selectFirst", "module_select1First"]);
		me.portal = portal_tool.features({onExit: me.checkExit, pin:[{ui:me.ui.module_header}]});
		
		me.viewMode = new Module_view(me);
		me.viewModuleMode = new Module_viewModule(me);
		me.viewSectionMode = new Module_viewSection(me);

		if ((me.portal.site.role >= Role.instructor) || (me.portal.site.role == Role.ta))
		{
			me.manageMode = new Module_manage(me);
			me.optionMode = new Module_option(me);
			me.resourceMode = new Module_resource(me);
			me.editModuleMode = new Module_editModule(me);
			me.editSectionMode = new Module_editSection(me);

			me.modes =
			[
				{name:me.i18n.lookup("mode_view", "View"), func:function(){me.startView();}},
				{name:me.i18n.lookup("mode_manage", "Manage"), func:function(){me.startManage();}},
				{name:me.i18n.lookup("mode_resource", "Resources"), func:function(){me.startResource();}},
				{name:me.i18n.lookup("mode_option", "Options"), func:function(){me.startOption();}}
			];
			me.ui.modebar = new e3_Modebar(me.ui.module_modebar);
			me.ui.modebar.set(me.modes, 0);

			me.manageMode.init();
			me.optionMode.init();
			me.resourceMode.init();
			me.editModuleMode.init();
			me.editSectionMode.init();
		}

		me.viewMode.init();
		me.viewModuleMode.init();
		me.viewSectionMode.init();

		show(me.ui.module_modebar, ((me.portal.site.role >= Role.instructor) || (me.portal.site.role == Role.ta)));
	};

	this.start = function()
	{
		if ((me.portal.site.role >= Role.instructor) || (me.portal.site.role == Role.ta))
		{
			me.startManage();
		}
		else if (me.portal.site.role == Role.student)
		{
			me.startView();
		}
	};

	this.startView = function()
	{
		if ((me.onExit != null) && (!me.onExit(function(){me.startView();}))) return;
		me.mode([me.ui.module_headerbar, me.ui.module_bar_view, me.ui.module_header_view, me.ui.module_view]);
		if (me.ui.modebar != null) me.ui.modebar.showSelected(0);
		me.viewMode.start();
	};

	this.startViewModule = function(moduleId)
	{
		if ((me.onExit != null) && (!me.onExit(function(){me.startViewModule(moduleId);}))) return;
		me.mode([me.ui.module_headerbar, me.ui.module_itemnav, me.ui.module_bar_viewModule, me.ui.module_header_viewModule, me.ui.module_viewModule]);
		if (me.ui.modebar != null) me.ui.modebar.showSelected(0);
		me.viewModuleMode.start(moduleId);
	};

	this.startViewSection = function(moduleId, sectionId)
	{
		if ((me.onExit != null) && (!me.onExit(function(){me.startViewSection(moduleId, sectionId);}))) return;
		me.mode([me.ui.module_headerbar, me.ui.module_itemnav, me.ui.module_bar_viewSection, me.ui.module_viewSection]);
		if (me.ui.modebar != null) me.ui.modebar.showSelected(0);
		me.viewSectionMode.start(moduleId, sectionId);
	};

	this.startManage = function()
	{
		if ((me.onExit != null) && (!me.onExit(function(){me.startManage();}))) return;
		me.mode([me.ui.module_headerbar, me.ui.module_bar_manage, me.ui.module_header_manage, me.ui.module_manage]);
		me.ui.modebar.showSelected(1);
		me.manageMode.start();
	};

	this.startOption = function()
	{
		if ((me.onExit != null) && (!me.onExit(function(){me.startOption();}))) return;
		me.mode([me.ui.module_option]);
		me.ui.modebar.showSelected(3);
		me.optionMode.start();
	};

	this.startResource = function()
	{
		if ((me.onExit != null) && (!me.onExit(function(){me.startResource();}))) return;
		me.mode([me.ui.module_resource]);
		me.ui.modebar.showSelected(2);
		me.resourceMode.start();
	};

	this.startEditModule = function(moduleId)
	{
		if ((me.onExit != null) && (!me.onExit(function(){me.startEditModule(moduleId);}))) return;
		me.mode([me.ui.module_headerbar, me.ui.module_itemnav, me.ui.module_bar_editModule, me.ui.module_editModule]);
		me.ui.modebar.showSelected(1);
		me.editModuleMode.start(moduleId);
	};

	this.startEditSection = function(moduleId, sectionId)
	{
		if ((me.onExit != null) && (!me.onExit(function(){me.startEditSection(moduleId, sectionId);}))) return;
		me.mode([me.ui.module_headerbar, me.ui.module_itemnav, me.ui.module_bar_editSection, me.ui.module_editSection]);
		me.ui.modebar.showSelected(1);
		me.editSectionMode.start(moduleId, sectionId);
	};

	this.mode = function(modeUi)
	{
		hide([me.ui.module_headerbar, me.ui.module_itemnav,
		      me.ui.module_bar_view, me.ui.module_header_view, me.ui.module_view,
		      me.ui.module_bar_viewModule, me.ui.module_header_viewModule, me.ui.module_viewModule,
		      me.ui.module_bar_viewSection, me.ui.module_viewSection,
		      me.ui.module_bar_manage, me.ui.module_header_manage, me.ui.module_manage,
		      me.ui.module_option,
		      me.ui.module_resource,
		      me.ui.module_bar_editModule, me.ui.module_editModule,
		      me.ui.module_bar_editSection, me.ui.module_editSection]);
		me.onExit = null;
		me.portal.resetScrolling();
		show(modeUi);
	};
	
	this.checkExit = function(deferred)
	{
		if (me.onExit == null) return true;
		return me.onExit(deferred);
	};
	
	this.fakeData = function(moduleId, sectionId)
	{
		var modules =
		[
			{
				id:1, title:"module 1", schedule:{open:0, due:0, allowUntil:0, close:0, hide:false, status:3, lastCall:false}, completed:true, inProgress:false, lastVisit:0,
				instructions: "Here are your instructions.  Read the one section.",
				nextSteps: "Next, read Module 2",
				attribution: {createdOn: 0, createdBy: "John Smith", modifiedOn: 0, modifiedBy: "John Smith"},
				sections:
				[
					{
						id: 10, title: "section 1-1",
						source: "A", content: "<p>This is my section content.</p>", instructions: "Read the following section for more understanding of the history of Flans and Fillings.",
						license: "Licensed under Creative Commons", licenseUrl: "http://creativecommons.org/licenses/by-nc-sa/2.0/",
						prev: null, next: null,
						attribution: {createdOn: 0, createdBy: "John Smith", modifiedOn: 0, modifiedBy: "John Smith"}
					}
				],
				prev: null, next: 2
			},
			{
				id:2, title:"module 2", schedule:{open:0, due:0, allowUntil:0, close:0, hide:false, status:3, lastCall:false}, completed:false, inProgress:true, lastVisit:0,
				instructions: "Here are your instructions.  Read all these sections.",
				attribution: {createdOn: 0, createdBy: "John Smith", modifiedOn: 0, modifiedBy: "John Smith"},
				sections:
				[
					{
						id:11, title:"section 2-1",
						source: "A", content:"<p>This is my section content.</p>", instructions:"Section instructions.",
						license: "Licensed under Creative Commons", licenseUrl: "http://creativecommons.org/licenses/by-nc-sa/2.0/",
						prev: null, next: 12,
						attribution: {createdOn: 0, createdBy: "John Smith", modifiedOn: 0, modifiedBy: "John Smith"}
					},
					{
						id:12, title:"section 2-2",
						source: "A", content:"<p>This is some section content.</p>",
						license: "Licensed under Creative Commons", licenseUrl: "http://creativecommons.org/licenses/by-nc-sa/2.0/",
						prev: 11, next: null,
						attribution: {createdOn: 0, createdBy: "John Smith", modifiedOn: 0, modifiedBy: "John Smith"}
					}
				],
				prev: 1, next: 3
			},
			{
				id:3, title:"module 3", schedule:{open:0, due:0, allowUntil:0, close:0, hide:false, status:3, lastCall:false}, completed:false, inProgress:false, lastVisit:0,
				nextSteps: "Work on Assignment 1.",
				attribution: {createdOn: 0, createdBy: "John Smith", modifiedOn: 0, modifiedBy: "John Smith"},
				sections:
				[
					{
						id:13, title:"section 3-1",
						source: "A", content:"<p>This is a section content.</p>",
						license: "Licensed under Creative Commons", licenseUrl: "http://creativecommons.org/licenses/by-nc-sa/2.0/",
						prev: null, next: 14,
						attribution: {createdOn: 0, createdBy: "John Smith", modifiedOn: 0, modifiedBy: "John Smith"}
					},
					{
						id:14, title:"section 3-2",
						source: "A", content:"<p>This is more section content.</p>", instructions:"Section instructions.",
						license: "Licensed under Creative Commons", licenseUrl: "http://creativecommons.org/licenses/by-nc-sa/2.0/",
						prev: 13, next: 15,
						attribution: {createdOn: 0, createdBy: "John Smith", modifiedOn: 0, modifiedBy: "John Smith"}
					},
					{
						id:15, title:"section 3-3",
						source: "A", content:"<p>This is other section content.</p>",
						license: "Licensed under Creative Commons", licenseUrl: "http://creativecommons.org/licenses/by-nc-sa/2.0/",
						prev: 14, next: null,
						attribution: {createdOn: 0, createdBy: "John Smith", modifiedOn: 0, modifiedBy: "John Smith"}
					}
				],
				prev: 2, next: null
			}
	    ];
		
		if ((moduleId != null) && (sectionId == null))
		{
			var module = findIdInList(moduleId, modules);
			return module;
		}
		
		if ((moduleId != null) && (sectionId != null))
		{
			var module = findIdInList(moduleId, modules);
			if (sectionId == "LAST")
			{
				var section = module.sections[module.sections.length-1];
				return section;
			}
			else
			{
				var section = findIdInList(sectionId, module.sections);
				return section;
			}
		}

		return modules;
	};
}

function Module_view(main)
{
	var me = this;

	this.ui = null;
	
	this.modules = [];

	this.init = function()
	{
		me.ui = findElements(["module_view_bookmarks", "module_view_table", "module_view_none"]);
		me.ui.table = new e3_Table(me.ui.module_view_table);

		onClick(me.ui.module_view_bookmarks, function(){console.log("bookmarks");});
		setupHoverControls([me.ui.module_view_bookmarks]);
	};

	this.start = function()
	{
		me.load();
	};
	
	this.load = function()
	{
		var params = main.portal.cdp.params();
		params.url.site = main.portal.site.id;
		main.portal.cdp.request("module_view", params, function(data)
		{
			me.modules = /* data.modules || []; */ main.fakeData();
			me.populate();
		});
	};

	this.populate = function()
	{
		me.ui.table.clear();

		$.each(me.modules, function(index, module)
		{
			var row = me.ui.table.row();
			// TODO: expand / collapse section control ?
			me.dot(module);
			// TODO: numbering ?
			me.ui.table.hotText(module.title, main.i18n.lookup("msg_viewModule", "view module"), function(){main.startViewModule(module.id);}, null, {width:"calc(100vw - 100px - 368px)", minWidth:"calc(1200px - 100px - 368px"});

			// TODO: prereq?
			me.ui.table.date(module.schedule.open, "-", "date2l");
			me.ui.table.date(module.schedule.due, "-", "date2l");
			me.ui.table.date(module.lastVisit, "-", "date2l");

			$.each(module.sections, function(i, section)
			{
				var row = me.ui.table.row();
				me.ui.table.dot(Dots.none);
				
				var cell = clone(main.ui.module_sectionTitle_template, ["module_sectionTitle_template_body", "module_sectionTitle_template_title"]);
				cell.module_sectionTitle_template_title.text(section.title);
				me.ui.table.hotElement(cell.module_sectionTitle_template_body, main.i18n.lookup("msg_viewSection", "view section"), function(){main.startViewSection(module.id, section.id);}, null, {width:"calc(100vw - 100px - 368px)", minWidth:"calc(1200px - 100px - 368px"});

				me.ui.table.date(null, "", "date2l");
				me.ui.table.date(null, "", "date2l");
				me.ui.table.date(null, "", "date2l");
			});
		});
		me.ui.table.done();
		show(me.ui.module_view_none, me.ui.table.rowCount() == 0);
	};

	this.dot = function(module)
	{
		if (module.completed)
		{
			me.ui.table.dot(Dots.complete, main.i18n.lookup("msg_complete", "finished"));
		}
		else if (module.inProgress)
		{
			me.ui.table.dot(Dots.progress, main.i18n.lookup("msg_inProgress", "in progress"));
		}
		else
		{
			me.ui.table.dot(Dots.none);
		}
	};
}

function Module_viewModule(main)
{
	var me = this;

	this.ui = null;

	this.module = null;

	this.init = function()
	{
		me.ui = findElements(["module_viewModule_title", "module_viewModule_table", "module_viewModule_none", "module_viewModule_instructions", "module_viewModule_next"]);
		me.ui.table = new e3_Table(me.ui.module_viewModule_table);
		me.ui.itemNav = new e3_ItemNav();

		// onClick(me.ui.module_grade_export, function(){me.exportCsv();});
		// setupHoverControls([me.ui.module_grade_export/*, me.ui.module_grade_boosting, me.ui.module_grade_showing, me.ui.module_grade_lowDrops*/]);
	};

	this.start = function(moduleId)
	{
		me.load(moduleId);
	};
	
	this.load = function(moduleId)
	{
		var params = main.portal.cdp.params();
		params.url.site = main.portal.site.id;
		params.url.module = moduleId;
		main.portal.cdp.request("module_viewModule", params, function(data)
		{
			me.module = /* data.module || {}; */ main.fakeData(moduleId);

			var nav = me.ui.itemNav.injectSpecial(main.ui.module_itemnav, main.ui.module_viewNav,
					["module_viewNav_contents", "module_viewNav_prev", "module_viewNav_next"], [me.done, me.prev, me.next]);
			nav.module_viewNav_prev.attr("disabled", (me.module.prev == null));
			applyClass("disabled", nav.module_viewNav_prev, (me.module.prev == null));
			nav.module_viewNav_next.attr("disabled", ((me.module.sections.length == 0) && (me.module.next == null)));
			applyClass("disabled", nav.module_viewNav_next, ((me.module.sections.length == 0) && (me.module.next == null)));

			me.populate();
		});
	};

	this.populate = function()
	{
		me.ui.module_viewModule_title.text(me.module.title);
		me.ui.module_viewModule_instructions.text(me.module.instructions);
		show(me.ui.module_viewModule_instructions, (me.module.instructions != null));
		
		me.ui.table.clear();
		
		me.ui.table.row();
		me.ui.table.headerRow(main.i18n.lookup("msg_sections", "Sections"));

		$.each(me.module.sections, function(index, section)
		{
			var row = me.ui.table.row();
			
			var cell = clone(main.ui.module_sectionTitle_template, ["module_sectionTitle_template_body", "module_sectionTitle_template_title"]);
			cell.module_sectionTitle_template_title.text(section.title);
			me.ui.table.hotElement(cell.module_sectionTitle_template_body, main.i18n.lookup("msg_viewSection", "view section"), function(){main.startViewSection(me.module.id, section.id);}, null, {width:"calc(100vw - 100px - 222px)", minWidth:"calc(1200px - 100px - 222px"});

			me.ui.table.date(section.lastVisit, "-", "date");
		});
		me.ui.table.done();
		show(me.ui.module_viewModule_none, me.ui.table.rowCount() == 0);
		
		// next steps
		me.ui.module_viewModule_next.text(me.module.nextSteps);
		show(me.ui.module_viewModule_next, (me.module.nextSteps != null));
	};

	this.prev = function()
	{
		if (me.module.prev != null)
		{
			main.startViewSection(me.module.prev, "LAST");
		}
	};

	this.next = function()
	{
		if (me.module.sections.length > 0)
		{
			main.startViewSection(me.module.id, me.module.sections[0].id);
		}
		else if (me.module.next != null)
		{
			main.startViewModule(me.module.next);
		}
	};

	this.done = function()
	{
		main.startView();
	};
}

function Module_viewSection(main)
{
	var me = this;

	this.ui = null;

	this.module = null;
	this.section = null;

	this.init = function()
	{
		me.ui = findElements(["module_viewSection_moduleTitle", "module_viewSection_sectionTitle", "module_viewSection_bookmarks",
		                      "module_viewSection_instructions", "module_viewSection_content", "module_viewSection_license"]);
		me.ui.itemNav = new e3_ItemNav();

		onClick(me.ui.module_viewSection_license, function(){me.ui.module_viewSection_license.blur();}, true);
		// setupHoverControls([me.ui.module_grade_export/*, me.ui.module_grade_boosting, me.ui.module_grade_showing, me.ui.module_grade_lowDrops*/]);
	};

	this.start = function(moduleId, sectionId)
	{
		me.load(moduleId, sectionId);
	};
	
	this.load = function(moduleId, sectionId)
	{
		var params = main.portal.cdp.params();
		params.url.site = main.portal.site.id;
		params.url.module = moduleId;
		params.url.section = sectionId;
		main.portal.cdp.request("module_viewSection", params, function(data)
		{
			me.module = /* data.module || {}; */ main.fakeData(moduleId);
			me.section = /* data.section || {}; */ main.fakeData(moduleId, sectionId);

			var nav = me.ui.itemNav.injectSpecial(main.ui.module_itemnav, main.ui.module_viewNav,
					["module_viewNav_contents", "module_viewNav_prev", "module_viewNav_next"],
					[me.done, me.prev, me.next]);
			nav.module_viewNav_next.attr("disabled", ((me.section.next == null) && (me.module.next == null)));
			applyClass("disabled", nav.module_viewNav_next, ((me.section.next == null) && (me.module.next == null)));
			onClick(me.ui.module_viewSection_moduleTitle, function(){main.startViewModule(me.module.id);});

			me.populate();
		});
	};

	this.populate = function()
	{
		me.ui.module_viewSection_moduleTitle.text(me.module.title);
		me.ui.module_viewSection_sectionTitle.text(me.section.title);

		me.ui.module_viewSection_instructions.text(me.section.instructions);
		show(me.ui.module_viewSection_instructions, (me.section.instructions != null));

		me.ui.module_viewSection_content.html(me.section.content);
		me.ui.module_viewSection_license.text(me.section.license);
		me.ui.module_viewSection_license.attr("href", me.section.licenseUrl);
		show(me.ui.module_viewSection_license.parent(), (me.section.license != null)); 
	};

	this.prev = function()
	{
		if (me.section.prev != null)
		{
			main.startViewSection(me.module.id, me.section.prev);
		}
		else
		{
			main.startViewModule(me.module.id);
		}
	};

	this.next = function()
	{
		if (me.section.next != null)
		{
			main.startViewSection(me.module.id, me.section.next);
		}
		else if (me.module.next != null)
		{
			main.startViewModule(me.module.next);
		}
	};

	this.done = function()
	{
		main.startView();
	};
}

function Module_manage(main)
{
	var me = this;

	this.ui = null;

	this.modules = [];
	this.init = function()
	{
		me.ui = findElements(["module_manage_addModule", "module_manage_delete", "module_manage_export", "module_manage_import", "module_manage_archive", "module_manage_restore", "module_manage_addSection",
		                      "module_manage_table", "module_manage_none"]);
		me.ui.table = new e3_Table(me.ui.module_manage_table);
		me.ui.table.setupSelection("module_manage_select", me.updateActions);
		me.ui.table.selectAllHeader(1, main.ui.module_header_manage);

		onClick(me.ui.module_manage_addModule, me.addModule);
		onClick(me.ui.module_manage_addSection, me.addSection);
		onClick(me.ui.module_manage_delete, me.deleteModule);
		onClick(me.ui.module_manage_export, me.exportModules);
		onClick(me.ui.module_manage_import, me.import);
		onClick(me.ui.module_manage_archive, me.archive);
		onClick(me.ui.module_manage_restore, me.restore);
		setupHoverControls([me.ui.module_manage_addModule, me.ui.module_manage_delete, me.ui.module_manage_export, me.ui.module_manage_import, me.ui.module_manage_archive, me.ui.module_manage_restore, me.ui.module_manage_addSection]);
	};

	this.start = function()
	{
		main.onExit = me.checkExit;
		me.load();
	};
	
	this.load = function()
	{
		var params = main.portal.cdp.params();
		params.url.site = main.portal.site.id;
		main.portal.cdp.request("module_manage", params, function(data)
		{
			me.processData(data);
		});
	};

	this.processData = function(data)
	{
		me.modules = /* data.modules || []; */ main.fakeData();

		me.makeEdit();
		me.populate();
	};

	this.makeEdit = function()
	{
		// edit of the modules, id and dates
		me.edit = new e3_Edit({items: me.modules}, ["items[].type", "items[].schedule.status", "items[].schedule.close", "items[].schedule.lastCall", "items[].sections"], function(changed)
		{
			main.ui.modebar.enableSaveDiscard(changed ? me.saveCancel : null);
		});
		main.ui.modebar.enableSaveDiscard(null);
	};

	this.populate = function()
	{
		me.ui.table.clear();

		$.each(me.modules, function(index, module)
		{
			var inEdit = findIdInList(module.id, me.edit.items);

			me.ui.table.row();
			me.ui.table.selectBox(module.id);

			me.ui.table.hotText(module.title, main.i18n.lookup("msg_editModule", "edit module"), function(){main.startEditModule(module.id);}, null, {width: "calc(100vw - 100px - 648px)", minWidth:"calc(1200px - 100px - 648px)"});

			me.edit.setupDateEdit(me.ui.table.inputDate().find("input"), inEdit.schedule, "open", main.portal.timestamp, true);
			me.edit.setupDateEdit(me.ui.table.inputDate().find("input"), inEdit.schedule, "due", main.portal.timestamp, false);
			me.edit.setupDateEdit(me.ui.table.inputDate().find("input"), inEdit.schedule, "allowUntil", main.portal.timestamp, false);

			me.ui.table.contextMenu(
			[
				{title: main.i18n.lookup("cm_access", "Special Access"), action: function(){me.access(module);}},
				{title: main.i18n.lookup("cm_duplicate", "Duplicate"), action: function(){me.duplicate(module);}},
				{title: main.i18n.lookup("cm_print", "Print"), action: function(){me.print(module);}}
	        ]);

			$.each(module.sections, function(i, section)
			{
				var row = me.ui.table.row();
				me.ui.table.text("", null, {width:16}); //me.ui.table.selectBox(section.id);

				var cell = clone(main.ui.module_sectionTitle_template, ["module_sectionTitle_template_body", "module_sectionTitle_template_title"]);
				cell.module_sectionTitle_template_title.text(section.title);
				me.ui.table.hotElement(cell.module_sectionTitle_template_body, main.i18n.lookup("msg_editSection", "edit section"), function(){main.startEditSection(module.id, section.id);}, null, {width:"calc(100vw - 100px - 648px)", minWidth:"calc(1200px - 100px - 648px"});

				me.ui.table.date(null, "", "date2l");
				me.ui.table.date(null, "", "date2l");
				me.ui.table.date(null, "", "date2l");
			});
		});
		me.ui.table.done();
		show(me.ui.module_view_none, me.ui.table.rowCount() == 0);
	};

	this.updateActions = function()
	{
		me.ui.table.updateActions([me.ui.module_manage_addSection], [me.ui.module_manage_delete, me.ui.module_manage_export, me.ui.module_manage_archive]);
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
			main.ui.modebar.enableSaveDiscard(null);
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
				// main.ui.modebar.enableSaveDiscard(null);
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
		console.log(params);
		main.portal.cdp.request("module_saveManage module_manage", params, function(data)
		{
			me.processData(data);

			if (deferred !== undefined) deferred();
		});
	};

	this.access = function(module)
	{
		console.log("special access", module);
	};

	this.duplicate = function(module)
	{
		console.log("duplicate", module);
	};

	this.print = function(module)
	{
		console.log("print", module);
	};
	
	this.addModule = function()
	{
		console.log("add module");
	};

	this.addSection = function()
	{
		var params = main.portal.cdp.params();
		params.url.site = main.portal.site.id;
		params.post.ids = me.ui.table.selected();

		if (params.post.ids.length != 1)
		{
			main.portal.dialogs.openAlert(main.ui.module_select1First);
			return;
		}

		console.log("add section", params.post.ids);
	};

	this.deleteModule = function()
	{
		var params = main.portal.cdp.params();
		params.url.site = main.portal.site.id;
		params.post.ids = me.ui.table.selected();

		if (params.post.ids.length == 0)
		{
			main.portal.dialogs.openAlert(main.ui.module_selectFirst);
			return;
		}

		main.portal.cdp.request("module_delete module_manage", params, function(data)
		{
			me.processData(data);
		});
	};

	this.exportModules = function()
	{
		var params = main.portal.cdp.params();
		params.url.site = main.portal.site.id;
		params.post.ids = me.ui.table.selected();

		if (params.post.ids.length == 0)
		{
			main.portal.dialogs.openAlert(main.ui.module_selectFirst);
			return;
		}

		console.log("export", params.post.ids);
//		main.portal.cdp.request("module_archive module_manage", params, function(data)
//		{
//			me.processData(data);
//		});
	};

	this.import = function()
	{
		console.log("import");
	};

	this.archive = function()
	{
		var params = main.portal.cdp.params();
		params.url.site = main.portal.site.id;
		params.post.ids = me.ui.table.selected();

		if (params.post.ids.length == 0)
		{
			main.portal.dialogs.openAlert(main.ui.module_selectFirst);
			return;
		}

		main.portal.cdp.request("module_archive module_manage", params, function(data)
		{
			me.processData(data);
		});
	};

	this.restore = function()
	{
		console.log("restore");
	}
}

function Module_resource(main)
{
	var me = this;

	this.ui = null;

	this.init = function()
	{
		me.ui = findElements(["module_resource_picker"]);
		me.ui.filer = new e3_FilerCK(me.ui.module_resource_picker, {nameStartsAtPartIndex: 9});

		// me.ui.table = new e3_Table(me.ui.module_grade_table);

		// onClick(me.ui.module_grade_export, function(){me.exportCsv();});
		// setupHoverControls([me.ui.module_grade_export/*, me.ui.module_grade_boosting, me.ui.module_grade_showing, me.ui.module_grade_lowDrops*/]);
	};

	this.start = function()
	{
//		main.onExit = me.checkExit;
		me.load();
	};
	
	this.load = function()
	{
		var params = main.portal.cdp.params();
		params.url.site = main.portal.site.id;
		main.portal.cdp.request("module_resources", params, function(data) // TODO: just need a good main.fs, which might already be set
		{
			me.populate();
		});
	};

	this.populate = function()
	{
		me.ui.filer.disable();
//		if (me.edit.fileRefId !== undefined)
//		{
//			me.ui.filer.set({refId: me.edit.fileRefId, name: me.item.fileName});
//		}
//		else
//		{
//			me.ui.filer.set({url: me.edit.fileUrl});
//		}
		me.ui.filer.enable(function()
		{
//			var selectedMyFile = me.ui.filer.get();
//			if (selectedMyFile.refId !== undefined)
//			{
//				me.edit.set(me.edit, "fileRefId", selectedMyFile.refId);
//			}
//			else
//			{
//				me.edit.set(me.edit, "fileUrl", selectedMyFile.url);
//			}
//			me.ui.module_editSection_F_selected.text(selectedMyFile.name);
//			fade(me.ui.module_editSection_F_alert, false);
		}, main.fs);
	};
}

function Module_option(main)
{
	var me = this;

	this.ui = null;

	this.options = null;
	this.edit = null;

	this.init = function()
	{
		me.ui = findElements(["module_option_copyright",
		                      "module_option_copyright_NY", "module_option_copyright_NY_name", "module_option_copyright_NY_year",
		                      "module_option_copyright_UND", "module_option_copyright_AUT", "module_option_copyright_PUB", "module_option_copyright_CCL", "module_option_copyright_FUE",
		                      "module_option_studentPrinting", "module_option_autoNumbering"]);
		// me.ui.table = new e3_Table(me.ui.module_grade_table);

		// onClick(me.ui.module_grade_export, function(){me.exportCsv();});
		// setupHoverControls([me.ui.module_grade_export/*, me.ui.module_grade_boosting, me.ui.module_grade_showing, me.ui.module_grade_lowDrops*/]);
	};

	this.start = function()
	{
		main.onExit = me.checkExit;
		me.load();
	};
	
	this.load = function()
	{
		var params = main.portal.cdp.params();
		params.url.site = main.portal.site.id;
		main.portal.cdp.request("module_options", params, function(data)
		{
			me.options = data.options;
			me.makeEdit();
			me.populate();
		});
	};

	this.makeEdit = function()
	{
		me.edit = new e3_Edit(me.options, [], function(changed)
		{
			main.ui.modebar.enableSaveDiscard(changed ? me.saveCancel : null);
		});

		main.ui.modebar.enableSaveDiscard(null);
	};

	this.populate = function()
	{
		me.populateCopyright();
		
		me.edit.setupCheckEdit(me.ui.module_option_studentPrinting, me.edit, "allowPrint");
		me.edit.setupCheckEdit(me.ui.module_option_autoNumbering, me.edit, "autoNumber");
	};

	this.populateCopyright = function()
	{
		var copyrightUndefined = true;
		if (me.edit.copyright !== undefined)
		{
			copyrightUndefined = me.edit.copyright == "UND";
		}
		me.ui.module_option_copyright.val(me.edit.copyright || "UND");
		onChange(me.ui.module_option_copyright, function(target, isFinalChange, event)
		{
			if (isFinalChange)
			{
				me.edit.set(me.edit, "copyright", me.ui.module_option_copyright.val());
				me.populateCopyright();
			}
		});

		if (!copyrightUndefined)
		{
			me.edit.setupFilteredEdit(me.ui.module_option_copyright_NY_name, me.edit, "copyrightName");
			me.edit.setupFilteredEdit(me.ui.module_option_copyright_NY_year, me.edit, "copyrightYear");
		}
		show(me.ui.module_option_copyright_NY, (!copyrightUndefined));
		show(me.ui.module_option_copyright_UND, ("UND" == me.edit.copyright));
		show(me.ui.module_option_copyright_AUT, ("AUT" == me.edit.copyright));
		show(me.ui.module_option_copyright_PUB, ("PUB" == me.edit.copyright));
		show(me.ui.module_option_copyright_CCL, ("CCL" == me.edit.copyright));
		show(me.ui.module_option_copyright_FUE, ("FUE" == me.edit.copyright));
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
				me.populate();
				if (deferred !== undefined) deferred();
			});

			return false;
		}

		return true;
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

	this.save = function(deferred)
	{
		var params = main.portal.cdp.params();
		params.url.site = main.portal.site.id;
		me.edit.params("", params);
		console.log(params);
		main.portal.cdp.request("module_saveOptions module_options", params, function(data)
		{
			me.options = data.options;

			me.makeEdit();
			me.populate();

			if (deferred !== undefined) deferred();
		});
	};
}

function Module_editModule(main)
{
	var me = this;

	this.ui = null;

	this.module = null;
	this.edit = null;

	this.init = function()
	{
		me.ui = findElements(["module_editModule_moduleTitle", "module_editModule_add",
		                      "module_editModule_title", "module_editModule_instructions", "module_editModule_nextSteps",
		                      "module_editModule_open", "module_editModule_hide", "module_editModule_due", "module_editModule_allow",
		                      "module_editModule_attribution",
		                      ]);
		me.ui.itemNav = new e3_ItemNav();

		onClick(me.ui.module_editModule_add, me.addSection);
		setupHoverControls([me.ui.module_editModule_add]);
	};

	this.start = function(moduleId)
	{
		main.onExit = me.checkExit;
		me.load(moduleId);
	};
	
	this.load = function(moduleId)
	{
		var params = main.portal.cdp.params();
		params.url.site = main.portal.site.id;
		params.url.module = moduleId;
		main.portal.cdp.request("module_editModule", params, function(data)
		{
			me.module = /* data.module; */ main.fakeData(moduleId);

			me.ui.nav = me.ui.itemNav.injectSpecial(main.ui.module_itemnav, main.ui.module_editNav,
					["module_editNav_done", "module_editNav_save", "module_editNav_prev", "module_editNav_next"],
					[me.done, me.save, me.prev, me.next]);
			me.ui.nav.module_editNav_prev.attr("disabled", (me.module.prev == null));
			applyClass("disabled", me.ui.nav.module_editNav_prev, (me.module.prev == null));
			me.ui.nav.module_editNav_next.attr("disabled", ((me.module.sections.length == 0) && (me.module.next == null)));
			applyClass("disabled", me.ui.nav.module_editNav_next, ((me.module.sections.length == 0) && (me.module.next == null)));

			me.makeEdit();
			me.populate();
		});
	};

	this.makeEdit = function()
	{
		me.edit = new e3_Edit(me.module, ["id", "lastVisit", "license", "licenseUrl", "prev", "next", "attribution", "completed", "inProgress", "schedule.lastCall", "schedule.status", "sections"], function(changed)
		{
			main.ui.modebar.enableSaveDiscard(changed ? me.saveCancel : null);
			me.ui.itemNav.enableSave(changed ? me.saveCancel : null, me.ui.nav.module_editNav_save);
		});
		me.edit.setFilters({"title": me.edit.stringFilter, "instructions": me.edit.stringFilter, "nextSteps": me.edit.stringFilter});
	
		main.ui.modebar.enableSaveDiscard(null);
		me.ui.itemNav.enableSave(null, me.ui.nav.module_editNav_save);
	};

	this.populate = function()
	{
		me.ui.module_editModule_moduleTitle.text(me.module.title);

		me.edit.setupFilteredEdit(me.ui.module_editModule_title, me.edit, "title", function(val, finalChange)
		{
			me.ui.module_editModule_moduleTitle.text(val);
		});

		me.edit.setupFilteredEdit(me.ui.module_editModule_instructions, me.edit, "instructions");
		me.edit.setupFilteredEdit(me.ui.module_editModule_nextSteps, me.edit, "nextSteps");

		me.edit.setupDateEdit(me.ui.module_editModule_open, me.edit.schedule, "open", main.portal.timestamp, true);
		me.edit.setupCheckEdit(me.ui.module_editModule_hide, me.edit.schedule, "hide");
		me.edit.setupDateEdit(me.ui.module_editModule_due, me.edit.schedule, "due", main.portal.timestamp, false);
		me.edit.setupDateEdit(me.ui.module_editModule_allow, me.edit.schedule, "allowUntil", main.portal.timestamp, false);

		if (me.module.id != -1) new e3_Attribution().inject(me.ui.module_editModule_attribution, me.module.id, me.module.attribution);
 		show(me.ui.module_editModule_attribution, (me.module.id != -1));
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
				me.populate();
				if (deferred !== undefined) deferred();
			});

			return false;
		}

		return true;
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

	this.save = function(deferred)
	{
		var params = main.portal.cdp.params();
		params.url.site = main.portal.site.id;
		params.url.module = me.module.id;
		me.edit.params("", params);
		console.log(params);
		main.portal.cdp.request("module_saveModule module_editModule", params, function(data)
		{
			me.module = /* data.module; */ main.fakeData(me.module.id);

			me.makeEdit();
			me.populate();

			if (deferred !== undefined) deferred();
		});
	};

	this.prev = function()
	{
		if (me.module.prev != null)
		{
			main.startEditSection(me.module.prev, "LAST");
		}
	};

	this.next = function()
	{
		if (me.module.sections.length > 0)
		{
			main.startEditSection(me.module.id, me.module.sections[0].id);
		}
		else if (me.module.next != null)
		{
			main.startEditModule(me.module.next);
		}
	};

	this.done = function()
	{
		if (me.edit.changed())
		{
			me.save(me.done)
		}
		else
		{
			main.startManage();
		}
	};
	
	this.addSection = function()
	{
		if ((main.onExit != null) && (!main.onExit(function(){me.addSection();}))) return;
		console.log("add section");
	};
}

function Module_editSection(main)
{
	var me = this;

	this.ui = null;

	this.module = null;
	this.section = null;
	this.edit = null;

	this.init = function()
	{
		me.ui = findElements(["module_editSection_moduleTitle", "module_editSection_add", "module_editSection_preview", "module_editSection_title", "module_editSection_instructions", "module_editSection_attribution",
		                      "module_editSection_A", "module_editSection_A_content",
		                      "module_editSection_F", "module_editSection_F_selected", "module_editSection_F_alert", "module_editSection_F_picker", "module_editSection_F_alt",
		                      "module_editSection_L", "module_editSection_L_url", "module_editSection_L_alert", "module_editSection_L_alt", "module_editSection_L_newWindow",
		                      "module_editSection_L_key", "module_editSection_L_secret", "module_editSection_L_custom",
		                      "module_editSection_W", "module_editSection_W_url", "module_editSection_W_alert", "module_editSection_W_alt", "module_editSection_W_newWindow",
		                      "module_editSection_copyright",
		                      "module_editSection_copyright_NY", "module_editSection_copyright_NY_name", "module_editSection_copyright_NY_year",
		                      "module_editSection_copyright_UND", "module_editSection_copyright_AUT", "module_editSection_copyright_PUB", "module_editSection_copyright_CCL", "module_editSection_copyright_FUE"
		                      ]);
		me.ui.itemNav = new e3_ItemNav();
		me.ui.editor = new e3_EditorCK(me.ui.module_editSection_A_content, {height: 350});
		me.ui.filer = new e3_FilerCK(me.ui.module_editSection_F_picker, {nameStartsAtPartIndex: 9});

		onClick(me.ui.module_editSection_add, me.addSection);
		onClick(me.ui.module_editSection_preview, me.preview);
		setupHoverControls([me.ui.module_editSection_add, me.ui.module_editSection_preview]);
	};

	this.start = function(moduleId, sectionId)
	{
		main.onExit = me.checkExit;
		me.load(moduleId, sectionId);
	};
	
	this.load = function(moduleId, sectionId)
	{
		var params = main.portal.cdp.params();
		params.url.site = main.portal.site.id;
		params.url.module = moduleId;
		params.url.section = sectionId;
		main.portal.cdp.request("module_editSection", params, function(data)
		{
			me.module = /* data.module; */ main.fakeData(moduleId);
			me.section = /* data.section; */ main.fakeData(moduleId, sectionId);

			me.ui.nav = me.ui.itemNav.injectSpecial(main.ui.module_itemnav, main.ui.module_editNav,
					["module_editNav_done", "module_editNav_save", "module_editNav_prev", "module_editNav_next"],
					[me.done, me.save, me.prev, me.next]);
			me.ui.nav.module_editNav_next.attr("disabled", ((me.section.next == null) && (me.module.next == null)));
			applyClass("disabled", me.ui.nav.module_editNav_next, ((me.section.next == null) && (me.module.next == null)));

			onClick(me.ui.module_editSection_moduleTitle, me.toModule);

			me.makeEdit();
			me.populate();
		});
	};

	this.makeEdit = function()
	{
		me.edit = new e3_Edit(me.section, ["id", "lastVisit", "license", "licenseUrl", "prev", "next", "attribution"], function(changed)
		{
			main.ui.modebar.enableSaveDiscard(changed ? me.saveCancel : null);
			me.ui.itemNav.enableSave(changed ? me.saveCancel : null, me.ui.nav.module_editNav_save);
		});
		me.edit.setFilters({"title": me.edit.stringFilter, "instructions": me.edit.stringFilter, "copyrightName": me.edit.stringFilter, "copyrightYear": me.edit.stringFilter,
			"url": me.edit.stringFilter, "alt": me.edit.stringFilter, "key": me.edit.stringFilter, "secret": me.edit.stringFilter, "custom": me.edit.stringFilter});
	
		main.ui.modebar.enableSaveDiscard(null);
		me.ui.itemNav.enableSave(null, me.ui.nav.module_editNav_save);
	};

	this.populate = function()
	{
		me.ui.module_editSection_moduleTitle.text(me.module.title);

		me.edit.setupFilteredEdit(me.ui.module_editSection_title, me.edit, "title");
		me.edit.setupFilteredEdit(me.ui.module_editSection_instructions, me.edit, "instructions");

		me.edit.setupRadioEdit("module_editSection_source", me.edit, "source", function()
		{
			me.populate();
		});
		
		if ("A" == me.edit.source)
		{
			// for authored content
			me.ui.editor.disable();
			me.ui.editor.set(me.edit.content);
			me.ui.editor.enable(function()
			{
				me.edit.set(me.edit, "content", me.ui.editor.get());
			}, false /* no focus */, main.fs);
		}
		else if ("F" == me.edit.source)
		{
			me.ui.filer.disable();
			if (me.edit.fileRefId !== undefined)
			{
				me.ui.filer.set({refId: me.edit.fileRefId, name: me.item.fileName});
			}
			else
			{
				me.ui.filer.set({url: me.edit.fileUrl});
			}
			me.ui.filer.enable(function()
			{
				var selectedMyFile = me.ui.filer.get();
				if (selectedMyFile.refId !== undefined)
				{
					me.edit.set(me.edit, "fileRefId", selectedMyFile.refId);
				}
				else
				{
					me.edit.set(me.edit, "fileUrl", selectedMyFile.url);
				}
				me.ui.module_editSection_F_selected.text(selectedMyFile.name);
				fade(me.ui.module_editSection_F_alert, false);
			}, main.fs);
			if (me.section.fileName !== undefined)
			{
				me.ui.module_editSection_F_selected.text(me.section.fileName);
			}
			else
			{
				me.ui.module_editSection_F_selected.html(main.i18n.lookup("msg_noFileSelected", "<i>none</i>"));
			}
			show(me.ui.module_editSection_F_alert, (me.section.fileName == null));

			me.edit.setupFilteredEdit(me.ui.module_editSection_F_alt, me.edit, "alt");
		}
		else if ("L" == me.edit.source)
		{
			me.edit.setupFilteredEdit(me.ui.module_editSection_L_url, me.edit, "url", function(val)
			{
				fade(me.ui.module_editSection_L_alert, (trim(val) == null));
			});
			show(me.ui.module_editSection_L_alert, (trim(me.edit.url) == null));

			me.edit.setupFilteredEdit(me.ui.module_editSection_L_alt, me.edit, "alt");
			me.edit.setupCheckEdit(me.ui.module_editSection_L_newWindow, me.edit, "newWindow");
			me.edit.setupFilteredEdit(me.ui.module_editSection_L_key, me.edit, "key");
			me.edit.setupFilteredEdit(me.ui.module_editSection_L_secret, me.edit, "secret");
			me.edit.setupFilteredEdit(me.ui.module_editSection_L_custom, me.edit, "custom");
		}
		else if ("W" == me.edit.source)
		{
			me.edit.setupFilteredEdit(me.ui.module_editSection_W_url, me.edit, "url", function(val)
			{
				fade(me.ui.module_editSection_W_alert, (trim(val) == null));
			});
			show(me.ui.module_editSection_W_alert, (trim(me.edit.url) == null));

			me.edit.setupFilteredEdit(me.ui.module_editSection_W_alt, me.edit, "alt");
			me.edit.setupCheckEdit(me.ui.module_editSection_W_newWindow, me.edit, "newWindow");
		}
		show(me.ui.module_editSection_A, ("A" == me.edit.source));
		show(me.ui.module_editSection_F, ("F" == me.edit.source));
		show(me.ui.module_editSection_L, ("L" == me.edit.source));
		show(me.ui.module_editSection_W, ("W" == me.edit.source));

		me.populateCopyright();
		
		if (me.section.id != -1) new e3_Attribution().inject(me.ui.module_editSection_attribution, me.section.id, me.section.attribution);
 		show(me.ui.module_editSection_attribution, (me.section.id != -1));
	};

	this.populateCopyright = function()
	{
		var copyrightUndefined = true;
		if (me.edit.copyright !== undefined)
		{
			copyrightUndefined = me.edit.copyright == "UND";
		}
		me.ui.module_editSection_copyright.val(me.edit.copyright || "UND");
		onChange(me.ui.module_editSection_copyright, function(target, isFinalChange, event)
		{
			if (isFinalChange)
			{
				me.edit.set(me.edit, "copyright", me.ui.module_editSection_copyright.val());
				me.populateCopyright();
			}
		});

		if (!copyrightUndefined)
		{
			me.edit.setupFilteredEdit(me.ui.module_editSection_copyright_NY_name, me.edit, "copyrightName");
			me.edit.setupFilteredEdit(me.ui.module_editSection_copyright_NY_year, me.edit, "copyrightYear");
		}
		show(me.ui.module_editSection_copyright_NY, (!copyrightUndefined));
		show(me.ui.module_editSection_copyright_UND, ("UND" == me.edit.copyright));
		show(me.ui.module_editSection_copyright_AUT, ("AUT" == me.edit.copyright));
		show(me.ui.module_editSection_copyright_PUB, ("PUB" == me.edit.copyright));
		show(me.ui.module_editSection_copyright_CCL, ("CCL" == me.edit.copyright));
		show(me.ui.module_editSection_copyright_FUE, ("FUE" == me.edit.copyright));
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
				me.populate();
				if (deferred !== undefined) deferred();
			});

			return false;
		}

		return true;
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

	this.save = function(deferred)
	{
		var params = main.portal.cdp.params();
		params.url.site = main.portal.site.id;
		params.url.module = me.module.id;
		params.url.section = me.section.id;
		me.edit.params("", params);
		console.log(params);
		main.portal.cdp.request("module_saveSection module_editSection", params, function(data)
		{
			me.module = /* data.module; */ main.fakeData(me.module.id);
			me.section = /* data.section; */ main.fakeData(me.module.id, me.section.id);

			me.makeEdit();
			me.populate();

			if (deferred !== undefined) deferred();
		});
	};

	this.prev = function()
	{
		if (me.edit.changed())
		{
			me.save(me.prev)
		}
		else
		{
			if (me.section.prev != null)
			{
				main.startEditSection(me.module.id, me.section.prev);
			}
			else
			{
				main.startEditModule(me.module.id);
			}
		}
	};

	this.next = function()
	{
		if (me.edit.changed())
		{
			me.save(me.next)
		}
		else
		{
			if (me.section.next != null)
			{
				main.startEditSection(me.module.id, me.section.next);
			}
			else if (me.module.next != null)
			{
				main.startEditModule(me.module.next);
			}
		}
	};

	this.toModule = function()
	{
		if (me.edit.changed())
		{
			me.save(me.toModule)
		}
		else
		{
			main.startEditModule(me.module.id);
		}
	};

	this.done = function()
	{
		if (me.edit.changed())
		{
			me.save(me.done)
		}
		else
		{
			main.startManage();
		}
	};
	
	this.preview = function()
	{
		console.log("preview", me.edit);
	};
	
	this.addSection = function()
	{
		if ((main.onExit != null) && (!main.onExit(function(){me.addSection();}))) return;
		console.log("add section");
	};
}

$(function()
{
	try
	{
		module_tool = new Module();
		module_tool.init();
		module_tool.start();
	}
	catch (e)
	{
		error(e);
	}
});

/**********************************************************************************
 * $URL: https://source.etudes.org/svn/serenity/trunk/syllabus/syllabus-webapp/src/main/webapp/syllabus.js $
 * $Id: syllabus.js 12504 2016-01-10 00:30:08Z ggolden $
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

var syllabus_tool = null;

function Syllabus()
{
	var me = this;

	this.i18n = new e3_i18n(syllabus_i10n, "en-us");
	this.ui = null;
	this.portal = null;

	this.viewMode = null;
	this.manageMode = null;
	this.editMode = null;

	this.onExit = null;

	this.syllabus = null;
	this.fs = 9; // 0 for homepage filesystem, 1 for CHS/resources file system, 9 for serenity fs

	this.sectionPosition = function(section)
	{
		for (index = 0; index < me.syllabus.sections.length; index++)
		{
			if (me.syllabus.sections[index].id == section.id)
			{
				var rv = {};
				rv.item = index+1;
				rv.total = me.syllabus.sections.length;
				rv.prev = index > 0 ? me.syllabus.sections[index-1] : null;
				rv.next = index < me.syllabus.sections.length-1 ? me.syllabus.sections[index+1] : null;
				
				return rv;
			}			
		}
	};
	
	this.findSection = function(id)
	{
		for (index = 0; index < me.syllabus.sections.length; index++)
		{
			if (me.syllabus.sections[index].id == id)
			{
				return me.syllabus.sections[index];
			}
		}
		
		return null;			
	};

	this.load = function(onLoad)
	{
		var params = me.portal.cdp.params();
		params.url.site = me.portal.site.id;
		me.portal.cdp.request("syllabus_get", params, function(data)
		{
			me.syllabus = data.syllabus;
			me.fs = data.fs;
			onLoad();
		});
	};

	this.init = function()
	{
		me.i18n.localize();
		me.ui = findElements(["syllabus_header", "syllabus_modebar", "syllabus_headerbar", "syllabus_itemnav",
		                      "syllabus_view", "syllabus_view_edit",
		                      "syllabus_manage", "syllabus_bar_manage", "syllabus_header_manage_sections",
		                      "syllabus_edit", "syllabus_bar_edit",
		                      "syllabus_template_newWindow", "syllabus_template_inline", "syllabus_template_section",
		                      "syllabus_selectFirst", "syllabus_confirmDelete"]);

		me.portal = portal_tool.features({onExit:function(deferred){return me.checkExit(deferred);}, pin:[{ui:me.ui.syllabus_header}]});

		me.viewMode = new SyllabusView(me);
		me.viewMode.init();
		
		if (me.portal.site.role >= Role.instructor)
		{
			me.ui.modebar = new e3_Modebar(me.ui.syllabus_modebar);
			me.modes =
			[
				{name:me.i18n.lookup("mode_view", "View"), func:me.startView},
				{name:me.i18n.lookup("mode_edit", "Manage"), func:me.startManage}
			];
			me.ui.modebar.set(me.modes, 0);

			me.manageMode = new SyllabusManage(me);
			me.manageMode.init();

			me.editMode = new SyllabusEdit(me);
			me.editMode.init();

			onClick(me.ui.syllabus_view_edit, me.startManage);
		}
	};

	this.start = function()
	{
		me.startView();
	};

	this.startView = function()
	{
		if (!me.checkExit(function(){me.startView();})) return;
		me.mode([me.ui.syllabus_view, ((me.portal.site.role >= Role.instructor) ? me.ui.syllabus_view_edit : null)]);
		if (me.ui.modebar != null) me.ui.modebar.showSelected(0);
		me.viewMode.start();
	};

	this.startManage = function()
	{
		if (!me.checkExit(function(){me.startManage();})) return;
		me.mode([me.ui.syllabus_modebar, me.ui.syllabus_headerbar, me.ui.syllabus_bar_manage, me.ui.syllabus_manage]);
		if (me.ui.modebar != null) me.ui.modebar.showSelected(1);
		me.manageMode.start();
	};

	this.startEdit = function(section)
	{
		if (!me.checkExit(function(){me.startEdit(section);})) return;
		me.mode([me.ui.syllabus_modebar, me.ui.syllabus_headerbar, me.ui.syllabus_itemnav, me.ui.syllabus_bar_edit, me.ui.syllabus_edit]);
		if (me.ui.modebar != null) me.ui.modebar.showSelected(1);
		me.editMode.start(section);
	};

	this.mode = function(elements)
	{
		hide([me.ui.syllabus_modebar, me.ui.syllabus_headerbar, me.ui.syllabus_itemnav, 
		      me.ui.syllabus_view, me.ui.syllabus_view_edit,
		      me.ui.syllabus_manage, me.ui.syllabus_bar_manage, me.ui.syllabus_header_manage_sections,
		      me.ui.syllabus_edit, me.ui.syllabus_bar_edit]);
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

function SyllabusView(main)
{
	var me = this;

	this.ui = null;

	this.init = function()
	{
		me.ui = findElements(["syllabus_view_syllabus", "syllabus_view_none", "syllabus_view_none_published", 
		                      "syllabus_view_accept", "syllabus_view_accept_button", "syllabus_view_accepted", "syllabus_view_accepted_on"]);

		onClick(me.ui.syllabus_view_accept_button, me.accept);
	};

	this.start = function()
	{
		main.load(me.populate);
	};

	this.populate = function()
	{
		me.ui.syllabus_view_syllabus.empty();

		// if we have anything
		var something = false
		var published = false;

		if (main.syllabus.source == "E")
		{
			if (main.syllabus.external.url != null)
			{
				something = true;
				published = true;
				
				if (main.syllabus.external.target == "W")
				{
					var elements = clone(main.ui.syllabus_template_newWindow, ["syllabus_template_newWindow_body", "syllabus_template_newWindow_a"]);
					elements.syllabus_template_newWindow_a.attr("href", main.syllabus.external.url);
					me.ui.syllabus_view_syllabus.append(elements.syllabus_template_newWindow_body);
				}
				else
				{
					var elements = clone(main.ui.syllabus_template_inline, ["syllabus_template_inline_body"]);
					elements.syllabus_template_inline_body.attr("src", main.syllabus.external.url);
					if (main.syllabus.external.height != null) elements.syllabus_template_inline_body.css({height: main.syllabus.external.height});
					me.ui.syllabus_view_syllabus.append(elements.syllabus_template_inline_body);
				}
			}
		}

		else
		{
			$.each(main.syllabus.sections, function(index, section)
			{
				something = true;
				if (section.published)
				{
					var elements = clone(main.ui.syllabus_template_section, ["syllabus_template_section_body", "syllabus_template_section_title", "syllabus_template_section_content"]);
					elements.syllabus_template_section_title.text(section.title);
					elements.syllabus_template_section_content.html(section.content);
					me.ui.syllabus_view_syllabus.append(elements.syllabus_template_section_body);
					published = true;
				}
			});
		}

		if (something)
		{
			hide(me.ui.syllabus_view_none);
			show(me.ui.syllabus_view_none_published, !published)
		}
		else
		{
			show(me.ui.syllabus_view_none);
			hide(me.ui.syllabus_view_none_published);
		}

		me.adjustAcceptUI(published);
	};

	this.adjustAcceptUI = function(syllabusExists)
	{
		if (!syllabusExists)
		{
			hide(me.ui.syllabus_view_accept);
			return;
		}

		show(me.ui.syllabus_view_accept);
		if (main.portal.site.role == Role.student)
		{
			if (main.syllabus.accepted != null)
			{
				hide(me.ui.syllabus_view_accept_button);
				show(me.ui.syllabus_view_accepted);
				me.ui.syllabus_view_accepted_on.text(main.portal.timestamp.display(main.syllabus.accepted));
			}
			else
			{
				hide(me.ui.syllabus_view_accepted);
				show(me.ui.syllabus_view_accept_button);
				applyClass("e3_disabled", me.ui.syllabus_view_accept_button, false);
				me.ui.syllabus_view_accept_button.removeAttr("disabled");
			}
		}
		else
		{
			hide(me.ui.syllabus_view_accepted);
			show(me.ui.syllabus_view_accept_button);
			applyClass("e3_disabled", me.ui.syllabus_view_accept_button, true);
			me.ui.syllabus_view_accept_button.attr("disabled", true);
		}
	};

	this.accept = function()
	{
		if (main.portal.site.role == Role.student)
		{
			var params = main.portal.cdp.params();
			params.url.site = main.portal.site.id;
			main.cdp.request("syllabus_accept", params, function(data)
			{
				main.syllabus.accepted = data.accepted;
				me.adjustAcceptUI(true);
			});
		}
	}
}

function SyllabusManage(main)
{
	var me = this;
	
	this.ui = null;
	this.edit = null;
	this.reorder = null;

	this.init = function()
	{
		me.ui = findElements(["syllabus_manage_action_source", "syllabus_manage_action_add", "syllabus_manage_action_delete",
		                      "syllabus_manage_action_publish", "syllabus_manage_action_unpublish", "syllabus_manage_action_view",
		                      "syllabus_manage_internal", "syllabus_manage_table",
		                      "syllabus_manage_external", "syllabus_manage_external_url", "syllabus_manage_external_heightUi", "syllabus_manage_external_height"]);
		
		me.ui.table = new e3_Table(me.ui.syllabus_manage_table);
		me.ui.table.setupSelection("syllabus_manage_table_select", me.updateActions);
		me.ui.table.selectAllHeader(2, main.ui.syllabus_header_manage_sections);
		me.ui.table.enableReorder(me.applyOrder);

		onClick(me.ui.syllabus_manage_action_add, me.add);
		onClick(me.ui.syllabus_manage_action_delete, me.remove);
		onClick(me.ui.syllabus_manage_action_publish, me.publish);
		onClick(me.ui.syllabus_manage_action_unpublish, me.unpublish);
		onClick(me.ui.syllabus_manage_action_view, me.view);
		setupHoverControls([me.ui.syllabus_manage_action_add, me.ui.syllabus_manage_action_delete, me.ui.syllabus_manage_action_publish, me.ui.syllabus_manage_action_unpublish, me.ui.syllabus_manage_action_view]);		
	};

	this.start = function()
	{
		main.onExit = me.checkExit;
		main.load(function()
		{
			me.makeEdit();
			me.populate();
		});
	};

	this.makeEdit = function()
	{
		me.edit = new e3_Edit(main.syllabus, ["createdOn", "createdBy", "modifiedOn", "modifiedBy", "sections", "accepted", "id", "source"], function(changed)
		{
			main.ui.modebar.enableSaveDiscard(changed ? me.saveCancel : null);
		});
		me.edit.setFilters({"external.url": me.edit.urlFilter});

		main.ui.modebar.enableSaveDiscard(null);
	};

	this.populate = function()
	{
		// set source and adjust the UI section order
		me.ui.syllabus_manage_action_source.empty();
		me.ui.source = new e3_SortAction();
		me.ui.source.inject(me.ui.syllabus_manage_action_source,
			{onSort: function(dir, val){me.saveSource(val);}, label: main.i18n.lookup("label_source", "TYPE"),
			 options: [{value: "S", title: main.i18n.lookup("label_sections", "Sections")}, {value: "E", title: main.i18n.lookup("label_external", "External URL")}],
			 initial: main.syllabus.source});
		me.adjustUi(main.syllabus.source);
		
		// external info setup with edit
		if (main.syllabus.source == "E")
		{
			me.edit.setupFilteredEdit(me.ui.syllabus_manage_external_url, me.edit.external, "url"); // TODO: url filter		
			me.edit.setupRadioEdit("syllabus_manage_external_target", me.edit.external, "target", function(target){show(me.ui.syllabus_manage_external_heightUi, ((main.fs == 9) && (me.edit.external.target == "I")));});
			me.edit.setupFilteredEdit(me.ui.syllabus_manage_external_height, me.edit.external, "height"); // TODO: number filter 
			show(me.ui.syllabus_manage_external_heightUi, ((main.fs == 9) && (me.edit.external.target == "I")));
		}
	
		else
		{
			// section info
			me.ui.table.clear();
			$.each(main.syllabus.sections, function(index, section)
			{
				me.ui.table.row();
	
				me.ui.table.reorder(main.i18n.lookup("msg_reorder", "Drag to Reorder"), section.id);
				me.ui.table.selectBox(section.id);
	
				if (!section.published)
				{
					me.ui.table.dot("red",  main.i18n.lookup("msg_unpublished", "Unpublished"));
				}
				else
				{
					me.ui.table.dot("green",  main.i18n.lookup("msg_published", "Published"));
				}
	
				me.ui.table.hotText(section.title, main.i18n.lookup("msg_edit", "Edit '%0'", "html", [section.title]), function(){main.startEdit(section);}, null, {width: "calc(100vw - 100px - 184px)", minWidth:"calc(1200px - 100px - 184px)"});

				me.ui.table.contextMenu(
				[
					{title: main.i18n.lookup("cm_preview", "preview"), action:function(){me.viewA(section);}},
					{title: main.i18n.lookup("cm_edit", "Edit"), action:function(){main.startEdit(section);}},
					{title: main.i18n.lookup("cm_publish", "Publish"), action:function(){me.publishA(section);}},
					{title: main.i18n.lookup("cm_unpublish", "Unpublish"), action:function(){me.unpublishA(section);}},
					{title: main.i18n.lookup("cm_delete", "Delete"), action:function(){me.removeA(section);}}
		        ]);
			});
			
			me.ui.table.done();
		}
	};

	this.adjustUi = function(source)
	{
		show([me.ui.syllabus_manage_external, me.ui.syllabus_manage_action_view], (source == "E"));
		if (source == "E") enableAction(me.ui.syllabus_manage_action_view);
		show([me.ui.syllabus_manage_internal, main.ui.syllabus_header_manage_sections,
		      me.ui.syllabus_manage_action_add.parent(), me.ui.syllabus_manage_action_publish.parent(),
		      me.ui.syllabus_manage_action_unpublish.parent(), me.ui.syllabus_manage_action_delete.parent()], (source == "S"));
	};

	this.updateActions = function()
	{
		me.ui.table.updateActions([me.ui.syllabus_manage_action_view], [me.ui.syllabus_manage_action_delete, me.ui.syllabus_manage_action_publish, me.ui.syllabus_manage_action_unpublish]);		
	};

	this.add = function()
	{
		var section = {id:-1, title:" ", content:"<p />", isPublic:false, published:false, order:main.syllabus.sections.length+1}
		main.syllabus.sections.push(section);
		main.startEdit(section);
	};

	this.remove = function()
	{
		var params = main.portal.cdp.params();
		params.url.site = main.portal.site.id;
		params.post.ids = me.ui.table.selected();

		if (params.post.ids.length == 0)
		{
			main.portal.dialogs.openAlert(main.ui.syllabus_selectFirst);
			return;
		}

		main.portal.dialogs.openConfirm(main.ui.syllabus_confirmDelete, main.i18n.lookup("action_delete", "Delete"), function()
		{
			main.portal.cdp.request("syllabus_remove syllabus_get", params, function(data)
			{
				main.syllabus = data.syllabus;
				me.populate();
			});

			return true;
		}, function(){me.ui.table.clearSelection();});
	};

	this.removeA = function(section)
	{
		var params = main.portal.cdp.params();
		params.url.site = main.portal.site.id;
		params.post.ids = [section.id];

		main.portal.dialogs.openConfirm(main.ui.syllabus_confirmDelete, main.i18n.lookup("action_delete", "Delete"), function()
		{
			main.portal.cdp.request("syllabus_remove syllabus_get", params, function(data)
			{
				main.syllabus = data.syllabus;
				me.populate();
			});

			return true;
		}, function(){me.ui.table.clearSelection();});
	};

	this.publish = function()
	{
		var params = main.portal.cdp.params();
		params.url.site = main.portal.site.id;
		params.post.ids = me.ui.table.selected();

		if (params.post.ids.length == 0)
		{
			main.portal.dialogs.openAlert(main.ui.syllabus_selectFirst);
			return;
		}

		main.portal.cdp.request("syllabus_publish syllabus_get", params, function(data)
		{
			main.syllabus = data.syllabus;
			me.populate();
		});
	};

	this.publishA = function(section)
	{
		var params = main.portal.cdp.params();
		params.url.site = main.portal.site.id;
		params.post.ids = [section.id];

		main.portal.cdp.request("syllabus_publish syllabus_get", params, function(data)
		{
			main.syllabus = data.syllabus;
			me.populate();
		});
	};

	this.unpublish = function()
	{
		var params = main.portal.cdp.params();
		params.url.site = main.portal.site.id;
		params.post.ids = me.ui.table.selected();

		if (params.post.ids.length == 0)
		{
			main.portal.dialogs.openAlert(main.ui.syllabus_selectFirst);
			return;
		}

		main.portal.cdp.request("syllabus_unpublish syllabus_get", params, function(data)
		{
			main.syllabus = data.syllabus;
			me.populate();
		});
	};

	this.unpublishA = function(section)
	{
		var params = main.portal.cdp.params();
		params.url.site = main.portal.site.id;
		params.post.ids = [section.id];

		main.portal.cdp.request("syllabus_unpublish syllabus_get", params, function(data)
		{
			main.syllabus = data.syllabus;
			me.populate();
		});
	};

	this.applyOrder = function(order)
	{
		main.ui.modebar.enableSaveDiscard(me.saveCancel);
		me.reorder = order;
	};

	this.saveCancel = function(mode, deferred)
	{
		if (mode)
		{
			me.save(deferred);
		}
		else
		{
			me.reorder = null;
			me.edit.revert();
			me.populate();
			if (deferred !== undefined) deferred();
		}
	};

	this.checkExit = function(deferred)
	{
		if (me.edit.changed() || (me.reorder != null))
		{
			main.portal.confirmNavigationWithChanges(function()
			{
				me.save(deferred);				
			}, function()
			{
				me.edit.revert();
				me.reorder = null;
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

		var cmd = null;

		if (me.reorder != null)
		{
			params.post.order = me.reorder;
			cmd = "syllabus_order";
		}
		else
		{
			me.edit.params("", params);
			cmd = "syllabus_save";
		}

		main.portal.cdp.request(cmd + " syllabus_get", params, function(data)
		{
			main.syllabus = data.syllabus;
			me.reorder = null;
			me.makeEdit();
			me.populate();
			if (deferred !== undefined) deferred();
		});
	};

	this.saveSource = function(val)
	{
		if (!me.checkExit(function(){me.saveSource(val);})) return;

		var params = main.portal.cdp.params();
		params.url.site = main.portal.site.id;
		params.post.source = val;
		main.portal.cdp.request("syllabus_saveSource syllabus_get", params, function(data)
		{
			main.syllabus = data.syllabus;
			me.makeEdit();
			me.populate();
		});
	};

	this.view = function()
	{
		if (main.syllabus.source == "E")
		{
			console.log("view external");
		}
		else
		{
			var ids = me.ui.table.selected();
			if (ids == 0)
			{
				main.portal.dialogs.openAlert(main.ui.syllabus_selectFirst);
				return;
			}
			
			var section = main.findSection(ids[0]);
			if (section == null) return;
			console.log("view section", section);
		}
	};

	this.viewA = function(section)
	{
		console.log("view section", section);
	};
}

function SyllabusEdit(main)
{
	var me = this;
	
	this.ui = null;

	this.section = null
	this.edit = null;

	this.init = function()
	{
		me.ui = findElements(["syllabus_edit_view", "syllabus_edit_title", "syllabus_edit_content_editor", "syllabus_edit_published", "syllabus_edit_public"]);
		me.ui.itemNav = new e3_ItemNav();
		me.ui.editor = new e3_EditorCK(me.ui.syllabus_edit_content_editor, {height: 350});

		onClick(me.ui.syllabus_edit_view, me.view);
		setupHoverControls([me.ui.syllabus_edit_view]);
	};
	
	this.start = function(section)
	{
		me.section = section;
		main.onExit = me.checkExit;
		me.ui.itemNav.inject(main.ui.syllabus_itemnav, {doneFunction:me.done, pos:main.sectionPosition(me.section), navigateFunction:me.goSection});
		me.makeEdit();
		me.populate();
	};
	
	this.makeEdit = function()
	{
		me.edit = new e3_Edit(me.section, ["createdOn", "createdBy", "modifiedOn", "modifiedBy", "id"], function(changed)
		{
			main.ui.modebar.enableSaveDiscard(changed ? me.saveCancel : null);
			me.ui.itemNav.enableSave(changed ? me.saveCancel : null);
		});

		main.ui.modebar.enableSaveDiscard(null);
		me.ui.itemNav.enableSave(null);
	};

	this.populate = function()
	{
		me.edit.setupFilteredEdit(me.ui.syllabus_edit_title, me.edit, "title");

		me.ui.editor.disable();
		me.ui.editor.set(me.edit.content);
		me.ui.editor.enable(function()
		{
			me.edit.set(me.edit, "content", me.ui.editor.get());
		}, false /* no focus */, main.fs);
		me.edit.setupCheckEdit(me.ui.syllabus_edit_published, me.edit, "published");
		me.edit.setupCheckEdit(me.ui.syllabus_edit_public, me.edit, "isPublic");
	};

	this.done = function()
	{
		if (me.edit.changed())
		{
			me.save(function(){main.startManage();})
		}
		else
		{
			// if just added, forget about it
			me.undoUnusedAddSection();

			main.startManage();
		}
	};

	this.goSection = function(section)
	{
		main.startEdit(section);
	};

	this.view = function()
	{
		console.log("view");
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

	this.undoUnusedAddSection = function()
	{
		// if just added, forget about it
		if ((me.section != null) && (me.section.id  < 0))
		{
			main.syllabus.sections.splice(main.syllabus.sections.length-1, 1);
			me.section = null;
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
				// if just added, forget about it
				me.undoUnusedAddSection();
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
		params.url.section = me.section.id;
		me.edit.params("", params);
		main.portal.cdp.request("syllabus_saveSection syllabus_get", params, function(data)
		{
			main.syllabus = data.syllabus;
			me.section = main.findSection(me.section.id);
			if (me.section == null) me.section = main.syllabus.sections[main.syllabus.sections.length-1]; // if it was new, it will be the last one
			me.makeEdit();
			me.populate();
			if (deferred !== undefined) deferred();
		});
	};
}

$(function()
{
	try
	{
		syllabus_tool = new Syllabus();
		syllabus_tool.init();
		syllabus_tool.start();
	}
	catch (e)
	{
		error(e);
	}
});

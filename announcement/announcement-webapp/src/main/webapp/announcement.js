/**********************************************************************************
 * $URL: https://source.etudes.org/svn/serenity/trunk/announcement/announcement-webapp/src/main/webapp/announcement.js $
 * $Id: announcement.js 12504 2016-01-10 00:30:08Z ggolden $
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

var announcement_tool = null;

function Announcement()
{
	var me = this;

	this.portal = null;
	this.i18n = new e3_i18n(announcement_i10n, "en-us");

	this.view = null;
	this.manage = null;
	this.edit = null;

	this.ui = null;
	this.onExit = null;
	this.fs = 9; // 0 for homepage filesystem, 1 for CHS/resources file system, 9 for serenity fs

	this.announcements = null;

	this.announcementPosition = function(annc)
	{
		for (index = 0; index < me.announcements.length; index++)
		{
			if (me.announcements[index].id == annc.id)
			{
				var rv = {};
				rv.item = index+1;
				rv.total = me.announcements.length;
				rv.prev = index > 0 ? me.announcements[index-1] : null;
				rv.next = index < me.announcements.length-1 ? me.announcements[index+1] : null;
				
				return rv;
			}			
		}
	};

	this.findAnnouncement = function(id)
	{
		for (index = 0; index < me.announcements.length; index++)
		{
			if (me.announcements[index].id == id)
			{
				return me.announcements[index];
			}
		}
		
		return null;
	};

	this.viewCount = function()
	{
		var count = 0;
		$.each(me.announcements, function(index, annc)
		{
			if (annc.released) count++;
		});

		return count;
	};

	this.loadAnnouncements = function(onLoad)
	{
		var params = me.portal.cdp.params();
		params.url.site = me.portal.site.id;
		me.portal.cdp.request("announcement_get", params, function(data)
		{
			me.announcements = data.announcements;
			me.fs = data.fs;
			onLoad();
		});
	};

	this.init = function()
	{		
		me.i18n.localize();
		
		me.ui = findElements(["annc_header", "annc_modebar", "annc_headerbar", "annc_itemnav",
		                      "annc_view",  "annc_view_edit",
		                      "annc_manage", "annc_bar_manage", "annc_header_manage",
		                      "annc_edit", "annc_bar_edit",
		                      "annc_selectFirst"]);
		me.portal = portal_tool.features({onExit:function(deferred){return me.checkExit(deferred);}, pin:[{ui:me.ui.annc_header}]});
		
		me.view = new Announcement_view(me);
		me.view.init();

		if (me.portal.site.role >= Role.instructor)
		{
			me.manage = new Announcement_manage(me);
			me.manage.init();

			me.ui.modebar = new e3_Modebar(me.ui.annc_modebar);
			me.modes =
			[
				{name:me.i18n.lookup("mode_view", "View"), func:function(){me.startView();}},
				{name:me.i18n.lookup("mode_edit", "Manage"), func:function(){me.startManage();}}
			];
			me.ui.modebar.set(me.modes, 0);
			
			me.edit = new Announcement_edit(me);
			me.edit.init();
			
			onClick(me.ui.annc_view_edit, me.startManage);
		}
	};

	this.start = function()
	{
		if (me.portal.site.role >= Role.instructor)
		{
			me.startManage();
		}
		else
		{
			me.startView();
		}
	};

	this.startView = function()
	{
		if (!me.checkExit(function(){me.startView();})) return;
		me.mode([me.ui.annc_view, ((me.portal.site.role >= Role.instructor) ? me.ui.annc_view_edit : null)]);
		if (me.ui.modebar !== undefined) me.ui.modebar.showSelected(0);
		me.view.start();
	};

	this.startManage = function()
	{
		if (!me.checkExit(function(){me.startManage();})) return;
		me.mode([me.ui.annc_modebar, me.ui.annc_headerbar, me.ui.annc_bar_manage, me.ui.annc_header_manage, me.ui.annc_manage]);
		me.ui.modebar.showSelected(1);
		me.manage.start();
	};

	this.startEdit = function(annc)
	{
		if (!me.checkExit(function(){me.startEdit(annc);})) return;
		me.mode([me.ui.annc_modebar, me.ui.annc_headerbar, me.ui.annc_itemnav, me.ui.annc_bar_edit, me.ui.annc_edit]);
		me.edit.start(annc);
	}

	this.mode = function(elements)
	{
		hide([me.ui.annc_modebar, me.ui.annc_headerbar, me.ui.annc_itemnav,
		      me.ui.annc_view, me.ui.annc_view_edit,
		      me.ui.annc_manage,  me.ui.annc_bar_manage, me.ui.annc_header_manage,
		      me.ui.annc_edit, me.ui.annc_bar_edit]);
		me.onExit = null;
		me.portal.resetScrolling();
		show(elements);
	};
	
	this.checkExit = function(deferred)
	{
		if (me.onExit == null) return true;
		return me.onExit(deferred);
	};
	
	this.comparePublished = function(a, b)
	{
		if (a.published == b.published) return 0;
		if (a.published) return -1;
		return 1;
	};
	
	this.compareDrafts = function(a, b)
	{
		var rv = -1 * compareN(a.modifiedOn, b.modifiedOn);
		if (rv == 0)
		{
			rv = compareS(a.title, b.title);
			if (rv == 0)
			{
				rv = compareN(a.id, b.id);
			}
		}
		return rv;
	};

	this.sortByOrder = function()
	{
		// if we don't have order defined, switch to a by-modified, descending sort
		var hasOrder = true;
		for (var i = 0; i < me.announcements.length; i++)
		{
			if (me.announcements[i].published && (me.announcements[i].order == null))
			{
				hasOrder = false;
				break;
			}
		}
		if (!hasOrder)
		{
			return me.sortByModified('D');
		}

		var sorted = [].concat(me.announcements);
		sorted.sort(function(a, b)
		{
			var rv = me.comparePublished(a, b);
			if (rv == 0)
			{
				if (a.published) // b will be, also
				{
					var rv = compareN(a.order, b.order);				
					if (rv == 0)
					{
						rv = compareS(a.title, b.title);
						if (rv == 0)
						{
							rv = compareN(a.id, b.id);
						}
					}
				}
				else rv = me.compareDrafts(a, b);
			}
			return rv;
		});

		return sorted;
	};

	this.sortByModified = function(direction)
	{
		var sorted = [].concat(me.announcements);
		var adjust = (direction == 'A') ? 1 : -1;
		sorted.sort(function(a, b)
		{
			var rv = me.comparePublished(a, b);
			if (rv == 0)
			{
				if (a.published) // b will be, also
				{
					rv = adjust * compareN(a.modifiedOn, b.modifiedOn);
					if (rv == 0)
					{
						rv = compareS(a.title, b.title);
						if (rv == 0)
						{
							rv = compareN(a.id, b.id);
						}
					}
				}
				else rv = me.compareDrafts(a, b);
			}
			return rv;
		});

		return sorted;
	};

	this.sortByRelease = function(direction)
	{
		var sorted = [].concat(me.announcements);
		var adjust = (direction == 'A') ? 1 : -1;
		sorted.sort(function(a, b)
		{
			var rv = me.comparePublished(a, b);
			if (rv == 0)
			{
				if (a.published) // b will be, also
				{
					rv = adjust * compareN(a.releaseDate, b.releaseDate);
					if (rv == 0)
					{
						rv = compareS(a.title, b.title);
						if (rv == 0)
						{
							rv = compareN(a.id, b.id);
						}
					}
				}
				else rv = me.compareDrafts(a, b);
			}
			return rv;
		});

		return sorted;
	};
}

function Announcement_manage(main)
{
	var me = this;
	this.ui = null;
	this.sortDirection = "A";
	this.sortMode = "P";
	this.showReorder = true;
	this.reorder = null;
	this.itemsSorted = null;

	this.init = function()
	{
		me.ui = findElements(["annc_manage_actions", "annc_manage_table", "annc_manage_add", "annc_manage_delete", "annc_manage_publish", "annc_manage_unpublish", "annc_manage_view", "annc_manage_none"]);

		me.ui.table = new e3_Table(me.ui.annc_manage_table);
		me.ui.table.setupSelection("annc_table_select", me.updateActions);
		me.ui.table.selectAllHeader(2, main.ui.annc_header_manage);

		me.ui.sort = new e3_SortAction();
		me.ui.sort.inject(me.ui.annc_manage_actions,
				{onSort: me.onSort, options:[{value:"P", title:main.i18n.lookup("sort_presentation", "Display Order")},
				                             {value:"M", title:main.i18n.lookup("sort_modified", "Modified Date")},
				                             {value:"R", title:main.i18n.lookup("sort_release", "Release Date")}]});

		onClick(me.ui.annc_manage_add, me.add);
		onClick(me.ui.annc_manage_delete, me.remove);
		onClick(me.ui.annc_manage_publish, me.publish);
		onClick(me.ui.annc_manage_unpublish, me.unpublish);
		onClick(me.ui.annc_manage_view, me.view);
		setupHoverControls([me.ui.annc_manage_add, me.ui.annc_manage_delete, me.ui.annc_manage_publish, me.ui.annc_manage_unpublish, me.ui.annc_manage_view]);
	};

	this.start = function()
	{
		main.onExit = me.checkExit;
		main.loadAnnouncements(function()
		{
			me.sortBy(me.sortDirection, me.sortMode);
			me.populate();
		});
	};

	this.populate = function()
	{
		me.ui.table.enableReorder((me.showReorder ? me.applyOrder : null));

		var addedDraftsHeader = false;
		me.ui.table.clear();
		$.each(me.itemsSorted, function(index, annc)
		{
			me.ui.table.row();

			if ((!annc.published) && (!addedDraftsHeader))
			{
				me.ui.table.headerRow(main.i18n.lookup("msg_unpublisheds", "UNPUBLISHED"));
				me.ui.table.row();
				addedDraftsHeader = true;
			}

			if (me.showReorder && (annc.published))
			{
				me.ui.table.reorder(main.i18n.lookup("msg_reorder", "drag to reorder"), annc.id);
			}
			else
			{
				me.ui.table.text("", "icon");
				me.ui.table.disableRowReorder();
			}

			me.ui.table.selectBox(annc.id);

			if (!annc.published)
			{
				me.ui.table.dot("red",  main.i18n.lookup("msg_unpublished", "unpublished"));
			}
			else if (annc.released)
			{
				me.ui.table.dot("green",  main.i18n.lookup("msg_released", "released"));
			}
			else
			{
				me.ui.table.dot("gray",  main.i18n.lookup("msg_unreleased", "not released"));
			}

			me.ui.table.hotText(annc.title, main.i18n.lookup("msg_edit", "Edit '%0'", "html", [annc.title]), function(){main.startEdit(annc);}, null, {width:"calc(100vw - 100px - 500px)", minWidth: "calc(1200px - 100px - 500px)"});
			me.ui.table.date(annc.modifiedOn);
			if ((annc.releaseDate != null) && (annc.published))
			{
				me.ui.table.date(annc.releaseDate);
			}
			else
			{
				me.ui.table.text(/*main.i18n.lookup("msg_publication", "publication")*/"", "e3_italic date");
			}
			
			me.ui.table.contextMenu(
			[
				{title: main.i18n.lookup("cm_preview", "Preview"), action:function(){me.viewA(annc);}},
				{title: main.i18n.lookup("cm_edit", "Edit"), action:function(){main.startEdit(annc);}},
				{title: main.i18n.lookup("cm_publish", "Publish"), action:function(){me.publishA(annc);}},
				{title: main.i18n.lookup("cm_unpublish", "Unpublish"), action:function(){me.unpublishA(annc);}},
				{title: main.i18n.lookup("cm_delete", "Delete"), action:function(){me.removeA(annc);}}
	        ]);
		});
		
		me.ui.table.done();
		show(me.ui.annc_manage_none, (me.ui.table.rowCount() == 0));
	};

	this.onSort = function(direction, option)
	{
		if ((main.onExit != null) && (!main.onExit(function(){me.onSort(direction, option);}))) return;

		me.sortBy(direction, option);
		me.populate();
	};

	this.sortBy = function(direction, option)
	{
		me.sortDirection = direction;
		me.sortMode = option;
		me.showReorder = (me.sortMode == "P");
		me.ui.sort.directional(me.sortMode != "P");

		if (me.sortMode == "P")
		{
			me.itemsSorted = main.sortByOrder();
		}
		else if (me.sortMode == "M")
		{
			me.itemsSorted = main.sortByModified(direction);
		}
		else if (me.sortMode == "R")
		{
			me.itemsSorted = main.sortByRelease(direction);
		}
	};

	this.updateActions = function()
	{
		me.ui.table.updateActions([me.ui.annc_manage_view], [me.ui.annc_manage_delete, me.ui.annc_manage_publish,me.ui.annc_manage_unpublish]);		
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
			main.ui.modebar.enableSaveDiscard(null);
			me.sortBy(me.sortDirection, me.sortMode);
			me.populate();
			if (deferred !== undefined) deferred();
		}
	};
	
	this.checkExit = function(deferred)
	{
		if (me.reorder != null)
		{
			main.portal.confirmNavigationWithChanges(function()
			{
				me.save(deferred);				
			}, function()
			{
				me.reorder = null;
				main.ui.modebar.enableSaveDiscard(null);
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

		params.post.order = me.reorder;
		main.portal.cdp.request("announcement_order announcement_get", params, function(data)
		{
			main.announcements = data.announcements || [];
			me.reorder = null;
			main.ui.modebar.enableSaveDiscard(null);
			me.sortBy(me.sortDirection, me.sortMode);
			me.populate();
			if (deferred !== undefined) deferred();
		});
	};
	
	this.add = function()
	{
		if ((main.onExit != null) && (!main.onExit(function(){me.add();}))) return;

		var annc = {id:-1, subject:" ", content:"<p />", published:false, isPublic: false, release:null, order:main.announcements.length+1, createdBy: main.portal.user.nameDisplay};
		main.announcements.push(annc);
		main.startEdit(annc);
	};
	
	this.remove = function()
	{
		if ((main.onExit != null) && (!main.onExit(function(){me.remove();}))) return;

		var params = main.portal.cdp.params();
		params.url.site = main.portal.site.id;
		params.post.ids = me.ui.table.selected();

		if (params.post.ids.length == 0)
		{
			main.portal.dialogs.openAlert(main.ui.annc_selectFirst);
			return;
		}

		main.portal.dialogs.openConfirm("annc_confirmDelete", main.i18n.lookup("action_delete", "Delete"), function()
		{
			main.portal.cdp.request("announcement_remove announcement_get", params, function(data)
			{
				main.announcements = data.announcements || [];
				me.sortBy(me.sortDirection, me.sortMode);
				me.populate();
			});

			return true;
		}, function(){me.ui.table.clearSelection();});
	};

	this.removeA = function(annc)
	{
		if ((main.onExit != null) && (!main.onExit(function(){me.removeA(annc);}))) return;

		var params = main.portal.cdp.params();
		params.url.site = main.portal.site.id;
		params.post.ids = [annc.id];

		main.portal.dialogs.openConfirm("annc_confirmDelete", main.i18n.lookup("action_delete", "Delete"), function()
		{
			main.portal.cdp.request("announcement_remove announcement_get", params, function(data)
			{
				main.announcements = data.announcements || [];
				me.sortBy(me.sortDirection, me.sortMode);
				me.populate();
			});

			return true;
		}, function(){me.ui.table.clearSelection();});
	};

	this.publish = function()
	{
		if ((main.onExit != null) && (!main.onExit(function(){me.publish();}))) return;

		var params = main.portal.cdp.params();
		params.url.site = main.portal.site.id;
		params.post.ids = me.ui.table.selected();

		if (params.post.ids.length == 0)
		{
			main.portal.dialogs.openAlert(main.ui.annc_selectFirst);
			return;
		}

		main.portal.cdp.request("announcement_publish announcement_get", params, function(data)
		{
			main.announcements = data.announcements || [];
			me.sortBy(me.sortDirection, me.sortMode);
			me.populate();
		});
	};

	this.publishA = function(annc)
	{
		if ((main.onExit != null) && (!main.onExit(function(){me.publishA(annc);}))) return;

		var params = main.portal.cdp.params();
		params.url.site = main.portal.site.id;
		params.post.ids = [annc.id];

		main.portal.cdp.request("announcement_publish announcement_get", params, function(data)
		{
			main.announcements = data.announcements || [];
			me.sortBy(me.sortDirection, me.sortMode);
			me.populate();
		});
	};

	this.unpublish = function()
	{
		if ((main.onExit != null) && (!main.onExit(function(){me.unpublish();}))) return;

		var params = main.portal.cdp.params();
		params.url.site = main.portal.site.id;
		params.post.ids = me.ui.table.selected();

		if (params.post.ids.length == 0)
		{
			main.portal.dialogs.openAlert(main.ui.annc_selectFirst);
			return;
		}

		main.portal.cdp.request("announcement_unpublish announcement_get", params, function(data)
		{
			main.announcements = data.announcements || [];
			me.sortBy(me.sortDirection, me.sortMode);
			me.populate();
		});
	};
	
	this.unpublishA = function(annc)
	{
		if ((main.onExit != null) && (!main.onExit(function(){me.unpublishA(annc);}))) return;

		var params = main.portal.cdp.params();
		params.url.site = main.portal.site.id;
		params.post.ids = [annc.id];

		main.portal.cdp.request("announcement_unpublish announcement_get", params, function(data)
		{
			main.announcements = data.announcements || [];
			me.sortBy(me.sortDirection, me.sortMode);
			me.populate();
		});
	};

	this.view = function()
	{
		var ids = me.ui.table.selected();
		if (ids == 0)
		{
			main.portal.dialogs.openAlert(main.ui.annc_selectFirst);
			return;
		}
		
		var annc = main.findAnnouncement(ids[0]);
		if (annc == null) return;
		
		main.edit.ui.annc_edit_view_annc_subject.text(annc.title);
		main.edit.ui.annc_edit_view_annc_byline.text(annc.createdBy);
		main.edit.ui.annc_edit_view_annc_bylineDate.text(main.portal.timestamp.display(annc.bylineDate));
		main.edit.ui.annc_edit_view_annc_content.html(annc.content);

		main.portal.dialogs.openAlert(main.edit.ui.annc_edit_view_annc);
	};

	this.viewA = function(annc)
	{
		main.edit.ui.annc_edit_view_annc_subject.text(annc.title);
		main.edit.ui.annc_edit_view_annc_byline.text(annc.createdBy);
		main.edit.ui.annc_edit_view_annc_bylineDate.text(main.portal.timestamp.display(annc.bylineDate));
		main.edit.ui.annc_edit_view_annc_content.html(annc.content);

		main.portal.dialogs.openAlert(main.edit.ui.annc_edit_view_annc);
	};
}

function Announcement_edit(main)
{
	var me = this;
	
	this.ui = null;

	this.announcement = null;
	this.edit = null;

	this.init = function()
	{
		me.ui = findElements(["annc_edit_subject", "annc_edit_content", "annc_edit_releaseDate", "annc_edit_published", "annc_edit_public", "annc_edit_view",
		                      "annc_edit_view_annc", "annc_edit_view_annc_subject", "annc_edit_view_annc_byline", "annc_edit_view_annc_bylineDate", "annc_edit_view_annc_content", "annc_edit_attribution"]);
		me.ui.itemNav = new e3_ItemNav();
		me.ui.editor = new e3_EditorCK(me.ui.annc_edit_content, {height: 350});

		onClick(me.ui.annc_edit_view, me.view);
		setupHoverControls([me.ui.annc_edit_view]);
	};

	this.start = function(annc)
	{
		if (annc === null) return;
		if (annc !== undefined) me.announcement = annc;

		main.onExit = me.checkExit;
		me.ui.itemNav.inject(main.ui.annc_itemnav, {doneFunction:me.done, pos:main.announcementPosition(me.announcement), navigateFunction:me.goAnnc});

		me.makeEdit();
		me.populate();
	};

	this.makeEdit = function()
	{
		me.edit = new e3_Edit(me.announcement, ["createdOn", "createdBy", "modifiedOn", "modifiedBy", "id"], function(changed)
		{
			main.ui.modebar.enableSaveDiscard(changed ? me.saveCancel : null);
			me.ui.itemNav.enableSave(changed ? me.saveCancel : null);
		});

		main.ui.modebar.enableSaveDiscard(null);
		me.ui.itemNav.enableSave(null);
	};

	this.populate = function()
	{
		me.edit.setupFilteredEdit(me.ui.annc_edit_subject, me.edit, "title");

		me.ui.editor.disable();
		me.ui.editor.set(me.edit.content);
		me.ui.editor.enable(function()
		{
			me.edit.set(me.edit, "content", me.ui.editor.get());
		}, false /* no focus */, main.fs);

		me.edit.setupDateEdit(me.ui.annc_edit_releaseDate, me.edit, "releaseDate", main.portal.timestamp, true);
		me.edit.setupCheckEdit(me.ui.annc_edit_published, me.edit, "published");
		me.edit.setupCheckEdit(me.ui.annc_edit_public, me.edit, "isPublic");

 		if (me.announcement.id != -1) new e3_Attribution().inject(me.ui.annc_edit_attribution, me.announcement);
 		show(me.ui.annc_edit_attribution, (me.announcement.id != -1));
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

	this.done = function()
	{
		// save if changed
		if (me.edit.changed())
		{
			me.save(function(){main.startManage();});
		}
		else
		{
			main.startManage();
		}
	};

	this.goAnnc = function(annc)
	{
		main.startEdit(annc);
	};

	this.save = function(deferred)
	{
		// save this edited annc
		var params = main.portal.cdp.params();
		params.url.site = main.portal.site.id;
		params.url.announcement = me.announcement.id;
		me.edit.params("", params);

		main.portal.cdp.request("announcement_save announcement_get", params, function(data)
		{
			main.announcements = data.announcements || [];
			me.announcement = main.findAnnouncement(me.announcement.id);
			if (me.announcement == null) me.announcement = main.announcements[main.announcements.length-1]; // if it was new, it will be the last one
			me.makeEdit();
			me.populate();
			if (deferred !== undefined) deferred();
		});
	};

	this.view = function()
	{
		me.ui.annc_edit_view_annc_subject.text(me.edit.title);
		me.ui.annc_edit_view_annc_byline.text(me.announcement.createdBy);
		me.ui.annc_edit_view_annc_bylineDate.text(main.i18n.lookup("msg_releaseDate", "release date"));
		me.ui.annc_edit_view_annc_content.html(me.edit.content);

		main.portal.dialogs.openAlert(me.ui.annc_edit_view_annc);
	};
}

function Announcement_view(main)
{
	var me = this;

	this.ui = null;
	this.itemsSorted = null;

	this.init = function()
	{
		me.ui = findElements(["annc_view_list", "annc_view_table", "annc_view_none", 
		                      "annc_view_annc", "annc_view_annc_subject", "annc_view_annc_byline", "annc_view_annc_content"]);		
		me.ui.table = new e3_Table(me.ui.annc_view_table);
	};

	this.start = function()
	{
		main.loadAnnouncements(function()
		{
			me.itemsSorted = main.sortByOrder();
			me.populate();
		});
	};

	this.populate = function()
	{
		me.ui.table.clear();
		var count = 0;
		$.each(main.announcements, function(index, annc)
		{
			if (annc.released)
			{
				me.ui.table.hotRow(function(){me.populateAnnouncement(annc);});
				count++;

				me.ui.table.text(annc.title, null, {width:"calc(100vw - 100px - 446px)", minWidth:"calc(1200px - 100px - 446px)"});
				me.ui.table.text(annc.createdBy, null, {width:200});
				me.ui.table.date(annc.bylineDate);
			}
		});

		show(me.ui.annc_view_none, count == 0);
		if (count > 0)
		{
			me.ui.annc_view_table.find("tbody>tr:first").click();
		}
		show(me.ui.annc_view_annc, count > 0);
	};
	
	this.populateAnnouncement = function(annc)
	{
		me.ui.annc_view_annc_subject.text(annc.title);
		me.ui.annc_view_annc_byline.text(main.i18n.lookup("msg_byline", "%0, %1", "html", [annc.createdBy, main.portal.timestamp.display(annc.bylineDate)]));
		me.ui.annc_view_annc_content.html(annc.content);
	};
}

$(function()
{
	try
	{
		announcement_tool = new Announcement();
		announcement_tool.init();
		announcement_tool.start();
	}
	catch (e)
	{
		error(e);
	}
});

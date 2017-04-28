/**********************************************************************************
 * $URL: https://source.etudes.org/svn/serenity/trunk/user/user-webapp/src/main/webapp/user.js $
 * $Id: user.js 12504 2016-01-10 00:30:08Z ggolden $
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

var user_tool = null;

function UserManage(main)
{
	var me = this;

	this.ui = null;

	this.total = 0;
	this.paging = {pageNum:1, pageSize:50};

	this.init = function()
	{
		me.ui = findElements(["user_manage_actions", "user_manage_table", "user_manage_add", "user_manage_purge"]);

		me.ui.table = new e3_Table(me.ui.user_manage_table);
		me.ui.table.setupSelection("user_table_select", me.updateActions);
		me.ui.table.selectAllHeader(1, main.ui.user_header_manage);

		onClick(me.ui.user_manage_add, me.add);
		onClick(me.ui.user_manage_purge, me.purge);
		setupHoverControls([me.ui.user_manage_add, me.ui.user_manage_purge]);

		// setup the actions
		onClick(me.ui.user_manage_add, me.add);
		onClick(me.ui.user_manage_purge, me.purge);

//		$('input:radio[name=user_searchOn][value="3"]').prop('checked', true);
//		$("#user_search").attr("placeholder", main.i18n.lookup("ph_name", "Partial Last, First Name"));
//		onClick("user_searchGo", me.search);
//		onClick("user_searchClear", me.clear);
//
//		onClick("user_searchOn_0", function(){$("#user_search").attr("placeholder", main.i18n.lookup("ph_iid", "Full IID"));}, true);
//		onClick("user_searchOn_1", function(){$("#user_search").attr("placeholder", main.i18n.lookup("ph_loginId", "Full Login ID"));}, true);
//		onClick("user_searchOn_2", function(){$("#user_search").attr("placeholder", main.i18n.lookup("ph_email", "Full email"));}, true);
//		onClick("user_searchOn_3", function(){$("#user_search").attr("placeholder", main.i18n.lookup("ph_name", "Partial Last, First Name"));}, true);
	};

	this.start = function()
	{
		me.load();
	};

	this.load = function()
	{
		var params = main.portal.cdp.params();
		
//		var value = trim($("#user_search").val());
//		if (value != null)
//		{
//			params.post.searchType = $('input:radio[name=user_searchOn]:checked').val();
//			params.post.search = value;
//		}
//		params.post.pageNum = me.paging.pageNum;
//		params.post.pageSize = me.paging.pageSize;

		main.portal.cdp.request("user_get", params, function(data)
		{
			main.users = data.users || [];

//			me.total = data.total || 0;
//			me.setPaging();
			me.populate();
		});
	};

	this.updateActions = function()
	{
		me.ui.table.updateActions([], [me.ui.user_manage_purge]);		
	};

	this.populate = function()
	{
		me.ui.table.clear();
		$.each(main.users, function(index, user)
		{
			me.ui.table.row();
			if (!user.protected)
			{
				me.ui.table.selectBox(user.id);
			}
			else
			{
				me.ui.table.text("", "icon");
			}
			
			me.ui.table.hotText(user.nameSort, main.i18n.lookup("msg_edit", "Edit %0", "html", [user.nameDisplay]), function()
			{
				main.startEdit(user);
			}, null, {width: "calc(100vw - 100px - 792px)", minWidth: "calc(1200px - 100px - 792px)"});

			me.ui.table.text(user.eid, null, {width: 128});
			me.ui.table.text(user.iid, null, {width: 128});

			if (user.emailOfficial == null)
			{
				me.ui.table.text("-", null, {width: 128});
			}
			else
			{
				me.ui.table.text(user.emailOfficial, null, {width: 128});
			}

			if (user.avatar != null)
			{
				// var path = user.avatar.split("/");
				me.ui.table.html("<a href='" + user.avatar + "' target='_blank' class='e3_simple'><img src='" + user.avatar + "' border='0' alt='avatar' style='width:48px; height:auto; vertical-align:middle; padding-right:10px;'/></a>",
						null, {width: 48});
			}
			else
			{
				me.ui.table.text("-", null, {width: 48});
			}

			me.ui.table.text(user.id, "e3_text special light", {width: 200, fontSize: 11});

			if (!user.protected)
			{
				me.ui.table.contextMenu(
				[
					{title: main.i18n.lookup("cm_purge", "purge"), action:function(){me.purgeA(user);}}
		        ]);
			}
			else
			{
				me.ui.table.text("", null, {width: 24});
			}
		});

		me.ui.table.done();
	};

	this.add = function()
	{
		var user = {id:-1, timeZone: "America/Los_Angeles"};
		main.startEdit(user);
	};

	this.purge = function()
	{
		var params = main.portal.cdp.params();
		params.post.ids = me.ui.table.selected();

		if (params.post.ids.length == 0)
		{
			main.portal.dialogs.openAlert(main.ui.user_selectFirst);
			return;
		}

		main.portal.dialogs.openConfirm("user_confirmPurge", main.i18n.lookup("action_purge", "Purge"), function()
		{
			main.portal.cdp.request("user_purge user_get", params, function(data)
			{
				main.users = data.users || [];
				me.populate();
			});

			return true;
		}, function(){me.ui.table.clearSelection();});		
	};

	this.purgeA = function(user)
	{
		var params = main.portal.cdp.params();
		params.post.ids = [user.id];

		main.portal.dialogs.openConfirm("user_confirmPurge", main.i18n.lookup("action_purge", "Purge"), function()
		{
			main.portal.cdp.request("user_purge user_get", params, function(data)
			{
				main.users = data.users || [];
				me.populate();
			});

			return true;
		}, function(){me.ui.table.clearSelection();});
	};

//	this.setPaging = function()
//	{
//		var lastOnPage = Math.min(((me.paging.pageNum - 1) * me.paging.pageSize) + me.paging.pageSize, me.total);
//		var firstOnPage = Math.min(((me.paging.pageNum - 1) * me.paging.pageSize) + 1, me.total);
//		$("#user_pageDisplay").text(main.i18n.lookup("msg_page", "Page %0 (User %1 - %2 of %3)", "html", [me.paging.pageNum, firstOnPage, lastOnPage, me.total]));
//		
//		onClick("user_page_first", me.firstPage);
//		onClick("user_page_prev", me.prevPage);
//		onClick("user_page_next", me.nextPage);
//		onClick("user_page_last", me.lastPage);
//		
//		applyClass("e3_disabled", ["user_page_first", "user_page_prev"], (me.paging.pageNum <= 1));
//		applyClass("e3_disabled", ["user_page_last", "user_page_next"], (me.paging.pageNum >= Math.ceil(me.total / me.paging.pageSize)));
//	};
//
//	this.prevPage = function()
//	{
//		if (me.paging.pageNum > 1)
//		{
//			me.paging.pageNum--;
//			me.load();
//		}
//	};
//	
//	this.nextPage = function()
//	{
//		if (me.paging.pageNum < Math.ceil(me.total / me.paging.pageSize))
//		{
//			me.paging.pageNum++;
//			me.load();
//		}
//	};
//	
//	this.firstPage = function()
//	{
//		if (me.paging.pageNum != 1)
//		{
//			me.paging.pageNum = 1;
//			me.load();
//		}
//	};
//	
//	this.lastPage = function()
//	{
//		var last = Math.ceil(me.total / me.paging.pageSize);
//		if (me.paging.pageNum != last)
//		{
//			me.paging.pageNum = last;
//			me.load();
//		}
//	};
//
//	this.search = function()
//	{
//		$("#user_search").val(trim($("#user_search").val()));
//		me.paging.pageNum = 1;
//		me.load();
//	};
//	
//	this.clear = function()
//	{
//		$("#user_search").val("");
//		me.paging.pageNum = 1;
//		me.load();
//	};
}

function UserEdit(main)
{
	var me = this;

	this.ui = null;

	this.user = null;
	this.edit = null;

	this.init = function()
	{
		me.ui = findElements(["user_edit_nameFirst", "user_edit_nameLast", "user_edit_eid", "user_edit_iid", "user_edit_emailUser", "user_edit_emailOfficial",
								"user_edit_emailExposed", "user_edit_connectAim", "user_edit_connectFacebook", "user_edit_connectGooglePlus",
								"user_edit_connectLinkedIn", "user_edit_connectSkype", "user_edit_connectTwitter", "user_edit_connectWeb",
								"user_edit_profileInterests", "user_edit_profileLocation", "user_edit_profileOccupation", "user_edit_timeZone",
								"user_edit_avatar", "user_edit_avatar_remove", "user_edit_avatar_display", "user_edit_password", "user_edit_password2",
								"user_edit_signature_editor", "user_edit_attribution",
								"user_samePassword", "user_strongPassword"]);
		me.ui.itemNav = new e3_ItemNav();
		me.ui.editor = new e3_Editor(me.ui.user_edit_signature_editor, {height: 100});

		onChange(me.ui.user_edit_avatar, me.acceptAvatarSelect);
		onChange(me.ui.user_edit_avatar_remove, me.acceptAvatarRemove);
	};
	
	this.start = function(user)
	{
		me.user = user;

		main.onExit = me.checkExit;
		me.ui.itemNav.inject(main.ui.user_itemnav, {doneFunction:me.done, pos:main.userPosition(user), navigateFunction:me.changeUser});

		me.makeEdit();
		me.populate();
	};

	this.makeEdit = function()
	{
		me.edit = new e3_Edit(me.user, ["createdOn", "createdBy", "modifiedOn", "modifiedBy", "id", "protected"], function(changed)
		{
			main.ui.modebar.enableSaveDiscard(changed ? me.saveCancel : null);
			me.ui.itemNav.enableSave(changed ? me.saveCancel : null);
		});
		// me.edit.avatarRemove = false;
		me.edit.setFilters({"avatarNew": me.edit.noFilter, "avatarRemove": me.edit.noFilter, "emailExposed": me.edit.noFilter,
							"newPassword": me.edit.stringFilter, "newPasswordVerify": me.edit.stringFilter, "defaultFilter": me.edit.stringZeroFilter});
		
		main.ui.modebar.enableSaveDiscard(null);
		me.ui.itemNav.enableSave(null);
	};

	this.populate = function()
	{
		new e3_Attribution().inject(me.ui.user_edit_attribution, me.user);
		
		me.edit.setupFilteredEdit(me.ui.user_edit_nameFirst, me.edit, "nameFirst");
		me.edit.setupFilteredEdit(me.ui.user_edit_nameLast, me.edit, "nameLast");
		me.edit.setupFilteredEdit(me.ui.user_edit_eid, me.edit, "eid");
		me.edit.setupFilteredEdit(me.ui.user_edit_iid, me.edit, "iid");
		me.edit.setupFilteredEdit(me.ui.user_edit_emailOfficial, me.edit, "emailOfficial");
		me.edit.setupFilteredEdit(me.ui.user_edit_emailUser, me.edit, "emailUser");
		me.edit.setupCheckEdit(me.ui.user_edit_emailExposed, me.edit, "emailExposed");		
		me.edit.setupFilteredEdit(me.ui.user_edit_connectAim, me.edit, "connectAim");
		me.edit.setupFilteredEdit(me.ui.user_edit_connectFacebook, me.edit, "connectFacebook");
		me.edit.setupFilteredEdit(me.ui.user_edit_connectGooglePlus, me.edit, "connectGooglePlus");
		me.edit.setupFilteredEdit(me.ui.user_edit_connectLinkedIn, me.edit, "connectLinkedIn");
		me.edit.setupFilteredEdit(me.ui.user_edit_connectSkype, me.edit, "connectSkype");
		me.edit.setupFilteredEdit(me.ui.user_edit_connectTwitter, me.edit, "connectTwitter");
		me.edit.setupFilteredEdit(me.ui.user_edit_connectWeb, me.edit, "connectWeb");
		// me.edit.setupFilteredEdit(me.ui.user_edit_profileInterests, me.edit, "profileInterests");
		me.edit.setupFilteredEdit(me.ui.user_edit_profileLocation, me.edit, "profileLocation");
		me.edit.setupFilteredEdit(me.ui.user_edit_profileOccupation, me.edit, "profileOccupation");

		// me.ui.user_edit_profileOccupation.val(me.edit.profileOccupation);

		me.ui.user_edit_timeZone.val(me.edit.timeZone);
		onChange(me.ui.user_edit_timeZone, function()
		{
			me.edit.set(me.edit, "timeZone", me.ui.user_edit_timeZone.val());
		});

		me.ui.user_edit_avatar.val("");
		me.ui.user_edit_avatar_remove.prop("checked", false);
		if (me.edit.avatar != null)
		{
			me.ui.user_edit_avatar_display.css("width", "128px").css("height", "auto");
			me.ui.user_edit_avatar_display.attr("src", me.edit.avatar);
			show(me.ui.user_edit_avatar_display);
		}
		else
		{
			hide(me.ui.user_edit_avatar_display);
		}

		me.edit.setupFilteredEdit(me.ui.user_edit_password, me.edit, "newPassword", me.checkNewPw);
		me.edit.setupFilteredEdit(me.ui.user_edit_password2, me.edit, "newPasswordVerify", me.checkNewPw);
		me.checkNewPw();

		// me.editor.myfilesUser(me.user.id); TODO: ???
		me.ui.editor.disable();
		me.ui.editor.set(me.edit.signature);
		me.ui.editor.enable(function()
		{
			me.edit.set(me.edit, "signature", trimToZero(me.ui.editor.get()));
		});
	};

	this.checkNewPw = function()
	{
		var same = (me.edit.newPassword == me.edit.newPasswordVerify);
		var strong = (((me.edit.newPassword != null) && (strongPassword(me.edit.newPassword))) || ((me.edit.newPasswordVerify != null) && (strongPassword(me.edit.newPasswordVerify))));
		show(me.ui.user_samePassword, !same);
		show(me.ui.user_strongPassword, !strong && ((me.edit.newPassword != null) || (me.edit.newPasswordVerify != null)));
	};

	this.acceptAvatarSelect = function()
	{
		var fl = me.ui.user_edit_avatar.prop("files");
		if ((fl != null) && (fl.length > 0))
		{
			reader = new FileReader();
			reader.onloadend = function(e)
			{
				me.ui.user_edit_avatar_display.css("width", "128px").css("height", "auto");
				me.ui.user_edit_avatar_display.attr("src", e.target.result);
				show(me.ui.user_edit_avatar_display);
			};
			reader.readAsDataURL(fl[0]);
			me.ui.user_edit_avatar_remove.prop("checked", false);

			me.edit.set(me.edit, "avatarNew", fl[0]);
			me.edit.set(me.edit, "avatarRemove", false);
		}
	};

	this.acceptAvatarRemove = function()
	{
		if (me.ui.user_edit_avatar_remove.is(":checked"))
		{
			hide(me.ui.user_edit_avatar_display);			
			me.edit.set(me.edit, "avatarRemove", true);
		}
		else
		{
			if ((me.edit.avatar != null) || (me.edit.avatarNew != null))
			{
				show(me.ui.user_edit_avatar_display);
			}
			
			me.edit.set(me.edit, "avatarRemove", false);
		}
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
	
	this.changeUser = function(user)
	{
		if (!me.checkExit(function(){me.changeUser(user);})) return;
		main.startEdit(user);
	};

	this.save = function(deferred)
	{
		var params = main.portal.cdp.params();
		params.url.user = me.user.id;
		me.edit.params("", params);
		main.portal.cdp.request("user_save user_get", params, function(data)
		{
			main.users = data.users || [];
			me.user = main.findUser(data.id);
			me.makeEdit();
			me.populate();
			if (deferred !== undefined) deferred();
		});
	};
}

function User()
{
	var me = this;

	this.portal = null;
	this.i18n = new e3_i18n(user_i10n, "en-us");

	this.manageMode = null;
	this.editMode = null;

	this.ui = null;
	this.onExit = null;

	this.users = [];

	this.findUser = function(id)
	{
		for (var i = 0; i < me.users.length; i++)
		{
			if (me.users[i].id == id) return me.users[i];
		}
		
		return null;
	};

	this.userPosition = function(user)
	{
		var rv = {};
		
		if (user.id == null)
		{
			rv.item = me.users.length + 1;
			rv.total = me.users.length + 1;
			rv.prev = me.users[me.users.length-1];
			rv.next = null;
		}
		else
		{
			var i = 1;
			var found = null;
			$.each(me.users || [], function(index, u)
			{
				if (u.id == user.id) found = i;
				i++;
			});
	
			rv.item = found;
			rv.total = me.users.length;
			rv.prev = found > 1 ? me.users[found-2] : null;
			rv.next = found < me.users.length ? me.users[found] : null;
		}

		return rv;
	};

	this.init = function()
	{
		me.i18n.localize();
		me.ui = findElements(["user_header", "user_modebar", "user_headerbar", "user_itemnav",
		                      "user_manage", "user_bar_manage", "user_header_manage",
		                      "user_edit",
		                      "user_selectFirst"]);

		me.portal = portal_tool.features({onExit:function(deferred){return me.checkExit(deferred);}, pin:[{ui:me.ui.user_header}]});
		
		me.manageMode = new UserManage(me);
		me.manageMode.init();
		me.editMode = new UserEdit(me);
		me.editMode.init();

		me.ui.modebar = new e3_Modebar(me.ui.user_modebar);
		me.ui.modebar.set([], 0);
	};

	this.start = function()
	{
		me.startManage();
	};

	this.startManage = function()
	{
		if (!me.checkExit(function(){me.startManage();})) return;
		me.mode([me.ui.user_headerbar, me.ui.user_bar_manage, me.ui.user_header_manage, me.ui.user_manage]);
		me.manageMode.start();
	};

	this.startEdit = function(user)
	{
		if (!me.checkExit(function(){me.startEdit(user);})) return;
		me.mode([me.ui.user_modebar, me.ui.user_headerbar, me.ui.user_itemnav, me.ui.user_edit]);
		me.editMode.start(user);
	}

	this.mode = function(elements)
	{
		hide([me.ui.user_modebar, me.ui.user_headerbar, me.ui.user_itemnav,
		      me.ui.user_manage,  me.ui.user_bar_manage, me.ui.user_header_manage,
		      me.ui.user_edit]);
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

$(function()
{
	try
	{
		user_tool = new User();
		user_tool.init();
		user_tool.start();
	}
	catch (e)
	{
		error(e);
	}
});

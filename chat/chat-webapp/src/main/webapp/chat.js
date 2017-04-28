/**********************************************************************************
 * $URL: https://source.etudes.org/svn/serenity/trunk/chat/chat-webapp/src/main/webapp/chat.js $
 * $Id: chat.js 12505 2016-01-10 00:34:15Z ggolden $
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

var chat_tool = null;

function Chat()
{
	var me = this;

	this.portal = null;
	this.i18n = new e3_i18n(chat_i10n, "en-us");

	this.view = null;
	this.manage = null;

	this.ui = null;
	this.onExit = null;

	this.init = function()
	{		
		me.i18n.localize();
		
		me.ui = findElements(["chat_header", "chat_modebar", "chat_headerbar",
		                      "chat_view",  "chat_view_edit",
		                      "chat_manage", "chat_bar_manage", "chat_header_manage",
		                      "chat_selectFirst", "chat_confirmDelete"]);

		me.portal = portal_tool.features({onExit:function(deferred){return me.checkExit(deferred);}, pin:[{ui:me.ui.chat_header}]});

		me.view = new Chat_view(me);
		me.view.init();

		if (me.portal.site.role >= Role.instructor)
		{
			me.manage = new Chat_manage(me);
			me.manage.init();

			me.ui.modebar = new e3_Modebar(me.ui.chat_modebar);
			me.modes =
			[
				{name:me.i18n.lookup("mode_view", "View"), func:function(){me.startView();}},
				{name:me.i18n.lookup("mode_edit", "Manage"), func:function(){me.startManage();}}
			];
			me.ui.modebar.set(me.modes, 0);
			
			onClick(me.ui.chat_view_edit, me.startManage);
		}
	};

	this.start = function()
	{
		me.startView();
	};

	this.startView = function()
	{
		if (!me.checkExit(function(){me.startView();})) return;
		me.mode([me.ui.chat_view, ((me.portal.site.role >= Role.instructor) ? me.ui.chat_view_edit : null)]);
		if (me.ui.modebar !== undefined) me.ui.modebar.showSelected(0);
		me.view.start();
	};

	this.startManage = function()
	{
		if (!me.checkExit(function(){me.startManage();})) return;
		me.mode([me.ui.chat_modebar, me.ui.chat_headerbar, me.ui.chat_bar_manage, me.ui.chat_header_manage, me.ui.chat_manage]);
		me.ui.modebar.showSelected(1);
		me.manage.start();
	};

	this.mode = function(elements)
	{
		hide([me.ui.chat_modebar, me.ui.chat_headerbar, me.ui.chat_itemnav,
		      me.ui.chat_view, me.ui.chat_view_edit,
		      me.ui.chat_manage,  me.ui.chat_bar_manage, me.ui.chat_header_manage]);
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

function Chat_manage(main)
{
	var me = this;
	this.ui = null;
	this.rooms = null;
	this.edit = null;
	this.reorder = null;

	this.init = function()
	{
		me.ui = findElements(["chat_manage_table", "chat_manage_add", "chat_manage_publish", "chat_manage_unpublish", "chat_manage_delete"]);

		me.ui.table = new e3_Table(me.ui.chat_manage_table);
		me.ui.table.setupSelection("chat_table_select", me.updateActions);
		me.ui.table.selectAllHeader(2, main.ui.chat_header_manage);
		me.ui.table.enableReorder(me.applyOrder);

		onClick(me.ui.chat_manage_add, me.add);
		onClick(me.ui.chat_manage_publish, me.publish);
		onClick(me.ui.chat_manage_unpublish, me.unpublish);
		onClick(me.ui.chat_manage_delete, me.remove);

		setupHoverControls([me.ui.chat_manage_add, me.ui.chat_manage_publish, me.ui.chat_manage_unpublish, me.ui.chat_manage_delete]);
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
		main.portal.cdp.request("chat_manage", params, function(data)
		{
			me.afterLoad(data);
		});
	};

	this.afterLoad = function(data)
	{
		me.rooms = data.rooms;
		me.makeEdit();
		me.populate();
	};

	this.makeEdit = function()
	{
		me.edit = new e3_Edit({rooms:me.rooms}, ["rooms[].published"], function(changed)
		{
			main.ui.modebar.enableSaveDiscard((changed || (me.order != null)) ? me.saveCancel : null);
		});
		me.edit.setFilters({"title": me.edit.stringFilter}, "rooms[]");
		main.ui.modebar.enableSaveDiscard(null);
	};

	this.populate = function()
	{
		me.ui.table.clear();
		$.each(me.edit.rooms, function(index, room)
		{
			var r = findIdInList(room, me.rooms);

			me.ui.table.row();

			me.ui.table.reorder(main.i18n.lookup("msg_reorder", "drag to reorder"), room.id);
			me.ui.table.selectBox(room.id);

			if ((r == null) || (!r.published))
			{
				me.ui.table.dot("red",  main.i18n.lookup("msg_unpublished", "unpublished"));
			}
			else
			{
				me.ui.table.dot("green",  main.i18n.lookup("msg_published", "published"));
			}

			// me.ui.table.text(room.title, null, {width:"calc(100vw - 100px - 184px)", minWidth: "calc(1200px - 100px - 184px)"});
			var td = me.ui.table.input({size: "30", type:"text"}, null, {width: "calc(100vw - 100px - 184px)", minWidth:"calc(1200px - 100px - 184px)"});
			me.edit.setupFilteredEdit(td.find("input"), room, "title");

			// TODO: col for site / group: access

			me.ui.table.contextMenu(
			[
				{title: main.i18n.lookup("cm_publish", "Publish"), action:function(){me.publishA(room);}},
				{title: main.i18n.lookup("cm_unpublish", "Unpublish"), action:function(){me.unpublishA(room);}},
				{title: main.i18n.lookup("cm_delete", "Delete"), action:function(){me.removeA(room);}}
	        ]);
		});
		
		me.ui.table.done();
	};

	this.updateActions = function()
	{
		me.ui.table.updateActions([], [me.ui.chat_manage_publish, me.ui.chat_manage_unpublish, me.ui.chat_manage_delete]);		
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
		if ((me.reorder != null) || me.edit.changed())
		{
			main.portal.confirmNavigationWithChanges(function()
			{
				me.save(deferred);				
			}, function()
			{
				me.reorder = null;
				me.edit.revert();
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
		if (me.reorder != null) params.post.order = me.reorder;
		me.edit.params("", params);
		main.portal.cdp.request("chat_save chat_manage", params, function(data)
		{
			me.afterLoad(data);
			if (deferred !== undefined) deferred();
		});
	};
	
	this.nextRoomId = -1;
	this.add = function()
	{
		var room = {id: me.nextRoomId--, title: ""};
		me.edit.add(me.edit, "rooms", room);

		me.populate();
	};

	this.remove = function()
	{
		if ((main.onExit != null) && (!main.onExit(function(){me.remove();}))) return;

		var params = main.portal.cdp.params();
		params.url.site = main.portal.site.id;
		params.post.ids = me.ui.table.selected();

		if (params.post.ids.length == 0)
		{
			main.portal.dialogs.openAlert(main.ui.chat_selectFirst);
			return;
		}

		main.portal.dialogs.openConfirm(main.ui.chat_confirmDelete, main.i18n.lookup("action_delete", "Delete"), function()
		{
			main.portal.cdp.request("chat_remove chat_manage", params, function(data)
			{
				me.afterLoad(data);
			});

			return true;
		}, function(){me.ui.table.clearSelection();});
	};

	this.removeA = function(room)
	{
		if ((main.onExit != null) && (!main.onExit(function(){me.removeA(room);}))) return;

		var params = main.portal.cdp.params();
		params.url.site = main.portal.site.id;
		params.post.ids = [room.id];

		main.portal.dialogs.openConfirm(main.ui.chat_confirmDelete, main.i18n.lookup("action_delete", "Delete"), function()
		{
			main.portal.cdp.request("chat_remove chat_manage", params, function(data)
			{
				me.afterLoad(data);
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
			main.portal.dialogs.openAlert(main.ui.chat_selectFirst);
			return;
		}

		main.portal.cdp.request("chat_publish chat_manage", params, function(data)
		{
			me.afterLoad(data);
		});
	};

	this.publishA = function(room)
	{
		if ((main.onExit != null) && (!main.onExit(function(){me.publishA(annc);}))) return;

		var params = main.portal.cdp.params();
		params.url.site = main.portal.site.id;
		params.post.ids = [room.id];

		main.portal.cdp.request("chat_publish chat_manage", params, function(data)
		{
			me.afterLoad(data);
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
			main.portal.dialogs.openAlert(main.ui.chat_selectFirst);
			return;
		}

		main.portal.cdp.request("chat_unpublish chat_manage", params, function(data)
		{
			me.afterLoad(data);
		});
	};
	
	this.unpublishA = function(room)
	{
		if ((main.onExit != null) && (!main.onExit(function(){me.unpublishA(room);}))) return;

		var params = main.portal.cdp.params();
		params.url.site = main.portal.site.id;
		params.post.ids = [room.id];

		main.portal.cdp.request("chat_unpublish chat_manage", params, function(data)
		{
			me.afterLoad(data);
		});
	};
}

function Chat_view(main)
{
	var me = this;

	this.ui = null;
	this.messages = null;
	this.online = null;
	this.rooms = null;
	this.room = null;
	this.refresh = null;
	this.refreshInterval = 10 * 1000;		// every 10 seconds
	
	this.colors = 
	[ "red", "blue", "green", "orange", "firebrick", "teal", "goldenrod", 
	  "darkgreen", "darkviolet", "lightslategray", "peru", "deeppink", "dodgerblue", 
	  "limegreen", "rosybrown", "cornflowerblue", "crimson", "turquoise", "darkorange", 
	  "blueviolet", "royalblue", "brown", "magenta", "olive", "saddlebrown", "purple", 
	  "coral", "mediumslateblue", "sienna", "mediumturquoise", "hotpink", "lawngreen", 
	  "mediumvioletred", "slateblue", "indianred", "slategray", "indigo", "darkcyan",
	  "springgreen", "darkgoldenrod", "steelblue", "darkgray", "orchid", "darksalmon", 
	  "lime", "gold", "darkturquoise", "navy", "orangered",  "darkkhaki", "darkmagenta", 
	  "darkolivegreen", "tomato", "aqua", "darkred", "olivedrab" 
	];
	this.nextColor = 0;

	this.userColors = {};

	this.init = function()
	{
		me.ui = findElements(["chat_view_messages", "chat_view_online", "chat_view_post", "chat_view_rooms", "chat_view_roomTable", "chat_view_roomTitle",
		                      "chat_view_message_template", "chat_view_header_template", "chat_view_online_template", "chat_view_room_template"]);
		me.ui.roomTable = new e3_Table(me.ui.chat_view_roomTable);

		onEnter(me.ui.chat_view_post, me.post);
	};

	this.start = function()
	{
		main.onExit = me.checkExit;
		me.load();
	};
	
	this.load = function()
	{
		me.stop();

		var params = main.portal.cdp.params();
		params.url.site = main.portal.site.id;
		if (me.room != null) params.url.room = me.room.id;
		if ((me.messages != null) && (me.messages.length > 0)) params.url.after = me.messages[me.messages.length-1].id;
		main.portal.cdp.request("chat_get", params, function(data)
		{
			me.afterLoad(data);
		});
	};

	this.afterLoad = function(data)
	{
		me.messages = data.messages;
		me.online = data.online
		me.rooms = data.rooms;
		me.room = findIdInList(data.roomId, me.rooms);

		me.populate();

		me.refresh = setInterval(function(){try{me.load();}catch(e){error(e);}}, me.refreshInterval);
	};

	this.stop = function()
	{
		// TODO: stop the periodic update
		if (me.refresh != null)
		{
			clearInterval(me.refresh);
			me.refresh = null;
		}
	};

	this.colorForUser = function(user)
	{
		var colorForUser = me.userColors[user.id];
		if (colorForUser == null)
		{
			colorForUser = me.colors[me.nextColor];
			me.nextColor++;
			if (me.nextColor >= me.colors.length) me.nextColor = 0;
			me.userColors[user.id] = colorForUser;
		}
		
		return colorForUser;
	};

	this.populate = function()
	{
		// the room title
		me.ui.chat_view_roomTitle.text(me.room.title);

		// clear the message area TODO: deal with append for new messages without clear/flash
		me.ui.chat_view_messages.empty();
		
		// timestamp of the last header placed
		var hdrDate = null;

		// populate the messages
		$.each(me.messages, function(index, msg)
		{
			// date display when enough time (15 minutes) has passed
			if ((hdrDate == null) || (main.portal.timestamp.differenceInMinutes(hdrDate, msg.createdOn) >= 15))
			{
				var hdrCell = clone(me.ui.chat_view_header_template, ["chat_view_header_template_body", "chat_view_header_template_date"]);
				me.ui.chat_view_messages.append(hdrCell.chat_view_header_template_body);

				// fill in template
				hdrCell.chat_view_header_template_date.text(main.portal.timestamp.display(msg.createdOn));
				
				hdrDate = msg.createdOn;
			}

			// ui from template
			var msgCell = clone(me.ui.chat_view_message_template, ["chat_view_message_template_body", "chat_view_message_template_from", "chat_view_message_template_content", "chat_view_room_template"]);
			me.ui.chat_view_messages.append(msgCell.chat_view_message_template_body);

			// fill in template
			msgCell.chat_view_message_template_from.text(msg.from.nameDisplay);
			msgCell.chat_view_message_template_content.text(msg.content);

			msgCell.chat_view_message_template_from.css({color: me.colorForUser(msg.from)});
		});

		// TODO: empty message
		
		// populate the online
		me.ui.chat_view_online.empty();
		$.each(me.online, function(index, user)
		{
			var onlineCell = clone(me.ui.chat_view_online_template, ["chat_view_online_template_body", "chat_view_online_template_name", "chat_view_online_template_avatar", "chat_view_online_template_avatar_img"]);
			me.ui.chat_view_online.append(onlineCell.chat_view_online_template_body);

			// fill in template
			onlineCell.chat_view_online_template_name.text(user.nameDisplay);
			onlineCell.chat_view_online_template_avatar_img.attr("src", user.avatar);
			
			onlineCell.chat_view_online_template_name.css({color: me.colorForUser(user)});
		});

		// populate rooms
//		me.ui.chat_view_rooms.empty();
		me.ui.roomTable.clear();
		$.each(me.rooms, function(index, room)
		{
//			var roomCell = clone(me.ui.chat_view_room_template, ["chat_view_room_template_body", "chat_view_room_template_title", "chat_view_room_template_online"]);
//			me.ui.chat_view_rooms.append(roomCell.chat_view_room_template_body);
//
//			// fill in template
//			roomCell.chat_view_room_template_title.text(room.title);
//			roomCell.chat_view_room_template_online.text(room.online);

			me.ui.roomTable.row();

			// TODO: indicate current room
			var roomCell = clone(me.ui.chat_view_room_template, ["chat_view_room_template_body", "chat_view_room_template_title", "chat_view_room_template_online"]);
			var td = me.ui.roomTable.hotElement(roomCell.chat_view_room_template_body, main.i18n.lookup("msg_viewRoom", "view chat room"), function(){me.changeRoom(room);}, null, {width:276}); // 324 = 24 + x + 24
			td.css({paddingLeft:24, paddingRight:24});

			roomCell.chat_view_room_template_title.text(room.title);
			roomCell.chat_view_room_template_online.text(main.i18n.lookup("msg_population", "population: %0", "html", [room.online]));
		});
		me.ui.roomTable.done();
	};

	this.post = function()
	{
		var msg = trim(me.ui.chat_view_post.val());
		if (msg != null)
		{
			me.stop();

			var params = main.portal.cdp.params();
			params.url.site = main.portal.site.id;
			if (me.room != null) params.url.room = me.room.id;
			params.post.content = msg;
			main.portal.cdp.request("chat_post chat_get", params, function(data)
			{
				me.afterLoad(data);
			});
		}
		me.ui.chat_view_post.val("");
	};
	
	this.changeRoom = function(room)
	{
		me.stop();
		me.room = room;
		me.messages = null;
		me.load();
	};
	
	this.checkExit = function(deferred)
	{
		me.stop();
		return true;
	};
}

$(function()
{
	try
	{
		chat_tool = new Chat();
		chat_tool.init();
		chat_tool.start();
	}
	catch (e)
	{
		error(e);
	}
});

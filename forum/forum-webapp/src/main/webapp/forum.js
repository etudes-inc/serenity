/**********************************************************************************
 * $URL: https://source.etudes.org/svn/serenity/trunk/forum/forum-webapp/src/main/webapp/forum.js $
 * $Id: forum.js 12060 2015-11-12 03:58:14Z ggolden $
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

var forum_tool = null;

function Forum()
{
	var me = this;

	this.i18n = new e3_i18n(forum_i10n, "en-us");
	this.ui = null;
	this.portal = null;

	this.forumsMode = null;
	this.forumMode = null;
	this.topicMode = null;
	this.onExit = null;

	this.ForumStatus = {normal:0, reply:1, read:2};

	this.categories = [{id:1, title:"Category One"},{id:2, title:"Category Two"},{id:3, title:"Category Three"}];
	this.forums = [{id:1, category:1, title:"Forum One", description:"Questions about homework?  Posts them here", numTopics:0, numPosts:0, schedule:{open:0, due:0, status:ScheduleStatus.closed}, status:0, published:true, lastPost:{on:0, by:{id:1, nameDisplay:"Charles Robert Darwin"}}},
	               {id:2, category:1, title:"Forum Two - Forum Two - Forum Two - Forum Two - Forum Two - Forum Two - Forum Two - Forum Two - Forum Two - Forum Two", description:"Questions about homework?  Posts them here.  Questions about homework?  Posts them here.  Questions about homework?  Posts them here.  Questions about homework?  Posts them here.  Questions about homework?  Posts them here.", numTopics:0, numPosts:0, schedule:{open:0, due:0, status:ScheduleStatus.open}, status:0, published:true, lastPost:{on:0, by:{id:1, nameDisplay:"Charles Darwin"}}},
	               {id:3, category:2, title:"Forum Three", description:"Questions about homework?  Posts them here", numTopics:0, numPosts:0, schedule:{open:0, due:0, status:ScheduleStatus.willOpen}, status:1, published:true, lastPost:{on:0, by:{id:1, nameDisplay:"Charles Darwin"}}},
	               {id:4, category:3, title:"Forum Four", description:"Questions about homework?  Posts them here", numTopics:0, numPosts:0, schedule:{open:0, due:0, status:ScheduleStatus.willOpenHide}, status:2, published:true, lastPost:{on:0, by:{id:1, nameDisplay:"Charles Darwin"}}},
	               {id:5, category:3, title:"Forum Five", description:"Questions about homework?  Posts them here", numTopics:0, numPosts:0, schedule:{open:0, due:0, status:ScheduleStatus.closed}, status:2, published:false, lastPost:{on:0, by:{id:1, nameDisplay:"Charles Darwin"}}}
	               ];
	
	this.findCategory = function(id)
	{
		for (var i = 0; i < me.categories.length; i++)
		{
			if (me.categories[i].id == id) return me.categories[i];
		}
		return null;
	};
	
	this.findForum = function(id)
	{
		for (var i = 0; i < me.forums.length; i++)
		{
			if (me.forums[i].id == id) return me.forums[i];
		}
		return null;
	};

	this.forumPosition = function(forum)
	{
		var i = 1;
		var found = null;
		$.each(me.forums || [], function(index, f)
		{
			if (f.id == forum.id) found = i;
			i++;
		});

		var rv = {};
		rv.item = found;
		rv.total = me.forums.length;
		rv.prev = found > 1 ? me.forums[found-2] : null;
		rv.next = found < me.forums.length ? me.forums[found] : null;

		return rv;
	};
	
	this.topicPosition = function(forum, topic)
	{
		var i = 1;
		var found = null;
		$.each(forum.topics || [], function(index, t)
		{
			if (t.id == topic.id) found = i;
			i++;
		});

		var rv = {};
		rv.item = found;
		rv.total = forum.topics.length;
		rv.prev = found > 1 ? forum.topics[found-2] : null;
		rv.next = found < forum.topics.length ? forum.topics[found] : null;

		return rv;
	};

	this.lastPostAttribution = function(post)
	{
		var cell = clone(me.ui.forum_lastTemplate, ["forum_lastTemplate_body", "forum_lastTemplate_on", "forum_lastTemplate_by"]);
		cell.forum_lastTemplate_on.text(me.portal.timestamp.displayDate(post.on));
		cell.forum_lastTemplate_by.text(post.by.nameDisplay);
		return cell.forum_lastTemplate_body;
	};

	this.init = function()
	{
		me.i18n.localize();
		me.ui = findElements(["forum_header", "forum_modebar", "forum_headerbar", "forum_itemnav", "forum_headerbar2", "forum_header_manage",
		                      "forum_forums", "forum_header_forums",
		                      "forum_forum", "forum_bar_forum", "forum_bar2_forum", "forum_header_forum",
		                      "forum_topic", "forum_bar_topic", "forum_bar2_topic",
		                      "forum_lastTemplate"]);
		me.portal = portal_tool.features({onExit:function(deferred){return me.checkExit(deferred);}, pin:[{ui:me.ui.forum_header}]});
		
		me.forumsMode = new ForumForums(me);
		me.forumMode = new ForumForum(me);
		me.topicMode = new ForumTopic(me);
		me.forumsMode.init();
		me.forumMode.init();
		me.topicMode.init();
		
		me.ui.modebar = new e3_Modebar(me.ui.forum_modebar);
		me.modes =
		[
			{name:me.i18n.lookup("mode_forums", "Discussions"), func:function(){me.startForums();}},
			{name:me.i18n.lookup("mode_recent", "Recent"), func:function(){me.startManage();}},
			{name:me.i18n.lookup("mode_manage", "Manage"), func:function(){me.startManage();}},
			{name:me.i18n.lookup("mode_grade", "Grade"), func:function(){me.startGrade();}}
		];
		me.ui.modebar.set(me.modes, 0);
		// adjust modes for instructor / student

//		onClick(me.ui.forum_view_edit, me.startManage);
	};

	this.start = function()
	{
		me.startForums();
		// show me.ui.forum_header_manage if instructor
	};

	this.startForums = function()
	{
		if (!me.checkExit(function(){me.startForums();})) return;
		me.mode([me.ui.forum_header_forums, me.ui.forum_forums]);
		me.forumsMode.start();
	}

	this.startForum = function(forum)
	{
		if (!me.checkExit(function(){me.startForum(forum);})) return;
		me.mode([me.ui.forum_headerbar, me.ui.forum_itemnav, me.ui.forum_bar_forum, me.ui.forum_headerbar2, me.ui.forum_bar2_forum,
		         me.ui.forum_header_forum, me.ui.forum_forum]);
		me.forumMode.start(forum);
	}

	this.startTopic = function(forum, topic)
	{
		if (!me.checkExit(function(){me.startTopic(forum, topic);})) return;
		me.mode([me.ui.forum_headerbar, me.ui.forum_itemnav, me.ui.forum_bar_topic, me.ui.forum_headerbar2, me.ui.forum_bar2_topic, me.ui.forum_topic]);
		me.topicMode.start(forum, topic);
	}

	this.mode = function(elements)
	{
		hide([me.ui.forum_headerbar, me.ui.forum_itemnav, me.ui.forum_headerbar2, me.ui.forum_header_manage, 
		      me.ui.forum_forums, me.ui.forum_header_forums,
		      me.ui.forum_forum, me.ui.forum_bar_forum, me.ui.forum_bar2_forum, me.ui.forum_header_forum,
		      me.ui.forum_topic, me.ui.forum_bar_topic, me.ui.forum_bar2_topic]);
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

function ForumForums(main)
{
	var me = this;
	
	this.ui = null;

	this.init = function()
	{
		me.ui = findElements(["forum_forums_table", "forum_forums_listTemplate"]);
		me.ui.table = new e3_Table(me.ui.forum_forums_table);
	};
	
	this.start = function()
	{
		me.load();
	};
	
	this.load = function()
	{
		// TODO:
		me.populate();
	};
	
	this.populate = function()
	{
		me.ui.table.clear();
		$.each(main.forums, function(index, forum)
		{
			me.ui.table.row();

			// insert an in-table heading if we are at the start of a new category
			if (((index > 0) && (main.forums[index-1].category != forum.category)) || (index == 0))
			{
				var cat = main.findCategory(forum.category);
				me.ui.table.headerRow(cat.title);
				me.ui.table.row();
			}

			me.ui.table.text(main.i18n.lookup("msg_forumStatus_" + forum.status), "e3_text special light", {fontSize:11, width:30, textAlign:"center"});

			if (!forum.published)
			{
				me.ui.table.dot("red",  main.i18n.lookup("msg_unpublished", "not published"), false);
			}
			else if (forum.schedule.status == ScheduleStatus.closed)
			{
				me.ui.table.dot("red",  main.i18n.lookup("msg_closed", "closed"), true);
			}
			else if (forum.schedule.status == ScheduleStatus.open)
			{
				me.ui.table.dot("green",  main.i18n.lookup("msg_open", "open"), true);
			}
			else if (forum.schedule.status == ScheduleStatus.willOpen)
			{
				me.ui.table.dot("yellow",  main.i18n.lookup("msg_willOpen", "will open"), true);
			}
			else if (forum.schedule.status == ScheduleStatus.willOpenHide)
			{
				me.ui.table.dot("gray",  main.i18n.lookup("msg_willOpenHidden", "hidden until open"), true);
			}
			
			var cell = clone(me.ui.forum_forums_listTemplate, ["forum_forums_listTemplate_body", "forum_forums_listTemplate_title", "forum_forums_listTemplate_description"]);
			cell.forum_forums_listTemplate_title.text(forum.title);
			cell.forum_forums_listTemplate_description.text(forum.description);
			me.ui.table.hotElement(cell.forum_forums_listTemplate_body, main.i18n.lookup("msg_viewForum", "view forum"), function(){main.startForum(forum);}, null, {width:"calc(100vw - 670px)"});

			me.ui.table.date(forum.schedule.open, "-", "date2l");
			me.ui.table.date(forum.schedule.due, "-", "date2l");
			me.ui.table.text(forum.numTopics, null, {width:50, textAlign:"center"});
			me.ui.table.text(forum.numPosts, null, {width:50, textAlign:"center"});
			me.ui.table.element(main.lastPostAttribution(forum.lastPost), null, {width:100});
			me.ui.table.text("-", null, {width:100});// score
		});
		
		me.ui.table.done();
	};
}

function ForumForum(main)
{
	var me = this;
	
	this.ui = null;

	this.forum = null;

	this.init = function()
	{
		me.ui = findElements(["forum_forum_actions", "forum_forum_table", "forum_forum_listTemplate",
		                      "forum_info_forum_category", "forum_info_forum_title", "forum_info_forum_description",
		                      "forum_forum_add", "forum_forum_watch"]);

		me.ui.table = new e3_Table(me.ui.forum_forum_table);
		me.ui.table.setupSelection("forum_table_select", me.updateActions);
		me.ui.table.selectAllHeader(1, main.ui.forum_header_forum);

		me.ui.itemNav = new e3_ItemNav();

		new e3_SortAction().inject(me.ui.forum_forum_actions,
				{onSort: me.onSort, options:[{value:"T", title:main.i18n.lookup("sort_topic", "TOPIC")}, {value:"A", title:main.i18n.lookup("sort_author", "AUTHOR")},
				                             {value:"D", title:main.i18n.lookup("sort_date", "DATE")}]});
		
		onClick(me.ui.forum_forum_watch, me.watch);
		setupHoverControls([me.ui.forum_forum_add, me.ui.forum_forum_watch]);
	};
	
	this.start = function(forum)
	{
		me.forum = forum;
		me.ui.itemNav.inject(main.ui.forum_itemnav, {returnFunction:me.goBack, pos:main.forumPosition(me.forum), navigateFunction:me.goForum});

		// enable only if forum is normal, not read-only or reply-only or closed or locked
		onClick(me.ui.forum_forum_add, me.newTopic);
		// TODO: else show LOCKED / CLOSED / REPLY-ONLY?
	
		me.load();
	};
	
	this.load = function()
	{
		me.forum.topics = [{id:1, category:1, title:"Topic One", createdBy:{id:1, nameDisplay:"John Smith"}, createdOn:0, numPosts:0, schedule:{open:0, due:0, status:ScheduleStatus.open}, status:0, published:true, lastPost:{on:0, by:{id:1, nameDisplay:"Charles Darwin"}}}
							];
		
		// TODO:
		me.populate();
	};
	
	this.populate = function()
	{
		var cat = main.findCategory(me.forum.category);
		me.ui.forum_info_forum_category.text(cat.title);
		me.ui.forum_info_forum_title.text(me.forum.title);
		me.ui.forum_info_forum_description.text(me.forum.description);
		
		me.ui.table.clear();
		$.each(me.forum.topics, function(index, topic)
		{
			me.ui.table.row();
			me.ui.table.selectBox(topic.id);
			me.ui.table.text(main.i18n.lookup("msg_forumStatus_" + me.forum.status), "e3_text special light", {fontSize:11, width:30, textAlign:"center"});// TODO: topic status

			if (!topic.published)
			{
				me.ui.table.dot("red",  main.i18n.lookup("msg_unpublished", "not published"), false);
			}
			else if (topic.schedule.status == ScheduleStatus.closed)
			{
				me.ui.table.dot("red",  main.i18n.lookup("msg_closed", "closed"), true);
			}
			else if (topic.schedule.status == ScheduleStatus.open)
			{
				me.ui.table.dot("green",  main.i18n.lookup("msg_open", "open"), true);
			}
			else if (topic.schedule.status == ScheduleStatus.willOpen)
			{
				me.ui.table.dot("yellow",  main.i18n.lookup("msg_willOpen", "will open"), true);
			}
			else if (topic.schedule.status == ScheduleStatus.willOpenHide)
			{
				me.ui.table.dot("gray",  main.i18n.lookup("msg_willOpenHidden", "hidden until open"), true);
			}
			
			var cell = clone(me.ui.forum_forum_listTemplate, ["forum_forum_listTemplate_body", "forum_forum_listTemplate_title", "forum_forum_listTemplate_attribution"]);
			cell.forum_forum_listTemplate_title.text(topic.title);
			cell.forum_forum_listTemplate_attribution.text(main.i18n.lookup("msg_topicAttribution", "- %0, %1", "html", [topic.createdBy.nameDisplay, main.portal.timestamp.displayDate(topic.createdOn)]));
			me.ui.table.hotElement(cell.forum_forum_listTemplate_body, main.i18n.lookup("msg_viewTopic", "view topic"), function(){main.startTopic(me.forum, topic);}, null, {width:"calc(100vw - 667px)"});

			me.ui.table.date(topic.schedule.open, "-", "date2l");
			me.ui.table.date(topic.schedule.due, "-", "date2l");
			me.ui.table.text(topic.numPosts, null, {width:50, textAlign:"center"});
			me.ui.table.element(main.lastPostAttribution(topic.lastPost), null, {width:100});
			me.ui.table.text("-", null, {width:100});// score
			me.ui.table.contextMenu([
			                         {title:"Mark As Read", action:function(){me.markRead(topic);}},
			                         {title:"Lock", action:function(){me.lock(topic);}},
			                         {title:"Reuse", action:function(){me.reuse(topic);}},
			                         {title:"Move", action:function(){me.moveTopic(topic);}},
			                         {title:"Delete", action:function(){me.deleteTopic(topic);}}
			                         ]);
		});

		me.ui.table.done();
	};

	this.onSort = function(direction, option)
	{
		console.log("sort", direction, option);
	};

	this.updateActions = function()
	{
		me.ui.table.updateActions([], []);		
	};

	this.newTopic = function()
	{
		console.log("new topic");
	};

	this.markRead = function(topic)
	{
		console.log("mark read", topic);
	};

	this.markUnread = function(topic)
	{
		console.log("mark unread", topic);
	};

	this.lock = function(topic)
	{
		console.log("lock", topic);
	};

	this.unlock = function(topic)
	{
		console.log("unlock", topic);
	};

	this.reuse = function(topic)
	{
		console.log("reuse", topic);
	};

	this.dontReuse = function(topic)
	{
		console.log("dont reuse", topic);
	};

	this.moveTopic = function(topic)
	{
		console.log("move topic", topic);
	};

	this.deleteTopic = function(topic)
	{
		console.log("delete topic", topic);
	};

	this.watch = function()
	{
		console.log("watch");
	};

	this.goBack = function()
	{
		main.startForums();
	};
	
	this.goForum = function(forum)
	{
		main.startForum(forum);
	};
}

function ForumTopic(main)
{
	var me = this;
	
	this.ui = null;

	this.forum = null;
	this.topic = null;

	this.init = function()
	{
		me.ui = findElements(["forum_topic_table", "forum_topic_posts",
		                      "forum_info_topic_category", "forum_info_topic_title", "forum_info_topic_description",
		                      "forum_topic_reply", "forum_topic_watch", "forum_topic_bookmark", "forum_topic_markRead",
		                      "forum_topic_postTemplate"]);
		me.ui.itemNav = new e3_ItemNav();
		setupHoverControls([me.ui.forum_topic_reply, me.ui.forum_topic_watch, me.ui.forum_topic_bookmark, me.ui.forum_topic_markRead]);
	};
	
	this.start = function(forum, topic)
	{
		me.forum = forum;
		me.topic = topic;

		// TODO: reply only if forum is normal or reply-only, not if read-only or locked
		onClick(me.ui.forum_topic_reply, me.reply);

		// todo: switch to un version if already set
		onClick(me.ui.forum_topic_watch, function(){me.watch(true);});
		onClick(me.ui.forum_topic_bookmark, function(){me.bookmark(true);});
		onClick(me.ui.forum_topic_markRead, function(){me.markRead(true);});

		me.ui.itemNav.inject(main.ui.forum_itemnav, {returnFunction:me.goBack, pos:main.topicPosition(me.forum, me.topic), navigateFunction:me.goTopic});
		me.load();
	};
	
	this.load = function()
	{
		me.topic.posts = [{id:1, title:"Topic One", createdBy:{id:1, nameDisplay:"John Smith", location:"Denver, CO"}, createdOn:0, content:"<p>This is a post</p>", starredByUser:true, stars:22},
		                  {id:1, title:"Topic One", createdBy:{id:1, nameDisplay:"John Smith", location:"Denver, CO"}, createdOn:0, content:"<p>This is a post</p><p>This is a post</p><p>This is a post</p><p>This is a post</p><p>This is a post</p><p>This is a post</p><p>This is a post</p><p>This is a post</p><p>This is a post</p><p>This is a post</p><p>This is a post</p><p>This is a post</p><p>This is a post</p><p>This is a post</p><p>This is a post</p><p>This is a post</p><p>This is a post</p><p>This is a post</p><p>This is a post</p><p>This is a post</p><p>This is a post</p><p>This is a post</p><p>This is a post</p><p>This is a post</p><p>This is a post</p><p>This is a post</p><p>This is a post</p><p>This is a post</p><p>This is a post</p><p>This is a post</p>", starredByUser:false, stars:0},
		                  {id:1, title:"Topic One", createdBy:{id:1, nameDisplay:"John Smith", location:"Denver, CO"}, createdOn:0, content:"<p>This is a post</p>", starredByUser:true, stars:9}
							];

		// TODO:
		me.populate();
	};

	this.contextMenu = function(post)
	{
		var menu = [];
		// if not locked and not read only, we can reply
		if (true) menu.push({title:"Quote Reply", action:function(){me.quoteReply(post);}});
		// if the author or instructor, we can edit
		if (true) menu.push({title:"Edit", action:function(){me.editPost(post);}});
		// if the instructor, we can delete
		if (main.portal.site.role >= Role.instructor) menu.push({title:"Delete", action:function(){me.deletePost(post);}});

		return menu;
	};

	this.populate = function()
	{
		var cat = main.findCategory(me.forum.category);
		me.ui.forum_info_topic_category.text(cat.title);
		me.ui.forum_info_topic_title.text(me.forum.title);
		me.ui.forum_info_topic_description.text(me.forum.description);
		
		me.ui.forum_topic_posts.empty();
		$.each(me.topic.posts, function(index, post)
		{
			var cell = clone(me.ui.forum_topic_postTemplate, ["forum_topic_postTemplate_body", "forum_topic_postTemplate_date", "forum_topic_postTemplate_title", "forum_topic_postTemplate_content",
			                                                  "forum_topic_postTemplate_menu",
			                                                  "forum_topic_postTemplate_avatar", "forum_topic_postTemplate_name", "forum_topic_postTemplate_occupation", "forum_topic_postTemplate_location",
			                                                  "forum_topic_postTemplate_star", "forum_topic_postTemplate_stars"]);

			cell.forum_topic_postTemplate_date.text(main.portal.timestamp.display(post.createdOn));
			cell.forum_topic_postTemplate_title.text(post.title);
			cell.forum_topic_postTemplate_content.html(post.content);

			cell.forum_topic_postTemplate_stars.text(post.stars);
			onClick(cell.forum_topic_postTemplate_star, function(){me.star(post);});
			if (post.starredByUser)
			{
				applyClass("green", cell.forum_topic_postTemplate_star, true);
				cell.forum_topic_postTemplate_star.css({color:""});
			}
			contextMenu(cell.forum_topic_postTemplate_menu, me.contextMenu(post));

			me.ui.forum_topic_posts.append(cell.forum_topic_postTemplate_body);
		});
	};

	this.reply = function()
	{
		console.log("reply");
	};

	this.quoteReply = function(post)
	{
		console.log("quote reply", post);
	};

	this.markRead = function(setting)
	{
		console.log("mark read", setting);
	};

	this.bookmark = function(setting)
	{
		console.log("bookmark", setting);
	};

	this.editPost = function(post)
	{
		console.log("edit post", post);
	};

	this.deletePost = function(post)
	{
		console.log("delete post", post);
	};

	this.watch = function(setting)
	{
		console.log("watch", setting);
	};

	this.star = function(post)
	{
		console.log("star", post);
	};

	this.goBack = function()
	{
		main.startForum(me.forum);
	};
	
	this.goTopic = function(topic)
	{
		main.startTopic(me.forum, topic);
	};
}

$(function()
{
	try
	{
		forum_tool = new Forum();
		forum_tool.init();
		forum_tool.start();
	}
	catch (e)
	{
		error(e);
	}
});

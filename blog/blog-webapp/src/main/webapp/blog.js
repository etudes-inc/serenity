/**********************************************************************************
 * $URL: https://source.etudes.org/svn/serenity/trunk/blog/blog-webapp/src/main/webapp/blog.js $
 * $Id: blog.js 11540 2015-09-02 04:17:55Z ggolden $
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

var blog_tool = null;

function Blog_blogs(tool)
{
	var me = this;
	this.tool = tool;

	this.table = null;

	this.blogs = [];
	this.members = [];
	this.permissions = {};
	this.editingBlog = null;

	this.findBlog = function(id)
	{
		var found = null;
		$.each(me.blogs, function(index, blog)
		{
			if (blog.id == id)
			{
				found = blog;
			}
		});

		return found;
	};

	this.init = function()
	{
		// setup the actions
		onClick("blog_blogs_action_add", function(){me.add();});
		onClick("blog_blogs_action_edit", function(){me.edit();});
		onClick("blog_blogs_action_delete", function(){me.deleteBlog();});
		
		// setup the table of blogs
		me.table = new e3_Table("blog_blogs_table");
	};

	this.start = function()
	{
		me.load();
	};

	// update the table actions
	this.updateActions = function()
	{
		me.table.updateActions(me.blogs, ["blog_blogs_action_edit"], ["blog_blogs_action_delete"]);
	};

	// load the list of blogs for the site, and update the view
	this.load = function()
	{
		var params = me.tool.cdp.params();
		if (me.tool.site != null) params.url.site = me.tool.site.id;

		me.tool.cdp.request("blog_getSiteBlogs", params, function(data)
		{
			me.blogs = data.blogs || [];
			me.members = data.members || [];
			me.permissions = data.permissions || {};

			// populate the view
			me.populate();
		});
	};

	this.populate = function()
	{
		if (me.permissions.mayEdit)
		{
			me.table.setupSelection("blog_blogs_table_select", function(){me.updateActions();});
			me.table.selectAllHeader(1);
			$("#blog_blogs_actions").removeClass("e3_offstage");
		}
		else
		{
			$("#blog_blogs_actions").addClass("e3_offstage");
		}

		me.table.clear();
		$.each(me.blogs, function(index, blog)
		{
			me.table.row();

			// select box only if the user is permitted to edit
			if (me.permissions.mayEdit)
			{
				me.table.selectBox(blog.id);
			}
			else
			{
				me.table.text("", "tight");				
			}
			me.table.text(blog.owner_nameSort, "tight rGap");
			me.table.hotText(blog.name, me.tool.i18n.lookup("visitIt", "Visit") + " " + blog.name, function()
			{
				me.visit(blog);
			});
		});

		show("blog_blogs_none", me.table.rowCount() == 0);

		me.table.sort({0:{sorter:false}},[[2,0]]);
		me.table.done();
	};

	this.add = function()
	{
		var sel = $("#blog_blogs_edit_owner");
		$(sel).empty();
		$.each(me.members, function(index, member)
		{
			$(sel).append($("<option />", {value: member.userId, text: member.nameSort + " (" + member.role + ")"}));
		});

		$("#blog_blogs_edit_id").val("");
		$("#blog_blogs_edit_site").val("");
		$("#blog_blogs_edit_created").val("");
		$("#blog_blogs_edit_modified").val("");
		$("#blog_blogs_edit_ownerDisplay").val("");
		$("#blog_blogs_edit_name").val("");
		$("#blog_blogs_edit_owner").val("");
		$("#blog_blogs_edit_info").addClass("e3_offstage");

		me.tool.dialogs.openDialog("blog_blogs_edit", "Add", function()
		{
			// save the edited blog info
			var params = me.tool.cdp.params();
			params.url.site = me.tool.site.id;
			params.post.name = $.trim($("#blog_blogs_edit_name").val());
			params.post.owner = $.trim($("#blog_blogs_edit_owner").val());

			me.tool.cdp.request("blog_add", params, function(data)
			{
				// update the list with the new blog info
				me.load();
			});
	
			return true;
		});
	};

	this.edit = function()
	{
		// find the checked blog
		me.editingBlog = me.findBlog(me.table.selected()[0]);

		if (me.editingBlog == null)
		{
			me.tool.dialogs.openAlert("blog_blogs_selectFirst");
			return;
		}

		var sel = $("#blog_blogs_edit_owner");
		$(sel).empty();
		$.each(me.members, function(index, member)
		{
			$(sel).append($("<option />", {value: member.userId, text: member.nameSort + " " + member.role}));
		});

		$("#blog_blogs_edit_id").val(me.editingBlog.id);
		$("#blog_blogs_edit_site").val(me.editingBlog.site);
		$("#blog_blogs_edit_created").val(me.editingBlog.createdBy + (me.editingBlog.createdOn == null ? "" : (", " + me.tool.timestamp.display(me.editingBlog.createdOn))));
		$("#blog_blogs_edit_modified").val(me.editingBlog.modifiedBy + (me.editingBlog.modifiedOn == null ? "" : (", " + me.tool.timestamp.display(me.editingBlog.modifiedOn))));
		$("#blog_blogs_edit_ownerDisplay").val(me.editingBlog.owner_nameDisplay);
		$("#blog_blogs_edit_name").val(me.editingBlog.name);
		$("#blog_blogs_edit_owner").val(me.editingBlog.owner_id);
		$("#blog_blogs_edit_info").removeClass("e3_offstage");

		me.tool.dialogs.openDialog("blog_blogs_edit", "Save", function()
		{
			// save the edited blog info
			var params = me.tool.cdp.params();
			params.url.id = me.editingBlog.id;
			params.post.name = $.trim($("#blog_blogs_edit_name").val());
			params.post.owner = $.trim($("#blog_blogs_edit_owner").val());

			me.tool.cdp.request("blog_update", params, function(data)
			{
				// update the list with the new blog info
				me.load();
			});

			return true;
		});
	};

	this.deleteBlog = function()
	{
		var params = me.tool.cdp.params();
		params.post.ids = me.table.selected();

		if (params.post.ids.length == 0)
		{
			me.tool.dialogs.openAlert("blog_blogs_selectFirst");
			return;
		}

		me.tool.dialogs.openConfirm("blog_blogs_confirmDelete", "Delete", function()
		{
			// delete the blog(s)
			me.tool.cdp.request("blog_remove", params, function(data)
			{
				// update the list with the new blog info
				me.load();
			});

			return true;
		});
	};
	
	this.visit = function(blog)
	{
		me.tool.startIndex(blog);
	};
}

function Blog_index(tool)
{
	var me = this;
	this.tool = tool;

	this.table = null;

	this.blog = null;
	this.editingBlogEntry = null;
	this.editingBlogEntryGallerySelected = null;
	this.permissions = {};
	this.myfiles = [];

	this.findBlogEntry = function(id)
	{
		if ((me.blog == null) || (me.blog.entries == null)) return null;
		var found = null;
		$.each(me.blog.entries, function(index, entry)
		{
			if (entry.id == id)
			{
				found = entry;
			}
		});

		return found;
	};

	this.init = function()
	{
		// setup the actions
		onClick("blog_index_action_add", function(){me.add();});
		onClick("blog_index_action_edit", function(){me.edit();});
		onClick("blog_index_action_delete", function(){me.deleteBlogEntry();});
		onClick("blog_index_return", function(){me.returnToBlogs();});
		onChange("blog_index_edit_image", function(){me.acceptImageSelect();});
		onChange("blog_index_edit_image_remove", function(){me.acceptImageRemove();});

		me.table = new e3_Table("blog_index_table");		
	};

	this.start = function(blog)
	{
		me.blog = blog;
		me.load();
	};

	// update the table actions
	this.updateActions = function()
	{
		if ((me.blog != null) && (me.blog.entries != null))
		{
			me.table.updateActions(me.blog.entries, ["blog_index_action_edit"], ["blog_index_action_delete"]);
		}
	};

	this.load = function()
	{
		var params = me.tool.cdp.params();
		params.url.id = me.blog.id;

		me.tool.cdp.request("blog_getBlog", params, function(data)
		{
			me.blog = data.blog;
			me.permissions = data.permissions || {};

			// populate the view
			me.populate();
		});
	};

	this.populate = function()
	{
		if (me.permissions.mayEdit)
		{
			me.table.setupSelection("blog_index_table_select", function(){me.updateActions();});
			me.table.selectAllHeader(1);
			$("#blog_index_actions").removeClass("e3_offstage");
		}
		else
		{
			$("#blog_index_actions").addClass("e3_offstage");
		}

		$("#blog_index_name").empty();
		$("#blog_index_owner").empty();
		if (me.blog != null)
		{
			$("#blog_index_name").text(me.blog.name);
			$("#blog_index_owner").text(me.blog.owner_nameDisplay);
		}

		me.table.clear();
		if ((me.blog != null) && (me.blog.entries != null))
		{
			$.each(me.blog.entries, function(index, entry)
			{
				me.table.row();
				if (me.permissions.mayEdit)
				{
					me.table.selectBox(entry.id);
				}
				else
				{
					me.table.text("", "tight");
				}
	
				// image
				if (entry.image != null)
				{
					me.table.html("<img src='" + entry.image + "' border='0' alt='illustration' style='width:32px; height:auto; vertical-align:middle; padding-right:10px;'/>", "tight");
				}
				else
				{
					me.table.text("", "tight");
				}
	
				me.table.text(me.tool.timestamp.display(entry.modifiedOn), "tight rGap");
				me.table.hotText(entry.title, me.tool.i18n.lookup("readIt", "Read") + " " + entry.title, function()
				{
					me.visit(entry);
				});
			});
		}

		if ((me.blog == null) || (me.blog.entries == null) || (me.blog.entries.length == 0))
		{
			me.table.row();
			me.table.text("", "tight");
			me.table.text("", "tight");
			me.table.text("", "tight rGap");
			me.table.text("");
		}

		me.table.sort({0:{sorter:false},1:{sorter:false}},[[2,0]]);
		me.table.done();
	};

	this.add = function()
	{
		if (me.blog == null) return;

		$("#blog_index_edit_id").val("");
		$("#blog_index_edit_created").val("");
		$("#blog_index_edit_modified").val("");
		$("#blog_index_edit_title").val("");
		me.tool.editor.disable();
		me.tool.editor.set(null);
		me.tool.editor.enable();
		$("#blog_index_edit_image").val("");
		$("#blog_index_edit_image_remove").prop("checked", false);
		$("#blog_index_edit_image_display").addClass("e3_offstage");
		$("#blog_index_edit_info").addClass("e3_offstage");

		// we need the user's myfiles before we can edit
		var params = me.tool.cdp.params();
		me.tool.cdp.request("myfiles_get", params, function(data)
		{
			me.myfiles = data.myfiles || [];
			me.editingBlogEntryGallerySelected = null;

			var gallery = $("#blog_index_edit_gallery");
			$(gallery).empty();
			$.each(me.myfiles, function(index, file)
			{
				var figure = $("<figure />",{style:"display:inline-block;"});
				var caption = $("<figcaption />").html(file.name);
				var img = $("<img />", {src: file.downloadUrl, border:"0", alt:file.name, style:"width:64px; height:auto;"});
				$(figure).append(img);
				$(figure).append(caption);
				var a = $("<a />", {href:"", class:"e3_toolUiLinkU"});
				onClick(a, function(){me.selectGallery(file);});
				$(a).append(figure);
				$(gallery).append(a);
				if ((index+1) % 4 == 0) $(gallery).append("<br />");
			});

			me.tool.dialogs.openDialog("blog_index_edit", me.tool.i18n.lookup("button_add", "Add"), function()
			{
				// save the edited blog entry info
				var params = me.tool.cdp.params();
				params.url.blog = me.blog.id;
				params.post.title = $.trim($("#blog_index_edit_title").val());
				params.post.content = me.tool.editor.get();
				params.post.image = $("#blog_index_edit_image").prop("files")[0];
				params.post.imageRemove = $("#blog_index_edit_image_remove").is(":checked") ? "1" : "0";
				if (me.editingBlogEntryGallerySelected != null) params.post.galleryImage = me.editingBlogEntryGallerySelected.refId;
	
				me.tool.cdp.request("blog_addEntry", params, function(data)
				{
					// update the blog display with the new blog info
					// TODO: have _addEntry return blog_getBlog to avoid the double call
					me.load();
				});
		
				return true;
			});
		});
	};

	this.edit = function()
	{
		// find the checked blog entry
		me.editingBlogEntry = me.findBlogEntry(me.table.selected()[0]);

		if (me.editingBlogEntry == null)
		{
			me.tool.dialogs.openAlert("blog_index_selectFirst");
			return;
		}
		
		// we need the full entry loaded to edit
		var params = me.tool.cdp.params();
		params.url.id = me.editingBlogEntry.id;

		me.tool.cdp.request("blog_getEntry myfiles_get", params, function(data)
		{
			me.editingBlogEntry = data.entry;
			me.editingBlogEntryGallerySelected = null;

			$("#blog_index_edit_id").val(me.editingBlogEntry.id);
			$("#blog_index_edit_created").val(me.editingBlogEntry.createdBy + (me.editingBlogEntry.createdOn == null ? "" : (", " + me.tool.timestamp.display(me.editingBlogEntry.createdOn))));
			$("#blog_index_edit_modified").val(me.editingBlogEntry.modifiedBy + (me.editingBlogEntry.modifiedOn == null ? "" : (", " + me.tool.timestamp.display(me.editingBlogEntry.modifiedOn))));
			$("#blog_index_edit_title").val(me.editingBlogEntry.title);
			me.tool.editor.set(me.editingBlogEntry.content);
			me.tool.editor.enable();
			$("#blog_index_edit_image").val("");
			$("#blog_index_edit_image_remove").prop("checked", false);
			$("#blog_index_edit_info").removeClass("e3_offstage");
	
			if (me.editingBlogEntry.image != null)
			{
				$("#blog_index_edit_image_display").css("width", "128px").css("height", "auto");
				$("#blog_index_edit_image_display").attr("src", me.editingBlogEntry.image);
				$("#blog_index_edit_image_display").removeClass("e3_offstage");
			}
			else
			{
				$("#blog_index_edit_image_display").addClass("e3_offstage");
			}
			
			me.myfiles = data.myfiles || [];
			var gallery = $("#blog_index_edit_gallery");
			$(gallery).empty();
			$.each(me.myfiles || [], function(index, file)
			{
				var figure = $("<figure />",{style:"display:inline-block;"});
				var caption = $("<figcaption />").html(file.name);
				var img = $("<img />", {src: file.downloadUrl, border:"0", alt:file.name, style:"width:64px; height:auto;"});
				$(figure).append(img);
				$(figure).append(caption);
				var a = $("<a />", {href:"", class:"e3_toolUiLinkU"});
				onClick(a, function(){me.selectGallery(file);});
				$(a).append(figure);
				$(gallery).append(a);
				if ((index+1) % 4 == 0) $(gallery).append("<br />");
			});
	
			me.tool.dialogs.openDialog("blog_index_edit", me.tool.i18n.lookup("button_save", "Save"), function()
			{
				// save the edited blog entry info
				var params = me.tool.cdp.params();
				params.url.id = me.editingBlogEntry.id;
				params.post.title = $.trim($("#blog_index_edit_title").val());
				params.post.content = me.tool.editor.get();
				params.post.image = $("#blog_index_edit_image").prop("files")[0];
				params.post.imageRemove = $("#blog_index_edit_image_remove").is(":checked") ? "1" : "0";
				if (me.editingBlogEntryGallerySelected != null) params.post.galleryImage = me.editingBlogEntryGallerySelected.refId;
				me.tool.cdp.request("blog_updateEntry", params, function(data)
				{
					// update the blog display with the new blog info
					// TODO: have _updateEntry return blog_getBlog to avoid the double call
					me.load();
				});
	
				return true;
			});
		});
	};

	this.deleteBlogEntry = function()
	{
		var params = me.tool.cdp.params();
		params.post.ids = me.table.selected();

		if (params.post.ids.length == 0)
		{
			me.tool.dialogs.openAlert("blog_index_selectFirst");
			return;
		}

		me.tool.dialogs.openConfirm("blog_index_confirmDelete", me.tool.i18n.lookup("button_delete", "Delete"), function()
		{
			// delete the entries(s)
			me.tool.cdp.request("blog_removeEntry", params, function(data)
			{
				// update the blog display with the new blog info
				// TODO: have _removeEntry return blog_getBlog to avoid the double call
				me.load();
			});

			return true;
		});
	};

	this.acceptImageSelect = function()
	{
		var fl = $("#blog_index_edit_image").prop("files");
		if (fl != null)
		{
			reader = new FileReader();
			reader.onloadend = function(e)
			{
				$("#blog_index_edit_image_display").css("width", "128px").css("height", "auto");
				$("#blog_index_edit_image_display").attr("src", e.target.result);
				$("#blog_index_edit_image_display").removeClass("e3_offstage");
			};
			reader.readAsDataURL(fl[0]);
		}
		else
		{
			// no support for Files - bad IE
		}
		
		$("#blog_index_edit_image_remove").prop("checked", false);
	};
	
	this.selectGallery = function(file)
	{
		me.editingBlogEntryGallerySelected = file;

		$("#blog_index_edit_image_display").css("width", "128px").css("height", "auto");
		$("#blog_index_edit_image_display").attr("src", file.downloadUrl);
		$("#blog_index_edit_image_display").removeClass("e3_offstage");

		$("#blog_index_edit_image_remove").prop("checked", false);
	};

	this.acceptImageRemove = function()
	{
		if ($("#blog_index_edit_image_remove").is(":checked"))
		{
			me.editingBlogEntryGallerySelected = null;
			$("#blog_index_edit_image").val("");
			$("#blog_index_edit_image_display").addClass("e3_offstage");
		}
		else
		{
			if (me.editingBlogEntry.image != null)
			{
				$("#blog_index_edit_image_display").css("width", "128px").css("height", "auto");
				$("#blog_index_edit_image_display").attr("src", me.editingBlogEntry.image);
				$("#blog_index_edit_image_display").removeClass("e3_offstage");
			}
		}
	};
	
	this.visit = function(entry)
	{
		me.tool.startEntry(me.blog, entry);
	};

	this.returnToBlogs = function()
	{
		me.tool.startBlogs();
	};
}

function Blog_entry(tool)
{
	var me = this;
	this.tool = tool;

	this.blog = null;
	this.entry = null;

	this.init = function()
	{
		onClick("blog_entry_return", function(){me.returnToIndex();});		
	};

	this.start = function(blog, entry)
	{
		me.blog = blog;
		me.entry = entry;

		me.load();
	};

	this.load = function()
	{
		var params = me.tool.cdp.params();
		params.url.id = me.entry.id;

		me.tool.cdp.request("blog_getEntry", params, function(data)
		{
			me.entry = data.entry;

			// populate the view
			me.populate();
		});
	};

	this.populate = function()
	{
		$("#blog_entry_blogName").empty().text(me.blog.name);
		$("#blog_entry_blogOwner").empty().text(me.blog.owner_nameDisplay);

		$("#blog_entry_title").empty();
		$("#blog_entry_owner").empty();
		$("#blog_entry_date").empty();
		$("#blog_entry_illustration").addClass("e3_offstage");
		$("#blog_entry_content").empty();
		if (me.entry != null)
		{
			$("#blog_entry_title").text(me.entry.title);
			$("#blog_entry_owner").text(me.entry.modifiedBy);
			$("#blog_entry_date").text(me.tool.timestamp.display(me.entry.modifiedOn));
			if (me.entry.image != null)
			{
				$("#blog_entry_illustration").removeClass("e3_offstage");
				$("#blog_entry_illustration_display").css("width", "128px").css("height", "auto");
				$("#blog_entry_illustration_display").attr("src", me.entry.image);
			}
			
			$("#blog_entry_content").html(me.entry.content);
		}
	};

	this.returnToIndex = function()
	{
		me.tool.startIndex(me.blog);
	};
}

function Blog()
{
	var _me = this;

	this.errorHandler = null;
	this.cdp = new e3_Cdp({onErr:function(code){if (_me.errorHandler != null) _me.errorHandler(code);}});
	this.i18n = new e3_i18n(blog_i10n, "en-us");
	this.dialogs = new e3_Dialog();
	this.editor = new e3_EditorCK("blog_index_edit_content");
	this.timestamp = new e3_Timestamp(this.cdp);

	this.site = null;

	this._blogs = new Blog_blogs(this);
	this._index = new Blog_index(this);
	this._entry = new Blog_entry(this);

	this.init = function()
	{
		_me.i18n.localize();
		_me._blogs.init();
		_me._index.init();
		_me._entry.init();
		
		// portal: title, reset and configure
		if (portal_tool != null)
		{
			var portalInfo = portal_tool.features(_me.i18n.lookup("titlebar","Blog"), function(){_me.startBlogs();}, null);
			_me.site = portalInfo.site;
			_me.errorHandler = portalInfo.errorHandler;
		}
	};

	this.start = function()
	{
		_me.startBlogs();
	};

	this.startBlogs = function()
	{
		_me._blogs.start();
		_me._mode("blog_blogs");
	};
	
	this.startIndex = function(blog)
	{
		_me._index.start(blog);
		_me._mode("blog_index");
	};
	
	this.startEntry = function(blog, entry)
	{
		_me._entry.start(blog, entry);
		_me._mode("blog_entry");
	};

	this._mode = function(modeName)
	{
		$("#blog_blogs").addClass("e3_offstage");
		$("#blog_index").addClass("e3_offstage");
		$("#blog_entry").addClass("e3_offstage");

		$("#" + modeName).removeClass("e3_offstage");
	};
}

$(function()
{
	try
	{
		blog_tool = new Blog();
		blog_tool.init();
		blog_tool.start();
	}
	catch (e)
	{
		error(e);
	}
});

/**********************************************************************************
 * $URL: https://source.etudes.org/svn/serenity/trunk/ui/ui-webapp/src/main/webapp/etudes-filer.js $
 * $Id: etudes-filer.js 12267 2015-12-13 23:27:07Z ggolden $
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

// Object used to build myfiles pickers and maintainers
// Note: all ids are reference ids, not raw file ids
function e3_Filer(ui, options)
{
	var _me = this;

	this._i18n = new e3_i18n(etudes_filer_i10n, "en-us");
	this._i18n.init();

	this._ui = null;

	this._files = [];
	this._selected = null;
	this._onChange = null;
	this._enabled = false;

	this._findFileByName = function(name)
	{
		var found = null;
		$.each(_me._files, function(index, file)
		{
			if (file.file.name == name)
			{
				found = file;
			}
		});
		
		return found;
	};

	this._findFileById = function(id)
	{
		var found = null;
		$.each(_me._files, function(index, file)
		{
			if (file.id == id)
			{
				found = file;
			}
		});
		
		return found;
	};

	this._init = function()
	{
		_me._ui = clone("e3_filer_template", ["e3_filer_template_bodyH", "e3_filer_template_bodyB", "e3_filer_tableHeaders", "e3_filer_headerPick",
		                                      "e3_filer_tableHeight", "e3_filer_template_filesTable", "e3_filer_tableFooter",
		                                      "e3_filer_actionbar", "e3_filer_view", "e3_filer_dowload", "e3_filer_rename", "e3_filer_replace", "e3_filer_delete", "e3_filer_upload",
		                                      "e3_filer_template_upload", "e3_filer_template_none"]);
		
		var target = ($.type(ui) === "string") ? $("#" + ui) : ui;
		if ((options !== undefined) && (options.headerUi !== undefined))
		{
			var targetHeader = ($.type(options.headerUi) === "string") ? $("#" + options.headerUi) : options.headerUi;
			targetHeader.empty().append(_me._ui.e3_filer_template_bodyH);
			target.empty().append(_me._ui.e3_filer_template_bodyB);			
		}
		else
		{
			target.empty().append(_me._ui.e3_filer_template_bodyH).append(_me._ui.e3_filer_template_bodyB);
		}

		_me._ui.table = new e3_Table(_me._ui.e3_filer_template_filesTable);
		_me._ui.table.setupSelection("e3_filer_table_select", _me._updateActions);
		_me._ui.table.selectAllHeader(1, _me._ui.e3_filer_tableHeaders);
		
		onClick(_me._ui.e3_filer_view, function(){_me._view(null);});
		onClick(_me._ui.e3_filer_delete, function(){_me._remove(null);});
		onClick(_me._ui.e3_filer_dowload, _me._noDownload);
		onClick(_me._ui.e3_filer_replace, function(){_me._startReplace(null);});
		onClick(_me._ui.e3_filer_rename, function(){_me._startRename(null);});
		onClick(_me._ui.e3_filer_upload, function(){_me._startUpload(null);});
		setupHoverControls([_me._ui.e3_filer_view, _me._ui.e3_filer_dowload, _me._ui.e3_filer_rename, _me._ui.e3_filer_replace, _me._ui.e3_filer_delete, _me._ui.e3_filer_upload]);
		
		if ((options !== undefined) && (options.select !== undefined) && (options.select == false)) _me._ui.e3_filer_headerPick.text("");
	};

	this._updateActions = function()
	{
		_me._ui.table.updateActions([_me._ui.e3_filer_view, _me._ui.e3_filer_dowload, _me._ui.e3_filer_rename, _me._ui.e3_filer_replace], [_me._ui.e3_filer_delete]);

		// for download
		var ids = _me._ui.table.selected();
		var file = null;
		if (ids.length == 1)
		{
			file = _me._findFileById(ids[0]);
		}
		if (file != null)
		{
			_me._ui.e3_filer_dowload.attr("download", file.file.name);
			_me._ui.e3_filer_dowload.attr("href", file.downloadUrl);
			_me._ui.e3_filer_dowload.off("click");
		}
		else
		{
			_me._ui.e3_filer_dowload.removeAttr("download");
			_me._ui.e3_filer_dowload.attr("href","");
			onClick(_me._ui.e3_filer_dowload, _me._noDownload);
		}
	};

	this._load = function()
	{
		portal_tool.cdp.request("myfiles_get", null, function(data)
		{
			_me._files = data.myfiles || [];
			_me._populate();
		});
	};

	this._select = function(item)
	{
		_me._selected = item;
		_me._populate();
		
		_me._changed();
	};

	this._populate = function()
	{
		_me._populateList();
	};

	this._populateList = function()
	{
		if ((options !== undefined) && (options.tableHeight !== undefined))
		{
			if (options.tableHeight == false)
			{
				_me._ui.e3_filer_tableHeight.css({height: "auto", overflowY: "visible"});
				hide(_me._ui.e3_filer_tableFooter);
			}
			else
			{
				_me._ui.e3_filer_tableHeight.css({height: options.tableHeight, overflowY: "scroll"});
				show(_me._ui.e3_filer_tableFooter);
			}
		}
		_me._ui.table.clear();
		$.each(_me._files, function(index, file)
		{
//			_me._ui.table.hotRow(function(){_me._select(file);});
			_me._ui.table.row();

			_me._ui.table.selectBox(file.id);
			if ((options !== undefined) && (options.select !== undefined) && (options.select == false))
			{
				_me._ui.table.dot(Dots.none, null, true, null, {width:48});
			}
			else
			{
				if ((_me._selected != null) && (file.id == _me._selected.id))
				{
					_me._ui.table.dot(Dots.green, _me._i18n.lookup("msg_selected", "currently picked file"), true, null, {width:48});
				}
				else
				{
					_me._ui.table.hotDot(Dots.hollow, _me._i18n.lookup("msg_select", "click to pick this file"), function(){_me._select(file)}, null, {width:48});
				}
			}

			if (isImageMimeType(file.file.mimeType))
			{
				_me._ui.table.image(file.downloadUrl, {maxWidth: 32, maxHeight: 32, verticalAlign: "middle"}, null, {width: 32});
			}
			else
			{
				_me._ui.table.image("/ui/icons/document32.png", {maxWidth: 32, maxHeight: 32, verticalAlign: "middle"}, null, {width: 32});				
			}

			if ((options !== undefined) && (options.select !== undefined) && (options.select == false))
			{
				var titleTd = _me._ui.table.text(file.file.name, null, {width: "calc(100vw - 100px - 658px)", minWidth: "calc(1200pxw - 100px - 658px))"});
				titleTd.css({cursor:"context-menu"});
				titleTd.contextPopup(
				{
					title: _me._i18n.lookup("label_actions", "Actions"),
					items:
					[
					       {label:_me._i18n.lookup("button_view", "View"), icon:'/ui/icons/magnifier.png', action:function(){_me._view(file);return false;}},
					       {label:_me._i18n.lookup("button_download", "Download"), icon:'/ui/icons/arrow_down.png', url:file.downloadUrl, download:file.file.name},//action:function(){_me.download(file);return false;}},
					       null,
					       {label:_me._i18n.lookup("button_rename", "Rename"), icon:'/ui/icons/ui-text-field-select.png', action:function(){_me._startRename(file, titleTd);return false;}},
					       {label:_me._i18n.lookup("button_replace", "Replace"), icon:'/ui/icons/arrow_refresh.png', action:function(){_me._startReplace(file);return false;}},
					       null,
					       {label:_me._i18n.lookup("button_delete", "Delete"), icon:'/ui/icons/remove.png', action:function(){_me._remove(file);return false;}, isEnabled:function(){return (file.usage.length == 0);}},
					]
				});
				titleTd.attr("fileid", file.id);
			}
			else
			{
				var titleTd = _me._ui.table.text(file.file.name, null, {width: "calc(100vw - 100px - 658px)", minWidth: "calc(1200px - 100px - 658px)"});
				titleTd.css({cursor:"context-menu"});
				titleTd.contextPopup(
				{
					title: _me._i18n.lookup("label_actions", "Actions"),
					items:
					[
					       {label:_me._i18n.lookup("button_select", "Pick"), icon:'/ui/icons/flag_green.png', action:function(){_me._select(file);return false;}},
					       null,
					       {label:_me._i18n.lookup("button_view", "View"), icon:'/ui/icons/magnifier.png', action:function(){_me._view(file);return false;}},
					       {label:_me._i18n.lookup("button_download", "Download"), icon:'/ui/icons/arrow_down.png', url:file.downloadUrl, download:file.file.name},//action:function(){_me.download(file);return false;}},
					       null,
					       {label:_me._i18n.lookup("button_rename", "Rename"), icon:'/ui/icons/ui-text-field-select.png', action:function(){_me._startRename(file, titleTd);return false;}},
					       {label:_me._i18n.lookup("button_replace", "Replace"), icon:'/ui/icons/arrow_refresh.png', action:function(){_me._startReplace(file);return false;}},
					       null,
					       {label:_me._i18n.lookup("button_delete", "Delete"), icon:'/ui/icons/remove.png', action:function(){_me._remove(file);return false;}, isEnabled:function(){return (file.usage.length == 0);}},
					]
				});
				titleTd.attr("fileid", file.id);
			}
			_me._ui.table.text(file.file.size, null, {width:100});
			_me._ui.table.text(file.file.mimeType, null, {width:200});
			_me._ui.table.date(file.file.date);
		});
		_me._ui.table.done();
		show(_me._ui.e3_filer_template_none, (_me._ui.table.rowCount() == 0));

		onChange(_me._ui.e3_filer_template_upload, _me._acceptUploadSelect);

		_me._ui.e3_filer_template_bodyH.off("drop").on("drop", _me._onDrop);
		_me._ui.e3_filer_template_bodyB.off("drop").on("drop", _me._onDrop);
	};

	this._view = function(file)
	{
		if (file == null)
		{
			var ids = _me._ui.table.selected();

			if (ids.length == 0)
			{
				portal_tool.dialogs.openAlert("e3_filer_select1First");
				return;
			}
			file = _me._findFileById(ids[0]);
		}

		if (file == null) return;

		window.open(file.downloadUrl, '_blank');
	};

	this._remove = function(file)
	{
		var ids = [];
		if (file == null)
		{
			ids = _me._ui.table.selected();

			if (ids.length == 0)
			{
				portal_tool.dialogs.openAlert("e3_filer_selectFirst");
				return;
			}

			var anyValid = false;
			for (var i = 0; i < ids.length; i++)
			{
				var file = _me._findFileById(ids[i]);
				if ((file != null) && (file.usage.length == 0))
				{
					anyValid = true;
					break;
				}
			}
			if (!anyValid)
			{
				portal_tool.dialogs.openAlert("e3_filer_noDelete");
				return;
			}
		}
		else
		{
			ids.push(file.id);
		}

		var params = portal_tool.cdp.params();
		params.url.ids = ids;
		portal_tool.dialogs.openConfirm("e3_filer_confirmRemove", _me._i18n.lookup("button_delete", "Delete"), function()
		{
			// unselect the file if selected
			if ((_me._selected != null) && (ids.indexOf(_me._selected.refId) != -1))
			{
				_me._selected = null;
				_me._changed();
			}

			portal_tool.cdp.request("myfiles_remove myfiles_get", params, function(data)
			{
				_me._files = data.myfiles || [];
				_me._populate();
			});

			return true;
		});
	};
	
	this._noDownload = function()
	{
		portal_tool.dialogs.openAlert("e3_filer_select1First");
	};

	this._changed = function()
	{
		if (_me._onChange != null)
		{
			try
			{
				_me._onChange();
			}
			catch (e)
			{
				error(e);
			}
		}
	};

	this._startUpload = function()
	{
		_me._ui.e3_filer_template_upload.click();
	};

	this._acceptUploadSelect = function()
	{
		var fl = _me._ui.e3_filer_template_upload.prop("files");
		_me._uploadFile(fl[0]);
		_me._ui.e3_filer_template_upload.val("");
	};

	this._onDrop = function(e)
	{
		try
		{
			e.preventDefault();
			e.stopPropagation();
			var files = e.originalEvent.dataTransfer.files;
			_me._uploadFiles(files);
		}
		catch (e)
		{
			error(e);
		}
		return false;		
	};

	this._uploadFiles = function(files)
	{
		var dups = false;
		$.each(files || [], function(index, file)
		{
			var myfile = _me._findFileByName(file.name);
			if (myfile != null)
			{
				dups = true;
			}
		});

		if (dups)
		{
			portal_tool.dialogs.openDialogButtons("e3_filer_template_confirmUpload",
			[
				{text: _me._i18n.lookup("button_replace", "Replace"), click: function(){_me._sendFiles(files, true); return true;}},
				{text: _me._i18n.lookup("button_keep", "Keep All"), click: function(){_me._sendFiles(files, false); return true;}}
			]);
		}
		else
		{
			_me._sendFiles(files, false);
		}
	};

	this._uploadFile = function(file)
	{
		if (file == null) return;
		var dups = false;
		var myfile = _me._findFileByName(file.name);
		if (myfile != null)
		{
			dups = true;
		}

		if (dups)
		{
			portal_tool.dialogs.openDialogButtons("e3_filer_template_confirmUpload",					
			[
				{text: _me._i18n.lookup("button_replace", "Replace"), click: function(){_me._sendFile(file, true); return true;}},
				{text: _me._i18n.lookup("button_keep", "Keep All"), click: function(){_me._sendFile(file, false); return true;}}
			]);
		}
		else
		{
			_me._sendFile(file, false);
		}
	};

	this._sendFile = function(file, replace)
	{
		var params = portal_tool.cdp.params();
		params.url.count = "1";
		params.post.replace = replace ? "1" : "0";
		params.post.file_0 = file;
		portal_tool.cdp.request("myfiles_upload myfiles_get", params, function(data)
		{
			_me._files = data.myfiles || [];
			_me._populate();
		});
	};

	this._sendFiles = function(files, replace)
	{
		var params = portal_tool.cdp.params();
		params.url.count = files.length.toString();
		params.post.replace = replace ? "1" : "0";
		$.each(files, function(index, file)
		{
			params.post["file_" + index] = file;
		});

		portal_tool.cdp.request("myfiles_upload myfiles_get", params, function(data)
		{
			_me._files = data.myfiles || [];
			_me._populate();
		});
	};

	this._startReplace = function(file)
	{
		if (file == null)
		{
			var ids = _me._ui.table.selected();

			if (ids.length == 0)
			{
				portal_tool.dialogs.openAlert("e3_filer_select1First");
				return;
			}
			file = _me._findFileById(ids[0]);
		}

		if (file == null) return;

		var input = $("<input type='file' />");
		onChange(input, function()
		{
			var fl = $(input).prop("files");
			_me._replace(file, fl[0]);
		});

		$(input).click();
	};

	this._replace = function(file, upload)
	{
		if (upload == null) return;

		// check for name conflict
		var myfile = _me._findFileByName(upload.name);
		if ((myfile != null) && (myfile.id != file.id))
		{
			portal_tool.dialogs.openAlert("e3_filer_replaceConflict");
			return;
		}

		var params = portal_tool.cdp.params();
		params.url.id = file.id;
		params.post.file = upload;

		portal_tool.cdp.request("myfiles_replace myfiles_get", params, function(data)
		{
			_me._files = data.myfiles || [];
			_me._populate();
		});
	};

	this._startRename = function(file, titleTd)
	{
		// file null, select from table...
		if (file == null)
		{
			var ids = _me._ui.table.selected();

			if (ids.length == 0)
			{
				portal_tool.dialogs.openAlert("e3_filer_select1First");
				return;
			}
			file = _me._findFileById(ids[0]);
			
			if (file != null)
			{
				titleTd = _me._ui.e3_filer_template_filesTable.find("td[fileid=" + file.id + "]");
			}
		}

		if (file == null) return;

		var save = $("<a />",{href: "", title: _me._i18n.lookup("a_save", "Save", "title")})
			.addClass("e3_inlineControl")
			.css({backgroundImage: "url('/ui/icons/accept.png')", top:8, right:20});
		var cancel = $("<a />",{href: "", title: _me._i18n.lookup("a_cancel", "Cancel", "title")})
			.addClass("e3_inlineControl")
			.css({backgroundImage: "url('/ui/icons/cancel.png')", top:8, right:0});

		titleTd.css({position: "relative"});
		titleTd.append(save);
		titleTd.append(cancel);

		onClick(save, function(){_me._rename(file, titleTd);});
		onClick(cancel, function(){});

		titleTd.find("div")
			.attr("contenteditable", true)
			.attr("spellcheck", false)
			.focus()
			.css({borderBottom: "1px solid #D8D8D8", outline: "none", paddingBottom: 4/*, width: (titleTd.find("div").width()-32)*/})
			.on("blur", function(){_me._scheduleCancelRename(file, titleTd);})
			;

//		$(size).addClass("e3_offstage");
		titleTd.find("div").off("keydown").on("keydown", function(event)
		{
			try
			{
				// enter
				if (event.keyCode == 13)
				{
					event.preventDefault();
					_me._rename(file, titleTd);
				}
				
				// escape or tab
				else if ((event.keyCode == 27) | (event.keyCode == 9))
				{
					event.preventDefault();
					_me._cancelRename(file, titleTd);
				}
				
				// ignore /
				else if (event.keyCode == 191)
				{
					event.preventDefault();
				}
			}
			catch (e)
			{
				error(e);
			}
		});
	};

	this._cancelRenameTimer = null;
	this._scheduleCancelRename = function(file, titleTd)
	{
		if (_me._cancelRenameTimer != null) return;
		_me._cancelRenameTimer = setTimeout(function(){_me._cancelRename(file, titleTd);}, 100);
	};

	this._lastRename = null;
	this._cancelRename = function(file, titleTd)
	{
		if (_me._cancelRenameTimer != null)
		{
			clearTimeout(_me._cancelRenameTimer);
			_me._cancelRenameTimer = null;
		}

		_me._lastRename = trim(titleTd.find("div").text());
		titleTd.find("a").remove();
		titleTd.find("div")
			.attr("contenteditable", false)
			.text(file.file.name)
			.off("keydown")
			.off("blur")
			.css({borderBottom: "none"/*, width: (titleTd.find("div").width()+32)*/})
			.blur();
	};

	this._rename = function(file, titleTd)
	{
		if (_me._cancelRenameTimer != null)
		{
			clearTimeout(_me._cancelRenameTimer);
			_me._cancelRenameTimer = null;
		}

		var newName = null;

		// already canceled
		if (titleTd.find("a").length == 0)
		{
			newName = _me._lastRename;
		}

		// need to cancel
		else
		{
			newName = trim(titleTd.find("div").text());
			_me._cancelRename(file, titleTd);
		}

		// check name
		if (newName == null)
		{
			portal_tool.dialogs.openAlert("e3_filer_renameConflict");
			return;
		}
		var myfile = _me._findFileByName(newName);
		if ((myfile != null) && (myfile.id != file.id))
		{
			portal_tool.dialogs.openAlert("e3_filer_renameConflict");
			return;
		}

		var params = portal_tool.cdp.params();
		params.url.id = file.id;
		params.post.name = newName;

		// rename the file
		portal_tool.cdp.request("myfiles_rename myfiles_get", params, function(data)
		{
			_me._files = data.myfiles || [];
			_me._populate();
		});
	};

	this.enable = function(onChange)
	{
		_me._onChange = onChange;
		if (_me._enabled) return;

		_me._load();

		_me._enabled = true;
	};

	this.disable = function()
	{
		if (!_me._enabled) return;

		_me._enabled = false;
	};

	this.isEnabled = function()
	{
		return _me._enabled;
	};

	this.set = function(file)
	{
		_me._selected = file;
	};

	this.get = function()
	{
		return _me._selected;
	};

	try
	{
		this._init();
	}
	catch (e)
	{
		error(e);
	}
};

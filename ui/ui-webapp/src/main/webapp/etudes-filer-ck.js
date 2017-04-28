/**********************************************************************************
 * $URL: https://source.etudes.org/svn/serenity/trunk/ui/ui-webapp/src/main/webapp/etudes-filer-ck.js $
 * $Id: etudes-filer-ck.js 12455 2016-01-06 01:44:24Z ggolden $
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

// Object used to build CKFinder myfiles based pickers
// Note: all ids are reference ids, not raw file ids
function e3_FilerCK(ui, options)
{
	var _me = this;

	this._target =  ((ui == null) ? null : (($.type(ui) === "string") ? $("#" + ui) : ui));

	this._finderApi = null;
	this._selected = null;
	this._onChange = null;
	this._enabled = false;
	this._nameStartsAtPartIndex = (((options !== undefined) && (options.nameStartsAtPartIndex !== undefined)) ? options.nameStartsAtPartIndex : 7);	// default for ??? homepage?  Used for Etudes (not Serenity) FS

//	this._findInFinder = function(file)
//	{
//		if ((_me._finderApi == null) || (_me._finderApi.files === undefined)) return null;
//
//		for (var i = 0; i < _me._finderApi.files.length; i++)
//		{
//			if (file.name == _me._finderApi.files[i].name) return _me._finderApi.files[i];
//		}
//		return null;
//	};

	this._init = function()
	{
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

	this._adjustFinder = function()
	{
		// disable maximize
		$(_me._finderApi.document).find(".cke_button_maximize").remove();

		// license warning in files panel
		$(_me._finderApi.document).find("#files_view").find("h4.message_content").css({display:"none"});

		// folders_view_label
		// $(_me._finderApi.document).find("iframe").contents().find("#folders_view_label").css({color:"white"});
	};

	this._selectActionFunction = function(fileUrl, data, allFiles)
	{
		// console.log("_selectActionFunction", fileUrl, data, allFiles /*, file, _me._files*/);
		if (data.custom !== undefined)
		{
			_me._selected = {refId: data.custom.refId, name: data.custom.fileName, url: fileUrl};
		}
		else
		{
			var parts = fileUrl.split("/");
			var name = (parts.length >= _me._nameStartsAtPartIndex) ? parts[_me._nameStartsAtPartIndex-1] : "";
			for (var i = _me._nameStartsAtPartIndex; i < parts.length; i++)
			{
				name = name + "/" + parts[i];
			}
			_me._selected = {name: name, url: fileUrl};
		}

		_me._changed();
	};

	// fs: 0 - homepage, 1 - CHS/resources, 2 - mneme private docs, 3 - melete private docs, 4 - user's myfiles, 9 - serenity
	this._enablePopup = function(fs, type)
	{
		if (_me._finderApi != null) _me._finderApi.destroy();
		var f = new CKFinder({}, _me._adjustFinder);
		f.basePath = "/ckfinder/";
		f.selectActionFunction = _me._selectActionFunction;

		if (type === undefined) type = "Files";

		var connectorPath = "/connector";
		var startupPath = "/";
		if (fs == 0)
		{
			connectorPath = "/resources/connector";
			startupPath = portal_tool.info.site.title + " Files:/Home/";
		}
		else if (fs == 1)
		{
			connectorPath = "/sakai-ck-connector/web/editor/filemanager/browser/default/connectors/jsp/connector";
			startupPath = "/";
		}
		else if (fs == 2) // TODO:
		{
			connectorPath = "/sakai-ck-connector/web/editor/filemanager/browser/default/connectors/jsp/connector";
			startupPath = "/";
		}
		else if (fs == 3) // TODO:
		{
			connectorPath = "/sakai-ck-connector/web/editor/filemanager/browser/default/connectors/jsp/connector";
			startupPath = "/";
		}
		else if (fs == 4) // TODO:
		{
			connectorPath = "/sakai-ck-connector/web/editor/filemanager/browser/default/connectors/jsp/connector";
			startupPath = "/";
		}

		var siteId = ((portal_tool.info.site != null) ? portal_tool.info.site.id : "-");
		if (fs == 4)
		{
			siteId = "~" + portal_tool.info.id;
		}

		f.connectorInfo = "siteId=" + siteId + "&rtype=" + type; // TODO:
		f.connectorPath = connectorPath;
		f.startupPath = startupPath;
		f.disableHelpButton = true;
		f.rememberLastFolder = false;
		f.startupFolderExpanded = false; // true if we want the startupPath folder opened up
		f.defaultViewType = ((fs == 9) ? "thumbnails" : "list");
		f.defaultSortBy = "filename";
		f.disableThumbnailSelection = true;
		f.disableHelpButton = true;
		f.rememberLastFolder = false;
		f.removePlugins = 'basket';
		f.selectMultiple = true;
		f.showContextMenuArrow = true;
		f.resourceType = type;

		_me._finderApi = f.popup();
	};

	this._enableAppend = function(fs, type)
	{
		if (_me._finderApi != null) _me._finderApi.destroy();
		var f = new CKFinder({}, _me._adjustFinder);
		f.basePath = "/ckfinder/";
		f.selectActionFunction = _me._selectActionFunction;

		if (type === undefined) type = "Files";

		var connectorPath = "/connector";
		var startupPath = "/";
		if (fs == 0)
		{
			connectorPath = "/resources/connector";
			startupPath = portal_tool.info.site.title + " Files:/Home/";
		}
		else if (fs == 1)
		{
			connectorPath = "/sakai-ck-connector/web/editor/filemanager/browser/default/connectors/jsp/connector";
			startupPath = "/";
		}
		else if (fs == 2) // TODO:
		{
			connectorPath = "/sakai-ck-connector/web/editor/filemanager/browser/default/connectors/jsp/connector";
			startupPath = "/";
		}
		else if (fs == 3) // TODO:
		{
			connectorPath = "/sakai-ck-connector/web/editor/filemanager/browser/default/connectors/jsp/connector";
			startupPath = "/";
		}
		else if (fs == 4) // TODO:
		{
			connectorPath = "/sakai-ck-connector/web/editor/filemanager/browser/default/connectors/jsp/connector";
			startupPath = "/";
		}

		var siteId = ((portal_tool.info.site != null) ? portal_tool.info.site.id : "-");
		if (fs == 4)
		{
			siteId = "~" + portal_tool.info.id;
		}

		_me._finderApi = f.appendTo(_me._target[0],
		{
			connectorInfo: "siteId=" + siteId + "&rtype=" + type, // TODO:
			connectorPath: connectorPath,
			startupPath:  startupPath,
			disableHelpButton: true,
			rememberLastFolder: false,
			startupFolderExpanded: false, // true if we want the startupPath folder opened up
			defaultViewType: ((fs == 9) ? "thumbnails" : "list"),
			defaultSortBy: "filename",
			disableThumbnailSelection: true,
			disableHelpButton: true,
			rememberLastFolder: false,
			removePlugins: 'basket',
			selectMultiple: true,
			showContextMenuArrow: true,
			resourceType: type
//			gallery_autoLoad: false
		});
	};

	this.enable = function(onChange, fs, type)
	{
		if (_me._enabled) return;

		_me._onChange = onChange;
		_me._enabled = true;

		// default to serenity fs
		if (fs === undefined) fs = 9;
		
		if (_me._target != null)
		{
			_me._enableAppend(fs, type);
		}
		else
		{
			_me._enablePopup(fs, type);
		}
	};

	this.disable = function()
	{
		if (!_me._enabled) return;
		if (_me.finderApi != null)
		{
			_me._finderApi.destroy();
			_me._finderApi = null;
		}

		if (_me._target != null) _me._target.empty();
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

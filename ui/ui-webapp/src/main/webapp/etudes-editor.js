/**********************************************************************************
 * $URL: https://source.etudes.org/svn/serenity/trunk/ui/ui-webapp/src/main/webapp/etudes-editor.js $
 * $Id: etudes-editor.js 11740 2015-10-01 18:43:09Z ggolden $
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

// Object used to build html content editors
function e3_Editor(targetId, options)
{
	var _me = this;

	this._target = ($.type(targetId) === "string") ? $("#" + targetId) : targetId;
	this._tools = null;
	this._editor = null;
	this._picker = null;
	this._myfilesUserId = null;
	this._toolButtons = {};
	this._enabled = false;
	this._onChange = null;

	this._i18n = new e3_i18n(etudes_editor_i10n, "en-us");
	this._i18n.init();

	// TODO: 
	this._options = 
	{
		sites: "T", // F, T, H
		sidebar: true,
		autoSidebar: true,
		editor: 
		{
			size:"S", // S, L
			shape:"R", // S, R
			outline:"F" // T, F
		}
	};

	this._commands =
	[
		{key:"undo", dflt:"undo", icon:"action-undo", cmd:"undo"},
		{key:"redo", dflt:"redo", icon:"action-redo", cmd:"redo"},
		{key:"selectall", dflt:"select all", icon:"infinity", cmd:"selectAll"},
		{key:"heading", dflt:"heading", icon:"header", special:function(a)
			{
				$(a).contextPopup(
				{
					event: "click",
					// title: _me._i18n.lookup("label_styles", "Styles"),
					items:
					[
				       {label:_me._i18n.lookup("style_h1", "H1"), action:function(){_me._command("formatBlock","h1");return false;}},
				       {label:_me._i18n.lookup("style_h2", "H2"), action:function(){_me._command("formatBlock","h2");return false;}},
				       {label:_me._i18n.lookup("style_h3", "H3"), action:function(){_me._command("formatBlock","h3");return false;}},
				       {label:_me._i18n.lookup("style_h4", "H4"), action:function(){_me._command("formatBlock","h4");return false;}},
				       {label:_me._i18n.lookup("style_h5", "H5"), action:function(){_me._command("formatBlock","h5");return false;}},
				       {label:_me._i18n.lookup("style_h6", "H6"), action:function(){_me._command("formatBlock","h6");return false;}},
				       null,
				       {label:_me._i18n.lookup("style_pre", "pre"), action:function(){_me._command("formatBlock","pre");return false;}},
				       {label:_me._i18n.lookup("style_div", "div"), action:function(){_me._command("formatBlock","div");return false;}},
				       {label:_me._i18n.lookup("style_p", "p"), action:function(){_me._command("formatBlock","p");return false;}}
					]
				});
			}},
		{key:"bold", dflt:"bold", icon:"bold", cmd:"bold"},
		{key:"italic", dflt:"italic", icon:"italic", cmd:"italic"},
		{key:"underline", dflt:"underline", icon:"underline", cmd:"underline"},
		{key:"strikethrough", dflt:"strike through", icon:"strikethrough", cmd:"strikeThrough"},
		{key:"subscript", dflt:"subscript", icon:"arrow-bottom", cmd:"subscript"},
		{key:"superscript", dflt:"superscript", icon:"arrow-top", cmd:"superscript"},
		{key:"removeFormat", dflt:"clear", icon:"bolt", cmd:"removeFormat"},
		{key:"image", dflt:"image", icon:"image", click:function(){_me._image();}},
		{key:"emoji", dflt:"emoji", icon:"star", click:function(){_me._emoji();}},
		{key:"hr", dflt:"hr", icon:"minus", cmd:"insertHorizontalRule"},
		{key:"link", dflt:"link", icon:"link-intact", click:function(){_me._link();}, special:function(a){_me._toolButtons["createLink"] = a;}},
		{key:"unlink", dflt:"unlink", icon:"link-broken", cmd:"unlink"},
		{key:"alignleft", dflt:"align left", icon:"align-left", cmd:"justifyLeft"},
		{key:"aligncenter", dflt:"align center", icon:"align-center", cmd:"justifyCenter"},
		{key:"alignright", dflt:"align right", icon:"align-right", cmd:"justifyRight"},
		{key:"alignfull", dflt:"justify", icon:"justify-center", cmd:"justifyFull"},
		{key:"indent", dflt:"increase indent", icon:"account-login", cmd:"indent"},
		{key:"outdent", dflt:"decrease indent", icon:"account-logout", cmd:"outdent"},		
		{key:"print", dflt:"print", icon:"print", click:function(){_me._print();}},
		{key:"source", dflt:"source", icon:"code", click:function(){_me._show();}}
	];

	this._init = function()
	{
		_me._tools = $("<div />");
		_me._tools.addClass("e3_editor_toolbar");
		_me._editor = $("<div />");
		_me._editor.addClass("e3_content");
		
		_me._target.empty();
		_me._target.append(_me._tools);
		_me._target.append(_me._editor);

		$.each(_me._commands, function(index, command)
		{
			a = $("<a />", {href:"", title:_me._i18n.lookup(command.key, command.dflt, "title"), "class":"e3_editorTool"});
			a.addClass("e3_icon_" + command.icon + ((_me._options.editor.size == "L") ? "_16x16" : "_8x8"));
			if (command.special != null)
			{
				command.special(a);
			}
			if (command.cmd != null)
			{
				onClick(a, function(){_me._command(command.cmd);});
				_me._toolButtons[command.cmd] = a;
			}
			else if (command.click != null)
			{
				onClick(a, command.click);
			}
			_me._tools.append(a);			
		});
		
		if ($("#editor_picker").length == 0)
		{
			var dialogDiv = $("<div />", {id:"editor_picker", title:_me._i18n.lookup("picker", "Select", "title"), style:"display:none;"});
			$("#portal_content").append(dialogDiv);
			var instructionsDiv = $("<div />", {"class":"e3_dialogBody"}).html(_me._i18n.lookup("picker", "Select to insert"));
			$(dialogDiv).append(instructionsDiv);
			var div = $("<div />", {id:"editor_picker_gallery"});
			$(dialogDiv).append(div);
		}
		_me._picker = $("#editor_picker_gallery");

		if ($("#editor_emoji").length == 0)
		{
			var dialogDiv = $("<div />", {id:"editor_emoji", title:_me._i18n.lookup("picker", "Select", "title"), style:"display:none;"});
			$("#portal_content").append(dialogDiv);
			var div = $("<div />", {id:"editor_emoji_gallery", "class":"e3_dialogBody", style:"font-size:24px;"});
			$(dialogDiv).append(div);

			// the emoticons block
			for (var code = 0x1F600; code <= 0x1F64F; code++)
			{
				// skip these
				if ((code >= 0x1F641) && (code <= 0x1F644)) continue;

				_me._setPickerChar(div, code, "editor_emoji");
			}

			// the Transport and Map Symbols Block
			$(div).append("<hr />");
			for (var code = 0x1F680; code <= 0x1F6C5; code++)
			{
				// skip these
				if ((code >= 0x1F641) && (code <= 0x1F644)) continue;

				_me._setPickerChar(div, code, "editor_emoji");
			}

			// the Miscellaneous Symbols and Pictographs Block
			$(div).append("<hr />");
			for (var code = 0x1F300; code <= 0x1F5FF; code++)
			{
				// skip these
				if (((code >= 0x1F321) && (code <= 0x1F32F))
						|| (code == 0x1F336)
						|| ((code >= 0x1F37D) && (code <= 0x1F37F))
						|| ((code >= 0x1F394) && (code <= 0x1F39F))
						|| ((code >= 0x1F3CB) && (code <= 0x1F3DF))
						|| ((code >= 0x1F3F1) && (code <= 0x1F3FF))
						|| (code == 0x1F43F)
						|| (code == 0x1F441)
						|| (code == 0x1F4F8)
						|| ((code >= 0x1F4FD) && (code <= 0x1F4FF))
						|| ((code >= 0x1F53E) && (code <= 0x1F54F))
						|| ((code >= 0x1F568) && (code <= 0x1F5FA))) continue;

				_me._setPickerChar(div, code, "editor_emoji");
			}
		}
		
		if ($("#editor_link").length == 0)
		{
			var dialogDiv = $("<div />", {id:"editor_link", title:_me._i18n.lookup("linker", "Link URL", "title"), style:"display:none;"});
			$("#portal_content").append(dialogDiv);
			var div = $("<div />", {id:"editor_linker_body", "class":"e3_dialogBody"});
			$(dialogDiv).append(div);
			var sect =  $("<div />", {"class":"e3_section"});
			$(div).append(sect);
			var fldSet = $("<fieldset />", {"class":"e3_horizontal"});
			$(sect).append(fldSet);
			var entryDiv =  $("<div />", {"class":"e3_entry"});
			$(fldSet).append(entryDiv);
			var label = $("<label />", {"class":"e3_label", "for":"editor_link_url"}).text("Link URL");
			$(entryDiv).append(label);
			var input = $("<input size='80' type='text' value='' class='e3_data' id='editor_link_url'/>");
			$(entryDiv).append(input);
		}
		
		if (_me._options.editor.shape == "R")
		{
			_me._tools.find(".e3_editorTool").addClass("round");
		}
		else
		{
			_me._tools.find(".e3_editorTool").addClass("square");
		}
		if (_me._options.editor.outline == "T")
		{
			_me._tools.find(".e3_editorTool").addClass("outline");
		}
		else
		{
			_me._tools.find(".e3_editorTool").addClass("noOutline");	
		}
	};

	this._setPickerChar = function(div, code, dialogId)
	{
		var html = "&#x" + code.toString(16) + ";"
		var a = $("<a />", {href:"", "class":"e3_toolUiLink", title:html}).html(html + " ");
		onClick(a, function()
		{
			_me._command("insertHtml", html);
			portal_tool.dialogs.close(dialogId);
		});
		$(div).append(a);
	};

	this._stealthCommand = function(cmd, arg)
	{
		// https://developer.mozilla.org/en-US/docs/Web/API/document.execCommand
		// https://dvcs.w3.org/hg/editing/raw-file/tip/editing.html#execcommand()
		// TODO: copy paste cut are all "insecure", may need our own clipboard
		try
		{
			document.execCommand(cmd, false, arg);
		}
		catch (err)
		{
			error(err);
		}
	};

	this._command = function(cmd, arg)
	{
		_me._stealthCommand(cmd, arg);

		_me._updateButtons();
		_me._changed();
		_me._editor.focus();
	};

	this._updateButtons = function()
	{
		// http://www.w3.org/TR/DOM-Level-2-Traversal-Range/ranges.html
		// https://dvcs.w3.org/hg/editing/raw-file/tip/editing.html
		var range = rangy.getSelection().getRangeAt(0);
		var container = range.commonAncestorContainer;
		if (container.nodeType == 3) {container = container.parentNode;}
//		console.log("rangy.getSelection",rangy.getSelection());
//		console.log("range",range);
//		console.log("container",container);
//		console.log("window.getSelection()", window.getSelection());
//		console.log("window.getSelection().toString()", window.getSelection().toString());
//		console.log("window.getSelection().getRangeAt(0)", window.getSelection().getRangeAt(0));
		
		var isAnchor = container.nodeName === "A";

		$(".e3_editorTool").removeClass("active");
		$.each(_me._toolButtons, function(name, value)
		{
			var state = document.queryCommandState(name);
			
			if (state && name == "underline" && isAnchor) state = false;

			if (state)
			{
				_me._toolButtons[name].addClass("active");
			}
		});

		if (isAnchor)
		{
			_me._toolButtons["unlink"].addClass("active");
		}
	};

	this._image = function()
	{
		$(_me._picker).empty();
		
		// get myFiles
		var params = portal_tool.cdp.params();
		if (_me._myfilesUserId != null)
		{
			params.url.id = _me._myfilesUserId;
		}
		params.url.type = "image/";
		portal_tool.cdp.request("myfiles_get", params, function(data)
		{
			$.each(data.myfiles, function(index, file)
			{
				var figure = $("<figure />",{style:"display:inline-block;"});
				var caption = $("<figcaption />").html(file.name);
				var img = $("<img />", {src: file.downloadUrl, border:"0", alt:file.name, style:"width:64px; height:auto;"});
				$(figure).append(img);
				$(figure).append(caption);
				var a = $("<a />", {href:"", class:"e3_toolUiLinkU"});
				onClick(a, function()
				{
					var html = "<img class='e3_editorImage' id='e3_editorNewImage' src='" + file.downloadUrl + "' />";
					_me._command("insertHtml", html);

					var img = $("#e3_editorNewImage").removeAttr("id");
					_me._setImageContextMenu(img);

					portal_tool.dialogs.close("editor_picker");
				});
				$(a).append(figure);
				$(_me._picker).append(a);
				if ((index+1) % 4 == 0) $(_me._picker).append("<br />");
			});
		});

		var sel = rangy.getSelection();
		var range = sel.getRangeAt(0);
		portal_tool.dialogs.openAlert("editor_picker", function()
		{
			_me._editor.focus();
			sel.removeAllRanges();
			sel.addRange(range);
		});
		return false;
	};

	this._setImageContextMenu = function(img)
	{
		$(img).contextPopup(
		{
			title: _me._i18n.lookup("label_actions", "Actions"),
			items:
			[
		       {label:_me._i18n.lookup("action_undo", "Undo"), icon:'/ui/icons/action-undo-2x.png', action:function(){_me._command("undo");return false;}},
		       null,
		       {label:_me._i18n.lookup("action_cut", "Cut"), icon:'/ui/icons/flag_green.png', action:function(){_me._command("undo");return false;}},
		       {label:_me._i18n.lookup("action_copy", "Copy"), icon:'/ui/icons/magnifier.png', action:function(){_me._command("undo");return false;}},
		       {label:_me._i18n.lookup("action_paste", "Paste"), icon:'/ui/icons/arrow_down.png', action:function(){_me._command("undo");return false;}},
		       {label:_me._i18n.lookup("action_delete", "Delete"), icon:'/ui/icons/arrow_down.png', action:function(){_me._command("undo");return false;}},
		       null,
		       {label:_me._i18n.lookup("action_selectAll", "Select All"), icon:'/ui/icons/ui-text-field-select.png', action:function(){_me._command("selectAll");return false;}},
		       null,
		       {label:_me._i18n.lookup("action_imageProperties", "Image Properties"), icon:'/ui/icons/file-2x.png', action:function(){_me._imageProperties(img);return false;}},
			]
		});
	};

	this._setAContextMenu = function(a)
	{
		$(a).contextPopup(
		{
			title: _me._i18n.lookup("label_actions", "Actions"),
			items:
			[
		       {label:_me._i18n.lookup("action_undo", "Undo"), icon:'/ui/icons/action-undo-2x.png', action:function(){_me._command("undo");return false;}},
		       null,
		       {label:_me._i18n.lookup("action_cut", "Cut"), icon:'/ui/icons/flag_green.png', action:function(){_me._command("undo");return false;}},
		       {label:_me._i18n.lookup("action_copy", "Copy"), icon:'/ui/icons/magnifier.png', action:function(){_me._command("undo");return false;}},
		       {label:_me._i18n.lookup("action_paste", "Paste"), icon:'/ui/icons/arrow_down.png', action:function(){_me._command("undo");return false;}},
		       {label:_me._i18n.lookup("action_delete", "Delete"), icon:'/ui/icons/arrow_down.png', action:function(){_me._command("undo");return false;}},
		       null,
		       {label:_me._i18n.lookup("action_selectAll", "Select All"), icon:'/ui/icons/ui-text-field-select.png', action:function(){_me._command("selectAll");return false;}},
		       null,
		       {label:_me._i18n.lookup("action_linkProperties", "Link Properties"),  icon:'/ui/icons/link-intact-2x.png', action:function(){_me._linkProperties(a);return false;}},
			]
		});
	};

	this._emoji = function()
	{
		var sel = rangy.getSelection();
		var range = sel.getRangeAt(0);
		portal_tool.dialogs.openAlert("editor_emoji", function()
		{
			_me._editor.focus();
			sel.removeAllRanges();
			sel.addRange(range);
		});
		return false;
	};
	
	this._link = function()
	{
		var sel = rangy.getSelection();
		var range = sel.getRangeAt(0);

		var container = range.commonAncestorContainer;
		if (container.nodeType == 3) container = container.parentNode;
		if (container.nodeName === "A")
		{
			$("#editor_link_url").val($(container).attr("href"));
		}
		else
		{
			$("#editor_link_url").val(sel.toString());
		}

		portal_tool.dialogs.openDialog("editor_link", _me._i18n.lookup("button_add", "Set"), function()
		{
			var url = $.trim($("#editor_link_url").val());
			_me._editor.focus();
			sel.removeAllRanges();
			sel.addRange(range);
			_me._command("createLink", url);
			
			_me._editor.find("a").each(function(index)
			{
				$(this).off("contextmenu");
				_me._setAContextMenu(this);
			});
			
			return true;
		}, function()
		{
			_me._editor.focus();
			sel.removeAllRanges();
			sel.addRange(range);
		});

		return false;
	};

	this._show = function()
	{
		console.log("contents", $.trim(_me._editor.html()));
		console.log("cleaned", _me.get());
		return false;
	};

	this._imageProperties = function(image)
	{
		console.log("image properties", image);
		console.log("image src", $(image).attr("src"));
		_me._changed();
	};

	this._linkProperties = function(a)
	{
		console.log("link properties", a);
		console.log("link href", $(a).attr("href"));
		_me._changed();
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

	this.enable = function(onChange, withFocus)
	{
		_me._onChange = onChange;
		if (_me._enabled) return;

		_me._enabled = true;
		_me._editor.addClass("e3_editor_body");
		_me._editor.css({height: (((options !== undefined) && (options.height !== undefined)) ? options.height : 600)});
		_me._editor.css("overflow", "auto");
		_me._editor.attr("contenteditable", true);
		_me._editor.on('drop', function(e){return _me._onDrop(e);});
		_me._editor.on('dragover', function(e){return _me._onDragover(e);});
		_me._tools.removeClass("e3_offstage");
		_me._editor.on('keyup', function(e){_me._onkeyup(); return true;});
		_me._editor.on('mouseup', function(e){_me._onmouseup(); return true;});
		
		_me._editor.contextPopup(
		{
			title: _me._i18n.lookup("label_actions", "Actions"),
			items:
			[
		       {label:_me._i18n.lookup("action_undo", "Undo"), icon:'/ui/icons/action-undo-2x.png', action:function(){_me._command("undo");return false;}},
		       null,
		       {label:_me._i18n.lookup("action_cut", "Cut"), icon:'/ui/icons/flag_green.png', action:function(){_me._command("cut"); return false;}},
		       {label:_me._i18n.lookup("action_copy", "Copy"), icon:'/ui/icons/magnifier.png', action:function(){_me._command("copy");return false;}},
		       {label:_me._i18n.lookup("action_paste", "Paste"), icon:'/ui/icons/arrow_down.png', action:function(){_me._command("paste");return false;}},
		       {label:_me._i18n.lookup("action_delete", "Delete"), icon:'/ui/icons/arrow_down.png', action:function(){_me._command("delete");return false;}},
		       null,
		       {label:_me._i18n.lookup("action_selectAll", "Select All"), icon:'/ui/icons/ui-text-field-select.png', action:function(){_me._command("selectAll");return false;}}
			]
		});
		_me._editor.find("img").each(function(index)
		{
			_me._setImageContextMenu(this);
		});
		_me._editor.find("a").each(function(index)
		{
			_me._setAContextMenu(this);
		});
		
		if ((withFocus === undefined) || withFocus) _me._editor.focus();
		_me._stealthCommand("styleWithCSS", true);
		_me._stealthCommand("insertBrOnReturn", false);
		_me._stealthCommand("defaultParagraphSeparator", "div");  // chrome, safari, not ff
	};

	this.disable = function()
	{
		if (!_me._enabled) return;

		_me._editor.blur();
		_me._editor.off("contextmenu");
		_me._editor.find("img").off("contextmenu");
		_me._editor.off('keyup');
		_me._editor.off('mouseup');
		_me._editor.off('dragover');
		_me._editor.off('drop');
		_me._editor.attr("contenteditable", false);
		_me._editor.prop("style").removeProperty("height");
		_me._editor.prop("style").removeProperty("width");
		_me._editor.prop("style").removeProperty("overflow");
		_me._editor.removeClass("e3_editor_body");
		_me._tools.addClass("e3_offstage");
		_me._enabled = false;
	};

	this.isEnabled = function()
	{
		return _me._enabled;
	};

	this.set = function(value)
	{
		_me._editor.empty();
		if (value != null)
		{
			_me._editor.html(value);
		}
	};

	this.get = function()
	{
		// return $.trim(_me._editor.html());
		return _me._postProcess();
	};

	this.myfilesUser = function(userId)
	{
		_me._myfilesUserId = userId;
	};

	this._entitizeSurrogates = function(source)
	{
		var rv = "";
		for (var i = 0; i < source.length; i++)
		{
			var cp = source.charCodeAt(i);
			if (0xD800 <= cp && cp <= 0xDBFF)
			{
				var hiSurrogate = cp;
				i++;
				var lowSurrogate = source.charCodeAt(i);
				cp = ((hiSurrogate - 0xD800) * 0x400) + (lowSurrogate - 0xDC00) + 0x10000;
				rv = rv + "&#x" + cp.toString(16) + ";";
			}

			else
			{
				rv = rv + String.fromCharCode(cp);
			}
		}

		return rv;
	};

	this._postProcess = function()
	{
		var div = _me._editor.clone();

		$(div).find(".e3_editorImage").removeAttr("class");

		$(div).find("a").each(function(index)
		{
			$(this).attr("target","_blank");
			$(this).off("contextmenu");
		});

		$(div).find("img").each(function(index)
		{
			$(this).off("contextmenu");
		});

		var rv = trim($(div).html());
		
		// if (rv.endsWith("<br>")) rv = rv.substring(0, rv.length-4);
		if (rv == "<br>") rv = null;
		// return $.trim(_me._entitizeSurrogates(_me._editor.html()));

		return rv;
	};

	// opens a visible window, otherwise works
	this._printA = function()
	{
		// http://stackoverflow.com/questions/2255291/print-the-contents-of-a-div
		var w = window.open('', '', 'height=400,width=600');
        w.document.write('<html><head><title></title></head><body>');
        w.document.write(_me.get());
        w.document.write('</body></html>');
        w.document.close();
        w.focus();
        w.print();
        w.close();		
	};

	// takes over the portal window, otherwise works
	this._printB = function()
	{
		var oldstr = document.body.innerHTML;
		document.body.innerHTML = "<html><head><title></title></head><body>"
			+ _me.get()
			+  "</body></html>";
		window.print();
		document.body.innerHTML = oldstr;
	}

	this._print = function()
	{
		var frameId = "F" + Math.floor(Math.random() * (100000 - 1)) + 1;
		var iframe = $("<iframe>").attr("id", frameId).css("width","0px").css("height","0px").css("border","0").appendTo(_me._tools);

		iframe[0].contentWindow.document.write("</head><body onload=\"setTimeout(function(){Print(); Close();},100);\" >");
		iframe[0].contentWindow.document.write(_me.get());
		iframe[0].contentWindow.document.write("<script type='text/javascript'>function Print(){window.print();}; function Close(){window.parent.document.getElementById('" 
				+ frameId + "').parentNode.removeChild(window.parent.document.getElementById('" 
				+ frameId + "'));};</script>");
		iframe[0].contentWindow.document.write('</body></html>');
		iframe[0].contentWindow.document.close();				
	};

	this._onkeyup = function()
	{
		_me._changed();
		_me._updateButtons();
		return true;
	};

	this._onmouseup = function()
	{
		_me._updateButtons();
		// https://developer.mozilla.org/en-US/docs/Web/API/Selection
		// https://developer.mozilla.org/en-US/docs/Web/API/Range
		// https://developer.mozilla.org/en-US/docs/Web/API/Node
		// console.log("MOUSE UP", window.getSelection().toString(), window.getSelection().getRangeAt(0), window.getSelection().getRangeAt(0).startContainer, window.getSelection().getRangeAt(0).startContainer.parentNode, window.getSelection().getRangeAt(0).startContainer.nextSibling, _me.get());
		// http://msdn.microsoft.com/en-us/library/ie/ms533049%28v=vs.85%29.aspx
		// console.log("BOLD: ", document.queryCommandState("Bold"));
		return true;
	};

	this._onDrop = function(e)
	{
		// don't let the portal's blanked blocker get this
		e.stopPropagation();

		// does it contain text/plain or text/html?
		var isText = false;
		$.each(e.originalEvent.dataTransfer.types, function(index, type)
		{
			if (type == 'text/html' || type == 'text/plain') isText = true;
		});

		// block non text
		if (!isText)
		{
			e.preventDefault();
			return false;
		}
		// TODO: accept images into myfiles
		
		return true;
	};
	
	this._onDragover = function(e)
	{
		// don't let the portal's blanked blocker get this
		e.stopPropagation();

		return true;
	}

	try
	{
		this._init();
	}
	catch (e)
	{
		error(e);
	}
};

/**********************************************************************************
 * $URL: https://source.etudes.org/svn/serenity/trunk/ui/ui-webapp/src/main/webapp/etudes-dialog.js $
 * $Id: etudes-dialog.js 11693 2015-09-21 22:29:28Z ggolden $
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

// Support for Alert, Confirm and other Dialogs
function e3_Dialog()
{
	var _me = this;

	this.openBusy = function(ui, onClose)
	{
		var dialog = ($.type(ui) === "string") ? $("#" + ui) : ui;
		dialog.dialog(
		{
			dialogClass: 'e3_dialog no_close',
			closeOnEscape: false,
			autoOpen: true,
			modal: true,
			resizable: false,
			position: {my:"center top", at:"center top+32", of:"#portal_toolStage"},
			draggable: false,
			show: {effect: "fade"},
			hide: {effect: "fade"},
			height: "auto",
			width: "auto",
			close: function()
			{
				dialog.dialog("destroy");
	
				if (onClose)
				{
					try
					{
						onClose();					
					}
					catch (e)
					{
						error(e);
					}
				}
			},
			buttons:
			[
			]
		});
	};

	this.doneBusy = function(ui)
	{
		var dialog = ($.type(ui) === "string") ? $("#" + ui) : ui;
		dialog.dialog("option", {closeOnEscape: true, dialogClass:"", buttons:[
		{
			text: "OK",
			"class": "e3_primary_dialog_button",
			click: function()
			{
				dialog.dialog("close");
			}
		}]});
	};

	this.openAlert = function(ui, onClose, btns)
	{
		var dialog = ($.type(ui) === "string") ? $("#" + ui) : ui;
		if (btns == null)
		{
			btns = [];
		}
		btns.push({text: "OK", "class": "e3_primary_dialog_button", click:function(){dialog.dialog("close");}});

		dialog.dialog(
		{
			dialogClass: 'e3_dialog',
			autoOpen: true,
			modal: true,
			resizable: false,
			position: {my:"center top", at:"center top+32", of:"#portal_toolStage"},
			draggable: false,
			show: {effect: "fade"},
			hide: {effect: "fade"},
			height: "auto",
			width: "auto",
			close: function()
			{
				dialog.dialog("destroy");
	
				if (onClose)
				{
					try
					{
						onClose();					
					}
					catch (e)
					{
						error(e);
					}
				}
			},
			buttons: btns
		});
	};

	this.openConfirm = function(ui, doItName, doItFunction, cancelFunction)
	{
		var dialog = ($.type(ui) === "string") ? $("#" + ui) : ui;
		var todo = null;
		dialog.dialog(
		{
			dialogClass: 'e3_dialog',
			autoOpen: true,
			modal: true,
			resizable: false,
			position: {my:"center top", at:"center top+32", of:"#portal_toolStage"},
			draggable: false,
			show: {effect: "fade"},
			hide: {effect: "fade"},
			height: "auto",
			width: "auto",
			buttons:
			[{
				text: doItName,
				"class": "e3_primary_dialog_button",
				click: function()
				{
					todo = doItFunction;
					dialog.dialog("close");
				}
		 	},
			{
				text: "Cancel",
				click: function()
				{
					todo = cancelFunction;
					dialog.dialog("close");
				}
			}],
			close: function()
			{
				dialog.dialog("destroy");
	
				if (todo != null)
				{
					try
					{
						todo();
					}
					catch (e)
					{
						error(e);
					}
				}				
			}
		});
	};

	this.openDialog = function(dialogId, doItName, doItFunction, onClose)
	{
		var buttons = new Array();
		if (doItFunction != null)
		{
			var b = new Object();
			b.text = doItName;
			b.click = doItFunction;
			buttons.push(b);
		}
		this.openDialogButtons(dialogId, buttons, onClose);
	};

	// each button click function will close the dialog if it returns true, keep it open if it returns false
	this._wrapDialogFunction = function(dialog, func)
	{
		return function()
		{
			try
			{
				if (func())
				{
					dialog.dialog("close");
				}
			}
			catch (err)
			{
				error(err);
			}
			return false;
		};
	};

	this.openDialogButtons = function(ui, btns, onClose, cancelText)
	{
		var dialog = ($.type(ui) === "string") ? $("#" + ui) : ui;
		var buttons = new Array();
		if ((btns != null) && (btns.length > 0))
		{
			for (var i = 0; i < btns.length; i++)
			{
				var b = new Object();
				b.text = btns[i].text;
				b.click = _me._wrapDialogFunction(dialog, btns[i].click);
				buttons.push(b);
			}
		}
	
		var b = new Object();
		b.text = cancelText || "Cancel";
		b.click = function(){dialog.dialog("close"); return false;}; // TODO: need a return?
		buttons.push(b);
		
		buttons[0]["class"] = "e3_primary_dialog_button";
	
		dialog.dialog(
		{
			dialogClass: 'e3_dialog',
			autoOpen: true,
			modal: true,
			resizable: false,
			position: {my:"center top", at:"center top+32", of:"#portal_toolStage"},
			draggable: false,
			show: {effect: "fade"},
			hide: {effect: "fade"},
			buttons: buttons,
			height: "auto",
			width: "auto",
			close: function()
			{
				dialog.dialog("destroy");
	
				if (onClose)
				{
					try
					{
						onClose();					
					}
					catch (e)
					{
						error(e);
					}
				}
			}
		});
	};

	this.close = function(ui)
	{
		var dialog = ($.type(ui) === "string") ? $("#" + ui) : ui;
		dialog.dialog("close");
	};
};

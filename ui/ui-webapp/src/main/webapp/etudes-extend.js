/**********************************************************************************
 * $URL: https://source.etudes.org/svn/serenity/trunk/ui/ui-webapp/src/main/webapp/etudes-extend.js $
 * $Id: etudes-extend.js 12553 2016-01-14 20:03:28Z ggolden $
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

// Extend javascript and jquery with assorted useful features
 
jQuery.fn.reverse = [].reverse;

jQuery.Color.hook("fill stroke");

// stupid IE fix for Array indexOf being missing
if (!Array.prototype.indexOf)
{
	Array.prototype.indexOf = function(obj, start)
	{
		for (var i = (start || 0), j = this.length; i<j; i++)
		{
			if (this[i] == obj)
			{
				return i;
			}
		}
		return -1;
	};
}

if (!String.prototype.endsWith)
{
	String.prototype.endsWith = function(pattern)
	{
		var d = this.length - pattern.length;
		return d >= 0 && this.lastIndexOf(pattern) === d;
	};
}

if (!String.prototype.startsWith)
{
	String.prototype.startsWith = function(str)
	{
		return this.indexOf(str) == 0;
	};
}

if (!String.prototype.capitalize)
{
	String.prototype.capitalize = function()
	{
		return this.charAt(0).toUpperCase() + this.slice(1);
	};
}


if (!String.prototype.stripHtml)
{
	String.prototype.stripHtml = function()
	{
		return $("<p />").html(this).text();
	};
}

// add ":unckecked" selector
$.extend($.expr[':'],
{
	unchecked : function(obj)
	{
		return ((obj.type == 'checkbox' || obj.type == 'radio') && !$(obj).is(':checked'));
	}
});

function error(e)
{
	var msg = e.toString() + "\n" + e.stack;
	console.log(msg);
	
	var err = localStorage["error:report"];
	if (err != null)
	{
		err = err + msg;
	}
	else
	{
		err = msg;
	}
	localStorage["error:report"] = err;
};

function onChange(id, f)
{
	var target = null;
	if ($.type(id) === "string")
	{
		target = $("#" + id);
	}
	else
	{
		target = id;
	}
	$(target).off("change click keyup input paste").on("change click keyup input paste", function(event)
	{
		try
		{
			f(target, (event.type == "change"), event);
		}
		catch (e)
		{
			error(e);
		}
	});
};

function onEnter(id, f)
{
	var target = null;
	if ($.type(id) === "string")
	{
		target = $("#" + id);
	}
	else
	{
		target = id;
	}
	$(target).off("keyup").on("keyup", function(event)
	{
		try
		{
			if (event.keyCode == 13) f();
		}
		catch (e)
		{
			error(e);
		}
	});
};

function onClick(ui, f, result)
{
	if ($.type(ui) === "array")
	{
		$.each(ui, function(i, element){onClick(element, f, result);});
		return;
	}

	var target = ($.type(ui) === "string") ? $("#" + ui) : ui;
	$(target).off("click").on("click", function(event)
	{
		$(this).blur();
		try
		{
			f(event);
		}
		catch (e)
		{
			error(e);
		}
	
		return result != null ? result : false;
	});
};

function onHover(ui, fOn, fOff)
{
	if ($.type(ui) === "array")
	{
		$.each(ui, function(i, element){onHover(element, fOn, fOff);});
		return;
	}

	var target = ($.type(ui) === "string") ? $("#" + ui) : ui;

	target.hover(function()
	{
		try
		{
			fOn(target);
		}
		catch (err)
		{
			error(err);
		}
	}, function()
	{
		try
		{
			fOff(target);
		}
		catch (err)
		{
			error(err);
		}
	});
};

function onFocus(ui, fOn, fOff)
{
	if ($.type(ui) === "array")
	{
		$.each(ui, function(i, element){onFocus(element, fOn, fOff);});
		return;
	}

	var target = ($.type(ui) === "string") ? $("#" + ui) : ui;

	target.off("focus").on("focus", function()
	{
		try
		{
			fOn(target);
		}
		catch (err)
		{
			error(err);
		}
	});

	target.off("blur").on("blur", function()
	{
		try
		{
			fOff(target);
		}
		catch (err)
		{
			error(err);
		}
	});
}

function isOdd(num)
{
	return (num % 2) == 1;
}

function show(ui, condition)
{
	if (ui == null) return;
	if ($.type(ui) === "array")
	{
		$.each(ui, function(i, element){show(element, condition);});
		return;
	}

	var target = ($.type(ui) === "string") ? $("#" + ui) : ui;
	if ((condition == null) || condition)
	{
		target.removeClass("e3_offstage");
	}
	else
	{
		target.addClass("e3_offstage");
	}
}

function fade(ui, condition)
{
	if (ui == null) return;
	if ($.type(ui) === "array")
	{
		$.each(ui, function(i, element){fade(element, condition);});
		return;
	}

	var target = ($.type(ui) === "string") ? $("#" + ui) : ui;
	if ((condition == null) || condition)
	{
		if (target.hasClass("e3_offstage"))
		{
			target.css({opacity: 0}).removeClass("e3_offstage");
			target.stop().animate({opacity:1}, Hover.quick);
		}
	}
	else
	{
		if (!target.hasClass("e3_offstage"))
		{
			target.stop().animate({opacity:0}, Hover.quick, function(){target.addClass("e3_offstage").css({opacity:1});});
		}
	}
}

function applyClass(className, ui, condition)
{
	if ($.type(ui) === "array")
	{
		$.each(ui, function(i, element){applyClass(className, element, condition);});
		return;
	}

	if ($.type(className) === "array")
	{
		$.each(className, function(i, cls){applyClass(cls, ui, condition);});
		return;
	}

	var target = ($.type(ui) === "string") ? $("#" + ui) : ui;
	if ((condition == null) || condition)
	{
		target.addClass(className);
	}
	else
	{
		target.removeClass(className);
	}
}

function hide(ui)
{
	if (ui == null) return;
	if ($.type(ui) === "array")
	{
		$.each(ui, function(i, element){hide(element);});
		return;
	}

	var target = ($.type(ui) === "string") ? $("#" + ui) : ui;
	target.addClass("e3_offstage");
}

function toggleShowHide(ui)
{
	if ($.type(ui) === "array")
	{
		$.each(ui, function(i, element){toggleShowHide(element);});
		return;
	}

	var target = ($.type(ui) === "string") ? $("#" + ui) : ui;
	if (target.hasClass("e3_offstage"))
	{
		target.removeClass("e3_offstage");
	}
	else
	{
		target.addClass("e3_offstage");
	}
}

function strongPassword(pw)
{
	if (pw == null) return false;
	if (pw.length < 8) return false;
	if (pw.search(/\d/) == -1) return false;
	if (pw.search(/(?=[A-Z])/) == -1) return false;
	if (pw.search(/(?=[a-z])/) == -1) return false;
	
	return true;
}

function trim(str)
{
	if (str == null) return null;
	var rv = $.trim(str);
	if (rv.length == 0) rv = null;
	return rv;
}

function trimToZero(str)
{
	if (str == null) return "";
	var rv = $.trim(str);
	if (rv.length == 0) rv = "";
	return rv;
}

function findIdInList(id, list)
{
	if (id.id !== undefined) id = id.id;

	for (i = 0; i < list.length; i++)
	{
		var listId = ((list[i].id !== undefined) ? list[i].id : list[i]);

		if (listId == id) return list[i];
	}

	return null; 
}

 function objectArraysDiffer(a, b)
{
	// all of a need to be in b, all of b need to be in a, sizes should match
	if (a.length != b.length) return true;
	
	for (var i = 0; i < a.length; i++)
	{
		if (findIdInList(a[i], b) == null) return true;
	}
	
	for (var i = 0; i < b.length; i++)
	{
		if (findIdInList(b[i], a) == null) return true;
	}
	
	return false;
};

// item is object with id field, or just id, list is of objects with id field, or just ids
function position(item, list)
{
	var i = 1;
	var found = null;
	var itemId = ((item.id !== undefined) ? item.id : item);
	$.each(list || [], function(index, itm)
	{
		var listId = ((itm.id !== undefined) ? itm.id : itm);
		if (listId == itemId) found = i;
		i++;
	});

	var notFound = (found == null);
	if (notFound) found = list.length+1;

	var rv = {};
	rv.item = found;
	rv.total = list.length + (notFound ? 1 : 0);
	rv.prev = found > 1 ? list[found-2] : null;
	rv.next = found < list.length ? list[found] : null;

	return rv;
}

function clone(toClone, toFind)
{
	var source = ($.type(toClone) === "string") ? $("#" + toClone) : toClone;
	var rv = {}
	rv.element = source.clone().removeAttr("id");
	$.each(toFind, function(index, id)
	{
		rv[id] = rv.element.find("#"+id).removeAttr("id");
	});
	
	return rv;
}

function findElements(names)
{
	var rv = {};
	$.each(names || [], function(index, name)
	{
		rv[name] = $("#" + name);
	});

	return rv;
}

function asPct(num, max)
{
	if (num == 0) return "0%";
	
	var pct = ((100 * num) / max);
	var rv = pct.toFixed(2);
	if (rv.endsWith(".00"))
	{
		rv = rv.substring(0, rv.length-3);
	}
	else if (rv.endsWith("0"))
	{
		rv = rv.substring(0, rv.length-1);
	}
	return rv + "%";
}

// to get an action that is setup with setupHoverControls back to the normal state.
function resetHoverControl(a)
{
	a.stop().css({color:a.e3_styleColor, backgroundColor:"", backgroundImage:""});
};

//for the actions in the action bar, and the controls in the item nav bar
function setupHoverControls(targets)
{
	$.each(targets, function(index, target)
	{
		target.e3_color = target.css("color");
		target.e3_styleColor = target[0].style.color;

		onHover(target,
			function(a)
			{
				if (a.attr("disabled")) return;
				a.css({backgroundImage:"none"});
				a.stop().animate({color:((a.attr("e3_hoverFg") !== undefined) ? a.attr("e3_hoverFg"): "white"), backgroundColor:((a.attr("e3_hoverBkg") !== undefined) ? a.attr("e3_hoverBkg") : "#898989")}, Hover.quick, function(){});
			},
			function(a)
			{
				if (a.attr("disabled")) return;
				a.css({backgroundImage:""});
				a.stop().animate({color:(((a.e3_color == "transparent") || (a.e3_color == "rgba(0, 0, 0, 0)")) ? "rgba(255,255,255, 0)" : ((a.attr("e3_hoverBkg") !== undefined) ? a.attr("e3_hoverBkg") : "#686868")), backgroundColor:((a.attr("e3_hoverFg") !== undefined) ? a.attr("e3_hoverFg") : "white")}, Hover.quick, function(){resetHoverControl(a);});
			});
	});
};

function isolateFileExtensionFromUrl(target)
{
	try
	{
		var url = target;
		var rv = null;
		if (url.indexOf(".") != -1)
		{
			rv = (url = url.substr(1 + url.lastIndexOf("/")).split('?')[0]).substr(url.lastIndexOf("."));
		}
		return rv;
	}
	catch (err)
	{
		return false;
	}
};

function isImageMimeType(mime)
{
	if (mime.toLowerCase().startsWith("image/")) return true;
	return false;
};

function isImageUrl(url)
{
	var ext = isolateFileExtensionFromUrl(url);
	if (ext == null) return false;
	ext = ext.toLowerCase();
	if (".jpg" == ext) return true;
	if (".jpeg" == ext) return true;
	if (".png" == ext) return true;
	if (".gif" == ext) return true;
	return false;
};

function disableAction(item)
{
	item.addClass("e3_disabled");
	item.attr("disabled", true);
	resetHoverControl(item);
};

function enableAction(item)
{
	item.removeClass("e3_disabled");
	item.attr("disabled", false);
	resetHoverControl(item);
};

function enableActions(ui, condition)
{
	if (ui == null) return;
	if ($.type(ui) === "array")
	{
		$.each(ui, function(i, element){enableAction(element, condition);});
		return;
	}

	var target = ($.type(ui) === "string") ? $("#" + ui) : ui;
	if ((condition == null) || condition)
	{
		target.removeClass("e3_disabled");
		target.attr("disabled", false);
		resetHoverControl(target);
	}
	else
	{
		target.addClass("e3_disabled");
		target.attr("disabled", true);
		resetHoverControl(target);
	}
};

function scheduleStatusIcon(id)
{
	if (id == 1) return "/ui/icons/calendar.png";
	else if (id == 2) return "/ui/icons/calendar_delete.png";
	else if (id == 3) return "/ui/icons/publish.png";
	return "/ui/icons/closed.gif";
}

function contextMenu(a, menu, below)
{
	var contextMenu = {menu: [], below: ((below == null) ? a : below), right: true};
	$.each(menu, function(index, menuItem)
	{
		contextMenu.menu.push(menuItem);
	});

	onClick(a, function()
	{
		portal_tool.nav.toggleContextMenu(true, contextMenu);
	});
//	onHover(a, function()
//	{
//		// TODO: if we want this to fall down: portal_tool.nav.toggleContextMenu(true, contextMenu);
//		a.stop().animate({color:"black"}, Hover.on);
//	}, function()
//	{
//		a.stop().animate({color:"#686868"}, Hover.off);
//	});
}

function compareN(a, b, nullBig)
{
	if (nullBig === undefined) nullBig = true;
	if ((a == null) && (b == null)) return 0;
	if ((a == null) && (b != null)) return (nullBig ? 1 : -1);
	if ((a != null) && (b == null)) return (nullBig ? -1 : 1);
	if (a == b) return 0;
	if (a < b) return -1;
	return 1;
}

// true sorts first via ascending (i.e. is smaller)
function compareB(a, b, nullBig)
{
	if (nullBig === undefined) nullBig = true;
	if ((a == null) && (b == null)) return 0;
	if ((a == null) && (b != null)) return (nullBig ? 1 : -1);
	if ((a != null) && (b == null)) return (nullBig ? -1 : 1);
	if (a == b) return 0;
	if ((a ? 0 : 1) < (b ? 0 : 1)) return -1;
	return 1;
}

function compareS(aO, bO, nullBig)
{
	var a = ((aO == null) ? aO : aO.toLowerCase());
	var b = ((bO == null) ? bO : bO.toLowerCase());
	if (nullBig === undefined) nullBig = true;
	if ((a == null) && (b == null)) return 0;
	if ((a == null) && (b != null)) return (nullBig ? 1 : -1);
	if ((a != null) && (b == null)) return (nullBig ? -1 : 1);
	return a.localeCompare(b);
}

function dot(color, title, solid)
{
	var element = null;
	if (solid === undefined) solid = true;
	if (color != Dots.none)
	{
		if (color == Dots.red)
		{
			element = $('<svg width="32" height="32" viewbox="0 0 32 32"><circle cx="16" cy="16" r="7" ' + (solid ? 'fill="#E00000"' : 'stroke="#E00000" fill="white"') + ' /></svg>');
		}
		else if (color == Dots.gray)
		{
			element = $('<svg width="32" height="32" viewbox="0 0 32 32"><circle cx="16" cy="16" r="7" ' + (solid ? 'fill="#A0A0A0"' : 'stroke="#A0A0A0" fill="white"') + ' /></svg>');
		}
		else if (color == Dots.yellow)
		{
			element = $('<svg width="32" height="32" viewbox="0 0 32 32"><circle cx="16" cy="16" r="7" ' + (solid ? 'fill="#FFB000"' : 'stroke="#FFB000" fill="white"') + ' /></svg>');
		}
		else if (color == Dots.green)
		{
			element = $('<svg width="32" height="32" viewbox="0 0 32 32"><circle cx="16" cy="16" r="7" ' + (solid ? 'fill="#2AB31D"' : 'stroke="#2AB31D" fill="white"') + ' /></svg>');
		}
		else if (color == Dots.blue)
		{
			element = $('<svg width="32" height="32" viewbox="0 0 32 32"><circle cx="16" cy="16" r="7" ' + (solid ? 'fill="#0072C6"' : 'stroke="#0072C6" fill="white"') + ' /></svg>');
		}
		else if (color == Dots.hollow)
		{
			element = $('<svg width="32" height="32" viewbox="0 0 32 32"><circle cx="16" cy="16" r="7.5" stroke="#A0A0A0" fill="white" /></svg>');
		}
		else if (color == Dots.closed)
		{
			element = $('<svg width="32" height="32" viewbox="0 0 32 32"><circle cx="16" cy="16" r="7" stroke="#E00000" stroke-width="2" fill="white" /><line x1="11.5" y1="16" x2="20.25" y2="16" stroke-width="2" stroke="#E00000" /></svg>');
		}
		else if (color == Dots.complete)
		{
			// from icoMoon - Free checkmark.png, 24px, w/h adjusted to shrink down and stay centered to match the other dots
			element = $('<svg width="16" height="16" viewbox="0 0 24 24"><path fill="#2AB31D" d="M20.25 3l-11.25 11.25-5.25-5.25-3.75 3.75 9 9 15-15z" /></svg>');
		}
		else if (color == Dots.progress)
		{
			// from icoMoon - Free spinner9.png, 24px, w/h adjusted to shrink down and stay centered to match the other dots
			element = $('<svg width="16" height="16" viewbox="0 0 24 24"><path fill="#0072C6" d="M12 0c-6.533 0-11.847 5.221-11.996 11.718 0.139-5.669 4.449-10.218 9.746-10.218 5.385 0 9.75 4.701 9.75 10.5 0 1.243 1.007 2.25 2.25 2.25s2.25-1.007 2.25-2.25c0-6.627-5.373-12-12-12zM12 24c6.533 0 11.847-5.221 11.996-11.718-0.139 5.669-4.449 10.218-9.746 10.218-5.385 0-9.75-4.701-9.75-10.5 0-1.243-1.007-2.25-2.25-2.25s-2.25 1.007-2.25 2.25c0 6.627 5.373 12 12 12z"></path></svg>');
		}
		else if (color == Dots.alert)
		{
			// from icoMoon - Free notification.svg, 24px, w/h adjusted to shrink down and stay centered to match the other dots
			element = $('<svg width="16" height="16" viewbox="0 0 24 24"><path fill="#686868" d="M12 2.25c-2.604 0-5.053 1.014-6.894 2.856s-2.856 4.29-2.856 6.894c0 2.604 1.014 5.053 2.856 6.894s4.29 2.856 6.894 2.856c2.604 0 5.053-1.014 6.894-2.856s2.856-4.29 2.856-6.894c0-2.604-1.014-5.053-2.856-6.894s-4.29-2.856-6.894-2.856zM12 0v0c6.627 0 12 5.373 12 12s-5.373 12-12 12c-6.627 0-12-5.373-12-12s5.373-12 12-12zM10.5 16.5h3v3h-3zM10.5 4.5h3v9h-3z"></path></svg>');
		}
		else if (color == Dots.redAlert)
		{
			// from icoMoon - Free notification.svg, 24px, w/h adjusted to shrink down and stay centered to match the other dots
			element = $('<svg width="16" height="16" viewbox="0 0 24 24"><path fill="#E00000" d="M12 2.25c-2.604 0-5.053 1.014-6.894 2.856s-2.856 4.29-2.856 6.894c0 2.604 1.014 5.053 2.856 6.894s4.29 2.856 6.894 2.856c2.604 0 5.053-1.014 6.894-2.856s2.856-4.29 2.856-6.894c0-2.604-1.014-5.053-2.856-6.894s-4.29-2.856-6.894-2.856zM12 0v0c6.627 0 12 5.373 12 12s-5.373 12-12 12c-6.627 0-12-5.373-12-12s5.373-12 12-12zM10.5 16.5h3v3h-3zM10.5 4.5h3v9h-3z"></path></svg>');
		}
	}
	
	if ((element != null) && (title != null)) element.attr("title", title);

	return element;
}

function dotSmall(color, title, solid)
{
	var element = null;
	if (solid === undefined) solid = true;
	if (color != Dots.none)
	{
		if (color == Dots.red)
		{
			element = $('<svg width="16" height="16" viewbox="0 0 16 16"><circle cx="8" cy="8" r="5" ' + (solid ? 'fill="#E00000"' : 'stroke="#E00000" fill="white"') + ' /></svg>');
		}
		else if (color == Dots.gray)
		{
			element = $('<svg width="16" height="16" viewbox="0 0 16 16"><circle cx="8" cy="8" r="5" ' + (solid ? 'fill="#A0A0A0"' : 'stroke="#A0A0A0" fill="white"') + ' /></svg>');
		}
		else if (color == Dots.yellow)
		{
			element = $('<svg width="16" height="16" viewbox="0 0 16 16"><circle cx="8" cy="8" r="5" ' + (solid ? 'fill="#FFB000"' : 'stroke="#FFB000" fill="white"') + ' /></svg>');
		}
		else if (color == Dots.green)
		{
			element = $('<svg width="16" height="16" viewbox="0 0 16 16"><circle cx="8" cy="8" r="5" ' + (solid ? 'fill="#2AB31D"' : 'stroke="#2AB31D" fill="white"') + ' /></svg>');
		}
		else if (color == Dots.blue) //#0093FF
		{
			element = $('<svg width="16" height="16" viewbox="0 0 16 16"><circle cx="8" cy="8" r="5" ' + (solid ? 'fill="#0072C6"' : 'stroke="#0072C6" fill="white"') + ' /></svg>');
		}
	}
	
	if ((element != null) && (title != null)) element.attr("title", title);

	return element;
}

// value 0=red .. 1-green, lightness ~50 for bright, higher for less bright (leave out for default pale)
function rgColor(value, lightness)
{
	var color = "hsl(" + (value * 120) + ", 100%, " + ((lightness !== undefined) ? lightness : 95) + "%)";
	return color;
}

function processMathMl()
{
	com.wiris.plugin.viewer.EditorViewer.main();
}

function emptyTransients(target)
{
	target.find('[transient="true"]').remove();
};

var Role = {admin:6, anonymous:-1, authenticated:0, custom:-2, guest:1, observer:2, instructor:5, none:0, student:3, ta:4};

var ScheduleStatus = {closed:4, open:3, willOpen:1, willOpenHide:2};

var AccessStatus = {closed:2, open:0, willOpen:1};

var ProgressStatus = {none: 0, inprogress: 1, complete: 2};

var ToolItemType = {
	none: {id:0, title:""},
	forum: {id:1, title:"Discussion"},
	assignment: {id:3, title:"Assignment"},
	test: {id:5, title:"Test"},
	essay: {id:6, title:"Essay"},
	offline: {id:4, title:"Offline Item"},
	fce: {id:2002, title:"Evaluation"},
	survey: {id:2003, title:"Survey"},
	syllabus: {id:2004, title:"Syllabus"},
	module: {id:2005, title:"Module"},
	blog: {id:2, title:"Blog"},
	extra: {id:1001, title:"Extra Credit"},
	chat: {id:2001, title:"Chat"},
	event: {id:2006, title:"Event"},
	header: {id:2007, title:"Header"},

	byId: function(id)
	{
		for (var name in ToolItemType)
		{
			if (name == "byId") continue;
			if (ToolItemType[name].id == id) return ToolItemType[name];
		}
		return null;
	}
};

var EvaluationType = {official:1, peer:2, unknown:0};

var Tools = {
	dashboard:{id:54, title:"Dashboard", url:"/dashboard/dashboard", role:Role.authenticated},
	sites: {id:52, title:"MySites", url:"/site/mysites", role:Role.authenticated},
	files: {id:50, title:"Files", url:"/myfiles/myfiles", role:Role.authenticated},
	account: {id:51, title:"Account", url:"/user/account", role: Role.authenticated},
	logout: {id:-1, title:"Logout", url:null, role:Role.authenticated},
	browseSites: {id:9, title:"Browse Sites", url:"/site/browser", role:Role.anonymous},
	resetpw: {id:8, title:"Reset Password", url:"/user/resetpw", role:Role.anonymous},
	login: {id:10, title:"Login", url:"/user/login", role:Role.anonymous},
	announcement: {id:104, url: "/announcement/announcement", title:"Announcements", role:Role.guest},
	schedule: {id:103, url:"/schedule/schedule", title:"Calendar", role:Role.guest},
	assessment: {id:107, url:"/assessment/assessment",title: "Assessments", role:Role.guest},
	syllabus: {id:105, url:"/syllabus/syllabus", title:"Syllabus", role:Role.guest},
	module: {id:106, url:"/module/module", title:"Modules", role:Role.guest},
	home: {id:101, url:"/home/home", title:"Home", role:Role.guest},
	coursemap: {id:102, url:"/coursemap/coursemap", title:"Course Map", role:Role.guest},
	forum: {id:108, url:"/forum/forum", title:"Discussions", role:Role.guest},
	resource: {id:110, url:"/resource/resource", title:"Resources",  role:Role.guest},
//	social: {id:115, url:"/social/social", title:"Social", role:Role.guest},
	evaluation: {id:111, url:"/evaluation/evaluation", title:"Grades", role:Role.student},
	blog: {id:100, url:"/blog/blog", title:"Blogs", role:Role.guest},
	message: {id:117, url:"/social/social", title:"Messages", role:Role.guest},
	member: {id:116, url:"/social/social", title:"Members", role:Role.guest},
	presence: {id:98, url:"/social/social", title:"Online", role:Role.guest},
	siteroster: {id:114, url:"/roster/siteroster", title:"Site Roster", role:Role.instructor},
	sitesetup: {id:113, url:"/site/setup", title:"Site Setup", role:Role.instructor},
	chat: {id:109, url:"/chat/chat", title:"Chat", role:Role.guest},
	activity: {id:112, url:"/activity/activity", title:"Activity Meter", role:Role.instructor},
	user: {id:1, url:"/user/user", title: "Users", role:Role.admin},
	roster: {id:2, url:"/roster/roster", title:"Rosters", role:Role.admin},
	site: {id:6, url:"/site/site", title:"Sites", role:Role.admin},
	monitor: {id:7, url:"/monitor/monitor", title:"Monitor", role:Role.guest},

	byId: function(id)
	{
		for (var name in Tools)
		{
			if (name == "byId") continue;
			if (Tools[name].id == id) return Tools[name];
		}
		return null;
	}
};

var Hover = {on:200, off:200, quick:200, medium:300}; // off could be longer 600, quick?

var Dots = {red: "red", gray: "gray", yellow: "yellow", green: "green", hollow: "hollow", blue: "blue",
			closed: "closed", complete: "complete", progress: "progress", alert: "alert", redAlert: "redAlert", check: "check", none: null};

/* inter-tool requests */
var Actions = {perform: 1, review: 2, edit: 3};

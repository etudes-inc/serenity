/**********************************************************************************
 * $URL: https://source.etudes.org/svn/serenity/trunk/ui/ui-webapp/src/main/webapp/etudes-table.js $
 * $Id: etudes-table.js 12525 2016-01-12 23:21:52Z ggolden $
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

// Object used to build data tables
function e3_Table(ui)
{
	var _me = this;

	this._table = ($.type(ui) === "string") ? $("#" + ui) : ui;
	this._tableBody = this._table.find("tbody");
	this._tableHead = this._table.find("thead");
	this._selectId = null;
	this._onSelectChange = null;
	this._row = null;
	this._sortHeaders = null;
	this._sortList = null;
	this._selectAllHeaderIndex = null;
	this._externalHeaders = null;
	this._selectAll = null;
	this._zebraStripe = false;
	this._reorderAble = false;
	this._onReorder = null;
	this._reorderSet = false;
	this._onEmpty = null;

	this.setupSelection = function(selectId, onSelectChange)
	{
		_me._selectId = selectId;
		_me._onSelectChange = onSelectChange;
	};
	
	this.enableReorder = function(onReorder)
	{
		if (onReorder == null)
		{
			_me._reorderAble = false;
			_me._onReorder = onReorder;	
		}
		else
		{
			_me._reorderAble = true;
			_me._onReorder = onReorder;
		}
	};

	this.onBecomingEmpty = function(onEmpty)
	{
		this._onEmpty = onEmpty;
	};

	// public method
	this.clear = function()
	{
		_me._table.trigger("destroy");
		_me._tableBody.empty();
	};
	
	this.header = function(index, value)
	{
		_me._tableHead.find("tr > th:nth-child(" + index + ")").text(value);
	};
	
	this.selectAllHeader = function(index, externalHeaders)
	{
		_me._selectAllHeaderIndex = index;
		if (externalHeaders !== undefined) _me._externalHeaders = ($.type(externalHeaders) === "string") ? $("#" + externalHeaders) : externalHeaders;
		_me._selectAll = null;
	};

	this.noStripes = function()
	{
		this._zebraStripe = false;
	};

	this.row = function()
	{
		_me._row = $("<tr />");
		_me._tableBody.append(_me._row);

		if (_me._reorderAble)
		{
			_me._row.attr("tabindex", 0);
			_me._row.off("keydown").on("keydown", _me._keydown);
			_me._row.off("blur").on("blur", _me._rowBlur);
			_me._row.off("focus").on("focus", _me._rowFocus);		
		}

		if ((_me._reorderAble) && (_me.rowCount() > 1) && (!_me._reorderSet))
		{
			_me._reorderSet = true;
			_me._tableBody.sortable({axis: "y", containment: _me._tableBody, handle: ".reorderable", tolerance: "pointer", items: "tr:not(.noDrop)",
				helper: function(e, ui)
				{
					var o = ui.children();
					var h = ui.clone();
					h.children().each(function(index)
					{
						$(this).width(o.eq(index).width());
					});
					$(h).css("background-color","#F4FAFF");
					return h;
				},
				stop: function()
				{
					try
					{
						_me._onReorder(_me._tableBody.find("td.reorder").map(function(){return $(this).attr("oid");}).get());
						// TODO: then redo the select number dropdown for the reordering
					}
					catch (err)
					{
						error(err);
					}
				}});			
		}

		return _me._row;
	};
	
	this.hotRow = function(onClickSelect)
	{
		var row =  $("<tr />");
		_me._row = row;
		_me._tableBody.append(row);
		
		row.attr("tabindex", 0);
		row.addClass("e3_tableHotRow");
		row.off("keydown").on("keydown", function(event){try{return _me._keydownHotRow(event, row.prev("tr.e3_tableHotRow"), row.next("tr.e3_tableHotRow"), onClickSelect);}catch(err){error(err);}});
		row.off("blur").on("blur", _me._rowBlur);
		row.off("focus").on("focus", _me._rowFocus);		
		onClick(row, function()
		{
			_me._tableBody.find("tr.selected").removeClass("selected");
			row.addClass("selected");
			row.focus();
			if (onClickSelect !== undefined) onClickSelect();
		});

		return row;
	};

	this.disableRowReorder = function()
	{
		_me._row.attr("noReorder", "1");
		_me._row.removeAttr("tabindex");
	};

	this.disableRowReorderTarget = function()
	{
		_me._row.addClass("noDrop");
	};

	this.removeRow = function(row)
	{
		var prev = $(row).prev('tr')[0];
		var next = $(row).next('tr')[0];
		
		$(row).remove();
		
		if (prev != null)
		{
			$(prev).focus();
		}
		else if (next != null)
		{
			$(next).focus();
		}
		
		if ((_me._reorderSet) && (_me.rowCount() <= 1))
		{
			_me._reorderSet = false;
			_me._tableBody.sortable("destroy");
		}
		
		if ((_me.rowCount() == 0) && (_me._onEmpty != null))
		{
			_me._onEmpty();
		}
	};

	this.rowCount = function()
	{
		return _me._tableBody.find("tr:last").index() + 1;
	};

	this.selectBox = function(objectId, secondId)
	{
		var td = $("<td />");
		var div = $("<div />");
		div.addClass("center icon");

		if (objectId != null)
		{
			var input = $("<input />", {type: "checkbox", sid: _me._selectId, oid: objectId});
			input.css({margin:0});
			if (secondId != null) input.attr("oid2", secondId);
			div.append(input);
			input.click(function()
			{
				try
				{
					_me._updateSelectAll();
					if (_me._onSelectChange() != null) _me._onSelectChange();
				}
				catch (err)
				{
					error(err);
				}
			});
		}

		td.append(div);
		_me._row.append(td);
		return td;
	};

	this.select = function(attributes, options, specialClass, specialCss)
	{
		var td = $("<td />");
		
		var div = $("<div />");
		div.addClass("td");
		if (specialClass != null) div.addClass(specialClass);
		if (specialCss != null) div.css(specialCss);
	
		var s = $("<select />", attributes);
		$.each(options, function(index, option)
		{
			var o = $("<option />");
			o.attr("value", option.value);
			o.text(option.text);
			s.append(o)
		});

		div.append(s);
		td.append(div);
		_me._row.append(td);

		return td;
	};

	this.text = function(value, specialClass, specialCss)
	{
		var td = $("<td />");

		var div = $("<div />");
		div.addClass("td");
		if (specialClass != null) div.addClass(specialClass);
		if (specialCss != null) div.css(specialCss);
		div.text(value);

		td.append(div);
		_me._row.append(td);

		return td;
	};
	
	this.date = function(value, placeholder, specialClass, specialCss)
	{
		td = $("<td />");
		
		var div = $("<div />");
		div.addClass("td");
		if (specialClass != null) div.addClass(specialClass);
		if (specialCss != null) div.css(specialCss);
		if (specialClass == null) div.addClass("date");

		if (value == null)
		{
			div.html(placeholder || "");
		}
		else
		{
			div.text(portal_tool.timestamp.display(value));
		}
	
		td.append(div);
		_me._row.append(td);

		return td;
	};

	this.html = function(value, specialClass, specialCss)
	{
		td = $("<td />");
		
		var div = $("<div />");
		div.addClass("td");
		if (specialClass != null) div.addClass(specialClass);
		if (specialCss != null) div.css(specialCss);
		div.html(value);
	
		td.append(div);
		_me._row.append(td);

		return td;
	};

	this.element = function(element, specialClass, specialCss)
	{
		td = $("<td />");
		
		var div = $("<div />");
		div.addClass("td");
		if (specialClass != null) div.addClass(specialClass);
		if (specialCss != null) div.css(specialCss);

		div.append(element);
	
		td.append(div);
		_me._row.append(td);

		return td;
	};

	this.image = function(src, imageCss, specialClass, specialCss)
	{
		td = $("<td />");
		
		var div = $("<div />");
		div.addClass("td");
		if (specialClass != null) div.addClass(specialClass);
		if (specialCss != null) div.css(specialCss);

		var img = $("<img />", {src: src});
		if (imageCss != null) img.css(imageCss);
		div.append(img);
	
		td.append(div);
		_me._row.append(td);

		return td;
	};

	this.hotText = function(value, title, onClick, specialClass, specialCss)
	{
		var td = $('<td />');
		var div = $("<div />");
		if (specialClass != null) div.addClass(specialClass);
		if (specialCss != null) div.css(specialCss);
		
		var a = $("<a />", {href: ""});
		a.addClass("td");
		div.append(a);
		if (value != null)
		{
			a.text(value);
		}
		else
		{
			a.html("&nbsp;");
		}

		a.attr("title", title);
		a.click(function(){$(this).blur();try{onClick();}catch(e){error(e);}; return false;});

		onHover(td,
				function()
				{
					td.stop().animate({backgroundColor:"#686868"}, Hover.on);
					a.stop().animate({color:"#FFFFFF"}, Hover.on);
				},	
				function()
				{
					td.stop().animate({backgroundColor:"#FFFFFF"}, Hover.off);
					a.stop().animate({color:"#000000"}, Hover.on);
				});

		td.append(div);
		_me._row.append(td);		

		return td;
	};

	this.hotHtml = function(value, title, onClick, specialClass, specialCss)
	{
		var td = $('<td />');
		var div = $("<div />");
		if (specialClass != null) div.addClass(specialClass);
		if (specialCss != null) div.css(specialCss);
		
		var a = $("<a />", {href: ""});
		a.addClass("td");
		div.append(a);
		if (value != null)
		{
			a.html(value);
		}
		else
		{
			a.html("&nbsp;");
		}

		a.attr("title", title);
		a.click(function(){$(this).blur();try{onClick();}catch(e){error(e);}; return false;});

		onHover(td,
				function()
				{
					td.stop().animate({backgroundColor:"#686868"}, Hover.on);
					a.stop().animate({color:"#FFFFFF"}, Hover.on);
				},	
				function()
				{
					td.stop().animate({backgroundColor:"#FFFFFF"}, Hover.off);
					a.stop().animate({color:"#000000"}, Hover.on);
				});

		td.append(div);
		_me._row.append(td);		

		return td;
	};

	this.hotElement = function(element, title, onClick, specialClass, specialCss, extra, extraBkgColor)
	{
		var td = $('<td />');
		var div = $("<div />");
		if (specialClass != null) div.addClass(specialClass);
		if (specialCss != null) div.css(specialCss);
		
		var a = $("<a />", {href: ""});
		a.addClass("td");
		a.css({lineHeight:"normal"});
		div.append(a);
		if (element != null)
		{
			a.append(element);
		}
		else
		{
			a.html("&nbsp;");
		}

		a.attr("title", title);
		a.click(function(){$(this).blur();try{onClick();}catch(e){error(e);}; return false;});

		$.each(element.find(".hover"), function(index, e)
		{
			$(e).attr("hoverColor", $(e).css("color"));
		});

		onHover(td,
				function()
				{
					if (extra != null)
					{
						extra.css({backgroundColor:"white"});
						show(extra);
						extra.stop().animate({backgroundColor: extraBkgColor}, Hover.on);
					}

					td.stop().animate({backgroundColor:"#686868"}, Hover.on);
					a.stop().animate({color:"#FFFFFF"}, Hover.on);
					$.each(element.find(".hover"), function(index, e)
					{
						$(e).stop().animate({color:"#FFFFFF"}, Hover.on);
					});
				},	
				function()
				{
					if (extra != null) hide(extra);

					td.stop().animate({backgroundColor:"#FFFFFF"}, Hover.off);
					a.stop().animate({color:"#000000"}, Hover.on);
					$.each(element.find(".hover"), function(index, e)
					{
						$(e).stop().animate({color:$(e).attr("hoverColor")}, Hover.on);
					});
				});

		td.append(div);
		_me._row.append(td);		

		return td;
	};

	this.hotIconText = function(value, icon, title, onClick)
	{
		var td = $('<td />');
		_me._row.append(td);
		
		var a = $("<a class='e3_toolUiLinkIT' href='' />");
		$(a).css("background-image", "url(" + icon + ")");
		td.append(a);
		$(a).text(value);
		$(a).attr("title", title);
		$(a).click(function(){$(this).blur();try{onClick();}catch(e){error(e);}; return false;});
	
		return td;
	};

	this.icon = function(icon, title, onClick)
	{
		var td = $("<td />");
		var div = $("<div />");
		div.addClass("td");
		div.css({width:16});

		var element = null;
		if (onClick == null)
		{
			element = div;
		}
		else
		{
			element = $("<a />", {href:""});
			element.click(function(){$(this).blur();try{onClick();}catch(e){error(e);}; return false;});
			div.append(element);
		}
		element.addClass("icon")
		element.css("background-image", "url(" + icon + ")");
		element.html("&nbsp;");
		if (title != null) element.attr("title", title);

		td.append(div);
		_me._row.append(td);

		return td;		
	};

	this.iconSvg = function(icon, title, onClick)
	{
		var td = $("<td />");
		var div = $("<div />");
		div.addClass("td");
		div.css({width:16});

		var element = null;
		if (onClick == null)
		{
			element = div;
		}
		else
		{
			element = $("<a />", {href:""});
			element.click(function(){$(this).blur();try{onClick();}catch(e){error(e);}; return false;});
			div.append(element);
		}
		element.addClass("icon")
		if (title != null) element.attr("title", title);

		var svg = $("<svg style='width:16px; height:16px; vertical-align:middle;'><use xlink:href='#" + icon + "'></use></svg>");
		element.append(svg);

		td.append(div);
		_me._row.append(td);

		return td;		
	};

	this._dot = function(color, solid)
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
//				element = $('<svg width="32" height="32" viewbox="0 0 32 32"><circle cx="16" cy="16" r="7.5" stroke="#A0A0A0" stroke-width="1" fill="white" /></svg>');
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
			else if (color == Dots.check)
			{
				// from icoMoon - Free checkmark.png, 24px, w/h adjusted to shrink down and stay centered to match the other dots
				element = $('<svg width="16" height="16" viewbox="0 0 24 24"><path fill="#A0A0A0" d="M20.25 3l-11.25 11.25-5.25-5.25-3.75 3.75 9 9 15-15z" /></svg>');
			}
		}
		
		return element;
	};

	// color is something from Dots (etudes-extend.js)
	this.dot = function(color, title, solid, specialClass, specialCss)
	{
		var td = $("<td />");
		var div = $("<div />");
		div.addClass("td");
		if (specialClass != null) div.addClass(specialClass);
		if (specialCss != null) div.css(specialCss);

		div.addClass("dot");

		if (solid === undefined) solid = true;
		var element = _me._dot(color, solid);
		if (element != null) div.append(element);

		if (title != null) div.attr("title", title);

		td.append(div);
		_me._row.append(td);

		return td;
	};

	this.hotDot = function(color, title, onClick, specialClass, specialCss)
	{
		if (color == Dots.none) return _me.dot(color, title, specialClas, specialCss);

		var td = $('<td />');
		var div = $("<div />");
		if (specialClass != null) div.addClass(specialClass);
		if (specialCss != null) div.css(specialCss);

		div.addClass("dot");

		var a = $("<a />", {href: ""});
		a.addClass("td");
		div.append(a);
		a.css({width: 32, height: 32, display: "inline-block", position: "relative", top: -1});

		var element = _me._dot(color);
		a.append(element);

		a.attr("title", title);
		a.click(function(){$(this).blur();try{onClick();}catch(e){error(e);}; return false;});

		// TODO: effect tuned to Dots.hollow
		onHover(a,
			function()
			{
//				a.stop().animate({backgroundColor:"#686868"}, Hover.on);
				element.stop().animate({strokeWidth: 3}, Hover.on);
			},	
			function()
			{
//				a.stop().animate({backgroundColor:"#FFFFFF"}, Hover.off);
				element.stop().animate({strokeWidth:1}, Hover.off);
			});

		td.append(div);
		_me._row.append(td);		

		return td;
	};

	this.contextMenu = function(menu, specialClass, specialCss)
	{
		var td = $('<td />');
		var div = $("<div />");
		if (specialClass != null) div.addClass(specialClass);
		if (specialCss != null) div.css(specialCss);
		
		div.addClass("menu");

		var a = $("<a />", {href: "", title:"menu"});
		a.addClass("td");
		a.addClass("menu");

		var svg = $("<svg><use xlink:href='#icon-menu'></use></svg>");
		a.append(svg);

		contextMenu(a, menu, td);

		div.append(a);
		td.append(div);
		_me._row.append(td);

		return td;
	};

	// e3_input e3_timestamp
	
	this.input = function(attributes, specialClass, specialCss)
	{
		var td = $("<td />");

		var div = $("<div />");
		if (specialClass != null) div.addClass(specialClass);
		if (specialCss != null) div.css(specialCss);
	
		var i = $("<input />", attributes);
		if (attributes["size"] != null) i.attr("size", attributes["size"]);
		if (("text" == attributes.type) || ("number" == attributes.type))
		{
			i.css({width:"calc(100% - 32px)", marginLeft: 8});
			i.addClass("e3_input");
		}

		div.append(i);
		td.append(div);
		_me._row.append(td);
	
		return td;
	};

	this.inputDate = function(specialClass, specialCss)
	{
		var td = $("<td />");

		var div = $("<div />");
		div.addClass("dateInput");
		if (specialClass != null) div.addClass(specialClass);
		if (specialCss != null) div.css(specialCss);
	
		var i = $("<input />", {type: "text"});
		i.css({width: 136, fontSize: 12, textTransform: "uppercase"});
		i.addClass("e3_input e3_timestamp");

		div.append(i);
		td.append(div);
		_me._row.append(td);
	
		return td;
	};

	this.radioAndLabel = function(id, name, value, label)
	{
		var td = $('<td style="width:16px;" />');
		_me._row.append(td);
		var input = $('<input type="radio" id="' + id + '" name="' + name + '" value="' + value + '" />');
		$(input).attr("label", label);
		td.append(input);
		
		td = $('<td />');
		_me._row.append(td);
		var lbl = $('<label for="' + id + '" />').html(label);
		td.append(lbl);
		
		return td;
	};

	this.iconAndLabel = function(icon, title)
	{
		var td = $('<td style="white-space:nowrap;" />');
		_me._row.append(td);
		
		var img = $("<img />",{src:icon, style:"vertical-align:text-bottom; padding-right:4px;"});
		td.append(img);
		td.append(title);
		
		return td;
	};
	
	this.reorder = function(title, id)
	{
		_me._reorderAble = true;

		var td = _me.icon("/ui/icons/select-arrows.png", title);
		td.addClass("reorder reorderable");
		td.attr("oid", id);

		return td;
	};

	this.headerRow = function(value)
	{
		var td = $('<td />');
		td.addClass("header");
		_me._row.append(td);

		var count = _me._tableHead.find("th:last").index() + 1;
		if (count == 0) count = 99;
		td.attr("colspan", count);

		
		var div = $("<div />");
		div.html(value);
		div.css({width: "calc(100vw - 100px - 48px)", minWidth: "calc(1200px - 100px - 48px)"});
		td.append(div);


//		td.html(value);

		return td;
	};

	// td from a headerRow, to have it report this id in the reorder, but not be reorderable
	this.includeInOrderDisabled = function(td, id)
	{
		td.attr("oid", id);
		td.addClass("reorder");
	};

	this.sort = function(headers, sortList)
	{
		_me._sortHeaders = headers;
		_me._sortList = sortList;
	};

	this.done = function()
	{
		if ((_me._sortHeaders != null) && (_me.rowCount() > 0))
		{
			var options = 
			{
				headers:_me._sortHeaders,
				sortList:_me._sortList,
				emptyTo:"zero"
			};
			if (_me._zebraStripe)
			{
				options.widgets = ['zebra'];
				options.widgetOptions = {zebra:['e3_tableStripe_stripe', 'e3_tableStripe_normal']};
			}

			_me._table.tablesorter(options);
		}
		
		_me._setSelectAllHeader();
		_me._updateSelectAll();
		if (_me._onSelectChange != null) _me._onSelectChange();
	};

	this.sortList = function()
	{
		return _me._table[0].config.sortList;
	};

	this.selected = function(attr)
	{
		var rv = new Array();
		_me._table.find('[sid="' + _me._selectId + '"]:checked').each(function(index)
		{
			rv.push($(this).attr(attr == null ? "oid" : attr));
		});
	
		return rv;
	};

	this.selectedSecondAttr = function()
	{
		var rv = new Array();
		_me._table.find('[sid="' + _me._selectId + '"]:checked').each(function(index)
		{
			rv.push($(this).attr("oid2"));
		});
	
		return rv;
	};

	this.clearSelection = function()
	{
		_me._table.find('[sid="' + _me._selectId + '"]').prop("checked", false);
		if (_me._selectAll  != null) _me._selectAll.prop("checked", false);
		if (_me._onSelectChange != null) _me._onSelectChange();
	};
	
	this.setSelection = function(id, attr)
	{
		if (attr == null) attr = "oid";
		_me._table.find('[sid="' + _me._selectId + '"][' + attr + '="' + id + '"]').prop("checked", true);
	};

	this.updateActions = function(select1RequiredItemNames, selectRequiredItemNames)
	{
		// for no items, remove both sets of actions
		if (_me.rowCount() == 0)
		{
			$.each(select1RequiredItemNames || [], function(index, ui)
			{
				var item = ($.type(ui) === "string") ? $("#" + ui) : ui;
				disableAction(item);
			});

			$.each(selectRequiredItemNames || [], function(index, ui)
			{
				var item = ($.type(ui) === "string") ? $("#" + ui) : ui;
				disableAction(item);
			});
		}
		else
		{
			// set both sets of actions to visible / disabled
			$.each(select1RequiredItemNames || [], function(index, ui)
			{
				var item = ($.type(ui) === "string") ? $("#" + ui) : ui;
				item.removeClass("e3_offstage");
				disableAction(item);
			});

			$.each(selectRequiredItemNames || [], function(index, ui)
			{
				var item = ($.type(ui) === "string") ? $("#" + ui) : ui;
				item.removeClass("e3_offstage");
				disableAction(item);
			});

			var selected = _me.selected();
			if (selected.length == 1)
			{
				// set both sets of actions to enabled
				$.each(select1RequiredItemNames || [], function(index, ui)
				{
					var item = ($.type(ui) === "string") ? $("#" + ui) : ui;
					enableAction(item);
				});

				$.each(selectRequiredItemNames || [], function(index, ui)
				{
					var item = ($.type(ui) === "string") ? $("#" + ui) : ui;
					enableAction(item);
				});
			}
			else if (selected.length > 1)
			{
				// set only the selectRequired items (not those that need just 1) to enabled
				$.each(selectRequiredItemNames || [], function(index, ui)
				{
					var item = ($.type(ui) === "string") ? $("#" + ui) : ui;
					enableAction(item);
				});
			}
		}
	};

	// private methods
	this._setSelectAllHeader = function()
	{
		if (_me._selectAllHeaderIndex == null) return;

		var hdr = null;
		if (_me._externalHeaders == null)
		{
			hdr = _me._tableHead.find("tr > th:nth-child(" + _me._selectAllHeaderIndex + ")");
			var inner = hdr.find("div.tablesorter-header-inner");
			if (inner.length > 0) hdr = inner;
		}
		else
		{
			hdr = _me._externalHeaders.find("div:nth-child(" + _me._selectAllHeaderIndex + ")");
		}

		hdr.empty();
	
		var input = $("<input />", {type: "checkbox"});
		input.css({margin:0});
		_me._selectAll = input;
		hdr.append(input);
		onClick(input, function()
		{
			_me._table.find('[sid="' + _me._selectId + '"]').prop("checked", input.prop("checked"));
			if (_me._onSelectChange != null) _me._onSelectChange();
		}, true);
	};
	
	this._updateSelectAll = function()
	{
		var allChecked = true;
		var anyDefined = false;
		_me._table.find('[sid="' + _me._selectId + '"]').each(function(index)
		{
			if ($(this).prop("checked") != true)
			{
				allChecked = false;
			}
			anyDefined = true;
		});
	
		// if there are no check boxes, hide the selectAll
		if (_me._selectAll != null)
		{
			if (!anyDefined)
			{
				hide(_me._selectAll);
			}
		
			// otherwise set the selectAll
			else
			{
				show(_me._selectAll);
				_me._selectAll.prop("checked", allChecked);
			}
		}
	};
	
	this._keydown = function(event)
	{
		if (event.target == event.currentTarget)
		{
			if ($(event.target).attr("noReorder") == "1") return false;

			// arrow up
			if (event.which == 38)
			{
				var prev = $(event.currentTarget).prev('tr');
				if ((prev.length == 0) || (prev.hasClass("noDrop"))) return false; // TODO: similar for next?
				$(event.currentTarget).insertBefore(prev);
				$(event.currentTarget).focus();
				try{_me._onReorder(_me._tableBody.find("td.reorder").map(function(){return $(this).attr("oid");}).get());} catch (err) {error(err);}
				return false;
			}
			// arrow down
			else if (event.which == 40)
			{
				var next = $(event.currentTarget).next('tr')[0];
				if (next != null) $(event.currentTarget).insertAfter(next);
				$(event.currentTarget).focus();
				try{_me._onReorder(_me._tableBody.find("td.reorder").map(function(){return $(this).attr("oid");}).get());} catch (err) {error(err);}
				return false;
			}
			// delete TODO:
//			else if (event.which == 8)
//			{
//				_me.removeRow(event.currentTarget);
//				return false;
//			}
		}

		return true;
	};

	this._keydownHotRow = function(event, prev, next, onClickSelect)
	{
		if (event.target == event.currentTarget)
		{
			// arrow up
			if (event.which == 38)
			{
				if ((prev != null) && (prev.length > 0))
				{
					prev.click();
//					_me._tableBody.find("tr.selected").removeClass("selected");
//					prev.addClass("selected");
//					prev.focus();
//					if (onClickSelect !== undefined) onClickSelect();
				}
				return false;
			}
			// arrow down
			else if (event.which == 40)
			{
				if ((next != null) && (next.length > 0))
				{
					next.click();
//					_me._tableBody.find("tr.selected").removeClass("selected");
//					next.addClass("selected");
//					next.focus();
//					if (onClickSelect !== undefined) onClickSelect();
				}
				return false;
			}
		}

		return true;
	};

	this._rowBlur = function()
	{
		$(this).removeClass("e3_kbdSelect_row");
	};

	this._rowFocus = function()
	{
		if (!($(this).attr("noReorder") == "1")) $(this).addClass("e3_kbdSelect_row");
	};
};

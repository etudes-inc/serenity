/**********************************************************************************
 * $URL: https://source.etudes.org/svn/serenity/trunk/ui/ui-webapp/src/main/webapp/etudes-edit.js $
 * $Id: etudes-edit.js 12253 2015-12-10 01:42:43Z ggolden $
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

function e3_Edit(seed, except, onChg)
{
	var _me = this;

	var filters = {};

	// except array[].value matches any array[n].value
	this._clone = function(obj, rv, path, except)
	{
		$.each(obj, function(name, value)
		{
			if (except.indexOf(path+name) != -1) return;

			if (!name.startsWith("orig_") && !name.startsWith("added_") && !name.startsWith("removed_"))
			{
				if ($.type(obj[name]) === "array")
				{
					rv[name] = [];
					$.each(obj[name], function(index, value)
					{
//						rv[name].push(_me._clone(value, {}, path + name + "[" + index + "].", except));
						rv[name].push(_me._clone(value, {}, path + name + "[].", except));
					});
				}
				else if ($.type(obj[name]) === "object")
				{
					rv[name] = _me._clone(value, {}, path + name + ".", except);
				}
				else
				{
					rv[name] = value;
				}
			}
		});

		return rv;
	};

	this._filter = function(name, value)
	{
		if (value == null) return null;
		
		var f = filters[name];
		if (f == null) f = filters["defaultFilter"];

		if (f == null) return value;
		
		return f(value);
	};

	this.namePath = function(obj, from)
	{
		if (from == null) return null;
		if (from === obj) return "";

		var names = Object.keys(from);
		for (var i = 0; i < names.length; i++)
		{
			if ($.type(from[names[i]]) === "function") continue;

			else if ($.type(from[names[i]]) === "array")
			{
				for (var index = 0; index < from[names[i]].length; index++)
				{
					var o = from[names[i]][index];
					if ($.type(o) === "object")
					{
						var path = _me.namePath(obj, o);
						if (path != null) return names[i] + "[]." + path;
					}
				}
			}

			else if ($.type(from[names[i]]) === "object")
			{
				var path = _me.namePath(obj, from[names[i]]);
				if (path != null) return names[i] + "." + path;
			}
		}
		return null;
	}

	this.dateFilter = function(value)
	{
		// TODO:
		return trim(value);
	};

	this.stringFilter = function(value)
	{
		return trim(value);
	};
	
	this.stringZeroFilter = function(value)
	{
		return trimToZero(value);
	};
	
	this.urlFilter = function(value)
	{
		var url = trim(value);
		if (url != null)
		{
			var urlLc = url.toLowerCase();
			if ((!urlLc.startsWith("http://")) && (!urlLc.startsWith("https://")) && (!urlLc.startsWith("//")))
			{
				// add a transport
				url = portal_tool.protocol + "//" + url;
			}
		}
		
		return url;
	};

	this.booleanFilter = function(value)
	{
		if (typeof value == "boolean") return value;
		return (value == "true");
	};

	this.numberFilter = function(value)
	{
		if (value == null) return null;
		var t = trim(value);
		if (t == null) return null;
		var v = Number(t);
		if (isNaN(v)) return null;
		return v;
	};

	this.noFilter = function(value)
	{
		return value;
	};

	this.find = function(list, id)
	{
		var rv = {index: null, obj: null};
		$.each(list, function(index, obj)
		{
			if (obj.id == id)
			{
				rv.index = index;
				rv.obj = obj;
			}
		});

		return rv;
	};

	this.ids = function(list)
	{
		var rv = new Array();
		$.each(list, function(index, obj)
		{
			rv.push(obj.id);
		});
		
		return rv;
	};

	this.revert = function(obj)
	{
		if (obj == null) obj = _me;
		$.each(obj, function(name, value)
		{
			if (!name.startsWith("orig_"))
			{
				// restore original value, or for arrays, the original array makeup and order
				if (obj["orig_" + name] !== undefined)
				{
					obj[name] = obj["orig_" + name];
					obj["orig_" + name] = undefined;
				}

				if ($.type(obj[name]) === "object")
				{
					_me.revert(obj[name]);
				}

				else if ($.type(obj[name]) === "array")
				{
					// restore each item in the array
					$.each(obj[name], function(index, value){_me.revert(value);});
					
					// clear added and removed
					if (obj["added_" + name] !== undefined)
					{
						obj["added_" + name] = undefined;
					}
					if (obj["removed_" + name] !== undefined)
					{
						obj["removed_" + name] = undefined;
					}
				}
			}
		});
		if (onChg !== undefined) onChg(_me.changed());
	};

	this.set = function(obj, name, value)
	{
		if (obj["orig_" + name] === undefined)
		{
			obj["orig_" + name] = (obj[name] === undefined) ? null : obj[name];
		}

		var path = _me.namePath(obj, _me) + name;
		obj[name] = _me._filter(path, value);

		if (onChg !== undefined) onChg(_me.changed());
	};

	this.add = function(obj, name, value)
	{
		// keep the original order and makeup of the list			
		if (obj["orig_" + name] === undefined)
		{
			obj["orig_" + name] = [];
			$.each(obj[name], function(i, orig){obj["orig_" + name].push(orig);})
		}

		// add the value to the end of the list
		obj[name].push(value);
		
		// track it on the added list
		if (obj["added_" + name] === undefined)
		{
			obj["added_" + name] = [];
		}
		obj["added_" + name].push(value);
		
		// if removed, no longer
		if (obj["removed_" + name] !== undefined)
		{
			var removed = _me.find(obj["removed_" + name], value.id);
			if (removed.index != null) obj["removed_" + name].splice(removed.index, 1);
		}
		if (onChg !== undefined) onChg(_me.changed());
	};

	// remove from obj's list [name] the object with id
	this.remove = function(obj, name, id)
	{
		var found = _me.find(obj[name], id);

		// keep the original order and makeup of the list			
		if (obj["orig_" + name] === undefined)
		{
			obj["orig_" + name] = [];
			$.each(obj[name], function(i, orig){obj["orig_" + name].push(orig);})
		}
		
		// move the value to the removed list
		if (obj["removed_" + name] === undefined)
		{
			obj["removed_" + name] = [];
		}
		obj["removed_" + name].push(found.obj);

		// remove the value from the list
		obj[name].splice(found.index, 1);
		
		// if added, no longer
		if (obj["added_" + name] !== undefined)
		{
			var added = _me.find(obj["added_" + name], id);
			if (added.index != null) obj["added_" + name].splice(added.index, 1);
		}
		if (onChg !== undefined) onChg(_me.changed());
	};

	this.move = function(obj, name, id, newIndex)
	{
		// keep the original order and makeup of the list			
		if (obj["orig_" + name] === undefined)
		{
			obj["orig_" + name] = [];
			$.each(obj[name], function(i, orig){obj["orig_" + name].push(orig);})
		}

		var found = _me.find(obj[name], id);

		obj[name].splice(found.index, 1);
		obj[name].splice(newIndex, 0, found.obj);
		if (onChg !== undefined) onChg(_me.changed());
	};

	// order is an array of ids
	this.order = function(obj, name, order)
	{
		// keep the original order and makeup of the list			
		if (obj["orig_" + name] === undefined)
		{
			obj["orig_" + name] = [];
			$.each(obj[name], function(i, orig){obj["orig_" + name].push(orig);})
		}

		var newArray = [];
		$.each(order, function(index, id)
		{
			var found = _me.find(obj[name], id);
			newArray.push(found.obj);
		});

		obj[name] = newArray;
		if (onChg !== undefined) onChg(_me.changed());
	};

	this.changed = function(obj)
	{
		if (obj == null) obj = _me;
		var rv = false;
		$.each(obj, function(name, value)
		{
			if (!name.startsWith("orig_") && !name.startsWith("added_") && !name.startsWith("removed_"))
			{
				if (obj[name] instanceof File)
				{
					rv = true;
				}
				else if ($.type(obj[name]) === "array")
				{
					$.each(obj[name], function(index, value){if (_me.changed(value)) rv = true;});
					if ((obj["added_" + name] !== undefined) && (obj["added_" + name].length > 0)) rv = true;
					if ((obj["removed_" + name] !== undefined) && (obj["removed_" + name].length > 0)) rv = true;
				}
				else if ($.type(obj[name]) === "object")
				{
					if (_me.changed(obj[name])) rv = true;
				}
				else if ($.type(obj[name]) !== "function")
				{
					if (_me.propChanged(obj, name)) rv = true;
					// if ((obj["orig_" + name] !== undefined) && (obj[name] != obj["orig_" + name])) rv = true;
				}
			}
		});

		return rv;
	};
	
	this.propChanged = function(obj, name)
	{
		if ((obj["orig_" + name] !== undefined) && (obj[name] != obj["orig_" + name])) return true;
		return false;
	};
	
	this.params = function(prefix, params, obj)
	{
		if (obj == null) obj = _me;
		var names = [];
		$.each(obj, function(name, value)
		{
			if (obj[name] == null) return;

			if (!name.startsWith("orig_") && !name.startsWith("added_") && !name.startsWith("removed_"))
			{
				if (obj[name] instanceof File)
				{
					params.post[prefix + name] = obj[name];
					names.push(name);
				}
				else if ($.type(obj[name]) === "array")
				{
					params.post[prefix + "count_" + name] = obj[name].length;
					$.each(obj[name], function(index, value)
					{
						_me.params(prefix + index + "_" + name + "_", params, value);
					});
					if ((obj["added_" + name] !== undefined) && (obj["added_" + name].length > 0))
					{
						params.post[prefix + "added_" + name] = _me.ids(obj["added_" + name]);
					}
					if ((obj["removed_" + name] !== undefined) && (obj["removed_" + name].length > 0))
					{
						params.post[prefix + "removed_" + name] = _me.ids(obj["removed_" + name]);
					}
				}
				else if ($.type(obj[name]) === "object")
				{
					_me.params(prefix + name + "_", params, obj[name]);
				}
				else if ($.type(obj[name]) !== "function")
				{
					params.post[prefix + name] = obj[name];
					names.push(name);
				}
			}
		});
		params.post[prefix] = names;
	};

	this.setupEdit = function(target, obj, name, onChg)
	{
		var ui = ($.type(target) === "string") ? $("#" + target) : target;
		ui.val(obj[name]);
		onChange(ui, function(t, finalChange)
		{
			if (!finalChange) return;
			_me.set(obj, name, ui.val());
			if (onChg !== undefined) onChg(ui.val(), finalChange);
		});
	};

	this.setupFilteredEdit = function(target, obj, name, onChg)
	{
		var ui = ($.type(target) === "string") ? $("#" + target) : target;
		ui.val(obj[name]);
		onChange(ui, function(t, finalChange)
		{
			_me.set(obj, name, ui.val());
			if (finalChange)
			{
				ui.val(obj[name]);
			}
			if (onChg !== undefined) onChg(ui.val(), finalChange);
		});
	};

	this.setupCheckEdit = function(target, obj, name, onChg)
	{
		var ui = ($.type(target) === "string") ? $("#" + target) : target;
		ui.prop("checked", obj[name]);
		onChange(ui, function(t, finalChange)
		{
			_me.set(obj, name, ui.is(":checked"));
			if (onChg !== undefined) onChg(ui.is(":checked"), finalChange);
		});
	};
	
	this.setupRadioEdit = function(target, obj, name, onChg)
	{
		// var ui = ($.type(target) === "string") ? $("#" + target) : target;
		$("input:radio[name=" + target + "][value=" + obj[name] + "]").prop('checked', true);
		onChange($("input:radio[name=" + target + "]"), function(t, finalChange)
		{
			if (!finalChange) return;
			_me.set(obj, name, $("input:radio[name=" + target + "]:checked").val());
			if (onChg !== undefined) onChg($("input:radio[name=" + target + "]:checked").val(), finalChange);
		});
	};

	this.setupSelectEdit = function(target, options, obj, name, onChg)
	{
		var ui = ($.type(target) === "string") ? $("#" + target) : target;
		if (options != null)
		{
			ui.empty();
			$.each(options || [], function(index, optionSpec)
			{
				var option = $("<option>",{value:optionSpec.value}).text(optionSpec.title);
				ui.append(option);
			});
		}
		ui.val(obj[name]);
		onChange(ui, function(t, finalChange)
		{
			_me.set(obj, name, ui.val());
			if (onChg !== undefined) onChg(ui.val(), finalChange);
		});
	};

	this.setupDateEdit = function(target, obj, name, timestamp, startNotEnd, onChg)
	{
		var ui = ($.type(target) === "string") ? $("#" + target) : target;
		timestamp.setInput(ui, startNotEnd, obj[name]);
		onChange(ui, function(t, finalChange)
		{
			_me.set(obj, name, timestamp.getInput(ui));
			if (onChg !== undefined) onChg(timestamp.getInput(ui), finalChange);
		});
	};

	this.setupEditorEdit = function(editor, obj, name, onChg, fs)
	{
		editor.disable();
		editor.set(obj[name]);
		editor.enable(function()
		{
			_me.set(obj, name, editor.get());
			if ((onChg !== undefined) && (onChg != null)) onChg();
		}, true, fs);		
	};

//	this.setupIndicatedFilteredEdit = function(target, obj, name, onChg)
//	{
//		var ui = ($.type(target) === "string") ? $("#" + target) : target;
//		ui.val(obj[name]);
//		onChange(ui, function(t, finalChange)
//		{
//			_me.set(obj, name, ui.val());
//			applyClass("e3_changed", t.parent().parent(), _me.propChanged(obj, name));
//			if (finalChange)
//			{
//				ui.val(obj[name]);
//			}
//			if (onChg !== undefined) onChg(ui.val());
//		});
//	};
//
//	this.setupIndicatedFilteredDateEdit = function(target, obj, name, timestamp, startNotEnd, onChg)
//	{
//		var ui = ($.type(target) === "string") ? $("#" + target) : target;
//		timestamp.setInput(ui, startNotEnd, obj[name]);
//		onChange(ui, function(t, finalChange)
//		{
//			_me.set(obj, name, timestamp.getInput(ui));
//			applyClass("e3_changed", t.parent().parent(), _me.propChanged(obj, name));
////			if (finalChange)
////			{
////				ui.val(obj[name]);
////			}
//			if (onChg !== undefined) onChg(timestamp.getInput(ui));
//		});
//	};
//
//	this.setupIndicatedCheckEdit = function(target, obj, name, onChg)
//	{
//		var ui = ($.type(target) === "string") ? $("#" + target) : target;
//		ui.prop("checked", obj[name]);
//		onChange(ui, function(t, finalChange)
//		{
//			_me.set(obj, name, ui.is(":checked"));
//			applyClass("e3_changed", t.parent().parent(), _me.propChanged(obj, name));
//			if (onChg !== undefined) onChg(ui.is(":checked"));
//		});
//	};

	// path. is prepended to each name if provided
	this.setFilters = function(fltrs, path)
	{
		$.each(fltrs, function(name, value)
		{
			if (path !== undefined)
			{
				filters[path + "." + name] = value;
			}
			else
			{
				filters[name] = value;
			}
		});
	};

	_me._clone(seed, _me, "", except || []);
}

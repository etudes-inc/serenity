/**********************************************************************************
 * $URL: https://source.etudes.org/svn/serenity/trunk/ui/ui-webapp/src/main/webapp/etudes-i18n.js $
 * $Id: etudes-i18n.js 11308 2015-07-17 22:02:41Z ggolden $
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

// Object used to localize UI wording
function e3_i18n(i10n, lang)
{
	var _me = this;
	this._i10n = i10n;
	this._lang = lang;
	this._combined = null;

	this._localize = function(scope)
	{
		// apply to the html
		$.each(_me._combined, function(id, value)
		{
			// find the item with the "i18n" attribute set to index
			var target = (scope == null) ? $('[i18n="' + id + '"]') : scope.find('[i18n="' + id + '"]');
			if (target.length > 0)
			{
				if ($.type(value) === "string")
				{
					// set the html
					$(target).html(value);
				}
				else
				{
					$.each(value, function(attr, wording)
					{
						if (attr === "html")
						{
							$(target).html(wording);
						}
						else
						{
							// set the attribute
							$(target).attr(attr, wording);
						}
					});
				}
			}
		});
	};

	this.lookup = function(key, dflt, attr, fill)
	{
		if (_me._lang === _me._i10n.native) return _me._fillin(dflt, fill);
		if (_me._combined == null) return _me._fillin(dflt, fill);

		var entry = _me._combined[key];
		if (entry == null) entry = dflt;

		if (entry != null)
		{
			if ($.type(entry) === "string")
			{
				return _me._fillin(entry, fill);
			}
			else
			{
				if (attr == null) attr = "html";
				var value = entry[attr];
				if (value != null)
				{
					return _me._fillin(value, fill);
				}
			}
		}

		return key;
	};

	this._fillin = function(text, values)
	{
		if (values == null) return text;
		
		$.each(values, function(index, value)
		{
			text = text.replace("%" + index, value);
		});
		
		return text;
	};

	this.init = function(lang)
	{
		if (lang != null) _me._lang = lang;

		// skip if the desired lang matches the native lang (exactly)
		if (_me._lang === _me._i10n.native) return;

		var combined = {};
		var langSplit = _me._lang.split("-");

		// start with the base of lang (if has a "-"), or the entire lang (if it does not)
		var strings = _me._i10n[langSplit[0]];
		if (strings != null)
		{
			$.each(strings, function(id, value)
			{
				combined[id] = value;
			});
		}

		// merge in (overwriting) the the full lang, if more than just the base
		if (langSplit.length > 1)
		{
			strings = _me._i10n[_me._lang];
			if (strings != null)
			{
				$.each(strings, function(id, value)
				{
					combined[id] = value;
				});
			}
		}

		_me._combined = combined;
	};

	this.localize = function(lang, scope)
	{
		_me.init(lang);
		_me._localize(scope || $("#portal_content"));
	};
};

/**********************************************************************************
 * $URL: https://source.etudes.org/svn/serenity/trunk/ui/ui-webapp/src/main/webapp/etudes-timestamp.js $
 * $Id: etudes-timestamp.js 12490 2016-01-07 22:59:11Z ggolden $
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

// support for date picker input fields
// <input class="e3_data" type="text" id="setup_edit_publish_ui_unpubDate" value="" />
function e3_Timestamp()
{
	var _me = this;

	this._dateTimePickerConfigStart =
	{
		dayNamesMin: ["S", "M" ,"T", "W", "T", "F", "S"],
		dateFormat: "M dd, yy",
		showButtonPanel: true,
		changeMonth: true,
		changeYear: true,
		showOn: "button", // "button" "both" "focus"
		buttonImage: "/ui/icons/date.png",
		buttonImageOnly: true,
		timeFormat: "hh:mm TT",
		controlType: "select",
		showTime: false,
		closeText: "OK",
		hour: 8,
		minute: 0,
		beforeShow: function(input, inst){_me._beforeShow(input, inst);}
	},

	this._dateTimePickerConfigEnd =
	{
		dayNamesMin: ["S", "M" ,"T", "W", "T", "F", "S"],
		dateFormat: "M dd, yy",
		showButtonPanel: true,
		changeMonth: true,
		changeYear: true,
		showOn: "button", // "button"
		buttonImage: "/ui/icons/date.png",
		buttonImageOnly: true,
		timeFormat: "hh:mm TT",
		controlType: "select",
		showTime: false,
		closeText: "OK",
		hour: 23,
		minute: 59,
		beforeShow: function(input, inst){_me._beforeShow(input, inst);}
	},
	
	this._datePickerConfig =
	{
		dayNamesMin: ["S", "M" ,"T", "W", "T", "F", "S"],
		dateFormat: "M dd, yy",
		showButtonPanel: true,
		changeMonth: true,
		changeYear: true,
		showOn: "both", // "button"
		buttonImage: "/ui/icons/date.png",
		buttonImageOnly: true,
		closeText: "OK",
		beforeShow: function(input, inst){_me._beforeShow(input, inst);}
	},

	this._calendarConfig =
	{
		dayNamesMin: ["S", "M" ,"T", "W", "T", "F", "S"],
		dateFormat: "M dd, yy"
//		showButtonPanel: true,
//		changeMonth: true,
//		changeYear: true,
//		showOn: "both", // "button"
//		buttonImage: "/ui/icons/date.png",
//		buttonImageOnly: true,
//		closeText: "OK"
	},

	this._beforeShow = function(input, inst)
	{
		var calendar = inst.dpDiv;
		setTimeout(function()
		{
			calendar.position({
					my: 'left top',
					at: 'left top',
					collision: 'none',
					of: input
			});
		}, 100);
	};

	this._tz = function()
	{
		if (portal_tool.info.user != null) return portal_tool.info.user.timeZone;
		return "America/Los_Angeles";
	};

	this.clearInput = function(id)
	{
		var element = ($.type(id) === "string") ? $("#" + id) : id;
		element.datetimepicker("destroy");
	};

	this.setInput = function(id, startNotEnd, valueInMs)
	{
		var element = ($.type(id) === "string") ? $("#" + id) : id;
		element.addClass("e3_timestamp");
		element.datetimepicker("destroy");
		element.datetimepicker(startNotEnd ? _me._dateTimePickerConfigStart : _me._dateTimePickerConfigEnd);
		if (valueInMs == null)
		{
			element.val("");
		}
		else
		{
			element.val(moment(valueInMs).tz(_me._tz()).format("MMM DD, YYYY hh:mm A"));
		}
	};
	
	this.setDateInput = function(id, valueInMs)
	{
		var element = ($.type(id) === "string") ? $("#" + id) : id;
		element.addClass("e3_datestamp");
		element.datepicker("destroy");
		element.datepicker(_me._datePickerConfig);
		if (valueInMs == null)
		{
			element.val("");
		}
		else
		{
			element.val(moment(valueInMs).tz(_me._tz()).format("MMM DD, YYYY"));
		}
	};

	this.refresh = function(id)
	{
		var element = ($.type(id) === "string") ? $("#" + id) : id;
		element.datepicker("refresh");
	};

	this.setCalendar = function(id, options)
	{
		var element = ($.type(id) === "string") ? $("#" + id) : id;
		element.datepicker(_me._calendarConfig);
		if (options != null)
		{
			if (options.onSelect != null) element.datepicker("option", "onSelect", function(date, inst){options.onSelect(_me._fmtDateInput(date));});
			if (options.beforeShowDay != null) element.datepicker("option", "beforeShowDay", function(date){return options.beforeShowDay(date);});
			if (options.onChangeMonthYear != null) element.datepicker("option", "onChangeMonthYear", function(year, month){return options.onChangeMonthYear(year, month);});
		}
	};

	this.getInput = function(id)
	{
		var element = ($.type(id) === "string") ? $("#" + id) : id;
		var value = $.trim(element.val());
		if (value == "") return null;

		var m = moment().tz(_me._tz());
		
		// "MMM DD, YYYY hh:mm A"
		var hourOffset = 0;
		if (value.substring(19,20) == "P") hourOffset = 12;
		m.set({year: parseInt(value.substring(8,12)), month:value.substring(0,3), date:parseInt(value.substring(4,6)), hour:parseInt(value.substring(13,15))+hourOffset, minute:parseInt(value.substring(16,18)), second:0, millisecond:0});

		var rv = parseInt(m.format("X") + "000");
		return rv;
	};

	this._fmtDateInput = function(value)
	{		
		var m = moment().tz(_me._tz());

		// "MMM DD, YYYY"
		m.set({year: parseInt(value.substring(8,12)), month:value.substring(0,3), date:parseInt(value.substring(4,6)), hour:0, minute:0, second:0, millisecond:0});

		var rv = parseInt(m.format("X") + "000");
		return rv;
	};

	this.getDateInput = function(id)
	{
		var element = ($.type(id) === "string") ? $("#" + id) : id;
		var value = $.trim(element.val());
		if (value == "") return null;

		return _me._fmtDateInput(value);
	};

	this.moment = function(valueInMs)
	{
		return moment(valueInMs).tz(_me._tz());
	};

	this.display = function(valueInMs)
	{
		if (valueInMs == null) return "";
//		return moment(valueInMs).tz(_me._tz()).calendar();
		return moment(valueInMs).tz(_me._tz()).format("MMM DD, YYYY hh:mm A");
	};
	
	this.displayDate = function(valueInMs)
	{
		if (valueInMs == null) return "";
		return moment(valueInMs).tz(_me._tz()).format("MMM DD, YYYY");
	};
	
	this.displayTime = function(valueInMs)
	{
		if (valueInMs == null) return "";
		return moment(valueInMs).tz(_me._tz()).format("hh:mm");
	};

	this.displayAmPm = function(valueInMs)
	{
		if (valueInMs == null) return "";
		return moment(valueInMs).tz(_me._tz()).format("A");
	};

	this.displayTz = function()
	{
		var m = moment().tz(_me._tz());
		return m.format("z Z");
	};

	this.difference = function(aMs, bMs)
	{
		var a = moment(aMs).tz(_me._tz()).set({hour:0, minute:0, second:0, millisecond:0});
		var b = moment(bMs).tz(_me._tz()).set({hour:0, minute:0, second:0, millisecond:0});
		return b.diff(a, 'days');
	};
	
	this.differenceInMinutes = function(aMs, bMs)
	{
		var a = moment(aMs).tz(_me._tz()).set({second:0, millisecond:0});
		var b = moment(bMs).tz(_me._tz()).set({second:0, millisecond:0});
		return b.diff(a, 'minutes');
	};

	this.adjust = function(valueInMs, days)
	{
		var m = moment(valueInMs).tz(_me._tz()).set({hour:0, minute:0, second:0, millisecond:0});
		m.add(days, 'days');
		return parseInt(m.format("X") + "000");
	}
};

moment.locale('en', {
    calendar : {
        lastDay : '[Yesterday at] LT',
        sameDay : '[Today at] LT',
        nextDay : '[Tomorrow at] LT',
        lastWeek : '[last] dddd [at] LT',
        nextWeek : 'dddd [at] LT',
        sameElse : 'MMM DD, YYYY hh:mm A'// defaults as 'L'
    }
});

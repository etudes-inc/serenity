/**********************************************************************************
 * $URL: https://source.etudes.org/svn/serenity/trunk/ui/ui-webapp/src/main/webapp/etudes-session.js $
 * $Id: etudes-session.js 11725 2015-09-28 03:44:56Z ggolden $
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

// Support for local session storage, shared across tabs and windows, reset when not kept alive for a period of time.
function e3_Session(window)
{
	// console.log("new session: inSession = " + inSession);

	var me = this;

	// separate entries for each window
	this._window = window;

	// how often to check for an active session going inactive (10 seconds)
	this._autoCheck = 10 * 1000;
	this._autoCheckTimer = null;

	// how long a use can remain inactive before we deactivate the session (30 minutes)
	this._activeTimeout = 30 * 60 * 1000;

	this._onInactive = null;
	this._active = false;

	// start checking
	this._check();
	this._autoCheckTimer = setInterval(function(){me._check();}, this._autoCheck);
}

e3_Session.prototype.setOnInactive = function(onInactive)
{
	// call this when an active session goes inactive
	this._onInactive = onInactive;
};

e3_Session.prototype.deactivate = function()
{
	// console.log("session deactivate");

	localStorage.clear();
	this._active = false;
};

e3_Session.prototype.isActive = function()
{
	return(localStorage["session:active.timestamp"] != null);
};

e3_Session.prototype.id = function()
{
	return localStorage["session:active.id"];
};

e3_Session.prototype.activate = function()
{
	// console.log("session activate");

	this._active = true;

	// id the session
	localStorage["session:active.id"] = (new Date()).getTime();
	
	// any keypress or click signals an active user - to keep the session marked as active
	localStorage["session:active.timestamp"] = (new Date()).getTime();
};

e3_Session.prototype.put = function(name, value, window)
{
	if (window == null) window = this._window;
	localStorage[this._key(window, name)] = value;
};

e3_Session.prototype.get = function(name)
{
	return localStorage[this._key(this._window, name)];
};

e3_Session.prototype.remove = function(name)
{
	return localStorage.removeItem(this._key(this._window, name));
};

e3_Session.prototype._key = function(window, name)
{
	return window + ":" + name;
};

// called by portal on any mousedown or keydown
e3_Session.prototype.recordUserActivity = function()
{
	if (!this._active) return;

	// console.log("session recordUserActivity");

	// we should have a timestamp to update - if not, ignore
	if (localStorage["session:active.timestamp"] != null)
	{
		localStorage["session:active.timestamp"] = (new Date()).getTime();
	}
};

e3_Session.prototype._check = function()
{
	// console.log("session _check");

	// if we have a timestamp, we need to check for inactivity
	ts = localStorage["session:active.timestamp"];
	if (ts != null)
	{
		// console.log("    checking active.timestamp");
		var tsi = parseInt(ts);
		if ((tsi + this._activeTimeout) < (new Date()).getTime())
		{
			// console.log("    session expired active");
			this.deactivate();
			if (this._onInactive != null) this._onInactive();
		}
	}
};

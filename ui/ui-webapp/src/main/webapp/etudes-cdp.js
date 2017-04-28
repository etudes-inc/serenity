/**********************************************************************************
 * $URL: https://source.etudes.org/svn/serenity/trunk/ui/ui-webapp/src/main/webapp/etudes-cdp.js $
 * $Id: etudes-cdp.js 11754 2015-10-02 18:16:45Z ggolden $
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

// Object used to make CDP requests: send in {onErr:function to call when there is an error from the request, whileLoadingShow:element id to show/hide while loading}
function e3_Cdp(params)
{
	// a "loading" element name
	this._loadingUiId = params.whileLoadingShow;

	// pick up the CDP url from our request url
	this._url = $(location).attr('protocol') + "//" + $(location).attr('hostname') + ":" + $(location).attr('port') + "/cdp/";
	
	this._errorFunction = params.onErr;
	
	this.domain = $(location).attr('hostname');
};

// public methods

e3_Cdp.prototype.params = function()
{
	var rv = new Object();
	rv.url = new Object();
	rv.post = new Object();
	
	return rv;
};

e3_Cdp.prototype.request = function(cdpRequest, params, completion)
{
	if (params == null) params = this.params();

	var err = localStorage["error:report"];
	if (err != null)
	{
		params.post["cdp:error"] = err;
		localStorage.removeItem("error:report");
	}

	var fd = this._makeFormData(params.post);
	this._cdpShowLoading();
	var me = this;
	$.ajax(
	{
		url: this._makeCdpRequestUrl(cdpRequest, params.url),
		type: "POST",
		data: fd,
		dataType:"json",
		error: function(jqXHR, textStatus, errorThrown){me._cdpDoneLoading();},
		success: function(json, textStatus, jqXHR){me._cdpSuccess(json, completion);},
		processData: false,
		contentType: false
	});
};

// private methods

e3_Cdp.prototype._makeFormData = function(data)
{
	var fd = new FormData;
	fd.append("cdp_version", "18");
	if (data != null)
	{
		$.each(data, function(key, value)
		{
			if ($.isArray(value))
			{
				var val = "";
				$.each(value, function(i, v)
				{
					val += v + "\t";
				});
				fd.append(key, val);
			}
			else if (value == null)
			{
				fd.append(key, "");
			}
			else
			{
				fd.append(key, value);
			}
		});
	}

	return fd;
};

e3_Cdp.prototype._makeCdpRequestUrl = function(cdpRequest, data)
{
	var rv = this._url + cdpRequest;
	
	if (data != null)
	{
		rv = rv + "?";
		$.each(data, function(key, value)
		{
			rv = rv + key + "=" + value + "&";
		});
		
		rv = rv.substring(0, rv.length-1);
	}
	
	return rv;
};

e3_Cdp.prototype._cdpSuccess = function(data, completion)
{
	this._cdpDoneLoading();

	// deal with not not CdpStatus.notLoggedIn (2), accessDenied (1), and other non-success status
	if (data["cdp:status"] != 0)
	{
		if (this._errorFunction != null)
		{
			try
			{
				this._errorFunction(data["cdp:status"]);
			}
			catch (e)
			{
				error(e);
			}
		}
	}

	if (completion != null)
	{
		try
		{
			completion(data);
		}
		catch (e)
		{
			error(e);
		}
	}
};

e3_Cdp.prototype._cdpShowLoading = function()
{
	if (this._loadingUiId != null)
	{
		$("#" + this._loadingUiId).removeClass("e3_offstage");
	}
};

e3_Cdp.prototype._cdpDoneLoading = function()
{
	if (this._loadingUiId != null)
	{
		$("#" + this._loadingUiId).addClass("e3_offstage");
	}
};

/**********************************************************************************
 * $URL: https://source.etudes.org/svn/serenity/trunk/user/user-webapp/src/main/webapp/account.js $
 * $Id: account.js 12504 2016-01-10 00:30:08Z ggolden $
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

var account_tool = null;

function Account()
{
	var me = this;

	this.portal = null;
	this.i18n = new e3_i18n(account_i10n, "en-us");
	this.edit = null;
	this.editor = null;
	this.ui = null;
	this.user = null;

	this.init = function()
	{
		me.i18n.localize();
		
		me.ui = findElements(["account_nameFirst", "account_nameLast", "account_eid", "account_iid", "account_emailUser", "account_emailOfficial",
		                      "account_emailExposed", "account_connectAim", "account_connectFacebook", "account_connectGooglePlus", "account_connectLinkedIn",
		                      "account_connectSkype", "account_connectTwitter", "account_connectWeb", "account_profileInterests", "account_profileLocation",
		                      "account_profileOccupation", "account_timeZone", "account_avatar", "account_avatar_remove", "account_avatar_display",
		                      "account_password", "account_password2", "account_signature_editor", "account_registarAlert",
		                      "account_nameFields", "account_linkedinHelp", "account_facebookHelp", "account_attribution",
		                      "account_header","account_modebar", "account_samePassword", "account_strongPassword"]);

		me.portal = portal_tool.features({onExit:function(deferred){return me.checkExit(deferred);}, pin:[{ui:me.ui.account_header}]});
		me.user = me.portal.user;

		me.editor = new e3_Editor(me.ui.account_signature_editor, {height: 100});

		onChange(me.ui.account_avatar, function(target){me.acceptAvatarSelect(target);});
		onChange(me.ui.account_avatar_remove, function(target){me.acceptAvatarRemove(target);});
		
		onClick(me.ui.account_linkedinHelp, function(){me.portal.dialogs.openAlert("account_linkedin_help");});
		onClick(me.ui.account_facebookHelp, function(){me.portal.dialogs.openAlert("account_facebook_help");});
		setupHoverControls([me.ui.account_linkedinHelp, me.ui.account_facebookHelp]);

		me.ui.modebar = new e3_Modebar(me.ui.account_modebar);
		me.ui.modebar.set([], 0);
	};

	this.checkNewPw = function()
	{
		var same = (me.edit.newPassword == me.edit.newPasswordVerify);
		var strong = (((me.edit.newPassword != null) && (strongPassword(me.edit.newPassword))) || ((me.edit.newPasswordVerify != null) && (strongPassword(me.edit.newPasswordVerify))));
		show(me.ui.account_samePassword, !same);
		show(me.ui.account_strongPassword, !strong && ((me.edit.newPassword != null) || (me.edit.newPasswordVerify != null)));
	};

	this.saveCancel = function(mode, deferred)
	{
		if (mode)
		{
			me.save(deferred);
		}
		else
		{
			me.edit.revert();
			me.populate();
			if (deferred !== undefined) deferred();
		}
	};

	this.acceptAvatarSelect = function(target)
	{
		var fl = me.ui.account_avatar.prop("files");
		if ((fl != null) && (fl.length > 0))
		{
			reader = new FileReader();
			reader.onloadend = function(e)
			{
				me.ui.account_avatar_display.css("width", "128px").css("height", "auto");
				me.ui.account_avatar_display.attr("src", e.target.result);
				show(me.ui.account_avatar_display);
			};
			reader.readAsDataURL(fl[0]);
			me.ui.account_avatar_remove.prop("checked", false);

			me.edit.set(me.edit, "avatarNew", fl[0]);
			me.edit.set(me.edit, "avatarRemove", false);
		}
	};

	this.acceptAvatarRemove = function(target)
	{
		if (me.ui.account_avatar_remove.is(":checked"))
		{
			hide(me.ui.account_avatar_display);			
			me.edit.set(me.edit, "avatarRemove", true);
		}
		else
		{
			if ((me.edit.avatar != null) || (me.edit.avatarNew != null))
			{
				show(me.ui.account_avatar_display);
			}
			
			me.edit.set(me.edit, "avatarRemove", false);
		}
	};

	this.extractLinkedinId = function(value)
	{
		if (value == null) return "";

		var index = value.toLowerCase().indexOf("www.linkedin.com/in/");
		if (index == -1) return value.toLowerCase();
		return value.substring(index + "www.linkedin.com/in/".length).toLowerCase();
	};

	this.extractFacebookId = function(value)
	{
		if (value == null) return "";

		var index = value.toLowerCase().indexOf("facebook.com/");
		if (index == -1) return value.toLowerCase();
		return value.substring(index + "facebook.com/".length).toLowerCase();
	};

	this.start = function()
	{
		me.populate();
	};

	this.populate = function()
	{
		me.edit = new e3_Edit(me.user, ["createdOn", "createdBy", "modifiedOn", "modifiedBy"], function(changed)
		{
			me.ui.modebar.enableSaveDiscard(changed ? me.saveCancel : null);
		});
		me.edit.setFilters({"avatarNew": me.edit.noFilter, "avatarRemove": me.edit.noFilter, "emailExposed": me.edit.noFilter,
							"connectFacebook": me.extractFacebookId, "connectLinkedIn": me.extractLinkedinId,
							"newPassword": me.edit.stringFilter, "newPasswordVerify": me.edit.stringFilter, "defaultFilter": me.edit.stringZeroFilter});
		me.ui.modebar.enableSaveDiscard(null);

		me.edit.avatarRemove = false;

		me.edit.setupFilteredEdit(me.ui.account_nameFirst, me.edit, "nameFirst");
		me.edit.setupFilteredEdit(me.ui.account_nameLast, me.edit, "nameLast");
		
		if (me.user.rosterUser)
		{
			show(me.ui.account_registarAlert);
			me.ui.account_nameFields.attr("disabled", true);
		}
		else
		{
			hide(me.ui.account_registarAlert);
			me.ui.account_nameFields.removeAttr("disabled");
		}

		me.ui.account_eid.val(me.edit.eid);
		
		me.edit.setupFilteredEdit(me.ui.account_password, me.edit, "newPassword", me.checkNewPw);
		me.edit.setupFilteredEdit(me.ui.account_password2, me.edit, "newPasswordVerify", me.checkNewPw);
		me.checkNewPw();

		me.ui.account_emailOfficial.val(me.edit.emailOfficial);
		me.edit.setupFilteredEdit(me.ui.account_emailUser, me.edit, "emailUser");
		me.edit.setupCheckEdit(me.ui.account_emailExposed, me.edit, "emailExposed");
		
		// me.editor.myfilesUser(me.user.id); TODO: ???
		me.editor.disable();
		me.editor.set(me.edit.signature);
		me.editor.enable(function()
		{
			me.edit.set(me.edit, "signature", me.editor.get());
		}, false /* no focus */);

		me.edit.setupFilteredEdit(me.ui.account_connectAim, me.edit, "connectAim");
		me.edit.setupFilteredEdit(me.ui.account_connectFacebook, me.edit, "connectFacebook");
		me.edit.setupFilteredEdit(me.ui.account_connectGooglePlus, me.edit, "connectGooglePlus");
		me.edit.setupFilteredEdit(me.ui.account_connectLinkedIn, me.edit, "connectLinkedIn");
		me.edit.setupFilteredEdit(me.ui.account_connectSkype, me.edit, "connectSkype");
		me.edit.setupFilteredEdit(me.ui.account_connectTwitter, me.edit, "connectTwitter");
		me.edit.setupFilteredEdit(me.ui.account_connectWeb, me.edit, "connectWeb");

//		me.edit.setupFilteredEdit(me.ui.account_profileInterests, me.edit, "profileInterests");
		me.edit.setupFilteredEdit(me.ui.account_profileLocation, me.edit, "profileLocation");
		me.edit.setupFilteredEdit(me.ui.account_profileOccupation, me.edit, "profileOccupation");

		me.ui.account_timeZone.val(me.edit.timeZone);
		onChange(me.ui.account_timeZone, function()
		{
			me.edit.set(me.edit, "timeZone", me.ui.account_timeZone.val());
		});

		me.ui.account_avatar.val("");
		me.ui.account_avatar_remove.prop("checked", false);
		if (me.edit.avatar != null)
		{
			me.ui.account_avatar_display.css("width", "128px").css("height", "auto");
			me.ui.account_avatar_display.attr("src", me.edit.avatar);
			show(me.ui.account_avatar_display);
		}
		else
		{
			hide(me.ui.account_avatar_display);
		}

		new e3_Attribution().inject(me.ui.account_attribution, me.user, {label: me.i18n.lookup("label_iid", "college ID:"), id: "iid"});
	};

	this.save = function(deferred)
	{
		var params = me.portal.cdp.params();
		params.url.user = me.edit.id;
		params.post.fetch = true;
		me.edit.params("", params);
		me.portal.cdp.request("user_save", params, function(data)
		{
			if (data.user != null) me.user = me.portal.updateUser(data.user);
			me.populate();
			if(deferred !== undefined) deferred();
		});
	};

	this.checkExit = function(deferred)
	{
		if (me.edit.changed())
		{
			me.portal.confirmNavigationWithChanges(function()
			{
				me.save(deferred);
			}, function()
			{
				me.edit.revert();
				if (deferred !== undefined) deferred();			
			});

			return false;
		}

		return true;
	};
}

$(function()
{
	try
	{
		account_tool = new Account();
		account_tool.init();
		account_tool.start();
	}
	catch (e)
	{
		error(e);
	}
});

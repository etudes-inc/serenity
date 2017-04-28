/**********************************************************************************
 * $URL: https://source.etudes.org/svn/serenity/trunk/social/social-webapp/src/main/webapp/social.js $
 * $Id: social.js 11740 2015-10-01 18:43:09Z ggolden $
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

var social_tool = null;

function SocialMessages(main)
{
	var me = this;
	this.ui = null;
	
	this.modes =
	[
		{name:main.i18n.lookup("header_inbox", "Inbox"), icon:"/social/art/pm_inbox.png", func:function(){me.startInbox();}},
		{name:main.i18n.lookup("header_drafts", "Drafts"), icon:"/social/art/pm_draft_folder.png", func:function(){me.starDrafts();}},
		{name:main.i18n.lookup("header_sent", "Sent"), icon:"/social/art/pm_sentbox.png", func:function(){me.startSent();}},
		{name:main.i18n.lookup("header_folders", "Folders"), icon:"/social/art/folders.png", func:function(){me.startFolders();}},
	];

	this.init = function()
	{
		me.ui = findElements(["social_msg_modebar","social_msg_actions","social_msg_action_add","social_msg_table","social_msg_none"]);
		me.ui.modebar = new e3_Modebar(me.ui.social_msg_modebar);
		me.ui.modebar.set(me.modes, 0);
		me.ui.table = new e3_Table(me.ui.social_msg_table);
		onClick(me.ui.social_msg_action_add, function(){console.log("compose")});
	};
	
	this.start = function()
	{
		me.startInbox();
	};

	this.startInbox = function()
	{
		console.log("startInbox");
		// TODO: load...
		me.populateInbox();
	};

	this.starDrafts = function()
	{
		console.log("starDrafts");
	};

	this.startSent = function()
	{
		console.log("startSent");
	};

	this.startFolders = function()
	{
		console.log("startFolders");
	};
	
	this.populateInbox = function()
	{
		me.ui.table.clear();
//		$.each(main.assessments, function(index, assessment)
//		{
//			me.table.row();
//			if (main.permissions.mayEdit)
//			{
//				me.table.selectBox(assessment.id);
//			}
//			else
//			{
//				me.table.text("", "tight");
//			}
//
//			// title
//			me.table.hotText(assessment.title, main.i18n.lookup("click", "Click") + " " + assessment.title, function()
//			{
//				main.startEditAssessment(assessment);
//			});
//
//			// dates
//			me.table.text(main.timestamp.display(assessment.schedule.open));
//			me.table.text(main.timestamp.display(assessment.schedule.due));
//			me.table.text(main.timestamp.display(assessment.schedule.allowUntil));
//			
//			// grade
//			me.table.icon("/ui/icons/grade.png", main.i18n.lookup("action_grade", "Grade"), function(){main.startGradeAssessment(assessment);});
//		});
		
		show(me.ui.social_msg_none, me.ui.table.rowCount() == 0);

		me.ui.table.sort({0:{sorter:false}, 1:{sorter:false}, 2:{sorter:false}}, []);
		me.ui.table.done();
	};
}

function SocialChat(main)
{
	var me = this;

	this.init = function()
	{
		me.ui = findElements(["social_chat_msg"]);
		onEnter(me.ui.social_chat_msg, function(){console.log("send msg: " + me.ui.social_chat_msg.val());});
	};

	this.start = function()
	{
	};
}

function SocialPresence(main)
{
	var me = this;

	this.init = function()
	{
	};

	this.start = function()
	{
	};
}

function SocialRoster(main)
{
	var me = this;

	this.ui = null;
	this.rosters = [];

	this.init = function()
	{
		me.ui = findElements(["social_roster","social_roster_template"]);
	};

	this.start = function()
	{
	};
	
	this.populate = function()
	{
		me.ui.social_roster.empty();
		$.each(me.rosters, function(index, member)
		{
			// get an entry from the template
			var tab = clone(me.ui.social_roster_template,
					["social_roster_template_avatar",
					"social_roster_template_name",
					"social_roster_template_byline",
					"social_roster_template_pm",
					"social_roster_template_email",
					"social_roster_template_fb",
					"social_roster_template_in",
					"social_roster_template_twitter",
					"social_roster_template_google",
					"social_roster_template_aol",
					"social_roster_template_skype",
					"social_roster_template_web"]);
			
			tab.social_roster_template_avatar.attr("src", member.avatar == null ? "/social/art/android.png" : member.avatar);
			tab.social_roster_template_name.text(member.nameDisplay);
			var byline = "";
			if (member.profileOccupation != null)
			{
				byline += member.profileOccupation;
				if (member.profileLocation != null) byline += ", ";
			}
			if (member.profileLocation != null) byline += member.profileLocation;
			tab.social_roster_template_byline.text(byline);
			onClick(tab.social_roster_template_pm, function(){alert("PM");});
			onClick(tab.social_roster_template_email, function(){alert("EMAIL");});
			show(tab.social_roster_template_email, member.emailExposed);
			onClick(tab.social_roster_template_fb, function(){alert("FACEBOOK");});
			show(tab.social_roster_template_fb, member.connectFacebook != null);
			onClick(tab.social_roster_template_in, function(){alert("LINKEDIN");});
			show(tab.social_roster_template_in, member.connectLinkedIn != null);
			onClick(tab.social_roster_template_twitter,function(){alert("TWITTER");});
			show(tab.social_roster_template_twitter, member.connectTwitter != null);
			onClick(tab.social_roster_template_google,function(){alert("GOOGLE+");});
			show(tab.social_roster_template_google, member.connectGooglePlus != null);
			onClick(tab.social_roster_template_aol,function(){alert("AIM");});
			show(tab.social_roster_template_aol, member.connectAim != null);
			onClick(tab.social_roster_template_skype,function(){alert("SKYPE");});
			show(tab.social_roster_template_skype, member.connectSkype != null);
			onClick(tab.social_roster_template_web,function(){alert("WEB");});
			show(tab.social_roster_template_web, member.connectWeb != null);

			show(tab.element);
			me.ui.social_roster.append(tab.element);
		});
	};
}

function Social()
{
	var me = this;

	this.portal = null;
	this.i18n = new e3_i18n(social_i10n, "en-us");
	this.messages = null;
	this.chat = null;
	this.presence = null;
	this.roster = null;

	this.init = function()
	{
		me.i18n.localize();
		me.portal = portal_tool.features(me.i18n.lookup("titlebar", "Social"));
		me.ui = findElements([]);

		me.messages = new SocialMessages(me);
		me.chat = new SocialChat(me);
		me.presence = new SocialPresence(me);
		me.roster = new SocialRoster(me);
		me.messages.init();
		me.chat.init();
		me.presence.init();
		me.roster.init();
	};

	this.start = function()
	{
		me.messages.start()
		me.chat.start();
		me.presence.start();
		me.roster.start();

		var params = me.portal.cdp.params();
		params.url.site = me.portal.site.id;
		me.portal.cdp.request("social_roster", params, function(data)
		{
			me.roster.rosters = data.roster || [];
			me.roster.populate();
		});

//		var params = me.portal.cdp.params();
//		me.portal.cdp.request("dashboard_events dashboard_announcements", params, function(data)
//		{
//			me.calendar.eventsDate = data.eventsDate;
//			me.calendar.populateEvents();
//			me.news.announcements = data.announcements;
//			me.news.populateAnnouncements();
//		});
	};
}

$(function()
{
	try
	{
		social_tool = new Social();
		social_tool.init();
		social_tool.start();
	}
	catch (e)
	{
		error(e);
	}
});

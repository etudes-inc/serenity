/**********************************************************************************
 * $URL: https://source.etudes.org/svn/serenity/trunk/core/core-api/src/main/java/org/etudes/tool/api/Tool.java $
 * $Id: Tool.java 12553 2016-01-14 20:03:28Z ggolden $
 ***********************************************************************************
 *
 * Copyright (c) 2014, 2015, 2016 Etudes, Inc.
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

package org.etudes.tool.api;

import java.util.Comparator;
import java.util.HashMap;
import java.util.Map;

import org.etudes.roster.api.Role;

/**
 * Tool models the Etudes tools Note: some values are hard-coded in portal.js
 */
public enum Tool
{
	// open: 118, 4, 53
	account(51, "/user/account", "Account", Role.authenticated, 93), //
	activity(112, "/activity/activity", "Activity Meter", Role.instructor, 140), //
	announcement(104, "/announcement/announcement", "Announcements", Role.guest, 103), //
	archive(5, "/archive/archive", "Archives", Role.admin, 5), //
	assessment(107, "/assessment/assessment", "Assessments", Role.guest, 106), //
	assessmentAnswer(201, "/assessment/assessment", "Assessments", Role.guest, 106), // special non-tool for reference
	assessmentQuestion(202, "/assessment/assessment", "Assessments", Role.guest, 106), // special non-tool for reference
	assessmentSubmission(203, "/assessment/assessment", "Assessments", Role.guest, 106), // special non-tool for reference
	blog(100, "/blog/blog", "Blogs", Role.guest, 108), //
	chat(109, "/social/social", "Chat", Role.guest, 192), //
	coursemap(102, "/coursemap/coursemap", "Course Map", Role.guest, 101), //
	dashboard(54, "/dashboard/dashboard", "Dashboard", Role.authenticated, 90), //
	evaluation(111, "/evaluation/evaluation", "Grades", Role.student, 120), //
	forum(108, "/forum/forum", "Discussions", Role.guest, 107), //
	home(101, "/home/home", "Home", Role.guest, 100), //
	login(10, "/user/login", "Login", Role.anonymous, 10), //
	member(116, "/social/social", "Members", Role.guest, 194), //
	message(117, "/social/social", "Messages", Role.guest, 191), //
	module(106, "/module/module", "Modules", Role.guest, 105), //
	monitor(7, "/monitor/monitor", "Monitor", Role.guest, 1), //
	motd(3, "/motd/motd", "MOTD", Role.admin, 3), //
	myfiles(50, "/myfiles/myfiles", "My Files", Role.authenticated, 92), //
	mysites(52, "/site/mysites", "My Sites", Role.authenticated, 91), //
	none(0, null, null, Role.none, 0), //
	online(4, "/online/online", "Online", Role.admin, 4), //
	portal(99, null, null, Role.authenticated, 0), //
	presence(98, "/social/social", "Online", Role.guest, 193), //
	resetPw(8, "/user/resetpw", "Reset Password", Role.anonymous, 8), //
	resource(110, "/resource/resource", "Resources", Role.guest, 110), //
	roster(2, "/roster/roster", "Rosters", Role.admin, 3), //
	schedule(103, "/schedule/schedule", "Calendar", Role.guest, 102), //
	site(6, "/site/site", "Sites", Role.admin, 90), //
	sitebrowser(9, "/site/browser", "Browse Sites", Role.anonymous, 9), //
	siteroster(114, "/roster/siteroster", "Site Roster", Role.instructor, 302), //
	sitesetup(113, "/site/setup", "Site Setup", Role.instructor, 301), // TODO: remove when the social cdp handler no longer needs
	social(115, "/social/social", "Social", Role.guest, 130), //
	syllabus(105, "/syllabus/syllabus", "Syllabus", Role.guest, 104), //
	user(1, "/user/user", "Users", Role.admin, 2);

	public static class ToolOrderComparator implements Comparator<Tool>
	{
		public int compare(Tool o1, Tool o2)
		{
			return o1.getOrder().compareTo(o2.getOrder());
		}
	}

	public static Tool valueOf(Integer i)
	{
		for (Tool r : Tool.values())
		{
			if (r.id.equals(i)) return r;
		}
		return none;
	}

	private final Integer id;
	private final Integer order;
	private final Role role;
	private final String title;

	private final String url;

	private Tool(int id, String url, String title, Role role, int order)
	{
		this.id = Integer.valueOf(id);
		this.url = url;
		this.title = title;
		this.role = role;
		this.order = order;
	}

	public Integer getId()
	{
		return this.id;
	}

	public Integer getOrder()
	{
		return this.order;
	}

	public Role getRole()
	{
		return this.role;
	}

	public String getTitle()
	{
		return this.title;
	}

	public String getUrl()
	{
		return this.url;
	}

	public Map<String, Object> send()
	{
		Map<String, Object> rv = new HashMap<String, Object>();

		rv.put("id", getId());
		// rv.put("title", getTitle());
		// rv.put("url", getUrl());
		rv.put("role", getRole().getLevel());

		return rv;
	}

}

/**********************************************************************************
 * $URL: https://source.etudes.org/svn/serenity/trunk/core/core-api/src/main/java/org/etudes/tool/api/ToolItemType.java $
 * $Id: ToolItemType.java 11914 2015-10-23 03:22:25Z ggolden $
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

package org.etudes.tool.api;

import java.util.HashMap;
import java.util.Map;

/**
 * ToolItemTyoe models the different types of items in the tools Note: values set in extend.js
 */
public enum ToolItemType
{
	// Note: items < 2000 are gradable
	assignment(3, Tool.assessment, "Assignment"), //
	blog(2, Tool.blog, "Blog"), //
	chat(2001, Tool.chat, "Chat"), //
	essay(6, Tool.assessment, "Essay"), //
	event(2006, Tool.schedule, "Event"), //
	extra(1001, Tool.evaluation, "Extra Credit"), //
	fce(2002, Tool.assessment, "Formal Evaluation"), //
	forum(1, Tool.forum, "Forum"), //
	header(2007, Tool.coursemap, "Header"), //
	module(2005, Tool.module, "Module"), //
	none(0, Tool.none, ""), //
	offline(4, Tool.assessment, "Offline"), //
	survey(2003, Tool.assessment, "Survey"), //
	syllabus(2004, Tool.syllabus, "Syllabus"), //
	test(5, Tool.assessment, "Test");

	public static ToolItemType valueOf(Integer i)
	{
		for (ToolItemType r : ToolItemType.values())
		{
			if (r.id.equals(i)) return r;
		}
		return none;
	}

	private final Integer id;
	private final String title;
	private final Tool tool;

	private ToolItemType(int id, Tool tool, String title)
	{
		this.id = Integer.valueOf(id);
		this.tool = tool;
		this.title = title;
	}

	public Integer getId()
	{
		return this.id;
	}

	public String getTitle()
	{
		return this.title;
	}

	public Tool getTool()
	{
		return this.tool;
	}

	/**
	 * Format for sending via CDP.
	 * 
	 * @return The map, ready to add as an element to the return map.
	 */
	public Map<String, Object> send()
	{
		Map<String, Object> rv = new HashMap<String, Object>();

		rv.put("id", this.id);
		rv.put("title", this.title);
		rv.put("tool", this.tool.getId());

		return rv;
	}
}

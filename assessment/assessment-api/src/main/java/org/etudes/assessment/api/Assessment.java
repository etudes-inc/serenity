/**********************************************************************************
 * $URL: https://source.etudes.org/svn/serenity/trunk/assessment/assessment-api/src/main/java/org/etudes/assessment/api/Assessment.java $
 * $Id: Assessment.java 11586 2015-09-09 20:01:27Z ggolden $
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

package org.etudes.assessment.api;

import java.util.Date;
import java.util.Map;
import java.util.Set;

import org.etudes.entity.api.Schedule;
import org.etudes.evaluation.api.EvaluationDesign;
import org.etudes.file.api.File;
import org.etudes.site.api.Site;
import org.etudes.tool.api.ToolItemReference;
import org.etudes.tool.api.ToolItemType;
import org.etudes.user.api.User;

/**
 * Assessment models an Etudes assessment.
 */
public interface Assessment
{
	enum Type
	{
		assignment("A"), essay("E"), fce("F"), offline("O"), survey("S"), test("T");

		public static Type fromCode(String c)
		{
			for (Type s : Type.values())
			{
				if (s.code.equals(c)) return s;
			}
			return assignment;
		}

		private String code;

		private Type(String code)
		{
			this.code = code;
		}

		public String getCode()
		{
			return this.code;
		}
	};

	/**
	 * @return the user who created this.
	 */
	User getCreatedBy();

	/**
	 * @return the date created.
	 */
	Date getCreatedOn();

	/**
	 * @return The assessment's evaluation design.
	 */
	EvaluationDesign getEvaluation();

	/**
	 * @return The syllabus id.
	 */
	Long getId();

	/**
	 * Get the instructions (html).
	 * 
	 * @param forDownload
	 *        if true, modify the content to convert embedded references from stored placeholder URLs to download URLs using references for this item.
	 * @return The instructions. If not defined, null.
	 */
	String getInstructions(boolean forDownload);

	/**
	 * @return the user who last modified this.
	 */
	User getModifiedBy();

	/**
	 * @return the date last modified.
	 */
	Date getModifiedOn();

	/**
	 * @return TRUE if published, FALSE if not.
	 */
	Boolean getPublished();

	/**
	 * Return the tool item reference to this assessment.
	 * 
	 * @return The ToolItemReference.
	 */
	ToolItemReference getReference();

	/**
	 * Collect all the User objects referenced in the assessment (created by, modified by ...)
	 * 
	 * @return The Set of User objects, possibly empty.
	 */
	Set<User> getReferencedUsers();

	/**
	 * @return The open / due / allow until schedule.
	 */
	Schedule getSchedule();

	/**
	 * @return The site in which the assessment lives.
	 */
	Site getSite();

	/**
	 * @return The title of the assessment.
	 */
	String getTitle();

	/**
	 * @return The ToolItemType.
	 */
	ToolItemType getToolItemType();

	/**
	 * @return the max # tries allowed, or null for unlimited.
	 */
	Integer getTries();

	/**
	 * @return The assessment type.
	 */
	Type getType();

	/**
	 * Check if someone with this many tries has any more tries for the assessment
	 * 
	 * @param count
	 *        The # tries already made
	 * @return TRUE if more tries are allowed, FALSE if not.
	 */
	Boolean isTriesRemain(Integer count);

	/**
	 * Update from CDP parameters.
	 * 
	 * @param prefix
	 *        The parameter names prefix.
	 * @param parameters
	 *        The parameters.
	 */
	void read(String prefix, Map<String, Object> parameters);

	/**
	 * Format for sending via CDP.
	 * 
	 * @return The map, ready to add as an "assessment" element to the return map.
	 */
	Map<String, Object> send();

	/**
	 * Set the instructions (html with embedded URLs in "placeholder" format).
	 * 
	 * @param content
	 *        The new instructions, or null to remove the instructions.
	 * @param downloadReferenceFormat
	 *        if true, the content has embedded references in download (not placeholder) format.
	 */
	void setInstructions(String content, boolean downloadReferenceFormat);

	/**
	 * Set the published status.
	 * 
	 * @param published
	 *        The published status.
	 */
	void setPublished(Boolean published);

	/**
	 * Set the assessment title.
	 * 
	 * @param title
	 *        The title.
	 */
	void setTitle(String title);

	/**
	 * Set the # tries.
	 * 
	 * @param tries
	 *        The max # tries, or null for unlimited.
	 */
	void setTries(Integer tries);

	/**
	 * Set the type.
	 * 
	 * @param type
	 *        The assessment type.
	 */
	void setType(Type type);

	/**
	 * Set the instructions to share an existing file system file.
	 * 
	 * @param file
	 *        The file to use.
	 */
	void shareInstructions(File file);
}

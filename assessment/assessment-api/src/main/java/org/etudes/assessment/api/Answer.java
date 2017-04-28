/**********************************************************************************
 * $URL: https://source.etudes.org/svn/serenity/trunk/assessment/assessment-api/src/main/java/org/etudes/assessment/api/Answer.java $
 * $Id: Answer.java 11561 2015-09-06 00:45:58Z ggolden $
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

import org.etudes.tool.api.ToolItemReference;

/**
 * Answer models one answer in a submission to an assessment.
 */
public interface Answer
{
	/**
	 * @return The latest date the answer was submitted.
	 */
	Date getAnsweredOn();

	/**
	 * Get the content (html).
	 * 
	 * @param forDownload
	 *        if true, modify the content to convert embedded references from stored placeholder URLs to download URLs using references for this item.
	 * @return The instructions. If not defined, null.
	 */
	String getContent(boolean forDownload);

	/**
	 * Get the answer's non-contents data.
	 * 
	 * @return The answer's data, or null if none.
	 */
	String getData();

	/**
	 * @return The answer id.
	 */
	Long getId();

	/**
	 * @return The question this is an answer to.
	 */
	Question getQuestion();

	/**
	 * @return The tool item reference for this answer.
	 */
	ToolItemReference getReference();

	/**
	 * @return The site in which the answer lives.
	 */
	Submission getSubmission();

	/**
	 * @return TRUE if the answer has been answered by the user, FALSE if not.
	 */
	Boolean isAnswered();

	/**
	 * @return the marked for review setting (TRUE or FALSE).
	 */
	Boolean isMarkedForReview();

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
	 * @return The map, ready to add as an element of the return map.
	 */
	Map<String, Object> send();

	/**
	 * Set the content (html with embedded URLs in "placeholder" format).
	 * 
	 * @param content
	 *        The new content, or null to remove the instructions.
	 * @param downloadReferenceFormat
	 *        if true, the content has embedded references in download (not placeholder) format.
	 */
	void setContent(String content, boolean downloadReferenceFormat);

	/**
	 * Set the answer data.
	 * 
	 * @param data
	 *        The new data.
	 */
	void setData(String data);

	/**
	 * Set marked for review
	 * 
	 * @param setting
	 *        The marked for review setting.
	 */
	void setMarkedForReview(Boolean setting);
}

/**********************************************************************************
 * $URL: https://source.etudes.org/svn/serenity/trunk/assessment/assessment-api/src/main/java/org/etudes/assessment/api/Question.java $
 * $Id: Question.java 11559 2015-09-04 22:52:46Z ggolden $
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

import java.util.Map;

import org.etudes.site.api.Site;
import org.etudes.tool.api.ToolItemReference;

/**
 * Question models one question in an assessment.
 */
public interface Question
{
	/**
	 * @return The question id.
	 */
	Long getId();

	/**
	 * @return The tool item reference for this question.
	 */
	ToolItemReference getReference();

	/**
	 * @return The site in which the question lives.
	 */
	Site getSite();

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
}

/**********************************************************************************
 * $URL: https://source.etudes.org/svn/serenity/trunk/assessment/assessment-api/src/main/java/org/etudes/assessment/api/Submission.java $
 * $Id: Submission.java 11561 2015-09-06 00:45:58Z ggolden $
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
import java.util.List;
import java.util.Map;
import java.util.Set;

import org.etudes.evaluation.api.Evaluation;
import org.etudes.tool.api.ToolItemReference;
import org.etudes.tool.api.ToolItemWorkReference;
import org.etudes.user.api.User;

/**
 * Submission models a submission to an assessment.
 */
public interface Submission
{
	/**
	 * @return The answers in the submission.
	 */
	List<Answer> getAnswers();

	/**
	 * @return The submissions's assessment.
	 */
	Assessment getAssessment();

	/**
	 * @return the date finished, or null if not yet finished.
	 */
	Date getFinished();

	/**
	 * @return The submission id.
	 */
	Long getId();

	/**
	 * @return The tool item reference for this submission.
	 */
	ToolItemReference getReference();

	/**
	 * Collect all the User objects referenced in the syllabus (user ...)
	 * 
	 * @return The Set of User objects, possibly empty.
	 */
	Set<User> getReferencedUsers();

	/**
	 * @return the date started.
	 */
	Date getStarted();

	/**
	 * @return The submission's user.
	 */
	User getUser();

	/**
	 * @return The tool item work reference for this submission.
	 */
	ToolItemWorkReference getWorkReference();

	/**
	 * @return TRUE if the submission is complete, FALSE if not.
	 */
	Boolean isComplete();

	/**
	 * @return TRUE if the submission is in-progress (started, not yet finished), FALSE if not.
	 */
	Boolean isInProgress();

	/**
	 * Prepare a submission for return.
	 * 
	 * @param userRosterName
	 *        (optional) the user's site membership roster name (if included, the "userRoster" will be included in the return).
	 * @param evaluations
	 *        The official evaluation for the submission, or null if none.
	 * @return The syllabus return map.
	 */
	Map<String, Object> send(String userRosterName, Evaluation e);
}

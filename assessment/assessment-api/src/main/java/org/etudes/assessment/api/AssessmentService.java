/**********************************************************************************
 * $URL: https://source.etudes.org/svn/serenity/trunk/assessment/assessment-api/src/main/java/org/etudes/assessment/api/AssessmentService.java $
 * $Id: AssessmentService.java 11682 2015-09-19 21:03:52Z ggolden $
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

import java.util.List;

import org.etudes.site.api.Site;
import org.etudes.user.api.User;

/**
 * The Serenity Assessment service.
 */
public interface AssessmentService
{
	enum SubmitStatus
	{
		again("A"), nothing("N"), resume("R"), start("S"), testdrive("T");

		public static SubmitStatus fromCode(String c)
		{
			for (SubmitStatus s : SubmitStatus.values())
			{
				if (s.code.equals(c)) return s;
			}
			return nothing;
		}

		private String code;

		private SubmitStatus(String code)
		{
			this.code = code;
		}

		public String getCode()
		{
			return this.code;
		}
	}

	/**
	 * Get an answer by id.
	 * 
	 * @param id
	 *        The answer id.
	 * @return The answer, or null if not found.
	 */
	Answer answerGet(Long id);;

	/**
	 * Save an answer
	 * 
	 * @param savedBy
	 *        The user saving.
	 * @param answer
	 *        The answer.
	 */
	void answerSave(User savedBy, Answer answer);

	/**
	 * Create a new assessment in the site
	 * 
	 * @param addedBy
	 *        The user doing the adding.
	 * @param inSite
	 *        The site holding the assessment.
	 * @return The added assessment.
	 */
	Assessment assessmentAdd(User addedBy, Site inSite);

	/**
	 * Check that this id is a valid assessment id. Use check() instead of get() if you don't need the full assessment information loaded.
	 * 
	 * @param id
	 *        The assessment id.
	 * @return An Assessment object with the id (at least) set, or null if not found
	 */
	Assessment assessmentCheck(Long id);

	/**
	 * Clear all submissions from this assessment.
	 * 
	 * @param assessment
	 *        The assessment.
	 */
	void assessmentClear(Assessment assessment);

	/**
	 * Clear all submissions for this user from this assessment.
	 * 
	 * @param assessment
	 *        The assessment.
	 * @param user
	 *        The user.
	 */
	void assessmentClear(Assessment assessment, User user);

	/**
	 * Find the assessments in this site.
	 * 
	 * @param inSite
	 *        The site holding the assessments.
	 * @return The site's assessments.
	 */
	List<Assessment> assessmentFindBySite(Site inSite);

	/**
	 * Find the assessments in this list that have this title (case insensitive).
	 * 
	 * @param assessments
	 *        The assessment list.
	 * @param title
	 *        The title.
	 * @return The list of assessments from the list with this title, may be empty.
	 */
	List<Assessment> assessmentFindByTitle(List<Assessment> assessments, String title);

	/**
	 * Find the assessments in this site that have this title (case insensitive).
	 * 
	 * @param site
	 *        The site.
	 * @param title
	 *        The title.
	 * @return The List of Assessments in the site with this title; may be empty.
	 */
	List<Assessment> assessmentFindByTitle(Site site, String title);

	/**
	 * Get the assessment with this id.
	 * 
	 * @param id
	 *        The assessment id.
	 * @return The Assessment with this id, or null if not found.
	 */
	Assessment assessmentGet(Long id);

	/**
	 * Refresh this Assessment object with a full data load from the database, overwriting any values, setting it to unchanged.
	 * 
	 * @param assessment
	 *        The assessment.
	 */
	void assessmentRefresh(Assessment assessment);

	/**
	 * Remove this assessment.
	 * 
	 * @param assessment
	 *        The assessment.
	 */
	void assessmentRemove(Assessment assessment);

	/**
	 * Save any changes made to this assessment.
	 * 
	 * @param savedBy
	 *        The user making the save.
	 * @param assessment
	 *        The assessment to save.
	 */
	void assessmentSave(User savedBy, Assessment assessment);

	/**
	 * Encapsulate an assessment id into an Assessment object. The id is not checked.
	 * 
	 * @param id
	 *        The assessment id.
	 * @return A Assessment object with this id set.
	 */
	Assessment assessmentWrap(Long id);

	/**
	 * Figure the user's submit status for this assessment.
	 * 
	 * @param submissions
	 *        A set of the user's submissions, which may include one or more for this assessment.
	 * @param user
	 *        The user.
	 * @param assessment
	 *        The assessment.
	 * @return THe SubmitStatus
	 */
	SubmitStatus getSubmitStatus(List<Submission> submissions, User user, Assessment assessment);

	/**
	 * Encapsulate a question id into a Question object. The id is not checked.
	 * 
	 * @param id
	 *        The question id.
	 * @return A Question object with this id set.
	 */
	Question questionWrap(Long id);

	/**
	 * Find the submissions to this assessment.
	 * 
	 * @param assessment
	 *        The assessment.
	 * @return The List of submissions, may be empty.
	 */
	List<Submission> submissionFindByAssessment(Assessment assessment);

	/**
	 * Find the submissions from this user in this site.
	 * 
	 * @param inSite
	 *        The site holding the submissions.
	 * @param user
	 *        The user.
	 * @return The List of submissions, may be empty.
	 */
	List<Submission> submissionFindBySite(Site inSite, User user);

	/**
	 * Find the submissions in the list of submissions from this user to this assessment.
	 * 
	 * @param submissions
	 *        The submission list.
	 * @param user
	 *        The user.
	 * @param assessment
	 *        The assessment.
	 * @return The List of submissions, may be empty.
	 */
	List<Submission> submissionFindByUserAssessment(List<Submission> submissions, User user, Assessment assessment);

	/**
	 * Find the submissions from this user to this assessment.
	 * 
	 * @param user
	 *        The user.
	 * @param assessment
	 *        The assessment.
	 * @return The List of submissions, may be empty.
	 */
	List<Submission> submissionFindByUserAssessment(User user, Assessment assessment);

	/**
	 * Finish a submission.
	 * 
	 * @param submission
	 */
	void submissionFinish(Submission submission);

	/**
	 * Get a submission by id, from the list.
	 * 
	 * @param submissions
	 *        The list of submissions.
	 * @param id
	 *        The submission id.
	 * @return The submission, or null if not found;
	 */
	Submission submissionGet(List<Submission> submissions, Long id);

	/**
	 * Get a submission.
	 * 
	 * @param id
	 *        The submission id.
	 * @return The submission, or null if not found.
	 */
	Submission submissionGet(Long id);

	/**
	 * Refresh this Submission object with a full data load from the database (except answers), overwriting any values, setting it to unchanged.
	 * 
	 * @param submission
	 *        The submission.
	 */
	void submissionRefresh(Submission submission);

	/**
	 * Refresh this Submission's answers with a full data load from the database, overwriting any values.
	 * 
	 * @param submission
	 *        The submission.
	 */
	void submissionRefreshAnswers(Submission submission);

	/**
	 * Remove this submission.
	 * 
	 * @param submission
	 *        The submission.
	 */
	void submissionRemove(Submission submission);

	/**
	 * Start a new submission for this user to this assessment.
	 * 
	 * @param user
	 *        The user.
	 * @param assessment
	 *        The assessment.
	 * @return The submission, or null if a submission could not be started.
	 */
	Submission submissionStart(User user, Assessment assessment);

	/**
	 * Encapsulate a submission id into a Submission object. The id is not checked.
	 * 
	 * @param id
	 *        The submission id.
	 * @return A Submission object with this id set.
	 */
	Submission submissionWrap(Long id);
}

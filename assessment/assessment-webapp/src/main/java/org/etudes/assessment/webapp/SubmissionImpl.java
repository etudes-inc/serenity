/**********************************************************************************
 * $URL: https://source.etudes.org/svn/serenity/trunk/assessment/assessment-webapp/src/main/java/org/etudes/assessment/webapp/SubmissionImpl.java $
 * $Id: SubmissionImpl.java 11561 2015-09-06 00:45:58Z ggolden $
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

package org.etudes.assessment.webapp;

import static org.etudes.util.Different.different;

import java.util.ArrayList;
import java.util.Date;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;

import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;
import org.etudes.assessment.api.Answer;
import org.etudes.assessment.api.Assessment;
import org.etudes.assessment.api.AssessmentService;
import org.etudes.assessment.api.Submission;
import org.etudes.cdp.api.CdpService;
import org.etudes.evaluation.api.Evaluation;
import org.etudes.service.api.Services;
import org.etudes.tool.api.Tool;
import org.etudes.tool.api.ToolItemReference;
import org.etudes.tool.api.ToolItemWorkReference;
import org.etudes.user.api.User;

/**
 * SubmissionImpl implements Submission.
 */
public class SubmissionImpl implements Submission
{
	/** Our log. */
	private static Log M_log = LogFactory.getLog(SubmissionImpl.class);

	protected List<Answer> answers = new ArrayList<Answer>();
	protected boolean answersLoaded = false;
	protected Assessment assessment = null;
	protected boolean changed = false;
	protected Date finished = null;
	protected Long id = null;
	protected boolean loaded = false;
	protected Date started = null;
	protected User user = null;

	public SubmissionImpl()
	{
	}

	public SubmissionImpl(Long id)
	{
		this.id = id;
	}

	@Override
	public boolean equals(Object obj)
	{
		if (this == obj) return true;
		if (obj == null) return false;
		if (!(obj instanceof SubmissionImpl)) return false;
		SubmissionImpl other = (SubmissionImpl) obj;
		if (different(id, other.id)) return false;
		return true;
	}

	@Override
	public List<Answer> getAnswers()
	{
		load();
		loadAnswers();
		return this.answers;
	}

	@Override
	public Assessment getAssessment()
	{
		load();
		return this.assessment;
	}

	@Override
	public Date getFinished()
	{
		load();
		return this.finished;
	}

	@Override
	public Long getId()
	{
		return this.id;
	}

	@Override
	public ToolItemReference getReference()
	{
		return new ToolItemReference(getAssessment().getSite(), Tool.assessmentSubmission, getId());
	}

	@Override
	public Set<User> getReferencedUsers()
	{
		Set<User> rv = new HashSet<User>();
		rv.add(getUser());

		return rv;
	}

	@Override
	public Date getStarted()
	{
		load();
		return this.started;
	}

	@Override
	public User getUser()
	{
		load();
		return this.user;
	}

	@Override
	public ToolItemWorkReference getWorkReference()
	{
		// reference the assessment as the item
		return new ToolItemWorkReference(getAssessment().getReference(), this.id, getUser(), getFinished());
	}

	@Override
	public int hashCode()
	{
		final int prime = 31;
		int result = 1;
		result = prime * result + ((id == null) ? 0 : id.hashCode());
		return result;
	}

	@Override
	public Boolean isComplete()
	{
		return Boolean.valueOf((getStarted() != null) && (getFinished() != null));
	}

	@Override
	public Boolean isInProgress()
	{
		return Boolean.valueOf((getStarted() != null) && (getFinished() == null));
	}

	@Override
	public Map<String, Object> send(String userRosterName, Evaluation e)
	{
		Map<String, Object> submissionMap = new HashMap<String, Object>();
		submissionMap.put("id", getId());
		submissionMap.put("userId", getUser().getId());
		submissionMap.put("userNameSort", getUser().getNameSort());
		submissionMap.put("userName", getUser().getNameDisplay());
		if (userRosterName != null) submissionMap.put("userRoster", userRosterName);
		if (getStarted() != null) submissionMap.put("started", cdpService().sendDate(getStarted()));
		if (getFinished() != null) submissionMap.put("finished", cdpService().sendDate(getFinished()));
		submissionMap.put("inProgress", isInProgress());
		submissionMap.put("complete", isComplete());
		if (e != null)
		{
			Map<String, Object> evaluationMap = e.send();
			submissionMap.put("evaluation", evaluationMap);
		}

		List<Map<String, Object>> answerList = new ArrayList<Map<String, Object>>();
		submissionMap.put("answers", answerList);
		for (Answer a : getAnswers())
		{
			answerList.add(a.send());
		}

		return submissionMap;
	}

	/**
	 * Mark the assessment as having no changes.
	 */
	protected void clearChanged()
	{
		this.changed = false;
	}

	protected void initAnswers(List<Answer> answers)
	{
		this.answers = answers;
	}

	protected void initAssessment(Assessment assessment)
	{
		this.assessment = assessment;
	}

	protected void initFinished(Date date)
	{
		this.finished = date;
	}

	protected void initId(Long id)
	{
		this.id = id;
	}

	protected void initStarted(Date date)
	{
		this.started = date;
	}

	protected void initUser(User user)
	{
		this.user = user;
	}

	protected boolean isChanged()
	{
		return this.changed;
	}

	/**
	 * @return true if loaded, false if not.
	 */
	protected boolean isLoaded()
	{
		return this.loaded;
	}

	/**
	 * If not fully loaded, load.
	 */
	protected void load()
	{
		if (this.loaded) return;

		assessmentService().submissionRefresh(this);
	}

	/**
	 * Load the answers if needed.
	 */
	protected void loadAnswers()
	{
		if (this.answersLoaded) return;

		assessmentService().submissionRefreshAnswers(this);
		this.answersLoaded = true;
	}

	/**
	 * Set that the full assessment information has been loaded.
	 */
	protected void setLoaded()
	{
		this.loaded = true;
	}

	/**
	 * @return The registered AssessmentService.
	 */
	private AssessmentService assessmentService()
	{
		return (AssessmentService) Services.get(AssessmentService.class);
	}

	/**
	 * @return The registered CdpService.
	 */
	private CdpService cdpService()
	{
		return (CdpService) Services.get(CdpService.class);
	}
}

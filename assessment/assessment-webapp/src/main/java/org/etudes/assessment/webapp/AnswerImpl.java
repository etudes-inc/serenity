/**********************************************************************************
 * $URL: https://source.etudes.org/svn/serenity/trunk/assessment/assessment-webapp/src/main/java/org/etudes/assessment/webapp/AnswerImpl.java $
 * $Id: AnswerImpl.java 11578 2015-09-07 00:43:58Z ggolden $
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
import static org.etudes.util.StringUtil.trimToNull;

import java.util.Date;
import java.util.HashMap;
import java.util.HashSet;
import java.util.Map;
import java.util.Set;

import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;
import org.etudes.assessment.api.Answer;
import org.etudes.assessment.api.Question;
import org.etudes.assessment.api.Submission;
import org.etudes.cdp.api.CdpService;
import org.etudes.file.api.FileService;
import org.etudes.file.api.Reference;
import org.etudes.roster.api.Role;
import org.etudes.service.api.Services;
import org.etudes.tool.api.Tool;
import org.etudes.tool.api.ToolItemReference;
import org.etudes.user.api.User;

/**
 * AnswerImpl implements Answer.
 */
public class AnswerImpl implements Answer
{
	/** Our log. */
	private static Log M_log = LogFactory.getLog(AnswerImpl.class);

	protected Boolean answered = Boolean.FALSE;
	protected Date answeredOn = null;
	protected boolean changed = false;
	protected Long contentReferenceId = null;
	protected String data = null;
	protected Long id = null;
	protected String newContent = null;
	protected Question question = null;
	protected boolean removeContent = false;
	protected Boolean reviewMark = Boolean.FALSE;
	protected Submission submission = null;

	public AnswerImpl()
	{

	}

	public AnswerImpl(Long id)
	{
		this.id = id;
	}

	@Override
	public boolean equals(Object obj)
	{
		if (this == obj) return true;
		if (obj == null) return false;
		if (!(obj instanceof AnswerImpl)) return false;
		AnswerImpl other = (AnswerImpl) obj;
		if (different(id, other.id)) return false;
		return true;
	}

	@Override
	public Date getAnsweredOn()
	{
		return this.answeredOn;
	}

	@Override
	public String getContent(boolean forDownload)
	{
		Reference ref = getContentReference();
		if (ref != null)
		{
			String content = ref.getFile().readString();
			if (forDownload) content = fileService().processContentPlaceholderToDownload(content, getReference());

			return content;
		}

		return null;
	}

	@Override
	public String getData()
	{
		return this.data;
	}

	@Override
	public Long getId()
	{
		return this.id;
	}

	@Override
	public Question getQuestion()
	{
		return this.question;
	}

	@Override
	public ToolItemReference getReference()
	{
		return new ToolItemReference(getSubmission().getAssessment().getSite(), Tool.assessmentAnswer, getId());
	}

	@Override
	public Submission getSubmission()
	{
		return this.submission;
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
	public Boolean isAnswered()
	{
		return this.answered;
	}

	@Override
	public Boolean isMarkedForReview()
	{
		return this.reviewMark;
	}

	@Override
	public void read(String prefix, Map<String, Object> parameters)
	{
		setMarkedForReview(cdpService().readBoolean(parameters.get(prefix + "review")));
		setContent(cdpService().readString(parameters.get(prefix + "content")), true);
		setData(cdpService().readString(parameters.get(prefix + "data")));
	}

	@Override
	public Map<String, Object> send()
	{
		Map<String, Object> rv = new HashMap<String, Object>();

		rv.put("id", getId());
		rv.put("question", getQuestion().getId());
		rv.put("answered", isAnswered());
		if (getAnsweredOn() != null) rv.put("answeredOn", cdpService().sendDate(getAnsweredOn()));
		rv.put("content", getContent(true));
		rv.put("data", getData());
		rv.put("review", isMarkedForReview());

		return rv;
	}

	@Override
	public void setContent(String content, boolean downloadReferenceFormat)
	{
		content = trimToNull(content);

		// make sure content ends up with embedded reference URLs in placeholder format
		if (downloadReferenceFormat) content = fileService().processContentDownloadToPlaceholder(content);

		if (different(content, this.getContent(false)))
		{
			this.newContent = content;
			this.removeContent = (content == null);
			this.changed = true;
		}
	}

	@Override
	public void setData(String data)
	{
		if (different(data, this.data))
		{
			this.changed = true;
			this.data = data;
		}
	}

	@Override
	public void setMarkedForReview(Boolean setting)
	{
		if (setting == null) setting = Boolean.FALSE;

		if (different(setting, this.reviewMark))
		{
			this.changed = true;
			this.reviewMark = setting;
		}
	}

	/**
	 * Mark the assessment as having no changes.
	 */
	protected void clearChanged()
	{
		this.changed = false;
	}

	protected Reference getContentReference()
	{
		return fileService().getReference(this.contentReferenceId);
	}

	protected Long getContentReferenceId()
	{
		return this.contentReferenceId;
	}

	protected void initAnswered(Boolean a)
	{
		if (a == null) a = Boolean.FALSE;
		this.answered = a;
	}

	protected void initAnsweredOn(Date date)
	{
		this.answeredOn = date;
	}

	protected void initContentReferenceId(Long id)
	{
		this.contentReferenceId = id;
	}

	protected void initData(String data)
	{
		this.data = data;
	}

	protected void initId(Long id)
	{
		this.id = id;
	}

	protected void initMarkedForReview(Boolean setting)
	{
		if (setting == null) setting = Boolean.FALSE;
		this.reviewMark = setting;
	}

	protected void initQuestion(Question q)
	{
		this.question = q;
	}

	protected void initSubmission(Submission s)
	{
		this.submission = s;
	}

	protected boolean isChanged()
	{
		return this.changed;
	}

	protected void saveContent(User savedBy)
	{
		// the references we need to keep
		Set<Reference> keepers = new HashSet<Reference>();

		// update the content
		this.contentReferenceId = fileService().savePrivateFile(this.removeContent, this.newContent, "asmtAnswer.html", "text/html", null,
				this.contentReferenceId, getReference(), Role.guest, keepers);

		// update our references to be just the keepers
		fileService().removeExcept(getReference(), keepers);

		this.newContent = null;
		this.removeContent = false;
	}

	/**
	 * @return The registered CdpService.
	 */
	private CdpService cdpService()
	{
		return (CdpService) Services.get(CdpService.class);
	}

	/**
	 * @return The registered FileService.
	 */
	private FileService fileService()
	{
		return (FileService) Services.get(FileService.class);
	}
}

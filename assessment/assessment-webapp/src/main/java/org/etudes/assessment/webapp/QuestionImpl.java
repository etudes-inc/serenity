/**********************************************************************************
 * $URL: https://source.etudes.org/svn/serenity/trunk/assessment/assessment-webapp/src/main/java/org/etudes/assessment/webapp/QuestionImpl.java $
 * $Id: QuestionImpl.java 11561 2015-09-06 00:45:58Z ggolden $
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

import java.util.HashMap;
import java.util.Map;

import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;
import org.etudes.assessment.api.AssessmentService;
import org.etudes.assessment.api.Question;
import org.etudes.cdp.api.CdpService;
import org.etudes.evaluation.api.EvaluationService;
import org.etudes.file.api.FileService;
import org.etudes.service.api.Services;
import org.etudes.site.api.Site;
import org.etudes.tool.api.Tool;
import org.etudes.tool.api.ToolItemReference;

/**
 * QuestionImpl implements Question.
 */
public class QuestionImpl implements Question
{
	/** Our log. */
	private static Log M_log = LogFactory.getLog(QuestionImpl.class);

	protected boolean changed = false;
	protected Long id = null;
	protected boolean loaded = false;
	protected Site site = null;

	/**
	 * Construct with just an id.
	 * 
	 * @param id
	 *        The id.
	 */
	public QuestionImpl(Long id)
	{
		initId(id);
	}

	@Override
	public boolean equals(Object obj)
	{
		if (this == obj) return true;
		if (obj == null) return false;
		if (!(obj instanceof QuestionImpl)) return false;
		QuestionImpl other = (QuestionImpl) obj;
		if (different(id, other.id)) return false;
		return true;
	}

	@Override
	public Long getId()
	{
		return this.id;
	}

	@Override
	public ToolItemReference getReference()
	{
		return new ToolItemReference(getSite(), Tool.assessmentQuestion, getId());
	}

	@Override
	public Site getSite()
	{
		load();
		return this.site;
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
	public void read(String prefix, Map<String, Object> parameters)
	{
	}

	@Override
	public Map<String, Object> send()
	{
		Map<String, Object> rv = new HashMap<String, Object>();

		rv.put("id", getId());

		return rv;
	}

	/**
	 * Mark the assessment as having no changes.
	 */
	protected void clearChanged()
	{
		this.changed = false;
	}

	protected void initId(Long id)
	{
		this.id = id;
	}

	protected void initSite(Site site)
	{
		this.site = site;
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

		// assessmentService().assessmentRefresh(this);
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

	/**
	 * @return The registered EvaluationService.
	 */
	private EvaluationService evaluationService()
	{
		return (EvaluationService) Services.get(EvaluationService.class);
	}

	/**
	 * @return The registered FileService.
	 */
	private FileService fileService()
	{
		return (FileService) Services.get(FileService.class);
	}
}

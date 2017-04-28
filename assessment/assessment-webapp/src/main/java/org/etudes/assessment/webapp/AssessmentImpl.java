/**********************************************************************************
 * $URL: https://source.etudes.org/svn/serenity/trunk/assessment/assessment-webapp/src/main/java/org/etudes/assessment/webapp/AssessmentImpl.java $
 * $Id: AssessmentImpl.java 12104 2015-11-21 01:19:23Z ggolden $
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
import org.etudes.assessment.api.Assessment;
import org.etudes.assessment.api.AssessmentService;
import org.etudes.cdp.api.CdpService;
import org.etudes.entity.api.Schedule;
import org.etudes.evaluation.api.EvaluationDesign;
import org.etudes.evaluation.api.EvaluationService;
import org.etudes.file.api.File;
import org.etudes.file.api.FileService;
import org.etudes.file.api.Reference;
import org.etudes.roster.api.Role;
import org.etudes.service.api.Services;
import org.etudes.site.api.Site;
import org.etudes.tool.api.Tool;
import org.etudes.tool.api.ToolItemReference;
import org.etudes.tool.api.ToolItemType;
import org.etudes.user.api.User;

/**
 * AssessmentImpl implements Assessment.
 */
public class AssessmentImpl implements Assessment
{
	/** Our log. */
	private static Log M_log = LogFactory.getLog(AssessmentImpl.class);

	protected boolean changed = false;
	protected User createdBy = null;
	protected Date createdOn = null;
	protected EvaluationDesign evaluation = null;
	protected Long id = null;
	protected Long instructionsReferenceId = null;
	protected boolean loaded = false;
	protected User modifiedBy = null;
	protected Date modifiedOn = null;
	protected String newInstructions = null;
	protected Boolean published = Boolean.FALSE;
	protected boolean removeInstructions = false;
	protected Schedule schedule = new Schedule();
	protected File sharedInstructions = null;
	protected Site site = null;

	protected String title = null;
	protected Integer tries = null;
	protected Type type = Type.assignment;

	/**
	 * Construct empty.
	 */
	public AssessmentImpl()
	{
	}

	/**
	 * Construct as a copy of another.
	 * 
	 * @param other
	 *        The other.
	 */
	public AssessmentImpl(AssessmentImpl other)
	{
		init(other);
	}

	/**
	 * Construct with just an id.
	 * 
	 * @param id
	 *        The id.
	 */
	public AssessmentImpl(Long id)
	{
		initId(id);
	}

	@Override
	public boolean equals(Object obj)
	{
		if (this == obj) return true;
		if (obj == null) return false;
		if (!(obj instanceof AssessmentImpl)) return false;
		AssessmentImpl other = (AssessmentImpl) obj;
		if (different(id, other.id)) return false;
		return true;
	}

	@Override
	public User getCreatedBy()
	{
		load();
		return this.createdBy;
	}

	@Override
	public Date getCreatedOn()
	{
		load();
		return this.createdOn;
	}

	@Override
	public EvaluationDesign getEvaluation()
	{
		load();
		return this.evaluation;
	}

	@Override
	public Long getId()
	{
		return this.id;
	}

	@Override
	public String getInstructions(boolean forDownload)
	{
		Reference ref = getInstructionsReference();
		if (ref != null)
		{
			String content = ref.getFile().readString();
			if (forDownload) content = fileService().processContentPlaceholderToDownload(content, getReference());

			return content;
		}

		return null;
	}

	@Override
	public User getModifiedBy()
	{
		load();
		return this.modifiedBy;
	}

	@Override
	public Date getModifiedOn()
	{
		load();
		return this.modifiedOn;
	};

	@Override
	public Boolean getPublished()
	{
		load();
		return this.published;
	}

	@Override
	public ToolItemReference getReference()
	{
		return new ToolItemReference(getSite(), Tool.assessment, getId());
	}

	@Override
	public Set<User> getReferencedUsers()
	{
		Set<User> rv = new HashSet<User>();
		rv.add(getCreatedBy());
		rv.add(getModifiedBy());

		return rv;
	}

	@Override
	public Schedule getSchedule()
	{
		load();
		return this.schedule;
	}

	@Override
	public Site getSite()
	{
		load();
		return this.site;
	}

	@Override
	public String getTitle()
	{
		load();
		return this.title;
	}

	@Override
	public ToolItemType getToolItemType()
	{
		ToolItemType type = ToolItemType.assignment;
		if (getType() == Type.essay) type = ToolItemType.essay;
		if (getType() == Type.fce) type = ToolItemType.fce;
		if (getType() == Type.offline) type = ToolItemType.offline;
		if (getType() == Type.survey) type = ToolItemType.survey;
		if (getType() == Type.test) type = ToolItemType.test;

		return type;
	}

	@Override
	public Integer getTries()
	{
		load();
		return this.tries;
	}

	@Override
	public Type getType()
	{
		load();
		return this.type;
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
	public Boolean isTriesRemain(Integer count)
	{
		load();
		if (this.tries == null) return Boolean.TRUE;
		return Boolean.valueOf(count < this.tries.intValue());
	}

	@Override
	public void read(String prefix, Map<String, Object> parameters)
	{
		setTitle(cdpService().readString(parameters.get(prefix + "title")));
		setPublished(cdpService().readBoolean(parameters.get(prefix + "published")));
		setType(Type.fromCode(cdpService().readString(parameters.get(prefix + "type"))));
		setInstructions(cdpService().readString(parameters.get(prefix + "instructions")), true);
		getSchedule().read(prefix + "schedule_", parameters);
		getEvaluation().read(prefix + "design_", parameters);
	}

	@Override
	public Map<String, Object> send()
	{
		Map<String, Object> assessmentMap = new HashMap<String, Object>();

		assessmentMap.put("id", getId());
		assessmentMap.put("createdBy", getCreatedBy() == null ? "SYSTEM" : getCreatedBy().getNameDisplay());
		if (getCreatedOn() != null) assessmentMap.put("createdOn", cdpService().sendDate(getCreatedOn()));
		assessmentMap.put("modifiedBy", getModifiedBy() == null ? "SYSTEM" : getModifiedBy().getNameDisplay());
		if (getModifiedOn() != null) assessmentMap.put("modifiedOn", cdpService().sendDate(getModifiedOn()));

		assessmentMap.put("type", getType().getCode());
		assessmentMap.put("title", getTitle());
		assessmentMap.put("published", getPublished());
		assessmentMap.put("instructions", getInstructions(true));

		assessmentMap.put("valid", Boolean.TRUE); // TODO: validity

		assessmentMap.put("schedule", getSchedule().send());
		assessmentMap.put("design", getEvaluation().send());

		// TODO:
		// assessmentMap.put("finalMsg","<p>this is the final message</p>");
		assessmentMap.put("shuffle", Boolean.FALSE);
		assessmentMap.put("hints", Boolean.FALSE);
		assessmentMap.put("layout", Integer.valueOf(0));
		assessmentMap.put("numbering", Integer.valueOf(0));
		// assessmentMap.put("password", "");
		assessmentMap.put("pledge", Boolean.FALSE);
		assessmentMap.put("tries", Integer.valueOf(0));
		// assessmentMap.put("numTries", Integer.valueOf(1));
		assessmentMap.put("limit", Integer.valueOf(0));
		// assessmentMap.put("timeLimit", "00:30");
		assessmentMap.put("reviewWhen", Integer.valueOf(0));
		// assessmentMap.put("reviewAfter", some date);
		assessmentMap.put("showSummary", Boolean.TRUE);
		assessmentMap.put("reviewIncludes", Integer.valueOf(0));
		assessmentMap.put("feedback", Boolean.TRUE);
		assessmentMap.put("model", Boolean.TRUE);
		assessmentMap.put("award", Boolean.TRUE);
		assessmentMap.put("awardPct", Integer.valueOf(90));
		assessmentMap.put("resultsEmail", "ggolden@etudes.org, ggolden22@mac.com");

		return assessmentMap;
	}

	@Override
	public void setInstructions(String content, boolean downloadReferenceFormat)
	{
		load();

		content = trimToNull(content);

		// make sure content ends up with embedded reference URLs in placeholder format
		if ((content != null) && (downloadReferenceFormat)) content = fileService().processContentDownloadToPlaceholder(content);

		if (different(content, this.getInstructions(false)))
		{
			this.newInstructions = content;
			this.removeInstructions = (content == null);
			this.sharedInstructions = null;
			this.changed = true;
		}
	}

	@Override
	public void setPublished(Boolean published)
	{
		if (published == null) published = Boolean.FALSE;
		load();
		// if (published == null) return;

		if (different(published, this.published))
		{
			this.changed = true;
			this.published = published;
		}
	}

	@Override
	public void setTitle(String title)
	{
		load();
		if (different(title, this.title))
		{
			this.title = title;
			this.changed = true;
		}
	}

	@Override
	public void setTries(Integer tries)
	{
		load();
		if (different(tries, this.tries))
		{
			this.tries = tries;
			this.changed = true;
		}
	}

	@Override
	public void setType(Type type)
	{
		if (type == null) return;

		load();
		if (different(type, this.type))
		{
			this.type = type;
			this.changed = true;
		}
	}

	@Override
	public void shareInstructions(File file)
	{
		load();

		if (different(file, this.sharedInstructions))
		{
			this.sharedInstructions = file;
			this.newInstructions = null;
			this.removeInstructions = false;
			this.changed = true;
		}
	}

	/**
	 * Mark the assessment as having no changes.
	 */
	protected void clearChanged()
	{
		this.changed = false;
		this.evaluation.clearChanged();
		this.schedule.clearChanged();
	}

	protected Reference getInstructionsReference()
	{
		return fileService().getReference(this.instructionsReferenceId);
	}

	protected Long getInstructionsReferenceId()
	{
		return this.instructionsReferenceId;
	}

	/**
	 * Init with values from another.
	 * 
	 * @param other
	 *        The other.
	 */
	protected void init(AssessmentImpl other)
	{
		this.createdBy = other.createdBy;
		this.createdOn = other.createdOn;
		this.id = other.id;
		this.modifiedBy = other.modifiedBy;
		this.modifiedOn = other.modifiedOn;
		this.title = other.title;
		this.published = other.published;
		this.site = other.site;
		this.evaluation = evaluationService().designClone(other.evaluation);
		this.schedule = new Schedule(other.schedule);
		this.changed = false;
		this.loaded = true;
	}

	protected void initCreatedBy(User user)
	{
		this.createdBy = user;
	}

	protected void initCreatedOn(Date createdOn)
	{
		this.createdOn = createdOn;
	}

	protected void initEvaluation(EvaluationDesign evaluation)
	{
		this.evaluation = evaluation;
	}

	protected void initId(Long id)
	{
		this.id = id;
	}

	protected void initInstructionsReferenceId(Long id)
	{
		this.instructionsReferenceId = id;
	}

	protected void initModifiedBy(User user)
	{
		this.modifiedBy = user;
	}

	protected void initModifiedOn(Date modifiedOn)
	{
		this.modifiedOn = modifiedOn;
	}

	protected void initPublished(Boolean published)
	{
		if (published == null) published = Boolean.FALSE;
		this.published = published;
	}

	protected void initSchedule(Schedule schedule)
	{
		this.schedule = schedule;
	}

	protected void initSite(Site site)
	{
		this.site = site;
	}

	protected void initTitle(String title)
	{
		this.title = title;
	}

	protected void initTries(Integer tries)
	{
		this.tries = tries;
	}

	protected void initType(Type type)
	{
		this.type = type;
	}

	protected boolean isChanged()
	{
		if ((this.evaluation != null) && this.evaluation.isChanged()) return true;
		if (this.schedule.isChanged()) return true;

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

		assessmentService().assessmentRefresh(this);
	}

	protected void saveInstructions(User savedBy)
	{
		// the references we need to keep
		Set<Reference> keepers = new HashSet<Reference>();

		// update the content
		this.instructionsReferenceId = fileService().savePrivateFile(this.removeInstructions, this.newInstructions, "asmtInstructions.html",
				"text/html", this.sharedInstructions, this.instructionsReferenceId, getReference(), Role.guest, keepers);

		// update our references to be just the keepers
		fileService().removeExcept(getReference(), keepers);

		this.newInstructions = null;
		this.removeInstructions = false;
		this.sharedInstructions = null;
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

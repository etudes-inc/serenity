/**********************************************************************************
 * $URL: https://source.etudes.org/svn/serenity/trunk/core/core-webapp/src/main/java/org/etudes/evaluation/webapp/EvaluationImpl.java $
 * $Id: EvaluationImpl.java 11561 2015-09-06 00:45:58Z ggolden $
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

package org.etudes.evaluation.webapp;

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
import org.etudes.cdp.api.CdpService;
import org.etudes.evaluation.api.Criterion;
import org.etudes.evaluation.api.Evaluation;
import org.etudes.evaluation.api.EvaluationService;
import org.etudes.file.api.File;
import org.etudes.file.api.FileService;
import org.etudes.file.api.Reference;
import org.etudes.roster.api.Role;
import org.etudes.service.api.Services;
import org.etudes.tool.api.Tool;
import org.etudes.tool.api.ToolItemReference;
import org.etudes.tool.api.ToolItemWorkReference;
import org.etudes.user.api.User;

class EvaluationImpl implements Evaluation
{
	/** Our log. */
	private static Log M_log = LogFactory.getLog(EvaluationImpl.class);

	protected boolean changed = false;
	protected Long commentReferenceId = null;

	protected boolean commentRemoved = false;

	protected User createdBy = null;

	protected Date createdOn = null;

	protected Boolean evaluated = Boolean.FALSE;

	protected Long id = null;

	protected User modifiedBy = null;

	// boolean loaded = false;

	protected Date modifiedOn = null;

	protected String newComment = null;

	protected Map<Criterion, Integer> origRatings = new HashMap<Criterion, Integer>();

	protected Map<Criterion, Integer> ratings = new HashMap<Criterion, Integer>();

	protected Boolean released = Boolean.FALSE;

	protected boolean reviewedChanged = false;

	protected Date reviewedOn = null;

	protected Float score = null;

	protected File sharedComment = null;

	protected Type type = Type.unknown;

	protected ToolItemWorkReference workReference = null;

	@Override
	public boolean equals(Object obj)
	{
		if (this == obj) return true;
		if (obj == null) return false;
		if (!(obj instanceof EvaluationImpl)) return false;
		EvaluationImpl other = (EvaluationImpl) obj;
		if (different(id, other.id)) return false;
		return true;
	}

	@Override
	public Float getActualScore()
	{
		return this.score;
	}

	@Override
	public String getComment(boolean forDownload)
	{
		Reference ref = getCommentReference();
		if (ref != null)
		{
			String content = ref.getFile().readString();
			if (forDownload) content = fileService().processContentPlaceholderToDownload(content, getReference());

			return content;
		}

		return null;
	}

	@Override
	public User getCreatedBy()
	{
		return this.createdBy;
	}

	@Override
	public Date getCreatedOn()
	{
		return this.createdOn;
	}

	@Override
	public Boolean getEvaluated()
	{
		return this.evaluated;
	}

	@Override
	public Long getId()
	{
		return this.id;
	}

	@Override
	public User getModifiedBy()
	{
		return this.modifiedBy;
	}

	@Override
	public Date getModifiedOn()
	{
		return this.modifiedOn;
	}

	@Override
	public Boolean getNeedsReview()
	{
		// if not released, no need for review
		if (!this.released) return Boolean.FALSE;

		// if reviewed after evaluated, no need for review
		if ((this.reviewedOn != null) && (this.modifiedOn != null) && (this.reviewedOn.after(this.modifiedOn))) return Boolean.FALSE;

		// is this even possible? TODO:
		if (this.modifiedOn == null) return Boolean.FALSE;

		// otherwise, needs review
		return Boolean.TRUE;
	}

	@Override
	public Map<Criterion, Integer> getRatings()
	{
		return this.ratings;
	}

	@Override
	public ToolItemReference getReference()
	{
		return new ToolItemReference(getWorkReference().getItem().getSite(), Tool.evaluation, this.id);
	}

	@Override
	public Boolean getReleased()
	{
		return this.released;
	}

	@Override
	public Date getReviewedOn()
	{
		return this.reviewedOn;
	}

	@Override
	public Float getScore()
	{
		if (this.score == null) return Float.valueOf(0);
		return this.score;
	}

	@Override
	public Type getType()
	{
		return this.type;
	}

	@Override
	public ToolItemWorkReference getWorkReference()
	{
		return workReference;
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
		setScore(cdpService().readFloat(parameters.get(prefix + "score")));
		setEvaluated(cdpService().readBoolean(parameters.get(prefix + "evaluated")));
		setReleased(cdpService().readBoolean(parameters.get(prefix + "released")));
		setComment(cdpService().readString(parameters.get(prefix + "comment")), true);

		// TODO: clear the existing ratings?
		List<String> cids = cdpService().readStrings(parameters.get(prefix + "ratings_"));
		if (cids != null)
		{
			for (String id : cids)
			{
				Integer value = cdpService().readInt(parameters.get(prefix + "ratings_" + id));
				if (value != null)
				{
					try
					{
						setRating(evaluationService().criterionWrap(Long.valueOf(id)), value);
					}
					catch (NumberFormatException e)
					{
						M_log.warn("read: non numberic criterion id: " + id);
					}
				}
			}
		}
	}

	@Override
	public Map<String, Object> send()
	{
		Map<String, Object> rv = new HashMap<String, Object>();
		rv.put("id", getId());
		rv.put("type", getType().getId());
		rv.put("score", getScore());
		rv.put("evaluated", getEvaluated());
		rv.put("released", getReleased());
		rv.put("comment", getComment(true));

		rv.put("submittedOn", getWorkReference().getSubmittedOn());
		if (getReviewedOn() != null) rv.put("reviewedOn", cdpService().sendDate(getReviewedOn()));

		rv.put("needsReview", getNeedsReview());

		Map<String, Object> ratingsMap = new HashMap<String, Object>();
		rv.put("ratings", ratingsMap);
		for (Criterion c : getRatings().keySet())
		{
			ratingsMap.put(c.getId().toString(), getRatings().get(c));
		}

		rv.put("createdBy", getCreatedBy() == null ? "SYSTEM" : getCreatedBy().getNameDisplay());
		if (getCreatedOn() != null) rv.put("createdOn", cdpService().sendDate(getCreatedOn()));
		rv.put("modifiedBy", getModifiedBy() == null ? "SYSTEM" : getModifiedBy().getNameDisplay());
		if (getModifiedOn() != null) rv.put("modifiedOn", cdpService().sendDate(getModifiedOn()));

		return rv;
	}

	@Override
	public void setComment(String comment, boolean downloadReferenceFormat)
	{
		// make sure signature ends up with embedded reference URLs in placeholder format
		if (downloadReferenceFormat) comment = fileService().processContentDownloadToPlaceholder(comment);

		if (different(comment, this.getComment(false)))
		{
			this.newComment = comment;
			if (comment == null) commentRemoved = true;
			this.sharedComment = null;
			this.changed = true;
		}
	}

	@Override
	public void setEvaluated(Boolean value)
	{
		if (different(this.evaluated, value))
		{
			this.evaluated = value;
			this.changed = true;
		}
	}

	@Override
	public void setRating(Criterion criterion, Integer rating)
	{
		Integer current = this.ratings.get(criterion);
		if (different(current, rating))
		{
			this.ratings.put(criterion, rating);
			this.changed = true;
		}
	}

	@Override
	public void setReleased(Boolean value)
	{
		if (different(this.released, value))
		{
			this.released = value;
			this.changed = true;
		}
	}

	@Override
	public void setReviewed()
	{
		// will not change the modified on / by attribution
		this.reviewedOn = new Date();
		this.reviewedChanged = true;
	}

	@Override
	public void setScore(Float score)
	{
		if (different(score, this.score))
		{
			this.score = score;
			changed = true;
		}
	}

	@Override
	public void shareComment(File file)
	{
		if (different(file, this.sharedComment))
		{
			this.sharedComment = file;
			this.newComment = null;
			this.commentRemoved = false;
			this.changed = true;
		}
	}

	/**
	 * Mark as having no changes.
	 */
	protected void clearChanged()
	{
		this.changed = false;
		this.reviewedChanged = false;

		this.origRatings.clear();
		this.origRatings.putAll(this.ratings);
	}

	/**
	 * @return A List of Criterion found in the ratings that are not in the origRatings
	 */
	protected List<Criterion> criteriaToAdd()
	{
		List<Criterion> rv = new ArrayList<Criterion>();

		for (Criterion c : this.ratings.keySet())
		{
			if (this.origRatings.get(c) == null) rv.add(c);
		}

		return rv;
	}

	/**
	 * @return A List of Criterion found in the origRatings that are no longer in ratings
	 */
	protected List<Criterion> criteriaToRemove()
	{
		List<Criterion> rv = new ArrayList<Criterion>();

		for (Criterion c : this.origRatings.keySet())
		{
			if (this.ratings.get(c) == null) rv.add(c);
		}

		return rv;
	}

	/**
	 * @return A List of Criterion found in both the ratings and origRatings that have changed
	 */
	protected List<Criterion> criteriaToUpdate()
	{
		List<Criterion> rv = new ArrayList<Criterion>();

		for (Criterion c : this.ratings.keySet())
		{
			Integer origValue = this.origRatings.get(c);
			if ((origValue != null) && (different(origValue, this.ratings.get(c)))) rv.add(c);
		}

		return rv;
	}

	protected Reference getCommentReference()
	{
		return fileService().getReference(this.commentReferenceId);
	}

	protected Long getCommentReferenceId()
	{
		return this.commentReferenceId;
	}

	protected void initCommentReferenceId(Long id)
	{
		this.commentReferenceId = id;
	}

	protected void initCreatedBy(User user)
	{
		this.createdBy = user;
	}

	protected void initCreatedOn(Date date)
	{
		this.createdOn = date;
	}

	protected void initEvaluated(Boolean setting)
	{
		this.evaluated = setting;
	}

	protected void initId(Long id)
	{
		this.id = id;
	}

	protected void initModifiedBy(User user)
	{
		this.modifiedBy = user;
	}

	protected void initModifiedOn(Date date)
	{
		this.modifiedOn = date;
	}

	protected void initRating(Criterion c, Integer r)
	{
		this.ratings.put(c, r);
		this.origRatings.put(c, r);
	}

	protected void initRatings(Map<Criterion, Integer> ratings)
	{
		this.ratings.clear();
		this.ratings.putAll(ratings);
		this.origRatings.clear();
		this.origRatings.putAll(this.ratings);
	}

	protected void initReleased(Boolean setting)
	{
		this.released = setting;
	}

	protected void initReviewedOn(Date date)
	{
		this.reviewedOn = date;
	}

	protected void initScore(Float score)
	{
		this.score = score;
	}

	protected void initType(Type type)
	{
		this.type = type;
	}

	protected void initWorkReference(ToolItemWorkReference ref)
	{
		this.workReference = ref;
	}

	/**
	 * Check if there are any changes.
	 * 
	 * @return true if changed, false if not.
	 */
	protected boolean isChanged()
	{
		return this.changed;
	}

	/**
	 * Check if reviewed has changed.
	 * 
	 * @return true if reviewed has changed, false if not.
	 */
	protected boolean isReviewedChanged()
	{
		return this.reviewedChanged;
	}

	/**
	 * Save the updated comment.
	 */
	protected void saveComment()
	{
		// the references we need to keep
		Set<Reference> keepers = new HashSet<Reference>();

		this.commentReferenceId = fileService().savePrivateFile(this.commentRemoved, this.newComment, "comment.html", "text/html",
				this.sharedComment, this.commentReferenceId, getReference(), Role.custom, keepers);

		// update our references to be just the keepers
		fileService().removeExcept(getReference(), keepers);

		this.newComment = null;
		this.commentRemoved = false;
		this.sharedComment = null;
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

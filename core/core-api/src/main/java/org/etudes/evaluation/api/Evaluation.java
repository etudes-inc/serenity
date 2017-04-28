/**********************************************************************************
 * $URL: https://source.etudes.org/svn/serenity/trunk/core/core-api/src/main/java/org/etudes/evaluation/api/Evaluation.java $
 * $Id: Evaluation.java 11561 2015-09-06 00:45:58Z ggolden $
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

package org.etudes.evaluation.api;

import java.util.Date;
import java.util.Map;

import org.etudes.file.api.File;
import org.etudes.tool.api.ToolItemReference;
import org.etudes.tool.api.ToolItemWorkReference;
import org.etudes.user.api.User;

public interface Evaluation
{
	enum Type
	{
		official(1), peer(2), unknown(0);

		public static Type fromCode(Integer id)
		{
			for (Type t : Type.values())
			{
				if (t.id.equals(id)) return t;
			}
			return unknown;
		}

		private final Integer id;

		private Type(int id)
		{
			this.id = Integer.valueOf(id);
		}

		public Integer getId()
		{
			return this.id;
		}
	}

	/**
	 * @return The score, may be null.
	 */
	Float getActualScore();

	/**
	 * Get the evaluation comment.
	 * 
	 * @param forDownload
	 *        if true, modify the content to convert embedded references from stored placeholder URLs to download URLs using references for this item.
	 * @return The evaluation comments.
	 */
	String getComment(boolean forDownload);

	/**
	 * @return the creating user.
	 */
	User getCreatedBy();

	/**
	 * @return the date created.
	 */
	Date getCreatedOn();

	/**
	 * @return TRUE if the evaluation is marked as evaluated, FALSE if not.
	 */
	Boolean getEvaluated();

	/**
	 * @return the evaluation id.
	 */
	Long getId();

	/**
	 * @return the last modifying user.
	 */
	User getModifiedBy();

	/**
	 * @return the date last modified.
	 */
	Date getModifiedOn();

	/**
	 * @return TRUE if the evaluation is newer than the last review.
	 */
	Boolean getNeedsReview();

	/**
	 * @return a Map by Criterion of the rating values for this evaluation.
	 */
	Map<Criterion, Integer> getRatings();

	/**
	 * @return The ToolItemReference to the evaluation.
	 */
	ToolItemReference getReference();

	/**
	 * @return TRUE if the evaluation has been released, FALSE if not.
	 */
	Boolean getReleased();

	/**
	 * @return The date the evaluation was last reviewed.
	 */
	Date getReviewedOn();

	/**
	 * @return The total point score of the evaluation. Returns 0 if not yet scored (never null).
	 */
	Float getScore();

	/**
	 * @return The evaluation type.
	 */
	Type getType();

	/**
	 * @return A reference to the work and item being evaluated.
	 */
	ToolItemWorkReference getWorkReference();

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
	 * Format evaluation for sending via CDP.
	 * 
	 * @return The map, ready to add as an "evaluation" element to the return map.
	 */
	Map<String, Object> send();

	/**
	 * Set the evaluation comment.
	 * 
	 * @param comment
	 *        The new comment, or null to remove.
	 * @param downloadReferenceFormat
	 *        if true, the comment has embedded references in download (not placeholder) format.
	 */
	void setComment(String comment, boolean downloadReferenceFormat);

	/**
	 * Set the evaluated status.
	 * 
	 * @param value
	 *        The new evaluated status.
	 */
	void setEvaluated(Boolean value);

	/**
	 * Set the rating for this criterion to this value.
	 * 
	 * @param criterion
	 *        The criterion.
	 * @param rating
	 *        The value.
	 */
	void setRating(Criterion criterion, Integer rating);

	/**
	 * Set the released status.
	 * 
	 * @param value
	 *        The new release status.
	 */
	void setReleased(Boolean value);

	/**
	 * Mark the evaluation as having been reviewed.
	 */
	void setReviewed();

	/**
	 * Set the evaluation score.
	 * 
	 * @param score
	 *        The total point score, or null to remove.
	 */
	void setScore(Float score);

	/**
	 * Set the evaluation comment to share an existing file system file.
	 * 
	 * @param file
	 *        The file to use.
	 */
	void shareComment(File file);
}

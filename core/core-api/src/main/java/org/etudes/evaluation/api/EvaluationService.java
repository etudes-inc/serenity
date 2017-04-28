/**********************************************************************************
 * $URL: https://source.etudes.org/svn/serenity/trunk/core/core-api/src/main/java/org/etudes/evaluation/api/EvaluationService.java $
 * $Id: EvaluationService.java 11587 2015-09-10 03:14:52Z ggolden $
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

import java.util.List;

import org.etudes.site.api.Site;
import org.etudes.tool.api.ToolItemReference;
import org.etudes.tool.api.ToolItemType;
import org.etudes.tool.api.ToolItemWorkReference;
import org.etudes.user.api.User;

public interface EvaluationService
{
	/**
	 * Create a new category.
	 * 
	 * @param createdBy
	 *        The user creating the category.
	 * @param site
	 *        The site in which the category will live.
	 * @return The category.
	 */
	Category categoryAdd(User createdBy, Site site);

	/**
	 * Get all the categories defined for a site.
	 *
	 * @param site
	 *        The site.
	 * @return The List of categories, may be empty.
	 */
	List<Category> categoryFindBySite(Site site);

	/**
	 * Find the category with this type.
	 * 
	 * @param categories
	 *        The list of categories.
	 * @param type
	 *        The type.
	 * @return
	 */
	Category categoryFindByType(List<Category> categories, ToolItemType type);

	/**
	 * Get the category with this id.
	 * 
	 * @param id
	 *        The id.
	 * @return The category with this id, or null if not found.
	 */
	Category categoryGet(Long id);

	/**
	 * Refresh this Category object with a full data load from the database, overwriting any values, setting it to unchanged.
	 * 
	 * @param category
	 *        The category.
	 */
	void categoryRefresh(Category category);

	/**
	 * Remove this category
	 * 
	 * @param category
	 *        The category to remove.
	 */
	void categoryRemove(Category category);

	/**
	 * Save any changes to this category.
	 * 
	 * @param savedBy
	 *        The user making the save.
	 * @param category
	 *        The category.
	 */
	void categorySave(User savedBy, Category category);

	/**
	 * Wrap an id into an unloaded category.
	 * 
	 * @param id
	 *        The id.
	 * @return The category.
	 */
	Category categoryWrap(Long id);

	/**
	 * Refresh this Criterion object with a full data load from the database, overwriting any values, setting it to unchanged.
	 * 
	 * @param criterion
	 *        The criterion.
	 */
	void criterionRefresh(Criterion criterion);

	/**
	 * Wrap an id into a Criterion object. The id is not checked.
	 * 
	 * @param id
	 *        The criterion id.
	 * @return The wrapped criterion.
	 */
	Criterion criterionWrap(Long id);

	/**
	 * Create a new evaluation design for this item.
	 * 
	 * @param ref
	 *        The item reference.
	 * @return The Evaluation design.
	 */
	EvaluationDesign designAdd(ToolItemReference ref);

	/**
	 * Clone the design into a new object.
	 * 
	 * @param design
	 *        The design.
	 * @return A deep copy of the design.
	 */
	EvaluationDesign designClone(EvaluationDesign design);

	/**
	 * Read the evaluation design for this tool item. If not found, return a new, empty one.
	 * 
	 * @param ref
	 *        The tool item reference.
	 * @return The EvaluatinSpec for this item, or null if the item has none defined.
	 */
	EvaluationDesign designGet(ToolItemReference ref);

	/**
	 * Remove this evaluation design
	 * 
	 * @param design
	 *        The evaluation design to remove.
	 */
	void designRemove(EvaluationDesign design);

	/**
	 * Save any changes to this design.
	 * 
	 * @param design
	 *        The evaluation design.
	 */
	void designSave(EvaluationDesign design);

	/**
	 * Create a new evaluation of this type for this work.
	 * 
	 * @param createdBy
	 *        The user creating.
	 * @param ref
	 *        The work reference.
	 * @param type
	 *        The evaluation type.
	 * @return The Evaluation.
	 */
	Evaluation evaluationAdd(User createdBy, ToolItemWorkReference ref, Evaluation.Type type);

	/**
	 * Find the best released evaluation for the work by this user for this item (from a list of evaluations).
	 * 
	 * @param evaluations
	 *        The evaluations list.
	 * @param ref
	 *        The item reference.
	 * @param user
	 *        The user submitting the work being evaluated.
	 * @return The evaluation with the best score for this user / item, or null if there are no evaluations.
	 */
	Evaluation evaluationFindBestByItemUser(List<Evaluation> evaluations, ToolItemReference ref, User user);

	/**
	 * Find the best released evaluation for the work by this user for this item
	 * 
	 * @param ref
	 *        The item reference.
	 * @param user
	 *        The user submitting the work being evaluated.
	 * @param type
	 *        The evaluation type, or null for all.
	 * @return The evaluation with the best score for this user / item, or null if there are no evaluations.
	 */
	Evaluation evaluationFindBestByItemUser(ToolItemReference ref, User user, Evaluation.Type type);

	/**
	 * Find the evaluations of this type for this item (all work)
	 * 
	 * @param ref
	 *        The item reference.
	 * @param type
	 *        The evaluation type, or null for all.
	 * @return The List of Evaluation for this work, may be empty.
	 */
	List<Evaluation> evaluationFindByItem(ToolItemReference ref, Evaluation.Type type);

	/**
	 * Find the evaluations of this type for this user in this site (all items and work)
	 * 
	 * @param site
	 *        The site.
	 * @param type
	 *        The evaluation type, or null for all.
	 * @param user
	 *        The user. If null, find for all users.
	 * @return The List of Evaluation for this work, may be empty.
	 */
	List<Evaluation> evaluationFindBySite(Site site, Evaluation.Type type, User user);

	/**
	 * Find the evaluation for this work (from a list of evaluations).
	 * 
	 * @param evaluations
	 *        The evaluations list.
	 * @param ref
	 *        The work reference.
	 * @return The Evaluation for this work, or null if not found.
	 */
	List<Evaluation> evaluationFindByWork(List<Evaluation> evaluations, ToolItemWorkReference ref);

	/**
	 * Find the evaluations of this type for this work
	 * 
	 * @param ref
	 *        The work reference.
	 * @param type
	 *        The evaluation type, or null for all.
	 * @return The List of Evaluation for this work, may be empty.
	 */
	List<Evaluation> evaluationFindByWork(ToolItemWorkReference ref, Evaluation.Type type);

	/**
	 * Get this evaluation.
	 * 
	 * @param id
	 *        The evaluation id.
	 * @return the evaluation, or null if not found.
	 */
	Evaluation evaluationGet(Long id);

	/**
	 * Remove all evaluations for this tool item.
	 * 
	 * @param item
	 *        The tool item reference.
	 */
	void evaluationRemoveItem(ToolItemReference item);

	/**
	 * Save this evaluation.
	 * 
	 * @param savedBy
	 *        The user saving.
	 * @param evaluation
	 *        The evaluation.
	 */
	void evaluationSave(User savedBy, Evaluation evaluation);

	/**
	 * Get this grading item.
	 * 
	 * @param item
	 *        The reference to the item.
	 * @return The GradingItems, or null if not found.
	 */
	GradingItem gradingItemFindByItem(ToolItemReference item);

	/**
	 * Get the items that contribute to grading from all tools in the site, with best evaluations for each user.
	 * 
	 * @param site
	 *        The site.
	 * @return The list of GradingItems, may be empty.
	 */
	List<GradingItem> gradingItemFindBySite(Site site);

	/**
	 * Get the items that contribute to grading from all tools in the site, with evaluations just from this user.
	 * 
	 * @param site
	 *        The site.
	 * @param user
	 *        The user.
	 * @return The list of GradingItems, may be empty.
	 */
	List<GradingItem> gradingItemFindBySiteUser(Site site, User user);

	/**
	 * Find the level in the scale for this level id.
	 * 
	 * @param id
	 *        The level id.
	 * @param scale
	 *        The scale.
	 * @return The level found, or null if not found.
	 */
	Level levelFindById(Long id, List<Level> scale);

	/**
	 * Find the level in the scale for this number.
	 * 
	 * @param number
	 *        The scale level number (0..n).
	 * @param scale
	 *        The scale.
	 * @return The level found, or null if not found.
	 */
	Level levelFindByNumber(Integer number, List<Level> scale);

	/**
	 * Refresh this Level object with a full data load from the database, overwriting any values, setting it to unchanged.
	 * 
	 * @param level
	 *        The level.
	 */
	void levelRefresh(Level level);

	/**
	 * Wrap an id into an unloaded level.
	 * 
	 * @param id
	 *        The id.
	 * @return The level.
	 */
	Level levelWrap(Long id);

	/**
	 * Get the options for this site.
	 * 
	 * @param site
	 *        The site.
	 * @return The options for this site.
	 */
	Options optionsGet(Site site);

	/**
	 * Save the options for the site.
	 * 
	 * @param user
	 *        The user making the changes.
	 * @param site
	 *        The site.
	 * @param options
	 *        The options for the site.
	 */
	void optionsSave(User savedBy, Site site, Options options);

	/**
	 * Create a new rubric.
	 * 
	 * @param createdBy
	 *        The user creating the rubric.
	 * @param site
	 *        The site in which the rubric will live.
	 * @return The rubric.
	 */
	Rubric rubricAdd(User createdBy, Site site);

	/**
	 * Clone the rubric into a new object.
	 * 
	 * @param rubric
	 *        The rubric.
	 * @return A deep copy of the rubric.
	 */
	Rubric rubricClone(Rubric rubric);

	/**
	 * Get all the rubrics defined for a site.
	 *
	 * @param site
	 *        The site.
	 * @return The List of rubrics, may be empty.
	 */
	List<Rubric> rubricFindBySite(Site site);

	/**
	 * Get the rubric with this id.
	 * 
	 * @param id
	 *        The id.
	 * @return The rubric with this id, or null if not found.
	 */
	Rubric rubricGet(Long id);

	/**
	 * Refresh this Rubric object with a full data load from the database, overwriting any values, setting it to unchanged.
	 * 
	 * @param rubric
	 *        The rubric.
	 */
	void rubricRefresh(Rubric rubric);

	/**
	 * Remove this rubric
	 * 
	 * @param rubric
	 *        The rubric to remove.
	 */
	void rubricRemove(Rubric rubric);

	/**
	 * Save any changes to this rubric.
	 * 
	 * @param savedBy
	 *        The user making the save.
	 * @param rubric
	 *        The rubric.
	 */
	void rubricSave(User savedBy, Rubric rubric);

	/**
	 * Wrap an id into an unloaded rubric.
	 * 
	 * @param id
	 *        The id.
	 * @return The rubric.
	 */
	Rubric rubricWrap(Long id);
}

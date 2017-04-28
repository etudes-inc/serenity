/**********************************************************************************
 * $URL: https://source.etudes.org/svn/serenity/trunk/core/core-webapp/src/main/java/org/etudes/evaluation/webapp/EvaluationServiceImpl.java $
 * $Id: EvaluationServiceImpl.java 11587 2015-09-10 03:14:52Z ggolden $
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

import java.sql.ResultSet;
import java.sql.SQLException;
import java.util.ArrayList;
import java.util.Date;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Map.Entry;
import java.util.Set;

import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;
import org.etudes.download.api.DownloadHandler;
import org.etudes.evaluation.api.Category;
import org.etudes.evaluation.api.Criterion;
import org.etudes.evaluation.api.Evaluation;
import org.etudes.evaluation.api.Evaluation.Type;
import org.etudes.evaluation.api.EvaluationDesign;
import org.etudes.evaluation.api.EvaluationService;
import org.etudes.evaluation.api.GradingItem;
import org.etudes.evaluation.api.Level;
import org.etudes.evaluation.api.Options;
import org.etudes.evaluation.api.Rubric;
import org.etudes.evaluation.api.Standard;
import org.etudes.file.api.FileService;
import org.etudes.roster.api.Member;
import org.etudes.roster.api.Membership;
import org.etudes.roster.api.Role;
import org.etudes.roster.api.RosterService;
import org.etudes.service.api.Service;
import org.etudes.service.api.Services;
import org.etudes.site.api.Site;
import org.etudes.site.api.SiteService;
import org.etudes.sitecontent.api.Archive;
import org.etudes.sitecontent.api.Artifact;
import org.etudes.sitecontent.api.GradeProvider;
import org.etudes.sitecontent.api.SiteContentHandler;
import org.etudes.sitecontent.api.StudentContentHandler;
import org.etudes.sql.api.SqlService;
import org.etudes.tool.api.Tool;
import org.etudes.tool.api.ToolItemReference;
import org.etudes.tool.api.ToolItemType;
import org.etudes.tool.api.ToolItemWorkReference;
import org.etudes.user.api.User;
import org.etudes.user.api.UserService;

public class EvaluationServiceImpl implements EvaluationService, SiteContentHandler, DownloadHandler, StudentContentHandler, Service
{
	/** Our log. */
	private static Log M_log = LogFactory.getLog(EvaluationServiceImpl.class);

	@Override
	public void archive(Site fromSite, Boolean authoredContentOnly, Archive toArchive)
	{
		// TODO: archive the criteria defined for the site - the designs and evaluations are taken care of by the tools
	}

	@Override
	public boolean authorize(User authenticatedUser, ToolItemReference holder)
	{
		// item is evaluation id - user must be instructor in site OR target (submitted_by) of the evaluation
		Role userRole = rosterService().userRoleInSite(authenticatedUser, holder.getSite());
		if (userRole.ge(Role.instructor)) return true;

		Evaluation e = evaluationGet(holder.getItemId()); // TODO: full read is a bit overkill, all we need is a) id is valid, b) the user. maybe the site
		if ((e != null) && (e.getWorkReference().getUser().equals(authenticatedUser))) return true;

		return false;
	}

	@Override
	public Category categoryAdd(User createdBy, Site site)
	{
		CategoryImpl rv = new CategoryImpl();
		rv.initSite(site);
		rv.initCreatedBy(createdBy);
		rv.initModifiedBy(createdBy);
		Date now = new Date();
		rv.initCreatedOn(now);
		rv.initModifiedOn(now);
		rv.setLoaded();

		categorySave(createdBy, rv);

		return rv;
	}

	@Override
	public List<Category> categoryFindBySite(Site site)
	{
		return categoryReadSiteTx(site);
	}

	@Override
	public Category categoryFindByType(List<Category> categories, ToolItemType type)
	{
		for (Category c : categories)
		{
			if (c.getType() == type) return c;
		}
		return null;
	}

	@Override
	public Category categoryGet(Long id)
	{
		CategoryImpl rv = new CategoryImpl();
		rv.initId(id);
		return categoryReadTx(rv);
	}

	@Override
	public void categoryRefresh(Category category)
	{
		categoryReadTx((CategoryImpl) category);
	}

	@Override
	public void categoryRemove(final Category category)
	{
		sqlService().transact(new Runnable()
		{
			@Override
			public void run()
			{
				categoryRemoveTx(category);
			}
		}, "categoryRemove");
	}

	@Override
	public void categorySave(User savedBy, final Category category)
	{
		if (category.isChanged() || category.getId() == null)
		{
			if (category.isChanged())
			{
				// set modified by/on
				((CategoryImpl) category).initModifiedBy(savedBy);
				((CategoryImpl) category).initModifiedOn(new Date());
			}

			// insert
			if (category.getId() == null)
			{
				sqlService().transact(new Runnable()
				{
					@Override
					public void run()
					{
						String sql = "INSERT INTO EVALUATION_CATEGORY (SITE, TITLE, CATEGORY_TYPE, SITEORDER, DROPLOWEST, CREATED_BY, CREATED_ON, MODIFIED_BY, MODIFIED_ON) VALUES (?,?,?,?,?,?,?,?,?)";
						Object[] fields = new Object[9];
						int i = 0;
						fields[i++] = category.getSite().getId();
						fields[i++] = category.getTitle();
						fields[i++] = category.getType().getId();
						fields[i++] = category.getOrder();
						fields[i++] = category.getNumberLowestToDrop();
						fields[i++] = category.getCreatedBy().getId();
						fields[i++] = category.getCreatedOn();
						fields[i++] = category.getModifiedBy().getId();
						fields[i++] = category.getModifiedOn();

						Long id = sqlService().insert(sql, fields, "ID");
						((CategoryImpl) category).initId(id);
					}
				}, "categorySave(insert)");
			}

			// update
			else
			{
				sqlService().transact(new Runnable()
				{
					@Override
					public void run()
					{
						String sql = "UPDATE EVALUATION_CATEGORY SET SITE=?, TITLE=?, CATEGORY_TYPE=?, SITEORDER=?, DROPLOWEST=?, CREATED_BY=?, CREATED_ON=?, MODIFIED_BY=?, MODIFIED_ON=? WHERE ID=?";
						Object[] fields = new Object[10];
						int i = 0;
						fields[i++] = category.getSite().getId();
						fields[i++] = category.getTitle();
						fields[i++] = category.getType().getId();
						fields[i++] = category.getOrder();
						fields[i++] = category.getNumberLowestToDrop();
						fields[i++] = category.getCreatedBy().getId();
						fields[i++] = category.getCreatedOn();
						fields[i++] = category.getModifiedBy().getId();
						fields[i++] = category.getModifiedOn();
						fields[i++] = category.getId();

						sqlService().update(sql, fields);
					}
				}, "categorySave(update)");
			}

			((CategoryImpl) category).clearChanged();
		}
	}

	@Override
	public Category categoryWrap(Long id)
	{
		if (id == null) return null;
		if (id == 0) return null;
		CategoryImpl rv = new CategoryImpl();
		rv.initId(id);
		return rv;
	}

	@Override
	public void clear(final Site site)
	{
		// evaluations are handled by tools.
	}

	@Override
	public void clear(final Site site, final User user)
	{
		// evaluations are handled by tools.
	}

	@Override
	public void criterionRefresh(Criterion criterion)
	{
		criterionReadTx((CriterionImpl) criterion);
	}

	@Override
	public Criterion criterionWrap(Long id)
	{
		CriterionImpl criterion = new CriterionImpl();
		criterion.initId(id);

		return criterion;
	}

	@Override
	public EvaluationDesign designAdd(ToolItemReference ref)
	{
		EvaluationDesignImpl rv = new EvaluationDesignImpl();
		rv.initRef(ref);

		designSave(rv);

		return rv;
	}

	@Override
	public EvaluationDesign designClone(EvaluationDesign design)
	{
		EvaluationDesignImpl rv = new EvaluationDesignImpl();
		rv.initId(design.getId());
		rv.initRef(design.getRef());
		rv.initForGrade(design.getForGrade());
		rv.initPoints(design.getActualPoints());
		rv.initRubric(design.getRubric());
		rv.initCategory(design.getActualCategory());
		rv.initDefaultCategory(((EvaluationDesignImpl) design).getDefaultCategory());
		rv.initCategoryPos(design.getCategoryPosition());
		rv.initAutoRelease(design.getAutoRelease());

		return rv;
	}

	@Override
	public EvaluationDesign designGet(ToolItemReference ref)
	{
		return designReadTx(ref);
	}

	@Override
	public void designRemove(final EvaluationDesign design)
	{
		sqlService().transact(new Runnable()
		{
			@Override
			public void run()
			{
				designRemoveTx(design);
			}
		}, "designRemove");
	}

	@Override
	public void designSave(final EvaluationDesign design)
	{
		if (design.isChanged())
		{
			// insert
			if (design.getId() == null)
			{
				sqlService().transact(new Runnable()
				{
					@Override
					public void run()
					{
						String sql = "INSERT INTO EVALUATION_DESIGN (SITE, TOOL, ITEM, GRADE, AUTORELEASE, POINTS, RUBRIC, CATEGORY, CATEGORY_POS) VALUES (?,?,?,?,?,?,?,?,?)";
						Object[] fields = new Object[9];
						int i = 0;
						fields[i++] = design.getRef().getSite().getId();
						fields[i++] = design.getRef().getTool().getId();
						fields[i++] = design.getRef().getItemId();
						fields[i++] = design.getForGrade();
						fields[i++] = design.getAutoRelease();
						fields[i++] = design.getActualPoints();
						fields[i++] = (design.getRubric() == null) ? null : design.getRubric().getId();
						fields[i++] = (design.getActualCategory() == null) ? null : design.getActualCategory().getId();
						fields[i++] = design.getCategoryPosition();

						Long id = sqlService().insert(sql, fields, "ID");
						((EvaluationDesignImpl) design).initId(id);
					}
				}, "designSave(insert)");
			}

			// update
			else
			{
				sqlService().transact(new Runnable()
				{
					@Override
					public void run()
					{
						String sql = "UPDATE EVALUATION_DESIGN SET SITE=?, TOOL=?, ITEM=?, GRADE=?, AUTORELEASE=?, POINTS=?, RUBRIC=? , CATEGORY=?, CATEGORY_POS=? WHERE ID=?";
						Object[] fields = new Object[10];
						int i = 0;
						fields[i++] = design.getRef().getSite().getId();
						fields[i++] = design.getRef().getTool().getId();
						fields[i++] = design.getRef().getItemId();
						fields[i++] = design.getForGrade();
						fields[i++] = design.getAutoRelease();
						fields[i++] = design.getActualPoints();
						fields[i++] = (design.getRubric() == null) ? null : design.getRubric().getId();
						fields[i++] = (design.getActualCategory() == null) ? null : design.getActualCategory().getId();
						fields[i++] = design.getCategoryPosition();

						fields[i++] = design.getId();

						sqlService().update(sql, fields);
					}
				}, "designSave(update)");
			}

			((EvaluationDesignImpl) design).clearChanged();
		}
	}

	@Override
	public Evaluation evaluationAdd(User createdBy, ToolItemWorkReference ref, Type type)
	{
		EvaluationImpl evaluation = new EvaluationImpl();
		evaluation.initType(type);
		evaluation.initWorkReference(ref);
		evaluation.initCreatedBy(createdBy);
		evaluation.initCreatedOn(new Date());
		evaluation.initModifiedBy(createdBy);
		evaluation.initModifiedOn(evaluation.getCreatedOn());
		evaluation.initRatings(new HashMap<Criterion, Integer>());

		// check the design for auto release
		EvaluationDesign design = this.designGet(ref.getItem());
		if ((design != null) && design.getAutoRelease()) evaluation.initReleased(Boolean.TRUE);

		evaluationSave(createdBy, evaluation);

		return evaluation;
	}

	@Override
	public Evaluation evaluationFindBestByItemUser(List<Evaluation> evaluations, ToolItemReference item, User user)
	{
		Evaluation best = null;

		for (Evaluation e : evaluations)
		{
			if (e.getWorkReference().getUser().equals(user) && e.getWorkReference().getItem().equals(item))
			{
				// has to be released
				if (e.getReleased())
				{
					// pick up the first one
					if (best == null)
					{
						best = e;
					}

					// pick up one with a higher score
					else if (e.getScore() > best.getScore())
					{
						best = e;
					}

					// pick up one with a match to the highest score if it is evaluated later
					else if ((e.getScore() == best.getScore()) && (e.getModifiedOn().after(best.getModifiedOn())))
					{
						best = e;
					}
				}
			}
		}

		return best;
	}

	@Override
	public Evaluation evaluationFindBestByItemUser(ToolItemReference ref, User user, Type type)
	{
		// TODO Auto-generated method stub
		return null;
	}

	@Override
	public List<Evaluation> evaluationFindByItem(ToolItemReference ref, Type type)
	{
		return evaluationFindByItemTx(ref, type);
	}

	@Override
	public List<Evaluation> evaluationFindBySite(Site site, Type type, User user)
	{
		return evaluationFindBySiteTx(site, type, user);
	}

	@Override
	public List<Evaluation> evaluationFindByWork(List<Evaluation> evaluations, ToolItemWorkReference ref)
	{
		List<Evaluation> rv = new ArrayList<Evaluation>();
		for (Evaluation e : evaluations)
		{
			if (e.getWorkReference().equals(ref))
			{
				rv.add(e);
			}
		}

		return rv;
	}

	@Override
	public List<Evaluation> evaluationFindByWork(ToolItemWorkReference ref, Type type)
	{
		return evaluationFindByWorkTx(ref, type);
	}

	@Override
	public Evaluation evaluationGet(Long id)
	{
		return evaluationGetTx(id);
	}

	@Override
	public void evaluationRemoveItem(final ToolItemReference item)
	{
		// clear any references
		List<Evaluation> evaluations = evaluationFindByItem(item, null);
		for (Evaluation evaluation : evaluations)
		{
			fileService().removeExcept(evaluation.getReference(), null);
		}

		sqlService().transact(new Runnable()
		{
			@Override
			public void run()
			{
				evaluationRemoveItemTx(item);
			}
		}, "evaluationRemoveItem");
	}

	@Override
	public void evaluationSave(User savedBy, final Evaluation evaluation)
	{
		if (((EvaluationImpl) evaluation).isChanged() || ((EvaluationImpl) evaluation).isReviewedChanged() || evaluation.getId() == null)
		{
			if (((EvaluationImpl) evaluation).isChanged())
			{
				// set modified by/on
				((EvaluationImpl) evaluation).initModifiedBy(savedBy);
				((EvaluationImpl) evaluation).initModifiedOn(new Date());

				// deal with the comment
				((EvaluationImpl) evaluation).saveComment();
			}

			// insert or update
			if (evaluation.getId() == null)
			{
				sqlService().transact(new Runnable()
				{
					@Override
					public void run()
					{
						evaluationInsertTx((EvaluationImpl) evaluation);
					}
				}, "evaluationSave(insert)");
			}
			else
			{
				sqlService().transact(new Runnable()
				{
					@Override
					public void run()
					{
						evaluationUpdateTx((EvaluationImpl) evaluation);

					}
				}, "evaluationSave(update)");
			}

			// deal with ratings
			if (((EvaluationImpl) evaluation).isChanged())
			{
				ratingsSave(evaluation);
			}

			((EvaluationImpl) evaluation).clearChanged();
		}
	}

	@Override
	public GradingItem gradingItemFindByItem(ToolItemReference item)
	{
		Map<Tool, Service> handlers = Services.getHandlers(GradeProvider.class);
		if (handlers != null)
		{
			Service s = handlers.get(item.getTool());
			if (s != null)
			{
				if (s instanceof GradeProvider)
				{
					GradingItem gi = ((GradeProvider) s).getGradingItem(item);
					return gi;
				}
			}
		}

		return null;
	}

	@Override
	public List<GradingItem> gradingItemFindBySite(Site site)
	{
		List<GradingItem> rv = new ArrayList<GradingItem>();

		// for all tools that provide grading items
		Map<Tool, Service> handlers = Services.getHandlers(GradeProvider.class);
		if (handlers != null)
		{
			Set<Entry<Tool, Service>> handlerSet = handlers.entrySet();
			for (Entry<Tool, Service> s : handlerSet)
			{
				if (s.getValue() instanceof GradeProvider)
				{
					rv.addAll(((GradeProvider) s.getValue()).getGradingItems(site));
				}
			}
		}

		// evaluation summary for these items - collect one best released evaluated complete submission's evaluation from each user (active or not) for each item
		Membership gradableSiteRoster = rosterService().getAggregateSiteRosterForRole(site, Role.student, Boolean.FALSE);
		List<Evaluation> evaluations = evaluationFindBySite(site, Evaluation.Type.official, null);

		for (GradingItem gi : rv)
		{
			for (Member m : gradableSiteRoster.getMembers())
			{
				Evaluation best = evaluationFindBestByItemUser(evaluations, gi.getItemReference(), m.getUser());
				if (best != null)
				{
					gi.getEvaluations().add(best);
				}
			}
		}

		// attach default categories
		attachDefaultCategories(site, rv);

		return rv;
	}

	@Override
	public List<GradingItem> gradingItemFindBySiteUser(Site site, User user)
	{
		List<GradingItem> rv = new ArrayList<GradingItem>();

		// for all tools that provide grading items
		Map<Tool, Service> handlers = Services.getHandlers(GradeProvider.class);
		if (handlers != null)
		{
			Set<Entry<Tool, Service>> handlerSet = handlers.entrySet();
			for (Entry<Tool, Service> s : handlerSet)
			{
				if (s.getValue() instanceof GradeProvider)
				{
					rv.addAll(((GradeProvider) s.getValue()).getGradingItems(site));
				}
			}
		}

		// evaluations from this user for these items - collect one best released evaluated complete submission's evaluation from the user for each item
		List<Evaluation> evaluations = evaluationFindBySite(site, Evaluation.Type.official, user);

		for (GradingItem gi : rv)
		{
			Evaluation best = evaluationFindBestByItemUser(evaluations, gi.getItemReference(), user);
			if (best != null)
			{
				gi.getEvaluations().add(best);
			}
		}

		// attach default categories
		attachDefaultCategories(site, rv);

		return rv;
	};

	@Override
	public void importFromArchive(Artifact fromArtifact, Boolean authoredContentOnly, Site intoSite, User importingUser)
	{
		// TODO: import criteria - tools take care of design
	}

	@Override
	public void importFromSite(Site fromSite, Site intoSite, User importingUser)
	{
		// TODO: import criteria - tools take care of design
	}

	@Override
	public Level levelFindById(Long id, List<Level> scale)
	{
		for (Level l : scale)
		{
			if (l.getId().equals(id)) return l;
		}

		return null;
	}

	@Override
	public Level levelFindByNumber(Integer number, List<Level> scale)
	{
		for (Level l : scale)
		{
			if (l.getNumber().equals(number)) return l;
		}

		return null;
	}

	@Override
	public void levelRefresh(Level level)
	{
		levelReadTx((LevelImpl) level);
	}

	@Override
	public Level levelWrap(Long id)
	{
		if (id == null) return null;
		if (id == 0) return null;
		LevelImpl rv = new LevelImpl();
		rv.initId(id);
		return rv;
	}

	@Override
	public Options optionsGet(Site site)
	{
		// TODO:
		return new OptionsImpl();
	}

	@Override
	public void optionsSave(User savedBy, Site site, Options options)
	{
		// TODO Auto-generated method stub
	}

	@Override
	public void purge(final Site site)
	{
		// purge criteria - tools take care of evaluations and design
		// TODO: maybe nothing?
	}

	@Override
	public Rubric rubricAdd(User createdBy, Site site)
	{
		RubricImpl rv = new RubricImpl();
		rv.initSite(site);
		rv.initCreatedBy(createdBy);
		rv.initModifiedBy(createdBy);
		Date now = new Date();
		rv.initCreatedOn(now);
		rv.initModifiedOn(now);
		rv.setLoaded();

		rubricSave(createdBy, rv);

		return rv;
	}

	@Override
	public Rubric rubricClone(Rubric rubric)
	{
		RubricImpl rv = new RubricImpl();
		rv.initEntity(rubric);
		rv.initTitle(rubric.getTitle());
		rv.initCriteria(rubric.getCriteria());

		return rv;
	}

	@Override
	public List<Rubric> rubricFindBySite(Site site)
	{
		return rubricReadSiteTx(site);
	}

	@Override
	public Rubric rubricGet(Long id)
	{
		RubricImpl rv = new RubricImpl();
		rv.initId(id);
		return rubricReadTx(rv);
	}

	@Override
	public void rubricRefresh(Rubric rubric)
	{
		rubricReadTx((RubricImpl) rubric);
	}

	@Override
	public void rubricRemove(final Rubric rubric)
	{
		sqlService().transact(new Runnable()
		{
			@Override
			public void run()
			{
				rubricRemoveTx(rubric);
			}
		}, "rubricRemove");
	}

	@Override
	public void rubricSave(User savedBy, final Rubric rubric)
	{
		if (rubric.isChanged() || rubric.getId() == null)
		{
			if (rubric.isChanged())
			{
				// set modified by/on
				((RubricImpl) rubric).initModifiedBy(savedBy);
				((RubricImpl) rubric).initModifiedOn(new Date());
			}

			// insert
			if (rubric.getId() == null)
			{
				sqlService().transact(new Runnable()
				{
					@Override
					public void run()
					{
						String sql = "INSERT INTO EVALUATION_RUBRIC (SITE, TITLE, CREATED_BY, CREATED_ON, MODIFIED_BY, MODIFIED_ON) VALUES (?,?,?,?,?,?)";
						Object[] fields = new Object[6];
						fields[0] = rubric.getSite().getId();
						fields[1] = rubric.getTitle();
						fields[2] = rubric.getCreatedBy().getId();
						fields[3] = rubric.getCreatedOn();
						fields[4] = rubric.getModifiedBy().getId();
						fields[5] = rubric.getModifiedOn();

						Long id = sqlService().insert(sql, fields, "ID");
						((RubricImpl) rubric).initId(id);

						sql = "INSERT INTO EVALUATION_LEVEL (RUBRIC, TITLE, DESCRIPTION, NUMBER) VALUES (?,?,?,?)";
						fields = new Object[4];
						fields[0] = rubric.getId();
						for (Level l : rubric.getScale())
						{
							fields[1] = l.getTitle();
							fields[2] = l.getDescription();
							fields[3] = l.getNumber();

							Long levelId = sqlService().insert(sql, fields, "ID");
							((LevelImpl) l).initId(levelId);
						}

						sql = "INSERT INTO EVALUATION_CRITERION (RUBRIC, TITLE, DESCRIPTION, SCOREPCT, CRITERIONORDER) VALUES (?,?,?,?,?)";
						String standardsSql = "INSERT INTO EVALUATION_STANDARD (CRITERION, LEVEL, DESCRIPTION) VALUES (?,?,?)";
						fields = new Object[5];
						fields[0] = rubric.getId();
						Object[] fieldsForStandards = new Object[3];

						int order = 1;
						for (Criterion c : rubric.getCriteria())
						{
							fields[1] = c.getTitle();
							fields[2] = c.getDescription();
							fields[3] = c.getScorePct();
							fields[4] = Integer.valueOf(order++);

							Long criterionId = sqlService().insert(sql, fields, "ID");
							((CriterionImpl) c).initId(criterionId);

							fieldsForStandards[0] = c.getId();
							for (Standard s : c.getStandards())
							{
								fieldsForStandards[1] = s.getLevel().getId();
								fieldsForStandards[2] = s.getDescription();

								Long standardId = sqlService().insert(standardsSql, fieldsForStandards, "ID");
								((StandardImpl) s).initId(standardId);
							}
						}
					}
				}, "rubricSave(insert)");
			}

			// update
			else
			{
				sqlService().transact(new Runnable()
				{
					@Override
					public void run()
					{
						// which levels to remove
						List<Long> levelIdsToRemove = ((RubricImpl) rubric).levelsToRemove();
						if (!levelIdsToRemove.isEmpty())
						{
							Object[] fields = new Object[1];
							String sql = "DELETE FROM EVALUATION_LEVEL WHERE ID=?";
							for (Long id : levelIdsToRemove)
							{
								fields[0] = id;
								sqlService().update(sql, fields);
							}
						}

						for (Level l : rubric.getScale())
						{
							// insert if new
							if (l.getId() == null)
							{
								String sql = "INSERT INTO EVALUATION_LEVEL (RUBRIC, TITLE, DESCRIPTION, NUMBER) VALUES (?,?,?,?)";
								Object[] fields = new Object[4];
								fields[0] = rubric.getId();
								fields[1] = l.getTitle();
								fields[2] = l.getDescription();
								fields[3] = l.getNumber();

								Long levelId = sqlService().insert(sql, fields, "ID");
								((LevelImpl) l).initId(levelId);
							}

							// update, if changed
							else if (l.isChanged())
							{
								String sql = "UPDATE EVALUATION_LEVEL SET TITLE=?, DESCRIPTION=?, NUMBER=? WHERE ID=?";
								Object[] fields = new Object[4];
								fields[0] = l.getTitle();
								fields[1] = l.getDescription();
								fields[2] = l.getNumber();
								fields[3] = l.getId();

								sqlService().update(sql, fields);
							}
						}

						// which criteria to remove
						List<Long> idsToRemove = ((RubricImpl) rubric).criteriaToRemove();
						if (!idsToRemove.isEmpty())
						{
							Object[] fields = new Object[1];
							String criterionSql = "DELETE FROM EVALUATION_CRITERION WHERE ID=?";
							String standardsSql = "DELETE EVALUATION_STANDARD FROM EVALUATION_STANDARD JOIN EVALUATION_CRITERION ON EVALUATION_STANDARD.CRITERION = EVALUATION_CRITERION.ID WHERE EVALUATION_CRITERION.ID=?";
							for (Long id : idsToRemove)
							{
								fields[0] = id;
								sqlService().update(standardsSql, fields);
								sqlService().update(criterionSql, fields);
							}
						}

						int order = 1;
						for (Criterion c : rubric.getCriteria())
						{
							// insert if new
							if (c.getId() == null)
							{
								String sql = "INSERT INTO EVALUATION_CRITERION (RUBRIC, TITLE, DESCRIPTION, SCOREPCT, CRITERIONORDER) VALUES (?,?,?,?,?)";
								Object[] fields = new Object[5];
								fields[0] = rubric.getId();
								fields[1] = c.getTitle();
								fields[2] = c.getDescription();
								fields[3] = c.getScorePct();
								fields[4] = Integer.valueOf(order++);

								Long criterionId = sqlService().insert(sql, fields, "ID");
								((CriterionImpl) c).initId(criterionId);
							}

							// update // TODO: if changed? watch out for order
							else
							{
								String sql = "UPDATE EVALUATION_CRITERION SET TITLE=?, DESCRIPTION=?, SCOREPCT=?, CRITERIONORDER=? WHERE ID=?";
								Object[] fields = new Object[5];
								fields[0] = c.getTitle();
								fields[1] = c.getDescription();
								fields[2] = c.getScorePct();
								fields[3] = Integer.valueOf(order++);
								fields[4] = c.getId();

								sqlService().update(sql, fields);
							}

							// criterion standards
							idsToRemove = ((CriterionImpl) c).standardsToRemove();
							if (!idsToRemove.isEmpty())
							{
								Object[] fields = new Object[1];
								String standardsSql = "DELETE FROM EVALUATION_STANDARD WHERE ID=?";
								for (Long id : idsToRemove)
								{
									fields[0] = id;
									sqlService().update(standardsSql, fields);
								}
							}

							for (Standard s : c.getStandards())
							{
								// insert if new
								if (s.getId() == null)
								{
									String sql = "INSERT INTO EVALUATION_STANDARD (CRITERION, LEVEL, DESCRIPTION) VALUES (?,?,?)";
									Object[] fields = new Object[3];
									fields[0] = c.getId();
									fields[1] = s.getLevel().getId();
									fields[2] = s.getDescription();

									Long standardId = sqlService().insert(sql, fields, "ID");
									((StandardImpl) s).initId(standardId);
								}

								// update
								else
								{
									String sql = "UPDATE EVALUATION_STANDARD SET CRITERION=?, LEVEL=?, DESCRIPTION=? WHERE ID=?";
									Object[] fields = new Object[4];
									fields[0] = c.getId();
									fields[1] = s.getLevel().getId();
									fields[2] = s.getDescription();
									fields[3] = s.getId();

									sqlService().update(sql, fields);
								}
							}
						}

						String sql = "UPDATE EVALUATION_RUBRIC SET SITE=?, TITLE=?, CREATED_BY=?, CREATED_ON=?, MODIFIED_BY=?, MODIFIED_ON=? WHERE ID=?";
						Object[] fields = new Object[7];
						fields[0] = rubric.getSite().getId();
						fields[1] = rubric.getTitle();
						fields[2] = rubric.getCreatedBy().getId();
						fields[3] = rubric.getCreatedOn();
						fields[4] = rubric.getModifiedBy().getId();
						fields[5] = rubric.getModifiedOn();
						fields[6] = rubric.getId();

						sqlService().update(sql, fields);
					}
				}, "rubricSave(update)");
			}

			((RubricImpl) rubric).clearChanged();
		}
	}

	@Override
	public Rubric rubricWrap(Long id)
	{
		if (id == null) return null;
		if (id == 0) return null;
		RubricImpl rv = new RubricImpl();
		rv.initId(id);
		return rv;
	}

	@Override
	public boolean start()
	{
		M_log.info("EvaluationServiceImpl: start");
		return true;
	}

	/**
	 * For any item that has no category, if it can be placed into an existing category by type, do so.
	 * 
	 * @param site
	 *        the site.
	 * @param items
	 *        The grading items.
	 */
	protected void attachDefaultCategories(Site site, List<GradingItem> items)
	{
		List<Category> categories = categoryFindBySite(site);
		for (GradingItem gi : items)
		{
			if (gi.getDesign().getCategory() == null)
			{
				Category c = categoryFindByType(categories, gi.getType());
				((EvaluationDesignImpl) (gi.getDesign())).initDefaultCategory(c);
			}
		}
	}

	/**
	 * Transaction code for reading categories in a site.
	 * 
	 * @param site
	 *        the site.
	 * @return The list of categories for the site, may be empty.
	 */
	protected List<Category> categoryReadSiteTx(final Site site)
	{
		String sql = "SELECT ID, TITLE, CATEGORY_TYPE, SITEORDER, DROPLOWEST, CREATED_BY, CREATED_ON, MODIFIED_BY, MODIFIED_ON FROM EVALUATION_CATEGORY WHERE SITE=? ORDER BY SITEORDER ASC";
		Object[] fields = new Object[1];
		fields[0] = site.getId();

		List<Category> rv = sqlService().select(sql, fields, new SqlService.Reader<Category>()
		{
			@Override
			public Category read(ResultSet result)
			{
				try
				{
					CategoryImpl category = new CategoryImpl();
					category.initSite(site);

					int i = 1;
					category.initId(sqlService().readLong(result, i++));
					category.initTitle(sqlService().readString(result, i++));
					category.initType(ToolItemType.valueOf(sqlService().readInteger(result, i++)));
					category.initOrder(sqlService().readInteger(result, i++));
					category.initDrop(sqlService().readInteger(result, i++));
					category.initCreatedBy(userService().wrap(sqlService().readLong(result, i++)));
					category.initCreatedOn(sqlService().readDate(result, i++));
					category.initModifiedBy(userService().wrap(sqlService().readLong(result, i++)));
					category.initModifiedOn(sqlService().readDate(result, i++));

					return category;
				}
				catch (SQLException e)
				{
					M_log.warn("categoryReadSiteTx: " + e);
					return null;
				}
			}
		});

		return rv;
	}

	/**
	 * Transaction code for reading a category.
	 * 
	 * @param category
	 *        the category with id set to read.
	 * @return The category, or null if not found.
	 */
	protected Category categoryReadTx(final CategoryImpl category)
	{
		String sql = "SELECT SITE, TITLE, CATEGORY_TYPE, SITEORDER, DROPLOWEST, CREATED_BY, CREATED_ON, MODIFIED_BY, MODIFIED_ON FROM EVALUATION_CATEGORY WHERE ID=?";
		Object[] fields = new Object[1];
		fields[0] = category.getId();

		List<Category> categories = sqlService().select(sql, fields, new SqlService.Reader<Category>()
		{
			@Override
			public Category read(ResultSet result)
			{
				try
				{
					int i = 1;
					category.initSite(siteService().wrap(sqlService().readLong(result, i++)));
					category.initTitle(sqlService().readString(result, i++));
					category.initType(ToolItemType.valueOf(sqlService().readInteger(result, i++)));
					category.initOrder(sqlService().readInteger(result, i++));
					category.initDrop(sqlService().readInteger(result, i++));
					category.initCreatedBy(userService().wrap(sqlService().readLong(result, i++)));
					category.initCreatedOn(sqlService().readDate(result, i++));
					category.initModifiedBy(userService().wrap(sqlService().readLong(result, i++)));
					category.initModifiedOn(sqlService().readDate(result, i++));

					category.setLoaded();
					return category;
				}
				catch (SQLException e)
				{
					M_log.warn("categoryReadTx: " + e);
					return null;
				}
			}
		});

		if (categories.size() == 0) return null;
		return category;
	}

	/**
	 * Transaction code for removing a category.
	 * 
	 * @param category
	 *        The category.
	 */
	protected void categoryRemoveTx(Category category)
	{
		String sql = "DELETE FROM EVALUATION_CATEGORY WHERE ID=?";
		Object[] fields = new Object[1];

		fields[0] = category.getId();
		sqlService().update(sql, fields);
	}

	/**
	 * Transaction code for reading a criterion.
	 * 
	 * @param criterion
	 *        the criterion with id set to read.
	 * @return The criterion, or null if not found.
	 */
	protected Criterion criterionReadTx(final CriterionImpl criterion)
	{
		String sql = "SELECT ID, TITLE, DESCRIPTION, SCOREPCT FROM EVALUATION_CRITERION C WHERE C.ID=?";
		Object[] fields = new Object[1];
		fields[0] = criterion.getId();
		List<Criterion> criteria = sqlService().select(sql, fields, new SqlService.Reader<Criterion>()
		{
			@Override
			public Criterion read(ResultSet result)
			{
				try
				{
					CriterionImpl criterion = new CriterionImpl();

					int i = 1;
					criterion.initId(sqlService().readLong(result, i++));
					criterion.initTitle(sqlService().readString(result, i++));
					criterion.initDescription(sqlService().readString(result, i++));
					criterion.initScorePct(sqlService().readInteger(result, i++));

					return criterion;
				}
				catch (SQLException e)
				{
					M_log.warn("criterionReadTx(CRITERION): " + e);
					return null;
				}
			}
		});

		if (criteria.size() == 0) return null;

		// read the stqndards for the criterion
		sql = "SELECT S.ID, S.LEVEL, S.DESCRIPTION FROM EVALUATION_STANDARD S JOIN EVALUATION_LEVEL L ON S.LEVEL = L.ID WHERE S.CRITERION=? ORDER BY L.NUMBER ASC";
		List<Standard> standards = sqlService().select(sql, fields, new SqlService.Reader<Standard>()
		{
			@Override
			public Standard read(ResultSet result)
			{
				try
				{
					StandardImpl standard = new StandardImpl();

					int i = 1;
					standard.initId(sqlService().readLong(result, i++));
					standard.initLevel(levelWrap(sqlService().readLong(result, i++)));
					standard.initDescription(sqlService().readString(result, i++));

					return standard;
				}
				catch (SQLException e)
				{
					M_log.warn("criterionReadTx(CRITERION): " + e);
					return null;
				}
			}
		});

		((CriterionImpl) criterion).initStandards(standards);

		((CriterionImpl) criterion).setLoaded();
		return criterion;
	}

	/**
	 * Transaction code for reading a design for a tool item.
	 * 
	 * @param ref
	 *        The ToolItemReference.
	 * @return The EvaluationDesign for the item, which may be new.
	 */
	protected EvaluationDesign designReadTx(final ToolItemReference ref)
	{
		final EvaluationDesignImpl rv = new EvaluationDesignImpl();
		rv.initRef(ref);

		String sql = "SELECT E.ID, E.GRADE, E.AUTORELEASE, E.POINTS, E.RUBRIC, C.ID, E.CATEGORY_POS FROM EVALUATION_DESIGN E LEFT OUTER JOIN EVALUATION_CATEGORY C ON E.CATEGORY = C.ID WHERE E.SITE=? AND E.TOOL=? AND E.ITEM=?";
		Object[] fields = new Object[3];
		fields[0] = ref.getSite().getId();
		fields[1] = ref.getTool().getId();
		fields[2] = ref.getItemId();

		sqlService().select(sql, fields, new SqlService.Reader<EvaluationDesign>()
		{
			@Override
			public EvaluationDesign read(ResultSet result)
			{
				try
				{
					int i = 1;
					rv.initId(sqlService().readLong(result, i++));
					rv.initForGrade(sqlService().readBoolean(result, i++));
					rv.initAutoRelease(sqlService().readBoolean(result, i++));
					rv.initPoints(sqlService().readFloat(result, i++));
					rv.initRubric(rubricWrap(sqlService().readLong(result, i++)));
					rv.initCategory(categoryWrap(sqlService().readLong(result, i++)));
					rv.initCategoryPos(sqlService().readInteger(result, i++));

					return null;
				}
				catch (SQLException e)
				{
					M_log.warn("designReadTx(EVALUATION_DESIGN): " + e);
					return null;
				}
			}
		});

		return rv;
	}

	/**
	 * Transaction code for removing a design.
	 * 
	 * @param design
	 *        The design.
	 */
	protected void designRemoveTx(EvaluationDesign design)
	{
		Object[] fields = new Object[1];
		fields[0] = design.getId();

		String sql = "DELETE FROM EVALUATION_DESIGN WHERE ID=?";
		sqlService().update(sql, fields);
	}

	/**
	 * Transaction code for reading the Evaluation for an item (all work.
	 * 
	 * @param ref
	 *        The the item reference.
	 * @param type
	 *        The evaluation type.
	 * @return The List of Evaluation of this type for this item, may be empty.
	 */
	protected List<Evaluation> evaluationFindByItemTx(final ToolItemReference ref, final Type type)
	{
		String sql = "SELECT ID, WORK, SUBMITTED_BY, SUBMITTED_ON, SCORE, COMMENT, EVALUATED, RELEASED, CREATED_BY, CREATED_ON, MODIFIED_BY, MODIFIED_ON, REVIEWED_ON FROM EVALUATION_EVALUATION WHERE SITE=? AND TOOL=? AND ITEM=?";
		Object[] fields = new Object[((type == null) ? 3 : 4)];
		fields[0] = ref.getSite().getId();
		fields[1] = ref.getTool().getId();
		fields[2] = ref.getItemId();
		if (type != null)
		{
			sql += " AND EVALUATION_TYPE=?";
			fields[3] = type.getId();
		}

		final List<Evaluation> rv = sqlService().select(sql, fields, new SqlService.Reader<Evaluation>()
		{
			@Override
			public EvaluationImpl read(ResultSet result)
			{
				try
				{
					int i = 1;
					EvaluationImpl evaluation = new EvaluationImpl();
					evaluation.initType(type);
					evaluation.initId(sqlService().readLong(result, i++));

					Long workId = sqlService().readLong(result, i++);
					User user = userService().wrap(sqlService().readLong(result, i++));
					Date submittedOn = sqlService().readDate(result, i++);
					evaluation.initWorkReference(new ToolItemWorkReference(new ToolItemReference(ref.getSite(), ref.getTool(), ref.getItemId()),
							workId, user, submittedOn));

					evaluation.initScore(sqlService().readFloat(result, i++));
					evaluation.initCommentReferenceId(sqlService().readLong(result, i++));
					evaluation.initEvaluated(sqlService().readBoolean(result, i++));
					evaluation.initReleased(sqlService().readBoolean(result, i++));
					evaluation.initCreatedBy(userService().wrap(sqlService().readLong(result, i++)));
					evaluation.initCreatedOn(sqlService().readDate(result, i++));
					evaluation.initModifiedBy(userService().wrap(sqlService().readLong(result, i++)));
					evaluation.initModifiedOn(sqlService().readDate(result, i++));
					evaluation.initReviewedOn(sqlService().readDate(result, i++));

					return evaluation;
				}
				catch (SQLException e)
				{
					M_log.warn("evaluationFindByItemTx(evaluation): " + e);
					return null;
				}
			}
		});

		if (!rv.isEmpty())
		{
			// read the ratings
			sql = "SELECT R.CRITERION, R.RATING, R.EVALUATION FROM EVALUATION_RATING R JOIN EVALUATION_EVALUATION E ON R.EVALUATION = E.ID WHERE E.SITE=? AND E.TOOL=? AND E.ITEM=?";
			if (type != null)
			{
				sql += " AND E.EVALUATION_TYPE=?";
			}

			sqlService().select(sql, fields, new SqlService.Reader<Integer>()
			{
				@Override
				public Integer read(ResultSet result)
				{
					try
					{
						int i = 1;

						Criterion c = criterionWrap(sqlService().readLong(result, i++));
						Integer r = sqlService().readInteger(result, i++);
						Long eid = sqlService().readLong(result, i++);

						// find the evaluation
						for (Evaluation e : rv)
						{
							if (e.getId().equals(eid))
							{
								((EvaluationImpl) e).initRating(c, r);
								break;
							}
						}

						return null;
					}
					catch (SQLException e)
					{
						M_log.warn("evaluationFindByItemTx(evaluting_rating): " + e);
						return null;
					}
				}
			});
		}

		return rv;
	}

	/**
	 * Transaction code for reading the Evaluation for a site, for a user.
	 * 
	 * @param site
	 *        The site.
	 * @param type
	 *        The evaluation type.
	 * @param user
	 *        The user. If null, all users.
	 * @return The List of Evaluation of this type for this work, may be empty.
	 */
	protected List<Evaluation> evaluationFindBySiteTx(final Site site, final Type type, final User user)
	{
		// TODO: may need new index site type user
		String sql = "SELECT ID, TOOL, ITEM, WORK, SUBMITTED_BY, SUBMITTED_ON, SCORE, COMMENT, EVALUATED, RELEASED, CREATED_BY, CREATED_ON, MODIFIED_BY, MODIFIED_ON, REVIEWED_ON FROM EVALUATION_EVALUATION WHERE SITE=? AND EVALUATION_TYPE=?";

		Object[] fields = null;
		if (user == null)
		{
			fields = new Object[2];
		}
		else
		{
			sql += " AND SUBMITTED_BY=?";

			fields = new Object[3];
			fields[2] = user.getId();
		}
		fields[0] = site.getId();
		fields[1] = type.getId();

		final List<Evaluation> rv = sqlService().select(sql, fields, new SqlService.Reader<Evaluation>()
		{
			@Override
			public EvaluationImpl read(ResultSet result)
			{
				try
				{
					int i = 1;
					EvaluationImpl evaluation = new EvaluationImpl();
					evaluation.initType(type);
					evaluation.initId(sqlService().readLong(result, i++));

					Tool tool = Tool.valueOf(sqlService().readInteger(result, i++));
					Long itemId = sqlService().readLong(result, i++);
					Long workId = sqlService().readLong(result, i++);
					User submittedBy = userService().wrap(sqlService().readLong(result, i++));
					Date submittedOn = sqlService().readDate(result, i++);
					evaluation.initWorkReference(new ToolItemWorkReference(new ToolItemReference(site, tool, itemId), workId, submittedBy,
							submittedOn));

					evaluation.initScore(sqlService().readFloat(result, i++));
					evaluation.initCommentReferenceId(sqlService().readLong(result, i++));
					evaluation.initEvaluated(sqlService().readBoolean(result, i++));
					evaluation.initReleased(sqlService().readBoolean(result, i++));
					evaluation.initCreatedBy(userService().wrap(sqlService().readLong(result, i++)));
					evaluation.initCreatedOn(sqlService().readDate(result, i++));
					evaluation.initModifiedBy(userService().wrap(sqlService().readLong(result, i++)));
					evaluation.initModifiedOn(sqlService().readDate(result, i++));
					evaluation.initReviewedOn(sqlService().readDate(result, i++));

					return evaluation;
				}
				catch (SQLException e)
				{
					M_log.warn("evaluationFindBySiteTx(evaluation): " + e);
					return null;
				}
			}
		});

		if (!rv.isEmpty())
		{
			// read the ratings
			sql = "SELECT R.CRITERION, R.RATING, R.EVALUATION FROM EVALUATION_RATING R JOIN EVALUATION_EVALUATION E ON R.EVALUATION = E.ID WHERE E.SITE=? AND E.EVALUATION_TYPE=?";
			if (user != null) sql += " AND E.SUBMITTED_BY=?";
			sqlService().select(sql, fields, new SqlService.Reader<Integer>()
			{
				@Override
				public Integer read(ResultSet result)
				{
					try
					{
						int i = 1;

						Criterion c = criterionWrap(sqlService().readLong(result, i++));
						Integer r = sqlService().readInteger(result, i++);
						Long eid = sqlService().readLong(result, i++);

						// find the evaluation
						for (Evaluation e : rv)
						{
							if (e.getId().equals(eid))
							{
								((EvaluationImpl) e).initRating(c, r);
								break;
							}
						}

						return null;
					}
					catch (SQLException e)
					{
						M_log.warn("evaluationFindBySiteTx(evaluting_rating): " + e);
						return null;
					}
				}
			});
		}

		return rv;
	}

	/**
	 * Transaction code for reading the Evaluation for a work.
	 * 
	 * @param ref
	 *        The the work reference.
	 * @param type
	 *        The evaluation type.
	 * @return The List of Evaluation of this type for this work, may be empty.
	 */
	protected List<Evaluation> evaluationFindByWorkTx(final ToolItemWorkReference ref, final Type type)
	{
		String sql = "SELECT ID, SCORE, COMMENT, EVALUATED, RELEASED, CREATED_BY, CREATED_ON, MODIFIED_BY, MODIFIED_ON, REVIEWED_ON FROM EVALUATION_EVALUATION WHERE SITE=? AND TOOL=? AND ITEM=? AND WORK=? AND EVALUATION_TYPE=?";
		Object[] fields = new Object[5];
		fields[0] = ref.getItem().getSite().getId();
		fields[1] = ref.getItem().getTool().getId();
		fields[2] = ref.getItem().getItemId();
		fields[3] = ref.getWorkId();
		fields[4] = type.getId();

		final List<Evaluation> rv = sqlService().select(sql, fields, new SqlService.Reader<Evaluation>()
		{
			@Override
			public EvaluationImpl read(ResultSet result)
			{
				try
				{
					int i = 1;
					EvaluationImpl evaluation = new EvaluationImpl();
					evaluation.initType(type);
					evaluation.initWorkReference(ref);

					evaluation.initId(sqlService().readLong(result, i++));
					evaluation.initScore(sqlService().readFloat(result, i++));
					evaluation.initCommentReferenceId(sqlService().readLong(result, i++));
					evaluation.initEvaluated(sqlService().readBoolean(result, i++));
					evaluation.initReleased(sqlService().readBoolean(result, i++));
					evaluation.initCreatedBy(userService().wrap(sqlService().readLong(result, i++)));
					evaluation.initCreatedOn(sqlService().readDate(result, i++));
					evaluation.initModifiedBy(userService().wrap(sqlService().readLong(result, i++)));
					evaluation.initModifiedOn(sqlService().readDate(result, i++));
					evaluation.initReviewedOn(sqlService().readDate(result, i++));

					return evaluation;
				}
				catch (SQLException e)
				{
					M_log.warn("evaluationFindByWorkTx(evaluation): " + e);
					return null;
				}
			}
		});

		if (!rv.isEmpty())
		{
			// read the ratings
			sql = "SELECT R.CRITERION, R.RATING, R.EVALUATION FROM EVALUATION_RATING R JOIN EVALUATION_EVALUATION E ON R.EVALUATION = E.ID WHERE E.SITE=? AND E.TOOL=? AND E.ITEM=? AND E.WORK=? AND E.EVALUATION_TYPE=?";
			sqlService().select(sql, fields, new SqlService.Reader<Integer>()
			{
				@Override
				public Integer read(ResultSet result)
				{
					try
					{
						int i = 1;

						Criterion c = criterionWrap(sqlService().readLong(result, i++));
						Integer r = sqlService().readInteger(result, i++);
						Long eid = sqlService().readLong(result, i++);

						// find the evaluation
						for (Evaluation e : rv)
						{
							if (e.getId().equals(eid))
							{
								((EvaluationImpl) e).initRating(c, r);
								break;
							}
						}

						return null;
					}
					catch (SQLException e)
					{
						M_log.warn("evaluationFindByWorkTx(evaluting_rating): " + e);
						return null;
					}
				}
			});
		}

		return rv;
	}

	/**
	 * Transaction code for reading an Evaluation.
	 * 
	 * @param id
	 *        The evaluation id.
	 * @return The Evaluation, or null if not found.
	 */
	protected Evaluation evaluationGetTx(final Long id)
	{
		String sql = "SELECT SITE, TOOL, ITEM, WORK, SUBMITTED_BY, SUBMITTED_ON, EVALUATION_TYPE, SCORE, COMMENT, EVALUATED, RELEASED, CREATED_BY, CREATED_ON, MODIFIED_BY, MODIFIED_ON, REVIEWED_ON FROM EVALUATION_EVALUATION WHERE ID=?";
		Object[] fields = new Object[1];
		fields[0] = id;

		List<Evaluation> rv = sqlService().select(sql, fields, new SqlService.Reader<Evaluation>()
		{
			@Override
			public EvaluationImpl read(ResultSet result)
			{
				try
				{
					int i = 1;
					EvaluationImpl evaluation = new EvaluationImpl();
					evaluation.initId(id);

					Site site = siteService().wrap(sqlService().readLong(result, i++));
					Tool tool = Tool.valueOf(sqlService().readInteger(result, i++));
					Long itemId = sqlService().readLong(result, i++);
					Long workId = sqlService().readLong(result, i++);
					User user = userService().wrap(sqlService().readLong(result, i++));
					Date submittedOn = sqlService().readDate(result, i++);
					evaluation.initWorkReference(new ToolItemWorkReference(new ToolItemReference(site, tool, itemId), workId, user, submittedOn));

					evaluation.initType(Evaluation.Type.fromCode(sqlService().readInteger(result, i++)));
					evaluation.initScore(sqlService().readFloat(result, i++));
					evaluation.initCommentReferenceId(sqlService().readLong(result, i++));
					evaluation.initEvaluated(sqlService().readBoolean(result, i++));
					evaluation.initReleased(sqlService().readBoolean(result, i++));
					evaluation.initCreatedBy(userService().wrap(sqlService().readLong(result, i++)));
					evaluation.initCreatedOn(sqlService().readDate(result, i++));
					evaluation.initModifiedBy(userService().wrap(sqlService().readLong(result, i++)));
					evaluation.initModifiedOn(sqlService().readDate(result, i++));
					evaluation.initReviewedOn(sqlService().readDate(result, i++));

					return evaluation;
				}
				catch (SQLException e)
				{
					M_log.warn("evaluationGetTx(evaluation): " + e);
					return null;
				}
			}
		});

		final Evaluation e = rv.isEmpty() ? null : rv.get(0);
		if (e != null)
		{
			// read the ratings
			sql = "SELECT CRITERION, RATING FROM EVALUATION_RATING WHERE EVALUATION=?";
			sqlService().select(sql, fields, new SqlService.Reader<Integer>()
			{
				@Override
				public Integer read(ResultSet result)
				{
					try
					{
						int i = 1;

						Criterion c = criterionWrap(sqlService().readLong(result, i++));
						Integer r = sqlService().readInteger(result, i++);
						((EvaluationImpl) e).initRating(c, r);

						return null;
					}
					catch (SQLException e)
					{
						M_log.warn("evaluationGetTx(evaluting_rating): " + e);
						return null;
					}
				}
			});
		}

		return e;
	}

	/**
	 * Transaction code for inserting an evaluation.
	 * 
	 * @param evaluation
	 *        The evaluation.
	 */
	protected void evaluationInsertTx(EvaluationImpl evaluation)
	{
		String sql = "INSERT INTO EVALUATION_EVALUATION (SITE, TOOL, ITEM, WORK, SUBMITTED_BY, SUBMITTED_ON, EVALUATION_TYPE, SCORE, COMMENT, EVALUATED, RELEASED, CREATED_BY, CREATED_ON, MODIFIED_BY, MODIFIED_ON, REVIEWED_ON) VALUES (?,?,?,?,?,?,?,?,?,?,?,?,?,?,?,?)";

		Object[] fields = new Object[16];
		int i = 0;
		fields[i++] = evaluation.getWorkReference().getItem().getSite().getId();
		fields[i++] = evaluation.getWorkReference().getItem().getTool().getId();
		fields[i++] = evaluation.getWorkReference().getItem().getItemId();
		fields[i++] = evaluation.getWorkReference().getWorkId();
		fields[i++] = evaluation.getWorkReference().getUser().getId();
		fields[i++] = evaluation.getWorkReference().getSubmittedOn();
		fields[i++] = evaluation.getType().getId();
		fields[i++] = evaluation.getActualScore();
		fields[i++] = evaluation.getCommentReferenceId();
		fields[i++] = evaluation.getEvaluated();
		fields[i++] = evaluation.getReleased();
		fields[i++] = evaluation.getCreatedBy().getId();
		fields[i++] = evaluation.getCreatedOn();
		fields[i++] = evaluation.getModifiedBy().getId();
		fields[i++] = evaluation.getModifiedOn();
		fields[i++] = evaluation.getReviewedOn();

		Long id = sqlService().insert(sql, fields, "ID");
		evaluation.initId(id);
	}

	/**
	 * Transaction code for removing all evaluations for a tool item.
	 * 
	 * @param item
	 *        The tool item reference.
	 */
	protected void evaluationRemoveItemTx(ToolItemReference item)
	{
		Object[] fields = new Object[3];
		fields[0] = item.getSite().getId();
		fields[1] = item.getTool().getId();
		fields[2] = item.getItemId();
		String sql = "DELETE EVALUATION_RATING FROM EVALUATION_RATING JOIN EVALUATION_EVALUATION ON EVALUATION_RATING.EVALUATION = EVALUATION_EVALUATION.ID WHERE EVALUATION_EVALUATION.SITE=? AND EVALUATION_EVALUATION.TOOL=? AND EVALUATION_EVALUATION.ITEM=?";
		sqlService().update(sql, fields);

		sql = "DELETE FROM EVALUATION_EVALUATION WHERE SITE=? AND TOOL=? AND ITEM=?";
		sqlService().update(sql, fields);
	}

	/**
	 * Transaction code for updating an existing evaluation.
	 * 
	 * @param evaluation
	 *        The evaluation.
	 */
	protected void evaluationUpdateTx(EvaluationImpl evaluation)
	{
		String sql = "UPDATE EVALUATION_EVALUATION SET SITE=?, TOOL=?, ITEM=?, WORK=?, SUBMITTED_BY=?, SUBMITTED_ON=?, EVALUATION_TYPE=?, SCORE=?, COMMENT=?, EVALUATED=?, RELEASED=?, CREATED_BY=?, CREATED_ON=?, MODIFIED_BY=?, MODIFIED_ON=?, REVIEWED_ON=? WHERE ID=?";

		Object[] fields = new Object[17];
		int i = 0;
		fields[i++] = evaluation.getWorkReference().getItem().getSite().getId();
		fields[i++] = evaluation.getWorkReference().getItem().getTool().getId();
		fields[i++] = evaluation.getWorkReference().getItem().getItemId();
		fields[i++] = evaluation.getWorkReference().getWorkId();
		fields[i++] = evaluation.getWorkReference().getUser().getId();
		fields[i++] = evaluation.getWorkReference().getSubmittedOn();
		fields[i++] = evaluation.getType().getId();
		fields[i++] = evaluation.getActualScore();
		fields[i++] = evaluation.getCommentReferenceId();
		fields[i++] = evaluation.getEvaluated();
		fields[i++] = evaluation.getReleased();
		fields[i++] = evaluation.getCreatedBy().getId();
		fields[i++] = evaluation.getCreatedOn();
		fields[i++] = evaluation.getModifiedBy().getId();
		fields[i++] = evaluation.getModifiedOn();
		fields[i++] = evaluation.getReviewedOn();

		fields[i++] = evaluation.getId();

		sqlService().update(sql, fields);
	}

	/**
	 * Transaction code for reading a level.
	 * 
	 * @param level
	 *        the level with id set to read.
	 * @return The level, or null if not found.
	 */
	protected Level levelReadTx(final LevelImpl level)
	{
		String sql = "SELECT TITLE, DESCRIPTION, NUMBER FROM EVALUATION_LEVEL WHERE ID=?";
		Object[] fields = new Object[1];
		fields[0] = level.getId();
		final List<Level> levels = sqlService().select(sql, fields, new SqlService.Reader<Level>()
		{
			@Override
			public Level read(ResultSet result)
			{
				try
				{
					LevelImpl level = new LevelImpl();

					int i = 1;
					level.initTitle(sqlService().readString(result, i++));
					level.initDescription(sqlService().readString(result, i++));
					level.initNumber(sqlService().readInteger(result, i++));

					level.setLoaded();
					return level;
				}
				catch (SQLException e)
				{
					M_log.warn("levelReadTx: " + e);
					return null;
				}
			}
		});

		if (levels.size() == 0) return null;
		return level;
	}

	/**
	 * Save the ratings that have changed.
	 * 
	 * @param evaluation
	 *        The evaluations.
	 */
	protected void ratingsSave(final Evaluation evaluation)
	{
		// which criterion remove
		final List<Criterion> toRemove = ((EvaluationImpl) evaluation).criteriaToRemove();

		// which criterion add
		final List<Criterion> toAdd = ((EvaluationImpl) evaluation).criteriaToAdd();

		// which have changed
		final List<Criterion> toChange = ((EvaluationImpl) evaluation).criteriaToUpdate();

		if (toRemove.isEmpty() && toAdd.isEmpty() && toChange.isEmpty()) return;

		sqlService().transact(new Runnable()
		{
			@Override
			public void run()
			{
				Object[] fields = new Object[2];
				fields[0] = evaluation.getId();

				String sql = "DELETE FROM EVALUATION_RATING WHERE EVALUATION=? AND CRITERION=?";
				for (Criterion c : toRemove)
				{
					fields[1] = c.getId();
					sqlService().update(sql, fields);
				}

				sql = "INSERT INTO EVALUATION_RATING (EVALUATION, CRITERION, RATING) VALUES (?,?,?)";
				fields = new Object[3];
				fields[0] = evaluation.getId();

				for (Criterion c : toAdd)
				{
					fields[1] = c.getId();
					fields[2] = evaluation.getRatings().get(c);
					sqlService().update(sql, fields);
				}

				sql = "UPDATE EVALUATION_RATING SET RATING=? WHERE EVALUATION=? AND CRITERION=?";
				fields = new Object[3];
				fields[1] = evaluation.getId();

				for (Criterion c : toChange)
				{
					fields[0] = evaluation.getRatings().get(c);
					fields[2] = c.getId();
					sqlService().update(sql, fields);
				}
			}
		}, "ratingsSave");
	}

	/**
	 * Transaction code for removing all evaluation for a tool item.
	 * 
	 * @param ref
	 *        The work reference.
	 */
	protected void removeItemEvaluationTx(ToolItemReference ref)
	{
		Object[] fields = new Object[3];
		fields[0] = ref.getSite().getId();
		fields[1] = ref.getTool().getId();
		fields[2] = ref.getItemId();

		String sql = "DELETE FROM EVALUATION_EVALUATION WHERE SITE=? AND TOOL=? AND ITEM=?";
		sqlService().update(sql, fields);
	}

	/**
	 * Transaction code for removing all evaluation for a work.
	 * 
	 * @param ref
	 *        The work reference.
	 */
	protected void removeWorkEvaluationTx(ToolItemWorkReference ref)
	{
		Object[] fields = new Object[4];
		fields[0] = ref.getItem().getSite().getId();
		fields[1] = ref.getItem().getTool().getId();
		fields[2] = ref.getItem().getItemId();
		fields[3] = ref.getWorkId();

		String sql = "DELETE FROM EVALUATION_EVALUATION WHERE SITE=? AND TOOL=? AND ITEM=? AND WORK=?";
		sqlService().update(sql, fields);
	}

	/**
	 * Transaction code for reading rubrics in a site.
	 * 
	 * @param site
	 *        the site.
	 * @return The list of rubrics for the site, may be empty.
	 */
	protected List<Rubric> rubricReadSiteTx(final Site site)
	{
		String sql = "SELECT ID, TITLE, CREATED_BY, CREATED_ON, MODIFIED_BY, MODIFIED_ON FROM EVALUATION_RUBRIC WHERE SITE=?";
		Object[] fields = new Object[1];
		fields[0] = site.getId();

		List<Rubric> rv = sqlService().select(sql, fields, new SqlService.Reader<Rubric>()
		{
			@Override
			public Rubric read(ResultSet result)
			{
				try
				{
					RubricImpl rubric = new RubricImpl();
					rubric.initSite(site);

					int i = 1;
					rubric.initId(sqlService().readLong(result, i++));
					rubric.initTitle(sqlService().readString(result, i++));
					rubric.initCreatedBy(userService().wrap(sqlService().readLong(result, i++)));
					rubric.initCreatedOn(sqlService().readDate(result, i++));
					rubric.initModifiedBy(userService().wrap(sqlService().readLong(result, i++)));
					rubric.initModifiedOn(sqlService().readDate(result, i++));

					return rubric;
				}
				catch (SQLException e)
				{
					M_log.warn("rubricReadSiteTx(RUBRIC): " + e);
					return null;
				}
			}
		});

		// TODO: one read for all
		for (Rubric r : rv)
		{
			sql = "SELECT ID, TITLE, DESCRIPTION, NUMBER FROM EVALUATION_LEVEL WHERE RUBRIC=? ORDER BY NUMBER ASC";
			fields = new Object[1];
			fields[0] = r.getId();
			final List<Level> scale = sqlService().select(sql, fields, new SqlService.Reader<Level>()
			{
				@Override
				public Level read(ResultSet result)
				{
					try
					{
						LevelImpl level = new LevelImpl();

						int i = 1;
						level.initId(sqlService().readLong(result, i++));
						level.initTitle(sqlService().readString(result, i++));
						level.initDescription(sqlService().readString(result, i++));
						level.initNumber(sqlService().readInteger(result, i++));

						level.setLoaded();
						return level;
					}
					catch (SQLException e)
					{
						M_log.warn("designReadTx(LEVEL): " + e);
						return null;
					}
				}
			});

			((RubricImpl) r).initScale(scale);

			sql = "SELECT ID, TITLE, DESCRIPTION, SCOREPCT FROM EVALUATION_CRITERION C WHERE C.RUBRIC=? ORDER BY CRITERIONORDER ASC";
			fields = new Object[1];
			fields[0] = r.getId();
			List<Criterion> criteria = sqlService().select(sql, fields, new SqlService.Reader<Criterion>()
			{
				@Override
				public Criterion read(ResultSet result)
				{
					try
					{
						CriterionImpl criterion = new CriterionImpl();

						int i = 1;
						criterion.initId(sqlService().readLong(result, i++));
						criterion.initTitle(sqlService().readString(result, i++));
						criterion.initDescription(sqlService().readString(result, i++));
						criterion.initScorePct(sqlService().readInteger(result, i++));

						return criterion;
					}
					catch (SQLException e)
					{
						M_log.warn("rubricReadTx(CRITERION): " + e);
						return null;
					}
				}
			});

			((RubricImpl) r).initCriteria(criteria);

			// TODO: criterion standards - one read
			for (Criterion criterion : criteria)
			{
				// read the stqndards for the criterion
				sql = "SELECT S.ID, S.LEVEL, S.DESCRIPTION FROM EVALUATION_STANDARD S JOIN EVALUATION_LEVEL L ON S.LEVEL = L.ID WHERE S.CRITERION=? ORDER BY L.NUMBER ASC";
				fields[0] = criterion.getId();
				List<Standard> standards = sqlService().select(sql, fields, new SqlService.Reader<Standard>()
				{
					@Override
					public Standard read(ResultSet result)
					{
						try
						{
							StandardImpl standard = new StandardImpl();

							int i = 1;
							standard.initId(sqlService().readLong(result, i++));
							standard.initLevel(levelFindById(sqlService().readLong(result, i++), scale));
							standard.initDescription(sqlService().readString(result, i++));

							return standard;
						}
						catch (SQLException e)
						{
							M_log.warn("criterionReadTx(CRITERION): " + e);
							return null;
						}
					}
				});

				((CriterionImpl) criterion).initStandards(standards);
				((CriterionImpl) criterion).setLoaded();
			}

			((RubricImpl) r).setLoaded();
		}

		return rv;
	}

	/**
	 * Transaction code for reading a rubric.
	 * 
	 * @param rubric
	 *        the rubric with id set to read.
	 * @return The rubric, or null if not found.
	 */
	protected Rubric rubricReadTx(final RubricImpl rubric)
	{
		String sql = "SELECT SITE, TITLE, CREATED_BY, CREATED_ON, MODIFIED_BY, MODIFIED_ON FROM EVALUATION_RUBRIC WHERE ID=?";
		Object[] fields = new Object[1];
		fields[0] = rubric.getId();

		List<Rubric> rubrics = sqlService().select(sql, fields, new SqlService.Reader<Rubric>()
		{
			@Override
			public Rubric read(ResultSet result)
			{
				try
				{
					int i = 1;
					rubric.initSite(siteService().wrap(sqlService().readLong(result, i++)));
					rubric.initTitle(sqlService().readString(result, i++));
					rubric.initCreatedBy(userService().wrap(sqlService().readLong(result, i++)));
					rubric.initCreatedOn(sqlService().readDate(result, i++));
					rubric.initModifiedBy(userService().wrap(sqlService().readLong(result, i++)));
					rubric.initModifiedOn(sqlService().readDate(result, i++));

					return rubric;
				}
				catch (SQLException e)
				{
					M_log.warn("rubricReadTx(RUBRIC): " + e);
					return null;
				}
			}
		});

		if (rubrics.size() == 0) return null;

		sql = "SELECT ID, TITLE, DESCRIPTION, NUMBER FROM EVALUATION_LEVEL WHERE RUBRIC=? ORDER BY NUMBER ASC";
		fields[0] = rubric.getId();
		final List<Level> scale = sqlService().select(sql, fields, new SqlService.Reader<Level>()
		{
			@Override
			public Level read(ResultSet result)
			{
				try
				{
					LevelImpl level = new LevelImpl();

					int i = 1;
					level.initId(sqlService().readLong(result, i++));
					level.initTitle(sqlService().readString(result, i++));
					level.initDescription(sqlService().readString(result, i++));
					level.initNumber(sqlService().readInteger(result, i++));

					level.setLoaded();
					return level;
				}
				catch (SQLException e)
				{
					M_log.warn("designReadTx(LEVEL): " + e);
					return null;
				}
			}
		});

		((RubricImpl) rubric).initScale(scale);

		sql = "SELECT ID, TITLE, DESCRIPTION, SCOREPCT FROM EVALUATION_CRITERION C WHERE C.RUBRIC=? ORDER BY CRITERIONORDER ASC";
		List<Criterion> criteria = sqlService().select(sql, fields, new SqlService.Reader<Criterion>()
		{
			@Override
			public Criterion read(ResultSet result)
			{
				try
				{
					CriterionImpl criterion = new CriterionImpl();

					int i = 1;
					criterion.initId(sqlService().readLong(result, i++));
					criterion.initTitle(sqlService().readString(result, i++));
					criterion.initDescription(sqlService().readString(result, i++));
					criterion.initScorePct(sqlService().readInteger(result, i++));

					return criterion;
				}
				catch (SQLException e)
				{
					M_log.warn("rubricReadTx(CRITERION): " + e);
					return null;
				}
			}
		});

		((RubricImpl) rubric).initCriteria(criteria);

		// TODO: criterion standards - one read
		for (Criterion criterion : criteria)
		{
			// read the stqndards for the criterion
			sql = "SELECT S.ID, S.LEVEL, S.DESCRIPTION FROM EVALUATION_STANDARD S JOIN EVALUATION_LEVEL L ON S.LEVEL = L.ID WHERE S.CRITERION=? ORDER BY L.NUMBER ASC";
			fields[0] = criterion.getId();
			List<Standard> standards = sqlService().select(sql, fields, new SqlService.Reader<Standard>()
			{
				@Override
				public Standard read(ResultSet result)
				{
					try
					{
						StandardImpl standard = new StandardImpl();

						int i = 1;
						standard.initId(sqlService().readLong(result, i++));
						standard.initLevel(levelFindById(sqlService().readLong(result, i++), scale));
						standard.initDescription(sqlService().readString(result, i++));

						return standard;
					}
					catch (SQLException e)
					{
						M_log.warn("criterionReadTx(CRITERION): " + e);
						return null;
					}
				}
			});

			((CriterionImpl) criterion).initStandards(standards);
			((CriterionImpl) criterion).setLoaded();
		}

		((RubricImpl) rubric).setLoaded();
		return rubric;
	}

	/**
	 * Transaction code for removing a rubric.
	 * 
	 * @param rubric
	 *        The rubric.
	 */
	protected void rubricRemoveTx(Rubric rubric)
	{
		Object[] fields = new Object[1];
		fields[0] = rubric.getId();

		String sql = "DELETE FROM EVALUATION_LEVEL WHERE RUBRIC=?";
		sqlService().update(sql, fields);

		// criterion standards
		sql = "DELETE EVALUATION_STANDARD FROM EVALUATION_STANDARD JOIN EVALUATION_CRITERION ON EVALUATION_STANDARD.CRITERION = EVALUATION_CRITERION.ID WHERE EVALUATION_CRITERION.RUBRIC=?";
		sqlService().update(sql, fields);

		sql = "DELETE FROM EVALUATION_CRITERION WHERE RUBRIC=?";
		sqlService().update(sql, fields);

		sql = "DELETE FROM EVALUATION_RUBRIC WHERE ID=?";
		sqlService().update(sql, fields);
	}

	/**
	 * @return The registered FileService.
	 */
	private FileService fileService()
	{
		return (FileService) Services.get(FileService.class);
	}

	/**
	 * @return The registered RosterService.
	 */
	private RosterService rosterService()
	{
		return (RosterService) Services.get(RosterService.class);
	}

	/**
	 * @return The registered SiteService.
	 */
	private SiteService siteService()
	{
		return (SiteService) Services.get(SiteService.class);
	}

	/**
	 * @return The registered SqlService.
	 */
	private SqlService sqlService()
	{
		return (SqlService) Services.get(SqlService.class);
	}

	/**
	 * @return The registered UserService.
	 */
	private UserService userService()
	{
		return (UserService) Services.get(UserService.class);
	}
}

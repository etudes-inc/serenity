/**********************************************************************************
 * $URL: https://source.etudes.org/svn/serenity/trunk/evaluation/evaluation-webapp/src/main/java/org/etudes/evaluation/webapp/EvaluationCdpHandler.java $
 * $Id: EvaluationCdpHandler.java 11959 2015-10-30 18:01:52Z ggolden $
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

import java.io.IOException;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import javax.servlet.ServletException;
import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;

import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;
import org.etudes.cdp.api.CdpHandler;
import org.etudes.cdp.api.CdpService;
import org.etudes.cdp.api.CdpStatus;
import org.etudes.evaluation.api.Category;
import org.etudes.evaluation.api.Evaluation;
import org.etudes.evaluation.api.EvaluationService;
import org.etudes.evaluation.api.GradingItem;
import org.etudes.evaluation.api.Options;
import org.etudes.evaluation.api.Rubric;
import org.etudes.roster.api.Member;
import org.etudes.roster.api.Membership;
import org.etudes.roster.api.Role;
import org.etudes.roster.api.RosterService;
import org.etudes.service.api.Services;
import org.etudes.site.api.Site;
import org.etudes.site.api.SiteService;
import org.etudes.tool.api.Tool;
import org.etudes.tool.api.ToolItemReference;
import org.etudes.tool.api.ToolItemType;
import org.etudes.user.api.User;
import org.etudes.user.api.UserService;

/**
 */
public class EvaluationCdpHandler implements CdpHandler
{
	/** Our log (commons). */
	private static Log M_log = LogFactory.getLog(EvaluationCdpHandler.class);

	public String getPrefix()
	{
		return "evaluation";
	}

	public Map<String, Object> handle(HttpServletRequest req, HttpServletResponse res, Map<String, Object> parameters, String requestPath,
			String path, User authenticatedUser) throws ServletException, IOException
	{
		// if no authenticated user, we reject all requests
		if (authenticatedUser == null)
		{
			Map<String, Object> rv = new HashMap<String, Object>();
			rv.put(CdpStatus.CDP_STATUS, CdpStatus.notLoggedIn.getId());
			return rv;
		}

		else if (requestPath.equals("gradingItems"))
		{
			return dispatchGradingItems(req, res, parameters, path, authenticatedUser);
		}

		else if (requestPath.equals("gradingMembers"))
		{
			return dispatchGradingMembers(req, res, parameters, path, authenticatedUser);
		}

		else if (requestPath.equals("gradesSave"))
		{
			return dispatchGradesSave(req, res, parameters, path, authenticatedUser);
		}

		else if (requestPath.equals("itemMembers"))
		{
			return dispatchItemMembers(req, res, parameters, path, authenticatedUser);
		}

		else if (requestPath.equals("memberItems"))
		{
			return dispatchMemberItems(req, res, parameters, path, authenticatedUser);
		}

		else if (requestPath.equals("review"))
		{
			return dispatchReview(req, res, parameters, path, authenticatedUser);
		}

		else if (requestPath.equals("rubricAdd"))
		{
			return dispatchRubricAdd(req, res, parameters, path, authenticatedUser);
		}

		else if (requestPath.equals("rubrics"))
		{
			return dispatchRubrics(req, res, parameters, path, authenticatedUser);
		}

		else if (requestPath.equals("rubricSave"))
		{
			return dispatchRubricSave(req, res, parameters, path, authenticatedUser);
		}

		else if (requestPath.equals("rubricDelete"))
		{
			return dispatchRubricDelete(req, res, parameters, path, authenticatedUser);
		}

		// else if (requestPath.equals("rubricOrder"))
		// {
		// return dispatchRubricOrder(req, res, parameters, path, authenticatedUser);
		// }

		else if (requestPath.equals("categories"))
		{
			return dispatchCategories(req, res, parameters, path, authenticatedUser);
		}

		else if (requestPath.equals("categoriesSave"))
		{
			return dispatchCategoriesSave(req, res, parameters, path, authenticatedUser);
		}

		else if (requestPath.equals("options"))
		{
			return dispatchOptions(req, res, parameters, path, authenticatedUser);
		}

		else if (requestPath.equals("optionsSave"))
		{
			return dispatchOptionsSave(req, res, parameters, path, authenticatedUser);
		}

		else if (requestPath.equals("dispatchChangeUseStandard"))
		{
			return dispatchChangeUseStandard(req, res, parameters, path, authenticatedUser);
		}

		return null;
	}

	// protected Map<String, Object> dispatchRubricOrder(HttpServletRequest req, HttpServletResponse res, Map<String, Object> parameters, String path,
	// User authenticatedUser) throws ServletException, IOException
	// {
	// Map<String, Object> rv = new HashMap<String, Object>();
	//
	// // site
	// Long siteId = cdpService().readLong(parameters.get("site"));
	// if (siteId == null)
	// {
	// M_log.warn("dispatchSave: missing site");
	// rv.put(CdpStatus.CDP_STATUS, CdpStatus.badRequest.getId());
	// return rv;
	// }
	// Site site = siteService().wrap(siteId);
	//
	// Role userRole = rosterService().userRoleInSite(authenticatedUser, site);
	//
	// // security: authenticatedUser must have a role of instructor "or higher" in the site
	// if (!userRole.ge(Role.instructor))
	// {
	// rv.put(CdpStatus.CDP_STATUS, CdpStatus.accessDenied.getId());
	// return rv;
	// }
	//
	// // order
	// List<Long> orderIds = cdpService().readIds(parameters.get("order"));
	// int order = 1;
	// for (Long rid : orderIds)
	// {
	// Rubric rubric = evaluationService().rubricGet(rid);
	// if (rubric != null)
	// {
	// rubric.s.setOrder(order++);
	// announcementService().save(authenticatedUser, a);
	// }
	// }
	//
	// // add status parameter
	// rv.put(CdpStatus.CDP_STATUS, CdpStatus.success.getId());
	//
	// return rv;
	// }

	protected Map<String, Object> dispatchCategories(HttpServletRequest req, HttpServletResponse res, Map<String, Object> parameters, String path,
			User authenticatedUser) throws ServletException, IOException
	{
		Map<String, Object> rv = new HashMap<String, Object>();

		Long siteId = cdpService().readLong(parameters.get("site"));
		if (siteId == null)
		{
			M_log.warn("dispatchCategories: missing site");
			rv.put(CdpStatus.CDP_STATUS, CdpStatus.badRequest.getId());
			return rv;
		}
		Site site = siteService().wrap(siteId);

		// security: authenticatedUser must have a role of guest "or higher" in the site
		if (!rosterService().userRoleInSite(authenticatedUser, site).ge(Role.guest))
		{
			rv.put(CdpStatus.CDP_STATUS, CdpStatus.accessDenied.getId());
			return rv;
		}

		List<Map<String, Object>> categoryList = new ArrayList<Map<String, Object>>();
		rv.put("categories", categoryList);

		List<Category> categories = evaluationService().categoryFindBySite(site);
		for (Category c : categories)
		{
			Map<String, Object> catMap = c.send();
			catMap.put("numItems", Integer.valueOf(1)); // TODO:
			categoryList.add(catMap);
		}

		rv.put("stdCategories", Boolean.FALSE); // TODO: standard or custom

		// add status parameter
		rv.put(CdpStatus.CDP_STATUS, CdpStatus.success.getId());

		return rv;
	}

	protected Map<String, Object> dispatchCategoriesSave(HttpServletRequest req, HttpServletResponse res, Map<String, Object> parameters,
			String path, User authenticatedUser) throws ServletException, IOException
	{
		Map<String, Object> rv = new HashMap<String, Object>();

		Long siteId = cdpService().readLong(parameters.get("site"));
		if (siteId == null)
		{
			M_log.warn("dispatchCategoriesSave: missing site");
			rv.put(CdpStatus.CDP_STATUS, CdpStatus.badRequest.getId());
			return rv;
		}
		Site site = siteService().wrap(siteId);

		// security: authenticatedUser must have a role of instructor "or higher" in the site
		if (!rosterService().userRoleInSite(authenticatedUser, site).ge(Role.instructor))
		{
			rv.put(CdpStatus.CDP_STATUS, CdpStatus.accessDenied.getId());
			return rv;
		}

		// removed ones
		List<Long> removed = cdpService().readIds(parameters.get("" + "removed_categories"));
		if (removed != null)
		{
			for (Long id : removed)
			{
				Category c = evaluationService().categoryGet(id);
				if (c != null)
				{
					evaluationService().categoryRemove(c);
				}
			}
		}

		// adds and updates
		int count = cdpService().readInt(parameters.get("" + "count_categories"));
		for (int i = 0; i < count; i++)
		{
			String prefix = i + "_categories_";
			Category c = null;

			Long id = cdpService().readLong(parameters.get(prefix + "id"));
			if (id < 0)
			{
				c = evaluationService().categoryAdd(authenticatedUser, site);
			}
			else
			{
				c = evaluationService().categoryGet(id);
			}

			if (c != null)
			{
				c.read(prefix, parameters);
				evaluationService().categorySave(authenticatedUser, c);
			}
		}

		// add status parameter
		rv.put(CdpStatus.CDP_STATUS, CdpStatus.success.getId());

		return rv;
	}

	protected Map<String, Object> dispatchChangeUseStandard(HttpServletRequest req, HttpServletResponse res, Map<String, Object> parameters,
			String path, User authenticatedUser) throws ServletException, IOException
	{
		Map<String, Object> rv = new HashMap<String, Object>();

		Long siteId = cdpService().readLong(parameters.get("site"));
		if (siteId == null)
		{
			M_log.warn("dispatchChangeUseStandard: missing site");
			rv.put(CdpStatus.CDP_STATUS, CdpStatus.badRequest.getId());
			return rv;
		}
		Site site = siteService().wrap(siteId);

		// security: authenticatedUser must have a role of instructor "or higher" in the site
		if (!rosterService().userRoleInSite(authenticatedUser, site).ge(Role.instructor))
		{
			rv.put(CdpStatus.CDP_STATUS, CdpStatus.accessDenied.getId());
			return rv;
		}

		Boolean useStandard = cdpService().readBoolean(parameters.get("standard"));
		if (useStandard == null)
		{
			M_log.warn("dispatchChangeUseStandard: missing standard");
			rv.put(CdpStatus.CDP_STATUS, CdpStatus.badRequest.getId());
			return rv;
		}

		// TODO:

		// add status parameter
		rv.put(CdpStatus.CDP_STATUS, CdpStatus.success.getId());

		return rv;
	}

	protected Map<String, Object> dispatchGradesSave(HttpServletRequest req, HttpServletResponse res, Map<String, Object> parameters, String path,
			User authenticatedUser) throws ServletException, IOException
	{
		Map<String, Object> rv = new HashMap<String, Object>();

		Long siteId = cdpService().readLong(parameters.get("site"));
		if (siteId == null)
		{
			M_log.warn("dispatchGradesSave: missing site");
			rv.put(CdpStatus.CDP_STATUS, CdpStatus.badRequest.getId());
			return rv;
		}
		Site site = siteService().wrap(siteId);

		// security: authenticatedUser must have a role of instructor "or higher" in the site
		if (!rosterService().userRoleInSite(authenticatedUser, site).ge(Role.instructor))
		{
			rv.put(CdpStatus.CDP_STATUS, CdpStatus.accessDenied.getId());
			return rv;
		}

		// TODO: parse and save the grades
		// count_members 0_members_id 0_members_grade

		rv.put(CdpStatus.CDP_STATUS, CdpStatus.success.getId());
		return rv;
	}

	protected Map<String, Object> dispatchGradingItems(HttpServletRequest req, HttpServletResponse res, Map<String, Object> parameters, String path,
			User authenticatedUser) throws ServletException, IOException
	{
		Map<String, Object> rv = new HashMap<String, Object>();

		Long siteId = cdpService().readLong(parameters.get("site"));
		if (siteId == null)
		{
			M_log.warn("dispatchGradingItems: missing site");
			rv.put(CdpStatus.CDP_STATUS, CdpStatus.badRequest.getId());
			return rv;
		}
		Site site = siteService().wrap(siteId);

		// security: authenticatedUser must have a role of instructor "or higher" in the site
		if (!rosterService().userRoleInSite(authenticatedUser, site).ge(Role.instructor))
		{
			rv.put(CdpStatus.CDP_STATUS, CdpStatus.accessDenied.getId());
			return rv;
		}

		List<Map<String, Object>> itemsList = new ArrayList<Map<String, Object>>();
		rv.put("items", itemsList);

		List<GradingItem> gradingItems = evaluationService().gradingItemFindBySite(site);
		for (GradingItem gi : gradingItems)
		{
			itemsList.add(gi.send());
		}

		// compute the total points and score for these items.
		Map<String, Object> totalsMap = new HashMap<String, Object>();
		rv.put("summary", totalsMap);
		float points = 0;
		int items = 0;
		float extraPoints = 0;
		int extraItems = 0;
		for (GradingItem gi : gradingItems)
		{
			// TODO: should category ever be null?
			if ((gi.getDesign().getCategory() != null) && (ToolItemType.extra == gi.getDesign().getCategory().getType()))
			{
				extraPoints += gi.getDesign().getPoints();
				extraItems++;
			}
			else
			{
				points += gi.getDesign().getPoints();
				items++;
			}
		}
		totalsMap.put("points", Float.valueOf(points));
		totalsMap.put("items", Integer.valueOf(items));
		totalsMap.put("extraPoints", Float.valueOf(extraPoints));
		totalsMap.put("extraItems", Integer.valueOf(extraItems));

		rv.put(CdpStatus.CDP_STATUS, CdpStatus.success.getId());
		return rv;
	}

	protected Map<String, Object> dispatchGradingMembers(HttpServletRequest req, HttpServletResponse res, Map<String, Object> parameters,
			String path, User authenticatedUser) throws ServletException, IOException
	{
		Map<String, Object> rv = new HashMap<String, Object>();

		Long siteId = cdpService().readLong(parameters.get("site"));
		if (siteId == null)
		{
			M_log.warn("dispatchGradingMembers: missing site");
			rv.put(CdpStatus.CDP_STATUS, CdpStatus.badRequest.getId());
			return rv;
		}
		Site site = siteService().wrap(siteId);

		// security: authenticatedUser must have a role of instructor "or higher" in the site
		if (!rosterService().userRoleInSite(authenticatedUser, site).ge(Role.instructor))
		{
			rv.put(CdpStatus.CDP_STATUS, CdpStatus.accessDenied.getId());
			return rv;
		}

		List<Map<String, Object>> memberList = new ArrayList<Map<String, Object>>();
		rv.put("members", memberList);

		Options options = evaluationService().optionsGet(site);
		List<GradingItem> gradingItems = evaluationService().gradingItemFindBySite(site);
		Membership siteRoster = rosterService().getAggregateSiteRosterForRole(site, Role.student, Boolean.FALSE);
		for (Member m : siteRoster.getMembers())
		{
			Map<String, Object> memberMap = m.send(site.getClient());
			memberList.add(memberMap);

			// compute the total points and score for this member across all items, based on the "best" (completed, released) evaluated submission.
			Map<String, Object> totalsMap = new HashMap<String, Object>();
			memberMap.put("summary", totalsMap);
			float points = 0;
			float score = 0;
			int items = 0;
			for (GradingItem gi : gradingItems)
			{
				// get this member's evaluation
				Evaluation best = evaluationService().evaluationFindBestByItemUser(gi.getEvaluations(), gi.getItemReference(), m.getUser());
				if (best != null)
				{
					points += gi.getDesign().getPoints();
					score += best.getScore();
					items++;
				}
			}
			totalsMap.put("score", Float.valueOf(score));
			totalsMap.put("points", Float.valueOf(points));
			totalsMap.put("items", Integer.valueOf(items));

			// the grade
			totalsMap.put("grade", options.scaleGradeForScore(score, points));

			// grade override
			totalsMap.put("gradeOverride", "A"); // TODO:
		}

		float points = 0;
		// int items = 0;
		float extraPoints = 0;
		// int extraItems = 0;
		for (GradingItem gi : gradingItems)
		{
			// TODO: should category ever be null?
			if ((gi.getDesign().getCategory() != null) && (ToolItemType.extra == gi.getDesign().getCategory().getType()))
			{
				extraPoints += gi.getDesign().getPoints();
				// extraItems++;
			}
			else
			{
				points += gi.getDesign().getPoints();
				// items++;
			}
		}

		Map<String, Object> summaryMap = new HashMap<String, Object>();
		rv.put("summary", summaryMap);
		summaryMap.put("points", Float.valueOf(points));
		// summaryMap.put("items", Integer.valueOf(items));
		summaryMap.put("extraPoints", Float.valueOf(extraPoints));
		// summaryMap.put("extraItems", Integer.valueOf(extraItems));

		rv.put(CdpStatus.CDP_STATUS, CdpStatus.success.getId());
		return rv;
	}

	protected Map<String, Object> dispatchItemMembers(HttpServletRequest req, HttpServletResponse res, Map<String, Object> parameters, String path,
			User authenticatedUser) throws ServletException, IOException
	{
		Map<String, Object> rv = new HashMap<String, Object>();

		// site, tool, item id for tool item reference
		Long siteId = cdpService().readLong(parameters.get("site"));
		if (siteId == null)
		{
			M_log.warn("dispatchItemMembers: missing site");
			rv.put(CdpStatus.CDP_STATUS, CdpStatus.badRequest.getId());
			return rv;
		}
		Site site = siteService().wrap(siteId);

		// security: authenticatedUser must have a role of instructor "or higher" in the site
		if (!rosterService().userRoleInSite(authenticatedUser, site).ge(Role.instructor))
		{
			rv.put(CdpStatus.CDP_STATUS, CdpStatus.accessDenied.getId());
			return rv;
		}

		Integer toolId = cdpService().readInt(parameters.get("tool"));
		if (toolId == null)
		{
			M_log.warn("dispatchItemMembers: missing tool");
			rv.put(CdpStatus.CDP_STATUS, CdpStatus.badRequest.getId());
			return rv;
		}
		Tool tool = Tool.valueOf(toolId);

		Long itemId = cdpService().readLong(parameters.get("item"));
		if (itemId == null)
		{
			M_log.warn("dispatchItemMembers: missing item");
			rv.put(CdpStatus.CDP_STATUS, CdpStatus.badRequest.getId());
			return rv;
		}

		// find the grading item for this item
		ToolItemReference item = new ToolItemReference(site, tool, itemId);
		GradingItem gi = evaluationService().gradingItemFindByItem(item);
		if (gi == null)
		{
			M_log.warn("dispatchItemMembers: grading item not found");
			rv.put(CdpStatus.CDP_STATUS, CdpStatus.badRequest.getId());
			return rv;
		}

		// send the item details
		rv.put("item", gi.send());

		// collect all the evaluations for this item
		List<Evaluation> evaluations = evaluationService().evaluationFindByItem(item, Evaluation.Type.official);

		// send a list of the site qualified members
		Membership siteRoster = rosterService().getAggregateSiteRosterForRole(site, Role.student, Boolean.FALSE);
		List<Map<String, Object>> memberList = new ArrayList<Map<String, Object>>();
		rv.put("members", memberList);
		for (Member m : siteRoster.getMembers())
		{
			Map<String, Object> memberMap = m.send(site.getClient());
			memberList.add(memberMap);

			// get the best evaluation for this member
			Evaluation e = evaluationService().evaluationFindBestByItemUser(evaluations, item, m.getUser());
			if (e != null)
			{
				// add the evaluation details to the member
				memberMap.put("evaluation", e.send());
			}
		}

		rv.put(CdpStatus.CDP_STATUS, CdpStatus.success.getId());
		return rv;
	}

	protected Map<String, Object> dispatchMemberItems(HttpServletRequest req, HttpServletResponse res, Map<String, Object> parameters, String path,
			User authenticatedUser) throws ServletException, IOException
	{
		Map<String, Object> rv = new HashMap<String, Object>();

		Long siteId = cdpService().readLong(parameters.get("site"));
		if (siteId == null)
		{
			M_log.warn("dispatchMemberItems: missing site");
			rv.put(CdpStatus.CDP_STATUS, CdpStatus.badRequest.getId());
			return rv;
		}
		Site site = siteService().wrap(siteId);

		// for who
		User target = authenticatedUser;
		Long userId = cdpService().readLong(parameters.get("user"));
		if (userId != null)
		{
			target = userService().wrap(userId);
		}

		// security: authenticatedUser must have a role of instructor "or higher" in the site, or be the target user
		if ((!target.equals(authenticatedUser)) && !rosterService().userRoleInSite(authenticatedUser, site).ge(Role.instructor))
		{
			rv.put(CdpStatus.CDP_STATUS, CdpStatus.accessDenied.getId());
			return rv;
		}

		Options options = evaluationService().optionsGet(site);

		// the items for the site, collecting the best evaluation only for this user in gi.getEvaluations()
		List<GradingItem> gradingItems = evaluationService().gradingItemFindBySiteUser(site, target);

		List<Map<String, Object>> itemsList = new ArrayList<Map<String, Object>>();
		rv.put("items", itemsList);

		for (GradingItem gi : gradingItems)
		{
			Map<String, Object> itemMap = gi.send();
			itemsList.add(itemMap);

			if (!gi.getEvaluations().isEmpty())
			{
				itemMap.put("evaluation", gi.getEvaluations().get(0).send());
			}
		}

		// the user member and summary info
		Member m = rosterService().userMemberInSite(target, site);
		if (m != null)
		{
			Map<String, Object> memberMap = m.send(site.getClient());
			rv.put("member", memberMap);

			// compute the total points and score for this member across all items, based on the "best" (completed, released) evaluated submission.
			Map<String, Object> totalsMap = new HashMap<String, Object>();
			memberMap.put("summary", totalsMap);
			float points = 0;
			float score = 0;
			float allPoints = 0;
			// int allItems = 0;
			// int items = 0;
			for (GradingItem gi : gradingItems)
			{
				if ((gi.getDesign().getCategory() == null) || (ToolItemType.extra != gi.getDesign().getCategory().getType()))
				{
					allPoints += gi.getDesign().getPoints();
					// allItems++;
				}

				// get this member's evaluation
				Evaluation best = ((gi.getEvaluations().isEmpty()) ? null : gi.getEvaluations().get(0));
				if (best != null)
				{
					points += gi.getDesign().getPoints();
					score += best.getScore();
					// items++;
				}
			}
			totalsMap.put("score", Float.valueOf(score));
			totalsMap.put("points", Float.valueOf(points));
			// totalsMap.put("items", Integer.valueOf(items));
			totalsMap.put("grade", options.scaleGradeForScore(score, points));
			totalsMap.put("allPoints", Float.valueOf(allPoints));
			// totalsMap.put("allItems", Integer.valueOf(allItems));
		}

		rv.put(CdpStatus.CDP_STATUS, CdpStatus.success.getId());
		return rv;
	}

	protected Map<String, Object> dispatchOptions(HttpServletRequest req, HttpServletResponse res, Map<String, Object> parameters, String path,
			User authenticatedUser) throws ServletException, IOException
	{
		Map<String, Object> rv = new HashMap<String, Object>();

		Long siteId = cdpService().readLong(parameters.get("site"));
		if (siteId == null)
		{
			M_log.warn("dispatchOptions: missing site");
			rv.put(CdpStatus.CDP_STATUS, CdpStatus.badRequest.getId());
			return rv;
		}
		Site site = siteService().wrap(siteId);

		// security: authenticatedUser must have a role of guest "or higher" in the site
		if (!rosterService().userRoleInSite(authenticatedUser, site).ge(Role.guest))
		{
			rv.put(CdpStatus.CDP_STATUS, CdpStatus.accessDenied.getId());
			return rv;
		}

		Options options = evaluationService().optionsGet(site);
		Map<String, Object> optionsMap = options.send();

		// add if grade scale may be changed
		optionsMap.put("gradingScaleMutable", Boolean.TRUE); // TODO: base on existence of grade overrides
		rv.put("options", optionsMap);

		// add status parameter
		rv.put(CdpStatus.CDP_STATUS, CdpStatus.success.getId());

		return rv;
	}

	protected Map<String, Object> dispatchOptionsSave(HttpServletRequest req, HttpServletResponse res, Map<String, Object> parameters, String path,
			User authenticatedUser) throws ServletException, IOException
	{
		Map<String, Object> rv = new HashMap<String, Object>();

		Long siteId = cdpService().readLong(parameters.get("site"));
		if (siteId == null)
		{
			M_log.warn("dispatchOptionsSave: missing site");
			rv.put(CdpStatus.CDP_STATUS, CdpStatus.badRequest.getId());
			return rv;
		}
		Site site = siteService().wrap(siteId);

		// security: authenticatedUser must have a role of instructor "or higher" in the site
		if (!rosterService().userRoleInSite(authenticatedUser, site).ge(Role.instructor))
		{
			rv.put(CdpStatus.CDP_STATUS, CdpStatus.accessDenied.getId());
			return rv;
		}

		Options options = evaluationService().optionsGet(site);
		options.read("options_", parameters);
		evaluationService().optionsSave(authenticatedUser, site, options);

		Integer count = cdpService().readInt(parameters.get("" + "count_categories"));
		if (count != null)
		{
			for (int i = 0; i < count; i++)
			{
				String prefix = i + "_categories_";
				Category c = null;

				Long id = cdpService().readLong(parameters.get(prefix + "id"));
				if (id > 0)
				{
					c = evaluationService().categoryGet(id);
				}

				if (c != null)
				{
					c.read(prefix, parameters);
					evaluationService().categorySave(authenticatedUser, c);
				}
			}
		}
		// add status parameter
		rv.put(CdpStatus.CDP_STATUS, CdpStatus.success.getId());

		return rv;
	}

	protected Map<String, Object> dispatchReview(HttpServletRequest req, HttpServletResponse res, Map<String, Object> parameters, String path,
			User authenticatedUser) throws ServletException, IOException
	{
		Map<String, Object> rv = new HashMap<String, Object>();

		// site
		Long siteId = cdpService().readLong(parameters.get("site"));
		if (siteId == null)
		{
			M_log.warn("dispatchReview: missing site");
			rv.put(CdpStatus.CDP_STATUS, CdpStatus.badRequest.getId());
			return rv;
		}
		Site site = siteService().wrap(siteId);

		Membership siteMembership = rosterService().getAggregateSiteRoster(site);
		Role userRole = siteMembership.findUser(authenticatedUser).getRole();

		// security: authenticatedUser must have a role of student "or higher" in the site
		if (!userRole.ge(Role.student))
		{
			rv.put(CdpStatus.CDP_STATUS, CdpStatus.accessDenied.getId());
			return rv;
		}

		// evaluation
		Long evaluationId = cdpService().readLong(parameters.get("evaluation"));
		if (evaluationId == null)
		{
			M_log.warn("dispatchReview: missing evaluation");
			rv.put(CdpStatus.CDP_STATUS, CdpStatus.badRequest.getId());
			return rv;
		}
		Evaluation evaluation = evaluationService().evaluationGet(evaluationId);
		if (evaluation == null)
		{
			M_log.warn("dispatchGetSubmissions: evaluation not found");
			rv.put(CdpStatus.CDP_STATUS, CdpStatus.badRequest.getId());
			return rv;
		}

		// make sure it is from this site, for this user
		if (!evaluation.getWorkReference().getUser().equals(authenticatedUser) || (!evaluation.getWorkReference().getItem().getSite().equals(site)))
		{
			rv.put(CdpStatus.CDP_STATUS, CdpStatus.accessDenied.getId());
			return rv;
		}

		// mark as reviewed
		evaluation.setReviewed();
		evaluationService().evaluationSave(authenticatedUser, evaluation);

		// return the updated evaluation
		rv.put("evaluation", evaluation.send());

		rv.put(CdpStatus.CDP_STATUS, CdpStatus.success.getId());

		return rv;
	}

	protected Map<String, Object> dispatchRubricAdd(HttpServletRequest req, HttpServletResponse res, Map<String, Object> parameters, String path,
			User authenticatedUser) throws ServletException, IOException
	{
		Map<String, Object> rv = new HashMap<String, Object>();

		Long siteId = cdpService().readLong(parameters.get("site"));
		if (siteId == null)
		{
			M_log.warn("dispatchRubricAdd: missing site");
			rv.put(CdpStatus.CDP_STATUS, CdpStatus.badRequest.getId());
			return rv;
		}
		Site site = siteService().wrap(siteId);

		// security: authenticatedUser must have a role of instructor "or higher" in the site
		if (!rosterService().userRoleInSite(authenticatedUser, site).ge(Role.instructor))
		{
			rv.put(CdpStatus.CDP_STATUS, CdpStatus.accessDenied.getId());
			return rv;
		}

		evaluationService().rubricAdd(authenticatedUser, site);

		// add status parameter
		rv.put(CdpStatus.CDP_STATUS, CdpStatus.success.getId());

		return rv;
	}

	protected Map<String, Object> dispatchRubricDelete(HttpServletRequest req, HttpServletResponse res, Map<String, Object> parameters, String path,
			User authenticatedUser) throws ServletException, IOException
	{
		Map<String, Object> rv = new HashMap<String, Object>();

		Long siteId = cdpService().readLong(parameters.get("site"));
		if (siteId == null)
		{
			M_log.warn("dispatchRubricDelete: missing site");
			rv.put(CdpStatus.CDP_STATUS, CdpStatus.badRequest.getId());
			return rv;
		}
		Site site = siteService().wrap(siteId);

		// security: authenticatedUser must have a role of instructor "or higher" in the site
		if (!rosterService().userRoleInSite(authenticatedUser, site).ge(Role.instructor))
		{
			rv.put(CdpStatus.CDP_STATUS, CdpStatus.accessDenied.getId());
			return rv;
		}

		// rubric ids
		List<Long> aids = cdpService().readIds(parameters.get("ids"));
		for (Long id : aids)
		{
			Rubric rubric = evaluationService().rubricGet(id);
			if (rubric != null)
			{
				// make sure it is in the site, for which we have cleared permissions
				if (rubric.getSite().equals(site))
				{
					evaluationService().rubricRemove(rubric);
				}
			}
		}
		//
		// Long id = cdpService().readLong(parameters.get("rubric"));
		// if (id == null)
		// {
		// M_log.warn("dispatchRubricDelete: missing rubric id");
		// rv.put(CdpStatus.CDP_STATUS, CdpStatus.badRequest.getId());
		// return rv;
		// }
		//
		// Rubric rubric = evaluationService().rubricGet(id);
		// if (rubric == null)
		// {
		// M_log.warn("dispatchRubricDelete: missing rubric id: " + id);
		// rv.put(CdpStatus.CDP_STATUS, CdpStatus.badRequest.getId());
		// return rv;
		// }
		//
		// evaluationService().rubricRemove(rubric);

		// add status parameter
		rv.put(CdpStatus.CDP_STATUS, CdpStatus.success.getId());

		return rv;
	}

	protected Map<String, Object> dispatchRubrics(HttpServletRequest req, HttpServletResponse res, Map<String, Object> parameters, String path,
			User authenticatedUser) throws ServletException, IOException
	{
		Map<String, Object> rv = new HashMap<String, Object>();

		Long siteId = cdpService().readLong(parameters.get("site"));
		if (siteId == null)
		{
			M_log.warn("dispatchRubricGetSite: missing site");
			rv.put(CdpStatus.CDP_STATUS, CdpStatus.badRequest.getId());
			return rv;
		}
		Site site = siteService().wrap(siteId);

		// security: authenticatedUser must have a role of guest "or higher" in the site
		if (!rosterService().userRoleInSite(authenticatedUser, site).ge(Role.guest))
		{
			rv.put(CdpStatus.CDP_STATUS, CdpStatus.accessDenied.getId());
			return rv;
		}

		List<Map<String, Object>> rubricList = new ArrayList<Map<String, Object>>();
		rv.put("rubrics", rubricList);

		List<Rubric> rubrics = evaluationService().rubricFindBySite(site);
		for (Rubric r : rubrics)
		{
			Map<String, Object> rubricMap = r.send();
			rubricList.add(rubricMap);
		}

		// add status parameter
		rv.put(CdpStatus.CDP_STATUS, CdpStatus.success.getId());

		return rv;
	}

	protected Map<String, Object> dispatchRubricSave(HttpServletRequest req, HttpServletResponse res, Map<String, Object> parameters, String path,
			User authenticatedUser) throws ServletException, IOException
	{
		Map<String, Object> rv = new HashMap<String, Object>();

		Long siteId = cdpService().readLong(parameters.get("site"));
		if (siteId == null)
		{
			M_log.warn("dispatchRubricSave: missing site");
			rv.put(CdpStatus.CDP_STATUS, CdpStatus.badRequest.getId());
			return rv;
		}
		Site site = siteService().wrap(siteId);

		// security: authenticatedUser must have a role of instructor "or higher" in the site
		if (!rosterService().userRoleInSite(authenticatedUser, site).ge(Role.instructor))
		{
			rv.put(CdpStatus.CDP_STATUS, CdpStatus.accessDenied.getId());
			return rv;
		}

		Long id = cdpService().readLong(parameters.get("rubric"));
		if (id == null)
		{
			M_log.warn("dispatchRubricSave: missing rubric id");
			rv.put(CdpStatus.CDP_STATUS, CdpStatus.badRequest.getId());
			return rv;
		}

		Rubric rubric = null;

		// for a new rubric
		if (id == -1)
		{
			rubric = evaluationService().rubricAdd(authenticatedUser, site);
		}
		else
		{
			rubric = evaluationService().rubricGet(id);
			if (rubric == null)
			{
				M_log.warn("dispatchRubricSave: missing rubric id: " + id);
				rv.put(CdpStatus.CDP_STATUS, CdpStatus.badRequest.getId());
				return rv;
			}
		}

		rubric.read("", parameters);

		evaluationService().rubricSave(authenticatedUser, rubric);

		// update the rubric
		rubric = evaluationService().rubricGet(rubric.getId());

		// send back the rubric id
		rv.put("rubric", rubric.getId());

		// add status parameter
		rv.put(CdpStatus.CDP_STATUS, CdpStatus.success.getId());

		return rv;
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
	 * @return The registered UserService.
	 */
	private UserService userService()
	{
		return (UserService) Services.get(UserService.class);
	}
}

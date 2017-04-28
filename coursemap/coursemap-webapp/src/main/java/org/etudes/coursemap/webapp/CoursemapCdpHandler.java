/**********************************************************************************
 * $URL: https://source.etudes.org/svn/serenity/trunk/coursemap/coursemap-webapp/src/main/java/org/etudes/coursemap/webapp/CoursemapCdpHandler.java $
 * $Id: CoursemapCdpHandler.java 11931 2015-10-26 03:24:08Z ggolden $
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

package org.etudes.coursemap.webapp;

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
import org.etudes.coursemap.api.CoursemapService;
import org.etudes.evaluation.api.Evaluation;
import org.etudes.evaluation.api.EvaluationService;
import org.etudes.evaluation.api.GradingItem;
import org.etudes.evaluation.api.Options;
import org.etudes.roster.api.Member;
import org.etudes.roster.api.Role;
import org.etudes.roster.api.RosterService;
import org.etudes.service.api.Services;
import org.etudes.site.api.Site;
import org.etudes.site.api.SiteService;
import org.etudes.tool.api.ToolItemType;
import org.etudes.user.api.User;
import org.etudes.user.api.UserService;

/**
 */
public class CoursemapCdpHandler implements CdpHandler
{
	/** Our log (commons). */
	private static Log M_log = LogFactory.getLog(CoursemapCdpHandler.class);

	public String getPrefix()
	{
		return "coursemap";
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

		else if (requestPath.equals("getView"))
		{
			return dispatchGetView(req, res, parameters, path, authenticatedUser);
		}

		else if (requestPath.equals("getManage"))
		{
			return dispatchGetView(req, res, parameters, path, authenticatedUser);
		}

		return null;
	}

	protected Map<String, Object> dispatchGetView(HttpServletRequest req, HttpServletResponse res, Map<String, Object> parameters, String path,
			User authenticatedUser) throws ServletException, IOException
	{
		// TODO: for now, just use the GB API

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
			itemMap.put("progress", 0); // 0 - none, 1- inprogress 2- complete (ProgressStatus)
			itemMap.put("count", 1);
			itemMap.put("type", gi.getType().getId());
			itemMap.put("scoreNA", Boolean.FALSE);
			itemMap.put("datesRO", Boolean.FALSE);
			itemMap.put("datesNA", Boolean.FALSE);
			itemMap.put("blocker", Boolean.FALSE);
			itemMap.put("blocking", Boolean.FALSE);
			itemMap.put("published", Boolean.TRUE);
			itemMap.put("active", Boolean.TRUE);
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

	/**
	 * @return The registered CdpService.
	 */
	private CdpService cdpService()
	{
		return (CdpService) Services.get(CdpService.class);
	}

	/**
	 * @return The registered CoursemapService.
	 */
	private CoursemapService coursemapService()
	{
		return (CoursemapService) Services.get(CoursemapService.class);
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

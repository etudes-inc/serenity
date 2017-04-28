/**********************************************************************************
 * $URL: https://source.etudes.org/svn/serenity/trunk/assessment/assessment-webapp/src/main/java/org/etudes/assessment/webapp/AssessmentCdpHandler.java $
 * $Id: AssessmentCdpHandler.java 12090 2015-11-18 22:38:02Z ggolden $
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

import java.io.IOException;
import java.util.ArrayList;
import java.util.Date;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import javax.servlet.ServletException;
import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;

import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;
import org.etudes.assessment.api.Answer;
import org.etudes.assessment.api.Assessment;
import org.etudes.assessment.api.AssessmentService;
import org.etudes.assessment.api.AssessmentService.SubmitStatus;
import org.etudes.assessment.api.Submission;
import org.etudes.cdp.api.CdpHandler;
import org.etudes.cdp.api.CdpService;
import org.etudes.cdp.api.CdpStatus;
import org.etudes.evaluation.api.Evaluation;
import org.etudes.evaluation.api.EvaluationService;
import org.etudes.roster.api.Member;
import org.etudes.roster.api.Membership;
import org.etudes.roster.api.Role;
import org.etudes.roster.api.RosterService;
import org.etudes.service.api.Services;
import org.etudes.site.api.Site;
import org.etudes.site.api.SiteService;
import org.etudes.user.api.User;

/**
 */
public class AssessmentCdpHandler implements CdpHandler
{
	/** Our log (commons). */
	private static Log M_log = LogFactory.getLog(AssessmentCdpHandler.class);

	public String getPrefix()
	{
		return "assessment";
	}

	public Map<String, Object> handle(HttpServletRequest req, HttpServletResponse res, Map<String, Object> parameters, String requestPath,
			String path, User authenticatedUser) throws ServletException, IOException
	{
		// if no authenticated user, we reject all requests TODO: public syllabus access
		if (authenticatedUser == null)
		{
			Map<String, Object> rv = new HashMap<String, Object>();
			rv.put(CdpStatus.CDP_STATUS, CdpStatus.notLoggedIn.getId());
			return rv;
		}

		else if (requestPath.equals("getSite"))
		{
			return dispatchGetSite(req, res, parameters, path, authenticatedUser);
		}

		else if (requestPath.equals("getSubmissions"))
		{
			return dispatchGetSubmissions(req, res, parameters, path, authenticatedUser);
		}

		else if (requestPath.equals("save"))
		{
			return dispatchSave(req, res, parameters, path, authenticatedUser);
		}

		else if (requestPath.equals("remove"))
		{
			return dispatchRemove(req, res, parameters, path, authenticatedUser);
		}

		else if (requestPath.equals("publish"))
		{
			return dispatchPublish(req, res, parameters, path, authenticatedUser);
		}

		else if (requestPath.equals("unpublish"))
		{
			return dispatchUnpublish(req, res, parameters, path, authenticatedUser);
		}

		else if (requestPath.equals("enter"))
		{
			return dispatchEnter(req, res, parameters, path, authenticatedUser);
		}

		else if (requestPath.equals("finish"))
		{
			return dispatchFinish(req, res, parameters, path, authenticatedUser);
		}

		else if (requestPath.equals("finish"))
		{
			return dispatchFinish(req, res, parameters, path, authenticatedUser);
		}

		else if (requestPath.equals("getAnswer"))
		{
			return dispatchGetAnswer(req, res, parameters, path, authenticatedUser);
		}

		else if (requestPath.equals("saveAnswer"))
		{
			return dispatchSaveAnswer(req, res, parameters, path, authenticatedUser);
		}

		else if (requestPath.equals("evaluate"))
		{
			return dispatchEvaluate(req, res, parameters, path, authenticatedUser);
		}

		else if (requestPath.equals("archive"))
		{
			return dispatchArchive(req, res, parameters, path, authenticatedUser);
		}

		else if (requestPath.equals("export"))
		{
			return dispatchExport(req, res, parameters, path, authenticatedUser);
		}

		else if (requestPath.equals("saveManage"))
		{
			return dispatchSaveManage(req, res, parameters, path, authenticatedUser);
		}

		else if (requestPath.equals("saveGradeAssessment"))
		{
			return dispatchSaveGradeAssessment(req, res, parameters, path, authenticatedUser);
		}

		else if (requestPath.equals("release"))
		{
			return dispatchRelease(req, res, parameters, path, authenticatedUser);
		}

		else if (requestPath.equals("review"))
		{
			return dispatchReview(req, res, parameters, path, authenticatedUser);
		}

		return null;
	}

	protected Map<String, Object> dispatchArchive(HttpServletRequest req, HttpServletResponse res, Map<String, Object> parameters, String path,
			User authenticatedUser) throws ServletException, IOException
	{
		Map<String, Object> rv = new HashMap<String, Object>();

		// site
		Long siteId = cdpService().readLong(parameters.get("site"));
		if (siteId == null)
		{
			M_log.warn("dispatchArchive: missing site");
			rv.put(CdpStatus.CDP_STATUS, CdpStatus.badRequest.getId());
			return rv;
		}
		Site site = siteService().wrap(siteId);

		Role userRole = rosterService().userRoleInSite(authenticatedUser, site);

		// security: authenticatedUser must have a role of instructor "or higher" in the site TODO: TA?
		if (!userRole.ge(Role.instructor))
		{
			rv.put(CdpStatus.CDP_STATUS, CdpStatus.accessDenied.getId());
			return rv;
		}

		// assessments
		List<Long> aids = cdpService().readIds(parameters.get("ids"));
		for (Long aid : aids)
		{
			Assessment assessment = assessmentService().assessmentGet(aid); // TODO: check?
			if (assessment != null)
			{
				// make sure it is in the site, for which we have cleared permissions
				if (assessment.getSite().equals(site))
				{
					// TODO:
					// assessment.setPublished(Boolean.TRUE);
					// assessmentService().assessmentSave(authenticatedUser, assessment);
				}
			}
		}

		// add status parameter
		rv.put(CdpStatus.CDP_STATUS, CdpStatus.success.getId());

		return rv;
	}

	protected Map<String, Object> dispatchEnter(HttpServletRequest req, HttpServletResponse res, Map<String, Object> parameters, String path,
			User authenticatedUser) throws ServletException, IOException
	{
		Map<String, Object> rv = new HashMap<String, Object>();

		// site
		Long siteId = cdpService().readLong(parameters.get("site"));
		if (siteId == null)
		{
			M_log.warn("dispatchGet: missing site");
			rv.put(CdpStatus.CDP_STATUS, CdpStatus.badRequest.getId());
			return rv;
		}
		Site site = siteService().wrap(siteId);

		Role userRole = rosterService().userRoleInSite(authenticatedUser, site);

		// security: authenticatedUser must have a role of student
		if (userRole != Role.student)
		{
			rv.put(CdpStatus.CDP_STATUS, CdpStatus.accessDenied.getId());
			return rv;
		}

		// assessment
		Long assessmentId = cdpService().readLong(parameters.get("assessment"));
		if (assessmentId == null)
		{
			M_log.warn("dispatchGet: missing assessment");
			rv.put(CdpStatus.CDP_STATUS, CdpStatus.badRequest.getId());
			return rv;
		}
		Assessment assessment = assessmentService().assessmentGet(assessmentId); // TODO: wrap?
		if (assessment == null)
		{
			M_log.warn("dispatchGet: missing assessment id: " + assessmentId);
			rv.put(CdpStatus.CDP_STATUS, CdpStatus.badRequest.getId());
			return rv;
		}

		// TODO:
		Submission s = assessmentService().submissionStart(authenticatedUser, assessment);

		rv.put("submission", s.send(null, null));

		// add status parameter
		rv.put(CdpStatus.CDP_STATUS, CdpStatus.success.getId());

		return rv;
	}

	protected Map<String, Object> dispatchEvaluate(HttpServletRequest req, HttpServletResponse res, Map<String, Object> parameters, String path,
			User authenticatedUser) throws ServletException, IOException
	{
		Map<String, Object> rv = new HashMap<String, Object>();

		// site
		Long siteId = cdpService().readLong(parameters.get("site"));
		if (siteId == null)
		{
			M_log.warn("dispatchEvaluate: missing site");
			rv.put(CdpStatus.CDP_STATUS, CdpStatus.badRequest.getId());
			return rv;
		}
		Site site = siteService().wrap(siteId);

		Role userRole = rosterService().userRoleInSite(authenticatedUser, site);

		// security: authenticatedUser must have a role of instructor "or higher" in the site TODO: TA?
		if (!userRole.ge(Role.instructor))
		{
			rv.put(CdpStatus.CDP_STATUS, CdpStatus.accessDenied.getId());
			return rv;
		}

		// submission
		Long submissionId = cdpService().readLong(parameters.get("submission"));
		if (submissionId == null)
		{
			M_log.warn("dispatchEvaluate: missing submission");
			rv.put(CdpStatus.CDP_STATUS, CdpStatus.badRequest.getId());
			return rv;
		}
		Submission submission = assessmentService().submissionGet(submissionId);
		if (submission == null)
		{
			M_log.warn("dispatchEvaluate: missing assessment id: " + submissionId);
			rv.put(CdpStatus.CDP_STATUS, CdpStatus.badRequest.getId());
			return rv;
		}

		List<Evaluation> evaluation = evaluationService().evaluationFindByWork(submission.getWorkReference(), Evaluation.Type.official);
		if (evaluation.isEmpty())
		{
			evaluation.add(evaluationService().evaluationAdd(authenticatedUser, submission.getWorkReference(), Evaluation.Type.official));
		}
		Evaluation e = evaluation.get(0);
		e.read("evaluation_", parameters);

		// save
		evaluationService().evaluationSave(authenticatedUser, e);

		// get the evaluation
		e = evaluationService().evaluationGet(e.getId());

		rv.put("evaluation", e.send());

		// add status parameter
		rv.put(CdpStatus.CDP_STATUS, CdpStatus.success.getId());

		return rv;
	}

	protected Map<String, Object> dispatchExport(HttpServletRequest req, HttpServletResponse res, Map<String, Object> parameters, String path,
			User authenticatedUser) throws ServletException, IOException
	{
		Map<String, Object> rv = new HashMap<String, Object>();

		// site
		Long siteId = cdpService().readLong(parameters.get("site"));
		if (siteId == null)
		{
			M_log.warn("dispatchArchive: missing site");
			rv.put(CdpStatus.CDP_STATUS, CdpStatus.badRequest.getId());
			return rv;
		}
		Site site = siteService().wrap(siteId);

		Role userRole = rosterService().userRoleInSite(authenticatedUser, site);

		// security: authenticatedUser must have a role of instructor "or higher" in the site TODO: TA?
		if (!userRole.ge(Role.instructor))
		{
			rv.put(CdpStatus.CDP_STATUS, CdpStatus.accessDenied.getId());
			return rv;
		}

		// assessments
		List<Long> aids = cdpService().readIds(parameters.get("ids"));
		for (Long aid : aids)
		{
			Assessment assessment = assessmentService().assessmentGet(aid); // TODO: check?
			if (assessment != null)
			{
				// make sure it is in the site, for which we have cleared permissions
				if (assessment.getSite().equals(site))
				{
					// TODO:
					// assessment.setPublished(Boolean.TRUE);
					// assessmentService().assessmentSave(authenticatedUser, assessment);
				}
			}
		}

		// add status parameter
		rv.put(CdpStatus.CDP_STATUS, CdpStatus.success.getId());

		return rv;
	}

	protected Map<String, Object> dispatchFinish(HttpServletRequest req, HttpServletResponse res, Map<String, Object> parameters, String path,
			User authenticatedUser) throws ServletException, IOException
	{
		Map<String, Object> rv = new HashMap<String, Object>();

		// site
		Long siteId = cdpService().readLong(parameters.get("site"));
		if (siteId == null)
		{
			M_log.warn("dispatchFinish: missing site");
			rv.put(CdpStatus.CDP_STATUS, CdpStatus.badRequest.getId());
			return rv;
		}
		Site site = siteService().wrap(siteId);

		Role userRole = rosterService().userRoleInSite(authenticatedUser, site);

		// security: authenticatedUser must have a role of student
		if (userRole != Role.student)
		{
			rv.put(CdpStatus.CDP_STATUS, CdpStatus.accessDenied.getId());
			return rv;
		}

		// assessment
		Long assessmentId = cdpService().readLong(parameters.get("assessment"));
		if (assessmentId == null)
		{
			M_log.warn("dispatchFinish: missing assessment");
			rv.put(CdpStatus.CDP_STATUS, CdpStatus.badRequest.getId());
			return rv;
		}
		Assessment assessment = assessmentService().assessmentGet(assessmentId); // TODO: check?
		if (assessment == null)
		{
			M_log.warn("dispatchFinish: missing assessment id: " + assessmentId);
			rv.put(CdpStatus.CDP_STATUS, CdpStatus.badRequest.getId());
			return rv;
		}

		// submission
		Long submissionId = cdpService().readLong(parameters.get("submission"));
		if (submissionId == null)
		{
			M_log.warn("dispatchFinish: missing submission");
			rv.put(CdpStatus.CDP_STATUS, CdpStatus.badRequest.getId());
			return rv;
		}
		Submission submission = assessmentService().submissionGet(submissionId);
		if (submission == null)
		{
			M_log.warn("dispatchFinish: missing submission id: " + submissionId);
			rv.put(CdpStatus.CDP_STATUS, CdpStatus.badRequest.getId());
			return rv;
		}

		// answer
		Long answerId = cdpService().readLong(parameters.get("answer"));
		if (answerId == null)
		{
			M_log.warn("dispatchFinish: missing answer");
			rv.put(CdpStatus.CDP_STATUS, CdpStatus.badRequest.getId());
			return rv;
		}
		Answer answer = assessmentService().answerGet(answerId);
		if (answer == null)
		{
			M_log.warn("dispatchFinish: missing answer id: " + answerId);
			rv.put(CdpStatus.CDP_STATUS, CdpStatus.badRequest.getId());
			return rv;
		}

		// save this answer
		answer.read("", parameters);
		assessmentService().answerSave(authenticatedUser, answer);

		// and finish the submission
		assessmentService().submissionFinish(submission);

		// add status parameter
		rv.put(CdpStatus.CDP_STATUS, CdpStatus.success.getId());

		return rv;
	}

	protected Map<String, Object> dispatchGetAnswer(HttpServletRequest req, HttpServletResponse res, Map<String, Object> parameters, String path,
			User authenticatedUser) throws ServletException, IOException
	{
		Map<String, Object> rv = new HashMap<String, Object>();

		// site
		Long siteId = cdpService().readLong(parameters.get("site"));
		if (siteId == null)
		{
			M_log.warn("dispatchGetAnswer: missing site");
			rv.put(CdpStatus.CDP_STATUS, CdpStatus.badRequest.getId());
			return rv;
		}
		Site site = siteService().wrap(siteId);

		Role userRole = rosterService().userRoleInSite(authenticatedUser, site);

		// security: authenticatedUser must have a role of student
		if (userRole != Role.student)
		{
			rv.put(CdpStatus.CDP_STATUS, CdpStatus.accessDenied.getId());
			return rv;
		}

		// assessment
		Long assessmentId = cdpService().readLong(parameters.get("assessment"));
		if (assessmentId == null)
		{
			M_log.warn("dispatchGetAnswer: missing assessment");
			rv.put(CdpStatus.CDP_STATUS, CdpStatus.badRequest.getId());
			return rv;
		}
		Assessment assessment = assessmentService().assessmentGet(assessmentId); // TODO: check?
		if (assessment == null)
		{
			M_log.warn("dispatchGetAnswer: missing assessment id: " + assessmentId);
			rv.put(CdpStatus.CDP_STATUS, CdpStatus.badRequest.getId());
			return rv;
		}

		// submission
		Long submissionId = cdpService().readLong(parameters.get("submission"));
		if (submissionId == null)
		{
			M_log.warn("dispatchGetAnswer: missing submission");
			rv.put(CdpStatus.CDP_STATUS, CdpStatus.badRequest.getId());
			return rv;
		}
		Submission submission = assessmentService().submissionGet(submissionId);
		if (submission == null)
		{
			M_log.warn("dispatchGetAnswer: missing submission id: " + submissionId);
			rv.put(CdpStatus.CDP_STATUS, CdpStatus.badRequest.getId());
			return rv;
		}

		// answer
		Long answerId = cdpService().readLong(parameters.get("answer"));
		if (answerId == null)
		{
			M_log.warn("dispatchGetAnswer: missing answer");
			rv.put(CdpStatus.CDP_STATUS, CdpStatus.badRequest.getId());
			return rv;
		}
		Answer answer = assessmentService().answerGet(answerId);
		if (answer == null)
		{
			M_log.warn("dispatchGetAnswer: missing answer id: " + answerId);
			rv.put(CdpStatus.CDP_STATUS, CdpStatus.badRequest.getId());
			return rv;
		}

		rv.put("answer", answer.send());

		// add status parameter
		rv.put(CdpStatus.CDP_STATUS, CdpStatus.success.getId());

		return rv;
	}

	protected Map<String, Object> dispatchGetSite(HttpServletRequest req, HttpServletResponse res, Map<String, Object> parameters, String path,
			User authenticatedUser) throws ServletException, IOException
	{
		Map<String, Object> rv = new HashMap<String, Object>();

		// site
		Long siteId = cdpService().readLong(parameters.get("site"));
		if (siteId == null)
		{
			M_log.warn("dispatchGet: missing site");
			rv.put(CdpStatus.CDP_STATUS, CdpStatus.badRequest.getId());
			return rv;
		}
		Site site = siteService().wrap(siteId);

		Role userRole = rosterService().userRoleInSite(authenticatedUser, site);
		boolean maySubmit = userRole.equals(Role.student);
		boolean mayTestdrive = userRole.equals(Role.instructor);

		// security: authenticatedUser must have a role of guest "or higher" in the site TODO: ???
		if (!userRole.ge(Role.guest))
		{
			rv.put(CdpStatus.CDP_STATUS, CdpStatus.accessDenied.getId());
			return rv;
		}

		List<Assessment> assessments = assessmentService().assessmentFindBySite(site);

		// for submitters, find the submissions, too
		List<Submission> submissions = null;
		List<Evaluation> evaluations = null;
		if (maySubmit)
		{
			submissions = assessmentService().submissionFindBySite(site, authenticatedUser);
			evaluations = evaluationService().evaluationFindBySite(site, Evaluation.Type.official, authenticatedUser);
		}

		List<Map<String, Object>> assessmentList = new ArrayList<Map<String, Object>>();
		rv.put("assessments", assessmentList);

		for (Assessment a : assessments)
		{
			// for submitters, skip unpublished assessments
			if (maySubmit && !a.getPublished()) continue;

			Map<String, Object> assessmentMap = a.send();
			assessmentList.add(assessmentMap);

			// for submitters, include all released or in-progress submission, for this assessment, with their evaluations
			if (maySubmit)
			{
				List<Map<String, Object>> submissionList = new ArrayList<Map<String, Object>>();
				assessmentMap.put("submissions", submissionList);

				List<Submission> userAssessmentSubmissions = assessmentService().submissionFindByUserAssessment(submissions, authenticatedUser, a);
				Submission inProgress = null;
				Submission latest = null;
				for (Submission s : userAssessmentSubmissions)
				{
					if (s.isInProgress() || s.isComplete())
					{
						if (s.isInProgress()) inProgress = s;

						// TODO: actual latest - are submissions sorted by start?
						if (s.isComplete()) latest = s;

						Evaluation e = null;
						if (s.isComplete())
						{
							List<Evaluation> es = evaluationService().evaluationFindByWork(evaluations, s.getWorkReference());
							if ((es != null) && (es.size() > 0)) e = es.get(0);

						}
						submissionList.add(s.send(null, e));
					}
				}

				// find the best evaluated submission for this assessment for this user
				Evaluation best = evaluationService().evaluationFindBestByItemUser(evaluations, a.getReference(), authenticatedUser);
				if (best != null) assessmentMap.put("submissionBest", best.getWorkReference().getWorkId());

				// latest complete submission
				if (latest != null) assessmentMap.put("submissionLatest", latest.getWorkReference().getWorkId());

				// is one in-progress?
				if (inProgress != null) assessmentMap.put("submissionInProgress", inProgress.getWorkReference().getWorkId());

				assessmentMap.put("submitStatus", assessmentService().getSubmitStatus(submissions, authenticatedUser, a).getCode());
			}
			else if (mayTestdrive)
			{
				assessmentMap.put("submitStatus", SubmitStatus.testdrive.getCode());
			}

			if (!maySubmit)
			{
				boolean needsGrading = false;

				List<Submission> allSubmissions = assessmentService().submissionFindByAssessment(a);
				List<Evaluation> allEvaluations = evaluationService().evaluationFindByItem(a.getReference(), Evaluation.Type.official);
				for (Submission s : allSubmissions)
				{
					List<Evaluation> es = evaluationService().evaluationFindByWork(allEvaluations, s.getWorkReference());
					Evaluation e = null;
					if ((es != null) && (es.size() > 0)) e = es.get(0);
					if (e != null)
					{
						if (!e.getEvaluated() && (!e.getReleased()))
						{
							needsGrading = true;
							break;
						}
					}
				}

				assessmentMap.put("needsGrading", Boolean.valueOf(needsGrading));
				assessmentMap.put("live", Boolean.valueOf(!allSubmissions.isEmpty()));
			}
		}

		rv.put("fs", Integer.valueOf(9)); // 0 - homepage, 1 - CHS/resources, 9 - serenity

		// add status parameter
		rv.put(CdpStatus.CDP_STATUS, CdpStatus.success.getId());

		return rv;
	}

	protected Map<String, Object> dispatchGetSubmissions(HttpServletRequest req, HttpServletResponse res, Map<String, Object> parameters,
			String path, User authenticatedUser) throws ServletException, IOException
	{
		Map<String, Object> rv = new HashMap<String, Object>();

		// site
		Long siteId = cdpService().readLong(parameters.get("site"));
		if (siteId == null)
		{
			M_log.warn("dispatchGetSubmissions: missing site");
			rv.put(CdpStatus.CDP_STATUS, CdpStatus.badRequest.getId());
			return rv;
		}
		Site site = siteService().wrap(siteId);

		Membership siteMembership = rosterService().getAggregateSiteRoster(site);
		Role userRole = siteMembership.findUser(authenticatedUser).getRole();

		// security: authenticatedUser must have a role of instructor "or higher" in the site TODO: TA?
		if (!userRole.ge(Role.instructor))
		{
			rv.put(CdpStatus.CDP_STATUS, CdpStatus.accessDenied.getId());
			return rv;
		}

		// assessment
		Long assessmentId = cdpService().readLong(parameters.get("assessment"));
		if (assessmentId == null)
		{
			M_log.warn("dispatchGetSubmissions: missing assessment");
			rv.put(CdpStatus.CDP_STATUS, CdpStatus.badRequest.getId());
			return rv;
		}
		Assessment assessment = assessmentService().assessmentGet(assessmentId); // TODO: check?
		if (assessment == null)
		{
			M_log.warn("dispatchGetSubmissions: missing assessment id: " + assessmentId);
			rv.put(CdpStatus.CDP_STATUS, CdpStatus.badRequest.getId());
			return rv;
		}

		List<Map<String, Object>> submissionList = new ArrayList<Map<String, Object>>();
		rv.put("submissions", submissionList);

		// submissions to this assessment
		List<Submission> submissions = assessmentService().submissionFindByAssessment(assessment);
		List<Evaluation> evaluations = evaluationService().evaluationFindByItem(assessment.getReference(), Evaluation.Type.official);
		for (Submission s : submissions)
		{
			List<Evaluation> es = evaluationService().evaluationFindByWork(evaluations, s.getWorkReference());
			Evaluation e = null;
			if ((es != null) && (es.size() > 0)) e = es.get(0);

			Member m = siteMembership.findUser(s.getUser());
			String rosterName = (m == null) ? null : m.getRoster().getName();

			submissionList.add(s.send(rosterName, e));
		}

		// add status parameter
		rv.put(CdpStatus.CDP_STATUS, CdpStatus.success.getId());

		return rv;
	}

	protected Map<String, Object> dispatchPublish(HttpServletRequest req, HttpServletResponse res, Map<String, Object> parameters, String path,
			User authenticatedUser) throws ServletException, IOException
	{
		Map<String, Object> rv = new HashMap<String, Object>();

		// site
		Long siteId = cdpService().readLong(parameters.get("site"));
		if (siteId == null)
		{
			M_log.warn("dispatchPublish: missing site");
			rv.put(CdpStatus.CDP_STATUS, CdpStatus.badRequest.getId());
			return rv;
		}
		Site site = siteService().wrap(siteId);

		Role userRole = rosterService().userRoleInSite(authenticatedUser, site);

		// security: authenticatedUser must have a role of instructor "or higher" in the site TODO: TA?
		if (!userRole.ge(Role.instructor))
		{
			rv.put(CdpStatus.CDP_STATUS, CdpStatus.accessDenied.getId());
			return rv;
		}

		// assessments
		List<Long> aids = cdpService().readIds(parameters.get("ids"));
		for (Long aid : aids)
		{
			Assessment assessment = assessmentService().assessmentGet(aid); // TODO: check?
			if (assessment != null)
			{
				// make sure it is in the site, for which we have cleared permissions
				if (assessment.getSite().equals(site))
				{
					assessment.setPublished(Boolean.TRUE);
					assessmentService().assessmentSave(authenticatedUser, assessment);
				}
			}
		}

		// add status parameter
		rv.put(CdpStatus.CDP_STATUS, CdpStatus.success.getId());

		return rv;
	}

	protected Map<String, Object> dispatchRelease(HttpServletRequest req, HttpServletResponse res, Map<String, Object> parameters, String path,
			User authenticatedUser) throws ServletException, IOException
	{
		Map<String, Object> rv = new HashMap<String, Object>();

		// site
		Long siteId = cdpService().readLong(parameters.get("site"));
		if (siteId == null)
		{
			M_log.warn("dispatchRelease: missing site");
			rv.put(CdpStatus.CDP_STATUS, CdpStatus.badRequest.getId());
			return rv;
		}
		Site site = siteService().wrap(siteId);

		Membership siteMembership = rosterService().getAggregateSiteRoster(site);
		Role userRole = siteMembership.findUser(authenticatedUser).getRole();

		// security: authenticatedUser must have a role of instructor "or higher" in the site TODO: TA?
		if (!userRole.ge(Role.instructor))
		{
			rv.put(CdpStatus.CDP_STATUS, CdpStatus.accessDenied.getId());
			return rv;
		}

		// assessment
		Long assessmentId = cdpService().readLong(parameters.get("assessment"));
		if (assessmentId == null)
		{
			M_log.warn("dispatchRelease: missing assessment");
			rv.put(CdpStatus.CDP_STATUS, CdpStatus.badRequest.getId());
			return rv;
		}
		Assessment assessment = assessmentService().assessmentGet(assessmentId); // TODO: check?
		if (assessment == null)
		{
			M_log.warn("dispatchRelease: missing assessment id: " + assessmentId);
			rv.put(CdpStatus.CDP_STATUS, CdpStatus.badRequest.getId());
			return rv;
		}

		// criteria - "A" all or "E" evaluated
		String criteria = cdpService().readString(parameters.get("criteria"));
		// TODO:
		// try
		// {
		// submissionService().releaseSubmissions(assessment, "E".equals(criteria));
		// }
		// catch (AssessmentPermissionException e)
		// {
		// M_log.warn("dispatchRelease: releaseSubmissions: " + assessmentId + " " + e.toString());
		// }

		// add status parameter
		rv.put(CdpStatus.CDP_STATUS, CdpStatus.success.getId());

		return rv;
	}

	protected Map<String, Object> dispatchRemove(HttpServletRequest req, HttpServletResponse res, Map<String, Object> parameters, String path,
			User authenticatedUser) throws ServletException, IOException
	{
		Map<String, Object> rv = new HashMap<String, Object>();

		// site
		Long siteId = cdpService().readLong(parameters.get("site"));
		if (siteId == null)
		{
			M_log.warn("dispatchRemove: missing site");
			rv.put(CdpStatus.CDP_STATUS, CdpStatus.badRequest.getId());
			return rv;
		}
		Site site = siteService().wrap(siteId);

		Role userRole = rosterService().userRoleInSite(authenticatedUser, site);

		// security: authenticatedUser must have a role of instructor "or higher" in the site TODO: TA?
		if (!userRole.ge(Role.instructor))
		{
			rv.put(CdpStatus.CDP_STATUS, CdpStatus.accessDenied.getId());
			return rv;
		}

		// assessments
		List<Long> aids = cdpService().readIds(parameters.get("ids"));
		for (Long aid : aids)
		{
			Assessment assessment = assessmentService().assessmentGet(aid); // TODO: check?
			if (assessment != null)
			{
				// make sure it is in the site, for which we have cleared permissions
				if (assessment.getSite().equals(site))
				{
					assessmentService().assessmentRemove(assessment);
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

	protected Map<String, Object> dispatchSave(HttpServletRequest req, HttpServletResponse res, Map<String, Object> parameters, String path,
			User authenticatedUser) throws ServletException, IOException
	{
		Map<String, Object> rv = new HashMap<String, Object>();

		// site
		Long siteId = cdpService().readLong(parameters.get("site"));
		if (siteId == null)
		{
			M_log.warn("dispatchSave: missing site");
			rv.put(CdpStatus.CDP_STATUS, CdpStatus.badRequest.getId());
			return rv;
		}
		Site site = siteService().wrap(siteId);

		Role userRole = rosterService().userRoleInSite(authenticatedUser, site);

		// security: authenticatedUser must have a role of instructor "or higher" in the site TODO: TA?
		if (!userRole.ge(Role.instructor))
		{
			rv.put(CdpStatus.CDP_STATUS, CdpStatus.accessDenied.getId());
			return rv;
		}

		// assessment - null for a new one
		Long assessmentId = cdpService().readLong(parameters.get("assessment"));
		if (assessmentId == null)
		{
			M_log.warn("dispatchSave: missing assessment");
			rv.put(CdpStatus.CDP_STATUS, CdpStatus.badRequest.getId());
			return rv;
		}
		Assessment assessment = null;
		if (assessmentId < 0)
		{
			assessment = assessmentService().assessmentAdd(authenticatedUser, site);
		}
		else
		{
			assessment = assessmentService().assessmentGet(assessmentId);
			if (assessment == null)
			{
				M_log.warn("dispatchSave: missing assessment id: " + assessmentId);
				rv.put(CdpStatus.CDP_STATUS, CdpStatus.badRequest.getId());
				return rv;
			}
		}

		assessment.read("", parameters);
		assessmentService().assessmentSave(authenticatedUser, assessment);

		// report the id for the assessment just saved
		rv.put("savedId", assessment.getId());

		// add status parameter
		rv.put(CdpStatus.CDP_STATUS, CdpStatus.success.getId());

		return rv;
	}

	protected Map<String, Object> dispatchSaveAnswer(HttpServletRequest req, HttpServletResponse res, Map<String, Object> parameters, String path,
			User authenticatedUser) throws ServletException, IOException
	{
		Map<String, Object> rv = new HashMap<String, Object>();

		// site
		Long siteId = cdpService().readLong(parameters.get("site"));
		if (siteId == null)
		{
			M_log.warn("dispatchSaveAnswer: missing site");
			rv.put(CdpStatus.CDP_STATUS, CdpStatus.badRequest.getId());
			return rv;
		}
		Site site = siteService().wrap(siteId);

		Role userRole = rosterService().userRoleInSite(authenticatedUser, site);

		// security: authenticatedUser must have a role of student
		if (userRole != Role.student)
		{
			rv.put(CdpStatus.CDP_STATUS, CdpStatus.accessDenied.getId());
			return rv;
		}

		// assessment
		Long assessmentId = cdpService().readLong(parameters.get("assessment"));
		if (assessmentId == null)
		{
			M_log.warn("dispatchSaveAnswer: missing assessment");
			rv.put(CdpStatus.CDP_STATUS, CdpStatus.badRequest.getId());
			return rv;
		}
		Assessment assessment = assessmentService().assessmentGet(assessmentId); // TODO: check?
		if (assessment == null)
		{
			M_log.warn("dispatchSaveAnswer: missing assessment id: " + assessmentId);
			rv.put(CdpStatus.CDP_STATUS, CdpStatus.badRequest.getId());
			return rv;
		}

		// submission
		Long submissionId = cdpService().readLong(parameters.get("submission"));
		if (submissionId == null)
		{
			M_log.warn("dispatchSaveAnswer: missing submission");
			rv.put(CdpStatus.CDP_STATUS, CdpStatus.badRequest.getId());
			return rv;
		}
		Submission submission = assessmentService().submissionGet(submissionId);
		if (submission == null)
		{
			M_log.warn("dispatchSaveAnswer: missing submission id: " + submissionId);
			rv.put(CdpStatus.CDP_STATUS, CdpStatus.badRequest.getId());
			return rv;
		}

		// answer
		Long answerId = cdpService().readLong(parameters.get("answer"));
		if (answerId == null)
		{
			M_log.warn("dispatchSaveAnswer: missing answer");
			rv.put(CdpStatus.CDP_STATUS, CdpStatus.badRequest.getId());
			return rv;
		}
		Answer answer = assessmentService().answerGet(answerId);
		if (answer == null)
		{
			M_log.warn("dispatchSaveAnswer: missing answer id: " + answerId);
			rv.put(CdpStatus.CDP_STATUS, CdpStatus.badRequest.getId());
			return rv;
		}

		// save this answer
		answer.read("", parameters);
		assessmentService().answerSave(authenticatedUser, answer);

		// add status parameter
		rv.put(CdpStatus.CDP_STATUS, CdpStatus.success.getId());

		return rv;
	}

	protected Map<String, Object> dispatchSaveGradeAssessment(HttpServletRequest req, HttpServletResponse res, Map<String, Object> parameters,
			String path, User authenticatedUser) throws ServletException, IOException
	{
		Map<String, Object> rv = new HashMap<String, Object>();

		// site
		Long siteId = cdpService().readLong(parameters.get("site"));
		if (siteId == null)
		{
			M_log.warn("dispatchSaveGradeAssessment: missing site");
			rv.put(CdpStatus.CDP_STATUS, CdpStatus.badRequest.getId());
			return rv;
		}
		Site site = siteService().wrap(siteId);

		Role userRole = rosterService().userRoleInSite(authenticatedUser, site);

		// security: authenticatedUser must have a role of instructor "or higher" in the site TODO: TA?
		if (!userRole.ge(Role.instructor))
		{
			rv.put(CdpStatus.CDP_STATUS, CdpStatus.accessDenied.getId());
			return rv;
		}

		// assessment - null for a new one
		Long assessmentId = cdpService().readLong(parameters.get("assessment"));
		if (assessmentId == null)
		{
			M_log.warn("dispatchSaveGradeAssessment: missing assessment");
			rv.put(CdpStatus.CDP_STATUS, CdpStatus.badRequest.getId());
			return rv;
		}
		Assessment assessment = assessmentService().assessmentGet(assessmentId);
		if (assessment == null)
		{
			M_log.warn("dispatchSaveGradeAssessment: missing assessment id: " + assessmentId);
			rv.put(CdpStatus.CDP_STATUS, CdpStatus.badRequest.getId());
			return rv;
		}

		Integer count = cdpService().readInt(parameters.get("count_submissions"));
		if (count == null)
		{
			M_log.warn("dispatchSaveGradeAssessment: missing count");
			rv.put(CdpStatus.CDP_STATUS, CdpStatus.badRequest.getId());
			return rv;
		}

		for (int i = 0; i < count; i++)
		{
			String prefix = i + "_submissions_";
			String id = cdpService().readString(parameters.get(prefix + "id"));
			Boolean evaluated = cdpService().readBoolean(parameters.get(prefix + "evaluated"));
			Boolean released = cdpService().readBoolean(parameters.get(prefix + "released"));
			Float score = cdpService().readFloat(parameters.get(prefix + "score"));

			// if phantom, ... create, set final score, evaluated and released

			// if existing, set evaluated and release, update override score

			// Assessment a = assessmentService().assessmentGet(id);
			// if (a != null)
			// {
			// a.getSchedule().setOpen(open);
			// a.getSchedule().setDue(due);
			// a.getSchedule().setAllowUntil(allowUntil);
			//
			// assessmentService().assessmentSave(authenticatedUser, a);
			// }
		}

		// add status parameter
		rv.put(CdpStatus.CDP_STATUS, CdpStatus.success.getId());

		return rv;
	}

	protected Map<String, Object> dispatchSaveManage(HttpServletRequest req, HttpServletResponse res, Map<String, Object> parameters, String path,
			User authenticatedUser) throws ServletException, IOException
	{
		Map<String, Object> rv = new HashMap<String, Object>();

		// site
		Long siteId = cdpService().readLong(parameters.get("site"));
		if (siteId == null)
		{
			M_log.warn("dispatchSaveManage: missing site");
			rv.put(CdpStatus.CDP_STATUS, CdpStatus.badRequest.getId());
			return rv;
		}
		Site site = siteService().wrap(siteId);

		Role userRole = rosterService().userRoleInSite(authenticatedUser, site);

		// security: authenticatedUser must have a role of instructor "or higher" in the site TODO: TA?
		if (!userRole.ge(Role.instructor))
		{
			rv.put(CdpStatus.CDP_STATUS, CdpStatus.accessDenied.getId());
			return rv;
		}

		Integer count = cdpService().readInt(parameters.get("count_items"));
		if (count == null)
		{
			M_log.warn("dispatchSaveManage: missing count");
			rv.put(CdpStatus.CDP_STATUS, CdpStatus.badRequest.getId());
			return rv;
		}

		for (int i = 0; i < count; i++)
		{
			String prefix = i + "_items_";
			Long id = cdpService().readLong(parameters.get(prefix + "id"));
			Date open = cdpService().readDate(parameters.get(prefix + "schedule_open"));
			Date due = cdpService().readDate(parameters.get(prefix + "schedule_due"));
			Date allowUntil = cdpService().readDate(parameters.get(prefix + "schedule_allowUntil"));

			Assessment a = assessmentService().assessmentGet(id);
			if (a != null)
			{
				a.getSchedule().setOpen(open);
				a.getSchedule().setDue(due);
				a.getSchedule().setAllowUntil(allowUntil);

				assessmentService().assessmentSave(authenticatedUser, a);
			}
		}

		// add status parameter
		rv.put(CdpStatus.CDP_STATUS, CdpStatus.success.getId());

		return rv;
	}

	protected Map<String, Object> dispatchUnpublish(HttpServletRequest req, HttpServletResponse res, Map<String, Object> parameters, String path,
			User authenticatedUser) throws ServletException, IOException
	{
		Map<String, Object> rv = new HashMap<String, Object>();

		// site
		Long siteId = cdpService().readLong(parameters.get("site"));
		if (siteId == null)
		{
			M_log.warn("dispatchUnpublish: missing site");
			rv.put(CdpStatus.CDP_STATUS, CdpStatus.badRequest.getId());
			return rv;
		}
		Site site = siteService().wrap(siteId);

		Role userRole = rosterService().userRoleInSite(authenticatedUser, site);

		// security: authenticatedUser must have a role of instructor "or higher" in the site TODO: TA?
		if (!userRole.ge(Role.instructor))
		{
			rv.put(CdpStatus.CDP_STATUS, CdpStatus.accessDenied.getId());
			return rv;
		}

		// assessments
		List<Long> aids = cdpService().readIds(parameters.get("ids"));
		for (Long aid : aids)
		{
			Assessment assessment = assessmentService().assessmentGet(aid); // TODO: check?
			if (assessment != null)
			{
				// make sure it is in the site, for which we have cleared permissions
				if (assessment.getSite().equals(site))
				{
					assessment.setPublished(Boolean.FALSE);
					assessmentService().assessmentSave(authenticatedUser, assessment);
				}
			}
		}

		// add status parameter
		rv.put(CdpStatus.CDP_STATUS, CdpStatus.success.getId());

		return rv;
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
}

/**********************************************************************************
 * $URL: https://source.etudes.org/svn/serenity/trunk/assessment/assessment-webapp/src/main/java/org/etudes/assessment/webapp/AssessmentServiceImpl.java $
 * $Id: AssessmentServiceImpl.java 11930 2015-10-25 18:21:10Z ggolden $
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

import java.sql.ResultSet;
import java.sql.SQLException;
import java.util.ArrayList;
import java.util.Date;
import java.util.List;

import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;
import org.etudes.assessment.api.Answer;
import org.etudes.assessment.api.Assessment;
import org.etudes.assessment.api.Assessment.Type;
import org.etudes.assessment.api.AssessmentService;
import org.etudes.assessment.api.Question;
import org.etudes.assessment.api.Submission;
import org.etudes.entity.api.Schedule;
import org.etudes.evaluation.api.Evaluation;
import org.etudes.evaluation.api.EvaluationDesign;
import org.etudes.evaluation.api.EvaluationService;
import org.etudes.evaluation.api.GradingItem;
import org.etudes.file.api.FileService;
import org.etudes.service.api.Service;
import org.etudes.service.api.Services;
import org.etudes.site.api.Site;
import org.etudes.site.api.SiteService;
import org.etudes.sitecontent.api.Archive;
import org.etudes.sitecontent.api.Artifact;
import org.etudes.sitecontent.api.BaseDateService;
import org.etudes.sitecontent.api.DateProvider;
import org.etudes.sitecontent.api.DateRange;
import org.etudes.sitecontent.api.GradeProvider;
import org.etudes.sitecontent.api.SiteContentHandler;
import org.etudes.sitecontent.api.StudentContentHandler;
import org.etudes.sql.api.SqlService;
import org.etudes.tool.api.Tool;
import org.etudes.tool.api.ToolItemReference;
import org.etudes.tool.api.ToolItemType;
import org.etudes.user.api.User;
import org.etudes.user.api.UserService;

// TODO: delete each submission to assessment when assessment deleted
// delete each answer (and refs) to submission when submission deleted
// delete questions when site cleared

/**
 * AssessmentServiceImpl implements AssessmentService.
 */
public class AssessmentServiceImpl implements AssessmentService, Service, SiteContentHandler, DateProvider, StudentContentHandler, GradeProvider
{
	/** Our log. */
	private static Log M_log = LogFactory.getLog(AssessmentServiceImpl.class);

	/**
	 * Construct
	 */
	public AssessmentServiceImpl()
	{
		M_log.info("AssessmentServiceImpl: construct");
	}

	@Override
	public void adjustDatesByDays(Site site, int days, User adjustingUser)
	{
		List<Assessment> assessments = assessmentFindBySite(site);
		for (Assessment a : assessments)
		{
			a.getSchedule().adjustDatesByDays(days);
			assessmentSave(adjustingUser, a);
		}
	}

	@Override
	public Answer answerGet(Long id)
	{
		AnswerImpl answer = new AnswerImpl();
		answer.initId(id);
		return answerReadTx(answer);
	}

	@Override
	public void answerSave(User savedBy, final Answer answer)
	{
		if (((AnswerImpl) answer).isChanged() || answer.getId() == null)
		{
			if (((AnswerImpl) answer).isChanged())
			{
				// set date
				((AnswerImpl) answer).initAnsweredOn(new Date());

				// considered answered
				((AnswerImpl) answer).initAnswered(Boolean.TRUE);

				// deal with the content
				((AnswerImpl) answer).saveContent(savedBy);
			}

			// insert or update
			if (answer.getId() == null)
			{
				sqlService().transact(new Runnable()
				{
					@Override
					public void run()
					{
						answerInsertTx((AnswerImpl) answer);
					}
				}, "save(insert)");
			}
			else
			{
				sqlService().transact(new Runnable()
				{
					@Override
					public void run()
					{
						answerUpdateTx((AnswerImpl) answer);
					}
				}, "save(update)");
			}

			((AnswerImpl) answer).clearChanged();
		}
	}

	@Override
	public void archive(Site fromSite, Boolean authoredContentOnly, Archive toArchive)
	{
		List<Assessment> source = assessmentFindBySite(fromSite);
		for (Assessment assessment : source)
		{
			// make an artifact for the assessment
			Artifact artifact = toArchive.newArtifact(Tool.assessment, "assessment");

			artifact.getProperties().put("createdbyId", assessment.getCreatedBy().getId());
			artifact.getProperties().put("createdon", assessment.getCreatedOn());
			artifact.getProperties().put("modifiedbyId", assessment.getModifiedBy().getId());
			artifact.getProperties().put("modifiedon", assessment.getModifiedOn());
			artifact.getProperties().put("id", assessment.getId());
			artifact.getProperties().put("title", assessment.getTitle());

			toArchive.archive(artifact);
		}
	}

	@Override
	public Assessment assessmentAdd(User addedBy, Site inSite)
	{
		AssessmentImpl rv = new AssessmentImpl();
		rv.initSite(inSite);
		rv.initCreatedBy(addedBy);
		rv.initCreatedOn(new Date());
		rv.initModifiedBy(addedBy);
		rv.initModifiedOn(rv.getCreatedOn());
		assessmentSave(addedBy, rv);

		// set this after the save and the id is set
		rv.initEvaluation(evaluationService().designAdd(rv.getReference()));

		return rv;
	}

	@Override
	public Assessment assessmentCheck(Long id)
	{
		return assessmentCheckTx(id);
	}

	@Override
	public void assessmentClear(Assessment assessment)
	{
		// remove all submissions
		List<Submission> submissions = submissionFindByAssessment(assessment);
		for (Submission s : submissions)
		{
			submissionRemove(s);
		}
	}

	@Override
	public void assessmentClear(Assessment assessment, User user)
	{
		// remove this user's submissions
		List<Submission> submissions = submissionFindByUserAssessment(user, assessment);
		for (Submission s : submissions)
		{
			submissionRemove(s);
		}
	}

	@Override
	public List<Assessment> assessmentFindBySite(Site inSite)
	{
		return assessmentFindBySiteTx(inSite);
	}

	@Override
	public List<Assessment> assessmentFindByTitle(List<Assessment> assessments, String title)
	{
		List<Assessment> rv = new ArrayList<Assessment>();

		for (Assessment a : assessments)
		{
			if (a.getTitle().equalsIgnoreCase(title))
			{
				rv.add(a);
			}
		}

		return null;
	}

	@Override
	public List<Assessment> assessmentFindByTitle(Site site, String title)
	{
		return assessmentFindByTitleTx(site, title);
	}

	@Override
	public Assessment assessmentGet(Long id)
	{
		AssessmentImpl rv = new AssessmentImpl(id);
		return assessmentReadTx(rv);
	}

	@Override
	public void assessmentRefresh(Assessment assessment)
	{
		assessmentReadTx((AssessmentImpl) assessment);
	}

	@Override
	public void assessmentRemove(final Assessment assessment)
	{
		sqlService().transact(new Runnable()
		{
			@Override
			public void run()
			{
				// the submissions
				assessmentClear(assessment);

				// evaluation design
				evaluationService().designRemove(assessment.getEvaluation());

				// deal with the content - remove all our references
				fileService().removeExcept(assessment.getReference(), null);

				// the assessment
				assessmentRemoveTx(assessment);
			}
		}, "assessmentRemove");
	}

	@Override
	public void assessmentSave(User savedBy, final Assessment assessment)
	{
		if (((AssessmentImpl) assessment).isChanged() || assessment.getId() == null)
		{
			if (((AssessmentImpl) assessment).isChanged())
			{
				// set modified by/on
				((AssessmentImpl) assessment).initModifiedBy(savedBy);
				((AssessmentImpl) assessment).initModifiedOn(new Date());

				// deal with the content
				((AssessmentImpl) assessment).saveInstructions(savedBy);
			}

			// insert or update
			if (assessment.getId() == null)
			{
				sqlService().transact(new Runnable()
				{
					@Override
					public void run()
					{
						assessmentInsertTx((AssessmentImpl) assessment);
						if (assessment.getEvaluation() != null) evaluationService().designSave(assessment.getEvaluation());
					}
				}, "save(insert)");
			}
			else
			{
				sqlService().transact(new Runnable()
				{
					@Override
					public void run()
					{
						assessmentUpdateTx((AssessmentImpl) assessment);
						if (assessment.getEvaluation() != null) evaluationService().designSave(assessment.getEvaluation());
					}
				}, "save(update)");
			}

			((AssessmentImpl) assessment).clearChanged();
		}
	}

	@Override
	public Assessment assessmentWrap(Long id)
	{
		return new AssessmentImpl(id);
	}

	@Override
	public void clear(final Site site)
	{
		List<Assessment> assessments = assessmentFindBySite(site);
		for (Assessment a : assessments)
		{
			assessmentClear(a);
		}
	}

	@Override
	public void clear(final Site site, final User user)
	{
		List<Assessment> assessments = assessmentFindBySite(site);
		for (Assessment a : assessments)
		{
			assessmentClear(a, user);
		}
	}

	@Override
	public DateRange getDateRange(Site site)
	{
		// TODO: don't need a full read! Just the dates
		Date[] minMax = new Date[2];
		List<Assessment> assessments = assessmentFindBySite(site);
		for (Assessment a : assessments)
		{
			baseDateService().computeMinMax(minMax, a.getSchedule().getOpen());
			baseDateService().computeMinMax(minMax, a.getSchedule().computeClose());
		}

		if (minMax[0] == null) return null;
		return baseDateService().newDateRange(Tool.assessment, minMax);
	}

	@Override
	public GradingItem getGradingItem(ToolItemReference item)
	{
		Assessment a = assessmentGet(item.getItemId());
		if ((a == null) || (!a.getSite().equals(item.getSite()))) return null;

		GradingItem gi = new GradingItem(a.getReference(), a.getToolItemType(), a.getTitle(), a.getSchedule(), a.getEvaluation());
		return gi;
	}

	@Override
	public List<GradingItem> getGradingItems(Site site)
	{
		List<GradingItem> rv = new ArrayList<GradingItem>();

		List<Assessment> assessments = assessmentFindBySite(site);
		for (Assessment a : assessments)
		{
			if ((!a.getEvaluation().getForGrade()) || (!a.getPublished())) continue;

			GradingItem gi = new GradingItem(a.getReference(), a.getToolItemType(), a.getTitle(), a.getSchedule(), a.getEvaluation());
			rv.add(gi);
		}

		return rv;
	}

	@Override
	public SubmitStatus getSubmitStatus(List<Submission> submissions, User user, Assessment assessment)
	{
		// find submissions to this assessment
		List<Submission> subs = new ArrayList<Submission>();
		for (Submission s : submissions)
		{
			if (s.getAssessment().equals(assessment)) subs.add(s);
		}
		
		// if something for this assessment is in-progress, submitter may continue
		for (Submission s : subs)
		{
			if (s.isInProgress()) return SubmitStatus.resume;
		}

		// closed(4, "Closed"), open(3, "Open"), willOpen(1, "Will Open"), willOpenHide(2, "Hidden Until Open");

		// if assessment is not open, submitter may do nothing
		if (assessment.getSchedule().getStatus() != Schedule.Status.open) return SubmitStatus.nothing;

		// if submitter used up tries, submitter may do nothing
		if (!assessment.isTriesRemain(submissions.size())) return SubmitStatus.nothing;

		if (subs.isEmpty()) return SubmitStatus.start;

		return SubmitStatus.again;
	}

	@Override
	public void importFromArchive(Artifact fromArtifact, Boolean authoredContentOnly, Site intoSite, User importingUser)
	{
		if (fromArtifact.getType().equals("assessment"))
		{
			String title = (String) fromArtifact.getProperties().get("title");
			if (assessmentFindByTitle(intoSite, title) == null)
			{
				Assessment newAssessment = assessmentAdd(importingUser, intoSite);

				newAssessment.setTitle(title);
				// TODO: rest of assessment

				assessmentSave(importingUser, newAssessment);
			}
		}
	}

	@Override
	public void importFromSite(Site fromSite, Site toSite, User importingUser)
	{
		List<Assessment> source = assessmentFindBySite(fromSite);
		List<Assessment> dest = assessmentFindBySite(toSite);

		for (Assessment sourceAssessment : source)
		{
			// if titled assessment exists, skip on import
			if (assessmentFindByTitle(dest, sourceAssessment.getTitle()) != null) continue;

			Assessment newAssessment = assessmentAdd(importingUser, toSite);

			newAssessment.setTitle(sourceAssessment.getTitle());
			// TODO: rest of assessment

			assessmentSave(importingUser, newAssessment);
			dest.add(newAssessment);
		}
	}

	@Override
	public void purge(Site site)
	{
		List<Assessment> assessments = assessmentFindBySite(site);
		for (Assessment a : assessments)
		{
			assessmentRemove(a);
		}
	}

	@Override
	public Question questionWrap(Long id)
	{
		QuestionImpl rv = new QuestionImpl(id);

		return rv;
	}

	@Override
	public boolean start()
	{
		M_log.info("AssessmentServiceImpl: start");
		return true;
	}

	@Override
	public List<Submission> submissionFindByAssessment(Assessment assessment)
	{
		return submissionFindByAssessmenTx(assessment);
	}

	@Override
	public List<Submission> submissionFindBySite(Site inSite, User user)
	{
		return submissionFindBySiteTx(inSite, user);
	}

	@Override
	public List<Submission> submissionFindByUserAssessment(List<Submission> submissions, User user, Assessment assessment)
	{
		List<Submission> rv = new ArrayList<Submission>();
		for (Submission s : submissions)
		{
			if (s.getUser().equals(user) && (s.getAssessment().equals(assessment)))
			{
				rv.add(s);
			}
		}

		return rv;
	}

	@Override
	public List<Submission> submissionFindByUserAssessment(User user, Assessment assessment)
	{
		return submissionFindByUserAssessmenTx(user, assessment);
	}

	@Override
	public void submissionFinish(final Submission submission)
	{
		((SubmissionImpl) submission).load();
		((SubmissionImpl) submission).initFinished(new Date());
		sqlService().transact(new Runnable()
		{
			@Override
			public void run()
			{
				submissionUpdateTx(((SubmissionImpl) submission));
			}
		}, "finish");

		// create an evaluation (for auto score) (created by SYSTEM) for this submission
		/* Evaluation e = */evaluationService().evaluationAdd(userService().wrap(UserService.SYSTEM), submission.getWorkReference(),
				Evaluation.Type.official);
	}

	@Override
	public Submission submissionGet(List<Submission> submissions, Long id)
	{
		for (Submission s : submissions)
		{
			if (s.getId().equals(id)) return s;
		}
		return null;
	}

	@Override
	public Submission submissionGet(Long id)
	{
		SubmissionImpl submission = new SubmissionImpl();
		submission.initId(id);
		submissionReadTx(submission);
		return submission;
	}

	@Override
	public void submissionRefresh(Submission submission)
	{
		submissionReadTx((SubmissionImpl) submission);
	}

	@Override
	public void submissionRefreshAnswers(Submission submission)
	{
		answerReadSubmissionTx((SubmissionImpl) submission);
	}

	@Override
	public void submissionRemove(final Submission submission)
	{
		// answers
		// evaluation
		sqlService().transact(new Runnable()
		{
			@Override
			public void run()
			{
				// answers
				for (Answer answer : submission.getAnswers())
				{
					// deal with the content - remove all our references
					fileService().removeExcept(answer.getReference(), null);
				}
				answerRemoveTx(submission);

				// evaluation
				evaluationService().evaluationRemoveItem(submission.getAssessment().getReference());

				// deal with the content - remove all our references
				fileService().removeExcept(submission.getReference(), null);

				// submission
				submissionRemoveTx(submission);
			}
		}, "submissionRemove");
	}

	@Override
	public Submission submissionStart(User user, Assessment assessment)
	{
		final SubmissionImpl submission = new SubmissionImpl();
		submission.initAssessment(assessment);
		submission.initUser(user);
		submission.initStarted(new Date());

		sqlService().transact(new Runnable()
		{
			@Override
			public void run()
			{
				submissionInsertTx(submission);
			}
		}, "start");

		// create answers
		if (assessment.getType() == Assessment.Type.essay)
		{
			// one answer, question id "0"
			AnswerImpl answer = new AnswerImpl();
			answer.initSubmission(submission);
			answer.initQuestion(questionWrap(Long.valueOf(0)));

			answerSave(user, answer);

			submission.getAnswers().add(answer);
		}

		return submission;
	}

	@Override
	public Submission submissionWrap(Long id)
	{
		SubmissionImpl rv = new SubmissionImpl(id);

		return rv;
	}

	/**
	 * Transaction code for inserting an answer.
	 * 
	 * @param submission
	 *        The submission.
	 */
	protected void answerInsertTx(AnswerImpl answer)
	{
		String sql = "INSERT INTO ASMT_ANSWER (SUBMISSION, QUESTION, ANSWERED, ANSWERED_ON, ANSWER_REF, ANSWER_DATA, REVIEW)"
				+ " VALUES (?,?,?,?,?,?,?)";

		Object[] fields = new Object[7];
		int i = 0;
		fields[i++] = answer.getSubmission().getId();
		fields[i++] = answer.getQuestion().getId();
		fields[i++] = answer.isAnswered();
		fields[i++] = answer.getAnsweredOn();
		fields[i++] = answer.getContentReferenceId();
		fields[i++] = answer.getData();
		fields[i++] = answer.isMarkedForReview();

		Long id = sqlService().insert(sql, fields, "ID");
		answer.initId(id);
	}

	protected void answerReadSubmissionTx(final SubmissionImpl submission)
	{
		String sql = "SELECT ID, QUESTION, ANSWERED, ANSWERED_ON, ANSWER_REF, ANSWER_DATA, REVIEW FROM ASMT_ANSWER WHERE SUBMISSION = ?";
		Object[] fields = new Object[1];
		fields[0] = submission.getId();
		List<Answer> answers = sqlService().select(sql, fields, new SqlService.Reader<Answer>()
		{
			@Override
			public Answer read(ResultSet result)
			{
				try
				{
					AnswerImpl answer = new AnswerImpl();
					answer.initSubmission(submission);

					int i = 1;
					answer.initId(sqlService().readLong(result, i++));
					answer.initQuestion(questionWrap(sqlService().readLong(result, i++)));
					answer.initAnswered(sqlService().readBoolean(result, i++));
					answer.initAnsweredOn(sqlService().readDate(result, i++));
					answer.initContentReferenceId(sqlService().readLong(result, i++));
					answer.initData(sqlService().readString(result, i++));
					answer.initMarkedForReview(sqlService().readBoolean(result, i++));

					return answer;
				}
				catch (SQLException e)
				{
					M_log.warn("answerReadSubmissionTx: " + e);
					return null;
				}
			}
		});

		submission.initAnswers(answers);
	}

	protected Answer answerReadTx(final AnswerImpl answer)
	{
		String sql = "SELECT SUBMISSION, QUESTION, ANSWERED, ANSWERED_ON, ANSWER_REF, ANSWER_DATA, REVIEW FROM ASMT_ANSWER WHERE ID = ?";
		Object[] fields = new Object[1];
		fields[0] = answer.getId();
		List<Answer> answers = sqlService().select(sql, fields, new SqlService.Reader<Answer>()
		{
			@Override
			public Answer read(ResultSet result)
			{
				try
				{
					int i = 1;
					answer.initSubmission(submissionWrap(sqlService().readLong(result, i++)));
					answer.initQuestion(questionWrap(sqlService().readLong(result, i++)));
					answer.initAnswered(sqlService().readBoolean(result, i++));
					answer.initAnsweredOn(sqlService().readDate(result, i++));
					answer.initContentReferenceId(sqlService().readLong(result, i++));
					answer.initData(sqlService().readString(result, i++));
					answer.initMarkedForReview(sqlService().readBoolean(result, i++));

					return answer;
				}
				catch (SQLException e)
				{
					M_log.warn("answerReadTx: " + e);
					return null;
				}
			}
		});

		if (answers.isEmpty()) return null;
		return answer;
	}

	/**
	 * Transaction code for removing all answers for a submission
	 * 
	 * @param submission
	 *        The submission.
	 */
	protected void answerRemoveTx(Submission submission)
	{
		String sql = "DELETE FROM ASMT_ANSWER WHERE SUBMISSION = ?";
		Object[] fields = new Object[1];
		fields[0] = submission.getId();
		sqlService().update(sql, fields);
	}

	/**
	 * Transaction code for updating an existing answer.
	 * 
	 * @param answer
	 *        The answer.
	 */
	protected void answerUpdateTx(AnswerImpl answer)
	{
		String sql = "UPDATE ASMT_ANSWER SET SUBMISSION=?, QUESTION=?, ANSWERED=?, ANSWERED_ON=?, ANSWER_REF=?, ANSWER_DATA=?, REVIEW=? WHERE ID=?";

		Object[] fields = new Object[8];
		int i = 0;
		fields[i++] = answer.getSubmission().getId();
		fields[i++] = answer.getQuestion().getId();
		fields[i++] = answer.isAnswered();
		fields[i++] = answer.getAnsweredOn();
		fields[i++] = answer.getContentReferenceId();
		fields[i++] = answer.getData();
		fields[i++] = answer.isMarkedForReview();

		fields[i++] = answer.getId();

		sqlService().update(sql, fields);
	}

	/**
	 * Transaction code for checking an assessment.
	 * 
	 * @param id
	 *        The assessment id.
	 * @return a wrapped Assessment object if found, null if not.
	 */
	protected AssessmentImpl assessmentCheckTx(final Long id)
	{
		String sql = "SELECT ID FROM ASMT_ASSESSMENT WHERE ID = ?";
		Object[] fields = new Object[1];
		fields[0] = id;
		List<AssessmentImpl> rv = sqlService().select(sql, fields, new SqlService.Reader<AssessmentImpl>()
		{
			@Override
			public AssessmentImpl read(ResultSet result)
			{
				AssessmentImpl assessment = new AssessmentImpl(id);
				return assessment;
			}
		});

		return rv.isEmpty() ? null : rv.get(0);
	}

	/**
	 * Transaction code for reading the assessments for a site.
	 * 
	 * @param siteId
	 *        the site id.
	 */
	protected List<Assessment> assessmentFindBySiteTx(final Site site)
	{
		String sql = "SELECT ID, ASMTTYPE, TITLE, OPEN, DUE, ALLOW, HIDE, PUBLISHED, INSTRUCTIONS, CREATED_BY, CREATED_ON, MODIFIED_BY, MODIFIED_ON FROM ASMT_ASSESSMENT WHERE SITE = ?";
		Object[] fields = new Object[1];
		fields[0] = site.getId();
		List<Assessment> rv = sqlService().select(sql, fields, new SqlService.Reader<Assessment>()
		{
			@Override
			public Assessment read(ResultSet result)
			{
				AssessmentImpl assessment = new AssessmentImpl();
				assessment.initSite(site);
				try
				{
					int i = 1;
					assessment.initId(sqlService().readLong(result, i++));
					assessment.initType(Type.fromCode(sqlService().readString(result, i++)));
					assessment.initTitle(sqlService().readString(result, i++));
					Schedule s = new Schedule();
					s.initOpen(sqlService().readDate(result, i++));
					s.initDue(sqlService().readDate(result, i++));
					s.initAllowUntil(sqlService().readDate(result, i++));
					s.initHideUntilOpen(sqlService().readBoolean(result, i++));
					assessment.initSchedule(s);
					assessment.initPublished(sqlService().readBoolean(result, i++));
					assessment.initInstructionsReferenceId(sqlService().readLong(result, i++));
					assessment.initCreatedBy(userService().wrap(sqlService().readLong(result, i++)));
					assessment.initCreatedOn(sqlService().readDate(result, i++));
					assessment.initModifiedBy(userService().wrap(sqlService().readLong(result, i++)));
					assessment.initModifiedOn(sqlService().readDate(result, i++));

					// TODO: efficiency, one read
					EvaluationDesign design = evaluationService().designGet(assessment.getReference());
					assessment.initEvaluation(design);

					assessment.setLoaded();

					return assessment;
				}
				catch (SQLException e)
				{
					M_log.warn("assessmentFindBySiteTx: " + e);
					return null;
				}
			}
		});

		return rv;
	}

	/**
	 * Transaction code for reading the assessments for a site with a given title.
	 * 
	 * @param siteId
	 *        the site id.
	 * @param title
	 *        The title.
	 */
	protected List<Assessment> assessmentFindByTitleTx(final Site site, String title)
	{
		String sql = "SELECT ID, ASMTTYPE, TITLE, OPEN, DUE, ALLOW, HIDE, PUBLISHED, INSTRUCTIONS, CREATED_BY, CREATED_ON, MODIFIED_BY, MODIFIED_ON FROM ASMT_ASSESSMENT WHERE SITE = ? AND TITLE = ?";
		Object[] fields = new Object[2];
		fields[0] = site.getId();
		fields[1] = title;
		List<Assessment> rv = sqlService().select(sql, fields, new SqlService.Reader<Assessment>()
		{
			@Override
			public Assessment read(ResultSet result)
			{
				AssessmentImpl assessment = new AssessmentImpl();
				assessment.initSite(site);
				try
				{
					int i = 1;
					assessment.initId(sqlService().readLong(result, i++));
					assessment.initType(Type.fromCode(sqlService().readString(result, i++)));
					assessment.initTitle(sqlService().readString(result, i++));
					Schedule s = new Schedule();
					s.initOpen(sqlService().readDate(result, i++));
					s.initDue(sqlService().readDate(result, i++));
					s.initAllowUntil(sqlService().readDate(result, i++));
					s.initHideUntilOpen(sqlService().readBoolean(result, i++));
					assessment.initSchedule(s);
					assessment.initPublished(sqlService().readBoolean(result, i++));
					assessment.initInstructionsReferenceId(sqlService().readLong(result, i++));
					assessment.initCreatedBy(userService().wrap(sqlService().readLong(result, i++)));
					assessment.initCreatedOn(sqlService().readDate(result, i++));
					assessment.initModifiedBy(userService().wrap(sqlService().readLong(result, i++)));
					assessment.initModifiedOn(sqlService().readDate(result, i++));

					// TODO: efficiency, one read
					EvaluationDesign design = evaluationService().designGet(assessment.getReference());
					assessment.initEvaluation(design);

					return assessment;
				}
				catch (SQLException e)
				{
					M_log.warn("assessmentFindByTitleTx: " + e);
					return null;
				}
			}
		});

		return rv;
	}

	/**
	 * Transaction code for inserting an assessment.
	 * 
	 * @param assessment
	 *        The assessment.
	 */
	protected void assessmentInsertTx(AssessmentImpl assessment)
	{
		String sql = "INSERT INTO ASMT_ASSESSMENT (SITE, ASMTTYPE, TITLE, OPEN, DUE, ALLOW, HIDE, PUBLISHED, INSTRUCTIONS, CREATED_BY, CREATED_ON, MODIFIED_BY, MODIFIED_ON)"
				+ " VALUES (?,?,?,?,?,?,?,?,?,?,?,?,?)";

		Object[] fields = new Object[13];
		int i = 0;
		fields[i++] = assessment.getSite().getId();
		fields[i++] = assessment.getType().getCode();
		fields[i++] = assessment.getTitle();
		fields[i++] = assessment.getSchedule().getOpen();
		fields[i++] = assessment.getSchedule().getDue();
		fields[i++] = assessment.getSchedule().getAllowUntil();
		fields[i++] = assessment.getSchedule().getHideUntilOpen();
		fields[i++] = assessment.getPublished();
		fields[i++] = assessment.getInstructionsReferenceId();
		fields[i++] = assessment.getCreatedBy().getId();
		fields[i++] = assessment.getCreatedOn();
		fields[i++] = assessment.getModifiedBy().getId();
		fields[i++] = assessment.getModifiedOn();

		Long id = sqlService().insert(sql, fields, "ID");
		assessment.initId(id);
	}

	/**
	 * Transaction code for reading an assessment
	 * 
	 * @param asssessment
	 *        The assessment to read (id is set, the rest is read).
	 */
	protected Assessment assessmentReadTx(final AssessmentImpl assessment)
	{
		String sql = "SELECT SITE, ASMTTYPE, TITLE, OPEN, DUE, ALLOW, HIDE, PUBLISHED, INSTRUCTIONS, CREATED_BY, CREATED_ON, MODIFIED_BY, MODIFIED_ON FROM ASMT_ASSESSMENT WHERE ID = ?";
		Object[] fields = new Object[1];
		fields[0] = assessment.getId();
		List<Assessment> rv = sqlService().select(sql, fields, new SqlService.Reader<Assessment>()
		{
			@Override
			public Assessment read(ResultSet result)
			{
				try
				{
					int i = 1;
					assessment.initSite(siteService().wrap(sqlService().readLong(result, i++)));
					assessment.initType(Type.fromCode(sqlService().readString(result, i++)));
					assessment.initTitle(sqlService().readString(result, i++));
					Schedule s = new Schedule();
					s.initOpen(sqlService().readDate(result, i++));
					s.initDue(sqlService().readDate(result, i++));
					s.initAllowUntil(sqlService().readDate(result, i++));
					s.initHideUntilOpen(sqlService().readBoolean(result, i++));
					assessment.initSchedule(s);
					assessment.initPublished(sqlService().readBoolean(result, i++));
					assessment.initInstructionsReferenceId(sqlService().readLong(result, i++));
					assessment.initCreatedBy(userService().wrap(sqlService().readLong(result, i++)));
					assessment.initCreatedOn(sqlService().readDate(result, i++));
					assessment.initModifiedBy(userService().wrap(sqlService().readLong(result, i++)));
					assessment.initModifiedOn(sqlService().readDate(result, i++));

					assessment.setLoaded();

					// TODO: efficiency, one read
					EvaluationDesign design = evaluationService().designGet(assessment.getReference());
					assessment.initEvaluation(design);

					return assessment;
				}
				catch (SQLException e)
				{
					M_log.warn("assessmentReadTx: " + e);
					return null;
				}
			}
		});

		if (rv.isEmpty()) return null;
		return assessment;
	}

	/**
	 * Transaction code for removing a assessment.
	 * 
	 * @param assessment
	 *        The assessment.
	 */
	protected void assessmentRemoveTx(Assessment assessment)
	{
		String sql = "DELETE FROM ASMT_ASSESSMENT WHERE ID = ?";
		Object[] fields = new Object[1];
		fields[0] = assessment.getId();
		sqlService().update(sql, fields);
		((AssessmentImpl) assessment).initId(null);
	}

	/**
	 * Transaction code for updating an existing assessment.
	 * 
	 * @param assessment
	 *        The assessment.
	 */
	protected void assessmentUpdateTx(AssessmentImpl assessment)
	{
		String sql = "UPDATE ASMT_ASSESSMENT SET SITE=?, ASMTTYPE=?, TITLE=?, OPEN=?, DUE=?, ALLOW=?, HIDE=?, PUBLISHED=?, INSTRUCTIONS=?, CREATED_BY=?, CREATED_ON=?, MODIFIED_BY=?, MODIFIED_ON=? WHERE ID=?";

		Object[] fields = new Object[14];
		int i = 0;
		fields[i++] = assessment.getSite().getId();
		fields[i++] = assessment.getType().getCode();
		fields[i++] = assessment.getTitle();
		fields[i++] = assessment.getSchedule().getOpen();
		fields[i++] = assessment.getSchedule().getDue();
		fields[i++] = assessment.getSchedule().getAllowUntil();
		fields[i++] = assessment.getSchedule().getHideUntilOpen();
		fields[i++] = assessment.getPublished();
		fields[i++] = assessment.getInstructionsReferenceId();
		fields[i++] = assessment.getCreatedBy().getId();
		fields[i++] = assessment.getCreatedOn();
		fields[i++] = assessment.getModifiedBy().getId();
		fields[i++] = assessment.getModifiedOn();

		fields[i++] = assessment.getId();

		sqlService().update(sql, fields);
	}

	/**
	 * Transaction code for reading the submissions for this assessment.
	 * 
	 * @param site
	 *        The site.
	 * @param user
	 *        The user. return The List of Submission, may be empty.
	 */
	protected List<Submission> submissionFindByAssessmenTx(final Assessment assessment)
	{
		String sql = "SELECT ID, USER, STARTED, FINISHED FROM ASMT_SUBMISSION WHERE ASSESSMENT = ?";
		Object[] fields = new Object[1];
		fields[0] = assessment.getId();
		List<Submission> rv = sqlService().select(sql, fields, new SqlService.Reader<Submission>()
		{
			@Override
			public Submission read(ResultSet result)
			{
				try
				{
					SubmissionImpl submission = new SubmissionImpl();
					submission.initAssessment(assessment);

					int i = 1;
					submission.initId(sqlService().readLong(result, i++));
					submission.initUser(userService().wrap(sqlService().readLong(result, i++)));
					submission.initStarted(sqlService().readDate(result, i++));
					submission.initFinished(sqlService().readDate(result, i++));
					submission.clearChanged();
					submission.setLoaded();

					return submission;
				}
				catch (SQLException e)
				{
					M_log.warn("submissionFindByAssessmenTx: " + e);
					return null;
				}
			}
		});

		return rv;
	}

	/**
	 * Transaction code for reading the submissions for this user in this site.
	 * 
	 * @param site
	 *        The site.
	 * @param user
	 *        The user. return The List of Submission, may be empty.
	 */
	protected List<Submission> submissionFindBySiteTx(final Site site, final User user)
	{
		String sql = "SELECT S.ID, S.ASSESSMENT, S.STARTED, S.FINISHED FROM ASMT_SUBMISSION S JOIN ASMT_ASSESSMENT A ON S.ASSESSMENT = A.ID WHERE A.SITE = ? AND S.USER = ?";
		Object[] fields = new Object[2];
		fields[0] = site.getId();
		fields[1] = user.getId();
		List<Submission> rv = sqlService().select(sql, fields, new SqlService.Reader<Submission>()
		{
			@Override
			public Submission read(ResultSet result)
			{
				try
				{
					SubmissionImpl submission = new SubmissionImpl();
					submission.initUser(user);

					int i = 1;
					submission.initId(sqlService().readLong(result, i++));
					submission.initAssessment(assessmentWrap(sqlService().readLong(result, i++)));
					submission.initStarted(sqlService().readDate(result, i++));
					submission.initFinished(sqlService().readDate(result, i++));
					submission.clearChanged();
					submission.setLoaded();

					return submission;
				}
				catch (SQLException e)
				{
					M_log.warn("submissionFindBySiteTx: " + e);
					return null;
				}
			}
		});

		return rv;
	}

	/**
	 * Transaction code for reading the submissions for this assessment.
	 * 
	 * @param site
	 *        The site.
	 * @param user
	 *        The user. return The List of Submission, may be empty.
	 */
	protected List<Submission> submissionFindByUserAssessmenTx(final User user, final Assessment assessment)
	{
		String sql = "SELECT ID, STARTED, FINISHED FROM ASMT_SUBMISSION WHERE ASSESSMENT = ? AND USER = ?";
		Object[] fields = new Object[2];
		fields[0] = assessment.getId();
		fields[1] = user.getId();
		List<Submission> rv = sqlService().select(sql, fields, new SqlService.Reader<Submission>()
		{
			@Override
			public Submission read(ResultSet result)
			{
				try
				{
					SubmissionImpl submission = new SubmissionImpl();
					submission.initAssessment(assessment);
					submission.initUser(user);

					int i = 1;
					submission.initId(sqlService().readLong(result, i++));
					submission.initStarted(sqlService().readDate(result, i++));
					submission.initFinished(sqlService().readDate(result, i++));
					submission.clearChanged();
					submission.setLoaded();

					return submission;
				}
				catch (SQLException e)
				{
					M_log.warn("submissionFindByAssessmenTx: " + e);
					return null;
				}
			}
		});

		return rv;
	}

	/**
	 * Transaction code for inserting a submission.
	 * 
	 * @param submission
	 *        The submission.
	 */
	protected void submissionInsertTx(SubmissionImpl submission)
	{
		String sql = "INSERT INTO ASMT_SUBMISSION (ASSESSMENT, USER, STARTED, FINISHED)" + " VALUES (?,?,?,?)";

		Object[] fields = new Object[4];
		int i = 0;
		fields[i++] = submission.getAssessment().getId();
		fields[i++] = submission.getUser().getId();
		fields[i++] = submission.getStarted();
		fields[i++] = submission.getFinished();

		Long id = sqlService().insert(sql, fields, "ID");
		submission.initId(id);
	}

	/**
	 * Transaction code for reading a submission
	 * 
	 * @param submission
	 *        The submission (id is set, rest is read).
	 */
	protected void submissionReadTx(final SubmissionImpl submission)
	{
		String sql = "SELECT ASSESSMENT, USER, STARTED, FINISHED FROM ASMT_SUBMISSION WHERE ID = ?";
		Object[] fields = new Object[1];
		fields[0] = submission.getId();
		sqlService().select(sql, fields, new SqlService.Reader<Submission>()
		{
			@Override
			public Submission read(ResultSet result)
			{
				try
				{
					int i = 1;
					submission.initAssessment(assessmentWrap(sqlService().readLong(result, i++)));
					submission.initUser(userService().wrap(sqlService().readLong(result, i++)));
					submission.initStarted(sqlService().readDate(result, i++));
					submission.initFinished(sqlService().readDate(result, i++));
					submission.clearChanged();
					submission.setLoaded();
					return submission;
				}
				catch (SQLException e)
				{
					M_log.warn("submissionReadTx: " + e);
					return null;
				}
			}
		});
	}

	/**
	 * Transaction code for removing a submission.
	 * 
	 * @param submission
	 *        The submission.
	 */
	protected void submissionRemoveTx(Submission submission)
	{
		String sql = "DELETE FROM ASMT_SUBMISSION WHERE ID = ?";
		Object[] fields = new Object[1];
		fields[0] = submission.getId();
		sqlService().update(sql, fields);
	}

	/**
	 * Transaction code for updating an existing submission (finished only).
	 * 
	 * @param submission
	 *        The submission.
	 */
	protected void submissionUpdateTx(SubmissionImpl submission)
	{
		String sql = "UPDATE ASMT_SUBMISSION SET FINISHED=? WHERE ID=?";

		Object[] fields = new Object[2];
		int i = 0;
		fields[i++] = submission.getFinished();

		fields[i++] = submission.getId();

		sqlService().update(sql, fields);
	}

	/**
	 * @return The registered BaseDateService.
	 */
	private BaseDateService baseDateService()
	{
		return (BaseDateService) Services.get(BaseDateService.class);
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

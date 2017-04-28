/**********************************************************************************
 * $URL: https://source.etudes.org/svn/serenity/trunk/assessment/assessment-webapp/src/main/webapp/assessment.js $
 * $Id: assessment.js 12504 2016-01-10 00:30:08Z ggolden $
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

var assessment_tool = null;

function AssessmentView(main)
{
	var me = this;
	
	this.ui = null;
	this.sortDirection = "A";
	this.sortMode = "S";
	this.assessments = null;
	this.assessmentsSorted = null;

	this.init = function()
	{
		me.ui = findElements(["asmt_view_table", "asmt_view_none"]);		
		me.ui.table = new e3_Table(me.ui.asmt_view_table);
		me.ui.sort = new e3_SortAction();
		me.ui.sort.inject(main.ui.asmt_bar_view,
				{onSort: me.onSort, options:[{value:"S", title:main.i18n.lookup("sort_status", "STATUS")},{value:"T", title:main.i18n.lookup("sort_title", "TITLE")},
				                             {value:"Y", title:main.i18n.lookup("sort_type", "TYPE")},{value:"D", title:main.i18n.lookup("sort_due", "DUE")}]});
		me.ui.sort.directional(true);
	};

	this.start = function()
	{
		me.load();
	};

	this.load = function()
	{
		var params = main.portal.cdp.params();
		params.url.site = main.portal.site.id;
		main.portal.cdp.request("assessment_getView evaluation_rubrics evaluation_categories", params, function(data)
		{
			if (data.rubrics != null) main.rubrics = data.rubrics;
			if (data.categories != null) main.categories = data.categories;
			if (data.fs != null) main.fs = data.fs;

			me.assessments = data.assessments;
			me.assessmentsSorted = main.sortAssessments(me.assessments, me.sortDirection, me.sortMode);

			// TODO: no need for full load if we are having a payload action, just the one assessment we need
			if (main.portal.payload != null)
			{
				if (main.portal.payload.action == Actions.perform)
				{
					var a = findIdInList(main.portal.payload.id, me.assessments);
					if (a != null)
					{
						// TODO:
						var s = findIdInList(a.submissionInProgress, a.submissions);
						if (s != null)
						{
							main.startDeliver(a, s);
							return;
						}
						else
						{
							main.startEnter(a);
							return;
						}
					}
				}
			}
			me.populate();
		});
	};

	this.titleMsg = function(inProgress, completed, assessment)
	{
		if (!assessment.published)
		{
			return null;
		}
		else if (inProgress)
		{
			return main.i18n.lookup("msg_title_inprogress", "- in progress; click to continue");
		}
		else if (completed)
		{
			if (assessment.submitStatus == main.SubmitStatus.again)
			{
				return main.i18n.lookup("msg_title_tryAgain", "- finished; click to try again");
			}
			else
			{
				return main.i18n.lookup("msg_title_finished", "- finished");
			}
		}
		else if (assessment.schedule.status == ScheduleStatus.willOpen)
		{
			return main.i18n.lookup("msg_title_willOpen", "- not yet open");
		}
		else if (assessment.schedule.status == ScheduleStatus.willOpenHide)
		{
			return null;
		}
		else if (assessment.schedule.status == ScheduleStatus.closed)
		{
			return main.i18n.lookup("msg_title_missed", "- missed!");
		}
		else // it has to be open if (assessment.schedule.status == ScheduleStatus.open)
		{
			if (assessment.submitStatus == main.SubmitStatus.start)
			{
				return main.i18n.lookup("msg_title_begin", "- available; click to begin");
			}
		}

		return null;
	};

	this.dot = function(inProgress, completed, assessment)
	{
		if (!assessment.published)
		{
			me.ui.table.dot(Dots.red, main.i18n.lookup("msg_unpublished", "not published"));
		}
		else if (inProgress)
		{
			me.ui.table.dot(Dots.progress, main.i18n.lookup("msg_inProgressSimple", "in progress"));
		}
		else if (completed)
		{
			me.ui.table.dot(Dots.complete, main.i18n.lookup("msg_complete", "finished"));
		}
		else if (assessment.schedule.status == ScheduleStatus.willOpen)
		{
			me.ui.table.dot(Dots.yellow,  main.i18n.lookup("msg_willOpen", "will open"));
		}
		else if (assessment.schedule.status == ScheduleStatus.willOpenHide)
		{
			me.ui.table.dot(Dots.gray, main.i18n.lookup("msg_willOpenHidden", "hidden until open"));
		}
		else if (assessment.schedule.status == ScheduleStatus.closed)
		{
			me.ui.table.dot(Dots.alert, main.i18n.lookup("msg_missed", "missed"));
		}
		else // it has to be open if (assessment.schedule.status == ScheduleStatus.open)
		{
			me.ui.table.dot(Dots.none);
			// me.ui.table.dot(Dots.blue,  main.i18n.lookup("msg_available", "available"));
		}
	};

	this.populate = function()
	{
		me.ui.table.clear();
		$.each(me.assessmentsSorted, function(index, assessment)
		{
			me.ui.table.row();
			me.dot((assessment.submissionInProgress != null), (assessment.submissionReview != null), assessment);

			var titleMsg = me.titleMsg((assessment.submissionInProgress != null), (assessment.submissionReview != null), assessment);

			if (assessment.submitStatus == main.SubmitStatus.nothing)
			{
				if (titleMsg == null)
				{
					var td = me.ui.table.text(assessment.title, null, {width:"calc(100vw - 100px - 778px)", minWidth:"calc(1200px - 100px - 778px)"});
					td.css({opacity: 0.4});
				}
				else
				{
					var cell = clone(main.ui.asmt_title_template, ["asmt_title_template_body", "asmt_title_template_title", "asmt_title_template_msg"]);
					me.ui.table.element(cell.asmt_title_template_body, null, {width:"calc(100vw - 100px - 778px)", minWidth:"calc(1200px - 100px - 778px"});

					cell.asmt_title_template_title.text(assessment.title).css({color: "#A8A8A8"});
					cell.asmt_title_template_msg.text(titleMsg).css({color: "#A8A8A8"});
				}
			}
			else
			{
				var msg = main.i18n.lookup("msg_enter_" + assessment.submitStatus, "", "html", [assessment.title]);
				if (titleMsg == null)
				{
					me.ui.table.hotText(assessment.title, msg, function(){me.goIn(assessment);}, null, {width:"calc(100vw - 100px - 778px)", minWidth:"calc(1200px - 100px - 778px"});
				}
				else
				{
					var cell = clone(main.ui.asmt_title_template, ["asmt_title_template_body", "asmt_title_template_title", "asmt_title_template_msg"]);
					me.ui.table.hotElement(cell.asmt_title_template_body, msg, function(){me.goIn(assessment);}, null, {width:"calc(100vw - 100px - 778px)", minWidth:"calc(1200px - 100px - 778px"});
		
					cell.asmt_title_template_title.text(assessment.title);
					cell.asmt_title_template_msg.text(titleMsg);
				}
			}

			main.typeTd(assessment, me.ui.table);

			me.ui.table.date(assessment.schedule.open, "-", "date2l");
			me.ui.table.date(assessment.schedule.due, "-", "date2l");

			me.ui.table.text("-", null, {width:40}); // TODO: time limit: assessment.options.timeLimitSet, assessment.options.timeLimit

			var triesMsg = null;
			if (assessment.options.triesSet)
			{
				triesMsg = main.i18n.lookup("msg_triesOf", "%0 / %1", "html", [assessment.submitCount, assessment.options.numTries]);
			}
			else
			{
				triesMsg = main.i18n.lookup("msg_tries", "%0", "html", [assessment.submitCount]);
			}
			me.ui.table.text(triesMsg, null, {width:40});

			// show the one picked for 'review'
			if (assessment.submissionReview != null)
			{
				me.ui.table.date(assessment.submissionReview.started, "-", "date2l");
				me.ui.table.date(assessment.submissionReview.finished, "-", "date2l");

				if (assessment.submissionReview.complete)
				{
					var td = me.ui.table.text("", null, {width:150});
					main.ui.evaluation.reviewLink.set(td, assessment.evaluationDesign, assessment.submissionReview.evaluation,
						function(){main.startReview(assessment.submissionReview.id);}, main.rubrics);
				}
				else
				{
					me.ui.table.text("-", null, {width:150});
				}
			}
			else
			{
				me.ui.table.text("-", "date2l");
				me.ui.table.text("-", "date2l");
				me.ui.table.text("-", null, {width:150});
			}
		});
		me.ui.table.done();
		show(me.ui.asmt_view_none, (me.ui.table.rowCount() == 0));
	};
	
	this.goIn = function(assessment)
	{
		if (assessment.submissionInProgress != null)
		{
			// main.startDeliver(assessment, assessment.submissionInProgress); // TODO: just an id
			main.startToc(assessment.submissionInProgress);		// TODO: other starting places?
		}
		else
		{
			main.startEnter(assessment);
		}
	};
	
	this.onSort = function(direction, option)
	{
		me.sortDirection = direction;
		me.sortMode = option;

		me.assessmentsSorted = main.sortAssessments(me.assessments, me.sortDirection, me.sortMode);
		me.populate();
	};
}

function AssessmentEnter(main)
{
	var me = this;

	this.ui = null;
	this.assessment = null;

	this.init = function()
	{
		me.ui = findElements(["asmt_enter_points", "asmt_enter_title", "asmt_enter_type", "asmt_enter_due", "asmt_enter_tries", "asmt_enter_timed", "asmt_enter_nearDue",
		                      "asmt_enter_instructions", "asmt_enter_evaluation_ui",
		                      "asmt_enter_notice_survey", "asmt_enter_notice_fce", "asmt_enter_notice_linear",
		                      "asmt_enter_notice_flexible", "asmt_enter_notice_flexible_exitNote", "asmt_enter_notice_flexible_nextPrev", "asmt_enter_notice_flexible_toc",
		                      "asmt_enter_notice_assignment", "asmt_enter_notice_assignment_exitNote",
		                      "asmt_enter_notice_highest", "asmt_enter_notice_timed", "asmt_beginPledgePassword",
		                      "asmt_beginPledgePassword_passwordUI", "asmt_beginPledgePassword_password", "asmt_beginPledgePassword_passwordLbl",
		                      "asmt_beginPledgePassword_pledgeUI", "asmt_beginPledgePassword_pledge", "asmt_beginPledgePassword_pledgeLbl",
		                      "asmt_enternav"
		                      ]);
		me.ui.itemNav = new e3_ItemNav();
	};

	this.start = function(assessment)
	{
		me.load(assessment.id);
	};
	
	this.load = function(id)
	{
		var params = main.portal.cdp.params();
		params.url.site = main.portal.site.id;
		params.url.assessment = id;
		main.portal.cdp.request("assessment_getEnter", params, function(data)
		{
			me.assessment = data.assessment;
			me.ui.itemNav.injectSpecial(main.ui.asmt_itemnav, me.ui.asmt_enternav, ["asmt_enter_cancel", "asmt_enter_begin"], [me.goBack, me.enter]);

			// me.makeEdit();
			me.populate();
		});
	};

	this.populate = function()
	{
		me.ui.asmt_enter_points.text(((me.assessment.evaluationDesign.actualPoints !== undefined) ? me.assessment.evaluationDesign.actualPoints : "-"));
		me.ui.asmt_enter_type.text(main.i18n.lookup("msg_type_" + me.assessment.type));
		me.ui.asmt_enter_title.text(me.assessment.title);
		me.ui.asmt_enter_due.text(me.assessment.schedule.due != null ? main.portal.timestamp.display(me.assessment.schedule.due) : "-");
		me.ui.asmt_enter_tries.text(((me.assessment.options.triesSet) ? me.assessment.options.numTries : "-"));
		me.ui.asmt_enter_timed.text(((me.assessment.options.timeLimitSet) ? me.assessment.options.timeLimit : "-"));
		
		show(me.ui.asmt_enter_nearDue, ((me.assessment.schedule.nearDue !== undefined) && (me.assessment.schedule.nearDue)));

		if (me.assessment.design.instructions != null)
		{
			me.ui.asmt_enter_instructions.empty().html(me.assessment.design.instructions);
		}
		show(me.ui.asmt_enter_instructions, (me.assessment.design.instructions != null));

		show(me.ui.asmt_enter_notice_survey, me.assessment.type == main.AssessmentType.survey);
		show(me.ui.asmt_enter_notice_fce, false);

		show(me.ui.asmt_enter_notice_linear, (!me.assessment.options.flexible && ((me.assessment.type == main.AssessmentType.test) || (me.assessment.type == main.AssessmentType.survey))));

		show(me.ui.asmt_enter_notice_flexible, (me.assessment.options.flexible && ((me.assessment.type == main.AssessmentType.test) || (me.assessment.type == main.AssessmentType.survey))));
		show(me.ui.asmt_enter_notice_flexible_exitNote, (!me.assessment.options.timeLimitSet));
		show(me.ui.asmt_enter_notice_flexible_nextPrev, me.assessment.options.layout != main.Layout.all);
		show(me.ui.asmt_enter_notice_flexible_toc, me.assessment.options.layout == main.Layout.all);

		show(me.ui.asmt_enter_notice_assignment, me.assessment.type == main.AssessmentType.assignment);
		show(me.ui.asmt_enter_notice_assignment_exitNote, (!me.assessment.options.timeLimitSet));

		show(me.ui.asmt_enter_notice_highest, ((!me.assessment.options.triesSet) || (me.assessment.options.numTries > 1)));
		show(me.ui.asmt_enter_notice_timed, (me.assessment.options.timeLimitSet));

		main.ui.evaluation.display.set(me.ui.asmt_enter_evaluation_ui, me.assessment.evaluationDesign, main.rubrics);
	};
	
	this.enter = function()
	{
		if ((me.assessment.options.pledge) || (me.assessment.options.password != null))
		{
			me.confirmPledgePassword();
			return;
		}

		var params = main.portal.cdp.params();
		params.url.site = main.portal.site.id;
		params.url.assessment = me.assessment.id;

		main.portal.cdp.request("assessment_enter", params, function(data)
		{
			// main.startDeliver(me.assessment, data.submission); // TODO:
			console.log(data.submissionId);
			main.startView();
		});
	};
	
	this.confirmPledgePassword = function()
	{
		show(me.ui.asmt_beginPledgePassword_pledgeUI, (me.assessment.options.pledge));
		show(me.ui.asmt_beginPledgePassword_passwordUI, (me.assessment.options.requiresPassword));

		me.ui.asmt_beginPledgePassword_pledge.prop("checked", false);
		me.ui.asmt_beginPledgePassword_pledgeLbl.css({color: ""});

		me.ui.asmt_beginPledgePassword_password.val("");
		me.ui.asmt_beginPledgePassword_passwordLbl.css({color: ""});

		main.portal.dialogs.openDialogButtons(me.ui.asmt_beginPledgePassword,
		[
			{text: main.i18n.lookup("action_Begin", "Begin"), click: function()
			{
				var pledge = me.ui.asmt_beginPledgePassword_pledge.is(":checked");
				var pw = trim(me.ui.asmt_beginPledgePassword_password.val());
				
				var pledgeRequirementSatisfied = ((!me.assessment.options.pledge) || pledge);
				var pwRequirementSatisfied = ((!me.assessment.options.requiresPassword) || (pw != null));

				if (pledgeRequirementSatisfied)
				{
					me.ui.asmt_beginPledgePassword_pledgeLbl.css({color: ""});
				}
				else
				{
					me.ui.asmt_beginPledgePassword_pledgeLbl.css({color: "#E00000"});
				}

				if (pwRequirementSatisfied)
				{
					me.ui.asmt_beginPledgePassword_passwordLbl.css({color: ""});
				}
				else
				{
					me.ui.asmt_beginPledgePassword_passwordLbl.css({color: "#E00000"});
				}

				if (pledgeRequirementSatisfied && pwRequirementSatisfied)
				{
					var params = main.portal.cdp.params();
					params.url.site = main.portal.site.id;
					params.url.assessment = me.assessment.id;
					if (pw != null) params.post.pw = pw;	
					main.portal.cdp.request("assessment_enter", params, function(data)
					{
						console.log(data.submissionId);
						main.startView();
						// main.startDeliver(me.assessment, data.submission); // TODO:
					});
					return true;
				}
				else
				{
					return false;
				}
			}},
		], null, main.i18n.lookup("action_cancel", "Cancel"));
	};

	this.goBack = function()
	{
		if (main.portal.toolReturn != null)
		{
			main.portal.navigate(main.portal.toolReturn.site, main.portal.toolReturn.toolId, false, false);
		}
		else
		{
			main.startView();
		}
	}
}

function AssessmentToc(main)
{
	var me = this;

	this.ui = null;

	this.assessment = null;
	this.entries = null;
	this.submissionId = null;

	this.init = function()
	{
		me.ui = findElements(["asmt_toc_points", "asmt_toc_title", "asmt_toc_type",
		                      "asmt_toc_table",
		                      "asmt_tocnav"
		                      ]);
		me.ui.itemNav = new e3_ItemNav();
		me.ui.itemNav.injectSpecial(main.ui.asmt_itemnav, me.ui.asmt_tocnav, ["asmt_toc_later", "asmt_toc_finish"], [me.later, me.finish]);
		
		me.ui.table = new e3_Table(me.ui.asmt_toc_table);
	};

	this.start = function(submissionId)
	{
		me.submissionId = submissionId;
		me.load();
	};
	
	this.load = function(id)
	{
		var params = main.portal.cdp.params();
		params.url.site = main.portal.site.id;
		params.url.submission = me.submissionId;
		main.portal.cdp.request("assessment_getToc", params, function(data)
		{
			me.assessment = data.assessment;
			me.entries = data.entries;

			me.populate();
		});
	};

	this.populate = function()
	{
		me.ui.asmt_toc_points.text(((me.assessment.evaluationDesign.forPoints) ? me.assessment.evaluationDesign.points : "-"));
		me.ui.asmt_toc_type.text(main.i18n.lookup("msg_type_" + me.assessment.type));
		me.ui.asmt_toc_title.text(me.assessment.title);

		// list of questions, each with
		//	status: unanswered / review / missing reason,  points: float,  question display text (text),  question or answer id within assessment design or submission
		var inPartId = null;
		var questionNumber = 1;
		me.ui.table.clear();
		$.each(me.entries, function(index, entry)
		{
			me.ui.table.row();
			
			// for the start of a part
			if ((inPartId == null) || (entry.partId != inPartId))
			{
				var part = findIdInList(entry.partId, me.assessment.design.parts);
				
				var msg = null;
				if (part.title != null)
				{
					msg = main.i18n.lookup("msg_partOrderTitle", "Part %0 of %1: %2", "html", [part.ordering.position, part.ordering.size, part.title]);
				}
				else
				{
					msg = main.i18n.lookup("msg_partOrder", "Part %0 of %1", "html", [part.ordering.position, part.ordering.size]);
				}
				me.ui.table.headerRow(msg);

				me.ui.table.row();

				inPartId = entry.partId;
				if (me.assessment.options.numbering == main.Numbering.restart)
				{
					questionNumber = 1;
				}
			}

			me.ui.table.text(entry.points, null, {width: 60});
			me.ui.table.text(main.i18n.lookup("msg_tocStatus_" + entry.status), "e3_text special light", {fontSize:11, width:75, textTransform: "uppercase"});
			me.ui.table.text(main.i18n.lookup("msg_questionNumberDescription", "%0. %1", "html", [questionNumber++, entry.description]), null, {width: "calc(100vw - 100px - 215px)", minWidth: "calc(1200px - 100px - 215px)"});
		});
		me.ui.table.done();		
	};
	
	this.later = function()
	{
		main.startView();
	};
	
	this.finish = function()
	{
		var params = main.portal.cdp.params();
		params.url.site = main.portal.site.id;
		params.url.submission = me.submissionId;
		main.portal.cdp.request("assessment_finish", params, function(data)
		{
			main.startView();	// finish view?
		});
	};
}

// TODO: major in progress!
function AssessmentDeliver(main)
{
	var me = this;

	this.ui = null;
	this.assessment = null;
	this.submission = null;
	this.answer = null;
	this.edit = null;

	this.replaceAnswer = function(answer)
	{
		for (var i = 0; i < me.submission.answers.length; i++)
		{
			if (me.submission.answers[i].id == answer.id)
			{
				me.submission.answers[i] = answer;
				me.answer = answer;
				return;
			}
		}
	};

	this.questionPosition = function(question)
	{
		// TODO: question in test, part in parts
		var rv = {};
		rv.item = 1;
		rv.total = 1;
		rv.prev = null;
		rv.next = null;

		return rv;
	};

	this.init = function()
	{
		me.ui = findElements(["asmt_deliver_template_finish", "asmt_deliver_title", "asmt_deliver_asmtPoints",
		                      "asmt_deliver_body", "asmt_deliver_template_essay"]);
		me.ui.itemNav = new e3_ItemNav();
	};

	// a specific question/answer, a specific part, a specific entire test, 
	this.start = function(assessment, submission)
	{
		me.assessment = assessment;
		me.submission = submission;

		main.onExit = me.checkExit;
		
		me.ui.itemNav.inject(main.ui.asmt_itemnav, {returnFunction:me.done, returnName: "Continue Later", withSave: true, pos:me.questionPosition(), navigateFunction:me.goQuestion, // TODO: i18n
			extra:{template:me.ui.asmt_deliver_template_finish, onClick:me.finish}});

		me.populate();
	};

	this.load = function(id)
	{
		var params = main.portal.cdp.params();
		params.url.site = main.portal.site.id;
		params.url.submission = id;
		params.url.questionOrPart = id; // TODO:
		main.portal.cdp.request("assessment_getDeliver", params, function(data)
		{
			// like for review
			me.submission = data.submission; // full submission
			me.assessment = data.assessment; // full assessment w/ design
			me.answerIds = data.answerIds;		// answers to present (1, part, all)  just ids

			// me.makeEdit();
			me.populate();
		});
	};

	this.populate = function()
	{
		me.ui.asmt_deliver_asmtPoints.text((me.assessment.design.forGrade ? main.i18n.lookup("msg_points", "%0 points", "html", [me.assessment.design.points]) : ""));
		me.ui.asmt_deliver_title.text(me.assessment.title);
		
		// questions
		me.populateDeliveryQuestions();
		
//		switch (me.assessment.type)
//		{
//			case /* main.AssessmentType.essay */ "E":
//			{
//				me.populateEssay();
//				break;
//			}
//
//			default:
//			{
//				// me.ui.nav.asmt_deliver_pos.text("1 of 1"); // TODO:
//				me.ui.asmt_deliver_body.empty().text("QUESTION AREA");
//			}
//		}
	};

	this.populateDeliveryQuestions = function()
	{
		me.ui.asmt_deliver_deliver_ui.empty();

		var inPartId = null;
		var questionNumber = 1;
		var partNumQuestions = null;

		var displayParts = ((me.assessment.design.parts.length > 1) || (me.assessment.design.parts[0].title != null) || (me.assessment.design.parts[0].instructions != null));

		// instructions - only if showing the entire assessment
		if (me.assessment.design.instructions != null)
		{
			me.ui.asmt_review_review_ui.append(me.assessment.design.instructions); // TODO:
		}

		// add a display for each question / answer
		$.each(me.answerIds, function(index, answerId)
		{
			// find the answer inthe submission, and the question
			var answer = findIdInList(answerId, me.submission.answers);
			var question = findIdInList(answer.questionId, me.questions);

			// for the start of a part
			if (displayParts && ((inPartId == null) || (question.partId != inPartId)))
			{
				var part = findIdInList(question.partId, me.assessment.design.parts);
				
				var msg = null;
				if (part.title != null)
				{
					msg = main.i18n.lookup("msg_partOrderOfTitle", "Part %0 of %1: %2", "html", [part.ordering.position, part.ordering.size, part.title]);
				}
				else
				{
					msg = main.i18n.lookup("msg_partOrderOf", "Part %0 of %1", "html", [part.ordering.position, part.ordering.size]);
				}

				inPartId = question.partId;
				if (me.assessment.options.numbering == 1)	// restart per part
				{
					questionNumber = 1;
					partNumQuestions = part.questions;
				}
				
				me.ui.asmt_review_review_ui.append($("<div>" + msg + "</div>")); // TODO:
				if (part.instructions != null)
				{
					me.ui.asmt_review_review_ui.append(part.instructions); // TODO:
				}
			}

			var ui = clone(me.ui.asmt_deliver_template_question, ["asmt_review_template_question_body", "asmt_review_template_question_position", "asmt_review_template_question_position_ui",
			                                                     "asmt_review_template_question_scorePct", "asmt_review_template_question_scorePct_ui",
			                                                     "asmt_review_template_question_scorePts", "asmt_review_template_question_scorePts_ui",
			                                                     "asmt_review_template_question_qa",
			                                                     "asmt_review_template_question_evaluation", "asmt_review_template_question_evaluation_official", "asmt_review_template_question_evaluation_peer",
			                                                     "asmt_review_template_question_evaluation_comment"]);
			if (me.submission.answers.length > 1) // TODO: if test has more than one question - need question's position in test (we might not have all the questions / answers here)
			{
				// TODO: add question.assessmentPosition {item, total, prev, next) and question.partPosition
				// TODO: add constant for numbering 1 / 0
				var pos = (me.assessment.options.numbering == 1) ? question.partPosition : question.assessmentPosition	// restart per part
				ui.asmt_review_template_question_position.text(main.i18n.lookup("msg_questionPosition", "%0 of %1", "html", [pos.item, pos.total]));
			}
			show(ui.asmt_review_template_question_position_ui, (me.submission.answers.length > 1));

			if (me.assessment.evaluationDesign.forPoints)
			{
				ui.asmt_deliver_template_question_points.text(question.evaluationDesign.points);
			}
			show(ui.asmt_deliver_template_question_points, (me.assessment.evaluationDesign.forPoints));

			me.populateQuestion(ui.asmt_review_template_question_qa, question, answer)
			
			me.ui.asmt_review_review_ui.append(ui.asmt_review_template_question_body);
		});
	};
	
	this.populateQuestion = function(area, question, answer)
	{
		if (question.type == "mneme:Essay")
		{
			area.append(me.populateEssay(question, answer));
		}
		else if (question.type == "mneme:TrueFalse")
		{
			area.append(me.populateTrueFalse(question, answer));
		}
		else if (question.type == "mneme:MultipleChoice")
		{
			area.append(me.populateMultipleChoice(question, answer));
		}
		else if (question.type == "mneme:LikertScale")
		{
			area.append(me.populateMultipleChoice(question, answer));
		}
		else if (question.type == "mneme:FillBlanks")
		{
			area.append(me.populateFillin(question, answer));
		}
		else if (question.type == "mneme:FillInline")
		{
			area.append(me.populateFillin(question, answer));
		}
		else
		{
			// unhandled question type
			area.append("<div>SOME QUESTION</div>");
		}
	};

	this.populateEssay = function(question, answer)
	{
		var ui = clone(me.ui.asmt_review_template_essay, ["asmt_review_template_essay_body", "asmt_review_template_essay_instructions", "asmt_review_template_essay_content"]);

		ui.asmt_review_template_essay_instructions.html(question.essay.question);		
		ui.asmt_review_template_essay_content.html(answer.essay.answer);

		return ui.asmt_review_template_essay_body;
	};

	this.populateTrueFalse = function(question, answer)
	{
		var ui = clone(me.ui.asmt_review_template_tf, ["asmt_review_template_tf_body", "asmt_review_template_tf_instructions",
		                                               "asmt_review_template_tf_true", "asmt_review_template_tf_false",
		                                               "asmt_review_template_tf_true_mark", "asmt_review_template_tf_false_mark"
		                                               ]);

		ui.asmt_review_template_tf_instructions.html(question.tf.question);
		ui.asmt_review_template_tf_true.prop("name", "tf_" + question.id);
		ui.asmt_review_template_tf_false.prop("name", "tf_" + question.id);
		if (answer.tf.answer !== undefined)
		{
			if (answer.tf.answer)
			{
				ui.asmt_review_template_tf_true.prop("checked", true);
				ui.asmt_review_template_tf_true_mark.append(me.mark(answer.tf.correct));
			}
			else
			{
				ui.asmt_review_template_tf_false.prop("checked", true);
				ui.asmt_review_template_tf_false_mark.append(me.mark(answer.tf.correct));
			}
		}
		else
		{
			if (question.tf.answer)
			{
				ui.asmt_review_template_tf_true_mark.append(me.indicate());
			}
			else
			{
				ui.asmt_review_template_tf_false_mark.append(me.indicate());
			}
		}

		return ui.asmt_review_template_tf_body;
	};

	this.populateMultipleChoice = function(question, answer)
	{
		var ui = clone(me.ui.asmt_review_template_mc, ["asmt_review_template_mc_body", "asmt_review_template_mc_instructions", "asmt_review_template_mc_choices"]);

		ui.asmt_review_template_mc_instructions.html(question.mc.question);

		$.each(question.mc.choices, function(index, choice)
		{
			var choiceUi = clone(me.ui.asmt_review_template_mc_entry, ["asmt_review_template_mc_entry_body", "asmt_review_template_mc_entry_mark", "asmt_review_template_mc_entry_radio", "asmt_review_template_mc_entry_label"]);
			ui.asmt_review_template_mc_choices.append(choiceUi.asmt_review_template_mc_entry_body);

			choiceUi.asmt_review_template_mc_entry_radio.prop("name", "mc_" + question.id);
			choiceUi.asmt_review_template_mc_entry_label.html(choice.text);
			if (answer.mc.answer == choice.answer)
			{
				choiceUi.asmt_review_template_mc_entry_radio.prop("checked", true);
				if (answer.mc.correct != undefined) choiceUi.asmt_review_template_mc_entry_mark.append(me.mark(answer.mc.correct));
			}
			else if ((choice.correct !== undefined) && (choice.correct))
			{
				choiceUi.asmt_review_template_mc_entry_mark.append(me.indicate());
			}
		});

		return ui.asmt_review_template_mc_body;
	};

	this.populateFillin = function(question, answer)
	{		
		var ui = clone(me.ui.asmt_review_template_fillin, ["asmt_review_template_fillin_body", "asmt_review_template_fillin_results"]);
		
		var text = "";
		$.each(question.fillin.segments, function(index, segment)
		{
			if (segment.text !== undefined)
			{
				text += segment.text;
			}
			else
			{
				text += "<span style='margin-left:8px;'>" + me.mark(answer.fillin.answers[segment.fillin].correct)[0].outerHTML + "</span>";
				// text += "<span style='margin-left:4px; background-color:#D8D8D8; text-decoration:underline; padding-left:4px; padding-right:4px;'>" + answer.fillin.answers[segment.fillin].text + "</span>";
				// text += "<input disabled class='e3_input' value='" + answer.fillin.answers[segment.fillin].text + "' type='text' style='color:#" + ((answer.fillin.answers[segment.fillin].correct) ? "2AB31D" : "E00000") + ";'>"
				
				if (answer.fillin.answers[segment.fillin].correct)
				{
					text += "<span class='e3_disabledInput' style='color:#2AB31D;'>" + answer.fillin.answers[segment.fillin].text + "</span>";
				}
				else
				{
					text += "<span class='e3_disabledInput'><span style='color:#E00000;'>" + answer.fillin.answers[segment.fillin].text + "</span>"
						+ "<span style='margin-left:16px; color:#0072C6; font-style:normal;'>" + me.indicate()[0].outerHTML
						+ question.fillin.answers[segment.fillin] + "</span></span>";
				}
			}
		});
		
		ui.asmt_review_template_fillin_results.html(text);

		return ui.asmt_review_template_fillin_body;
	};

	
//
//	this.populateEssay = function()
//	{
//		var ui = clone(me.ui.asmt_deliver_template_essay, ["asmt_deliver_template_essay_body", "asmt_deliver_template_essay_instructions", "asmt_deliver_template_essay_editor"]);
//		me.ui.asmt_deliver_body.empty().append(ui.asmt_deliver_template_essay_body);
//
//		ui.asmt_deliver_template_essay_instructions.html(me.assessment.instructions);
//		
//		me.answer = me.submission.answers[0];
//
//		me.makeEdit();
//		
//		me.ui.editor = new e3_EditorCK(ui.asmt_deliver_template_essay_editor, {height: 300}); // TODO: should this create be in init?
//		me.ui.editor.disable();
//		me.ui.editor.set(me.answer.content);
//		me.ui.editor.enable(function()
//		{
//			me.edit.set(me.edit, "content", me.ui.editor.get());
//		}, true, main.fs);
//
//		// me.ui.nav.asmt_deliver_pos.text("1 of 1"); // TODO:
//	};

	this.makeEdit = function()
	{
		me.edit = new e3_Edit(me.answer, ["answeredOn", "id", "question", "answered"], function(changed)
		{
			// main.ui.modebar.enableSaveDiscard(changed ? me.saveCancel : null);
			me.ui.itemNav.enableSave(changed ? me.saveCancel : null);
		});
		me.ui.itemNav.enableSave(null);
	};

	this.saveCancel = function(mode, deferred)
	{
		if (mode)
		{
			me.save(deferred);
		}
		else
		{
			me.edit.revert();
			me.populate();
			if (deferred !== undefined) deferred();
		}
	};

	this.checkExit = function(deferred)
	{
		if (me.edit == null) return true;

		if (me.edit.changed())
		{
			main.portal.confirmNavigationWithChanges(function()
			{
				me.save(deferred);				
			}, function()
			{
				me.edit.revert();
				// me.populate();
				if (deferred !== undefined) deferred();
			});

			return false;
		}

		return true;
	};

	this.save = function(deferred)
	{
		var params = main.portal.cdp.params();
		params.url.site = main.portal.site.id;
		params.url.assessment = me.assessment.id;
		params.url.submission = me.submission.id;
		params.url.answer = me.answer.id;
		me.edit.params("", params);

		main.portal.cdp.request("assessment_saveAnswer assessment_getAnswer", params, function(data)
		{
			me.replaceAnswer(data.answer);
			me.populate();
			if (deferred !== undefined) deferred();
		});
	};

	this.finish = function()
	{
		var params = main.portal.cdp.params();
		params.url.site = main.portal.site.id;
		params.url.assessment = me.assessment.id;
		params.url.submission = me.submission.id;
		params.url.answer = me.answer.id;
		me.edit.params("", params);

		main.portal.cdp.request("assessment_finish", params, function(data)
		{
			me.edit.revert();
			// TODO: complete view
			me.goBack();
		});
	};

	this.done = function()
	{
		if (me.edit.changed())
		{
			me.save(function(){main.startView();})
		}
		else
		{
			me.goBack();
		}
	};

	this.goBack = function()
	{
		if (main.portal.toolReturn != null)
		{
			main.portal.navigate(main.portal.toolReturn.site, main.portal.toolReturn.toolId, false, false);
		}
		else
		{
			main.startView();
		}
	};

	this.goQuestion = function(question)
	{
		console.log("go");
	};

	this.prev = function()
	{
		console.log("prev");
	};

	this.next = function()
	{
		console.log("next");
	};
};

function AssessmentReview(main)
{
	var me = this;

	this.ui = null;
	this.assessment = null;
	this.submission = null;
	this.submissionIds = null;
	this.questions = null;

	this.init = function()
	{
		me.ui = findElements(["asmt_review_points", "asmt_review_title", "asmt_review_scorePct", "asmt_review_scorePts", "asmt_review_started", "asmt_review_finished", "asmt_review_scorePct_bkg",
		                      "asmt_review_best", "asmt_review_noReview",
		                      "asmt_review_review_ui", "asmt_review_evaluation_ui", "asmt_review_noReview",
		                      "asmt_review_template_marking_correct", "asmt_review_template_marking_incorrect", "asmt_review_template_marking_indicate",
		                      "asmt_review_template_question", "asmt_review_template_essay", "asmt_review_template_tf",
		                      "asmt_review_template_mc", "asmt_review_template_mc_entry", "asmt_review_template_fillin"]);		
		me.ui.itemNav = new e3_ItemNav();
	};

	this.start = function(submissionId)
	{
		me.load(submissionId);
	};

	this.load = function(submissionId)
	{
		var params = main.portal.cdp.params();
		params.url.site = main.portal.site.id;
		params.url.submission = submissionId;
		main.portal.cdp.request("assessment_getReview", params, function(data)
		{
			me.submission = data.submission;
			me.submissionIds = data.submissionIds;
			me.assessment = data.assessment;
			me.questions = data.questions;

			me.ui.itemNav.inject(main.ui.asmt_itemnav, {returnFunction: me.goBack, pos: position(me.submission, me.submissionIds), navigateFunction: me.goSubmission});
			me.populate();
		});
	};

	this.populate = function()
	{
        me.ui.asmt_review_points.text((me.assessment.evaluationDesign.forPoints ? me.assessment.evaluationDesign.points : "-"));
		me.ui.asmt_review_title.text(me.assessment.title);
		
		me.ui.asmt_review_started.text((me.submission.started == null) ? "-" : main.portal.timestamp.display(me.submission.started));
		
		if (me.submission.finished == null)
		{
			me.ui.asmt_gradeSubmission_finished.text("-");
		}
		else
		{
			me.ui.asmt_review_finished.text(main.portal.timestamp.display(me.submission.finished));

			if (me.submission.late) me.ui.asmt_review_finished.append($("<span />").text(main.i18n.lookup("msg_late", "late")).addClass("e3_text special").css({color: "#E00000", fontSize: 9, paddingLeft: 4, textTransform: "uppercase"}));
			if (me.submission.status == "A") me.ui.asmt_review_finished.append($("<span />").text(main.i18n.lookup("msg_auto", "auto")).addClass("e3_text special").css({color: "#E00000", fontSize: 9, paddingLeft: 4, textTransform: "uppercase"}));
		}

		if (me.submission.evaluation.mayReview)
		{
			if (me.assessment.evaluationDesign.forPoints && me.submission.evaluation.released)
			{
				me.ui.asmt_review_scorePct.text(asPct(me.submission.evaluation.score, me.assessment.evaluationDesign.points));
				
				// color with red at 65% and below, green at 100% and above
				var pct = (me.submission.evaluation.score / me.assessment.evaluationDesign.points) - .65;
				if (pct < 0) pct = 0;
				if (pct > .35) pct = .35;
				pct = pct * (1.0 / 0.35);
				me.ui.asmt_review_scorePct_bkg.css({backgroundColor: rgColor(pct)});
				
				me.ui.asmt_review_scorePts.text(me.submission.evaluation.score);
				
				show(me.ui.asmt_review_best, (me.submission.best && (me.submissionIds.length > 1)));
			}
			else
			{
				me.ui.asmt_review_scorePct.text("-");
				me.ui.asmt_review_scorePct_bkg.css({backgroundColor: "white"});

				if (me.submission.evaluation.released)
				{
					me.ui.asmt_review_scorePts.text("-");
				}
				else
				{
					me.ui.asmt_review_scorePts.text(main.i18n.lookup("msg_ungraded", "not graded")).css({fontFamily: "Oswald, sans-serif", fontSize: 16, textTransform: "uppercase"});
				}

				hide(me.ui.asmt_review_best);
			}

			me.populateReviewQuestions();

			show([me.ui.asmt_review_review_ui, me.ui.asmt_review_evaluation_ui]);
			hide(me.ui.asmt_review_noReview);
		}

		// no review
		else
		{
			me.ui.asmt_review_scorePct.text("-");
			me.ui.asmt_review_scorePct_bkg.css({backgroundColor: "white"});

			me.ui.asmt_review_scorePts.text("-");

			hide(me.ui.asmt_review_best);

			hide([me.ui.asmt_review_review_ui, me.ui.asmt_review_evaluation_ui]);
			show(me.ui.asmt_review_noReview);
		}
	};

	this.populateReviewQuestions = function()
	{
		me.ui.asmt_review_review_ui.empty();

		var inPartId = null;
		var questionNumber = 1;
		var partNumQuestions = null;

		var displayParts = ((me.assessment.design.parts.length > 1) || (me.assessment.design.parts[0].title != null) || (me.assessment.design.parts[0].instructions != null));

		// instructions
		if (me.assessment.design.instructions != null)
		{
			me.ui.asmt_review_review_ui.append(me.assessment.design.instructions); // TODO:
		}

		// add a display for each question / answer
		$.each(me.submission.answers, function(index, answer)
		{
			var question = findIdInList(answer.questionId, me.questions);

			// for the start of a part
			if (displayParts && ((inPartId == null) || (question.partId != inPartId)))
			{
				var part = findIdInList(question.partId, me.assessment.design.parts);
				
				var msg = null;
				if (part.title != null)
				{
					msg = main.i18n.lookup("msg_partOrderOfTitle", "Part %0 of %1: %2", "html", [part.ordering.position, part.ordering.size, part.title]);
				}
				else
				{
					msg = main.i18n.lookup("msg_partOrderOf", "Part %0 of %1", "html", [part.ordering.position, part.ordering.size]);
				}

				inPartId = question.partId;
				if (me.assessment.options.numbering == main.Numbering.restart)
				{
					questionNumber = 1;
					partNumQuestions = part.questions;
				}
				
				me.ui.asmt_review_review_ui.append($("<div>" + msg + "</div>")); // TODO:
				if (part.instructions != null)
				{
					me.ui.asmt_review_review_ui.append(part.instructions); // TODO:
				}
			}

			var ui = clone(me.ui.asmt_review_template_question, ["asmt_review_template_question_body", "asmt_review_template_question_position", "asmt_review_template_question_position_ui",
			                                                     "asmt_review_template_question_scorePct", "asmt_review_template_question_scorePct_ui",
			                                                     "asmt_review_template_question_scorePts", "asmt_review_template_question_scorePts_ui",
			                                                     "asmt_review_template_question_qa",
			                                                     "asmt_review_template_question_evaluation", "asmt_review_template_question_evaluation_official", "asmt_review_template_question_evaluation_peer",
			                                                     "asmt_review_template_question_evaluation_comment"]);
			if (me.submission.answers.length > 1)
			{
				// TODO: switch from questionNumber to question.ordering
				ui.asmt_review_template_question_position.text(main.i18n.lookup("msg_questionPosition", "%0 of %1", "html", [questionNumber++, (partNumQuestions != null ? partNumQuestions : me.submission.answers.length)]));
			}
			show(ui.asmt_review_template_question_position_ui, (me.submission.answers.length > 1));

			if (me.assessment.evaluationDesign.forPoints)
			{
				ui.asmt_review_template_question_scorePct.text(asPct(answer.evaluation.score, question.evaluationDesign.points));
				ui.asmt_review_template_question_scorePts.text(main.i18n.lookup("msg_scoreOfPoints", "%0 / %1", "html", [answer.evaluation.score, question.evaluationDesign.points]));
			}
			show([ui.asmt_review_template_question_scorePct_ui, ui.asmt_review_template_question_scorePts_ui], (me.assessment.evaluationDesign.forPoints));

			if (answer.evaluation.comment != null)
			{
				ui.asmt_review_template_question_evaluation_comment.html(answer.evaluation.comment);
				show(ui.asmt_review_template_question_evaluation_official, (answer.evaluation.type == main.EvaluationType.official));
				show(ui.asmt_review_template_question_evaluation_peer, (answer.evaluation.type == main.EvaluationType.peer));
			}
			show(ui.asmt_review_template_question_evaluation, (answer.evaluation.comment != null));

			me.populateQuestion(ui.asmt_review_template_question_qa, question, answer)
			
			me.ui.asmt_review_review_ui.append(ui.asmt_review_template_question_body);
		});
		
		// overall evaluation
		main.ui.evaluation.review.set(me.ui.asmt_review_evaluation_ui, me.assessment.evaluationDesign, me.submission.evaluation, main.rubrics);
	};

	this.populateQuestion = function(area, question, answer)
	{
		if (question.type == "mneme:Essay")
		{
			area.append(me.populateEssay(question, answer));
		}
		else if (question.type == "mneme:TrueFalse")
		{
			area.append(me.populateTrueFalse(question, answer));
		}
		else if (question.type == "mneme:MultipleChoice")
		{
			area.append(me.populateMultipleChoice(question, answer));
		}
		else if (question.type == "mneme:LikertScale")
		{
			area.append(me.populateMultipleChoice(question, answer));
		}
		else if (question.type == "mneme:FillBlanks")
		{
			area.append(me.populateFillin(question, answer));
		}
		else if (question.type == "mneme:FillInline")
		{
			area.append(me.populateFillin(question, answer));
		}
		else
		{
			// TODO: unhandled question type
			area.append("<div>SOME QUESTION</div>");
		}
	};

	this.populateEssay = function(question, answer)
	{
		var ui = clone(me.ui.asmt_review_template_essay, ["asmt_review_template_essay_body", "asmt_review_template_essay_instructions", "asmt_review_template_essay_content"]);

		ui.asmt_review_template_essay_instructions.html(question.essay.question);		
		ui.asmt_review_template_essay_content.html(answer.essay.answer);

		return ui.asmt_review_template_essay_body;
	};

	this.populateTrueFalse = function(question, answer)
	{
		var ui = clone(me.ui.asmt_review_template_tf, ["asmt_review_template_tf_body", "asmt_review_template_tf_instructions",
		                                               "asmt_review_template_tf_true", "asmt_review_template_tf_false",
		                                               "asmt_review_template_tf_true_mark", "asmt_review_template_tf_false_mark"
		                                               ]);

		ui.asmt_review_template_tf_instructions.html(question.tf.question);
		ui.asmt_review_template_tf_true.prop("name", "tf_" + question.id);
		ui.asmt_review_template_tf_false.prop("name", "tf_" + question.id);
		if (answer.tf.answer !== undefined)
		{
			if (answer.tf.answer)
			{
				ui.asmt_review_template_tf_true.prop("checked", true);
				ui.asmt_review_template_tf_true_mark.append(me.mark(answer.tf.correct));
			}
			else
			{
				ui.asmt_review_template_tf_false.prop("checked", true);
				ui.asmt_review_template_tf_false_mark.append(me.mark(answer.tf.correct));
			}
		}
		else
		{
			if (question.tf.answer)
			{
				ui.asmt_review_template_tf_true_mark.append(me.indicate());
			}
			else
			{
				ui.asmt_review_template_tf_false_mark.append(me.indicate());
			}
		}

		return ui.asmt_review_template_tf_body;
	};

	this.populateMultipleChoice = function(question, answer)
	{
		var ui = clone(me.ui.asmt_review_template_mc, ["asmt_review_template_mc_body", "asmt_review_template_mc_instructions", "asmt_review_template_mc_choices"]);

		ui.asmt_review_template_mc_instructions.html(question.mc.question);

		$.each(question.mc.choices, function(index, choice)
		{
			var choiceUi = clone(me.ui.asmt_review_template_mc_entry, ["asmt_review_template_mc_entry_body", "asmt_review_template_mc_entry_mark", "asmt_review_template_mc_entry_radio", "asmt_review_template_mc_entry_label"]);
			ui.asmt_review_template_mc_choices.append(choiceUi.asmt_review_template_mc_entry_body);

			choiceUi.asmt_review_template_mc_entry_radio.prop("name", "mc_" + question.id);
			choiceUi.asmt_review_template_mc_entry_label.html(choice.text);
			if (answer.mc.answer == choice.answer)
			{
				choiceUi.asmt_review_template_mc_entry_radio.prop("checked", true);
				if (answer.mc.correct != undefined) choiceUi.asmt_review_template_mc_entry_mark.append(me.mark(answer.mc.correct));
			}
			else if ((choice.correct !== undefined) && (choice.correct))
			{
				choiceUi.asmt_review_template_mc_entry_mark.append(me.indicate());
			}
		});

		return ui.asmt_review_template_mc_body;
	};

	this.populateFillin = function(question, answer)
	{		
		var ui = clone(me.ui.asmt_review_template_fillin, ["asmt_review_template_fillin_body", "asmt_review_template_fillin_results"]);
		
		var text = "";
		$.each(question.fillin.segments, function(index, segment)
		{
			if (segment.text !== undefined)
			{
				text += segment.text;
			}
			else
			{
				text += "<span style='margin-left:8px;'>" + me.mark(answer.fillin.answers[segment.fillin].correct)[0].outerHTML + "</span>";
				// text += "<span style='margin-left:4px; background-color:#D8D8D8; text-decoration:underline; padding-left:4px; padding-right:4px;'>" + answer.fillin.answers[segment.fillin].text + "</span>";
				// text += "<input disabled class='e3_input' value='" + answer.fillin.answers[segment.fillin].text + "' type='text' style='color:#" + ((answer.fillin.answers[segment.fillin].correct) ? "2AB31D" : "E00000") + ";'>"
				
				if (answer.fillin.answers[segment.fillin].correct)
				{
					text += "<span class='e3_disabledInput' style='color:#2AB31D;'>" + answer.fillin.answers[segment.fillin].text + "</span>";
				}
				else
				{
					text += "<span class='e3_disabledInput'><span style='color:#E00000;'>" + answer.fillin.answers[segment.fillin].text + "</span>"
						+ "<span style='margin-left:16px; color:#0072C6; font-style:normal;'>" + me.indicate()[0].outerHTML
						+ question.fillin.answers[segment.fillin] + "</span></span>";
				}
			}
		});
		
		ui.asmt_review_template_fillin_results.html(text);

		return ui.asmt_review_template_fillin_body;
	};

	this.indicate = function()
	{
		var ui = clone(me.ui.asmt_review_template_marking_indicate, ["asmt_review_template_marking_indicate_body"]);
		return ui.asmt_review_template_marking_indicate_body;
	};

	this.mark = function(correct)
	{
		if (correct)
		{
			var ui = clone(me.ui.asmt_review_template_marking_correct, ["asmt_review_template_marking_correct_body"]);
			return ui.asmt_review_template_marking_correct_body;
		}
		else
		{
			var ui = clone(me.ui.asmt_review_template_marking_incorrect, ["asmt_review_template_marking_incorrect_body"]);
			return ui.asmt_review_template_marking_incorrect_body;
		}
	};

	this.goBack = function()
	{
		main.startView();
	};
	
	this.goSubmission = function(id)
	{
		main.startReview(id);
	};
};

function AssessmentGradeAssessment(main)
{
	var me = this;

	this.ui = null;
	this.assessment = null;
	this.assessments = null;
	this.submissionsSorted = [];
	this.edit = null;
	this.sectionFilter = null;
	this.bestSubmssionOnly = false;
	this.sections = [];
	this.sortDirection = "A";
	this.sortMode = "N";

	this.bulkRelease = {evaluated: "E", all: "A"};

	this.init = function()
	{
		me.ui = findElements(["asmt_gradeAsmt_actions", "asmt_gradeAsmt_title", "asmt_gradeAsmt_points", "asmt_gradeAsmt_due", "asmt_gradeAsmt_allow",
		                      "asmt_gradeAsmt_releaseEvaluated", "asmt_gradeAsmt_releaseAll", "asmt_gradeAsmt_commentAll", "asmt_gradeAsmt_importScores", "asmt_gradeAsmt_adjustScores",
		                      "asmt_gradeAsmt_table", "asmt_gradeAsmt_none", "asmt_gradeAsmt_title_template",
		                      "asmt_gradeAsmt_commentDialog", "asmt_gradeAsmt_commentDialog_comment",
		                      "asmt_gradeAsmt_scoresDialog", "asmt_gradeAsmt_scoresDialog_adjustScores", "asmt_gradeAsmt_scoresDialog_deductLate", "asmt_gradeAsmt_scoresDialog_zeroNonSubmit",
		                      "asmt_gradeAsmt_importDialog", "asmt_gradeAsmt_importDialog_import", "asmt_gradeAsmt_importDialog_help"]);
		me.ui.itemNav = new e3_ItemNav();
		me.ui.table = new e3_Table(me.ui.asmt_gradeAsmt_table);
		onClick(me.ui.asmt_gradeAsmt_releaseEvaluated, function(){me.release(me.bulkRelease.evaluated);});
		onClick(me.ui.asmt_gradeAsmt_releaseAll, function(){me.release(me.bulkRelease.all);});
		onClick(me.ui.asmt_gradeAsmt_commentAll, me.commentAll);
		onClick(me.ui.asmt_gradeAsmt_importScores, me.importScores);
		onClick(me.ui.asmt_gradeAsmt_adjustScores, me.adjustScores);
		setupHoverControls([me.ui.asmt_gradeAsmt_releaseEvaluated, me.ui.asmt_gradeAsmt_releaseAll,
		                    me.ui.asmt_gradeAsmt_commentAll, me.ui.asmt_gradeAsmt_importScores, me.ui.asmt_gradeAsmt_adjustScores, me.ui.asmt_gradeAsmt_importDialog_help]);
	};

	this.start = function(assessment, assessments)
	{
		main.onExit = me.checkExit;
		if (assessments !== undefined) me.assessments = assessments;

		me.load(assessment.id);
	};

	this.load = function(id)
	{
		var params = main.portal.cdp.params();
		params.url.site = main.portal.site.id;
		params.url.assessment = id;

		main.portal.cdp.request("assessment_getGradeAssessment", params, function(data)
		{
			me.assessment = data.assessment;
			me.submissions = data.submissions;

			me.ui.itemNav.inject(main.ui.asmt_itemnav, {returnFunction: me.goBack, pos: position(me.assessment, me.assessments), navigateFunction: me.goAssessment});

			me.sectionFilter = null;
			me.extractSections();
			me.populateFilters();
			me.submissionsSorted = me.sortBy(me.submissions, me.sortDirection, me.sortMode);

			me.makeEdit();
			me.populate();
		});
	};

	this.makeEdit = function()
	{
		var scores = {submissions:[]};
		$.each(me.submissionsSorted, function(index, submission)
		{
			scores.submissions.push({id: submission.id, score: submission.evaluation.score, evaluated: submission.evaluation.evaluated, released: submission.evaluation.released});
		});
		me.edit = new e3_Edit(scores, [], function(changed)
		{
			main.ui.modebar.enableSaveDiscard(changed ? me.saveCancel : null);
			me.ui.itemNav.enableSave(changed ? me.saveCancel : null);
		});
		me.edit.setFilters({"submissions[].score": me.edit.numberFilter});

		main.ui.modebar.enableSaveDiscard(null);
		me.ui.itemNav.enableSave(null);
	};

	this.populateFilters = function()
	{
		me.ui.asmt_gradeAsmt_actions.empty();

		// best only or all
		var options = [{value: false, title: main.i18n.lookup("msg_allSubmissions", "All")}, {value: true, title: main.i18n.lookup("msg_bestSubmissions", "Best Only")}];
		me.ui.view = new e3_SortAction();
		me.ui.view.inject(me.ui.asmt_gradeAsmt_actions,
				{onSort: function(dir, val){me.viewSubmissions(val);}, label: main.i18n.lookup("header_viewSubmissions", "SUBMISSIONS"), options: options, initial: me.bestSubmssionOnly});

		// sections
		var options = [{value: "*", title: main.i18n.lookup("msg_allSections", "All")}];
		$.each(me.sections, function(index, section)
		{
			options.push({value: section === undefined ? "-" : section, title: section === undefined ? main.i18n.lookup("msg_noSection", "none") : section});
		});
		me.ui.section = new e3_SortAction();
		me.ui.section.inject(me.ui.asmt_gradeAsmt_actions,
				{onSort: function(dir, val){me.filterBySection(val);}, label: main.i18n.lookup("header_section", "SECTION"), options: options, initial: ((me.sectionFilter == null) ? "*" : me.sectionFilter)});
		
		// sort
		me.ui.sort = new e3_SortAction();
		me.ui.sort.inject(me.ui.asmt_gradeAsmt_actions,
				{onSort: me.onSort, options:[{value:"N", title:main.i18n.lookup("sort_name", "NAME")},{value:"S", title:main.i18n.lookup("sort_section", "SECTION")},
				                             {value:"F", title:main.i18n.lookup("sort_finished", "FINISHED")},{value:"I", title:main.i18n.lookup("sort_final", "FINAL")},
				                             {value:"E", title:main.i18n.lookup("sort_evaluated", "EVALUATED")},{value:"R", title:main.i18n.lookup("sort_released", "RELEASED")}]});
		me.ui.sort.directional(true);
	};

	this.populate = function()
	{
		me.ui.asmt_gradeAsmt_points.text(((me.assessment.evaluationDesign.forPoints) ? me.assessment.evaluationDesign.points : ""));
		me.ui.asmt_gradeAsmt_title.text(me.assessment.title);
		me.ui.asmt_gradeAsmt_due.text((me.assessment.schedule.due == null) ? "-" : main.portal.timestamp.display(me.assessment.schedule.due));
		me.ui.asmt_gradeAsmt_allow.text((me.assessment.schedule.allowUntil == null) ? "-" : main.portal.timestamp.display(me.assessment.schedule.allowUntil));

		me.ui.table.clear();
		$.each(me.submissionsSorted, function(index, submission)
		{
			var inTheEdit = findIdInList(submission.id, me.edit.submissions);

			// filter by section
			if ((me.sectionFilter != null) && (((submission.userRoster === undefined) ? "-" : submission.userRoster) != me.sectionFilter)) return;

			// filter by best only submission
			if (me.bestSubmssionOnly && ((submission.best === undefined) || (!submission.best))) return;

			me.ui.table.row();
			
			var cell = clone(me.ui.asmt_gradeAsmt_title_template, ["asmt_gradeAsmt_title_template_body", "asmt_gradeAsmt_title_template_name", "asmt_gradeAsmt_title_template_iid"]);
			cell.asmt_gradeAsmt_title_template_name.text(submission.userNameSort);
			if (!submission.complete)
			{
				cell.asmt_gradeAsmt_title_template_name.css({color: "#A8A8A8"});
				cell.asmt_gradeAsmt_title_template_iid.css({color: "#A8A8A8"});
			}
			if (submission.userIid != null) cell.asmt_gradeAsmt_title_template_iid.text(main.i18n.lookup("msg_iid", "ID: %0", "html", [submission.userIid]));
			
			// name - click to grade if complete
			if (submission.complete)
			{
				me.ui.table.hotElement(cell.asmt_gradeAsmt_title_template_body, main.i18n.lookup("msg_grade", "Grade %0", "html", [submission.userNameSort]), function()
				{
					me.grade(submission);
				}, null, {width:"calc(100vw - 100px - 642px)", minWidth:"calc(1200px - 100px - 642px)"});
			}
			else
			{
				me.ui.table.element(cell.asmt_gradeAsmt_title_template_body, null, {width:"calc(100vw - 100px - 642px)", minWidth:"calc(1200px - 100px - 642px)" });
			}
			
			me.ui.table.text(((submission.userRoster == null) ? "-" : submission.userRoster), null, {width:60});

			me.ui.table.date(submission.started, "-", "date2l");
			var area = me.ui.table.date(submission.finished, (submission.inProgress ? main.i18n.lookup("msg_inProgress", "<i>in progress</i>") : "-") , "date2l").find("div");
			if (submission.late ||  (submission.status == "A")) area.append($("<br />"));
			if (submission.late) area.append($("<span />").text(main.i18n.lookup("msg_late", "late")).addClass("e3_text special").css({color: "#E00000", fontSize: 9, paddingLeft: 4, paddingRight: 4}));
			if (submission.status == "A") area.append($("<span />").text(main.i18n.lookup("msg_auto", "auto")).addClass("e3_text special").css({color: "#E00000", fontSize: 9, paddingLeft: 4, paddingRight: 4}));

			if (submission.evaluation != null)
			{
				if (submission.inProgress)
				{
					me.ui.table.text("-", null, {width:60});
					me.ui.table.text("-", null, {width:60});
				}
				else
				{
					if (submission.autoScore == null)
					{
						me.ui.table.text("-", null, {width:60});
					}
					else
					{
						me.ui.table.text(submission.autoScore, null, {width:60});
					}

					var input = me.ui.table.input({size: 2, type: "text"}, null, {width: 60}).find("input");
					input.css({fontSize: 13, width: 32});
					me.edit.setupFilteredEdit(input, inTheEdit, "score");
				}

				if (submission.complete)
				{
					input = me.ui.table.input({type: "checkbox"}, null, {width: 75}).find("input");
					me.edit.setupCheckEdit(input, inTheEdit, "evaluated");
	
					input = me.ui.table.input({type: "checkbox"}, null, {width: 75}).find("input");
					me.edit.setupCheckEdit(input, inTheEdit, "released");
				}
				else
				{
					me.ui.table.text("-", null, {width:75});
					me.ui.table.text("-", null, {width:75});
				}				
			}
			else
			{
				me.ui.table.text("-", null, {width:60});
				me.ui.table.text("-", null, {width:60});
				me.ui.table.text("-", null, {width:75});
				me.ui.table.text("-", null, {width:75});
			}
			
			me.ui.table.contextMenu(
			[
				{title: main.i18n.lookup("cm_specialAccess", "Special Access"), action:function(){console.log("special access");}},
				{condition: (submission.complete)},
				{title: main.i18n.lookup("cm_grade", "Grade"), action:function(){me.grade(submission);}, condition: (submission.complete)}
	        ]);
		});
		me.ui.table.done();
		show(me.ui.asmt_gradeAsmt_none, (me.ui.table.rowCount() == 0));
	};

	this.saveCancel = function(mode, deferred)
	{
		if (mode)
		{
			me.save(deferred);
		}
		else
		{
			me.edit.revert();
			me.populate();
			if (deferred !== undefined) deferred();
		}
	};
	
	this.checkExit = function(deferred)
	{
		if (me.edit.changed())
		{
			main.portal.confirmNavigationWithChanges(function()
			{
				me.save(deferred);				
			}, function()
			{
				me.edit.revert();
				if (deferred !== undefined) deferred();
			});

			return false;
		}

		return true;
	};

	this.save = function(deferred)
	{
		var params = main.portal.cdp.params();
		params.url.site = main.portal.site.id;
		params.url.assessment = me.assessment.id;
		me.edit.params("", params);
		main.portal.cdp.request("assessment_saveGradeAssessment assessment_getGradeAssessment", params, function(data)
		{
			me.assessment = data.assessment;
			me.submissions = data.submissions;
			me.submissionsSorted = me.sortBy(me.submissions, me.sortDirection, me.sortMode);

			me.makeEdit();
			me.populate();

			if (deferred !== undefined) deferred();
		});
	};

	this.goBack = function()
	{
		// main.startGrade();
		main.startManage();
	};

	this.goAssessment = function(assessment)
	{
		main.startGradeAssessment(assessment);
	};

	this.grade = function(submission)
	{
		// check here so we have a chance, after saving, to pick up an updated (by save) submission
		if (!me.checkExit(function(){me.grade(submission);})) return;

		// pick up an updated (by save) submission
		submission = findIdInList(submission.id, me.submissions);

		main.startGradeSubmission(submission, me.submissionsSorted, me.assessment);
	};

	this.extractSections = function()
	{
		me.sections = [];
		$.each(me.submissions, function(index, submission)
		{
			// if (member.adhoc) return;

			if (me.sections.indexOf(submission.userRoster) == -1)
			{
				me.sections.push(submission.userRoster);
			}
		});
		me.sections.sort();
	};

	this.filterBySection = function(section)
	{
		var filter = (section == "*") ? null : section;
		if (filter != me.sectionFilter)
		{
			me.sectionFilter = filter;
			me.populate();
		}
	};

	this.onSort = function(direction, option)
	{
		me.sortDirection = direction;
		me.sortMode = option;
		me.ui.sort.directional(true);

		me.submissionsSorted = me.sortBy(me.submissions, me.sortDirection, me.sortMode);
		me.populate();
	};

	this.sortBy = function(submissions, direction, option)
	{
		var sorted = [];

		if (option == "N")
		{
			sorted = main.sortSubmissionsByName(submissions, direction);
		}
		else if (option == "S")
		{
			sorted = main.sortSubmissionsByRoster(submissions, direction);
		}
		else if (option == "F")
		{
			sorted = main.sortSubmissionsByFinished(submissions, direction);
		}
		else if (option == "I")
		{
			sorted = main.sortSubmissionsByFinal(submissions, direction);
		}
		else if (option == "E")
		{
			sorted = main.sortSubmissionstByEvaluated(submissions, direction);
		}
		else if (option == "R")
		{
			sorted = main.sortSubmissionsByReleased(submissions, direction);
		}
		
		return sorted;
	};
	
	this.viewSubmissions = function(val)
	{
		me.bestSubmssionOnly = val;
		me.populate();
	};

	this.release = function(what)
	{
		if (!me.checkExit(function(){me.release(what);})) return;

		var params = main.portal.cdp.params();
		params.url.site = main.portal.site.id;
		params.url.assessment = me.assessment.id;
		params.url.criteria = what;
		main.portal.cdp.request("assessment_release assessment_getGradeAssessment", params, function(data)
		{
			me.assessment = data.assessment;
			me.submissionsSorted = me.sortBy(me.assessment.submissions, me.sortDirection, me.sortMode);
			me.makeEdit();
			me.populate();
		});
	};

	this.commentAll = function()
	{
		if (!me.checkExit(function(){me.commentAll();})) return;

		me.ui.asmt_gradeAsmt_commentDialog_comment.empty();
		var editor = new e3_EditorCK(me.ui.asmt_gradeAsmt_commentDialog_comment, {height: 100});
		editor.disable();
		editor.set("");
		editor.enable(function(){}, true, main.fs);

		main.portal.dialogs.openDialogButtons(me.ui.asmt_gradeAsmt_commentDialog,
		[
			{text: main.i18n.lookup("action_comment", "Comment"), click: function()
			{
				var params = main.portal.cdp.params();
				params.url.site = main.portal.site.id;
				params.url.assessment = me.assessment.id;
				params.post.comment = editor.get();	
				main.portal.cdp.request("assessment_commentAll assessment_getGradeAssessment", params, function(data)
				{
					me.assessment = data.assessment;
					me.submissions = data.submissions;
					me.submissionsSorted = me.sortBy(me.submissions, me.sortDirection, me.sortMode);

					me.makeEdit();
					me.populate();
				});

				return true;
			}},
		], null, main.i18n.lookup("action_cancel", "Cancel"));
	};

	// TODO: needs major work - a whole confirm (/adjust?) view
	this.importScores = function()
	{
		if (!me.checkExit(function(){me.importScores();})) return;

		me.ui.asmt_gradeAsmt_importDialog_import.val("");
		me.ui.asmt_gradeAsmt_importDialog_import.focus();

		main.portal.dialogs.openDialogButtons(me.ui.asmt_gradeAsmt_importDialog,
		[
			{text: main.i18n.lookup("action_import", "Import"), click: function()
			{
				var fl = me.ui.asmt_gradeAsmt_importDialog_import.prop("files");
				if ((fl != null) && (fl.length > 0))
				{
					var params = main.portal.cdp.params();
					params.url.site = main.portal.site.id;
					params.url.assessment = me.assessment.id;
					params.post.scoresFile = fl[0];
					main.portal.cdp.request("assessment_importScores assessment_getGradeAssessment", params, function(data)
					{
						me.assessment = data.assessment;
						me.submissions = data.submissions;
						me.submissionsSorted = me.sortBy(me.submissions, me.sortDirection, me.sortMode);

						me.makeEdit();
						me.populate();
					});
				}
				else
				{
					// TODO: red something
					return false;
				}

				return true;
			}},
		], null, main.i18n.lookup("action_cancel", "Cancel"));
	};

	this.adjustScores = function()
	{
		if (!me.checkExit(function(){me.adjustScores();})) return;

		me.ui.asmt_gradeAsmt_scoresDialog_adjustScores.val("");
		me.ui.asmt_gradeAsmt_scoresDialog_deductLate.val("");
		me.ui.asmt_gradeAsmt_scoresDialog_zeroNonSubmit.prop("checked", false);

		main.portal.dialogs.openDialogButtons(me.ui.asmt_gradeAsmt_scoresDialog,
		[
			{text: main.i18n.lookup("action_adjust", "Adjust"), click: function()
			{
				var params = main.portal.cdp.params();
				params.url.site = main.portal.site.id;
				params.url.assessment = me.assessment.id;
				params.post.adjustAll = me.ui.asmt_gradeAsmt_scoresDialog_adjustScores.val();
				params.post.deductLate = me.ui.asmt_gradeAsmt_scoresDialog_deductLate.val();
				params.post.zeroNonSubmits = me.ui.asmt_gradeAsmt_scoresDialog_zeroNonSubmit.is(":checked");

				main.portal.cdp.request("assessment_adjustScores assessment_getGradeAssessment", params, function(data)
				{
					me.assessment = data.assessment;
					me.submissions = data.submissions;
					me.submissionsSorted = me.sortBy(me.submissions, me.sortDirection, me.sortMode);

					me.makeEdit();
					me.populate();
				});

				return true;
			}},
		], null, main.i18n.lookup("action_cancel", "Cancel"));
	};
};

function AssessmentGradeSubmission(main)
{
	var me = this;

	this.ui = null;
	this.submission = null;
	this.submissions = null;
	this.assessment = null;
	this.edit = null;

	this.init = function()
	{
		me.ui = findElements(["asmt_gradeSubmission_answer", "asmt_gradeSubmission_grade",
		                      "asmt_gradeSubmission_template_essay",
		                      "asmt_gradeSubmission_points", "asmt_gradeSubmission_title", "asmt_gradeSubmission_due", "asmt_gradeSubmission_allow",
		                      "asmt_gradeSubmission_userName", "asmt_gradeSubmission_started", "asmt_gradeSubmission_finished"]);
		me.ui.itemNav = new e3_ItemNav();
	};

	this.start = function(submission, submissions, assessment)
	{
		if (submissions !== undefined)
		{
			// we want only submissions we can go into grading with... submission.complete
			me.submissions = [];
			$.each(submissions, function(index, submission)
			{
				if (submission.complete) me.submissions.push(submission);
			});
		}

		if (assessment !== undefined) me.assessment = assessment; // just to be optional and to use the id

		main.onExit = me.checkExit;
	
		me.load(submission.id, me.assessment.id);
	};

	this.load = function(submissionId, assessmentId)
	{
		var params = main.portal.cdp.params();
		params.url.site = main.portal.site.id;
		params.url.assessment = assessmentId;
		params.url.submission = submissionId;

		main.portal.cdp.request("assessment_getGradeSubmission", params, function(data)
		{
			me.assessment = data.assessment;
			me.submission = data.submission;
			me.ui.itemNav.inject(main.ui.asmt_itemnav, {doneFunction: me.done, pos: position(me.submission, me.submissions), navigateFunction: me.goSubmission});

			me.makeEdit();
			me.populate();
		});
	};

	this.makeEdit = function()
	{
		me.edit = new e3_Edit(me.submission.evaluation, ["type", "mayReview", "reviewedOn", "needsReview"], function(changed) // TODO: tune: "createdOn", "createdBy", "modifiedOn", "modifiedBy", "id", "submittedOn",
		{
			main.ui.modebar.enableSaveDiscard(changed ? me.saveCancel : null);
			me.ui.itemNav.enableSave(changed ? me.saveCancel : null);
		});

		main.ui.modebar.enableSaveDiscard(null);
		me.ui.itemNav.enableSave(null);
	};

	this.populate = function()
	{
		me.ui.asmt_gradeSubmission_userName.text(me.submission.userName);
		me.ui.asmt_gradeSubmission_started.text((me.submission.started == null) ? "-" : main.portal.timestamp.display(me.submission.started));
		
		if (me.submission.finished == null)
		{
			me.ui.asmt_gradeSubmission_finished.text("-");
		}
		else
		{
			me.ui.asmt_gradeSubmission_finished.text(main.portal.timestamp.display(me.submission.finished));

			if (me.submission.late) me.ui.asmt_gradeSubmission_finished.append($("<span />").text(main.i18n.lookup("msg_late", "late")).addClass("e3_text special").css({color: "#E00000", fontSize: 9, paddingLeft: 4, textTransform: "uppercase"}));
			if (me.submission.status == "A") me.ui.asmt_gradeSubmission_finished.append($("<span />").text(main.i18n.lookup("msg_auto", "auto")).addClass("e3_text special").css({color: "#E00000", fontSize: 9, paddingLeft: 4, textTransform: "uppercase"}));
		}

		me.ui.asmt_gradeSubmission_points.text(((me.assessment.evaluationDesign.forPoints) ? me.assessment.evaluationDesign.points : ""));
		me.ui.asmt_gradeSubmission_title.text(me.assessment.title);
		me.ui.asmt_gradeSubmission_due.text((me.assessment.schedule.due == null) ? "-" : main.portal.timestamp.display(me.assessment.schedule.due));
		me.ui.asmt_gradeSubmission_allow.text((me.assessment.schedule.allowUntil == null) ? "-" : main.portal.timestamp.display(me.assessment.schedule.allowUntil));

		// TODO: show the submission!  we have submission.answers, we need questions (see review)
//		switch (me.assessment.type)
//		{
//			case /* main.AssessmentType.essay */ "E":
//			{
//				me.ui.asmt_gradeSubmission_answer.empty().append(me.populateEssay());
//				break;
//			}
//
//			default:
//			{
				me.ui.asmt_gradeSubmission_answer.empty().text("QUESTION / ANSWER AREA");
//			}
//		}

		main.ui.evaluation.grade.set(me.ui.asmt_gradeSubmission_grade, me.assessment.evaluationDesign, me.edit, me.edit, main.rubrics, main.fs);
	};

//	this.populateEssay = function() // TODO: need to restore data for this
//	{
//		var ui = clone(me.ui.asmt_gradeSubmission_template_essay, ["asmt_gradeSubmission_template_essay_body", "asmt_gradeSubmission_template_essay_instructions", "asmt_gradeSubmission_template_essay_answer"]);
//
//		ui.asmt_gradeSubmission_template_essay_instructions.html(me.assessment.design.instructions);
//		
//		me.answer = me.submission.answers[0];
//		ui.asmt_gradeSubmission_template_essay_answer.html(me.answer.content);
//
//		return ui.asmt_gradeSubmission_template_essay_body;
//	};

	this.done = function()
	{
		if (me.edit.changed())
		{
			me.save(function(){main.startGradeAssessment(me.assessment);})
		}
		else
		{
			main.startGradeAssessment(me.assessment);
		}
	};
	
	this.goSubmission = function(submission)
	{
		main.startGradeSubmission(submission);
	};

	this.saveCancel = function(mode, deferred)
	{
		if (mode)
		{
			me.save(deferred);
		}
		else
		{
			me.edit.revert();
			me.populate();
			if (deferred !== undefined) deferred();
		}
	};

	this.checkExit = function(deferred)
	{
		if (me.edit.changed())
		{
			main.portal.confirmNavigationWithChanges(function()
			{
				me.save(deferred);				
			}, function()
			{
				me.edit.revert();
				if (deferred !== undefined) deferred();
			});

			return false;
		}

		return true;
	};

	this.save = function(deferred)
	{
		var params = main.portal.cdp.params();
		params.url.site = main.portal.site.id;
		params.url.submission = me.submission.id;
		me.edit.params("evaluation_", params); // TODO: per-answer evaluations
		main.portal.cdp.request("assessment_evaluate", params, function(data)
		{
			me.submission.evaluation = data.evaluation;
			me.makeEdit();
			me.populate();
			if (deferred !== undefined) deferred();
		});
	};
}

function AssessmentPools(main)
{
	var me = this;
	
	this.pools = null;
	this.poolsSorted = null;
	this.sortDirection = "A";
	this.sortMode = "T";

	this.init = function()
	{
		me.ui = findElements(["asmt_pools_add", "asmt_pools_view", "asmt_pools_combine", "asmt_pools_delete", "asmt_pools_import", "asmt_pools_actions",
		                      "asmt_pools_table", "asmt_pools_none"]);

		me.ui.sort = new e3_SortAction();
		me.ui.sort.inject(me.ui.asmt_pools_actions,
				{onSort: me.onSort, initial: me.sortMode,
					options:[{value:"T", title:main.i18n.lookup("sort_title", "TITLE")},
					         {value:"D", title:main.i18n.lookup("sort_created", "CREATED")}]});
		me.ui.sort.directional(true);

		me.ui.table = new e3_Table(me.ui.asmt_pools_table);
		me.ui.table.setupSelection("asmt_pools_select", me.updateActions);
		me.ui.table.selectAllHeader(1, main.ui.asmt_header_pool);
	
		onClick(me.ui.asmt_pools_add, me.add);
		onClick(me.ui.asmt_pools_view, me.view);
		onClick(me.ui.asmt_pools_combine, me.combine);
		onClick(me.ui.asmt_pools_delete, me.remove);
		onClick(me.ui.asmt_pools_import, me.import);

		setupHoverControls([me.ui.asmt_pools_add, me.ui.asmt_pools_view, me.ui.asmt_pools_combine, me.ui.asmt_pools_delete, me.ui.asmt_pools_import]);
	};
	
	this.start = function()
	{
		main.onExit = me.checkExit;
		me.load();
	};

	this.load = function()
	{
		var params = main.portal.cdp.params();
		params.url.site = main.portal.site.id;
		main.portal.cdp.request("assessment_getPools", params, function(data)
		{
			me.pools = data.pools;
			me.poolsSorted = me.sortPools(me.pools, me.sortDirection, me.sortMode);

			me.populate();
		});
	};

	this.populate = function()
	{
		me.ui.table.clear();
		$.each(me.poolsSorted, function(index, pool)
		{
			me.ui.table.row();
			me.ui.table.selectBox(pool.id);

			if (pool.description != null)
			{
				var cell = clone(main.ui.asmt_title_template, ["asmt_title_template_body", "asmt_title_template_title", "asmt_title_template_msg"]);
				me.ui.table.hotElement(cell.asmt_title_template_body, main.i18n.lookup("msg_editPool", "edit pool"), function(){me.edit(pool);}, null, {width:"calc(100vw - 100px - 527px)", minWidth:"calc(1200px - 100px - 527px"});
				cell.asmt_title_template_title.text(pool.title);
				cell.asmt_title_template_msg.text(pool.description); // TODO: .stripHtml() ???
			}
			else
			{
				me.ui.table.hotText(pool.title, main.i18n.lookup("msg_editPool", "edit pool"), function(){me.edit(pool);}, null, {width: "calc(100vw - 100px - 527px)", minWidth:"calc(1200px - 100px - 527px)"});
			}

			me.ui.table.text(pool.numQuestions, null, {width: 75});
			me.ui.table.text(pool.points, null, {width: 75});
			me.ui.table.text(pool.difficulty, null, {width: 75});
			me.ui.table.date(pool.attribution.createdOn, "-");

			me.ui.table.contextMenu(
			[
				{title: main.i18n.lookup("cm_duplicate", "Duplicate"), action:function(){me.duplicate(pool);}},
				{},
				{title: main.i18n.lookup("cm_edit", "Edit"), action:function(){me.edit(pool);}},
				{},
				{title: main.i18n.lookup("cm_delete", "Delete"), action:function(){me.removeA(pool);}}
	        ]);
		});
		me.ui.table.done();
		show(me.ui.asmt_pools_none, (me.ui.table.rowCount() == 0));
	};

	this.onSort = function(direction, option)
	{
		me.sortDirection = direction;
		me.sortMode = option;
		me.ui.sort.directional(true);

		me.poolsSorted = me.sortPools(me.pools, me.sortDirection, me.sortMode);

		me.populate();
	};

	this.sortPools = function(pools, direction, mode)
	{
		var sorted = [];

		if (mode == "T")
		{
			sorted = me.sortPoolsByTitle(pools, direction);
		}
		else if (mode == "D")
		{
			sorted = me.sortPoolsByDate(pools, direction);
		}
		
		return sorted
	};

	this.sortPoolsByTitle = function(pools, direction)
	{
		var sorted = [].concat(pools);
		var adjust = (direction == 'A') ? 1 : -1;
		sorted.sort(function(a, b)
		{
			var rv = adjust * compareS(a.title, b.title);
			if (rv == 0)
			{
				rv = compareN(a.id, b.id);
			}
			return rv;
		});

		return sorted;
	};

	this.sortPoolsByDate = function(pools, direction)
	{
		var sorted = [].concat(pools);
		var adjust = (direction == 'A') ? 1 : -1;
		sorted.sort(function(a, b)
		{
			var rv = adjust * compareN(a.attribution.createdOn, b.attribution.createdOn);
			if (rv == 0)
			{
				rv = compareS(a.title, b.title);
				if (rv == 0)
				{
					rv = compareN(a.id, b.id);
				}
			}
			return rv;
		});

		return sorted;
	};

	this.updateActions = function()
	{
		me.ui.table.updateActions([me.ui.asmt_pools_view], [me.ui.asmt_pools_combine, me.ui.asmt_pools_delete]); // TODO: combine needs two?
	};
	
	this.edit = function(pool)
	{
		main.startEditPool(pool, me.poolsSorted);
	};
}

function AssessmentEditPool(main)
{
	var me = this;
	
	this.ui = null;

	this.pool = null;
	this.pools = null;
	this.questions = null;
	this.questionsSorted = null;
	this.edit = null;
	this.sortDirection = "A";
	this.sortMode = "C";

	this.init = function()
	{
		me.ui = findElements(["asmt_editPool_bar_title", "asmt_editPool_title", "asmt_editPool_difficultyBar", "asmt_editPool_points", "asmt_editPool_description",
		                      "asmt_editPool_add", "asmt_editPool_view", "asmt_editPool_move", "asmt_editPool_copy", "asmt_editPool_delete", "asmt_editPool_actions",
		                      "asmt_editPool_table", "asmt_editPool_none",
		                      "asmt_editPool_attribution",
		                      ]);
		me.ui.itemNav = new e3_ItemNav();

		me.ui.table = new e3_Table(me.ui.asmt_editPool_table);
		me.ui.table.setupSelection("asmt_poolQuestion_select", me.updateActions);

		me.ui.sort = new e3_SortAction();
		me.ui.sort.inject(me.ui.asmt_editPool_actions,
				{onSort: me.onSort, initial: me.sortMode,
					options:[{value:"Q", title:main.i18n.lookup("sort_question", "QUESTION")},
					         {value:"T", title:main.i18n.lookup("sort_title", "TITLE")},
					         {value:"Y", title:main.i18n.lookup("sort_type", "TYPE")},
					         {value:"C", title:main.i18n.lookup("sort_created", "CREATED")}
					         ]});
		me.ui.sort.directional(true);

		onClick(me.ui.asmt_editPool_add, function(){console.log("add");});
		onClick(me.ui.asmt_editPool_view, function(){console.log("view");});
		onClick(me.ui.asmt_editPool_move, function(){console.log("move");});
		onClick(me.ui.asmt_editPool_copy, function(){console.log("copy");});
		onClick(me.ui.asmt_editPool_delete, me.remove);

		setupHoverControls([me.ui.asmt_editPool_add, me.ui.asmt_editPool_view, me.ui.asmt_editPool_move,  me.ui.asmt_editPool_copy, me.ui.asmt_editPool_delete]);
	};

	this.updateActions = function()
	{
		me.ui.table.updateActions([me.ui.asmt_editPool_view], [me.ui.asmt_editPool_move, me.ui.asmt_editPool_copy, me.ui.asmt_editPool_delete]);
	};

	this.start = function(pool, pools)
	{
		if (pools !== undefined) me.pools = pools;

		main.onExit = me.checkExit;

		me.load(pool.id);
	};

	this.load = function(id)
	{
		var params = main.portal.cdp.params();
		params.url.site = main.portal.site.id;
		params.url.pool = id;
		main.portal.cdp.request("assessment_getPool", params, function(data)
		{
			me.pool = data.pool;
			me.questions = data.questions;
			me.questionsSorted = me.sortPoolQuestions(me.questions, me.sortDirection, me.sortMode);

			me.ui.itemNav.inject(main.ui.asmt_itemnav, {doneFunction: me.done, pos: position(me.pool, me.pools), navigateFunction: me.goPool});

			me.makeEdit();
			me.populate();
		});
	};

	this.makeEdit = function()
	{
		me.edit = new e3_Edit(me.pool, ["questions", "attribution", "numQuestions"], function(changed)
		{
			main.ui.modebar.enableSaveDiscard(changed ? me.saveCancel : null);
			me.ui.itemNav.enableSave(changed ? me.saveCancel : null);
		});
		me.edit.setFilters({"title": me.edit.stringFilter, "description": me.edit.stringFilter, "points": me.edit.numberFilter, "difficulty": me.edit.stringFilter}); // TODO: tune?

		main.ui.modebar.enableSaveDiscard(null);
		me.ui.itemNav.enableSave(null);
	};

	this.populate = function()
	{
		me.edit.setupFilteredEdit(me.ui.asmt_editPool_title, me.edit, "title", function(val, finalChange)
		{
			me.ui.asmt_editPool_bar_title.text(val);
		});
		me.ui.asmt_editPool_bar_title.text(me.edit.title);

		me.edit.setupFilteredEdit(me.ui.asmt_editPool_points, me.edit, "points");
		me.edit.setupFilteredEdit(me.ui.asmt_editPool_description, me.edit, "description");

		me.ui.asmt_editPool_difficultyBar.empty();
		me.ui.type = new e3_SortAction();
		me.ui.type.inject(me.ui.asmt_editPool_difficultyBar,
			{onSort: function(dir, val){me.edit.set(me.edit, "difficulty", val);}, label: main.i18n.lookup("scrbrd_difficulty", "DIFFICULTY"),
			 options: [{value: "1", title: main.i18n.lookup("label_difficulty1", "Level 1")},
			           {value: "2", title: main.i18n.lookup("label_difficulty2", "Level 2")},
			           {value: "3", title: main.i18n.lookup("label_difficulty3", "Level 3")},
			           {value: "4", title: main.i18n.lookup("label_difficulty4", "Level 4")},
			           {value: "5", title: main.i18n.lookup("label_difficulty5", "Level 5")}],
			 initial: me.edit.difficulty});

		me.ui.table.clear();
		$.each(me.questionsSorted, function(index, question)
		{
			me.ui.table.row();
			me.ui.table.selectBox(question.id);

			var cell = clone(main.ui.asmt_title_template, ["asmt_title_template_body", "asmt_title_template_title", "asmt_title_template_msg"]);
			me.ui.table.hotElement(cell.asmt_title_template_body, main.i18n.lookup("msg_editQuestion", "edit question"), function(){me.editQuestion(question);}, null, {width:"calc(100vw - 100px - 346px)", minWidth:"calc(1200px - 100px - 346px"});
			cell.asmt_title_template_title.text(question.description);
			cell.asmt_title_template_msg.text(question.title);

			me.ui.table.text(main.i18n.lookup("msg_questionType_" + question.type, question.type), "e3_text special light", {fontSize:11, width:60});
			me.ui.table.date(question.attribution.createdOn, "-");

			me.ui.table.contextMenu(
			[
				{title: main.i18n.lookup("cm_duplicate", "Duplicate"), action:function(){me.duplicate(question);}},
				{},
				{title: main.i18n.lookup("cm_edit", "Edit"), action:function(){main.startEditQuestion(question, me.questionsSorted);}}, // TODO:
				{},
				{title: main.i18n.lookup("cm_view", "Preview"), action:function(){me.viewA(question);}},
				{title: main.i18n.lookup("cm_move", "Move"), action:function(){me.optionsA(question);}},
				{title: main.i18n.lookup("cm_copy", "Copy"), action:function(){me.publishA(question);}},
				{title: main.i18n.lookup("cm_delete", "Delete"), action:function(){me.removeA(question);}}
	        ]);
		});
		me.ui.table.done();
		show(me.ui.asmt_editPool_none, me.ui.table.rowCount() == 0);
	};

	this.saveCancel = function(mode, deferred)
	{
		if (mode)
		{
			me.save(deferred);
		}
		else
		{
			me.edit.revert();
			me.populate();
			if (deferred !== undefined) deferred();
		}
	};

	this.checkExit = function(deferred)
	{
		if (me.edit.changed())
		{
			main.portal.confirmNavigationWithChanges(function()
			{
				me.save(deferred);				
			}, function()
			{
				me.edit.revert();
				// me.populate();
				if (deferred !== undefined) deferred();
			});

			return false;
		}

		return true;
	};

	this.done = function()
	{
		if (me.edit.changed())
		{
			me.save(function(){main.startPools();})
		}
		else
		{
			main.startPools();
		}
	};

	this.goPool = function(pool)
	{
		main.startEditPool(pool);
	};

	this.save = function(deferred)
	{
		var params = main.portal.cdp.params();
		params.url.site = main.portal.site.id;
		params.url.pool = me.pool.id;
		me.edit.params("", params);
		main.portal.cdp.request("assessment_savePool assessment_getPool", params, function(data)
		{
			me.pool = data.pool;
			me.questions = data.questions;
			me.questionsSorted = me.sortPoolQuestions(me.questions, me.sortDirection, me.sortMode);

			me.makeEdit();
			me.populate();
			if (deferred !== undefined) deferred();
		});
	};

	this.onSort = function(direction, option)
	{
		me.sortDirection = direction;
		me.sortMode = option;
		me.ui.sort.directional(true);

		me.questionsSorted = me.sortPoolQuestions(me.questions, me.sortDirection, me.sortMode);

		me.populate();
	};

	this.sortPoolQuestions = function(questions, direction, mode)
	{
		var sorted = [];

		if (mode == "Q")
		{
			sorted = me.sortQuestionsByPresentation(questions, direction);
		}
		else if (mode == "T")
		{
			sorted = me.sortQuestionsByTitle(questions, direction);
		}
		else if (mode == "Y")
		{
			sorted = me.sortQuestionsByType(questions, direction);
		}
		else if (mode == "C")
		{
			sorted = me.sortQuestionsByCreated(questions, direction);
		}
		
		return sorted
	};

	this.sortQuestionsByPresentation = function(questions, direction)
	{
		var sorted = [].concat(questions);
		var adjust = (direction == 'A') ? 1 : -1;
		sorted.sort(function(a, b)
		{
			var rv = adjust * compareS(a.presentation, b.presentation);
			if (rv == 0)
			{
				rv = compareN(a.id, b.id);
			}
			return rv;
		});

		return sorted;
	};

	this.sortQuestionsByTitle = function(questions, direction)
	{
		var sorted = [].concat(questions);
		var adjust = (direction == 'A') ? 1 : -1;
		sorted.sort(function(a, b)
		{
			var rv = adjust * compareS(a.title, b.title);
			if (rv == 0)
			{
				rv = compareN(a.id, b.id);
			}
			return rv;
		});

		return sorted;
	};

	this.sortQuestionsByType = function(questions, direction)
	{
		var sorted = [].concat(questions);
		var adjust = (direction == 'A') ? 1 : -1;
		sorted.sort(function(a, b)
		{
			var rv = adjust * compareS(a.type, b.type); // TODO: ???
			if (rv == 0)
			{
				rv = compareS(a.presentation, b.presentation);
				if (rv == 0)
				{
					rv = compareN(a.id, b.id);
				}
			}
			return rv;
		});

		return sorted;
	};

	this.sortQuestionsByCreated = function(questions, direction)
	{
		var sorted = [].concat(questions);
		var adjust = (direction == 'A') ? 1 : -1;
		sorted.sort(function(a, b)
		{
			var rv = adjust * compareN(a.attribution.createdOn, b.attribution.createdOn);
			if (rv == 0)
			{
				rv = compareS(a.presentation, b.presentation);
				if (rv == 0)
				{
					rv = compareN(a.id, b.id);
				}
			}
			return rv;
		});

		return sorted;
	};

	this.remove = function()
	{
		var ids = me.ui.table.selected();

		if (ids.length == 0)
		{
			main.portal.dialogs.openAlert(main.ui.asmt_selectFirst);
			return;
		}

//		$.each(ids, function(index, id)
//		{
//			me.edit.remove(me.edit, "details", id);
//		});

		me.populate();
	};

	this.removeA = function(detail)
	{
//		me.edit.remove(me.edit, "details", detail.id);

		me.populate();
	};
};

function AssessmentManage(main)
{
	var me = this;

	this.ui = null;
	this.edit = null;
	this.assessments = null;
	this.assessmentsSorted = null;
	this.sortDirection = "A";
	this.sortMode = "D";

	this.init = function()
	{
		me.ui = findElements(["asmt_manage_actions", "asmt_manage_table", "asmt_manage_add", "asmt_manage_delete", "asmt_manage_publish", "asmt_manage_unpublish",
		                      "asmt_manage_import", "asmt_manage_export", "asmt_manage_archive", "asmt_manage_restore", "asmt_manage_options", "asmt_manage_view", "asmt_manage_none",
		                      "asmt_manage_graded_template"]);

		me.ui.sort = new e3_SortAction();
		me.ui.sort.inject(me.ui.asmt_manage_actions,
				{onSort: me.onSort, initial: me.sortMode,
					options:[{value:"P", title:main.i18n.lookup("sort_status", "STATUS")},{value:"T", title:main.i18n.lookup("sort_title", "TITLE")},
					         {value:"Y", title:main.i18n.lookup("sort_type", "TYPE")},{value:"O", title:main.i18n.lookup("sort_open", "OPEN")},
					         {value:"D", title:main.i18n.lookup("sort_due", "DUE")}]});
		me.ui.sort.directional(true);

		me.ui.table = new e3_Table(me.ui.asmt_manage_table);
		me.ui.table.setupSelection("asmt_manage_select", me.updateActions);
		me.ui.table.selectAllHeader(1, main.ui.asmt_header_manage);
	
		onClick(me.ui.asmt_manage_add, me.add);
		onClick(me.ui.asmt_manage_delete, me.remove);
		onClick(me.ui.asmt_manage_publish, me.publish);
		onClick(me.ui.asmt_manage_unpublish, me.unpublish);
		onClick(me.ui.asmt_manage_view, me.view);
		onClick(me.ui.asmt_manage_options, me.options);
		onClick(me.ui.asmt_manage_export, me.exportN);
		onClick(me.ui.asmt_manage_archive, me.archive);
		onClick(me.ui.asmt_manage_import, me.import);
		onClick(me.ui.asmt_manage_restore, me.restore);

		setupHoverControls([me.ui.asmt_manage_add, me.ui.asmt_manage_delete, me.ui.asmt_manage_publish, me.ui.asmt_manage_unpublish, me.ui.asmt_manage_view,
		                    me.ui.asmt_manage_import, me.ui.asmt_manage_export, me.ui.asmt_manage_archive, me.ui.asmt_manage_restore, me.ui.asmt_manage_options]);
	};

	this.start = function()
	{
		main.onExit = me.checkExit;
		me.load();
	};

	this.load = function()
	{
		var params = main.portal.cdp.params();
		params.url.site = main.portal.site.id;
		main.portal.cdp.request("assessment_getManage evaluation_rubrics evaluation_categories", params, function(data)
		{
			if (data.rubrics != null) main.rubrics = data.rubrics;
			if (data.categories != null) main.categories = data.categories;
			if (data.fs != null) main.fs = data.fs;

			me.assessments = data.assessments;
			me.assessmentsSorted = main.sortAssessments(me.assessments, me.sortDirection, me.sortMode);

			me.makeEdit();
			me.populate();
		});
	};

	this.makeEdit = function()
	{
		// edit of the items, id and blocker and dates
		me.edit = new e3_Edit({items: me.assessments}, ["items[].type", "items[].published", "items[].valid", "items[].title", "items[].allowRemove", "items[].schedule.hide",
		                                                "items[].schedule.status", "items[].schedule.close", "items[].schedule.lastCall", "items[].gradingInfo"], function(changed)
		{
			main.ui.modebar.enableSaveDiscard(changed? me.saveCancel : null);
		});

		main.ui.modebar.enableSaveDiscard(null);
	};

	this.populate = function()
	{
		me.ui.table.clear();
		$.each(me.assessmentsSorted, function(index, assessment)
		{
			var inEdit = findIdInList(assessment.id, me.edit.items);

			me.ui.table.row();
			me.ui.table.selectBox(assessment.id);
			main.publicationStatusDotTd(assessment, me.ui.table);

			me.ui.table.hotText(assessment.title, "", function(){main.startEditAssessment(assessment, me.assessmentsSorted);}, null, {width: "calc(100vw - 100px - 864px)", minWidth:"calc(1200px - 100px - 864px)"});
			main.typeTd(assessment, me.ui.table);

			me.edit.setupDateEdit(me.ui.table.inputDate().find("input"), inEdit.schedule, "open", main.portal.timestamp, true);
			me.edit.setupDateEdit(me.ui.table.inputDate().find("input"), inEdit.schedule, "due", main.portal.timestamp, false);
			me.edit.setupDateEdit(me.ui.table.inputDate().find("input"), inEdit.schedule, "allowUntil", main.portal.timestamp, false);

			var cell = clone(me.ui.asmt_manage_graded_template, ["asmt_manage_graded_template_body", "asmt_manage_graded_template_dot", "asmt_manage_graded_template_msg"]);
			
			// blue dot for needs grading
			if (assessment.gradingInfo.needsGrading && assessment.valid)
			{
				cell.asmt_manage_graded_template_dot.append(dotSmall(Dots.blue, "", true).css({verticalAlign: "bottom", marginRight: 4}));
			}

			cell.asmt_manage_graded_template_msg.append(main.i18n.lookup("msg_gradedOfTotal", "%0 / %1", "html", [assessment.gradingInfo.numGradedSubmissions, assessment.gradingInfo.numCompleteSubmissions]));

			// hot to grade assessment if gradable - "Results" for surveys, "Grade" for others
			if (assessment.valid && assessment.published)
			{
				me.ui.table.hotElement(cell.asmt_manage_graded_template_body,
					((assessment.type == main.AssessmentType.survey) ? main.i18n.lookup("msg_resultsAction", "Results") : main.i18n.lookup("msg_gradeAction", "Grade")),
					function(){main.startGradeAssessment(assessment, me.assessmentsSorted);}, null, {width:100});
			}
			else
			{
				me.ui.table.element(cell.asmt_manage_graded_template_body, null, {width:100});
			}

			me.ui.table.contextMenu(
			[
				{title: main.i18n.lookup("cm_access", "Special Access"), action:function(){me.access(assessment);}},
				{title: main.i18n.lookup("cm_duplicate", "Duplicate"), action:function(){me.duplicate(assessment);}},
				{title: main.i18n.lookup("cm_print", "Print"), action:function(){me.print(assessment);}},
				{},
				{title: main.i18n.lookup("cm_edit", "Edit"), action:function(){main.startEditAssessment(assessment, me.assessmentsSorted);}},
				{title: main.i18n.lookup("cm_grade", "Grade"), action:function(){main.startGradeAssessment(assessment, me.assessmentsSorted);},
						condition: ((assessment.type != main.AssessmentType.survey) && assessment.valid && assessment.published)},
				{title: main.i18n.lookup("cm_results", "Results"), action:function(){main.startGradeAssessment(assessment, me.assessmentsSorted);},
						condition: ((assessment.type == main.AssessmentType.survey) && assessment.valid && assessment.published)},
				{},
				{title: main.i18n.lookup("cm_view", "Preview"), action:function(){me.viewA(assessment);}},
				{title: main.i18n.lookup("cm_options", "Options"), action:function(){me.optionsA(assessment);}},
				{title: main.i18n.lookup("cm_publish", "Publish"), action:function(){me.publishA(assessment);}},
				{title: main.i18n.lookup("cm_unpublish", "Unpublish"), action:function(){me.unpublishA(assessment);}},
				{title: main.i18n.lookup("cm_delete", "Delete"), action:function(){me.removeA(assessment);}},
				{title: main.i18n.lookup("cm_export", "Export"), action:function(){me.exportA(assessment);}},
				{title: main.i18n.lookup("cm_archive", "Archive"), action:function(){me.archiveA(assessment);}},
	        ]);
		});
		me.ui.table.done();
		show(me.ui.asmt_manage_none, (me.ui.table.rowCount() == 0));
	};

	this.onSort = function(direction, option)
	{
		me.sortDirection = direction;
		me.sortMode = option;
		me.ui.sort.directional(true);

		me.assessmentsSorted = main.sortAssessments(me.assessments, me.sortDirection, me.sortMode);

		me.populate();
	};

	this.updateActions = function()
	{
		me.ui.table.updateActions([me.ui.asmt_manage_view], [me.ui.asmt_manage_delete, me.ui.asmt_manage_publish, me.ui.asmt_manage_unpublish,
		                                                     me.ui.asmt_manage_export, me.ui.asmt_manage_archive, me.ui.asmt_manage_options]);		
	};

	this.saveCancel = function(mode, deferred)
	{
		if (mode)
		{
			me.save(deferred);
		}
		else
		{
			me.edit.revert();
			main.ui.modebar.enableSaveDiscard(null);
			me.populate();
			if (deferred !== undefined) deferred();
		}
	};
	
	this.checkExit = function(deferred)
	{
		if (me.edit.changed())
		{
			main.portal.confirmNavigationWithChanges(function()
			{
				me.save(deferred);				
			}, function()
			{
				me.edit.revert();
				main.ui.modebar.enableSaveDiscard(null);
				if (deferred !== undefined) deferred();
			});

			return false;
		}

		return true;
	};

	this.save = function(deferred)
	{
		var params = main.portal.cdp.params();
		params.url.site = main.portal.site.id;
		me.edit.params("", params);
		main.portal.cdp.request("assessment_saveManage assessment_getManage", params, function(data)
		{
			me.assessments = data.assessments;
			me.assessmentsSorted = main.sortAssessments(me.assessments, me.sortDirection, me.sortMode);

			me.makeEdit();
			me.populate();

			if (deferred !== undefined) deferred();
		});
	};

	this.add = function()// TODO: is this complete enough for edit and options and instructions?  should we just go to the server?
	{
		var assessment = {id:-1, type:main.AssessmentType.assignment, title:null, published:false, schedule:{open:null, due:null, allowUntil:null, hide:false}, design:{forGrade:false, autoRelease:false, points:null, rubricSelected:0, categorySelected:0, actualCategory:0}};
		main.assessments.push(assessment);
		main.startEditAssessment(assessment, me.assessmentsSorted);
	};

	this.duplicate = function(assessment) // TODO: see add's TODO: this is not right... evaluationDesign, instructions...
	{
		var assessment =
			{
				id: -1,
				type: assessment.type,
				title: assessment.title,
				published: false,
				instructions: assessment.instructions,
				schedule: {open:assessment.schedule.open, due:assessment.schedule.due, allowUntil:assessment.schedule.allowUntil, hide:assessment.schedule.hide}, 
				design: {forGrade:assessment.design.forGrade, autoRelease:assessment.design.autoRelease, points:assessment.design.points,
					rubricSelected:assessment.design.rubricSelected, categorySelected:assessment.design.categorySelected, actualCategory:assessment.design.actualCategory}};
		main.assessments.push(assessment);
		main.startEditAssessment(assessment, me.assessmentsSorted);
	};

	this.remove = function()
	{
		var params = main.portal.cdp.params();
		params.url.site = main.portal.site.id;
		params.post.ids = me.ui.table.selected();

		if (params.post.ids.length == 0)
		{
			main.portal.dialogs.openAlert(main.ui.asmt_selectFirst);
			return;
		}

		main.portal.dialogs.openConfirm(main.ui.asmt_confirmDelete, main.i18n.lookup("action_delete", "Delete"), function()
		{
			main.portal.cdp.request("assessment_remove assessment_getManage", params, function(data)
			{
				me.assessments = data.assessments;
				me.assessmentsSorted = main.sortAssessments(me.assessments, me.sortDirection, me.sortMode);

				me.makeEdit();
				me.populate();
			});

			return true;
		}/*, function(){me.ui.table.clearSelection();}*/);
	};

	this.removeA = function(assessment)
	{
		var params = main.portal.cdp.params();
		params.url.site = main.portal.site.id;
		params.post.ids = [assessment.id];

		main.portal.dialogs.openConfirm(main.ui.asmt_confirmDelete, main.i18n.lookup("action_delete", "Delete"), function()
		{
			main.portal.cdp.request("assessment_remove assessment_getManage", params, function(data)
			{
				me.assessments = data.assessments;
				me.assessmentsSorted = main.sortAssessments(me.assessments, me.sortDirection, me.sortMode);

				me.makeEdit();
				me.populate();
			});

			return true;
		});
	};

	this.publish = function()
	{
		var params = main.portal.cdp.params();
		params.url.site = main.portal.site.id;
		params.post.ids = me.ui.table.selected();

		if (params.post.ids.length == 0)
		{
			main.portal.dialogs.openAlert(main.ui.asmt_selectFirst);
			return;
		}

		main.portal.cdp.request("assessment_publish assessment_getManage", params, function(data)
		{
			me.assessments = data.assessments;
			me.assessmentsSorted = main.sortAssessments(me.assessments, me.sortDirection, me.sortMode);

			me.makeEdit();
			me.populate();
		});
	};

	this.publishA = function(assessment)
	{
		var params = main.portal.cdp.params();
		params.url.site = main.portal.site.id;
		params.post.ids = [assessment.id];

		main.portal.cdp.request("assessment_publish assessment_getManage", params, function(data)
		{
			me.assessments = data.assessments;
			me.assessmentsSorted = main.sortAssessments(me.assessments, me.sortDirection, me.sortMode);

			me.makeEdit();
			me.populate();
		});
	};

	this.unpublish = function()
	{
		var params = main.portal.cdp.params();
		params.url.site = main.portal.site.id;
		params.post.ids = me.ui.table.selected();

		if (params.post.ids.length == 0)
		{
			main.portal.dialogs.openAlert(main.ui.asmt_selectFirst);
			return;
		}

		main.portal.cdp.request("assessment_unpublish assessment_getManage", params, function(data)
		{
			me.assessments = data.assessments;
			me.assessmentsSorted = main.sortAssessments(me.assessments, me.sortDirection, me.sortMode);

			me.makeEdit();
			me.populate();
		});
	};
	
	this.unpublishA = function(assessment)
	{
		var params = main.portal.cdp.params();
		params.url.site = main.portal.site.id;
		params.post.ids = [assessment.id];

		main.portal.cdp.request("assessment_unpublish assessment_getManage", params, function(data)
		{
			me.assessments = data.assessments;
			me.assessmentsSorted = main.sortAssessments(me.assessments, me.sortDirection, me.sortMode);

			me.makeEdit();
			me.populate();
		});
	};

	this.view = function()
	{
		var ids = me.ui.table.selected();
		if (ids == 0)
		{
			main.portal.dialogs.openAlert(main.ui.asmt_selectFirst);
			return;
		}

		var assessment = findIdInList(ids[0], me.assessments);
		if (assessment == null) return;
		
		console.log("view", assessment);
	};

	this.viewA = function(assessment)
	{
		console.log("view", assessment);
	};
	
	this.options = function()
	{
		var params = main.portal.cdp.params();
		params.url.site = main.portal.site.id;
		params.post.ids = me.ui.table.selected();

		if (params.post.ids.length == 0)
		{
			main.portal.dialogs.openAlert(main.ui.asmt_selectFirst);
			return;
		}
		
		if (params.post.ids.length == 1)
		{
			var assessment = findIdInList(params.post.ids[0], me.assessments);
			me.optionsA(assessment);
			return;
		}

		// TODO: multiple options
		console.log("options");
	};

	this.optionsA = function(assessment)
	{
		main.startOption(assessment, me.assessmentsSorted);
	};

	this.access = function(assessment)
	{
		console.log("special access");
	};
	
	this.print = function(assessment)
	{
		console.log("print");
	};
	
	this.exportN = function()
	{
		var params = main.portal.cdp.params();
		params.url.site = main.portal.site.id;
		params.post.ids = me.ui.table.selected();

		if (params.post.ids.length == 0)
		{
			main.portal.dialogs.openAlert(main.ui.asmt_selectFirst);
			return;
		}

		main.portal.cdp.request("assessment_export", params, function(data)
		{
			// TODO:
		});
	};

	this.exportA = function(assessment)
	{
		var params = main.portal.cdp.params();
		params.url.site = main.portal.site.id;
		params.post.ids = [assessment.id];

		main.portal.cdp.request("assessment_export", params, function(data)
		{
			// TODO:
		});
	};
	
	this.archive = function()
	{
		var params = main.portal.cdp.params();
		params.url.site = main.portal.site.id;
		params.post.ids = me.ui.table.selected();

		if (params.post.ids.length == 0)
		{
			main.portal.dialogs.openAlert(main.ui.asmt_selectFirst);
			return;
		}

		main.portal.cdp.request("assessment_archive assessment_getManage", params, function(data)
		{
			me.assessments = data.assessments;
			me.assessmentsSorted = main.sortAssessments(me.assessments, me.sortDirection, me.sortMode);

			me.makeEdit();
			me.populate();
		});
	};

	this.archiveA = function(assessment)
	{
		var params = main.portal.cdp.params();
		params.url.site = main.portal.site.id;
		params.post.ids = [assessment.id];

		main.portal.cdp.request("assessment_archive assessment_getManage", params, function(data)
		{
			me.assessments = data.assessments;
			me.assessmentsSorted = main.sortAssessments(me.assessments, me.sortDirection, me.sortMode);

			me.makeEdit();
			me.populate();
		});
	};
	
	this.import = function()
	{
		console.log("import");
	};
	
	this.restore = function()
	{
		console.log("restore");
	};
}

function AssessmentGrade(main) // TODO: remove when confirmed not needed
{
	var me = this;

	this.ui = null;
	this.table = null;
	this.assessmentsSorted = null;
	this.sortDirection = "A";
	this.sortMode = "D";

	this.init = function()
	{
//		me.ui = findElements(["asmt_grade_actions", "asmt_grade_table", "asmt_grade_none"]);
//		me.ui.sort = new e3_SortAction();
//		me.ui.sort.inject(me.ui.asmt_grade_actions,
//				{onSort: me.onSort, initial: me.sortMode,
//					options:[{value:"T", title:main.i18n.lookup("sort_title", "TITLE")},
//					         {value:"O", title:main.i18n.lookup("sort_open", "OPEN")},
//					         {value:"D", title:main.i18n.lookup("sort_due", "DUE")}]});
//		me.ui.sort.directional(true);
//
//		me.ui.table = new e3_Table(me.ui.asmt_grade_table);
//
//		onClick(me.ui.asmt_grade_view, me.view);
	};

	this.filterForGrading = function(assessments)
	{
		var rv = [];
		$.each(assessments, function(index, assessment)
		{
			if ((assessment.valid) && (assessment.published) && (assessment.type != me.AssessmentType.fce))
			{
				rv.push(assessment);
			}
		});
		
		return rv;
	};

	this.start = function()
	{
//		main.load(function()
//		{
//			me.assessmentsSorted = main.sortAssessments(me.filterForGrading(main.assessments), me.sortDirection, me.sortMode);
//			me.populate();
//		});
	};

//	this.populate = function()
//	{
//		me.ui.table.clear();
//		$.each(me.assessmentsSorted, function(index, assessment)
//		{
//			me.ui.table.row();
//
//			var cell = clone(main.ui.asmt_title_template, ["asmt_title_template_body", "asmt_title_template_title", "asmt_title_template_msg"]);
//			me.ui.table.hotElement(cell.asmt_title_template_body, main.i18n.lookup("msg_gradeAssessment", "grade assessment"), function()
//			{
//				main.startGradeAssessment(assessment, me.assessmentsSorted);
//			}, null, {width:"calc(100vw - 100px - 480px)", minWidth:"calc(1200px - 100px - 480px"});
//
//			cell.asmt_title_template_title.text(assessment.title);
//			
//			var msg = null;
//			var dot = false;
//			if (assessment.needsGrading)
//			{
//				msg = main.i18n.lookup("msg_ungraded", "has ungraded submissions");
//				dot = dotSmall(Dots.blue, "", true).css({verticalAlign: "bottom", marginRight: 4});
//			}
//			else if (assessment.live)
//			{
//				msg = main.i18n.lookup("msg_live", "- live");
//			}
//			if (msg != null)
//			{
//				if (dot != null)
//				{
//					cell.asmt_title_template_msg.append(dot).append(msg);
//				}
//				else
//				{
//					cell.asmt_title_template_msg.text(msg);
//				}
//			}
//
//			main.typeTd(assessment, me.ui.table);
//			me.ui.table.date(assessment.schedule.open);
//			me.ui.table.date(assessment.schedule.due);
//
//			me.ui.table.contextMenu(
//			[
//				{title: main.i18n.lookup("cm_grade", "Grade"), action:function(){main.startGradeAssessment(assessment, me.assessmentsSorted);}},
//				{title: main.i18n.lookup("cm_view", "Preview"), action:function(){me.view(assessment);}}
//	        ]);
//		});
//		me.ui.table.done();
//		show(me.ui.asmt_grade_none, (me.ui.table.rowCount() == 0));
//	};
//
//	this.onSort = function(direction, option)
//	{
//		me.sortDirection = direction;
//		me.sortMode = option;
//		me.ui.sort.directional(true);
//
//		me.assessmentsSorted = main.sortAssessments(me.filterForGrading(main.assessments), me.sortDirection, me.sortMode);
//		me.populate();
//	};
//
//	this.view = function(assessment)
//	{
//		console.log("view", assessment);
//	};
}

function AssessmentEditAssessment(main)
{
	var me = this;
	
	this.ui = null;

	this.assessment = null;
	this.assessments = null;
	this.edit = null;

	this.init = function()
	{
		me.ui = findElements(["asmt_edit_bar_title", "asmt_edit_view", "asmt_edit_typeBar", "asmt_edit_title",
		                      "asmt_edit_instructions", "asmt_edit_parts", "asmt_edit_options",
		                      "asmt_edit_add", "asmt_edit_select", "asmt_edit_draw", "asmt_edit_move", "asmt_edit_remove",
		                      "asmt_edit_questions", "asmt_edit_points",
		                      "asmt_edit_questions_table", "asmt_edit_questions_none", "asmt_edit_attribution"]);
		me.ui.itemNav = new e3_ItemNav();

		me.ui.table = new e3_Table(me.ui.asmt_edit_questions_table);
		me.ui.table.setupSelection("asmt_question_select", me.updateActions);
		me.ui.table.enableReorder(me.applyDetailOrder);

		onClick(me.ui.asmt_edit_view, me.view);

		onClick(me.ui.asmt_edit_instructions, me.instructions);
		onClick(me.ui.asmt_edit_parts, me.parts);
		onClick(me.ui.asmt_edit_options, me.options);

		onClick(me.ui.asmt_edit_add, function(){console.log("add");});
		onClick(me.ui.asmt_edit_select, function(){console.log("select");});
		onClick(me.ui.asmt_edit_draw, function(){console.log("draw");});
		onClick(me.ui.asmt_edit_move, function(){console.log("move");});
		onClick(me.ui.asmt_edit_remove, function(){me.remove();});

		setupHoverControls([me.ui.asmt_edit_view,
		                    me.ui.asmt_edit_instructions, me.ui.asmt_edit_parts,  me.ui.asmt_edit_options,
		                    me.ui.asmt_edit_add,  me.ui.asmt_edit_select,  me.ui.asmt_edit_draw,  me.ui.asmt_edit_move,  me.ui.asmt_edit_remove]);
	};

	this.updateActions = function()
	{
		me.ui.table.updateActions([], [me.ui.asmt_edit_move, me.ui.asmt_edit_remove]);
	};

	this.applyDetailOrder = function(order)
	{
		me.edit.set(me.edit, "reorder", order.join());
	};

	this.partsAndDetails = function()
	{
		var rv = [];
		var partId = null;
		$.each(me.assessment.design.details, function(index, detail)
		{
			if ((partId == null) || (detail.partId != partId))
			{
				partId = detail.partId;
				rv.push({id: "P:" + partId});
			}
			rv.push(detail);
		});

		return rv;
	};

	this.findPartsBetween = function(start, end)
	{
		var rv = (start == null) ? [] : null;
		for (var i = 0; i < me.assessment.design.parts.length; i++)
		{
			if ((rv == null) && (start == me.assessment.design.parts[i].id))
			{
				rv = [];
			}
			else if ((end != null) && (end == me.assessment.design.parts[i].id))
			{
				return rv;
			}
			else if (rv != null)
			{
				rv.push(me.assessment.design.parts[i]);
			}
		}

		return rv;
	};

	this.start = function(assessment, assessments)
	{
		if (assessments !== undefined) me.assessments = assessments;

		main.onExit = me.checkExit;

		// get a fresh assessment
		me.load(assessment.id);
	};

	this.load = function(id)
	{
		var params = main.portal.cdp.params();
		params.url.site = main.portal.site.id;
		params.url.assessment = id;
		main.portal.cdp.request("assessment_getDesign", params, function(data)
		{
			me.assessment = data.assessment;

			me.ui.itemNav.inject(main.ui.asmt_itemnav, {doneFunction: me.done, pos: position(me.assessment, me.assessments), navigateFunction: me.goAssessment});
			me.makeEdit();
			me.populate();
		});
	};

	this.makeEdit = function()
	{
		me.edit = new e3_Edit({details: me.partsAndDetails(), title: me.assessment.title, type: me.assessment.type},
				["details[].partId", "details[].type", "details[].description", "details[].questionType", "details[].poolTitle", "details[].count", "details[].ordering"], function(changed) // TODO: tune questionId, poolId?
		{
			main.ui.modebar.enableSaveDiscard(changed ? me.saveCancel : null);
			me.ui.itemNav.enableSave(changed ? me.saveCancel : null);
		});
		me.edit.setFilters({"title": me.edit.stringFilter, "details[].points": me.edit.numberFilter});

		main.ui.modebar.enableSaveDiscard(null);
		me.ui.itemNav.enableSave(null);
	};

	this.populate = function()
	{
		me.edit.setupFilteredEdit(me.ui.asmt_edit_title, me.edit, "title", function(val, finalChange)
		{
			me.ui.asmt_edit_bar_title.text(val);
		});
		me.ui.asmt_edit_bar_title.text(me.edit.title);
		
		me.ui.asmt_edit_typeBar.empty();
		me.ui.type = new e3_SortAction();
		me.ui.type.inject(me.ui.asmt_edit_typeBar,
			{onSort: function(dir, val){me.edit.set(me.edit, "type", val);}, label: main.i18n.lookup("scrbrd_type", "TYPE"),
			 options: [{value: "A", title: main.i18n.lookup("label_assignment", "Assignment")},
			           {value: "T", title: main.i18n.lookup("label_test", "Test")},
			           {value: "S", title: main.i18n.lookup("label_survey", "Survey")},
			           {value: "O", title: main.i18n.lookup("label_offline", "Offline")}],
			 initial: me.edit.type});

		var inPartId = null;
		var top = true;
		me.ui.table.clear();
		$.each(me.assessment.design.details, function(index, detail)
		{
			// for the start of a part
			if ((inPartId == null) || (detail.partId != inPartId))
			{
				// any skipped empty parts?  between inPart, the last we placed with questions, and detail.part, this next one - place table headers
				var before = me.findPartsBetween(inPartId, detail.partId);
				
				inPartId = detail.partId;
				var part = findIdInList(detail.partId, me.assessment.design.parts);
				before.push(part);
				me.populatePartHeaders(before, top);
				top = false;
			}
			me.populateDetail(detail);
		});

		// any remaining empty parts?  past inPart, the last we placed with questions - place table headers
		var after = me.findPartsBetween(inPartId, null);
		me.populatePartHeaders(after, top);

		me.ui.table.done();
		show(me.ui.asmt_edit_questions_none, me.ui.table.rowCount() == 0);

		if (me.assessment.id != -1) new e3_Attribution().inject(me.ui.asmt_edit_attribution, me.assessment.id, me.assessment.attribution);
 		show(me.ui.asmt_edit_attribution, (me.assessment.id != -1));

 		me.populatePoints();
 		me.populateNumQuestions();
	};

	this.populatePartHeaders = function(parts, top)
	{
		$.each(parts, function(index, part)
		{
			me.ui.table.row();
			
			var msg = null;
			if (part.title != null)
			{
				if (part.shuffle)
				{
					msg = main.i18n.lookup("msg_partOrderTitleShuffled", "<svg style='width:12px; height:12px; fill:#A8A8A8; margin-right:8px;'><use xlink:href='#icon-shuffle'></use></svg>Part %0 of %1: %2", "html",
							[part.ordering.position, part.ordering.size, part.title]);
				}
				else
				{
					msg = main.i18n.lookup("msg_partOrderTitle", "Part %0 of %1: %2", "html", [part.ordering.position, part.ordering.size, part.title]);
				}
			}
			else
			{
				if (part.shuffle)
				{
					msg = main.i18n.lookup("msg_partOrderShuffled", "<svg style='width:12px; height:12px; fill:#A8A8A8; margin-right:8px;'><use xlink:href='#icon-shuffle'></use></svg>Part %0 of %1", "html",
							[part.ordering.position, part.ordering.size]);
				}
				else
				{
					msg = main.i18n.lookup("msg_partOrder", "Part %0 of %1", "html", [part.ordering.position, part.ordering.size]);
				}
			}
			var td = me.ui.table.headerRow(msg);
			me.ui.table.includeInOrderDisabled(td, "P:" + part.id);
			me.ui.table.disableRowReorder();
			if (top && (index == 0)) me.ui.table.disableRowReorderTarget();
		});
	};

	this.populateDetail = function(detail)
	{
		var inTheEdit = findIdInList(detail.id, me.edit.details);
		if (inTheEdit == null) return; // might have been deleted from the edit, but not the me.assessment.design.details yet

		me.ui.table.row();
		me.ui.table.selectBox(detail.id);
		me.ui.table.reorder(main.i18n.lookup("msg_reorder", "drag to reorder"), detail.id);

		if (detail.type == "S")
		{
			me.ui.table.text(detail.description, null, {width:"calc(100vw - 100px - 556px)", minWidth:"calc(1200px - 100px - 556px)"});
		}
		else
		{
			me.ui.table.text(main.i18n.lookup("msg_randomDraw", "Random Draw"), "e3_text italic", {width:"calc(100vw - 100px - 556px)", minWidth:"calc(1200px - 100px - 556px)"});
		}
		
		if (me.assessment.evaluationDesign.forPoints)
		{
			var input = me.ui.table.input({size: 2, type: "text"}, null, {width: 60}).find("input");
			input.css({fontSize: 13, width: 32});
			input.val(detail.points);
			me.edit.setupFilteredEdit(input, inTheEdit, "points", function(val, finalChange)
			{
				if (finalChange) me.populatePoints();
			});
		}
		else
		{
			me.ui.table.text("-", null, {width:60});
		}

		me.ui.table.text(main.i18n.lookup("msg_questionType_" + detail.questionType, detail.questionType), "e3_text special light", {fontSize:11, width:60});
		me.ui.table.text(detail.poolTitle, null, {width:200});
		me.ui.table.text(detail.count, null, {width:60});

		me.ui.table.contextMenu(
		[
			{title: main.i18n.lookup("cm_viewQuestion", "View Question"), action:function(){console.log("View Question");}, condition: (detail.type == "S")},
			{title: main.i18n.lookup("cm_viewPool", "View Pool"), action:function(){console.log("View Pool");}, condition: (detail.type == "D")},
			{title: main.i18n.lookup("cm_editQuestion", "Edit Question"), action:function(){console.log("Edit Question");}, condition: (detail.type == "S")},
			{},
			{title: main.i18n.lookup("cm_remove", "Remove"), action:function(){me.removeA(detail);}}
        ]);
	};

	this.populatePoints = function()
	{
		if (me.assessment.evaluationDesign.forPoints)
		{
			var points = 0;
			$.each(me.edit.details, function(index, detail)
			{
				if (detail.points !== undefined)
				{
					points += detail.points;
				}
			});
			me.ui.asmt_edit_points.text(points);
		}
		else
		{
			me.ui.asmt_edit_points.text("-");
		}
	};

	this.populateNumQuestions = function()
	{
		var numQuestions = 0;
		$.each(me.assessment.design.details, function(index, detail)
		{
			var inEdit = findIdInList(detail.id, me.edit.details);
			if (inEdit == null) return;
			
			numQuestions += detail.count;
		});

		me.ui.asmt_edit_questions.text(numQuestions);
	};
	
	this.saveCancel = function(mode, deferred)
	{
		if (mode)
		{
			me.save(deferred);
		}
		else
		{
			me.edit.revert();
			me.populate();
			if (deferred !== undefined) deferred();
		}
	};

	this.checkExit = function(deferred)
	{
		if (me.edit.changed())
		{
			main.portal.confirmNavigationWithChanges(function()
			{
				me.save(deferred);				
			}, function()
			{
				me.edit.revert();
				// me.populate();
				if (deferred !== undefined) deferred();
			});

			return false;
		}

		return true;
	};

	this.done = function()
	{
		if (me.edit.changed())
		{
			me.save(function(){main.startManage();})
		}
		else
		{
			main.startManage();
		}
	};

	this.goAssessment = function(assessment)
	{
		main.startEditAssessment(assessment);
	};

	this.save = function(deferred)
	{
		var params = main.portal.cdp.params();
		params.url.site = main.portal.site.id;
		params.url.assessment = me.assessment.id;
		me.edit.params("", params);
		main.portal.cdp.request("assessment_saveDesign assessment_getDesign", params, function(data)
		{
			me.assessment = data.assessment;

			me.makeEdit();
			me.populate();
			if (deferred !== undefined) deferred();
		});
	};

	this.view = function()
	{
		console.log("view", me.edit);
	};

	this.instructions = function()
	{
		if (!me.checkExit(function(){me.instructions();})) return;// TODO: needed?  we'll reload the assessment in instructions mode ???
		main.startInstruction(me.assessment, me.assessments);
	};

	this.parts = function()
	{
		if (!me.checkExit(function(){me.parts();})) return;// TODO: needed?  we'll reload the assessment in parts mode ???
		main.startPart(me.assessment, me.assessments);
	};

	this.options = function()
	{
		if (!me.checkExit(function(){me.options();})) return;// TODO: needed?  we'll reload the assessment in parts mode ???
		main.startOption(me.assessment, me.assessments);
	};

	this.remove = function()
	{
		var ids = me.ui.table.selected();

		if (ids.length == 0)
		{
			main.portal.dialogs.openAlert(main.ui.asmt_selectFirst);
			return;
		}

		$.each(ids, function(index, id)
		{
			me.edit.remove(me.edit, "details", id);
		});

		me.populate();
	};

	this.removeA = function(detail)
	{
		me.edit.remove(me.edit, "details", detail.id);

		me.populate();
	};
};

function AssessmentOption(main)
{
	var me = this;
	
	this.ui = null;

	this.assessment = null;
	this.assessments = null;
	this.edit = null;

	this.init = function()
	{
		me.ui = findElements(["asmt_option_bar_title", "asmt_option_title", "asmt_option_open", "asmt_option_due", "asmt_option_allow", "asmt_option_hide", "asmt_option_published",
		                      "asmt_option_password", "asmt_option_requireHonorPledge", "asmt_option_numTries", "asmt_option_timeLimit", "asmt_option_reviewAfterDate",
		                      "asmt_option_showSummary", "asmt_option_showFeedback", "asmt_option_showModelAnswer",
		                      "asmt_option_finalMsg_editor", "asmt_option_certificate", "asmt_option_certificate_pct", "asmt_option_email",
		                      "asmt_option_shuffle", "asmt_option_showHints", 
		                      "asmt_option_evalDesign", "asmt_option_attribution", "asmt_option_type_new"]);
		me.ui.itemNav = new e3_ItemNav();
		me.ui.editor = new e3_EditorCK(me.ui.asmt_option_finalMsg_editor, {height: 150});
	};

	this.start = function(assessment, assessments)
	{
		if (assessments !== undefined) me.assessments = assessments;

		main.onExit = me.checkExit;

		me.load(assessment.id);
	};

	this.load = function(assessmentId)
	{
		var params = main.portal.cdp.params();
		params.url.site = main.portal.site.id;
		params.url.assessment = assessmentId;
		main.portal.cdp.request("assessment_getOptions", params, function(data)
		{
			me.assessment = data.assessment;

			me.ui.itemNav.inject(main.ui.asmt_itemnav, {doneFunction: me.done, pos: position(me.assessment, me.assessments), navigateFunction: me.goAssessment});
			me.makeEdit();
			me.populate();
		});
	};

	this.makeEdit = function()
	{
		me.edit = new e3_Edit(me.assessment, ["id", "valid", "attribution",
		                                      "schedule.status", "schedule.close",
		                                      "evaluationDesign.rubric", /*"evaluationDesign.mutablePoints", "evaluationDesign.actualPoints", "evaluationDesign.actualCategory",*/ "evaluationDesign.categoryPosition"
		                                      ], function(changed) // TODO: tune
		{
			main.ui.modebar.enableSaveDiscard(changed ? me.saveCancel : null);
			me.ui.itemNav.enableSave(changed ? me.saveCancel : null);
		});
		me.edit.setFilters({"title": me.edit.stringFilter});
		me.edit.setFilters({"password": me.edit.stringFilter, "numTries": me.edit.numberFilter, "timeLimit": me.edit.stringFilter, 
							"reviewAfter": me.edit.dateFilter, "awardPct": me.edit.numberFilter, "resultsEmail": me.edit.stringFilter}, "options");  // TODO: tune filters

		main.ui.modebar.enableSaveDiscard(null);
		me.ui.itemNav.enableSave(null);
	};

	this.populate = function()
	{
		show(me.ui.asmt_option_type_new, (main.fs == 9));

		me.edit.setupFilteredEdit(me.ui.asmt_option_title, me.edit, "title", function(val, finalChange)
		{
			me.ui.asmt_option_bar_title.text(val);
		});
		me.ui.asmt_option_bar_title.text(me.edit.title);

		me.edit.setupRadioEdit("asmt_option_type", me.edit, "type");
		me.edit.setupCheckEdit(me.ui.asmt_option_shuffle, me.edit.options, "shuffle");
		me.edit.setupCheckEdit(me.ui.asmt_option_showHints, me.edit.options, "hints");
		me.edit.setupRadioEdit("asmt_option_layout", me.edit.options, "layout");
		me.edit.setupRadioEdit("asmt_option_partNumbering", me.edit.options, "numbering");
		me.edit.setupDateEdit(me.ui.asmt_option_open, me.edit.schedule, "open", main.portal.timestamp, true);
		me.edit.setupCheckEdit(me.ui.asmt_option_hide, me.edit.schedule, "hide");
		me.edit.setupDateEdit(me.ui.asmt_option_due, me.edit.schedule, "due", main.portal.timestamp, false);
		me.edit.setupDateEdit(me.ui.asmt_option_allow, me.edit.schedule, "allowUntil", main.portal.timestamp, false);
		me.edit.setupCheckEdit(me.ui.asmt_option_published, me.edit, "published");
		me.edit.setupFilteredEdit(me.ui.asmt_option_password, me.edit.options, "password");
		me.edit.setupCheckEdit(me.ui.asmt_option_requireHonorPledge, me.edit.options, "pledge");

		me.edit.setupRadioEdit("asmt_option_tries", me.edit.options, "triesSet", function(val) // TODO: triesSet a boolean ... make it work with this radio
		{
			show(me.ui.asmt_option_numTries, (val == "true"));
			if (val != "true")
			{
				me.ui.asmt_option_numTries.val("");
				me.edit.set(me.edit.options, "numTries", null);
			}
		});
		me.edit.setupFilteredEdit(me.ui.asmt_option_numTries, me.edit.options, "numTries");
		show(me.ui.asmt_option_numTries, (me.edit.options.triesSet));

		me.edit.setupRadioEdit("asmt_option_limit", me.edit.options, "timeLimitSet", function(val)// TODO: timeLimitSet a boolean ... make it work with this radio
		{
			show(me.ui.asmt_option_timeLimit, (val == "true"));
			if (val != "true")
			{
				me.ui.asmt_option_timeLimit.val("");
				me.edit.set(me.edit.options, "timeLimit", null);
			}
		});
		me.edit.setupFilteredEdit(me.ui.asmt_option_timeLimit, me.edit.options, "timeLimit");
		show(me.ui.asmt_option_timeLimit, (me.edit.options.timeLimitSet));

		me.edit.setupRadioEdit("asmt_option_reviewAvailable", me.edit.evaluationDesign, "reviewWhen", function(val)
		{
			show([me.ui.asmt_option_reviewAfterDate, me.ui.asmt_option_reviewAfterDate.next()], (val == 2));
			if (val != 2)
			{
				me.ui.asmt_option_reviewAfterDate.val("");
				me.edit.set(me.edit.evaluationDesign, "reviewAfter", null);
			}
		});
		me.edit.setupDateEdit(me.ui.asmt_option_reviewAfterDate, me.edit.evaluationDesign, "reviewAfter", main.portal.timestamp, true);
		show([me.ui.asmt_option_reviewAfterDate, me.ui.asmt_option_reviewAfterDate.next()], (me.edit.evaluationDesign.reviewWhen == 2));

		me.edit.setupCheckEdit(me.ui.asmt_option_showSummary, me.edit.options, "showSummary");
		me.edit.setupRadioEdit("asmt_option_reviewIncludes", me.edit.options, "reviewIncludes");
		me.edit.setupCheckEdit(me.ui.asmt_option_showFeedback, me.edit.options, "showFeedback");
		me.edit.setupCheckEdit(me.ui.asmt_option_showModelAnswer, me.edit.options, "showModelAnswer");		
		me.edit.setupEditorEdit(me.ui.editor, me.edit.options, "finalMsg", null, main.fs);

		me.edit.setupCheckEdit(me.ui.asmt_option_certificate, me.edit.options, "award", function(val)
		{
			show([me.ui.asmt_option_certificate_pct, me.ui.asmt_option_certificate_pct.next()], val);
			if (!val)
			{
				me.ui.asmt_option_certificate_pct.val("");
				me.edit.set(me.edit.options, "awardPct", null);
			}
		});
		me.edit.setupFilteredEdit(me.ui.asmt_option_certificate_pct, me.edit.options, "awardPct");
		show([me.ui.asmt_option_certificate_pct, me.ui.asmt_option_certificate_pct.next()], me.edit.options.award);

		me.edit.setupFilteredEdit(me.ui.asmt_option_email, me.edit.options, "resultsEmail");

		main.ui.evaluation.edit.set(me.ui.asmt_option_evalDesign, me.edit.evaluationDesign, me.edit, main.rubrics, main.categories);

 		if (me.assessment.id != -1) new e3_Attribution().inject(me.ui.asmt_option_attribution, me.assessment.id, me.assessment.attribution);
 		show(me.ui.asmt_option_attribution, (me.assessment.id != -1));
	};

	this.saveCancel = function(mode, deferred)
	{
		if (mode)
		{
			me.save(deferred);
		}
		else
		{
			me.edit.revert();
			me.populate();
			if (deferred !== undefined) deferred();
		}
	};

	this.checkExit = function(deferred)
	{
		if (me.edit.changed())
		{
			main.portal.confirmNavigationWithChanges(function()
			{
				me.save(deferred);				
			}, function()
			{
				me.edit.revert();
				// me.populate();
				if (deferred !== undefined) deferred();
			});

			return false;
		}

		return true;
	};

	this.done = function()
	{
		if (me.edit.changed())
		{
			me.save(function(){main.startManage();})
		}
		else
		{
			main.startManage();
		}
	};

	this.goAssessment = function(assessment)
	{
		main.startOption(assessment);
	};

	this.save = function(deferred)
	{
		var params = main.portal.cdp.params();
		params.url.site = main.portal.site.id;
		params.url.assessment = me.assessment.id;
		me.edit.params("", params);
		main.portal.cdp.request("assessment_saveOptions assessment_getOptions", params, function(data)
		{
			me.assessment = data.assessment;

			me.makeEdit();
			me.populate();
			if (deferred !== undefined) deferred();
		});
	};
};

function AssessmentInstruction(main)
{
	var me = this;
	
	this.ui = null;

	this.assessment = null;
	this.assessments = null;

	this.edit = null;

	this.init = function()
	{
		me.ui = findElements(["asmt_instruction_title", "asmt_instruction_assessment", "asmt_instruction_parts",
		                      "asmt_instruction_template"]);
		me.ui.itemNav = new e3_ItemNav();
		me.ui.editor = new e3_EditorCK(me.ui.asmt_instruction_assessment, {height: 150});
	};

	this.start = function(assessment, assessments)
	{
		if (assessments !== undefined) me.assessments = assessments;
		main.onExit = me.checkExit;
		me.load(assessment.id);
	};

	this.load = function(id)
	{
		var params = main.portal.cdp.params();
		params.url.site = main.portal.site.id;
		params.url.assessment = id;
		main.portal.cdp.request("assessment_getInstructions", params, function(data)
		{
			me.assessment = data.assessment;
			me.ui.itemNav.inject(main.ui.asmt_itemnav, {doneFunction: me.done, pos: position(me.assessment, me.assessments), navigateFunction: me.goAssessment});

			me.makeEdit();
			me.populate();
		});
	};

	this.makeEdit = function()
	{
		me.edit = new e3_Edit(me.assessment, ["id", "type", "title", "valid", "published", "allowRemove",
		                                      "design.parts[].title", "design.parts[].ordering", "design.parts[].points",
		                                      "design.parts[].shuffle", "design.parts[].questions", "design.details"], function(changed)
		{
			main.ui.modebar.enableSaveDiscard(changed ? me.saveCancel : null);
			me.ui.itemNav.enableSave(changed ? me.saveCancel : null);
		});
		me.edit.setFilters({"instructions": me.edit.stringFilter, "parts[].instructions": me.edit.stringFilter}, "design");
	
		main.ui.modebar.enableSaveDiscard(null);
		me.ui.itemNav.enableSave(null);
	};

	this.populate = function()
	{
		me.ui.asmt_instruction_title.text(me.assessment.title);
		me.edit.setupEditorEdit(me.ui.editor, me.edit.design, "instructions", null, main.fs);

		me.ui.asmt_instruction_parts.empty();
		me.ui.parts = [];
		$.each(me.assessment.design.parts, function(index, part)
		{
			var inEdit = findIdInList(part.id, me.edit.design.parts);

			var msg = null;
			if (part.title != null)
			{
				msg = main.i18n.lookup("msg_partInstructionsTitle", "Instructions for Part %0 of %1: %2:", "html", [part.ordering.position, part.ordering.size, part.title]);
			}
			else
			{
				msg = main.i18n.lookup("msg_partInstructions", "Instructions for Part %0 of %1:", "html", [part.ordering.position, part.ordering.size]);
			}

			var cell = clone(me.ui.asmt_instruction_template, ["asmt_instruction_template_body", "asmt_instruction_template_label", "asmt_instruction_template_editor"]);
			cell.asmt_instruction_template_label.text(msg);
			me.ui.asmt_instruction_parts.append(cell.asmt_instruction_template_body);
			
			me.ui.parts.push({ui: cell, part: inEdit});
		});
		
		$.each(me.ui.parts, function(index, part)
		{
			var editor = new e3_EditorCK(part.ui.asmt_instruction_template_editor, {height: 150});
			me.edit.setupEditorEdit(editor, part.part, "instructions", null, main.fs);
		});
	};

	this.saveCancel = function(mode, deferred)
	{
		if (mode)
		{
			me.save(deferred);
		}
		else
		{
			me.edit.revert();
			me.populate();
			if (deferred !== undefined) deferred();
		}
	};

	this.checkExit = function(deferred)
	{
		if (me.edit.changed())
		{
			main.portal.confirmNavigationWithChanges(function()
			{
				me.save(deferred);				
			}, function()
			{
				me.edit.revert();
				// me.populate();
				if (deferred !== undefined) deferred();
			});

			return false;
		}

		return true;
	};

	this.done = function()
	{
		if (me.edit.changed())
		{
			me.save(function(){main.startManage();})
		}
		else
		{
			main.startManage();
		}
	};

	this.goAssessment = function(assessment)
	{
		main.startInstruction(assessment);
	};

	this.save = function(deferred)
	{
		var params = main.portal.cdp.params();
		params.url.site = main.portal.site.id;
		params.url.assessment = me.assessment.id;
		me.edit.params("", params);
		main.portal.cdp.request("assessment_saveInstructions assessment_getInstructions", params, function(data)
		{
			me.assessment = data.assessment;

			me.makeEdit();
			me.populate();
			if (deferred !== undefined) deferred();
		});
	};
};

function AssessmentPart(main)
{
	var me = this;
	
	this.ui = null;

	this.assessment = null;
	this.assessments = null;
	this.edit = null;

	this.init = function()
	{
		me.ui = findElements(["asmt_part_add", "asmt_part_delete", "asmt_part_title", "asmt_part_instructions", "asmt_part_table"]);
		me.ui.itemNav = new e3_ItemNav();

		me.ui.table = new e3_Table(me.ui.asmt_part_table);
		me.ui.table.setupSelection("asmt_part_select", me.updateActions);
		me.ui.table.enableReorder(me.applyPartOrder);
		me.ui.table.selectAllHeader(1, main.ui.asmt_header_part);

		onClick(me.ui.asmt_part_instructions, me.instructions);
		onClick(me.ui.asmt_part_add, me.add);
		onClick(me.ui.asmt_part_delete, me.remove);
		
		setupHoverControls([me.ui.asmt_part_add, me.ui.asmt_part_delete, me.ui.asmt_part_instructions]);
	};

	this.updateActions = function()
	{
		me.ui.table.updateActions([], [me.ui.asmt_part_delete]);
	};

	this.applyPartOrder = function(order)
	{
		me.edit.set(me.edit, "reorder", order.join());
	};

	this.start = function(assessment, assessments)
	{
		if (assessments !== undefined) me.assessments = assessments;
		main.onExit = me.checkExit;
		
		me.load(assessment.id);
	}
	
	this.load = function(id)
	{
		var params = main.portal.cdp.params();
		params.url.site = main.portal.site.id;
		params.url.assessment = id;
		main.portal.cdp.request("assessment_getParts", params, function(data)
		{
			me.assessment = data.assessment;
			me.ui.itemNav.inject(main.ui.asmt_itemnav, {doneFunction: me.done, pos: position(me.assessment, me.assessments), navigateFunction: me.goAssessment});

			me.makeEdit();
			me.populate();
		});
	};

	this.makeEdit = function()
	{
		me.edit = new e3_Edit(me.assessment, ["id", "type", "title", "valid", "published", "allowRemove", "design.details", "design.instructions",
		                                      "design.parts[].instructions", "design.parts[].ordering", "design.parts[].points", "design.parts[].questions"], function(changed) // TODO: tune
		{
			main.ui.modebar.enableSaveDiscard(changed ? me.saveCancel : null);
			me.ui.itemNav.enableSave(changed ? me.saveCancel : null);
		});
		me.edit.setFilters({"design.parts[].title": me.edit.stringFilter});
	
		main.ui.modebar.enableSaveDiscard(null);
		me.ui.itemNav.enableSave(null);
	};

	this.populate = function()
	{
		me.ui.asmt_part_title.text(me.assessment.title);

		me.ui.table.clear();
		$.each(me.edit.design.parts, function(index, part)
		{
			var fullPart = findIdInList(part.id, me.assessment.design.parts);
	
			me.ui.table.row();
			me.ui.table.selectBox(part.id);
			me.ui.table.reorder(main.i18n.lookup("msg_reorder", "drag to reorder"), part.id);

			// title
			var input = me.ui.table.input({type:"text"}, null, {width: "calc(100vw - 100px - 464px)", minWidth:"calc(1200px - 100px - 464px)"}).find("input");
			me.edit.setupFilteredEdit(input, part, "title");
			
			// shuffle
			input = me.ui.table.input({type: "checkbox"}, null, {width: 60}).find("input");
			me.edit.setupCheckEdit(input, part, "shuffle");

			// questions
			me.ui.table.text(fullPart.questions, null, {width:100});

			// points
			me.ui.table.text(fullPart.points, null, {width:100});

			// context
			me.ui.table.contextMenu(
			[
				{title: main.i18n.lookup("cm_delete", "Delete"), action:function(){me.removeA(part);}}
	        ]);
		});
		me.ui.table.done();
	};

	this.saveCancel = function(mode, deferred)
	{
		if (mode)
		{
			me.save(deferred);
		}
		else
		{
			me.edit.revert();
			me.populate();
			if (deferred !== undefined) deferred();
		}
	};

	this.checkExit = function(deferred)
	{
		if (me.edit.changed())
		{
			main.portal.confirmNavigationWithChanges(function()
			{
				me.save(deferred);				
			}, function()
			{
				me.edit.revert();
				// me.populate();
				if (deferred !== undefined) deferred();
			});

			return false;
		}

		return true;
	};

	this.done = function()
	{
		if (me.edit.changed())
		{
			me.save(function(){main.startManage();})
		}
		else
		{
			main.startManage();
		}
	};

	this.goAssessment = function(assessment)
	{
		main.startPart(assessment);
	};

	this.save = function(deferred)
	{
		var params = main.portal.cdp.params();
		params.url.site = main.portal.site.id;
		params.url.assessment = me.assessment.id;
		me.edit.params("", params);
		main.portal.cdp.request("assessment_saveParts assessment_getParts", params, function(data)
		{
			me.assessment = data.assessment;

			me.makeEdit();
			me.populate();
			if (deferred !== undefined) deferred();
		});
	};

	this.instructions = function()
	{
		if (!me.checkExit(function(){me.instructions();})) return;
		main.startInstruction(me.assessment, me.assessments);
	};

	this.remove = function()
	{
		var ids = me.ui.table.selected();

		if (ids.length == 0)
		{
			main.portal.dialogs.openAlert(main.ui.asmt_selectFirst);
			return;
		}

		$.each(ids, function(index, id)
		{
			me.edit.remove(me.edit.design, "parts", id);
		});

		me.populate();
	};

	this.removeA = function(part)
	{
		me.edit.remove(me.edit.design, "parts", part.id);
		me.populate();
	};
	
	this.newPartId = -1;

	this.add = function()
	{
		var newPart = {id: me.newPartId--, shuffle: false};
		me.edit.add(me.edit.design, "parts", newPart);

		var newFullPart = {id: newPart.id, shuffle: newPart.shuffle, points: 0, questions: 0};
		me.assessment.design.parts.push(newFullPart);

		me.populate();
	};
};

function Assessment()
{
	var me = this;

	this.portal = null;
	this.i18n = new e3_i18n(assessment_i10n, "en-us");

	this.viewMode = null;
	this.manageMode = null;
	this.deliverMode = null;
	this.tocMode = null;
	this.enterMode = null;
	this.gradeMode = null;
	this.gradeAssessmentMode = null;
	this.gradeSubmissionMode = null;
	this.editAssessmentMode = null;
	this.optionMode = null;
	this.reviewMode = null;
	this.poolsMode = null;
	this.editPoolMode = null;
	this.instructionMode = null;
	this.partMode = null;

	this.ui = null;
	this.onExit = null;

	this.rubrics = [];
	this.categories = [];

	this.fs = 9; // 0 for homepage filesystem, 1 for CHS/resources file system, 2 - mneme, 9 for serenity fs

	this.SubmitStatus = {again: "A", nothing: "N", resume: "R", start: "S", test: "T"};
	this.SubmissionStatus = {autoComplete: "A", evaluationNonSubmit: "E", userFinished: "U"};
	this.AssessmentType = {assignment: "A", essay: "E", fce: "F", offline: "O", survey: "S", test: "T"};
	this.PartType = {select: "S", draw: "D"};
	this.Layout = {question: 0, part: 1, all: 2};
	this.Numbering = {continuous: 0, restart: 1};
	this.TOCStatus = {unanswered: 1, review: 2,  reason: 3, answered: 0};
	this.EvaluationType = {official: 1, peer: 2};

	this.findAssessment = function(id)
	{
		return me.findById(me.assessments, id);
	};

	this.publicationStatusDotTd = function(assessment, table)
	{
		if (!assessment.valid)
		{
			table.dot(Dots.alert, me.i18n.lookup("msg_invalid", "invalid"));
		}
		else if (!assessment.published)
		{
			table.dot(Dots.red, me.i18n.lookup("msg_unpublished", "not published"));
		}
		else if (assessment.schedule.status == ScheduleStatus.willOpenHide)
		{
			table.dot(Dots.gray, me.i18n.lookup("msg_willOpenHidden", "hidden until open"));
		}
		else
		{
			table.dot(Dots.green, me.i18n.lookup("msg_published", "published"));
		}
	};
	
	this.scheduleStatusDotTd = function(schedule, table)
	{
		// var ScheduleStatus = {closed:4, open:3, willOpen:1, willOpenHide:2};
		// nothing for open, yellow for will open, grey for hidden will open, closed for closed
		if (schedule.status == ScheduleStatus.closed)
		{
			table.dot(Dots.closed, me.i18n.lookup("msg_closed", "closed"));
		}
		else if (schedule.status == ScheduleStatus.willOpen)
		{
			table.dot(Dots.yellow, me.i18n.lookup("msg_willOpen", "will open"));
		}
		else if (schedule.status == ScheduleStatus.willOpenHide)
		{
			table.dot(Dots.gray, me.i18n.lookup("msg_willOpenHidden", "hidden until open"));
		}
//		else if (schedule.status == ScheduleStatus.open)
//		{
//			table.dot(Dots.green, me.i18n.lookup("msg_open", "open"));
//		}
		else
		{
			table.dot(Dots.none);
		}
	};

	this.typeTd = function(assessment, table)
	{
		table.text(me.i18n.lookup("msg_type_" + assessment.type), "e3_text special light", {fontSize:11, width:60, textTransform: "uppercase"});
	};

	this.init = function()
	{
		me.i18n.localize();
		
		me.ui = findElements(["asmt_header", "asmt_modebar", "asmt_headerbar", "asmt_headerbar2", "asmt_headerbar3", "asmt_itemnav",
		                      "asmt_view_edit",
		                      "asmt_view",  "asmt_view_edit", "asmt_bar_view", "asmt_header_view",
		                      "asmt_manage", "asmt_bar_manage", "asmt_bar2_manage", "asmt_header_manage",
		                      "asmt_edit", "asmt_bar_edit", "asmt_bar2_edit", "asmt_bar3_edit", "asmt_header_edit",
		                      "asmt_option", "asmt_bar_option",
		                      "asmt_grade", "asmt_bar_grade", "asmt_header_grade",
		                      "asmt_gradeAsmt", "asmt_bar_gradeAsmt", "asmt_bar2_gradeAsmt", "asmt_header_gradeAsmt",
		                      "asmt_enter", "asmt_bar_enter",  "asmt_bar2_enter",
		                      "asmt_toc", "asmt_bar_toc", "asmt_header_toc",
		                      "asmt_deliver", "asmt_bar_deliver",
		                      "asmt_review", "asmt_bar_review", "asmt_bar2_review",
		                      "asmt_gradeSubmission", "asmt_bar_gradeSubmission", "asmt_bar2_gradeSubmission",
		                      "asmt_pools", "asmt_header_pools", "asmt_bar_pools",
		                      "asmt_editPool", "asmt_header_editPool", "asmt_bar_editPool", "asmt_bar2_editPool", "asmt_bar3_editPool",
		                      "asmt_selectFirst", "asmt_confirmDelete", "asmt_select1First",
		                      "asmt_instruction", "asmt_bar_instruction",
		                      "asmt_part", "asmt_header_part", "asmt_bar_part", "asmt_bar2_part",
		                      "asmt_title_template"]);
		me.portal = portal_tool.features({onExit:function(deferred){return me.checkExit(deferred);}, pin:[{ui:me.ui.asmt_header}]});

		me.ui.evaluation = new e3_Evaluation(me.portal.cdp, me.portal.dialogs, me.portal.timestamp);

		me.viewMode = new AssessmentView(me);
		me.viewMode.init();
		me.enterMode = new AssessmentEnter(me);
		me.enterMode.init();
		me.tocMode = new AssessmentToc(me);
		me.tocMode.init();
		me.deliverMode = new AssessmentDeliver(me);
		me.deliverMode.init();
		me.reviewMode = new AssessmentReview(me);
		me.reviewMode.init();

		if (me.portal.site.role >= Role.instructor)
		{
			me.manageMode = new AssessmentManage(me);
			me.manageMode.init();
			me.gradeMode = new AssessmentGrade(me);
			me.gradeMode.init();
			me.gradeAssessmentMode = new AssessmentGradeAssessment(me);
			me.gradeAssessmentMode.init();
			me.gradeSubmissionMode = new AssessmentGradeSubmission(me);
			me.gradeSubmissionMode.init();
			me.editAssessmentMode = new AssessmentEditAssessment(me);
			me.editAssessmentMode.init();
			me.optionMode = new AssessmentOption(me);
			me.optionMode.init();
			me.poolsMode = new AssessmentPools(me);
			me.poolsMode.init();
			me.editPoolMode = new AssessmentEditPool(me);
			me.editPoolMode.init();
			me.instructionMode = new AssessmentInstruction(me);
			me.instructionMode.init();
			me.partMode = new AssessmentPart(me);
			me.partMode.init();

			me.ui.modebar = new e3_Modebar(me.ui.asmt_modebar);
			me.modes =
			[
				{name:me.i18n.lookup("mode_view", "VIEW"), func:function(){me.startView();}},
				{name:me.i18n.lookup("mode_edit", "ASSESSMENTS"), func:function(){me.startManage();}},
				{name:me.i18n.lookup("mode_pool", "POOLS"), func:function(){me.startPools();}}
//				{name:me.i18n.lookup("mode_grade", "GRADE"), func:function(){me.startGrade();}}
			];
			me.ui.modebar.set(me.modes, 0);
			
			onClick(me.ui.asmt_view_edit, me.startManage);
		}
	};

	this.start = function()
	{
		if (me.portal.site.role >= Role.instructor)
		{
			me.startManage();
		}
		else
		{
			me.startView();
		}
	};

	this.startView = function()
	{
		if (!me.checkExit(function(){me.startView();})) return;
		me.mode([me.ui.asmt_headerbar, me.ui.asmt_bar_view, me.ui.asmt_header_view, me.ui.asmt_view, ((me.portal.site.role >= Role.instructor) ? me.ui.asmt_view_edit : null)]);
		if (me.ui.modebar !== undefined) me.ui.modebar.showSelected(0);
		me.viewMode.start();
	};

	this.startEnter = function(assessment)
	{
		if (!me.checkExit(function(){me.startEnter(assessment);})) return;
		me.mode([me.ui.asmt_headerbar, me.ui.asmt_bar_enter, me.ui.asmt_headerbar2, me.ui.asmt_bar2_enter, me.ui.asmt_enter]);
		if (me.ui.modebar !== undefined) me.ui.modebar.showSelected(0);
		me.enterMode.start(assessment);
	};

	this.startToc = function(submissionId)
	{
		if (!me.checkExit(function(){me.startToc(submissionId);})) return;
		me.mode([me.ui.asmt_headerbar, me.ui.asmt_itemnav, me.ui.asmt_bar_toc, me.ui.asmt_header_toc, me.ui.asmt_toc]);
		if (me.ui.modebar !== undefined) me.ui.modebar.showSelected(0);
		me.tocMode.start(submissionId);
	};

	this.startDeliver = function(assessment, submission)
	{
		if (!me.checkExit(function(){me.startDeliver(assessment, submission);})) return;
		me.mode([me.ui.asmt_headerbar, me.ui.asmt_bar_deliver, me.ui.asmt_deliver]);
		if (me.ui.modebar !== undefined) me.ui.modebar.showSelected(0);
		me.deliverMode.start(assessment, submission);
	};

	this.startReview = function(submissionId)
	{
		if (!me.checkExit(function(){me.startReview(submissionId);})) return;
		me.mode([me.ui.asmt_headerbar, me.ui.asmt_bar_review, me.ui.asmt_headerbar2, me.ui.asmt_bar2_review, me.ui.asmt_review]);
		if (me.ui.modebar !== undefined) me.ui.modebar.showSelected(0);
		me.reviewMode.start(submissionId);
	};

	this.startManage = function()
	{
		if (!me.checkExit(function(){me.startManage();})) return;
		me.mode([me.ui.asmt_modebar, me.ui.asmt_headerbar, me.ui.asmt_bar_manage, me.ui.asmt_headerbar2, me.ui.asmt_bar2_manage, me.ui.asmt_header_manage, me.ui.asmt_manage]);
		me.ui.modebar.showSelected(1);
		me.manageMode.start();
	};

	this.startEditAssessment = function(assessment, assessments)
	{
		if (!me.checkExit(function(){me.startEditAssessment(assessment, assessments);})) return;
		me.mode([me.ui.asmt_modebar,  me.ui.asmt_headerbar, me.ui.asmt_itemnav, me.ui.asmt_bar_edit, me.ui.asmt_headerbar2, me.ui.asmt_bar2_edit,
		         me.ui.asmt_headerbar3, me.ui.asmt_bar3_edit, me.ui.asmt_headerbar, me.ui.asmt_header_edit, me.ui.asmt_edit]);
		me.ui.modebar.showSelected(1);
		me.editAssessmentMode.start(assessment, assessments);
	};

	this.startOption = function(assessment, assessments)
	{
		if (!me.checkExit(function(){me.startOption(assessment, assessments);})) return;
		me.mode([me.ui.asmt_modebar,  me.ui.asmt_headerbar, me.ui.asmt_itemnav, me.ui.asmt_bar_option, me.ui.asmt_option]);
		me.ui.modebar.showSelected(1);
		me.optionMode.start(assessment, assessments);
	};

	this.startInstruction = function(assessment, assessments)
	{
		if (!me.checkExit(function(){me.startInstruction(assessment, assessments);})) return;
		me.mode([me.ui.asmt_modebar,  me.ui.asmt_headerbar, me.ui.asmt_itemnav, me.ui.asmt_bar_instruction, me.ui.asmt_instruction]);
		me.ui.modebar.showSelected(1);
		me.instructionMode.start(assessment, assessments);
	};

	this.startPart = function(assessment, assessments)
	{
		if (!me.checkExit(function(){me.startPart(assessment, assessments);})) return;
		me.mode([me.ui.asmt_modebar,  me.ui.asmt_headerbar, me.ui.asmt_itemnav, me.ui.asmt_bar_part, me.ui.asmt_headerbar2, me.ui.asmt_bar2_part, me.ui.asmt_header_part, me.ui.asmt_part]);
		me.ui.modebar.showSelected(1);
		me.partMode.start(assessment, assessments);
	};

//	this.startGrade = function()
//	{
//		if (!me.checkExit(function(){me.startGrade();})) return;
//		me.mode([me.ui.asmt_modebar, me.ui.asmt_headerbar, me.ui.asmt_bar_grade, me.ui.asmt_header_grade, me.ui.asmt_grade]);
//		me.ui.modebar.showSelected(3);
//		me.gradeMode.start();
//	};

	this.startGradeAssessment = function(assessment, assessments)
	{
		if (!me.checkExit(function(){me.startGradeAssessment(assessment, assessments);})) return;
		me.mode([me.ui.asmt_modebar, me.ui.asmt_headerbar, me.ui.asmt_itemnav, me.ui.asmt_bar_gradeAsmt, me.ui.asmt_headerbar2, me.ui.asmt_bar2_gradeAsmt, me.ui.asmt_header_gradeAsmt, me.ui.asmt_gradeAsmt]);
		if (me.ui.modebar !== undefined) me.ui.modebar.showSelected(1); // 3
		me.gradeAssessmentMode.start(assessment, assessments);
	};

	this.startGradeSubmission = function(submission, submissions, assessment)
	{
		if (!me.checkExit(function(){me.startGradeSubmission(submission, submissions, assessment);})) return;
		me.mode([me.ui.asmt_modebar, me.ui.asmt_headerbar, me.ui.asmt_itemnav, me.ui.asmt_bar_gradeSubmission, me.ui.asmt_headerbar2, me.ui.asmt_bar2_gradeSubmission, me.ui.asmt_gradeSubmission]);
		if (me.ui.modebar !== undefined) me.ui.modebar.showSelected(1); // 3
		me.gradeSubmissionMode.start(submission, submissions, assessment);
	};

	this.startPools = function()
	{
		if (!me.checkExit(function(){me.startPools();})) return;
		me.mode([me.ui.asmt_modebar, me.ui.asmt_headerbar, me.ui.asmt_pools, me.ui.asmt_header_pools, me.ui.asmt_bar_pools]);
		me.ui.modebar.showSelected(2);
		me.poolsMode.start();
	};

	this.startEditPool = function(pool, pools)
	{
		if (!me.checkExit(function(){me.startEditPool(pool, pools);})) return;
		me.mode([me.ui.asmt_modebar, me.ui.asmt_headerbar, me.ui.asmt_itemnav, me.ui.asmt_bar_editPool, me.ui.asmt_headerbar2, me.ui.asmt_bar2_editPool, me.ui.asmt_headerbar3, me.ui.asmt_bar3_editPool,
		         me.ui.asmt_header_editPool, me.ui.asmt_editPool]);
		me.ui.modebar.showSelected(2);
		me.editPoolMode.start(pool, pools);
	};

	this.mode = function(elements)
	{
		hide([
		      me.ui.asmt_modebar, me.ui.asmt_headerbar, me.ui.asmt_headerbar2, me.ui.asmt_headerbar3, me.ui.asmt_itemnav,
		      me.ui.asmt_view, me.ui.asmt_bar_view, me.ui.asmt_header_view, me.ui.asmt_view_edit,
		      me.ui.asmt_manage, me.ui.asmt_bar_manage, me.ui.asmt_bar2_manage, me.ui.asmt_header_manage,
		      me.ui.asmt_grade, me.ui.asmt_bar_grade, me.ui.asmt_header_grade,
		      me.ui.asmt_gradeAsmt, me.ui.asmt_bar_gradeAsmt, me.ui.asmt_bar2_gradeAsmt, me.ui.asmt_header_gradeAsmt,
		      me.ui.asmt_enter, me.ui.asmt_bar_enter, me.ui.asmt_bar2_enter,
		      me.ui.asmt_toc, me.ui.asmt_bar_toc, me.ui.asmt_header_toc,
		      me.ui.asmt_deliver, me.ui.asmt_bar_deliver,
		      me.ui.asmt_review, me.ui.asmt_bar_review, me.ui.asmt_bar2_review,
		      me.ui.asmt_gradeSubmission, me.ui.asmt_bar_gradeSubmission, me.ui.asmt_bar2_gradeSubmission,
		      me.ui.asmt_pools, me.ui.asmt_header_pools, me.ui.asmt_bar_pools,
		      me.ui.asmt_editPool, me.ui.asmt_header_editPool, me.ui.asmt_bar_editPool, me.ui.asmt_bar2_editPool, me.ui.asmt_bar3_editPool,
		      me.ui.asmt_edit, me.ui.asmt_bar_edit, me.ui.asmt_bar2_edit, me.ui.asmt_bar3_edit, me.ui.asmt_header_edit,
		      me.ui.asmt_instruction, me.ui.asmt_bar_instruction,
		      me.ui.asmt_part, me.ui.asmt_bar_part, me.ui.asmt_bar2_part, me.ui.asmt_header_part,
		      me.ui.asmt_option, me.ui.asmt_bar_option]);
		me.onExit = null;
		me.portal.resetScrolling();
		show(elements);
	};

	this.checkExit = function(deferred)
	{
		if (me.onExit == null) return true;
		return me.onExit(deferred);
	};
	
	this.sortByTitle = function(assessments, direction)
	{
		var sorted = [].concat(assessments);
		var adjust = (direction == 'A') ? 1 : -1;
		sorted.sort(function(a, b)
		{
			var rv = adjust * compareS(a.title, b.title);
			if (rv == 0)
			{
				rv = compareN(a.id, b.id);
			}
			return rv;
		});

		return sorted;
	};

	this.sortByType = function(assessments, direction)
	{
		var sorted = [].concat(assessments);
		var adjust = (direction == 'A') ? 1 : -1;
		sorted.sort(function(a, b)
		{
			var rv = adjust * compareN(a.type, b.type);
			if (rv == 0)
			{
				rv = compareS(a.title, b.title);
				if (rv == 0)
				{
					rv = compareN(a.id, b.id);
				}
			}
			return rv;
		});

		return sorted;
	};

	this.publicationStatusRanking = function(assessment)
	{
		// published, published hidden, unpublished
		if (assessment.published)
		{
			if (assessment.schedule.hide)
			{
				return 2;
			}
			return 1;
		}
		return 3;
	};

	this.submitStatusRanking = function(assessment)
	{
		// sort order (a) other, future, hiddenTillOpen, over, complete, completeReady, ready, inProgress, inProgressAlert (reversed)

		// TODO: inProgressAlert
		if (assessment.submitStatus == me.SubmitStatus.resume)
		{
			return 0;
		}
		else if (assessment.submitStatus == me.SubmitStatus.start)
		{
			return 1;
		}
		else if (assessment.submitStatus == me.SubmitStatus.again)
		{
			return 2;
		}
		return 3; // complete, over, hiddenTillOpen, future
	};

	this.sortByPublicationStatus = function(assessments, direction)
	{
		var sorted = [].concat(assessments);
		var adjust = (direction == 'A') ? 1 : -1;
		sorted.sort(function(a, b)
		{
			var rv = adjust * compareN(me.publicationStatusRanking(a), me.publicationStatusRanking(b));
			if (rv == 0)
			{
				rv = compareS(a.title, b.title);
				if (rv == 0)
				{
					rv = compareN(a.id, b.id);
				}
			}
			return rv;
		});

		return sorted;
	};

	this.sortBySubmitStatus = function(assessments, direction)
	{
		var sorted = [].concat(assessments);
		var adjust = (direction == 'A') ? 1 : -1;
		sorted.sort(function(a, b)
		{
			var rv = adjust * compareN(me.submitStatusRanking(a), me.submitStatusRanking(b));
			if (rv == 0)
			{
				// second by due date ascending
				var aVal = ((a.schedule == null) ? null : a.schedule.due);
				var bVal = ((b.schedule == null) ? null : b.schedule.due);
				var rv = compareN(aVal, bVal);
				if (rv == 0)
				{
					// then by title, id
					rv = compareS(a.title, b.title);
					if (rv == 0)
					{
						rv = compareN(a.id, b.id);
					}
				}
			}
			return rv;
		});

		return sorted;
	};

	this.sortByOpen = function(assessments, direction)
	{
		var sorted = [].concat(assessments);
		var adjust = (direction == 'A') ? 1 : -1;
		sorted.sort(function(a, b)
		{
			var aVal = ((a.schedule == null) ? null : a.schedule.open);
			var bVal = ((b.schedule == null) ? null : b.schedule.open);
			var rv = adjust * compareN(aVal, bVal);
			if (rv == 0)
			{
				rv = compareS(a.title, b.title);
				if (rv == 0)
				{
					rv = compareN(a.id, b.id);
				}
			}
			return rv;
		});

		return sorted;
	};

	this.sortByDue = function(assessments, direction)
	{
		var sorted = [].concat(assessments);
		var adjust = (direction == 'A') ? 1 : -1;
		sorted.sort(function(a, b)
		{
			var aVal = ((a.schedule == null) ? null : a.schedule.due);
			var bVal = ((b.schedule == null) ? null : b.schedule.due);
			var rv = adjust * compareN(aVal, bVal);
			if (rv == 0)
			{
				rv = compareS(a.title, b.title);
				if (rv == 0)
				{
					rv = compareN(a.id, b.id);
				}
			}
			return rv;
		});

		return sorted;
	};
	
	this.sortAssessments = function(assessments, direction, mode)
	{
		var sorted = [];

		if (mode == "P")
		{
			sorted = me.sortByPublicationStatus(assessments, direction);
		}
		else if (mode == "S")
		{
			sorted = me.sortBySubmitStatus(assessments, direction);
		}
		else if (mode == "T")
		{
			sorted = me.sortByTitle(assessments, direction);
		}
		else if (mode == "Y")
		{
			sorted = me.sortByType(assessments, direction);
		}
		else if (mode == "O")
		{
			sorted = me.sortByOpen(assessments, direction);
		}
		else if (mode == "D")
		{
			sorted = me.sortByDue(assessments, direction);
		}
		
		return sorted;
	};

	this.sortSubmissionsByFinished = function(submissions, direction)
	{
		var sorted = [].concat(submissions);
		var adjust = (direction == 'A') ? 1 : -1;
		sorted.sort(function(a, b)
		{
			var rv = adjust * compareN(a.finished, b.finished);
			if (rv == 0)
			{
				rv = compareS(a.userNameSort, b.userNameSort);
				if (rv == 0)
				{
					rv = compareN(a.id, b.id);
				}
			}
			return rv;
		});

		return sorted;
	};
	
	this.sortSubmissionsByName = function(submissions, direction)
	{
		var sorted = [].concat(submissions);
		var adjust = (direction == 'A') ? 1 : -1;
		sorted.sort(function(a, b)
		{
			var rv = adjust * compareS(a.userNameSort, b.userNameSort);
			if (rv == 0)
			{
				rv = compareN(a.id, b.id);
			}
			return rv;
		});

		return sorted;
	};

	this.sortSubmissionsByRoster = function(submissions, direction)
	{
		var sorted = [].concat(submissions);
		var adjust = (direction == 'A') ? 1 : -1;
		sorted.sort(function(a, b)
		{
			var rv = adjust * compareS(a.userRoster, b.userRoster);
			if (rv == 0)
			{
				var rv = compareS(a.userNameSort, b.userNameSort);
				if (rv == 0)
				{
					rv = compareN(a.id, b.id);
				}
			}
			return rv;
		});

		return sorted;
	};

	this.sortSubmissionsByFinal = function(assessments, direction)
	{
		var sorted = [].concat(assessments);
		var adjust = (direction == 'A') ? 1 : -1;
		sorted.sort(function(a, b)
		{
			var aVal = ((a.evaluation == null) ? null : a.evaluation.score);
			var bVal = ((b.evaluation == null) ? null : b.evaluation.score);
			var rv = adjust * compareN(aVal, bVal);
			if (rv == 0)
			{
				rv = compareS(a.userNameSort, b.userNameSort);
				if (rv == 0)
				{
					rv = compareN(a.id, b.id);
				}
			}
			return rv;
		});

		return sorted;
	};

	this.sortSubmissionstByEvaluated = function(assessments, direction)
	{
		var sorted = [].concat(assessments);
		var adjust = (direction == 'A') ? 1 : -1;
		sorted.sort(function(a, b)
		{
			var aVal = ((a.evaluation == null) ? null : a.evaluation.evaluated);
			var bVal = ((b.evaluation == null) ? null : b.evaluation.evaluated);
			var rv = adjust * compareB(aVal, bVal);
			if (rv == 0)
			{
				rv = compareS(a.userNameSort, b.userNameSort);
				if (rv == 0)
				{
					rv = compareN(a.id, b.id);
				}
			}
			return rv;
		});

		return sorted;
	};

	this.sortSubmissionsByReleased = function(assessments, direction)
	{
		var sorted = [].concat(assessments);
		var adjust = (direction == 'A') ? 1 : -1;
		sorted.sort(function(a, b)
		{
			var aVal = ((a.evaluation == null) ? null : a.evaluation.released);
			var bVal = ((b.evaluation == null) ? null : b.evaluation.released);
			var rv = adjust * compareB(aVal, bVal);
			if (rv == 0)
			{
				rv = compareS(a.userNameSort, b.userNameSort);
				if (rv == 0)
				{
					rv = compareN(a.id, b.id);
				}
			}
			return rv;
		});

		return sorted;
	};
}

$(function()
{
	try
	{
		assessment_tool = new Assessment();
		assessment_tool.init();
		assessment_tool.start();
	}
	catch (e)
	{
		error(e);
	}
});

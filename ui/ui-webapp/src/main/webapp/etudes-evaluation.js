/**********************************************************************************
 * $URL: https://source.etudes.org/svn/serenity/trunk/ui/ui-webapp/src/main/webapp/etudes-evaluation.js $
 * $Id: etudes-evaluation.js 12184 2015-12-02 03:45:48Z ggolden $
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

function e3_EvaluationEdit(main)
{
	var _me = this;
	
	this._ui = null;

	this._edit = null;
	this._design = null;

	this._inject = function(target)
	{
		_me._ui = clone(main._ui.e3_EvaluationEditTemplate,["e3_EvaluationEdit_autoRelease", "e3_EvaluationEdit_forGrade", "e3_EvaluationEdit_points",
		                                                           "e3_EvaluationEdit_category", "e3_EvaluationEdit_rubric",
		                                                           "e3_EvaluationEdit_anonGrading", "e3_EvaluationEdit_worthPoints", "e3_EvaluationEdit_pointsSet"]);
		target.empty().append(_me._ui.element.children());
	};

	this._adjustPointsUI = function(forPoints)
	{
		if (!forPoints)
		{
			hide([_me._ui.e3_EvaluationEdit_pointsSet, _me._ui.e3_EvaluationEdit_points]);
		}
		else
		{
			if ((_me._design.mutablePoints === undefined) || (_me._design.mutablePoints))
			{
				_me._edit.setupFilteredEdit(_me._ui.e3_EvaluationEdit_points, _me._design, "actualPoints");
				show(_me._ui.e3_EvaluationEdit_points);
				hide(_me._ui.e3_EvaluationEdit_pointsSet);
			}
			else
			{
				if (_me._design.actualPoints !== undefined)
				{
					_me._ui.e3_EvaluationEdit_pointsSet.text(main._i18n.lookup("msg_pointsSetTo", "(set to %0 points)", "html", [_me._design.actualPoints]));
					show(_me._ui.e3_EvaluationEdit_pointsSet);
					hide(_me._ui.e3_EvaluationEdit_points);
				}
				else
				{
					hide([_me._ui.e3_EvaluationEdit_pointsSet, _me._ui.e3_EvaluationEdit_points]);
				}
			}
		}
	};

	this._populate = function()
	{
		_me._edit.setupCheckEdit(_me._ui.e3_EvaluationEdit_autoRelease, _me._design, "autoRelease");
		_me._edit.setupCheckEdit(_me._ui.e3_EvaluationEdit_forGrade, _me._design, "forGrade");
		_me._edit.setupCheckEdit(_me._ui.e3_EvaluationEdit_anonGrading, _me._design, "anonGrading");

		_me._edit.setupCheckEdit(_me._ui.e3_EvaluationEdit_worthPoints, _me._design, "forPoints", function(val)
		{
			_me._adjustPointsUI(val);
			if (!val)
			{
				_me._edit.set(_me._edit, "actualPoints", null);
			}
		});
		_me._adjustPointsUI(_me._design.forPoints);

		var options = [{value:0, title:main._i18n.lookup("msg_noRubric", "No Rubric")}];
		$.each(main._rubrics, function(index, rubric)
		{
			options.push({value:rubric.id, title:rubric.title});
		});
		_me._edit.setupSelectEdit(_me._ui.e3_EvaluationEdit_rubric, options, _me._design, "rubricSelected");
		
		var options = [{value:0, title:main._i18n.lookup("msg_noCategory", "No Category")}];
		$.each(main._categories, function(index, category)
		{
			options.push({value:category.id, title:category.title});
		});	
		_me._edit.setupSelectEdit(_me._ui.e3_EvaluationEdit_category, options, _me._design, "actualCategory");
	};

	// set the edit UI into the element(ui) for this design which is in this edit, with these predefined rubrics (id and title) and categories
	this.set = function(ui, design, edit, rubrics, categories)
	{
		main._rubrics = rubrics || [];
		main._categories = categories || [];
		_me._design = design;
		_me._edit = edit;

		var target = ((ui == null) ? null : ($.type(ui) === "string") ? $("#" + ui) : ui);
		_me._inject(target);

		var path = _me._edit.namePath(_me._design, _me._edit);
		_me._edit.setFilters({"forGrade": _me._edit.noFilter, "autoRelease": _me._edit.noFilter, "rubricSelected": _me._edit.noFilter, "actualCategory": _me._edit.noFilter,
			"actualPoints": _me._edit.numberFilter}, path);

		_me._populate();
	};
};

// full rubric display
function e3_EvaluationRubricView(main)
{
	var me = this;

	this.set = function(into, rubric)
	{
		var elements = findElements(["e3_evaluation_rubricView_criterion_template"]);

		var target = ((into == null) ? null : ($.type(into) === "string") ? $("#" + into) : into);
		target.empty();
		
		$.each(rubric.criteria, function(index, criterion)
		{
			var line = clone(elements.e3_evaluation_rubricView_criterion_template,
					["e3_evaluation_rubricView_criterion_template_body", "e3_evaluation_rubricView_criterion_template_description",
					 "e3_evaluation_rubricView_criterion_template_standard_0", "e3_evaluation_rubricView_criterion_template_standard_0_level", "e3_evaluation_rubricView_criterion_template_standard_0_standard",
					 "e3_evaluation_rubricView_criterion_template_standard_1", "e3_evaluation_rubricView_criterion_template_standard_1_level", "e3_evaluation_rubricView_criterion_template_standard_1_standard",
					 "e3_evaluation_rubricView_criterion_template_standard_2", "e3_evaluation_rubricView_criterion_template_standard_2_level", "e3_evaluation_rubricView_criterion_template_standard_2_standard",
					 "e3_evaluation_rubricView_criterion_template_standard_3", "e3_evaluation_rubricView_criterion_template_standard_3_level", "e3_evaluation_rubricView_criterion_template_standard_3_standard",
					 "e3_evaluation_rubricView_criterion_template_standard_4", "e3_evaluation_rubricView_criterion_template_standard_4_level", "e3_evaluation_rubricView_criterion_template_standard_4_standard",
					 "e3_evaluation_rubricView_criterion_template_standard_5", "e3_evaluation_rubricView_criterion_template_standard_5_level", "e3_evaluation_rubricView_criterion_template_standard_5_standard"]);
			line.e3_evaluation_rubricView_criterion_template_description.text(criterion.description);
			
			$.each(rubric.scale, function(index, level)
			{
				line["e3_evaluation_rubricView_criterion_template_standard_" + level.number + "_level"].text(level.title);
				line["e3_evaluation_rubricView_criterion_template_standard_" + level.number + "_standard"].text(criterion.standards[level.number].description);
			});
			for (var index = 0; index < /*7*/6; index++)
			{
				show(line["e3_evaluation_rubricView_criterion_template_standard_" + index], index < criterion.standards.length);
				line["e3_evaluation_rubricView_criterion_template_standard_" + index].css({backgroundColor: rgColor(index / (criterion.standards.length-1))});
			}

			target.append(line.e3_evaluation_rubricView_criterion_template_body);
		});
	};
}

// condensed rubric display, w/o standards
function e3_EvaluationRubricDisplay(main)
{
	var me = this;
//	this.colors = ["#FF8080", "#FFE34D", "#80C080"];

	this.set = function(into, rubric)
	{
		var elements = findElements(["e3_evaluation_rubricDisplay_template", "e3_evaluation_rubricDisplay_criterion_template"]);
		var ui = clone(elements.e3_evaluation_rubricDisplay_template, ["e3_evaluation_rubricDisplay_template_body",
		                          "e3_evaluation_rubricDisplay_template_scale_0", "e3_evaluation_rubricDisplay_template_scale_1", "e3_evaluation_rubricDisplay_template_scale_2",
		                          "e3_evaluation_rubricDisplay_template_scale_3", "e3_evaluation_rubricDisplay_template_scale_4", "e3_evaluation_rubricDisplay_template_scale_5",
		                          "e3_evaluation_rubricDisplay_template_criteria"]);

		var target = ((into == null) ? null : ($.type(into) === "string") ? $("#" + into) : into);
		target.empty().append(ui.e3_evaluation_rubricDisplay_template_body);
		
		$.each(rubric.scale, function(index, level)
		{
			ui["e3_evaluation_rubricDisplay_template_scale_" + level.number].text(level.title);
		});
		for (var index = 0; index < /*7*/6; index++)
		{
			show(ui["e3_evaluation_rubricDisplay_template_scale_" + index], index < rubric.scale.length);
			ui["e3_evaluation_rubricDisplay_template_scale_" + index].css({backgroundColor: rgColor(index / (rubric.scale.length-1)) /*me.colors[1]*/});
		}
//		ui.e3_evaluation_rubricDisplay_template_scale_0.css({backgroundColor: me.colors[0]});
//		ui["e3_evaluation_rubricDisplay_template_scale_" + (rubric.scale.length-1)].css({backgroundColor: me.colors[2]});

		$.each(rubric.criteria, function(index, criterion)
		{
			var line = clone(elements.e3_evaluation_rubricDisplay_criterion_template, ["e3_evaluation_rubricDisplay_criterion_template_body", "e3_evaluation_rubricDisplay_criterion_template_description"]);
			line.e3_evaluation_rubricDisplay_criterion_template_description.text(criterion.description);
			ui.e3_evaluation_rubricDisplay_template_criteria.append(line.e3_evaluation_rubricDisplay_criterion_template_body);
		});
	};
}

function e3_EvaluationDisplay(main)
{
	var _me = this;
	this._ui = null;
//	this._colors = ["e3_evaluation_rate_red", "e3_evaluation_rate_yellow", "e3_evaluation_rate_green"];

	this._design = null;

	this._inject = function(target)
	{
		_me._ui = clone(main._ui.e3_EvaluationDisplayTemplate, ["e3_EvaluationDisplay_criteria", "e3_EvaluationDisplay_rubricView"]);
		target.empty().append(_me._ui.element.children());
	};

	this._populate = function()
	{
//		if (_me._design.forGrade)
//		{
//			_me._ui.e3_EvaluationDisplay_gradePoints.html(main._i18n.lookup("msg_forGradePoints", "For Grade: Worth <b>%0</b> Points", "html", [_me._design.points]));
//		}
//		else
//		{
//			_me._ui.e3_EvaluationDisplay_gradePoints.html(main._i18n.lookup("msg_notForGrade", "Not For Grade"));
//		}
//
		if (_me._design.rubricSelected != 0)
		{
			main.rubricView.set(_me._ui.e3_EvaluationDisplay_rubricView, main._findRubric(_me._design.rubricSelected));
		}
		show(_me._ui.e3_EvaluationDisplay_criteria, (_me._design.rubricSelected != 0));
	};

	// put an evaluation design into this element (ui) - design might be partial, just a rubric
	this.set = function(ui, design, rubrics)
	{
		main._rubrics = rubrics || [];
		_me._design = design;
		var target = ((ui == null) ? null : ($.type(ui) === "string") ? $("#" + ui) : ui);
		_me._inject(target);
		_me._populate();
	};
};

function e3_EvaluationReview(main)
{
	var _me = this;

	this._ui = null;
	this._elements = null;
	this._colorClasses = ["e3_evaluation_review_red", "e3_evaluation_review_yellow", "e3_evaluation_review_green"];

	this._design = null;
	this._evaluation = null;
//	this._colors = ["#FF8080", "#FFE34D", "#80C080"];

	this._inject = function(target)
	{
		_me._ui = clone(main._ui.e3_EvaluationReviewTemplate, ["e3_EvaluationReview_score", "e3_EvaluationReview_commentUI",
		                                                       "e3_EvaluationReview_comment", "e3_EvaluationReviewTemplate_official", "e3_EvaluationReviewTemplate_peer",
		                                                       "e3_EvaluationReview_rubric", "e3_EvaluationReview_rubricReview"]);
		target.empty().append(_me._ui.element.children());

		_me._elements = findElements(["e3_evaluation_review_criterion_template"]);
	};

	this._populateCriterion = function(criterion)
	{
		var rating = _me._evaluation.ratings[criterion.id];
		var rubric = main._findRubric(_me._design.rubricSelected);

		// clone a template
		var line = clone(_me._elements.e3_evaluation_review_criterion_template,
				["e3_evaluation_review_criterion_template_body", "e3_evaluation_review_criterion_template_description",
				 "e3_evaluation_review_criterion_template_scale", "e3_evaluation_review_criterion_template_standard"]);

		line.e3_evaluation_review_criterion_template_description.text(criterion.description);

		// size & color the scale appropriately
		var width =  "calc(" + (((rating === undefined) ? 0 : rating) + 1) + " * ((100% / 6) - 25px))"
		line.e3_evaluation_review_criterion_template_scale
				.css({width: width, backgroundColor: rgColor(rating / (rubric.scale.length-1))})
				.text(((rating == undefined) ? main._i18n.lookup("msg_notGraded", "Not Graded") : rubric.scale[rating].title));

		line.e3_evaluation_review_criterion_template_standard.text(((rating == undefined) ? "" : criterion.standards[rating].description));

		return line.e3_evaluation_review_criterion_template_body;
	};

	this._populate = function()
	{
		var rubric = main._findRubric(_me._design.rubricSelected);

		if (_me._design.forGrade)
		{
			if ((_me._evaluation == null) || (!_me._evaluation.released))
			{
				_me._ui.e3_EvaluationReview_score.html(main._i18n.lookup("msg_notGraded", "Not Graded"));
			}
			else
			{
				_me._ui.e3_EvaluationReview_score.html(main._i18n.lookup("msg_score", "Score: %0 &nbsp;&nbsp;&nbsp;&nbsp;&nbsp;(%1 of %2)", "html",
						[asPct(_me._evaluation.score, _me._design.points), _me._evaluation.score, _me._design.points]));
			}
		}
		else
		{
			_me._ui.e3_EvaluationReview_score.html(main._i18n.lookup("msg_notForGrade", "Not For Grade"));
		}

		if ((_me._evaluation != null) && (_me._evaluation.released))
		{
			if (_me._evaluation.comment !== undefined)
			{
				_me._ui.e3_EvaluationReview_comment.html(_me._evaluation.comment);

				show(_me._ui.e3_EvaluationReviewTemplate_official, (_me._evaluation.type == EvaluationType.official));
				show(_me._ui.e3_EvaluationReviewTemplate_peer, ((_me._evaluation.type != EvaluationType.official)));
			}
			show(_me._ui.e3_EvaluationReview_commentUI, (_me._evaluation.comment !== undefined));

			if (rubric != null)
			{
				_me._ui.e3_EvaluationReview_rubricReview.empty();
				$.each(rubric.criteria, function(index, criterion)
				{
					var line = _me._populateCriterion(criterion);
					_me._ui.e3_EvaluationReview_rubricReview.append(line);
				});
			}
			show(_me._ui.e3_EvaluationReview_rubric, (rubric != null));
		}
		else
		{
			hide([_me._ui.e3_EvaluationReview_commentUI, _me._ui.e3_EvaluationReview_rubric]);
		}
	};

	// put a design review into this element (ui) using this design and evaluation
	this.set = function(ui, design, evaluation, rubrics)
	{
		main._rubrics = rubrics || [];
		_me._design = design;
		_me._evaluation = evaluation;
		var target = ((ui == null) ? null : ($.type(ui) === "string") ? $("#" + ui) : ui);
		_me._inject(target);
		_me._populate();
	};
};

function e3_EvaluationGrade(main)
{
	var _me = this;
	
	this._ui = null
	this._elements = null;
	
	this._design = null;
	this._evaluation = null;
	this._edit = null;

	this._inject = function(target)
	{
		_me._ui = clone(main._ui.e3_EvaluationGradeTemplate, ["e3_EvaluationGrade_score", "e3_EvaluationGrade_points", "e3_EvaluationGrade_evaluated", "e3_EvaluationGrade_released", "e3_EvaluationGrade_comment",
		                                                      "e3_EvaluationGrade_rubric", "e3_EvaluationGrade_rubricGrading"]);
		target.empty().append(_me._ui.element.children());
		_me._elements = findElements(["e3_evaluation_grade_criterion_template"]);
		
		_me._ui.editor = new e3_EditorCK(_me._ui.e3_EvaluationGrade_comment, {height: 300});
	};

	this._populateRubricGrading = function(rubric)
	{
		_me._ui.e3_EvaluationGrade_rubricGrading.empty();
		var lastEntry = null;
		$.each(rubric.criteria, function(index, criterion)
		{
			var rating = _me._evaluation.ratings[criterion.id];

			// clone a template
			var line = clone(_me._elements.e3_evaluation_grade_criterion_template,
					["e3_evaluation_grade_criterion_template_body", "e3_evaluation_grade_criterion_template_description",
					 "e3_evaluation_grade_criterion_template_scale_0", "e3_evaluation_grade_criterion_template_scale_0_level", "e3_evaluation_grade_criterion_template_scale_0_standard",
					 "e3_evaluation_grade_criterion_template_scale_1", "e3_evaluation_grade_criterion_template_scale_1_level", "e3_evaluation_grade_criterion_template_scale_1_standard",
					 "e3_evaluation_grade_criterion_template_scale_2", "e3_evaluation_grade_criterion_template_scale_2_level", "e3_evaluation_grade_criterion_template_scale_2_standard",
					 "e3_evaluation_grade_criterion_template_scale_3", "e3_evaluation_grade_criterion_template_scale_3_level", "e3_evaluation_grade_criterion_template_scale_3_standard",
					 "e3_evaluation_grade_criterion_template_scale_4", "e3_evaluation_grade_criterion_template_scale_4_level", "e3_evaluation_grade_criterion_template_scale_4_standard",
					 "e3_evaluation_grade_criterion_template_scale_5", "e3_evaluation_grade_criterion_template_scale_5_level", "e3_evaluation_grade_criterion_template_scale_5_standard"]);

			line.e3_evaluation_grade_criterion_template_description.text(criterion.description);
			$.each(rubric.scale, function(index, level)
			{
				line["e3_evaluation_grade_criterion_template_scale_" + level.number].find("a")
						.attr("title", main._i18n.lookup("msg_rubricGrade", "Grade criterion at level '%0'", "html", [level.title]));
				line["e3_evaluation_grade_criterion_template_scale_" + level.number + "_level"].text(level.title);
				line["e3_evaluation_grade_criterion_template_scale_" + level.number + "_standard"].text(criterion.standards[level.number].description);
				onClick(line["e3_evaluation_grade_criterion_template_scale_" + level.number].find("a"), function()
				{
					_me._evaluation.set(_me._evaluation.ratings, criterion.id, level.number);
					_me._populateRubricGrading(rubric);
				});

				onHover(line["e3_evaluation_grade_criterion_template_scale_" + level.number],
					function(a)
					{
						if (level.number != rating)
						{
							a.find("a").stop().animate({color: "#000000"}, Hover.quick, function(){});
						}
					},
					function(a)
					{
						if (level.number != rating)
						{
							a.find("a").stop().animate({color: "#686868"}, Hover.quick, function(){});
						}
					});
			});
			for (var index = 0; index < /*7*/6; index++)
			{
				show(line["e3_evaluation_grade_criterion_template_scale_" + index], index < rubric.scale.length);
				line["e3_evaluation_grade_criterion_template_scale_" + index].css({backgroundColor: rgColor(index / (rubric.scale.length-1))/*, borderColor: rgColor(index / (_me._design.rubric.scale.length-1))*/});
			}

			if (rating != null)
			{
				line["e3_evaluation_grade_criterion_template_scale_" + rating].css({borderColor: "#000000"});
				line["e3_evaluation_grade_criterion_template_scale_" + rating].find("span:first-child").css({fontWeight: 700, color: "#000000"});
			}

			_me._ui.e3_EvaluationGrade_rubricGrading.append(line.e3_evaluation_grade_criterion_template_body);
			
			lastEntry = line.e3_evaluation_grade_criterion_template_body;
		});

		if (lastEntry != null) lastEntry.css({paddingBottom:0});
	};

	this._populate = function(fs)
	{
		var rubric = main._findRubric(_me._design.rubricSelected);

		_me._edit.setupFilteredEdit(_me._ui.e3_EvaluationGrade_score, _me._evaluation, "score");
		_me._ui.e3_EvaluationGrade_points.text(main._i18n.lookup("msg_ofPoints", "(of %0 possible points)", "html", [_me._design.points]));
		_me._edit.setupCheckEdit(_me._ui.e3_EvaluationGrade_evaluated, _me._evaluation, "evaluated");
		_me._edit.setupCheckEdit(_me._ui.e3_EvaluationGrade_released, _me._evaluation, "released");
		_me._edit.setupEditorEdit(_me._ui.editor, _me._evaluation, "comment", null, fs);

		if (rubric != null)
		{	
			_me._populateRubricGrading(rubric);
		}
		show(_me._ui.e3_EvaluationGrade_rubric, (rubric != null));
	};

	// grade: into this ui, for this design, setting this evaluation object which is in this edit
	this.set = function(ui, design, evaluation, edit, rubrics, fs)
	{
		main._rubrics = rubrics || [];
		_me._design = design;
		_me._evaluation = evaluation;
		_me._edit = edit;

		var target = ((ui == null) ? null : ($.type(ui) === "string") ? $("#" + ui) : ui);
		_me._inject(target);
		_me._populate(fs);
	};
};

function e3_EvaluationReviewLink(main)
{
	var _me = this;
//	this._colors = ["e3_evaluation_reviewLink_red", "e3_evaluation_reviewLink_yellow", "e3_evaluation_reviewLink_green"];

	// set a score / review display/link into this element (ui) based on the design and evaluation, using onReview if review is clicked
	this.set = function(ui, design, evaluation, onReview, rubrics)
	{
		main._rubrics = rubrics || [];
		
		var rubric = main._findRubric(design.rubricSelected);

		var target = ((ui == null) ? null : ($.type(ui) === "string") ? $("#" + ui) : ui);
		var ui = clone(main._ui.e3_EvaluationReviewLinkTemplate, ["e3_EvaluationReviewLinkBars", "e3_EvaluationReviewLink_pct", "e3_EvaluationReviewLink_score", "e3_EvaluationReviewLink_notGraded",
			                                                       "e3_EvaluationReviewLinkReviewUI", "e3_EvaluationReviewLinkReviewDot", "e3_EvaluationReviewLinkReview", "e3_EvaluationReviewLinkNoReview"]);
		target.empty().append(ui.element.children());

		var review = (onReview != null) && (evaluation.mayReview);
		if (review)
		{
			onClick(ui.e3_EvaluationReviewLinkReview, onReview);
			setupHoverControls([ui.e3_EvaluationReviewLinkReview]);

			var msg = ((evaluation == null) || (evaluation.reviewedOn == null)) ? main._i18n.lookup("msg_notReviewed", "not reviewed") : main._i18n.lookup("msg_lastReviewed", "last reviewed: %0", "html", [main._timestamp.display(evaluation.reviewedOn)]);
			ui.e3_EvaluationReviewLinkReview.attr("title", msg);
			if ((evaluation != null) && evaluation.released && evaluation.needsReview)
			{
				ui.e3_EvaluationReviewLinkReviewDot.html(dotSmall(Dots.blue, true));
				ui.e3_EvaluationReviewLinkReviewDot.attr("title", main._i18n.lookup("msg_needsReview", "Needs Review!"));
			}
		}
		else
		{
			// TODO: unreleased should not be here ... this may need work... (consider mneme's non-submit no review unless comment)
			var msg = ((design.reviewWhen == 2) ? "msg_reviewLater" : "msg_noReview");
			ui.e3_EvaluationReviewLinkNoReview.text(main._i18n.lookup(msg));
			if (design.reviewWhen != 3) ui.e3_EvaluationReviewLinkNoReview.attr("title", main._i18n.lookup("msg_noReview_" + design.reviewWhen, null, "html", [main._timestamp.display(design.reviewAfter)]));
		}
		show(ui.e3_EvaluationReviewLinkReviewUI, review);
		show(ui.e3_EvaluationReviewLinkNoReview, !review);

		if ((evaluation != null) && evaluation.released && (evaluation.score != null))
		{
			ui.e3_EvaluationReviewLink_pct.text(asPct(evaluation.score, design.points));
			ui.e3_EvaluationReviewLink_score.html(main._i18n.lookup("msg_scoreOf", "%0 <span style='font-size:8px;font-weight:bold;'>OF</span> %1 pts", "html", [evaluation.score, design.points]));
			hide(ui.e3_EvaluationReviewLink_notGraded);
		}
		else
		{
			hide([ui.e3_EvaluationReviewLink_pct, ui.e3_EvaluationReviewLink_score]);
		}
		
		if ((evaluation != null) && evaluation.released && (evaluation.ratings != null) && (rubric != null))
		{
			ui.e3_EvaluationReviewLinkBars.empty();
			$.each(rubric.criteria, function(index, criterion)
			{
				var div = $("<div />");
				div.addClass("e3_evaluation_reviewLink"); // main._classForRating({number:evaluation.ratings[criterion.id]}, design.rubric.scale, _me._colors));
				div.css({backgroundColor: rgColor(((evaluation.ratings[criterion.id] == null) ? 0 : (evaluation.ratings[criterion.id] / (rubric.scale.length-1))), 42), width: (10 * (((evaluation.ratings[criterion.id] === undefined) ? -0.75 : evaluation.ratings[criterion.id])+1))});
				ui.e3_EvaluationReviewLinkBars.append(div);
			});
		}
	};
};

function e3_Evaluation(cdp, dialogs, timestamp)
{
	var _me = this;

	this._ui = null;

	this._i18n = new e3_i18n(etudes_evaluation_i10n, "en-us");
	this._i18n.init();

	this._cdp = cdp;
	this._dialogs = dialogs;
	this._timestamp = timestamp;

	this.edit = new e3_EvaluationEdit(this);
	this.grade = new e3_EvaluationGrade(this);
	this.display = new e3_EvaluationDisplay(this);
	this.reviewLink = new e3_EvaluationReviewLink(this);
	this.review = new e3_EvaluationReview(this);
	this.rubricDisplay = new e3_EvaluationRubricDisplay(this);
	this.rubricView = new e3_EvaluationRubricView(this);

	this._rubrics = [];
	this._categories = [];

	this._findRubric = function(id)
	{
		for (var i = 0; i < _me._rubrics.length; i++)
		{
			if (_me._rubrics[i].id == id) return _me._rubrics[i];
		}
	};

	this._findCategory = function(id)
	{
		for (var i = 0; i < _me._categories.length; i++)
		{
			if (_me._categories[i].id == id) return _me._categories[i];
		}
	};

	this._init = function()
	{
		_me._ui = findElements(["e3_EvaluationEditTemplate", "e3_EvaluationDisplayTemplate",
		                        "e3_EvaluationReviewLinkTemplate", "e3_EvaluationGradeTemplate", "e3_EvaluationReviewTemplate"]);
	};

	this._maxScale = function(design)
	{
		return design.rubric.scale.length-1;
	};

	try
	{
		this._init();
	}
	catch (e)
	{
		error(e);
	}
};

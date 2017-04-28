/**********************************************************************************
 * $URL: https://source.etudes.org/svn/serenity/trunk/ui/ui-webapp/src/main/webapp/etudes-editor-ck.js $
 * $Id: etudes-editor-ck.js 11887 2015-10-21 02:49:14Z ggolden $
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

// Object used to build html content editors based on CKEditor
function e3_EditorCK(targetId, options)
{
	var _me = this;

	this._target = ($.type(targetId) === "string") ? $("#" + targetId) : targetId;
	
	this._tools = null;
	this._editor = null;
	this._editorApi = null;	
	this._fs = null;

	this._enabled = false;
	this._onChange = null;

	this._i18n = new e3_i18n(etudes_editor_i10n, "en-us");
	this._i18n.init();

	this._ckconfig =
	{
		skin: 'office2003',
		showWordCount: true,
		showCharCount: false,
		countHTML: false,
		audiorecorder : {maxSeconds: 120, attemptAllowed: Number.MAX_VALUE, attemptsRemaining: Number.MAX_VALUE},
		filebrowserBrowseUrl: function(params)
		{
	   		portal_tool.picker.pick(function(value)
			{
    			CKEDITOR.tools.callFunction(params.CKEditorFuncNum, value.url);
			}, _me._fs, "Files");
//    		var filer = new e3_FilerCK();
//    		filer.disable();
//    		filer.enable(function()
//    		{
//    			var selectedMyfile = filer.get();
//    			CKEDITOR.tools.callFunction(params.CKEditorFuncNum, selectedMyfile.url);
//    			filer.disable();
//    		});
		},

    	filebrowserImageBrowseUrl: function(params)
		{
    		portal_tool.picker.pick(function(value)
			{
    			CKEDITOR.tools.callFunction(params.CKEditorFuncNum, value.url);
			}, _me._fs, "Images");
//    		var filer = new e3_FilerCK();
//    		filer.disable();
//    		filer.enable(function()
//    		{
//    			var selectedMyfile = filer.get();
//    			CKEDITOR.tools.callFunction(params.CKEditorFuncNum, selectedMyfile.url);
//    			filer.disable();
//    		});
		},

		pasteFromWordRemoveFontStyles : false,
		pasteFromWordRemoveStyles : false,
		disableNativeSpellChecker: false,
		browserContextMenuOnCtrl: true,
		toolbar_Etudes:
		[
			[ 'Source','-','DocProps','Print','-'] ,
			[ 'Cut','Copy','Paste','PasteText','PasteFromWord','-','Undo','Redo' ] ,
			[ 'Find','Replace','-','SelectAll','-','SpellChecker', 'Scayt' ] ,
			[ 'Bold','Italic','Underline','Strike','Subscript','Superscript','-','RemoveFormat' ] ,
			[ 'NumberedList','BulletedList','-','Outdent','Indent','-','Blockquote',
			'-','JustifyLeft','JustifyCenter','JustifyRight','JustifyBlock','-','BidiLtr','BidiRtl' ] ,
			[ 'Link','Unlink','Anchor'] ,
			[ 'Image','MediaEmbed','AudioRecorder','VTLTIConnect','Flash','Table','HorizontalRule','Smiley','SpecialChar','PageBreak'] ,
			[ 'Maximize', 'ShowBlocks'] , // 'Preview', 
			[ 'TextColor','BGColor' ] ,
			[ 'Styles','Format','Font','FontSize','ckeditor_wiris_formulaEditor']
		],
		toolbar: 'Etudes',
		removePlugins: 'elementspath',
		resize_dir: 'vertical',
		resize_enabled: false,
		toolbarCanCollapse : false,
		extraPlugins: 'MediaEmbed,audiorecorder,movieplayer,wordcount,onchange,ckeditor_wiris',
		scayt_srcUrl: "https://spell.etudes.org/spellcheck/lf/scayt/scayt.js",
		wsc_customLoaderScript: "https://spell.etudes.org/spellcheck/lf/22/js/wsc_fck2plugin.js",
		smiley_path: '/docs/smilies/',
		smiley_images:
		[
		 	'Straight-Face.png','Sun.png','Sweating.png','Thinking.png','Tongue.png',
			'Vomit.png','Wave.png','Whew.png','Win.png','Winking.png','Yawn.png','Yawn2.png',
			'Zombie.png','Angry.png','Balloon.png','Big-Grin.png','Bomb.png','Broken-Heart.png',
			'Cake.png','Cat.png','Clock.png','Clown.png','Cold.png','Confused.png','Cool.png',
			'Crying.png','Crying2.png','Dead.png','Devil.png','Dizzy.png','Dog.png',
			'Don\'t-tell-Anyone.png','Drinks.png','Drooling.png','Flower.png','Ghost.png','Gift.png',
			'Girl.png','Goodbye.png','Heart.png','Hug.png','Kiss.png','Laughing.png','Ligthbulb.png',
			'Loser.png','Love.png','Mail.png','Music.png','Nerd.png','Night.png','Ninja.png',
			'Not-Talking.png','on-the-Phone.png','Party.png','Pig.png','Poo.png','Rainbow.png',
			'Rainning.png','Sacred.png','Sad.png','Scared.png','Sick.png','Sick2.png','Silly.png',
			'Sleeping.png','Sleeping2.png','Sleepy.png','Sleepy2.png','smile.png','Smoking.png','Smug.png','Stars.png'
		],
		smiley_descriptions:
		[
			'Straight Face','Sun','Sweating','Thinking','Tongue',
			'Vomit','Wave','Whew','Win','Winking','Yawn','Yawn2',
			'Zombie','Angry','Balloon','Big Grin','Bomb','Broken Heart',
			'Cake','Cat','Clock','Clown','Cold','Confused','Cool',
			'Crying','Crying2','Dead','Devil','Dizzy','Dog',
			'Don\'t-tell-Anyone','Drinks','Drooling','Flower','Ghost','Gift',
			'Girl','Goodbye','Heart','Hug','Kiss','Laughing','Lightbulb',
			'Loser','Love','Mail','Music','Nerd','Night','Ninja',
			'Not Talking','On The Phone','Party','Pig','Poo','Rainbow',
			'Raining','Sacred','Sad','Scared','Sick','Sick2','Silly',
			'Sleeping','Sleeping2','Sleepy','Sleepy2','smile','Smoking','Smug','Stars'
		],
		smiley_columns: 9,
		protectedSource: [/<link[\s\S]*?\/>/g]
	};

	this._init = function()
	{
	};

	this._changed = function()
	{
		portal_tool.userActivity();

		if (_me._onChange != null)
		{
			try
			{
				_me._onChange();
			}
			catch (e)
			{
				error(e);
			}
		}
	};

	this.enable = function(onChange, withFocus, fs)
	{
		_me._onChange = onChange;
		if (_me._enabled) return;
		_me._enabled = true;

		if ((options !== undefined) && (options.height !== undefined))
		{
			_me._ckconfig.height = options.height;
		}
		else
		{
			_me._ckconfig.height = 600;
		}

		_me._fs = fs;

		_me._editorApi = CKEDITOR.appendTo(_me._target[0], _me._ckconfig, _me._valueToSet);		
		_me._editorApi.on("change", function(e)
		{
			_me._changed();
		});

		// if ((withFocus === undefined) || withFocus) _me._editor.focus(); // TODO:
	};

	this.disable = function()
	{
		if (!_me._enabled) return;
		if (_me._editorApi != null)
		{
			_me._editorApi.destroy();
			_me._editorApi = null;
		}

		_me._target.empty();
		_me._enabled = false;
	};

	this.isEnabled = function()
	{
		return _me._enabled;
	};

	this.set = function(value)
	{
		_me._valueToSet = value;
	};

	this.get = function()
	{
		var rv = _me._editorApi.getData();
		rv = trim(rv);
		return rv;
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

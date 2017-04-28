/**********************************************************************************
 * $URL: https://source.etudes.org/svn/serenity/trunk/roster/roster-webapp/src/main/webapp/siteroster.js $
 * $Id: siteroster.js 12547 2016-01-13 21:34:50Z ggolden $
 ***********************************************************************************
 *
 * Copyright (c) 2014, 2015, 2016 Etudes, Inc.
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

var siteroster_tool = null;

function SiteRoster()
{
	var me = this;

	this.i18n = new e3_i18n(siteroster_i10n, "en-us");
	this.portal = null;

	this.rosterMode = null;
	this.groupMode = null;
	this.groupEditMode = null;

	this.ui = null;
	this.onExit = null;

	this.site = null;
	this.members = null;
	this.groups = null;
	this.groupsSorted = null;

	this.init = function()
	{
		me.i18n.localize();

		me.ui = findElements(["siteroster_header", "siteroster_modebar", "siteroster_headerbar", "siteroster_headerbar2", "siteroster_itemnav",
		                      "siteroster_bar_roster", "siteroster_bar2_roster", "siteroster_header_roster", "siteroster_roster",
		                      "siteroster_bar_group", "siteroster_header_group", "siteroster_group",
		                      "siteroster_bar_groupEdit", "siteroster_groupEdit",
		                      "siteroster_title_template", "siteroster_selectFirst"]);

		me.portal = portal_tool.features({onExit: me.checkExit, pin:[{ui:me.ui.siteroster_header}]});
		me.site = me.portal.site; // TODO: ???

		me.rosterMode = new SiteRoster_roster(this);
		me.rosterMode.init();
		
		me.groupMode = new SiteRoster_group(this);
		me.groupMode.init();

		me.groupEditMode = new SiteRoster_groupEdit(this);
		me.groupEditMode.init();

		me.ui.modebar = new e3_Modebar(me.ui.siteroster_modebar);
		me.modes =
		[
			{name:me.i18n.lookup("mode_roster", "Roster"), func: me.startRoster},
			{name:me.i18n.lookup("mode_group", "Groups"), func: me.startGroup}
		];
		me.ui.modebar.set(me.modes, 0);

//		// portal: title, reset and configure - returns cdpError handler
//		if (portal_tool != null)
//		{
//			me.portal = portal_tool.features(me.i18n.lookup("titlebar", "Roster"), function(){me.reset();}, null);
//			me.site = me.portal.site;
//			me.errorHandler = me.portal.errorHandler;
//			me.portalNavigate = me.portal.navigate;
//			me.portalToolReturn = me.portal.toolReturn;
//
//			if (me.portalToolReturn != null)
//			{
//				me.sites = me.portal.sites;
//			}
//		}
//		
//		if (me.portalToolReturn != null)
//		{
////			onClick("siteroster_nav_sitelink", function(){me.portalNavigate(me.site, null, false);});
//			onClick("siteroster_nav_setuplink", function(){me.portalNavigate(me.site, 113, null);});
//			$("#siteroster_nav_siteTitle").text(me.site.name);
//
//			me.setPagingAndNav();
//
//			show(["siteroster_itemNav", "siteroster_nav_setuplink_ui", "siteroster_nav_siteTitle"]);
//		}
//		else
//		{
//			hide(["siteroster_itemNav", "siteroster_nav_setuplink_ui" ,"siteroster_nav_siteTitle"]);
//		}
	};

//	this.sitePosition = function()
//	{
//		var rv = {};
//		
//		var i = 1;
//		var found = null;
//		$.each(me.sites || [], function(index, site)
//		{
//			if (site.id == main.site.id) found = i;
//			i++;
//		});
//
//		rv.item = found;
//		rv.total = me.sites.length;
//		rv.prev = found > 1 ? me.sites[found-2] : null;
//		rv.next = found < me.sites.length ? me.sites[found] : null;
//		
//		return rv;
//	};
//
//	this.setPagingAndNav = function()
//	{
//		$("#siteroster_nav_siteTitle").text(me.site.name);
//
//		me.itemNav.inject("siteroster_itemNav", {pos:me.sitePosition(), returnFunction:function(){me.portalNavigate(me.portalToolReturn.site, me.portalToolReturn.toolId, false);}, navigateFunction:me.changeSite});
//		$("#setup_nav_sitelink").text(me.site.name);
//	};
//
//	this.changeSite = function(site)
//	{
//		if (site == null) return;
//
//		me.site = site;
//		me.setPagingAndNav();
//
//		me.resetFunction();
//	};

	this.start = function()
	{
		me.startRoster();
	};

	this.startRoster = function()
	{
		if ((me.onExit != null) && (!me.onExit(function(){me.startRoster();}))) return;
		me.mode([me.ui.siteroster_headerbar, me.ui.siteroster_bar_roster, me.ui.siteroster_headerbar2, me.ui.siteroster_bar2_roster, me.ui.siteroster_header_roster, me.ui.siteroster_roster]);
		me.ui.modebar.showSelected(0);
		me.rosterMode.start();
	};
	
	this.startGroup = function()
	{
		if ((me.onExit != null) && (!me.onExit(function(){me.startGroups();}))) return;
		me.mode([me.ui.siteroster_headerbar, me.ui.siteroster_bar_group, me.ui.siteroster_header_group, me.ui.siteroster_group]);
		me.ui.modebar.showSelected(1);
		me.groupMode.start();
	};

	this.startGroupEdit = function(groupOrId)
	{
		if ((me.onExit != null) && (!me.onExit(function(){me.startGroupEdit(groupOrId);}))) return;
		me.mode([me.ui.siteroster_headerbar, me.ui.siteroster_itemnav, me.ui.siteroster_bar_groupEdit, me.ui.siteroster_groupEdit]);
		me.ui.modebar.showSelected(1);
		me.groupEditMode.start(groupOrId);
	};

	this.mode = function(modeUi)
	{
		hide([me.ui.siteroster_headerbar, me.ui.siteroster_headerbar2, me.ui.siteroster_itemnav,
			  me.ui.siteroster_bar_roster, me.ui.siteroster_bar2_roster, me.ui.siteroster_header_roster, me.ui.siteroster_roster,
			  me.ui.siteroster_bar_group, me.ui.siteroster_header_group, me.ui.siteroster_group,
			  me.ui.siteroster_bar_groupEdit, me.ui.siteroster_groupEdit
			  ]);
		me.onExit = null;
		me.portal.resetScrolling();
		show(modeUi);
	};

	this.checkExit = function(deferred)
	{
		if (me.onExit == null) return true;
		return me.onExit(deferred);
	};

	this.roleName = function(role)
	{
		switch (role)
		{
			case Role.guest: return me.i18n.lookup("Guest", "Guest");
			case Role.observer: return me.i18n.lookup("Observer", "Observer");
			case Role.student: return me.i18n.lookup("Student", "Student");
			case Role.ta: return me.i18n.lookup("TA", "TA");
			case Role.instructor: return me.i18n.lookup("Instructor", "Instructor");
			case Role.admin: return me.i18n.lookup("Administrator", "Administrator");
			default: return me.i18n.lookup("None", "None");
		}
	};

	this.memberStatus = function(member)
	{
		if (member.blocked)
		{
			if ((member.official) && (!member.master))
			{
				if (member.active)
				{
					return me.i18n.lookup("blockedEnrolled", "Blocked (E)");
				}
				else
				{
					return me.i18n.lookup("blockedDropped", "Blocked (D)");
				}
			}
			else
			{
				return me.i18n.lookup("Blocked", "Blocked");
			}
		}
		else if ((member.official) && (!member.master))
		{
			if (member.active)
			{
				return me.i18n.lookup("Enrolled", "Enrolled");
			}
			else
			{
				return me.i18n.lookup("Dropped", "Dropped");
			}
		}
		else if (member.adhoc)
		{			
			return me.i18n.lookup("Added", "Added");
		}
		else
		{
			return me.i18n.lookup("Active", "Active");
		}
	};

	this.memberStatusRanking = function(member)
	{
		if (member.blocked) return 3;
		if (!member.active) return 4;
		if (member.adhoc) return 2;
		return 1;
	};

	this.rosterSummary = function(members)
	{
		var rv = {enrolled: 0, dropped: 0, blocked: 0, instructors: 0, tas: 0, observers: 0, students: 0, guests: 0};

		$.each(members, function(index, member)
		{
			if (member.blocked == "1")
			{
				rv.blocked++;
			}
			else if (member.official == "1")
			{
				if (member.active == "1")
				{
					rv.enrolled++;
				}
				else
				{
					rv.dropped++;
				}
			}

			switch (member.role)
			{
				case Role.guest:
				{
					rv.guests++;
					break;
				}
				case Role.observer:
				{
					rv.observers++;
					break;
				}
				case Role.student:
				{
					rv.students++;
					break;
				}
				case Role.ta:
				{
					rv.tas++;
					break;
				}
				case Role.instructor:
				{
					rv.instructors++;
					break;
				}
			}
		});
		
		return rv;
	};
}

function SiteRoster_roster(main)
{
	var me = this;
	
	this.ui = null;

	this.membersSorted = null;

	this.sortDirection = "A";
	this.sortMode = "N";

	this.init = function()
	{
		me.ui = findElements(["siteroster_roster_enrolled", "siteroster_roster_dropped", "siteroster_roster_blocked",
		                      "siteroster_roster_instructors", "siteroster_roster_tas", "siteroster_roster_students", "siteroster_roster_observers", "siteroster_roster_guests",
		                      "siteroster_roster_add", "siteroster_roster_remove", "siteroster_roster_block", "siteroster_roster_unblock", "siteroster_roster_role",
		                      "siteroster_roster_table",
		                      "siteroster_roster_actions",
		                      "siteroster_roster_confirmAdd", "siteroster_roster_confirmAdd_identifiers",
		                      "siteroster_roster_confirmRemove", "siteroster_roster_confirmBlock", "siteroster_roster_confirmUnblock", "siteroster_roster_confirmRole",
		                      "siteroster_accessDialog", "siteroster_accessDialog_name", "siteroster_accessDialog_days", "siteroster_accessDialog_add", "siteroster_accessDialog_mult" /* siteroster_accessDialog_limit */
		                      ]);
		me.ui.table = new e3_Table(me.ui.siteroster_roster_table);
		me.ui.table.setupSelection("siteroster_roster_select", me.updateActions);
		me.ui.table.selectAllHeader(1, main.ui.siteroster_header_roster);

		me.ui.sort = new e3_SortAction();
		me.ui.sort.inject(me.ui.siteroster_roster_actions,
				{onSort: me.onSort, options:[{value:"N", title:main.i18n.lookup("sort_name", "NAME")},{value:"S", title:main.i18n.lookup("sort_section", "SECTION")},
				                             {value:"T", title:main.i18n.lookup("sort_status", "STATUS")},{value:"R", title:main.i18n.lookup("sort_role", "ROLE")}]});
		me.ui.sort.directional(true);

		onClick(me.ui.siteroster_roster_add, me.add);
		onClick(me.ui.siteroster_roster_remove, me.remove);
		onClick(me.ui.siteroster_roster_block, me.block);
		onClick(me.ui.siteroster_roster_unblock, me.unblock);
		onClick(me.ui.siteroster_roster_role, me.role);
		
		setupHoverControls([me.ui.siteroster_roster_add, me.ui.siteroster_roster_remove, me.ui.siteroster_roster_block, me.ui.siteroster_roster_unblock, me.ui.siteroster_roster_role]);
	};

	this.start = function()
	{
		// TODO: site might not be current site ...???
//		me.site = main.portal.site;

		me.load();
//		if (main.siteRoster == null)
//		{
//			me.load();
//		}
//		else
//		{
//			me.populateRoster();
//			me.populateSummary();	
//		}
	};

	this.add = function()
	{
		var params = main.portal.cdp.params();
		params.url.site = main.site.id;

		me.ui.siteroster_roster_confirmAdd_identifiers.val("");
		$("input:radio[name=siteroster_roster_confirmAdd_role][value=1]").prop('checked', true);
		main.portal.dialogs.openConfirm(me.ui.siteroster_roster_confirmAdd,  main.i18n.lookup("Add", "Add"), function()
		{
			params.post.role = $("input:radio[name=siteroster_roster_confirmAdd_role]:checked").val();
			params.post.users = $.trim(me.ui.siteroster_roster_confirmAdd_identifiers.val());
			main.portal.cdp.request("roster_addMembers roster_aggregateRoster", params, function(data)
			{
				me.populateResults(data.results);
				me.afterLoad(data);
			});

			return true;
		});
	};

	this.remove = function()
	{
		var params = main.portal.cdp.params();
		params.url.site = main.site.id;
		params.post.users = me.ui.table.selected();

		if (params.post.users.length == 0)
		{
			main.portal.dialogs.openAlert(main.ui.siteroster_selectFirst);
			return;
		}

		main.portal.dialogs.openConfirm(me.ui.siteroster_roster_confirmRemove, main.i18n.lookup("Remove", "Remove"), function()
		{
			main.portal.cdp.request("roster_removeMembers roster_aggregateRoster", params, function(data)
			{
				me.populateResults(data.results);
				me.afterLoad(data);
			});

			return true;
		});
	};
	
	this.block = function()
	{
		var params = main.portal.cdp.params();
		params.url.site = main.site.id;
		params.post.users = me.ui.table.selected();

		if (params.post.users.length == 0)
		{
			main.portal.dialogs.openAlert(main.ui.siteroster_selectFirst);
			return;
		}

		main.portal.dialogs.openConfirm(me.ui.siteroster_roster_confirmBlock,  main.i18n.lookup("Block", "Block"), function()
		{
			main.portal.cdp.request("roster_blockMembers roster_aggregateRoster", params, function(data)
			{
				me.populateResults(data.results);
				me.afterLoad(data);
			});

			return true;
		});
	};
	
	this.unblock = function()
	{
		var params = main.portal.cdp.params();
		params.url.site = main.site.id;
		params.post.users = me.ui.table.selected();

		if (params.post.users.length == 0)
		{
			main.portal.dialogs.openAlert(main.ui.siteroster_selectFirst);
			return;
		}

		main.portal.dialogs.openConfirm(me.ui.siteroster_roster_confirmUnblock,  main.i18n.lookup("Unblock", "Unblock"), function()
		{
			main.portal.cdp.request("roster_unblockMembers roster_aggregateRoster", params, function(data)
			{
				me.populateResults(data.results);
				me.afterLoad(data);
			});

			return true;
		});
	};
	
	this.role = function()
	{
		var params = main.portal.cdp.params();
		params.url.site = main.site.id;
		params.post.users = me.ui.table.selected();

		if (params.post.users.length == 0)
		{
			main.portal.dialogs.openAlert(main.ui.siteroster_selectFirst);
			return;
		}

		$("input:radio[name=siteroster_roster_confirmRole_role][value=1]").prop('checked', true);
		main.portal.dialogs.openConfirm(me.ui.siteroster_roster_confirmRole, main.i18n.lookup("Assign", "Assign"), function()
		{
			params.post.role = $("input:radio[name=siteroster_roster_confirmRole_role]:checked").val();
			main.portal.cdp.request("roster_roleMembers roster_aggregateRoster", params, function(data)
			{
				me.populateResults(data.results);
				me.afterLoad(data);
			});

			return true;
		});
	};

	this.specialAccess = function(member)
	{
		me.ui.siteroster_accessDialog_name.html(main.i18n.lookup("msg_accessFor", "Special access for <span style='font-weight:bold; color:black;'>%0</span> for all items.", "html", [member.nameSort])); // TODO: nameDisplay
		var edit = new e3_Edit({access: member.access || {extendDue: 2, extendTimeOption: "M", extendTimeMultiplier: "1.5"}}, [], function(changed){});
		edit.setupFilteredEdit(me.ui.siteroster_accessDialog_days, edit.access, "extendDue");
		edit.setupRadioEdit("siteroster_accessDialog_limit", edit.access, "extendTimeOption", function(val)
		{
			// "A" for add, "M" for mult, "U" for unlimited
			show(me.ui.siteroster_accessDialog_add, (val == "A"));
			show(me.ui.siteroster_accessDialog_mult, (val == "M"));
//			if (val != "true")
//			{
//				me.ui.asmt_option_timeLimit.val("");
//				me.edit.set(me.edit.options, "timeLimit", null);
//			}
		});
		edit.setupFilteredEdit(me.ui.siteroster_accessDialog_add, edit.access, "extendTimeValue");
		edit.setupFilteredEdit(me.ui.siteroster_accessDialog_mult, edit.access, "extendTimeMultiplier");
		show(me.ui.siteroster_accessDialog_add, (edit.access.extendTimeOption == "A"));
		show(me.ui.siteroster_accessDialog_mult, (edit.access.extendTimeOption == "M"));

		main.portal.dialogs.openDialogButtons(me.ui.siteroster_accessDialog,
		[
			{
				text: main.i18n.lookup("action_save", "SAVE"),
				click: function()
				{
					var params = main.portal.cdp.params();
					params.url.site = main.portal.site.id;
					params.url.user = member.userId;
					edit.params("", params);
					console.log(params);
					main.portal.cdp.request("roster_accessSave roster_aggregateRoster", params, function(data)
					{
						me.afterLoad(data);
					});
					return true;
				}
			},
			{
				text: main.i18n.lookup("action_delete", "DELETE"),
				click: function()
				{
					var params = main.portal.cdp.params();
					params.url.site = main.portal.site.id;
					params.url.user = member.userId;
					console.log(params);
					main.portal.cdp.request("roster_accessDelete roster_aggregateRoster", params, function(data)
					{
						me.afterLoad(data);
					});
					return true;
				}
			}
		]);
		
//		main.portal.dialogs.openDialog(me.ui.siteroster_accessDialog, main.i18n.lookup("action_save", "SAVE"), function() // TODO: a delete button
//		{
//			var params = main.portal.cdp.params();
//			params.url.site = main.portal.site.id;
//			params.url.user = member.userId;
//			edit.params("", params);
//			console.log(params);
//			main.portal.cdp.request("roster_accessSave roster_aggregateRoster", params, function(data)
//			{
//				me.afterLoad(data);
//			});
//			return true;
//		});
	};

	this.load = function()
	{
		var params = main.portal.cdp.params();
		params.url.site = main.site.id;
		main.portal.cdp.request("roster_aggregateRoster", params, function(data)
		{
			me.afterLoad(data);
		});
	};
	
	this.afterLoad = function(data)
	{
		main.members = me.sortMembers(data.members || [], "A", "N");		// we want the "unsorted" list sorted by name
		me.membersSorted = me.sortMembers(main.members, me.sortDirection, me.sortMode);

		me.populateRoster();
		me.populateSummary();	
	};

	this.updateActions = function()
	{
		me.ui.table.updateActions([], [me.ui.siteroster_roster_remove, me.ui.siteroster_roster_block, me.ui.siteroster_roster_unblock, me.ui.siteroster_roster_role]);
	};

	this.populateRoster = function()
	{
		me.ui.table.clear();
		$.each(me.membersSorted, function(index, member)
		{
			var row = me.ui.table.row();
//			if (member.blocked == "1")
//			{
//				row.css("background-color","#FFBAD2");
//			}
//			else if (member.adhoc == "1")
//			{
//				row.css("background-color","#C8CFB4");		
//			}

			me.ui.table.selectBox(member.userId);
			
			if (member.avatar != null)
			{
				var td = me.ui.table.html("<a href='" + member.avatar + "' target='_blank' class='e3_simple'><img src='" + member.avatar + "' border='0' alt='avatar' style='max-width:48px; max-height:48px;'/></a>",
						null, {width: 64, textAlign: "center"});
				if (member.blocked || (!member.active)) td.css({opacity: 0.4});
			}
			else
			{
				me.ui.table.text("", null, {width: 64});
			}

			var cell = clone(main.ui.siteroster_title_template, ["siteroster_title_template_body", "siteroster_title_template_title", "siteroster_title_template_msg"]);
			me.ui.table.element(cell.siteroster_title_template_body, null, {width: "calc(100vw - 100px - 576px)", minWidth: "calc(1200px - 100px - 576px"});

			cell.siteroster_title_template_title.text(member.nameSort);
			
			if (member.blocked || (!member.active)) cell.siteroster_title_template_title.css({color: "#A8A8A8"});
	
			cell.siteroster_title_template_msg.text(member.iid);

			if (member.adhoc || member.master)
			{
				me.ui.table.text("");
			}
			else
			{
				var td = me.ui.table.text(member.rosterName, null, {width: 120});
				if (member.blocked || (!member.active)) td.css({color: "#A8A8A8"});
			}

			var td = me.ui.table.text(main.memberStatus(member), "e3_text special light", {fontSize:11, width:120, textTransform: "uppercase"});
//			if (!member.active)
//			{
//				td.css("color","#F00");
//			}
			if (member.blocked || (!member.active)) td.css({color: "#A8A8A8"});

			td = me.ui.table.text(main.roleName(member.role), "e3_text special light", {fontSize:11, width:120, textTransform: "uppercase"});
			if (member.blocked || (!member.active)) td.css({color: "#A8A8A8"});

			if (!member.blocked && member.active && (member.role == Role.student))
			{
				me.ui.table.contextMenu(
				[
					{title: main.i18n.lookup("cm_access", "Special Access"), action:function(){me.specialAccess(member);}}
		        ]);
			}
			else
			{
				me.ui.table.text("", null, {width: 24});
			}
		});

//		me.ui.table.sort({0:{sorter:false},1:{sorter:"text"},2:{sorter:"text"},3:{sorter:"text"},4:{sorter:"text"},5:{sorter:false}},[[1,0]]);
		me.ui.table.done();
	};

	this.populateSummary = function()
	{
		var summary = main.rosterSummary(main.members);

		me.ui.siteroster_roster_enrolled.text(summary.enrolled);
		me.ui.siteroster_roster_dropped.text(summary.dropped);
		me.ui.siteroster_roster_blocked.text(summary.blocked);
		me.ui.siteroster_roster_instructors.text(summary.instructors);
		me.ui.siteroster_roster_tas.text(summary.tas);
		me.ui.siteroster_roster_students.text(summary.students);
		me.ui.siteroster_roster_observers.text(summary.observers);
		me.ui.siteroster_roster_guests.text(summary.guests);
	};
	
	// TODO: how should this work?
	this.populateResults = function(results)
	{
		var area = $("#siteroster_roster_results_details");
		$(area).empty();
		var ul = $("<ul />");
		$(area).append(ul);
		$.each(results, function(index, result)
		{
			var li = $("<li />");
			$(ul).append(li);
			$(li).html(main.i18n.lookup(result.status, result.status, "html", [result.name, result.ident]));
		});

		main.portal.dialogs.openAlert("siteroster_roster_results");
	};

	this.onSort = function(direction, option)
	{
		me.sortDirection = direction;
		me.sortMode = option;

		me.membersSorted = me.sortMembers(main.members, me.sortDirection, me.sortMode);
		me.populateRoster();
	};
	
	this.sortMembers = function(members, direction, mode)
	{
		var sorted = [];

		if (mode == "N")
		{
			sorted = me.sortByName(members, direction);
		}
		else if (mode == "S")
		{
			sorted = me.sortByRoster(members, direction);
		}
		else if (mode == "T")
		{
			sorted = me.sortByStatus(members, direction);
		}
		else if (mode == "R")
		{
			sorted = me.sortByRole(members, direction);
		}
		
		return sorted;
	};

	this.sortByName = function(members, direction)
	{
		var sorted = [].concat(members);
		var adjust = (direction == 'A') ? 1 : -1;
		sorted.sort(function(a, b)
		{
			var rv = adjust * compareS(a.nameSort, b.nameSort);
			if (rv == 0)
			{
				rv = compareN(a.userId, b.userId);
			}
			return rv;
		});

		return sorted;
	};
	
	this.sortByRoster = function(members, direction)
	{
		var sorted = [].concat(members);
		var adjust = (direction == 'A') ? 1 : -1;
		sorted.sort(function(a, b)
		{
			var rv = adjust * compareS(a.rosterName, b.rosterName);
			if (rv == 0)
			{
				var rv = compareS(a.nameSort, b.nameSort);
				if (rv == 0)
				{
					rv = compareN(a.userId, b.userId);
				}
			}
			return rv;
		});

		return sorted;
	};

	this.sortByStatus = function(members, direction)
	{
		var sorted = [].concat(members);
		var adjust = (direction == 'A') ? 1 : -1;
		sorted.sort(function(a, b)
		{
			var rv = adjust * compareN(main.memberStatusRanking(a), main.memberStatusRanking(b));
			if (rv == 0)
			{
				rv = compareS(a.nameSort, b.nameSort);
				if (rv == 0)
				{
					rv = compareN(a.userId, b.userId);
				}
			}
			return rv;
		});

		return sorted;
	};

	this.sortByRole = function(members, direction)
	{
		var sorted = [].concat(members);
		var adjust = (direction == 'A') ? 1 : -1;
		sorted.sort(function(a, b)
		{
			var rv = adjust * compareN(a.role, b.role);
			if (rv == 0)
			{
				var rv = compareS(a.nameSort, b.nameSort);
				if (rv == 0)
				{
					rv = compareN(a.userId, b.userId);
				}
			}
			return rv;
		});

		return sorted;
	};
}

function SiteRoster_group(main)
{
	var me = this;

	this.rosterSorted = null;

	this.sortDirection = "A";
	this.sortMode = "T";

	this.init = function()
	{
		me.ui = findElements(["siteroster_group_add", "siteroster_group_delete", "siteroster_group_actions",
		                      "siteroster_group_table", "siteroster_group_none", "siteroster_confirmDelete"
		                      ]);
		me.ui.table = new e3_Table(me.ui.siteroster_group_table);
		me.ui.table.setupSelection("siteroster_group_select", me.updateActions);
		me.ui.table.selectAllHeader(1, main.ui.siteroster_header_group);

		me.ui.sort = new e3_SortAction();
		me.ui.sort.inject(me.ui.siteroster_group_actions,
				{onSort: me.onSort, options:[{value:"T", title:main.i18n.lookup("sort_title", "TITLE")},{value:"S", title:main.i18n.lookup("sort_size", "SIZE")}]});
		me.ui.sort.directional(true);

		onClick(me.ui.siteroster_group_add, me.add);
		onClick(me.ui.siteroster_group_delete, me.remove);
		
		setupHoverControls([me.ui.siteroster_group_add, me.ui.siteroster_group_delete]);
	};

	this.start = function()
	{
		// TODO: site might not be current site ...???
		me.site = main.portal.site;

		me.load();
	};

	this.load = function()
	{
		var params = main.portal.cdp.params();
		params.url.site = main.site.id;
		main.portal.cdp.request("roster_groups", params, function(data)
		{
			me.afterLoad(data);
		});
	};
	
	this.afterLoad = function(data)
	{
		main.groups = data.groups || [];
		main.groupsSorted = me.sortGroups(main.groups, me.sortDirection, me.sortMode);

		me.populate();
	};

	this.remove = function()
	{
		var params = main.portal.cdp.params();
		params.url.site = main.portal.site.id;
		params.post.groups = me.ui.table.selected();

		if (params.post.groups.length == 0)
		{
			main.portal.dialogs.openAlert(main.ui.siteroster_selectFirst);
			return;
		}

		main.portal.dialogs.openConfirm(me.ui.siteroster_confirmDelete, main.i18n.lookup("action_delete", "Delete"), function()
		{
			main.portal.cdp.request("roster_groupRemove roster_groups", params, function(data)
			{
				me.afterLoad(data);
			});

			return true;
		}, function(){me.ui.table.clearSelection();});
	};	

	this.populate = function()
	{
		me.ui.table.clear();
		$.each(main.groupsSorted, function(index, group)
		{
			var row = me.ui.table.row();

			me.ui.table.selectBox(group.id);
			me.ui.table.hotText(group.title, main.i18n.lookup("msg_editGroup", "edit group"), function(){main.startGroupEdit(group.id)}, null, {width:"calc(100vw - 100px - 176px)", minWidth:"calc(1200px - 100px - 176px"});
			me.ui.table.text(group.size, null, {width: 80});
		});
		me.ui.table.done();
		show(me.ui.siteroster_group_none, (me.ui.table.rowCount() == 0));
	};

	this.updateActions = function()
	{
		me.ui.table.updateActions([], [me.ui.siteroster_group_delete]);
	};

	this.onSort = function(direction, option)
	{
		me.sortDirection = direction;
		me.sortMode = option;

		main.groupsSorted = me.sortGroups(main.groups, me.sortDirection, me.sortMode);
		me.populate();
	};
	
	this.sortGroups = function(groups, direction, mode)
	{
		var sorted = [];

		if (mode == "T")
		{
			sorted = me.sortByTitle(groups, direction);
		}
		else if (mode == "S")
		{
			sorted = me.sortBySize(groups, direction);
		}
		
		return sorted;
	};

	this.sortByTitle = function(groups, direction)
	{
		var sorted = [].concat(groups);
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
	
	this.sortBySize = function(groups, direction)
	{
		var sorted = [].concat(groups);
		var adjust = (direction == 'A') ? 1 : -1;
		sorted.sort(function(a, b)
		{
			var rv = adjust * compareN(a.size, b.size);
			if (rv == 0)
			{
				var rv = compareS(a.title, b.title);
				if (rv == 0)
				{
					rv = compareN(a.id, b.id);
				}
			}
			return rv;
		});

		return sorted;
	};
	
	this.add = function()
	{
		var group = {id: -1, title: "", members: []};
		main.startGroupEdit(group);
	};
}

function SiteRoster_groupEdit(main)
{
	var me = this;
	
	this.ui = null;

	this.group = null;
	this.edit = null;

	this.init = function()
	{
		me.ui = findElements(["siteroster_groupEdit_title", "siteroster_groupEdit_table"]);

		me.ui.itemNav = new e3_ItemNav();

		me.ui.table = new e3_Table(me.ui.siteroster_groupEdit_table);
		me.ui.table.setupSelection("siteroster_roster_selectMember", me.selectionChanged);
	};

	this.start = function(groupOrId)
	{
		main.onExit = me.checkExit;
		if (groupOrId.id !== undefined)
		{
			me.afterLoad({group: groupOrId});
		}
		else
		{
			me.load(groupOrId);
		}
	};
	
	this.load = function(groupId)
	{
		var params = main.portal.cdp.params();
		params.url.site = main.site.id;
		params.url.group = groupId;
		main.portal.cdp.request("roster_group", params, function(data)
		{
			me.afterLoad(data);
		});
	};

	this.afterLoad = function(data)
	{
		me.group = data.group;

		me.ui.itemNav.inject(main.ui.siteroster_itemnav, {doneFunction: me.done, pos: position(me.group, main.groupsSorted), navigateFunction: me.goGroup});

		me.makeEdit();
		me.populate();
	};

	this.makeEdit = function()
	{
		me.edit = new e3_Edit(me.group, ["id"], function(changed)
		{
			main.ui.modebar.enableSaveDiscard(me.changed(changed) ? me.saveCancel : null);
			me.ui.itemNav.enableSave(me.changed(changed) ? me.saveCancel : null);
		});

		main.ui.modebar.enableSaveDiscard(null);
		me.ui.itemNav.enableSave(null);
	};

	this.changed = function(changed)
	{
		if (changed == null) changed = me.edit.changed();
		return changed || objectArraysDiffer(me.group.members, me.ui.table.selected());
	};

	this.selectionChanged = function()
	{
		main.ui.modebar.enableSaveDiscard(me.changed() ? me.saveCancel : null);
		me.ui.itemNav.enableSave(me.changed() ? me.saveCancel : null);
	};

	this.populate = function()
	{
		me.ui.table.clear();
		$.each(main.members, function(index, member)
		{
			// skip non active, non student?
			if (member.role != Role.student) return;
			if (member.blocked || (!member.active)) return;

			var row = me.ui.table.row();
			me.ui.table.selectBox(member.userId);

			if (member.avatar != null)
			{
				var td = me.ui.table.html("<a href='" + member.avatar + "' target='_blank' class='e3_simple'><img src='" + member.avatar + "' border='0' alt='avatar' style='max-width:48px; max-height:48px;'/></a>",
						null, {width: 64, textAlign: "center"});
			}
			else
			{
				me.ui.table.text("", null, {width: 64});
			}

			var cell = clone(main.ui.siteroster_title_template, ["siteroster_title_template_body", "siteroster_title_template_title", "siteroster_title_template_msg"]);
			me.ui.table.element(cell.siteroster_title_template_body, null, {width: "calc(100vw - 100px - 576px)", minWidth: "calc(1200px - 100px - 576px"});
			cell.siteroster_title_template_title.text(member.nameSort);
			cell.siteroster_title_template_msg.text(member.iid);
		});
		me.ui.table.done();

		$.each(me.group.members, function(index, member)
		{
			me.ui.table.setSelection(member);
		});

		me.edit.setupFilteredEdit(me.ui.siteroster_groupEdit_title, me.edit, "title");

		main.ui.modebar.enableSaveDiscard(null);
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
		// save if changed
		if (me.edit.changed())
		{
			me.save(function(){main.startGroup();});
		}
		else
		{
			main.startGroup();
		}
	};

	this.goGroup = function(group)
	{
		main.startGroupEdit(group.id);
	};

	this.save = function(deferred)
	{
		var params = main.portal.cdp.params();
		params.url.site = main.portal.site.id;
		params.url.group = me.group.id;
		me.edit.params("", params);
		params.post.members = me.ui.table.selected();
		console.log(params);
		main.portal.cdp.request("roster_groupSave roster_group", params, function(data)
		{
			me.group = data.group;
			me.makeEdit();
			me.populate();
			if (deferred !== undefined) deferred();
		});
	};
}

$(function()
{
	try
	{
		siteroster_tool = new SiteRoster();
		siteroster_tool.init();
		siteroster_tool.start();
	}
	catch (e)
	{
		error(e);
	}
});

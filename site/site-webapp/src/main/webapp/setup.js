/**********************************************************************************
 * $URL: https://source.etudes.org/svn/serenity/trunk/site/site-webapp/src/main/webapp/setup.js $
 * $Id: setup.js 11740 2015-10-01 18:43:09Z ggolden $
 ***********************************************************************************
 *
 * Copyright (c) 2014, 2015 Etudes, Inc.
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

var setup_tool = null;

function Setup()
{
	var me = this;

	this.portal = null;
	this.i18n = new e3_i18n(setup_i10n, "en-us");
	this.ui = null;

	this.skins = [];
	this.site = null;

	this.findClientSkin = function(client)
	{
		var found = me.skins[0];
		$.each(me.skins || [], function(index, skin)
		{
			if (skin.client == client.id) found = skin;
		});
		
		return found;
	};

	this.init = function()
	{
		me.i18n.localize();

		me.ui = findElements(["setup_header", "setup_edit_links_table", "setup_select_import_site_table", "setup_nav_rosterlink", "setup_navbar", "setup_actionbar", "setup_nav_siteTitle", "setup_itemNav",
		                      "setup_edit_appearance", "setup_edit_tools", "setup_edit_links", "setup_edit_services", "setup_edit_links_add", "setup_import",
		                      "setup_edit_publication", "setup_editBaseDate", "setup_edit_publish_ui_option_1", "setup_edit_publish_ui_option_2", "setup_edit_publish_ui_option_3",
		                      "setup_skin", "setup_skin_logo", "setup_edit_skin_l", "setup_edit_skin_r",
		                      "setup_tools", "setup_links", "setup_services", "setup_baseDate", "setup_baseDate_table",
		                      "setup_publish_published", "setup_publish_unpublished", "setup_publish_willPublish", "setup_publish_publishOn", "setup_publish_unpublishOn",
		                      "setup_edit_skin_ui", "setup_edit_tools_ui", "setup_edit_links_ui",
		                      "setup_select_import_site_alert", "setup_select_import_site_ui", "setup_importing_site", "setup_select_import_tools", "setup_select_import_tools_alert", "setup_select_import_tools_ui", "setup_importing_tools",
		                      "setup_importing_busy", "setup_importing_done", "setup_importing",
		                      "setup_edit_publish_ui_pubDate", "setup_edit_publish_ui_unpubDate", "setup_edit_publish_ui", "setup_edit_publish_ui_schedule",
		                      "setup_edit_baseDate_baseDate", "setup_edit_baseDate_newDate", "setup_edit_baseDate_table", "setup_edit_baseDate"]);

		me.ui.linksTable = new e3_Table(me.ui.setup_edit_links_table);
		me.ui.sitesTable = new e3_Table(me.ui.setup_select_import_site_table);
		me.ui.itemNav = new e3_ItemNav();

		me.portal = portal_tool.features({pin:[{ui:me.ui.setup_header}]});

		me.site = me.portal.site;
		if (me.portal.toolReturn != null)
		{
			me.portal.sites = me.portal.sites;
		}
	
		me.ui.linksTable.enableReorder();
		me.ui.linksTable.onBecomingEmpty(me.addLink);

		onClick(me.ui.setup_edit_appearance, function(){me.editAppearance();});
		onClick(me.ui.setup_edit_tools, function(){me.editTools();});
		onClick(me.ui.setup_edit_links, function(){me.editLinks();});
		onClick(me.ui.setup_edit_services, function(){me.editServices();});
		onClick(me.ui.setup_edit_links_add, function(){me.addLink();});
		onClick(me.ui.setup_import, function(){me.import();});
		onClick(me.ui.setup_edit_publication, function(){me.editPublication();});
		onClick(me.ui.setup_editBaseDate, function(){me.editBaseDate();});

		onClick(me.ui.setup_edit_publish_ui_option_1, function(){hide(me.ui.setup_edit_publish_ui_schedule);}, true);
		onClick(me.ui.setup_edit_publish_ui_option_2, function(){hide(me.ui.setup_edit_publish_ui_schedule);}, true);
		onClick(me.ui.setup_edit_publish_ui_option_3, function(){show(me.ui.setup_edit_publish_ui_schedule);}, true);

		if (me.portal.toolReturn != null)
		{
			onClick(me.ui.setup_nav_rosterlink, function(){me.portal.navigate(me.site, 114, null);});

			me.setPagingAndNav();

			show([me.ui.setup_navbar, me.ui.setup_actionbar]);
		}
		else
		{
			hide([me.ui.setup_navbar, me.ui.setup_actionbar]);
		}
		
		// setup_header
	};

	this.sitePosition = function()
	{
		var rv = {};
		
		var i = 1;
		var found = null;
		$.each(me.portal.sites || [], function(index, site)
		{
			if (site.id == me.site.id) found = i;
			i++;
		});

		rv.item = found;
		rv.total = me.portal.sites.length;
		rv.prev = found > 1 ? me.portal.sites[found-2] : null;
		rv.next = found < me.portal.sites.length ? me.portal.sites[found] : null;
		
		return rv;
	};

	this.setPagingAndNav = function()
	{		
		me.ui.itemNav.inject(me.ui.setup_itemNav, {pos:me.sitePosition(), returnFunction:function(){me.portal.navigate(me.portal.toolReturn.site, me.portal.toolReturn.toolId, false);}, navigateFunction:me.changeSite});
		me.ui.setup_nav_siteTitle.text(me.site.name);
	};

	this.changeSite = function(site)
	{
		if (site == null) return;

		me.portal.navigate(site, Tools.sitesetup, null, false);
	};

	this.start = function()
	{
		if (this.skins.length == 0)
		{
			me.loadConfig();
		}
		else
		{
			me.populateSite();
		}
	};

	this.loadConfig = function()
	{
		var params = me.portal.cdp.params();
		params.url.site = me.site.id;
		me.portal.cdp.request("site_config", params, function(data)
		{
			me.skins = data.skins || [];
			me.baseDate = data.baseDate || {};
			me.populateSkins();
			me.populateSite();
		});
	};

	this.populateSite = function()
	{
		var name = me.site.skin.name;
		if (me.site.skin.client != 0) name = me.site.client.name;
		me.ui.setup_skin.empty().text(name);
		me.ui.setup_skin_logo.attr("src","/ui/skin/" + me.site.skin.name + "/logo_inst.gif");

		me.ui.setup_tools.empty();
		$.each(me.site.tools || [], function(index, tool)
		{
			var li = $("<li />");
			me.ui.setup_tools.append(li);
			li.text(tool.title);
		});

		me.ui.setup_links.empty();
		$.each(me.site.links || [], function(index, link)
		{
			var dt = $("<dt />");
			me.ui.setup_links.append(dt);
			dt.text(link.title);
			
			var dd = $("<dd />");
			me.ui.setup_links.append(dd);
			dd.text(link.url);
			dd.css("font-style","italic");
		});

		me.ui.setup_services.empty();
		// TODO: li for each service
		
		if (me.baseDate.baseDate != null)
		{
			me.ui.setup_baseDate.text(me.portal.timestamp.displayDate(me.baseDate.baseDate));
		}
		else
		{
			me.ui.setup_baseDate.text("");
		}

		var table = new e3_Table(me.ui.setup_baseDate_table);
		table.clear();
		$.each(me.baseDate.ranges, function(index, range)
		{
			table.row();
			table.text(range.tool);
			if (range.min == range.max)
			{
				table.text(me.portal.timestamp.displayDate(range.min));
			}
			else
			{
				table.text(me.portal.timestamp.displayDate(range.min) + " - " + me.portal.timestamp.displayDate(range.max));
			}
		});
		table.done();

		hide([me.ui.setup_publish_published, me.ui.setup_publish_unpublished, me.ui.setup_publish_willPublish]);
		if (me.site.accessStatus == AccessStatus.open)
		{
			show(me.ui.setup_publish_published);
			hide(me.ui.setup_edit_publish_ui_schedule);
		}
		else if (me.site.accessStatus == AccessStatus.willOpen)
		{
			show([me.ui.setup_publish_willPublish, me.ui.setup_edit_publish_ui_schedule]);
		}
		else // closed
		{
			show(me.ui.setup_publish_unpublished);
			hide(me.ui.setup_edit_publish_ui_schedule);
		}
		
		var na = me.i18n.lookup("na", "<i>n/a</i>");
		var html = na;
		if (me.site.publishOn != null)
		{
			html = me.portal.timestamp.display(me.site.publishOn);
		}
		me.ui.setup_publish_publishOn.html(html);

		html = na;
		if (me.site.unpublishOn != null)
		{
			html = me.portal.timestamp.display(me.site.unpublishOn);
		}
		me.ui.setup_publish_unpublishOn.html(html);
	};

	this.populateSkins = function()
	{
		me.ui.setup_edit_skin_l.empty();
		me.ui.setup_edit_skin_r.empty();
		
		// the site's client's skin
		var clientSkin = me.findClientSkin(me.site.client);
		var clientSkins = [];
		if ((clientSkin != null) && (clientSkin.client != 0)) clientSkins.push(clientSkin);
		var skins = clientSkins.concat(me.skins);
		
		// only generic skins
		var len = 0;
		$.each(me.skins, function(index, skin)
		{
			if (skin.client == 0) len++;
		});

		$.each(skins, function(index, skin)
		{
			if ((index > 0) && (skin.client != 0)) return;

			var entry = $("<div />").addClass("e3_entry");
			var data = $("<div />").addClass("e3_data");
			var label = $("<div />").addClass("e3_label");
			$(entry).append(data);
			$(entry).append(label);
			
			var name = skin.name;
			if (skin.client != 0) name = me.site.client.name;

			var input = $("<input />",{name: "setup_edit_skin_choice", id: "setup_edit_skin_choice_" + skin.id, value: skin.id, type: "radio"});
			var lbl = $("<label />",{"for": "setup_edit_skin_choice_" + skin.id,}).text(name);
			$(data).append(input);
			$(data).append(lbl);
			
			lbl = $("<label />",{"for": "setup_edit_skin_choice_" + skin.id,});
			var img = $("<img />",{src: "/ui/skin/" + skin.name + "/logo_inst.gif"});
			$(lbl).append(img);
			$(label).append(lbl);

			if (index <= (len-1)/2)
			{
				me.ui.setup_edit_skin_l.append(entry);
			}
			else
			{
				me.ui.setup_edit_skin_r.append(entry);
			}
		});
	};

	this.editAppearance = function()
	{
		// setup the current skin choice as checked
		// $("input:radio[name=setup_edit_skin_choice]").prop("checked", false);
		$("#setup_edit_skin_choice_" + me.site.skin.id).prop("checked", true);

		me.portal.dialogs.openDialog(me.ui.setup_edit_skin_ui, me.i18n.lookup("Done", "Done"), function()
		{
			var params = me.portal.cdp.params();
			params.url.site = me.site.id;
			params.post.skin = $("input[name=setup_edit_skin_choice]:checked").val();
			me.portal.cdp.request("site_update", params, function(data)
			{
				me.site = data.site || me.site;
				me.populateSite();
				//if (me.portalRefresh != null) setTimeout(me.portalRefresh, 100);
				location.reload();
			});
			
			return true;
		});
	};

	this.editTools = function()
	{
		$("input:checkbox[name=setup_edit_tools_choices]").prop("checked", false);
		$.each(me.site.tools || [], function(index, tool)
		{
			$("#setup_edit_tools_choices_" + tool.id).prop("checked", true);
		});

		me.portal.dialogs.openDialog(me.ui.setup_edit_tools_ui, me.i18n.lookup("Done", "Done"), function()
		{
			var params = me.portal.cdp.params();
			params.url.site = me.site.id;
			params.post.tools = "";
			$.each($("input:checkbox[name=setup_edit_tools_choices]:checked"), function(index, value)
			{
				params.post.tools = params.post.tools +  $(value).val() + ",";
			});
			if (params.post.tools.length > 0) params.post.tools = params.post.tools.substring(0, params.post.tools.length-1);

			me.portal.cdp.request("site_update", params, function(data)
			{
				me.site = data.site || me.site;
				me.populateSite();
				//if (me.portalRefresh != null) setTimeout(me.portalRefresh, 100);
				location.reload();
			});
			
			return true;
		});
	};

	this.editLinks = function()
	{
		// populate the links into the editor
		me.ui.linksTable.clear();
		$.each(me.site.links || [], function(index, link)
		{
			var row = me.ui.linksTable.row();
			me.ui.linksTable.input({id: "setup_edit_links_title_" + link.id, oid: link.id, size: "30", type:"text", value: link.title, "class": "setup_edit_links_title"});
			me.ui.linksTable.input({id: "setup_edit_links_url_" + link.id, oid: link.id, size: "60", type:"text", value: link.url, "class": "setup_edit_links_url"});
			me.ui.linksTable.text("");
			me.ui.linksTable.icon("/ui/icons/delete.png", me.i18n.lookup("Delete", "Delete"), function(){me.deleteLink(row);});
			me.ui.linksTable.reorder(me.i18n.lookup("reorder", "Drag to Reorder"));
		});

		if (me.site.links.length == 0)
		{
			me.addLink();
		}
		me.ui.linksTable.done();

		me.portal.dialogs.openDialog(me.ui.setup_edit_links_ui, me.i18n.lookup("Done", "Done"), function()
		{
			// validate, return false
			
			var params = me.portal.cdp.params();
			params.url.site = me.site.id;

			// collect the edited links
			var titles = $(".setup_edit_links_title");
			var urls = $(".setup_edit_links_url");

			for (var i = 0; i < titles.length; i++)
			{
				params.post["title" + i] = $.trim($(titles[i]).val());
				params.post["url" + i] = $.trim($(urls[i]).val());
				params.post["id" + i] = $.trim($(titles[i]).attr("oid"));
			}

			params.post.links = titles.length;

			me.portal.cdp.request("site_update", params, function(data)
			{
				me.site = data.site || me.site;
				me.populateSite();
				//if (me.portalRefresh != null) setTimeout(me.portalRefresh, 100);
				location.reload();
			});
			
			return true;
		});
	};

	this.addLink = function()
	{
		var link = {id:0, title:"", url:""};
		var row = me.ui.linksTable.row();
		me.ui.linksTable.input({id: "setup_edit_links_title_" + link.id, oid: link.id, size: "30", type:"text", value: link.title, "class": "setup_edit_links_title"});			
		me.ui.linksTable.input({id: "setup_edit_links_url_" + link.id, oid: link.id, size: "60", type:"text", value: link.url, "class": "setup_edit_links_url"});
		me.ui.linksTable.text("");
		me.ui.linksTable.icon("/ui/icons/delete.png", me.i18n.lookup("Delete", "Delete"), function(){me.deleteLink(row);});
		me.ui.linksTable.reorder(me.i18n.lookup("reorder", "Drag to Reorder"));
		
		$(row).focus();
		return row;
	};

	this.deleteLink = function(tableRow)
	{
		me.ui.linksTable.removeRow(tableRow);
	};

	this.editServices = function()
	{
		alert("edit services");
	};

	this.import = function()
	{
		var params = me.portal.cdp.params();
		params.url.site = me.site.id;
		me.portal.cdp.request("site_importSites", params, function(data)
		{
			var splitSites = me.prepForImportSitesList(data.sites, data.archives);

			me.ui.sitesTable.clear();
			hide(me.ui.setup_select_import_site_alert);
			$.each(splitSites || [], function(index, value)
			{
				any = true;
				var row = me.ui.sitesTable.row();
				
				// header?
				if ($.type(value.left) === "string")
				{
					me.ui.sitesTable.headerRow(value.left);
					// row = me.ui.sitesTable.row();
				}
				
				// otherwise one or two sites
				else
				{
					// left
					var td = me.ui.sitesTable.radioAndLabel("setup_select_import_site_choice_" + value.left.type + value.left.id, "setup_select_import_site_choice", value.left.type + value.left.id, value.left.name);
					
					// right
					if (value.right != null)
					{
						me.ui.sitesTable.radioAndLabel("setup_select_import_site_choice_" + value.left.type + value.right.id, "setup_select_import_site_choice", value.right.type + value.right.id, value.right.name);
					}
					else
					{
						me.ui.sitesTable.text("");
						me.ui.sitesTable.text("");
					}
				}
			});
			
			if ((data.sites.length == 0) && (data.archives.length == 0))
			{
				me.ui.sitesTable.row();
				me.ui.sitesTable.headerRow(me.i18n.lookup("noSitesImport", "You have no other sites to import from."));
			}
			me.ui.sitesTable.done();

			onClick($("input[name=setup_select_import_site_choice]"), function(){hide(me.ui.setup_select_import_site_alert);}, true);

			me.portal.dialogs.openDialog(me.ui.setup_select_import_site_ui, me.i18n.lookup("Continue", "Continue"), ((data.sites.length == 0) && (data.archives.length == 0)) ? null : function()
			{
				var val = $("input[name=setup_select_import_site_choice]:checked").val();
				params.post.import = val.substring(1);
				params.post.type = val.charAt(0);
				if (params.post.import == null)
				{
					show(me.ui.setup_select_import_site_alert);
					return false;
				}
				me.ui.setup_importing_site.empty().text($("input[name=setup_select_import_site_choice]:checked").attr("label"));

				me.portal.cdp.request("site_importTools", params, function(data)
				{
					var tools = data.tools;

					// populate the tools, bring up the dialog...
					me.ui.setup_select_import_tools.empty();
					hide(me.ui.setup_select_import_tools_alert);
					$.each(tools, function(index, tool)
					{
						var entry = $("<div />").addClass("e3_entry");
						me.ui.setup_select_import_tools.append(entry);						
						var input = $("<input />",{name: "setup_select_import_tools_choice", id: "setup_select_import_tools_choice_" + tool.id, value: tool.id, type: "checkbox"});
						$(input).attr("label", tool.title);
						var lbl = $("<label />",{"for": "setup_select_import_tools_choice_" + tool.id,}).text(tool.title);
						$(entry).append(input);
						$(entry).append(lbl);
					});

					onClick($("input:checkbox[name=setup_select_import_tools_choice]"), function(){hide(me.ui.setup_select_import_tools_alert);}, true);

					me.portal.dialogs.openDialog(me.ui.setup_select_import_tools_ui, me.i18n.lookup("Import", "Import"), (tools.length == 0) ? null : function()
					{
						params.post.tools = "";
						var toolsUl = me.ui.etup_importing_tools;
						me.ui.setup_importing_tools.empty();
						$.each($("input:checkbox[name=setup_select_import_tools_choice]:checked"), function(index, value)
						{
							params.post.tools = params.post.tools +  value.val() + ",";
							
							var li = $("<li />");
							me.ui.etup_importing_tools.append(li);
							li.text($(value).attr("label"));
						});

						if (params.post.tools == "")
						{
							show(me.ui.setup_select_import_tools_alert);
							return false;
						}

						// start spinning
						show(me.ui.setup_importing_busy);
						hide(me.ui.setup_importing_done);
						me.portal.dialogs.openBusy(me.ui.setup_importing, function(){location.reload();});

						me.portal.cdp.request("site_import", params, function(data)
						{
							// stop spinning
							hide(me.ui.setup_importing_busy);
							show(me.ui.setup_importing_done);
							me.portal.dialogs.doneBusy(me.ui.setup_importing);
						});

						return true;
					});
				});
				
				return true;
			});
		});
	};

	// into two cols, left  to right, with headers in the left for new terms (and the right for those empty)
	this.prepForImportSitesList = function(sites, archives)
	{
		var rv = new Array();
		
		for (var i = 0; i < sites.length; i++)
		{
			// inject header for a term change
			if (((i > 0) && (sites[i-1].term != sites[i].term)) || (i == 0))
			{
				var entry = {};
				entry.left = sites[i].term;
				entry.right = null;
				rv.push(entry);
			}
			
			var entry = {};
			entry.left = sites[i];
			entry.right = null;
			// do the right only if it matches the term of the left
			if ((i+1 < sites.length) && (sites[i+1].term == sites[i].term))
			{
				i++;
				entry.right = sites[i];
			}
			rv.push(entry);
		}

		for (var i = 0; i < archives.length; i++)
		{
			// inject header for a term change
			if (((i > 0) && (archives[i-1].term != archives[i].term)) || (i == 0))
			{
				var entry = {};
				entry.left = archives[i].term + " " + me.i18n.lookup("Archives", "Archives");
				entry.right = null;
				rv.push(entry);
			}
			
			var entry = {};
			entry.left = archives[i];
			entry.right = null;
			// do the right only if it matches the term of the left
			if ((i+1 < archives.length) && (archives[i+1].term == archives[i].term))
			{
				i++;
				entry.right = archives[i];
			}
			rv.push(entry);
		}

		return rv;
	};

	this.editPublication = function()
	{
		// if there are dates defined, enable the dates, otherwise set based on published
		if ((me.site.publishOn != null) || (me.site.unpublishOn != null))
		{
			$("input:radio[name=setup_edit_publish_ui_option][value=3]").prop('checked', true);
			show(me.ui.setup_edit_publish_ui_schedule);
		}
		else
		{
			if (me.site.accessStatus == 0) // open
			{
				$("input:radio[name=setup_edit_publish_ui_option][value=1]").prop('checked', true);
			}
			else // closed
			{
				$("input:radio[name=setup_edit_publish_ui_option][value=2]").prop('checked', true);
			}
			hide(me.ui.setup_edit_publish_ui_schedule);
		}
		me.portal.timestamp.setInput(me.ui.setup_edit_publish_ui_pubDate, true, me.site.publishOn);
		me.portal.timestamp.setInput(me.ui.setup_edit_publish_ui_unpubDate, false, me.site.unpublishOn);

		me.portal.dialogs.openDialog(me.ui.setup_edit_publish_ui, me.i18n.lookup("Done", "Done"), function()
		{
			var params = me.portal.cdp.params();
			params.url.site = me.site.id;
			params.post.publishOption = $("input:radio[name=setup_edit_publish_ui_option]:checked").val();
			params.post.publishDate = me.portal.timestamp.getInput(me.ui.setup_edit_publish_ui_pubDate);
			params.post.unpublishDate = me.portal.timestamp.getInput(me.ui.setup_edit_publish_ui_unpubDate);
			me.portal.cdp.request("site_update", params, function(data)
			{
				me.site = data.site || me.site;
				me.populateSite();
				//if (me.portalRefresh != null) setTimeout(me.portalRefresh, 100);
				location.reload();
			});
			
			return true;
		});
	};

	this.editBaseDate = function()
	{
		me.ui.setup_edit_baseDate_baseDate.text(me.portal.timestamp.displayDate(me.baseDate.baseDate));
		me.portal.timestamp.setDateInput(me.ui.setup_edit_baseDate_newDate, me.baseDate.baseDate);
		onChange(me.ui.setup_edit_baseDate_newDate, function(){me.adjustNewRanges();});

		var table = new e3_Table(me.ui.setup_edit_baseDate_table);
		table.clear();
		$.each(me.baseDate.ranges, function(index, range)
		{
			table.row();
			table.text(range.tool);
			var rangeDisplay = null;
			if (range.min == range.max)
			{
				rangeDisplay = me.portal.timestamp.displayDate(range.min);
			}
			else
			{
				rangeDisplay = me.portal.timestamp.displayDate(range.min) + " - " + me.portal.timestamp.displayDate(range.max);
			}
			table.text(rangeDisplay);
			table.text(rangeDisplay);
		});
		table.done();

		me.portal.dialogs.openDialog(me.ui.setup_edit_baseDate, me.i18n.lookup("Done", "Done"), function()
		{
			var params = me.portal.cdp.params();
			params.url.site = me.site.id;
			params.post.newBaseDate = me.portal.timestamp.getDateInput(me.ui.setup_edit_baseDate_newDate);
			params.post.baseDate = me.baseDate.baseDate;
			me.portal.cdp.request("site_update", params, function(data)
			{
				me.site = data.site || me.site;
				me.populateSite();
				//if (me.portalRefresh != null) setTimeout(me.portalRefresh, 100);
				location.reload();
			});
			
			return true;
		});
	};
	
	this.adjustNewRanges = function()
	{
		var newDate = me.portal.timestamp.getDateInput(me.ui.setup_edit_baseDate_newDate);
		if (newDate == null)
		{
			newDate = me.baseDate.baseDate;
			me.portal.timestamp.setDateInput(me.ui.setup_edit_baseDate_newDate, newDate);
		}
		var days = me.portal.timestamp.difference(me.baseDate.baseDate, newDate);
		var table = new e3_Table(me.ui.setup_edit_baseDate_table);
		table.clear();
		$.each(me.baseDate.ranges, function(index, range)
		{
			table.row();
			table.text(range.tool);
			var special = "e3_draft";
			if (range.min == range.max)
			{
				table.text(me.portal.timestamp.displayDate(range.min));
			}
			else
			{
				table.text(me.portal.timestamp.displayDate(range.min) + " - " + me.portal.timestamp.displayDate(range.max));
			}

			if (range.min == range.max)
			{
				table.text(me.portal.timestamp.displayDate(me.portal.timestamp.adjust(range.min, days)), ((days == 0) ? null : special));
			}
			else
			{
				table.text(me.portal.timestamp.displayDate(me.portal.timestamp.adjust(range.min, days)) + " - " + me.portal.timestamp.displayDate(me.portal.timestamp.adjust(range.max, days)), ((days == 0) ? null : special));
			}
		});
		table.done();
	};
}

$(function()
{
	try
	{
		setup_tool = new Setup();
		setup_tool.init();
		setup_tool.start();
	}
	catch (e)
	{
		error(e);
	}
});

/**********************************************************************************
 * $URL: https://source.etudes.org/svn/serenity/trunk/core/core-api/src/main/java/org/etudes/sitecontent/api/SiteImportService.java $
 * $Id: SiteImportService.java 9484 2014-12-08 19:34:15Z ggolden $
 ***********************************************************************************
 *
 * Copyright (c) 2014 Etudes, Inc.
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

package org.etudes.sitecontent.api;

import java.util.Set;

import org.etudes.site.api.Site;
import org.etudes.tool.api.Tool;
import org.etudes.user.api.User;

/**
 * The E3 SiteImport service.
 */
public interface SiteImportService
{
	/**
	 * Import contents from one site into another.
	 * 
	 * @param fromSite
	 *        The source site.
	 * @param tools
	 *        A Set of Tools to import content from.
	 * @param intoSite
	 *        The destination site.
	 * @param importingUser
	 *        The user doing the import.
	 * @return true if successful, false if not.
	 */
	boolean importFromSite(Site fromSite, Set<Tool> tools, Site intoSite, User importingUser);

	/**
	 * Check if this tool imports from site.
	 * 
	 * @param tool
	 *        The Tool.
	 * @return true of the tool participates in site import, false if not.
	 */
	boolean imports(Tool tool);
}

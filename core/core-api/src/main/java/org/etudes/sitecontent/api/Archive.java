/**********************************************************************************
 * $URL: https://source.etudes.org/svn/serenity/trunk/core/core-api/src/main/java/org/etudes/sitecontent/api/Archive.java $
 * $Id: Archive.java 10081 2015-02-14 23:06:22Z ggolden $
 ***********************************************************************************
 *
 * Copyright (c) 2009, 2015 Etudes, Inc.
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

import org.etudes.tool.api.Tool;

/**
 * Archive is the collection of archived data for a site.
 */
public interface Archive
{
	/**
	 * Add this artifact to the archive.
	 * 
	 * @param artifact
	 *        The artifact to archive.
	 */
	void archive(Artifact artifact);

	/**
	 * Create a new artifact that can be archived.
	 * 
	 * @param tool
	 *        The tool handling the artifact.
	 * @param type
	 *        The artifact type (a string defined by each archiving tool).
	 * @returnA new empty artifact.
	 */
	Artifact newArtifact(Tool tool, String type);

	/**
	 * Translate the content html by any needed translations.
	 * 
	 * @param content
	 *        The content html.
	 * @return The updated content.
	 */
	String translateContentBody(String content);
}

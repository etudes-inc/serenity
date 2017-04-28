/**********************************************************************************
 * $URL: https://source.etudes.org/svn/serenity/trunk/core/core-api/src/main/java/org/etudes/file/api/Reference.java $
 * $Id: Reference.java 11567 2015-09-06 20:22:36Z ggolden $
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

package org.etudes.file.api;

import java.util.Comparator;
import java.util.Map;

import org.etudes.roster.api.Role;
import org.etudes.tool.api.ToolItemReference;

/**
 * E3 file reference object. Any tool item (or other item) that references a FileService file must register a reference. When FileService files have no references, they can be deleted.
 */
public interface Reference
{
	public static class FileNameComparator implements Comparator<Reference>
	{
		public int compare(Reference r1, Reference r2)
		{
			return r1.getFile().getName().toLowerCase().compareTo(r2.getFile().getName().toLowerCase());
		}
	}

	/**
	 * @return The full (sever root relative) URL for downloading this referenced file, if the file is published, or null if not.
	 */
	String getDownloadUrl();

	/**
	 * @return The File referenced.
	 */
	File getFile();

	/**
	 * @return the ToolItemReference for the tool item holding this reference to this file.
	 */
	ToolItemReference getHolder();

	/**
	 * @return the reference id.
	 */
	Long getId();

	/**
	 * @return The placeholder URL to be placed into stored content and later converted to a download URL before serving.
	 */
	String getPlaceholderUrl();

	/**
	 * @return The security level (Role) of the publication.
	 */
	Role getSecurity();

	/**
	 * Format for sending via CDP.
	 * 
	 * @return The map, ready to add as an element to the return map.
	 */
	Map<String, Object> send();
}

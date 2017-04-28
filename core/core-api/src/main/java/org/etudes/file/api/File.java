/**********************************************************************************
 * $URL: https://source.etudes.org/svn/serenity/trunk/core/core-api/src/main/java/org/etudes/file/api/File.java $
 * $Id: File.java 11567 2015-09-06 20:22:36Z ggolden $
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

import java.io.InputStream;
import java.util.Date;
import java.util.Map;

/**
 * E3 file storage object.
 */
public interface File
{
	/**
	 * @return The date the file was created / initially uploaded
	 */
	Date getDate();

	/**
	 * @return The file system id.
	 */
	Long getId();

	/**
	 * @return The date the file was last modified.
	 */
	Date getModifiedOn();

	/**
	 * @return The file name (at the time of creation) (no path)
	 */
	String getName();

	/**
	 * @return The file size (in bytes).
	 */
	int getSize();

	/**
	 * @return The file mime type (normalized to lower case)
	 */
	String getType();

	/**
	 * @return The file content as a byte[]
	 */
	byte[] readBytes();

	/**
	 * @return The file content as a stream.
	 */
	InputStream readStream();

	/**
	 * @return The file content as a string (UTF-8 encoding).
	 */
	String readString();

	/**
	 * Format for sending via CDP.
	 * 
	 * @return The map, ready to add as an element to the return map.
	 */
	Map<String, Object> send();
}

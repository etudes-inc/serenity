/**********************************************************************************
 * $URL: https://source.etudes.org/svn/serenity/trunk/core/core-api/src/main/java/org/etudes/threadlocal/api/ThreadLocalService.java $
 * $Id: ThreadLocalService.java 9804 2015-01-12 22:30:55Z ggolden $
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

package org.etudes.threadlocal.api;

/**
 * Provide a map, keyed by string, storing any object needed, that is threadlocal to each thread. The API is a subset of the java Map interface.
 */
public interface ThreadLocalService
{
	/**
	 * Remove all objects in the threadlocal map for this thread.
	 */
	void clear();

	/**
	 * Find the object under this key in the threadlocal map for the current thread.
	 * 
	 * @param name
	 *        The binding name.
	 * @return The object for this key, or null if not found.
	 */
	Object get(String key);

	/**
	 * Set this object into the threadlocal map for the current thread under this key.
	 * 
	 * @param key
	 *        The key to retrieve the object.
	 * @param value
	 *        The object. If null, acts as a remove(key).
	 */
	void put(String key, Object value);

	/**
	 * Remove the object under this key from the threadlocal map for the current thread.
	 * 
	 * @param key
	 *        The key of the object to remove.
	 */
	void remove(String key);
}

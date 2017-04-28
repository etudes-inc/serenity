/**********************************************************************************
 * $URL: https://source.etudes.org/svn/serenity/trunk/core/core-api/src/main/java/org/etudes/cron/api/RunTime.java $
 * $Id: RunTime.java 8504 2014-08-23 04:12:49Z ggolden $
 ***********************************************************************************
 *
 * Copyright (c) 2009, 2014 Etudes, Inc.
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

package org.etudes.cron.api;

/**
 * RunTime represents a time of day to run a task.
 */
public interface RunTime
{
	/**
	 * @return The hour (0..23)
	 */
	int getHour();

	/**
	 * @return The minute (0..59)
	 */
	int getMinute();

	/**
	 * Check if the other time matches this one, or is just after within a threshold.
	 * 
	 * @param other
	 *        The other time.
	 * @return true if satisfied, false if not.
	 */
	boolean isSatisfiedBy(RunTime other);
}

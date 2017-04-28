/**********************************************************************************
 * $URL: https://source.etudes.org/svn/serenity/trunk/core/core-api/src/main/java/org/etudes/cron/api/CronService.java $
 * $Id: CronService.java 8504 2014-08-23 04:12:49Z ggolden $
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

package org.etudes.cron.api;

public interface CronService
{
	/**
	 * Run a job in the background, on behalf of the user.
	 * 
	 * @param user
	 *        The user.
	 * @param job
	 *        The job.
	 */
	void runJob(Runnable job);

	/**
	 * Create a new RunTime based on now.
	 * 
	 * @return The RunTime.
	 */
	RunTime runTime();

	/**
	 * Create a new RunTime.
	 * 
	 * @param hourOfDay
	 *        The hour (24 hour clock, 0 .. 23).
	 * @param minuteOfHour
	 *        The minute (0 .. 59).
	 * @return The RunTime.
	 */
	RunTime runTime(int hourOfDay, int minuteOfHour);

	/**
	 * Create a new RunTime.
	 * 
	 * @param runtime
	 *        The string representation of the runtime (i.e. "23:59")
	 * @return The RunTime.
	 */
	RunTime runTime(String runtime);
}

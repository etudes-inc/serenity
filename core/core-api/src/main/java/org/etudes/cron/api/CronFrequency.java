/**********************************************************************************
 * $URL: https://source.etudes.org/svn/serenity/trunk/core/core-api/src/main/java/org/etudes/cron/api/CronFrequency.java $
 * $Id: CronFrequency.java 8504 2014-08-23 04:12:49Z ggolden $
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

public enum CronFrequency
{
	fiveMins(5), //
	halfDay(12 * 60), //
	halfHour(30), //
	hour(60), //
	minute(1), //
	never(0), //
	runTime(0);

	private int mins;

	private CronFrequency(int mins)
	{
		this.mins = mins;
	}

	public int getMins()
	{
		return this.mins;
	}
}

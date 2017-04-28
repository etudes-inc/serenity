/**********************************************************************************
 * $URL: https://source.etudes.org/svn/serenity/trunk/core/core-api/src/main/java/org/etudes/sitecontent/api/DateRange.java $
 * $Id: DateRange.java 10143 2015-02-25 03:37:59Z ggolden $
 ***********************************************************************************
 *
 * Copyright (c) 2015 Etudes, Inc.
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

import java.util.Date;

import org.etudes.tool.api.Tool;

/**
 * DateRange models a tool's range of dates.
 */
public interface DateRange
{
	/**
	 * @return The maximum date used by the tool.
	 */
	Date getMax();

	/**
	 * @return The minimum date used by the tool.
	 */
	Date getMin();

	/**
	 * @return The tool.
	 */
	Tool getTool();
}

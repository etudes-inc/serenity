/**********************************************************************************
 * $URL: https://source.etudes.org/svn/serenity/trunk/syllabus/syllabus-api/src/main/java/org/etudes/syllabus/api/SyllabusAcceptance.java $
 * $Id: SyllabusAcceptance.java 10165 2015-02-26 23:24:48Z ggolden $
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

package org.etudes.syllabus.api;

import java.util.Date;

import org.etudes.user.api.User;

/**
 * SyllabusAcceptance models a user accepting a syllabus
 */
public interface SyllabusAcceptance
{
	/**
	 * @return The user who accepted.
	 */
	User getAcceptedBy();

	/**
	 * @return The date accepted
	 */
	Date getAcceptedOn();

	/**
	 * @return The syllabus accepted.
	 */
	Syllabus getSyllabus();

	/**
	 * @return TRUE if the acceptance is valid, FALSE if not.
	 */
	Boolean isValid();
}

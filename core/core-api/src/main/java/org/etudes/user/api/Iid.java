/**********************************************************************************
 * $URL: https://source.etudes.org/svn/serenity/trunk/core/core-api/src/main/java/org/etudes/user/api/Iid.java $
 * $Id: Iid.java 10421 2015-04-03 20:34:51Z ggolden $
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

package org.etudes.user.api;

/**
 * IID models an Etudes user's Institutional Id.
 */
public interface Iid extends Comparable<Iid>
{
	/**
	 * @return the client IID code portion of the IID.
	 */
	String getCode();

	/**
	 * @return the id portion of the IID.
	 */
	String getId();
}

/**********************************************************************************
 * $URL: https://source.etudes.org/svn/serenity/trunk/core/core-api/src/main/java/org/etudes/site/api/Skin.java $
 * $Id: Skin.java 8641 2014-09-04 02:35:12Z ggolden $
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

package org.etudes.site.api;

public interface Skin
{
	/**
	 * @return The client code associated with the skin. If 0, the skin can be used by any client. If >0, the skin is intended for just the client with this id.
	 */
	Long getClient();

	/**
	 * @return The skin color (6 digit hex code).
	 */
	String getColor();

	/**
	 * @return The skin id.
	 */
	Long getId();

	/**
	 * @return The skin name.
	 */
	String getName();
}

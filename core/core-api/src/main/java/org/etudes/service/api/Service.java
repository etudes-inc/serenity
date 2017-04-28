/**********************************************************************************
 * $URL: https://source.etudes.org/svn/serenity/trunk/core/core-api/src/main/java/org/etudes/service/api/Service.java $
 * $Id: Service.java 8354 2014-07-14 00:25:39Z ggolden $
 ***********************************************************************************
 *
 * Copyright (c) 2013, 2014 Etudes, Inc.
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

package org.etudes.service.api;

/**
 * Interface implemented by all Etudes services.
 */
public interface Service
{
	/**
	 * Prepare to start service. All other services have been loaded, but not yet started.
	 * 
	 * @return true if successful.
	 */
	boolean start();
}

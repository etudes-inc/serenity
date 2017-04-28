/**********************************************************************************
 * $URL: https://source.etudes.org/svn/serenity/trunk/core/core-api/src/main/java/org/etudes/tracking/api/Tracking.java $
 * $Id: Tracking.java 8432 2014-08-05 20:54:34Z ggolden $
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

package org.etudes.tracking.api;

import java.util.Date;

import org.etudes.site.api.Site;
import org.etudes.user.api.User;

public interface Tracking
{
	User getUser();
	Site getSite();
	Date getFirstVisit();
	Date getLastVisit();
	Long getVisits();
}

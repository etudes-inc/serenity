/**********************************************************************************
 * $URL: https://source.etudes.org/svn/serenity/trunk/tomcatlistener/src/main/java/org/etudes/tomcat/Listener.java $
 * $Id: Listener.java 8293 2014-06-20 21:42:19Z ggolden $
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

package org.etudes.tomcat;

import org.apache.catalina.LifecycleEvent;
import org.apache.catalina.LifecycleListener;
import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;
import org.etudes.service.api.Services;

// configure in tomcat's conf/server.xml, in the main <Server section, below the other listeners:
// <Listener className="org.etudes.tomcat.Listener" />

public class Listener implements LifecycleListener
{
	/** Our log. */
	private static Log M_log = LogFactory.getLog(Listener.class);

	public void lifecycleEvent(LifecycleEvent event)
	{
		if (event.getType().equals("after_start"))
		{
			M_log.info("after_start event - starting Services");
			Services.start();
		}
	}
}

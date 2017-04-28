/**********************************************************************************
 * $URL: https://source.etudes.org/svn/serenity/trunk/gateway/gateway-webapp/src/main/java/org/etudes/gateway/webapp/GatewayServiceImpl.java $
 * $Id: GatewayServiceImpl.java 10386 2015-04-01 20:41:28Z ggolden $
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

package org.etudes.gateway.webapp;

import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;
import org.etudes.gateway.api.GatewayService;
import org.etudes.service.api.Service;

/**
 * GatewayServiceImpl implements GatewayService.
 */
public class GatewayServiceImpl implements GatewayService, Service
{
	/** Our log. */
	private static Log M_log = LogFactory.getLog(GatewayServiceImpl.class);

	/**
	 * Construct
	 */
	public GatewayServiceImpl()
	{
		M_log.info("HomeServiceImpl: construct");
	}

	@Override
	public boolean start()
	{
		M_log.info("HomeServiceImpl: start");
		return true;
	}
}

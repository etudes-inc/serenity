/**********************************************************************************
 * $URL: https://source.etudes.org/svn/serenity/trunk/monitor/monitor-api/src/main/java/org/etudes/monitor/api/MonitorService.java $
 * $Id: MonitorService.java 12036 2015-11-08 01:49:32Z ggolden $
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

package org.etudes.monitor.api;

import java.util.List;

/**
 * The Serenity Monitor service.
 */
public interface MonitorService
{
	/**
	 * Get the current alerts for this source.
	 * 
	 * @param source
	 *        The name of the source.
	 * @return The list of alerts; empty if there are none.
	 */
	public List<Alert> getAlerts(String source);

	/**
	 * @return The current set of options.
	 */
	public Options getOptions();

	/**
	 * access the current sample from all sources.
	 * 
	 * @return The list of samples
	 */
	public List<Sample> getSamples();

	/**
	 * Accept a new sample.
	 */
	public void sample(Sample sample);

	/**
	 * Save these as the options.
	 * 
	 * @param options
	 *        The options.
	 */
	public void setOptions(Options options);

	/**
	 * Check the source against the IP address. The source must be expected, and match the IP address.
	 * 
	 * @param source
	 *        The sample source.
	 * @param address
	 *        The request IP address.
	 * @return true if valid, false if not.
	 */
	public boolean validSource(String source, String address);
}

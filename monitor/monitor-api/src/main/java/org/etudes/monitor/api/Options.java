/**********************************************************************************
 * $URL: https://source.etudes.org/svn/serenity/trunk/monitor/monitor-api/src/main/java/org/etudes/monitor/api/Options.java $
 * $Id: Options.java 12036 2015-11-08 01:49:32Z ggolden $
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

import java.util.HashMap;
import java.util.Map;

import org.etudes.cdp.api.CdpService;
import org.etudes.service.api.Services;

/**
 * Monitor Options
 */
public class Options
{
	protected float appserverResponse = 10.0f; // over this time in seconds to check an app server is a concern
	protected int diskUsedPct = 90; // over this % used on a disk is a concern
	protected float loadAvg = 4.0f; // server 1-minute load > 4 is a concern
	protected int openApache = 200; // over this # of connections between tomcat and apache is a concern
	protected int openFiles = 1500; // over this # of open files total is a concern
	protected int openMysql = 20; // over this # of connections to the db is a concern
	protected int queriesActive = 20; // mysql with > x active (not sleeping) queries is a concern
	protected int queriesTotal = 180; // mysql with > x queries (connections) is a concern
	protected long sinceDbBackup = 24l * 60l * 60l * 1000l; // 1 day expressed in MS - how long since last db backup is considered too long
	protected long sinceFsBackup = 2l * 60l * 60l * 1000l; // 2 hours expressed in MS - how long since last fs backup is considered too long
	protected long sinceOffsiteBackup = 24l * 60l * 60l * 1000l; // 1 day expressed in MS - how long since last offsite sync is considered too long
	protected long sinceReport = 2l * 60l * 1000l; // 2 minutes expressed in MS - how long a sample lasts until the source is considered absent

	public Float getAppserverResponse()
	{
		return this.appserverResponse;
	}

	public Integer getDiskUsedPct()
	{
		return this.diskUsedPct;
	}

	public Float getLoadAvg()
	{
		return this.loadAvg;
	}

	public Integer getOpenApache()
	{
		return this.openApache;
	}

	public Integer getOpenFiles()
	{
		return this.openFiles;
	}

	public Integer getOpenMysql()
	{
		return this.openMysql;
	}

	public Integer getQueriesActive()
	{
		return this.queriesActive;
	}

	public Integer getQueriesTotal()
	{
		return this.queriesTotal;
	}

	public Long getSinceDbBackup()
	{
		return this.sinceDbBackup;
	}

	public Long getSinceFsBackup()
	{
		return this.sinceFsBackup;
	}

	public Long getSinceOffsiteBackup()
	{
		return this.sinceOffsiteBackup;
	}

	public Long getSinceReport()
	{
		return this.sinceReport;
	}

	public void read(String prefix, Map<String, Object> parameters)
	{
		setOpenFiles(cdpService().readInt(parameters.get(prefix + "openFiles")));
		setOpenApache(cdpService().readInt(parameters.get(prefix + "openApache")));
		setOpenMysql(cdpService().readInt(parameters.get(prefix + "openMysql")));
		setSinceReport(cdpService().readLong(parameters.get(prefix + "sinceReport")));
		setSinceDbBackup(cdpService().readLong(parameters.get(prefix + "sinceDbBackup")));
		setSinceFsBackup(cdpService().readLong(parameters.get(prefix + "sinceFsBackup")));
		setSinceOffsiteBackup(cdpService().readLong(parameters.get(prefix + "sinceOffsiteBackup")));
		setLoadAvg(cdpService().readFloat(parameters.get(prefix + "loadAvg")));
		setDiskUsedPct(cdpService().readInt(parameters.get(prefix + "diskUsedPct")));
		setAppserverResponse(cdpService().readFloat(parameters.get(prefix + "appserverResponse")));
		setQueriesTotal(cdpService().readInt(parameters.get(prefix + "queriesTotal")));
		setQueriesActive(cdpService().readInt(parameters.get(prefix + "queriesActive")));
	}

	public Map<String, Object> send()
	{
		Map<String, Object> rv = new HashMap<String, Object>();

		rv.put("openFiles", getOpenFiles());
		rv.put("openApache", getOpenApache());
		rv.put("openMysql", getOpenMysql());
		rv.put("sinceReport", getSinceReport());
		rv.put("sinceDbBackup", getSinceDbBackup());
		rv.put("sinceFsBackup", getSinceFsBackup());
		rv.put("sinceOffsiteBackup", getSinceOffsiteBackup());
		rv.put("loadAvg", getLoadAvg());
		rv.put("diskUsedPct", getDiskUsedPct());
		rv.put("appserverResponse", getAppserverResponse());
		rv.put("queriesTotal", getQueriesTotal());
		rv.put("queriesActive", getQueriesActive());

		return rv;
	}

	public void setAppserverResponse(Float v)
	{
		this.appserverResponse = v;
	}

	public void setDiskUsedPct(Integer v)
	{
		this.diskUsedPct = v;
	}

	public void setLoadAvg(Float v)
	{
		this.loadAvg = v;
	}

	public void setOpenApache(Integer v)
	{
		this.openApache = v;
	}

	public void setOpenFiles(Integer v)
	{
		this.openFiles = v;
	}

	public void setOpenMysql(Integer v)
	{
		this.openMysql = v;
	}

	public void setQueriesActive(Integer v)
	{
		this.queriesActive = v;
	}

	public void setQueriesTotal(Integer v)
	{
		this.queriesTotal = v;
	}

	public void setSinceDbBackup(Long v)
	{
		this.sinceDbBackup = v;
	}

	public void setSinceFsBackup(Long v)
	{
		this.sinceFsBackup = v;
	}

	public void setSinceOffsiteBackup(Long v)
	{
		this.sinceOffsiteBackup = v;
	}

	public void setSinceReport(Long v)
	{
		this.sinceReport = v;
	}

	/**
	 * @return The registered CdpService.
	 */
	private CdpService cdpService()
	{
		return (CdpService) Services.get(CdpService.class);
	}
}

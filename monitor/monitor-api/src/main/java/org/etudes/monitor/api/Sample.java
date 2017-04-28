/**********************************************************************************
 * $URL: https://source.etudes.org/svn/serenity/trunk/monitor/monitor-api/src/main/java/org/etudes/monitor/api/Sample.java $
 * $Id: Sample.java 12030 2015-11-06 21:09:15Z ggolden $
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

import java.util.Date;
import java.util.HashMap;
import java.util.Map;

import org.etudes.cdp.api.CdpService;
import org.etudes.service.api.Services;

/**
 * Monitor Sample
 */
public class Sample
{
	protected Integer activeQueries = null;
	protected Integer apacheConnections = null;
	protected Boolean appserverMatch = null;
	protected Integer appserverRv = null;
	protected Float appserverTime = null;
	protected Date backup = null;
	protected Integer fsUsedPct = null;
	protected Long id = null;
	protected Boolean jira = null;
	protected Float load = null;
	protected Integer mysqlConnections = null;
	protected Boolean nfs = null;
	protected Date offsite = null;
	protected Integer queries = null;
	protected Date sampledOn = null;
	protected Boolean slave = null;
	protected String source = null;
	protected Boolean svn = null;
	protected Integer totalOpenFiles = null;
	protected Boolean wirisStatus = null;
	protected Boolean wscStatus = null;

	public Integer getActiveQueries()
	{
		return this.activeQueries;
	}

	public Integer getApacheConnections()
	{
		return this.apacheConnections;
	}

	public Boolean getAppserverMatch()
	{
		return this.appserverMatch;
	}

	public Integer getAppserverRv()
	{
		return this.appserverRv;
	}

	public Float getAppserverTime()
	{
		return this.appserverTime;
	}

	public Date getBackupOn()
	{
		return backup;
	}

	public Integer getFsUsedPct()
	{
		return this.fsUsedPct;
	}

	public Long getId()
	{
		return this.id;
	}

	public Boolean getJiraStatus()
	{
		return jira;
	}

	public Float getLoad()
	{
		return load;
	}

	public Integer getMysqlConnections()
	{
		return this.mysqlConnections;
	}

	public Boolean getNfsStatus()
	{
		return nfs;
	}

	public Date getOffsiteOn()
	{
		return offsite;
	}

	public Integer getQueries()
	{
		return queries;
	}

	public Date getSampledOn()
	{
		return this.sampledOn;
	}

	public Boolean getSlaveStatus()
	{
		return slave;
	}

	public String getSource()
	{
		return this.source;
	}

	public Boolean getSvnStatus()
	{
		return svn;
	}

	public Integer getTotalOpenFiles()
	{
		return this.totalOpenFiles;
	}

	public Boolean getWirisStatus()
	{
		return this.wirisStatus;
	}

	public Boolean getWscStatus()
	{
		return this.wscStatus;
	}

	public void read(String prefix, Map<String, Object> parameters)
	{
		setTotalOpenFiles(cdpService().readInt(parameters.get(prefix + "open")));
		setApacheConnections(cdpService().readInt(parameters.get(prefix + "apache")));
		setMysqlConnections(cdpService().readInt(parameters.get(prefix + "mysql")));
		setAppserverRv(cdpService().readInt(parameters.get(prefix + "appserverRv")));
		setAppserverMatch(cdpService().readBoolean(parameters.get(prefix + "appserverMatch")));
		setAppserverTime(cdpService().readFloat(parameters.get(prefix + "appserverTime")));
		setFsUsedPct(cdpService().readInt(parameters.get(prefix + "fsPctUsed")));
		setWscStatus(cdpService().readBoolean(parameters.get(prefix + "wsc")));
		setWirisStatus(cdpService().readBoolean(parameters.get(prefix + "wiris")));
		setJiraStatus(cdpService().readBoolean(parameters.get(prefix + "jira")));
		setSvnStatus(cdpService().readBoolean(parameters.get(prefix + "svn")));
		setNfsStatus(cdpService().readBoolean(parameters.get(prefix + "nfs")));
		setSlaveStatus(cdpService().readBoolean(parameters.get(prefix + "slave")));
		setOffsiteOn(cdpService().readDate(parameters.get(prefix + "offsite")));
		setBackupOn(cdpService().readDate(parameters.get(prefix + "backup")));
		setActiveQueries(cdpService().readInt(parameters.get(prefix + "active")));
		setQueries(cdpService().readInt(parameters.get(prefix + "queries")));
		setLoad(cdpService().readFloat(parameters.get(prefix + "load")));
		setSource(cdpService().readString(parameters.get(prefix + "source")));
		setSampledOn(cdpService().readDate(parameters.get(prefix + "ts")));
	}

	public Map<String, Object> send()
	{
		Map<String, Object> rv = new HashMap<String, Object>();

		rv.put("id", getId());

		rv.put("source", getSource());
		rv.put("open", getTotalOpenFiles());
		rv.put("apache", getApacheConnections());
		rv.put("mysql", getMysqlConnections());
		rv.put("appserverRv", getAppserverRv());
		rv.put("appserverMatch", getAppserverMatch());
		rv.put("appserverTime", getAppserverTime());
		rv.put("fsPctUsed", getFsUsedPct());
		rv.put("wsc", getWscStatus());
		rv.put("wiris", getWirisStatus());
		rv.put("jira", getJiraStatus());
		rv.put("svn", getSvnStatus());
		rv.put("nfs", getNfsStatus());
		rv.put("slave", getSlaveStatus());
		rv.put("offsite", getOffsiteOn());
		rv.put("backup", getBackupOn());
		rv.put("active", getActiveQueries());
		rv.put("queries", getQueries());
		rv.put("load", getLoad());
		rv.put("ts", getSampledOn());

		return rv;
	}

	public void setActiveQueries(Integer count)
	{
		this.activeQueries = count;
	}

	public void setApacheConnections(Integer num)
	{
		this.apacheConnections = num;
	}

	public void setAppserverMatch(Boolean match)
	{
		this.appserverMatch = match;
	}

	public void setAppserverRv(Integer connect)
	{
		this.appserverRv = connect;
	}

	public void setAppserverTime(Float time)
	{
		this.appserverTime = time;
	}

	public void setBackupOn(Date time)
	{
		this.backup = time;
	}

	public void setFsUsedPct(Integer pct)
	{
		this.fsUsedPct = pct;
	}

	public void setId(Long id)
	{
		this.id = id;
	}

	public void setJiraStatus(Boolean status)
	{
		this.jira = status;
	}

	public void setLoad(Float time)
	{
		this.load = time;
	}

	public void setMysqlConnections(Integer num)
	{
		this.mysqlConnections = num;
	}

	public void setNfsStatus(Boolean status)
	{
		this.nfs = status;
	}

	public void setOffsiteOn(Date time)
	{
		this.offsite = time;
	}

	public void setQueries(Integer num)
	{
		this.queries = num;
	}

	public void setSampledOn(Date date)
	{
		this.sampledOn = date;
	}

	public void setSlaveStatus(Boolean status)
	{
		this.slave = status;
	}

	public void setSource(String source)
	{
		this.source = source;
	}

	public void setSvnStatus(Boolean status)
	{
		this.svn = status;
	}

	public void setTotalOpenFiles(Integer num)
	{
		this.totalOpenFiles = num;
	}

	public void setWirisStatus(Boolean status)
	{
		this.wirisStatus = status;
	}

	public void setWscStatus(Boolean status)
	{
		this.wscStatus = status;
	}

	public String toString()
	{
		return "id: " + getId() + " apacheConnections: " + getApacheConnections() + " appserverRv: " + getAppserverRv() + " appserverMatch: "
				+ getAppserverMatch() + " appserverTime: " + getAppserverTime() + " fsUsedPct: " + getFsUsedPct() + " mysqlConnections: "
				+ getMysqlConnections() + " sampledOn: " + getSampledOn() + " source: " + getSource() + " totalOpenFiles: " + getTotalOpenFiles()
				+ " wirisStatus: " + getWirisStatus() + " wscStatus: " + getWscStatus() + " jira: " + getJiraStatus() + " svn: " + getSvnStatus()
				+ " nfs: " + getNfsStatus() + " offsite: " + getOffsiteOn() + " backup: " + getBackupOn() + " slave: " + getSlaveStatus()
				+ " queries: " + getQueries() + " active queries: " + getActiveQueries() + " load: " + getLoad();
	}

	/**
	 * @return The registered CdpService.
	 */
	private CdpService cdpService()
	{
		return (CdpService) Services.get(CdpService.class);
	}
}

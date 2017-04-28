/**********************************************************************************
 * $URL: https://source.etudes.org/svn/serenity/trunk/monitor/monitor-api/src/main/java/org/etudes/monitor/api/Alert.java $
 * $Id: Alert.java 12030 2015-11-06 21:09:15Z ggolden $
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

import java.text.SimpleDateFormat;
import java.util.Date;

public class Alert
{
	public enum AlertType
	{
		absent(1, "No status reports since"), //
		activeQueries(17, "Number active queries"), //
		apacheConnections(2, "Number of apache/tomcat connections"), //
		appserver(3, "CURL from appserver returned"), //
		appserverMatch(4, "Content from appserver as expected"), //
		appserverSlow(5, "Appserver response time"), //
		backup(6, "Last backup"), //
		diskFilling(7, "Diskspace used percent"), //
		load(8, "Load average"), //
		mysqlConnections(9, "Number of mysql connections"), //
		nfs(10, "NFS is running"), //
		offsite(11, "Last offsite backup"), //
		openFiles(12, "Number of open file"), //
		queries(13, "Mysql queries"), //
		slave(14, "DB Slave is running"), //
		wiris(15, "WIRIS is running"), //
		wsc(16, "WSC is running"); //

		protected Integer code = 0;
		protected String description = null;

		private AlertType(int code, String description)
		{
			this.code = Integer.valueOf(code);
			this.description = description;
		}

		public Integer getCode()
		{
			return this.code;
		}

		public String getDescription()
		{
			return this.description;
		}
	}

	protected Date created = null;
	protected String msg = null;
	protected boolean notified = false;
	protected AlertType type = null;

	public Alert(AlertType type, Object msg)
	{
		this.type = type;
		this.msg = (msg == null) ? null : ((msg instanceof Date) ? new SimpleDateFormat("HH:mm:ss").format((Date) msg) : msg.toString());
		this.created = new Date();
	}

	public boolean equals(Object obj)
	{
		if (this == obj) return true;
		if ((obj == null) || (obj.getClass() != this.getClass())) return false;
		if (this.type != ((Alert) obj).getType()) return false;
		return true;
	}

	public Date getCreated()
	{
		return this.created;
	}

	public AlertType getType()
	{
		return this.type;
	}

	public int hashCode()
	{
		return this.type.hashCode();
	}

	public boolean isNotified()
	{
		return this.notified;
	}

	public void setNotified()
	{
		this.notified = true;
	}

	public String toString()
	{
		return new SimpleDateFormat("HH:mm:ss").format(this.created) + " : " + this.type.getDescription() + ": " + this.msg;
	}
}

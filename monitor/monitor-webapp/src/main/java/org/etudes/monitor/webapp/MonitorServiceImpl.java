/**********************************************************************************
 * $URL: https://source.etudes.org/svn/serenity/trunk/monitor/monitor-webapp/src/main/java/org/etudes/monitor/webapp/MonitorServiceImpl.java $
 * $Id: MonitorServiceImpl.java 12553 2016-01-14 20:03:28Z ggolden $
 ***********************************************************************************
 *
 * Copyright (c) 2015, 2016 Etudes, Inc.
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

package org.etudes.monitor.webapp;

import java.sql.ResultSet;
import java.sql.SQLException;
import java.util.ArrayList;
import java.util.Calendar;
import java.util.Collections;
import java.util.Comparator;
import java.util.Date;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;

import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;
import org.etudes.config.api.ConfigService;
import org.etudes.cron.api.CronFrequency;
import org.etudes.cron.api.CronHandler;
import org.etudes.cron.api.RunTime;
import org.etudes.email.api.EmailService;
import org.etudes.monitor.api.Alert;
import org.etudes.monitor.api.Alert.AlertType;
import org.etudes.monitor.api.MonitorService;
import org.etudes.monitor.api.Options;
import org.etudes.monitor.api.Sample;
import org.etudes.roster.api.Membership;
import org.etudes.roster.api.RosterService;
import org.etudes.service.api.Service;
import org.etudes.service.api.Services;
import org.etudes.site.api.SiteService;
import org.etudes.sql.api.SqlService;
import org.etudes.user.api.User;

/**
 * MonitorServiceImpl implements MonitorService.
 */
public class MonitorServiceImpl implements MonitorService, Service, CronHandler
{
	class Status
	{
		protected Set<Alert> alerts = new HashSet<Alert>();

		protected Integer order = null;

		protected Sample sample = null;

		protected String source = null;

		protected String sourceAddress = null;

		public Status(String source, String address, Integer order)
		{
			this.source = source;
			this.sourceAddress = address;
			this.order = order;
		}

		public void addAlert(Alert a)
		{
			// not if we already have one
			for (Alert alert : this.alerts)
			{
				if (alert.getType().equals(a.getType())) return;
			}

			this.alerts.add(a);
		}

		public Set<Alert> getAlerts()
		{
			return this.alerts;
		}

		public Integer getOrder()
		{
			return this.order;
		}

		public Sample getSample()
		{
			return sample;
		}

		public String getSource()
		{
			return this.source;
		}

		public String getSourceAddress()
		{
			return this.sourceAddress;
		}

		public boolean hasAlert(AlertType type)
		{
			for (Alert a : this.alerts)
			{
				if (a.getType().equals(type)) return true;
			}
			return false;

		}

		public boolean isAbsent()
		{
			if (this.sample == null) return true;
			if (new Date().getTime() - this.sample.getSampledOn().getTime() > options.getSinceReport()) return true;

			return false;
		}

		public void removeAlert(AlertType type)
		{
			Set<Alert> newAlerts = new HashSet<Alert>();
			for (Alert a : this.alerts)
			{
				if (!a.getType().equals(type)) newAlerts.add(a);
			}
			this.alerts = newAlerts;
		}

		public void setSample(Sample sample)
		{
			this.sample = sample;
		}
	}

	/** Our log. */
	private static Log M_log = LogFactory.getLog(MonitorServiceImpl.class);

	/** Set to true if the monitor is active on this server. */
	protected boolean active = false;

	/** Options. */
	protected Options options = new Options();

	/** registered servers. */
	protected Map<String, Status> sources = new HashMap<String, Status>();

	/**
	 * Construct
	 */
	public MonitorServiceImpl()
	{
		M_log.info("MonitorServiceImpl: construct");

		// setup to get configured once all services are started
		Services.whenAvailable(ConfigService.class, new Runnable()
		{
			public void run()
			{
				String monitorServer = configService().getString("MonitorService.server");
				String serverName = configService().getString("server");
				active = (serverName.equals(monitorServer));
				if (active)
				{
					String[] sourceNames = configService().getStrings("MonitorService.source");
					String[] sourceAddresses = configService().getStrings("MonitorService.address");

					M_log.info("MonitorServiceImpl: started with " + sourceNames.length + " sources.");

					for (int i = 0; i < sourceNames.length; i++)
					{
						if ((sourceAddresses != null) && (sourceAddresses.length > i))
						{
							sources.put(sourceNames[i], new Status(sourceNames[i], sourceAddresses[i], i));
						}
					}
				}
			}
		});

		Services.whenAvailable(SqlService.class, new Runnable()
		{
			public void run()
			{
				optionsReadTx();
			}
		});
	}

	@Override
	public CronFrequency cronGetFrequency()
	{
		// if we are not active, we never run
		if (!active) return CronFrequency.never;

		// run every minute
		return CronFrequency.minute;
	}

	@Override
	public RunTime[] cronGetRunTimes()
	{
		return null;
	}

	@Override
	public void cronRun()
	{
		// check all sources for unexpected silence, recovery, etc.
		for (Status s : this.sources.values())
		{
			// to sync the periodic checks with the incoming samples
			synchronized (s)
			{
				Set<Alert> currentAlerts = new HashSet<Alert>();
				currentAlerts.addAll(s.getAlerts());

				// detect absence of status from the source
				if (s.isAbsent())
				{
					// if not already notified, notify
					if (!s.hasAlert(AlertType.absent))
					{
						Alert a = new Alert(AlertType.absent, ((s.getSample() == null) ? null : s.getSample().getSampledOn()));
						s.addAlert(a);
					}
				}

				// detect recovery
				else
				{
					if (s.hasAlert(AlertType.absent))
					{
						s.removeAlert(AlertType.absent);
					}
				}

				// notify if alerts changed
				notify(s, currentAlerts);
			}
		}
	}

	@Override
	public List<Alert> getAlerts(String source)
	{
		List<Alert> rv = new ArrayList<Alert>();

		Status s = this.sources.get(source);
		if (s != null)
		{
			rv.addAll(s.getAlerts());
		}

		return rv;
	}

	@Override
	public Options getOptions()
	{
		return this.options;
	}

	@Override
	public List<Sample> getSamples()
	{
		List<Sample> rv = new ArrayList<Sample>();

		for (Status s : this.sources.values())
		{
			if (s.getSample() != null)
			{
				rv.add(s.getSample());
			}
			else
			{
				Sample sample = new Sample();
				sample.setSource(s.getSource());
				rv.add(sample);
			}
		}

		// sort by order found in config
		Collections.sort(rv, new Comparator<Sample>()
		{
			public int compare(Sample s1, Sample s2)
			{
				return sources.get(s1.getSource()).getOrder().compareTo(sources.get(s2.getSource()).getOrder());
			}
		});

		return rv;
	}

	@Override
	public void sample(Sample sample)
	{
		Status s = this.sources.get(sample.getSource());
		if (s != null)
		{
			// to sync the periodic checks with the incoming samples
			synchronized (s)
			{
				Set<Alert> currentAlerts = new HashSet<Alert>();
				currentAlerts.addAll(s.getAlerts());

				// if in alert, notify that we have reconnected
				if (s.hasAlert(AlertType.absent))
				{
					s.removeAlert(AlertType.absent);
				}

				s.setSample(sample);

				// detect immediate trouble
				if (sample.getWirisStatus() != null)
				{
					if (!sample.getWirisStatus())
					{
						Alert a = new Alert(AlertType.wiris, sample.getWirisStatus());
						s.addAlert(a);
					}
					else
					{
						s.removeAlert(AlertType.wiris);
					}
				}

				if (sample.getWscStatus() != null)
				{
					if (!sample.getWscStatus())
					{
						Alert a = new Alert(AlertType.wsc, sample.getWscStatus());
						s.addAlert(a);
					}
					else
					{
						s.removeAlert(AlertType.wsc);
					}
				}

				if (sample.getAppserverRv() != null)
				{
					if (sample.getAppserverRv() != 0)
					{
						Alert a = new Alert(AlertType.appserver, sample.getAppserverRv());
						s.addAlert(a);
					}
					else
					{
						s.removeAlert(AlertType.appserver);
					}
				}

				if (sample.getAppserverMatch() != null)
				{
					if (!sample.getAppserverMatch())
					{
						Alert a = new Alert(AlertType.appserverMatch, sample.getAppserverMatch());
						s.addAlert(a);
					}
					else
					{
						s.removeAlert(AlertType.appserverMatch);
					}
				}

				if (sample.getAppserverTime() != null)
				{
					if (sample.getAppserverTime() > this.options.getAppserverResponse())
					{
						Alert a = new Alert(AlertType.appserverSlow, sample.getAppserverTime());
						s.addAlert(a);
					}
					else
					{
						s.removeAlert(AlertType.appserverSlow);
					}
				}

				if (sample.getMysqlConnections() != null)
				{
					if (sample.getMysqlConnections() > this.options.getOpenMysql())
					{
						Alert a = new Alert(AlertType.mysqlConnections, sample.getMysqlConnections());
						s.addAlert(a);
					}
					else
					{
						s.removeAlert(AlertType.mysqlConnections);
					}
				}

				if (sample.getApacheConnections() != null)
				{
					if (sample.getApacheConnections() > this.options.getOpenApache())
					{
						Alert a = new Alert(AlertType.apacheConnections, sample.getApacheConnections());
						s.addAlert(a);
					}
					else
					{
						s.removeAlert(AlertType.apacheConnections);
					}
				}

				if (sample.getTotalOpenFiles() != null)
				{
					if (sample.getTotalOpenFiles() > this.options.getOpenFiles())
					{
						Alert a = new Alert(AlertType.openFiles, sample.getTotalOpenFiles());
						s.addAlert(a);
					}
					else
					{
						s.removeAlert(AlertType.openFiles);
					}
				}

				if (sample.getFsUsedPct() != null)
				{
					if (sample.getFsUsedPct() > this.options.getDiskUsedPct())
					{
						Alert a = new Alert(AlertType.diskFilling, sample.getFsUsedPct());
						s.addAlert(a);
					}
					else
					{
						s.removeAlert(AlertType.diskFilling);
					}
				}

				if (sample.getLoad() != null)
				{
					if (sample.getLoad() > this.options.getLoadAvg())
					{
						Alert a = new Alert(AlertType.load, sample.getLoad());
						s.addAlert(a);
					}
					else
					{
						s.removeAlert(AlertType.load);
					}
				}

				if (sample.getSlaveStatus() != null)
				{
					if (!sample.getSlaveStatus())
					{
						Alert a = new Alert(AlertType.slave, sample.getSlaveStatus());
						s.addAlert(a);
					}
					else
					{
						s.removeAlert(AlertType.slave);
					}
				}

				if (sample.getNfsStatus() != null)
				{
					if (!sample.getNfsStatus())
					{
						Alert a = new Alert(AlertType.nfs, sample.getNfsStatus());
						s.addAlert(a);
					}
					else
					{
						s.removeAlert(AlertType.nfs);
					}
				}

				if (sample.getQueries() != null)
				{
					if (sample.getQueries() > this.options.getQueriesTotal())
					{
						Alert a = new Alert(AlertType.queries, sample.getQueries());
						s.addAlert(a);
					}
					else
					{
						s.removeAlert(AlertType.queries);
					}
				}

				if (sample.getActiveQueries() != null)
				{
					if (sample.getActiveQueries() > this.options.getQueriesActive())
					{
						Alert a = new Alert(AlertType.activeQueries, sample.getActiveQueries());
						s.addAlert(a);
					}
					else
					{
						s.removeAlert(AlertType.activeQueries);
					}
				}

				if (sample.getBackupOn() != null)
				{
					long threshold = ("olive.etudes.org".equals(sample.getSource()) ? this.options.getSinceFsBackup() : this.options
							.getSinceDbBackup());
					if (new Date().getTime() - sample.getBackupOn().getTime() > threshold)
					{
						Alert a = new Alert(AlertType.backup, sample.getBackupOn());
						s.addAlert(a);
					}
					else
					{
						s.removeAlert(AlertType.backup);
					}
				}

				if (sample.getOffsiteOn() != null)
				{
					if (new Date().getTime() - sample.getBackupOn().getTime() > this.options.getSinceOffsiteBackup())
					{
						Alert a = new Alert(AlertType.offsite, sample.getOffsiteOn());
						s.addAlert(a);
					}
					else
					{
						s.removeAlert(AlertType.offsite);
					}
				}

				notify(s, currentAlerts);
			}
		}
		else
		{
			M_log.warn("sample: unexpected source: " + sample);
		}
	}

	@Override
	public void setOptions(Options options)
	{
		sqlService().transact(new Runnable()
		{
			@Override
			public void run()
			{
				optionsUpdateTx();

			}
		}, "setOptions");
	}

	@Override
	public boolean start()
	{
		M_log.info("MonitorServiceImpl: start");
		return true;
	}

	@Override
	public boolean validSource(String source, String address)
	{
		Status s = this.sources.get(source);
		if (s == null) return false;

		if (!s.getSourceAddress().equals(address)) return false;

		return true;
	}

	protected boolean inBlackout()
	{
		Calendar cal = Calendar.getInstance();
		int hour = cal.get(Calendar.HOUR_OF_DAY);
		int minute = cal.get(Calendar.MINUTE);

		// blackout 4 - 4:30 am to avoid all the kerfuffle of restarts
		if ((hour == 4) && (minute < 30)) return true;

		return false;
	}

	protected void notify(Status s, Set<Alert> currentAlerts)
	{
		// if we are blacked out, just wait...
		if (inBlackout()) return;

		List<Alert> notify = new ArrayList<Alert>();
		List<Alert> revoke = new ArrayList<Alert>();
		List<Alert> others = new ArrayList<Alert>();

		// find new alerts not yet notified
		for (Alert a : s.getAlerts())
		{
			if (!currentAlerts.contains(a) || !a.isNotified())
			{
				notify.add(a);
			}
			else
			{
				others.add(a);
			}
		}

		// find notified alerts now cleared
		for (Alert a : currentAlerts)
		{
			if (!s.getAlerts().contains(a))
			{
				revoke.add(a);
			}
		}

		if (notify.isEmpty() && revoke.isEmpty()) return;

		// notify all users in the roster(s) associated with the special "MONITOR" site
		Membership membership = rosterService().getActiveSiteMembers(siteService().get(SiteService.MONITOR));
		List<User> toUsers = new ArrayList<User>(membership.getUsers());

		String subject = "Monitor Report: " + s.getSource();
		StringBuilder textMessage = new StringBuilder();

		textMessage.append(s.getSource());

		// first line status - no active alerts
		if (notify.isEmpty() && others.isEmpty())
		{
			textMessage.append(": All Clear\n\n");
		}

		// first line status - some active - any new?
		else if (!notify.isEmpty())
		{
			textMessage.append(": ");
			textMessage.append(notify.size());
			if (notify.size() == 1)
			{
				textMessage.append(" New Alert, ");
			}
			else
			{
				textMessage.append(" New Alerts, ");
			}
			textMessage.append((notify.size() + others.size()));
			textMessage.append(" Total\n\n");
		}

		// first line status - some active, none new
		else
		{
			textMessage.append(": ");
			textMessage.append((notify.size() + others.size()));
			if (notify.size() + others.size() == 1)
			{
				textMessage.append(" Total Alert\n\n");
			}
			else
			{
				textMessage.append(" Total Alerts\n\n");
			}
		}

		// new alerts
		if (!notify.isEmpty())
		{
			textMessage.append("New Alerts:\n");
			for (Alert a : notify)
			{
				textMessage.append("\t");
				textMessage.append(s.getSource());
				textMessage.append(": ");
				textMessage.append(a.toString());
				textMessage.append("\n");

				a.setNotified();
			}
			textMessage.append("\n");
		}

		// cleared alerts
		if (!revoke.isEmpty())
		{
			textMessage.append("Cleared Alerts:\n");
			for (Alert a : revoke)
			{
				textMessage.append("\t");
				textMessage.append(s.getSource());
				textMessage.append(": ");
				textMessage.append(a.toString());
				textMessage.append("\n");
			}
			textMessage.append("\n");
		}

		// remaining alerts
		if (!others.isEmpty())
		{
			textMessage.append("Continuing Alerts:\n");
			for (Alert a : others)
			{
				textMessage.append("\t");
				textMessage.append(s.getSource());
				textMessage.append(": ");
				textMessage.append(a.toString());
				textMessage.append("\n");
			}
		}

		String htmlMessage = textMessage.toString().replaceAll("\n", "<br />\n");
		htmlMessage = htmlMessage.replaceAll("\t", "&nbsp;&nbsp;&nbsp;&nbsp;");

		emailService().send(textMessage.toString(), htmlMessage, subject, toUsers);
	}

	/**
	 * Transaction code for reading options
	 */
	protected void optionsReadTx()
	{
		String sql = "SELECT OPEN_FILES, OPEN_APACHE, OPEN_MYSQL, SINCE_REPORT, SINCE_DB_BACKUP, SINCE_FS_BACKUP, SINCE_OFFSITE_BACKUP, LOAD_AVG, DISK_USED_PCT, APPSERVER_RESPONSE, QUERIES_TOTAL, QUERIES_ACTIVE FROM MONITOR_OPTIONS LIMIT 1";
		sqlService().select(sql, null, new SqlService.Reader<Object>()
		{
			@Override
			public Object read(ResultSet result)
			{
				try
				{
					int i = 1;
					options.setOpenFiles(sqlService().readInteger(result, i++));
					options.setOpenApache(sqlService().readInteger(result, i++));
					options.setOpenMysql(sqlService().readInteger(result, i++));
					options.setSinceReport(sqlService().readLong(result, i++));
					options.setSinceDbBackup(sqlService().readLong(result, i++));
					options.setSinceFsBackup(sqlService().readLong(result, i++));
					options.setSinceOffsiteBackup(sqlService().readLong(result, i++));
					options.setLoadAvg(sqlService().readFloat(result, i++));
					options.setDiskUsedPct(sqlService().readInteger(result, i++));
					options.setAppserverResponse(sqlService().readFloat(result, i++));
					options.setQueriesTotal(sqlService().readInteger(result, i++));
					options.setQueriesActive(sqlService().readInteger(result, i++));

					return null;
				}
				catch (SQLException e)
				{
					M_log.warn("optionsReadTx: " + e);
					return null;
				}
			}
		});
	}

	/**
	 * Transaction code for updating options.
	 */
	protected void optionsUpdateTx()
	{
		String sql = "UPDATE MONITOR_OPTIONS SET OPEN_FILES=?, OPEN_APACHE=?, OPEN_MYSQL=?, SINCE_REPORT=?, SINCE_DB_BACKUP=?, SINCE_FS_BACKUP=?, SINCE_OFFSITE_BACKUP=?, LOAD_AVG=?, DISK_USED_PCT=?, APPSERVER_RESPONSE=?, QUERIES_TOTAL=?, QUERIES_ACTIVE=?";

		Object[] fields = new Object[12];
		int i = 0;
		fields[i++] = options.getOpenFiles();
		fields[i++] = options.getOpenApache();
		fields[i++] = options.getOpenMysql();
		fields[i++] = options.getSinceReport();
		fields[i++] = options.getSinceDbBackup();
		fields[i++] = options.getSinceFsBackup();
		fields[i++] = options.getSinceOffsiteBackup();
		fields[i++] = options.getLoadAvg();
		fields[i++] = options.getDiskUsedPct();
		fields[i++] = options.getAppserverResponse();
		fields[i++] = options.getQueriesTotal();
		fields[i++] = options.getQueriesActive();

		sqlService().update(sql, fields);
	}

	/**
	 * @return The registered ConfigService.
	 */
	private ConfigService configService()
	{
		return (ConfigService) Services.get(ConfigService.class);
	}

	/**
	 * @return The registered EmailService.
	 */
	private EmailService emailService()
	{
		return (EmailService) Services.get(EmailService.class);
	}

	/**
	 * @return The registered RosterService.
	 */
	private RosterService rosterService()
	{
		return (RosterService) Services.get(RosterService.class);
	}

	/**
	 * @return The registered SiteService.
	 */
	private SiteService siteService()
	{
		return (SiteService) Services.get(SiteService.class);
	}

	/**
	 * @return The registered SqlService.
	 */
	private SqlService sqlService()
	{
		return (SqlService) Services.get(SqlService.class);
	}
}

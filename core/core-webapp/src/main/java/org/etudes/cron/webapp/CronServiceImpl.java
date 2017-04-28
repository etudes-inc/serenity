/**********************************************************************************
 * $URL: https://source.etudes.org/svn/serenity/trunk/core/core-webapp/src/main/java/org/etudes/cron/webapp/CronServiceImpl.java $
 * $Id: CronServiceImpl.java 8505 2014-08-23 22:37:42Z ggolden $
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

package org.etudes.cron.webapp;

import java.util.Calendar;
import java.util.HashSet;
import java.util.Map;
import java.util.Set;

import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;
import org.etudes.config.api.ConfigService;
import org.etudes.cron.api.CronFrequency;
import org.etudes.cron.api.CronHandler;
import org.etudes.cron.api.CronService;
import org.etudes.cron.api.RunTime;
import org.etudes.service.api.Service;
import org.etudes.service.api.Services;
import org.etudes.threadlocal.api.ThreadLocalService;
import org.etudes.tool.api.Tool;

public class CronServiceImpl implements CronService, Service, Runnable
{
	/** Our log. */
	private static Log M_log = LogFactory.getLog(CronServiceImpl.class);

	/** Maintenance thread. */
	protected Thread maintenanceThread = null;

	/** Signal to the maintenance thread to stop. */
	protected boolean stopMaintenanceThread = false;

	/**
	 * Construct
	 */
	public CronServiceImpl()
	{
		// if we are on the proper server, get the maintenance thread running
		Services.whenStarted(new Runnable()
		{
			public void run()
			{
				String cronServer = configService().getString("CronService.server");
				String serverName = configService().getString("server");
				if (serverName.equals(cronServer))
				{
					startMaintenance();
					M_log.info("CronServiceImpl()[whenStarted]: starting cron");
				}
			}
		});

		M_log.info("CronServiceImpl: construct");
	}

	@Override
	public void run()
	{
		long tick = 0;
		// which jobs we have just recently run - keyed by Handler & RunTime
		Set<String> jobsRecentlyRun = new HashSet<String>();

		while ((!this.stopMaintenanceThread) && (!Thread.currentThread().isInterrupted()))
		{
			// Note: delay one cycle before starting to run, so that other "when started" jobs have a chance to first run, in case they setup for their cron usage

			// return in a minute
			try
			{
				Thread.sleep(60000);
			}
			catch (Exception ignore)
			{
			}

			Map<Tool, Service> handlers = Services.getHandlers(CronHandler.class);
			if (handlers != null)
			{
				for (Service s : handlers.values())
				{
					if (s instanceof CronHandler)
					{
						// if this handler wants to be run now, spawn a thread for it
						final CronHandler h = ((CronHandler) s);
						CronFrequency frequency = h.cronGetFrequency();

						// for runtime
						if (CronFrequency.runTime == frequency)
						{
							RunTime[] times = h.cronGetRunTimes();
							if ((times != null) && (times.length > 0))
							{
								RunTime now = runTime();

								// run if any of the times are satisfied by now
								boolean run = false;
								for (RunTime t : times)
								{
									String key = h.getClass().getName() + t.toString();
									if (t.isSatisfiedBy(now))
									{
										// if we have not recently ran this, run it now
										if (!jobsRecentlyRun.contains(key))
										{
											run = true;
											jobsRecentlyRun.add(key);
											break;
										}
									}

									// if not satisfied by now (with the grace period), remove any note that we recently ran this
									else
									{
										jobsRecentlyRun.remove(key);
									}
								}

								if (run)
								{
									runJob(new Runnable()
									{
										public void run()
										{
											try
											{
												h.cronRun();
											}
											catch (Exception e)
											{
												M_log.warn(h.getClass().getName() + ": cron: ", e);
											}
										}
									});
								}
							}
						}

						// for periodic
						else if ((frequency.getMins() > 0) && (tick % frequency.getMins() == 0))
						{
							runJob(new Runnable()
							{
								public void run()
								{
									try
									{
										h.cronRun();
									}
									catch (Exception e)
									{
										M_log.warn(h.getClass().getName() + ": cron: ", e);
									}
								}
							});
						}
					}
				}
			}

			// count another minute / cycle
			tick++;
		}
	}

	@Override
	public void runJob(final Runnable job)
	{
		// TODO: cache threads ?
		Thread thread = new Thread(new Runnable()
		{
			public void run()
			{
				try
				{
					job.run();
				}
				catch (Exception e)
				{
					M_log.warn("runJob: ", e);
				}
				finally
				{
					// clear any bound current values
					threadLocalService().clear();
				}
			}
		});

		thread.setDaemon(true);
		thread.start();
	}

	@Override
	public RunTime runTime()
	{
		// based on default / server TZ
		Calendar now = Calendar.getInstance();
		RunTimeImpl rv = new RunTimeImpl(now.get(Calendar.HOUR_OF_DAY), now.get(Calendar.MINUTE));

		return rv;
	}

	@Override
	public RunTime runTime(int hourOfDay, int minuteOfHour)
	{
		RunTime rv = new RunTimeImpl(hourOfDay, minuteOfHour);
		return rv;
	}

	@Override
	public RunTime runTime(String runtime)
	{
		RunTime rv = new RunTimeImpl(runtime);
		return rv;
	}

	@Override
	public boolean start()
	{
		M_log.info("CronServiceImpl: start");
		return true;
	}

	/**
	 * Start the cron maintenance thread.
	 */
	protected void startMaintenance()
	{
		this.stopMaintenanceThread = false;
		this.maintenanceThread = new Thread(this, getClass().getName());
		this.maintenanceThread.setDaemon(true);
		this.maintenanceThread.start();
	}

	/**
	 * Signal the cron maintenance thread to stop.
	 */
	protected void stopMaintenance()
	{
		if (this.maintenanceThread == null) return;

		this.stopMaintenanceThread = true;
		this.maintenanceThread.interrupt();
		this.maintenanceThread = null;
	}

	/**
	 * @return The registered ConfigService.
	 */
	private ConfigService configService()
	{
		return (ConfigService) Services.get(ConfigService.class);
	}

	/**
	 * @return The registered ThreadLocalService.
	 */
	private ThreadLocalService threadLocalService()
	{
		return (ThreadLocalService) Services.get(ThreadLocalService.class);
	}
}

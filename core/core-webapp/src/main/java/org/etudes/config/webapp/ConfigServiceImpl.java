/**********************************************************************************
 * $URL: https://source.etudes.org/svn/serenity/trunk/core/core-webapp/src/main/java/org/etudes/config/webapp/ConfigServiceImpl.java $
 * $Id: ConfigServiceImpl.java 11537 2015-09-01 21:12:54Z ggolden $
 ***********************************************************************************
 *
 * Copyright (c) 2014, 2015 Etudes, Inc.
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

package org.etudes.config.webapp;

import java.io.File;
import java.io.FileInputStream;
import java.io.FileNotFoundException;
import java.io.IOException;
import java.util.Properties;

import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;
import org.etudes.config.api.ConfigService;
import org.etudes.service.api.Service;

/**
 * ConfigServiceImpl implements ConfigService
 */
public class ConfigServiceImpl implements ConfigService, Service
{
	/** Our log. */
	private static Log M_log = LogFactory.getLog(ConfigServiceImpl.class);

	protected Properties config = new Properties();

	protected String homePath = null;

	/**
	 * Construct
	 */
	public ConfigServiceImpl()
	{
		M_log.info("ConfigServiceImpl: construct");

		// get the "home" where we find the configuration files
		this.homePath = System.getProperty("serenity.config");

		// load up the configuration files - the base one first
		File configFile = new File(homePath + "serenity.properties");
		if (configFile.exists())
		{
			try
			{
				this.config.load(new FileInputStream(configFile));
			}
			catch (FileNotFoundException e)
			{
			}
			catch (IOException e)
			{
			}
		}

		// add configuration from the service level configuration file (i.e. dev, staging, or production)
		configFile = new File(homePath + "service.properties");
		if (configFile.exists())
		{
			try
			{
				this.config.load(new FileInputStream(configFile));
			}
			catch (FileNotFoundException e)
			{
			}
			catch (IOException e)
			{
			}
		}

		// add configuration from the server level configuration file (i.e. the app server)
		configFile = new File(homePath + "server.properties");
		if (configFile.exists())
		{
			try
			{
				this.config.load(new FileInputStream(configFile));
			}
			catch (FileNotFoundException e)
			{
			}
			catch (IOException e)
			{
			}
		}

		M_log.info("ConfigServiceImpl: home: " + this.homePath + " " + this.config);
	}

	@Override
	public boolean getBoolean(String name, boolean valueIfMissing)
	{
		boolean rv = valueIfMissing;
		String value = getString(name);
		if (value != null)
		{
			rv = Boolean.parseBoolean(value);
		}

		return rv;
	}

	@Override
	public int getInt(String name, int valueIfMissing)
	{
		int rv = valueIfMissing;
		String value = getString(name);
		if (value != null)
		{
			try
			{
				rv = Integer.parseInt(value);
			}
			catch (NumberFormatException e)
			{
			}
		}

		return rv;
	}

	@Override
	public long getLong(String name, long valueIfMissing)
	{
		long rv = valueIfMissing;
		String value = getString(name);
		if (value != null)
		{
			try
			{
				rv = Long.parseLong(value);
			}
			catch (NumberFormatException e)
			{
			}
		}

		return rv;
	}

	@Override
	public String getString(String name)
	{
		return this.config.getProperty(name);
	}

	@Override
	public String getString(String name, String valueIfMissing)
	{
		return this.config.getProperty(name, valueIfMissing);
	}

	@Override
	public String[] getStrings(String name)
	{
		// the configuration item named <name>.count has how many items to look for. Each item is named <name>.#
		int numItems = getInt(name + ".count", 0);
		if (numItems > 0)
		{
			String[] rv = new String[numItems];
			for (int i = 1; i <= numItems; i++)
			{
				rv[i - 1] = getString(name + "." + i, "");
			}
			return rv;
		}

		return null;
	}

	@Override
	public boolean start()
	{
		M_log.info("ConfigServiceImpl: start");
		return true;
	}
}

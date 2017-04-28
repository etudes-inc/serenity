/**********************************************************************************
 * $URL: https://source.etudes.org/svn/serenity/trunk/core/core-api/src/main/java/org/etudes/service/api/Services.java $
 * $Id: Services.java 8354 2014-07-14 00:25:39Z ggolden $
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

package org.etudes.service.api;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;
import org.etudes.tool.api.Tool;

/**
 * Static class used by all Etudes to register and discover services.
 */
@SuppressWarnings("rawtypes")
public class Services
{
	/** Map a handler class to the services that registered to handle it. */
	protected static Map<Class, Map<Tool, Service>> handlers = new HashMap<Class, Map<Tool, Service>>();

	/** Map of registered services. */
	protected static Map<Class, Service> services = new HashMap<Class, Service>();

	/** Set to true once started. */
	protected static boolean started = false;

	/** Any Runnables that need to be run once a component shows up - mapped by component interface class name. */
	protected static Map<String, List<Runnable>> toRuns = new HashMap<String, List<Runnable>>();

	/** Any Runnables that need to be run once all components are registered and started. */
	protected static List<Runnable> toStart = new ArrayList<Runnable>();

	/** Our log. */
	private static Log M_log = LogFactory.getLog(Services.class);

	/**
	 * Discover a service by API class.
	 * 
	 * @param clazz
	 *        The API class.
	 * @return The service, or null if not found.
	 */
	public static Service get(Class clazz)
	{
		return services.get(clazz);
	}

	/**
	 * Get the Service that is registered for this tool for this handler interface.
	 * 
	 * @param handlerClass
	 *        The handler interface class.
	 * @param tool
	 *        The tool under which the service is registered.
	 * @return The service, or null if none registered.
	 */
	public static Service getHandler(Class handlerClass, Tool tool)
	{
		Map<Tool, Service> h = handlers.get(handlerClass);
		if (h != null)
		{
			Service rv = h.get(tool);
			return rv;
		}

		return null;
	}

	/**
	 * Get all the services that registered for this handler interface.
	 * 
	 * @param handlerClass
	 *        The handler interface (class).
	 * @return The list of Service object that registered for this handler interface. May be empty or null.
	 */
	public static Map<Tool, Service> getHandlers(Class handlerClass)
	{
		return handlers.get(handlerClass);
	}

	/**
	 * Register a service implementation of this API.
	 * 
	 * @param clazz
	 *        The service API class.
	 * @param service
	 *        The service implementation.
	 * @param tool
	 *        The Tool to use to register the handlers.
	 * @param handlerClasses
	 *        Any number of interfaces that the service implements and wants to register for.
	 */
	public static void register(Class clazz, Service service, Tool tool, Class... handlerClasses)
	{
		services.put(clazz, service);

		// register for any of the handler classes
		for (Class c : handlerClasses)
		{
			Map<Tool, Service> handlerMap = handlers.get(c);
			if (handlerMap == null)
			{
				handlerMap = new HashMap<Tool, Service>();
				handlers.put(c, handlerMap);
			}
			handlerMap.put(tool, service);
		}

		// run any Runnables waiting
		List<Runnable> toRun = toRuns.get(clazz.getName());
		if (toRun != null)
		{
			toRuns.remove(clazz.getName());

			for (Runnable run : toRun)
			{
				run.run();
			}
		}
	}

	/**
	 * Remove the service implementation for this API.
	 * 
	 * @param claszz
	 *        The service API.
	 * @param tool
	 *        The Tool to use to unregister the handlers.
	 */
	public static void remove(Class claszz, Tool tool)
	{
		Service s = services.remove(claszz);
		if (s != null)
		{
			for (Map<Tool, Service> handlerMap : handlers.values())
			{
				// if the handler for this interface for this tool is this service, remove it
				if (handlerMap.get(tool) == s)
				{
					handlerMap.remove(tool);
				}
			}
		}
	}

	/**
	 * Run the start() method of all registered services, and all toStart runnables.
	 */
	public static void start()
	{
		M_log.info("start()");

		for (Service s : services.values())
		{
			s.start();
		}

		for (Runnable r : toStart)
		{
			r.run();
		}

		toStart.clear();
		started = true;
	}

	/**
	 * A Runnable to run once the service identified by the class is registered. Will run now if that service already exists.
	 * 
	 * @param clazz
	 *        The service API class.
	 * @param run
	 *        The Runnable to run,
	 */
	public static void whenAvailable(Class clazz, Runnable run)
	{
		// if we have it now, run it now
		if (get(clazz) != null)
		{
			run.run();
		}
		else
		{
			// otherwise save it for when it becomes registered
			List<Runnable> toRun = toRuns.get(clazz.getName());
			if (toRun == null)
			{
				toRun = new ArrayList<Runnable>();
				toRuns.put(clazz.getName(), toRun);
			}
			toRun.add(run);
		}
	}

	/**
	 * Register a Runnable to run once all services are registered and started.
	 * 
	 * @param run
	 *        The Runnable.
	 */
	public static void whenStarted(Runnable run)
	{
		if (started)
		{
			run.run();
		}
		else
		{
			toStart.add(run);
		}
	}
}

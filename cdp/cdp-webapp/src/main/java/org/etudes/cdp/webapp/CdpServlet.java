/**********************************************************************************
 * $URL: https://source.etudes.org/svn/serenity/trunk/cdp/cdp-webapp/src/main/java/org/etudes/cdp/webapp/CdpServlet.java $
 * $Id: CdpServlet.java 11790 2015-10-07 19:54:23Z ggolden $
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

package org.etudes.cdp.webapp;

import static org.apache.commons.lang3.StringEscapeUtils.escapeHtml4;
import static org.etudes.util.Cookies.cookieRead;
import static org.etudes.util.Cookies.cookieRemove;
import static org.etudes.util.Cookies.cookieWrite;

import java.io.IOException;
import java.io.PrintWriter;
import java.io.UnsupportedEncodingException;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import javax.servlet.ServletConfig;
import javax.servlet.ServletException;
import javax.servlet.http.Cookie;
import javax.servlet.http.HttpServlet;
import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;

import org.apache.commons.fileupload.FileItem;
import org.apache.commons.fileupload.FileItemFactory;
import org.apache.commons.fileupload.FileUploadException;
import org.apache.commons.fileupload.disk.DiskFileItemFactory;
import org.apache.commons.fileupload.servlet.ServletFileUpload;
import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;
import org.codehaus.jackson.JsonGenerationException;
import org.codehaus.jackson.map.JsonMappingException;
import org.codehaus.jackson.map.ObjectMapper;
import org.etudes.authentication.api.AuthenticationService;
import org.etudes.cdp.api.CdpHandler;
import org.etudes.cdp.api.CdpService;
import org.etudes.cdp.api.CdpStatus;
import org.etudes.config.api.ConfigService;
import org.etudes.email.api.EmailService;
import org.etudes.service.api.Services;
import org.etudes.site.api.SiteService;
import org.etudes.threadlocal.api.ThreadLocalService;
import org.etudes.tracking.api.TrackingService;
import org.etudes.user.api.User;
import org.etudes.user.api.UserService;

/**
 * Lifecycle container servlet for E3: cdp, also /cdp request handling servlet
 */
@SuppressWarnings("serial")
public class CdpServlet extends HttpServlet
{
	private static final String AUTH_TOKEN = "JSESSIONID"; // "EtudesToken", someday!  Coordinate with serenity.js, ConnectorServlet, DownloadServlet

	/** Separator character in requests for compound requests between the separate request paths. */
	private static final String COMPOUND = " ";

	/** Our log. */
	private static Log M_log = LogFactory.getLog(CdpServlet.class);

	/**
	 * Shutdown the servlet.
	 */
	public void destroy()
	{
		M_log.info("destroy()");
		super.destroy();
	}

	/**
	 * Access the Servlet's information display.
	 * 
	 * @return servlet information.
	 */
	public String getServletInfo()
	{
		return "CdpServlet";
	}

	/**
	 * Initialize the servlet.
	 * 
	 * @param config
	 *        The servlet config.
	 * @throws ServletException
	 */
	public void init(ServletConfig config) throws ServletException
	{
		super.init(config);
	}

	/**
	 * Dispatch the request based on the path
	 * 
	 * @param req
	 * @param res
	 * @param parameters
	 * @param path
	 * @throws ServletException
	 * @throws IOException
	 */
	protected void dispatch(HttpServletRequest req, HttpServletResponse res, Map<String, Object> parameters, String path, User authenticatedUser)
			throws ServletException, IOException
	{
		// all responses send a JSON map, 200 status response. The real response status is in the map in the "cdp:status" entry.
		Map<String, Object> responseMap = null;

		String userAgent = req.getHeader("user-agent");

		String[] pathComponents = path.split("/");
		if (pathComponents.length > 1)
		{
			responseMap = new HashMap<String, Object>();

			// split compound requests
			String[] requests = pathComponents[1].split(COMPOUND);
			boolean gotOne = false;

			for (String request : requests)
			{
				// check for a prefixed request
				if (request.indexOf("_") != -1)
				{
					String[] prefixRequest = request.split("_");

					// get the handler for the prefix
					CdpHandler handler = cdpService().getCdpHandler(prefixRequest[0]);
					if (handler != null)
					{
						// handle it
						Map<String, Object> rm = handler.handle(req, res, parameters, prefixRequest[1], path, authenticatedUser);
						if (rm != null)
						{
							responseMap.putAll(rm);
							gotOne = true;
						}
					}
				}

				else
				{
					if (request.equals("login"))
					{
						authenticatedUser = dispatchLogin(req, res, parameters, path, responseMap);
						gotOne = true;
					}
					else if (request.equals("logout"))
					{
						Map<String, Object> rm = dispatchLogout(req, res, parameters, path);
						if (rm != null) responseMap.putAll(rm);
						authenticatedUser = null;
						gotOne = true;
					}
					else if (request.equals("checkAuth"))
					{
						Map<String, Object> rm = dispatchCheckAuth(req, res, parameters, path, authenticatedUser);
						if (rm != null) responseMap.putAll(rm);
						gotOne = true;
					}
				}
			}

			if (!gotOne)
			{
				responseMap = dispatchError(req, res, parameters, path);
			}
		}
		else
		{
			responseMap = dispatchError(req, res, parameters, path);
		}

		// IE9 does not like "application/json"... but inTouch requires it!
		String contentType = "text/plain";
		if (userAgent.startsWith("inTouch")) contentType = "application/json";

		// send the JSON response
		String response = formatResponse(responseMap);
		// M_log.info("request: " + path + " response: " + response);
		res.setContentType(contentType);
		res.setCharacterEncoding("UTF-8");
		PrintWriter out = res.getWriter();
		out.print(response);
	}

	protected Map<String, Object> dispatchCheckAuth(HttpServletRequest req, HttpServletResponse res, Map<String, Object> parameters, String path,
			User authenticated) throws ServletException, IOException
	{
		Map<String, Object> rv = new HashMap<String, Object>();

		if (authenticated == null)
		{
			// clear the cookie
			cookieRemove(res, AUTH_TOKEN);

			rv.put(CdpStatus.CDP_STATUS, CdpStatus.notLoggedIn.getId());
			return rv;
		}

		rv.put(CdpStatus.CDP_STATUS, CdpStatus.success.getId());

		return rv;
	}

	/**
	 * Dispatch the error response
	 * 
	 * @param req
	 * @param res
	 * @param parameters
	 * @param path
	 * @return
	 * @throws ServletException
	 * @throws IOException
	 */
	protected Map<String, Object> dispatchError(HttpServletRequest req, HttpServletResponse res, Map<String, Object> parameters, String path)
			throws ServletException, IOException
	{
		Map<String, Object> rv = new HashMap<String, Object>();
		rv.put(CdpStatus.CDP_STATUS, CdpStatus.badRequest.getId());

		return rv;
	}

	protected User dispatchLogin(HttpServletRequest req, HttpServletResponse res, Map<String, Object> parameters, String path, Map<String, Object> rv)
			throws ServletException, IOException
	{
		String userId = (String) parameters.get("userid");
		String password = (String) parameters.get("password");

		List<User> users = userService().findByEid(userId);
		User authenticated = null;
		boolean multiple = false;
		for (User user : users)
		{
			if (user.checkPassword(password))
			{
				if (authenticated == null)
				{
					authenticated = user;
				}
				else
				{
					authenticated = null;
					multiple = true;
					break;
				}
			}
		}

		if ((!multiple) && (authenticated != null))
		{
			// add status parameter
			rv.put(CdpStatus.CDP_STATUS, CdpStatus.success.getId());

			// get an authentication token for this user
			String token = authenticationService().createAuthenticationToken(authenticated, req.getRemoteAddr(), req.getHeader("user-agent"));

			// track the service visit
			trackingService().track(authenticated);

			// write our cookie
			cookieWrite(res, AUTH_TOKEN, token);
		}

		else
		{
			authenticated = null;

			// indicate no good
			rv.put(CdpStatus.CDP_STATUS, CdpStatus.accessDenied.getId());

			// clear the cookie
			cookieRemove(res, AUTH_TOKEN);
		}

		return authenticated;
	}

	protected Map<String, Object> dispatchLogout(HttpServletRequest req, HttpServletResponse res, Map<String, Object> parameters, String path)
			throws ServletException, IOException
	{
		Map<String, Object> rv = new HashMap<String, Object>();

		// get the cookie to read the token
		Cookie tokenCookie = cookieRead(req, AUTH_TOKEN);
		if (tokenCookie != null)
		{
			// cancel the authentication
			authenticationService().cancelAuthenticationToken(tokenCookie.getValue());

			// add status parameter
			rv.put(CdpStatus.CDP_STATUS, CdpStatus.success.getId());
		}
		else
		{
			// indicate there was no login to logout from
			rv.put(CdpStatus.CDP_STATUS, CdpStatus.notLoggedIn.getId());
		}

		// clear the cookie
		cookieRemove(res, AUTH_TOKEN);

		return rv;
	}

	protected Map<String, Object> dispatchTest(HttpServletRequest req, HttpServletResponse res, Map<String, Object> parameters, String path)
			throws ServletException, IOException
	{
		Map<String, Object> rv = new HashMap<String, Object>();

		for (String key : parameters.keySet())
		{
			Object value = parameters.get(key);
			M_log.warn("dispatchTest - parameters: key: " + key + " value: " + value.toString());
			rv.put(key, value);
		}

		rv.put(CdpStatus.CDP_STATUS, CdpStatus.success.getId());

		try
		{
			Thread.sleep(10000);
		}
		catch (InterruptedException e)
		{
		}

		return rv;
	}

	/**
	 * Respond to CDP GET requests.
	 * 
	 * @param req
	 *        The servlet request.
	 * @param res
	 *        The servlet response.
	 * @throws ServletException.
	 * @throws IOException.
	 */
	protected void doGet(HttpServletRequest req, HttpServletResponse res) throws ServletException, IOException
	{
		Cookie tokenCookie = cookieRead(req, AUTH_TOKEN);
		if (tokenCookie != null)
		{
			/* User u = */authenticationService().authenticateByToken(tokenCookie.getValue(), req.getRemoteAddr());
		}

		// Note: we might want to break this out to another webapp... /docs or something
		try
		{
			// TODO: dispatchDoc(req, res);
		}
		catch (Exception e)
		{
			M_log.warn("doGet: ", e);
			res.sendError(HttpServletResponse.SC_INTERNAL_SERVER_ERROR);
		}
		finally
		{
			// clear all threadlocal values to prepare the thread for reuse
			threadLocalService().clear();
		}
	}

	/**
	 * Respond to requests.
	 * 
	 * @param req
	 *        The servlet request.
	 * @param res
	 *        The servlet response.
	 * @throws ServletException.
	 * @throws IOException.
	 */
	protected void doPost(HttpServletRequest req, HttpServletResponse res) throws ServletException, IOException
	{
		try
		{
			User authenticatedUser = null;
			Cookie tokenCookie = cookieRead(req, AUTH_TOKEN);

			// handle the post body
			Map<String, Object> parameters = processBody(req);

			if (tokenCookie != null)
			{
				authenticatedUser = authenticationService().authenticateByToken(tokenCookie.getValue(), req.getRemoteAddr());
			}

			// check for error report
			String errorReport = (String) parameters.get("cdp:error");
			if (errorReport != null)
			{
				reportError(authenticatedUser, errorReport, tokenCookie, req);
			}

			// dispatch the request based on path
			String path = req.getPathInfo();
			dispatch(req, res, parameters, path, authenticatedUser);
		}
		catch (Exception e)
		{
			M_log.warn("doPost:", e);
		}
		finally
		{
			// clear all threadlocal values to prepare the thread for reuse
			threadLocalService().clear();
		}
	}

	/**
	 * Format the parameter map into a client JSON response string.
	 * 
	 * @param parameters
	 *        The parameter map.
	 * @return The client JSON response string.
	 */
	protected String formatResponse(Map<String, Object> parameters)
	{
		ObjectMapper mapper = new ObjectMapper();

		String rv = null;
		try
		{
			rv = mapper.writeValueAsString(parameters);
		}
		catch (JsonGenerationException e)
		{
			M_log.warn("formatResponse: exception:" + e);
		}
		catch (JsonMappingException e)
		{
			M_log.warn("formatResponse: exception:" + e);
		}
		catch (IOException e)
		{
			M_log.warn("formatResponse: exception:" + e);
		}

		return rv;
	}

	protected void mergeParameter(Map<String, Object> parameters, String name, String value)
	{
		// merge into our map of key / values
		Object current = parameters.get(name);

		// if not there, start with the value
		if (current == null)
		{
			parameters.put(name, value);
		}

		// if we find a value, change it to an array containing both
		else if (current instanceof String)
		{
			String[] values = new String[2];
			values[0] = (String) current;
			values[1] = value;
			parameters.put(name, values);
		}

		// if an array is found, extend our current values to include this additional one
		else if (current instanceof String[])
		{
			String[] currentArray = (String[]) current;
			String[] values = new String[currentArray.length + 1];
			System.arraycopy(currentArray, 0, values, 0, currentArray.length);
			values[currentArray.length] = value;
			parameters.put(name, values);
		}
	}

	/**
	 * Process the request URL parameters and post body data to form a map of all parameters
	 * 
	 * @param req
	 *        The request.
	 * @return The map of parameters: keyed by parameter name, values either String, String[], or (later) file.
	 */
	@SuppressWarnings("unchecked")
	protected Map<String, Object> processBody(HttpServletRequest req)
	{
		// keyed by name, value can be String, String[], or (later) a file
		Map<String, Object> rv = new HashMap<String, Object>();

		// Create a factory for disk-based file items
		FileItemFactory factory = new DiskFileItemFactory();

		// Create a new file upload handler
		ServletFileUpload upload = new ServletFileUpload(factory);

		// Parse the request
		try
		{
			List<FileItem> items = upload.parseRequest(req);
			for (FileItem item : items)
			{
				if (item.isFormField())
				{
					// the name
					String name = item.getFieldName();

					// the value
					String value = item.getString("UTF-8");

					mergeParameter(rv, name, value);
				}
				else
				{
					rv.put(item.getFieldName(), item);
				}
			}
		}
		catch (FileUploadException e)
		{
			M_log.warn("processBody: exception:" + e);
		}
		catch (UnsupportedEncodingException e)
		{
			M_log.warn("processBody: exception:" + e);
		}

		// add URL parameters
		Map<String, Object> urlParameterMap = req.getParameterMap();
		for (String name : urlParameterMap.keySet())
		{
			String[] values = req.getParameterValues(name);
			if (values != null)
			{
				for (String value : values)
				{
					mergeParameter(rv, name, value);
				}
			}
		}

		return rv;
	}

	protected void reportError(User authenticatedUser, String errorReport, Cookie tokenCookie, HttpServletRequest req)
	{
		StringBuilder textMessage = new StringBuilder();
		StringBuilder htmlMessage = new StringBuilder();

		htmlMessage.append("<style>tr td:first-child{text-align:right; padding-right: 8px;}</style><table>");

		// user info
		if (authenticatedUser != null)
		{
			textMessage.append("User: " + authenticatedUser.getNameSort() + "\n");
			htmlMessage.append("<tr><td>User:</td><td>" + authenticatedUser.getNameSort() + "</td></tr>");

			textMessage.append("EID: " + authenticatedUser.getEid() + "\n");
			htmlMessage.append("<tr><td>EID:</td><td>" + authenticatedUser.getEid() + "</td></tr>");

			textMessage.append("IID: " + authenticatedUser.getIidDisplay() + "\n");
			htmlMessage.append("<tr><td>IID:</td><td>" + authenticatedUser.getIidDisplay() + "</td></tr>");

			textMessage.append("ID: " + authenticatedUser.getId() + "\n");
			htmlMessage.append("<tr><td>ID:</td><td>" + authenticatedUser.getId() + "</td></tr>");

			textMessage.append("Email: " + authenticatedUser.getEmailOfficial() + "\n\n");
			htmlMessage.append("<tr><td>Email:</td><td>" + authenticatedUser.getEmailOfficial() + "</td></tr>");
		}
		else
		{
			textMessage.append("User: none\n");
			htmlMessage.append("<tr><td>User:</td><td>none</td></tr>");
		}

		// request
		textMessage.append("IP: " + req.getRemoteAddr() + "\n");
		htmlMessage.append("<tr><td>IP:</td><td>" + req.getRemoteAddr() + "</td></tr>");

		if (tokenCookie != null)
		{
			textMessage.append("Authentication: " + tokenCookie.getValue() + "\n");
			htmlMessage.append("<tr><td>Authentication:</td><td>" + tokenCookie.getValue() + "</td></tr>");
		}

		textMessage.append("Browser: " + req.getHeader("user-agent") + "\n\n");
		htmlMessage.append("<tr><td>Browser:</td><td>" + escapeHtml4(req.getHeader("user-agent")) + "</td></tr>");

		// server
		textMessage.append("Server: " + configService().getString("server") + "\n");
		htmlMessage.append("<tr><td>Server:</td><td>" + configService().getString("server") + "</td></tr>");

		textMessage.append("Service: " + configService().getString("service") + "\n\n");
		htmlMessage.append("<tr><td>Service:</td><td>" + configService().getString("service") + "</td></tr>");

		// title
		textMessage.append("\nBrowser Error Report\n\n");
		htmlMessage.append("<tr><td></td><td><hr /></td></tr><tr><td></td><td>Browser Error Report</td></tr>");

		htmlMessage.append("</table>");

		// error
		textMessage.append(errorReport);
		htmlMessage.append("<pre>" + escapeHtml4(errorReport) + "</pre>");

		List<User> toUsers = new ArrayList<User>();
		toUsers.add(userService().get(UserService.ADMIN));
		emailService().send(textMessage.toString(), htmlMessage.toString(), "Bug Report", toUsers);
	}

	/**
	 * @return The registered AuthenticationService.
	 */
	private AuthenticationService authenticationService()
	{
		return (AuthenticationService) Services.get(AuthenticationService.class);
	}

	/**
	 * @return The registered CdpService.
	 */
	private CdpService cdpService()
	{
		return (CdpService) Services.get(CdpService.class);
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
	 * @return The registered SiteService.
	 */
	private SiteService siteService()
	{
		return (SiteService) Services.get(SiteService.class);
	}

	/**
	 * @return The registered ThreadLocalService.
	 */
	private ThreadLocalService threadLocalService()
	{
		return (ThreadLocalService) Services.get(ThreadLocalService.class);
	}

	/**
	 * @return The registered TrackingService.
	 */
	private TrackingService trackingService()
	{
		return (TrackingService) Services.get(TrackingService.class);
	}

	/**
	 * @return The registered UserService.
	 */
	private UserService userService()
	{
		return (UserService) Services.get(UserService.class);
	}
}

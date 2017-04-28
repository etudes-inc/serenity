/**********************************************************************************
 * $URL: https://source.etudes.org/svn/serenity/trunk/download/download-webapp/src/main/java/org/etudes/download/webapp/DownloadServlet.java $
 * $Id: DownloadServlet.java 11790 2015-10-07 19:54:23Z ggolden $
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

package org.etudes.download.webapp;

import static org.etudes.util.Cookies.cookieRead;
import static org.etudes.util.StringUtil.split;

import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;
import java.io.PrintWriter;
import java.util.List;

import javax.servlet.ServletConfig;
import javax.servlet.ServletException;
import javax.servlet.http.Cookie;
import javax.servlet.http.HttpServlet;
import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;

import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;
import org.etudes.authentication.api.AuthenticationService;
import org.etudes.download.api.DownloadHandler;
import org.etudes.file.api.File;
import org.etudes.file.api.FileService;
import org.etudes.file.api.Reference;
import org.etudes.roster.api.Role;
import org.etudes.roster.api.RosterService;
import org.etudes.service.api.Service;
import org.etudes.service.api.Services;
import org.etudes.threadlocal.api.ThreadLocalService;
import org.etudes.user.api.User;
import org.etudes.user.api.UserService;

/**
 * The AccessServlet servlet fields get requests for site resources, either directly or embedded in content.
 */
@SuppressWarnings("serial")
public class DownloadServlet extends HttpServlet
{
	/** The chunk size used when streaming (100k). */
	protected static final int STREAM_BUFFER_SIZE = 102400;

	private static final String AUTH_TOKEN = "JSESSIONID";

	/** Our log. */
	private static Log M_log = LogFactory.getLog(DownloadServlet.class);

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
		return "Download";
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
	 * Respond to requests.
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
		// Note: sync token logic with CdpServlet
		User authenticatedUser = null;
		Cookie tokenCookie = cookieRead(req, AUTH_TOKEN);
		if (tokenCookie != null)
		{
			authenticatedUser = authenticationService().authenticateByToken(tokenCookie.getValue(), req.getRemoteAddr());
		}

		try
		{
			// see if we are marked for attachment disposition
			boolean attachment = req.getParameter("attachment") != null;

			// parse the request: /refid/file.name
			String path = req.getPathInfo();
			String[] parts = split(path, "/");
			Reference ref = null;

			// special "U<userid>" handling (special for CKFinder)
			if (parts[1].startsWith("U"))
			{
				Long userId = Long.valueOf(parts[1].substring(1));
				User user = userService().get(userId);
				// TODO: if user != authenticatedUser, the authenticatedUser is accessing user's myfiles - allowed?

				// find a myFiles reference for the authenticated user matching the file name, the remainder of the path (TODO: folders)
				List<Reference> myFiles = fileService().getReferences(user);
				ref = fileService().findReferenceWithName(parts[2], myFiles);
			}
			else
			{
				// by reference - find it
				Long refId = Long.valueOf(parts[1]);
				ref = fileService().getReference(refId);
			}

			if (ref != null)
			{
				// TODO: verify the file name ?

				// is this user allowed to view this download?
				Role securityRole = ref.getSecurity();
				boolean allowed = false;

				// for custom security
				if (securityRole == Role.custom)
				{
					Service s = Services.getHandler(DownloadHandler.class, ref.getHolder().getTool());
					if ((s != null) && (s instanceof DownloadHandler))
					{
						allowed = ((DownloadHandler) s).authorize(authenticatedUser, ref.getHolder());
					}
					else
					{
						M_log.warn("doGet: custom security - no handler: " + path);
					}
				}

				// for anonymous access
				else if (securityRole == Role.anonymous)
				{
					allowed = true;
				}

				// for those who have at least logged in
				else if (securityRole == Role.authenticated)
				{
					allowed = authenticatedUser != null;
				}

				// for those who have a particular role level or higher in the site (must also be logged in)
				else if (authenticatedUser != null)
				{
					Role role = rosterService().userRoleInSite(authenticatedUser, ref.getHolder().getSite());
					if (role.ge(securityRole))
					{
						allowed = true;
					}
				}

				if (allowed)
				{
					// send the file referenced by the publication
					File file = ref.getFile();
					if (file != null)
					{
						sendContent(req, res, file, attachment);
					}
				}

				else
				{
					res.sendError(HttpServletResponse.SC_NOT_FOUND);
				}
			}
			else
			{
				res.sendError(HttpServletResponse.SC_NOT_FOUND);
			}
		}
		catch (Exception e)
		{
			M_log.warn("doGet: ", e);
			res.sendError(HttpServletResponse.SC_NOT_FOUND);
		}
		finally
		{
			// clear any bound current values
			threadLocalService().clear();
		}
	}

	/**
	 * @return The registered UserService.
	 */
	private UserService userService()
	{
		return (UserService) Services.get(UserService.class);
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
		M_log.warn("doPost");
		res.sendError(HttpServletResponse.SC_NOT_FOUND);
	}

	/**
	 * Find a cookie by this name from the request; one with a value that has the specified suffix.
	 * 
	 * @param req
	 *        The servlet request.
	 * @param name
	 *        The cookie name
	 * @param suffix
	 *        The suffix string to find at the end of the found cookie value.
	 * @return The cookie of this name in the request, or null if not found.
	 */
	protected Cookie findCookie(HttpServletRequest req, String name, String suffix)
	{
		Cookie[] cookies = req.getCookies();
		if (cookies != null)
		{
			for (int i = 0; i < cookies.length; i++)
			{
				if (cookies[i].getName().equals(name))
				{
					if ((suffix == null) || cookies[i].getValue().endsWith(suffix))
					{
						return cookies[i];
					}
				}
			}
		}

		return null;
	}

	/**
	 * Send the requested CHS resource
	 * 
	 * @param req
	 *        request
	 * @param res
	 *        response
	 * @param resourceId
	 *        the CHS resource id to dispatch
	 * @param secure
	 *        if false, bypass normal CHS security
	 * @param attachment
	 *        if true, add content disposition headers for a file download
	 * @throws ServletException
	 * @throws IOException
	 */
	protected void sendContent(HttpServletRequest req, HttpServletResponse res, File f, boolean attachment) throws IOException
	{
		int len = f.getSize();
		String contentType = f.getType();

		if (attachment)
		{
			res.setHeader("Content-Disposition", "attachment");
			res.setHeader("filename", f.getName());
		}

		// for text, we need to do some special handling
		if (contentType.startsWith("text/"))
		{
			// get the content as text
			String contentText = f.readString();
			sendContentText(req, res, contentType, contentText);
		}

		// for non-text, just send it (stream it in chunks to avoid the elephant-in-snake problem)
		else
		{
			InputStream content = f.readStream();
			if (content == null)
			{
				res.sendError(HttpServletResponse.SC_BAD_REQUEST);
			}

			sendContentBinary(req, res, contentType, null, len, content);
		}
	}

	protected void sendContentBinary(HttpServletRequest req, HttpServletResponse res, String contentType, String encoding, int len,
			InputStream content) throws IOException
	{
		OutputStream out = null;

		try
		{
			if ((encoding != null) && (encoding.length() > 0))
			{
				contentType = contentType + "; charset=" + encoding;
			}
			res.setContentType(contentType);
			// res.addHeader("Content-Disposition", disposition);
			res.setContentLength(len);

			// set the buffer of the response to match what we are reading from the request
			if (len < STREAM_BUFFER_SIZE)
			{
				res.setBufferSize(len);
			}
			else
			{
				res.setBufferSize(STREAM_BUFFER_SIZE);
			}

			out = res.getOutputStream();

			// chunk
			byte[] chunk = new byte[STREAM_BUFFER_SIZE];
			int lenRead;
			while ((lenRead = content.read(chunk)) != -1)
			{
				out.write(chunk, 0, lenRead);
			}
		}
		catch (Throwable e)
		{
			M_log.warn("sendContentBinary (while streaming, ignoring): " + e);
		}
		finally
		{
			// be a good little program and close the stream - freeing up valuable system resources
			if (content != null)
			{
				content.close();
			}

			if (out != null)
			{
				try
				{
					out.close();
				}
				catch (Throwable ignore)
				{
				}
			}
		}
	}

	/**
	 * Check for a session cookie - and if found, set that session as the current session
	 * 
	 * @param req
	 *        The request object.
	 * @param res
	 *        The response object.
	 * @return The Session object if found, else null.
	 */
	// protected Session establishSessionFromCookie(HttpServletRequest req, HttpServletResponse res)
	// {
	// // compute the session cookie suffix, based on this configured server id
	// String suffix = System.getProperty(SAKAI_SERVERID);
	// if ((suffix == null) || (suffix.length() == 0))
	// {
	// suffix = "sakai";
	// }
	//
	// // find our session id from our cookie
	// Cookie c = findCookie(req, SESSION_COOKIE, suffix);
	// if (c == null) return null;
	//
	// // get our session id
	// String sessionId = c.getValue();
	//
	// // remove the server id suffix
	// int dotPosition = sessionId.indexOf(".");
	// if (dotPosition > -1)
	// {
	// sessionId = sessionId.substring(0, dotPosition);
	// }
	//
	// // find the session
	// Session s = sessionManager().getSession(sessionId);
	//
	// // mark as active
	// if (s != null)
	// {
	// s.setActive();
	// }
	//
	// // set this as the current session
	// sessionManager().setCurrentSession(s);
	//
	// return s;
	// }

	protected void sendContentText(HttpServletRequest req, HttpServletResponse res, String contentType, String text) throws IOException
	{
		// text/url - send a redirect to the URL
		if (contentType.equals("text/url"))
		{
			res.sendRedirect(text);
		}

		// text/anything but html
		else if (!contentType.endsWith("/html"))
		{
			res.setCharacterEncoding("UTF-8");
			res.setContentType("text/html");
			PrintWriter out = res.getWriter();

			// send it as html in a PRE section
			out.println("<!DOCTYPE html>");
			out.println("<html><head>");
			out.println("<meta name=\"viewport\" content=\"width=device-width, initial-scale=1\">");
			out.println("</head><body>");
			out.print("<pre>");
			out.print(text);
			out.println("</pre>");
			out.println("</body></html>");
		}

		// text/html
		else
		{
			res.setCharacterEncoding("UTF-8");
			res.setContentType("text/html");
			PrintWriter out = res.getWriter();

			// if just a fragment, wrap it into a full document
			boolean fragment = !text.startsWith("<html");
			if (fragment)
			{
				out.println("<html><head>");
				out.println("<meta name=\"viewport\" content=\"width=device-width, initial-scale=1\">");
				// out.println("<script type=\"text/javascript\" src=\"/ckeditor/ckeditor/plugins/ckeditor_wiris/core/WIRISplugins.js?viewer=image\" defer=\"defer\"></script>");
				out.println("</head><body>");
			}

			// out.print(accessToCdpDoc(text, false));
			out.print(text);

			if (fragment)
			{
				out.println("</body></html>");
			}
		}
	}

	/**
	 * @return The registered AuthenticationService.
	 */
	private AuthenticationService authenticationService()
	{
		return (AuthenticationService) Services.get(AuthenticationService.class);
	}

	/**
	 * @return The registered FileService.
	 */
	private FileService fileService()
	{
		return (FileService) Services.get(FileService.class);
	}

	/**
	 * @return The registered RosterService.
	 */
	private RosterService rosterService()
	{
		return (RosterService) Services.get(RosterService.class);
	}

	/**
	 * @return The registered ThreadLocalService.
	 */
	private ThreadLocalService threadLocalService()
	{
		return (ThreadLocalService) Services.get(ThreadLocalService.class);
	}
}

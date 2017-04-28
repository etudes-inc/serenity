/**********************************************************************************
 * $URL: https://source.etudes.org/svn/serenity/trunk/connector/connector-webapp/src/main/java/org/etudes/connector/webapp/ConnectorServlet.java $
 * $Id: ConnectorServlet.java 11790 2015-10-07 19:54:23Z ggolden $
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

package org.etudes.connector.webapp;

import static org.etudes.util.Cookies.cookieRead;

import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;
import java.io.StringWriter;
import java.text.DateFormat;
import java.text.SimpleDateFormat;
import java.util.ArrayList;
import java.util.Date;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.TimeZone;
import java.util.UUID;

import javax.servlet.ServletException;
import javax.servlet.http.Cookie;
import javax.servlet.http.HttpServlet;
import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;
import javax.xml.parsers.DocumentBuilder;
import javax.xml.parsers.DocumentBuilderFactory;
import javax.xml.parsers.ParserConfigurationException;
import javax.xml.transform.Transformer;
import javax.xml.transform.TransformerFactory;
import javax.xml.transform.dom.DOMSource;
import javax.xml.transform.stream.StreamResult;

import net.coobird.thumbnailator.Thumbnails;

import org.apache.commons.fileupload.FileItem;
import org.apache.commons.fileupload.FileItemFactory;
import org.apache.commons.fileupload.FileUploadException;
import org.apache.commons.fileupload.disk.DiskFileItemFactory;
import org.apache.commons.fileupload.servlet.ServletFileUpload;
import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;
import org.etudes.authentication.api.AuthenticationService;
import org.etudes.config.api.ConfigService;
import org.etudes.file.api.FileService;
import org.etudes.file.api.Reference;
import org.etudes.roster.api.Role;
import org.etudes.service.api.Services;
import org.etudes.site.api.SiteService;
import org.etudes.threadlocal.api.ThreadLocalService;
import org.etudes.user.api.User;
import org.w3c.dom.Document;
import org.w3c.dom.Element;
import org.w3c.dom.Node;
import org.w3c.dom.NodeList;

/**
 * The ConnectorServlet servlet feeds CKFinder
 */
@SuppressWarnings("serial")
public class ConnectorServlet extends HttpServlet
{
	protected static int ACL_FileDelete = 128;
	protected static int ACL_FileRename = 64;

	protected static int ACL_FileUpload = 32;
	protected static int ACL_FileView = 16;
	protected static int ACL_FolderCreate = 2;
	protected static int ACL_FolderDelete = 8;
	protected static int ACL_FolderRename = 4;
	protected static int ACL_FolderView = 1;
	protected static int ACL_z_ALL = ACL_FolderView + ACL_FileView + ACL_FileUpload + ACL_FileRename + ACL_FileDelete;
	protected static final int MAX_BUFFER_SIZE = 1024;

	/** The chunk size used when streaming (100k). */
	protected static final int STREAM_BUFFER_SIZE = 102400;

	private static final String AUTH_TOKEN = "JSESSIONID";

	/** Our log. */
	private static Log M_log = LogFactory.getLog(ConnectorServlet.class);

	/** CKFinder license information. */
	protected String licenseKey = null;
	protected String licenseName = null;

	long maxSize = 0l;

	public void init() throws ServletException
	{
		// read configuration - once the config service is ready
		Services.whenAvailable(ConfigService.class, new Runnable()
		{
			public void run()
			{
				licenseKey = configService().getString("ckfinder.licenseKey");
				licenseName = configService().getString("ckfinder.licenseName");

				// configured in megs, report in bytes
				maxSize = configService().getLong("content.upload.max", 1l) * 1024 * 1024;
			}
		});
	}

	protected String accessUrl(String resourceId, User user)
	{
		// DownloadServlet and FileService knows about "U<userid>"
		return "/download/U" + user.getId() + resourceId;
	}

	/**
	 * Seed the response document with common elements.
	 * 
	 * @param doc
	 * @param commandStr
	 * @param type
	 * @param currentFolder
	 * @return
	 */
	protected Node createCommonXml(Document doc, String type, String currentFolder, User user)
	{
		// URL root of all files, for display (and for pasting the selected file into the CKEditor source)
		String curFolderUrl = accessUrl(currentFolder, user);

		Element root = doc.createElement("Connector");
		doc.appendChild(root);
		root.setAttribute("resourceType", type);

		Element errEl = doc.createElement("Error");
		errEl.setAttribute("number", "0");
		root.appendChild(errEl);

		Element element = doc.createElement("CurrentFolder");
		element.setAttribute("path", currentFolder);
		element.setAttribute("url", curFolderUrl);
		element.setAttribute("acl", "255");
		root.appendChild(element);

		return root;
	}

	/**
	 * create a Document object for the return
	 */
	protected Document createDocument()
	{
		Document document = null;
		try
		{
			DocumentBuilderFactory factory = DocumentBuilderFactory.newInstance();
			DocumentBuilder builder = factory.newDocumentBuilder();
			document = builder.newDocument();
		}
		catch (ParserConfigurationException e)
		{
			M_log.warn("createDocument: " + e.toString());
		}
		return document;
	}

	protected void doGet(HttpServletRequest request, HttpServletResponse response) throws ServletException, IOException
	{
		// Note: sync token logic with CdpServlet
		User authenticatedUser = null;
		Cookie tokenCookie = cookieRead(request, AUTH_TOKEN);
		if (tokenCookie != null)
		{
			authenticatedUser = authenticationService().authenticateByToken(tokenCookie.getValue(), request.getRemoteAddr());
		}

		try
		{
			// CKFinder command
			String commandStr = request.getParameter("command");

			// type is "Images" when we are in the image dialog, "Files" when we are in the link dialog
			String type = request.getParameter("type");

			String subFolder = request.getParameter("currentFolder");
			String rtype = request.getParameter("rtype");
			if (type == null) type = rtype;

			String currentFolderForResource = "";
			if (subFolder != null && !subFolder.equals("/"))
			{
				currentFolderForResource = currentFolderForResource + subFolder.substring(1, subFolder.length());
			}
			String currentFolder = "/" + currentFolderForResource;

			// called when the finder is started
			if ("Init".equals(commandStr))
			{
				Document document = createDocument();
				getInit(authenticatedUser, document, type, currentFolder);
				respondInXML(document, response);
			}

			// called to build up the folder hierarchy: once for the "currentFolder", and then once for each folder reported within
			else if ("GetFolders".equals(commandStr))
			{
				Document document = createDocument();
				Node root = createCommonXml(document, type, currentFolder, authenticatedUser);
				getFolders(authenticatedUser, currentFolder, root, document);
				respondInXML(document, response);
			}

			// called to get the list of files for "currentFolder" (which has "subFolder" appended already)
			else if ("GetFiles".equals(commandStr))
			{
				Document document = createDocument();
				Node root = createCommonXml(document, type, currentFolder, authenticatedUser);
				getFiles(authenticatedUser, currentFolder, root, document, type);
				respondInXML(document, response);
			}

			// called when the user selects (via the right click menu) to download a file
			else if ("DownloadFile".equals(commandStr))
			{
				String fileName = request.getParameter("FileName");
				// String resourceName = currentFolderForResource + fileName;

				// TODO: folders
				List<Reference> myFiles = fileService().getReferences(authenticatedUser);
				Reference ref = fileService().findReferenceWithName(fileName, myFiles);
				if (ref != null)
				{
					downloadContent(request, response, ref);
				}
				else
				{
					response.sendError(HttpServletResponse.SC_BAD_REQUEST);
				}
			}

			// called for a thumbnail of an image resource
			else if ("Thumbnail".equals(commandStr))
			{
				String fileName = request.getParameter("FileName");
				// TODO: deal with folders, currentFolderForResource

				// find the resource
				List<Reference> myFiles = fileService().getReferences(authenticatedUser);
				Reference ref = fileService().findReferenceWithName(fileName, myFiles);
				if (ref != null)
				{
					// stream a newly created thumbnail
					response.setContentType("image/png");
					OutputStream out = response.getOutputStream();
					try
					{
						// https://github.com/coobird/thumbnailator
						Thumbnails.of(ref.getFile().readStream()).size(80, 80).outputFormat("png").toOutputStream(out);
					}
					finally
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
		}
		finally
		{
			// clear any bound current values
			threadLocalService().clear();
		}
	}

	/**
	 * Manage the Post requests (FileUpload).<br>
	 * 
	 * The servlet accepts commands sent in the following format:<br>
	 * connector?Command=FileUpload&Type=ResourceType<br>
	 * <br>
	 * It stores the file (renaming it in case a file with the same name exists) and then return an HTML file with a javascript command in it.
	 * 
	 */
	protected void doPost(HttpServletRequest request, HttpServletResponse response) throws ServletException, IOException
	{
		// Note: sync token logic with CdpServlet
		User authenticatedUser = null;
		Cookie tokenCookie = cookieRead(request, AUTH_TOKEN);
		if (tokenCookie != null)
		{
			authenticatedUser = authenticationService().authenticateByToken(tokenCookie.getValue(), request.getRemoteAddr());
		}

		Map<String, Object> params = processBody(request);

		response.setContentType("text/plain; charset=UTF-8");
		// response.setHeader("Cache-Control", "no-cache");

		String command = request.getParameter("command");
		// String type = request.getParameter("type");
		String subFolder = request.getParameter("currentFolder");
		String type = request.getParameter("type");
		String rtype = request.getParameter("rtype");
		if (type == null) type = rtype;

		String currentFolder = "";
		if (subFolder != null && !subFolder.equals("/"))
		{
			currentFolder = currentFolder + subFolder.substring(1, subFolder.length());
		}

		// security - site update permission

		if (command.equals("FileUpload"))
		{
			List<Reference> myFiles = fileService().getReferences(authenticatedUser);

			// String responseType = request.getParameter("response_type");
			// String cKFinderFuncNum = request.getParameter("CKFinderFuncNum");

			Boolean replace = null; // TODO:?
			if (replace == null) replace = Boolean.FALSE;

			FileItem upload = null;
			Object o = params.get("upload");
			if ((o != null) && (o instanceof FileItem))
			{
				upload = (FileItem) o;
			}

			// reject non-type files TODO:
			// if (matchesType(mimeType, type))

			if ((upload != null) && (upload.getSize() > 0))
			{
				// if not replacing, come up with a unique name
				String name = upload.getName();
				if (!replace)
				{
					name = fileService().makeUniqueName(name, myFiles);
					fileService().add(name, (int) upload.getSize(), upload.getContentType(), upload.getInputStream(),
							fileService().myFileReference(authenticatedUser), Role.custom);
				}

				// if replacing, find the existing ref / file to replace
				else
				{
					Reference toBeReplaced = fileService().findReferenceWithName(name, myFiles);

					// if we don't have one by this name, just add it as usual
					if (toBeReplaced == null)
					{
						fileService().add(name, (int) upload.getSize(), upload.getContentType(), upload.getInputStream(),
								fileService().myFileReference(authenticatedUser), Role.custom);
					}

					// we need to replace this reference's file's contents with the new contents
					// the name is the same, so existing references to the file and their publications need no change.
					else
					{
						fileService().replace(toBeReplaced.getFile(), (int) upload.getSize(), upload.getContentType(), upload.getInputStream());
					}
				}

				// response for type "txt" - text/plain
				response.setContentType("text/plain; charset=UTF-8");
				response.setHeader("Cache-Control", "no-cache");

				// String rv = "<script type=\"text/javascript\">" + resourceName + "|";
				String rv = name + "|"; // add any error message after the |
				OutputStream out = response.getOutputStream();
				out.write(rv.getBytes("UTF-8"));
				out.close();
			}
		}
		else if (command.equals("QuickUploadAttachment"))
		{
			M_log.warn("QuickUploadAttachment");
			InputStream contents = null;
			String resourceName = "";

			contents = request.getInputStream();
			InputStream origContents = contents;
			String mimeType = request.getHeader("Content-Type");

			// If there's no filename, make a guid name with the mime extension?

			if (contents != null)
			{
				if ("".equals(resourceName))
				{
					resourceName = UUID.randomUUID().toString() + ".wav";
				}
				mimeType = improveMimeType(resourceName, mimeType);

				// siteResourcesService().addSiteResource(mimeType, 0, contents, siteId, "Home/" + resourceName); // TODO:

				// response for type "txt" - text/plain
				response.setContentType("text/plain; charset=UTF-8");
				response.setHeader("Cache-Control", "no-cache");

				// String rv = "<script type=\"text/javascript\">" + resourceName + "|";
				OutputStream out = response.getOutputStream();
				String url = accessUrl("/Home/", authenticatedUser) + resourceName;
				out.write(url.getBytes("UTF-8"));
				out.close();
			}
		}
		else if (command.equals("CopyFiles") || command.equals("MoveFiles") || command.equals("DeleteFiles"))
		{
			List<Reference> myFiles = fileService().getReferences(authenticatedUser);

			Document document = createDocument();
			Node root = createCommonXml(document, type, currentFolder, authenticatedUser);

			// which myFiles
			List<Reference> refs = new ArrayList<Reference>();
			int i = 0;
			String paramName = "files[" + i + "][name]";
			while (request.getParameter(paramName) != null)
			{
				String name = request.getParameter(paramName);

				// TODO: folder
				String folder = request.getParameter("files[" + i + "][folder]");
				// String options = request.getParameter("files[" + i + "][options]");
				// String typex = request.getParameter("files[" + i + "][type]");

				Reference ref = fileService().findReferenceWithName(name, myFiles);
				if (ref != null)
				{
					refs.add(ref);
				}

				i++;
				paramName = "files[" + (i) + "][name]";
			}

			if (command.equals("DeleteFiles"))
			{
				List<String> deleted = new ArrayList<String>();
				for (Reference ref : refs)
				{
					// not if in use
					List<Reference> otherRefs = fileService().getReferences(ref);
					if (otherRefs.size() == 1)
					{
						// remove the reference
						deleted.add(ref.getFile().getName());
						fileService().remove(ref);
					}
				}

				for (String name : deleted)
				{
					Element deletedEl = document.createElement("DeletedFile");
					root.appendChild(deletedEl);
					deletedEl.setAttribute("name", name);
				}

				if (deleted.isEmpty())
				{
					NodeList nodes = ((Element) root).getElementsByTagName("Error");
					Element errNode = (Element) nodes.item(0);
					errNode.setAttribute("number", "1");
					errNode.setAttribute("text", "Nothing deleted");
				}
			}

			else if (command.equals("MoveFiles"))
			{
				// TODO: folders!
				// TODO: fail if file in use?
				String fail = null;
				for (Reference ref : refs)
				{
					if (/* siteResourcesService().hasToolReferences(p) TODO: */false)
					{
						if (fail == null)
						{
							fail = "Not moved: ";
						}
						fail += ref.getFile().getName() + " ";
					}
				}

				if (fail == null)
				{
					// TODO: (if not fail if in use) update any uses of these resources to reflect the resulting path in the name
					int count = 0;
					for (Reference ref : refs)
					{
						// make the name unique
						String resourceName = ref.getFile().getName();
						// resourceName = siteResourcesService().uniqueResourceName(subFolder, resourceName, siteId); // TODO:

						count++;
						// siteResourcesService().renameReference(p, currentFolder + resourceName); // TODO:
					}

					Element el = document.createElement("MoveFiles");
					root.appendChild(el);
					el.setAttribute("moved", Integer.toString(count));
				}
				else
				{
					fail += " in use.";
					NodeList nodes = ((Element) root).getElementsByTagName("Error");
					Element errNode = (Element) nodes.item(0);
					errNode.setAttribute("number", "1");
					errNode.setAttribute("text", fail);
				}
			}

			else if (command.equals("CopyFiles"))
			{
				// TODO: folders

				int count = 0;
				for (Reference ref : refs)
				{
					// make the name unique
					String resourceName = ref.getFile().getName();
					// resourceName = siteResourcesService().uniqueResourceName(subFolder, resourceName, siteId); // TODO:

					count++;
					// siteResourcesService().addReference(p.getSiteResource(), siteId, currentFolder + resourceName); // TODO:
				}

				Element el = document.createElement("CopyFiles");
				root.appendChild(el);
				el.setAttribute("copied", Integer.toString(count));
			}

			respondInXML(document, response);
		}

		else if (command.equals("RenameFile"))
		{
			Document document = createDocument();
			Node root = createCommonXml(document, type, currentFolder, authenticatedUser);

			String fileName = request.getParameter("fileName");
			String newFileName = request.getParameter("newFileName");

			// find the myFile
			List<Reference> myFiles = fileService().getReferences(authenticatedUser);
			Reference ref = fileService().findReferenceWithName(fileName, myFiles);
			String failMsg = "Not renamed";
			if (ref != null)
			{
				// only if the name has really changed (case insensitive)
				if (!ref.getFile().getName().equalsIgnoreCase(newFileName))
				{
					// and is valid
					if (fileService().validFileName(newFileName))
					{
						// and if the new name does not conflict with any other file in myFiles
						Reference existingFile = fileService().findReferenceWithName(newFileName, myFiles);
						if (existingFile == null)
						{
							// rename the file, updating all references and publications and tool content
							fileService().rename(authenticatedUser, ref, newFileName);

							failMsg = null;
						}
					}
				}

				if (failMsg == null)
				{
					Element renamedEl = document.createElement("RenamedFile");
					root.appendChild(renamedEl);
					renamedEl.setAttribute("name", fileName);
					renamedEl.setAttribute("newName", newFileName);
				}
				else
				{
					NodeList nodes = ((Element) root).getElementsByTagName("Error");
					Element errNode = (Element) nodes.item(0);
					errNode.setAttribute("number", "1");
					errNode.setAttribute("text", failMsg);
				}
			}

			respondInXML(document, response);
		}
	}

	/**
	 * Send the requested site resource as a save-able download
	 * 
	 * @param req
	 *        request
	 * @param res
	 *        response
	 * @param placement
	 *        The Reference.
	 * @throws IOException
	 */
	protected void downloadContent(HttpServletRequest req, HttpServletResponse res, Reference ref) throws IOException
	{
		OutputStream out = null;
		int len = ref.getFile().getSize();
		InputStream content = ref.getFile().readStream();
		if (content == null)
		{
			res.sendError(HttpServletResponse.SC_BAD_REQUEST);
		}

		try
		{
			res.setContentType(ref.getFile().getType());
			res.setContentLength(len);
			res.setHeader("Cache-Control", "cache, must-revalidate");
			res.setHeader("Pragma", "public");
			res.setHeader("Expires", "0");
			res.setHeader("Content-Disposition", "attachment; filename=\"" + ref.getFile().getName() + "\"");

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
	 * Respond to the GetFiles command.
	 * 
	 * @param siteId
	 * @param userId
	 * @param currentFolder
	 * @param root
	 * @param doc
	 * @param type
	 */
	protected void getFiles(User user, String currentFolder, Node root, Document doc, String type)
	{
		// M_log.warn("getFiles: " + currentFolder);
		Element files = doc.createElement("Files");
		root.appendChild(files);

		// collect the user's myfiles
		List<Reference> myFiles = fileService().getReferences(user);

		for (Reference ref : myFiles)
		{
			// filter based on request type (Files or Images or Flash)
			if (!matchesType(ref.getFile().getType(), type)) continue;

			// filter based on path
			// TODO:
			// if (false) continue;
			// String[] path = p.getFilePath();
			// if (("/".equals(currentFolder) && (path == null))
			// || ((path != null) && (path.length == 2) && (currentFolder.equals("/" + path[0] + "/"))))

			Element element = doc.createElement("File");
			element.setAttribute("name", ref.getFile().getName());
			element.setAttribute("date", timeDisplayInUserZone(user, ref.getFile().getModifiedOn().getTime()));
			element.setAttribute("size", lengthInK(ref.getFile().getSize()));
			element.setAttribute("refId", ref.getId().toString());
			element.setAttribute("fileName", ref.getFile().getName());

			files.appendChild(element);
		}
	}

	/**
	 * Respond to the GetFolders command.
	 * 
	 * @param siteId
	 * @param userId
	 * @param currentFolder
	 * @param root
	 * @param doc
	 */
	protected void getFolders(User user, String currentFolder, Node root, Document doc)
	{
		Element folders = doc.createElement("Folders");
		root.appendChild(folders);

		// ACL=49: value to disable context menu options

		// fixed set of folders
		// TODO: by tool inclusion in site?
		if (currentFolder.equals("/"))
		{
			// Element element = doc.createElement("Folder");
			// // element.setAttribute("url", this.accessUrl(siteId, currentFolder + "/General/"));
			// element.setAttribute("name", "General");
			// element.setAttribute("acl", Integer.toString(ACL_z_ALL));
			// element.setAttribute("hasChildren", "false");
			// folders.appendChild(element);
			//
			// element = doc.createElement("Folder");
			// element.setAttribute("name", "Home");
			// element.setAttribute("acl", Integer.toString(ACL_z_ALL));
			// element.setAttribute("hasChildren", "false");
			// folders.appendChild(element);

			// element = doc.createElement("Folder");
			// element.setAttribute("name", "Schedule");
			// element.setAttribute("acl", "49");
			// element.setAttribute("hasChildren", "false");
			// folders.appendChild(element);
			//
			// element = doc.createElement("Folder");
			// element.setAttribute("name", "Announcements");
			// element.setAttribute("acl", "49");
			// element.setAttribute("hasChildren", "false");
			// folders.appendChild(element);
			//
			// element = doc.createElement("Folder");
			// element.setAttribute("name", "Syllabus");
			// element.setAttribute("acl", "49");
			// element.setAttribute("hasChildren", "false");
			// folders.appendChild(element);
			//
			// element = doc.createElement("Folder");
			// element.setAttribute("name", "Modules");
			// element.setAttribute("acl", "49");
			// element.setAttribute("hasChildren", "false");
			// folders.appendChild(element);
			//
			// element = doc.createElement("Folder");
			// element.setAttribute("name", "ATS");
			// element.setAttribute("acl", "49");
			// element.setAttribute("hasChildren", "false");
			// folders.appendChild(element);
			//
			// element = doc.createElement("Folder");
			// element.setAttribute("name", "Resources");
			// element.setAttribute("acl", "49");
			// element.setAttribute("hasChildren", "false");
			// folders.appendChild(element);
		}
	}

	/**
	 * Create the DOM for the init request.
	 * 
	 * @param doc
	 * @param type
	 * @param currentFolder
	 * @return
	 */
	protected Node getInit(User user, Document doc, String type, String currentFolder)
	{
		Element root = doc.createElement("Connector");
		doc.appendChild(root);

		Element errEl = doc.createElement("Error");
		errEl.setAttribute("number", "0");
		root.appendChild(errEl);

		Element ciEl = doc.createElement("ConnectorInfo");
		ciEl.setAttribute("enabled", "true");
		ciEl.setAttribute("imgWidth", "");
		ciEl.setAttribute("imgHeight", "");
		ciEl.setAttribute("s", this.licenseName);
		ciEl.setAttribute("c", this.licenseKey);
		ciEl.setAttribute("thumbsEnabled", "true");
		// ciEl.setAttribute("thumbsUrl", "");
		// ciEl.setAttribute("thumbsDirectAccess", "true");
		root.appendChild(ciEl);

		Element rtsEl = doc.createElement("ResourceTypes");
		root.appendChild(rtsEl);

		// URL to access the folder
		String curFolderUrl = accessUrl(currentFolder, user);

		// if ((this.types != null) && (this.types.size() > 0))
		// {
		// if (this.types.get(type) != null)
		// {
		// // Properties of a resource type(Files, Images and Flash) can be configured in the settings xml
		// ResourceType rt = (ResourceType) this.types.get(type);
		Element rtEl = doc.createElement("ResourceType");
		rtEl.setAttribute("name", "My Resources"); // TODO: i18n, user name? type?
		rtEl.setAttribute("url", curFolderUrl);
		rtEl.setAttribute("allowedExtensions", ""); // bmp,gif,jpeg,jpg,png
		rtEl.setAttribute("deniedExtensions", "");
		rtEl.setAttribute("maxSize", String.valueOf(this.maxSize));
		rtEl.setAttribute("defaultView", "thumbnails"); // or "list"
		rtEl.setAttribute("hash", "4d8ddfd385d0952b");
		rtEl.setAttribute("hasChildren", "false"); // TODO: "true" when we have sub-folders
		rtEl.setAttribute("acl", Integer.toString(ACL_z_ALL));
		rtsEl.appendChild(rtEl);
		// }
		// }

		return root;
	}

	/**
	 * IE 11 sends files in with the mime type "application/octet-stream" when it really should be "image/*". See if we can get a better mime type from the file extension.
	 * 
	 * @param name
	 *        The file name.
	 * @param type
	 *        The given mime type.
	 * @return A possibly improved mime type.
	 */
	protected String improveMimeType(String name, String type)
	{
		if (((type == null) || "application/octet-stream".equalsIgnoreCase(type)) && (name != null))
		{
			// isolate the file extension
			int pos = name.lastIndexOf(".");
			if (pos != -1)
			{
				String ext = name.substring(pos + 1);

				// recognize some image extensions: jpg jpeg gif png x-png bmp bm tif tiff
				if ("jpg".equalsIgnoreCase(ext))
					return "image/jpeg";
				else if ("jpeg".equalsIgnoreCase(ext))
					return "image/jpeg";
				else if ("gif".equalsIgnoreCase(ext))
					return "image/gif";
				else if ("png".equalsIgnoreCase(ext))
					return "image/png";
				else if ("x-png".equalsIgnoreCase(ext))
					return "image/png";
				else if ("bmp".equalsIgnoreCase(ext))
					return "image/bmp";
				else if ("bm".equalsIgnoreCase(ext))
					return "image/bmp";
				else if ("tif".equalsIgnoreCase(ext))
					return "image/tiff";
				else if ("tiff".equalsIgnoreCase(ext))
					return "image/tiff";
				else if ("html".equalsIgnoreCase(ext))
					return "text/html";
				else if ("htm".equalsIgnoreCase(ext))
					return "text/html";
				else if ("pdf".equalsIgnoreCase(ext))
					return "application/pdf";
				else if ("doc".equalsIgnoreCase(ext))
					return "application/msword";
				else if ("docm".equalsIgnoreCase(ext))
					return "application/vnd.ms-word.document.macroEnabled.12";
				else if ("docx".equalsIgnoreCase(ext))
					return "application/vnd.openxmlformats-officedocument.wordprocessingml.document";
				else if ("ppt".equalsIgnoreCase(ext))
					return "application/vnd.ms-powerpoint";
				else if ("ppsx".equalsIgnoreCase(ext))
					return "application/vnd.openxmlformats-officedocument.presentationml.slideshow";
				else if ("url".equalsIgnoreCase(ext))
					return "text/url";
				else if ("webloc".equalsIgnoreCase(ext))
					return "text/url";
				else if ("xls".equalsIgnoreCase(ext))
					return "application/vnd.ms-excel";
				else if ("xlsx".equalsIgnoreCase(ext))
					return "application/vnd.openxmlformats-officedocument.spreadsheetml.sheet";
				else if ("pptx".equalsIgnoreCase(ext)) return "application/vnd.openxmlformats-officedocument.presentationml.presentation";
			}
		}

		return type;
	}

	/**
	 * Compute a rounded kbytes string from an int byte count.
	 * 
	 * @param len
	 *        The bytes.
	 * @return The "K" value.
	 */
	protected String lengthInK(int len)
	{
		if (len < 1024) return "1";
		return Integer.toString(Math.round(((float) len) / 1024f));
	}

	/**
	 * Check if the site resource's mime type is appropriate for the requested type.
	 * 
	 * @param resource
	 * @param type
	 * @return
	 */
	protected boolean matchesType(String mime, String type)
	{
		// site title then space then the real type
		String[] parts = type.split(" ");
		type = parts[parts.length - 1];

		if (type.equals("Files")) return true;

		if (type.equals("Flash")) return mime.equals("application/x-shockwave-flash");

		if (type.equals("Images")) return mime.startsWith("image");

		return true;
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
					// the key
					String key = item.getFieldName();

					// the value
					String value = item.getString();

					// merge into our map of key / values
					Object current = rv.get(item.getFieldName());

					// if not there, start with the value
					if (current == null)
					{
						rv.put(key, value);
					}

					// if we find a value, change it to an array containing both
					else if (current instanceof String)
					{
						String[] values = new String[2];
						values[0] = (String) current;
						values[1] = value;
						rv.put(key, values);
					}

					// if an array is found, extend our current values to include this additional one
					else if (current instanceof String[])
					{
						String[] currentArray = (String[]) current;
						String[] values = new String[currentArray.length + 1];
						System.arraycopy(currentArray, 0, values, 0, currentArray.length);
						values[currentArray.length] = value;
						rv.put(key, values);
					}
				}
				else
				{
					rv.put(item.getFieldName(), item);
				}
			}
		}
		catch (FileUploadException e)
		{
			// M_log.warn("processBody: exception:" + e);
		}

		// TODO: add URL parameters

		return rv;
	}

	/**
	 * Sends the response in XML format.
	 */
	protected void respondInXML(Document document, HttpServletResponse response)
	{
		if (document == null) return;
		response.setContentType("text/xml; charset=UTF-8");
		response.setHeader("Cache-Control", "no-cache");

		try
		{
			OutputStream out = response.getOutputStream();

			document.getDocumentElement().normalize();

			StringWriter stw = new StringWriter();
			TransformerFactory tFactory = TransformerFactory.newInstance();
			Transformer transformer = tFactory.newTransformer();

			DOMSource source = new DOMSource(document);

			StreamResult result = new StreamResult(stw);
			transformer.transform(source, result);
			out.write(stw.toString().getBytes("UTF-8"));
			out.flush();
			out.close();
		}
		catch (Exception ex)
		{
			ex.printStackTrace();
		}
	}

	/**
	 * @return time formatted for the user's time zone with month & date, hour, minute, am/pm
	 */
	protected String timeDisplayInUserZone(User user, long timeStartMs)
	{
		DateFormat dateFormat = new SimpleDateFormat("yyyyMMddkkmm");
		dateFormat.setTimeZone(TimeZone.getTimeZone(user.getTimeZoneDflt()));
		String rv = dateFormat.format(new Date(timeStartMs));

		return rv;
	}

	/**
	 * @return The registered AuthenticationService.
	 */
	private AuthenticationService authenticationService()
	{
		return (AuthenticationService) Services.get(AuthenticationService.class);
	}

	/**
	 * @return The registered ConfigService.
	 */
	private ConfigService configService()
	{
		return (ConfigService) Services.get(ConfigService.class);
	}

	/**
	 * @return The registered FileService.
	 */
	private FileService fileService()
	{
		return (FileService) Services.get(FileService.class);
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
}

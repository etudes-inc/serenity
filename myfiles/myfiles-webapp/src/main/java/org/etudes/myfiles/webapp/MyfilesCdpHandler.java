/**********************************************************************************
 * $URL: https://source.etudes.org/svn/serenity/trunk/myfiles/myfiles-webapp/src/main/java/org/etudes/myfiles/webapp/MyfilesCdpHandler.java $
 * $Id: MyfilesCdpHandler.java 11567 2015-09-06 20:22:36Z ggolden $
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

package org.etudes.myfiles.webapp;

import static org.etudes.util.StringUtil.formatFileSize;
import static org.etudes.util.StringUtil.trimToNull;

import java.io.IOException;
import java.util.ArrayList;
import java.util.Collections;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import javax.servlet.ServletException;
import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;

import org.apache.commons.fileupload.FileItem;
import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;
import org.etudes.cdp.api.CdpHandler;
import org.etudes.cdp.api.CdpService;
import org.etudes.cdp.api.CdpStatus;
import org.etudes.file.api.File;
import org.etudes.file.api.FileService;
import org.etudes.file.api.Reference;
import org.etudes.roster.api.Role;
import org.etudes.service.api.Services;
import org.etudes.site.api.SiteService;
import org.etudes.tool.api.Tool;
import org.etudes.user.api.User;
import org.etudes.user.api.UserService;

/**
 */
public class MyfilesCdpHandler implements CdpHandler
{
	/** Our log (commons). */
	private static Log M_log = LogFactory.getLog(MyfilesCdpHandler.class);

	public String getPrefix()
	{
		return "myfiles";
	}

	public Map<String, Object> handle(HttpServletRequest req, HttpServletResponse res, Map<String, Object> parameters, String requestPath,
			String path, User authenticatedUser) throws ServletException, IOException
	{
		// if no authenticated user, we reject all requests
		if (authenticatedUser == null)
		{
			Map<String, Object> rv = new HashMap<String, Object>();
			rv.put(CdpStatus.CDP_STATUS, CdpStatus.notLoggedIn.getId());
			return rv;
		}
		else if (requestPath.equals("get"))
		{
			return dispatchGet(req, res, parameters, path, authenticatedUser);
		}
		else if (requestPath.equals("upload"))
		{
			return dispatchUpload(req, res, parameters, path, authenticatedUser);
		}
		else if (requestPath.equals("remove"))
		{
			return dispatchRemove(req, res, parameters, path, authenticatedUser);
		}
		else if (requestPath.equals("rename"))
		{
			return dispatchRename(req, res, parameters, path, authenticatedUser);
		}
		else if (requestPath.equals("replace"))
		{
			return dispatchReplace(req, res, parameters, path, authenticatedUser);
		}

		return null;
	}

	protected Map<String, Object> dispatchGet(HttpServletRequest req, HttpServletResponse res, Map<String, Object> parameters, String path,
			User authenticatedUser) throws ServletException, IOException
	{
		Map<String, Object> rv = new HashMap<String, Object>();

		User forUser = authenticatedUser;

		// user override - ignore if it is for the authenticated user, otherwise, it's only allowed for admin
		Long forUserId = cdpService().readLong(parameters.get("forUserId"));
		if ((forUserId != null) && (forUserId != authenticatedUser.getId()))
		{
			// only good for admin
			if (!authenticatedUser.isAdmin())
			{
				M_log.warn("dispatchGet: forUser from nonAdmin");
				rv.put(CdpStatus.CDP_STATUS, CdpStatus.badRequest.getId());
				return rv;
			}

			forUser = userService().get(forUserId);
			if (forUser == null)
			{
				M_log.warn("dispatchGet: forUser not found: " + forUserId);
				rv.put(CdpStatus.CDP_STATUS, CdpStatus.badRequest.getId());
				return rv;
			}
		}

		String type = cdpService().readString(parameters.get("type"));

		rv.put("myfiles", respondMyFiles(forUser, type));

		// add status parameter
		rv.put(CdpStatus.CDP_STATUS, CdpStatus.success.getId());

		return rv;
	}

	protected Map<String, Object> dispatchRemove(HttpServletRequest req, HttpServletResponse res, Map<String, Object> parameters, String path,
			User authenticatedUser) throws ServletException, IOException
	{
		Map<String, Object> rv = new HashMap<String, Object>();

		// file reference ids
		List<Long> refIds = cdpService().readIds(parameters.get("ids"));
		if (refIds == null)
		{
			M_log.warn("dispatchRemove: missing ids");
			rv.put(CdpStatus.CDP_STATUS, CdpStatus.badRequest.getId());
			return rv;
		}

		for (Long refId : refIds)
		{
			// make sure this is to a myFiles reference for the user
			Reference ref = fileService().getReference(refId);
			if (ref == null)
			{
				M_log.warn("dispatchRemove: reference not found");
				rv.put(CdpStatus.CDP_STATUS, CdpStatus.badRequest.getId());
				return rv;
			}
			if ((ref.getHolder().getTool() != Tool.myfiles) || (!ref.getHolder().getItemId().equals(authenticatedUser.getId())))
			{
				M_log.warn("dispatchRemove: reference not user's myfile");
				rv.put(CdpStatus.CDP_STATUS, CdpStatus.badRequest.getId());
				return rv;
			}

			// quietly ignore if in use
			List<Reference> otherRefs = fileService().getReferences(ref);
			if (otherRefs.size() == 1)
			{
				// remove the reference
				fileService().remove(ref);
			}
		}

		// add status parameter
		rv.put(CdpStatus.CDP_STATUS, CdpStatus.success.getId());

		return rv;
	}

	protected Map<String, Object> dispatchRename(HttpServletRequest req, HttpServletResponse res, Map<String, Object> parameters, String path,
			User authenticatedUser) throws ServletException, IOException
	{
		Map<String, Object> rv = new HashMap<String, Object>();

		// reference id of a myFiles reference
		Long refId = cdpService().readLong(parameters.get("id"));
		if (refId == null)
		{
			M_log.warn("dispatchRename: missing id");
			rv.put(CdpStatus.CDP_STATUS, CdpStatus.badRequest.getId());
			return rv;
		}

		// make sure this is to a myFiles reference for the user
		Reference ref = fileService().getReference(refId);
		if (ref == null)
		{
			M_log.warn("dispatchRename: reference not found");
			rv.put(CdpStatus.CDP_STATUS, CdpStatus.badRequest.getId());
			return rv;
		}

		if ((ref.getHolder().getTool() != Tool.myfiles) || (!ref.getHolder().getItemId().equals(authenticatedUser.getId())))
		{
			M_log.warn("dispatchRename: reference not user's myfile");
			rv.put(CdpStatus.CDP_STATUS, CdpStatus.badRequest.getId());
			return rv;
		}

		// the new name
		String name = cdpService().readString(parameters.get("name"));
		if (name != null) name = trimToNull(name);
		if (name == null)
		{
			M_log.warn("dispatchRename: missing name");
			rv.put(CdpStatus.CDP_STATUS, CdpStatus.badRequest.getId());
			return rv;
		}

		// only if the name has really changed (case insensitive)
		if (!ref.getFile().getName().equalsIgnoreCase(name))
		{
			// and is valid
			if (fileService().validFileName(name))
			{
				// and if the new name does not conflict with any other file in myFiles
				List<Reference> myFiles = fileService().getReferences(authenticatedUser);
				Reference existingFile = fileService().findReferenceWithName(name, myFiles);
				if (existingFile == null)
				{
					// rename the file, updating all references and publications and tool content
					fileService().rename(authenticatedUser, ref, name);
				}
			}
		}

		// add status parameter
		rv.put(CdpStatus.CDP_STATUS, CdpStatus.success.getId());

		return rv;
	}

	protected Map<String, Object> dispatchReplace(HttpServletRequest req, HttpServletResponse res, Map<String, Object> parameters, String path,
			User authenticatedUser) throws ServletException, IOException
	{
		Map<String, Object> rv = new HashMap<String, Object>();

		// reference id of file to replace
		Long refId = cdpService().readLong(parameters.get("id"));
		if (refId == null)
		{
			M_log.warn("dispatchReplace: missing id");
			rv.put(CdpStatus.CDP_STATUS, CdpStatus.badRequest.getId());
			return rv;
		}

		FileItem upload = null;
		Object o = parameters.get("file");
		if ((o != null) && (o instanceof FileItem))
		{
			upload = (FileItem) o;
		}
		if ((upload == null) || (upload.getSize() == 0))
		{
			M_log.warn("dispatchReplace: missing file");
			rv.put(CdpStatus.CDP_STATUS, CdpStatus.badRequest.getId());
			return rv;
		}

		Reference toBeReplaced = fileService().getReference(refId);
		if (toBeReplaced == null)
		{
			M_log.warn("dispatchReplace: cannot find reference: " + refId);
			rv.put(CdpStatus.CDP_STATUS, CdpStatus.badRequest.getId());
			return rv;
		}

		// if the name is different, check that the replacement name is not in use
		boolean nameConflict = false;
		boolean renamed = false;
		if (!toBeReplaced.getFile().getName().equalsIgnoreCase(upload.getName()))
		{
			renamed = true;

			List<Reference> myFiles = fileService().getReferences(authenticatedUser);
			Reference existingFile = fileService().findReferenceWithName(upload.getName(), myFiles);
			if (existingFile != null)
			{
				nameConflict = true;
			}
		}

		if (!nameConflict)
		{
			// replace the body
			fileService().replace(toBeReplaced.getFile(), (int) upload.getSize(), upload.getContentType(), upload.getInputStream());

			// rename if needed
			if (renamed)
			{
				fileService().rename(authenticatedUser, toBeReplaced, upload.getName());
			}
		}

		// add status parameter
		rv.put(CdpStatus.CDP_STATUS, CdpStatus.success.getId());

		return rv;
	}

	protected Map<String, Object> dispatchUpload(HttpServletRequest req, HttpServletResponse res, Map<String, Object> parameters, String path,
			User authenticatedUser) throws ServletException, IOException
	{
		Map<String, Object> rv = new HashMap<String, Object>();

		// upload count
		Integer count = cdpService().readInt(parameters.get("count"));
		if (count == null)
		{
			M_log.warn("dispatchUpload: missing count");
			rv.put(CdpStatus.CDP_STATUS, CdpStatus.badRequest.getId());
			return rv;
		}

		// get the authenticated user's myfiles
		List<Reference> myFiles = fileService().getReferences(authenticatedUser);

		// replace or keep / renamed policy
		Boolean replace = cdpService().readBoolean(parameters.get("replace"));
		if (replace == null) replace = Boolean.FALSE;

		for (int i = 0; i < count; i++)
		{
			FileItem upload = null;
			Object o = parameters.get("file_" + i);
			if ((o != null) && (o instanceof FileItem))
			{
				upload = (FileItem) o;
			}
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

				// refresh myFiles
				myFiles = fileService().getReferences(authenticatedUser);
			}
		}

		// add status parameter
		rv.put(CdpStatus.CDP_STATUS, CdpStatus.success.getId());

		return rv;
	}

	/**
	 * 
	 * @param user
	 * @param type
	 * @return
	 */
	protected List<Map<String, Object>> respondMyFiles(User user, String type)
	{
		// make sure the mime type filter is normalized to lower case
		if (type != null) type = type.toLowerCase();

		// get the authenticated user's myFiles
		List<Reference> myFiles = fileService().getReferences(user);

		// sort by name
		Collections.sort(myFiles, new Reference.FileNameComparator());

		List<Map<String, Object>> myFilesMap = new ArrayList<Map<String, Object>>();
		for (Reference ref : myFiles)
		{
			// filter by type
			if (type != null)
			{
				if (!ref.getFile().getType().startsWith(type)) continue;
			}

			Map<String, Object> myFileMap = ref.send();
			myFilesMap.add(myFileMap);

			// usage
			List<Reference> otherRefs = fileService().getReferences(ref);
			List<Map<String, Object>> usageMap = new ArrayList<Map<String, Object>>();
			myFileMap.put("usage", usageMap);

			for (Reference other : otherRefs)
			{
				// filter out myFiles references
				if (other.getHolder().getTool() == Tool.myfiles) continue;

				usageMap.add(other.getHolder().send());
			}
		}

		return myFilesMap;
	}

	/**
	 * @return The registered CdpService.
	 */
	private CdpService cdpService()
	{
		return (CdpService) Services.get(CdpService.class);
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
	 * @return The registered UserService.
	 */
	private UserService userService()
	{
		return (UserService) Services.get(UserService.class);
	}
}

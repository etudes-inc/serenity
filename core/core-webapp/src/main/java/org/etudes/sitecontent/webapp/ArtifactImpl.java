/**********************************************************************************
 * $URL: https://source.etudes.org/svn/serenity/trunk/core/core-webapp/src/main/java/org/etudes/sitecontent/webapp/ArtifactImpl.java $
 * $Id: ArtifactImpl.java 10086 2015-02-18 03:43:41Z ggolden $
 ***********************************************************************************
 *
 * Copyright (c) 2009, 2015 Etudes, Inc.
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

package org.etudes.sitecontent.webapp;

import java.util.Collection;
import java.util.HashMap;
import java.util.LinkedHashSet;
import java.util.Map;
import java.util.Set;

import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;
import org.etudes.file.api.File;
import org.etudes.file.api.Reference;
import org.etudes.sitecontent.api.Archive;
import org.etudes.sitecontent.api.Artifact;
import org.etudes.tool.api.Tool;

/**
 * ArchiverHandler
 */
public class ArtifactImpl implements Artifact
{
	/** Our log. */
	private static Log M_log = LogFactory.getLog(ArtifactImpl.class);

	/** The archive. */
	protected Archive archive = null;

	/** The file ids referenced (used when reading an existing artifact). */
	protected Set<Long> fileIdsReferenced = new LinkedHashSet<Long>();

	/** File extension number. */
	protected int fileNum = 1;

	/** The old-style files referenced, as strings (used when reading an existing old-style artifact). */
	protected Set<String> fileRefsReferenced = new LinkedHashSet<String>();

	/** File name in archives. */
	protected String fName = null;

	/** Id. */
	protected Long id = null;

	/** Properties. */
	protected Map<String, Object> properties = new HashMap<String, Object>();

	/** The references (to files) (used when creating an artifact for archiving). Note: this type of set preserves insertion order */
	protected Set<File> references = new LinkedHashSet<File>();

	protected Tool tool = null;

	/** Artifact type. */
	protected String type = null;

	public ArtifactImpl(Archive archive, Long id, Tool tool, String type)
	{
		this.archive = archive;
		this.id = id;
		this.tool = tool;
		this.type = type;
	}

	@Override
	public void addReferences(Collection<Reference> references)
	{
		for (Reference ref : references)
		{
			this.references.add(ref.getFile());
		}
	}

	@Override
	public Archive getArchive()
	{
		return this.archive;
	}

	@Override
	public Map<String, Object> getProperties()
	{
		return this.properties;
	}

	@Override
	public Tool getTool()
	{
		return this.tool;
	}

	@Override
	public String getType()
	{
		return this.type;
	}

	/**
	 * @return The fileIdsReferenced set.
	 */
	protected Set<Long> getFileIdsReferenced()
	{
		return this.fileIdsReferenced;
	}

	/**
	 * {@inheritDoc}
	 */
	protected String getFileName()
	{
		return this.fName;
	}

	/**
	 * @return The fileRefsReferenced set.
	 */
	protected Set<String> getFileRefsReferenced()
	{
		return this.fileRefsReferenced;
	}

	/**
	 * {@inheritDoc}
	 */
	protected Long getId()
	{
		return this.id;
	}

	/**
	 * {@inheritDoc}
	 */
	protected String getNextFileSuffix()
	{
		return "-file-" + this.fileNum++;
	}

	/**
	 * @return The references set.
	 */
	protected Set<File> getReferences()
	{
		return this.references;
	}

	/**
	 * Set the archives file name for this artifact (relative to the archive)
	 * 
	 * @param fName
	 *        The relative file name.
	 */
	protected void setFileName(String fName)
	{
		this.fName = fName;
	}
}

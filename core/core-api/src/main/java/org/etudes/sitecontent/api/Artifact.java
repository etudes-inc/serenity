/**********************************************************************************
 * $URL: https://source.etudes.org/svn/serenity/trunk/core/core-api/src/main/java/org/etudes/sitecontent/api/Artifact.java $
 * $Id: Artifact.java 10054 2015-02-10 21:07:02Z ggolden $
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

package org.etudes.sitecontent.api;

import java.util.Collection;
import java.util.Map;

import org.etudes.file.api.Reference;
import org.etudes.tool.api.Tool;

/**
 * Artifact represents one unit of information in an archives.
 */
public interface Artifact
{
	// /**
	// * Generate another unique suffix for files stored with this artifact.
	// *
	// * @return The file suffix.
	// */
	// String getNextFileSuffix();

	/**
	 * Add these file system references to the artifact - these are the references used by the artifact and that need to be part of the archive.
	 * 
	 * @param references
	 *        The list of references.
	 */
	void addReferences(Collection<Reference> references);

	/**
	 * @return The artifact's archive.
	 */
	Archive getArchive();

	/**
	 * Access the Map of properties that describe this artifact.
	 * 
	 * @return The Map of properties.
	 */
	Map<String, Object> getProperties();

	/**
	 * @return The artifact tool.
	 */
	Tool getTool();

	// /**
	// * Get the reference string for this artifact (from the source system).
	// *
	// * @return The artifact's reference string.
	// */
	// String getReference();
	//
	// /**
	// * Access the set of reference strings to artifacts referenced by this artifact.
	// *
	// * @return The set of reference strings.
	// */
	// Set<String> getReferences();
	//
	/**
	 * Get the artifact type.
	 *
	 * @return The artifact type (a string defined by each archiving tool).
	 */
	String getType();

	//
	// /**
	// * Set the artifact's reference string (from the source system).
	// *
	// * @param reference
	// * The artifact's reference string.
	// */
	// void setReference(String reference);
}

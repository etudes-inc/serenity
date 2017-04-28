/**********************************************************************************
 * $URL: https://source.etudes.org/svn/serenity/trunk/core/core-api/src/main/java/org/etudes/file/api/FileService.java $
 * $Id: FileService.java 11562 2015-09-06 18:40:42Z ggolden $
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

package org.etudes.file.api;

import java.io.InputStream;
import java.util.Date;
import java.util.List;
import java.util.Map;
import java.util.Set;

import org.etudes.roster.api.Role;
import org.etudes.tool.api.ToolItemReference;
import org.etudes.user.api.User;

/**
 * Serenity File service.
 * 
 * Notes:
 * 
 * A "Download URL" is in the form: "/download/22/picture.png", where "22" is the id of the file Reference held by the tool, and "picture.png" is the name of the file (so the browser has a name and extensions).
 * 
 * Download URLs are never stored in the database or embedded in html content. Instead, they are built on-the-fly using:
 * 
 * - for sending to the browser: processContentPlaceholderToDownload()
 * 
 * - for bringing back from being edited in the browser: processContentDownloadToPlaceholder()
 * 
 * A "Placeholder url" is in the form: "/file/42", where "42" is the id of the File.
 * 
 * Placeholder URLs are used to store embedded in html content. This allows the file system file behind a File to be shared across various tool use and across sites.
 * 
 * The e3 filer browser component works with myFiles references.
 * 
 * When using the e3 filer to select a myFile, the reference the tool holds needs to be converted to the user's myFiles reference.
 * 
 * This can be done with: Reference getReference(Reference other, User myFilesUser), passing in the tool's reference.
 */
public interface FileService
{
	/**
	 * Add a reference to an already existing file.
	 * 
	 * @param file
	 *        The file to reference.
	 * @param holder
	 *        The ToolItemReference for the tool item referencing the file.
	 * @param security
	 *        The publication security for the reference.
	 * @return The new Reference to the File.
	 */
	Reference add(File file, ToolItemReference holder, Role security);

	/**
	 * Add a reference to an already existing file, for a user's myFiles.
	 * 
	 * @param file
	 *        The file to reference.
	 * @param myFilesUser
	 *        A User to take "my files" ownership of this file.
	 * @return The Reference to the new File object.
	 */
	Reference add(File file, User myFilesUser);

	/**
	 * Add a file with no references.
	 * 
	 * @param name
	 *        The file name (as created / uploaded)
	 * @param size
	 *        The file size (in bytes).
	 * @param type
	 *        The mime type.
	 * @param content
	 *        The file content stream.
	 * @param id
	 *        (optional) The File's internal id. If null, a new id is generated.
	 * @param date
	 *        (optional) The file's creation / upload date. If null, now is used.
	 * @param modifiedOn
	 *        (optional) The file's modified date. If null, now is used.
	 * @return The file.
	 */
	File add(String name, int size, String type, InputStream content, Long id, Date date, Date modifiedOn);

	/**
	 * Add a file to the file system, recording one tool reference.
	 * 
	 * @param name
	 *        The file name (as created / uploaded)
	 * @param size
	 *        The file size (in bytes).
	 * @param type
	 *        The mime type.
	 * @param content
	 *        The file content stream.
	 * @param holder
	 *        The ToolItemReference for the tool item holding the reference.
	 * @param security
	 *        The publication security for the reference.
	 * @return The Reference to the new File object.
	 */
	Reference add(String name, int size, String type, InputStream content, ToolItemReference holder, Role security);

	/**
	 * Find existing references in the content html (by their placeholder URLs, matched to existing references from this site/tool/itemId address). If any are not found, they are created.
	 * 
	 * @param content
	 *        The content html.
	 * @param holder
	 *        The ToolItemReference for the tool item holding the reference. The item (in the tool) making the reference.
	 * @param security
	 *        The publication security for the reference.
	 * @return A Set of Reference, may be empty.
	 */
	Set<Reference> extractReferencesFromPlaceholders(String content, ToolItemReference holder, Role security);

	/**
	 * Find the reference in the list of references given that has a file that matches this one.
	 * 
	 * @param file
	 *        The file to match.
	 * @param refs
	 *        The references to check.
	 * @return The Reference if found, or null if not.
	 */
	Reference findReferenceWithFile(File file, List<Reference> refs);

	/**
	 * Find the reference in the list of references given that has a file that matches this name.
	 * 
	 * @param name
	 *        The name to match.
	 * @param refs
	 *        The references to check.
	 * @return The Reference if found, or null if not.
	 */
	Reference findReferenceWithName(String name, List<Reference> refs);

	/**
	 * Find the reference in the list of references given that has a placeholder URL that matches this one.
	 * 
	 * @param placeholderUrl
	 *        The placeholder Url to match.
	 * @param refs
	 *        The references to check.
	 * @return The Reference if found, or null if not.
	 */
	Reference findReferenceWithPlaceholderUrl(String placeholderUrl, List<Reference> refs);

	/**
	 * Get the file with this id.
	 * 
	 * @param id
	 *        The file id.
	 * @return The File if found, or null if not found.
	 */
	File getFile(Long id);

	/**
	 * Get the reference with this id
	 * 
	 * @param id
	 *        The reference id.
	 * @return The Reference if found, or null if not found.
	 */
	Reference getReference(Long id);

	/**
	 * Find the user's myFiles reference to the file referenced by this other reference.
	 * 
	 * @param other
	 *        The other reference.
	 * @param myFilesUser
	 *        The user.
	 * @return The user's myFiles reference, or null if not found.
	 */
	Reference getReference(Reference other, User myFilesUser);

	/**
	 * Find all the references to this file.
	 * 
	 * @param file
	 *        The file.
	 * @return A List of References, possibly null.
	 */
	List<Reference> getReferences(File file);

	/**
	 * Find all the references to the file that this reference references.
	 * 
	 * @param ref
	 *        The reference.
	 * @return A List of References; will contain ref.
	 */
	List<Reference> getReferences(Reference ref);

	/**
	 * Find all the references to the file that this reference references, from the tool item.
	 * 
	 * @param ref
	 *        The reference.
	 * @param holder
	 *        The ToolItemReference for the tool item holding the reference.
	 * @return A List of References; may contain ref.
	 */
	List<Reference> getReferences(Reference ref, ToolItemReference holder);

	/**
	 * Find all the references from this tool item.
	 * 
	 * @param holder
	 *        The ToolItemReference for the tool item holding the reference.
	 * @return A List of References; may be empty;
	 */
	List<Reference> getReferences(ToolItemReference holder);

	/**
	 * Find all the references in the user's myFiles.
	 * 
	 * @param myFilesUser
	 *        The user.
	 * @return A List of References; may be empty.
	 */
	List<Reference> getReferences(User myFilesUser);

	/**
	 * If name matches any name in the refs, change name with (-n, before the extension) to be unique.
	 * 
	 * @param name
	 *        The original name.
	 * @param refs
	 *        A list of References.
	 * @return The name made unique for these refs.
	 */
	String makeUniqueName(String name, List<Reference> refs);

	/**
	 * Return the ToolItemReference for the myFiles for this user.
	 * 
	 * @param user
	 * @return
	 */
	ToolItemReference myFileReference(User user);

	/**
	 * Process HTML content that may have embedded references, converting all download URLs to placeholder URLs.
	 * 
	 * @param content
	 *        The HTML content to process.
	 * @return The modified content.
	 */
	String processContentDownloadToPlaceholder(String content);

	/**
	 * Process HTML content that may have embedded references, converting all placeholder URLs to download URLs using some reference held by this tool item.
	 * 
	 * @param content
	 *        The HTML content to process.
	 * @param holder
	 *        The ToolItemReference for the tool item holding the reference.
	 * @return The modified content.
	 */
	String processContentPlaceholderToDownload(String content, ToolItemReference holder);

	/**
	 * Convert all placeholder references to translated file ids
	 * 
	 * @param content
	 *        The content to process.
	 * @param translations
	 *        The translations - keyed by old file id, value is new file id.
	 * @return The converted content.
	 */
	String processContentPlaceholderToPlaceholderTranslated(String content, Map<Long, Long> translations);

	/**
	 * Remove this file reference - if it is the last reference to the file, remove the file.
	 * 
	 * @param ref
	 *        The Reference to remove.
	 */
	void remove(Reference ref);

	/**
	 * Remove all references held by this tool item, except for those in keepers.
	 * 
	 * @param holder
	 *        The ToolItemReference for the tool item holding the reference.
	 * @param keepers
	 *        The set of references to not remove. May be null or empty.
	 */
	void removeExcept(ToolItemReference holder, Set<Reference> keepers);

	/**
	 * Rename the file behind this reference, and all publications of references to this file which share the file's current name
	 * 
	 * @param authenticatedUser
	 *        The user making the change.
	 * @param ref
	 *        The reference to the file to rename.
	 * @param name
	 *        The new name.
	 */
	void rename(User authenticatedUser, Reference ref, String name);

	/**
	 * Replace the body of the file.
	 * 
	 * @param file
	 *        The file to be replaced.
	 * @param size
	 *        The file size (in bytes).
	 * @param type
	 *        The mime type.
	 * @param content
	 *        The file content stream.
	 */
	void replace(File file, int size, String type, InputStream content);

	/**
	 * Add, update or remove a myFiles file referenced by tool content.
	 * 
	 * @param remove
	 *        if true, the file is to be removed.
	 * @param name
	 *        The file name (used when a contents stream is provided).
	 * @param size
	 *        The file size (used when a contents stream is provided).
	 * @param type
	 *        The file mime type (used when a contents stream is provided).
	 * @param user
	 *        The myFiles user.
	 * @param contents
	 *        The new file contents stream.
	 * @param shareRef
	 *        (optional if not contents and name .. type) The file reference to add.
	 * @param currentReferenceId
	 *        The current reference id.
	 * @param holder
	 *        The ToolItemReference for the tool item holding the reference.
	 * @param security
	 *        The security for the item.
	 * @param keepers
	 *        The reference of the content that the tool neeFds to keep.
	 * @return
	 */
	Long saveMyFile(boolean remove, String name, int size, String type, User user, InputStream contents, Reference shareRef, Long currentReferenceId,
			ToolItemReference holder, Role security, Set<Reference> keepers);

	/**
	 * Add or update the contents of a private (not myFiles) file, taking care of the content and embedded references.
	 * 
	 * @param remove
	 *        if true, the file is to be removed.
	 * @param newContent
	 *        The new file contents in string form.
	 * @param name
	 *        The file name (for newContent)
	 * @param type
	 *        The file mime type (for newContent)
	 * @param shareContent
	 *        The new file contents when it is already a file and we are sharing it.
	 * @param currentReferenceId
	 *        The current file reference id.
	 * @param holder
	 *        The ToolItemReference for the tool item holding the reference.
	 * @param embeddedSecurity
	 *        The security for any embedded references in the file.
	 * @param keepers
	 *        The references of and in the content that the tool needs to keep.
	 * @return The new file reference id.
	 */
	Long savePrivateFile(boolean remove, String newContent, String name, String type, File shareContent, Long currentReferenceId,
			ToolItemReference holder, Role embeddedSecurity, Set<Reference> keepers);

	/**
	 * Check the name as a valid name for a file.
	 * 
	 * @param name
	 *        The name to check.
	 * @return true if it is valid, false if not.
	 */
	boolean validFileName(String name);
}

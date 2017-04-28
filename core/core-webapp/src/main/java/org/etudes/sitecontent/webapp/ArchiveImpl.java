/**********************************************************************************
 * $URL: https://source.etudes.org/svn/serenity/trunk/core/core-webapp/src/main/java/org/etudes/sitecontent/webapp/ArchiveImpl.java $
 * $Id: ArchiveImpl.java 10086 2015-02-18 03:43:41Z ggolden $
 ***********************************************************************************
 *
 * Copyright (c) 2009, 2010, 2011, 2012, 2013, 2015 Etudes, Inc.
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

import java.io.BufferedReader;
import java.io.BufferedWriter;
import java.io.FileInputStream;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.io.OutputStreamWriter;
import java.io.Writer;
import java.util.ArrayList;
import java.util.Collection;
import java.util.Date;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.regex.Matcher;
import java.util.regex.Pattern;
import java.util.zip.ZipEntry;
import java.util.zip.ZipFile;
import java.util.zip.ZipOutputStream;

import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;
import org.etudes.file.api.File;
import org.etudes.file.api.FileService;
import org.etudes.file.api.Reference;
import org.etudes.service.api.Services;
import org.etudes.sitecontent.api.Archive;
import org.etudes.sitecontent.api.Artifact;
import org.etudes.tool.api.Tool;

// TODO: write directly to a zip file

/**
 * ArchiverHandler
 */
public class ArchiveImpl implements Archive
{
	protected class Info
	{
		String key;

		int next;

		String type;

		Object value;
	}

	// Note on types: S-string, [-array string, M-map, C-collection of string, T-set of string, L-Long, I-Integer, B-Boolean, F-Float, A-Date, X-collection of map, Z-file, R-set of References (file id) r-single reference (file id)

	/** Our log. */
	private static Log M_log = LogFactory.getLog(ArchiveImpl.class);

	/** artifact id generator. */
	protected long artifactId = 1;

	/** The artifacts. */
	protected List<Artifact> artifacts = new ArrayList<Artifact>();

	/** The file path to our root directory. */
	protected String filePath = null;

	/** Translations of old style source file CHS references to destination file ids. */
	protected Map<String, Long> oldTranslations = new HashMap<String, Long>();

	/** Translations of source file ids to destination file ids. */
	protected Map<Long, Long> translations = new HashMap<Long, Long>();

	/** The open zip file for reading. */
	protected ZipFile zip = null;

	@Override
	public void archive(Artifact artifact)
	{
		// write this out
		write((ArtifactImpl) artifact);

		// add to the manifest
		this.artifacts.add((ArtifactImpl) artifact);
	}

	@Override
	public Artifact newArtifact(Tool tool, String type)
	{
		ArtifactImpl rv = new ArtifactImpl(this, this.artifactId++, tool, type);

		return rv;
	}

	@Override
	public String translateContentBody(String content)
	{
		content = fileService().processContentPlaceholderToPlaceholderTranslated(content, this.translations);

		// translations from old style CHS references
		for (Map.Entry<String, Long> translation : this.oldTranslations.entrySet())
		{
			content = content.replaceAll(Pattern.quote(translation.getKey()), Matcher.quoteReplacement("/file/" + translation.getValue().toString()));
		}

		return content;
	}

	/**
	 * Clear the way for the archives folder - delete the folder or file that is there.
	 * 
	 * @param loc
	 *        The file location we want to make clear.
	 */
	protected void clear(java.io.File loc)
	{
		if (loc.isDirectory())
		{
			java.io.File[] files = loc.listFiles();
			for (java.io.File f : files)
			{
				f.delete();
			}
		}

		loc.delete();
	}

	/**
	 * Close up after all reading is done.
	 */
	protected void close()
	{
		if (this.zip != null)
		{
			try
			{
				this.zip.close();
				this.zip = null;
			}
			catch (IOException e)
			{
				M_log.warn("close: " + e);
			}
		}
	}

	/**
	 * Finish the creation of the archive.
	 */
	protected void complete()
	{
		// create the manifest
		writeManifest();

		// zip up the folder
		writeZip(this.filePath);

		// remove the unzip folder
		java.io.File dir = new java.io.File(this.filePath);
		clear(dir);
	}

	/**
	 * Compose a key and value, where the value is a collection of strings.
	 * 
	 * @param key
	 *        The key.
	 * @param value
	 *        The collection.
	 * @param artifact
	 *        The artifact.
	 * @return The composed collection.
	 */
	@SuppressWarnings("unchecked")
	protected String composeCollection(String key, Collection<Object> value, Artifact artifact)
	{
		if (value.isEmpty()) return "";

		// put the values into a string
		StringBuilder buf = new StringBuilder();
		buf.append(Integer.toString(value.size()));
		buf.append(":");
		String type = "C";
		for (Object o : value)
		{
			if (o instanceof String)
			{
				buf.append(composeString(null, (String) o, null));
			}

			else if (o instanceof Map)
			{
				type = "X";
				buf.append(composeMap(null, (Map<String, Object>) o, artifact));
			}

			else
			{
				M_log.warn("composeCollection: unknown type: key: " + key + " value: " + o.getClass());
			}
		}

		return composeString(key, buf.toString(), type);
	}

	/**
	 * Compose a key and value, where the value is a map of strings, string[]s, maps or collections.
	 * 
	 * @param masterKey
	 *        The key.
	 * @param map
	 *        The map.
	 * @param artifact
	 *        The artifact.
	 * @return The composed map.
	 */
	@SuppressWarnings("unchecked")
	protected String composeMap(String masterKey, Map<String, Object> map, Artifact artifact)
	{
		StringBuilder buf = new StringBuilder();
		for (Map.Entry<String, Object> entry : map.entrySet())
		{
			String key = entry.getKey();
			Object value = entry.getValue();

			// skip any null values
			if (value == null)
				continue;

			// collection handling
			else if (value instanceof Map)
			{
				buf.append(composeMap(key, (Map<String, Object>) value, artifact));
			}

			// collection handling
			else if (value instanceof Collection)
			{
				buf.append(composeCollection(key, (Collection<Object>) value, artifact));
			}

			else if (value instanceof String[])
			{
				buf.append(composeStrings(key, (String[]) value));
			}

			// stream handling
			else if (value instanceof InputStream)
			{
				buf.append(composeStream(key, (InputStream) value, artifact));
			}

			// otherwise treat it as a string
			else if (value instanceof String)
			{
				buf.append(composeString(key, (String) value, "S"));
			}

			else if (value instanceof Long)
			{
				buf.append(composeString(key, value.toString(), "L"));
			}

			else if (value instanceof Integer)
			{
				buf.append(composeString(key, value.toString(), "I"));
			}

			else if (value instanceof Boolean)
			{
				buf.append(composeString(key, value.toString(), "B"));
			}

			else if (value instanceof Float)
			{
				buf.append(composeString(key, value.toString(), "F"));
			}

			else if (value instanceof Date)
			{
				buf.append(composeString(key, Long.toString(((Date) value).getTime()), "A"));
			}

			else if (value instanceof Reference)
			{
				Long id = ((Reference) value).getFile().getId();
				buf.append(composeString(key, id.toString(), "r"));
			}

			else if (value instanceof File)
			{
				Long id = ((File) value).getId();
				buf.append(composeString(key, id.toString(), "r"));
			}

			else
			{
				M_log.warn("composeMap: unknown type: key: " + key + " value: " + value);
			}
		}

		return composeString(masterKey, buf.toString(), "M");
	}

	/**
	 * Compose a key and value, where the value is a set of references to files.
	 * 
	 * @param key
	 *        The key.
	 * @param value
	 *        The collection.
	 * @return The composed collection.
	 */
	protected String composeReferenceSet(String key, Set<File> value)
	{
		if (value.isEmpty()) return "";

		// put the values into a string: format: file id
		StringBuilder buf = new StringBuilder();
		buf.append(Integer.toString(value.size()));
		buf.append(":");
		for (File f : value)
		{
			String str = f.getId().toString();
			buf.append(Integer.toString(str.length()));
			buf.append(":");
			buf.append(str);
		}

		return composeString(key, buf.toString(), "R");
	}

	/**
	 * Compose a key and value, where the value is a stream to end up in a separate file; the key value has the file name.
	 * 
	 * @param key
	 *        The key.
	 * @param in
	 *        The stream.
	 * @param artifact
	 *        The artifact.
	 * @return the composed string.
	 */
	protected String composeStream(String key, InputStream in, Artifact artifact)
	{
		if (in == null) return "";

		String fName = this.filePath + ((ArtifactImpl) artifact).getFileName() + ((ArtifactImpl) artifact).getNextFileSuffix();

		// stream to a file
		writeFile(in, fName);

		StringBuilder buf = new StringBuilder();

		// the length of our key and value (the relative file name)
		fName = fName.substring(fName.lastIndexOf("/") + 1);
		int len = key.length() + 1 + fName.length() + 2;

		// write the length as characters
		buf.append(Integer.toString(len));
		buf.append(":");

		// write the key
		buf.append(key);
		buf.append(":");

		// write the type
		buf.append("Z:");

		// write the value
		buf.append(fName);

		return buf.toString();
	}

	/**
	 * Compose a string for output
	 * 
	 * @param key
	 *        The key.
	 * @param value
	 *        The string.
	 * @return The composed string.
	 */
	protected String composeString(String key, String value, String type)
	{
		// Note: we don't want to trim - we want to reproduce the content exactly. -ggolden
		// value = value.trim();
		if (value.length() == 0) return "";

		StringBuilder buf = new StringBuilder();

		// the length of our key and type and value
		int len = value.length();
		if (key != null)
		{
			len += key.length() + 1 + type.length() + 1;
		}

		// write the length
		buf.append(Integer.toString(len));
		buf.append(":");

		// write the key and type
		if (key != null)
		{
			buf.append(key);
			buf.append(":");

			buf.append(type);
			buf.append(":");
		}

		// write the value
		buf.append(value);

		return buf.toString();
	}

	/**
	 * Compose a key and value, where the value is an array of strings.
	 * 
	 * @param out
	 *        The writer.
	 * @param key
	 *        The key.
	 * @param value
	 *        The String[].
	 */
	protected String composeStrings(String key, String[] value)
	{
		if (value.length == 0) return "";

		// for the value
		StringBuilder buf = new StringBuilder();

		// the number of items
		buf.append(Integer.toString(value.length));
		buf.append(":");

		for (String str : value)
		{
			if (str == null)
			{
				buf.append("N:");
			}
			else
			{
				buf.append(Integer.toString(str.length()));
				buf.append(":");
				buf.append(str);
			}
		}

		return composeString(key, buf.toString(), "[");
	}

	/**
	 * Compose a key and value, where the value is a set of strings.
	 * 
	 * @param key
	 *        The key.
	 * @param value
	 *        The collection.
	 * @return The composed collection.
	 */
	protected String composeStringSet(String key, Set<String> value)
	{
		if (value.isEmpty()) return "";

		// put the values into a string
		StringBuilder buf = new StringBuilder();
		buf.append(Integer.toString(value.size()));
		buf.append(":");
		for (Object c : value)
		{
			String str = c.toString();
			buf.append(Integer.toString(str.length()));
			buf.append(":");
			buf.append(str);
		}

		return composeString(key, buf.toString(), "T");
	}

	/**
	 * Decode a tool from the "type" manifest string of the archive.
	 * 
	 * @param value
	 *        The value to decode.
	 * @return The Tool.
	 */
	protected Tool decodeTool(Info value)
	{
		if (value.type.equals("S"))
		{
			if (value.value.equals("sakai.syllabus")) return Tool.syllabus;
			if (value.value.equals("sakai.resources")) return Tool.myfiles;
			// TODO: others

			return Tool.none;
		}

		else if (value.type.equals("I"))
		{
			Tool tool = Tool.valueOf(Integer.valueOf((String) value.value));
			return tool;
		}

		return Tool.none;
	}

	/**
	 * Pull a collection of maps from the source string.
	 * 
	 * @param source
	 *        The source string.
	 * @return A collection of maps.
	 */
	protected Collection<Map<String, Object>> decomposeCollectionMap(String source)
	{
		// take up to the ":" as count of maps
		int i = source.indexOf(':');
		String str = source.substring(0, i);
		int count = Integer.parseInt(str);
		Collection<Map<String, Object>> collection = new ArrayList<Map<String, Object>>();

		// take each map
		Info inner = new Info();
		inner.next = i + 1;
		while (count-- > 0)
		{
			// get the string holding the map
			inner = this.decomposeString(source, inner.next, false);

			Map<String, Object> map = decomposeMap((String) inner.value);
			collection.add(map);
		}

		return collection;
	}

	/**
	 * Pull a set out of the compose buffer.
	 * 
	 * @param source
	 *        The source buffer.
	 * @param pos
	 *        The starting position
	 * @return an Info with the key, set in value, and next position in source.
	 */
	protected Map<String, Object> decomposeMap(String source)
	{
		Map<String, Object> rv = new HashMap<String, Object>();

		Info info = new Info();
		info.next = 0;
		while (true)
		{
			info = decomposeString(source, info.next, true);
			if (info.key == null) break;

			// type specific processing
			if (info.type.equals("["))
			{
				// string array, staring with a count
				info.value = decomposeStrings((String) info.value);
			}
			else if (info.type.equals("C"))
			{
				// Collection of strings
				String[] strs = decomposeStrings((String) info.value);
				Collection<String> collection = new ArrayList<String>();
				for (String s : strs)
				{
					collection.add(s);
				}
				info.value = collection;
			}
			else if (info.type.equals("M"))
			{
				// Map
				Map<String, Object> map = decomposeMap((String) info.value);
				info.value = map;
			}
			else if (info.type.equals("T"))
			{
				// Set of strings
				String[] strs = decomposeStrings((String) info.value);
				Set<String> set = new HashSet<String>();
				for (String s : strs)
				{
					set.add(s);
				}
				info.value = set;
			}
			// else if (info.type.equals("R"))
			// {
			// // Set of References
			// String[] strs = decomposeStrings((String) info.value);
			// Set<ArchivedReference> set = new HashSet<ArchivedReference>();
			// for (String s : strs)
			// {
			// String[] parts = split(s, "/");
			// set.add(new ArchivedReferenceImpl(Long.valueOf(parts[0]), Long.valueOf(parts[1])));
			// }
			// info.value = set;
			// }
			else if (info.type.equals("r"))
			{
				Long sourceId = Long.valueOf((String) info.value);
				Long newId = this.translations.get(sourceId);
				File file = fileService().getFile(newId);
				info.value = file;
			}

			else if (info.type.equals("X"))
			{
				// Collection of Map
				Collection<Map<String, Object>> collection = decomposeCollectionMap((String) info.value);
				info.value = collection;
			}
			else if (info.type.equals("I"))
			{
				// Integer
				info.value = Integer.valueOf((String) info.value);
			}
			else if (info.type.equals("B"))
			{
				// Boolean
				info.value = Boolean.valueOf((String) info.value);
			}
			else if (info.type.equals("F"))
			{
				// Float
				info.value = Float.valueOf((String) info.value);
			}
			else if (info.type.equals("L"))
			{
				// Long
				info.value = Long.valueOf((String) info.value);
			}
			else if (info.type.equals("A"))
			{
				// Date
				info.value = new Date(Long.valueOf((String) info.value).longValue());
			}

			rv.put(info.key, info.value);
		}

		return rv;
	}

	/**
	 * Pull a set ("T" strings, "R" longs - referenced file ids) out of the compose buffer.
	 * 
	 * @param source
	 *        The source buffer.
	 * @param pos
	 *        The starting position
	 * @return an Info with the key, set in value, and next position in source.
	 */
	protected Info decomposeSet(String source, int pos)
	{
		// get the next string - sets the key and next
		Info rv = decomposeString(source, pos, true);

		// pull the set out of the value - first the count
		source = (String) rv.value;
		int i = source.indexOf(':');
		String str = source.substring(0, i);
		pos = i + 1;
		int count = Integer.parseInt(str);

		// each value - either longs or strings
		Set<Long> setL = new HashSet<Long>();
		Set<String> setS = new HashSet<String>();

		while (count > 0)
		{
			// length
			i = source.indexOf(':', pos);
			str = source.substring(pos, i);
			pos = i + 1;
			int len = Integer.parseInt(str);

			str = source.substring(pos, pos + len);
			pos += len;

			if (rv.type.equals("R"))
			{
				try
				{
					Long l = Long.valueOf((String) str);
					setL.add(l);
				}
				catch (NumberFormatException e)
				{
				}
			}
			else
			{
				setS.add(str);
			}

			count--;
		}

		// replace value with one of the sets
		rv.value = (rv.type.equals("R") ? setL : setS);

		return rv;
	}

	/**
	 * Pull a string out of the reader.
	 * 
	 * @param in
	 *        The reader.
	 * @return an Info with the key, string in value, and next position in source.
	 */
	protected Info decomposeString(BufferedReader in) throws IOException
	{
		Info rv = new Info();
		rv.key = null;
		rv.value = null;
		StringBuilder buf = new StringBuilder();

		// take up to the ":" as length of the following: key : type : value
		while (true)
		{
			int next = in.read();
			if ((next == -1) || (next == ':')) break;

			buf.append((char) next);
		}
		if (buf.length() == 0) return rv;
		int len = Integer.parseInt(buf.toString());
		buf.setLength(0);

		// take the key
		while (true)
		{
			int next = in.read();
			if ((next == -1) || (next == ':')) break;

			buf.append((char) next);
		}
		rv.key = buf.toString();
		buf.setLength(0);

		// take the type
		while (true)
		{
			int next = in.read();
			if ((next == -1) || (next == ':')) break;

			buf.append((char) next);
		}
		rv.type = buf.toString();
		buf.setLength(0);

		// take the value, len - key.length() bytes
		while (buf.length() < len - (rv.key.length() + 1 + rv.type.length() + 1))
		{
			int next = in.read();
			if (next == -1) break;

			buf.append((char) next);
		}
		rv.value = buf.toString();

		return rv;
	}

	/**
	 * Pull a string out of the compose buffer.
	 * 
	 * @param source
	 *        The source buffer.
	 * @param pos
	 *        The starting position
	 * @param keyAndTypeExpected
	 *        true if a key and type is expected, false if not.
	 * @return an Info with the key, set in value, and next position in source.
	 */
	protected Info decomposeString(String source, int pos, boolean keyAndTypeExpected)
	{
		Info rv = new Info();
		rv.key = null;
		rv.value = null;
		rv.type = null;
		if (pos >= source.length()) return rv;

		// take up to the ":" as length of the following: key : type : value
		int i = source.indexOf(':', pos);
		String str = source.substring(pos, i);
		pos = i + 1;
		int len = Integer.parseInt(str);

		if (keyAndTypeExpected)
		{
			// take the key
			i = source.indexOf(':', pos);
			rv.key = source.substring(pos, i);
			pos = i + 1;

			// take the type
			i = source.indexOf(':', pos);
			rv.type = source.substring(pos, i);
			pos = i + 1;

			// take the value, len - key and type length bytes
			rv.value = source.substring(pos, pos + len - (rv.key.length() + 1 + rv.type.length() + 1));

			// the next character to process
			rv.next = pos + len - (rv.key.length() + 1 + rv.type.length() + 1);
		}

		else
		{
			rv.key = "";
			rv.type = "";

			// take the value
			rv.value = source.substring(pos, pos + len);

			// the next character to process
			rv.next = pos + len;
		}

		return rv;
	}

	/**
	 * Pull a string array from a source string.
	 * 
	 * @param source
	 *        The source string.
	 * @return The string array.
	 */
	protected String[] decomposeStrings(String source)
	{
		// take up to the ":" as count of items
		int i = source.indexOf(':');
		String str = source.substring(0, i);
		int pos = i + 1;
		int count = Integer.parseInt(str);

		String[] rv = new String[count];
		for (int index = 0; index < count; index++)
		{
			// the length of the string
			i = source.indexOf(':', pos);
			str = source.substring(pos, i);
			pos = i + 1;
			int len = 0;
			if ("N".equals(str))
			{
				// the string is null
				rv[index] = null;
			}
			else
			{
				len = Integer.parseInt(str);

				// the string
				rv[index] = source.substring(pos, pos + len);
			}

			pos = pos + len;
		}

		return rv;
	}

	protected List<Artifact> getArtifacts()
	{
		return this.artifacts;
	}

	/**
	 * Start a new archive.
	 */
	protected void init()
	{
		// create the root directory for the archive
		java.io.File dir = new java.io.File(this.filePath);

		if (dir.exists())
		{
			// if exists, clear it out
			clear(dir);
		}

		// make sure it exists
		dir.mkdirs();
	}

	/**
	 * Prepare to read by opening the zip file.
	 */
	protected void open()
	{
		String zipName = this.filePath.substring(0, this.filePath.length() - 1) + ".zip";
		try
		{
			this.zip = new ZipFile(zipName);
		}
		catch (IOException e)
		{
			M_log.warn("open: " + e.toString());
		}
	}

	/**
	 * Read in the properties of the artifact.
	 * 
	 * @param artifact
	 *        The artifact.
	 */
	protected void readArtifact(Artifact artifact)
	{
		BufferedReader in = null;

		try
		{
			// find the artifact
			ZipEntry entry = this.zip.getEntry(((ArtifactImpl) artifact).getFileName());
			if (entry != null)
			{
				in = new BufferedReader(new InputStreamReader(this.zip.getInputStream(entry), "UTF-8"));

				// read in the strings
				while (true)
				{
					Info info = decomposeString(in);
					if (info.key == null) break;

					// type specific processing
					if (info.type.equals("["))
					{
						// string array, staring with a count
						info.value = decomposeStrings((String) info.value);
					}
					else if (info.type.equals("C"))
					{
						// Collection of strings
						String[] strs = decomposeStrings((String) info.value);
						Collection<String> collection = new ArrayList<String>();
						for (String s : strs)
						{
							collection.add(s);
						}
						info.value = collection;
					}
					else if (info.type.equals("M"))
					{
						// Map
						Map<String, Object> map = decomposeMap((String) info.value);
						info.value = map;
					}
					else if (info.type.equals("T"))
					{
						// Set of strings
						String[] strs = decomposeStrings((String) info.value);
						Set<String> set = new HashSet<String>();
						for (String s : strs)
						{
							set.add(s);
						}
						info.value = set;
					}
					// else if (info.type.equals("R"))
					// {
					// // Set of References (file ids) TODO:
					// String[] strs = decomposeStrings((String) info.value);
					// Set<ArchivedReference> set = new HashSet<ArchivedReference>();
					// for (String s : strs)
					// {
					// String[] parts = split(s, "/");
					// set.add(new ArchivedReferenceImpl(Long.valueOf(parts[0]), Long.valueOf(parts[1])));
					// }
					// info.value = set;
					// }
					else if (info.type.equals("r"))
					{
						Long sourceId = Long.valueOf((String) info.value);
						Long newId = this.translations.get(sourceId);
						File file = fileService().getFile(newId);
						info.value = file;
					}
					else if (info.type.equals("X"))
					{
						// Collection of Map
						Collection<Map<String, Object>> collection = decomposeCollectionMap((String) info.value);
						info.value = collection;
					}
					else if (info.type.equals("I"))
					{
						// Integer
						info.value = Integer.valueOf((String) info.value);
					}
					else if (info.type.equals("B"))
					{
						// Boolean
						info.value = Boolean.valueOf((String) info.value);
					}
					else if (info.type.equals("F"))
					{
						// Float
						info.value = Float.valueOf((String) info.value);
					}
					else if (info.type.equals("D"))
					{
						// Double
						info.value = Double.valueOf((String) info.value);
					}
					else if (info.type.equals("L"))
					{
						// Long
						info.value = Long.valueOf((String) info.value);
					}
					else if (info.type.equals("A"))
					{
						// Date
						info.value = new Date(Long.valueOf((String) info.value).longValue());
					}
					else if (info.type.equals("Z"))
					{
						// stream - zip entry name is the value
						String entryName = (String) info.value;
						ZipEntry fileEntry = this.zip.getEntry(entryName);
						if (fileEntry != null)
						{
							info.value = this.zip.getInputStream(fileEntry);
						}
					}

					artifact.getProperties().put(info.key, info.value);
				}
			}
		}
		catch (IOException e)
		{
			M_log.warn("readArtifact: " + e);
		}
		finally
		{
			if (in != null)
			{
				try
				{
					in.close();
				}
				catch (IOException e)
				{
					M_log.warn("readArtifact/close in: " + e);
				}
			}
		}
	}

	/**
	 * Read the archive from its file.
	 * 
	 * @param include
	 *        A subset of tools - read the artifacts only handled by these tools - if null, read them all.
	 * @param exclude
	 *        A subset of tools - do not read the artifacts handled by these tools - if null, read them all.
	 */
	protected void readArtifacts(Set<Tool> include, Set<Tool> exclude)
	{
		for (Artifact artifact : this.artifacts)
		{
			if ((include != null) && (!include.contains(artifact.getTool()))) continue;
			if ((exclude != null) && (exclude.contains(artifact.getTool()))) continue;

			readArtifact(artifact);
		}
	}

	/**
	 * Read the archive manifest.
	 */
	@SuppressWarnings("unchecked")
	protected void readManifest()
	{
		BufferedReader in = null;
		try
		{
			// find the manifest
			ZipEntry entry = this.zip.getEntry("manifest");
			if (entry != null)
			{
				in = new BufferedReader(new InputStreamReader(this.zip.getInputStream(entry), "UTF-8"));

				// each line is an artifact
				while (true)
				{
					String line = in.readLine();
					if (line == null) break;

					Long id = null;
					Tool tool = null;
					String type = null;

					Info info = decomposeString(line, 0, true); // "id"
					id = Long.valueOf((String) info.value);

					if (info.next < line.length())
					{
						info = decomposeString(line, info.next, true); // "type" was "ref"
						type = (String) info.value;
						// artifact.setReference((String) info.value);
					}

					if (info.next < line.length())
					{
						info = decomposeString(line, info.next, true); // "tool" was "type"
						tool = decodeTool(info);
					}

					// create the artifact
					ArtifactImpl artifact = new ArtifactImpl(this, id, tool, type);

					if (info.next < line.length())
					{
						info = decomposeString(line, info.next, true); // "artifact"
						artifact.setFileName((String) info.value);
					}

					// files
					if (info.next < line.length())
					{
						info = decomposeSet(line, info.next); // "files" was "refs"

						if (info.key.equals("files"))
						{
							artifact.getFileIdsReferenced().addAll((Set<Long>) info.value);
						}

						// for the old style
						else if (info.key.equals("refs"))
						{
							artifact.getFileRefsReferenced().addAll((Set<String>) info.value);
						}
					}

					this.artifacts.add(artifact);

					// update the artifactId in case we generate more
					if (artifact.getId().longValue() >= this.artifactId)
					{
						this.artifactId = artifact.getId().longValue() + 1;
					}
				}
			}
		}
		catch (IOException e)
		{
			M_log.warn("readManifest: " + e);
		}
		finally
		{
			if (in != null)
			{
				try
				{
					in.close();
				}
				catch (IOException e)
				{
					M_log.warn("readManifest/close in: " + e);
				}
			}
		}
	}

	/**
	 * Set the file path to the root directory for the archive (including trailing slash).
	 * 
	 * @param filePath
	 *        The file path.
	 */
	protected void setFilePath(String filePath)
	{
		this.filePath = filePath;
	}

	protected void setTranslations(Map<Long, Long> translations, Map<String, Long> oldTranslations)
	{
		if (translations == null) translations = new HashMap<Long, Long>();
		if (oldTranslations == null) oldTranslations = new HashMap<String, Long>();
		this.translations = translations;
		this.oldTranslations = oldTranslations;
	}

	/**
	 * Write the artifact to a file.
	 * 
	 * @param artifact
	 *        The artifact to write.
	 */
	@SuppressWarnings("unchecked")
	protected void write(ArtifactImpl artifact)
	{
		// the artifact's relative file name
		artifact.setFileName("artifact-" + artifact.getId().toString());

		// the file
		Writer out = null;
		String artifactFileName = this.filePath + artifact.getFileName();
		try
		{
			out = new BufferedWriter(new OutputStreamWriter(new FileOutputStream(artifactFileName), "UTF-8"));
			for (Map.Entry<String, Object> entry : artifact.getProperties().entrySet())
			{
				String key = entry.getKey();
				Object value = entry.getValue();

				// skip any null values
				if (value == null) continue;

				// stream handling
				if (value instanceof InputStream)
				{
					out.write(composeStream(key, (InputStream) value, artifact));
				}

				// map handling
				else if (value instanceof Map)
				{
					out.write(composeMap(key, (Map<String, Object>) value, artifact));
				}

				// collection handling
				else if (value instanceof Collection)
				{
					out.write(composeCollection(key, (Collection<Object>) value, artifact));
				}

				// array handling (strings)
				else if (value instanceof String[])
				{
					out.write(composeStrings(key, (String[]) value));
				}

				else if (value instanceof String)
				{
					out.write(composeString(key, (String) value, "S"));
				}

				else if (value instanceof Long)
				{
					out.write(composeString(key, value.toString(), "L"));
				}

				else if (value instanceof Integer)
				{
					out.write(composeString(key, value.toString(), "I"));
				}

				else if (value instanceof Boolean)
				{
					out.write(composeString(key, value.toString(), "B"));
				}

				else if (value instanceof Float)
				{
					out.write(composeString(key, value.toString(), "F"));
				}

				else if (value instanceof Double)
				{
					out.write(composeString(key, value.toString(), "D"));
				}

				else if (value instanceof Date)
				{
					out.write(composeString(key, Long.toString(((Date) value).getTime()), "A"));
				}

				else if (value instanceof Reference)
				{
					Long id = ((Reference) value).getFile().getId();
					out.write(composeString(key, id.toString(), "r"));
				}

				else if (value instanceof File)
				{
					Long id = ((File) value).getId();
					out.write(composeString(key, id.toString(), "r"));
				}

				else
				{
					M_log.warn("write: unknown type: key: " + key + " value: " + value.getClass());
				}
			}
		}
		catch (IOException e)
		{
			M_log.warn("write: " + e);
		}
		finally
		{
			if (out != null)
			{
				try
				{
					out.close();
				}
				catch (IOException e)
				{
					M_log.warn("write/close: " + e);
				}
			}
		}
	}

	/**
	 * Write a file at path with the contents of the input stream.
	 * 
	 * @param in
	 *        The content stream.
	 * @param path
	 *        The full file name.
	 */
	protected void writeFile(InputStream in, String path)
	{
		FileOutputStream out = null;
		try
		{
			out = new FileOutputStream(path);
			byte[] buffer = new byte[10000];
			while (true)
			{
				int len = in.read(buffer);
				if (len == -1) break;

				out.write(buffer, 0, len);
			}
		}
		catch (IOException e)
		{
			M_log.warn("writeFile: " + e);
		}
		finally
		{
			if (out != null)
			{
				try
				{
					out.close();
				}
				catch (IOException e)
				{
					M_log.warn("writeFile/close out: " + e);
				}
			}

			if (in != null)
			{
				try
				{
					in.close();
				}
				catch (IOException e)
				{
					M_log.warn("writeFile/close in: " + e);
				}
			}
		}
	}

	/**
	 * Write the manifest to a file.
	 */
	protected void writeManifest()
	{
		// the file
		Writer out = null;
		String artifactFileName = this.filePath + "manifest";
		try
		{
			out = new BufferedWriter(new OutputStreamWriter(new FileOutputStream(artifactFileName), "UTF-8"));

			// write out a line for each artifact
			for (Artifact a : this.artifacts)
			{
				out.write(composeString("id", ((ArtifactImpl) a).getId().toString(), "L"));
				out.write(composeString("type", ((ArtifactImpl) a).getType(), "S")); // was "ref"
				out.write(composeString("tool", a.getTool().getId().toString(), "I")); // was "type" "S"
				out.write(composeString("artifact", ((ArtifactImpl) a).getFileName(), "S"));
				out.write(composeReferenceSet("files", ((ArtifactImpl) a).getReferences())); // was "refs"
				out.write("\n");
			}
		}
		catch (IOException e)
		{
			M_log.warn("writeManifest: " + e);
		}
		finally
		{
			if (out != null)
			{
				try
				{
					out.close();
				}
				catch (IOException e)
				{
					M_log.warn("writeManifest/close: " + e);
				}
			}
		}
	}

	/**
	 * Create a zip of the folder, named the folder name with ".zip"
	 * 
	 * @param root
	 *        The folder path.
	 */
	protected void writeZip(String root)
	{
		ZipOutputStream out = null;
		FileInputStream in = null;

		try
		{
			// the name of the zip file
			String zipName = this.filePath.substring(0, this.filePath.length() - 1) + ".zip";

			// delete the file if present
			java.io.File zip = new java.io.File(zipName);
			if (zip.exists())
			{
				zip.delete();
			}

			// the zip file
			out = new ZipOutputStream(new FileOutputStream(zipName));

			// the archives folder
			java.io.File archives = new java.io.File(this.filePath);

			// zip up all the files in there
			java.io.File[] files = archives.listFiles();
			for (java.io.File f : files)
			{
				// get the file
				in = new FileInputStream(f);

				// add an entry in the zip
				out.putNextEntry(new ZipEntry(f.getName()));

				// read from the file into the zip
				byte[] buffer = new byte[10000];
				while (true)
				{
					int len = in.read(buffer);
					if (len == -1) break;

					out.write(buffer, 0, len);
				}

				// close the zip entry
				out.closeEntry();

				// close the file
				in.close();
				in = null;
			}
		}
		catch (IOException e)
		{
			M_log.warn("writeZip" + e);
		}
		finally
		{
			if (in != null)
			{
				try
				{
					in.close();
				}
				catch (IOException e)
				{
					M_log.warn("writeZip/close in" + e);
				}
			}

			if (out != null)
			{
				try
				{
					out.close();
				}
				catch (IOException e)
				{
					M_log.warn("writeZip/close out" + e);
				}
			}
		}
	}

	/**
	 * @return The registered FileService.
	 */
	private FileService fileService()
	{
		return (FileService) Services.get(FileService.class);
	}
}

/**********************************************************************************
 * $URL: https://source.etudes.org/svn/serenity/trunk/core/core-api/src/main/java/org/etudes/util/StringUtil.java $
 * $Id: StringUtil.java 9491 2014-12-08 21:42:09Z ggolden $
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

package org.etudes.util;

import java.util.ArrayList;
import java.util.Collection;
import java.util.Iterator;
import java.util.List;

/**
 * <p>
 * StringUtil collects together some string utility classes.
 * </p>
 */
public class StringUtil
{
	/**
	 * Determine if a String is contained in a String Collection
	 * 
	 * @param stringCollection
	 *        The collection of (String) to scan
	 * @param value
	 *        The value to look for
	 * @return true if the string was found
	 */
	public static boolean contains(Collection<String> stringCollection, String value)
	{
		if (stringCollection == null || value == null) return false;
		if (value.length() == 0) return false;
		for (Iterator<String> i = stringCollection.iterator(); i.hasNext();)
		{
			String o = i.next();
			if (value.equals(o)) return true;
		}
		return false;
	}

	/**
	 * Determine if a String is contained in a String[]
	 * 
	 * @param stringCollection
	 *        The String[] to scan
	 * @param value
	 *        The value to look for
	 * @return true if the string was found
	 */
	public static boolean contains(String[] stringCollection, String value)
	{
		if (stringCollection == null || value == null) return false;
		if ((stringCollection.length == 0) || (value.length() == 0)) return false;
		for (String s : stringCollection)
		{
			if (value.equals(s)) return true;
		}
		return false;
	}

	/**
	 * Determine if a String is contained in a String [], ignoring case or not as specified
	 * 
	 * @param stringCollection
	 *        The String[] to scan
	 * @param value
	 *        The value to look for
	 * @param ignoreCase
	 *        if true, we will do the compare case insensitive.
	 * @return true if the string was found
	 */
	public static boolean contains(String[] stringCollection, String value, boolean ignoreCase)
	{
		if (stringCollection == null || value == null) return false;
		if ((stringCollection.length == 0) || (value.length() == 0)) return false;
		for (String s : stringCollection)
		{
			if (ignoreCase)
			{
				if (value.equalsIgnoreCase(s)) return true;
			}
			else
			{
				if (value.equals(s)) return true;
			}
		}
		return false;
	}

	/**
	 * Determine if a String is contained in a String Collection, ignoring case
	 * 
	 * @param stringCollection
	 *        The collection of (String) to scan
	 * @param value
	 *        The value to look for
	 * @return true if the string was found
	 */
	public static boolean containsIgnoreCase(Collection<String> stringCollection, String value)
	{
		if (stringCollection == null || value == null) return false;
		if (value.length() == 0) return false;
		for (Iterator<String> i = stringCollection.iterator(); i.hasNext();)
		{
			String o = i.next();
			if (value.equalsIgnoreCase(o)) return true;
		}
		return false;
	}

	/**
	 * Check if the target contains the substring anywhere, ignore case.
	 * 
	 * @param target
	 *        The string to check.
	 * @param substring
	 *        The value to check for.
	 * @return true of the target contains the substring anywhere, ignore case, or false if it does not.
	 */
	public static boolean containsIgnoreCase(String target, String substring)
	{
		if ((target == null) || (substring == null)) return false;

		target = target.toLowerCase();
		substring = substring.toLowerCase();
		int pos = target.indexOf(substring);
		return (pos != -1);
	}

	/**
	 * Determine if a String is contained in a String [], ignoring case
	 * 
	 * @param stringCollection
	 *        The String[] to scan
	 * @param value
	 *        The value to look for
	 * @return true if the string was found
	 */
	public static boolean containsIgnoreCase(String[] stringCollection, String value)
	{
		if (stringCollection == null || value == null) return false;
		if ((stringCollection.length == 0) || (value.length() == 0)) return false;
		for (String s : stringCollection)
		{
			if (value.equalsIgnoreCase(s)) return true;
		}
		return false;
	}

	/**
	 * Compare two byte[] for differences, either may be null
	 * 
	 * @param a
	 *        One byte[].
	 * @param b
	 *        The other byte[].
	 * @return true if the byte[]s are different, false if they are the same.
	 */
	public static boolean different(byte[] a, byte[] b)
	{
		// if both null, they are the same
		if ((a == null) && (b == null)) return false;

		// if either are null (they both are not), they are different
		if ((a == null) || (b == null)) return true;

		// if the lengths are different, they are different
		if (a.length != b.length) return true;

		// now we know neither are null, so compare, item for item (order counts)
		for (int i = 0; i < a.length; i++)
		{
			if (a[i] != b[i]) return true;
		}

		// they are NOT different!
		return false;
	}

	/**
	 * Compare two strings for differences, either may be null, ignore case if specified
	 * 
	 * @param a
	 *        One String.
	 * @param b
	 *        The other String.
	 * @param ignoreCase
	 *        if true, we will do the compare case insensitive.
	 * @return true if the strings are different, false if they are the same.
	 */
	public static boolean different(String a, String b, boolean ignoreCase)
	{
		// if both null, they are the same
		if ((a == null) && (b == null)) return false;

		// if either are null (they both are not), they are different
		if ((a == null) || (b == null)) return true;

		// now we know neither are null, so compare
		if (ignoreCase)
		{
			return (!a.equalsIgnoreCase(b));
		}

		return (!a.equals(b));
	}

	/**
	 * Compare two String[] for differences, either may be null
	 * 
	 * @param a
	 *        One String[].
	 * @param b
	 *        The other String[].
	 * @return true if the String[]s are different, false if they are the same.
	 */
	public static boolean different(String[] a, String[] b)
	{
		// if both null, they are the same
		if ((a == null) && (b == null)) return false;

		// if either are null (they both are not), they are different
		if ((a == null) || (b == null)) return true;

		// if the lengths are different, they are different
		if (a.length != b.length) return true;

		// now we know neither are null, so compare, item for item (order counts)
		for (int i = 0; i < a.length; i++)
		{
			if (!a[i].equals(b[i])) return true;
		}

		// they are NOT different!
		return false;
	}

	/**
	 * Format a file size for display.
	 * 
	 * @param size
	 *        The file size in bytes.
	 * @return The formatted file size.
	 */
	public static String formatFileSize(int size)
	{
		if (size < 1024)
		{
			return Integer.toString(size);
		}
		else if (size < 1024 * 1024)
		{
			return Integer.toString(size / 1024) + " kb";
		}
		else
		{
			float s = ((float) size) / (1024f * 1024f);

			// round to two places
			String rv = Float.toString(Math.round(s * 100.0f) / 100.0f);

			// get rid of ".00"
			if (rv.endsWith(".00"))
			{
				rv = rv.substring(0, rv.length() - 3);
			}

			// get rid of ".0"
			if (rv.endsWith(".0"))
			{
				rv = rv.substring(0, rv.length() - 2);
			}

			return rv + " mb";
		}
	}

	/**
	 * Limit the string to a certain number of characters, adding "..." if it was truncated
	 * 
	 * @param value
	 *        The string to limit.
	 * @param length
	 *        the length to limit to (as an int).
	 * @return The limited string.
	 */
	public static String limit(String value, int length)
	{
		StringBuffer buf = new StringBuffer(value);
		if (buf.length() > length)
		{
			buf.setLength(length);
			buf.append("...");
		}

		return buf.toString();
	}

	/**
	 * Like String.split...
	 */
	public static String[] split(String source, String splitter)
	{
		// hold the results as we find them
		List<String> rv = new ArrayList<String>();
		int last = 0;
		int next = 0;
		do
		{
			// find next splitter in source
			next = source.indexOf(splitter, last);
			if (next != -1)
			{
				// isolate from last thru before next
				rv.add(source.substring(last, next));
				last = next + splitter.length();
			}
		}
		while (next != -1);
		if (last < source.length())
		{
			rv.add(source.substring(last, source.length()));
		}

		// convert to array
		return (String[]) rv.toArray(new String[rv.size()]);
	}

	/**
	 * Split the source into two strings at the first occurrence of the splitter Subsequent occurrences are not treated specially, and may be part of the second string.
	 * 
	 * @param source
	 *        The string to split
	 * @param splitter
	 *        The string that forms the boundary between the two strings returned.
	 * @return An array of two strings split from source by splitter.
	 */
	public static String[] splitFirst(String source, String splitter)
	{
		// hold the results as we find them
		List<String> rv = new ArrayList<String>();
		int last = 0;
		int next = 0;

		// find first splitter in source
		next = source.indexOf(splitter, last);
		if (next != -1)
		{
			// isolate from last thru before next
			rv.add(source.substring(last, next));
			last = next + splitter.length();
		}

		if (last < source.length())
		{
			rv.add(source.substring(last, source.length()));
		}

		// convert to array
		return (String[]) rv.toArray(new String[rv.size()]);
	}

	/**
	 * Split the source into two strings at the last occurrence of the splitter. Previous occurrences are not treated specially, and may be part of the first string.
	 * 
	 * @param source
	 *        The string to split
	 * @param splitter
	 *        The string that forms the boundary between the two strings returned.
	 * @return An array of two strings split from source by splitter.
	 */
	public static String[] splitLast(String source, String splitter)
	{
		String start = null;
		String end = null;

		// find last splitter in source
		int pos = source.lastIndexOf(splitter);

		// if not found, return null
		if (pos == -1)
		{
			return null;
		}

		// take up to the splitter for the start
		start = source.substring(0, pos);

		// and the rest after the splitter
		end = source.substring(pos + splitter.length(), source.length());

		String[] rv = new String[2];
		rv[0] = start;
		rv[1] = end;

		return rv;
	}

	/**
	 * Trim blanks, and if nothing left, make null.
	 * 
	 * @param value
	 *        The string to trim.
	 * @return value trimmed of blanks, and if nothing left, made null.
	 */
	public static String trimToNull(String value)
	{
		if (value == null) return null;
		value = value.trim();
		if (value.length() == 0) return null;
		return value;
	}

	/**
	 * Trim blanks, and if nothing left, make null, else lowercase.
	 * 
	 * @param value
	 *        The string to trim.
	 * @return value trimmed of blanks, lower cased, and if nothing left, made null.
	 */
	public static String trimToNullLower(String value)
	{
		if (value == null) return null;
		value = value.trim();
		if (value.length() == 0) return null;
		return value.toLowerCase();
	}

	/**
	 * Trim blanks, and assure there is a value, and it's not null.
	 * 
	 * @param value
	 *        The string to trim.
	 * @return value trimmed of blanks, assuring it not to be null.
	 */
	public static String trimToZero(String value)
	{
		if (value == null) return "";
		value = value.trim();
		return value;
	}

	/**
	 * Trim blanks, and assure there is a value, and it's not null, then lowercase.
	 * 
	 * @param value
	 *        The string to trim.
	 * @return value trimmed of blanks, lower cased, assuring it not to be null.
	 */
	public static String trimToZeroLower(String value)
	{
		if (value == null) return "";
		value = value.trim();
		return value.toLowerCase();
	}

	/**
	 * Reverse the split operation.
	 * 
	 * @param parts
	 *        The parts to combine
	 * @param index
	 *        the index to the fist part to use
	 * @param length
	 *        the number of parts to use
	 * @param splitter
	 *        The between-parts text
	 */
	public static String unsplit(String[] parts, int index, int length, String splitter)
	{
		if (parts == null) return null;
		if ((index < 0) || (index >= parts.length)) return null;
		if (index + length > parts.length) return null;

		StringBuffer buf = new StringBuffer();
		for (int i = index; i < index + length; i++)
		{
			if (parts[i] != null) buf.append(parts[i]);
			buf.append(splitter);
		}

		// remove the trailing splitter
		buf.setLength(buf.length() - splitter.length());
		return buf.toString();
	}

	/**
	 * Reverse the split operation.
	 * 
	 * @param parts
	 *        The parts to combine
	 * @param splitter
	 *        The between-parts text
	 */
	public static String unsplit(String[] parts, String splitter)
	{
		if (parts == null) return null;

		StringBuffer buf = new StringBuffer();
		for (int i = 0; i < parts.length; i++)
		{
			if (parts[i] != null) buf.append(parts[i]);
			if (i < parts.length - 1) buf.append(splitter);
		}

		return buf.toString();
	}
}

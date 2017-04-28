/**********************************************************************************
 * $URL: https://source.etudes.org/svn/serenity/trunk/core/core-api/src/main/java/org/etudes/util/Different.java $
 * $Id: Different.java 9491 2014-12-08 21:42:09Z ggolden $
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

/**
 * Utility class to compute difference considering nulls.
 */
public class Different
{
	/**
	 * Compare two byte[]s for differences, either may be null
	 * 
	 * @param a
	 *        One array.
	 * @param b
	 *        The other array.
	 * @return true if the arrays are different, false if they are the same.
	 */
	public static boolean different(byte[] a, byte[] b)
	{
		// if both null, they are the same
		if ((a == null) && (b == null)) return false;

		// if either are null (they both are not), they are different
		if ((a == null) || (b == null)) return true;

		// now we know neither are null, so compare

		// lengths must match
		if (a.length != b.length) return true;

		// bytes must match
		for (int i = 0; i < a.length; i++)
		{
			if (a[i] != b[i]) return true;
		}

		return false;
	}

	/**
	 * Compare two objects for differences, either may be null
	 * 
	 * @param a
	 *        One object.
	 * @param b
	 *        The other object.
	 * @return true if the object are different, false if they are the same.
	 */
	public static boolean different(Object a, Object b)
	{
		// if both null, they are the same
		if ((a == null) && (b == null)) return false;

		// if either are null (they both are not), they are different
		if ((a == null) || (b == null)) return true;

		// now we know neither are null, so compare
		return (!a.equals(b));
	}

	/**
	 * Compare two strings for differences, ignoring case, either may be null
	 * 
	 * @param a
	 *        One object.
	 * @param b
	 *        The other object.
	 * @return true if the object are different, false if they are the same.
	 */
	public static boolean differentIgnoreCase(String a, String b)
	{
		// if both null, they are the same
		if ((a == null) && (b == null)) return false;

		// if either are null (they both are not), they are different
		if ((a == null) || (b == null)) return true;

		// now we know neither are null, so compare
		return (!a.equalsIgnoreCase(b));
	}

}

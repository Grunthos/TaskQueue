/*
 * @copyright 2012 Philip Warner
 * @license GNU General Public License
 * 
 * This file is part of Book Catalogue.
 *
 * TaskQueue is free software: you can redistribute it and/or modify
 * it under the terms of the GNU General Public License as published by
 * the Free Software Foundation, either version 3 of the License, or
 * (at your option) any later version.
 *
 * TaskQueue is distributed in the hope that it will be useful,
 * but WITHOUT ANY WARRANTY; without even the implied warranty of
 * MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the
 * GNU General Public License for more details.
 *
 * You should have received a copy of the GNU General Public License
 * along with Book Catalogue.  If not, see <http://www.gnu.org/licenses/>.
 */

package net.philipwarner.taskqueue;

import java.io.ByteArrayInputStream;
import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.io.ObjectInputStream;
import java.io.ObjectOutput;
import java.io.ObjectOutputStream;
import java.io.Serializable;
import java.io.StreamCorruptedException;
import java.text.DateFormat;
import java.text.SimpleDateFormat;
import java.util.Date;

public class Utils {
	/** Date format used in displaying and parsing dates in the database */
	private static final DateFormat m_stdDateFormat = new SimpleDateFormat("yyyy-MM-dd HH:mm:ss");

	/**
	 * Utility routine to convert a date to a string.
	 * 
	 * @param date	Date to convert
	 * @return		Formatted string
	 */
	public static String date2string(Date date) {
		return m_stdDateFormat.format(date).toString();
	}
	
	/**
	 * Utility routine to convert a 'standard' date string to a date. Returns current date on failure.
	 * 
	 * @param dateString	String to convert
	 * @return				Resulting Date
	 */
	public static Date string2date(String dateString) {
		Date date;
		try {
			date = m_stdDateFormat.parse(dateString);			
		} catch (Exception e) {
			date = new Date();
		}
		return date;
	}

	/**
	 * Utility routine to convert a Serializable object to a byte array.
	 * 
	 * @param o		Object to convert
	 * @return		Resulting byte array. NULL on failure.
	 */
	public static byte[] serializeObject(Serializable o) {
		ByteArrayOutputStream bos = new ByteArrayOutputStream();
		ObjectOutput out;
		try {
			out = new ObjectOutputStream(bos);
			out.writeObject(o);
			out.close();
		} catch (Exception e) {
			out = null;
		}

		// Get the bytes of the serialized object
		byte[] buf;
		if (out != null) {
			buf = bos.toByteArray();
		} else {
			buf = null; //new byte[]{};
		}
		return buf;
	}

	/**
	 * Special purpose method to ensure that a Legacy object is serialized cas the original object.
	 * 
	 * @param o
	 * @return
	 */
	public static byte[] serializeObject(LegacyEvent o) {
		return o.getOriginal();
	}

	public static class DeserializationException extends Exception {
		private static final long serialVersionUID = -2040548134317746620L;
		public Exception inner;
		DeserializationException(Exception e) {
			super();
			inner = e;
		}
	}
	public static Object deserializeObject(byte[] blob) throws DeserializationException {
		try {
			ObjectInputStream in = new ObjectInputStream(new ByteArrayInputStream(blob)); 
		    return in.readObject();		
		} catch (StreamCorruptedException e) {
			throw new DeserializationException(e);
		} catch (IOException e) {
			throw new DeserializationException(e);
		} catch (ClassNotFoundException e) {
			throw new DeserializationException(e);
		}
	}
}

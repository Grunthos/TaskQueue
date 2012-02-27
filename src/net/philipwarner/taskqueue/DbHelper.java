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

import java.util.Hashtable;

import android.content.Context;
import android.database.sqlite.SQLiteDatabase;
import android.database.sqlite.SQLiteOpenHelper;
/**
 * Standard Android class to handle database open/creatiob.upgrade.
 * 
 * @author Philip Warner
 */
public class DbHelper extends SQLiteOpenHelper {

	// File name for database
	private static final String DB_NAME = "net.philipwarner.taskqueue.database.db";
	// Got to start somewhere
	private static final int DB_VERSION = 1;

	// Domain names for fields in tables. Yes, I mix nomenclatures.
	protected static final String DOM_EXCEPTION = "exception";
	protected static final String DOM_EXCEPTION_DATE = "exception_date";
	protected static final String DOM_FAILURE_REASON = "failure_reason";
	protected static final String DOM_ID = "_id"; // Needs to start with '_' so CursorAdapter understands its an ID
	protected static final String DOM_JOB_ID = "job_id";
	protected static final String DOM_NAME = "name";
	protected static final String DOM_EVENT = "event";
	protected static final String DOM_EVENT_COUNT = "event_count";
	protected static final String DOM_EVENT_DATE = "event_date";
	protected static final String DOM_EVENT_ID = "event_id";
	protected static final String DOM_QUEUE_ID = "queue_id";
	protected static final String DOM_QUEUED_DATE = "queued_date";
	protected static final String DOM_PRIORITY = "priority";
	protected static final String DOM_RETRY_DATE = "retry_date";
	protected static final String DOM_RETRY_COUNT = "retry_count";
	protected static final String DOM_STATUS_CODE = "status_code";
	//protected static final String DOM_STRICT = "strict";
	protected static final String DOM_TASK = "task";
	protected static final String DOM_TASK_ID = "task_id";
	//protected static final String DOM_TYPE = "type";
	protected static final String DOM_VALUE = "value";

	// Table definitions
	protected static final String TBL_CONFIG = "config";
	private static final String TBL_CONFIG_DEFN = DOM_ID + " integer primary key autoincrement,"
													+ DOM_NAME + " text not null,"
													+ DOM_VALUE + " blob not null";

	// Queue definition. In a future version, implement LIFO and FIFO queues (we just do FIFO in 
	// version 1). Also implement 'strict' queues, where a 'strict' FIFO queue requires that
	// all entries SUCCEED in order.
	//
	// Also in a future version, consider adding inter-job dependencies to avoid the need for 'strict'
	// queues. Most jobs can run independently, but some require specific predecessors.
	//
	protected static final String TBL_QUEUE = "queue";
	private static final String TBL_QUEUE_DEFN = DOM_ID + " integer primary key autoincrement,\n"
													+ DOM_NAME + " String";
	private static final String[] TBL_QUEUE_IX1 = new String[] { TBL_QUEUE, "unique", DOM_ID };
	private static final String[] TBL_QUEUE_IX2 = new String[] { TBL_QUEUE, "unique", DOM_NAME};

	// Scheduled task definition.
	protected static final String TBL_TASK = "task";
	private static final String TBL_TASK_DEFN = DOM_ID + " integer primary key autoincrement,\n"
													+ DOM_QUEUE_ID + " integer not null references " + TBL_QUEUE + ",\n"
													+ DOM_QUEUED_DATE + " datetime default current_timestamp,\n"
													+ DOM_PRIORITY + " long default 0,\n"
													+ DOM_STATUS_CODE + " char default 'Q',\n"
													+ DOM_RETRY_DATE + " datetime default current_timestamp,\n"
													+ DOM_RETRY_COUNT + " int default 0,\n"
													+ DOM_FAILURE_REASON + " text,\n"
													+ DOM_EXCEPTION + " blob,\n"
													+ DOM_TASK + " blob not null";

	private static final String[] TBL_TASK_IX1 = new String[] { TBL_TASK, "unique", DOM_ID };
	private static final String[] TBL_TASK_IX2 = new String[] { TBL_TASK, "", DOM_STATUS_CODE, DOM_QUEUE_ID, DOM_RETRY_DATE};
	private static final String[] TBL_TASK_IX3 = new String[] { TBL_TASK, "", DOM_STATUS_CODE, DOM_QUEUE_ID, DOM_RETRY_DATE, DOM_PRIORITY};

	// Event table definition.
	protected static final String TBL_EVENT = "event";
	private static final String TBL_EVENT_DEFN = DOM_ID + " integer primary key autoincrement,\n"
													+ DOM_TASK_ID + " integer references " + TBL_TASK + ",\n"
													+ DOM_EVENT + " blob not null,\n"
													+ DOM_EVENT_DATE + " datetime default current_timestamp"
													;

	private static final String[] TBL_EVENTS_IX1 = new String[] { TBL_EVENT, "unique", DOM_ID };
	private static final String[] TBL_EVENTS_IX2 = new String[] { TBL_EVENT, "unique", DOM_EVENT_DATE, DOM_ID };
	private static final String[] TBL_EVENTS_IX3 = new String[] { TBL_EVENT, "", DOM_TASK_ID, DOM_ID };

	/*
	// Task_Events...for events that link to tasks.
	protected static final String TBL_TASK_EVENTS = "task_events";
	private static final String TBL_TASK_EVENTS_DEFN = DOM_ID + " integer primary key autoincrement,\n"
													+ DOM_TASK_ID + " integer not null references " + TBL_TASK + ",\n"
													+ DOM_EVENT_ID + " integer not null references " + TBL_EVENT
													;

	private static final String[] TBL_TASK_EVENTS_IX1 = new String[] { TBL_TASK_EVENTS, "unique", DOM_ID };
	private static final String[] TBL_TASK_EVENTS_IX2 = new String[] { TBL_TASK_EVENTS, "unique", DOM_TASK_ID, DOM_EVENT_ID };
	private static final String[] TBL_TASK_EVENTS_IX3 = new String[] { TBL_TASK_EVENTS, "unique", DOM_EVENT_ID, DOM_TASK_ID };
	*/

	// Collection of all table definitions
	protected static final String[] m_tables = new String[] { 
		TBL_CONFIG, TBL_CONFIG_DEFN,
		TBL_QUEUE, TBL_QUEUE_DEFN,
		TBL_TASK, TBL_TASK_DEFN,
		TBL_EVENT, TBL_EVENT_DEFN,
		/* TBL_TASK_EVENTS, TBL_TASK_EVENTS_DEFN, */
	};

	protected static final String[][] m_indices = new String[][] { 
		TBL_QUEUE_IX1,
		TBL_QUEUE_IX2,
		TBL_TASK_IX1,
		TBL_TASK_IX2,
		TBL_TASK_IX3,
		TBL_EVENTS_IX1,
		TBL_EVENTS_IX2,
		TBL_EVENTS_IX3,
/*		TBL_TASK_EVENTS_IX1,
		TBL_TASK_EVENTS_IX2,
		TBL_TASK_EVENTS_IX3,
*/
		};

	/**
	 * Constructor. Call superclass using locally defined name & version.
	 * 
	 * @param context Context
	 */
	protected DbHelper(Context context) {
		super(context, DB_NAME, null, DB_VERSION);
	}

	/**
	 * Create tables and indexes; this is perhaps more complex than necessary, but it makes
	 * the definitions easier.
	 */
	@Override
	public void onCreate(SQLiteDatabase db) {
		for(int i = 0; i < m_tables.length; i=i+2 ) {
			db.execSQL("Create Table " + m_tables[i] + "(" + m_tables[i+1] + ")");
		}
		// Turn on foreign key support so that CASCADE works.
		db.execSQL("PRAGMA foreign_keys = ON");
		
		// Indices
		
		// We have one counter per table to manage index numeric suffixes.
		Hashtable<String,Integer> counters = new Hashtable<String,Integer>();
		// Loop through definitions.
		for(String[] defn : m_indices) {
			// Get prefix fields
			final String tbl = defn[0];
			final String qual = defn[1];
			// See how many are already defined for this table; get next counter value
			int cnt;
			if (counters.containsKey(tbl)) {
				cnt = counters.get(tbl) + 1;
			} else {
				cnt = 1;
			}
			// Save the value
			counters.put(tbl, cnt);
			
			// Start definition using first field.
			String sql = "Create " + qual + " Index " + tbl + "_IX" + cnt + " On " + tbl + "(\n";
			sql += "    " + defn[2];
			// Loop through remaining fields, if any
			for(int i = 3; i < defn.length; i++) {
				sql += ",\n    " + defn[i];
			}
			sql += ");\n";
			// Define it
			db.execSQL(sql);
		}
	}

	@Override
	/**
	 * Called to upgrade DB. Currently no upgrades.
	 */
	public void onUpgrade(SQLiteDatabase db, int oldVersion, int newVersion) {
		if (oldVersion != newVersion)
			throw new RuntimeException("Unsupported version upgrade");
		// Turn on foreign key support so that CASCADE works.
		db.execSQL("PRAGMA foreign_keys = ON");
	}

	@Override
	public void onOpen(SQLiteDatabase db) {
		// Turn on foreign key support so that CASCADE works.
		db.execSQL("PRAGMA foreign_keys = ON");
	}

}

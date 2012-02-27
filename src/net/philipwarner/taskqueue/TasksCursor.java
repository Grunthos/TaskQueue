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

import java.util.Date;

import net.philipwarner.taskqueue.Utils.DeserializationException;

import android.database.Cursor;
import android.database.sqlite.SQLiteDatabase;
import android.database.sqlite.SQLiteDatabase.CursorFactory;
import static net.philipwarner.taskqueue.DbHelper.*;
import android.database.sqlite.SQLiteCursorDriver;
import android.database.sqlite.SQLiteQuery;

/**
 * Cursor subclass used to make accessing Tasks a little easier.
 * 
 * @author Philip Warner
 * 
 */
public class TasksCursor extends BindableItemSQLiteCursor {

	/** Static Factory object to create the custom cursor */
	private static final CursorFactory m_factory = new CursorFactory() {
		@Override
		public Cursor newCursor(SQLiteDatabase db,
				SQLiteCursorDriver masterQuery, String editTable,
				SQLiteQuery query) {
			return new TasksCursor(db, masterQuery, editTable, query);
		}
	};

	private static String m_failedTasksQuery = "Select *, "
			+ " (Select Count(*) from " + TBL_EVENT + " e Where e." + DOM_TASK_ID + "=t." + DOM_ID + ") as " + DOM_EVENT_COUNT
			+ " From " + TBL_TASK + " t "
			+ " Where " + DOM_STATUS_CODE + " = 'W' Order by " + DOM_ID + " desc";

	private static String m_allTasksQuery = "Select *, "
			+ " (Select Count(*) from " + TBL_EVENT + " e Where e." + DOM_TASK_ID + "=t." + DOM_ID + ") as " + DOM_EVENT_COUNT
			+ " From " + TBL_TASK + " t "
			+ " Order by " + DOM_ID + " desc";

	private static String m_activeTasksQuery = "Select *, "
			+ " (Select Count(*) from " + TBL_EVENT + " e Where e." + DOM_TASK_ID + "=t." + DOM_ID + ") as " + DOM_EVENT_COUNT
			+ " From " + TBL_TASK + " t "
			+ " Where " + DOM_STATUS_CODE + " <> 'S' Order by " + DOM_ID + " desc";

	private static String m_queuedTasksQuery = "Select *, "
			+ " (Select Count(*) from " + TBL_EVENT + " e Where e." + DOM_TASK_ID + "=t." + DOM_ID + ") as " + DOM_EVENT_COUNT
			+ " From " + TBL_TASK + " t "
			+ " Where " + DOM_STATUS_CODE + " = 'Q' Order by " + DOM_ID + " desc";

	public enum TaskCursorSubtype {all, failed, active, queued};

	/**
	 * Static method to get a TaskExceptions Cursor.
	 * 
	 * @param db
	 *            Database
	 * @param taskId
	 *            ID of the task whose exceptions we want
	 * 
	 * @return A new TaskExceptionsCursor
	 */
	public static TasksCursor fetchTasks(SQLiteDatabase db, TaskCursorSubtype type) {
		String query;
		switch(type) {
			case all:
				query = m_allTasksQuery;
				break;
			case queued:
				query = m_queuedTasksQuery;
				break;
			case failed:
				query = m_failedTasksQuery;
				break;
			case active:
				query = m_activeTasksQuery;
				break;
			default:
				throw new RuntimeException("Unexpected cursor subtype specified: " + type);
		}
		return (TasksCursor) db.rawQueryWithFactory(m_factory, query, new String[] {}, "");
	}
	
	/**
	 * Constructor, based on SQLiteCursor constructor
	 * 
	 * @param db		Database
	 * @param driver	?
	 * @param editTable	?
	 * @param query		?
	 */
	public TasksCursor(SQLiteDatabase db, SQLiteCursorDriver driver,
			String editTable, SQLiteQuery query) {
		super(db, driver, editTable, query);
	}

	/** Column number of ID column. */
	private static int m_idCol = -1;

	/**
	 * Accessor for ID field.
	 * 
	 * @return row id
	 */
	public long getId() {
		if (m_idCol == -1)
			m_idCol = this.getColumnIndex(DOM_ID);
		return getLong(m_idCol);
	}

	/** Column number of Queue_id column. */
	private static int m_queueIdCol = -1;

	/**
	 * Accessor for Task ID field.
	 * 
	 * @return task id
	 */
	public long getQueueId() {
		if (m_queueIdCol == -1)
			m_queueIdCol = this.getColumnIndex(DOM_QUEUE_ID);
		return getLong(m_queueIdCol);
	}

	/** Column number of date column. */
	private static int m_queuedDateCol = -1;

	/**
	 * Accessor for Exception date field.
	 * 
	 * @return Exception date
	 */
	public Date getQueuedDate() {
		if (m_queuedDateCol == -1)
			m_queuedDateCol = this.getColumnIndex(DOM_QUEUED_DATE);
		return Utils.string2date(getString(m_queuedDateCol));
	}

	/** Column number of retry date column. */
	private static int m_retryDateCol = -1;

	/**
	 * Accessor for retry date field.
	 * 
	 * @return retry date
	 */
	public Date getRetryDate() {
		if (m_retryDateCol == -1)
			m_retryDateCol = this.getColumnIndex(DOM_RETRY_DATE);
		return Utils.string2date(getString(m_retryDateCol));
	}

	/** Column number of retry count column. */
	private static int m_retryCountCol = -1;

	/**
	 * Accessor for retry count field.
	 * 
	 * @return retry count
	 */
	public long getRetryCount() {
		if (m_retryCountCol == -1)
			m_retryCountCol = this.getColumnIndex(DOM_RETRY_COUNT);
		return getLong(m_retryCountCol);
	}


	/** Column number of retry count column. */
	private static int m_statusCodeCol = -1;

	/**
	 * Accessor for retry count field.
	 * 
	 * @return retry count
	 */
	public String getStatusCode() {
		if (m_statusCodeCol == -1)
			m_statusCodeCol = this.getColumnIndex(DOM_STATUS_CODE);
		return getString(m_statusCodeCol);
	}

	/** Column number of reason column. */
	private static int m_reasonCol = -1;

	/**
	 * Accessor for reason field.
	 * 
	 * @return reason
	 */
	public String getReason() {
		if (m_reasonCol == -1)
			m_reasonCol = this.getColumnIndex(DOM_FAILURE_REASON);
		return getString(m_reasonCol);
	}
	
	
	/** Column number of Exception column. */
	private static int m_exceptionCol = -1;

	/**
	 * Accessor for Exception field.
	 * 
	 * @return TaskException object
	 * @throws DeserializationException
	 */
	public Exception getException() throws DeserializationException {
		if (m_exceptionCol == -1)
			m_exceptionCol = this.getColumnIndex(DOM_EXCEPTION);
		return (Exception) Utils.deserializeObject(getBlob(m_exceptionCol));
	}

	/** Column number of Exception column. */
	private static int m_taskCol = -2;

	/**
	 * Accessor for Exception field.
	 * 
	 * @return TaskException object
	 * @throws DeserializationException
	 */
	public Task getTask() {
		if (m_taskCol == -2)
			m_taskCol = this.getColumnIndex(DOM_TASK);
		Task t;
		byte[] blob = getBlob(m_taskCol);
		try {
			t = (Task) Utils.deserializeObject(blob);
		} catch (DeserializationException de) {
			t = QueueManager.getQueueManager().newLegacyTask(blob);
		}
		t.setId(this.getId());
		return t;
	}

	/** Column number of NoteCount column. */
	private static int m_noteCountCol = -1;

	/**
	 * Accessor for Exception field.
	 * 
	 * @return TaskException object
	 * @throws DeserializationException
	 */
	public int getNoteCount() {
		if (m_noteCountCol == -1)
			m_noteCountCol = this.getColumnIndex(DOM_EVENT_COUNT);
		return getInt(m_noteCountCol);
	}

	@Override
	public BindableItem getBindableItem() {
		return getTask();
	}

}

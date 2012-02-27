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

import android.content.Context;

/**
 * Extends the Task object to add a run(...) method that avoids the need to implement a runOneTask() method
 * in a subclassed QueueManager.
 * 
 * @author Philip Warner
 *
 */
public abstract class RunnableTask extends Task {

	public RunnableTask(String description) {
		super(description);
	}

	private static final long serialVersionUID = 5399775565316896935L;

	public abstract boolean run(QueueManager manager, Context c);

}

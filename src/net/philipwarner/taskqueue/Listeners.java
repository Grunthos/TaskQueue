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

public class Listeners {

	public static enum EventActions { created, deleted, updated };
	public static enum TaskActions { created, deleted, updated, completed, running, waiting };

	public interface OnEventChangeListener {
		public void onEventChange(Event event, EventActions action);
	}
	public interface OnTaskChangeListener {
		public void onTaskChange(Task task, TaskActions action);
	}
}

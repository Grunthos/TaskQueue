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

/**
 * Class to make building a 'context menu' from an AlertDialog a little easier.
 * Used in Event.buildDialogItems and related Activities.
 * 
 * @author Philip Warner
 *
 */
public class ContextDialogItem implements CharSequence {
	public String name;
	public Runnable handler;
	public ContextDialogItem(String name, Runnable handler ) {
		this.name = name;
		this.handler = handler;
	}
	@Override
	public String toString() {
		return name;
	}
	@Override
	public char charAt(int index) {
		return name.charAt(index);
	}
	@Override
	public int length() {
		return name.length();
	}
	@Override
	public CharSequence subSequence(int start, int end) {
		return name.subSequence(start, end);
	}
}


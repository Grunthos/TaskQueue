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

import java.lang.ref.WeakReference;
import java.util.ArrayList;
import java.util.Hashtable;
import android.app.Notification;
import android.app.NotificationManager;
import android.app.PendingIntent;
import android.app.Service;
import android.content.Intent;
import android.os.Binder;
import android.os.Bundle;
import android.os.Handler;
import android.os.IBinder;
import android.os.Message;
import net.philipwarner.taskqueue.DbAdapter;
import net.philipwarner.taskqueue.Listeners.*;
import net.philipwarner.taskqueue.TasksCursor.TaskCursorSubtype;

/**
 * Class to handle service-level aspects of the queues. 
 * 
 * Each defined queue results in a fresh Queue object being created in its own thread; the QueueManager 
 * creates these. Queue objects last until there are no entries left in the queue.
 * 
 * ENHANCE: Split QueueManager into *Manager and *Service, and autocreate QueueManager which will start Service when queues need to execute. Service stops after last queue.
 * ENHANCE: Add a 'requiresNetwork()' task property
 * ENHANCE: Register a BroadcastReceiver for ConnectivityManager.CONNECTIVITY_ACTION. In the onReceive handler you can call NetworkInfo info = (NetworkInfo) intent.getParcelableExtra(ConnectivityManager.EXTRA_NETWORK_INFO)
 * ENHANCE: Terminate queues if network not available and all jobs require network.
 * ENHANCE: Add a 'stopQueue(name, force)' method which kills a queue by terminating its thread (force=true), or by asking it to stop, waiting 30 seconds and killing it.
 * 
 * @author Philip Warner
 *
 */
public abstract class QueueManager extends Service {
	/** Used to sent notifications regarding tasks */
	private NotificationManager m_notifier;
	/** Database access layer */
	private DbAdapter m_dba;
	/** Collection of currently active queues */
	private Hashtable<String,Queue> m_activeQueues = new Hashtable<String,Queue>();
	/** The UI thread */
	private WeakReference<Thread> m_uiThread = null;
	/** Handle inter-thread messages */
	MessageHandler m_messageHandler;

	/** Objects listening for Event operations */
	ArrayList<WeakReference<OnEventChangeListener>> m_eventChangeListeners = new ArrayList<WeakReference<OnEventChangeListener>>();
	/** Objects listening for Task operations */
	ArrayList<WeakReference<OnTaskChangeListener>> m_taskChangeListeners = new ArrayList<WeakReference<OnTaskChangeListener>>();
	
	/** Static reference to the active QueueManager */
	private static QueueManager m_queueManager;

	public QueueManager() {
		super();
		if (m_queueManager != null) {
			// This is an essential requirement because (a) synchronization will not work with more than one
			// and (b) we want to store a static reference in the class.
			throw new RuntimeException("Only one QueueManager can be present");
		}
		m_queueManager = this;
	}

	public static final QueueManager getQueueManager() {
		return m_queueManager;
	}

	@Override
    public void onCreate() {
		// Save the thread ... it is the UI thread
		m_uiThread = new WeakReference<Thread>(Thread.currentThread());
		m_messageHandler = new MessageHandler();

		// Create the notifier
		m_notifier = (NotificationManager)getSystemService(NOTIFICATION_SERVICE);
		// Connect to DB
		m_dba = new DbAdapter(this);

        // Get active queues.
    	synchronized(this) {
	        m_dba.getAllQueues(this);
    	}
    }

	public void registerEventListener(OnEventChangeListener listener) {
		synchronized(m_eventChangeListeners) {
			for (WeakReference<OnEventChangeListener> lr :  m_eventChangeListeners) {
				OnEventChangeListener l = lr.get();
				if (l != null && l.equals(listener))
					return;
			}
			m_eventChangeListeners.add(new WeakReference<OnEventChangeListener>(listener));			
		}
	}
	public void unregisterEventListener(OnEventChangeListener listener) {
		synchronized(m_eventChangeListeners) {
			ArrayList<WeakReference<OnEventChangeListener>> ll = new ArrayList<WeakReference<OnEventChangeListener>>();
			for (WeakReference<OnEventChangeListener> l :  m_eventChangeListeners) {
				if (l.get().equals(listener))
					ll.add(l);
			}
			for(WeakReference<OnEventChangeListener> l :  ll) {
				m_eventChangeListeners.remove(l);
			}
		}
	}

	public void registerTaskListener(OnTaskChangeListener listener) {
		synchronized(m_taskChangeListeners) {
			for (WeakReference<OnTaskChangeListener> lr :  m_taskChangeListeners) {
				OnTaskChangeListener l = lr.get();
				if (l != null && l.equals(listener))
					return;
			}
			m_taskChangeListeners.add(new WeakReference<OnTaskChangeListener>(listener));			
		}
	}

	public void unregisterTaskListener(OnTaskChangeListener listener) {
		synchronized(m_taskChangeListeners) {
			ArrayList<WeakReference<OnTaskChangeListener>> ll = new ArrayList<WeakReference<OnTaskChangeListener>>();
			for (WeakReference<OnTaskChangeListener> l :  m_taskChangeListeners) {
				if (l.get().equals(listener))
					ll.add(l);
			}
			for(WeakReference<OnTaskChangeListener> l :  ll) {
				m_taskChangeListeners.remove(l);
			}
		}
	}

	protected void notifyTaskChange(final Task task, final TaskActions action) {
		// Make a copy of the list so we can cull dead elements from the original
		ArrayList<WeakReference<OnTaskChangeListener>> list = new ArrayList<WeakReference<OnTaskChangeListener>>();
		synchronized(m_taskChangeListeners) {
			for(WeakReference<OnTaskChangeListener> wl : m_taskChangeListeners) {
				list.add(wl);
			}
		}
		// Scan through the list. If the ref is dead, delete from original, otherwise call it.
		for(WeakReference<OnTaskChangeListener> wl : list) {
			final OnTaskChangeListener l = wl.get();
			if (l == null) {
				synchronized(m_taskChangeListeners) {
					m_taskChangeListeners.remove(wl);
				}
			} else {
				try {
					Runnable r = new Runnable(){
						@Override
						public void run() {
							l.onTaskChange(task, action);
						}};
					m_messageHandler.post(r);
				} catch (Exception e) {
					// Throw away errors.
				}
			}
		}
	}

	protected void notifyEventChange(final Event event, final EventActions action) {
		// Make a copy of the list so we can cull dead elements from the original
		ArrayList<WeakReference<OnEventChangeListener>> list = new ArrayList<WeakReference<OnEventChangeListener>>();
		synchronized(m_eventChangeListeners) {
			for(WeakReference<OnEventChangeListener> wl : m_eventChangeListeners) {
				list.add(wl);
			}
		}
		// Scan through the list. If the ref is dead, delete from original, otherwise call it.
		for(WeakReference<OnEventChangeListener> wl : list) {
			final OnEventChangeListener l = wl.get();
			if (l == null) {
				synchronized(m_eventChangeListeners) {
					m_eventChangeListeners.remove(wl);
				}
			} else {
				try {
					Runnable r = new Runnable(){
						@Override
						public void run() {
							l.onEventChange(event, action);
						}};
					m_messageHandler.post(r);
				} catch (Exception e) {
					// Throw away errors.
				}
			}
		}
	}

	/**
	 * Class to enable client access to this object.
	 * 
	 * @author Philip Warner
	 */
	public class QueueManagerBinder extends Binder {
		public QueueManager getService() {
            return QueueManager.this;
        }
    }

	// Create the object that receives interactions from clients.
    private final IBinder m_binder = new QueueManagerBinder();

    @Override
	public IBinder onBind(Intent intent) {
		return m_binder;
	}

	@Override
	public void onStart(Intent intent, int flags) {
		// Nothing to do?
	}

	/**
     * Show a notification while this service is running.
	 * 
	 * @param title
	 * @param message
	 */
    public void showNotification(int id, String title, String message, Intent i) {
        // In this sample, we'll use the same text for the ticker and the expanded notification
        CharSequence text = message; //getText(R.string.local_service_started);

        // Set the icon, scrolling text and timestamp
        Notification notification = new Notification(android.R.drawable.ic_dialog_info, text, System.currentTimeMillis());

        // The PendingIntent to launch our activity if the user selects this notification
        PendingIntent contentIntent = PendingIntent.getActivity(this, 0, i, 0);

        // Set the info for the views that show in the notification panel.
        notification.setLatestEventInfo(this, title, //getText(R.string.local_service_label),
                       text, contentIntent);

        // Send the notification.
        m_notifier.notify(id, notification);
    }

    /**
     * Store a Task in the database to run on the specified Queue and start queue if necessary.
     * 
     * @param task			task to queue
     * @param queueName		Name of queue
     */
    public long enqueueTask(Task task, String queueName, long priority) {
    	long id;
    	synchronized(this) {
    		// Save it
	    	id = m_dba.enqueTask(task, queueName, priority);
	    	// Ensure Queue exists
	    	if (m_activeQueues.containsKey(queueName)) {
	    		// Get the Queue
	    		Queue queue = m_activeQueues.get(queueName);
	    		// Wake it up
	    		synchronized(queue) {
	    			queue.notify();	    			
	    		}
	    	} else {
	    		// Create the queue; it will start and add itself to the manager
	    		new Queue(this.getApplicationContext(), this, queueName);
	    	}
    	}
		this.notifyTaskChange(task, TaskActions.created);
    	return id;
    }

	/**
	 * Create the specified queue if it does not exist.
	 * 
	 * @param queueName	Name of the queue
	 * @return			ID of resulting queie
	 */
    public long initializeQueue(String name) {
    	return m_dba.createQueue(name);
    }

    /**
     * Called by a Queue object in its Constructor to inform the QueueManager of its existence
     * 
     * @param queue		New queue object
     */
    public void queueStarting(Queue queue) {
    	synchronized(this) {
        	m_activeQueues.put(queue.getQueueName(), queue);    		
    	}
    }

    /**
     * Called by the Queue object when it is terminating and no longer processing Tasks.
     * 
     * @param queue		Queue that is stopping
     */
    public void queueTerminating(Queue queue) {
    	synchronized(this) {
        	try {
        		// It's possible that a queue terminated and another started; make sure we are removing
        		// the one that called us.
        		Queue q = m_activeQueues.get(queue.getQueueName());
        		if (q.equals(queue))
	            	m_activeQueues.remove(queue.getQueueName());
        	} catch (Exception e) {
        		// Ignore failures.
        	}    		
    	}
    }

    /**
     * Called by a Queue object to run a task. This method is in the QueueManager so that
     * it can be easily overridden by a subclass.
     * 
     * @param task	Task to run
     * @return		Result from run(...) method
     */
    protected boolean runOneTask(Task task) {
		if (task instanceof RunnableTask) {
			return ((RunnableTask) task).run(this, this.getApplicationContext());
		} else {
			throw new RuntimeException("Can not handle tasks that are not RunnableTasks. Either extend RunnabkeTask, or override QueueManager.runOneTask()");
		}    	
    }

	/**
	 * Save the passed task back to the database. The parameter must be a Task that
	 * is already in the database. This method is used to preserve a task state.
	 * 
	 * @param task The task to be saved. Must exist in database.
	 */
    public void saveTask(Task task) {
    	m_dba.updateTask(task);
		this.notifyTaskChange(task, TaskActions.updated);
    }
//    private boolean isMyServiceRunning() {
//        ActivityManager manager = (ActivityManager) getSystemService(ACTIVITY_SERVICE);
//        for (RunningServiceInfo service : manager.getRunningServices(Integer.MAX_VALUE)) {
//            if ("com.example.MyService".equals(service.service.getClassName())) {
//                return true;
//            }
//        }
//        return false;
//    }

	/**
	 * Make a toast message for the caller. Queue in UI thread if necessary.
	 * 
	 * @param message	Message to send
	 */
	public void doToast(String message) {
		if (Thread.currentThread() == m_uiThread.get()) {
			synchronized(this) {
				android.widget.Toast.makeText(this.getApplicationContext(), message, android.widget.Toast.LENGTH_LONG).show();			
			}
		} else {
			/* Send message to the handler */
			Message msg = m_messageHandler.obtainMessage();
			Bundle b = new Bundle();
			b.putString("__internal", "toast");
			b.putString("message", message);
			msg.setData(b);
			m_messageHandler.sendMessage(msg);			
		}
	}

	/**
	 * Handler for internal UI thread messages.
	 * 
	 * @author Philip Warner
	 */
	private class MessageHandler extends Handler {
		public void handleMessage(Message msg) {
			Bundle b = msg.getData();
			if (b.containsKey("__internal")) {
				String kind = b.getString("__internal");
				if (kind.equals("toast")) {
					doToast(b.getString("message"));
				//} else if (kind.equals(....)) {
				//	   ...
				//} else {
				//	    ...
				}
			} else {
				throw new RuntimeException("Unknown message");
			}
		}		
	}

	/**
	 * Store an Event object for later retrieval after task has completed. This is 
	 * analogous to writing a line to the 'log file' for the task.
	 * 
	 * @param t		Related task
	 * @param e		Exception (usually subclassed)
	 */
	public long storeTaskEvent(Task t, Event e) {
		long id = m_dba.storeTaskEvent(t, e);
		this.notifyEventChange(e, EventActions.created);
		return id;
	}

	/**
	 * Return an EventsCursor for all events.
	 * 
	 * @param taskId	ID of the task
	 * 
	 * @return			Cursor of exceptions
	 */
	public EventsCursor getAllEvents() {
		return m_dba.getAllEvents();
	}

	/**
	 * Return an EventsCursor for the specified task ID.
	 * 
	 * @param taskId	ID of the task
	 * 
	 * @return			Cursor of exceptions
	 */
	public EventsCursor getTaskEvents(long taskId) {
		return m_dba.getTaskEvents(taskId);
	}

	/**
	 * Return as TasksCursor for the specified type.
	 * 
	 * @param type		Subtype of cursor to retrieve
	 * 
	 * @return			Cursor of exceptions
	 */
	public TasksCursor getTasks(TaskCursorSubtype type) {
		return m_dba.getTasks(type);
	}

	/**
	 * Delete the specified Task object and related Event objects
	 * 
	 * @param id	ID of TaskException to delete.
	 */
	public void deleteTask(long id) {
		// Check if the task is running in a queue.
		synchronized(this) {
			// Synchronize so that no queue will be able to get another task while we are deleting
			for( Queue q : m_activeQueues.values() ) {
				Task t = q.getTask();
				if (t.getId() == id) {
					t.abortTask();
					break;
				}
			}
			m_dba.deleteTask(id);
		}
		this.notifyEventChange(null, EventActions.deleted);
		// This is non-optimal, but ... it's easy and clear.
		// Deleting an event MAY result in an orphan task being deleted.
		this.notifyTaskChange(null, TaskActions.deleted);
	}

	/**
	 * Delete the specified Event object.
	 * 
	 * @param id	ID of TaskException to delete.
	 */
	public void deleteEvent(long id) {
		m_dba.deleteEvent(id);
		this.notifyEventChange(null, EventActions.deleted);
		// This is non-optimal, but ... it's easy and clear.
		// Deleting an event MAY result in an orphan task being deleted.
		this.notifyTaskChange(null, TaskActions.deleted);
	}

	/**
	 * Delete Event records more than a certain age.
	 * 
	 * @param ageInDays	Age in days for stale records
	 */
	public void cleanupOldEvents(int ageInDays) {
		m_dba.cleanupOldEvents(ageInDays);
		m_dba.cleanupOrphans();
		// This is non-optimal, but ... it's easy and clear.
		this.notifyEventChange(null, EventActions.deleted);
		this.notifyTaskChange(null, TaskActions.deleted);
	}

	/**
	 * Delete Task records more than a certain age.
	 * 
	 * @param ageInDays	Age in days for stale records
	 */
	public void cleanupOldTasks(int ageInDays) {
		m_dba.cleanupOldTasks(ageInDays);
		m_dba.cleanupOrphans();
		// This is non-optimal, but ... it's easy and clear.
		this.notifyEventChange(null, EventActions.deleted);
		this.notifyTaskChange(null, TaskActions.deleted);
	}

	/**
	 * Update the priority of the specified task so that it will be at the front of the queue.
	 * 
	 * @param taskId	ID of the Task to update
	 */
	public void bringTaskToFront(long taskId) {
		synchronized(this) {
			m_dba.bringTaskToFront(taskId);			
		}
	}
	/**
	 * Update the priority of the specified task so that it will be at the back of the queue.
	 * 
	 * @param taskId	ID of the Task to update
	 */
	public void sendTaskToBack(long taskId) {
		synchronized(this) {
			m_dba.sendTaskToBack(taskId);			
		}		
	}

	/**
	 * Get a new Event object capable of representing a non-deserializable Event object.
	 * 
	 * @param original	original serialization source
	 * @return
	 */
	public abstract LegacyEvent newLegacyEvent(byte[] original);

	/**
	 * Get a new Task object capable of representing a non-deserializable Task object.
	 * 
	 * @param original	original serialization source
	 * @return
	 */
	public abstract LegacyTask newLegacyTask(byte[] original);

}

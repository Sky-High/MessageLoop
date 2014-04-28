package com.hifly.messageloop;
import android.app.Activity;
import android.os.Bundle;
import android.os.Handler;
import android.os.Looper;
import android.os.Message;
import android.support.v4.app.Fragment;
import android.util.Log;

/**
 * The TaskFragment fragment manages a single background task and retains itself across
 * configuration changes.
 * <h2>Scenario</h2>
 * <ol><li>
 * The MainActivity can be reset after a reconfiguration. Therefore a separate fragment
 * (TaskFragment) is created as restrained instance that will remain unchanged after
 * a reconfiguration.
 * </li><li>
 * TaskFragment still runs in the main UI thread, so its actions may block the UI.
 * Therefore a new work thread (TaskThread) is created in TaskFragment that will run
 * independently of the UI.
 * </li><li>
 * Actions for the work thread will be started by sending a message to the TaskThread.
 * For this a message looper is created in TaskThread and a handler
 * TaskHandler is defined with methods for sending messages to this looper.
 * </li><li>
 * TaskHandler dispatches the message commands to DispatchWork for actual processing.
 * </li><li>
 * Correspondingly, a handler MainHandler is created in TaskFragment by which
 * DispatchWork can deliver messages with results and replies from the worker
 * thread to the main UI thread. In this way the UI can be updated as required by
 * the actions in the worker thread. 
 * </li></ol>
 * 
 * <h2>Operation</h2>
 * <ul><li>
 * The work thread message looper will not be reset during the application life cycle,
 * so has a unique instance. The concerning class instances of TaskThread,
 * TaskHandler and DispatchWork are therefore defined as static, although this is
 * not really required from a technical point of view.
 * </li><li>
 * References to Handler objects like mMainHandler are defined as static as well,
 * as these may refer (indirectly) to MainActivity, and are therefore susceptible
 * to leaks because of reconfigurations.
 * <br>
 * The same is true for objects like mCallbacks. Technically, this is no problem as
 * the concerning objects are intended to be and remain unique anyway.
 * </li><li>
 * </li></ul>
 */
public class TaskFragment extends Fragment {
	private static final String 	TF_TAG = TaskFragment.class.getSimpleName();
	private static final String 	MH_TAG = "MainHandler";
	private static final String 	TT_TAG = "TaskThread";
	private static final String 	TH_TAG = "TaskHandler";
	private static MainHandler  	mMainHandler;
	private static TaskHandler  	mTaskHandler;
	private static TaskCallbacks	mCallbacks;
	/**
	 * Callback interface through which the fragment can report the task's
	 * progress and results back to the Activity.
	 */
	static interface TaskCallbacks {
		public void 	updActivity(String t);
	}

	/*****************************
	 * Android passes us a reference to the newly created Activity by calling this
	 * method after each configuration change.
	 * <p>
	 * Call sequence first time:
	 * <br>- onAttach, onCreate, onActivityCreated, onStart (... onResume)
	 * <br>
	 * After configuration change:
	 * <br>- onAttach, onActivityCreated,onStart (... onResume)
	 */
	@Override
	public void onAttach(Activity activity) {
		super.onAttach(activity);
		if (!(activity instanceof TaskCallbacks)) {
			throw new IllegalStateException("Activity must implement the TaskCallbacks interface.");
		}
		// Hold a reference to the parent Activity so we can report back the task's
		// current progress and results.
		mCallbacks	= (TaskCallbacks) activity;
		Log.i(TF_TAG, "-- onAttach("+activity+") in "+this);
	}

	/*****************************
	 * This method is called only once when the Fragment is first created.
	 * Start the worker thread TaskThread, and create the MainHandler for the
	 * main UI thread.
	 */
	@Override
	public void onCreate(Bundle savedInstanceState) {
		super.onCreate(savedInstanceState);
		setRetainInstance(true);
		Log.i(TF_TAG, "-- onCreate TaskFragment in "+ this);

		// Create the main handler.
		mMainHandler	= new MainHandler();

		// Start the task thread. This thread runs in this separate fragment and will
		// exist over configuration changes. So no GC issues, and no weak referencing
		// is required as long as the UI system is not used.
		new TaskThread().start();
	}	// ----- end onCreate -----

	/*****************************
	 * This method is <em>not</em> called when the Fragment is being retained
	 * across Activity instances.
	 */
	@Override
	public void onDestroy() {
		Log.i(TF_TAG, "-- onDestroy in "+this);
		// looper: quit()
		// thread: interrupt(). Stops sleep() with InterruptedException
		// handler: --
		super.onDestroy();
		DispatchWork.quit();
		mTaskHandler.getLooper().quit();
	}

	/*****************************
	 * process command from MainActivity by sending a command code to the Task Thread
	 * @param what action command code to submit
	 */
	public void doCmd(int what) {
		mTaskHandler.sendEmptyMessage(what);
	}

	/**************************************
	 * <h1>the MainHandler class</h1>
	 * Create the main handler.
	 * <br>
	 * Even though TaskFragment is a separate fragment, it is on the main thread so bound to
	 * the MainActivity main thread's message queue. So it is sensitive to leaks after
	 * a configurations change, and it makes sense to define the handler as static.
	 * <br>
	 * As a consequence, the variables mCallbacks and mMainHandler must be static as well.
	 * Maybe they should even be WeakReferences. This poses no problems however.
	 */
	private static class MainHandler extends Handler {
		/* ********************** */
		// explicitly specify constructor, for testing and logging only
		MainHandler()	{
			super();
			Log.i(MH_TAG, "-- "+this+" bound to: "+this.getLooper().getThread().getName());
		}
		/* ********************** */

		@Override
		public void handleMessage(Message msg) {
			String	MsgTxt	= "<"+msg.what+">";
			Log.i(MH_TAG, "<<"  + msg);
			// Handle the message returned from the task thread.
			// This is performed in MainActivity in the UI thread.
			mCallbacks.updActivity(MsgTxt);
		}
	}	// ---- end MainHandler

	/**************************************
	 * <h1>the TaskThread class</h1>
	 * The TaskThread class implements a thread that runs independently and separately from
	 * the main UI thread.
	 * <br>
	 * Class variables may be declared static without problem as this class will be created
	 * only once and will not change after configuration changes.
	 */
	private static class TaskThread extends Thread {
		@Override
		public void run() {
			this.setName(TT_TAG);
			Looper.prepare();
			// Create the task handler on the task thread so it is bound to the task thread's message queue.
			mTaskHandler = new TaskHandler();
			Log.i(TT_TAG, "-- "+ mTaskHandler +" in "+this+
					" bound to " + mTaskHandler.getLooper().getThread().getName());
			// Start looping the message queue of this thread.
			Looper.loop();
			Log.i(TT_TAG, "-- task handler ends");
		}
	}	// ----- end TaskThread

	/**************************************
	 * <h1>the TaskHandler class</h1>
	 * TaskHandler is the message handler for TaskThread worker thread
	 * <p>
	 * TaskFragment is retained, so this thread is not supposed to be reconfigured or leak.
	 * But no problem making it static anyway.
	 * <br>
	 * mFragment and DispatchWork must be static as well, in that case.
	 * Actually, the DispatchWork class is even not instantiated at all, and is
	 * just called by its class name.
	 */
	protected static class TaskHandler extends Handler {
		TaskHandler() {
			super();	    			// required?
			new DispatchWork(this);		// call constructor for initialization
		}

		/**
		 * Perform the action command requested by the submitted message
		 * The action is executed in the DispatchWork class.
		 * @param msg The message containing the action command code
		 */
		@Override
		public void handleMessage(Message msg) {
			Log.i(TH_TAG, ">>"+msg);
			DispatchWork.doWork(msg.what);
		}

		/**
		 * send a return value from the task to the Main UI Thread
		 * @param n return value
		 */
		protected void sendResult(int n) {
			mMainHandler.sendEmptyMessage(n);
		}

	}	// ----- end TaskHandler


	/************************/
	/***** LOGS & STUFF *****/
	/************************/

	/* ********************** * /
	@Override
	public void onActivityCreated(Bundle savedInstanceState) {
		Log.i(TF_TAG, "onActivityCreated "+this);
		super.onActivityCreated(savedInstanceState);
	}

	@Override
	public void onStart() {
		Log.i(TF_TAG, "onStart "+this);
		super.onStart();
	}

	@Override
	public void onResume() {
		Log.i(TF_TAG, "onResume "+this);
		super.onResume();
	}

	@Override
	public void onPause() {
		Log.i(TF_TAG, "onPause "+this);
		super.onPause();
	}

	@Override
	public void onStop() {
		Log.i(TF_TAG, "onStop "+this);
		super.onStop();
	}
	/* ********************** */
}
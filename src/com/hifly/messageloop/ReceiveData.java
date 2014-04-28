package com.hifly.messageloop;
import android.os.Handler;
import android.os.Looper;
import android.os.Message;
import android.util.Log;

/**
 * <h1>The ReceiveData class</h1>
 * implements a thread that runs independently and separately from
 * the main UI thread and the worker thread.
 * <br>
 * Class variables may be declared static without problem as this class will be created
 * only once and will not change after configuration changes.
 */
public class ReceiveData extends Thread {
	private static final String 	RT_TAG = "ReceiveThread";
	private static final String 	RH_TAG = "ReceiveHandler";
	private static TaskFragment.TaskHandler 	hTask;	//handler of worker thread
	private static ReceiveHandler  	mReceiveHandler;
	private static SrvConnect   	SrvConnect;

	/*****************************
	 * ReceiveData constructor
	 * @param handle message handler of the Task Thread
	 * @param sc server connection object
	 */
	ReceiveData(TaskFragment.TaskHandler handle, SrvConnect sc) {
		this.setName(RT_TAG);
		hTask   	= handle;
		SrvConnect	= sc;
		Log.i(RT_TAG, "-- Rcv("+ hTask +":"+ SrvConnect +  ") constructed: "+this);
	}

	/*****************************
	 * start the message looper for the ReceiveThread thread
	 */
	@Override
	public void run() {
		Looper.prepare();
		mReceiveHandler = new ReceiveHandler();
		Log.i(RT_TAG, "-- "+ mReceiveHandler +" in "+this+" bound to " + mReceiveHandler.getLooper().getThread().getName());
		Looper.loop();
		Log.i(RT_TAG, "-- receive thread ends");
	}

	/*****************************
	 * quit ReceiveData
	 */
	public void quit() {
		mReceiveHandler.getLooper().quit();
	}

	/*****************************
	 * send command message to ReceiveData thread handler
	 * @param what command code
	 */
	public void sendReadCmd(int what) {
		mReceiveHandler.sendEmptyMessage(what);
	}

	/*****************************
	 * <h1>the ReceiveHandler class</h1>
	 * ReceiveHandler is called when a command message is received from the sender
	 * by means of a call to sendReadCmd().
	 * This handler will process the messages.
	 */
	private static class ReceiveHandler extends Handler {
		/* ********************** */
		// explicitly specify constructor, for testing and logging only
		ReceiveHandler() {
			super();
			Log.i(RH_TAG, "-- "+this+" bound to: "+this.getLooper().getThread().getName());
		}
		/* ********************** */

		/*****************************
		 * handle the data received after a call from sendReadCmd()
		 */
		@Override
		public void handleMessage(Message msg) {
			Log.i(RH_TAG, "... rcv readSrv("+msg+")");
			SrvConnect.readRecord();
			Log.i(RH_TAG, "... rcv readSrv{"+ DispatchWork.num +"}=<"+ DispatchWork.val+">");
			// send message to worker thread for DispatchWork
			//hTask.sendEmptyMessage(DispatchWork.DW_READ_SRV);

			// for testing, send message to main thread for displaying
			hTask.sendResult(DispatchWork.DW_READ_SRV);	// to updMain for testing
		}
	}	// ---- end ReceiveHandler
	
}

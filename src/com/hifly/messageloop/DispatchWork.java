package com.hifly.messageloop;
import android.util.Log;

/**
 * <h1>the DispatchWork class</h1>
 * Dispatches action commands submitted as messages. DispatchWork runs
 * in a worker thread ChildThread which is a fragment and thread separate from the main UI
 * thread of MainActivity as to not interfere or block UI actions.
 * <br>
 * The DispatchWork class consists of a set of unique methods (functions)
 * and does not need instantiation.
 * Therefore, the class, all methods and global (class) variables are defined static.
 * The public methods are called by their class name, not by an instance name.
 * <p>
 * <em>Note 1</em><br>
 * As DispatchWork is only called by its static class name, the class
 * constructor is never called, unless explicitly or by
 * calling "new DispatchWork()".
 */
public class DispatchWork {
	/**
	 * action command code enumeration
	 */
	public static final int	CM_START=0, CM_CANCEL=1,
			CM_WRITESRV=5, CM_READ_INIT=6, CM_READSRV=8;
	/**
	 * Return code enumeration of DispatchWork action commands.
	 */
	public static final int	DW_UNDEFINED=999, DW_FINAL=700,
			DW_OPEN_SOCKET=901, DW_CLOSE_SOCKET=902, DW_CLOSE_SOCKET_NULL=903,
			DW_WRITE_SRV=906, DW_WRITE_SRV_NULL=907,
			DW_READ_SRV=908, DW_READ_SRV_NULL=909;

	private static TaskFragment.TaskHandler 	hTask;	    	//message handler for work thread loop
	private static SrvConnect   	srvConnect;
	private static final String 	TAG = "DispatchWork";
	private static final int    	SERVERPORT = 5000;
	public static final String    	SERVERIP = "10.0.2.2";		//localhost outside emulator
	private static boolean	    	bConnected = false;
	public static String	    	server_IP;
	public static String	    	val;
	public static int   	    	num;

	/*****************************
	 * <h1>DispatchWork constructor</h1>
	 * For sockets, java.nio and bytebuffers, see also:
	 * <br>http://examples.javacodegeeks.com/android/core/socket-core/android-socket-example/<br>
	 * For android emulator ip address and port configuration, see:
	 * <br>http://developer.android.com/tools/devices/emulator.html
	 * <p>
	 * @param handler message handler for sending return value
	 */
	DispatchWork(TaskFragment.TaskHandler handler) {
		Log.i(TAG, "DispatchWork constructor: "+handler);
		hTask	    	= handler;
		server_IP   	= SERVERIP;
		val 	    	= "$$";
		num 	    	= 0;
	}

	/*****************************
	 * dispatch the action command
	 * @param iCmd enumeration code for action command
	 */
	public static void doWork(int iCmd) {

		// dispatch action command
		switch (iCmd) {
		case CM_START:
			openSocket();
			return;
		case CM_CANCEL:
			closeSocket();
			return;
		case CM_WRITESRV:
			writeSrv();
			return;
		default:
			// undefined command
		}

		hTask.sendResult(DW_UNDEFINED);
		Log.i(TAG, "... undefined command: "+iCmd);

	}	// -------- end doWork

	// ---------------------------
	//	supporting functions

	/*****************************
	 * open a socket connection with the server
	 */
	private static void openSocket() {
		Log.i(TAG, "-- openSocket{begin}");
		srvConnect	= new SrvConnect(hTask);
		srvConnect.Open(server_IP, SERVERPORT);

		bConnected	= true;
		val	    	= "Connected";
		hTask.sendResult(DW_OPEN_SOCKET);
		Log.i(TAG, "-- openSocket{end}");
	}

	/*****************************
	 * close socket connection with server
	 */
	private static void closeSocket() {
		bConnected	= false;
		srvConnect.Close();
		hTask.sendResult(DW_CLOSE_SOCKET);
	}

	/*****************************
	 * close resources when quitting
	 */
	public static void quit() {
		bConnected	= false;
		srvConnect.Close();
		Log.i(TAG, "... quit");
	}

	/*****************************
	 * test if socket connection with server is established
	 * @return <b>true</b> if so, <b>false</b> if not
	 */
	public static boolean isConnected() {
		return bConnected;
	}

	/*****************************
	 * write a record
	 */
	private static void writeSrv() {
		Log.i(TAG, "... writeSrv>>prepare read");

		// prepare to receive data
		srvConnect.sendReadCmd(CM_READ_INIT);

		Log.i(TAG, "... writeSrv>>write record");

		srvConnect.writeRecord();

		Log.i(TAG, "... writeSrv>>done:"+DW_WRITE_SRV);
		hTask.sendResult(DW_WRITE_SRV);
	}

}

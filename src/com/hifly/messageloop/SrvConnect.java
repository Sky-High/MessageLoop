package com.hifly.messageloop;
import java.io.IOException;
import java.net.ConnectException;
import java.net.InetSocketAddress;
import java.net.UnknownHostException;
import java.nio.ByteBuffer;
import java.nio.ByteOrder;
import java.nio.channels.AsynchronousCloseException;
import java.nio.channels.SocketChannel;

import android.util.Log;

/**
 * <h1>the SrvConnect class</h1>
 * Handle the socket channel connection to the server and its read and send buffers. 
 */
public class SrvConnect {
	private static final String 	SRV_TAG = "SrvConnect";
	private static TaskFragment.TaskHandler	 hTask; 	//handler of worker thread
	private static final int    	BUFLEN = 512;
	private static ReceiveData  	receiveData;
	private static SocketChannel	socketCh;
	private static ByteBuffer   	readBuf, sendBuf;

	/*****************************
	 * <h1>SrvConnect constructor</h1>
	 * Initialize parameters, create read and send ByteBuffers.
	 * @param handler message handler of the worker Task Thread
	 */
	SrvConnect(TaskFragment.TaskHandler handler) {
		socketCh	= null;
		receiveData	= null;
		hTask		= handler;		// for submitting messages
		Log.i(SRV_TAG, "-- SrvConnect("+ hTask + ") constructed: "+this);
		readBuf		= ByteBuffer.allocateDirect(BUFLEN);
		readBuf.order(ByteOrder.LITTLE_ENDIAN);
		sendBuf		= ByteBuffer.allocateDirect(BUFLEN);
		sendBuf.order(ByteOrder.LITTLE_ENDIAN);
	}

	/*****************************
	 * Open server connection as SocketChannel and start the ReceiveData thread.
	 * @param server_IP IP address of server
	 * @param serverPort port at server
	 * @return handle to SocketChannel
	 */
	SocketChannel Open(String server_IP, int serverPort) {
		Log.i(SRV_TAG, "... Open:"+server_IP+":"+serverPort);
		try {
			socketCh = SocketChannel.open(new InetSocketAddress(server_IP, serverPort));
		} catch (UnknownHostException e) {
			e.printStackTrace();
		} catch (ConnectException e) {
			e.printStackTrace();
		} catch (IOException e) {
			e.printStackTrace();
		}
		Log.i(SRV_TAG, "... Open>>"+socketCh);
		
		// start receive thread to loop for data reads
		if( null == receiveData) {
			receiveData	= new ReceiveData(hTask, this);
			receiveData.start();
		}
		
		return socketCh;
	}

	/*****************************
	 * Close socket connection and its ReceiveData thread.
	 */
	void Close() {
		if(null == socketCh) {
			Log.i(SRV_TAG, "... Close:null");
			return;
		}
		Log.i(SRV_TAG, "... Close>>"+socketCh.toString());
		
		// abort possible pending read operation
		receiveData.quit();

		try {
			socketCh.close();
		} catch (UnknownHostException e1) {
			e1.printStackTrace();
		} catch (ConnectException e1) {
			e1.printStackTrace();
		} catch (IOException e1) {
			e1.printStackTrace();
		} catch (Exception e1) {
			e1.printStackTrace();
		}
		Log.i(SRV_TAG, "... Close: "+ ((null == socketCh )?"$$": socketCh.toString()) );
		return;
	}

	/*****************************
	 * send command message to ReceiveData thread handler
	 * @param what command code
	 */
	void sendReadCmd(int what) {
		Log.i(SRV_TAG, "... sendReadCmd:"+what);
		receiveData.sendReadCmd(what);
	}

	/*****************************
	 *	read data from the socket into the read buffer readBuf
	 */
	void readRecord(){
		readBuf.clear();
		try {
			socketCh.read(readBuf);
		} catch (AsynchronousCloseException e) {
			Log.i(SRV_TAG, "... canceled read");
		} catch (IOException e) {
			e.printStackTrace();
		}
		int nRead	        = readBuf.position();	// number of bytes read
		String txt        	= new String(readBuf.array(), 0, nRead);
		DispatchWork.num	= nRead;
		DispatchWork.val	= txt;
		Log.i(SRV_TAG, "... ["+nRead+"]="+txt);
	}

	/*****************************
	 * write data to socket channel
	 */
	void writeRecord() {
		Log.i(SRV_TAG, "... writeRecord{"+DispatchWork.num+"}"+DispatchWork.val);
		sendBuf.clear();
		sendBuf.put(DispatchWork.val.getBytes());
		sendBuf.flip();
		try {
			socketCh.write(sendBuf);		
		} catch (Exception e) {
			e.printStackTrace();
		}
	}

}

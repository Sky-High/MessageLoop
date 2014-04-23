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

public class SrvConnect {
	private static final String 	SRV_TAG = "SrvConnect";
	private static TaskFragment.TaskHandler	 handler;	//handler of worker thread
	private static final int    	BUFLEN = 512;
	private static ReceiveData  	receiveData;
	private static SocketChannel	socketCh;
	private static ByteBuffer   	readBuf, sendBuf;

	/*****************************
	 * SrvConnect constructor: initialize parameters
	 * @param hHandler
	 * @param socket
	 */
	SrvConnect(TaskFragment.TaskHandler hHandler) {
		socketCh	= null;
		receiveData	= null;
		handler		= hHandler;		// for submitting messages
		Log.i(SRV_TAG, "-- SrvConnect("+ handler + ") constructed: "+this);
		readBuf		= ByteBuffer.allocateDirect(BUFLEN);
		readBuf.order(ByteOrder.LITTLE_ENDIAN);
		sendBuf		= ByteBuffer.allocateDirect(BUFLEN);
		sendBuf.order(ByteOrder.LITTLE_ENDIAN);
	}

	/*****************************
	 * Open server connection as SocketChannel
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
			receiveData	= new ReceiveData(handler, this);
			receiveData.start();
		}
		
		return socketCh;
	}

	/*****************************
	 * Close socket connection
	 * @return handle to closed SocketChannel
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
	 *	read data from the socket into the read buffer bBuf
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
		int cbRead	        = readBuf.position();	// nr of bytes read
		DispatchWork.num	= cbRead;
		byte[] bBytes   	= new byte[cbRead];
		readBuf.rewind();
		readBuf.get(bBytes);

		String t	= "rcv["+cbRead+"]";
		DispatchWork.val	= t+":"+new String(bBytes);
		Log.i(SRV_TAG, "... "+t);
	}

	/*****************************
	 * write data to socket
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

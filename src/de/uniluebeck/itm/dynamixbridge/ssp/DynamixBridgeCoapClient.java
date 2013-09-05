package de.uniluebeck.itm.dynamixbridge.ssp;

import java.net.URI;

import de.uniluebeck.itm.ncoap.application.client.CoapClientApplication;
import de.uniluebeck.itm.ncoap.application.client.CoapResponseProcessor;
import de.uniluebeck.itm.ncoap.communication.reliability.outgoing.EmptyAcknowledgementProcessor;
import de.uniluebeck.itm.ncoap.communication.reliability.outgoing.InternalEmptyAcknowledgementReceivedMessage;
import de.uniluebeck.itm.ncoap.communication.reliability.outgoing.InternalRetransmissionTimeoutMessage;
import de.uniluebeck.itm.ncoap.communication.reliability.outgoing.RetransmissionTimeoutProcessor;
import de.uniluebeck.itm.ncoap.message.CoapRequest;
import de.uniluebeck.itm.ncoap.message.CoapResponse;
import de.uniluebeck.itm.ncoap.message.header.Code;
import de.uniluebeck.itm.ncoap.message.header.MsgType;
import android.app.Service;
import android.content.Intent;
import android.os.IBinder;
import android.util.Log;

public class DynamixBridgeCoapClient extends Service 
{

	private final String TAG ="SSPBridge";
	private String sspIP="";
	@Override
	public IBinder onBind(Intent arg0) 
	{
		// TODO Auto-generated method stub
		return null;
	}

	@Override
	public int onStartCommand(Intent intent, int flags, int startId) 
	{
		super.onStartCommand(intent, flags, startId);
		Log.e(TAG, "START CoaP Client SERVICE");
		if(!sspIP.equals(""))
		{
			try
			{
				CoapClientApplication client = new CoapClientApplication();
				URI targetURI = new URI ("coap://"+sspIP+"/here_i_am");
				CoapRequest coapRequest =  new CoapRequest(MsgType.CON, Code.GET, targetURI);
				client.writeCoapRequest(coapRequest, new SimpleResponseProcessor());
			}
			catch(Exception e)
			{
				Log.e(TAG, "finding the SSP threw an exception");
			}
		}
		else
		{
			Log.d(TAG, "no IP of any SSP is known...");
		}
		return START_NOT_STICKY;
	}
	
	@Override
	public void onCreate() 
	{
		super.onCreate();
		Log.e(TAG, "onCreate()");
	}
	
	@Override
	public void onDestroy()
	{
		super.onDestroy();
		Log.d(TAG, "Stop... Destroy!");
	}
	
	public class SimpleResponseProcessor implements CoapResponseProcessor, EmptyAcknowledgementProcessor, RetransmissionTimeoutProcessor 
	{

		
		@Override
		public void processCoapResponse(CoapResponse coapResponse) 
		{
		    Log.d(TAG, "Received Response: " + coapResponse);
		}
		
		@Override
		public void processEmptyAcknowledgement(InternalEmptyAcknowledgementReceivedMessage message) 
		{
			Log.d(TAG, "Received empty ACK: " + message);
		}
		
		@Override
		public void processRetransmissionTimeout(InternalRetransmissionTimeoutMessage timeoutMessage) 
		{
			Log.d(TAG, "Transmission timed out: " + timeoutMessage);
		}
	}
}

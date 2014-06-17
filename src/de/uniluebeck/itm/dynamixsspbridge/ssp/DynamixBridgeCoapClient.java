/*
 * Copyright (C) Institute of Telematics, Lukas Ruge
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *      http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */
package de.uniluebeck.itm.dynamixsspbridge.ssp;

import java.net.InetSocketAddress;
import java.net.URI;

import de.uniluebeck.itm.ncoap.application.client.CoapClientApplication;
import de.uniluebeck.itm.ncoap.application.client.CoapResponseProcessor;
import de.uniluebeck.itm.ncoap.communication.reliability.outgoing.EmptyAcknowledgementProcessor;
import de.uniluebeck.itm.ncoap.communication.reliability.outgoing.InternalEmptyAcknowledgementReceivedMessage;
import de.uniluebeck.itm.ncoap.communication.reliability.outgoing.InternalRetransmissionTimeoutMessage;
import de.uniluebeck.itm.ncoap.communication.reliability.outgoing.RetransmissionTimeoutProcessor;
import de.uniluebeck.itm.ncoap.message.CoapRequest;
import de.uniluebeck.itm.ncoap.message.CoapResponse;
import de.uniluebeck.itm.ncoap.message.MessageCode;
import de.uniluebeck.itm.ncoap.message.MessageType;
import android.app.Service;
import android.content.Intent;
import android.os.IBinder;
import android.util.Log;

public class DynamixBridgeCoapClient extends Service 
{

	private final String TAG ="SSPBridge";
	private String sspIP="";
    private int sspPort=0;
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
		Log.e(TAG, "START CoaP Client SERVICE ");
		if(!sspIP.equals(""))
		{
			try
			{
				CoapClientApplication client = new CoapClientApplication();
				URI targetURI = new URI ("coap://"+sspIP+"/here_i_am");
                InetSocketAddress isa = new InetSocketAddress("coap://"+sspIP+"/here_i_am", sspPort);
				CoapRequest coapRequest =  new CoapRequest(MessageType.Name.CON, MessageCode.Name.GET, targetURI);
				byte[] bytes = "lol".getBytes();
				coapRequest.setContent(bytes);
                client.sendCoapRequest(coapRequest, new SimpleResponseProcessor(), isa);
                //OLD API
				//client.writeCoapRequest(coapRequest, new SimpleResponseProcessor());
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
        public void processEmptyAcknowledgement()
        {
            Log.d(TAG, "Received empty ACK: ");
        }

        @Override
        public void processRetransmissionTimeout()
        {
            Log.d(TAG, "Transmission timed out: ");
        }
    }
}

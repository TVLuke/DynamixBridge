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

package de.uniluebeck.itm.coapserver;

import java.net.InetSocketAddress;
import java.nio.charset.Charset;
import java.util.Date;
import java.util.List;
import java.util.Set;

import org.ambientdynamix.api.application.ContextEvent;

import android.util.Log;

import com.google.common.util.concurrent.SettableFuture;

import de.uniluebeck.itm.dynamixsspbridge.dynamix.ContextType;
import de.uniluebeck.itm.ncoap.application.server.webservice.NotObservableWebservice;
import de.uniluebeck.itm.ncoap.message.CoapRequest;
import de.uniluebeck.itm.ncoap.message.CoapResponse;
import de.uniluebeck.itm.ncoap.message.InvalidHeaderException;
import de.uniluebeck.itm.ncoap.message.InvalidMessageException;
import de.uniluebeck.itm.ncoap.message.MessageCode;
import de.uniluebeck.itm.ncoap.message.options.ContentFormat;

public class NotObservableDynamixWebservice  extends NotObservableWebservice<ContextEvent> 
{
	private static String TAG ="SSPBridge";
	private ContextType contexttype;
	
	public NotObservableDynamixWebservice(ContextType contexttype)
	{
        super("/"+contexttype.getName().replace(".", "/"), contexttype.getCurrentEvent(), 99999999999999999l);
        this.contexttype=contexttype;
    	Log.d(TAG, "starting test Server");
    	//Log.d(TAG, "Path: "+ this.getPath());
    	Log.d(TAG, "Name: "+ this.contexttype.getName());
    	contexttype.registerForUpdates(this);
    }


	public void shutdown() 
	{
		contexttype.deactivate();
	}


	@Override
	public void processCoapRequest(SettableFuture responseFuture, CoapRequest request, InetSocketAddress remoteAddress) 
	{
		 Log.d(TAG, "Service " + getPath() + " received request: " + request);
	        try
	        {
	            if(request.getMessageCode() == MessageCode.Name.GET.getNumber())
	            {
	            	Log.d(TAG, "its a get");
	                processGet(responseFuture, request);
	            }
	            else if(request.getMessageCode() == MessageCode.Name.POST.getNumber())
	            {
	            	Log.d(TAG, "its a post");
	                processPost(responseFuture, request);
	            }
	            else
	            {
	                responseFuture.set(new CoapResponse(request.getMessageTypeName(), MessageCode.Name.METHOD_NOT_ALLOWED_405));
	            }
	        }
	        catch(Exception e)
	        {
	            try {
					responseFuture.set(new CoapResponse(request.getMessageTypeName(), MessageCode.Name.INTERNAL_SERVER_ERROR_500));
				} catch (Exception e1) {
					// TODO Auto-generated catch block
					e1.printStackTrace();
				}
	        }
		
	}

	private void processPost(SettableFuture<CoapResponse> responseFuture, CoapRequest request) throws InvalidHeaderException, InvalidMessageException 
	{
        CoapResponse response;
        try{
            //parse new status value
            String payload = request.getContent().toString(Charset.forName("UTF-8"));
            //String newValue = Long.parseLong(payload);

            //set new status
            //this.setResourceStatus(newValue);

            //create response
            response = new CoapResponse(request.getMessageTypeName(), MessageCode.Name.CHANGED_204);
            response.setContent(createPayloadFromAcutualStatus(ContentFormat.TEXT_PLAIN_UTF8), ContentFormat.TEXT_PLAIN_UTF8);

            responseFuture.set(response);

        }
        catch(Exception e)
        {
            response = new CoapResponse(request.getMessageTypeName(), MessageCode.Name.BAD_REQUEST_400);
            response.setContent(e.getMessage().getBytes(Charset.forName("UTF-8")));
            responseFuture.set(response);	
        }
	}
	
	private void processGet(SettableFuture<CoapResponse> responseFuture, CoapRequest request) throws InvalidHeaderException 
	{
		Set<Long> acceptOptions = request.getAcceptedContentFormats();

		try
		{
	        //If accept option is not set in the request, use the default (TEXT_PLAIN)
	        if(acceptOptions.isEmpty())
	        {
	        	Log.d(TAG, "isempty");
	            CoapResponse response = new CoapResponse(request.getMessageTypeName(), MessageCode.Name.CONTENT_205);
	            Log.d(TAG, "line2");
	            response.setContent(createPayloadFromAcutualStatus(ContentFormat.TEXT_PLAIN_UTF8), ContentFormat.TEXT_PLAIN_UTF8);
	            Log.d(TAG, "line3");
	            Log.d(TAG, "line4");
	            responseFuture.set(response);
	        }
	
	        for(long option : acceptOptions)
	        {
	        	Log.d(TAG, "Option.Accept");
	            long acceptedMediaType = option;
	            Log.d(TAG, "Try to create payload for accepted mediatype " + acceptedMediaType);
	            byte[] payload = createPayloadFromAcutualStatus(acceptedMediaType);
	
	            //the requested mediatype is supported
	            if(payload != null)
	            {
	                CoapResponse response = new CoapResponse(request.getMessageTypeName(), MessageCode.Name.CONTENT_205);
	                response.setContent(payload, acceptedMediaType);
	                responseFuture.set(response);
	            }
	        }
		}
		catch(Exception e)
		{
			Log.d(TAG, "Exception at process get");
		}

        //This is only reached if all accepted mediatypes are not supported!
        CoapResponse response = new CoapResponse(request.getMessageTypeName(), MessageCode.Name.UNSUPPORTED_CONTENT_FORMAT_415);
        responseFuture.set(response);
		
	}
	

    private byte[] createPayloadFromAcutualStatus(long mediaType)
    {
    	Log.d(TAG, "payload from actual status");
    	ContextEvent event = getResourceStatus();
    	Date d = new Date();
    	if(event!=null)
    	{
    		if(event.getExpireTime().before(d))
    		{
		    	Log.d(TAG, "mediatype="+mediaType);
				if(mediaType==ContentFormat.APP_XML)
				{
					Log.d(TAG, "formatxml");
					StringBuffer payload = new StringBuffer();
					payload.append(event.getStringRepresentation("XML"));
					return payload.toString().getBytes(Charset.forName("UTF-8"));
				}
				if(mediaType == ContentFormat.TEXT_PLAIN_UTF8)
				{
					Log.d(TAG, "format is Plain Stuff");
					String payload =event.getStringRepresentation("text/plain"); 
					Log.d(TAG, payload);
					return payload.getBytes(Charset.forName("UTF-8"));
				}
    		}
    		else
    		{
				Log.d(TAG, "event has expired");
				contexttype.requestContextUpdate();
				String payload =event.getStringRepresentation("text/plain"); 
				payload="event has expired, but just in case you care... "+payload;
				return payload.getBytes(Charset.forName("UTF-8"));
    		}
    	}
		else
		{
			Log.d(TAG, "random old stuff");
			contexttype.requestContextUpdate();
			String payload =""; 
			return payload.getBytes(Charset.forName("UTF-8"));
		}
		return null;
    }


    @Override
    public byte[] getEtag(long contentFormat)
    {
        //TODO: I do not know what this method even does...
        return new byte[0];
    }

    @Override
	public void updateEtag(ContextEvent resourceStatus) {
		// TODO Auto-generated method stub
		
	}


	@Override
	public byte[] getSerializedResourceStatus(long contentFormatNumber){
		// TODO Auto-generated method stub
		return null;
	}
}

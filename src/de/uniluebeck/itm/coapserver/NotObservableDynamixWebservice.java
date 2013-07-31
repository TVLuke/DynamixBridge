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

import static de.uniluebeck.itm.ncoap.message.options.OptionRegistry.MediaType.APP_XML;
import static de.uniluebeck.itm.ncoap.message.options.OptionRegistry.MediaType.TEXT_PLAIN_UTF8;

import java.net.InetSocketAddress;
import java.nio.charset.Charset;
import java.util.Date;
import java.util.List;

import org.ambientdynamix.api.application.ContextEvent;

import android.util.Log;

import com.google.common.util.concurrent.SettableFuture;

import de.uniluebeck.itm.dynamixsspbridge.dynamix.ContextType;
import de.uniluebeck.itm.ncoap.application.server.webservice.NotObservableWebService;
import de.uniluebeck.itm.ncoap.message.CoapRequest;
import de.uniluebeck.itm.ncoap.message.CoapResponse;
import de.uniluebeck.itm.ncoap.message.MessageDoesNotAllowPayloadException;
import de.uniluebeck.itm.ncoap.message.header.Code;
import de.uniluebeck.itm.ncoap.message.options.Option;
import de.uniluebeck.itm.ncoap.message.options.UintOption;
import de.uniluebeck.itm.ncoap.message.options.OptionRegistry.MediaType;
import de.uniluebeck.itm.ncoap.message.options.OptionRegistry.OptionName;

public class NotObservableDynamixWebservice  extends NotObservableWebService<ContextEvent> 
{
	private static String TAG ="SSPBridge";
	private ContextType contexttype;
	
	public NotObservableDynamixWebservice(ContextType contexttype)
	{
        super("/"+contexttype.getName().replace(".", "/"), contexttype.getCurrentEvent());
        this.contexttype=contexttype;
    	Log.d(TAG, "starting test Server");
    	Log.d(TAG, "Path: "+ this.getPath());
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
	            if(request.getCode() == Code.GET)
	            {
	            	Log.d(TAG, "its a get");
	                processGet(responseFuture, request);
	            }
	            else if(request.getCode() == Code.POST)
	            {
	            	Log.d(TAG, "its a post");
	                processPost(responseFuture, request);
	            }
	            else
	            {
	                responseFuture.set(new CoapResponse(Code.METHOD_NOT_ALLOWED_405));
	            }
	        }
	        catch(Exception e)
	        {
	            responseFuture.set(new CoapResponse(Code.INTERNAL_SERVER_ERROR_500));
	        }
		
	}

	private void processPost(SettableFuture<CoapResponse> responseFuture, CoapRequest request) 
	{
        CoapResponse response;
        try{
            //parse new status value
            String payload = request.getPayload().toString(Charset.forName("UTF-8"));
            //String newValue = Long.parseLong(payload);

            //set new status
            //this.setResourceStatus(newValue);

            //create response
            response = new CoapResponse(Code.CHANGED_204);
            response.setPayload(createPayloadFromAcutualStatus(TEXT_PLAIN_UTF8));
            response.setContentType(MediaType.TEXT_PLAIN_UTF8);

            responseFuture.set(response);

        }
        catch(Exception e){
            response = new CoapResponse(Code.BAD_REQUEST_400);
            try {
                response.setPayload(e.getMessage().getBytes(Charset.forName("UTF-8")));
            } catch (MessageDoesNotAllowPayloadException e1) {
                //This should never happen!
            }
            responseFuture.set(response);
        }
		
	}
	
	private void processGet(SettableFuture<CoapResponse> responseFuture, CoapRequest request) 
	{
		List<Option> acceptOptions = request.getOption(OptionName.ACCEPT);

		try
		{
	        //If accept option is not set in the request, use the default (TEXT_PLAIN)
	        if(acceptOptions.isEmpty())
	        {
	        	Log.d(TAG, "isempty");
	            CoapResponse response = new CoapResponse(Code.CONTENT_205);
	            Log.d(TAG, "line2");
	            response.setPayload(createPayloadFromAcutualStatus(TEXT_PLAIN_UTF8));
	            Log.d(TAG, "line3");
	            response.setContentType(TEXT_PLAIN_UTF8);
	            Log.d(TAG, "line4");
	            responseFuture.set(response);
	        }
	
	        for(Option option : request.getOption(OptionName.ACCEPT))
	        {
	        	Log.d(TAG, "Option.Accept");
	            MediaType acceptedMediaType = MediaType.getByNumber(((UintOption) option).getDecodedValue());
	            Log.d(TAG, "Try to create payload for accepted mediatype " + acceptedMediaType);
	            byte[] payload = createPayloadFromAcutualStatus(acceptedMediaType);
	
	            //the requested mediatype is supported
	            if(payload != null)
	            {
	                CoapResponse response = new CoapResponse(Code.CONTENT_205);
	                response.setPayload(payload);
	                response.setContentType(acceptedMediaType);
	                responseFuture.set(response);
	            }
	        }
		}
		catch(Exception e)
		{
			Log.d(TAG, "Exception at process get");
		}

        //This is only reached if all accepted mediatypes are not supported!
        CoapResponse response = new CoapResponse(Code.UNSUPPORTED_MEDIA_TYPE_415);
        responseFuture.set(response);
		
	}
	

    private byte[] createPayloadFromAcutualStatus(MediaType mediaType)
    {
    	Log.d(TAG, "payload from actual status");
    	ContextEvent event = getResourceStatus();
    	Date d = new Date();
    	if(event!=null)
    	{
    		if(event.getExpireTime().before(d))
    		{
		    	Log.d(TAG, "mediatype="+mediaType);
				if(mediaType==APP_XML)
				{
					Log.d(TAG, "formatxml");
					StringBuffer payload = new StringBuffer();
					payload.append(event.getStringRepresentation("XML"));
					return payload.toString().getBytes(Charset.forName("UTF-8"));
				}
				if(mediaType == TEXT_PLAIN_UTF8)
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
}

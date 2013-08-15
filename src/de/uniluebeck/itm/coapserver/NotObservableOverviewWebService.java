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

import de.uniluebeck.itm.dynamixsspbridge.core.UpdateManager;
import de.uniluebeck.itm.dynamixsspbridge.dynamix.ContextType;
import de.uniluebeck.itm.dynamixsspbridge.dynamix.DynamixConnectionService;
import de.uniluebeck.itm.dynamixsspbridge.support.NotificationService;
import de.uniluebeck.itm.ncoap.application.server.webservice.NotObservableWebService;
import de.uniluebeck.itm.ncoap.message.CoapRequest;
import de.uniluebeck.itm.ncoap.message.CoapResponse;
import de.uniluebeck.itm.ncoap.message.MessageDoesNotAllowPayloadException;
import de.uniluebeck.itm.ncoap.message.header.Code;
import de.uniluebeck.itm.ncoap.message.options.Option;
import de.uniluebeck.itm.ncoap.message.options.UintOption;

import static de.uniluebeck.itm.ncoap.message.options.OptionRegistry.MediaType;
import static de.uniluebeck.itm.ncoap.message.options.OptionRegistry.MediaType.APP_XML;
import static de.uniluebeck.itm.ncoap.message.options.OptionRegistry.MediaType.TEXT_PLAIN_UTF8;
import static de.uniluebeck.itm.ncoap.message.options.OptionRegistry.OptionName;

import java.net.InetSocketAddress;
import java.nio.charset.Charset;
import java.util.HashMap;
import java.util.List;
import java.util.concurrent.ConcurrentHashMap;

import android.app.PendingIntent;
import android.util.Log;

import com.google.common.util.concurrent.SettableFuture;

public class NotObservableOverviewWebService extends NotObservableWebService<String> 
{
	private static String TAG ="SSPBridge";
	
    public NotObservableOverviewWebService(String servicePath, String initialStatus) 
    {
    	super(servicePath, initialStatus);
    	Log.d(TAG, "starting test Server");
    	Log.d(TAG, "Path:"+ this.getPath());
    }

    //TODO: this should be PUT!
	private void processPut(SettableFuture<CoapResponse> responseFuture, CoapRequest request) 
	{
        CoapResponse response;
        try{
            //parse new status value
            String payload = request.getPayload().toString(Charset.forName("UTF-8"));
            Log.d(TAG, "POST: "+payload);

            final ConcurrentHashMap<String, ContextType> contexttypes = UpdateManager.getContextTypes();
            ContextType type = (ContextType)contexttypes.get(payload);
            if(type!=null)
            {
            	Log.d(TAG, "type!=null");
            	NotificationService.requestContext(payload);
	            response = new CoapResponse(Code.CHANGED_204);
	            response.setPayload(createPayloadFromAcutualStatus(TEXT_PLAIN_UTF8));
	            response.setContentType(MediaType.TEXT_PLAIN_UTF8);

	            responseFuture.set(response);
            }
            //create response


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
	        	Log.d(TAG, "accept optioon is empty");
	            CoapResponse response = new CoapResponse(Code.CONTENT_205);
	            response.setPayload(createPayloadFromAcutualStatus(TEXT_PLAIN_UTF8));
	            response.setContentType(TEXT_PLAIN_UTF8);
	            responseFuture.set(response);
	        }
	
	        for(Option option : request.getOption(OptionName.ACCEPT)){
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
    	Log.d(TAG, ""+mediaType);
    	if(mediaType == APP_XML)
        {
            StringBuffer payload = new StringBuffer();
            payload.append("<contexttypes>\n");
        	ConcurrentHashMap<String, ContextType> types = UpdateManager.getContextTypes();
        	for(int i=0; i<types.size(); i++)
        	{
        		String key = (String) types.keySet().toArray()[i];
        		ContextType type = types.get(key);
        		//payload=payload+type.getName();
        		 payload.append("  <contexttype>\n");
        		 payload.append("    <name>"+type.getName()+"</name>\n");
        		 payload.append("    <readablename>"+type.getUserFriendlyName()+"</readablename>\n");
        		 payload.append("    <description>"+type.getDescription()+"</description>\n");
        		 payload.append("    <active>"+type.active()+"</active>\n");
        		 if(type.active())
        		 {
        			 payload.append("     <url>http://"+UpdateManager.getIP()+"/"+type.getName().replace(".", "/")+"/</url>\n");
        		 }
        		 payload.append("  </contexttype>\n");
        	}
            payload.append("</contexttypes>");

            return payload.toString().getBytes(Charset.forName("UTF-8"));
        }
        else if(mediaType == TEXT_PLAIN_UTF8)
        {
        	ConcurrentHashMap<String, ContextType> types = UpdateManager.getContextTypes();
        	String payload="";
        	for(int i=0; i<types.size(); i++)
        	{
        		String key = (String) types.keySet().toArray()[i];
        		ContextType type = types.get(key);
        		payload=payload+"\n -"+type.getName();
        	}
            return payload.getBytes(Charset.forName("UTF-8"));
        }
        else
        {
            return null;
        }
    }

	@Override
	public void processCoapRequest(SettableFuture responseFuture, CoapRequest request, InetSocketAddress remoteAddress) 
	{
		 Log.d(TAG, "Service " + getPath() + " received request: " + request);
	        try
	        {
	            if(request.getCode() == Code.GET)
	            {
	                processGet(responseFuture, request);
	            }
	            else if(request.getCode() == Code.PUT)
	            {
	                processPut(responseFuture, request);
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

	@Override
	public void shutdown() 
	{
		// TODO Auto-generated method stub
		
	}
}

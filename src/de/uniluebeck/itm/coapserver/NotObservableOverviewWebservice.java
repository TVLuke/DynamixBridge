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

import de.uniluebeck.itm.dynamixsspbridge.core.ManagerManager;
import de.uniluebeck.itm.dynamixsspbridge.core.UpdateManager;
import de.uniluebeck.itm.dynamixsspbridge.dynamix.ContextType;
import de.uniluebeck.itm.dynamixsspbridge.dynamix.DynamixConnectionService;
import de.uniluebeck.itm.dynamixsspbridge.support.NotificationService;
import de.uniluebeck.itm.ncoap.application.server.webservice.NotObservableWebservice;
import de.uniluebeck.itm.ncoap.message.CoapRequest;
import de.uniluebeck.itm.ncoap.message.CoapResponse;
import de.uniluebeck.itm.ncoap.message.InvalidHeaderException;
import de.uniluebeck.itm.ncoap.message.InvalidMessageException;
import de.uniluebeck.itm.ncoap.message.MessageCode;
import de.uniluebeck.itm.ncoap.message.options.ContentFormat;
import de.uniluebeck.itm.ncoap.message.options.InvalidOptionException;

import java.net.InetSocketAddress;
import java.nio.charset.Charset;
import java.util.HashMap;
import java.util.List;
import java.util.Set;
import java.util.concurrent.ConcurrentHashMap;

import android.app.PendingIntent;
import android.os.Bundle;
import android.util.Log;

import com.google.common.util.concurrent.SettableFuture;

public class NotObservableOverviewWebservice extends NotObservableWebservice<String> 
{
	private static String TAG ="SSPBridge";
	
    public NotObservableOverviewWebservice(String servicePath, String initialStatus) 
    {
    	super(servicePath, initialStatus, 999999999999999l);
    	Log.d(TAG, "starting test Server");
    	Log.d(TAG, "Path:"+ this.getPath());
    }

	private void processPut(SettableFuture<CoapResponse> responseFuture, CoapRequest request) throws InvalidOptionException, InvalidMessageException, InvalidHeaderException 
	{
        CoapResponse response;
        try
        {
            //parse new status value
            String payload = request.getContent().toString(Charset.forName("UTF-8"));
            Log.d(TAG, "PUT: "+payload);
            long requestMediaType = request.getContentFormat();
            Bundle scanConfig = ManagerManager.parseRequest(MessageCode.Name.PUT, payload, requestMediaType);
            if(scanConfig.containsKey("action_type"))
    		{
            	Log.d(TAG, "PUT contains action_type");
            	Log.d(TAG, scanConfig.getString("action_type"));
    			if(scanConfig.getString("action_type").equals("subscribe") && scanConfig.containsKey("context_type"))
    			{
    				Log.d(TAG, "PUT subscribe and contains context_type");
    				String ctype = scanConfig.getString("context_type");
    				Log.d(TAG, scanConfig.getString("context_type"));
    				Log.d(TAG, ctype);
    				final HashMap<String, ContextType> contexttypes = UpdateManager.getContextTypes();
    				ContextType type = (ContextType)contexttypes.get(ctype);
		            if(type!=null)
		            {
		            	Log.d(TAG, "type!=null");
		            	NotificationService.requestContext(ctype);
			            response = new CoapResponse(request.getMessageTypeName(), MessageCode.Name.CHANGED_204);
			            response.setContent(createPayloadFromAcutualStatus(ContentFormat.TEXT_PLAIN_UTF8), ContentFormat.TEXT_PLAIN_UTF8);

		
			            responseFuture.set(response);
		            }
		            else
		            {
		            	Log.d(TAG, "type==null");
		            }
    			}
    			if(scanConfig.getString("action_type").equals("unsubscribe") && scanConfig.containsKey("context_type"))
    			{
    				String ctype = scanConfig.getString("context_type");
    				final HashMap<String, ContextType> contexttypes = UpdateManager.getContextTypes();
    				ContextType type = (ContextType)contexttypes.get(ctype);
		            if(type!=null)
		            {
		            	Log.d(TAG, "type!=null");
		            	NotificationService.requestUnsubscribeContext(ctype);
			            response = new CoapResponse(request.getMessageTypeName(), MessageCode.Name.CHANGED_204);
			            response.setContent(createPayloadFromAcutualStatus(ContentFormat.TEXT_PLAIN_UTF8), ContentFormat.TEXT_PLAIN_UTF8);
		
			            responseFuture.set(response);
		            }
    			}
    		}
            //create response


        }
        catch(Exception e)
        {
            response = new CoapResponse(request.getMessageTypeName(), MessageCode.Name.BAD_REQUEST_400);
            response.setContent(e.getMessage().getBytes(Charset.forName("UTF-8")), ContentFormat.TEXT_PLAIN_UTF8);
            responseFuture.set(response);
        }
		
	}

	private void processGet(SettableFuture<CoapResponse> responseFuture, CoapRequest request) throws InvalidHeaderException 
	{
		Set<Long> acceptOptions =request.getAcceptedContentFormats();

		try
		{
	        //If accept option is not set in the request, use the default (TEXT_PLAIN)
	        if(acceptOptions.isEmpty())
	        {
	        	Log.d(TAG, "accept optioon is empty");
	            CoapResponse response = new CoapResponse(request.getMessageTypeName(), MessageCode.Name.CONTENT_205);
	            response.setContent(createPayloadFromAcutualStatus(ContentFormat.TEXT_PLAIN_UTF8), ContentFormat.TEXT_PLAIN_UTF8);
	            responseFuture.set(response);
	        }
	
	        for(long option : acceptOptions){
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
			Log.d(TAG, "Exception at process get ");
		}

        //This is only reached if all accepted mediatypes are not supported!
        CoapResponse response = new CoapResponse(request.getMessageTypeName(), MessageCode.Name.UNSUPPORTED_CONTENT_FORMAT_415);
        responseFuture.set(response);
		
	}

    private byte[] createPayloadFromAcutualStatus(long mediaType)
    {
    	Log.d(TAG, ""+mediaType);
		byte[] r = ManagerManager.createContextTypeListResponse(""+mediaType);
		return r;
    }

	@Override
	public void processCoapRequest(SettableFuture responseFuture, CoapRequest request, InetSocketAddress remoteAddress) 
	{
		 Log.d(TAG, "Service " + getPath() + " received request: " + request);
	        try
	        {
	            if(request.getMessageCode() == MessageCode.Name.GET.getNumber())
	            {
	                processGet(responseFuture, request);
	            }
	            else if(request.getMessageCode() == MessageCode.Name.PUT.getNumber())
	            {
	                processPut(responseFuture, request);
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
					e1.printStackTrace();
				}
	        }
		
	}

	@Override
	public void shutdown() 
	{

		
	}

    @Override
    public byte[] getEtag(long contentFormat)
    {
        //TODO: I have no idea what this method even does...
        return new byte[0];
    }

    @Override
	public void updateEtag(String resourceStatus) {
		// TODO Auto-generated method stub
		
	}

	@Override
	public byte[] getSerializedResourceStatus(long contentFormatNumber){
		// TODO Auto-generated method stub
		return null;
	}


}

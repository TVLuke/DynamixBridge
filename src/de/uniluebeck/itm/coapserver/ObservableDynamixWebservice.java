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

import static de.uniluebeck.itm.ncoap.message.options.OptionRegistry.MediaType.TEXT_PLAIN_UTF8;

import java.net.InetSocketAddress;
import java.nio.charset.Charset;
import java.util.List;
import java.util.concurrent.ScheduledExecutorService;
import java.util.concurrent.TimeUnit;

import org.ambientdynamix.api.application.ContextEvent;

import android.os.Bundle;
import android.util.Log;

import com.google.common.util.concurrent.SettableFuture;

import de.uniluebeck.itm.dynamixsspbridge.core.ManagerManager;
import de.uniluebeck.itm.dynamixsspbridge.dynamix.ContextType;
import de.uniluebeck.itm.ncoap.application.server.webservice.MediaTypeNotSupportedException;
import de.uniluebeck.itm.ncoap.application.server.webservice.ObservableWebService;
import de.uniluebeck.itm.ncoap.message.CoapRequest;
import de.uniluebeck.itm.ncoap.message.CoapResponse;
import de.uniluebeck.itm.ncoap.message.header.Code;
import de.uniluebeck.itm.ncoap.message.options.Option;
import de.uniluebeck.itm.ncoap.message.options.UintOption;
import de.uniluebeck.itm.ncoap.message.options.OptionRegistry.MediaType;
import de.uniluebeck.itm.ncoap.message.options.OptionRegistry.OptionName;

public class ObservableDynamixWebservice  extends ObservableWebService<ContextEvent> 
{
	private static String TAG ="SSPBridge";
	private ContextType contexttype;
	private int updateintervall=10000;
	
	public ObservableDynamixWebservice(ContextType contexttype, int updateintervall)
	{
		super("/"+contexttype.getName().replace(".", "/"), contexttype.getCurrentEvent());
		this.updateintervall=updateintervall;
		int age = updateintervall/1000;
		if(age<1)
		{
			age=1;
		}
		setMaxAge(age);
		this.contexttype=contexttype;
		contexttype.registerForUpdates(this);
		Log.d(TAG, "new Service has been established for "+contexttype.getName());

	}
	
	@Override
	public void shutdown() 
	{
		contexttype.deactivate();
	}

	@Override
    public void setScheduledExecutorService(ScheduledExecutorService executorService)
	{
       super.setScheduledExecutorService(executorService);
       schedulePeriodicResourceUpdate();
    }
	
	private void schedulePeriodicResourceUpdate()
	{
        getScheduledExecutorService().scheduleAtFixedRate(new Runnable()
        {

            @Override
            public void run() 
            {
                setResourceStatus(contexttype.getCurrentEvent());
                Log.d(TAG, "New status of resource " + getPath() + ": " + getResourceStatus().getContextType());
            }
        },0, updateintervall, TimeUnit.MILLISECONDS);
    }
	
	@Override
	public void processCoapRequest(SettableFuture<CoapResponse> responseFuture, CoapRequest coapRequest, InetSocketAddress remoteAddress) 
	{
		Log.d(TAG, "got request");
		try
		{
            if(coapRequest.getCode() == Code.GET)
            {
    			Log.d(TAG, "GET");
                processGet(responseFuture, coapRequest);
            }
            else if(coapRequest.getCode() == Code.POST)
            {
    			Log.d(TAG, "POST");
                processPost(responseFuture, coapRequest);
            }
            else
            {
                responseFuture.set(new CoapResponse(Code.METHOD_NOT_ALLOWED_405));
            }
        }
        catch(Exception e)
        {
            Log.e(TAG, ""+e);
            responseFuture.set(new CoapResponse(Code.INTERNAL_SERVER_ERROR_500));
        }
	}

	private void processPost(SettableFuture<CoapResponse> responseFuture, CoapRequest request)
	{
		CoapResponse response = null;
        Bundle scanConfig = new Bundle();
        //parse new status value
        String payload = request.getPayload().toString(Charset.forName("UTF-8"));
        MediaType requestMediaType =  request.getContentType();
        Log.d(TAG, "POST: "+payload);
        scanConfig = ManagerManager.parseRequest(payload, requestMediaType);    
        ManagerManager.updateToReleventEvent(contexttype, scanConfig);

		List<Option> acceptOptions = request.getOption(OptionName.ACCEPT);
		try
		{
			//If accept option is not set in the request, use the default (TEXT_PLAIN)
		        if(acceptOptions.isEmpty())
		        {
		        	Log.d(TAG, "accept optioon is empty");
		            response = new CoapResponse(Code.CONTENT_205);
		            response.setPayload(ManagerManager.createPayloadFromAcutualStatus(TEXT_PLAIN_UTF8, contexttype, false));
		            response.setContentType(TEXT_PLAIN_UTF8);
		            responseFuture.set(response);
		        }
		
		        for(Option option : request.getOption(OptionName.ACCEPT))
		        {
		            MediaType acceptedMediaType = MediaType.getByNumber(((UintOption) option).getDecodedValue());
		            Log.d(TAG, "Try to create payload for accepted mediatype " + acceptedMediaType);
		            byte[] payloadx = ManagerManager.createPayloadFromAcutualStatus(acceptedMediaType, contexttype, false);
		            //the requested mediatype is supported
		            if(payloadx != null)
		            {
		                response = new CoapResponse(Code.CONTENT_205);
		                response.setPayload(payloadx);
		                response.setContentType(acceptedMediaType);
		                responseFuture.set(response);
		            }
		        }
			}
			catch(Exception e)
			{
				Log.d(TAG, "Exception at process get");
			}
		  responseFuture.set(response);
	}
	
	private void processGet(SettableFuture<CoapResponse> responseFuture, CoapRequest request)
	{
		Log.d(TAG, "process get");
		List<Option> acceptOptions = request.getOption(OptionName.ACCEPT);

		try
		{
	        //If accept option is not set in the request, use the default (TEXT_PLAIN)
	        if(acceptOptions.isEmpty())
	        {
	        	Log.d(TAG, "accept optioon is empty");
	            CoapResponse response = new CoapResponse(Code.CONTENT_205);
	            response.setPayload(ManagerManager.createPayloadFromAcutualStatus(TEXT_PLAIN_UTF8, contexttype, false));
	            response.setContentType(TEXT_PLAIN_UTF8);
	            responseFuture.set(response);
	        }
	
	        for(Option option : request.getOption(OptionName.ACCEPT)){
	            MediaType acceptedMediaType = MediaType.getByNumber(((UintOption) option).getDecodedValue());
	            Log.d(TAG, "Try to create payload for accepted mediatype " + acceptedMediaType);
	            byte[] payload = ManagerManager.createPayloadFromAcutualStatus(acceptedMediaType, contexttype, false);
	
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
	
   	@Override
	public byte[] getSerializedResourceStatus(MediaType mediaType) throws MediaTypeNotSupportedException 
	{
		return ManagerManager.createPayloadFromAcutualStatus(mediaType, contexttype, false);
	}
	
}

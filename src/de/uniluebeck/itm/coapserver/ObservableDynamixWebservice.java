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
import java.util.Set;
import java.util.concurrent.ScheduledExecutorService;
import java.util.concurrent.TimeUnit;

import org.ambientdynamix.api.application.ContextEvent;

import android.os.Bundle;
import android.util.Log;

import com.google.common.primitives.Ints;
import com.google.common.primitives.Longs;
import com.google.common.util.concurrent.SettableFuture;

import de.uniluebeck.itm.dynamixsspbridge.core.ManagerManager;
import de.uniluebeck.itm.dynamixsspbridge.dynamix.ContextType;
import de.uniluebeck.itm.ncoap.application.server.webservice.ObservableWebservice;
import de.uniluebeck.itm.ncoap.message.CoapRequest;
import de.uniluebeck.itm.ncoap.message.CoapResponse;
import de.uniluebeck.itm.ncoap.message.InvalidHeaderException;
import de.uniluebeck.itm.ncoap.message.MessageCode;
import de.uniluebeck.itm.ncoap.message.options.ContentFormat;

public class ObservableDynamixWebservice extends ObservableWebservice<ContextEvent>
{
	private static String TAG ="SSPBridge";
	private ContextType contexttype;
	private int updateintervall=10000;
    private int weakEtag;
	
	public ObservableDynamixWebservice(ContextType contexttype, int updateintervall)
	{
		super("/"+contexttype.getName().replace(".", "/"), contexttype.getCurrentEvent(), updateintervall/1000);
		Log.d(TAG, "constructor line 2");
		this.updateintervall=updateintervall;
		Log.d(TAG, "constructor line 3");
		int age = updateintervall/1000;
		if(age<1)
		{
			age=1;
		}
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

    //TODO: This method is new... Probably this is not what it should do.
    @Override
    public byte[] getEtag(long contentFormat)
    {
        return Ints.toByteArray(weakEtag & Longs.hashCode(contentFormat));
    }

    private void schedulePeriodicResourceUpdate()
	{
        Log.e(TAG, "schedule periodic resource update.");
        getScheduledExecutorService().scheduleAtFixedRate(new Runnable()
        {

            @Override
            public void run() 
            {
                setResourceStatus(contexttype.getCurrentEvent(), (updateintervall/1000));
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
            if(coapRequest.getMessageCode() == MessageCode.Name.GET.getNumber())
            {
    			Log.d(TAG, "GET - THERE SHOULD NOT BE GET REQUESTS REALLY... ");
    			Log.d(TAG, remoteAddress.getAddress().getHostAddress());
    			String query = coapRequest.getUriQuery();
    			if(query!=null)
    			{
    				Log.d(TAG, "Query "+query);
    			}
                processGet(responseFuture, coapRequest, query);
            }
            else if(coapRequest.getMessageCode() == MessageCode.Name.POST.getNumber())
            {
    			Log.d(TAG, "POST");
    			Log.d(TAG, remoteAddress.getAddress().getHostAddress());
                processPost(responseFuture, coapRequest);
            }
            else
            {
                responseFuture.set(new CoapResponse(coapRequest.getMessageTypeName(), MessageCode.Name.METHOD_NOT_ALLOWED_405));
            }
        }
        catch(Exception e)
        {
            Log.e(TAG, ""+e);
            try {
				responseFuture.set(new CoapResponse(coapRequest.getMessageTypeName(), MessageCode.Name.INTERNAL_SERVER_ERROR_500));
			} catch (Exception e1) {
				e1.printStackTrace();
			}
        }
	}

	private void processPost(SettableFuture<CoapResponse> responseFuture, CoapRequest request)
	{
        Log.d(TAG, "gotPost");
		CoapResponse response = null;
        Bundle scanConfig = new Bundle();
        //parse new status value
        String payload = request.getContent().toString(Charset.forName("UTF-8"));
        long requestMediaType =  request.getContentFormat();
        Log.d(TAG, "POST: "+payload);
        scanConfig = ManagerManager.parseRequest(MessageCode.Name.POST, payload, requestMediaType); 
        String accesstoken="";
        if(contexttype.isPublic())
        {
        	//everything is fine... get the token anyway, if its there. You never know what it might be good for
        	if(scanConfig.containsKey("token"))
        	{
        		accesstoken = scanConfig.getString("token");
        		
        	}
        }
        else
        {
        	//one has to be authorized for this... so, at least there hast to be a tolken in the request
        	if(scanConfig.containsKey("token"))
        	{
        		accesstoken = scanConfig.getString("token");
        		
        	}
        }
        ManagerManager.updateToReleventEvent(contexttype, scanConfig);

		Set<Long> acceptOptions = request.getAcceptedContentFormats();
		try
		{
			//If accept option is not set in the request, use the default (APP_XML)
		        if(acceptOptions.isEmpty())
		        {
		        	Log.d(TAG, "accept optioon is empty");
		            response = new CoapResponse(request.getMessageTypeName(), MessageCode.Name.CONTENT_205);
		            response.setContent(ManagerManager.createPayloadFromAcutualStatus(ContentFormat.APP_XML, contexttype, false), ContentFormat.APP_XML);
		            responseFuture.set(response);
		        }
		
		        for(long option : acceptOptions)
		        {
		            long acceptedMediaType = option;
		            Log.d(TAG, "Try to create payload for accepted mediatype " + acceptedMediaType);
		            byte[] payloadx = ManagerManager.createPayloadFromAcutualStatus(acceptedMediaType, contexttype, false);
		            //the requested mediatype is supported
		            if(payloadx != null)
		            {
		                response = new CoapResponse(request.getMessageTypeName(), MessageCode.Name.CONTENT_205);
		                response.setContent(payloadx, acceptedMediaType);
		                responseFuture.set(response);
		            }
		        }
		}
		catch(Exception e)
		{
			Log.d(TAG, "Exception at process get in the process POST method of ObservableDynamixWebservice: "+e.getMessage());
		}
		responseFuture.set(response);
	}
	
	private void processGet(SettableFuture<CoapResponse> responseFuture, CoapRequest request, String query) throws InvalidHeaderException
	{
		Log.d(TAG, "process get");
		Set<Long> acceptOptions = request.getAcceptedContentFormats();
		 Bundle scanConfig = new Bundle();
	     //parse new status value
	     String payload = request.getContent().toString(Charset.forName("UTF-8"));
	     long requestMediaType =  request.getContentFormat();
	     Log.d(TAG, "GET: "+payload);
	     scanConfig = ManagerManager.parseRequest(MessageCode.Name.GET, query, requestMediaType); 
	     String accesstoken="";
	        if(contexttype.isPublic())
	        {
	        	//everything is fine... get the token anyway, if its there. You never know what it might be good for
	        	if(scanConfig.containsKey("token"))
	        	{
	        		accesstoken = scanConfig.getString("token");
	        		
	        	}
	        }
	        else
	        {
	        	//one has to be authorized for this... so, at least there hast to be a tolken in the request
	        	if(scanConfig.containsKey("token"))
	        	{
	        		accesstoken = scanConfig.getString("token");
	        		
	        	}
	        }
	        ManagerManager.updateToReleventEvent(contexttype, scanConfig);

		try
		{
	        //If accept option is not set in the request, use the default (TEXT_PLAIN)
	        if(acceptOptions.isEmpty())
	        {
	        	Log.d(TAG, "accept optioon is empty ");
	            CoapResponse response = new CoapResponse(request.getMessageTypeName(), MessageCode.Name.CONTENT_205);
	            response.setContent(ManagerManager.createPayloadFromAcutualStatus(ContentFormat.APP_XML, contexttype, false), ContentFormat.APP_XML);
	            responseFuture.set(response);
	        }
	
	        for(long option : acceptOptions){
	            long acceptedMediaType = option;
	            Log.d(TAG, "Try to create payload for accepted mediatype " + acceptedMediaType);
	            byte[] payloadx = ManagerManager.createPayloadFromAcutualStatus(acceptedMediaType, contexttype, false);
	
	            //the requested mediatype is supported
	            if(payloadx != null)
	            {
	                CoapResponse response = new CoapResponse(request.getMessageTypeName(), MessageCode.Name.CONTENT_205);
	                response.setContent(payloadx, acceptedMediaType);
	                responseFuture.set(response);
	            }
	        }
		}
		catch(Exception e)
		{
			Log.d(TAG, "Exception at process get in the ObservableDynamixWebService");
		}

        //This is only reached if all accepted mediatypes are not supported!
        CoapResponse response = new CoapResponse(request.getMessageTypeName(), MessageCode.Name.UNSUPPORTED_CONTENT_FORMAT_415);
        responseFuture.set(response);


	}

	@Override
	public void updateEtag(ContextEvent resourceStatus)
	{
        Log.d(TAG, "update ETAG");
        byte[] etag = new byte[4];
        if(resourceStatus!=null)
        {
            String rs = ""+resourceStatus.getExpireMills();
            //TODO: später vernünftig machen!
            if(rs.getBytes().length>3)
            {
                etag[0]= rs.getBytes()[0];
                etag[1]= rs.getBytes()[1];
                etag[2]= rs.getBytes()[2];
                etag[3]= rs.getBytes()[3];
            }
            else
            {
                etag[0]= rs.getBytes()[0];
                etag[1]= rs.getBytes()[0];
                etag[2]= rs.getBytes()[0];
                etag[3]= rs.getBytes()[0];
            }
        }
        else
        {
            etag[0]= 1;
            etag[1]= 1;
            etag[2]= 1;
            etag[3]= 1;
        }
	}


	@Override
	public byte[] getSerializedResourceStatus(long contentFormat) {
		return ManagerManager.createPayloadFromAcutualStatus(contentFormat, contexttype, false);
	}
	

	
}

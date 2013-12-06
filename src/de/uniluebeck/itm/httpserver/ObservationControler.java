package de.uniluebeck.itm.httpserver;

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


import java.util.concurrent.ConcurrentHashMap;

import org.jboss.netty.buffer.ChannelBuffer;
import org.jboss.netty.buffer.ChannelBuffers;
import org.jboss.netty.handler.codec.http.HttpResponseStatus;

import android.os.Bundle;
import android.util.Log;

import com.strategicgains.restexpress.ContentType;
import com.strategicgains.restexpress.Format;
import com.strategicgains.restexpress.Request;
import com.strategicgains.restexpress.Response;

import de.uniluebeck.itm.dynamixsspbridge.core.ManagerManager;
import de.uniluebeck.itm.dynamixsspbridge.core.UpdateManager;
import de.uniluebeck.itm.dynamixsspbridge.dynamix.ContextType;
import de.uniluebeck.itm.dynamixsspbridge.support.NotificationService;
import de.uniluebeck.itm.ncoap.message.options.OptionRegistry.MediaType;

public class ObservationControler 
{

	private static String TAG ="SSPBridge";
	private ContextType contexttype;

	public void create(Request request, Response response)
	{
		Log.i(TAG, " ObservationControler PUT");
		response.setResponseCreated();
		String format = request.getFormat();
		Log.d(TAG, "requested Format = "+format);
		ChannelBuffer body = request.getBody();
		byte[] ba = body.array();
		String payload = new String(ba);
		//MediaType is strange in this lib...
		String f = request.getFormat();
		MediaType mt = null;
		if(f.equals(Format.TXT))
		{
			mt = MediaType.TEXT_PLAIN_UTF8;
		}
		if(f.equals(Format.JSON))
		{
			mt = MediaType.APP_JSON;
		}
		if(f.equals(Format.XML))
		{
			mt = MediaType.APP_XML;
		}
		Bundle scanConfig = ManagerManager.parseRequest(payload, mt);
		if(scanConfig.containsKey("action_type"))
		{
			if(scanConfig.getString("action_type").equals("subscribe") && scanConfig.containsKey("context_type"))
			{
				Log.d(TAG, "PUT subscribe and contains context_type");
				String ctype = scanConfig.getString("context_type");
				Log.d(TAG, scanConfig.getString("context_type"));
				Log.d(TAG, ctype);
				Log.d(TAG, "getall");
				final ConcurrentHashMap<String, ContextType> contexttypes = UpdateManager.getContextTypes();
		        ContextType type = (ContextType)contexttypes.get(ctype);
		        if(type!=null)
		        {
		           	Log.d(TAG, "type!=null");
		           	NotificationService.requestContext(ctype);
		           	response.setResponseStatus(HttpResponseStatus.CREATED);
		        }
		        else
		        {
		        	Log.d(TAG, "type==null");
		        	response.setResponseStatus(HttpResponseStatus.NOT_FOUND);
		        }
			}
			if(scanConfig.getString("action_type").equals("unsubscribe") && scanConfig.containsKey("context_type"))
			{
				Log.d(TAG, "PUT subscribe and contains context_type");
				String ctype = scanConfig.getString("context_type");
				Log.d(TAG, scanConfig.getString("context_type"));
				Log.d(TAG, ctype);
				Log.d(TAG, "getall");
				final ConcurrentHashMap<String, ContextType> contexttypes = UpdateManager.getContextTypes();
		        ContextType type = (ContextType)contexttypes.get(ctype);
		        if(type!=null)
		        {
		           	NotificationService.requestUnsubscribeContext(ctype);
		           	response.setResponseStatus(HttpResponseStatus.CREATED);
		        }
		        else
		        {
		        	response.setResponseStatus(HttpResponseStatus.NOT_FOUND);
		        }
			}
		}
	}

	public String delete(Request request, Response response)
	{
		Log.i(TAG, " ObservationControler DELETE");
		response.setResponseCreated();
		return "tatda";
	}

	public void read(Request request, Response response)
	{
		Log.i(TAG, " ObservationControler GET");
		String format = request.getFormat();
		Log.d(TAG, "requested Format = "+format);
		if(!(format!=null))
		{
			format=Format.XML;
		}
		Log.d(TAG, "requested Format = "+format);
		byte[] r = ManagerManager.createContextTypeListResponse(format);
		response.setResponseStatus(HttpResponseStatus.OK);
		
		if(format.equals(Format.XML))
		{
			response.setContentType(Format.XML);
			response.addHeader("version", "1.0");
			response.setResponseProcessor(ResponseProcessors.xml());
			response.setBody(new String(r));			
			response.setResponseCreated();
		}
		if(format.equals(Format.JSON))
		{
			response.setResponseStatus(HttpResponseStatus.UNSUPPORTED_MEDIA_TYPE);
			response.setResponseCreated();
		}
		response.setResponseStatus(HttpResponseStatus.UNSUPPORTED_MEDIA_TYPE);
		response.setResponseCreated();
	}

	public String update(Request request, Response response)
	{
		Log.i(TAG, " ObservationControler PUT");
		response.setResponseCreated();
		return "tatda";
	}
	
	
}

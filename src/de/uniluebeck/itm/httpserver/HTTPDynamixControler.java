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

package de.uniluebeck.itm.httpserver;

import org.jboss.netty.buffer.ChannelBuffer;
import org.jboss.netty.handler.codec.http.HttpResponseStatus;

import android.os.Bundle;
import android.util.Log;

import com.strategicgains.restexpress.Format;
import com.strategicgains.restexpress.Request;
import com.strategicgains.restexpress.Response;

import de.uniluebeck.itm.dynamixsspbridge.core.ManagerManager;
import de.uniluebeck.itm.dynamixsspbridge.dynamix.ContextType;
import de.uniluebeck.itm.ncoap.message.options.ContentFormat;
import de.uniluebeck.itm.ncoap.message.MessageCode;

public class HTTPDynamixControler 
{


	private static String TAG ="SSPBridge";
	private ContextType contexttype;
	private int updateintervall;
	
	public HTTPDynamixControler (ContextType contexttype, int updateintervall)
	{
		this.contexttype=contexttype;	
		this.updateintervall=updateintervall;
		contexttype.registerForUpdates(this);
	}
	
	public void create(Request request, Response response)
	{
		
	}
	
	public String delete(Request request, Response response)
	{
		return "x";	
	}
	
	public void read(Request request, Response response)
	{
		Log.i(TAG, " HTTPDynamix GET");
		Log.i(TAG, "Adress"+request.getRemoteAddress().getAddress());
		response.setResponseCreated();
		String format = request.getFormat();
		Log.d(TAG, "requested Format = "+format);
		//BODY
		ChannelBuffer body = request.getBody();
		byte[] ba = body.array();
		String payload = new String(ba);
		if(!(format!=null))
		{
			format=Format.TXT;
		}
		if(format.equals(Format.TXT))
		{
			byte[] r = ManagerManager.createPayloadFromAcutualStatus(ContentFormat.TEXT_PLAIN_UTF8, contexttype, false);
			response.setContentType(Format.TXT);
			response.setBody(new String(r));			
			response.setResponseCreated();
		}
		if(format.equals(Format.XML))
		{
            Log.d(TAG, "ok, calling createPayloadfromactualstatus now...");
			byte[] r = ManagerManager.createPayloadFromAcutualStatus(ContentFormat.APP_XML, contexttype, false);
            Log.d(TAG, "got a payload back...");
			response.setContentType(Format.XML);
			response.addHeader("version", "1.0");
			response.setResponseProcessor(ResponseProcessors.xml());
			response.setBody(new String(r));			
			response.setResponseCreated();
            Log.d(TAG, "done with creating the response... should be all good.");
		}

	}
	
	public void update(Request request, Response response)
	{
		Log.i(TAG, " ObservationControler POST");
		String format = request.getFormat();
		Log.d(TAG, "requested Format = "+format);
		//BODY
		ChannelBuffer body = request.getBody();
		byte[] ba = body.array();
		String payload = new String(ba);
		Log.d(TAG, "getall");
		Log.d(TAG, payload);
		Bundle scanConfig = new Bundle();
		String f = request.getFormat();
		Long mt = null;
		if(f.equals(Format.TXT))
		{
			mt = ContentFormat.TEXT_PLAIN_UTF8;
		}
		if(f.equals(Format.JSON))
		{
			mt = ContentFormat.APP_JSON;
		}
		if(f.equals(Format.XML))
		{
			mt = ContentFormat.APP_XML;
		}
		scanConfig = ManagerManager.parseRequest(MessageCode.Name.POST, payload, mt);
		Log.d(TAG, "x");
	    ManagerManager.updateToReleventEvent(contexttype, scanConfig);
	    //TODO: if Bundle contains a command to compres only transmit the dif between this status and the previous one.
		if(!(format!=null))
		{
			format=Format.TXT;
		}
		if(format.equals(Format.TXT))
		{
			byte[] r = ManagerManager.createPayloadFromAcutualStatus(ContentFormat.TEXT_PLAIN_UTF8, contexttype, false);
			response.setContentType(Format.TXT);
			response.setBody(new String(r));			
			response.setResponseCreated();
		}
		if(format.equals(Format.XML))
		{
			byte[] r = ManagerManager.createPayloadFromAcutualStatus(ContentFormat.APP_XML, contexttype, false);
			response.setContentType(Format.XML);
			response.addHeader("version", "1.0");
			response.setResponseProcessor(ResponseProcessors.xml());
			response.setBody(new String(r));			
			response.setResponseCreated();
		}
	    Log.d(TAG, "y");
		response.setResponseStatus(HttpResponseStatus.ACCEPTED);
		Log.d(TAG, "z");
	}
}

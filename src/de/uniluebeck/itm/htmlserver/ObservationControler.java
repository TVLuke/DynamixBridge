package de.uniluebeck.itm.htmlserver;

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

import org.jboss.netty.handler.codec.http.HttpResponseStatus;

import android.util.Log;

import com.strategicgains.restexpress.Format;
import com.strategicgains.restexpress.Request;
import com.strategicgains.restexpress.Response;

import de.uniluebeck.itm.dynamixsspbridge.core.UpdateManager;
import de.uniluebeck.itm.dynamixsspbridge.dynamix.ContextType;

public class ObservationControler {

		private static String TAG ="SSPBridge";
	

	public void create(Request request, Response response)
	{
		Log.i(TAG, " ObservationControler PUT");
		response.setResponseCreated();
		String format = request.getFormat();
		Log.d(TAG, "requested Format = "+format);
		if(format!=null)
		{
			
		}
		else
		{
			format=Format.XML;
		}
		String action = (String) request.getParameter("actiontype");
		Log.d(TAG, 	action );
		response.setResponseStatus(HttpResponseStatus.CREATED);
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
		if(format!=null)
		{
			
		}
		else
		{
			format=Format.XML;
		}
		response.setResponseStatus(HttpResponseStatus.OK);
		if(format.equals(Format.XML))
		{
			response.setContentType(Format.XML);
			response.addHeader("version", "1.0");
			response.setResponseProcessor(ResponseProcessors.xml());
			ConcurrentHashMap<String, ContextType> types = UpdateManager.getContextTypes();
			String result ="<contexttypes>\n";
			for(int i=0; i<types.size(); i++)
	    	{
	    		String key = (String) types.keySet().toArray()[i];
	    		ContextType type = types.get(key);
	    		//payload=payload+type.getName();
	    		 result=result+"  <contexttype>\n";
	    		 result=result+"    <name>"+type.getName()+"</name>\n";
	    		 result=result+"    <readablename>"+type.getUserFriendlyName()+"</readablename>\n";
	    		 result=result+"    <description>"+type.getDescription()+"</description>\n";
	    		 result=result+"    <active>"+type.active()+"</active>\n";
	    		 if(type.active())
	    		 {
	    			 result=result+"     <url>http://"+UpdateManager.getIP()+"/"+type.getName().replace(".", "/")+"/</url>\n";
	    		 }
	    		 result=result+"  </contexttype>\n";
	    	}
			result=result+"</contexttypes>";
			response.setBody(result);
			//response.serialize();
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

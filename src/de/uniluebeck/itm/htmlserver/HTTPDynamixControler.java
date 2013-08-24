package de.uniluebeck.itm.htmlserver;

import static de.uniluebeck.itm.ncoap.message.options.OptionRegistry.MediaType.TEXT_PLAIN_UTF8;
import static de.uniluebeck.itm.ncoap.message.options.OptionRegistry.MediaType.APP_XML;

import org.jboss.netty.buffer.ChannelBuffer;
import org.jboss.netty.handler.codec.http.HttpResponseStatus;

import android.os.Bundle;
import android.util.Log;

import com.strategicgains.restexpress.Format;
import com.strategicgains.restexpress.Request;
import com.strategicgains.restexpress.Response;

import de.uniluebeck.itm.dynamixsspbridge.core.ManagerManager;
import de.uniluebeck.itm.dynamixsspbridge.dynamix.ContextType;

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
			byte[] r = ManagerManager.createPayloadFromAcutualStatus(TEXT_PLAIN_UTF8, contexttype);
			response.setContentType(Format.TXT);
			response.setBody(new String(r));			
			response.setResponseCreated();
		}
		if(format.equals(Format.XML))
		{
			byte[] r = ManagerManager.createPayloadFromAcutualStatus(APP_XML, contexttype);
			response.setContentType(Format.XML);
			response.addHeader("version", "1.0");
			response.setResponseProcessor(ResponseProcessors.xml());
			response.setBody(new String(r));			
			response.setResponseCreated();
		}
		
	}
	
	public void update(Request request, Response response)
	{
		Log.i(TAG, " ObservationControler POST");
		response.setResponseCreated();
		String format = request.getFormat();
		Log.d(TAG, "requested Format = "+format);
		//BODY
		ChannelBuffer body = request.getBody();
		byte[] ba = body.array();
		String payload = new String(ba);
		Log.d(TAG, "getall");
		Log.d(TAG, payload);
		Bundle scanConfig = new Bundle();
		scanConfig = ManagerManager.parseRequest(payload); 
		Log.d(TAG, "x");
	    ManagerManager.updateToReleventEvent(contexttype, scanConfig);
	    Log.d(TAG, "y");
		response.setResponseStatus(HttpResponseStatus.ACCEPTED);
		Log.d(TAG, "z");
	}
}

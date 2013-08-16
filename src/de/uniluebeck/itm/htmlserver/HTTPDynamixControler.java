package de.uniluebeck.itm.htmlserver;

import android.util.Log;

import com.strategicgains.restexpress.Request;
import com.strategicgains.restexpress.Response;

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
	}
	
	public String update(Request request, Response response)
	{
		return "x";
	}
}

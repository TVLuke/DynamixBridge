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

package de.uniluebeck.itm.dynamixsspbridge.core;

import static de.uniluebeck.itm.ncoap.message.options.OptionRegistry.MediaType.*;

import java.lang.reflect.Method;
import java.nio.charset.Charset;
import java.util.ArrayList;
import java.util.Date;
import java.util.HashMap;
import java.util.Set;
import java.util.StringTokenizer;
import java.util.concurrent.ConcurrentHashMap;

import org.ambientdynamix.api.application.ContextEvent;
import org.ambientdynamix.api.application.IContextInfo;

import android.app.Service;
import android.content.Intent;
import android.os.Bundle;
import android.os.IBinder;
import android.util.Log;

import com.strategicgains.restexpress.RestExpress;

import de.uniluebeck.itm.coapserver.CoapServerManager;
import de.uniluebeck.itm.dynamixsspbridge.dynamix.ContextType;
import de.uniluebeck.itm.htmlserver.HTTPServerManager;
import de.uniluebeck.itm.ncoap.message.CoapResponse;
import de.uniluebeck.itm.ncoap.message.header.Code;
import de.uniluebeck.itm.ncoap.message.options.OptionRegistry.MediaType;
import difflib.DiffUtils;

/**
 * With more then only coap being suported we actually need to have a unified interface for all the Managers and then check which 
 * things are activated for what and act accordingly...
 *
 * @author lukas
 *
 */
public class ManagerManager extends Service
{

	private static String TAG ="SSPBridge";
	
	@Override
	public IBinder onBind(Intent arg0) {
		// TODO Auto-generated method stub
		return null;
	}
	
	@Override
	public void onCreate() 
	{
		super.onCreate();
		 Log.d(TAG, "ManagerManager on Create");
	}
	
	@Override
	public int onStartCommand(Intent intent, int flags, int startId) 
	{
		super.onStartCommand(intent, flags, startId);
		Log.i(TAG, "ManagerManager on Start");
		return START_STICKY;
	}
	
	@Override
	public void onDestroy() 
	{
		super.onDestroy();	
	}
	
	public static void startServer(String servername)
	{
		 Log.d(TAG, "ManagerManager startServer");
		CoapServerManager.startServer(servername);
		HTTPServerManager.startServer(servername);
	}
	
	public static void startServer(String servername, int port)
	{
		 Log.d(TAG, "ManagerManager startServe");
		
	}
	
	public static void stopAllServers()
	{
		 Log.e(TAG, "ManagerManager stopAllServers");
		CoapServerManager.stopAllServers();
		HTTPServerManager.stopAllServers();
	}
	
	public static void stopServer(String serverName)
	{
		 Log.e(TAG, "ManagerManager stopServer");

	}
	
	public static void addService(ContextType contexttype, int updateintervall)
	{
		 Log.d(TAG, "ManagerManager addService");

		CoapServerManager.addService(contexttype, updateintervall);
		HTTPServerManager.addService(contexttype, updateintervall);
		
	}
	
	public static void removeService(ContextType contexttype)
	{
		 Log.d(TAG, "ManagerManager removeService");

		CoapServerManager.removeService(contexttype);
		HTTPServerManager.removeService(contexttype);
	}
	
	public static String generateResponseFromBundle(Bundle b)
	{
     	Log.e(TAG, "ManagerManager generateResponsefromBundle");
		return "";
	}
	
	public static Bundle parseRequest(String payload)
	{
		 Log.d(TAG, "ManagerManager parseRequest");

		Bundle scanConfig = new Bundle();
		payload=payload.replace("=", " ");
		payload=payload.replace("\"","");
        StringTokenizer tk = new StringTokenizer(payload, ";;");
        ArrayList<Object> arguments = new ArrayList<Object>(); 
        while(tk.hasMoreTokens())
        {
        	try
        	{
        		//try to parse Strings into all kinds of stuff so there will probably be exeptions
            	String argument = tk.nextToken();
            	if(argument.contains("("))
            	{
            		//user generated classes can not be handled yet... this may come. or not. its an invitation for viruses.
            	}
            	else
            	{
            		//normal type. lets go
            		arguments.add(argument);
            		StringTokenizer tk2 = new StringTokenizer(argument, " ");
            		String[] argumentsArray = new String[tk2.countTokens()];
            		int counter=0;
            		if(tk2.countTokens()>=3)
            		{
	            		while(tk2.hasMoreTokens())
	            		{
	            			argumentsArray[counter]=tk2.nextToken();
	            			Log.d(TAG, argumentsArray[counter]);
	            			if(counter>2)
	            			{
	            				argumentsArray[2]=argumentsArray[2]+" "+argumentsArray[counter];
	            			}
	            			counter++;
	            		}
            		}
            		if(argumentsArray[0].contains("["))
            		{
            			argumentsArray[0]=argumentsArray[0].replace("[", "");
            			argumentsArray[0]=argumentsArray[0].replace("]", "");
            			//array of something
	            		if(argumentsArray[0].equals("boolean"))
	            		{
	            			boolean[] x;
	            			if(argumentsArray[2].contains("{"))
	            			{
	            				//get rid of the bracets
	            				argumentsArray[2].replace("{", "");
	            				argumentsArray[2].replace("}", "");
	            				StringTokenizer tk3 = new StringTokenizer(argumentsArray[2], ",");
	            				x = new boolean[tk3.countTokens()];
	            				int counter2=0;
	            				while(tk3.hasMoreTokens())
	            				{
	            					String v = tk3.nextToken();
	            					v=v.trim();
			            			if(v.equals("true"))
			            			{
			            				x[counter2]=true;
			            			}
			            			if(v.equals("false"))
			            			{
			            				x[counter2]=false;
			            			}
			            			counter2++;
	            				}
	            			}
	            			else
	            			{
	            				argumentsArray[2]=argumentsArray[2].trim();
	            				x = new boolean[1];
		            			if(argumentsArray[2].equals("true"))
		            			{
		            				x[0]=true;
		            			}
		            			if(argumentsArray[2].equals("false"))
		            			{
		            				x[0]=false;
		            			}
	            				
	            			}
	            			scanConfig.putBooleanArray(argumentsArray[1], x);
	            		}
	            		if(argumentsArray[0].equals("String"))
	            		{
	            			String[] x;
	            			if(argumentsArray[2].contains("{"))
	            			{
	            				//get rid of the bracets
	            				argumentsArray[2].replace("{", "");
	            				argumentsArray[2].replace("}", "");
	            				StringTokenizer tk3 = new StringTokenizer(argumentsArray[2], ",");
	            				x=new String[tk3.countTokens()];
	            				int counter2=0;
	            				while(tk3.hasMoreTokens())
	            				{
	            					String v = tk3.nextToken();
	            					v=v.trim();
	            					x[counter2]=v;
			            			counter2++;
	            				}
	            			}
	            			else
	            			{
	            				argumentsArray[2]=argumentsArray[2].trim();
	            				x = new String[1];
	            				x[0]=argumentsArray[2];
	            				
	            			}
	            			scanConfig.putStringArray(argumentsArray[1], x);
	            		}
	            		if(argumentsArray[0].equals("int"))
	            		{
	            			int[] x;
	            			if(argumentsArray[2].contains("{"))
	            			{
	            				//get rid of the bracets
	            				argumentsArray[2].replace("{", "");
	            				argumentsArray[2].replace("}", "");
	            				StringTokenizer tk3 = new StringTokenizer(argumentsArray[2], ",");
	            				int counter2=0;
	            				x = new int[tk3.countTokens()];
	            				while(tk3.hasMoreTokens())
	            				{
	            					String v = tk3.nextToken();
	            					v=v.trim();
	            					x[counter2]=Integer.parseInt(v);
			            			counter2++;
	            				}
	            			}
	            			else
	            			{
	            				//its just a single value, where { and } are optional.
	            				argumentsArray[2]=argumentsArray[2].trim();
	            				x = new int[1];
	            				x[0]=Integer.parseInt(argumentsArray[2]);
	            				
	            			}
	            			scanConfig.putIntArray(argumentsArray[1], x);
	            		}
	            		if(argumentsArray[0].equals("long"))
	            		{
	            			long[] x;
	            			if(argumentsArray[2].contains("{"))
	            			{
	            				//get rid of the bracets
	            				argumentsArray[2].replace("{", "");
	            				argumentsArray[2].replace("}", "");
	            				StringTokenizer tk3 = new StringTokenizer(argumentsArray[2], ",");
	            				int counter2=0;
	            				x = new long[tk3.countTokens()];
	            				while(tk3.hasMoreTokens())
	            				{
	            					String v = tk3.nextToken();
	            					v=v.trim();
	            					x[counter2]=Long.parseLong(v);
			            			counter2++;
	            				}
	            			}
	            			else
	            			{
	            				//its just a single value, where { and } are optional.
	            				argumentsArray[2]=argumentsArray[2].trim();
	            				x = new long[1];
	            				x[0]=Long.parseLong(argumentsArray[2]);

	            				
	            			}
	            			scanConfig.putLongArray(argumentsArray[1], x);
	            		}
	            		if(argumentsArray[0].equals("byte"))
	            		{
	            			byte[] x;
	            			if(argumentsArray[2].contains("{"))
	            			{
	            				//get rid of the bracets
	            				argumentsArray[2].replace("{", "");
	            				argumentsArray[2].replace("}", "");
	            				StringTokenizer tk3 = new StringTokenizer(argumentsArray[2], ",");
	            				int counter2=0;
	            				x=new byte[tk3.countTokens()];
	            				while(tk3.hasMoreTokens())
	            				{
	            					String v = tk3.nextToken();
	            					v=v.trim();
	            					x[counter2]=Byte.parseByte(v);
	            					counter2++;
	            				}
	            			}
	            			else
	            			{
	            				//its just a single value, where { and } are optional.
	            				argumentsArray[2]=argumentsArray[2].trim();
	            				x = new byte[1];
	            				x[0]=Byte.parseByte(argumentsArray[2]);
	            				
	            			}
	            			scanConfig.putByteArray(argumentsArray[1], x);
	            		}
	            		if(argumentsArray[0].equals("short"))
	            		{
	            			short[] x;
	            			if(argumentsArray[2].contains("{"))
	            			{
	            				//get rid of the bracets
	            				argumentsArray[2].replace("{", "");
	            				argumentsArray[2].replace("}", "");
	            				StringTokenizer tk3 = new StringTokenizer(argumentsArray[2], ",");
	            				int counter2=0;
	            				x = new short[tk3.countTokens()];
	            				while(tk3.hasMoreTokens())
	            				{
	            					String v = tk3.nextToken();
	            					v=v.trim();
	            					x[counter2]=Short.parseShort(v);
			            			counter2++;

	            				}
	            			}
	            			else
	            			{
	            				//its just a single value, where { and } are optional.
	            				argumentsArray[2]=argumentsArray[2].trim();
	            				x = new short[1];
	            				x[0]=Short.parseShort(argumentsArray[2]);
	            				//its just a single value, where { and } are optional.
	            				
	            			}
	            			scanConfig.putShortArray(argumentsArray[1], x);
	            		}
	            		if(argumentsArray[0].equals("float"))
	            		{
	            			float[] x;
	            			if(argumentsArray[2].contains("{"))
	            			{
	            				//get rid of the bracets
	            				argumentsArray[2].replace("{", "");
	            				argumentsArray[2].replace("}", "");
	            				StringTokenizer tk3 = new StringTokenizer(argumentsArray[2], ",");
	            				int counter2=0;
	            				x = new float[tk3.countTokens()];
	            				while(tk3.hasMoreTokens())
	            				{
	            					String v = tk3.nextToken();
	            					v=v.trim();
	            					x[counter2]=Float.parseFloat(v);
			            			counter2++;
	            				}
	            			}
	            			else
	            			{
	            				//its just a single value, where { and } are optional.
	            				argumentsArray[2]=argumentsArray[2].trim();
	            				x = new float[1];
	            				x[0]=Float.parseFloat(argumentsArray[2]);
	            				//its just a single value, where { and } are optional.
	            				
	            			}
	            			scanConfig.putFloatArray(argumentsArray[1], x);
	            		}
	            		if(argumentsArray[0].equals("double"))
	            		{
	            			double[] x;
	            			if(argumentsArray[2].contains("{"))
	            			{
	            				//get rid of the bracets
	            				argumentsArray[2].replace("{", "");
	            				argumentsArray[2].replace("}", "");
	            				StringTokenizer tk3 = new StringTokenizer(argumentsArray[2], ",");
	            				int counter2=0;
	            				x=new double[tk3.countTokens()];
	            				while(tk3.hasMoreTokens())
	            				{
	            					String v = tk3.nextToken();
	            					v=v.trim();
	            					x[counter2]=Double.parseDouble(v);
			            			counter2++;
	            				}
	            			}
	            			else
	            			{
	            				//its just a single value, where { and } are optional.
	            				argumentsArray[2]=argumentsArray[2].trim();
	            				x = new double[1];
	            				x[0]=Double.parseDouble(argumentsArray[2]);
	            				//its just a single value, where { and } are optional.
	            				
	            			}
	            			scanConfig.putDoubleArray(argumentsArray[1], x);
	            		}
	            		if(argumentsArray[0].equals("char"))
	            		{
	            			char[] x;
	            			if(argumentsArray[2].contains("{"))
	            			{
	            				//get rid of the bracets
	            				argumentsArray[2].replace("{", "");
	            				argumentsArray[2].replace("}", "");
	            				StringTokenizer tk3 = new StringTokenizer(argumentsArray[2], ",");
	            				int counter2=0;
	            				x = new char[tk3.countTokens()];
	            				while(tk3.hasMoreTokens())
	            				{
	            					String v = tk3.nextToken();
	            					v=v.trim();
	            					x[counter2]=v.charAt(0);
			            			counter2++;
	            				}
	            			}
	            			else
	            			{
	            				//its just a single value, where { and } are optional.
	            				argumentsArray[2]=argumentsArray[2].trim();
	            				x = new char[1];
	            				x[0]=argumentsArray[2].charAt(0);
	            				//its just a single value, where { and } are optional.
	            				
	            			}
	            			scanConfig.putCharArray(argumentsArray[1], x);
	            		}
            		}
            		else
            		{
            			//simple variables
	            		if(argumentsArray[0].equals("boolean"))
	            		{
	            			argumentsArray[2]=argumentsArray[2].trim();
	            			boolean x=true;
	            			if(argumentsArray[2].equals("true"))
	            			{
	            				x=true;
	            			}
	            			if(argumentsArray[2].equals("false"))
	            			{
	            				x=false;
	            			}
	            			scanConfig.putBoolean(argumentsArray[1], x);
	            		}
	            		if(argumentsArray[0].equals("String"))
	            		{
	            			argumentsArray[2]=argumentsArray[2].trim();
	            			scanConfig.putString(argumentsArray[1],argumentsArray[2]);
	            		}
	            		if(argumentsArray[0].equals("int"))
	            		{
	            			argumentsArray[2]=argumentsArray[2].trim();
	            			int x = Integer.parseInt(argumentsArray[2]);
	            			scanConfig.putInt(argumentsArray[1], x);
	            		}
	            		if(argumentsArray[0].equals("long"))
	            		{
	            			argumentsArray[2]=argumentsArray[2].trim();
	            			long x = Long.parseLong(argumentsArray[2]);
	            			scanConfig.putLong(argumentsArray[1], x);
	            		}
	            		if(argumentsArray[0].equals("byte"))
	            		{
	            			argumentsArray[2]=argumentsArray[2].trim();
	            			byte x = Byte.parseByte(argumentsArray[2]);
	            			scanConfig.putByte(argumentsArray[1], x);		
	            		}
	            		if(argumentsArray[0].equals("short"))
	            		{
	            			argumentsArray[2]=argumentsArray[2].trim();
	            			short x = Short.parseShort(argumentsArray[2]);
	            			scanConfig.putShort(argumentsArray[1], x);
	            		}
	            		if(argumentsArray[0].equals("float"))
	            		{
	            			argumentsArray[2]=argumentsArray[2].trim();
	            			float x = Float.parseFloat(argumentsArray[2]);
	            			scanConfig.putFloat(argumentsArray[1], x);
	            		}
	            		if(argumentsArray[0].equals("double"))
	            		{
	            			argumentsArray[2]=argumentsArray[2].trim();
	            			double x = Double.parseDouble(argumentsArray[2]);
	            			scanConfig.putDouble(argumentsArray[1], x);
	            		}
	            		if(argumentsArray[0].equals("char"))
	            		{
	            			argumentsArray[2]=argumentsArray[2].trim();
	            			char x = argumentsArray[2].charAt(0);
	            			scanConfig.putChar(argumentsArray[1], x);
	            		}
            		}
            	}
        	}
        	catch(Exception e)
        	{
        		Log.d(TAG, "something did not work during parsing");
        		return null;
        	}
        }
        return scanConfig;  
	}

	public static void updateToReleventEvent(ContextType contexttype, Bundle scanConfig) 
	{
		int counter=0;
		if(contexttype.getCurrentEvent()!=null)
		{
			Date d = new Date();
			if(contexttype.getCurrentEvent().getExpireMills()<d.getTime())
			{
				String rid = contexttype.getCurrentEvent().getResponseId();
				contexttype.requestConfiguredContextUpdate(scanConfig);
				while(rid.equals(contexttype.getCurrentEvent().getResponseId()) && counter<200)
				{
					//wait
					try 
					{
						Thread.sleep(5);
					} 
					catch (InterruptedException e) 
					{
						// TODO Auto-generated catch block
						e.printStackTrace();
					}
					counter++;
				}
			}
		}
		else
		{
			contexttype.requestConfiguredContextUpdate(scanConfig);
			while(!(contexttype.getCurrentEvent()!=null) && counter<50)
			{
				//wait
				try 
				{
					Thread.sleep(5);
				  } 
				  catch (InterruptedException e) 
				  {
					// TODO Auto-generated catch block
					e.printStackTrace();
				  }
				counter++;
			  } 
		  }
	}
	
	public static String createStringPayloadFromAcutualStatus(MediaType mediaType, ContextEvent event, ContextType contexttype)
	{
		Log.d(TAG, "payload from actual status");
    	Date d = new Date();
    	if(event!=null)
    	{
        	Log.d(TAG, "expire time "+event.getExpireTime().getTime());
        	Log.d(TAG, "D "+d.getTime());
        	long dif1 = event.getExpireTime().getTime()-d.getTime();
        	Date exp=new Date(event.getExpireTime().getTime());
			long thousand=31000000000000l;
			if(dif1>thousand)//THOUSAND YEAR BUG
			{
				Log.d(TAG, "Yey, thousand year bug"+event.getExpireTime().getYear());
				int theyear= event.getExpireTime().getYear();
				exp = new Date((theyear-1000), event.getExpireTime().getMonth(), event.getExpireTime().getDate(), event.getExpireTime().getHours(), event.getExpireTime().getMinutes(), event.getExpireTime().getSeconds());
				exp = new Date(exp.getTime()+10000);
			}
			Log.d(TAG,""+exp.getYear());
			dif1 = exp.getTime()-d.getTime();
        	Log.d(TAG, "Difference in millis="+dif1);
        	Log.d(TAG, "Difference in seconds="+dif1/1000);
        	Log.d(TAG, "Difference in minutes="+(dif1/1000)/60);
        	Log.d(TAG, "Difference in hours="+((dif1/1000)/60)/60);
        	
    		if(dif1>0)
    		{
				Set<String> formats = event.getStringRepresentationFormats();
		    	Log.d(TAG, "mediatype="+mediaType);				
				if(mediaType==APP_XML)
				{
					Log.d(TAG, "formatxml");
					if(formats.contains("XML"))
					{
						Log.d(TAG, "formats contained xml...");
						StringBuffer payload = new StringBuffer();
						String xmldata = event.getStringRepresentation("XML");
						//TODO: here be some wrapping
						String xmlresult =  "<contextEvent>\n" +
											"	<contextType>\n" +
											"		<id>" +event.getContextType() + "</id>\n"+
											"		<createdAt>"+event.getTimeStamp().getTime()+"</createdAt>\n"+
											"		<expires>"+event.expires()+"</expires>"+
											"		<expiresAt>"+event.getExpireTime().getTime()+"</expiresAt>\n"+
											"		<source>\n"+
											"			<plugin>\n"+
											"				<pluginId>"+event.getEventSource().getPluginId()+"</pluginId>\n"+
											"				<pluginName>"+event.getEventSource().getPluginName()+"</pluginName>\n"+
											"			</plugin>\n"+
											//TODO here should be stuff on source in terms of hardware and such things... but this requeres the app to have the jar types...
											"		</source>\n"+
											"	</contextType>\n" +
											"	<contextData>\n";
											if(event.getStringRepresentation("XML").endsWith("\n"))
											{
												xmlresult=xmlresult+"		"+event.getStringRepresentation("XML").replace("\n", "\n		");
											}
											else
											{
												xmlresult=xmlresult+"		"+event.getStringRepresentation("XML").replace("\n", "\n		")+"\n";
											}
											xmlresult=xmlresult+"	</contextData>\n"+
											"</contextEvent>";
						payload.append(xmlresult);
						return payload.toString();
					}
					else
					{
						
					}
				}
				if(mediaType == TEXT_PLAIN_UTF8)
				{
					Log.d(TAG, "format is Plain Stuff");
					if(formats.contains("text/plain"))
					{
						String payload =event.getStringRepresentation("text/plain"); 
						Log.d(TAG, payload);
						return payload.toString();
					}
				}
				if(mediaType == APP_JSON)
				{
					Log.d(TAG, "format is JSON");
					if(formats.contains("JSON"))
					{
						
					}
					else
					{
						

					}
				}
				if(mediaType == APP_N3)
				{
					if(formats.contains("N3"))
					{
						
					}
				}
				if(mediaType== APP_TURTLE)
				{
					if(formats.contains("Turtle"))
					{
						
					}
				}
				if(mediaType == APP_SHDT)
				{
					if(formats.contains("SHDT"))
					{
						
					}
				}
					
    		}
    		else
    		{
				Log.d(TAG, "event has expired");
				String payload =event.getStringRepresentation("text/plain"); 
				payload="event has expired, but just in case you care... "+payload;
				return payload.toString();
    		}
    	}
		else
		{
			Log.d(TAG, "random old stuff");
			Log.d(TAG, "requested update");
			contexttype.requestContextUpdateIfNeeded();
			String payload =" "; 
			return payload.toString();
		}
		return null;
	}
	
	public static byte[] createPayloadFromAcutualStatus(MediaType mediaType, ContextType contextType, boolean compress)
	{
		if(compress)
		{
			//TRying to use https://code.google.com/p/java-diff-utils/
			String payload1 = createStringPayloadFromAcutualStatus(mediaType, contextType.getPreviousEvent(), contextType);
			String payload2 = createStringPayloadFromAcutualStatus(mediaType, contextType.getCurrentEvent(), contextType);
			//DiffUtils.diff(payload1, payload2);
			return "".getBytes(Charset.forName("UTF-8"));
		}
		else
		{
			String payload = createStringPayloadFromAcutualStatus(mediaType, contextType.getCurrentEvent(), contextType);
			return payload.getBytes(Charset.forName("UTF-8"));
		}
	}
	
	public static byte[] createContextTypeListResponse(String format)
	{
		MediaType mediaType = APP_XML;
		if(format!=null)
		{
			return createContextTypeListResponse(mediaType);			
		}
		else
		{
			return createContextTypeListResponse(mediaType);
		}
	}
	
	public static byte[] createContextTypeListResponse(MediaType mediaType)
	{
		String payload = new String();
		ConcurrentHashMap<String, ContextType> types = UpdateManager.getContextTypes();
		if(mediaType==APP_XML)
		{
			payload ="<contexttypes>\n";
			for(int i=0; i<types.size(); i++)
	    	{
	    		String key = (String) types.keySet().toArray()[i];
	    		ContextType type = types.get(key);
	    		//payload=payload+type.getName();
	    		payload=payload+"  <contexttype>\n";
	    		payload=payload+"    <name>"+type.getName()+"</name>\n";
	    		payload=payload+"    <readablename>"+type.getUserFriendlyName()+"</readablename>\n";
	    		payload=payload+"    <description>"+type.getDescription()+"</description>\n";
	    		payload=payload+"    <active>"+type.active()+"</active>\n";
	    		 if(type.active())
	    		 {
	    			 payload=payload+"     <url>\n";
	    			 payload=payload+"         <http>http://"+UpdateManager.getIP()+":8081/"+type.getName().replace(".", "/")+"</http>\n";
	    			 payload=payload+"         <coap>coap://"+UpdateManager.getIP()+":5683/"+type.getName().replace(".", "/")+"</coap>\n";
	    			 payload=payload+"     </url>\n";
	    		 }
	    		 payload=payload+"  </contexttype>\n";
	    	}
			payload=payload+"</contexttypes>";
		}
		if(mediaType == TEXT_PLAIN_UTF8)
		{
			for(int i=0; i<types.size(); i++)
	    	{
	    		String key = (String) types.keySet().toArray()[i];
	    		ContextType type = types.get(key);
	    		payload=payload+"- "+type.getName()+"\n";
	    		payload=payload+"  "+type.getUserFriendlyName()+"\n";
	    		payload=payload+"  "+type.getDescription()+"\n";
	    	}
		}
		if(mediaType == APP_JSON)
		{
			payload="currently not supported";
		}
		
		return payload.getBytes(Charset.forName("UTF-8"));
	}
}

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

import static de.uniluebeck.itm.ncoap.message.options.OptionRegistry.MediaType.APP_XML;
import static de.uniluebeck.itm.ncoap.message.options.OptionRegistry.MediaType.TEXT_PLAIN_UTF8;

import java.net.InetSocketAddress;
import java.nio.charset.Charset;
import java.util.ArrayList;
import java.util.Date;
import java.util.HashMap;
import java.util.List;
import java.util.StringTokenizer;
import java.util.concurrent.ScheduledExecutorService;
import java.util.concurrent.TimeUnit;

import org.ambientdynamix.api.application.ContextEvent;

import android.os.Bundle;
import android.util.Log;

import com.google.common.util.concurrent.SettableFuture;

import de.uniluebeck.itm.dynamixsspbridge.dynamix.ContextType;
import de.uniluebeck.itm.dynamixsspbridge.dynamix.DynamixConnectionService;
import de.uniluebeck.itm.ncoap.application.server.webservice.MediaTypeNotSupportedException;
import de.uniluebeck.itm.ncoap.application.server.webservice.ObservableWebService;
import de.uniluebeck.itm.ncoap.message.CoapRequest;
import de.uniluebeck.itm.ncoap.message.CoapResponse;
import de.uniluebeck.itm.ncoap.message.header.Code;
import de.uniluebeck.itm.ncoap.message.options.InvalidOptionException;
import de.uniluebeck.itm.ncoap.message.options.Option;
import de.uniluebeck.itm.ncoap.message.options.ToManyOptionsException;
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
		Log.d(TAG, "new Service has been established");
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
		  try{
	            //parse new status value
	            String payload = request.getPayload().toString(Charset.forName("UTF-8"));
	            Log.d(TAG, "POST: "+payload);
	            payload=payload.replace("=", " ");
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
			            			scanConfig.putString(argumentsArray[1], argumentsArray[2]);
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
	            		response = new CoapResponse(Code.BAD_OPTION_402);
	            		response.setContentType(MediaType.TEXT_PLAIN_UTF8);
	            		responseFuture.set(response);
	            		return;
	            	}
	            }
	            
		  }
		  catch(Exception e)
		  {
			  
		  }
		  contexttype.requestConfiguredContextUpdate(scanConfig);
		  List<Option> acceptOptions = request.getOption(OptionName.ACCEPT);
		  try
			{
		        //If accept option is not set in the request, use the default (TEXT_PLAIN)
		        if(acceptOptions.isEmpty())
		        {
		        	Log.d(TAG, "accept optioon is empty");
		            response = new CoapResponse(Code.CONTENT_205);
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
		                response = new CoapResponse(Code.CONTENT_205);
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
    	Log.d(TAG, "payload from actual status");
    	ContextEvent event = getResourceStatus();
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
    			
		    	Log.d(TAG, "mediatype="+mediaType);
				if(mediaType==APP_XML)
				{
					Log.d(TAG, "formatxml");
					StringBuffer payload = new StringBuffer();
					payload.append(event.getStringRepresentation("XML"));
					return payload.toString().getBytes(Charset.forName("UTF-8"));
				}
				if(mediaType == TEXT_PLAIN_UTF8)
				{
					Log.d(TAG, "format is Plain Stuff");
					String payload =event.getStringRepresentation("text/plain"); 
					Log.d(TAG, payload);
					return payload.getBytes(Charset.forName("UTF-8"));
				}
    		}
    		else
    		{
				Log.d(TAG, "event has expired");
				contexttype.requestContextUpdate();
				String payload =event.getStringRepresentation("text/plain"); 
				payload="event has expired, but just in case you care... "+payload;
				return payload.getBytes(Charset.forName("UTF-8"));
    		}
    	}
		else
		{
			Log.d(TAG, "random old stuff");
			if(contexttype!=null)
			{
				contexttype.requestContextUpdate();
			}
			else
			{
				Log.d(TAG, "contexttype=null");
			}
			Log.d(TAG, "requested update");
			String payload =" "; 
			return payload.getBytes(Charset.forName("UTF-8"));
		}
		return null;
    }

	@Override
	public byte[] getSerializedResourceStatus(MediaType mediaType)
			throws MediaTypeNotSupportedException 
			{
		return createPayloadFromAcutualStatus(mediaType);
	}
}

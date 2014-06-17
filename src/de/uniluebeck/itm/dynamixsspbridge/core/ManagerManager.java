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

import java.io.ByteArrayInputStream;
import java.io.InputStream;
import java.io.StringWriter;
import java.io.UnsupportedEncodingException;
import java.nio.charset.Charset;
import java.util.ArrayList;
import java.util.Date;
import java.util.HashMap;
import java.util.Iterator;
import java.util.Map;
import java.util.Set;
import java.util.StringTokenizer;
import java.util.concurrent.ConcurrentHashMap;

import org.ambientdynamix.api.application.ContextEvent;

import android.app.Service;
import android.content.Context;
import android.content.Intent;
import android.os.Bundle;
import android.os.IBinder;
import android.util.Log;

import com.json.parsers.JSONParser;
import com.json.parsers.JsonParserFactory;

import de.uniluebeck.itm.coapserver.CoapServerManager;
import de.uniluebeck.itm.dynamixsspbridge.dynamix.ContextType;
import de.uniluebeck.itm.dynamixsspbridge.support.Constants;
import de.uniluebeck.itm.httpserver.HTTPServerManager;
import de.uniluebeck.itm.ncoap.message.MessageCode;
import de.uniluebeck.itm.ncoap.message.options.ContentFormat;

import com.hp.hpl.jena.rdf.model.Model;
import com.hp.hpl.jena.rdf.model.ModelFactory;
import com.hp.hpl.jena.rdf.model.Property;
import com.hp.hpl.jena.rdf.model.ResIterator;
import com.hp.hpl.jena.rdf.model.Resource;
import com.hp.hpl.jena.rdf.model.Statement;
import com.hp.hpl.jena.rdf.model.StmtIterator;
import com.hp.hpl.jena.vocabulary.RDF;

/**
 * With more then only coap being supported we actually need to have a unified interface for all the Managers and then check which
 * things are activated for what and act accordingly...
 *
 * @author lukas
 *
 */
public class ManagerManager extends Service
{

	private static String TAG ="SSPBridge";
	private static Context context=null;
	
	@Override
	public IBinder onBind(Intent arg0) {
		// TODO Auto-generated method stub
		return null;
	}
	
	@Override
	public void onCreate() 
	{
		super.onCreate();
		context=this;
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
	
	public static void startServer(final String servername)
	{
		 Log.d(TAG, "ManagerManager startServer");

                CoapServerManager.startServer(servername);
                HTTPServerManager.startServer(servername);

	}
	
	public static void stopAllServers() throws InterruptedException
	{
		 Log.e(TAG, "ManagerManager stopAllServers");
		CoapServerManager.stopAllServers();
		HTTPServerManager.stopAllServers();
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
	
	public static boolean validateRequest(Bundle xyz)
	{
		return false;
	}
	
	public static Bundle parseRequest(MessageCode.Name code, String payload, long type)
	{
		Log.d(TAG, "ManagerManager parseRequest");
		HashMap<String, String> payloadmap = new HashMap<String, String>();
		Log.d(TAG, "Code "+code);
		if(MessageCode.Name.GET==code)
		{
			Log.d(TAG, "tokenize");
			StringTokenizer tk = new StringTokenizer(payload, "=");
			Bundle scanConfig = new Bundle();
			while(tk.hasMoreElements())
			{
				String key = tk.nextToken();
				String value = tk.nextToken();
				Log.d(TAG, "key:value ->"+key+":"+value);
				payloadmap.put(key, value);

				scanConfig.putString("token", value);
			}
			return scanConfig;
		}
		if(code == MessageCode.Name.POST || code == MessageCode.Name.PUT)
		{
			Log.d(TAG, "Post or Put");
			if(type ==  ContentFormat.TEXT_PLAIN_UTF8)
			{
				Log.d(TAG, "MediaType Plain Text");
				if(payload.equals(""))
				{
					return  new Bundle();
				}
				else
				{
					return parsePlainTextRequest(payload);
				}
			}
			if(type ==  ContentFormat.APP_JSON)
			{
				Log.d(TAG, "MediaType JSON");
				if(payload.equals(""))
				{
					return  new Bundle();
				}
				else
				{
					Bundle request = parseJSONRequest(payload);
					return request;
				}
			}
		}
		return new Bundle();
	}

	private static Bundle parseJSONRequest(String payload)
	{
		Log.d(TAG, "parse JSON Request");
		Bundle scanConfig = new Bundle();
		//DO STUFF
		JsonParserFactory factory=JsonParserFactory.getInstance();
		JSONParser parser=factory.newJsonParser();
		Map jsonData=parser.parseJson(payload);
		Set keys = jsonData.keySet();
		Iterator it = keys.iterator();
		while(it.hasNext())
		{
			Object key = it.next();
			Object x=jsonData.get(key);
			if(x instanceof String)
			{
				if(key.equals("token"))
				{
					Log.d(TAG, "TOKEN"+x);
					scanConfig.putString("token", (String) x);
				}
				else
				{
					Log.d(TAG, key+" "+x);
				}
			}
			if(x instanceof HashMap)
			{
				System.out.println("Map");
				HashMap m = (HashMap) x;
				if(key.equals("boolean"))
				{
					Set kset = m.keySet();
					Iterator it2 = kset.iterator();
					while(it2.hasNext())
					{
						String kk = (String) it2.next();
						if(m.get(kk) instanceof ArrayList)
						{
							Log.d(TAG, "ARRAY");
							ArrayList<String> da = (ArrayList<String>) m.get(kk);
							boolean[] d = new boolean[da.size()];
							System.out.print(kk+" [");
							for(int i=0; i<da.size(); i++)
							{
								boolean b=false;
								if(da.get(i).equals("true"))
								{
									b=true;
								}
								Log.d(TAG, b+", ");
								d[i] = b;
							}
							System.out.println("]");
							scanConfig.putBooleanArray(kk, d);
						}
						if(m.get(kk) instanceof String)
						{
							boolean b=false;
							if(m.get(kk).equals("true"))
							{
								b=true;
							}
							Log.d(TAG, "BOOLEAN "+kk+" "+b);
							scanConfig.putBoolean(kk, b);
						}

					}
				 }
				 if(key.equals("int"))
				 {
					Set kset = m.keySet();
					Iterator it2 = kset.iterator();
					while(it2.hasNext())
					{
						String kk = (String) it2.next();
						if(m.get(kk) instanceof ArrayList)
						{
							Log.d(TAG, "ARRAY");
							ArrayList<String> da = (ArrayList<String>) m.get(kk);
							int[] d = new int[da.size()];
							System.out.print(kk+" [");
							for(int i=0; i<da.size(); i++)
							{
								Log.d(TAG, da.get(i)+", ");
								d[i] = Integer.parseInt(da.get(i));
							}
							Log.d(TAG, "]");
							scanConfig.putIntArray(kk, d);
						}
						if(m.get(kk) instanceof String)
						{
							int i = Integer.parseInt((String) m.get(kk));
							Log.d(TAG, "INT "+kk+" "+i);
							scanConfig.putInt(kk, i);
						}
					}
				 }
				 if(key.equals("long"))
				 {
					Set kset = m.keySet();
					Iterator it2 = kset.iterator();
					while(it2.hasNext())
					{
						String kk = (String) it2.next();
						if(m.get(kk) instanceof ArrayList)
						{
							Log.d(TAG, "ARRAY");
							ArrayList<String> da = (ArrayList<String>) m.get(kk);
							long[] d = new long[da.size()];
							System.out.print(kk+" [");
							for(int i=0; i<da.size(); i++)
							{
								Log.d(TAG, da.get(i)+", ");
								d[i] = Long.parseLong(da.get(i));
							}
							Log.d(TAG, "]");
							scanConfig.putLongArray(kk, d);
						}
						if(m.get(kk) instanceof String)
						{
							long l = Long.parseLong((String) m.get(kk));
							Log.d(TAG, "LONG "+kk+" "+l);
							scanConfig.putLong(kk, l);
						}
					}
				 }
				 if(key.equals("byte"))
				 {
					Set kset = m.keySet();
					Iterator it2 = kset.iterator();
					while(it2.hasNext())
					{
						String kk = (String) it2.next();
						if(m.get(kk) instanceof ArrayList)
						{
							Log.d(TAG, "ARRAY");
							ArrayList<String> da = (ArrayList<String>) m.get(kk);
							byte[] d = new byte[da.size()];
							Log.d(TAG, kk+" [");
							for(int i=0; i<da.size(); i++)
							{
								Log.d(TAG, da.get(i)+", ");
								d[i] = Byte.parseByte(da.get(i));
							}
							Log.d(TAG, "]");
							scanConfig.putByteArray(kk, d);
						}
						if(m.get(kk) instanceof String)
						{
							byte b = Byte.parseByte((String) m.get(kk));
							Log.d(TAG, "Byte "+kk+" "+b);
							scanConfig.putByte(kk, b);
						}
					}
				 }
				 if(key.equals("short"))
				 {
					Set kset = m.keySet();
					Iterator it2 = kset.iterator();
					while(it2.hasNext())
					{
						String kk = (String) it2.next();
						if(m.get(kk) instanceof ArrayList)
						{
							Log.d(TAG, "ARRAY");
							ArrayList<String> da = (ArrayList<String>) m.get(kk);
							short[] d = new short[da.size()];
							Log.d(TAG, kk+" [");
							for(int i=0; i<da.size(); i++)
							{
								Log.d(TAG, da.get(i)+", ");
								d[i] = Short.parseShort(da.get(i));
							}
							Log.d(TAG, "]");
							scanConfig.putShortArray(kk, d);
						}
						if(m.get(kk) instanceof String)
						{
							short s = Short.parseShort((String) m.get(kk));
							Log.d(TAG, "Short "+kk+" "+s);
							scanConfig.putShort(kk, s);
						}
					}
				 }
				 if(key.equals("float"))
				 {
					Set kset = m.keySet();
					Iterator it2 = kset.iterator();
					while(it2.hasNext())
					{
						String kk = (String) it2.next();
						if(m.get(kk) instanceof ArrayList)
						{
							Log.d(TAG, "ARRAY");
							ArrayList<String> da = (ArrayList<String>) m.get(kk);
							float[] d = new float[da.size()];
							Log.d(TAG, kk+" [");
							for(int i=0; i<da.size(); i++)
							{
								Log.d(TAG, da.get(i)+", ");
								d[i] = Float.parseFloat(da.get(i));
							}
							Log.d(TAG, "]");
							scanConfig.putFloatArray(kk, d);
						}
						if(m.get(kk) instanceof String)
						{
							float f = Float.parseFloat((String) m.get(kk));
							Log.d(TAG, "Float "+kk+" "+f);
							scanConfig.putFloat(kk, f);
						}
					}
				 }
				 if(key.equals("double"))
				 {
					Set kset = m.keySet();
					Iterator it2 = kset.iterator();
					while(it2.hasNext())
					{
						String kk = (String) it2.next();
						if(m.get(kk) instanceof ArrayList)
						{
							Log.d(TAG, "ARRAY");
							ArrayList<String> da = (ArrayList<String>) m.get(kk);
							double[] d = new double[da.size()];
							Log.d(TAG, kk+" [");
							for(int i=0; i<da.size(); i++)
							{
								Log.d(TAG, da.get(i)+", ");
								d[i] = Double.parseDouble(da.get(i));
							}
							Log.d(TAG, "]");
							scanConfig.putDoubleArray(kk, d);
						}
						if(m.get(kk) instanceof String)
						{
							double d = Double.parseDouble((String) m.get(kk));
							Log.d(TAG, "Double "+kk+" "+d);
							scanConfig.putDouble(kk, d);
						}
					}
				 }
				 if(key.equals("String"))
				 {
					Set kset = m.keySet();
					Iterator it2 = kset.iterator();
					while(it2.hasNext())
					{
						String kk = (String) it2.next();
						if(m.get(kk) instanceof ArrayList)
						{
							Log.d(TAG, "ARRAY");
							ArrayList<String> da = (ArrayList<String>) m.get(kk);
							String[] d = new String[da.size()];
							Log.d(TAG, kk+" [");
							for(int i=0; i<da.size(); i++)
							{
								Log.d(TAG, da.get(i)+", ");
								d[i] = da.get(i);
							}
							Log.d(TAG, "]");
							scanConfig.putStringArray(kk, d);
						}
						if(m.get(kk) instanceof String)
						{
							String d = (String) m.get(kk);
							Log.d(TAG, "String "+kk+" "+d);
							scanConfig.putString(kk, d);
						}
					}	
				 }
				 if(key.equals("char"))
				 {
					Set kset = m.keySet();
					Iterator it2 = kset.iterator();
					while(it2.hasNext())
					{
						String kk = (String) it2.next();
						if(m.get(kk) instanceof ArrayList)
						{
							System.out.println("ARRAY");
							ArrayList<String> da = (ArrayList<String>) m.get(kk);
							char[] d = new char[da.size()];
							System.out.print(kk+" [");
							for(int i=0; i<da.size(); i++)
							{
								System.out.print(da.get(i)+", ");
								d[i] = da.get(i).charAt(0);
							}
							System.out.println("]");
							scanConfig.putCharArray(kk, d);
						}
						if(m.get(kk) instanceof String)
						{
							char c = ((String) m.get(kk)).charAt(0);
							System.out.println("Char "+kk+" "+c);
							scanConfig.putChar(kk, c);
						}
					}
				 }
			 }
		}		 
		return scanConfig;
	}
	
	
	private static Bundle parsePlainTextRequest(String payload)
	{
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
	
	public static String createStringPayloadFromAcutualStatus(long mediaType, ContextEvent event, ContextType contexttype)
	{
		Log.d(TAG, "payload from actual status");
		Log.d(TAG, "Mediatype: "+mediaType);
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
        		Set<String> formats = event.getStringRepresentationFormats();
        		if(mediaType==ContentFormat.TEXT_PLAIN_UTF8)
        		{
        			//TODO: This is evil. And Wroooong. And Evil. And Evil.
        			mediaType= ContentFormat.APP_XML;
        		}
        			
		    	Log.d(TAG, "mediatype="+mediaType);				
				if(mediaType==ContentFormat.APP_XML)
				{
					Log.d(TAG, "formatxml");
					if(formats.contains("RDF/XML"))
					{
						Log.d(TAG, "format contains rdf/XML");
						Model model = getSemanticInfo(event);
						if(model!=null)
						{
							Log.d(TAG, "Model is not null");
							String syntax = "RDF/XML"; // also try "N-TRIPLE" and "TURTLE"
							Log.d(TAG, "step 6b");
							StringWriter out = new StringWriter();
							Log.d(TAG, "step 6c");
							String result="";
							Log.d(TAG, "step 6d");
							
							model.write(out, syntax);
							Log.d(TAG, "step 6e");
							result = out.toString();
							Log.d(TAG, "step 6f");
							Log.d(TAG, result);
							Log.d(TAG, "step 6g");
							return result;
						}
						else
						{
							return "null";
						}
					}
					else
					{
                        Log.d(TAG, "else...");
						if(true)
						{
							Log.d(TAG, "formats contained xml...");
							//TODO: here be some wrapping
							String xmlresult =  "<contextEvent>\n" +
												"	<contextType>\n" +
												"		<id>" +event.getContextType() + "</id>\n"+
												"		<createdAt>"+event.getTimeStamp().getTime()+"</createdAt>\n"+
												"		<expires>"+event.expires()+"</expires>\n"+
												"		<expiresAt>"+event.getExpireTime().getTime()+"</expiresAt>\n"+
												"		<source>\n"+
												"			<plugin>\n"+
												"				<pluginId>"+event.getEventSource().getPluginId()+"</pluginId>\n"+
												"				<pluginName>"+event.getEventSource().getPluginName()+"</pluginName>\n"+
												"			</plugin>\n"+
															//TODO here should be stuff on source in terms of hardware and such things... but this requires the app to have the jar types...
												"		</source>\n"+
												"	</contextType>\n" +
												"	<contextData>\n";
                            Log.d(TAG, "-----");
                            Log.d(TAG, ""+event.getStringRepresentationFormats().size());
                            for(String f : event.getStringRepresentationFormats())
                            {
                                Log.d(TAG, f);
                            }
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
                            Log.d(TAG, xmlresult);
							return xmlresult;
						}
					}

				}
				if(mediaType == ContentFormat.TEXT_PLAIN_UTF8)
				{
					Log.d(TAG, "format is Plain Stuff");
					if(formats.contains("text/plain"))
					{
						String payload =event.getStringRepresentation("text/plain"); 
						Log.d(TAG, payload);
						return payload.toString();
					}
				}
				if(mediaType == ContentFormat.APP_JSON)
				{
					
					Log.d(TAG, "format is JSON ");
					if(formats.contains("RDF/XML"))
					{
						Log.d(TAG, "format contains rdf/xml");
						Model model = getSemanticInfo(event);

						String syntax = "RDF/JSON"; // also try "N-TRIPLE" and "TURTLE"
						Log.d(TAG, "step 6b");
						StringWriter out = new StringWriter();
						Log.d(TAG, "step 6c");
						String result="";
						Log.d(TAG, "step 6d");
						
						model.write(out, syntax);
						Log.d(TAG, "step 6e");
						result = out.toString();
						Log.d(TAG, "step 6f");
						Log.d(TAG, result);
						Log.d(TAG, "step 6g");
						return result;
					}
					else
					{
						if(formats.contains("JSON"))
						{
							
						}
						else
						{
							
						}
					}
				}
				if(mediaType == ContentFormat.APP_N3)
				{
					if(formats.contains("N3"))
					{
						
					}
				}
				if(mediaType== ContentFormat.APP_TURTLE)
				{
					if(formats.contains("Turtle"))
					{
						
					}
				}
				if(mediaType == ContentFormat.APP_SHDT)
				{
					if(formats.contains("SHDT"))
					{
						
					}
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
	
	private static Model getSemanticInfo(ContextEvent event)
	{
		Log.d(TAG, "get semantic representation");
		String payload =event.getStringRepresentation("RDF/XML"); 
		if(payload!=null)
		{
			Log.d(TAG, "payload\n"+payload);
			payload=payload.replace("j.0", "xx.0");
			payload=payload.replace("j.1", "xx.1");
			payload=payload.replace("j.2", "xx.2");
			Log.d(TAG, "step0");
			Model model = ModelFactory.createDefaultModel();
			Log.d(TAG, "step0b");
			String syntax = "RDF/XML"; // also try "N-TRIPLE" and "TURTLE"
			Log.d(TAG, "step0c");
			StringWriter out = new StringWriter();
			Log.d(TAG, "step0d");
			String result ="";
			Log.d(TAG, "step1");
			
			Resource res_contextEvent = model.createResource("http://dynamix.org/semmodel/0.1/ContextEvent/"+event.getContextType()+"_"+event.getTimeStamp().getTime()+"");
			Resource res_sensor = model.createResource("http://dynamix.org/semmodel/0.1/DynamixPlugin/"+event.getEventSource().getPluginId()+"");
			Log.d(TAG, "step2");
			//create the Properties
			Property prop_typeID= model.createProperty("http://dynamix.org/semmodel/0.1/", "hasContextType");
			Property prop_createdAt = model.createProperty("http://dynamix.org/semmodel/0.1/", "CreatedAt");
			Property prop_expiration = model.createProperty("http://dynamix.org/semmodel/0.1/", "Expired");
			Property prop_expirationAt = model.createProperty("http://dynamix.org/semmodel/0.1/", "ExpiresAt");
			Property prop_hasSource = model.createProperty("http://dynamix.org/semmodel/0.1/", "createdByPlugin");
			Property prop_hasData = model.createProperty("http://dynamix.org/semmodel/0.1/", "hasData");
			Property prop_hasDevice = model.createProperty("http://dynamix.org/semmodel/0.1/", "hasDevice");
			Log.d(TAG, "step3");
								
			Property prop_pluginID= model.createProperty("http://dynamix.org/semmodel/0.1/", "hasID");
			Property prop_pluginName= model.createProperty("http://dynamix.org/semmodel/0.1/", "hasName");
			
			res_contextEvent.addProperty(prop_typeID, event.getContextType());
			
			res_contextEvent.addProperty(prop_createdAt, ""+event.getTimeStamp().getTime());
			Date d = new Date();
			long t = d.getTime();
			if(t>event.getExpireTime().getTime())
			{
				res_contextEvent.addProperty(prop_expiration, "true");
			}
			else
			{
				res_contextEvent.addProperty(prop_expiration, "false");
			}
			res_contextEvent.addProperty(prop_expirationAt, ""+event.getExpireTime().getTime());
			res_contextEvent.addProperty(RDF.type, "http://dynamix.org/semmodel/0.1/ContextEvent");
			res_contextEvent.addProperty(prop_hasSource, res_sensor);
			
			//String deviceid=Secure.getString(context.getContentResolver(), Secure.ANDROID_ID); 
			res_sensor.addProperty(prop_pluginID, event.getEventSource().getPluginId());
			res_sensor.addProperty(RDF.type, "http://dynamix.org/semmodel/0.1/DynamixPlugin");
			res_sensor.addProperty(prop_pluginName, event.getEventSource().getPluginName());
			//res_sensor.addProperty(prop_hasDevice , ""+deviceid);
			
			Model model_2 = ModelFactory.createDefaultModel();
			Log.d(TAG, "step4");
			InputStream stream;
			try 
			{
				stream = new ByteArrayInputStream(payload.getBytes("UTF-8"));
				Log.d(TAG, "step4a");
				model_2.read(stream, null);
				Log.d(TAG, "step4b");
				out = new StringWriter();
				Log.d(TAG, "step4c");
				result="";
				Log.d(TAG, "step4d");
				model_2.write(out, syntax);
				Log.d(TAG, "step4e");
				result = out.toString();
				Log.d(TAG, "step4f");
			} 
			catch (UnsupportedEncodingException e) 
			{
				// TODO Auto-generated catch block
				e.printStackTrace();
				Log.e(TAG, "unspoorted coding exeption");
			}
			catch(Exception ex)
			{
				return null;
			}
			Log.d(TAG, "step 5");
			//System.out.println("");
			//System.out.println("");
			
			ResIterator resiter = model_2.listSubjects();
			while(resiter.hasNext())
			{
				Log.d(TAG, "x");
				Resource res = resiter.next();
				res_contextEvent.addProperty(prop_hasData, res);
				StmtIterator stmtIterator = res.listProperties();
				model.add(stmtIterator);
				while(stmtIterator.hasNext())
				{
					Statement stmt = stmtIterator.next();
	
				}
				
			}
			Log.d(TAG, "step 6 ");
			return model;
		}
		return null;
	}
	
	public static byte[] createPayloadFromAcutualStatus(long mediaType, ContextType contextType, boolean compress)
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
            Log.d(TAG, "got String payload from actual status, now create a byte array");
			return payload.getBytes(Charset.forName("UTF-8"));
		}
	}
	
	public static byte[] createContextTypeListResponse(String format)
	{
		long mediaType = ContentFormat.APP_XML;
		if(format!=null)
		{
			return createContextTypeListResponse(mediaType);			
		}
		else
		{
			return createContextTypeListResponse(mediaType);
		}
	}
	
	public static byte[] createContextTypeListResponse(long mediaType)
	{
		String payload = new String();
		HashMap<String, ContextType> types = UpdateManager.getContextTypes();
		if(mediaType==ContentFormat.APP_XML)
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
	    			 payload=payload+"         <http>http://"+UpdateManager.getIP()+":"+Constants.HTTP_PORT+"/"+type.getName().replace(".", "/")+"</http>\n";
	    			 payload=payload+"         <coap>coap://"+UpdateManager.getIP()+":"+Constants.COAP_PORT+"/"+type.getName().replace(".", "/")+"</coap>\n";
	    			 payload=payload+"     </url>\n";
	    		 }
	    		 payload=payload+"  </contexttype>\n";
	    	}
			payload=payload+"</contexttypes>";
		}
		if(mediaType == ContentFormat.TEXT_PLAIN_UTF8)
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
		if(mediaType == ContentFormat.APP_JSON)
		{
			payload="currently not supported";
		}
		
		return payload.getBytes(Charset.forName("UTF-8"));
	}
}

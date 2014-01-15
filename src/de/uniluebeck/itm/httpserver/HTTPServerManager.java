
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

import java.util.HashMap;
import java.util.concurrent.ConcurrentHashMap;

import org.jboss.netty.handler.codec.http.HttpMethod;

import com.strategicgains.restexpress.Format;
import com.strategicgains.restexpress.RestExpress;

import de.uniluebeck.itm.dynamixsspbridge.core.UpdateManager;
import de.uniluebeck.itm.dynamixsspbridge.dynamix.ContextType;
import de.uniluebeck.itm.dynamixsspbridge.support.Constants;

import android.app.Service;
import android.content.Intent;
import android.os.IBinder;
import android.util.Log;

public class HTTPServerManager extends Service
{

	private static String TAG ="SSPBridge";
	
	private static HashMap<String, RestExpress> serverList = new HashMap<String, RestExpress>();
	
	@Override
	public IBinder onBind(Intent arg0) {
		// TODO Auto-generated method stub
		return null;
	}
	
	@Override
	public void onCreate() 
	{
		super.onCreate();
		 Log.e(TAG, "HTTP bla1b");
	}
	
	@Override
	public int onStartCommand(Intent intent, int flags, int startId) 
	{
		super.onStartCommand(intent, flags, startId);
		Log.i(TAG, "HTTPmanager: bla3");
		return START_STICKY;
	}
	
	@Override
	public void onDestroy() 
	{
		stopAllServers();
		super.onDestroy();	
	}
	
	public static void startServer(String servername)
	{
		startServer(servername, Constants.HTTP_PORT);
	}
	
	public static void startServer(final String servername, final int port)
	{
		Log.d(TAG, "Start HTTP server "+servername);
		try
		{
			Thread tt = new Thread(new Runnable()
			{
				@Override
				public void run()
				{
					UpdateManager.updateList();
					ConcurrentHashMap<String, ContextType> types = UpdateManager.getContextTypes();
					while(types.size()<1)//needed for the workaround... otherwise the server starts before any context types are known. Even this only kind of works.
					{
						types = UpdateManager.getContextTypes();
					}
					RestExpress s = new RestExpress()
					.setName(servername)
				    .setPort(port)
				    .setDefaultFormat(Format.XML)
				    .putResponseProcessor(Format.JSON, ResponseProcessors.json())
				    .putResponseProcessor(Format.XML, ResponseProcessors.xml());
					serverList.put(servername, s);
					s.uri("/service/dynamix/contexttypes", new ObservationControler())
					.action("read", HttpMethod.GET)
					.action("create", HttpMethod.PUT)
					.defaultFormat(Format.XML)
					.noSerialization();
					//TODO: this is a capitulation on the limits of the implementation of RestExpress... shit man.
					types = UpdateManager.getContextTypes();
					Log.d(TAG, "HTTP "+types.size()+" go.");
					for(int j=0; j<types.size(); j++)
			    	{
			    		String key = (String) types.keySet().toArray()[j];
			    		ContextType type = types.get(key);
			    		if(true)
			    		{
			    			Log.d(TAG, "HTTP for "+type.getName());
			    			s.uri(type.getName().replace(".", "/"), new HTTPDynamixControler(type, type.getUpdateIntervall()))
			    			.action("read", HttpMethod.GET)
			    			.action("update", HttpMethod.POST)
			    			.defaultFormat(Format.XML)
			    			.noSerialization();
			    			if(!type.getName().endsWith(".man"))
			    			{
			    				s.uri("/"+type.getManType().getName().replace(".", "/")+"/man", new HTTPDynamixControler(type.getManType(), type.getUpdateIntervall()))
			    				.action("read", HttpMethod.GET)
			    				.action("create", HttpMethod.PUT)
			    				.defaultFormat(Format.XML)
			    				.noSerialization();
			    			}
			    		}
			    	}
					try
					{
						s.bind();
					}
					catch(Exception e)
					{
						Log.e(TAG, "HTTP Bind failed.");
					}
					s.awaitShutdown();
					Log.d(TAG, "HTTP Server Base URL= "+s.getBaseUrl());
					Log.d(TAG, "HTTP Server Port= "+s.getPort());
				}
			});
			tt.start(); 
			
		}
		catch(Exception e)
		{
			Log.e(TAG, "Some HTTP handling seems to be missing.");
			//startServer(servername, port++);
		}
	}
	
	public static void stopAllServers()
	{
		Log.d(TAG, "stop all HTTP servers");
		for(int i=0; i<serverList.size(); i++)
		{
			RestExpress s = serverList.get((String) (serverList.keySet().toArray()[i]));
			s.shutdown();
		}
	}
	
	public static void stopServer(String serverName)
	{
		RestExpress s = serverList.get(serverName);
		if(s!=null)
		{
			s.shutdown();
			serverList.remove(serverName);
		}
	}
	
	public static void addService(final ContextType contexttype, final int updateintervall)
	{
		Log.d(TAG, "add Service");
		Log.d(TAG, "serverList size="+serverList.size());
		Thread tt = new Thread(new Runnable()
		{
			@Override
			public void run()
			{
				/**int port = 8081;
				for(int i=0; i<serverList.size(); i++)
				{
					RestExpress s = serverList.get((String) (serverList.keySet().toArray()[i]));
					port = s.getPort();
					Log.d(TAG, "1");
					//s.setName(s.getName());					
					Log.d(TAG, "1b");
					s.shutdown();
					//s.shutdown(); //this sometimes blocks the entire progress
					Log.d(TAG, "2");
				}**/
				/**for(int i=0; i<serverList.size(); i++)
				{
					Log.d(TAG, "3");
					RestExpress s = serverList.get((String) (serverList.keySet().toArray()[i]));
					Log.d(TAG, "4");
					String servername = s.getName();
					Log.d(TAG, "5");
					
					Log.d(TAG, "6");
					Log.d(TAG, "restarting "+servername+" at port "+port);
					s = new RestExpress()
					.setName(servername)
				    .setPort(port)
				    .setDefaultFormat(Format.XML)
				    .putResponseProcessor(Format.JSON, ResponseProcessors.json())
				    .putResponseProcessor(Format.XML, ResponseProcessors.xml());
					Log.d(TAG, "7");
					serverList.put(servername, s);
					s.uri("/contexttypes", new ObservationControler())
					.action("read", HttpMethod.GET)
					.action("create", HttpMethod.PUT)
					.defaultFormat(Format.XML)
					.noSerialization();
					ConcurrentHashMap<String, ContextType> types = UpdateManager.getContextTypes();
					for(int j=0; j<types.size(); j++)
			    	{
			    		String key = (String) types.keySet().toArray()[j];
			    		ContextType type = types.get(key);
			    		if(type.contextSupported())
			    		{
			    			Log.d(TAG, "HTTP for "+type.getName());
			    			s.uri(type.getName().replace(".", "/"), new HTTPDynamixControler(type, type.getUpdateIntervall()))
			    			.action("read", HttpMethod.GET)
			    			.action("update", HttpMethod.POST)
			    			.defaultFormat(Format.XML)
			    			.noSerialization();
			    			if(!type.getName().endsWith(".man"))
			    			{
			    				s.uri("/"+type.getManType().getName().replace(".", "/")+"/man", new HTTPDynamixControler(type.getManType(), type.getUpdateIntervall()))
			    				.action("read", HttpMethod.GET)
			    				.action("create", HttpMethod.PUT)
			    				.defaultFormat(Format.XML)
			    				.noSerialization();
			    			}
			    		}
			    	}
					try
					{
						s.bind();
					}
					catch(Exception e)
					{
						Log.e(TAG, "HTTP Bind failed.");
					}
					s.awaitShutdown();
				}**/
				contexttype.activate(Constants.HTTP_PORT, updateintervall);
			}
		});
		tt.start(); 
	}
	
	public static void removeService(ContextType contexttype)
	{
		//RestExpress s = serverList.get((String) (serverList.keySet().toArray()[0]));
		//s.uri("/"+contexttype.getName().replace(".", "/"), null);
	}
}

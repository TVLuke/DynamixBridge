
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

package de.uniluebeck.itm.htmlserver;

import java.util.HashMap;
import java.util.Iterator;
import java.util.Set;
import java.util.concurrent.ConcurrentHashMap;

import org.jboss.netty.handler.codec.http.HttpMethod;

import com.strategicgains.restexpress.Format;
import com.strategicgains.restexpress.Parameters;
import com.strategicgains.restexpress.RestExpress;


import de.uniluebeck.itm.coapserver.DynamixCoapServer;
import de.uniluebeck.itm.coapserver.NotObservableOverviewWebService;
import de.uniluebeck.itm.dynamixsspbridge.dynamix.ContextType;
import de.uniluebeck.itm.ncoap.application.server.webservice.WebService;

import android.app.Service;
import android.content.Intent;
import android.os.IBinder;
import android.util.Log;

public class HTMLServerManager extends Service
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
		 Log.e(TAG, "HTML bla1b");
	}
	
	@Override
	public int onStartCommand(Intent intent, int flags, int startId) 
	{
		super.onStartCommand(intent, flags, startId);
		Log.i(TAG, "HTML manager: bla3");
		startServer("s1");
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
		startServer(servername, 8081);
	}
	
	public static void startServer(final String servername, final int port)
	{
		Log.d(TAG, "Start HTML server "+servername);
		try
		{
			Thread tt = new Thread(new Runnable()
			{
				@Override
				public void run()
				{
					RestExpress s = new RestExpress()
					.setName(servername)
				    .setPort(port)
				    .setDefaultFormat(Format.XML)
				    .putResponseProcessor(Format.JSON, ResponseProcessors.json())
				    .putResponseProcessor(Format.XML, ResponseProcessors.xml());
					s.uri("/contexttypes", new ObservationControler())
					.action("read", HttpMethod.GET)
					.action("create", HttpMethod.PUT)
					.defaultFormat(Format.XML)
					.noSerialization();
					serverList.put(servername, s);
					try
					{
					s.bind();
					}
					catch(Exception e)
					{
						Log.e(TAG, "HTML Bind failed.");
					}
					s.awaitShutdown();
					Log.d(TAG, "HTML Server Base URL= "+s.getBaseUrl());
					Log.d(TAG, "HTML Server Port= "+s.getPort());
				}
			});
			tt.start(); 
			
		}
		catch(Exception e)
		{
			Log.e(TAG, "Some HTML handling seems to be missing.");
			//startServer(servername, port++);
		}
	}
	
	public static void stopAllServers()
	{
		Log.d(TAG, "stop all HTML servers");
		for(int i=0; i<serverList.size(); i++)
		{
			RestExpress s = serverList.get((String) (serverList.keySet().toArray()[i]));
			s.shutdown();
		}
	}
	
	public static void stopServer(String serverName)
	{
		
	}
	
	public static void addService(ContextType contexttype, int updateintervall)
	{
		
	}
	
	public static void removeService(ContextType contexttype)
	{
		
	}
}

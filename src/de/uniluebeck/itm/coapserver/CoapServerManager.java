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


import java.util.HashMap;
import java.util.Iterator;
import java.util.Set;
import java.util.concurrent.ConcurrentHashMap;


import de.uniluebeck.itm.dynamixsspbridge.dynamix.ContextType;
import de.uniluebeck.itm.dynamixsspbridge.ui.ContextItemView;
import de.uniluebeck.itm.ncoap.application.server.webservice.WebService;
import android.app.Service;
import android.content.Intent;
import android.os.IBinder;
import android.util.Log;

public class CoapServerManager extends Service
{
	private static String TAG ="SSPBridge";
	
	private static HashMap<String, DynamixCoapServer> serverList = new HashMap<String, DynamixCoapServer>();
	
	@Override
	public IBinder onBind(Intent arg0) {
		// TODO Auto-generated method stub
		return null;
	}
	
	@Override
	public void onCreate() 
	{
		super.onCreate();
		 Log.e(TAG, "bla");
	}
	
	@Override
	public int onStartCommand(Intent intent, int flags, int startId) 
	{
		super.onStartCommand(intent, flags, startId);
		Log.i(TAG, "coap manager: bla2");
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
		startServer(servername, 5683);
	}
	
	public static void startServer(String servername, int port)
	{
		Log.d(TAG, "Start server "+servername);
		try
		{
			DynamixCoapServer server = new DynamixCoapServer();
			Log.d(TAG, "Server started and listening on port " + server.getServerPort());
			server.registerService(new NotObservableOverviewWebService("/service/dynamix/contexttypes"," "));
			serverList.put("s1", server);
		}
		catch(Exception e)
		{
			Log.e(TAG, "Some handling seems to be missing.");
			//startServer(servername, port++);
		}
	}
	
	public static void stopAllServers()
	{
		Log.d(TAG, "stop all servers");
		try 
		{
			for(int i=0; i<serverList.size(); i++)
			{
				DynamixCoapServer s = serverList.get((String) (serverList.keySet().toArray()[i]));
				ConcurrentHashMap<String, WebService> services = s.getRegisteredServices();
				Set<String> keys = services.keySet();
				Iterator<String> it = keys.iterator();
				while(it.hasNext())
				{
					String key = it.next();
					WebService serv = services.get(key);
					serv.shutdown();
				}
				s.shutdown();
				serverList.remove((String) (serverList.keySet().toArray()[i]));
			}
		} 
		catch (InterruptedException e) 
		{
			Log.e(TAG, "This is a strange error");
			e.printStackTrace();
		}
	}
	
	public static void stopServer(String serverName)
	{
		Log.d(TAG, "Stop Server "+serverName);
		DynamixCoapServer s = serverList.get(serverName);
		ConcurrentHashMap<String, WebService> services = s.getRegisteredServices();
		Set<String> keys = services.keySet();
		Iterator<String> it = keys.iterator();
		while(it.hasNext())
		{
			String key = it.next();
			WebService serv = services.get(key);
			serv.shutdown();
		}
		if(s!=null)
		{
			try 
			{
				s.shutdown();
				serverList.remove(serverName);
			} 
			catch (InterruptedException e) 
			{
				// TODO Auto-generated catch block
				e.printStackTrace();
			}
		}
	}

	public static void addService(ContextType contexttype, int updateintervall)
	{
		Log.d(TAG, "add Service for "+contexttype.getName());
		DynamixCoapServer s = serverList.get((String) (serverList.keySet().toArray()[0]));
		if(s!=null)
		{
			Log.d(TAG, "server is not null.");
			if(contexttype!=null && updateintervall>0)
			{
				Log.d(TAG, "The values given are cool.");
				try
				{
					s.registerService(new ObservableDynamixWebservice(contexttype, updateintervall));
				}
				catch(Exception e)
				{
					Log.e(TAG, "was get da bitte?");
				}
				Log.d(TAG, "next line");
				contexttype.activate(s.getServerPort(), updateintervall);
				Log.d(TAG, "next line2");
				if(!contexttype.getName().endsWith(".man"))
				{
					Log.d(TAG, "in the if");
					s.registerService(new NotObservableDynamixWebservice(contexttype.getManType()));
					Log.d(TAG, "still");
					contexttype.getManType().activate(s.getServerPort(), updateintervall);
				}
				Log.d(TAG, "whatup");
			Log.d(TAG, "done");
			}
		}
		else
		{
			Log.e(TAG, "for some strange reason the server has not been found...");
		}
	}
	
	public static void removeService(ContextType contexttype)
	{
		if(serverList.size()>0)
		{
			DynamixCoapServer s = serverList.get((String) (serverList.keySet().toArray()[0]));
			contexttype.deactivate();
			contexttype.getManType().deactivate();
			s.removeService("/"+contexttype.getName().replace(".", "/"));
			s.removeService("/"+contexttype.getName().replace(".", "/")+"/man");
		}
	}
	
	
}

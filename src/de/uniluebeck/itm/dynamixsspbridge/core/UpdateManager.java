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

import java.io.IOException;
import java.util.Calendar;
import java.util.HashMap;
import java.util.Iterator;
import java.util.List;
import java.util.Set;
import java.util.concurrent.ConcurrentHashMap;

import org.ambientdynamix.api.application.ContextEvent;
import org.jdom.Document;
import org.jdom.Element;
import org.jdom.JDOMException;
import org.jdom.input.SAXBuilder;

import de.uniluebeck.itm.coapserver.CoapServerManager;
import de.uniluebeck.itm.coapserver.Utils;
import de.uniluebeck.itm.dynamixbridge.discovery.DiscoveryService;
import de.uniluebeck.itm.dynamixsspbridge.dynamix.ContextType;
import de.uniluebeck.itm.dynamixsspbridge.dynamix.DynamixConnectionService;
import de.uniluebeck.itm.dynamixsspbridge.support.Constants;
import android.app.AlarmManager;
import android.app.IntentService;
import android.app.PendingIntent;
import android.app.Service;
import android.content.Context;
import android.content.Intent;
import android.content.SharedPreferences;
import android.util.Log;

public class UpdateManager extends IntentService
{

	private static String TAG ="SSPBridge";
	private static final String PREFS = Constants.PREFS;
	Intent intent;
	private static String localIPv4="";
	private static String localIPv6="";
	private static boolean useIPv4=true;
	private static boolean run=true;
	private static int countdowntorestart=1;
	private static int littlecounter = 0;
	
	private static ConcurrentHashMap<String, ContextType> contexttypes = new ConcurrentHashMap<String, ContextType>();
	
	public UpdateManager() 
	{
		super("UpdateManager");
		//Log.i(TAG, "updatemanager: Constructor");
	}



	@Override
	public void onCreate() 
	{
		super.onCreate();
		//Log.i(TAG, "updatemanager: on create");
	}
		
	@Override
	public int onStartCommand(Intent intent, int flags, int startId) 
	{
		super.onStartCommand(intent, flags, startId);
		//Log.i(TAG, "updatemanager on start command");
		//Log.i(TAG, "Received start id " + startId + ": " + intent);
		return Service.START_NOT_STICKY;
	}
	
	@Override
	protected void onHandleIntent(Intent intent) 
	{
		Log.i(TAG, "updatemanager handle");
		//get the needed lists and variables in
		SharedPreferences prefs = getSharedPreferences(PREFS, 0);
		contexttypes = DynamixConnectionService.updateContextTypes();
		//Log.i(TAG, "updatemanager handle2");
		//Update the IP and find if it has changed
		if(localIPv4.equals("") || localIPv6.equals("")) //FIRST START
		{
			//Log.i(TAG, "getIPs");
			localIPv4=Utils.getIPAddress(true);
			localIPv6=Utils.getIPAddress(false);
			countdowntorestart=-1;
		}
		else
		{
			if(localIPv4.equals(Utils.getIPAddress(true)))
			{
				//Log.i(TAG, "we are in a old network");
			}
			else
			{
				//Log.i(TAG, "we are in a new network");
				//we got into a new network
				//deactivate all context types
				Set<String> keyset = contexttypes.keySet();
				Iterator<String> it = keyset.iterator();
				while(it.hasNext())
				{
					String key = it.next();
					ContextType ct = contexttypes.get(key);
					try
					{
						ct.deactivate();
					}
					catch(Exception e)
					{
						
					}
				}
				//stop all servers
				ManagerManager.stopAllServers();
				countdowntorestart--;
				//we don't restart servers at once, they need a bit of time to shut down
				
			}
			localIPv4=Utils.getIPAddress(true);
			localIPv6=Utils.getIPAddress(false);
		}
		if(!localIPv4.equals("") || !localIPv6.equals(""))
		{
			boolean useIPv4= prefs.getBoolean("CoapUseIPv4", true);
			//Log.i(TAG, "updatemanager handle3");
			//TODO: find if we are in a nated lan and if so, take the appropiate steps to get an address
			//restart server if it has been shut down and the countdown has ended
			if(countdowntorestart<1)
			{
				//Log.i(TAG, "coundown to restart: "+countdowntorestart);
				countdowntorestart--;
				if(countdowntorestart<0)
				{
					//Log.i(TAG, "restart");
					countdowntorestart=1;
					//restart all servers (which right now is just one)
					ManagerManager.startServer("Dynamix");
					startService(new Intent(this, DiscoveryService.class));
				}
			}
			//check if we have any outstanding subscriptions. This can be seen by the fact, that  the shared preferences say, the type has been activated but
			//the context type does not support that.
			if(DynamixConnectionService.isConnected())
			{
				//Log.i(TAG, "dynamix is connected");
				//OK, we seem to have a connected Dynmix
				Set<String> keyset = contexttypes.keySet();
				Iterator<String> it = keyset.iterator();
				while(it.hasNext())
				{
					String key = it.next();
					boolean shouldbeactive= prefs.getBoolean(key, false);
					ContextType ct = contexttypes.get(key);
					if(shouldbeactive)
					{
						if(!ct.isWaitingForSubscription() && !ct.active() && !ct.contextSupported())
						{
							ct.subscribe();
						}
						if(ct.contextSupported() && !ct.active() && ct!=null)
						{
							//Log.i(TAG, ct.getName()+" totally should start." + " However it is already subscribed, which is a problem.");
							ct.unsubscribe();
						}
						
					}
					if(ct.active())
					{
						ct.requestContextUpdateIfNeeded();
					}
				}
	
				
			}
			else
			{
				Log.i(TAG, "dynamix is not connected");
			}
			new Thread(new Runnable() 
	        {
	            public void run() 
	            {
	        		//TODO: Update descriptions of context and stuff...
	            	Iterator<String> itx = contexttypes.keySet().iterator();
	            	while(itx.hasNext())
	            	{
	            		ContextType ct = contexttypes.get(itx.next());
	            		if(ct.getDescription().equals("") || littlecounter==60)
	            		{
	            			//now, download the description and probably some other stuff from the thing
	            			updateDetail(ct);
	            		}
	            		littlecounter++;
	            	}
	            }
	        }).start();
		}
		else
		{
			Log.e(TAG, "nope, IP is still not found.");
		}
		scheduleNext();
	}
	
	@Override
	public void onDestroy()
	{
		super.onDestroy();
		//Log.i(TAG, "updatemanager: on destroy");
		if(!run)
		{
			run=true;
		}
	}
	
	private void scheduleNext()
	{
		if(run)
		{
			//Log.d(TAG, "updatemanager: sNext");
	        Calendar cal = Calendar.getInstance();
        	cal.add(Calendar.MINUTE, 1);
	        intent = new Intent(this, UpdateManager.class);
	        //Log.d(TAG, "snext2");
	        PendingIntent pendingIntent = PendingIntent.getService(this, 792468, intent, 0);
	        //Log.d(TAG, "snext3");
	        AlarmManager am = (AlarmManager)getSystemService(Context.ALARM_SERVICE);
	        //Log.d(TAG, "snext4");
	        am.set(AlarmManager.RTC_WAKEUP, cal.getTimeInMillis(), pendingIntent);
	        //Log.d(TAG, "snext5");
		}
		else
		{
			Log.d(TAG, "updatemanager: no next");
			run=true;
		}
	}
	
	public static ConcurrentHashMap<String, ContextType> getContextTypes()
	{
		return contexttypes;
	}
	
	public static void updateList()
	{
		contexttypes = DynamixConnectionService.updateContextTypes();
	}
	
	public static String getIP()
	{
		if(useIPv4)
		{
			return localIPv4;
		}
		else
		{
			return localIPv6;
		}
	}
	
	public static ContextType getTypeById(String contextTypeId) throws NullPointerException
	{
		ContextType ct = contexttypes.get(contextTypeId);
		if(ct!=null)
		{
		return ct;	
		}
		else
		{
			NullPointerException e = new NullPointerException();
			throw e;
		}
	}
	
	public static void deactivate()
	{
		run=false;
	}
	
	public static String[] getUsedPorts()
	{
		return null;
	}
	
	private void updateDetail(ContextType ct)
	{
		//Log.d(TAG, ct.getName());
		final SAXBuilder builder = new SAXBuilder();
		try 
		{
			Document doc = builder.build("https://raw.github.com/TVLuke/DynamixRepository/master/context.xml");
			Element root = doc.getRootElement();
			List<Element> children = root.getChildren();
			Iterator<Element> childrenIterator = children.iterator();
			while(childrenIterator.hasNext())
            {
				Element child = childrenIterator.next(); 
                //Log.d(TAG, ""+child.getName());
                List<Element> grandchildren = child.getChildren();
                Iterator<Element> grandchildrenIterator = grandchildren.iterator();
                boolean gotit=false;
                while(grandchildrenIterator.hasNext())
                {
                	Element grandchild = grandchildrenIterator.next();
                	if(grandchild.getName().equals("id") && grandchild.getText().equals(ct.getName()))
                	{
                		gotit=true;
                		//Log.d(TAG, "x");
                	}
                	if(gotit)
                	{
                		//Log.d(TAG, grandchild.getName());
                		if(grandchild.getName().equals("name"))
                		{
                			ct.setUserFriendlyName(grandchild.getText());
                		}
                		if(grandchild.getName().equals("shortDescription"))
                		{
                			ct.setShortDescription(grandchild.getText());
                		}
                		if(grandchild.getName().equals("description"))
                		{
                			
                		}
                		if(grandchild.getName().equals("categories"))
                		{
                			
                		}
                		if(grandchild.getName().equals("web"))
                		{
                			
                		}
                		if(grandchild.getName().equals("logo"))
                		{
                			
                		}
                		if(grandchild.getName().equals("redirect"))
                		{
                			ct.setDeprecated(true);
                			ct.setRedirectID(grandchild.getText());
                		}
                	}
                }
            	gotit=false;
            }
		} 
		catch (JDOMException e) 
		{
			// TODO Auto-generated catch block
			//Log.e(TAG, e.getMessage());
			//e.printStackTrace();
		} 
		catch (IOException e) 
		{
			// TODO Auto-generated catch block
			//Log.e(TAG, e.getMessage());
			//e.printStackTrace();
		}
	}
	
}

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

import de.uniluebeck.itm.coapserver.CoapServerManager;
import de.uniluebeck.itm.dynamixbridge.networking.DiscoveryService;
import de.uniluebeck.itm.dynamixbridge.ssp.DynamixBridgeCoapClient;
import de.uniluebeck.itm.dynamixsspbridge.dynamix.DynamixConnectionService;
import de.uniluebeck.itm.dynamixsspbridge.support.NotificationService;
import de.uniluebeck.itm.httpserver.HTTPServerManager;
import android.app.Service;
import android.content.Intent;
import android.os.IBinder;
import android.util.Log;

/**
 * This Service handles the start, it initializes the needed 
 * @author lukas
 *
 */
public class StartService extends Service 
{

	private static String TAG ="SSPBridge";
	private static boolean running=false;
	
	@Override
	public IBinder onBind(Intent arg0) 
	{
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
	public void onDestroy()
	{
		super.onDestroy();
		Log.d(TAG, "START SERVICE ON DESTROY GET CALLED...3");
		stopService(new Intent(this, DynamixConnectionService.class));
		stopService(new Intent(this, CoapServerManager.class));
		stopService(new Intent(this, HTTPServerManager.class));
		stopService(new Intent(this, ManagerManager.class));
		stopService(new Intent(this, UpdateManager.class));
		stopService(new Intent(this, NotificationService.class));
		DiscoveryService.tearDown();
		stopService(new Intent(this, DiscoveryService.class));
		UpdateManager.deactivate();
		stopService(new Intent(this, DynamixBridgeCoapClient.class));
		running=false;
	}
	
	@Override
	public int onStartCommand(Intent intent, int flags, int startId) 
	{
		if(!running)
		{
			running=true;
			super.onStartCommand(intent, flags, startId);
			Log.i(TAG, "START SERVICE");
			try
			{
				startService(new Intent(this, DynamixConnectionService.class));
				startService(new Intent(this, CoapServerManager.class));
				startService(new Intent(this, HTTPServerManager.class));
				startService(new Intent(this, ManagerManager.class));
				startService(new Intent(this, UpdateManager.class));
				startService(new Intent(this, NotificationService.class));
				startService(new Intent(this, DiscoveryService.class));
				startService(new Intent(this, DynamixBridgeCoapClient.class));
				UpdateManager.updateList();
			}
			catch(Exception e)
			{
				
			}
			return START_STICKY;
		}
		else
		{
			return START_STICKY;
		}
	}
	

}

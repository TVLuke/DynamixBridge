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

package de.uniluebeck.itm.dynamixsspbridge.networking;

import java.io.IOException;
import java.net.ServerSocket;

import android.app.Service;
import android.content.Context;
import android.content.Intent;
import android.net.nsd.NsdManager;
import android.net.nsd.NsdManager.RegistrationListener;
import android.net.nsd.NsdServiceInfo;
import android.os.IBinder;
import android.util.Log;

public class DiscoveryService extends Service
{

	ServerSocket mServerSocket;
	static RegistrationListener mRegistrationListener;
	String mServiceName;
	static NsdManager mNsdManager;
	private final String TAG ="SSPBridge";
	
	@Override
	public IBinder onBind(Intent arg0) 
	{
		return null;
	}

	@Override
	public int onStartCommand(Intent intent, int flags, int startId) 
	{
		super.onStartCommand(intent, flags, startId);
		Log.e(TAG, "START NSD SERVICE");
		int port = getAvaliablePort();
		registerService(port);
		return START_STICKY;
	}
	
	@Override
	public void onCreate() 
	{
		super.onCreate();
		Log.e(TAG, "onCreate()");
	}
	
	@Override
	public void onDestroy()
	{
        Log.e(TAG, "on destroy on the Dicovery Service is called...");
	    tearDown();
		super.onDestroy();
		Log.d(TAG, "Stop... Destroy!");
	}
	
	public void registerService(final int port)
	{
		Log.d(TAG, "REGISTER STARTS");
        final Context ctx = this;
        //TODO: There is  a bug in the newes Android Versions that makes using this imposible. Has not been fixed from 4.2.2 to 4.3 so it seems to be low priority for the android team
        //https://code.google.com/p/android/issues/detail?id=35585
/**
                // Create the NsdServiceInfo object, and populate it.
                NsdServiceInfo serviceInfo  = new NsdServiceInfo();

                // The name is subject to change based on conflicts
                // with other services advertised on the same network.

                serviceInfo.setServiceName("DynamixBridge_"+port);
                serviceInfo.setServiceType("_http._tcp.");
                serviceInfo.setPort(port);

                initializeRegistrationListener();

                mNsdManager = (NsdManager) ctx.getSystemService(Context.NSD_SERVICE);

                mNsdManager.registerService(serviceInfo, NsdManager.PROTOCOL_DNS_SD, mRegistrationListener);
	    Log.d(TAG, "REGISTER DONE... WHAT NOW?");
 **/
	}
	
	public int getAvaliablePort() 
	{
		Log.d(TAG, "I AM SEARCHING FOR AN AVALIABLE PORT");
		int mLocalPort=8889;
	    // Initialize a server socket on the next available port.
	    try 
	    {
			mServerSocket = new ServerSocket(0);
		} 
	    catch (IOException e) 
	    {
			// TODO Auto-generated catch block
			//e.printStackTrace();
		}
	    Log.d(TAG, "THE CHOSEN PORT IS");
	    // Store the chosen port.
	    mLocalPort = mServerSocket.getLocalPort();
	    Log.d(TAG, "->"+mLocalPort);
	    return mLocalPort;
	}
	
	public void initializeRegistrationListener() 
	{
	    mRegistrationListener = new NsdManager.RegistrationListener() 
	    {

	        @Override
	        public void onServiceRegistered(NsdServiceInfo NsdServiceInfo) 
	        {
	        	Log.d(TAG, "ON NSD SERVICE REGISTERED");
	            // Save the service name.  Android may have changed it in order to
	            // resolve a conflict, so update the name you initially requested
	            // with the name Android actually used.
	            mServiceName = NsdServiceInfo.getServiceName();
	        }

	        @Override
	        public void onRegistrationFailed(NsdServiceInfo serviceInfo, int errorCode) 
	        {
                Log.e(TAG, "onRegistrationFailed...");
	            // Registration failed!  Put debugging code here to determine why.
	        }

	        @Override
	        public void onServiceUnregistered(NsdServiceInfo arg0) 
	        {
	            // Service has been unregistered.  This only happens when you call
	            // NsdManager.unregisterService() and pass in this listener.
	        }

	        @Override
	        public void onUnregistrationFailed(NsdServiceInfo serviceInfo, int errorCode) 
	        {
	            // Unregistration failed.  Put debugging code here to determine why.
	        }
	    };
	}
	
	 // NsdHelper's tearDown method
    public static void tearDown() 
    {
        if(mNsdManager!=null)
        {
            mNsdManager.unregisterService(mRegistrationListener);
        }
    }
}

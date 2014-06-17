package de.uniluebeck.itm.dynamixsspbridge.networking;

import java.io.IOException;
import java.net.ServerSocket;
import java.util.HashMap;
import java.util.Map;

import android.app.Service;
import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.Intent;
import android.content.IntentFilter;
import android.net.wifi.p2p.WifiP2pManager;
import android.net.wifi.p2p.WifiP2pManager.ActionListener;
import android.net.wifi.p2p.WifiP2pManager.Channel;
import android.net.wifi.p2p.nsd.WifiP2pDnsSdServiceInfo;
import android.os.IBinder;
import android.util.Log;

public class WifiDirectService extends Service 
{

	private static String TAG ="SSPBridge";
	
	WifiP2pManager mManager;
	Channel mChannel;
	BroadcastReceiver mReceiver;
	IntentFilter mIntentFilter;
	ServerSocket mServerSocket;
	
	@Override
	public IBinder onBind(Intent arg0) 
	{
		// TODO Auto-generated method stub
		return null;
	}

	@Override
	public int onStartCommand(Intent intent, int flags, int startId) 
	{
		super.onStartCommand(intent, flags, startId);
		Log.d(TAG, "START WIFI DIRECT SERVICE");
		mManager = (WifiP2pManager) getSystemService(Context.WIFI_P2P_SERVICE);
		mChannel = mManager.initialize(this, getMainLooper(), null);
		mReceiver = new WifiDirectBroadcastReceiver(mManager, mChannel, this);
		
		mIntentFilter = new IntentFilter();
	    mIntentFilter.addAction(WifiP2pManager.WIFI_P2P_STATE_CHANGED_ACTION);
	    mIntentFilter.addAction(WifiP2pManager.WIFI_P2P_PEERS_CHANGED_ACTION);
	    mIntentFilter.addAction(WifiP2pManager.WIFI_P2P_CONNECTION_CHANGED_ACTION);
	    mIntentFilter.addAction(WifiP2pManager.WIFI_P2P_THIS_DEVICE_CHANGED_ACTION);
	    
	    registerReceiver(mReceiver, mIntentFilter);
	    
	    mManager.discoverPeers(mChannel, new WifiP2pManager.ActionListener() 
	    {
	        @Override
	        public void onSuccess() 
	        {
	            Log.d(TAG, "success on Discovering Peers, no idea yet what this means");
	        }

	        @Override
	        public void onFailure(int reasonCode) 
	        {
	        	 Log.d(TAG, "fail on Discovering Peers, no idea yet what this means");
	        }
	    });
	    
	    startRegistration();
	    
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
		unregisterReceiver(mReceiver);
		super.onDestroy();
		
	}
	
    private void startRegistration() 
    {
        //  Create a string map containing information about your service.
    	int SERVER_PORT = getAvaliablePort(); 
        Map record = new HashMap();
        record.put("listenport", String.valueOf(SERVER_PORT));
        record.put("buddyname", "DynamixBridge_" + SERVER_PORT);
        record.put("available", "visible");

        // Service information.  Pass it an instance name, service type
        // _protocol._transportlayer , and the map containing
        // information other devices will want once they connect to this one.
        WifiP2pDnsSdServiceInfo serviceInfo =
                WifiP2pDnsSdServiceInfo.newInstance("_test", "_presence._tcp", record);

        // Add the local service, sending the service info, network channel,
        // and listener that will be used to indicate success or failure of
        // the request.
        mManager.addLocalService(mChannel, serviceInfo, new ActionListener() {
            @Override
            public void onSuccess() 
            {
                // Command successful! Code isn't necessarily needed here,
                // Unless you want to update the UI or add logging statements.
            }

            @Override
            public void onFailure(int arg0) 
            {
                // Command failed.  Check for P2P_UNSUPPORTED, ERROR, or BUSY
            }
        });
    }
	
	public int getAvaliablePort() 
	{
		Log.e(TAG, "I AM SEARCHING FOR AN AVALIABLE PORT");
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
	    Log.e(TAG, "THE CHOSEN PORT IS");
	    // Store the chosen port.
	    mLocalPort = mServerSocket.getLocalPort();
	    Log.e(TAG, "->"+mLocalPort);
	    return mLocalPort;
	}
}

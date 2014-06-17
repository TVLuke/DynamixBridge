package de.uniluebeck.itm.dynamixsspbridge.networking;

import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.Intent;
import android.net.wifi.p2p.WifiP2pManager;
import android.net.wifi.p2p.WifiP2pManager.Channel;
import android.util.Log;

public class WifiDirectBroadcastReceiver extends BroadcastReceiver
{

	private static String TAG ="SSPBridge";
	
	 private WifiP2pManager mManager;
	 private Channel mChannel;
	 private WifiDirectService wifiDirectService;
	
	public WifiDirectBroadcastReceiver(WifiP2pManager mManager,	Channel mChannel, WifiDirectService wifiDirectService)
	{
		super();
        this.mManager = mManager;
        this.mChannel = mChannel;
        this.wifiDirectService = wifiDirectService;
	}

	@Override
	public void onReceive(Context context, Intent intent) 
	{
	    String action = intent.getAction();
	    if (WifiP2pManager.WIFI_P2P_STATE_CHANGED_ACTION.equals(action)) 
	    {
	    	int state = intent.getIntExtra(WifiP2pManager.EXTRA_WIFI_STATE, -1);
	        if (state == WifiP2pManager.WIFI_P2P_STATE_ENABLED) 
	        {
	            // Wifi P2P is enabled
	        	Log.d(TAG, "Wifi P2P is enabled");
	        } 
	        else 
	        {
	        	Log.d(TAG, "Wi-Fi P2P is not enabled");
	            // Wi-Fi P2P is not enabled
	        }
        } 
	    else if (WifiP2pManager.WIFI_P2P_PEERS_CHANGED_ACTION.equals(action)) 
        {
	    	Log.d(TAG, "pEERS cHANGED");
        } 
        else if (WifiP2pManager.WIFI_P2P_CONNECTION_CHANGED_ACTION.equals(action)) 
        {
        	Log.d(TAG, "WIFI_P2P_CONNECTION_CHANGED_ACTION");
        } 
        else if (WifiP2pManager.WIFI_P2P_THIS_DEVICE_CHANGED_ACTION.equals(action)) 
        {
        	Log.d(TAG, "WIFI_P2P_THIS_DEVICE_CHANGED_ACTION");
        }
	}
}

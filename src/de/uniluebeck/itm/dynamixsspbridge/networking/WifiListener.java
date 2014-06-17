package de.uniluebeck.itm.dynamixsspbridge.networking;

import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.Intent;
import android.net.ConnectivityManager;
import android.net.NetworkInfo;
import android.net.wifi.WifiManager;
import android.util.Log;

public class WifiListener  extends BroadcastReceiver 
{
	private static String TAG ="SSPBridge";
	
	public static boolean wifi=false;
	 @Override
	    public void onReceive(Context context, Intent intent) 
	    {
	       Log.d(TAG, "onReceive");

	        NetworkInfo networkInfo = intent.getParcelableExtra(WifiManager.EXTRA_NETWORK_INFO);
	        if (networkInfo != null) 
	        {
	                if (networkInfo.getType() == ConnectivityManager.TYPE_WIFI) 
	                {
	                        if (networkInfo.getState() == NetworkInfo.State.CONNECTED) 
	                        {
	                                wifi=true;
	                                context.stopService(new Intent(context, WifiDirectService.class));
	                                context.startService(new Intent(context, DiscoveryService.class));
	                        }
	                        else
	                        {
	                                wifi=false;
	                                context.stopService(new Intent(context, DiscoveryService.class));
	                                context.startService(new Intent(context, WifiDirectService.class));
	                        }
	                }
	        	}

	        }
}

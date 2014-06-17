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

package de.uniluebeck.itm.dynamixsspbridge.ui;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.concurrent.ConcurrentHashMap;

import de.uniluebeck.itm.coapserver.CoapServerManager;
import de.uniluebeck.itm.coapserver.Utils;
import de.uniluebeck.itm.dynamixsspbridge.R;
import de.uniluebeck.itm.dynamixsspbridge.core.UpdateManager;
import de.uniluebeck.itm.dynamixsspbridge.dynamix.ContextPlugin;
import de.uniluebeck.itm.dynamixsspbridge.dynamix.ContextType;
import de.uniluebeck.itm.dynamixsspbridge.support.Constants;
import android.app.Activity;
import android.content.Context;
import android.content.Intent;
import android.content.SharedPreferences;
import android.content.SharedPreferences.Editor;
import android.os.Bundle;
import android.os.Handler;
import android.util.Log;
import android.view.View;
import android.view.View.OnClickListener;
import android.widget.Button;
import android.widget.EditText;
import android.widget.ImageView;
import android.widget.ListView;
import android.widget.TextView;

public class ContextUnsubscriptionRequested extends Activity
{
	private static String TAG ="SSPBridge";
	private static final String PREFS = Constants.PREFS;
	
	static String name;
	
	private UIUpdater updater;
	
	protected void onCreate(Bundle savedInstanceState) 
	{
		super.onCreate(savedInstanceState);
		setContentView(R.layout.unsubscriptionrequest);
		Intent intent = getIntent();
		String contextType = intent.getStringExtra("contexttype");
   		
		Log.d(TAG, contextType+"");
		HashMap<String, ContextType> contexttypes = UpdateManager.getContextTypes();
		final ContextType type = contexttypes.get(contextType);
		if(type!=null)
		{
	        TextView friendlynameTV = (TextView) findViewById(R.id.friendlyname);
			friendlynameTV.setText(type.getUserFriendlyName());
			TextView nameTV = (TextView) findViewById(R.id.name);
			name = type.getName();
			nameTV.setText(type.getName());
			ListView lv = (ListView) findViewById(R.id.pluginlistview);
			Log.d(TAG, "a");
			ArrayList<ContextPlugin> plugins = type.getPluginList();
	        
			final PluginListViewAdapter adapter =  new PluginListViewAdapter(this, plugins, type.getName());
			Log.d(TAG, "b");
			lv.setAdapter(adapter);
			Button button1 = (Button) findViewById(R.id.button1);
			button1.setOnClickListener(new OnClickListener() 
			{
				@Override
				public void onClick(View v) 
				{
					type.unsubscribe();
					SharedPreferences prefs = ContextUnsubscriptionRequested.this.getSharedPreferences(PREFS, 0);
					Editor e = prefs.edit();
					e.putBoolean(type.getName(), false);
					e.commit();
	           		adapter.notifyDataSetChanged();
	           		ContextUnsubscriptionRequested.this.finish();
				}
			});
			Button button2 = (Button) findViewById(R.id.button2);
			button2.setOnClickListener(new OnClickListener() 
			{
				@Override
				public void onClick(View v) 
				{
					ContextUnsubscriptionRequested.this.finish();
				}
			});
		}
		
		
		updater= new UIUpdater();
		updater.run();
	}
	

	@Override
	protected void onPause() 
	{
		super.onPause();
		if(updater!=null)
		{
			updater.onPause();
		}
	}
	
	@Override
	public void onDestroy()
	{
		Log.d(TAG, "on Destroy-!");
		super.onDestroy();
	}
	
	
	@Override
	protected void onResume()
	{
		super.onResume();
		if(updater!=null)
		{
			updater.onResume();
		}
		else
		{
			updater= new UIUpdater();
			updater.run();
		}
		
	}
	
	private class  UIUpdater implements Runnable
	{
    	private Handler handler = new Handler();
    	public static final int delay= 500;
    	
    	@Override
		public void run() 
    	{
    		Log.d(TAG, "updater ContextItem View");

    		ContextType contextType = UpdateManager.getTypeById(name);
    		if(contextType.active())
            {
            	TextView url = (TextView) findViewById(R.id.ipandport);
            	try
            	{
            	url.setText("coap://"+UpdateManager.getIP()+":"+"5683"+"/"+name.replace(".", "/"));
            	}
            	catch (Exception e)
            	{
            		//nothing
            	}
           		ImageView image = (ImageView) findViewById(R.id.imageView1);
           		image.setImageResource(R.drawable.active);
            }
    		else if(contextType.isWaitingForSubscription())
    		{
    			ImageView pic = (ImageView) findViewById(R.id.imageView1);
    			pic.setImageResource(R.drawable.waitingforsupport);
    		}
    		else if(contextType.contextSupported())
    		{
    			ImageView pic = (ImageView) findViewById(R.id.imageView1);
    			pic.setImageResource(R.drawable.supported);
    		}
    		else
    		{
    			ImageView pic = (ImageView) findViewById(R.id.imageView1);
    			pic.setImageResource(R.drawable.inactive);
    		}
    		 
	        handler.removeCallbacks(this); // remove the old callback
	        handler.postDelayed(this, delay); // register a new one
		}
    	
    	public void onResume()
    	{
            handler.removeCallbacks(this); // remove the old callback
            handler.postDelayed(this, delay); // register a new one
    	}
    	
        public void onPause()
        {
            handler.removeCallbacks(this); // stop the map from updating
        }
	}
}

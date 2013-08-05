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
import java.util.Iterator;
import java.util.Map.Entry;
import java.util.Set;
import java.util.concurrent.ConcurrentHashMap;

import de.uniluebeck.itm.coapserver.CoapServerManager;
import de.uniluebeck.itm.coapserver.Utils;
import de.uniluebeck.itm.dynamixsspbridge.R;
import de.uniluebeck.itm.dynamixsspbridge.core.StartService;
import de.uniluebeck.itm.dynamixsspbridge.core.UpdateManager;
import de.uniluebeck.itm.dynamixsspbridge.dynamix.ContextType;
import de.uniluebeck.itm.dynamixsspbridge.dynamix.DynamixConnectionService;
import de.uniluebeck.itm.dynamixsspbridge.support.Constants;
import de.uniluebeck.itm.dynamixsspbridge.support.NotificationService;

import android.app.Activity;
import android.content.Context;
import android.content.Intent;
import android.os.Bundle;
import android.os.Handler;
import android.util.Log;
import android.view.View;
import android.view.View.OnClickListener;
import android.widget.AdapterView;
import android.widget.AdapterView.OnItemClickListener;
import android.widget.ArrayAdapter;
import android.widget.Button;
import android.widget.ListView;
import android.widget.TextView;

public class SubscriptionView extends Activity 
{
	
	private final String TAG ="SSPBridge";
	private static final String PREFS = Constants.PREFS;
	private static Context ctx;
	private static ConcurrentHashMap<String, ContextType> list;
	static ContextTypeListViewAdapter adapter;

	private UIUpdater updater;

	protected void onCreate(Bundle savedInstanceState) 
	{
		super.onCreate(savedInstanceState);
		setContentView(R.layout.activity_subscription);
		startService(new Intent(this, StartService.class));
		Log.e(TAG, "onCreate()");
		ctx=this;
		Log.e(TAG, "IP : "+UpdateManager.getIP());
		TextView coapip = (TextView) findViewById(R.id.coapip);
		coapip.setText(UpdateManager.getIP());
		ListView lv = (ListView) findViewById(R.id.listView1);
		UpdateManager.updateList();
		list =UpdateManager.getContextTypes();
        adapter =  new ContextTypeListViewAdapter(ctx, list);
        lv.setAdapter(adapter); 
        adapter.notifyDataSetChanged();
		Button button1 = (Button) findViewById(R.id.button1);
		button1.setOnClickListener(new OnClickListener() 
		{
			@Override
			public void onClick(View v) 
			{
				//Log.i(TAG, "KLICK");
				try
				{
						UpdateManager.updateList();
				}
				catch(Exception e)
				{
					//blablabla concurent modification and stuff
				}
				list = UpdateManager.getContextTypes();
				if(list!=null)
				{
					adapter.notifyDataSetChanged();
					//Log.d(TAG, "List not null..."+list.size());
			
				}
				else
				{
					Log.d(TAG, "list is null");
				}
			}
		});
        lv.setOnItemClickListener(new OnItemClickListener() 
        {
       	 
            @Override
            public void onItemClick(AdapterView<?> parent, View view, int position, long id) {
           	 //Log.d(TAG, "KLICK AGAIN. I DARE YOU, I DOUBLE DARE YOU");
           	 Intent i = new Intent(SubscriptionView.this, ContextItemView.class);
           	 //Log.d(TAG, ""+position);
           	 i.putExtra("position", position);
           	 startActivity(i);
            }
 
        });
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
	protected void onDestroy()
	{
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
    		//Log.d(TAG, "updater main View");
    		list =UpdateManager.getContextTypes();
    		//Log.e(TAG, "IP : "+UpdateManager.getIP());
    		TextView coapip = (TextView) findViewById(R.id.coapip);
    		coapip.setText("IP: "+UpdateManager.getIP());
    		//adapter.notifyDataSetInvalidated();
    		adapter.notifyDataSetChanged();

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

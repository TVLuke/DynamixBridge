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
import de.uniluebeck.itm.dynamixsspbridge.dynamix.DynamixConnectionService;
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
import android.widget.AdapterView;
import android.widget.AdapterView.OnItemLongClickListener;
import android.widget.CheckBox;
import android.widget.CompoundButton;
import android.widget.EditText;
import android.widget.ImageView;
import android.widget.ListView;
import android.widget.AdapterView.OnItemClickListener;
import android.widget.CompoundButton.OnCheckedChangeListener;
import android.widget.TextView;

public class ContextItemView extends Activity
{
	private static final String TAG ="SSPBridge";
	private static final String PREFS = Constants.PREFS;
	String friendlyname;
	static String name;
	static Context ctx;
	
	private UIUpdater updater;
	
	@Override
    public void onCreate(Bundle savedInstanceState) 
	{
        super.onCreate(savedInstanceState);
        setContentView(R.layout.contextitemview);
		Log.d(TAG, "ok, oncreate");
        ctx=this;
        final SharedPreferences prefs = ctx.getSharedPreferences(PREFS, 0);
        Intent i = getIntent();
        final int position = i.getExtras().getInt("position");
        final ConcurrentHashMap<String, ContextType> contexttypes = UpdateManager.getContextTypes();
        final ContextType contextType = contexttypes.get((String) contexttypes.keySet().toArray()[position]);
		name = contextType.getName();
        friendlyname = contextType.getUserFriendlyName();
        ArrayList<ContextPlugin> plugins = contextType.getPluginList();
        boolean ischecked= prefs.getBoolean(name, false);
		if(contextType.active())
        {
        	TextView url = (TextView) findViewById(R.id.ipandport);
        	url.setText("coap://"+Utils.getIPAddress(true)+":"+"5683"+"/"+name.replace(".", "/"));
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
        TextView friendlynameTV = (TextView) findViewById(R.id.friendlyname);
		friendlynameTV.setText(friendlyname);

		TextView nameTV = (TextView) findViewById(R.id.name);
		nameTV.setText(name);


		TextView description = (TextView) findViewById(R.id.description);
		description.setText(contextType.getShortDescription());
		Log.d(TAG, "ok, trying to build a list now...");
		ListView lv = (ListView) findViewById(R.id.pluginlistview);
		Log.d(TAG, "a");
		final PluginListViewAdapter adapter =  new PluginListViewAdapter(ctx, plugins, name);
		Log.d(TAG, "b");
		lv.setAdapter(adapter); 
		Log.d(TAG, "c");
		
		Log.d(TAG, "ok, checkbox");
		CheckBox sendtossp = (CheckBox) findViewById(R.id.checkBox1);
		sendtossp.setChecked(ischecked);
		if(ischecked)
		{
			
		}
		else
		{
       		ImageView image = (ImageView) findViewById(R.id.imageView1);
       		image.setImageResource(R.drawable.applications_gray);
		}
		sendtossp.setOnCheckedChangeListener(new OnCheckedChangeListener()
		{
			@Override
			public void onCheckedChanged(CompoundButton button, boolean checked) 
			{
				if(checked)
				{
					EditText updateIntervall = (EditText) findViewById(R.id.updateIntervall);

					
					int uint = 10000;
					try
					{
						String intervall = updateIntervall.getText().toString();
						uint=Integer.parseInt(intervall);
						uint=uint*1000;
					}
					catch(Exception e)
					{
						
					}
					contextType.subscribe();
					Editor e = prefs.edit();
					e.putBoolean(name, true);
					e.commit();
	           		adapter.notifyDataSetChanged();
	           		ImageView image = (ImageView) findViewById(R.id.imageView1);
	           		image.setImageResource(R.drawable.logo);
	                if(contextType.active())
	                {
	                	TextView url = (TextView) findViewById(R.id.ipandport);
	                	url.setText("coap://"+Utils.getIPAddress(true)+":"+"XXXX"+"/"+name.replace(".", "/"));
	                }
				}
				else
				{
					DynamixConnectionService.unsubscribeToContext(contextType);
					Editor e = prefs.edit();
					e.putBoolean(name, false);
					e.commit();
	           		adapter.notifyDataSetChanged();
	           		ImageView image = (ImageView) findViewById(R.id.imageView1);
	           		image.setImageResource(R.drawable.applications_gray);
	           		TextView url = (TextView) findViewById(R.id.ipandport);
	           		url.setText("");
				}
			}
		});
		lv.setOnItemClickListener(new OnItemClickListener() {
       	 
            @Override
            public void onItemClick(AdapterView<?> parent, View view, int posi, long id) 
            {
            	Log.d(TAG, "clickccccclock");
           		adapter.clickItem(posi);
           		adapter.notifyDataSetChanged();
            }
 
        });
		lv.setOnItemLongClickListener(new OnItemLongClickListener() {

            public boolean onItemLongClick(AdapterView<?> arg0, View arg1, int pos, long id) 
            {
            	Log.d(TAG, "loooooooooooooooooooong click at"+pos);
            	adapter.longClickItem(pos);
           		adapter.notifyDataSetChanged();
                return true;
            }
        }); 

		lv.setClickable(true);
		Log.d(TAG, "d");
		
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
    		Log.d(TAG, "updater ContextItem View");

    		ContextType contextType = UpdateManager.getTypeById(name);
    		if(contextType.active())
            {
            	TextView url = (TextView) findViewById(R.id.ipandport);
            	url.setText("coap://"+Utils.getIPAddress(true)+":"+"XXXX"+"/"+name.replace(".", "/"));
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

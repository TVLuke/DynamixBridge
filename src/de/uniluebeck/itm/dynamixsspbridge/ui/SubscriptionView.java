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

import java.net.URI;
import java.util.concurrent.ConcurrentHashMap;

import de.uniluebeck.itm.coapserver.CoapServerManager;
import de.uniluebeck.itm.coapserver.Utils;
import de.uniluebeck.itm.dynamixbridge.ssp.DynamixBridgeCoapClient.SimpleResponseProcessor;
import de.uniluebeck.itm.dynamixsspbridge.R;
import de.uniluebeck.itm.dynamixsspbridge.core.StartService;
import de.uniluebeck.itm.dynamixsspbridge.core.UpdateManager;
import de.uniluebeck.itm.dynamixsspbridge.dynamix.ContextType;
import de.uniluebeck.itm.dynamixsspbridge.dynamix.DynamixConnectionService;
import de.uniluebeck.itm.dynamixsspbridge.support.Constants;
import de.uniluebeck.itm.dynamixsspbridge.support.NotificationService;
import de.uniluebeck.itm.ncoap.application.client.CoapClientApplication;
import de.uniluebeck.itm.ncoap.application.client.CoapResponseProcessor;
import de.uniluebeck.itm.ncoap.communication.reliability.outgoing.EmptyAcknowledgementProcessor;
import de.uniluebeck.itm.ncoap.communication.reliability.outgoing.InternalEmptyAcknowledgementReceivedMessage;
import de.uniluebeck.itm.ncoap.communication.reliability.outgoing.InternalRetransmissionTimeoutMessage;
import de.uniluebeck.itm.ncoap.communication.reliability.outgoing.RetransmissionTimeoutProcessor;
import de.uniluebeck.itm.ncoap.message.CoapRequest;
import de.uniluebeck.itm.ncoap.message.CoapResponse;
import de.uniluebeck.itm.ncoap.message.header.Code;
import de.uniluebeck.itm.ncoap.message.header.MsgType;

import android.app.Activity;
import android.content.Context;
import android.content.Intent;
import android.os.Bundle;
import android.os.Handler;
import android.util.Log;
import android.view.Menu;
import android.view.MenuInflater;
import android.view.MenuItem;
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
        //TODO: here I should sort this stuff
        lv.setAdapter(adapter); 
        adapter.notifyDataSetChanged();
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
	
	@Override
	public boolean onCreateOptionsMenu(Menu menu) 
	{
	    // Inflate the menu items for use in the action bar
	    MenuInflater inflater = getMenuInflater();
	    inflater.inflate(R.menu.main_activity_actions, menu);
	    return super.onCreateOptionsMenu(menu);
	}

	@Override
	public boolean onOptionsItemSelected(MenuItem item) 
	{
	    // Handle presses on the action bar items
	    switch (item.getItemId()) 
	    {
	        case R.id.action_update:
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
	            return true;
	        case R.id.action_list:
	        	Intent i = new Intent(this, TolkenOverview.class);
	        	startActivity(i);
	            //TODO start activity TolkenOv
	            return true;
	        case R.id.action_key:
	            //TODO delet this stuff again later.
	        	Thread thr = new Thread() 
	        	{
	        		  public void run() 
	        		  {
	        			try
	      				{
	      					CoapClientApplication client = new CoapClientApplication();
	      					URI targetURI = new URI ("coap://"+UpdateManager.getIP()+"/org/ambientdynamix/contextplugins/context/info/environment/currentsong?token=123456");
	      					CoapRequest coapRequest =  new CoapRequest(MsgType.CON, Code.POST, targetURI);
	      					byte[] bytes = "lol".getBytes();
	      					coapRequest.setPayload(bytes);
	      					
	      					client.writeCoapRequest(coapRequest, new SimpleResponseProcessor());
	      				}
	      				catch(Exception e)
	      				{
	      					Log.e(TAG, "test"+e.getMessage());
	      				}
	        		  }
	        		};
	        		thr.start();
	     
	        	//---
	            return true;
	        default:
	            return super.onOptionsItemSelected(item);
	    }
	}
	
	public class SimpleResponseProcessor implements CoapResponseProcessor, EmptyAcknowledgementProcessor, RetransmissionTimeoutProcessor 
	{

		
		@Override
		public void processCoapResponse(CoapResponse coapResponse) 
		{
		    Log.d(TAG, "Received Response: " + coapResponse);
		}
		
		@Override
		public void processEmptyAcknowledgement(InternalEmptyAcknowledgementReceivedMessage message) 
		{
			Log.d(TAG, "Received empty ACK: " + message);
		}
		
		@Override
		public void processRetransmissionTimeout(InternalRetransmissionTimeoutMessage timeoutMessage) 
		{
			Log.d(TAG, "Transmission timed out: " + timeoutMessage);
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
    		SubscriptionView.this.runOnUiThread(new Runnable()
    		{
    		    public void run()
    		    {
    	    		list =UpdateManager.getContextTypes();
    	    		//Log.e(TAG, "IP : "+UpdateManager.getIP());
    	    		TextView coapip = (TextView) findViewById(R.id.coapip);
    	    		coapip.setText("IP: "+UpdateManager.getIP()+" ");
    	    		//adapter.notifyDataSetInvalidated();
    	    		adapter.notifyDataSetChanged();
    		    }
    		});

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

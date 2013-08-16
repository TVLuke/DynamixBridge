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

package de.uniluebeck.itm.dynamixsspbridge.support;

import org.ambientdynamix.api.application.IDynamixFacade;

import de.uniluebeck.itm.dynamixsspbridge.R;
import de.uniluebeck.itm.dynamixsspbridge.ui.ContextSubscriptionRequested;
import de.uniluebeck.itm.dynamixsspbridge.ui.SubscriptionView;

import android.app.Notification;
import android.app.NotificationManager;
import android.app.PendingIntent;
import android.app.Service;
import android.content.Context;
import android.content.Intent;
import android.os.IBinder;
import android.os.RemoteException;
import android.util.Log;

public class NotificationService extends Service
{

	private static String TAG ="SSPBridge";
	private static Context ctx;
	private static int id=1111;
	
	@Override
	public IBinder onBind(Intent arg0) {
		// TODO Auto-generated method stub
		return null;
	}
	
	@Override
	public void onCreate() 
	{
		super.onCreate();
		ctx=this;
		Log.e(TAG, "starting notification Service");
	}
	
	@Override
	public int onStartCommand(Intent intentX, int flags, int startId) 
	{
		super.onStartCommand(intentX, flags, startId);
		if(ctx!=null)
		{
			id++;
			Log.d(TAG, "ctx!=null");
			NotificationManager notificationManager = (NotificationManager) ctx.getSystemService(NOTIFICATION_SERVICE); 
			notificationManager.cancel(id);
			Intent intent = new Intent(ctx,SubscriptionView.class);
			PendingIntent pIntent = PendingIntent.getActivity(ctx, id, intent, 0);
			Notification noti = new Notification.Builder(ctx)
	        .setContentTitle("Dynamix Bridge Running")
	        .setContentText("")
	        .setSmallIcon(R.drawable.ic_launcher)
	        .setContentIntent(pIntent).build();
			Log.d(TAG, "flag.");
			noti.flags |= Notification.FLAG_ONGOING_EVENT;
			Log.d(TAG, "go:");
			notificationManager.notify(id, noti); 
		}
		//TODO: permanente notication starten
		return START_STICKY;
	}
	
	@Override
	public void onDestroy()
	{
		super.onDestroy();
		NotificationManager notificationManager = (NotificationManager) ctx.getSystemService(NOTIFICATION_SERVICE); 
		notificationManager.cancel(id);
	}
	
	public static void requestContext(String payload) 
	{
		Log.d(TAG, "blabla="+payload);
		if(ctx!=null)
		{
			id++;
			Log.d(TAG, "ctx!=null");
			NotificationManager notificationManager = (NotificationManager) ctx.getSystemService(NOTIFICATION_SERVICE); 
			Intent intent = new Intent(ctx, ContextSubscriptionRequested.class);
			intent.putExtra("contexttype", payload);
			Log.d(TAG, intent.getStringExtra("contexttype"));
			PendingIntent pIntent = PendingIntent.getActivity(ctx, id, intent, 0);
			Notification noti = new Notification.Builder(ctx)
	        .setContentTitle("Request for Context Subscription")
	        .setContentText(payload)
	        .setSmallIcon(R.drawable.notification)
	        .setContentIntent(pIntent).build();
			Log.d(TAG, "flag");
			noti.flags |= Notification.FLAG_AUTO_CANCEL;
			noti.flags |= Notification.FLAG_SHOW_LIGHTS;
			noti.ledARGB=0xffffffff; //color, in this case, white
			noti.ledOnMS=1000; //light on in milliseconds
			noti.ledOffMS=4000; //light off in milliseconds
			noti.defaults |= Notification.DEFAULT_SOUND;
			noti.defaults |= Notification.DEFAULT_VIBRATE;
			Log.d(TAG, "go:");
			notificationManager.notify(id, noti); 
		}
		
	}
}

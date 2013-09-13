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

package de.uniluebeck.itm.dynamixsspbridge.dynamix;

import java.util.Iterator;
import java.util.List;
import java.util.StringTokenizer;
import java.util.concurrent.ConcurrentHashMap;

import org.ambientdynamix.api.application.ContextEvent;
import org.ambientdynamix.api.application.ContextPluginInformation;
import org.ambientdynamix.api.application.ContextPluginInformationResult;
import org.ambientdynamix.api.application.ContextSupportInfo;
import org.ambientdynamix.api.application.IContextInfo;
import org.ambientdynamix.api.application.IDynamixFacade;
import org.ambientdynamix.api.application.IDynamixListener;
import org.ambientdynamix.contextplugins.context.info.sample.IPingContextInfo;

import android.app.Service;
import android.content.ComponentName;
import android.content.Context;
import android.content.Intent;
import android.content.ServiceConnection;
import android.os.Bundle;
import android.os.IBinder;
import android.os.RemoteException;
import android.util.Log;

public class DynamixConnectionService extends Service 
{

	private static String TAG ="SSPBridge";
	public static DynamixConnectionService ctx;
	private static IDynamixFacade dynamix;

	private static ConcurrentHashMap<String, ContextType> allContextTypes =  new ConcurrentHashMap<String, ContextType>();
	
	@Override
	public IBinder onBind(Intent arg0) 
	{
		return null;
	}
	
	@Override
	public void onCreate() 
	{
		super.onCreate();
		ctx=this;
		 Log.e(TAG, "bla");
	}
	
	@Override
	public int onStartCommand(Intent intent, int flags, int startId) 
	{
		super.onStartCommand(intent, flags, startId);
		Log.i(TAG, "bla2");
		dynamixconnectandbind();
		return START_STICKY;
	}
	
	private void dynamixconnectandbind()
	{
		if (dynamix == null) 
		{
			bindService(new Intent(IDynamixFacade.class.getName()), sConnection, Context.BIND_AUTO_CREATE);
			Log.i(TAG, "A1 - Connecting to Dynamix...");
		} 
		else 
		{
			try 
			{
				if (!dynamix.isSessionOpen()) 
				{
					Log.i(TAG, "Dynamix connected... trying to open session");
					dynamix.openSession();
				} 
				else
				{
					Log.i(TAG, "Session is already open");
				}
			} 
			catch (RemoteException e) 
			{
				Log.e(TAG, e.toString());
			}
		}
	}
    @Override
	public void onDestroy() 
    {
    	Log.d(TAG, "bye");
		if (dynamix != null) 
		{
			try 
			{
				dynamix.removeDynamixListener(dynamixCallback);
				unbindService(sConnection);
			} 
			catch (Exception e) 
			{
				
			}
		}

    	super.onDestroy();
    }

    
    /*
	 * The ServiceConnection is used to receive callbacks from Android telling our application that it's been connected
	 * to Dynamix, or that it's been disconnected from Dynamix. These events come from Android, not Dynamix. Dynamix
	 * events are always sent to our IDynamixListener object (defined farther below), which is registered (in this case)
	 * in during the 'addDynamixListener' call in the 'onServiceConnected' method of the ServiceConnection.
	 */
	private ServiceConnection sConnection = new ServiceConnection() 
	{
		/*
		 * Indicates that we've successfully connected to Dynamix. During this call, we transform the incoming IBinder
		 * into an instance of the IDynamixFacade, which is used to call Dynamix methods.
		 */

		public void onServiceConnected(ComponentName name, IBinder service) 
		{
			// Add ourselves as a Dynamix listener
			try 
			{
				Log.i(TAG, "A1 - Dynamix is connected!");
				// Create a Dynamix Facade using the incoming IBinder
				dynamix = IDynamixFacade.Stub.asInterface(service);
				// Create a Dynamix listener using the callback
				dynamix.addDynamixListener(dynamixCallback);
			} 
			catch (Exception e) 
			{
				Log.w(TAG, e);
			}
		}

		/*
		 * Indicates that a previously connected IDynamixFacade has been disconnected from Dynamix. This typically means
		 * that Dynamix has crashed or been shut down by Android to conserve resources. In this case,
		 * 'onServiceConnected' will be called again automatically once Dynamix boots again.
		 */

		public void onServiceDisconnected(ComponentName name)
		{
			Log.i(TAG, "A1 - Dynamix is disconnected!");
			dynamix = null;
		}
	};
	
	/*
	 * Implementation of the IDynamixListener interface. For details on the IDynamixListener interface, see the Dynamix
	 * developer website.
	 */
	private static IDynamixListener dynamixCallback = new IDynamixListener.Stub() 
	{

		@Override
		public void onDynamixListenerAdded(String listenerId) throws RemoteException {
			Log.i(TAG, "A1 - onDynamixListenerAdded for listenerId: " + listenerId);
			// Open a Dynamix Session if it's not already opened
			if (dynamix != null) {
				if (!dynamix.isSessionOpen())
				{
					dynamix.openSession();
				}
				else
				{
					registerForContextTypes();
				}
					
			} 
			else
			{
				Log.i(TAG, "dynamix already connected");
			}
		}

		@Override
		public void onDynamixListenerRemoved() throws RemoteException 
		{
			Log.i(TAG, "A1 - onDynamixListenerRemoved");
		}

		@Override
		public void onSessionOpened(String sessionId) throws RemoteException 
		{
			Log.i(TAG, "A1 - onSessionOpened");
			registerForContextTypes();
		}

		@Override
		public void onSessionClosed() throws RemoteException 
		{
			Log.i(TAG, "A1 - onSessionClosed");
		}

		@Override
		public void onAwaitingSecurityAuthorization() throws RemoteException 
		{
			Log.i(TAG, "A1 - onAwaitingSecurityAuthorization");
		}

		@Override
		public void onSecurityAuthorizationGranted() throws RemoteException 
		{
			Log.i(TAG, "A1 - onSecurityAuthorizationGranted");
		}

		@Override
		public void onSecurityAuthorizationRevoked() throws RemoteException 
		{
			Log.w(TAG, "A1 - onSecurityAuthorizationRevoked");
		}

		@Override
		public void onContextSupportAdded(ContextSupportInfo supportInfo) throws RemoteException 
		{
			Log.i(TAG, "A1 - onContextSupportAdded for " + supportInfo.getContextType() + " using plugin "+ supportInfo.getPlugin() + " | id was: " + supportInfo.getSupportId());
			ContextType ct= allContextTypes.get(supportInfo.getContextType());
			ct.contextsupportadded();
		}

		@Override
		public void onContextSupportRemoved(ContextSupportInfo supportInfo) throws RemoteException 
		{
			Log.i(TAG, "A1 - onContextSupportRemoved for " + supportInfo.getSupportId());
			ContextType ct= allContextTypes.get(supportInfo.getContextType());
			ct.contextsupportremoved();			
		}

		@Override
		public void onContextTypeNotSupported(String contextType) throws RemoteException 
		{
			Log.i(TAG, "A1 - onContextTypeNotSupported for " + contextType);
		}

		@Override
		public void onInstallingContextSupport(ContextPluginInformation plug, String contextType) throws RemoteException 
		{
			Log.i(TAG, "A1 - onInstallingContextSupport: plugin = " + plug + " | Context Type = " + contextType);
		}
		
		@Override
		public void onContextPluginInstallProgress(ContextPluginInformation plug, int percentComplete) throws RemoteException 
		{
			Log.i(TAG, "A1 - onContextPluginInstallProgress for " + plug + " with % " + percentComplete);			
		}

		@Override
		public void onInstallingContextPlugin(ContextPluginInformation plug) throws RemoteException 
		{
			Log.i(TAG, "A1 - onInstallingContextPlugin: plugin = " + plug);
		}

		@Override
		public void onContextPluginInstalled(ContextPluginInformation plug) throws RemoteException 
		{
			Log.i(TAG, "A1 - onContextPluginInstalled for " + plug);
		}

		@Override
		public void onContextPluginUninstalled(ContextPluginInformation plug) throws RemoteException 
		{
			Log.i(TAG, "A1 - onContextPluginUninstalled for " + plug);
		}

		@Override
		public void onContextPluginInstallFailed(ContextPluginInformation plug, String message) throws RemoteException 
		{
			Log.i(TAG, "A1 - onContextPluginInstallFailed for " + plug + " with message: " + message);
		}

		@Override
		public void onContextEvent(ContextEvent event) throws RemoteException 
		{
			/*
			 * Log some information about the incoming event
			 */
			Log.i(TAG, "A1 - onContextEvent received from plugin: " + event.getEventSource());
			Log.i(TAG, "A1 - -------------------");
			Log.i(TAG, "A1 - Event context type: " + event.getContextType());
			Log.i(TAG, "A1 - Event timestamp " + event.getTimeStamp());
			if (event.expires())
			{
				Log.i(TAG, "A! - Event expires: "+event.expires());
				Log.i(TAG, "A1 - Event expires at " + event.getExpireTime());
			}
			else
			{
				Log.i(TAG, "A1 - Event does not expire");
			}
			if (event.hasIContextInfo()) 
			{
				Log.i(TAG, "A1 - Event contains native IContextInfo: " + event.getIContextInfo());
			}
			else
			{
				Log.i(TAG, "A1 - NO CONTEXT INFO ");
			}
			for (String format : event.getStringRepresentationFormats()) 
			{
				Log.i(TAG,
						"Event string-based format: " + format + " contained data: "
								+ event.getStringRepresentation(format));
				
			}
			ContextType type = allContextTypes.get(event.getContextType());
			type.setCurrentEvent(event);

		}

		@Override
		public void onContextRequestFailed(String requestId, String errorMessage, int errorCode) throws RemoteException 
		{
			Log.w(TAG, "A1 - onContextRequestFailed for requestId " + requestId + " with error message: " + errorMessage);
		}

		@Override
		public void onContextPluginDiscoveryStarted() throws RemoteException 
		{
			Log.i(TAG, "A1 - onContextPluginDiscoveryStarted");
		}

		@Override
		public void onContextPluginDiscoveryFinished(List<ContextPluginInformation> discoveredPlugins) throws RemoteException 
		{
			Log.i(TAG, "A1 - onContextPluginDiscoveryFinished");
		}

		@Override
		public void onDynamixFrameworkActive() throws RemoteException 
		{
			Log.i(TAG, "A1 - onDynamixFrameworkActive");
		}

		@Override
		public void onDynamixFrameworkInactive() throws RemoteException 
		{
			Log.i(TAG, "A1 - onDynamixFrameworkInactive");
		}

		@Override
		public void onContextPluginError(ContextPluginInformation plug, String message) throws RemoteException 
		{
			Log.i(TAG, "A1 - onContextPluginError for " + plug + " with message " + message);
		}

		@Override
		public void onContextPluginDisabled(ContextPluginInformation arg0)
				throws RemoteException 
		{
			// TODO Auto-generated method stub
			
		}

		@Override
		public void onContextPluginEnabled(ContextPluginInformation arg0)
				throws RemoteException {
			// TODO Auto-generated method stub
			
		}
	};


	private static void registerForContextTypes()
	{
		//nope...
	}
	
	public static ConcurrentHashMap<String, ContextType> updateContextTypes()
	{
		Log.d(TAG, "getallcontexttypes");
		if (dynamix != null) 
		{
			try 
			{
				//Log.d(TAG, "try");
				if(dynamix.isSessionOpen())
				{
					//Log.d(TAG, "dynamix session open");
					ContextPluginInformationResult d = dynamix.getAllContextPluginInformation();
					//Log.d(TAG, "message "+d.getMessage());
					List<ContextPluginInformation> plugininformationlist = d.getContextPluginInformation();
					//Log.d(TAG, ""+plugininformationlist.size());
					Iterator<ContextPluginInformation> infoit = plugininformationlist.iterator();
					while(infoit.hasNext())
					{
						ContextPluginInformation plugininfo = infoit.next();
						ContextPlugin p = new ContextPlugin(plugininfo.getPluginId(), plugininfo.getPluginName(), plugininfo.getContextPluginType().toString());
						List<String> contextytpeslist = plugininfo.getSupportedContextTypes();
						Iterator<String> typeit = contextytpeslist.iterator();
						while(typeit.hasNext())
						{
							String contextytpe = typeit.next();
							//Log.d(TAG, ">"+ contextytpe);
							if(!contextytpe.endsWith(".man"))
							{
								if(!allContextTypes.containsKey(contextytpe))
								{
									//Log.d(TAG, "new type");
									StringTokenizer tk = new StringTokenizer(contextytpe, ".");
									String fname=contextytpe;
									while(tk.hasMoreTokens())
									{
										fname=tk.nextToken();
									}
									ContextType ct = new ContextType(contextytpe, fname, "");
									//Log.d(TAG, plugininfo.getPluginName()+" ("+plugininfo.getPluginId()+")");
									ct.addPlugIn(p);
									allContextTypes.put(contextytpe, ct);
								}
								else
								{
									//Log.d(TAG, "old Type, new Plugin");
									//Log.d(TAG, plugininfo.getPluginName()+" ("+plugininfo.getPluginId()+")");
									allContextTypes.get(contextytpe).addPlugIn(p);
								}
							}
						}
					}
				} 
				else
				{
					Log.d(TAG, "dynamix session not open");
				}
			}
			catch (RemoteException e) 
			{
				// TODO Auto-generated catch block
				e.printStackTrace();
			}
		}
		else
		{
			Log.d(TAG, "dynamix=null");
			if(ctx!=null)
			{
				ctx.dynamixconnectandbind();
			}
		}
		//Log.d(TAG, "proove="+ct.getName());
		return allContextTypes;
	}
	
	public static ConcurrentHashMap<String, ContextType> getContextTypes()
	{
		return allContextTypes;
	}
	
	public static void subscribeToContext(ContextType contexttype)
	{
		if (dynamix != null) 
		{
			try 
			{
				Log.d(TAG, "try to subscribe");
				if(dynamix.isSessionOpen())
				{
					Log.d(TAG, "dynamix session is open "+contexttype.getName());
					dynamix.addContextSupport(dynamixCallback, contexttype.getName());
				}
			}
			catch (RemoteException e) 
			{
				// TODO Auto-generated catch block
				e.printStackTrace();
			}
		}
		else
		{
			Log.d(TAG, "dynamix=null");
			if(ctx!=null)
			{
				ctx.dynamixconnectandbind();
			}
		}
	}
	
	public static void unsubscribeToContext(ContextType contexttype)
	{
		if (dynamix != null) 
		{
			try 
			{
				Log.d(TAG, "try to unsubscrube from "+contexttype.getName());
				if(dynamix.isSessionOpen())
				{
					Log.d(TAG, "dynamix session is open");
					dynamix.removeContextSupportForContextType(dynamixCallback, contexttype.getName());
					
				}
			}
			catch (RemoteException e) 
			{
				// TODO Auto-generated catch block
				Log.e(TAG, "error on unsubscribe");
				e.printStackTrace();
			}
		}
		else
		{
			contexttype.contextsupportremoved();	
			Log.d(TAG, "dynamix=null");
			if(ctx!=null)
			{
				ctx.dynamixconnectandbind();
			}
		}
	}
	
	public static void requestContext(ContextType contexttype, ContextPlugin plugin)
	{
		Log.d(TAG, "DynamixConnectionService, request");
		try 
		{
			Log.d(TAG, "try");
			Log.d(TAG, "dynamixcallback with "+plugin.getId()+" for "+contexttype.getName());
			dynamix.contextRequest(dynamixCallback, plugin.getId(), contexttype.getName());
		} 
		catch (RemoteException e) 
		{
			// TODO Auto-generated catch block
			Log.e(TAG, "erro on context request");
			e.printStackTrace();
		}
	}
	
	public static void requestConfiguiredContext(ContextType contexttype, ContextPlugin plugin, Bundle scanConfig)
	{
		try 
		{
			String id = plugin.getId();
			Log.d(TAG, "configured dynamixcallback with "+id+" for "+contexttype.getName()+ " and a bundle.");
			dynamix.configuredContextRequest(dynamixCallback, id, contexttype.getName(), scanConfig);
		} 
		catch (RemoteException e) 
		{
			// TODO Auto-generated catch block
			Log.e(TAG, "requesting configured context triggered an exception");
			e.printStackTrace();
		}
	}

	public static boolean isConnected() 
	{
		if (dynamix != null) 
		{
			try 
			{
				return dynamix.isSessionOpen();
			} 
			catch (RemoteException e) 
			{
				// TODO Auto-generated catch block
				e.printStackTrace();
			}
		}
		else
		{			
			if(ctx!=null)
			{
				ctx.dynamixconnectandbind();
			}
		}
		return false;
	}
}

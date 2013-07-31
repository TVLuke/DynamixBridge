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

import java.util.ArrayList;
import java.util.Date;

import org.ambientdynamix.api.application.ContextEvent;


import de.uniluebeck.itm.coapserver.CoapServerManager;
import de.uniluebeck.itm.coapserver.NotObservableDynamixWebservice;
import de.uniluebeck.itm.coapserver.ObservableDynamixWebservice;

import android.os.Bundle;
import android.util.Log;

public class ContextType
{
	private final String TAG ="SSPBridge";
	
	private ArrayList<ContextPlugin> plugins = new ArrayList<ContextPlugin>();
	private String name;
	private String userFriendlyName;
	private String description;
	private ContextEvent currentEvent=null;
	private ArrayList<ObservableDynamixWebservice> observableservices = new ArrayList<ObservableDynamixWebservice>();
	private ArrayList<NotObservableDynamixWebservice> notobservableservices = new ArrayList<NotObservableDynamixWebservice>();
	private ContextType manType;
	private int updateIntervall=10000;
	private ContextTypeStatus status= ContextTypeStatus.INACTIVE;
	
	
	private static enum ContextTypeStatus 
	{
	    INACTIVE, WAITINGFORSUPPORT, SUPPORTED, ACTIVE
	}
	
	public ContextType(String name, String userFriendllyName, String description)
	{
		Log.d(TAG, "creating ContextType"+name);
		this.name=name;
		this.userFriendlyName=userFriendllyName;
		this.description=description;
		if(!name.endsWith(".man"))
		{
			manType=new ContextType(name+".man", "", "");
		}
		
	}
	
	public void addPlugIn(ContextPlugin plugIn)
	{
		
		if(newPlugin(plugIn))
		{
			Log.d(TAG, "add Plugin "+plugIn.getName()+" to ContextType "+ name);
			plugins.add(plugIn);
		}
		if(!name.endsWith(".man"))
		{
			manType.addPlugIn(plugIn);
		}
	}
	
	public ArrayList<ContextPlugin> getPluginList()
	{
		return plugins;
	}
	
	public String getName()
	{
		return name;
	}
	
	public String getUserFriendlyName()
	{
		return userFriendlyName;
	}
	
	public String getDescription()
	{
		return description;
	}
	
	public ContextEvent getCurrentEvent()
	{
		return currentEvent;
	}
	
	public void setCurrentEvent(ContextEvent event)
	{
		Log.d(TAG, "update for "+name);
		currentEvent = event;
		for(int i=0; i<observableservices.size(); i++)
		{
			Log.d(TAG, "update observable service");
			observableservices.get(i).setResourceStatus(event);
		}
		for(int i=0; i<notobservableservices.size(); i++)
		{
			Log.d(TAG, "update not observable service");
			notobservableservices.get(i).setResourceStatus(event);
		}
	}

	public void registerForUpdates(ObservableDynamixWebservice service) 
	{
		if(service!=null)
		{
			observableservices.add(service);
			if(currentEvent!=null)
			{
				service.setResourceStatus(currentEvent);
			}
		}
		else
		{
			Log.e(TAG, "The service is null, this makes no sense.");
		}
	}
	
	public void registerForUpdates(	NotObservableDynamixWebservice service) 
	{
		notobservableservices.add(service);
		if(currentEvent!=null)
		{
			service.setResourceStatus(currentEvent);
		}
	}
	
	public void requestContextUpdate()
	{
		Log.d(TAG, "request update from...");
		for(int i=0; i<plugins.size(); i++)
		{
			if(plugins.get(i).isActive() && !plugins.get(i).isBlacklisted())
			{
				Log.d(TAG, ">"+plugins.get(i).getId());
				DynamixConnectionService.requestContext(this, plugins.get(i));
			}
		}
	}
	
	public void requestContextUpdateIfNeeded()
	{
		Log.d(TAG, "request context update if needed");
		Date d = new Date();
		if(currentEvent!= null)
		{
			if(currentEvent.getExpireTime().before(d) && currentEvent.expires())
			{
				Log.d(TAG, "it is needed.");
				requestContextUpdate();
			}
		}
	}

	public void activatePlugins() 
	{
		for(ContextPlugin plugin: plugins)
		{
			if(!plugin.isBlacklisted())
			{
				plugin.activate();
			}
		}
	}
	
	public void deactivatePlugins()
	{
		for(ContextPlugin plugin: plugins)
		{
			plugin.deactivate();
		}
	}
	
	private boolean newPlugin(ContextPlugin p)
	{
		for(ContextPlugin plugin: plugins)
		{
			if(plugin.getId().equals(p.getId()))
			{
				return false;
			}
		}
		return true;
	}

	public void requestConfiguredContextUpdate(Bundle scanConfig) 
	{
		Log.d(TAG, "request configured update from...");
		for(int i=0; i<plugins.size(); i++)
		{
			if(plugins.get(i).isActive() && !plugins.get(i).isBlacklisted())
			{
				Log.d(TAG, ">"+plugins.get(i).getId());
				DynamixConnectionService.requestConfiguiredContext(this, plugins.get(i), scanConfig);
				
			}
		}
	}
	
	public boolean active()
	{
		return status.equals(ContextTypeStatus.ACTIVE);
	}
	
	public void activate(int port, int updateInt)
	{
		if(status.equals(ContextTypeStatus.SUPPORTED))
		{
			status=ContextTypeStatus.ACTIVE;
			updateIntervall=updateInt;
			activatePlugins();
		}
	}
	
	public void deactivate()
	{
		if(status.equals(ContextTypeStatus.ACTIVE))
		{
			status=ContextTypeStatus.SUPPORTED;
		}
		else if(status.equals(ContextTypeStatus.WAITINGFORSUPPORT))
		{
			
		}
		else
		{
			status=ContextTypeStatus.INACTIVE;
		}
		updateIntervall=10000;
		observableservices.clear();
		notobservableservices.clear();
	}
	
	public ContextType getManType()
	{
		return manType;
	}

	public int getUpdateIntervall() 
	{
		return updateIntervall;
	}

	public void contextsupportadded() 
	{
		if(status.equals(ContextTypeStatus.WAITINGFORSUPPORT))
		{
			status=ContextTypeStatus.SUPPORTED;
			CoapServerManager.addService(this, getUpdateIntervall());
		}
	}

	public void contextsupportremoved() 
	{
		CoapServerManager.removeService(this);
		status = ContextTypeStatus.INACTIVE;
	}
	
	public boolean contextSupported()
	{
		if(status.equals(ContextTypeStatus.SUPPORTED) || status.equals(ContextTypeStatus.ACTIVE))
		{
			return true;
		}
		return false;
	}

	public void subscribe() 
	{
		if(status.equals(ContextTypeStatus.INACTIVE))
		{
			DynamixConnectionService.subscribeToContext(this);
			status=ContextTypeStatus.WAITINGFORSUPPORT;
		}
	}
	
	public void unsubscribe()
	{
		if(status.equals(ContextTypeStatus.SUPPORTED) || status.equals(ContextTypeStatus.ACTIVE))
		{
			deactivate();
			DynamixConnectionService.unsubscribeToContext(this);
			status=ContextTypeStatus.INACTIVE;
		}
	}
	
	public boolean isWaitingForSubscription()
	{
		return status.equals(ContextTypeStatus.WAITINGFORSUPPORT);
	}
	
}

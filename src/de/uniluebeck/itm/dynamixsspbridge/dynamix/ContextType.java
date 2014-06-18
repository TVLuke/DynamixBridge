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


import de.uniluebeck.itm.coapserver.ObservableDynamixWebservice;
import de.uniluebeck.itm.dynamixsspbridge.accesscontrol.Tolken;
import de.uniluebeck.itm.dynamixsspbridge.core.ManagerManager;
import de.uniluebeck.itm.dynamixsspbridge.support.Constants;
import de.uniluebeck.itm.httpserver.HTTPDynamixControler;

import android.os.Bundle;
import android.util.Log;

public class ContextType
{
	private final String TAG ="SSPBridge";
	
	private ArrayList<ContextPlugin> plugins = new ArrayList<ContextPlugin>();
	private String name;
	private String userFriendlyName;
	private String description;
	private String shortDescription="";
	private ContextEvent currentEvent=null;
	private ContextEvent previousEvent=null;
	private ArrayList<ObservableDynamixWebservice> observableservices = new ArrayList<ObservableDynamixWebservice>();
	private ArrayList<ObservableDynamixWebservice> notobservableservices = new ArrayList<ObservableDynamixWebservice>();
	private ArrayList<HTTPDynamixControler> httpContextTypeControler = new ArrayList<HTTPDynamixControler>();
	private ContextType manType;
	private int updateIntervall=10000;
	private ContextTypeStatus status= ContextTypeStatus.INACTIVE;
	private boolean deprecated=false;
	private String redirect="";
	private boolean httpactive=false;
	private boolean coapactive=false;
	private boolean ispublic = true;
	private ArrayList<Tolken> alowed = new ArrayList<Tolken>();
	
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
	
	public ContextEvent getPreviousEvent()
	{
		return previousEvent;
	}
	
	public boolean isPublic()
	{
		return ispublic;
	}
	
	public void setCurrentEvent(ContextEvent event)
	{
		Log.d(TAG, "update for "+name);
		previousEvent = currentEvent;
		currentEvent = event;
		for(int i=0; i<observableservices.size(); i++)
		{
			Log.d(TAG, "update observable service");
			observableservices.get(i).setResourceStatus(event, 10);
		}
		for(int i=0; i<notobservableservices.size(); i++)
		{
			Log.d(TAG, "update not observable service");
			notobservableservices.get(i).setResourceStatus(event, 10);
		}
	}

	public void registerForUpdates(ObservableDynamixWebservice service) 
	{
		if(service!=null)
		{
			observableservices.add(service);
			if(currentEvent!=null)
			{
				service.setResourceStatus(currentEvent, 10);
			}
		}
		else
		{
			Log.e(TAG, "The service is null, this makes no sense.");
		}
	}
	
	public void registerForUpdates(HTTPDynamixControler service) 
	{
		httpContextTypeControler.add(service);		
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
		else
		{
			requestContextUpdate();
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
			if(port==Constants.COAP_PORT)
			{
				coapactive=true;
			}
			if(port==Constants.HTTP_PORT)
			{
				httpactive=true;
			}
			if(coapactive && httpactive)
			{
				status=ContextTypeStatus.ACTIVE;
				updateIntervall=updateInt;
				activatePlugins();
			}
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
			deactivate();
			DynamixConnectionService.unsubscribeToContext(this);
		}
		else
		{
			status=ContextTypeStatus.INACTIVE;
		}
		updateIntervall=10000;
		coapactive=false;
		httpactive=false;
		observableservices.clear();
		notobservableservices.clear();
		httpContextTypeControler.clear();
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
		Log.d(TAG, "ContextType: contextsupportadded()");
		if(status.equals(ContextTypeStatus.WAITINGFORSUPPORT))
		{
			Log.d(TAG, "was waiting, so...");
			status=ContextTypeStatus.SUPPORTED;
			ManagerManager.addService(this, getUpdateIntervall());
		}
	}

	public void contextsupportremoved() 
	{
		status = ContextTypeStatus.INACTIVE;
		coapactive=false;
		httpactive=false;
		ManagerManager.removeService(this);
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
		Log.d(TAG, "subscribe!!");
		if(status.equals(ContextTypeStatus.INACTIVE))
		{
			Log.d(TAG, "success");
			DynamixConnectionService.subscribeToContext(this);
			status=ContextTypeStatus.WAITINGFORSUPPORT;
		}
	}
	
	public void unsubscribe()
	{
		if(status.equals(ContextTypeStatus.SUPPORTED) || status.equals(ContextTypeStatus.ACTIVE) || status.equals(ContextTypeStatus.WAITINGFORSUPPORT))
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
	
	public boolean isDeprecated()
	{
		return deprecated;
	}
	
	public void setDeprecated(boolean b)
	{
		deprecated=b;
	}
	
	public void setRedirectID(String s)
	{
		redirect=s;
	}
	
	public String getRederictID()
	{
		return redirect;
	}
	
	public void setUserFriendlyName(String s)
	{
		userFriendlyName=s;
	}
	
	public String getShortDescription()
	{
		return shortDescription;
	}
	
	public void setShortDescription(String s)
	{
		shortDescription=s;
	}

}

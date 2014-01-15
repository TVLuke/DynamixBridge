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


package de.uniluebeck.itm.coapserver;

import java.util.Collections;
import java.util.Map;
import java.util.TreeMap;

import android.util.Log;

import java.lang.reflect.Field;
import java.util.concurrent.ConcurrentHashMap;

import de.uniluebeck.itm.ncoap.application.server.CoapServerApplication;
import de.uniluebeck.itm.ncoap.application.server.webservice.Webservice;
import de.uniluebeck.itm.ncoap.application.server.webservice.WellKnownCoreResource;

//based on https://bitbucket.org/natas_sevil/coapserver

public class DynamixCoapServer extends CoapServerApplication
{
	private final String TAG ="SSPBridge";
	public static boolean[] usedports= new boolean[10000];
	private int port=5683;

	public DynamixCoapServer()
	{
		//super(8885);
		super();
	}
	
	public DynamixCoapServer(int port)
	{
		super(port);
		this.port=port;
		usedports[port]=true;
	}
	
    public void handleRetransmissionTimout() 
    {
    }

    protected ConcurrentHashMap<String, Webservice> getRegisteredServices() 
    {
        try 
        {
            Field privateStringField = getClass().getDeclaredField("registeredServices");
            return (ConcurrentHashMap<String, Webservice>) privateStringField.get(this);
        } 
        catch (NoSuchFieldException e) 
        {
            e.printStackTrace();
        } 
        catch (IllegalAccessException e) 
        {
            e.printStackTrace();
        }
        return new ConcurrentHashMap<String, Webservice>();
    }

    public void refreshRootService() 
    {
        removeService("/.well-known/core");
        registerService(new WellKnownCoreResource(getRegisteredServices()));
    }

	public int getServerPort() 
	{
		return port;
	}

}

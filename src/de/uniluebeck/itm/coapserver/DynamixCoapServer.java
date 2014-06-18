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
import java.util.HashMap;
import java.util.Map;
import java.util.TreeMap;

import android.util.Log;

import java.lang.reflect.Field;
import java.util.concurrent.ConcurrentHashMap;

import de.uniluebeck.itm.dynamixsspbridge.support.Constants;
import de.uniluebeck.itm.ncoap.application.server.CoapServerApplication;
import de.uniluebeck.itm.ncoap.application.server.webservice.Webservice;
import de.uniluebeck.itm.ncoap.application.server.webservice.WellKnownCoreResource;

//based on https://bitbucket.org/natas_sevil/coapserver

public class DynamixCoapServer extends CoapServerApplication
{

	public DynamixCoapServer()
	{
		super(Constants.COAP_PORT);
	}
	
    public void handleRetransmissionTimout() 
    {
    }

    protected HashMap<String, Webservice> getRegisteredServices()
    {
        try 
        {
            Field privateStringField = getClass().getDeclaredField("registeredServices");
            return (HashMap<String, Webservice>) privateStringField.get(this);
        } 
        catch (NoSuchFieldException e) 
        {
            e.printStackTrace();
        } 
        catch (IllegalAccessException e) 
        {
            e.printStackTrace();
        }
        return new HashMap<String, Webservice>();
    }

    public void refreshRootService() 
    {
        //TODO: The API seems to have changed and removeSerivice is no longer allowed...
        registerService(new WellKnownCoreResource(getRegisteredServices()));
    }

	public int getServerPort()
	{
        //why does a server application not have any method to return the port...?
		return Constants.COAP_PORT;
	}
}

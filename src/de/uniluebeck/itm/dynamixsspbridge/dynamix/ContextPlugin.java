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

import android.content.SharedPreferences;

public class ContextPlugin {

	String id;
	String name;
	String type;
	boolean active=false;
	boolean blacklisted=false;
	
	public ContextPlugin(String id, String name, String type)
	{
		this.id=id;
		this.name=name;
		this.type=type;
	}
	
	public String getId()
	{
		return id;
	}
	
	public String getName()
	{
		return name;
	}
	
	public String getType()
	{
		return type;
	}
	
	public boolean isActive()
	{
		return active;
	}
	
	public void activate()
	{
		active=true;
	}
	
	public void deactivate()
	{
		active=false;
	}

	public boolean isBlacklisted() 
	{
		return blacklisted;
	}
	
	public void setBlacklisted(boolean b)
	{
		blacklisted = b;
	}
	
}

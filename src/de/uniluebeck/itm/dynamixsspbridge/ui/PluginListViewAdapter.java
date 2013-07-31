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

import de.uniluebeck.itm.dynamixsspbridge.R;
import de.uniluebeck.itm.dynamixsspbridge.dynamix.ContextPlugin;
import de.uniluebeck.itm.dynamixsspbridge.support.Constants;

import android.content.Context;
import android.content.SharedPreferences;
import android.content.SharedPreferences.Editor;
import android.graphics.Paint;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.BaseAdapter;
import android.widget.CheckBox;
import android.widget.TextView;

public class PluginListViewAdapter  extends BaseAdapter 
{
	ArrayList<ContextPlugin> plugins;
	private static final String PREFS = Constants.PREFS;
	LayoutInflater inflater;
	Context context;
	String contexttype;
	
	public PluginListViewAdapter(Context context, ArrayList<ContextPlugin> plugins, String contexttype)
	{
		this.context=context;
		this.plugins=plugins;
		this.contexttype = contexttype;
	}
	
	@Override
	public int getCount() 
	{
		return plugins.size();
	}

	@Override
	public Object getItem(int position) 
	{
		return plugins.get(position);
	}

	@Override
	public long getItemId(int arg0) 
	{
		return 0;
	}

	@Override
	public View getView(int position, View convertView, ViewGroup parent) 
	{

		inflater = (LayoutInflater) context
                .getSystemService(Context.LAYOUT_INFLATER_SERVICE);
		View itemView = inflater.inflate(R.layout.listview_plugin_item, parent, false);
		CheckBox name = (CheckBox) itemView.findViewById(R.id.checkBox1);
		name.setChecked(plugins.get(position).isActive());
		name.setClickable(false);
		name.setText(plugins.get(position).getName()+" ("+plugins.get(position).getType()+")");
		if(plugins.get(position).isBlacklisted())
		{
			name.setChecked(false);
			name.setPaintFlags(name.getPaintFlags() | Paint.STRIKE_THRU_TEXT_FLAG);
			plugins.get(position).isBlacklisted();
		}
		TextView nametv = (TextView) itemView.findViewById(R.id.name);
		nametv.setText(plugins.get(position).getId());
		return itemView;
	}
	
	public void clickItem(int position)
	{
		SharedPreferences prefs = context.getSharedPreferences(PREFS, 0);
		if(plugins.get(position).isActive())
		{
			plugins.get(position).deactivate();
		}
		else
		{
			plugins.get(position).activate();
		}
		
	}

	public void longClickItem(int position) 
	{
		final SharedPreferences prefs = context.getSharedPreferences(PREFS, 0);
		if(prefs.getBoolean("blackout_"+plugins.get(position).getName(), false))
		{
			Editor edit = prefs.edit();
			edit.putBoolean("blackout_"+plugins.get(position).getName(), false);
			plugins.get(position).setBlacklisted(false);
			edit.commit();
		}
		else
		{
			Editor edit = prefs.edit();
			edit.putBoolean("blackout_"+plugins.get(position).getName(), true);
			plugins.get(position).setBlacklisted(true);
			edit.commit();
		}
	}

}

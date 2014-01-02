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

import java.util.HashMap;
import java.util.concurrent.ConcurrentHashMap;

import de.uniluebeck.itm.dynamixsspbridge.R;
import de.uniluebeck.itm.dynamixsspbridge.dynamix.ContextType;
import de.uniluebeck.itm.dynamixsspbridge.support.Constants;

import android.content.Context;
import android.content.SharedPreferences;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.BaseAdapter;
import android.widget.ImageView;
import android.widget.TextView;

//masive help from http://www.androidbegin.com/tutorial/android-custom-listview-texts-and-images-tutorial/

public class ContextTypeListViewAdapter extends BaseAdapter 
{

	ConcurrentHashMap<String, ContextType> contexttypes;
	private static final String PREFS = Constants.PREFS;
	LayoutInflater inflater;
	Context context;
	
	public ContextTypeListViewAdapter(Context context, ConcurrentHashMap<String, ContextType> contexttypes)
	{
		this.contexttypes=contexttypes;
		this.context=context;
		
	}
	
	@Override
	public int getCount() 
	{
		return contexttypes.size();
	}

	@Override
	public ContextType getItem(int position) 
	{
		return contexttypes.get((String) contexttypes.keySet().toArray()[position]);
	}

	@Override
	public long getItemId(int position) 
	{
		return 0;
	}

	@Override
	public View getView(int position, View convertView, ViewGroup parent) 
	{
		inflater = (LayoutInflater) context.getSystemService(Context.LAYOUT_INFLATER_SERVICE);
		View itemView = inflater.inflate(R.layout.listview_contextevent_item, parent, false);
		try
		{
			TextView friendlyname = (TextView) itemView.findViewById(R.id.friendlyname);
			ContextType ct = contexttypes.get((String) contexttypes.keySet().toArray()[position]);
			friendlyname.setText(ct.getUserFriendlyName());
			TextView name = (TextView) itemView.findViewById(R.id.name);
			name.setText(ct.getName());
			final SharedPreferences prefs = context.getSharedPreferences(PREFS, 0);
			//boolean ischecked= contexttypes.get((String) contexttypes.keySet().toArray()[position]).active();
			if(ct.active())
			{
				ImageView pic = (ImageView) itemView.findViewById(R.id.imageView1);
				pic.setImageResource(R.drawable.active);
			}
			else if(ct.isWaitingForSubscription())
			{
				ImageView pic = (ImageView) itemView.findViewById(R.id.imageView1);
				pic.setImageResource(R.drawable.waitingforsupport);
			}
			else if(ct.contextSupported())
			{
				ImageView pic = (ImageView) itemView.findViewById(R.id.imageView1);
				pic.setImageResource(R.drawable.supported);
			}
			else
			{
				ImageView pic = (ImageView) itemView.findViewById(R.id.imageView1);
				pic.setImageResource(R.drawable.inactive);
			}
			if(ct.isPublic())
			{
				ImageView pic = (ImageView) itemView.findViewById(R.id.imageView2);
				pic.setVisibility(View.GONE);
			}
			else
			{
				ImageView pic = (ImageView) itemView.findViewById(R.id.imageView2);
				pic.setImageResource(R.drawable.key);
			}
		}
		catch(Exception e)
		{
			//sometimes the view throws strange stuff...
		}
		//TODO set Picture
		return itemView;
	}

}

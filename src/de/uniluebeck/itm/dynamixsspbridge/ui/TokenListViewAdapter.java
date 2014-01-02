package de.uniluebeck.itm.dynamixsspbridge.ui;

import de.uniluebeck.itm.dynamixsspbridge.R;
import de.uniluebeck.itm.dynamixsspbridge.support.Constants;
import android.content.Context;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.BaseAdapter;
import android.widget.TextView;

public class TokenListViewAdapter   extends BaseAdapter 
{

	private static final String PREFS = Constants.PREFS;
	LayoutInflater inflater;
	Context context;
	
	public TokenListViewAdapter()
	{
		
	}
	
	@Override
	public int getCount() {
		// TODO Auto-generated method stub
		return 0;
	}

	@Override
	public Object getItem(int arg0) {
		// TODO Auto-generated method stub
		return null;
	}

	@Override
	public long getItemId(int position) {
		return 0;
	}

	@Override
	public View getView(int position, View convertView, ViewGroup parent) {
		inflater = (LayoutInflater) context
                .getSystemService(Context.LAYOUT_INFLATER_SERVICE);
		View itemView = inflater.inflate(R.layout.listview_tolken_item, parent, false);
		TextView friendlyname = (TextView) itemView.findViewById(R.id.friendlyname);
		friendlyname.setText("olol");
		return itemView;
	}

}

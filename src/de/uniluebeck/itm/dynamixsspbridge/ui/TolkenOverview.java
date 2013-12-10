package de.uniluebeck.itm.dynamixsspbridge.ui;

import de.uniluebeck.itm.dynamixsspbridge.R;
import de.uniluebeck.itm.dynamixsspbridge.core.StartService;
import de.uniluebeck.itm.dynamixsspbridge.support.Constants;
import android.app.Activity;
import android.content.Context;
import android.content.Intent;
import android.os.Bundle;
import android.util.Log;

public class TolkenOverview  extends Activity
{
	private final String TAG ="SSPBridge";
	private static final String PREFS = Constants.PREFS;
	private static Context ctx;
	

	protected void onCreate(Bundle savedInstanceState) 
	{
		super.onCreate(savedInstanceState);
		setContentView(R.layout.tolkenoverview);
		startService(new Intent(this, StartService.class));
		Log.e(TAG, "onCreate()");
	}
}

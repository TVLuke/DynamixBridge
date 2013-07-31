/*
    Copyright 2012, Strategic Gains, Inc.

	Licensed under the Apache License, Version 2.0 (the "License");
	you may not use this file except in compliance with the License.
	You may obtain a copy of the License at

		http://www.apache.org/licenses/LICENSE-2.0

	Unless required by applicable law or agreed to in writing, software
	distributed under the License is distributed on an "AS IS" BASIS,
	WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
	See the License for the specific language governing permissions and
	limitations under the License.
*/

package de.uniluebeck.itm.htmlserver;

import android.util.Log;

import com.strategicgains.restexpress.response.DefaultResponseWrapper;
import com.strategicgains.restexpress.response.RawResponseWrapper;
import com.strategicgains.restexpress.response.ResponseProcessor;
import com.strategicgains.restexpress.response.ResponseWrapper;
import com.strategicgains.restexpress.serialization.SerializationProcessor;

/**
 * A factory to create ResponseProcessors for serialization and wrapping of responses.
 * 
 * @author toddf
 * @since May 15, 2012
 */
public class ResponseProcessors
{
	// SECTION: CONSTANTS

	private static final SerializationProcessor JSON_SERIALIZER = new JsonSerializationProcessor();
	private static final SerializationProcessor XML_SERIALIZER = new XmlSerializationProcessor();
	private static final ResponseWrapper RAW_WRAPPER = new RawResponseWrapper();
	private static final ResponseWrapper WRAPPING_WRAPPER = new DefaultResponseWrapper();

	private static String TAG ="SSPBridge";
	// SECTION: FACTORY

	public static ResponseProcessor json()
	{
		Log.d(TAG, "ResonseProcessor json");
		return new ResponseProcessor(JSON_SERIALIZER, RAW_WRAPPER);
	}

	public static ResponseProcessor wrappedJson()
	{
		return new ResponseProcessor(JSON_SERIALIZER, WRAPPING_WRAPPER);
	}

	public static ResponseProcessor xml()
	{
		Log.d(TAG, "ResonseProcessor xml");
		return new ResponseProcessor(XML_SERIALIZER, RAW_WRAPPER);
	}

	public static ResponseProcessor wrappedXml()
	{
		Log.d(TAG, "ResonseProcessor wrapped xml");
		return new ResponseProcessor(XML_SERIALIZER, WRAPPING_WRAPPER);
	}
}
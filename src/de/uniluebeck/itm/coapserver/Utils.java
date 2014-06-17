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

import android.content.Context;
import android.provider.Settings.Secure;
import com.google.common.collect.HashBiMap;
import de.uniluebeck.itm.ncoap.message.CoapRequest;
import de.uniluebeck.itm.ncoap.message.options.ContentFormat;
import de.uniluebeck.itm.ncoap.message.options.*;
import org.apache.http.conn.util.InetAddressUtils;

import java.io.UnsupportedEncodingException;
import java.net.InetAddress;
import java.net.NetworkInterface;
import java.net.URLDecoder;
import java.text.SimpleDateFormat;
import java.util.*;

public class Utils {

    /**
     * Comma separated List of IP adresses of available network interfaces
     * @param useIPv4
     * @return
     */
    public static String getIPAddress(boolean useIPv4) {

        String addresses = "";
        try {
            List<NetworkInterface> interfaces = Collections.list(NetworkInterface.getNetworkInterfaces());
            for (NetworkInterface intf : interfaces) {
                List<InetAddress> addrs = Collections.list(intf.getInetAddresses());
                for (InetAddress addr : addrs) {
                    if (!addr.isLoopbackAddress()) {
                        String sAddr = addr.getHostAddress().toUpperCase();
                        boolean isIPv4 = InetAddressUtils.isIPv4Address(sAddr);
                        if (useIPv4) {
                            if (isIPv4)
                                addresses += sAddr + ", ";
                        } else {
                            if (!isIPv4) {
                                int delim = sAddr.indexOf('%'); // drop ip6 port suffix
                                if(delim<0) addresses += sAddr + ", ";
                                else addresses += sAddr.substring(0, delim) + ", ";
                            }
                        }
                    }
                }
            }
        } catch (Exception ex) { } // for now eat exceptions
        if(addresses == null || addresses.length() <= 3) return "";
        return addresses.subSequence(0, addresses.length()-2).toString();
    }

    /**
     * Absolute URI of this request. For use as namespace for RDF XML responses
     * @param request CoAP request
     * @return absolute uri
     */
    public static String targetUri(CoapRequest request) {
        String path = request.getUriPath();//TODO: not sure if this is the right metjod... I don't get the new coap yet.
        String[] ipAddresses = Utils.getIPAddress(true).split(", ");
        if(ipAddresses.length > 0 && !ipAddresses[0].isEmpty()) {
            return "coap://"+ipAddresses[0] + path;
        }
        else return "coap://example.com"+path;
    }

    /**
     * Returns a unique device id, which is stable across restarts
     * @param context
     * @return
     */
    public static String deviceId(Context context) {
        return Secure.getString(context.getContentResolver(), Secure.ANDROID_ID);
    }

    private static TimeZone tz = TimeZone.getTimeZone("UTC");
    private static SimpleDateFormat simpleDateFormat = new SimpleDateFormat("yyyy-MM-dd'T'HH:mm'Z'");
    {
        simpleDateFormat.setTimeZone(tz);
    }


    public static final HashBiMap<String, Long> MEDIA_TYPE_MAP = HashBiMap.create();
    static 
    {
        MEDIA_TYPE_MAP.put("RDF/XML", ContentFormat.APP_RDF_XML);
        MEDIA_TYPE_MAP.put("TURTLE", ContentFormat.APP_TURTLE);
        MEDIA_TYPE_MAP.put("N3", ContentFormat.APP_N3);
    }

    public static final HashMap<Long,Long> COPPER_ACCEPT_MAP = new HashMap<Long, Long>();
    static 
    {
        COPPER_ACCEPT_MAP.put(43l, 201l);
        COPPER_ACCEPT_MAP.put(43l, 201l);
        COPPER_ACCEPT_MAP.put(43l, 201l);
    }
}
/****************************************************************************
Copyright (c) 2010-2014 cocos2d-x.org
Copyright (c) 2014-2016 Chukong Technologies Inc.
Copyright (c) 2017-2018 Xiamen Yaji Software Co., Ltd.

http://www.cocos2d-x.org

Permission is hereby granted, free of charge, to any person obtaining a copy
of this software and associated documentation files (the "Software"), to deal
in the Software without restriction, including without limitation the rights
to use, copy, modify, merge, publish, distribute, sublicense, and/or sell
copies of the Software, and to permit persons to whom the Software is
furnished to do so, subject to the following conditions:

The above copyright notice and this permission notice shall be included in
all copies or substantial portions of the Software.

THE SOFTWARE IS PROVIDED "AS IS", WITHOUT WARRANTY OF ANY KIND, EXPRESS OR
IMPLIED, INCLUDING BUT NOT LIMITED TO THE WARRANTIES OF MERCHANTABILITY,
FITNESS FOR A PARTICULAR PURPOSE AND NONINFRINGEMENT. IN NO EVENT SHALL THE
AUTHORS OR COPYRIGHT HOLDERS BE LIABLE FOR ANY CLAIM, DAMAGES OR OTHER
LIABILITY, WHETHER IN AN ACTION OF CONTRACT, TORT OR OTHERWISE, ARISING FROM,
OUT OF OR IN CONNECTION WITH THE SOFTWARE OR THE USE OR OTHER DEALINGS IN
THE SOFTWARE.
 ****************************************************************************/
package org.cocos2dx.lib;

import android.util.Log;

import java.io.BufferedInputStream;
import java.io.ByteArrayOutputStream;
import java.io.FileInputStream;
import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;
import java.net.HttpURLConnection;
import java.net.ProtocolException;
import java.net.URL;
import java.security.KeyStore;
import java.security.cert.Certificate;
import java.security.cert.CertificateFactory;
import java.security.cert.X509Certificate;
import java.text.ParseException;
import java.text.SimpleDateFormat;
import java.util.Calendar;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.Map.Entry;
import java.util.zip.GZIPInputStream;
import java.util.zip.InflaterInputStream;

import javax.net.ssl.HttpsURLConnection;
import javax.net.ssl.SSLContext;
import javax.net.ssl.TrustManagerFactory;

public class Cocos2dxHttpURLConnection
{
    private static String TAG = "Cocos2dxHttpURLConnection";
    private static final String POST_METHOD = "POST" ;
    private static final String PUT_METHOD = "PUT" ;

    static HttpURLConnection createHttpURLConnection(final String linkURL) {
        URL url;
        HttpURLConnection urlConnection;
        try {
            url = new URL(linkURL);
            urlConnection = (HttpURLConnection) url.openConnection();
            //Accept-Encoding
            urlConnection.setRequestProperty("Accept-Encoding", "identity");
            urlConnection.setDoInput(true);
        } catch (final Exception e) {
            e.printStackTrace();
            Log.e(TAG, "createHttpURLConnection:" + e.toString());
            return null;
        }

        return urlConnection;
    }

    static void setReadAndConnectTimeout(final HttpURLConnection urlConnection, final int readMiliseconds, final int connectMiliseconds) {
        urlConnection.setReadTimeout(readMiliseconds);
        urlConnection.setConnectTimeout(connectMiliseconds);
    }

    static void setRequestMethod(final HttpURLConnection urlConnection, final String method){
        try {
            urlConnection.setRequestMethod(method);
            if(method.equalsIgnoreCase(POST_METHOD) || method.equalsIgnoreCase(PUT_METHOD)) {
                urlConnection.setDoOutput(true);
            }
        } catch (final ProtocolException e) {
            Log.e(TAG, "setRequestMethod:" + e.toString());
        }

    }

    static void setVerifySSL(final HttpURLConnection urlConnection, final String sslFilename) {
        if(!(urlConnection instanceof HttpsURLConnection))
            return;


        final HttpsURLConnection httpsURLConnection = (HttpsURLConnection)urlConnection;

        try {
            InputStream caInput = null;
            String assetString = "@assets/";
            String cachesString = "@caches/";
            if (sslFilename.startsWith("/")) {
                caInput = new BufferedInputStream(new FileInputStream(sslFilename));
            }else if(sslFilename.startsWith(assetString)){
                String assetsfilenameString = sslFilename.substring(assetString.length());
                assetsfilenameString = Utils.mingameSourceJoinPath(SharedVisit.gameActivity, assetsfilenameString);
                FileInputStream fs = new FileInputStream(assetsfilenameString);
                caInput = new BufferedInputStream(fs);
            } else if (sslFilename.startsWith(cachesString)) {
                String filenameString = sslFilename.substring(cachesString.length());
                filenameString = Utils.mingameCacheJoinPath(SharedVisit.gameActivity, filenameString);
                FileInputStream fs = new FileInputStream(filenameString);
                caInput = new BufferedInputStream(fs);
            }

            final CertificateFactory cf = CertificateFactory.getInstance("X.509");
            Certificate ca;
            ca = cf.generateCertificate(caInput);
            System.out.println("ca=" + ((X509Certificate) ca).getSubjectDN());
            caInput.close();

            // Create a KeyStore containing our trusted CAs
            final String keyStoreType = KeyStore.getDefaultType();
            final KeyStore keyStore = KeyStore.getInstance(keyStoreType);
            keyStore.load(null, null);
            keyStore.setCertificateEntry("ca", ca);

            // Create a TrustManager that trusts the CAs in our KeyStore
            final String tmfAlgorithm = TrustManagerFactory.getDefaultAlgorithm();
            final TrustManagerFactory tmf = TrustManagerFactory.getInstance(tmfAlgorithm);
            tmf.init(keyStore);

            // Create an SSLContext that uses our TrustManager
            final SSLContext context = SSLContext.getInstance("TLS");
            context.init(null, tmf.getTrustManagers(), null);

            httpsURLConnection.setSSLSocketFactory(context.getSocketFactory());
        } catch (final Exception e) {
            e.printStackTrace();
            Log.e(TAG, "setVerifySSL:" + e.toString());
        }
    }

    //Add header
    static void addRequestHeader(final HttpURLConnection urlConnection, final String key, final String value) {
        urlConnection.setRequestProperty(key, value);
    }

    static int connect(final HttpURLConnection http) {
        int suc = 0;

        try {
            http.connect();
        } catch (final Exception e) {
            e.printStackTrace();
            Log.e(TAG, "connect" + e.toString());
            suc = 1;
        }

        return suc;
    }

    static void disconnect(final HttpURLConnection http) {
        http.disconnect();
    }

    static void sendRequest(final HttpURLConnection http, final byte[] byteArray) {
        try {
            final OutputStream out = http.getOutputStream();
            if(null !=  byteArray) {
                out.write(byteArray);
                out.flush();
            }
            out.close();
        } catch (final Exception e) {
            e.printStackTrace();
            Log.e(TAG, "sendRequest:" + e.toString());
        }
    }

    static String getResponseHeaders(final HttpURLConnection http) {
        final Map<String, List<String>> headers = http.getHeaderFields();
        if (null == headers) {
            return null;
        }

        String header = "";

        for (final Entry<String, List<String>> entry: headers.entrySet()) {
            final String key = entry.getKey();
            if (null == key) {
                header += listToString(entry.getValue(), ",") + "\n";
            } else {
                header += key + ":" + listToString(entry.getValue(), ",") + "\n";
            }
        }

        return header;
    }

    static String getResponseHeaderByIdx(final HttpURLConnection http, final int idx) {
        final Map<String, List<String>> headers = http.getHeaderFields();
        if (null == headers) {
            return null;
        }

        String header = null;

        int counter = 0;
        for (final Entry<String, List<String>> entry: headers.entrySet()) {
            if (counter == idx) {
                final String key = entry.getKey();
                if (null == key) {
                    header = listToString(entry.getValue(), ",") + "\n";
                } else {
                    header = key + ":" + listToString(entry.getValue(), ",") + "\n";
                }
                break;
            }
            counter++;
        }

        return header;
    }

    static String getResponseHeaderByKey(final HttpURLConnection http, final String key) {
        if (null == key) {
            return null;
        }

        final Map<String, List<String>> headers = http.getHeaderFields();
        if (null == headers) {
            return null;
        }

        String header = null;

        for (final Entry<String, List<String>> entry: headers.entrySet()) {
            if (key.equalsIgnoreCase(entry.getKey())) {
                if ("set-cookie".equalsIgnoreCase(key)) {
                    header = combinCookies(entry.getValue(), http.getURL().getHost());
                } else {
                    header = listToString(entry.getValue(), ",");
                }
                break;
            }
        }

        return header;
    }

    static int getResponseHeaderByKeyInt(final HttpURLConnection http, final String key) {
        final String value = http.getHeaderField(key);

        if (null == value) {
            return 0;
        } else {
            return Integer.parseInt(value);
        }
    }

    static byte[] getResponseContent(final HttpURLConnection http) {
        InputStream in;
        try {
            in = http.getInputStream();
            final String contentEncoding = http.getContentEncoding();
            if (contentEncoding != null) {
                if(contentEncoding.equalsIgnoreCase("gzip")){
                    in = new GZIPInputStream(http.getInputStream()); //reads 2 bytes to determine GZIP stream!
                }
                else if(contentEncoding.equalsIgnoreCase("deflate")){
                    in = new InflaterInputStream(http.getInputStream());
                }
            }
        } catch (final IOException e) {
            in = http.getErrorStream();
        } catch (final Exception e) {
            e.printStackTrace();
            Log.e(TAG, "1 getResponseContent: " + e.toString());
            return null;
        }

        try {
            final byte[] buffer = new byte[1024];
            int size   = 0;
            final ByteArrayOutputStream bytestream = new ByteArrayOutputStream();
            while((size = in.read(buffer, 0 , 1024)) != -1)
            {
                bytestream.write(buffer, 0, size);
            }
            final byte retbuffer[] = bytestream.toByteArray();
            bytestream.close();
            return retbuffer;
        } catch (final Exception e) {
            e.printStackTrace();
            Log.e(TAG, "2 getResponseContent:" + e.toString());
        }

        return null;
    }

    static int getResponseCode(final HttpURLConnection http) {
        int code = 0;
        try {
            code = http.getResponseCode();
        } catch (final Exception e) {
            e.printStackTrace();
            Log.e(TAG, "getResponseCode:" + e.toString());
        }
        return code;
    }

    static String getResponseMessage(final HttpURLConnection http) {
        String msg;
        try {
            msg = http.getResponseMessage();
        } catch (final Exception e) {
            e.printStackTrace();
            msg = e.toString();
            Log.e(TAG, "getResponseMessage: " + msg);
        }

        return msg;
    }

    public static String listToString(final List<String> list, final String strInterVal) {
        if (list == null) {
            return null;
        }
        final StringBuilder result = new StringBuilder();
        boolean flag = false;
        for (String str : list) {
            if (flag) {
                result.append(strInterVal);
            }
            if (null == str) {
                str = "";
            }
            result.append(str);
            flag = true;
        }
        return result.toString();
    }

    public static String combinCookies(final List<String> list, final String hostDomain) {
        final StringBuilder sbCookies = new StringBuilder();
        String domain    = hostDomain;
        final String tailmatch = "FALSE";
        String path      = "/";
        String secure    = "FALSE";
        String key = null;
        String value = null;
        String expires = null;
        for (final String str : list) {
            final String[] parts = str.split(";");
            for (final String part : parts) {
                final int firstIndex = part.indexOf("=");
                if (-1 == firstIndex)
                    continue;

                final String[] item =  {part.substring(0, firstIndex), part.substring(firstIndex + 1)};
                if ("expires".equalsIgnoreCase(item[0].trim())) {
                    expires = str2Seconds(item[1].trim());
                } else if("path".equalsIgnoreCase(item[0].trim())) {
                    path = item[1];
                } else if("secure".equalsIgnoreCase(item[0].trim())) {
                    secure = item[1];
                } else if("domain".equalsIgnoreCase(item[0].trim())) {
                    domain = item[1];
                } else if("version".equalsIgnoreCase(item[0].trim()) || "max-age".equalsIgnoreCase(item[0].trim())) {
                    //do nothing
                } else {
                    key = item[0];
                    value = item[1];
                }
            }

            if (null == domain) {
                domain = "none";
            }

            sbCookies.append(domain);
            sbCookies.append('\t');
            sbCookies.append(tailmatch);  //access
            sbCookies.append('\t');
            sbCookies.append(path);      //path
            sbCookies.append('\t');
            sbCookies.append(secure);    //secure
            sbCookies.append('\t');
            sbCookies.append(expires);   //expires
            sbCookies.append("\t");
            sbCookies.append(key);       //key
            sbCookies.append("\t");
            sbCookies.append(value);     //value
            sbCookies.append('\n');
        }

        return sbCookies.toString();
    }

    private static String str2Seconds(final String strTime) {
        final Calendar c = Calendar.getInstance();
        long milliseconds = 0;

        try {
            c.setTime(new SimpleDateFormat("EEE, dd-MMM-yy hh:mm:ss zzz", Locale.US).parse(strTime));
            milliseconds = c.getTimeInMillis() / 1000;
        } catch (final ParseException e) {
            Log.e(TAG, "str2Seconds: " + e.toString());
        }

        return Long.toString(milliseconds);
    }
}

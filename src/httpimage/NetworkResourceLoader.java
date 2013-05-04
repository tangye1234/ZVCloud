//  Copyright 2012 Zonghai Li. All rights reserved.
//
//  Redistribution and use in binary and source forms, with or without modification,
//  are permitted for any project, commercial or otherwise, provided that the
//  following conditions are met:
//  
//  Redistributions in binary form must display the copyright notice in the About
//  view, website, and/or documentation.
//  
//  Redistributions of source code must retain the copyright notice, this list of
//  conditions, and the following disclaimer.
//
//  THIS SOFTWARE IS PROVIDED "AS IS", WITHOUT WARRANTY OF ANY KIND, EXPRESS OR IMPLIED,
//  INCLUDING BUT NOT LIMITED TO THE WARRANTIES OF MERCHANTABILITY, FITNESS FOR A
//  PARTICULAR PURPOSE AND NONINFRINGEMENT OF THIRD PARTY RIGHTS. IN NO EVENT SHALL THE
//  AUTHORS OR COPYRIGHT HOLDERS BE LIABLE FOR ANY CLAIM, DAMAGES OR OTHER LIABILITY,
//  WHETHER IN AN ACTION OF CONTRACT, TORT OR OTHERWISE, ARISING FROM, OUT OF OR IN
//  CONNECTION WITH THE SOFTWARE OR THE USE OR OTHER DEALINGS IN THIS SOFTWARE.


package httpimage;

import java.io.IOException;
import org.apache.http.HttpResponse;
import org.apache.http.client.methods.HttpGet;
import org.apache.http.conn.params.ConnManagerParams;
import org.apache.http.params.BasicHttpParams;
import org.apache.http.params.HttpParams;

import com.zigvine.android.http.HttpManager;

import android.util.Log;


/**
 * resource loader using apache HTTP client. Support HTTP and HTTPS request.
 * 
 * @author zonghai@gmail.com
 */
public class NetworkResourceLoader {
    public static final String TAG = "NetworkResourceLoader";
    public static final boolean DEBUG = false;

    
    /**
     * Gets the input stream from a response entity. If the entity is gzipped then this will get a
     * stream over the uncompressed data.
     *
     * @param entity the entity whose content should be read
     * @return the input stream to read from
     * @throws IOException
     */
    public HttpResponse load (HttpGet httpGet) throws IOException{
        if (DEBUG) Log.d(TAG, "Requesting: " + httpGet.getURI().toASCIIString());
        httpGet.addHeader("Accept-Encoding", "gzip");
        
        HttpParams params = httpGet.getParams();
        if (params == null) {
        	params = new BasicHttpParams();
        }
        
        ConnManagerParams.setTimeout(params, 20000);
        return HttpManager.getImageHttpManager().nativeExecute(httpGet);

    }

}

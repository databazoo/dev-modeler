package com.databazoo.tools;

import java.io.IOException;
import java.net.InetSocketAddress;
import java.net.Proxy;
import java.net.ProxySelector;
import java.net.URI;
import java.net.URLEncoder;
import java.util.LinkedHashMap;
import java.util.Map;

import com.databazoo.components.UIConstants;
import com.github.markusbernhardt.proxy.ProxySearch;
import us.monoid.web.FormData;
import us.monoid.web.JSONResource;
import us.monoid.web.Resty;

/**
 * A simple REST client.
 * Basically, a wrapper for Resty with system proxy detection.
 */
public class RestClient {
    private static final String APP_REST_URL = UIConstants.getProperty("app.rest.url");

    private static RestClient instance;

    public static synchronized RestClient getInstance() {
        if(instance == null){
            instance = new RestClient();
        }
        return instance;
    }

    private final Resty resty = new Resty();

    private RestClient() {
        try {
            ProxySelector.setDefault(ProxySearch.getDefaultProxySearch().getProxySelector());
            ProxySelector proxySelector = ProxySelector.getDefault();
            if(proxySelector != null) {
                for (Proxy proxy : proxySelector.select(new URI("http://www.databazoo.com/"))) {
                    InetSocketAddress addr = (InetSocketAddress) proxy.address();
                    if (addr != null) {
                        Dbg.info("Setting proxy to: " + addr.getHostName() + ":" + addr.getPort());
                        new Resty.Proxy(addr.getHostName(), addr.getPort()).init(resty);
                    }
                }
            }
        } catch (Exception e) {
            Dbg.fixme("Proxy Selector failed", e);
        }
    }

    public JSONResource getJSON(String url, String... values) throws IOException {
        if(values.length % 2 != 0){
            throw new IllegalArgumentException("Provided values are not a key-pair list");
        }
        Map<String, String> valueMap = new LinkedHashMap<>();
        String key = null;
        for(int i=0; i < values.length; i++){
            if(i % 2 == 0){
                key = values[i];
            }else{
                valueMap.put(key, values[i]);
            }
        }
        return getJSON(url, valueMap);
    }

    public JSONResource getJSON(String url, Map<String, String> values) throws IOException {
        StringBuilder urlBuilder = new StringBuilder(url.startsWith("http") ? url : APP_REST_URL + url);
        char comma = '?';
        for(Map.Entry<String, String> data : values.entrySet()){
            urlBuilder.append(comma).append(data.getKey()).append("=").append(URLEncoder.encode(data.getValue(), "UTF-8"));
            comma = '&';
        }
        return resty.json(urlBuilder.toString());
    }

    public JSONResource postJSON(String url, String... values) throws IOException {
        if(values.length % 2 != 0){
            throw new IllegalArgumentException("Provided values are not a key-pair list");
        }
        FormData[] formData = new FormData[values.length/2];
        String key = null;
        for(int i=0; i < values.length; i++){
            if(i % 2 == 0){
                key = values[i];
            }else{
                formData[(i-1)/2] = Resty.data(key, values[i]);
            }
        }
        return resty.json(url.startsWith("http") ? url : APP_REST_URL + url, Resty.form(formData));
    }

    public JSONResource postJSON(String url, Map<String, String> values) throws IOException {
        FormData[] formData = new FormData[values.size()];
        int i = 0;
        for(Map.Entry<String, String> data : values.entrySet()){
            formData[i] = Resty.data(data.getKey(), data.getValue());
            i++;
        }
        return resty.json(url.startsWith("http") ? url : APP_REST_URL + url, Resty.form(formData));
    }
}

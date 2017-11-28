package utils;

import java.io.UnsupportedEncodingException;
import java.net.URL;
import java.net.URLDecoder;
import java.util.LinkedHashMap;
import java.util.Map;


public class Url {

    /*
    if key exist return value
    else return null
     */
    public static String getParamValueUrl(URL url, String key) {

        try {
            return Url.splitQuery(url).get(key);
        } catch (UnsupportedEncodingException e) {
            return null;
        }
    }

    public static Map<String, String> splitQuery(URL url) throws UnsupportedEncodingException {
        Map<String, String> query_pairs = new LinkedHashMap<String, String>();
        String query = url.getQuery();
        String[] pairs = query.split("&");
        for (String pair : pairs) {
            int idx = pair.indexOf("=");
            query_pairs.put(URLDecoder.decode(pair.substring(0, idx), "UTF-8"), URLDecoder.decode(pair.substring(idx + 1), "UTF-8"));
        }
        return query_pairs;
    }
}

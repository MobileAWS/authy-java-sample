package com.authy;

import com.authy.api.Resource;
import org.json.JSONObject;

import javax.crypto.Mac;
import javax.crypto.spec.SecretKeySpec;
import javax.xml.bind.DatatypeConverter;
import java.io.IOException;
import java.io.InputStream;
import java.io.UnsupportedEncodingException;
import java.net.URLEncoder;
import java.util.*;
import java.util.logging.Level;
import java.util.logging.Logger;

/**
 * @author hansospina
 *         <p>
 *         Copyright © 2016 Twilio, Inc. All Rights Reserved.
 */
public class AuthyUtil {

    private static final Logger LOGGER = Logger.getLogger(Resource.class.getName());

    private static String hmacSha(String KEY, String VALUE) {
        try {
            SecretKeySpec signingKey = new SecretKeySpec(KEY.getBytes("UTF-8"), "HmacSHA256");
            Mac mac = Mac.getInstance("HmacSHA256");
            mac.init(signingKey);
            byte[] rawHmac = mac.doFinal(VALUE.getBytes("UTF-8"));
            return DatatypeConverter.printBase64Binary(rawHmac);
        } catch (Exception ex) {
            throw new RuntimeException(ex);
        }
    }


    /**
     * Validates the request information to
     *
     * @param parameters The request parameters(all of them)
     * @param headers    The headers of the request
     * @param url        The url of the request.
     * @param authyToken the security token from the authy library
     * @return true if the signature ios valid, false otherwise
     * @throws UnsupportedEncodingException if the string parameters have problems with UTF-8 encoding.
     */
    private static boolean validateSignature(Map<String, String> parameters, Map<String, String> headers, String method, String url, String authyToken) throws UnsupportedEncodingException {

        StringBuilder sb = new StringBuilder(headers.get("X-Authy-Signature-Nonce"))
                .append("|")
                .append(method)
                .append("|")
                .append(url)
                .append("|")
                .append(mapToQuery(parameters));

        String signature = hmacSha(authyToken, sb.toString());

        // let's check that the Authy signature is valid
        return signature.equals(headers.get("X-Authy-Signature"));
    }


    private static void extract(String pre, JSONObject obj, HashMap<String, String> map) {

        for (String k : obj.keySet()) {

            String key = pre.length() == 0 ? k : pre + "[" + k + "]";

            if (obj.optJSONObject(k) != null) {
                extract(key, obj.getJSONObject(k), map);
            } else {

                Object val = obj.get(k);

                if (val instanceof Boolean) {
                    map.put(key, Boolean.toString(obj.getBoolean(k)));
                } else if (val instanceof Integer || val instanceof Long) {
                    map.put(key, Long.toString(obj.getLong(k)));
                } else if (val instanceof Float || val instanceof Double) {
                    map.put(key, Double.toString(obj.getDouble(k)));
                } else if (JSONObject.NULL.equals(val)) {
                    map.put(key, "");
                } else {
                    map.put(key, obj.getString(k));
                }

            }
        }

    }

    private static String mapToQuery(Map<String, String> map) throws UnsupportedEncodingException {

        StringBuilder sb = new StringBuilder();

        SortedSet<String> keys = new TreeSet<>(map.keySet());

        boolean first = true;

        for (String key : keys) {

            if (first) {
                first = false;
            } else {
                sb.append("&");
            }

            String value = map.get(key);

            // don't encode null values
            if (value == null) {
                continue;
            }

            sb.append(URLEncoder.encode(key, "UTF-8")).append("=").append(URLEncoder.encode(value, "UTF-8"));
        }

        return sb.toString();

    }


    /**
     * Validates the request information to
     *
     * @param body       The body of the request in case of a POST method
     * @param headers    The headers of the request
     * @param url        The url of the request.
     * @param authyToken the security token from the authy library
     * @return true if the signature ios valid, false otherwise
     * @throws UnsupportedEncodingException if the string parameters have problems with UTF-8 encoding.
     */
    public static boolean validateSignatureForPost(String body, Map<String, String> headers, String url, String authyToken) throws UnsupportedEncodingException {
        HashMap<String, String> params = new HashMap<>();
        extract("", new JSONObject(body), params);
        return validateSignature(params, headers, "POST", url, authyToken);
    }

    /**
     * Validates the request information to
     *
     * @param params     The query parameters in case of a GET request
     * @param headers    The headers of the request
     * @param url        The url of the request.
     * @param authyToken the security token from the authy library
     * @return true if the signature ios valid, false otherwise
     * @throws UnsupportedEncodingException if the string parameters have problems with UTF-8 encoding.
     */
    public static boolean validateSignatureForGet(Map<String, String> params, Map<String, String> headers, String url, String authyToken) throws UnsupportedEncodingException {
        return validateSignature(params, headers, "GET", url, authyToken);
    }


    /**
     * Loads your api_key and api_url properties from the given property file
     * <p>
     * Two important things to have in mind here:
     * 1) if api_key and api_url are defined as environment variables, they will be returned as the properties.
     * 2) If you want to load your properties file have in mind your classloader path may change.
     *
     * @return the Properties object containing the properties to setup Authy or an empty Properties object if no properties were found
     */
    public static Properties loadProperties(String path, Class cls) {

        Properties properties = new Properties();

        // environment variables will always override properties file

        try {

            InputStream in = cls.getResourceAsStream(path);

            // if we cant find the properties file
            if (in != null) {
                properties.load(in);
            }


            // Env variables will always override properties
            if (System.getenv("api_key") != null && System.getenv("api_url") != null) {
                properties.put("api_key", System.getenv("api_key"));
                properties.put("api_url", System.getenv("api_url"));
            }

        } catch (IOException e) {
            LOGGER.log(Level.SEVERE, "Problems loading properties", e);
        }


        return properties;
    }

}

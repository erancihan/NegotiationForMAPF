package edu.ozu.drone.utils;

import com.google.gson.Gson;
import com.google.gson.GsonBuilder;
import org.springframework.util.Assert;

import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStreamReader;
import java.io.OutputStream;
import java.net.HttpURLConnection;
import java.net.URL;
import java.nio.charset.StandardCharsets;
import java.util.HashMap;

public class Utils {
    private static final org.slf4j.Logger logger = org.slf4j.LoggerFactory.getLogger(Utils.class);
    private static Gson gson = new Gson();

    public static String get(String path)
    {
        try {
            URL url = new URL(path);
            HttpURLConnection conn = (HttpURLConnection) url.openConnection();

            conn.setRequestMethod("GET");

            BufferedReader in = new BufferedReader(new InputStreamReader(conn.getInputStream()));

            String il;
            StringBuffer response = new StringBuffer();
            while ((il = in.readLine()) != null) {
                response.append(il);
            }

            return String.valueOf(response);
        } catch (IOException error) {
            if (error.getClass().getName().equals("java.net.ConnectException")) {
                logger.error("«check server status»");
            } else {
                error.printStackTrace();
            }
        }

        return null;
    }

    public static String post(String path, HashMap<String, String> payload)
    {
        try {
            String post_data = gson.toJson(payload);
            Assert.notNull(post_data, "Post Data returned null!");

            URL url = new URL(path);
            HttpURLConnection conn = (HttpURLConnection) url.openConnection();

            conn.setRequestMethod("POST");
            conn.setRequestProperty("Content-Type", "application/json; utf-8");
            conn.setRequestProperty("Accept", "application/json");
            conn.setDoOutput(true);

            // write to output stream
            try (OutputStream stream = conn.getOutputStream()) {
                byte[] bytes = post_data.getBytes(StandardCharsets.UTF_8);
                stream.write(bytes, 0, bytes.length);
            }

            // read response
            BufferedReader reader = new BufferedReader(new InputStreamReader(conn.getInputStream(), StandardCharsets.UTF_8));
            String line;
            StringBuilder response = new StringBuilder();
            while ((line = reader.readLine()) != null) {
                response.append(line);
            }

            return String.valueOf(response);
        } catch (IOException err) {
            err.printStackTrace();
        }

        return null;
    }

    public static String toString(Object[] a) {
        return  toString(a, ", ");
    }

    public static String toString(Object[] arr, String delim) {
        if (arr == null)
            return "null";

        StringBuilder sb = new StringBuilder();
        sb.append('[');
        for (int i = 0; i < arr.length; i++)
        {
            sb.append(arr[i]);
            if (i == arr.length - 1)
                return sb.append(']').toString();
            sb.append(delim);
        }

        return "[]";
    }
}

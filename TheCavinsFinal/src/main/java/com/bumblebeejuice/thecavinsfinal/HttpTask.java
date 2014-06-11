package com.bumblebeejuice.thecavinsfinal;

import android.os.AsyncTask;
import android.util.Log;
import android.webkit.CookieManager;

import org.apache.http.util.ByteArrayBuffer;
import org.json.JSONObject;

import java.io.BufferedInputStream;
import java.io.BufferedOutputStream;
import java.io.BufferedReader;
import java.io.File;
import java.io.FileInputStream;
import java.io.InputStreamReader;
import java.net.CookieHandler;
import java.net.HttpCookie;
import java.net.HttpURLConnection;
import java.net.URI;
import java.net.URL;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

/**
 * Created by robcavin on 10/27/13.
 */
public class HttpTask extends AsyncTask<String, Float, JSONObject> {

    private final String API_URL = "http://thecavins.com";
    private final String POST_BOUNDARY = "jYfg5Y6HGVCjhyzxPUIw";

    private URL url;
    private HttpURLConnection connection;
    private BufferedOutputStream postWriter;

    private String method;
    private HashMap<String, String> headers;
    private HashMap<String, String> args;
    private ArrayList<HashMap<String, Object>> files;
    private String csrfToken;

    HttpTask() {
        this.method = "GET";
        this.headers = null;
        this.args = null;
        this.files = null;
        this.csrfToken = null;
    }

    public void setMethod(String method) {
        this.method = method;
    }

    public void setHeaders(HashMap<String, String> headers) {
        this.headers = headers;
    }

    public void setArgs(HashMap<String, String> args) {
        this.args = args;
    }

    public void setFiles(ArrayList<HashMap<String, Object>> files) {
        this.files = files;
    }

    private void sendRequest(String urlString) {

        try {

            boolean multipartPost = (method == "POST") && !((files == null) || files.size() == 0);
            StringBuilder queryString = null;

            // Build URL string
            if ((this.args != null) && !multipartPost) {
                queryString = new StringBuilder();
                for (Map.Entry<String, String> entry : args.entrySet()) {
                    queryString.append(entry.getKey()).append("=").append(entry.getValue()).append("&");
                }

                if (method == "GET") urlString.concat("?" + queryString.toString());
            }

            this.url = new URL(API_URL + urlString);
            this.connection = (HttpURLConnection) url.openConnection();

            this.connection.setRequestMethod(method);

            this.connection.setRequestProperty("Accept", "application/json");

            // Copy any cookies from the webKit cookie store into the urlconnection one
            CookieManager webKitCookieManager = CookieManager.getInstance();
            String cookieString = webKitCookieManager.getCookie(API_URL);
            this.connection.setRequestProperty("Cookie",cookieString);

            String[] cookies = cookieString.split(";");
            for (String temp_cookie : cookies) {
                HttpCookie cookie = HttpCookie.parse(temp_cookie).get(0);
                if (cookie.getName().equals("csrftoken"))
                    this.connection.setRequestProperty("X-CSRFToken", cookie.getValue());
            }

            if (this.headers != null) {
                for (Map.Entry<String, String> entry : this.headers.entrySet()) {
                    this.connection.setRequestProperty(entry.getKey(), entry.getValue());
                }
            }

            this.connection.setDoInput(true);

            if (method == "POST") {
                this.connection.setDoOutput(true);

                if (!multipartPost) {
                    if (queryString != null)
                        postWriter = new BufferedOutputStream(this.connection.getOutputStream());
                    postWriter.write(queryString.toString().getBytes("UTF-8"));
                } else {
                    // This sets a header and must be called before opening the output port
                    this.multipartFormInitialize();

                    postWriter = new BufferedOutputStream(this.connection.getOutputStream());

                    if (this.args != null) {
                        this.multipartFormAppendDataWithParams(this.args);
                    }

                    if (this.files != null) {
                        for (HashMap<String, Object> fileDict : this.files) {
                            BufferedInputStream fileStream = (BufferedInputStream) fileDict.get("stream");
                            if (fileStream == null) {
                                File file = new File((String) fileDict.get("path"));
                                fileStream = new BufferedInputStream(new FileInputStream(file));
                            }

                            this.multipartFormAppendFileData(
                                    fileStream,
                                    (String) fileDict.get("filename"),
                                    (String) fileDict.get("mime-type"),
                                    (String) fileDict.get("name"));
                        }
                    }

                    this.multipartFormTerminate();
                }

                // Flush and close the buffered writer
                postWriter.close();
            }

        } catch (Exception e) {
            e.printStackTrace();
        }
    }

    private void multipartFormInitialize() {

        String contentType = "multipart/form-data; boundary=" + POST_BOUNDARY;
        this.connection.setRequestProperty("Content-Type", contentType);
    }

    private void multipartFormAppendDataWithParams(HashMap<String, String> params) {

        StringBuilder postString = new StringBuilder();
        for (Map.Entry<String, String> entry : params.entrySet()) {
            postString.append("\r\n--" + POST_BOUNDARY + "\r\n");
            postString.append("Content-Disposition: form-data; name=\"" + entry.getKey() + "\"\r\n\r\n");
            postString.append(entry.getValue());
        }

        byte postData[];
        try {
            postData = postString.toString().getBytes("UTF-8");
            this.postWriter.write(postData);
        } catch (Exception e) {
            e.printStackTrace();
        }
    }

    private void multipartFormAppendFileData(BufferedInputStream fileInputStream, String filename, String contentType, String key) {

        StringBuilder postString = new StringBuilder();
        postString.append("\r\n--" + POST_BOUNDARY + "\r\n");
        postString.append("Content-Disposition: form-data; name=\"" + key + "\"; filename=\"" + filename + "\"\r\n");
        postString.append("Content-type: " + contentType + "\r\n\r\n");

        int bufferSize = 64 * 1024;
        byte buffer[] = new byte[bufferSize];
        int bytesRead = 0;

        try {
            // Write header into post form
            this.postWriter.write(postString.toString().getBytes("UTF-8"));

            // Write file into post form
            bytesRead = fileInputStream.read(buffer, 0, bufferSize);
            while (bytesRead > 0) {
                this.postWriter.write(buffer, 0, bytesRead);
                bytesRead = fileInputStream.read(buffer, 0, bufferSize);
            }
        } catch (Exception e) {
            e.printStackTrace();
        }
    }


    private void multipartFormTerminate() {
        String terminator = "\r\n--" + POST_BOUNDARY + "--\r\n";
        try {
            this.postWriter.write(terminator.getBytes("UTF-8"));
        } catch (Exception e) {
            e.printStackTrace();
        }
    }

    private JSONObject readResponse() {

        JSONObject jsonResponse = null;
        try {
            InputStreamReader in_reader;
            if (this.connection.getResponseCode() > 201) {
                in_reader = new InputStreamReader(this.connection.getErrorStream());
            } else {
                in_reader = new InputStreamReader(this.connection.getInputStream());
            }

            BufferedReader reader = new BufferedReader(in_reader);
            StringBuilder response = new StringBuilder();

            String line;
            while ((line = reader.readLine()) != null) {
                response.append(line);
            }

            if (this.connection.getResponseCode() > 201) {
                Log.d(this.getClass().getName(), response.toString());
            } else {
                jsonResponse = new JSONObject(response.toString());
            }

            List<String> cookieList = this.connection.getHeaderFields().get("Set-Cookie");
            CookieManager cookieManager = CookieManager.getInstance();
            if (cookieList != null) {
                for (String cookie : cookieList) {
                    cookieManager.setCookie(API_URL, cookie);
                }
            }

        } catch (Exception e) {
            e.printStackTrace();
        }

        return jsonResponse;
    }

    @Override
    protected JSONObject doInBackground(String... urlStrings) {

        // Create connection and send request
        this.sendRequest(urlStrings[0]);

        JSONObject response = this.readResponse();

        String cookies = CookieManager.getInstance().getCookie("http://thecavins.com/");
        Log.d(this.getClass().getName(), cookies);

        this.connection.disconnect();

        return response;
    }
}

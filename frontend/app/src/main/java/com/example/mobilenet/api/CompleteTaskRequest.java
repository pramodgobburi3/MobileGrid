package com.example.mobilenet.api;
import android.os.AsyncTask;
import android.util.Log;

import java.io.IOException;
import java.net.HttpURLConnection;
import java.net.URL;

public class CompleteTaskRequest extends AsyncTask<String, Void, Integer> {
    public interface CompleteTaskRequestCallback {
        void onRequestComplete(int responseCode);
        void onRequestFailed(Exception e);
    }

    private CompleteTaskRequestCallback callback;
    private Exception exception;

    public CompleteTaskRequest(CompleteTaskRequestCallback callback) {
        this.callback = callback;
    }

    @Override
    protected Integer doInBackground(String... params) {
        String deviceId = params[0];
        String taskId = params[1];
        int responseCode = -1;

        try {
            URL url = new URL("http://192.168.0.40:5000/device/" + deviceId + "/task/" + taskId + "/complete");
            HttpURLConnection connection = (HttpURLConnection) url.openConnection();
            connection.setRequestMethod("POST");
            connection.setDoOutput(true);
            connection.connect();

            responseCode = connection.getResponseCode();
            Log.d("HttpRequestTask", "Response Code: " + responseCode);

            connection.disconnect();
        } catch (IOException e) {
            Log.e("HttpRequestTask", "Error making POST request", e);
            exception = e;
        }

        return responseCode;
    }

    @Override
    protected void onPostExecute(Integer responseCode) {
        if (exception != null) {
            if (callback != null) {
                callback.onRequestFailed(exception);
            }
        } else {
            if (callback != null) {
                callback.onRequestComplete(responseCode);
            }
        }
    }
}

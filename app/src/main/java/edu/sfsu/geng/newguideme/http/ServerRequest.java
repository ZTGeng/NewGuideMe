package edu.sfsu.geng.newguideme.http;

import android.os.AsyncTask;
import android.os.Handler;
import android.os.Looper;
import android.util.Log;

import org.apache.http.HttpEntity;
import org.apache.http.HttpResponse;
import org.apache.http.NameValuePair;
import org.apache.http.client.HttpClient;
import org.apache.http.client.entity.UrlEncodedFormEntity;
import org.apache.http.client.methods.HttpGet;
import org.apache.http.client.methods.HttpPost;
import org.apache.http.impl.client.DefaultHttpClient;

import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.util.List;

/**
 * Created by geng on 7/15/16.
 */
class ServerRequest {

    private static final String TAG = "ServerRequest";

    private static final Handler mMainHandler = new Handler(Looper.getMainLooper());

    static void getJSON(String url, List<NameValuePair> params, ServerApi.DataListener dataListener) {

        UPL upl = new UPL(url, params, dataListener);
        Request myTask = new Request();
//        try {
            myTask.execute(upl);//.get();
//        } catch (InterruptedException | ExecutionException e) {
//            e.printStackTrace();
//        }
    }

    static void keepResAlive(String url, ServerApi.DataListener dataListener) {
        UPL upl = new UPL(url, dataListener);
        AliveRequest myTask = new AliveRequest();
//        try {
            myTask.execute(upl);//.get();
//        } catch (InterruptedException | ExecutionException e) {
//            e.printStackTrace();
//        }
    }

    private static SL getJSONFromUrl(String url, List<NameValuePair> params, ServerApi.DataListener dataListener) {

        InputStream is = null;
        try {
            DefaultHttpClient httpClient = new DefaultHttpClient();
            HttpPost httpPost = new HttpPost(url);
            httpPost.setEntity(new UrlEncodedFormEntity(params));

            HttpResponse httpResponse = httpClient.execute(httpPost);
            HttpEntity httpEntity = httpResponse.getEntity();
            is = httpEntity.getContent();
        } catch (IOException e) {
            Log.e(TAG, "getJSONFromUrl: " + e);
            e.printStackTrace();
        }

        String jsonStr = "";
        try {
            BufferedReader reader = new BufferedReader(new InputStreamReader(is, "iso-8859-1"), 8);
            StringBuilder sb = new StringBuilder();
            String line;
            while ((line = reader.readLine()) != null) {
                sb.append(line).append("\n");
            }
            is.close();
            jsonStr = sb.toString();
            Log.d(TAG, jsonStr);
        } catch (Exception e) {
            Log.e(TAG, "Error converting result " + e.toString());
        }

        return new SL(jsonStr, dataListener);

    }

    private static void keepAliveResp(final String url, final ServerApi.DataListener dataListener) {

        new Thread(new Runnable() {
            @Override
            public void run() {
                InputStream mEventStream = null;
                try {
                    HttpClient httpClient = new DefaultHttpClient();
                    HttpGet httpGet = new HttpGet(url);

                    HttpResponse httpResponse = httpClient.execute(httpGet);
                    HttpEntity httpEntity = httpResponse.getEntity();

                    if (httpEntity != null) {
                        mEventStream = httpEntity.getContent();
                        BufferedReader bufferedReader = new BufferedReader(new InputStreamReader(mEventStream));

                        String line;
                        while ((line = bufferedReader.readLine()) != null) {
                            if (line.length() > 1) {
                                String[] dataSplit = line.split(":", 2);

                                if (dataSplit.length != 2) {
                                    Log.e(TAG, "Invalid data: " + line);
                                    while (!(line = bufferedReader.readLine()).isEmpty()) {
                                        Log.e(TAG, "Skipped: " + line);
                                    }
                                    break;
                                }

                                switch (dataSplit[0]) {
                                    case "refresh":
                                        final String helperListJSON = dataSplit[1];

                                        mMainHandler.post(new Runnable() {
                                            @Override
                                            public void run() {
                                                dataListener.onReceiveData(helperListJSON);
                                            }
                                        });
                                        break;
                                    case "select":
                                        final String selectJSON = dataSplit[1];

                                        mMainHandler.post(new Runnable() {
                                            @Override
                                            public void run() {
                                                dataListener.onReceiveData(selectJSON);
                                            }
                                        });
                                        break;
                                    case "error":
                                        Log.e(TAG, "Server error: " + dataSplit[1]);
                                        break;
                                    default:
                                        Log.e(TAG, "Unhandled event: " + line);
                                        break;
                                }
                            }
                        }
                    }
                } catch (IOException exception) {
                    Log.e(TAG, "keepAliveResp: " + exception);
                    exception.printStackTrace();
                } finally {
                    if (mEventStream != null) {
                        try {
                            mEventStream.close();
                        } catch (IOException ignored) {

                        }
                    }
                    dataListener.onClose();
                }
            }
        }).start();
    }


    /*
     * Url, Params, and Listener
     */
    private static class UPL {
        String url;
        List<NameValuePair> params;
        ServerApi.DataListener dataListener;

        UPL(String url, List<NameValuePair> params, ServerApi.DataListener dataListener) {
            this.url = url;
            this.params = params;
            this.dataListener = dataListener;
        }

        UPL(String url, ServerApi.DataListener dataListener) {
            this.url = url;
            this.params = null;
            this.dataListener = dataListener;
        }
    }

    private static class SL {
        String data;
        ServerApi.DataListener dataListener;

        SL(String data, ServerApi.DataListener dataListener) {
            this.data = data;
            this.dataListener = dataListener;
        }
    }

    private static class Request extends AsyncTask<UPL, String, SL> {

        @Override
        protected SL doInBackground(UPL... args) {
            return ServerRequest.getJSONFromUrl(args[0].url, args[0].params, args[0].dataListener);
        }

        @Override
        protected void onPostExecute(SL sl) {
            if (sl.dataListener != null) {
                sl.dataListener.onReceiveData(sl.data);
            }
            super.onPostExecute(sl);
        }

    }

    private static class AliveRequest extends AsyncTask<UPL, String, Void> {

        @Override
        protected Void doInBackground(UPL... args) {
            ServerRequest.keepAliveResp(args[0].url, args[0].dataListener);
            return null;
        }

    }

}

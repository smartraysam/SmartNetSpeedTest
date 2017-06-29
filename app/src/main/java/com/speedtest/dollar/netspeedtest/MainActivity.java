package com.speedtest.dollar.netspeedtest;


import android.content.ActivityNotFoundException;
import android.content.Context;
import android.content.Intent;
import android.content.SharedPreferences;
import android.content.res.AssetFileDescriptor;
import android.content.res.AssetManager;
import android.net.ConnectivityManager;
import android.net.NetworkInfo;
import android.net.TrafficStats;
import android.os.AsyncTask;
import android.os.Environment;
import android.os.Handler;
import android.os.Message;
import android.support.v7.app.AppCompatActivity;
import android.os.Bundle;
import android.text.method.ScrollingMovementMethod;
import android.util.Log;
import android.view.View;
import android.widget.Button;
import android.widget.ProgressBar;
import android.widget.RadioButton;
import android.widget.Spinner;
import android.widget.TextView;
import android.widget.Toast;

import org.jsoup.Connection;
import org.jsoup.Jsoup;
import org.jsoup.nodes.Document;
import org.jsoup.nodes.Element;

import java.io.DataOutputStream;
import java.io.File;
import java.io.FileInputStream;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;
import java.net.HttpURLConnection;
import java.net.MalformedURLException;
import java.net.URL;
import java.net.URLConnection;
import java.text.DecimalFormat;
import java.util.ArrayList;
import java.util.Calendar;
import java.util.Collection;
import java.util.Collections;
import java.util.Timer;
import java.util.TimerTask;
import java.util.UUID;

import com.androidplot.xy.XYPlot;


public class MainActivity extends AppCompatActivity implements View.OnClickListener {

    public static Spinner server;
    Button butSave, butStart, netPer;
    TextView speedView, responseStatus, netType, mtestTime, connStatus;
    static TextView pingRd, DownRd, UpRd;
    String uniqueId = null;
    ProgressBar pingProgress, DownProgress, UpProgress;
    String DownUrl = "http://gamesdeed.com/test/down/downloadfile.bin";
    String UpUrl = "http://gamesdeed.com/test/up/test.html?";
    String postUrl = "http://gamesdeed.com/test/api/submit/?";
    public static int serverID;
    static double aveDown, aveUp, avePin = 0;
    public int AppCheck, conCheck ;
    public long startTime, diffTime, pingstartTime, pingendTime, pingdiffTime, diffTimemin = 0;
    public long testTimeStart, testTimeUsed;
    public double upSpeed;
    private int downCount, upCount, pingCount = 0;

    //Private fields
    private static final String TAG = MainActivity.class.getSimpleName();
    private static final int EXPECTED_SIZE_IN_BYTES = 1048576;//1MB 1024*1024
    private static final double BYTE_TO_KILOBIT = 0.0078125;
    private static final double KILOBIT_TO_MEGABIT = 0.0048125;//0.0009765625;


    private final int MSG_UPDATE_STATUS = 0;
    private final int MSG_UPDATE_CONNECTION_TIME = 1;
    private final int MSG_COMPLETE_STATUS = 2;
    long diffdownTime;
    private final static int UPDATE_THRESHOLD = 300;
    private DecimalFormat mDecimalFormater;
    String upLoadServerUri = null;
    int serverResponseCode = 0;
    int sec, min, hour, day, month, year;
    String postUID;
    /**********
     * File Path
     *************/
    static String path;
    String fileFolder = "/upload/";
    String filename = "upload.bin";
    Calendar getTime = Calendar.getInstance();
    private int progressDown, progressUp, progressPing;
    long TotalStartTx, TotalEndTx;
    double totalUpSpeed;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_main);
        pingRd = (TextView) findViewById(R.id.PingTime);
        DownRd = (TextView) findViewById(R.id.DownSpeed);
        UpRd = (TextView) findViewById(R.id.UpSpeed);
        server = (Spinner) findViewById(R.id.serverLists);
        speedView = (TextView) findViewById(R.id.textView);
        mtestTime = (TextView) findViewById(R.id.timeStamp);
        netType = (TextView) findViewById(R.id.netType);
        speedView.setMovementMethod(new ScrollingMovementMethod());
        pingProgress = (ProgressBar) findViewById(R.id.pingProg);
        DownProgress = (ProgressBar) findViewById(R.id.DownProg);
        UpProgress = (ProgressBar) findViewById(R.id.UpProg);
        butSave = (Button) findViewById(R.id.saveToServer);
        netPer = (Button) findViewById(R.id.IntPer);
        butStart = (Button) findViewById(R.id.startTest);
        responseStatus = (TextView) findViewById(R.id.testMsg);
        mDecimalFormater = new DecimalFormat("##.##");
        netPer.setOnClickListener(MainActivity.this);
        butSave.setOnClickListener(MainActivity.this);
        butStart.setOnClickListener(MainActivity.this);
        butStart.setVisibility(View.GONE);
        getUUID();
        copyAssets();
        speedView.setText("");
        checkNet();
        upLoadServerUri = UpUrl;
        File loadfile = getCacheDir();
        path = loadfile.getAbsolutePath() + fileFolder;
        responseStatus.setText("Please wait making connection to server...");
        conCheck = 0;
        new PingServer().execute();


    }

    // generate UUID
    public void getUUID() {
        SharedPreferences GpPro = getSharedPreferences("settingpro", MODE_PRIVATE);
        uniqueId = GpPro.getString("uid", null);
        if (uniqueId == null) {
            uniqueId = UUID.randomUUID().toString();
        }

        postUID = uniqueId.replaceAll("-", "");

    }

    // create upload file upload.bin
    private void copyAssets() {
        AssetManager assetManager = getAssets();
        String[] files = null;
        InputStream in = null;
        OutputStream out = null;
        try {
            File myDir = new File(getCacheDir(), "upload");
            myDir.mkdir();
            System.out.println(myDir.getAbsolutePath().toString());
            in = assetManager.open(filename);
            File outFile = new File(myDir.getAbsolutePath().toString(), filename);
            out = new FileOutputStream(outFile);
            copyFile(in, out);
            System.out.println("copied");

        } catch (IOException e) {
            Log.e("tag", "Failed to copy asset file: " + filename, e);
        } finally {
            if (in != null) {
                try {
                    in.close();
                    System.out.println("in closed");
                } catch (IOException e) {
                    // NOOP
                }
            }
            if (out != null) {
                try {
                    out.close();
                    System.out.println("out closed");
                } catch (IOException e) {
                    // NOOP
                }
            }
        }
    }

    // copying upload file from asset folder to phone memory
    private void copyFile(InputStream in, OutputStream out) throws IOException {
        byte[] buffer = new byte[1024];
        int read;
        while ((read = in.read(buffer)) != -1) {
            out.write(buffer, 0, read);
        }
    }

    @Override
    protected void onPause() {
        super.onPause();
        SharedPreferences GpPro = getSharedPreferences("settingpro", MODE_PRIVATE);
        SharedPreferences.Editor ePro = GpPro.edit();
        ePro.putString("uid", uniqueId);
        ePro.commit();

    }

    private boolean isOnline() {
        ConnectivityManager ConMan = (ConnectivityManager) getSystemService(Context.CONNECTIVITY_SERVICE);
        NetworkInfo netInfo = ConMan.getActiveNetworkInfo();
        return (netInfo != null && netInfo.isConnected());
    }

    @Override
    public void onClick(View v) {

        if (v == butStart) {
            conCheck++;
            responseStatus.setText("Evaluating network speed");
            DownRd.setText(String.valueOf(0) + "MBps");
            pingRd.setText(String.valueOf(0) + "ms");
            UpRd.setText(String.valueOf(0) + "MBps");
            speedView.setText("");
            pingCount = 0;
            upCount = 0;
            downCount = 0;
            avePin = 0;
            aveDown = 0;
            aveUp = 0;
            day = getTime.get(Calendar.DAY_OF_MONTH);
            month = getTime.get(Calendar.MONTH) + 1;
            year = getTime.get(Calendar.YEAR);
            sec = getTime.get(Calendar.SECOND);
            min = getTime.get(Calendar.MINUTE);
            hour = getTime.get(Calendar.HOUR_OF_DAY);
            mtestTime.setText(day + "/" + month + "/" + year + "  " + hour + ":" + min + ":" + sec);
            checkNet();
            speedView.append("Speed evaluation start\n");
            new PingServer().execute();
        }
        if (v == butSave) {
            speedView.setText("");
            new PostResult().execute();
        }
        if (v == netPer) {
            AppCheck++;
            Intent netGuard = getPackageManager().getLaunchIntentForPackage("eu.faircode.netguard");
            if (netGuard != null) {
                try {
                    startActivity(netGuard);
                } catch (ActivityNotFoundException err) {
                    Toast t = Toast.makeText(getApplicationContext(), "app_not_found", Toast.LENGTH_SHORT);
                    t.show();
                }
            }
        }
    }


    // post speed result to server
    private class PostResult extends AsyncTask<Void, Void, Void> {
        Document res = null;
        String response;
        String upvalue = String.valueOf(mDecimalFormater.format(aveUp / upCount));
        String pingvalue = String.valueOf(mDecimalFormater.format(avePin / pingCount));
        String downvalue = String.valueOf(mDecimalFormater.format(aveDown / downCount));


        @Override
        protected void onPreExecute() {
            super.onPreExecute();
            responseStatus.setText("Saving speed result to server");
            speedView.setText("Saving speed result to server\n");
            Toast.makeText(MainActivity.this, "Ping: " + pingvalue + "ms" + "  " + "Download: " + downvalue + "MBps" + " " + "Upload: " + upvalue + "MBps", Toast.LENGTH_LONG).show();
        }

        @Override
        protected Void doInBackground(Void... params) {
            try {
                res = Jsoup.connect(postUrl)
                        //referrer will be the login page's URL
                        .referrer(postUrl)
                        //uid=c99a11a53a3748269e3f86d7ac38df11&ts=111111&d=1000&u=100&p=30&s=1
                        //user agent
                        .userAgent("Mozilla")
                        //connect and read time out
                        .timeout(100000)
                        .data("uid", postUID)
                        .data("ts", String.valueOf(hour) + String.valueOf(min) + String.valueOf(sec))
                        .data("d", downvalue)
                        .data("u", upvalue)
                        .data("p", pingvalue)
                        .data("s", String.valueOf(serverID))
                        .post();
                Element Usercheck = res.body();
                response = Usercheck.text();
            } catch (IOException e) {
                // TODO Auto-generated catch block
                e.printStackTrace();
            }

            return null;
        }

        @Override
        protected void onPostExecute(Void result) {
            super.onPostExecute(result);
            Toast.makeText(MainActivity.this, response, Toast.LENGTH_SHORT).show();
            speedView.append("Ping respone time " + " " + String.valueOf(mDecimalFormater.format(avePin / pingCount)) + "ms\n");
            speedView.append("Download Speed is " + " " + String.valueOf(mDecimalFormater.format(aveDown / downCount)) + "MBps\n");
            speedView.append("Upload Speed is " + " " + String.valueOf(mDecimalFormater.format(aveUp / upCount)) + "MBps\n");
            if (response.contains("success")) {
                // Toast.makeText(MainActivity.this, "post successful", Toast.LENGTH_SHORT).show();
                responseStatus.setText("Post successsful");
                speedView.append("Post successsful");
            } else {
                // Toast.makeText(MainActivity.this, "post unsuccessful", Toast.LENGTH_SHORT).show();
                responseStatus.setText("Post unsuccesssful");
                speedView.append("Post unsuccesssful");
            }

        }
    }

    //ping server
    private class PingServer extends AsyncTask<Void, Void, Void> {
        String url = getServer();
        Connection.Response res = null;
        int code;
        String status;
        String responseMsg;

        @Override
        protected void onPreExecute() {
            super.onPreExecute();
            testTimeStart = System.currentTimeMillis();
            if (conCheck > 0) {
                speedView.append("Evaluating network ping response\n");
            }
            else{
                speedView.append("Finding the best server\n");
            }

        }


        @Override
        protected Void doInBackground(Void... params) {
            while (System.currentTimeMillis() < testTimeStart + 10000) {
                try {
                    pingstartTime = System.currentTimeMillis();
                    res = Jsoup.connect(url).timeout(5000).header("User-Agent",
                            "Mozilla/5.0 (Windows NT 6.1; WOW64; rv:29.0) Gecko/20100101 Firefox/29.0").execute();
                    pingendTime = System.currentTimeMillis();
                    pingdiffTime = pingendTime - pingstartTime;
                    code = res.statusCode();
                    status = res.statusMessage();
                    responseMsg = "Ping network respone was successful:" + " " + "Time response: " + String.valueOf(pingdiffTime) + "ms" + "\n";
                    avePin = avePin + pingdiffTime;
                    pingCount++;
                    runOnUiThread(new Runnable() {
                        public void run() {
                            if (conCheck > 0) {
                                if (code == 200) {
                                    pingRd.setText(String.valueOf(pingdiffTime) + "ms");
                                } else {
                                    pingRd.setText(String.valueOf(pingdiffTime) + "ms");
                                }

                            }
                        }
                    });

                } catch (IOException e) {
                    // TODO Auto-generated catch block
                    e.printStackTrace();
                }
            }
            return null;
        }


        @Override
        protected void onPostExecute(Void result) {
            super.onPostExecute(result);
            checkNet();
            new Thread(downSpeed).start();
            if (conCheck > 0) {
                speedView.append("Ping test Complete\n");
                //speedView.append("Ping respone time " + " " + String.valueOf((long)avePin / pingCount) + "ms\n");
                pingRd.setText("");
                pingRd.setText(String.valueOf((long) avePin / pingCount) + "ms");
            }
            else{
                speedView.append("Server found\n");
            }
        }
    }

    // select server to ping

    public static String getServer() {
        if (server.getSelectedItemPosition() == 0) {
            serverID = 1;
            return "http://gamesdeed.com";
        } else if (server.getSelectedItemPosition() == 1) {
            serverID = 2;
            return "http://www.google.com";
        } else if (server.getSelectedItemPosition() == 2) {
            serverID = 3;
            return "http://www.google.com";
        } else if (server.getSelectedItemPosition() == 3) {
            serverID = 4;
            return "http://gamesdeed.com/test/api/submit";
        } else if (server.getSelectedItemPosition() == 4) {
            serverID = 5;
            return "http://www.facebook.com";
        } else {
            return null;
        }

    }
    //get download speed

    private final Handler mHandler = new Handler() {
        @Override
        public void handleMessage(final Message msg) {
            switch (msg.what) {
                case MSG_UPDATE_STATUS:
                    final SpeedInfo info1 = (SpeedInfo) msg.obj;
                    DownRd.setText(String.format(getResources().getString(R.string.update_speed), mDecimalFormater.format(info1.megabits)));
                    aveDown = aveDown + info1.megabits;
                    checkNet();
                    downCount++;
                    break;
                case MSG_UPDATE_CONNECTION_TIME:
                    speedView.append("Evaluating download speed\n");
                    speedView.append(String.format(getResources().getString(R.string.update_connectionspeed), msg.arg1) + "\n");
                    break;
                case MSG_COMPLETE_STATUS:
                    final SpeedInfo info2 = (SpeedInfo) msg.obj;
                    DownRd.setText(String.format(getResources().getString(R.string.update_downloaded_complete), mDecimalFormater.format(info2.megabits)));
                    /**

                     **/
                    break;
                default:
                    super.handleMessage(msg);
            }
        }
    };

    private final Runnable downSpeed = new Runnable() {
        @Override
        public void run() {
            long downtestTime = System.currentTimeMillis();

            while (System.currentTimeMillis() < downtestTime + 30000) {
                InputStream stream = null;
                diffdownTime = 0;
                try {
                    int bytesIn = 0;
                    String downloadFileUrl = DownUrl;
                    long startCon = System.currentTimeMillis();
                    URL url = new URL(downloadFileUrl);
                    URLConnection con = url.openConnection();
                    con.setUseCaches(false);
                    long connectionLatency = System.currentTimeMillis() - startCon;
                    stream = con.getInputStream();

                    Message msgUpdateConnection = Message.obtain(mHandler, MSG_UPDATE_CONNECTION_TIME);
                    msgUpdateConnection.arg1 = (int) connectionLatency;
                    if (conCheck > 0) {
                        mHandler.sendMessage(msgUpdateConnection);
                    }


                    final long start = System.currentTimeMillis();
                    int currentByte = 0;
                    long updateStart = System.currentTimeMillis();
                    long updateDelta = 0;
                    int bytesInThreshold = 0;
                    while ((currentByte = stream.read()) != -1) {
                        bytesIn++;
                        bytesInThreshold++;
                        if (updateDelta >= UPDATE_THRESHOLD) {
                            int progress = (int) ((bytesIn / (double) EXPECTED_SIZE_IN_BYTES) * 100);
                            Message msg = Message.obtain(mHandler, MSG_UPDATE_STATUS, calculate(updateDelta, bytesInThreshold));
                            msg.arg1 = progress;
                            msg.arg2 = bytesIn;
                            if (conCheck > 0) {
                                mHandler.sendMessage(msg);
                            }

                            updateStart = System.currentTimeMillis();
                            bytesInThreshold = 0;
                        }
                        updateDelta = System.currentTimeMillis() - updateStart;

                    }

                    long downloadTime = (System.currentTimeMillis() - start);
                    //Prevent AritchmeticException
                    if (downloadTime == 0) {
                        downloadTime = 1;
                    }
                    Message msg = Message.obtain(mHandler, MSG_COMPLETE_STATUS, calculate(downloadTime, bytesIn));
                    diffdownTime = (System.currentTimeMillis() - start);
                    msg.arg1 = bytesIn;
                    if (conCheck > 0) {
                        mHandler.sendMessage(msg);
                    }

                } catch (MalformedURLException e) {
                    Log.e(TAG, e.getMessage());
                } catch (IOException e) {
                    //     Log.e(TAG, e.getMessage());
                } finally {
                    try {
                        if (stream != null) {
                            stream.close();
                        }
                    } catch (IOException e) {
                        //Suppressed
                    }
                }
            }
            runOnUiThread(new Runnable() {
                public void run() {
                    if (conCheck > 0) {
                        speedView.append("Download Speed evaluation is complete\n");
                        //speedView.append("Download Speed is " + " " + String.valueOf(mDecimalFormater.format(aveDown / downCount)) + "MBps\n");
                        DownRd.setText(String.valueOf(mDecimalFormater.format(aveDown / downCount)) + "MBps");

                    }
                    else{
                        speedView.append("Connecting to server...\n");
                    }
                }
            });
            checkNet();
            UploadTask t = new UploadTask();
            String[] upparam = new String[]{UpUrl, path + "" + filename};
            t.execute(upparam);
        }
    };

    public static SpeedInfo calculate(final long downloadTime, final long bytesIn) {
        SpeedInfo info = new SpeedInfo();
        //from mil to sec
        long bytespersecond = (bytesIn / downloadTime) * 1000;
        double kilobits = bytespersecond * BYTE_TO_KILOBIT;
        double megabits = kilobits * KILOBIT_TO_MEGABIT;
        info.downspeed = bytespersecond;
        info.kilobits = kilobits;
        info.megabits = megabits;
        return info;
    }

    /**
     * Transfer Object
     *
     * @author devil
     */
    private static class SpeedInfo {
        public double kilobits = 0;
        public double megabits = 0;
        public double downspeed = 0;
    }

    private class UploadTask extends AsyncTask<String, Void, String> {
        HttpURLConnection conn = null;
        long byteUpload;

        @Override
        protected String doInBackground(String... params) {
            runOnUiThread(new Runnable() {
                public void run() {
                    if(conCheck>0){
                        speedView.append("Evaluating upload speed\n");
                    }

                }
            });
            String upLoadServerUri = params[0];
            String fileName = params[1];
            diffTime = 0;
            String serverResponseMessage = null;
            DataOutputStream dos = null;
            String lineEnd = "\r\n";
            String twoHyphens = "--";
            String boundary = "*****";
            int bytesRead, bytesAvailable, bufferSize;
            byte[] buffer;
            int maxBufferSize = 512;
            File sourceFile = new File(fileName);
            if (!sourceFile.isFile()) {
                Log.e("uploadFile", "Source File not exist :"
                        + fileFolder + "" + filename);

                runOnUiThread(new Runnable() {
                    public void run() {
                        responseStatus.setText("Source File not exist :"
                                + fileFolder + "" + filename);
                    }
                });

                return null;

            } else {
                long newtestTime = System.currentTimeMillis();
                while (System.currentTimeMillis() < newtestTime + 10000) {
                    try {
                        // open a URL connection to the Servlet
                        FileInputStream fileInputStream = new FileInputStream(sourceFile);
                        //byte[] randomData=generateBinData(5*1024);
                        URL url = new URL(upLoadServerUri);
                        // Open a HTTP  connection to  the URL
                        conn = (HttpURLConnection) url.openConnection();
                        conn.setDoInput(true); // Allow Inputs
                        conn.setDoOutput(true); // Allow Outputs
                        conn.setUseCaches(false); // Don't use a Cached Copy
                        conn.setReadTimeout(15000); // Don't use a Cached Copy
                        conn.setConnectTimeout(15000); // Don't use a Cached Copy
                        conn.setRequestMethod("POST");
                        conn.setRequestProperty("Connection", "Keep-Alive");
                        conn.setRequestProperty("ENCTYPE", "multipart/form-data");
                        conn.setRequestProperty("Content-Type", "multipart/form-data;boundary=" + boundary);
                        conn.setRequestProperty("uploaded_file", fileName);
                        dos = new DataOutputStream(conn.getOutputStream());

                        dos.writeBytes(twoHyphens + boundary + lineEnd);
                        dos.writeBytes("Content-Disposition: form-data; name=\"uploaded_file\";filename=\""
                                + fileName + "\"" + lineEnd);

                        dos.writeBytes(lineEnd);
                        // create a buffer of  maximum size
                        bytesAvailable = fileInputStream.available();
                        bufferSize = Math.min(bytesAvailable, maxBufferSize);
                        buffer = new byte[bufferSize];
                        // read file and write it into form...
                        bytesRead = fileInputStream.read(buffer, 0, bufferSize);
                        startTime = System.currentTimeMillis();
                        while (bytesRead > 0) {
                            upCount++;
                            dos.write(buffer, 0, bufferSize);
                            bytesAvailable = fileInputStream.available();
                            bufferSize = Math.min(bytesAvailable, maxBufferSize);
                            bytesRead = fileInputStream.read(buffer, 0, bufferSize);
                            diffTime = System.currentTimeMillis() - startTime;
                            if (diffTime == 0) {
                                diffTime = 1000;
                            } else {
                                upSpeed = (1 / (((double) diffTime) / 1000)) * (0.09765625 / 3); // convert 1byte to Megabyte
                                aveUp = aveUp + upSpeed;
                                runOnUiThread(new Runnable() {
                                    public void run() {
                                        if(conCheck>0){
                                            UpRd.setText(mDecimalFormater.format(upSpeed) + "MBps");
                                        }

                                    }
                                });

                            }
                        }

                        // send multipart form data necesssary after file data...
                        dos.writeBytes(lineEnd);
                        dos.writeBytes(twoHyphens + boundary + twoHyphens + lineEnd);
                        Log.i("uploadFile", "HTTP Response is : "
                                + serverResponseMessage + ": " + serverResponseCode);
                        //close the streams //
                        fileInputStream.close();
                        dos.flush();
                        dos.close();
                        diffTimemin = System.currentTimeMillis() - startTime;
                        //    conn.setRequestProperty("Connection", "close");
                    } catch (MalformedURLException ex) {
                        ex.printStackTrace();
                        Log.e("Upload file to server", "error: " + ex.getMessage(), ex);
                    } catch (Exception e) {
                        e.printStackTrace();
                        Log.e("Error Uploading", "Exception : "
                                + e.getMessage(), e);
                    }
                }

            }
            return serverResponseMessage;
        }

        @Override
        protected void onPostExecute(String data) {
            //Toast.makeText(MainActivity.this, "Upload completed in "+String.valueOf(diffTimemin), Toast.LENGTH_SHORT).show();
            if(conCheck>0){
                speedView.append("Upload Speed evaluation is complete\n");
                //speedView.append("Upload Speed is " + " " +  String.valueOf(mDecimalFormater.format(aveUp / upCount)) + "MBps\n");
                UpRd.setText(String.valueOf(mDecimalFormater.format(aveUp / upCount)) + "MBps");
                speedView.append("Speed evaluation is complete\n");
                responseStatus.setText("Speed evaluation complete");
            }
            else{
                responseStatus.setText("Connection sucessful, start test");
                speedView.append("Connection sucessful start test\n");
                butStart.setVisibility(View.VISIBLE);

            }

        }
    }
    // calculate upload speed

    /**
     * public static String uploadSpeed(String filename, String uploadUrl) {
     * String response = "error";
     * HttpURLConnection UpConnection = null;
     * DataOutputStream UpByte = null;
     * <p>
     * String pathToOurFile = filename;
     * String urlServer = uploadUrl;
     * String lineEnd = "\r\n";
     * String twoHyphens = "--";
     * String boundary = "*****";
     * <p>
     * int bytesRead, bytesAvailable, bufferSize;
     * byte[] buffer;
     * int maxBufferSize = 1 * 1024;
     * try {
     * FileInputStream fileInputStream = new FileInputStream(new File(
     * path+""+filename));
     * <p>
     * URL url = new URL(urlServer);
     * UpConnection = (HttpURLConnection) url.openConnection();
     * System.out.println("uploading started");
     * // Allow Inputs & Outputs
     * UpConnection.setDoInput(true); // Allow Inputs
     * UpConnection.setDoOutput(true); // Allow Outputs
     * UpConnection.setUseCaches(false); // Don't use a Cached Copy
     * UpConnection.setRequestMethod("POST");
     * UpConnection.setRequestProperty("Connection", "Keep-Alive");
     * UpConnection.setRequestProperty("ENCTYPE", "multipart/form-data");
     * UpConnection.setRequestProperty("Content-Type", "multipart/form-data;boundary=" + boundary);
     * UpConnection.setRequestProperty("uploaded_file", filename);
     * UpByte = new DataOutputStream(UpConnection.getOutputStream());
     * <p>
     * UpByte.writeBytes(twoHyphens + boundary + lineEnd);
     * UpByte.writeBytes("Content-Disposition: form-data; name=\"uploaded_file\";filename=\""
     * + filename + "\"" + lineEnd);
     * <p>
     * UpByte.writeBytes(lineEnd);
     * <p>
     * <p>
     * bytesAvailable = fileInputStream.available();
     * bufferSize = Math.min(bytesAvailable, maxBufferSize);
     * buffer = new byte[bufferSize];
     * <p>
     * // Read file
     * bytesRead = fileInputStream.read(buffer, 0, bufferSize);
     * long uploadStart = System.currentTimeMillis();
     * long uploadDelta = 0;
     * try {
     * while (bytesRead > 0) {
     * try {
     * UpByte.write(buffer, 0, bufferSize);
     * System.out.println("send"+ String.valueOf(buffer)+ String.valueOf(bytesRead));
     * } catch (OutOfMemoryError e) {
     * e.printStackTrace();
     * response = "outofmemoryerror";
     * return response;
     * }
     * bytesAvailable = fileInputStream.available();
     * bufferSize = Math.min(bytesAvailable, maxBufferSize);
     * bytesRead = fileInputStream.read(buffer, 0, bufferSize);
     * uploadDelta = System.currentTimeMillis() - uploadStart;
     * upSpeed=(bytesRead/uploadDelta)*1000;
     * UpRd.append(String.valueOf(upSpeed)+"kbit/s");
     * }
     * } catch (Exception e) {
     * e.printStackTrace();
     * response = "error";
     * return response;
     * }
     * UpByte.writeBytes(lineEnd);
     * UpByte.writeBytes(twoHyphens + boundary + twoHyphens
     * + lineEnd);
     * <p>
     * // Responses from the server (code and message)
     * int serverResponseCode = UpConnection.getResponseCode();
     * String serverResponseMessage = UpConnection.getResponseMessage();
     * System.out.println("Server Response Code " + " " + serverResponseCode);
     * System.out.println("Server Response Message " + serverResponseMessage);
     * <p>
     * if (serverResponseCode == 200) {
     * response = "true";
     * } else {
     * response = "false";
     * }
     * <p>
     * fileInputStream.close();
     * UpByte.flush();
     * <p>
     * UpConnection.getInputStream();
     * //for android InputStream is = connection.getInputStream();
     * java.io.InputStream is = UpConnection.getInputStream();
     * <p>
     * int ch;
     * StringBuffer b = new StringBuffer();
     * while ((ch = is.read()) != -1) {
     * b.append((char) ch);
     * }
     * <p>
     * String responseString = b.toString();
     * System.out.println("response string is" + responseString); //Here is the actual output
     * <p>
     * UpByte.close();
     * UpByte = null;
     * <p>
     * } catch (Exception ex) {
     * // Exception handling
     * response = "error";
     * System.out.println("Send file Exception" + ex.getMessage() + "");
     * ex.printStackTrace();
     * }
     * return response;
     * }
     **/
    //check network status
    public void checkNet() {
        runOnUiThread(new Runnable() {
            public void run() {
                ConnectivityManager ConMan = (ConnectivityManager) getSystemService(Context.CONNECTIVITY_SERVICE);
                NetworkInfo netInfo = ConMan.getActiveNetworkInfo();
                if (isOnline()) {
                    int type = netInfo.getType();
                    int subtype = netInfo.getSubtype();
                    String nettypeName = netInfo.getTypeName();
                    String netsubtypeName = netInfo.getSubtypeName();
                    if (Connectivity.isConnectionFast(type, subtype) == true) {
                        netType.setText(nettypeName + " " + "Connection" + "/" + netsubtypeName + " " + "Network");
                    } else if (Connectivity.isConnectionFast(type, subtype) == false) {
                        netType.setText(nettypeName + " " + "Connection" + "/" + netsubtypeName + " " + "Network");
                    }
                } else {
                    Toast.makeText(MainActivity.this, "no inetrnet connection", Toast.LENGTH_LONG).show();

                }
            }
        });

    }

}

package com.gorkemgok.hastatakipsistemi;

import java.io.IOException;
import java.io.InputStream;
import java.text.SimpleDateFormat;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Date;
import java.util.UUID;
import android.app.Activity;
import android.app.ProgressDialog;
import android.bluetooth.BluetoothAdapter;
import android.bluetooth.BluetoothDevice;
import android.bluetooth.BluetoothSocket;
import android.content.Intent;
import android.graphics.Color;
import android.os.AsyncTask;
import android.os.Build;
import android.os.Bundle;
import android.util.Log;
import android.widget.TextView;
import android.widget.Toast;

import androidx.annotation.RequiresApi;

public class veriEkrani extends Activity {


    private static final String TAG = "Hasta Takip Sistemi";
    private int mMaxChars = 50000;//Default
    private UUID mDeviceUUID;
    private BluetoothSocket mBTSocket;
    private ReadInput mReadThread = null;


    private boolean mIsUserInitiatedDisconnect = false;

    TextView hastaAdi;
    TextView mTxtReceive;
    TextView satDeger;
    TextView nabDeger;
    TextView sicakDeger;
    TextView durum;
    TextView veriZaman;
    TextView baglilik;
    TextView basariOran;

    private boolean mIsBluetoothConnected = false;

    private BluetoothDevice mDevice;

    private ProgressDialog progressDialog;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_veri_ekrani);
        hastaAdi = findViewById(R.id.hastaAdi2);
        satDeger = findViewById(R.id.satDeger);
        nabDeger = findViewById(R.id.nabDeger);
        sicakDeger = findViewById(R.id.sicakDeger);
        durum = findViewById(R.id.durum);
        veriZaman = findViewById(R.id.veriZaman);
        Intent intent = getIntent();
        String hasta = intent.getStringExtra("userInput");
        hastaAdi.setText(hasta);
        Bundle b = intent.getExtras();
        mDevice = b.getParcelable(anaProgram.DEVICE_EXTRA);
        mDeviceUUID = UUID.fromString(b.getString(anaProgram.DEVICE_UUID));
        mMaxChars = b.getInt(anaProgram.BUFFER_SIZE);
        Log.d(TAG, "Hazır.");
        mTxtReceive = (TextView) findViewById(R.id.durum);
        baglilik = (TextView) findViewById(R.id.baglilik);
        basariOran = (TextView) findViewById(R.id.basariOran);
    }

    private class ReadInput implements Runnable {

        private boolean bStop = false;
        private Thread t;
        public ReadInput() {
            t = new Thread(this, "Alıcı thread.");
            t.start();
        }

        public boolean isRunning() {
            return t.isAlive();
        }

        @RequiresApi(api = Build.VERSION_CODES.O)
        @Override
        public void run() {
            InputStream inputStream;
            try {
                inputStream = mBTSocket.getInputStream();
                while (!bStop) {
                    byte[] buffer = new byte[256];
                    if (inputStream.available() > 0) {
                        inputStream.read(buffer);
                        Date date = new Date();
                        SimpleDateFormat formatter = new SimpleDateFormat("HH:mm:ss");
                        String zaman = formatter.format(date);
                        int i = 0;
                    
                        for (i = 0; i < buffer.length && buffer[i] != 0; i++) {
                        }
                        final String strInput = new String(buffer, 0, i);
                            mTxtReceive.post(new Runnable() {
                                @Override
                                public void run() {
                                    System.out.println(strInput);

                                    ArrayList<String> veriler = new ArrayList<>(Arrays.asList(strInput.split(",")));
                                    if(veriler.size() == 5){
                                        char controlDurum = veriler.get(3).charAt(0);
                                        String temp = controlDurum =='1' ? "Acil kontrol gerekli !": controlDurum == '2' ? "Kontrol gerekli !"
                                                : controlDurum == '3' ? "İyi" : controlDurum == '4' ? "Çok iyi !" : "Veri bekleniyor.";
                                        int colorDurum = controlDurum =='1' ? Color.RED: controlDurum == '2' ? Color.rgb(252, 128, 0)
                                                : controlDurum == '3' ? Color.rgb(0, 120, 0) : controlDurum == '4' ? Color.BLUE : Color.BLACK;
                                        durum.setText(temp);
                                        durum.setTextColor(colorDurum);
                                        int controlSat = Integer.parseInt(veriler.get(0));
                                        controlSat = controlSat >= 90 ? Color.rgb(0, 120, 0): controlSat >=80  ? Color.rgb(252, 128, 0)
                                                : controlSat < 80 ? Color.RED : Color.BLACK;
                                        satDeger.setText("%" + veriler.get(0));
                                        satDeger.setTextColor(controlSat);
                                        int controlNab = Integer.parseInt(veriler.get(1));
                                        controlNab = controlNab < 60 ? Color.RED : controlNab >100 ? Color.RED : Color.rgb(0, 120, 0);
                                        nabDeger.setText(veriler.get(1) + " bpm");
                                        nabDeger.setTextColor(controlNab);
                                        float controlSicak = Float.parseFloat(veriler.get(2));
                                        controlSicak = controlSicak<35.5 ? Color.RED : controlSicak > 38 ? Color.RED : Color.rgb(0, 120, 0);
                                        sicakDeger.setText(veriler.get(2) + " °C");
                                        sicakDeger.setTextColor((int) controlSicak);
                                        veriZaman.setText(zaman);
                                        basariOran.setText("% " + veriler.get(4));
                                        if(Integer.parseInt(String.valueOf(controlDurum)) == 0){
                                            baglilik.setText("Cihaz bağlantısı kesildi. Son veriler:");
                                            baglilik.setTextColor(Color.RED);
                                        }
                                        else{
                                            baglilik.setText("Cihaz bağlandı.");
                                            baglilik.setTextColor(Color.rgb(0, 120, 0));
                                        }
                                    }
                                    int txtLength = mTxtReceive.length();
                                    if(txtLength > mMaxChars){
                                        mTxtReceive.getEditableText().delete(0, txtLength - mMaxChars);
                                    }
                                }
                            });


                    }
                    Thread.sleep(3000);
                }
            } catch (IOException e) {
                e.printStackTrace();
            } catch (InterruptedException e) {
                e.printStackTrace();
            }
        }

        public void stop() {
            bStop = true;
        }

    }

    private class DisConnectBT extends AsyncTask<Void, Void, Void> {

        @Override
        protected void onPreExecute() {
        }

        @Override
        protected Void doInBackground(Void... params) {

            if (mReadThread != null) {
                mReadThread.stop();
                while (mReadThread.isRunning())
                    ;
                mReadThread = null;

            }

            try {
                mBTSocket.close();
            } catch (IOException e) {
                e.printStackTrace();
            }

            return null;
        }

        @Override
        protected void onPostExecute(Void result) {
            super.onPostExecute(result);
            mIsBluetoothConnected = false;
            if (mIsUserInitiatedDisconnect) {
                finish();
            }
        }

    }

    private void msg(String s) {
        Toast.makeText(getApplicationContext(), s, Toast.LENGTH_SHORT).show();
    }

    @Override
    protected void onPause() {
        if (mBTSocket != null && mIsBluetoothConnected) {
            new DisConnectBT().execute();
        }
        Log.d(TAG, "Duraklatıldı.");
        super.onPause();
    }

    @Override
    protected void onResume() {
        if (mBTSocket == null || !mIsBluetoothConnected) {
            new ConnectBT().execute();
        }
        Log.d(TAG, "Devam ediyor.");
        super.onResume();
    }

    @Override
    protected void onStop() {
        Log.d(TAG, "Kapatıldı.");
        super.onStop();
    }

    @Override
    protected void onSaveInstanceState(Bundle outState) {
        super.onSaveInstanceState(outState);
    }

    private class ConnectBT extends AsyncTask<Void, Void, Void> {
        private boolean mConnectSuccessful = true;

        @Override
        protected void onPreExecute() {
            progressDialog = ProgressDialog.show(veriEkrani.this, "Lütfen bekleyiniz..", "Bağlanıyor...");// http://stackoverflow.com/a/11130220/1287554
        }

        @Override
        protected Void doInBackground(Void... devices) {

            try {
                if (mBTSocket == null || !mIsBluetoothConnected) {
                    mBTSocket = mDevice.createInsecureRfcommSocketToServiceRecord(mDeviceUUID);
                    BluetoothAdapter.getDefaultAdapter().cancelDiscovery();
                    mBTSocket.connect();
                }
            } catch (IOException e) {
                e.printStackTrace();
                mConnectSuccessful = false;
            }
            return null;
        }

        @Override
        protected void onPostExecute(Void result) {
            super.onPostExecute(result);

            if (!mConnectSuccessful) {
                Toast.makeText(getApplicationContext(), "Cihaz ile eşleşme sağlanamadı.", Toast.LENGTH_LONG).show();
                finish();
            } else {
                msg("Cihaza bağlandı.");
                mIsBluetoothConnected = true;
                mReadThread = new ReadInput(); 
            }

            progressDialog.dismiss();
        }

    }



}

package com.example.appscan;

import android.content.Context;
import android.content.Intent;
import android.os.AsyncTask;
import android.os.Bundle;
import android.view.View;
import android.widget.Button;
import android.widget.TextView;
import android.widget.Toast;

import androidx.appcompat.app.AppCompatActivity;

import com.google.zxing.integration.android.IntentIntegrator;
import com.google.zxing.integration.android.IntentResult;

import org.json.JSONException;
import org.json.JSONObject;

import java.io.BufferedOutputStream;
import java.io.BufferedReader;
import java.io.BufferedWriter;
import java.io.IOException;
import java.io.InputStreamReader;
import java.io.OutputStream;
import java.io.OutputStreamWriter;
import java.io.UnsupportedEncodingException;
import java.net.HttpURLConnection;
import java.net.MalformedURLException;
import java.net.ProtocolException;
import java.net.URL;

//implementing onclicklistener
public class MainActivity extends AppCompatActivity implements View.OnClickListener {

    //View Objects
    private Button buttonScan;
    private TextView textViewTemp;

    //qr code scanner object
    private IntentIntegrator qrScan;
    String line;
    String strUrl;
    String typeRequest;
    String postData;
    String contentType;
    Button button;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_main);

        //View objects
        buttonScan = (Button) findViewById(R.id.buttonScan);
        button = (Button) findViewById(R.id.button);
        textViewTemp = (TextView) findViewById(R.id.textViewName);


        //intializing scan object
        qrScan = new IntentIntegrator(this);

        //attaching onclick listener
        buttonScan.setOnClickListener(this);

        Context context = getApplicationContext();
        CharSequence text = "Requête envoyée";
        int duration = Toast.LENGTH_SHORT;

        final Toast toast = Toast.makeText(context, text, duration);


        button.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                postData= "TEMP= "+textViewTemp.getText();
                //postData="TEMP=27";
                strUrl= "http://192.168.1.49:8888/Test/API/temperature";
                contentType="application/x-www-form-urlencoded";
                typeRequest = "POST";

                toast.show();

                new HTTPTask().execute(strUrl,typeRequest,postData,contentType);
            }
        });

    }

    //Getting the scan results
    @Override
    protected void onActivityResult(int requestCode, int resultCode, Intent data) {
        IntentResult result = IntentIntegrator.parseActivityResult(requestCode, resultCode, data);
        if (result != null) {
            //if qrcode has nothing in it
            if (result.getContents() == null) {
                Toast.makeText(this, "Result Not Found", Toast.LENGTH_LONG).show();
            } else {
                //if qr contains data
                try {
                    //converting the data to json
                    JSONObject obj = new JSONObject(result.getContents());
                    //setting values to textviews
                    textViewTemp.setText(obj.getString("TEMP"));
                } catch (JSONException e) {
                    e.printStackTrace();
                    //if control comes here
                    //that means the encoded format not matches
                    //in this case you can display whatever data is available on the qrcode
                    //to a toast
                    Toast.makeText(this, result.getContents(), Toast.LENGTH_LONG).show();
                }
            }
        } else {
            super.onActivityResult(requestCode, resultCode, data);
        }
    }
    @Override
    public void onClick(View view) {
        //initiating the qr code scan
        qrScan.initiateScan();
    }

    private class HTTPTask extends AsyncTask<String,Void,String>{

        @Override
        protected String doInBackground(String... params) {
            HttpURLConnection urlConnection = null;
            String strUrl=params[0];
            String typeRequest = params[1];
            String postData = params[2];
            String contentType = params[3];
            StringBuffer buffer = null;

            URL url = null;
            try {
                url = new URL(strUrl);
            } catch (MalformedURLException e) {
                e.printStackTrace();
            }
            try {
                urlConnection = (HttpURLConnection) url.openConnection();
            } catch (IOException e) {
                e.printStackTrace();
            }
            urlConnection.setRequestProperty("Content-Type", contentType);
            try {
                urlConnection.setRequestMethod(typeRequest);
            } catch (ProtocolException e) {
                e.printStackTrace();
            }
            urlConnection.setDoOutput(true);
            urlConnection.setDoInput(true);
            urlConnection.setChunkedStreamingMode(0);

            OutputStream out = null;
            try {
                out = new BufferedOutputStream(urlConnection.getOutputStream());
            } catch (IOException e) {
                e.printStackTrace();
            }
            BufferedWriter writer = null;
            try {
                writer = new BufferedWriter(new OutputStreamWriter(
                        out, "UTF-8"));
            } catch (UnsupportedEncodingException e) {
                e.printStackTrace();
            }
            try {
                writer.write(postData.toString());
            } catch (IOException e) {
                e.printStackTrace();
            }
            try {
                writer.flush();
            } catch (IOException e) {
                e.printStackTrace();
            }
            int code = 0;
            try {
                code = urlConnection.getResponseCode();
            } catch (IOException e) {
                e.printStackTrace();
            }
            if (code !=  200) {
                try {
                    throw new IOException("Invalid response from server: " + code);
                } catch (IOException e) {
                    e.printStackTrace();
                }
            }

            BufferedReader rd = null;
            try {
                rd = new BufferedReader(new InputStreamReader(
                        urlConnection.getInputStream()));
            } catch (IOException e) {
                e.printStackTrace();
            }
            buffer = new StringBuffer();

            while (true) {

                try {
                    if (!((line = rd.readLine()) != null)) break;
                } catch (IOException e) {
                    e.printStackTrace();
                }
                buffer.append(line);
                //resultat = line;
            }
            if (urlConnection != null) {
                urlConnection.disconnect();
            }

            return buffer.toString();
        }
    }
}
package com.example.mantra;

import androidx.annotation.Nullable;

import android.app.Activity;
import android.app.ProgressDialog;
import android.graphics.Bitmap;
import android.graphics.BitmapFactory;
import android.os.Bundle;
import android.util.Log;
import android.view.View;
import android.view.WindowManager;
import android.widget.Button;
import android.widget.EditText;
import android.widget.ImageView;
import android.widget.TextView;
import android.widget.Toast;

import com.android.volley.AuthFailureError;
import com.android.volley.Request;
import com.android.volley.RequestQueue;
import com.android.volley.Response;
import com.android.volley.VolleyError;
import com.android.volley.toolbox.StringRequest;
import com.android.volley.toolbox.Volley;
import com.mantra.mfs100.FingerData;
import com.mantra.mfs100.MFS100;
import com.mantra.mfs100.MFS100Event;

import org.json.JSONArray;
import org.json.JSONException;
import org.json.JSONObject;

import java.util.Base64;
import java.util.HashMap;
import java.util.Map;

public class MainActivity extends Activity implements MFS100Event {

    Button cap, stop_cap, match, save;
    TextView l_message, textView1, textView2;
    ImageView ImageFinger;
    EditText Name;


    String api_savefinger="https://www.schedular.in/MBS/USER/mfs_demo.php";
    String api_match="https://www.schedular.in/MBS/USER/mfs_match.php";
    String finger1="";


    private enum ScannerAction {
        Capture, Verify
    }

    byte[] Enroll_Template;
    byte[] Verify_Template;
    private FingerData lastCapFingerData = null;
    ScannerAction scannerAction = ScannerAction.Capture;

    int timeout = 10000;
    MFS100 mfs100 = null;

    private boolean isCaptureRunning = false;


    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_main);

        try {
            this.getWindow().setSoftInputMode(WindowManager.LayoutParams.SOFT_INPUT_STATE_ALWAYS_HIDDEN);
        } catch (Exception e) {
            Log.e("Error", e.toString());
        }

        cap = findViewById(R.id.button);
        stop_cap = findViewById(R.id.button2);
        l_message = findViewById(R.id.textView2);
        ImageFinger = findViewById(R.id.imgFinger);
        match = findViewById(R.id.match);
        textView2 = findViewById(R.id.textView1);
        save=findViewById(R.id.save);
        Name=findViewById(R.id.name);
        textView1=findViewById(R.id.textView1);

        match.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View view) {
                ImageFinger.setImageResource(R.drawable.ic_baseline_fingerprint_24);
                new Thread(new Runnable() {
                    @Override
                    public void run() {
                        SetTextOnUIThread("started.. matching.......");
                        isCaptureRunning = true;
                        try {
                            FingerData fingerData = new FingerData();
                            int ret = mfs100.AutoCapture(fingerData, timeout, true);
                            Log.e("StartSyncCapture.RET", ""+ret);
                            if (ret != 0) {
                                SetTextOnUIThread(mfs100.GetErrorMsg(ret));
                            } else {
                                lastCapFingerData = fingerData;
                                final Bitmap bitmap = BitmapFactory.decodeByteArray(fingerData.FingerImage(), 0,
                                        fingerData.FingerImage().length);
                                MainActivity.this.runOnUiThread(new Runnable() {
                                    @Override
                                    public void run() {
                                        ImageFinger.setImageBitmap(bitmap);
                                        Verify_Template =fingerData.ISOTemplate();
                                        match_finger();
                                    }
                                });
                            }
                        } catch (Exception ex) {
                            SetTextOnUIThread("Error");
                        } finally {
                            isCaptureRunning = false;
                        }
                    }
                }).start();
            }
        });

        cap.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View view) {
                new Thread(new Runnable() {

                    @Override
                    public void run() {
                        SetTextOnUIThread("capturing.....");
                        isCaptureRunning = true;
                        try {
                            FingerData fingerData = new FingerData();
                            int ret = mfs100.AutoCapture(fingerData, timeout, true);
                            Log.e("StartSyncCapture.RET", ""+ret);
                            if (ret != 0) {
                                SetTextOnUIThread(mfs100.GetErrorMsg(ret));
                            } else {
                                lastCapFingerData = fingerData;
                                final Bitmap bitmap = BitmapFactory.decodeByteArray(fingerData.FingerImage(), 0,
                                        fingerData.FingerImage().length);
                                MainActivity.this.runOnUiThread(new Runnable() {
                                    @Override
                                    public void run() {
                                        ImageFinger.setImageBitmap(bitmap);
                                        SetTextOnUIThread("capture..success....");
                                    }
                                });

                                 finger1 = Base64.getEncoder().encodeToString(fingerData.ISOTemplate());
                              //  Enroll_Template= Base64.getDecoder().decode(finger1);
                            }
                        } catch (Exception ex) {
                            SetTextOnUIThread("Error");
                        } finally {
                            isCaptureRunning = false;
                        }
                    }
                }).start();
            }
        });

        stop_cap.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View view) {
                StopCapture();
            }
        });

        save.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View view) {
                ProgressDialog progressDialog = new ProgressDialog(MainActivity.this);
                progressDialog.setMessage("Saving details");
                progressDialog.show();

                StringRequest save_data =new StringRequest(Request.Method.POST, api_savefinger, new Response.Listener<String>() {
                    @Override
                    public void onResponse(String response) {
                        try {
                            JSONObject jsonObject = new JSONObject(response);
                            String status = jsonObject.getString("status");
                            JSONArray jsonArray=jsonObject.getJSONArray("details");
                            if (status.equals("1")){
                                progressDialog.dismiss();
                                Enroll_Template=null;
                                Toast.makeText(MainActivity.this, "success: ", Toast.LENGTH_SHORT).show();
                            }else {
                                progressDialog.dismiss();
                                Toast.makeText(MainActivity.this, "not success : "+status, Toast.LENGTH_SHORT).show();
                            }

                        } catch (JSONException e) {
                            Toast.makeText(MainActivity.this, "catch : "+e.getMessage(), Toast.LENGTH_SHORT).show();
                            progressDialog.dismiss();
                        }
                    }
                }, new Response.ErrorListener() {
                    @Override
                    public void onErrorResponse(VolleyError error) {
                        progressDialog.dismiss();
                        Toast.makeText(MainActivity.this, "network error", Toast.LENGTH_SHORT).show();
                    }
                }){
                    @Nullable
                    @Override
                    protected Map<String, String> getParams() throws AuthFailureError {
                        Map<String, String> data = new HashMap<>();
                        data.put("name", Name.getText().toString());
                        data.put("finger1", finger1);
                        return data;
                    }
                };

                RequestQueue requestQueue = Volley.newRequestQueue(MainActivity.this);
                requestQueue.add(save_data);
            }
        });


    }

    private void match_finger() {

        StringRequest match = new StringRequest(Request.Method.POST, api_match, new Response.Listener<String>() {
            @Override
            public void onResponse(String response) {

                String finger="";
                try {
                    JSONObject jsonObject = new JSONObject(response);
                    String status = jsonObject.getString("status");
                    JSONArray jsonArray=jsonObject.getJSONArray("details");
                    Toast.makeText(MainActivity.this, "status : "+status, Toast.LENGTH_SHORT).show();
                    if (status.equals("1")){
                        for (int i=0; i<jsonArray.length(); i++){
                            JSONObject object=jsonArray.getJSONObject(i);
                             finger = object.getString("finger");
                        }

                        Enroll_Template=Base64.getDecoder().decode(finger);
                        Toast.makeText(MainActivity.this, "matching", Toast.LENGTH_SHORT).show();
                        int ret = mfs100.MatchISO(Enroll_Template, Verify_Template);
                        if (ret < 0) {
                            SetTextOnUIThread("Error: " + ret + "(" + mfs100.GetErrorMsg(ret) + ")");
                        } else {
                            if (ret >= 1400) {
                                SetTextOnUIThread("Finger matched with score: " + ret);
                                return;
                            } else {
                                SetTextOnUIThread("Finger not matched, score: " + ret);
                            }
                        }

                    }
                } catch (JSONException e) {
                    Toast.makeText(MainActivity.this, "catch : "+e.getMessage(), Toast.LENGTH_SHORT).show();
                }

            }
        }, new Response.ErrorListener() {
            @Override
            public void onErrorResponse(VolleyError error) {
                Toast.makeText(MainActivity.this, "network error", Toast.LENGTH_SHORT).show();
            }
        }){
            @Nullable
            @Override
            protected Map<String, String> getParams() throws AuthFailureError {
                Map<String, String> data = new HashMap<>();
                data.put("name", Name.getText().toString());
                return data;
            }
        };
        RequestQueue requestQueue=Volley.newRequestQueue(MainActivity.this);
        requestQueue.add(match);




    }

    private void SetTextOnUIThread(final String str) {

        l_message.post(new Runnable() {
            public void run() {
                l_message.setText(str);
            }
        });
    }

    @Override
    protected void onStart() {
        if (mfs100 == null) {
            mfs100 = new MFS100(this);
            mfs100.SetApplicationContext(MainActivity.this);
        } else {
            InitScanner();
        }
        super.onStart();
    }

    private void InitScanner() {
        try {
            int ret = mfs100.Init();
            if (ret != 0) {
                SetTextOnUIThread(mfs100.GetErrorMsg(ret));
            } else {
                SetTextOnUIThread("Init success");
            }
        } catch (Exception ex) {
            Toast.makeText(this, "Init failed, unhandled exception",
                    Toast.LENGTH_LONG).show();
            SetTextOnUIThread("Init failed, unhandled exception");
        }
    }

    protected void onStop() {
        UnInitScanner();
        super.onStop();
    }

    @Override
    protected void onDestroy() {
        if (mfs100 != null) {
            mfs100.Dispose();
        }
        super.onDestroy();
    }

    private void StopCapture() {
        try {
            mfs100.StopAutoCapture();
        } catch (Exception e) {
            SetTextOnUIThread("Error");
        }
    }

    @Override
    public void OnDeviceAttached(int vid, int pid, boolean hasPermission) {
        int ret;
        if (!hasPermission) {
            SetTextOnUIThread("Permission denied");
            return;
        }
        if (vid == 1204 || vid == 11279) {
            if (pid == 34323) {
                ret = mfs100.LoadFirmware();
                if (ret != 0) {
                    SetTextOnUIThread(mfs100.GetErrorMsg(ret));
                } else {
                    SetTextOnUIThread("Load firmware success");
                }
            } else if (pid == 4101) {
                String key = "Without Key";
                ret = mfs100.Init();
                if (ret == 0) {
                    SetTextOnUIThread("Init success");
                } else {
                    SetTextOnUIThread(mfs100.GetErrorMsg(ret));
                }

            }
        }
    }

    @Override
    public void OnDeviceDetached() {
        UnInitScanner();
        SetTextOnUIThread("Device removed");
    }

    private void UnInitScanner() {
        try {
            int ret = mfs100.UnInit();
            if (ret != 0) {
                SetTextOnUIThread(mfs100.GetErrorMsg(ret));
            } else {
               // SetLogOnUIThread("Uninit Success");
                SetTextOnUIThread("Uninit Success");
                lastCapFingerData = null;
            }
        } catch (Exception e) {
            Log.e("UnInitScanner.EX", e.toString());
        }
    }

    @Override
    public void OnHostCheckFailed(String s) {
        try {
            Toast.makeText(this, "Error "+ s, Toast.LENGTH_LONG).show();
        } catch (Exception ignored) {
            Log.e("HostCheckFailed", ignored.toString());
        }
    }



}
package com.example.mantra;

import androidx.appcompat.app.AppCompatActivity;

import android.app.Activity;
import android.graphics.Bitmap;
import android.graphics.BitmapFactory;
import android.os.Bundle;
import android.util.Log;
import android.view.View;
import android.view.WindowManager;
import android.widget.Button;
import android.widget.ImageView;
import android.widget.TextView;
import android.widget.Toast;

import com.mantra.mfs100.FingerData;
import com.mantra.mfs100.MFS100;
import com.mantra.mfs100.MFS100Event;

public class MainActivity extends Activity implements MFS100Event {

    Button cap, stop_cap, match;
    TextView l_message;
    ImageView ImageFinger;



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
        l_message = findViewById(R.id.textView);
        ImageFinger = findViewById(R.id.imgFinger);
        match = findViewById(R.id.match);

        match.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View view) {
                new Thread(new Runnable() {

                    @Override
                    public void run() {
                        SetTextOnUIThread("");
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
                                        Verify_Template = fingerData.ISOTemplate();
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
                        SetTextOnUIThread("");
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
                                    }
                                });
                                Enroll_Template = fingerData.ISOTemplate();
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

    }

    private void match_finger() {
        Toast.makeText(this, "into match", Toast.LENGTH_SHORT).show();
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

    private void SetTextOnUIThread(final String str) {

        l_message.post(new Runnable() {
            public void run() {
                l_message.setText(str);
            }
        });
    }

}
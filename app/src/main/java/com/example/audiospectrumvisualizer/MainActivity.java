package com.example.audiospectrumvisualizer;

import androidx.annotation.NonNull;
import androidx.appcompat.app.AppCompatActivity;
import androidx.core.app.ActivityCompat;
import androidx.core.content.ContextCompat;

import android.Manifest;
import android.app.AlertDialog;
import android.content.DialogInterface;
import android.content.Intent;
import android.content.pm.PackageManager;
import android.content.res.ColorStateList;
import android.graphics.Color;
import android.media.MediaPlayer;
import android.media.audiofx.Visualizer;
import android.net.Uri;
import android.os.Bundle;
import android.provider.Settings;
import android.util.Log;
import android.view.MotionEvent;
import android.view.View;
import android.widget.Button;

import com.google.ar.core.Anchor;
import com.google.ar.core.HitResult;
import com.google.ar.core.Plane;
import com.google.ar.sceneform.AnchorNode;
import com.google.ar.sceneform.Node;
import com.google.ar.sceneform.math.Vector3;
import com.google.ar.sceneform.rendering.MaterialFactory;
import com.google.ar.sceneform.rendering.ModelRenderable;
import com.google.ar.sceneform.rendering.ShapeFactory;
import com.google.ar.sceneform.rendering.ViewRenderable;
import com.google.ar.sceneform.ux.ArFragment;
import com.google.ar.sceneform.ux.TransformableNode;

import java.nio.ByteBuffer;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

public class MainActivity extends AppCompatActivity {


    private ArFragment arFragment;

    // Cube variables
    private ModelRenderable cubeRenderable;
    private AnchorNode anchorNode;
    private Node cubeNode;
    private float cubeLastLocalScale = 0f;


    enum PlaybackState
    {
        Playing,
        Paused
    }
    private PlaybackState playbackState;
    private MediaPlayer mediaPlayer;
    private Visualizer visualizer;
    private byte[] FFTData;


    private static final int PERMISSIONS_REQUEST_CODE = 200;
    private String[] permissions = {Manifest.permission.RECORD_AUDIO};

    @Override
    public void onRequestPermissionsResult(int requestCode, @NonNull String[] permissions, @NonNull int[] grantResults) {
        if (requestCode == PERMISSIONS_REQUEST_CODE)
        {
            HashMap<String, Integer> permissionResults = new HashMap<>();
            int deniedCount = 0;

            // Gather permission grant results
            for (int i=0; i<grantResults.length; i++)
            {
                // Add only permissions which are denied
                if (grantResults[i] == PackageManager.PERMISSION_DENIED)
                {
                    permissionResults.put(permissions[i], grantResults[i]);
                    deniedCount++;
                }
            }

            // Check if all permissions are granted
            if (deniedCount == 0)
            {
                // Proceed ahead with the app
                InitializeApp();
            }
            // Atleast one or all permissions are denied
            else
            {
                for (Map.Entry<String, Integer> entry : permissionResults.entrySet())
                {
                    String permName = entry.getKey();
                    int permResult = entry.getValue();

                    // permission is denied (this is the first time, when "never ask again" is not checked)
                    // so ask again explaining the usage of permission
                    // shouldShowRequestPermissionRationale will return true
                    if (ActivityCompat.shouldShowRequestPermissionRationale(this, permName))
                    {
                        // Show dialog of explanation
                        showDialog("", "This app needs Location and Storage permissions to work wihout any issues and problems.",
                                "Yes, Grant permissions",
                                new DialogInterface.OnClickListener() {
                                    @Override
                                    public void onClick(DialogInterface dialogInterface, int i) {
                                        dialogInterface.dismiss();
                                        checkAndRequestPermissions();
                                    }
                                },
                                "No, Exit app", new DialogInterface.OnClickListener() {
                                    @Override
                                    public void onClick(DialogInterface dialogInterface, int i) {
                                        dialogInterface.dismiss();
                                        finish();
                                    }
                                }, false);
                    }
                    //permission is denied (and never ask again is  checked)
                    //shouldShowRequestPermissionRationale will return false
                    else
                    {
                        // Ask user to go to settings and manually allow permissions
                        showDialog("", "You have denied some permissions to the app. Please allow all permissions at [Setting] > [Permissions] screen",
                                "Go to Settings",
                                new DialogInterface.OnClickListener() {
                                    @Override
                                    public void onClick(DialogInterface dialogInterface, int i) {
                                        dialogInterface.dismiss();
                                        // Go to app settings
                                        Intent intent = new Intent(Settings.ACTION_APPLICATION_DETAILS_SETTINGS,
                                                Uri.fromParts("package", getPackageName(), null));
                                        intent.addFlags(Intent.FLAG_ACTIVITY_NEW_TASK);
                                        startActivity(intent);
                                        finish();
                                    }
                                },
                                "No, Exit app", new DialogInterface.OnClickListener() {
                                    @Override
                                    public void onClick(DialogInterface dialogInterface, int i) {
                                        dialogInterface.dismiss();
                                        finish();
                                    }
                                }, false);
                        break;
                    }
                }
            }
        }
    }
    public AlertDialog showDialog(String title, String msg, String positiveLabel,
                                  DialogInterface.OnClickListener positiveOnClick,
                                  String negativeLabel, DialogInterface.OnClickListener negativeOnClick,
                                  boolean isCancelAble)
    {
        AlertDialog.Builder builder = new AlertDialog.Builder(this);
        builder.setTitle(title);
        builder.setCancelable(isCancelAble);
        builder.setMessage(msg);
        builder.setPositiveButton(positiveLabel, positiveOnClick);
        builder.setNegativeButton(negativeLabel, negativeOnClick);

        AlertDialog alert = builder.create();
        alert.show();
        return alert;
    }

    public boolean checkAndRequestPermissions()
    {
        List<String> listPermissionsNeeded = new ArrayList<>();
        for(String perm : permissions)
        {
            if(ContextCompat.checkSelfPermission(this, perm) != PackageManager.PERMISSION_GRANTED)
            {
                listPermissionsNeeded.add(perm);
            }
        }

        if(!listPermissionsNeeded.isEmpty())
        {
            ActivityCompat.requestPermissions(
                    this,
                    listPermissionsNeeded.toArray(new String[listPermissionsNeeded.size()]),
                    PERMISSIONS_REQUEST_CODE
            );
            return false;
        }
        return true;
    }

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_main);


        //Check if permission to record audio is granted
        if(ContextCompat.checkSelfPermission(this, Manifest.permission.RECORD_AUDIO) != PackageManager.PERMISSION_GRANTED)
        {
            // Show an explanation why permission is needed
            if (ActivityCompat.shouldShowRequestPermissionRationale(this,
                    Manifest.permission.RECORD_AUDIO)) {
                InitializeApp();
            } else {
                // No explanation needed; request the permission
                ActivityCompat.requestPermissions(this,
                        new String[]{Manifest.permission.READ_CONTACTS},
                        PERMISSIONS_REQUEST_CODE);

            }
        } else
        {
            InitializeApp();
        }


    }

    private void InitializeApp()
    {
        // Initialize the variables
        arFragment = (ArFragment) getSupportFragmentManager().findFragmentById(R.id.ux_fragment);
        mediaPlayer = MediaPlayer.create(getApplicationContext(), R.raw.music1);
        playbackState = PlaybackState.Paused;
        visualizer = new Visualizer(mediaPlayer.getAudioSessionId());
        visualizer.setCaptureSize(256);

        // Get the FFT date and save it in FFTData variable
        visualizer.setDataCaptureListener(new Visualizer.OnDataCaptureListener() {
            @Override
            public void onWaveFormDataCapture(Visualizer visualizer, byte[] bytes, int i) {

            }

            @Override
            public void onFftDataCapture(Visualizer visualizer, byte[] bytes, int o) {
                // Obtain magnitude and phase values
                int n = bytes.length;
                float[] magnitudes = new float[n / 2 + 1];
                float[] phases = new float[n / 2 + 1];
                magnitudes[0] = (float)Math.abs(bytes[0]);      // DC
                magnitudes[n / 2] = (float)Math.abs(bytes[1]);  // Nyquist
                phases[0] = phases[n / 2] = 0;
                for (int k = 1; k < n / 2; k++) {
                    int i = k * 2;
                    magnitudes[k] = (float)Math.hypot(bytes[i], bytes[i + 1]);
                    phases[k] = (float)Math.atan2(bytes[i + 1], bytes[i]);
                }

                if(cubeRenderable != null && cubeNode != null)
                {
                    double lowFrequencyAverage = 0f;
                    int frequencySamples = 10;

                    // Get the average of the lower frequencies
                    for (int a = 0; a < frequencySamples; a++)
                    {
                        lowFrequencyAverage += magnitudes[a];
                    }
                    lowFrequencyAverage /= 10*frequencySamples;

                    Log.i("MAGNITUDES: ", Double.toString(Math.exp(1 + lowFrequencyAverage)));
                    Log.i("INFO: ", "set local scale" + cubeNode.getLocalScale());

                    float cubeScale = lerp(cubeLastLocalScale, (float) (1f + 1f*lowFrequencyAverage), 0.7f);
                    cubeNode.setLocalScale(new Vector3(
                            cubeScale,
                            cubeScale,
                            cubeScale));
                    cubeLastLocalScale = cubeScale;
                }
            }
            }, Visualizer.getMaxCaptureRate()/2, false, true);

        // Enable the visualizer
        visualizer.setEnabled(true);
        // Once the song has ended, set disable the visualizer
        mediaPlayer.setOnCompletionListener(mediaPlayer -> visualizer.setEnabled(false));

        //Create a sphere with a given color, material, size and position
        MaterialFactory.makeOpaqueWithColor(this, new com.google.ar.sceneform.rendering.Color(android.graphics.Color.GRAY))
                .thenAccept(
                        material -> {
                            cubeRenderable =
                                    ShapeFactory.makeCube(new Vector3(0.1f, 0.1f, 0.1f), new Vector3(0.0f, 0.15f, 0.0f), material);
                        });


        // When clicked on a plane in the app, a certain object (cube in this case) will be spawned in place
        arFragment.setOnTapArPlaneListener(
                (HitResult hitResult, Plane plane, MotionEvent motionEvent) ->
                {
                    if(cubeRenderable == null)
                        return;

                    // Create the anchor
                    Anchor anchor = hitResult.createAnchor();
                    if(anchorNode == null)
                    {
                        anchorNode = new AnchorNode(anchor);
                        anchorNode.setParent(arFragment.getArSceneView().getScene());

                        cubeNode = new Node();
                        cubeNode.setParent(anchorNode);
                        cubeNode.setRenderable(cubeRenderable);
                        cubeNode.setLocalScale(Vector3.one());
                    }
                }
        );
    }

    private float lerp(float a, float b, float f)
    {
        return a + f * (b - a);
    }

    @Override
    protected void onResume()
    {
        super.onResume();
    }

    @Override
    protected void onPause()
    {
        if(visualizer != null)
        {
            visualizer.setEnabled(false);
            visualizer.release();
        }
        super.onPause();
        //mediaPlayer.stop();
        //mediaPlayer.release();
    }

    public void playbackStateChanger(View view)
    {
        Button playbackButton = findViewById(R.id.play_button);
        if(playbackState == PlaybackState.Paused)
        {
            playMusic(playbackButton);
        }
        else
        {
            pauseMusic(playbackButton);
        }
    }
    public void playMusic(Button playButton)
    {
        mediaPlayer.start();
        playbackState = PlaybackState.Playing;
        playButton.setText("Pause");
    }

    public void pauseMusic(Button playButton)
    {
        mediaPlayer.pause();
        playbackState = PlaybackState.Paused;
        playButton.setText("Play");
    }

}

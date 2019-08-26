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
    private Visualizer visualizer = null;


    private static final int PERMISSIONS_REQUEST_CODE = 200;
    private String[] permissions = {Manifest.permission.RECORD_AUDIO};


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
        if(ContextCompat.checkSelfPermission(this, Manifest.permission.RECORD_AUDIO) == PackageManager.PERMISSION_GRANTED)
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

        startVisualizer();

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

    private void startVisualizer()
    {
        visualizer = new Visualizer(mediaPlayer.getAudioSessionId());
        visualizer.setCaptureSize(visualizer.getCaptureSize());

        // Get the FFT date and save it in FFTData variable
        visualizer.setDataCaptureListener(new Visualizer.OnDataCaptureListener() {
            @Override
            public void onWaveFormDataCapture(Visualizer visualizer, byte[] bytes, int i) {
                if(cubeRenderable != null && cubeNode != null)
                {
                    float lowFrequencyAverage = 0f;
                    float frequencySamples = 32;

                    float div = bytes.length / 256;


                    for (int a = 0; a < frequencySamples; a++)
                    {
                        int bytePos = (int) Math.ceil(a * div);
                        int byteVal = Math.abs((byte) (unsignedToBytes(bytes[bytePos])));
                        lowFrequencyAverage += byteVal;
                    }
                    lowFrequencyAverage /= 100*frequencySamples;

                    float cubeScale = lerp(1f + cubeLastLocalScale,(1f + lowFrequencyAverage), 0.2f);
                    Log.i("MAGNITUDES: ", Float.toString(cubeScale));
                    cubeNode.setLocalScale(new Vector3(
                            cubeScale,
                            cubeScale,
                            cubeScale));
                    cubeLastLocalScale = cubeScale-1;
                }

            }

            @Override
            public void onFftDataCapture(Visualizer visualizer, byte[] bytes, int o) {

            }
        }, Visualizer.getMaxCaptureRate(), true, false);

        // Enable the visualizer
        visualizer.setEnabled(true);
    }

    // Convert 8-Bit unsigned into normal byte
    public static int unsignedToBytes(byte b) {
        return b & 0xFF;
    }

    // Linear interpolation between who values.
    private float lerp(float a, float b, float f)
    {
        return a + f * (b - a);
    }

    @Override
    protected void onResume()
    {
        super.onResume();
        checkAndRequestPermissions();
    }

    @Override
    protected void onPause()
    {

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

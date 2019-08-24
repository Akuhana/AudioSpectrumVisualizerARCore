package com.example.audiospectrumvisualizer;

import androidx.appcompat.app.AppCompatActivity;

import android.content.res.ColorStateList;
import android.graphics.Color;
import android.media.MediaPlayer;
import android.net.Uri;
import android.os.Bundle;
import android.view.MotionEvent;
import android.view.View;
import android.widget.Button;

import com.google.ar.core.Anchor;
import com.google.ar.core.HitResult;
import com.google.ar.core.Plane;
import com.google.ar.sceneform.AnchorNode;
import com.google.ar.sceneform.math.Vector3;
import com.google.ar.sceneform.rendering.MaterialFactory;
import com.google.ar.sceneform.rendering.ModelRenderable;
import com.google.ar.sceneform.rendering.ShapeFactory;
import com.google.ar.sceneform.rendering.ViewRenderable;
import com.google.ar.sceneform.ux.ArFragment;
import com.google.ar.sceneform.ux.TransformableNode;

public class MainActivity extends AppCompatActivity {

    private ModelRenderable cubeRenderable;
    private ArFragment arFragment;

    enum PlaybackState
    {
        Playing,
        Paused
    }
    private PlaybackState playbackState;
    private MediaPlayer mediaPlayer;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_main);

        // Initialize the variables
        arFragment = (ArFragment) getSupportFragmentManager().findFragmentById(R.id.ux_fragment);
        mediaPlayer = MediaPlayer.create(getApplicationContext(), R.raw.music1);
        playbackState = PlaybackState.Paused;


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
                    AnchorNode anchorNode = new AnchorNode(anchor);
                    anchorNode.setParent(arFragment.getArSceneView().getScene());

                    TransformableNode cubeNode = new TransformableNode(arFragment.getTransformationSystem());
                    cubeNode.setParent(anchorNode);
                    cubeNode.setRenderable(cubeRenderable);
                    cubeNode.select();
                }
        );
    }

    @Override
    protected void onResume()
    {
        super.onResume();
    }

    @Override
    protected void onPause()
    {
        super.onPause();
        mediaPlayer.stop();
        mediaPlayer.release();
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

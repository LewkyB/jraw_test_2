package com.example.jraw_test_2;

import com.example.jraw_test_2.Classifier;

import android.graphics.Bitmap;
import android.graphics.ColorSpace;
import android.os.Bundle;
import android.util.Log;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.Button;
import android.widget.Toast;
import android.app.Activity;
import android.content.res.AssetFileDescriptor;

import android.graphics.BitmapFactory;

import java.io.*;
import java.nio.channels.*;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.fragment.app.Fragment;

import androidx.annotation.Nullable;
import androidx.fragment.app.Fragment;

import com.google.android.gms.tasks.OnCompleteListener;
import com.google.android.gms.tasks.OnFailureListener;
import com.google.android.gms.tasks.OnSuccessListener;
import com.google.android.gms.tasks.Task;
import com.google.firebase.auth.FirebaseAuth;
import com.google.firebase.database.FirebaseDatabase;
import com.google.firebase.storage.FirebaseStorage;
import com.google.firebase.storage.StorageReference;
import com.google.firebase.storage.UploadTask;


import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.util.UUID;

public class PaintFragment extends Fragment implements View.OnClickListener {

    public static final String TAG = "PaintFragment";

    FirebaseAuth mAuth;

    PaintView paintView;
    public Button uploadButton, undoButton, clearButton;

    Bitmap bmp;

    private Classifier tf;

    @Override
    public View onCreateView(LayoutInflater inflater, ViewGroup container,
                             Bundle savedInstanceState) {
        Log.d(TAG, "PaintFragment onCreateView()");

        View view = inflater.inflate(R.layout.fragment_paint, container, false);

        paintView = view.findViewById(R.id.PaintView);
        // testButton = view.findViewById(R.id.test_button);

        // used for testing upload functionality
        uploadButton = view.findViewById(R.id.upload_button);
        uploadButton.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                try {
                    storeBitmapFirebase();
                } catch (IOException e) {
                    e.printStackTrace();
                }
            }
        });

        undoButton = view.findViewById(R.id.undo_button);
        undoButton.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) { paintView.undo(); }
        });

        clearButton = view.findViewById(R.id.clear_button);
        clearButton.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) { paintView.clear(); }
        });

        return view;
    }

    @Override
    public void onCreate(@Nullable Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        Log.d(TAG, "PaintFragment onCreate()");
        mAuth = FirebaseAuth.getInstance(); // start FirebaseAuth
        tf = new Classifier(this.getActivity()); //Create the classifier
    }

    @Override
    public void onStart() {
        super.onStart();
        Log.d(TAG, "PaintFragment onStart()");
    }

    @Override
    public void onDestroy() {
        super.onDestroy();
        Log.d(TAG, "PaintFragment onDestroy()");
    }

    // takes image drawn on canvas as a bitmap, converts to jpg, then uploads to firebase storage
    public void storeBitmapFirebase() throws IOException {
        Log.d(TAG, "starting storeBitmapFirebase()");

        FirebaseStorage storage = FirebaseStorage.getInstance();
        StorageReference storageRef = storage.getReferenceFromUrl("gs://jrawtest.appspot.com");

        // create random UUID for unique file name
        String randomUUID = UUID.randomUUID().toString();

        // Create a reference to "mountains.jpg"
        StorageReference imageRef = storageRef.child(randomUUID);

        // Create a reference to 'images/mountains.jpg'
        StorageReference userImagesRef = storageRef.child("user_images/" + randomUUID);

        // While the file names are the same, the references point to different files
        imageRef.getName().equals(userImagesRef.getName());    // true
        imageRef.getPath().equals(userImagesRef.getPath());    // false

        // get bitmap from paintView
        paintView.setDrawingCacheEnabled(true);
        paintView.buildDrawingCache();
        bmp = paintView.getDrawingCache();
        Classifier.ModelOutput out = tf.classify(bmp);

        // convert image
        ByteArrayOutputStream baos = new ByteArrayOutputStream();
        bmp.compress(Bitmap.CompressFormat.JPEG, 100, baos);
        byte[] data = baos.toByteArray();

        // send image to firebase storage
        UploadTask uploadTask = imageRef.putBytes(data);
        uploadTask.addOnFailureListener(new OnFailureListener() {
            @Override
            public void onFailure(@NonNull Exception exception) {
                Log.d(TAG, "Firebase storeBitmap:failure");
                Toast.makeText(getContext(), "Upload Failure!", Toast.LENGTH_LONG).show();
            }
        }).addOnSuccessListener(new OnSuccessListener<UploadTask.TaskSnapshot>() {
            @Override
            public void onSuccess(UploadTask.TaskSnapshot taskSnapshot) {
                Log.d(TAG, "Firebase storeBitmap:success");
                Toast.makeText(getContext(), "Upload Success!", Toast.LENGTH_LONG).show();

                // add image's randomUUID to list of images the user has submitted
                FirebaseDatabase.getInstance().getReference("Users")
                        .child(FirebaseAuth.getInstance().getCurrentUser().getUid())
                        .child("imageList").push().setValue(randomUUID)
                        .addOnCompleteListener(new OnCompleteListener<Void>() {
                    @Override
                    public void onComplete(@NonNull Task<Void> task) {
                        if (task.isSuccessful()) {
                            Log.d(TAG, "Firebase storeBitmapFirebase add image to user's image list:success");
                        } else {
                            Log.d(TAG, "Firebase storeBitmapFirebase add image to user's image list:failure");
                        }
                    }
                });
            }
        });
    }

    @Override
    public void onClick(View v) {
        // forced to implement this method by IDE
    }
}
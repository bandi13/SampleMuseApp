package com.bandilabs.samplemuseapp;

import android.Manifest;
import android.app.AlertDialog;
import android.bluetooth.BluetoothAdapter;
import android.content.DialogInterface;
import android.content.pm.PackageManager;
import android.os.Bundle;
import android.support.v4.app.ActivityCompat;
import android.support.v4.content.ContextCompat;
import android.support.v7.app.AppCompatActivity;
import android.view.View;
import android.widget.Button;

import com.choosemuse.libmuse.MuseManagerAndroid;

import org.tensorflow.DataType;
import org.tensorflow.Graph;
import org.tensorflow.Output;
import org.tensorflow.Session;
import org.tensorflow.Tensor;

import java.util.ArrayList;

public class MainActivity extends AppCompatActivity {
    private MuseManagerAndroid manager;

    // runs without a timer by reposting this handler at the end of the runnable
    RunnableStateMachine timerRunnable;

    // Based off of: https://stackoverflow.com/questions/26097513/android-simple-alert-dialog
    private void alertView( String message, DialogInterface.OnClickListener onOk, DialogInterface.OnClickListener onCancel ) {
         AlertDialog.Builder dialog = new AlertDialog.Builder(this);
             dialog.setTitle( "Alert" )
                   .setMessage(message);
             if(onCancel != null) dialog.setNegativeButton("Cancel", onCancel);
             if(onOk != null) dialog.setPositiveButton("Ok", onOk);
             dialog.show();
    }
    private void alertViewCloseApp(String message) {
        alertView(message, new DialogInterface.OnClickListener() {
            @Override
            public void onClick(DialogInterface dialog, int which) { MainActivity.this.finish(); }
        }, null);

    }

    final int PERMISSIONS_REQUEST_STARTUP = 0;
    @Override
    public void onRequestPermissionsResult(int requestCode, String permissions[], int[] grantResults) {
        switch (requestCode) {
            case PERMISSIONS_REQUEST_STARTUP: {
                if(grantResults.length == 0) return; // If request is cancelled, the result arrays are empty.
                for(int i = 0; i < permissions.length; i++) {
                    if (grantResults[i] != PackageManager.PERMISSION_GRANTED) {
                        alertViewCloseApp("No permission for: " + permissions[i]);
                        break;
                    }
                }
            }
        }
    }

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_main);
        Button buttonname = (Button) findViewById(R.id.button) ;
        buttonname.setOnClickListener(new View.OnClickListener() {
           @Override
           public void onClick(View view) {
               Graph graph = new Graph();
               Session session = new Session(graph);
               // Construct a graph to add two float Tensors, using placeholders.
               Output x = graph.opBuilder("Placeholder", "x").setAttr("dtype", DataType.FLOAT).build().output(0);
               Output y = graph.opBuilder("Placeholder", "y").setAttr("dtype", DataType.FLOAT).build().output(0);
               Output z = graph.opBuilder("Add", "z").addInput(x).addInput(y).build().output(0);
               // Execute the graph multiple times, each time with a different value of x and y
               float[] X = new float[]{1,2,3};
               float[] Y = new float[]{4,5,6};
               for (int i = 0; i < X.length; i++) {
                   try (Tensor tx = Tensor.create(X[i]);
                        Tensor ty = Tensor.create(Y[i]);
                        Tensor tz = session.runner().feed("x", tx).feed("y", ty).fetch("z").run().get(0)) {
                       System.out.println(X[i] + " + " + Y[i] + " = " + tz.floatValue());
                   }
               }
           }
        });

        String permissions[] = new String[]{Manifest.permission.ACCESS_COARSE_LOCATION,Manifest.permission.BLUETOOTH,Manifest.permission.BLUETOOTH_ADMIN};
        ArrayList<String> permissionsNeeded = new ArrayList<>();
        for(String permission:permissions) {
            if (ContextCompat.checkSelfPermission(this, permission) != PackageManager.PERMISSION_GRANTED) {
                permissionsNeeded.add(permission);
            }
        }
        if(permissionsNeeded.size() != 0)
            ActivityCompat.requestPermissions(this,permissionsNeeded.toArray(new String[0]), PERMISSIONS_REQUEST_STARTUP);

        manager = MuseManagerAndroid.getInstance();
        manager.setContext(this);
        final BluetoothAdapter bluetoothAdapter = BluetoothAdapter.getDefaultAdapter();
        if(bluetoothAdapter == null) {
            alertViewCloseApp(getResources().getString(R.string.no_bluetooth));
        } else {
            if(!bluetoothAdapter.isEnabled()) bluetoothAdapter.enable();
        }
        timerRunnable = new RunnableStateMachine(manager, this);
    }
}

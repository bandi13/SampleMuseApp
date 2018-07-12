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

import com.choosemuse.libmuse.MuseManagerAndroid;

import org.deeplearning4j.nn.conf.NeuralNetConfiguration;
import org.deeplearning4j.nn.conf.layers.DenseLayer;
import org.deeplearning4j.nn.conf.layers.OutputLayer;
import org.deeplearning4j.nn.multilayer.MultiLayerNetwork;
import org.deeplearning4j.nn.weights.WeightInit;
import org.nd4j.linalg.activations.Activation;
import org.nd4j.linalg.api.ndarray.INDArray;
import org.nd4j.linalg.dataset.DataSet;
import org.nd4j.linalg.factory.Nd4j;
import org.nd4j.linalg.lossfunctions.LossFunctions;

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


                    //build the layers of the network
            DenseLayer inputLayer = new DenseLayer.Builder()
                    .nIn(4)
                    .nOut(3)
                    .name("Input")
                    .build();

            DenseLayer hiddenLayer = new DenseLayer.Builder()
                    .nIn(3)
                    .nOut(3)
                    .name("Hidden")
                    .build();

            OutputLayer outputLayer = new OutputLayer.Builder(LossFunctions.LossFunction.NEGATIVELOGLIKELIHOOD)
                    .nIn(3)
                    .nOut(3)
                    .name("Output")
                    .activation(Activation.SOFTMAX)
                    .build();
    }
}

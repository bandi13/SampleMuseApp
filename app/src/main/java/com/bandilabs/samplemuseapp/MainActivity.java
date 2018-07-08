package com.bandilabs.samplemuseapp;

import android.app.AlertDialog;
import android.bluetooth.BluetoothAdapter;
import android.content.DialogInterface;
import android.content.pm.PackageManager;
import android.os.Bundle;
import android.support.annotation.NonNull;
import android.support.design.widget.BottomNavigationView;
import android.support.v4.app.ActivityCompat;
import android.support.v4.content.ContextCompat;
import android.support.v7.app.AppCompatActivity;
import android.view.MenuItem;
import android.widget.TextView;
import com.choosemuse.libmuse.MuseManagerAndroid;

public class MainActivity extends AppCompatActivity {
    private MuseManagerAndroid manager;
    private TextView mTextMessage;

    private BottomNavigationView.OnNavigationItemSelectedListener mOnNavigationItemSelectedListener
            = new BottomNavigationView.OnNavigationItemSelectedListener() {

        @Override
        public boolean onNavigationItemSelected(@NonNull MenuItem item) {
            switch (item.getItemId()) {
                case R.id.navigation_home:
                    mTextMessage.setText(R.string.title_home);
                    return true;
                case R.id.navigation_dashboard:
                    mTextMessage.setText(R.string.title_dashboard);
                    return true;
                case R.id.navigation_notifications:
                    mTextMessage.setText(R.string.title_notifications);
                    return true;
            }
            return false;
        }
    };

    /**
     * Check for Bluetooth.
     * Taken from: https://stackoverflow.com/questions/7672334/how-to-check-if-bluetooth-is-enabled-programmatically
     * @return True if Bluetooth is available.
     */
    public static boolean isBluetoothAvailable() {
        final BluetoothAdapter bluetoothAdapter = BluetoothAdapter.getDefaultAdapter();
        return (bluetoothAdapter != null && bluetoothAdapter.isEnabled());
    }

    // Based off of: https://stackoverflow.com/questions/26097513/android-simple-alert-dialog
    private void alertView( String message, DialogInterface.OnClickListener onOk, DialogInterface.OnClickListener onCancel ) {
         AlertDialog.Builder dialog = new AlertDialog.Builder(this);
             dialog.setTitle( "Alert" )
                   .setMessage(message);
             if(onCancel != null) dialog.setNegativeButton("Cancel", onCancel);
             if(onOk != null) dialog.setPositiveButton("Ok", onOk);
             dialog.show();
    }

    final int MY_PERMISSIONS_REQUEST = 0;
    @Override
    public void onRequestPermissionsResult(int requestCode, String permissions[], int[] grantResults) {
        switch (requestCode) {
            case MY_PERMISSIONS_REQUEST: {
                // If request is cancelled, the result arrays are empty.
                if (grantResults.length > 0 && grantResults[0] == PackageManager.PERMISSION_GRANTED) {
                    // permission was granted, yay!
                } else {
                    alertView(getResources().getString(R.string.no_bluetooth), new DialogInterface.OnClickListener() {
                        @Override
                        public void onClick(DialogInterface dialog, int which) { MainActivity.this.finish(); }
                    }, null);
                }
            }
        }
    }

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_main);

        mTextMessage = (TextView) findViewById(R.id.message);
        BottomNavigationView navigation = (BottomNavigationView) findViewById(R.id.navigation);
        navigation.setOnNavigationItemSelectedListener(mOnNavigationItemSelectedListener);

        String permission = Manifest.permission.ACCESS_COARSE_LOCATION;
        if (ContextCompat.checkSelfPermission(this, permission) != PackageManager.PERMISSION_GRANTED) {
                ActivityCompat.requestPermissions(this, new String[]{permission}, MY_PERMISSIONS_REQUEST);
        }

        manager = MuseManagerAndroid.getInstance();
        manager.setContext(this);
        if(!isBluetoothAvailable()) {
            alertView(getResources().getString(R.string.no_bluetooth), new DialogInterface.OnClickListener() {
                @Override
                public void onClick(DialogInterface dialog, int which) { MainActivity.this.finish(); }
                }, null);
        }
        manager.removeFromListAfter(0);
    }
}

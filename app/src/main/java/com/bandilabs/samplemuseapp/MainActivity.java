package com.bandilabs.samplemuseapp;

import android.Manifest;
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

        mTextMessage = (TextView) findViewById(R.id.message);
        BottomNavigationView navigation = (BottomNavigationView) findViewById(R.id.navigation);
        navigation.setOnNavigationItemSelectedListener(mOnNavigationItemSelectedListener);

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
        manager.removeFromListAfter(0);
    }
}

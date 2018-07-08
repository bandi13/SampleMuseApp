package com.bandilabs.samplemuseapp;

import android.os.Handler;
import android.support.v7.app.AppCompatActivity;
import android.util.Log;
import android.widget.TextView;

import com.choosemuse.libmuse.ConnectionState;
import com.choosemuse.libmuse.Muse;
import com.choosemuse.libmuse.MuseArtifactPacket;
import com.choosemuse.libmuse.MuseConnectionListener;
import com.choosemuse.libmuse.MuseConnectionPacket;
import com.choosemuse.libmuse.MuseDataListener;
import com.choosemuse.libmuse.MuseDataPacket;
import com.choosemuse.libmuse.MuseDataPacketType;
import com.choosemuse.libmuse.MuseManagerAndroid;

import java.text.DecimalFormat;
import java.util.ArrayList;

public class RunnableStateMachine implements Runnable {
    // This is used to keep track of current connections. If we have one open, then curMuse != null and
    // we do not call the timer/runnable
    private Muse curMuse = null;
    private final Handler timerHandler = new Handler();
    private final int HANDLER_DELAY = 100; // timer interval in ms
    private final MuseManagerAndroid manager;
    private final RunnableStateMachine thisRunnable;
    private final TextView txtConnectionInfo;
    private final TextView txtDataInfo;
    private int graphNumPoints = 0;

    public RunnableStateMachine(MuseManagerAndroid manager, AppCompatActivity view) {
        this.manager = manager;
        manager.removeFromListAfter(5); // clean the list after 5 seconds
        this.thisRunnable = this;
        this.txtConnectionInfo = (TextView) view.findViewById(R.id.connectionInfo);
        this.txtDataInfo = (TextView) view.findViewById(R.id.dataInfo);
        timerHandler.postDelayed(this, HANDLER_DELAY);
    }

    @Override
    public void run() {
        Log.i("Runnable","Searching for Muse");
        ArrayList<Muse> muses = manager.getMuses();
        if(muses.size() == 0) {
            txtConnectionInfo.setText("Muses: None detected");
            manager.startListening();
            timerHandler.postDelayed(this, HANDLER_DELAY);
            return;
        }

        manager.stopListening();
        curMuse = muses.get(0);
        Log.i("Runnable","Found muse: "+curMuse.getName());
        txtConnectionInfo.setText("Muse: "+curMuse.getName());
        curMuse.registerDataListener(new MuseDataListener() {
            private String doubleToStr(double value) {
                final DecimalFormat formatter = new DecimalFormat("#.##");
                return formatter.format(value);
            }
            @Override
            public void receiveMuseDataPacket(MuseDataPacket museDataPacket, Muse muse) {
                StringBuilder out = new StringBuilder();
                ArrayList<Double> values = museDataPacket.values();
                for(Double curVal : values) {
                    out.append(doubleToStr(curVal));
                    out.append(' ');
                }
                Log.i("DataPacket","Packet received: "+out.toString());
                txtDataInfo.setText(out.toString());
                graphNumPoints++;
            }

            @Override
            public void receiveMuseArtifactPacket(MuseArtifactPacket museArtifactPacket, Muse muse) {
                Log.i("ArtifactPacket","Packet received: "+museArtifactPacket.toString());
            }
        }, MuseDataPacketType.EEG);
        curMuse.registerConnectionListener(new MuseConnectionListener() {
            @Override
            public void receiveMuseConnectionPacket(MuseConnectionPacket museConnectionPacket, Muse muse) {
                if(museConnectionPacket.getCurrentConnectionState() == ConnectionState.DISCONNECTED) {
                    Log.i("Connection","Disconnected");
                    curMuse.unregisterAllListeners();
                    curMuse = null;
                    manager.startListening();
                    timerHandler.postDelayed(thisRunnable, HANDLER_DELAY);
                }
            }
        });
        curMuse.runAsynchronously();
    }
}

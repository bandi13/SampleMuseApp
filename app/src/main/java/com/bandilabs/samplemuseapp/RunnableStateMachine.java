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
    private Muse curMuse;
    enum State { STOPPED, STARTED, RUNNING }
    private volatile State curState = State.STOPPED;
    private final MuseManagerAndroid manager;
    private final Handler timerHandler;
    private TextView txtConnectionInfo;
    private TextView txtDataInfo;

    public RunnableStateMachine(MuseManagerAndroid manager, Handler timerHandler, AppCompatActivity view) {
        this.manager = manager;
        this.timerHandler = timerHandler;
        this.txtConnectionInfo = (TextView) view.findViewById(R.id.connectionInfo);
        this.txtDataInfo = (TextView) view.findViewById(R.id.dataInfo);
    }

    public void stop() {
        if(curState == State.STOPPED) { return; }
        timerHandler.removeCallbacks(this);
        curState = State.STOPPED;
        if(curMuse != null) { curMuse.disconnect(); curMuse.unregisterAllListeners(); curMuse = null; }
        manager.stopListening();
    }
    public void start() {
        if(curState != State.STOPPED) { return; }
        curState = State.STARTED;
        manager.startListening();
        timerHandler.postDelayed(this, 1000);
    }

    @Override
    public void run() {
        if(curState != State.STARTED) { return; }

        ArrayList<Muse> muses = manager.getMuses();
        if(muses.size() == 0) {
            txtConnectionInfo.setText("Muses: None detected");
            timerHandler.postDelayed(this, 1000);
            return;
        }

        String str = "";
        for(Muse m:muses) {
            if(str.length() != 0) str+='\n';
            str+=m.getName()+'('+m.getRssi()+')';
        }
        manager.stopListening();
        curMuse = muses.get(0);
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
                    muse.runAsynchronously();
                }
            }
        });
        curMuse.runAsynchronously();
        curState = State.RUNNING;
    }
}

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
import com.github.mikephil.charting.charts.LineChart;
import com.github.mikephil.charting.components.YAxis;
import com.github.mikephil.charting.data.Entry;
import com.github.mikephil.charting.data.LineData;
import com.github.mikephil.charting.data.LineDataSet;
import com.github.mikephil.charting.interfaces.datasets.ILineDataSet;

import java.text.DecimalFormat;
import java.util.ArrayList;
import java.util.LinkedList;
import java.util.List;

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
    private final LineChart chart;
    private final List<LineDataSet> chartDataSets;
    private int graphNumPoints = 0;

    public RunnableStateMachine(MuseManagerAndroid manager, AppCompatActivity view) {
        this.manager = manager;
        manager.removeFromListAfter(5); // clean the list after 5 seconds
        this.thisRunnable = this;
        this.txtConnectionInfo = (TextView) view.findViewById(R.id.connectionInfo);
        this.txtDataInfo = (TextView) view.findViewById(R.id.dataInfo);
        this.chart = (LineChart) view.findViewById(R.id.chart);
        this.chartDataSets = new ArrayList<>();
        chart.setData(new LineData());
        chart.setAutoScaleMinMaxEnabled(true);
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
                ArrayList<Double> values = museDataPacket.values();
                int numSets = values.size();
                while(chart.getLineData().getDataSetCount() < numSets) {
                    Log.i("ChartDataSet", "Adding data set to chart");
                    LinkedList<Entry> entryList = new LinkedList<>();
                    entryList.add(new Entry(0,0));
                    LineDataSet dataSet = new LineDataSet(entryList, chart.getLineData().getDataSetCount()+"");
                    dataSet.setAxisDependency(YAxis.AxisDependency.LEFT);
                    // dataSet.setColor(...);
                    chartDataSets.add(dataSet);
                    chart.getLineData().addDataSet(dataSet);
                    chart.notifyDataSetChanged();
                }
                StringBuilder out = new StringBuilder();
                for(int i = 0; i < numSets; i++) {
                    LineDataSet curData = chartDataSets.get(i);
                    curData.addEntry(new Entry(graphNumPoints,values.get(i).floatValue()));
                    if(curData.getEntryCount() > 30) { curData.removeFirst(); }
                    curData.notifyDataSetChanged();
                    out.append(doubleToStr(values.get(i)));
                    out.append(' ');
                }
                String outString = out.toString();
                Log.i("DataPacket","Packet received: "+outString);
                txtDataInfo.setText(outString);
                chart.invalidate();
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

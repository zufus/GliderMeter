package com.skylabmodels.glidermeter;

import android.os.Handler;
import android.util.Log;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.Toast;
import androidx.annotation.NonNull;
import androidx.recyclerview.widget.RecyclerView;
import java.util.List;


public class sklmSensorViewAdapter extends RecyclerView.Adapter<sklmSensorViewHolder>
        implements sklmSensorAdapterInterface {
    private static final String TAG = "sklmAdapter";

    private final List<sklmBLTSensor> sensorList;


    public sklmSensorViewAdapter(List<sklmBLTSensor> list){
        this.sensorList = list;
    }


    @NonNull
    @Override
    public sklmSensorViewHolder onCreateViewHolder(@NonNull ViewGroup parent, int viewType) {
        View layoutView = LayoutInflater.from(parent.getContext()).inflate(R.layout.sklm_sensor_layout, parent, false);
        return new sklmSensorViewHolder(layoutView);
    }

    @Override
    public void onBindViewHolder(@NonNull sklmSensorViewHolder holder, int position) {

        Log.d(TAG, "in onBindViewHolder: processing position " + position);
        if (sensorList != null && position < sensorList.size()){
            sklmBLTSensor sensor = sensorList.get(position);
            Log.d(TAG, "in onBindViewHolder: sensor: " + sensor.getSensorName());
            holder.sensorName.setText(sensor.getSensorName());
            holder.sensorSwitch.setChecked(false);
            holder.sensorSwitch.setOnCheckedChangeListener((compoundButton, isChecked) -> {

            //TODO: Manage connections
            if (!sensor.isAddressValid()) {
                Toast.makeText(compoundButton.getContext(), "Address not valid: please select a sensor", Toast.LENGTH_SHORT).show();
                return;
            }
            if (isChecked) {
                // TODO: Start connection to device.
                Log.d(TAG, sensor.getSensorName() + " has valid address " +
                        sensor.getAddress() + ". Connecting");
                try {
                    sensor.connect(compoundButton.getContext().getApplicationContext());
                    sensor.readData();
                } catch (Exception e) {
                    e.printStackTrace();
                    compoundButton.setChecked(false);
                }

            }

            if (!isChecked)
                sensor.disconnect();
            });
        }
    }

    @Override
    public int getItemCount() {
        return sensorList.size();
    }

    @Override
    public void setSensorHandler(Handler mQueue, int position){
        sklmBLTSensor sensor = sensorList.get(position);
        sensor.setQueue(mQueue);
    }

    @Override
    public void setSensorSwitch(int position, boolean state) {
        //TODO Check if this method can be effectively deleted
    }

    @Override
    public void calibrateSensor(int p) {
        //TODO Check if this method can be effectively deleted
    }

}

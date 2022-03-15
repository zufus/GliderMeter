package com.skylabmodels.glidermeter;

import android.view.View;
import android.widget.TextView;

import androidx.annotation.NonNull;
import androidx.appcompat.widget.SwitchCompat;
import androidx.recyclerview.widget.RecyclerView;

public class sklmSensorViewHolder extends RecyclerView.ViewHolder {

    public TextView sensorName;
    public TextView sensorValue;
    public SwitchCompat sensorSwitch;

    public sklmSensorViewHolder(@NonNull View itemView) {
        super(itemView);
        sensorName = itemView.findViewById(R.id.textViewSensor);
        sensorValue = itemView.findViewById(R.id.textSensorValue);
        sensorSwitch = itemView.findViewById(R.id.switchConnectSensor);
    }
}

package com.skylabmodels.glidermeter;

import android.os.Handler;

public interface sklmSensorAdapterInterface {
    void setSensorHandler(Handler queue, int position);
    void setSensorSwitch(int position, boolean state);
    void calibrateSensor(int position);
}


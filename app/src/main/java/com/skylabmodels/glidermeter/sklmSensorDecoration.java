package com.skylabmodels.glidermeter;

import android.graphics.Rect;
import android.view.View;

import androidx.recyclerview.widget.RecyclerView;

public class sklmSensorDecoration extends RecyclerView.ItemDecoration {
    private int verticalSpaceHeight;

    public sklmSensorDecoration(int verticalSpaceHeight) {
        this.verticalSpaceHeight = verticalSpaceHeight;
    }

    @Override
    public void getItemOffsets(Rect outRect, View view, RecyclerView parent,
                               RecyclerView.State state) {
        outRect.top = verticalSpaceHeight;
        outRect.bottom = verticalSpaceHeight;
    }

}


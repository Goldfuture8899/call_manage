package com.chooloo.www.callmanager.util;

import android.annotation.SuppressLint;
import android.os.Handler;
import android.os.Message;

import org.jetbrains.annotations.NotNull;

@SuppressLint("HandlerLeak")
public class CallTimeHandler extends Handler {
    public static final int TIME_START = 1;
    public static final int TIME_STOP = 0;
    public static final int TIME_UPDATE = 2;
    public static final int REFRESH_RATE = 100;

    private CallTimeHandlerCallbacks mCallbacks;

    public CallTimeHandler(CallTimeHandlerCallbacks callbacks) {
        mCallbacks = callbacks;
    }

    @Override
    public void handleMessage(@NotNull Message msg) {
        super.handleMessage(msg);
        switch (msg.what) {
            case TIME_START:
                mCallbacks.onTimeStart();
                break;
            case TIME_STOP:
                mCallbacks.onTimeStop();
                break;
            case TIME_UPDATE:
                mCallbacks.onTimeUpdate();
                break;
            default:
                break;
        }
    }

    public interface CallTimeHandlerCallbacks {
        void onTimeStart();

        void onTimeStop();

        void onTimeUpdate();
    }
}
package com.chooloo.www.callmanager.mvp;

import android.app.Activity;
import android.content.Context;
import android.view.KeyEvent;
import android.view.ViewGroup;

import androidx.annotation.NonNull;

import org.jetbrains.annotations.NotNull;

public interface CallActivityContract {
    interface Presenter {

        void onBackPressed();

        void onRequestPermissionResult(int requestCode, @NonNull int[] grantResults);

        void onKeyPressed(KeyEvent keyEvent);

        void onClickMute(boolean toggle);

        void onClickSpeaker(boolean toggle);

        void onClickHold(boolean toggle);

        void onClickKeypad(boolean toggle);

        void onStateChanged(int state);

        void answerCall();

        void endCall();

    }

    interface View {
        Context getContext();

        Activity getActivity();

        String getStateText();

        void toggleIconMute(boolean toggle);

        void toggleIconSpeaker(boolean toggle);

        void setBottomSheetState(int state);

        void switchToCallingUI();

        void updateTime(String time);

        void updateUIState(int state);

        void setStatusText(int statusTextRes);

        void finishActivity();
    }
}

package com.chooloo.www.callmanager.util;

import android.media.AudioManager;
import android.os.CountDownTimer;
import android.widget.Toast;

import androidx.annotation.ColorInt;
import androidx.annotation.ColorRes;
import androidx.annotation.StringRes;
import androidx.core.content.ContextCompat;

import com.chooloo.www.callmanager.R;
import com.chooloo.www.callmanager.ui.activity.OngoingCallActivity;

import java.util.Locale;

class ActionTimer {

    CountDownTimer mTimer = null;
    boolean mIsRejecting = true;
    boolean mIsActionTimerEnable = false;

    Integer oldVolume;

    private void setData(long millisInFuture, boolean isRejecting) {
        mIsRejecting = isRejecting;
        @ColorRes int textColorRes;
        @StringRes int textIndicator;
        if (isRejecting) {
            textColorRes = R.color.red_phone;
            textIndicator = R.string.reject_timer_indicator;
        } else {
            textColorRes = R.color.green_phone;
            textIndicator = R.string.answer_timer_indicator;
        }

        @ColorInt int textColor = ContextCompat.getColor(OngoingCallActivity.this, textColorRes);
        mActionTimeLeftText.setTextColor(textColor);
        mTimerIndicatorText.setText(textIndicator);

        mTimer = new CountDownTimer(millisInFuture, REFRESH_RATE) {
            Locale mLocale = Locale.getDefault();

            @Override
            public void onTick(long millisUntilFinished) {
                int secondsUntilFinished = (int) (millisUntilFinished / 1000);
                String timer = String.format(mLocale, "00:%02d", secondsUntilFinished);
                mActionTimeLeftText.setText(timer);
            }

            @Override
            public void onFinish() {
                end();
            }
        };
    }

    private void start() {
        mIsActionTimerEnable = true;
        oldVolume = mAudioManager.getStreamVolume(AudioManager.STREAM_RING);
        mAudioManager.setStreamVolume(AudioManager.STREAM_RING, 0, 0);
        if (mTimer != null) mTimer.start();
        else
            Toast.makeText(getApplicationContext(), "Couldn't start action timer (timer is null)", Toast.LENGTH_LONG).show();
        if (mActionTimerOverlay != null) setOverlay(mActionTimerOverlay);
    }

    private void cancel() {
        if (mTimer != null) mTimer.cancel();
        finalEndCommonMan();
    }

    private void end() {
        if (mIsRejecting) endCall();
        else activateCall();
        finalEndCommonMan();
    }

    private void finalEndCommonMan() {
        if (mIsActionTimerEnable)
            mAudioManager.setStreamVolume(AudioManager.STREAM_RING, oldVolume, 0);
        mIsActionTimerEnable = false;
        removeOverlay();
    }
}

package com.chooloo.www.callmanager.mvp;

import android.app.NotificationChannel;
import android.app.NotificationManager;
import android.app.PendingIntent;
import android.content.Context;
import android.content.Intent;
import android.media.AudioManager;
import android.os.Build;
import android.os.Handler;
import android.os.PowerManager;
import android.telecom.Call;
import android.view.KeyEvent;
import android.view.ViewGroup;
import android.widget.Toast;

import androidx.annotation.NonNull;
import androidx.annotation.StringRes;
import androidx.core.app.NotificationCompat;
import androidx.core.app.NotificationManagerCompat;

import com.chooloo.www.callmanager.R;
import com.chooloo.www.callmanager.database.entity.Contact;
import com.chooloo.www.callmanager.listener.NotificationActionReceiver;
import com.chooloo.www.callmanager.ui.activity.OngoingCallActivity;
import com.chooloo.www.callmanager.util.CallManager;
import com.chooloo.www.callmanager.util.CallTimeHandler;
import com.chooloo.www.callmanager.util.PermissionUtils;
import com.chooloo.www.callmanager.util.Stopwatch;
import com.chooloo.www.callmanager.util.ThemeUtils;
import com.google.android.material.bottomsheet.BottomSheetBehavior;

import org.jetbrains.annotations.NotNull;

import java.util.HashMap;

import static android.app.Notification.EXTRA_NOTIFICATION_ID;
import static android.content.Context.AUDIO_SERVICE;
import static android.content.Context.POWER_SERVICE;
import static android.telecom.Call.STATE_ACTIVE;
import static android.telecom.Call.STATE_CONNECTING;
import static android.telecom.Call.STATE_DIALING;
import static android.telecom.Call.STATE_DISCONNECTED;
import static android.telecom.Call.STATE_HOLDING;
import static android.telecom.Call.STATE_RINGING;
import static com.chooloo.www.callmanager.util.BiometricUtils.showBiometricPrompt;
import static com.chooloo.www.callmanager.util.CallTimeHandler.REFRESH_RATE;
import static com.chooloo.www.callmanager.util.CallTimeHandler.TIME_STOP;
import static com.chooloo.www.callmanager.util.CallTimeHandler.TIME_UPDATE;
import static com.google.android.material.bottomsheet.BottomSheetBehavior.STATE_COLLAPSED;
import static com.google.android.material.bottomsheet.BottomSheetBehavior.STATE_EXPANDED;

public class CallActivityPresenterImpl implements CallActivityContract.Presenter, CallTimeHandler.CallTimeHandlerCallbacks {


    private PowerManager powerManager;
    private PowerManager.WakeLock wakeLock;
    private Stopwatch mCallTimer = new Stopwatch();
    private CallTimeHandler mCallTimeHandler;
    private AudioManager mAudioManager;
    private CallActivityContract.View mView;
    private NotificationCompat.Builder mBuilder;

    public CallActivityPresenterImpl(CallActivityContract.View view) {
        this.mView = view;

        mCallTimeHandler = new CallTimeHandler(this);
        mAudioManager = (AudioManager).getSystemService(AUDIO_SERVICE);
        mAudioManager.setMode(AudioManager.MODE_IN_CALL);

        // Initiate PowerManager and WakeLock (turn screen on/off according to distance from face)
        int field = 0x00000020;
        try {
            field = PowerManager.class.getField("PROXIMITY_SCREEN_OFF_WAKE_LOCK").getInt(null);
        } catch (NoSuchFieldException | NullPointerException | IllegalAccessException e) {
            e.printStackTrace();
            Toast.makeText(mView.getContext(), "Can't use ear sensor for some reason :(", Toast.LENGTH_SHORT).show();
        }
        powerManager = (PowerManager) mView.getContext().getSystemService(POWER_SERVICE);
        wakeLock = powerManager.newWakeLock(field, mView.getActivity().getLocalClassName());
    }

    // Default overrides


    @Override
    public void onBackPressed() {
        mView.setBottomSheetState(STATE_COLLAPSED);
    }

    @Override
    public void onRequestPermissionResult(int requestCode, @NonNull int[] grantResults) {
        if (requestCode == PermissionUtils.PERMISSION_RC && PermissionUtils.checkPermissionsGranted(grantResults))
            setSmsOverlay(mFloatingSendSMSButton);
    }

    @Override
    public void onKeyPressed(KeyEvent keyEvent) {
        CallManager.keypad((char) keyEvent.getUnicodeChar());
    }

    // On Clicks

    @Override
    public void onClickMute(boolean toggle) {
        mAudioManager.setMicrophoneMute(toggle);
        mView.toggleIconMute(toggle);
    }

    @Override
    public void onClickSpeaker(boolean toggle) {
        mAudioManager.setSpeakerphoneOn(toggle);
    }

    @Override
    public void onClickHold(boolean toggle) {
        CallManager.hold(toggle);
    }

    @Override
    public void onClickKeypad(boolean toggle) {
        mView.setBottomSheetState(STATE_EXPANDED);
    }

    // State
    @Override
    public void onStateChanged(int state) {
        HashMap<Integer, Integer> statusToText = new HashMap<>();
        statusToText.put(STATE_ACTIVE, R.string.status_call_active);
        statusToText.put(STATE_DISCONNECTED, R.string.status_call_disconnected);
        statusToText.put(STATE_RINGING, R.string.status_call_incoming);
        statusToText.put(STATE_DIALING, R.string.status_call_dialing);
        statusToText.put(STATE_CONNECTING, R.string.status_call_dialing);
        statusToText.put(STATE_HOLDING, R.string.status_call_holding);

        int statusTextRes = statusToText.getOrDefault(state, R.string.status_call_active);
        mView.setStatusText(statusTextRes);

        if (state != STATE_RINGING && state != STATE_DISCONNECTED) mView.switchToCallingUI();
        if (state == STATE_DISCONNECTED) endCall();
        if (state == STATE_RINGING) showBiometricPrompt();
    }

    // Call Actions

    @Override
    public void answerCall() {
        CallManager.answer();
        mView.switchToCallingUI();
    }

    @Override
    public void endCall() {
        CallManager.reject();
        releaseWakeLock();
        toggleAutoCalling();
        mCallTimeHandler.sendEmptyMessage(TIME_STOP);
    }

    private void toggleAutoCalling() {
        if (CallManager.isAutoCalling()) {
            CallManager.nextCall(mView.getActivity());
        } else {
            new Handler().postDelayed(() -> mView.finishActivity(), 1000);
        }
    }

    // Call time callbacks

    @Override
    public void onTimeStart() {
        mCallTimer.start();
        mCallTimeHandler.sendEmptyMessage(TIME_UPDATE);
    }

    @Override
    public void onTimeStop() {
        mCallTimer.stop();
        mView.updateTime(mCallTimer.getStringTime());
        mCallTimeHandler.removeMessages(TIME_UPDATE);
    }

    @Override
    public void onTimeUpdate() {
        mView.updateTime(mCallTimer.getStringTime());
        mCallTimeHandler.sendEmptyMessageDelayed(TIME_UPDATE, REFRESH_RATE);
    }

    // Wake Lock

    private void acquireWakeLock() {
        if (!wakeLock.isHeld()) wakeLock.acquire(10 * 60 * 1000L /*10 minutes*/);
    }

    private void releaseWakeLock() {
        if (wakeLock.isHeld()) wakeLock.release();
    }

}

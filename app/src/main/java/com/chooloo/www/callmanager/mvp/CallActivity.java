package com.chooloo.www.callmanager.mvp;

import android.app.Activity;
import android.content.Context;
import android.os.Bundle;
import android.os.PersistableBundle;
import android.telecom.Call;
import android.view.View;
import android.view.ViewGroup;
import android.widget.Button;
import android.widget.FrameLayout;
import android.widget.ImageView;
import android.widget.TextView;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.annotation.StringRes;
import androidx.constraintlayout.widget.ConstraintLayout;
import androidx.core.content.ContextCompat;

import com.chooloo.www.callmanager.R;
import com.chooloo.www.callmanager.ui.activity.AbsThemeActivity;
import com.chooloo.www.callmanager.util.ThemeUtils;
import com.google.android.material.bottomsheet.BottomSheetBehavior;
import com.google.android.material.floatingactionbutton.FloatingActionButton;
import com.google.android.material.textfield.TextInputEditText;

import butterknife.BindView;
import butterknife.ButterKnife;
import butterknife.OnClick;

import static com.chooloo.www.callmanager.util.BiometricUtils.showBiometricPrompt;
import static com.google.android.material.bottomsheet.BottomSheetBehavior.STATE_COLLAPSED;
import static com.google.android.material.bottomsheet.BottomSheetBehavior.STATE_EXPANDED;
import static com.google.android.material.bottomsheet.BottomSheetBehavior.STATE_HIDDEN;

public class CallActivity extends AbsThemeActivity implements CallActivityContract.View {

    private static String mStateText;

    BottomSheetBehavior mBottomSheetBehavior;

    // Edit Texts
    @BindView(R.id.edit_sms) TextInputEditText mEditSms;

    // Text views
    @BindView(R.id.text_status) TextView mStatusText;
    @BindView(R.id.text_caller) TextView mCallerText;
    @BindView(R.id.text_reject_call_timer_desc) TextView mRejectCallTimerText;
    @BindView(R.id.text_answer_call_timer_desc) TextView mAnswerCallTimerText;
    @BindView(R.id.text_action_time_left) TextView mActionTimeLeftText;
    @BindView(R.id.text_timer_indicator) TextView mTimerIndicatorText;
    @BindView(R.id.text_stopwatch) TextView mTimeText;

    // Action buttons
    @BindView(R.id.answer_btn) FloatingActionButton mAnswerButton;
    @BindView(R.id.reject_btn) FloatingActionButton mRejectButton;

    // Image Views
    @BindView(R.id.caller_image_layout) FrameLayout mImageLayout;
    @BindView(R.id.image_placeholder) ImageView mPlaceholderImage;
    @BindView(R.id.image_photo) ImageView mPhotoImage;
    @BindView(R.id.button_hold) ImageView mButtonHold;
    @BindView(R.id.button_mute) ImageView mButtonMute;
    @BindView(R.id.button_keypad) ImageView mButtonKeypad;
    @BindView(R.id.button_speaker) ImageView mButtonSpeaker;
    @BindView(R.id.button_add_call) ImageView mButtonAddCall;
    @BindView(R.id.button_send_sms) Button mButtonSms;

    // Floating Action Buttons
    @BindView(R.id.button_floating_reject_call_timer) FloatingActionButton mFloatingRejectCallTimerButton;
    @BindView(R.id.button_floating_answer_call_timer) FloatingActionButton mFloatingAnswerCallTimerButton;
    @BindView(R.id.button_floating_send_sms) FloatingActionButton mFloatingSendSMSButton;
    @BindView(R.id.button_floating_cancel_overlay) FloatingActionButton mFloatingCancelOverlayButton;

    @Nullable
    @BindView(R.id.button_cancel_sms)
    FloatingActionButton mFloatingCancelSMS;
    @BindView(R.id.button_cancel_timer) FloatingActionButton mFloatingCancelTimerButton;

    // Layouts and overlays
    @BindView(R.id.frame) ViewGroup mRootView;
    @BindView(R.id.dialer_fragment) View mDialerFrame;
    @BindView(R.id.ongoing_call_layout) ConstraintLayout mOngoingCallLayout;
    @BindView(R.id.overlay_reject_call_options) ViewGroup mRejectCallOverlay;
    @BindView(R.id.overlay_answer_call_options) ViewGroup mAnswerCallOverlay;
    @BindView(R.id.overlay_action_timer) ViewGroup mActionTimerOverlay;
    @BindView(R.id.overlay_send_sms) ViewGroup mSendSmsOverlay;

    private CallActivityContract.Presenter mPresenter;

    @Override
    public void onCreate(@Nullable Bundle savedInstanceState, @Nullable PersistableBundle persistentState) {
        super.onCreate(savedInstanceState, persistentState);

        setThemeType(ThemeUtils.TYPE_TRANSPARENT_STATUS_BAR);
        setThemeType(ThemeUtils.TYPE_NO_ACTION_BAR);
        setContentView(R.layout.activity_ongoing_call);
        ButterKnife.bind(this);

        mPresenter = new CallActivityPresenterImpl(this);

        // click listeners
        mButtonMute.setOnClickListener(view -> mPresenter.onClickMute(view.isActivated()));
        mButtonSpeaker.setOnClickListener(view -> mPresenter.onClickSpeaker(view.isActivated()));

        // Bottom Sheet Behaviour
        mBottomSheetBehavior = BottomSheetBehavior.from(mDialerFrame);
        setBottomSheetState(STATE_HIDDEN);
    }

    @OnClick({R.id.button_speaker, R.id.button_hold, R.id.button_mute})
    public void onClickAction(View view) {
        ((ImageView) view).setColorFilter(ContextCompat.getColor(this, view.isActivated() ? R.color.white : R.color.soft_black));
        view.setActivated(!view.isActivated());
    }

    @Override
    public void onBackPressed() {
        super.onBackPressed();
        mPresenter.onBackPressed();
    }

    @Override
    public void onRequestPermissionsResult(int requestCode, @NonNull String[] permissions, @NonNull int[] grantResults) {
        super.onRequestPermissionsResult(requestCode, permissions, grantResults);
        mPresenter.onRequestPermissionResult(requestCode, grantResults);
    }

    @Override
    public void finishActivity() {
        finish();
    }

    @Override
    public Context getContext() {
        return getApplicationContext();
    }

    @Override
    public Activity getActivity() {
        return getActivity();
    }

    @Override
    public String getStateText() {
        return mStateText;
    }

    @Override
    public void toggleIconMute(boolean toggle) {
        mButtonMute.setImageResource(toggle ? R.drawable.ic_mic_off_black_24dp : R.drawable.ic_mic_black_24dp);
    }

    @Override
    public void toggleIconSpeaker(boolean toggle) {

    }

    @Override
    public void updateTime(String time) {
        mTimeText.setText(time);
    }

    @Override
    public void setBottomSheetState(int state) {
        mBottomSheetBehavior.setState(state);
    }

    @Override
    public void switchToCallingUI() {

    }

    @Override
    public void updateUIState(int state) {
        @StringRes int statusTextRes;
        switch (state) {
            case Call.STATE_ACTIVE: // Ongoing
                statusTextRes = R.string.status_call_active;
                break;
            case Call.STATE_DISCONNECTED: // Ended
                statusTextRes = R.string.status_call_disconnected;
                break;
            case Call.STATE_RINGING: // Incoming
                statusTextRes = R.string.status_call_incoming;
                showBiometricPrompt(this);
                break;
            case Call.STATE_DIALING: // Outgoing
                statusTextRes = R.string.status_call_dialing;
                break;
            case Call.STATE_CONNECTING: // Connecting (probably outgoing)
                statusTextRes = R.string.status_call_dialing;
                break;
            case Call.STATE_HOLDING: // On Hold
                statusTextRes = R.string.status_call_holding;
                break;
            default:
                statusTextRes = R.string.status_call_active;
                break;
        }
        mStatusText.setText(statusTextRes);
        mStateText = getResources().getString(statusTextRes);
    }

    @Override
    public void setStatusText(int statusTextRes) {
        mStatusText.setText(statusTextRes);
    }
}

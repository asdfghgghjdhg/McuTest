package com.google.asdfghgghjdhg.mcutest;

import androidx.appcompat.app.AppCompatActivity;

import android.app.Activity;
import android.app.AlarmManager;
import android.app.Instrumentation;
import android.app.PendingIntent;
import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.Intent;
import android.content.IntentFilter;
import android.graphics.Color;
import android.os.Bundle;
import android.os.Handler;
import android.os.IBinder;
import android.os.Message;
import android.os.PowerManager;
import android.os.SystemClock;
import android.text.method.ScrollingMovementMethod;
import android.util.Log;
import android.view.KeyEvent;
import android.view.View;
import android.view.ViewGroup;
import android.view.Window;
import android.view.WindowManager;
import android.widget.Button;
import android.widget.TextView;

import com.google.asdfghgghjdhg.auto.enums.McuEnums;
import com.google.asdfghgghjdhg.auto.mcu.HostCallback;
import com.google.asdfghgghjdhg.auto.mcu.MCU;
import com.google.asdfghgghjdhg.auto.mcu.McuPackets;
import com.google.asdfghgghjdhg.auto.mcu.McuService;
import com.google.asdfghgghjdhg.auto.constants.McuConstants;
import com.google.asdfghgghjdhg.auto.util.Debug;

import java.lang.reflect.Method;
import java.text.DateFormat;
import java.text.SimpleDateFormat;
import java.util.Calendar;
import java.util.Date;
import java.util.Locale;

import static android.content.Intent.ACTION_SCREEN_OFF;

public class MainActivity extends AppCompatActivity implements HostCallback {

    private McuService mcuService = new McuService();
    private MCU mcu = null;

    private View barStatus = null;
    private View barSettings = null;
    private View barTuner = null;
    private View barAudio = null;
    private View barTunerPresets = null;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_main);

        Intent intent = new Intent("INVALIDATE_SERIAL_ACTION");
        getApplicationContext().sendBroadcast(intent);

        barStatus = findViewById(R.id.barStatus);
        barSettings = findViewById(R.id.barSettings);
        barTuner = findViewById(R.id.barTuner);
        barAudio = findViewById(R.id.barAudio);
        barTunerPresets = findViewById(R.id.barTunerPresets);

        setDecIncListener((McuParamSettings) findViewById(R.id.paramTunerFreq));
        setDecIncListener((McuParamSettings) findViewById(R.id.paramTunerRegion));

        setDecIncListener((McuParamSettings) findViewById(R.id.paramAudioPreset));
        setDecIncListener((McuParamSettings) findViewById(R.id.paramAudioBalance));
        setDecIncListener((McuParamSettings) findViewById(R.id.paramAudioFade));
        setDecIncListener((McuParamSettings) findViewById(R.id.paramAudioBassGain));
        setDecIncListener((McuParamSettings) findViewById(R.id.paramAudioMiddleGain));
        setDecIncListener((McuParamSettings) findViewById(R.id.paramAudioTrebleGain));

        TextView console = findViewById(R.id.console);
        console.setHorizontallyScrolling(true);
        console.setMovementMethod(new ScrollingMovementMethod());

        mcu = MCU.getInstance();
        mcu.setDebugLevel(Debug.DebugLevel.DEBUG);
        mcu.addListener(this);

        mcuService.startService(getApplicationContext());

        updateStatusBar();
        updateTunerBar();
        updateAudioBar();
        showStatusBar();
    }

    @Override
    protected void onDestroy() {
        mcuService.stopService();
        mcu.removeListener(this);

        Intent intent = new Intent("VALIDATE_SERIAL_ACTION");
        getApplicationContext().sendBroadcast(intent);

        super.onDestroy();
    }

    private void setDecIncListener(McuParamSettings view) {
        view.setDecOnClickListener(new View.OnClickListener() {
            public void onClick(View view) {
                onParamDecClick(view);
            }
        });
        view.setIncOnClickListener(new View.OnClickListener() {
            public void onClick(View view) {
                onParamIncClick(view);
            }
        });
    }

    private void addLineToConsole(String text) {
        TextView console = findViewById(R.id.console);
        DateFormat tf = new SimpleDateFormat("HH:mm:ss.SSSZ", Locale.getDefault());

        console.append(String.format(Locale.getDefault(),"[%s] %s\n", tf.format(Calendar.getInstance().getTime()), text));

        while (console.canScrollVertically(1)) {
            console.scrollBy(0, 10);
        }
    }

    private void updateStatusBar() {
        McuParamView paramView = null;

        paramView = findViewById(R.id.paramConnected);
        paramView.setParamValue(String.format(Locale.getDefault(), "%b", mcu.isConnected()));

        paramView = findViewById(R.id.paramPoweredOn);
        paramView.setParamValue(String.format(Locale.getDefault(), "%b", mcu.isPoweredOn()));

        paramView = findViewById(R.id.paramVolume);
        paramView.setParamValue(String.format(Locale.getDefault(), "%d", mcu.getAudio().getVolume()));

        paramView = findViewById(R.id.paramMuted);
        paramView.setParamValue(String.format(Locale.getDefault(), "%b", mcu.getAudio().isMuted()));

        paramView = findViewById(R.id.paramStatusFlags);
        String value = Integer.toBinaryString(mcu.getStatusFlags());
        while (value.length() < 16) { value = "0" + value; }
        paramView.setParamValue(value);

        paramView = findViewById(R.id.paramVersion);
        paramView.setParamValue(mcu.getMcuVersion());

        paramView = findViewById(R.id.paramFrontSource);
        paramView.setParamValue(mcu.getFrontSource().name());

        paramView = findViewById(R.id.paramRearSource);
        paramView.setParamValue(mcu.getRearSource().name());
    }
    private void updateTunerBar() {
        McuParamView paramView = null;

        paramView = findViewById(R.id.paramTunerFreq);
        paramView.setParamValue(String.valueOf(mcu.getTuner().getFreq()));

        paramView = findViewById(R.id.paramTunerBand);
        paramView.setParamValue(mcu.getTuner().getBand().name());

        paramView = findViewById(R.id.paramTunerFlags);
        String value = Integer.toBinaryString(mcu.getTuner().getStatusFlags());
        while (value.length() < 8) { value = "0" + value; }
        paramView.setParamValue(value);

        paramView = findViewById(R.id.paramTunerRange);
        MCU.Tuner.TunerRange range = mcu.getTuner().getFmRange();
        if (mcu.getTuner().getBand() == McuEnums.TunerBand.AM1 || mcu.getTuner().getBand() == McuEnums.TunerBand.AM2) {
            range = mcu.getTuner().getAmRange();
        }
        paramView.setParamValue(String.format(Locale.getDefault(), "%.2f..%.2f", range.getMinFreq(), range.getMaxFreq()));

        paramView = findViewById(R.id.paramTunerRegion);
        paramView.setParamValue(mcu.getTuner().getRegion().name());

        Button btnAS = findViewById(R.id.btnTunerAS);
        btnAS.setPressed(mcu.getTuner().isAutoScan());

        Button btnPS = findViewById(R.id.btnTunerPS);
        btnPS.setPressed(mcu.getTuner().isPresetScan());

        Button btnScan = findViewById(R.id.btnTunerScan);
        btnScan.setPressed(mcu.getTuner().isScanning());

        Button btnSeekDown = findViewById(R.id.btnTunerSeekDown);
        Button btnSeekUp = findViewById(R.id.btnTunerSeekUp);
        btnSeekDown.setPressed(mcu.getTuner().isSeeking());
        btnSeekUp.setPressed(mcu.getTuner().isSeeking());

        Button btnLocal = findViewById(R.id.btnTunerLocal);
        btnLocal.setPressed(mcu.getTuner().isLocalMode());

        for (int i = 0; i < McuConstants.RadioConst.RADIO_PRESETS_COUNT; i++) {
            String btnId = String.format(Locale.getDefault(), "btnTunerPreset%d", i);
            Button presetBtn = barTunerPresets.findViewById(getApplicationContext().getResources().getIdentifier(btnId, "id", getApplicationContext().getPackageName()));
            presetBtn.setText(String.format(Locale.getDefault(), "%.2f", mcu.getTuner().getPresets().getPreset(i)));
            boolean statePressed = i == mcu.getTuner().getPresetIndex();
            presetBtn.setPressed(statePressed);
        }
    }
    private void updateAudioBar() {
        McuParamView paramView = null;

        paramView = findViewById(R.id.paramAudioPreset);
        paramView.setParamValue(mcu.getAudio().getEqPreset().name());

        paramView = findViewById(R.id.paramAudioBalance);
        paramView.setParamValue(String.valueOf(mcu.getAudio().getBalance()));

        paramView = findViewById(R.id.paramAudioFade);
        paramView.setParamValue(String.valueOf(mcu.getAudio().getFade()));

        paramView = findViewById(R.id.paramAudioBassGain);
        paramView.setParamValue(String.valueOf(mcu.getAudio().getBass().getGain()));

        paramView = findViewById(R.id.paramAudioMiddleGain);
        paramView.setParamValue(String.valueOf(mcu.getAudio().getMiddle().getGain()));

        paramView = findViewById(R.id.paramAudioTrebleGain);
        paramView.setParamValue(String.valueOf(mcu.getAudio().getTreble().getGain()));
    }

    private void showStatusBar() {
        barSettings.setVisibility(View.INVISIBLE);
        barTuner.setVisibility(View.INVISIBLE);
        barAudio.setVisibility(View.INVISIBLE);
        barTunerPresets.setVisibility(View.INVISIBLE);
        barStatus.setVisibility(View.VISIBLE);
    }
    private void showSettingsBar() {
        barStatus.setVisibility(View.INVISIBLE);
        barTuner.setVisibility(View.INVISIBLE);
        barAudio.setVisibility(View.INVISIBLE);
        barTunerPresets.setVisibility(View.INVISIBLE);
        barSettings.setVisibility(View.VISIBLE);
    }
    private void showTunerBar() {
        barStatus.setVisibility(View.INVISIBLE);
        barSettings.setVisibility(View.INVISIBLE);
        barAudio.setVisibility(View.INVISIBLE);
        barTunerPresets.setVisibility(View.INVISIBLE);
        barTuner.setVisibility(View.VISIBLE);
    }
    private void showAudioBar() {
        barStatus.setVisibility(View.INVISIBLE);
        barSettings.setVisibility(View.INVISIBLE);
        barTuner.setVisibility(View.INVISIBLE);
        barTunerPresets.setVisibility(View.INVISIBLE);
        barAudio.setVisibility(View.VISIBLE);
    }
    private void showTunerPresetsBar() {
        barStatus.setVisibility(View.INVISIBLE);
        barSettings.setVisibility(View.INVISIBLE);
        barTuner.setVisibility(View.INVISIBLE);
        barAudio.setVisibility(View.INVISIBLE);
        barTunerPresets.setVisibility(View.VISIBLE);
    }

    public void onError(McuEnums.McuError error) {
        if (error == null) { return; }
        addLineToConsole("Error: " + error.toString());
    }
    public void onConnect() {
        addLineToConsole("Connected to MCU");
        updateStatusBar();
        updateTunerBar();
        updateAudioBar();
    }
    public void onInitialize() {
        addLineToConsole("MCU initialized");
    }
    public void onDisconnect() {
        addLineToConsole("Disconnected from MCU");
        updateStatusBar();
    }
    public void onPacketReceived(McuPackets.McuRxPacket packet) {
        if (packet == null) { return; }

        if (packet.getCommand() == McuEnums.McuCommand.ACKNOWLEDGE) {
            //addLineToConsole("MCU: Acknowledge");
        } else {
            addLineToConsole(String.format(Locale.getDefault(), "MCU: %s (%s)", packet.toString(), packet.getCommand().name()));
        }
    }
    public void onPacketSent(McuPackets.McuTxPacket packet) {
        if (packet == null) { return; }

        if (packet.getCommand() == McuEnums.McuCommand.ACKNOWLEDGE) {
            //addLineToConsole("Host: Acknowledge");
        } else {
            addLineToConsole(String.format(Locale.getDefault(), "MCU: %s (%s)", packet.toString(), packet.getCommand().name()));
        }
    }
    public void onPowerOn() {
        addLineToConsole("Powered on");
    }
    public void onPowerOff() {
        addLineToConsole("Powered off");
    }
    public void onPowerStatusChanged() {
        updateStatusBar();
    }
    public void onFrontSourceChanged() {
        updateStatusBar();
    }
    public void onRearSourceChanged() {
        updateStatusBar();
    }
    public void onVolumeChanged() {
        updateStatusBar();
    }
    public void onAudioSettingsChanged() {
        updateAudioBar();
    }
    public void onStatusChanged() {
        updateStatusBar();
    }
    public void onDiscPresentChanged() {
        updateStatusBar();
    }
    public void onDiscInsertedChanged() {
        updateStatusBar();
    }
    public void onAgingModeChanged() {
        updateStatusBar();
    }
    public void onCarUSBModeChanged() {
        updateStatusBar();
    }
    public void onAVInStatusChanged() {
        updateStatusBar();
    }
    public void onReverseStatusChanged() {
        updateStatusBar();
    }
    public void onHandbrakeStatusChanged() {
        updateStatusBar();
    }
    public void onParkingLightsStatusChanged() {
        updateStatusBar();
    }
    public void onDTVStatusChanged() {
        updateStatusBar();
    }
    public void onTestModeChanged() {
        updateStatusBar();
    }
    public void onACCStatusChanged() {
        updateStatusBar();
    }
    public void onBluetoothStatusChanged() {
        updateStatusBar();
    }
    public void onTunerChanged() {
        updateTunerBar();
    }
    public void onSettingsChanged()  {
        updateStatusBar();
    }
    public void onTunerPresetsChanged() {
        updateTunerBar();
    }

    public void btnVolDownOnClick(View v) {
        if (mcu.getAudio().isMuted()) {
            mcu.getAudio().setMuted(false);
        } else {
            mcu.getAudio().setVolume(mcu.getAudio().getVolume() - 1);
        }
        updateStatusBar();
    }
    public void btnVolUpOnClick(View v) {
        if (mcu.getAudio().isMuted()) {
            mcu.getAudio().setMuted(false);
        } else {
            mcu.getAudio().setVolume(mcu.getAudio().getVolume() + 1);
        }
        updateStatusBar();
    }
    public void btnMuteOnClick(View v) {
        mcu.getAudio().setMuted(!mcu.getAudio().isMuted());
        updateStatusBar();
    }
    public void btnStatusOnClick(View v) {
        showStatusBar();
    }
    public void btnSettingsOnClick(View v) {
        showSettingsBar();
    }
    public void btnTunerOnClick(View v) {
        showTunerBar();
    }
    public void btnAudioOnClick(View v) {
        showAudioBar();
    }
    public void btnTunerPresetsOnClick(View v) {
        showTunerPresetsBar();
    }
    public void btnExitOnClick(View v) {
        finishAndRemoveTask();
    }
    public void btnTest1OnClick(View v) {
        mcu.test();
    }
    public void btnTest2OnClick(View v) {

    }
    public void btnSDAlwaysOnClick(View v) {
        mcu.getSettings().setShutdownTime(McuEnums.ShutdownTime.ALWAYS);
    }
    public void btnSD2HoursOnClick(View v) {
        mcu.getSettings().setShutdownTime(McuEnums.ShutdownTime.AFTER_2_HOURS);
    }
    public void btnSD12HoursOnClick(View v) {
        mcu.getSettings().setShutdownTime(McuEnums.ShutdownTime.AFTER_12_HOURS);
    }
    public void btnSD24HoursOnClick(View v) {
        mcu.getSettings().setShutdownTime(McuEnums.ShutdownTime.AFTER_24_HOURS);
    }
    public void btnBandOnClick(View v) {
        mcu.getTuner().sendCommand(McuEnums.TunerCommand.COMMAND_SWITCH_BAND);
    }
    public void btnASOnClick(View v) {
        mcu.getTuner().sendCommand(McuEnums.TunerCommand.COMMAND_AUTOSCAN);
    }
    public void btnPSOnClick(View v) {
        mcu.getTuner().sendCommand(McuEnums.TunerCommand.COMMAND_PRESET_SCAN);
    }
    public void btnScanOnClick(View v) {
        mcu.getTuner().sendCommand(McuEnums.TunerCommand.COMMAND_SCAN);
    }
    public void btnSeekDownOnClick(View v) {
        mcu.getTuner().sendCommand(McuEnums.TunerCommand.COMMAND_SEEK_DOWN);
    }
    public void btnSeekUpOnClick(View v) {
        mcu.getTuner().sendCommand(McuEnums.TunerCommand.COMMAND_SEEK_UP);
    }
    public void btnTunerOnOnClick(View v) {
        mcu.setFrontSource(McuEnums.McuSource.SOURCE_TUNER);
        updateStatusBar();
    }
    public void btnTunerOffOnClick(View v) {
        mcu.setFrontSource(McuEnums.McuSource.SOURCE_ARM);
        updateStatusBar();
    }
    public void btnTunerLocalOnClick(View v) {
        mcu.getTuner().sendCommand(McuEnums.TunerCommand.COMMAND_SWITCH_DX_LOCAL);
    }
    public void btnTunerPresetOnClick(View v) {
        String id = v.getResources().getResourceName(v.getId());
        id = id.replace("com.google.asdfghgghjdhg.mcutest:id/btnTunerPreset", "");
        int index = Integer.parseInt(id);
        mcu.getTuner().setPresetIndex(index);

        updateTunerBar();
    }

    public void onParamDecClick(View view) {
        switch (view.getId()) {
            case R.id.paramTunerFreq:
                mcu.getTuner().sendCommand(McuEnums.TunerCommand.COMMAND_MANUAL_DOWN);
                break;
            case R.id.paramTunerRegion:
                if (mcu.getTuner().getRegion().ordinal() == 0) { break; }
                mcu.getTuner().setRegion(McuEnums.TunerRegion.values()[mcu.getTuner().getRegion().ordinal() - 1]);
                updateTunerBar();
                break;
            case R.id.paramAudioPreset:
                if (mcu.getAudio().getEqPreset().ordinal() == 0) { break; }
                mcu.getAudio().setEqPreset(McuEnums.EqPreset.values()[mcu.getAudio().getEqPreset().ordinal() - 1]);
                updateAudioBar();
                break;
            case R.id.paramAudioBalance:
                mcu.getAudio().setBalance(mcu.getAudio().getBalance() - 1);
                updateAudioBar();
                break;
            case R.id.paramAudioFade:
                mcu.getAudio().setFade(mcu.getAudio().getFade() - 1);
                updateAudioBar();
                break;
            case R.id.paramAudioBassGain:
                mcu.getAudio().getBass().setGain(mcu.getAudio().getBass().getGain() - 1);
                updateAudioBar();
                break;
            case R.id.paramAudioMiddleGain:
                mcu.getAudio().getMiddle().setGain(mcu.getAudio().getMiddle().getGain() - 1);
                updateAudioBar();
                break;
            case R.id.paramAudioTrebleGain:
                mcu.getAudio().getTreble().setGain(mcu.getAudio().getTreble().getGain() - 1);
                updateAudioBar();
                break;
        }
    }
    public void onParamIncClick(View view) {
        switch (view.getId()) {
            case R.id.paramTunerFreq:
                mcu.getTuner().sendCommand(McuEnums.TunerCommand.COMMAND_MANUAL_UP);
                break;
            case R.id.paramTunerRegion:
                if (mcu.getTuner().getRegion().ordinal() == McuEnums.TunerRegion.values().length - 1) { break; }
                mcu.getTuner().setRegion(McuEnums.TunerRegion.values()[mcu.getTuner().getRegion().ordinal() + 1]);
                updateTunerBar();
                break;
            case R.id.paramAudioPreset:
                if (mcu.getAudio().getEqPreset().ordinal() == McuEnums.EqPreset.values().length - 1) { break; }
                mcu.getAudio().setEqPreset(McuEnums.EqPreset.values()[mcu.getAudio().getEqPreset().ordinal() + 1]);
                updateAudioBar();
                break;
            case R.id.paramAudioBalance:
                mcu.getAudio().setBalance(mcu.getAudio().getBalance() + 1);
                updateAudioBar();
                break;
            case R.id.paramAudioFade:
                mcu.getAudio().setFade(mcu.getAudio().getFade() + 1);
                updateAudioBar();
                break;
            case R.id.paramAudioBassGain:
                mcu.getAudio().getBass().setGain(mcu.getAudio().getBass().getGain() + 1);
                updateAudioBar();
                break;
            case R.id.paramAudioMiddleGain:
                mcu.getAudio().getMiddle().setGain(mcu.getAudio().getMiddle().getGain() + 1);
                updateAudioBar();
                break;
            case R.id.paramAudioTrebleGain:
                mcu.getAudio().getTreble().setGain(mcu.getAudio().getTreble().getGain() + 1);
                updateAudioBar();
                break;
        }
    }
}

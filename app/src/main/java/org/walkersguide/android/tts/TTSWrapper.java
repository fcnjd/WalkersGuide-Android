package org.walkersguide.android.tts;

import android.annotation.TargetApi;

import android.content.Context;

import android.os.Build;

import android.speech.tts.TextToSpeech;
import android.speech.tts.UtteranceProgressListener;

import android.view.accessibility.AccessibilityManager;

import com.google.common.base.Splitter;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Locale;
import android.media.AudioAttributes;
import android.accessibilityservice.AccessibilityServiceInfo;
import timber.log.Timber;
import org.walkersguide.android.util.GlobalInstance;
import org.walkersguide.android.util.SettingsManager;


public class TTSWrapper extends UtteranceProgressListener {
    private static final String UTTERANCE_ID_SPEAK = "utteranceidspeak";

    private static TTSWrapper managerInstance;
    private AccessibilityManager accessibilityManager;
    private TextToSpeech tts;

    public static TTSWrapper getInstance() {
        if (managerInstance == null){
            managerInstance = getInstanceSynchronized();
        }
        return managerInstance;
    }

    private static synchronized TTSWrapper getInstanceSynchronized() {
        if (managerInstance == null){
            managerInstance = new TTSWrapper();
        }
        return managerInstance;
    }

    private TTSWrapper() {
        accessibilityManager = (AccessibilityManager) GlobalInstance.getContext().getSystemService(Context.ACCESSIBILITY_SERVICE);
        tts = new TextToSpeech(GlobalInstance.getContext(), new TextToSpeech.OnInitListener() {
            @Override public void onInit(int status) {
                if (status != TextToSpeech.ERROR) {
                    tts.setLanguage(Locale.getDefault());
                    tts.setOnUtteranceProgressListener(TTSWrapper.this);
                } else {
                    tts = null;
                }
            }
        });
    }

    public boolean isInitialized() {
        if (tts != null) {
            return true;
        }
        return false;
    }

    public boolean isSpeaking() {
    if (isInitialized()) {
            return tts.isSpeaking();
        }
        return false;
    }

    public boolean isScreenReaderEnabled() {
        return ! accessibilityManager.getEnabledAccessibilityServiceList(AccessibilityServiceInfo.FEEDBACK_SPOKEN).isEmpty();
    }

    public void screenReader(String message) {
        if (isScreenReaderEnabled()) {
            speak(message);
        }
    }

    public void announce(String message) {
        if (SettingsManager.getInstance().getTtsSettings().getAnnouncementsEnabled()) {
            speak(message);
        }
    }

    private void speak(String message) {
        if (isInitialized()) {
            if (isSpeaking()) {
                tts.stop();
            }
            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.LOLLIPOP) {
                ttsAtLeastApi21(message);
            } else {
                ttsUnderApi20(message);
            }
        }
    }

    @SuppressWarnings("deprecation")
    private void ttsUnderApi20(String message) {
        HashMap<String,String> map = new HashMap<String,String>();
        map.put(TextToSpeech.Engine.KEY_PARAM_UTTERANCE_ID, UTTERANCE_ID_SPEAK);
        for (String chunk : Splitter.fixedLength(tts.getMaxSpeechInputLength()).splitToList(message)) {
            tts.speak(chunk, TextToSpeech.QUEUE_ADD, map);
        }
    }

    @TargetApi(Build.VERSION_CODES.LOLLIPOP)
    private void ttsAtLeastApi21(String message) {
        Timber.d("isScreenReaderEnabled: %1$s", isScreenReaderEnabled());
        // set audio attributes
        AudioAttributes audioAttributes = new AudioAttributes.Builder()
            .setContentType(AudioAttributes.CONTENT_TYPE_SPEECH)
            .setUsage(
                    isScreenReaderEnabled()
                    ? AudioAttributes.USAGE_ASSISTANCE_ACCESSIBILITY
                    : AudioAttributes.USAGE_ASSISTANCE_NAVIGATION_GUIDANCE)
            .build();
        tts.setAudioAttributes(audioAttributes);

        // speak
        for (String chunk : Splitter.fixedLength(tts.getMaxSpeechInputLength()).splitToList(message)) {
            tts.speak(chunk, TextToSpeech.QUEUE_ADD, null, UTTERANCE_ID_SPEAK);
        }
    }


    /**
     * UtteranceProgressListener interface inplementation
     */

    @Override public void onStart(String utteranceId) {
    }

    @Override public void onError(String utteranceId) {
        tts.setLanguage(Locale.getDefault());
    }

    @Override public void onDone(String utteranceId) {
    }

}

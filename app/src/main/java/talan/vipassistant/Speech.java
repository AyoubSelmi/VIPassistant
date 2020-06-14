package talan.vipassistant;

import android.Manifest;
import android.content.Context;
import android.content.Intent;
import android.content.pm.PackageManager;
import android.os.Bundle;
import android.speech.RecognitionListener;
import android.speech.RecognizerIntent;
import android.speech.SpeechRecognizer;
import android.speech.tts.TextToSpeech;
import android.speech.tts.UtteranceProgressListener;
import android.util.Log;
import android.widget.Toast;

import com.karumi.dexter.Dexter;
import com.karumi.dexter.PermissionToken;
import com.karumi.dexter.listener.PermissionDeniedResponse;
import com.karumi.dexter.listener.PermissionGrantedResponse;
import com.karumi.dexter.listener.PermissionRequest;
import com.karumi.dexter.listener.single.PermissionListener;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.Locale;

public class Speech  implements TextToSpeech.OnInitListener {
    private boolean initialized;
    private String queuedText;
    private TextToSpeech tts; //text to speech

    public Speech() {
    }

    public TextToSpeech getTts() {
        return tts;
    }

    public void setTts(TextToSpeech tts) {
        this.tts = tts;
    }
    public void speak(String text,String utteranceId) {
        if (!initialized) {
            queuedText = text;
            return;
        }
        queuedText = null;
        HashMap <String, String> params= new HashMap();
        params.put(TextToSpeech.Engine.KEY_PARAM_UTTERANCE_ID,utteranceId);
        tts.speak(text, TextToSpeech.QUEUE_ADD, params);
    }
    @Override
    public void onInit(int status) {
        if (status == TextToSpeech.SUCCESS) {
            initialized = true;
            tts.setLanguage(Locale.ENGLISH);
            if (queuedText != null) {
                speak(queuedText,"init");
            }
        }

    }
}

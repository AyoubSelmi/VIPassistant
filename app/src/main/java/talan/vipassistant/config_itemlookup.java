package talan.vipassistant;

import androidx.appcompat.app.AppCompatActivity;

import android.Manifest;
import android.accounts.Account;
import android.accounts.AccountManager;
import android.content.Context;
import android.content.Intent;
import android.os.Bundle;
import android.speech.RecognitionListener;
import android.speech.RecognizerIntent;
import android.speech.SpeechRecognizer;
import android.speech.tts.TextToSpeech;
import android.speech.tts.UtteranceProgressListener;
import android.util.Log;
import android.util.Patterns;
import android.view.View;
import android.widget.RelativeLayout;
import android.widget.Toast;

import com.karumi.dexter.Dexter;
import com.karumi.dexter.PermissionToken;
import com.karumi.dexter.listener.PermissionDeniedResponse;
import com.karumi.dexter.listener.PermissionGrantedResponse;
import com.karumi.dexter.listener.PermissionRequest;
import com.karumi.dexter.listener.single.PermissionListener;

import java.util.ArrayList;
import java.util.regex.Pattern;

public class config_itemlookup extends AppCompatActivity {
    RelativeLayout btitemconfig,btroomsconfig;
    private Speech speech = new Speech();
    private SpeechRecognitionListener speechRecognitionListener = new SpeechRecognitionListener();
    private boolean clicked_btitemconfig;
    private boolean clicked_btroomsconfig;
    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_config_itemlookup);
        btitemconfig = (RelativeLayout) findViewById(R.id.btitemconfig);
        btroomsconfig = (RelativeLayout) findViewById(R.id.btroomsconfig);
        btitemconfig.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                Intent i=new Intent(new Intent(getApplicationContext(), config_items.class));
                i.putExtra("owner",getIntent().getExtras().getString("owner"));
                startActivity(i);
            }
        });
        btroomsconfig.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                Intent j=new Intent(getApplicationContext(),config_rooms.class);
                j.putExtra("owner",getIntent().getExtras().getString("owner"));
                startActivity(j);
            }
        });
    }

    @Override
    protected void onRestart() {
        speechRecognitionListener.CreateSpeechRecognizer(getApplicationContext(), getPackageName());
        speech = new Speech();
        speechRecognitionListener = new SpeechRecognitionListener();
        super.onRestart();
    }

    @Override
    protected void onResume() {
        super.onResume();
        clicked_btitemconfig=false;
        clicked_btroomsconfig=false;
        speechRecognitionListener.CreateSpeechRecognizer(getApplicationContext(), getPackageName());
        speech.setTts(new TextToSpeech(this, speech));
        speech.speak("This is item lookup config menu. Click the buttons. Or say one for "
                + "configuring your items, two for configuring your rooms, or say back", "VOICE_COMMAND_AFTER");
        getVoiceCommand();
    }
    @Override
    public void onStop()  {
        if (speech.getTts() != null) {
            speech.getTts().stop();
            speech.getTts().shutdown();
        }
        if (speechRecognitionListener.getmSpeechRecognizer() != null)
        {
            speechRecognitionListener.getmSpeechRecognizer().destroy();
        }
        super.onStop() ;
    }
    /**
     * Speech related methods
     * getVoiceCommand launch recognizing voice command after the instruction spoken are said.
     * onActivity does process the voice command
     * onActivity does process the voice command
     * onDestroy liberates TTS (text to speech) resources
     */
    public void getVoiceCommand() {

        UtteranceProgressListener mProgressListener = new UtteranceProgressListener() {
            @Override
            public void onStart(String utteranceId) {
            }

            @Override
            public void onError(String utteranceId) {
            }

            @Override
            public void onDone(String utteranceId) {
                runOnUiThread(new Runnable() {
                    @Override
                    public void run() {
                        speechRecognitionListener.getmSpeechRecognizer().startListening(speechRecognitionListener.getmSpeechRecognizerIntent());
                    }
                });
            }
        };
        speech.getTts().setOnUtteranceProgressListener(mProgressListener);

    }

    @Override
    public void onDestroy() {
        if (speech.getTts() != null) {
            speech.getTts().stop();
            speech.getTts().shutdown();
        }
        if (speechRecognitionListener.getmSpeechRecognizer() != null)
        {
            speechRecognitionListener.getmSpeechRecognizer().destroy();
        }
        super.onDestroy();
    }
    protected class SpeechRecognitionListener implements RecognitionListener {
        private SpeechRecognizer mSpeechRecognizer;
        private Intent mSpeechRecognizerIntent;
        public void CreateSpeechRecognizer(Context context, String packageManager){
            mSpeechRecognizer = SpeechRecognizer.createSpeechRecognizer(context);
            mSpeechRecognizerIntent = new Intent(RecognizerIntent.ACTION_RECOGNIZE_SPEECH);
            mSpeechRecognizerIntent.putExtra(RecognizerIntent.EXTRA_LANGUAGE_MODEL,
                    RecognizerIntent.LANGUAGE_MODEL_FREE_FORM);
            mSpeechRecognizerIntent.putExtra(RecognizerIntent.EXTRA_CALLING_PACKAGE,packageManager);
            mSpeechRecognizer.setRecognitionListener(this);
        }
        public SpeechRecognizer getmSpeechRecognizer() {
            return mSpeechRecognizer;
        }
        public void setmSpeechRecognizer(SpeechRecognizer mSpeechRecognizer) {
            this.mSpeechRecognizer = mSpeechRecognizer;
        }
        public Intent getmSpeechRecognizerIntent() {
            return mSpeechRecognizerIntent;
        }
        public void setmSpeechRecognizerIntent(Intent mSpeechRecognizerIntent) {
            this.mSpeechRecognizerIntent = mSpeechRecognizerIntent;
        }
        @Override
        public void onBeginningOfSpeech() {}//Log.d(TAG, "onBeginingOfSpeech");}
        @Override
        public void onBufferReceived(byte[] buffer) { }
        @Override
        public void onEndOfSpeech() {}//Log.d(TAG, "onEndOfSpeech");}
        @Override
        public void onError(int error)
        { mSpeechRecognizer.startListening(mSpeechRecognizerIntent);}//Log.d(TAG, "error = " + error);}
        @Override
        public void onEvent(int eventType, Bundle params) { }
        @Override
        public void onPartialResults(Bundle partialResults) { }
        @Override
        public void onReadyForSpeech(Bundle params)
        { Log.d("STT", "onReadyForSpeech");} //$NON-NLS-1$
        @Override
        public void onResults(Bundle results)
        { ArrayList<String> matches = results.getStringArrayList(SpeechRecognizer.RESULTS_RECOGNITION);
            // matches are the return values of speech recognition engine
            String txt_result=matches.get(0);
            Log.d("hhh",txt_result);
            if(txt_result.equals("back")){
                finish();
            }
            else if ((txt_result.equals("one") || txt_result.equals("1"))&&!clicked_btitemconfig) {
                clicked_btitemconfig=true;
                btitemconfig.performClick(); }
            else if ((txt_result.equals("two") || txt_result.equals("2"))&&!clicked_btroomsconfig) {
                clicked_btroomsconfig=true;
                btroomsconfig.performClick();
                }
        }
        @Override
        public void onRmsChanged(float rmsdB) {}
    }

}

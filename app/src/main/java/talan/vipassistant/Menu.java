package talan.vipassistant;

import androidx.appcompat.app.AppCompatActivity;

import android.Manifest;
import android.content.Context;
import android.content.Intent;
import android.os.Bundle;
import android.speech.RecognitionListener;
import android.speech.RecognizerIntent;
import android.speech.SpeechRecognizer;
import android.speech.tts.TextToSpeech;
import android.speech.tts.UtteranceProgressListener;
import android.util.Log;
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
import java.util.Locale;

public class Menu extends AppCompatActivity {
    private RelativeLayout btSceneDescription, btItemLookup;
    private Speech speech = new Speech();
    private SpeechRecognitionListener speechRecognitionListener = new SpeechRecognitionListener();
    private boolean clicked_btItemLookup;
    private boolean clicked_btSceneDescription;
    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_menu);
        btItemLookup = (RelativeLayout) findViewById(R.id.btItem_lookup);
        btSceneDescription = (RelativeLayout) findViewById(R.id.btSceneDescription);

        btItemLookup.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                Intent intent=new Intent(getApplicationContext(), Itemlookup_main.class);
                startActivity(intent);

            }
        });
        btSceneDescription.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                Intent intent=new Intent(getApplicationContext(), SceneDescription.class);
                startActivity(intent);
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
        clicked_btItemLookup=false;
        clicked_btSceneDescription=false;
        checkaudiopermission();
        speechRecognitionListener.CreateSpeechRecognizer(getApplicationContext(), getPackageName());
        speech.setTts(new TextToSpeech(this, speech));
        speech.speak("This is the menu. Click the buttons. Or say one for "
                + "scene description and two for item lookup", "VOICE_COMMAND_AFTER");
        getVoiceCommand();
        super.onResume();

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

//            Intent speech_intent=new Intent(RecognizerIntent.ACTION_RECOGNIZE_SPEECH);
//            speech_intent.putExtra(RecognizerIntent.EXTRA_LANGUAGE_MODEL,RecognizerIntent.LANGUAGE_MODEL_FREE_FORM);
//            speech_intent.putExtra(RecognizerIntent.EXTRA_LANGUAGE, Locale.ENGLISH);
//            speech_intent.putExtra(RecognizerIntent.EXTRA_PROMPT,"Say something");
//            if (speech_intent.resolveActivity(getPackageManager())!=null){
//                Log.d("STT","Launch recognizing speech input");
//                startActivityForResult(speech_intent,10);
//            }
//            else{
//                Log.d("STT","Your device don't support Speech Input");
//                }
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
//
//    @Override
//    protected void onActivityResult(int requestCode, int resultCode, Intent data) {
//        super.onActivityResult(requestCode, resultCode, data);
//        switch (requestCode) {
//            case 10:
//                if (resultCode == RESULT_OK && data != null) {
//                    ArrayList<String> result = data.getStringArrayListExtra(RecognizerIntent.EXTRA_RESULTS);
//                    String txt_result = result.get(0);
//                    if (txt_result.equals("one") || txt_result.equals("1")) {
//                        btSceneDescription.performClick();
//                        speech.speak("you chose scene description", "");
//                    } else if (txt_result.equals("two") || txt_result.equals("2")) {
//                        btItemLookup.performClick();
//                        speech.speak("you chose item lookup", "");
//                    }
//                }
//                break;
//        }
//    }

    @Override
    public void onStop() {
        if (speech.getTts() != null) {
            speech.getTts().stop();
            speech.getTts().shutdown();
        }
        if (speechRecognitionListener.getmSpeechRecognizer() != null)
        {
            speechRecognitionListener.getmSpeechRecognizer().destroy();
        }
        super.onStop();
    }

    public void checkaudiopermission(){
        Dexter.withActivity(this).withPermission(Manifest.permission.RECORD_AUDIO).withListener(new PermissionListener() {
            @Override
            public void onPermissionGranted(PermissionGrantedResponse permissionGrantedResponse) {

            }
            @Override
            public void onPermissionDenied(PermissionDeniedResponse permissionDeniedResponse) {
                Toast.makeText(getApplicationContext(),"getting ACCESS_FINE_LOCATION permission required",Toast.LENGTH_SHORT).show();
            }
            @Override
            public void onPermissionRationaleShouldBeShown(PermissionRequest permissionRequest, PermissionToken permissionToken) {
                permissionToken.continuePermissionRequest();
            }
        }).check();
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
        public void onBeginningOfSpeech()
        {
            //Log.d(TAG, "onBeginingOfSpeech");
        }

        @Override
        public void onBufferReceived(byte[] buffer)
        {

        }

        @Override
        public void onEndOfSpeech()
        {
            //Log.d(TAG, "onEndOfSpeech");
        }

        @Override
        public void onError(int error)
        {
            mSpeechRecognizer.startListening(mSpeechRecognizerIntent);

            //Log.d(TAG, "error = " + error);
        }

        @Override
        public void onEvent(int eventType, Bundle params)
        {

        }

        @Override
        public void onPartialResults(Bundle partialResults)
        {

        }

        @Override
        public void onReadyForSpeech(Bundle params)
        {
            Log.d("STT", "onReadyForSpeech"); //$NON-NLS-1$
        }

        @Override
        public void onResults(Bundle results)
        {
            ArrayList<String> matches = results.getStringArrayList(SpeechRecognizer.RESULTS_RECOGNITION);
            // matches are the return values of speech recognition engine
            String txt_result=matches.get(0);
            Log.d("hhh",txt_result);
            if ((txt_result.equals("one") || txt_result.equals("1"))&&!clicked_btSceneDescription) {
                clicked_btSceneDescription=true;
                btSceneDescription.performClick();
            } else if ((txt_result.equals("two") || txt_result.equals("2"))&&!clicked_btItemLookup) {
                btItemLookup.performClick();
                clicked_btItemLookup=true;
            }
            else getVoiceCommand();

        }

        @Override
        public void onRmsChanged(float rmsdB)
        {
        }

    }
}

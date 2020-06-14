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

public class Itemlookup_main extends AppCompatActivity {
    RelativeLayout btconfigilmode,btresearchitem;
    private Speech speech = new Speech();
    private SpeechRecognitionListener speechRecognitionListener = new SpeechRecognitionListener();
    private boolean clicked_btconfigilmode;
    private boolean clicked_btresearchitem;
    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_itemlookup_main);
        btconfigilmode = (RelativeLayout) findViewById(R.id.btconfigilmode);
        btresearchitem = (RelativeLayout) findViewById(R.id.btresearchitem);
        checkemailpermission();
        btconfigilmode.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                Intent j=new Intent(getApplicationContext(), config_itemlookup.class);
                j.putExtra("owner",getEmail());
                startActivity(j);
            }
        });

        btresearchitem.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                Intent i=new Intent(getApplicationContext(), Research_item.class);
                i.putExtra("owner",getEmail());
                i.setFlags(Intent.FLAG_ACTIVITY_CLEAR_TOP);
                startActivity(i);
            }
        });
    }

    // used to get the primary email for the device which is used as the parent node for all of the users items in the firebase db
    public String getEmail() {
        String email="";
        Pattern emailPattern = Patterns.EMAIL_ADDRESS;
        Account[] accounts = AccountManager.get(getApplicationContext()).getAccounts();
        for (Account account : accounts) {
            if (emailPattern.matcher(account.name).matches()) {
                //Toast.makeText(QRcode_scanner.this,"email  :"+account.name,Toast.LENGTH_LONG).show();
                String TAG = "--(getEmail)--";
                Log.d(TAG, String.format("%s - %s", account.name, account.type));
                email= account.name;
            }
        }
        return email.replace("."," ");

    }

    public void checkemailpermission(){
        Dexter.withActivity(this).withPermission(Manifest.permission.GET_ACCOUNTS).withListener(new PermissionListener() {
            @Override
            public void onPermissionGranted(PermissionGrantedResponse permissionGrantedResponse) {
            }
            @Override
            public void onPermissionDenied(PermissionDeniedResponse permissionDeniedResponse) {
                Toast.makeText(getApplicationContext(),"email permission required",Toast.LENGTH_SHORT).show();
            }
            @Override
            public void onPermissionRationaleShouldBeShown(PermissionRequest permissionRequest, PermissionToken permissionToken) {
                permissionToken.continuePermissionRequest();
            }
        }).check();
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
        clicked_btconfigilmode=false;
        clicked_btresearchitem=false;
        speechRecognitionListener.CreateSpeechRecognizer(getApplicationContext(), getPackageName());
        speech.setTts(new TextToSpeech(this, speech));
        speech.speak("This is item lookup menu. Click the buttons. Or say one for "
                + "configuration, two for searching an item, or say back", "VOICE_COMMAND_AFTER");
        getVoiceCommand();
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
            if (txt_result.equals("back")){
                finish();
            }
            else if ((txt_result.equals("one") || txt_result.equals("1"))&&!clicked_btconfigilmode) {
                btconfigilmode.performClick();
                clicked_btconfigilmode=true;}
            else if ((txt_result.equals("two") || txt_result.equals("2"))&&!clicked_btresearchitem) {
                clicked_btresearchitem=true;
                btresearchitem.performClick();

            }
        }
        @Override
        public void onRmsChanged(float rmsdB) {}
    }
}

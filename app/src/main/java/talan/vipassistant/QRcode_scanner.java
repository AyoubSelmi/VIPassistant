package talan.vipassistant;

import androidx.annotation.NonNull;
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
import android.widget.Toast;
import com.budiyev.android.codescanner.CodeScanner;
import com.budiyev.android.codescanner.CodeScannerView;
import com.budiyev.android.codescanner.DecodeCallback;
import com.google.firebase.database.DatabaseReference;
import com.google.firebase.database.FirebaseDatabase;
import com.google.zxing.Result;
import com.karumi.dexter.Dexter;
import com.karumi.dexter.PermissionToken;
import com.karumi.dexter.listener.PermissionDeniedResponse;
import com.karumi.dexter.listener.PermissionGrantedResponse;
import com.karumi.dexter.listener.PermissionRequest;
import com.karumi.dexter.listener.single.PermissionListener;

import java.util.ArrayList;

public class QRcode_scanner extends AppCompatActivity {
    CodeScanner codeScanner;
    CodeScannerView qrcode_camera;
    String qrcode;
    DatabaseReference dbref;
    private Speech speech = new Speech();
    private SpeechRecognitionListener speechRecognitionListener = new SpeechRecognitionListener();
    private String owner ;
    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_q_rcode_scanner);
        owner=getIntent().getStringExtra("owner");
        qrcode_camera=(CodeScannerView) findViewById(R.id.qrcode_camera);
        codeScanner=new CodeScanner(this,qrcode_camera);
        checkcamerapermission();
        dbref= FirebaseDatabase.getInstance().getReference().child("items");
        codeScanner.setDecodeCallback(new DecodeCallback() {
            @Override
            public void onDecoded(@NonNull final Result result) {
                runOnUiThread(new Runnable() {
                    @Override
                    public void run() {
                        // for testing purpose
                        qrcode=result.getText();
                        Toast.makeText(QRcode_scanner.this,"QRcodefound ! :"+qrcode,Toast.LENGTH_SHORT).show();
                        item item=new item();
                        // identify owner by the e-mail used for the android system
                        // according to the first two letters of the qr code we get the item type
                        switch(qrcode.substring(0,2)){
                            case "bc":item.setDescription("Blind cane");break;
                            case "mg":item.setDescription("Magnifying glass");break;
                            case "sg":item.setDescription("Sunglasses");break;
                            case "ky":item.setDescription("Keys");break;
                            case "bt":item.setDescription("Bottle");break;
                            default:item.setDescription("Not defined!");
                        }
                        item.setLatitude(" ");
                        item.setLongitude(" ");
                        System.out.println("owner "+owner+" qrcode "+qrcode);
                        dbref.child(owner).child(qrcode.toString()).setValue(item);
                        Intent i=new Intent(getApplicationContext(),config_items.class);
                        i.putExtra("owner",owner);
                        i.setFlags(Intent.FLAG_ACTIVITY_CLEAR_TOP);
                        startActivity(i);
                        finish();
                    }
                });
            }
        });
    }

    @Override
    protected void onPause() {
    codeScanner.releaseResources();
        super.onPause();
    }
    @Override
    protected void onResume() {
        super.onResume();
        speech.setTts(new TextToSpeech(this, speech));
        speech.speak("Please put the object's Q R code in front of the back camera or say back to quit", "VOICE_COMMAND_AFTER");
        speechRecognitionListener.CreateSpeechRecognizer(getApplicationContext(), getPackageName());
        getVoiceCommand();
    }

    @Override
    public void onStop()  {
        if (speech.getTts() != null) {
            speech.getTts().stop();
            speech.getTts().shutdown();
        }
        super.onStop() ;
    }

    public void checkcamerapermission(){
        Dexter.withActivity(this).withPermission(Manifest.permission.CAMERA).withListener(new PermissionListener() {
            @Override
            public void onPermissionGranted(PermissionGrantedResponse permissionGrantedResponse) {
                 codeScanner.startPreview();
            }
            @Override
            public void onPermissionDenied(PermissionDeniedResponse permissionDeniedResponse) {
                Toast.makeText(QRcode_scanner.this,"camera permission required",Toast.LENGTH_SHORT).show();
            }
            @Override
            public void onPermissionRationaleShouldBeShown(PermissionRequest permissionRequest, PermissionToken permissionToken) {
                permissionToken.continuePermissionRequest();
            }
        }).check();
    }


    public void getVoiceCommand() {
        UtteranceProgressListener mProgressListener = new UtteranceProgressListener() {
            @Override
            public void onStart(String utteranceId) {}
            @Override
            public void onError(String utteranceId) {}
            @Override
            public void onDone(String utteranceId) {
                Log.e("ayoub",utteranceId);
                runOnUiThread(new Runnable() {
                    @Override
                    public void run() {
                        speechRecognitionListener.getmSpeechRecognizer().startListening(speechRecognitionListener.getmSpeechRecognizerIntent());

                    }
                });
            }};
        speech.getTts().setOnUtteranceProgressListener(mProgressListener);
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
        }
        @Override
        public void onRmsChanged(float rmsdB) {}
    }

}

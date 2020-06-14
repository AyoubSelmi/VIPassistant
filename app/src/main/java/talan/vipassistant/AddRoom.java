package talan.vipassistant;

import androidx.appcompat.app.AppCompatActivity;

import android.Manifest;
import android.content.Context;
import android.content.Intent;
import android.os.Bundle;
import android.os.Looper;
import android.speech.RecognitionListener;
import android.speech.RecognizerIntent;
import android.speech.SpeechRecognizer;
import android.speech.tts.TextToSpeech;
import android.speech.tts.UtteranceProgressListener;
import android.util.Log;
import android.view.View;
import android.widget.Button;
import android.widget.TextView;
import android.widget.Toast;

import com.google.android.gms.location.LocationCallback;
import com.google.android.gms.location.LocationRequest;
import com.google.android.gms.location.LocationResult;
import com.google.android.gms.location.LocationServices;
import com.google.firebase.database.DatabaseReference;
import com.google.firebase.database.FirebaseDatabase;

import com.karumi.dexter.Dexter;
import com.karumi.dexter.PermissionToken;
import com.karumi.dexter.listener.PermissionDeniedResponse;
import com.karumi.dexter.listener.PermissionGrantedResponse;
import com.karumi.dexter.listener.PermissionRequest;
import com.karumi.dexter.listener.single.PermissionListener;

import java.util.ArrayList;

public class AddRoom extends AppCompatActivity {
    private TextView textlatlong;
    private Button ok;
    private DatabaseReference dbref;
    private String owner;
    private TextView location_corner;
    private Speech speech = new Speech();
    private String roomname;
    private boolean yes;
    private boolean SayingRoomName;
    private SpeechRecognitionListener speechRecognitionListener = new SpeechRecognitionListener();
    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_add_room);
        checklocationpermission();
        owner=getIntent().getExtras().getString("owner");
        dbref= FirebaseDatabase.getInstance().getReference().child("rooms").child(owner);
        textlatlong=findViewById(R.id.textlatlong);
        location_corner=findViewById(R.id.location_corner);
        textlatlong.setVisibility(View.INVISIBLE);
        location_corner.setVisibility(View.INVISIBLE);
        ok=findViewById(R.id.ok);
            // guide user through configuring his room

    }
    @Override
    protected void onResume() {
        super.onResume();
        roomname="";
        yes=false;
        SayingRoomName=false;
        speech.setTts(new TextToSpeech(this, speech));
        speech.speak("you are adding a room say yes to continue or back if not", "VOICE_COMMAND_AFTER");
        getCurrentLocation();
        speechRecognitionListener.CreateSpeechRecognizer(getApplicationContext(), getPackageName());
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
    public void guide_config_rooms(final room r,final Button ok){
        getCurrentLocation();
        speech.speak("the button O K is in the middle of the screen. Go to the first corner and click on the button","");
        delay(1000);
        ok.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                delay(1000);
                r.setCorner1_coordinates(textlatlong.getText().toString());
                speech.speak( "go to the second corner and click on the button","corner1");
                ok.setOnClickListener(new View.OnClickListener() {
                    @Override
                    public void onClick(View v) {
                        getCurrentLocation();
                        delay(1000);
                        r.setCorner2_coordinates(textlatlong.getText().toString());
                        speech.speak( "go to the third corner and click on the button", "corner2");
                        ok.setOnClickListener(new View.OnClickListener() {
                            @Override
                            public void onClick(View v) {
                                getCurrentLocation();
                                delay(1000);
                                r.setCorner3_coordinates(textlatlong.getText().toString());
                                speech.speak("go to the forth corner and click on the button", "corner3");
                                ok.setOnClickListener(new View.OnClickListener() {
                                    @Override
                                    public void onClick(View v) {
                                        getCurrentLocation();
                                        delay(1000);
                                        r.setCorner4_coordinates(textlatlong.getText().toString());
                                        speech.speak("say the name of the room and the button","corner4");
                                        speechRecognitionListener.CreateSpeechRecognizer(getApplicationContext(), getPackageName());
                                        getVoiceCommand();
                                        ok.setOnClickListener(new View.OnClickListener() {
                                            @Override
                                            public void onClick(View v) {
                                                if (roomname.isEmpty()) {
                                                    speech.speak("you didn't choose a name for your room, please say it to finish", "roomname_not_correct");
                                                    getVoiceCommand();
                                                } else {
                                                    r.setRoomName(roomname);
                                                    dbref.child(r.getRoomName()).setValue(r);
                                                    delay(1000);
                                                    finish();
                                                }
                                            }
                                        });

                                    }
                                });
                            }
                        });
                    }
                });
            }

        });

    }

    public void getCurrentLocation(){
        runOnUiThread(new Runnable() {
            @Override
            public void run() {
                final LocationRequest locationRequest= new LocationRequest();
                locationRequest.setInterval(100);
                locationRequest.setFastestInterval(30);
                locationRequest.setPriority(locationRequest.PRIORITY_HIGH_ACCURACY);
                LocationServices.getFusedLocationProviderClient(AddRoom.this)
                        .requestLocationUpdates(locationRequest,new LocationCallback(){
                            @Override
                            public void onLocationResult(LocationResult locationResult) {
                                super.onLocationResult(locationResult);
                                LocationServices.getFusedLocationProviderClient(AddRoom.this).removeLocationUpdates(this);
                                if (locationResult != null && locationResult.getLocations().size()>0){
                                    int lastestLocationIndex=locationResult.getLocations().size()-1;
                                    double latitude=locationResult.getLocations().get(lastestLocationIndex).getLatitude();
                                    double longitude=locationResult.getLocations().get(lastestLocationIndex).getLongitude();
                                    textlatlong.setText(latitude+","+longitude);
                                }
                            }
                        }, Looper.getMainLooper());
            }
        });
        }
    public void delay(int milliseconds) {
        try{Thread.sleep(milliseconds);}
        catch(InterruptedException e){
            e.printStackTrace();
        }
    }

    public void checklocationpermission(){
        Dexter.withActivity(this).withPermission(Manifest.permission.ACCESS_FINE_LOCATION).withListener(new PermissionListener() {
            @Override
            public void onPermissionGranted(PermissionGrantedResponse permissionGrantedResponse) { }
            @Override
            public void onPermissionDenied(PermissionDeniedResponse permissionDeniedResponse) {
                Toast.makeText(AddRoom.this,"getting ACCESS_FINE_LOCATION permission required",Toast.LENGTH_SHORT).show();
            }
            @Override
            public void onPermissionRationaleShouldBeShown(PermissionRequest permissionRequest, PermissionToken permissionToken) {
                permissionToken.continuePermissionRequest();
            }}).check();
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
                if(utteranceId.equals("corner4")||utteranceId.equals("roomname_not_correct")){
                    SayingRoomName=true;
                }
                if (utteranceId.equals("init")||utteranceId.equals("corner4")||utteranceId.equals("roomname_not_correct")){
                    runOnUiThread(new Runnable() {
                        @Override
                        public void run() {
                            speechRecognitionListener.getmSpeechRecognizer().startListening(speechRecognitionListener.getmSpeechRecognizerIntent());
                        }
                    });}
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
        { mSpeechRecognizer.startListening(mSpeechRecognizerIntent);}
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
            else if (txt_result.equals("yes")&&!yes){
                yes=true;
                room r=new room();
                guide_config_rooms(r,ok);}
            else if (SayingRoomName==true){
                SayingRoomName=false;
                roomname = txt_result;
                ok.performClick();
            }
        }
        @Override
        public void onRmsChanged(float rmsdB) {}
    }
}
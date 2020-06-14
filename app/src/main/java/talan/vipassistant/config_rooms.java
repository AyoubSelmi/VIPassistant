package talan.vipassistant;

import androidx.appcompat.app.AppCompatActivity;
import androidx.annotation.NonNull;
import androidx.recyclerview.widget.LinearLayoutManager;
import androidx.recyclerview.widget.RecyclerView;

import android.content.Context;
import android.content.Intent;
import android.os.Bundle;
import android.speech.RecognitionListener;
import android.speech.RecognizerIntent;
import android.speech.SpeechRecognizer;
import android.speech.tts.TextToSpeech;
import android.speech.tts.UtteranceProgressListener;
import android.util.Log;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.ImageButton;

import com.firebase.ui.database.FirebaseRecyclerAdapter;
import com.firebase.ui.database.FirebaseRecyclerOptions;
import com.google.firebase.database.DatabaseReference;
import com.google.firebase.database.FirebaseDatabase;

import java.util.ArrayList;

public class config_rooms extends AppCompatActivity {
    private ImageButton AddRoom;
    private DatabaseReference DBref;
    private RecyclerView recyclerView;
    private String owner;
    private FirebaseRecyclerOptions<room> options;
    private FirebaseRecyclerAdapter<room,MyViewHolderRoom> adapter;
    private ArrayList<String>roomNames=new ArrayList<String>();
    private String roomName;
    private boolean SayingRoomName;
    private boolean clicked_AddRoom;
    private boolean Deleted_room;
    private Speech speech = new Speech();
    private SpeechRecognitionListener speechRecognitionListener = new SpeechRecognitionListener();

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_config_rooms);
        owner= getIntent().getExtras().getString("owner");
        AddRoom=(ImageButton) findViewById(R.id.btAddRoom);
        AddRoom.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                Intent i=new Intent(getApplicationContext(),AddRoom.class);
                i.putExtra("owner",getIntent().getExtras().getString("owner"));
                startActivity(i);
            }
        });
        populate_recyclerview(owner);
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
        clicked_AddRoom=false;
        roomName="";
        SayingRoomName=false;
        Deleted_room=false;
        speechRecognitionListener.CreateSpeechRecognizer(getApplicationContext(), getPackageName());
        speech.setTts(new TextToSpeech(this, speech));
        speech.speak("This is room configuration. Click the buttons. Or say one for "
                + "adding a room , two for listing the existing rooms, or say back ", "VOICE_COMMAND_AFTER");
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

    public void populate_recyclerview(final String owner){
        // VIP(visually impaired person) is the owner.
        DBref=FirebaseDatabase.getInstance().getReference().child("rooms").child(owner);
        // we get under the node of the owner where all his items are listed using the qrcode as the node to access each one attributes
        //Now we need to populate our recyclerview with the items of the VIperson
        recyclerView=(RecyclerView) findViewById(R.id.recyclerview_rooms);
        recyclerView.setHasFixedSize(true);
        recyclerView.setLayoutManager(new LinearLayoutManager(this));
        options=new FirebaseRecyclerOptions.Builder<room>().setQuery(DBref,room.class).build();
        adapter=new FirebaseRecyclerAdapter<room, MyViewHolderRoom>(options) {
            @Override
            protected void onBindViewHolder(@NonNull MyViewHolderRoom holder,final int position, @NonNull room model) {
                holder.room_description.setText(""+model.getRoomName());
                roomNames.add(model.getRoomName());
                //delete a room when clicked
                holder.itemView.setOnClickListener(new View.OnClickListener() {
                    @Override
                    public void onClick(View v) {
                        deleteItem(owner,getRef(position).getKey());
                    }
                });
            }
            @NonNull
            @Override
            public MyViewHolderRoom onCreateViewHolder(@NonNull ViewGroup parent, int viewType) {
                View v=LayoutInflater.from(parent.getContext()).inflate(R.layout.room_details,parent,false);
                return new MyViewHolderRoom(v);
            }
        };
        adapter.startListening();
        recyclerView.setAdapter(adapter);
    }

    public void deleteItem(String owner,String room_name){
        FirebaseDatabase.getInstance().getReference().child("rooms").child(owner).child(room_name).removeValue();
    }
    public void list_rooms(){
        String rooms="";
        if (roomNames.size()==0){speech.speak("no rooms saved","no rooms");}
        else {
            for (int i = 0; i < roomNames.size(); i++) {
                rooms += roomNames.get(i) + ", ";
            }
            speech.speak("your rooms are " + rooms + "say the name of the room you want to delete", "list_rooms");
        }
    }
    public void Delete_room_from_voice_command(){
        list_rooms();
        getVoiceCommand();
    }
public boolean Delete_room(){
    if (roomNames.contains(roomName)){
        deleteItem(owner,roomName);
        return true;
    }
    else {
        speech.speak("the room name is not correct say it again","room name not correct");
    return false;}
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
                if (utteranceId.equals("list_rooms")){SayingRoomName=true;}
                System.out.println("utteranceID is "+utteranceId+" and SayingRoomName is "+SayingRoomName);
                if(utteranceId.equals("deleted room")){
                    Intent i=new Intent(getApplicationContext(),config_rooms.class);
                    i.putExtra("owner",owner);
                    startActivity(i);
                }
                else{
                runOnUiThread(new Runnable() {
                    @Override
                    public void run() {
                        speechRecognitionListener.getmSpeechRecognizer().startListening(speechRecognitionListener.getmSpeechRecognizerIntent());
                    }
                });}
            }
        };
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
            else if ((txt_result.equals("one") || txt_result.equals("1"))&&!clicked_AddRoom) {
                clicked_AddRoom=true;
                AddRoom.performClick(); }
            else if ((txt_result.equals("two") || txt_result.equals("2"))&&!Deleted_room) {
                Deleted_room=true;
                Delete_room_from_voice_command(); }
            else if(SayingRoomName==true){
                roomName=txt_result;
                Log.e("ayoub","roomName="+roomName);
                if(Delete_room()){
                    SayingRoomName=false;
                    speech.speak("you deleted "+roomName,"deleted room");
                }
                else{
                    roomName="";
                    getVoiceCommand();
                    }
                }
        }
        @Override
        public void onRmsChanged(float rmsdB) {}
    }
}

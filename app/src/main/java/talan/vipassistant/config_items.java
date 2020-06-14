package talan.vipassistant;

import androidx.annotation.NonNull;
import androidx.appcompat.app.AppCompatActivity;
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
import java.util.HashMap;

public class config_items extends AppCompatActivity {
    private ImageButton AddItem;
    private DatabaseReference DBref;
    private RecyclerView recyclerView;
    private String owner;

    private FirebaseRecyclerOptions<item> options;
    private FirebaseRecyclerAdapter<item,MyViewHolder> adapter;

    private HashMap<String,String> itemNames=new HashMap<String, String>();
    private boolean clicked_AddItem;
    private String itemName;
    private boolean SayingItemName;
    private boolean Delete_item;
    private Speech speech = new Speech();
    private SpeechRecognitionListener speechRecognitionListener = new SpeechRecognitionListener();

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_config_items);
        AddItem=(ImageButton) findViewById(R.id.btAddItem);
        owner= getIntent().getExtras().getString("owner");
        AddItem.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                Intent i=new Intent(getApplicationContext(),QRcode_scanner.class);
                i.putExtra("owner",owner);
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
        Log.e("ayoub","onRestart config_items");
        super.onRestart();
    }

    @Override
    protected void onResume() {
        super.onResume();
        clicked_AddItem=true;
        itemName="";
        SayingItemName=false;
        Delete_item=false;
        speechRecognitionListener.CreateSpeechRecognizer(getApplicationContext(), getPackageName());
        speech.setTts(new TextToSpeech(this, speech));
        speech.speak("This is items configuration. Click the buttons. Or say one for "
                + "adding an item, two for listing the existing items, or say back", "VOICE_COMMAND_AFTER");
        getVoiceCommand();
        Log.e("ayoub","onResume config_items");
    }


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
        super.onStop() ;
    }

    public void populate_recyclerview(final String owner){
        // VIP(visually impaired person) is the owner.
        DBref=FirebaseDatabase.getInstance().getReference().child("items").child(owner);
        // we get under the node of the owner where all his items are listed using the qrcode as the node to access each one attributes
        //Now we need to populate our recyclerview with the items of the VIperson
        recyclerView=(RecyclerView) findViewById(R.id.recyclerview_items);
        recyclerView.setHasFixedSize(true);
        recyclerView.setLayoutManager(new LinearLayoutManager(this));
        options=new FirebaseRecyclerOptions.Builder<item>().setQuery(DBref,item.class).build();
        adapter=new FirebaseRecyclerAdapter<item, MyViewHolder>(options) {
            @Override
            protected void onBindViewHolder(@NonNull MyViewHolder holder, final int position, @NonNull item model) {
                holder.item_description.setText(""+model.getDescription());
                itemNames.put(model.getDescription(),getRef(position).getKey());
                // delete an item when clicked
                holder.itemView.setOnClickListener(new View.OnClickListener() {
                    @Override
                    public void onClick(View v) {
                        deleteItem(owner,getRef(position).getKey());
                    }
                });
            }
            @NonNull
            @Override
            public MyViewHolder onCreateViewHolder(@NonNull ViewGroup parent, int viewType) {
                View v=LayoutInflater.from(parent.getContext()).inflate(R.layout.single_view_layout,parent,false);
                return new MyViewHolder(v);
            }
        };
        adapter.startListening();
        recyclerView.setAdapter(adapter);

    }


    public void deleteItem(String owner,String item_qrcode){
        FirebaseDatabase.getInstance().getReference().child("items").child(owner).child(item_qrcode).removeValue();
    }

    public void list_items(){
        String items="";
        if (itemNames.size()==0){speech.speak("no items saved","no items");}
        else {
            for (String Value:itemNames.keySet()) {
                items += Value + ", ";
            }
            speech.speak("your items are " + items + "say the name of the item you want to delete", "list_items");
        }
    }
    public void Delete_item_from_voice_command(){
        list_items();
        getVoiceCommand();
    }
    public boolean Delete_item(){
       String value="";
        for ( String Value:itemNames.keySet()) {
            if(itemName.toLowerCase().equals(Value.toLowerCase())){
                itemName=Value;
                value=Value;
                break;
            }
        }
        Log.i("Delete_item","itemName="+itemName+" and itemNames.get(i)="+value);
        if (itemName.equals(value)){
            deleteItem(owner,itemNames.get(value));
        return true;}
        else {speech.speak("the item name is not correct","item name not correct");
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
                if (utteranceId.equals("list_items")){SayingItemName=true;}
                else if(utteranceId.equals("deleted item")){
                    Intent i=new Intent(getApplicationContext(),config_items.class);
                    i.putExtra("owner",owner);
                    startActivity(i);
                }
                System.out.println("utteranceID is "+utteranceId+" and SayingItemName is "+SayingItemName);
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
            else if ((txt_result.equals("one") || txt_result.equals("1"))&&!clicked_AddItem) {
                clicked_AddItem=true;
                AddItem.performClick(); }
            else if ((txt_result.equals("two") || txt_result.equals("2"))&&!Delete_item) {
                Delete_item=true;
                Delete_item_from_voice_command(); }
            else if(SayingItemName==true){
                itemName=txt_result;
                if (Delete_item()){
                Log.e("ayoub","itemName="+itemName);
                SayingItemName=false;
                Delete_item();
                speech.speak("you deleted "+itemName,"deleted item");
                }
                else{
                    itemName="";
                    getVoiceCommand();
                }
            }

        }
        @Override
        public void onRmsChanged(float rmsdB) {}
    }
}

package talan.vipassistant;

import androidx.annotation.NonNull;
import androidx.appcompat.app.AppCompatActivity;
import androidx.recyclerview.widget.LinearLayoutManager;
import androidx.recyclerview.widget.RecyclerView;

import android.content.Context;
import android.content.Intent;
import android.location.Location;
import android.os.Bundle;
import android.os.Handler;
import android.os.ResultReceiver;
import android.speech.RecognitionListener;
import android.speech.RecognizerIntent;
import android.speech.SpeechRecognizer;
import android.speech.tts.TextToSpeech;
import android.speech.tts.UtteranceProgressListener;
import android.text.Editable;
import android.text.TextWatcher;
import android.util.Log;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.Button;
import android.widget.TextView;
import android.widget.Toast;

import com.firebase.ui.database.FirebaseRecyclerAdapter;
import com.firebase.ui.database.FirebaseRecyclerOptions;
import com.google.firebase.database.DataSnapshot;
import com.google.firebase.database.DatabaseError;
import com.google.firebase.database.DatabaseReference;
import com.google.firebase.database.FirebaseDatabase;
import com.google.firebase.database.ValueEventListener;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.DoubleSummaryStatistics;
import java.util.HashMap;
import java.util.Iterator;
import java.util.List;

public class Research_item extends AppCompatActivity {
    private DatabaseReference DBref;
    private RecyclerView recyclerView;
    private ResultReceiver resultReceiver;
    private FirebaseRecyclerOptions<item> options;
    private FirebaseRecyclerAdapter<item,MyViewHolder> adapter;

    private String address;
    private TextView address_itm;
    private Button launch_camera;
    private String result_room;
    private String description;
    private String  owner;
    private HashMap<String, String>itemNames= new HashMap<String, String>();
    private String itemName="";
    private boolean Issearching_item=false;
    private boolean SayingItemName=false;
    private Speech speech = new Speech();
    private SpeechRecognitionListener speechRecognitionListener = new SpeechRecognitionListener();

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_research_item);
        owner= getIntent().getExtras().getString("owner");
        resultReceiver = new AddressResultReceiver(new Handler());
        address_itm=(TextView) findViewById(R.id.address);
        launch_camera=(Button)findViewById(R.id.launch_camera);
        /**/
        launch_camera.setEnabled(false);
        launch_camera.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                Intent k=new Intent(getApplicationContext(), camera_detection.class);
                k.putExtra("object_class",description);
                startActivity(k);
            }
        });
        address_itm.setVisibility(View.INVISIBLE);
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
        populate_recyclerview(owner);
        speechRecognitionListener.CreateSpeechRecognizer(getApplicationContext(), getPackageName());
        speech.setTts(new TextToSpeech(this, speech));
        speech.speak("if you want to list your items say yes otherwise say back", "VOICE_COMMAND_AFTER");
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
        DBref= FirebaseDatabase.getInstance().getReference().child("items").child(owner);
        // we get under the node of the owner where all his items are listed using the qrcode as the node to access each one attributes
        //Now we need to populate our recyclerview with the items of the VIperson
        recyclerView=(RecyclerView) findViewById(R.id.recyclerview_items_lookup);
        recyclerView.setVisibility(View.INVISIBLE);
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
                        Search_for_item(owner,getRef(position).getKey());
                    }
                });
            }
            @NonNull
            @Override
            public MyViewHolder onCreateViewHolder(@NonNull ViewGroup parent, int viewType) {
                View v= LayoutInflater.from(parent.getContext()).inflate(R.layout.single_view_layout,parent,false);
                return new MyViewHolder(v);
            }
        };
        adapter.startListening();
        recyclerView.setAdapter(adapter);
    }

    public void list_items(){
        String items="";
        if (itemNames.size()==0){speech.speak("no items saved","no items");}
        else {
            for (String Value:itemNames.keySet()) {
                items += Value + ", ";
            }
            speech.speak("your items are " + items + "say the name of the item you want lookup", "list_items");
        }
    }
    private void select_item_and_begin(){
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
            Search_for_item(owner,itemNames.get(value));}
        else {speech.speak("the item name is not correct","item name not correct");}

    }

    public void Search_for_item(final String owner,String item_qrcode){
        final DatabaseReference selected_item=FirebaseDatabase.getInstance().getReference().child("items").child(owner).child(item_qrcode);
        selected_item.addValueEventListener(new ValueEventListener() {
            @Override
            public void onDataChange(@NonNull final DataSnapshot dataSnapshot) {
                String Latitude=dataSnapshot.child("latitude").getValue().toString();
                String Longitude=dataSnapshot.child("longitude").getValue().toString();
                description=dataSnapshot.child("description").getValue().toString();
                result_room="not found";
                Compare_itemCoord_to_rooms(Latitude,Longitude,owner);
            }
            @Override
            public void onCancelled(@NonNull DatabaseError databaseError) {
            }
        });
    }
    public void Compare_itemCoord_to_rooms(final String Latitude, final String Longitude, String owner){
        // we will loop through the children of "rooms/owner/"
        // we test if the lat and long of the item are in between the min and max of lat , and min and max of long of each room
        // if so the element is in the room

        final DatabaseReference rooms_ref= FirebaseDatabase.getInstance().getReference().child("rooms").child(owner);

        rooms_ref.addValueEventListener(new ValueEventListener() {
            @Override
            public void onDataChange(@NonNull DataSnapshot dataSnapshot) {
                Iterator<DataSnapshot> rooms=dataSnapshot.getChildren().iterator();
                List<Double>min_max_lat_long;
                boolean room_found= false;

                //for(DataSnapshot child : dataSnapshot.getChildren()){
                 while(rooms.hasNext()&&room_found==false){
                    DataSnapshot room= rooms.next();
                    min_max_lat_long=CalculateArea_latlong(
                            room.child("corner1_coordinates").getValue().toString()
                            ,room.child("corner2_coordinates").getValue().toString()
                            ,room.child("corner3_coordinates").getValue().toString()
                            ,room.child("corner4_coordinates").getValue().toString());
                    if ((Double.parseDouble(Latitude)>=min_max_lat_long.get(0) )
                            &&(Double.parseDouble(Latitude)<=min_max_lat_long.get(1) )
                            &&(Double.parseDouble(Longitude)>=min_max_lat_long.get(2) )
                            &&(Double.parseDouble(Longitude)<= min_max_lat_long.get(3))){
                        result_room=room.child("roomName").getValue().toString();
                        room_found=true;
                    }
                     if(result_room=="not found"){
                         // the object is outdoor
                         // we give the address where the object is
                         Location location=new Location("ProviderNA");
                         location.setLatitude( Double.parseDouble(Latitude));
                         location.setLongitude( Double.parseDouble(Longitude));
                         fetchAddressFromLatLong(location);
                         // wait till the address is filled

                         if (address==""){
                             speech.speak("could not find the address of the object.\n Try again.","address not found");
                         }
                         else{
                             address_itm.addTextChangedListener(new TextWatcher() {
                                 @Override
                                 public void beforeTextChanged(CharSequence s, int start, int count, int after) {
                                 }
                                 @Override
                                 public void onTextChanged(CharSequence s, int start, int before, int count) {
                                     launch_camera.setEnabled(true);
                                     speech.speak("the object is at "+address+", click on the button on the screen to launch identification via camera", "object_address");
                                 }
                                 @Override
                                 public void afterTextChanged(Editable s) {
                                 }
                             });
                         }
                     }
                     else {
                         // the object is indoor
                         // so we tell the user in which room the object is and we tell him to click the button if he wants to
                         // use camera to identify the object
                         launch_camera.setEnabled(true);
                         speech.speak( "the item is in the "+result_room+
                                 "\nClick on the button on the screen to launch identification via camera", "item_room");
                     }
                }
            }

            @Override
            public void onCancelled(@NonNull DatabaseError databaseError) {

            }
        });
    }
    public List<Double> CalculateArea_latlong(String corner1,String corner2,String corner3,String corner4){
        List<Double>result=new ArrayList<>();
        double[] latitudes = {Double.parseDouble(corner1.split(",")[0]),Double.parseDouble(corner2.split(",")[0])
                            , Double.parseDouble(corner3.split(",")[0]),Double.parseDouble(corner4.split(",")[0])};
        double[] longitudes={Double.parseDouble(corner1.split(",")[1]),Double.parseDouble(corner2.split(",")[1])
                , Double.parseDouble(corner3.split(",")[1]),Double.parseDouble(corner4.split(",")[1])};
        DoubleSummaryStatistics stat_lat = Arrays.stream(latitudes).summaryStatistics();
        DoubleSummaryStatistics stat_long = Arrays.stream(longitudes).summaryStatistics();
        result.add(stat_lat.getMin());
        result.add(stat_lat.getMax());
        result.add(stat_long.getMin());
        result.add(stat_long.getMax());
        return result;
    }
    // have the service look for the address of the location and get the result through the resultReceiver
    private void fetchAddressFromLatLong(Location location){
        Intent intent= new Intent(this,FetchAddresIntentService.class);
        intent.putExtra("talan.vipassistant.RECEIVER",resultReceiver);
        intent.putExtra("talan.vipassistant.LOCATION_DATA_EXTRA",location);
        startService(intent);
    }

    // define our custom resultReceiver
    // when we get the address what do we do ? => define the needed task in onReceiveResult
    private class AddressResultReceiver extends ResultReceiver {
        AddressResultReceiver(Handler handler) {
            super(handler);
        }

        @Override
        protected void onReceiveResult(int resultCode, Bundle resultData) {
            super.onReceiveResult(resultCode, resultData);
            //success code is 1
            if (resultCode==1){
                address=resultData.getString("talan.vipassistant.RESULT_DATA_KEY");
                address_itm.setText(address);
            }
            else{
                address="";
                Toast.makeText(Research_item.this,"talan.vipassistant.RESULT_DATA_KEY" , Toast.LENGTH_SHORT).show();
            }
        }
    }
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
            if(txt_result.equals("yes")&&!Issearching_item) {Issearching_item=true;
                list_items();
                getVoiceCommand();
               }
             else if(SayingItemName==true) {
                itemName=txt_result;
                Log.e("ayoub","itemName="+itemName);
                SayingItemName=false;
                select_item_and_begin();
            }
        }

        @Override
        public void onRmsChanged(float rmsdB) {}
    }
}
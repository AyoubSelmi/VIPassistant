package talan.vipassistant;

import android.app.IntentService;
import android.content.Intent;
import android.location.Address;
import android.location.Geocoder;
import android.location.Location;
import android.os.Bundle;
import android.os.ResultReceiver;
import android.text.TextUtils;

import androidx.annotation.Nullable;

import java.util.ArrayList;
import java.util.Locale;
import java.util.List;
import java.util.Objects;

public class FetchAddresIntentService extends IntentService {
    private ResultReceiver resultReceiver;
    public FetchAddresIntentService() {
        super("FetchAddressIntentService");
    }

    @Override
    protected void onHandleIntent(@Nullable Intent intent) {
        if (intent!=null){
            String errorMessage="";
            resultReceiver=intent.getParcelableExtra("talan.vipassistant.RECEIVER");
            Location location=intent.getParcelableExtra("talan.vipassistant.LOCATION_DATA_EXTRA");
            if (location==null){
                return;
            }
            Geocoder geocoder= new Geocoder(this, Locale.getDefault());
            List<Address> addresses=null;
            try{
                addresses=geocoder.getFromLocation(location.getLatitude(),location.getLongitude(),1);
            }
            catch(Exception exception){
                errorMessage=exception.getMessage();
            }
            if (addresses==null || addresses.isEmpty()){
                //failure result code =0
                deliverResultToReceiver( 0,errorMessage);
            }
            else{
                Address address=addresses.get(0);
                ArrayList<String> addressFragments=new ArrayList<>();
                for(int i=0;i<=address.getMaxAddressLineIndex();i++){
                    addressFragments.add(address.getAddressLine(i));
                }
                //result success code is 1
                deliverResultToReceiver(
                        1,
                        TextUtils.join(
                                Objects.requireNonNull(System.getProperty("line.separator"))
                                ,addressFragments)
                        );
            }
        }
    }
    public void deliverResultToReceiver(int resultCode,String addressMessage){
        Bundle bundle=new Bundle();
        bundle.putString("talan.vipassistant.RESULT_DATA_KEY",addressMessage);
        resultReceiver.send(resultCode,bundle);
    }
}

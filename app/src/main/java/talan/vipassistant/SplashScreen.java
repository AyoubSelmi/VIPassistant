package talan.vipassistant;

import androidx.appcompat.app.AppCompatActivity;

import android.content.Intent;
import android.os.Bundle;
import android.speech.tts.TextToSpeech;
import android.view.animation.Animation;
import android.view.animation.AnimationUtils;
import android.widget.ImageView;


public class SplashScreen extends AppCompatActivity {
    private ImageView logo;
    private Speech speech = new Speech();
    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_splash_screen);
        speech.setTts(new TextToSpeech(this, speech));
        speech.speak("welcome to VIP assistant","welcome app");
        final Intent i = new Intent(this, Menu.class);


        Thread Timer=new Thread(){
            public void run(){
                try{
                    sleep(3000);
                }
                catch (InterruptedException e){e.printStackTrace();}
                finally {
                    startActivity(i);
                    i.setFlags(Intent.FLAG_ACTIVITY_CLEAR_TOP);
                    finish();
                }
            }
        };
        Timer.start();
    }
    @Override
    public void onDestroy() {
        if (speech.getTts()!=null){
            speech.getTts().stop();
            speech.getTts().shutdown();
        }
        super.onDestroy();
    }
}

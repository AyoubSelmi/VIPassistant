package talan.vipassistant;

import androidx.appcompat.app.AppCompatActivity;
import androidx.core.app.ActivityCompat;
import androidx.core.content.ContextCompat;

import android.Manifest;
import android.content.pm.PackageManager;
import android.os.Environment;
import android.os.Bundle;
import android.speech.tts.TextToSpeech;
import android.util.Log;
import android.view.SurfaceView;
import android.view.View;
import android.widget.Toast;

import org.opencv.android.BaseLoaderCallback;
import org.opencv.android.CameraBridgeViewBase;
import org.opencv.android.JavaCameraView;
import org.opencv.android.OpenCVLoader;
import org.opencv.core.Core;
import org.opencv.core.Mat;
import org.opencv.core.MatOfFloat;
import org.opencv.core.MatOfInt;
import org.opencv.core.MatOfRect;
import org.opencv.core.Point;
import org.opencv.core.Rect;
import org.opencv.core.Scalar;
import org.opencv.core.Size;
import org.opencv.dnn.Net;
import org.opencv.imgproc.Imgproc;

import org.opencv.dnn.Dnn;
import org.opencv.utils.Converters;


import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;

public class camera_detection extends AppCompatActivity implements CameraBridgeViewBase.CvCameraViewListener2 {
    private String item_to_lookup;
    private Speech speech = new Speech();
    CameraBridgeViewBase cameraBridgeViewBase;
    BaseLoaderCallback baseLoaderCallback;
    boolean startYolo = false;
    boolean firstTimeYolo = false;
    Net tinyYolo;
    private static final int EXT_STORAGE_PERMISSION_CODE = 101;
    private String tmpFolder = Environment.getExternalStorageDirectory()+ "/blindAssistant";


    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_main);
        if (ContextCompat.checkSelfPermission(
                camera_detection.this,
                Manifest.permission.WRITE_EXTERNAL_STORAGE)
                == PackageManager.PERMISSION_DENIED) {
            ActivityCompat.requestPermissions(
                    camera_detection.this,
                    new String[]{Manifest.permission.WRITE_EXTERNAL_STORAGE},
                    EXT_STORAGE_PERMISSION_CODE);
            Log.d("TAG", "After getting permission: "+ Manifest.permission.WRITE_EXTERNAL_STORAGE + " " + ContextCompat.checkSelfPermission(
                    camera_detection.this,
                    Manifest.permission.WRITE_EXTERNAL_STORAGE));
        } else {
            // We were granted permission already before
            Log.d("TAG", "Already has permission to write to external storage");
        }
        item_to_lookup= getIntent().getExtras().getString("object_class");
        cameraBridgeViewBase = (JavaCameraView)findViewById(R.id.CameraView);
        cameraBridgeViewBase.setVisibility(SurfaceView.VISIBLE);
        cameraBridgeViewBase.setCvCameraViewListener(this);


        //System.loadLibrary(Core.NATIVE_LIBRARY_NAME);
        //System.loadLibrary(Core.NATIVE_LIBRARY_NAME);
        baseLoaderCallback = new BaseLoaderCallback(this) {
            @Override
            public void onManagerConnected(int status) {
                super.onManagerConnected(status);
                switch(status){
                    case BaseLoaderCallback.SUCCESS:
                        cameraBridgeViewBase.enableView();
                        break;
                    default:
                        super.onManagerConnected(status);
                        break;
                }
            }
        };
    }

    @Override
    public Mat onCameraFrame(CameraBridgeViewBase.CvCameraViewFrame inputFrame) {
        Log.e("azaz","onCameraFrame");
        if(startYolo==true){
        Log.e("azaz","startyolo==true");}
        else{
            Log.e("azaz","startYolo==false");
        }
        Mat frame = inputFrame.rgba();
//        if (firstTimeYolo == false){
//            firstTimeYolo = true;
//            LoadModel model =new LoadModel(getApplicationContext());
//            //copy model files to tmpFolder
//            model.loadmodel();
//            String tinyYoloCfg =  tmpFolder+"/yolov3-tiny.cfg";//_obj.cfg" ;
//            String tinyYoloWeights =  tmpFolder+ "/yolov3-tiny.weights";
//            tinyYolo = Dnn.readNetFromDarknet(tinyYoloCfg, tinyYoloWeights);
//            if(tinyYolo==null){Log.e("azaz","tinyYolo==null");}
//            else {Log.e("azaz","tinyYolo!=null");}
//            System.out.println("tinyYolo loaded model");
//        }
//        startYolo = true;
        if (startYolo == true) {
            Imgproc.cvtColor(frame, frame, Imgproc.COLOR_RGBA2RGB);
            Mat imageBlob = Dnn.blobFromImage(frame, 0.00392, new Size(416,416),new Scalar(0, 0, 0),/*swapRB*/false, /*crop*/false);
            tinyYolo.setInput(imageBlob);
            java.util.List<Mat> result = new java.util.ArrayList<Mat>(2);
            List<String> outBlobNames = new java.util.ArrayList<>();
            outBlobNames.add(0, "yolo_16");
            outBlobNames.add(1, "yolo_23");
            tinyYolo.forward(result,outBlobNames);
            float confThreshold = 0.2f;
            List<Integer> clsIds = new ArrayList<>();
            List<Float> confs = new ArrayList<>();
            List<Rect> rects = new ArrayList<>();
            for (int i = 0; i < result.size(); ++i)
            {
                Mat level = result.get(i);
                for (int j = 0; j < level.rows(); ++j)
                {
                    Mat row = level.row(j);
                    Mat scores = row.colRange(5, level.cols());
                    Core.MinMaxLocResult mm = Core.minMaxLoc(scores);
                    float confidence = (float)mm.maxVal;
                    Point classIdPoint = mm.maxLoc;
                    if (confidence > confThreshold)
                    {
                        int centerX = (int)(row.get(0,0)[0] * frame.cols());
                        int centerY = (int)(row.get(0,1)[0] * frame.rows());
                        int width   = (int)(row.get(0,2)[0] * frame.cols());
                        int height  = (int)(row.get(0,3)[0] * frame.rows());
                        int left    = centerX - width  / 2;
                        int top     = centerY - height / 2;
                        clsIds.add((int)classIdPoint.x);
                        confs.add((float)confidence);
                        rects.add(new Rect(left, top, width, height));
                    }
                }
            }
            int ArrayLength = confs.size();
            if (ArrayLength>=1) {
                // Apply non-maximum suppression procedure.
                float nmsThresh = 0.2f;
                MatOfFloat confidences = new MatOfFloat(Converters.vector_float_to_Mat(confs));
                Rect[] boxesArray = rects.toArray(new Rect[0]);
                MatOfRect boxes = new MatOfRect(boxesArray);
                MatOfInt indices = new MatOfInt();
                Dnn.NMSBoxes(boxes, confidences, confThreshold, nmsThresh, indices);
                // Draw result boxes:
                int[] ind = indices.toArray();
                for (int i = 0; i < ind.length; ++i) {
                    int idx = ind[i];
                    Rect box = boxesArray[idx];
                    int idGuy = clsIds.get(idx);
                    float conf = confs.get(idx);
                    List<String> cocoNames = Arrays.asList("blind cane","keys","bottle","Sunglasses","Magnifying glass");
                    int intConf = (int) (conf * 100);
                    Imgproc.putText(frame,cocoNames.get(idGuy) + " " + intConf + "%",box.tl(),Core.FONT_HERSHEY_SIMPLEX, 2, new Scalar(255,255,0),2);
                    Imgproc.rectangle(frame, box.tl(), box.br(), new Scalar(255, 0, 0), 2);
                    if(item_to_lookup.equals(cocoNames.get(idGuy))) {
                        speech.speak(item_to_lookup + " found", "detected_object");
                    }
                }
            }
        }
        return frame;
    }
    @Override
    public void onCameraViewStarted(int width, int height) {
        Log.e("azaz","onCameraViewStarted");
        LoadModel model =new LoadModel(getApplicationContext());
        model.loadmodel();
        String tinyYoloCfg =  tmpFolder+"/yolov3-tiny.cfg";//_obj.cfg" ;
        String tinyYoloWeights =  tmpFolder+ "/yolov3-tiny.weights";
        tinyYolo = Dnn.readNetFromDarknet(tinyYoloCfg, tinyYoloWeights);
        if(tinyYolo==null){Log.e("azaz","tinyYolo==null");}
        else {Log.e("azaz","tinyYolo!=null");}
        System.out.println("tinyYolo loaded model");
        startYolo=true;

    }
    @Override
    public void onCameraViewStopped() {

    }
    @Override
    protected void onResume() {
        super.onResume();
        speech.setTts(new TextToSpeech(this, speech));
        speech.speak("detection started. Move your phone around slowly", "VOICE_COMMAND_AFTER");
        if (!OpenCVLoader.initDebug()){
            Toast.makeText(getApplicationContext(),"There's a problem in loading opencv", Toast.LENGTH_SHORT).show();
        }

        else
        {
            baseLoaderCallback.onManagerConnected(baseLoaderCallback.SUCCESS);
        }
    }
    @Override
    protected void onPause() {
        super.onPause();
        if(cameraBridgeViewBase!=null){

            cameraBridgeViewBase.disableView();
        }
    }
    @Override
    protected void onRestart() {
        speech = new Speech();
        super.onRestart();
    }
    @Override
    public void onStop() {
        if (speech.getTts() != null) {
            speech.getTts().stop();
            speech.getTts().shutdown();
        }
        super.onStop() ;
    }
    @Override
    protected void onDestroy() {
        super.onDestroy();
        if (cameraBridgeViewBase!=null){
            cameraBridgeViewBase.disableView();
        }
    }
}

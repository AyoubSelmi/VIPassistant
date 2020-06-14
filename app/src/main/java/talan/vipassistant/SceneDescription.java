package talan.vipassistant;

import androidx.annotation.NonNull;
import androidx.appcompat.app.AppCompatActivity;
import androidx.core.app.ActivityCompat;


import android.Manifest;
import android.content.Context;
import android.content.Intent;
import android.content.pm.PackageManager;
import android.graphics.Bitmap;
import android.graphics.BitmapFactory;
import android.graphics.ImageFormat;
import android.graphics.SurfaceTexture;
import android.hardware.camera2.CameraAccessException;
import android.hardware.camera2.CameraCaptureSession;
import android.hardware.camera2.CameraCharacteristics;
import android.hardware.camera2.CameraDevice;
import android.hardware.camera2.CameraManager;
import android.hardware.camera2.CameraMetadata;
import android.hardware.camera2.CaptureRequest;
import android.hardware.camera2.TotalCaptureResult;
import android.hardware.camera2.params.StreamConfigurationMap;
import android.media.Image;
import android.media.ImageReader;
import android.os.Bundle;
import android.os.Environment;
import android.os.Handler;
import android.os.HandlerThread;
import android.speech.RecognitionListener;
import android.speech.RecognizerIntent;
import android.speech.SpeechRecognizer;
import android.speech.tts.TextToSpeech;
import android.speech.tts.UtteranceProgressListener;
import android.util.DisplayMetrics;
import android.util.Log;
import android.util.Size;
import android.util.SparseIntArray;
import android.view.Surface;
import android.view.TextureView;
import android.view.View;
import android.widget.Button;
import android.widget.FrameLayout;

import java.io.ByteArrayOutputStream;
import java.io.File;
import java.io.FileNotFoundException;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.OutputStream;
import java.nio.ByteBuffer;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;

import okhttp3.Call;
import okhttp3.Callback;
import okhttp3.MediaType;
import okhttp3.MultipartBody;
import okhttp3.OkHttpClient;
import okhttp3.Request;
import okhttp3.RequestBody;
import okhttp3.Response;

public class SceneDescription extends AppCompatActivity {
    String selectedImagePath;
    TextureView textureView;
    Button save_picture;
    //Button upload_picture;
    private Speech speech = new Speech();
    private SpeechRecognitionListener speechRecognitionListener = new SpeechRecognitionListener();

    private static final SparseIntArray ORIENTATIONS=new SparseIntArray();
    static{
        ORIENTATIONS.append(Surface.ROTATION_0,90);
        ORIENTATIONS.append(Surface.ROTATION_90,0);
        ORIENTATIONS.append(Surface.ROTATION_180,270);
        ORIENTATIONS.append(Surface.ROTATION_270,180);
    }
    private String cameraId;
    CameraDevice cameraDevice;
    CameraCaptureSession cameraCaptureSession;
    CaptureRequest captureRequest;
    CaptureRequest.Builder captureRequestBuilder;
    private Size imageDimensions;
    private ImageReader imageReader;
    private File file;
    Handler mBackgroundHandler;
    HandlerThread mBackgroundThread;
    private boolean clicked_describe;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_scene_description);
        save_picture=findViewById(R.id.save_picture_scened);
        //upload_picture=findViewById(R.id.upload_picture_scened);
        textureView=(TextureView) findViewById(R.id.texture_scene_description);
        textureView.setSurfaceTextureListener(TextureListener);
        save_picture.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                try {
                    takepicture();
                } catch (CameraAccessException e) {
                    e.printStackTrace();
                }
            }
        });
//        upload_picture.setOnClickListener(new View.OnClickListener() {
//            @Override
//            public void onClick(View v) {
//                Log.e("aaa","inside_upload_onclick");
//                uploadPicture();
//            }
//        });
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
        clicked_describe=false;
        startBackgroundThread();
        if (textureView.isAvailable()){
            try {
                openCamera();
            } catch (CameraAccessException e) {
                e.printStackTrace();
            }
        }
        else{
            textureView.setSurfaceTextureListener(TextureListener);
        }
        speechRecognitionListener.CreateSpeechRecognizer(getApplicationContext(), getPackageName());
        speech.setTts(new TextToSpeech(this, speech));
        speech.speak("This is scene description. Say describe to get the scene description or say back" , "VOICE_COMMAND_AFTER");
        getVoiceCommand();
        super.onResume();
    }
    @Override
    protected void onPause() {
        try {
            stopBackgroundThread();
        } catch (InterruptedException e) {
            e.printStackTrace();
        }
        super.onPause();
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
        super.onStop();
    }
    ///***  camera preview ****////

    TextureView.SurfaceTextureListener TextureListener=new TextureView.SurfaceTextureListener() {
        @Override
        public void onSurfaceTextureAvailable(SurfaceTexture surface, int width, int height) {
            try {openCamera();}
            catch (CameraAccessException e){
                e.printStackTrace();
            }
        }

        @Override
        public void onSurfaceTextureSizeChanged(SurfaceTexture surface, int width, int height) {

        }

        @Override
        public boolean onSurfaceTextureDestroyed(SurfaceTexture surface) {
            return false;
        }

        @Override
        public void onSurfaceTextureUpdated(SurfaceTexture surface) {

        }
    };
    public void openCamera() throws CameraAccessException {
        CameraManager cameraManager=(CameraManager)getSystemService(Context.CAMERA_SERVICE);
        cameraId=cameraManager.getCameraIdList()[0];
        CameraCharacteristics characteristics=cameraManager.getCameraCharacteristics(cameraId);
        StreamConfigurationMap map=characteristics.get(CameraCharacteristics.SCALER_STREAM_CONFIGURATION_MAP);
        imageDimensions=map.getOutputSizes(ImageFormat.JPEG)[0];
    //        SurfaceTexture.class
        if ((ActivityCompat.checkSelfPermission(this, Manifest.permission.CAMERA)!= PackageManager.PERMISSION_GRANTED)
            &&(ActivityCompat.checkSelfPermission(this,Manifest.permission.WRITE_EXTERNAL_STORAGE)!=PackageManager.PERMISSION_GRANTED)){
            ActivityCompat.requestPermissions(SceneDescription.this,new String[]{Manifest.permission.CAMERA,Manifest.permission.WRITE_EXTERNAL_STORAGE},101);
            return;
        }
        setAspectRatioTextureView(imageDimensions.getHeight(),imageDimensions.getWidth());
        cameraManager.openCamera(cameraId,stateCallback,null);
    }
    private final CameraDevice.StateCallback stateCallback= new CameraDevice.StateCallback() {
        @Override
        public void onOpened(@NonNull CameraDevice camera) {
            cameraDevice=camera;
            try {
                CreateCameraPreview();
            } catch (CameraAccessException e) {
                e.printStackTrace();
            }
        }
        @Override
        public void onDisconnected(@NonNull CameraDevice camera) {
            cameraDevice.close();
        }
        @Override
        public void onError(@NonNull CameraDevice camera, int error) {
            cameraDevice.close();
            cameraDevice=null;
        }
    };
    public void CreateCameraPreview() throws CameraAccessException {
        SurfaceTexture texture=textureView.getSurfaceTexture();
        texture.setDefaultBufferSize(imageDimensions.getWidth(),imageDimensions.getHeight());
        Surface surface=new Surface(texture);
        captureRequestBuilder=cameraDevice.createCaptureRequest(CameraDevice.TEMPLATE_PREVIEW);
        captureRequestBuilder.addTarget(surface);
        cameraDevice.createCaptureSession(Arrays.asList(surface), new CameraCaptureSession.StateCallback() {
            @Override
            public void onConfigured( CameraCaptureSession session) {
                    if (cameraDevice==null){return;}
                    cameraCaptureSession=session;
                try {
                    updatePreview();
                } catch (CameraAccessException e) {
                    e.printStackTrace();
                }
            }

            @Override
            public void onConfigureFailed(CameraCaptureSession session) {
                Log.e("onConfigureFailed","onConfigureFailed");
            }
        },null);
    }
    public void updatePreview() throws CameraAccessException {
        if (cameraDevice==null){
            return;
        }
        captureRequestBuilder.set(CaptureRequest.CONTROL_MODE, CameraMetadata.CONTROL_MODE_AUTO);
        cameraCaptureSession.setRepeatingRequest(captureRequestBuilder.build(),null,mBackgroundHandler);
    }


    /***** taking a picture and sending it to the api to get back the description *****/
    public void takepicture() throws CameraAccessException {
        if (cameraDevice==null){return;}
        CameraManager cameraManager=(CameraManager)getSystemService(Context.CAMERA_SERVICE);
        CameraCharacteristics characteristics=cameraManager.getCameraCharacteristics(cameraDevice.getId());
        Size[] jpegsizes=null;
        jpegsizes=characteristics.get(CameraCharacteristics.SCALER_STREAM_CONFIGURATION_MAP).getOutputSizes(ImageFormat.JPEG);
        int width=720;
        int height=1280;
//        if ((jpegsizes!=null)&&(jpegsizes.length>0)){
//            width=jpegsizes[0].getWidth();
//            height=jpegsizes[0].getHeight();
//        }
        ImageReader reader=ImageReader.newInstance(width,height,ImageFormat.JPEG,1);
        List<Surface> outputSurfaces= new ArrayList<>(2);
        outputSurfaces.add(reader.getSurface());
        outputSurfaces.add(new Surface(textureView.getSurfaceTexture()));
        final CaptureRequest.Builder captureBuilder=cameraDevice.createCaptureRequest(CameraDevice.TEMPLATE_STILL_CAPTURE);
        captureBuilder.addTarget(reader.getSurface());
        captureBuilder.set(CaptureRequest.CONTROL_MODE,CameraMetadata.CONTROL_MODE_AUTO);
        int rotation=getWindowManager().getDefaultDisplay().getRotation();
        //////
        DisplayMetrics displayMetrics = new DisplayMetrics();
        getWindowManager().getDefaultDisplay().getMetrics(displayMetrics);
        Log.d("displayMetrics",""+displayMetrics.heightPixels+ " x"+displayMetrics.widthPixels);
        /////
        captureBuilder.set(CaptureRequest.JPEG_ORIENTATION,ORIENTATIONS.get(rotation));
        Long tsLong=System.currentTimeMillis()/1000;
        String ts=tsLong.toString();
        file=new File(this.getExternalFilesDir(null).getAbsolutePath()+"/"+ts+".jpg");
        selectedImagePath=this.getExternalFilesDir(null).getAbsolutePath()+"/"+ts+".jpg";
        final ImageReader.OnImageAvailableListener readerListener= new ImageReader.OnImageAvailableListener() {
            @Override
            public void onImageAvailable(ImageReader reader) {
                Image image=null;
                image=reader.acquireLatestImage();
                ByteBuffer buffer=image.getPlanes()[0].getBuffer();
                byte[] bytes= new byte[buffer.capacity()];
                buffer.get(bytes);
//                Bitmap bitmap = BitmapFactory.decodeByteArray(bytes, 0, bytes.length);
//                ByteArrayOutputStream stream=null;
//                    stream= new ByteArrayOutputStream();
//                    bitmap.compress(Bitmap.CompressFormat.JPEG, 50, stream);
                try {
                    //save(stream.toByteArray());
                    save(bytes);
                } catch (FileNotFoundException e) {
                    e.printStackTrace();
                } catch (IOException e) {
                    e.printStackTrace();
                }

                finally {
                    if (image!=null){
                        image.close();

                    }
                }
            }
        };
        reader.setOnImageAvailableListener(readerListener,mBackgroundHandler);
        final CameraCaptureSession.CaptureCallback captureListener= new CameraCaptureSession.CaptureCallback() {
            @Override
            public void onCaptureCompleted( CameraCaptureSession session,CaptureRequest request,TotalCaptureResult result) {
                super.onCaptureCompleted(session, request, result);
                uploadPicture();
                try {
                    CreateCameraPreview();
                } catch (CameraAccessException e) {
                    e.printStackTrace();
                }
            }
        };
        cameraDevice.createCaptureSession(outputSurfaces, new CameraCaptureSession.StateCallback() {
            @Override
            public void onConfigured(CameraCaptureSession session) {
                try {
                    session.capture(captureBuilder.build(),captureListener,mBackgroundHandler);
                } catch (CameraAccessException e) {
                    e.printStackTrace();
                }
            }

            @Override
            public void onConfigureFailed(CameraCaptureSession session) {

            }
        },mBackgroundHandler);
    }

    private void save(byte[] bytes) throws IOException {
        OutputStream  outputStream=null;
        outputStream= new FileOutputStream(file);
        outputStream.write(bytes);
    }

    @Override
    public void onRequestPermissionsResult(int requestCode, @NonNull String[] permissions, @NonNull int[] grantResults) {
        if (requestCode==101){
            if (grantResults[0]==PackageManager.PERMISSION_DENIED){
               speech.speak("camera permission necessary", "camera permission necessary");
            }
        }
        else if(requestCode==1){
            if(grantResults[0]== PackageManager.PERMISSION_GRANTED){

            }
            else{
                speech.speak( "read external storage Permission Denied", "read external storage Permission Denied");
            }
        }
    }

    private void startBackgroundThread() {
        mBackgroundThread = new HandlerThread("camera background");
        mBackgroundThread.start();
        mBackgroundHandler= new Handler(mBackgroundThread.getLooper());
    }


    private void stopBackgroundThread() throws InterruptedException {
        mBackgroundThread.quitSafely();
        mBackgroundThread.join();
        mBackgroundThread=null;
        mBackgroundHandler=null;
    }
    private void setAspectRatioTextureView(int ResolutionWidth , int ResolutionHeight )
    {
        DisplayMetrics displayMetrics = new DisplayMetrics();
        getWindowManager().getDefaultDisplay().getMetrics(displayMetrics);
        int DSI_height =980;
        int DSI_width = 720;
//        if(ResolutionWidth > ResolutionHeight){
//            int newWidth = DSI_width;
//            int newHeight = ((DSI_width * ResolutionWidth)/ResolutionHeight);
//            updateTextureViewSize(newWidth,newHeight);
//
//        }else {
            int newWidth = DSI_width;
            int newHeight =DSI_width;//((DSI_width * ResolutionHeight)/ResolutionWidth);
            updateTextureViewSize(newWidth,newHeight);
//        }
    }

    private void updateTextureViewSize(int viewWidth, int viewHeight) {
        Log.d("aspectRatio", "TextureView Width : " + viewWidth + " TextureView Height : " + viewHeight);
        textureView.setLayoutParams(new FrameLayout.LayoutParams(viewWidth, viewHeight));
    }

    /////***** requesting image_captioning model api   *******/////
    private void uploadPicture() {
        if (ActivityCompat.checkSelfPermission(getApplicationContext(), Manifest.permission.READ_EXTERNAL_STORAGE) != PackageManager.PERMISSION_GRANTED) {
            ActivityCompat.requestPermissions(SceneDescription.this, new String[]{
                    Manifest.permission.READ_EXTERNAL_STORAGE
            }, 1);
        }
        Log.d("aaa","inside_upload_method");
        connectServer();
    }

    void connectServer(){
        Log.e("aaa","inside connectServer");
        String ipv4Address ="192.168.1.8";
        String portNumber ="5000";
        String postUrl= "http://"+ipv4Address+":"+portNumber+"/";
        ByteArrayOutputStream stream = new ByteArrayOutputStream();
        BitmapFactory.Options options = new BitmapFactory.Options();
        options.inPreferredConfig = Bitmap.Config.RGB_565;
        // Read BitMap by file path
        Bitmap bitmap = BitmapFactory.decodeFile(selectedImagePath, options);
        bitmap.compress(Bitmap.CompressFormat.JPEG, 100, stream);
        byte[] byteArray = stream.toByteArray();

        RequestBody postBodyImage = new MultipartBody.Builder()
                .setType(MultipartBody.FORM)
                .addFormDataPart("image", "androidFlask.jpg", RequestBody.create(MediaType.parse("image/*jpg"), byteArray))
                .build();

        speech.speak( "Please wait", "waiting server response");

        postRequest(postUrl, postBodyImage);
    }

    void postRequest(String postUrl, RequestBody postBody) {

        OkHttpClient client = new OkHttpClient();

        Request request = new Request.Builder()
                .url(postUrl)
                .post(postBody)
                .build();

        client.newCall(request).enqueue(new Callback() {
            @Override
            public void onFailure(Call call, IOException e) {
                // Cancel the post on failure.
                call.cancel();

                // In order to access the TextView inside the UI thread, the code is executed inside runOnUiThread()
                runOnUiThread(new Runnable() {
                    @Override
                    public void run() {
                        speech.speak( "Failed to Connect to Server", "failed to connect");
                    }
                });
            }

            @Override
            public void onResponse(Call call, final Response response) throws IOException {
                // In order to access the TextView inside the UI thread, the code is executed inside runOnUiThread()
                runOnUiThread(new Runnable() {
                    @Override
                    public void run() {
                        try {
                            speech.speak( response.body().string(),"response_description");
                            clicked_describe=false;
                            File file = new File(selectedImagePath);
                            if (file.delete()) {
                                Log.d("deleting_image_file","file Deleted ");
                            } else {
                                Log.d("deleting_image_file","file not Deleted");
                            }

                        } catch (IOException e) {
                            e.printStackTrace();
                        }
                    }
                });
            }
        });
    }

    /////*****  speech and audio description *****/////
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
            Log.d("speech", "onBeginingOfSpeech");
        }

        @Override
        public void onBufferReceived(byte[] buffer)
        {
            Log.d("speech", "onBufferReceived");
        }

        @Override
        public void onEndOfSpeech()
        {
            Log.d("speech", "onEndOfSpeech");
        }

        @Override
        public void onError(int error)
        {
            mSpeechRecognizer.cancel();
            mSpeechRecognizer.startListening(mSpeechRecognizerIntent);
            Log.d("speech", "error = " + error);
            String message="";
            switch (error) {
                case SpeechRecognizer.ERROR_AUDIO:
                    message = "Audio recording error";
                    break;
                case SpeechRecognizer.ERROR_CLIENT:
                    message = "Client side error";
                    break;
                case SpeechRecognizer.ERROR_INSUFFICIENT_PERMISSIONS:
                    message = "Insufficient permissions";
                    break;
                case SpeechRecognizer.ERROR_NETWORK:
                    message = "Network error";
                    break;
                case SpeechRecognizer.ERROR_NETWORK_TIMEOUT:
                    message = "Network timeout";
                    break;
                case SpeechRecognizer.ERROR_NO_MATCH:
                    message = "No match";
                    break;
                case SpeechRecognizer.ERROR_RECOGNIZER_BUSY:
                    message = "RecognitionService busy";
                    Log.e("STT",message);

                    break;
                case SpeechRecognizer.ERROR_SERVER:
                    message = "error from server";
                    break;
                case SpeechRecognizer.ERROR_SPEECH_TIMEOUT:
                    message = "No speech input";
                    Log.e("STT",message);
                    break;
                default:
                    message = "Didn't understand, please try again.";
                    Log.e("STT",message);
                    break;
            }
            Log.e("STT",message);
        }

        @Override
        public void onEvent(int eventType, Bundle params)
        {
            Log.d("speech","onEvent");
        }

        @Override
        public void onPartialResults(Bundle partialResults)
        {
            Log.d("speech","onPartialResults");
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
            if (txt_result.equals("back")){
                finish();
            }
             else if ((txt_result.equals("describe"))&&!clicked_describe) {
                save_picture.performClick();
                clicked_describe=true;
            }
            else {  speech.speak("invalid choice, please say describe or back ","invalide_choice");
            }
        }
        @Override
        public void onRmsChanged(float rmsdB)
        {
            //Log.d("STT","onRmsChanged");
        }
    }
}

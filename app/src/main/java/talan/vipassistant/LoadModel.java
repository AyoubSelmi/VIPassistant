package talan.vipassistant;

import android.content.Context;
import android.content.res.AssetManager;
import android.os.Environment;
import android.util.Log;
import java.io.File;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;
import java.util.ArrayList;
import java.util.List;

public class LoadModel {
    private String TAG =" MainActivity";
    private Context context;
    private String tmpFolder= Environment.getExternalStorageDirectory().getPath()+ "/blindAssistant";
    public LoadModel(Context context){
        this.context=context;

    }
    public void loadmodel(){
        // create or use temporary model directory
        File tmpDir = new File(tmpFolder);
        if (!tmpDir.exists()) {
            Log.d(TAG,"Tmp dir to store model does not exist");
            tmpDir.mkdir();
            Log.d(TAG,"Tmpdir created " + tmpDir.exists());
        } else {
            Log.d(TAG,"Tmpdir already exists " + tmpDir.exists());
        }
        List<String> fileNames = getModelFromAssets(context);
        Log.d(TAG,"Number of model files in assets folder:" + fileNames.size());
        for (final String fileName : fileNames) {
            copyFileFromAssets(fileName,context);
        }

    }

    private void copyFileFromAssets(String fileName, Context context) {
        Log.i(TAG, "Copy file from asset:" + fileName);
        AssetManager assetManager = context.getAssets();
        // file to copy to from assets
        File cacheFile = new File( tmpFolder + "/" + fileName );
        InputStream in = null;
        OutputStream out = null;
        try {
            Log.d(TAG,"Copying from assets folder to cache folder");
            if ( cacheFile.exists() ) {
                // already there. Do not copy
                Log.d(TAG, "Cache file exists at:" + cacheFile.getAbsolutePath());
                return;
            } else {
                Log.d(TAG, "Cache file does NOT exist at:" + cacheFile.getAbsolutePath());
                // TODO: There should be some error catching/validation etc before proceeding
                in = assetManager.open(fileName);
                out = new FileOutputStream(cacheFile);
                copyFile(in, out);

                in.close();
                in = null;
                out.flush();
                out.close();
                out = null;

            }

        } catch (IOException ioe) {
            Log.e(TAG, "Error in copying file from assets " + fileName);
            ioe.printStackTrace();

        }

    }

    private void copyFile(InputStream in, OutputStream out) throws IOException {
        byte[] buffer = new byte[1024];
        int read;
        while ((read = in.read(buffer)) != -1) {
            out.write(buffer, 0, read);
        }
    }

    private List<String> getModelFromAssets(Context context) {
        List<String> ModelFiles = new ArrayList<>();
        AssetManager assetManager = context.getAssets();
        try {
            for (String name : assetManager.list("")) {
                // include files which end with cfg and weights only
                if (name.toLowerCase().endsWith("cfg")||name.toLowerCase().endsWith("weights")) {
                    ModelFiles.add(name);
                }
            }
        } catch (IOException ioe) {
            String mesg = "Could not read files from assets folder";
            Log.e(TAG, mesg);
        }
        return ModelFiles;

    }
}


package co.introtuce.mediapipesegmentationgradle;

import androidx.appcompat.app.AppCompatActivity;
import androidx.core.content.FileProvider;

import android.content.Intent;
import android.graphics.SurfaceTexture;
import android.media.MediaPlayer;
import android.net.Uri;
import android.os.Bundle;
import android.util.Log;
import android.view.Surface;
import android.view.SurfaceView;
import android.view.TextureView;
import android.view.View;
import android.widget.FrameLayout;
import android.widget.TextView;
import android.widget.Toast;

import com.google.mediapipe.components.CameraHelper;
import com.google.mediapipe.components.ExternalTextureConverter;
import com.google.mediapipe.components.FrameProcessor;
import com.google.mediapipe.framework.AndroidAssetUtil;
import com.google.mediapipe.framework.Packet;
import com.google.mediapipe.framework.PacketCallback;
import com.google.mediapipe.glutil.EglManager;

import java.io.File;
import java.io.IOException;
import java.io.InputStream;
import java.util.Date;

import co.introtuce.mediapipesegmentationgradle.helper.CustomFrameProcessor;
import co.introtuce.mediapipesegmentationgradle.helper.MyCusExternalTextureConverter;
import co.introtuce.mediapipesegmentationgradle.helper.MyPermissionHelper;
import co.introtuce.mediapipesegmentationgradle.helper.SaveLocal;
import co.introtuce.mediapipesegmentationgradle.helper.SessionManager;
import co.introtuce.mediapipesegmentationgradle.videoviews.AutoFitTextureView;
import co.introtuce.mediapipesegmentationgradle.videoviews.MyGLSurfaceView;
import co.introtuce.mediapipesegmentationgradle.videoviews.MySurfaceTexture;
import co.introtuce.mediapipesegmentationgradle.videoviews.RecordableSurfaceView;

import static co.introtuce.mediapipesegmentationgradle.helper.LogManager.calcCpuCoreCount;
import static co.introtuce.mediapipesegmentationgradle.helper.LogManager.takeCurrentCpuFreq;

public class SegmenterActivity extends AppCompatActivity {

    private static final Object CONTENT_STATE_VIDEO_RECORDING = 0xf1;
    private static final Object CONTENT_STATE_VIDEO_STOPED = 0xf2;
    private String fileUri;
    private String base_filename;
    private String outputfileUri;
    private TextView cpu,gpu,runtime;

    private static final String TAG = "SegmenterActivity";
    private static final String BINARY_GRAPH_NAME = "hairsegmentationgpu.binarypb";
    private static final String INPUT_VIDEO_STREAM_NAME = "input_video";
    private static final String PREV_RATION_STREAM_NAME = "previos_ratio";
    private static final String OUTPUT_VIDEO_STREAM_NAME = "output_video";
    private static final CameraHelper.CameraFacing CAMERA_FACING = CameraHelper.CameraFacing.BACK;
    // Flips the camera-preview frames vertically before sending them into FrameProcessor to be
    // processed in a MediaPipe graph, and flips the processed frames back when they are displayed.
    // This is needed because OpenGL represents images assuming the image origin is at the bottom-left
    // corner, whereas MediaPipe in general assumes the image origin is at top-left.
    private static final boolean FLIP_FRAMES_VERTICALLY = true;

    static {
        // Load all native libraries needed by the app.
        System.loadLibrary("mediapipe_jni");
        //System.loadLibrary("opencv_java3");
    }
    // {@link SurfaceTexture} where the camera-preview frames can be accessed.
    //private SurfaceTexture previewFrameTexture;
    MediaPlayer player;
    private SurfaceTexture surfaceTexture;
    // {@link SurfaceView} that displays the camera-preview frames processed by a MediaPipe graph.
    //private SurfaceView previewDisplayView;
    // Creates and manages an {@link EGLContext}.
    private EglManager eglManager;
    // Sends camera-preview frames into a MediaPipe graph for processing, and displays the processed
    // frames onto a {@link Surface}.
    private CustomFrameProcessor processor;
    // Converts the GL_TEXTURE_EXTERNAL_OES texture from Android camera into a regular texture to be
    // consumed by {@link FrameProcessor} and the underlying MediaPipe graph.
    private MyCusExternalTextureConverter converter;
    private AutoFitTextureView videoTexture;
    private FrameLayout recorderSurface;

    private boolean mIsRecording;
    private Object content_state;
    private File mOutputFile;
    private Uri contentUri;



    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_segmenter);
        fileUri = getIntent().getStringExtra("FILE_URI");
        if(fileUri == null){
            finishWithFail();
        }
        base_filename = extractName();
        cpu = findViewById(R.id.cpu);
        gpu = findViewById(R.id.gpu);
        runtime = findViewById(R.id.runtime);
        initMediapipe();
    }

    long oldTime = 0l, cur_time=0l;

    private void initMediapipe(){
        videoTexture = findViewById(R.id.preview);
        recorderSurface = findViewById(R.id.recorderSurface);
        setupPreviewDisplayView(videoTexture);
        //previewDisplayView = new SurfaceView(this);
        //setupPreviewDisplayView();
        // Initialize asset manager so that MediaPipe native libraries can access the app assets, e.g.,
        // binary graphs.
        AndroidAssetUtil.initializeNativeAssetManager(this);

        eglManager = new EglManager(null);
        processor =
                new CustomFrameProcessor(
                        this,
                        eglManager.getNativeContext(),
                        BINARY_GRAPH_NAME,
                        INPUT_VIDEO_STREAM_NAME,
                        OUTPUT_VIDEO_STREAM_NAME,
                        PREV_RATION_STREAM_NAME,true);
        processor.getVideoSurfaceOutput().setFlipY(FLIP_FRAMES_VERTICALLY);
        processor.getGraph().addPacketCallback(OUTPUT_VIDEO_STREAM_NAME, new PacketCallback() {
            @Override
            public void process(Packet packet) {
                cur_time = System.currentTimeMillis();
                total_runtime = (total_runtime+(cur_time-oldTime))/2;
                if(stringBuilder != null){
                    stringBuilder.append("\nRuntime : "+(cur_time-oldTime));
                }
                runOnUiThread(new Runnable() {
                    @Override
                    public void run() {
                        runtime.setText("Runtime: "+(cur_time-oldTime));
                        oldTime = cur_time;
                    }
                });
            }
        });
        initRecorder();
    }

    private void finishWithFail(){
        setResult(RESULT_FIRST_USER);
        finish();
    }
    private void finishWithSuccess(){
        recordingSetup();
        saveFIle();
        Intent intent = new Intent();
        if(contentUri!=null){
            intent.putExtra("OUTPUT_PATH",contentUri.toString());
        }

        setResult(RESULT_OK,intent);
        finish();
    }
    private int mWidth=0,mHeight=0;
    private void setupPreviewDisplayView(AutoFitTextureView textureView){
        textureView.setVisibility(View.GONE);
        textureView.setSurfaceTextureListener(new TextureView.SurfaceTextureListener() {
            @Override
            public void onSurfaceTextureAvailable(SurfaceTexture surface, int width, int height) {
                Log.d(TAG,"onSurfaceTextureAvailable(SurfaceTexture surface, int width, int height)");
                Surface surface1 = new Surface(surface);
                mHeight=height;
                mWidth=width;
                processor.getVideoSurfaceOutput().setSurface(surface1);
                converter.setSurfaceTextureAndAttachToGLContext(surfaceTexture,width,height);
                //converter.setbGSurfaceTextureAndAttachToGLContext(surfaceTexture,width,height);
                //converter.setbGSurfaceTextureAndAttachToGLContext(previewFrameTexture,width,height);
                //converter.setSurfaceTextureAndAttachToGLContext(surfaceTexture,width,height);
                //initRecorder();
            }

            @Override
            public void onSurfaceTextureSizeChanged(SurfaceTexture surface, int width, int height) {
                Log.d(TAG,"onSurfaceTextureSizeChanged(SurfaceTexture surface, int width, int height)");
            }

            @Override
            public boolean onSurfaceTextureDestroyed(SurfaceTexture surface) {
                Log.d(TAG,"oonSurfaceTextureDestroyed(SurfaceTexture surface)");
                processor.getVideoSurfaceOutput().setSurface(null);
                return false;
            }

            @Override
            public void onSurfaceTextureUpdated(SurfaceTexture surface) {
                //Log.d(TAG,"onSurfaceTextureUpdated(SurfaceTexture surface)");

            }
        });
    }

    //Recording Helper Properties
    private RecordableSurfaceView mGLView;
    MyGLSurfaceView mglView;


    private String RECORD_TAG="RECORD_TAG";
    private void mediaPlay(){
        surfaceTexture = new MySurfaceTexture(42);
        Uri filUri = Uri.fromFile(new File(this.fileUri));
        player = MediaPlayer.create(this,filUri);
        player.setSurface(new Surface(surfaceTexture));
        player.setOnCompletionListener(new MediaPlayer.OnCompletionListener() {
            @Override
            public void onCompletion(MediaPlayer mp) {
                finishWithSuccess();
            }
        });
        player.start();

    }
    private void play(){
        try{
            mediaPlay();
        }catch (Exception e){
            Log.d("FFF",e.toString());
        }
    }

    @Override
    protected void onResume() {
        super.onResume();
        try{
            recordingSetup();
            play();
            startSegmentation();
            captureLogs();
        }catch (Exception e){
            Log.d(TAG,e.toString());
        }

        //newCpuMeter();

    }
    private void startSegmentation(){
        converter = new MyCusExternalTextureConverter(eglManager.getContext());
        converter.setPrevRation(new SessionManager(this).getPrevration());
        converter.setFlipY(FLIP_FRAMES_VERTICALLY);
        converter.setConsumer(processor);
        videoTexture.setVisibility(View.VISIBLE);

    }


    @Override
    protected void onPause() {
        super.onPause();
        stopPlayer();
        converter.close();
        mglView.pause();
        mglView.pauseRender();
        captureLog = false;
    }

    private void stopPlayer(){
        try{
            player.stop();
        }catch (Exception e){
            Log.d("FFF",e.toString());
        }
    }
    public static final String NEW_REC_TAG="MY_NEW_CHANGING";
    //todo: Recording implimentation
    private void recordingSetup(){
            try{
                if(!mIsRecording) {
                    startRecording();
                    Log.d(NEW_REC_TAG,"Starting rec");
                    //mIsRecording = true;
                }
                else if(mIsRecording){
                    //mIsRecording=false;
                    Log.d(NEW_REC_TAG,"Stoping rec");
                    stopRecording();
                }
            }catch (Exception e){
                e.printStackTrace();
                Log.d(NEW_REC_TAG,""+e.toString());
                Log.d(TAG,e.toString());
            }
    }

    public void initRecorder(){
        Log.d(RECORD_TAG,"Initializing");
        if(recorderSurface==null){
            return;
        }
        mglView = new MyGLSurfaceView(this);
        mglView.setSourceTexture(videoTexture);
        mGLView = mglView;
        mGLView.setZOrderMediaOverlay(true);
        recorderSurface.addView(mGLView);
        Log.d(RECORD_TAG,"Initialization done");
    }
    private void startRecording(){
        Log.d(NEW_REC_TAG,"Call starting ");
        mglView.resumeRenderer();
        mglView.resume();
        Log.d(NEW_REC_TAG,"Recording started");
        try {
            mOutputFile = createVideoOutputFile();
            android.graphics.Point size = new android.graphics.Point();
            mGLView.initRecorder(mOutputFile, size.x, size.y, null, null);
        } catch (IOException ioex) {
            Log.e(NEW_REC_TAG, "Couldn't re-init recording", ioex);
        }
        Log.e(NEW_REC_TAG, "Starting now");
        startRec();
    }
    private void stopRecording(){
        mglView.pauseRender();
        mglView.pause();
        Log.d(RECORD_TAG,"Recording stopped");
        startRec();
    }
    public void startRec(){
        Log.d(RECORD_TAG,"TRIGGER_");
        if (mIsRecording) {
            content_state=CONTENT_STATE_VIDEO_STOPED;
            mGLView.stopRecording();
            //fine.setVisibility(View.GONE);
            //onVideoStopped();
            contentUri = FileProvider.getUriForFile(this,
                    "co.introtuce.mediapipesegmentationgradle.fileprovider", mOutputFile);
            //share(contentUri);
            onCompleteRecording(contentUri);
            mIsRecording = false;
            mIsRecording = false;
            mOutputFile = createVideoOutputFile();
            try {
                int screenWidth = mGLView.getWidth();
                int screenHeight = mGLView.getHeight();
                mGLView.initRecorder(mOutputFile, (int) screenWidth, (int) screenHeight, null,
                        null);
            } catch (IOException ioex) {
                Log.e(RECORD_TAG, "Couldn't re-init recording", ioex);
            }
            //  item.setTitle("Record");
            // start.setText("Start");
        } else {
            mIsRecording = true;
            content_state=CONTENT_STATE_VIDEO_RECORDING;
            mGLView.startRecording();
            Log.v(RECORD_TAG, "Recording Started");
        }
    }
    private void onCompleteRecording(Uri contentUri){



    }

    private File createVideoOutputFile() {

        File tempFile = null;
        try {
            File dirCheck = new File(
                    this.getFilesDir().getCanonicalPath() + "/" + "captures");

            if (!dirCheck.exists()) {
                dirCheck.mkdirs();
            }

            String filename = new Date().getTime() + "";
            tempFile = new File(
                    this.getFilesDir().getCanonicalPath() + "/" + "captures" + "/"
                            + filename + ".mp4");
        } catch (IOException ioex) {
            Log.e(TAG, "Couldn't create output file", ioex);
        }


        return tempFile;

    }

    private void saveFIle(){
        if(!MyPermissionHelper.readWritePermissionsGranted(this)){
            requestPermission();
            return;
        }
        if(contentUri!=null) {
            SaveLocal.saveVideo(getApplicationContext(), contentUri);
            String path = SaveLocal.copyFileFromUri(getApplicationContext(), contentUri,base_filename);
            saveLog(base_filename+path);
            if (path.equals("")) {
                Toast.makeText(getApplicationContext(), "Something went wrng..", Toast.LENGTH_LONG).show();
                return;
            }
            else{
                Toast.makeText(this,"Output has been saved ", Toast.LENGTH_LONG).show();
            }
        }
    }

    private void requestPermission(){
        MyPermissionHelper.checkAndRequestreadWritePermissions(this);
    }
    @Override
    public void onRequestPermissionsResult(
            int requestCode, String[] permissions, int[] grantResults) {
        super.onRequestPermissionsResult(requestCode, permissions, grantResults);
        MyPermissionHelper.onRequestPermissionsResult(requestCode, permissions, grantResults);
        if(MyPermissionHelper.readWritePermissionsGranted(this)){
            saveFIle();
        }
        else{
            requestPermission();
        }
    }

    private String extractName(){
       String str[] =  fileUri.split("/");
       String fileName = str[str.length-1];
       fileName = fileName.replace('.','_');
       Log.d("NAME_EXT",""+fileName);
       return fileName;
    }

    boolean captureLog = false;
    int total = 0;
    int count = 0;
    long total_runtime=0l,count_runtime = 0l;
    private void captureLogs(){
        if(captureLog){
            return;
        }
        captureLog = true;
        new Thread(new Runnable() {
            @Override
            public void run() {
                stringBuilder = new StringBuilder();
                newCpuMeter();
                
            }
        }).start();
    }

    ProcessBuilder processBuilder;
    String Holder = "";
    String[] DATA = {"/system/bin/cat", "/proc/cpuinfo"};
    InputStream inputStream;
    Process process ;
    byte[] byteArry ;
    private StringBuilder stringBuilder;
    private void newCpuMeter(){
        byteArry = new byte[1024];
        try{
            processBuilder = new ProcessBuilder(DATA);
            process = processBuilder.start();
            inputStream = process.getInputStream();
            while(inputStream.read(byteArry) != -1){
                Holder = Holder + new String(byteArry);
            }
            stringBuilder.append(Holder);
            inputStream.close();
        } catch(IOException ex){
            ex.printStackTrace();
        }
        //cpu.setText(Holder);
    }

    private void saveLog(String fileName){
        try{
            if(stringBuilder!=null){
                stringBuilder.append("\nAVG Runtime : "+total_runtime);
                stringBuilder.append("\n\n GPU INFO \n");
                stringBuilder.append(mglView.getGPUInfo());
                SaveLocal.saveLogFile(fileName,new String(stringBuilder));
            }


        }catch (Exception e){
            Log.d(TAG,e.toString());
        }
    }
}

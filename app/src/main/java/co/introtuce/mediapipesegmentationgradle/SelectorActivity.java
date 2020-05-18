package co.introtuce.mediapipesegmentationgradle;

import androidx.annotation.Nullable;
import androidx.appcompat.app.AppCompatActivity;

import android.content.Intent;
import android.database.Cursor;
import android.net.Uri;
import android.os.Bundle;
import android.provider.DocumentsContract;
import android.provider.MediaStore;
import android.util.Log;
import android.view.View;
import android.widget.Button;
import android.widget.ImageButton;
import android.widget.TextView;

import com.google.mediapipe.components.PermissionHelper;

import java.util.ArrayList;

import co.introtuce.mediapipesegmentationgradle.helper.MediaHelper;
import co.introtuce.mediapipesegmentationgradle.helper.MyPermissionHelper;

public class SelectorActivity extends AppCompatActivity implements View.OnClickListener
{
    public static final String TAG = "SelectorActivity";
    public static final int SELECT_FILE_REQ=0xf3;
    public static final int SEGMENTATION_START_REQ=0xf4;
    public static final int SETTING_START = 0xf9;
    public static final int START_LIVE_CAM=0xf5;
    private ImageButton selectButton, setting_button, live;
    private Button startButton;
    private TextView label;
    private ArrayList<Uri> files = new ArrayList<Uri>();


    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_selector);
        initViews();
    }

    private void initViews(){
        selectButton = findViewById(R.id.select_button);
        startButton = findViewById(R.id.start_button);
        label = findViewById(R.id.label);
        setting_button = findViewById(R.id.setting);
        live = findViewById(R.id.live);
        live.setOnClickListener(this);
        setting_button.setOnClickListener(this);
        selectButton.setOnClickListener(this);
        startButton.setOnClickListener(this);
    }

    private void requestPermission(){
        MyPermissionHelper.checkAndRequestReadPermissions(this);
        MyPermissionHelper.checkAndRequestreadWritePermissions(this);
    }


    @Override
    public void onClick(View v) {

        switch (v.getId()){
            case R.id.start_button:
                cur_index = 0;
                startSegmentation();

                break;
            case R.id.select_button:
                selectFiles();
                break;
            case R.id.setting:
                startSetting();
                break;
            case R.id.live:
                startLiveCam();
                break;
        }
    }
    private void selectFiles(){
        selectImages();
    }


    public void selectImages() {
        if(MyPermissionHelper.readPermissionsGranted(this)){
            files.clear();
            Intent intent = new Intent(Intent.ACTION_GET_CONTENT);
            intent.putExtra(Intent.EXTRA_ALLOW_MULTIPLE, true);
            intent.setType("video/*");
            startActivityForResult(intent, SELECT_FILE_REQ);
        }
        else{
            requestPermission();
        }

    }


    @Override
    protected void onActivityResult(int requestCode, int resultCode, @Nullable Intent data) {
        super.onActivityResult(requestCode, resultCode, data);
        if(resultCode == RESULT_OK){
            if(requestCode==SELECT_FILE_REQ){
                if(data!=null){
                    if(data.getClipData()!=null){
                        for(int i=0;i<data.getClipData().getItemCount();i++){
                            Uri uri = data.getClipData().getItemAt(i).getUri();
                            files.add(uri);
                        }
                    }
                    else{
                        Uri uri = data.getData();
                        files.add(uri);
                    }
                    onFilesSelected();
                }
            }
            else if(requestCode == SEGMENTATION_START_REQ){
                Log.d(TAG,"Path is : "+data.getStringExtra("OUTPUT_PATH"));
                cur_index = cur_index+1;
                startSegmentation();
            }
        }

    }

    private void onFilesSelected(){
        if(files.size()!=0){
            Log.d(TAG,"FILE_PATH: "+ MediaHelper.getPath(this,files.get(0)));
            label.setText(files.size()+" files selected.");
        }
        else {
            label.setText("Select files");
        }

    }

    int cur_index=0;
    private void startSegmentation(){
        try{
            if(cur_index<files.size()){
                startButton.setVisibility(View.GONE);
                Intent intent = new Intent(this,SegmenterActivity.class);
                intent.putExtra("FILE_URI",MediaHelper.getPath(this,files.get(cur_index)));
                startActivityForResult(intent,SEGMENTATION_START_REQ);
            }
            else{
                startButton.setVisibility(View.VISIBLE);
            }
        }catch (Exception e){
            Log.d(TAG,e.toString());
        }

    }


    @Override
    public void onRequestPermissionsResult(
            int requestCode, String[] permissions, int[] grantResults) {
        super.onRequestPermissionsResult(requestCode, permissions, grantResults);
        MyPermissionHelper.onRequestPermissionsResult(requestCode, permissions, grantResults);
    }

    private void startSetting(){
        Intent intent  = new Intent(this, SettingActivity.class);
        startActivityForResult(intent, SETTING_START);
    }
    private void startLiveCam(){
        Intent intent = new Intent(this, MainActivity.class);
        startActivityForResult(intent, START_LIVE_CAM);
    }

}

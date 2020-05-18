package co.introtuce.mediapipesegmentationgradle;

import androidx.appcompat.app.AppCompatActivity;

import android.os.Bundle;
import android.view.View;
import android.widget.Button;
import android.widget.EditText;
import android.widget.Toast;

import co.introtuce.mediapipesegmentationgradle.helper.SessionManager;

public class SettingActivity extends AppCompatActivity implements View.OnClickListener {

    SessionManager sessionManager;
    private EditText editText;
    private Button save;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_setting);
        editText = findViewById(R.id.prev_r);
        save = findViewById(R.id.save);
        save.setOnClickListener(this);
        sessionManager = new SessionManager(this);
        editText.setText(sessionManager.getPrevration()+"");

    }


    @Override
    public void onClick(View v) {
        try {
            float val = Float.parseFloat(editText.getText().toString());
            sessionManager.savePrevRation(val);
            setResult(RESULT_OK);
            finish();
        }catch (Exception e){
            Toast.makeText(this,"Please give float value only ", Toast.LENGTH_LONG).show();
        }
    }
}

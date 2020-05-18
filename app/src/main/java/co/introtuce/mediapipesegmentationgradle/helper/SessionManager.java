package co.introtuce.mediapipesegmentationgradle.helper;
import android.content.Context;
import android.content.SharedPreferences;
import android.content.SharedPreferences.Editor;

public class SessionManager {

    SharedPreferences pref;

    // Editor for Shared preferences
    Editor editor;

    // Context
    Context _context;
    // Shared pref mode
    int PRIVATE_MODE = 0;

    // Sharedpref file name
    private static final String PREF_NAME = "AndroidHivePref";

    public static final String KEY_PREVRATION = "PREV_RATIO";

    public SessionManager(Context context){
        final  int c;
        this._context = context;
        pref = _context.getSharedPreferences(PREF_NAME, PRIVATE_MODE);
        editor = pref.edit();
    }

    public void savePrevRation(float ration){
        editor.putFloat(KEY_PREVRATION,ration);
        editor.commit();
    }


    public void saveFCMToken(String token){
        editor.putString("FCM_TOKEN",token);
        editor.commit();
    }
    /**
     * Check login method wil check user login status
     * If false it will redirect user to login page
     * Else won't do anything
     * */


    public float getPrevration(){
        return  pref.getFloat(KEY_PREVRATION,0.9f);
    }



}

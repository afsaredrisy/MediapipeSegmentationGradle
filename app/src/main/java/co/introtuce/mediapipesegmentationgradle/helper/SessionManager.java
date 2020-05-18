package com.nex2me.introtuce.version10.Support;
import android.content.Context;
import android.content.SharedPreferences;
import android.content.SharedPreferences.Editor;
public class SessionManager {

    final int x=5;
    byte a;

    SharedPreferences pref;

    // Editor for Shared preferences
    Editor editor;

    // Context
    Context _context;
    // Shared pref mode
    int PRIVATE_MODE = 0;

    // Sharedpref file name
    private static final String PREF_NAME = "AndroidHivePref";

    // All Shared Preferences Keys
    private static final String IS_LOGIN = "IsLoggedIn";
    private static final String PREV_ID = "pre_id";

    // User name (make variable public to access from outside)
    public static final String KEY_PHONE = "phone";
    public static final String KEY_STATUS = "status";
    public static final String KEY_PUSHY = "pushy";

    public SessionManager(Context context){
        final  int c;
        this._context = context;
        pref = _context.getSharedPreferences(PREF_NAME, PRIVATE_MODE);
        editor = pref.edit();
    }
    /**
     * Create login session
     * */
    public void savePhone(String phone){
        editor.putString(KEY_PHONE, phone);
        editor.commit();
    }
    // 1  LoginUser , 0 Login But Not Varify
    public void saveStatus(String status){
        editor.putString(KEY_STATUS, status);
        editor.commit();
    }

    public void savePushy(String deviceToken){
        editor.putString(KEY_PUSHY,deviceToken);
        editor.commit();
    }

    public void createLoginSession(){
        // Storing login value as TRUE
        editor.putBoolean(IS_LOGIN, true);

        // Storing phone in pref


        // commit changes
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
    public boolean isLoggedIn(){
        return pref.getBoolean(IS_LOGIN, false);
    }
    public void logoutUser(){
        // Clearing all data from Shared Preferences
        savePhone("");
        saveDescription("");
        savePrevEvent("");
        saveGender("");
        saveMyBirthday("");
        saveMyName("");
        saveStatus("");
        saveThumbnail("");
       // editor.clear();
       // editor.commit();

        // After logout redirect user to Loing Activity

    }
    public String getPhone(){
        return pref.getString(KEY_PHONE,null);
    }

    public String getStatus(){
        return pref.getString(KEY_STATUS,null);
    }
    public String getFCMToken(){
        return pref.getString("FCM_TOKEN",null);
    }
    public String getPushy(){
        return  pref.getString(KEY_PUSHY,null);
    }
    public void saveMyName(String name){
        editor.putString("MY_NAME",name);
        editor.commit();
    }
    public void saveThumbnail(String thumnail){
        editor.putString("MY_THUMNAIL",thumnail);
        editor.commit();
    }

    public String getMyName(){
        return pref.getString("MY_NAME",null);
    }
    public String getThumbnail(){
        return pref.getString("MY_THUMNAIL",null);
    }
    public void saveGender(String name){
        editor.putString("MY_GENDER",name);
        editor.commit();
    }
    public void saveDescription(String name){
        editor.putString("MY_DESC",name);
        editor.commit();
    }
    public void saveMyBirthday(String name){
        editor.putString("MY_DOB",name);
        editor.commit();
    }
    public void savePrevEvent(String name){
        editor.putString(PREV_ID,name);
        editor.commit();
    }


    private void logout(){
        editor.putBoolean(IS_LOGIN, false);

        // Storing phone in pref


        // commit changes
        editor.commit();
    }
    public String getDescription(){
        return pref.getString("MY_DESC",null);
    }
    public String getMyBirthday(){
        return pref.getString("MY_DOB",null);
    }
    public String getGender(){
        return pref.getString("MY_GENDER",null);
    }
    public String getPrevId(){
        return pref.getString(PREV_ID,null);
    }

}

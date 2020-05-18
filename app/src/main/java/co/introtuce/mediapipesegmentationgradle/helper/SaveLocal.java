package co.introtuce.mediapipesegmentationgradle.helper;

import android.content.ContentResolver;
import android.content.Context;
import android.graphics.Bitmap;
import android.net.Uri;
import android.os.Environment;
import android.util.Log;
import android.widget.Toast;

import java.io.BufferedInputStream;
import java.io.BufferedOutputStream;
import java.io.File;
import java.io.FileInputStream;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;
import java.sql.Time;
import java.time.LocalDateTime;
import java.util.Calendar;
import java.util.Date;
import java.util.Random;

public class SaveLocal {

    private static void prepDirectory(){

        String root= Environment.getDataDirectory().getAbsolutePath().toString()+"/nex2me/media/videos";
        File rootDir=new File(root);
        if(!rootDir.exists()){
            rootDir.mkdir();
        }
        Log.d("SAVE_PHONE","Directory Started");
    }

    private static void prepRootDirectory(){
        String root= Environment.getDataDirectory().getAbsolutePath().toString()+"/nex2me/media";
        File rootDir=new File(root);
        if(!rootDir.exists()){
            rootDir.mkdir();
        }
    }


    public static void saveVideo (Context context, Uri uri){
        File file = new File(Environment.getExternalStorageDirectory(),"/nex2me/media");
        String root = Environment.getExternalStorageDirectory().toString()+"/nex2me/media";

        File myDir = new File(root);
        if(!myDir.exists())
            myDir.mkdirs();
        Log.d("SAVE_PHONE",""+myDir.getAbsolutePath());
        if(!file.exists()){
            Log.d("SAVE_PHONE","file not exist");
            file.mkdir();
            Log.d("SAVE_PHONE","created");
        }
        Log.d("SAVE_PHONE",""+file.getAbsolutePath());
        Log.d("SAVE_PHONE","Saving wth contex");
        savefile(uri,myDir.getAbsolutePath());

        Log.d("SAVE_PHONE","Saving done context");
    }

    public static void saveVideo(Uri source) {

        prepDirectory();
        //savefile(source);
    }



    private static void savefile(Uri sourceuri,String rootpath)
    {
        String sourceFilename= sourceuri.getPath();
        Date dateTime = Calendar.getInstance().getTime();
        String timestamp = dateTime.toLocaleString();
        String destinationFilename =rootpath+""+File.separatorChar+timestamp+".mp4";
        File file=new File(rootpath,"myg.mp4");


        //    File file = new File(Environment.getExternalStorageDirectory(),"nex2me/media");
        //File gpxfile = new File(file, "testFile");
        //android.os.Environment.getExternalStorageDirectory().getPath()+File.separatorChar+"abc.mp4";

        BufferedInputStream bis = null;
        BufferedOutputStream bos = null;

        try {
            bis = new BufferedInputStream(new FileInputStream(sourceFilename));
            bos = new BufferedOutputStream(new FileOutputStream(file, false));
            byte[] buf = new byte[1024];
            bis.read(buf);
            do {
                bos.write(buf);
            } while(bis.read(buf) != -1);
        } catch (IOException e) {
            e.printStackTrace();
        } finally {
            try {
                if (bis != null) bis.close();
                if (bos != null) bos.close();
                Log.d("SAVE_PHONE","file created "+file.getAbsolutePath());
                Log.d("SAVE_PHONE","file existing issue "+file.exists());
            } catch (Exception e) {
                Log.d("SAVE_PHONE",""+e.toString());
                e.printStackTrace();
            }
        }
    }

    public static void SaveImage(Bitmap finalBitmap) {

        String root = Environment.getExternalStorageDirectory().toString();
        File myDir = new File(root + "/saved_images");
        if (!myDir.exists()) {
            myDir.mkdirs();
        }

        Random generator = new Random();
        int n = 10000;
        n = generator.nextInt(n);
        String fname = "Image-"+ n +".jpg";
        File file = new File (myDir, fname);
        if (file.exists ())
            file.delete ();
        try {
            FileOutputStream out = new FileOutputStream(file);
            finalBitmap.compress(Bitmap.CompressFormat.JPEG, 90, out);
            out.flush();
            out.close();

        } catch (Exception e) {
            e.printStackTrace();
        }
    }

    public static void SaveVideo(Uri uri) {

        String root = Environment.getExternalStorageDirectory().toString();
        File myDir = new File(root + "/saved_images");
        if (!myDir.exists()) {
            myDir.mkdirs();
        }

        Random generator = new Random();
        int n = 10000;
        n = generator.nextInt(n);
        String fname = "Image-"+ n +".mp4";
        File file = new File (myDir, fname);
        if (file.exists ())
            file.delete ();
        try {
            FileOutputStream out = new FileOutputStream(file);
            BufferedInputStream bis = new BufferedInputStream(new FileInputStream(uri.getPath()));
            //finalBitmap.compress(Bitmap.CompressFormat.JPEG, 90, out);

            BufferedOutputStream bos = new BufferedOutputStream(new FileOutputStream(file, false));
            byte[] buf = new byte[1024];
            bis.read(buf);
            do {
                bos.write(buf);
            } while(bis.read(buf) != -1);
            out.flush();
            out.close();

        } catch (Exception e) {
            e.printStackTrace();
        }
    }

    public static String copyFileFromUri(Context context, Uri fileUri)
    {
        InputStream inputStream = null;
        OutputStream outputStream = null;

        try
        {
            ContentResolver content = context.getContentResolver();

            inputStream = content.openInputStream(fileUri);
            Log.d("SAVE_PHONE","Get Content resolver");
            File root = Environment.getExternalStorageDirectory();
            if(root == null){
                Log.d("SAVE_PHONE", "Failed to get root");
            }

            // create a directory
            File saveDirectory = new File(Environment.getExternalStorageDirectory()+File.separator+ "nex2me/media/video" +File.separator);
            // create direcotory if it doesn't exists
            if(!saveDirectory.exists())
                saveDirectory.mkdirs();
            String fileName=fileUri.getPath().substring(fileUri.getPath().lastIndexOf("/"+1));
            if(fileName.indexOf(".")>0){
                fileName=fileName.substring(0,fileName.indexOf("."));
            }
            outputStream = new FileOutputStream( saveDirectory + fileName+""+"nex2me.mp4"); // filename.png, .mp3, .mp4 ...
            if(outputStream != null){
                Log.e( "SAVE_PHONE", "Output Stream Opened successfully");
            }

            byte[] buffer = new byte[1000];
            int bytesRead = 0;
            while ( ( bytesRead = inputStream.read( buffer, 0, buffer.length ) ) >= 0 )
            {
                outputStream.write( buffer, 0, buffer.length );
            }
            return saveDirectory + fileName+""+"nex2me.mp4";
        } catch ( Exception e ){
            Log.e( "SAVE_PHONE", "Exception occurred " + e.getMessage());
        } finally{

        }
        return "";
    }
    public static String copyFileFromUri(Context context, Uri fileUri, String baseFileName)
    {
        InputStream inputStream = null;
        OutputStream outputStream = null;

        try
        {
            ContentResolver content = context.getContentResolver();

            inputStream = content.openInputStream(fileUri);
            Log.d("SAVE_PHONE","Get Content resolver");
            File root = Environment.getExternalStorageDirectory();
            if(root == null){
                Log.d("SAVE_PHONE", "Failed to get root");
            }

            // create a directory
            File saveDirectory = new File(Environment.getExternalStorageDirectory()+File.separator+ "nex2me/media/video" +File.separator);
            // create direcotory if it doesn't exists
            if(!saveDirectory.exists())
                saveDirectory.mkdirs();
            long time = System.currentTimeMillis();
            String fileName=baseFileName+"_"+time;//fileUri.getPath().substring(fileUri.getPath().lastIndexOf("/"+1));
            if(fileName.indexOf(".")>0){
                fileName=fileName.substring(0,fileName.indexOf("."));
            }
            outputStream = new FileOutputStream( saveDirectory + fileName+""+"nex2me.mp4"); // filename.png, .mp3, .mp4 ...
            if(outputStream != null){
                Log.e( "SAVE_PHONE", "Output Stream Opened successfully");
            }

            byte[] buffer = new byte[1000];
            int bytesRead = 0;
            while ( ( bytesRead = inputStream.read( buffer, 0, buffer.length ) ) >= 0 )
            {
                outputStream.write( buffer, 0, buffer.length );
            }
            return ""+time;
        } catch ( Exception e ){
            Log.e( "SAVE_PHONE", "Exception occurred " + e.getMessage());
        } finally{

        }
        return "";
    }




    public static boolean copyImageFileFromUri(Context context, Uri fileUri)
    {
        InputStream inputStream = null;
        OutputStream outputStream = null;

        try
        {
            ContentResolver content = context.getContentResolver();
            inputStream = content.openInputStream(fileUri);

            File root = Environment.getExternalStorageDirectory();
            if(root == null){
                Log.d("SAVE_PHONE", "Failed to get root");
            }

            // create a directory
            File saveDirectory = new File(Environment.getExternalStorageDirectory()+File.separator+ "nex2me/media/images" +File.separator);
            // create direcotory if it doesn't exists
            if(!saveDirectory.exists())
                saveDirectory.mkdirs();
            String fileName=fileUri.getPath().substring(fileUri.getPath().lastIndexOf("/"+1));
            if(fileName.indexOf(".")>0){
                fileName=fileName.substring(0,fileName.indexOf("."));
            }
            outputStream = new FileOutputStream( saveDirectory + fileName+""+"nex2me.png"); // filename.png, .mp3, .mp4 ...
            if(outputStream != null){
                Log.e( "SAVE_PHONE", "Output Stream Opened successfully");
            }

            byte[] buffer = new byte[1000];
            int bytesRead = 0;
            while ( ( bytesRead = inputStream.read( buffer, 0, buffer.length ) ) >= 0 )
            {
                outputStream.write( buffer, 0, buffer.length );
            }
        } catch ( Exception e ){
            Log.e( "SAVE_PHONE", "Exception occurred " + e.getMessage());
        } finally{

        }
        return true;
    }


    public boolean saveFile(String csv_contents, Context context){
        OutputStream outputStream = null;
        try{
            File root = Environment.getExternalStorageDirectory();
            if(root == null){
                Log.d("SAVE_PHONE", "Failed to get root");
                return false;
            }

            // create a directory
            File saveDirectory = new File(root,"appName/files/csv" );
            // create direcotory if it doesn't exists

            // create direcotory if it doesn't exists
            if(!saveDirectory.exists()) if ( !saveDirectory.mkdirs()){
                Toast.makeText(context,"sorry could not create directory"+saveDirectory.getAbsolutePath(), Toast.LENGTH_LONG).show();
                return false;
            }

            outputStream = new FileOutputStream( saveDirectory + "myfile.csv"); // filename.png, .mp3, .mp4 ...
            if(outputStream != null){
                Log.e( "SAVE_PHONE", "Output Stream Opened successfully");
            }

            byte[] bytes = csv_contents.getBytes();
            outputStream.write( bytes, 0, bytes.length );
            outputStream.close();
            return true;

        }catch (Exception e){
            Log.d("EXCEPTION_IN",e.toString());
            return false;
        }

    }
   /* public void submit(View v)
    {
        String nline = System.getProperty("line.separator");
        String fname = firstName.getText().toString() + ",";
        String sname = surname.getText().toString() + ",";
        String gender = genderSpin.getSelectedItem().toString() + ",";
        String eaddress = email.getText().toString() + ",";
        String mnum = mobile.getText().toString() + ",";
        String fos = course.getText().toString() + ",";
        String prole = proleSpin.getSelectedItem().toString();
        FileOutputStream file = null;

        if(fname.length() <= 1 || sname.length() <= 1 || eaddress.length() <= 1){
            Toast.makeText(this, "Please enter all mandatory fields", Toast.LENGTH_SHORT).show();
        }

        String csv_contents = nline+""+fname+sname+gender+eaddress+mnum+fos+prole;
        if(saveFile(csv_contents,this)){
            //File has saved
            // DO something
        }
        else{
            //Could not save file
            // DO something
        }
    }*/


    public static String saveLogFile(String fn,String content)
    {
        OutputStream outputStream = null;

        try
        {
            //ContentResolver content = context.getContentResolver();

            //inputStream = content.openInputStream(fileUri);
            Log.d("SAVE_PHONE","Get Content resolver");
            File root = Environment.getExternalStorageDirectory();
            if(root == null){
                Log.d("SAVE_PHONE", "Failed to get root");
            }

            // create a directory
            File saveDirectory = new File(Environment.getExternalStorageDirectory()+File.separator+ "nex2me/log" +File.separator);
            // create direcotory if it doesn't exists
            if(!saveDirectory.exists())
                saveDirectory.mkdirs();
            String fileName=fn;
            if(fileName.indexOf(".")>0){
                fileName=fileName.substring(0,fileName.indexOf("."));
            }
            String rootPath = Environment.getExternalStorageDirectory()+File.separator+ "nex2me/log" +File.separator;
            outputStream = new FileOutputStream( rootPath + fileName+""+"_log.txt"); // filename.png, .mp3, .mp4 ...
            if(outputStream != null){
                Log.e( "SAVE_PHONE", "Output Stream Opened successfully");
            }

            byte[] buffer = content.getBytes();
            outputStream.write(buffer);
            int bytesRead = 0;

            return saveDirectory + fileName+""+"_log.txt";
        } catch ( Exception e ){
            Log.e( "SAVE_PHONE", "Exception occurred " + e.getMessage());
        } finally{

        }
        return "";
    }


}

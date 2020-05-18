package co.introtuce.mediapipesegmentationgradle.helper;

import android.os.Bundle;
import android.widget.TextView;

import java.io.BufferedReader;
import java.io.File;
import java.io.FileFilter;
import java.io.FileInputStream;
import java.io.InputStreamReader;
import java.util.regex.Pattern;
public class LogManager {

    private static int sLastCpuCoreCount = -1;

    private static int readIntegerFile(String filePath) {

        try {
            final BufferedReader reader = new BufferedReader(
                    new InputStreamReader(new FileInputStream(filePath)), 1000);
            final String line = reader.readLine();
            reader.close();

            return Integer.parseInt(line);
        } catch (Exception e) {
            return 0;
        }
    }

    public static int takeCurrentCpuFreq(int coreIndex) {
        return readIntegerFile("/sys/devices/system/cpu/cpu" + coreIndex + "/cpufreq/scaling_cur_freq");
    }

    public static int calcCpuCoreCount() {

        if (sLastCpuCoreCount >= 1) {
            // キャッシュさせる
            return sLastCpuCoreCount;
        }

        try {
            // Get directory containing CPU info
            final File dir = new File("/sys/devices/system/cpu/");
            // Filter to only list the devices we care about
            final File[] files = dir.listFiles(new FileFilter() {

                public boolean accept(File pathname) {
                    //Check if filename is "cpu", followed by a single digit number
                    if (Pattern.matches("cpu[0-9]", pathname.getName())) {
                        return true;
                    }
                    return false;
                }
            });

            // Return the number of cores (virtual CPU devices)
            sLastCpuCoreCount = files.length;

        } catch(Exception e) {
            sLastCpuCoreCount = Runtime.getRuntime().availableProcessors();
        }

        return sLastCpuCoreCount;
    }
}

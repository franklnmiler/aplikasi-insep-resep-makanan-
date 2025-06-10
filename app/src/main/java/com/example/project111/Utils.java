package com.example.project111;

import java.text.SimpleDateFormat;
import java.util.Date;

public class Utils {

    public static String formatTime(long timeInMillis) {
        SimpleDateFormat sdf = new SimpleDateFormat("dd-MM-yyyy HH:mm");
        return sdf.format(new Date(timeInMillis));
    }
}

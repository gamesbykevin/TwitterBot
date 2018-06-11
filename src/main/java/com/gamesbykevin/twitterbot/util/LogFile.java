package com.gamesbykevin.twitterbot.util;

import java.io.File;
import java.io.PrintWriter;

import static com.gamesbykevin.twitterbot.util.Email.getFileDateDesc;

public class LogFile {

    private static String LOG_DIRECTORY;

    public static String getFilenameEmail() {
        return "email.log";
    }

    public static String getFileNameTwitterLog() {
        return "twitter_" + getFileDateDesc() + ".log";
    }

    public static final String FILE_SEPARATOR = System.getProperty("file.separator");

    public static String getFileDirectory() {
        return "config";
    }

    public static String getLogDirectory() {

        if (LOG_DIRECTORY == null)
            LOG_DIRECTORY = "logs-" + getFileDateDesc();

        return LOG_DIRECTORY;
    }

    public static PrintWriter getPrintWriter(final String filename, final String directories) {

        try {

            //create a new directory if we specified one
            if (directories != null) {

                File file = new File(directories);

                //if the directory does not exist, create it
                if (!file.exists())
                    file.mkdirs();

                //object no longer needed
                file = null;
            }

            //create new print writer
            if (directories != null) {
                return new PrintWriter(directories + FILE_SEPARATOR + filename, "UTF-8");
            } else {
                return new PrintWriter(filename, "UTF-8");
            }

        } catch (Exception e) {
            e.printStackTrace();
        }

        return null;
    }
}
package com.gamesbykevin.twitterbot.util;

import com.gamesbykevin.twitterbot.Main;

import java.io.FileInputStream;
import java.io.PrintWriter;
import java.util.Properties;

import static com.gamesbykevin.twitterbot.util.Email.getTextDateDesc;

public class PropertyUtil {

    public static final String PROPERTY_FILE_APP = "./twitterbot.properties";

    public static final String PROPERTY_FILE_TWITTER_4J = "twitter4j.properties";

    private static Properties PROPERTIES;

    public static final long SECONDS_PER_MINUTE = 60L;

    public static final long MILLISECONDS_PER_SECOND = 1000L;

    //property file stuff
    public static long DELAY = 0;
    public static String USERNAME;
    public static Float RATIO_LIMIT;
    public static Float RATIO_RESTART;
    public static Float FOLLOW_RATE;
    public static int FOLLOW_LIMIT;
    public static int UNFOLLOW_LIMIT;
    public static int SEARCH_RESULTS;
    public static String SEARCH_HASHTAG;

    //how many milliseconds are there per minute
    public static final long MILLISECONDS_PER_MINUTE = MILLISECONDS_PER_SECOND * SECONDS_PER_MINUTE;

    /**
     * Are we running this application in IntelliJ? (yes=true, no=false)
     */
    public static final boolean DEBUG = true;

    public static Properties getPropertyFile(String filename) {

        Properties properties = new Properties();

        try {

            if (DEBUG) {
                properties.load(Main.class.getClassLoader().getResourceAsStream(filename));
            } else {
                properties.load(new FileInputStream(filename));
            }

        } catch (Exception e) {

            e.printStackTrace();

            //stop the application if we can't load a property file
            System.exit(10);
        }

        //return our properties object
        return properties;
    }

    private static Properties getProperties() {

        if (PROPERTIES == null)
            PROPERTIES = getPropertyFile(PROPERTY_FILE_APP);

        return PROPERTIES;
    }

    public static void loadProperties() {

        displayMessage("Loading properties: " + PROPERTY_FILE_APP);

        //grab the email address from our config
        Email.EMAIL_NOTIFICATION_ADDRESS = getProperties().getProperty("email.send");

        //our gmail login we need so we have an smtp server to send emails
        Email.GMAIL_SMTP_USERNAME = getProperties().getProperty("email.username");
        Email.GMAIL_SMTP_PASSWORD = getProperties().getProperty("email.password");

        //populate values
        SEARCH_HASHTAG = getProperties().getProperty("search.hashtag");
        SEARCH_RESULTS = Integer.parseInt(getProperties().getProperty("search.results"));
        USERNAME = getProperties().getProperty("user.name");
        DELAY = Long.parseLong(getProperties().getProperty("interval.delay")) * MILLISECONDS_PER_MINUTE;
        RATIO_LIMIT = Float.parseFloat(getProperties().getProperty("follows.ratio.max"));
        RATIO_RESTART = Float.parseFloat(getProperties().getProperty("follows.ratio.restart"));
        FOLLOW_RATE = Float.parseFloat(getProperties().getProperty("follow.back.rate"));
        FOLLOW_LIMIT = Integer.parseInt(getProperties().getProperty("follow.limit"));
        UNFOLLOW_LIMIT = Integer.parseInt(getProperties().getProperty("unfollow.limit"));
    }

    public static synchronized void displayMessage(final String message) {
        displayMessage(message,null);
    }

    public static synchronized void displayMessage(String message, PrintWriter writer) {

        printConsole(message);
        writeFile(message, writer);
    }

    public static synchronized void printConsole(String message) {

        //don't continue if there is nothing
        if (message == null)
            return;

        //print to console
        System.out.println(message);
        System.out.flush();
    }

    public static synchronized void writeFile(String message, PrintWriter writer) {

        //don't continue if there is nothing
        if (message == null)
            return;

        try {

            if (writer != null) {
                writer.println(getTextDateDesc() + ":  " + message);
                writer.flush();
            }

        } catch (Exception e) {
            e.printStackTrace();
        }
    }

    public static synchronized void displayMessage(final Exception e, PrintWriter writer) {
        displayMessage(getErrorMessage(e), writer);
    }

    private static String getErrorMessage(Exception e) {

        String message = "";

        try {

            message += e.getMessage() + "\n\t\t";;

            StackTraceElement[] stack = e.getStackTrace();

            for (int i = 0; i <  stack.length; i++) {
                message = message + stack[i].toString() + "\n\t\t";
            }

        } catch (Exception ex) {
            ex.printStackTrace();
        }

        return message;
    }
}
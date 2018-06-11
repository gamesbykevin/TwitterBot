package com.gamesbykevin.twitterbot;

import com.gamesbykevin.twitterbot.agent.Agent;
import com.gamesbykevin.twitterbot.io.TwitterList;
import com.gamesbykevin.twitterbot.io.TwitterList.Key;
import com.gamesbykevin.twitterbot.util.Email;
import com.gamesbykevin.twitterbot.util.PropertyUtil;

import java.io.PrintWriter;
import java.io.StringWriter;
import java.text.SimpleDateFormat;
import java.util.*;

import static com.gamesbykevin.twitterbot.util.PropertyUtil.*;

public class Main extends Thread {

    //the date format to track the last run time
    private static final String FORMAT = "MM/dd/yyyy HH:mm:ss";

    //date format converter object
    public static final SimpleDateFormat DATE_FORMAT = new SimpleDateFormat(FORMAT, Locale.US);

    //how long do we sleep the thread
    private static final long THREAD_SLEEP = 1000L;

    public static void main(String[] args) {

        try {

            //create new twitter bot
            Main bot = new Main();

            //start the thread
            bot.start();

        } catch (Exception e) {
            e.printStackTrace();
        }
    }

    /**
     * Default constructor
     */
    public Main() {

        try {

            //load properties
            PropertyUtil.loadProperties();

        } catch (Exception e) {
            e.printStackTrace();
        }
    }

    @Override
    public void run() {

        try {

            //last run
            long previous = getLastRun();

            //our twitter agent
            Agent agent = null;

            while (true) {

                //our twitter agent
                agent = null;

                //has enough time elapsed
                if (System.currentTimeMillis() - previous >= DELAY) {

                    //display message
                    displayMessage("Resuming");

                    //create new agent
                    agent = new Agent();

                    //run program
                    agent.execute();

                    //save last run date
                    saveLastRun();

                    //recycle
                    agent.recycle();
                    agent = null;

                    //update the last run time
                    previous = System.currentTimeMillis();
                }

                //how much time has passed since our last run
                long difference = (System.currentTimeMillis() - previous);

                //calculate remaining time
                long minutes = ((DELAY - difference) / 1000)  / 60;
                int seconds = (int)((DELAY - difference) / 1000) % 60;
                displayMessage("Job will run again in " + minutes + " minutes, " + seconds + " seconds");

                //sleep for a short while before we check again
                super.sleep(THREAD_SLEEP);
            }

        } catch (Exception e) {

            e.printStackTrace();

            //convert stack track to a string so we can email it
            StringWriter sw = new StringWriter();
            e.printStackTrace(new PrintWriter(sw));
            sw.toString();

            //send email with error details
            Email.sendEmail("Twitter Error", sw.toString());
        }
    }

    private long getLastRun() throws Exception {

        //last time the robot ran
        Calendar lastRun = Calendar.getInstance();

        //last run file
        TwitterList list = new TwitterList(Key.LastRun);

        //if there is no last run time, assign the current time
        if (list.getValues().isEmpty()) {

            //move current time back so the robot does not appear to have run
            lastRun.add(Calendar.MINUTE, -(int)(DELAY / MILLISECONDS_PER_MINUTE));

            //save the date in our file
            saveLastRun();

        } else {

            //assign the time
            lastRun.setTime(DATE_FORMAT.parse(list.getValues().get(0)));

        }

        //return the last run time
        return lastRun.getTimeInMillis();
    }

    /**
     * Save the current time as the last run date
     */
    public static void saveLastRun() {

        TwitterList list = null;

        try {

            list = new TwitterList(Key.LastRun);

            //remove all existing values
            list.removeValues();

            //add the current calendar item and save
            list.addValue(DATE_FORMAT.format(Calendar.getInstance().getTime()));

            //save the list
            list.save();

        } catch (Exception e) {

            e.printStackTrace();

        } finally {

            if (list != null)
                list.recycle();

            //recycle
            list = null;
        }
    }
}
package com.gamesbykevin.twitterbot.agent;

import com.gamesbykevin.twitterbot.TwitterBotHelper;
import com.gamesbykevin.twitterbot.io.TwitterList;
import com.gamesbykevin.twitterbot.io.TwitterList.Key;
import com.gamesbykevin.twitterbot.util.Email;
import com.gamesbykevin.twitterbot.util.LogFile;
import twitter4j.Twitter;
import twitter4j.TwitterFactory;

import java.io.PrintWriter;
import java.util.ArrayList;
import java.util.HashMap;

import static com.gamesbykevin.twitterbot.agent.AgentHelper.*;
import static com.gamesbykevin.twitterbot.util.LogFile.getFileNameTwitterLog;
import static com.gamesbykevin.twitterbot.util.PropertyUtil.*;

public class Agent {

    /**
     * The current number of people we have followed
     */
    private int follows = 0;

    /**
     * The current number of people we have un-followed
     */
    private int unfollows = 0;

    //hash map of lists
    private HashMap<Key, TwitterList> twitterLists;

    //keep class variable so we can access how many followers, following at any time
    private ArrayList<Long> followers, following;

    //our print writer object
    private PrintWriter writer;

    public Agent() throws Exception {

        //create our file lists for tracking
        this.twitterLists = new HashMap<>();

        //create our lists
        for (Key key : Key.values()) {

            if (key == Key.LastRun)
                continue;

            this.twitterLists.put(key, new TwitterList(key));
        }

        this.writer = LogFile.getPrintWriter(getFileNameTwitterLog(), LogFile.getLogDirectory());
    }

    public PrintWriter getWriter() {
        return this.writer;
    }

    public void execute() throws Exception {

        //summary for email
        String text = "";

        //reset to 0 just in case
        setFollows(0);
        setUnfollows(0);

        //create new factory
        TwitterFactory factory = new TwitterFactory();

        //assign twitter object to that instance
        Twitter twitter = factory.getInstance();

        //create a new access token and assign to our twitter object
        twitter.setOAuthAccessToken(TwitterBotHelper.getAccessToken());

        //get list of followers that I have from twitter
        this.followers = TwitterBotHelper.getTwitterFollowers(twitter, USERNAME);

        //get list of users I am following from twitter
        this.following = TwitterBotHelper.getTwitterFollowing(twitter, USERNAME);

        //if we are above our ratio limit, we will try to un-follow users to lower
        if (getFollowRatio() >= RATIO_LIMIT) {

            displayMessage("Our " + getFollowRatio() + " ratio is > " + RATIO_LIMIT, getWriter());

            //our list of non followers sorted by when we followed them (oldest first)
            ArrayList<Long> nonfollowers = getNonFollowers(this);
            String message = "Total non-followers: " + nonfollowers.size();
            text += message + "\n";
            displayMessage(message, getWriter());
            displayMessage("Trying to unfollow", getWriter());

            //check all of our non followers to lower our ratio
            for (int index = 0; index < nonfollowers.size(); index++) {

                //we can only un-follow so many people
                if (getUnfollows() < UNFOLLOW_LIMIT) {

                    long userId = nonfollowers.get(index);

                    //let's try to un-follow this user
                    if (unfollow(this, twitter, userId, false)) {
                        message = "User " + userId + " un-followed";
                        displayMessage(message, getWriter());
                        text += message + "\n";
                    }

                } else {

                    //else we exit the loop
                    break;
                }
            }

            displayMessage("New ratio is " + getFollowRatio(), getWriter());
        }

        displayMessage("Total followers: " + getFollowers().size(), getWriter());
        displayMessage("Total following: " + getFollowing().size(), getWriter());

        if (getListFollowers().getValues().isEmpty()) {

            //add each from our followers list
            for (long id : getFollowers()) {
                getListFollowers().addValue(String.valueOf(id));
            }

            //finally save the file
            getListFollowers().save();
        }

        //un-follow users
        unfollowUsers(this, twitter);

        //follow users
        followUsers(this, twitter);

        //now update our followers list with the current
        getListFollowers().removeValues();

        //add each value to our file object
        for (long id : getFollowers())
        {
            getListFollowers().addValue(String.valueOf(id));
        }

        //now save the file
        getListFollowers().save();

        //search tweets for new users
        searchTweets(this, twitter);

        String message = "Ratio: " + getFollowRatio();
        text += message + "\n";
        displayMessage(message, getWriter());

        //save the pending files and blocked file, in case any changes were made
        getListPendingFollow().save();
        getListPendingUnfollow().save();
        getListBlocked().save();
        getListIgnore().save();

        //append to our text message
        text = "Followers: " + getFollowers().size() + "\n" +
               "Following: " + getFollowing().size() + "\n\n" +
               "List summary" + "\n" +
               "Ignore       : " + getListIgnore().getValues().size() + "\n" +
               "Blocked      : " + getListBlocked().getValues().size() + "\n" +
               "Pend Follow  : " + getListPendingFollow().getValues().size() + "\n" +
               "Pend Unfollow: " + getListPendingUnfollow().getValues().size() + "\n";

        //send email
        Email.sendEmail("Twitter Update", text);
    }

    /**
     * Current list of people following us on twitter
     * @return
     */
    public ArrayList<Long> getFollowers() {
        return followers;
    }

    /**
     * Current list of people we are following
     * @return
     */
    public ArrayList<Long> getFollowing() {
        return following;
    }

    /**
     * Get our twitter follow ratio.<br>
     * @return The percent of people we follow compared to the number of followers we have
     */
    public double getFollowRatio()
    {
        return ((double)this.following.size() / (double)this.followers.size());
    }

    public int getFollows() {
        return this.follows;
    }

    public void setFollows(int follows) {
        this.follows = follows;
    }

    public int getUnfollows() {
        return this.unfollows;
    }

    public void setUnfollows(int unfollows) {
        this.unfollows = unfollows;
    }

    protected TwitterList getListFollowers() {
        return this.twitterLists.get(Key.Followers);
    }

    protected TwitterList getListPendingFollow() {
        return this.twitterLists.get(Key.PendingFollow);
    }

    protected TwitterList getListPendingUnfollow() {
        return this.twitterLists.get(Key.PendingUnfollow);
    }

    protected TwitterList getListBlocked() {
        return this.twitterLists.get(Key.Blocked);
    }

    protected TwitterList getListIgnore() {
        return this.twitterLists.get(Key.Ignore);
    }

    public void recycle() {

        if (this.twitterLists != null) {

            for (Key key : Key.values()) {

                if (this.twitterLists.get(key) != null) {
                    this.twitterLists.get(key).recycle();
                    this.twitterLists.put(key, null);
                }
            }

            this.twitterLists.clear();
            this.twitterLists = null;
        }

        if (this.followers != null) {
            this.followers.clear();
            this.followers = null;
        }

        if (this.following != null) {
            this.following.clear();
            this.following = null;
        }

        if (this.writer != null) {
            this.writer.close();
            this.writer = null;
        }
    }
}
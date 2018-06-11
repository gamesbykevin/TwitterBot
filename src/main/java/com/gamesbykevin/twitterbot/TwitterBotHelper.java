package com.gamesbykevin.twitterbot;

import twitter4j.*;
import twitter4j.auth.AccessToken;

import java.util.ArrayList;
import java.util.List;
import java.util.Properties;

import static com.gamesbykevin.twitterbot.util.PropertyUtil.PROPERTY_FILE_TWITTER_4J;
import static com.gamesbykevin.twitterbot.util.PropertyUtil.getPropertyFile;

public class TwitterBotHelper {

    /**
     * Get twitter results containing the specified keyword
     * @param twitter Object used to retrieve twitter info
     * @param keyword The keyword we want to search for
     * @param results The desired number of results to return (max 100,min 1)
     * @return List of tweets containing the keyword search
     */
    public static final List<Status> getTwitterSearch(Twitter twitter, String keyword, int results) throws Exception
    {
        //create our query
        Query query = new Query(keyword);

        if (results > 100)
            results = 100;
        if (results < 1)
            results = 1;

        //set the max amount of results to result (max 100)
        query.setCount(results);

        //now perform the tweet search containing that keyword
        QueryResult result = twitter.search(query);

        //now return our results
        return result.getTweets();
    }

    /**
     * Get list of people that are following me on twitter
     * @param twitter Object used to retrieve twitter info
     * @param twitterName The twitterName of the user's followers we want
     * @return list of user ids that are following me
     */
    public static ArrayList<Long> getTwitterFollowers(Twitter twitter, String twitterName) {

        //create our result list
        ArrayList<Long> result = new ArrayList<>();

        long cursor = -1;

        //continue until our cursor is done
        while(cursor != 0) {

            //list of the followers id's
            long[] userIds = null;

            try {

                //get the list of followers id's
                IDs temp = twitter.getFollowersIDs(twitterName, cursor);

                //check the cursor to know if we are at the end of the list
                cursor = temp.getNextCursor();

                //populate the array with all the id's
                userIds = temp.getIDs();
            } catch (Exception e) {
                e.printStackTrace();
            }

            //make sure our array is not null before adding
            if(userIds != null) {

                //add each one to the list
                for (long id : userIds) {
                    result.add(id);
                }
            }
        }

        //return our result
        return result;
    }

    /**
     * Get list of people I am following on twitter
     * @param twitter Object used to retrieve twitter info
     * @param twitterName The twitterName of the user's followers we want
     * @return list of user ids that I am following
     */
    public static ArrayList<Long> getTwitterFollowing(Twitter twitter, String twitterName) {

        //create our result list
        ArrayList<Long> result = new ArrayList<>();

        long cursor = -1;

        //continue until our cursor is done
        while(cursor != 0) {

            //list of following id's
            long[] userIds = null;

            try {

                //get the list of following id's
                IDs temp = twitter.getFriendsIDs(twitterName, cursor);

                //check the cursor to know if we are at the end of the list
                cursor = temp.getNextCursor();

                //populate the array with all the id's
                userIds = temp.getIDs();
            } catch (Exception e) {
                e.printStackTrace();
            }

            //make sure our array is not null before adding
            if(userIds != null) {

                //add each one to the list
                for (long id : userIds) {
                    result.add(id);
                }
            }
        }

        //return our result
        return result;
    }

    public static AccessToken getAccessToken() {

        //our properties object
        Properties properties = getPropertyFile(PROPERTY_FILE_TWITTER_4J);

        //return new access token object
        return (new AccessToken(
            properties.getProperty("oauth.accessToken"),
            properties.getProperty("oauth.accessTokenSecret"))
        );
    }
}
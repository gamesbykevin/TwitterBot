package com.gamesbykevin.twitterbot.agent;

import com.gamesbykevin.twitterbot.TwitterBotHelper;
import com.gamesbykevin.twitterbot.io.TwitterList;
import twitter4j.Status;
import twitter4j.Twitter;
import twitter4j.TwitterException;

import java.util.ArrayList;
import java.util.List;

import static com.gamesbykevin.twitterbot.util.PropertyUtil.*;

public class AgentHelper {

    /**
     * Get the list of non followers
     * @return List of users we are following but not following us
     */
    protected static ArrayList<Long> getNonFollowers(Agent agent) {

        //our list of non followers sorted by when we followed them (oldest first)
        ArrayList<Long> nonfollowers = new ArrayList<>();

        //check all following
        for (int i = agent.getFollowing().size() - 1; i >= 0; i--) {

            long userId = agent.getFollowing().get(i);

            //if the user we are following is not following us, they are a non-follower
            if (!TwitterList.hasValue(agent.getFollowers(), userId))
                nonfollowers.add(userId);
        }

        //return our result
        return nonfollowers;
    }

    /**
     * Get new followers
     * @return New users following me on twitter
     */
    protected static ArrayList<Long> getNewFollowers(Agent agent) {

        //list of users for us to follow
        ArrayList<Long> follow = new ArrayList<>();

        //check each follower from twitter to determine who is new and we potentially could follow
        for (long id : agent.getFollowers()) {

            //if we don't have this user id, they are a new follower
            if (!agent.getListFollowers().hasValue(id)) {

                //if the id is not in the following list, we need to follow this user
                if (!TwitterList.hasValue(agent.getFollowing(), id)) {

                    //add it to the list
                    follow.add(id);
                }
            }
        }

        //return our result
        return follow;
    }

    /**
     * Get new un-followers
     * @return Users that have un-followed me on twitter
     */
    protected static ArrayList<Long> getNewUnfollowers(Agent agent) {

        //list of users for us to unfollow
        ArrayList<Long> unfollow = new ArrayList<>();

        //now check each follower missing from twitter to determine who we should unfollow
        for (String id : agent.getListFollowers().getValues()) {

            //if the follower id from our previous list of followers is not in the current list, they have unfollowed us
            if (!TwitterList.hasValue(agent.getFollowers(), id)) {

                //add to the list
                unfollow.add(Long.parseLong(id));
            }
        }

        //return our result
        return unfollow;
    }

    protected static void followUsers(Agent agent, Twitter twitter) {

        //list of users for us to follow and un-follow
        ArrayList<Long> follow = getNewFollowers(agent);
        displayMessage("New followers: " + follow.size(), agent.getWriter());

        //let's follow all our new followers
        displayMessage("Following new followers", agent.getWriter());
        for (long id : follow) {
            follow(agent, twitter, id);
        }

        /**
         * Now lets check our pending follow list
         */
        String[] users = new String[agent.getListPendingFollow().getValues().size()];
        displayMessage("Checking pending followers list size: " + users.length, agent.getWriter());
        for (int i = 0; i < users.length; i++) {
            users[i] = agent.getListPendingFollow().getValues().get(i);
        }

        //follow users in pending list
        for (String id : users) {
            follow(agent, twitter, Long.parseLong(id));
        }
    }

    protected static void unfollowUsers(Agent agent, Twitter twitter) {

        ArrayList<Long> unfollow = getNewUnfollowers(agent);
        displayMessage("New Unfollowers: " + unfollow.size(), agent.getWriter());

        //let's un-follow our new un-followers
        displayMessage("Unfollowing new un-followers", agent.getWriter());
        for (long id : unfollow) {
            unfollow(agent, twitter, id, true);
        }

        //let's see if we can un-follow anyone in our pending list
        String[] users = new String[agent.getListPendingUnfollow().getValues().size()];
        displayMessage("Checking pending un-followers list size: " + users.length, agent.getWriter());
        for (int i = 0; i < users.length; i++) {
            users[i] = agent.getListPendingUnfollow().getValues().get(i);
        }

        //un-follow users in the pending list
        for (String id : users) {
            unfollow(agent, twitter, Long.parseLong(id), false);
        }
    }

    /**
     * Unfollow a twitter user.<br>
     * If we have already reached our unfollow limit the user will be added to the pending unfollow list
     * @param twitter Object used to interact with twitter
     * @param id The id of the user we want to un-follow
     * @param ignore Do we not want to follow this user again in the future even if they follow us
     * @return true if the user was followed, false otherwise
     */
    protected static boolean unfollow(Agent agent, Twitter twitter, long id, boolean ignore) {

        try {

            //make sure we haven't passed the un-follow limit
            if (agent.getUnfollows() <= UNFOLLOW_LIMIT) {

                //increase the number of un-follows
                agent.setUnfollows(agent.getUnfollows() + 1);

                //un-follow this user
                twitter.destroyFriendship(id);

                //if this exists in our pending list,remove it
                if (agent.getListPendingUnfollow().hasValue(id))
                    agent.getListPendingUnfollow().removeValue(id);

                //if we have this value in our list, remove it
                if (TwitterList.hasValue(agent.getFollowing(), id))
                    TwitterList.removeValue(agent.getFollowing(), id);

                //if we are un-following and we don't want to follow them back in the future we will ignore them
                if (ignore) {

                    //if it isn't in the ignore list yet let's add it
                    if (!agent.getListIgnore().hasValue(id))
                        agent.getListIgnore().addValue(id);
                }

                //success
                return true;
            }
            else
            {
                //add to pending list if it doesn't already exist
                if (!agent.getListPendingUnfollow().hasValue(id))
                    agent.getListPendingUnfollow().addValue(id);
            }

        } catch (TwitterException e) {

            //either of these errors means the user has blocked us from following them
            if (e.getErrorCode() == 34 || e.getErrorCode() == 404 ||
                    e.getStatusCode() == 34 || e.getStatusCode() == 404) {

                //if this exists in our pending list, we will remove it
                if (agent.getListPendingUnfollow().hasValue(id))
                    agent.getListPendingUnfollow().removeValue(id);
            }

            e.printStackTrace();

        } catch (Exception e) {
            e.printStackTrace();
        }

        //user was not un-followed
        return false;
    }

    protected static void searchTweets(Agent agent, Twitter twitter) throws Exception {

        //look for new tweeting users
        String[] keywords = SEARCH_HASHTAG.split(",");

        //search each keyword
        for (String keyword : keywords) {

            //skip if the keyword doesn't exist
            if (keyword == null || keyword.trim().length() < 2)
                continue;

            //display what we are doing next
            displayMessage("Searching tweets for keyword: " + keyword, agent.getWriter());

            //now we can search for the keyword and see if we can follow some people
            List<Status> tweets = TwitterBotHelper.getTwitterSearch(twitter, keyword, SEARCH_RESULTS);

            //check each tweet
            for (Status tweet : tweets) {

                //get the total number of followers/following for this specific user
                double countFollowers = tweet.getUser().getFollowersCount();
                double countFollowing = tweet.getUser().getFriendsCount();

                //make sure the user has an acceptable follow back rate
                if (countFollowing / countFollowers >= FOLLOW_RATE) {

                    //also make sure the user is not my own twitter name
                    if (!tweet.getUser().getName().replaceAll("@", "").equalsIgnoreCase(USERNAME)) {

                        try {

                            //follow user
                            follow(agent, twitter, tweet.getUser().getId());

                        } catch (Exception e) {
                            e.printStackTrace();
                        }
                    }
                }
            }

            displayMessage("Done: " + keyword, agent.getWriter());
        }
    }

    /**
     * Follow a twitter user.<br>
     * If we have already reached our follow limit the user will be added to the pending follow list
     * @param twitter Object used to interact with twitter
     * @param id The id of the user we want to follow
     * @return true if the user was followed, false otherwise
     */
    protected static boolean follow(Agent agent, Twitter twitter, long id) {

        try {

            //if we are already following, don't do it again
            if (TwitterList.hasValue(agent.getFollowing(), id)) {

                //if this exists in our pending list, remove it
                if (agent.getListPendingFollow().hasValue(id))
                    agent.getListPendingFollow().removeValue(id);

                //return false
                return false;
            }

            //we also don't want to follow if we are ignoring the user
            if (agent.getListIgnore().hasValue(id)) {

                //if this exists in our pending list,remove it because we don't want to follow
                if (agent.getListPendingFollow().hasValue(id))
                    agent.getListPendingFollow().removeValue(id);

                return false;
            }


            //if the user previously blocked us, don't follow again
            if (agent.getListBlocked().hasValue(id))
                return false;

            //make sure we haven't passed the follow limit and that we haven't exceeded our ratio
            if (agent.getFollows() <= FOLLOW_LIMIT && agent.getFollowRatio() < RATIO_LIMIT) {

                //increase the number of follows
                agent.setFollows(agent.getFollows() + 1);

                //follow this user
                twitter.createFriendship(id, false);

                //if this exists in our pending list,remove it
                if (agent.getListPendingFollow().hasValue(id))
                    agent.getListPendingFollow().removeValue(id);

                //keep track of who we are following
                agent.getFollowing().add(id);

                //success
                return true;

            } else {

                //add to pending list if it doesn't already exist
                if (!agent.getListPendingFollow().hasValue(id))
                    agent.getListPendingFollow().addValue(id);
            }

            if (agent.getFollowRatio() >= RATIO_LIMIT)
                displayMessage("We reached our ratio follow limit for now", agent.getWriter());

        } catch (TwitterException e) {

            //this means the user has blocked us from following them
            if (e.getErrorCode() == 162 || e.getErrorCode() == 403 || e.getStatusCode() == 162 || e.getStatusCode() == 403) {

                //if this exists in our pending list, we will remove it
                if (agent.getListPendingFollow().hasValue(id))
                    agent.getListPendingFollow().removeValue(id);

                //we will add to our blocked list if the value does not exist
                if (!agent.getListBlocked().hasValue(id))
                    agent.getListBlocked().addValue(id);
            }

            //display error
            e.printStackTrace();

        } catch (Exception e) {
            e.printStackTrace();
        }

        //failure
        return false;
    }
}
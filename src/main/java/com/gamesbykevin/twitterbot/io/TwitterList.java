package com.gamesbykevin.twitterbot.io;

import java.io.*;
import java.util.ArrayList;

import static com.gamesbykevin.twitterbot.util.LogFile.FILE_SEPARATOR;
import static com.gamesbykevin.twitterbot.util.LogFile.getFileDirectory;

public class TwitterList {

    //list of users
    private ArrayList<String> values;

    /**
     * We have a twitter file list for each of these keys
     */
    public enum Key {

        //list of users we are following
        Followers("followers.txt"),

        //store the last time the bot run to prevent us from being blocked by twitter
        LastRun("lastrun.txt"),

        //list of users we want to follow
        PendingFollow("pendingFollow.txt"),

        //list of users we want to unfollow
        PendingUnfollow("pendingUnfollow.txt"),

        //list of users who blocked us
        Blocked("blocked.txt"),

        //list of users we don't want to follow
        Ignore("ignore.txt");

        private final String filename;

        Key(String filename) {
            this.filename = getFileDirectory() + FILE_SEPARATOR + filename;
        }
    }

    private final Key key;

    public TwitterList(Key key) throws IOException {

        //save the reference
        this.key = key;

        //create our reference object
        File file = new File(key.filename);

        //make sure the file exists before we attempt to read it
        if (file.exists()) {

            //create the file reader object
            FileReader fileReader = new FileReader(file);

            //always wrap FileReader in BufferedReader.
            BufferedReader bufferedReader = new BufferedReader(fileReader);

            //the current line
            String line = bufferedReader.readLine();

            //continue to check every line
            while (line != null) {

                //if it exists we will add to the list
                if (line.trim().length() > 1)
                    getValues().add(line);

                //read the next line
                line = bufferedReader.readLine();
            }

            //close the reader when done
            bufferedReader.close();
            bufferedReader = null;
            fileReader.close();
            fileReader = null;
            line = null;

        } else {

            //if the file doesn't exist, create it
            file.createNewFile();
        }

        file = null;
    }

    public ArrayList<String> getValues() {

        if (this.values == null)
            this.values = new ArrayList<>();

        return this.values;
    }

    public void removeValues() {

        //remove all values
        getValues().clear();
    }

    public boolean removeValue(long value) {
        return removeValue(String.valueOf(value));
    }

    public boolean removeValue(String value) {

        //we won't be able to remove a null value
        if (value == null)
            return false;

        //check each element
        for (int i = 0; i < getValues().size(); i++) {

            //if the value exists and matches
            if (getValues().get(i) != null && getValues().get(i).equalsIgnoreCase(value)) {
                //remove the item
                getValues().remove(i);

                //return true as item was removed successfully
                return true;
            }
        }

        //item was not found, thus not removed
        return false;
    }

    public boolean addValue(long value) {
        return addValue(String.valueOf(value));
    }

    public boolean addValue(String value) {

        //we won't be able to add a null value
        if (value == null)
            return false;

        //check each element
        for (String line : getValues()) {

            //if the value already exists, we won't add again
            if (line != null && line.equalsIgnoreCase(value))
                return false;
        }

        //add value to list
        getValues().add(value);

        //return true as value was added successfully
        return true;
    }

    public boolean hasValue(long value) {
        return hasValue(String.valueOf(value));
    }

    public boolean hasValue(String value) {

        //we won't be able to add a null value
        if (value == null)
            return false;

        //check each element
        for (String line : getValues()) {

            //if the value already exists, return true
            if (line != null && line.equalsIgnoreCase(value))
                return true;
        }

        //value was not found, return false
        return false;
    }

    public static boolean hasValue(ArrayList<Long> values, String value) {
        return hasValue(values, Long.parseLong(value));
    }

    public static boolean hasValue(ArrayList<Long> values, long value) {

        //check each element
        for (long tmp : values) {

            //if the value already exists, return true
            if (tmp == value)
                return true;
        }

        //value was not found, return false
        return false;
    }

    /**
     * Remove the value from the list
     * @param values The list of ids we are checking
     * @param value The id we want to remove
     * @return true if the value was removed, false otherwise
     */
    public static boolean removeValue(ArrayList<Long> values, long value) {

        for (int i = 0; i < values.size(); i++) {

            //if it is a match
            if (values.get(i) == value) {
                //remove it
                values.remove(i);

                //return true for success
                return true;
            }
        }

        //not found return false
        return false;
    }

    public void save() throws Exception {

        //create our file writer, and don't append
        FileWriter fileWriter = new FileWriter(key.filename, false);

        //use this to write to the file
        BufferedWriter bufferedWriter = new BufferedWriter(fileWriter);

        //write each user to the list
        for (String line : getValues()) {

            //if null, we won't write to the file
            if (line == null)
                continue;

            //write to file
            bufferedWriter.write(line);

            //add new line
            bufferedWriter.newLine();
        }

        //write any remaining bytes to the stream
        bufferedWriter.flush();

        //close once done
        bufferedWriter.close();

        //recycle
        bufferedWriter = null;
        fileWriter.close();
        fileWriter = null;
    }

    public void recycle() {

        if (this.values != null)
            this.values.clear();

        this.values = null;
    }
}
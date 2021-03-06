package com.example.reconnect;

import android.content.ContentValues;
import android.content.Context;
import android.database.Cursor;
import android.database.sqlite.SQLiteDatabase;

import android.util.Log;

import java.util.ArrayList;
import java.util.Calendar;
import java.util.HashMap;


//Have tested all methods excluding delete person, delete interaction and update methods.


public class DataManager {

     protected  ReconnectDBHelper helper;
     public DataManager(Context c){
         helper = new ReconnectDBHelper(c);
         helper.getWritableDatabase().execSQL(ReconnectContract.createPersonTable());
         helper.getWritableDatabase().execSQL(ReconnectContract.createInteractionTable());

     }

    //Tested create tables, add persons to table, add interactions to table, get interaction, get contacts

    /**
     *Add a single person's record to the person table.This method is overloaded.
     *
     * @param firstName
     * @param lastName
     * @param pictureLoc
     * @param freq_contact
     * @return
     */
    public boolean addPersonRecord(String firstName, String lastName, String pictureLoc, String relationship,  String freq_contact){
        //add timestamp of when this record was added.
        Calendar today = Calendar.getInstance();
        String add_date = this.formatCalendarDate(today);
        return addPersonRecord(firstName, lastName,pictureLoc,relationship,freq_contact,add_date);
    }


   //Allows you to manually specify when the person's record is added.
    public boolean addPersonRecord(String firstName, String lastName, String pictureLoc, String relationship,  String freq_contact, String add_date){
        //Map of values where column names are used as keys.
        ContentValues values = new ContentValues();
        values.put(ReconnectContract.Person.FIRST_NAME, firstName);
        values.put(ReconnectContract.Person.LAST_NAME, lastName);
        values.put(ReconnectContract.Person.PIC_LOCATION, pictureLoc);
        values.put(ReconnectContract.Person.CONTACT_RELATIONSHIP, relationship);
        values.put(ReconnectContract.Person.CONTACT_FREQUENCY, freq_contact);
        values.put(ReconnectContract.Person.CONTACT_ADD_DATE, add_date);

        SQLiteDatabase db = helper.getWritableDatabase();
        try{

            long value = db.insert(ReconnectContract.Person.TABLE_NAME, null, values);
            return value >= 0;


        }catch(Exception e){
            Log.i("Mainactivity class", "Adding to person table failed");
        }
        return false;
    }




    /**
     *
     * Add an interaction record given a person's first name and last name.
     *
     * @param date
     * @param duration
     * @param type
     * @param notes
     * @param first_name
     * @param last_name
     * @return
     */

    public  boolean addInteractionRecord(String date, String duration, String type, String notes, String first_name, String last_name){

        String contact_id = getIDFromName(first_name, last_name);
        return addInteractionRecord(date, duration, type, notes, contact_id);
    }





    /**
     *Add a single person's record to the person table.
     * @param
     * @param
     * @param
     * @param contact_id: add interaction record using the contact_id to tie it to specific person.
     *
     *
     * @return
     */
    public  boolean addInteractionRecord(String date, String duration, String type, String notes, String contact_id){
        SQLiteDatabase db = helper.getWritableDatabase();

        ContentValues values  = new ContentValues();
        values.put(ReconnectContract.Interaction.DATE, date);
        values.put(ReconnectContract.Interaction.DURATION, duration);
        values.put(ReconnectContract.Interaction.NOTES, notes);
        values.put(ReconnectContract.Interaction.TYPE, type);
        values.put(ReconnectContract.Interaction.CONTACT_ID, contact_id);

        //when we add an interaction record, we reset our reminder status so that new reminders can be given to
        //a person.
        Contact curContact = getNameFromID(contact_id);
        updateContactReminder(curContact.first_name, curContact.last_name, "");

        try{
            db.insert(ReconnectContract.Interaction.TABLE_NAME, null, values);
            return true;

        }catch(Exception e){
            Log.i("Mainactivity class", "Adding to interactions table failed");
        }
        return false;
    }


    //Return an arraylist of contacts, where each entry is a person.
    public ArrayList<Contact> getContacts(){
        SQLiteDatabase db = helper.getReadableDatabase();

        String[] projection = {
                ReconnectContract.Person._ID,
                ReconnectContract.Person.FIRST_NAME,
                ReconnectContract.Person.LAST_NAME,
                ReconnectContract.Person.PIC_LOCATION,
                ReconnectContract.Person.CONTACT_RELATIONSHIP,
                ReconnectContract.Person.CONTACT_FREQUENCY,
                ReconnectContract.Person.CONTACT_ADD_DATE,
                ReconnectContract.Person.REMINDER_NOTE
        };

        String sortOrder = ReconnectContract.Person.LAST_NAME  + " DESC";
        Cursor cursor = db.query(
                ReconnectContract.Person.TABLE_NAME,
                projection,
                null,
                null,
                null,
                null,
                sortOrder
        );

        ArrayList<Contact> friends = new ArrayList<>();
        while(cursor.moveToNext()){
            int indexIDColumn = cursor.getColumnIndexOrThrow(ReconnectContract.Person._ID);
            int firstNameColumn = cursor.getColumnIndexOrThrow(ReconnectContract.Person.FIRST_NAME);
            int lastNameColumn = cursor.getColumnIndexOrThrow(ReconnectContract.Person.LAST_NAME);
            int picLocationColumn = cursor.getColumnIndexOrThrow(ReconnectContract.Person.PIC_LOCATION);
            int relationshipColumn = cursor.getColumnIndexOrThrow(ReconnectContract.Person.CONTACT_RELATIONSHIP);
            int contactFreqColumn = cursor.getColumnIndexOrThrow(ReconnectContract.Person.CONTACT_FREQUENCY);
            int dateAddedColumn = cursor.getColumnIndexOrThrow(ReconnectContract.Person.CONTACT_ADD_DATE);
            int reminderNoteColumn = cursor.getColumnIndexOrThrow(ReconnectContract.Person.REMINDER_NOTE);

            Contact myFriend = new Contact();

            myFriend.id = cursor.getString(indexIDColumn);
            myFriend.first_name = cursor.getString(firstNameColumn);
            myFriend.last_name = cursor.getString(lastNameColumn);
            myFriend.pic_location = cursor.getString(picLocationColumn);
            myFriend.contact_relationship = cursor.getString(relationshipColumn);
            myFriend.contact_frequency = cursor.getString(contactFreqColumn);
            myFriend.date_added = cursor.getString(dateAddedColumn);
            myFriend.reminder_note = cursor.getString(reminderNoteColumn);

            friends.add(myFriend);
        }

        cursor.close();
        return friends;
    }

    //Generally not called directly.
    private ArrayList<Communication> getInteractionsHelper(String filterColumn, String[] filterValues){
        SQLiteDatabase db = helper.getReadableDatabase();

        String sortOrder = ReconnectContract.Interaction.DATE  + " DESC";
        Cursor cursor = db.query(
                ReconnectContract.Interaction.TABLE_NAME,
                null, //get all data that interactions contain.
                filterColumn,
                filterValues,
                null,
                null,
                sortOrder
        );


        ArrayList<Communication> interactions = new ArrayList<>();
        while (cursor.moveToNext()){
            int indexIDColumn = cursor.getColumnIndexOrThrow(ReconnectContract.Interaction._ID);
            int dateColumn = cursor.getColumnIndexOrThrow(ReconnectContract.Interaction.DATE);
            int durationColumn = cursor.getColumnIndexOrThrow(ReconnectContract.Interaction.DURATION);
            int typeColumn = cursor.getColumnIndexOrThrow(ReconnectContract.Interaction.TYPE);
            int notesColumn = cursor.getColumnIndexOrThrow(ReconnectContract.Interaction.NOTES);
            int contactIDColumn =   cursor.getColumnIndexOrThrow(ReconnectContract.Interaction.CONTACT_ID);

            Communication comm = new Communication();
            comm.contact_id = cursor.getString(contactIDColumn);
            comm.notes = cursor.getString(notesColumn);
            comm.type = cursor.getString(typeColumn);
            comm.duration = cursor.getString(durationColumn);
            comm.date = cursor.getString(dateColumn);

            interactions.add(comm);

        }
        cursor.close();
        return interactions;


    }


    public ArrayList<Communication> getAllInteractions(){
        return getInteractionsHelper(null, null);
    }


    //Filter based on person's first and last name. Also pass in date range for interaction.
    //Can be used to generate contact history for a person.
    //Pass in a large number if you want all recent interactions e.g. 10000
    public ArrayList<Communication> getAllInteractionsForPerson(String firstName, String lastName, int nDays){

        //get the date range for a person.
        Calendar curCalendar = Calendar.getInstance();
        String curDate = formatCalendarDate(curCalendar);
        curCalendar.add(Calendar.DATE, -1 *  nDays);
        String previousDate = formatCalendarDate(curCalendar);

        //find the person's id.
        String  person_id = getIDFromName(firstName, lastName);

        //build the query.
        String selection = ReconnectContract.Interaction.CONTACT_ID + " = ? AND " + ReconnectContract.Interaction.DATE + " <= ? AND " + ReconnectContract.Interaction.DATE + " >= ?" ;;
        String[] selectionArgs = {person_id, curDate, previousDate};
        return getInteractionsHelper(selection, selectionArgs);
    }

    /**
     * For timeline feature
     * ***BUG: This method gives date assuming January is 0. Need to add fix
     *Return an arraylist of interactions in the last n days,
     *where each element corresponds to one interaction.
     * Dates should be inputted in format Year--Month
     *@return
     */
    public  ArrayList<Communication> getAllInteractionsByDate(int nDays){
        Calendar curCalendar = Calendar.getInstance();
        String curDate = formatCalendarDate(curCalendar);
        curCalendar.add(Calendar.DATE, -1 *  nDays);
        String previousDate = formatCalendarDate(curCalendar);
        ArrayList<Communication> allCommunications = getAllInteractions();
        ArrayList<Communication> datedComms = new ArrayList<>();
        for(Communication cur: allCommunications){
            if (cur.date.compareTo(previousDate) >= 0){
                datedComms.add(cur);
            }
        }
        return datedComms;
    }


    /**
     * TODO: Need to remove hardcoded string for reminder check here.
     * Returns a hashmap of people that you should reconnect with
     * The keys are the contact objetcs, the values are the strings you want to display for those people.
     * @return
     */
    public HashMap<Contact, String> getContactsToReconnectWith(){
        ArrayList<Contact> friends = getContacts();
        HashMap<Contact, String> peopleToTalkTo = new HashMap<>();
        Calendar curCalendar = Calendar.getInstance();
        String todayDate = formatCalendarDate(curCalendar);
        for (Contact friend: friends){
            //how often to reconnect with a friend in days.
            int freqContactInDays  =  getDaysFromString(friend.contact_frequency);

            //num of days which have passed since from date contact added to today.
            int diffBetweenAddedDateAndToday = differenceInDays(friend.date_added, this.formatCalendarDate(Calendar.getInstance()));
            
             
           
            curCalendar = Calendar.getInstance();
            curCalendar.add(Calendar.DATE, -1 *  freqContactInDays);

             //date we expect you to have reconnected by.
            String idealReconnectDate = formatCalendarDate(curCalendar);




            ArrayList<Communication> contactInteractions = getAllInteractionsForPerson(friend.first_name,friend.last_name,10000);


            boolean reminderAlreadyGiven = friend.reminder_note != null && friend.reminder_note.equalsIgnoreCase("REMINDME");

            //no interaction with a person yet. If date of expected interaction has passed, remind user to initiate interaction.
            //if date has not passed, user still has time to reconnect. No need to send reminder.
            if (contactInteractions.isEmpty()&& diffBetweenAddedDateAndToday > freqContactInDays && reminderAlreadyGiven == false){
                peopleToTalkTo.put(friend, "You have not connected with " + friend.first_name + " yet.");
                continue;
            }

            //separate if statement needed because it's possible there's no interaction yet, and user still has time to add it.
            //after this statement, we assume at least one interaction, so code may crash if statement removed.
            if (contactInteractions.isEmpty()){
                continue;
            }

            String previousReconnectDate = contactInteractions.get(0).date; //date you actually reconnected with the friend last.;

            //if you have not connected with this person recently, generate reminder for communication.
            if (idealReconnectDate.compareTo(previousReconnectDate) > 0 && reminderAlreadyGiven == false){
                 peopleToTalkTo.put(friend, reconnectMessage(friend.first_name, previousReconnectDate, todayDate));
            }


        }
        return peopleToTalkTo;
    }

    //return the difference given two dates in days(integer).
     private int differenceInDays(String previousDate, String curDate){

         String[] previousTime = previousDate.split("-");
         String[] curTime = curDate.split("-");

         int prevDay = Integer.parseInt(previousTime[2].trim());
         int prevMonth = Integer.parseInt(previousTime[1].trim());
         int prevYear = Integer.parseInt(previousTime[0].trim());

         int curDay = Integer.parseInt(curTime[2].trim());
         int curMonth = Integer.parseInt(curTime[1].trim());
         int curYear = Integer.parseInt(curTime[0].trim());

         curDay = curDay - prevDay;
         if (curDay < 0){
             curDay = curDay + 30;
             curMonth = curMonth - 1;
         }

         curMonth = curMonth - prevMonth;
         if (curMonth < 0){
             curMonth = curMonth + 12;
             curYear = curYear -  1;
         }

         curYear = curYear - prevYear;
         if (curYear < 0){
             //throw new Error("Passing incorrect year data. Previous year should be less than or equal to the current year");
             Log.i("Warning", "Previous date is larger than current date");
         }
         int totalDays = (curYear * 365) + (curMonth * 28) + curDay;
         return totalDays;

     }
    /**
     * TODO: Method is functional but does too much. Need to reorganize code into smaller methods.
     * TODO: Also, might have rounding error in method because  in code logic, we are just getting the int component after division.
     *
     *
     *   given date of previous interaction and the current date (or other point of reference),
     *   return a personalized string for reconnection.
     */


     private String reconnectMessage(String firstName, String previousDate, String curDate){
        int totalDays = differenceInDays(previousDate, curDate);
        String message = "";

        if (totalDays > 1 && totalDays < 14){
            message = "It has been " + totalDays + " days since you last connected with " + firstName + ".";
        }else if(totalDays >= 14 && totalDays <= 60){
            message = "It has been " + totalDays / 7 + " weeks since you last connected with " + firstName + ".";
        }else if(totalDays > 60 && totalDays < 365){
            message = "It has been " + totalDays/30 + " months since you last connected with " + firstName + ".";
        }else if(totalDays > 365){
            message = "It has been over " + totalDays/365 + " years since you last connected with " + firstName + ".";
        }

        return message;

     }





    //properly format dates so that they are in the form YYYY-MM-DD
    private String formatCalendarDate(Calendar curCalendar){
        String curYear = curCalendar.get(Calendar.YEAR) + "";
        String curMonth = curCalendar.get(Calendar.MONTH) + "";
        String curDay = curCalendar.get(Calendar.DAY_OF_MONTH) + "";

        //month goes from 0-11. Need to recalibrate to 1-12.
        curMonth = (Integer.parseInt(curMonth) + 1) + "";
        if (curMonth.length() < 2){
            curMonth = "0" + curMonth;
        }
        if (curDay.length() < 2){
            curDay = "0" + curDay;
        }
        return curYear + "-" + curMonth  + "-" + curDay;
    }

    //get the contact frequency info as an int in days. E.g Method will return 14 if string is "2 weeks"
    public int getDaysFromString(String contactFreq){
        String[] time = contactFreq.split(" ");
        int days = 7; //7 is just default (i.e. talk to person every week)
        try{
            days = Integer.parseInt(time[0]);

            if (time[1].equalsIgnoreCase("weeks") || time[1].equalsIgnoreCase("week") ){
                days = days * 7;
            }else if(time[1].equalsIgnoreCase("months") || time[1].equalsIgnoreCase("month")){
                days = days * 30;
            }else if(time[1].equalsIgnoreCase("years") || time[1].equalsIgnoreCase("year")){
                days = days * 365;
            }
            return days;

        }catch(Exception e){
            Log.i("DataManager.java", "Could not process date");
            return days;
        }
    }




    //get contact given person's id. Still need to test this method. NOT TESTED YET.
    public Contact getNameFromID(String contact_id){
        SQLiteDatabase db = helper.getReadableDatabase();
        String selection = ReconnectContract.Person._ID + " = ?";
        String[] selectionArgs = {contact_id};

        Cursor cursor = db.query(
                ReconnectContract.Person.TABLE_NAME,
                null,
                selection,
                selectionArgs,
                null,
                null,
                null
        );
        cursor.moveToNext();
        int firstName_Column = cursor.getColumnIndexOrThrow(ReconnectContract.Person.FIRST_NAME);
        int lastName_Column = cursor.getColumnIndexOrThrow(ReconnectContract.Person.LAST_NAME);
        int picLocationColumn = cursor.getColumnIndexOrThrow(ReconnectContract.Person.PIC_LOCATION);
        int relationshipColumn = cursor.getColumnIndexOrThrow(ReconnectContract.Person.CONTACT_RELATIONSHIP);
        int contactFreqColumn = cursor.getColumnIndexOrThrow(ReconnectContract.Person.CONTACT_FREQUENCY);
        int reminderNoteColumn = cursor.getColumnIndexOrThrow(ReconnectContract.Person.REMINDER_NOTE);



        Contact newPerson = new Contact();
        newPerson.first_name = cursor.getString(firstName_Column);
        newPerson.last_name = cursor.getString(lastName_Column);
        newPerson.pic_location = cursor.getString(picLocationColumn);
        newPerson.contact_relationship = cursor.getString(relationshipColumn);
        newPerson.contact_frequency = cursor.getString(contactFreqColumn);
        newPerson.reminder_note = cursor.getString(reminderNoteColumn);


        cursor.close();
        return newPerson;
    }


    //get id of person given their first and last name.
    public String getIDFromName(String firstName, String lastName){
         Log.i("Searching for id of ", firstName + " " + lastName);
        SQLiteDatabase db = helper.getReadableDatabase();
        String selection = ReconnectContract.Person.FIRST_NAME + " = ? AND " +
                ReconnectContract.Person.LAST_NAME + " = ?";
        String[] selectionArgs = {firstName, lastName};
        Cursor cursor = db.query(
                ReconnectContract.Person.TABLE_NAME,
                null,
                selection,
                selectionArgs,
                null,
                null,
                null
        );
        cursor.moveToNext();
        int id_Column = cursor.getColumnIndexOrThrow(ReconnectContract.Person._ID);
        String id = cursor.getString(id_Column);
        Log.i("Id for " + firstName, id);
        cursor.close();
        return id;
    }






    //delete a contact given their first and last name.
    public boolean deleteContact(String firstName, String lastName){
        String contact_id = getIDFromName(firstName, lastName);
        String selection = ReconnectContract.Person._ID + " = ?"; //filter based on the person's id, which matches contact_id in interactions table.
        String[] selectionArgs = {contact_id};
        try {
            int deletedRows = helper.getWritableDatabase().delete(ReconnectContract.Person.TABLE_NAME, selection, selectionArgs);
            return true;
        }catch(Exception e){
            Log.i("Main activity failed", "Can't delete from contact table");
            return false;
        }
    }


    //Allows you to delete an interaction based on the contact's name, type of interaction and date.
    //All this info required to ensure that we don't accidentally delete wrong data.
    public boolean deleteInteraction(String firstName, String lastName, String typeInteraction, String dateInteraction){
        String contact_id = getIDFromName(firstName, lastName);
        String selection = ReconnectContract.Interaction.CONTACT_ID + " = ? " + ReconnectContract.Interaction.TYPE + " = ? " + ReconnectContract.Interaction.DATE + " = ?";
        String[] selectionArgs = {contact_id, typeInteraction, dateInteraction};
        try {
            int deletedRows = helper.getWritableDatabase().delete(ReconnectContract.Interaction.TABLE_NAME, selection, selectionArgs);
            return true;
        }catch(Exception e){
            Log.i("Main activity failed", "Can't delete from interactions table");
            return false;
        }
    }

    /**
     * Method to remove all the data from the database.
     * Method must be passed "DELETEALL" to ensure that it is not called accidentally.
     * Warning: Will clear out all contacts and interactions!
     * Will reinitialize the tables with no values.
     */
    public void clearAllData(String deleteConfirmation){
        if (deleteConfirmation.equals("DELETEALL")) {
            SQLiteDatabase db = helper.getWritableDatabase();
            db.execSQL("DROP TABLE IF EXISTS " + ReconnectContract.Person.TABLE_NAME);
            db.execSQL("DROP TABLE IF EXISTS " + ReconnectContract.Interaction.TABLE_NAME);
            db.execSQL(ReconnectContract.createPersonTable());
            db.execSQL(ReconnectContract.createInteractionTable());
        }
    }


    /** TODO: Update a person's info based on new info.
     * Must be made when button is clicked/activity launched.
     * Can't write function independently.
     *
     * @param firstName
     * @param lastName
     * @return
     */
    public boolean updateContact(String firstName, String lastName){
        return false;
    }


    public void updateContactReminder(String firstName, String lastName, String newReminder){
        SQLiteDatabase db = helper.getWritableDatabase();

        ContentValues values = new ContentValues();
        values.put(ReconnectContract.Person.REMINDER_NOTE, newReminder);

        String whereClause = ReconnectContract.Person._ID + " = ?";
        String whereArgs[] = {getIDFromName(firstName, lastName)};
        db.update(ReconnectContract.Person.TABLE_NAME, values, whereClause, whereArgs);


    }


    /** TODO: Update interaction info based on new data.
     * Must be made when button is clicked/trigger event occurs.
     * Can't write function independently.
     * @return
     */
    public boolean updateInteraction(){
        return false;
    }







}

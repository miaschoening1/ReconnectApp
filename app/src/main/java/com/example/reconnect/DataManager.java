package com.example.reconnect;

import android.content.ContentValues;
import android.content.Context;
import android.database.Cursor;
import android.database.sqlite.SQLiteDatabase;

import android.util.Log;

import java.util.ArrayList;
import java.util.Calendar;


//Have tested all methods excluding delete person, delete interaction and update methods.


public class DataManager {

     protected  ReconnectDBHelper helper;
     public DataManager(Context c){
         helper = new ReconnectDBHelper(c);
         helper.getWritableDatabase().execSQL(ReconnectContract.createPersonTable());
         helper.getWritableDatabase().execSQL(ReconnectContract.createInteractionTable());

     }

    //Tested create tables, add persons to table, add interactions to table, get interaction, get contacts
    //Test

    /**
     *Add a single person's record to the person table.
     * @param firstName
     * @param lastName
     * @param pictureLoc
     * @param freq_contact
     * @return
     */
    public boolean addPersonRecord(String firstName, String lastName, String pictureLoc, String relationship,  String freq_contact){

        //Map of values where column names are used as keys.
        ContentValues values = new ContentValues();
        values.put(ReconnectContract.Person.FIRST_NAME, firstName);
        values.put(ReconnectContract.Person.LAST_NAME, lastName);
        values.put(ReconnectContract.Person.PIC_LOCATION, pictureLoc);
        values.put(ReconnectContract.Person.CONTACT_RELATIONSHIP, relationship);
        values.put(ReconnectContract.Person.CONTACT_FREQUENCY, freq_contact);

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
                ReconnectContract.Person.CONTACT_RELATIONSHIP
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

            Contact myFriend = new Contact();
            myFriend.id = cursor.getString(indexIDColumn);
            myFriend.first_name = cursor.getString(firstNameColumn);
            myFriend.last_name = cursor.getString(lastNameColumn);
            myFriend.pic_location = cursor.getString(picLocationColumn);
            myFriend.contact_relationship = cursor.getString(relationshipColumn);
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
        String curDate = curCalendar.get(Calendar.YEAR) + "-" + curCalendar.get(Calendar.MONTH) + "-" + curCalendar.get(Calendar.DAY_OF_MONTH);
        curCalendar.add(Calendar.DATE, -1 *  nDays);
        String previousDate = curCalendar.get(Calendar.YEAR) + "-" + curCalendar.get(Calendar.MONTH) + "-" + curCalendar.get(Calendar.DAY_OF_MONTH);

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
        String curDate = curCalendar.get(Calendar.YEAR) + "-" + curCalendar.get(Calendar.MONTH) + "-" + curCalendar.get(Calendar.DAY_OF_MONTH);
        curCalendar.add(Calendar.DATE, -1 *  nDays);
        String previousDate = curCalendar.get(Calendar.YEAR) + "-" + curCalendar.get(Calendar.MONTH) + "-" + curCalendar.get(Calendar.DAY_OF_MONTH);
        String dateSelection = ReconnectContract.Interaction.DATE + " <= ? AND " + ReconnectContract.Interaction.DATE + " >= ?" ;
        String[] dateRange = {curDate, previousDate};
        return getInteractionsHelper(dateSelection, dateRange);
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

        Contact newPerson = new Contact();
        newPerson.first_name = cursor.getString(firstName_Column);
        newPerson.last_name = cursor.getString(lastName_Column);

        cursor.close();
        return newPerson;
    }


    //get id of person given their first and last name.
    public String getIDFromName(String firstName, String lastName){
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


    /** TODO: Update interaction info based on new data.
     * Must be made when button is clicked/trigger event occurs.
     * Can't write function independently.
     * @return
     */
    public boolean updateInteraction(){
        return false;
    }







}

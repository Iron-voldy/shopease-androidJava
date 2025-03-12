package com.example.shopease.models;

import android.content.ContentValues;
import android.content.Context;
import android.database.Cursor;
import android.database.sqlite.SQLiteDatabase;
import android.database.sqlite.SQLiteOpenHelper;

public class DatabaseHelper extends SQLiteOpenHelper {
    private static final String DATABASE_NAME = "SmartShop.db";
    private static final int DATABASE_VERSION = 1;

    // Table and column names
    private static final String TABLE_USERS = "Users";
    private static final String COLUMN_ID = "id";
    private static final String COLUMN_EMAIL = "email";
    private static final String COLUMN_FIRST_NAME = "firstName";
    private static final String COLUMN_LAST_NAME = "lastName";
    private static final String COLUMN_ROLE = "role";
    private static final String COLUMN_IS_BLOCKED = "isBlocked";

    private static final String CREATE_TABLE_USERS =
            "CREATE TABLE " + TABLE_USERS + " (" +
                    COLUMN_ID + " TEXT PRIMARY KEY, " +
                    COLUMN_EMAIL + " TEXT, " +
                    COLUMN_FIRST_NAME + " TEXT, " +
                    COLUMN_LAST_NAME + " TEXT, " +
                    COLUMN_ROLE + " TEXT, " +
                    COLUMN_IS_BLOCKED + " INTEGER)"; // Use INTEGER for boolean

    public DatabaseHelper(Context context) {
        super(context, DATABASE_NAME, null, DATABASE_VERSION);
    }

    @Override
    public void onCreate(SQLiteDatabase db) {
        db.execSQL(CREATE_TABLE_USERS);
    }

    @Override
    public void onUpgrade(SQLiteDatabase db, int oldVersion, int newVersion) {
        db.execSQL("DROP TABLE IF EXISTS " + TABLE_USERS);
        onCreate(db);
    }

    // Method to insert or update user data
    public void insertOrUpdateUser(String id, String email, String firstName, String lastName, String role, boolean isBlocked) {
        SQLiteDatabase db = this.getWritableDatabase();
        ContentValues values = new ContentValues();
        values.put(COLUMN_ID, id);
        values.put(COLUMN_EMAIL, email);
        values.put(COLUMN_FIRST_NAME, firstName);
        values.put(COLUMN_LAST_NAME, lastName);
        values.put(COLUMN_ROLE, role);
        values.put(COLUMN_IS_BLOCKED, isBlocked ? 1 : 0); // Convert boolean to integer

        // Insert or replace existing user data
        db.insertWithOnConflict(TABLE_USERS, null, values, SQLiteDatabase.CONFLICT_REPLACE);
        db.close();
    }

    // Method to check if the user exists
    public boolean isUserExists(String userId) {
        SQLiteDatabase db = this.getReadableDatabase();
        Cursor cursor = db.rawQuery("SELECT * FROM " + TABLE_USERS + " WHERE " + COLUMN_ID + "=?", new String[]{userId});
        boolean exists = cursor.getCount() > 0;
        cursor.close();
        db.close();
        return exists;
    }

    public String getUserRole() {
        SQLiteDatabase db = this.getReadableDatabase();
        String role = null;

        Cursor cursor = db.rawQuery("SELECT role FROM Users LIMIT 1", null);
        if (cursor.moveToFirst()) {
            role = cursor.getString(0);  // Get the role of the first user
        }

        cursor.close();
        db.close();
        return role;  // Returns role if found, otherwise null
    }

    // Add this method to DatabaseHelper class
    public String getUserId() {
        SQLiteDatabase db = this.getReadableDatabase();
        String userId = null;

        Cursor cursor = db.rawQuery("SELECT " + COLUMN_ID + " FROM " + TABLE_USERS + " LIMIT 1", null);
        if (cursor.moveToFirst()) {
            userId = cursor.getString(0); // Get the id of the first user
        }

        cursor.close();
        db.close();
        return userId; // Returns userId if found, otherwise null
    }

    // Add this method to DatabaseHelper class
    public void deleteAllUsers() {
        SQLiteDatabase db = this.getWritableDatabase();
        db.execSQL("DELETE FROM " + TABLE_USERS);
        db.close();
    }

    // Add this method to DatabaseHelper class
    public String getFullName(String userId) {
        SQLiteDatabase db = this.getReadableDatabase();
        String fullName = null;

        // Query to select firstName and lastName based on userId
        Cursor cursor = db.rawQuery("SELECT " + COLUMN_FIRST_NAME + ", " + COLUMN_LAST_NAME +
                        " FROM " + TABLE_USERS +
                        " WHERE " + COLUMN_ID + "=?",
                new String[]{userId});

        if (cursor.moveToFirst()) {
            String firstName = cursor.getString(0);  // Index 0 for firstName
            String lastName = cursor.getString(1);   // Index 1 for lastName

            // Combine first and last name with a space
            fullName = firstName + " " + lastName;
        }

        cursor.close();
        db.close();
        return fullName;  // Returns "First Last" if found, otherwise null
    }

}

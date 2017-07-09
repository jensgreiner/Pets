package com.example.android.pets.data;

import android.content.ContentProvider;
import android.content.ContentUris;
import android.content.ContentValues;
import android.content.UriMatcher;
import android.database.Cursor;
import android.database.sqlite.SQLiteDatabase;
import android.net.Uri;
import android.support.annotation.NonNull;
import android.util.Log;

import com.example.android.pets.data.PetContract.PetEntry;

/**
 * {@link ContentProvider} for Pets app.
 */
public class PetProvider extends ContentProvider {

    /**
     * Tag for the log messages
     */
    public static final String LOG_TAG = PetProvider.class.getSimpleName();

    private static final int PETS = 100;
    private static final int PET_ID = 101;
    private static final UriMatcher sUriMatcher = new UriMatcher(UriMatcher.NO_MATCH);

    static {
        sUriMatcher.addURI(PetContract.CONTENT_AUTHORITY, PetContract.PATH_PETS, PETS);

        sUriMatcher.addURI(PetContract.CONTENT_AUTHORITY, PetContract.PATH_PETS + "/#", PET_ID);
    }

    private PetDbHelper mDbHelper;

    /**
     * Initialize the provider and the database helper object.
     */
    @Override
    public boolean onCreate() {
        mDbHelper = new PetDbHelper(getContext());
        return true;
    }

    /**
     * Perform the query for the given URI. Use the given projection, selection, selection arguments, and sort order.
     */
    @Override
    public Cursor query(@NonNull Uri uri, String[] projection, String selection, String[] selectionArgs,
                        String sortOrder) {
        // Get readable database
        SQLiteDatabase database = mDbHelper.getReadableDatabase();

        // This cursor will hold the result of the query
        Cursor cursor;

        // Figure out if the URI matcher can match the URI to a specific code
        int match = sUriMatcher.match(uri);
        switch (match) {
            case PETS:
                // For the PETS code, query the pets table directly with the given
                // projection, selection, selection arguments, and sort order. The cursor
                // could contain multiple rows of the pets table.
                cursor = database.query(PetEntry.TABLE_NAME, projection, selection, selectionArgs, null, null, sortOrder);
                break;
            case PET_ID:
                // For the PET_ID code, extract out the ID from the URI.
                // For an example URI such as "content://com.example.android.pets/pets/3",
                // the selection will be "_id=?" and the selection argument will be a
                // String array containing the actual ID of 3 in this case.
                //
                // For every "?" in the selection, we need to have an element in the selection
                // arguments that will fill in the "?". Since we have 1 question mark in the
                // selection, we have 1 String in the selection arguments' String array.
                selection = PetEntry._ID + "=?";
                selectionArgs = new String[]{String.valueOf(ContentUris.parseId(uri))};

                // This will perform a query on the pets table where the _id equals 3 to return a
                // Cursor containing that row of the table.
                cursor = database.query(PetEntry.TABLE_NAME, projection, selection, selectionArgs,
                        null, null, sortOrder);
                break;
            default:
                throw new IllegalArgumentException("Cannot query unknown URI " + uri);
        }
        return cursor;
    }

    /**
     * Insert new data into the provider with the given ContentValues.
     */
    @Override
    public Uri insert(@NonNull Uri uri, ContentValues contentValues) {

        final int match = sUriMatcher.match(uri);
        switch (match) {
            case PETS:
                return insertPet(uri, contentValues);
            default:
                throw new IllegalArgumentException("Insertion is not supported for " + uri);
        }
    }

    private boolean genderIsValid(Integer gender) {
        return (gender == PetEntry.GENDER_UNKNOWN || gender == PetEntry.GENDER_MALE || gender == PetEntry.GENDER_FEMALE);
    }

    /**
     * Insert a pet into the database with the given content values. Return the new content URI
     * for that specific row in the database.
     */
    private Uri insertPet(Uri uri, ContentValues contentValues) {

        // Check that the name is not null
        String name = contentValues.getAsString(PetEntry.COLUMN_PET_NAME);
        if (name == null || name.isEmpty()) {
            throw new IllegalArgumentException("Pet requires a name");
        }

        // Check that gender is not valid
        Integer gender = contentValues.getAsInteger(PetEntry.COLUMN_PET_GENDER);
        if (gender == null || !genderIsValid(gender)) {
            throw new IllegalArgumentException("Gender is not valid");
        }

        // Check that weight is not negative
        Integer weight = contentValues.getAsInteger(PetEntry.COLUMN_PET_WEIGHT);
        if (weight == null || weight < 0) {
            throw new IllegalArgumentException("Weight is negative");
        }

        SQLiteDatabase database = mDbHelper.getWritableDatabase();

        long id = database.insert(PetEntry.TABLE_NAME, null, contentValues);

        if (id == -1) {
            Log.e(LOG_TAG, "Failed to insert row for" + uri);
            return null;
        }

        // Once we know the ID of the new row in the table,
        // return the new URI with the ID appended to the end of it
        return ContentUris.withAppendedId(uri, id);
    }

    /**
     * Updates the data at the given selection and selection arguments, with the new ContentValues.
     */
    @Override
    public int update(@NonNull Uri uri, ContentValues contentValues, String selection,
                      String[] selectionArgs) {

        final int match = sUriMatcher.match(uri);
        switch (match) {
            case PETS:
                return updatePet(uri, contentValues, selection, selectionArgs);
            case PET_ID:
                // For the PET_ID code, extract out the ID from the URI,
                // so we know which row to update. Selection will be "_id=?" and selection
                // arguments will be a String array containing the actual ID.
                selection = PetEntry._ID + "=?";
                selectionArgs = new String[]{String.valueOf(ContentUris.parseId(uri))};
                return updatePet(uri, contentValues, selection, selectionArgs);
            default:
                throw new IllegalArgumentException("Update is not supported for " + uri);
        }
    }

    /**
     * Update pets in the database with the given content values. Apply the changes to the rows
     * specified in the selection and selection arguments (which could be 0 or 1 or more pets).
     * Return the number of rows that were successfully updated.
     */
    private int updatePet(Uri uri, ContentValues contentValues, String selection, String[] selectionArgs) {

        // If there are no values to update, then don't try to update the database
        if (contentValues.size() == 0) {
            return 0;
        }

        // Check if name is given
        if (contentValues.containsKey(PetEntry.COLUMN_PET_NAME)) {
            // Check that the name is not null
            String name = contentValues.getAsString(PetEntry.COLUMN_PET_NAME);
            if (name == null || name.isEmpty()) {
                throw new IllegalArgumentException("Pet requires a name");
            }
        }

        // Check if gender is given
        if (contentValues.containsKey(PetEntry.COLUMN_PET_GENDER)) {
            // Check that gender is not valid
            Integer gender = contentValues.getAsInteger(PetEntry.COLUMN_PET_GENDER);
            if (gender == null || !genderIsValid(gender)) {
                throw new IllegalArgumentException("Pet requires valid gender");
            }
        }

        // Check if weight is given
        if (contentValues.containsKey(PetEntry.COLUMN_PET_WEIGHT)) {
            // Check that weight is not negative
            Integer weight = contentValues.getAsInteger(PetEntry.COLUMN_PET_WEIGHT);
            if (weight == null || weight < 0) {
                throw new IllegalArgumentException("Pet requires valid weight");
            }
        }

        SQLiteDatabase database = mDbHelper.getWritableDatabase();

        return database.update(PetEntry.TABLE_NAME, contentValues, selection, selectionArgs);
    }

    /**
     * Delete the data at the given selection and selection arguments.
     */
    @Override
    public int delete(@NonNull Uri uri, String selection, String[] selectionArgs) {
        return 0;
    }

    /**
     * Returns the MIME type of data for the content URI.
     */
    @Override
    public String getType(@NonNull Uri uri) {
        return null;
    }
}

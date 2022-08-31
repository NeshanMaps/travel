package org.neshan.travel.activity;

import android.database.Cursor;
import android.database.SQLException;
import android.database.sqlite.SQLiteDatabase;
import android.os.Bundle;
import android.util.Log;

import androidx.appcompat.app.AppCompatActivity;
import androidx.recyclerview.widget.LinearLayoutManager;
import androidx.recyclerview.widget.RecyclerView;

import org.json.JSONObject;
import org.neshan.common.model.LatLng;
import org.neshan.travel.R;
import org.neshan.travel.adapter.CategoriesAdapter;
import org.neshan.travel.database_helper.AssetDatabaseHelper;
import org.neshan.travel.model.Category;

import java.io.IOException;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import okhttp3.Call;
import okhttp3.Callback;
import okhttp3.OkHttpClient;
import okhttp3.Request;
import okhttp3.Response;

public class MainActivity extends AppCompatActivity {

    private SQLiteDatabase dB;
    private RecyclerView recyclerCategories;
    private List<Category> categoryList;
    private CategoriesAdapter categoriesAdapter;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_main);
    }

    @Override
    protected void onStart() {
        super.onStart();

        initLayoutReferences();
        loadData();
    }

    private void loadData() {
        categoriesAdapter = new CategoriesAdapter();
        categoriesAdapter.setCategories(getCategories());
        recyclerCategories.setAdapter(categoriesAdapter);
    }

    private void initLayoutReferences() {
        initViews();
    }

    private void initViews() {
        recyclerCategories = findViewById(R.id.recycler_categories);
        recyclerCategories.setLayoutManager(new LinearLayoutManager(MainActivity.this, LinearLayoutManager.VERTICAL, false));
    }

    private List<Category> getCategories() {
        AssetDatabaseHelper myDbHelper = new AssetDatabaseHelper(this);

        try {
            myDbHelper.createDataBase();
        } catch (IOException ioe) {
            throw new Error("Unable to create database");
        }

        try {
            dB = myDbHelper.openDataBase();
        } catch (SQLException sqle) {
            sqle.printStackTrace();
        }

        // creating a cursor and query all rows of points table
        Cursor cursor = dB.rawQuery("select * from category", null);

        //reading all points and adding a marker for each one
        List<Category> categories = new ArrayList<>();
        if (cursor.moveToFirst()) {
            while (!cursor.isAfterLast()) {
                Category category = new Category();
                category.setImageAddress(cursor.getString(cursor.getColumnIndex("image_address")))
                        .setId(cursor.getString(cursor.getColumnIndex("id")))
                        .setName(cursor.getString(cursor.getColumnIndex("name")));
                categories.add(category);
                cursor.moveToNext();
            }

        }
        cursor.close();
        return categories;
    }

}
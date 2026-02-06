package com.example.lab5_starter;

import android.os.Bundle;
import android.util.Log;
import android.widget.ArrayAdapter;
import android.widget.Button;
import android.widget.ListView;
import android.widget.Toast;

import androidx.activity.EdgeToEdge;
import androidx.appcompat.app.AlertDialog;
import androidx.appcompat.app.AppCompatActivity;
import androidx.core.graphics.Insets;
import androidx.core.view.ViewCompat;
import androidx.core.view.WindowInsetsCompat;

import com.google.firebase.firestore.CollectionReference;
import com.google.firebase.firestore.DocumentSnapshot;
import com.google.firebase.firestore.FirebaseFirestore;
import com.google.firebase.firestore.ListenerRegistration;

import java.util.ArrayList;
import java.util.Objects;

public class MainActivity extends AppCompatActivity implements CityDialogFragment.CityDialogListener {

    private static final String TAG = "MainActivity";

    private Button addCityButton;
    private ListView cityListView;

    private ArrayList<City> cityArrayList;
    private ArrayAdapter<City> cityArrayAdapter;

    // Firestore
    private FirebaseFirestore db;
    private CollectionReference citiesRef;
    private ListenerRegistration citiesListener;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        EdgeToEdge.enable(this);
        setContentView(R.layout.activity_main);

        ViewCompat.setOnApplyWindowInsetsListener(findViewById(R.id.main), (v, insets) -> {
            Insets systemBars = insets.getInsets(WindowInsetsCompat.Type.systemBars());
            v.setPadding(systemBars.left, systemBars.top, systemBars.right, systemBars.bottom);
            return insets;
        });

        // Views
        addCityButton = findViewById(R.id.buttonAddCity);
        cityListView = findViewById(R.id.listviewCities);

        // Adapter
        cityArrayList = new ArrayList<>();
        cityArrayAdapter = new CityArrayAdapter(this, cityArrayList);
        cityListView.setAdapter(cityArrayAdapter);

        // Firestore init
        db = FirebaseFirestore.getInstance();
        citiesRef = db.collection("cities");

        // 1) Listen to Firestore and update UI
        attachCitiesListener();

        // 2) Add city -> open dialog
        addCityButton.setOnClickListener(view -> {
            CityDialogFragment cityDialogFragment = new CityDialogFragment();
            cityDialogFragment.show(getSupportFragmentManager(), "Add City");
        });

        // 3) Click item -> open details dialog
        cityListView.setOnItemClickListener((adapterView, view, i, l) -> {
            City city = cityArrayAdapter.getItem(i);
            if (city == null) return;
            CityDialogFragment cityDialogFragment = CityDialogFragment.newInstance(city);
            cityDialogFragment.show(getSupportFragmentManager(), "City Details");
        });

        // 4) Long press -> delete
        cityListView.setOnItemLongClickListener((parent, view, position, id) -> {
            City city = cityArrayAdapter.getItem(position);
            if (city == null) return true;

            // Optional confirmation dialog
            new AlertDialog.Builder(this)
                    .setTitle("Delete City")
                    .setMessage("Delete " + city.getName() + " " + city.getProvince() + "?")
                    .setPositiveButton("DELETE", (dialog, which) -> deleteCityFromFirestore(city))
                    .setNegativeButton("Cancel", null)
                    .show();

            return true;
        });
    }

    private void attachCitiesListener() {
        // If already attached, remove first
        if (citiesListener != null) {
            citiesListener.remove();
        }

        citiesListener = citiesRef
                .addSnapshotListener((snapshots, e) -> {
                    if (e != null) {
                        Log.e(TAG, "Firestore listen failed", e);
                        Toast.makeText(this, "Firestore error: " + e.getMessage(), Toast.LENGTH_LONG).show();
                        return;
                    }
                    if (snapshots == null) return;

                    cityArrayList.clear();
                    for (DocumentSnapshot doc : snapshots.getDocuments()) {
                        String id = doc.getId();
                        String name = doc.getString("name");
                        String province = doc.getString("province");

                        if (name == null) name = "";
                        if (province == null) province = "";

                        cityArrayList.add(new City(id, name, province));
                    }
                    cityArrayAdapter.notifyDataSetChanged();
                });
    }

    private void deleteCityFromFirestore(City city) {
        if (city.getId() == null || city.getId().isEmpty()) {
            Toast.makeText(this, "Cannot delete: missing document id", Toast.LENGTH_SHORT).show();
            return;
        }
        citiesRef.document(city.getId())
                .delete()
                .addOnSuccessListener(unused ->
                        Toast.makeText(this, "Deleted", Toast.LENGTH_SHORT).show()
                )
                .addOnFailureListener(err -> {
                    Log.e(TAG, "Delete failed", err);
                    Toast.makeText(this, "Delete failed: " + err.getMessage(), Toast.LENGTH_LONG).show();
                });
    }

    // --- Dialog callbacks ---

    @Override
    public void addCity(City city) {
        // Write to Firestore only; UI updates via snapshot listener
        if (city == null) return;

        citiesRef.add(city.toMap())
                .addOnSuccessListener(docRef ->
                        Toast.makeText(this, "Added", Toast.LENGTH_SHORT).show()
                )
                .addOnFailureListener(err -> {
                    Log.e(TAG, "Add failed", err);
                    Toast.makeText(this, "Add failed: " + err.getMessage(), Toast.LENGTH_LONG).show();
                });
    }

    @Override
    public void updateCity(City city, String title, String year) {
        // Your dialog uses (title, year) but they represent (name, province)
        if (city == null) return;

        city.setName(title);
        city.setProvince(year);

        // Must have id to update
        if (city.getId() == null || city.getId().isEmpty()) {
            Toast.makeText(this, "Cannot update: missing document id", Toast.LENGTH_SHORT).show();
            return;
        }

        citiesRef.document(Objects.requireNonNull(city.getId()))
                .set(city.toMap())
                .addOnSuccessListener(unused ->
                        Toast.makeText(this, "Updated", Toast.LENGTH_SHORT).show()
                )
                .addOnFailureListener(err -> {
                    Log.e(TAG, "Update failed", err);
                    Toast.makeText(this, "Update failed: " + err.getMessage(), Toast.LENGTH_LONG).show();
                });

        // UI will refresh by snapshot listener
    }

    @Override
    protected void onDestroy() {
        super.onDestroy();
        if (citiesListener != null) citiesListener.remove();
    }
}
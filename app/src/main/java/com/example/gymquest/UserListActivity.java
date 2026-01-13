package com.example.gymquest;

import android.os.Bundle;
import android.widget.ArrayAdapter;
import android.widget.ListView;
import android.widget.Toast;
import androidx.appcompat.app.AlertDialog;
import androidx.appcompat.app.AppCompatActivity;
import com.google.firebase.firestore.FirebaseFirestore;
import com.google.firebase.firestore.QueryDocumentSnapshot;
import java.util.ArrayList;
import java.util.List;

public class UserListActivity extends AppCompatActivity {

    private ListView listView;
    private ArrayAdapter<String> adapter;
    private List<String> userNames = new ArrayList<>();
    private List<String> userIds = new ArrayList<>();

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        // We will reuse the simple layout for now, or create a simple XML with a ListView
        setContentView(R.layout.activity_user_list);

        listView = findViewById(R.id.userListView);
        adapter = new ArrayAdapter<>(this, android.R.layout.simple_list_item_1, userNames);
        listView.setAdapter(adapter);

        loadUsers();

        // Handle Click to Delete
        listView.setOnItemClickListener((parent, view, position, id) -> {
            new AlertDialog.Builder(this)
                    .setTitle("Delete User")
                    .setMessage("Delete this user data? (Note: This deletes database info, not the login account)")
                    .setPositiveButton("Delete", (dialog, which) -> deleteUser(position))
                    .setNegativeButton("Cancel", null)
                    .show();
        });
    }

    private void loadUsers() {
        FirebaseFirestore.getInstance().collection("Users")
                .get()
                .addOnSuccessListener(snapshots -> {
                    userNames.clear();
                    userIds.clear();
                    for (QueryDocumentSnapshot doc : snapshots) {
                        // Show Email or ID
                        String display = doc.getId();
                        if(doc.contains("email")) display = doc.getString("email");

                        userNames.add(display);
                        userIds.add(doc.getId());
                    }
                    adapter.notifyDataSetChanged();
                });
    }

    private void deleteUser(int position) {
        String uid = userIds.get(position);
        FirebaseFirestore.getInstance().collection("Users").document(uid)
                .delete()
                .addOnSuccessListener(v -> {
                    Toast.makeText(this, "User Data Deleted", Toast.LENGTH_SHORT).show();
                    loadUsers(); // Refresh
                });
    }
}
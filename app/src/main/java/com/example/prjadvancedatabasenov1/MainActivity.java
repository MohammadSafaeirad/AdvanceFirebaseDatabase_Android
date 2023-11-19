package com.example.prjadvancedatabasenov1;

import androidx.activity.result.ActivityResult;
import androidx.activity.result.ActivityResultCallback;
import androidx.activity.result.ActivityResultLauncher;
import androidx.activity.result.contract.ActivityResultContracts;
import androidx.annotation.NonNull;
import androidx.appcompat.app.AppCompatActivity;

import android.app.ProgressDialog;
import android.content.Intent;
import android.graphics.Bitmap;
import android.net.Uri;
import android.os.Bundle;
import android.provider.MediaStore;
import android.util.Log;
import android.view.View;
import android.widget.Button;
import android.widget.EditText;
import android.widget.ImageView;
import android.widget.Toast;

import com.google.android.gms.tasks.OnCompleteListener;
import com.google.android.gms.tasks.OnFailureListener;
import com.google.android.gms.tasks.OnSuccessListener;
import com.google.android.gms.tasks.Task;
import com.google.firebase.database.DataSnapshot;
import com.google.firebase.database.DatabaseError;
import com.google.firebase.database.DatabaseReference;
import com.google.firebase.database.FirebaseDatabase;
import com.google.firebase.database.ValueEventListener;
import com.google.firebase.storage.FirebaseStorage;
import com.google.firebase.storage.StorageReference;
import com.squareup.picasso.Picasso;

import java.util.ArrayList;
import java.util.Map;
import java.util.UUID;

import model.Car;
import model.Person;

public class MainActivity extends AppCompatActivity implements View.OnClickListener, OnSuccessListener, OnFailureListener, OnCompleteListener, ValueEventListener {

    EditText edId;
    Button btnAdd,btnFind,btnBrowse,btnUpload;
    ImageView imPhoto;

    // For Realtime database
    DatabaseReference personDatabase;
    // For Firebase Storage
    FirebaseStorage storage;
    StorageReference storageReference,sRef;
    // The path of the image in the device
    Uri filePath;
    // For the progression of uploading the file to firebase storage
    ProgressDialog progressDialog;
    // For receiving results (image) when we click the button browse
    ActivityResultLauncher aResL;

    String fileUrl;


    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_main);
        initialize();
    }

    private void initialize() {

        edId = findViewById(R.id.edId);
        imPhoto = findViewById(R.id.imPhoto);
        btnAdd = findViewById(R.id.btnAdd);
        btnBrowse = findViewById(R.id.btnBrowse);
        btnUpload = findViewById(R.id.btnUpload);
        btnFind = findViewById(R.id.btnFind);
        btnAdd.setOnClickListener(this);
        btnBrowse.setOnClickListener(this);
        btnUpload.setOnClickListener(this);
        btnFind.setOnClickListener(this);

        // Initialization of objects to Firebase database & Storage
        personDatabase = FirebaseDatabase.getInstance().getReference("Person");
        storage = FirebaseStorage.getInstance();
        storageReference = storage.getReference();
        // Registration of Activity Result Launcher
        runActivityResLauncher();

    }

    private void runActivityResLauncher() {
        aResL = registerForActivityResult(
                new ActivityResultContracts.StartActivityForResult(),
                new ActivityResultCallback<ActivityResult>() {
                    @Override
                    public void onActivityResult(ActivityResult o) {
                        getPhoto(o);

                    }
                }
        );
    }

    private void getPhoto(ActivityResult result) {
        if (result.getResultCode()==RESULT_OK){
            filePath = result.getData().getData();
            try {
                Bitmap bitmap =
                        MediaStore.Images.Media.getBitmap(getContentResolver(), filePath);
                imPhoto.setImageBitmap(bitmap);
            } catch (Exception e) {
                Toast.makeText(this, e.getMessage(), Toast.LENGTH_LONG).show();
            }
        }
    }

    @Override
    public void onClick(View v) {
        int id = v.getId();
        if(id==R.id.btnAdd)
            addPerson();
        if(id== R.id.btnFind)
            findPerson();
        if(id== R.id.btnBrowse)
            selectPhoto();
        if(id== R.id.btnUpload)
            uploadPhoto();
    }

    private void uploadPhoto() {
        if (filePath != null) {
            progressDialog = new ProgressDialog(this);
            progressDialog.setTitle("Uploading photo in progress ...");
            progressDialog.show();

            sRef = storageReference.child("images/"+ UUID.randomUUID());
            sRef.putFile(filePath).addOnSuccessListener(this);
            sRef.putFile(filePath).addOnFailureListener(this);
        } else {
            Toast.makeText(this, "There is no photo to upload", Toast.LENGTH_LONG).show();
        }
    }

    @Override
    public void onFailure(@NonNull Exception e) {
        Toast.makeText(this, e.getMessage(), Toast.LENGTH_LONG).show();
        progressDialog.dismiss();
    }

    @Override
    public void onSuccess(Object o) {
        Toast.makeText(this, "The photo has been uploaded successfully", Toast.LENGTH_LONG).show();
        progressDialog.dismiss();
        sRef.getDownloadUrl().addOnCompleteListener(this);
    }

    @Override
    public void onComplete(@NonNull Task task) {
        fileUrl = task.getResult().toString();
        Log.d("ADV_FIREBASE", fileUrl);
    }

    private void selectPhoto() {
        Intent intent = new Intent();
        intent.setType("image/*");
        intent.setAction(Intent.ACTION_GET_CONTENT);
        aResL.launch(Intent.createChooser(intent, "Select one photo"));
    }

    private void findPerson() {
        String id = edId.getText().toString();
        personDatabase.child(id).addValueEventListener(this);
    }

    @Override
    public void onDataChange(@NonNull DataSnapshot snapshot) {
        if (snapshot.exists()) {
            String name = snapshot.child("name").getValue().toString();

            ArrayList<String> hobbies = (ArrayList) snapshot.child("hobbies").getValue();
            Log.d("ADV_FIREBASE", hobbies.toString());

            Map car = (Map)snapshot.child("car").getValue();

            Log.d("ADV_FIREBASE", "Car id " + car.get("id"));
            Log.d("ADV_FIREBASE", "Car brand " + car.get("brand"));
            Log.d("ADV_FIREBASE", "Car model " + car.get("model"));

            //get the photo

            String urlPhoto = snapshot.child("photo").getValue().toString();
            Picasso.with(this).load(urlPhoto).placeholder(R.drawable.temp_image).into(imPhoto);

        }
        else {
            Toast.makeText(this, "No document found!", Toast.LENGTH_LONG).show();
        }
    }

    @Override
    public void onCancelled(@NonNull DatabaseError error) {

    }
    private void addPerson() {
        ArrayList<String> hobbies = new ArrayList<String>();
        hobbies.add("Soccer");
        hobbies.add("Handball");
        hobbies.add("Music");

        Car car = new Car("M300", "Mazda", "Mazda6");
        Person p = new Person(200, "Richard", fileUrl, car, hobbies);
        personDatabase.child("200").setValue(p);
        Toast.makeText(this, "One document has been added successfully", Toast.LENGTH_LONG).show();
    }



}
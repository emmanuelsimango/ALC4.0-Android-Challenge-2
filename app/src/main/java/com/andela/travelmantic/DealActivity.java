package com.andela.travelmantic;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.appcompat.app.AppCompatActivity;

import android.content.Intent;
import android.content.res.Resources;
import android.net.Uri;
import android.os.Bundle;
import android.util.Log;
import android.view.Menu;
import android.view.MenuInflater;
import android.view.MenuItem;
import android.view.View;
import android.widget.Button;
import android.widget.EditText;
import android.widget.ImageView;
import android.widget.Toast;

import com.google.android.gms.tasks.Continuation;
import com.google.android.gms.tasks.OnCompleteListener;
import com.google.android.gms.tasks.OnSuccessListener;
import com.google.android.gms.tasks.Task;
import com.google.firebase.database.DatabaseReference;
import com.google.firebase.database.FirebaseDatabase;
import com.google.firebase.storage.StorageReference;
import com.google.firebase.storage.UploadTask;
import com.squareup.picasso.Picasso;


public class DealActivity extends AppCompatActivity {

   private FirebaseDatabase mFirebaseDatabase;
   private DatabaseReference mDatabaseReference;
   private static final int PICTURE_RESULT = 42;
   EditText txtTitle;
   EditText txtDescription;
   EditText txtPrice;
    private TravelDeal deal;
    ImageView imageView;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_deal);
        mFirebaseDatabase = FirebaseUtil.mFirebaseDatabase;
        mDatabaseReference = FirebaseUtil.mDatabaseReference;
        txtTitle = findViewById(R.id.txtTitle);
        txtPrice = findViewById(R.id.txtPrice);
        txtDescription = findViewById(R.id.txtDescription);
        imageView = findViewById(R.id.imageDeal);

        final Intent intent  = getIntent();
        TravelDeal deal = (TravelDeal)intent.getSerializableExtra("Deal");
        if (deal==null){
            deal = new TravelDeal();
        }
        this.deal = deal;
        txtTitle.setText(deal.getTitle());
        txtPrice.setText(deal.getPrice());
        txtDescription.setText(deal.getDescription());
        Button btnImage = findViewById(R.id.btnImage);
        showImage(deal.getImageUrl());
        btnImage.setVisibility(FirebaseUtil.isAdmin?View.VISIBLE:View.INVISIBLE);
        btnImage.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View view) {
                Intent intent1 = new Intent(Intent.ACTION_GET_CONTENT);
                intent1.setType("image/jpeg");
                intent1.putExtra(Intent.EXTRA_LOCAL_ONLY,true);
                startActivityForResult(intent1.createChooser(intent1,"Insert Picture"),PICTURE_RESULT);
            }
        });

    }

    @Override
    public boolean onCreateOptionsMenu(Menu menu) {
        MenuInflater inflater = getMenuInflater();
        inflater.inflate(R.menu.save_menu,menu);

        menu.findItem(R.id.save_menu).setVisible(FirebaseUtil.isAdmin);
        menu.findItem(R.id.delete_menu).setVisible(FirebaseUtil.isAdmin);
        enableEditText(FirebaseUtil.isAdmin);
        return true;
    }

    @Override
    public boolean onOptionsItemSelected(@NonNull MenuItem item) {
//        Toast.makeText(this,"Deal Saved",Toast.LENGTH_LONG).show();
        switch (item.getItemId()){
            case R.id.save_menu:
                saveDeal();
                Toast.makeText(this,"Deal Saved",Toast.LENGTH_LONG).show();
                clean();
                backToList();
                return  true;
            case R.id.delete_menu:
                deleteDeal();
                Toast.makeText(this,"Deal Deleted",Toast.LENGTH_LONG).show();
                backToList();
                return true;
            default:
                return super.onOptionsItemSelected(item);
        }


    }

    private void clean() {
        txtTitle.setText("");
        txtDescription.setText("");
        txtPrice.setText("");

        txtTitle.requestFocus();
    }

    private void saveDeal() {
        deal.setTitle(txtTitle.getText().toString());
        deal.setPrice(txtPrice.getText().toString());
        deal.setDescription(txtDescription.getText().toString());
        if (deal.getId()==null){
            mDatabaseReference.push().setValue(deal);
        }else{
            mDatabaseReference.child(deal.getId()).setValue(deal);
        }
    }

    private void backToList(){
        Intent intent = new Intent(this,ListActivity.class);
        startActivity(intent);
    }

    private void deleteDeal(){
        if (deal==null){
            Toast.makeText(this,"Please save deal before delete",Toast.LENGTH_LONG).show();
        }else  {
            mDatabaseReference.child(deal.getId()).removeValue();
        }
    }

    public void enableEditText(boolean isEnabled){
        txtTitle.setEnabled(isEnabled);
        txtDescription.setEnabled(isEnabled);
        txtPrice.setEnabled(isEnabled);
    }

    @Override
    protected void onActivityResult(int requestCode, int resultCode, @Nullable Intent data) {
        super.onActivityResult(requestCode, resultCode, data);
        if (requestCode==PICTURE_RESULT&&resultCode==RESULT_OK){
            assert data != null;
            Uri imageUri = data.getData();
            final StorageReference ref = FirebaseUtil.mStorageRef.child(imageUri.getLastPathSegment());
//            UploadTask uploadTask = ref.putFile(imageUri);
//            UploadTask uploadTask = ref.putFile(imageUri);

            Task<Uri> urlTask = ref.putFile(imageUri).continueWithTask(new Continuation<UploadTask.TaskSnapshot, Task<Uri>>() {
                @Override
                public Task<Uri> then(@NonNull Task<UploadTask.TaskSnapshot> task) throws Exception {
                    if (!task.isSuccessful()) {
//                        throw task.getException();
                        Toast.makeText(getBaseContext(),"Failed to upload image",Toast.LENGTH_LONG).show();
                    }

                    // Continue with the task to get the download URL
                    return ref.getDownloadUrl();
                }
            }).addOnCompleteListener(new OnCompleteListener<Uri>() {
                @Override
                public void onComplete(@NonNull Task<Uri> task) {
                    if (task.isSuccessful()) {
                        Uri downloadUri = task.getResult();
                        deal.setImageUrl(downloadUri.toString());
                        showImage(downloadUri.toString());
                    } else {
                        // Handle failures
                        // ...
                        Toast.makeText(getBaseContext(),"Failed to upload image",Toast.LENGTH_LONG).show();
                    }
                }
            });

            if(urlTask.isSuccessful()){
                Toast.makeText(this, "Succesfull", Toast.LENGTH_LONG).show();

            }

        }
    }

    private void showImage(String url){

        if(url!=null && !url.isEmpty()){
            int width = Resources.getSystem().getDisplayMetrics().widthPixels;
            Picasso.get()
                    .load(url)
                    .resize(width,width*2/3)
                    .centerCrop()
                    .into(imageView);
        }
    }
}

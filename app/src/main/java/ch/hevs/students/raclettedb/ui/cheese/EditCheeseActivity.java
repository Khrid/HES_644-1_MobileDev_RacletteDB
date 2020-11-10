package ch.hevs.students.raclettedb.ui.cheese;

import android.content.ActivityNotFoundException;
import android.content.Intent;
import android.content.SharedPreferences;
import android.graphics.Bitmap;
import android.graphics.BitmapFactory;
import android.net.Uri;
import android.os.Bundle;
import android.os.Environment;
import android.provider.MediaStore;
import android.text.TextUtils;
import android.util.Log;
import android.view.Menu;
import android.widget.Button;
import android.widget.EditText;
import android.widget.ImageView;
import android.widget.Spinner;
import android.widget.TextView;
import android.widget.Toast;

import androidx.annotation.Nullable;
import androidx.core.content.FileProvider;
import androidx.lifecycle.ViewModelProviders;

import java.io.File;
import java.io.IOException;
import java.text.SimpleDateFormat;
import java.util.ArrayList;
import java.util.Date;
import java.util.List;

import ch.hevs.students.raclettedb.BaseApp;
import ch.hevs.students.raclettedb.R;
import ch.hevs.students.raclettedb.adapter.ListAdapter;
import ch.hevs.students.raclettedb.database.entity.CheeseEntity;
import ch.hevs.students.raclettedb.database.entity.ShielingEntity;
import ch.hevs.students.raclettedb.ui.BaseActivity;
import ch.hevs.students.raclettedb.util.OnAsyncEventListener;
import ch.hevs.students.raclettedb.viewmodel.cheese.CheeseViewModel;
import ch.hevs.students.raclettedb.viewmodel.shieling.ShielingListViewModel;
import ch.hevs.students.raclettedb.viewmodel.shieling.ShielingViewModel;

public class EditCheeseActivity extends BaseActivity {

    private static final String TAG = "TAG-"+ BaseApp.APP_NAME+"-EditCheeseActivity";

    private Long cheeseId;
    private CheeseEntity cheese;
    private boolean isEditMode;
    private Toast toast;
    private EditText etCheeseName;
    private EditText etCheeseDescription;
    private EditText etCheeseType;
    private TextView tvEditCheeseTitle;
    private Spinner spinCheeseShieling;
    private Button btSaveCheese;
    private ImageView ivCheese;

    private String currentPhotoPath = BaseActivity.IMAGE_CHEESE_DEFAULT;

    private CheeseViewModel cheeseViewModel;
    private ShielingViewModel shielingViewModel;
    private ShielingListViewModel shielingListViewModel;
    private ListAdapter<ShielingEntity> adapterShieling;


    static SharedPreferences settings;
    static SharedPreferences.Editor editor;


    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        getLayoutInflater().inflate(R.layout.activity_edit_cheese, frameLayout);

        settings = getSharedPreferences(BaseActivity.PREFS_NAME, 0);
        editor = settings.edit();

        navigationView.setCheckedItem(position);

        tvEditCheeseTitle = findViewById(R.id.tvEditCheeseTitle);
        etCheeseName = findViewById(R.id.etCheeseName);
        etCheeseName.requestFocus();
        etCheeseDescription = findViewById(R.id.etCheeseDescription);
        etCheeseType = findViewById(R.id.etCheeseType);

        btSaveCheese = findViewById(R.id.btSaveCheese);
        btSaveCheese.setOnClickListener(view -> {
            saveChanges(etCheeseName.getText().toString(),etCheeseDescription.getText().toString(), etCheeseType.getText().toString(),((ShielingEntity)spinCheeseShieling.getSelectedItem()).getId(), ivCheese.getTag().toString());

            onBackPressed();
            toast.show();
        });



        cheeseId = getIntent().getLongExtra("cheeseId", 0L);
        if (cheeseId == 0L) {
            setTitle(R.string.empty);
            tvEditCheeseTitle.setText(R.string.cheese_new_title);
            btSaveCheese.setText(R.string.save);
            toast = Toast.makeText(this, getString(R.string.cheese_new_created), Toast.LENGTH_LONG);
            isEditMode = false;
        } else {
            setTitle(R.string.empty);
            btSaveCheese.setText(R.string.update);
            toast = Toast.makeText(this, getString(R.string.cheese_edit_edited), Toast.LENGTH_LONG);
            isEditMode = true;
        }

        ivCheese = findViewById(R.id.ivEditCheesePhoto);
        ivCheese.setOnClickListener(v -> takePicture());

        ivCheese.setImageResource(R.drawable.placeholder_cheese);

        setupShielingSpinner();
        setupViewModels();
    }

    private boolean removePicture() {
        currentPhotoPath = "";
        ivCheese.setImageResource(R.drawable.placeholder_cheese);
        Toast.makeText(this, getString(R.string.cheese_picture_removed), Toast.LENGTH_LONG).show();
        return true;
    }

    private void takePicture() {
        Intent takePictureIntent = new Intent(MediaStore.ACTION_IMAGE_CAPTURE);
        // Ensure that there's a camera activity to handle the intent
        if (takePictureIntent.resolveActivity(getPackageManager()) != null) {
            // Create the File where the photo should go
            File photoFile = null;
            try {
                photoFile = createImageFile();
            } catch (IOException ex) {
                // Error occurred while creating the File
            //...
            }
            // Continue only if the File was successfully created
            if (photoFile != null) {
                Uri photoURI = FileProvider.getUriForFile(this,
                        "ch.hevs.students.raclettedb.fileprovider",
                        photoFile);
                takePictureIntent.putExtra(MediaStore.EXTRA_OUTPUT, photoURI);
                startActivityForResult(takePictureIntent, 1);
            }
        }
    }

    @Override
    protected void onActivityResult(int requestCode, int resultCode, @Nullable Intent data) {
        super.onActivityResult(requestCode, resultCode, data);
        if(resultCode == RESULT_OK) {
            //Bitmap thumbnail = (Bitmap) data.getExtras().get(MediaStore.EXTRA_OUTPUT);
            //ivCheese.setImageBitmap(thumbnail);

            File imageFile = getImageFile();
            Bitmap bitmap = BitmapFactory.decodeFile(imageFile.getAbsolutePath());
            currentPhotoPath = imageFile.getAbsolutePath();
            ivCheese.setTag(currentPhotoPath);
            ivCheese.setImageBitmap(bitmap);
        }
    }

    private void setupViewModels() {

        ShielingListViewModel.Factory shielingFactory = new ShielingListViewModel.Factory(
                getApplication());
        shielingListViewModel = ViewModelProviders.of(this, shielingFactory).get(ShielingListViewModel.class);
        shielingListViewModel.getShielings().observe(this, shielingEntities -> {
            if (shielingEntities != null) {

                updateShielingSpinner(shielingEntities);
            }
        });

        CheeseViewModel.Factory cheeseFactory = new CheeseViewModel.Factory(
                getApplication(), cheeseId);
        cheeseViewModel = ViewModelProviders.of(this, cheeseFactory).get(CheeseViewModel.class);
        if (isEditMode) {
            cheeseViewModel.getCheese().observe(this, cheeseEntity -> {
                if (cheeseEntity != null) {
                    cheese = cheeseEntity;
                    etCheeseName.setText(cheese.getName());
                    etCheeseDescription.setText(cheese.getDescription());
                    etCheeseType.setText(cheese.getType());

                    if(!TextUtils.isEmpty(cheese.getImagePath())) {
                        if(!cheese.getImagePath().equals(BaseActivity.IMAGE_CHEESE_DEFAULT)) {
                            Bitmap bitmap = BitmapFactory.decodeFile(cheese.getImagePath());
                            ivCheese.setImageBitmap(bitmap);
                            ivCheese.setTag(cheese.getImagePath());
                            ivCheese.setOnLongClickListener(v -> removePicture());
                        }
                    }
                    // TODO A faire comme ça ?
                    ShielingViewModel.Factory factory = new ShielingViewModel.Factory(
                            getApplication(), cheese.getShieling());
                    shielingViewModel = ViewModelProviders.of(this, factory).get(ShielingViewModel.class);
                    shielingViewModel.getShieling().observe(this, shielingEntity -> {
                        if (shielingEntity != null) {
                            spinCheeseShieling.setSelection(adapterShieling.getPosition(shielingEntity));
                        }
                    });
                }
            });
        }
    }


    @Override
    protected void onResume() {
        if(!settings.getBoolean(BaseActivity.PREFS_IS_ADMIN, false)) {
            finish();
        }
        super.onResume();
    }

    @Override
    public boolean onPrepareOptionsMenu(Menu menu) {
        return false;
    }


    private void setupShielingSpinner() {
        spinCheeseShieling = findViewById(R.id.spinCheeseShieling);
        adapterShieling = new ListAdapter<>(this, R.layout.row_shieling, new ArrayList<>());
        spinCheeseShieling.setAdapter(adapterShieling);
    }

    private void updateShielingSpinner(List<ShielingEntity> shielings) {
        adapterShieling.updateData(new ArrayList<>(shielings));
    }


    private void saveChanges(String cheeseName, String description, String cheeseType, Long Shieling, String imagePath) {
        if (isEditMode) {
            if(!"".equals(cheeseName)) {
                cheese.setName(cheeseName);
                cheese.setDescription(description);
                cheese.setType(cheeseType);
                cheese.setShieling(Shieling);
                cheese.setImagePath(imagePath);
                cheeseViewModel.updateCheese(cheese, new OnAsyncEventListener() {
                    @Override
                    public void onSuccess() {
                        Log.d(TAG, "updateCheese: success");
                    }

                    @Override
                    public void onFailure(Exception e) {
                        Log.d(TAG, "updateCheese: failure", e);
                    }
                });
            }
        } else {
            CheeseEntity newCheese = new CheeseEntity();
            newCheese.setName(cheeseName);
            newCheese.setDescription(description);
            newCheese.setType(cheeseType);
            newCheese.setShieling(Shieling);
            //newCheese.setImagePath(imagePath);
            cheeseViewModel.createCheese(newCheese, new OnAsyncEventListener() {
                @Override
                public void onSuccess() {
                    Log.d(TAG, "createCheese: success");
                }

                @Override
                public void onFailure(Exception e) {
                    Log.d(TAG, "createCheese: failure", e);
                }
            });
        }
    }

    private File createImageFile() throws IOException {
        // Create an image file name
        String timeStamp = new SimpleDateFormat("yyyyMMdd_HHmmss").format(new Date());
        String imageFileName = "JPEG_" + timeStamp + "_";
        File storageDir = getExternalFilesDir(Environment.DIRECTORY_PICTURES);
        File image = File.createTempFile(
                imageFileName,  /* prefix */
                ".jpg",         /* suffix */
                storageDir      /* directory */
        );

        // Save a file: path for use with ACTION_VIEW intents
        currentPhotoPath = image.getAbsolutePath();
        return image;
    }

    private File getImageFile() {
        String Path = Environment.getExternalStorageDirectory() + "/MyApp";
        File storageDir = getExternalFilesDir(Environment.DIRECTORY_PICTURES);
        File f = new File(Path);
        File imageFiles[] = storageDir.listFiles();

        if (imageFiles == null || imageFiles.length == 0) {
            return null;
        }

        File lastModifiedFile = imageFiles[0];
        for (int i = 1; i < imageFiles.length; i++) {
            if (lastModifiedFile.lastModified() < imageFiles[i].lastModified()) {
                lastModifiedFile = imageFiles[i];
            }
        }
        return lastModifiedFile;
    }
}

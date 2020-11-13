package ch.hevs.students.raclettedb.ui.cheese;

import android.Manifest;
import android.app.AlertDialog;
import android.content.ActivityNotFoundException;
import android.content.DialogInterface;
import android.content.Intent;
import android.content.SharedPreferences;
import android.content.pm.PackageManager;
import android.database.Cursor;
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

import java.io.ByteArrayOutputStream;
import java.io.File;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.InputStream;
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
import ch.hevs.students.raclettedb.util.MediaUtils;
import ch.hevs.students.raclettedb.util.OnAsyncEventListener;
import ch.hevs.students.raclettedb.viewmodel.cheese.CheeseViewModel;
import ch.hevs.students.raclettedb.viewmodel.shieling.ShielingListViewModel;
import ch.hevs.students.raclettedb.viewmodel.shieling.ShielingViewModel;

public class EditCheeseActivity extends BaseActivity {

    private static final String TAG = "TAG-"+ BaseApp.APP_NAME+"-EditCheeseActivity";

    private Long cheeseId;
    private CheeseEntity cheese = new CheeseEntity();
    private boolean isEditMode;
    private Toast toast;
    private EditText etCheeseName;
    private EditText etCheeseDescription;
    private EditText etCheeseType;
    private TextView tvEditCheeseTitle;
    private Spinner spinCheeseShieling;
    private Button btSaveCheese;
    private ImageView ivCheese;
    private File destination = null;
    private InputStream inputStreamImg;
    private String imgPath = null;

    private String currentPhotoPath = BaseActivity.IMAGE_CHEESE_DEFAULT;

    private CheeseViewModel cheeseViewModel;
    private ShielingViewModel shielingViewModel;
    private ShielingListViewModel shielingListViewModel;
    private ListAdapter<ShielingEntity> adapterShieling;


    static SharedPreferences settings;
    static SharedPreferences.Editor editor;

    MediaUtils mediaUtils;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        getLayoutInflater().inflate(R.layout.activity_edit_cheese, frameLayout);
        mediaUtils = new MediaUtils(this);

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
            saveChanges(etCheeseName.getText().toString(),etCheeseDescription.getText().toString(), etCheeseType.getText().toString(),((ShielingEntity)spinCheeseShieling.getSelectedItem()).getId(), cheese.getImagePath());

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
        ivCheese.setOnClickListener(v -> mediaUtils.selectImage());

        ivCheese.setImageResource(R.drawable.placeholder_cheese);

        setupShielingSpinner();
        setupViewModels();
    }

    private boolean removePicture() {
        currentPhotoPath = "";
        ivCheese.setImageResource(R.drawable.placeholder_cheese);
        cheese.setImagePath(BaseActivity.IMAGE_CHEESE_DEFAULT);
        Toast.makeText(this, getString(R.string.cheese_picture_removed), Toast.LENGTH_LONG).show();
        return true;
    }

    @Override
    protected void onActivityResult(int requestCode, int resultCode, @Nullable Intent data) {
        Bitmap bitmap;
        super.onActivityResult(requestCode, resultCode, data);
        if(resultCode == RESULT_OK) {
            //Bitmap thumbnail = (Bitmap) data.getExtras().get(MediaStore.EXTRA_OUTPUT);
            //ivCheese.setImageBitmap(thumbnail);
            if(requestCode == 1) {
                File imageFile = mediaUtils.getImageFile();
                bitmap = BitmapFactory.decodeFile(imageFile.getAbsolutePath());
                currentPhotoPath = imageFile.getAbsolutePath();
                cheese.setImagePath(imageFile.getAbsolutePath());
                ivCheese.setTag(currentPhotoPath);
                ivCheese.setImageBitmap(bitmap);
            } else if (requestCode == 2) {
                Uri selectedImage = data.getData();
                try {
                    bitmap = MediaStore.Images.Media.getBitmap(this.getContentResolver(), selectedImage);
                    ByteArrayOutputStream bytes = new ByteArrayOutputStream();
                    bitmap.compress(Bitmap.CompressFormat.JPEG, 50, bytes);
                    Log.e(TAG, "Pick from Gallery::>>> ");

                    File f = mediaUtils.copyToLocalStorage(bitmap);

                    /*imgPath = getRealPathFromURI(selectedImage);
                    destination = new File(imgPath.toString());*/
                    cheese.setImagePath(f.getAbsolutePath());
                    ivCheese.setImageBitmap(bitmap);

                } catch (Exception e) {
                    e.printStackTrace();
                }
            }
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
            newCheese.setImagePath(imagePath);
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

}

package devliving.online.cvscannersample;

import android.Manifest;
import android.content.DialogInterface;
import android.content.Intent;
import android.content.pm.PackageManager;
import android.net.Uri;
import android.os.Build;
import android.os.Bundle;
import android.support.annotation.NonNull;
import android.support.v7.app.AlertDialog;
import android.support.v7.app.AppCompatActivity;
import android.support.v7.widget.LinearLayoutManager;
import android.support.v7.widget.RecyclerView;
import android.support.v7.widget.Toolbar;
import android.util.Log;
import android.view.Menu;
import android.view.MenuItem;
import android.view.View;

import com.getbase.floatingactionbutton.FloatingActionButton;

import java.io.File;
import java.io.IOException;

import devliving.online.cvscanner.CVScanner;
import devliving.online.cvscanner.util.Util;

public class MainActivity extends AppCompatActivity {

    final int REQ_SCAN = 11;

    private static final int REQUEST_TAKE_PHOTO = 121;
    private static final int REQUEST_PICK_PHOTO = 123;
    private static final int REQ_CROP_IMAGE = 122;
    private static final int REQ_PERMISSIONS = 120;

    Uri currentPhotoUri = null;

    RecyclerView list;
    ImageAdapter mAdapter;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_main);
        Toolbar toolbar = (Toolbar) findViewById(R.id.toolbar);
        setSupportActionBar(toolbar);

        list = (RecyclerView) findViewById(R.id.image_list);

        list.setLayoutManager(new LinearLayoutManager(this));

        mAdapter = new ImageAdapter();
        list.setAdapter(mAdapter);

        FloatingActionButton fabScan = findViewById(R.id.action_scan);
        fabScan.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View view) {
                new AlertDialog.Builder(MainActivity.this)
                        .setMessage("Are you scanning a Passport?")
                        .setPositiveButton("Yes", new DialogInterface.OnClickListener() {
                            @Override
                            public void onClick(DialogInterface dialog, int which) {
                                startScannerIntent(true);
                            }
                        })
                        .setNegativeButton("No", new DialogInterface.OnClickListener() {
                            @Override
                            public void onClick(DialogInterface dialog, int which) {
                                startScannerIntent(false);
                            }
                        }).show();
            }
        });

        FloatingActionButton fabCrop = findViewById(R.id.action_crop);
        fabCrop.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View view) {
                new AlertDialog.Builder(MainActivity.this)
                        .setMessage("Choose photo to crop")
                        .setPositiveButton("With Camera", new DialogInterface.OnClickListener() {
                            @Override
                            public void onClick(DialogInterface dialog, int which) {
                            startCameraIntent();
                            }
                        })
                        .setNeutralButton("From Device", new DialogInterface.OnClickListener() {
                            @Override
                            public void onClick(DialogInterface dialogInterface, int i) {
                                startImagePickerIntent();
                            }
                        })
                        .show();
            }
        });

    }

    void startScannerIntent(boolean isPassport){
        CVScanner.startScanner(this, isPassport, REQ_SCAN);
    }

    void startCameraIntent(){
        if(Build.VERSION.SDK_INT < Build.VERSION_CODES.M ||
                checkSelfPermission(Manifest.permission.WRITE_EXTERNAL_STORAGE) == PackageManager.PERMISSION_GRANTED) {
            try {
                currentPhotoUri = CVScanner.startCameraIntent(this, REQUEST_TAKE_PHOTO);
            } catch (IOException e) {
                e.printStackTrace();
            }
        }
        else{
            requestPermissions(new String[]{Manifest.permission.WRITE_EXTERNAL_STORAGE}, REQ_PERMISSIONS);
        }
    }

    void startImagePickerIntent(){
        Intent intent = new Intent(Intent.ACTION_GET_CONTENT);
        intent.setType("image/*");
        startActivityForResult(Intent.createChooser(intent, "Pick an image"), REQUEST_PICK_PHOTO);
    }

    void startImageCropIntent(){
        CVScanner.startManualCropper(this, currentPhotoUri, REQ_CROP_IMAGE);
    }

    @Override
    public boolean onCreateOptionsMenu(Menu menu) {
        // Inflate the menu; this adds items to the action bar if it is present.
        getMenuInflater().inflate(R.menu.menu_main, menu);
        return true;
    }

    @Override
    public boolean onOptionsItemSelected(MenuItem item) {
        // Handle action bar item clicks here. The action bar will
        // automatically handle clicks on the Home/Up button, so long
        // as you specify a parent activity in AndroidManifest.xml.
        int id = item.getItemId();

        //noinspection SimplifiableIfStatement
        if (id == R.id.action_step_by_step) {
            new AlertDialog.Builder(this)
                    .setTitle("Choose one")
                    .setItems(new String[]{"Document (ID, license etc.)" ,
                            "Passport - HoughLines algorithm",
                            "Passport - MRZ based algorithm",
                        "Passport - MRZ based retrial"}, new DialogInterface.OnClickListener() {
                        @Override
                        public void onClick(DialogInterface dialog, int which) {
                            Intent intent = new Intent(MainActivity.this, StepByStepTestActivity.class);
                            intent.putExtra(StepByStepTestActivity.EXTRA_SCAN_TYPE, StepByStepTestActivity.CVCommand.values()[which]);
                            startActivity(intent);
                        }
                    })
                    .setCancelable(true)
                    .show();
            return true;
        }

        return super.onOptionsItemSelected(item);
    }

    @Override
    protected void onActivityResult(int requestCode, int resultCode, Intent data) {
        super.onActivityResult(requestCode, resultCode, data);

        Log.d("MAIN", "got activity result, code: " + requestCode + ", result: " + (resultCode == RESULT_OK));

        if(resultCode == RESULT_OK){
            switch (requestCode){
                case REQ_SCAN:
                case REQ_CROP_IMAGE:
                    Log.d("MAIN", "got intent data");
                    if(data != null && data.getExtras() != null){
                        String path = data.getStringExtra(CVScanner.RESULT_IMAGE_PATH);
                        File file = new File(path);
                        Uri imageUri = Util.getUriForFile(this, file);
                        if(imageUri != null) mAdapter.add(imageUri);
                        Log.d("MAIN", "added: " + imageUri);
                    }
                    break;
                case REQUEST_TAKE_PHOTO:
                    startImageCropIntent();
                    break;

                case REQUEST_PICK_PHOTO:
                    if(data.getData() != null){
                        currentPhotoUri = data.getData();
                        startImageCropIntent();
                    }
                    break;
            }
        }
    }

    @Override
    public void onRequestPermissionsResult(int requestCode, @NonNull String[] permissions, @NonNull int[] grantResults) {
        super.onRequestPermissionsResult(requestCode, permissions, grantResults);

        switch (requestCode) {
            case REQ_PERMISSIONS: {
                // If request is cancelled, the result arrays are empty.
                if (grantResults.length > 0
                        && grantResults[0] == PackageManager.PERMISSION_GRANTED) {

                    startCameraIntent();
                }
            }
        }
    }
}

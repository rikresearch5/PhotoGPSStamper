package com.example.photogpsstamper;

import androidx.annotation.Nullable;
import androidx.appcompat.app.AppCompatActivity;
import androidx.core.app.ActivityCompat;
import androidx.core.content.ContextCompat;

import android.Manifest;
import android.content.Intent;
import android.content.pm.PackageManager;
import android.graphics.Bitmap;
import android.graphics.BitmapFactory;
import android.graphics.Canvas;
import android.graphics.Color;
import android.graphics.Paint;
import android.graphics.Typeface;
import android.net.Uri;
import android.os.Bundle;
import android.os.Environment;
import android.provider.MediaStore;
import android.view.View;
import android.widget.Button;
import android.widget.EditText;
import android.widget.ImageView;
import android.widget.Toast;

import java.io.File;
import java.io.FileOutputStream;
import java.text.SimpleDateFormat;
import java.util.Date;
import java.util.Locale;

public class MainActivity extends AppCompatActivity {

    private static final int CAMERA_REQUEST = 1888;
    private static final int PERMISSION_REQUEST_CODE = 200;
    private ImageView imageView;
    private EditText latEditText, longEditText;
    private String currentPhotoPath;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_main);

        imageView = findViewById(R.id.imageView);
        Button captureButton = findViewById(R.id.captureButton);
        Button saveButton = findViewById(R.id.saveButton);
        latEditText = findViewById(R.id.latEditText);
        longEditText = findViewById(R.id.longEditText);

        if (checkPermission()) {
            Toast.makeText(this, "Permissions Granted", Toast.LENGTH_SHORT).show();
        } else {
            requestPermission();
        }

        captureButton.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                Intent cameraIntent = new Intent(MediaStore.ACTION_IMAGE_CAPTURE);
                startActivityForResult(cameraIntent, CAMERA_REQUEST);
            }
        });

        saveButton.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                if (currentPhotoPath != null) {
                    String latitude = latEditText.getText().toString();
                    String longitude = longEditText.getText().toString();
                    
                    if (latitude.isEmpty() || longitude.isEmpty()) {
                        Toast.makeText(MainActivity.this, "Please enter both latitude and longitude", Toast.LENGTH_SHORT).show();
                        return;
                    }
                    
                    Bitmap originalBitmap = BitmapFactory.decodeFile(currentPhotoPath);
                    Bitmap stampedBitmap = stampImage(originalBitmap, latitude, longitude);
                    saveImage(stampedBitmap);
                } else {
                    Toast.makeText(MainActivity.this, "No photo to save", Toast.LENGTH_SHORT).show();
                }
            }
        });
    }

    private Bitmap stampImage(Bitmap original, String latitude, String longitude) {
        int width = original.getWidth();
        int height = original.getHeight();
        Bitmap newBitmap = Bitmap.createBitmap(width, height, original.getConfig());

        Canvas canvas = new Canvas(newBitmap);
        canvas.drawBitmap(original, 0, 0, null);

        Paint paint = new Paint();
        paint.setColor(Color.RED);
        paint.setTextSize(40);
        paint.setTypeface(Typeface.DEFAULT_BOLD);
        paint.setAntiAlias(true);

        String timeStamp = new SimpleDateFormat("yyyy-MM-dd HH:mm:ss", Locale.getDefault()).format(new Date());
        String gpsInfo = "Lat: " + latitude + ", Long: " + longitude;
        String stampText = gpsInfo + "\n" + timeStamp;

        // Add black background for text
        Paint bgPaint = new Paint();
        bgPaint.setColor(Color.BLACK);
        bgPaint.setAlpha(150); // semi-transparent
        float textWidth = paint.measureText(stampText.split("\n")[0]);
        float textHeight = paint.getTextSize() * 2.5f;
        canvas.drawRect(20, height - textHeight - 40, textWidth + 40, height - 20, bgPaint);

        // Draw text
        canvas.drawText(gpsInfo, 30, height - 60, paint);
        canvas.drawText(timeStamp, 30, height - 20, paint);

        return newBitmap;
    }

    private void saveImage(Bitmap finalBitmap) {
        String timeStamp = new SimpleDateFormat("yyyyMMdd_HHmmss", Locale.getDefault()).format(new Date());
        String imageFileName = "GPS_Stamped_" + timeStamp + ".jpg";

        File storageDir = Environment.getExternalStoragePublicDirectory(Environment.DIRECTORY_PICTURES);
        File imageFile = new File(storageDir, imageFileName);

        try {
            FileOutputStream out = new FileOutputStream(imageFile);
            finalBitmap.compress(Bitmap.CompressFormat.JPEG, 90, out);
            out.close();
            
            // Add the image to the gallery
            Intent mediaScanIntent = new Intent(Intent.ACTION_MEDIA_SCANNER_SCAN_FILE);
            mediaScanIntent.setData(Uri.fromFile(imageFile));
            sendBroadcast(mediaScanIntent);
            
            Toast.makeText(this, "Image saved to Pictures folder", Toast.LENGTH_LONG).show();
        } catch (Exception e) {
            e.printStackTrace();
            Toast.makeText(this, "Error saving image", Toast.LENGTH_SHORT).show();
        }
    }

    @Override
    protected void onActivityResult(int requestCode, int resultCode, @Nullable Intent data) {
        super.onActivityResult(requestCode, resultCode, data);
        if (requestCode == CAMERA_REQUEST && resultCode == RESULT_OK) {
            Bundle extras = data.getExtras();
            Bitmap imageBitmap = (Bitmap) extras.get("data");
            imageView.setImageBitmap(imageBitmap);
            
            // Save full size image
            String timeStamp = new SimpleDateFormat("yyyyMMdd_HHmmss", Locale.getDefault()).format(new Date());
            String imageFileName = "GPS_Temp_" + timeStamp + ".jpg";
            File storageDir = getExternalFilesDir(Environment.DIRECTORY_PICTURES);
            File imageFile = new File(storageDir, imageFileName);
            currentPhotoPath = imageFile.getAbsolutePath();
            
            try {
                FileOutputStream out = new FileOutputStream(imageFile);
                imageBitmap.compress(Bitmap.CompressFormat.JPEG, 100, out);
                out.close();
            } catch (Exception e) {
                e.printStackTrace();
            }
        }
    }

    private boolean checkPermission() {
        int camera = ContextCompat.checkSelfPermission(getApplicationContext(), Manifest.permission.CAMERA);
        int storage = ContextCompat.checkSelfPermission(getApplicationContext(), Manifest.permission.WRITE_EXTERNAL_STORAGE);
        return camera == PackageManager.PERMISSION_GRANTED && storage == PackageManager.PERMISSION_GRANTED;
    }

    private void requestPermission() {
        ActivityCompat.requestPermissions(this, new String[]{
                Manifest.permission.CAMERA,
                Manifest.permission.WRITE_EXTERNAL_STORAGE,
                Manifest.permission.READ_EXTERNAL_STORAGE
        }, PERMISSION_REQUEST_CODE);
    }

    @Override
    public void onRequestPermissionsResult(int requestCode, String[] permissions, int[] grantResults) {
        super.onRequestPermissionsResult(requestCode, permissions, grantResults);
        if (requestCode == PERMISSION_REQUEST_CODE) {
            if (grantResults.length > 0) {
                boolean cameraAccepted = grantResults[0] == PackageManager.PERMISSION_GRANTED;
                boolean storageAccepted = grantResults[1] == PackageManager.PERMISSION_GRANTED;
                if (cameraAccepted && storageAccepted) {
                    Toast.makeText(this, "Permissions Granted", Toast.LENGTH_SHORT).show();
                } else {
                    Toast.makeText(this, "Permissions Denied", Toast.LENGTH_SHORT).show();
                }
            }
        }
    }
}
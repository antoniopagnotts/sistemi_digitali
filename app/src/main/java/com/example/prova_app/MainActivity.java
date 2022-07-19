package com.example.prova_app;

import android.Manifest;
import android.annotation.SuppressLint;
import android.app.Activity;
import android.content.Context;
import android.content.DialogInterface;
import android.content.Intent;
import android.content.pm.PackageManager;
import android.content.res.AssetManager;
import android.graphics.Bitmap;
import android.location.Address;
import android.location.Geocoder;
import android.location.Location;
import android.media.ThumbnailUtils;
import android.net.Uri;
import android.os.Bundle;
import android.provider.MediaStore;
import android.provider.Settings;
import android.util.SparseArray;
import android.view.View;
import android.widget.Button;
import android.widget.ImageView;
import android.widget.TextView;
import android.widget.Toast;

import androidx.annotation.*;
import androidx.appcompat.app.AlertDialog;
import androidx.appcompat.app.AppCompatActivity;
import androidx.core.app.ActivityCompat;

import com.example.prova_app.ml.Model;
import com.google.android.gms.location.FusedLocationProviderClient;
import com.google.android.gms.location.LocationServices;
import com.google.android.gms.tasks.OnCompleteListener;
import com.google.android.gms.tasks.Task;
import com.google.android.gms.vision.Frame;
import com.google.android.gms.vision.text.TextBlock;
import com.google.android.gms.vision.text.TextRecognizer;
import com.karumi.dexter.Dexter;
import com.karumi.dexter.MultiplePermissionsReport;
import com.karumi.dexter.PermissionToken;
import com.karumi.dexter.listener.PermissionRequest;
import com.karumi.dexter.listener.multi.MultiplePermissionsListener;

import org.tensorflow.lite.DataType;
import org.tensorflow.lite.support.tensorbuffer.TensorBuffer;

import java.io.BufferedReader;
import java.io.IOException;


import java.io.InputStream;
import java.io.InputStreamReader;
import java.nio.ByteBuffer;
import java.nio.ByteOrder;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Locale;


import butterknife.BindView;
import butterknife.ButterKnife;
import butterknife.OnClick;

public class MainActivity extends AppCompatActivity {
    private String locale;
    private static final String TAG = MainActivity.class.getSimpleName();
    public static final int REQUEST_IMAGE = 100;
    int imageSize = 160;
    @BindView(R.id.imageView)
    ImageView imgProfile;
    @BindView(R.id.result)
    TextView result;
    Bitmap bitmap1;

    //Location variables
    FusedLocationProviderClient fusedLocationProviderClient;
    Double latitude=0.0;
    Double longitude=0.0;

    //Speech to text variables
    Button speech_to_text_button;

    //Locations' data
    final int BEIRUT_SNACK = 0;
    final int BONELLI = 1;
    final int CITTA_DORO = 2;
    final int ENOTECA_ITALIANA = 3;
    final int FORNO_BRISA = 4;
    final int LA_CAVERNA = 5;
    final int LA_FORCHETTA = 6;
    final int LA_PIZZA_DA_ZERO = 7;
    final int MYAKO = 8;
    final int NUOVO_CAFFE_DEL_PORTO = 9;
    final int OSTERIA_DELLE_MOLINE = 10;
    final int PANZARO = 11;
    final int POKE_RAINBOW_CAFFE = 12;
    final int POKE_SHARK = 13;
    final int TRATTORIA_BELFIORE = 14;
    final int TRATTORIA_DA_PIETRO = 15;

    //data used in putCoordinate to set the hashMap values
    final int LATITUDE = 0;
    final int LONGITUDE = 1;

    HashMap<Integer, HashMap<Integer,Double>> locationHashmap = new HashMap<>();

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_main);
        ButterKnife.bind(this);

        // Clearing older images from cache directory
        // don't call this line if you want to choose multiple images in the same activity
        // call this once the bitmap(s) usage is over
        ImagePickerActivity.clearCache(this);

        speech_to_text_button = findViewById(R.id.speech_to_text_button);
        speech_to_text_button.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View view) {
                openSpeechToTextActivity();
            }
        });

        //initialize locations' HashMap
        initializeHashMapValues();

        //Location on map
        fusedLocationProviderClient = LocationServices.getFusedLocationProviderClient(this);
    }

    public void openSpeechToTextActivity() {
        Intent intent = new Intent(this, SpeechToTextActivity.class);
        startActivity(intent);
    }

    @OnClick({R.id.take})
    void onProfileImageClick() {
        result.setText("");
        Dexter.withActivity(this)
                .withPermissions(Manifest.permission.CAMERA, Manifest.permission.WRITE_EXTERNAL_STORAGE)
                .withListener(new MultiplePermissionsListener() {
                    @Override
                    public void onPermissionsChecked(MultiplePermissionsReport report) {
                        if (report.areAllPermissionsGranted()) {
                            showImagePickerOptions();
                        }

                        if (report.isAnyPermissionPermanentlyDenied()) {
                            showSettingsDialog();
                        }
                    }
                    @Override
                    public void onPermissionRationaleShouldBeShown(List<PermissionRequest> permissions, PermissionToken token) {
                        token.continuePermissionRequest();
                    }
                }).check();
    }


    private void showImagePickerOptions() {
        ImagePickerActivity.showImagePickerOptions(this, new ImagePickerActivity.PickerOptionListener() {
            @Override
            public void onTakeCameraSelected() {
                launchCameraIntent();
            }

            @Override
            public void onChooseGallerySelected() {
                launchGalleryIntent();
            }
        });
    }

    private void launchCameraIntent() {
        Intent intent = new Intent(MainActivity.this, ImagePickerActivity.class);
        intent.putExtra(ImagePickerActivity.INTENT_IMAGE_PICKER_OPTION, ImagePickerActivity.REQUEST_IMAGE_CAPTURE);

        // setting aspect ratio
        intent.putExtra(ImagePickerActivity.INTENT_LOCK_ASPECT_RATIO, true);
        intent.putExtra(ImagePickerActivity.INTENT_ASPECT_RATIO_X, 1); // 16x9, 1x1, 3:4, 3:2
        intent.putExtra(ImagePickerActivity.INTENT_ASPECT_RATIO_Y, 1);

        // setting maximum bitmap width and height
        intent.putExtra(ImagePickerActivity.INTENT_SET_BITMAP_MAX_WIDTH_HEIGHT, true);
        intent.putExtra(ImagePickerActivity.INTENT_BITMAP_MAX_WIDTH, 1000);
        intent.putExtra(ImagePickerActivity.INTENT_BITMAP_MAX_HEIGHT, 1000);

        startActivityForResult(intent, REQUEST_IMAGE);
    }

    private void launchGalleryIntent() {
        Intent intent = new Intent(MainActivity.this, ImagePickerActivity.class);
        intent.putExtra(ImagePickerActivity.INTENT_IMAGE_PICKER_OPTION, ImagePickerActivity.REQUEST_GALLERY_IMAGE);

        // setting aspect ratio
        intent.putExtra(ImagePickerActivity.INTENT_LOCK_ASPECT_RATIO, true);
        intent.putExtra(ImagePickerActivity.INTENT_ASPECT_RATIO_X, 1); // 16x9, 1x1, 3:4, 3:2
        intent.putExtra(ImagePickerActivity.INTENT_ASPECT_RATIO_Y, 1);
        startActivityForResult(intent, REQUEST_IMAGE);
    }

    @Override
    protected void onActivityResult(int requestCode, int resultCode, @Nullable Intent data) {
        super.onActivityResult(requestCode, resultCode, data);
        if (requestCode == REQUEST_IMAGE) {
            if (resultCode == Activity.RESULT_OK) {
                assert data != null;
                Uri uri = data.getParcelableExtra("path");
                try {
                    // You can update this bitmap to your server
                    bitmap1 = MediaStore.Images.Media.getBitmap(this.getContentResolver(), uri);
                    imgProfile.setImageBitmap(bitmap1);

                    int dimension = Math.min(bitmap1.getWidth(), bitmap1.getHeight());
                    bitmap1 = ThumbnailUtils.extractThumbnail(bitmap1, dimension, dimension);

                    bitmap1 = Bitmap.createScaledBitmap(bitmap1, imageSize, imageSize, false);

                } catch (IOException e) {
                    e.printStackTrace();
                }
            }
        }
    }

    @OnClick({R.id.scan})
    void classifyImage() {
        try {
            Model model = Model.newInstance(getApplicationContext());

            // Creates inputs for reference.
            TensorBuffer inputFeature0 = TensorBuffer.createFixedSize(new int[]{1, 160, 160, 3}, DataType.FLOAT32);
            ByteBuffer byteBuffer = ByteBuffer.allocateDirect(4 * imageSize * imageSize * 3);
            byteBuffer.order(ByteOrder.nativeOrder());

            int[] intValues = new int[imageSize * imageSize];
            bitmap1.getPixels(intValues, 0, bitmap1.getWidth(), 0, 0, bitmap1.getWidth(), bitmap1.getHeight());
            int pixel = 0;
            //iterate over each pixel and extract R, G, and B values. Add those values individually to the byte buffer.
            for (int i = 0; i < imageSize; i++) {
                for (int j = 0; j < imageSize; j++) {
                    int val = intValues[pixel++]; // RGB
                    byteBuffer.putFloat(((val >> 16) & 0xFF) * (1.f / 1));
                    byteBuffer.putFloat(((val >> 8) & 0xFF) * (1.f / 1));
                    byteBuffer.putFloat((val & 0xFF) * (1.f / 1));
                }
            }

            inputFeature0.loadBuffer(byteBuffer);

            // Runs model inference and gets result.
            Model.Outputs outputs = model.process(inputFeature0);
            TensorBuffer outputFeature0 = outputs.getOutputFeature0AsTensorBuffer();

            float[] confidences = outputFeature0.getFloatArray();
            // find the index of the class with the biggest confidence.
            int maxPos = 0;
            float maxConfidence = 0;

            String[] classes = {
                    "Beirut Snack",
                    "Bonelli",
                    "Città D'oro",
                    "Enoteca Italiana",
                    "Forno Brisa",
                    "La Caverna",
                    "La Forchetta",
                    "La Pizza Da Zero",
                    "Myako",
                    "Nuovo Caffè Del Porto",
                    "Osteria Delle Moline",
                    "Panzarò",
                    "Pokè Rainbow Caffè",
                    "Pokè Shark",
                    "Trattoria Belfiore",
                    "Trattoria Da Pietro"
            };

            for (int i = 0; i < confidences.length; i++) {
                confidences[i]=confidences[i]*100;
            }
            for (int i = 0; i < confidences.length; i++) {
                if (confidences[i] > maxConfidence) {
                    maxConfidence = confidences[i];
                    maxPos = i;
                }
            }
            locale = classes[maxPos].toLowerCase();

            if (ActivityCompat.checkSelfPermission(MainActivity.this, Manifest.permission.ACCESS_FINE_LOCATION) == PackageManager.PERMISSION_GRANTED &&
                    ActivityCompat.checkSelfPermission(MainActivity.this, Manifest.permission.ACCESS_COARSE_LOCATION) == PackageManager.PERMISSION_GRANTED) {
                getLocation();
            } else {
                ActivityCompat.requestPermissions(MainActivity.this, new String[]{Manifest.permission.ACCESS_FINE_LOCATION}, 44);
                ActivityCompat.requestPermissions(MainActivity.this, new String[]{Manifest.permission.ACCESS_COARSE_LOCATION}, 44);
            }

            double localLongitude;
            double localLatitude;
            switch(classes[maxPos]){
                case "Città D'oro":
                    localLongitude = locationHashmap.get(CITTA_DORO).get(LONGITUDE);
                    localLatitude = locationHashmap.get(CITTA_DORO).get(LATITUDE);
                    break;
                case "Enoteca Italiana":
                    localLongitude = locationHashmap.get(ENOTECA_ITALIANA).get(LONGITUDE);
                    localLatitude = locationHashmap.get(ENOTECA_ITALIANA).get(LATITUDE);
                    break;
                case "Forno Brisa":
                    localLongitude = locationHashmap.get(FORNO_BRISA).get(LONGITUDE);
                    localLatitude = locationHashmap.get(FORNO_BRISA).get(LATITUDE);
                    break;
                case "La Forchetta":
                    localLongitude = locationHashmap.get(LA_FORCHETTA).get(LONGITUDE);
                    localLatitude = locationHashmap.get(LA_FORCHETTA).get(LATITUDE);
                    break;
                case "La Pizza Da Zero":
                    localLongitude = locationHashmap.get(LA_PIZZA_DA_ZERO).get(LONGITUDE);
                    localLatitude = locationHashmap.get(LA_PIZZA_DA_ZERO).get(LATITUDE);
                    break;
                case "Nuovo Caffè Del Porto":
                    localLongitude = locationHashmap.get(NUOVO_CAFFE_DEL_PORTO).get(LONGITUDE);
                    localLatitude = locationHashmap.get(NUOVO_CAFFE_DEL_PORTO).get(LATITUDE);
                    break;
                case "Pokè Rainbow Caffè":
                    localLongitude = locationHashmap.get(POKE_RAINBOW_CAFFE).get(LONGITUDE);
                    localLatitude = locationHashmap.get(POKE_RAINBOW_CAFFE).get(LATITUDE);
                    break;
                case "Trattoria Belfiore":
                    localLongitude = locationHashmap.get(TRATTORIA_BELFIORE).get(LONGITUDE);
                    localLatitude = locationHashmap.get(TRATTORIA_BELFIORE).get(LATITUDE);
                    break;
                case "Beirut Snack":
                    localLongitude = locationHashmap.get(BEIRUT_SNACK).get(LONGITUDE);
                    localLatitude = locationHashmap.get(BEIRUT_SNACK).get(LATITUDE);
                    break;
                case "Bonelli":
                    localLongitude = locationHashmap.get(BONELLI).get(LONGITUDE);
                    localLatitude = locationHashmap.get(BONELLI).get(LATITUDE);
                    break;
                case "La Caverna":
                    localLongitude = locationHashmap.get(LA_CAVERNA).get(LONGITUDE);
                    localLatitude = locationHashmap.get(LA_CAVERNA).get(LATITUDE);
                    break;
                case "Myako":
                    localLongitude = locationHashmap.get(MYAKO).get(LONGITUDE);
                    localLatitude = locationHashmap.get(MYAKO).get(LATITUDE);
                    break;
                case "Osteria Delle Moline":
                    localLongitude = locationHashmap.get(OSTERIA_DELLE_MOLINE).get(LONGITUDE);
                    localLatitude = locationHashmap.get(OSTERIA_DELLE_MOLINE).get(LATITUDE);
                    break;
                case "Panzarò":
                    localLongitude = locationHashmap.get(PANZARO).get(LONGITUDE);
                    localLatitude = locationHashmap.get(PANZARO).get(LATITUDE);
                    break;
                case "Pokè Shark":
                    localLongitude = locationHashmap.get(POKE_SHARK).get(LONGITUDE);
                    localLatitude = locationHashmap.get(POKE_SHARK).get(LATITUDE);
                    break;
                case "Trattoria Da Pietro":
                    localLongitude = locationHashmap.get(TRATTORIA_DA_PIETRO).get(LONGITUDE);
                    localLatitude = locationHashmap.get(TRATTORIA_DA_PIETRO).get(LATITUDE);
                    break;
                default:
                    localLatitude = 0;
                    localLongitude = 0;
                    break;
            }

            String res = String.format("%s: %.1f%%\n", classes[maxPos], confidences[maxPos]);
            //res +=textFromImage(bitmap1)+"\n";
            double calculatedDistance = -1000;
            calculatedDistance = calculateDistanceOfTheUserFromTheLocal(latitude,longitude,localLatitude,localLongitude);
            //considering the accurancy of google maps (20 meters circa), we consider for a good accurancy 30meters
            if(confidences[maxPos]>70){
                if(calculatedDistance>30){
                    result.setText("\n" + res + "\n" +
                            "You are far away from the local ("+ calculatedDistance + " meters)!\n" +
                            "if you want to see the menù click\n"+
                            "MENU\n"
                    );
                } else {
                    openMenu();
                }
            }else{
                result.setText("Restaurant not recognized, retry!");
            }
            // Releases model resources if no longer used.
            model.close();
        } catch (Exception e) {
            Toast.makeText(getApplicationContext(),"Take a photo!",Toast.LENGTH_LONG).show();
            e.printStackTrace();
        }
    }

    @OnClick({R.id.openMenu})
    void openMenu(){
        TextView output= (TextView) findViewById(R.id.result);
        StringBuilder text = new StringBuilder();
        output.setText("");
        BufferedReader reader = null;
        Context context = this.getApplicationContext();
        try {
            AssetManager am = context.getAssets();
            InputStream is = am.open(locale+".txt");

            reader = new BufferedReader(
                    new InputStreamReader(is));

            // do reading, usually loop until end of file reading
            String mLine = null;
            while ((mLine = reader.readLine()) != null) {
                text.append(mLine);
                text.append('\n');
            }
        } catch (IOException e) {
            Toast.makeText(getApplicationContext(),"Error reading file!",Toast.LENGTH_LONG).show();
            e.printStackTrace();
        } finally {
            if (reader != null) {
                try {
                    reader.close();
                } catch (IOException e) {
                    e.printStackTrace();
                }
            }
            output.setText((CharSequence) text);
        }
    }


    /**
     * It consider the Google Maps coordinates (WGS84) and uses the Haversine formula
     * @param lat1
     * @param lon1
     * @param lat2
     * @param lon2
     * @return the distance in meter from the user and the local
     */
    private double calculateDistanceOfTheUserFromTheLocal(double lat1, double lon1, double lat2, double lon2){
        double R = 6371; // Radius of earth in KM
        double dLat = degreeToRadiant(lat2-lat1);
        double dLon = degreeToRadiant(lon2-lon1);
        double a = Math.sin(dLat/2) * Math.sin(dLat/2) +
                Math.cos(degreeToRadiant(lat1)) * Math.cos(degreeToRadiant(lat2)) *
                        Math.sin(dLon/2) * Math.sin(dLon/2);
        double c = Math.atan2(Math.sqrt(a), Math.sqrt(1-a));
        double d = R * c * 1000;
        return d;
    }

    /**
     * Support function used in calculateDistanceOfTheUserFromTheLocal(...);
     * @param degree
     * @return the conversion in radiant
     */
    private double degreeToRadiant(double degree){
        return degree * (Math.PI/180);
    }

    /**
     * Used to get the coordinates
     */
    @SuppressLint("MissingPermission")
    private void getLocation() {

        fusedLocationProviderClient.getLastLocation().addOnCompleteListener(new OnCompleteListener<Location>() {
            @Override
            public void onComplete(@NonNull Task<Location> task) {
                //initialize location
                Location location = task.getResult();
                if (location != null) {
                    //Initialize geoCoder
                    Geocoder geocoder = new Geocoder(MainActivity.this, Locale.getDefault());
                    //initialize address list
                    try {
                        List<Address> addresses = new ArrayList<>();
                        addresses = geocoder.getFromLocation(
                                location.getLatitude(), location.getLongitude(), 1
                        );
                        latitude = addresses.get(0).getLatitude();
                        longitude = addresses.get(0).getLongitude();
                    } catch (IOException e) {
                        e.printStackTrace();
                    }
                }
            }
        });
    }


    public void initializeHashMapValues(){
        HashMap<Integer,Double> locationHashmapData = new HashMap<Integer,Double>();

        this.locationHashmap.put(CITTA_DORO,putCoordinate(44.5045183,11.3397822));
        this.locationHashmap.put(ENOTECA_ITALIANA,putCoordinate(44.4967059,11.3391671));
        this.locationHashmap.put(FORNO_BRISA,putCoordinate(44.4967052,11.3391671));
        this.locationHashmap.put(LA_FORCHETTA,putCoordinate(44.5080911,11.3520538));
        this.locationHashmap.put(LA_PIZZA_DA_ZERO,putCoordinate(44.4966059,11.3392671));
        this.locationHashmap.put(NUOVO_CAFFE_DEL_PORTO,putCoordinate(44.5012647,11.3309974));
        this.locationHashmap.put(POKE_RAINBOW_CAFFE,putCoordinate(44.4985837,11.3294094));
        this.locationHashmap.put(TRATTORIA_BELFIORE,putCoordinate(44.4974444,11.3359676));

        this.locationHashmap.put(BEIRUT_SNACK,putCoordinate(44.4981302,11.345195));
        this.locationHashmap.put(BONELLI,putCoordinate(44.4986933,11.3377552));
        this.locationHashmap.put(LA_CAVERNA,putCoordinate(44.4977071,11.3464599));
        this.locationHashmap.put(MYAKO,putCoordinate(44.5080911,11.3520538));
        this.locationHashmap.put(OSTERIA_DELLE_MOLINE,putCoordinate(44.497815,11.3478201));
        this.locationHashmap.put(PANZARO,putCoordinate(44.4985733,11.3445127));
        this.locationHashmap.put(POKE_SHARK,putCoordinate(44.4982402,11.3357261));
        this.locationHashmap.put(TRATTORIA_DA_PIETRO,putCoordinate(44.4981958,11.3439585));

    }

    /**
     *
     * @param latitude
     * @param longitude
     * @return the HashMap with the coordinate
     */
    public HashMap<Integer,Double> putCoordinate(double latitude, double longitude){
        HashMap<Integer,Double> values = new HashMap<>();
        values.put(LONGITUDE,longitude);
        values.put(LATITUDE,latitude);
        return values;
    }


    public String textFromImage(Bitmap image){
        TextRecognizer recognizer=new TextRecognizer.Builder(this).build();
        if(!recognizer.isOperational()){
            Toast.makeText(MainActivity.this, "Error Occurred", Toast.LENGTH_SHORT).show();
            return "isOperational";
        }
        else{
            Frame frame=new Frame.Builder().setBitmap(image).build();
            SparseArray<TextBlock> textBlockSparseArray=recognizer.detect(frame);
            StringBuilder stringBuilder=new StringBuilder();
            for(int i=0; i<textBlockSparseArray.size();i++){
                TextBlock textBlock=textBlockSparseArray.valueAt(i);
                stringBuilder.append(textBlock.getValue());
                stringBuilder.append("/n");
            }
            return stringBuilder.toString();
        }
    }


    /**
     * Showing Alert Dialog with Settings option
     * Navigates user to app settings
     * NOTE: Keep proper title and message depending on your app
     */


    private void showSettingsDialog() {
        AlertDialog.Builder builder = new AlertDialog.Builder(MainActivity.this);
        builder.setTitle(getString(R.string.dialog_permission_title));
        builder.setMessage(getString(R.string.dialog_permission_message));
        builder.setPositiveButton(getString(R.string.go_to_settings), (dialog, which) -> {
            dialog.cancel();
            openSettings();
        });
        builder.setNegativeButton(getString(android.R.string.cancel), (dialog, which) -> dialog.cancel());
        builder.show();

    }

    // navigating user to app settings
    private void openSettings() {
        Intent intent = new Intent(Settings.ACTION_APPLICATION_DETAILS_SETTINGS);
        Uri uri = Uri.fromParts("package", getPackageName(), null);
        intent.setData(uri);
        startActivityForResult(intent, 101);
    }
}

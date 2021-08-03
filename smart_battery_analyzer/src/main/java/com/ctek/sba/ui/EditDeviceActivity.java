package com.ctek.sba.ui;

import android.app.Activity;
import android.app.AlertDialog;
import android.content.ClipData;
import android.content.Context;
import android.content.DialogInterface;
import android.content.Intent;
import android.content.pm.PackageManager;
import android.database.Cursor;
import android.graphics.Bitmap;
import android.graphics.BitmapFactory;
import android.graphics.Matrix;
import android.media.ExifInterface;
import android.media.Image;
import android.net.Uri;
import android.os.Bundle;
import android.os.Environment;
import android.os.StrictMode;
import android.provider.MediaStore;
import android.support.v4.content.FileProvider;
import android.support.v7.widget.Toolbar;
import android.text.InputFilter;
import android.util.Log;
import android.view.View;
import android.view.WindowManager;
import android.widget.ArrayAdapter;
import android.widget.Button;
import android.widget.EditText;
import android.widget.ImageView;
import android.widget.TextView;
import android.widget.Toast;

import com.ctek.sba.appwidget.UpdateService;
import com.ctek.sba.bluetooth.Notifications;
import com.ctek.sba.device.DeviceMap;
import com.ctek.sba.device.DeviceMapIntervals;
import com.ctek.sba.device.DeviceSoCMap;
import com.ctek.sba.rest.SrvPostCapacity;
import com.ctek.sba.ui.support.MarshMallowPermission;
import com.ctek.sba.ui.support.MarshMallowPermissionImages;
import com.ctek.sba.util.PermissionCheck;

import com.ctek.sba.util.Utils;

import java.io.File;
import java.io.IOException;
import java.text.SimpleDateFormat;
import java.util.Date;

import greendao.Device;

import com.ctek.sba.R;

import com.ctek.sba.io.dao.DeviceRepository;


public class EditDeviceActivity extends BaseActivity {

  public static final String EXTRA_DEVICE_ID = "id";
  public static final String EXTRA_NEW_DEVICE = "new_device";

  public static void start (Context ctx, boolean bNewDevice, long id) {
    Intent i_ = new Intent(ctx, EditDeviceActivity.class);
    i_.putExtra(EXTRA_NEW_DEVICE, bNewDevice);
    i_.putExtra(EXTRA_DEVICE_ID, id);
    ctx.startActivity(i_);
  }

  static final int REQUEST_IMAGE_CAPTURE = 1;
  static final int REQUEST_PICK_PHOTO_CODE = 2;


  private final String IMAGE_FOLDER_NAME = "CTEK";
  private final String TAG = this.getClass().getSimpleName();



  private File newDeviceImageFile;//if image is selected

 // private ImageChooserManager imageChooserManager;
  private String filePath;
  private int chooserType;
  private ImageView photoImageView;
  private TextView clickToChangePictureText;
  private boolean imageChanged;

  private EditText nameEditText;
  private TextView bottomButton;
  private TextView txtCapacity;

  private Device mDevice;
  private boolean isNewDevice;

  // We save old capacity before starting BatteryCapacityActivity
  // and check whether it was changed in onResume().
  // Also both activities are assumed to be in portrait mode only.
  private double oldCapacity = Double.MIN_VALUE;

  protected static final int MAX_DEVICE_NAME = 64;

  @Override
  protected void onCreate(Bundle savedInstanceState) {
    super.onCreate(savedInstanceState);
    setContentView(R.layout.activity_edit_device);

    Toolbar actionBar = (Toolbar) findViewById(R.id.action_bar);
    setSupportActionBar(actionBar);

    Bundle extras = getIntent().getExtras();
    if (extras != null) {
      isNewDevice = extras.getBoolean(EXTRA_NEW_DEVICE);
      mDevice = DeviceRepository.getDeviceForId(this, extras.getLong(EXTRA_DEVICE_ID));
    }

    // Make both the image and the text clickable
    photoImageView = (ImageView) findViewById(R.id.device_photo);
    photoImageView.setOnClickListener(photoPickerListener);

    clickToChangePictureText = (TextView) findViewById(R.id.edit_photo);
    clickToChangePictureText.setOnClickListener(photoPickerListener);

    nameEditText = (EditText) findViewById(R.id.device_name);
    nameEditText.setFilters(new InputFilter[]{new InputFilter.LengthFilter(MAX_DEVICE_NAME)});

    bottomButton = (TextView) findViewById(R.id.bottom_button);
    ImageView backImgVw = (ImageView) actionBar.findViewById(R.id.img_back);


    String deviceName = (mDevice!=null) ? mDevice.getName() : null;
    if (deviceName!=null) {
      if(deviceName.length() > MAX_DEVICE_NAME) {
        deviceName = deviceName.substring(0, MAX_DEVICE_NAME);
      }
      nameEditText.setText(deviceName);
    }

    if (isNewDevice) {
      nameEditText.setText("");
      backImgVw.setVisibility(View.GONE);

      bottomButton.setBackgroundResource(R.color.ctek_orange);
      bottomButton.setText(R.string.finish);

      TextView saveButton = (TextView)findViewById(R.id.save_button);
      saveButton.setVisibility(View.GONE);

    } else {
      bottomButton.setBackgroundResource(R.color.ctek_orange); // red_button. CBS-36 Remove Sender should be CTEK orange.
      bottomButton.setText(R.string.delete_sender);
    }

    if (mDevice != null && mDevice.getImagePath() != null) {
      photoImageView.setScaleType(ImageView.ScaleType.CENTER_CROP);
      String imgPath = mDevice.getImagePath();
      this.setDeviceImage(new File(mDevice.getImagePath()));
      //Picasso.with(photoImageView.getContext()).load(new File(mDevice.getImagePath())).
        //  skipMemoryCache().into(photoImageView);
    }

    txtCapacity = (TextView) findViewById(R.id.battery_capacity);
    TextView txtSenderId = (TextView) findViewById(R.id.sender_id_textview);
    txtSenderId.setText(mDevice.getSerialnumber());
    setCapacity();
  }

  @Override
  public void onResume() {
    super.onResume();

    double newCapacity = DeviceMap.getInstance().getDeviceCapacity(mDevice.getId());
    if((oldCapacity!=Double.MIN_VALUE) && (newCapacity!=oldCapacity)) {
      SrvPostCapacity.start(this, mDevice.getId(), newCapacity);
    }
    setCapacity();
    // CBS-55 Android - Keyboard in Edit device dialog.
    getWindow().setSoftInputMode(isNewDevice ? WindowManager.LayoutParams.SOFT_INPUT_STATE_VISIBLE : WindowManager.LayoutParams.SOFT_INPUT_STATE_HIDDEN);
  }

  public void back(View view) {
    // Save and finish (Back is only used when editing existing devices)
    finish();
  }

  @Override
  public void onBackPressed() {
    if (isNewDevice == false) {
      // Save and finish (Back is only used when editing existing devices)
      finish();
    }
  }

  public void onBottomButtonClick(View view) {
    if (isNewDevice) {
      save();
      broadcasrDeviceAdded();
      //Added
      if (!nameEditText.getText().toString().isEmpty()) {
        finish();
      }
    } else {
      showConfirmDeleteDialog();
    }
  }

  public void saveDetails(View view) {
    save();
    broadcasrDeviceUpdated();
    if (!nameEditText.getText().toString().isEmpty()) {
      finish();
    }
  }

  private void save() {
    if (nameEditText.getText().toString().isEmpty()) {
      nameEditText.setError(getString(R.string.error_name_empty));
    } else {
      mDevice.setName(nameEditText.getText().toString());
      Toast.makeText(EditDeviceActivity.this, getString(R.string.changes_save_success), Toast.LENGTH_SHORT).show();
      if (newDeviceImageFile != null) {
        //Save new image in local storage and update the new path in DB
        String newImgPath = newDeviceImageFile.getPath();
        if (newImgPath == "") {
          //There is some problem in storing image in local storeage
        }
        else{
          mDevice.setImagePath(newImgPath);
          Toast.makeText(EditDeviceActivity.this, getString(R.string.changes_save_success), Toast.LENGTH_SHORT).show();
        }
      }
      DeviceRepository.insertOrUpdate(this, mDevice);
    }
//    Toast.makeText(EditDeviceActivity.this, "Saved", Toast.LENGTH_SHORT).show();
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

    newDeviceImageFile = image.getAbsoluteFile();
    return image;
  }

  private void showConfirmDeleteDialog() {

    onConfirmDeleteDeleteClickListener = new DialogInterface.OnClickListener() {
      public void onClick(DialogInterface dialog, int which) {
        delete();
      }
    };
    new AlertDialog.Builder(this)
        .setTitle(R.string.confirm_delete_title)
        .setMessage(R.string.confirm_delete_message)
        .setNegativeButton(R.string.delete, onConfirmDeleteDeleteClickListener)
        .setPositiveButton(R.string.cancel, null)
        .show();
  }

  private void delete() {
    Long device_id = mDevice.getId();
    Context ctxApp = getApplicationContext();
    Notifications.deleteAlert4Device(ctxApp, mDevice);
    Notifications.deletePendingNotifications4DeviceId(ctxApp, device_id);

    DeviceRepository.deleteDeviceWithId(this, device_id);
    DeviceMap.getInstance().removeDevice(device_id);
    DeviceMapIntervals.getInstance().removeDevice(device_id);
    DeviceSoCMap.getInstance().removeDevice(device_id);

    //Fixed: Image not deleted after deleting the sender
    photoImageView.setImageDrawable(null);
    ///

    broadcastDeviceDelete();

    finish();
  }
  
  private void broadcastDeviceDelete() {
    Intent intent = new Intent(UpdateService.WIDGET_DEVICE_REMOVED);
    getApplicationContext().sendBroadcast(intent);
  }
  
  private void broadcasrDeviceAdded() {
    Intent intent = new Intent(UpdateService.WIDGET_NEW_DEVICE_ADDED);
    getApplicationContext().sendBroadcast(intent);
  }

  private void broadcasrDeviceUpdated() {
    Intent intent = new Intent(UpdateService.WIDGET_DEVICE_UPDATED);
    getApplicationContext().sendBroadcast(intent);
  }
  
  private void showPickerDialog () {
    final String[] items = new String[] {
        EditDeviceActivity.this.getString(R.string.take_from_camera),
        EditDeviceActivity.this.getString(R.string.select_from_gallery),
    };
    ArrayAdapter<String> adapter = new ArrayAdapter<String>(EditDeviceActivity.this, android.R.layout.select_dialog_item, items);
    AlertDialog.Builder builder = new AlertDialog.Builder(EditDeviceActivity.this);

    builder.setAdapter(adapter, new DialogInterface.OnClickListener() {
      public void onClick(DialogInterface dialog, int item) {
        // pick from camera
        if (item == 0) {
          doTakePicture();
        } else {
          doChooseImage();
        }
      }
    });

    final AlertDialog dialog = builder.create();
    dialog.show();
    return;
  }

  private void checkImagesPermission () {
    MarshMallowPermission perm = new MarshMallowPermissionImages(this);
    if (perm.check()) {
      showPickerDialog();
    }
    else {
      perm.request();
    }
  }

  // Photo picker listener
  private final View.OnClickListener photoPickerListener = new View.OnClickListener() {
    @Override
    public void onClick(View view) {
      checkImagesPermission();
    }
  };

  // handle result for photo picking
  @Override
  public void onActivityResult(int requestCode, int resultCode, Intent data) {
    if (requestCode == REQUEST_IMAGE_CAPTURE && resultCode == RESULT_OK) {
      //Update the device image
      this.setDeviceImage(newDeviceImageFile);
    }
    else if (resultCode == Activity.RESULT_OK && requestCode == REQUEST_PICK_PHOTO_CODE) {
        final Uri imageUri = data.getData();
        newDeviceImageFile = new File(this.getRealPathFromURI(this,imageUri));
      //Update the device image
        this.setDeviceImage(newDeviceImageFile);
    }
  }

  public String getRealPathFromURI(Context context, Uri contentUri) {
    Cursor cursor = null;
    try {
      String[] proj = { MediaStore.Images.Media.DATA };
      cursor = context.getContentResolver().query(contentUri,  proj, null, null, null);
      int column_index = cursor.getColumnIndexOrThrow(MediaStore.Images.Media.DATA);
      cursor.moveToFirst();
      return cursor.getString(column_index);
    } finally {
      if (cursor != null) {
        cursor.close();
      }
    }
  }

  @Override
  public void onRequestPermissionsResult(int requestCode, String permissions[], int[] grantResults) {
    boolean success = (grantResults.length > 0) && (grantResults[0] == PackageManager.PERMISSION_GRANTED);
    switch (requestCode) {
      case  MarshMallowPermission.REQUEST_CODE_CAMERA:
        if (success) {
          //camera permission granted
          doTakePicture();
        }
        else {
            //camera permission denied
        }
        break;
      case  MarshMallowPermission.REQUEST_CODE_IMAGES:
        if (success) {
          // doChooseImage();
          //image access permission granted
          showPickerDialog();
        }
        else {
          //Image permission access denied
        }
        break;
      case PermissionCheck.CTEK_CAMERA_PERMISSION_CODE:
        if (success) {
          //camera permission granted
          doTakePicture();
        }
        else {
          //camera permisisn denied
        }
        break;
    }
  }

  private void doChooseImage() {
    Intent intent = new Intent(Intent.ACTION_PICK);
    intent.setType("image/*");
    startActivityForResult(intent, REQUEST_PICK_PHOTO_CODE);
  }

  private void doTakePicture() {
    if (PermissionCheck.isCameraPermissionGranted(this)) {
      Intent takePhotoIntent = new Intent(MediaStore.ACTION_IMAGE_CAPTURE);
      if (takePhotoIntent.resolveActivity(getPackageManager()) != null) {

        File photoFile = null;
        try {
          photoFile = createImageFile();
        } catch (IOException ex) {
          // Error occurred while creating the File

        }
        // Continue only if the File was successfully created
        if (photoFile != null) {
          Uri photoURI = FileProvider.getUriForFile(getApplication().getApplicationContext(),
              "ibas.provider", photoFile);

              /*FileProvider.getUriForFile(this,
              "com.example.android.fileprovider",
              photoFile);*/
          takePhotoIntent.putExtra(MediaStore.EXTRA_OUTPUT, photoURI);
          startActivityForResult(takePhotoIntent, REQUEST_IMAGE_CAPTURE);
        }
      } else {
        //camera cannot be started
      }
    }
    else {

    }
  }
/*
  @Override
  public void onImageChosen(ChosenImage image) {

    // Use local variable. Suspected that the parameter as final did not always work.
    final ChosenImage myImage = image;

    EditDeviceActivity.this.runOnUiThread(new Runnable() {

      @Override
      public void run() {
        if (myImage != null) {
          setChosenImage(myImage);
          photoImageView.setScaleType(ImageView.ScaleType.CENTER_CROP);
          Picasso.with(photoImageView.getContext()).load(new File(myImage.getFileThumbnail())).
              skipMemoryCache().into(photoImageView);
        }
      }
    });
  }
  @Override
  public void onImagesChosen(ChosenImages chosenImages) {

  }
  private void setChosenImage(ChosenImage image) {
    chosenImage = image;
    imageChanged = true;
  }

  @Override
  public void onError(final String reason) {
    EditDeviceActivity.this.runOnUiThread(new Runnable() {

      @Override
      public void run() {
        Toast.makeText(EditDeviceActivity.this, reason, Toast.LENGTH_LONG).show();
      }
    });
  }*/

  private void setDeviceImage(final File imgFile) {
    EditDeviceActivity.this.runOnUiThread(new Runnable() {
      @Override
      public void run() {
          photoImageView.setScaleType(ImageView.ScaleType.CENTER_CROP);
          photoImageView.setImageBitmap(Utils.rotateImage(imgFile));
        }
    });

  }





  private DialogInterface.OnClickListener onConfirmDeleteDeleteClickListener = new DialogInterface.OnClickListener() {
    public void onClick(DialogInterface dialog, int which) {
      delete();
    }
  };

  public void onBatteryButtonClick(View view) {
    oldCapacity = DeviceMap.getInstance().getDeviceCapacity(mDevice.getId());
    BatteryCapacityActivity.start(this, mDevice.getId());
  }

  private void setCapacity() {
    double dCapacity = DeviceMap.getInstance().getDeviceCapacity(mDevice.getId());
    long   iCapacity = Math.round(dCapacity);
    String sCapacity = getString(R.string.capacity_is_NNN).replace("NNN", String.valueOf(iCapacity));
    txtCapacity.setText(sCapacity);
  }

} // EOClass

/**
 * Created by Fabrice Armisen (farmisen@gmail.com) on 1/4/16.
 */

package com.lwansbrough.RCTCamera;

import android.graphics.Bitmap;
import android.content.ContentValues;
import android.graphics.BitmapFactory;
import android.content.ContentResolver;
import android.hardware.Camera;
import android.media.MediaActionSound;
import android.net.Uri;
import android.os.Environment;
import android.provider.MediaStore;
import android.util.Base64;
import android.util.Log;
import com.facebook.react.bridge.*;
import android.media.MediaScannerConnection;

import javax.annotation.Nullable;
import java.io.*;
import android.media.MediaRecorder;
import android.media.CamcorderProfile;
import android.media.MediaPlayer;
import java.text.SimpleDateFormat;
import java.util.Collections;
import java.util.Date;
import java.util.HashMap;
import java.util.Map;
import java.util.List;
import android.content.Intent;
import android.content.Context;

public class RCTCameraModule extends ReactContextBaseJavaModule {
    private static final String TAG = "RCTCameraModule";

    public static final int RCT_CAMERA_ASPECT_FILL = 0;
    public static final int RCT_CAMERA_ASPECT_FIT = 1;
    public static final int RCT_CAMERA_ASPECT_STRETCH = 2;
    public static final int RCT_CAMERA_CAPTURE_MODE_STILL = 0;
    public static final int RCT_CAMERA_CAPTURE_MODE_VIDEO = 1;
    public static final int RCT_CAMERA_CAPTURE_TARGET_MEMORY = 0;
    public static final int RCT_CAMERA_CAPTURE_TARGET_DISK = 1;
    public static final int RCT_CAMERA_CAPTURE_TARGET_CAMERA_ROLL = 2;
    public static final int RCT_CAMERA_CAPTURE_TARGET_TEMP = 3;
    public static final int RCT_CAMERA_ORIENTATION_AUTO = 0;
    public static final int RCT_CAMERA_ORIENTATION_LANDSCAPE_LEFT = 1;
    public static final int RCT_CAMERA_ORIENTATION_LANDSCAPE_RIGHT = 2;
    public static final int RCT_CAMERA_ORIENTATION_PORTRAIT = 3;
    public static final int RCT_CAMERA_ORIENTATION_PORTRAIT_UPSIDE_DOWN = 4;
    public static final int RCT_CAMERA_TYPE_FRONT = 1;
    public static final int RCT_CAMERA_TYPE_BACK = 2;
    public static final int RCT_CAMERA_FLASH_MODE_OFF = 0;
    public static final int RCT_CAMERA_FLASH_MODE_ON = 1;
    public static final int RCT_CAMERA_FLASH_MODE_AUTO = 2;
    public static final int RCT_CAMERA_TORCH_MODE_OFF = 0;
    public static final int RCT_CAMERA_TORCH_MODE_ON = 1;
    public static final int RCT_CAMERA_TORCH_MODE_AUTO = 2;
    public static final int MEDIA_TYPE_IMAGE = 1;
    public static final int MEDIA_TYPE_VIDEO = 2;
    public static final int MEDIA_TYPE_AUDIO = 3;

    private final ReactApplicationContext _reactContext;

     private MediaRecorder mediaRecorder = null;
     private MediaRecorder audioRecorder = null;
     private MediaPlayer mPlayer = null;
     private File videoFile;
     private File audioFile;
     private static String audioFilePath = null;
     private static String audioFileName = null;
     private static String videoFileName = null;
     private Camera mCamera = null;
     private Promise recordingPromise = null;
     private Promise audioRecorderPromise = null;
     private ContentValues values;
     private MediaScannerConnection mScanner;
     private Promise deleteImagePromise = null;


    public RCTCameraModule(ReactApplicationContext reactContext) {
        super(reactContext);
        _reactContext = reactContext;
        // audioRecorder = new MediaRecorder(reactContext);
        
    }

    @Override
    public String getName() {
        return "RCTCameraModule";
    }

    private String generateAudioFileName() {
        String tempfile = Environment.getExternalStorageDirectory().getAbsolutePath();
         String timeStamp = new SimpleDateFormat("yyyyMMdd_HHmmss").format(new Date());
        tempfile +=  "Hamza" + timeStamp + ".3gp";
        return tempfile;
    }

    private boolean prepareAudioRecorder() {
        audioRecorder = new MediaRecorder();
        audioRecorder.setAudioSource(MediaRecorder.AudioSource.MIC);
        audioRecorder.setOutputFormat(MediaRecorder.OutputFormat.THREE_GPP);
        audioFileName = Environment.getExternalStorageDirectory().getAbsolutePath();
        String timeStamp = new SimpleDateFormat("yyyyMMdd_HHmmss").format(new Date());
         // audioFileName += "/" + timeStamp;
        audioFileName += "/" + timeStamp;

        audioRecorder.setOutputFile(audioFileName);
        audioRecorder.setAudioEncoder(MediaRecorder.AudioEncoder.AMR_NB);

        try {
            audioRecorder.prepare();
            return true;
        } catch (IOException e) {
            Log.e(TAG, "prepare() failed");
            return false;
        }           
    }

    @ReactMethod
    public void startAudioRecording(final Promise promise) {
        if (audioRecorderPromise == null) {
            if (prepareAudioRecorder()) {
                try {
                    audioRecorder.start();
                    audioRecorderPromise = promise;
                } catch (final Exception e) {
                    promise.reject("error: unable to invoke audioRecorder.start(): " + e.getMessage());
                }  
            } else {
                promise.reject("AudioRecorder returned false");
            }
        } else {
            promise.reject("AudioRecorderPromise was not null");
        }        
    }

    @ReactMethod

    public void deletePicture(String imagePath, final Promise promise) {

        deleteImagePromise = promise;

        if (imagePath != null && !imagePath.isEmpty()) {

            if (deletePicture(imagePath)) {
                promise.resolve("successfully deleted picture!");
            } else {
                promise.resolve("unable to delete image!");
            }
        }
    }

    private boolean deletePicture(String imagePath) {

        File fdelete = new File(imagePath);
        if (fdelete.exists()) {

            try {
                fdelete.delete();
                deleteImagePromise.resolve("deleted image!");
                return true;
            } catch (final Exception e) {
                deleteImagePromise.resolve("unable to delete Image: " + e.getMessage());
            }
        }
        return false; 
    }
    

    @ReactMethod
    public void stopAudioRecording(final Promise promise) {
        
        if (audioRecorderPromise != null) {

            audioFile = new File(audioFileName);
            audioFilePath = audioFile.getAbsolutePath();



            Intent intent = new Intent(Intent.ACTION_MEDIA_SCANNER_SCAN_FILE);         
            intent.setData(Uri.fromFile(audioFile));
            _reactContext.sendBroadcast(intent);
            releaseAudioRecorder();
            storeFile();
            promise.resolve("finished recording");
        } else {
            promise.resolve("not recording");
        }
    }

    private void storeFile() {

        // MediaScannerConnection.scanFile(_reactContext,
        //               new String[] { audioFilePath }, null,
        //               new MediaScannerConnection.OnScanCompletedListener() {
        //           public void onScanCompleted(String path, Uri uri) {
        //           }
        //     });

        // values = new ContentValues();
        // values.put(MediaStore.Audio.Media.TITLE, audioFileName);
        // // values.put(MediaStore.Audio.Media.DESCRIPTION, "ayy");
        // values.put(MediaStore.Audio.Media.DATE_ADDED, System.currentTimeMillis());
        // values.put(MediaStore.Audio.Media.MIME_TYPE, "audio/3gpp");
        // values.put(MediaStore.Audio.Media.DATA, audioFileName);
        // // _reactContext.getContentResolver().insert(MediaStore.Audio.Media.EXTERNAL_CONTENT_URI,values);
        // ContentResolver cr =  _reactContext.getContentResolver();
        // cr.insert(MediaStore.Audio.Media.EXTERNAL_CONTENT_URI, values);
        // Uri base = MediaStore.Audio.Media.EXTERNAL_CONTENT_URI;
        // Uri newUri = cr.insert(base, values);
        // Intent intent = new Intent(Intent.ACTION_MEDIA_SCANNER_SCAN_FILE);         
        // intent.setData(Uri.fromFile(audioFile));
        // _reactContext.sendBroadcast(intent);        
    }

    private void releaseAudioRecorder() {
        if (audioRecorder != null) {
            audioRecorder.stop();
            audioRecorder.release();
             audioRecorder = null;
            if (audioRecorderPromise != null) {
                // audioRecorderPromise.resolve(Uri.fromFile(audioFile).toString());
                audioRecorderPromise.resolve(audioFileName);
                audioRecorderPromise = null;
            }   
        }       
    }

    private boolean prepareMediaRecorder(String captureQuality, int target) {

          int quality = CamcorderProfile.QUALITY_480P; 
        switch (captureQuality) {
             case "low":
                quality = CamcorderProfile.QUALITY_LOW; // select the lowest res
                 break;
             case "medium":
                 quality = CamcorderProfile.QUALITY_720P; // select medium
                 break;
             case "high":
                quality = CamcorderProfile.QUALITY_HIGH; // select the highest res (default)
                 break;
         }
 
        mediaRecorder = new MediaRecorder();
        mCamera.unlock();  // make available for mediarecorder
        mediaRecorder.setCamera(mCamera);
 
         int actualDeviceOrientation = (90 + ((720 - RCTCamera.getInstance().getActualDeviceOrientation() * 90))) % 360;
 
        mediaRecorder.setAudioSource(MediaRecorder.AudioSource.CAMCORDER);
        mediaRecorder.setVideoSource(MediaRecorder.VideoSource.CAMERA);
        mediaRecorder.setProfile(CamcorderProfile.get(CamcorderProfile.QUALITY_HIGH));
         mediaRecorder.setOrientationHint(actualDeviceOrientation);
 
       
 
         // videoFile = null;
         // switch (target) {
         //     case RCT_CAMERA_CAPTURE_TARGET_MEMORY:
         //        break;
         //     case RCT_CAMERA_CAPTURE_TARGET_CAMERA_ROLL:
         //        videoFile = getOutputMediaFile(MEDIA_TYPE_VIDEO);

         //     case RCT_CAMERA_CAPTURE_TARGET_TEMP:
         //         videoFile = getTempMediaFile(MEDIA_TYPE_VIDEO);
         //         break;
         //     default:
         //    case RCT_CAMERA_CAPTURE_TARGET_DISK:
         //         videoFile = getOutputMediaFile(MEDIA_TYPE_VIDEO);
         //         break;
         // }
         // if (videoFile == null) {
         //     return false;
         // }

        videoFileName = Environment.getExternalStorageDirectory().getAbsolutePath();
        String timeStamp = new SimpleDateFormat("yyyyMMdd_HHmmss").format(new Date());
         videoFileName += "/" + timeStamp;
         videoFile = getOutputMediaFile(MEDIA_TYPE_VIDEO);
         mediaRecorder.setOutputFile(getOutputMediaFile(MEDIA_TYPE_VIDEO).toString());
         // mediaRecorder.setOutputFile(videoFileName);
         mediaRecorder.setMaxDuration(3000000); // Set max duration 60 sec.
         // mediaRecorder.setMaxFileSize(15000000); // Set max file size 15MB
         try {
             mediaRecorder.prepare();
         } catch (IllegalStateException e) {
             releaseMediaRecorder();
             return false;
         } catch (IOException e) {
             releaseMediaRecorder();
             return false;
         }
         return true;
     }
 
     @ReactMethod
     private void record(final ReadableMap options, final Promise promise) {
         if (recordingPromise == null) {
             mCamera = RCTCamera.getInstance().acquireCameraInstance(options.getInt("type"));
             if (null == mCamera) {
                 promise.reject("No camera found.");
                 return;
             }
             if (!prepareMediaRecorder(options.getString("quality"), options.getInt("target"))) {
                 promise.reject("Fail in prepareMediaRecorder()!");
                 return;
             }
             try {    
                 mediaRecorder.start();
                 recordingPromise = promise;  // only got here if mediaRecorder started
             } catch (final Exception ex) {
                promise.reject("Exception in thread");
                 return;
             }
         }
     }

         @ReactMethod
    public void stopCapture(final Promise promise) {
        if (recordingPromise != null) {
             // videoFile = new File(videoFileName);
            Intent intent = new Intent(Intent.ACTION_MEDIA_SCANNER_SCAN_FILE);         
            intent.setData(Uri.fromFile(videoFile));
            _reactContext.sendBroadcast(intent);
             releaseMediaRecorder(); // release the MediaRecorder object
             promise.resolve("Finished recording.");
         } else {
             promise.resolve("Not recording.");
         }
      }
 
     private void releaseMediaRecorder() {
        if (mediaRecorder != null) {
             if (recordingPromise != null) mediaRecorder.stop(); // stop the recording
             mediaRecorder.reset(); // clear recorder configuration
             mediaRecorder.release(); // release the recorder object
             mediaRecorder = null;
         }
        if (mCamera != null) {
             mCamera.lock(); // relock camera for later use since we unlocked it
             mCamera = null;
        }
        if (recordingPromise != null) {
            recordingPromise.resolve(Uri.fromFile(videoFile).toString());
            recordingPromise = null;
         }
     }

    @Nullable
    @Override
    public Map<String, Object> getConstants() {
        return Collections.unmodifiableMap(new HashMap<String, Object>() {
            {
                put("Aspect", getAspectConstants());
                put("Type", getTypeConstants());
                put("CaptureQuality", getCaptureQualityConstants());
                put("CaptureMode", getCaptureModeConstants());
                put("CaptureTarget", getCaptureTargetConstants());
                put("Orientation", getOrientationConstants());
                put("FlashMode", getFlashModeConstants());
                put("TorchMode", getTorchModeConstants());
            }

            private Map<String, Object> getAspectConstants() {
                return Collections.unmodifiableMap(new HashMap<String, Object>() {
                    {
                        put("stretch", RCT_CAMERA_ASPECT_STRETCH);
                        put("fit", RCT_CAMERA_ASPECT_FIT);
                        put("fill", RCT_CAMERA_ASPECT_FILL);
                    }
                });
            }

            private Map<String, Object> getTypeConstants() {
                return Collections.unmodifiableMap(new HashMap<String, Object>() {
                    {
                        put("front", RCT_CAMERA_TYPE_FRONT);
                        put("back", RCT_CAMERA_TYPE_BACK);
                    }
                });
            }

            private Map<String, Object> getCaptureQualityConstants() {
                return Collections.unmodifiableMap(new HashMap<String, Object>() {
                    {
                        put("low", "low");
                        put("medium", "medium");
                        put("high", "high");
                    }
                });
            }

            private Map<String, Object> getCaptureModeConstants() {
                return Collections.unmodifiableMap(new HashMap<String, Object>() {
                    {
                        put("still", RCT_CAMERA_CAPTURE_MODE_STILL);
                        put("video", RCT_CAMERA_CAPTURE_MODE_VIDEO);
                    }
                });
            }

            private Map<String, Object> getCaptureTargetConstants() {
                return Collections.unmodifiableMap(new HashMap<String, Object>() {
                    {
                        put("memory", RCT_CAMERA_CAPTURE_TARGET_MEMORY);
                        put("disk", RCT_CAMERA_CAPTURE_TARGET_DISK);
                        put("cameraRoll", RCT_CAMERA_CAPTURE_TARGET_CAMERA_ROLL);
                        put("temp", RCT_CAMERA_CAPTURE_TARGET_TEMP);
                    }
                });
            }

            private Map<String, Object> getOrientationConstants() {
                return Collections.unmodifiableMap(new HashMap<String, Object>() {
                    {
                        put("auto", RCT_CAMERA_ORIENTATION_AUTO);
                        put("landscapeLeft", RCT_CAMERA_ORIENTATION_LANDSCAPE_LEFT);
                        put("landscapeRight", RCT_CAMERA_ORIENTATION_LANDSCAPE_RIGHT);
                        put("portrait", RCT_CAMERA_ORIENTATION_PORTRAIT);
                        put("portraitUpsideDown", RCT_CAMERA_ORIENTATION_PORTRAIT_UPSIDE_DOWN);
                    }
                });
            }

            private Map<String, Object> getFlashModeConstants() {
                return Collections.unmodifiableMap(new HashMap<String, Object>() {
                    {
                        put("off", RCT_CAMERA_FLASH_MODE_OFF);
                        put("on", RCT_CAMERA_FLASH_MODE_ON);
                        put("auto", RCT_CAMERA_FLASH_MODE_AUTO);
                    }
                });
            }

            private Map<String, Object> getTorchModeConstants() {
                return Collections.unmodifiableMap(new HashMap<String, Object>() {
                    {
                        put("off", RCT_CAMERA_TORCH_MODE_OFF);
                        put("on", RCT_CAMERA_TORCH_MODE_ON);
                        put("auto", RCT_CAMERA_TORCH_MODE_AUTO);
                    }
                });
            }
        });
    }

    @ReactMethod
    public void capture(final ReadableMap options, final Promise promise) {
        Camera camera = RCTCamera.getInstance().acquireCameraInstance(options.getInt("type"));
        if (camera == null) {
            promise.reject("No camera found.");
            return;
        }
        if (options.getBoolean("playSoundOnCapture")) {
            MediaActionSound sound = new MediaActionSound();
            sound.play(MediaActionSound.SHUTTER_CLICK);
        }
        RCTCamera.getInstance().setCaptureQuality(options.getInt("type"), options.getString("quality"));
         if (options.getInt("mode") == RCT_CAMERA_CAPTURE_MODE_VIDEO) { 
             record(options, promise);
             return;
         }
        camera.takePicture(null, null, new Camera.PictureCallback() {
            @Override
            public void onPictureTaken(byte[] data, Camera camera) {
                camera.stopPreview();
                camera.startPreview();
                switch (options.getInt("target")) {
                    case RCT_CAMERA_CAPTURE_TARGET_MEMORY:
                        String encoded = Base64.encodeToString(data, Base64.DEFAULT);
                        promise.resolve(encoded);
                        break;
                    case RCT_CAMERA_CAPTURE_TARGET_CAMERA_ROLL:
                        BitmapFactory.Options bitmapOptions = new BitmapFactory.Options();
                        Bitmap bitmap = BitmapFactory.decodeByteArray(data, 0, data.length, bitmapOptions);
                        String url = MediaStore.Images.Media.insertImage(
                                _reactContext.getContentResolver(),
                                bitmap, options.getString("title"),
                                options.getString("description"));
                        promise.resolve(url);
                        break;
                    case RCT_CAMERA_CAPTURE_TARGET_DISK:
                        File pictureFile = getOutputMediaFile(MEDIA_TYPE_IMAGE);
                        if (pictureFile == null) {
                            promise.reject("Error creating media file.");
                            return;
                        }

                        try {
                            FileOutputStream fos = new FileOutputStream(pictureFile);
                            fos.write(data);
                            fos.close();
                        } catch (FileNotFoundException e) {
                            promise.reject("File not found: " + e.getMessage());
                        } catch (IOException e) {
                            promise.reject("Error accessing file: " + e.getMessage());
                        }
                        promise.resolve(Uri.fromFile(pictureFile).toString());
                        break;
                    case RCT_CAMERA_CAPTURE_TARGET_TEMP:
                        File tempFile = getTempMediaFile(MEDIA_TYPE_IMAGE);

                        if (tempFile == null) {
                            promise.reject("Error creating media file.");
                            return;
                        }

                        try {
                            FileOutputStream fos = new FileOutputStream(tempFile);
                            fos.write(data);
                            fos.close();
                        } catch (FileNotFoundException e) {
                            promise.reject("File not found: " + e.getMessage());
                        } catch (IOException e) {
                            promise.reject("Error accessing file: " + e.getMessage());
                        }
                        promise.resolve(Uri.fromFile(tempFile).toString());
                        break;
                }
            }
        });
    }



    @ReactMethod
    public void hasFlash(ReadableMap options, final Promise promise) {
        Camera camera = RCTCamera.getInstance().acquireCameraInstance(options.getInt("type"));
        if (null == camera) {
            promise.reject("No camera found.");
            return;
        }
        List<String> flashModes = camera.getParameters().getSupportedFlashModes();
        promise.resolve(null != flashModes && !flashModes.isEmpty());
    }


    private File getOutputMediaFile(int type) {

        String state = Environment.getExternalStorageState();
        if (Environment.MEDIA_MOUNTED.equals(state)) { 

            // File mediaStorageDir = new File(Environment.getExternalStoragePublicDirectory(Environment.DIRECTORY_DOWNLOADS)  + File.separator + "Vidao");
  
            File mediaStorageDir = new File(Environment.getExternalStoragePublicDirectory(Environment.DIRECTORY_PICTURES), "Vidao");
            if (!mediaStorageDir.exists()) {
                if (!mediaStorageDir.mkdirs()) {
                    return null;
                }
            }
            String timeStamp = new SimpleDateFormat("yyyyMMdd_HHmmss").format(new Date());
            File mediaFile;
            if (type == MEDIA_TYPE_IMAGE) {
                mediaFile = new File(mediaStorageDir.getPath() + File.separator +
                        "IMG_" + timeStamp + ".jpg");
            } else if (type == MEDIA_TYPE_VIDEO) {
                mediaFile = new File(mediaStorageDir.getPath() + File.separator +
                        "VID_" + timeStamp + ".mp4");
            } else if (type == MEDIA_TYPE_AUDIO) {
                mediaFile = new File(mediaStorageDir.getPath() + File.separator +
                        "AUD_" + timeStamp + ".wav");
            }
            else {
                return null;
            }
            return mediaFile;
        } else {
            return null;
        }
    }

    private File getTempMediaFile(int type) {
        try {
            String timeStamp = new SimpleDateFormat("yyyyMMdd_HHmmss").format(new Date());
            File outputDir = _reactContext.getCacheDir();
            File outputFile;

            if (type == MEDIA_TYPE_IMAGE) {
                outputFile = File.createTempFile("IMG_" + timeStamp, ".jpg", outputDir);
            } else if (type == MEDIA_TYPE_VIDEO) {
                outputFile = File.createTempFile("VID_" + timeStamp, ".mp4", outputDir);
            } else {
                Log.e(TAG, "Unsupported media type:" + type);
                return null;
            }
            return outputFile;
        } catch (Exception e) {
            Log.e(TAG, e.getMessage());
            return null;
        }
    }
}
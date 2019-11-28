package com.sahni.rahul.facedetector.captureimage;

import android.Manifest;
import android.app.AlertDialog;
import android.content.Intent;
import android.content.pm.PackageManager;
import android.graphics.Matrix;
import android.media.Image;
import android.os.Bundle;

import com.bumptech.glide.Glide;
import com.bumptech.glide.request.RequestOptions;
import com.google.firebase.ml.vision.FirebaseVision;
import com.google.firebase.ml.vision.common.FirebaseVisionImage;
import com.google.firebase.ml.vision.common.FirebaseVisionImageMetadata;
import com.google.firebase.ml.vision.face.FirebaseVisionFaceDetector;
import com.google.firebase.ml.vision.face.FirebaseVisionFaceDetectorOptions;
import com.sahni.rahul.facedetector.R;
import com.sahni.rahul.facedetector.showimage.ShowCapturedImageActivity;
import com.sahni.rahul.facedetector.utils.Constants;
import com.sahni.rahul.facedetector.utils.FileUtils;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.appcompat.app.AppCompatActivity;
//import androidx.camera.core.AspectRatio;
import androidx.camera.core.CameraX;
import androidx.camera.core.ImageAnalysis;
import androidx.camera.core.ImageAnalysisConfig;
import androidx.camera.core.ImageCapture;
import androidx.camera.core.ImageCaptureConfig;
import androidx.camera.core.ImageProxy;
import androidx.camera.core.Preview;
import androidx.camera.core.PreviewConfig;
//import androidx.camera.view.CameraView;
import androidx.core.app.ActivityCompat;
import androidx.core.content.ContextCompat;
import androidx.lifecycle.MutableLiveData;

import android.os.CountDownTimer;
import android.util.DisplayMetrics;
import android.util.Log;
import android.util.Rational;
import android.view.Surface;
import android.view.View;
import android.view.ViewGroup;
import android.widget.Button;
import android.widget.ImageButton;
import android.widget.TextView;
import android.widget.Toast;

import java.io.File;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collections;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.TimeUnit;

public class CaptureImageActivity extends AppCompatActivity {

    private static final String TAG = CaptureImageActivity.class.getSimpleName();

    private static final int MY_PERMISSIONS_REQUEST_CAMERA = 11;

    private FirebaseVisionFaceDetector detector;
    private ImageCapture imageCapture;
    private ImageAnalysis imageAnalysis;
    private FaceAnalyzer faceAnalyzer;
    private CameraX.LensFacing lensFacing = CameraX.LensFacing.FRONT;

    private ArrayList<String> capturedPhotoList;

    private AutoFitTextureView viewFinder;
    private ImageButton thumbnailImageButton;
    private TextView countDownTextView;
    private Button takeMorePhotoButton;
    private TextView searchingTextView;

    private FileUtils fileUtils;
    private boolean lockToggleAndGalleryButton = false;
    private int faceNotDetectedCount = 0;
    private ExecutorService executor = Executors.newSingleThreadExecutor();

    private MutableLiveData<Boolean> searchingLiveData = new MutableLiveData<>();

    private CountDownTimer timer;
    private static final int FACE_NOT_DETECTED_COUNT_CHECK = 3;
    private static final long COUNTDOWN = 3000;
    private static final long COUNTDOWN_INTERVAL = 1000;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_capture_image);

        viewFinder = findViewById(R.id.texture_view);
        thumbnailImageButton = findViewById(R.id.photo_view_button);
        countDownTextView = findViewById(R.id.countdown_text_view);
        takeMorePhotoButton = findViewById(R.id.take_more_photos);
        ImageButton cameraSwitchButton = findViewById(R.id.camera_switch_button);
        searchingTextView = findViewById(R.id.searching_text_view);

        fileUtils = new FileUtils(this);

        FirebaseVisionFaceDetectorOptions realTimeOpts =
                new FirebaseVisionFaceDetectorOptions.Builder()
                        .setContourMode(FirebaseVisionFaceDetectorOptions.ALL_CONTOURS)
                        .build();

        detector = FirebaseVision.getInstance()
                .getVisionFaceDetector(realTimeOpts);

        capturedPhotoList = new ArrayList<>();

        if (isPermissionGranted()) {
            viewFinder.post(() -> {
                startCamera();

                executor.submit(() -> {
                    File[] files = fileUtils.getOutputDirectory().listFiles();
                    if (files != null) {
                        Arrays.sort(files, Collections.reverseOrder());
                        setThumbnail(files[0]);
                        for(File file : files) {
                            capturedPhotoList.add(file.getAbsolutePath());
                        }
                    }
                });
            });
        } else {
            ActivityCompat.requestPermissions(this,
                    new String[]{Manifest.permission.CAMERA},
                    MY_PERMISSIONS_REQUEST_CAMERA);
        }

        searchingLiveData.observe(this, aBoolean -> {
            if(aBoolean) {
                searchingTextView.setVisibility(View.VISIBLE);
            } else {
                searchingTextView.setVisibility(View.INVISIBLE);
            }
        });

        cameraSwitchButton.setOnClickListener( v -> {
            if(!lockToggleAndGalleryButton) {
                if (lensFacing == CameraX.LensFacing.FRONT) {
                    lensFacing = CameraX.LensFacing.BACK;
                } else {
                    lensFacing = CameraX.LensFacing.FRONT;
                }
                resetCamera();
            }
        });

        thumbnailImageButton.setOnClickListener(v -> {
            if(!lockToggleAndGalleryButton && capturedPhotoList.size() > 0) {
                Intent intent = new Intent(CaptureImageActivity.this, ShowCapturedImageActivity.class);
                intent.putStringArrayListExtra(Constants.IMAGE_LIST_PATH_KEY, capturedPhotoList);
                startActivity(intent);
            }
        });

        takeMorePhotoButton.setOnClickListener(v -> {
            imageAnalysis = getImageAnalysis();
            CameraX.bindToLifecycle(CaptureImageActivity.this, imageAnalysis);
            takeMorePhotoButton.setVisibility(View.INVISIBLE);
            searchingTextView.setVisibility(View.VISIBLE);
        });

        viewFinder.addOnLayoutChangeListener((v, left, top, right, bottom, oldLeft, oldTop, oldRight, oldBottom) -> {
            updateTransform();
        });

    }

    private void setThumbnail(File file) {
        thumbnailImageButton.post(() -> {
            int padding = (int) getResources().getDimension(R.dimen.stroke_small);
            thumbnailImageButton.setPadding(padding,padding, padding, padding);
            Glide.with(thumbnailImageButton.getContext())
                    .load(file)
                    .apply(RequestOptions.circleCropTransform())
                    .into(thumbnailImageButton);
        });
    }

    @Override
    public void onRequestPermissionsResult(int requestCode, @NonNull String[] permissions, @NonNull int[] grantResults) {
        if (requestCode == MY_PERMISSIONS_REQUEST_CAMERA) {
            if (grantResults.length > 0
                    && grantResults[0] == PackageManager.PERMISSION_GRANTED) {
                viewFinder.post(this::startCamera);
            } else {
                Toast.makeText(this, "Permission denied!", Toast.LENGTH_SHORT).show();
                finish();
            }
        }
    }


    private void startCamera() {
        Preview preview = getPreview();
        imageCapture = getImageCapture();
        imageAnalysis = getImageAnalysis();
        CameraX.bindToLifecycle(CaptureImageActivity.this, preview, imageCapture, imageAnalysis);
        searchingLiveData.setValue(true);
    }

    private void resetCamera() {
        faceNotDetectedCount = 0;
        stopCamera();
        startCamera();
        takeMorePhotoButton.setVisibility(View.INVISIBLE);
    }

    private void stopCamera() {
        CameraX.unbindAll();
        searchingLiveData.setValue(false);
    }

    private FaceAnalyzer getFaceAnalyzer() {
        if(faceAnalyzer == null) {
            faceAnalyzer = new FaceAnalyzer();
        }
        return faceAnalyzer;
    }

    private ImageAnalysis getImageAnalysis() {
        ImageAnalysisConfig imageAnalysisConfig = new ImageAnalysisConfig.Builder()
                .setImageReaderMode(ImageAnalysis.ImageReaderMode.ACQUIRE_LATEST_IMAGE)
                .setLensFacing(lensFacing)
                .build();

        ImageAnalysis imageAnalysis = new ImageAnalysis(imageAnalysisConfig);
        imageAnalysis.setAnalyzer(getFaceAnalyzer());
        return imageAnalysis;

    }

    private ImageCapture getImageCapture() {
        ImageCaptureConfig imageCaptureConfig = new ImageCaptureConfig.Builder()
                .setCaptureMode(ImageCapture.CaptureMode.MIN_LATENCY)
                .setLensFacing(lensFacing)
                .build();
        return new ImageCapture(imageCaptureConfig);
    }

    private Preview getPreview() {

        DisplayMetrics  displayMetrics = new DisplayMetrics();
        viewFinder.getDisplay().getRealMetrics(displayMetrics);
        viewFinder.setAspectRatio(3,4);
        PreviewConfig previewConfig = new PreviewConfig.Builder()
                .setTargetAspectRatio(new Rational(3, 4))
                .setLensFacing(lensFacing)
                .build();

        Preview preview = new Preview(previewConfig);
        preview.setOnPreviewOutputUpdateListener(output -> {
            ViewGroup viewFinderParent = (ViewGroup) viewFinder.getParent();
            viewFinderParent.removeView(viewFinder);
            viewFinderParent.addView(viewFinder, 0);
            viewFinder.setSurfaceTexture(output.getSurfaceTexture());
            updateTransform();
        });
        return preview;
    }

    private void updateTransform() {

        Matrix matrix = new Matrix();
        float centerX = viewFinder.getWidth() / 2f;
        float centerY = viewFinder.getHeight() / 2f;

        int rotationDegree = 0;
        switch (viewFinder.getDisplay().getRotation()) {
            case Surface.ROTATION_0:
                rotationDegree = 0;
                break;
            case Surface.ROTATION_90:
                rotationDegree = 90;
                break;
            case Surface.ROTATION_180:
                rotationDegree = 180;
                break;
            case Surface.ROTATION_270:
                rotationDegree = 270;
                break;
        }

        matrix.postRotate((float) -rotationDegree, centerX, centerY);
        viewFinder.setTransform(matrix);
    }

    private boolean isPermissionGranted() {
        return ContextCompat.checkSelfPermission(this, Manifest.permission.CAMERA)
                == PackageManager.PERMISSION_GRANTED;
    }

    private class FaceAnalyzer implements ImageAnalysis.Analyzer {

        private long lastAnalyzedTimeStamp = 0L;

        private int degreesToFirebaseRotation(int degrees) {
            switch (degrees) {
                case 0:
                    return FirebaseVisionImageMetadata.ROTATION_0;
                case 90:
                    return FirebaseVisionImageMetadata.ROTATION_90;
                case 180:
                    return FirebaseVisionImageMetadata.ROTATION_180;
                case 270:
                    return FirebaseVisionImageMetadata.ROTATION_270;
                default:
                    throw new IllegalArgumentException(
                            "Rotation must be 0, 90, 180, or 270.");
            }
        }

        @Override
        public void analyze(ImageProxy imageProxy, int degrees) {

            if (imageProxy == null || imageProxy.getImage() == null) {
                return;
            }

            long currentTimeStamp = System.currentTimeMillis();
            if (currentTimeStamp - lastAnalyzedTimeStamp >= TimeUnit.SECONDS.toMillis(3)) {
                Image mediaImage = imageProxy.getImage();
                int rotation = degreesToFirebaseRotation(degrees);
                detectFace(FirebaseVisionImage.fromMediaImage(mediaImage, rotation));
                lastAnalyzedTimeStamp = currentTimeStamp;
            }

        }
    }

    private void detectFace(FirebaseVisionImage image) {
        detector.detectInImage(image)
                .addOnSuccessListener(
                        firebaseVisionFaces -> {
                            if(firebaseVisionFaces.size() == 0) {
                                faceNotDetectedCount++;
                                if(faceNotDetectedCount == FACE_NOT_DETECTED_COUNT_CHECK) {
                                    stopCamera();
                                    AlertDialog alertDialog = new AlertDialog.Builder(CaptureImageActivity.this)
                                            .setCancelable(false)
                                            .setMessage("Face not detected")
                                            .setPositiveButton("Try Again", (dialog, which) -> {
                                                dialog.dismiss();
                                                faceNotDetectedCount = 0;
                                                startCamera();
                                            })
                                            .setNegativeButton("Exit", (dialog, which) -> {
                                                dialog.dismiss();
                                                finish();
                                            })
                                            .create();
                                    alertDialog.show();

                                }
                            } else {

                                searchingLiveData.setValue(false);
                                CameraX.unbind(imageAnalysis);
                                lockToggleAndGalleryButton = true;
                                countDownTextView.setVisibility(View.VISIBLE);

                                if(timer == null) {
                                    timer = new CountDownTimer(COUNTDOWN, COUNTDOWN_INTERVAL) {
                                        @Override
                                        public void onTick(long millisUntilFinished) {
                                            countDownTextView.setText(""+TimeUnit.MILLISECONDS.toSeconds(millisUntilFinished));
                                        }

                                        @Override
                                        public void onFinish() {
                                            countDownTextView.setVisibility(View.INVISIBLE);
                                            faceNotDetectedCount = 0;
                                            File photoFile = fileUtils.createFile(
                                                    Constants.FILENAME_FORMAT,
                                                    Constants.PHOTO_EXTENSION_JPG);

                                            ImageCapture.Metadata metadata = new ImageCapture.Metadata();
                                            metadata.isReversedHorizontal = lensFacing == CameraX.LensFacing.FRONT;

                                            imageCapture.takePicture(photoFile, new ImageCapture.OnImageSavedListener() {
                                                @Override
                                                public void onImageSaved(@NonNull File file) {
                                                    takeMorePhotoButton.setVisibility(View.VISIBLE);
                                                    Toast.makeText(CaptureImageActivity.this, "Photo saved!", Toast.LENGTH_SHORT).show();
                                                    capturedPhotoList.add(0,file.getAbsolutePath());
                                                    setThumbnail(file);
                                                    lockToggleAndGalleryButton = false;
                                                    thumbnailImageButton.performClick();
                                                }

                                                @Override
                                                public void onError(@NonNull ImageCapture.ImageCaptureError imageCaptureError, @NonNull String message, @Nullable Throwable cause) {
                                                    Log.e(TAG, "onError: Couldn't save photo: "+message);
                                                    lockToggleAndGalleryButton = false;
                                                    takeMorePhotoButton.setVisibility(View.VISIBLE);
                                                }
                                            }, metadata);
                                        }
                                    };

                                }
                                timer.start();

                            }
                        })
                .addOnFailureListener(e -> {
                    Log.d(TAG, "onFailure: face not detected");
                    e.printStackTrace();
                });
    }

}

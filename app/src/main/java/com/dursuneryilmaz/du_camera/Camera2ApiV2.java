package com.dursuneryilmaz.du_camera;

import androidx.annotation.NonNull;
import androidx.appcompat.app.AppCompatActivity;
import androidx.core.app.ActivityCompat;

import android.Manifest;
import android.annotation.SuppressLint;
import android.content.Context;
import android.content.pm.PackageManager;
import android.graphics.Camera;
import android.graphics.ImageFormat;
import android.graphics.SurfaceTexture;
import android.hardware.camera2.CameraAccessException;
import android.hardware.camera2.CameraCaptureSession;
import android.hardware.camera2.CameraCharacteristics;
import android.hardware.camera2.CameraDevice;
import android.hardware.camera2.CameraManager;
import android.hardware.camera2.CameraMetadata;
import android.hardware.camera2.CaptureRequest;
import android.hardware.camera2.TotalCaptureResult;
import android.hardware.camera2.params.StreamConfigurationMap;
import android.media.Image;
import android.media.ImageReader;
import android.os.Bundle;
import android.os.Environment;
import android.os.Handler;
import android.os.HandlerThread;
import android.util.Size;
import android.util.SparseIntArray;
import android.view.Surface;
import android.view.TextureView;
import android.view.View;
import android.widget.Button;
import android.widget.ImageButton;
import android.widget.Toast;

import java.io.File;
import java.io.FileNotFoundException;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.OutputStream;
import java.lang.reflect.Array;
import java.nio.ByteBuffer;
import java.text.SimpleDateFormat;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collections;
import java.util.Comparator;
import java.util.Date;
import java.util.List;
import java.util.Objects;
import java.util.UUID;

public class Camera2ApiV2 extends AppCompatActivity {

    private ImageButton btnCapture;
    private ImageButton btnUpload;
    private int mCamera = 1;
    private boolean mIsPreviewing = true;
    private TextureView mTextureView;
    private TextureView.SurfaceTextureListener mtextureListener = new TextureView.SurfaceTextureListener() {
        @Override
        public void onSurfaceTextureAvailable(SurfaceTexture surface, int width, int height) {
            openCamera();
        }

        @Override
        public void onSurfaceTextureSizeChanged(SurfaceTexture surface, int width, int height) {

        }

        @Override
        public boolean onSurfaceTextureDestroyed(SurfaceTexture surface) {
            return false;
        }

        @Override
        public void onSurfaceTextureUpdated(SurfaceTexture surface) {

        }
    };

    // Kamera gereçleri
    private CameraManager mCameraManager;
    private CameraCharacteristics mCameraCharacteristics;
    private String mCameraId;
    private CameraDevice mCameraDevice;
    private CameraCaptureSession mCameraCaptureSession;
    private CaptureRequest.Builder mCaptureRequestBuilder;
    private static final int REQUEST_CAMERA_PERMISSION = 200;
    private boolean mFlashSupported;
    private StreamConfigurationMap mMap;


    private static SparseIntArray ORIENTATIONS = new SparseIntArray();
    static {
        ORIENTATIONS.append(Surface.ROTATION_0, 0);
        ORIENTATIONS.append(Surface.ROTATION_90, 90);
        ORIENTATIONS.append(Surface.ROTATION_180, 180);
        ORIENTATIONS.append(Surface.ROTATION_270, 270);
    }


    //Save to File
    private File file;
    private String mImageFileName;
    private File mImageFolder;
    private Size mImageDimension;
    private ImageReader mImageReader;

    private Handler mBackgroundHandler;
    private HandlerThread mBackgroundThread;


    private SurfaceTexture mTexture;
    private Surface mSurface;


    CameraDevice.StateCallback mStateCallBack = new CameraDevice.StateCallback() {
        @Override
        public void onOpened(@NonNull CameraDevice camera) {
            mCameraDevice = camera;
            createCameraPreview();
        }

        @Override
        public void onDisconnected(@NonNull CameraDevice camera) {
            mCameraDevice.close();
            mCameraDevice = null;

        }

        @Override
        public void onError(@NonNull CameraDevice camera, int error) {
            mCameraDevice.close();
            mCameraDevice = null;

        }
    };

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_camera2_api_v2);

        createImageFolder();

        mTextureView = (TextureView) findViewById(R.id.textureView);
        assert mTextureView != null;

        mTextureView.setSurfaceTextureListener(mtextureListener);
    
        btnCapture = (ImageButton) findViewById(R.id.btnCaptureImage);
        btnCapture.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                takePicture();
                if(mIsPreviewing) {
                    btnCapture.setVisibility(View.INVISIBLE);
                    btnUpload.setVisibility(View.VISIBLE);
                    Toast.makeText(getApplicationContext(), "Fotoğraf Kaydedilecek.",Toast.LENGTH_SHORT).show();
                    mIsPreviewing = false;
                }

            }
        });



        btnUpload = (ImageButton) findViewById(R.id.btnUploadImage);
        btnUpload.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                if(!mIsPreviewing){
                    mIsPreviewing = true;
                    Toast.makeText(getApplicationContext(),"Fotoğraf sunucuya Yüklenecek..",Toast.LENGTH_SHORT).show();
                    btnUpload.setVisibility(View.INVISIBLE);
                    btnCapture.setVisibility(View.VISIBLE);
                }
            }
        });
    }


    private void openCamera() {
        mCameraManager = (CameraManager)getSystemService(Context.CAMERA_SERVICE);
        try
        {
            assert mCameraManager != null;
            for(String cameraId : mCameraManager.getCameraIdList()) {
                mCameraCharacteristics = mCameraManager.getCameraCharacteristics(cameraId);
                if (mCameraCharacteristics.get(CameraCharacteristics.LENS_FACING) == CameraCharacteristics.LENS_FACING_FRONT) {
                    mCameraId = cameraId;
                    continue;
                }

            }

            mCameraCharacteristics = mCameraManager.getCameraCharacteristics(mCameraId);
            mMap = mCameraCharacteristics.get(CameraCharacteristics.SCALER_STREAM_CONFIGURATION_MAP);
            assert mMap != null;
            mImageDimension = mMap.getOutputSizes(SurfaceTexture.class)[0];

            //check realtime permission
            if(ActivityCompat.checkSelfPermission(this, Manifest.permission.CAMERA) != PackageManager.PERMISSION_GRANTED)
            {
                ActivityCompat.requestPermissions(this,new String[]{
                        Manifest.permission.CAMERA,
                        Manifest.permission.WRITE_EXTERNAL_STORAGE
                },REQUEST_CAMERA_PERMISSION);
                return;
            }
            mCameraManager.openCamera(mCameraId,mStateCallBack,mBackgroundHandler);

        }
        catch (CameraAccessException | NullPointerException ex)
        {
            ex.printStackTrace();
        }
    }


    private void createCameraPreview() {
        try {
            mTexture = mTextureView.getSurfaceTexture();
            assert mTexture != null;
            mTexture.setDefaultBufferSize(mImageDimension.getWidth(),mImageDimension.getHeight());
            mSurface = new Surface(mTexture);


            mCaptureRequestBuilder = mCameraDevice.createCaptureRequest(CameraDevice.TEMPLATE_PREVIEW);
            mCaptureRequestBuilder.addTarget(mSurface);

            mCameraDevice.createCaptureSession(Collections.singletonList(mSurface), new CameraCaptureSession.StateCallback() {
                @Override
                public void onConfigured(@NonNull CameraCaptureSession session) {
                    if(mCameraDevice == null)
                        return;
                    mCameraCaptureSession = session;
                    updatePreview();
                }

                @Override
                public void onConfigureFailed(@NonNull CameraCaptureSession session) {
                    Toast.makeText(getApplicationContext(),"Hata",Toast.LENGTH_SHORT).show();
                }
            },mBackgroundHandler);
        }
        catch (CameraAccessException e)
        {
          e.printStackTrace();
        }
    }


    private void updatePreview() {
        if(mCameraDevice == null)
            Toast.makeText(this,"Hata", Toast.LENGTH_SHORT).show();
        mCaptureRequestBuilder.set(CaptureRequest.CONTROL_MODE, CaptureRequest.CONTROL_MODE_AUTO);
        try
        {
         mCameraCaptureSession.setRepeatingRequest(mCaptureRequestBuilder.build(),null, mBackgroundHandler);

        } catch (CameraAccessException e) {
            e.printStackTrace();
        }
    }


    private void takePicture() {
        if(mCameraDevice == null)
            return;
        //mCameraManager = (CameraManager) getSystemService(Context.CAMERA_SERVICE);
        try
        {
            assert mCameraManager != null;

            Size [] jpegSizes = null;

            jpegSizes = Objects.requireNonNull(mCameraCharacteristics.get(CameraCharacteristics.SCALER_STREAM_CONFIGURATION_MAP)).getOutputSizes(ImageFormat.JPEG);

            int width = 640;
            int height = 480;
            if(jpegSizes != null && jpegSizes.length > 0)
            {
                width = jpegSizes[jpegSizes.length-1].getWidth();
                height = jpegSizes[jpegSizes.length-1].getHeight();
            }

            //check rotation based on device
            int rotation = getWindowManager().getDefaultDisplay().getRotation();
            //int jpegOrientation = getJpegOrientation(cameraCharacteristics, rotation);
            mImageReader = ImageReader.newInstance(width,height,ImageFormat.JPEG,1);
            List<Surface> outputSurface = new ArrayList<>(1);
            outputSurface.add(mImageReader.getSurface());
            //outputSurface.add(new Surface(mTextureView.getSurfaceTexture()));

            mCaptureRequestBuilder = mCameraDevice.createCaptureRequest(CameraDevice.TEMPLATE_STILL_CAPTURE);
            mCaptureRequestBuilder.addTarget(mImageReader.getSurface());
            mCaptureRequestBuilder.set(CaptureRequest.CONTROL_MODE, CameraMetadata.CONTROL_MODE_AUTO);



            mCaptureRequestBuilder.set(CaptureRequest.JPEG_ORIENTATION, ORIENTATIONS.get(90));

            file = createImageFileName();
            ImageReader.OnImageAvailableListener mReaderListener = new ImageReader.OnImageAvailableListener() {
                @Override
                public void onImageAvailable(ImageReader reader) {
                    Image image = null;
                    try
                    {
                        image = reader.acquireLatestImage();
                        ByteBuffer buffer = image.getPlanes()[0].getBuffer();
                        byte[] bytes= new byte[buffer.capacity()];
                        buffer.get(bytes);
                        save(bytes);
                    } catch (IOException e)
                    {
                        e.printStackTrace();
                    } finally{
                        {
                            if(image != null)

                                image.close();
                        }
                    }

                }
                private void save(byte[] bytes)  throws IOException{
                    try (OutputStream outputStream = new FileOutputStream(file)) {
                        outputStream.write(bytes);

                    }
                }
            };
            mImageReader.setOnImageAvailableListener(mReaderListener, mBackgroundHandler);
            final CameraCaptureSession.CaptureCallback mCaptureListener = new CameraCaptureSession.CaptureCallback() {
                @Override
                public void onCaptureCompleted(@NonNull CameraCaptureSession session, @NonNull CaptureRequest request, @NonNull TotalCaptureResult result) {
                    super.onCaptureCompleted(session, request, result);
                    Toast.makeText(getApplicationContext(),"Kayıt Edildi! "+file,Toast.LENGTH_SHORT).show();
                    closeCamera();
                    openCamera();
                }
            };
            mCameraDevice.createCaptureSession(outputSurface, new CameraCaptureSession.StateCallback() {
                @Override
                public void onConfigured(@NonNull CameraCaptureSession session) {
                    try
                    {
                        try {
                            mCaptureRequestBuilder = session.getDevice().createCaptureRequest(CameraDevice.TEMPLATE_STILL_CAPTURE);
                            mCaptureRequestBuilder.addTarget(mImageReader.getSurface());
                            mCaptureRequestBuilder.set(CaptureRequest.JPEG_ORIENTATION, mCameraCharacteristics.get(CameraCharacteristics.SENSOR_ORIENTATION));
                            mCaptureRequestBuilder.set(CaptureRequest.JPEG_QUALITY, (byte) 100);
                            //mCaptureRequestBuilder.set(CaptureRequest.CONTROL_AF_TRIGGER, CaptureRequest.CONTROL_AF_TRIGGER_START);
                            mCaptureRequestBuilder.set(CaptureRequest.LENS_OPTICAL_STABILIZATION_MODE, CaptureRequest.LENS_OPTICAL_STABILIZATION_MODE_ON);

                            session.capture(mCaptureRequestBuilder.build(), null, mBackgroundHandler);
                        } catch (CameraAccessException e) {
                            e.printStackTrace();
                        }

                        session.capture(mCaptureRequestBuilder.build(), mCaptureListener, mBackgroundHandler);

                    }
                    catch (CameraAccessException e)
                    {
                        e.printStackTrace();
                    }
                }

                @Override
                public void onConfigureFailed(@NonNull CameraCaptureSession session) {

                }
            },mBackgroundHandler);
        }catch (CameraAccessException | NullPointerException | IOException e)
        {
            e.printStackTrace();
        }
    }


    @Override
    public void onRequestPermissionsResult(int requestCode, @NonNull String[] permissions, @NonNull int[] grantResults) {
        if(requestCode == REQUEST_CAMERA_PERMISSION)
        {
            if(grantResults[0] != PackageManager.PERMISSION_GRANTED)
            {
                Toast.makeText(this,"Kamera izin verilmeden kullanılamaz",Toast.LENGTH_SHORT).show();
                finish();
            }
        }
    }


    @Override
    protected void onResume() {
        super.onResume();
        startBackgroundThread();
        if(mTextureView.isAvailable())
        {
            openCamera();
        }
        else
        {
            mTextureView.setSurfaceTextureListener(mtextureListener);
        }
    }


    @Override
    protected void onPause() {
        closeCamera();
        stopBackgroundThread();
        super.onPause();
    }


    private void closeCamera(){
        if(mCameraDevice != null){
            mCameraDevice.close();
            mCameraDevice = null;
        }
    }


    private void startBackgroundThread(){
        mBackgroundThread = new HandlerThread("Camera2Apiv2 background");
        mBackgroundThread.start();
        mBackgroundHandler = new Handler(mBackgroundThread.getLooper());
    }


    private void stopBackgroundThread(){
        mBackgroundThread.quitSafely();
        try {
            mBackgroundThread.join();
            mBackgroundThread = null;
            mBackgroundHandler = null;
        }catch (InterruptedException e){
            e.printStackTrace();
        }
    }


    private void createImageFolder(){
        File imageFile = Environment.getExternalStoragePublicDirectory(Environment.DIRECTORY_PICTURES);
        mImageFolder = new File(imageFile, "DuCameraImage");
        if(!mImageFolder.exists()){
            mImageFolder.mkdir();
        }
    }


    private File createImageFileName() throws IOException {
        @SuppressLint("SimpleDateFormat") String timestamp = new SimpleDateFormat("yyyyMMdd_HHmmss").format(new Date());
        String prepend = "IMAGE_"+timestamp+"_";
        File imageFile = File.createTempFile(prepend, ".jpg", mImageFolder);
        mImageFileName = imageFile.getAbsolutePath();
        return imageFile;
    }

}

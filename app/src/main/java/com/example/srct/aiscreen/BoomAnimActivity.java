package com.example.srct.aiscreen;

import android.Manifest;
import android.animation.Animator;
import android.animation.AnimatorSet;
import android.animation.ValueAnimator;
import android.app.Activity;
import android.content.Context;
import android.content.Intent;
import android.content.pm.PackageManager;
import android.graphics.Bitmap;
import android.graphics.Rect;
import android.hardware.display.DisplayManager;
import android.hardware.display.VirtualDisplay;
import android.media.Image;
import android.media.ImageReader;
import android.media.projection.MediaProjection;
import android.media.projection.MediaProjectionManager;
import android.net.Uri;
import android.os.Bundle;
import android.os.Environment;
import android.os.Handler;
import android.os.Message;
//import android.support.v4.app.ActivityCompat;
//import android.support.v4.content.ContextCompat;

import android.util.DisplayMetrics;
import android.util.Log;
import android.view.View;
import android.view.WindowManager;
import android.view.animation.Animation;
import android.view.animation.LinearInterpolator;
import android.view.animation.ScaleAnimation;
import android.widget.Button;
import android.widget.FrameLayout;
import android.widget.ImageView;
import android.widget.Toast;

//import com.intsig.csopen.sdk.CSOpenAPI;

import com.samsung.strdivider.DividerEngine;
import com.samsung.strdivider.DividerResultCallBack;
import com.theartofdev.edmodo.cropper.CropImageView;

import java.io.ByteArrayOutputStream;
import java.io.File;
import java.io.FileOutputStream;
import java.io.IOException;
import java.nio.ByteBuffer;
import java.util.ArrayList;

import androidx.core.app.ActivityCompat;
import androidx.core.content.ContextCompat;

public class BoomAnimActivity extends Activity {

    private String TAG = "BoomAnimActivity";
    private FrameLayout mLoopAnimFrame;
    private ImageView mLoopRotateImage;
    private boolean mAnimating = false;
    private boolean mLoopAnimating = false;

    private float mTouchX;
    private float mTouchY;

    private AnimatorSet mTouchAnimation;

    private boolean mTouchAnimating = false;
    private boolean mOcrResult = false;

    static boolean sBoomCancel = false;
    private AnimatorSet mLoopAnimation;


    private Bitmap mBitmap = null;

    private static final int START_ANIMATION = 1;
    private static final int END_ANIMATION = 2;
    private static final int SHOW_SCREENSHOT = 3;
    private Handler mHandler = new Handler() {
        @Override
        public void handleMessage(Message msg) {
            super.handleMessage(msg);
            switch(msg.what) {
                case START_ANIMATION:
                    Log.d(TAG, "lzh START_ANIMATION");
                    startTouchBoomAnimation();
                    break;
                case END_ANIMATION:
                    Log.d(TAG, "lzh END_ANIMATION");
                    if (mTouchAnimation!= null)
                        mTouchAnimation.cancel();
                    if (mLoopAnimation!= null)
                        mLoopAnimation.cancel();
                    mLoopAnimFrame.setVisibility(View.INVISIBLE);
                    mLoopRotateImage.setVisibility(View.INVISIBLE);
                    showScreenshot();
                    mAnimating = false;
                    break;
                default:
                    break;
            }
        }
    };

    ImageView mImage = null;
    CropImageView mCropImageView = null;

    //screenshot
    private FrameLayout mFrameLayout = null;
     private static final int REQUEST_MEDIA_PROJECTION = 1;
    private static final String STATE_RESULT_CODE = "result_code";
    private static final String STATE_RESULT_DATA = "result_data";

    private final int REQUEST_CODE_SAVE_IMAGE_FILE = 110;

    private final static int  REQUEST_CODE_IMAGE_CROP = 111;

    private int mResultCode;
    private Intent mResultData;

    private MediaProjectionManager mMediaProjectionManager;
    private MediaProjection mMediaProjection;
    private VirtualDisplay mVirtualDisplay;
    private WindowManager mWindowManager;
    private ImageReader mImageReader;

    private int mWindowWidth;
    private int mWindowHeight;
    private int mScreenDensity;

    private String mImageName;
    private String mImagePath;
    private Button mButton;



    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.boom_layout);
        if (savedInstanceState != null) {
            mResultCode = savedInstanceState.getInt(STATE_RESULT_CODE);
            mResultData = savedInstanceState.getParcelable(STATE_RESULT_DATA);
        }
        initScreenshot();
        takeScreenshot();
       // Log.d(TAG, "lzh mBitmap" + mBitmap.getHeight() + mBitmap.getHeight());
        mTouchX = getIntent().getIntExtra("boom_startx", 0);
        mTouchY = getIntent().getIntExtra("boom_starty", 0);
        Log.d(TAG, "lzh mTouchX" + mTouchX + " ,mTouchY" + mTouchY);
        mLoopAnimFrame = (FrameLayout)findViewById(R.id.anim_loop);
        mLoopAnimFrame.setVisibility(View.INVISIBLE);
        mLoopRotateImage = (ImageView) findViewById(R.id.loop_rotate);
        mLoopAnimFrame.addOnLayoutChangeListener(new View.OnLayoutChangeListener() {
            @Override
            public void onLayoutChange(View v, int left, int top, int right, int bottom, int oldLeft, int oldTop, int oldRight, int oldBottom) {
                if (!mAnimating) {
                    final int l = left;
                    final int t = top;
                    final int r = right;
                    final int b = bottom;
                    mLoopAnimFrame.post(new Runnable() {
                        @Override
                        public void run() {
                            mLoopAnimFrame.setTranslationX(mTouchX - (r - l) / 2f);
                            mLoopAnimFrame.setTranslationY(mTouchY - (b - t) / 2f);
                            //startTouchBoomAnimation();
                            Log.d(TAG, "lzh onLayoutChange");
                            Message m = new Message();
                            m.what = START_ANIMATION;
                            mHandler.sendMessageDelayed(m, 200);

                            Message msg = new Message();
                            msg.what = END_ANIMATION;
                            mHandler.sendMessageDelayed(msg, 1400);
                        }
                    });
                }
            }
        });
        mImage = (ImageView) findViewById(R.id.screenshot);
        mCropImageView = (CropImageView)findViewById(R.id.cropImageView);

        mFrameLayout = (FrameLayout) findViewById(R.id.frame);

//        Message msg = new Message();
//        msg.what = END_ANIMATION;
//        mHandler.sendMessageDelayed(msg, 1600);

        mButton = (Button)findViewById(R.id.button);
        mButton.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                Bitmap bitmap = mCropImageView.getCroppedImage();
                if (bitmap!=null)
                    Log.d(TAG, "lzh bitmap" + bitmap.getWidth());
                showCardView(bitmap);
                finish();
            }
        });


        DividerEngine.getInstance().initialize(this);

;

    }
    private void showCardView(Bitmap bitmap) {

//            DividerEngine.getInstance().startRecognize(this,bitmap, new DividerResultCallBack() {
//                @Override
//                public void onSuccess(ArrayList<String> listWords) {
//                    Toast.makeText(BoomAnimActivity.this, "Success", Toast.LENGTH_LONG).show();
//                    String result = "";
//                    for (String word : listWords) {
//                        result = result + word + "| ";
//                    }
//                    Log.d(TAG, "lzh result="+result);
//                    //DividerEngine.getInstance().showCards(BoomAnimActivity.this);
//                }
//
//                @Override
//                public void onFailure(String errorMsg) {
//                    Toast.makeText(BoomAnimActivity.this,errorMsg,Toast.LENGTH_LONG).show();
//                }
//            });
        Intent intent = new Intent();
        intent.setClassName("com.example.srct.aiscreen_part2", "com.example.srct.aiscreen_part2.MainActivity");
        ByteArrayOutputStream baos=new ByteArrayOutputStream();
        bitmap.compress(Bitmap.CompressFormat.PNG, 100, baos);
        byte [] bitmapByte =baos.toByteArray();
        intent.putExtra("bitmap", bitmapByte);
        startActivity(intent);

    }
    public static final float TOUCH_SCALE_FROM = 2f;
    public static final float TOUCH_SCALE_TO_1 = 0.2f;
    public static final float TOUCH_SCALE_TO_2 = 1.15f;
    public static final long TOUCH_DELAY = 0;

    public static final long SCALE_1_DURATION = 400;
    public static final long SCALE_2_DURATION = 200;

    public static final float TOUCH_ALPHA_FROM = 0.4f;
    public static final float TOUCH_ALPHA_TO = 1f;

    private static final float LOOP_SCALE_FROM = 1.15f;
    private static final float LOOP_SCALE_TO = 1.1f;
    private static final long SCALE_DURATION = 600;
    private void initScreenshot() {
        mImagePath = Environment.getExternalStorageDirectory().getPath() + "/crosswalk/screenshot/";
        mWindowManager = (WindowManager) getSystemService(Context.WINDOW_SERVICE);
        mWindowWidth = mWindowManager.getDefaultDisplay().getWidth();
        mWindowHeight = mWindowManager.getDefaultDisplay().getHeight();
        DisplayMetrics displayMetrics = new DisplayMetrics();
        mWindowManager.getDefaultDisplay().getMetrics(displayMetrics);
        mScreenDensity = displayMetrics.densityDpi;
        mImageReader = ImageReader.newInstance(mWindowWidth, mWindowHeight, 0x1, 2);
        mMediaProjectionManager = (MediaProjectionManager)
                getSystemService(Context.MEDIA_PROJECTION_SERVICE);
    }


    private void startTouchBoomAnimation() {
        Log.d(TAG, "startTouchBoomAnimation");
//        if (sBoomCancel) {
//            return;
//        }
        mAnimating = true;
        mLoopAnimFrame.setVisibility(View.INVISIBLE);
        mLoopRotateImage.setVisibility(View.INVISIBLE);
        // init scale to TOUCH_SCALE_FROM
        mLoopAnimFrame.setScaleX(TOUCH_SCALE_FROM);
        mLoopAnimFrame.setScaleY(TOUCH_SCALE_FROM);
        mLoopAnimFrame.setAlpha(TOUCH_ALPHA_FROM);

        AnimatorSet setAnim = new AnimatorSet();
        // anim scale from TOUCH_SCALE_FROM to TOUCH_SCALE_TO_1
        ValueAnimator scaleAnimation = new ValueAnimator().ofFloat(TOUCH_SCALE_FROM, TOUCH_SCALE_TO_1);
        scaleAnimation.addUpdateListener(new ValueAnimator.AnimatorUpdateListener() {

            @Override
            public void onAnimationUpdate(ValueAnimator animation) {
                float animatorValue = (Float)animation.getAnimatedValue();

                mLoopAnimFrame.setScaleX(animatorValue);
                mLoopAnimFrame.setScaleY(animatorValue);
            }
        });
        scaleAnimation.addListener(new Animator.AnimatorListener() {
            @Override
            public void onAnimationStart(Animator animation) {
                Log.d(TAG, "touch scale start");
                mTouchAnimating = true;
                mLoopAnimFrame.setVisibility(View.VISIBLE);
            }

            @Override
            public void onAnimationEnd(Animator animation) {
            }

            @Override
            public void onAnimationCancel(Animator animation) {
            }

            @Override
            public void onAnimationRepeat(Animator animation) {
            }
        });

        scaleAnimation.setDuration(SCALE_1_DURATION);

        ValueAnimator alphaAnimation = new ValueAnimator().ofFloat(TOUCH_ALPHA_FROM, TOUCH_ALPHA_TO);
        alphaAnimation.addUpdateListener(new ValueAnimator.AnimatorUpdateListener() {

            @Override
            public void onAnimationUpdate(ValueAnimator animation) {
                float animatorValue = (Float)animation.getAnimatedValue();

                mLoopAnimFrame.setAlpha(animatorValue);
            }
        });

        alphaAnimation.setDuration(SCALE_1_DURATION);
        setAnim.playTogether(scaleAnimation, alphaAnimation);

        // anim scale from TOUCH_SCALE_TO_1 to TOUCH_SCALE_TO_2
        ValueAnimator scaleAnimation2 = new ValueAnimator().ofFloat(TOUCH_SCALE_TO_1, TOUCH_SCALE_TO_2);
        scaleAnimation2.addUpdateListener(new ValueAnimator.AnimatorUpdateListener() {

            @Override
            public void onAnimationUpdate(ValueAnimator animation) {
                float animatorValue = (Float)animation.getAnimatedValue();

                mLoopAnimFrame.setScaleX(animatorValue);
                mLoopAnimFrame.setScaleY(animatorValue);
            }
        });

        scaleAnimation2.setDuration(SCALE_2_DURATION);

        mTouchAnimation = new AnimatorSet();
        mTouchAnimation.playSequentially(setAnim, scaleAnimation2);
        mTouchAnimation.addListener(new Animator.AnimatorListener() {
            @Override
            public void onAnimationStart(Animator animation) {
            }

            @Override
            public void onAnimationEnd(Animator animation) {
                Log.d(TAG, "touch scale end");
                if (!mTouchAnimating) {
                    return;
                }
                mLoopRotateImage.setVisibility(View.VISIBLE);
                mLoopAnimFrame.setAlpha(1f);
                if (!mOcrResult) {
                    startLoopAnimation();
                }
                mTouchAnimating = false;
            }

            @Override
            public void onAnimationCancel(Animator animation) {
                Log.d(TAG, "touch scale cancel");
                mTouchAnimating = false;
            }

            @Override
            public void onAnimationRepeat(Animator animation) {

            }
        });
        mTouchAnimation.setInterpolator(new CubicInInterpolator());
        mTouchAnimation.setStartDelay(TOUCH_DELAY);
        mTouchAnimation.start();
    }

    private void showScreenshot() {
       // mImage.setVisibility(View.VISIBLE);
        ScaleAnimation scaleAnimation = new ScaleAnimation(1f, 0.85f, 1f, 0.85f,mImage.getWidth() / 2f, mImage.getHeight() / 2f);
        scaleAnimation.setDuration(300);
        scaleAnimation.setFillAfter(true);
        scaleAnimation.setAnimationListener(new Animation.AnimationListener() {
            @Override
            public void onAnimationStart(Animation animation) {
                mFrameLayout.setBackgroundColor(R.color.darkBackground);
            }

            @Override
            public void onAnimationEnd(Animation animation) {
               // mFrameLayout.setBackgroundColor(R.color.darkBackground);
                mButton.setVisibility(View.VISIBLE);
            }

            @Override
            public void onAnimationRepeat(Animation animation) {

            }
        });
        //mImage.startAnimation(scaleAnimation);
        mCropImageView.setImageBitmap(mBitmap);
        mCropImageView.setAutoZoomEnabled(false);
        DisplayMetrics dm = new DisplayMetrics();
        mWindowManager.getDefaultDisplay().getMetrics(dm);
        int width = dm.widthPixels;
        int height = dm.heightPixels;

        int top = (int)(mTouchY < 400 ? 0 : (mTouchY - 400));
        top = top + 800 > height ? height - 800  : top;
        mCropImageView.setCropRect(new Rect(20, top, width - 20,top + 800));
        mCropImageView.startAnimation(scaleAnimation);
       // mCropImageView.setCropRect(new Rect(100, 400, 500, 800));

    }



    private void startLoopAnimation() {
//        if (sBoomCancel && !mOcrStarted || mOcrResult) {
//            return;
//        }
        ValueAnimator scaleIn = new ValueAnimator().ofFloat(LOOP_SCALE_FROM, LOOP_SCALE_TO);
        scaleIn.addUpdateListener(new ValueAnimator.AnimatorUpdateListener() {

            @Override
            public void onAnimationUpdate(ValueAnimator animation) {
                float animatorValue = (Float)animation.getAnimatedValue();
                mLoopAnimFrame.setScaleX(animatorValue);
                mLoopAnimFrame.setScaleY(animatorValue);
            }
        });

        scaleIn.setDuration(SCALE_DURATION);
        scaleIn.setInterpolator(new SineInoutInterpolator());

        ValueAnimator scaleOut = new ValueAnimator().ofFloat(LOOP_SCALE_TO, LOOP_SCALE_FROM);
        scaleOut.addUpdateListener(new ValueAnimator.AnimatorUpdateListener() {

            @Override
            public void onAnimationUpdate(ValueAnimator animation) {
                float animatorValue = (Float)animation.getAnimatedValue();
                mLoopAnimFrame.setScaleX(animatorValue);
                mLoopAnimFrame.setScaleY(animatorValue);
            }
        });

        scaleOut.setDuration(SCALE_DURATION);
        scaleOut.setInterpolator(new SineInoutInterpolator());

        AnimatorSet scaleAnimation = new AnimatorSet();
        scaleAnimation.playSequentially(scaleIn, scaleOut);
        scaleAnimation.addListener(new Animator.AnimatorListener() {
            @Override
            public void onAnimationStart(Animator animation) {

            }

            @Override
            public void onAnimationEnd(Animator animation) {
                if (mLoopAnimating && !mOcrResult) {
                    animation.start();
                }
            }

            @Override
            public void onAnimationCancel(Animator animation) {

            }

            @Override
            public void onAnimationRepeat(Animator animation) {

            }
        });

        ValueAnimator rotateAnimation = new ValueAnimator().ofInt(0, 360);
        mLoopAnimFrame.setPivotX(mLoopAnimFrame.getWidth() / 2f);
        mLoopAnimFrame.setPivotY(mLoopAnimFrame.getHeight() / 2f);
        rotateAnimation.addUpdateListener(new ValueAnimator.AnimatorUpdateListener() {

            @Override
            public void onAnimationUpdate(ValueAnimator animation) {
                int animatorValue = (Integer) animation.getAnimatedValue();

                mLoopAnimFrame.setRotation(animatorValue);
            }
        });
        rotateAnimation.setDuration(SCALE_DURATION * 2);
        rotateAnimation.setRepeatMode(ValueAnimator.RESTART);
        rotateAnimation.setRepeatCount(ValueAnimator.INFINITE);
        rotateAnimation.setInterpolator(new LinearInterpolator());

        mLoopAnimation = new AnimatorSet();
        mLoopAnimation.playTogether(scaleAnimation, rotateAnimation);
        mLoopAnimation.addListener(new Animator.AnimatorListener() {
            @Override
            public void onAnimationStart(Animator animation) {
                mLoopAnimating = true;
            }

            @Override
            public void onAnimationEnd(Animator animation) {
                mLoopAnimating = false;
            }

            @Override
            public void onAnimationCancel(Animator animation) {
                mLoopAnimating = false;
            }

            @Override
            public void onAnimationRepeat(Animator animation) {

            }
        });
        mLoopAnimation.start();
    }

    private void stopBoomAnimation() {
    }


    private void startCapture() {
        mImageName = System.currentTimeMillis() + ".png";
        Log.i(TAG, "image name is : " + mImageName);
        Image image = mImageReader.acquireLatestImage();
        if (image == null) {
            Log.e(TAG, "image is null.");
            return ;
        }
        int width = image.getWidth();
        int height = image.getHeight();
        final Image.Plane[] planes = image.getPlanes();
        final ByteBuffer buffer = planes[0].getBuffer();
        int pixelStride = planes[0].getPixelStride();
        int rowStride = planes[0].getRowStride();
        int rowPadding = rowStride - pixelStride * width;
        mBitmap = Bitmap.createBitmap(width + rowPadding / pixelStride, height, Bitmap.Config.ARGB_8888);
        mBitmap.copyPixelsFromBuffer(buffer);
        mBitmap = Bitmap.createBitmap(mBitmap, 0, 0, width, height);
        image.close();

        stopScreenCapture();
        if (mBitmap != null) {
            Log.d(TAG, "bitmap create success ");
            if (mImage != null) {
                mImage.setImageBitmap(mBitmap);
            }
            checkPermission();
        }
        //mCropImageView.setImageBitmap(mBitmap);

    }
    public static boolean checkSelfPermission(Context context, String permission) {
        return ContextCompat.checkSelfPermission(context, permission) != PackageManager.PERMISSION_GRANTED;
    }
    private void checkPermission() {

        if (checkSelfPermission(this, Manifest.permission.WRITE_EXTERNAL_STORAGE)) {
            if (ActivityCompat.shouldShowRequestPermissionRationale(this, Manifest.permission.WRITE_EXTERNAL_STORAGE)) {
                requestPermissions(new String[]{Manifest.permission.WRITE_EXTERNAL_STORAGE}, REQUEST_CODE_SAVE_IMAGE_FILE);
            } else {
                requestPermissions(new String[]{Manifest.permission.WRITE_EXTERNAL_STORAGE}, REQUEST_CODE_SAVE_IMAGE_FILE);
            }
            return;
        } else {

            saveToFile();

        }

    }
    private void saveToFile() {
        try {
            File fileFolder = new File(mImagePath);
            if (!fileFolder.exists())
                fileFolder.mkdirs();
            File file = new File(mImagePath, mImageName);
            if (!file.exists()) {
                Log.d(TAG, "file create success ");
                file.createNewFile();
            }
            FileOutputStream out = new FileOutputStream(file);
            mBitmap.compress(Bitmap.CompressFormat.PNG, 100, out);
            out.flush();
            out.close();
            Log.d(TAG, "file save success ");
            Toast.makeText(this.getApplicationContext(), "Screenshot is done.", Toast.LENGTH_SHORT).show();
        } catch (IOException e) {
            Log.e(TAG, e.toString());
            e.printStackTrace();
        }
    }
    @Override
    protected void onActivityResult(int requestCode, int resultCode, Intent data) {
        if (requestCode == REQUEST_MEDIA_PROJECTION) {
            if (resultCode != Activity.RESULT_OK) {
                Log.d(TAG, "User cancelled");
                Toast.makeText(this, "User cancelled", Toast.LENGTH_SHORT).show();
                return;
            }
            if (this == null) {
                return;
            }
            Log.d(TAG, "Starting screen capture");
            mResultCode = resultCode;
            mResultData = data;
            setUpMediaProjection();
            setUpVirtualDisplay();
            Handler handler = new Handler();
            handler.postDelayed(new Runnable() {
                @Override
                public void run() {
                    Log.e(TAG, "start startCapture");
                    startCapture();
                }
            }, 200);
        }

    }

    private void setUpMediaProjection() {
        mMediaProjection = mMediaProjectionManager.getMediaProjection(mResultCode, mResultData);
    }

    private void setUpVirtualDisplay() {

        mVirtualDisplay = mMediaProjection.createVirtualDisplay("ScreenCapture",
                mWindowWidth, mWindowHeight, mScreenDensity,
                DisplayManager.VIRTUAL_DISPLAY_FLAG_AUTO_MIRROR,
                mImageReader.getSurface(), null, null);

    }

    private boolean startScreenCapture() {
        if (this == null) {
            return false;
        }
        if (mMediaProjection != null) {
            setUpVirtualDisplay();
            return true;
        } else if (mResultCode != 0 && mResultData != null) {
            setUpMediaProjection();
            setUpVirtualDisplay();
            return true;
        } else {
            Log.d(TAG, "Requesting confirmation");
            // This initiates a prompt dialog for the user to confirm screen projection.
            startActivityForResult(
                    mMediaProjectionManager.createScreenCaptureIntent(),
                    REQUEST_MEDIA_PROJECTION);
            return false;
        }
    }

    private void stopScreenCapture() {
        if (mVirtualDisplay != null) {
            mVirtualDisplay.release();
            mVirtualDisplay = null;
        }
    }

    private void takeScreenshot() {
        if (startScreenCapture()) {
            Handler handler = new Handler();
            handler.postDelayed(new Runnable() {
                @Override
                public void run() {
                    Log.e(TAG, "start startCapture");
                    startCapture();
                }
            }, 200);
        }
    }




}

/*===============================================================================
Copyright (c) 2018 PTC Inc. All Rights Reserved.

Copyright (c) 2012-2014 Qualcomm Connected Experiences, Inc. All Rights Reserved.

Vuforia is a trademark of PTC Inc., registered in the United States and other 
countries.
===============================================================================*/

package com.vuforia.engine.CoreSamples.app.ModelTargetsTrained;

import java.util.ArrayList;
import java.util.Vector;

import android.app.Activity;
import android.app.AlertDialog;
import android.content.DialogInterface;
import android.content.pm.ActivityInfo;
import android.content.res.Configuration;
import android.graphics.Color;
import android.os.Bundle;
import android.os.Handler;
import android.util.Log;
import android.view.GestureDetector;
import android.view.MotionEvent;
import android.view.View;
import android.view.ViewGroup.LayoutParams;
import android.widget.Button;
import android.widget.ImageButton;
import android.widget.ImageView;
import android.widget.RelativeLayout;
import android.widget.TextView;
import android.widget.Toast;

import com.vuforia.CameraDevice;
import com.vuforia.ModelTarget;
import com.vuforia.ObjectTarget;
import com.vuforia.ObjectTracker;
import com.vuforia.PositionalDeviceTracker;
import com.vuforia.State;
import com.vuforia.STORAGE_TYPE;
import com.vuforia.TargetFinder;
import com.vuforia.TargetFinderQueryResult;
import com.vuforia.Tracker;
import com.vuforia.TrackerManager;
import com.vuforia.TrackableResult;
import com.vuforia.ModelTargetResult;
import com.vuforia.Vuforia;
import com.vuforia.engine.CoreSamples.app.ModelTargetsTrained.ModelTargetTrainedRenderer.GuideViewStatus;
import com.vuforia.engine.CoreSamples.app.ModelTargetsTrained.ModelTargetTrainedRenderer.GuideViewModels;
import com.vuforia.engine.CoreSamples.ui.SampleAppMessage;
import com.vuforia.engine.SampleApplication.utils.SampleAppTimer;
import com.vuforia.engine.SampleApplication.SampleApplicationControl;
import com.vuforia.engine.SampleApplication.SampleApplicationException;
import com.vuforia.engine.SampleApplication.SampleApplicationSession;
import com.vuforia.engine.SampleApplication.utils.LoadingDialogHandler;
import com.vuforia.engine.SampleApplication.utils.SampleApplicationGLView;
import com.vuforia.engine.SampleApplication.utils.Texture;
import com.vuforia.engine.CoreSamples.R;
import com.vuforia.engine.CoreSamples.ui.SampleAppMenu.SampleAppMenu;
import com.vuforia.engine.CoreSamples.ui.SampleAppMenu.SampleAppMenuGroup;
import com.vuforia.engine.CoreSamples.ui.SampleAppMenu.SampleAppMenuInterface;


public class ModelTargetsTrained extends Activity implements SampleApplicationControl,
    SampleAppMenuInterface
{
    private static final String LOGTAG = "ModelTargetsTrained";
    
    SampleApplicationSession vuforiaAppSession;
    
    TargetFinder mTargetFinder;

    // Our OpenGL view:
    private SampleApplicationGLView mGlView;
    
    // Our renderer:
    private ModelTargetTrainedRenderer mRenderer;
    
    private GestureDetector mGestureDetector;
    
    // The textures we will use for rendering:
    private Vector<Texture> mTextures;
    
    private RelativeLayout mUILayout;
    private View mSearchingReticleView;
    private View mInitialScreenView;
    private View mAllModelsDetectedView;
    private View mMainARView;
    private ImageView mLanderStatusView;
    private ImageView mBikeStatusView;

    private boolean mIsTutorialComplete = false;

    private SampleAppMenu mSampleAppMenu;
    ArrayList<View> mSettingsAdditionalViews = new ArrayList<>();

    private boolean mContAutofocus = false;

    LoadingDialogHandler loadingDialogHandler = new LoadingDialogHandler(this);
    
    // Alert Dialog used to display SDK errors
    private AlertDialog mErrorDialog;

    GuideViewStatus mBikeGuideViewStatus;
    GuideViewStatus mLanderGuideViewStatus;

    private SampleAppMessage mSampleAppMessage;
    private SampleAppTimer mRelocalizationTimer;
    private SampleAppTimer mStatusDelayTimer;

    private int mCurrentStatusInfo;

    // Called when the activity first starts or the user navigates back to an
    // activity.
    @Override
    protected void onCreate(Bundle savedInstanceState)
    {
        Log.d(LOGTAG, "onCreate");
        super.onCreate(savedInstanceState);
        
        vuforiaAppSession = new SampleApplicationSession(this, CameraDevice.MODE.MODE_OPTIMIZE_SPEED);
        
        startLoadingAnimation();

        mBikeGuideViewStatus = GuideViewStatus.PASSIVE;
        mLanderGuideViewStatus = GuideViewStatus.PASSIVE;

        setViewsReferences();

        showSearchReticle(false);
        showMainARView(false);
        showInitialScreen(false);
        showAllModelsDetectedView(false);

        vuforiaAppSession
            .initAR(this, ActivityInfo.SCREEN_ORIENTATION_LANDSCAPE);
        
        mGestureDetector = new GestureDetector(this, new GestureListener());
        
        // Load any sample specific textures:
        mTextures = new Vector<>();
        loadTextures();

        // Relocalization timer and message
        mSampleAppMessage = new SampleAppMessage(this, mUILayout, mUILayout.findViewById(R.id.topbar_layout), false);
        mRelocalizationTimer = new SampleAppTimer(10000, 1000)
        {
            @Override
            public void onFinish()
            {
                if (vuforiaAppSession != null)
                {
                    vuforiaAppSession.resetDeviceTracker();
                }

                super.onFinish();
            }
        };

        mStatusDelayTimer = new SampleAppTimer(1000, 1000)
        {
            @Override
            public void onFinish()
            {
                if (!mRelocalizationTimer.isRunning())
                {
                    mRelocalizationTimer.startTimer();
                }

                runOnUiThread(new Runnable()
                {
                    @Override
                    public void run()
                    {
                        mSampleAppMessage.show(getString(R.string.instruct_relocalize));
                    }
                });

                super.onFinish();
            }
        };
    }

    void setViewsReferences()
    {
        mSearchingReticleView = findViewById(R.id.searching_reticle_view);
        mInitialScreenView = findViewById(R.id.initial_screen_view);
        mAllModelsDetectedView = findViewById(R.id.all_models_detected_view);
        mMainARView = findViewById(R.id.main_ar_view);
        mLanderStatusView = findViewById(R.id.lander_status_view);
        mBikeStatusView = findViewById(R.id.bike_status_view);

        mSettingsAdditionalViews.add(mMainARView);
        mSettingsAdditionalViews.add(mInitialScreenView);
        mSettingsAdditionalViews.add(mAllModelsDetectedView);

        Button allModelDetectedContinueButton = findViewById(R.id.all_models_detected_continue_button);
        Button initialScreenContinueButton = findViewById(R.id.initial_screen_get_started_button);

        allModelDetectedContinueButton.setOnClickListener(new View.OnClickListener()
        {
            @Override
            public void onClick(View view)
            {
                dismissDetectedAllTargetsView();
            }
        });

        initialScreenContinueButton.setOnClickListener(new View.OnClickListener()
        {
            @Override
            public void onClick(View view)
            {
                dismissInitialScreen();
            }
        });
    }
    
    // Process Single Tap event to trigger autofocus
    private class GestureListener extends
        GestureDetector.SimpleOnGestureListener
    {
        // Used to set autofocus one second after a manual focus is triggered
        private final Handler autofocusHandler = new Handler();
        
        
        @Override
        public boolean onDown(MotionEvent e)
        {
            return true;
        }
        
        
        @Override
        public boolean onSingleTapUp(MotionEvent e)
        {
            // Generates a Handler to trigger autofocus
            // after 1 second
            autofocusHandler.postDelayed(new Runnable()
            {
                public void run()
                {
                    boolean result = CameraDevice.getInstance().setFocusMode(
                        CameraDevice.FOCUS_MODE.FOCUS_MODE_TRIGGERAUTO);
                    
                    if (!result)
                        Log.e("SingleTapUp", "Unable to trigger focus");
                }
            }, 1000L);
            
            return true;
        }
    }
    
    
    // We want to load specific textures from the APK, which we will later use
    // for rendering.
    
    private void loadTextures()
    {
        mTextures.add(Texture.loadTextureFromApk("Lander.png", getAssets()));
        mTextures.add(Texture.loadTextureFromApk("ModelTargetsTrained/Diffuse_KTM.png", getAssets()));
    }
    
    
    // Called when the activity will start interacting with the user.
    @Override
    protected void onResume()
    {
        Log.d(LOGTAG, "onResume");
        super.onResume();

        showProgressIndicator(true);
        
        vuforiaAppSession.onResume();
    }
    
    
    // Callback for configuration changes the activity handles itself
    @Override
    public void onConfigurationChanged(Configuration config)
    {
        Log.d(LOGTAG, "onConfigurationChanged");
        super.onConfigurationChanged(config);
        
        vuforiaAppSession.onConfigurationChanged();

        Handler handler = new Handler();
        handler.postDelayed(new Runnable()
        {
            @Override
            public void run()
            {
                mRenderer.updateRenderingPrimitives();
                showProgressIndicator(false);
            }
        }, 100);
    }
    
    
    // Called when the system is about to start resuming a previous activity.
    @Override
    protected void onPause()
    {
        Log.d(LOGTAG, "onPause");
        super.onPause();
        
        if (mGlView != null)
        {
            mGlView.setVisibility(View.INVISIBLE);
            mGlView.onPause();
        }

        vuforiaAppSession.onPause();
    }
    
    
    // The final call you receive before your activity is destroyed.
    @Override
    protected void onDestroy()
    {
        Log.d(LOGTAG, "onDestroy");
        super.onDestroy();
        
        try
        {
            vuforiaAppSession.stopAR();
        } catch (SampleApplicationException e)
        {
            Log.e(LOGTAG, e.getString());
        }
        
        // Unload texture:
        mTextures.clear();
        mTextures = null;
        
        System.gc();
    }
    
    
    // Initializes AR application components.
    private void initApplicationAR()
    {
        // Create OpenGL ES view:
        int depthSize = 16;
        int stencilSize = 0;
        boolean translucent = Vuforia.requiresAlpha();
        
        mGlView = new SampleApplicationGLView(this);
        mGlView.init(translucent, depthSize, stencilSize);

        mRenderer = new ModelTargetTrainedRenderer(this, vuforiaAppSession);
        mRenderer.setTextures(mTextures);
        mGlView.setRenderer(mRenderer);
        mGlView.setPreserveEGLContextOnPause(true);
    }
    
    
    private void startLoadingAnimation()
    {
        mUILayout = (RelativeLayout) View.inflate(this, R.layout.camera_overlay_model_targets_trained,
            null);
        
        mUILayout.setVisibility(View.VISIBLE);
        mUILayout.setBackgroundColor(Color.BLACK);

        RelativeLayout topbarLayout = mUILayout.findViewById(R.id.topbar_layout);
        topbarLayout.setVisibility(View.VISIBLE);

        TextView title = mUILayout.findViewById(R.id.topbar_title);
        title.setText(getText(R.string.feature_model_targets_trained));

        mSettingsAdditionalViews.add(topbarLayout);
        
        // Gets a reference to the loading dialog
        loadingDialogHandler.mLoadingDialogContainer = mUILayout
            .findViewById(R.id.loading_indicator);
        
        // Shows the loading indicator at start
        loadingDialogHandler
            .sendEmptyMessage(LoadingDialogHandler.SHOW_LOADING_DIALOG);
        
        // Adds the inflated layout to the view
        addContentView(mUILayout, new LayoutParams(LayoutParams.MATCH_PARENT,
            LayoutParams.MATCH_PARENT));
        
    }


    public void dismissInitialScreen()
    {
        showInitialScreen(false);
        showSearchReticle(true);
        showMainARView(true);
    }


    public void dismissDetectedAllTargetsView()
    {
        showAllModelsDetectedView(false);
        showMainARView(true);
    }


    public void showSearchReticle(final boolean show)
    {
        runOnUiThread(new Runnable()
        {
            @Override
            public void run()
            {
                mSearchingReticleView.setVisibility(show ? View.VISIBLE : View.INVISIBLE);
            }
        });

    }


    public void showInitialScreen(final boolean show)
    {
        runOnUiThread(new Runnable()
        {
            @Override
            public void run()
            {
                mInitialScreenView.setVisibility(show ? View.VISIBLE : View.INVISIBLE);
            }
        });
    }


    public void showMainARView(final boolean show)
    {
        runOnUiThread(new Runnable()
        {
            @Override
            public void run()
            {
                int mainARViewVisibility = mMainARView.getVisibility();
                int requestedNewVisibility = show ? View.VISIBLE : View.INVISIBLE;

                if (mainARViewVisibility != requestedNewVisibility)
                {
                    setStatusImage(GuideViewModels.LANDER, GuideViewStatus.PASSIVE);
                    setStatusImage(GuideViewModels.BIKE, GuideViewStatus.PASSIVE);
                    mMainARView.setVisibility(requestedNewVisibility);
                }
            }
        });
    }


    public void showAllModelsDetectedView(final boolean show)
    {
        runOnUiThread(new Runnable()
        {
            @Override
            public void run()
            {
                mAllModelsDetectedView.setVisibility(show ? View.VISIBLE : View.INVISIBLE);
            }
        });
    }


    // Methods to load and destroy tracking data.
    @Override
    public boolean doLoadTrackersData()
    {
        TrackerManager tManager = TrackerManager.getInstance();
        ObjectTracker objectTracker = (ObjectTracker) tManager
            .getTracker(ObjectTracker.getClassType());

        if (objectTracker == null)
        {
            return false;
        }

        mTargetFinder = objectTracker.getTargetFinder(ObjectTracker.TargetFinderType.MODEL_RECO);
        mTargetFinder.startInit("ModelTargetsTrained/Vuforia_Motorcycle_Marslander.xml",
                STORAGE_TYPE.STORAGE_APPRESOURCE);

        if (mTargetFinder == null)
        {
            return false;
        }

        mTargetFinder.waitUntilInitFinished();

        if (mTargetFinder.getInitState() != TargetFinder.INIT_SUCCESS)
        {
            Log.e(LOGTAG, "Not able to successfully initialize Target Finder");
            return false;
        }

        return true;
    }
    
    
    @Override
    public boolean doUnloadTrackersData()
    {
        // To return true if the trackers were unloaded correctly

        TrackerManager tManager = TrackerManager.getInstance();
        ObjectTracker objectTracker = (ObjectTracker) tManager
            .getTracker(ObjectTracker.getClassType());
        if (objectTracker == null)
            return false;
        
        if (mTargetFinder != null)
        {
            if (mTargetFinder.isRequesting())
            {
                mTargetFinder.stop();
            }

            mTargetFinder.deinit();
            mTargetFinder = null;
        }
        
        return true;
    }
    
    
    @Override
    public void onInitARDone(SampleApplicationException exception)
    {
        
        if (exception == null)
        {
            initApplicationAR();
            
            mRenderer.setActive(true);
            
            // Now add the GL surface view. It is important
            // that the OpenGL ES surface view gets added
            // BEFORE the camera is started and video
            // background is configured.
            addContentView(mGlView, new LayoutParams(LayoutParams.MATCH_PARENT,
                LayoutParams.MATCH_PARENT));
            
            // Sets the UILayout to be drawn in front of the camera
            mUILayout.bringToFront();

            // Sets the layout background to transparent
            mUILayout.setBackgroundColor(Color.TRANSPARENT);

            ImageButton resetBtn = mUILayout.findViewById(R.id.reset_btn);

            resetBtn.setOnClickListener(new View.OnClickListener() {
                @Override
                public void onClick(View view)
                {
                    resetTrackersAndUI();
                }
            });

            if (mRenderer.areModelsLoaded())
            {
                showProgressIndicator(false);
            }

            vuforiaAppSession.startAR(CameraDevice.CAMERA_DIRECTION.CAMERA_DIRECTION_DEFAULT);

            mSampleAppMenu = new SampleAppMenu(this, this, getString(R.string.feature_model_targets_trained),
                mGlView, mUILayout, mSettingsAdditionalViews);
            setSampleAppMenuSettings();

        } else
        {
            Log.e(LOGTAG, exception.getString());
            showInitializationErrorMessage(exception.getString());
        }
        
    }

    public void resetTrackersAndUI()
    {
        TrackerManager trackerManager = TrackerManager.getInstance();
        ObjectTracker objectTracker = (ObjectTracker) trackerManager.
                getTracker(ObjectTracker.getClassType());
        PositionalDeviceTracker deviceTracker = (PositionalDeviceTracker) trackerManager.
                getTracker(PositionalDeviceTracker.getClassType());

        if (objectTracker != null)
        {
            objectTracker.stop();

            if (mTargetFinder != null)
            {
                mTargetFinder.clearTrackables();
                mTargetFinder.stop();
                mTargetFinder.startRecognition();
            }

            objectTracker.start();
        }

        if (deviceTracker != null)
        {
            deviceTracker.reset();
        }

        // Set current model to null and delete guide view texture
        mRenderer.onNewModelTarget(null);

        showInitialUIState();
    }

    public void showInitialUIState()
    {
        mIsTutorialComplete = false;
        showSearchReticle(true);
        showMainARView(false);
        showInitialScreen(true);
        showAllModelsDetectedView(false);
    }

    public void showProgressIndicator(boolean show)
    {
        if (loadingDialogHandler != null)
        {
            if (show)
            {
                loadingDialogHandler
                        .sendEmptyMessage(LoadingDialogHandler.SHOW_LOADING_DIALOG);
            }
            else
            {
                loadingDialogHandler
                        .sendEmptyMessage(LoadingDialogHandler.HIDE_LOADING_DIALOG);
            }
        }
    }
    
    
    // Shows initialization error messages as System dialogs
    public void showInitializationErrorMessage(String message)
    {
        final String errorMessage = message;
        runOnUiThread(new Runnable()
        {
            public void run()
            {
                if (mErrorDialog != null)
                {
                    mErrorDialog.dismiss();
                }
                
                // Generates an Alert Dialog to show the error message
                AlertDialog.Builder builder = new AlertDialog.Builder(
                    ModelTargetsTrained.this);
                builder
                    .setMessage(errorMessage)
                    .setTitle(getString(R.string.INIT_ERROR))
                    .setCancelable(false)
                    .setIcon(0)
                    .setPositiveButton(getString(R.string.button_OK),
                        new DialogInterface.OnClickListener()
                        {
                            public void onClick(DialogInterface dialog, int id)
                            {
                                finish();
                            }
                        });
                
                mErrorDialog = builder.create();
                mErrorDialog.show();
            }
        });
    }
    
    
    @Override
    public void onVuforiaUpdate(State state)
    {
        if (mTargetFinder == null || mRenderer == null || !mRenderer.areModelsLoaded())
        {
            return;
        }

        // look for poses on the state
        boolean modelTargetResultFound = false;
        for (TrackableResult result : state.getTrackableResults())
        {
            if ((result instanceof ModelTargetResult) && (result.getStatus() == TrackableResult.STATUS.TRACKED))
            {
                modelTargetResultFound = true;
                break;
            }
        }

        // consume results even if we decide not to use them
        TargetFinderQueryResult queryResult = mTargetFinder.updateQueryResults();

        if (modelTargetResultFound)
        {
            return; // don't switch while tracking
        }

        // If we get a result from the Target Finder we will enable tracking for the first result,
        // then we show the guide view which corresponds to that result so the user can snap and track that target
        if (queryResult.getStatus() == TargetFinder.UPDATE_RESULTS_AVAILABLE && !queryResult.getResults().empty())
        {
            ObjectTarget target = mTargetFinder.enableTracking(queryResult.getResults().at(0));

            if ((target instanceof ModelTarget))
            {
                // Set the new model target to activate
                mRenderer.onNewModelTarget((ModelTarget) target);
                showInitialScreen(false);
                showSearchReticle(false);
                showMainARView(true);

                GuideViewModels guideViewUIToUpdate = GuideViewModels.BIKE;
                if (target.getName().contains("MarsLander"))
                {
                    guideViewUIToUpdate = GuideViewModels.LANDER;
                }

                // Set the UI to be highlighted for the currently detected model
                setStatusImage(guideViewUIToUpdate, GuideViewStatus.RECOGNIZED);
            }
        }
    }

    @Override
    public void onVuforiaResumed()
    {
        if (mGlView != null)
        {
            mGlView.setVisibility(View.VISIBLE);
            mGlView.onResume();
        }
    }

    @Override
    public void onVuforiaStarted()
    {
        mRenderer.updateRenderingPrimitives();

        // Set camera focus mode
        if(!CameraDevice.getInstance().setFocusMode(CameraDevice.FOCUS_MODE.FOCUS_MODE_CONTINUOUSAUTO))
        {
            // If continuous autofocus mode fails, attempt to set to a different mode
            if(!CameraDevice.getInstance().setFocusMode(CameraDevice.FOCUS_MODE.FOCUS_MODE_TRIGGERAUTO))
            {
                CameraDevice.getInstance().setFocusMode(CameraDevice.FOCUS_MODE.FOCUS_MODE_NORMAL);
            }
        }

        if (mRenderer.areModelsLoaded())
        {
            showProgressIndicator(false);
        }
    }


    @Override
    public boolean doInitTrackers()
    {
        // Indicate if the trackers were initialized correctly
        boolean result = true;

        TrackerManager trackerManager = TrackerManager.getInstance();

        // Initialize the object tracker:
        Tracker tracker = trackerManager.initTracker(ObjectTracker.getClassType());

        if (tracker == null)
        {
            Log.d(LOGTAG, "Failed to initialize ObjectTracker.");
            result = false;
        } else
        {
            Log.d(LOGTAG, "Successfully initialized ObjectTracker.");
        }

        // Initialize the Positional Device Tracker
        PositionalDeviceTracker deviceTracker = (PositionalDeviceTracker)
                trackerManager.initTracker(PositionalDeviceTracker.getClassType());

        if (deviceTracker != null)
        {
            Log.i(LOGTAG, "Successfully initialized Device Tracker");
        }
        else
        {
            Log.e(LOGTAG, "Failed to initialize Device Tracker");
        }
        return result;
    }
    
    
    @Override
    public boolean doStartTrackers()
    {
        // Indicate if the trackers were started correctly
        boolean result = true;
        
        Tracker objectTracker = TrackerManager.getInstance().getTracker(
            ObjectTracker.getClassType());
        if (objectTracker != null)
            objectTracker.start();

        if (!mTargetFinder.startRecognition())
        {
            result = false;
        }

        result = result && setDeviceTrackerEnabled(true);

        return result;
    }
    
    
    @Override
    public boolean doStopTrackers()
    {
        // Returns true if the trackers were stopped correctly
        Tracker objectTracker = TrackerManager.getInstance().getTracker(
            ObjectTracker.getClassType());
        if (objectTracker != null)
        {
            objectTracker.stop();
        }

        return setDeviceTrackerEnabled(false);
    }

    boolean setDeviceTrackerEnabled(boolean enabled)
    {
        boolean result = true;

        Tracker deviceTracker = TrackerManager.getInstance().getTracker(
                PositionalDeviceTracker.getClassType());

        if (deviceTracker == null)
        {
            Log.e(LOGTAG, "ERROR: Could not toggle device tracker state");
            return false;
        }

        if (enabled)
        {
            if (deviceTracker.start())
            {
                Log.d(LOGTAG, "Successfully started device tracker");
            }
            else
            {
                result = false;
                Log.e(LOGTAG, "Failed to start device tracker");
            }
        }
        else
        {
            deviceTracker.stop();
            Log.d(LOGTAG, "Successfully stopped device tracker");
        }

        return result;
    }
    
    
    @Override
    public boolean doDeinitTrackers()
    {
        // Returns true if the trackers were deinitialized correctly
        boolean result;

        TrackerManager tManager = TrackerManager.getInstance();
        result = tManager.deinitTracker(ObjectTracker.getClassType());
        result = result && tManager.deinitTracker(PositionalDeviceTracker.getClassType());
        
        return result;
    }
    
    
    @Override
    public boolean onTouchEvent(MotionEvent event)
    {
        // Process the Gestures
        return mSampleAppMenu != null && mSampleAppMenu.processEvent(event) ||
                mGestureDetector.onTouchEvent(event);

    }
    
    
    final public static int CMD_BACK = -1;
    final public static int CMD_AUTOFOCUS = 0;

    
    // This method sets the menu's settings
    private void setSampleAppMenuSettings()
    {
        SampleAppMenuGroup group;
        
        group = mSampleAppMenu.addGroup("", false);
        group.addTextItem(getString(R.string.menu_back), CMD_BACK);

        group = mSampleAppMenu.addGroup(getString(R.string.menu_camera), true);
        group.addSelectionItem(getString(R.string.menu_contAutofocus),
                CMD_AUTOFOCUS, mContAutofocus);

        mSampleAppMenu.attachMenu();
    }
    
    
    @Override
    public boolean menuProcess(int command)
    {
        boolean result = true;
        
        switch (command)
        {
            case CMD_BACK:
                finish();
                break;

            case CMD_AUTOFOCUS:

                if (mContAutofocus)
                {
                    result = CameraDevice.getInstance().setFocusMode(
                            CameraDevice.FOCUS_MODE.FOCUS_MODE_NORMAL);

                    if (result)
                    {
                        mContAutofocus = false;
                    } else
                    {
                        showToast(getString(R.string.menu_contAutofocus_error_off));
                        Log.e(LOGTAG,
                                getString(R.string.menu_contAutofocus_error_off));
                    }
                } else
                {
                    result = CameraDevice.getInstance().setFocusMode(
                            CameraDevice.FOCUS_MODE.FOCUS_MODE_CONTINUOUSAUTO);

                    if (result)
                    {
                        mContAutofocus = true;
                    } else
                    {
                        showToast(getString(R.string.menu_contAutofocus_error_on));
                        Log.e(LOGTAG,
                                getString(R.string.menu_contAutofocus_error_on));
                    }
                }

                break;

        }
        
        return result;
    }

    private void showToast(String text)
    {
        Toast.makeText(this, text, Toast.LENGTH_SHORT).show();
    }


    // Set the guide views status image if it has been recognized or snapped
    public void setStatusImage(GuideViewModels model, GuideViewStatus newStatus)
    {
        // We check if the status was already set
        if (model == GuideViewModels.LANDER && mLanderGuideViewStatus == newStatus)
        {
            return;
        }

        if (model == GuideViewModels.BIKE && mBikeGuideViewStatus == newStatus)
        {
            return;
        }

        final ImageView imageView;

        // If this is the first time both models have been 'snapped', show the 'detected all models' dialog
        if (model == GuideViewModels.LANDER)
        {
            if (newStatus == GuideViewStatus.SNAPPED && mBikeGuideViewStatus == newStatus
                    && !mIsTutorialComplete)
            {
                showAllModelsDetectedView(true);
                mIsTutorialComplete = true;
            }
            imageView = mLanderStatusView;
            mLanderGuideViewStatus = newStatus;
        }
        else
        {
            if (newStatus == GuideViewStatus.SNAPPED && mLanderGuideViewStatus == newStatus
                    && !mIsTutorialComplete)
            {
                showAllModelsDetectedView(true);
                mIsTutorialComplete = true;
            }
            imageView = mBikeStatusView;
            mBikeGuideViewStatus = newStatus;
        }

        final int guideViewImageDrawableId;
        if (model == GuideViewModels.LANDER)
        {
            switch (newStatus)
            {
                case PASSIVE:
                    guideViewImageDrawableId = R.drawable.lander_status_passive;
                    break;
                case RECOGNIZED:
                    guideViewImageDrawableId = R.drawable.lander_status_recognized;
                    break;
                case SNAPPED:
                    guideViewImageDrawableId = R.drawable.lander_status_snapped;
                    break;
                default:
                    Log.e(LOGTAG, "Should not reach this point");
                    guideViewImageDrawableId = R.drawable.lander_status_passive;
                    break;
            }
        }
        else
        {
            switch (newStatus)
            {
                case PASSIVE:
                    guideViewImageDrawableId = R.drawable.bike_status_passive;
                    break;
                case RECOGNIZED:
                    guideViewImageDrawableId = R.drawable.bike_status_recognized;
                    break;
                case SNAPPED:
                    guideViewImageDrawableId = R.drawable.bike_status_snapped;
                    break;
                default:
                    Log.e(LOGTAG, "Should not reach this point");
                    guideViewImageDrawableId = R.drawable.bike_status_passive;
                    break;
            }
        }

        runOnUiThread(new Runnable()
        {
            @Override
            public void run()
            {
                imageView.setImageResource(guideViewImageDrawableId);
            }
        });

    }


    public void checkForRelocalization(final int statusInfo)
    {
        if (mCurrentStatusInfo == statusInfo)
        {
            return;
        }

        mCurrentStatusInfo = statusInfo;

        if (mCurrentStatusInfo == TrackableResult.STATUS_INFO.RELOCALIZING)
        {
            // If the status is RELOCALIZING, start the timer
            if (!mStatusDelayTimer.isRunning())
            {
                mStatusDelayTimer.startTimer();
            }
        }
        else
        {
            // If the status is not RELOCALIZING, stop the timers and hide the message
            if (mStatusDelayTimer.isRunning())
            {
                mStatusDelayTimer.stopTimer();
            }

            if (mRelocalizationTimer.isRunning())
            {
                mRelocalizationTimer.stopTimer();
            }

            runOnUiThread(new Runnable()
            {
                @Override
                public void run()
                {
                    mSampleAppMessage.hide();
                }
            });
        }
    }
}

package com.spaamdemo;

/******Java Specific Libraries******/
import java.io.IOException;
import java.lang.reflect.Field;
import java.util.Set;
import java.util.UUID;

import jp.epson.moverio.bt200.DisplayControl;

import org.opencv.android.OpenCVLoader;

import android.app.Activity;
import android.app.ActivityManager;
import android.bluetooth.BluetoothAdapter;
import android.bluetooth.BluetoothDevice;
import android.bluetooth.BluetoothServerSocket;
import android.bluetooth.BluetoothSocket;
import android.content.Context;
import android.content.Intent;
import android.content.pm.ActivityInfo;
import android.content.pm.ConfigurationInfo;
import android.content.res.Configuration;
import android.opengl.GLSurfaceView;
import android.os.AsyncTask;
import android.os.Bundle;
import android.util.DisplayMetrics;
import android.util.Log;
import android.view.Menu;
import android.view.MenuItem;
import android.view.MotionEvent;
import android.view.View;
import android.view.ViewGroup;
import android.view.Window;
import android.view.WindowManager;
import android.view.View.OnTouchListener;
import android.widget.RelativeLayout;
import android.widget.TextView;
import android.widget.Toast;

import com.SharpViewDemo.R;
import com.qualcomm.QCAR.QCAR;
/******Qualcomm AR library for Vuforia Useage******/
/******Android specific Libraries******/

public class OGLActivity extends Activity {
	////////////////////////////////////////////////////////////////
	//////////////Epson Specific Members////////////////////////////
	private DisplayControl mDisplayControl = null;
	
	////////////////////////////////////////////////////////////////
	//////////////Members used in the Vuforia Functions/////////////
	//Simple Name to identify the class//
	private static final String TAG = "SPAAM Activity";
	
	// Focus mode constants:
	private static final int FOCUS_MODE_NORMAL = 0;
	private static final int FOCUS_MODE_CONTINUOUS_AUTO = 1;
	
	// Application status constants:
	private static final int APPSTATUS_UNINITED         = -1;
	private static final int APPSTATUS_INIT_APP         = 0;
	private static final int APPSTATUS_INIT_QCAR        = 1;
	private static final int APPSTATUS_INIT_TRACKER     = 2;
	private static final int APPSTATUS_INIT_APP_AR      = 3;
	private static final int APPSTATUS_LOAD_TRACKER     = 4;
	private static final int APPSTATUS_INITED           = 5;
	private static final int APPSTATUS_CAMERA_STOPPED   = 6;
	private static final int APPSTATUS_CAMERA_RUNNING   = 7;
	
	// Name of the native dynamic libraries to load:
	private static final String NATIVE_LIB_SAMPLE = "VuforiaNative";
	private static final String NATIVE_LIB_QCAR = "Vuforia";
	
	// Display size of the device:
	private int mScreenWidth = 0;
	private int mScreenHeight = 0;
	
	// Constant representing invalid screen orientation to trigger a query:
	private static final int INVALID_SCREEN_ROTATION = -1;
	
	// Last detected screen rotation:
	private int mLastScreenRotation = INVALID_SCREEN_ROTATION;
	
	// The current application status:
	private int mAppStatus = APPSTATUS_UNINITED;
	
	// The async tasks to initialize the QCAR SDK:
	private InitQCARTask mInitQCARTask;
	private LoadTrackerTask mLoadTrackerTask;
	
	// An object used for synchronizing QCAR initialization, dataset loading and
	// the Android onDestroy() life cycle event. If the application is destroyed
	// while a data set is still being loaded, then we wait for the loading
	// operation to finish before shutting down QCAR:
	private Object mShutdownLock = new Object();
	
	// QCAR initialization flags:
	private int mQCARFlags = 0;
	
	// Contextual Menu Options for Camera Flash - Autofocus
	private boolean mFlash = false;
	private boolean mContAutofocus = false;
	
	// The menu item for swapping data sets:
	MenuItem mDataSetMenuItem = null;
	boolean mIsStonesAndChipsDataSetActive  = false;
	
	private RelativeLayout mUILayout;
	
	////////////////////////////////////////////////////
	/** Native tracker initialization and deinitialization. */
	public native int initTracker();
	public native void deinitTracker();
	
	/** Native functions to load and destroy tracking data. */
	public native int loadTrackerData();
	public native void destroyTrackerData();
	
	/** Native sample initialization. */
	public native void onQCARInitializedNative();
	
	/** Native methods for starting and stopping the camera. */
	private native void startCamera();
	private native void stopCamera();
	
	/** Native method for setting / updating the projection matrix
	* for AR content rendering */
	private native void setProjectionMatrix();
	
	/** Native function to initialize the application. */
	private native void initApplicationNative(int width, int height);
	
	/** Native function to deinitialize the application.*/
	private native void deinitApplicationNative();    
	
	/** Tells native code whether we are in portait or landscape mode */
	private native void setActivityPortraitMode(boolean isPortrait);
	
	/** Tells native code to switch dataset as soon as possible*/
	private native void switchDatasetAsap();
	
	private native boolean autofocus();
	private native boolean setFocusMode(int mode);
	
	/** Activates the Flash */
	private native boolean activateFlash(boolean flash);
	///////////////////////////////////////////////////////////
	
	/** A helper for loading native libraries stored in "libs/armeabi*". */
	public static boolean loadLibrary(String nLibName)
	{
		try
		{
			System.loadLibrary(nLibName);
			Log.d(TAG, "Native library lib" + nLibName + ".so loaded");
			return true;
		}
		catch (UnsatisfiedLinkError ulee)
		{
			Log.d(TAG, "The library lib" + nLibName +
			".so could not be loaded");
		}
		catch (SecurityException se)
		{
			Log.d(TAG, "The library lib" + nLibName +
			".so was not allowed to be loaded");
		}
	
		return false;
	}
	
	/** Static initializer block to load native libraries on start-up. */
	static
	{
		loadLibrary(NATIVE_LIB_QCAR);
		loadLibrary(NATIVE_LIB_SAMPLE);
	}
	
	static {
	    if (!OpenCVLoader.initDebug()) {
	        // Handle initialization error
	    }
	}
	
	/** An async task to initialize QCAR asynchronously. */
	private class InitQCARTask extends AsyncTask<Void, Integer, Boolean>
	{
		// Initialize with invalid value:
		private int mProgressValue = -1;
		
		protected Boolean doInBackground(Void... params)
		{
			// Prevent the onDestroy() method to overlap with initialization:
			synchronized (mShutdownLock)
			{
				QCAR.setInitParameters(OGLActivity.this, mQCARFlags);
				
				do
				{
					// QCAR.init() blocks until an initialization step is
					// complete, then it proceeds to the next step and reports
					// progress in percents (0 ... 100%).
					// If QCAR.init() returns -1, it indicates an error.
					// Initialization is done when progress has reached 100%.
					mProgressValue = QCAR.init();
					
					// Publish the progress value:
					publishProgress(mProgressValue);
					
					// We check whether the task has been canceled in the
					// meantime (by calling AsyncTask.cancel(true)).
					// and bail out if it has, thus stopping this thread.
					// This is necessary as the AsyncTask will run to completion
					// regardless of the status of the component that
					// started is.
				} while (!isCancelled() && mProgressValue >= 0 && mProgressValue < 100);
				
				return (mProgressValue > 0);
			}
		}
	
		protected void onPostExecute(Boolean result)
		{
			// Done initializing QCAR, proceed to next application
			// initialization status:
			if (result)
			{
				Log.d(TAG,"InitQCARTask::onPostExecute: QCAR " +
				"initialization successful");
				
				updateApplicationStatus(APPSTATUS_INIT_TRACKER);
			}
			else
			{
				Log.d(TAG,"InitQCARTask::onPostExecute: QCAR " +
				"initialization failed");
			}
		}
	}
	
	/** An async task to load the tracker data asynchronously. */
	private class LoadTrackerTask extends AsyncTask<Void, Integer, Boolean>
	{
		protected Boolean doInBackground(Void... params)
		{
			// Prevent the onDestroy() method to overlap:
			synchronized (mShutdownLock)
			{
				// Load the tracker data set:
				return (loadTrackerData() > 0);
			}
		}
		
		protected void onPostExecute(Boolean result)
		{
			Log.d(TAG,"LoadTrackerTask::onPostExecute: execution " +
			(result ? "successful" : "failed"));
			
			if (result)
			{
				// The stones and chips data set is now active:
				mIsStonesAndChipsDataSetActive = true;
				
				// Done loading the tracker, update application status:
				updateApplicationStatus(APPSTATUS_INITED);
			}
			else
			{
			
			}
		}
	}
	
	/** Stores screen dimensions */
	private void storeScreenDimensions()
	{
		// Query display dimensions:
		DisplayMetrics metrics = new DisplayMetrics();
		getWindowManager().getDefaultDisplay().getMetrics(metrics);
		mScreenWidth = metrics.widthPixels;
		mScreenHeight = metrics.heightPixels;
	}
	
	private void updateActivityOrientation()
	{
		Configuration config = getResources().getConfiguration();
		
		boolean isPortrait = false;
		
		switch (config.orientation)
		{
			case Configuration.ORIENTATION_PORTRAIT:
				isPortrait = true;
				break;
			case Configuration.ORIENTATION_LANDSCAPE:
				isPortrait = false;
				break;
			case Configuration.ORIENTATION_UNDEFINED:
			default:
				break;
		}
		
		Log.d(TAG,"Activity is in "
		+ (isPortrait ? "PORTRAIT" : "LANDSCAPE"));
		setActivityPortraitMode(isPortrait);
	}
	
	/**
	* Updates projection matrix and viewport after a screen rotation
	* change was detected.
	*/
	public void updateRenderView()
	{
		int currentScreenRotation = getWindowManager().getDefaultDisplay().getRotation();
		if (currentScreenRotation != mLastScreenRotation)
		{
			// Set projection matrix if there is already a valid one:
			
			if ( (QCAR.isInitialized() && mAppStatus == APPSTATUS_CAMERA_RUNNING))
			{
				Log.d(TAG,"VuforiaJMEActivity::updateRenderView");
				
				// Query display dimensions:
				storeScreenDimensions();
				
				// Update viewport via renderer:
				//TODO SET Screen here
				// mRenderer.updateRendering(mScreenWidth, mScreenHeight);
				
				// Update projection matrix:
				setProjectionMatrix();
				
				// Cache last rotation used for setting projection matrix:
				mLastScreenRotation = currentScreenRotation;
			}
		}
	}
	
	/** NOTE: this method is synchronized because of a potential concurrent
	* access by VuforiaJMEActivity::onResume() and InitQCARTask::onPostExecute(). */
	private synchronized void updateApplicationStatus(int appStatus)
	{
		// Exit if there is no change in status:
		if (mAppStatus == appStatus)
			return;
		
		// Store new status value:
		mAppStatus = appStatus;
		
		// Execute application state-specific actions:
		switch (mAppStatus)
		{
			case APPSTATUS_INIT_APP:
				// Initialize application elements that do not rely on QCAR
				// initialization:
				initApplication();
				// Proceed to next application initialization status:
				updateApplicationStatus(APPSTATUS_INIT_QCAR);
				break;
			
			case APPSTATUS_INIT_QCAR:
				// Initialize QCAR SDK asynchronously to avoid blocking the
				// main (UI) thread.
				//
				// NOTE: This task instance must be created and invoked on the
				// UI thread and it can be executed only once!
				
				try
				{
					mInitQCARTask = new InitQCARTask();
					mInitQCARTask.execute();
				}
				catch (Exception e)
				{
					Log.d(TAG,"Initializing QCAR SDK failed");
					Toast.makeText(getBaseContext(), "AppStatusInitQcarFail", Toast.LENGTH_LONG).show();
					
				}
				break;
			
			case APPSTATUS_INIT_TRACKER:
				// Initialize the ImageTracker:
				
				if (initTracker() > 0)
				{
					// Proceed to next application initialization status:
					updateApplicationStatus(APPSTATUS_INIT_APP_AR);
				}
				break;
			
			case APPSTATUS_INIT_APP_AR:
				// Initialize Augmented Reality-specific application elements
				// that may rely on the fact that the QCAR SDK has been
				// already initialized:
				initApplicationAR();
				
				// Proceed to next application initialization status:
				updateApplicationStatus(APPSTATUS_LOAD_TRACKER);
				break;
			
			case APPSTATUS_LOAD_TRACKER:
				// Load the tracking data set:
				//
				// NOTE: This task instance must be created and invoked on the
				// UI thread and it can be executed only once!
				try
				{
					mLoadTrackerTask = new LoadTrackerTask();
					mLoadTrackerTask.execute();
				}
				catch (Exception e)
				{
					Log.d(TAG,"Loading tracking data set failed");
				}
				break;
			
			case APPSTATUS_INITED:
				// Hint to the virtual machine that it would be a good time to
				// run the garbage collector:
				//
				// NOTE: This is only a hint. There is no guarantee that the
				// garbage collector will actually be run.
				System.gc();
				
				// Native post initialization:
				onQCARInitializedNative();
				
				// Activate the renderer:
				// mRenderer.mIsActive = true;
				
				// Now add the GL surface view. It is important
				// that the OpenGL ES surface view gets added
				// BEFORE the camera is started and video
				// background is configured.
				
				//  addContentView(mGlView, new LayoutParams(LayoutParams.MATCH_PARENT,
				//          LayoutParams.MATCH_PARENT));
				
				// Sets the UILayout to be drawn in front of the camera
				//  mUILayout.bringToFront();
				
				// Start the camera:
				updateApplicationStatus(APPSTATUS_CAMERA_RUNNING);
				
				break;
			
			case APPSTATUS_CAMERA_STOPPED:
				// Call the native function to stop the camera:
				stopCamera();
				break;
			
			case APPSTATUS_CAMERA_RUNNING:
				// Call the native function to start the camera:
				startCamera();
				
				// Sets the layout background to transparent
				//   mUILayout.setBackgroundColor(Color.TRANSPARENT);
				
				
				// Set continuous auto-focus if supported by the device,
				// otherwise default back to regular auto-focus mode.
				// This will be activated by a tap to the screen in this
				// application.
				if (!setFocusMode(FOCUS_MODE_CONTINUOUS_AUTO))
				{
					mContAutofocus = false;
					setFocusMode(FOCUS_MODE_NORMAL);
				}
				else
				{
					mContAutofocus = true;
				}
				TextView editBox = new TextView(getBaseContext());
				editBox.setText("Loading Models & Textures....");
				editBox.setId(9807654);
				oglRenderer.mText = editBox;
				glSurfaceView.setRenderer(oglRenderer);
				renderSet = true;
				//function to prepare the correct calibration file for reading/writing//
				try {
					oglRenderer.SetupCalibrationFunc();
				} catch (IOException e) {
					// TODO Auto-generated catch block
					e.printStackTrace();
				}
				glSurfaceView.setOnTouchListener(new OnTouchListener(){
					@Override
					public boolean onTouch(View v, MotionEvent event){
						if (event != null){
							if ( event.getAction() == MotionEvent.ACTION_DOWN){
								glSurfaceView.queueEvent(new Runnable(){
									@Override
									public void run(){
										try {
											oglRenderer.handleTouchPress();
										} catch (IOException e) {
											// TODO Auto-generated catch block
											e.printStackTrace();
										}
									}
								});
							} else if (event.getAction() == MotionEvent.ACTION_MOVE){
								glSurfaceView.queueEvent(new Runnable(){
									@Override
									public void run(){
										oglRenderer.handleTouchDrag();
									}
								});
							}
							return true;
						} else {
							return false;
						}
					}
				});
				
				setContentView(glSurfaceView);
				addContentView(editBox, new ViewGroup.LayoutParams(ViewGroup.LayoutParams.WRAP_CONTENT, ViewGroup.LayoutParams.WRAP_CONTENT));
				
				break;
			
			default:
				throw new RuntimeException("Invalid application state");
		}
	}
	
	/** Initialize application GUI elements that are not related to AR. */
	private void initApplication()
	{
		// Set the screen orientation:
		// NOTE: Use SCREEN_ORIENTATION_LANDSCAPE or SCREEN_ORIENTATION_PORTRAIT
		//       to lock the screen orientation for this activity.
		int screenOrientation = ActivityInfo.SCREEN_ORIENTATION_LANDSCAPE;
		
		// This is necessary for enabling AutoRotation in the Augmented View
		if (screenOrientation == ActivityInfo.SCREEN_ORIENTATION_SENSOR)
		{
			// NOTE: We use reflection here to see if the current platform
			// supports the full sensor mode (available only on Gingerbread
			// and above.
			try
			{
				// SCREEN_ORIENTATION_FULL_SENSOR is required to allow all 
				// 4 screen rotations if API level >= 9:
				Field fullSensorField = ActivityInfo.class
				.getField("SCREEN_ORIENTATION_FULL_SENSOR");
				screenOrientation = fullSensorField.getInt(null);
			}
			catch (NoSuchFieldException e)
			{
				// App is running on API level < 9, do nothing.
			}
			catch (Exception e)
			{
				e.printStackTrace();
			}
		}
	
		// Apply screen orientation
		setRequestedOrientation(screenOrientation);
		
		updateActivityOrientation();
		
		// Query display dimensions:
		storeScreenDimensions();
		
		// As long as this window is visible to the user, keep the device's
		// screen turned on and bright:
		getWindow().setFlags(
		WindowManager.LayoutParams.FLAG_KEEP_SCREEN_ON,
		WindowManager.LayoutParams.FLAG_KEEP_SCREEN_ON);
	}
	
	/** Initializes AR application components. */
	private void initApplicationAR()
	{
		// Do application initialization in native code (e.g. registering
		// callbacks, etc.):
		initApplicationNative(mScreenWidth, mScreenHeight);
	}
	
	private boolean mCreatedBefore = false;
	
	////////////////////////////////////////////////////////////////
	////////////////////////////////////////////////////////////////
	////////////////////////////////////////////////////////////////
	/////////////////Non Vuforia Related Members//////////////////////////////
	
	///////////////////////////////////////////////////////////////
	///////////////////////DATA MEMBERS////////////////////////////
	private GLSurfaceView glSurfaceView = null;
	private boolean renderSet = false;
	
	OGLRenderer oglRenderer = null;
	
	///////////////////////DATA MEMBERS////////////////////////////
	BluetoothAdapter mBluetoothAdapter;	
	Set<BluetoothDevice> pairedDevices;
	AcceptThread serverThread = null;
	boolean connected;
	
	///////////////////////////////////////////////////////////////
	
	///////////////////////////////////////////////////////////////
	//////////////////////////Classes//////////////////////////////
	
	//////////////////////////////////////////////////////////////
	///////////////////////Other Methods/////////////////////////
	
	///////////////////////////////////////////////////////////////
	//////////////////////////Classes//////////////////////////////
	private class AcceptThread extends Thread {
		//////////////////////////////////////////////////////////////
		////////////////////Data Members//////////////////////////////
		private final BluetoothServerSocket mmServerSocket;
		String NAME;
		UUID MY_UUID;
		Context context;
		//////////////////////////////////////////////////////////////

		public AcceptThread( String name, UUID uuid, Context cc ) {
			NAME = name;
			MY_UUID = uuid;
			context = cc;
			// Use a temporary object that is later assigned to mmServerSocket,
			// because mmServerSocket is final
			BluetoothServerSocket tmp = null;
			try {
				// MY_UUID is the app's UUID string, also used by the client code
				tmp = mBluetoothAdapter.listenUsingRfcommWithServiceRecord(NAME, MY_UUID);
			} catch (IOException e) { }
			mmServerSocket = tmp;
		}

		public void run() {
			BluetoothSocket socket = null;
			// Keep listening until exception occurs or a socket is returned
			while (true) {
				try {
					//Toast.makeText(context, "Attempting to Connect", Toast.LENGTH_LONG).show();
					socket = mmServerSocket.accept();

				} catch (IOException e) {
					break;
				}
				// If a connection was accepted
				if (socket != null) {
					oglRenderer.socket = socket;
					oglRenderer.ConnectFunc();
					connected = true;

					try {
						mmServerSocket.close();
					} catch (IOException e) {
						// TODO Auto-generated catch block
						e.printStackTrace();
					}
					break;
				}
			}
		}

		/** Will cancel the listening socket, and cause the thread to finish */
		public void cancel() {
			try {
				mmServerSocket.close();
			} catch (IOException e) { }
		}
	}

	/////////////////////////////////////////////////////////////////
	
	static boolean firstTimeGetImage=true;
	
	@Override
	protected void onCreate(Bundle savedInstanceState) {
		super.onCreate(savedInstanceState);

		//////////////////////////////////////////////
		/////////////Get Moverio Display Controller///
		mDisplayControl = new DisplayControl(this);
		
		/////////////////////////////////
		///////App Setup///////////////
		///////////////////////////////////
		/////////////////////////////////
		///////App Setup///////////////
		///////////////////////////////////
		requestWindowFeature(Window.FEATURE_NO_TITLE);
		
		Window win = getWindow();
		WindowManager.LayoutParams winParams = win.getAttributes();
		winParams.flags |= 0x80000000;
		win.setAttributes(winParams); 
		setContentView(R.layout.activity_ogl);
		/////////////////////////////////
		///////////////OGL///////////////
		/////////////////////////////////
		final ActivityManager activityManager = (ActivityManager) getSystemService(Context.ACTIVITY_SERVICE);
		final ConfigurationInfo configurationInfo = activityManager.getDeviceConfigurationInfo();
		final boolean supportsEs2 = configurationInfo.reqGlEsVersion >= 0x20000;
		///////////////
		glSurfaceView = new GLSurfaceView(this);
		//////////////////////////
		if ( supportsEs2 ){
			oglRenderer = new OGLRenderer(this);
			glSurfaceView.setEGLContextClientVersion(2);
			
		} else {
		
		}

		
		//////////////////////////////////
		///////////////Vuforia////////////
		//////////////////////////////////
		updateApplicationStatus(APPSTATUS_INIT_APP);
		//////////////////////////////////
	}
	
	@Override
	public boolean onCreateOptionsMenu(Menu menu) {
		return true;
	}
	
	@Override
	public boolean onOptionsItemSelected(MenuItem item) {
		// Handle action bar item clicks here. The action bar will
		// automatically handle clicks on the Home/Up button, so long
		// as you specify a parent activity in AndroidManifest.xml.
		//int id = item.getItemId();
		//if (id == R.id.action_settings) {
		//    return true;
		//}
		return super.onOptionsItemSelected(item);
	}
	
	public void onResume(){
		super.onResume();
		
		///Set Moverio Display Mode to be 3D///
		mDisplayControl.setMode(DisplayControl.DISPLAY_MODE_3D, false);

		///////////////////////////////////
		////////////Vuforia///////////////
		// QCAR-specific resume operation
		QCAR.onResume();
		
		// We may start the camera only if the QCAR SDK has already been
		// initialized
		if (mAppStatus == APPSTATUS_CAMERA_STOPPED)
		{
			updateApplicationStatus(APPSTATUS_CAMERA_RUNNING);
		}
		
		firstTimeGetImage=true;
		/////////////BT//////////////////////
		/////////////////////////////////////
	    //////////////Make Sure Bluetooth is Active//////////////////////
		mBluetoothAdapter= BluetoothAdapter.getDefaultAdapter();
		if (mBluetoothAdapter == null) {
			Toast.makeText(getBaseContext(), "Bluetooth Adapter Not Found", Toast.LENGTH_SHORT).show();
		}
		else{
			if (!mBluetoothAdapter.isEnabled()) {
				int REQUEST_ENABLE_BT = 1;
			    Intent enableBtIntent = new Intent(BluetoothAdapter.ACTION_REQUEST_ENABLE);
			    startActivityForResult(enableBtIntent, REQUEST_ENABLE_BT);
			}
		}
		
		pairedDevices = mBluetoothAdapter.getBondedDevices();
		// If there are paired devices
		if (pairedDevices.size() > 0) {
		    // Loop through paired devices
		    for (BluetoothDevice device : pairedDevices) {
		        // Add the name and address to an array adapter to show in a ListView
		    	Toast.makeText(getBaseContext(), device.getName() + device.getAddress(), Toast.LENGTH_SHORT).show();
		    }
		    
		    serverThread = new AcceptThread("SPAAM", UUID.fromString("F8609B20-E179-11E3-8B68-0800200C9A66"), getBaseContext());
		    serverThread.start();
		}
	
		/////////////////////////////////////
		///////////////OGL//////////////////
		///////////////////////////////////
		if (renderSet){
			glSurfaceView.onResume();
		}
		////////////////////////////////////
	}
	
	public void onPause(){
		super.onPause();
		
		/////////////////////////////////
		/////////////Vuforia/////////////
		/////////////////////////////////
		if (mAppStatus == APPSTATUS_CAMERA_RUNNING)
		{
			updateApplicationStatus(APPSTATUS_CAMERA_STOPPED);
		}
		// Disable flash when paused
		if (mFlash)
		{
			mFlash = false;
			activateFlash(mFlash);
		}
		
		// QCAR-specific pause operation
		QCAR.onPause();
		
		firstTimeGetImage=true;
		///////////////////////////////
		
		/////////////////////////////////
		////////////OGL//////////////////
		if ( renderSet){
			glSurfaceView.onPause();
		}
		/////////////////////////////////
	}
	
	@Override
	protected void onDestroy()
	{
		super.onDestroy();
		
		// Cancel potentially running tasks
		if (mInitQCARTask != null && mInitQCARTask.getStatus() != InitQCARTask.Status.FINISHED)
		{
			mInitQCARTask.cancel(true);
			mInitQCARTask = null;
		}
		
		if (mLoadTrackerTask != null &&	mLoadTrackerTask.getStatus() != LoadTrackerTask.Status.FINISHED)
		{
			mLoadTrackerTask.cancel(true);
			mLoadTrackerTask = null;
		}
		
		// Ensure that all asynchronous operations to initialize QCAR
		// and loading the tracker datasets do not overlap:
		synchronized (mShutdownLock) {	
			// Do application deinitialization in native code:
			deinitApplicationNative();
			
			// Destroy the tracking data set:
			destroyTrackerData();
			
			// Deinit the tracker:
			deinitTracker();
			
			// Deinitialize QCAR SDK:
			QCAR.deinit();
		}
		
		System.gc();
	}
	
	@Override
	public void onConfigurationChanged(Configuration config)
	{
		// DebugLog.LOGD("VuforiaJMEActivity::onConfigurationChanged");
		super.onConfigurationChanged(config);
		
		updateActivityOrientation();
		
		storeScreenDimensions();
		
		// Invalidate screen rotation to trigger query upon next render call:
		mLastScreenRotation = INVALID_SCREEN_ROTATION;
	}
}

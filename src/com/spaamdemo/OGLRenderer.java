package com.spaamdemo;

import static android.opengl.GLES20.GL_COLOR_BUFFER_BIT;
import static android.opengl.GLES20.GL_DEPTH_BUFFER_BIT;
import static android.opengl.GLES20.GL_DEPTH_TEST;
import static android.opengl.GLES20.GL_FLOAT;
import static android.opengl.GLES20.GL_LEQUAL;
import static android.opengl.GLES20.glClear;
import static android.opengl.GLES20.glClearColor;
import static android.opengl.GLES20.glDepthFunc;
import static android.opengl.GLES20.glEnable;
import static android.opengl.GLES20.glGetAttribLocation;
import static android.opengl.GLES20.glGetUniformLocation;
import static android.opengl.GLES20.glUseProgram;
import static android.opengl.GLES20.glVertexAttribPointer;
import static android.opengl.GLES20.glViewport;
import static android.opengl.Matrix.multiplyMM;

import java.io.File;
import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;
import java.io.RandomAccessFile;
import java.nio.ByteBuffer;
import java.nio.ByteOrder;
import java.nio.FloatBuffer;
import java.util.ArrayList;
import java.util.Timer;
import java.util.TimerTask;

import javax.microedition.khronos.egl.EGLConfig;
import javax.microedition.khronos.opengles.GL10;

import org.opencv.android.Utils;
import org.opencv.core.Core;
import org.opencv.core.CvType;
import org.opencv.core.Mat;
import org.opencv.core.Scalar;
import org.opencv.imgproc.Imgproc;

import android.app.Activity;
import android.bluetooth.BluetoothSocket;
import android.content.Context;
import android.opengl.GLSurfaceView.Renderer;
import android.os.AsyncTask;
import android.os.Environment;
import android.util.Log;
import android.view.View;
import android.widget.TextView;

import com.SharpViewDemo.R;
import com.spaamdemo.objects.GenericObject;
import com.spaamdemo.objects.MoverioObject;
import com.spaamdemo.programs.BlendTextureShaderProgram;
import com.spaamdemo.programs.ModelShaderProgram;
import com.spaamdemo.programs.TextureShaderProgram;
import com.spaamdemo.util.ShaderHelper;
import com.spaamdemo.util.TextResourceReader;
import com.spaamdemo.util.TextureHelper;
/******Java specific Libraries******/
/******Android Specific Libraries******/
/******Qualcomm Specific Libraries required by Vuforia******/
/******Java specific Libraries******/
/******Android Specific Libraries******/
/******Qualcomm Specific Libraries required by Vuforia******/

public class OGLRenderer extends Activity implements Renderer{

	/******Members for openGL ES rendering******/
	//Members Specific to the Verification Square//
	private static final int SQUARE_POSITION_COMPONENT_COUNT = 3;
	private static final int BYTES_PER_FLOAT = 4;
	private static final String U_COLOR = "u_Color";
	private static final String A_POSITION = "a_Position";
	private static final String U_PROJECTION = "u_Projection";
	private static final String U_TRANSFORM = "u_Transform";
	float [] squareVertices = {
			//Bottom Rectangle
			-.1f, .05f, 0.0f, .1f, .05f, 0.0f,
			-.1f, .05f, 0.0f, -.1f, -.05f, 0.0f,
			-.1f, -0.05f, 0.0f, .1f, -.05f, 0.0f,
			.1f, -0.05f, 0.0f, .1f, .05f, 0.0f,
			//Top Rectangle//
			-.1f, .05f, 0.10f, .1f, .05f, 0.10f,
			-.1f, .05f, 0.10f, -.1f, -.05f, 0.10f,
			-.1f, -0.05f, 0.10f, .1f, -.05f, 0.10f,
			.1f, -0.05f, 0.10f, .1f, .05f, 0.10f,
			//Left Rectangle//
			-.1f, .05f, 0.0f, -.1f, .05f, .1f,
			-.1f, -.05f, 0.0f, -.1f, -.05f, .1f,
			-.1f, .05f, 0.0f, -.1f, -.05f, .1f,
			-.1f, -.05f, 0.0f, -.1f, .05f, .1f,
			//Right Rectangle//
			.1f, .05f, 0.0f, .1f, .05f, .1f,
			.1f, -.05f, 0.0f, .1f, -.05f, .1f,
			.1f, .05f, 0.0f, .1f, -.05f, .1f,
			.1f, -.05f, 0.0f, .1f, .05f, .1f,
			
			};
	
	float[] u_ProjectionLeft = {4.517603766f, -0.187385877f, 0.049614169f, 0.04951504f,
							0.03085878f, 8.304909585f, 0.132992709f, 0.132726989f,
							-0.459890387f, 1.581686211f, -0.991896862f, -0.98991505f,
							0.503220564f, -0.081912651f, -0.071915019f, 0.128028867f};
	float[] u_ProjectionRight = {4.346900158f, -0.099565239f, -0.110730603f, -0.110509363f,
							0.114737754f, 8.085934414f, 0.034289488f, 0.034220978f,
							-0.426256822f, 1.536851238f, -0.995274322f, -0.993285762f,
							0.246058517f, -0.046813489f, -0.171563852f, 0.028579133f};
	float[] u_Transform = {1.0f, 0.0f, 0.0f, 0.0f,
							0.0f, 1.0f, 0.0f, 0.0f,
							0.0f, 0.0f, 1.0f, 0.0f,
							0.0f, 0.0f, 0.0f, 1.0f};
	float[] u_Transform_N = {1.0f, 0.0f, 0.0f, 0.0f,
			0.0f, 1.0f, 0.0f, 0.0f,
			0.0f, 0.0f, 1.0f, 0.0f,
			0.0f, 0.0f, 0.0f, 1.0f};

	private final FloatBuffer squareVertexData;
	private int squareProgram;
	private int uSquareColorLocation;
	private int aSquarePositionLocation;
	private int uOrthoLocation;
	private int uProjectionLocation;
	private int uTransformLocation;
	
	//Members Specific to the Rendering Window//
	int WIDTH = 0;
	int HEIGHT = 0;
	boolean tracking = false;
	Context context;
	public TextView mText = null;
	
	//////////////////////////////////////////////
	private TextureShaderProgram textureProgram;
	private BlendTextureShaderProgram blendTextureProgram;
	private ModelShaderProgram modelProgram;
	
	//DepthSlicingRectangle//
	private GenericObject DepthRectangle;
	private int DepthTexture;
	//Eye Image Objects//
	private GenericObject EyeImage1_model;
	private int EyeImage1_texture;
	private float EyeImage1_rotation;
	private GenericObject EyeImage2_model;
	private int EyeImage2_texture;
	private GenericObject EyeImage3_model;
	private int EyeImage3_texture;
	private GenericObject EyeImage4_model;
	private int EyeImage4_texture;
	private GenericObject EyeImage5_model;
	private int EyeImage5_texture;
	//Evaluation Board//
	private GenericObject EvaluationBoardV_model;
	private GenericObject EvaluationBoardH_model;
	private int EvaluationBoard_texture;
	private int NormalView_texture;
	private int SharpView_texture;
	private int NormalMode_texture;
	
	//Screen Calibration//
	private GenericObject ScreenCalibration_model;
	private int ScreenCalibration_texture1;
	private int ScreenCalibration_texture2;
	private int ScreenCalibration_texture3;
	private int ScreenCalibration_texture4;
	private int ScreenCalibration_texture5;
	private int ScreenCalibration_texture6;
	private float ScreenCalibration_blend;
	private int ScreenCalibration_blend_timer;
	//HMD Models//
	private MoverioObject Moverio_model;
	private MoverioObject Lumus_model;
	
	///////////////////////////////////////////////////////////////////
	//Object used to transmit the GPS data to the Google Glass//
	
	ConnectedThread mThread = null;
	BluetoothSocket socket = null;
	boolean btconnected = false;
	///////////////////////////////////////////////////////////////////
	
	///////////////////////////////////////////////////////////////////
	////////Reading in Calibration Related Items///////////////////////
	File LeftCalibFile = null;
	File RightCalibFile = null;
	boolean sharpImage = true;
	float pupil_diam = 5.0f;
	final float SCREEN_DISTANCE = 7.0f;
	float last_marker_distance = .5f;
	
	Mat img_orig = null;
	Mat img_sharp = null;
	boolean dirty_flag = false;
	////////////////////////////////////////////////////////////////////
	//////////////Timer based Sharpening
	Timer timer = new Timer();
	class UpdateSharpTask extends TimerTask {
		
	   public void run() {
	       //update the sharpened image;
		   new AsyncTask<Void, Void, String>()  {
		        @Override
		        protected String doInBackground(Void... params) {
		        	img_sharp = SharpenImage();
		        	return null;
		        }
		        @Override
		        protected void onPostExecute(String msg) {
		        	dirty_flag = true;
		        }
		    }.execute(null, null, null);
	   }
	}
	final int interval = 1;
	TimerTask updatesharp = new UpdateSharpTask();	
	
	/////////////////////////////////////////////////////////////////////
	
	////////Functions for Handling File Access///////
	//// Checks if external storage is available for read and write ////
	public boolean isExternalStorageWritable() {
	    String state = Environment.getExternalStorageState();
	    if (Environment.MEDIA_MOUNTED.equals(state)) {
	        return true;
	    }
	    return false;
	}

	//// Checks if external storage is available to at least read ////
	public boolean isExternalStorageReadable() {
	    String state = Environment.getExternalStorageState();
	    if (Environment.MEDIA_MOUNTED.equals(state) ||
	        Environment.MEDIA_MOUNTED_READ_ONLY.equals(state)) {
	        return true;
	    }
	    return false;
	}
	
	//// Function for Accessing a Public Directory File ////
	public File getAlbumStorageDir(String albumName) {
	    // Get the directory for the user's public pictures directory. 
	    File file = new File(Environment.getExternalStoragePublicDirectory(
	            Environment.DIRECTORY_DOWNLOADS), albumName);
	    if (!file.mkdirs()) {
	        Log.e("SPAAM ACTIVITY", "Directory not created");
	    }
	    return file;
	}	
	
	/****************************************************************
     * @param eye - the eye chosen for calibration
     * @throws IOException - exception thrown when file cannot be accessed
     * 
     * This function checks if the calibration file for the selected
     * eye is already created and can be read from. If it is not
     * already created, then the file is created.
     ***************************************************************/
	public void SetupCalibrationFunc( ) throws IOException
	{
		//Is the storage of the device readable//
		if (isExternalStorageReadable() && isExternalStorageWritable()) {
			File storageDirectory = getAlbumStorageDir("SPAAM_Calib");
			if ( storageDirectory != null ){
				if ( storageDirectory.listFiles() != null )
				{
					LeftCalibFile = null;
					RightCalibFile = null;
				
					//Check if the file for the chosen eye exists. If the file does not exist, create it//
					//Right eye//
					//Check if file exists//
					for ( int i = 0; i < storageDirectory.listFiles().length; i++ ){
						if ( storageDirectory.listFiles()[i].getName() == "Right.calib" ){
							RightCalibFile = storageDirectory.listFiles()[i];
							break;
						}
					}
					//file does not exist//
					if ( RightCalibFile != null )
					{
						//Attempt to read from the file. If the file is empty (just created, nothing is read)//
						RandomAccessFile rac_file = new RandomAccessFile(RightCalibFile.getAbsolutePath(), "r");
						if ( rac_file.length() >= 16*8 )
						{
							//Store the calibration result into the correct projection for the selected eye//
							for ( int i = 0; i < 16; i++ )
							{
				                u_ProjectionRight[i] = (float)rac_file.readDouble();
							}
						}
						rac_file.close();
					}
					
					//Left eye//
					//Check if file exists//
					for ( int i = 0; i < storageDirectory.listFiles().length; i++ ){
						if ( storageDirectory.listFiles()[i].getName() == "Left.calib" ){
							LeftCalibFile = storageDirectory.listFiles()[i];
							break;
						}
					}
					//file does not exist//
					if ( LeftCalibFile != null )
					{
						//Attempt to read from the file. If the file is empty (just created, nothing is read)//
						RandomAccessFile rac_file = new RandomAccessFile(LeftCalibFile.getAbsolutePath(), "r");
						if ( rac_file.length() >= 16*8 )
						{
							//Store the calibration result into the correct projection for the selected eye//
							for ( int i = 0; i < 16; i++ )
							{
				                u_ProjectionLeft[i] = (float)rac_file.readDouble();
							}
						}
						rac_file.close();
					}
				}
			}
		}//File Storage could not be accessed
		else{
			;
		}
	}

	/////////////////////////////////////////////////////////////////////////////
	
	////////////////////////////////////////////////////////////////////////////
	/** Native function for initializing the renderer. */
    public native void initTracking(int width, int height);

    /** Native function to update the renderer. */
    public native void updateTracking();
    ////////////////////////////////////////////////////////////////////////////
	
    private class ConnectedThread extends Thread {
    	private final BluetoothSocket mmSocket;
    	private final InputStream mmInStream;
    	private final OutputStream mmOutStream;

    	public ConnectedThread(BluetoothSocket socket) {
    		mmSocket = socket;
    		InputStream tmpIn = null;
    		OutputStream tmpOut = null;

    		// Get the input and output streams, using temp objects because
    		// member streams are final
    		try {
    			tmpIn = socket.getInputStream();
    			tmpOut = socket.getOutputStream();
    		} catch (IOException e) { }

    		mmInStream = tmpIn;
    		mmOutStream = tmpOut;
    	}

    	public void run() {

    		/////////////////////
    		byte[] buffer = new byte[1*32];  // buffer store for the stream
    		int bytes; // bytes returned from read()

    		// Keep listening to the InputStream until an exception occurs
    		while (true) {
    			try {
    				// Read from the InputStream
    				ByteBuffer recvbb = ByteBuffer.allocate(32);

    				bytes = mmInStream.read(buffer);

    				recvbb.order(ByteOrder.LITTLE_ENDIAN);
    				recvbb.put(buffer);
    				recvbb.rewind();
    				// Send the obtained bytes to the UI activity
    				//mHandler.obtainMessage(MESSAGE_READ, bytes, -1, buffer)
    				//       .sendToTarget();
    				pupil_diam = (float)recvbb.getFloat();
    				//this.runOnUiThread(ToastFunc);
    			} catch (IOException e) {
    				break;
    			}
    		}
    	}

    	/* Call this from the main activity to send data to the remote device */
    	public void write(byte[] bytes) {
    		try {
    			mmOutStream.write(bytes);
    		} catch (IOException e) { }
    	}

    	/* Call this from the main activity to shutdown the connection */
    	public void cancel() {
    		try {
    			mmSocket.close();
    		} catch (IOException e) { }
    	}
    }

    public void ConnectFunc()
    {
    	// Do work to manage the connection (in a separate thread)
    	if ( !btconnected && socket != null )
    	{
    		mThread = new ConnectedThread(socket);
    		mThread.start();
    		btconnected = true;
    	}
    }
    /////////////////////////////////////////////////////////////////////////////
    ////////////////Sharpening Filter Functions//////////////////////////////////
    //Replacement 4 quadrant
	Mat ShiftDFT(Mat src_arr, int IMAGE_WIDTH, int IMAGE_HEIGHT)
	{
		int cx = IMAGE_WIDTH/2, cy = IMAGE_HEIGHT/2;
		int i, j;
		Mat dst_arr = Mat.zeros(IMAGE_HEIGHT, IMAGE_WIDTH, CvType.CV_32F);//img_orig.type());//CvType.CV_32F);

		for( j=0; j < cy; j++ ){
			for( i=0; i < cx; i++ ){
				dst_arr.put(j, i, src_arr.get(j + cy, i + cx));	
				dst_arr.put(j + cy, i, src_arr.get(j, i + cx));
				dst_arr.put(j, i + cx, src_arr.get(j + cy, i));
				dst_arr.put(j + cy, i + cx, src_arr.get(j, i));
			}
		}

		return dst_arr;
	}

	//Merging for Fourier Transform
	Mat Merge(Mat src_arr, Mat src_arr2)
	{
		ArrayList<Mat> mv = new ArrayList<Mat>();
		mv.add(src_arr);
		mv.add(src_arr2);

		Mat complex = new Mat();
		Core.merge(mv, complex);

		return complex;
	}

	//Splitting for Fourier Transform
	Mat Split(Mat src_arr, int flag)
	{
		ArrayList<Mat> complex = new ArrayList<Mat>();
		Core.split(src_arr, complex);

		if(flag == 0){

			return complex.get(0);

		}else{

			return complex.get(1);
		}
	}

    Mat Gaussian_picture(float SIGMA, int IMAGE_WIDTH, int IMAGE_HEIGHT)
    {	
    	int cx = IMAGE_WIDTH/2, cy = IMAGE_HEIGHT/2;
		int i, j;
		double SIGMA_variable = 1 / (2*SIGMA*SIGMA);
		double distance;

		Mat dst_arr = Mat.zeros(IMAGE_HEIGHT, IMAGE_WIDTH, CvType.CV_32F);//img_orig.type());//, CV_64F);

		for(j = 0; j < IMAGE_HEIGHT; j++){
			for(i = 0; i < IMAGE_WIDTH; i++){
				distance = (cx - i) * (cx - i) + (cy - j) * (cy - j);
				float dstv = (float) ((SIGMA_variable / Math.PI) * Math.exp(-distance * SIGMA_variable));
				double[] dstva = {dstv};//, dstv, dstv, 0.0f}; 
				dst_arr.put(j, i, dstva);		
			}
		}

		return dst_arr;
    }
    
    //Wiener Filter
	Mat Wiener_Filter(Mat src_arr, Mat src_arr2, double SN_inverse, int IMAGE_WIDTH, int IMAGE_HEIGHT)
	{
		Mat defocusing_dft = new Mat(); Mat gaussian_dft = new Mat();
		Mat mix_real = new Mat(); Mat mix_imaginary = new Mat();
		Mat mix_real2 = new Mat(); Mat mix_imaginary2 = new Mat();

		//Initialize
		Mat defocusing_imaginary = Mat.zeros(IMAGE_HEIGHT, IMAGE_WIDTH, CvType.CV_32F);

		//Generating 2-channel array for the real and imaginary parts
		Mat defocusing_complex = Merge(src_arr, defocusing_imaginary);
		Mat gaussian_complex = Merge(src_arr2, defocusing_imaginary);


		//Fourier Transform
		Core.dft(defocusing_complex, defocusing_dft);
		Core.dft(gaussian_complex, gaussian_dft);

		
		//Dividing the Fourier transform to the real and imaginary parts
		Mat defocusimg_dft_real = Split(defocusing_dft, 0);
		Mat defocusimg_dft_imaginary = Split(defocusing_dft, 1);

		Mat gaussian_dft_real = Split(gaussian_dft, 0);
		Mat gaussian_dft_imaginary = Split(gaussian_dft, 1);


		//Gaussian power
		double[] t = {0.050}; 
		Scalar sn = new Scalar(t);
		Mat gaussian_spectrum = new Mat(); Core.add( gaussian_dft_real.mul(gaussian_dft_real), gaussian_dft_imaginary.mul(gaussian_dft_imaginary), gaussian_spectrum);
		Mat gaussian_sqrt = new Mat(); Core.add( gaussian_spectrum, sn, gaussian_sqrt);

		//Wiener filter
		Core.add( defocusimg_dft_real.mul(gaussian_dft_real), defocusimg_dft_imaginary.mul(gaussian_dft_imaginary), mix_real2);
		Core.subtract( defocusimg_dft_imaginary.mul(gaussian_dft_real), defocusimg_dft_real.mul(gaussian_dft_imaginary), mix_imaginary2);

		Core.divide( mix_real2, gaussian_sqrt, mix_real);
		Core.divide(mix_imaginary2, gaussian_sqrt, mix_imaginary);	

		//Merging of real and imaginary
		Mat Gaussian_deconvolution = Merge(mix_real, mix_imaginary);

		//Inverse Fourier Transform
		Mat defocusimg_idft = new Mat();
		Core.idft(Gaussian_deconvolution, defocusimg_idft, Core.DFT_SCALE, Gaussian_deconvolution.rows());

		//Picking up the real part
		Mat idft_result = Split(defocusimg_idft, 0);

		//Replacement 4 quadrant
		Mat idft_result_shift = ShiftDFT(idft_result, IMAGE_WIDTH, IMAGE_HEIGHT);

		return idft_result_shift;
	}
    
    Mat SharpenImage( )
    {
    	Mat temp_sharpmat = new Mat();
    	img_orig.assignTo(temp_sharpmat, CvType.CV_32F);// = Mat(img_orig);
    	
    	float width_of_blur = (float) (pupil_diam/2.0f*Math.abs((float) (1.0f - SCREEN_DISTANCE/last_marker_distance)/2.58f)); //u_Transform[14])/2.58f)));
    	//Log.d("WOB", Float.toString(width_of_blur));
    	
    	Mat gaussian_real = Gaussian_picture(width_of_blur, temp_sharpmat.width(), temp_sharpmat.height());

		//Deconvolution according to PSF
		Mat deconvolution_result = Wiener_Filter(temp_sharpmat, gaussian_real, 0.05, temp_sharpmat.width(), temp_sharpmat.height());

		deconvolution_result.assignTo(deconvolution_result, CvType.CV_8U);
    	
    	return deconvolution_result;
    }

	///////////////////////////////////////////////////////////////////////////
    public OGLRenderer( Context context )
	{
    	this.context = context;
		//Load Square Vertex Data//
		squareVertexData = ByteBuffer.allocateDirect(squareVertices.length*BYTES_PER_FLOAT)
				.order(ByteOrder.nativeOrder()).asFloatBuffer();
		squareVertexData.put(squareVertices);
		
		//Setup the Vuforia Tracker//
		initTracking(960, 492);
	}
	
	@Override
	public void onSurfaceCreated(GL10 gl, EGLConfig config) {
		//Setup openGL Fixed Functionality Calls//
		glClearColor(0.0f, 0.0f, 0.0f, 1.0f);
		glEnable( GL_DEPTH_TEST );
		glDepthFunc( GL_LEQUAL );
		
		//////////////////////////////////
		//Setup the Square Shaders//
		String vertexShaderSource = TextResourceReader.readTextFileFromResource(context, R.raw.square_vertex_shader);
		String fragmentShaderSource = TextResourceReader.readTextFileFromResource(context, R.raw.square_fragment_shader);
		int vertexShader = ShaderHelper.compileVertexShader(vertexShaderSource);
		int fragmentShader = ShaderHelper.compileFragmentShader(fragmentShaderSource);
		squareProgram = ShaderHelper.linkProgram(vertexShader, fragmentShader);
		glUseProgram(squareProgram);
		uSquareColorLocation = glGetUniformLocation(squareProgram, U_COLOR);
		
		uProjectionLocation = glGetUniformLocation(squareProgram, U_PROJECTION);
		uTransformLocation = glGetUniformLocation(squareProgram, U_TRANSFORM);
		
		aSquarePositionLocation = glGetAttribLocation(squareProgram, A_POSITION);
		squareVertexData.position(0);
		glVertexAttribPointer(aSquarePositionLocation, 3, GL_FLOAT, false, 0, squareVertexData);
		//glEnableVertexAttribArray(aSquarePositionLocation);
		
		////////////////////////////////////////////////////////////////////////
		textureProgram = new TextureShaderProgram(this.context);
		blendTextureProgram = new BlendTextureShaderProgram(this.context);
		modelProgram = new ModelShaderProgram(this.context);
		
		DepthRectangle = new GenericObject();
		DepthTexture = TextureHelper.loadTexture(this.context, R.drawable.depth_texture);
		
		EyeImage1_model = new GenericObject();
		EyeImage1_texture = TextureHelper.loadTexture(this.context, R.drawable.eye_source);
		EyeImage1_rotation = 0.0f;
		EyeImage2_model = new GenericObject();
		EyeImage2_texture = TextureHelper.loadTexture(this.context, R.drawable.eye_final);
		EyeImage3_model = new GenericObject();
		EyeImage3_texture = TextureHelper.loadTexture(this.context, R.drawable.eyes_intermediate_2);
		EyeImage4_model = new GenericObject();
		EyeImage4_texture = TextureHelper.loadTexture(this.context, R.drawable.eyes_intermediate_1);
		EyeImage5_model = new GenericObject();
		EyeImage5_texture = TextureHelper.loadTexture(this.context, R.drawable.eye_box);
		
		EvaluationBoardV_model = new GenericObject();
		EvaluationBoardH_model = new GenericObject();
		//EvaluationBoard_texture = TextureHelper.loadTexture(this.context, R.drawable.sharpview_image);//evaluation_texture);
		
		//img_sharp = new Mat();
		img_orig = new Mat();
		try {
			img_orig = Utils.loadResource(this.context, R.drawable.sharpview_image);
			Imgproc.cvtColor(img_orig, img_orig, Imgproc.COLOR_BGRA2GRAY);//COLOR_BGRA2RGBA);
			img_sharp = SharpenImage();
			SharpView_texture = TextureHelper.loadTexture(img_sharp, SharpView_texture);
			NormalView_texture = TextureHelper.loadTexture(img_orig, NormalView_texture);
			
			NormalMode_texture = TextureHelper.loadTexture(this.context, R.drawable.normalview_image);
			timer.scheduleAtFixedRate(updatesharp, 1000, interval*500);
		} catch (IOException e) {
			// TODO Auto-generated catch block
			e.printStackTrace();
		}
		
		ScreenCalibration_model = new GenericObject();
		ScreenCalibration_texture1 = TextureHelper.loadTexture(this.context, R.drawable.screen_calib_intro);
		ScreenCalibration_texture2 = TextureHelper.loadTexture(this.context, R.drawable.screen_calib_1);
		ScreenCalibration_texture3 = TextureHelper.loadTexture(this.context, R.drawable.screen_calib_2);
		ScreenCalibration_texture4 = TextureHelper.loadTexture(this.context, R.drawable.screen_calib_3);
		ScreenCalibration_texture5 = TextureHelper.loadTexture(this.context, R.drawable.screen_calib_4);
		ScreenCalibration_texture6 = TextureHelper.loadTexture(this.context, R.drawable.screen_calib_5);
		ScreenCalibration_blend = 0.0f;
		ScreenCalibration_blend_timer = 0;
		
		Moverio_model = new MoverioObject(this.context, "model.txt");
		Lumus_model = new MoverioObject(this.context, "lmodel.txt");
	}

	@Override
	public void onSurfaceChanged(GL10 gl, int width, int height) {
		glViewport(0, 0, width, height);
		
		WIDTH = width;
		HEIGHT = height;
	}

	public void setCameraOrientationNative(float transform0, float transform1, float transform2, float transform3,
			float transform4, float transform5, float transform6, float transform7, float transform8, float transform9,
			float transform10, float transform11, float transform12, float transform13, float transform14, float transform15) {
	 
		u_Transform[0] = transform0; u_Transform[1] = -transform1; u_Transform[2] = -transform2; u_Transform[3] = transform3;
		u_Transform[4] = transform4; u_Transform[5] = -transform5; u_Transform[6] = -transform6; u_Transform[7] = transform7;
		u_Transform[8] = transform8; u_Transform[9] = -transform9; u_Transform[10] = -transform10; u_Transform[11] = transform11;
		u_Transform[12] = transform12/100.0f; u_Transform[13] = -transform13/100.0f; u_Transform[14] = -transform14/100.0f; u_Transform[15] = transform15;
		
		last_marker_distance = Math.abs(u_Transform[14]);
	}

	public void setTrackedNative(boolean istracked) {
		 tracking = istracked;

		 
		 if ( tracking && mText != null ){
			 runOnUiThread(new Runnable(){
			     public void run() {
			    	 mText.setVisibility(View.INVISIBLE);
			     }
			});
		
		 }else{
			 runOnUiThread(new Runnable(){
			     public void run() {
			    	 mText.setVisibility(View.INVISIBLE);
			     }
			});
		 }
	}

	/**********************************************************
	 	Poster OnDrawFrame Function!!
	 	@Override
	public void onDrawFrame(GL10 gl) {
		updateTracking();        
		
		//Reset the Display Buffers//
		glClear(GL_COLOR_BUFFER_BIT | GL_DEPTH_BUFFER_BIT);
		/////////////////////////////////////////////////////////////////////////
		if ( tracking ){
			glClearColor(0.0f, 0.0f, 0.0f, 1.0f);
			//Move Models//
			DepthRectangle.T_reset();
			DepthRectangle.T_scale(6.0f, 11.0f, 1.0f);
			
			//Eye Tracking Models//
			EyeImage1_model.T_reset();
			EyeImage1_model.T_translate(-.15f, -.3f+.05f, 0.0f);
			EyeImage1_model.T_scale(1.4f, 1.4f, 1.4f);
			EyeImage2_model.T_reset();
			EyeImage2_model.T_translate(-.15f, -.3f+.05f, 0.0f);
			EyeImage2_model.T_scale(1.4f, 1.4f, 1.4f);
			EyeImage3_model.T_reset();
			EyeImage3_model.T_translate(-.15f, -.3f+.05f, 0.0f);
			EyeImage3_model.T_scale(1.4f, 1.4f, 1.4f);
			EyeImage4_model.T_reset();
			EyeImage4_model.T_translate(-.15f, -.3f+.05f, 0.0f);
			EyeImage4_model.T_scale(1.4f, 1.4f, 1.4f);
			EyeImage5_model.T_reset();
			EyeImage5_model.T_translate(-.15f, -.3f+.05f, 0.0f);
			EyeImage5_model.T_scale(1.4f, 1.4f, 1.4f);
			
			EyeImage1_model.T_rotate(EyeImage1_rotation, 0.0f, 1.0f, 0.0f);
			EyeImage1_model.T_translate(0.0f, 0.0f, 0.139f);
			
			EyeImage2_model.T_rotate(EyeImage1_rotation + 72, 0.0f, 1.0f, 0.0f);
			EyeImage2_model.T_translate(0.0f, 0.0f, 0.139f);
			
			EyeImage3_model.T_rotate(EyeImage1_rotation + 144, 0.0f, 1.0f, 0.0f);
			EyeImage3_model.T_translate(0.0f, 0.0f, 0.139f);
			
			EyeImage4_model.T_rotate(EyeImage1_rotation + 216, 0.0f, 1.0f, 0.0f);
			EyeImage4_model.T_translate(0.0f, 0.0f, 0.139f);
			
			EyeImage5_model.T_rotate(EyeImage1_rotation + 288, 0.0f, 1.0f, 0.0f);
			EyeImage5_model.T_translate(0.0f, 0.0f, 0.139f);
			EyeImage1_rotation += .25f;
			if ( EyeImage1_rotation > 360.0f )
				EyeImage1_rotation = .1f;
			
			//Evaluation Board Models//
			EvaluationBoardV_model.T_reset();
			EvaluationBoardV_model.T_translate(.25f, -.345f+.05f, 0.05f);
			EvaluationBoardV_model.T_scale(1.5f, 3.0f, 1.0f);
			
			EvaluationBoardH_model.T_reset();
			EvaluationBoardH_model.T_translate(.25f, -.495f+.05f, 0.20f);
			EvaluationBoardH_model.T_rotate(270.0f, 1.0f, 0.0f, 0.0f);
			EvaluationBoardH_model.T_scale(1.5f, 3.0f, 1.0f);
			
			//Screen Calibration Models//
			ScreenCalibration_model.T_reset();
			ScreenCalibration_model.T_translate(.23f, -.005f+.1f, 0.05f);
			ScreenCalibration_model.T_scale(1.5f, 3.0f, 1.0f);
			ScreenCalibration_blend_timer++;
			if ( ScreenCalibration_blend_timer > 300)
			{
				ScreenCalibration_blend += .01f;
				if ( ScreenCalibration_blend >= 6.0f)
				{
					ScreenCalibration_blend = 0.0f;
				}
			}
			if ( ScreenCalibration_blend_timer > 399)
			{
				ScreenCalibration_blend_timer = 0;
			}
			
			//HMD Models//
			Moverio_model.T_reset();
			Moverio_model.T_translate(-.05f, -.01f+.15f, .2f);
			Moverio_model.T_rotate(EyeImage1_rotation, 1.0f, 0.0f, 0.0f);
			Moverio_model.T_rotate(-EyeImage1_rotation, 0.0f, 1.0f, 0.0f);
			Moverio_model.T_rotate(EyeImage1_rotation, 0.0f, 0.0f, 1.0f);
			Moverio_model.T_scale(.18f, .18f, .18f);
			
			Lumus_model.T_reset();
			Lumus_model.T_translate(-.25f, .16f+.15f, .2f);
			Lumus_model.T_rotate(-EyeImage1_rotation, 1.0f, 0.0f, 0.0f);
			Lumus_model.T_rotate(EyeImage1_rotation, 0.0f, 1.0f, 0.0f);
			Lumus_model.T_rotate(-EyeImage1_rotation, 0.0f, 0.0f, 1.0f);
			Lumus_model.T_scale(.18f, .18f, .18f);
			
			/////////
			
			float[] temp = new float[16];
						
			//Draw Left Eye//
			glViewport(0, 0, WIDTH/2, HEIGHT);
			
			textureProgram.useProgram();
			//Draw Models//
			//Depth Slice//
			multiplyMM(temp, 0, u_Transform, 0, DepthRectangle.T_get(), 0);
			textureProgram.setUniforms(u_ProjectionLeft, temp, DepthTexture);
			DepthRectangle.bindData(textureProgram);
			DepthRectangle.draw();
			
			//Eye Tracking Models//
			multiplyMM(temp, 0, u_Transform, 0, EyeImage1_model.T_get(), 0);
			textureProgram.setUniforms(u_ProjectionLeft, temp, EyeImage1_texture);
			EyeImage1_model.bindData(textureProgram);
			EyeImage1_model.draw();
			
			multiplyMM(temp, 0, u_Transform, 0, EyeImage2_model.T_get(), 0);
			textureProgram.setUniforms(u_ProjectionLeft, temp, EyeImage2_texture);
			EyeImage2_model.bindData(textureProgram);
			EyeImage2_model.draw();
			
			multiplyMM(temp, 0, u_Transform, 0, EyeImage3_model.T_get(), 0);
			textureProgram.setUniforms(u_ProjectionLeft, temp, EyeImage3_texture);
			EyeImage3_model.bindData(textureProgram);
			EyeImage3_model.draw();
			
			multiplyMM(temp, 0, u_Transform, 0, EyeImage4_model.T_get(), 0);
			textureProgram.setUniforms(u_ProjectionLeft, temp, EyeImage4_texture);
			EyeImage4_model.bindData(textureProgram);
			EyeImage4_model.draw();
			
			multiplyMM(temp, 0, u_Transform, 0, EyeImage5_model.T_get(), 0);
			textureProgram.setUniforms(u_ProjectionLeft, temp, EyeImage5_texture);
			EyeImage5_model.bindData(textureProgram);
			EyeImage5_model.draw();
			
			//Evaluation Models//
			multiplyMM(temp, 0, u_Transform, 0, EvaluationBoardV_model.T_get(), 0);
			textureProgram.setUniforms(u_ProjectionLeft, temp, EvaluationBoard_texture);
			EvaluationBoardV_model.bindData(textureProgram);
			EvaluationBoardV_model.draw();
			
			multiplyMM(temp, 0, u_Transform, 0, EvaluationBoardH_model.T_get(), 0);
			textureProgram.setUniforms(u_ProjectionLeft, temp, EvaluationBoard_texture);
			EvaluationBoardH_model.bindData(textureProgram);
			EvaluationBoardH_model.draw();
			
			//Screen Calibration Models//
			blendTextureProgram.useProgram();
			multiplyMM(temp, 0, u_Transform, 0, ScreenCalibration_model.T_get(), 0);
			blendTextureProgram.setUniforms(u_ProjectionLeft, temp, ScreenCalibration_texture1, ScreenCalibration_texture2,
					ScreenCalibration_texture3, ScreenCalibration_texture4, ScreenCalibration_texture5, 
					ScreenCalibration_texture6, ScreenCalibration_blend);
			ScreenCalibration_model.bindData(blendTextureProgram);
			ScreenCalibration_model.draw();
			
			//HMD Models//
			modelProgram.useProgram();
			multiplyMM(temp, 0, u_Transform, 0, Moverio_model.T_get(), 0);
			modelProgram.setUniforms(u_ProjectionLeft, temp, .8f, .9f, .6f);
			Moverio_model.bindData(modelProgram);
			Moverio_model.draw();
			
			modelProgram.useProgram();
			multiplyMM(temp, 0, u_Transform, 0, Lumus_model.T_get(), 0);
			modelProgram.setUniforms(u_ProjectionLeft, temp, .5f, .6f, .9f);
			Lumus_model.bindData(modelProgram);
			Lumus_model.draw();
			
			///////////////////////////////////////////////////////////////////////////
			///////////////////////////////////////////////////////////////////////////
			//Draw Right Eye//
			glViewport(WIDTH/2, 0, WIDTH/2, HEIGHT);
			
			textureProgram.useProgram();
			//Draw Models//
			//Depth Slice//
			multiplyMM(temp, 0, u_Transform, 0, DepthRectangle.T_get(), 0);
			textureProgram.setUniforms(u_ProjectionRight, temp, DepthTexture);
			DepthRectangle.bindData(textureProgram);
			DepthRectangle.draw();
			
			//Eye Tracking Models//
			multiplyMM(temp, 0, u_Transform, 0, EyeImage1_model.T_get(), 0);
			textureProgram.setUniforms(u_ProjectionRight, temp, EyeImage1_texture);
			EyeImage1_model.bindData(textureProgram);
			EyeImage1_model.draw();
			
			multiplyMM(temp, 0, u_Transform, 0, EyeImage2_model.T_get(), 0);
			textureProgram.setUniforms(u_ProjectionRight, temp, EyeImage2_texture);
			EyeImage2_model.bindData(textureProgram);
			EyeImage2_model.draw();
			
			multiplyMM(temp, 0, u_Transform, 0, EyeImage3_model.T_get(), 0);
			textureProgram.setUniforms(u_ProjectionRight, temp, EyeImage3_texture);
			EyeImage3_model.bindData(textureProgram);
			EyeImage3_model.draw();
			
			multiplyMM(temp, 0, u_Transform, 0, EyeImage4_model.T_get(), 0);
			textureProgram.setUniforms(u_ProjectionRight, temp, EyeImage4_texture);
			EyeImage4_model.bindData(textureProgram);
			EyeImage4_model.draw();
			
			multiplyMM(temp, 0, u_Transform, 0, EyeImage5_model.T_get(), 0);
			textureProgram.setUniforms(u_ProjectionRight, temp, EyeImage5_texture);
			EyeImage5_model.bindData(textureProgram);
			EyeImage5_model.draw();
			
			//Evaluation Models//
			multiplyMM(temp, 0, u_Transform, 0, EvaluationBoardV_model.T_get(), 0);
			textureProgram.setUniforms(u_ProjectionRight, temp, EvaluationBoard_texture);
			EvaluationBoardV_model.bindData(textureProgram);
			EvaluationBoardV_model.draw();
			
			multiplyMM(temp, 0, u_Transform, 0, EvaluationBoardH_model.T_get(), 0);
			textureProgram.setUniforms(u_ProjectionRight, temp, EvaluationBoard_texture);
			EvaluationBoardH_model.bindData(textureProgram);
			EvaluationBoardH_model.draw();
			
			//Screen Calibration Models//
			blendTextureProgram.useProgram();
			multiplyMM(temp, 0, u_Transform, 0, ScreenCalibration_model.T_get(), 0);
			blendTextureProgram.setUniforms(u_ProjectionRight, temp, ScreenCalibration_texture1, ScreenCalibration_texture2,
					ScreenCalibration_texture3, ScreenCalibration_texture4, ScreenCalibration_texture5, 
					ScreenCalibration_texture6, ScreenCalibration_blend);
			ScreenCalibration_model.bindData(blendTextureProgram);
			ScreenCalibration_model.draw();

			//HMD Models//
			modelProgram.useProgram();
			multiplyMM(temp, 0, u_Transform, 0, Moverio_model.T_get(), 0);
			modelProgram.setUniforms(u_ProjectionRight, temp, .8f, .9f, .6f);
			Moverio_model.bindData(modelProgram);
			Moverio_model.draw();
			
			modelProgram.useProgram();
			multiplyMM(temp, 0, u_Transform, 0, Lumus_model.T_get(), 0);
			modelProgram.setUniforms(u_ProjectionRight, temp, .5f, .6f, .9f);
			Lumus_model.bindData(modelProgram);
			Lumus_model.draw();
			
			///////////////////////////////////////////////////////////////////////////
		}else
		{
			glClearColor(1.0f, 0.0f, 0.0f, 1.0f);
		}
	}
	************************************************************/
	
	/********************************************************
	 SharpView OnDrawFrame Function!!
	*******************************************************/
	@Override
	public void onDrawFrame(GL10 gl) {
		updateTracking();        
		
		if ( dirty_flag )
		{
			SharpView_texture = TextureHelper.loadTexture(img_sharp, SharpView_texture);
			dirty_flag = false;
			Log.d("dirty", "dirty!");
		}
		
		
		//Reset the Display Buffers//
		glClear(GL_COLOR_BUFFER_BIT | GL_DEPTH_BUFFER_BIT);
		
		EvaluationBoardH_model.T_reset();
		EvaluationBoardH_model.T_translate(-0.1670897f, 0.02499922f, -0.33620003f);
		EvaluationBoardH_model.T_scale(.05f, .1f, 1.0f);
		float[] temp = new float[16];
		/////////////////////////////////////////////////////////////////////////
		if ( tracking ){
			//Move Models//
			//Evaluation Board Models//
			EvaluationBoardV_model.T_reset();
			EvaluationBoardV_model.T_translate(0.0f, 0.0f, 0.0f);
			EvaluationBoardV_model.T_scale(.25f, .5f, 1.0f);
			
			//Calculate Angle to Center//
			float CPD = Math.abs(u_Transform[14]);
			float dist = (float) Math.sqrt(u_Transform[12]*u_Transform[12] + u_Transform[13]*u_Transform[13]);
			float angle = (float) Math.abs(25.0 - (float) (Math.atan(dist/CPD)*180.0/Math.PI));
			//Log.d("X", Float.toString(u_Transform[12]));
			//Log.d("Y", Float.toString(u_Transform[13]));
			//Log.d("Z", Float.toString(u_Transform[14]));
			
			////////Apply SharpView////////
			if ( angle > 5.0 )
			{

			}
			//////Normal View////////
			else if (angle < 3.0 )
			{

			}
			////////////////////////////////////////////		
			///////////////////////////////////////////////////////////////////////////
			///////////////////////////////////////////////////////////////////////////

			//Draw Models//
			//Evaluation Models//
			//Draw Right Eye//
			glViewport(WIDTH/2, 0, WIDTH/2, HEIGHT);
			
			textureProgram.useProgram();
			multiplyMM(temp, 0, u_Transform, 0, EvaluationBoardV_model.T_get(), 0);
			if ( sharpImage )
			{
				textureProgram.setUniforms(u_ProjectionRight, temp, SharpView_texture);
			}
			else
			{
				textureProgram.setUniforms(u_ProjectionRight, temp, NormalView_texture);
			}
			EvaluationBoardV_model.bindData(textureProgram);
			EvaluationBoardV_model.draw();
			
			
			/*
			//Draw Left Eye//
			glViewport(0, 0, WIDTH/2, HEIGHT);
			
			textureProgram.useProgram();
			multiplyMM(temp, 0, u_Transform, 0, EvaluationBoardV_model.T_get(), 0);
			if ( sharpImage )
			{
				textureProgram.setUniforms(u_ProjectionLeft, temp, SharpView_texture);
			}
			else
			{
				textureProgram.setUniforms(u_ProjectionLeft, temp, NormalView_texture);
			}
			EvaluationBoardV_model.bindData(textureProgram);
			EvaluationBoardV_model.draw();
			*/
		}
		if ( !sharpImage )
		{
			multiplyMM(temp, 0, u_Transform_N, 0, EvaluationBoardH_model.T_get(), 0);
			textureProgram.setUniforms(u_ProjectionRight, temp, NormalMode_texture);
			EvaluationBoardH_model.bindData(textureProgram);
			EvaluationBoardH_model.draw();

		}
		///////////////////////////////////////////////////////////////////////////
	}

	/**************************************************************************
	 * @throws IOException
	 * 
	 * This function handles the tap event for the touchpad of the Moverio.
	 * It checks to make sure that the marker is being tracked and then checks
	 * the state (which cross) and passes the 2D pixel location of the cross
	 * and the 3D position data of the marker center to the SVD related math functions
	 * to solve for the SPAAM solution.
	 **************************************************************************/
	public void handleTouchPress() throws IOException{
		sharpImage = !sharpImage;
	}
	
	/***************************************************************************
	 * This function is needed for the touch events callback setup. It does not
	 * currently perform any meaningful function however.
	 **************************************************************************/
	public void handleTouchDrag(){
		
	}


}

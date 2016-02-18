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

import android.app.Activity;
import android.content.Context;
import android.opengl.GLSurfaceView.Renderer;
import android.os.AsyncTask;
import android.os.Environment;
import android.util.Log;
import android.view.View;
import android.widget.TextView;

import com.spaamdemo.R;
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
	
	float[] u_ProjectionLeft = {1f, 0f, 0f, 0f,
			0f, 1, 0f, 0f,
			0f, 0f, 1f, 0f,
			0f, 0f, 0f, 1f};
	float[] u_ProjectionRight = {1f, 0f, 0f, 0f,
					0f, 1, 0f, 0f,
					0f, 0f, 1f, 0f,
					0f, 0f, 0f, 1f};
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
	
	///////////////////////////////////////////////////////////////////
	////////Reading in Calibration Related Items///////////////////////
	File LeftCalibFile = null;
	File RightCalibFile = null;
	
	////////////////////////////////////////////////////////////////////
	
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
						if ( storageDirectory.listFiles()[i].getName().contains("Right")){// == "Right.calib" ){
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
						if ( storageDirectory.listFiles()[i].getName().contains("Left")){ //== "Left.calib" ){
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
	
	///////////////////////////////////////////////////////////////////////////
    public OGLRenderer( Context context )
	{
    	this.context = context;
		//Load Square Vertex Data//
		squareVertexData = ByteBuffer.allocateDirect(squareVertices.length*BYTES_PER_FLOAT)
				.order(ByteOrder.nativeOrder()).asFloatBuffer();
		squareVertexData.put(squareVertices);
		
		//Setup the Vuforia Tracker//
		initTracking(960, 540);
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
	**********************************************************/
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
			glClearColor(0.0f, 0.0f, 0.0f, 1.0f);
		}
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
	
	}
	
	/***************************************************************************
	 * This function is needed for the touch events callback setup. It does not
	 * currently perform any meaningful function however.
	 **************************************************************************/
	public void handleTouchDrag(){
		
	}

}

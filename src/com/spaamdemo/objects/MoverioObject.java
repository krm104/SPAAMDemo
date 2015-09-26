package com.spaamdemo.objects;

import static android.opengl.GLES20.GL_TRIANGLES;
import static android.opengl.GLES20.glDrawArrays;
import static android.opengl.Matrix.rotateM;
import static android.opengl.Matrix.scaleM;
import static android.opengl.Matrix.setIdentityM;
import static android.opengl.Matrix.translateM;

import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.nio.ByteBuffer;
import java.nio.ByteOrder;
import java.nio.FloatBuffer;
import java.util.ArrayList;
import java.util.List;

import android.content.Context;

import com.spaamdemo.data.VertexArray;
import com.spaamdemo.programs.BlendTextureShaderProgram;
import com.spaamdemo.programs.ModelShaderProgram;
import com.spaamdemo.programs.TextureShaderProgram;

public class MoverioObject {
	private static final int POSITION_COMPONENT_COUNT = 3;
	private static final int TEXTURE_COORDINATES_COMPONENT_COUNT = 3;
	private static final int STRIDE = (POSITION_COMPONENT_COUNT + TEXTURE_COORDINATES_COMPONENT_COUNT) * 4;
	
	private final VertexArray vertexArray;
	private float[] tMatrix;
	
	private float[] VERTEX_DATA;

	public MoverioObject(Context context, String model){
		// create Scanner inFile1
		InputStream mFile = null;
		try {
			mFile = context.getResources().getAssets().open(model);
		} catch (IOException e) {
			// TODO Auto-generated catch block
			e.printStackTrace();
		}
		
		byte[] buffer = new byte[5500000];
	    ByteArrayOutputStream outStream = new ByteArrayOutputStream(5500000);
	    int read = -1;
	    while (true) {
	      try {
			read = mFile.read(buffer);
		} catch (IOException e) {
			// TODO Auto-generated catch block
			e.printStackTrace();
		}
	      if (read == -1)
	        break;
	      outStream.write(buffer, 0, read);
	    }
	    ByteBuffer byteData = ByteBuffer.wrap(outStream.toByteArray());
	
	    byteData.order(ByteOrder.LITTLE_ENDIAN);
	    FloatBuffer fBuff = byteData.asFloatBuffer();
	    
	    VERTEX_DATA = new float[fBuff.remaining()];
	    int i = 0;
	    while ( fBuff.hasRemaining())
	    {
	    	VERTEX_DATA[i++] = fBuff.get();
	    }
		vertexArray = new VertexArray(VERTEX_DATA);
		tMatrix = new float[16];
		setIdentityM(tMatrix, 0);
	}
	
	public void bindData(ModelShaderProgram textureProgram)
	{
		vertexArray.setVertexAttribPointer(0, textureProgram.getPositionAttributeLocation(), POSITION_COMPONENT_COUNT, STRIDE);
		
		vertexArray.setVertexAttribPointer(POSITION_COMPONENT_COUNT, textureProgram.getNormalAttributeLocation(),
				3, STRIDE);
	}
	
	public void bindData(TextureShaderProgram textureProgram)
	{
		vertexArray.setVertexAttribPointer(0, textureProgram.getPositionAttributeLocation(), POSITION_COMPONENT_COUNT, STRIDE);
		
		vertexArray.setVertexAttribPointer(POSITION_COMPONENT_COUNT, textureProgram.getTextureCoordinatesAttributeLocation(),
				TEXTURE_COORDINATES_COMPONENT_COUNT, STRIDE);
	}
	
	public void bindData(BlendTextureShaderProgram textureProgram)
	{
		vertexArray.setVertexAttribPointer(0, textureProgram.getPositionAttributeLocation(), POSITION_COMPONENT_COUNT, STRIDE);
		
		vertexArray.setVertexAttribPointer(POSITION_COMPONENT_COUNT, textureProgram.getTextureCoordinatesAttributeLocation(),
				TEXTURE_COORDINATES_COMPONENT_COUNT, STRIDE);
	}
	
	public void draw()
	{
		glDrawArrays(GL_TRIANGLES, 0, VERTEX_DATA.length/(STRIDE/4));
	}

	public void T_reset()
	{
		setIdentityM(tMatrix, 0);
	}
	
	public void T_translate( float x, float y, float z)
	{
		translateM(tMatrix, 0, x, y, z);
	}
	
	public void T_rotate( float deg, float a_x, float a_y, float a_z)
	{
		rotateM(tMatrix, 0, deg, a_x, a_y, a_z);
	}
	
	public void T_scale(float s_x, float s_y, float s_z)
	{
		scaleM(tMatrix, 0, s_x, s_y, s_z);
	}
	
	public float[] T_get()
	{
		return tMatrix;
	}
}

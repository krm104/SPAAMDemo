package com.spaamdemo.objects;

import static android.opengl.GLES20.GL_TRIANGLE_STRIP;
import static android.opengl.GLES20.glDrawArrays;
import static android.opengl.Matrix.rotateM;
import static android.opengl.Matrix.scaleM;
import static android.opengl.Matrix.setIdentityM;
import static android.opengl.Matrix.translateM;

import com.spaamdemo.data.VertexArray;
import com.spaamdemo.programs.BlendTextureShaderProgram;
import com.spaamdemo.programs.ModelShaderProgram;
import com.spaamdemo.programs.TextureShaderProgram;

public class GenericObject {

	private static final int POSITION_COMPONENT_COUNT = 3;
	private static final int TEXTURE_COORDINATES_COMPONENT_COUNT = 2;
	private static int STRIDE = (POSITION_COMPONENT_COUNT + TEXTURE_COORDINATES_COMPONENT_COUNT) * 4;
	
	private final VertexArray vertexArray;
	private float[] tMatrix;
	
	private static final float[] VERTEX_DATA = {
		//order of coordinates X, Y, Z, S, T//		
		-.1f, .05f, 0.0f, 0.0f, 0.0f,
		.1f, .05f, 0.0f, 1.0f, 0.0f,
		-.1f, -.05f, 0.0f, 0.0f, 1.0f,
		.1f, -.05f, 0.0f, 1.0f, 1.0f
	};

	public GenericObject(){
		vertexArray = new VertexArray(VERTEX_DATA);
		tMatrix = new float[16];
		setIdentityM(tMatrix, 0);
	}
	
	public void bindData(ModelShaderProgram textureProgram)
	{
		STRIDE += 4;
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
		glDrawArrays(GL_TRIANGLE_STRIP, 0, VERTEX_DATA.length/(STRIDE/4));
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

package com.spaamdemo.programs;

import static android.opengl.GLES20.glGetAttribLocation;
import static android.opengl.GLES20.glGetUniformLocation;
import static android.opengl.GLES20.glUniform1f;
import static android.opengl.GLES20.glUniform1i;
import static android.opengl.GLES20.glUniform4f;
import static android.opengl.GLES20.glUniformMatrix4fv;
import android.content.Context;

import com.SharpViewDemo.R;

public class ModelShaderProgram extends ShaderProgram{

	//Uniform location//
	private final int uProjectionLocation;
	private final int uTransformLocation;
	private final int uColorLocation;
	
	//Attribute locations//
	private final int aPositionLocation;
	private final int aNormalLocation;
	
	public ModelShaderProgram(Context context)
	{
		super(context, R.raw.model_vertex_shader, R.raw.model_fragment_shader);
		
		//Retrieve uniform locations for the shader program//
		uProjectionLocation = glGetUniformLocation(program, U_PROJECTION);
		uTransformLocation = glGetUniformLocation(program, U_TRANSFORM);
		uColorLocation = glGetUniformLocation(program, "u_Color");
		
		//Retrieve attribute locations for the shader program//
		aPositionLocation = glGetAttribLocation(program, A_POSITION);
		aNormalLocation = glGetAttribLocation(program, "a_Normal");
	}
	
	public void setUniforms(float[] ProjMatrix, float[] TransMatrix, float r, float g, float b)
	{
		//Pass the matrix into the shader program//
		glUniformMatrix4fv(uProjectionLocation, 1, false, ProjMatrix, 0);
		glUniformMatrix4fv(uTransformLocation, 1, false, TransMatrix, 0);	
		glUniform4f(uColorLocation, r, g, b, 1.0f);
	}

	public int getPositionAttributeLocation()
	{
		return aPositionLocation;
	}
	
	public int getNormalAttributeLocation()
	{
		return aNormalLocation;
	}
}

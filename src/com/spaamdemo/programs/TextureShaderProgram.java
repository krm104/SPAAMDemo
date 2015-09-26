package com.spaamdemo.programs;

import android.content.Context;
import static android.opengl.GLES20.glGetUniformLocation;
import static android.opengl.GLES20.glGetAttribLocation;
import static android.opengl.GLES20.glUniformMatrix4fv;
import static android.opengl.GLES20.GL_TEXTURE0;
import static android.opengl.GLES20.GL_TEXTURE_2D;
import static android.opengl.GLES20.glUniform1i;
import static android.opengl.GLES20.glActiveTexture;
import static android.opengl.GLES20.glBindTexture;

import com.SharpViewDemo.R;

public class TextureShaderProgram extends ShaderProgram{
	
	//Uniform location//
		private final int uProjectionLocation;
		private final int uTransformLocation;
		private final int uTextureUnitLocation;
		
		//Attribute locations//
		private final int aPositionLocation;
		private final int aTextureCoordinatesLocation;
		
		public TextureShaderProgram(Context context)
		{
			super(context, R.raw.texture_vertex_shader, R.raw.texture_fragment_shader);
			
			//Retrieve uniform locations for the shader program//
			uProjectionLocation = glGetUniformLocation(program, U_PROJECTION);
			uTransformLocation = glGetUniformLocation(program, U_TRANSFORM);
			uTextureUnitLocation = glGetUniformLocation(program, U_TEXTURE_UNIT);
			
			//Retrieve attribute locations for the shader program//
			aPositionLocation = glGetAttribLocation(program, A_POSITION);
			aTextureCoordinatesLocation = glGetAttribLocation(program, A_TEXTURE_COORDINATES);
		}
		
		public void setUniforms(float[] ProjMatrix, float[] TransMatrix, int textureId)
		{
			//Pass the matrix into the shader program//
			glUniformMatrix4fv(uProjectionLocation, 1, false, ProjMatrix, 0);
			glUniformMatrix4fv(uTransformLocation, 1, false, TransMatrix, 0);
			
			//Set the active texture unit to the texture unit 0//
			glActiveTexture(GL_TEXTURE0);
			
			//Bind the texture to this unit//
			glBindTexture(GL_TEXTURE_2D, textureId);
			
			//Tell the texture uniform sample to use this texture in the shader by
			//telling it to read from texture unit 0//
			glUniform1i(uTextureUnitLocation, 0);
		}

		public int getPositionAttributeLocation()
		{
			return aPositionLocation;
		}
		
		public int getTextureCoordinatesAttributeLocation()
		{
			return aTextureCoordinatesLocation;
		}

}

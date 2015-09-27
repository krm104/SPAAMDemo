package com.spaamdemo.programs;

import static android.opengl.GLES20.GL_TEXTURE0;
import static android.opengl.GLES20.GL_TEXTURE1;
import static android.opengl.GLES20.GL_TEXTURE2;
import static android.opengl.GLES20.GL_TEXTURE3;
import static android.opengl.GLES20.GL_TEXTURE4;
import static android.opengl.GLES20.GL_TEXTURE5;
import static android.opengl.GLES20.GL_TEXTURE_2D;
import static android.opengl.GLES20.glActiveTexture;
import static android.opengl.GLES20.glBindTexture;
import static android.opengl.GLES20.glGetAttribLocation;
import static android.opengl.GLES20.glGetUniformLocation;
import static android.opengl.GLES20.glUniform1i;
import static android.opengl.GLES20.glUniform1f;
import static android.opengl.GLES20.glUniformMatrix4fv;
import android.content.Context;

import com.spaamdemo.R;

public class BlendTextureShaderProgram extends ShaderProgram{

	//Uniform location//
	private final int uProjectionLocation;
	private final int uTransformLocation;
	
	private final int uTextureUnitLocation1;
	private final int uTextureUnitLocation2;
	private final int uTextureUnitLocation3;
	private final int uTextureUnitLocation4;
	private final int uTextureUnitLocation5;
	private final int uTextureUnitLocation6;
	
	private final int uBlendPerc;
	
	//Attribute locations//
	private final int aPositionLocation;
	private final int aTextureCoordinatesLocation;
	
	public BlendTextureShaderProgram(Context context)
	{
		super(context, R.raw.texture_vertex_shader, R.raw.blend_texture_fragment_shader);
		
		//Retrieve uniform locations for the shader program//
		uProjectionLocation = glGetUniformLocation(program, U_PROJECTION);
		uTransformLocation = glGetUniformLocation(program, U_TRANSFORM);
		uTextureUnitLocation1 = glGetUniformLocation(program, "u_TextureUnit1");
		uTextureUnitLocation2 = glGetUniformLocation(program, "u_TextureUnit2");
		uTextureUnitLocation3 = glGetUniformLocation(program, "u_TextureUnit3");
		uTextureUnitLocation4 = glGetUniformLocation(program, "u_TextureUnit4");
		uTextureUnitLocation5 = glGetUniformLocation(program, "u_TextureUnit5");
		uTextureUnitLocation6 = glGetUniformLocation(program, "u_TextureUnit6");
		uBlendPerc = glGetUniformLocation( program, "u_BlendPerc");
		
		//Retrieve attribute locations for the shader program//
		aPositionLocation = glGetAttribLocation(program, A_POSITION);
		aTextureCoordinatesLocation = glGetAttribLocation(program, A_TEXTURE_COORDINATES);
	}
	
	public void setUniforms(float[] ProjMatrix, float[] TransMatrix, int textureId1, int textureId2, int textureId3,
			int textureId4, int textureId5, int textureId6, float blendperc)
	{
		//Pass the matrix into the shader program//
		glUniformMatrix4fv(uProjectionLocation, 1, false, ProjMatrix, 0);
		glUniformMatrix4fv(uTransformLocation, 1, false, TransMatrix, 0);
		
		glActiveTexture(GL_TEXTURE0);
		glBindTexture(GL_TEXTURE_2D, textureId1);
		glUniform1i(uTextureUnitLocation1, 0);
		
		glActiveTexture(GL_TEXTURE1);
		glBindTexture(GL_TEXTURE_2D, textureId2);
		glUniform1i(uTextureUnitLocation2, 1);
		
		glActiveTexture(GL_TEXTURE2);
		glBindTexture(GL_TEXTURE_2D, textureId3);
		glUniform1i(uTextureUnitLocation3, 2);
		
		glActiveTexture(GL_TEXTURE3);
		glBindTexture(GL_TEXTURE_2D, textureId4);
		glUniform1i(uTextureUnitLocation4, 3);
		
		glActiveTexture(GL_TEXTURE4);
		glBindTexture(GL_TEXTURE_2D, textureId5);
		glUniform1i(uTextureUnitLocation5, 4);
		
		glActiveTexture(GL_TEXTURE5);
		glBindTexture(GL_TEXTURE_2D, textureId6);
		glUniform1i(uTextureUnitLocation6, 5);
		
		glUniform1f(uBlendPerc, blendperc);
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

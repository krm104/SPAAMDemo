package com.spaamdemo.programs;

import android.content.Context;
import static android.opengl.GLES20.glUseProgram;
import com.spaamdemo.util.ShaderHelper;
import com.spaamdemo.util.TextResourceReader;

public class ShaderProgram {
	//Uniform constants//
	protected static final String U_PROJECTION = "u_Projection";
	protected static final String U_TRANSFORM = "u_Transform";
	protected static final String U_TEXTURE_UNIT = "U_TextureUnit";	
	//Attribute constants//
	protected static final String A_POSITION = "a_Position";
	protected static final String A_COLOR = "a_Color";
	protected static final String A_TEXTURE_COORDINATES = "a_TextureCoordinates";
	
	//Shader program//
	protected final int program;
	
	protected ShaderProgram(Context context, int vertexShaderResourceId, int fragmentShaderResourceId)
	{
		//compile the shaders and link the program//
		program = ShaderHelper.buildProgram(
				TextResourceReader.readTextFileFromResource(
						context, vertexShaderResourceId),
				TextResourceReader.readTextFileFromResource(
						context, fragmentShaderResourceId));
	}
	
	public void useProgram()
	{
		//Set the current OpenGL shader program to this program//
		glUseProgram(program);
	}
	
}

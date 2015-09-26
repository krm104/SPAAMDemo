package com.spaamdemo.data;

import java.nio.ByteBuffer;
import java.nio.ByteOrder;
import java.nio.FloatBuffer;
import static android.opengl.GLES20.GL_FLOAT;
import static android.opengl.GLES20.glVertexAttribPointer;
import static android.opengl.GLES20.glEnableVertexAttribArray;

public class VertexArray {
	private final FloatBuffer floatBuffer;

	public VertexArray(float[] vertexData)
	{
		floatBuffer = ByteBuffer
				.allocateDirect(vertexData.length * 4)
				.order(ByteOrder.nativeOrder())
				.asFloatBuffer()
				.put(vertexData);
	}
	
	public void setVertexAttribPointer(int dataOffset, int attributeLocation, int componentCount, int stride)
	{
		floatBuffer.position(dataOffset);
		glVertexAttribPointer(attributeLocation, componentCount, GL_FLOAT, false, stride, floatBuffer);
		glEnableVertexAttribArray(attributeLocation);
		
		floatBuffer.position(0);
	}
}

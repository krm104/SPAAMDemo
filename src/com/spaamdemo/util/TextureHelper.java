package com.spaamdemo.util;

import static android.opengl.GLES20.GL_LINEAR;
import static android.opengl.GLES20.GL_LINEAR_MIPMAP_LINEAR;
import static android.opengl.GLES20.GL_TEXTURE_2D;
import static android.opengl.GLES20.GL_TEXTURE_MAG_FILTER;
import static android.opengl.GLES20.GL_TEXTURE_MIN_FILTER;
import static android.opengl.GLES20.glBindTexture;
import static android.opengl.GLES20.glDeleteTextures;
import static android.opengl.GLES20.glGenTextures;
import static android.opengl.GLES20.glGenerateMipmap;
import static android.opengl.GLES20.glTexParameteri;
import static android.opengl.GLUtils.texImage2D;

import org.opencv.android.Utils;
import org.opencv.core.Mat;

import android.content.Context;
import android.graphics.Bitmap;
import android.graphics.BitmapFactory;

public class TextureHelper {
		public static int loadTexture(Context context, int resourceId)
		{
			final int[] textureObjectIds = new int[1];
			glGenTextures(1, textureObjectIds, 0);
			
			if (textureObjectIds[0] == 0)
			{
				return 0;
			}
			
			final BitmapFactory.Options options = new BitmapFactory.Options();
			options.inScaled = false;
			
			final Bitmap bitmap = BitmapFactory.decodeResource(context.getResources(),  resourceId, options);
			
			if (bitmap == null)
			{
				glDeleteTextures(1, textureObjectIds, 0);
				return 0;
			}
			
			glBindTexture(GL_TEXTURE_2D, textureObjectIds[0]);
			glTexParameteri(GL_TEXTURE_2D, GL_TEXTURE_MIN_FILTER, GL_LINEAR_MIPMAP_LINEAR);
			glTexParameteri(GL_TEXTURE_2D, GL_TEXTURE_MAG_FILTER, GL_LINEAR);
			
			texImage2D(GL_TEXTURE_2D, 0, bitmap, 0);
			bitmap.recycle();
			glGenerateMipmap(GL_TEXTURE_2D);
			glBindTexture(GL_TEXTURE_2D, 0);
			
			return textureObjectIds[0];
		}
		
		public static int loadTexture(Bitmap bitmap)
		{
			final int[] textureObjectIds = new int[1];
			glGenTextures(1, textureObjectIds, 0);
			
			if (textureObjectIds[0] == 0)
			{
				return 0;
			}
			
			if (bitmap == null)
			{
				glDeleteTextures(1, textureObjectIds, 0);
				return 0;
			}
			
			glBindTexture(GL_TEXTURE_2D, textureObjectIds[0]);
			glTexParameteri(GL_TEXTURE_2D, GL_TEXTURE_MIN_FILTER, GL_LINEAR_MIPMAP_LINEAR);
			glTexParameteri(GL_TEXTURE_2D, GL_TEXTURE_MAG_FILTER, GL_LINEAR);
			
			texImage2D(GL_TEXTURE_2D, 0, bitmap, 0);
			//bitmap.recycle();
			glGenerateMipmap(GL_TEXTURE_2D);
			glBindTexture(GL_TEXTURE_2D, 0);
			
			return textureObjectIds[0];
		}
		
		public static int loadTexture(Mat bitmap, int TextureID)
		{
			//if (TextureID <= 0)
			{
				final int[] textureObjectIds = new int[1];
				glGenTextures(1, textureObjectIds, 0);
			
				if (textureObjectIds[0] == 0)
				{
					return 0;
				}
				
				if (bitmap == null)
				{
					glDeleteTextures(1, textureObjectIds, 0);
					return 0;
				}
				
				TextureID = textureObjectIds[0];
			}
			
			glBindTexture(GL_TEXTURE_2D, TextureID);
			glTexParameteri(GL_TEXTURE_2D, GL_TEXTURE_MIN_FILTER, GL_LINEAR_MIPMAP_LINEAR);
			glTexParameteri(GL_TEXTURE_2D, GL_TEXTURE_MAG_FILTER, GL_LINEAR);

			Bitmap.Config conf = Bitmap.Config.ARGB_8888; //see other conf types
			Bitmap newBit = Bitmap.createBitmap(bitmap.width(), bitmap.height(), conf);
					
			Utils.matToBitmap(bitmap, newBit);
			texImage2D(GL_TEXTURE_2D, 0, newBit, 0);
			newBit.recycle();
			glGenerateMipmap(GL_TEXTURE_2D);
			glBindTexture(GL_TEXTURE_2D, 0);
			
			return TextureID;
		}
}

#version 100
//Projection Matrices//
uniform mat4 u_Projection;

//Transformation Matrices//
uniform mat4 u_Transform;

attribute vec4 a_Position;
attribute vec2 a_TextureCoordinates;

varying vec2 v_TextureCoordinates;

void main( )
{
	v_TextureCoordinates = a_TextureCoordinates;
	gl_Position = u_Projection*u_Transform*a_Position;
}
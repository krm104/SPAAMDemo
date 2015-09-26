precision mediump float;

uniform float u_BlendPerc;

uniform sampler2D u_TextureUnit1;
uniform sampler2D u_TextureUnit2;
uniform sampler2D u_TextureUnit3;
uniform sampler2D u_TextureUnit4;
uniform sampler2D u_TextureUnit5;
uniform sampler2D u_TextureUnit6;

varying vec2 v_TextureCoordinates;

void main()
{
	vec4 color1 = vec4(1.0, 1.0, 1.0, 1.0);
	vec4 color2 = vec4(1.0, 1.0, 1.0, 1.0);
	vec4 finalcolor = vec4(1.0, 1.0, 1.0, 1.0);
	float perc = 0.0;
	
	if ( u_BlendPerc < 1.0 )
	{
		perc = 1.0 - u_BlendPerc; 
		color1 = texture2D(u_TextureUnit1, v_TextureCoordinates);
		color2 = texture2D(u_TextureUnit2, v_TextureCoordinates);
		finalcolor = perc*color1 + (1.0-perc)*color2;
	}
	else if ( u_BlendPerc < 2.0 )
	{
		perc = 2.0 - u_BlendPerc; 
		color1 = texture2D(u_TextureUnit2, v_TextureCoordinates);
		color2 = texture2D(u_TextureUnit3, v_TextureCoordinates);
		finalcolor = perc*color1 + (1.0-perc)*color2;
	}
	else if ( u_BlendPerc < 3.0 )
	{
		perc = 3.0 - u_BlendPerc; 
		color1 = texture2D(u_TextureUnit3, v_TextureCoordinates);
		color2 = texture2D(u_TextureUnit4, v_TextureCoordinates);
		finalcolor = perc*color1 + (1.0-perc)*color2;
	}
	else if ( u_BlendPerc < 4.0 )
	{
		perc = 4.0 - u_BlendPerc; 
		color1 = texture2D(u_TextureUnit4, v_TextureCoordinates);
		color2 = texture2D(u_TextureUnit5, v_TextureCoordinates);
		finalcolor = perc*color1 + (1.0-perc)*color2;
	}
	else if ( u_BlendPerc < 5.0 )
	{
		perc = 5.0 - u_BlendPerc; 
		color1 = texture2D(u_TextureUnit5, v_TextureCoordinates);
		color2 = texture2D(u_TextureUnit6, v_TextureCoordinates);
		finalcolor = perc*color1 + (1.0-perc)*color2;
	}
	else if ( u_BlendPerc < 6.0 )
	{
		perc = 6.0 - u_BlendPerc; 
		color1 = texture2D(u_TextureUnit6, v_TextureCoordinates);
		color2 = texture2D(u_TextureUnit1, v_TextureCoordinates);
		finalcolor = perc*color1 + (1.0-perc)*color2;
	}
	gl_FragColor = finalcolor;
}
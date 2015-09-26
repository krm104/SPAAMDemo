precision mediump float;

uniform vec4 u_Color;

varying float LightIntensity;

void main()
{
	gl_FragColor = u_Color*LightIntensity;
}
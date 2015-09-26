#version 100
//Projection Matrices//
uniform mat4 u_Projection;

//Transformation Matrices//
uniform mat4 u_Transform;

//Vertex Coordinates//
attribute vec4 a_Position;
attribute vec4 a_Normal;

varying float LightIntensity;

void main()
{
	vec3 LightPosition = vec3(0.0, .3, 1.0);

    vec3 ecPosition = vec3(u_Transform*a_Position).xyz;
    vec3 tnorm      = normalize(mat3(u_Transform)*a_Normal.xyz).xyz;
    vec3 lightVec   = normalize(LightPosition - ecPosition);
    vec3 reflectVec = reflect(-lightVec, tnorm);
    vec3 viewVec    = normalize(ecPosition);

    float spec      = clamp(dot(reflectVec, viewVec), 0.0, 1.0);
    spec            = pow(spec, 16.0);

    LightIntensity  = 1.0 * max(dot(lightVec, tnorm), 0.0)
                      + .01 * spec;

    gl_Position = u_Projection*u_Transform*a_Position;
}
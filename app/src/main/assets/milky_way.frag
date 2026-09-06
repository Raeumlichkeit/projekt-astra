precision highp float;
uniform sampler2D skyTexture;
uniform vec2 resolution;
uniform vec2 textureSize;
uniform vec3 view;
uniform mat3 horizontalToJ2000;
uniform float arMode;
uniform float enhanced;
uniform float strength;
const float PI = 3.141592653589793;

// Bilinear wrap at RA=12h without requiring power-of-two texture dimensions on GLES2.
vec3 sampleSky(vec2 uv) {
    float pixel = uv.x * textureSize.x - 0.5;
    float left = floor(pixel);
    float y = clamp(uv.y, 0.5 / textureSize.y, 1.0 - 0.5 / textureSize.y);
    vec2 a = vec2((mod(left, textureSize.x) + 0.5) / textureSize.x, y);
    vec2 b = vec2((mod(left + 1.0, textureSize.x) + 0.5) / textureSize.x, y);
    return mix(texture2D(skyTexture, a).rgb, texture2D(skyTexture, b).rgb, fract(pixel));
}

void main() {
    vec2 pixel = vec2(gl_FragCoord.x, resolution.y - gl_FragCoord.y);
    float az = view.x;
    float alt = view.y;
    vec3 direction;
    if (arMode > 0.5) {
        az += (pixel.x / resolution.x - 0.5) * view.z;
        alt += (resolution.y * 0.5 - pixel.y) * view.z / resolution.x;
        if (abs(alt) > PI * 0.5) { gl_FragColor = vec4(0.0); return; }
        direction = vec3(cos(alt) * sin(az), cos(alt) * cos(az), sin(alt));
    } else {
        float focal = resolution.x / (2.0 * tan(view.z * 0.5));
        vec2 offset = vec2(pixel.x - resolution.x * 0.5, resolution.y * 0.5 - pixel.y) / focal;
        vec3 right = vec3(cos(az), -sin(az), 0.0);
        vec3 up = vec3(-sin(alt) * sin(az), -sin(alt) * cos(az), cos(alt));
        vec3 forward = vec3(cos(alt) * sin(az), cos(alt) * cos(az), sin(alt));
        direction = normalize(forward + right * offset.x + up * offset.y);
    }
    float horizonGlow = pow(1.0 - abs(direction.z), 5.0);
    vec3 background = mix(vec3(0.007, 0.012, 0.021), vec3(0.018, 0.028, 0.044), horizonGlow);
    if (strength <= 0.0) {
        gl_FragColor = arMode > 0.5 ? vec4(0.0) : vec4(background, 1.0);
        return;
    }
    vec3 equatorial = normalize(horizontalToJ2000 * direction);
    float ra = atan(equatorial.y, equatorial.x);
    float dec = asin(clamp(equatorial.z, -1.0, 1.0));
    vec2 uv = vec2(fract(0.5 - ra / (2.0 * PI)), 0.5 - dec / PI);
    vec3 source = sampleSky(uv);
    float luminance = dot(source, vec3(0.2126, 0.7152, 0.0722));
    // Display styling, not a model of sky brightness or naked-eye visibility.
    vec3 diffuse = mix(vec3(luminance), source, mix(0.28, 0.85, enhanced));
    diffuse = pow(max(diffuse, vec3(0.0)), vec3(mix(1.25, 0.9, enhanced)));
    float gain = strength * mix(0.48, 0.94, enhanced);
    if (arMode > 0.5) {
        float alpha = clamp(luminance * strength * 0.34, 0.0, 0.38);
        // Premultiplied alpha for transparent TextureView composition over the camera.
        gl_FragColor = vec4(mix(vec3(0.65), source, 0.25) * alpha, alpha);
    } else {
        gl_FragColor = vec4(clamp(background + diffuse * gain, 0.0, 1.0), 1.0);
    }
}

#version 150

in vec2 FragCoord;
in vec4 FragColor;

uniform vec2 Size;
uniform vec4 Radius;
uniform float Smoothness;
uniform vec4 ColorModulator;

out vec4 OutColor;

float roundedBoxSDF(vec2 p, vec2 b, vec4 r) {
    r.xy = (p.x > 0.0) ? r.xy : r.zw;
    r.x  = (p.y > 0.0) ? r.x  : r.y;
    vec2 q = abs(p) - b + r.x;
    return min(max(q.x, q.y), 0.0) + length(max(q, 0.0)) - r.x;
}

void main() {
    vec2 center = Size * 0.5;
    vec2 p = (FragCoord * Size) - center;
    float dist = roundedBoxSDF(p, center - 1.0, Radius);

    float alpha = 1.0 - smoothstep(0.0, Smoothness, dist);
    if (alpha <= 0.001) {
        discard;
    }

    vec4 finalColor = vec4(FragColor.rgb, FragColor.a * alpha);
    OutColor = finalColor * ColorModulator;
}

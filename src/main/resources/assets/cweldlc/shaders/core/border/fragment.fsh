#version 150

in vec2 FragCoord;
in vec4 FragColor;

uniform vec2 Size;
uniform vec4 Radius;
uniform float Smoothness;
uniform float Thickness;
uniform vec4 ColorModulator;

out vec4 OutColor;

float roundedBoxSDF(vec2 p, vec2 b, vec4 r) {
    r = min(r, vec4(b.x, b.y, b.x, b.y));
    r.xy = (p.x > 0.0) ? r.xy : r.zw;
    r.x  = (p.y > 0.0) ? r.x  : r.y;
    vec2 q = abs(p) - b + r.x;
    return min(max(q.x, q.y), 0.0) + length(max(q, 0.0)) - r.x;
}

void main() {
    vec2 center = Size * 0.5;
    vec2 b = max(vec2(0.5), center - 1.0);
    vec2 p = (FragCoord * Size) - center;
    float dist = roundedBoxSDF(p, b, Radius);

    float sm = max(0.2, Smoothness);
    float inner = smoothstep(-Thickness - sm, -Thickness, dist);
    float outer = 1.0 - smoothstep(-sm, 0.0, dist);
    float alpha = clamp(inner * outer, 0.0, 1.0);

    if (alpha <= 0.002) {
        discard;
    }

    vec4 finalColor = vec4(FragColor.rgb, FragColor.a * alpha);
    OutColor = finalColor * ColorModulator;
}

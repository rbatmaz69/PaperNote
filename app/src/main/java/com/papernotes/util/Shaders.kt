package com.papernotes.util

import org.intellij.lang.annotations.Language

/**
 * AGSL-Quelltext (API 33+). Erzeugt ein "Aquarell-Trickfilm-Papier":
 * - weiche, ungleichmäßige Farbwäsche (fbm-Value-Noise) zwischen Basis- und Wash-Ton,
 * - sanfte Posterisierung der Helligkeit in wenige Bänder (Cel-/Zeichentrick-Look),
 * - sparsame dunkle Papierflocken (Einschlüsse),
 * - großzügige, weiche Vignette wie eine Buchseite.
 * Bewusst kontrastarm, damit Text gut lesbar bleibt.
 */
@Language("AGSL")
val PAPER_SHADER_SRC = """
    uniform float2 iResolution;
    uniform float3 iBase;
    uniform float3 iWash;
    uniform float iStrength;

    float hash(float2 p) {
        p = fract(p * float2(123.34, 456.21));
        p += dot(p, p + 45.32);
        return fract(p.x * p.y);
    }

    float vnoise(float2 p) {
        float2 i = floor(p);
        float2 f = fract(p);
        float a = hash(i);
        float b = hash(i + float2(1.0, 0.0));
        float c = hash(i + float2(0.0, 1.0));
        float d = hash(i + float2(1.0, 1.0));
        float2 u = f * f * (3.0 - 2.0 * f);
        return mix(mix(a, b, u.x), mix(c, d, u.x), u.y);
    }

    float fbm(float2 p) {
        float v = 0.0;
        float amp = 0.5;
        for (int i = 0; i < 4; i++) {
            v += amp * vnoise(p);
            p *= 2.0;
            amp *= 0.5;
        }
        return v;
    }

    half4 main(float2 fragCoord) {
        float2 uv = fragCoord / iResolution;
        float aspect = iResolution.x / iResolution.y;
        float2 p = float2(uv.x * aspect, uv.y);

        // weiche, organische Wäsche
        float w = fbm(p * 3.0);
        w = smoothstep(0.25, 0.85, w);

        // Helligkeits-Wäsche + Cel-Posterisierung (wenige weiche Bänder)
        float shade = mix(0.93, 1.05, w);
        float bands = 11.0;
        shade = floor(shade * bands + 0.5) / bands;

        float3 col = iBase * shade;

        // in dunkleren Zonen einen Hauch Wash-Ton einmischen → "coloriert"
        col = mix(col, iWash, (1.0 - w) * 0.12 * iStrength);

        // sparsame Papierflocken
        float fleck = hash(floor(fragCoord / 3.0));
        col *= 1.0 - step(0.987, fleck) * 0.10;

        // sehr feines Korn, damit die Bänder nicht zu "digital" wirken
        col += (hash(fragCoord) - 0.5) * 0.012;

        // weiche Vignette zu den Rändern
        float2 d = uv - 0.5;
        float vig = 1.0 - dot(d, d) * 0.22;
        col *= vig;

        return half4(clamp(col, 0.0, 1.0), 1.0);
    }
""".trimIndent()

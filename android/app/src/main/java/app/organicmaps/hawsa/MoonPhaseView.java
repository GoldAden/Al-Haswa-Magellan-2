package app.organicmaps.hawsa;

import android.animation.Animator;
import android.animation.AnimatorListenerAdapter;
import android.animation.ValueAnimator;
import android.content.Context;
import android.graphics.Canvas;
import android.graphics.Color;
import android.graphics.Matrix;
import android.graphics.Paint;
import android.graphics.Path;
import android.graphics.RadialGradient;
import android.graphics.Shader;
import android.os.Build;
import android.util.AttributeSet;
import android.view.View;
import android.view.animation.AccelerateDecelerateInterpolator;
import android.view.animation.LinearInterpolator;

import java.util.ArrayList;
import java.util.List;
import java.util.Random;

/**
 * Beautiful animated moon phase view with:
 * - Pulsing golden glow around the moon
 * - Smooth terminator (phase) transitions
 * - Twinkling stars in background
 * - Subtle slow rotation of the star field
 * - Shadow/illumination animated appearance
 *
 * Theme: dark navy #0D1B2A background, gold #D4AF37 accents, white/blue moon
 */
public class MoonPhaseView extends View {

    // Theme colors
    private static final int COLOR_BG = 0xFF0D1B2A;
    private static final int COLOR_MOON_DARK = 0xFF1E293B;
    private static final int COLOR_MOON_LIT = 0xFFF1F5F9;
    private static final int COLOR_MOON_EDGE = 0xFF94A3B8;
    private static final int COLOR_GLOW_INNER = 0xFFD4AF37;
    private static final int COLOR_GLOW_OUTER = 0x20D4AF37;
    private static final int COLOR_STAR = 0xFFE0E1DD;
    private static final int COLOR_STAR_BRIGHT = 0xFFFFFFFF;

    // Paints
    private Paint paintBg;
    private Paint paintMoonLit;
    private Paint paintMoonDark;
    private Paint paintMoonEdge;
    private Paint paintGlow;
    private Paint paintStar;
    private Paint paintStarBright;

    // Star data
    private static class Star {
        float x, y;
        float radius;
        float baseAlpha;
        float phase; // for twinkling offset
    }

    private List<Star> stars = new ArrayList<>();
    private boolean starsInitialized = false;

    // Moon parameters
    private float phase = 0.0f;  // 0=new, 0.5=full, 1.0=new again
    private float illumination = 0.0f; // 0-1
    private float viewCenterX, viewCenterY;
    private float moonRadius;

    // Animation state
    private float glowAlpha = 0.4f;      // pulsing glow 0.2-0.8
    private float glowScale = 1.0f;     // glow ring size pulse 1.0-1.15
    private float starTwinkle = 0.0f;  // star twinkle phase
    private float starFieldRotation = 0.0f; // slow rotation of stars
    private float appearanceProgress = 0.0f; // 0-1 for initial fade-in
    private float targetPhase = 0.0f;  // target phase for smooth transition
    private float animatedPhase = 0.0f; // current animated phase value

    // Animators
    private ValueAnimator glowPulseAnimator;
    private ValueAnimator starTwinkleAnimator;
    private ValueAnimator rotationAnimator;
    private ValueAnimator phaseTransitionAnimator;
    private ValueAnimator appearAnimator;

    public MoonPhaseView(Context context) {
        super(context);
        init();
    }

    public MoonPhaseView(Context context, AttributeSet attrs) {
        super(context, attrs);
        init();
    }

    public MoonPhaseView(Context context, AttributeSet attrs, int defStyleAttr) {
        super(context, attrs, defStyleAttr);
        init();
    }

    private void init() {
        // Background
        paintBg = new Paint(Paint.ANTI_ALIAS_FLAG);
        paintBg.setColor(COLOR_BG);
        paintBg.setStyle(Paint.Style.FILL);

        // Moon lit side
        paintMoonLit = new Paint(Paint.ANTI_ALIAS_FLAG);
        paintMoonLit.setColor(COLOR_MOON_LIT);
        paintMoonLit.setStyle(Paint.Style.FILL);

        // Moon dark side
        paintMoonDark = new Paint(Paint.ANTI_ALIAS_FLAG);
        paintMoonDark.setColor(COLOR_MOON_DARK);
        paintMoonDark.setStyle(Paint.Style.FILL);

        // Moon edge highlight
        paintMoonEdge = new Paint(Paint.ANTI_ALIAS_FLAG);
        paintMoonEdge.setColor(COLOR_MOON_EDGE);
        paintMoonEdge.setStyle(Paint.Style.STROKE);
        paintMoonEdge.setStrokeWidth(1.5f);

        // Glow paint (radial gradient, updated per frame)
        paintGlow = new Paint(Paint.ANTI_ALIAS_FLAG);
        paintGlow.setStyle(Paint.Style.FILL);

        // Star paints
        paintStar = new Paint(Paint.ANTI_ALIAS_FLAG);
        paintStar.setStyle(Paint.Style.FILL);

        paintStarBright = new Paint(Paint.ANTI_ALIAS_FLAG);
        paintStarBright.setStyle(Paint.Style.FILL);

        // Start animations
        startGlowPulse();
        startStarTwinkle();
        startStarFieldRotation();
        startAppearanceAnimation();
    }

    /**
     * Set the moon phase (0-1 where 0=new, 0.25=first quarter, 0.5=full, 0.75=last quarter, 1=new)
     */
    public void setPhase(float phase, float illumination) {
        this.targetPhase = phase;
        this.illumination = illumination;

        // Smooth phase transition animation
        if (phaseTransitionAnimator != null && phaseTransitionAnimator.isRunning()) {
            phaseTransitionAnimator.cancel();
        }

        float startPhase = this.animatedPhase;
        // Handle wrap-around (e.g. from 0.9 to 0.1 should go forward through 1.0)
        float diff = this.targetPhase - startPhase;
        if (Math.abs(diff) > 0.5f) {
            if (diff > 0) startPhase += 1.0f;
            else startPhase -= 1.0f;
        }

        phaseTransitionAnimator = ValueAnimator.ofFloat(startPhase, this.targetPhase);
        phaseTransitionAnimator.setDuration(800);
        phaseTransitionAnimator.setInterpolator(new AccelerateDecelerateInterpolator());
        phaseTransitionAnimator.addUpdateListener(anim -> {
            animatedPhase = (float) anim.getAnimatedValue() % 1.0f;
            if (animatedPhase < 0) animatedPhase += 1.0f;
            invalidate();
        });
        phaseTransitionAnimator.start();
    }

    // ===== Animation methods =====

    private void startGlowPulse() {
        glowPulseAnimator = ValueAnimator.ofFloat(0.3f, 0.75f);
        glowPulseAnimator.setDuration(2500);
        glowPulseAnimator.setRepeatMode(ValueAnimator.REVERSE);
        glowPulseAnimator.setRepeatCount(ValueAnimator.INFINITE);
        glowPulseAnimator.setInterpolator(new AccelerateDecelerateInterpolator());
        glowPulseAnimator.addUpdateListener(anim -> {
            glowAlpha = (float) anim.getAnimatedValue();
            glowScale = 1.0f + (glowAlpha - 0.3f) * 0.25f; // 1.0 to ~1.11
            invalidate();
        });
        glowPulseAnimator.start();
    }

    private void startStarTwinkle() {
        starTwinkleAnimator = ValueAnimator.ofFloat(0, (float)(2 * Math.PI));
        starTwinkleAnimator.setDuration(4000);
        starTwinkleAnimator.setRepeatCount(ValueAnimator.INFINITE);
        starTwinkleAnimator.setInterpolator(new LinearInterpolator());
        starTwinkleAnimator.addUpdateListener(anim -> {
            starTwinkle = (float) anim.getAnimatedValue();
            invalidate();
        });
        starTwinkleAnimator.start();
    }

    private void startStarFieldRotation() {
        rotationAnimator = ValueAnimator.ofFloat(0, 360);
        rotationAnimator.setDuration(120000); // Very slow: full rotation in 2 minutes
        rotationAnimator.setRepeatCount(ValueAnimator.INFINITE);
        rotationAnimator.setInterpolator(new LinearInterpolator());
        rotationAnimator.addUpdateListener(anim -> {
            starFieldRotation = (float) anim.getAnimatedValue();
            invalidate();
        });
        rotationAnimator.start();
    }

    private void startAppearanceAnimation() {
        appearAnimator = ValueAnimator.ofFloat(0, 1);
        appearAnimator.setDuration(600);
        appearAnimator.setInterpolator(new AccelerateDecelerateInterpolator());
        appearAnimator.addUpdateListener(anim -> {
            appearanceProgress = (float) anim.getAnimatedValue();
            invalidate();
        });
        appearAnimator.start();
    }

    // ===== Drawing =====

    private void initStars(int w, int h) {
        if (starsInitialized) return;
        starsInitialized = true;

        Random rand = new Random(42); // Fixed seed for consistent star positions
        stars.clear();

        int starCount = 80;
        for (int i = 0; i < starCount; i++) {
            Star s = new Star();
            s.x = rand.nextFloat() * w;
            s.y = rand.nextFloat() * h;
            s.radius = rand.nextFloat() * 1.5f + 0.5f;
            s.baseAlpha = rand.nextFloat() * 0.5f + 0.2f;
            s.phase = rand.nextFloat() * (float)(2 * Math.PI);
            stars.add(s);
        }
    }

    @Override
    protected void onDraw(Canvas canvas) {
        super.onDraw(canvas);

        int w = getWidth();
        int h = getHeight();
        initStars(w, h);

        viewCenterX = w / 2f;
        viewCenterY = h / 2f;
        moonRadius = Math.min(w, h) * 0.22f;

        // 1) Background
        canvas.drawRect(0, 0, w, h, paintBg);

        // 2) Rotated star field
        canvas.save();
        canvas.rotate(starFieldRotation, viewCenterX, viewCenterY);
        drawStars(canvas, w, h);
        canvas.restore();

        // 3) Moon glow (behind moon)
        if (appearanceProgress > 0.3f) {
            drawMoonGlow(canvas);
        }

        // 4) Moon body
        if (appearanceProgress > 0.5f) {
            drawMoonBody(canvas);
        }
    }

    private void drawStars(Canvas canvas, int w, int h) {
        for (Star s : stars) {
            // Twinkle: alpha varies sinusoidally
            float twinkleAlpha = s.baseAlpha + 0.3f * (float) Math.sin(starTwinkle + s.phase);
            twinkleAlpha = Math.max(0.05f, Math.min(1.0f, twinkleAlpha));
            twinkleAlpha *= appearanceProgress;

            // Occasional bright flash
            boolean bright = (Math.sin(starTwinkle * 2 + s.phase * 3) > 0.85);

            if (bright) {
                paintStarBright.setColor(COLOR_STAR_BRIGHT);
                paintStarBright.setAlpha((int)(twinkleAlpha * 255));
                canvas.drawCircle(s.x, s.y, s.radius * 1.5f, paintStarBright);
            } else {
                paintStar.setColor(COLOR_STAR);
                paintStar.setAlpha((int)(twinkleAlpha * 255));
                canvas.drawCircle(s.x, s.y, s.radius, paintStar);
            }
        }
    }

    private void drawMoonGlow(Canvas canvas) {
        float glowRadius = moonRadius * 1.6f * glowScale;
        float effectiveAlpha = glowAlpha * appearanceProgress;

        // Outer glow
        RadialGradient glowGrad = new RadialGradient(
            viewCenterX, viewCenterY, glowRadius,
            new int[] {
                Color.argb((int)(effectiveAlpha * 180), 0xD4, 0xAF, 0x37),
                Color.argb((int)(effectiveAlpha * 60), 0xD4, 0xAF, 0x37),
                Color.argb(0, 0xD4, 0xAF, 0x37)
            },
            new float[] {0.3f, 0.65f, 1.0f},
            Shader.TileMode.CLAMP
        );

        paintGlow.setShader(glowGrad);
        paintGlow.setAlpha((int)(effectiveAlpha * 255));
        canvas.drawCircle(viewCenterX, viewCenterY, glowRadius, paintGlow);
    }

    private void drawMoonBody(Canvas canvas) {
        float scale = appearanceProgress;
        float effectiveRadius = moonRadius * scale;

        // Full moon circle (lit side base)
        paintMoonLit.setAlpha((int)(255 * scale));
        canvas.drawCircle(viewCenterX, viewCenterY, effectiveRadius, paintMoonLit);

        // Draw the dark side overlay using terminator
        drawTerminator(canvas, effectiveRadius, scale);

        // Moon edge ring
        paintMoonEdge.setAlpha((int)(180 * scale));
        canvas.drawCircle(viewCenterX, viewCenterY, effectiveRadius, paintMoonEdge);

        // Surface detail (subtle craters)
        if (scale > 0.7f) {
            drawCraters(canvas, effectiveRadius);
        }
    }

    private void drawTerminator(Canvas canvas, float radius, float scale) {
        // Phase: 0=new (all dark), 0.25=first quarter, 0.5=full (all lit), 0.75=last quarter, 1.0=new
        float p = animatedPhase;

        // The terminator line position
        float terminatorX;

        Path darkPath = new Path();

        if (p <= 0.5f) {
            // Waxing: right side lit, dark moves from right to left
            // At p=0, full dark; at p=0.25, first quarter (right half lit); at p=0.5, full moon

            // Dark path covers the left portion
            darkPath.moveTo(viewCenterX, viewCenterY - radius);

            if (p < 0.25f) {
                // Crescent: terminator curves inward on right side of center
                float curvature = (0.25f - p) / 0.25f; // 1 at new, 0 at quarter
                terminatorX = viewCenterX + radius * curvature;

                // Right arc (lit edge)
                darkPath.arcTo(
                    viewCenterX - radius, viewCenterY - radius,
                    viewCenterX + radius, viewCenterY + radius,
                    -90, -180, true
                );
                // Terminator curve (elliptical approximation)
                float curveX = viewCenterX + radius * (1 - 2 * p / 0.5f);
                for (float angle = 90; angle >= -90; angle -= 5) {
                    float y = viewCenterY + radius * (float) Math.sin(Math.toRadians(angle));
                    float x = viewCenterX + radius * (1 - 2 * p / 0.5f) * (float) Math.cos(Math.toRadians(angle));
                    darkPath.lineTo(x, y);
                }
            } else {
                // Gibbous: terminator on left side of center
                darkPath.arcTo(
                    viewCenterX - radius, viewCenterY - radius,
                    viewCenterX + radius, viewCenterY + radius,
                    -90, -180, true
                );
                float curveFactor = (p - 0.25f) / 0.25f; // 0 at quarter, 1 at full
                for (float angle = 90; angle >= -90; angle -= 5) {
                    float y = viewCenterY + radius * (float) Math.sin(Math.toRadians(angle));
                    float x = viewCenterX - radius * curveFactor * (float) Math.cos(Math.toRadians(angle));
                    darkPath.lineTo(x, y);
                }
            }

            darkPath.close();

        } else {
            // Waning: left side lit, dark moves from left to right
            float wp = p - 0.5f; // 0 at full, 0.5 at new

            darkPath.moveTo(viewCenterX, viewCenterY - radius);

            if (wp < 0.25f) {
                // Waning gibbous: most still lit, terminator moves left-to-center
                darkPath.arcTo(
                    viewCenterX - radius, viewCenterY - radius,
                    viewCenterX + radius, viewCenterY + radius,
                    -90, 180, false
                );
                float curveFactor = wp / 0.25f; // 0 at full, 1 at third quarter
                for (float angle = 90; angle >= -90; angle -= 5) {
                    float y = viewCenterY + radius * (float) Math.sin(Math.toRadians(angle));
                    float x = viewCenterX - radius * (1 - curveFactor) * (float) Math.cos(Math.toRadians(angle));
                    darkPath.lineTo(x, y);
                }
            } else {
                // Waning crescent: left side lit
                darkPath.arcTo(
                    viewCenterX - radius, viewCenterY - radius,
                    viewCenterX + radius, viewCenterY + radius,
                    -90, 180, false
                );
                float curveFactor = 1 - (wp - 0.25f) / 0.25f; // 1 at third quarter, 0 at new
                for (float angle = 90; angle >= -90; angle -= 5) {
                    float y = viewCenterY + radius * (float) Math.sin(Math.toRadians(angle));
                    float x = viewCenterX - radius * curveFactor * (float) Math.cos(Math.toRadians(angle));
                    darkPath.lineTo(x, y);
                }
            }

            darkPath.close();
        }

        paintMoonDark.setAlpha((int)(230 * scale));
        canvas.drawPath(darkPath, paintMoonDark);
    }

    private void drawCraters(Canvas canvas, float radius) {
        // Subtle surface detail — a few small darker circles
        Paint craterPaint = new Paint(Paint.ANTI_ALIAS_FLAG);
        craterPaint.setStyle(Paint.Style.FILL);
        craterPaint.setColor(0xFFCBD5E1); // slightly darker than moon lit
        craterPaint.setAlpha(35);

        float r = radius;
        // Some fixed crater positions relative to moon center
        canvas.drawCircle(viewCenterX + r * 0.3f, viewCenterY - r * 0.2f, r * 0.06f, craterPaint);
        canvas.drawCircle(viewCenterX - r * 0.15f, viewCenterY + r * 0.25f, r * 0.05f, craterPaint);
        canvas.drawCircle(viewCenterX + r * 0.1f, viewCenterY + r * 0.1f, r * 0.07f, craterPaint);
        canvas.drawCircle(viewCenterX - r * 0.3f, viewCenterY - r * 0.15f, r * 0.04f, craterPaint);
        canvas.drawCircle(viewCenterX + r * 0.35f, viewCenterY + r * 0.3f, r * 0.05f, craterPaint);
    }

    // ===== Lifecycle =====

    @Override
    protected void onDetachedFromWindow() {
        super.onDetachedFromWindow();
        stopAllAnimations();
    }

    public void stopAllAnimations() {
        if (glowPulseAnimator != null) glowPulseAnimator.cancel();
        if (starTwinkleAnimator != null) starTwinkleAnimator.cancel();
        if (rotationAnimator != null) rotationAnimator.cancel();
        if (phaseTransitionAnimator != null) phaseTransitionAnimator.cancel();
        if (appearAnimator != null) appearAnimator.cancel();
    }

    public void startAllAnimations() {
        if (glowPulseAnimator == null || !glowPulseAnimator.isRunning()) startGlowPulse();
        if (starTwinkleAnimator == null || !starTwinkleAnimator.isRunning()) startStarTwinkle();
        if (rotationAnimator == null || !rotationAnimator.isRunning()) startStarFieldRotation();
    }
}
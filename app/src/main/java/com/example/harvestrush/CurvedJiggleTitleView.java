package com.example.harvestrush;

import android.content.Context;
import android.content.res.TypedArray;
import android.graphics.BlurMaskFilter;
import android.graphics.Canvas;
import android.graphics.Color;
import android.graphics.Paint;
import android.graphics.Path;
import android.graphics.RectF;
import android.graphics.Typeface;
import android.text.TextUtils;
import android.util.AttributeSet;
import android.view.View;

/**
 * CurvedJiggleTitleView
 * Draws text along a semicircular arc (curved) with optional breathing / jiggle animation.
 * Attributes (see attrs.xml):
 *  - harvestTitleFontAsset: path inside assets/ (e.g. fonts/luckiest_guy.ttf)
 *  - harvestTitleBreathing: enables subtle periodic scale pulsing.
 *  - harvestTitleStraight: if true, draw straight baseline text (no curved path).
 *
 * For performance the drawing is relatively lightweight; the view invalidates every frame
 * (approx 60fps) only if animation is enabled. If no breathing and no jiggle needed, it will
 * not continuously invalidate.
 */
public class CurvedJiggleTitleView extends View {
    private String text = "";
    private final Paint paint = new Paint(Paint.ANTI_ALIAS_FLAG);
    private final Paint glowPaint = new Paint(Paint.ANTI_ALIAS_FLAG);
    private final Paint extrudePaint = new Paint(Paint.ANTI_ALIAS_FLAG);
    private final Path arcPath = new Path();
    private boolean breathing;
    private boolean straight;
    private Typeface customTypeface;

    // Animation timing
    private long startTimeMs;

    // Configurable factors
    private static final float BREATH_AMPLITUDE = 0.05f; // scale +/-5%
    private static final float BREATH_SPEED = 1.2f; // cycles per second
    private static final float ARC_SWEEP_DEG = 120f; // gentle curve (smaller sweep than 180)
    private static final float LINE_VERTICAL_SPACING = 1.15f; // tighter for straight lines
    private static final float WIDTH_FILL_FRACTION = 0.9f;
    private static final int FIT_ITERATIONS = 3; // small refinement passes

    // Adaptive sizing state
    private boolean adaptiveEnabled=false;
    private int designWidthPx=0;
    private float adaptiveBaseSp=0f, adaptiveMinSp=0f, adaptiveMaxSp=0f;

    // 3D / Glow configuration (defaults)
    private int primaryColor = Color.WHITE;
    private int shadowColor = 0xCC222222;
    private int glowColor = 0xFFFFE9A0;
    private int depthLayers = 6;
    private float extrudeOffsetPx = 0f; // per-layer offset (pixels)
    private float glowRadiusPx = 0f;
    private float pulseAmplitude = 0.08f; // scale amplitude (fraction)
    private float pulseSpeed = 1.0f; // cycles per second

    // New fields for dynamic text and modes
    private boolean forceHarvestRush = true; // default previous behavior
    private boolean curvedMode = false;      // when true (and not forcing harvest rush) single-line text draws curved
    private boolean invertedCurve = false;   // new: draw opposite curve direction when single-line curved
    private boolean verticalStackMode = false; // new: display characters stacked vertically
    private float curvedSweepDeg = 160f; // new configurable sweep for curved single-line mode (0<deg<=340 recommended)
    private float letterSpacingEm = 0f;  // optional letter spacing (em units)

    public CurvedJiggleTitleView(Context context) { this(context, null); }
    public CurvedJiggleTitleView(Context context, AttributeSet attrs) { this(context, attrs, 0); }
    public CurvedJiggleTitleView(Context context, AttributeSet attrs, int defStyleAttr) {
        super(context, attrs, defStyleAttr);
        paint.setColor(Color.WHITE);
        paint.setTextSize(sp(54)); // start bigger, we'll scale down if needed
        paint.setFakeBoldText(true);
        paint.setTextAlign(Paint.Align.CENTER);
        setLayerType(LAYER_TYPE_SOFTWARE, null);
        if (attrs != null) {
            TypedArray a = context.obtainStyledAttributes(attrs, R.styleable.CurvedJiggleTitleView);
            String fontAsset = a.getString(R.styleable.CurvedJiggleTitleView_harvestTitleFontAsset);
            breathing = a.getBoolean(R.styleable.CurvedJiggleTitleView_harvestTitleBreathing, false);
            straight = true; // force straight mode
            // we ignore provided colors to keep white 3D look
            shadowColor = a.getColor(R.styleable.CurvedJiggleTitleView_harvestTitleShadowColor, 0xBF000000);
            depthLayers = a.getInt(R.styleable.CurvedJiggleTitleView_harvestTitleDepthLayers, 8);
            float extrudeDp = a.getFloat(R.styleable.CurvedJiggleTitleView_harvestTitleExtrudeOffsetDp, 1.8f);
            float glowDp = a.getFloat(R.styleable.CurvedJiggleTitleView_harvestTitleGlowRadiusDp, 26f);
            pulseAmplitude = a.getFloat(R.styleable.CurvedJiggleTitleView_harvestTitlePulseAmplitude, 0f); // default off
            pulseSpeed = a.getFloat(R.styleable.CurvedJiggleTitleView_harvestTitlePulseSpeed, pulseSpeed);
            a.recycle();
            if (!TextUtils.isEmpty(fontAsset)) {
                try { customTypeface = Typeface.createFromAsset(getContext().getAssets(), fontAsset); } catch (Exception ignored) {}
                if (customTypeface != null) paint.setTypeface(customTypeface);
            }
            extrudeOffsetPx = dpF(extrudeDp);
            glowRadiusPx = dpF(glowDp);
            if(pulseAmplitude>0) breathing=true; else breathing=false;
        }
        primaryColor = Color.WHITE;
        glowColor = 0x66FFFFFF;
        if(TextUtils.isEmpty(text)) text = "HARVEST\nRUSH"; // enforce two lines
        configurePaints();
        startTimeMs = System.currentTimeMillis();
        setWillNotDraw(false);
    }

    private void configurePaints(){
        extrudePaint.set(paint);
        extrudePaint.setColor(shadowColor);
        glowPaint.set(paint);
        glowPaint.setColor(glowColor);
        glowPaint.setMaskFilter(glowRadiusPx>0? new BlurMaskFilter(glowRadiusPx, BlurMaskFilter.Blur.NORMAL): null);
    }

    // Public API additions
    public void setForceHarvestRush(boolean force){
        if(this.forceHarvestRush!=force){
            this.forceHarvestRush=force;
            if(force){
                // reset to fixed two-line text
                this.text="HARVEST\nRUSH";
            }
            requestLayout(); invalidate();
        }
    }
    public void setCurvedMode(boolean curved){
        if(this.curvedMode!=curved){
            this.curvedMode=curved; requestLayout(); invalidate();
        }
    }
    public void setInvertedCurve(boolean inverted){ if(this.invertedCurve!=inverted){ this.invertedCurve=inverted; invalidate(); } }
    public void setVerticalStackMode(boolean vertical){ if(this.verticalStackMode!=vertical){ this.verticalStackMode=vertical; requestLayout(); invalidate(); } }
    public void setCurvedSweepDeg(float deg){
        if(deg < 30f) deg = 30f; if(deg>340f) deg=340f;
        if(this.curvedSweepDeg!=deg){ this.curvedSweepDeg=deg; requestLayout(); invalidate(); }
    }
    public void setLetterSpacing(float em){
        if(this.letterSpacingEm!=em){ this.letterSpacingEm=em; if(android.os.Build.VERSION.SDK_INT>=21){ paint.setLetterSpacing(em); glowPaint.setLetterSpacing(em); extrudePaint.setLetterSpacing(em);} requestLayout(); invalidate(); }
    }

    public void setTitleText(String t){
        if(forceHarvestRush){
            // ignore custom text when forced mode
            if(!"HARVEST\nRUSH".equals(this.text)) { this.text="HARVEST\nRUSH"; requestLayout(); invalidate(); }
            return;
        }
        if(t==null) t=""; t=t.trim();
        // Normalize newline usage
        // Uppercase only the fixed harvest/rush variant; leave custom case as provided (except we keep exclamations)
        this.text = t;
        requestLayout();
        invalidate();
    }
    // === Compatibility alias methods (used elsewhere in project) ===
    /** Alias for setTitleText to support existing calls using setText */
    public void setText(String t){ setTitleText(t); }
    /** Alias for setBreathing(boolean) named setBreathingEnabled */
    public void setBreathingEnabled(boolean enabled){ setBreathing(enabled); }
    /** Alias for setStraight(boolean) named setStraightMode */
    public void setStraightMode(boolean enabled){ setStraight(enabled); }
    /** Alias for setTextSizeSp(float) named setBaseSizeSp */
    public void setBaseSizeSp(float sp){ setTextSizeSp(sp); }

    public void setBreathing(boolean enabled){ if(breathing!=enabled){ breathing=enabled; invalidate(); } }
    public void setStraight(boolean enabled){ if(straight!=enabled){ straight=enabled; requestLayout(); invalidate(); } }
    public void setTitleFontAsset(String asset){ if(TextUtils.isEmpty(asset)) return; try{ Typeface tf=Typeface.createFromAsset(getContext().getAssets(), asset); customTypeface=tf; paint.setTypeface(tf); invalidate(); }catch(Exception ignored){} }
    public void setTextSizeSp(float sp){ paint.setTextSize(sp(sp)); requestLayout(); invalidate(); }
    public void setPrimaryColor(int c){ primaryColor = c; paint.setColor(c); invalidate(); }
    public void setShadowColor(int c){ shadowColor = c; extrudePaint.setColor(c); invalidate(); }
    public void setGlowColor(int c){ glowColor = c; glowPaint.setColor(c); invalidate(); }
    public void setGlowRadiusDp(float dp){ glowRadiusPx = dpF(dp); glowPaint.setMaskFilter(new BlurMaskFilter(glowRadiusPx, BlurMaskFilter.Blur.NORMAL)); invalidate(); }
    public void setPulse(float amplitude,float speed){ this.pulseAmplitude=amplitude; this.pulseSpeed=speed; breathing = amplitude>0; invalidate(); }

    /**
     * Enable adaptive sizing: scales text size relative to actual view width compared to a design reference width.
     * @param designWidthPx reference screen width you designed the base size for (e.g., current screenWidthPx)
     * @param baseSp base size (applied when view width == designWidthPx)
     * @param minSp minimum clamped size
     * @param maxSp maximum clamped size
     */
    public void enableAdaptiveSizing(int designWidthPx, float baseSp, float minSp, float maxSp){
        if(designWidthPx <=0) return;
        this.designWidthPx=designWidthPx;
        adaptiveBaseSp=baseSp;
        adaptiveMinSp=minSp;
        adaptiveMaxSp=maxSp;
        adaptiveEnabled=true;
        applyAdaptiveSizing();
    }
    public void disableAdaptiveSizing(){ adaptiveEnabled=false; }
    private void applyAdaptiveSizing(){
        if(!adaptiveEnabled) return;
        int w=getWidth();
        if(w<=0||designWidthPx<=0) return;
        float ratio = w/(float)designWidthPx;
        float targetSp = adaptiveBaseSp * ratio;
        if(targetSp < adaptiveMinSp) targetSp = adaptiveMinSp;
        if(targetSp > adaptiveMaxSp) targetSp = adaptiveMaxSp;
        paint.setTextSize(sp(targetSp));
        requestLayout();
        invalidate();
    }

    // Provide missing helper for splitting multi-line text safely.
    private String[] getLines(){
        if(TextUtils.isEmpty(text)) return new String[0];
        String[] raw = text.split("\r?\n");
        // Filter out trailing empty lines that can skew measurement.
        int end = raw.length;
        while(end>0 && raw[end-1].trim().isEmpty()) end--;
        if(end==raw.length) return raw;
        String[] trimmed = new String[end];
        System.arraycopy(raw,0,trimmed,0,end);
        return trimmed;
    }

    @Override protected void onSizeChanged(int w, int h, int oldw, int oldh) {
        super.onSizeChanged(w, h, oldw, oldh);
        applyAdaptiveSizing();
    }

    @Override protected void onMeasure(int widthMeasureSpec, int heightMeasureSpec) {
        if(forceHarvestRush){
            // original straight two-line sizing
            String[] lines = new String[]{"HARVEST","RUSH"};
            int specMode = MeasureSpec.getMode(widthMeasureSpec);
            int specSize = MeasureSpec.getSize(widthMeasureSpec);
            if(specMode != MeasureSpec.UNSPECIFIED){
                int availW = specSize - getPaddingLeft() - getPaddingRight();
                if(availW>0){
                    for(int i=0;i<FIT_ITERATIONS;i++){
                        float maxLine = computeMaxLineWidth(lines);
                        if(maxLine<=0) break;
                        float desired = availW * WIDTH_FILL_FRACTION;
                        float scale = desired / maxLine;
                        if(Math.abs(scale-1f) < 0.015f) break;
                        float newSize = paint.getTextSize()*scale;
                        float newSp = newSize / getResources().getDisplayMetrics().scaledDensity;
                        if(newSp>150f) newSp=150f; if(newSp<32f) newSp=32f;
                        paint.setTextSize(sp(newSp));
                    }
                }
            }
            float textSize = paint.getTextSize();
            float lineHeight = textSize * LINE_VERTICAL_SPACING;
            float maxWidth = computeMaxLineWidth(lines);
            int desiredWidth = (int)(maxWidth + getPaddingLeft() + getPaddingRight());
            if(MeasureSpec.getMode(widthMeasureSpec)!=MeasureSpec.UNSPECIFIED) desiredWidth = MeasureSpec.getSize(widthMeasureSpec);
            int desiredHeight = (int)(getPaddingTop() + textSize + lineHeight + getPaddingBottom());
            setMeasuredDimension(resolveSize(desiredWidth,widthMeasureSpec), resolveSize(desiredHeight,heightMeasureSpec));
            return;
        }
        // Dynamic mode
        String[] lines = text.split("\n");
        if(lines.length==1 && verticalStackMode){
            // Measure vertical stack: width ~ max char width, height = chars * spacing
            String line = lines[0]; if(line.isEmpty()) line = " "; float maxChar = 0f; for(int i=0;i<line.length();i++){ String ch=line.substring(i,i+1); float w=paint.measureText(ch); if(w>maxChar) maxChar=w; } float textSize = paint.getTextSize(); float spacing = textSize * 1.12f; int desiredW = (int)(maxChar + getPaddingLeft()+getPaddingRight()); int desiredH = (int)(line.length()*spacing + getPaddingTop()+getPaddingBottom()); setMeasuredDimension(resolveSize(desiredW,widthMeasureSpec), resolveSize(desiredH,heightMeasureSpec)); return;
        }
        if(lines.length==1 && curvedMode){
            // Curved single-line measurement: width ~ text width, height ~ radius portion
            float baseSize = paint.getTextSize();
            float textW = paint.measureText(lines[0]);
            float thetaRad = (float)Math.toRadians(curvedSweepDeg);
            float r = textW / thetaRad; if(r < baseSize*0.8f) r = baseSize*0.8f;
            // vertical footprint: proportion of radius depending on sweep (smaller sweep -> shallower curve)
            float vertical = r * ( (curvedSweepDeg/180f) * 0.6f ) + baseSize*0.4f;
            int desiredW = (int)(textW + getPaddingLeft()+getPaddingRight());
            int desiredH = (int)(vertical + getPaddingTop()+getPaddingBottom());
            setMeasuredDimension(resolveSize(desiredW,widthMeasureSpec), resolveSize(desiredH,heightMeasureSpec));
            return;
        }
        // Straight dynamic lines
        float maxW = computeMaxLineWidth(lines); float textSize = paint.getTextSize(); float lineHeight = textSize * LINE_VERTICAL_SPACING; int desiredW = (int)(maxW + getPaddingLeft()+getPaddingRight()); int desiredH = (int)(getPaddingTop() + lines.length*lineHeight + (textSize*(1f - LINE_VERTICAL_SPACING)) + getPaddingBottom()); setMeasuredDimension(resolveSize(desiredW,widthMeasureSpec), resolveSize(desiredH,heightMeasureSpec));
    }

    @Override protected void onDraw(Canvas canvas) {
        if(forceHarvestRush){
            super.onDraw(canvas);
            String[] lines = {"HARVEST","RUSH"};
            long now = System.currentTimeMillis();
            float elapsed = (now - startTimeMs)/1000f;
            float scale = 1f;
            if(breathing && pulseAmplitude>0f){ scale += pulseAmplitude * (float)Math.sin(elapsed * Math.PI*2f * pulseSpeed); }
            canvas.save(); canvas.scale(scale, scale, getWidth()/2f, getHeight()/2f);
            float textSize = paint.getTextSize();
            float lineHeight = textSize * LINE_VERTICAL_SPACING;
            float firstBaseline = getPaddingTop() + textSize;
            for(int i=0;i<lines.length;i++){
                float y = firstBaseline + i*lineHeight;
                drawStraight3DLine(canvas, lines[i], y);
            }
            canvas.restore();
            if(breathing) postInvalidateOnAnimation();
            return;
        }
        super.onDraw(canvas);
        if(text.isEmpty()) return;
        String[] lines = text.split("\n");
        long now = System.currentTimeMillis();
        float elapsed = (now - startTimeMs)/1000f;
        float scale = 1f;
        if(breathing && pulseAmplitude>0f){ scale += pulseAmplitude * (float)Math.sin(elapsed * Math.PI*2f * pulseSpeed); }
        canvas.save(); canvas.scale(scale, scale, getWidth()/2f, getHeight()/2f);
        if(lines.length==1){
            if(verticalStackMode){ drawVerticalStack3D(canvas, lines[0]); }
            else if(curvedMode){ drawSingleCurved3D(canvas, lines[0]); }
            else { float textSize=paint.getTextSize(); float firstBaseline=getPaddingTop()+textSize; drawStraight3DLine(canvas, lines[0], firstBaseline); }
        } else {
            float textSize=paint.getTextSize(); float lineHeight=textSize*LINE_VERTICAL_SPACING; float firstBaseline=getPaddingTop()+textSize; for(int i=0;i<lines.length;i++){ float y=firstBaseline + i*lineHeight; drawStraight3DLine(canvas, lines[i], y);} }
        canvas.restore(); if(breathing) postInvalidateOnAnimation();
    }

    private void drawSingleCurved3D(Canvas canvas, String line){
        arcPath.reset();
        float centerX = getWidth()/2f;
        float textWidth = paint.measureText(line);
        float thetaRad = (float)Math.toRadians(curvedSweepDeg);
        float r = textWidth/thetaRad; if(r < paint.getTextSize()*0.8f) r = paint.getTextSize()*0.8f;
        float top = getPaddingTop();
        RectF oval = new RectF(centerX - r, top, centerX + r, top + 2*r);
        if(!invertedCurve){
            float start = 180f + curvedSweepDeg/2f; float sweep = -curvedSweepDeg; arcPath.addArc(oval, start, sweep);
        } else {
            float start = 90f - curvedSweepDeg/2f; float sweep = curvedSweepDeg; // bottom arc rising
            arcPath.addArc(oval, start, sweep);
        }
        float arcLen = r*thetaRad; float hOffset=(arcLen - textWidth)/2f; if(hOffset<0) hOffset=0;
        float vOffset = invertedCurve? paint.getTextSize()*0.18f : -paint.getTextSize()*0.12f;
        if(glowRadiusPx>0) canvas.drawTextOnPath(line, arcPath, hOffset, vOffset, glowPaint);
        for(int i=depthLayers;i>=1;i--){ canvas.save(); float off=i*extrudeOffsetPx; canvas.translate(off,off); canvas.drawTextOnPath(line, arcPath, hOffset, vOffset, extrudePaint); canvas.restore(); }
        paint.setColor(primaryColor); canvas.drawTextOnPath(line, arcPath, hOffset, vOffset, paint);
    }

    private void drawStraight3DLine(Canvas canvas, String line, float baselineY){
        float x = getWidth()/2f;
        // Glow under everything
        if(glowRadiusPx>0) canvas.drawText(line, x, baselineY, glowPaint);
        // Extrusion (shadow) layers back to front
        for(int i=depthLayers;i>=1;i--){ float off = i*extrudeOffsetPx; canvas.drawText(line, x+off, baselineY+off, extrudePaint); }
        paint.setColor(primaryColor);
        canvas.drawText(line, x, baselineY, paint);
    }

    private void drawVerticalStack3D(Canvas canvas, String line){
        if(line==null) return;
        line = line.toUpperCase();
        float textSize = paint.getTextSize();
        float spacing = textSize * 1.12f;
        float centerX = getWidth()/2f;
        float startY = getPaddingTop() + textSize; // baseline of first char
        int row = 0;
        for(int i=0;i<line.length();i++){
            char c = line.charAt(i);
            if(c==' '||c=='\n' || c=='\t') continue; // skip spacing chars
            String ch = String.valueOf(c);
            float baselineY = startY + row * spacing;
            float offsetNorm = (line.length()==1)?0f: (row/(float)Math.max(1,(line.replace(" ","").length()-1))); // normalized excluding spaces
            float xOffset = (float)Math.sin((offsetNorm-0.5f)*Math.PI) * textSize * 0.3f; // symmetrical outward for slight arch feel
            drawStraight3DLine(canvas, ch, baselineY, centerX + xOffset);
            row++;
        }
    }

    // Overload supporting custom x
    private void drawStraight3DLine(Canvas canvas, String line, float baselineY, float centerX){
        if(glowRadiusPx>0) canvas.drawText(line, centerX, baselineY, glowPaint);
        for(int i=depthLayers;i>=1;i--){ float off=i*extrudeOffsetPx; canvas.drawText(line, centerX+off, baselineY+off, extrudePaint); }
        paint.setColor(primaryColor); canvas.drawText(line, centerX, baselineY, paint);
    }

    // Remove unused arc drawing helpers (keep computeMaxLineWidth & utility)
    private void drawCurvedSharedRadius(Canvas canvas, String line, int index, float topOffset, float r, float thetaRad) { /* no-op now */ }
    private void drawCurvedLine3D(Canvas canvas, String line){ /* legacy unused */ }

    private float dpF(float v){ return getResources().getDisplayMetrics().density * v; }
    private int dp(int v){ return Math.round(getResources().getDisplayMetrics().density * v); }
    private float sp(float v){ return v * getResources().getDisplayMetrics().scaledDensity; }
    private float computeMaxLineWidth(String[] lines){
        float max = 0f;
        if(lines==null) return 0f;
        for(String s: lines){
            if(s==null) continue;
            float w = paint.measureText(s);
            if(w>max) max = w;
        }
        return max;
    }
}

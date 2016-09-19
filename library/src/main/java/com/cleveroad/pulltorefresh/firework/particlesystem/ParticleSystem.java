package com.cleveroad.pulltorefresh.firework.particlesystem;

import android.animation.Animator;
import android.animation.Animator.AnimatorListener;
import android.animation.ValueAnimator;
import android.animation.ValueAnimator.AnimatorUpdateListener;
import android.app.Activity;
import android.graphics.Bitmap;
import android.graphics.Canvas;
import android.graphics.drawable.AnimationDrawable;
import android.graphics.drawable.BitmapDrawable;
import android.graphics.drawable.Drawable;
import android.support.annotation.ColorInt;
import android.support.v4.content.ContextCompat;
import android.util.DisplayMetrics;
import android.view.Gravity;
import android.view.View;
import android.view.ViewGroup;
import android.view.animation.Interpolator;
import android.view.animation.LinearInterpolator;

import com.cleveroad.pulltorefresh.firework.particlesystem.initializers.AccelerationInitializer;
import com.cleveroad.pulltorefresh.firework.particlesystem.initializers.ParticleInitializer;
import com.cleveroad.pulltorefresh.firework.particlesystem.initializers.RotationInitializer;
import com.cleveroad.pulltorefresh.firework.particlesystem.initializers.RotationSpeedInitializer;
import com.cleveroad.pulltorefresh.firework.particlesystem.initializers.ScaleInitializer;
import com.cleveroad.pulltorefresh.firework.particlesystem.initializers.SpeedByComponentsInitializer;
import com.cleveroad.pulltorefresh.firework.particlesystem.initializers.SpeedModuleAndRangeInitializer;
import com.cleveroad.pulltorefresh.firework.particlesystem.modifiers.AlphaModifier;
import com.cleveroad.pulltorefresh.firework.particlesystem.modifiers.ParticleModifier;

import java.lang.ref.WeakReference;
import java.util.ArrayList;
import java.util.LinkedList;
import java.util.List;
import java.util.Random;
import java.util.Timer;
import java.util.TimerTask;

public class ParticleSystem {

    private static final long TIMER_TASK_INTERVAL = 50;
    private final List<Particle> mActiveParticles = new LinkedList<>();
    private final ParticleTimerTask mTimerTask = new ParticleTimerTask(this);
    private ViewGroup mParentView;
    private int mMaxParticles;
    private Random mRandom;
    private ArrayList<Particle> mParticles;
    private long mTimeToLive;
    private long mCurrentTime = 0;
    private float mParticlesPerMillisecond;
    private int mActivatedParticles;
    private long mEmittingTime;
    private List<ParticleModifier> mModifiers;
    private List<ParticleInitializer> mParticleInitializers;
    private ValueAnimator mAnimator;
    private Timer mTimer;
    private float mDpToPxScale;
    private int[] mParentLocation;

    private int mEmitterXMin;
    private int mEmitterXMax;
    private int mEmitterYMin;
    private int mEmitterYMax;

    private ParticleSystem(ViewGroup parentView, int maxParticles, long timeToLive) {
        mRandom = new Random();
        mParentLocation = new int[2];

        setParentViewGroup(parentView);

        mModifiers = new ArrayList<>();
        mParticleInitializers = new ArrayList<>();

        mMaxParticles = maxParticles;
        // Create the particles

        mParticles = new ArrayList<>();
        mTimeToLive = timeToLive;

        DisplayMetrics displayMetrics = parentView.getContext().getResources().getDisplayMetrics();
        mDpToPxScale = (displayMetrics.xdpi / DisplayMetrics.DENSITY_DEFAULT);
    }

    /**
     * Creates a particle system with the given parameters
     *
     * @param parentView   The parent view group
     * @param drawable     The drawable to use as a particle
     * @param maxParticles The maximum number of particles
     * @param timeToLive   The time to live for the particles
     */
    public ParticleSystem(ViewGroup parentView, int maxParticles, Drawable drawable, long timeToLive) {
        this(parentView, maxParticles, timeToLive);

        if (drawable instanceof BitmapDrawable) {
            Bitmap bitmap = ((BitmapDrawable) drawable).getBitmap();
            for (int i = 0; i < mMaxParticles; i++) {
                mParticles.add(new Particle(bitmap));
            }
        } else //noinspection StatementWithEmptyBody
            if (drawable instanceof AnimationDrawable) {
                AnimationDrawable animation = (AnimationDrawable) drawable;
                for (int i = 0; i < mMaxParticles; i++) {
                    mParticles.add(new AnimatedParticle(animation));
                }
            } else {
                // Not supported, no particles are being created
            }
    }

    /**
     * Creates a particle system with the given parameters
     *
     * @param a             The parent activity
     * @param maxParticles  The maximum number of particles
     * @param drawableResId The drawable resource to use as particle (supports Bitmaps and Animations)
     * @param timeToLive    The time to live for the particles
     */
    public ParticleSystem(Activity a, int maxParticles, int drawableResId, long timeToLive) {
        this(a, maxParticles, ContextCompat.getDrawable(a, drawableResId), timeToLive, android.R.id.content);
    }

    /**
     * Creates a particle system with the given parameters
     *
     * @param a             The parent activity
     * @param maxParticles  The maximum number of particles
     * @param drawableResId The drawable resource to use as particle (supports Bitmaps and Animations)
     * @param timeToLive    The time to live for the particles
     * @param parentViewId  The view Id for the parent of the particle system
     */
    public ParticleSystem(Activity a, int maxParticles, int drawableResId, long timeToLive, int parentViewId) {
        this(a, maxParticles, ContextCompat.getDrawable(a, drawableResId), timeToLive, parentViewId);
    }

    /**
     * Creates a particle system with the given parameters
     *
     * @param a             The parent activity
     * @param maxParticles  The maximum number of particles
     * @param drawableResId The drawable resource to use as particle (supports Bitmaps and Animations)
     * @param timeToLive    The time to live for the particles
     * @param parentView    The parent view of the particle system
     */
    public ParticleSystem(Activity a, int maxParticles, int drawableResId, long timeToLive, ViewGroup parentView) {
        this(parentView, maxParticles, ContextCompat.getDrawable(a, drawableResId), timeToLive);
    }

    /**
     * Utility constructor that receives a Drawable
     *
     * @param a            The parent activity
     * @param maxParticles The maximum number of particles
     * @param drawable     The drawable to use as particle (supports Bitmaps and Animations)
     * @param timeToLive   The time to live for the particles
     */
    public ParticleSystem(Activity a, int maxParticles, Drawable drawable, long timeToLive) {
        this(a, maxParticles, drawable, timeToLive, android.R.id.content);
    }

    /**
     * Utility constructor that receives a Drawable
     *
     * @param a            The parent activity
     * @param maxParticles The maximum number of particles
     * @param drawable     The drawable to use as particle (supports Bitmaps and Animations)
     * @param timeToLive   The time to live for the particles
     * @param parentViewId The view Id for the parent of the particle system
     */
    public ParticleSystem(Activity a, int maxParticles, Drawable drawable, long timeToLive, int parentViewId) {
        this((ViewGroup) a.findViewById(parentViewId), maxParticles, drawable, timeToLive);
    }

    /**
     * Utility constructor that receives a Bitmap
     *
     * @param a            The parent activity
     * @param maxParticles The maximum number of particles
     * @param bitmap       The bitmap to use as particle
     * @param timeToLive   The time to live for the particles
     */
    public ParticleSystem(Activity a, int maxParticles, Bitmap bitmap, long timeToLive) {
        this(a, maxParticles, bitmap, timeToLive, android.R.id.content);
    }

    /**
     * Utility constructor that receives a Bitmap
     *
     * @param a            The parent activity
     * @param maxParticles The maximum number of particles
     * @param bitmap       The bitmap to use as particle
     * @param timeToLive   The time to live for the particles
     * @param parentViewId The view Id for the parent of the particle system
     */
    public ParticleSystem(Activity a, int maxParticles, Bitmap bitmap, long timeToLive, int parentViewId) {
        this((ViewGroup) a.findViewById(parentViewId), maxParticles, timeToLive);
        for (int i = 0; i < mMaxParticles; i++) {
            mParticles.add(new Particle(bitmap));
        }
    }

    /**
     * Utility constructor that receives an AnimationDrawble
     *
     * @param a            The parent activity
     * @param maxParticles The maximum number of particles
     * @param animation    The animation to use as particle
     * @param timeToLive   The time to live for the particles
     */
    public ParticleSystem(Activity a, int maxParticles, AnimationDrawable animation, long timeToLive) {
        this(a, maxParticles, animation, timeToLive, android.R.id.content);
    }

    /**
     * Utility constructor that receives an AnimationDrawble
     *
     * @param a            The parent activity
     * @param maxParticles The maximum number of particles
     * @param animation    The animation to use as particle
     * @param timeToLive   The time to live for the particles
     * @param parentViewId The view Id for the parent of the particle system
     */
    public ParticleSystem(Activity a, int maxParticles, AnimationDrawable animation, long timeToLive, int parentViewId) {
        this((ViewGroup) a.findViewById(parentViewId), maxParticles, timeToLive);
        // Create the particles
        for (int i = 0; i < mMaxParticles; i++) {
            mParticles.add(new AnimatedParticle(animation));
        }
    }

    public float dpToPx(float dp) {
        return dp * mDpToPxScale;
    }

    /**
     * Adds a modifier to the Particle system, it will be executed on each update.
     *
     * @param modifier modifier to be added to the ParticleSystem
     */
    public ParticleSystem addModifier(ParticleModifier modifier) {
        mModifiers.add(modifier);
        return this;
    }

    public ParticleSystem setSpeedRange(float speedMin, float speedMax) {
        mParticleInitializers.add(new SpeedModuleAndRangeInitializer(dpToPx(speedMin), dpToPx(speedMax), 0, 360));
        return this;
    }

    /**
     * Initializes the speed range and angle range of emitted particles. Angles are in degrees
     * and non negative:
     * 0 meaning to the right, 90 to the bottom,... in clockwise orientation. Speed is non
     * negative and is described in pixels per millisecond.
     *
     * @param speedMin The minimum speed to emit particles.
     * @param speedMax The maximum speed to emit particles.
     * @param minAngle The minimum angle to emit particles in degrees.
     * @param maxAngle The maximum angle to emit particles in degrees.
     * @return This.
     */
    public ParticleSystem setSpeedModuleAndAngleRange(float speedMin, float speedMax, int minAngle, int maxAngle) {
        // else emitting from top (270°) to bottom (90°) range would not be possible if someone
        // entered minAngle = 270 and maxAngle=90 since the module would swap the values
        while (maxAngle < minAngle) {
            maxAngle += 360;
        }
        mParticleInitializers.add(new SpeedModuleAndRangeInitializer(dpToPx(speedMin), dpToPx(speedMax), minAngle, maxAngle));
        return this;
    }

    /**
     * Initializes the speed components ranges that particles will be emitted. Speeds are
     * measured in density pixels per millisecond.
     *
     * @param speedMinX The minimum speed in x direction.
     * @param speedMaxX The maximum speed in x direction.
     * @param speedMinY The minimum speed in y direction.
     * @param speedMaxY The maximum speed in y direction.
     * @return This.
     */
    public ParticleSystem setSpeedByComponentsRange(float speedMinX, float speedMaxX, float speedMinY, float speedMaxY) {
        mParticleInitializers.add(new SpeedByComponentsInitializer(dpToPx(speedMinX), dpToPx(speedMaxX),
                dpToPx(speedMinY), dpToPx(speedMaxY)));
        return this;
    }

    /**
     * Initializes the initial rotation range of emitted particles. The rotation angle is
     * measured in degrees with 0° being no rotation at all and 90° tilting the image to the right.
     *
     * @param minAngle The minimum tilt angle.
     * @param maxAngle The maximum tilt angle.
     * @return This.
     */
    public ParticleSystem setInitialRotationRange(int minAngle, int maxAngle) {
        mParticleInitializers.add(new RotationInitializer(minAngle, maxAngle));
        return this;
    }

    /**
     * Initializes the scale range of emitted particles. Will scale the images around their
     * center multiplied with the given scaling factor.
     *
     * @param minScale The minimum scaling factor
     * @param maxScale The maximum scaling factor.
     * @return This.
     */
    public ParticleSystem setScaleRange(float minScale, float maxScale) {
        mParticleInitializers.add(new ScaleInitializer(minScale, maxScale));
        return this;
    }

    /**
     * Initializes the rotation speed of emitted particles. Rotation speed is measured in degrees
     * per second.
     *
     * @param rotationSpeed The rotation speed.
     * @return This.
     */
    public ParticleSystem setRotationSpeed(float rotationSpeed) {
        mParticleInitializers.add(new RotationSpeedInitializer(rotationSpeed, rotationSpeed));
        return this;
    }

    /**
     * Initializes the rotation speed range for emitted particles. The rotation speed is measured
     * in degrees per second and can be positive or negative.
     *
     * @param minRotationSpeed The minimum rotation speed.
     * @param maxRotationSpeed The maximum rotation speed.
     * @return This.
     */
    public ParticleSystem setRotationSpeedRange(float minRotationSpeed, float maxRotationSpeed) {
        mParticleInitializers.add(new RotationSpeedInitializer(minRotationSpeed, maxRotationSpeed));
        return this;
    }

    /**
     * Initializes the acceleration range and angle range of emitted particles. The acceleration
     * components in x and y direction are controlled by the acceleration angle. The acceleration
     * is measured in density pixels per square millisecond. The angle is measured in degrees
     * with 0° pointing to the right and going clockwise.
     *
     * @param minAcceleration
     * @param maxAcceleration
     * @param minAngle
     * @param maxAngle
     * @return
     */
    public ParticleSystem setAccelerationModuleAndAndAngleRange(float minAcceleration, float maxAcceleration, int minAngle, int maxAngle) {
        mParticleInitializers.add(new AccelerationInitializer(dpToPx(minAcceleration), dpToPx(maxAcceleration),
                minAngle, maxAngle));
        return this;
    }

    /**
     * Initializes the acceleration for emitted particles with the given angle. Acceleration is
     * measured in pixels per square millisecond. The angle is measured in degrees with 0°
     * meaning to the right and orientation being clockwise. The angle controls the acceleration
     * direction.
     *
     * @param acceleration The acceleration.
     * @param angle        The acceleration direction.
     * @return This.
     */
    public ParticleSystem setAcceleration(float acceleration, int angle) {
        mParticleInitializers.add(new AccelerationInitializer(acceleration, acceleration, angle, angle));
        return this;
    }

    /**
     * Initializes the parent view group. This needs to be done before any other configuration or
     * emitting is done. Drawing will be done to a child that is added to this view. So this view
     * needs to allow displaying an arbitrary sized view on top of its other content.
     *
     * @param viewGroup The view group to use.
     * @return This.
     */
    public ParticleSystem setParentViewGroup(ViewGroup viewGroup) {
        mParentView = viewGroup;
        if (mParentView != null) {
            mParentView.getLocationInWindow(mParentLocation);
        }
        return this;
    }

    public ParticleSystem setStartTime(int time) {
        mCurrentTime = time;
        return this;
    }

    /**
     * Configures a fade out for the particles when they disappear
     *
     * @param milisecondsBeforeEnd fade out duration in milliseconds
     * @param interpolator         the interpolator for the fade out (default is linear)
     */
    public ParticleSystem setFadeOut(long milisecondsBeforeEnd, Interpolator interpolator) {
        mModifiers.add(new AlphaModifier(255, 0, mTimeToLive - milisecondsBeforeEnd, mTimeToLive, interpolator));
        return this;
    }

    /**
     * Configures a fade out for the particles when they disappear
     *
     * @param duration fade out duration in milliseconds
     */
    public ParticleSystem setFadeOut(long duration) {
        return setFadeOut(duration, new LinearInterpolator());
    }

    /**
     * Starts emiting particles from a specific view. If at some point the number goes over the amount of particles availabe on create
     * no new particles will be created
     *
     * @param emiter             View from which center the particles will be emited
     * @param gravity            Which position among the view the emission takes place
     * @param particlesPerSecond Number of particles per second that will be emited (evenly distributed)
     * @param emitingTime        time the emiter will be emiting particles
     */
    public void emitWithGravity(View emiter, int gravity, int particlesPerSecond, int emitingTime) {
        // Setup emiter
        configureEmiter(emiter, gravity);
        startEmiting(particlesPerSecond, emitingTime);
    }

    /**
     * Starts emiting particles from a specific view. If at some point the number goes over the amount of particles availabe on create
     * no new particles will be created
     *
     * @param emiter             View from which center the particles will be emited
     * @param particlesPerSecond Number of particles per second that will be emited (evenly distributed)
     * @param emitingTime        time the emiter will be emiting particles
     */
    public void emit(View emiter, int particlesPerSecond, int emitingTime) {
        emitWithGravity(emiter, Gravity.CENTER, particlesPerSecond, emitingTime);
    }

    public void emit(int left, int top, int width, int height, int particlesPerSecond, int emitingTime) {
        configureEmiter(left, top, width, height, Gravity.CENTER);
        startEmiting(particlesPerSecond, emitingTime);
    }

    /**
     * Starts emiting particles from a specific view. If at some point the number goes over the amount of particles availabe on create
     * no new particles will be created
     *
     * @param emiter             View from which center the particles will be emited
     * @param particlesPerSecond Number of particles per second that will be emited (evenly distributed)
     */
    public void emit(View emiter, int particlesPerSecond) {
        // Setup emiter
        emitWithGravity(emiter, Gravity.CENTER, particlesPerSecond);
    }

    /**
     * Starts emiting particles from a specific view. If at some point the number goes over the amount of particles availabe on create
     * no new particles will be created
     *
     * @param emiter             View from which center the particles will be emited
     * @param gravity            Which position among the view the emission takes place
     * @param particlesPerSecond Number of particles per second that will be emited (evenly distributed)
     */
    public void emitWithGravity(View emiter, int gravity, int particlesPerSecond) {
        // Setup emiter
        configureEmiter(emiter, gravity);
        startEmiting(particlesPerSecond);
    }

    private void startEmiting(int particlesPerSecond) {
        mActivatedParticles = 0;
        mParticlesPerMillisecond = particlesPerSecond / 1000f;
        mEmittingTime = -1; // Meaning infinite
        updateParticlesBeforeStartTime(particlesPerSecond);
        mTimer = new Timer();
        mTimer.schedule(mTimerTask, 0, TIMER_TASK_INTERVAL);
    }

    public void emit(int emitterX, int emitterY, int particlesPerSecond, int emitingTime) {
        configureEmiter(emitterX, emitterY);
        startEmiting(particlesPerSecond, emitingTime);
    }

    private void configureEmiter(int emitterX, int emitterY) {
        // We configure the emiter based on the window location to fix the offset of action bar if present
        mEmitterXMin = emitterX - mParentLocation[0];
        mEmitterXMax = mEmitterXMin;
        mEmitterYMin = emitterY - mParentLocation[1];
        mEmitterYMax = mEmitterYMin;
    }

    private void startEmiting(int particlesPerSecond, int emitingTime) {
        mActivatedParticles = 0;
        mParticlesPerMillisecond = particlesPerSecond / 1000f;
        updateParticlesBeforeStartTime(particlesPerSecond);
        mEmittingTime = emitingTime;
        startAnimator(new LinearInterpolator(), emitingTime + mTimeToLive);
    }

    public void emit(int emitterX, int emitterY, int particlesPerSecond) {
        configureEmiter(emitterX, emitterY);
        startEmiting(particlesPerSecond);
    }

    public void updateEmitPoint(int emitterX, int emitterY) {
        configureEmiter(emitterX, emitterY);
    }

    public void updateEmitPoint(View emiter, int gravity) {
        configureEmiter(emiter, gravity);
    }

    /**
     * Launches particles in one Shot
     *
     * @param emiter       View from which center the particles will be emited
     * @param numParticles number of particles launched on the one shot
     */
    public void oneShot(View emiter, int numParticles) {
        oneShot(emiter, numParticles, new LinearInterpolator());
    }

    /**
     * Launches particles in one Shot using a special Interpolator
     *
     * @param emiter       View from which center the particles will be emited
     * @param numParticles number of particles launched on the one shot
     * @param interpolator the interpolator for the time
     */
    public void oneShot(View emiter, int numParticles, Interpolator interpolator) {
        configureEmiter(emiter, Gravity.CENTER);
        mActivatedParticles = 0;
        mEmittingTime = mTimeToLive;
        // We create particles based in the parameters
        for (int i = 0; i < numParticles && i < mMaxParticles; i++) {
            activateParticle(0);
        }
        // We start a property animator that will call us to do the update
        // Animate from 0 to timeToLiveMax
        startAnimator(interpolator, mTimeToLive);
    }

    public void setTintColor(@ColorInt int color) {
        for (Particle p : mParticles) {
            p.setTintColor(color);
        }
    }

    private void startAnimator(Interpolator interpolator, long animnationTime) {
        mAnimator = ValueAnimator.ofInt(0, (int) animnationTime);
        mAnimator.setDuration(animnationTime);
        mAnimator.addUpdateListener(new AnimatorUpdateListener() {
            @Override
            public void onAnimationUpdate(ValueAnimator animation) {
                int miliseconds = (Integer) animation.getAnimatedValue();
                onUpdate(miliseconds);
            }
        });
        mAnimator.addListener(new AnimatorListener() {
            @Override
            public void onAnimationStart(Animator animation) {
            }

            @Override
            public void onAnimationRepeat(Animator animation) {
            }

            @Override
            public void onAnimationEnd(Animator animation) {
                cleanupAnimation();
            }

            @Override
            public void onAnimationCancel(Animator animation) {
                cleanupAnimation();
            }
        });
        mAnimator.setInterpolator(interpolator);
        mAnimator.start();
    }

    private void configureEmiter(View emiter, int gravity) {
        // It works with an emision range
        int[] location = new int[2];
        emiter.getLocationInWindow(location);
        configureEmiter(location[0], location[1], emiter.getWidth(), emiter.getHeight(), gravity);
    }

    private void configureEmiter(int x, int y, int width, int height, int gravity) {
        // It works with an emision range

        // Check horizontal gravity and set range
        if (hasGravity(gravity, Gravity.LEFT)) {
            mEmitterXMin = x - mParentLocation[0];
            mEmitterXMax = mEmitterXMin;
        } else if (hasGravity(gravity, Gravity.RIGHT)) {
            mEmitterXMin = x + width - mParentLocation[0];
            mEmitterXMax = mEmitterXMin;
        } else if (hasGravity(gravity, Gravity.CENTER_HORIZONTAL)) {
            mEmitterXMin = x + width / 2 - mParentLocation[0];
            mEmitterXMax = mEmitterXMin;
        } else {
            // All the range
            mEmitterXMin = x - mParentLocation[0];
            mEmitterXMax = x + width - mParentLocation[0];
        }

        // Now, vertical gravity and range
        if (hasGravity(gravity, Gravity.TOP)) {
            mEmitterYMin = y - mParentLocation[1];
            mEmitterYMax = mEmitterYMin;
        } else if (hasGravity(gravity, Gravity.BOTTOM)) {
            mEmitterYMin = y + height - mParentLocation[1];
            mEmitterYMax = mEmitterYMin;
        } else if (hasGravity(gravity, Gravity.CENTER_VERTICAL)) {
            mEmitterYMin = y + height / 2 - mParentLocation[1];
            mEmitterYMax = mEmitterYMin;
        } else {
            // All the range
            mEmitterYMin = y - mParentLocation[1];
            mEmitterYMax = y + height - mParentLocation[1];
        }
    }

    private boolean hasGravity(int gravity, int gravityToCheck) {
        return (gravity & gravityToCheck) == gravityToCheck;
    }

    private void activateParticle(long delay) {
        Particle particle = mParticles.remove(0);
        particle.init();
        // Initialization goes before configuration, scale is required before can be configured properly
        for (int i = 0; i < mParticleInitializers.size(); i++) {
            mParticleInitializers.get(i).initParticle(particle, mRandom);
        }
        int particleX = getFromRange(mEmitterXMin, mEmitterXMax);
        int particleY = getFromRange(mEmitterYMin, mEmitterYMax);
        particle.configure(mTimeToLive, particleX, particleY);
        particle.activate(delay, mModifiers);
        mActiveParticles.add(particle);
        mActivatedParticles++;
    }

    private int getFromRange(int minValue, int maxValue) {
        if (minValue == maxValue) {
            return minValue;
        }
        return mRandom.nextInt(maxValue - minValue) + minValue;
    }

    private void onUpdate(long miliseconds) {
        while (((mEmittingTime > 0 && miliseconds < mEmittingTime) || mEmittingTime == -1) && // This point should emit
                !mParticles.isEmpty() && // We have particles in the pool
                mActivatedParticles < mParticlesPerMillisecond * miliseconds) { // and we are under the number of particles that should be launched
            // Activate a new particle
            activateParticle(miliseconds);
        }
        synchronized (mActiveParticles) {
            for (int i = 0; i < mActiveParticles.size(); i++) {
                boolean active = mActiveParticles.get(i).update(miliseconds);
                if (!active) {
                    Particle p = mActiveParticles.remove(i);
                    i--; // Needed to keep the index at the right position
                    mParticles.add(p);
                }
            }
        }
    }

    private void cleanupAnimation() {
        mParentView.postInvalidate();
        mParticles.addAll(mActiveParticles);
    }

    /**
     * Stops emitting new particles, but will draw the existing ones until their timeToLive expire
     * For an cancellation and stop drawing of the particles, use cancel instead.
     */
    public void stopEmitting() {
        // The time to be emiting is the current time (as if it was a time-limited emiter
        mEmittingTime = mCurrentTime;
    }

    public synchronized List<Particle> getActiveParticles() {
        return mActiveParticles;
    }

    public boolean isRunning() {
        return mAnimator != null && mAnimator.isRunning();
    }

    public void draw(Canvas canvas) {
        synchronized (mActiveParticles) {
            for (Particle particle : mActiveParticles) {
                particle.draw(canvas);
            }
        }
    }

    /**
     * Cancels the particle system and all the animations.
     * To stop emitting but animate until the end, use stopEmitting instead.
     */
    public void cancel() {
        if (mAnimator != null && mAnimator.isRunning()) {
            mAnimator.cancel();
        }
        if (mTimer != null) {
            mTimer.cancel();
            mTimer.purge();
            cleanupAnimation();
        }
    }

    private void updateParticlesBeforeStartTime(int particlesPerSecond) {
        if (particlesPerSecond == 0) {
            return;
        }
        long currentTimeInMs = mCurrentTime / 1000;
        long framesCount = currentTimeInMs / particlesPerSecond;
        if (framesCount == 0) {
            return;
        }
        long frameTimeInMs = mCurrentTime / framesCount;
        for (int i = 1; i <= framesCount; i++) {
            onUpdate(frameTimeInMs * i + 1);
        }
    }

    private static class ParticleTimerTask extends TimerTask {

        private final WeakReference<ParticleSystem> mPs;

        ParticleTimerTask(ParticleSystem ps) {
            mPs = new WeakReference<>(ps);
        }

        @Override
        public void run() {
            if (mPs.get() != null) {
                ParticleSystem ps = mPs.get();
                ps.onUpdate(ps.mCurrentTime);
                ps.mCurrentTime += TIMER_TASK_INTERVAL;
            }
        }
    }
}
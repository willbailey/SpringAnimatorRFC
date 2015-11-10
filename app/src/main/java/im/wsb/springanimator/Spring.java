package im.wsb.springanimator;

import android.util.Log;

import java.util.concurrent.CopyOnWriteArraySet;

public class Spring {

  private static final double MAX_DELTA_TIME_SEC = 0.064;
  private static final double SOLVER_TIMESTEP_SEC = 0.001;
  private SpringConfig mSpringConfig;
  private boolean mOvershootClampingEnabled;

  private static class PhysicsState {
    double position;
    double velocity;
  }

  private final PhysicsState mCurrentState = new PhysicsState();
  private final PhysicsState mPreviousState = new PhysicsState();
  private final PhysicsState mTempState = new PhysicsState();
  private double mStartValue;
  private double mEndValue;
  private boolean mWasAtRest = true;
  private double mRestSpeedThreshold = 0.001;
  private double mDisplacementFromRestThreshold = 0.001;
  private CopyOnWriteArraySet<SpringListener> mListeners = new CopyOnWriteArraySet<>();
  private double mTimeAccumulator = 0;

  public Spring setSpringConfig(SpringConfig springConfig) {
    if (springConfig == null) {
      throw new IllegalArgumentException("springConfig is required");
    }
    mSpringConfig = springConfig;
    return this;
  }

  public SpringConfig getSpringConfig() {
    return mSpringConfig;
  }

  public Spring setCurrentValue(double currentValue) {
    return setCurrentValue(currentValue, true);
  }

  public Spring setCurrentValue(double currentValue, boolean setAtRest) {
    mStartValue = currentValue;
    mCurrentState.position = currentValue;
    for (SpringListener listener : mListeners) {
      listener.onSpringUpdate(this);
    }
    if (setAtRest) {
      setAtRest();
    }
    return this;
  }

  public double getCurrentValue() {
    return mCurrentState.position;
  }

  public double getStartValue() {
    return mStartValue;
  }

  public double getCurrentDisplacementDistance() {
    return getDisplacementDistanceForState(mCurrentState);
  }

  private double getDisplacementDistanceForState(PhysicsState state) {
    return Math.abs(mEndValue - state.position);
  }

  public Spring setEndValue(double endValue) {
    if (mEndValue == endValue && isAtRest()) {
      return this;
    }
    mStartValue = getCurrentValue();
    mEndValue = endValue;
    return this;
  }

  public double getEndValue() {
    return mEndValue;
  }

  public Spring setVelocity(double velocity) {
    if (velocity == mCurrentState.velocity) {
      return this;
    }
    mCurrentState.velocity = velocity;
    return this;
  }

  public double getVelocity() {
    return mCurrentState.velocity;
  }

  public Spring setRestSpeedThreshold(double restSpeedThreshold) {
    mRestSpeedThreshold = restSpeedThreshold;
    return this;
  }

  public double getRestSpeedThreshold() {
    return mRestSpeedThreshold;
  }

  public Spring setRestDisplacementThreshold(double displacementFromRestThreshold) {
    mDisplacementFromRestThreshold = displacementFromRestThreshold;
    return this;
  }

  public double getRestDisplacementThreshold() {
    return mDisplacementFromRestThreshold;
  }

  public Spring setOvershootClampingEnabled(boolean overshootClampingEnabled) {
    mOvershootClampingEnabled = overshootClampingEnabled;
    return this;
  }

  public boolean isOvershootClampingEnabled() {
    return mOvershootClampingEnabled;
  }

  public boolean isOvershooting() {
    return mSpringConfig.tension > 0 &&
        ((mStartValue < mEndValue && getCurrentValue() > mEndValue) ||
            (mStartValue > mEndValue && getCurrentValue() < mEndValue));
  }

  void advance(double realDeltaTime) {
    boolean isAtRest = isAtRest();

    if (isAtRest && mWasAtRest) {
      return;
    }

    // clamp the amount of realTime to simulate to avoid stuttering in the UI. We should be able
    // to catch up in a subsequent advance if necessary.
    double adjustedDeltaTime = realDeltaTime;
    if (realDeltaTime > MAX_DELTA_TIME_SEC) {
      adjustedDeltaTime = MAX_DELTA_TIME_SEC;
    }

    mTimeAccumulator += adjustedDeltaTime;

    double tension = mSpringConfig.tension;
    double friction = mSpringConfig.friction;

    double position = mCurrentState.position;
    double velocity = mCurrentState.velocity;
    double tempPosition = mTempState.position;
    double tempVelocity = mTempState.velocity;

    double aVelocity, aAcceleration;
    double bVelocity, bAcceleration;
    double cVelocity, cAcceleration;
    double dVelocity, dAcceleration;

    double dxdt, dvdt;

    // iterate over the true time
    while (mTimeAccumulator >= SOLVER_TIMESTEP_SEC) {
      /* begin debug
      iterations++;
      end debug */
      mTimeAccumulator -= SOLVER_TIMESTEP_SEC;

      if (mTimeAccumulator < SOLVER_TIMESTEP_SEC) {
        // This will be the last iteration. Remember the previous state in case we need to
        // interpolate
        mPreviousState.position = position;
        mPreviousState.velocity = velocity;
      }

      // Perform an RK4 integration to provide better detection of the acceleration curve via
      // sampling of Euler integrations at 4 intervals feeding each derivative into the calculation
      // of the next and taking a weighted sum of the 4 derivatives as the final output.

      // This math was inlined since it made for big performance improvements when advancing several
      // springs in one pass of the BaseSpringSystem.

      // The initial derivative is based on the current velocity and the calculated acceleration
      aVelocity = velocity;
      aAcceleration = (tension * (mEndValue - tempPosition)) - friction * velocity;

      // Calculate the next derivatives starting with the last derivative and integrating over the
      // timestep
      tempPosition = position + aVelocity * SOLVER_TIMESTEP_SEC * 0.5;
      tempVelocity = velocity + aAcceleration * SOLVER_TIMESTEP_SEC * 0.5;
      bVelocity = tempVelocity;
      bAcceleration = (tension * (mEndValue - tempPosition)) - friction * tempVelocity;

      tempPosition = position + bVelocity * SOLVER_TIMESTEP_SEC * 0.5;
      tempVelocity = velocity + bAcceleration * SOLVER_TIMESTEP_SEC * 0.5;
      cVelocity = tempVelocity;
      cAcceleration = (tension * (mEndValue - tempPosition)) - friction * tempVelocity;

      tempPosition = position + cVelocity * SOLVER_TIMESTEP_SEC;
      tempVelocity = velocity + cAcceleration * SOLVER_TIMESTEP_SEC;
      dVelocity = tempVelocity;
      dAcceleration = (tension * (mEndValue - tempPosition)) - friction * tempVelocity;

      // Take the weighted sum of the 4 derivatives as the final output.
      dxdt = 1.0/6.0 * (aVelocity + 2.0 * (bVelocity + cVelocity) + dVelocity);
      dvdt = 1.0/6.0 * (aAcceleration + 2.0 * (bAcceleration + cAcceleration) + dAcceleration);

      position += dxdt * SOLVER_TIMESTEP_SEC;
      velocity += dvdt * SOLVER_TIMESTEP_SEC;
    }

    mTempState.position = tempPosition;
    mTempState.velocity = tempVelocity;

    mCurrentState.position = position;
    mCurrentState.velocity = velocity;

    if (mTimeAccumulator > 0) {
      interpolate(mTimeAccumulator / SOLVER_TIMESTEP_SEC);
    }

    // End the spring immediately if it is overshooting and overshoot clamping is enabled.
    // Also make sure that if the spring was considered within a resting threshold that it's now
    // snapped to its end value.
    if (isAtRest() || (mOvershootClampingEnabled && isOvershooting())) {
      // Don't call setCurrentValue because that forces a call to onSpringUpdate
      if (tension > 0) {
        mStartValue = mEndValue;
        mCurrentState.position = mEndValue;
      } else {
        mEndValue = mCurrentState.position;
        mStartValue = mEndValue;
      }
      setVelocity(0);
      isAtRest = true;
    }

    boolean notifyActivate = false;
    if (mWasAtRest) {
      mWasAtRest = false;
      notifyActivate = true;
    }
    boolean notifyAtRest = false;
    if (isAtRest) {
      mWasAtRest = true;
      notifyAtRest = true;
    }
    for (SpringListener listener : mListeners) {
      // updated
      listener.onSpringUpdate(this);

      // coming to rest
      if (notifyAtRest) {
        listener.onSpringAtRest(this);
      }
    }
  }

  private void interpolate(double alpha) {
    mCurrentState.position = mCurrentState.position * alpha + mPreviousState.position *(1-alpha);
    mCurrentState.velocity = mCurrentState.velocity * alpha + mPreviousState.velocity *(1-alpha);
  }

  public boolean wasAtRest() {
    return mWasAtRest;
  }

  public boolean isAtRest() {
    return Math.abs(mCurrentState.velocity) <= mRestSpeedThreshold &&
        (getDisplacementDistanceForState(mCurrentState) <= mDisplacementFromRestThreshold ||
            mSpringConfig.tension == 0);
  }

  public Spring setAtRest() {
    mEndValue = mCurrentState.position;
    mTempState.position = mCurrentState.position;
    mCurrentState.velocity = 0;
    return this;
  }

  public boolean currentValueIsApproximately(double value) {
    return Math.abs(getCurrentValue() - value) <= getRestDisplacementThreshold();
  }

  /** listeners **/

  public Spring addListener(SpringListener newListener) {
    if (newListener == null) {
      throw new IllegalArgumentException("newListener is required");
    }
    mListeners.add(newListener);
    return this;
  }

  public Spring removeListener(SpringListener listenerToRemove) {
    if (listenerToRemove == null) {
      throw new IllegalArgumentException("listenerToRemove is required");
    }
    mListeners.remove(listenerToRemove);
    return this;
  }

  public Spring removeAllListeners() {
    mListeners.clear();
    return this;
  }

}


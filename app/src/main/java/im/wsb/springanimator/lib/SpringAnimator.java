package im.wsb.springanimator.lib;

import android.animation.TimeAnimator;

public class SpringAnimator extends TimeAnimator implements TimeAnimator.TimeListener {
  private final Spring mSpring;

  public SpringAnimator() {
    mSpring = new Spring().setSpringConfig(SpringConfig.defaultConfig);
    setTimeListener(this);
  }

  /* Spring API wrapper */
  public void setSpringConfig(SpringConfig springConfig) {
    mSpring.setSpringConfig(springConfig);
  }

  public SpringConfig getSpringConfig() {
    return mSpring.getSpringConfig();
  }

  public void setCurrentValue(double value) {
    mSpring.setCurrentValue(value);
  }

  public double getCurrentValue() {
    return mSpring.getCurrentValue();
  }

  public void setEndValue(double value) {
    mSpring.setEndValue(value);
    if (!mSpring.isAtRest()) {
      start();
    }
  }

  public double getEndValue() {
    return mSpring.getEndValue();
  }

  public void setVelocity(double velocity) {
    mSpring.setVelocity(velocity);
    if (!mSpring.isAtRest()) {
      start();
    }
  }

  public double getVelocity() {
    return mSpring.getVelocity();
  }

  public void addSpringListener(SpringListener springListener) {
    mSpring.addListener(springListener);
  }

  public void removeSpringListener(SpringListener springListener) {
    mSpring.removeListener(springListener);
  }

  public void removeAllSpringListeners() {
    mSpring.removeAllListeners();
  }

  /* TimeAnimator.TimeListener */
  @Override
  public void onTimeUpdate(TimeAnimator animation, long totalTime, long deltaTime) {
    if (mSpring.isAtRest() && mSpring.wasAtRest()) {
      cancel();
      return;
    }
    mSpring.advance(deltaTime / 1000f);
  }
}

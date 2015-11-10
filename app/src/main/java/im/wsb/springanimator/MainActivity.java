package im.wsb.springanimator;

import android.animation.ArgbEvaluator;
import android.content.res.Resources;
import android.os.Bundle;
import android.support.v7.app.AppCompatActivity;
import android.view.MotionEvent;
import android.view.View;


public class MainActivity extends AppCompatActivity implements SpringListener, View.OnTouchListener {

  private SpringAnimator mSpringAnimator;
  private View mAnimatedView;
  private ArgbEvaluator mArgbEvaluator;
  private Integer mStartColor;
  private Integer mEndColor;

  @Override
  protected void onCreate(Bundle savedInstanceState) {
    super.onCreate(savedInstanceState);
    setContentView(R.layout.activity_main);

    mSpringAnimator = new SpringAnimator();
    mSpringAnimator.addSpringListener(this);
    mSpringAnimator.setSpringConfig(SpringConfig.fromOrigamiTensionAndFriction(40, 5));

    mAnimatedView = findViewById(R.id.animated_view);
    mAnimatedView.setOnTouchListener(this);

    mArgbEvaluator = new ArgbEvaluator();

    Resources res = getResources();
    mStartColor = res.getColor(R.color.colorAccent);
    mEndColor = res.getColor(R.color.colorPrimary);
  }

  /* View.OnTouchListener */
  @Override
  public boolean onTouch(View v, MotionEvent event) {
    int action = event.getActionMasked();
    switch (action) {
      case MotionEvent.ACTION_DOWN:
        mSpringAnimator.setEndValue(1);
        break;
      case MotionEvent.ACTION_UP:
      case MotionEvent.ACTION_CANCEL:
        mSpringAnimator.setEndValue(0);
        break;
    }
    return true;
  }

  /* SpringListener */
  @Override
  public void onSpringUpdate(Spring spring) {
    // Do some transformations on the view based on the current value.
    float scale = (float) lerp(spring.getCurrentValue(), 0, 1, 1, 3);

    mAnimatedView.setScaleX(scale);
    mAnimatedView.setScaleY(scale);

    float rot = (float) lerp(spring.getCurrentValue(), 0, 1, 0, 360);
    mAnimatedView.setRotation(rot);

    Integer color = (Integer) mArgbEvaluator.evaluate(
        (float) clamp(spring.getCurrentValue(), 0, 1),
        mStartColor,
        mEndColor);
    mAnimatedView.setBackgroundColor(color);
  }

  @Override
  public void onSpringAtRest(Spring spring) {}

  /* Util */
  private static double lerp(double value, double fromLow, double fromHigh, double toLow, double toHigh) {
    double fromRangeSize = fromHigh - fromLow;
    double toRangeSize = toHigh - toLow;
    double valueScale = (value - fromLow) / fromRangeSize;
    return toLow + (valueScale * toRangeSize);
  }

  private static double clamp(double val, double low, double high) {
    return Math.min(Math.max(low, val), high);
  }
}

package im.wsb.springanimator.lib;

public class SpringConfig {
  public double friction;
  public double tension;

  public static SpringConfig defaultConfig = SpringConfig.fromOrigamiTensionAndFriction(40, 7);

  public SpringConfig(double tension, double friction) {
    this.tension = tension;
    this.friction = friction;
  }

  public static SpringConfig fromOrigamiTensionAndFriction(double qcTension, double qcFriction) {
    return new SpringConfig(
        tensionFromOrigamiValue(qcTension),
        frictionFromOrigamiValue(qcFriction));
  }

  public static double tensionFromOrigamiValue(double oValue) {
    return oValue == 0 ? 0 : (oValue - 30.0) * 3.62 + 194.0;
  }

  public static double origamiValueFromTension(double tension) {
    return tension == 0 ? 0 : (tension - 194.0) / 3.62 + 30.0;
  }

  public static double frictionFromOrigamiValue(double oValue) {
    return oValue == 0 ? 0 : (oValue - 8.0) * 3.0 + 25.0;
  }

  public static double origamiValueFromFriction(double friction) {
    return friction == 0 ? 0 : (friction - 25.0) / 3.0 + 8.0;
  }

}

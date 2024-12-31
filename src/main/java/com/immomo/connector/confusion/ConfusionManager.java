package com.immomo.connector.confusion;

import java.util.HashMap;
import java.util.Map;

public class ConfusionManager {

  private static final Map<Integer, IConfusion> MAP = new HashMap<Integer, IConfusion>();
  private static final Map<Integer, IConfusion> MAPV3 = new HashMap<Integer, IConfusion>();

  private static IConfusion DEFAULT = new DefaultConfusion();

  private static IConfusion DEFAULTV3 = new DefaultConfusionV3();

  static {
    MAP.put(1, new Confusion1());
    MAP.put(2, new Confusion2());
    MAP.put(3, new Confusion3());
    MAP.put(4, new Confusion4());
    MAP.put(5, new Confusion5());

    MAPV3.put(1, new Confusion1());
    MAPV3.put(2, new Confusion2());
    MAPV3.put(3, new Confusion3());
    MAPV3.put(4, new Confusion4());
    MAPV3.put(5, new Confusion5());
    MAPV3.put(6, new DefaultConfusionV3());
    MAPV3.put(7, new Confusion7V3());
    MAPV3.put(8, new Confusion8V3());
  }

  public static IConfusion get(int version) {
    IConfusion confusion = MAP.get(version);
    if (confusion == null) {
      confusion = DEFAULT;
    }
    return confusion;
  }


  public static IConfusion getV3(int version) {
    IConfusion confusion = MAPV3.get(version);
    if (confusion == null) {
      confusion = DEFAULTV3;
    }
    return confusion;
  }
}

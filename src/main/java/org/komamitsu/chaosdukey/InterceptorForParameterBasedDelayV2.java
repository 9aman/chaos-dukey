package org.komamitsu.chaosdukey;

import java.util.Map;

public class InterceptorForParameterBasedDelayV2 extends InterceptorForParameterBasedDelayBase {
  public static final String V2_QUERY_DELAY_CLASS_NAME =
      "org.apache.pinot.query.service.server.QueryServer";
  public static final String V2_QUERY_DELAY_METHOD_NAME = "submitInternal";

  public InterceptorForParameterBasedDelayV2(DelayConfig config, boolean debug) {
    super(config, debug);
  }

  @Override
  @SuppressWarnings("unchecked")
  protected Map<String, String> getQueryOptions(Object[] args) {
    System.err.println("Able to intercept calls for the new param based delay injector V1");
    if (args.length < 2) {
      return Map.of();
    }
    return args[1] instanceof Map ? (Map<String, String>) args[1] : Map.of();
  }
}

package org.komamitsu.chaosdukey;

import java.util.Map;

public class InterceptorForParameterBasedMemoryAllocationV2
    extends InterceptorForParameterBasedMemoryAllocation {
  public InterceptorForParameterBasedMemoryAllocationV2(boolean debug) {
    super(debug);
  }

  @Override
  @SuppressWarnings("unchecked")
  protected Map<String, String> getQueryOptions(Object[] args) {
    System.err.println("Able to intercept calls for the memory allocation injector v2");
    if (args.length < 2) {
      return Map.of();
    }
    return args[1] instanceof Map ? (Map<String, String>) args[1] : Map.of();
  }
}

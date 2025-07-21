package org.komamitsu.chaosdukey;

import java.lang.reflect.Method;
import java.util.Map;
import java.util.Optional;
import java.util.concurrent.Callable;
import java.util.concurrent.ThreadLocalRandom;
import net.bytebuddy.implementation.bind.annotation.AllArguments;
import net.bytebuddy.implementation.bind.annotation.Origin;
import net.bytebuddy.implementation.bind.annotation.RuntimeType;
import net.bytebuddy.implementation.bind.annotation.SuperCall;
import org.komamitsu.chaosdukey.utils.QueryOptionsUtils;

public class InterceptorForParameterBasedMemoryAllocation {
  private final boolean debug;
  private static final String ENABLE_MEMORY_ALLOCATION = "enableMemoryAllocation";
  private static final boolean ENABLE_MEMORY_ALLOCATION_DEFAULT = false;
  private static final String MAX_MEMORY_TO_ALLOCATE = "maxMemoryToAllocate";
  private static final int DEFAULT_MAX_MEMORY_TO_ALLOCATE = 1024 * 1024; // 1MB
  private static final String ALLOCATION_PERCENTAGE = "allocationPercentage";
  private static final long DEFAULT_ALLOCATION_PPM = 10000;

  public static final String V1_QUERY_MEMORY_CLASS_NAME =
      "org.apache.pinot.core.query.executor.ServerQueryExecutorV1Impl";
  public static final String V1_QUERY_MEMORY_METHOD_NAME = "execute";

  public InterceptorForParameterBasedMemoryAllocation(boolean debug) {
    this.debug = debug;
  }

  byte[] allocateMemory(int memorySize) {
    try {
      byte[] memoryAllocation = new byte[memorySize];
      System.err.println("Allocated memory: " + memorySize + " bytes");
      return memoryAllocation;
    } catch (Exception e) {
      if (debug) {
        System.err.println("[Chaos-Dukey] Error allocating memory: " + e.getMessage());
      }
      return null;
    }
  }

  @RuntimeType
  public Object intercept(
      @Origin Method origin, @SuperCall Callable<?> callable, @AllArguments Object[] args)
      throws Exception {
    System.err.println(
        "Intercepting: "
            + origin.getDeclaringClass().getName()
            + "."
            + origin.getName()
            + " with params: "
            + java.util.Arrays.toString(origin.getParameterTypes()));
    byte[] allocatedMemory = null;
    try {
      Map<String, String> queryOptions = getQueryOptions(args);
      boolean enableMemoryAllocation =
          Optional.ofNullable(queryOptions.get(ENABLE_MEMORY_ALLOCATION))
              .map(Boolean::parseBoolean)
              .orElse(ENABLE_MEMORY_ALLOCATION_DEFAULT);
      System.err.println("enableMemoryAllocation: " + enableMemoryAllocation);
      if (enableMemoryAllocation) {
        long allocationPPM =
            Optional.ofNullable(queryOptions.get(ALLOCATION_PERCENTAGE))
                .map(percentage -> Long.parseLong(percentage) * 10000)
                .orElse(DEFAULT_ALLOCATION_PPM);
        System.err.println("allocationPPM: " + allocationPPM);
        if (ThreadLocalRandom.current().nextLong(1000000) < allocationPPM) {
          int memoryToAllocate =
              Optional.ofNullable(queryOptions.get(MAX_MEMORY_TO_ALLOCATE))
                  .map(Integer::parseInt)
                  .orElse(DEFAULT_MAX_MEMORY_TO_ALLOCATE);
          System.err.println("memoryToAllocate: " + memoryToAllocate);
          allocatedMemory = allocateMemory(memoryToAllocate);
        }
      }
    } catch (Exception e) {
      if (debug) {
        System.err.println(
            "[Chaos-Dukey] Error while injecting memory allocation for single stage queries: "
                + e.getMessage());
      }
    }

    try {
      return callable.call();
    } finally {
      if (allocatedMemory != null) {
        System.err.println("Releasing allocated memory");
        allocatedMemory = null;
      }
    }
  }

  protected Map<String, String> getQueryOptions(Object[] args) {
    System.err.println("Able to intercept calls for the memory allocation injector v1");
    try {
      if (args.length > 0) {
        Map<String, String> queryOptions =
            QueryOptionsUtils.getQueryOptionsFromServerQueryRequest(args[0]);
        return queryOptions == null ? Map.of() : queryOptions;
      }
    } catch (Exception e) {
      if (debug) {
        System.err.println("[Chaos-Dukey] Error extracting query options: " + e.getMessage());
      }
    }
    return Map.of();
  }
}

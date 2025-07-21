package org.komamitsu.chaosdukey;

import java.lang.reflect.Method;
import java.util.Map;
import java.util.Optional;
import java.util.concurrent.Callable;
import java.util.concurrent.ThreadLocalRandom;
import java.util.concurrent.TimeUnit;
import net.bytebuddy.implementation.bind.annotation.AllArguments;
import net.bytebuddy.implementation.bind.annotation.Origin;
import net.bytebuddy.implementation.bind.annotation.RuntimeType;
import net.bytebuddy.implementation.bind.annotation.SuperCall;
import org.komamitsu.chaosdukey.utils.QueryOptionsUtils;

public class InterceptorForParameterBasedDelayBase {
  private final DelayConfig config;
  private final boolean debug;
  private static final String ENABLE_DELAY = "enableDelay";
  private static final boolean ENABLE_DELAY_DEFAULT = false;
  private static final String MAX_DELAY_TIME_MS = "maxDelayTimeMs";
  private static final int DEFAULT_MAX_DELAY_TIME_MS = 1000;
  private static final String DELAY_PERCENTAGE = "delayPercentage";
  private static final long DEFAULT_DELAY_PPM = 10000;

  public static final String V1_QUERY_DELAY_CLASS_NAME =
      "org.apache.pinot.core.query.executor.ServerQueryExecutorV1Impl";
  public static final String V1_QUERY_DELAY_METHOD_NAME = "execute";

  public InterceptorForParameterBasedDelayBase(DelayConfig config, boolean debug) {
    this.config = config;
    this.debug = true;
  }

  void waitForDelay(int waitDurationMs) throws InterruptedException {
    int durationMillis;
    if (config.waitMode != null && config.waitMode == InterceptorForDelay.DelayWaitMode.FIXED) {
      System.err.println("waitMode: " + InterceptorForDelay.DelayWaitMode.FIXED);
      durationMillis = waitDurationMs;
    } else {
      System.err.println("waitMode asds : " + InterceptorForDelay.DelayWaitMode.RANDOM);
      durationMillis = ThreadLocalRandom.current().nextInt(waitDurationMs) + 1;
    }
    System.err.println("durationMillis : " + durationMillis);
    waitForDuration(durationMillis);
  }

  void waitForDuration(int durationMillis) throws InterruptedException {
    TimeUnit.MILLISECONDS.sleep(durationMillis);
  }

  @RuntimeType
  public Object intercept(
      @Origin Method origin, @SuperCall Callable<?> callable, @AllArguments Object[] args)
      throws Exception {
    System.err.println("Able to intercept calls for the new param based delay injector");
    try {
      Map<String, String> queryOptions = getQueryOptions(args);
      boolean enableDelay =
          Optional.ofNullable(queryOptions.get(ENABLE_DELAY))
              .map(Boolean::parseBoolean)
              .orElse(ENABLE_DELAY_DEFAULT);
      System.err.println("enableDelay: " + enableDelay);
      if (enableDelay) {
        long delayPPM =
            Optional.ofNullable(queryOptions.get(DELAY_PERCENTAGE))
                .map(percentage -> Long.parseLong(percentage) * 10000)
                .orElse(DEFAULT_DELAY_PPM);
        System.err.println("delayPPM: " + delayPPM);
        if (ThreadLocalRandom.current().nextLong(1000000) < delayPPM) {
          int waitDurationMs =
              Optional.ofNullable(queryOptions.get(MAX_DELAY_TIME_MS))
                  .map(Integer::parseInt)
                  .orElse(DEFAULT_MAX_DELAY_TIME_MS);
          System.err.println("waitDurationMs: " + waitDurationMs);
          waitForDelay(waitDurationMs);
        }
      }
    } catch (Exception e) {
      if (debug) {
        System.err.println(
            "[Chaos-Dukey] Error while injecting delay for single stage queries: "
                + e.getMessage());
      }
    }

    return callable.call();
  }

  protected Map<String, String> getQueryOptions(Object[] args) {
    try {
      if (args.length > 0) {
        System.out.println("Extracting query options from the query");
        Map<String, String> queryOptions =
            QueryOptionsUtils.getQueryOptionsFromServerQueryRequest(args[0]);
        System.out.println("available query options are: " + queryOptions);
        return queryOptions;
      }
    } catch (Exception e) {
      if (debug) {
        System.err.println("[Chaos-Dukey] Error extracting query options: " + e.getMessage());
      }
    }
    return Map.of();
  }
}

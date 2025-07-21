package org.komamitsu.chaosdukey;

import java.io.IOException;
import java.lang.instrument.Instrumentation;
import java.util.Properties;
import net.bytebuddy.agent.builder.AgentBuilder;
import net.bytebuddy.implementation.MethodDelegation;
import net.bytebuddy.matcher.ElementMatchers;

public final class Agent {
  static Properties propertiesFromArguments(String arguments) {
    Properties properties = new Properties();
    if (arguments != null) {
      for (String kv : arguments.split(",")) {
        String[] tokens = kv.trim().split("=");
        if (tokens.length == 2) {
          String k = tokens[0].trim();
          String v = tokens[1].trim();
          properties.put(k, v);
        } else {
          throw new IllegalArgumentException(
              "Parameters should be in the format <key>=<value>. But, an invalid parameter was passed: "
                  + kv);
        }
      }
    }
    return properties;
  }

  public static void premain(String arguments, Instrumentation instrumentation) throws IOException {
    try {
      System.err.println("[Agent] Starting chaos-dukey agent initialization");
      System.err.println("[Agent] Arguments: " + arguments);
      Properties props = propertiesFromArguments(arguments);
      System.err.println("[Agent] Parsed properties: " + props);
      Config config = new Config.Loader().load(props);
      System.err.println("[Agent] Config loaded successfully");
      {
        InterceptorForDelay interceptor = new InterceptorForDelay(config.delayConfig, config.debug);
        AgentBuilder agentBuilder =
            new AgentBuilder.Default()
                .type(config.delayConfig.typeMatcher)
                .transform(
                    (builder, type, classLoader, module, protectionDomain) ->
                        builder
                            .method(config.delayConfig.methodMatcher)
                            .intercept(MethodDelegation.to(interceptor)));
        if (config.debug) {
          agentBuilder = agentBuilder.with(AgentBuilder.Listener.StreamWriting.toSystemError());
        }
        agentBuilder.installOn(instrumentation);
        System.err.println("[Agent] Delay interceptor installed");
      }
      {
        InterceptorForFailure interceptor =
            new InterceptorForFailure(config.failureConfig, config.debug);
        AgentBuilder agentBuilder =
            new AgentBuilder.Default()
                .type(config.failureConfig.typeMatcher)
                .transform(
                    (builder, type, classLoader, module, protectionDomain) ->
                        builder
                            .method(config.failureConfig.methodMatcher)
                            .intercept(MethodDelegation.to(interceptor)));
        if (config.debug) {
          agentBuilder = agentBuilder.with(AgentBuilder.Listener.StreamWriting.toSystemError());
        }
        agentBuilder.installOn(instrumentation);
        System.err.println("[Agent] Failure interceptor installed");
      }
      {
        System.err.println("[Agent] Installing parameter-based delay interceptor");
        InterceptorForParameterBasedDelayBase interceptor =
            new InterceptorForParameterBasedDelayBase(config.delayConfig, config.debug);
        AgentBuilder agentBuilder =
            new AgentBuilder.Default()
                .type(
                    ElementMatchers.named(
                        InterceptorForParameterBasedDelayBase.V1_QUERY_DELAY_CLASS_NAME))
                .transform(
                    (builder, type, classLoader, module, protectionDomain) ->
                        builder
                            .method(
                                ElementMatchers.named(
                                        InterceptorForParameterBasedDelayBase
                                            .V1_QUERY_DELAY_METHOD_NAME)
                                    .and(ElementMatchers.isPublic()))
                            .intercept(MethodDelegation.to(interceptor)));
        if (config.debug) {
          agentBuilder = agentBuilder.with(AgentBuilder.Listener.StreamWriting.toSystemError());
        }
        agentBuilder.installOn(instrumentation);
        System.err.println("[Agent] Parameter-based delay interceptor installed");
      }
      {
        System.err.println("[Agent] Installing parameter-based memory allocation interceptor v1");
        InterceptorForParameterBasedMemoryAllocation interceptor =
            new InterceptorForParameterBasedMemoryAllocation(config.debug);
        AgentBuilder agentBuilder =
            new AgentBuilder.Default()
                .type(
                    ElementMatchers.named(
                        InterceptorForParameterBasedDelayBase.V1_QUERY_DELAY_CLASS_NAME))
                .transform(
                    (builder, type, classLoader, module, protectionDomain) ->
                        builder
                            .method(
                                ElementMatchers.named(
                                        InterceptorForParameterBasedDelayBase
                                            .V1_QUERY_DELAY_METHOD_NAME)
                                    .and(ElementMatchers.isPublic()))
                            .intercept(MethodDelegation.to(interceptor)));
        if (config.debug) {
          agentBuilder = agentBuilder.with(AgentBuilder.Listener.StreamWriting.toSystemError());
        }
        agentBuilder.installOn(instrumentation);
        System.err.println(
            "[Agent] Parameter-based parameter-based memory allocation installed v1");
      }
      {
        System.err.println("[Agent] Installing parameter-based delay interceptor");
        InterceptorForParameterBasedDelayBase interceptor =
            new InterceptorForParameterBasedDelayBase(config.delayConfig, config.debug);
        AgentBuilder agentBuilder =
            new AgentBuilder.Default()
                .type(
                    ElementMatchers.named(
                        InterceptorForParameterBasedDelayV2.V2_QUERY_DELAY_CLASS_NAME))
                .transform(
                    (builder, type, classLoader, module, protectionDomain) ->
                        builder
                            .method(
                                ElementMatchers.named(
                                    InterceptorForParameterBasedDelayV2.V2_QUERY_DELAY_METHOD_NAME))
                            .intercept(MethodDelegation.to(interceptor)));
        if (config.debug) {
          agentBuilder = agentBuilder.with(AgentBuilder.Listener.StreamWriting.toSystemError());
        }
        agentBuilder.installOn(instrumentation);
        System.err.println("[Agent] Parameter-based delay interceptor for v2 installed");
      }
      {
        System.err.println("[Agent] Installing parameter-based memory allocation interceptor v2");
        InterceptorForParameterBasedMemoryAllocationV2 interceptor =
            new InterceptorForParameterBasedMemoryAllocationV2(config.debug);
        AgentBuilder agentBuilder =
            new AgentBuilder.Default()
                .type(
                    ElementMatchers.named(
                        InterceptorForParameterBasedDelayV2.V2_QUERY_DELAY_CLASS_NAME))
                .transform(
                    (builder, type, classLoader, module, protectionDomain) ->
                        builder
                            .method(
                                ElementMatchers.named(
                                    InterceptorForParameterBasedDelayV2.V2_QUERY_DELAY_METHOD_NAME))
                            .intercept(MethodDelegation.to(interceptor)));
        if (config.debug) {
          agentBuilder = agentBuilder.with(AgentBuilder.Listener.StreamWriting.toSystemError());
        }
        agentBuilder.installOn(instrumentation);
        System.err.println(
            "[Agent] Parameter-based parameter-based memory allocation installed v2");
      }
      System.err.println("[Agent] All interceptors installed successfully");
    } catch (Exception e) {
      System.err.println("[Agent] ERROR during initialization: " + e.getMessage());
      e.printStackTrace(System.err);
      throw e;
    }
  }
}

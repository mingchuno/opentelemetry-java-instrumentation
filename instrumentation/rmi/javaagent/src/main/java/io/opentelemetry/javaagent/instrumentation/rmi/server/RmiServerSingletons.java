/*
 * Copyright The OpenTelemetry Authors
 * SPDX-License-Identifier: Apache-2.0
 */

package io.opentelemetry.javaagent.instrumentation.rmi.server;

import io.opentelemetry.api.GlobalOpenTelemetry;
import io.opentelemetry.instrumentation.api.instrumenter.Instrumenter;
import io.opentelemetry.instrumentation.api.instrumenter.SpanKindExtractor;
import io.opentelemetry.instrumentation.api.instrumenter.rpc.RpcServerAttributesExtractor;
import io.opentelemetry.instrumentation.api.instrumenter.rpc.RpcSpanNameExtractor;
import io.opentelemetry.instrumentation.api.util.ClassAndMethod;

public final class RmiServerSingletons {

  private static final Instrumenter<ClassAndMethod, Void> INSTRUMENTER;

  static {
    RmiServerAttributesGetter rpcAttributesGetter = RmiServerAttributesGetter.INSTANCE;

    INSTRUMENTER =
        Instrumenter.<ClassAndMethod, Void>builder(
                GlobalOpenTelemetry.get(),
                "io.opentelemetry.rmi",
                RpcSpanNameExtractor.create(rpcAttributesGetter))
            .addAttributesExtractor(RpcServerAttributesExtractor.create(rpcAttributesGetter))
            .buildInstrumenter(SpanKindExtractor.alwaysServer());
  }

  public static Instrumenter<ClassAndMethod, Void> instrumenter() {
    return INSTRUMENTER;
  }

  private RmiServerSingletons() {}
}

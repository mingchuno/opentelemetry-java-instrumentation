/*
 * Copyright The OpenTelemetry Authors
 * SPDX-License-Identifier: Apache-2.0
 */

package io.opentelemetry.javaagent.instrumentation.vertx.v4_0.client;

import io.opentelemetry.instrumentation.api.instrumenter.net.NetClientAttributesGetter;
import io.opentelemetry.semconv.trace.attributes.SemanticAttributes;
import io.vertx.core.http.HttpClientRequest;
import io.vertx.core.http.HttpClientResponse;
import javax.annotation.Nullable;

final class Vertx4NetAttributesGetter
    implements NetClientAttributesGetter<HttpClientRequest, HttpClientResponse> {

  @Override
  public String transport(HttpClientRequest request, @Nullable HttpClientResponse response) {
    return SemanticAttributes.NetTransportValues.IP_TCP;
  }

  @Nullable
  @Override
  public String peerName(HttpClientRequest request, @Nullable HttpClientResponse response) {
    return request.getHost();
  }

  @Override
  public Integer peerPort(HttpClientRequest request, @Nullable HttpClientResponse response) {
    return request.getPort();
  }
}

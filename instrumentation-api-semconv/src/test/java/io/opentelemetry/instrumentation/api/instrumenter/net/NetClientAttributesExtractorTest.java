/*
 * Copyright The OpenTelemetry Authors
 * SPDX-License-Identifier: Apache-2.0
 */

package io.opentelemetry.instrumentation.api.instrumenter.net;

import static io.opentelemetry.sdk.testing.assertj.OpenTelemetryAssertions.assertThat;
import static org.assertj.core.api.Assertions.entry;

import io.opentelemetry.api.common.AttributeKey;
import io.opentelemetry.api.common.Attributes;
import io.opentelemetry.api.common.AttributesBuilder;
import io.opentelemetry.context.Context;
import io.opentelemetry.semconv.trace.attributes.SemanticAttributes;
import java.util.HashMap;
import java.util.Map;
import org.junit.jupiter.api.Test;

// TODO (trask) add more test coverage for #6268
class NetClientAttributesExtractorTest {

  static class TestNetClientAttributesGetter
      implements NetClientAttributesGetter<Map<String, String>, Map<String, String>> {

    @Override
    public String transport(Map<String, String> request, Map<String, String> response) {
      return response.get("transport");
    }

    @Override
    public String peerName(Map<String, String> request, Map<String, String> response) {
      if (response != null) {
        return response.get("peerName");
      }
      return null;
    }

    @Override
    public Integer peerPort(Map<String, String> request, Map<String, String> response) {
      if (response != null) {
        return Integer.valueOf(response.get("peerPort"));
      }
      return null;
    }

    @Override
    public String sockPeerAddr(Map<String, String> request, Map<String, String> response) {
      if (response != null) {
        return response.get("sockPeerAddr");
      }
      return null;
    }
  }

  @Test
  void normal() {
    // given
    Map<String, String> request = new HashMap<>();
    request.put("transport", "TCP");
    request.put("peerName", "github.com");
    request.put("peerPort", "123");
    request.put("sockPeerAddr", "1.2.3.4");

    Map<String, String> response = new HashMap<>();
    response.put("peerName", "opentelemetry.io");
    response.put("peerPort", "42");
    response.put("sockPeerAddr", "4.3.2.1");

    TestNetClientAttributesGetter getter = new TestNetClientAttributesGetter();
    NetClientAttributesExtractor<Map<String, String>, Map<String, String>> extractor =
        NetClientAttributesExtractor.create(getter);

    Context context = Context.root();

    // when
    AttributesBuilder startAttributes = Attributes.builder();
    extractor.onStart(startAttributes, context, request);

    AttributesBuilder endAttributes = Attributes.builder();
    extractor.onEnd(endAttributes, context, request, response, null);

    // then
    assertThat(startAttributes.build()).isEmpty();

    assertThat(endAttributes.build())
        .containsOnly(
            entry(SemanticAttributes.NET_PEER_NAME, "opentelemetry.io"),
            entry(SemanticAttributes.NET_PEER_PORT, 42L),
            entry(AttributeKey.stringKey("net.sock.peer.addr"), "4.3.2.1"));
  }

  @Test
  public void doesNotSetDuplicateAttributes() {
    // given
    Map<String, String> request = new HashMap<>();
    request.put("transport", "TCP");
    request.put("peerName", "1.2.3.4");
    request.put("sockPeerAddr", "1.2.3.4");
    request.put("peerPort", "123");

    Map<String, String> response = new HashMap<>();
    response.put("peerName", "4.3.2.1");
    response.put("peerPort", "42");
    response.put("sockPeerAddr", "4.3.2.1");

    TestNetClientAttributesGetter getter = new TestNetClientAttributesGetter();
    NetClientAttributesExtractor<Map<String, String>, Map<String, String>> extractor =
        NetClientAttributesExtractor.create(getter);

    Context context = Context.root();

    // when
    AttributesBuilder startAttributes = Attributes.builder();
    extractor.onStart(startAttributes, context, request);

    AttributesBuilder endAttributes = Attributes.builder();
    extractor.onEnd(endAttributes, context, request, response, null);

    // then
    assertThat(startAttributes.build()).isEmpty();

    assertThat(endAttributes.build())
        .containsOnly(
            entry(SemanticAttributes.NET_PEER_PORT, 42L),
            entry(SemanticAttributes.NET_PEER_NAME, "4.3.2.1"));
  }

  @Test
  public void doesNotSetNegativePort() {
    // given
    Map<String, String> request = new HashMap<>();
    request.put("peerPort", "-42");

    Map<String, String> response = new HashMap<>();
    response.put("peerPort", "-1");

    TestNetClientAttributesGetter getter = new TestNetClientAttributesGetter();
    NetClientAttributesExtractor<Map<String, String>, Map<String, String>> extractor =
        NetClientAttributesExtractor.create(getter);

    Context context = Context.root();

    // when
    AttributesBuilder startAttributes = Attributes.builder();
    extractor.onStart(startAttributes, context, request);

    AttributesBuilder endAttributes = Attributes.builder();
    extractor.onEnd(endAttributes, context, request, response, null);

    // then
    assertThat(startAttributes.build()).isEmpty();
    assertThat(endAttributes.build()).isEmpty();
  }
}

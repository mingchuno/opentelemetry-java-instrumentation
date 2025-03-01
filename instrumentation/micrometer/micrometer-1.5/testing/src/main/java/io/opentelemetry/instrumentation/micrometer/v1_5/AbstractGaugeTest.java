/*
 * Copyright The OpenTelemetry Authors
 * SPDX-License-Identifier: Apache-2.0
 */

package io.opentelemetry.instrumentation.micrometer.v1_5;

import static io.opentelemetry.instrumentation.micrometer.v1_5.AbstractCounterTest.INSTRUMENTATION_NAME;
import static io.opentelemetry.sdk.testing.assertj.OpenTelemetryAssertions.assertThat;
import static io.opentelemetry.sdk.testing.assertj.OpenTelemetryAssertions.attributeEntry;

import io.micrometer.core.instrument.Gauge;
import io.micrometer.core.instrument.Metrics;
import io.opentelemetry.instrumentation.testing.junit.InstrumentationExtension;
import java.lang.ref.WeakReference;
import java.util.concurrent.atomic.AtomicLong;
import org.assertj.core.api.AbstractIterableAssert;
import org.junit.jupiter.api.Test;

abstract class AbstractGaugeTest {

  protected abstract InstrumentationExtension testing();

  @Test
  void testGauge() {
    // given
    Gauge gauge =
        Gauge.builder("testGauge", () -> 42)
            .description("This is a test gauge")
            .tags("tag", "value")
            .baseUnit("items")
            .register(Metrics.globalRegistry);

    // then
    testing()
        .waitAndAssertMetrics(
            INSTRUMENTATION_NAME,
            "testGauge",
            metrics ->
                metrics.anySatisfy(
                    metric ->
                        assertThat(metric)
                            .hasDescription("This is a test gauge")
                            .hasUnit("items")
                            .hasDoubleGaugeSatisfying(
                                doubleGauge ->
                                    doubleGauge.hasPointsSatisfying(
                                        point ->
                                            point
                                                .hasValue(42)
                                                .hasAttributes(attributeEntry("tag", "value"))))));

    // when
    Metrics.globalRegistry.remove(gauge);
    testing().clearData();

    // then
    testing()
        .waitAndAssertMetrics(INSTRUMENTATION_NAME, "testGauge", AbstractIterableAssert::isEmpty);
  }

  @Test
  void gaugesWithSameNameAndDifferentTags() {
    // given
    Gauge.builder("testGaugeWithTags", () -> 12)
        .description("First description wins")
        .baseUnit("items")
        .tags("tag", "1")
        .register(Metrics.globalRegistry);
    Gauge.builder("testGaugeWithTags", () -> 42)
        .description("ignored")
        .baseUnit("items")
        .tags("tag", "2")
        .register(Metrics.globalRegistry);

    // then
    testing()
        .waitAndAssertMetrics(
            INSTRUMENTATION_NAME,
            "testGaugeWithTags",
            metrics ->
                metrics.anySatisfy(
                    metric ->
                        assertThat(metric)
                            .hasDescription("First description wins")
                            .hasUnit("items")
                            .hasDoubleGaugeSatisfying(
                                gauge ->
                                    gauge.hasPointsSatisfying(
                                        point ->
                                            point
                                                .hasValue(12)
                                                .hasAttributes(attributeEntry("tag", "1")),
                                        point ->
                                            point
                                                .hasValue(42)
                                                .hasAttributes(attributeEntry("tag", "2"))))));
  }

  @Test
  void testWeakRefGauge() throws InterruptedException {
    // given
    AtomicLong num = new AtomicLong(42);
    Gauge.builder("testWeakRefGauge", num, AtomicLong::get)
        .strongReference(false)
        .register(Metrics.globalRegistry);

    // then
    testing()
        .waitAndAssertMetrics(
            INSTRUMENTATION_NAME,
            "testWeakRefGauge",
            metrics ->
                metrics.anySatisfy(
                    metric ->
                        assertThat(metric)
                            .hasDoubleGaugeSatisfying(
                                gauge -> gauge.hasPointsSatisfying(point -> point.hasValue(42)))));

    // when
    WeakReference<AtomicLong> numWeakRef = new WeakReference<>(num);
    num = null;
    awaitGc(numWeakRef);
    testing().clearData();

    // then
    testing()
        .waitAndAssertMetrics(
            INSTRUMENTATION_NAME, "testWeakRefGauge", AbstractIterableAssert::isEmpty);
  }

  private static void awaitGc(WeakReference<?> ref) throws InterruptedException {
    while (ref.get() != null) {
      if (Thread.interrupted()) {
        throw new InterruptedException();
      }
      System.gc();
      System.runFinalization();
    }
  }
}

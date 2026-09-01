// Copyright 2013 The Go Authors. All rights reserved.
// Use of this source code is governed by a BSD-style
// license that can be found in the LICENSE file.

package io.github.goquerystring.query;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertNotEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;

import java.time.Instant;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.time.OffsetDateTime;
import java.time.ZoneOffset;
import java.time.ZonedDateTime;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collections;
import java.util.Date;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import org.junit.jupiter.api.Test;

/**
 * Cases with no counterpart in {@code query/encode_test.go}, added by the migration.
 *
 * <p>They exist because the port has surface the Go original does not: {@code url.Values} is a
 * standard-library type covered by Go's own tests, whereas {@link UrlValues} is ported code and is
 * only reached by the ported suite through {@code add}. The rest of the cases cover the kinds the
 * source's suite never hands to the renderer -- Go's own suite exercises one temporal type and one
 * float width, while the port's renderer accepts several of each, and an unexercised branch is
 * where a classification error hides.
 *
 * <p>{@link QueryTest} stays a one-for-one port of the Go suite; nothing here is a substitute for
 * any case in it.
 */
class QueryApiTest {

  private static final Instant TIME =
      LocalDateTime.of(2000, 1, 1, 12, 34, 56).toInstant(ZoneOffset.UTC);

  // -----------------------------------------------------------------------------------------
  // UrlValues: the port of net/url.Values
  // -----------------------------------------------------------------------------------------

  @Test
  void urlValuesAccessors() {
    UrlValues v = new UrlValues();
    assertTrue(v.isEmpty());
    assertEquals(0, v.size());
    assertEquals("", v.get("absent"));
    assertEquals(Collections.emptyList(), v.getAll("absent"));
    assertFalse(v.containsKey("absent"));

    v.add("a", "1");
    v.add("a", "2");
    v.add("b", "3");

    assertFalse(v.isEmpty());
    assertEquals(2, v.size());
    assertTrue(v.containsKey("a"));
    assertEquals("1", v.get("a"), "get returns the first value");
    assertEquals(Arrays.asList("1", "2"), v.getAll("a"));
    assertEquals(Arrays.asList("a", "b"), new ArrayList<>(v.keys()), "keys stay in insertion order");

    v.set("a", "9");
    assertEquals(Collections.singletonList("9"), v.getAll("a"), "set replaces every value");

    v.remove("a");
    assertFalse(v.containsKey("a"));
    assertEquals("", v.get("a"));
    assertEquals(1, v.size());
  }

  @Test
  void urlValuesKeysAndMapAreUnmodifiable() {
    UrlValues v = new UrlValues();
    v.add("a", "1");

    assertThrows(UnsupportedOperationException.class, () -> v.keys().add("b"));
    assertThrows(UnsupportedOperationException.class, () -> v.getAll("a").add("2"));
    Map<String, List<String>> map = v.asMap();
    assertThrows(UnsupportedOperationException.class, () -> map.put("b", new ArrayList<>()));
    assertThrows(UnsupportedOperationException.class, () -> map.get("a").add("2"));
  }

  @Test
  void urlValuesEquality() {
    UrlValues a = new UrlValues();
    a.add("k", "1");
    a.add("k", "2");
    UrlValues b = new UrlValues();
    b.add("k", "1");
    b.add("k", "2");
    UrlValues reordered = new UrlValues();
    reordered.add("k", "2");
    reordered.add("k", "1");

    assertEquals(a, b);
    assertEquals(a.hashCode(), b.hashCode());
    assertEquals(a.toString(), b.toString());
    assertNotEquals(a, reordered, "values under one key are ordered");
    assertNotEquals(a, new UrlValues());
    assertNotEquals(a, "not a UrlValues");
    assertTrue(a.toString().contains("k"));
  }

  @Test
  void urlValuesEncodeEmpty() {
    assertEquals("", new UrlValues().encode());
  }

  @Test
  void queryEscapeMatchesGo() {
    assertEquals("abcXYZ019", UrlValues.queryEscape("abcXYZ019"), "unreserved letters and digits");
    assertEquals("-_.~", UrlValues.queryEscape("-_.~"), "unreserved punctuation");
    assertEquals("+", UrlValues.queryEscape(" "), "space becomes a plus");
    assertEquals("%2F%3F%23%5B%5D%40", UrlValues.queryEscape("/?#[]@"), "upper-case hex");
    assertEquals("%C3%A9", UrlValues.queryEscape("\u00e9"), "per UTF-8 byte");
    assertEquals("", UrlValues.queryEscape(""));
  }

  // -----------------------------------------------------------------------------------------
  // Ptr
  // -----------------------------------------------------------------------------------------

  @Test
  void ptrValueSemantics() {
    Ptr<String> a = Ptr.of("s");
    Ptr<String> b = Ptr.of("s");

    assertEquals("s", a.get());
    assertEquals(a, b);
    assertEquals(a.hashCode(), b.hashCode());
    assertEquals("s", a.toString());
    assertNotEquals(a, Ptr.of("t"));
    assertNotEquals(a, "s");
    assertThrows(
        NullPointerException.class, () -> Ptr.of(null), "a null field is the nil pointer");
  }

  // -----------------------------------------------------------------------------------------
  // Tag parsing
  // -----------------------------------------------------------------------------------------

  @Test
  void tagOptionsToString() {
    assertEquals("[omitempty, int]", Query.parseTag("name,omitempty,int").opts.toString());
    assertEquals("[]", Query.parseTag("name").opts.toString());
  }

  @Test
  void parseTagKeepsEmptyName() {
    Query.Tag tag = Query.parseTag(",omitempty");
    assertEquals("", tag.name);
    assertTrue(tag.opts.contains("omitempty"));
  }

  // -----------------------------------------------------------------------------------------
  // Temporal types the Go suite does not exercise
  // -----------------------------------------------------------------------------------------

  static class STemporal {
    @Url("instant")
    public Instant instant = TIME;

    @Url("offset")
    public OffsetDateTime offset = TIME.atOffset(ZoneOffset.UTC);

    @Url("zoned")
    public ZonedDateTime zoned = TIME.atZone(ZoneOffset.UTC);

    @Url("local")
    public LocalDateTime local = LocalDateTime.ofInstant(TIME, ZoneOffset.UTC);

    @Url("date")
    public LocalDate date = LocalDate.of(2000, 1, 1);

    @Url("legacy")
    public Date legacy = Date.from(TIME);
  }

  @Test
  void everyTemporalTypeRendersAsRfc3339() throws QueryException {
    UrlValues v = Query.values(new STemporal());

    assertEquals("2000-01-01T12:34:56Z", v.get("instant"));
    assertEquals("2000-01-01T12:34:56Z", v.get("offset"));
    assertEquals("2000-01-01T12:34:56Z", v.get("zoned"));
    assertEquals("2000-01-01T12:34:56Z", v.get("local"));
    assertEquals("2000-01-01T00:00:00Z", v.get("date"), "a date starts its day");
    assertEquals("2000-01-01T12:34:56Z", v.get("legacy"));
  }

  static class SNonUtcOffset {
    @Url("v")
    public OffsetDateTime V = TIME.atOffset(ZoneOffset.ofHours(2));
  }

  static class SNonUtcUnix {
    @Url("v,unix")
    public OffsetDateTime V = TIME.atOffset(ZoneOffset.ofHours(2));
  }

  @Test
  void offsetIsPreservedAndUnixIsAbsolute() throws QueryException {
    assertEquals(
        "2000-01-01T14:34:56+02:00",
        Query.values(new SNonUtcOffset()).get("v"),
        "an offset renders in its own zone");
    assertEquals(
        Long.toString(TIME.getEpochSecond()),
        Query.values(new SNonUtcUnix()).get("v"),
        "unix time is the same instant regardless of zone");
  }

  static class SZeroTime {
    @Url("kept")
    public Instant kept = Query.ZERO_TIME;

    @Url("dropped,omitempty")
    public Instant dropped = Query.ZERO_TIME;

    @Url("droppedDate,omitempty")
    public LocalDate droppedDate = LocalDate.of(1, 1, 1);

    @Url("present,omitempty")
    public Instant present = TIME;
  }

  @Test
  void zeroTimeIsEmptyOnlyForOmitEmpty() throws QueryException {
    UrlValues v = Query.values(new SZeroTime());

    assertTrue(v.containsKey("kept"), "a zero time is still encoded without omitempty");
    assertFalse(v.containsKey("dropped"));
    assertFalse(v.containsKey("droppedDate"), "the zero time of any temporal type is empty");
    assertTrue(v.containsKey("present"));
    assertTrue(Query.isEmptyValue(Query.ZERO_TIME));
    assertFalse(Query.isEmptyValue(Instant.EPOCH), "the unix epoch is not the zero time");
  }

  // -----------------------------------------------------------------------------------------
  // Float rendering: Go's fmt.Sprint, not Java's toString
  // -----------------------------------------------------------------------------------------

  static class SDouble {
    @Url("v")
    public double V;

    SDouble(double v) {
      V = v;
    }
  }

  static class SFloat {
    @Url("v")
    public float V;

    SFloat(float v) {
      V = v;
    }
  }

  private static String rendered(double d) throws QueryException {
    return Query.values(new SDouble(d)).get("v");
  }

  @Test
  void doublesRenderInGoShortestForm() throws QueryException {
    assertEquals("0", rendered(0d));
    assertEquals("1", rendered(1d));
    assertEquals("-1", rendered(-1d));
    assertEquals("0.1", rendered(0.1d));
    assertEquals("-0.5", rendered(-0.5d));
    assertEquals("100", rendered(100d));
    assertEquals("1234.5678", rendered(1234.5678d));
    assertEquals("0.001", rendered(0.001d));
  }

  @Test
  void doublesSwitchToExponentFormAtGosThresholds() throws QueryException {
    assertEquals("0.0001", rendered(1e-4), "the last decimal form on the small side");
    assertEquals("1e-05", rendered(1e-5), "below it, exponent form");
    assertEquals("1e+21", rendered(1e21), "at and above 1e21, exponent form");
    assertEquals("-1e-07", rendered(-1e-7));
    assertEquals("1.5e+22", rendered(1.5e22));
  }

  @Test
  void nonFiniteAndNegativeZeroFollowGo() throws QueryException {
    assertEquals("NaN", rendered(Double.NaN));
    assertEquals("+Inf", rendered(Double.POSITIVE_INFINITY));
    assertEquals("-Inf", rendered(Double.NEGATIVE_INFINITY));
    assertEquals("-0", rendered(-0.0d), "negative zero keeps its sign");
  }

  @Test
  void floatsUseTheirOwnWidth() throws QueryException {
    assertEquals("0.1", Query.values(new SFloat(0.1f)).get("v"), "not the double widening of 0.1f");
    assertEquals("NaN", Query.values(new SFloat(Float.NaN)).get("v"));
    assertEquals("+Inf", Query.values(new SFloat(Float.POSITIVE_INFINITY)).get("v"));
  }

  // -----------------------------------------------------------------------------------------
  // Kinds rejected as input
  // -----------------------------------------------------------------------------------------

  @Test
  void everyNonObjectKindIsRejected() {
    Object[] rejected = {
      "", true, 42, 42L, (short) 42, (byte) 42, 4.2f, 4.2d, 'c',
      Collections.singletonList("a"), new HashMap<String, String>(), new int[] {1},
      Instant.now(), Ptr.of("s"),
    };

    for (Object value : rejected) {
      QueryException e =
          assertThrows(
              QueryException.class,
              () -> Query.values(value),
              () -> "expected " + value.getClass().getSimpleName() + " to be rejected");
      assertTrue(
          e.getMessage().startsWith("query: Values() expects struct input. Got "),
          () -> "unexpected message: " + e.getMessage());
    }
  }

  @Test
  void nilPointerInputIsNotAnError() throws QueryException {
    assertTrue(Query.values(null).isEmpty());
  }

  // -----------------------------------------------------------------------------------------
  // Emptiness for kinds the Go suite does not cover
  // -----------------------------------------------------------------------------------------

  @Test
  void emptinessOfRemainingKinds() {
    assertTrue(Query.isEmptyValue('\0'));
    assertFalse(Query.isEmptyValue('a'));
    assertTrue(Query.isEmptyValue(new StringBuilder()), "any CharSequence, not just String");
    assertFalse(Query.isEmptyValue(new StringBuilder("a")));
    assertTrue(Query.isEmptyValue(new String[0]));
    assertFalse(Query.isEmptyValue(new String[] {"a"}));
  }

  // -----------------------------------------------------------------------------------------
  // Custom encoders that cannot be instantiated
  // -----------------------------------------------------------------------------------------

  static final class NoDefaultConstructor implements Encoder {
    private final String value;

    NoDefaultConstructor(String value) {
      this.value = value;
    }

    @Override
    public void encodeValues(String key, UrlValues values) {
      values.set(key, value);
    }
  }

  static class SNoDefaultConstructor {
    @Url("v")
    public NoDefaultConstructor V;
  }

  @Test
  void aNullEncoderFieldNeedsANoArgConstructor() {
    QueryException e =
        assertThrows(QueryException.class, () -> Query.values(new SNoDefaultConstructor()));
    assertTrue(
        e.getMessage().contains("no-argument constructor"),
        () -> "unexpected message: " + e.getMessage());
    assertTrue(e.getCause() instanceof ReflectiveOperationException, "the cause is kept");
  }

  // -----------------------------------------------------------------------------------------
  // Stringer, and the raw Ptr whose target type cannot be resolved
  // -----------------------------------------------------------------------------------------

  static final class Money implements Stringer {
    private final int cents;

    Money(int cents) {
      this.cents = cents;
    }

    @Override
    public String string() {
      return cents + "c";
    }
  }

  static class SStringer {
    @Url("price")
    public Money price = new Money(250);
  }

  @Test
  void aStringerIsOneValueRatherThanAnObjectToWalkInto() throws QueryException {
    UrlValues v = Query.values(new SStringer());
    assertEquals("250c", v.get("price"));
    assertEquals(1, v.size(), "its fields are not walked into");
  }

  @SuppressWarnings("rawtypes")
  static class SRawPtr {
    @Url("v")
    public Ptr V;
  }

  @Test
  void aRawPtrFieldFallsBackToAnEmptyValue() throws QueryException {
    assertEquals(
        "", Query.values(new SRawPtr()).get("v"), "with no type argument there is no encoder to find");
  }

  // -----------------------------------------------------------------------------------------
  // Documented behaviours that the Go suite states only in its doc comment
  // -----------------------------------------------------------------------------------------

  static class Address {
    @Url("postcode")
    public String postcode = "1234";

    @Url("city")
    public String city = "SFO";
  }

  static class User {
    @Url("name")
    public String name = "acme";

    @Url("addr")
    public Address addr = new Address();
  }

  static class UserRequest {
    @Url("user")
    public User user = new User();
  }

  @Test
  void nestedObjectsAreScopedAsTheDocCommentSays() throws QueryException {
    UrlValues v = Query.values(new UserRequest());

    assertEquals("acme", v.get("user[name]"));
    assertEquals("1234", v.get("user[addr][postcode]"));
    assertEquals("SFO", v.get("user[addr][city]"));
  }

  static class SDuplicateNames {
    @Url("v")
    public String a = "1";

    @Url("v")
    public String b = "2";
  }

  @Test
  void twoFieldsSharingANameBecomeTwoValues() throws QueryException {
    assertEquals(Arrays.asList("1", "2"), Query.values(new SDuplicateNames()).getAll("v"));
  }
}

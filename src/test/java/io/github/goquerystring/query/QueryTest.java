// Copyright 2013 The Go Authors. All rights reserved.
// Use of this source code is governed by a BSD-style
// license that can be found in the LICENSE file.

package io.github.goquerystring.query;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;

import java.time.Instant;
import java.time.LocalDateTime;
import java.time.ZoneOffset;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collections;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import org.junit.jupiter.api.Test;

/**
 * Port of {@code query/encode_test.go} from github.com/google/go-querystring.
 *
 * <p>Field names are kept as in the Go tests (a bare {@code V}) so each case can be read against the
 * original. Where Go relies on zero values that Java spells differently -- a zero struct value, a
 * zero named scalar -- the value is constructed explicitly and the difference is noted.
 */
class QueryTest {

  private static final Instant TIME =
      LocalDateTime.of(2000, 1, 1, 12, 34, 56).toInstant(ZoneOffset.UTC);

  /** Tests that {@code Query.values(input)} matches {@code want}. */
  private static void testValue(Object input, UrlValues want) {
    UrlValues got;
    try {
      got = Query.values(input);
    } catch (QueryException e) {
      throw new AssertionError("Values(" + input + ") returned error: " + e.getMessage(), e);
    }
    assertEquals(want.asMap(), got.asMap(), () -> "Values(" + describe(input) + ") mismatch");
  }

  private static String describe(Object input) {
    return input == null ? "null" : input.getClass().getSimpleName() + " " + input;
  }

  private static Want want() {
    return new Want();
  }

  /** Small builder for the expected {@link UrlValues}. */
  private static final class Want {
    private final UrlValues values = new UrlValues();

    Want add(String key, String... vals) {
      for (String v : vals) {
        values.add(key, v);
      }
      return this;
    }

    UrlValues done() {
      return values;
    }
  }

  // -----------------------------------------------------------------------------------------
  // TestValues_BasicTypes
  // -----------------------------------------------------------------------------------------

  static class SString {
    public String V;

    SString() {}

    SString(String v) {
      V = v;
    }
  }

  static class SInt {
    public int V;

    SInt() {}

    SInt(int v) {
      V = v;
    }
  }

  /** Go's {@code uint}; Java has no unsigned int, so a {@code long} carries the same values. */
  static class SUint {
    public long V;

    SUint() {}

    SUint(long v) {
      V = v;
    }
  }

  static class SFloat32 {
    public float V;

    SFloat32() {}

    SFloat32(float v) {
      V = v;
    }
  }

  static class SBool {
    public boolean V;

    SBool() {}

    SBool(boolean v) {
      V = v;
    }
  }

  static class SBoolInt {
    @Url(",int")
    public boolean V;

    SBoolInt(boolean v) {
      V = v;
    }
  }

  static class STime {
    public Instant V;

    STime(Instant v) {
      V = v;
    }
  }

  static class STimeUnix {
    @Url(",unix")
    public Instant V;

    STimeUnix(Instant v) {
      V = v;
    }
  }

  static class STimeUnixMilli {
    @Url(",unixmilli")
    public Instant V;

    STimeUnixMilli(Instant v) {
      V = v;
    }
  }

  static class STimeUnixNano {
    @Url(",unixnano")
    public Instant V;

    STimeUnixNano(Instant v) {
      V = v;
    }
  }

  static class STimeLayout {
    @Layout("yyyy-MM-dd")
    public Instant V;

    STimeLayout(Instant v) {
      V = v;
    }
  }

  @Test
  void valuesBasicTypes() {
    // zero values
    testValue(new SString(), want().add("V", "").done());
    testValue(new SInt(), want().add("V", "0").done());
    testValue(new SUint(), want().add("V", "0").done());
    testValue(new SFloat32(), want().add("V", "0").done());
    testValue(new SBool(), want().add("V", "false").done());

    // simple non-zero values
    testValue(new SString("v"), want().add("V", "v").done());
    testValue(new SInt(1), want().add("V", "1").done());
    testValue(new SUint(1), want().add("V", "1").done());
    testValue(new SFloat32(0.1f), want().add("V", "0.1").done());
    testValue(new SBool(true), want().add("V", "true").done());

    // bool-specific options
    testValue(new SBoolInt(false), want().add("V", "0").done());
    testValue(new SBoolInt(true), want().add("V", "1").done());

    // time values
    testValue(new STime(TIME), want().add("V", "2000-01-01T12:34:56Z").done());
    testValue(new STimeUnix(TIME), want().add("V", "946730096").done());
    testValue(new STimeUnixMilli(TIME), want().add("V", "946730096000").done());
    testValue(new STimeUnixNano(TIME), want().add("V", "946730096000000000").done());
    testValue(new STimeLayout(TIME), want().add("V", "2000-01-01").done());
  }

  // -----------------------------------------------------------------------------------------
  // TestValues_Pointers
  // -----------------------------------------------------------------------------------------

  static class SPtrString {
    public Ptr<String> V;

    SPtrString() {}

    SPtrString(Ptr<String> v) {
      V = v;
    }
  }

  static class SPtrInt {
    public Ptr<Integer> V;

    SPtrInt() {}
  }

  static class SPtrPtrString {
    public Ptr<Ptr<String>> V;

    SPtrPtrString(Ptr<Ptr<String>> v) {
      V = v;
    }
  }

  static class SListPtrString {
    public List<Ptr<String>> V;

    SListPtrString() {}

    SListPtrString(List<Ptr<String>> v) {
      V = v;
    }
  }

  static class SPtrListString {
    public Ptr<List<String>> V;

    SPtrListString() {}

    SPtrListString(Ptr<List<String>> v) {
      V = v;
    }
  }

  static class Empty {}

  @Test
  void valuesPointers() {
    String str = "s";

    // nil pointers (zero values)
    testValue(new SPtrString(), want().add("V", "").done());
    testValue(new SPtrInt(), want().add("V", "").done());

    // non-zero pointer values
    testValue(new SPtrString(Ptr.of(str)), want().add("V", "s").done());
    testValue(new SPtrPtrString(Ptr.of(Ptr.of(str))), want().add("V", "s").done());

    // lists of pointer values
    testValue(new SListPtrString(), want().done());
    testValue(
        new SListPtrString(Arrays.asList(Ptr.of(str), Ptr.of(str))),
        want().add("V", "s", "s").done());

    // pointer to list
    testValue(new SPtrListString(), want().add("V", "").done());
    testValue(
        new SPtrListString(Ptr.of(Arrays.asList("a", "b"))), want().add("V", "a", "b").done());

    // pointer values for the input object itself
    testValue(null, want().done());
    testValue(new Empty(), want().done());
    testValue(Ptr.of(new Empty()), want().done());
    testValue(Ptr.of(new SString()), want().add("V", "").done());
    testValue(Ptr.of(new SString("v")), want().add("V", "v").done());
  }

  // -----------------------------------------------------------------------------------------
  // TestValues_Slices
  // -----------------------------------------------------------------------------------------

  static class SListString {
    public List<String> V;

    SListString() {}

    SListString(List<String> v) {
      V = v;
    }
  }

  static class SListStringComma {
    @Url(",comma")
    public List<String> V;

    SListStringComma(List<String> v) {
      V = v;
    }
  }

  static class SListStringSpace {
    @Url(",space")
    public List<String> V;

    SListStringSpace(List<String> v) {
      V = v;
    }
  }

  static class SListStringSemicolon {
    @Url(",semicolon")
    public List<String> V;

    SListStringSemicolon(List<String> v) {
      V = v;
    }
  }

  static class SListStringBrackets {
    @Url(",brackets")
    public List<String> V;

    SListStringBrackets(List<String> v) {
      V = v;
    }
  }

  static class SListStringNumbered {
    @Url(",numbered")
    public List<String> V;

    SListStringNumbered(List<String> v) {
      V = v;
    }
  }

  static class SArrayString {
    public String[] V;

    SArrayString(String[] v) {
      V = v;
    }
  }

  static class SArrayStringComma {
    @Url(",comma")
    public String[] V;

    SArrayStringComma(String[] v) {
      V = v;
    }
  }

  static class SArrayStringSpace {
    @Url(",space")
    public String[] V;

    SArrayStringSpace(String[] v) {
      V = v;
    }
  }

  static class SArrayStringSemicolon {
    @Url(",semicolon")
    public String[] V;

    SArrayStringSemicolon(String[] v) {
      V = v;
    }
  }

  static class SArrayStringBrackets {
    @Url(",brackets")
    public String[] V;

    SArrayStringBrackets(String[] v) {
      V = v;
    }
  }

  static class SArrayStringNumbered {
    @Url(",numbered")
    public String[] V;

    SArrayStringNumbered(String[] v) {
      V = v;
    }
  }

  static class SListStringDelComma {
    @Del(",")
    public List<String> V;

    SListStringDelComma(List<String> v) {
      V = v;
    }
  }

  static class SListStringDelPipe {
    @Del("|")
    public List<String> V;

    SListStringDelPipe(List<String> v) {
      V = v;
    }
  }

  static class SListStringDelEmoji {
    @Del("\uD83E\uDD51")
    public List<String> V;

    SListStringDelEmoji(List<String> v) {
      V = v;
    }
  }

  static class SListBoolSpaceInt {
    @Url(",space,int")
    public List<Boolean> V;

    SListBoolSpaceInt(List<Boolean> v) {
      V = v;
    }
  }

  @Test
  void valuesSlices() {
    // lists of strings
    testValue(new SListString(), want().done());
    testValue(new SListString(new ArrayList<>()), want().done());
    testValue(new SListString(Collections.singletonList("")), want().add("V", "").done());
    testValue(new SListString(Arrays.asList("a", "b")), want().add("V", "a", "b").done());
    testValue(new SListStringComma(new ArrayList<>()), want().done());
    testValue(new SListStringComma(Collections.singletonList("")), want().add("V", "").done());
    testValue(new SListStringComma(Arrays.asList("a", "b")), want().add("V", "a,b").done());
    testValue(new SListStringSpace(Arrays.asList("a", "b")), want().add("V", "a b").done());
    testValue(new SListStringSemicolon(Arrays.asList("a", "b")), want().add("V", "a;b").done());
    testValue(new SListStringBrackets(Arrays.asList("a", "b")), want().add("V[]", "a", "b").done());
    testValue(
        new SListStringNumbered(Arrays.asList("a", "b")),
        want().add("V0", "a").add("V1", "b").done());

    // arrays of strings; Go's [2]string zero value holds two empty strings
    testValue(new SArrayString(new String[2]), want().add("V", "", "").done());
    testValue(new SArrayString(new String[] {"a", "b"}), want().add("V", "a", "b").done());
    testValue(new SArrayStringComma(new String[] {"a", "b"}), want().add("V", "a,b").done());
    testValue(new SArrayStringSpace(new String[] {"a", "b"}), want().add("V", "a b").done());
    testValue(new SArrayStringSemicolon(new String[] {"a", "b"}), want().add("V", "a;b").done());
    testValue(
        new SArrayStringBrackets(new String[] {"a", "b"}), want().add("V[]", "a", "b").done());
    testValue(
        new SArrayStringNumbered(new String[] {"a", "b"}),
        want().add("V0", "a").add("V1", "b").done());

    // custom delimiters
    testValue(new SListStringDelComma(Arrays.asList("a", "b")), want().add("V", "a,b").done());
    testValue(new SListStringDelPipe(Arrays.asList("a", "b")), want().add("V", "a|b").done());
    testValue(
        new SListStringDelEmoji(Arrays.asList("a", "b")),
        want().add("V", "a\uD83E\uDD51b").done());

    // list of booleans with additional options
    testValue(new SListBoolSpaceInt(Arrays.asList(true, false)), want().add("V", "1 0").done());
  }

  // -----------------------------------------------------------------------------------------
  // TestValues_NestedTypes
  // -----------------------------------------------------------------------------------------

  static class SubNested {
    @Url("value")
    public String Value;

    SubNested() {}

    SubNested(String value) {
      Value = value;
    }
  }

  static class Nested {
    /** Go's {@code A SubNested}: a struct value is never nil, so it starts out zeroed. */
    @Url("a")
    public SubNested A = new SubNested();

    @Url("b")
    public SubNested B;

    @Url("ptr,omitempty")
    public SubNested Ptr;
  }

  static class SNest {
    @Url("nest")
    public Nested Nest;

    SNest(Nested nest) {
      Nest = nest;
    }
  }

  @Test
  void valuesNestedTypes() {
    Nested a = new Nested();
    a.A = new SubNested("v");
    testValue(new SNest(a), want().add("nest[a][value]", "v").add("nest[b]", "").done());

    Nested ptr = new Nested();
    ptr.Ptr = new SubNested("v");
    testValue(
        new SNest(ptr),
        want()
            .add("nest[a][value]", "")
            .add("nest[b]", "")
            .add("nest[ptr][value]", "v")
            .done());

    testValue(null, want().done());
  }

  // -----------------------------------------------------------------------------------------
  // TestValues_OmitEmpty
  // -----------------------------------------------------------------------------------------

  /** Go's non-exported field: in Java, a non-public field without {@link Embedded}. */
  static class SUnexported {
    String v;
  }

  static class SOmitEmpty {
    @Url(",omitempty")
    public String V;
  }

  static class SDash {
    @Url("-")
    public String V;
  }

  /** A field actually named "omitempty". */
  static class SNamedOmitEmpty {
    @Url("omitempty")
    public String V;
  }

  static class SPtrOmitEmpty {
    @Url(",omitempty")
    public Ptr<String> V;

    SPtrOmitEmpty(Ptr<String> v) {
      V = v;
    }
  }

  @Test
  void valuesOmitEmpty() {
    testValue(new SUnexported(), want().done());
    testValue(new SOmitEmpty(), want().done());
    testValue(new SDash(), want().done());
    testValue(new SNamedOmitEmpty(), want().add("omitempty", "").done());

    // include value for a non-nil pointer to an empty value
    testValue(new SPtrOmitEmpty(Ptr.of("")), want().add("V", "").done());
  }

  // -----------------------------------------------------------------------------------------
  // TestValues_EmbeddedStructs
  // -----------------------------------------------------------------------------------------

  static class Inner {
    public String V;

    Inner() {}

    Inner(String v) {
      V = v;
    }
  }

  static class Outer {
    @Embedded public Inner inner;

    Outer(Inner inner) {
      this.inner = inner;
    }
  }

  static class OuterPtr {
    @Embedded public Ptr<Inner> inner;

    OuterPtr(Ptr<Inner> inner) {
      this.inner = inner;
    }
  }

  static class Mixed {
    @Embedded public Inner inner;
    public String V;

    Mixed(Inner inner, String v) {
      this.inner = inner;
      this.V = v;
    }
  }

  /** Go's unexported embedded type. */
  static class Unexported {
    @Embedded Inner inner;
    public String V;

    Unexported(Inner inner, String v) {
      this.inner = inner;
      this.V = v;
    }
  }

  static class Exported {
    @Embedded Unexported unexported;

    Exported(Unexported unexported) {
      this.unexported = unexported;
    }
  }

  /** Java superclasses are embedded automatically, no annotation needed. */
  static class Base {
    public String V;

    Base(String v) {
      V = v;
    }
  }

  static class Derived extends Base {
    public String W;

    Derived(String v, String w) {
      super(v);
      W = w;
    }
  }

  @Test
  void valuesEmbeddedStructs() {
    testValue(new Outer(new Inner("a")), want().add("V", "a").done());
    testValue(new OuterPtr(Ptr.of(new Inner("a"))), want().add("V", "a").done());
    testValue(new Mixed(new Inner("a"), "b"), want().add("V", "b", "a").done());

    // values from an unexported embed are still included
    testValue(
        new Exported(new Unexported(new Inner("bar"), "foo")),
        want().add("V", "foo", "bar").done());

    // Java superclass fields are encoded after the subclass's own fields
    testValue(new Derived("a", "b"), want().add("W", "b").add("V", "a").done());
  }

  // -----------------------------------------------------------------------------------------
  // TestValues_InvalidInput
  // -----------------------------------------------------------------------------------------

  @Test
  void valuesInvalidInput() {
    assertThrows(
        QueryException.class,
        () -> Query.values(""),
        "expected Values() to return an error on invalid input");
  }

  // -----------------------------------------------------------------------------------------
  // TestValues_CustomEncodingSlice
  // -----------------------------------------------------------------------------------------

  /** A list of strings with a custom URL encoding. */
  static final class CustomEncodedStrings implements Encoder {

    private final List<String> items;

    /** The zero value: Go's nil slice. */
    CustomEncodedStrings() {
      this.items = Collections.emptyList();
    }

    CustomEncodedStrings(String... items) {
      this.items = Arrays.asList(items);
    }

    /**
     * Encodes using a key name of the form "{key}.N" where N increments with each value. A value of
     * "err" returns an error.
     */
    @Override
    public void encodeValues(String key, UrlValues values) throws QueryException {
      for (int i = 0; i < items.size(); i++) {
        String arg = items.get(i);
        if ("err".equals(arg)) {
          throw new QueryException("encoding error");
        }
        values.set(key + "." + i, arg);
      }
    }
  }

  static class SCustomStrings {
    @Url("v")
    public CustomEncodedStrings V;

    SCustomStrings() {}

    SCustomStrings(CustomEncodedStrings v) {
      V = v;
    }
  }

  static class SPtrCustomStrings {
    @Url("v")
    public Ptr<CustomEncodedStrings> V;

    SPtrCustomStrings() {}

    SPtrCustomStrings(Ptr<CustomEncodedStrings> v) {
      V = v;
    }
  }

  static class SInterfaceCustom {
    @Url("v")
    public Object V;

    SInterfaceCustom(Object v) {
      V = v;
    }
  }

  @Test
  void valuesCustomEncodingSlice() {
    testValue(new SCustomStrings(), want().done());
    testValue(
        new SCustomStrings(new CustomEncodedStrings("a", "b")),
        want().add("v.0", "a").add("v.1", "b").done());

    // pointers to custom encoded types
    testValue(new SPtrCustomStrings(), want().done());
    testValue(
        new SPtrCustomStrings(Ptr.of(new CustomEncodedStrings("a", "b"))),
        want().add("v.0", "a").add("v.1", "b").done());

    // custom encoded type held in an untyped field
    testValue(
        new SInterfaceCustom(new CustomEncodedStrings("a", "b")),
        want().add("v.0", "a").add("v.1", "b").done());
  }

  // -----------------------------------------------------------------------------------------
  // TestValues_CustomEncoding_Error
  // -----------------------------------------------------------------------------------------

  static class St {
    public CustomEncodedStrings V;

    St(CustomEncodedStrings v) {
      V = v;
    }
  }

  static class SStructField {
    public St S;

    SStructField(St s) {
      S = s;
    }
  }

  static class SEmbeddedSt {
    @Embedded public St st;

    SEmbeddedSt(St st) {
      this.st = st;
    }
  }

  /**
   * One of the few ways encoding will return an error is if a custom encoder returns one. Tests all
   * of the various ways that can happen.
   */
  @Test
  void valuesCustomEncodingError() {
    CustomEncodedStrings err = new CustomEncodedStrings("err");

    assertThrows(QueryException.class, () -> Query.values(new St(err)));
    // struct field
    assertThrows(QueryException.class, () -> Query.values(new SStructField(new St(err))));
    // embedded struct
    assertThrows(QueryException.class, () -> Query.values(new SEmbeddedSt(new St(err))));
  }

  // -----------------------------------------------------------------------------------------
  // TestValues_CustomEncodingInt
  // -----------------------------------------------------------------------------------------

  /** An int with a custom URL encoding, the Java equivalent of a Go value receiver. */
  static final class CustomEncodedInt implements Encoder, Zeroable {

    private final int value;

    CustomEncodedInt() {
      this(0);
    }

    CustomEncodedInt(int value) {
      this.value = value;
    }

    /** Encodes values with leading underscores. */
    @Override
    public void encodeValues(String key, UrlValues values) {
      values.set(key, "_" + value);
    }

    @Override
    public boolean isZero() {
      return value == 0;
    }
  }

  static class SCustomInt {
    @Url("v")
    public CustomEncodedInt V;

    SCustomInt() {}

    SCustomInt(CustomEncodedInt v) {
      V = v;
    }
  }

  static class SCustomIntOmitEmpty {
    @Url("v,omitempty")
    public CustomEncodedInt V;

    SCustomIntOmitEmpty(CustomEncodedInt v) {
      V = v;
    }
  }

  static class SPtrCustomInt {
    @Url("v")
    public Ptr<CustomEncodedInt> V;

    SPtrCustomInt() {}

    SPtrCustomInt(Ptr<CustomEncodedInt> v) {
      V = v;
    }
  }

  static class SPtrCustomIntOmitEmpty {
    @Url("v,omitempty")
    public Ptr<CustomEncodedInt> V;

    SPtrCustomIntOmitEmpty() {}

    SPtrCustomIntOmitEmpty(Ptr<CustomEncodedInt> v) {
      V = v;
    }
  }

  @Test
  void valuesCustomEncodingInt() {
    CustomEncodedInt zero = new CustomEncodedInt(0);
    CustomEncodedInt one = new CustomEncodedInt(1);

    testValue(new SCustomInt(), want().add("v", "_0").done());
    testValue(new SCustomIntOmitEmpty(zero), want().done());
    testValue(new SCustomInt(one), want().add("v", "_1").done());

    // pointers to custom encoded types
    testValue(new SPtrCustomInt(), want().add("v", "_0").done());
    testValue(new SPtrCustomIntOmitEmpty(), want().done());
    testValue(new SPtrCustomIntOmitEmpty(Ptr.of(zero)), want().add("v", "_0").done());
    testValue(new SPtrCustomInt(Ptr.of(one)), want().add("v", "_1").done());
  }

  // -----------------------------------------------------------------------------------------
  // TestValues_CustomEncodingPointer
  // -----------------------------------------------------------------------------------------

  /**
   * An int with a custom URL encoding defined on its pointer value: the Java equivalent of a Go
   * pointer receiver is {@link NilEncoder}, which only applies through a {@link Ptr} field. As a
   * bare field the type is encoded normally, through {@link Stringer}.
   */
  static final class CustomEncodedIntPtr implements NilEncoder, Zeroable, Stringer {

    private final int value;

    CustomEncodedIntPtr() {
      this(0);
    }

    CustomEncodedIntPtr(int value) {
      this.value = value;
    }

    /** Encodes values with leading underscores. */
    @Override
    public void encodeValues(String key, UrlValues values) {
      values.set(key, "_" + value);
    }

    /** Encodes a null pointer as "undefined". */
    @Override
    public void encodeNilValues(String key, UrlValues values) {
      values.set(key, "undefined");
    }

    @Override
    public boolean isZero() {
      return value == 0;
    }

    @Override
    public String string() {
      return Integer.toString(value);
    }
  }

  static class SCustomIntPtr {
    @Url("v")
    public CustomEncodedIntPtr V;

    SCustomIntPtr(CustomEncodedIntPtr v) {
      V = v;
    }
  }

  static class SCustomIntPtrOmitEmpty {
    @Url("v,omitempty")
    public CustomEncodedIntPtr V;

    SCustomIntPtrOmitEmpty(CustomEncodedIntPtr v) {
      V = v;
    }
  }

  static class SPtrCustomIntPtr {
    @Url("v")
    public Ptr<CustomEncodedIntPtr> V;

    SPtrCustomIntPtr() {}

    SPtrCustomIntPtr(Ptr<CustomEncodedIntPtr> v) {
      V = v;
    }
  }

  static class SPtrCustomIntPtrOmitEmpty {
    @Url("v,omitempty")
    public Ptr<CustomEncodedIntPtr> V;

    SPtrCustomIntPtrOmitEmpty() {}

    SPtrCustomIntPtrOmitEmpty(Ptr<CustomEncodedIntPtr> v) {
      V = v;
    }
  }

  /**
   * Tests behaviour when encoding is defined for a pointer of a custom type. The custom type is able
   * to encode values for null pointers.
   */
  @Test
  void valuesCustomEncodingPointer() {
    CustomEncodedIntPtr zero = new CustomEncodedIntPtr(0);
    CustomEncodedIntPtr one = new CustomEncodedIntPtr(1);

    // non-pointer values do not get the custom encoding
    testValue(new SCustomIntPtr(zero), want().add("v", "0").done());
    testValue(new SCustomIntPtrOmitEmpty(zero), want().done());
    testValue(new SCustomIntPtr(one), want().add("v", "1").done());

    // pointers to custom encoded types
    testValue(new SPtrCustomIntPtr(), want().add("v", "undefined").done());
    testValue(new SPtrCustomIntPtrOmitEmpty(), want().done());
    testValue(new SPtrCustomIntPtr(Ptr.of(zero)), want().add("v", "_0").done());
    testValue(new SPtrCustomIntPtrOmitEmpty(Ptr.of(zero)), want().add("v", "_0").done());
    testValue(new SPtrCustomIntPtr(Ptr.of(one)), want().add("v", "_1").done());
  }

  // -----------------------------------------------------------------------------------------
  // TestIsEmptyValue
  // -----------------------------------------------------------------------------------------

  /** An unknown type: never empty unless null. */
  static class Unknown {
    int i;
  }

  @Test
  void isEmptyValue() {
    Map<String, String> emptyMap = new HashMap<>();
    Map<String, String> fullMap = new HashMap<>();
    fullMap.put("a", "b");

    Object[][] tests = {
      // lists, arrays and maps
      {new ArrayList<Integer>(), true},
      {Collections.singletonList(0), false},
      {new int[0], true},
      {new int[3], false},
      {new int[] {1, 0, 0}, false},
      {emptyMap, true},
      {fullMap, false},

      // strings
      {"", true},
      {" ", false},
      {"a", false},

      // bool
      {true, false},
      {false, true},

      // ints of various types
      {0, true}, {1, false}, {-1, false},
      {(byte) 0, true}, {(byte) 1, false}, {(byte) -1, false},
      {(short) 0, true}, {(short) 1, false}, {(short) -1, false},
      {0L, true}, {1L, false}, {-1L, false},

      // floats
      {0f, true}, {0.0f, true}, {0.1f, false},
      {0d, true}, {0.0d, true}, {0.1d, false},

      // pointers
      {null, true},
      {Ptr.of(new int[0]), false},
      {Ptr.of("string"), false},

      // time
      {Query.ZERO_TIME, true},
      {Instant.now(), false},

      // unknown type - always false unless null, and nulls are always empty
      {new Unknown(), false},
    };

    for (Object[] tt : tests) {
      Object value = tt[0];
      boolean want = (Boolean) tt[1];
      boolean got = Query.isEmptyValue(value);
      assertEquals(
          want,
          got,
          () -> "isEmptyValue(" + describe(value) + ") returned " + !want + "; want " + want);
    }
  }

  // -----------------------------------------------------------------------------------------
  // TestParseTag
  // -----------------------------------------------------------------------------------------

  @Test
  void parseTag() {
    Query.Tag tag = Query.parseTag("field,foobar,foo");
    assertEquals("field", tag.name, "name");

    assertEquals(true, tag.opts.contains("foobar"), "contains(foobar)");
    assertEquals(true, tag.opts.contains("foo"), "contains(foo)");
    assertEquals(false, tag.opts.contains("bar"), "contains(bar)");
    assertEquals(false, tag.opts.contains("field"), "contains(field)");

    Query.Tag empty = Query.parseTag("");
    assertEquals("", empty.name, "empty name");
    assertEquals(false, empty.opts.contains(""), "empty options");
  }

  // -----------------------------------------------------------------------------------------
  // UrlValues.encode: the README example
  // -----------------------------------------------------------------------------------------

  static class Options {
    @Url("q")
    public String query;

    @Url("all")
    public boolean showAll;

    @Url("page")
    public int page;

    Options(String query, boolean showAll, int page) {
      this.query = query;
      this.showAll = showAll;
      this.page = page;
    }
  }

  @Test
  void encode() throws QueryException {
    UrlValues v = Query.values(new Options("foo", true, 2));
    assertEquals("all=true&page=2&q=foo", v.encode());

    UrlValues escaped = new UrlValues();
    escaped.add("a b", "c&d=e");
    escaped.add("emoji", "\uD83E\uDD51");
    assertEquals("a+b=c%26d%3De&emoji=%F0%9F%A5%91", escaped.encode());
  }
}

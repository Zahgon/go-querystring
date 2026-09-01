// Copyright 2013 The Go Authors. All rights reserved.
// Use of this source code is governed by a BSD-style
// license that can be found in the LICENSE file.

package io.github.goquerystring.query;

import java.nio.charset.StandardCharsets;
import java.util.ArrayList;
import java.util.Collections;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.TreeSet;

/**
 * A multi-map of query parameters: the port of Go's {@code net/url.Values}.
 *
 * <p>Keys keep insertion order for iteration, while {@link #encode()} sorts them, exactly like
 * Go's {@code Values.Encode}.
 */
public final class UrlValues {

  private final Map<String, List<String>> values = new LinkedHashMap<>();

  /** Creates an empty parameter map. */
  public UrlValues() {}

  /** Appends {@code value} to the list of values for {@code key} (Go: {@code Values.Add}). */
  public void add(String key, String value) {
    values.computeIfAbsent(key, k -> new ArrayList<>()).add(value);
  }

  /** Replaces any existing values for {@code key} with {@code value} (Go: {@code Values.Set}). */
  public void set(String key, String value) {
    List<String> list = new ArrayList<>(1);
    list.add(value);
    values.put(key, list);
  }

  /**
   * Returns the first value for {@code key}, or the empty string when absent (Go: {@code
   * Values.Get}).
   */
  public String get(String key) {
    List<String> list = values.get(key);
    return list == null || list.isEmpty() ? "" : list.get(0);
  }

  /** Returns all values for {@code key}, or an empty list when absent. */
  public List<String> getAll(String key) {
    List<String> list = values.get(key);
    return list == null ? Collections.emptyList() : Collections.unmodifiableList(list);
  }

  /** Returns whether {@code key} is present. */
  public boolean containsKey(String key) {
    return values.containsKey(key);
  }

  /** Removes {@code key} and all of its values (Go: {@code Values.Del}). */
  public void remove(String key) {
    values.remove(key);
  }

  /** Returns the keys in insertion order. */
  public Set<String> keys() {
    return Collections.unmodifiableSet(values.keySet());
  }

  /** Returns the number of distinct keys. */
  public int size() {
    return values.size();
  }

  /** Returns whether no parameters are present. */
  public boolean isEmpty() {
    return values.isEmpty();
  }

  /** Returns an unmodifiable snapshot of the underlying map. */
  public Map<String, List<String>> asMap() {
    Map<String, List<String>> copy = new LinkedHashMap<>();
    values.forEach((k, v) -> copy.put(k, Collections.unmodifiableList(new ArrayList<>(v))));
    return Collections.unmodifiableMap(copy);
  }

  /**
   * Encodes the values into "URL encoded" form ({@code "bar=baz&foo=quux"}) sorted by key (Go:
   * {@code Values.Encode}).
   */
  public String encode() {
    if (values.isEmpty()) {
      return "";
    }
    StringBuilder buf = new StringBuilder();
    for (String key : new TreeSet<>(values.keySet())) {
      String keyEscaped = queryEscape(key);
      for (String value : values.get(key)) {
        if (buf.length() > 0) {
          buf.append('&');
        }
        buf.append(keyEscaped).append('=').append(queryEscape(value));
      }
    }
    return buf.toString();
  }

  /**
   * Escapes a string so it can be safely placed inside a URL query, mirroring Go's {@code
   * url.QueryEscape}: spaces become {@code '+'}, {@code A-Za-z0-9} and {@code -_.~} are kept
   * verbatim, everything else is percent-encoded per UTF-8 byte with upper-case hex.
   */
  public static String queryEscape(String s) {
    StringBuilder out = new StringBuilder(s.length());
    for (byte b : s.getBytes(StandardCharsets.UTF_8)) {
      int c = b & 0xFF;
      if ((c >= 'A' && c <= 'Z')
          || (c >= 'a' && c <= 'z')
          || (c >= '0' && c <= '9')
          || c == '-'
          || c == '_'
          || c == '.'
          || c == '~') {
        out.append((char) c);
      } else if (c == ' ') {
        out.append('+');
      } else {
        out.append('%').append(Character.toUpperCase(Character.forDigit(c >> 4, 16)))
            .append(Character.toUpperCase(Character.forDigit(c & 0xF, 16)));
      }
    }
    return out.toString();
  }

  @Override
  public boolean equals(Object o) {
    return o instanceof UrlValues && values.equals(((UrlValues) o).values);
  }

  @Override
  public int hashCode() {
    return values.hashCode();
  }

  @Override
  public String toString() {
    return values.toString();
  }
}

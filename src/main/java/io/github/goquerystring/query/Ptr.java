// Copyright 2013 The Go Authors. All rights reserved.
// Use of this source code is governed by a BSD-style
// license that can be found in the LICENSE file.

package io.github.goquerystring.query;

import java.util.Objects;

/**
 * An explicit Go pointer.
 *
 * <p>Java references already model {@code *T} for object types, so most Go pointer fields port to a
 * plain nullable field. {@code Ptr} exists for the cases Java cannot otherwise express:
 *
 * <ul>
 *   <li>{@code **string} &rarr; {@code Ptr<Ptr<String>>}
 *   <li>{@code *[]string} &rarr; {@code Ptr<List<String>>} (a {@code null} list field means a nil
 *       slice, which Go skips; a {@code null} {@code Ptr} field means a nil pointer, which Go
 *       encodes as an empty value)
 *   <li>{@code []*string} &rarr; {@code List<Ptr<String>>}
 * </ul>
 *
 * <p>A {@code null} field of type {@code Ptr<T>} is a nil pointer. A non-null {@code Ptr} always
 * points at a non-null value, so it is never empty for {@code omitempty} purposes -- matching Go,
 * where a non-nil pointer to an empty value is still encoded.
 *
 * @param <T> the pointee type
 */
public final class Ptr<T> {

  private final T value;

  private Ptr(T value) {
    this.value = value;
  }

  /**
   * Returns a pointer to {@code value}.
   *
   * @throws NullPointerException if {@code value} is null; use a {@code null} field for a nil
   *     pointer
   */
  public static <T> Ptr<T> of(T value) {
    return new Ptr<>(Objects.requireNonNull(value, "Ptr.of(null): use a null field for a nil pointer"));
  }

  /** Returns the pointed-to value, never null. */
  public T get() {
    return value;
  }

  @Override
  public boolean equals(Object o) {
    return o instanceof Ptr && Objects.equals(value, ((Ptr<?>) o).value);
  }

  @Override
  public int hashCode() {
    return Objects.hashCode(value);
  }

  @Override
  public String toString() {
    return String.valueOf(value);
  }
}

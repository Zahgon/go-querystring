// Copyright 2013 The Go Authors. All rights reserved.
// Use of this source code is governed by a BSD-style
// license that can be found in the LICENSE file.

package io.github.goquerystring.query;

/**
 * Marks a type as a single, self-describing value rather than a nested object.
 *
 * <p>Port of Go's {@code fmt.Stringer}: {@code encode.go} falls back to {@code fmt.Sprint(v)} for
 * anything that is not a slice, array, time or struct, and {@code fmt.Sprint} honours a {@code
 * String()} method. Go can tell a named scalar type (kind {@code int}, {@code string}, ...) from a
 * struct by its reflect kind; Java cannot, because both are just classes. Implementing this
 * interface says "encode me as one parameter value" instead of recursing into my fields.
 *
 * <pre>{@code
 * // Go:   type UserID int   (kind int -> encoded as a single value)
 * // Java:
 * public final class UserId implements Stringer {
 *   private final int id;
 *   public UserId(int id) { this.id = id; }
 *   public String string() { return Integer.toString(id); }
 * }
 * }</pre>
 */
public interface Stringer {

  /** Returns the string representation used as the parameter value. */
  String string();
}

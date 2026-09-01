// Copyright 2013 The Go Authors. All rights reserved.
// Use of this source code is governed by a BSD-style
// license that can be found in the LICENSE file.

package io.github.goquerystring.query;

/**
 * Lets a type declare when it should be considered empty for the {@code omitempty} option.
 *
 * <p>Port of the anonymous {@code zeroable} interface used by Go's {@code isEmptyValue}:
 *
 * <pre>{@code
 * type zeroable interface {
 *     IsZero() bool
 * }
 * }</pre>
 */
public interface Zeroable {

  /** Returns {@code true} when this value counts as empty. */
  boolean isZero();
}

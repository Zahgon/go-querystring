// Copyright 2013 The Go Authors. All rights reserved.
// Use of this source code is governed by a BSD-style
// license that can be found in the LICENSE file.

package io.github.goquerystring.query;

/**
 * Thrown when a value cannot be encoded into URL query parameters.
 *
 * <p>Port of the {@code error} values returned by Go's {@code query.Values} and by user supplied
 * {@code Encoder.EncodeValues} implementations.
 */
public class QueryException extends Exception {

  private static final long serialVersionUID = 1L;

  public QueryException(String message) {
    super(message);
  }

  public QueryException(String message, Throwable cause) {
    super(message, cause);
  }
}

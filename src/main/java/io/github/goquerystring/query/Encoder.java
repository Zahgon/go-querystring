// Copyright 2013 The Go Authors. All rights reserved.
// Use of this source code is governed by a BSD-style
// license that can be found in the LICENSE file.

package io.github.goquerystring.query;

/**
 * Implemented by any type that wishes to encode itself into URL values in a non-standard way.
 *
 * <p>Port of Go's {@code query.Encoder} interface:
 *
 * <pre>{@code
 * type Encoder interface {
 *     EncodeValues(key string, v *url.Values) error
 * }
 * }</pre>
 */
public interface Encoder {

  /**
   * Encodes this value into {@code values} under (or derived from) {@code key}.
   *
   * @param key the parameter name computed for the field holding this value
   * @param values the accumulating parameter map, mutated in place
   * @throws QueryException to abort encoding, mirroring a non-nil {@code error} return in Go
   */
  void encodeValues(String key, UrlValues values) throws QueryException;
}

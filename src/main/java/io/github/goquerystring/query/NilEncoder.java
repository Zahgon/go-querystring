// Copyright 2013 The Go Authors. All rights reserved.
// Use of this source code is governed by a BSD-style
// license that can be found in the LICENSE file.

package io.github.goquerystring.query;

/**
 * An {@link Encoder} that can also encode the {@code null} (Go: nil) case.
 *
 * <p>In Go, an {@code EncodeValues} method declared on a pointer receiver may be invoked with a nil
 * receiver, letting a type encode "no value" itself:
 *
 * <pre>{@code
 * func (m *customEncodedIntPtr) EncodeValues(key string, v *url.Values) error {
 *     if m == nil {
 *         v.Set(key, "undefined")
 *     }
 *     ...
 * }
 * }</pre>
 *
 * <p>Java has no nil receivers, so a type opts into that behaviour by implementing this interface.
 * When a field is {@code null} and its declared type (or the type argument of a {@link Ptr}) is a
 * {@code NilEncoder}, the port instantiates the type's no-argument constructor and calls {@link
 * #encodeNilValues(String, UrlValues)} instead of {@link Encoder#encodeValues(String, UrlValues)}.
 */
public interface NilEncoder extends Encoder {

  /**
   * Encodes the absence of a value.
   *
   * @param key the parameter name computed for the field
   * @param values the accumulating parameter map, mutated in place
   * @throws QueryException to abort encoding
   */
  void encodeNilValues(String key, UrlValues values) throws QueryException;
}

// Copyright 2013 The Go Authors. All rights reserved.
// Use of this source code is governed by a BSD-style
// license that can be found in the LICENSE file.

package io.github.goquerystring.query;

import java.lang.annotation.Documented;
import java.lang.annotation.ElementType;
import java.lang.annotation.Retention;
import java.lang.annotation.RetentionPolicy;
import java.lang.annotation.Target;

/**
 * Java equivalent of Go's {@code `del:"..."`} struct tag: the delimiter used to join a collection
 * into a single parameter value.
 *
 * <pre>{@code
 * // Encode a list of booleans as ints ("1"/"0") separated by "!".
 * @Url(",int") @Del("!") public List<Boolean> field;
 * }</pre>
 *
 * <p>Ignored when {@code comma}, {@code space}, {@code semicolon} or {@code brackets} is present in
 * the {@link Url} tag.
 */
@Documented
@Retention(RetentionPolicy.RUNTIME)
@Target(ElementType.FIELD)
public @interface Del {

  /** The delimiter string. */
  String value();
}

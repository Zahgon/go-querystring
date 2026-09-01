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
 * Java equivalent of Go's {@code `url:"..."`} struct tag.
 *
 * <p>The value is the parameter name, followed by an optional comma separated list of options,
 * exactly as in Go:
 *
 * <pre>{@code
 * @Url("-")                 // field is ignored
 * @Url("myName")            // field appears as parameter "myName"
 * @Url("myName,omitempty")  // ... and is omitted when empty
 * @Url(",omitempty")        // parameter keeps the field name, omitted when empty
 * }</pre>
 *
 * <p>Recognised options: {@code omitempty}, {@code int}, {@code unix}, {@code unixmilli}, {@code
 * unixnano}, {@code comma}, {@code space}, {@code semicolon}, {@code brackets}, {@code numbered}.
 */
@Documented
@Retention(RetentionPolicy.RUNTIME)
@Target(ElementType.FIELD)
public @interface Url {

  /** The raw tag value, e.g. {@code "myName,omitempty"}. */
  String value() default "";
}

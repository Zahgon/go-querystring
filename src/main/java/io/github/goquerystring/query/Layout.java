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
 * Java equivalent of Go's {@code `layout:"..."`} struct tag for date/time fields.
 *
 * <p>Unlike Go, the value is a {@link java.time.format.DateTimeFormatter} pattern rather than a Go
 * reference-time layout:
 *
 * <pre>{@code
 * // Go:   Field time.Time `layout:"2006-01-02"`
 * // Java: @Layout("yyyy-MM-dd") public Instant field;
 * }</pre>
 */
@Documented
@Retention(RetentionPolicy.RUNTIME)
@Target(ElementType.FIELD)
public @interface Layout {

  /** A {@link java.time.format.DateTimeFormatter} pattern. */
  String value();
}

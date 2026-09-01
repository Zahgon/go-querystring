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
 * Marks a field as the Java equivalent of a Go anonymous (embedded) struct field.
 *
 * <p>The annotated field's own name is not used: its inner fields are encoded as if they were
 * fields of the outer object, breadth-first (after all of the outer object's own fields), matching
 * Go's embedding rules. Giving the field a name via {@link Url} makes it a normal named field
 * again, exactly as in Go.
 *
 * <p>An annotated field is traversed even when it is not {@code public}, mirroring Go's handling of
 * anonymous fields of unexported types. Java superclass fields are treated as embedded fields
 * automatically and do not need this annotation.
 */
@Documented
@Retention(RetentionPolicy.RUNTIME)
@Target(ElementType.FIELD)
public @interface Embedded {}

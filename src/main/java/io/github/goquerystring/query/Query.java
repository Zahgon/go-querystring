// Copyright 2013 The Go Authors. All rights reserved.
// Use of this source code is governed by a BSD-style
// license that can be found in the LICENSE file.

package io.github.goquerystring.query;

import java.lang.reflect.Array;
import java.lang.reflect.Constructor;
import java.lang.reflect.Field;
import java.lang.reflect.Modifier;
import java.lang.reflect.ParameterizedType;
import java.lang.reflect.Type;
import java.time.Instant;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.time.OffsetDateTime;
import java.time.ZoneOffset;
import java.time.ZonedDateTime;
import java.time.format.DateTimeFormatter;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collection;
import java.util.Collections;
import java.util.Date;
import java.util.List;
import java.util.Map;

/**
 * Encodes objects into URL query parameters.
 *
 * <p>Java port of {@code github.com/google/go-querystring} (package {@code query}). As a simple
 * example:
 *
 * <pre>{@code
 * public class Options {
 *   @Url("q")    public String query;
 *   @Url("all")  public boolean showAll;
 *   @Url("page") public int page;
 * }
 *
 * Options opt = new Options();
 * opt.query = "foo";
 * opt.showAll = true;
 * opt.page = 2;
 * UrlValues v = Query.values(opt);
 * System.out.print(v.encode()); // will output: "all=true&page=2&q=foo"
 * }</pre>
 *
 * <p>The exact mapping between Java values and {@link UrlValues} is described in the documentation
 * for {@link #values(Object)}.
 */
public final class Query {

  /**
   * The Java equivalent of Go's zero {@code time.Time}: January 1 of year 1, 00:00:00 UTC. A
   * temporal field equal to this instant is treated as empty by {@code omitempty}, mirroring {@code
   * time.Time.IsZero()}.
   */
  public static final Instant ZERO_TIME =
      LocalDateTime.of(1, 1, 1, 0, 0, 0).toInstant(ZoneOffset.UTC);

  /** Go's {@code time.RFC3339}: {@code "2006-01-02T15:04:05Z07:00"}. */
  private static final DateTimeFormatter RFC3339 =
      DateTimeFormatter.ofPattern("yyyy-MM-dd'T'HH:mm:ssXXX");

  private Query() {}

  /**
   * Returns the {@link UrlValues} encoding of {@code v}.
   *
   * <p>{@code values} expects to be passed an object, and traverses it recursively using the
   * following encoding rules.
   *
   * <p>Each public field is encoded as a URL parameter unless
   *
   * <ul>
   *   <li>the field's {@link Url} tag is {@code "-"}, or
   *   <li>the field is empty and its tag specifies the {@code "omitempty"} option
   * </ul>
   *
   * <p>The empty values are {@code false}, {@code 0}, {@code null}, any array, collection, map or
   * string of length zero, a temporal value equal to {@link #ZERO_TIME}, and any type that
   * implements {@link Zeroable} and returns {@code true} from {@link Zeroable#isZero()}.
   *
   * <p>The URL parameter name defaults to the field name but can be specified in the field's {@link
   * Url} annotation. The annotation value is the key name, followed by an optional comma and
   * options. For example:
   *
   * <pre>{@code
   * @Url("-")                // Field is ignored by this package.
   * @Url("myName")           // Field appears as URL parameter "myName".
   * @Url("myName,omitempty") // ... and the field is omitted if its value is empty.
   * @Url(",omitempty")       // Field appears as URL parameter "field" (the default),
   *                          // but is skipped if empty.  Note the leading comma.
   * }</pre>
   *
   * <p>For encoding individual field values, the following type-dependent rules apply:
   *
   * <p>Boolean values default to encoding as the strings {@code "true"} or {@code "false"}.
   * Including the {@code "int"} option signals that the field should be encoded as the strings
   * {@code "1"} or {@code "0"}.
   *
   * <p>Temporal values ({@link Instant}, {@link OffsetDateTime}, {@link ZonedDateTime}, {@link
   * LocalDateTime}, {@link LocalDate} and {@link Date}) default to encoding as RFC3339 timestamps.
   * Including the {@code "unix"} option signals that the field should be encoded as a Unix time.
   * The {@code "unixmilli"} and {@code "unixnano"} options will encode the number of milliseconds
   * and nanoseconds, respectively, since January 1, 1970. Including the {@link Layout} annotation
   * (separate from the {@link Url} annotation) will use its value as a {@link DateTimeFormatter}
   * pattern. For example:
   *
   * <pre>{@code
   * // Encode an Instant as YYYY-MM-DD
   * @Layout("yyyy-MM-dd") public Instant field;
   * }</pre>
   *
   * <p>Collection and array values default to encoding as multiple URL values of the same name.
   * Including the {@code "comma"} option signals that the field should be encoded as a single
   * comma-delimited value. Including the {@code "space"} option similarly encodes the value as a
   * single space-delimited string. Including the {@code "semicolon"} option will encode the value
   * as a semicolon-delimited string. Including the {@code "brackets"} option signals that the
   * multiple URL values should have {@code "[]"} appended to the value name. {@code "numbered"}
   * will append a number to the end of each incidence of the value name, example:
   * {@code name0=value0&name1=value1}, etc. Including the {@link Del} annotation (separate from the
   * {@link Url} annotation) will use its value as the delimiter. For example:
   *
   * <pre>{@code
   * // Encode a list of booleans as ints ("1" for true, "0" for false),
   * // separated by exclamation points "!".
   * @Url(",int") @Del("!") public List<Boolean> field;
   * }</pre>
   *
   * <p>Fields annotated with {@link Embedded}, and the fields a class inherits from its superclass,
   * are encoded as if their inner public fields were fields of the outer object -- the Java
   * equivalent of Go's anonymous struct fields. An {@link Embedded} field with a name given in its
   * {@link Url} annotation is treated as having that name, rather than being embedded.
   *
   * <p>Non-null {@link Ptr} values are encoded as the value pointed to.
   *
   * <p>Nested objects have their fields processed recursively and are encoded including parent
   * fields in value names for scoping. For example,
   *
   * <pre>{@code
   * "user[name]=acme&user[addr][postcode]=1234&user[addr][city]=SFO"
   * }</pre>
   *
   * <p>All other values are encoded using their default string representation.
   *
   * <p>Multiple fields that encode to the same URL parameter name will be included as multiple URL
   * values of the same name.
   *
   * @param v the object to encode; may be {@code null}
   * @return the encoded parameters, never {@code null}
   * @throws QueryException if {@code v} is not an object with encodable fields, or if a custom
   *     {@link Encoder} fails
   */
  public static UrlValues values(Object v) throws QueryException {
    UrlValues values = new UrlValues();

    if (v == null) {
      return values;
    }

    Object val = v;
    while (val instanceof Ptr) {
      val = ((Ptr<?>) val).get();
      if (val == null) {
        return values;
      }
    }

    if (!isStructLike(val)) {
      throw new QueryException("query: Values() expects struct input. Got " + kindOf(val));
    }

    reflectValue(values, val, val.getClass(), "");
    return values;
  }

  /**
   * Populates {@code values} from the fields of {@code val} declared by {@code type}. Embedded
   * values are followed recursively (using the rules defined in {@link #values(Object)})
   * breadth-first.
   */
  private static void reflectValue(UrlValues values, Object val, Class<?> type, String scope)
      throws QueryException {
    List<Target> embedded = new ArrayList<>();

    for (Field sf : encodableFields(type)) {
      Object sv = readField(sf, val);

      Url url = sf.getAnnotation(Url.class);
      String tag = url == null ? "" : url.value();
      if ("-".equals(tag)) {
        continue;
      }
      Tag parsed = parseTag(tag);
      String name = parsed.name;
      TagOptions opts = parsed.opts;
      boolean anonymous = sf.isAnnotationPresent(Embedded.class);

      if (name.isEmpty()) {
        if (anonymous) {
          Object inner = indirect(sv);
          if (inner != null && isStructLike(inner)) {
            // save embedded value for later processing
            embedded.add(new Target(inner, inner.getClass()));
            continue;
          }
        }

        name = sf.getName();
      }

      if (!scope.isEmpty()) {
        name = scope + "[" + name + "]";
      }

      if (opts.contains("omitempty") && isEmptyValue(sv)) {
        continue;
      }

      // a value implementing Encoder encodes itself; a NilEncoder models a Go method declared on a
      // pointer receiver, so it only applies through a Ptr field
      Object pointee = sv instanceof Ptr ? ((Ptr<?>) sv).get() : sv;
      boolean declaredPtr = Ptr.class.isAssignableFrom(sf.getType());
      if (pointee instanceof Encoder) {
        boolean pointerReceiverOnly =
            pointee instanceof NilEncoder && !declaredPtr && !(sv instanceof Ptr);
        if (!pointerReceiverOnly) {
          ((Encoder) pointee).encodeValues(name, values);
          continue;
        }
      } else if (pointee == null) {
        // a nil pointer whose type defines a custom encoding: a NilEncoder encodes the nil case
        // itself, any other Encoder is encoded as its zero value
        Class<?> target = targetType(sf);
        if (target != null && NilEncoder.class.isAssignableFrom(target)) {
          ((NilEncoder) newZeroValue(target, sf)).encodeNilValues(name, values);
          continue;
        }
        if (target != null && Encoder.class.isAssignableFrom(target)) {
          ((Encoder) newZeroValue(target, sf)).encodeValues(name, values);
          continue;
        }
      }

      // recursively dereference pointers. break on nil pointers
      Object fv = indirect(sv);

      if (fv == null && isCollectionType(sf.getType())) {
        // a null collection field is Go's nil slice: skipped like an empty one
        continue;
      }

      if (isCollection(fv)) {
        int len = lengthOf(fv);
        if (len == 0) {
          // skip if collection or array is empty
          continue;
        }

        String del = "";
        if (opts.contains("comma")) {
          del = ",";
        } else if (opts.contains("space")) {
          del = " ";
        } else if (opts.contains("semicolon")) {
          del = ";";
        } else if (opts.contains("brackets")) {
          name = name + "[]";
        } else {
          Del d = sf.getAnnotation(Del.class);
          del = d == null ? "" : d.value();
        }

        if (!del.isEmpty()) {
          StringBuilder s = new StringBuilder();
          boolean first = true;
          for (Object element : elementsOf(fv)) {
            if (first) {
              first = false;
            } else {
              s.append(del);
            }
            s.append(valueString(element, opts, sf));
          }
          values.add(name, s.toString());
        } else {
          int i = 0;
          for (Object element : elementsOf(fv)) {
            String k = name;
            if (opts.contains("numbered")) {
              k = name + i;
            }
            values.add(k, valueString(element, opts, sf));
            i++;
          }
        }
        continue;
      }

      if (isTimeValue(fv)) {
        values.add(name, valueString(fv, opts, sf));
        continue;
      }

      if (isStructLike(fv)) {
        reflectValue(values, fv, fv.getClass(), name);
        continue;
      }

      values.add(name, valueString(fv, opts, sf));
    }

    // a superclass is Java's implicit anonymous field: encoded after this class's own fields
    Class<?> superclass = type.getSuperclass();
    if (superclass != null && superclass != Object.class) {
      embedded.add(new Target(val, superclass));
    }

    for (Target f : embedded) {
      reflectValue(values, f.value, f.type, scope);
    }
  }

  /** Returns the string representation of a value. */
  static String valueString(Object value, TagOptions opts, Field sf) {
    Object v = indirect(value);
    if (v == null) {
      return "";
    }

    if (v instanceof Boolean && opts.contains("int")) {
      return ((Boolean) v) ? "1" : "0";
    }

    if (isTimeValue(v)) {
      OffsetDateTime t = toOffsetDateTime(v);
      Instant instant = t.toInstant();
      if (opts.contains("unix")) {
        return Long.toString(instant.getEpochSecond());
      }
      if (opts.contains("unixmilli")) {
        return Long.toString(instant.toEpochMilli());
      }
      if (opts.contains("unixnano")) {
        return Long.toString(instant.getEpochSecond() * 1_000_000_000L + instant.getNano());
      }
      Layout layout = sf == null ? null : sf.getAnnotation(Layout.class);
      if (layout != null && !layout.value().isEmpty()) {
        return DateTimeFormatter.ofPattern(layout.value()).format(t);
      }
      return RFC3339.format(t);
    }

    if (v instanceof Stringer) {
      return ((Stringer) v).string();
    }

    // Go prints floats in their shortest round-trip form: 0 rather than 0.0, 1e+21 rather than
    // 1.0E21
    if (v instanceof Float) {
      return formatFloat((Float) v, true);
    }
    if (v instanceof Double) {
      return formatFloat((Double) v, false);
    }

    return String.valueOf(v);
  }

  /**
   * Checks if a value should be considered empty for the purposes of omitting fields with the
   * {@code "omitempty"} option.
   */
  static boolean isEmptyValue(Object v) {
    if (v == null) {
      return true;
    }
    if (v instanceof CharSequence) {
      return ((CharSequence) v).length() == 0;
    }
    if (v instanceof Boolean) {
      return !((Boolean) v);
    }
    if (v instanceof Character) {
      return ((Character) v) == '\0';
    }
    if (v instanceof Number) {
      return ((Number) v).doubleValue() == 0;
    }
    if (v instanceof Collection) {
      return ((Collection<?>) v).isEmpty();
    }
    if (v instanceof Map) {
      return ((Map<?, ?>) v).isEmpty();
    }
    if (v.getClass().isArray()) {
      return Array.getLength(v) == 0;
    }
    if (isTimeValue(v)) {
      return toOffsetDateTime(v).toInstant().equals(ZERO_TIME);
    }
    if (v instanceof Zeroable) {
      return ((Zeroable) v).isZero();
    }
    return false;
  }

  /**
   * The parsed form of a field's {@link Url} annotation: a name and its comma-separated options.
   */
  static final class Tag {
    final String name;
    final TagOptions opts;

    Tag(String name, TagOptions opts) {
      this.name = name;
      this.opts = opts;
    }
  }

  /**
   * The list of options following a comma in a field's {@link Url} annotation, or the empty list.
   * It does not include the leading comma.
   */
  public static final class TagOptions {

    private final List<String> options;

    TagOptions(List<String> options) {
      this.options = options;
    }

    /** Checks whether these options contain the specified option. */
    public boolean contains(String option) {
      for (String s : options) {
        if (s.equals(option)) {
          return true;
        }
      }
      return false;
    }

    @Override
    public String toString() {
      return options.toString();
    }
  }

  /** Splits a field's {@link Url} annotation value into its name and comma-separated options. */
  static Tag parseTag(String tag) {
    String[] s = tag.split(",", -1);
    List<String> opts =
        s.length > 1
            ? new ArrayList<>(Arrays.asList(s).subList(1, s.length))
            : Collections.<String>emptyList();
    return new Tag(s[0], new TagOptions(opts));
  }

  // ---------------------------------------------------------------------------------------------
  // reflection helpers
  // ---------------------------------------------------------------------------------------------

  /** An object paired with the class whose declared fields should be encoded. */
  private static final class Target {
    final Object value;
    final Class<?> type;

    Target(Object value, Class<?> type) {
      this.value = value;
      this.type = type;
    }
  }

  /**
   * Returns the fields of {@code type} that take part in encoding: its public declared fields, plus
   * any field marked {@link Embedded} (the Java equivalent of Go's unexported anonymous fields,
   * which are also traversed).
   */
  private static List<Field> encodableFields(Class<?> type) {
    List<Field> fields = new ArrayList<>();
    for (Field f : type.getDeclaredFields()) {
      int mods = f.getModifiers();
      if (Modifier.isStatic(mods) || f.isSynthetic()) {
        continue;
      }
      if (!Modifier.isPublic(mods) && !f.isAnnotationPresent(Embedded.class)) {
        // unexported
        continue;
      }
      try {
        f.setAccessible(true);
      } catch (RuntimeException ignored) {
        // fall through: the read below reports the failure with the field name
      }
      fields.add(f);
    }
    return fields;
  }

  private static Object readField(Field f, Object owner) throws QueryException {
    try {
      return f.get(owner);
    } catch (IllegalAccessException e) {
      throw new QueryException(
          "query: cannot read field " + f.getDeclaringClass().getName() + "." + f.getName(), e);
    }
  }

  /**
   * Returns the type a null field points at: the type argument of a {@link Ptr} field, or the
   * declared field type. Returns null if a {@code Ptr}'s type argument cannot be resolved.
   */
  private static Class<?> targetType(Field sf) {
    Class<?> declared = sf.getType();
    if (!Ptr.class.isAssignableFrom(declared)) {
      return declared;
    }
    Type generic = sf.getGenericType();
    if (generic instanceof ParameterizedType) {
      Type arg = ((ParameterizedType) generic).getActualTypeArguments()[0];
      if (arg instanceof Class) {
        return (Class<?>) arg;
      }
      if (arg instanceof ParameterizedType) {
        Type raw = ((ParameterizedType) arg).getRawType();
        if (raw instanceof Class) {
          return (Class<?>) raw;
        }
      }
    }
    return null;
  }

  /** Creates the zero value of a custom encoded type, mirroring Go's {@code reflect.New}. */
  private static Object newZeroValue(Class<?> type, Field sf) throws QueryException {
    try {
      Constructor<?> ctor = type.getDeclaredConstructor();
      ctor.setAccessible(true);
      return ctor.newInstance();
    } catch (ReflectiveOperationException | RuntimeException e) {
      throw new QueryException(
          "query: cannot create the zero value of "
              + type.getName()
              + " for null field "
              + sf.getDeclaringClass().getName()
              + "."
              + sf.getName()
              + ": a no-argument constructor is required to encode a null value",
          e);
    }
  }

  /** Recursively dereferences {@link Ptr} values. Returns null for a nil pointer. */
  private static Object indirect(Object v) {
    Object out = v;
    while (out instanceof Ptr) {
      out = ((Ptr<?>) out).get();
    }
    return out;
  }

  private static boolean isCollectionType(Class<?> type) {
    return Collection.class.isAssignableFrom(type) || type.isArray();
  }

  private static boolean isCollection(Object v) {
    return v != null && (v instanceof Collection || v.getClass().isArray());
  }

  private static int lengthOf(Object v) {
    return v instanceof Collection ? ((Collection<?>) v).size() : Array.getLength(v);
  }

  private static Iterable<Object> elementsOf(Object v) {
    if (v instanceof Collection) {
      @SuppressWarnings("unchecked")
      Collection<Object> c = (Collection<Object>) v;
      return c;
    }
    int len = Array.getLength(v);
    List<Object> out = new ArrayList<>(len);
    for (int i = 0; i < len; i++) {
      out.add(Array.get(v, i));
    }
    return out;
  }

  private static boolean isTimeValue(Object v) {
    return v instanceof Instant
        || v instanceof OffsetDateTime
        || v instanceof ZonedDateTime
        || v instanceof LocalDateTime
        || v instanceof LocalDate
        || v instanceof Date;
  }

  private static OffsetDateTime toOffsetDateTime(Object v) {
    if (v instanceof OffsetDateTime) {
      return (OffsetDateTime) v;
    }
    if (v instanceof ZonedDateTime) {
      return ((ZonedDateTime) v).toOffsetDateTime();
    }
    if (v instanceof Instant) {
      return ((Instant) v).atOffset(ZoneOffset.UTC);
    }
    if (v instanceof LocalDateTime) {
      return ((LocalDateTime) v).atOffset(ZoneOffset.UTC);
    }
    if (v instanceof LocalDate) {
      return ((LocalDate) v).atStartOfDay().atOffset(ZoneOffset.UTC);
    }
    return ((Date) v).toInstant().atOffset(ZoneOffset.UTC);
  }

  /**
   * Reports whether a value should have its fields walked recursively -- the Java equivalent of Go's
   * {@code reflect.Struct} kind. Scalars, collections, temporal values, {@link Stringer}s and JDK
   * types are leaves.
   */
  private static boolean isStructLike(Object v) {
    if (v == null) {
      return false;
    }
    Class<?> c = v.getClass();
    if (c.isArray()
        || c.isEnum()
        || v instanceof Ptr
        || v instanceof CharSequence
        || v instanceof Character
        || v instanceof Number
        || v instanceof Boolean
        || v instanceof Enum
        || v instanceof Collection
        || v instanceof Map
        || v instanceof Stringer
        || isTimeValue(v)) {
      return false;
    }
    String n = c.getName();
    return !n.startsWith("java.")
        && !n.startsWith("javax.")
        && !n.startsWith("jdk.")
        && !n.startsWith("sun.")
        && !n.startsWith("com.sun.");
  }

  /** Returns a Go-style kind name, used for the "expects struct input" error message. */
  private static String kindOf(Object v) {
    if (v instanceof String) {
      return "string";
    }
    if (v instanceof Boolean) {
      return "bool";
    }
    if (v instanceof Integer) {
      return "int";
    }
    if (v instanceof Long) {
      return "int64";
    }
    if (v instanceof Short) {
      return "int16";
    }
    if (v instanceof Byte) {
      return "int8";
    }
    if (v instanceof Float) {
      return "float32";
    }
    if (v instanceof Double) {
      return "float64";
    }
    if (v instanceof Collection) {
      return "slice";
    }
    if (v instanceof Map) {
      return "map";
    }
    if (v.getClass().isArray()) {
      return "array";
    }
    return v.getClass().getSimpleName();
  }

  // ---------------------------------------------------------------------------------------------
  // float formatting
  // ---------------------------------------------------------------------------------------------

  /**
   * Formats a float the way Go's {@code fmt.Sprint} does: the shortest representation that
   * round-trips, without Java's trailing {@code ".0"}, switching to exponent form when the decimal
   * exponent is below -4 or at least 21.
   */
  private static String formatFloat(double value, boolean isFloat32) {
    if (Double.isNaN(value)) {
      return "NaN";
    }
    if (Double.isInfinite(value)) {
      return value > 0 ? "+Inf" : "-Inf";
    }

    String repr = isFloat32 ? Float.toString((float) value) : Double.toString(value);
    boolean negative = repr.startsWith("-");
    if (negative) {
      repr = repr.substring(1);
    }

    String mantissa = repr;
    int exponent = 0;
    int e = repr.indexOf('E');
    if (e >= 0) {
      mantissa = repr.substring(0, e);
      exponent = Integer.parseInt(repr.substring(e + 1));
    }

    int dot = mantissa.indexOf('.');
    String intPart = dot < 0 ? mantissa : mantissa.substring(0, dot);
    String fracPart = dot < 0 ? "" : mantissa.substring(dot + 1);
    String digits = intPart + fracPart;

    // position of the decimal point within digits
    int point = intPart.length() + exponent;

    int lead = 0;
    while (lead < digits.length() - 1 && digits.charAt(lead) == '0') {
      lead++;
      point--;
    }
    digits = digits.substring(lead);

    int end = digits.length();
    while (end > 1 && digits.charAt(end - 1) == '0') {
      end--;
    }
    digits = digits.substring(0, end);

    if ("0".equals(digits)) {
      return negative ? "-0" : "0";
    }

    StringBuilder out = new StringBuilder();
    if (negative) {
      out.append('-');
    }
    int exp = point - 1;
    if (exp < -4 || exp >= 21) {
      out.append(digits.charAt(0));
      if (digits.length() > 1) {
        out.append('.').append(digits, 1, digits.length());
      }
      out.append('e').append(exp < 0 ? '-' : '+');
      int abs = Math.abs(exp);
      if (abs < 10) {
        out.append('0');
      }
      out.append(abs);
    } else if (point >= digits.length()) {
      out.append(digits);
      for (int i = digits.length(); i < point; i++) {
        out.append('0');
      }
    } else if (point > 0) {
      out.append(digits, 0, point).append('.').append(digits, point, digits.length());
    } else {
      out.append("0.");
      for (int i = 0; i < -point; i++) {
        out.append('0');
      }
      out.append(digits);
    }
    return out.toString();
  }
}

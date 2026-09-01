# go-querystring-java

Java port of [github.com/google/go-querystring](https://github.com/google/go-querystring) — encodes
objects into URL query parameters.

The port covers `query/encode.go` in full: `Values`, `reflectValue`, `valueString`, `isEmptyValue`,
`parseTag` and `tagOptions.Contains`, plus `net/url.Values` (`UrlValues`) with Go's exact
`QueryEscape` behaviour.

## Build

```sh
mvn test      # runs the ported test suite
mvn package   # builds target/go-querystring-java-1.0.0.jar
```

Requires JDK 17.

## Usage

```java
public class Options {
  @Url("q")    public String query;
  @Url("all")  public boolean showAll;
  @Url("page") public int page;
}

Options opt = new Options();
opt.query = "foo";
opt.showAll = true;
opt.page = 2;

UrlValues v = Query.values(opt);
System.out.print(v.encode()); // all=true&page=2&q=foo
```

Nested objects are scoped exactly as in Go:

```
user[name]=acme&user[addr][postcode]=1234&user[addr][city]=SFO
```

## Tag mapping

Go struct tags become field annotations:

| Go | Java |
|---|---|
| `` `url:"name,omitempty"` `` | `@Url("name,omitempty")` |
| `` `del:"!"` `` | `@Del("!")` |
| `` `layout:"2006-01-02"` `` | `@Layout("yyyy-MM-dd")` |
| anonymous (embedded) field | `@Embedded`, or a superclass |

All `url` options are supported unchanged: `omitempty`, `int`, `unix`, `unixmilli`, `unixnano`,
`comma`, `space`, `semicolon`, `brackets`, `numbered`.

## Type mapping

| Go | Java |
|---|---|
| exported field | `public` field |
| unexported field | non-`public` field (skipped, same as Go) |
| `*T` | `T` (a Java reference is already a pointer), or `Ptr<T>` |
| `**T`, `*[]T` | `Ptr<Ptr<T>>`, `Ptr<List<T>>` |
| `[]T`, `[N]T` | `List<T>`, `T[]` |
| `time.Time` | `Instant`, `OffsetDateTime`, `ZonedDateTime`, `LocalDateTime`, `LocalDate`, `Date` |
| `interface{}` field | `Object` field |
| `Encoder` (value receiver) | `Encoder` |
| `Encoder` (pointer receiver) | `NilEncoder` |
| `IsZero() bool` | `Zeroable` |
| `fmt.Stringer` | `Stringer` |
| `error` return | `QueryException` |
| `url.Values` | `UrlValues` |

`Ptr<T>` is only needed where Java cannot otherwise express the pointer — double pointers, pointers
to collections, and collections of pointers. A `null` field of type `T` already behaves like Go's
nil `*T`.

## Porting notes

Points where Go and Java do not line up one-to-one, and how each was resolved:

- **Pointer vs. value receivers.** Go decides whether a custom encoder applies by whether the method
  sits on `T` or `*T`. Java has no such split, so `Encoder` models a value receiver (applies to both
  `T` and `Ptr<T>` fields) and `NilEncoder` models a pointer receiver (applies only through a
  `Ptr<T>` field, and its `encodeNilValues` handles Go's nil-receiver call).
- **Zero values.** Go instantiates the zero value of a nil pointer whose type defines an encoder
  (`reflect.New`). The port calls the type's no-argument constructor, and reports a `QueryException`
  naming the field if the type has none.
- **Scalar vs. struct.** Go tells a named scalar (`type UserID int`) from a struct by reflect kind.
  Java cannot, so a class is walked recursively unless it is a collection, map, array, temporal
  value, JDK type or a `Stringer`. Implement `Stringer` to encode a value object as one parameter.
- **Emptiness.** Go's `isEmptyValue` switches on reflect kind. The port checks `null`, string and
  collection length, numeric zero, `false`, then `Zeroable`. Go's zero `time.Time` maps to
  `Query.ZERO_TIME` (year 1, 00:00 UTC), so a temporal field equal to it counts as empty.
- **Embedding order.** `@Embedded` fields and superclass fields are encoded breadth-first, after the
  class's own fields, matching Go — `Mixed{Inner{"a"}, "b"}` yields `V=b&V=a`.
- **Layouts.** `@Layout` takes a `DateTimeFormatter` pattern (`yyyy-MM-dd`), not a Go reference-time
  layout (`2006-01-02`).
- **Float formatting.** Go's `fmt.Sprint` prints the shortest round-trip form; the port reproduces
  it rather than using Java's, so a float is `0`, `0.1` and `1e+21` — not `0.0`, `0.1` and `1.0E21`.
- **Integer width.** Java has no unsigned types; Go's `uint`/`uint64` fields port to `long`.

## Test parity

`src/test/java/.../QueryTest.java` is a case-by-case port of `query/encode_test.go`. Every Go test
function has a counterpart, with the same inputs and the same expected parameters:

| Go | Java |
|---|---|
| `TestValues_BasicTypes` | `valuesBasicTypes` |
| `TestValues_Pointers` | `valuesPointers` |
| `TestValues_Slices` | `valuesSlices` |
| `TestValues_NestedTypes` | `valuesNestedTypes` |
| `TestValues_OmitEmpty` | `valuesOmitEmpty` |
| `TestValues_EmbeddedStructs` | `valuesEmbeddedStructs` |
| `TestValues_InvalidInput` | `valuesInvalidInput` |
| `TestValues_CustomEncodingSlice` | `valuesCustomEncodingSlice` |
| `TestValues_CustomEncoding_Error` | `valuesCustomEncodingError` |
| `TestValues_CustomEncodingInt` | `valuesCustomEncodingInt` |
| `TestValues_CustomEncodingPointer` | `valuesCustomEncodingPointer` |
| `TestIsEmptyValue` | `isEmptyValue` |
| `TestParseTag` | `parseTag` |
| — | `encode` (covers `UrlValues.encode`) |

### Cases added by the port

`QueryApiTest.java` holds the cases with no counterpart in the Go suite, kept in a separate file so
`QueryTest.java` stays a one-for-one port. They cover surface the original does not have — `UrlValues`
is ported code where Go's `url.Values` is the standard library — and the kinds the Go suite never
hands to the renderer: the temporal types beyond `Instant`, both float widths including the
non-finite values and negative zero, Go's escape set, and the failure raised when a null field's
type has no no-argument constructor.

## License

BSD-3-Clause, unchanged from the Go original - see [LICENSE](LICENSE). Every ported file carries
the copyright header its Go counterpart carried.

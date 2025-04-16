I’m implementing a PATCH API in Java using JSON Patch as per RFC 6902. I have a strict validator that only allows explicitly defined operations and paths.

Please generate the `getAllowedOperationVsPath()` method for the given POJO, following these rules:

1. All three operations "add", "remove", and "replace" must be supported.
2. Prefer full, **leaf-level JSON Pointer paths** (e.g., `/address/city`) over object-level paths (e.g., `/address`) unless full object replacement is explicitly desired.
3. For **"add"**, allow use of `-` in list/set paths to append (e.g., `/files/-`, `/packages/0/files/-`).
4. For **"remove"** and **"replace"**, support indexed or wildcard-style (`*`) paths for arrays/lists/sets (e.g., `/files/*`, `/packages/*/name`).
5. Support multi-level nesting: nested objects, `List<T>`, `Set<String>`, etc.
6. Return a Java method that maps `operation -> set of valid paths` as a `Map<String, Set<String>>`.

---

### Example POJO:
```java
public class Tour {
    private String name;
    private Set<String> categories;
    private List<String> files;
    private BookingDetails bookingDetails;
    private List<Package> packages;

    public static class BookingDetails {
        private LocalDate startDate;
        private boolean isOpen;
    }

    public static class Package {
        private String name;
        private List<String> files;
    }
}
```
### Expected Output: 
```java
@Override
protected Map<String, Set<String>> getAllowedOperationVsPath() {
    return Map.of(
        "add", Set.of(
            "/categories/-",
            "/files/-",
            "/packages/-",
            "/packages/*/files/-"
        ),
        "remove", Set.of(
            "/categories/*",
            "/files/*",
            "/packages/*",
            "/packages/*/files/*"
        ),
        "replace", Set.of(
            "/name",
            "/bookingDetails/startDate",
            "/bookingDetails/isOpen",
            "/packages/*/name"
        )
    );
}
```

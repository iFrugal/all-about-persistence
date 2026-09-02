package lazydevs.readeasy.registry;

import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertNull;

class QueryRegistryTest {

    private static final String FILE_A = """
            queries:
              byId:
                raw: '{"nativeSQL": "SELECT * FROM users WHERE id = :id"}'
              all:
                raw: '{"nativeSQL": "SELECT * FROM users"}'
            """;

    private static final String FILE_B = """
            queries:
              search:
                raw: '{"nativeSQL": "SELECT * FROM users WHERE name LIKE :name"}'
            """;

    private static final String FILE_A_UPDATED = """
            queries:
              byId:
                raw: '{"nativeSQL": "SELECT id, name FROM users WHERE id = :id"}'
            """;

    @Test
    void registerAndGet() {
        QueryRegistry registry = new QueryRegistry(null);
        assertEquals(2, registry.register("users", "a.yaml", FILE_A));
        assertNotNull(registry.get("users.byId"));
        assertNotNull(registry.get("users.all"));
        assertEquals(2, registry.size());
    }

    @Test
    void reloadingOneFileKeepsSiblingFilesOfSameNamespace() {
        // Regression guard: one namespace fed by two files; re-registering file A
        // must not remove file B's queries.
        QueryRegistry registry = new QueryRegistry(null);
        registry.register("users", "a.yaml", FILE_A);
        registry.register("users", "b.yaml", FILE_B);
        assertEquals(3, registry.size());

        registry.register("users", "a.yaml", FILE_A_UPDATED);

        assertNotNull(registry.get("users.byId"), "updated query from a.yaml must exist");
        assertNull(registry.get("users.all"), "query removed from a.yaml must be gone");
        assertNotNull(registry.get("users.search"), "b.yaml's query must survive a.yaml's reload");
        assertEquals(2, registry.size());
    }

    @Test
    void reRegisteringSameContentIsIdempotent() {
        QueryRegistry registry = new QueryRegistry(null);
        registry.register("users", "a.yaml", FILE_A);
        registry.register("users", "a.yaml", FILE_A);
        assertEquals(2, registry.size());
    }

    @Test
    void unknownQueryIdReturnsNull() {
        QueryRegistry registry = new QueryRegistry(null);
        registry.register("users", "a.yaml", FILE_A);
        assertNull(registry.get("users.nope"));
        assertNull(registry.get("other.byId"));
    }
}

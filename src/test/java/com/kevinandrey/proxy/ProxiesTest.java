package com.kevinandrey.proxy;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertNotEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;

import java.util.ArrayList;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.Set;
import java.util.concurrent.CountDownLatch;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.Future;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.atomic.AtomicInteger;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.ValueSource;

class ProxiesTest {

    private static final User READER = new User("lector", Set.of(Role.READER));
    private static final User AUDITOR = new User("auditora", Set.of(Role.AUDITOR));

    @Nested
    class Values {

        @ParameterizedTest
        @ValueSource(strings = {"", "  "})
        void documentRejectsBlankIds(String id) {
            assertThrows(IllegalArgumentException.class, () -> new Document(id, "t", Classification.PUBLIC));
        }

        @Test
        void documentRejectsNulls() {
            assertThrows(NullPointerException.class, () -> new Document(null, "t", Classification.PUBLIC));
            assertThrows(NullPointerException.class, () -> new Document("a", null, Classification.PUBLIC));
            assertThrows(NullPointerException.class, () -> new Document("a", "t", null));
        }

        @Test
        void userCopiesRolesAndRejectsBlankNames() {
            Set<Role> roles = new HashSet<>(Set.of(Role.READER));
            User user = new User("ana", roles);
            roles.add(Role.AUDITOR);

            assertFalse(user.has(Role.AUDITOR));
            assertThrows(UnsupportedOperationException.class, () -> user.roles().add(Role.AUDITOR));
            assertThrows(IllegalArgumentException.class, () -> new User(" ", Set.of()));
            assertThrows(NullPointerException.class, () -> new User("ana", null));
        }
    }

    @Nested
    class Archive {

        @Test
        void countsEveryLookup() {
            ArchiveRepository archive = ArchiveRepository.sample();

            assertEquals("Informe anual 2018", archive.findById("INF-2018").orElseThrow().title());
            assertEquals(Optional.empty(), archive.findById("NO-EXISTE"));
            assertEquals(2, archive.lookups());
        }

        @Test
        void rejectsDuplicateIdsAndNullLookups() {
            Document doc = new Document("A", "t", Classification.PUBLIC);

            assertThrows(IllegalArgumentException.class, () -> new ArchiveRepository(List.of(doc, doc)));
            assertThrows(NullPointerException.class, () -> ArchiveRepository.sample().findById(null));
        }
    }

    @Nested
    class Lazy {

        @Test
        void createsTheTargetOnFirstLookupOnly() {
            AtomicInteger created = new AtomicInteger();
            LazyRepository lazy = new LazyRepository(() -> {
                created.incrementAndGet();

                return ArchiveRepository.sample();
            });

            assertFalse(lazy.isInitialized());
            assertEquals(0, created.get());
            lazy.findById("INF-2018");
            lazy.findById("MAN-001");
            assertTrue(lazy.isInitialized());
            assertEquals(1, created.get());
        }

        @Test
        void createsTheTargetOnceUnderConcurrentFirstCalls() throws Exception {
            AtomicInteger created = new AtomicInteger();
            LazyRepository lazy = new LazyRepository(() -> {
                created.incrementAndGet();

                return ArchiveRepository.sample();
            });
            int threads = 32;
            CountDownLatch start = new CountDownLatch(1);
            ExecutorService pool = Executors.newFixedThreadPool(threads);
            try {
                List<Future<Optional<Document>>> answers = new ArrayList<>();
                for (int i = 0; i < threads; i++) {
                    answers.add(pool.submit(() -> {
                        start.await();

                        return lazy.findById("INF-2018");
                    }));
                }
                start.countDown();
                for (Future<Optional<Document>> answer : answers) {
                    assertTrue(answer.get(10, TimeUnit.SECONDS).isPresent());
                }
            } finally {
                pool.shutdownNow();
            }

            assertEquals(1, created.get());
        }

        @Test
        void rejectsAFactoryThatReturnsNull() {
            LazyRepository lazy = new LazyRepository(() -> null);

            NullPointerException error = assertThrows(NullPointerException.class, () -> lazy.findById("A"));
            assertEquals("factory returned null", error.getMessage());
            assertThrows(NullPointerException.class, () -> new LazyRepository(null));
        }
    }

    @Nested
    class Caching {

        @Test
        void servesRepeatedLookupsFromTheCacheIncludingMisses() {
            ArchiveRepository archive = ArchiveRepository.sample();
            CachingRepository cache = new CachingRepository(archive, 8);

            cache.findById("INF-2018");
            cache.findById("INF-2018");
            cache.findById("NO-EXISTE");
            cache.findById("NO-EXISTE");

            assertEquals(2, archive.lookups());
            assertEquals(2, cache.hits());
            assertEquals(2, cache.misses());
            assertEquals(2, cache.size());
        }

        @Test
        void evictsTheLeastRecentlyUsedId() {
            ArchiveRepository archive = ArchiveRepository.sample();
            CachingRepository cache = new CachingRepository(archive, 2);

            cache.findById("INF-2018");
            cache.findById("MAN-001");
            cache.findById("INF-2018");
            cache.findById("NOM-2018");
            cache.findById("INF-2018");
            cache.findById("MAN-001");

            assertEquals(2, cache.size());
            assertEquals(4, archive.lookups());
        }

        @ParameterizedTest
        @ValueSource(ints = {0, -1})
        void rejectsNonPositiveCapacities(int capacity) {
            assertThrows(IllegalArgumentException.class, () -> new CachingRepository(ArchiveRepository.sample(), capacity));
        }
    }

    @Nested
    class Protection {

        @Test
        void readersSeePublicDocumentsOnly() {
            ProtectedRepository repository = new ProtectedRepository(ArchiveRepository.sample(), READER);

            assertTrue(repository.findById("INF-2018").isPresent());
            AccessDeniedException denied =
                    assertThrows(AccessDeniedException.class, () -> repository.findById("NOM-2018"));
            assertEquals("lector", denied.userName());
            assertEquals("NOM-2018", denied.documentId());
            assertEquals("user lector may not read document NOM-2018", denied.getMessage());
        }

        @Test
        void auditorsSeeConfidentialDocuments() {
            ProtectedRepository repository = new ProtectedRepository(ArchiveRepository.sample(), AUDITOR);

            assertEquals(Classification.CONFIDENTIAL, repository.findById("NOM-2018").orElseThrow().classification());
        }

        @Test
        void unknownIdsStayEmpty() {
            assertEquals(Optional.empty(), new ProtectedRepository(ArchiveRepository.sample(), READER).findById("X"));
        }
    }

    @Nested
    class Counting {

        @Test
        void countsRequestsPerId() {
            CountingRepository counting = new CountingRepository(ArchiveRepository.sample());

            counting.findById("MAN-001");
            counting.findById("INF-2018");
            counting.findById("MAN-001");

            assertEquals(2, counting.count("MAN-001"));
            assertEquals(0, counting.count("NOM-2018"));
            assertEquals(3, counting.total());
            assertEquals(List.of("INF-2018", "MAN-001"), List.copyOf(counting.snapshot().keySet()));
            assertEquals(Map.of("INF-2018", 1L, "MAN-001", 2L), counting.snapshot());
        }
    }

    @Nested
    class Auditing {

        @Test
        void reportsResultsAndRethrowsTheOriginalException() {
            List<String> log = new ArrayList<>();
            DocumentRepository audited = AuditingProxy.wrap(
                    DocumentRepository.class, new ProtectedRepository(ArchiveRepository.sample(), READER), log::add);

            audited.findById("NO-EXISTE");
            assertThrows(AccessDeniedException.class, () -> audited.findById("NOM-2018"));

            assertEquals(List.of("findById(NO-EXISTE) -> Optional.empty", "findById(NOM-2018) threw AccessDeniedException"), log);
        }

        @Test
        void handlesObjectMethodsWithoutReachingTheTarget() {
            List<String> log = new ArrayList<>();
            DocumentRepository audited = AuditingProxy.wrap(DocumentRepository.class, ArchiveRepository.sample(), log::add);
            DocumentRepository other = AuditingProxy.wrap(DocumentRepository.class, ArchiveRepository.sample(), log::add);

            assertTrue(audited.equals(audited));
            assertNotEquals(audited, other);
            assertEquals(System.identityHashCode(audited), audited.hashCode());
            assertTrue(audited.toString().startsWith("AuditingProxy["));
            assertTrue(log.isEmpty());
        }

        @Test
        void rejectsClassesAndNulls() {
            ArchiveRepository archive = ArchiveRepository.sample();

            assertThrows(IllegalArgumentException.class, () -> AuditingProxy.wrap(ArchiveRepository.class, archive, line -> { }));
            assertThrows(NullPointerException.class, () -> AuditingProxy.wrap(DocumentRepository.class, archive, null));
        }
    }

    @Test
    void stackedProxiesReachTheArchiveOncePerDistinctId() {
        AtomicInteger created = new AtomicInteger();
        ArchiveRepository archive = ArchiveRepository.sample();
        LazyRepository lazy = new LazyRepository(() -> {
            created.incrementAndGet();

            return archive;
        });
        CachingRepository cache = new CachingRepository(lazy, 16);
        CountingRepository counting = new CountingRepository(cache);
        DocumentRepository chain = new ProtectedRepository(counting, AUDITOR);

        for (String id : List.of("INF-2018", "NOM-2018", "INF-2018", "MAN-001", "INF-2018")) {
            chain.findById(id);
        }

        assertEquals(1, created.get());
        assertEquals(3, archive.lookups());
        assertEquals(5, counting.total());
        assertEquals(2, cache.hits());
    }
}

package com.kevinandrey.proxy.cli;

import com.kevinandrey.proxy.AccessDeniedException;
import com.kevinandrey.proxy.ArchiveRepository;
import com.kevinandrey.proxy.AuditingProxy;
import com.kevinandrey.proxy.CachingRepository;
import com.kevinandrey.proxy.CountingRepository;
import com.kevinandrey.proxy.Document;
import com.kevinandrey.proxy.DocumentRepository;
import com.kevinandrey.proxy.LazyRepository;
import com.kevinandrey.proxy.ProtectedRepository;
import com.kevinandrey.proxy.Role;
import com.kevinandrey.proxy.User;
import java.io.PrintStream;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.Optional;
import java.util.Set;
import java.util.concurrent.atomic.AtomicReference;
import java.util.stream.Collectors;

/** Stacks every proxy over the sample archive and prints, in Spanish, what each layer did. */
public final class ProxyDemo {

    static final int OK = 0;
    static final int USAGE_ERROR = 2;

    static final List<String> DEFAULT_IDS =
            List.of("INF-2018", "NOM-2018", "INF-2018", "MAN-001", "NO-EXISTE", "INF-2018");

    private static final String ROLE_FLAG = "--rol";
    private static final int CACHE_CAPACITY = 16;

    private ProxyDemo() {
    }

    /**
     * Entry point.
     *
     * @param args {@code [--rol lector|auditor] [id...]}
     */
    public static void main(String[] args) {
        int status = run(args, System.out, System.err);
        if (status != OK) {
            System.exit(status);
        }
    }

    static int run(String[] args, PrintStream out, PrintStream err) {
        Request request;
        try {
            request = parse(args);
        } catch (IllegalArgumentException e) {
            err.println("error: " + e.getMessage());
            err.println("uso: java -jar proxy-pattern.jar [--rol lector|auditor] [id ...]");

            return USAGE_ERROR;
        }
        new Session(request, out).run();

        return OK;
    }

    private record Request(Role role, List<String> ids) {
    }

    private static Request parse(String[] args) {
        Role role = Role.READER;
        List<String> ids = new ArrayList<>(Arrays.asList(args));
        if (!ids.isEmpty() && ROLE_FLAG.equals(ids.get(0))) {
            if (ids.size() < 2) {
                throw new IllegalArgumentException(ROLE_FLAG + " requiere lector o auditor");
            }
            role = switch (ids.get(1).toLowerCase(Locale.ROOT)) {
                case "lector" -> Role.READER;
                case "auditor" -> Role.AUDITOR;
                default -> throw new IllegalArgumentException("rol desconocido: " + ids.get(1));
            };
            ids = ids.subList(2, ids.size());
        }
        for (String id : ids) {
            if (id.isBlank() || id.startsWith("--")) {
                throw new IllegalArgumentException("identificador inválido: '" + id + "'");
            }
        }

        return new Request(role, ids.isEmpty() ? DEFAULT_IDS : List.copyOf(ids));
    }

    private static final class Session {

        private final Request request;
        private final PrintStream out;
        private final AtomicReference<ArchiveRepository> archive = new AtomicReference<>();
        private final List<String> audit = new ArrayList<>();
        private final LazyRepository lazy;
        private final CachingRepository caching;
        private final CountingRepository counting;
        private final DocumentRepository chain;

        Session(Request request, PrintStream out) {
            this.request = request;
            this.out = out;
            lazy = new LazyRepository(() -> {
                ArchiveRepository created = ArchiveRepository.sample();
                archive.set(created);

                return created;
            });
            caching = new CachingRepository(lazy, CACHE_CAPACITY);
            counting = new CountingRepository(caching);
            User user = new User(userName(request.role()), Set.of(request.role()));
            chain = AuditingProxy.wrap(DocumentRepository.class, new ProtectedRepository(counting, user), audit::add);
        }

        void run() {
            out.println("Usuario : " + userName(request.role()) + " (" + roleName(request.role()) + ")");
            out.println("Cadena  : auditoría > protección > conteo > caché > carga diferida > archivo");
            out.println("Archivo creado antes de consultar: " + yesNo(lazy.isInitialized()));
            out.println();
            for (String id : request.ids()) {
                out.printf("%-10s %s%n", id, describe(id));
            }
            out.println();
            out.println("Archivo creado al final      : " + yesNo(lazy.isInitialized()));
            out.println("Consultas de clientes        : " + counting.total());
            out.println("Consultas al archivo real    : " + (archive.get() == null ? 0 : archive.get().lookups()));
            out.println("Aciertos de caché            : " + caching.hits());
            out.println("Consultas por documento      : " + render(counting.snapshot()));
            out.println();
            out.println("Auditoría (proxy dinámico):");
            audit.forEach(line -> out.println("  " + line));
        }

        private String describe(String id) {
            try {
                Optional<Document> document = chain.findById(id);

                return document.map(Document::title).orElse("no existe");
            } catch (AccessDeniedException e) {
                return "acceso denegado (confidencial)";
            }
        }
    }

    private static String userName(Role role) {
        return role == Role.AUDITOR ? "auditora" : "lector";
    }

    private static String roleName(Role role) {
        return role == Role.AUDITOR ? "rol auditor" : "rol lector";
    }

    private static String yesNo(boolean value) {
        return value ? "sí" : "no";
    }

    private static String render(Map<String, Long> counts) {
        return counts.entrySet().stream().map(e -> e.getKey() + "=" + e.getValue()).collect(Collectors.joining(", "));
    }
}

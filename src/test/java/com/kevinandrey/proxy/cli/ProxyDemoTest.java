package com.kevinandrey.proxy.cli;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;

import java.io.ByteArrayOutputStream;
import java.io.PrintStream;
import java.nio.charset.StandardCharsets;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.ValueSource;

class ProxyDemoTest {

    private final ByteArrayOutputStream out = new ByteArrayOutputStream();
    private final ByteArrayOutputStream err = new ByteArrayOutputStream();

    private int run(String... args) {
        return ProxyDemo.run(
                args,
                new PrintStream(out, true, StandardCharsets.UTF_8),
                new PrintStream(err, true, StandardCharsets.UTF_8));
    }

    private String output() {
        return out.toString(StandardCharsets.UTF_8);
    }

    @Test
    void defaultRunShowsEveryLayer() {
        assertEquals(ProxyDemo.OK, run());

        String text = output();
        assertTrue(text.contains("Usuario : lector (rol lector)"));
        assertTrue(text.contains("Archivo creado antes de consultar: no"));
        assertTrue(text.contains("NOM-2018   acceso denegado (confidencial)"));
        assertTrue(text.contains("NO-EXISTE  no existe"));
        assertTrue(text.contains("Archivo creado al final      : sí"));
        assertTrue(text.contains("Consultas de clientes        : 6"));
        assertTrue(text.contains("Consultas al archivo real    : 4"));
        assertTrue(text.contains("Aciertos de caché            : 2"));
        assertTrue(text.contains("Consultas por documento      : INF-2018=3, MAN-001=1, NO-EXISTE=1, NOM-2018=1"));
        assertTrue(text.contains("  findById(NOM-2018) threw AccessDeniedException"));
    }

    @Test
    void auditorsReadConfidentialDocuments() {
        assertEquals(ProxyDemo.OK, run("--rol", "AUDITOR", "NOM-2018", "NOM-2018"));

        String text = output();
        assertTrue(text.contains("Usuario : auditora (rol auditor)"));
        assertTrue(text.contains("NOM-2018   Nómina de diciembre 2018"));
        assertTrue(text.contains("Consultas al archivo real    : 1"));
        assertTrue(text.contains("Aciertos de caché            : 1"));
    }

    @Test
    void rejectsUnknownRoles() {
        assertEquals(ProxyDemo.USAGE_ERROR, run("--rol", "gerente"));
        assertTrue(err.toString(StandardCharsets.UTF_8).contains("error: rol desconocido: gerente"));
        assertTrue(err.toString(StandardCharsets.UTF_8).contains("uso: java -jar proxy-pattern.jar"));
        assertEquals("", output());
    }

    @ParameterizedTest
    @ValueSource(strings = {"--rol", "--otro", " "})
    void rejectsMalformedArguments(String arg) {
        assertEquals(ProxyDemo.USAGE_ERROR, run(arg));
        assertTrue(err.toString(StandardCharsets.UTF_8).startsWith("error: "));
    }
}

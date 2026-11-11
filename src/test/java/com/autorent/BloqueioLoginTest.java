package com.autorent;

import com.autorent.config.BloqueioLogin;
import org.junit.jupiter.api.Test;

import java.time.Clock;
import java.time.Duration;
import java.time.Instant;
import java.time.ZoneId;

import static org.junit.jupiter.api.Assertions.*;

class BloqueioLoginTest {

    static class RelogioFalso extends Clock {
        Instant agora = Instant.parse("2026-11-12T10:00:00Z");

        @Override public ZoneId getZone() { return ZoneId.of("UTC"); }
        @Override public Clock withZone(ZoneId zone) { return this; }
        @Override public Instant instant() { return agora; }
        void avancar(Duration d) { agora = agora.plus(d); }
    }

    private final RelogioFalso relogio = new RelogioFalso();
    private final BloqueioLogin bloqueio = new BloqueioLogin(3, 10, relogio);

    @Test
    void bloqueiaNaTerceiraFalhaPorDezMinutos() {
        bloqueio.registrarFalha("1.1.1.1", "a@x.com");
        bloqueio.registrarFalha("1.1.1.1", "a@x.com");
        assertEquals(0, bloqueio.segundosRestantes("1.1.1.1", "a@x.com"));
        bloqueio.registrarFalha("1.1.1.1", "a@x.com");
        assertEquals(600, bloqueio.segundosRestantes("1.1.1.1", "A@x.com "));
    }

    @Test
    void naoAfetaOutroEmailNemOutroIp() {
        for (int i = 0; i < 3; i++) bloqueio.registrarFalha("1.1.1.1", "a@x.com");
        assertEquals(0, bloqueio.segundosRestantes("1.1.1.1", "outro@x.com"));
        assertEquals(0, bloqueio.segundosRestantes("2.2.2.2", "a@x.com"));
    }

    @Test
    void desbloqueiaAposOIntervalo() {
        for (int i = 0; i < 3; i++) bloqueio.registrarFalha("1.1.1.1", "a@x.com");
        relogio.avancar(Duration.ofMinutes(9));
        assertTrue(bloqueio.segundosRestantes("1.1.1.1", "a@x.com") > 0);
        relogio.avancar(Duration.ofMinutes(1).plusSeconds(1));
        assertEquals(0, bloqueio.segundosRestantes("1.1.1.1", "a@x.com"));
    }

    @Test
    void acertoZeraOContador() {
        bloqueio.registrarFalha("1.1.1.1", "a@x.com");
        bloqueio.registrarFalha("1.1.1.1", "a@x.com");
        bloqueio.registrarSucesso("1.1.1.1", "a@x.com");
        bloqueio.registrarFalha("1.1.1.1", "a@x.com");
        bloqueio.registrarFalha("1.1.1.1", "a@x.com");
        assertEquals(0, bloqueio.segundosRestantes("1.1.1.1", "a@x.com"));
    }

    @Test
    void falhasAntigasForaDaJanelaNaoSomam() {
        bloqueio.registrarFalha("1.1.1.1", "a@x.com");
        bloqueio.registrarFalha("1.1.1.1", "a@x.com");
        relogio.avancar(Duration.ofMinutes(11));
        bloqueio.registrarFalha("1.1.1.1", "a@x.com");
        assertEquals(0, bloqueio.segundosRestantes("1.1.1.1", "a@x.com"));
    }
}

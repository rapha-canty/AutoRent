package com.autorent.config;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.event.EventListener;
import org.springframework.security.authentication.event.AuthenticationFailureBadCredentialsEvent;
import org.springframework.security.authentication.event.AuthenticationSuccessEvent;
import org.springframework.security.core.Authentication;
import org.springframework.security.web.authentication.WebAuthenticationDetails;
import org.springframework.stereotype.Component;

import java.time.Clock;
import java.time.Duration;
import java.time.Instant;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;

@Component
public class BloqueioLogin {

    private static final class Registro {
        int falhas;
        Instant inicioJanela;
        Instant bloqueadoAte;
    }

    private final int maxTentativas;
    private final Duration bloqueio;
    private final Clock relogio;
    private final Map<String, Registro> registros = new ConcurrentHashMap<>();

    @Autowired
    public BloqueioLogin(@Value("${autorent.login.max-tentativas}") int maxTentativas,
                         @Value("${autorent.login.bloqueio-minutos}") long bloqueioMinutos) {
        this(maxTentativas, bloqueioMinutos, Clock.systemDefaultZone());
    }

    public BloqueioLogin(int maxTentativas, long bloqueioMinutos, Clock relogio) {
        this.maxTentativas = maxTentativas;
        this.bloqueio = Duration.ofMinutes(bloqueioMinutos);
        this.relogio = relogio;
    }

    public long segundosRestantes(String ip, String email) {
        Registro r = registros.get(chave(ip, email));
        Instant agora = relogio.instant();
        if (r == null || r.bloqueadoAte == null || !agora.isBefore(r.bloqueadoAte)) {
            return 0;
        }
        return (Duration.between(agora, r.bloqueadoAte).toMillis() + 999) / 1000;
    }

    public void registrarFalha(String ip, String email) {
        Instant agora = relogio.instant();
        registros.compute(chave(ip, email), (k, atual) -> {
            Registro r = atual;
            boolean recomecar = r == null
                    || (r.bloqueadoAte != null && !agora.isBefore(r.bloqueadoAte))
                    || (r.bloqueadoAte == null && agora.isAfter(r.inicioJanela.plus(bloqueio)));
            if (recomecar) {
                r = new Registro();
                r.inicioJanela = agora;
            }
            r.falhas++;
            if (r.falhas >= maxTentativas) {
                r.bloqueadoAte = agora.plus(bloqueio);
            }
            return r;
        });
        if (registros.size() > 10_000) {
            registros.entrySet().removeIf(e -> e.getValue().bloqueadoAte != null && !agora.isBefore(e.getValue().bloqueadoAte));
        }
    }

    public void registrarSucesso(String ip, String email) {
        registros.remove(chave(ip, email));
    }

    @EventListener
    public void aoFalhar(AuthenticationFailureBadCredentialsEvent evento) {
        Authentication a = evento.getAuthentication();
        registrarFalha(ip(a), String.valueOf(a.getName()));
    }

    @EventListener
    public void aoAutenticar(AuthenticationSuccessEvent evento) {
        Authentication a = evento.getAuthentication();
        registrarSucesso(ip(a), a.getName());
    }

    private String ip(Authentication a) {
        return a.getDetails() instanceof WebAuthenticationDetails d ? d.getRemoteAddress() : "desconhecido";
    }

    private String chave(String ip, String email) {
        String e = email == null ? "" : email.trim().toLowerCase();
        return ip + "|" + (e.length() > 150 ? e.substring(0, 150) : e);
    }
}

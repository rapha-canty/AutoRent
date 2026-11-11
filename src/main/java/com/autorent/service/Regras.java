package com.autorent.service;

import java.math.BigDecimal;
import java.math.RoundingMode;
import java.time.Duration;
import java.time.LocalDateTime;

public final class Regras {

    private static final long MINUTOS_DIA = 24 * 60;

    private Regras() {
    }

    public static String somenteDigitos(String valor) {
        return valor == null ? null : valor.replaceAll("\\D", "");
    }

    public static boolean cpfValido(String cpf) {
        String d = somenteDigitos(cpf);
        if (d == null || d.length() != 11 || d.chars().distinct().count() == 1) {
            return false;
        }
        return digito(d, 9) == d.charAt(9) - '0' && digito(d, 10) == d.charAt(10) - '0';
    }

    private static int digito(String d, int tamanho) {
        int soma = 0;
        for (int i = 0; i < tamanho; i++) {
            soma += (d.charAt(i) - '0') * (tamanho + 1 - i);
        }
        int resto = (soma * 10) % 11;
        return resto == 10 ? 0 : resto;
    }

    public static boolean placaValida(String placa) {
        return placa != null && placa.matches("^[A-Z]{3}[0-9][A-Z0-9][0-9]{2}$");
    }

    public static long diarias(LocalDateTime inicio, LocalDateTime fim) {
        long minutos = Duration.between(inicio, fim).toMinutes();
        return Math.max(1, (minutos + MINUTOS_DIA - 1) / MINUTOS_DIA);
    }

    public static BigDecimal valorEstimado(LocalDateTime inicio, LocalDateTime fim, BigDecimal valorDiaria) {
        return valorDiaria.multiply(BigDecimal.valueOf(diarias(inicio, fim))).setScale(2, RoundingMode.HALF_UP);
    }

    public static BigDecimal multaAtraso(LocalDateTime prevista, LocalDateTime efetiva, BigDecimal valorDiaria,
                                         BigDecimal fatorDiaria, long toleranciaMinutos) {
        long atraso = Duration.between(prevista, efetiva).toMinutes();
        if (atraso <= toleranciaMinutos) {
            return BigDecimal.ZERO.setScale(2);
        }
        long dias = (atraso + MINUTOS_DIA - 1) / MINUTOS_DIA;
        return valorDiaria.multiply(BigDecimal.valueOf(dias)).multiply(fatorDiaria).setScale(2, RoundingMode.HALF_UP);
    }
}

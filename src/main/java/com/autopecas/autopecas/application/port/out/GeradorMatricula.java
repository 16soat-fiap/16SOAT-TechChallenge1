package com.autopecas.autopecas.application.port.out;

/**
 * Port de saída para a geração de matrículas de funcionários (MEC-0001, ATD-0001).
 *
 * <p>As implementações usam as sequences mecanico_seq e atendente_seq do Postgres, evitando
 * colisão em ambientes concorrentes.
 */
public interface GeradorMatricula {

    String proximaDeMecanico();

    String proximaDeAtendente();
}

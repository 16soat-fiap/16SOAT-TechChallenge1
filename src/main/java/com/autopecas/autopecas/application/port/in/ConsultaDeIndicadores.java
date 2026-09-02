package com.autopecas.autopecas.application.port.in;

import com.autopecas.autopecas.application.port.in.view.IndicadoresView;
import com.autopecas.autopecas.application.port.in.view.TempoMedioView;

import java.util.List;

/** Inbound port do dashboard da oficina. */
public interface ConsultaDeIndicadores {

    IndicadoresView indicadores();

    List<TempoMedioView> tempoMedioDeExecucao();
}

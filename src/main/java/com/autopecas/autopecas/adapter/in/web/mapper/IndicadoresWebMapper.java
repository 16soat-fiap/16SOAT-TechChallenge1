package com.autopecas.autopecas.adapter.in.web.mapper;

import com.autopecas.autopecas.adapter.in.web.dto.dashboard.DashboardDTO;
import com.autopecas.autopecas.adapter.in.web.dto.dashboard.TempoMedioDTO;
import com.autopecas.autopecas.application.port.in.view.IndicadoresView;
import com.autopecas.autopecas.application.port.in.view.TempoMedioView;
import org.mapstruct.Mapper;

/** Tradução das views do dashboard para os DTOs de resposta HTTP. */
@Mapper(componentModel = "spring")
public interface IndicadoresWebMapper {

    DashboardDTO paraResposta(IndicadoresView view);

    TempoMedioDTO paraResposta(TempoMedioView view);
}

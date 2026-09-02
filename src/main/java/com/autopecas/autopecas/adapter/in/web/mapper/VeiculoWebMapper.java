package com.autopecas.autopecas.adapter.in.web.mapper;

import com.autopecas.autopecas.adapter.in.web.dto.veiculo.VeiculoCreateDTO;
import com.autopecas.autopecas.adapter.in.web.dto.veiculo.VeiculoResponseDTO;
import com.autopecas.autopecas.adapter.in.web.dto.veiculo.VeiculoUpdateDTO;
import com.autopecas.autopecas.application.port.in.GestaoDeVeiculos;
import com.autopecas.autopecas.application.port.in.view.VeiculoView;
import org.mapstruct.Mapper;

/** Tradução entre os DTOs HTTP de veículo e os commands/views da aplicação. */
@Mapper(componentModel = "spring")
public interface VeiculoWebMapper {

    VeiculoResponseDTO paraResposta(VeiculoView view);

    GestaoDeVeiculos.Cadastrar paraComando(VeiculoCreateDTO dto);

    GestaoDeVeiculos.AtualizarDados paraComando(VeiculoUpdateDTO dto);
}

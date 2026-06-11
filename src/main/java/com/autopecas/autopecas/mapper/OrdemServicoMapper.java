package com.autopecas.autopecas.mapper;

import com.autopecas.autopecas.domain.entity.OrdemServico;
import com.autopecas.autopecas.dto.os.OrdemServicoResponseDTO;
import org.mapstruct.Mapper;
import org.mapstruct.Mapping;

@Mapper(componentModel = "spring")
public interface OrdemServicoMapper {

    @Mapping(source = "cliente.id", target = "clienteId")
    @Mapping(source = "cliente.nome", target = "clienteNome")
    @Mapping(source = "veiculo.id", target = "veiculoId")
    @Mapping(source = "veiculo.placa", target = "veiculoPlaca")
    @Mapping(target = "status", expression = "java(os.getStatus() != null ? os.getStatus().name() : null)")
    OrdemServicoResponseDTO toResponse(OrdemServico os);
}

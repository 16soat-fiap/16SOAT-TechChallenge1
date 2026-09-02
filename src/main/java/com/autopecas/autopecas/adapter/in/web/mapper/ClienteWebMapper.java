package com.autopecas.autopecas.adapter.in.web.mapper;

import com.autopecas.autopecas.adapter.in.web.dto.cliente.ClienteCreatePFDTO;
import com.autopecas.autopecas.adapter.in.web.dto.cliente.ClienteCreatePJDTO;
import com.autopecas.autopecas.adapter.in.web.dto.cliente.ClienteResponseDTO;
import com.autopecas.autopecas.adapter.in.web.dto.cliente.ClienteUpdateDTO;
import com.autopecas.autopecas.application.port.in.GestaoDeClientes;
import com.autopecas.autopecas.application.port.in.view.ClienteView;
import org.mapstruct.Mapper;

/** Tradução entre os DTOs HTTP de cliente e os commands/views da aplicação. */
@Mapper(componentModel = "spring")
public interface ClienteWebMapper {

    ClienteResponseDTO paraResposta(ClienteView view);

    GestaoDeClientes.CadastrarPF paraComando(ClienteCreatePFDTO dto);

    GestaoDeClientes.CadastrarPJ paraComando(ClienteCreatePJDTO dto);

    GestaoDeClientes.AtualizarDados paraComando(ClienteUpdateDTO dto);
}

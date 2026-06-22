package com.autopecas.autopecas.controller;

import com.autopecas.autopecas.config.SecurityConfig;
import com.autopecas.autopecas.dto.cliente.ClienteCreatePFDTO;
import com.autopecas.autopecas.dto.cliente.ClienteCreatePJDTO;
import com.autopecas.autopecas.dto.cliente.ClienteResponseDTO;
import com.autopecas.autopecas.dto.cliente.ClienteUpdateDTO;
import com.autopecas.autopecas.exception.ResourceNotFoundException;
import com.autopecas.autopecas.service.ClienteService;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.webmvc.test.autoconfigure.WebMvcTest;
import org.springframework.context.annotation.Import;
import org.springframework.http.MediaType;
import org.springframework.test.context.bean.override.mockito.MockitoBean;
import org.springframework.test.web.servlet.MockMvc;

import java.time.LocalDate;
import java.util.List;
import java.util.UUID;

import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.delete;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.put;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

@WebMvcTest(ClienteController.class)
@Import(SecurityConfig.class)
@DisplayName("ClienteController")
class ClienteControllerTest {

    @Autowired
    private MockMvc mockMvc;

    @MockitoBean
    private ClienteService clienteService;

    @Nested
    @DisplayName("Testes dos endpoints")
    class EndpointTests {

        @Test
        @DisplayName("GET /api/clientes deve listar clientes")
        void getApiClientesDeveListarClientes() throws Exception {
            // Given
            ClienteResponseDTO response = responseDTO();
            when(clienteService.listar()).thenReturn(List.of(response));

            // When / Then
            mockMvc.perform(get("/api/clientes"))
                    .andExpect(status().isOk())
                    .andExpect(jsonPath("$[0].id").value(response.id().toString()))
                    .andExpect(jsonPath("$[0].nome").value(response.nome()));
        }

        @Test
        @DisplayName("GET /api/clientes/{id} deve mapear 404")
        void getApiClientesIdDeveMapear404() throws Exception {
            // Given
            UUID id = UUID.randomUUID();
            when(clienteService.buscarPorId(id)).thenThrow(new ResourceNotFoundException("Cliente não encontrado através do ID: " + id));

            // When / Then
            mockMvc.perform(get("/api/clientes/{id}", id))
                    .andExpect(status().isNotFound())
                    .andExpect(jsonPath("$.message").value("Cliente não encontrado através do ID: " + id));
        }

        @Test
        @DisplayName("GET /api/clientes/buscarDOC deve buscar por documento")
        void getApiClientesBuscarDocDeveBuscarPorDocumento() throws Exception {
            // Given
            ClienteResponseDTO response = responseDTO();
            when(clienteService.buscarPorDocumento("12345678901")).thenReturn(response);

            // When / Then
            mockMvc.perform(get("/api/clientes/buscarDOC").param("documento", "12345678901"))
                    .andExpect(status().isOk())
                    .andExpect(jsonPath("$.cpf").value(response.cpf()));
        }

        @Test
        @DisplayName("POST /api/clientes/pf deve criar PF")
        void postApiClientesPfDeveCriarPf() throws Exception {
            // Given
            ClienteCreatePFDTO request = new ClienteCreatePFDTO("Cliente PF", "pf@teste.com", "11999999999", true, "12345678901", LocalDate.of(1990, 1, 1), "RG", "MASCULINO");
            ClienteResponseDTO response = responseDTO();
            when(clienteService.criarPF(request)).thenReturn(response);

            // When / Then
            mockMvc.perform(post("/api/clientes/pf")
                            .contentType(MediaType.APPLICATION_JSON)
                            .content("""
                                    {
                                      \"nome\": \"Cliente PF\",
                                      \"email\": \"pf@teste.com\",
                                      \"telefone\": \"11999999999\",
                                      \"aceitaNotificacoes\": true,
                                      \"cpf\": \"12345678901\",
                                      \"dataNascimento\": \"1990-01-01\",
                                      \"rg\": \"RG\",
                                      \"genero\": \"MASCULINO\"
                                    }
                                    """))
                    .andExpect(status().isCreated())
                    .andExpect(jsonPath("$.nome").value(response.nome()));
        }

        @Test
        @DisplayName("POST /api/clientes/pf com body vazio deve refletir falha atual de desserialização")
        void postApiClientesPfComBodyVazioDeveRefletirFalhaAtualDeDesserializacao() throws Exception {
            // When / Then
            mockMvc.perform(post("/api/clientes/pf")
                            .contentType(MediaType.APPLICATION_JSON)
                            .content("{}"))
                    .andExpect(status().isInternalServerError())
                    .andExpect(jsonPath("$.status").value(500))
                    .andExpect(jsonPath("$.error").value("Erro interno"));
        }

        @Test
        @DisplayName("POST /api/clientes/pj deve criar PJ")
        void postApiClientesPjDeveCriarPj() throws Exception {
            // Given
            ClienteCreatePJDTO request = new ClienteCreatePJDTO("Empresa", "empresa@teste.com", "11333333333", true, "12345678000190", "Empresa LTDA", "123");
            ClienteResponseDTO response = responseDTO();
            when(clienteService.criarPJ(request)).thenReturn(response);

            // When / Then
            mockMvc.perform(post("/api/clientes/pj")
                            .contentType(MediaType.APPLICATION_JSON)
                            .content("""
                                    {
                                      \"nome\": \"Empresa\",
                                      \"email\": \"empresa@teste.com\",
                                      \"telefone\": \"11333333333\",
                                      \"aceitaNotificacoes\": true,
                                      \"cnpj\": \"12345678000190\",
                                      \"razaoSocial\": \"Empresa LTDA\",
                                      \"inscricaoEstadual\": \"123\"
                                    }
                                    """))
                    .andExpect(status().isCreated())
                    .andExpect(jsonPath("$.nome").value(response.nome()));
        }

        @Test
        @DisplayName("PUT /api/clientes/{id} deve atualizar cliente")
        void putApiClientesIdDeveAtualizarCliente() throws Exception {
            // Given
            UUID id = UUID.randomUUID();
            ClienteUpdateDTO request = new ClienteUpdateDTO("Novo Nome", "novo@teste.com", "11888888888", false);
            ClienteResponseDTO response = responseDTO();
            when(clienteService.atualizar(id, request)).thenReturn(response);

            // When / Then
            mockMvc.perform(put("/api/clientes/{id}", id)
                            .contentType(MediaType.APPLICATION_JSON)
                            .content("""
                                    {
                                      \"nome\": \"Novo Nome\",
                                      \"email\": \"novo@teste.com\",
                                      \"telefone\": \"11888888888\",
                                      \"aceitaNotificacoes\": false
                                    }
                                    """))
                    .andExpect(status().isOk())
                    .andExpect(jsonPath("$.email").value(response.email()));
        }

        @Test
        @DisplayName("DELETE /api/clientes/{id} deve retornar 204")
        void deleteApiClientesIdDeveRetornar204() throws Exception {
            // Given
            UUID id = UUID.randomUUID();

            // When / Then
            mockMvc.perform(delete("/api/clientes/{id}", id))
                    .andExpect(status().isNoContent());

            verify(clienteService).desativar(id);
        }
    }

    private ClienteResponseDTO responseDTO() {
        return new ClienteResponseDTO(UUID.randomUUID(), "Cliente Teste", "12345678901", "", "cliente@teste.com", "11999999999");
    }
}
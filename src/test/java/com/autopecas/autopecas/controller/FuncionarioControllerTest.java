package com.autopecas.autopecas.controller;

import com.autopecas.autopecas.config.SecurityConfig;
import com.autopecas.autopecas.dto.funcionario.AtendenteCreateDTO;
import com.autopecas.autopecas.dto.funcionario.FuncionarioResponseDTO;
import com.autopecas.autopecas.dto.funcionario.MecanicoCreateDTO;
import com.autopecas.autopecas.service.FuncionarioService;
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
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

@WebMvcTest(FuncionarioController.class)
@Import(SecurityConfig.class)
@DisplayName("FuncionarioController")
class FuncionarioControllerTest {

    @Autowired
    private MockMvc mockMvc;

    @MockitoBean
    private FuncionarioService funcionarioService;

    @Nested
    @DisplayName("Testes dos endpoints")
    class EndpointTests {

        @Test
        @DisplayName("GET /api/funcionarios deve listar funcionários")
        void getApiFuncionariosDeveListarFuncionarios() throws Exception {
            // Given
            FuncionarioResponseDTO response = responseDTO();
            when(funcionarioService.listar()).thenReturn(List.of(response));

            // When / Then
            mockMvc.perform(get("/api/funcionarios"))
                    .andExpect(status().isOk())
                    .andExpect(jsonPath("$[0].matricula").value(response.matricula()));
        }

        @Test
        @DisplayName("GET /api/funcionarios/{id} deve buscar por id")
        void getApiFuncionariosIdDeveBuscarPorId() throws Exception {
            // Given
            FuncionarioResponseDTO response = responseDTO();
            when(funcionarioService.buscarPorId(response.id())).thenReturn(response);

            // When / Then
            mockMvc.perform(get("/api/funcionarios/{id}", response.id()))
                    .andExpect(status().isOk())
                    .andExpect(jsonPath("$.nome").value(response.nome()));
        }

        @Test
        @DisplayName("POST /api/funcionarios/mecanico deve criar mecânico")
        void postApiFuncionariosMecanicoDeveCriarMecanico() throws Exception {
            // Given
            MecanicoCreateDTO request = new MecanicoCreateDTO("Mecânico", "12345678900", "mec@teste.com", "11999999999", LocalDate.of(1988, 1, 1));
            FuncionarioResponseDTO response = responseDTO();
            when(funcionarioService.criarMecanico(request)).thenReturn(response);

            // When / Then
            mockMvc.perform(post("/api/funcionarios/mecanico")
                            .contentType(MediaType.APPLICATION_JSON)
                            .content("""
                                    {
                                      \"nome\": \"Mecânico\",
                                      \"cpf\": \"12345678900\",
                                      \"email\": \"mec@teste.com\",
                                      \"telefone\": \"11999999999\",
                                      \"dataNascimento\": \"1988-01-01\"
                                    }
                                    """))
                    .andExpect(status().isCreated())
                    .andExpect(jsonPath("$.tipo").value(response.tipo()));
        }

        @Test
        @DisplayName("POST /api/funcionarios/atendente deve criar atendente")
        void postApiFuncionariosAtendenteDeveCriarAtendente() throws Exception {
            // Given
            AtendenteCreateDTO request = new AtendenteCreateDTO("Atendente", "12345678900", "atd@teste.com", "11888888888", LocalDate.of(1990, 1, 1));
            FuncionarioResponseDTO response = responseDTO();
            when(funcionarioService.criarAtendente(request)).thenReturn(response);

            // When / Then
            mockMvc.perform(post("/api/funcionarios/atendente")
                            .contentType(MediaType.APPLICATION_JSON)
                            .content("""
                                    {
                                      \"nome\": \"Atendente\",
                                      \"cpf\": \"12345678900\",
                                      \"email\": \"atd@teste.com\",
                                      \"telefone\": \"11888888888\",
                                      \"dataNascimento\": \"1990-01-01\"
                                    }
                                    """))
                    .andExpect(status().isCreated())
                    .andExpect(jsonPath("$.nome").value(response.nome()));
        }

        @Test
        @DisplayName("POST /api/funcionarios/mecanico deve retornar 400 para payload inválido")
        void postApiFuncionariosMecanicoDeveRetornar400ParaPayloadInvalido() throws Exception {
            // When / Then
            mockMvc.perform(post("/api/funcionarios/mecanico")
                            .contentType(MediaType.APPLICATION_JSON)
                            .content("{}"))
                    .andExpect(status().isBadRequest())
                    .andExpect(jsonPath("$.campos.nome").value("Nome é obrigatório"))
                    .andExpect(jsonPath("$.campos.cpf").value("CPF é obrigatório"));
        }

        @Test
        @DisplayName("DELETE /api/funcionarios/{id} deve retornar 204")
        void deleteApiFuncionariosIdDeveRetornar204() throws Exception {
            // Given
            UUID id = UUID.randomUUID();

            // When / Then
            mockMvc.perform(delete("/api/funcionarios/{id}", id))
                    .andExpect(status().isNoContent());

            verify(funcionarioService).desativar(id);
        }
    }

    private FuncionarioResponseDTO responseDTO() {
        return new FuncionarioResponseDTO(UUID.randomUUID(), "MEC-0001", "Mecânico Teste", "mec@teste.com", "11999999999", "MECANICO", true);
    }
}




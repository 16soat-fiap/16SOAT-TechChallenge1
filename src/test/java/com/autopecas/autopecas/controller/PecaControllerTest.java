package com.autopecas.autopecas.controller;

import com.autopecas.autopecas.config.SecurityConfig;
import com.autopecas.autopecas.config.WebMvcSecurityTestConfig;
import com.autopecas.autopecas.dto.peca.MovimentacaoCreateDTO;
import com.autopecas.autopecas.dto.peca.MovimentacaoResponseDTO;
import com.autopecas.autopecas.dto.peca.PecaCreateDTO;
import com.autopecas.autopecas.dto.peca.PecaResponseDTO;
import com.autopecas.autopecas.exception.BusinessException;
import com.autopecas.autopecas.service.PecaService;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.webmvc.test.autoconfigure.WebMvcTest;
import org.springframework.context.annotation.Import;
import org.springframework.http.MediaType;
import org.springframework.test.context.bean.override.mockito.MockitoBean;
import org.springframework.test.web.servlet.MockMvc;

import java.math.BigDecimal;
import java.time.LocalDateTime;
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

@WebMvcTest(PecaController.class)
@Import({SecurityConfig.class, WebMvcSecurityTestConfig.class})
@DisplayName("PecaController")
class PecaControllerTest {

    @Autowired
    private MockMvc mockMvc;

    @MockitoBean
    private PecaService pecaService;

    @Nested
    @DisplayName("Testes dos endpoints")
    class EndpointTests {

        @Test
        @DisplayName("GET /api/pecas deve listar peças com filtro opcional")
        void getApiPecasDeveListarPecasComFiltroOpcional() throws Exception {
            // Given
            PecaResponseDTO response = responseDTO();
            when(pecaService.listar(true)).thenReturn(List.of(response));

            // When / Then
            mockMvc.perform(get("/api/pecas").param("estoqueBaixo", "true"))
                    .andExpect(status().isOk())
                    .andExpect(jsonPath("$[0].codigo").value(response.codigo()));
        }

        @Test
        @DisplayName("GET /api/pecas/{id} deve buscar por id")
        void getApiPecasIdDeveBuscarPorId() throws Exception {
            // Given
            PecaResponseDTO response = responseDTO();
            when(pecaService.buscarPorId(response.id())).thenReturn(response);

            // When / Then
            mockMvc.perform(get("/api/pecas/{id}", response.id()))
                    .andExpect(status().isOk())
                    .andExpect(jsonPath("$.nome").value(response.nome()));
        }

        @Test
        @DisplayName("GET /api/pecas/buscar deve buscar por código")
        void getApiPecasBuscarDeveBuscarPorCodigo() throws Exception {
            // Given
            PecaResponseDTO response = responseDTO();
            when(pecaService.buscarPorCodigo("PEC001")).thenReturn(response);

            // When / Then
            mockMvc.perform(get("/api/pecas/buscar").param("codigo", "PEC001"))
                    .andExpect(status().isOk())
                    .andExpect(jsonPath("$.codigo").value(response.codigo()));
        }

        @Test
        @DisplayName("POST /api/pecas deve criar peça")
        void postApiPecasDeveCriarPeca() throws Exception {
            // Given
            PecaCreateDTO request = new PecaCreateDTO("PEC001", "Pastilha", "Dianteira", new BigDecimal("99.90"), 10, 2, "kit");
            PecaResponseDTO response = responseDTO();
            when(pecaService.criar(request)).thenReturn(response);

            // When / Then
            mockMvc.perform(post("/api/pecas")
                            .contentType(MediaType.APPLICATION_JSON)
                            .content("""
                                    {
                                      \"codigo\": \"PEC001\",
                                      \"nome\": \"Pastilha\",
                                      \"descricao\": \"Dianteira\",
                                      \"precoVenda\": 99.90,
                                      \"quantidadeInicial\": 10,
                                      \"quantidadeMinima\": 2,
                                      \"unidade\": \"kit\"
                                    }
                                    """))
                    .andExpect(status().isCreated())
                    .andExpect(jsonPath("$.codigo").value(response.codigo()));
        }

        @Test
        @DisplayName("POST /api/pecas deve retornar 400 para payload inválido")
        void postApiPecasDeveRetornar400ParaPayloadInvalido() throws Exception {
            // When / Then
            mockMvc.perform(post("/api/pecas")
                            .contentType(MediaType.APPLICATION_JSON)
                            .content("{}"))
                    .andExpect(status().isBadRequest())
                    .andExpect(jsonPath("$.campos.codigo").value("Código é obrigatório"))
                    .andExpect(jsonPath("$.campos.nome").value("Nome é obrigatório"))
                    .andExpect(jsonPath("$.campos.precoVenda").value("Preço de venda deve ser preenchido"));
        }

        @Test
        @DisplayName("PUT /api/pecas/{id} deve atualizar peça")
        void putApiPecasIdDeveAtualizarPeca() throws Exception {
            // Given
            UUID id = UUID.randomUUID();
            PecaCreateDTO request = new PecaCreateDTO("PEC001", "Pastilha Premium", "Desc", new BigDecimal("120.00"), null, 3, "par");
            PecaResponseDTO response = responseDTO();
            when(pecaService.atualizar(id, request)).thenReturn(response);

            // When / Then
            mockMvc.perform(put("/api/pecas/{id}", id)
                            .contentType(MediaType.APPLICATION_JSON)
                            .content("""
                                    {
                                      \"codigo\": \"PEC001\",
                                      \"nome\": \"Pastilha Premium\",
                                      \"descricao\": \"Desc\",
                                      \"precoVenda\": 120.00,
                                      \"quantidadeMinima\": 3,
                                      \"unidade\": \"par\"
                                    }
                                    """))
                    .andExpect(status().isOk())
                    .andExpect(jsonPath("$.nome").value(response.nome()));
        }

        @Test
        @DisplayName("DELETE /api/pecas/{id} deve retornar 204")
        void deleteApiPecasIdDeveRetornar204() throws Exception {
            // Given
            UUID id = UUID.randomUUID();

            // When / Then
            mockMvc.perform(delete("/api/pecas/{id}", id))
                    .andExpect(status().isNoContent());

            verify(pecaService).desativar(id);
        }

        @Test
        @DisplayName("POST /api/pecas/{id}/movimentacoes deve registrar movimentação")
        void postApiPecasIdMovimentacoesDeveRegistrarMovimentacao() throws Exception {
            // Given
            UUID id = UUID.randomUUID();
            MovimentacaoCreateDTO request = new MovimentacaoCreateDTO("entrada", 5, "Reposição");
            MovimentacaoResponseDTO response = new MovimentacaoResponseDTO(UUID.randomUUID(), "ENTRADA", 5, 20, "Reposição", LocalDateTime.now());
            when(pecaService.registrarMovimentacao(id, request)).thenReturn(response);

            // When / Then
            mockMvc.perform(post("/api/pecas/{id}/movimentacoes", id)
                            .contentType(MediaType.APPLICATION_JSON)
                            .content("""
                                    {
                                      \"tipo\": \"entrada\",
                                      \"quantidade\": 5,
                                      \"motivo\": \"Reposição\"
                                    }
                                    """))
                    .andExpect(status().isCreated())
                    .andExpect(jsonPath("$.tipo").value("ENTRADA"));
        }

        @Test
        @DisplayName("POST /api/pecas/{id}/movimentacoes deve mapear exceção de negócio para 422")
        void postApiPecasIdMovimentacoesDeveMapearExcecaoDeNegocioPara422() throws Exception {
            // Given
            UUID id = UUID.randomUUID();
            MovimentacaoCreateDTO request = new MovimentacaoCreateDTO("erro", 5, null);
            when(pecaService.registrarMovimentacao(id, request)).thenThrow(new BusinessException("Tipo de movimentação inválido: erro"));

            // When / Then
            mockMvc.perform(post("/api/pecas/{id}/movimentacoes", id)
                            .contentType(MediaType.APPLICATION_JSON)
                            .content("""
                                    {
                                      \"tipo\": \"erro\",
                                      \"quantidade\": 5
                                    }
                                    """))
                    .andExpect(status().is(422))
                    .andExpect(jsonPath("$.message").value("Tipo de movimentação inválido: erro"));
        }
    }

    private PecaResponseDTO responseDTO() {
        return new PecaResponseDTO(UUID.randomUUID(), "PEC001", "Pastilha", "Dianteira", "Marca X", new BigDecimal("99.90"), 10, 2, "kit", true, null, null, false);
    }
}




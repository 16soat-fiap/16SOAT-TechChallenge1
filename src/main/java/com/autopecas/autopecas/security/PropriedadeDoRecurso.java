package com.autopecas.autopecas.security;

import com.autopecas.autopecas.application.port.in.ControleDeAcessoDoCliente;
import org.springframework.security.core.Authentication;
import org.springframework.stereotype.Component;

import java.util.UUID;

/**
 * Ponte entre as expressões @PreAuthorize e a port de controle de acesso.
 *
 * <p>Registrado com o nome curto "propriedade" para que as anotações fiquem legíveis:
 * {@code @PreAuthorize("hasAnyRole('ADMIN','ATENDENTE') or @propriedade.ehOProprioCliente(authentication, #id)")}.
 *
 * <p>A única responsabilidade daqui é extrair o identificador do usuário do Authentication e
 * delegar. A regra de quem é dono de quê vive na aplicação; o Spring Security não a conhece.
 */
@Component("propriedade")
public class PropriedadeDoRecurso {

    private final ControleDeAcessoDoCliente controleDeAcesso;

    public PropriedadeDoRecurso(ControleDeAcessoDoCliente controleDeAcesso) {
        this.controleDeAcesso = controleDeAcesso;
    }

    public boolean ehOProprioCliente(Authentication authentication, UUID clienteId) {
        return controleDeAcesso.ehOProprioCliente(emailDe(authentication), clienteId);
    }

    public boolean ehDonoDoVeiculo(Authentication authentication, UUID veiculoId) {
        return controleDeAcesso.ehDonoDoVeiculo(emailDe(authentication), veiculoId);
    }

    public boolean ehDonoDaOrdemServico(Authentication authentication, UUID ordemServicoId) {
        return controleDeAcesso.ehDonoDaOrdemServico(emailDe(authentication), ordemServicoId);
    }

    public boolean ehDonoDaOrdemServicoPorNumero(Authentication authentication, String numero) {
        return controleDeAcesso.ehDonoDaOrdemServicoPorNumero(emailDe(authentication), numero);
    }

    /** O nome do principal é o e-mail do token — ver {@link KeycloakJwtAuthConverter}. */
    private String emailDe(Authentication authentication) {
        return authentication != null ? authentication.getName() : null;
    }
}

package com.autopecas.autopecas.application.port.in;

import java.util.UUID;

/**
 * Inbound port que responde se um cliente autenticado é dono de um recurso.
 *
 * <p>A role CLIENTE sozinha diz apenas "é um cliente", não "é o dono deste registro". Sem esta
 * checagem, qualquer cliente autenticado consegue ler o cadastro, os veículos e as ordens de
 * serviço de qualquer outro. As perguntas ficam aqui, e não no adapter web, porque respondê-las
 * exige consultar repositórios — o adapter só traduz o token em um e-mail e pergunta.
 *
 * <p>O vínculo entre o usuário do Keycloak e o cadastro é o e-mail: a coluna clientes.email é
 * UNIQUE e o token traz o claim "email". Toda resposta é negativa quando o e-mail é nulo, quando
 * não existe cliente ativo com aquele e-mail ou quando o recurso não existe — nunca lança.
 */
public interface ControleDeAcessoDoCliente {

    boolean ehOProprioCliente(String email, UUID clienteId);

    boolean ehDonoDoVeiculo(String email, UUID veiculoId);

    boolean ehDonoDaOrdemServico(String email, UUID ordemServicoId);

    boolean ehDonoDaOrdemServicoPorNumero(String email, String numero);
}

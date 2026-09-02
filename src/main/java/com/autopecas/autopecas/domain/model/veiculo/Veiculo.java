package com.autopecas.autopecas.domain.model.veiculo;

import com.autopecas.autopecas.domain.exception.BusinessException;
import com.autopecas.autopecas.domain.vo.Placa;

import java.util.UUID;

/**
 * Veículo de um cliente.
 *
 * <p>Regras:
 * <ul>
 *   <li>A placa é normalizada e validada pelo Value Object Placa.</li>
 *   <li>Marca e modelo são obrigatórios.</li>
 *   <li>Um veículo pertence a um cliente, referenciado por id (agregado separado).</li>
 *   <li>Veículo nunca é excluído — apenas desativado.</li>
 * </ul>
 *
 * <p>A unicidade de chassi e renavam depende de consulta ao repositório e por isso é
 * validada no caso de uso, não aqui.
 */
public final class Veiculo {

    private final UUID id;
    private final Placa placa;
    private String chassi;
    private String renavam;
    private String marca;
    private String modelo;
    private Integer anoModelo;
    private String cor;
    private String observacoes;
    private boolean ativo;
    private final UUID clienteId;

    private Veiculo(UUID id, Placa placa, String chassi, String renavam, String marca, String modelo,
                    Integer anoModelo, String cor, String observacoes, boolean ativo, UUID clienteId) {
        if (placa == null) {
            throw new BusinessException("Placa é obrigatória");
        }
        if (marca == null || marca.isBlank()) {
            throw new BusinessException("Marca é obrigatória");
        }
        if (modelo == null || modelo.isBlank()) {
            throw new BusinessException("Modelo é obrigatório");
        }
        if (clienteId == null) {
            throw new BusinessException("Veículo deve pertencer a um cliente");
        }
        this.id = id;
        this.placa = placa;
        this.chassi = chassi;
        this.renavam = renavam;
        this.marca = marca;
        this.modelo = modelo;
        this.anoModelo = anoModelo;
        this.cor = cor;
        this.observacoes = observacoes;
        this.ativo = ativo;
        this.clienteId = clienteId;
    }

    /** Novo veículo, ainda sem id. */
    public static Veiculo criar(Placa placa, String chassi, String renavam, String marca, String modelo,
                                Integer anoModelo, String cor, UUID clienteId) {
        return new Veiculo(null, placa, chassi, renavam, marca, modelo, anoModelo, cor, null, true, clienteId);
    }

    /** Re-hidratação a partir da persistência — uso exclusivo dos mappers de adapter. */
    public static Veiculo reconstituir(UUID id, Placa placa, String chassi, String renavam, String marca,
                                       String modelo, Integer anoModelo, String cor, String observacoes,
                                       boolean ativo, UUID clienteId) {
        return new Veiculo(id, placa, chassi, renavam, marca, modelo, anoModelo, cor, observacoes,
                ativo, clienteId);
    }

    /** Atualização parcial: campos nulos são ignorados. A placa não é alterável. */
    public void atualizarDados(String marca, String modelo, Integer anoModelo, String cor,
                               String chassi, String renavam) {
        if (marca != null) {
            this.marca = marca;
        }
        if (modelo != null) {
            this.modelo = modelo;
        }
        if (anoModelo != null) {
            this.anoModelo = anoModelo;
        }
        if (cor != null) {
            this.cor = cor;
        }
        if (chassi != null) {
            this.chassi = chassi;
        }
        if (renavam != null) {
            this.renavam = renavam;
        }
    }

    public void desativar() {
        this.ativo = false;
    }

    public boolean isNovo() {
        return id == null;
    }

    public UUID getId() {
        return id;
    }

    public Placa getPlaca() {
        return placa;
    }

    public String getChassi() {
        return chassi;
    }

    public String getRenavam() {
        return renavam;
    }

    public String getMarca() {
        return marca;
    }

    public String getModelo() {
        return modelo;
    }

    public Integer getAnoModelo() {
        return anoModelo;
    }

    public String getCor() {
        return cor;
    }

    public String getObservacoes() {
        return observacoes;
    }

    public boolean isAtivo() {
        return ativo;
    }

    public UUID getClienteId() {
        return clienteId;
    }
}

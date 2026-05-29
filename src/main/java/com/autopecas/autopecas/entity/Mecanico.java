package com.autopecas.autopecas.entity;

import com.autopecas.autopecas.enums.TipoFuncionario;
import jakarta.persistence.*;
import lombok.*;
import lombok.experimental.SuperBuilder;

import java.util.ArrayList;
import java.util.List;

/**
 * Mecânico/técnico — funcionário que executa serviços nas OS.
 *
 * Tabela "mecanicos" — herda campos comuns de "funcionarios".
 */
@Entity
@Table(name = "mecanicos")
@DiscriminatorValue("MECANICO")
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@SuperBuilder
@ToString(callSuper = true, exclude = "ordensServico")
public class Mecanico extends Funcionario {

    /**
     * podemos criar especialidades para o mecanicao
     * freios / eletronica / pintura / etc...
     */

    // Relacionamentos ─────────────────

    /** OS em que este mecânico é o responsável técnico. */
    @OneToMany(mappedBy = "mecanicoResponsavel", fetch = FetchType.LAZY)
    @Builder.Default
    private List<OrdemServico> ordensServico = new ArrayList<>();

    @Override
    public TipoFuncionario getTipo() {
        return TipoFuncionario.MECANICO;
    }
}


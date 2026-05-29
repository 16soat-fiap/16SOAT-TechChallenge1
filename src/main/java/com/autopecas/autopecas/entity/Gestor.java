package com.autopecas.autopecas.entity;


import com.autopecas.autopecas.enums.TipoFuncionario;
import jakarta.persistence.*;
import lombok.*;
import lombok.experimental.SuperBuilder;

/**
 * Aprova descontos
 * reposição de peças
 */

@Entity
@Table(name = "gestores")
@DiscriminatorValue("GESTOR")
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@SuperBuilder
@ToString(callSuper = true)
public class Gestor extends Funcionario {

    @Column(name = "area_gestao", nullable = false, length = 100)
    private String areaGestao;

    /** Pode aprovar descontos sem limite (true) ou apenas até certo valor (false). */
    @Column(name = "aprova_descontos_irrestritos", nullable = false)
    private Boolean aprovaDescontosIrrestritos = false;

    @Override
    public TipoFuncionario getTipo() {
        return TipoFuncionario.GESTOR;
    }
}


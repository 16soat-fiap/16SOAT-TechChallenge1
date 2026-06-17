package com.autopecas.autopecas.domain.entity;


import com.autopecas.autopecas.domain.enums.TipoFuncionario;
import jakarta.persistence.*;
import lombok.*;
import lombok.experimental.SuperBuilder;

import java.util.ArrayList;
import java.util.List;
@Entity
@Table(name = "atendentes")
@DiscriminatorValue("ATENDENTE")
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@SuperBuilder
@ToString(callSuper = true, exclude = {"ordensRecepcionadas", "ordensEntregues"})
public class Atendente extends Funcionario {

    //relacionamentos

    //OS criada pelo atendente
    @OneToMany(mappedBy = "atendenteRecepcao", fetch = FetchType.LAZY)
    @Builder.Default
    private List<OrdemServico> ordensRecepcionadas = new ArrayList<>();

    @OneToMany(mappedBy = "atendenteEntrega", fetch = FetchType.LAZY)
    @Builder.Default
    private List<OrdemServico> ordensEntregues = new ArrayList<>();

    @Override
    public TipoFuncionario getTipo() {
        return TipoFuncionario.ATENDENTE;
    }

}

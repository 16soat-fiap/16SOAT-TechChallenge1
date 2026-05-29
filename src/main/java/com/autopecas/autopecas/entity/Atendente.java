package com.autopecas.autopecas.entity;


import com.autopecas.autopecas.enums.TipoFuncionario;
import jakarta.persistence.*;
import lombok.*;
import lombok.experimental.SuperBuilder;

import java.math.BigDecimal;
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

    //Desconto que um funcionário poderá dar a um cliente
    @Column( name = "limite_desconto_percentual", precision = 5, scale = 2)
    private BigDecimal limiteDescontoPercentual;

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

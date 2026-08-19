package com.autopecas.autopecas.adapter.out.persistence.entity;

import jakarta.persistence.DiscriminatorValue;
import jakarta.persistence.Entity;
import jakarta.persistence.Table;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

/** Entidade JPA do mecânico. */
@Entity
@Table(name = "mecanicos")
@DiscriminatorValue("MECANICO")
@Getter
@Setter
@NoArgsConstructor
public class MecanicoJpaEntity extends FuncionarioJpaEntity {
}

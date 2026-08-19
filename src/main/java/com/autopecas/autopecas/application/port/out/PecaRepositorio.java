package com.autopecas.autopecas.application.port.out;

import com.autopecas.autopecas.domain.model.estoque.Peca;

import java.util.List;
import java.util.Optional;
import java.util.UUID;

/** Port de saída do agregado Peca. */
public interface PecaRepositorio {

    Peca salvar(Peca peca);

    Optional<Peca> porId(UUID id);

    Optional<Peca> porCodigo(String codigo);

    List<Peca> ativas();

    List<Peca> comEstoqueBaixo();

    boolean existePorCodigo(String codigo);
}

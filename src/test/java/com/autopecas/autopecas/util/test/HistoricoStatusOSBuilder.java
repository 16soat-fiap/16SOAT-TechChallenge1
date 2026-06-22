package com.autopecas.autopecas.util.test;

import com.autopecas.autopecas.domain.entity.Atendente;
import com.autopecas.autopecas.domain.entity.Funcionario;
import com.autopecas.autopecas.domain.entity.HistoricoStatusOS;
import com.autopecas.autopecas.domain.entity.OrdemServico;
import com.autopecas.autopecas.domain.enums.StatusOS;

public class HistoricoStatusOSBuilder {

    public static HistoricoStatusOS.HistoricoStatusOSBuilder historicoStatusOS() {
        // Este builder é mais um wrapper para os factory methods da entidade HistoricoStatusOS
        // Não é um builder tradicional do Lombok para a própria classe HistoricoStatusOS
        // Usaremos os factory methods diretamente nos testes ou criaremos helpers aqui.
        throw new UnsupportedOperationException("Use os factory methods da classe HistoricoStatusOS diretamente ou os helpers deste builder.");
    }

    public static HistoricoStatusOS abertura(OrdemServico os, Atendente atendente) {
        return HistoricoStatusOS.abertura(os, atendente);
    }

    public static HistoricoStatusOS porFuncionario(OrdemServico os, StatusOS anterior, StatusOS novo, Funcionario funcionario) {
        return HistoricoStatusOS.porFuncionario(os, anterior, novo, "Observação padrão do funcionário", funcionario);
    }

    public static HistoricoStatusOS porSistema(OrdemServico os, StatusOS anterior, StatusOS novo) {
        return HistoricoStatusOS.porSistema(os, anterior, novo, "Observação padrão do sistema");
    }
}

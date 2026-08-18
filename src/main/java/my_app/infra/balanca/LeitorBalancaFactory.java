package my_app.infra.balanca;

import my_app.db.models.ConexaoBalancaModel;

public class LeitorBalancaFactory {

    private LeitorBalancaFactory() {
    }

    private static final String NAO_CONFIGURADA = "Nenhuma conexão com a balança configurada. Configure em Conexão da balança.";

    public static LeitorBalanca criar(ConexaoBalancaModel config) {
        if (config == null || config.getTipoConexao() == null) {
            throw new IllegalStateException(NAO_CONFIGURADA);
        }

        if (config.getTipoConexao().equalsIgnoreCase("Serial")) {
            // config incompleta (ex.: linha de seed antiga sem porta/baud) — trata igual a
            // "não configurada" em vez de deixar o unboxing de Integer null estourar NPE.
            if (config.getPortaCom() == null || config.getPortaCom().isBlank() || config.getBaudRate() == null) {
                throw new IllegalStateException(NAO_CONFIGURADA);
            }
            return new LeitorBalancaSerial(config.getPortaCom(), config.getBaudRate());
        }
        if (config.getTipoConexao().equalsIgnoreCase("TCP")) {
            if (config.getIpAddress() == null || config.getIpAddress().isBlank() || config.getIpPort() == null) {
                throw new IllegalStateException(NAO_CONFIGURADA);
            }
            return new LeitorBalancaTcp(config.getIpAddress(), config.getIpPort());
        }

        throw new IllegalStateException("Tipo de conexão desconhecido: " + config.getTipoConexao());
    }
}

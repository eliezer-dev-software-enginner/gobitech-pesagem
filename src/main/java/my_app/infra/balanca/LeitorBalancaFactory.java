package my_app.infra.balanca;

import my_app.db.models.ConexaoBalancaModel;

public class LeitorBalancaFactory {

    private LeitorBalancaFactory() {
    }

    public static LeitorBalanca criar(ConexaoBalancaModel config) {
        if (config == null || config.getTipoConexao() == null) {
            throw new IllegalStateException("Nenhuma conexão com a balança configurada. Configure em Conexão da balança.");
        }

        if (config.getTipoConexao().equalsIgnoreCase("Serial")) {
            return new LeitorBalancaSerial(config.getPortaCom(), config.getBaudRate());
        }
        if (config.getTipoConexao().equalsIgnoreCase("TCP")) {
            return new LeitorBalancaTcp(config.getIpAddress(), config.getIpPort());
        }

        throw new IllegalStateException("Tipo de conexão desconhecido: " + config.getTipoConexao());
    }
}

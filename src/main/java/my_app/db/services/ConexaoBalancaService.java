package my_app.db.services;

import my_app.db.DB;
import my_app.db.models.ConexaoBalancaModel;
import my_app.db.repositories.ConexaoBalancaRepository;
import net.sf.persism.Session;

import java.sql.SQLException;
import java.time.LocalDateTime;

public class ConexaoBalancaService extends BaseService<ConexaoBalancaModel> {

    public ConexaoBalancaService() throws SQLException {
        this(DB.getPersismSession());
    }

    public ConexaoBalancaService(Session session) {
        super(new ConexaoBalancaRepository(session));
    }

    public ConexaoBalancaModel buscarUnico() throws SQLException {
        return ((ConexaoBalancaRepository) repository).buscarUnico();
    }

    public ConexaoBalancaModel salvarOuAtualizar(ConexaoBalancaModel model) throws SQLException {
        validarCampos(model);
        var existente = buscarUnico();
        if (existente == null) {
            model.setDataCriacao(LocalDateTime.now());
            return repository.salvar(model);
        }
        model.setId(existente.getId());
        model.setDataCriacao(existente.getDataCriacao());
        repository.atualizar(model);
        return model;
    }

    private void validarCampos(ConexaoBalancaModel model) {
        if (model.getTipoConexao() == null || model.getTipoConexao().isBlank())
            throw new IllegalArgumentException("Tipo de conexão é obrigatório");

        if (model.getTipoConexao().equalsIgnoreCase("Serial")) {
            if (model.getPortaCom() == null || model.getPortaCom().isBlank())
                throw new IllegalArgumentException("Porta COM é obrigatória para conexão Serial");
            if (model.getBaudRate() == null)
                throw new IllegalArgumentException("Baud rate é obrigatório para conexão Serial");
        } else if (model.getTipoConexao().equalsIgnoreCase("TCP")) {
            if (model.getIpAddress() == null || model.getIpAddress().isBlank())
                throw new IllegalArgumentException("Endereço IP é obrigatório para conexão TCP");
            if (model.getIpPort() == null)
                throw new IllegalArgumentException("Porta IP é obrigatória para conexão TCP");
        } else {
            throw new IllegalArgumentException("Tipo de conexão deve ser 'Serial' ou 'TCP'");
        }
    }
}

package my_app.db.services;

import my_app.db.DB;
import my_app.db.models.ClienteModel;
import my_app.db.repositories.ClienteRepository;
import net.sf.persism.Session;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.sql.SQLException;
import java.time.LocalDateTime;

import static my_app.utils.Utils.*;

public class ClienteService extends BaseService<ClienteModel> {

    private static final Logger log = LoggerFactory.getLogger(ClienteService.class);

    private final ClienteRepository clienteRepository;

    // produção
    public ClienteService() throws SQLException {
        this(DB.getPersismSession());
    }

    // testes
    public ClienteService(Session session) {
        super(new ClienteRepository(session));
        this.clienteRepository = (ClienteRepository) repository;
    }

    @Override
    public ClienteModel salvar(ClienteModel model) throws SQLException {
        validarCampos(model);
        if (model.getAtivo() == null) model.setAtivo(true);
        model.setDataCriacao(LocalDateTime.now());
        var salvo = repository.salvar(model);
        log.info("Cliente salvo: id={} loja={}", salvo.getId(), salvo.getLoja());
        return salvo;
    }

    @Override
    public void atualizar(ClienteModel model) throws SQLException {
        validarCampos(model);
        repository.atualizar(model);
        log.info("Cliente atualizado: id={} loja={}", model.getId(), model.getLoja());
    }

    private void validarCampos(ClienteModel model) throws SQLException {
        if (model.getLoja() == null || model.getLoja().isBlank()) {
            throw new IllegalArgumentException("Loja é obrigatória");
        }
        if (model.getRazaoSocial() == null || model.getRazaoSocial().isBlank()) {
            throw new IllegalArgumentException("Razão social é obrigatória");
        }

        var existenteLoja = clienteRepository.buscarPorLoja(model.getLoja());
        if (existenteLoja != null && !existenteLoja.getId().equals(model.getId())) {
            throw new IllegalArgumentException("Já existe um cliente cadastrado com essa loja");
        }

        if (model.getCpfCnpj() != null && !model.getCpfCnpj().isBlank()) {
            var existenteDoc = clienteRepository.buscarPorCpfCnpj(model.getCpfCnpj());
            if (existenteDoc != null && !existenteDoc.getId().equals(model.getId())) {
                throw new IllegalArgumentException("CPF/CNPJ já cadastrado para outro cliente");
            }
        }

        if (model.getTelefone() != null && !model.getTelefone().isBlank() && !isValidPhone(model.getTelefone())) {
            throw new IllegalArgumentException("Telefone inválido (informe DDD + Número)");
        }
        if (model.getCep() != null && !model.getCep().isBlank() && !isValidCep(model.getCep())) {
            throw new IllegalArgumentException("CEP inválido");
        }
    }
}

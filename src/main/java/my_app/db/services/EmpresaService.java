package my_app.db.services;

import my_app.db.DB;
import my_app.db.models.EmpresaModel;
import my_app.db.repositories.EmpresaRepository;
import my_app.utils.Utils;
import net.sf.persism.Session;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.sql.SQLException;
import java.time.LocalDateTime;

public class EmpresaService extends BaseService<EmpresaModel> {

    private static final Logger log = LoggerFactory.getLogger(EmpresaService.class);

    public EmpresaService() throws SQLException {
        this(DB.getPersismSession());
    }

    public EmpresaService(Session session) {
        super(new EmpresaRepository(session));
    }

    @Override
    public void atualizar(EmpresaModel model) throws SQLException {
        validarCampos(model);
        repository.atualizar(model);
        log.info("Empresa atualizada: id={} nome={}", model.getId(), model.getNome());
    }

    public EmpresaModel buscarUnico() throws SQLException {
        EmpresaRepository repo = (EmpresaRepository) repository;
        return repo.buscarUnico();
    }

    public EmpresaModel salvarOuAtualizar(EmpresaModel model) throws SQLException {
        validarCampos(model);
        var existente = buscarUnico();
        if (existente == null) {
            model.setDataCriacao(LocalDateTime.now());
            var salvo = repository.salvar(model);
            log.info("Empresa cadastrada: id={} nome={}", salvo.getId(), salvo.getNome());
            return salvo;
        }
        model.setId(existente.getId());
        model.setDataCriacao(existente.getDataCriacao());
        repository.atualizar(model);
        log.info("Empresa atualizada: id={} nome={}", model.getId(), model.getNome());
        return model;
    }

    private void validarCampos(EmpresaModel model) {
        if (model.getNome() == null || model.getNome().isBlank()) {
            throw new IllegalArgumentException("Nome é obrigatório");
        }
        if (model.getTelefone() != null && !model.getTelefone().isBlank() && !Utils.isValidPhone(model.getTelefone())) {
            throw new IllegalArgumentException("Telefone inválido");
        }
        if (model.getCep() != null && !model.getCep().isBlank() && !Utils.isValidCep(model.getCep())) {
            throw new IllegalArgumentException("CEP inválido");
        }
    }
}

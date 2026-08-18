package my_app.db.services;

import my_app.db.DB;
import my_app.db.models.LicensaModel;
import my_app.db.repositories.LicensaRepository;
import net.sf.persism.Session;

import java.sql.SQLException;
import java.time.LocalDateTime;
import java.util.UUID;

public class LicensaService extends BaseService<LicensaModel> {

    private final LicensaRepository licensaRepository;

    public LicensaService() throws SQLException {
        this(DB.getPersismSession());
    }

    public LicensaService(Session session) {
        super(new LicensaRepository(session));
        this.licensaRepository = (LicensaRepository) repository;
    }

    @Override
    public LicensaModel salvar(LicensaModel model) throws SQLException {
        if (model.getValor() == null || model.getValor().isBlank())
            throw new IllegalArgumentException("Valor da licença é obrigatório");
        var existente = licensaRepository.buscarPorValor(model.getValor());
        if (existente != null)
            throw new IllegalArgumentException("Já existe uma licença com esse valor");
        model.setDataCriacao(LocalDateTime.now());
        return repository.salvar(model);
    }

    /**
     * Gera e salva uma nova licença. Quem pode chamar isso (ex.: só admin) é
     * responsabilidade da camada de tela/ViewModel, não deste serviço.
     */
    public LicensaModel gerarNova(LocalDateTime expiraEm) throws SQLException {
        var model = new LicensaModel();
        model.setValor(UUID.randomUUID().toString());
        model.setExpiraEm(expiraEm);
        return salvar(model);
    }

    public LicensaModel buscarMaisRecente() throws SQLException {
        var lista = licensaRepository.listarMaisRecentePrimeiro();
        return lista.isEmpty() ? null : lista.getFirst();
    }

    public boolean validar(String valor) throws SQLException {
        var licensa = licensaRepository.buscarPorValor(valor);
        return licensa != null && !licensa.expirada();
    }
}

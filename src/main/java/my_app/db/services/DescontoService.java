package my_app.db.services;

import my_app.db.DB;
import my_app.db.models.DescontoModel;
import my_app.db.repositories.DescontoRepository;
import net.sf.persism.Session;

import java.math.BigDecimal;
import java.sql.SQLException;
import java.time.LocalDateTime;

public class DescontoService extends BaseService<DescontoModel> {

    public DescontoService() throws SQLException {
        this(DB.getPersismSession());
    }

    public DescontoService(Session session) {
        super(new DescontoRepository(session));
    }

    @Override
    public DescontoModel salvar(DescontoModel model) throws SQLException {
        zerarNulos(model);
        model.setDataCriacao(LocalDateTime.now());
        return repository.salvar(model);
    }

    @Override
    public void atualizar(DescontoModel model) throws SQLException {
        zerarNulos(model);
        repository.atualizar(model);
    }

    private void zerarNulos(DescontoModel model) {
        if (model.getAvariados() == null) model.setAvariados(BigDecimal.ZERO);
        if (model.getArdidos() == null) model.setArdidos(BigDecimal.ZERO);
        if (model.getQuebraArdidos() == null) model.setQuebraArdidos(BigDecimal.ZERO);
        if (model.getImpurezas() == null) model.setImpurezas(BigDecimal.ZERO);
        if (model.getQuebraImpurezas() == null) model.setQuebraImpurezas(BigDecimal.ZERO);
        if (model.getUmidade() == null) model.setUmidade(BigDecimal.ZERO);
        if (model.getQuebraUmidade() == null) model.setQuebraUmidade(BigDecimal.ZERO);
        if (model.getOutros() == null) model.setOutros(BigDecimal.ZERO);
    }
}

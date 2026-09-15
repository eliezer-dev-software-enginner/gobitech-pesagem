package my_app.db.services;

import my_app.db.DB;
import my_app.db.models.PreferenciasModel;
import my_app.db.repositories.PreferenciasRepository;
import my_app.domain.pesagem.TipoImpressao;
import net.sf.persism.Session;

import java.sql.SQLException;
import java.time.LocalDateTime;

public class PreferenciasService extends BaseService<PreferenciasModel> {

    public PreferenciasService() throws SQLException {
        this(DB.getPersismSession());
    }

    public PreferenciasService(Session session) {
        super(new PreferenciasRepository(session));
    }

    public TipoImpressao buscarTipoImpressao() throws SQLException {
        var preferencias = ((PreferenciasRepository) repository).buscarUnico();
        return preferencias == null ? TipoImpressao.LASER : TipoImpressao.doValor(preferencias.getTipoImpressao());
    }

    public void salvarTipoImpressao(TipoImpressao tipo) throws SQLException {
        if (tipo == null) throw new IllegalArgumentException("Selecione o tipo de impressão.");
        var preferencias = ((PreferenciasRepository) repository).buscarUnico();
        if (preferencias == null) {
            preferencias = new PreferenciasModel();
            preferencias.setPrimeiroAcesso(1);
            preferencias.setDataCriacao(LocalDateTime.now());
            preferencias.setTipoImpressao(tipo.valor());
            salvar(preferencias);
        } else {
            preferencias.setTipoImpressao(tipo.valor());
            atualizar(preferencias);
        }
    }

    @Override
    public PreferenciasModel salvar(PreferenciasModel model) throws SQLException {
        TipoImpressao.doValor(model.getTipoImpressao());
        return super.salvar(model);
    }

    @Override
    public void atualizar(PreferenciasModel model) throws SQLException {
        TipoImpressao.doValor(model.getTipoImpressao());
        super.atualizar(model);
    }
}

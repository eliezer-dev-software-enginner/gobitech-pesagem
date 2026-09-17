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

    public String getImagemHorizontalLogo() throws SQLException {
        var preferencias = getPreferencias();
        return preferencias == null ? null : preferencias.getImagemHorizontalLogoTipo();
    }

    public PreferenciasModel getPreferencias() throws SQLException {
        return ((PreferenciasRepository) repository).buscarUnico();
    }

    public TipoImpressao buscarTipoImpressao() throws SQLException {
        var preferencias = getPreferencias();
        return preferencias == null ? TipoImpressao.LASER : TipoImpressao.doValor(preferencias.getTipoImpressao());
    }

    /**
     * Salva tipo de impressão e logomarca horizontal como uma linha única de preferências: cria
     * quando não existe, atualiza quando já existe (a tabela tem UMA linha vinda do seed V10 —
     * insert cego duplicaria a chave/linha).
     */
    public void salvarConfiguracoes(TipoImpressao tipo, String imagemHorizontal) throws SQLException {
        if (tipo == null) throw new IllegalArgumentException("Selecione o tipo de impressão.");
        var preferencias = getPreferencias();
        var imagem = imagemHorizontal == null ? "" : imagemHorizontal;
        if (preferencias == null) {
            preferencias = new PreferenciasModel();
            preferencias.setDataCriacao(LocalDateTime.now());
            preferencias.setTipoImpressao(tipo.valor());
            preferencias.setImagemHorizontalLogoTipo(imagem);
            salvar(preferencias);
        } else {
            preferencias.setTipoImpressao(tipo.valor());
            preferencias.setImagemHorizontalLogoTipo(imagem);
            atualizar(preferencias);
        }
    }

    public void salvarTipoImpressao(TipoImpressao tipo) throws SQLException {
        if (tipo == null) throw new IllegalArgumentException("Selecione o tipo de impressão.");
        var preferencias = getPreferencias();
        var imagem = preferencias == null ? null : preferencias.getImagemHorizontalLogoTipo();
        salvarConfiguracoes(tipo, imagem);
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

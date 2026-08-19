package my_app.db.services;

import my_app.db.DB;
import my_app.db.models.ConexaoCameraModel;
import my_app.db.repositories.ConexaoCameraRepository;
import net.sf.persism.Session;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.sql.SQLException;
import java.time.LocalDateTime;

public class ConexaoCameraService extends BaseService<ConexaoCameraModel> {

    private static final Logger log = LoggerFactory.getLogger(ConexaoCameraService.class);

    public ConexaoCameraService() throws SQLException {
        this(DB.getPersismSession());
    }

    public ConexaoCameraService(Session session) {
        super(new ConexaoCameraRepository(session));
    }

    public ConexaoCameraModel buscarUnico() throws SQLException {
        return ((ConexaoCameraRepository) repository).buscarUnico();
    }

    /**
     * Diferente de {@link ConexaoBalancaService} (uma balança é sempre obrigatória), as duas
     * câmeras são opcionais e independentes uma da outra — só configurar a da frente, só a de
     * trás, ou nenhuma ainda (a captura automática na pesagem simplesmente pula a câmera sem
     * IP configurado, ver {@code PesagemViewModel}). Só valida que, se um IP foi informado, a
     * porta também foi — o resto (usuário/senha/canal) fica por conta de quem configurou.
     */
    public ConexaoCameraModel salvarOuAtualizar(ConexaoCameraModel model) throws SQLException {
        validarCampos(model);
        var existente = buscarUnico();
        if (existente == null) {
            model.setDataCriacao(LocalDateTime.now());
            var salvo = repository.salvar(model);
            log.info("Conexão das câmeras configurada: frente={} costas={}",
                    descreverCamera(salvo.getFrenteIp(), salvo.getFrentePorta()),
                    descreverCamera(salvo.getCostasIp(), salvo.getCostasPorta()));
            return salvo;
        }
        model.setId(existente.getId());
        model.setDataCriacao(existente.getDataCriacao());
        repository.atualizar(model);
        log.info("Conexão das câmeras atualizada: frente={} costas={}",
                descreverCamera(model.getFrenteIp(), model.getFrentePorta()),
                descreverCamera(model.getCostasIp(), model.getCostasPorta()));
        return model;
    }

    private String descreverCamera(String ip, Integer porta) {
        return (ip == null || ip.isBlank()) ? "não configurada" : ip + ":" + porta;
    }

    private void validarCampos(ConexaoCameraModel model) {
        if (model.getFrenteIp() != null && !model.getFrenteIp().isBlank() && model.getFrentePorta() == null) {
            throw new IllegalArgumentException("Porta da câmera da frente é obrigatória quando o IP é informado");
        }
        if (model.getCostasIp() != null && !model.getCostasIp().isBlank() && model.getCostasPorta() == null) {
            throw new IllegalArgumentException("Porta da câmera de trás é obrigatória quando o IP é informado");
        }
    }
}

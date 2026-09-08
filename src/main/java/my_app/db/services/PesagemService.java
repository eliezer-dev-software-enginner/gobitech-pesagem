package my_app.db.services;

import my_app.db.DB;
import my_app.db.models.PesagemModel;
import my_app.db.repositories.ClienteRepository;
import my_app.db.repositories.DescontoRepository;
import my_app.db.repositories.PesagemRepository;
import my_app.db.repositories.ProdutoRepository;
import my_app.db.repositories.UsuarioRepository;
import net.sf.persism.Session;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import pack.utilities.ValidatorPack;

import java.math.BigDecimal;
import java.sql.SQLException;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.List;

public class PesagemService extends BaseService<PesagemModel> {

    private static final Logger log = LoggerFactory.getLogger(PesagemService.class);

    private final PesagemRepository pesagemRepository;
    private final ClienteRepository clienteRepository;
    private final ProdutoRepository produtoRepository;
    private final DescontoRepository descontoRepository;
    private final UsuarioRepository usuarioRepository;

    public PesagemService() throws SQLException {
        this(DB.getPersismSession());
    }

    public PesagemService(Session session) {
        super(new PesagemRepository(session));
        this.pesagemRepository = (PesagemRepository) repository;
        this.clienteRepository = new ClienteRepository(session);
        this.produtoRepository = new ProdutoRepository(session);
        this.descontoRepository = new DescontoRepository(session);
        this.usuarioRepository = new UsuarioRepository(session);
    }

    @Override
    public PesagemModel salvar(PesagemModel model) throws SQLException {
        validarCampos(model);
        model.setDataCriacao(LocalDateTime.now());
        var salvo = repository.salvar(model);
        log.info("Pesagem salva: id={} placa={} tipo={} pesoLiquido={}",
                salvo.getId(), salvo.getPlaca(), salvo.getTipoPesagem(), salvo.getPesoFinal());
        return salvo;
    }

    @Override
    public void atualizar(PesagemModel model) throws SQLException {
        validarCampos(model);
        repository.atualizar(model);
        log.info("Pesagem atualizada: id={} placa={} pesoLiquido={}", model.getId(), model.getPlaca(), model.getPesoFinal());
    }

    /**
     * Última pesagem de tipo "entrada" daquela placa — a base pra pesagem de "saída"
     * (aplica-se sempre, não por paridade de visita): a tela de Saída puxa dela os dados do
     * caminhão/motorista/cliente/produto e a Tara, sem precisar redigitar nem repesar vazio.
     * {@code null} se a placa nunca teve uma Entrada registrada.
     */
    public PesagemModel buscarUltimaEntrada(String placa) throws SQLException {
        if (placa == null || placa.isBlank()) return null;
        var anteriores = pesagemRepository.buscarPorPlacaETipo(placa, "entrada");
        return anteriores.isEmpty() ? null : anteriores.getLast();
    }

    /**
     * Tara sugerida pra uma pesagem de "saída": a da última Entrada daquela placa. {@code null}
     * se a placa não tem Entrada (a próxima pesagem seria uma Entrada, sem sugestão) ou se essa
     * Entrada não tinha Tara preenchida.
     */
    public BigDecimal buscarTaraSugerida(String placa) throws SQLException {
        var entrada = buscarUltimaEntrada(placa);
        return entrada == null ? null : entrada.getPesoVeiculo();
    }

    public PesagemModel buscarComRelacoes(long id) throws SQLException {
        var pesagem = repository.buscarById(id);
        if (pesagem != null) anexarRelacoes(pesagem);
        return pesagem;
    }

    public List<PesagemModel> listarComRelacoes() throws SQLException {
        var lista = repository.listar();
        for (var pesagem : lista) anexarRelacoes(pesagem);
        return lista;
    }

    public List<PesagemModel> filtrar(String placa, String motoristaNome, Integer clienteId,
                                       Integer produtoId, LocalDate dataInicio, LocalDate dataFim,
                                       String tipoPesagem) throws SQLException {
        var lista = pesagemRepository.filtrar(placa, motoristaNome, clienteId, produtoId, dataInicio, dataFim, tipoPesagem);
        for (var pesagem : lista) anexarRelacoes(pesagem);
        return lista;
    }

    private void anexarRelacoes(PesagemModel pesagem) throws SQLException {
        if (pesagem.getClienteId() != null) {
            pesagem.setCliente(clienteRepository.buscarById(pesagem.getClienteId()));
        }
        if (pesagem.getProdutoId() != null) {
            pesagem.setProduto(produtoRepository.buscarById(pesagem.getProdutoId()));
        }
        if (pesagem.getDescontoId() != null) {
            pesagem.setDesconto(descontoRepository.buscarById(pesagem.getDescontoId()));
        }
        if (pesagem.getUsuarioId() != null) {
            pesagem.setUsuario(usuarioRepository.buscarById(pesagem.getUsuarioId()));
        }
    }

    /**
     * A pesagem de entrada vinculada a uma saída (via {@code entrada_id}) — traz data/hora e
     * peso entrada pro ticket. {@code null} se não houver (entrada/avulsa/manual sem par).
     */
    public PesagemModel buscarEntradaVinculada(PesagemModel pesagem) throws SQLException {
        if (pesagem.getEntradaId() == null) return null;
        var entrada = repository.buscarById(pesagem.getEntradaId());
        if (entrada != null) anexarRelacoes(entrada);
        return entrada;
    }

    private void validarCampos(PesagemModel model) {
        if (model.getPlaca() == null || model.getPlaca().isBlank())
            throw new IllegalArgumentException("Placa é obrigatória");
        if (model.getTipoPesagem() == null || model.getTipoPesagem().isBlank())
            throw new IllegalArgumentException("Tipo de pesagem é obrigatório");
        if (model.getMotoristaDocumento() != null && !model.getMotoristaDocumento().isBlank()
                && !ValidatorPack.isValidDocumento(model.getMotoristaDocumento()))
            throw new IllegalArgumentException("Documento do motorista inválido (informe um RG ou CPF válido).");
        if (model.getMotoristaNome() != null && model.getMotoristaNome().length() > 100)
            throw new IllegalArgumentException("Nome do motorista excede o limite de 100 caracteres.");
        if (model.getPesoVeiculo() == null) model.setPesoVeiculo(BigDecimal.ZERO);
        if (model.getPesoTotal() == null) model.setPesoTotal(BigDecimal.ZERO);
        if (model.getPesoFinal() == null) model.setPesoFinal(BigDecimal.ZERO);
        // "Bruto < Tara" quebra o próprio fluxo "Só Tara" (C1): tara preenchida sem bruto vira
        // bruto=0 e o usuário pode registrar Entrada só com o caminhão vazio. A regra só
        // dispara quando os DOIS pesos preenchidos (mesma condição da ViewModel).
        if (model.getPesoTotal().signum() > 0 && model.getPesoVeiculo().signum() > 0
                && model.getPesoTotal().compareTo(model.getPesoVeiculo()) < 0)
            throw new IllegalArgumentException("Peso bruto não pode ser menor que a Tara (peso líquido estaria negativo).");
    }
}

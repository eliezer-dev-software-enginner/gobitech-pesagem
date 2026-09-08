package my_app.db.services;

import my_app.db.DB;
import my_app.db.models.ClienteModel;
import my_app.db.models.DescontoModel;
import my_app.db.models.PesagemModel;
import my_app.db.models.ProdutoModel;
import my_app.db.models.UsuarioModel;
import my_app.db.repositories.ClienteRepository;
import my_app.db.repositories.DescontoRepository;
import my_app.db.repositories.PesagemRepository;
import my_app.db.repositories.ProdutoRepository;
import my_app.db.repositories.UsuarioRepository;
import my_app.core.Identifier;
import net.sf.persism.Session;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import pack.utilities.ValidatorPack;

import java.math.BigDecimal;
import java.sql.SQLException;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;

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
        if (pesagem != null) anexarRelacoes(List.of(pesagem));
        return pesagem;
    }

    public List<PesagemModel> listarComRelacoes() throws SQLException {
        var lista = repository.listar();
        anexarRelacoes(lista);
        return lista;
    }

    public List<PesagemModel> filtrar(String placa, String motoristaNome, Integer clienteId,
                                       Integer produtoId, LocalDate dataInicio, LocalDate dataFim,
                                       String tipoPesagem) throws SQLException {
        var lista = pesagemRepository.filtrar(placa, motoristaNome, clienteId, produtoId, dataInicio, dataFim, tipoPesagem);
        anexarRelacoes(lista);
        return lista;
    }

    /** Total de pesagens de um período (datas inclusivas) — sem anexar relações nem trafegar linhas. */
    public long contarPorPeriodo(LocalDate dataInicio, LocalDate dataFim) throws SQLException {
        return pesagemRepository.contarPorPeriodo(dataInicio, dataFim);
    }

    /**
     * Anexa Cliente/Produto/Desconto/Usuario a uma lista inteira com 4 SELECTs em lote
     * ({@code WHERE id IN (...)}) no lugar de N×4 SELECTs individuais (N+1 do M20).
     */
    public void anexarRelacoes(List<PesagemModel> pesagens) throws SQLException {
        if (pesagens == null || pesagens.isEmpty()) return;

        Set<Integer> clienteIds = new HashSet<>();
        Set<Integer> produtoIds = new HashSet<>();
        Set<Integer> descontoIds = new HashSet<>();
        Set<Integer> usuarioIds = new HashSet<>();
        for (var pesagem : pesagens) {
            if (pesagem.getClienteId() != null) clienteIds.add(pesagem.getClienteId());
            if (pesagem.getProdutoId() != null) produtoIds.add(pesagem.getProdutoId());
            if (pesagem.getDescontoId() != null) descontoIds.add(pesagem.getDescontoId());
            if (pesagem.getUsuarioId() != null) usuarioIds.add(pesagem.getUsuarioId());
        }

        Map<Integer, ClienteModel> clientes = emMapa(clienteRepository.buscarPorIds(clienteIds));
        Map<Integer, ProdutoModel> produtos = emMapa(produtoRepository.buscarPorIds(produtoIds));
        Map<Integer, DescontoModel> descontos = emMapa(descontoRepository.buscarPorIds(descontoIds));
        Map<Integer, UsuarioModel> usuarios = emMapa(usuarioRepository.buscarPorIds(usuarioIds));

        for (var pesagem : pesagens) {
            if (pesagem.getClienteId() != null) pesagem.setCliente(clientes.get(pesagem.getClienteId()));
            if (pesagem.getProdutoId() != null) pesagem.setProduto(produtos.get(pesagem.getProdutoId()));
            if (pesagem.getDescontoId() != null) pesagem.setDesconto(descontos.get(pesagem.getDescontoId()));
            if (pesagem.getUsuarioId() != null) pesagem.setUsuario(usuarios.get(pesagem.getUsuarioId()));
        }
    }

    private static <M extends Identifier> Map<Integer, M> emMapa(List<M> modelos) {
        var mapa = new HashMap<Integer, M>();
        for (var modelo : modelos) mapa.put(modelo.getId(), modelo);
        return mapa;
    }

    /**
     * A pesagem de entrada vinculada a uma saída (via {@code entrada_id}) — traz data/hora e
     * peso entrada pro ticket. {@code null} se não houver (entrada/avulsa/manual sem par).
     */
    public PesagemModel buscarEntradaVinculada(PesagemModel pesagem) throws SQLException {
        if (pesagem.getEntradaId() == null) return null;
        var entrada = repository.buscarById(pesagem.getEntradaId());
        if (entrada != null) anexarRelacoes(List.of(entrada));
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

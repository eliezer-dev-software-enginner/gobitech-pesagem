package my_app.db.services;

import my_app.db.DB;
import my_app.db.models.PesagemModel;
import my_app.db.repositories.ClienteRepository;
import my_app.db.repositories.DescontoRepository;
import my_app.db.repositories.PesagemRepository;
import my_app.db.repositories.ProdutoRepository;
import net.sf.persism.Session;

import java.math.BigDecimal;
import java.sql.SQLException;
import java.time.LocalDateTime;
import java.util.List;

public class PesagemService extends BaseService<PesagemModel> {

    private final PesagemRepository pesagemRepository;
    private final ClienteRepository clienteRepository;
    private final ProdutoRepository produtoRepository;
    private final DescontoRepository descontoRepository;

    public PesagemService() throws SQLException {
        this(DB.getPersismSession());
    }

    public PesagemService(Session session) {
        super(new PesagemRepository(session));
        this.pesagemRepository = (PesagemRepository) repository;
        this.clienteRepository = new ClienteRepository(session);
        this.produtoRepository = new ProdutoRepository(session);
        this.descontoRepository = new DescontoRepository(session);
    }

    @Override
    public PesagemModel salvar(PesagemModel model) throws SQLException {
        validarCampos(model);
        if (model.getOperacao() == null || model.getOperacao().isBlank()) {
            model.setOperacao(determinarOperacao(model.getPlaca()));
        }
        model.setDataCriacao(LocalDateTime.now());
        return repository.salvar(model);
    }

    @Override
    public void atualizar(PesagemModel model) throws SQLException {
        validarCampos(model);
        repository.atualizar(model);
    }

    /**
     * "Entrada" se não houver pesagem em aberto pra essa placa; "Saída" se houver
     * (número ímpar de pesagens já registradas pra ela) — mesma regra do app original,
     * só que isolada aqui em vez de espalhada na tela.
     */
    public String determinarOperacao(String placa) throws SQLException {
        var anteriores = pesagemRepository.buscarPorPlaca(placa);
        boolean temPesagemEmAberto = !anteriores.isEmpty() && anteriores.size() % 2 != 0;
        return temPesagemEmAberto ? "Saída" : "Entrada";
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
                                       Integer produtoId, Long dataInicioMillis, Long dataFimMillis) throws SQLException {
        var lista = pesagemRepository.filtrar(placa, motoristaNome, clienteId, produtoId, dataInicioMillis, dataFimMillis);
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
    }

    private void validarCampos(PesagemModel model) {
        if (model.getMotoristaNome() == null || model.getMotoristaNome().isBlank())
            throw new IllegalArgumentException("Nome do motorista é obrigatório");
        if (model.getPlaca() == null || model.getPlaca().isBlank())
            throw new IllegalArgumentException("Placa é obrigatória");
        if (model.getClienteId() == null)
            throw new IllegalArgumentException("Cliente é obrigatório");
        if (model.getProdutoId() == null)
            throw new IllegalArgumentException("Produto é obrigatório");
        if (model.getPesoVeiculo() == null) model.setPesoVeiculo(BigDecimal.ZERO);
        if (model.getPesoTotal() == null) model.setPesoTotal(BigDecimal.ZERO);
        if (model.getPesoFinal() == null) model.setPesoFinal(BigDecimal.ZERO);
    }
}

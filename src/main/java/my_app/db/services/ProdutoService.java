package my_app.db.services;

import my_app.db.DB;
import my_app.db.models.ProdutoModel;
import my_app.db.repositories.ProdutoRepository;
import net.sf.persism.Session;

import java.math.BigDecimal;
import java.sql.SQLException;
import java.time.LocalDateTime;

public class ProdutoService extends BaseService<ProdutoModel> {

    private final ProdutoRepository produtoRepository;

    public ProdutoService() throws SQLException {
        this(DB.getPersismSession());
    }

    public ProdutoService(Session session) {
        super(new ProdutoRepository(session));
        this.produtoRepository = (ProdutoRepository) repository;
    }

    @Override
    public ProdutoModel salvar(ProdutoModel model) throws SQLException {
        validar(model);
        if (model.getAtivo() == null) model.setAtivo(true);
        if (model.getDesconto() == null) model.setDesconto(BigDecimal.ZERO);
        model.setDataCriacao(LocalDateTime.now());
        return repository.salvar(model);
    }

    @Override
    public void atualizar(ProdutoModel model) throws SQLException {
        validar(model);
        if (model.getDesconto() == null) model.setDesconto(BigDecimal.ZERO);
        repository.atualizar(model);
    }

    private void validar(ProdutoModel model) throws SQLException {
        if (model.getNome() == null || model.getNome().isBlank())
            throw new IllegalArgumentException("Nome do produto é obrigatório");

        var existente = produtoRepository.buscarPorNome(model.getNome().trim());
        if (existente != null && !existente.getId().equals(model.getId()))
            throw new IllegalArgumentException("Já existe um produto cadastrado com esse nome");
    }

    public ProdutoModel buscarPorNome(String nome) throws SQLException {
        return produtoRepository.buscarPorNome(nome);
    }
}

package my_app.db.services;

import my_app.db.models.ProdutoModel;
import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.*;

class ProdutoServiceTest extends BaseServiceTest {

    private ProdutoService produtoService;

    @Override
    protected void initService() {
        produtoService = new ProdutoService(session);
    }

    private ProdutoModel produtoValido() {
        var p = new ProdutoModel();
        p.setNome("Soja");
        p.setUnidade("SC");
        p.setObservacoes("Observação de teste");
        return p;
    }

    @Test
    void deveLancarExcecaoQuandoNomeVazio() {
        var p = produtoValido();
        p.setNome("");
        assertThrows(IllegalArgumentException.class, () -> produtoService.salvar(p));
    }

    @Test
    void deveLancarExcecaoQuandoNomeEmBranco() {
        var p = produtoValido();
        p.setNome(" ");
        assertThrows(IllegalArgumentException.class, () -> produtoService.salvar(p));
    }

    @Test
    void deveSalvarProduto() throws Exception {
        var salvo = produtoService.salvar(produtoValido());
        assertNotNull(salvo.getId());
        assertNotNull(salvo.getDataCriacao());
        assertEquals(true, salvo.getAtivo());
    }

    @Test
    void deveAtualizarProduto() throws Exception {
        var salvo = produtoService.salvar(produtoValido());
        salvo.setUnidade("KG");
        produtoService.atualizar(salvo);
        var buscado = produtoService.buscarById(salvo.getId());
        assertEquals("KG", buscado.getUnidade());
    }

    @Test
    void deveBuscarPorNome() throws Exception {
        produtoService.salvar(produtoValido());
        var encontrado = produtoService.buscarPorNome("Soja");
        assertNotNull(encontrado);
        assertEquals("SC", encontrado.getUnidade());
    }

    @Test
    void deveLancarExcecaoQuandoNomeDuplicado() throws Exception {
        produtoService.salvar(produtoValido());
        var p2 = produtoValido();
        assertThrows(IllegalArgumentException.class, () -> produtoService.salvar(p2));
    }

    @Test
    void devePermitirAtualizarProdutoMantendoNome() throws Exception {
        var salvo = produtoService.salvar(produtoValido());
        salvo.setObservacoes("Observação editada");
        assertDoesNotThrow(() -> produtoService.atualizar(salvo));
    }
}

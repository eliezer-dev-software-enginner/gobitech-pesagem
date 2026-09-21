package my_app.core;

import megalodonte.base.route.Route;
import megalodonte.base.route.RouteProps;
import megalodonte.router.v5.Router;
import my_app.Main;
import my_app.SplashScreen;
import my_app.screens.clienteScreen.AddOrEditClienteScreen;
import my_app.screens.authScreen.AuthScreen;
import my_app.screens.clienteScreen.ClienteScreen;
import my_app.screens.clienteScreen.DetailsClienteScreen;
import my_app.screens.conexaoBalancaScreen.ConexaoBalancaScreen;
import my_app.screens.conexaoCameraScreen.ConexaoCameraScreen;
import my_app.screens.configuracoesScreen.ConfiguracoesScreen;
import my_app.screens.empresaScreen.CadastroEmpresaScreen;
import my_app.screens.homeScreen.HomeScreen;
import my_app.screens.licensaScreen.LicensaScreen;
import my_app.screens.logsScreen.LogsScreen;
import my_app.screens.pesagemScreen.*;
import my_app.screens.produtoScreen.AddOrEditProdutoScreen;
import my_app.screens.produtoScreen.DetailsProdutoScreen;
import my_app.screens.produtoScreen.ProdutoScreen;
import my_app.screens.usuarioScreen.AddOrEditUsuarioScreen;
import my_app.screens.usuarioScreen.DetailsUsuarioScreen;
import my_app.screens.usuarioScreen.UsuarioScreen;

import java.util.Set;

public class AppRoutes {
    public enum Screens {
        SPLASH,
        AUTH,
        HOME,
        PRODUTOS,
        USUARIOS,
        EMPRESA,
        CLIENTES,
        CONEXAO_BALANCA,
        CONEXAO_CAMERA,
        CONFIGURACOES,
        LICENSA,
        LOGS,
        ADD_OR_EDIT_PRODUTO,
        ADD_OR_EDIT_CLIENTE,
        ADD_OR_EDIT_USUARIO,
        DETAILS_PRODUTO,
        DETAILS_CLIENTE,
        DETAILS_USUARIO,
        DETAILS_PESAGEM,
        PESAGEM_ENTRADA,
        PESAGEM_SAIDA,
        PESAGEM_AVULSA,
        PESAGEM_MANUAL,
        PESAGEM_HISTORICO
    }

    final int MIN_WIDTH = 600;
    final int MIN_HEIGHT = 500;

    final int MEDIUM_WIDTH = 950;
    final int MEDIUM_HEIGHT = 530;

    final int MAX_WIDTH = 1200;
    final int MAX_HEIGHT = 620;

    public Set<Route> routes() {
        return Set.of(
                new Route(Screens.SPLASH.name(), ctx -> new SplashScreen(),
                        new RouteProps(MIN_WIDTH-100, MIN_HEIGHT-100, Main.BASE_TITLE, false)),
                new Route(Screens.AUTH.name(), AuthScreen::new, new RouteProps(MEDIUM_WIDTH, MEDIUM_HEIGHT, "Seja muito bem vindo", false)),
                new Route(Screens.HOME.name(), HomeScreen::new, new RouteProps(MAX_WIDTH, MAX_HEIGHT, Main.BASE_TITLE, true)),
                new Route(Screens.PRODUTOS.name(), ProdutoScreen::new, new RouteProps(MAX_WIDTH, MEDIUM_HEIGHT, "Cadastro de produtos", true)),
                new Route(Screens.ADD_OR_EDIT_PRODUTO.name()+"/${id}/${type}", AddOrEditProdutoScreen::new, new RouteProps(MEDIUM_WIDTH, MEDIUM_HEIGHT, "Gerenciar produto", true)),
                new Route(Screens.ADD_OR_EDIT_CLIENTE.name()+"/${id}/${type}", AddOrEditClienteScreen::new, new RouteProps(MEDIUM_WIDTH, MEDIUM_HEIGHT, "Gerenciar cliente", true)),
                new Route(Screens.ADD_OR_EDIT_USUARIO.name()+"/${id}/${type}", AddOrEditUsuarioScreen::new, new RouteProps(MEDIUM_WIDTH, MEDIUM_HEIGHT, "Gerenciar usuário", true)),
                new Route(Screens.DETAILS_PRODUTO.name()+"/${id}", DetailsProdutoScreen::new, new RouteProps(MEDIUM_WIDTH, MEDIUM_HEIGHT, "Detalhes do produto", true)),
                new Route(Screens.DETAILS_CLIENTE.name()+"/${id}", DetailsClienteScreen::new, new RouteProps(MEDIUM_WIDTH, MEDIUM_HEIGHT, "Detalhes do cliente", true)),
                new Route(Screens.DETAILS_USUARIO.name()+"/${id}", DetailsUsuarioScreen::new, new RouteProps(MEDIUM_WIDTH, MEDIUM_HEIGHT, "Detalhes do usuário", true)),
                new Route(Screens.DETAILS_PESAGEM.name()+"/${id}", DetailsPesagemScreen::new, new RouteProps(MAX_WIDTH, MEDIUM_HEIGHT, "Detalhes da pesagem", true)),
                new Route(Screens.USUARIOS.name(), UsuarioScreen::new, new RouteProps(MAX_WIDTH, MAX_HEIGHT, "Gerenciamento de usuários", true)),
                new Route(Screens.EMPRESA.name(), CadastroEmpresaScreen::new, new RouteProps(MEDIUM_WIDTH, MEDIUM_HEIGHT, "Informações da empresa", false)),
                new Route(Screens.CLIENTES.name(), ClienteScreen::new, new RouteProps(MAX_WIDTH, MEDIUM_HEIGHT, "Gerenciamento de clientes", true)),
                new Route(Screens.CONEXAO_BALANCA.name(), ConexaoBalancaScreen::new, new RouteProps(MEDIUM_WIDTH, MEDIUM_HEIGHT, "Conexão com a balança", false)),
                new Route(Screens.CONEXAO_CAMERA.name(), ConexaoCameraScreen::new, new RouteProps(MAX_WIDTH, MEDIUM_HEIGHT, "Conexão das câmeras", false)),
                new Route(Screens.CONFIGURACOES.name(), ConfiguracoesScreen::new, new RouteProps(MEDIUM_WIDTH, MEDIUM_HEIGHT, "Configurações", false)),
                new Route(Screens.LICENSA.name(), LicensaScreen::new, new RouteProps(MEDIUM_WIDTH, MEDIUM_HEIGHT, "Gerar licença", false)),
                new Route(Screens.LOGS.name(), LogsScreen::new, new RouteProps(MAX_WIDTH, MAX_HEIGHT, "Logs da aplicação", true)),
                new Route(Screens.PESAGEM_ENTRADA.name(), PesagemEntradaScreen::new, new RouteProps(MIN_WIDTH, MAX_HEIGHT, "Pesagem de entrada", true)),
                new Route(Screens.PESAGEM_SAIDA.name(), PesagemSaidaScreen::new, new RouteProps(MIN_WIDTH, MAX_HEIGHT, "Pesagem de saída", true)),
                new Route(Screens.PESAGEM_AVULSA.name(), PesagemAvulsaScreen::new, new RouteProps(MIN_WIDTH, MAX_HEIGHT, "Pesagem avulsa", true)),
                new Route(Screens.PESAGEM_MANUAL.name(), PesagemManualScreen::new, new RouteProps(MIN_WIDTH, MAX_HEIGHT, "Pesagem manual", true)),
                new Route(Screens.PESAGEM_HISTORICO.name(), PesagemHistoricoScreen::new, new RouteProps(MIN_WIDTH, MAX_HEIGHT, "Histórico de pesagens", true))
        );
    }
}

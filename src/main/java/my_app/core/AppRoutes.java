package my_app.core;

import megalodonte.base.route.RouteProps;
import megalodonte.router.v4.Router;
import my_app.Main;
import my_app.SplashScreen;
import my_app.screens.acessoBloqueadoScreen.AcessoBloqueadoScreen;
import my_app.screens.clienteScreen.AddOrEditClienteScreen;
import my_app.screens.authScreen.AuthScreen;
import my_app.screens.clienteScreen.ClienteScreen;
import my_app.screens.clienteScreen.DetailsClienteScreen;
import my_app.screens.conexaoBalancaScreen.ConexaoBalancaScreen;
import my_app.screens.conexaoCameraScreen.ConexaoCameraScreen;
import my_app.screens.empresaScreen.CadastroEmpresaScreen;
import my_app.screens.homeScreen.HomeScreen;
import my_app.screens.licensaScreen.LicensaScreen;
import my_app.screens.logsScreen.LogsScreen;
import my_app.screens.pesagemScreen.DetailsPesagemScreen;
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
        LICENSA,
        LOGS,
        ACESSO_BLOQUEADO,
        ADD_OR_EDIT_PRODUTO,
        ADD_OR_EDIT_CLIENTE,
        ADD_OR_EDIT_USUARIO,
        DETAILS_PRODUTO,
        DETAILS_CLIENTE,
        DETAILS_USUARIO,
        DETAILS_PESAGEM,
    }

    final int MIN_WIDTH = 600;
    final int MIN_HEIGHT = 500;

    final int MEDIUM_WIDTH = 950;
    final int MEDIUM_HEIGHT = 530;

    final int MAX_WIDTH = 1200;
    final int MAX_HEIGHT = 620;

    public Set<Router.Route> routes() {
        return Set.of(
                new Router.Route(Screens.SPLASH.name(), ctx -> new SplashScreen(),
                        new RouteProps(MIN_WIDTH-100, MIN_HEIGHT-100, Main.BASE_TITLE, false)),
                new Router.Route(Screens.AUTH.name(), AuthScreen::new, new RouteProps(MEDIUM_WIDTH, MEDIUM_HEIGHT, "Seja muito bem vindo", false)),
                new Router.Route(Screens.HOME.name(), HomeScreen::new, new RouteProps(MAX_WIDTH, MAX_HEIGHT, Main.BASE_TITLE, true)),
                new Router.Route(Screens.PRODUTOS.name(), ProdutoScreen::new, new RouteProps(MAX_WIDTH, MEDIUM_HEIGHT, "Cadastro de produtos", true)),
                new Router.Route(Screens.ADD_OR_EDIT_PRODUTO.name()+"/${id}/${type}", AddOrEditProdutoScreen::new, new RouteProps(MEDIUM_WIDTH, MEDIUM_HEIGHT, "Gerenciar produto", true)),
                new Router.Route(Screens.ADD_OR_EDIT_CLIENTE.name()+"/${id}/${type}", AddOrEditClienteScreen::new, new RouteProps(MEDIUM_WIDTH, MEDIUM_HEIGHT, "Gerenciar cliente", true)),
                new Router.Route(Screens.ADD_OR_EDIT_USUARIO.name()+"/${id}/${type}", AddOrEditUsuarioScreen::new, new RouteProps(MEDIUM_WIDTH, MEDIUM_HEIGHT, "Gerenciar usuário", true)),
                new Router.Route(Screens.DETAILS_PRODUTO.name()+"/${id}", DetailsProdutoScreen::new, new RouteProps(MEDIUM_WIDTH, MEDIUM_HEIGHT, "Detalhes do produto", true)),
                new Router.Route(Screens.DETAILS_CLIENTE.name()+"/${id}", DetailsClienteScreen::new, new RouteProps(MEDIUM_WIDTH, MEDIUM_HEIGHT, "Detalhes do cliente", true)),
                new Router.Route(Screens.DETAILS_USUARIO.name()+"/${id}", DetailsUsuarioScreen::new, new RouteProps(MEDIUM_WIDTH, MEDIUM_HEIGHT, "Detalhes do usuário", true)),
                new Router.Route(Screens.DETAILS_PESAGEM.name()+"/${id}", DetailsPesagemScreen::new, new RouteProps(MAX_WIDTH, MEDIUM_HEIGHT, "Detalhes da pesagem", true)),
                new Router.Route(Screens.USUARIOS.name(), UsuarioScreen::new, new RouteProps(MAX_WIDTH, MAX_HEIGHT, "Gerenciamento de usuários", true)),
                new Router.Route(Screens.EMPRESA.name(), CadastroEmpresaScreen::new, new RouteProps(MEDIUM_WIDTH, MEDIUM_HEIGHT, "Informações da empresa", false)),
                new Router.Route(Screens.CLIENTES.name(), ClienteScreen::new, new RouteProps(MAX_WIDTH, MEDIUM_HEIGHT, "Gerenciamento de clientes", true)),
                new Router.Route(Screens.CONEXAO_BALANCA.name(), ConexaoBalancaScreen::new, new RouteProps(MEDIUM_WIDTH, MEDIUM_HEIGHT, "Conexão com a balança", false)),
                new Router.Route(Screens.CONEXAO_CAMERA.name(), ConexaoCameraScreen::new, new RouteProps(MAX_WIDTH, MEDIUM_HEIGHT, "Conexão das câmeras", false)),
                new Router.Route(Screens.LICENSA.name(), LicensaScreen::new, new RouteProps(MEDIUM_WIDTH, MEDIUM_HEIGHT, "Gerar licença", false)),
                new Router.Route(Screens.LOGS.name(), LogsScreen::new, new RouteProps(MAX_WIDTH, MAX_HEIGHT, "Logs da aplicação", true)),
                new Router.Route(Screens.ACESSO_BLOQUEADO.name(), ctx -> new AcessoBloqueadoScreen(),
                        new RouteProps(MIN_WIDTH, MIN_HEIGHT, "Acesso bloqueado", false))
        );
    }
}

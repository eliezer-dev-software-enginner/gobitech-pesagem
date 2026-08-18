package my_app.core;

import megalodonte.base.route.RouteProps;
import megalodonte.router.v4.Router;
import my_app.Main;
import my_app.SplashScreen;
import my_app.screens.acessoBloqueadoScreen.AcessoBloqueadoScreen;
import my_app.screens.welcomeScreen.WelcomeScreen;
import my_app.screens.infoUpdateScreen.InfoUpdateScreen;
import my_app.screens.feedbackScreen.RelatarErroScreen;
import my_app.screens.feedbackScreen.SugerirMelhoriaScreen;
import my_app.screens.authScreen.AuthScreen;
import my_app.screens.clienteScreen.ClienteScreen;
import my_app.screens.conexaoBalancaScreen.ConexaoBalancaScreen;
import my_app.screens.empresaScreen.CadastroEmpresaScreen;
import my_app.screens.homeScreen.HomeScreen;
import my_app.screens.licensaScreen.LicensaScreen;
import my_app.screens.pesagemScreen.PesagemScreen;
import my_app.screens.produtoScreen.ProdutoScreen;
import my_app.screens.usuarioScreen.UsuarioScreen;

import java.util.Set;

public class AppRoutes {
    public enum Screens {
        SPLASH,
        WELCOME,
        AUTH,
        HOME,
        PRODUTOS,
        USUARIOS,
        EMPRESA,
        CLIENTES,
        PESAGENS,
        CONEXAO_BALANCA,
        LICENSA,
        RELATAR_ERRO,
        SUGERIR_MELHORIA,
        INFO_UPDATE,
        ACESSO_BLOQUEADO
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
                        new RouteProps(MIN_WIDTH, MIN_HEIGHT, Main.BASE_TITLE, false)),
                new Router.Route(Screens.WELCOME.name(), WelcomeScreen::new, new RouteProps(MEDIUM_WIDTH, MEDIUM_HEIGHT, Main.BASE_TITLE, true)),
                new Router.Route(Screens.AUTH.name(), AuthScreen::new, new RouteProps(MEDIUM_WIDTH, MEDIUM_HEIGHT, "Seja muito bem vindo", true)),
                new Router.Route(Screens.HOME.name(), HomeScreen::new, new RouteProps(MAX_WIDTH, MAX_HEIGHT, Main.BASE_TITLE, true)),
                new Router.Route(Screens.PRODUTOS.name(), ProdutoScreen::new, new RouteProps(MAX_WIDTH, MAX_HEIGHT, "Cadastro de produtos", true)),
                new Router.Route(Screens.USUARIOS.name(), UsuarioScreen::new, new RouteProps(MAX_WIDTH, MAX_HEIGHT, "Gerenciamento de usuários", true)),
                new Router.Route(Screens.EMPRESA.name(), CadastroEmpresaScreen::new, new RouteProps(MEDIUM_WIDTH, MEDIUM_HEIGHT, "Informações da empresa", false)),
                new Router.Route(Screens.CLIENTES.name(), ClienteScreen::new, new RouteProps(MAX_WIDTH, MAX_HEIGHT, "Gerenciamento de clientes", true)),
                new Router.Route(Screens.PESAGENS.name(), PesagemScreen::new, new RouteProps(MAX_WIDTH, MAX_HEIGHT, "Pesagens", true)),
                new Router.Route(Screens.CONEXAO_BALANCA.name(), ConexaoBalancaScreen::new, new RouteProps(MEDIUM_WIDTH, MEDIUM_HEIGHT, "Conexão com a balança", false)),
                new Router.Route(Screens.LICENSA.name(), LicensaScreen::new, new RouteProps(MEDIUM_WIDTH, MEDIUM_HEIGHT, "Gerar licença", false)),
                new Router.Route(Screens.RELATAR_ERRO.name(), RelatarErroScreen::new, new RouteProps(MIN_WIDTH, MIN_HEIGHT, "Relatar erros", true)),
                new Router.Route(Screens.SUGERIR_MELHORIA.name(), SugerirMelhoriaScreen::new, new RouteProps(MIN_WIDTH, MIN_HEIGHT, "Detalhes de melhoria ou funcionalidades a serem sugeridas", true)),
                new Router.Route(Screens.INFO_UPDATE.name(), InfoUpdateScreen::new, new RouteProps(MAX_WIDTH, MAX_HEIGHT, "Atualizações do aplicativo", false)),
                new Router.Route(Screens.ACESSO_BLOQUEADO.name(), ctx -> new AcessoBloqueadoScreen(),
                        new RouteProps(MIN_WIDTH, MIN_HEIGHT, "Acesso bloqueado", false))
        );
    }
}

package my_app.domain;

import my_app.db.models.UsuarioModel;

/**
 * Guarda o usuário autenticado na sessão atual do app (um processo = um login).
 * Setado por AuthScreenViewModel após autenticar; usado pra decidir quem pode ver/
 * acessar telas restritas a admin (ex: gerar licença).
 */
public class SessaoUsuario {
    private static UsuarioModel usuarioLogado;

    public static void login(UsuarioModel usuario) {
        usuarioLogado = usuario;
    }

    public static void logout() {
        usuarioLogado = null;
    }

    public static UsuarioModel usuarioLogado() {
        return usuarioLogado;
    }

    public static boolean isAdmin() {
        return usuarioLogado != null && Boolean.TRUE.equals(usuarioLogado.getAdmin());
    }
}

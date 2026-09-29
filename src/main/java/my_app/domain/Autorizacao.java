package my_app.domain;

/** Regras centrais de autorização da sessão autenticada. */
public final class Autorizacao {
    private Autorizacao() { }

    public static boolean podeAdministrar() {
        return SessaoUsuario.isAdmin();
    }

    public static void exigirAdmin(String acao) {
        if (!podeAdministrar()) {
            throw new IllegalArgumentException("Somente administradores podem " + acao + ".");
        }
    }

    public static boolean usuarioLogado(long usuarioId) {
        var usuario = SessaoUsuario.usuarioLogado();
        return usuario != null && usuario.getId() != null && usuario.getId().longValue() == usuarioId;
    }
}

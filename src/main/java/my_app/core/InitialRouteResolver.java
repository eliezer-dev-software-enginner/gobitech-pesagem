package my_app.core;

import my_app.Main;

public class InitialRouteResolver {

    /**
     * Login é sempre obrigatório (cada usuário tem sua própria conta —
     * ver AppRoutes/AuthScreen). WELCOME só aparece uma vez, no primeiro acesso.
     */
    public static String resolve(boolean isFirstAccess) {
        if (isFirstAccess) {
            return AppRoutes.Screens.WELCOME.name();
        }
        return AppRoutes.Screens.AUTH.name();
    }
}
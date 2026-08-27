package my_app.core;

import my_app.Main;

public class InitialRouteResolver {
    public static String resolve() {
        if(Main.devMode)return AppRoutes.Screens.PRODUTOS.name();
        return AppRoutes.Screens.AUTH.name();
    }
}
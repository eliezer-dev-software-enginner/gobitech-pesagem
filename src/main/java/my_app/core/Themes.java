package my_app.core;


import megalodonte.base.theme.*;

public class Themes {
    public static final ThemeInterface DARK = new ThemeInterface() {
        @Override
        public ThemeColors colors() {
return new ThemeColors(
                "#1e293b",                    // background
                "#1a2235",                    // surface
                "#fff",                    // primary
                "#334155",                    // secondary
                "#ffffff",                    // text primary
                "#94a3b8",                    // text secondary
                "#334155",                    // border,
        "#A9A9A9",
        "#dbeafe",                    // selection (primary with low opacity)
        "#93c5fd",                    // focusRing (lighter primary)
        "#2d3a4f"                     // hover
            );
        }

        @Override
        public ThemeTypography typography() {
            return new ThemeTypography("Roboto", 35, 20, 16, 13);
        }

        @Override
        public ThemeSpacing spacing() {
            return new ThemeSpacing(4, 8, 12, 20, 32);
        }

        @Override
        public ThemePadding padding() {
            return new ThemePadding(4, 8, 12, 20, 32);
        }

        @Override
        public ThemeBorder border() {
            return new ThemeBorder(1,4,8,12);
        }
    };

    public static final ThemeInterface LIGHT = new ThemeInterface() {
        @Override
        public ThemeColors colors() {
            // Paleta do app original (pesagemFinal): dourado/mostarda como cor de ação
            // (Login/Cadastrar/Salvar em toda tela), painel escuro na sidebar, fundo
            // quase branco — ver UI.Login/UI.Sidebar do projeto legado.
            return new ThemeColors(
                "#f7f7f7",                    // background
                "#ffffff",                    // surface: Cards e tabelas
                "#f0cb54",                    // primary: dourado dos botões de ação do app original
                "#ffe79a",                    // secondary: amarelo claro (sidebar/painéis do app original)
                "#000000",                    // text primary: preto, igual ao app original
                "#5a5a5a",                    // text secondary
                "#cccccc",                    // border: cinza neutro do app original
                "#9ca3af",                    // placeholder
                "#fbe8b8",                     // selection (tom claro do dourado)
                "#e4c06a",                     // focusRing (tom médio do dourado)
                "#ffe79a"                      // hover

            );
        }

        @Override
        public ThemeTypography typography() {
            return new ThemeTypography("Roboto", 35, 20, 16, 13);
        }

        @Override
        public ThemeSpacing spacing() {
            return new ThemeSpacing(4, 8, 12, 20, 32);
        }

        @Override
        public ThemePadding padding() {
            return new ThemePadding(4, 8, 12, 20, 32);
        }

        @Override
        public ThemeBorder border() {
            return new ThemeBorder(1,4,8,12);
        }
    };
}
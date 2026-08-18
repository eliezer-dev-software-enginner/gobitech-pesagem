package my_app.screens.homeScreen;

import megalodonte.router.v4.ScreenContext;

public class HomeScreenViewModel {

    private final ScreenContext screenContext;

    public HomeScreenViewModel(ScreenContext screenContext) {
        this.screenContext = screenContext;
    }

    public void onDestroy() {
        // sem services abertos por essa tela hoje
    }
}

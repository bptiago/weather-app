// App inspired by TapTap (https://www.youtube.com/@TapTap_196)

import javax.swing.*;

public class AppLauncher {
    public static void main(String[] args) {
        SwingUtilities.invokeLater(new Runnable() {
            @Override
            public void run() {
                // Open window
                new WeatherAppGui().setVisible(true);
            }
        });
    }
}

/**
 * Classe de lancement pour le fat JAR.
 * JavaFX nécessite que la classe main ne soit pas une sous-classe de Application
 * lorsqu'on utilise un fat JAR (shaded JAR).
 */
public class Launcher {
    public static void main(String[] args) {
        App.main(args);
    }
}

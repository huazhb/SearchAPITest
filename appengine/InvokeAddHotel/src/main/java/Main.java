import java.io.IOException;

public class Main {

    public static void main(String[] args) throws IOException, InterruptedException {

        for (int i = 0; i < 100; i ++) {
            Poster poster = new Poster();
            poster.Post();

            Thread.sleep(100000);
        }

    }
}

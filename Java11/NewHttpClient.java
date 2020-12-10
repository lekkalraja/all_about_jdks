import java.io.IOException;
import java.net.URI;
import java.net.http.HttpClient;
import java.net.http.HttpRequest;
import java.net.http.HttpResponse;
import java.net.http.HttpResponse.BodyHandlers;
import java.util.concurrent.CompletableFuture;


public class NewHttpClient {

    public static void main(String... args) throws IOException, InterruptedException {
        // Create HttpClient
        var httpClient = HttpClient.newHttpClient();

        // Create HttpRequest
        var request = HttpRequest.newBuilder()
        .GET()
        .uri(URI.create("https://feeds.citibikenyc.com/stations/stations.json"))
        .build();
        
        // Send Request to Get the HttpResponse
        HttpResponse<String> response = httpClient.send(request, BodyHandlers.ofString());

        System.out.printf("[Sync] Got Response Status Code : %s \n", response.statusCode());
        // System.out.printf("[Sync] Response Body : \n %s", response.body());

        CompletableFuture<HttpResponse<String>> asyncResponse = httpClient.sendAsync(request, BodyHandlers.ofString());
            
        asyncResponse.thenAccept(item -> {
                System.out.printf("[Async] Got Response Status Code : %s \n", item.statusCode());
                // System.out.printf("[Async] Response Body : \n %s", response.body());
            })
            .join();
    }
}
package pt.ist.meic.cnv.SudokuSolver;

import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.util.MultiValueMap;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.reactive.function.BodyInserters;
import org.springframework.web.reactive.function.client.ClientResponse;
import org.springframework.web.reactive.function.client.WebClient;
import reactor.core.publisher.Mono;

import java.util.Optional;
import java.util.concurrent.atomic.AtomicReference;

@RestController
public class SudokuRestController {

    private static final int MAX_RETRIES = 10;

    @PostMapping("/sudoku")
    public String computeSudoku(@RequestBody String body,
                                        @RequestParam MultiValueMap<String,String> params) {
        // http://63.33.43.84:8080/sudoku?s=CP&un=81&n1=9&n2=9&i=SUDOKU_PUZZLE_9x9_101
        // {s=CP, un=81, n1=9, n2=9, i=SUDOKU_PUZZLE_9x9_101}
        // s = solver
        // un = unassigned entries
        // n1,n2 = puzzle size
        // i = map
        int attempts = 0;

        System.out.println("Params = " + params);
        System.out.println("Body = " + body);
        while (attempts < MAX_RETRIES) {
            String selectedServer = "34.247.37.36";
            String port = "8000";
            WebClient.UriSpec<WebClient.RequestBodySpec> req = WebClient
                    .create("http://" + selectedServer + ":" + port)
                    .post();

            WebClient.RequestHeadersSpec<?> rqh = req.uri(uriBuilder -> uriBuilder.path("/sudoku")
                    .queryParams(params).build())
                    .body(BodyInserters.fromValue(body));

            Mono<ClientResponse> clientResponse = rqh.exchange();
            System.out.println("blocking...");
            Optional<ClientResponse> optionalResponse = clientResponse.blockOptional();

            if (optionalResponse.isPresent()) {
                ClientResponse response = optionalResponse.get();

                ClientResponse.Headers headers = response.headers();
                HttpStatus statusCode = response.statusCode();
                String bodyResponse = response.bodyToMono(String.class).block();

                System.out.println("body response:" + bodyResponse);
                System.out.println("headers response:" + headers.asHttpHeaders());
                System.out.println("statusCode response:" + statusCode);

                return bodyResponse;
            } else {
                attempts++;
            }
        }
        throw new IllegalStateException("Couldn't find compute unit after " + attempts + "attempts.");

    }

}

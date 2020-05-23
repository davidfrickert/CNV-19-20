package pt.ist.meic.cnv.webapp;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.core.annotation.Order;
import org.springframework.http.HttpStatus;
import org.springframework.util.MultiValueMap;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.reactive.function.BodyInserters;
import org.springframework.web.reactive.function.client.ClientResponse;
import org.springframework.web.reactive.function.client.WebClient;
import pt.ist.meic.cnv.webapp.Balancer.LoadBalancer;
import pt.ist.meic.cnv.webapp.SudokuSolver.exception.NoInstanceAvailable;
import pt.ist.meic.cnv.webapp.SudokuSolver.instance.InstanceInfo;
import pt.ist.meic.cnv.webapp.SudokuSolver.instance.Request;
import reactor.core.publisher.Mono;

import java.util.*;

@RestController
@Order(value = 3)
public class SudokuRestController {

    private static final int MAX_RETRIES = 10;

    @Autowired
    private LoadBalancer loadBalancer;



    @PostMapping("/sudoku")
    public String computeSudoku(@RequestBody String body,
                                @RequestParam MultiValueMap<String,String> params) {

        System.out.println("lbal on controller: " + loadBalancer);
        // http://63.33.43.84:8080/sudoku?s=CP&un=81&n1=9&n2=9&i=SUDOKU_PUZZLE_9x9_101
        // {s=CP, un=81, n1=9, n2=9, i=SUDOKU_PUZZLE_9x9_101}
        // s = solver
        // un = unassigned entries
        // n1,n2 = puzzle size
        // i = map
        int attempts = 0;


        InstanceInfo bestInstance = loadBalancer.getBestInstance();

        System.out.println("Params = " + params);
        System.out.println("Body = " + body);
        while (attempts < MAX_RETRIES) {
            try {
                String selectedServer = bestInstance.getInstanceData().getPrivateIpAddress();

                String port = "8000";
                Request reqData = new Request(params);
                bestInstance.addRequest(reqData);
                WebClient.RequestHeadersSpec<?> req = WebClient
                        .create("http://" + selectedServer + ":" + port)
                        .post()
                        .uri(uriBuilder -> uriBuilder.path("/sudoku")
                                .queryParams(params).build())
                        .body(BodyInserters.fromValue(body));

                Mono<ClientResponse> clientResponse = req.exchange();
                System.out.println("Waiting for computation...");
                Optional<ClientResponse> optionalResponse = clientResponse.blockOptional();
                System.out.println("Received Sudoku!");

                if (optionalResponse.isPresent()) {
                    ClientResponse response = optionalResponse.get();

                    ClientResponse.Headers headers = response.headers();
                    HttpStatus statusCode = response.statusCode();
                    String bodyResponse = response.bodyToMono(String.class).block();

                    System.out.println("body response:" + bodyResponse);
                    System.out.println("headers response:" + headers.asHttpHeaders());
                    System.out.println("statusCode response:" + statusCode);

                    bestInstance.removeRequest(reqData);

                    return bodyResponse;
                }
            } catch (NoInstanceAvailable ignored) {
                try {
                    Thread.sleep(5000);
                } catch (InterruptedException e) {
                    e.printStackTrace();
                }
            }
            attempts++;
        }

        throw new IllegalStateException("Couldn't find compute unit after " + attempts + "attempts.");

    }


    @Override
    public String toString() {
        return "SudokuRestController{" +
                "loadBalancer=" + loadBalancer +
                '}';
    }
}

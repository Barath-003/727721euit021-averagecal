package com.example.average_calculator;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;
import org.springframework.web.client.RestTemplate;

import java.util.*;
import java.util.concurrent.TimeUnit;

@RestController
@RequestMapping("/numbers")
public class AverageCalculatorController {

    @Value("${window.size}")
    private int windowSize;

    @Value("${test.server.primes.url}")
    private String primesUrl;

    @Value("${test.server.fibo.url}")
    private String fiboUrl;

    @Value("${test.server.even.url}")
    private String evenUrl;

    @Value("${test.server.rand.url}")
    private String randUrl;

    private final Map<String, List<Integer>> numberStorage = new HashMap<>() {{
        put("p", new ArrayList<>());
        put("f", new ArrayList<>());
        put("e", new ArrayList<>());
        put("r", new ArrayList<>());
    }};

    @GetMapping("/{numberid}")
    public ResponseEntity<?> getNumbers(@PathVariable String numberid) {
        if (!numberStorage.containsKey(numberid)) {
            return ResponseEntity.status(HttpStatus.BAD_REQUEST).body("Invalid number ID");
        }

        List<Integer> windowPrevState = new ArrayList<>(numberStorage.get(numberid));

        List<Integer> newNumbers = fetchNumbersFromServer(numberid);
        if (newNumbers == null) {
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR).body("Error fetching numbers from third-party server");
        }

        Set<Integer> existingNumbers = new HashSet<>(numberStorage.get(numberid));
        for (Integer num : newNumbers) {
            if (!existingNumbers.contains(num)) {
                numberStorage.get(numberid).add(num);
            }
        }

        if (numberStorage.get(numberid).size() > windowSize) {
            numberStorage.put(numberid, numberStorage.get(numberid).subList(numberStorage.get(numberid).size() - windowSize, numberStorage.get(numberid).size()));
        }

        List<Integer> windowCurrState = numberStorage.get(numberid);
        double avg = calculateAverage(windowCurrState);

        Map<String, Object> response = new HashMap<>();
        response.put("windowPrevState", windowPrevState);
        response.put("windowCurrState", windowCurrState);
        response.put("numbers", newNumbers);
        response.put("avg", Math.round(avg * 100.0) / 100.0);

        return ResponseEntity.ok(response);
    }

    private List<Integer> fetchNumbersFromServer(String numberid) {
        String url;
        switch (numberid) {
            case "p":
                url = primesUrl;
                break;
            case "f":
                url = fiboUrl;
                break;
            case "e":
                url = evenUrl;
                break;
            case "r":
                url = randUrl;
                break;
            default:
                return null;
        }

        try {
            RestTemplate restTemplate = new RestTemplate();
            ResponseEntity<Map> response = restTemplate.getForEntity(url, Map.class);

            if (response.getStatusCode().is2xxSuccessful() && response.getBody() != null && response.getBody().containsKey("numbers")) {
                return (List<Integer>) response.getBody().get("numbers");
            }
        } catch (Exception e) {
            // Log the exception
            System.err.println("Error fetching numbers from server: " + e.getMessage());
        }

        return null;
    }

    private double calculateAverage(List<Integer> numbers) {
        if (numbers.isEmpty()) {
            return 0.0;
        }

        return numbers.stream().mapToInt(Integer::intValue).average().orElse(0.0);
    }
}

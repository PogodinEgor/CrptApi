package org.example;


import com.fasterxml.jackson.annotation.JsonTypeInfo;
import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.ObjectMapper;
import lombok.Data;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

import java.io.IOException;
import java.net.URI;
import java.net.http.HttpClient;
import java.net.http.HttpRequest;
import java.net.http.HttpResponse;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;
import java.util.concurrent.Executors;
import java.util.concurrent.ScheduledExecutorService;
import java.util.concurrent.Semaphore;
import java.util.concurrent.TimeUnit;


@Getter
@Setter
public class CrptApi {

    private final static String URL = "https://ismp.crpt.ru/api/v3/lk/documents/create";

    private final ScheduledExecutorService sheduler = Executors.newScheduledThreadPool(1);
    private final Semaphore semaphore;

    public CrptApi(TimeUnit timeUnit, int requestLimit) {
        semaphore = new Semaphore(requestLimit);

        // Задача для периодического освобождения семафора
        Runnable releaseTask = () -> semaphore.release(requestLimit - semaphore.availablePermits());

        // Запуск задачи с интервалом, указанным в timeUnit
        long period = timeUnit.toMillis(1);
        sheduler.scheduleAtFixedRate(releaseTask, period, period, TimeUnit.MILLISECONDS);
    }

    public String createDocument(Document document, String signature) throws JsonProcessingException, InterruptedException {
        if (document == null) {
            throw new IllegalArgumentException("Document cannot be null");
        }

        if (signature == null || signature.trim().isEmpty()) {
            throw new IllegalArgumentException("Signature cannot be null or empty");
        }
        ObjectMapper objectMapper = new ObjectMapper();
        String requestBody = objectMapper.writeValueAsString(document);

        HttpRequest request = HttpRequest.newBuilder()
                .uri(URI.create(URL))
                .header("Content-Type", "application/json")
                .header("Signature", signature) // Предполагается, что подпись передается так
                .POST(HttpRequest.BodyPublishers.ofString(requestBody))
                .build();
        System.out.println(requestBody);
        semaphore.acquire(); // Ограничение на количество запросов

        try {
            HttpClient client = HttpClient.newHttpClient();
            HttpResponse<String> response = client.send(request, HttpResponse.BodyHandlers.ofString());
            int statusCode = response.statusCode();
            if (statusCode == 200) {
                return response.body(); // Возвращаем строку в виде документа
            } else {
                System.out.println("Ошибка запроса: " + statusCode);
            }

        } catch (IOException e) {
            throw new RuntimeException(e);
        } finally {
            semaphore.release();
        }
        return "";
    }




        @Data
        @NoArgsConstructor
        public static class Document {
            private Description description;
            private String doc_id;
            private String doc_status;
            private DocType doc_type;
            private boolean importRequest;
            private String owner_inn;
            private String participant_inn;
            private String producer_inn;
            private String production_date;
            private String production_type;
            private List<Product> products;
            private String reg_date;
            private String reg_number;
        }

            public enum DocType {
                LP_INTRODUCE_GOODS
            }

            @Data
            @NoArgsConstructor
            public static class Description {
                private String participantInn;

            }
            @Data
            @NoArgsConstructor
            public static class Product {
                private String certificate_document;
                private String certificate_document_date;
                private String certificate_document_number;
                private String owner_inn;
                private String producer_inn;
                private String production_date;
                private String tnved_code;
                private String uit_code;
                private String uitu_code;

            }

        public static void main (String[]args) throws Exception {
            CrptApi api = new CrptApi(TimeUnit.SECONDS, 5);
            Document document = new Document();

            Product product = new Product();

            List<Product> products = new ArrayList<>(Arrays.asList(product));
            Description description = new Description();
            description.setParticipantInn("participantInn");
            document.setDescription(description);
            document.setDoc_id("doc_id");
            document.setDoc_status("doc_status");
            document.setDoc_type(DocType.LP_INTRODUCE_GOODS);
            document.setImportRequest(true);
            document.setOwner_inn("owner_inn");
            document.setParticipant_inn("participant_inn");
            document.setProducer_inn("producer_inn");
            document.setProduction_date("production_date");
            document.setProduction_type("production_type");


            document.setProducts(products);
            document.setReg_date("reg_date");
            document.setReg_number("reg_number");

            String result = api.createDocument(document, "signature");
            System.out.println(result);
        }

    }


package com.gruppe2.backend;

import com.google.gson.Gson;
import org.apache.kafka.clients.consumer.ConsumerConfig;
import org.apache.kafka.clients.consumer.ConsumerRecord;
import org.apache.kafka.clients.consumer.ConsumerRecords;
import org.apache.kafka.clients.consumer.KafkaConsumer;
import org.apache.kafka.clients.producer.KafkaProducer;
import org.apache.kafka.clients.producer.ProducerConfig;
import org.apache.kafka.clients.producer.ProducerRecord;
import org.apache.kafka.common.serialization.StringDeserializer;
import org.apache.kafka.common.serialization.StringSerializer;

import java.time.Duration;
import java.util.*;

public class KafkaTester {

    private static String BOOTSTRAP_SERVERS = "localhost:9092";
    private static Gson gson = new Gson();

    public static void main(String[] args) {
        System.out.println("Kafka test client");

        try (Scanner scanner = new Scanner(System.in)) {
            while (true) {
                System.out.println("\nChoose an action:");
                System.out.println("1. Send a vote (produce)");
                System.out.println("2. Listen for votes (consume)");
                System.out.println("3. Exit");
                System.out.print("> ");

                String choice = scanner.nextLine();

                switch (choice) {
                    case "1":
                        sendVote(scanner);
                        break;
                    case "2":
                        listenForVotes(scanner);
                        break;
                    case "3":
                        System.out.println("Exiting.");
                        return;
                    default:
                        System.out.println("Invalid choice.");
                }
            }
        }
    }

    private static void sendVote(Scanner scanner) {
        Properties props = new Properties();
        props.put(ProducerConfig.BOOTSTRAP_SERVERS_CONFIG, BOOTSTRAP_SERVERS);
        props.put(ProducerConfig.KEY_SERIALIZER_CLASS_CONFIG, StringSerializer.class.getName());
        props.put(ProducerConfig.VALUE_SERIALIZER_CLASS_CONFIG, StringSerializer.class.getName());

        try (KafkaProducer<String, String> producer = new KafkaProducer<>(props)) {

            System.out.print("Enter poll id: ");
            int pollId = Integer.parseInt(scanner.nextLine());

            System.out.print("Enter user id or leave blank for anon vote: ");
            String userIdInput = scanner.nextLine();

            System.out.print("Enter presentation order of option to vote for: ");
            int presentationOrder = Integer.parseInt(scanner.nextLine());

            Map<String, Object> voteData = new HashMap<>();
            voteData.put("pollId", pollId);
            if (userIdInput != null && !userIdInput.isBlank()) {
                voteData.put("userId", Integer.parseInt(userIdInput));
            } else {
                voteData.put("userId", null);
            }
            voteData.put("presentationOrder", presentationOrder);

            String jsonPayload = gson.toJson(voteData);

            String topic = "poll.voteChange." + pollId;
            ProducerRecord<String, String> record = new ProducerRecord<>(topic, jsonPayload);
            producer.send(record);
            producer.flush();

            System.out.println("\nVote successfully sent to topic:'" + topic);
            System.out.println("Contents of vote: " + jsonPayload);

        } catch (NumberFormatException e) {
            System.err.println("Invalid number format.");
        }
    }

    private static void listenForVotes(Scanner scanner) {
        Properties props = new Properties();
        props.put(ConsumerConfig.BOOTSTRAP_SERVERS_CONFIG, BOOTSTRAP_SERVERS);
        props.put(ConsumerConfig.GROUP_ID_CONFIG, "test-consumer-group");
        props.put(ConsumerConfig.KEY_DESERIALIZER_CLASS_CONFIG, StringDeserializer.class.getName());
        props.put(ConsumerConfig.VALUE_DESERIALIZER_CLASS_CONFIG, StringDeserializer.class.getName());
        props.put(ConsumerConfig.AUTO_OFFSET_RESET_CONFIG, "earliest");

        System.out.print("Enter the topic name to listen to (poll.voteChange.<pollId>): ");
        String topic = scanner.nextLine();

        try (KafkaConsumer<String, String> consumer = new KafkaConsumer<>(props)) {
            consumer.subscribe(Collections.singletonList(topic));
            System.out.println("\nListening for messages on topic '" + topic);

            while (true) {
                ConsumerRecords<String, String> records = consumer.poll(Duration.ofMillis(1000));
                for (ConsumerRecord<String, String> record : records) {
                    System.out.printf("Received Message: value = " +record.value());
                }
            }
        }
    }
}
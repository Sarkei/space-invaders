package com.spaceinvaders.web;

import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;

/**
 * Einstiegspunkt für den Web-Modus (Docker/Cloudflare).
 *
 * Starten:  mvn spring-boot:run
 *        ODER  java -jar target/space-invaders-*.jar
 *        ODER  docker-compose up --build
 *
 * Browser:  http://localhost:8080
 */
@SpringBootApplication
public class WebMain {

    public static void main(String[] args) {
        // Headless-Modus: AWT-Klassen (Image, Rectangle) können trotzdem
        // geladen werden – nur keine Fenster/Displays.
        System.setProperty("java.awt.headless", "true");
        SpringApplication.run(WebMain.class, args);
    }
}

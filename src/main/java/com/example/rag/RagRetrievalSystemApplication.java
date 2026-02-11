package com.example.rag;

import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.scheduling.annotation.EnableAsync;

/**
 * RAG Retrieval System Application
 * 
 * Main entry point for the RAG (Retrieval Augmented Generation) System.
 * This application provides document management and intelligent Q&A capabilities
 * using vector search and large language models.
 */
@SpringBootApplication
@EnableAsync
public class RagRetrievalSystemApplication {

    public static void main(String[] args) {
        SpringApplication.run(RagRetrievalSystemApplication.class, args);
    }
}

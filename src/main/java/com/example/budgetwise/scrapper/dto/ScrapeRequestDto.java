package com.example.budgetwise.scrapper.dto;

import com.fasterxml.jackson.annotation.JsonProperty;

public record ScrapeRequestDto (

        @JsonProperty("target_url")
        String url,
        @JsonProperty("file_content") String fileContent,
        @JsonProperty("filename") String filename
){}
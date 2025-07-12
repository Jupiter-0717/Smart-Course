package com.example.smartlearn.dto;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class KnowledgeGraphEdgeDTO {
    private String source;
    private String target;
    private String relationship;
}
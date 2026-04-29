package com.example.literaturesearchsystem.es.document;

import lombok.Data;
import lombok.NoArgsConstructor;
import lombok.AllArgsConstructor;
import java.util.List;

@Data
@NoArgsConstructor
@AllArgsConstructor
public class Completion {
    private List<String> input;
    private Integer weight;
}
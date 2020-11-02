package tech.picnic.assignment.impl.model.output;

import lombok.AllArgsConstructor;
import lombok.Data;

import java.time.Instant;

@Data
@AllArgsConstructor
public class Pick {
    private String articleName;
    private Instant timestamp;
}
package tech.picnic.assignment.impl.model.input;

import lombok.Data;

import java.time.Instant;

@Data
public class Picker {
    private String id;
    private String name;
    private Instant activeSince;
}

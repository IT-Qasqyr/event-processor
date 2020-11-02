package tech.picnic.assignment.impl.model.input;

import lombok.Data;

import java.time.Instant;

@Data
public class PickEvent {
    private Instant timestamp;
    private String id;
    private Picker picker;
    private Article article;
    private Integer quantity;

    public boolean isAmbient() {
        return this.getArticle().getTemperatureZone() == Article.TemperatureZone.AMBIENT;
    }
}
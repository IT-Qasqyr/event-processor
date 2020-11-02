package tech.picnic.assignment.impl.model.output;

import com.fasterxml.jackson.annotation.JsonIgnore;
import lombok.AllArgsConstructor;
import lombok.Data;

import java.time.Instant;
import java.util.Comparator;
import java.util.Set;

@Data
@AllArgsConstructor
public class PickGroup {
    @JsonIgnore
    private String id;
    private String pickerName;
    private Instant activeSince;
    private Set<Pick> picks;

    public void addItem(String articleName, Instant timestamp) {
        picks.add(new Pick(articleName, timestamp));
    }

    public static Comparator<PickGroup> sortByActiveSinceAndId() {
        return Comparator.comparing(PickGroup::getActiveSince).thenComparing(PickGroup::getId);
    }
}
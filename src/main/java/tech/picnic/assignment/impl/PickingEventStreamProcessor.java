package tech.picnic.assignment.impl;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.databind.PropertyNamingStrategy;
import com.fasterxml.jackson.databind.SerializationFeature;
import com.fasterxml.jackson.datatype.jsr310.JavaTimeModule;
import tech.picnic.assignment.api.StreamProcessor;
import tech.picnic.assignment.impl.model.input.PickEvent;
import tech.picnic.assignment.impl.model.input.Picker;
import tech.picnic.assignment.impl.model.output.Pick;
import tech.picnic.assignment.impl.model.output.PickGroup;

import java.io.*;
import java.time.Clock;
import java.time.Duration;
import java.time.Instant;
import java.time.ZoneId;
import java.util.*;
import java.util.stream.Collectors;

public class PickingEventStreamProcessor implements StreamProcessor {

    private final int maxEvents;
    private final Clock maxTime;

    private final ObjectMapper mapper = new ObjectMapper()
            .registerModule(new JavaTimeModule())
            .configure(SerializationFeature.WRITE_DATES_AS_TIMESTAMPS, false)
            .setPropertyNamingStrategy(PropertyNamingStrategy.SNAKE_CASE);

    public PickingEventStreamProcessor(int maxEvents, Duration maxTime) {
        this.maxEvents = maxEvents;
        this.maxTime = fixTheTime(maxTime);
    }

    private Clock fixTheTime(Duration maxTime) {
        Clock fixedTime = Clock.fixed(Instant.now(), ZoneId.systemDefault());
        return Clock.offset(fixedTime, maxTime);
    }

    @Override
    public void process(InputStream source, OutputStream sink) throws IOException {
        List<PickEvent> events = readAllEvents(source);
        List<PickEvent> filteredEvents = filterEvents(events);
        List<PickGroup> groups = groupAndSortEvents(filteredEvents);
        writeAllEvents(sink, groups);
    }

    private List<PickEvent> readAllEvents(InputStream source) throws IOException {
        List<PickEvent> events = new LinkedList<>();
        String line;
        int eventCount = 0;

        try (BufferedReader reader = new BufferedReader(new InputStreamReader(source))) {
            while (Objects.nonNull(line = reader.readLine()) && hasEnoughCapacity(eventCount)) {
                events.add(parseEvent(line));
                eventCount++;
            }
        }

        return events;
    }

    private boolean hasEnoughCapacity(int eventCount) {
        return eventCount < maxEvents && Instant.now().isBefore(maxTime.instant());
    }

    private PickEvent parseEvent(String line) {
        try {
            return mapper.readValue(line, PickEvent.class);
        } catch (JsonProcessingException e) {
            return null; // catch "keep-alive" messages
        }
    }

    private List<PickEvent> filterEvents(List<PickEvent> events) {
        return events.stream().filter(Objects::nonNull).filter(PickEvent::isAmbient).collect(Collectors.toList());
    }

    private List<PickGroup> groupAndSortEvents(List<PickEvent> picks) {
        Map<Picker, PickGroup> groups = groupEvents(picks);
        return sortEvents(groups);
    }

    private Map<Picker, PickGroup> groupEvents(List<PickEvent> picks) {
        Map<Picker, PickGroup> map = new HashMap<>();

        picks.forEach(pick -> {
            Picker picker = pick.getPicker();
            map.computeIfAbsent(picker, p -> new PickGroup(p.getId(), p.getName(), p.getActiveSince(), new TreeSet<>(Comparator.comparing(Pick::getTimestamp))));
            map.get(picker).addItem(pick.getArticle().getName().toUpperCase(), pick.getTimestamp());
        });

        return map;
    }

    private List<PickGroup> sortEvents(Map<Picker, PickGroup> groups) {
        return groups.values().stream().sorted(PickGroup.sortByActiveSinceAndId()).collect(Collectors.toList());
    }

    private void writeAllEvents(OutputStream sink, List<PickGroup> groups) throws IOException {
        try (BufferedWriter writer = new BufferedWriter(new OutputStreamWriter(sink))) {
            writer.write(mapper.writeValueAsString(groups));
        }
    }

    @Override
    public void close() {
    }
}

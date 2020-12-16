package io.github.zero88.msa.sql.marker;

import java.util.HashMap;
import java.util.LinkedHashMap;
import java.util.Map;
import java.util.Map.Entry;
import java.util.Objects;
import java.util.Set;
import java.util.stream.Collectors;
import java.util.stream.Stream;

import io.github.zero88.msa.bp.dto.JsonData;
import io.github.zero88.msa.bp.exceptions.ErrorCode;
import io.github.zero88.msa.bp.exceptions.ImplementationError;
import io.github.zero88.msa.sql.EntityMetadata;
import io.github.zero88.utils.Functions;
import io.github.zero88.utils.Strings;
import io.reactivex.Observable;
import io.vertx.core.json.JsonObject;


import lombok.NoArgsConstructor;
import lombok.NonNull;

/**
 * Represents mapping between {@code json field} and {@code resource field}.
 * <p>
 * In most case, {@code json field} will be similar to {@code resource field}. For examples:
 * <table>
 *  <tr><th>Json Field</th><th>Resource field</th></tr>
 *  <tr><td>device_id</td><td>DEVICE_ID</td></tr>
 *  <tr><td>device_id</td><td>DEVICE</td></tr>
 *  <tr><td>device</td><td>DEVICE</td></tr>
 * </table>
 *
 * @since 1.0.0
 */
@NoArgsConstructor
public final class EntityReferences {

    private final Map<EntityMetadata, String> fields = new LinkedHashMap<>();

    @NonNull
    public Set<EntityMetadata> keys() {
        return fields.keySet();
    }

    @NonNull
    public Stream<Entry<EntityMetadata, String>> stream() {
        return fields.entrySet().stream();
    }

    @NonNull
    public Observable<Entry<EntityMetadata, String>> toObservable() {
        return Observable.fromIterable(fields.entrySet());
    }

    @NonNull
    public String get(@NonNull EntityMetadata metadata) {
        if (fields.containsKey(metadata)) {
            return fields.get(metadata);
        }
        throw new ImplementationError(ErrorCode.NOT_FOUND, "Unexpected metadata");
    }

    @NonNull
    public boolean contains(@NonNull EntityMetadata metadata) {
        return fields.containsKey(metadata);
    }

    /**
     * Add entity reference.
     *
     * @param metadata the metadata
     * @return the entity references
     * @since 1.0.0
     */
    public EntityReferences add(@NonNull EntityMetadata metadata) {
        fields.put(metadata, metadata.requestKeyName());
        return this;
    }

    /**
     * Add entity reference.
     *
     * @param metadata the metadata
     * @param fkField  the json foreign key field. If it is {@code blank}, it will fallback to {@link
     *                 EntityMetadata#requestKeyName()}
     * @return the entity references
     * @since 1.0.0
     */
    public EntityReferences add(@NonNull EntityMetadata metadata, String fkField) {
        fields.put(metadata, Strings.isBlank(fkField) ? metadata.requestKeyName() : fkField);
        return this;
    }

    public Set<String> ignoreFields() {
        return Stream.concat(fields.keySet().stream().map(EntityMetadata::requestKeyName), fields.values().stream())
                     .collect(Collectors.toSet());
    }

    public Map<String, Object> computeRequest(@NonNull JsonObject body) {
        if (body.isEmpty()) {
            return body.getMap();
        }
        return fields.entrySet()
                     .stream()
                     .filter(entry -> body.containsKey(entry.getKey().requestKeyName()))
                     .collect(HashMap::new, (map, entry) -> map.put(entry.getValue(), getValue(body, entry)),
                              Map::putAll);
    }

    private Object getValue(@NonNull JsonObject body, @NonNull Entry<EntityMetadata, String> entry) {
        final EntityMetadata metadata = entry.getKey();
        final Object value = body.getValue(metadata.requestKeyName());
        return Objects.isNull(value)
               ? null
               : Functions.getOrThrow(() -> JsonData.checkAndConvert(metadata.parseKey(value.toString())),
                                      t -> new IllegalArgumentException("Invalid " + metadata.requestKeyName(), t));
    }

}

/*
 *  Copyright (c) 2017 Otávio Santana and others
 *   All rights reserved. This program and the accompanying materials
 *   are made available under the terms of the Eclipse Public License v1.0
 *   and Apache License v2.0 which accompanies this distribution.
 *   The Eclipse Public License is available at http://www.eclipse.org/legal/epl-v10.html
 *   and the Apache License v2.0 is available at http://www.opensource.org/licenses/apache2.0.php.
 *
 *   You may elect to redistribute this code under either of these licenses.
 *
 *   Contributors:
 *
 *   Otavio Santana
 */
package org.eclipse.jnosql.communication.couchbase.keyvalue;


import com.couchbase.client.java.Bucket;
import com.couchbase.client.java.Collection;
import com.couchbase.client.java.kv.GetResult;
import com.couchbase.client.java.kv.UpsertOptions;
import jakarta.nosql.Value;
import jakarta.nosql.keyvalue.BucketManager;
import jakarta.nosql.keyvalue.KeyValueEntity;
import org.eclipse.jnosql.communication.driver.JsonbSupplier;
import org.eclipse.jnosql.communication.driver.ValueJSON;

import javax.json.bind.Jsonb;
import java.time.Duration;
import java.util.Objects;
import java.util.Optional;
import java.util.logging.Logger;

import static java.util.Objects.requireNonNull;
import static java.util.stream.Collectors.toList;
import static java.util.stream.StreamSupport.stream;
import static org.eclipse.jnosql.communication.driver.ValueUtil.convert;

/**
 * The couchbase implementation to {@link BucketManager}
 */
public class CouchbaseBucketManager implements BucketManager {

    private static final Logger LOGGER = Logger.getLogger(CouchbaseBucketManager.class.getName());

    private static final Jsonb JSONB = JsonbSupplier.getInstance().get();

    private final Bucket bucket;

    private final String bucketName;

    private final Collection collection;

    CouchbaseBucketManager(Bucket bucket, String bucketName) {
        this.bucket = bucket;
        this.bucketName = bucketName;
        this.collection = bucket.defaultCollection();
    }

    @Override
    public <K, V> void put(K key, V value) {
        requireNonNull(key, "key is required");
        requireNonNull(value, "value is required");
        collection.upsert(key.toString(), value);

    }

    @Override
    public void put(KeyValueEntity entity) throws NullPointerException {
        requireNonNull(entity, "entity is required");
        put(entity.getKey(), convert(Value.of(entity.getValue())));
    }

    @Override
    public void put(KeyValueEntity entity, Duration ttl) {
        requireNonNull(entity, "entity is required");
        requireNonNull(ttl, "ttl is required");
        String key = entity.getKey(String.class);
        Object value = convert(Value.of(entity.getValue()));
        collection.upsert(key, value, UpsertOptions.upsertOptions().expiry(ttl));

    }

    @Override
    public void put(Iterable<KeyValueEntity> keyValueEntities) {
        requireNonNull(keyValueEntities, "keyValueEntities is required");
        keyValueEntities.forEach(this::put);
    }

    @Override
    public void put(Iterable<KeyValueEntity> keyValueEntities, Duration ttl) {
        requireNonNull(keyValueEntities, "keyValueEntities is required");
        requireNonNull(ttl, "ttl is required");
        keyValueEntities.forEach(k -> this.put(k, ttl));
    }

    @Override
    public <K> Optional<Value> get(K key) throws NullPointerException {
        requireNonNull(key, "key is required");
        GetResult result = this.collection.get(key.toString());
        return Optional.of(ValueJSON.of(value.toString()));
    }

    @Override
    public <K> Iterable<Value> get(Iterable<K> keys) {
        requireNonNull(keys, "keys is required");
        return stream(keys.spliterator(), false)
                .map(this::get)
                .filter(Optional::isPresent)
                .map(Optional::get)
                .collect(toList());
    }

    @Override
    public <K> void delete(K key) {
        requireNonNull(key, "key is required");
        collection.remove(key.toString());
    }

    @Override
    public <K> void delete(Iterable<K> keys) {
        requireNonNull(keys, "keys is required");
        keys.forEach(this::delete);
    }

    @Override
    public void close() {
    }

}

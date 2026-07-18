package com.tuempresa.nms;

import com.tuempresa.nms.core.ports.ReadingRepository;
import com.tuempresa.nms.infrastructure.persistence.ReadingEntity;
import org.springframework.data.domain.Example;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.domain.Sort;
import org.springframework.data.repository.query.FluentQuery;

import java.time.Instant;
import java.util.*;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.atomic.AtomicLong;
import java.util.function.Function;
import java.util.stream.Collectors;

public class InMemoryReadingRepository implements ReadingRepository {

    private final Map<Long, ReadingEntity> storage = new ConcurrentHashMap<>();
    private final AtomicLong idGenerator = new AtomicLong(1);

    @Override
    public List<ReadingEntity> findByDeviceIdOrderByPolledAtDesc(Long deviceId) {
        return storage.values().stream()
            .filter(r -> r.getDevice() != null && deviceId.equals(r.getDevice().getId()))
            .sorted(Comparator.comparing(ReadingEntity::getPolledAt).reversed())
            .collect(Collectors.toList());
    }

    @Override
    public List<ReadingEntity> findByDeviceIdAndPolledAtAfter(Long deviceId, Instant since) {
        return storage.values().stream()
            .filter(r -> r.getDevice() != null && deviceId.equals(r.getDevice().getId()))
            .filter(r -> r.getPolledAt() != null && r.getPolledAt().isAfter(since))
            .sorted(Comparator.comparing(ReadingEntity::getPolledAt))
            .collect(Collectors.toList());
    }

    @Override
    public <S extends ReadingEntity> S save(S entity) {
        if (entity.getId() == null) {
            entity.setId(idGenerator.getAndIncrement());
        }
        storage.put(entity.getId(), entity);
        return entity;
    }

    @Override public List<ReadingEntity> findAll() { return List.copyOf(storage.values()); }
    @Override public Optional<ReadingEntity> findById(Long id) { return Optional.ofNullable(storage.get(id)); }
    @Override public boolean existsById(Long id) { return storage.containsKey(id); }
    @Override public long count() { return storage.size(); }
    @Override public void deleteById(Long id) { storage.remove(id); }
    @Override public void delete(ReadingEntity entity) { deleteById(entity.getId()); }
    @Override public void deleteAllById(Iterable<? extends Long> ids) { ids.forEach(this::deleteById); }
    @Override public void deleteAll(Iterable<? extends ReadingEntity> entities) { entities.forEach(this::delete); }
    @Override public void deleteAll() { storage.clear(); }
    @Override public <S extends ReadingEntity> List<S> saveAll(Iterable<S> entities) { List<S> result = new ArrayList<>(); for (S e : entities) result.add(save(e)); return result; }
    @Override public List<ReadingEntity> findAllById(Iterable<Long> ids) { List<ReadingEntity> result = new ArrayList<>(); for (Long id : ids) findById(id).ifPresent(result::add); return result; }
    @Override public List<ReadingEntity> findAll(Sort sort) { return findAll(); }
    @Override public Page<ReadingEntity> findAll(Pageable pageable) { return null; }
    @Override public <S extends ReadingEntity> Optional<S> findOne(Example<S> example) { return Optional.empty(); }
    @Override public <S extends ReadingEntity> List<S> findAll(Example<S> example) { return List.of(); }
    @Override public <S extends ReadingEntity> List<S> findAll(Example<S> example, Sort sort) { return List.of(); }
    @Override public <S extends ReadingEntity> Page<S> findAll(Example<S> example, Pageable pageable) { return null; }
    @Override public <S extends ReadingEntity> long count(Example<S> example) { return 0; }
    @Override public <S extends ReadingEntity> boolean exists(Example<S> example) { return false; }
    @Override public <S extends ReadingEntity, R> R findBy(Example<S> example, Function<FluentQuery.FetchableFluentQuery<S>, R> queryFunction) { return null; }
    @Override public void flush() {}
    @Override public <S extends ReadingEntity> S saveAndFlush(S entity) { return save(entity); }
    @Override public <S extends ReadingEntity> List<S> saveAllAndFlush(Iterable<S> entities) { return saveAll(entities); }
    @Override public void deleteAllInBatch(Iterable<ReadingEntity> entities) { deleteAll(entities); }
    @Override public void deleteAllByIdInBatch(Iterable<Long> ids) { deleteAllById(ids); }
    @Override public void deleteAllInBatch() { deleteAll(); }
    @Override public ReadingEntity getOne(Long id) { return findById(id).orElse(null); }
    @Override public ReadingEntity getById(Long id) { return findById(id).orElse(null); }
    @Override public ReadingEntity getReferenceById(Long id) { return findById(id).orElse(null); }
}

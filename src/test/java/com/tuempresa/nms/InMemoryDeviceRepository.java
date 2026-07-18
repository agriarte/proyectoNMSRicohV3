package com.tuempresa.nms;

import com.tuempresa.nms.core.ports.DeviceRepository;
import com.tuempresa.nms.infrastructure.persistence.DeviceEntity;
import org.springframework.data.domain.Example;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.domain.Sort;
import org.springframework.data.repository.query.FluentQuery;

import java.util.*;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.atomic.AtomicLong;
import java.util.function.Function;

/**
 * Implementacion en memoria de DeviceRepository para tests.
 */
public class InMemoryDeviceRepository implements DeviceRepository {

    private final Map<Long, DeviceEntity> storage = new ConcurrentHashMap<>();
    private final Map<String, DeviceEntity> byIp = new ConcurrentHashMap<>();
    private final AtomicLong idGenerator = new AtomicLong(1);

    @Override
    public Optional<DeviceEntity> findByIp(String ip) {
        return Optional.ofNullable(byIp.get(ip));
    }

    @Override
    public List<DeviceEntity> findByBrand(String brand) {
        return storage.values().stream()
            .filter(d -> brand.equals(d.getBrand()))
            .toList();
    }

    @Override
    public List<DeviceEntity> findByFamilyCode(String familyCode) {
        return storage.values().stream()
            .filter(d -> familyCode.equals(d.getFamilyCode()))
            .toList();
    }

    @Override
    public <S extends DeviceEntity> S save(S entity) {
        if (entity.getId() == null) {
            entity.setId(idGenerator.getAndIncrement());
        }
        storage.put(entity.getId(), entity);
        byIp.put(entity.getIp(), entity);
        return entity;
    }

    @Override public List<DeviceEntity> findAll() { return List.copyOf(storage.values()); }
    @Override public Optional<DeviceEntity> findById(Long id) { return Optional.ofNullable(storage.get(id)); }
    @Override public boolean existsById(Long id) { return storage.containsKey(id); }
    @Override public long count() { return storage.size(); }
    @Override public void deleteById(Long id) { DeviceEntity e = storage.remove(id); if (e != null) byIp.remove(e.getIp()); }
    @Override public void delete(DeviceEntity entity) { deleteById(entity.getId()); }
    @Override public void deleteAllById(Iterable<? extends Long> ids) { ids.forEach(this::deleteById); }
    @Override public void deleteAll(Iterable<? extends DeviceEntity> entities) { entities.forEach(this::delete); }
    @Override public void deleteAll() { storage.clear(); byIp.clear(); }
    @Override public <S extends DeviceEntity> List<S> saveAll(Iterable<S> entities) { List<S> result = new ArrayList<>(); for (S e : entities) result.add(save(e)); return result; }
    @Override public List<DeviceEntity> findAllById(Iterable<Long> ids) { List<DeviceEntity> result = new ArrayList<>(); for (Long id : ids) findById(id).ifPresent(result::add); return result; }
    @Override public List<DeviceEntity> findAll(Sort sort) { return findAll(); }
    @Override public Page<DeviceEntity> findAll(Pageable pageable) { return null; }
    @Override public <S extends DeviceEntity> Optional<S> findOne(Example<S> example) { return Optional.empty(); }
    @Override public <S extends DeviceEntity> List<S> findAll(Example<S> example) { return List.of(); }
    @Override public <S extends DeviceEntity> List<S> findAll(Example<S> example, Sort sort) { return List.of(); }
    @Override public <S extends DeviceEntity> Page<S> findAll(Example<S> example, Pageable pageable) { return null; }
    @Override public <S extends DeviceEntity> long count(Example<S> example) { return 0; }
    @Override public <S extends DeviceEntity> boolean exists(Example<S> example) { return false; }
    @Override public <S extends DeviceEntity, R> R findBy(Example<S> example, Function<FluentQuery.FetchableFluentQuery<S>, R> queryFunction) { return null; }
    @Override public void flush() {}
    @Override public <S extends DeviceEntity> S saveAndFlush(S entity) { return save(entity); }
    @Override public <S extends DeviceEntity> List<S> saveAllAndFlush(Iterable<S> entities) { return saveAll(entities); }
    @Override public void deleteAllInBatch(Iterable<DeviceEntity> entities) { deleteAll(entities); }
    @Override public void deleteAllByIdInBatch(Iterable<Long> ids) { deleteAllById(ids); }
    @Override public void deleteAllInBatch() { deleteAll(); }
    @Override public DeviceEntity getOne(Long id) { return findById(id).orElse(null); }
    @Override public DeviceEntity getById(Long id) { return findById(id).orElse(null); }
    @Override public DeviceEntity getReferenceById(Long id) { return findById(id).orElse(null); }
}

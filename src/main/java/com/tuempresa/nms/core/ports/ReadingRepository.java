package com.tuempresa.nms.core.ports;

import com.tuempresa.nms.infrastructure.persistence.ReadingEntity;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.time.Instant;
import java.util.List;

@Repository
public interface ReadingRepository extends JpaRepository<ReadingEntity, Long> {
    List<ReadingEntity> findByDeviceIdOrderByPolledAtDesc(Long deviceId);
    List<ReadingEntity> findByDeviceIdAndPolledAtAfter(Long deviceId, Instant since);
}

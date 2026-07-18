package com.tuempresa.nms.core.ports;

import com.tuempresa.nms.infrastructure.persistence.DeviceEntity;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.Optional;

@Repository
public interface DeviceRepository extends JpaRepository<DeviceEntity, Long> {
    Optional<DeviceEntity> findByIp(String ip);
    List<DeviceEntity> findByBrand(String brand);
    List<DeviceEntity> findByFamilyCode(String familyCode);
}

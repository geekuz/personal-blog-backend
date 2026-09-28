package com.personalblog.media;

import java.util.List;
import java.util.UUID;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;

public interface MediaAssetRepository extends JpaRepository<MediaAsset, UUID> {
    List<MediaAsset> findAllByOrderByCreatedAtDesc(Pageable pageable);
}

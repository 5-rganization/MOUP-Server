package com.moup.server.model.dto;

import com.moup.server.model.entity.Workplace;

public interface WorkplaceUpdateRequest {
    Workplace toWorkplaceEntity(Long ownerId);
}

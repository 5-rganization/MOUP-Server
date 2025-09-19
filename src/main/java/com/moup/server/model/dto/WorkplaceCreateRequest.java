package com.moup.server.model.dto;

import com.moup.server.model.entity.Workplace;

public interface WorkplaceCreateRequest {
    String getWorkplaceName();
    String getCategoryName();
    String getLabelColor();
    String getAddress();
    Double getLatitude();
    Double getLongitude();
    Workplace toWorkplaceEntity(Long ownerId);
}

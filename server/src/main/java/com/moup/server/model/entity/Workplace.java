package com.moup.server.model.entity;

import lombok.*;

@Getter
@Builder
@ToString
public class Workplace {
    private Long id;
    private Long ownerId;
    private String workplaceName;
    private String categoryName;
    private boolean isShared;
    private String address;
    private Double latitude;
    private Double longitude;
}
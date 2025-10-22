package com.moup.server.model.entity;

import lombok.Builder;
import lombok.Getter;
import lombok.ToString;

@Getter
@Builder
@ToString
public class Worker {
    private Long id;
    private Long userId;
    private Long workplaceId;
    private String workerBasedLabelColor;
    private String ownerBasedLabelColor;
    private Boolean isAccepted;
    private Boolean isNowWorking;
}

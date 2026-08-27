package com.moup.domain.workplace.domain;

import lombok.*;

@Getter
@NoArgsConstructor
@AllArgsConstructor
@Builder
@ToString
public class Workplace {
    private Long id;
    private Long ownerId;
    private String workplaceName;
    private WorkplaceCategory categoryName;
    private boolean isShared;
    private String address;
    private Double latitude;
    private Double longitude;
}

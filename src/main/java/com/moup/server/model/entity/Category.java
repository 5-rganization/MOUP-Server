package com.moup.server.model.entity;

import lombok.*;

@Getter
@Builder
@ToString
@NoArgsConstructor
@AllArgsConstructor
public class Category {
    private Long id;
    private String categoryName;
}

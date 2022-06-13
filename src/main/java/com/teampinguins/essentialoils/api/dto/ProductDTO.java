package com.teampinguins.essentialoils.api.dto;

import lombok.*;
import lombok.experimental.FieldDefaults;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
@FieldDefaults(level = AccessLevel.PRIVATE)
public class ProductDTO {
    Long id;
    String name;
    String description;
    String img;
    String aroma;
    String usage;
    String benefits;
    String keywords;
}

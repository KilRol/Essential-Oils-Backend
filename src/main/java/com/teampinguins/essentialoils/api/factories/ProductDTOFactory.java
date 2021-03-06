package com.teampinguins.essentialoils.api.factories;

import com.teampinguins.essentialoils.api.dto.ProductDTO;
import com.teampinguins.essentialoils.model.entities.ProductEntity;
import org.springframework.stereotype.Component;

@Component
public class ProductDTOFactory {
    public ProductDTO makeProductDTO(ProductEntity entity) {
        return ProductDTO.builder()
                .id(entity.getId())
                .name(entity.getName())
                .description(entity.getDescription())
                .aroma(entity.getAroma())
                .img(entity.getImg())
                .benefits(entity.getBenefits())
                .usage(entity.getUsage())
                .warnings(entity.getWarnings())
                .keywords(entity.getKeywords())
                .build();
    }
}

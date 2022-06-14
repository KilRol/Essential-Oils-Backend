package com.teampinguins.essentialoils.api.services;

import com.teampinguins.essentialoils.api.dto.AckDTO;
import com.teampinguins.essentialoils.api.dto.ProductDTO;

import java.util.List;

public interface ProductService {
    List<ProductDTO> searchProduct(String q, int type);

    ProductDTO fetchProductById(Long id);

    List<ProductDTO> fetchAllProduct();

    ProductDTO createOrEditProduct(ProductDTO productRequest);

    AckDTO deleteProduct(Long id);

    List<ProductDTO> getSimilarProductsById(Long id);
}

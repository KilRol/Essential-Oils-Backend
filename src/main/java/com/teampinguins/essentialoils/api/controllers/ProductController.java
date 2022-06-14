package com.teampinguins.essentialoils.api.controllers;

import com.teampinguins.essentialoils.api.dto.AckDTO;
import com.teampinguins.essentialoils.api.dto.ProductDTO;
import com.teampinguins.essentialoils.api.services.ProductService;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.List;

@RestController("/")
public class ProductController {
    private final ProductService productService;

    public ProductController(ProductService productService) {
        this.productService = productService;
    }

    @RequestMapping(value = "/api/products", method = RequestMethod.GET)
    public ResponseEntity<List<ProductDTO>> fetchAll() {
        return ResponseEntity.ok(productService.fetchAllProduct());
    }

    @RequestMapping(value = "/api/products/{id}", method = RequestMethod.GET)
    public ResponseEntity<ProductDTO> fetchById(@PathVariable Long id) {
        return ResponseEntity.ok(productService.fetchProductById(id));
    }

    @RequestMapping(value = "/api/products/{id}/similar", method = RequestMethod.GET)
    public ResponseEntity<List<ProductDTO>> getSimilarById(@PathVariable Long id) {
        return ResponseEntity.ok(productService.getSimilarProductsById(id));
    }

    @RequestMapping(value = "/api/products/search", method = RequestMethod.GET)
    public ResponseEntity<List<ProductDTO>> search(@RequestParam(value = "q") String q,
                                                   @RequestParam(value = "type") int type) {
        return ResponseEntity.ok(productService.searchProduct(q, type));
    }

    @RequestMapping(value = "/api/products/add", method = RequestMethod.PUT)
    public ResponseEntity<ProductDTO> createOrEdit(@RequestBody ProductDTO productRequest) {
        return ResponseEntity.ok(productService.createOrEditProduct(productRequest));
    }

    @RequestMapping(value = "/api/products/{id}", method = RequestMethod.DELETE)
    public ResponseEntity<AckDTO> deleteById(@PathVariable Long id) {
        return ResponseEntity.ok(productService.deleteProduct(id));
    }
}

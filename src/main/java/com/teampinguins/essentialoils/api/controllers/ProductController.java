package com.teampinguins.essentialoils.api.controllers;

import com.teampinguins.essentialoils.api.dto.AckDTO;
import com.teampinguins.essentialoils.api.dto.ProductDTO;
import com.teampinguins.essentialoils.api.exceptions.BadRequestException;
import com.teampinguins.essentialoils.api.exceptions.NotFoundException;
import com.teampinguins.essentialoils.api.factories.ProductDTOFactory;
import com.teampinguins.essentialoils.api.services.JaccardSimilarity;
import com.teampinguins.essentialoils.store.entities.ProductEntity;
import com.teampinguins.essentialoils.store.repositories.ProductRepository;
import lombok.AccessLevel;
import lombok.RequiredArgsConstructor;
import lombok.experimental.FieldDefaults;
import org.springframework.web.bind.annotation.*;

import javax.transaction.Transactional;
import java.util.List;
import java.util.Objects;
import java.util.Optional;
import java.util.stream.Collectors;
import java.util.stream.Stream;

@RequiredArgsConstructor
@FieldDefaults(level = AccessLevel.PRIVATE, makeFinal = true)
@Transactional
@RestController
public class ProductController {

    public static final String FETCH_PRODUCT = "/api/products/{name}";
    public static final String FETCH_ALL_PRODUCTS = "/api/products";
    public static final String DELETE_PRODUCT = "/api/products/{id}";
    public static final String CREATE_OR_EDIT_PRODUCT = "/api/products/add";
    ProductDTOFactory productDTOFactory;
    ProductRepository productRepository;

    @GetMapping("/api/products/search")
    public List<ProductDTO> searchProduct(@RequestParam(value = "q", required = true) Optional<String> q,
                                          @RequestParam(value = "type", required = true) int type) {

        if (type == 0) {
            Stream<ProductEntity> productEntityStream = productRepository
                    .streamAllBy().filter(product -> {
                        JaccardSimilarity jaccardSimilarity = new JaccardSimilarity();
                        double c = jaccardSimilarity.apply(q.get(), product.getName());
                        System.out.println(c);
                        return c > 0.5;
                    });
            return productEntityStream.map(productDTOFactory::makeProductDTO).collect(Collectors.toList());
        }

        Stream<ProductEntity> productEntityStream = productRepository
                .streamAllBy().filter(product -> {
                    JaccardSimilarity jaccardSimilarity = new JaccardSimilarity();
                    double c = jaccardSimilarity.apply(q.get(), product.getName() + product.getDescription() + product.getAroma() + product.getBenefits());
                    System.out.println(c);
                    return c > 0.25;
                });
        return productEntityStream.map(productDTOFactory::makeProductDTO).collect(Collectors.toList());

    }
//    @GetMapping(FETCH_PRODUCT)
//    public ProductDTO fetchProduct(@PathVariable String name) {
//
//        if (name.isEmpty()) {
//            throw new BadRequestException("name cannot be empty");
//        }
//
//        ProductEntity product = productRepository
//                .findByName(name)
//                .orElseThrow(() -> new NotFoundException(
//                        String.format("Product with name \"%s\" doesn't exists", name))
//                );
//
//        return productDTOFactory.makeProductDTO(product);
//    }

    @GetMapping(FETCH_ALL_PRODUCTS)
    public List<ProductDTO> fetchAllProduct(@RequestParam(value = "prefix_name", required = false) Optional<String> optionalPrefixName) {

        optionalPrefixName = optionalPrefixName.filter(prefixName -> !prefixName.trim().isEmpty());

        Stream<ProductEntity> productEntityStream = optionalPrefixName
                .map(productRepository::streamAllByNameStartsWithIgnoreCase)
                .orElseGet(productRepository::streamAllBy);

        return productEntityStream.map(productDTOFactory::makeProductDTO).collect(Collectors.toList());
    }

    @PutMapping(CREATE_OR_EDIT_PRODUCT)
    public ProductDTO createOrEditProduct(@RequestBody ProductDTO prod) {
        Optional<Long> optionalProductId = Optional.ofNullable(prod.getId());
        Optional<String> optionalProductImg = Optional.ofNullable(prod.getImg()).filter(str -> !str.trim().isEmpty());
        Optional<String> optionalProductName = Optional.ofNullable(prod.getName()).filter(str -> !str.trim().isEmpty());
        Optional<String> optionalProductUsage = Optional.ofNullable(prod.getUsage()).filter(str -> !str.trim().isEmpty());
        Optional<String> optionalProductAroma = Optional.ofNullable(prod.getAroma()).filter(str -> !str.trim().isEmpty());
        Optional<String> optionalProductBenefits = Optional.ofNullable(prod.getBenefits()).filter(str -> !str.trim().isEmpty());
        Optional<String> optionalProductDescription = Optional.ofNullable(prod.getDescription()).filter(str -> !str.trim().isEmpty());

        boolean isCreate = optionalProductId.isEmpty();


        if (isCreate && optionalProductName.isEmpty()) {
            throw new BadRequestException("name cannot be empty");
        }

        final ProductEntity product = optionalProductId
                .map(this::getProductOrThrowException)
                .orElseGet(() -> ProductEntity.builder().build());

        optionalProductName.ifPresent(productName -> {
            productRepository.findByName(productName)
                    .filter(anotherProduct -> !Objects.equals(anotherProduct.getId(), product.getId()))
                    .ifPresent(anotherProduct -> {
                        throw new BadRequestException(String.format("Product \"%s\" already exists", productName));
                    });
            product.setName(productName);
        });

        optionalProductImg.ifPresent(product::setImg);
        optionalProductUsage.ifPresent(product::setUsage);
        optionalProductAroma.ifPresent(product::setAroma);
        optionalProductBenefits.ifPresent(product::setBenefits);
        optionalProductDescription.ifPresent(product::setDescription);

        final ProductEntity savedProduct = productRepository.saveAndFlush(product);
        return productDTOFactory.makeProductDTO(savedProduct);
    }

    @DeleteMapping(DELETE_PRODUCT)
    public AckDTO deleteProduct(@PathVariable Long id) {

        getProductOrThrowException(id);
        productRepository.deleteById(id);

        return AckDTO.makeDefault(true);
    }

    private ProductEntity getProductOrThrowException(Long productId) {
        return productRepository
                .findById(productId)
                .orElseThrow(() -> new NotFoundException(
                        String.format("Product with \"%s\" doesn't exists", productId))
                );
    }
}

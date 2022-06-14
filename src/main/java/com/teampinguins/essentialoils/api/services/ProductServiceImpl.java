package com.teampinguins.essentialoils.api.services;

import com.teampinguins.essentialoils.api.dto.AckDTO;
import com.teampinguins.essentialoils.api.dto.ProductDTO;
import com.teampinguins.essentialoils.api.exceptions.BadRequestException;
import com.teampinguins.essentialoils.api.exceptions.NotFoundException;
import com.teampinguins.essentialoils.api.factories.ProductDTOFactory;
import com.teampinguins.essentialoils.store.entities.ProductEntity;
import com.teampinguins.essentialoils.store.repositories.ProductRepository;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;
import java.util.Objects;
import java.util.Optional;
import java.util.stream.Collectors;
import java.util.stream.Stream;

@Service
@Transactional
public class ProductServiceImpl implements ProductService {

    private final ProductDTOFactory productDTOFactory;
    private final ProductRepository productRepository;

    public ProductServiceImpl(ProductDTOFactory productDTOFactory, ProductRepository productRepository) {
        this.productDTOFactory = productDTOFactory;
        this.productRepository = productRepository;
    }

    @Override
    public List<ProductDTO> searchProduct(String q, int type) {

        if (q.trim().isEmpty()) {
            throw new BadRequestException("Empty query");
        }

        Stream<ProductEntity> productEntityStream;
        if (type == 0) {
            System.out.println("SEARCH BY NAME");
            productEntityStream = productRepository
                    .streamAllBy().filter(product -> {
                        JaccardSimilarity jaccardSimilarity = new JaccardSimilarity();
                        double c = jaccardSimilarity.apply(q.toLowerCase(), product.getName().toLowerCase());
                        System.out.println(c);
                        return c > 0.3;
                    });

        } else {
            System.out.println("SEARCH BY TAG");
            productEntityStream = productRepository
                    .streamAllBy().filter(product -> product.getKeywords() != null && !product.getKeywords().isEmpty()).filter(product -> {
                        TanimotoSimilarity tanimotoSimilarity = new TanimotoSimilarity(4, 2, 0.2);
                        double c = tanimotoSimilarity.CalculateFuzzyEqualValue(q.toLowerCase(), product.getKeywords().toLowerCase());
                        System.out.println(c);
                        return c > 0.03;
                    });
        }
        System.out.println("----------");
        return productEntityStream.map(productDTOFactory::makeProductDTO).collect(Collectors.toList());
    }

    @Override
    public ProductDTO fetchProductById(Long id) {
        ProductEntity product = productRepository
                .findById(id)
                .orElseThrow(() -> new NotFoundException(
                        String.format("Product with id \"%s\" doesn't exists", id))
                );
        return productDTOFactory.makeProductDTO(product);
    }

    @Override
    public List<ProductDTO> fetchAllProduct() {
        Stream<ProductEntity> productEntityStream = productRepository.streamAllBy();
        return productEntityStream.map(productDTOFactory::makeProductDTO).collect(Collectors.toList());
    }

    @Override
    public ProductDTO createOrEditProduct(ProductDTO productRequest) {
        Optional<Long> optionalProductId = Optional.ofNullable(productRequest.getId());
        Optional<String> optionalProductImg = Optional.ofNullable(productRequest.getImg()).filter(str -> !str.trim().isEmpty());
        Optional<String> optionalProductName = Optional.ofNullable(productRequest.getName()).filter(str -> !str.trim().isEmpty());
        Optional<String> optionalProductUsage = Optional.ofNullable(productRequest.getUsage()).filter(str -> !str.trim().isEmpty());
        Optional<String> optionalProductAroma = Optional.ofNullable(productRequest.getAroma()).filter(str -> !str.trim().isEmpty());
        Optional<String> optionalProductKeywords = Optional.ofNullable(productRequest.getKeywords()).filter(str -> !str.trim().isEmpty());
        Optional<String> optionalProductBenefits = Optional.ofNullable(productRequest.getBenefits()).filter(str -> !str.trim().isEmpty());
        Optional<String> optionalProductDescription = Optional.ofNullable(productRequest.getDescription()).filter(str -> !str.trim().isEmpty());

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
        optionalProductKeywords.ifPresent(product::setKeywords);
        optionalProductBenefits.ifPresent(product::setBenefits);
        optionalProductDescription.ifPresent(product::setDescription);

        final ProductEntity savedProduct = productRepository.saveAndFlush(product);
        return productDTOFactory.makeProductDTO(savedProduct);
    }

    @Override
    public AckDTO deleteProduct(Long id) {

        getProductOrThrowException(id);
        productRepository.deleteById(id);

        return AckDTO.makeDefault(true);
    }

    @Override
    public List<ProductDTO> getSimilarProductsById(Long id) {
        ProductEntity p = productRepository
                .findById(id)
                .orElseThrow(() -> new NotFoundException(
                        String.format("Product with id \"%s\" doesn't exists", id))
                );
        System.out.println("SIMILARITY");
        Stream<ProductEntity> productEntityStream = productRepository
                .streamAllBy().filter(product -> product.getKeywords() != null && !product.getId().equals(id) && !product.getKeywords().isEmpty())
                .filter(product -> {

                    TanimotoSimilarity tanimotoSimilarity = new TanimotoSimilarity(4, 2, 0.15);
                    double c = tanimotoSimilarity.CalculateFuzzyEqualValue(p.getKeywords().toLowerCase(), product.getKeywords().toLowerCase());
                    System.out.println(c);
                    return c > 0.15;
                });
        System.out.println("----------");
        return productEntityStream.map(productDTOFactory::makeProductDTO).collect(Collectors.toList());
    }

    private ProductEntity getProductOrThrowException(Long productId) {
        return productRepository
                .findById(productId)
                .orElseThrow(() -> new NotFoundException(
                        String.format("Product with \"%s\" doesn't exists", productId))
                );
    }
}

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
import java.util.ArrayList;
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
    public static final String FETCH_PRODUCT_BY_ID = "/api/products/{id}";
    public static final String CREATE_OR_EDIT_PRODUCT = "/api/products/add";
    ProductDTOFactory productDTOFactory;
    ProductRepository productRepository;


    private static final int subTokenLength = 4;
    private static final double ThresholdWord = 0.35;
    private static final int MinWordLength = 2;


    private String NormalizeSentence(String sentence) {
        var resultContainer = new StringBuilder(100);
        CharSequence lowerSentece = sentence.toLowerCase();
        for (int i = 0; i < lowerSentece.length(); i++) {
            if (IsNormalChar(lowerSentece.charAt(i))) {
                resultContainer.append(lowerSentece.charAt(i));
            }
        }

        return resultContainer.toString();
    }

    /// <summary>
/// Возвращает признак подходящего символа.
/// </summary>
/// <param name="c">Символ.</param>
/// <returns>True - если символ буква или цифра или пробел, False - иначе.</returns>
    private boolean IsNormalChar(char c) {
        return Character.isLetterOrDigit(c) || c == ' ';
    }

    private ArrayList<String> GetTokens(String sentence) {
        var tokens = new ArrayList<String>();
        var words = sentence.split(" ");
        for (String word : words) {
            if (word.length() >= MinWordLength) {
                tokens.add(word);
            }
        }
        return tokens;
    }

    private boolean IsTokensFuzzyEqual(String firstToken, String secondToken) {
        int equalSubtokensCount = 0;
        if (secondToken.length() - subTokenLength + 1 <= 0) {
            return false;
        }
        boolean[] usedTokens = new boolean[secondToken.length() - subTokenLength + 1];
        for (var i = 0; i < firstToken.length() - subTokenLength + 1 && i <= subTokenLength; i++) {
            var subTokenFirst = firstToken.substring(i, subTokenLength);
            for (var j = 0; j < secondToken.length() - subTokenLength + 1 && j <= subTokenLength; j++) {
                if (!usedTokens[j]) {
                    var subTokenSecond = secondToken.substring(j, subTokenLength);
                    if (subTokenFirst.equals(subTokenSecond)) {
                        equalSubtokensCount++;
                        usedTokens[j] = true;
                        break;
                    }
                }
            }
        }

        var subTokenFirstCount = firstToken.length() - subTokenLength + 1;
        var subTokenSecondCount = secondToken.length() - subTokenLength + 1;
        var tanimoto = (1.0 * equalSubtokensCount) / (subTokenFirstCount + subTokenSecondCount - equalSubtokensCount);

        return ThresholdWord <= tanimoto;
    }

    public double CalculateFuzzyEqualValue(String first, String second) {
        if (first.trim().isEmpty() && second.trim().isEmpty()) {
            return 1.0;
        }

        if (first.trim().isEmpty() || second.trim().isEmpty()) {
            return 0.0;
        }

        var normalizeStringFirst = NormalizeSentence(first);
        var normalizeStringSecond = NormalizeSentence(second);

        var tokensFirst = GetTokens(normalizeStringFirst);
        var tokensSecond = GetTokens(normalizeStringSecond);

        var fuzzyEqualsTokens = GetFuzzyEqualsTokens(tokensFirst, tokensSecond);

        var equalsCount = fuzzyEqualsTokens.size();
        var firstCount = tokensFirst.size();
        var secondCount = tokensSecond.size();

        return (1.0 * equalsCount) / (firstCount + secondCount - equalsCount);
    }

    private ArrayList<String> GetFuzzyEqualsTokens(ArrayList<String> tokensFirst, ArrayList<String> tokensSecond) {
        var equalsTokens = new ArrayList<String>();
        var usedToken = new boolean[tokensSecond.size()];
        for (String s : tokensFirst) {
            for (var j = 0; j < tokensSecond.size(); ++j) {
                if (!usedToken[j]) {
                    if (IsTokensFuzzyEqual(s, tokensSecond.get(j))) {
                        equalsTokens.add(s);
                        usedToken[j] = true;
                        break;
                    }
                }
            }
        }

        return equalsTokens;
    }

    @GetMapping("/api/products/search")
    public List<ProductDTO> searchProduct(@RequestParam(value = "q", required = true) String q,
                                          @RequestParam(value = "type", required = true) int type) {

        if (q.trim().isEmpty()) {
            throw new BadRequestException("Empty query");
        }

        if (type == 0) {
            Stream<ProductEntity> productEntityStream = productRepository
                    .streamAllBy().filter(product -> {
                        JaccardSimilarity jaccardSimilarity = new JaccardSimilarity();
                        double c = jaccardSimilarity.apply(q.toLowerCase(), product.getName().toLowerCase());
                        System.out.println(c);
                        return c > 0.3;
                    });
            return productEntityStream.map(productDTOFactory::makeProductDTO).collect(Collectors.toList());
        } else {

            Stream<ProductEntity> productEntityStream = productRepository
                    .streamAllBy().filter(product -> product.getKeywords() != null && !product.getKeywords().isEmpty()).filter(product -> {
                        JaccardSimilarity jaccardSimilarity = new JaccardSimilarity();
                        double c = CalculateFuzzyEqualValue(q.toLowerCase(), product.getKeywords().toLowerCase());
                        System.out.println(c);
                        return c > 0.05;
                    });
            return productEntityStream.map(productDTOFactory::makeProductDTO).collect(Collectors.toList());
        }
    }

    @GetMapping(FETCH_PRODUCT_BY_ID)
    public ProductDTO fetchProductById(@PathVariable Long id) {
        ProductEntity product = productRepository
                .findById(id)
                .orElseThrow(() -> new NotFoundException(
                        String.format("Product with id \"%s\" doesn't exists", id))
                );
        return productDTOFactory.makeProductDTO(product);
    }

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
        Optional<String> optionalProductKeywords = Optional.ofNullable(prod.getKeywords()).filter(str -> !str.trim().isEmpty());
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
        optionalProductKeywords.ifPresent(product::setKeywords);
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

package com.teampinguins.essentialoils.store.entities;

import lombok.*;
import lombok.experimental.FieldDefaults;

import javax.persistence.*;
import java.util.Objects;

@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
@Entity
@FieldDefaults(level = AccessLevel.PRIVATE)
@Table(name = "t_products")
public class ProductEntity {
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Basic
    @Column(name = "name")
    private String name;

    @Basic
    @Column(name = "aroma")
    private String aroma;

    @Basic
    @Column(name = "description")
    private String description;

    @Basic
    @Column(name = "usage")
    private String usage;

    @Basic
    @Column(name = "img")
    private String img;

    @Basic
    @Column(name = "benefits")
    private String benefits;

    @Basic
    @Column(name = "keywords")
    private String keywords;

    @Override
    public boolean equals(Object o) {
        if (this == o) {
            return true;
        }
        if (o == null || getClass() != o.getClass()) {
            return false;
        }
        ProductEntity product = (ProductEntity) o;
        return Objects.equals(id, product.id);
    }

    @Override
    public int hashCode() {
        return Objects.hash(id);
    }
}

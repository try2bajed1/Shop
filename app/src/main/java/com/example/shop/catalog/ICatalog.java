package com.example.shop.catalog;

import android.support.annotation.Nullable;

public interface ICatalog {
    void onProductDetailsSelected(@Nullable Product product);

    void onProductSelected(@Nullable Product product);

    void onCategorySelected(@Nullable Category category);
}

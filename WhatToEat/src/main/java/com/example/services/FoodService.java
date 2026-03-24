package com.example.services;

import com.example.models.Food;
import java.util.ArrayList;
import java.util.List;
import java.util.Optional;

public class FoodService {
    private List<Food> foodList;

    public FoodService() {
        this.foodList = new ArrayList<>();
    }

    public void addFood(Food food) {
        foodList.add(food);
    }

    public List<Food> getAllFoods() {
        return new ArrayList<>(foodList);
    }

    public Optional<Food> getFoodByName(String name) {
        return foodList.stream()
                .filter(food -> food.getName().equalsIgnoreCase(name))
                .findFirst();
    }

    public boolean deleteFood(String name) {
        return foodList.removeIf(food -> food.getName().equalsIgnoreCase(name));
    }
}
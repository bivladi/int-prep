package com.example.rev.lb;

import java.util.List;

public interface RollingStrategy {
    String next(List<String> elementsAmount);
}

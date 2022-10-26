package com.jetbulb.interview;

import java.time.Instant;

public record Order(long id, Instant createdAt) {
}

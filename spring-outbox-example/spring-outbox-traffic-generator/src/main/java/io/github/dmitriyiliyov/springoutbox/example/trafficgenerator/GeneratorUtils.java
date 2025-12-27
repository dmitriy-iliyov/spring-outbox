package io.github.dmitriyiliyov.springoutbox.example.trafficgenerator;

import io.github.dmitriyiliyov.springoutbox.example.shared.OrderCreateDto;
import io.github.dmitriyiliyov.springoutbox.example.shared.OrderUpdateDto;
import lombok.experimental.UtilityClass;

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.Random;
import java.util.function.Supplier;
import java.util.stream.Collectors;

@UtilityClass
public class GeneratorUtils {

    public List<Long> generateIds(int desiredSize, List<Long> ids) {
        int size = Math.min(ids.size() / 2, desiredSize);
        List<Long> currentIds = new ArrayList<>(ids);
        Collections.shuffle(currentIds);
        return currentIds.subList(0, size);
    }

    public String generateItemsIds(Random random, int randomOrigin) {
        int count = random.nextInt(1, randomOrigin);
        String ids = random.ints(count, 1, randomOrigin)
                .mapToObj(Integer::toString)
                .collect(Collectors.joining(", "));
        return "[%s]".formatted(ids);
    }

    public List<OrderCreateDto> generateCreateDtoList(int size, Supplier<Long> userIdSupplier,
                                                      Supplier<String> itemIdsSupplier) {
        List<OrderCreateDto> toCreate = new ArrayList<>();
        for (int i = 0; i < size; i++) {
            toCreate.add(new OrderCreateDto(userIdSupplier.get(), itemIdsSupplier.get()));
        }
        return toCreate;
    }

    public List<OrderUpdateDto> generateUpdateDtoList(List<Long> ids, Supplier<String> itemIdsSupplier) {
        List<OrderUpdateDto> toUpdate = new ArrayList<>();
        for (Long id : ids) {
            toUpdate.add(new OrderUpdateDto(id, itemIdsSupplier.get()));
        }
        return toUpdate;
    }
}

package com.example.demo1.dto.request;

import com.example.demo1.common.enums.CollectionTargetType;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Size;
import lombok.Data;

@Data
public class FootprintRecordRequest {

    @NotNull
    private CollectionTargetType targetType;

    @NotNull
    private Long targetId;

    @Size(max = 255)
    private String title;

    @Size(max = 500)
    private String summary;

    @Size(max = 255)
    private String coverUrl;
}


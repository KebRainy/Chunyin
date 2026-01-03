package com.example.demo1.dto.request;

import jakarta.validation.constraints.Future;
import jakarta.validation.constraints.Min;
import jakarta.validation.constraints.NotEmpty;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Size;
import lombok.Data;

import java.time.LocalDateTime;
import java.util.List;

/**
 * 创建活动请求
 */
@Data
public class CreateActivityRequest {
    /**
     * 活动时间
     */
    @NotNull(message = "活动时间不能为空")
    @Future(message = "活动时间必须是未来时间")
    private LocalDateTime activityTime;

    /**
     * 酒类标签ID列表（最多选择2个）
     */
    @NotEmpty(message = "请至少选择一个酒类标签")
    @Size(max = 2, message = "最多只能选择2个酒类标签")
    private List<Long> alcoholIds;

    /**
     * 酒吧ID（可选，如果不提供则根据酒类标签推荐）
     */
    private Long barId;

    /**
     * 参与人数上限
     */
    @NotNull(message = "参与人数上限不能为空")
    @Min(value = 2, message = "参与人数上限至少为2人")
    private Integer maxParticipants;

    /**
     * 备注
     */
    private String remark;
}


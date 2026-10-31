package com.aichat.model.dto;

import com.aichat.constant.BusinessConstants;
import jakarta.validation.Valid;
import jakarta.validation.constraints.NotEmpty;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Size;
import java.util.List;

/** 用户确认后的候选内容与树版本。 */
public record AcceptSuggestionDTO(@NotNull Long revision,
    @NotEmpty @Size(max = BusinessConstants.MAX_AI_CANDIDATES) List<@Valid CandidateDTO> children) {
}

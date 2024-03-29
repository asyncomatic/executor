//  Copyright (c) 2024 JC Cormier
//  All rights reserved.
//  SPDX-License-Identifier: MIT
//  For full license text, see LICENSE file in the repo root or https://opensource.org/licenses/MIT

package io.github.asyncomatic.context;

import com.google.gson.annotations.SerializedName;
import lombok.Getter;
import lombok.Setter;

public class Context {
    @Getter
    @Setter
    @SerializedName("class")
    private String className = "";

    @Getter
    @Setter
    @SerializedName("method")
    private String methodName = "";

    @Getter
    @Setter
    @SerializedName("queue")
    private String queueName = "";

    @Getter
    @Setter
    @SerializedName("state")
    private Object testState;

    @Getter
    @Setter
    @SerializedName("delay")
    private Long delayDuration = 0L;

    @Getter
    @Setter
    @SerializedName("retry_count")
    private int retryCount = 0;
}

package com.example.literaturesearchsystem.common;

import lombok.Getter;

@Getter
public enum CorrectionStatusEnum {

    PENDING(0, "待审核"),
    APPROVED(1, "已通过"),
    REJECTED(2, "已驳回");

    private final Integer code;
    private final String desc;

    CorrectionStatusEnum(Integer code, String desc) {
        this.code = code;
        this.desc = desc;
    }

    public static String getDescByCode(Integer code) {
        for (CorrectionStatusEnum status : values()) {
            if (status.code.equals(code)) {
                return status.desc;
            }
        }
        return "未知";
    }
}
package com.example.literaturesearchsystem.common;

import lombok.Getter;

@Getter
public enum LiteratureStatusEnum {

    PENDING(0, "待审核", "warning"),
    APPROVED(1, "已通过", "success"),
    REJECTED(2, "已驳回", "danger");

    private final Integer code;
    private final String desc;
    private final String badgeClass;

    LiteratureStatusEnum(Integer code, String desc, String badgeClass) {
        this.code = code;
        this.desc = desc;
        this.badgeClass = badgeClass;
    }

    public Integer getCode() {
        return code;
    }

    public static String getDescByCode(Integer code) {
        for (LiteratureStatusEnum status : values()) {
            if (status.code.equals(code)) {
                return status.desc;
            }
        }
        return "未知";
    }

    public static String getBadgeClass(Integer code) {
        for (LiteratureStatusEnum status : values()) {
            if (status.code.equals(code)) {
                return status.badgeClass;
            }
        }
        return "info";
    }
}
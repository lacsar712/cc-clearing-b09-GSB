package com.clearing.netting.domain.model;

public enum DuplicateGroupStatus {
    /** 待处理：组内仍有未复核的疑似重复义务，阻止对应交割日+币种轧差。 */
    PENDING,
    /** 已复核：操作员确认组内义务均有效，不再阻止轧差。 */
    REVIEWED
}

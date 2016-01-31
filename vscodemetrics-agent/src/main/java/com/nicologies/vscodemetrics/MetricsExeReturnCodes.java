package com.nicologies.vscodemetrics;

import java.util.EnumSet;

public enum MetricsExeReturnCodes {
    ANALYSIS_ERROR(0x1),
    TARGET_LOAD_ERROR(0x8),
    OUTPUT_ERROR(0x40),
    COMMAND_LINE_SWITCH_ERROR(0x80),
    INITIALIZATION_ERROR(0x100),
    ASSEMBLY_REFERENCES_ERROR(0x200),
    UNKNOWN_ERROR(0x1000000);

    private final int myCode;

    public int getCode() {
        return myCode;
    }

    MetricsExeReturnCodes(final int code) {
        myCode = code;
    }

    public static EnumSet<MetricsExeReturnCodes> decodeReturnCode(int code) {
        EnumSet<MetricsExeReturnCodes> result = EnumSet.noneOf(MetricsExeReturnCodes.class);

        for (MetricsExeReturnCodes value : MetricsExeReturnCodes.values()) {
            if ((code & value.getCode()) != 0) {
                result.add(value);
            }
        }

        return result;
    }
}

package com.pullit.item.enums;

import lombok.Getter;
import lombok.RequiredArgsConstructor;

@Getter
@RequiredArgsConstructor
public enum AreaCode {
    MA("수학"),
    KO("국어"),
    EN("영어"),
    SO("사회"),
    HS("역사"),
    MO("도덕"),
    SC("과학");

    private final String title;

    public static String getStringCode(AreaCode areaCode) {
        return areaCode != null ? areaCode.name() : null;
    }

}

package com.pullit.common.embedded;

import jakarta.persistence.Column;
import jakarta.persistence.Embeddable;
import lombok.*;

@Embeddable
@Getter
@NoArgsConstructor(access = AccessLevel.PROTECTED)
@AllArgsConstructor
@Builder
public class StringCodeNamePair {

    @Column(length = 50)
    private String code;

    @Column(length = 200)
    private String name;

    public boolean isValid() {
        return code != null && !code.trim().isEmpty();
    }

    public String getDisplayName() {
        if (name != null && !name.trim().isEmpty()) {
            return name;
        }
        return code != null ? code : "";
    }

    public boolean hasCode(String targetCode) {
        return code != null && code.equals(targetCode);
    }

}

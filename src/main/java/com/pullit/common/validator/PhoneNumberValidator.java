package com.pullit.common.validator;

import com.pullit.common.annotation.PhoneNumber;
import com.pullit.common.constants.ValidationConstants;
import com.pullit.common.utils.StringUtils;
import jakarta.validation.ConstraintValidator;
import jakarta.validation.ConstraintValidatorContext;
import java.util.regex.Pattern;

/**
 * @PhoneNumber 어노테이션의 검증 로직 구현
 */
public class PhoneNumberValidator implements ConstraintValidator<PhoneNumber, String> {
    
    private static final Pattern PHONE_PATTERN = Pattern.compile(ValidationConstants.PATTERN_PHONE_CLEAN);
    private boolean required;
    
    @Override
    public void initialize(PhoneNumber annotation) {
        this.required = annotation.required();
    }
    
    @Override
    public boolean isValid(String value, ConstraintValidatorContext context) {
        if (StringUtils.isEmpty(value)) {
            return !required;
        }
        
        // 하이픈 제거 후 검증
        String cleanPhone = value.replaceAll("-", "");
        return PHONE_PATTERN.matcher(cleanPhone).matches();
    }
}
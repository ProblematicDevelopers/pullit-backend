package com.pullit.common.validator;

import com.pullit.common.annotation.ValidEnum;
import jakarta.validation.ConstraintValidator;
import jakarta.validation.ConstraintValidatorContext;

/**
 * @ValidEnum 어노테이션의 검증 로직 구현
 */
public class EnumValidator implements ConstraintValidator<ValidEnum, String> {
    
    private Class<? extends Enum<?>> enumClass;
    private boolean ignoreCase;
    private boolean allowNull;
    
    @Override
    public void initialize(ValidEnum annotation) {
        this.enumClass = annotation.enumClass();
        this.ignoreCase = annotation.ignoreCase();
        this.allowNull = annotation.allowNull();
    }
    
    @Override
    public boolean isValid(String value, ConstraintValidatorContext context) {
        if (value == null) {
            return allowNull;
        }
        
        Enum<?>[] enumConstants = enumClass.getEnumConstants();
        for (Enum<?> enumConstant : enumConstants) {
            String enumValue = enumConstant.name();
            if (ignoreCase ? enumValue.equalsIgnoreCase(value) : enumValue.equals(value)) {
                return true;
            }
        }
        
        return false;
    }
}
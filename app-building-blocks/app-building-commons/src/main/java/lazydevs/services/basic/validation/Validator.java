package lazydevs.services.basic.validation;

import lombok.Getter;
import lombok.Setter;
import lombok.ToString;

@Getter @Setter @ToString
public  class Validator{
    private boolean required = true;
    private String typeFqcn;
    private String regexValidator;
    private String jsFunctionName;
}
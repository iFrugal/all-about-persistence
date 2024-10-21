package lazydevs.services.basic.validation;

import lombok.Getter;
import lombok.Setter;
import lombok.ToString;


@Getter
@Setter
@ToString
public  class Param extends Validator{
    private Object defaultValue;
}


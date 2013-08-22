package main.com.encens.khipus.service.production;

import javax.ejb.Local;

@Local
public interface ProductionOrderCodeGeneratorService {

    public int findLasCounter(String seed);
}

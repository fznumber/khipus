package main.com.encens.khipus.service.production;

import com.encens.hp90.exception.production.ProductCompositionException;
import com.encens.hp90.framework.service.GenericService;
import com.encens.hp90.model.production.ProductComposition;
import com.encens.hp90.model.production.ProductionOrder;

import javax.ejb.Local;
import java.io.IOException;

@Local
public interface EvaluatorMathematicalExpressionsService {

    void executeMathematicalFormulas(ProductComposition productComposition) throws ProductCompositionException, IOException;

    void executeMathematicalFormulas(ProductionOrder productionOrder) throws ProductCompositionException, IOException;
}

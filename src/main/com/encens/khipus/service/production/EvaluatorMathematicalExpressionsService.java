package com.encens.khipus.service.production;

import com.encens.khipus.exception.production.ProductCompositionException;
import com.encens.khipus.model.production.ProductComposition;
import com.encens.khipus.model.production.ProductionOrder;

import javax.ejb.Local;
import java.io.IOException;

@Local
public interface EvaluatorMathematicalExpressionsService {

    void executeMathematicalFormulas(ProductComposition productComposition) throws ProductCompositionException, IOException;

    void executeMathematicalFormulas(ProductionOrder productionOrder) throws ProductCompositionException, IOException;

    public void excuteFormulate(ProductionOrder productionOrder, Double formulateContainer, Double formulateSupposed) throws ProductCompositionException, IOException;

    public void excuteParemeterizadFormulate(ProductionOrder productionOrder, Double formulateContainer, Double formulateSupposed) throws ProductCompositionException, IOException;

    public Double excuteParemeterized(ProductionOrder productionOrder, Double containerWeight, Double supposedAmount)throws ProductCompositionException, IOException;
}

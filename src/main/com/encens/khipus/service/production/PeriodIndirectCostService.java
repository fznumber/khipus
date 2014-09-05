package com.encens.khipus.service.production;

import com.encens.khipus.model.employees.Gestion;
import com.encens.khipus.model.employees.Month;
import com.encens.khipus.model.production.PeriodIndirectCost;

import javax.ejb.Local;

/**
 * Created by Diego on 29/08/2014.
 */
@Local
public interface PeriodIndirectCostService {
    public PeriodIndirectCost findLastPeriodIndirectCost();

    PeriodIndirectCost findPeriodIndirect(Month month, Gestion gestion);

    boolean findPeriodIndirect(PeriodIndirectCost periodIndirectCost);

    PeriodIndirectCost findLastPeriodIndirectCostUsed();

    boolean findPeriodIndirectCostUsed(PeriodIndirectCost periodIndirectCost);
}

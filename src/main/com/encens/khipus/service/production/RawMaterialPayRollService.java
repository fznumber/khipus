package com.encens.khipus.service.production;


import com.encens.khipus.exception.EntryDuplicatedException;
import com.encens.khipus.exception.EntryNotFoundException;
import com.encens.khipus.exception.production.RawMaterialPayRollException;
import com.encens.khipus.framework.service.GenericService;
import com.encens.khipus.model.production.*;

import javax.ejb.Local;
import java.util.Calendar;
import java.util.Date;
import java.util.List;

@Local
public interface RawMaterialPayRollService extends GenericService {

    public RawMaterialPayRoll generatePayroll(RawMaterialPayRoll rawMaterialPayRoll) throws EntryNotFoundException, RawMaterialPayRollException;

    void calculateLiquidPayable(RawMaterialPayRoll rawMaterialPayRoll);

    void create(RawMaterialPayRoll rawMaterialPayRoll) throws EntryDuplicatedException, RawMaterialPayRollException;

    void createAll(RawMaterialPayRoll rawMaterialPayRoll) throws EntryDuplicatedException, RawMaterialPayRollException;

    void validate(RawMaterialPayRoll rawMaterialPayRoll) throws RawMaterialPayRollException;

    RawMaterialPayRoll getTotalsRawMaterialPayRoll(Calendar dateIni, Calendar dateEnd, ProductiveZone productiveZone, MetaProduct metaProduct);

    RawMaterialPayRollServiceBean.Discounts getDiscounts(Calendar dateIni, Calendar dateEnd,ProductiveZone zone, MetaProduct metaProduct);

    RawMaterialPayRollServiceBean.SummaryTotal getSumaryTotal(Calendar dateIni, Calendar dateEnd,ProductiveZone zone, MetaProduct metaProduct);

    List<RawMaterialPayRecordDetailDummy> generateDetails(RawMaterialPayRecord rawMaterialPayRecord) throws RawMaterialPayRollException;

    Double getTotalWeightMoney(double unitPrice,Calendar startDate,Calendar endDate, MetaProduct metaProduct);

    Double getTotalMoneyDiff(double unitPrice,Calendar startDate,Calendar endDate, MetaProduct metaProduct);

    Double getBalanceWeightTotal(Double unitPrice, Calendar dateIni, Calendar dateEnd, MetaProduct metaProduct);

    Double getTotalDiff(double unitPrice,Calendar startDate,Calendar endDate, MetaProduct metaProduct);

    List<RawMaterialPayRoll> findAll(Date startDate, Date endDate, MetaProduct metaProduct);

    List<RawMaterialPayRoll> findAll();
}

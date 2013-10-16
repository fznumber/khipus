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

    RawMaterialPayRoll getTotalsRawMaterialPayRoll(Date dateIni, Date dateEnd, ProductiveZone productiveZone, MetaProduct metaProduct);

    RawMaterialPayRollServiceBean.Discounts getDiscounts(Date dateIni, Date dateEnd,ProductiveZone zone, MetaProduct metaProduct);

    RawMaterialPayRollServiceBean.SummaryTotal getSumaryTotal(Date dateIni, Date dateEnd,ProductiveZone zone, MetaProduct metaProduct);

    List<RawMaterialPayRecordDetailDummy> generateDetails(RawMaterialPayRecord rawMaterialPayRecord) throws RawMaterialPayRollException;

    Double getTotalWeightMoney(double unitPrice,Date startDate,Date endDate, MetaProduct metaProduct);

    Double getTotalMoneyDiff(double unitPrice,Date startDate,Date endDate, MetaProduct metaProduct);

    Double getBalanceWeightTotal(Double unitPrice, Date dateIni, Date dateEnd, MetaProduct metaProduct);

    Double getTotalDiff(double unitPrice,Date startDate,Date endDate, MetaProduct metaProduct);

    List<RawMaterialPayRoll> findAll(Date startDate, Date endDate, MetaProduct metaProduct);

    List<RawMaterialPayRoll> findAll();

    boolean verifDayColected(Calendar date_aux, ProductiveZone zone);
}

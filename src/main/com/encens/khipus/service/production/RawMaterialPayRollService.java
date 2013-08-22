package com.encens.khipus.service.production;


import com.encens.khipus.exception.EntryDuplicatedException;
import com.encens.khipus.exception.EntryNotFoundException;
import com.encens.khipus.exception.production.RawMaterialPayRollException;
import com.encens.khipus.framework.service.GenericService;
import com.encens.khipus.model.production.RawMaterialPayRecord;
import com.encens.khipus.model.production.RawMaterialPayRecordDetailDummy;
import com.encens.khipus.model.production.RawMaterialPayRoll;

import javax.ejb.Local;
import java.util.List;

@Local
public interface RawMaterialPayRollService extends GenericService {

    public RawMaterialPayRoll generatePayroll(RawMaterialPayRoll rawMaterialPayRoll) throws EntryNotFoundException, RawMaterialPayRollException;

    void calculateLiquidPayable(RawMaterialPayRoll rawMaterialPayRoll);

    void create(RawMaterialPayRoll rawMaterialPayRoll) throws EntryDuplicatedException, RawMaterialPayRollException;

    void validate(RawMaterialPayRoll rawMaterialPayRoll) throws RawMaterialPayRollException;

    List<RawMaterialPayRecordDetailDummy> generateDetails(RawMaterialPayRecord rawMaterialPayRecord) throws RawMaterialPayRollException;
}

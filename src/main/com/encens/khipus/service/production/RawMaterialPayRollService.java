package main.com.encens.khipus.service.production;


import com.encens.hp90.exception.EntryDuplicatedException;
import com.encens.hp90.exception.EntryNotFoundException;
import com.encens.hp90.exception.production.RawMaterialPayRollException;
import com.encens.hp90.framework.service.GenericService;
import com.encens.hp90.model.production.RawMaterialPayRecord;
import com.encens.hp90.model.production.RawMaterialPayRecordDetailDummy;
import com.encens.hp90.model.production.RawMaterialPayRoll;

import javax.ejb.Local;
import javax.ejb.TransactionAttribute;
import javax.ejb.TransactionAttributeType;
import java.util.List;

@Local
public interface RawMaterialPayRollService extends GenericService {

    public RawMaterialPayRoll generatePayroll(RawMaterialPayRoll rawMaterialPayRoll) throws EntryNotFoundException, RawMaterialPayRollException;

    void calculateLiquidPayable(RawMaterialPayRoll rawMaterialPayRoll);

    void create(RawMaterialPayRoll rawMaterialPayRoll) throws EntryDuplicatedException, RawMaterialPayRollException;

    void validate(RawMaterialPayRoll rawMaterialPayRoll) throws RawMaterialPayRollException;

    List<RawMaterialPayRecordDetailDummy> generateDetails(RawMaterialPayRecord rawMaterialPayRecord) throws RawMaterialPayRollException;
}
